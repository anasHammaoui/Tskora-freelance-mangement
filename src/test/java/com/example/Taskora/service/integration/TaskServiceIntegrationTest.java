package com.example.Taskora.service.integration;

import com.example.Taskora.dto.request.CreateClientRequest;
import com.example.Taskora.dto.request.CreateProjectRequest;
import com.example.Taskora.dto.request.CreateTaskRequest;
import com.example.Taskora.dto.request.CreateUserRequest;
import com.example.Taskora.dto.response.ProjectResponse;
import com.example.Taskora.dto.response.TaskResponse;
import com.example.Taskora.dto.response.UserResponse;
import com.example.Taskora.entity.ProjectStatus;
import com.example.Taskora.entity.TaskStatus;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.service.ClientService;
import com.example.Taskora.service.ProjectService;
import com.example.Taskora.service.TaskService;
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
@DisplayName("TaskService Integration Tests")
class TaskServiceIntegrationTest {

    @Autowired private TaskService taskService;
    @Autowired private UserService userService;
    @Autowired private ClientService clientService;
    @Autowired private ProjectService projectService;

    private Long userId;
    private Long projectId;

    @BeforeEach
    void setUpPrerequisites() {
        CreateUserRequest userReq = new CreateUserRequest();
        userReq.setFullName("Task Tester");
        userReq.setEmail("task-tester@example.com");
        userReq.setPassword("password");
        userId = userService.createUser(userReq).getId();

        CreateClientRequest clientReq = new CreateClientRequest();
        clientReq.setName("Task Test Client");
        clientReq.setEmail("taskclient@example.com");
        clientReq.setUserId(userId);
        Long clientId = clientService.createClient(clientReq).getId();

        CreateProjectRequest projectReq = new CreateProjectRequest();
        projectReq.setTitle("Task Test Project");
        projectReq.setStatus(ProjectStatus.EN_COURS);
        projectReq.setBudget(new BigDecimal("5000.00"));
        projectReq.setStartDate(LocalDate.now());
        projectReq.setClientId(clientId);
        projectReq.setUserId(userId);
        projectId = projectService.createProject(projectReq).getId();
    }

    private CreateTaskRequest buildTaskRequest(String title, TaskStatus status) {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle(title);
        req.setDescription("Test task: " + title);
        req.setDueDate(LocalDate.now().plusDays(7));
        req.setStatus(status);
        req.setProjectId(projectId);
        req.setUserId(userId);
        return req;
    }

    // ---- createTask ----

    @Test
    @DisplayName("createTask: persists task linked to project and user")
    void createTask_persistsTask() {
        TaskResponse result = taskService.createTask(buildTaskRequest("Design UI", TaskStatus.A_FAIRE));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Design UI");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.A_FAIRE);
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("createTask: defaults status to A_FAIRE when not provided")
    void createTask_nullStatus_defaultsToAFaire() {
        CreateTaskRequest req = buildTaskRequest("Default Status Task", null);

        TaskResponse result = taskService.createTask(req);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.A_FAIRE);
    }

    @Test
    @DisplayName("createTask: throws ResourceNotFoundException when project does not exist")
    void createTask_projectNotFound_throwsException() {
        CreateTaskRequest req = buildTaskRequest("Bad Task", TaskStatus.A_FAIRE);
        req.setProjectId(Long.MAX_VALUE);

        assertThatThrownBy(() -> taskService.createTask(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createTask: throws ResourceNotFoundException when user does not exist")
    void createTask_userNotFound_throwsException() {
        CreateTaskRequest req = buildTaskRequest("Bad Task", TaskStatus.A_FAIRE);
        req.setUserId(Long.MAX_VALUE);

        assertThatThrownBy(() -> taskService.createTask(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getTaskById ----

    @Test
    @DisplayName("getTaskById: retrieves persisted task")
    void getTaskById_returnsTask() {
        TaskResponse created = taskService.createTask(buildTaskRequest("Find Me", null));

        TaskResponse found = taskService.getTaskById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getTitle()).isEqualTo("Find Me");
    }

    @Test
    @DisplayName("getTaskById: throws ResourceNotFoundException for unknown id")
    void getTaskById_notFound_throwsException() {
        assertThatThrownBy(() -> taskService.getTaskById(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getTasksByProject ----

    @Test
    @DisplayName("getTasksByProject: returns tasks for the given project")
    void getTasksByProject_returnsProjectTasks() {
        taskService.createTask(buildTaskRequest("Task 1", TaskStatus.A_FAIRE));
        taskService.createTask(buildTaskRequest("Task 2", TaskStatus.TERMINEE));

        Page<TaskResponse> page = taskService.getTasksByProject(projectId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).allMatch(t -> t.getProjectId().equals(projectId));
    }

    // ---- getTasksByProjectAndStatus ----

    @Test
    @DisplayName("getTasksByProjectAndStatus: filters tasks by project and status")
    void getTasksByProjectAndStatus_filtersCorrectly() {
        taskService.createTask(buildTaskRequest("Todo Task", TaskStatus.A_FAIRE));
        taskService.createTask(buildTaskRequest("Doing Task", TaskStatus.TERMINEE));

        Page<TaskResponse> page = taskService.getTasksByProjectAndStatus(
                projectId, TaskStatus.A_FAIRE, PageRequest.of(0, 10));

        assertThat(page.getContent()).allMatch(t -> t.getStatus() == TaskStatus.A_FAIRE);
    }

    // ---- getTasksByUser ----

    @Test
    @DisplayName("getTasksByUser: returns tasks assigned to the user")
    void getTasksByUser_returnsUserTasks() {
        taskService.createTask(buildTaskRequest("My Task", null));

        Page<TaskResponse> page = taskService.getTasksByUser(userId, PageRequest.of(0, 10));

        assertThat(page.getContent()).allMatch(t -> t.getUserId().equals(userId));
    }

    // ---- updateTask ----

    @Test
    @DisplayName("updateTask: updates task title and status")
    void updateTask_updatesFields() {
        TaskResponse created = taskService.createTask(buildTaskRequest("Original Title", null));

        CreateTaskRequest updateReq = buildTaskRequest("Updated Title", TaskStatus.TERMINEE);
        TaskResponse updated = taskService.updateTask(created.getId(), updateReq);

        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.TERMINEE);
    }

    // ---- markAsDone ----

    @Test
    @DisplayName("markAsDone: changes task status to TERMINEE")
    void markAsDone_setsTermineeStatus() {
        TaskResponse created = taskService.createTask(buildTaskRequest("Work Task", null));

        TaskResponse done = taskService.markAsDone(created.getId());

        assertThat(done.getStatus()).isEqualTo(TaskStatus.TERMINEE);
    }

    @Test
    @DisplayName("markAsDone: throws ResourceNotFoundException for unknown id")
    void markAsDone_notFound_throwsException() {
        assertThatThrownBy(() -> taskService.markAsDone(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteTask ----

    @Test
    @DisplayName("deleteTask: removes task from database")
    void deleteTask_removesTask() {
        TaskResponse created = taskService.createTask(buildTaskRequest("Delete Me", null));

        taskService.deleteTask(created.getId());

        assertThatThrownBy(() -> taskService.getTaskById(created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTask: throws ResourceNotFoundException for unknown id")
    void deleteTask_notFound_throwsException() {
        assertThatThrownBy(() -> taskService.deleteTask(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
