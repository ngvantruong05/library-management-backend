package edu.uet.library_management.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {
    private Long id;
    private String title;
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
    private boolean activated;
    private LocalDateTime lastUpdated;
    private PublisherDto publisher;
    private Set<AuthorDto> authors;
    private Set<CategoryDto> categories;
}
