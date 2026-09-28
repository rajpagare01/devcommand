package com.devcommand.devcommand.jobs.repository;

import com.devcommand.devcommand.jobs.entity.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Deliberately does NOT query by user - InterviewRoundService always
 * resolves and ownership-verifies the parent JobApplication first (via
 * JobApplicationService, which uses JobApplicationRepository.findByIdAndUserId),
 * then every method here is scoped to that already-verified jobApplicationId.
 * This is what makes "a round can never be accessed through another user's
 * job application" true even though InterviewRound has no direct user
 * relationship of its own to filter on.
 */
public interface InterviewRoundRepository extends JpaRepository<InterviewRound, Long> {

    List<InterviewRound> findByJobApplicationId(Long jobApplicationId);

    Optional<InterviewRound> findByIdAndJobApplicationId(Long id, Long jobApplicationId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM InterviewRound i WHERE i.jobApplication.user.id = :userId")
    long countByJobApplicationUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM InterviewRound i WHERE i.jobApplication.user.id = :userId AND i.status = :status")
    long countByJobApplicationUserIdAndStatus(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("status") com.devcommand.devcommand.jobs.entity.InterviewStatus status);
}
