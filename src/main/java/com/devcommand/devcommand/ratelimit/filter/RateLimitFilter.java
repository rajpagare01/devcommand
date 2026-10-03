package com.devcommand.devcommand.ratelimit.filter;

import com.devcommand.devcommand.metrics.DevCommandMetrics;
import com.devcommand.devcommand.ratelimit.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet filter that rate-limits high-risk, externally-accessible endpoints.
 *
 * <p>Only unauthenticated / public / integration endpoints are rate-limited here.
 * Authenticated REST endpoints are protected by JWT authentication and are NOT
 * rate-limited indiscriminately — adding per-user rate limits would belong in a
 * separate, authenticated-context filter if ever required.
 *
 * <p>Rate-limited endpoints and their limits:
 * <ul>
 *   <li>{@code POST /api/auth/login}           — 10 req/min per IP (brute-force protection)</li>
 *   <li>{@code POST /api/auth/register}         — 5  req/min per IP (signup spam protection)</li>
 *   <li>{@code POST /api/integrations/telegram/bootstrap} — 5 req/min per IP</li>
 *   <li>{@code POST /api/integrations/identities/{id}/verify} — 5 req/min per IP</li>
 *   <li>{@code POST /api/integrations/whatsapp/webhook} — 30 req/min per IP</li>
 * </ul>
 *
 * <p>The bucket key is always {@code "<endpoint_label>:<client_ip>"} — never includes
 * user IDs, emails, JWTs, or other sensitive identifiers.
 *
 * <p>On rejection, returns HTTP 429 with a safe JSON body (no internal implementation
 * details exposed).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int WINDOW_SECONDS = 60;

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final DevCommandMetrics metrics;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper, DevCommandMetrics metrics) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = resolveClientIp(request);

        RateLimitRule rule = resolveRule(method, path);

        if (rule != null) {
            String bucketKey = rule.label() + ":" + clientIp;
            boolean allowed = rateLimitService.isAllowed(bucketKey, rule.limit(), WINDOW_SECONDS);
            if (!allowed) {
                metrics.recordRateLimitRejection(rule.label());
                rejectWithTooManyRequests(response, request.getRequestURI());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns the rate-limit rule for the given method + path, or null if not rate-limited.
     * Path matching is exact or prefix-based; more specific rules must come first.
     */
    private RateLimitRule resolveRule(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) {
            return null; // Only POST endpoints are rate-limited
        }

        if (path.equals("/api/auth/login")) {
            return new RateLimitRule("auth:login", 10);
        }
        if (path.equals("/api/auth/register")) {
            return new RateLimitRule("auth:register", 5);
        }
        if (path.equals("/api/integrations/telegram/bootstrap")) {
            return new RateLimitRule("telegram:bootstrap", 5);
        }
        if (path.matches("/api/integrations/identities/\\d+/verify")) {
            return new RateLimitRule("identity:verify", 5);
        }
        if (path.equals("/api/integrations/whatsapp/webhook")) {
            return new RateLimitRule("whatsapp:webhook", 30);
        }

        return null;
    }

    /**
     * Resolves the client IP address.
     * Honours X-Forwarded-For only if it is set — in production behind a trusted proxy this
     * gives the real client IP. In direct-access scenarios, REMOTE_ADDR is used.
     *
     * Note: X-Forwarded-For can be spoofed by clients if the proxy does not strip it.
     * Deploy behind a trusted reverse proxy (nginx, AWS ALB) that overwrites this header.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Take only the first (leftmost) address — the original client IP
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void rejectWithTooManyRequests(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Too many requests. Please try again later.");
        body.put("path", path);

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /** Immutable value object describing a rate-limit rule. */
    private record RateLimitRule(String label, int limit) {}
}
