package com.busreservation.repository;

import com.busreservation.model.Booking;
import com.busreservation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Naya import
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser(User user);

    // Naya Method 1: Confirmed bookings ka total price calculate karne ke liye
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0.0) FROM Booking b WHERE b.status = 'CONFIRMED'")
    Double getTotalRevenue();

    // Naya Method 2: Sabse nayi 5 bookings nikalne ke liye
    List<Booking> findTop5ByOrderByBookingDateDescIdDesc();
    @Query(value = "SELECT CAST(b.booking_date AS CHAR), COUNT(b.id) FROM bookings b WHERE b.booking_date > CURDATE() - INTERVAL 7 DAY GROUP BY b.booking_date ORDER BY b.booking_date", nativeQuery = true)
    List<Object[]> countBookingsPerDay();
}