package com.busreservation.repository;

import com.busreservation.model.Notification;
import com.busreservation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, Boolean isRead);

    long countByUserAndIsRead(User user, Boolean isRead);

    List<Notification> findTop5ByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
