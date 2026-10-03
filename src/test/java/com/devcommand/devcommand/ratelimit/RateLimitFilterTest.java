package com.devcommand.devcommand.ratelimit;

import com.devcommand.devcommand.metrics.DevCommandMetrics;
import com.devcommand.devcommand.ratelimit.filter.RateLimitFilter;
import com.devcommand.devcommand.ratelimit.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitFilter}.
 * Uses Mockito to stub {@link RateLimitService} — no DB required.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    private RateLimitFilter filter;
    private DevCommandMetrics metrics;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        metrics = new DevCommandMetrics(new SimpleMeterRegistry());
        filter = new RateLimitFilter(rateLimitService, new ObjectMapper(), metrics);
        chain = new MockFilterChain();
    }

    // ------------------------------------------------------------------ Below limit

    @Test
    void belowLimit_requestIsAllowed_chainContinues() throws Exception {
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);

        MockHttpServletRequest req = postRequest("/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isNotEqualTo(429);
        assertThat(chain.getRequest()).isNotNull(); // chain was invoked
    }

    // ------------------------------------------------------------------ At limit (boundary)

    @Test
    void atLimit_lastAllowedRequest_chainContinues() throws Exception {
        // isAllowed returns true for the "at limit" request (count == limit is still allowed)
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);

        MockHttpServletRequest req = postRequest("/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    // ------------------------------------------------------------------ Above limit

    @Test
    void aboveLimit_returns429WithSafeBody() throws Exception {
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false);

        MockHttpServletRequest req = postRequest("/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentType()).contains("application/json");

        String body = res.getContentAsString();
        assertThat(body).contains("Too Many Requests");
        assertThat(body).contains("429");
        // Must NOT expose internal details
        assertThat(body).doesNotContain("RateLimitFilter");
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("stack");
        assertThat(body).doesNotContain("bucket");
    }

    @Test
    void aboveLimit_chainIsNotInvoked() throws Exception {
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false);

        MockHttpServletRequest req = postRequest("/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain spy = new MockFilterChain();

        filter.doFilter(req, res, spy);

        // Chain should NOT have been called
        assertThat(spy.getRequest()).isNull();
    }

    // ------------------------------------------------------------------ Correct limits per endpoint

    @Test
    void loginEndpoint_usesCorrectLimit() throws Exception {
        when(rateLimitService.isAllowed(anyString(), eq(10), eq(60))).thenReturn(true);

        MockHttpServletRequest req = postRequest("/api/auth/login");
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        verify(rateLimitService).isAllowed(contains("auth:login"), eq(10), eq(60));
    }

    @Test
    void registerEndpoint_usesCorrectLimit() throws Exception {
        when(rateLimitService.isAllowed(anyString(), eq(5), eq(60))).thenReturn(true);

        MockHttpServletRequest req = postRequest("/api/auth/register");
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimitService).isAllowed(contains("auth:register"), eq(5), eq(60));
    }

    @Test
    void telegramBootstrapEndpoint_usesCorrectLimit() throws Exception {
        when(rateLimitService.isAllowed(anyString(), eq(5), eq(60))).thenReturn(true);

        MockHttpServletRequest req = postRequest("/api/integrations/telegram/bootstrap");
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimitService).isAllowed(contains("telegram:bootstrap"), eq(5), eq(60));
    }

    @Test
    void whatsappWebhookEndpoint_usesCorrectLimit() throws Exception {
        when(rateLimitService.isAllowed(anyString(), eq(30), eq(60))).thenReturn(true);

        MockHttpServletRequest req = postRequest("/api/integrations/whatsapp/webhook");
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimitService).isAllowed(contains("whatsapp:webhook"), eq(30), eq(60));
    }

    // ------------------------------------------------------------------ Independent users/IPs

    @Test
    void differentIps_haveIndependentBuckets() throws Exception {
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false, true);

        MockHttpServletRequest req1 = postRequest("/api/auth/login");
        req1.setRemoteAddr("1.1.1.1");
        MockHttpServletRequest req2 = postRequest("/api/auth/login");
        req2.setRemoteAddr("2.2.2.2");

        MockHttpServletResponse res1 = new MockHttpServletResponse();
        MockHttpServletResponse res2 = new MockHttpServletResponse();

        filter.doFilter(req1, res1, new MockFilterChain());
        filter.doFilter(req2, res2, new MockFilterChain());

        // First IP is blocked, second is allowed
        assertThat(res1.getStatus()).isEqualTo(429);
        assertThat(res2.getStatus()).isNotEqualTo(429);
    }

    // ------------------------------------------------------------------ Non-rate-limited endpoints

    @Test
    void getRequest_isNeverRateLimited() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(rateLimitService);
    }

    @Test
    void unrelatedPostEndpoint_isNotRateLimited() throws Exception {
        MockHttpServletRequest req = postRequest("/api/tasks");
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(rateLimitService);
    }

    // ------------------------------------------------------------------ Bucket key uses IP

    @Test
    void bucketKey_containsClientIp() throws Exception {
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);

        MockHttpServletRequest req = postRequest("/api/auth/login");
        req.setRemoteAddr("192.168.1.100");
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        verify(rateLimitService).isAllowed(contains("192.168.1.100"), anyInt(), anyInt());
    }

    @Test
    void bucketKey_neverContainsSensitiveData() throws Exception {
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);

        MockHttpServletRequest req = postRequest("/api/auth/login");
        req.addHeader("Authorization", "Bearer eyJhbGc.secret.token");
        req.setParameter("password", "mysecretpassword");
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        // Capture what bucket key was used
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(rateLimitService).isAllowed(captor.capture(), anyInt(), anyInt());

        String usedKey = captor.getValue();
        assertThat(usedKey).doesNotContain("eyJhbGc");
        assertThat(usedKey).doesNotContain("secret");
        assertThat(usedKey).doesNotContain("password");
    }

    // ------------------------------------------------------------------ Helpers

    private MockHttpServletRequest postRequest(String path) {
        return new MockHttpServletRequest("POST", path);
    }
}
