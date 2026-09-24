package com.devcommand.devcommand.projects.repository;

import com.devcommand.devcommand.projects.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long> {
}
