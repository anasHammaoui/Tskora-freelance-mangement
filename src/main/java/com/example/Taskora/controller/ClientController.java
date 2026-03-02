package com.example.Taskora.controller;

import com.example.Taskora.dto.request.CreateClientRequest;
import com.example.Taskora.dto.response.ClientResponse;
import com.example.Taskora.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Client Management", description = "APIs for managing clients")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(summary = "Create a new client")
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @GetMapping
    @Operation(summary = "Get all clients with optional search by name")
    public ResponseEntity<Page<ClientResponse>> getAllClients(
            @RequestParam(required = false) String search,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(clientService.searchClients(search, pageable));
        }
        return ResponseEntity.ok(clientService.getAllClients(pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all clients for a specific user")
    public ResponseEntity<Page<ClientResponse>> getClientsByUser(
            @PathVariable Long userId,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(clientService.getClientsByUser(userId, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a client")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.ok(clientService.updateClient(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a client")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
