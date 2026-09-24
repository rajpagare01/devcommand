package com.devcommand.devcommand.jobs.repository;

import com.devcommand.devcommand.jobs.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * JpaSpecificationExecutor backs the combinable, optional filters + search
 * on GET /api/jobs, same pattern as DsaProblemRepository/DailyTaskRepository.
 *
 * findByIdAndUserId is the ownership-safe alternative to findById(id),
 * used for every single-record job operation (get one/update/delete/
 * change status) AND as the entry point interview-round operations use to
 * verify the parent job's ownership before touching any InterviewRound.
 */
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);
}
