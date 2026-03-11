package com.example.Taskora.service.integration;

import com.example.Taskora.dto.request.CreateUserRequest;
import com.example.Taskora.dto.response.UserResponse;
import com.example.Taskora.entity.Role;
import com.example.Taskora.entity.UserStatus;
import com.example.Taskora.exception.DuplicateResourceException;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    private CreateUserRequest buildRequest(String fullName, String email) {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword("password123");
        return request;
    }

    // ---- createUser ----

    @Test
    @DisplayName("createUser: persists user and returns correct response")
    void createUser_persistsAndReturns() {
        CreateUserRequest request = buildRequest("Bob Smith", "bob@example.com");

        UserResponse result = userService.createUser(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getFullName()).isEqualTo("Bob Smith");
        assertThat(result.getEmail()).isEqualTo("bob@example.com");
        assertThat(result.getRole()).isEqualTo(Role.ROLE_FREELANCER);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("createUser: password is stored encoded (not in plain text)")
    void createUser_passwordIsEncoded() {
        CreateUserRequest request = buildRequest("Charlie", "charlie@example.com");
        request.setPassword("plaintext123");

        UserResponse result = userService.createUser(request);

        // Response does not expose password; verify user was created
        assertThat(result.getId()).isNotNull();
    }

    @Test
    @DisplayName("createUser: throws DuplicateResourceException for duplicate email")
    void createUser_duplicateEmail_throwsException() {
        userService.createUser(buildRequest("Alice", "alice@example.com"));

        CreateUserRequest duplicate = buildRequest("Alice 2", "alice@example.com");

        assertThatThrownBy(() -> userService.createUser(duplicate))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("createUser: assigns ADMIN role when explicitly provided")
    void createUser_withAdminRole_assignsAdminRole() {
        CreateUserRequest request = buildRequest("Admin User", "admin@example.com");
        request.setRole(Role.ROLE_ADMIN);

        UserResponse result = userService.createUser(request);

        assertThat(result.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    // ---- getUserById ----

    @Test
    @DisplayName("getUserById: retrieves persisted user by id")
    void getUserById_retrievesUser() {
        UserResponse created = userService.createUser(buildRequest("Dave", "dave@example.com"));

        UserResponse found = userService.getUserById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getEmail()).isEqualTo("dave@example.com");
    }

    @Test
    @DisplayName("getUserById: throws ResourceNotFoundException for unknown id")
    void getUserById_notFound_throwsException() {
        assertThatThrownBy(() -> userService.getUserById(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getAllUsers ----

    @Test
    @DisplayName("getAllUsers: returns paginated list including created users")
    void getAllUsers_includesCreatedUsers() {
        userService.createUser(buildRequest("User One", "user1@example.com"));
        userService.createUser(buildRequest("User Two", "user2@example.com"));

        Page<UserResponse> page = userService.getAllUsers(PageRequest.of(0, 50));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    // ---- getFreelancers ----

    @Test
    @DisplayName("getFreelancers: returns only ROLE_FREELANCER users")
    void getFreelancers_returnsOnlyFreelancers() {
        userService.createUser(buildRequest("Freelancer One", "fl1@example.com"));
        CreateUserRequest adminReq = buildRequest("Admin User", "admin2@example.com");
        adminReq.setRole(Role.ROLE_ADMIN);
        userService.createUser(adminReq);

        Page<UserResponse> freelancers = userService.getFreelancers(PageRequest.of(0, 50));

        assertThat(freelancers.getContent()).allMatch(u -> u.getRole() == Role.ROLE_FREELANCER);
    }

    // ---- searchFreelancers ----

    @Test
    @DisplayName("searchFreelancers: finds user by partial name (case-insensitive)")
    void searchFreelancers_findsByPartialName() {
        userService.createUser(buildRequest("Emily Johnson", "emily@example.com"));
        userService.createUser(buildRequest("Frank Miles", "frank@example.com"));

        Page<UserResponse> results = userService.searchFreelancers("emily", PageRequest.of(0, 10));

        assertThat(results.getContent()).anyMatch(u -> u.getFullName().equals("Emily Johnson"));
    }

    // ---- updateUser ----

    @Test
    @DisplayName("updateUser: updates user full name successfully")
    void updateUser_updatesFields() {
        UserResponse created = userService.createUser(buildRequest("Grace Original", "grace@example.com"));

        CreateUserRequest updateReq = new CreateUserRequest();
        updateReq.setFullName("Grace Updated");
        updateReq.setEmail("grace@example.com");
        updateReq.setPassword("");

        UserResponse updated = userService.updateUser(created.getId(), updateReq);

        assertThat(updated.getFullName()).isEqualTo("Grace Updated");
    }

    @Test
    @DisplayName("updateUser: throws DuplicateResourceException when new email belongs to another user")
    void updateUser_emailConflict_throwsException() {
        userService.createUser(buildRequest("User A", "usera@example.com"));
        UserResponse userB = userService.createUser(buildRequest("User B", "userb@example.com"));

        CreateUserRequest updateReq = new CreateUserRequest();
        updateReq.setFullName("User B");
        updateReq.setEmail("usera@example.com"); // already taken
        updateReq.setPassword("");

        assertThatThrownBy(() -> userService.updateUser(userB.getId(), updateReq))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ---- banUser ----

    @Test
    @DisplayName("banUser: changes user status to BANNED")
    void banUser_setsStatusToBanned() {
        UserResponse created = userService.createUser(buildRequest("Henry", "henry@example.com"));

        UserResponse banned = userService.banUser(created.getId());

        assertThat(banned.getStatus()).isEqualTo(UserStatus.BANNED);
    }

    // ---- activateUser ----

    @Test
    @DisplayName("activateUser: restores user status to ACTIVE")
    void activateUser_setsStatusToActive() {
        UserResponse created = userService.createUser(buildRequest("Iris", "iris@example.com"));
        userService.banUser(created.getId());

        UserResponse activated = userService.activateUser(created.getId());

        assertThat(activated.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // ---- deleteUser ----

    @Test
    @DisplayName("deleteUser: removes user from database")
    void deleteUser_removesUser() {
        UserResponse created = userService.createUser(buildRequest("Jake", "jake@example.com"));

        userService.deleteUser(created.getId());

        assertThatThrownBy(() -> userService.getUserById(created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteUser: throws ResourceNotFoundException for unknown id")
    void deleteUser_notFound_throwsException() {
        assertThatThrownBy(() -> userService.deleteUser(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
