package edu.uet.library_management.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateRequest {
    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotBlank(message = "Comment content cannot be empty")
    private String content;
}
