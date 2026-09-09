package edu.uet.library_management;

import tools.jackson.databind.ObjectMapper;
import edu.uet.library_management.domain.dto.RatingCreateUpdateRequest;
import edu.uet.library_management.domain.enums.LoanStatus;
import edu.uet.library_management.domain.enums.LoanType;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.BookLoan;
import edu.uet.library_management.domain.model.Rating;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.infrastructure.persistence.BookLoanRepository;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.RatingRepository;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RatingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private BookLoanRepository bookLoanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser1;
    private User testUser2;
    private String tokenUser1;
    private String tokenUser2;
    private Book testBook;

    @BeforeEach
    public void setup() {
        ratingRepository.deleteAll();
        bookLoanRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        testUser1 = userRepository.save(User.builder()
                .email("user1@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Reader One")
                .role(Role.USER)
                .build());

        testUser2 = userRepository.save(User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Reader Two")
                .role(Role.USER)
                .build());

        tokenUser1 = jwtService.generateAccessToken(testUser1);
        tokenUser2 = jwtService.generateAccessToken(testUser2);

        testBook = bookRepository.save(Book.builder()
                .title("Clean Architecture")
                .isbn("978-0134494166")
                .description("A Craftsman's Guide to Software Structure and Design")
                .price(200000)
                .discountPrice(180000)
                .pageCount(432)
                .language("English")
                .build());
    }

    @Test
    public void testPublicAccessRatingSummary_ReturnsZeroWhenNoRatings() throws Exception {
        mockMvc.perform(get("/api/ratings/book/" + testBook.getId() + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", is(0.0)))
                .andExpect(jsonPath("$.totalRatings", is(0)))
                .andExpect(jsonPath("$.distribution.5", is(0)));
    }

    @Test
    public void testRateBook_UnauthorizedWithoutToken() throws Exception {
        RatingCreateUpdateRequest request = RatingCreateUpdateRequest.builder()
                .bookId(testBook.getId())
                .score(5)
                .review("Masterpiece!")
                .build();

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRateBook_ScoreValidation() throws Exception {
        RatingCreateUpdateRequest invalidLow = RatingCreateUpdateRequest.builder()
                .bookId(testBook.getId())
                .score(0)
                .build();

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLow)))
                .andExpect(status().isBadRequest());

        RatingCreateUpdateRequest invalidHigh = RatingCreateUpdateRequest.builder()
                .bookId(testBook.getId())
                .score(6)
                .build();

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidHigh)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRateBook_SuccessAndUpsert() throws Exception {
        // 1. User 1 submits 5 stars
        RatingCreateUpdateRequest request = RatingCreateUpdateRequest.builder()
                .bookId(testBook.getId())
                .score(5)
                .review("Excellent architectural patterns!")
                .build();

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(5)))
                .andExpect(jsonPath("$.review", is("Excellent architectural patterns!")))
                .andExpect(jsonPath("$.verifiedBorrower", is(false)));

        assertEquals(1, ratingRepository.count());

        // 2. User 1 updates rating to 4 stars (Upsert)
        RatingCreateUpdateRequest updateRequest = RatingCreateUpdateRequest.builder()
                .bookId(testBook.getId())
                .score(4)
                .review("Updated: Good but a bit repetitive.")
                .build();

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(4)))
                .andExpect(jsonPath("$.review", is("Updated: Good but a bit repetitive.")));

        // Verify still only 1 rating record exists for this user + book
        assertEquals(1, ratingRepository.count());
    }

    @Test
    public void testRatingSummaryAndDistributionCalculation() throws Exception {
        // User 1 rates 5
        ratingRepository.save(Rating.builder()
                .user(testUser1)
                .book(testBook)
                .score(5)
                .review("5 stars")
                .build());

        // User 2 rates 4
        ratingRepository.save(Rating.builder()
                .user(testUser2)
                .book(testBook)
                .score(4)
                .review("4 stars")
                .build());

        // Check summary: Average should be 4.5, total 2
        mockMvc.perform(get("/api/ratings/book/" + testBook.getId() + "/summary")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", is(4.5)))
                .andExpect(jsonPath("$.totalRatings", is(2)))
                .andExpect(jsonPath("$.distribution.5", is(1)))
                .andExpect(jsonPath("$.distribution.4", is(1)))
                .andExpect(jsonPath("$.distribution.3", is(0)))
                .andExpect(jsonPath("$.currentUserRating.score", is(5)));
    }

    @Test
    public void testVerifiedBorrowerBadge() throws Exception {
        // Create a loan record for user1
        bookLoanRepository.save(BookLoan.builder()
                .user(testUser1)
                .book(testBook)
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .status(LoanStatus.BORROWED)
                .type(LoanType.OFFLINE)
                .numCopies(1)
                .valid(true)
                .build());

        // User 1 rates the book
        RatingCreateUpdateRequest request = RatingCreateUpdateRequest.builder()
                .bookId(testBook.getId())
                .score(5)
                .review("Verified reader review!")
                .build();

        mockMvc.perform(post("/api/ratings")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedBorrower", is(true)));
    }

    @Test
    public void testDeleteRating() throws Exception {
        ratingRepository.save(Rating.builder()
                .user(testUser1)
                .book(testBook)
                .score(5)
                .review("To be deleted")
                .build());

        assertEquals(1, ratingRepository.count());

        mockMvc.perform(delete("/api/ratings/book/" + testBook.getId())
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isNoContent());

        assertEquals(0, ratingRepository.count());
    }
}
