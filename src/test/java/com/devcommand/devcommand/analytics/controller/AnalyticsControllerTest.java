package com.devcommand.devcommand.analytics.controller;

import com.devcommand.devcommand.analytics.dto.*;
import com.devcommand.devcommand.analytics.service.AnalyticsService;
import com.devcommand.devcommand.security.JwtAuthenticationFilter;
import com.devcommand.devcommand.security.JwtService;
import com.devcommand.devcommand.security.UserPrincipal;
import com.devcommand.devcommand.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtService jwtService;

    private UserPrincipal userA;
    private UserPrincipal userB;

    @BeforeEach
    void setUp() {
        User entityA = new User();
        entityA.setId(100L);
        entityA.setEmail("userA@example.com");
        entityA.setPassword("password");
        userA = new UserPrincipal(entityA);

        User entityB = new User();
        entityB.setId(200L);
        entityB.setEmail("userB@example.com");
        entityB.setPassword("password");
        userB = new UserPrincipal(entityB);
        
        Mockito.when(analyticsService.getOverviewAnalytics(eq(100L)))
               .thenReturn(AnalyticsOverviewResponse.builder().build());
        Mockito.when(analyticsService.getOverviewAnalytics(eq(200L)))
               .thenReturn(AnalyticsOverviewResponse.builder().build());
    }

    @Test
    void getOverviewAnalytics_authenticatedUser_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities());
        mockMvc.perform(get("/api/analytics/overview").with(authentication(auth)))
                .andExpect(status().isOk());
        Mockito.verify(analyticsService).getOverviewAnalytics(100L);
    }

    @Test
    void getOverviewAnalytics_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getDsaAnalytics_authenticatedUser_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userB, null, userB.getAuthorities());
        mockMvc.perform(get("/api/analytics/dsa").with(authentication(auth)))
                .andExpect(status().isOk());
        Mockito.verify(analyticsService).getDsaAnalytics(200L);
    }
    
    @Test
    void getTaskAnalytics_authenticatedUser_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities());
        mockMvc.perform(get("/api/analytics/tasks").with(authentication(auth)))
                .andExpect(status().isOk());
        Mockito.verify(analyticsService).getTaskAnalytics(100L);
    }
    
    @Test
    void getJobAnalytics_authenticatedUser_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userB, null, userB.getAuthorities());
        mockMvc.perform(get("/api/analytics/jobs").with(authentication(auth)))
                .andExpect(status().isOk());
        Mockito.verify(analyticsService).getJobAnalytics(200L);
    }
    
    @Test
    void getLearningAnalytics_authenticatedUser_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities());
        mockMvc.perform(get("/api/analytics/learning").with(authentication(auth)))
                .andExpect(status().isOk());
        Mockito.verify(analyticsService).getLearningAnalytics(100L);
    }
    
    @Test
    void getProjectAnalytics_authenticatedUser_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities());
        mockMvc.perform(get("/api/analytics/projects").with(authentication(auth)))
                .andExpect(status().isOk());
        Mockito.verify(analyticsService).getProjectAnalytics(100L);
    }
}
