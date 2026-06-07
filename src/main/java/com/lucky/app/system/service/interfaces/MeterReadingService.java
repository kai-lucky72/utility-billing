package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.MeterReadingRequest;
import com.lucky.app.system.dto.response.MeterReadingResponse;
import com.lucky.app.system.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

/** Contract for capturing and querying meter readings. */
public interface MeterReadingService {
    MeterReadingResponse create(MeterReadingRequest request);
    PagedResponse<MeterReadingResponse> getAll(Pageable pageable);
    MeterReadingResponse getById(Long id);
    PagedResponse<MeterReadingResponse> getByMeter(Long meterId, Pageable pageable);
    PagedResponse<MeterReadingResponse> getMonthly(Integer month, Integer year, Pageable pageable);
}
