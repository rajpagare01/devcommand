package com.devcommand.devcommand.projects.service;

import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.projects.dto.ProjectRequest;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.entity.Project;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.mapper.ProjectMapper;
import com.devcommand.devcommand.projects.repository.ProjectRepository;
import com.devcommand.devcommand.projects.repository.ProjectSpecifications;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "name", "startDate", "endDate", "status"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper mapper;

    @Transactional
    public ProjectResponse create(ProjectRequest request, Long userId) {
        User owner = userRepository.getReferenceById(userId);
        Project project = mapper.toEntity(request);
        project.setUser(owner);
        Project saved = projectRepository.save(project);
        return mapper.toResponse(saved);
    }

    public Page<ProjectResponse> getAll(
            Long userId,
            ProjectStatus status,
            String search,
            Integer page,
            Integer size,
            String sortBy,
            String direction
    ) {
        Specification<Project> spec = Specification.where(ProjectSpecifications.ownerIs(userId));
        
        if (status != null) {
            spec = spec.and(ProjectSpecifications.statusEquals(status));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(ProjectSpecifications.searchName(search));
        }

        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return projectRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    public ProjectResponse getById(Long id, Long userId) {
        return mapper.toResponse(getOwnedEntityOrThrow(id, userId));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request, Long userId) {
        Project project = getOwnedEntityOrThrow(id, userId);
        mapper.updateEntityFromRequest(request, project);
        return mapper.toResponse(project);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Project project = getOwnedEntityOrThrow(id, userId);
        projectRepository.delete(project);
    }

    @Transactional
    public ProjectResponse updateStatus(Long id, ProjectStatus status, Long userId) {
        Project project = getOwnedEntityOrThrow(id, userId);
        project.setStatus(status);
        return mapper.toResponse(project);
    }

    public List<ProjectResponse> getActiveProjects(Long userId) {
        Specification<Project> spec = Specification.where(ProjectSpecifications.ownerIs(userId))
                .and(ProjectSpecifications.statusIn(ProjectStatus.PLANNING, ProjectStatus.IN_PROGRESS));
        return projectRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    public List<ProjectResponse> getCompletedProjects(Long userId) {
        Specification<Project> spec = Specification.where(ProjectSpecifications.ownerIs(userId))
                .and(ProjectSpecifications.statusEquals(ProjectStatus.COMPLETED));
        return projectRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    public Project getOwnedEntityOrThrow(Long id, Long userId) {
        return projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
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
