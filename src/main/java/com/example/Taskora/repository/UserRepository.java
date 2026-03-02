package com.example.Taskora.repository;

import com.example.Taskora.entity.Role;
import com.example.Taskora.entity.User;
import com.example.Taskora.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    Page<User> findByRoleAndStatus(Role role, UserStatus status, Pageable pageable);
    Page<User> findByFullNameContainingIgnoreCase(String name, Pageable pageable);
    Page<User> findByRoleAndFullNameContainingIgnoreCase(Role role, String name, Pageable pageable);
    long countByRole(Role role);
    long countByStatus(UserStatus status);
    long countByRoleAndStatus(Role role, UserStatus status);
}
