package edu.uet.library_management;

import tools.jackson.databind.ObjectMapper;
import edu.uet.library_management.domain.dto.UserUpdateRequest;
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
import edu.uet.library_management.interfaces.rest.AuthController.PasswordUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserAndAuthIntegrationTest {

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
    private Book testBook;
    private BookLoan offlineLoan;
    private Fine testFine;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Xoá dữ liệu cũ theo đúng thứ tự ràng buộc khóa ngoại
        fineRepository.deleteAll();
        bookLoanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Tạo và lưu user admin
        adminUser = User.builder()
                .email("admin@library.com")
                .password(passwordEncoder.encode("admin123"))
                .displayName("System Admin")
                .role(Role.ADMIN)
                .disabled(false)
                .build();
        userRepository.save(adminUser);

        // 2. Tạo và lưu user thường
        normalUser = User.builder()
                .email("user@library.com")
                .password(passwordEncoder.encode("user123"))
                .displayName("Normal User")
                .role(Role.USER)
                .disabled(false)
                .build();
        userRepository.save(normalUser);

        // 3. Tạo Sách và Bản sao sách
        testBook = Book.builder()
                .title("Clean Code")
                .isbn("9780132350884")
                .description("A Handbook of Agile Software Craftsmanship")
                .publishedDate("2008")
                .pageCount(464)
                .price(100.0)
                .discountPrice(90.0)
                .thumbnail("http://example.com/thumbnail.jpg")
                .language("en")
                .pdfLink("http://example.com/pdf")
                .currencyCode("VND")
                .activated(true)
                .build();
        bookRepository.save(testBook);

        BookCopy bookCopy = BookCopy.builder()
                .book(testBook)
                .totalCopies(5)
                .availableCopies(4)
                .build();
        bookCopyRepository.save(bookCopy);

        // 4. Tạo khoản mượn offline của user thường
        offlineLoan = BookLoan.builder()
                .user(normalUser)
                .book(testBook)
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .status(LoanStatus.BORROWED)
                .type(LoanType.OFFLINE)
                .numCopies(1)
                .build();
        bookLoanRepository.save(offlineLoan);

        // 5. Tạo phí phạt cho khoản mượn
        testFine = Fine.builder()
                .bookLoan(offlineLoan)
                .fineAmount(15000.0)
                .overdueDays(3)
                .status(FineStatus.UNPAID)
                .createdAt(LocalDateTime.now())
                .build();
        fineRepository.save(testFine);

        // 6. Tạo JWT Tokens
        adminToken = jwtService.generateAccessToken(adminUser);
        userToken = jwtService.generateAccessToken(normalUser);
    }

    // --- TEST PHÂN QUYỀN ADMIN (ADMIN AUTHORIZATION) ---

    @Test
    void testGetAllUsers_AsAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetAllUsers_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReturnOfflineBook_AsAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/book-loans/" + offlineLoan.getId() + "/return")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void testReturnOfflineBook_AsUser_ReturnsForbidden() throws Exception {
        // User thường tự gửi request trả sách offline của chính mình
        mockMvc.perform(post("/api/book-loans/" + offlineLoan.getId() + "/return")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPayFine_AsAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/fines/" + testFine.getId() + "/pay")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void testPayFine_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/fines/" + testFine.getId() + "/pay")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // --- TEST THÔNG TIN CÁ NHÂN (USER ME ENDPOINTS) ---

    @Test
    void testUpdateProfile_Success() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setDisplayName("Updated Normal User");
        updateRequest.setPhoneNumber("+84987654321");
        updateRequest.setBirthday("1995-10-15");

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Normal User"))
                .andExpect(jsonPath("$.phoneNumber").value("+84987654321"));

        // Kiểm tra xem database có lưu đúng không
        User updated = userRepository.findByEmail(normalUser.getEmail()).orElseThrow();
        assertEquals("Updated Normal User", updated.getDisplayName());
        assertEquals("+84987654321", updated.getPhoneNumber());
    }

    @Test
    void testUpdateProfile_Unauthenticated_ReturnsUnauthorized() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setDisplayName("Should Fail");

        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdatePassword_Success() throws Exception {
        PasswordUpdateRequest passwordRequest = new PasswordUpdateRequest();
        passwordRequest.setOldPassword("user123");
        passwordRequest.setNewPassword("newPass123!");

        mockMvc.perform(put("/api/auth/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật mật khẩu thành công"));

        // Xác nhận mật khẩu mới đã đổi và khớp Bcrypt
        User updated = userRepository.findByEmail(normalUser.getEmail()).orElseThrow();
        assertTrue(passwordEncoder.matches("newPass123!", updated.getPassword()));
    }

    @Test
    void testUpdatePassword_WrongOldPassword_ReturnsBadRequest() throws Exception {
        PasswordUpdateRequest passwordRequest = new PasswordUpdateRequest();
        passwordRequest.setOldPassword("wrong_old_pass");
        passwordRequest.setNewPassword("newPass123!");

        mockMvc.perform(put("/api/auth/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu cũ không chính xác"));
    }
}
