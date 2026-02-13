package com.example.Taskora.service;

import com.example.Taskora.dto.request.CreateProjectRequest;
import com.example.Taskora.dto.response.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    ProjectResponse createProject(CreateProjectRequest request);
    ProjectResponse getProjectById(Long id);
    Page<ProjectResponse> getAllProjects(Pageable pageable);
    ProjectResponse updateProject(Long id, CreateProjectRequest request);
    void deleteProject(Long id);
}
