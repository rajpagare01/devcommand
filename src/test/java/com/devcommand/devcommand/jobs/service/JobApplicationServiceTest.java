package com.devcommand.devcommand.jobs.service;

import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.jobs.dto.CreateJobApplicationRequest;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.dto.JobStatusUpdateRequest;
import com.devcommand.devcommand.jobs.dto.UpdateJobApplicationRequest;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.mapper.JobApplicationMapper;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
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
class JobApplicationServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private UserRepository userRepository;

    private final JobApplicationMapper mapper = new JobApplicationMapper();

    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(jobApplicationRepository, userRepository, mapper);
    }

    private JobApplication sampleJob(Long id, Long ownerId) {
        User owner = User.builder().id(ownerId).name("Ada").email("ada@example.com").password("hash").build();
        return JobApplication.builder()
                .id(id)
                .company("TCS")
                .role("Java Developer")
                .applicationDate(LocalDate.of(2026, 9, 24))
                .status(ApplicationStatus.APPLIED)
                .user(owner)
                .build();
    }

    @Test
    void create_savesJobOwnedByTheGivenUser_andReturnsResponse() {
        User owner = User.builder().id(OWNER_ID).name("Ada").email("ada@example.com").password("hash").build();
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);

        CreateJobApplicationRequest request = new CreateJobApplicationRequest(
                "TCS", "Java Developer", "Indore", "https://example.com/job", "LinkedIn",
                "6-8 LPA", LocalDate.of(2026, 9, 24), ApplicationStatus.APPLIED, "Java + Spring Boot role"
        );

        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        when(jobApplicationRepository.save(captor.capture())).thenAnswer(inv -> {
            JobApplication toSave = inv.getArgument(0);
            toSave.setId(100L);
            return toSave;
        });

        JobApplicationResponse response = service.create(request, OWNER_ID);

        assertThat(captor.getValue().getUser()).isEqualTo(owner);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.company()).isEqualTo("TCS");
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void getById_whenOwnedByCaller_returnsJob() {
        JobApplication job = sampleJob(10L, OWNER_ID);
        when(jobApplicationRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(job));

        JobApplicationResponse response = service.getById(10L, OWNER_ID);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void getById_whenOwnedByAnotherUser_throwsNotFound_ratherThanExposingIt() {
        when(jobApplicationRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_whenOwnedByCaller_appliesEditableFields_andNeverTouchesOwner() {
        JobApplication job = sampleJob(10L, OWNER_ID);
        when(jobApplicationRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(job));

        UpdateJobApplicationRequest request = new UpdateJobApplicationRequest(
                "TCS", "Senior Java Developer", "Indore", "https://example.com/job", "LinkedIn",
                "10-12 LPA", LocalDate.of(2026, 9, 24), ApplicationStatus.SCREENING, "Promoted to senior req"
        );

        JobApplicationResponse response = service.update(10L, OWNER_ID, request);

        assertThat(response.role()).isEqualTo("Senior Java Developer");
        assertThat(response.status()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(job.getUser().getId()).isEqualTo(OWNER_ID); // owner unchanged
    }

    @Test
    void update_whenOwnedByAnotherUser_throwsNotFound() {
        when(jobApplicationRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdateJobApplicationRequest request = new UpdateJobApplicationRequest(
                "x", "x", null, null, null, null, LocalDate.now(), ApplicationStatus.SAVED, null
        );

        assertThatThrownBy(() -> service.update(10L, OTHER_USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenOwnedByCaller_deletesTheEntity() {
        JobApplication job = sampleJob(10L, OWNER_ID);
        when(jobApplicationRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(job));

        service.delete(10L, OWNER_ID);

        verify(jobApplicationRepository, times(1)).delete(job);
    }

    @Test
    void delete_whenOwnedByAnotherUser_throwsNotFound_andNeverDeletes() {
        when(jobApplicationRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobApplicationRepository, never()).delete(any(JobApplication.class));
    }

    @Test
    void changeStatus_whenOwnedByCaller_updatesStatus() {
        JobApplication job = sampleJob(10L, OWNER_ID); // starts APPLIED
        when(jobApplicationRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(job));

        JobApplicationResponse response = service.changeStatus(10L, OWNER_ID, new JobStatusUpdateRequest(ApplicationStatus.INTERVIEW));

        assertThat(response.status()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void changeStatus_whenOwnedByAnotherUser_throwsNotFound() {
        when(jobApplicationRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.changeStatus(10L, OTHER_USER_ID, new JobStatusUpdateRequest(ApplicationStatus.OFFER))
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOwnedEntityOrThrow_whenOwnedByCaller_returnsEntity_usedByInterviewRoundService() {
        JobApplication job = sampleJob(10L, OWNER_ID);
        when(jobApplicationRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(job));

        JobApplication result = service.getOwnedEntityOrThrow(10L, OWNER_ID);

        assertThat(result).isSameAs(job);
    }

    @Test
    void getOwnedEntityOrThrow_whenOwnedByAnotherUser_throwsNotFound() {
        when(jobApplicationRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnedEntityOrThrow(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withNoParams_usesDefaultPageSizeAndSort() {
        when(jobApplicationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAll(OWNER_ID, null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jobApplicationRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(used.getSort().getOrderFor("createdAt").getDirection().isDescending()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withExplicitParams_honorsPageSizeAndAscendingSort() {
        when(jobApplicationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAll(OWNER_ID, "TCS", "Java Developer", "LinkedIn", "INTERVIEW", "java", 2, 5, "company", "asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jobApplicationRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(2);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort().getOrderFor("company").getDirection().isAscending()).isTrue();
    }

    @Test
    void getAll_withInvalidStatus_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.getAll(OWNER_ID, null, null, null, "NOT_A_REAL_STATUS", null, null, null, null, null)
        ).isInstanceOf(BadRequestException.class);
    }
}
