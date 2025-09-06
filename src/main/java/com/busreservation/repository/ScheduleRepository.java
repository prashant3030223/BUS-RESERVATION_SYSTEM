package com.busreservation.repository;

import com.busreservation.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findBySourceAndDestination(String source, String destination, Sort sort);

}