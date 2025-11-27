package com.busreservation.service;

import com.busreservation.model.Notification;
import com.busreservation.model.User;
import com.busreservation.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Create and save a new notification
     */
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification(user, title, message, type);
        return notificationRepository.save(notification);
    }

    /**
     * Save a notification directly
     */
    public Notification saveNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
    }

    /**
     * Get read notifications for a user
     */
    public List<Notification> getReadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, true);
    }

    /**
     * Get count of unread notifications
     */
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    /**
     * Get recent notifications (top 5)
     */
    public List<Notification> getRecentNotifications(User user) {
        return notificationRepository.findTop5ByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Mark a notification as read
     */
    public void markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = getUnreadNotifications(user);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Delete a notification
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Delete all notifications for a user
     */
    public void deleteAllUserNotifications(User user) {
        notificationRepository.deleteByUser(user);
    }

    /**
     * Get a specific notification by ID
     */
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    // Utility methods for creating specific types of notifications

    public void notifyBookingConfirmed(User user, String bookingDetails) {
        createNotification(user,
                "Booking Confirmed",
                "Your booking has been confirmed. " + bookingDetails,
                "SUCCESS");
    }

    public void notifyBookingCancelled(User user, String bookingDetails) {
        createNotification(user,
                "Booking Cancelled",
                "Your booking has been cancelled. " + bookingDetails,
                "WARNING");
    }

    public void notifyPaymentReceived(User user, String paymentDetails) {
        createNotification(user,
                "Payment Received",
                "Your payment has been received. " + paymentDetails,
                "SUCCESS");
    }

    public void notifyScheduleChange(User user, String scheduleDetails) {
        createNotification(user,
                "Schedule Change",
                "There has been a change in your bus schedule. " + scheduleDetails,
                "WARNING");
    }

    public void notifyGeneralInfo(User user, String title, String message) {
        createNotification(user, title, message, "INFO");
    }

    /**
     * Get all notifications (for admin)
     */
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
}