package com.lucky.app.system.service.impl;

import com.lucky.app.system.dto.request.AdminConvertUserToCustomerRequest;
import com.lucky.app.system.dto.request.CustomerProfileRequest;
import com.lucky.app.system.dto.request.CustomerRequest;
import com.lucky.app.system.dto.response.CustomerResponse;
import com.lucky.app.system.dto.response.PagedResponse;
import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.User;
import com.lucky.app.system.enums.CustomerStatus;
import com.lucky.app.system.enums.Role;
import com.lucky.app.system.enums.UserStatus;
import com.lucky.app.system.exception.BusinessRuleException;
import com.lucky.app.system.exception.DuplicateResourceException;
import com.lucky.app.system.exception.ResourceNotFoundException;
import com.lucky.app.system.repository.BillRepository;
import com.lucky.app.system.repository.CustomerRepository;
import com.lucky.app.system.repository.MeterRepository;
import com.lucky.app.system.repository.PaymentRepository;
import com.lucky.app.system.repository.UserRepository;
import com.lucky.app.system.service.interfaces.CustomerService;
import com.lucky.app.system.util.EntityMapper;
import com.lucky.app.system.util.PageResponseBuilder;
import com.lucky.app.system.util.PhoneNumberNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer CRUD and lifecycle: admin-managed customer records plus self-service/admin conversion
 * of a login user into a customer profile. Enforces unique national ID/email, normalizes phone
 * numbers to Rwanda format, and gates activation on an eligible, verified, active user account.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final MeterRepository meterRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        validateUniqueness(request, null);
        Customer customer = new Customer();
        apply(customer, request);
        customer.setStatus(CustomerStatus.ACTIVE);
        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Override
    public PagedResponse<CustomerResponse> getAll(Pageable pageable) {
        return PageResponseBuilder.build(customerRepository.findAll(pageable), "Customers retrieved successfully", EntityMapper::toCustomerResponse);
    }

    @Override
    public CustomerResponse getById(Long id) {
        return EntityMapper.toCustomerResponse(getCustomer(id));
    }

    @Override
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        validateUniqueness(request, id);
        apply(customer, request);
        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse activate(Long id) {
        Customer customer = getCustomer(id);
        customer.setStatus(CustomerStatus.ACTIVE);
        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse deactivate(Long id) {
        Customer customer = getCustomer(id);
        customer.setStatus(CustomerStatus.INACTIVE);
        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse delete(Long id) {
        Customer customer = getCustomer(id);
        CustomerResponse response = EntityMapper.toCustomerResponse(customer);
        boolean hasHistory = !meterRepository.findByCustomer(customer).isEmpty()
                || billRepository.existsByCustomer(customer)
                || paymentRepository.existsByCustomer(customer);
        if (hasHistory) {
            customer.setStatus(CustomerStatus.INACTIVE);
            customerRepository.save(customer);
            return EntityMapper.toCustomerResponse(customer);
        }
        customerRepository.delete(customer);
        return response;
    }

    @Override
    @Transactional
    public CustomerResponse createMyProfile(CustomerProfileRequest request) {
        User user = authenticatedUserService.currentUser();
        customerRepository.findByUser(user).ifPresent(existing -> {
            throw new BusinessRuleException("Your customer profile has already been created");
        });
        if (customerRepository.existsByNationalId(request.nationalId())) {
            throw new DuplicateResourceException("Customer with this national ID already exists");
        }

        Customer customer = new Customer();
        customer.setFullName(user.getFullName());
        customer.setNationalId(request.nationalId());
        customer.setEmail(user.getEmail());
        customer.setPhoneNumber(user.getPhoneNumber());
        customer.setAddress(request.address());
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setUser(user);
        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse createForUser(Long userId, AdminConvertUserToCustomerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        validateCustomerUserEligibility(user);
        customerRepository.findByUser(user).ifPresent(existing -> {
            throw new BusinessRuleException("This user already has a customer profile");
        });
        if (customerRepository.existsByNationalId(request.nationalId())) {
            throw new DuplicateResourceException("Customer with this national ID already exists");
        }

        Customer customer = new Customer();
        customer.setFullName(user.getFullName());
        customer.setNationalId(request.nationalId());
        customer.setEmail(user.getEmail());
        customer.setPhoneNumber(user.getPhoneNumber());
        customer.setAddress(request.address());
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setUser(user);
        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Override
    public CustomerResponse getMe() {
        return EntityMapper.toCustomerResponse(authenticatedUserService.currentCustomer());
    }

    @Override
    public PagedResponse<CustomerResponse> getPendingVerification(Pageable pageable) {
        return PageResponseBuilder.build(
                customerRepository.findAllByStatusAndUserIsNotNull(CustomerStatus.INACTIVE, pageable),
                "Pending customer verifications retrieved successfully",
                EntityMapper::toCustomerResponse
        );
    }

    @Override
    @Transactional
    public CustomerResponse activateByUserId(Long userId) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No customer profile found for this user"));
        validateCustomerUserEligibility(customer.getUser());
        customer.setStatus(CustomerStatus.ACTIVE);
        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    private Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private void validateUniqueness(CustomerRequest request, Long existingId) {
        if (existingId == null || !getCustomer(existingId).getNationalId().equals(request.nationalId())) {
            if (customerRepository.existsByNationalId(request.nationalId())) {
                throw new DuplicateResourceException("Customer with this national ID already exists");
            }
        }
        if (request.email() != null) {
            if (existingId == null || !request.email().equalsIgnoreCase(getCustomer(existingId).getEmail())) {
                if (customerRepository.existsByEmail(request.email().toLowerCase())) {
                    throw new DuplicateResourceException("Customer with this email already exists");
                }
            }
        }
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setFullName(request.fullName());
        customer.setNationalId(request.nationalId());
        customer.setEmail(request.email() == null ? null : request.email().toLowerCase());
        customer.setPhoneNumber(PhoneNumberNormalizer.toRwandaFormat(request.phoneNumber()));
        customer.setAddress(request.address());
        if (request.userId() != null) {
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            validateCustomerUserEligibility(user);
            customerRepository.findByUser(user).ifPresent(existing -> {
                if (!existing.getId().equals(customer.getId())) {
                    throw new BusinessRuleException("This user is already linked to another customer profile");
                }
            });
            customer.setUser(user);
        } else {
            customer.setUser(null);
        }
    }

    private void validateCustomerUserEligibility(User user) {
        if (user.getRole() != Role.ROLE_CUSTOMER) {
            throw new BusinessRuleException("Only customer users can be linked to customer profiles");
        }
        if (!user.isEmailVerified()) {
            throw new BusinessRuleException("Customer user must verify email before customer profile verification");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Customer user must be active before customer profile verification");
        }
    }
}
