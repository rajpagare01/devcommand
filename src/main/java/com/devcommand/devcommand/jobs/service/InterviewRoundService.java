package com.devcommand.devcommand.jobs.service;

import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.jobs.dto.CreateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.dto.InterviewRoundResponse;
import com.devcommand.devcommand.jobs.dto.UpdateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.entity.InterviewRound;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.mapper.InterviewRoundMapper;
import com.devcommand.devcommand.jobs.repository.InterviewRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Every method here first resolves the parent job through
 * JobApplicationService.getOwnedEntityOrThrow(jobId, userId) - which is the
 * exact same ownership check used by GET/PUT/DELETE /api/jobs/{id} - before
 * touching any InterviewRound. That single check is what makes "a round can
 * never be reached through another user's job application" true: if the
 * job doesn't belong to the caller, execution never gets far enough to look
 * at rounds at all, regardless of which round id was requested.
 *
 * Depending on JobApplicationService (rather than reaching into
 * JobApplicationRepository directly) keeps the ownership rule defined in
 * exactly one place.
 */
@Service
@RequiredArgsConstructor
public class InterviewRoundService {

    private final InterviewRoundRepository interviewRoundRepository;
    private final JobApplicationService jobApplicationService;
    private final InterviewRoundMapper mapper;

    @Transactional
    public InterviewRoundResponse create(Long jobId, Long userId, CreateInterviewRoundRequest request) {
        JobApplication job = jobApplicationService.getOwnedEntityOrThrow(jobId, userId);
        InterviewRound round = mapper.toEntity(request, job);
        InterviewRound saved = interviewRoundRepository.save(round);
        return mapper.toResponse(saved);
    }

    public List<InterviewRoundResponse> getAllForJob(Long jobId, Long userId) {
        jobApplicationService.getOwnedEntityOrThrow(jobId, userId); // ownership check only
        return interviewRoundRepository.findByJobApplicationId(jobId)
                .stream().map(mapper::toResponse).toList();
    }

    public InterviewRoundResponse getById(Long jobId, Long roundId, Long userId) {
        jobApplicationService.getOwnedEntityOrThrow(jobId, userId);
        return mapper.toResponse(findOwnedRound(jobId, roundId));
    }

    @Transactional
    public InterviewRoundResponse update(Long jobId, Long roundId, Long userId, UpdateInterviewRoundRequest request) {
        jobApplicationService.getOwnedEntityOrThrow(jobId, userId);
        InterviewRound round = findOwnedRound(jobId, roundId);
        mapper.applyUpdate(round, request);
        return mapper.toResponse(round); // managed entity - flushed automatically at commit
    }

    @Transactional
    public void delete(Long jobId, Long roundId, Long userId) {
        jobApplicationService.getOwnedEntityOrThrow(jobId, userId);
        InterviewRound round = findOwnedRound(jobId, roundId);
        interviewRoundRepository.delete(round);
    }

    /** Round lookup scoped to the already-ownership-verified parent job. */
    private InterviewRound findOwnedRound(Long jobId, Long roundId) {
        return interviewRoundRepository.findByIdAndJobApplicationId(roundId, jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found: " + roundId));
    }
}
