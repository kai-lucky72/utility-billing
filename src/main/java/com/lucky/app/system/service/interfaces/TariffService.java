package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.TariffRequest;
import com.lucky.app.system.dto.request.TariffTierRequest;
import com.lucky.app.system.dto.response.TariffResponse;
import java.util.List;

public interface TariffService {
    TariffResponse create(TariffRequest request);
    List<TariffResponse> getAll();
    TariffResponse getById(Long id);
    TariffResponse addTier(Long tariffId, TariffTierRequest request);
    TariffResponse deactivate(Long id);
}
