package com.busreservation.controller;

import com.busreservation.exception.ResourceNotFoundException;
import com.busreservation.model.*;
import com.busreservation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Set; // Naya import

@Controller
public class BookingController {

    @Autowired private ScheduleService scheduleService;
    @Autowired private BookingService bookingService;
    @Autowired private UserService userService;

    @GetMapping("/book/{scheduleId}")
    public String showBookingPage(@PathVariable Long scheduleId, Model model) {
        Schedule schedule = scheduleService.getScheduleById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));
        
        Set<String> bookedSeats = scheduleService.getBookedSeats(scheduleId);

        model.addAttribute("schedule", schedule);
        model.addAttribute("bookedSeats", bookedSeats);
        model.addAttribute("booking", new Booking());
        return "booking-page";
    }

    @PostMapping("/book/confirm")
    public String confirmBooking(@ModelAttribute Booking booking, @RequestParam Long scheduleId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(authentication.getName());
        Schedule schedule = scheduleService.getScheduleById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        String[] selectedSeats = booking.getSeatNumbers().split(",");
        int numberOfSeats = selectedSeats.length;

        // Check karein ki sufficient seats hain ya nahi
        if (schedule.getAvailableSeats() < numberOfSeats) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sorry, only " + schedule.getAvailableSeats() + " seat(s) available.");
            return "redirect:/book/" + scheduleId;
        }

        // Available seats kam karein
        schedule.setAvailableSeats(schedule.getAvailableSeats() - numberOfSeats);
        scheduleService.saveSchedule(schedule); // Schedule ko update karein

        // Price Calculation Logic...
        double pricePerSeat = schedule.getPrice();
        double totalPrice = numberOfSeats * pricePerSeat;
        booking.setTotalPrice(totalPrice);
        
        // Baaki booking ka logic
        booking.setUser(currentUser);
        booking.setSchedule(schedule);
        booking.setBookingDate(LocalDate.now());
        booking.setStatus("CONFIRMED");

        Booking savedBooking = bookingService.saveBooking(booking);
        return "redirect:/ticket/" + savedBooking.getId();
    }

    
    @GetMapping("/ticket/{bookingId}")
    public String showTicketPage(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        model.addAttribute("booking", booking);
        return "ticket";
    }

    @GetMapping("/my-bookings")
    public String showMyBookings(Model model, Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        model.addAttribute("bookings", bookingService.getBookingsByUser(currentUser));
        return "my-bookings";
    }

    @PostMapping("/booking/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/my-bookings?error";
        }

        booking.setStatus("CANCELLED");
        bookingService.saveBooking(booking);
        if ("CONFIRMED".equals(booking.getStatus())) {
            booking.setStatus("CANCELLED");
            bookingService.saveBooking(booking);

            // Seats ki ginti wapas badhayein
            Schedule schedule = booking.getSchedule();
            int cancelledSeatsCount = booking.getSeatNumbers().split(",").length;
            schedule.setAvailableSeats(schedule.getAvailableSeats() + cancelledSeatsCount);
            scheduleService.saveSchedule(schedule);
        }
        return "redirect:/my-bookings?cancel_success";
    }
}