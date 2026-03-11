package com.example.Taskora.service.unit;

import com.example.Taskora.dto.request.CreateClientRequest;
import com.example.Taskora.dto.response.ClientResponse;
import com.example.Taskora.entity.Client;
import com.example.Taskora.entity.Role;
import com.example.Taskora.entity.User;
import com.example.Taskora.entity.UserStatus;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.mapper.ClientMapper;
import com.example.Taskora.repository.ClientRepository;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.impl.ClientServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientServiceImpl Unit Tests")
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientServiceImpl clientService;

    private User user;
    private Client client;
    private ClientResponse clientResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .fullName("Alice Martin")
                .email("alice@example.com")
                .role(Role.ROLE_FREELANCER)
                .status(UserStatus.ACTIVE)
                .build();

        client = Client.builder()
                .id(10L)
                .name("Acme Corp")
                .email("contact@acme.com")
                .phone("+1-800-0000")
                .company("Acme Corp")
                .address("123 Main St")
                .user(user)
                .build();

        clientResponse = new ClientResponse();
        clientResponse.setId(10L);
        clientResponse.setName("Acme Corp");
        clientResponse.setEmail("contact@acme.com");
        clientResponse.setUserId(1L);
        clientResponse.setUserFullName("Alice Martin");
    }

    // ---- createClient ----

    @Test
    @DisplayName("createClient: creates and saves client linked to user")
    void createClient_validRequest_returnsClientResponse() {
        CreateClientRequest request = new CreateClientRequest();
        request.setName("Acme Corp");
        request.setEmail("contact@acme.com");
        request.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(clientMapper.toEntity(request)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        ClientResponse result = clientService.createClient(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Acme Corp");
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(clientRepository).save(client);
    }

    @Test
    @DisplayName("createClient: throws ResourceNotFoundException when user not found")
    void createClient_userNotFound_throwsException() {
        CreateClientRequest request = new CreateClientRequest();
        request.setUserId(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.createClient(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(clientRepository, never()).save(any());
    }

    // ---- getClientById ----

    @Test
    @DisplayName("getClientById: returns ClientResponse for existing client")
    void getClientById_existingId_returnsClient() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        ClientResponse result = clientService.getClientById(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getClientById: throws ResourceNotFoundException when client not found")
    void getClientById_notFound_throwsException() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getAllClients ----

    @Test
    @DisplayName("getAllClients: returns paginated clients")
    void getAllClients_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(clientRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(client)));
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        Page<ClientResponse> result = clientService.getAllClients(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---- searchClients ----

    @Test
    @DisplayName("searchClients: returns clients matching name")
    void searchClients_matchingName_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(clientRepository.findByNameContainingIgnoreCase("Acme", pageable))
                .thenReturn(new PageImpl<>(List.of(client)));
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        Page<ClientResponse> result = clientService.searchClients("Acme", pageable);

        assertThat(result.getContent().get(0).getName()).isEqualTo("Acme Corp");
    }

    // ---- getClientsByUser ----

    @Test
    @DisplayName("getClientsByUser: returns clients belonging to the user")
    void getClientsByUser_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(clientRepository.findByUserId(1L, pageable)).thenReturn(new PageImpl<>(List.of(client)));
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        Page<ClientResponse> result = clientService.getClientsByUser(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---- updateClient ----

    @Test
    @DisplayName("updateClient: updates client and returns updated response")
    void updateClient_existingId_updatesClient() {
        CreateClientRequest request = new CreateClientRequest();
        request.setName("Acme Updated");
        request.setUserId(1L);

        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(clientMapper).updateEntityFromRequest(request, client);
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        ClientResponse result = clientService.updateClient(10L, request);

        assertThat(result).isNotNull();
        verify(clientMapper).updateEntityFromRequest(request, client);
    }

    @Test
    @DisplayName("updateClient: throws ResourceNotFoundException when client not found")
    void updateClient_clientNotFound_throwsException() {
        CreateClientRequest request = new CreateClientRequest();
        request.setUserId(1L);

        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateClient(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteClient ----

    @Test
    @DisplayName("deleteClient: deletes existing client")
    void deleteClient_existingId_deletesClient() {
        when(clientRepository.existsById(10L)).thenReturn(true);

        clientService.deleteClient(10L);

        verify(clientRepository).deleteById(10L);
    }

    @Test
    @DisplayName("deleteClient: throws ResourceNotFoundException when client not found")
    void deleteClient_notFound_throwsException() {
        when(clientRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> clientService.deleteClient(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(clientRepository, never()).deleteById(any());
    }
}
