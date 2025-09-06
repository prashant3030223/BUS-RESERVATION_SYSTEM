package com.busreservation.service;

import com.busreservation.model.Bus;
import java.util.List;
import java.util.Optional;

public interface BusService {
    List<Bus> getAllBuses();
    void saveBus(Bus bus);
    Optional<Bus> getBusById(Long id);
    void deleteBusById(Long id);
    long countBuses();
}