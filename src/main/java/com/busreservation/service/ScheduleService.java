package com.busreservation.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Sort;

import com.busreservation.model.Schedule;

public interface ScheduleService {
    List<Schedule> getAllSchedules();

    List<Schedule> findAllSchedules();

    void saveSchedule(Schedule schedule);

    Optional<Schedule> getScheduleById(Long id);

    Schedule findScheduleById(Long id);

    void deleteScheduleById(Long id);

    void deleteSchedule(Long id);

    long countSchedules();

    Set<String> getBookedSeats(Long scheduleId);

    List<Schedule> findSchedulesBySourceAndDestination(String source, String destination, Sort sort);

    List<Schedule> searchSchedules(String source, String destination, String busType, Double maxPrice, Sort sort);

    List<Schedule> findUpcomingSchedules(int limit);

    void updateScheduleStatus(Long scheduleId, String status, int delayMinutes);

}