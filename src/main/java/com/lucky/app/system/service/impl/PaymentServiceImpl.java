package com.lucky.app.system.service.impl;

import com.lucky.app.system.dto.request.PaymentRequest;
import com.lucky.app.system.dto.response.PagedResponse;
import com.lucky.app.system.dto.response.PaymentResponse;
import com.lucky.app.system.entity.Bill;
import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.Payment;
import com.lucky.app.system.entity.User;
import com.lucky.app.system.enums.BillStatus;
import com.lucky.app.system.enums.CustomerStatus;
import com.lucky.app.system.enums.NotificationType;
import com.lucky.app.system.enums.UserStatus;
import com.lucky.app.system.exception.BusinessRuleException;
import com.lucky.app.system.exception.ResourceNotFoundException;
import com.lucky.app.system.repository.BillRepository;
import com.lucky.app.system.repository.PaymentRepository;
import com.lucky.app.system.service.interfaces.NotificationService;
import com.lucky.app.system.service.interfaces.PaymentService;
import com.lucky.app.system.util.EntityMapper;
import com.lucky.app.system.util.PageResponseBuilder;
import com.lucky.app.system.util.ReferenceGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records payments against approved bills. Supports partial and full payments, updates the bill's
 * amountPaid/outstandingBalance, flips status to PARTIALLY_PAID or PAID, and notifies the customer.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final NotificationService notificationService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Bill bill = billRepository.findByBillReference(request.billReference())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));

        if (request.amountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero");
        }
        if (!(bill.getStatus() == BillStatus.APPROVED || bill.getStatus() == BillStatus.PARTIALLY_PAID || bill.getStatus() == BillStatus.OVERDUE)) {
            throw new BusinessRuleException("Bill must be approved before payment");
        }
        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled bills cannot be paid");
        }
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessRuleException("Paid bills cannot receive new payments");
        }
        if (request.amountPaid().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BusinessRuleException("Payment amount cannot exceed outstanding balance");
        }
        LocalDate billIssuedDate = bill.getCreatedAt().toLocalDate();
        if (request.paymentDate().isBefore(billIssuedDate)) {
            throw new BusinessRuleException("Payment date cannot be earlier than the bill issue date");
        }
        if (bill.getApprovedAt() != null && request.paymentDate().isBefore(bill.getApprovedAt().toLocalDate())) {
            throw new BusinessRuleException("Payment date cannot be earlier than the bill approval date");
        }

        User recordedBy = authenticatedUserService.currentUser();
        Customer customer = bill.getCustomer();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Payments can only be recorded for active customer profiles");
        }
        if (customer.getUser() == null || customer.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Payments can only be recorded for customers with active user accounts");
        }

        Payment payment = new Payment();
        payment.setPaymentReference("TEMP-" + System.nanoTime());
        payment.setBill(bill);
        payment.setCustomer(customer);
        payment.setAmountPaid(request.amountPaid());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentDate(request.paymentDate());
        payment.setRecordedBy(recordedBy);
        Payment saved = paymentRepository.save(payment);
        saved.setPaymentReference(ReferenceGenerator.paymentReference(request.paymentDate(), saved.getId()));
        Payment finalPayment = paymentRepository.save(saved);

        BigDecimal newAmountPaid = bill.getAmountPaid().add(request.amountPaid());
        BigDecimal newOutstanding = bill.getTotalAmount().subtract(newAmountPaid).setScale(2, RoundingMode.HALF_UP);
        bill.setAmountPaid(newAmountPaid);
        bill.setOutstandingBalance(newOutstanding);
        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
        } else {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }
        billRepository.save(bill);

        notificationService.create(customer, bill, paymentConfirmedMessage(bill, request.amountPaid()), NotificationType.PAYMENT_CONFIRMED);
        if (bill.getStatus() == BillStatus.PAID) {
            notificationService.create(customer, bill, billPaidMessage(bill), NotificationType.BILL_PAID);
        }

        return EntityMapper.toPaymentResponse(finalPayment);
    }

    @Override
    public PagedResponse<PaymentResponse> getAll(Pageable pageable) {
        return PageResponseBuilder.build(paymentRepository.findAll(pageable), "Payments retrieved successfully", EntityMapper::toPaymentResponse);
    }

    @Override
    public PaymentResponse getById(Long id) {
        return EntityMapper.toPaymentResponse(paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found")));
    }

    @Override
    public List<PaymentResponse> getByBill(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        return paymentRepository.findByBill(bill).stream().map(EntityMapper::toPaymentResponse).toList();
    }

    @Override
    public PagedResponse<PaymentResponse> getByCustomer(Long customerId, Pageable pageable) {
        Customer customer = new Customer();
        customer.setId(customerId);
        return PageResponseBuilder.build(paymentRepository.findByCustomer(customer, pageable), "Customer payments retrieved successfully", EntityMapper::toPaymentResponse);
    }

    @Override
    public PagedResponse<PaymentResponse> getMyPayments(Pageable pageable) {
        Customer customer = authenticatedUserService.currentActiveCustomer();
        return PageResponseBuilder.build(paymentRepository.findByCustomer(customer, pageable), "Payments retrieved successfully", EntityMapper::toPaymentResponse);
    }

    private String paymentConfirmedMessage(Bill bill, BigDecimal amountPaid) {
        return "Dear %s,%nYour payment of %s FRW for %d/%d utility bill has been received."
                .formatted(
                        bill.getCustomer().getFullName(),
                        amountPaid.setScale(2, RoundingMode.HALF_UP),
                        bill.getBillingMonth(),
                        bill.getBillingYear()
                );
    }

    private String billPaidMessage(Bill bill) {
        return "Dear %s,%nYour payment for %d/%d utility bill has been received. Your bill is now fully paid."
                .formatted(
                        bill.getCustomer().getFullName(),
                        bill.getBillingMonth(),
                        bill.getBillingYear()
                );
    }
}
