package com.devcommand.devcommand.dsa.platform.leetcode;

import com.devcommand.devcommand.dsa.platform.DsaPlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class LeetCodeClient {

    private final RestTemplate restTemplate;
    private final LeetCodeConfig config;

    public LeetCodeClient(RestTemplateBuilder restTemplateBuilder, LeetCodeConfig config) {
        this.restTemplate = restTemplateBuilder.build();
        this.config = config;
    }

    public LeetCodeProfileResponse fetchProfile(String username) {
        if (!config.isEnabled()) {
            throw new DsaPlatformException("LeetCode integration is disabled");
        }

        String url = config.getBaseUrl() + "/graphql";
        
        String query = """
            query userPublicProfile($username: String!) {
              matchedUser(username: $username) {
                username
                profile {
                  ranking
                }
                submitStats {
                  acSubmissionNum {
                    difficulty
                    count
                  }
                }
              }
            }
            """;

        Map<String, Object> requestBody = Map.of(
            "query", query,
            "variables", Map.of("username", username)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Fetching LeetCode profile for username: {}", username);
            ResponseEntity<LeetCodeProfileResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    LeetCodeProfileResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.warn("Failed to fetch LeetCode profile for {}: HTTP {}", username, response.getStatusCode());
                throw new DsaPlatformException("Failed to fetch LeetCode profile: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.warn("Error communicating with LeetCode API for username {}: {}", username, e.getMessage());
            throw new DsaPlatformException("Error communicating with LeetCode API", e);
        }
    }
}
