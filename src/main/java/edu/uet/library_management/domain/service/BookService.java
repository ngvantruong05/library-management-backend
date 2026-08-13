package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.BookCreateRequest;
import edu.uet.library_management.domain.dto.BookDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BookService {
    List<BookDto> getAllBooks();
    Page<BookDto> getBooksPaginated(int page, int size, String query, Long categoryId, String sortBy, String sortDir);
    BookDto getBookById(Long id);
    List<BookDto> searchBooks(String query);
    BookDto createBook(BookCreateRequest request);
    BookDto updateBook(Long id, BookCreateRequest request);
    void deleteBook(Long id);
}

