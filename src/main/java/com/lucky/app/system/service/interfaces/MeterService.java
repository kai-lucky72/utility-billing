package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.MeterRequest;
import com.lucky.app.system.dto.response.MeterResponse;
import com.lucky.app.system.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

/** Contract for meter CRUD, activation, and per-customer/owner queries. */
public interface MeterService {
    MeterResponse create(MeterRequest request);
    PagedResponse<MeterResponse> getAll(Pageable pageable);
    PagedResponse<MeterResponse> getActive(Pageable pageable);
    MeterResponse getById(Long id);
    PagedResponse<MeterResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<MeterResponse> getMyMeters(Pageable pageable);
    MeterResponse update(Long id, MeterRequest request);
    MeterResponse activate(Long id);
    MeterResponse deactivate(Long id);
}
