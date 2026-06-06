package com.lucky.app.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lucky.app.system.dto.request.MeterReadingRequest;
import com.lucky.app.system.dto.request.PaymentRequest;
import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.FixedCharge;
import com.lucky.app.system.entity.Meter;
import com.lucky.app.system.entity.Tariff;
import com.lucky.app.system.entity.TaxConfig;
import com.lucky.app.system.entity.User;
import com.lucky.app.system.enums.CustomerStatus;
import com.lucky.app.system.enums.MeterStatus;
import com.lucky.app.system.enums.MeterType;
import com.lucky.app.system.enums.PaymentMethod;
import com.lucky.app.system.enums.Role;
import com.lucky.app.system.enums.TariffType;
import com.lucky.app.system.enums.UserStatus;
import com.lucky.app.system.exception.BusinessRuleException;
import com.lucky.app.system.repository.BillRepository;
import com.lucky.app.system.repository.CustomerRepository;
import com.lucky.app.system.repository.FixedChargeRepository;
import com.lucky.app.system.repository.MeterRepository;
import com.lucky.app.system.repository.MeterReadingRepository;
import com.lucky.app.system.repository.TariffRepository;
import com.lucky.app.system.repository.TaxConfigRepository;
import com.lucky.app.system.repository.UserRepository;
import com.lucky.app.system.service.interfaces.BillingService;
import com.lucky.app.system.service.interfaces.MeterReadingService;
import com.lucky.app.system.service.interfaces.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class TimelineValidationIntegrationTest {

    @Autowired
    private MeterReadingService meterReadingService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private MeterRepository meterRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private TariffRepository tariffRepository;

    @Autowired
    private FixedChargeRepository fixedChargeRepository;

    @Autowired
    private TaxConfigRepository taxConfigRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void rejectsReadingBeforeMeterInstallationDate() {
        TestFixture fixture = createFixture();
        authenticateAs(fixture.operator());

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> meterReadingService.create(
                new MeterReadingRequest(
                        fixture.meter().getId(),
                        new BigDecimal("100"),
                        fixture.meter().getInstallationDate().minusDays(1)
                )
        ));

        assertEquals("Reading date cannot be before the meter installation date", exception.getMessage());
    }

    @Test
    @Transactional
    void rejectsOutOfOrderBackdatedReadingAfterLaterReadingExists() {
        TestFixture fixture = createFixture();
        authenticateAs(fixture.operator());

        meterReadingService.create(new MeterReadingRequest(
                fixture.meter().getId(),
                new BigDecimal("100"),
                fixture.meter().getInstallationDate().plusMonths(1)
        ));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> meterReadingService.create(
                new MeterReadingRequest(
                        fixture.meter().getId(),
                        new BigDecimal("150"),
                        fixture.meter().getInstallationDate().plusDays(10)
                )
        ));

        assertEquals("Reading date must be later than the latest existing reading date for this meter", exception.getMessage());
    }

    @Test
    @Transactional
    void rejectsPaymentDatedBeforeBillIssueOrApprovalDate() {
        TestFixture fixture = createFixture();
        authenticateAs(fixture.operator());

        meterReadingService.create(new MeterReadingRequest(
                fixture.meter().getId(),
                new BigDecimal("100"),
                LocalDate.now().minusDays(2)
        ));

        var latestReading = meterReadingRepository.findTopByMeterOrderByReadingDateDescIdDesc(fixture.meter())
                .orElseThrow();
        Long billId = billRepository.findByMeterReading(latestReading).orElseThrow().getId();

        authenticateAs(fixture.finance());
        var approvedBill = billingService.approve(billId);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> paymentService.create(
                new PaymentRequest(
                        approvedBill.billReference(),
                        new BigDecimal("1000"),
                        PaymentMethod.CASH,
                        LocalDate.now().minusDays(10)
                )
        ));

        assertEquals("Payment date cannot be earlier than the bill issue date", exception.getMessage());
    }

    private TestFixture createFixture() {
        User operator = saveUser("operator.timeline@example.com", Role.ROLE_OPERATOR);
        User finance = saveUser("finance.timeline@example.com", Role.ROLE_FINANCE);
        User customerUser = saveUser("customer.timeline@example.com", Role.ROLE_CUSTOMER);

        Customer customer = new Customer();
        customer.setFullName("Timeline Customer");
        customer.setNationalId("1199080076500001");
        customer.setEmail(customerUser.getEmail());
        customer.setPhoneNumber("0781234567");
        customer.setAddress("Kigali");
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setUser(customerUser);
        Customer savedCustomer = customerRepository.save(customer);

        Meter meter = new Meter();
        meter.setMeterNumber("TML-0001");
        meter.setMeterType(MeterType.WATER);
        meter.setInstallationDate(LocalDate.now().minusMonths(2));
        meter.setStatus(MeterStatus.ACTIVE);
        meter.setCustomer(savedCustomer);
        Meter savedMeter = meterRepository.save(meter);

        Tariff tariff = new Tariff();
        tariff.setName("Timeline Water Tariff");
        tariff.setMeterType(MeterType.WATER);
        tariff.setTariffType(TariffType.FLAT);
        tariff.setRatePerUnit(new BigDecimal("300"));
        tariff.setVersion(1);
        tariff.setEffectiveFrom(LocalDate.now().minusYears(1));
        tariff.setActive(true);
        tariffRepository.save(tariff);

        FixedCharge fixedCharge = new FixedCharge();
        fixedCharge.setMeterType(MeterType.WATER);
        fixedCharge.setAmount(new BigDecimal("1000"));
        fixedCharge.setVersion(1);
        fixedCharge.setEffectiveFrom(LocalDate.now().minusYears(1));
        fixedCharge.setActive(true);
        fixedChargeRepository.save(fixedCharge);

        TaxConfig taxConfig = new TaxConfig();
        taxConfig.setName("VAT");
        taxConfig.setPercentage(new BigDecimal("18"));
        taxConfig.setEffectiveFrom(LocalDate.now().minusYears(1));
        taxConfig.setActive(true);
        taxConfigRepository.save(taxConfig);

        return new TestFixture(operator, finance, customerUser, savedCustomer, savedMeter);
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setFullName(role.name());
        user.setEmail(email);
        user.setPhoneNumber("0780000000");
        user.setPassword("encoded");
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        return userRepository.save(user);
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, user.getAuthorities())
        );
    }

    private record TestFixture(User operator, User finance, User customerUser, Customer customer, Meter meter) {
    }
}
