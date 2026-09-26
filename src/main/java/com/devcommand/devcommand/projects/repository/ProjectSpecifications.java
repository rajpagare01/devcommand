package com.devcommand.devcommand.projects.repository;

import com.devcommand.devcommand.projects.entity.Project;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import org.springframework.data.jpa.domain.Specification;

public class ProjectSpecifications {

    public static Specification<Project> ownerIs(Long userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Project> statusEquals(ProjectStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Project> statusIn(ProjectStatus... statuses) {
        return (root, query, criteriaBuilder) ->
                root.get("status").in((Object[]) statuses);
    }

    public static Specification<Project> searchName(String nameKeyword) {
        return (root, query, criteriaBuilder) -> {
            if (nameKeyword == null || nameKeyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + nameKeyword.toLowerCase() + "%");
        };
    }
}
