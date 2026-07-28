package edu.uet.library_management.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    private String description;

    private String publishedDate;

    private int pageCount;

    private double price;

    private double discountPrice;

    private String thumbnail;

    private String language;

    private String pdfLink;

    private String currencyCode;

    @NotNull(message = "Publisher ID is required")
    private Long publisherId;

    private Set<Long> authorIds;

    private Set<Long> categoryIds;
}
