package ru.wobot.sm.core.fetch;

import java.util.Map;

public final class Redirect extends AbstractResponse {
    private final String location;

    public Redirect(String location, Map<String, Object> metadata) {
        super(metadata);
        this.location = location;
    }

    @Override
    public String getData() {
        return "";
    }

    @Override
    public Object getMessage() {
        return location;
    }
}
