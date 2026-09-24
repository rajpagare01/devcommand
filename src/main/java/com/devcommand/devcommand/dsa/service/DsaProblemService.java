package com.devcommand.devcommand.dsa.service;

import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.dto.UpdateDsaProblemRequest;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.mapper.DsaProblemMapper;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.dsa.repository.DsaProblemSpecifications;
import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * All DSA business logic lives here, per the architecture brief - the
 * controller only translates HTTP <-> these method calls.
 *
 * Every method takes the authenticated user's id as an explicit parameter
 * (rather than reaching into SecurityContextHolder itself), so ownership
 * enforcement is visible in the method signature and the service stays
 * testable with plain Mockito, with no Spring Security context needed.
 */
@Service
@RequiredArgsConstructor
public class DsaProblemService {

    /** Whitelisted sort fields - prevents ?sortBy=<arbitrary/invalid property> from
     *  producing a 500 (Spring Data throws PropertyReferenceException for unknown
     *  properties) and keeps sorting to fields that make sense for this resource. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "dateSolved", "revisionDate", "difficulty", "status"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final DsaProblemRepository dsaProblemRepository;
    private final UserRepository userRepository;
    private final DsaProblemMapper mapper;

    @Transactional
    public DsaProblemResponse create(CreateDsaProblemRequest request, Long userId) {
        User owner = userRepository.getReferenceById(userId);
        DsaProblem problem = mapper.toEntity(request, owner);
        DsaProblem saved = dsaProblemRepository.save(problem);
        return mapper.toResponse(saved);
    }

    public Page<DsaProblemResponse> getAll(
            Long userId,
            String topic,
            String platform,
            String difficultyParam,
            String statusParam,
            Integer page,
            Integer size,
            String sortBy,
            String direction
    ) {
        Specification<DsaProblem> spec = buildSpecification(userId, topic, platform, difficultyParam, statusParam);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return dsaProblemRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    public DsaProblemResponse getById(Long id, Long userId) {
        return mapper.toResponse(findOwned(id, userId));
    }

    @Transactional
    public DsaProblemResponse update(Long id, Long userId, UpdateDsaProblemRequest request) {
        DsaProblem problem = findOwned(id, userId);
        mapper.applyUpdate(problem, request);
        return mapper.toResponse(problem); // managed entity - flushed automatically at commit
    }

    @Transactional
    public void delete(Long id, Long userId) {
        DsaProblem problem = findOwned(id, userId);
        dsaProblemRepository.delete(problem);
    }

    @Transactional
    public DsaProblemResponse markSolved(Long id, Long userId) {
        DsaProblem problem = findOwned(id, userId);
        problem.setStatus(ProblemStatus.SOLVED);
        if (problem.getDateSolved() == null) {
            problem.setDateSolved(LocalDate.now());
        }
        return mapper.toResponse(problem);
    }

    @Transactional
    public DsaProblemResponse markForRevision(Long id, Long userId) {
        DsaProblem problem = findOwned(id, userId);
        problem.setStatus(ProblemStatus.REVISION);
        return mapper.toResponse(problem);
    }

    // ---- helpers ---------------------------------------------------------

    /** Ownership-safe single-record lookup used by every by-id operation. */
    private DsaProblem findOwned(Long id, Long userId) {
        return dsaProblemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("DSA problem not found: " + id));
    }

    private Specification<DsaProblem> buildSpecification(
            Long userId, String topic, String platform, String difficultyParam, String statusParam
    ) {
        List<Specification<DsaProblem>> specs = new ArrayList<>();
        specs.add(DsaProblemSpecifications.ownerIs(userId));

        if (topic != null && !topic.isBlank()) {
            specs.add(DsaProblemSpecifications.topicEquals(topic));
        }
        if (platform != null && !platform.isBlank()) {
            specs.add(DsaProblemSpecifications.platformEquals(platform));
        }
        if (difficultyParam != null && !difficultyParam.isBlank()) {
            specs.add(DsaProblemSpecifications.difficultyEquals(parseEnum(Difficulty.class, difficultyParam, "difficulty")));
        }
        if (statusParam != null && !statusParam.isBlank()) {
            specs.add(DsaProblemSpecifications.statusEquals(parseEnum(ProblemStatus.class, statusParam, "status")));
        }

        return Specification.allOf(specs);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, String fieldName) {
        try {
            return Enum.valueOf(enumType, rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid " + fieldName + " value: " + rawValue);
        }
    }

    private Pageable buildPageable(Integer page, Integer size, String sortBy, String direction) {
        int resolvedPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int resolvedSize = (size != null && size > 0) ? size : DEFAULT_SIZE;

        String resolvedSortField = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy))
                ? sortBy
                : DEFAULT_SORT_FIELD;

        Sort.Direction resolvedDirection = ("asc".equalsIgnoreCase(direction))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC; // default: desc (newest first)

        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(resolvedDirection, resolvedSortField));
    }
}
