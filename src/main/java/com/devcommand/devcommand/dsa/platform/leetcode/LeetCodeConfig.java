package com.devcommand.devcommand.dsa.platform.leetcode;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "leetcode")
public class LeetCodeConfig {
    private boolean enabled = false;
    private String baseUrl = "https://leetcode.com";
    private int timeoutSeconds = 10;
}
