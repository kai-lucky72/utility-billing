package com.lucky.app.system.repository;

import com.lucky.app.system.entity.Meter;
import com.lucky.app.system.entity.MeterReading;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link MeterReading}s: monthly-duplicate check, latest reading, and period queries. */
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    boolean existsByMeterAndBillingMonthAndBillingYear(Meter meter, Integer billingMonth, Integer billingYear);
    Optional<MeterReading> findTopByMeterOrderByReadingDateDescIdDesc(Meter meter);
    List<MeterReading> findByMeter(Meter meter);
    Page<MeterReading> findByBillingMonthAndBillingYear(Integer billingMonth, Integer billingYear, Pageable pageable);
    Page<MeterReading> findByMeter(Meter meter, Pageable pageable);
}
