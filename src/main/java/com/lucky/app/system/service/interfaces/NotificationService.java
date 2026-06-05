package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.response.NotificationResponse;
import com.lucky.app.system.dto.response.PagedResponse;
import com.lucky.app.system.entity.Bill;
import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    NotificationResponse create(Customer customer, Bill bill, String message, NotificationType type);
    PagedResponse<NotificationResponse> getAll(Pageable pageable);
    PagedResponse<NotificationResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable);
    NotificationResponse markAsRead(Long id);
}
