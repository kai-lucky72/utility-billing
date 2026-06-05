package com.lucky.app.system.repository;

import com.lucky.app.system.entity.Tariff;
import com.lucky.app.system.entity.TariffTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffTierRepository extends JpaRepository<TariffTier, Long> {
    List<TariffTier> findByTariffOrderByMinUnitsAsc(Tariff tariff);
}
