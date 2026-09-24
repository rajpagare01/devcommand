package com.devcommand.devcommand.learning.repository;

import com.devcommand.devcommand.learning.entity.LearningTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningTopicRepository extends JpaRepository<LearningTopic, Long> {
}
