package edu.uet.library_management;

import tools.jackson.databind.ObjectMapper;
import edu.uet.library_management.domain.dto.CommentCreateRequest;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.Comment;
import edu.uet.library_management.domain.model.Favorite;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.CommentRepository;
import edu.uet.library_management.infrastructure.persistence.FavoriteRepository;
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
public class CommentAndFavoriteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User normalUser;
    private String userToken;
    private Book testBook;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        favoriteRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        normalUser = userRepository.save(User.builder()
                .email("user@test.com")
                .password(passwordEncoder.encode("user123"))
                .displayName("Normal User")
                .role(Role.USER)
                .disabled(false)
                .build());

        userToken = jwtService.generateAccessToken(normalUser);

        testBook = bookRepository.save(Book.builder()
                .title("Refactoring")
                .isbn("9780201485677")
                .price(110.0)
                .activated(true)
                .build());

        testComment = commentRepository.save(Comment.builder()
                .user(normalUser)
                .book(testBook)
                .content("Great book on code smell!")
                .build());
    }

    // --- COMMENT TESTS ---

    @Test
    void testGetBookComments_PublicAccess() throws Exception {
        mockMvc.perform(get("/api/comments/book/" + testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is("Great book on code smell!")));
    }

    @Test
    void testAddComment_AsUser_ReturnsOk() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBookId(testBook.getId());
        request.setContent("Another insightful comment.");

        mockMvc.perform(post("/api/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("Another insightful comment.")));

        assertEquals(2, commentRepository.count());
    }

    @Test
    void testUpdateComment_AsAuthor_ReturnsOk() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBookId(testBook.getId());
        request.setContent("Updated comment content.");

        mockMvc.perform(put("/api/comments/" + testComment.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("Updated comment content.")));
    }

    @Test
    void testDeleteComment_AsAuthor_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/comments/" + testComment.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        assertEquals(0, commentRepository.count());
    }

    // --- FAVORITE TESTS ---

    @Test
    void testAddFavorite_AsUser_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/favorites/" + testBook.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        assertEquals(1, favoriteRepository.count());
    }

    @Test
    void testCheckFavorite_AsUser_ReturnsTrue() throws Exception {
        favoriteRepository.save(Favorite.builder()
                .user(normalUser)
                .book(testBook)
                .build());

        mockMvc.perform(get("/api/favorites/check/" + testBook.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite", is(true)));
    }

    @Test
    void testGetFavorites_AsUser_ReturnsFavoriteBooks() throws Exception {
        favoriteRepository.save(Favorite.builder()
                .user(normalUser)
                .book(testBook)
                .build());

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Refactoring")));
    }

    @Test
    void testRemoveFavorite_AsUser_ReturnsOk() throws Exception {
        favoriteRepository.save(Favorite.builder()
                .user(normalUser)
                .book(testBook)
                .build());

        mockMvc.perform(delete("/api/favorites/" + testBook.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        assertEquals(0, favoriteRepository.count());
    }
}
