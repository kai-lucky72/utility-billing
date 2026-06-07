package com.lucky.app.system.entity;

import com.lucky.app.system.enums.MeterType;
import com.lucky.app.system.enums.TariffType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A versioned consumption tariff for a meter type. Either FLAT (single ratePerUnit) or TIERED
 * (priced via {@link TariffTier}s). The effectiveFrom/effectiveTo window controls which billing
 * cycles it applies to; new tariffs are restricted to future months.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tariffs")
public class Tariff extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType meterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TariffType tariffType;

    @Column(precision = 19, scale = 4)
    private BigDecimal ratePerUnit;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TariffTier> tiers = new ArrayList<>();
}
