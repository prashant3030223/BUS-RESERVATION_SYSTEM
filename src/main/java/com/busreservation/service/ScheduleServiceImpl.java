package com.busreservation.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.busreservation.exception.ResourceNotFoundException;
import com.busreservation.model.Schedule;
import com.busreservation.repository.ScheduleRepository;
import com.busreservation.repository.LoyaltyPointsRepository;
import com.busreservation.repository.NotificationRepository;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private LoyaltyPointsRepository loyaltyPointsRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    @Override
    public List<Schedule> findAllSchedules() {
        return scheduleRepository.findAll();
    }

    @Override
    public void saveSchedule(Schedule schedule) {
        scheduleRepository.save(schedule);
    }

    @Override
    public Optional<Schedule> getScheduleById(Long id) {
        return scheduleRepository.findById(id);
    }

    @Override
    public Schedule findScheduleById(Long id) {
        return scheduleRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteScheduleById(Long id) {
        if (id != null) {
            scheduleRepository.deleteById(id);
        }
    }

    @Override
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    @Override
    public long countSchedules() {
        return scheduleRepository.count();
    }

    @Override
    public List<Schedule> findSchedulesBySourceAndDestination(String source, String destination, Sort sort) {
        return scheduleRepository.findBySourceAndDestination(source, destination, sort);
    }

    @Override
    public List<Schedule> searchSchedules(String source, String destination, String busType, Double maxPrice,
            Sort sort) {
        List<Schedule> schedules = scheduleRepository.findBySourceAndDestination(source, destination, sort);

        return schedules.stream()
                .filter(s -> {
                    boolean matchesType = true;
                    if (busType != null && !busType.isEmpty() && !"All".equals(busType)) {
                        String dbBusType = s.getBus().getBusType();
                        if (dbBusType == null) {
                            matchesType = false;
                        } else {
                            // Normalize strings for comparison (remove spaces, lowercase)
                            String normalizedFilter = busType.toLowerCase().replace(" ", "");
                            String normalizedDb = dbBusType.toLowerCase().replace(" ", "");
                            matchesType = normalizedDb.contains(normalizedFilter)
                                    || normalizedFilter.contains(normalizedDb);
                        }
                    }

                    boolean matchesPrice = (maxPrice == null || maxPrice <= 0) ? true : s.getPrice() <= maxPrice;
                    return matchesType && matchesPrice;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Schedule> findUpcomingSchedules(int limit) {
        List<Schedule> allSchedules = scheduleRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        return allSchedules.stream()
                .filter(s -> s.getDepartureTime().isAfter(now))
                .sorted((s1, s2) -> s1.getDepartureTime().compareTo(s2.getDepartureTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getBookedSeats(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        if (schedule.getBookings() == null) {
            return new HashSet<>();
        }
        return schedule.getBookings().stream()
                .filter(booking -> "CONFIRMED".equalsIgnoreCase(booking.getStatus()))
                .flatMap(booking -> Arrays.stream(booking.getSeatNumbers().split(",")))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    @Override
    public void updateScheduleStatus(Long scheduleId, String status, int delayMinutes) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        schedule.setStatus(status);
        schedule.setDelayMinutes(delayMinutes);

        // Automated Delay Compensation Logic
        if ("DELAYED".equalsIgnoreCase(status) && delayMinutes >= 60 && !schedule.isCompensationProcessed()) {
            processDelayCompensation(schedule);
        }

        scheduleRepository.save(schedule);
    }

    private void processDelayCompensation(Schedule schedule) {
        List<com.busreservation.model.Booking> bookings = schedule.getBookings();
        if (bookings == null)
            return;

        for (com.busreservation.model.Booking booking : bookings) {
            if ("CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                com.busreservation.model.User user = booking.getUser();

                // Calculate Compensation (e.g., 10% of ticket price as points)
                int points = (int) (schedule.getPrice() * 0.10);
                if (points < 50)
                    points = 50; // Minimum 50 points

                // Credit Loyalty Points
                com.busreservation.model.LoyaltyPoints loyaltyEntry = new com.busreservation.model.LoyaltyPoints(
                        user,
                        points,
                        "COMPENSATION",
                        "Compensation for delay of " + schedule.getDelayMinutes() + " minutes on schedule "
                                + schedule.getSource() + " - " + schedule.getDestination());
                loyaltyEntry.setBooking(booking);
                loyaltyPointsRepository.save(loyaltyEntry);

                // Send Notification
                com.busreservation.model.Notification notification = new com.busreservation.model.Notification(
                        user,
                        "Delay Compensation",
                        "We apologize for the delay. " + points + " loyalty points have been credited to your account.",
                        "INFO");
                notificationRepository.save(notification);
            }
        }
        schedule.setCompensationProcessed(true);
    }
}