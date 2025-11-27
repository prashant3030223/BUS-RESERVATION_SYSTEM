package com.busreservation.service;

import com.busreservation.model.Booking;
import com.busreservation.model.LoyaltyPoints;
import com.busreservation.model.Referral;
import com.busreservation.model.User;
import com.busreservation.model.LoyaltySetting;
import com.busreservation.repository.LoyaltyPointsRepository;
import com.busreservation.repository.LoyaltySettingRepository;
import com.busreservation.repository.ReferralRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class LoyaltyService {

    @Autowired
    private LoyaltyPointsRepository loyaltyPointsRepository;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private LoyaltySettingRepository loyaltySettingRepository;

    @Autowired
    private com.busreservation.repository.NotificationRepository notificationRepository;

    // Default values
    private static final int DEFAULT_POINTS_PER_RUPEE = 1;
    private static final int DEFAULT_REFERRAL_BONUS = 100;
    private static final int DEFAULT_SIGNUP_BONUS = 50;
    private static final double DEFAULT_REDEMPTION_VALUE = 0.1; // 1 point = ₹0.1

    /**
     * Award points for a booking
     */
    public void awardBookingPoints(User user, Booking booking) {
        int pointsPerRupee = getIntSetting("POINTS_PER_RUPEE", DEFAULT_POINTS_PER_RUPEE);
        int points = (int) (booking.getTotalPrice() * pointsPerRupee);
        LoyaltyPoints loyaltyPoints = new LoyaltyPoints(
                user,
                points,
                "BOOKING",
                "Earned from booking #" + booking.getId());
        loyaltyPoints.setBooking(booking);
        loyaltyPointsRepository.save(loyaltyPoints);

        // Send Notification
        createNotification(user, "Points Earned", "You earned " + points + " loyalty points for your booking!");
    }

    /**
     * Award signup bonus
     */
    public void awardSignupBonus(User user) {
        int signupBonus = getIntSetting("SIGNUP_BONUS", DEFAULT_SIGNUP_BONUS);
        LoyaltyPoints loyaltyPoints = new LoyaltyPoints(
                user,
                signupBonus,
                "BONUS",
                "Welcome bonus for new user");
        loyaltyPointsRepository.save(loyaltyPoints);

        // Send Notification
        createNotification(user, "Welcome Bonus",
                "Welcome! You received " + signupBonus + " loyalty points as a signup bonus.");
    }

    /**
     * Award referral bonus
     */
    public void awardReferralBonus(User referrer, User referredUser) {
        int referralBonus = getIntSetting("REFERRAL_BONUS", DEFAULT_REFERRAL_BONUS);
        // Award points to referrer
        LoyaltyPoints loyaltyPoints = new LoyaltyPoints(
                referrer,
                referralBonus,
                "REFERRAL",
                "Referral bonus for inviting " + referredUser.getFullName());
        loyaltyPointsRepository.save(loyaltyPoints);

        // Send Notification
        createNotification(referrer, "Referral Bonus",
                "You earned " + referralBonus + " points for referring " + referredUser.getFullName());

        // Update referral status
        Optional<Referral> referralOpt = referralRepository.findByReferredUser(referredUser);
        if (referralOpt.isPresent()) {
            Referral referral = referralOpt.get();
            referral.setStatus("REWARDED");
            referral.setPointsEarned(referralBonus);
            referral.setCompletedAt(LocalDateTime.now());
            referralRepository.save(referral);
        }
    }

    /**
     * Get total points for a user
     */
    public int getTotalPoints(User user) {
        return loyaltyPointsRepository.getTotalPointsByUser(user);
    }

    /**
     * Get points history for a user
     */
    public List<LoyaltyPoints> getPointsHistory(User user) {
        return loyaltyPointsRepository.findByUserOrderByEarnedAtDesc(user);
    }

    /**
     * Get recent points transactions
     */
    public List<LoyaltyPoints> getRecentPoints(User user) {
        return loyaltyPointsRepository.findTop10ByUserOrderByEarnedAtDesc(user);
    }

    /**
     * Generate unique referral code
     */
    public String generateReferralCode(User user) {
        String code = user.getFullName().replaceAll("\\s+", "").toUpperCase().substring(0,
                Math.min(4, user.getFullName().length()));
        code += new Random().nextInt(9999);
        return code;
    }

    /**
     * Create referral for user
     */
    public Referral createReferral(User referrer) {
        String code = generateReferralCode(referrer);
        Referral referral = new Referral(referrer, code);
        return referralRepository.save(referral);
    }

    /**
     * Process referral code during signup
     */
    public void processReferralCode(User newUser, String referralCode) {
        if (referralCode != null && !referralCode.isEmpty()) {
            Optional<Referral> referralOpt = referralRepository.findByReferralCode(referralCode);
            if (referralOpt.isPresent()) {
                Referral referral = referralOpt.get();
                referral.setReferredUser(newUser);
                referral.setStatus("COMPLETED");
                referralRepository.save(referral);

                // Award points to Referrer
                awardReferralBonus(referral.getReferrer(), newUser);

                // Award points to Referee (New User)
                int referralBonus = getIntSetting("REFERRAL_BONUS", DEFAULT_REFERRAL_BONUS);
                LoyaltyPoints refereePoints = new LoyaltyPoints(
                        newUser,
                        referralBonus,
                        "REFERRAL_BONUS",
                        "Bonus for joining via referral");
                loyaltyPointsRepository.save(refereePoints);

                // Send Notification
                createNotification(newUser, "Referral Bonus",
                        "You received " + referralBonus + " extra points for joining via referral!");
            }
        }
    }

    private void createNotification(User user, String title, String message) {
        com.busreservation.model.Notification notification = new com.busreservation.model.Notification(
                user, title, message, "SUCCESS");
        notificationRepository.save(notification);
    }

    /**
     * Get user's referrals
     */
    public List<Referral> getUserReferrals(User user) {
        return referralRepository.findByReferrerOrderByCreatedAtDesc(user);
    }

    /**
     * Get referral statistics
     */
    public long getSuccessfulReferralsCount(User user) {
        return referralRepository.countByReferrerAndStatus(user, "REWARDED");
    }

    /**
     * Redeem points (deduct points)
     */
    public void redeemPoints(User user, int points, String description) {
        LoyaltyPoints redemption = new LoyaltyPoints(
                user,
                -points, // Negative points for redemption
                "REDEMPTION",
                description);
        loyaltyPointsRepository.save(redemption);
    }

    /**
     * Calculate discount from points
     */
    public double calculateDiscount(int points) {
        double redemptionValue = getDoubleSetting("REDEMPTION_VALUE", DEFAULT_REDEMPTION_VALUE);
        return points * redemptionValue;
    }

    // Helper methods for Settings
    public int getIntSetting(String key, int defaultValue) {
        return loyaltySettingRepository.findBySettingKey(key)
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(defaultValue);
    }

    public double getDoubleSetting(String key, double defaultValue) {
        return loyaltySettingRepository.findBySettingKey(key)
                .map(s -> Double.parseDouble(s.getSettingValue()))
                .orElse(defaultValue);
    }

    public void updateSetting(String key, String value) {
        LoyaltySetting setting = loyaltySettingRepository.findBySettingKey(key)
                .orElse(new LoyaltySetting(key, value, ""));
        setting.setSettingValue(value);
        loyaltySettingRepository.save(setting);
    }
}
