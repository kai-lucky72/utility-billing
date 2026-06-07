package com.lucky.app.system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Operator payload to capture a reading: meter, current reading (&gt; 0), and a non-future reading date. */
public record MeterReadingRequest(
        @NotNull(message = "Meter ID is required")
        Long meterId,
        @NotNull(message = "Current reading is required")
        @Positive(message = "Current reading must be greater than zero")
        BigDecimal currentReading,
        @NotNull(message = "Reading date is required")
        @PastOrPresent(message = "Reading date cannot be in the future")
        LocalDate readingDate
) {
}
