package com.devcommand.devcommand.projects.repository;

import com.devcommand.devcommand.projects.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    Optional<Project> findByIdAndUserId(Long id, Long userId);
    
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, com.devcommand.devcommand.projects.entity.ProjectStatus status);

    public interface ProjectAnalyticsProjection {
        Long getTotal();
        Long getPlanning();
        Long getInProgress();
        Long getCompleted();
        Long getOnHold();
        Long getArchived();
    }

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "COUNT(p) as total, " +
            "SUM(CASE WHEN p.status = 'PLANNING' THEN 1 ELSE 0 END) as planning, " +
            "SUM(CASE WHEN p.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress, " +
            "SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
            "SUM(CASE WHEN p.status = 'ON_HOLD' THEN 1 ELSE 0 END) as onHold, " +
            "SUM(CASE WHEN p.status = 'ARCHIVED' THEN 1 ELSE 0 END) as archived " +
            "FROM Project p WHERE p.user.id = :userId")
    ProjectAnalyticsProjection getProjectAnalyticsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "COUNT(p) as total, " +
            "SUM(CASE WHEN p.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as active, " +
            "SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed " +
            "FROM Project p WHERE p.user.id = :userId")
    ProjectOverviewProjection getProjectOverviewByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    interface ProjectOverviewProjection {
        Long getTotal();
        Long getActive();
        Long getCompleted();
    }
}
