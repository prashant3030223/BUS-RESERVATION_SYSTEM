package com.busreservation.repository;

import com.busreservation.model.Booking;
import com.busreservation.model.Schedule;
import com.busreservation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Naya import
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser(User user);

    List<Booking> findBySchedule(Schedule schedule);

    // Naya Method 1: Confirmed bookings ka total price calculate karne ke liye
    // Naya Method 1: Confirmed bookings ka total price calculate karne ke liye
    @Query(value = "SELECT COALESCE(SUM(total_price), 0.0) FROM bookings WHERE status = 'CONFIRMED'", nativeQuery = true)
    Double getTotalRevenue();

    // Naya Method 2: Sabse nayi 5 bookings nikalne ke liye
    List<Booking> findTop5ByOrderByBookingDateDesc();

    @Query(value = "SELECT CAST(b.booking_date AS CHAR), COUNT(b.id) FROM bookings b WHERE b.booking_date > CURDATE() - INTERVAL 7 DAY GROUP BY b.booking_date ORDER BY b.booking_date", nativeQuery = true)
    List<Object[]> countBookingsPerDay();
}