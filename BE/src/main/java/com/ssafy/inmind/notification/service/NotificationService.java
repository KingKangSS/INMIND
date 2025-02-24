package com.ssafy.inmind.notification.service;

import com.ssafy.inmind.exception.ErrorCode;
import com.ssafy.inmind.exception.RestApiException;
import com.ssafy.inmind.notification.dto.NotificationDto;
import com.ssafy.inmind.notification.entity.Notification;
import com.ssafy.inmind.notification.entity.NotificationType;
import com.ssafy.inmind.notification.repository.NotificationRepository;
import com.ssafy.inmind.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SseEmitterService sseEmitterService;
    private final NotificationRepository notificationRepository;

    // 알림 스케줄러
    @Scheduled(cron = "0 0,30 * * * *")
    public void eventSchedule() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        List<Notification> notifications = notificationRepository.findByScheduledDateAndScheduledTime(today, currentTime);

        if (notifications.isEmpty()) {
            System.out.println("알림이 없습니다.");
            return;
        }

        for (Notification notification : notifications) {
            long userId = notification.getUser().getId();
            String emitterId = sseEmitterService.makeTimeIncludeId(userId);
            NotificationDto notificationDto = NotificationDto.builder()
                    .id(notification.getId())
                    .message(notification.getMessage())
                    .scheduledDate(notification.getScheduledDate())
                    .scheduledTime(notification.getScheduledTime())
                    .isRead(notification.getIsRead())
                    .notificationType(notification.getType())
                    .createdAt(notification.getCreatedAt())
                    .build();
            sseEmitterService.sendNotification(emitterId, notificationDto);
        }
    }

    public List<NotificationDto> getUnreadNotification(Long userId) {
         return notificationRepository.findByUserIdAndIsRead(userId, "N").stream()
                 .map(notification -> NotificationDto.builder()
                         .id(notification.getId())
                         .message(notification.getMessage())
                         .scheduledDate(notification.getScheduledDate())
                         .scheduledTime(notification.getScheduledTime())
                         .isRead(notification.getIsRead())
                         .notificationType(notification.getType())
                         .createdAt(notification.getCreatedAt())
                         .build())
                 .collect(Collectors.toList());
    }

    public NotificationDto getNotification(Long id) {

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RestApiException(ErrorCode.BAD_REQUEST));

        Notification updateNotification = Notification.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .scheduledDate(notification.getScheduledDate())
                .scheduledTime(notification.getScheduledTime())
                .isRead(notification.getIsRead())
                .type(notification.getType())
                .build();

        notificationRepository.save(updateNotification);

        return NotificationDto.builder()
                .id(updateNotification.getId())
                .message(updateNotification.getMessage())
                .scheduledDate(updateNotification.getScheduledDate())
                .scheduledTime(updateNotification.getScheduledTime())
                .isRead(updateNotification.getIsRead())
                .notificationType(updateNotification.getType())
                .build();

    }

    @Transactional
    public void deleteNotification(Long id) {
        try {
            notificationRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new RestApiException(ErrorCode.BAD_REQUEST);
        }
    }

    @Transactional
    public void updateRead(List<Long> ids) {
        notificationRepository.markAsRead(ids);
    }

    @Transactional
    public void deleteNotifications(List<Long> ids) {
        try {
            notificationRepository.deleteAllById(ids);
        } catch (EmptyResultDataAccessException e) {
            throw new RestApiException(ErrorCode.BAD_REQUEST);
        }
    }
}