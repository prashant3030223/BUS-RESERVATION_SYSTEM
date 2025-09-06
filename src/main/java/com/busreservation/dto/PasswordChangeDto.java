package com.busreservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class PasswordChangeDto {
    @NotEmpty(message = "Old password cannot be empty")
    private String oldPassword;

    @NotEmpty(message = "New password cannot be empty")
    @Size(min = 6, message = "New password must be at least 6 characters long")
    private String newPassword;

    @NotEmpty(message = "Please confirm your new password")
    private String confirmPassword;

    // --- Getters and Setters ---
    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}