package com.lucky.app.system.entity;

import com.lucky.app.system.enums.MeterType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A versioned fixed service charge per meter type, added to every bill on top of consumption
 * cost. Selected by the effectiveFrom/effectiveTo window at billing time.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fixed_charges")
public class FixedCharge extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType meterType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active;
}
