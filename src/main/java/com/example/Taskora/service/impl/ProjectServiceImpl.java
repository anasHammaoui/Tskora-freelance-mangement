package com.example.Taskora.service.impl;

import com.example.Taskora.dto.request.CreateProjectRequest;
import com.example.Taskora.dto.response.ProjectResponse;
import com.example.Taskora.entity.Client;
import com.example.Taskora.entity.Project;
import com.example.Taskora.entity.User;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.mapper.ProjectMapper;
import com.example.Taskora.repository.ClientRepository;
import com.example.Taskora.repository.ProjectRepository;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.getClientId()));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        Project project = projectMapper.toEntity(request);
        project.setClient(client);
        project.setUser(user);
        Project saved = projectRepository.save(project);
        return projectMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable).map(projectMapper::toResponse);
    }

    @Override
    public ProjectResponse updateProject(Long id, CreateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.getClientId()));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        projectMapper.updateEntityFromRequest(request, project);
        project.setClient(client);
        project.setUser(user);
        Project saved = projectRepository.save(project);
        return projectMapper.toResponse(saved);
    }

    @Override
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        }
        projectRepository.deleteById(id);
    }
}
