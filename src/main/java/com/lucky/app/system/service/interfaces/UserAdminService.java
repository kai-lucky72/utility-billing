package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.CreateStaffUserRequest;
import com.lucky.app.system.dto.response.PagedResponse;
import com.lucky.app.system.dto.response.UserResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** Contract for admin management of staff and customer login accounts (create/list/activate). */
public interface UserAdminService {
    UserResponse createStaff(CreateStaffUserRequest request);
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);
    List<UserResponse> getAllStaffUsers();
    List<UserResponse> getCustomerUsers(boolean unlinkedOnly, String search);
    List<UserResponse> getUnlinkedCustomerUsers(String search);
    UserResponse activate(Long id);
    UserResponse deactivate(Long id);
}
