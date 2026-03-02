package com.example.Taskora.service.impl;

import com.example.Taskora.dto.request.CreateClientRequest;
import com.example.Taskora.dto.response.ClientResponse;
import com.example.Taskora.entity.Client;
import com.example.Taskora.entity.User;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.mapper.ClientMapper;
import com.example.Taskora.repository.ClientRepository;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;

    @Override
    public ClientResponse createClient(CreateClientRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        Client client = clientMapper.toEntity(request);
        client.setUser(user);
        Client saved = clientRepository.save(client);
        return clientMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        return clientMapper.toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientResponse> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable).map(clientMapper::toResponse);
    }

    @Override
    public ClientResponse updateClient(Long id, CreateClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        clientMapper.updateEntityFromRequest(request, client);
        client.setUser(user);
        Client saved = clientRepository.save(client);
        return clientMapper.toResponse(saved);
    }

    @Override
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client", id);
        }
        clientRepository.deleteById(id);
    }
}