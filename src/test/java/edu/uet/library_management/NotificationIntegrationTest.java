package edu.uet.library_management;

import tools.jackson.databind.ObjectMapper;
import edu.uet.library_management.domain.enums.LoanStatus;
import edu.uet.library_management.domain.enums.LoanType;
import edu.uet.library_management.domain.enums.NotificationType;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.Book;
import edu.uet.library_management.domain.model.BookLoan;
import edu.uet.library_management.domain.model.Notification;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.infrastructure.persistence.BookLoanRepository;
import edu.uet.library_management.infrastructure.persistence.BookRepository;
import edu.uet.library_management.infrastructure.persistence.NotificationRepository;
import edu.uet.library_management.infrastructure.persistence.UserRepository;
import edu.uet.library_management.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookLoanRepository bookLoanRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String userToken;
    private Book testBook;
    private BookLoan dueLoan;

    @BeforeEach
    public void setup() {
        notificationRepository.deleteAll();
        bookLoanRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("notifyuser@example.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("Notification User")
                .role(Role.USER)
                .build());

        userToken = jwtService.generateAccessToken(testUser);

        testBook = bookRepository.save(Book.builder()
                .title("Spring Boot in Action")
                .isbn("978-1617292545")
                .price(150000)
                .build());

        // Loan expiring in 1 day (due date = tomorrow)
        dueLoan = bookLoanRepository.save(BookLoan.builder()
                .user(testUser)
                .book(testBook)
                .borrowDate(LocalDateTime.now().minusDays(13))
                .dueDate(LocalDateTime.now().plusDays(1))
                .status(LoanStatus.BORROWED)
                .type(LoanType.OFFLINE)
                .numCopies(1)
                .valid(true)
                .build());
    }

    @Test
    public void testGetNotifications_EmptyInitially() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(0)));
    }

    @Test
    public void testTriggerScan_GeneratesDueReminderNotification() throws Exception {
        // Trigger due-date scan
        mockMvc.perform(post("/api/notifications/trigger-scan")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount", is(1)));

        // Check user notifications list
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Sắp đến hạn trả sách (1 ngày)")))
                .andExpect(jsonPath("$[0].type", is("DUE_SOON")))
                .andExpect(jsonPath("$[0].isRead", is(false)));

        // Check unread count
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(1)));
    }

    @Test
    public void testMarkAsRead() throws Exception {
        Notification notification = notificationRepository.save(Notification.builder()
                .user(testUser)
                .bookLoan(dueLoan)
                .title("Nhắc nhở trả sách")
                .message("Sách sắp hết hạn trả")
                .type(NotificationType.DUE_SOON)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(put("/api/notifications/" + notification.getId() + "/read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)));

        // Verify unread count becomes 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(0)));
    }

    @Test
    public void testMarkAllAsRead() throws Exception {
        notificationRepository.save(Notification.builder()
                .user(testUser)
                .title("Thông báo 1")
                .message("Nội dung 1")
                .type(NotificationType.SYSTEM)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        notificationRepository.save(Notification.builder()
                .user(testUser)
                .title("Thông báo 2")
                .message("Nội dung 2")
                .type(NotificationType.DUE_SOON)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("All notifications marked as read")));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(0)));
    }
}
