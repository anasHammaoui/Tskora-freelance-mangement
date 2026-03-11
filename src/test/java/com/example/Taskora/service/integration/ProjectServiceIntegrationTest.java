package com.example.Taskora.service.integration;

import com.example.Taskora.dto.request.CreateClientRequest;
import com.example.Taskora.dto.request.CreateProjectRequest;
import com.example.Taskora.dto.request.CreateUserRequest;
import com.example.Taskora.dto.response.ClientResponse;
import com.example.Taskora.dto.response.ProjectResponse;
import com.example.Taskora.dto.response.UserResponse;
import com.example.Taskora.entity.ProjectStatus;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.service.ClientService;
import com.example.Taskora.service.ProjectService;
import com.example.Taskora.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("ProjectService Integration Tests")
class ProjectServiceIntegrationTest {

    @Autowired private ProjectService projectService;
    @Autowired private UserService userService;
    @Autowired private ClientService clientService;

    private Long userId;
    private Long clientId;

    @BeforeEach
    void setUpPrerequisites() {
        CreateUserRequest userReq = new CreateUserRequest();
        userReq.setFullName("Project Tester");
        userReq.setEmail("project-tester@example.com");
        userReq.setPassword("password");
        userId = userService.createUser(userReq).getId();

        CreateClientRequest clientReq = new CreateClientRequest();
        clientReq.setName("Test Client");
        clientReq.setEmail("testclient@example.com");
        clientReq.setUserId(userId);
        clientId = clientService.createClient(clientReq).getId();
    }

    private CreateProjectRequest buildProjectRequest(String title, ProjectStatus status) {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setTitle(title);
        req.setDescription("Test description for " + title);
        req.setBudget(new BigDecimal("10000.00"));
        req.setStatus(status);
        req.setStartDate(LocalDate.now());
        req.setClientId(clientId);
        req.setUserId(userId);
        return req;
    }

    // ---- createProject ----

    @Test
    @DisplayName("createProject: persists project linked to client and user")
    void createProject_persistsProject() {
        CreateProjectRequest req = buildProjectRequest("My First Project", ProjectStatus.EN_COURS);

        ProjectResponse result = projectService.createProject(req);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("My First Project");
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.EN_COURS);
        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("createProject: throws ResourceNotFoundException when client does not exist")
    void createProject_clientNotFound_throwsException() {
        CreateProjectRequest req = buildProjectRequest("Bad Project", ProjectStatus.EN_COURS);
        req.setClientId(Long.MAX_VALUE);

        assertThatThrownBy(() -> projectService.createProject(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createProject: throws ResourceNotFoundException when user does not exist")
    void createProject_userNotFound_throwsException() {
        CreateProjectRequest req = buildProjectRequest("Bad Project", ProjectStatus.EN_COURS);
        req.setUserId(Long.MAX_VALUE);

        assertThatThrownBy(() -> projectService.createProject(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getProjectById ----

    @Test
    @DisplayName("getProjectById: retrieves persisted project")
    void getProjectById_returnsProject() {
        ProjectResponse created = projectService.createProject(
                buildProjectRequest("Find Me", ProjectStatus.EN_COURS));

        ProjectResponse found = projectService.getProjectById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getTitle()).isEqualTo("Find Me");
    }

    @Test
    @DisplayName("getProjectById: throws ResourceNotFoundException for unknown id")
    void getProjectById_notFound_throwsException() {
        assertThatThrownBy(() -> projectService.getProjectById(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getAllProjects ----

    @Test
    @DisplayName("getAllProjects: returns paginated list of all projects")
    void getAllProjects_returnsPage() {
        projectService.createProject(buildProjectRequest("Project Alpha", ProjectStatus.EN_COURS));
        projectService.createProject(buildProjectRequest("Project Beta", ProjectStatus.TERMINE));

        Page<ProjectResponse> page = projectService.getAllProjects(PageRequest.of(0, 50));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    // ---- getProjectsByUser ----

    @Test
    @DisplayName("getProjectsByUser: returns only projects of specified user")
    void getProjectsByUser_returnsUserProjects() {
        projectService.createProject(buildProjectRequest("My Project", ProjectStatus.EN_COURS));

        Page<ProjectResponse> page = projectService.getProjectsByUser(userId, PageRequest.of(0, 10));

        assertThat(page.getContent()).allMatch(p -> p.getUserId().equals(userId));
        assertThat(page.getContent()).anyMatch(p -> p.getTitle().equals("My Project"));
    }

    // ---- getProjectsByStatus ----

    @Test
    @DisplayName("getProjectsByStatus: filters projects by status")
    void getProjectsByStatus_returnsMatchingProjects() {
        projectService.createProject(buildProjectRequest("Active Project", ProjectStatus.EN_COURS));
        projectService.createProject(buildProjectRequest("Pending Project", ProjectStatus.TERMINE));

        Page<ProjectResponse> page = projectService.getProjectsByStatus(
                ProjectStatus.EN_COURS, PageRequest.of(0, 50));

        assertThat(page.getContent()).allMatch(p -> p.getStatus() == ProjectStatus.EN_COURS);
    }

    // ---- getProjectsByUserAndStatus ----

    @Test
    @DisplayName("getProjectsByUserAndStatus: returns projects filtered by user and status")
    void getProjectsByUserAndStatus_returnsFilteredProjects() {
        projectService.createProject(buildProjectRequest("Active User Project", ProjectStatus.EN_COURS));
        projectService.createProject(buildProjectRequest("Completed User Project", ProjectStatus.TERMINE));

        Page<ProjectResponse> page = projectService.getProjectsByUserAndStatus(
                userId, ProjectStatus.EN_COURS, PageRequest.of(0, 10));

        assertThat(page.getContent()).allMatch(p ->
                p.getUserId().equals(userId) && p.getStatus() == ProjectStatus.EN_COURS);
    }

    // ---- updateProject ----

    @Test
    @DisplayName("updateProject: updates project title and budget")
    void updateProject_updatesFields() {
        ProjectResponse created = projectService.createProject(
                buildProjectRequest("Original Title", ProjectStatus.EN_COURS));

        CreateProjectRequest updateReq = buildProjectRequest("Updated Title", ProjectStatus.TERMINE);
        updateReq.setBudget(new BigDecimal("20000.00"));

        ProjectResponse updated = projectService.updateProject(created.getId(), updateReq);

        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getStatus()).isEqualTo(ProjectStatus.TERMINE);
        assertThat(updated.getBudget()).isEqualByComparingTo("20000.00");
    }

    // ---- markAsComplete ----

    @Test
    @DisplayName("markAsComplete: changes project status to TERMINE")
    void markAsComplete_setsCompleteStatus() {
        ProjectResponse created = projectService.createProject(
                buildProjectRequest("Ongoing Project", ProjectStatus.EN_COURS));

        ProjectResponse completed = projectService.markAsComplete(created.getId());

        assertThat(completed.getStatus()).isEqualTo(ProjectStatus.TERMINE);
    }

    // ---- deleteProject ----

    @Test
    @DisplayName("deleteProject: removes project from database")
    void deleteProject_removesProject() {
        ProjectResponse created = projectService.createProject(
                buildProjectRequest("To Delete", ProjectStatus.EN_COURS));

        projectService.deleteProject(created.getId());

        assertThatThrownBy(() -> projectService.getProjectById(created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
