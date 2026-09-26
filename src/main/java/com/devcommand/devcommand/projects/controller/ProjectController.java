package com.devcommand.devcommand.projects.controller;

import com.devcommand.devcommand.projects.dto.ProjectRequest;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.dto.ProjectStatusUpdateRequest;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.service.ProjectService;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectService.create(request, userPrincipal.getId());
    }

    @GetMapping
    public Page<ProjectResponse> getAll(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectService.getAll(userPrincipal.getId(), status, search, page, size, sortBy, direction);
    }

    @GetMapping("/active")
    public List<ProjectResponse> getActiveProjects(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectService.getActiveProjects(userPrincipal.getId());
    }

    @GetMapping("/completed")
    public List<ProjectResponse> getCompletedProjects(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectService.getCompletedProjects(userPrincipal.getId());
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectService.getById(id, userPrincipal.getId());
    }

    @PutMapping("/{id}")
    public ProjectResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectService.update(id, request, userPrincipal.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        projectService.delete(id, userPrincipal.getId());
    }

    @PatchMapping("/{id}/status")
    public ProjectResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProjectStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return projectService.updateStatus(id, request.status(), userPrincipal.getId());
    }
}
