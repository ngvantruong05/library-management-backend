package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.BookRatingSummaryDto;
import edu.uet.library_management.domain.dto.RatingCreateUpdateRequest;
import edu.uet.library_management.domain.dto.RatingDto;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.BookLoan;
import edu.uet.library_management.domain.model.Rating;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.domain.service.RatingService;
import edu.uet.library_management.infrastructure.persistence.BookLoanRepository;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.RatingRepository;
import edu.uet.library_management.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookLoanRepository bookLoanRepository;

    @Override
    @Transactional
    public RatingDto rateBook(RatingCreateUpdateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + request.getBookId()));

        Optional<Rating> existingRatingOpt = ratingRepository.findByUserEmailAndBookId(userEmail, request.getBookId());

        Rating rating;
        if (existingRatingOpt.isPresent()) {
            rating = existingRatingOpt.get();
            rating.setScore(request.getScore());
            rating.setReview(request.getReview());
        } else {
            rating = Rating.builder()
                    .user(user)
                    .book(book)
                    .score(request.getScore())
                    .review(request.getReview())
                    .build();
        }

        Rating saved = ratingRepository.save(rating);
        boolean isBorrower = checkIfVerifiedBorrower(user.getId(), book.getId());
        return toDto(saved, isBorrower);
    }

    @Override
    @Transactional
    public void deleteRatingByBook(Long bookId, String userEmail) {
        Rating rating = ratingRepository.findByUserEmailAndBookId(userEmail, bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found for this book"));
        ratingRepository.delete(rating);
    }

    @Override
    @Transactional
    public void deleteRatingById(Long id, String currentUserEmail) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found with id: " + id));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isOwner = rating.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this rating");
        }

        ratingRepository.delete(rating);
    }

    @Override
    public BookRatingSummaryDto getBookRatingSummary(Long bookId, String currentUserEmail) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId);
        }

        Double avgRaw = ratingRepository.getAverageRatingByBookId(bookId);
        double averageRating = 0.0;
        if (avgRaw != null) {
            averageRating = BigDecimal.valueOf(avgRaw)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        Long totalCount = ratingRepository.countByBookId(bookId);
        long totalRatings = totalCount != null ? totalCount : 0L;

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            distribution.put(i, 0L);
        }

        List<Object[]> rawDist = ratingRepository.getRatingDistributionByBookId(bookId);
        for (Object[] row : rawDist) {
            if (row != null && row.length >= 2) {
                int score = ((Number) row[0]).intValue();
                long count = ((Number) row[1]).longValue();
                distribution.put(score, count);
            }
        }

        RatingDto currentUserRating = null;
        if (currentUserEmail != null && !currentUserEmail.trim().isEmpty()) {
            currentUserRating = ratingRepository.findByUserEmailAndBookId(currentUserEmail, bookId)
                    .map(r -> toDto(r, checkIfVerifiedBorrower(r.getUser().getId(), bookId)))
                    .orElse(null);
        }

        return BookRatingSummaryDto.builder()
                .bookId(bookId)
                .averageRating(averageRating)
                .totalRatings(totalRatings)
                .distribution(distribution)
                .currentUserRating(currentUserRating)
                .build();
    }

    @Override
    public Page<RatingDto> getBookRatings(Long bookId, Pageable pageable) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId);
        }

        Page<Rating> ratingsPage = ratingRepository.findByBookIdOrderByCreatedAtDesc(bookId, pageable);
        return ratingsPage.map(r -> toDto(r, checkIfVerifiedBorrower(r.getUser().getId(), bookId)));
    }

    @Override
    public RatingDto getUserRating(Long bookId, String userEmail) {
        return ratingRepository.findByUserEmailAndBookId(userEmail, bookId)
                .map(r -> toDto(r, checkIfVerifiedBorrower(r.getUser().getId(), bookId)))
                .orElse(null);
    }

    private boolean checkIfVerifiedBorrower(Long userId, Long bookId) {
        List<BookLoan> loans = bookLoanRepository.findByUserId(userId);
        return loans.stream().anyMatch(l -> l.getBook() != null && l.getBook().getId().equals(bookId));
    }

    private RatingDto toDto(Rating rating, boolean verifiedBorrower) {
        return RatingDto.builder()
                .id(rating.getId())
                .bookId(rating.getBook().getId())
                .userId(rating.getUser().getId())
                .userEmail(rating.getUser().getEmail())
                .userDisplayName(rating.getUser().getDisplayName() != null ? rating.getUser().getDisplayName() : rating.getUser().getEmail())
                .userPhotoUrl(rating.getUser().getPhotoUrl())
                .score(rating.getScore())
                .review(rating.getReview())
                .verifiedBorrower(verifiedBorrower)
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}
