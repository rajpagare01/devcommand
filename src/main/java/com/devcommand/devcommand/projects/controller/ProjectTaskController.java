package com.devcommand.devcommand.projects.controller;

import com.devcommand.devcommand.projects.dto.ProjectTaskPriorityUpdateRequest;
import com.devcommand.devcommand.projects.dto.ProjectTaskRequest;
import com.devcommand.devcommand.projects.dto.ProjectTaskResponse;
import com.devcommand.devcommand.projects.dto.ProjectTaskStatusUpdateRequest;
import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import com.devcommand.devcommand.projects.service.ProjectTaskService;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class ProjectTaskController {

    private final ProjectTaskService projectTaskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectTaskResponse create(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectTaskRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectTaskService.create(projectId, request, userPrincipal.getId());
    }

    @GetMapping
    public Page<ProjectTaskResponse> getAll(
            @PathVariable Long projectId,
            @RequestParam(required = false) ProjectTaskStatus status,
            @RequestParam(required = false) ProjectTaskPriority priority,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectTaskService.getAllForProject(projectId, userPrincipal.getId(), status, priority, page, size, sortBy, direction);
    }

    @GetMapping("/pending")
    public List<ProjectTaskResponse> getPendingTasks(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectTaskService.getPendingTasks(projectId, userPrincipal.getId());
    }

    @GetMapping("/completed")
    public List<ProjectTaskResponse> getCompletedTasks(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectTaskService.getCompletedTasks(projectId, userPrincipal.getId());
    }

    @GetMapping("/{taskId}")
    public ProjectTaskResponse getById(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectTaskService.getById(projectId, taskId, userPrincipal.getId());
    }

    @PutMapping("/{taskId}")
    public ProjectTaskResponse update(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody ProjectTaskRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectTaskService.update(projectId, taskId, request, userPrincipal.getId());
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        projectTaskService.delete(projectId, taskId, userPrincipal.getId());
    }

    @PatchMapping("/{taskId}/status")
    public ProjectTaskResponse updateStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody ProjectTaskStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectTaskService.updateStatus(projectId, taskId, request.status(), userPrincipal.getId());
    }

    @PatchMapping("/{taskId}/priority")
    public ProjectTaskResponse updatePriority(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody ProjectTaskPriorityUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectTaskService.updatePriority(projectId, taskId, request.priority(), userPrincipal.getId());
    }
}
