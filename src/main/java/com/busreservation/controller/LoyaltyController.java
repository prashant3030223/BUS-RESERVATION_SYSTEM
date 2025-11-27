package com.busreservation.controller;

import com.busreservation.model.LoyaltyPoints;
import com.busreservation.model.Referral;
import com.busreservation.model.User;
import com.busreservation.service.LoyaltyService;
import com.busreservation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/loyalty")
public class LoyaltyController {

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String loyaltyDashboard(Model model, Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());

        // Get user's total points
        int totalPoints = loyaltyService.getTotalPoints(currentUser);

        // Get points history
        List<LoyaltyPoints> pointsHistory = loyaltyService.getRecentPoints(currentUser);

        // Get or create referral code
        List<Referral> userReferrals = loyaltyService.getUserReferrals(currentUser);
        String referralCode = "";
        if (userReferrals.isEmpty()) {
            Referral newReferral = loyaltyService.createReferral(currentUser);
            referralCode = newReferral.getReferralCode();
        } else {
            referralCode = userReferrals.get(0).getReferralCode();
        }

        // Get successful referrals count
        long successfulReferrals = loyaltyService.getSuccessfulReferralsCount(currentUser);

        // Calculate available discount
        double availableDiscount = loyaltyService.calculateDiscount(totalPoints);

        model.addAttribute("totalPoints", totalPoints);
        model.addAttribute("pointsHistory", pointsHistory);
        model.addAttribute("referralCode", referralCode);
        model.addAttribute("successfulReferrals", successfulReferrals);
        model.addAttribute("availableDiscount", availableDiscount);
        model.addAttribute("user", currentUser);

        return "loyalty-dashboard";
    }
}
