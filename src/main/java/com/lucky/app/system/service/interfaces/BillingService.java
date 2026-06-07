package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.response.BillResponse;
import com.lucky.app.system.dto.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** Contract for bill generation, queries, approval/cancellation, and overdue processing. */
public interface BillingService {
    BillResponse generateBillForReadingId(Long readingId);
    List<BillResponse> generateMonthly(Integer month, Integer year);
    PagedResponse<BillResponse> getAll(Pageable pageable);
    BillResponse getById(Long id);
    BillResponse getByReference(String billReference);
    PagedResponse<BillResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<BillResponse> getMyBills(Pageable pageable);
    BillResponse approve(Long id);
    BillResponse cancel(Long id);
    List<BillResponse> processOverdueBills();
}
