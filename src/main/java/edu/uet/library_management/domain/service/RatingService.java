package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.BookRatingSummaryDto;
import edu.uet.library_management.domain.dto.RatingCreateUpdateRequest;
import edu.uet.library_management.domain.dto.RatingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RatingService {

    RatingDto rateBook(RatingCreateUpdateRequest request, String userEmail);

    void deleteRatingByBook(Long bookId, String userEmail);

    void deleteRatingById(Long id, String currentUserEmail);

    BookRatingSummaryDto getBookRatingSummary(Long bookId, String currentUserEmail);

    Page<RatingDto> getBookRatings(Long bookId, Pageable pageable);

    RatingDto getUserRating(Long bookId, String userEmail);
}
