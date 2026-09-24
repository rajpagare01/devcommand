package com.devcommand.devcommand.tasks.service;

import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.dto.UpdateDailyTaskRequest;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.mapper.DailyTaskMapper;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyTaskServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private DailyTaskRepository dailyTaskRepository;

    @Mock
    private UserRepository userRepository;

    // Plain mapping logic, no dependencies of its own - a real instance
    // keeps the test honest about what the service actually returns.
    private final DailyTaskMapper mapper = new DailyTaskMapper();

    private DailyTaskService service;

    @BeforeEach
    void setUp() {
        service = new DailyTaskService(dailyTaskRepository, userRepository, mapper);
    }

    private DailyTask sampleTask(Long id, Long ownerId) {
        User owner = User.builder().id(ownerId).name("Ada").email("ada@example.com").password("hash").build();
        return DailyTask.builder()
                .id(id)
                .title("Revise Spring Security")
                .category(TaskCategory.LEARNING)
                .priority(TaskPriority.HIGH)
                .status(DailyTaskStatus.TODO)
                .dueDate(LocalDate.now())
                .user(owner)
                .build();
    }

    @Test
    void create_savesTaskOwnedByTheGivenUser_andReturnsResponse() {
        User owner = User.builder().id(OWNER_ID).name("Ada").email("ada@example.com").password("hash").build();
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);

        CreateDailyTaskRequest request = new CreateDailyTaskRequest(
                "Solve 3 DSA problems", "Complete today's DSA practice",
                TaskCategory.DSA, TaskPriority.HIGH, DailyTaskStatus.TODO, LocalDate.of(2026, 9, 25)
        );

        ArgumentCaptor<DailyTask> captor = ArgumentCaptor.forClass(DailyTask.class);
        when(dailyTaskRepository.save(captor.capture())).thenAnswer(inv -> {
            DailyTask toSave = inv.getArgument(0);
            toSave.setId(100L);
            return toSave;
        });

        DailyTaskResponse response = service.create(request, OWNER_ID);

        assertThat(captor.getValue().getUser()).isEqualTo(owner);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("Solve 3 DSA problems");
        assertThat(response.category()).isEqualTo(TaskCategory.DSA);
    }

    @Test
    void getById_whenOwnedByCaller_returnsTask() {
        DailyTask task = sampleTask(10L, OWNER_ID);
        when(dailyTaskRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(task));

        DailyTaskResponse response = service.getById(10L, OWNER_ID);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void getById_whenOwnedByAnotherUser_throwsNotFound_ratherThanExposingIt() {
        // User B (OTHER_USER_ID) tries to read User A's (OWNER_ID) task #10.
        // findByIdAndUserId(10, OTHER_USER_ID) correctly finds nothing.
        when(dailyTaskRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_whenOwnedByCaller_appliesEditableFields_andNeverTouchesOwnerOrCompletedAt() {
        DailyTask task = sampleTask(10L, OWNER_ID);
        when(dailyTaskRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(task));

        UpdateDailyTaskRequest request = new UpdateDailyTaskRequest(
                "Revise Spring Security (part 2)", "Focus on filters",
                TaskCategory.LEARNING, TaskPriority.MEDIUM, DailyTaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 9, 26)
        );

        DailyTaskResponse response = service.update(10L, OWNER_ID, request);

        assertThat(response.title()).isEqualTo("Revise Spring Security (part 2)");
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.status()).isEqualTo(DailyTaskStatus.IN_PROGRESS);
        assertThat(response.completedAt()).isNull(); // update never sets completedAt
        assertThat(task.getUser().getId()).isEqualTo(OWNER_ID); // owner unchanged
    }

    @Test
    void update_whenOwnedByAnotherUser_throwsNotFound() {
        when(dailyTaskRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdateDailyTaskRequest request = new UpdateDailyTaskRequest(
                "x", null, TaskCategory.PERSONAL, TaskPriority.LOW, DailyTaskStatus.TODO, null
        );

        assertThatThrownBy(() -> service.update(10L, OTHER_USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenOwnedByCaller_deletesTheEntity() {
        DailyTask task = sampleTask(10L, OWNER_ID);
        when(dailyTaskRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(task));

        service.delete(10L, OWNER_ID);

        verify(dailyTaskRepository, times(1)).delete(task);
    }

    @Test
    void delete_whenOwnedByAnotherUser_throwsNotFound_andNeverDeletes() {
        when(dailyTaskRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(dailyTaskRepository, never()).delete(any());
    }

    @Test
    void complete_setsStatusCompleted_andStampsCompletedAt() {
        DailyTask task = sampleTask(10L, OWNER_ID); // status TODO, completedAt null
        when(dailyTaskRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(task));

        DailyTaskResponse response = service.complete(10L, OWNER_ID);

        assertThat(response.status()).isEqualTo(DailyTaskStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void complete_whenAlreadyCompleted_isIdempotent_doesNotOverwriteCompletedAt() {
        DailyTask task = sampleTask(10L, OWNER_ID);
        LocalDateTime originalCompletedAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        task.setStatus(DailyTaskStatus.COMPLETED);
        task.setCompletedAt(originalCompletedAt);
        when(dailyTaskRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(task));

        DailyTaskResponse response = service.complete(10L, OWNER_ID);

        assertThat(response.status()).isEqualTo(DailyTaskStatus.COMPLETED);
        assertThat(response.completedAt()).isEqualTo(originalCompletedAt);
        verify(dailyTaskRepository, never()).save(any()); // no duplicate record created
    }

    @Test
    void start_setsStatusInProgress() {
        DailyTask task = sampleTask(10L, OWNER_ID);
        when(dailyTaskRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(task));

        DailyTaskResponse response = service.start(10L, OWNER_ID);

        assertThat(response.status()).isEqualTo(DailyTaskStatus.IN_PROGRESS);
    }

    @Test
    void start_whenOwnedByAnotherUser_throwsNotFound() {
        when(dailyTaskRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void today_queriesOnlyTheCallersTasksDueToday() {
        DailyTask task = sampleTask(10L, OWNER_ID);
        when(dailyTaskRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(task));

        List<DailyTaskResponse> result = service.today(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        verify(dailyTaskRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void upcoming_returnsWhatRepositoryProvides() {
        DailyTask task = sampleTask(11L, OWNER_ID);
        task.setDueDate(LocalDate.now().plusDays(3));
        when(dailyTaskRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(task));

        List<DailyTaskResponse> result = service.upcoming(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(11L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void completed_returnsWhatRepositoryProvides() {
        DailyTask task = sampleTask(12L, OWNER_ID);
        task.setStatus(DailyTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        when(dailyTaskRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(task));

        List<DailyTaskResponse> result = service.completed(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DailyTaskStatus.COMPLETED);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withNoParams_usesDefaultPageSizeAndSort() {
        when(dailyTaskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAll(OWNER_ID, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dailyTaskRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(used.getSort().getOrderFor("createdAt").getDirection().isDescending()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withExplicitParams_honorsPageSizeAndAscendingSort() {
        when(dailyTaskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAll(OWNER_ID, "DSA", "HIGH", "TODO", LocalDate.of(2026, 9, 25), 2, 5, "dueDate", "asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dailyTaskRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(2);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort().getOrderFor("dueDate").getDirection().isAscending()).isTrue();
    }

    @Test
    void getAll_withInvalidCategory_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.getAll(OWNER_ID, "NOT_A_REAL_CATEGORY", null, null, null, null, null, null, null)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getAll_withInvalidPriority_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.getAll(OWNER_ID, null, "NOT_A_REAL_PRIORITY", null, null, null, null, null, null)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getAll_withInvalidStatus_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.getAll(OWNER_ID, null, null, "NOT_A_REAL_STATUS", null, null, null, null, null)
        ).isInstanceOf(BadRequestException.class);
    }
}
