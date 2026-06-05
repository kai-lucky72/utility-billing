package com.lucky.app.system.service.impl;

import com.lucky.app.system.dto.request.FixedChargeRequest;
import com.lucky.app.system.dto.request.PenaltyConfigRequest;
import com.lucky.app.system.dto.request.TaxConfigRequest;
import com.lucky.app.system.dto.response.FixedChargeResponse;
import com.lucky.app.system.dto.response.PenaltyConfigResponse;
import com.lucky.app.system.dto.response.TaxConfigResponse;
import com.lucky.app.system.entity.FixedCharge;
import com.lucky.app.system.entity.PenaltyConfig;
import com.lucky.app.system.entity.TaxConfig;
import com.lucky.app.system.exception.ResourceNotFoundException;
import com.lucky.app.system.repository.FixedChargeRepository;
import com.lucky.app.system.repository.PenaltyConfigRepository;
import com.lucky.app.system.repository.TaxConfigRepository;
import com.lucky.app.system.service.interfaces.BillingConfigService;
import com.lucky.app.system.util.EntityMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingConfigServiceImpl implements BillingConfigService {

    private final FixedChargeRepository fixedChargeRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final PenaltyConfigRepository penaltyConfigRepository;

    @Override
    @Transactional
    public FixedChargeResponse createFixedCharge(FixedChargeRequest request) {
        FixedCharge fixedCharge = new FixedCharge();
        fixedCharge.setMeterType(request.meterType());
        fixedCharge.setAmount(request.amount());
        fixedCharge.setVersion(request.version());
        fixedCharge.setEffectiveFrom(request.effectiveFrom());
        fixedCharge.setEffectiveTo(request.effectiveTo());
        fixedCharge.setActive(request.active() == null || request.active());
        return EntityMapper.toFixedChargeResponse(fixedChargeRepository.save(fixedCharge));
    }

    @Override
    public List<FixedChargeResponse> getFixedCharges() {
        return fixedChargeRepository.findAll().stream().map(EntityMapper::toFixedChargeResponse).toList();
    }

    @Override
    public FixedChargeResponse getFixedCharge(Long id) {
        return EntityMapper.toFixedChargeResponse(getFixedChargeEntity(id));
    }

    @Override
    @Transactional
    public FixedChargeResponse deactivateFixedCharge(Long id) {
        FixedCharge fixedCharge = getFixedChargeEntity(id);
        fixedCharge.setActive(false);
        return EntityMapper.toFixedChargeResponse(fixedChargeRepository.save(fixedCharge));
    }

    @Override
    @Transactional
    public TaxConfigResponse createTax(TaxConfigRequest request) {
        TaxConfig taxConfig = new TaxConfig();
        taxConfig.setName(request.name());
        taxConfig.setPercentage(request.percentage());
        taxConfig.setActive(request.active() == null || request.active());
        taxConfig.setEffectiveFrom(request.effectiveFrom());
        taxConfig.setEffectiveTo(request.effectiveTo());
        return EntityMapper.toTaxConfigResponse(taxConfigRepository.save(taxConfig));
    }

    @Override
    public List<TaxConfigResponse> getTaxes() {
        return taxConfigRepository.findAll().stream().map(EntityMapper::toTaxConfigResponse).toList();
    }

    @Override
    public TaxConfigResponse getTax(Long id) {
        return EntityMapper.toTaxConfigResponse(getTaxEntity(id));
    }

    @Override
    @Transactional
    public TaxConfigResponse deactivateTax(Long id) {
        TaxConfig taxConfig = getTaxEntity(id);
        taxConfig.setActive(false);
        return EntityMapper.toTaxConfigResponse(taxConfigRepository.save(taxConfig));
    }

    @Override
    @Transactional
    public PenaltyConfigResponse createPenalty(PenaltyConfigRequest request) {
        PenaltyConfig penaltyConfig = new PenaltyConfig();
        penaltyConfig.setName(request.name());
        penaltyConfig.setPenaltyType(request.penaltyType());
        penaltyConfig.setAmountOrPercentage(request.amountOrPercentage());
        penaltyConfig.setGracePeriodDays(request.gracePeriodDays());
        penaltyConfig.setActive(request.active() == null || request.active());
        penaltyConfig.setEffectiveFrom(request.effectiveFrom());
        penaltyConfig.setEffectiveTo(request.effectiveTo());
        return EntityMapper.toPenaltyConfigResponse(penaltyConfigRepository.save(penaltyConfig));
    }

    @Override
    public List<PenaltyConfigResponse> getPenalties() {
        return penaltyConfigRepository.findAll().stream().map(EntityMapper::toPenaltyConfigResponse).toList();
    }

    @Override
    public PenaltyConfigResponse getPenalty(Long id) {
        return EntityMapper.toPenaltyConfigResponse(getPenaltyEntity(id));
    }

    @Override
    @Transactional
    public PenaltyConfigResponse deactivatePenalty(Long id) {
        PenaltyConfig penaltyConfig = getPenaltyEntity(id);
        penaltyConfig.setActive(false);
        return EntityMapper.toPenaltyConfigResponse(penaltyConfigRepository.save(penaltyConfig));
    }

    private FixedCharge getFixedChargeEntity(Long id) {
        return fixedChargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed charge not found"));
    }

    private TaxConfig getTaxEntity(Long id) {
        return taxConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax config not found"));
    }

    private PenaltyConfig getPenaltyEntity(Long id) {
        return penaltyConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty config not found"));
    }
}
