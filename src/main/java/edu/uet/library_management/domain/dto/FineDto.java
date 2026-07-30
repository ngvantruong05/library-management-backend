package edu.uet.library_management.domain.dto;

import edu.uet.library_management.domain.enums.FineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineDto {
    private Long id;
    private Long bookLoanId;
    private String userEmail;
    private String userDisplayName;
    private String bookTitle;
    private double fineAmount;
    private int overdueDays;
    private FineStatus status;
    private LocalDateTime createdAt;
}
