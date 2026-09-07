package edu.uet.library_management.infrastructure.persistence;

import edu.uet.library_management.domain.enums.NotificationType;
import edu.uet.library_management.domain.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserEmailOrderByIsReadAscCreatedAtDesc(String email);

    long countByUserEmailAndIsReadFalse(String email);

    boolean existsByBookLoanIdAndTypeAndCreatedAtAfter(Long bookLoanId, NotificationType type, LocalDateTime after);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.email = :email AND n.isRead = false")
    void markAllAsReadForUser(@Param("email") String email);
}
