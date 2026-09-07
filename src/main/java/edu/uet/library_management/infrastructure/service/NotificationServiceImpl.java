package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.NotificationDto;
import edu.uet.library_management.domain.enums.LoanStatus;
import edu.uet.library_management.domain.enums.NotificationType;
import edu.uet.library_management.domain.model.BookLoan;
import edu.uet.library_management.domain.model.Notification;
import edu.uet.library_management.domain.service.NotificationService;
import edu.uet.library_management.infrastructure.persistence.BookLoanRepository;
import edu.uet.library_management.infrastructure.persistence.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final BookLoanRepository bookLoanRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public List<NotificationDto> getMyNotifications(String email) {
        return notificationRepository.findByUserEmailOrderByIsReadAscCreatedAtDesc(email).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public long getUnreadCount(String email) {
        return notificationRepository.countByUserEmailAndIsReadFalse(email);
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to notification");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(String email) {
        notificationRepository.markAllAsReadForUser(email);
    }

    @Override
    @Transactional
    public int checkAndCreateDueNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        List<BookLoan> activeLoans = bookLoanRepository.findByValid(true);
        int createdCount = 0;

        for (BookLoan loan : activeLoans) {
            if (loan.getStatus() == LoanStatus.RETURNED) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(now.toLocalDate(), loan.getDueDate().toLocalDate());

            if (daysRemaining >= 0 && daysRemaining <= 3) {
                // Check if DUE_SOON notification already sent for this loan in the last 24h
                boolean exists = notificationRepository.existsByBookLoanIdAndTypeAndCreatedAtAfter(
                        loan.getId(), NotificationType.DUE_SOON, twentyFourHoursAgo);

                if (!exists) {
                    String formattedDate = loan.getDueDate().format(DATE_FORMATTER);
                    String title = (daysRemaining == 0)
                            ? "Sách đến hạn trả hôm nay!"
                            : "Sắp đến hạn trả sách (" + daysRemaining + " ngày)";

                    String message = String.format("Sách '%s' của bạn sẽ đến hạn trả vào %s. Vui lòng hoàn trả đúng hạn.",
                            loan.getBook().getTitle(), formattedDate);

                    Notification notification = Notification.builder()
                            .user(loan.getUser())
                            .bookLoan(loan)
                            .title(title)
                            .message(message)
                            .type(NotificationType.DUE_SOON)
                            .isRead(false)
                            .createdAt(now)
                            .build();

                    notificationRepository.save(notification);
                    createdCount++;
                    log.info("[Notification] Generated DUE_SOON notice for loan ID {}, user {}", loan.getId(), loan.getUser().getEmail());
                }
            } else if (daysRemaining < 0 || loan.getStatus() == LoanStatus.OVERDUE) {
                // Check if OVERDUE notification already sent for this loan in the last 24h
                boolean exists = notificationRepository.existsByBookLoanIdAndTypeAndCreatedAtAfter(
                        loan.getId(), NotificationType.OVERDUE, twentyFourHoursAgo);

                if (!exists) {
                    long overdueDays = Math.abs(daysRemaining);
                    String title = "Sách quá hạn trả!";
                    String message = String.format("Sách '%s' đã quá hạn trả %d ngày. Vui lòng trả sách sớm nhất có thể.",
                            loan.getBook().getTitle(), overdueDays);

                    Notification notification = Notification.builder()
                            .user(loan.getUser())
                            .bookLoan(loan)
                            .title(title)
                            .message(message)
                            .type(NotificationType.OVERDUE)
                            .isRead(false)
                            .createdAt(now)
                            .build();

                    notificationRepository.save(notification);
                    createdCount++;
                    log.info("[Notification] Generated OVERDUE notice for loan ID {}, user {}", loan.getId(), loan.getUser().getEmail());
                }
            }
        }

        return createdCount;
    }

    private NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .userEmail(notification.getUser().getEmail())
                .loanId(notification.getBookLoan() != null ? notification.getBookLoan().getId() : null)
                .bookTitle(notification.getBookLoan() != null && notification.getBookLoan().getBook() != null
                        ? notification.getBookLoan().getBook().getTitle() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
