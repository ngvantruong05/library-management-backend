package edu.uet.library_management.interfaces.rest;

import edu.uet.library_management.domain.dto.BookRatingSummaryDto;
import edu.uet.library_management.domain.dto.RatingCreateUpdateRequest;
import edu.uet.library_management.domain.dto.RatingDto;
import edu.uet.library_management.domain.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @GetMapping("/book/{bookId}/summary")
    public ResponseEntity<BookRatingSummaryDto> getBookRatingSummary(
            @PathVariable Long bookId,
            Authentication authentication) {
        String currentUserEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ratingService.getBookRatingSummary(bookId, currentUserEmail));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<RatingDto>> getBookRatings(
            @PathVariable Long bookId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ratingService.getBookRatings(bookId, pageable));
    }

    @GetMapping("/book/{bookId}/my-rating")
    public ResponseEntity<RatingDto> getMyRating(
            @PathVariable Long bookId,
            Authentication authentication) {
        return ResponseEntity.ok(ratingService.getUserRating(bookId, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<RatingDto> rateBook(
            @Valid @RequestBody RatingCreateUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ratingService.rateBook(request, authentication.getName()));
    }

    @DeleteMapping("/book/{bookId}")
    public ResponseEntity<Void> deleteMyRating(
            @PathVariable Long bookId,
            Authentication authentication) {
        ratingService.deleteRatingByBook(bookId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRatingById(
            @PathVariable Long id,
            Authentication authentication) {
        ratingService.deleteRatingById(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
