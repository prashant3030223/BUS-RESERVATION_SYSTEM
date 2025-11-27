package com.busreservation.service;

import com.busreservation.model.Bus;
import java.util.List;
import java.util.Optional;

public interface BusService {
    List<Bus> getAllBuses();

    List<Bus> findAllBuses();

    void saveBus(Bus bus);

    Optional<Bus> getBusById(Long id);

    Bus findBusById(Long id);

    void deleteBusById(Long id);

    void deleteBus(Long id);

    long countBuses();
}