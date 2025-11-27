package com.busreservation.controller;

import com.busreservation.model.*;
import com.busreservation.service.*;
import com.busreservation.repository.LoyaltyPointsRepository;
import com.busreservation.repository.ReferralRepository;
import com.busreservation.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private BusService busService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private LoyaltyPointsRepository loyaltyPointsRepository;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private CarbonFootprintService carbonFootprintService;

    /**
     * Admin Dashboard
     */
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Principal principal) {
        // Get statistics
        long totalUsers = userService.countUsers();
        long totalBuses = busService.countBuses();
        long totalBookings = bookingService.countBookings();

        // Calculate total revenue safely
        Double totalRevenue = bookingService.getTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }

        // Get recent data
        List<Booking> recentBookings = bookingService.findRecentBookings(5);
        List<Schedule> upcomingSchedules = scheduleService.findUpcomingSchedules(5);

        // Get daily revenue for the chart
        Map<String, Double> dailyRevenue = bookingService.getDailyRevenueForWeek();

        // Bus Type Distribution
        List<Bus> allBuses = busService.findAllBuses();
        Map<String, Long> busTypeStats = allBuses.stream()
                .collect(Collectors.groupingBy(
                        bus -> bus.getBusType() != null ? bus.getBusType() : "Unknown",
                        Collectors.counting()));

        // Add to model
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalBuses", totalBuses);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentBookings", recentBookings);
        model.addAttribute("upcomingSchedules", upcomingSchedules);
        model.addAttribute("dailyRevenue", dailyRevenue);
        model.addAttribute("busTypeStats", busTypeStats);
        model.addAttribute("currentURI", "/admin/dashboard");

        return "admin/admin-dashboard";
    }

    /**
     * Manage Buses
     */
    @GetMapping("/buses")
    public String manageBuses(Model model) {
        List<Bus> buses = busService.findAllBuses();
        model.addAttribute("buses", buses);
        model.addAttribute("bus", new Bus());
        model.addAttribute("currentURI", "/admin/buses");
        return "admin/manage-buses";
    }

    @PostMapping("/buses/add")
    public String addBus(@ModelAttribute Bus bus, RedirectAttributes redirectAttributes) {
        busService.saveBus(bus);
        redirectAttributes.addFlashAttribute("success", "Bus added successfully!");
        return "redirect:/admin/buses";
    }

    @GetMapping("/buses/edit/{id}")
    public String editBusForm(@PathVariable Long id, Model model) {
        Bus bus = busService.findBusById(id);
        model.addAttribute("bus", bus);
        model.addAttribute("currentURI", "/admin/buses");
        return "admin/edit-bus";
    }

    @PostMapping("/buses/edit/{id}")
    public String updateBus(@PathVariable Long id, @ModelAttribute Bus bus, RedirectAttributes redirectAttributes) {
        bus.setId(id);
        busService.saveBus(bus);
        redirectAttributes.addFlashAttribute("success", "Bus updated successfully!");
        return "redirect:/admin/buses";
    }

    @GetMapping("/buses/delete/{id}")
    public String deleteBus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        busService.deleteBus(id);
        redirectAttributes.addFlashAttribute("success", "Bus deleted successfully!");
        return "redirect:/admin/buses";
    }

    /**
     * Manage Schedules
     */
    @GetMapping("/schedules")
    public String manageSchedules(Model model) {
        List<Schedule> schedules = scheduleService.findAllSchedules();
        if (schedules == null) {
            schedules = List.of();
        }
        List<Bus> buses = busService.findAllBuses();
        model.addAttribute("schedules", schedules);
        model.addAttribute("buses", buses);
        model.addAttribute("schedule", new Schedule());
        model.addAttribute("currentURI", "/admin/schedules");
        return "admin/manage-schedules";
    }

    @PostMapping("/schedules/add")
    public String addSchedule(@ModelAttribute Schedule schedule,
            @RequestParam Long busId,
            RedirectAttributes redirectAttributes) {
        Bus bus = busService.findBusById(busId);
        if (bus != null) {
            schedule.setBus(bus);
            schedule.setAvailableSeats(bus.getTotalSeats());
            scheduleService.saveSchedule(schedule);
            redirectAttributes.addFlashAttribute("success", "Schedule added successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Selected bus not found!");
        }
        return "redirect:/admin/schedules";
    }

    @GetMapping("/schedules/edit/{id}")
    public String editScheduleForm(@PathVariable Long id, Model model) {
        Schedule schedule = scheduleService.findScheduleById(id);
        List<Bus> buses = busService.findAllBuses();
        model.addAttribute("schedule", schedule);
        model.addAttribute("buses", buses);
        model.addAttribute("currentURI", "/admin/schedules");
        return "admin/edit-schedule";
    }

    @PostMapping("/schedules/edit/{id}")
    public String updateSchedule(@PathVariable Long id,
            @ModelAttribute Schedule schedule,
            @RequestParam Long busId,
            RedirectAttributes redirectAttributes) {
        Schedule existingSchedule = scheduleService.findScheduleById(id);
        if (existingSchedule != null) {
            Bus bus = busService.findBusById(busId);

            // Update fields
            existingSchedule.setBus(bus);
            existingSchedule.setPrice(schedule.getPrice());
            existingSchedule.setSource(schedule.getSource());
            existingSchedule.setDestination(schedule.getDestination());
            existingSchedule.setDepartureTime(schedule.getDepartureTime());
            existingSchedule.setArrivalTime(schedule.getArrivalTime());
            existingSchedule.setDistance(schedule.getDistance());

            // If bus capacity changed, we might need to adjust available seats?
            // For now, we preserve availableSeats unless it's greater than new total seats
            // (which shouldn't happen often if just editing details)
            // But if they switch to a smaller bus, we might have issues.
            // Let's just ensure availableSeats doesn't exceed totalSeats of new bus.
            if (existingSchedule.getAvailableSeats() > bus.getTotalSeats()) {
                existingSchedule.setAvailableSeats(bus.getTotalSeats());
            }

            scheduleService.saveSchedule(existingSchedule);
            redirectAttributes.addFlashAttribute("success", "Schedule updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Schedule not found!");
        }
        return "redirect:/admin/schedules";
    }

    @GetMapping("/schedules/delete/{id}")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        scheduleService.deleteSchedule(id);
        redirectAttributes.addFlashAttribute("success", "Schedule deleted successfully!");
        return "redirect:/admin/schedules";
    }

    @PostMapping("/schedules/update-status/{id}")
    public String updateScheduleStatus(@PathVariable Long id,
            @RequestParam String status,
            @RequestParam int delayMinutes,
            RedirectAttributes redirectAttributes) {
        scheduleService.updateScheduleStatus(id, status, delayMinutes);
        redirectAttributes.addFlashAttribute("success", "Schedule status updated successfully!");
        return "redirect:/admin/schedules";
    }

    /**
     * View Schedule Bookings
     */
    @GetMapping("/schedule/{id}/bookings")
    public String viewScheduleBookings(@PathVariable Long id, Model model) {
        Schedule schedule = scheduleService.findScheduleById(id);
        List<Booking> bookings = bookingService.findBookingsBySchedule(schedule);
        model.addAttribute("schedule", schedule);
        model.addAttribute("bookings", bookings);
        model.addAttribute("currentURI", "/admin/schedules");
        return "admin/view-schedule-bookings";
    }

    /**
     * View All Bookings
     */
    @GetMapping("/bookings")
    public String viewAllBookings(Model model) {
        List<Booking> bookings = bookingService.findAllBookings();
        model.addAttribute("bookings", bookings);
        model.addAttribute("currentURI", "/admin/bookings");
        return "admin/view-bookings";
    }

    /**
     * View All Users
     */
    @GetMapping("/users")
    public String viewAllUsers(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("currentURI", "/admin/users");
        return "admin/view-users";
    }

    /**
     * Security Audit
     */
    @GetMapping("/security-audit")
    public String securityAudit(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("currentURI", "/admin/security-audit");
        return "admin/security-audit";
    }

    @PostMapping("/security-audit/verify-user/{id}")
    public String verifyUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findAllUsers().stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
        if (user != null) {
            user.setVerified(true);
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("success", "User verified successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found!");
        }
        return "redirect:/admin/security-audit";
    }

    @PostMapping("/security-audit/block-user/{id}")
    public String blockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findAllUsers().stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
        if (user != null) {
            user.setEnabled(!user.isEnabled()); // Toggle status
            userService.saveUser(user);
            String status = user.isEnabled() ? "unblocked" : "blocked";
            redirectAttributes.addFlashAttribute("success", "User " + status + " successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found!");
        }
        return "redirect:/admin/security-audit";
    }

    /**
     * Feedback Management
     */
    @GetMapping("/feedback")
    public String viewFeedback(Model model) {
        List<Feedback> feedbacks = feedbackService.getAllFeedback();
        long newFeedbackCount = feedbackService.getNewFeedbackCount();
        long totalFeedbackCount = feedbackService.getTotalFeedbackCount();

        model.addAttribute("feedbackList", feedbacks);
        model.addAttribute("newCount", newFeedbackCount);
        model.addAttribute("totalCount", totalFeedbackCount);
        model.addAttribute("currentURI", "/admin/feedback");
        return "admin/admin-feedback";
    }

    @PostMapping("/feedback/{id}/status")
    public String updateFeedbackStatus(@PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        feedbackService.updateFeedbackStatus(id, status);
        redirectAttributes.addFlashAttribute("success", "Feedback status updated!");
        return "redirect:/admin/feedback";
    }

    @GetMapping("/feedback/delete/{id}")
    public String deleteFeedback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        feedbackService.deleteFeedback(id);
        redirectAttributes.addFlashAttribute("success", "Feedback deleted successfully!");
        return "redirect:/admin/feedback";
    }

    /**
     * Loyalty & Referral Program
     */
    @GetMapping("/loyalty-program")
    public String loyaltyProgram(Model model) {
        List<User> users = userService.findAllUsers();

        // Calculate statistics
        List<LoyaltyPoints> allPoints = loyaltyPointsRepository.findAll();

        long totalPointsAwarded = allPoints.stream()
                .mapToInt(LoyaltyPoints::getPoints)
                .sum();

        long totalReferrals = referralRepository.count();

        // Calculate points per user
        Map<Long, Integer> userPointsMap = allPoints.stream()
                .collect(Collectors.groupingBy(lp -> lp.getUser().getId(),
                        Collectors.summingInt(LoyaltyPoints::getPoints)));

        // Ensure all users have an entry and sort by points
        for (User user : users) {
            userPointsMap.putIfAbsent(user.getId(), 0);
        }

        users.sort((u1, u2) -> userPointsMap.get(u2.getId()).compareTo(userPointsMap.get(u1.getId())));

        model.addAttribute("users", users);
        model.addAttribute("userPointsMap", userPointsMap);
        model.addAttribute("totalPointsAwarded", totalPointsAwarded);
        model.addAttribute("totalReferrals", totalReferrals);

        // Add Settings to Model
        model.addAttribute("pointsPerRupee", loyaltyService.getIntSetting("POINTS_PER_RUPEE", 1));
        model.addAttribute("signupBonus", loyaltyService.getIntSetting("SIGNUP_BONUS", 50));
        model.addAttribute("referralBonus", loyaltyService.getIntSetting("REFERRAL_BONUS", 100));
        model.addAttribute("redemptionValue", loyaltyService.getDoubleSetting("REDEMPTION_VALUE", 0.1));

        model.addAttribute("currentURI", "/admin/loyalty-program");
        return "admin/loyalty-program";
    }

    @PostMapping("/loyalty-program/update-settings")
    public String updateLoyaltySettings(
            @RequestParam String pointsPerRupee,
            @RequestParam String signupBonus,
            @RequestParam String referralBonus,
            @RequestParam String redemptionValue,
            RedirectAttributes redirectAttributes) {

        try {
            // Validate and Parse
            int points = Integer.parseInt(pointsPerRupee);
            int signup = Integer.parseInt(signupBonus);
            int referral = Integer.parseInt(referralBonus);
            double redemption = Double.parseDouble(redemptionValue);

            loyaltyService.updateSetting("POINTS_PER_RUPEE", String.valueOf(points));
            loyaltyService.updateSetting("SIGNUP_BONUS", String.valueOf(signup));
            loyaltyService.updateSetting("REFERRAL_BONUS", String.valueOf(referral));
            loyaltyService.updateSetting("REDEMPTION_VALUE", String.valueOf(redemption));

            redirectAttributes.addFlashAttribute("success", "Loyalty program settings updated!");
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid number format. Please check your inputs.");
        } catch (Exception e) {
            e.printStackTrace(); // Log error
            redirectAttributes.addFlashAttribute("error", "Failed to update settings: " + e.getMessage());
        }

        return "redirect:/admin/loyalty-program";
    }

    @PostMapping("/loyalty-program/generate-referral")
    public String generateReferral(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        User user = userService.findAllUsers().stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found for referral generation.");
            return "redirect:/admin/loyalty-program";
        }
        Referral referral = loyaltyService.createReferral(user);
        redirectAttributes.addFlashAttribute("success", "Referral code generated: " + referral.getReferralCode());
        return "redirect:/admin/loyalty-program";
    }

    /**
     * Staff Management
     */
    @Autowired
    private StaffService staffService;

    @GetMapping("/staff")
    public String manageStaff(Model model) {
        List<Staff> staffList = staffService.getAllStaff();
        model.addAttribute("staffList", staffList);
        model.addAttribute("staff", new Staff());
        model.addAttribute("currentURI", "/admin/staff");
        return "admin/manage-staff";
    }

    @PostMapping("/staff/add")
    public String addStaff(@ModelAttribute Staff staff, RedirectAttributes redirectAttributes) {
        staff.setStatus("Active");
        staffService.saveStaff(staff);
        redirectAttributes.addFlashAttribute("success", "Staff member added successfully!");
        return "redirect:/admin/staff";
    }

    @GetMapping("/staff/edit/{id}")
    public String editStaffForm(@PathVariable Long id, Model model) {
        Staff staff = staffService.getStaffById(id);
        model.addAttribute("staff", staff);
        model.addAttribute("currentURI", "/admin/staff");
        return "admin/edit-staff";
    }

    @PostMapping("/staff/edit/{id}")
    public String updateStaff(@PathVariable Long id, @ModelAttribute Staff staff,
            RedirectAttributes redirectAttributes) {
        Staff existingStaff = staffService.getStaffById(id);
        if (existingStaff != null) {
            existingStaff.setFullName(staff.getFullName());
            existingStaff.setEmail(staff.getEmail());
            existingStaff.setMobile(staff.getMobile());
            existingStaff.setRole(staff.getRole());
            existingStaff.setLicenseNumber(staff.getLicenseNumber());
            existingStaff.setStatus(staff.getStatus());
            staffService.saveStaff(existingStaff);
            redirectAttributes.addFlashAttribute("success", "Staff updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Staff not found!");
        }
        return "redirect:/admin/staff";
    }

    @GetMapping("/staff/delete/{id}")
    public String deleteStaff(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        staffService.deleteStaff(id);
        redirectAttributes.addFlashAttribute("success", "Staff deleted successfully!");
        return "redirect:/admin/staff";
    }

    /**
     * Reports & Analytics
     */
    @GetMapping("/reports")
    public String viewReports(Model model) {
        // Revenue Analytics
        Double totalRevenue = bookingService.getTotalRevenue();
        if (totalRevenue == null)
            totalRevenue = 0.0;

        Map<String, Double> dailyRevenue = bookingService.getDailyRevenueForWeek();

        // Booking Statistics
        long totalBookings = bookingService.countBookings();
        List<Booking> recentBookings = bookingService.findRecentBookings(10);

        // Popular Routes
        List<Schedule> allSchedules = scheduleService.findAllSchedules();
        Map<String, Long> routeBookings = new java.util.HashMap<>();
        for (Schedule schedule : allSchedules) {
            String route = schedule.getSource() + " → " + schedule.getDestination();
            long bookingCount = bookingService.findBookingsBySchedule(schedule).size();
            routeBookings.put(route, bookingCount);
        }

        // Bus Utilization
        List<Bus> buses = busService.findAllBuses();
        Map<String, Double> busUtilization = new java.util.HashMap<>();
        for (Bus bus : buses) {
            List<Schedule> busSchedules = allSchedules.stream()
                    .filter(s -> s.getBus() != null && s.getBus().getId().equals(bus.getId()))
                    .collect(Collectors.toList());

            double avgOccupancy = 0.0;
            if (!busSchedules.isEmpty()) {
                avgOccupancy = busSchedules.stream()
                        .mapToDouble(s -> ((double) (s.getBus().getTotalSeats() - s.getAvailableSeats())
                                / s.getBus().getTotalSeats()) * 100)
                        .average()
                        .orElse(0.0);
            }
            busUtilization.put(bus.getBusName(), avgOccupancy);
        }

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("dailyRevenue", dailyRevenue);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("recentBookings", recentBookings);
        model.addAttribute("routeBookings", routeBookings);
        model.addAttribute("busUtilization", busUtilization);
        model.addAttribute("currentURI", "/admin/reports");
        return "admin/reports";
    }

    /**
     * System Settings
     */
    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @GetMapping("/settings")
    public String systemSettings(Model model) {
        List<SystemSetting> allSettings = systemSettingRepository.findAll();

        Map<String, List<SystemSetting>> settingsByCategory = allSettings.stream()
                .collect(Collectors.groupingBy(s -> s.getCategory() != null ? s.getCategory() : "GENERAL"));

        model.addAttribute("settingsByCategory", settingsByCategory);
        model.addAttribute("currentURI", "/admin/settings");
        return "admin/system-settings";
    }

    @PostMapping("/settings/update")
    public String updateSystemSettings(@RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("setting_")) {
                    String settingKey = entry.getKey().substring(8); // Remove "setting_" prefix
                    String settingValue = entry.getValue();

                    SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
                            .orElse(new SystemSetting(settingKey, settingValue, "", "GENERAL"));

                    setting.setSettingValue(settingValue);
                    systemSettingRepository.save(setting);
                }
            }
            redirectAttributes.addFlashAttribute("success", "Settings updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update settings: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/add")
    public String addSystemSetting(
            @RequestParam String settingKey,
            @RequestParam String settingValue,
            @RequestParam String description,
            @RequestParam String category,
            RedirectAttributes redirectAttributes) {

        try {
            SystemSetting setting = new SystemSetting(settingKey, settingValue, description, category);
            systemSettingRepository.save(setting);
            redirectAttributes.addFlashAttribute("success", "Setting added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add setting: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    /**
     * Admin Carbon Emission Report
     */
    @GetMapping("/carbon-report")
    public String showAdminCarbonReport(Model model) {
        List<Booking> allBookings = bookingService.findAllBookings();

        double totalCO2Saved = 0.0;
        double totalDistance = 0.0;
        int totalTrips = 0;
        double totalBusEmissions = 0.0;

        for (Booking booking : allBookings) {
            if (booking.getStatus().equals("CONFIRMED") && booking.getSchedule() != null) {
                double distance = booking.getSchedule().getDistance();
                int passengers = 1; // Each booking represents 1 passenger

                if (distance > 0) {
                    totalCO2Saved += carbonFootprintService.calculateCO2Saved(distance, passengers);
                    totalBusEmissions += carbonFootprintService.calculateBusEmissions(distance, passengers);
                    totalDistance += distance;
                    totalTrips++;
                }
            }
        }

        // Calculate equivalent metrics
        double treesEquivalent = totalCO2Saved / 21.0;
        double carKmEquivalent = totalCO2Saved / 0.192;

        model.addAttribute("totalCO2Saved", totalCO2Saved);
        model.addAttribute("totalBusEmissions", totalBusEmissions);
        model.addAttribute("totalDistance", totalDistance);
        model.addAttribute("totalTrips", totalTrips);
        model.addAttribute("treesEquivalent", treesEquivalent);
        model.addAttribute("carKmEquivalent", carKmEquivalent);

        return "admin/carbon-report";
    }

}
