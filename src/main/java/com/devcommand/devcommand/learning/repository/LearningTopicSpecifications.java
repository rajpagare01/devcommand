package com.devcommand.devcommand.learning.repository;

import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import org.springframework.data.jpa.domain.Specification;

/**
 * Small, explicit Specification builders - matching the pattern already
 * established by DsaProblemSpecifications/DailyTaskSpecifications/
 * JobApplicationSpecifications. LearningTopicService always includes
 * ownerIs(userId) first in every combination it builds, so filtering/
 * search can never return another user's topics.
 *
 * technology filter is a case-insensitive EXACT match (mirrors the DSA
 * module's topic/platform filters and the Jobs module's company/role/
 * source filters); "search" is the separate, partial-match OR-across-
 * fields query the spec calls out distinctly.
 */
public final class LearningTopicSpecifications {

    private LearningTopicSpecifications() {
    }

    public static Specification<LearningTopic> ownerIs(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<LearningTopic> technologyEquals(String technology) {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("technology")), technology.toUpperCase());
    }

    public static Specification<LearningTopic> statusEquals(LearningStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<LearningTopic> progressEquals(Integer progress) {
        return (root, query, cb) -> cb.equal(root.get("progress"), progress);
    }

    /** technology OR topic contains the keyword, case-insensitive. Not a search engine on purpose. */
    public static Specification<LearningTopic> searchKeyword(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("technology")), pattern),
                cb.like(cb.lower(root.get("topic")), pattern)
        );
    }
}
