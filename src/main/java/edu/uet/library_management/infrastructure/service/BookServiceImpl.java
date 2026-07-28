package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.*;
import edu.uet.library_management.domain.model.Author;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.Category;
import edu.uet.library_management.domain.model.Publisher;
import edu.uet.library_management.domain.service.BookService;
import edu.uet.library_management.infrastructure.persistence.AuthorRepository;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.CategoryRepository;
import edu.uet.library_management.infrastructure.persistence.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BookDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + id));
        return toDto(book);
    }

    @Override
    public List<BookDto> searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllBooks();
        }
        return bookRepository.searchBooks(query.trim()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BookDto createBook(BookCreateRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book already exists with ISBN: " + request.getIsbn());
        }

        Publisher publisher = publisherRepository.findById(request.getPublisherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publisher not found with id: " + request.getPublisherId()));

        Set<Author> authors = new HashSet<>();
        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            for (Long authorId : request.getAuthorIds()) {
                Author author = authorRepository.findById(authorId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found with id: " + authorId));
                authors.add(author);
            }
        }

        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long catId : request.getCategoryIds()) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + catId));
                categories.add(category);
            }
        }

        Book book = Book.builder()
                .title(request.getTitle())
                .isbn(request.getIsbn())
                .description(request.getDescription())
                .publishedDate(request.getPublishedDate())
                .pageCount(request.getPageCount())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .thumbnail(request.getThumbnail())
                .language(request.getLanguage())
                .pdfLink(request.getPdfLink())
                .currencyCode(request.getCurrencyCode())
                .publisher(publisher)
                .authors(authors)
                .categories(categories)
                .build();

        Book savedBook = bookRepository.save(book);
        return toDto(savedBook);
    }

    @Override
    public BookDto updateBook(Long id, BookCreateRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + id));

        if (!book.getIsbn().equalsIgnoreCase(request.getIsbn()) && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book already exists with ISBN: " + request.getIsbn());
        }

        Publisher publisher = publisherRepository.findById(request.getPublisherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publisher not found with id: " + request.getPublisherId()));

        Set<Author> authors = new HashSet<>();
        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            for (Long authorId : request.getAuthorIds()) {
                Author author = authorRepository.findById(authorId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found with id: " + authorId));
                authors.add(author);
            }
        }

        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long catId : request.getCategoryIds()) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + catId));
                categories.add(category);
            }
        }

        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setDescription(request.getDescription());
        book.setPublishedDate(request.getPublishedDate());
        book.setPageCount(request.getPageCount());
        book.setPrice(request.getPrice());
        book.setDiscountPrice(request.getDiscountPrice());
        book.setThumbnail(request.getThumbnail());
        book.setLanguage(request.getLanguage());
        book.setPdfLink(request.getPdfLink());
        book.setCurrencyCode(request.getCurrencyCode());
        book.setPublisher(publisher);
        book.setAuthors(authors);
        book.setCategories(categories);

        Book updatedBook = bookRepository.save(book);
        return toDto(updatedBook);
    }

    @Override
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + id));
        bookRepository.delete(book);
    }

    private BookDto toDto(Book book) {
        PublisherDto pubDto = null;
        if (book.getPublisher() != null) {
            pubDto = PublisherDto.builder()
                    .id(book.getPublisher().getId())
                    .name(book.getPublisher().getName())
                    .address(book.getPublisher().getAddress())
                    .phoneNumber(book.getPublisher().getPhoneNumber())
                    .build();
        }

        Set<AuthorDto> authorDtos = new HashSet<>();
        if (book.getAuthors() != null) {
            authorDtos = book.getAuthors().stream()
                    .map(a -> AuthorDto.builder()
                            .id(a.getId())
                            .name(a.getName())
                            .description(a.getDescription())
                            .build())
                    .collect(Collectors.toSet());
        }

        Set<CategoryDto> categoryDtos = new HashSet<>();
        if (book.getCategories() != null) {
            categoryDtos = book.getCategories().stream()
                    .map(c -> CategoryDto.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .build())
                    .collect(Collectors.toSet());
        }

        return BookDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .publishedDate(book.getPublishedDate())
                .pageCount(book.getPageCount())
                .price(book.getPrice())
                .discountPrice(book.getDiscountPrice())
                .thumbnail(book.getThumbnail())
                .language(book.getLanguage())
                .pdfLink(book.getPdfLink())
                .currencyCode(book.getCurrencyCode())
                .activated(book.isActivated())
                .lastUpdated(book.getLastUpdated())
                .publisher(pubDto)
                .authors(authorDtos)
                .categories(categoryDtos)
                .build();
    }
}
