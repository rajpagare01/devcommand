package com.devcommand.devcommand.jobs.service;

import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.jobs.dto.CreateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.dto.InterviewRoundResponse;
import com.devcommand.devcommand.jobs.dto.UpdateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.entity.InterviewRound;
import com.devcommand.devcommand.jobs.entity.InterviewStatus;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.mapper.InterviewRoundMapper;
import com.devcommand.devcommand.jobs.repository.InterviewRoundRepository;
import com.devcommand.devcommand.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/**
 * jobApplicationService is mocked here rather than using a real instance -
 * this test is specifically about InterviewRoundService's own logic
 * ("resolve the job first, then operate on rounds scoped to it"), not
 * about re-verifying JobApplicationService's ownership check itself
 * (that's JobApplicationServiceTest's job). The key assertions are that
 * getOwnedEntityOrThrow is always called before any repository access,
 * and that its exception propagates untouched.
 */
@ExtendWith(MockitoExtension.class)
class InterviewRoundServiceTest {

    private static final Long JOB_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private InterviewRoundRepository interviewRoundRepository;

    @Mock
    private JobApplicationService jobApplicationService;

    private final InterviewRoundMapper mapper = new InterviewRoundMapper();

    private InterviewRoundService service;

    @BeforeEach
    void setUp() {
        service = new InterviewRoundService(interviewRoundRepository, jobApplicationService, mapper);
    }

    private JobApplication sampleJob() {
        User owner = User.builder().id(OWNER_ID).name("Ada").email("ada@example.com").password("hash").build();
        return JobApplication.builder()
                .id(JOB_ID).company("TCS").role("Java Developer")
                .applicationDate(LocalDate.now()).status(ApplicationStatus.INTERVIEW).user(owner)
                .build();
    }

    private InterviewRound sampleRound(Long id) {
        return InterviewRound.builder()
                .id(id).roundNumber(1).roundType("Technical")
                .status(InterviewStatus.SCHEDULED).jobApplication(sampleJob())
                .build();
    }

    @Test
    void create_whenJobOwnedByCaller_savesRoundAgainstThatJob() {
        JobApplication job = sampleJob();
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OWNER_ID)).thenReturn(job);
        when(interviewRoundRepository.save(any())).thenAnswer(inv -> {
            InterviewRound r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        CreateInterviewRoundRequest request = new CreateInterviewRoundRequest(
                1, "Technical", LocalDateTime.of(2026, 9, 28, 11, 0),
                InterviewStatus.SCHEDULED, null, "Prepare Spring Security"
        );

        InterviewRoundResponse response = service.create(JOB_ID, OWNER_ID, request);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.roundType()).isEqualTo("Technical");
        assertThat(response.status()).isEqualTo(InterviewStatus.SCHEDULED);
    }

    @Test
    void create_whenJobNotOwnedByCaller_throwsNotFound_andNeverSavesARound() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OTHER_USER_ID))
                .thenThrow(new ResourceNotFoundException("Job application not found: " + JOB_ID));

        CreateInterviewRoundRequest request = new CreateInterviewRoundRequest(
                1, "Technical", null, InterviewStatus.SCHEDULED, null, null
        );

        assertThatThrownBy(() -> service.create(JOB_ID, OTHER_USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRoundRepository, never()).save(any());
    }

    @Test
    void getAllForJob_whenJobOwnedByCaller_returnsRoundsForThatJob() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OWNER_ID)).thenReturn(sampleJob());
        when(interviewRoundRepository.findByJobApplicationId(JOB_ID)).thenReturn(List.of(sampleRound(1L), sampleRound(2L)));

        List<InterviewRoundResponse> result = service.getAllForJob(JOB_ID, OWNER_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllForJob_whenJobNotOwnedByCaller_throwsNotFound_andNeverQueriesRounds() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OTHER_USER_ID))
                .thenThrow(new ResourceNotFoundException("Job application not found: " + JOB_ID));

        assertThatThrownBy(() -> service.getAllForJob(JOB_ID, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRoundRepository, never()).findByJobApplicationId(any());
    }

    @Test
    void getById_whenJobOwnedAndRoundBelongsToIt_returnsRound() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OWNER_ID)).thenReturn(sampleJob());
        when(interviewRoundRepository.findByIdAndJobApplicationId(5L, JOB_ID)).thenReturn(Optional.of(sampleRound(5L)));

        InterviewRoundResponse response = service.getById(JOB_ID, 5L, OWNER_ID);

        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    void getById_whenRoundDoesNotBelongToThatJob_throwsNotFound() {
        // Job is owned, but the round id given doesn't belong to it (e.g. it belongs
        // to a different job entirely) - findByIdAndJobApplicationId correctly finds nothing.
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OWNER_ID)).thenReturn(sampleJob());
        when(interviewRoundRepository.findByIdAndJobApplicationId(999L, JOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(JOB_ID, 999L, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_whenJobNotOwnedByCaller_throwsNotFound_beforeEvenLookingAtRounds() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OTHER_USER_ID))
                .thenThrow(new ResourceNotFoundException("Job application not found: " + JOB_ID));

        assertThatThrownBy(() -> service.getById(JOB_ID, 5L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRoundRepository, never()).findByIdAndJobApplicationId(any(), any());
    }

    @Test
    void update_whenJobOwnedAndRoundBelongsToIt_appliesFields() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OWNER_ID)).thenReturn(sampleJob());
        InterviewRound round = sampleRound(5L);
        when(interviewRoundRepository.findByIdAndJobApplicationId(5L, JOB_ID)).thenReturn(Optional.of(round));

        UpdateInterviewRoundRequest request = new UpdateInterviewRoundRequest(
                2, "HR", LocalDateTime.of(2026, 10, 1, 10, 0),
                InterviewStatus.COMPLETED, "Went well", "Follow up next week"
        );

        InterviewRoundResponse response = service.update(JOB_ID, 5L, OWNER_ID, request);

        assertThat(response.roundNumber()).isEqualTo(2);
        assertThat(response.roundType()).isEqualTo("HR");
        assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(response.feedback()).isEqualTo("Went well");
    }

    @Test
    void update_whenJobNotOwnedByCaller_throwsNotFound() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OTHER_USER_ID))
                .thenThrow(new ResourceNotFoundException("Job application not found: " + JOB_ID));

        UpdateInterviewRoundRequest request = new UpdateInterviewRoundRequest(
                1, "Technical", null, InterviewStatus.SCHEDULED, null, null
        );

        assertThatThrownBy(() -> service.update(JOB_ID, 5L, OTHER_USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenJobOwnedAndRoundBelongsToIt_deletesTheRound() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OWNER_ID)).thenReturn(sampleJob());
        InterviewRound round = sampleRound(5L);
        when(interviewRoundRepository.findByIdAndJobApplicationId(5L, JOB_ID)).thenReturn(Optional.of(round));

        service.delete(JOB_ID, 5L, OWNER_ID);

        verify(interviewRoundRepository, times(1)).delete(round);
    }

    @Test
    void delete_whenJobNotOwnedByCaller_throwsNotFound_andNeverDeletes() {
        when(jobApplicationService.getOwnedEntityOrThrow(JOB_ID, OTHER_USER_ID))
                .thenThrow(new ResourceNotFoundException("Job application not found: " + JOB_ID));

        assertThatThrownBy(() -> service.delete(JOB_ID, 5L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewRoundRepository, never()).delete(any());
    }
}
