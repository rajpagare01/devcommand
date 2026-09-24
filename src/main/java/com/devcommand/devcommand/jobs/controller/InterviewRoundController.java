package com.devcommand.devcommand.jobs.controller;

import com.devcommand.devcommand.jobs.dto.CreateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.dto.InterviewRoundResponse;
import com.devcommand.devcommand.jobs.dto.UpdateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.service.InterviewRoundService;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Nested under the parent job's id. Every method delegates straight to
 * InterviewRoundService, which is what actually enforces "jobId must be
 * owned by the caller before any round operation proceeds" - the
 * controller itself does no ownership checking, same thin-controller
 * pattern as the rest of the project.
 */
@RestController
@RequestMapping("/api/jobs/{jobId}/interviews")
@RequiredArgsConstructor
public class InterviewRoundController {

    private final InterviewRoundService interviewRoundService;

    @PostMapping
    public ResponseEntity<InterviewRoundResponse> create(
            @PathVariable Long jobId,
            @Valid @RequestBody CreateInterviewRoundRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        InterviewRoundResponse created = interviewRoundService.create(jobId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<InterviewRoundResponse>> getAll(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(interviewRoundService.getAllForJob(jobId, principal.getId()));
    }

    @GetMapping("/{roundId}")
    public ResponseEntity<InterviewRoundResponse> getById(
            @PathVariable Long jobId,
            @PathVariable Long roundId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(interviewRoundService.getById(jobId, roundId, principal.getId()));
    }

    @PutMapping("/{roundId}")
    public ResponseEntity<InterviewRoundResponse> update(
            @PathVariable Long jobId,
            @PathVariable Long roundId,
            @Valid @RequestBody UpdateInterviewRoundRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(interviewRoundService.update(jobId, roundId, principal.getId(), request));
    }

    @DeleteMapping("/{roundId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long jobId,
            @PathVariable Long roundId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        interviewRoundService.delete(jobId, roundId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
