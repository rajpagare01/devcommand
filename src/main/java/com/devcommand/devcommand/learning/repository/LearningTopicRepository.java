package com.devcommand.devcommand.learning.repository;

import com.devcommand.devcommand.learning.entity.LearningTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * JpaSpecificationExecutor backs the combinable, optional filters + search
 * on GET /api/learning, same pattern as the DSA/Tasks/Jobs repositories.
 *
 * findByIdAndUserId is the ownership-safe alternative to findById(id),
 * used for every single-record operation (get one/update/delete/progress/
 * complete).
 */
public interface LearningTopicRepository extends JpaRepository<LearningTopic, Long>, JpaSpecificationExecutor<LearningTopic> {

    Optional<LearningTopic> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, com.devcommand.devcommand.learning.entity.LearningStatus status);

    /**
     * Case-insensitive lookup by topic name or technology name, always scoped to the owner.
     * Replaces the former in-memory scan (findAll + equalsIgnoreCase iteration).
     * Both predicates are guarded by user.id = :userId, so cross-user results are impossible.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT l FROM LearningTopic l WHERE l.user.id = :userId " +
            "AND (LOWER(l.topic) = LOWER(:name) OR LOWER(l.technology) = LOWER(:name))"
    )
    java.util.List<LearningTopic> findByUserIdAndTopicOrTechnologyIgnoreCase(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("name") String name
    );

    public interface LearningAnalyticsProjection {
        Long getTotal();
        Long getNotStarted();
        Long getInProgress();
        Long getCompleted();
        Long getOnHold();
        Double getAverageProgress();
        Double getTotalHoursSpent();
    }

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "COUNT(l) as total, " +
            "SUM(CASE WHEN l.status = 'NOT_STARTED' THEN 1 ELSE 0 END) as notStarted, " +
            "SUM(CASE WHEN l.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress, " +
            "SUM(CASE WHEN l.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
            "SUM(CASE WHEN l.status = 'ON_HOLD' THEN 1 ELSE 0 END) as onHold, " +
            "AVG(l.progress) as averageProgress, " +
            "SUM(l.hoursSpent) as totalHoursSpent " +
            "FROM LearningTopic l WHERE l.user.id = :userId")
    LearningAnalyticsProjection getLearningAnalyticsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT l.technology as technology, COUNT(l) as topicCount, " +
            "SUM(CASE WHEN l.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount, " +
            "AVG(l.progress) as averageProgress " +
            "FROM LearningTopic l WHERE l.user.id = :userId GROUP BY l.technology")
    java.util.List<TechnologyStats> getTechnologyBreakdown(@org.springframework.data.repository.query.Param("userId") Long userId);
    
    interface TechnologyStats {
        String getTechnology();
        Long getTopicCount();
        Long getCompletedCount();
        Double getAverageProgress();
    }

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "COUNT(l) as total, " +
            "SUM(CASE WHEN l.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed " +
            "FROM LearningTopic l WHERE l.user.id = :userId")
    LearningOverviewProjection getLearningOverviewByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    interface LearningOverviewProjection {
        Long getTotal();
        Long getCompleted();
    }
}
