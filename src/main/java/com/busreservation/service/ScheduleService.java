package com.busreservation.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Sort;

import com.busreservation.model.Schedule;


public interface ScheduleService {
    List<Schedule> getAllSchedules();
    void saveSchedule(Schedule schedule);
    Optional<Schedule> getScheduleById(Long id);
    void deleteScheduleById(Long id);
    long countSchedules();
    Set<String> getBookedSeats(Long scheduleId);
    List<Schedule> findSchedulesBySourceAndDestination(String source, String destination, Sort sort);

}