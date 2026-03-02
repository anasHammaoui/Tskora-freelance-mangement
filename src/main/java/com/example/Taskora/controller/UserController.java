package com.example.Taskora.controller;

import com.example.Taskora.dto.request.CreateUserRequest;
import com.example.Taskora.dto.response.UserResponse;
import com.example.Taskora.entity.UserStatus;
import com.example.Taskora.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users (Admin)")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Register a new user (freelancer or admin)")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @Operation(summary = "Get all users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/freelancers")
    @Operation(summary = "Get all freelancers, optionally filtered by status or search by name (Admin)")
    public ResponseEntity<Page<UserResponse>> getFreelancers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(userService.searchFreelancers(search, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(userService.getFreelancersByStatus(status, pageable));
        }
        return ResponseEntity.ok(userService.getFreelancers(pageable));
    }

    @PatchMapping("/{id}/ban")
    @Operation(summary = "Ban a user account (Admin)")
    public ResponseEntity<UserResponse> banUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.banUser(id));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate or reactivate a user account (Admin)")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
