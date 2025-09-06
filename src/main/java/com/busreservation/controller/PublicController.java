package com.busreservation.controller;
import org.springframework.data.domain.Sort;
import com.busreservation.dto.UserRegistrationDto;
import com.busreservation.model.Schedule;
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
    public String registerUser(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto, BindingResult bindingResult, Model model) {
        if (userService.findByEmail(userDto.getEmail()) != null) {
            bindingResult.rejectValue("email", null, "An account already exists with this email.");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        userService.save(userDto);
        return "redirect:/register?success";
    }

    @GetMapping("/search")
    public String searchBuses(@RequestParam String source, @RequestParam String destination, 
                              @RequestParam(defaultValue = "price,asc") String sort, Model model) {
        
        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);

        List<Schedule> schedules = scheduleService.findSchedulesBySourceAndDestination(source, destination, sortOrder);
        
        model.addAttribute("schedules", schedules);
        model.addAttribute("source", source); // search query ko wapas bhejein
        model.addAttribute("destination", destination); // search query ko wapas bhejein
        model.addAttribute("sort", sort); // current sort order ko wapas bhejein

        return "search-results";
    }
}