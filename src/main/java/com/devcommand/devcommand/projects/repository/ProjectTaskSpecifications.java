package com.devcommand.devcommand.projects.repository;

import com.devcommand.devcommand.projects.entity.ProjectTask;
import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import org.springframework.data.jpa.domain.Specification;

public class ProjectTaskSpecifications {

    public static Specification<ProjectTask> projectIs(Long projectId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<ProjectTask> statusEquals(ProjectTaskStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<ProjectTask> statusNotEquals(ProjectTaskStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("status"), status);
    }

    public static Specification<ProjectTask> priorityEquals(ProjectTaskPriority priority) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("priority"), priority);
    }
}
