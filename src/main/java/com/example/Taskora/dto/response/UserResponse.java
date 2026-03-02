package com.example.Taskora.dto.response;

import com.example.Taskora.entity.Role;
import com.example.Taskora.entity.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
}
