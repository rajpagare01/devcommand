package com.devcommand.devcommand.tasks.controller;

import com.devcommand.devcommand.security.UserPrincipal;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.dto.UpdateDailyTaskRequest;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Every method here pulls the authenticated user's id off the security
 * context (via @AuthenticationPrincipal, populated by
 * JwtAuthenticationFilter) and delegates to DailyTaskService. No business
 * logic, no direct repository access - same pattern as
 * DsaProblemController.
 *
 * /today, /upcoming and /completed are declared before /{id} for
 * readability. Spring MVC's path matcher already disambiguates the static
 * "/today" segment from the "{id}" variable correctly regardless of
 * declaration order, so this ordering isn't load-bearing - it just reads
 * more naturally grouped with the other collection-level endpoints.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class DailyTaskController {

    private final DailyTaskService dailyTaskService;

    @PostMapping
    public ResponseEntity<DailyTaskResponse> create(
            @Valid @RequestBody CreateDailyTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DailyTaskResponse created = dailyTaskService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<DailyTaskResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Page<DailyTaskResponse> result = dailyTaskService.getAll(
                principal.getId(), category, priority, status, dueDate, page, size, sortBy, direction
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/today")
    public ResponseEntity<List<DailyTaskResponse>> today(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dailyTaskService.today(principal.getId()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<DailyTaskResponse>> upcoming(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dailyTaskService.upcoming(principal.getId()));
    }

    @GetMapping("/completed")
    public ResponseEntity<List<DailyTaskResponse>> completed(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dailyTaskService.completed(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DailyTaskResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dailyTaskService.getById(id, principal.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DailyTaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDailyTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dailyTaskService.update(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        dailyTaskService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<DailyTaskResponse> complete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dailyTaskService.complete(id, principal.getId()));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<DailyTaskResponse> start(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(dailyTaskService.start(id, principal.getId()));
    }
}
