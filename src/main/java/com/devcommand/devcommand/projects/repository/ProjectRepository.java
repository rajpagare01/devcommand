package com.devcommand.devcommand.projects.repository;

import com.devcommand.devcommand.projects.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
