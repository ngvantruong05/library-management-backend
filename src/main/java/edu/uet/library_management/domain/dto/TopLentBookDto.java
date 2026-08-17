package edu.uet.library_management.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopLentBookDto {
    private Long bookId;
    private String bookTitle;
    private String bookThumbnail;
    private Long loanCount;
}
