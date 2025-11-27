package com.busreservation.repository;

import com.busreservation.model.Referral;
import com.busreservation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findByReferrerOrderByCreatedAtDesc(User referrer);

    Optional<Referral> findByReferralCode(String referralCode);

    Optional<Referral> findByReferredUser(User referredUser);

    long countByReferrerAndStatus(User referrer, String status);
}
