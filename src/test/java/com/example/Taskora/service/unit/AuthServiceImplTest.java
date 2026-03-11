package com.example.Taskora.service.unit;

import com.example.Taskora.dto.request.LoginRequest;
import com.example.Taskora.dto.request.RegisterRequest;
import com.example.Taskora.dto.response.AuthResponse;
import com.example.Taskora.entity.Role;
import com.example.Taskora.entity.User;
import com.example.Taskora.entity.UserStatus;
import com.example.Taskora.exception.DuplicateResourceException;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.security.JwtUtil;
import com.example.Taskora.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .password("encodedPassword")
                .role(Role.ROLE_FREELANCER)
                .status(UserStatus.ACTIVE)
                .build();

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("jane@example.com")
                .password("encodedPassword")
                .roles("FREELANCER")
                .build();
    }

    // ---- login ----

    @Test
    @DisplayName("login: returns AuthResponse with token on valid credentials")
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("jane@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token-123");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getFullName()).isEqualTo("Jane Doe");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getRole()).isEqualTo("ROLE_FREELANCER");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(userDetails);
    }

    @Test
    @DisplayName("login: throws RuntimeException when user not found after authentication")
    void login_userNotFound_throwsRuntimeException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ---- register ----

    @Test
    @DisplayName("register: saves new freelancer user and returns AuthResponse")
    void register_newEmail_savesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Smith");
        request.setEmail("john@example.com");
        request.setPassword("secret");

        User savedUser = User.builder()
                .id(2L)
                .fullName("John Smith")
                .email("john@example.com")
                .password("encodedSecret")
                .role(Role.ROLE_FREELANCER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("register-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("register-token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo("ROLE_FREELANCER");

        verify(userRepository).save(argThat(u ->
                u.getRole() == Role.ROLE_FREELANCER && u.getStatus() == UserStatus.ACTIVE));
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException when email already exists")
    void register_duplicateEmail_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("password");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }
}
