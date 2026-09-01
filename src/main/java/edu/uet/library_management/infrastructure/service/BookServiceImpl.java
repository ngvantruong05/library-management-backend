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
import edu.uet.library_management.infrastructure.persistence.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final RatingRepository ratingRepository;

    @Override
    public List<BookDto> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        List<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toList());
        Map<Long, RatingStat> statsMap = loadRatingStatsForBooks(bookIds);
        return books.stream()
                .map(b -> toDtoWithStats(b, statsMap))
                .collect(Collectors.toList());
    }

    @Override
    public Page<BookDto> getBooksPaginated(int page, int size, String query, Long categoryId, String sortBy, String sortDir) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        Page<Book> bookPage;

        if ("rating".equalsIgnoreCase(sortBy) || "averageRating".equalsIgnoreCase(sortBy)) {
            Pageable pageable = PageRequest.of(page, size);
            if ("asc".equalsIgnoreCase(sortDir)) {
                bookPage = bookRepository.searchBooksOrderByRatingAsc(cleanQuery, categoryId, pageable);
            } else {
                bookPage = bookRepository.searchBooksOrderByRatingDesc(cleanQuery, categoryId, pageable);
            }
        } else {
            String property = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "id";
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
            bookPage = bookRepository.searchBooksPaginated(cleanQuery, categoryId, pageable);
        }

        List<Long> bookIds = bookPage.getContent().stream().map(Book::getId).collect(Collectors.toList());
        Map<Long, RatingStat> statsMap = loadRatingStatsForBooks(bookIds);
        return bookPage.map(b -> toDtoWithStats(b, statsMap));
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
        Double avg = ratingRepository.getAverageRatingByBookId(book.getId());
        double averageRating = 0.0;
        if (avg != null) {
            averageRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
        }
        Long count = ratingRepository.countByBookId(book.getId());
        long ratingCount = count != null ? count : 0L;
        return toDto(book, averageRating, ratingCount);
    }

    private BookDto toDtoWithStats(Book book, Map<Long, RatingStat> statsMap) {
        RatingStat stat = statsMap != null ? statsMap.get(book.getId()) : null;
        double avg = stat != null ? stat.average : 0.0;
        long count = stat != null ? stat.count : 0L;
        return toDto(book, avg, count);
    }

    private Map<Long, RatingStat> loadRatingStatsForBooks(Collection<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = ratingRepository.getRatingStatsForBookIds(bookIds);
        Map<Long, RatingStat> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 3 && row[0] != null) {
                Long bId = ((Number) row[0]).longValue();
                Double avg = row[1] != null
                        ? BigDecimal.valueOf(((Number) row[1]).doubleValue()).setScale(1, RoundingMode.HALF_UP).doubleValue()
                        : 0.0;
                Long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                map.put(bId, new RatingStat(avg, count));
            }
        }
        return map;
    }

    private BookDto toDto(Book book, double averageRating, long ratingCount) {
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
                .averageRating(averageRating)
                .ratingCount(ratingCount)
                .build();
    }

    private static class RatingStat {
        final double average;
        final long count;

        RatingStat(double average, long count) {
            this.average = average;
            this.count = count;
        }
    }
}
