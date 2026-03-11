package com.example.Taskora.service.unit;

import com.example.Taskora.dto.request.CreateTaskRequest;
import com.example.Taskora.dto.response.TaskResponse;
import com.example.Taskora.entity.*;
import com.example.Taskora.exception.ResourceNotFoundException;
import com.example.Taskora.mapper.TaskMapper;
import com.example.Taskora.repository.ProjectRepository;
import com.example.Taskora.repository.TaskRepository;
import com.example.Taskora.repository.UserRepository;
import com.example.Taskora.service.impl.TaskServiceImpl;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl Unit Tests")
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User user;
    private Project project;
    private Task task;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).fullName("Alice").email("alice@example.com")
                .role(Role.ROLE_FREELANCER).status(UserStatus.ACTIVE).build();

        project = Project.builder()
                .id(100L).title("Website Redesign")
                .status(ProjectStatus.EN_COURS).user(user).build();

        task = Task.builder()
                .id(200L).title("Design mockups")
                .status(TaskStatus.A_FAIRE)
                .dueDate(LocalDate.now().plusDays(7))
                .project(project).user(user).build();

        taskResponse = new TaskResponse();
        taskResponse.setId(200L);
        taskResponse.setTitle("Design mockups");
        taskResponse.setStatus(TaskStatus.A_FAIRE);
        taskResponse.setProjectId(100L);
        taskResponse.setUserId(1L);
    }

    // ---- createTask ----

    @Test
    @DisplayName("createTask: saves task with default A_FAIRE status when status is null")
    void createTask_nullStatus_setsAFaireDefault() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Design mockups");
        request.setProjectId(100L);
        request.setUserId(1L);

        task.setStatus(null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        TaskResponse result = taskService.createTask(request);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.A_FAIRE);
        assertThat(result).isNotNull();
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("createTask: preserves explicit status when provided")
    void createTask_withExplicitStatus_preservesStatus() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Design mockups");
        request.setStatus(TaskStatus.TERMINEE);
        request.setProjectId(100L);
        request.setUserId(1L);

        task.setStatus(TaskStatus.TERMINEE);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        taskService.createTask(request);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TERMINEE);
    }

    @Test
    @DisplayName("createTask: throws ResourceNotFoundException when project not found")
    void createTask_projectNotFound_throwsException() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setProjectId(999L);
        request.setUserId(1L);

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createTask: throws ResourceNotFoundException when user not found")
    void createTask_userNotFound_throwsException() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setProjectId(100L);
        request.setUserId(999L);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getTaskById ----

    @Test
    @DisplayName("getTaskById: returns task when it exists")
    void getTaskById_exists_returnsTask() {
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        TaskResponse result = taskService.getTaskById(200L);

        assertThat(result.getId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("getTaskById: throws ResourceNotFoundException when task not found")
    void getTaskById_notFound_throwsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getTasksByProject ----

    @Test
    @DisplayName("getTasksByProject: returns page of tasks for given project")
    void getTasksByProject_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(taskRepository.findByProjectId(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        Page<TaskResponse> result = taskService.getTasksByProject(100L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ---- getTasksByProjectAndStatus ----

    @Test
    @DisplayName("getTasksByProjectAndStatus: filters tasks by project and status")
    void getTasksByProjectAndStatus_returnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(taskRepository.findByProjectIdAndStatus(100L, TaskStatus.A_FAIRE, pageable))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        Page<TaskResponse> result = taskService.getTasksByProjectAndStatus(100L, TaskStatus.A_FAIRE, pageable);

        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TaskStatus.A_FAIRE);
    }

    // ---- updateTask ----

    @Test
    @DisplayName("updateTask: updates task fields and relations")
    void updateTask_existingId_updatesTask() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Updated task");
        request.setProjectId(100L);
        request.setUserId(1L);

        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(taskMapper).updateEntityFromRequest(request, task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        TaskResponse result = taskService.updateTask(200L, request);

        assertThat(result).isNotNull();
        verify(taskMapper).updateEntityFromRequest(request, task);
    }

    // ---- markAsDone ----

    @Test
    @DisplayName("markAsDone: sets task status to TERMINEE")
    void markAsDone_existingTask_setsTermineeStatus() {
        TaskResponse doneResponse = new TaskResponse();
        doneResponse.setStatus(TaskStatus.TERMINEE);

        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(doneResponse);

        TaskResponse result = taskService.markAsDone(200L);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TERMINEE);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TERMINEE);
    }

    @Test
    @DisplayName("markAsDone: throws ResourceNotFoundException when task not found")
    void markAsDone_notFound_throwsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.markAsDone(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteTask ----

    @Test
    @DisplayName("deleteTask: deletes existing task")
    void deleteTask_existingId_deletesTask() {
        when(taskRepository.existsById(200L)).thenReturn(true);

        taskService.deleteTask(200L);

        verify(taskRepository).deleteById(200L);
    }

    @Test
    @DisplayName("deleteTask: throws ResourceNotFoundException when task not found")
    void deleteTask_notFound_throwsException() {
        when(taskRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
