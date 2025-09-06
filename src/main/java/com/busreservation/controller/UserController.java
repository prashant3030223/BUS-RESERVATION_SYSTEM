package com.busreservation.controller;

import com.busreservation.dto.PasswordChangeDto;
import com.busreservation.model.User;
import com.busreservation.service.FileStorageService;
import com.busreservation.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/profile")
    public String viewProfile(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        User currentUser = userService.findByEmail(authentication.getName());
        model.addAttribute("user", currentUser);
        
        // YEH LINE ZAROORI HAI taaki "Change Password" form kaam kare
        if (!model.containsAttribute("passwordChangeDto")) {
            model.addAttribute("passwordChangeDto", new PasswordChangeDto());
        }
        
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String mobile,
                                @RequestParam String address,
                                @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }
        String email = authentication.getName();

        userService.updateProfile(email, fullName, mobile, address);

        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String photoUrl = fileStorageService.storeFile(profileImage);
                userService.updateProfilePhoto(email, photoUrl);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Could not upload profile image: " + e.getMessage());
                return "redirect:/profile";
            }
        }
        
        redirectAttributes.addFlashAttribute("successMessage", "Your profile has been updated successfully!");
        return "redirect:/profile";
    }
    
    @PostMapping("/profile/change-password")
    public String changePassword(@ModelAttribute("passwordChangeDto") @Valid PasswordChangeDto passwordChangeDto,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordChangeDto", bindingResult);
            redirectAttributes.addFlashAttribute("passwordChangeDto", passwordChangeDto);
            return "redirect:/profile?tab=password";
        }

        boolean isChanged = userService.changePassword(authentication.getName(), passwordChangeDto.getOldPassword(), passwordChangeDto.getNewPassword());

        if (isChanged) {
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Incorrect old password.");
        }
        
        return "redirect:/profile?tab=password";
    }
}