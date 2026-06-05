package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.FixedChargeRequest;
import com.lucky.app.system.dto.request.PenaltyConfigRequest;
import com.lucky.app.system.dto.request.TaxConfigRequest;
import com.lucky.app.system.dto.response.FixedChargeResponse;
import com.lucky.app.system.dto.response.PenaltyConfigResponse;
import com.lucky.app.system.dto.response.TaxConfigResponse;
import java.util.List;

public interface BillingConfigService {
    FixedChargeResponse createFixedCharge(FixedChargeRequest request);
    List<FixedChargeResponse> getFixedCharges();
    FixedChargeResponse getFixedCharge(Long id);
    FixedChargeResponse deactivateFixedCharge(Long id);

    TaxConfigResponse createTax(TaxConfigRequest request);
    List<TaxConfigResponse> getTaxes();
    TaxConfigResponse getTax(Long id);
    TaxConfigResponse deactivateTax(Long id);

    PenaltyConfigResponse createPenalty(PenaltyConfigRequest request);
    List<PenaltyConfigResponse> getPenalties();
    PenaltyConfigResponse getPenalty(Long id);
    PenaltyConfigResponse deactivatePenalty(Long id);
}
