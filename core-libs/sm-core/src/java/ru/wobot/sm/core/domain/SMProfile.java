package ru.wobot.sm.core.domain;

public final class SMProfile {
    /**
     * ID of profile in social media
     */
    private final String userId;

    /**
     * Profile account in social media, could be use in URI instead of ID
     */
    private final String domain;

    /**
     * Name of profile in social media
     */
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
