package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.PaymentRequest;
import com.lucky.app.system.dto.response.PagedResponse;
import com.lucky.app.system.dto.response.PaymentResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** Contract for recording and querying payments against bills. */
public interface PaymentService {
    PaymentResponse create(PaymentRequest request);
    PagedResponse<PaymentResponse> getAll(Pageable pageable);
    PaymentResponse getById(Long id);
    List<PaymentResponse> getByBill(Long billId);
    PagedResponse<PaymentResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<PaymentResponse> getMyPayments(Pageable pageable);
}
