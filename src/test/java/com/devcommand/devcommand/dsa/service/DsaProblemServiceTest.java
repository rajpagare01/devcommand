package com.devcommand.devcommand.dsa.service;

import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.dto.UpdateDsaProblemRequest;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.mapper.DsaProblemMapper;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DsaProblemServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private DsaProblemRepository dsaProblemRepository;

    @Mock
    private UserRepository userRepository;

    // Plain mapping logic, no dependencies of its own - a real instance is
    // more useful here than a mock and keeps the test honest about what the
    // service actually returns.
    private final DsaProblemMapper mapper = new DsaProblemMapper();

    private DsaProblemService service;

    @BeforeEach
    void setUp() {
        service = new DsaProblemService(dsaProblemRepository, userRepository, mapper);
    }

    private DsaProblem sampleProblem(Long id, Long ownerId) {
        User owner = User.builder().id(ownerId).name("Ada").email("ada@example.com").password("hash").build();
        return DsaProblem.builder()
                .id(id)
                .title("Two Sum")
                .platform("LEETCODE")
                .topic("ARRAY")
                .difficulty(Difficulty.EASY)
                .status(ProblemStatus.TODO)
                .user(owner)
                .build();
    }

    @Test
    void create_savesProblemOwnedByTheGivenUser_andReturnsResponse() {
        User owner = User.builder().id(OWNER_ID).name("Ada").email("ada@example.com").password("hash").build();
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);

        CreateDsaProblemRequest request = new CreateDsaProblemRequest(
                "Two Sum", "LEETCODE", "https://leetcode.com/problems/two-sum/", "ARRAY",
                Difficulty.EASY, ProblemStatus.SOLVED, LocalDate.of(2026, 9, 23), 25,
                "Solved with HashMap", LocalDate.of(2026, 9, 30)
        );

        ArgumentCaptor<DsaProblem> captor = ArgumentCaptor.forClass(DsaProblem.class);
        when(dsaProblemRepository.save(captor.capture())).thenAnswer(inv -> {
            DsaProblem toSave = inv.getArgument(0);
            toSave.setId(100L);
            return toSave;
        });

        DsaProblemResponse response = service.create(request, OWNER_ID);

        assertThat(captor.getValue().getUser()).isEqualTo(owner);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("Two Sum");
        assertThat(response.status()).isEqualTo(ProblemStatus.SOLVED);
    }

    @Test
    void getById_whenOwnedByCaller_returnsProblem() {
        DsaProblem problem = sampleProblem(10L, OWNER_ID);
        when(dsaProblemRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(problem));

        DsaProblemResponse response = service.getById(10L, OWNER_ID);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void getById_whenOwnedByAnotherUser_throwsNotFound_ratherThanExposingIt() {
        // User B (OTHER_USER_ID) tries to read User A's (OWNER_ID) problem #10.
        // findByIdAndUserId(10, OTHER_USER_ID) correctly finds nothing, since
        // the WHERE clause requires both id AND user_id to match.
        when(dsaProblemRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_whenOwnedByCaller_appliesAllEditableFields() {
        DsaProblem problem = sampleProblem(10L, OWNER_ID);
        when(dsaProblemRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(problem));

        UpdateDsaProblemRequest request = new UpdateDsaProblemRequest(
                "Two Sum (revisited)", "LEETCODE", "https://leetcode.com/problems/two-sum/", "HASHMAP",
                Difficulty.EASY, ProblemStatus.MASTERED, LocalDate.of(2026, 9, 23), 10,
                "Optimal solution now", LocalDate.of(2026, 10, 15)
        );

        DsaProblemResponse response = service.update(10L, OWNER_ID, request);

        assertThat(response.title()).isEqualTo("Two Sum (revisited)");
        assertThat(response.topic()).isEqualTo("HASHMAP");
        assertThat(response.status()).isEqualTo(ProblemStatus.MASTERED);
        assertThat(response.timeTaken()).isEqualTo(10);
    }

    @Test
    void update_whenOwnedByAnotherUser_throwsNotFound() {
        when(dsaProblemRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        UpdateDsaProblemRequest request = new UpdateDsaProblemRequest(
                "x", "x", null, "x", Difficulty.EASY, ProblemStatus.TODO, null, null, null, null
        );

        assertThatThrownBy(() -> service.update(10L, OTHER_USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_whenOwnedByCaller_deletesTheEntity() {
        DsaProblem problem = sampleProblem(10L, OWNER_ID);
        when(dsaProblemRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(problem));

        service.delete(10L, OWNER_ID);

        verify(dsaProblemRepository, times(1)).delete(problem);
    }

    @Test
    void delete_whenOwnedByAnotherUser_throwsNotFound_andNeverDeletes() {
        when(dsaProblemRepository.findByIdAndUserId(10L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(dsaProblemRepository, never()).delete(any());
    }

    @Test
    void markSolved_setsStatusSolved_andFillsDateSolvedWhenMissing() {
        DsaProblem problem = sampleProblem(10L, OWNER_ID); // status TODO, dateSolved null
        when(dsaProblemRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(problem));

        DsaProblemResponse response = service.markSolved(10L, OWNER_ID);

        assertThat(response.status()).isEqualTo(ProblemStatus.SOLVED);
        assertThat(response.dateSolved()).isEqualTo(LocalDate.now());
    }

    @Test
    void markSolved_doesNotOverwriteAnExistingDateSolved() {
        DsaProblem problem = sampleProblem(10L, OWNER_ID);
        LocalDate originalDate = LocalDate.of(2026, 1, 1);
        problem.setDateSolved(originalDate);
        when(dsaProblemRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(problem));

        DsaProblemResponse response = service.markSolved(10L, OWNER_ID);

        assertThat(response.dateSolved()).isEqualTo(originalDate);
    }

    @Test
    void markForRevision_setsStatusRevision() {
        DsaProblem problem = sampleProblem(10L, OWNER_ID);
        when(dsaProblemRepository.findByIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(problem));

        DsaProblemResponse response = service.markForRevision(10L, OWNER_ID);

        assertThat(response.status()).isEqualTo(ProblemStatus.REVISION);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withNoParams_usesDefaultPageSizeAndSort() {
        when(dsaProblemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        service.getAll(OWNER_ID, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dsaProblemRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(used.getSort().getOrderFor("createdAt").getDirection().isDescending()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withExplicitParams_honorsPageSizeAndAscendingSort() {
        when(dsaProblemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        service.getAll(OWNER_ID, "ARRAY", "LEETCODE", "EASY", "SOLVED", 2, 5, "title", "asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dsaProblemRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(2);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort().getOrderFor("title").getDirection().isAscending()).isTrue();
    }

    @Test
    void getAll_withInvalidDifficulty_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.getAll(OWNER_ID, null, null, "NOT_A_REAL_DIFFICULTY", null, null, null, null, null)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getAll_withInvalidStatus_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.getAll(OWNER_ID, null, null, null, "NOT_A_REAL_STATUS", null, null, null, null)
        ).isInstanceOf(BadRequestException.class);
    }
}
