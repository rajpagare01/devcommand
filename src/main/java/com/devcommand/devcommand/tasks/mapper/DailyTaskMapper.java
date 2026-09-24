package com.devcommand.devcommand.tasks.mapper;

import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.dto.UpdateDailyTaskRequest;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Plain hand-written mapper, matching the pattern already established by
 * DsaProblemMapper - no MapStruct/ModelMapper dependency for a handful of
 * straightforward field copies.
 */
@Component
public class DailyTaskMapper {

    /** Builds a new, unsaved entity from a create request, owned by the given user. */
    public DailyTask toEntity(CreateDailyTaskRequest request, User owner) {
        return DailyTask.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .priority(request.priority())
                .status(request.status())
                .dueDate(request.dueDate())
                .user(owner)
                .build();
    }

    /**
     * Applies a full-replace update request onto an already-loaded, owned
     * entity. Never touches completedAt or user - completedAt is only ever
     * set by the dedicated /complete transition, and the owner is fixed at
     * creation time and not editable through any DTO.
     */
    public void applyUpdate(DailyTask task, UpdateDailyTaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setCategory(request.category());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
    }

    public DailyTaskResponse toResponse(DailyTask task) {
        return new DailyTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategory(),
                task.getPriority(),
                task.getStatus(),
                task.getDueDate(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
