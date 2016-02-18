package ru.wobot.sm.core.fetch;

import java.util.Map;

public final class AccessDenied extends AbstractResponse {
    private final Object message;

    public AccessDenied(Object message, Map<String, Object> metadata) {
        super(metadata);
        this.message = message;
    }

    @Override
    public String getData() {
        return "";
    }

    public Object getMessage() {
        return message;
    }
}
