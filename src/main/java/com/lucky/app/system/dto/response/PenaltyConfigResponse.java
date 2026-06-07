package com.lucky.app.system.dto.response;

import com.lucky.app.system.enums.PenaltyType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** API view of a penalty rule: type, amount/percentage, grace days, active flag, and effective window. */
public record PenaltyConfigResponse(
        Long id,
        String name,
        PenaltyType penaltyType,
        BigDecimal amountOrPercentage,
        Integer gracePeriodDays,
        boolean active,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
