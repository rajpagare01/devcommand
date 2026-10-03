package com.devcommand.devcommand.ratelimit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * DB-backed sliding-window rate limiter using a fixed-window algorithm.
 *
 * <p>Algorithm: fixed window (truncated to the window duration) per bucket key.
 * Each request atomically increments a counter via a PostgreSQL
 * {@code INSERT ... ON CONFLICT DO UPDATE RETURNING} executed through {@link JdbcTemplate}
 * (using {@code JdbcTemplate#queryForObject} which supports RETURNING statements,
 * unlike {@code @Modifying} Spring Data queries which use {@code executeUpdate()}).
 *
 * <p>Key format: {@code "<endpoint_label>:<ip>"}, e.g. {@code "auth:login:192.168.1.1"}.
 *
 * <p>Trade-off: fixed-window allows up to 2× the limit at window boundaries.
 * Acceptable for human-facing endpoints at the current scale.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final String UPSERT_SQL =
            "INSERT INTO rate_limit_buckets (bucket_key, window_start, request_count, created_at) " +
            "VALUES (?, ?, 1, NOW()) " +
            "ON CONFLICT (bucket_key, window_start) " +
            "DO UPDATE SET request_count = rate_limit_buckets.request_count + 1 " +
            "RETURNING request_count";

    private static final String DELETE_STALE_SQL =
            "DELETE FROM rate_limit_buckets WHERE window_start < ?";

    private final JdbcTemplate jdbcTemplate;

    public RateLimitService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Attempts to consume one request unit from the bucket.
     *
     * @param bucketKey      identifies the resource + identity (e.g. "auth:login:1.2.3.4")
     * @param limit          max allowed requests per window
     * @param windowSeconds  window duration in seconds (e.g. 60 for per-minute)
     * @return {@code true} if the request is allowed, {@code false} if the limit is exceeded
     */
    public boolean isAllowed(String bucketKey, int limit, int windowSeconds) {
        LocalDateTime windowStart = truncateToWindow(windowSeconds);
        try {
            Integer count = jdbcTemplate.queryForObject(UPSERT_SQL, Integer.class, bucketKey, windowStart);
            if (count == null) {
                log.warn("Rate limit upsert returned null for key={}, failing open", bucketKey);
                return true;
            }
            boolean allowed = count <= limit;
            if (!allowed) {
                log.warn("Rate limit exceeded: key={} count={} limit={}", bucketKey, count, limit);
            }
            return allowed;
        } catch (Exception e) {
            // Fail-open: if the DB is temporarily unavailable, don't block legitimate users.
            log.error("Rate limit check failed for key={}, failing open", bucketKey, e);
            return true;
        }
    }

    /**
     * Truncates the current UTC timestamp to the start of the current fixed window.
     * E.g. with windowSeconds=60, all calls within the same minute share the same windowStart.
     */
    private LocalDateTime truncateToWindow(int windowSeconds) {
        long epochSecond = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
        long windowStartEpoch = (epochSecond / windowSeconds) * windowSeconds;
        return LocalDateTime.ofEpochSecond(windowStartEpoch, 0, ZoneOffset.UTC);
    }

    /**
     * Periodic cleanup of stale rate-limit buckets.
     * Runs every 10 minutes. Removes rows older than 2 hours.
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void cleanupStaleBuckets() {
        LocalDateTime cutoff = LocalDateTime.now().minus(2, ChronoUnit.HOURS);
        jdbcTemplate.update(DELETE_STALE_SQL, cutoff);
        log.debug("Rate limit stale bucket cleanup complete (cutoff={})", cutoff);
    }
}
