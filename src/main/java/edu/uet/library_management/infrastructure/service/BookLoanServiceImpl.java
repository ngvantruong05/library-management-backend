package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.BookLoanCreateRequest;
import edu.uet.library_management.domain.dto.BookLoanDto;
import edu.uet.library_management.domain.enums.FineStatus;
import edu.uet.library_management.domain.enums.LoanStatus;
import edu.uet.library_management.domain.enums.LoanType;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.BookLoan;
import edu.uet.library_management.domain.model.Fine;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.domain.service.BookCopyService;
import edu.uet.library_management.domain.service.BookLoanService;
import edu.uet.library_management.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookLoanServiceImpl implements BookLoanService {

    private final BookLoanRepository bookLoanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookCopyService bookCopyService;
    private final FineRepository fineRepository;

    @Override
    @Transactional
    public BookLoanDto createLoan(BookLoanCreateRequest request, String currentUserEmail) {
        User user;
        // If Admin specifies a userId, find that user. Otherwise, find by current user's email.
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + request.getUserId()));
        } else {
            user = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logged-in user details not found"));
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + request.getBookId()));

        if (request.getType() == LoanType.OFFLINE) {
            if (request.getNumCopies() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of copies must be greater than 0 for OFFLINE borrowing");
            }
            // Check inventory copies
            var copy = bookCopyRepository.findByBookId(book.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No inventory copies defined for this book"));
            if (copy.getAvailableCopies() < request.getNumCopies()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough copies available in library shelf. Available: " + copy.getAvailableCopies());
            }
            // Decrement copies
            bookCopyService.increaseCopy(book.getId(), -request.getNumCopies());
        } else {
            request.setNumCopies(0); // Online loans don't have copies count
        }

        LocalDateTime borrowDate = request.getBorrowDate() != null ? request.getBorrowDate() : LocalDateTime.now();
        LocalDateTime dueDate = request.getDueDate() != null ? request.getDueDate() : borrowDate.plusDays(14); // 14-day default

        BookLoan bookLoan = BookLoan.builder()
                .user(user)
                .book(book)
                .borrowDate(borrowDate)
                .dueDate(dueDate)
                .status(LoanStatus.BORROWED)
                .type(request.getType())
                .numCopies(request.getNumCopies())
                .valid(true)
                .build();

        BookLoan saved = bookLoanRepository.save(bookLoan);
        return toDto(saved);
    }

    @Override
    @Transactional
    public BookLoanDto returnBook(Long loanId) {
        BookLoan loan = bookLoanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book loan record not found with id: " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book has already been returned");
        }

        LocalDateTime returnDate = LocalDateTime.now();
        loan.setReturnDate(returnDate);
        loan.setStatus(LoanStatus.RETURNED);
        loan.setValid(false);
        BookLoan saved = bookLoanRepository.save(loan);

        // If OFFLINE, increase copy count back
        if (loan.getType() == LoanType.OFFLINE) {
            bookCopyService.increaseCopy(loan.getBook().getId(), loan.getNumCopies());
        }

        // Calculate fine if overdue
        if (returnDate.isAfter(loan.getDueDate())) {
            long daysBetween = ChronoUnit.DAYS.between(loan.getDueDate().toLocalDate(), returnDate.toLocalDate());
            if (daysBetween > 0) {
                // Fine rate: 5,000 VND per day per copy (default 5,000 for ONLINE too as 1 unit)
                int multiplier = loan.getType() == LoanType.OFFLINE ? loan.getNumCopies() : 1;
                double fineAmount = daysBetween * multiplier * 5000.0;

                Fine fine = Fine.builder()
                        .bookLoan(saved)
                        .fineAmount(fineAmount)
                        .overdueDays((int) daysBetween)
                        .status(FineStatus.UNPAID)
                        .build();
                fineRepository.save(fine);
            }
        }

        return toDto(saved);
    }

    @Override
    public List<BookLoanDto> getMyLoans(String email) {
        return bookLoanRepository.findByUserEmail(email).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookLoanDto> getAllLoans() {
        return bookLoanRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BookLoanDto getLoanById(Long id) {
        BookLoan loan = bookLoanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book loan not found with id: " + id));
        return toDto(loan);
    }

    @Override
    @Transactional
    public boolean refreshDatabase() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<BookLoan> activeLoans = bookLoanRepository.findByStatus(LoanStatus.BORROWED);
            for (BookLoan loan : activeLoans) {
                if (now.isAfter(loan.getDueDate())) {
                    loan.setStatus(LoanStatus.OVERDUE);
                    bookLoanRepository.save(loan);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private BookLoanDto toDto(BookLoan loan) {
        return BookLoanDto.builder()
                .id(loan.getId())
                .userId(loan.getUser().getId())
                .userEmail(loan.getUser().getEmail())
                .userDisplayName(loan.getUser().getDisplayName())
                .bookId(loan.getBook().getId())
                .bookTitle(loan.getBook().getTitle())
                .bookThumbnail(loan.getBook().getThumbnail())
                .borrowDate(loan.getBorrowDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .status(loan.getStatus())
                .type(loan.getType())
                .numCopies(loan.getNumCopies())
                .valid(loan.isValid())
                .lastUpdated(loan.getLastUpdated())
                .build();
    }
}
