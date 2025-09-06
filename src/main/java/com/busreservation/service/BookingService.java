package com.busreservation.service;

import com.busreservation.model.Booking;
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
    Map<String, Long> getBookingsPerDay();
}