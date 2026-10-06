package com.devcommand.devcommand.dsa.platform.leetcode;

import com.devcommand.devcommand.dsa.platform.PlatformProfile;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LeetCodeProfile implements PlatformProfile {
    private String username;
    private String profileUrl;
    private Integer totalSolved;
    private Integer easySolved;
    private Integer mediumSolved;
    private Integer hardSolved;
    private Integer ranking;
}
