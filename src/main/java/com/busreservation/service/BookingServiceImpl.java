package com.busreservation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // Naya import

import com.busreservation.model.Booking;
import com.busreservation.model.User; // Naya import
import com.busreservation.repository.BookingRepository;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Override
    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    @Override
    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUser(user);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public long countBookings() {
        return bookingRepository.count();
    }

    @Override
    public Double getTotalRevenue() {
        Double revenue = bookingRepository.getTotalRevenue();
        return revenue == null ? 0.0 : revenue;
    }

    @Override
    public List<Booking> getRecentBookings() {
        return bookingRepository.findTop5ByOrderByBookingDateDescIdDesc();
    }
    
    @Override
    public Map<String, Long> getBookingsPerDay() {
        List<Object[]> results = bookingRepository.countBookingsPerDay();
        Map<String, Long> bookingsPerDay = new LinkedHashMap<>();
        for (Object[] result : results) {
            String date = (String) result[0];
            Long count = ((Number) result[1]).longValue();
            bookingsPerDay.put(date, count);
        }
        return bookingsPerDay;
    }
}