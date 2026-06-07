package com.lucky.app.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One pricing band of a TIERED tariff: units from minUnits up to maxUnits (null = unbounded top
 * tier) are charged at ratePerUnit. Tiers must be contiguous and start at 0.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tariff_tiers")
public class TariffTier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal minUnits;

    @Column(precision = 19, scale = 4)
    private BigDecimal maxUnits;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal ratePerUnit;
}
