package com.devcommand.devcommand.ratelimit.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Sliding-window rate-limit bucket.
 *
 * Each row represents a count of requests for a given (bucket_key, window_start) pair.
 * The unique constraint on (bucket_key, window_start) enables atomic
 * "INSERT ... ON CONFLICT DO UPDATE" increments without explicit row-level locks.
 */
@Entity
@Table(name = "rate_limit_buckets",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_rate_limit_bucket",
               columnNames = {"bucket_key", "window_start"}
       ))
public class RateLimitBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bucket_key", nullable = false, length = 512)
    private String bucketKey;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected RateLimitBucket() {}

    public RateLimitBucket(String bucketKey, LocalDateTime windowStart) {
        this.bucketKey = bucketKey;
        this.windowStart = windowStart;
        this.requestCount = 1;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getBucketKey() { return bucketKey; }
    public LocalDateTime getWindowStart() { return windowStart; }
    public Integer getRequestCount() { return requestCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
