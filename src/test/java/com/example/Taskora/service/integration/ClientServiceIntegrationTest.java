package com.example.Taskora.service.integration;

import com.example.Taskora.dto.request.CreateClientRequest;
import com.example.Taskora.dto.request.CreateUserRequest;
import com.example.Taskora.dto.response.ClientResponse;
import com.example.Taskora.dto.response.UserResponse;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.service.ClientService;
import com.example.Taskora.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("ClientService Integration Tests")
class ClientServiceIntegrationTest {

    @Autowired private ClientService clientService;
    @Autowired private UserService userService;

    private Long userId;

    @BeforeEach
    void createUser() {
        CreateUserRequest userReq = new CreateUserRequest();
        userReq.setFullName("Test Freelancer");
        userReq.setEmail("freelancer-client-test@example.com");
        userReq.setPassword("password");
        UserResponse user = userService.createUser(userReq);
        userId = user.getId();
    }

    private CreateClientRequest buildClientRequest(String name, String email) {
        CreateClientRequest req = new CreateClientRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPhone("+1-555-0000");
        req.setCompany(name + " Corp");
        req.setAddress("123 Test St");
        req.setUserId(userId);
        return req;
    }

    // ---- createClient ----

    @Test
    @DisplayName("createClient: persists client linked to user")
    void createClient_persistsClient() {
        ClientResponse result = clientService.createClient(buildClientRequest("Acme", "acme@example.com"));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Acme");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUserFullName()).isEqualTo("Test Freelancer");
    }

    @Test
    @DisplayName("createClient: throws ResourceNotFoundException when user does not exist")
    void createClient_userNotFound_throwsException() {
        CreateClientRequest req = buildClientRequest("Ghost Client", "ghost@example.com");
        req.setUserId(Long.MAX_VALUE);

        assertThatThrownBy(() -> clientService.createClient(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getClientById ----

    @Test
    @DisplayName("getClientById: retrieves persisted client")
    void getClientById_returnsClient() {
        ClientResponse created = clientService.createClient(buildClientRequest("Beta Corp", "beta@example.com"));

        ClientResponse found = clientService.getClientById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Beta Corp");
    }

    @Test
    @DisplayName("getClientById: throws ResourceNotFoundException for unknown id")
    void getClientById_notFound_throwsException() {
        assertThatThrownBy(() -> clientService.getClientById(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getAllClients ----

    @Test
    @DisplayName("getAllClients: returns all persisted clients")
    void getAllClients_returnsClients() {
        clientService.createClient(buildClientRequest("Client One", "c1@example.com"));
        clientService.createClient(buildClientRequest("Client Two", "c2@example.com"));

        Page<ClientResponse> page = clientService.getAllClients(PageRequest.of(0, 50));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    // ---- searchClients ----

    @Test
    @DisplayName("searchClients: finds clients by partial name (case-insensitive)")
    void searchClients_findsByPartialName() {
        clientService.createClient(buildClientRequest("Delta Technologies", "delta@example.com"));
        clientService.createClient(buildClientRequest("Omega Solutions", "omega@example.com"));

        Page<ClientResponse> results = clientService.searchClients("delta", PageRequest.of(0, 10));

        assertThat(results.getContent()).anyMatch(c -> c.getName().equals("Delta Technologies"));
        assertThat(results.getContent()).noneMatch(c -> c.getName().equals("Omega Solutions"));
    }

    // ---- getClientsByUser ----

    @Test
    @DisplayName("getClientsByUser: returns only clients belonging to the specified user")
    void getClientsByUser_returnsUserClients() {
        // Create a second user
        CreateUserRequest secondUserReq = new CreateUserRequest();
        secondUserReq.setFullName("Second User");
        secondUserReq.setEmail("second-user@example.com");
        secondUserReq.setPassword("password");
        Long secondUserId = userService.createUser(secondUserReq).getId();

        clientService.createClient(buildClientRequest("My Client", "my@example.com"));

        CreateClientRequest otherReq = buildClientRequest("Other Client", "other@example.com");
        otherReq.setUserId(secondUserId);
        clientService.createClient(otherReq);

        Page<ClientResponse> myClients = clientService.getClientsByUser(userId, PageRequest.of(0, 10));

        assertThat(myClients.getContent()).allMatch(c -> c.getUserId().equals(userId));
        assertThat(myClients.getContent()).anyMatch(c -> c.getName().equals("My Client"));
    }

    // ---- updateClient ----

    @Test
    @DisplayName("updateClient: updates client name and email")
    void updateClient_updatesFields() {
        ClientResponse created = clientService.createClient(buildClientRequest("Old Name", "old@example.com"));

        CreateClientRequest updateReq = new CreateClientRequest();
        updateReq.setName("New Name");
        updateReq.setEmail("new@example.com");
        updateReq.setUserId(userId);

        ClientResponse updated = clientService.updateClient(created.getId(), updateReq);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("updateClient: throws ResourceNotFoundException when client does not exist")
    void updateClient_notFound_throwsException() {
        CreateClientRequest req = buildClientRequest("Ghost", "ghost@example.com");

        assertThatThrownBy(() -> clientService.updateClient(Long.MAX_VALUE, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteClient ----

    @Test
    @DisplayName("deleteClient: removes client from database")
    void deleteClient_removesClient() {
        ClientResponse created = clientService.createClient(buildClientRequest("To Delete", "delete@example.com"));

        clientService.deleteClient(created.getId());

        assertThatThrownBy(() -> clientService.getClientById(created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteClient: throws ResourceNotFoundException for unknown id")
    void deleteClient_notFound_throwsException() {
        assertThatThrownBy(() -> clientService.deleteClient(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
