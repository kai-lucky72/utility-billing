package com.lucky.app.system.dto.response;

import com.lucky.app.system.enums.NotificationStatus;
import com.lucky.app.system.enums.NotificationType;
import java.time.LocalDateTime;

/** API view of a notification: linked bill, message text, type, and read/unread status. */
public record NotificationResponse(
        Long id,
        Long customerId,
        Long billId,
        String billReference,
        String message,
        NotificationType notificationType,
        NotificationStatus status,
        LocalDateTime createdAt
) {
}
