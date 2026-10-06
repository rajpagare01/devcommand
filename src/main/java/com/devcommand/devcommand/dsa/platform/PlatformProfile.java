package com.devcommand.devcommand.dsa.platform;

public interface PlatformProfile {
    String getUsername();
    String getProfileUrl();
    Integer getTotalSolved();
    Integer getEasySolved();
    Integer getMediumSolved();
    Integer getHardSolved();
    Integer getRanking();
}
