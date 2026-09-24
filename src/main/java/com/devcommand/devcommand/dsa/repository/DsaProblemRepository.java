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
}
