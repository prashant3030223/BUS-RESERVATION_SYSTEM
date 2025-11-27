package com.busreservation.service;

import com.busreservation.dto.UserRegistrationDto;
import com.busreservation.model.User;
import java.util.List;

public interface UserService {

    User save(UserRegistrationDto registrationDto);

    User findByEmail(String email);

    void updateProfile(String email, String fullName, String mobile, String address);

    void updateProfilePhoto(String email, String photoUrl);

    long countUsers();

    List<User> findAllUsers();

    boolean changePassword(String email, String oldPassword, String newPassword);

    void saveUser(User user);
}