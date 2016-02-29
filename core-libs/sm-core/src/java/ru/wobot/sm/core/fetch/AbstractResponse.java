package ru.wobot.sm.core.fetch;

import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.util.Map;
import java.util.Objects;

public abstract class AbstractResponse implements FetchResponse {
    private static final String JSON_MIME_TYPE = "application/json";
    private final Map<String, Object> metadata;

    protected AbstractResponse(Map<String, Object> metadata) {
        this.metadata = Objects.requireNonNull(metadata);
        if (!this.metadata.containsKey(ContentMetaConstants.MIME_TYPE))
            this.metadata.put(ContentMetaConstants.MIME_TYPE, JSON_MIME_TYPE);

        if (!this.metadata.containsKey(ContentMetaConstants.FETCH_TIME))
            this.metadata.put(ContentMetaConstants.FETCH_TIME, System.currentTimeMillis());
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
