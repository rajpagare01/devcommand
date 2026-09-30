package com.devcommand.devcommand.learning.service;

import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.learning.dto.CreateLearningTopicRequest;
import com.devcommand.devcommand.learning.dto.LearningSummary;
import com.devcommand.devcommand.learning.dto.LearningTopicResponse;
import com.devcommand.devcommand.learning.dto.ProgressUpdateRequest;
import com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest;
import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.mapper.LearningTopicMapper;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
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
class LearningTopicServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private LearningTopicRepository learningTopicRepository;

    @Mock
    private UserRepository userRepository;

    private final LearningTopicMapper mapper = new LearningTopicMapper();

    private LearningTopicService service;

    @BeforeEach
    void setUp() {
        service = new LearningTopicService(learningTopicRepository, userRepository, mapper);
    }

    private LearningTopic sampleTopic(Long id, Long ownerId, LearningStatus status, int progress) {
        User owner = User.builder().id(ownerId).name("Ada").email("ada@example.com").password("hash").build();
        return LearningTopic.builder()
                .id(id)
                .technology("Spring Boot")
                .topic("Spring Security")
                .progress(progress)
                .status(status)
                .hoursSpent(8.5)
                .user(owner)
                .build();
    }

    @Test
    void create_savesTopicOwnedByTheGivenUser_andReturnsResponse() {
        User owner = User.builder().id(OWNER_ID).name("Ada").email("ada@example.com").password("hash").build();
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);

        CreateLearningTopicRequest request = new CreateLearningTopicRequest(
                "Spring Boot", "Spring Security", 65, LearningStatus.IN_PROGRESS,
                8.5, "https://example.com", "Currently learning JWT authentication"
        );

        ArgumentCaptor<LearningTopic> captor = ArgumentCaptor.forClass(LearningTopic.class);
        when(learningTopicRepository.save(captor.capture())).thenAnswer(inv -> {
            LearningTopic toSave = inv.getArgument(0);
            toSave.setId(100L);
            return toSave;
        });

        LearningTopicResponse response = service.create(request, OWNER_ID);

        assertThat(captor.getValue().getUser()).isEqualTo(owner);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.technology()).isEqualTo("Spring Boot");
        assertThat(response.progress()).isEqualTo(65);
    }

    @Test
    void getById_whenOwnedByCaller_returnsTopic() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.IN_PROGRESS, 65);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        LearningTopicResponse response = service.getById(10L, OWNER_ID);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void getById_whenOwnedByAnotherUser_throwsNotFound_ratherThanExposingIt() {
        when(learningTopicRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_whenOwnedByCaller_appliesEditableFields_andNeverTouchesOwner() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.IN_PROGRESS, 65);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        UpdateLearningTopicRequest request = new UpdateLearningTopicRequest(
                "Spring Boot", "Spring Security (advanced)", 80, LearningStatus.IN_PROGRESS,
                12.0, "https://example.com/advanced", "Filters and OAuth2 next"
        );

        LearningTopicResponse response = service.update(10L, OWNER_ID, request);

        assertThat(response.topic()).isEqualTo("Spring Security (advanced)");
        assertThat(response.progress()).isEqualTo(80);
        assertThat(response.hoursSpent()).isEqualTo(12.0);
        assertThat(topic.getUser().getId()).isEqualTo(OWNER_ID); // owner unchanged
    }

    @Test
    void update_whenOwnedByAnotherUser_throwsNotFound() {
        when(learningTopicRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdateLearningTopicRequest request = new UpdateLearningTopicRequest(
                "x", "x", 0, LearningStatus.NOT_STARTED, null, null, null
        );

        assertThatThrownBy(() -> service.update(10L, OTHER_USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenOwnedByCaller_deletesTheEntity() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.IN_PROGRESS, 65);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        service.delete(10L, OWNER_ID);

        verify(learningTopicRepository, times(1)).delete(topic);
    }

    @Test
    void delete_whenOwnedByAnotherUser_throwsNotFound_andNeverDeletes() {
        when(learningTopicRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(learningTopicRepository, never()).delete(any(LearningTopic.class));
    }

    // ---- progress auto-transition rules ----------------------------------

    @Test
    void updateProgress_reaching100_autoCompletesRegardlessOfPriorStatus() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.IN_PROGRESS, 65);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        LearningTopicResponse response = service.updateProgress(10L, OWNER_ID, new ProgressUpdateRequest(100));

        assertThat(response.progress()).isEqualTo(100);
        assertThat(response.status()).isEqualTo(LearningStatus.COMPLETED);
    }

    @Test
    void updateProgress_reaching100_completesEvenWhenPreviouslyOnHold() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.ON_HOLD, 40);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        LearningTopicResponse response = service.updateProgress(10L, OWNER_ID, new ProgressUpdateRequest(100));

        assertThat(response.status()).isEqualTo(LearningStatus.COMPLETED);
    }

    @Test
    void updateProgress_fromNotStarted_autoBumpsToInProgress() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.NOT_STARTED, 0);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        LearningTopicResponse response = service.updateProgress(10L, OWNER_ID, new ProgressUpdateRequest(10));

        assertThat(response.progress()).isEqualTo(10);
        assertThat(response.status()).isEqualTo(LearningStatus.IN_PROGRESS);
    }

    @Test
    void updateProgress_zeroProgress_doesNotBumpFromNotStarted() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.NOT_STARTED, 0);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        LearningTopicResponse response = service.updateProgress(10L, OWNER_ID, new ProgressUpdateRequest(0));

        assertThat(response.status()).isEqualTo(LearningStatus.NOT_STARTED);
    }

    @Test
    void updateProgress_onHoldStatus_isNotSilentlyBumpedToInProgress() {
        // The topic was deliberately paused (ON_HOLD). A progress update
        // that isn't a full completion must not un-pause it automatically.
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.ON_HOLD, 40);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        LearningTopicResponse response = service.updateProgress(10L, OWNER_ID, new ProgressUpdateRequest(45));

        assertThat(response.progress()).isEqualTo(45);
        assertThat(response.status()).isEqualTo(LearningStatus.ON_HOLD);
    }

    @Test
    void updateProgress_alreadyInProgress_leavesStatusAlone() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.IN_PROGRESS, 65);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        LearningTopicResponse response = service.updateProgress(10L, OWNER_ID, new ProgressUpdateRequest(70));

        assertThat(response.status()).isEqualTo(LearningStatus.IN_PROGRESS);
    }

    @Test
    void updateProgress_whenOwnedByAnotherUser_throwsNotFound() {
        when(learningTopicRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProgress(10L, OTHER_USER_ID, new ProgressUpdateRequest(50)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void complete_setsProgress100AndStatusCompleted_evenIfOnHold() {
        LearningTopic topic = sampleTopic(10L, OWNER_ID, LearningStatus.ON_HOLD, 40);
        when(learningTopicRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(topic));

        LearningTopicResponse response = service.complete(10L, OWNER_ID);

        assertThat(response.progress()).isEqualTo(100);
        assertThat(response.status()).isEqualTo(LearningStatus.COMPLETED);
    }

    @Test
    void complete_whenOwnedByAnotherUser_throwsNotFound() {
        when(learningTopicRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- completed / in-progress convenience lists ------------------------

    @SuppressWarnings("unchecked")
    @Test
    void completedTopics_returnsWhatRepositoryProvides() {
        LearningTopic topic = sampleTopic(11L, OWNER_ID, LearningStatus.COMPLETED, 100);
        when(learningTopicRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(topic));

        List<LearningTopicResponse> result = service.completedTopics(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(LearningStatus.COMPLETED);
    }

    @SuppressWarnings("unchecked")
    @Test
    void inProgressTopics_returnsWhatRepositoryProvides() {
        LearningTopic topic = sampleTopic(12L, OWNER_ID, LearningStatus.IN_PROGRESS, 50);
        when(learningTopicRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(topic));

        List<LearningTopicResponse> result = service.inProgressTopics(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(LearningStatus.IN_PROGRESS);
    }

    // ---- filtering / pagination / sorting ---------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withNoParams_usesDefaultPageSizeAndSort() {
        when(learningTopicRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAll(OWNER_ID, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(learningTopicRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(used.getSort().getOrderFor("createdAt").getDirection().isDescending()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withExplicitParams_honorsPageSizeAndAscendingSort() {
        when(learningTopicRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAll(OWNER_ID, "Spring Boot", "IN_PROGRESS", 65, "security", 2, 5, "progress", "asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(learningTopicRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(2);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort().getOrderFor("progress").getDirection().isAscending()).isTrue();
    }

    @Test
    void getAll_withInvalidStatus_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.getAll(OWNER_ID, null, "NOT_A_REAL_STATUS", null, null, null, null, null, null)
        ).isInstanceOf(BadRequestException.class);
    }

    // ---- summary (internal, unexposed by any endpoint) --------------------

    @Test
    void getSummary_aggregatesAcrossTheCallersTopicsOnly() {
        List<LearningTopic> topics = List.of(
                sampleTopic(1L, OWNER_ID, LearningStatus.COMPLETED, 100),
                sampleTopic(2L, OWNER_ID, LearningStatus.IN_PROGRESS, 50),
                sampleTopic(3L, OWNER_ID, LearningStatus.NOT_STARTED, 0)
        );
        when(learningTopicRepository.findAll(any(Specification.class))).thenReturn(topics);

        LearningSummary summary = service.getSummary(OWNER_ID);

        assertThat(summary.totalTopics()).isEqualTo(3);
        assertThat(summary.completedTopics()).isEqualTo(1);
        assertThat(summary.inProgressTopics()).isEqualTo(1);
        assertThat(summary.totalHoursSpent()).isEqualTo(25.5); // 8.5 * 3 samples
        assertThat(summary.averageProgress()).isEqualTo(50.0); // (100+50+0)/3
    }
}
