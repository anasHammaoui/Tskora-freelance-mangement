package com.example.Taskora.service.unit;

import com.example.Taskora.dto.request.CreateProjectRequest;
import com.example.Taskora.dto.response.ProjectResponse;
import com.example.Taskora.entity.*;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.mapper.ProjectMapper;
import com.example.Taskora.repository.ClientRepository;
import com.example.Taskora.repository.ProjectRepository;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.impl.ProjectServiceImpl;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl Unit Tests")
class ProjectServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private User user;
    private Client client;
    private Project project;
    private ProjectResponse projectResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).fullName("Alice").email("alice@example.com")
                .role(Role.ROLE_FREELANCER).status(UserStatus.ACTIVE).build();

        client = Client.builder()
                .id(10L).name("Acme Corp").user(user).build();

        project = Project.builder()
                .id(100L).title("Website Redesign").status(ProjectStatus.EN_COURS)
                .budget(new BigDecimal("5000")).client(client).user(user).build();

        projectResponse = new ProjectResponse();
        projectResponse.setId(100L);
        projectResponse.setTitle("Website Redesign");
        projectResponse.setStatus(ProjectStatus.EN_COURS);
        projectResponse.setClientId(10L);
        projectResponse.setUserId(1L);
    }

    // ---- createProject ----

    @Test
    @DisplayName("createProject: saves project linked to client and user")
    void createProject_validRequest_savesProject() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("Website Redesign");
        request.setStatus(ProjectStatus.EN_COURS);
        request.setClientId(10L);
        request.setUserId(1L);

        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(projectResponse);

        ProjectResponse result = projectService.createProject(request);

        assertThat(result.getTitle()).isEqualTo("Website Redesign");
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.EN_COURS);
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("createProject: throws ResourceNotFoundException when client not found")
    void createProject_clientNotFound_throwsException() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setClientId(99L);
        request.setUserId(1L);

        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createProject: throws ResourceNotFoundException when user not found")
    void createProject_userNotFound_throwsException() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setClientId(10L);
        request.setUserId(99L);

        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getProjectById ----

    @Test
    @DisplayName("getProjectById: returns project when it exists")
    void getProjectById_exists_returnsProject() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(projectResponse);

        ProjectResponse result = projectService.getProjectById(100L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getProjectById: throws ResourceNotFoundException when not found")
    void getProjectById_notFound_throwsException() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getAllProjects ----

    @Test
    @DisplayName("getAllProjects: returns paginated projects")
    void getAllProjects_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(project)));
        when(projectMapper.toResponse(project)).thenReturn(projectResponse);

        Page<ProjectResponse> result = projectService.getAllProjects(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---- getProjectsByStatus ----

    @Test
    @DisplayName("getProjectsByStatus: returns projects with given status")
    void getProjectsByStatus_returnsMatchingPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projectRepository.findByStatus(ProjectStatus.EN_COURS, pageable))
                .thenReturn(new PageImpl<>(List.of(project)));
        when(projectMapper.toResponse(project)).thenReturn(projectResponse);

        Page<ProjectResponse> result = projectService.getProjectsByStatus(ProjectStatus.EN_COURS, pageable);

        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ProjectStatus.EN_COURS);
    }

    // ---- updateProject ----

    @Test
    @DisplayName("updateProject: updates existing project")
    void updateProject_existingId_updatesProject() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("Updated Title");
        request.setStatus(ProjectStatus.TERMINE);
        request.setClientId(10L);
        request.setUserId(1L);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(projectMapper).updateEntityFromRequest(request, project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(projectResponse);

        ProjectResponse result = projectService.updateProject(100L, request);

        assertThat(result).isNotNull();
        verify(projectMapper).updateEntityFromRequest(request, project);
    }

    // ---- markAsComplete ----

    @Test
    @DisplayName("markAsComplete: sets project status to TERMINE")
    void markAsComplete_existingProject_setsCompleteStatus() {
        ProjectResponse completedResponse = new ProjectResponse();
        completedResponse.setStatus(ProjectStatus.TERMINE);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(completedResponse);

        ProjectResponse result = projectService.markAsComplete(100L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.TERMINE);
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.TERMINE);
    }

    @Test
    @DisplayName("markAsComplete: throws ResourceNotFoundException when project not found")
    void markAsComplete_notFound_throwsException() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.markAsComplete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteProject ----

    @Test
    @DisplayName("deleteProject: deletes existing project")
    void deleteProject_existingId_deletes() {
        when(projectRepository.existsById(100L)).thenReturn(true);

        projectService.deleteProject(100L);

        verify(projectRepository).deleteById(100L);
    }

    @Test
    @DisplayName("deleteProject: throws ResourceNotFoundException when not found")
    void deleteProject_notFound_throwsException() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.deleteProject(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
