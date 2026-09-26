package com.devcommand.devcommand.projects.service;

import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.projects.dto.ProjectTaskRequest;
import com.devcommand.devcommand.projects.dto.ProjectTaskResponse;
import com.devcommand.devcommand.projects.entity.Project;
import com.devcommand.devcommand.projects.entity.ProjectTask;
import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import com.devcommand.devcommand.projects.mapper.ProjectTaskMapper;
import com.devcommand.devcommand.projects.repository.ProjectTaskRepository;
import com.devcommand.devcommand.projects.repository.ProjectTaskSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectTaskService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "dueDate", "status", "priority"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectService projectService;
    private final ProjectTaskMapper mapper;

    @Transactional
    public ProjectTaskResponse create(Long projectId, ProjectTaskRequest request, Long userId) {
        Project project = projectService.getOwnedEntityOrThrow(projectId, userId);
        ProjectTask task = mapper.toEntity(request);
        task.setProject(project);
        ProjectTask saved = projectTaskRepository.save(task);
        return mapper.toResponse(saved);
    }

    public Page<ProjectTaskResponse> getAllForProject(
            Long projectId,
            Long userId,
            ProjectTaskStatus status,
            ProjectTaskPriority priority,
            Integer page,
            Integer size,
            String sortBy,
            String direction
    ) {
        // Enforce ownership of parent project first
        projectService.getOwnedEntityOrThrow(projectId, userId);

        Specification<ProjectTask> spec = Specification.where(ProjectTaskSpecifications.projectIs(projectId));

        if (status != null) {
            spec = spec.and(ProjectTaskSpecifications.statusEquals(status));
        }
        if (priority != null) {
            spec = spec.and(ProjectTaskSpecifications.priorityEquals(priority));
        }

        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return projectTaskRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    public ProjectTaskResponse getById(Long projectId, Long taskId, Long userId) {
        return mapper.toResponse(getOwnedTaskOrThrow(projectId, taskId, userId));
    }

    @Transactional
    public ProjectTaskResponse update(Long projectId, Long taskId, ProjectTaskRequest request, Long userId) {
        ProjectTask task = getOwnedTaskOrThrow(projectId, taskId, userId);
        mapper.updateEntityFromRequest(request, task);
        return mapper.toResponse(task);
    }

    @Transactional
    public void delete(Long projectId, Long taskId, Long userId) {
        ProjectTask task = getOwnedTaskOrThrow(projectId, taskId, userId);
        projectTaskRepository.delete(task);
    }

    @Transactional
    public ProjectTaskResponse updateStatus(Long projectId, Long taskId, ProjectTaskStatus status, Long userId) {
        ProjectTask task = getOwnedTaskOrThrow(projectId, taskId, userId);
        task.setStatus(status);
        return mapper.toResponse(task);
    }

    @Transactional
    public ProjectTaskResponse updatePriority(Long projectId, Long taskId, ProjectTaskPriority priority, Long userId) {
        ProjectTask task = getOwnedTaskOrThrow(projectId, taskId, userId);
        task.setPriority(priority);
        return mapper.toResponse(task);
    }

    public List<ProjectTaskResponse> getPendingTasks(Long projectId, Long userId) {
        projectService.getOwnedEntityOrThrow(projectId, userId);
        Specification<ProjectTask> spec = Specification.where(ProjectTaskSpecifications.projectIs(projectId))
                .and(ProjectTaskSpecifications.statusNotEquals(ProjectTaskStatus.DONE));
        return projectTaskRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dueDate"))
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    public List<ProjectTaskResponse> getCompletedTasks(Long projectId, Long userId) {
        projectService.getOwnedEntityOrThrow(projectId, userId);
        Specification<ProjectTask> spec = Specification.where(ProjectTaskSpecifications.projectIs(projectId))
                .and(ProjectTaskSpecifications.statusEquals(ProjectTaskStatus.DONE));
        return projectTaskRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    private ProjectTask getOwnedTaskOrThrow(Long projectId, Long taskId, Long userId) {
        projectService.getOwnedEntityOrThrow(projectId, userId);
        return projectTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectTask not found or doesn't belong to project"));
    }

    private Pageable buildPageable(Integer page, Integer size, String sortBy, String direction) {
        int resolvedPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int resolvedSize = (size != null && size > 0) ? size : DEFAULT_SIZE;

        String resolvedSortField = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy))
                ? sortBy
                : DEFAULT_SORT_FIELD;

        Sort.Direction resolvedDirection = ("asc".equalsIgnoreCase(direction))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(resolvedDirection, resolvedSortField));
    }
}
