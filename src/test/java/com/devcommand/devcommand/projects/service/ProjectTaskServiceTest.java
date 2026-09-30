package com.devcommand.devcommand.projects.service;

import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.projects.dto.ProjectTaskRequest;
import com.devcommand.devcommand.projects.dto.ProjectTaskResponse;
import com.devcommand.devcommand.projects.entity.Project;
import com.devcommand.devcommand.projects.entity.ProjectTask;
import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import com.devcommand.devcommand.projects.mapper.ProjectTaskMapper;
import com.devcommand.devcommand.projects.repository.ProjectTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectTaskServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PROJECT_ID = 100L;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private ProjectService projectService;

    private final ProjectTaskMapper mapper = new ProjectTaskMapper();

    private ProjectTaskService service;

    @BeforeEach
    void setUp() {
        service = new ProjectTaskService(projectTaskRepository, projectService, mapper);
    }

    private Project sampleProject() {
        return Project.builder().id(PROJECT_ID).build();
    }

    private ProjectTask sampleTask(Long id) {
        return ProjectTask.builder()
                .id(id)
                .title("Setup DB")
                .status(ProjectTaskStatus.TODO)
                .priority(ProjectTaskPriority.HIGH)
                .project(sampleProject())
                .build();
    }

    @Test
    void create_whenProjectOwned_savesTaskLinkedToProject() {
        Project project = sampleProject();
        when(projectService.getOwnedEntityOrThrow(PROJECT_ID, OWNER_ID)).thenReturn(project);

        ProjectTaskRequest request = new ProjectTaskRequest(
                "Setup DB", "PostgreSQL", ProjectTaskStatus.TODO, ProjectTaskPriority.HIGH, null
        );

        ArgumentCaptor<ProjectTask> captor = ArgumentCaptor.forClass(ProjectTask.class);
        when(projectTaskRepository.save(captor.capture())).thenAnswer(inv -> {
            ProjectTask t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        ProjectTaskResponse response = service.create(PROJECT_ID, request, OWNER_ID);

        assertThat(captor.getValue().getProject()).isEqualTo(project);
        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void create_whenProjectNotOwned_throwsNotFound() {
        when(projectService.getOwnedEntityOrThrow(PROJECT_ID, OTHER_USER_ID))
                .thenThrow(new ResourceNotFoundException("Project not found: " + PROJECT_ID));

        ProjectTaskRequest request = new ProjectTaskRequest("x", null, ProjectTaskStatus.TODO, ProjectTaskPriority.LOW, null);

        assertThatThrownBy(() -> service.create(PROJECT_ID, request, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verify(projectTaskRepository, never()).save(any());
    }

    @Test
    void getById_whenProjectNotOwned_throwsNotFound() {
        when(projectService.getOwnedEntityOrThrow(PROJECT_ID, OTHER_USER_ID))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        assertThatThrownBy(() -> service.getById(PROJECT_ID, 10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_whenProjectOwnedButTaskNotLinked_throwsNotFound() {
        when(projectService.getOwnedEntityOrThrow(PROJECT_ID, OWNER_ID)).thenReturn(sampleProject());
        when(projectTaskRepository.findByIdAndProjectId(10L, PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(PROJECT_ID, 10L, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
