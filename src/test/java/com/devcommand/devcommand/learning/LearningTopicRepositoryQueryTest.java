package com.devcommand.devcommand.learning;

import com.devcommand.devcommand.integration.AbstractIntegrationTest;
import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.ratelimit.repository.RateLimitBucketRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LearningTopicRepository#findByUserIdAndTopicOrTechnologyIgnoreCase}.
 *
 * Verifies that the new DB-scoped query correctly replaces the former in-memory iteration.
 * Runs against the real PostgreSQL test database.
 */
class LearningTopicRepositoryQueryTest extends AbstractIntegrationTest {

    @Autowired LearningTopicRepository learningTopicRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RateLimitBucketRepository rateLimitBucketRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        rateLimitBucketRepository.deleteAll();
        learningTopicRepository.deleteAll();
        userRepository.deleteAll();

        user1 = userRepository.save(buildUser("alice@example.com", "Alice"));
        user2 = userRepository.save(buildUser("bob@example.com", "Bob"));

        // User1 topics
        save(user1, "Spring Boot", "Spring Security", LearningStatus.IN_PROGRESS);
        save(user1, "Java", "Concurrency", LearningStatus.NOT_STARTED);
        save(user1, "React", "Hooks", LearningStatus.COMPLETED);

        // User2 topics (same names as user1 — must not bleed across users)
        save(user2, "Spring Boot", "Spring Data JPA", LearningStatus.IN_PROGRESS);
        save(user2, "Python", "AsyncIO", LearningStatus.NOT_STARTED);
    }

    // ------------------------------------------------------------------ Match by topic name

    @Test
    void findByTopic_exactCaseMatch() {
        List<LearningTopic> results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "Spring Security");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTopic()).isEqualTo("Spring Security");
        assertThat(results.get(0).getUser().getId()).isEqualTo(user1.getId());
    }

    // ------------------------------------------------------------------ Case-insensitive match by topic

    @Test
    void findByTopic_caseInsensitiveMatch() {
        List<LearningTopic> results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "spring security");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTopic()).isEqualTo("Spring Security");
    }

    @Test
    void findByTopic_uppercaseMatch() {
        List<LearningTopic> results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "CONCURRENCY");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTopic()).isEqualTo("Concurrency");
    }

    // ------------------------------------------------------------------ Match by technology name

    @Test
    void findByTechnology_exactMatch() {
        List<LearningTopic> results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "Java");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTechnology()).isEqualTo("Java");
    }

    @Test
    void findByTechnology_caseInsensitiveMatch() {
        List<LearningTopic> results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "react");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTechnology()).isEqualTo("React");
    }

    // ------------------------------------------------------------------ No match

    @Test
    void noMatch_returnsEmptyList() {
        List<LearningTopic> results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "Kubernetes");

        assertThat(results).isEmpty();
    }

    // ------------------------------------------------------------------ Cross-user isolation

    @Test
    void crossUser_user2TopicNotReturnedForUser1() {
        // "Spring Data JPA" belongs to user2 only
        List<LearningTopic> results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "Spring Data JPA");

        assertThat(results).isEmpty();
    }

    @Test
    void crossUser_sameTopicNameOnlyReturnsOwnersRecord() {
        // Both user1 and user2 have a "Spring Boot" technology topic
        List<LearningTopic> user1Results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "Spring Boot");
        List<LearningTopic> user2Results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user2.getId(), "Spring Boot");

        assertThat(user1Results).allMatch(t -> t.getUser().getId().equals(user1.getId()));
        assertThat(user2Results).allMatch(t -> t.getUser().getId().equals(user2.getId()));
    }

    @Test
    void crossUser_user2TopicNotVisibleToUser1_caseInsensitive() {
        List<LearningTopic> results = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(user1.getId(), "asyncio");

        assertThat(results).isEmpty();
    }

    // ------------------------------------------------------------------ Helpers

    private User buildUser(String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setPassword(passwordEncoder.encode("Password1!"));
        return u;
    }

    private LearningTopic save(User user, String technology, String topic, LearningStatus status) {
        LearningTopic lt = new LearningTopic();
        lt.setUser(user);
        lt.setTechnology(technology);
        lt.setTopic(topic);
        lt.setStatus(status);
        lt.setProgress(0);
        return learningTopicRepository.save(lt);
    }
}
