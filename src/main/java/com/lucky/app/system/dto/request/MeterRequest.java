package com.lucky.app.system.dto.request;

import com.lucky.app.system.enums.MeterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Payload for registering a meter against a customer.
 * A meter cannot be installed in the future, so the installation date must be today or earlier.
 */
public record MeterRequest(
        @NotBlank(message = "Meter number is required")
        String meterNumber,
        @NotNull(message = "Meter type is required")
        MeterType meterType,
        @NotNull(message = "Installation date is required")
        @PastOrPresent(message = "Installation date cannot be in the future")
        LocalDate installationDate,
        @NotNull(message = "Customer ID is required")
        Long customerId
) {
}
