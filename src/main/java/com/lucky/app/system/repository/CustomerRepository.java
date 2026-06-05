package com.lucky.app.system.repository;

import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByNationalId(String nationalId);
    boolean existsByEmail(String email);
    boolean existsByUser(User user);
    Optional<Customer> findByUser(User user);
    Page<Customer> findAllByStatus(com.lucky.app.system.enums.CustomerStatus status, Pageable pageable);
}
