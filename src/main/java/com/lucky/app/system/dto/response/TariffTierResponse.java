package com.lucky.app.system.dto.response;

import java.math.BigDecimal;

/** API view of one tariff tier: unit band (min/max) and its rate per unit. */
public record TariffTierResponse(
        Long id,
        BigDecimal minUnits,
        BigDecimal maxUnits,
        BigDecimal ratePerUnit
) {
}
