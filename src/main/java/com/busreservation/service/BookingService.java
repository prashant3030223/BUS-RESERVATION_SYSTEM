package com.busreservation.service;

import com.busreservation.model.Booking;
import com.busreservation.model.Schedule;
import com.busreservation.model.User;
import java.util.*;
import java.util.Optional;

public interface BookingService {
    Booking saveBooking(Booking booking);

    Optional<Booking> getBookingById(Long id);

    List<Booking> getBookingsByUser(User user);

    List<Booking> getAllBookings();

    long countBookings();

    Double getTotalRevenue();

    List<Booking> getRecentBookings();

    List<Booking> findRecentBookings(int limit);

    List<Booking> findBookingsBySchedule(Schedule schedule);

    List<Booking> findAllBookings();

    Map<String, Long> getBookingsPerDay();

    Map<String, Double> getDailyRevenueForWeek();
}