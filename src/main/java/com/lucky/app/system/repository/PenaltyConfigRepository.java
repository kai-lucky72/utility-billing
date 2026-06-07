package com.lucky.app.system.repository;

import com.lucky.app.system.entity.PenaltyConfig;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link PenaltyConfig}s: resolves the active penalty rule effective on a given date. */
public interface PenaltyConfigRepository extends JpaRepository<PenaltyConfig, Long> {
    Optional<PenaltyConfig> findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByIdDesc(LocalDate effectiveDate);
    Optional<PenaltyConfig> findFirstByActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByIdDesc(
            LocalDate effectiveDate1, LocalDate effectiveDate2);
}
