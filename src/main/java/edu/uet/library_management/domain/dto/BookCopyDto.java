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
public class BookCopyDto {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private int totalCopies;
    private int availableCopies;
    private LocalDateTime lastUpdated;
}
