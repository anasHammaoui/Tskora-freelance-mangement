package com.example.Taskora.service;

import com.example.Taskora.dto.request.CreateUserRequest;
import com.example.Taskora.dto.response.UserResponse;
import com.example.Taskora.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Long id);
    Page<UserResponse> getAllUsers(Pageable pageable);
    Page<UserResponse> getFreelancers(Pageable pageable);
    Page<UserResponse> searchFreelancers(String name, Pageable pageable);
    Page<UserResponse> getFreelancersByStatus(UserStatus status, Pageable pageable);
    UserResponse updateUser(Long id, CreateUserRequest request);
    UserResponse banUser(Long id);
    UserResponse activateUser(Long id);
    void deleteUser(Long id);
}
