package com.busreservation.controller;

import com.busreservation.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/bookings-per-day")
    public ResponseEntity<Map<String, Long>> getBookingsPerDay() {
        return ResponseEntity.ok(bookingService.getBookingsPerDay());
    }
}