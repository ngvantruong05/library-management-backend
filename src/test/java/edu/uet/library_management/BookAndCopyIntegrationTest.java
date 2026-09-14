package edu.uet.library_management;

import tools.jackson.databind.ObjectMapper;
import edu.uet.library_management.domain.dto.BookCreateRequest;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.Author;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.BookCopy;
import edu.uet.library_management.domain.model.Category;
import edu.uet.library_management.domain.model.Publisher;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.infrastructure.persistence.AuthorRepository;
import edu.uet.library_management.infrastructure.persistence.BookCopyRepository;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.CategoryRepository;
import edu.uet.library_management.infrastructure.persistence.PublisherRepository;
import edu.uet.library_management.infrastructure.persistence.UserRepository;
import edu.uet.library_management.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BookAndCopyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User normalUser;
    private String adminToken;
    private String userToken;
    private Publisher testPublisher;
    private Author testAuthor;
    private Category testCategory;
    private Book testBook;

    @BeforeEach
    void setUp() {
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        authorRepository.deleteAll();
        publisherRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .displayName("Admin User")
                .role(Role.ADMIN)
                .disabled(false)
                .build());

        normalUser = userRepository.save(User.builder()
                .email("user@test.com")
                .password(passwordEncoder.encode("user123"))
                .displayName("Normal User")
                .role(Role.USER)
                .disabled(false)
                .build());

        adminToken = jwtService.generateAccessToken(adminUser);
        userToken = jwtService.generateAccessToken(normalUser);

        testPublisher = publisherRepository.save(Publisher.builder()
                .name("O'Reilly")
                .address("USA")
                .build());

        testAuthor = authorRepository.save(Author.builder()
                .name("Robert C. Martin")
                .description("Uncle Bob")
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Software Engineering")
                .build());

        testBook = bookRepository.save(Book.builder()
                .title("Clean Code")
                .isbn("9780132350884")
                .description("A Handbook of Agile Software Craftsmanship")
                .publisher(testPublisher)
                .price(100.0)
                .activated(true)
                .build());

        bookCopyRepository.save(BookCopy.builder()
                .book(testBook)
                .totalCopies(10)
                .availableCopies(10)
                .build());
    }

    @Test
    void testGetBooksPublicAccess_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Clean Code")));
    }

    @Test
    void testGetBookById_ReturnsBook() throws Exception {
        mockMvc.perform(get("/api/books/" + testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Clean Code")))
                .andExpect(jsonPath("$.isbn", is("9780132350884")));
    }

    @Test
    void testSearchBooks_ReturnsMatchingBooks() throws Exception {
        mockMvc.perform(get("/api/books/search").param("q", "Clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Clean Code")));
    }

    @Test
    void testCreateBook_AsAdmin_ReturnsCreated() throws Exception {
        BookCreateRequest request = BookCreateRequest.builder()
                .title("Design Patterns")
                .isbn("9780201633610")
                .description("Elements of Reusable Object-Oriented Software")
                .publisherId(testPublisher.getId())
                .authorIds(Set.of(testAuthor.getId()))
                .categoryIds(Set.of(testCategory.getId()))
                .price(120.0)
                .build();

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Design Patterns")))
                .andExpect(jsonPath("$.isbn", is("9780201633610")));

        assertEquals(2, bookRepository.count());
    }

    @Test
    void testCreateBook_AsUser_ReturnsForbidden() throws Exception {
        BookCreateRequest request = BookCreateRequest.builder()
                .title("Unauthorized Book")
                .isbn("1111111111")
                .publisherId(testPublisher.getId())
                .build();

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateBook_AsAdmin_ReturnsOk() throws Exception {
        BookCreateRequest request = BookCreateRequest.builder()
                .title("Clean Code - 2nd Edition")
                .isbn("9780132350884")
                .description("Updated Description")
                .publisherId(testPublisher.getId())
                .price(150.0)
                .build();

        mockMvc.perform(put("/api/books/" + testBook.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Clean Code - 2nd Edition")));
    }

    @Test
    void testDeleteBook_AsAdmin_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/books/" + testBook.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Book deletedBook = bookRepository.findById(testBook.getId()).orElse(null);
        assertFalse(deletedBook != null && deletedBook.isActivated());
    }

    @Test
    void testGetBookCopyByBookId_ReturnsCopy() throws Exception {
        mockMvc.perform(get("/api/book-copies/book/" + testBook.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCopies", is(10)))
                .andExpect(jsonPath("$.availableCopies", is(10)));
    }

    @Test
    void testUpdateBookCopy_AsAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(put("/api/book-copies/book/" + testBook.getId())
                        .param("totalCopies", "15")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCopies", is(15)))
                .andExpect(jsonPath("$.availableCopies", is(15)));
    }

    @Test
    void testUpdateBookCopy_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/book-copies/book/" + testBook.getId())
                        .param("totalCopies", "20")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
