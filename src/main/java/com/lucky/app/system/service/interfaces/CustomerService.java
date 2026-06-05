package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.CustomerProfileRequest;
import com.lucky.app.system.dto.request.CustomerRequest;
import com.lucky.app.system.dto.response.CustomerResponse;
import com.lucky.app.system.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerResponse create(CustomerRequest request);
    PagedResponse<CustomerResponse> getAll(Pageable pageable);
    CustomerResponse getById(Long id);
    CustomerResponse update(Long id, CustomerRequest request);
    CustomerResponse activate(Long id);
    CustomerResponse deactivate(Long id);
    CustomerResponse delete(Long id);
    CustomerResponse createMyProfile(CustomerProfileRequest request);
    CustomerResponse getMe();
}
