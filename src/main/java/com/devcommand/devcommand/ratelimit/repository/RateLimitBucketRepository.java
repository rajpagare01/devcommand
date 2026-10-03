package com.devcommand.devcommand.ratelimit.repository;

import com.devcommand.devcommand.ratelimit.entity.RateLimitBucket;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Simple JPA repository for {@link RateLimitBucket}.
 *
 * <p>The rate-limit upsert-and-count operation is performed via {@link org.springframework.jdbc.core.JdbcTemplate}
 * in {@link com.devcommand.devcommand.ratelimit.service.RateLimitService} (using
 * {@code INSERT ... ON CONFLICT DO UPDATE RETURNING}), because {@code @Modifying} Spring Data
 * queries use {@code executeUpdate()} which discards the {@code RETURNING} result set.
 *
 * <p>This repository is used for test-level cleanup ({@code deleteAll()}) and potential
 * future read-side queries (e.g. admin views).
 */
public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, Long> {
}
