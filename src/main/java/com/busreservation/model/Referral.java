package com.busreservation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "referrals")
public class Referral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "referrer_id")
    private User referrer; // Person who referred

    @ManyToOne
    @JoinColumn(name = "referred_user_id")
    private User referredUser; // Person who was referred

    private String referralCode;
    private String status; // PENDING, COMPLETED, REWARDED
    private int pointsEarned;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Constructors
    public Referral() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    public Referral(User referrer, String referralCode) {
        this.referrer = referrer;
        this.referralCode = referralCode;
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getReferrer() {
        return referrer;
    }

    public void setReferrer(User referrer) {
        this.referrer = referrer;
    }

    public User getReferredUser() {
        return referredUser;
    }

    public void setReferredUser(User referredUser) {
        this.referredUser = referredUser;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
