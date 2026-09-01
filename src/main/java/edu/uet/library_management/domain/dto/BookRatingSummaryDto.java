package edu.uet.library_management.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRatingSummaryDto {
    private Long bookId;
    private double averageRating;
    private long totalRatings;
    private Map<Integer, Long> distribution;
    private RatingDto currentUserRating;
}
