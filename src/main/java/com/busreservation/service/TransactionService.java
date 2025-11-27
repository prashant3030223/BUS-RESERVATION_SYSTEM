package com.busreservation.service;

import com.busreservation.model.Booking;
import com.busreservation.model.Transaction;
import com.busreservation.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction recordPayment(Booking booking, Double amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setBooking(booking);
        transaction.setUser(booking.getUser());
        transaction.setAmount(amount);
        transaction.setType("PAYMENT");
        transaction.setStatus("SUCCESS");
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    public Transaction recordRefund(Booking booking, Double amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setBooking(booking);
        transaction.setUser(booking.getUser());
        transaction.setAmount(amount);
        transaction.setType("REFUND");
        transaction.setStatus("SUCCESS"); // Assuming instant refund for now
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc();
    }

    public List<Transaction> getUserTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public Double getTotalRevenue() {
        Double revenue = transactionRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }

    public Map<String, Double> getDailyRevenue() {
        // Simple implementation: Group by date and sum amount
        // In a real app, do this in DB query for performance
        List<Transaction> payments = transactionRepository.findAll().stream()
                .filter(t -> "PAYMENT".equals(t.getType()) && "SUCCESS".equals(t.getStatus()))
                .collect(Collectors.toList());

        return payments.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTimestamp().toLocalDate().toString(),
                        Collectors.summingDouble(Transaction::getAmount)));
    }
}
