package edu.uet.library_management.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.uet.library_management.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private Long loanId;
    private String bookTitle;
    private String title;
    private String message;
    private NotificationType type;

    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime createdAt;
}
