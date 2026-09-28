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

    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, com.devcommand.devcommand.jobs.entity.ApplicationStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT j.status as status, COUNT(j) as count FROM JobApplication j WHERE j.user.id = :userId GROUP BY j.status")
    java.util.List<ApplicationStatusCount> countStatusByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    interface ApplicationStatusCount {
        com.devcommand.devcommand.jobs.entity.ApplicationStatus getStatus();
        Long getCount();
    }

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "COUNT(j) as total, " +
           "SUM(CASE WHEN j.status IN ('APPLIED', 'SCREENING', 'INTERVIEW') THEN 1 ELSE 0 END) as active " +
           "FROM JobApplication j WHERE j.user.id = :userId")
    JobsOverviewProjection getJobsOverviewByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    interface JobsOverviewProjection {
        Long getTotal();
        Long getActive();
    }
}
