package com.lucky.app.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** API view of a meter reading: values, period, who captured it, and the bill it generated (if any). */
public record MeterReadingResponse(
        Long id,
        Long meterId,
        String meterNumber,
        BigDecimal previousReading,
        BigDecimal currentReading,
        BigDecimal consumption,
        LocalDate readingDate,
        Integer billingMonth,
        Integer billingYear,
        Long capturedById,
        String capturedByName,
        String generatedBillReference,
        LocalDateTime createdAt
) {
}
