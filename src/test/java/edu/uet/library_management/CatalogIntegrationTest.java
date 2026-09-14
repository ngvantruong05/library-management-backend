package edu.uet.library_management;

import tools.jackson.databind.ObjectMapper;
import edu.uet.library_management.domain.dto.AuthorDto;
import edu.uet.library_management.domain.dto.CategoryDto;
import edu.uet.library_management.domain.dto.PublisherDto;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.Author;
import edu.uet.library_management.domain.model.Category;
import edu.uet.library_management.domain.model.Publisher;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.infrastructure.persistence.AuthorRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PublisherRepository publisherRepository;

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
    private Category testCategory;
    private Author testAuthor;
    private Publisher testPublisher;

    @BeforeEach
    void setUp() {
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

        testCategory = categoryRepository.save(Category.builder()
                .name("Computer Science")
                .build());

        testAuthor = authorRepository.save(Author.builder()
                .name("Martin Fowler")
                .description("Software Development Expert")
                .build());

        testPublisher = publisherRepository.save(Publisher.builder()
                .name("Addison-Wesley")
                .address("Boston, MA")
                .phoneNumber("+1-800-555-0199")
                .build());
    }

    // --- CATEGORY TESTS ---

    @Test
    void testGetAllCategories_PublicAccess() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Computer Science")));
    }

    @Test
    void testCreateCategory_AsAdmin_ReturnsCreated() throws Exception {
        CategoryDto dto = new CategoryDto();
        dto.setName("Database Systems");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Database Systems")));

        assertEquals(2, categoryRepository.count());
    }

    @Test
    void testCreateCategory_AsUser_ReturnsForbidden() throws Exception {
        CategoryDto dto = new CategoryDto();
        dto.setName("Unauthorized Category");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // --- AUTHOR TESTS ---

    @Test
    void testGetAllAuthors_AuthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/authors")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Martin Fowler")));
    }

    @Test
    void testCreateAuthor_AsAdmin_ReturnsCreated() throws Exception {
        AuthorDto dto = new AuthorDto();
        dto.setName("Joshua Bloch");
        dto.setDescription("Author of Effective Java");

        mockMvc.perform(post("/api/authors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Joshua Bloch")));

        assertEquals(2, authorRepository.count());
    }

    @Test
    void testDeleteAuthor_AsAdmin_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/authors/" + testAuthor.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertEquals(0, authorRepository.count());
    }

    // --- PUBLISHER TESTS ---

    @Test
    void testGetAllPublishers_AuthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/publishers")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Addison-Wesley")));
    }

    @Test
    void testCreatePublisher_AsAdmin_ReturnsCreated() throws Exception {
        PublisherDto dto = new PublisherDto();
        dto.setName("Manning Publications");
        dto.setAddress("Shelter Island, NY");

        mockMvc.perform(post("/api/publishers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Manning Publications")));

        assertEquals(2, publisherRepository.count());
    }
}
