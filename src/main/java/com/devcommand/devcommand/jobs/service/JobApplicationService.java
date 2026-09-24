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
import com.devcommand.devcommand.jobs.repository.JobApplicationSpecifications;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * All job-application business logic lives here, mirroring
 * DsaProblemService/DailyTaskService's shape.
 *
 * Every method takes the caller's user id as an explicit parameter rather
 * than reading SecurityContextHolder itself - same rationale as the other
 * two modules: it's what keeps this testable with plain Mockito, and what
 * would let a future WhatsApp command parser call create(...)/changeStatus(...)
 * directly with a resolved userId, no service changes needed.
 */
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "applicationDate", "createdAt", "company", "status"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final JobApplicationMapper mapper;

    @Transactional
    public JobApplicationResponse create(CreateJobApplicationRequest request, Long userId) {
        User owner = userRepository.getReferenceById(userId);
        JobApplication job = mapper.toEntity(request, owner);
        JobApplication saved = jobApplicationRepository.save(job);
        return mapper.toResponse(saved);
    }

    public Page<JobApplicationResponse> getAll(
            Long userId,
            String company,
            String role,
            String source,
            String statusParam,
            String search,
            Integer page,
            Integer size,
            String sortBy,
            String direction
    ) {
        Specification<JobApplication> spec = buildSpecification(userId, company, role, source, statusParam, search);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return jobApplicationRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    public JobApplicationResponse getById(Long id, Long userId) {
        return mapper.toResponse(findOwned(id, userId));
    }

    @Transactional
    public JobApplicationResponse update(Long id, Long userId, UpdateJobApplicationRequest request) {
        JobApplication job = findOwned(id, userId);
        mapper.applyUpdate(job, request);
        return mapper.toResponse(job); // managed entity - flushed automatically at commit
    }

    @Transactional
    public void delete(Long id, Long userId) {
        JobApplication job = findOwned(id, userId);
        jobApplicationRepository.delete(job); // cascades to interview rounds (orphanRemoval on the entity)
    }

    @Transactional
    public JobApplicationResponse changeStatus(Long id, Long userId, JobStatusUpdateRequest request) {
        JobApplication job = findOwned(id, userId);
        job.setStatus(request.status());
        return mapper.toResponse(job);
    }

    /**
     * Ownership-verified entity lookup for reuse by InterviewRoundService.
     * Returns the managed entity (not a DTO) so interview-round operations
     * can attach to / query against the real jobApplication_id - but the
     * ownership check (findByIdAndUserId) is identical to every other
     * single-record lookup in this service, so a non-owner is rejected
     * here exactly as they would be for GET /api/jobs/{id} itself.
     */
    public JobApplication getOwnedEntityOrThrow(Long jobId, Long userId) {
        return findOwned(jobId, userId);
    }

    // ---- helpers ---------------------------------------------------------

    private JobApplication findOwned(Long id, Long userId) {
        return jobApplicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found: " + id));
    }

    private Specification<JobApplication> buildSpecification(
            Long userId, String company, String role, String source, String statusParam, String search
    ) {
        List<Specification<JobApplication>> specs = new ArrayList<>();
        specs.add(JobApplicationSpecifications.ownerIs(userId));

        if (company != null && !company.isBlank()) {
            specs.add(JobApplicationSpecifications.companyEquals(company));
        }
        if (role != null && !role.isBlank()) {
            specs.add(JobApplicationSpecifications.roleEquals(role));
        }
        if (source != null && !source.isBlank()) {
            specs.add(JobApplicationSpecifications.sourceEquals(source));
        }
        if (statusParam != null && !statusParam.isBlank()) {
            specs.add(JobApplicationSpecifications.statusEquals(parseStatus(statusParam)));
        }
        if (search != null && !search.isBlank()) {
            specs.add(JobApplicationSpecifications.searchKeyword(search));
        }

        return Specification.allOf(specs);
    }

    private ApplicationStatus parseStatus(String rawValue) {
        try {
            return ApplicationStatus.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value: " + rawValue);
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
