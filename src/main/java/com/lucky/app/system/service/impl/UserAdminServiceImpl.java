package com.lucky.app.system.service.impl;

import com.lucky.app.system.dto.request.CreateStaffUserRequest;
import com.lucky.app.system.dto.response.UserResponse;
import com.lucky.app.system.entity.User;
import com.lucky.app.system.enums.Role;
import com.lucky.app.system.enums.UserStatus;
import com.lucky.app.system.exception.BusinessRuleException;
import com.lucky.app.system.exception.DuplicateResourceException;
import com.lucky.app.system.exception.ResourceNotFoundException;
import com.lucky.app.system.repository.CustomerRepository;
import com.lucky.app.system.repository.UserRepository;
import com.lucky.app.system.service.interfaces.UserAdminService;
import com.lucky.app.system.util.EntityMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createStaff(CreateStaffUserRequest request) {
        if (request.role() == Role.ROLE_CUSTOMER) {
            throw new BusinessRuleException("Staff creation endpoint cannot create customer users");
        }
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setPhoneNumber(request.phoneNumber());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        return EntityMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public List<UserResponse> getAllStaffUsers() {
        return userRepository.findAllByRoleIn(List.of(Role.ROLE_ADMIN, Role.ROLE_OPERATOR, Role.ROLE_FINANCE))
                .stream()
                .map(EntityMapper::toUserResponse)
                .toList();
    }

    @Override
    public List<UserResponse> getCustomerUsers(boolean unlinkedOnly, String search) {
        String term = search == null ? "" : search.trim().toLowerCase();
        return userRepository.findAllByRole(Role.ROLE_CUSTOMER).stream()
                // unlinkedOnly: only users without a customer profile yet (candidates for direct linking).
                .filter(user -> !unlinkedOnly || !customerRepository.existsByUser(user))
                .filter(user -> term.isEmpty()
                        || user.getEmail().toLowerCase().contains(term)
                        || user.getFullName().toLowerCase().contains(term))
                .map(EntityMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        return EntityMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // There must always be at least one active admin; never let the last one be disabled.
        if (user.getRole() == Role.ROLE_ADMIN
                && user.getStatus() == UserStatus.ACTIVE
                && userRepository.countByRoleAndStatus(Role.ROLE_ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new BusinessRuleException("Cannot deactivate the last active admin");
        }
        user.setStatus(UserStatus.INACTIVE);
        return EntityMapper.toUserResponse(userRepository.save(user));
    }
}
