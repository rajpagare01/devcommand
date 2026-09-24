package com.devcommand.devcommand.tasks.repository;

import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Small, explicit Specification builders - matching the pattern already
 * established by DsaProblemSpecifications. DailyTaskService always
 * includes ownerIs(userId) first in every combination it builds, so
 * filtering can never return another user's tasks.
 */
public final class DailyTaskSpecifications {

    private DailyTaskSpecifications() {
    }

    public static Specification<DailyTask> ownerIs(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<DailyTask> categoryEquals(TaskCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<DailyTask> priorityEquals(TaskPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<DailyTask> statusEquals(DailyTaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<DailyTask> statusNot(DailyTaskStatus status) {
        return (root, query, cb) -> cb.notEqual(root.get("status"), status);
    }

    public static Specification<DailyTask> dueDateEquals(LocalDate date) {
        return (root, query, cb) -> cb.equal(root.get("dueDate"), date);
    }

    public static Specification<DailyTask> dueDateAfter(LocalDate date) {
        return (root, query, cb) -> cb.greaterThan(root.get("dueDate"), date);
    }
}
