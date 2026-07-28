package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.BookCreateRequest;
import edu.uet.library_management.domain.dto.BookDto;

import java.util.List;

public interface BookService {
    List<BookDto> getAllBooks();
    BookDto getBookById(Long id);
    List<BookDto> searchBooks(String query);
    BookDto createBook(BookCreateRequest request);
    BookDto updateBook(Long id, BookCreateRequest request);
    void deleteBook(Long id);
}
