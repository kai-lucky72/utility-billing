package com.lucky.app.system.dto.response;

import com.lucky.app.system.enums.MeterType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedChargeResponse(
        Long id,
        MeterType meterType,
        BigDecimal amount,
        Integer version,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active
) {
}
