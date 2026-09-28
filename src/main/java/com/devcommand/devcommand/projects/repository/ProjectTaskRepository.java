package com.devcommand.devcommand.projects.repository;

import com.devcommand.devcommand.projects.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long>, JpaSpecificationExecutor<ProjectTask> {
    Optional<ProjectTask> findByIdAndProjectId(Long id, Long projectId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM ProjectTask t WHERE t.project.user.id = :userId")
    long countByProjectUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM ProjectTask t WHERE t.project.user.id = :userId AND t.status = :status")
    long countByProjectUserIdAndStatus(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("status") com.devcommand.devcommand.projects.entity.ProjectTaskStatus status);

    public interface ProjectTaskAnalyticsProjection {
        Long getTotal();
        Long getTodo();
        Long getInProgress();
        Long getDone();
    }

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "COUNT(t) as total, " +
            "SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END) as todo, " +
            "SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress, " +
            "SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) as done " +
            "FROM ProjectTask t WHERE t.project.user.id = :userId")
    ProjectTaskAnalyticsProjection getProjectTaskAnalyticsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "COUNT(t) as total, " +
            "SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) as completed " +
            "FROM ProjectTask t WHERE t.project.user.id = :userId")
    ProjectTaskOverviewProjection getProjectTaskOverviewByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    interface ProjectTaskOverviewProjection {
        Long getTotal();
        Long getCompleted();
    }
}
