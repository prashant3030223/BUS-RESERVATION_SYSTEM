package com.busreservation.controller;

import com.busreservation.model.Transaction;
import com.busreservation.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/payments")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public String showPaymentsDashboard(Model model) {
        List<Transaction> transactions = transactionService.getAllTransactions();
        Double totalRevenue = transactionService.getTotalRevenue();
        Map<String, Double> dailyRevenue = transactionService.getDailyRevenue();

        model.addAttribute("transactions", transactions);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("dailyRevenue", dailyRevenue);

        return "admin/payments";
    }
}
