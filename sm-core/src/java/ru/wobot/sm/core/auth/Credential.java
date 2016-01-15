package ru.wobot.sm.core.auth;

public interface Credential {
    String getAccessToken();

    String getClientSecret();
}
