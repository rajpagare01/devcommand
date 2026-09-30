package com.devcommand.devcommand.projects.service;

import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.projects.dto.ProjectRequest;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.entity.Project;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.mapper.ProjectMapper;
import com.devcommand.devcommand.projects.repository.ProjectRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    private final ProjectMapper mapper = new ProjectMapper();

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, userRepository, mapper);
    }

    private Project sampleProject(Long id, Long ownerId) {
        User owner = User.builder().id(ownerId).name("Ada").email("ada@example.com").password("hash").build();
        return Project.builder()
                .id(id)
                .name("DevCommand")
                .description("Developer Dashboard")
                .status(ProjectStatus.IN_PROGRESS)
                .user(owner)
                .build();
    }

    @Test
    void create_savesProjectOwnedByTheGivenUser() {
        User owner = User.builder().id(OWNER_ID).name("Ada").email("ada@example.com").password("hash").build();
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);

        ProjectRequest request = new ProjectRequest(
                "DevCommand", "Developer Dashboard", null, null, ProjectStatus.IN_PROGRESS, null, null
        );

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        when(projectRepository.save(captor.capture())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProjectResponse response = service.create(request, OWNER_ID);

        assertThat(captor.getValue().getUser()).isEqualTo(owner);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("DevCommand");
    }

    @Test
    void getById_whenOwnedByCaller_returnsProject() {
        Project project = sampleProject(10L, OWNER_ID);
        when(projectRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(project));

        ProjectResponse response = service.getById(10L, OWNER_ID);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void getById_whenOwnedByAnotherUser_throwsNotFound() {
        when(projectRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_whenOwnedByAnotherUser_throwsNotFound() {
        when(projectRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        ProjectRequest request = new ProjectRequest("x", "x", null, null, ProjectStatus.PLANNING, null, null);

        assertThatThrownBy(() -> service.update(10L, request, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenOwnedByAnotherUser_throwsNotFound_andNeverDeletes() {
        when(projectRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectRepository, never()).delete(any(Project.class));
    }
}
