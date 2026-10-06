package com.devcommand.devcommand.dsa.platform.leetcode;

import com.devcommand.devcommand.dsa.platform.DsaPlatform;
import com.devcommand.devcommand.dsa.platform.DsaPlatformAdapter;
import com.devcommand.devcommand.dsa.platform.PlatformProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeetCodeAdapter implements DsaPlatformAdapter {

    private final LeetCodeClient client;
    private final LeetCodeProfileMapper mapper;

    @Override
    public DsaPlatform getPlatform() {
        return DsaPlatform.LEETCODE;
    }

    @Override
    public PlatformProfile fetchProfile(String username) {
        LeetCodeProfileResponse response = client.fetchProfile(username);
        return mapper.map(response);
    }
}
