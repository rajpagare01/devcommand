package com.devcommand.devcommand.dsa.platform.leetcode;

import com.devcommand.devcommand.dsa.platform.DsaPlatform;
import com.devcommand.devcommand.dsa.platform.PlatformProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeetCodeAdapterTest {

    @Mock
    private LeetCodeClient client;

    @Mock
    private LeetCodeProfileMapper mapper;

    @InjectMocks
    private LeetCodeAdapter adapter;

    @Test
    void getPlatform_ReturnsLeetCode() {
        assertEquals(DsaPlatform.LEETCODE, adapter.getPlatform());
    }

    @Test
    void fetchProfile_CallsClientAndMapper() {
        String username = "testuser";
        LeetCodeProfileResponse response = new LeetCodeProfileResponse();
        LeetCodeProfile profile = LeetCodeProfile.builder().username(username).build();

        when(client.fetchProfile(username)).thenReturn(response);
        when(mapper.map(response)).thenReturn(profile);

        PlatformProfile result = adapter.fetchProfile(username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(client).fetchProfile(username);
        verify(mapper).map(response);
    }
}
