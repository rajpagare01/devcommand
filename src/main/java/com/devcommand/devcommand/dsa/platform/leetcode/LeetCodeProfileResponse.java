package com.devcommand.devcommand.dsa.platform.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeetCodeProfileResponse {

    private DataNode data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataNode {
        private MatchedUser matchedUser;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchedUser {
        private String username;
        private Profile profile;
        private SubmitStats submitStats;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        private Integer ranking;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmitStats {
        private List<SubmissionNode> acSubmissionNum;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmissionNode {
        private String difficulty;
        private Integer count;
    }
}
