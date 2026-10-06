package com.devcommand.devcommand.dsa.platform.leetcode;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LeetCodeProfileMapperTest {

    private final LeetCodeProfileMapper mapper = new LeetCodeProfileMapper();

    @Test
    void map_ValidResponse_ReturnsProfile() {
        LeetCodeProfileResponse.SubmissionNode all = new LeetCodeProfileResponse.SubmissionNode();
        all.setDifficulty("All");
        all.setCount(100);
        
        LeetCodeProfileResponse.SubmissionNode easy = new LeetCodeProfileResponse.SubmissionNode();
        easy.setDifficulty("Easy");
        easy.setCount(50);
        
        LeetCodeProfileResponse.SubmitStats stats = new LeetCodeProfileResponse.SubmitStats();
        stats.setAcSubmissionNum(List.of(all, easy));
        
        LeetCodeProfileResponse.Profile prof = new LeetCodeProfileResponse.Profile();
        prof.setRanking(12345);
        
        LeetCodeProfileResponse.MatchedUser matchedUser = new LeetCodeProfileResponse.MatchedUser();
        matchedUser.setUsername("testuser");
        matchedUser.setProfile(prof);
        matchedUser.setSubmitStats(stats);
        
        LeetCodeProfileResponse.DataNode data = new LeetCodeProfileResponse.DataNode();
        data.setMatchedUser(matchedUser);
        
        LeetCodeProfileResponse response = new LeetCodeProfileResponse();
        response.setData(data);

        LeetCodeProfile result = mapper.map(response);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("https://leetcode.com/u/testuser/", result.getProfileUrl());
        assertEquals(12345, result.getRanking());
        assertEquals(100, result.getTotalSolved());
        assertEquals(50, result.getEasySolved());
        assertNull(result.getMediumSolved());
        assertNull(result.getHardSolved());
    }

    @Test
    void map_NullResponse_ReturnsNull() {
        assertNull(mapper.map(null));
        assertNull(mapper.map(new LeetCodeProfileResponse()));
    }
}
