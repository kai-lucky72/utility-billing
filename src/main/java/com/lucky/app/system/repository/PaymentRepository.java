package com.lucky.app.system.repository;

import com.lucky.app.system.entity.Bill;
import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Payment}s: lookups by reference, bill, and customer. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    boolean existsByCustomer(Customer customer);
    List<Payment> findByBill(Bill bill);
    Page<Payment> findByCustomer(Customer customer, Pageable pageable);
}
