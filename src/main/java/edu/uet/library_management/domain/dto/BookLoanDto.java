package edu.uet.library_management.domain.dto;

import edu.uet.library_management.domain.enums.LoanStatus;
import edu.uet.library_management.domain.enums.LoanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookLoanDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userDisplayName;
    private Long bookId;
    private String bookTitle;
    private String bookThumbnail;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private LoanStatus status;
    private LoanType type;
    private int numCopies;
    private boolean valid;
    private LocalDateTime lastUpdated;
}
