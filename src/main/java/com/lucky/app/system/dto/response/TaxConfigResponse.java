package com.lucky.app.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** API view of a tax: name, percentage, active flag, and effective window. */
public record TaxConfigResponse(
        Long id,
        String name,
        BigDecimal percentage,
        boolean active,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
