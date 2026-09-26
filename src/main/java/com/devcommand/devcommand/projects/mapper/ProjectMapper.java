package com.devcommand.devcommand.projects.mapper;

import com.devcommand.devcommand.projects.dto.ProjectRequest;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toEntity(ProjectRequest request) {
        if (request == null) return null;
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setGithubUrl(request.githubUrl());
        project.setLiveUrl(request.liveUrl());
        project.setStatus(request.status());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        return project;
    }

    public void updateEntityFromRequest(ProjectRequest request, Project project) {
        if (request == null || project == null) return;
        project.setName(request.name());
        project.setDescription(request.description());
        project.setGithubUrl(request.githubUrl());
        project.setLiveUrl(request.liveUrl());
        project.setStatus(request.status());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
    }

    public ProjectResponse toResponse(Project project) {
        if (project == null) return null;
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getGithubUrl(),
                project.getLiveUrl(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
