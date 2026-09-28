package com.devcommand.devcommand.dsa.repository;

import com.devcommand.devcommand.dsa.entity.DsaProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * JpaSpecificationExecutor backs the combinable, optional filters on
 * GET /api/dsa (topic/difficulty/status/platform) without hand-writing a
 * derived-query method for every combination.
 *
 * findByIdAndUserId is the ownership-safe alternative to findById(id) - it
 * is used for every single-record operation (get one / update / delete /
 * solve / revision) so a mismatched owner simply looks like "not found" at
 * the database level, rather than "found, then a service-layer check
 * decided to reject it."
 */
public interface DsaProblemRepository extends JpaRepository<DsaProblem, Long>, JpaSpecificationExecutor<DsaProblem> {

    Optional<DsaProblem> findByIdAndUserId(Long id, Long userId);
    
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, com.devcommand.devcommand.dsa.entity.ProblemStatus status);
    long countByUserIdAndDifficulty(Long userId, com.devcommand.devcommand.dsa.entity.Difficulty difficulty);
    long countByUserIdAndStatusAndDateSolved(Long userId, com.devcommand.devcommand.dsa.entity.ProblemStatus status, java.time.LocalDate dateSolved);
    long countByUserIdAndStatusAndDateSolvedGreaterThanEqual(Long userId, com.devcommand.devcommand.dsa.entity.ProblemStatus status, java.time.LocalDate dateSolved);

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "COUNT(p) as total, " +
           "SUM(CASE WHEN p.status = 'TODO' THEN 1 ELSE 0 END) as todoCount, " +
           "SUM(CASE WHEN p.status = 'SOLVED' THEN 1 ELSE 0 END) as solvedCount, " +
           "SUM(CASE WHEN p.status = 'REVISION' THEN 1 ELSE 0 END) as revisionCount, " +
           "SUM(CASE WHEN p.status = 'MASTERED' THEN 1 ELSE 0 END) as masteredCount, " +
           "SUM(CASE WHEN p.difficulty = 'EASY' THEN 1 ELSE 0 END) as easyCount, " +
           "SUM(CASE WHEN p.difficulty = 'MEDIUM' THEN 1 ELSE 0 END) as mediumCount, " +
           "SUM(CASE WHEN p.difficulty = 'HARD' THEN 1 ELSE 0 END) as hardCount, " +
           "SUM(CASE WHEN p.status = 'SOLVED' AND p.dateSolved = :today THEN 1 ELSE 0 END) as solvedToday, " +
           "SUM(CASE WHEN p.status = 'SOLVED' AND p.dateSolved >= :startOfWeek THEN 1 ELSE 0 END) as solvedThisWeek, " +
           "SUM(CASE WHEN p.status = 'SOLVED' AND p.dateSolved >= :startOfMonth THEN 1 ELSE 0 END) as solvedThisMonth " +
           "FROM DsaProblem p WHERE p.user.id = :userId")
    DsaAnalyticsProjection getDsaAnalyticsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId, 
                                                   @org.springframework.data.repository.query.Param("today") java.time.LocalDate today, 
                                                   @org.springframework.data.repository.query.Param("startOfWeek") java.time.LocalDate startOfWeek, 
                                                   @org.springframework.data.repository.query.Param("startOfMonth") java.time.LocalDate startOfMonth);

    interface DsaAnalyticsProjection {
        Long getTotal();
        Long getTodoCount();
        Long getSolvedCount();
        Long getRevisionCount();
        Long getMasteredCount();
        Long getEasyCount();
        Long getMediumCount();
        Long getHardCount();
        Long getSolvedToday();
        Long getSolvedThisWeek();
        Long getSolvedThisMonth();
    }

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "COUNT(p) as total, " +
           "SUM(CASE WHEN p.status = 'SOLVED' THEN 1 ELSE 0 END) as solved " +
           "FROM DsaProblem p WHERE p.user.id = :userId")
    DsaOverviewProjection getDsaOverviewByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    interface DsaOverviewProjection {
        Long getTotal();
        Long getSolved();
    }
}
