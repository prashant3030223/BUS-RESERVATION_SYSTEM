package com.busreservation.service;
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

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Override
    public List<Schedule> getAllSchedules() {
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
    public void deleteScheduleById(Long id) {
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
}