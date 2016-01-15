package ru.wobot.sm.core.dto;


public class SMProfile {
    private final String userId;
    private final String domain;
    private final String fullName;

    public SMProfile(String userId, String domain, String fullName) {
        this.userId = userId;
        this.domain = domain;
        this.fullName = fullName;
    }

    public String getId() {
        return userId;
    }

    public String getDomain() {
        return domain;
    }

    public String getFullName() {
        return fullName;
    }
}
