package com.example.Taskora.service;

import com.example.Taskora.dto.request.CreateClientRequest;
import com.example.Taskora.dto.response.ClientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {
    ClientResponse createClient(CreateClientRequest request);
    ClientResponse getClientById(Long id);
    Page<ClientResponse> getAllClients(Pageable pageable);
    Page<ClientResponse> searchClients(String name, Pageable pageable);
    Page<ClientResponse> getClientsByUser(Long userId, Pageable pageable);
    ClientResponse updateClient(Long id, CreateClientRequest request);
    void deleteClient(Long id);
}
