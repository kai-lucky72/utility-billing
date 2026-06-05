package com.lucky.app.system.repository;

import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.Bill;
import com.lucky.app.system.entity.Notification;
import com.lucky.app.system.enums.NotificationType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByCustomer(Customer customer, Pageable pageable);

    Optional<Notification> findFirstByBillAndNotificationType(Bill bill, NotificationType notificationType);
}
