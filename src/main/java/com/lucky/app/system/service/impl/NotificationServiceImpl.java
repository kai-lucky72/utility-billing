package com.lucky.app.system.service.impl;

import com.lucky.app.system.dto.response.NotificationResponse;
import com.lucky.app.system.dto.response.PagedResponse;
import com.lucky.app.system.entity.Bill;
import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.Notification;
import com.lucky.app.system.entity.User;
import com.lucky.app.system.enums.NotificationStatus;
import com.lucky.app.system.enums.NotificationType;
import com.lucky.app.system.enums.Role;
import com.lucky.app.system.exception.ForbiddenException;
import com.lucky.app.system.exception.ResourceNotFoundException;
import com.lucky.app.system.repository.CustomerRepository;
import com.lucky.app.system.repository.NotificationRepository;
import com.lucky.app.system.service.interfaces.MailService;
import com.lucky.app.system.service.interfaces.NotificationService;
import com.lucky.app.system.util.EntityMapper;
import com.lucky.app.system.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and queries customer notifications. Every notification is persisted (in-app inbox)
 * and also emailed to the customer as a copy. Bill/payment notifications are de-duplicated per
 * (bill, type) so the DB triggers and this service never produce two rows for the same event.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final MailService mailService;

    @Override
    @Transactional
    public NotificationResponse create(Customer customer, Bill bill, String message, NotificationType type) {
        // Reuse the row a DB trigger may already have inserted for this (bill, type) instead of
        // duplicating it; otherwise create it. Either way the customer is emailed exactly once.
        Notification notification = (bill != null)
                ? notificationRepository.findFirstByBillAndNotificationType(bill, type)
                        .orElseGet(() -> saveNotification(customer, bill, message, type))
                : saveNotification(customer, bill, message, type);

        // Send the same message as an email copy (best-effort; failures are logged, not thrown).
        mailService.sendNotificationEmail(customer.getEmail(), customer.getFullName(),
                subjectFor(type), notification.getMessage());

        return EntityMapper.toNotificationResponse(notification);
    }

    private Notification saveNotification(Customer customer, Bill bill, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setCustomer(customer);
        notification.setBill(bill);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setStatus(NotificationStatus.UNREAD);
        return notificationRepository.save(notification);
    }

    /** Maps a notification type to a human-friendly email subject line. */
    private String subjectFor(NotificationType type) {
        return switch (type) {
            case BILL_GENERATED -> "Your utility bill has been generated";
            case PAYMENT_CONFIRMED -> "Payment received";
            case BILL_PAID -> "Your utility bill is fully paid";
            case BILL_OVERDUE -> "Your utility bill is overdue";
        };
    }

    @Override
    public PagedResponse<NotificationResponse> getAll(Pageable pageable) {
        return PageResponseBuilder.build(notificationRepository.findAll(pageable), "Notifications retrieved successfully", EntityMapper::toNotificationResponse);
    }

    @Override
    public PagedResponse<NotificationResponse> getByCustomer(Long customerId, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return PageResponseBuilder.build(notificationRepository.findByCustomer(customer, pageable), "Customer notifications retrieved successfully", EntityMapper::toNotificationResponse);
    }

    @Override
    public PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        Customer customer = authenticatedUserService.currentActiveCustomer();
        return PageResponseBuilder.build(notificationRepository.findByCustomer(customer, pageable), "Notifications retrieved successfully", EntityMapper::toNotificationResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        User user = authenticatedUserService.currentUser();
        if (user.getRole() == Role.ROLE_CUSTOMER) {
            Customer customer = authenticatedUserService.currentActiveCustomer();
            if (!notification.getCustomer().getId().equals(customer.getId())) {
                throw new ForbiddenException("You cannot modify another customer's notifications");
            }
        }
        notification.setStatus(NotificationStatus.READ);
        return EntityMapper.toNotificationResponse(notificationRepository.save(notification));
    }
}
