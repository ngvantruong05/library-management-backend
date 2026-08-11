package edu.uet.library_management.infrastructure.config;

import edu.uet.library_management.domain.enums.FineStatus;
import edu.uet.library_management.domain.enums.LoanStatus;
import edu.uet.library_management.domain.enums.LoanType;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.*;
import edu.uet.library_management.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;
    private final BookLoanRepository bookLoanRepository;
    private final FineRepository fineRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${google.books.api.key:}")
    private String googleBooksApiKey;

    @Override
    public void run(String... args) throws Exception {
        File isbnFile = new File("src/main/resources/import_isbns.txt");
        if (!isbnFile.exists() || isbnFile.length() == 0) {
            return;
        }

        log.info("Found import_isbns.txt. Starting bulk book import...");

        try {
            // 1. Read ISBN lines and fetch
            List<String> rawIsbns = Files.readAllLines(isbnFile.toPath());
            int importedCount = 0;
            for (String isbn : rawIsbns) {
                isbn = isbn.trim();
                if (!isbn.isEmpty()) {
                    importBookFromGoogle(isbn, googleBooksApiKey);
                    importedCount++;
                }
            }

            // 2. Clear the contents of the file instead of deleting it
            Files.write(isbnFile.toPath(), new byte[0]);
            log.info("Successfully cleared src/main/resources/import_isbns.txt contents.");

            // Also clear target build copy if present
            File targetFile = new File("target/classes/import_isbns.txt");
            if (targetFile.exists()) {
                Files.write(targetFile.toPath(), new byte[0]);
                log.info("Successfully cleared target/classes/import_isbns.txt contents.");
            }

            log.info("Successfully processed and imported {} ISBN entries!", importedCount);

        } catch (Exception e) {
            log.error("An error occurred during bulk book import: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void importBookFromGoogle(String isbn, String apiKey) {
        try {
            java.util.Optional<Book> existingBookOpt = bookRepository.findByIsbn(isbn);
            if (existingBookOpt.isPresent()) {
                Book existingBook = existingBookOpt.get();
                BookCopy copy = bookCopyRepository.findByBookId(existingBook.getId()).orElse(null);
                if (copy != null) {
                    copy.setTotalCopies(copy.getTotalCopies() + 1);
                    copy.setAvailableCopies(copy.getAvailableCopies() + 1);
                    bookCopyRepository.save(copy);
                    log.info("Book with ISBN {} already exists. Incremented copy count to: total={}, available={}",
                            isbn, copy.getTotalCopies(), copy.getAvailableCopies());
                } else {
                    BookCopy newCopy = BookCopy.builder()
                            .book(existingBook)
                            .totalCopies(1)
                            .availableCopies(1)
                            .build();
                    bookCopyRepository.save(newCopy);
                    log.info("Book with ISBN {} already exists, but BookCopy was missing. Created new BookCopy with 1 copy.", isbn);
                }
                return;
            }

            String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                url += "&key=" + apiKey;
            }

            log.info("Calling Google Books API for ISBN {}: {}", isbn, url);
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("items")) {
                log.warn("No book found on Google Books for ISBN: {}", isbn);
                return;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null || items.isEmpty()) {
                log.warn("No book items list found for ISBN: {}", isbn);
                return;
            }

            Map<String, Object> volumeInfo = (Map<String, Object>) items.get(0).get("volumeInfo");
            if (volumeInfo == null) {
                log.warn("volumeInfo field is missing for ISBN: {}", isbn);
                return;
            }

            String title = (String) volumeInfo.get("title");
            if (title == null || title.trim().isEmpty()) {
                title = "Untitled Book (" + isbn + ")";
            }

            String description = (String) volumeInfo.get("description");
            String publishedDate = (String) volumeInfo.get("publishedDate");

            Integer pageCount = 0;
            Object pageCountObj = volumeInfo.get("pageCount");
            if (pageCountObj instanceof Number) {
                pageCount = ((Number) pageCountObj).intValue();
            }

            String language = (String) volumeInfo.get("language");
            if (language == null) {
                language = "English";
            }

            // Thumbnail URL
            String thumbnail = "";
            if (volumeInfo.containsKey("imageLinks")) {
                Map<String, String> imageLinks = (Map<String, String>) volumeInfo.get("imageLinks");
                if (imageLinks != null) {
                    thumbnail = imageLinks.get("thumbnail");
                }
            }

            // Publisher
            String publisherName = (String) volumeInfo.get("publisher");
            if (publisherName == null || publisherName.trim().isEmpty()) {
                publisherName = "Unknown Publisher";
            }
            Publisher publisher = getOrCreatePublisher(publisherName, "Unknown Address", "Unknown Phone");

            // Authors
            Set<Author> authors = new HashSet<>();
            if (volumeInfo.containsKey("authors")) {
                List<String> authorNames = (List<String>) volumeInfo.get("authors");
                if (authorNames != null) {
                    for (String name : authorNames) {
                        authors.add(getOrCreateAuthor(name, "Google Books Author"));
                    }
                }
            }
            if (authors.isEmpty()) {
                authors.add(getOrCreateAuthor("Unknown Author", "Unknown Author"));
            }

            // Categories
            Set<Category> categories = new HashSet<>();
            if (volumeInfo.containsKey("categories")) {
                List<String> catNames = (List<String>) volumeInfo.get("categories");
                if (catNames != null) {
                    for (String name : catNames) {
                        categories.add(getOrCreateCategory(name));
                    }
                }
            }
            if (categories.isEmpty()) {
                categories.add(getOrCreateCategory("General"));
            }

            // Extract PDF Link or Web Reader Link from accessInfo
            String pdfLink = "";
            Map<String, Object> firstItem = items.get(0);
            if (firstItem != null && firstItem.containsKey("accessInfo")) {
                Map<String, Object> accessInfo = (Map<String, Object>) firstItem.get("accessInfo");
                if (accessInfo != null) {
                    if (accessInfo.containsKey("pdf")) {
                        Map<String, Object> pdfInfo = (Map<String, Object>) accessInfo.get("pdf");
                        if (pdfInfo != null && Boolean.TRUE.equals(pdfInfo.get("isAvailable"))) {
                            pdfLink = (String) pdfInfo.get("downloadLink");
                        }
                    }
                    if ((pdfLink == null || pdfLink.isEmpty()) && accessInfo.containsKey("webReaderLink")) {
                        pdfLink = (String) accessInfo.get("webReaderLink");
                    }
                }
            }
            if (pdfLink == null) {
                pdfLink = "";
            }

            Book book = Book.builder()
                    .title(title)
                    .isbn(isbn)
                    .description(description)
                    .publishedDate(publishedDate)
                    .pageCount(pageCount)
                    .price(150000.0) // Default price
                    .discountPrice(120000.0)
                    .thumbnail(thumbnail)
                    .language(language)
                    .pdfLink(pdfLink)
                    .currencyCode("VND")
                    .publisher(publisher)
                    .authors(authors)
                    .categories(categories)
                    .build();

            Book savedBook = bookRepository.save(book);
            log.info("Saved imported book: '{}'", title);

            // Generate a random number of copies between 5 and 10 inclusive
            int copies = 5 + (int) (Math.random() * 6);
            BookCopy copy = BookCopy.builder()
                    .book(savedBook)
                    .totalCopies(copies)
                    .availableCopies(copies)
                    .build();
            bookCopyRepository.save(copy);
            log.info("Assigned {} physical copies to book '{}'", copies, title);

        } catch (Exception e) {
            log.error("Failed to import book with ISBN {}: {}", isbn, e.getMessage(), e);
        }
    }

    private Category getOrCreateCategory(String name) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).build()));
    }

    private Author getOrCreateAuthor(String name, String description) {
        return authorRepository.findByName(name)
                .orElseGet(() -> authorRepository.save(Author.builder().name(name).description(description).build()));
    }

    private Publisher getOrCreatePublisher(String name, String address, String phoneNumber) {
        return publisherRepository.findByName(name)
                .orElseGet(() -> publisherRepository.save(Publisher.builder()
                        .name(name)
                        .address(address)
                        .phoneNumber(phoneNumber)
                        .build()));
    }

}
