package com.lucky.app.system.service.impl;

import com.lucky.app.system.entity.Customer;
import com.lucky.app.system.entity.User;
import com.lucky.app.system.enums.CustomerStatus;
import com.lucky.app.system.enums.UserStatus;
import com.lucky.app.system.exception.BusinessRuleException;
import com.lucky.app.system.exception.ResourceNotFoundException;
import com.lucky.app.system.exception.UnauthorizedException;
import com.lucky.app.system.repository.CustomerRepository;
import com.lucky.app.system.repository.UserRepository;
import com.lucky.app.system.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves the currently authenticated principal into domain objects: the login {@link User},
 * their linked {@link Customer}, or their customer profile only if both account and profile are
 * active. Used by self-service endpoints to scope data to the caller.
 */
@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public User currentUser() {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }

    public Customer currentCustomer() {
        User user = currentUser();
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No customer profile is linked to the current user"));
    }

    public Customer currentActiveCustomer() {
        User user = currentUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Your user account is inactive");
        }

        Customer customer = currentCustomer();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Your customer profile is pending admin verification");
        }

        return customer;
    }
}
