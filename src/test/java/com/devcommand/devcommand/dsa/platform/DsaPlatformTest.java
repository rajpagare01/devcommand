package com.devcommand.devcommand.dsa.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DsaPlatformTest {
    @Test
    void testEnumValues() {
        assertEquals(3, DsaPlatform.values().length);
        assertNotNull(DsaPlatform.valueOf("LEETCODE"));
        assertNotNull(DsaPlatform.valueOf("GFG"));
        assertNotNull(DsaPlatform.valueOf("CODEFORCES"));
    }
}
