package edu.uet.library_management.domain.dto;

import edu.uet.library_management.domain.enums.LoanType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookLoanCreateRequest {
    private Long userId;

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotNull(message = "Loan type (ONLINE/OFFLINE) is required")
    private LoanType type;

    private int numCopies; // only relevant for OFFLINE

    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
}
