package ru.wobot.sm.fetch;

import ru.wobot.sm.core.fetch.Response;

public class AccessDenied implements Response {
    private String message;

    public AccessDenied(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
