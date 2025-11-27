package com.busreservation.service;

import com.busreservation.dto.UserRegistrationDto;
import com.busreservation.model.User;
import com.busreservation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoyaltyService loyaltyService;

    @Override
    public User save(UserRegistrationDto registrationDto) {
        User user = new User();
        user.setFullName(registrationDto.getFullName());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole("ROLE_USER");

        // Generate unique referral code
        user.setReferralCode(loyaltyService.generateReferralCode(user));

        User savedUser = userRepository.save(user);

        // Signup Bonus
        loyaltyService.awardSignupBonus(savedUser);

        // Process Referral if code provided
        if (registrationDto.getReferralCode() != null && !registrationDto.getReferralCode().isEmpty()) {
            loyaltyService.processReferralCode(savedUser, registrationDto.getReferralCode());
        }

        return savedUser;
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Is method ko naye fields save karne ke liye update kiya gaya hai.
     */
    @Override
    public void updateProfile(String email, String fullName, String mobile, String address) {
        User user = findByEmail(email);
        if (user != null) {
            user.setFullName(fullName);
            user.setMobile(mobile); // Mobile number save karein
            user.setAddress(address); // Address save karein
            userRepository.save(user);
        }
    }

    @Override // <-- Naya method
    public void updateProfilePhoto(String email, String photoUrl) {
        User user = findByEmail(email);
        if (user != null) {
            user.setProfilePhotoUrl(photoUrl);
            userRepository.save(user);
        }
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public boolean changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }
}
