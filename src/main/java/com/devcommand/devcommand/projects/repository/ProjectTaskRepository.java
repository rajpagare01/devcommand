package com.devcommand.devcommand.projects.repository;

import com.devcommand.devcommand.projects.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long>, JpaSpecificationExecutor<ProjectTask> {
    Optional<ProjectTask> findByIdAndProjectId(Long id, Long projectId);
}
