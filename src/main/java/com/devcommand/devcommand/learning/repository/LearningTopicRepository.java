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
}
