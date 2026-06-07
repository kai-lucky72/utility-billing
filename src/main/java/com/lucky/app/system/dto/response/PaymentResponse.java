package com.lucky.app.system.dto.response;

import com.lucky.app.system.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** API view of a payment: amount, method, date, the bill/customer it applies to, and who recorded it. */
public record PaymentResponse(
        Long id,
        String paymentReference,
        Long billId,
        String billReference,
        Long customerId,
        String customerName,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        LocalDate paymentDate,
        Long recordedById,
        String recordedByName,
        LocalDateTime createdAt
) {
}
