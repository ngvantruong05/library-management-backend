package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.NotificationDto;

import java.util.List;

public interface NotificationService {

    List<NotificationDto> getMyNotifications(String email);

    long getUnreadCount(String email);

    NotificationDto markAsRead(Long notificationId, String email);

    void markAllAsRead(String email);

    int checkAndCreateDueNotifications();
}
