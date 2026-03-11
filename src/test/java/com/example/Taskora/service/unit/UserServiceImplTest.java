package com.example.Taskora.service.unit;

import com.example.Taskora.dto.request.CreateUserRequest;
import com.example.Taskora.dto.response.UserResponse;
import com.example.Taskora.entity.Role;
import com.example.Taskora.entity.User;
import com.example.Taskora.entity.UserStatus;
import com.example.Taskora.exception.DuplicateResourceException;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.mapper.UserMapper;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .fullName("Alice Martin")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.ROLE_FREELANCER)
                .status(UserStatus.ACTIVE)
                .build();

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setFullName("Alice Martin");
        userResponse.setEmail("alice@example.com");
        userResponse.setRole(Role.ROLE_FREELANCER);
        userResponse.setStatus(UserStatus.ACTIVE);
        userResponse.setCreatedAt(LocalDateTime.now());
    }

    // ---- createUser ----

    @Test
    @DisplayName("createUser: creates user with default FREELANCER role and ACTIVE status")
    void createUser_newEmail_savesUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("Alice Martin");
        request.setEmail("alice@example.com");
        request.setPassword("password");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.createUser(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("createUser: sets default FREELANCER role when role is null")
    void createUser_nullRole_setsFreelancerRole() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@example.com");
        request.setPassword("pass");
        request.setFullName("New User");

        User noRoleUser = User.builder().email("new@example.com").password("encoded").build();

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(noRoleUser);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(noRoleUser)).thenReturn(noRoleUser);
        when(userMapper.toResponse(noRoleUser)).thenReturn(userResponse);

        userService.createUser(request);

        assertThat(noRoleUser.getRole()).isEqualTo(Role.ROLE_FREELANCER);
        assertThat(noRoleUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("createUser: throws DuplicateResourceException on duplicate email")
    void createUser_duplicateEmail_throwsException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("alice@example.com");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    // ---- getUserById ----

    @Test
    @DisplayName("getUserById: returns UserResponse when user exists")
    void getUserById_existingId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserById: throws ResourceNotFoundException when user not found")
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getAllUsers ----

    @Test
    @DisplayName("getAllUsers: returns paginated list of users")
    void getAllUsers_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@example.com");
    }

    // ---- updateUser ----

    @Test
    @DisplayName("updateUser: updates existing user fields")
    void updateUser_existingId_updatesUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("Alice Updated");
        request.setEmail("alice@example.com");
        request.setPassword("newPass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNew");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        doNothing().when(userMapper).updateEntityFromRequest(request, user);

        UserResponse result = userService.updateUser(1L, request);

        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("newPass");
    }

    @Test
    @DisplayName("updateUser: throws DuplicateResourceException when new email belongs to another user")
    void updateUser_changedEmailAlreadyExists_throwsException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("taken@example.com");
        request.setPassword("");

        user.setEmail("alice@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ---- banUser ----

    @Test
    @DisplayName("banUser: sets user status to BANNED")
    void banUser_existingId_setsBannedStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.banUser(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.BANNED);
    }

    @Test
    @DisplayName("banUser: throws ResourceNotFoundException when user not found")
    void banUser_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.banUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- activateUser ----

    @Test
    @DisplayName("activateUser: sets user status to ACTIVE")
    void activateUser_bannedUser_setsActiveStatus() {
        user.setStatus(UserStatus.BANNED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.activateUser(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // ---- deleteUser ----

    @Test
    @DisplayName("deleteUser: deletes existing user")
    void deleteUser_existingId_deletesUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser: throws ResourceNotFoundException when user not found")
    void deleteUser_notFound_throwsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }
}
