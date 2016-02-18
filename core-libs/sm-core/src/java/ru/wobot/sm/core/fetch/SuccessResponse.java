package ru.wobot.sm.core.fetch;

import java.util.Map;

public final class SuccessResponse extends AbstractResponse {
    private final String data;

    public SuccessResponse(final String data, final Map<String, Object> metadata) {
        super(metadata);
        this.data = data;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public Object getMessage() {
        return "API fetch finished successfully.";
    }
}
