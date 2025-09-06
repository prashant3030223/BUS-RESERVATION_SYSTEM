package com.busreservation.controller;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.busreservation.exception.ResourceNotFoundException;
import com.busreservation.model.Booking;
import com.busreservation.model.Bus;
import com.busreservation.model.Schedule;
import com.busreservation.service.BookingService;
import com.busreservation.service.BusService;
import com.busreservation.service.ScheduleService;
import com.busreservation.service.UserService;
import com.opencsv.CSVWriter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private BusService busService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private BookingService bookingService;
    @Autowired private UserService userService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalBuses", busService.countBuses());
        model.addAttribute("totalSchedules", scheduleService.countSchedules());
        model.addAttribute("totalBookings", bookingService.countBookings());
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("totalRevenue", bookingService.getTotalRevenue());
        model.addAttribute("recentBookings", bookingService.getRecentBookings());
        return "admin/admin-dashboard";
    }

    @GetMapping("/bookings")
    public String viewAllBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "admin/view-bookings";
    }

    @GetMapping("/users")
    public String viewAllUsers(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/view-users"; // Yeh view-users.html page ko dikhayega
    }

    @GetMapping("/buses")
    public String manageBuses(Model model) {
        model.addAttribute("bus", new Bus());
        model.addAttribute("buses", busService.getAllBuses());
        return "admin/manage-buses";
    }

    @PostMapping("/buses/add")
    public String addBus(@Valid @ModelAttribute("bus") Bus bus, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("buses", busService.getAllBuses());
            return "admin/manage-buses";
        }
        busService.saveBus(bus);
        return "redirect:/admin/buses";
    }

    @GetMapping("/buses/edit/{id}")
    public String showEditBusForm(@PathVariable Long id, Model model) {
        Bus bus = busService.getBusById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));
        model.addAttribute("bus", bus);
        return "admin/edit-bus";
    }

    @PostMapping("/buses/update/{id}")
    public String updateBus(@PathVariable Long id, @Valid @ModelAttribute("bus") Bus busDetails, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/edit-bus";
        }
        busDetails.setId(id);
        busService.saveBus(busDetails);
        return "redirect:/admin/buses";
    }

    @GetMapping("/buses/delete/{id}")
    public String deleteBus(@PathVariable Long id) {
        busService.deleteBusById(id);
        return "redirect:/admin/buses";
    }
    
    @GetMapping("/schedules")
    public String manageSchedules(Model model) {
        model.addAttribute("schedule", new Schedule());
        model.addAttribute("buses", busService.getAllBuses());
        model.addAttribute("schedules", scheduleService.getAllSchedules());
        return "admin/manage-schedules";
    }

    @PostMapping("/schedules/add")
    public String addSchedule(@ModelAttribute Schedule schedule, @RequestParam Long busId) {
        Bus bus = busService.getBusById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));
        schedule.setBus(bus);
        schedule.setAvailableSeats(bus.getTotalSeats());
        scheduleService.saveSchedule(schedule);
        return "redirect:/admin/schedules";
    }

    @GetMapping("/schedules/edit/{id}")
    public String showEditScheduleForm(@PathVariable Long id, Model model) {
        Schedule schedule = scheduleService.getScheduleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        model.addAttribute("schedule", schedule);
        model.addAttribute("buses", busService.getAllBuses());
        return "admin/edit-schedule";
    }

    @PostMapping("/schedules/update/{id}")
    public String updateSchedule(@PathVariable Long id, @ModelAttribute Schedule scheduleDetails, @RequestParam Long busId) {
        Bus bus = busService.getBusById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));
        scheduleDetails.setId(id);
        scheduleDetails.setBus(bus);
        scheduleService.saveSchedule(scheduleDetails);
        return "redirect:/admin/schedules";
    }

    @GetMapping("/schedules/delete/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteScheduleById(id);
        return "redirect:/admin/schedules";
    }
    @GetMapping("/schedule/{id}/bookings")
    public String viewBookingsForSchedule(@PathVariable Long id, Model model) {
        Schedule schedule = scheduleService.getScheduleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        model.addAttribute("schedule", schedule);
        model.addAttribute("bookings", schedule.getBookings());
        return "admin/view-schedule-bookings";
    }
    @GetMapping("/bookings/export")
    public void exportBookingsToCsv(HttpServletResponse response) throws IOException {
        // File ka naam aur type set karein
        String filename = "bookings_report.csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (CSVWriter writer = new CSVWriter(response.getWriter())) {
            // Step 1: CSV file ka Header (column ke naam) likhein
            String[] header = {
                "Booking ID", "Passenger Name", "Email", "Phone No", "Address",
                "Bus Name", "Route", "Departure Time", "Seats", "Status",
                "Total Price", "Booking Date"
            };
            writer.writeNext(header);

            // Step 2: Sabhi bookings ko fetch karein
            List<Booking> bookings = bookingService.getAllBookings();

            // Step 3: Har booking ke liye data row likhein
            for (Booking booking : bookings) {
                String[] data = {
                    String.valueOf(booking.getId()),
                    booking.getUser().getFullName(),
                    booking.getUser().getEmail(),
                    booking.getUser().getMobile() != null ? booking.getUser().getMobile() : "N/A",
                    booking.getUser().getAddress() != null ? booking.getUser().getAddress() : "N/A",
                    booking.getSchedule().getBus().getBusName(),
                    booking.getSchedule().getSource() + " to " + booking.getSchedule().getDestination(),
                    booking.getSchedule().getDepartureTime().toString(),
                    booking.getSeatNumbers(),
                    booking.getStatus(),
                    String.valueOf(booking.getTotalPrice()),
                    booking.getBookingDate().toString()
                };
                writer.writeNext(data);
            }
        }
    }
}