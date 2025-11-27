package com.busreservation.controller;

import com.busreservation.model.User;
import com.busreservation.service.GoogleAuthService;
import com.busreservation.service.UserService;
import com.busreservation.service.CustomUserDetailsService;
import com.busreservation.service.LoyaltyService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Controller
public class GoogleLoginController {

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoyaltyService loyaltyService;

    private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/auth/google")
    @ResponseBody
    public ResponseEntity<?> googleLogin(@RequestParam("credential") String credential, HttpServletRequest request,
            HttpServletResponse response) {
        try {
            GoogleIdToken.Payload payload = googleAuthService.verifyToken(credential);
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            System.out.println("----- GOOGLE LOGIN DEBUG -----");
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);
            System.out.println("Picture URL: " + pictureUrl);
            System.out.println("------------------------------");

            User user = userService.findByEmail(email);
            if (user == null) {
                System.out.println("User not found. Registering new user...");
                // Register new user
                user = new User();
                user.setEmail(email);
                user.setFullName(name);
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Dummy password
                user.setRole("ROLE_USER");
                user.setVerified(true);
                user.setProfilePhotoUrl(pictureUrl);

                // Generate referral code
                user.setReferralCode(loyaltyService.generateReferralCode(user));

                userService.saveUser(user);
                System.out.println("New user saved with ID: " + user.getId());

                // Award signup bonus
                loyaltyService.awardSignupBonus(user);
            } else {
                System.out.println("User found with ID: " + user.getId());
                // Update profile photo if changed
                if (pictureUrl != null && !pictureUrl.equals(user.getProfilePhotoUrl())) {
                    user.setProfilePhotoUrl(pictureUrl);
                    userService.saveUser(user);
                    System.out.println("Updated profile photo for user: " + email);
                }
            }

            // Log the user in
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("status", "success");
            responseBody.put("redirectUrl", "/");
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Google login failed: " + e.getMessage());
        }
    }
}
