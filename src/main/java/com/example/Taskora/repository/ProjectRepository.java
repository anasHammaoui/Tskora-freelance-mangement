package com.example.Taskora.repository;

import com.example.Taskora.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByUserId(Long userId, Pageable pageable);
    Page<Project> findByClientId(Long clientId, Pageable pageable);
}
