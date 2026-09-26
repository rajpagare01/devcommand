package com.devcommand.devcommand.learning.controller;

import com.devcommand.devcommand.learning.dto.CreateLearningTopicRequest;
import com.devcommand.devcommand.learning.dto.LearningTopicResponse;
import com.devcommand.devcommand.learning.dto.ProgressUpdateRequest;
import com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest;
import com.devcommand.devcommand.learning.service.LearningTopicService;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Every method pulls the authenticated user's id off the security context
 * (via @AuthenticationPrincipal, populated by JwtAuthenticationFilter) and
 * delegates to LearningTopicService. No business logic, no direct
 * repository access - same pattern as the DSA/Tasks/Jobs controllers.
 *
 * /completed and /in-progress are declared before /{id} for readability;
 * Spring MVC's path matcher already disambiguates the static segments from
 * the {id} variable correctly regardless of declaration order.
 */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningTopicController {

    private final LearningTopicService learningTopicService;

    @PostMapping
    public ResponseEntity<LearningTopicResponse> create(
            @Valid @RequestBody CreateLearningTopicRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LearningTopicResponse created = learningTopicService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<LearningTopicResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String technology,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer progress,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Page<LearningTopicResponse> result = learningTopicService.getAll(
                principal.getId(), technology, status, progress, search, page, size, sortBy, direction
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<LearningTopicResponse>> completed(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(learningTopicService.completedTopics(principal.getId()));
    }

    @GetMapping("/in-progress")
    public ResponseEntity<List<LearningTopicResponse>> inProgress(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(learningTopicService.inProgressTopics(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LearningTopicResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(learningTopicService.getById(id, principal.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LearningTopicResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningTopicRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(learningTopicService.update(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        learningTopicService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<LearningTopicResponse> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody ProgressUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(learningTopicService.updateProgress(id, principal.getId(), request));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<LearningTopicResponse> complete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(learningTopicService.complete(id, principal.getId()));
    }
}
