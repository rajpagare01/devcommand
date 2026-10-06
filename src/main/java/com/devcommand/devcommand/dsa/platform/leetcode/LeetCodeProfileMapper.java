package com.devcommand.devcommand.dsa.platform.leetcode;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class LeetCodeProfileMapper {

    public LeetCodeProfile map(LeetCodeProfileResponse response) {
        if (response == null || response.getData() == null || response.getData().getMatchedUser() == null) {
            return null;
        }

        LeetCodeProfileResponse.MatchedUser user = response.getData().getMatchedUser();
        
        LeetCodeProfile profile = LeetCodeProfile.builder()
                .username(user.getUsername())
                .profileUrl("https://leetcode.com/u/" + user.getUsername() + "/")
                .build();
                
        if (user.getProfile() != null) {
            profile.setRanking(user.getProfile().getRanking());
        }
        
        if (user.getSubmitStats() != null && user.getSubmitStats().getAcSubmissionNum() != null) {
            List<LeetCodeProfileResponse.SubmissionNode> stats = user.getSubmitStats().getAcSubmissionNum();
            for (LeetCodeProfileResponse.SubmissionNode stat : stats) {
                if (stat.getDifficulty() == null) continue;
                
                switch (stat.getDifficulty()) {
                    case "All" -> profile.setTotalSolved(stat.getCount());
                    case "Easy" -> profile.setEasySolved(stat.getCount());
                    case "Medium" -> profile.setMediumSolved(stat.getCount());
                    case "Hard" -> profile.setHardSolved(stat.getCount());
                }
            }
        }
        
        return profile;
    }
}
