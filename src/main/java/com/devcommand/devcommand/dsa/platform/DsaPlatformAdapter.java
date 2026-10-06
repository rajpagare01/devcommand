package com.devcommand.devcommand.dsa.platform;

public interface DsaPlatformAdapter {
    DsaPlatform getPlatform();
    PlatformProfile fetchProfile(String username);
}
