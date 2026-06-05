package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.CreateStaffUserRequest;
import com.lucky.app.system.dto.response.UserResponse;
import java.util.List;

public interface UserAdminService {
    UserResponse createStaff(CreateStaffUserRequest request);
    List<UserResponse> getAllStaffUsers();
    List<UserResponse> getCustomerUsers(boolean unlinkedOnly, String search);
    UserResponse activate(Long id);
    UserResponse deactivate(Long id);
}
