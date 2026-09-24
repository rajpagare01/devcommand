package com.devcommand.devcommand.jobs.repository;

import com.devcommand.devcommand.jobs.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
}
