package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.BookCopyDto;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.BookCopy;
import edu.uet.library_management.domain.service.BookCopyService;
import edu.uet.library_management.infrastructure.persistence.BookCopyRepository;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BookCopyServiceImpl implements BookCopyService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;

    @Override
    public BookCopyDto getCopyByBookId(Long bookId) {
        BookCopy copy = bookCopyRepository.findByBookId(bookId)
                .orElseGet(() -> {
                    Book book = bookRepository.findById(bookId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId));
                    BookCopy newCopy = BookCopy.builder()
                            .book(book)
                            .totalCopies(0)
                            .availableCopies(0)
                            .build();
                    return bookCopyRepository.save(newCopy);
                });
        return toDto(copy);
    }

    @Override
    @Transactional
    public BookCopyDto updateCopy(Long bookId, int totalCopies) {
        if (totalCopies < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total copies cannot be negative");
        }

        BookCopy copy = bookCopyRepository.findByBookId(bookId)
                .orElseGet(() -> {
                    Book book = bookRepository.findById(bookId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId));
                    return BookCopy.builder()
                            .book(book)
                            .totalCopies(0)
                            .availableCopies(0)
                            .build();
                });

        int diff = totalCopies - copy.getTotalCopies();
        int newAvailable = copy.getAvailableCopies() + diff;
        if (newAvailable < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot decrease total copies because some copies are currently borrowed");
        }

        copy.setTotalCopies(totalCopies);
        copy.setAvailableCopies(newAvailable);
        BookCopy saved = bookCopyRepository.save(copy);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void increaseCopy(Long bookId, int count) {
        BookCopy copy = bookCopyRepository.findByBookId(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found for book id: " + bookId));
        int newAvailable = copy.getAvailableCopies() + count;
        if (newAvailable < 0 || newAvailable > copy.getTotalCopies()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid inventory operation: available copies would become " + newAvailable);
        }
        copy.setAvailableCopies(newAvailable);
        bookCopyRepository.save(copy);
    }

    private BookCopyDto toDto(BookCopy copy) {
        return BookCopyDto.builder()
                .id(copy.getId())
                .bookId(copy.getBook().getId())
                .bookTitle(copy.getBook().getTitle())
                .totalCopies(copy.getTotalCopies())
                .availableCopies(copy.getAvailableCopies())
                .lastUpdated(copy.getLastUpdated())
                .build();
    }
}
