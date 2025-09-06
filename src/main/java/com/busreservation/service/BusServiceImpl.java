package com.busreservation.service;

import com.busreservation.model.Bus;
import com.busreservation.repository.BusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BusServiceImpl implements BusService {

    @Autowired
    private BusRepository busRepository;

    @Override
    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    @Override
    public void saveBus(Bus bus) {
        busRepository.save(bus);
    }

    @Override
    public Optional<Bus> getBusById(Long id) {
        return busRepository.findById(id);
    }

    @Override
    public void deleteBusById(Long id) {
        busRepository.deleteById(id);
    }
    
    @Override
    public long countBuses() {
        return busRepository.count();
    }
}