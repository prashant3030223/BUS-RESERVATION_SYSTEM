package com.busreservation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // Naya import

import com.busreservation.model.Booking;
import com.busreservation.model.Schedule;
import com.busreservation.model.User;
import com.busreservation.model.LoyaltyPoints;
import com.busreservation.repository.BookingRepository;
import com.busreservation.repository.LoyaltyPointsRepository;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private LoyaltyPointsRepository loyaltyPointsRepository;

    @Override
    public Booking saveBooking(Booking booking) {
        Booking savedBooking = bookingRepository.save(booking);

        // Award Loyalty Points if Confirmed and not already awarded
        if ("CONFIRMED".equalsIgnoreCase(savedBooking.getStatus())) {
            // Check if points already awarded for this booking
            LoyaltyPoints existingPoints = loyaltyPointsRepository.findByBooking(savedBooking);
            if (existingPoints == null) {
                // Award points using LoyaltyService
                loyaltyService.awardBookingPoints(savedBooking.getUser(), savedBooking);
            }
        }

        return savedBooking;
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
        return bookingRepository.findTop5ByOrderByBookingDateDesc();
    }

    @Override
    public List<Booking> findRecentBookings(int limit) {
        List<Booking> allBookings = bookingRepository.findAll();
        return allBookings.stream()
                .sorted((b1, b2) -> b2.getBookingDate().compareTo(b1.getBookingDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findBookingsBySchedule(Schedule schedule) {
        return bookingRepository.findBySchedule(schedule);
    }

    @Override
    public List<Booking> findAllBookings() {
        return bookingRepository.findAll();
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

    @Override
    public Map<String, Double> getDailyRevenueForWeek() {
        Map<String, Double> dailyRevenue = new LinkedHashMap<>();
        List<Booking> allBookings = bookingRepository.findAll();

        // Get bookings from the last 7 days
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] days = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };

        // Initialize all days with 0
        for (String day : days) {
            dailyRevenue.put(day, 0.0);
        }

        // Calculate revenue for each day of the current week
        for (Booking booking : allBookings) {
            if (booking.getBookingDate() != null && "CONFIRMED".equals(booking.getStatus())) {
                java.time.LocalDate bookingDate = booking.getBookingDate();
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(bookingDate, today);

                // Only include bookings from the last 7 days
                if (daysBetween >= 0 && daysBetween < 7) {
                    java.time.DayOfWeek dayOfWeek = bookingDate.getDayOfWeek();
                    String dayName = dayOfWeek.toString().substring(0, 3);
                    dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase();

                    Double currentRevenue = dailyRevenue.getOrDefault(dayName, 0.0);
                    dailyRevenue.put(dayName, currentRevenue + booking.getTotalPrice());
                }
            }
        }

        return dailyRevenue;
    }
}