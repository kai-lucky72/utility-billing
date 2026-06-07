package com.lucky.app.system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Admin payload to add a pricing band to a tiered tariff (minUnits, optional maxUnits, ratePerUnit). */
public record TariffTierRequest(
        @NotNull(message = "Minimum units are required")
        @PositiveOrZero(message = "Minimum units must be zero or positive")
        BigDecimal minUnits,
        @Positive(message = "Maximum units must be positive")
        BigDecimal maxUnits,
        @NotNull(message = "Rate per unit is required")
        @Positive(message = "Rate per unit must be positive")
        BigDecimal ratePerUnit
) {
}
