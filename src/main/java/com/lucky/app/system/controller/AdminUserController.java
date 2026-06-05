package com.lucky.app.system.controller;

import com.lucky.app.system.dto.request.CreateStaffUserRequest;
import com.lucky.app.system.dto.response.ApiResponse;
import com.lucky.app.system.dto.response.UserResponse;
import com.lucky.app.system.service.interfaces.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Admin-only staff user management APIs")
public class AdminUserController {

    private final UserAdminService userAdminService;

    @PostMapping("/staff")
    @Operation(summary = "Create a staff user")
    public ResponseEntity<ApiResponse<UserResponse>> createStaff(@Valid @RequestBody CreateStaffUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Staff user created successfully")
                .data(userAdminService.createStaff(request))
                .build());
    }

    @GetMapping
    @Operation(summary = "List staff users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getStaffUsers() {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .message("Staff users retrieved successfully")
                .data(userAdminService.getAllStaffUsers())
                .build());
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a staff user")
    public ResponseEntity<ApiResponse<UserResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User activated successfully")
                .data(userAdminService.activate(id))
                .build());
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a staff user")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User deactivated successfully")
                .data(userAdminService.deactivate(id))
                .build());
    }
}
