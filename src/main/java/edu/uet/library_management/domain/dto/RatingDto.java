package edu.uet.library_management.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDto {
    private Long id;
    private Long bookId;
    private Long userId;
    private String userEmail;
    private String userDisplayName;
    private String userPhotoUrl;
    private int score;
    private String review;
    private boolean verifiedBorrower;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
