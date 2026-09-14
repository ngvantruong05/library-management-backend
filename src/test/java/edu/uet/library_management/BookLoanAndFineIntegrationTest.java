package edu.uet.library_management;

import tools.jackson.databind.ObjectMapper;
import edu.uet.library_management.domain.dto.BookLoanCreateRequest;
import edu.uet.library_management.domain.enums.FineStatus;
import edu.uet.library_management.domain.enums.LoanStatus;
import edu.uet.library_management.domain.enums.LoanType;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.BookCopy;
import edu.uet.library_management.domain.model.BookLoan;
import edu.uet.library_management.domain.model.Fine;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.infrastructure.persistence.BookCopyRepository;
import edu.uet.library_management.infrastructure.persistence.BookLoanRepository;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.FineRepository;
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
public class BookLoanAndFineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private BookLoanRepository bookLoanRepository;

    @Autowired
    private FineRepository fineRepository;

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
    private Book testBook;
    private BookLoan testLoan;
    private Fine testFine;

    @BeforeEach
    void setUp() {
        fineRepository.deleteAll();
        bookLoanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
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

        testBook = bookRepository.save(Book.builder()
                .title("Effective Java")
                .isbn("9780134685991")
                .price(150.0)
                .activated(true)
                .build());

        bookCopyRepository.save(BookCopy.builder()
                .book(testBook)
                .totalCopies(5)
                .availableCopies(5)
                .build());

        testLoan = bookLoanRepository.save(BookLoan.builder()
                .user(normalUser)
                .book(testBook)
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .status(LoanStatus.BORROWED)
                .type(LoanType.ONLINE)
                .numCopies(1)
                .valid(true)
                .build());

        testFine = fineRepository.save(Fine.builder()
                .bookLoan(testLoan)
                .fineAmount(10000.0)
                .overdueDays(2)
                .status(FineStatus.UNPAID)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    void testCreateOnlineLoan_AsUser_ReturnsCreated() throws Exception {
        BookLoanCreateRequest request = BookLoanCreateRequest.builder()
                .bookId(testBook.getId())
                .type(LoanType.ONLINE)
                .numCopies(1)
                .build();

        mockMvc.perform(post("/api/book-loans")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("ONLINE")))
                .andExpect(jsonPath("$.status", is("BORROWED")));
    }

    @Test
    void testCreateLoan_AsUserSpecifyingOtherUserId_ReturnsForbidden() throws Exception {
        BookLoanCreateRequest request = BookLoanCreateRequest.builder()
                .userId(adminUser.getId())
                .bookId(testBook.getId())
                .type(LoanType.ONLINE)
                .build();

        mockMvc.perform(post("/api/book-loans")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetMyLoans_AsUser_ReturnsLoans() throws Exception {
        mockMvc.perform(get("/api/book-loans/my-loans")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(testLoan.getId().intValue())));
    }

    @Test
    void testGetAllLoans_AsAdmin_ReturnsAll() throws Exception {
        mockMvc.perform(get("/api/book-loans")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testGetAllLoans_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/book-loans")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReturnOnlineBook_AsUser_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/book-loans/" + testLoan.getId() + "/return")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RETURNED")));
    }

    @Test
    void testGetMyFines_AsUser_ReturnsFines() throws Exception {
        mockMvc.perform(get("/api/fines/my-fines")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fineAmount", is(10000.0)));
    }

    @Test
    void testSubmitPayment_AsUser_ReturnsPending() throws Exception {
        mockMvc.perform(post("/api/fines/" + testFine.getId() + "/submit-payment")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")));

        Fine updatedFine = fineRepository.findById(testFine.getId()).orElseThrow();
        assertEquals(FineStatus.PENDING, updatedFine.getStatus());
    }

    @Test
    void testGetAllFines_AsAdmin_ReturnsFines() throws Exception {
        mockMvc.perform(get("/api/fines")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testPayFine_AsAdmin_ReturnsPaid() throws Exception {
        mockMvc.perform(post("/api/fines/" + testFine.getId() + "/pay")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));

        Fine updatedFine = fineRepository.findById(testFine.getId()).orElseThrow();
        assertEquals(FineStatus.PAID, updatedFine.getStatus());
    }
}
