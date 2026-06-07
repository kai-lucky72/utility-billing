package com.lucky.app.system.repository;

import com.lucky.app.system.entity.Tariff;
import com.lucky.app.system.entity.TariffTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link TariffTier}s: returns a tariff's tiers ordered by their lower bound. */
public interface TariffTierRepository extends JpaRepository<TariffTier, Long> {
    List<TariffTier> findByTariffOrderByMinUnitsAsc(Tariff tariff);
}
