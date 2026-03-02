package com.example.Taskora.repository;

import com.example.Taskora.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Page<Client> findByUserId(Long userId, Pageable pageable);
    Page<Client> findByUserIdAndNameContainingIgnoreCase(Long userId, String name, Pageable pageable);
    Page<Client> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
