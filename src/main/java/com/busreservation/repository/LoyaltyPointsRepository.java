package com.busreservation.repository;

import com.busreservation.model.LoyaltyPoints;
import com.busreservation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, Long> {
    List<LoyaltyPoints> findByUserOrderByEarnedAtDesc(User user);

    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoints lp WHERE lp.user = :user")
    int getTotalPointsByUser(User user);

    List<LoyaltyPoints> findTop10ByUserOrderByEarnedAtDesc(User user);

    LoyaltyPoints findByBooking(com.busreservation.model.Booking booking);
}
