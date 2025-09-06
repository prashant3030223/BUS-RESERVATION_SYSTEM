package com.busreservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public class UserRegistrationDto {
    @NotEmpty(message = "Full name cannot be empty")
    private String fullName;
    @NotEmpty(message = "Email cannot be empty")
    @Email(message = "Please provide a valid email")
    private String email;
    @NotEmpty(message = "Password cannot be empty")
    private String password;
    
    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}