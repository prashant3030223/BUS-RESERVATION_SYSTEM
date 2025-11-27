package com.busreservation.repository;

import com.busreservation.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTimestampDesc(Long userId);

    List<Transaction> findAllByOrderByTimestampDesc();

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = 'PAYMENT' AND t.status = 'SUCCESS'")
    Double getTotalRevenue();

    @Query("SELECT t FROM Transaction t WHERE t.type = 'REFUND' ORDER BY t.timestamp DESC")
    List<Transaction> findRefundTransactions();
}
