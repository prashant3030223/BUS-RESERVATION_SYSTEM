package com.busreservation.controller;

import org.springframework.data.domain.Sort;
import com.busreservation.dto.UserRegistrationDto;
import com.busreservation.model.Feedback;
import com.busreservation.model.Schedule;
import com.busreservation.service.FeedbackService;
import com.busreservation.service.ScheduleService;
import com.busreservation.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
public class PublicController {

    @Autowired
    private UserService userService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/")
    public String homePage() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
            BindingResult bindingResult,
            Model model) {
        if (userService.findByEmail(userDto.getEmail()) != null) {
            bindingResult.rejectValue("email", null, "An account already exists with this email.");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // Save user (Handles signup bonus and referrals internally)
        userService.save(userDto);

        return "redirect:/register?success";
    }

    @GetMapping("/search")
    public String searchBuses(@RequestParam String source, @RequestParam String destination,
            @RequestParam(required = false) String busType,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "price,asc") String sort, Model model) {

        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);

        List<Schedule> schedules = scheduleService.searchSchedules(source, destination, busType, maxPrice, sortOrder);

        model.addAttribute("schedules", schedules);
        model.addAttribute("source", source);
        model.addAttribute("destination", destination);
        model.addAttribute("sort", sort);
        model.addAttribute("busType", busType); 
        model.addAttribute("maxPrice", maxPrice);

        return "search-results";
    }

    @GetMapping("/feedback")
    public String feedbackPage() {
        return "feedback";
    }

    @PostMapping("/feedback")
    public String submitFeedback(@RequestParam String name,
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(required = false) Integer rating,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (rating == null) {
            redirectAttributes.addFlashAttribute("error", "Please select a rating to submit your feedback.");
            return "redirect:/feedback";
        }

        try {
            Feedback feedback = new Feedback(name, email, subject, message, rating);
            feedbackService.saveFeedback(feedback);
            return "redirect:/feedback?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred while submitting feedback. Please try again.");
            return "redirect:/feedback";
        }
    }

    @GetMapping("/about")
    public String aboutPage() {
        return "about";
    }

    @GetMapping("/contact")
    public String contactPage() {
        return "contact";
    }

    @GetMapping("/help-center")
    public String helpCenterPage() {
        return "help-center";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicyPage() {
        return "privacy-policy";
    }

    @GetMapping("/terms-of-service")
    public String termsOfServicePage() {
        return "terms-of-service";
    }
}