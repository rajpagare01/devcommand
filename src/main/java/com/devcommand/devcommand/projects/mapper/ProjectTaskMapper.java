package com.devcommand.devcommand.projects.mapper;

import com.devcommand.devcommand.projects.dto.ProjectTaskRequest;
import com.devcommand.devcommand.projects.dto.ProjectTaskResponse;
import com.devcommand.devcommand.projects.entity.ProjectTask;
import org.springframework.stereotype.Component;

@Component
public class ProjectTaskMapper {

    public ProjectTask toEntity(ProjectTaskRequest request) {
        if (request == null) return null;
        ProjectTask task = new ProjectTask();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return task;
    }

    public void updateEntityFromRequest(ProjectTaskRequest request, ProjectTask task) {
        if (request == null || task == null) return;
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
    }

    public ProjectTaskResponse toResponse(ProjectTask task) {
        if (task == null) return null;
        return new ProjectTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
