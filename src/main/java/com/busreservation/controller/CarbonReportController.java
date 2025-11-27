package com.busreservation.controller;

import com.busreservation.model.Booking;
import com.busreservation.model.User;
import com.busreservation.service.BookingService;
import com.busreservation.service.CarbonFootprintService;
import com.busreservation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/carbon-report")
public class CarbonReportController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private CarbonFootprintService carbonFootprintService;

    @GetMapping
    public String showCarbonReport(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(principal.getName());
        List<Booking> userBookings = bookingService.getBookingsByUser(user);

        double totalCO2Saved = 0.0;
        double totalDistance = 0.0;
        int totalTrips = 0;

        for (Booking booking : userBookings) {
            if (booking.getStatus().equals("CONFIRMED") && booking.getSchedule() != null) {
                double distance = booking.getSchedule().getDistance();
                int passengers = 1; // Each booking is for 1 passenger

                if (distance > 0) {
                    totalCO2Saved += carbonFootprintService.calculateCO2Saved(distance, passengers);
                    totalDistance += distance;
                    totalTrips++;
                }
            }
        }

        // Calculate equivalent metrics
        double treesEquivalent = totalCO2Saved / 21.0; // 1 tree absorbs ~21kg CO2/year
        double carKmEquivalent = totalCO2Saved / 0.192; // Car emissions per km

        model.addAttribute("totalCO2Saved", totalCO2Saved);
        model.addAttribute("totalDistance", totalDistance);
        model.addAttribute("totalTrips", totalTrips);
        model.addAttribute("treesEquivalent", treesEquivalent);
        model.addAttribute("carKmEquivalent", carKmEquivalent);
        model.addAttribute("userBookings", userBookings);

        return "carbon-report";
    }
}
