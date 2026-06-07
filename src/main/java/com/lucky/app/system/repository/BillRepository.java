package com.lucky.app.system.repository;

import com.lucky.app.system.entity.Bill;
import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.Meter;
import com.lucky.app.system.entity.MeterReading;
import com.lucky.app.system.enums.BillStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Bill}s: reference/reading lookups, per-customer queries, and overdue sweeps. */
public interface BillRepository extends JpaRepository<Bill, Long> {
    boolean existsByMeterAndBillingMonthAndBillingYear(Meter meter, Integer billingMonth, Integer billingYear);
    boolean existsByCustomer(Customer customer);
    Optional<Bill> findByMeterReading(MeterReading meterReading);
    Optional<Bill> findByBillReference(String billReference);
    Page<Bill> findByCustomer(Customer customer, Pageable pageable);
    Page<Bill> findByCustomerAndStatusIn(Customer customer, List<BillStatus> statuses, Pageable pageable);
    Page<Bill> findAll(Pageable pageable);
    List<Bill> findByBillingMonthAndBillingYear(Integer billingMonth, Integer billingYear);
    List<Bill> findByStatusInAndDueDateBefore(List<BillStatus> statuses, LocalDate dueDate);
}
