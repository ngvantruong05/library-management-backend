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
public class CommentDto {
    private Long id;
    private Long bookId;
    private Long userId;
    private String userDisplayName;
    private String userEmail;
    private String userPhotoUrl;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
