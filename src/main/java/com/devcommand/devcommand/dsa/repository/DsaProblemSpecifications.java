package com.devcommand.devcommand.dsa.repository;

import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Small, explicit Specification builders instead of a generic dynamic-query
 * framework - each filter is one readable method, and DsaProblemService
 * combines only the ones actually present on the request with
 * Specification.allOf(...).
 *
 * The user filter is not optional - every specification list built by the
 * service always includes ownerIs(userId) first, so filtering can never
 * accidentally return another user's data.
 */
public final class DsaProblemSpecifications {

    private DsaProblemSpecifications() {
    }

    public static Specification<DsaProblem> ownerIs(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<DsaProblem> topicEquals(String topic) {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("topic")), topic.toUpperCase());
    }

    public static Specification<DsaProblem> platformEquals(String platform) {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("platform")), platform.toUpperCase());
    }

    public static Specification<DsaProblem> difficultyEquals(Difficulty difficulty) {
        return (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<DsaProblem> statusEquals(ProblemStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
