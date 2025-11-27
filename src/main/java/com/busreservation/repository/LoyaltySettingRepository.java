package com.busreservation.repository;

import com.busreservation.model.LoyaltySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LoyaltySettingRepository extends JpaRepository<LoyaltySetting, Long> {
    Optional<LoyaltySetting> findBySettingKey(String settingKey);
}
