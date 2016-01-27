package ru.wobot.sm.core.domain;

import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SMContent {
    private static final String JSON_MIME_TYPE = "application/json";
    private final String url;
    private final byte[] data;
    private final Map<String, Object> metadata;

    public SMContent(String url, byte[] data) {
        this(url, data, new HashMap<String, Object>());
    }

    public SMContent(String url, byte[] data, Map<String, Object> metadata) {
        this.url = url;
        this.data = data;
        this.metadata = Objects.requireNonNull(metadata);
        // for convenience, may be remove later
        this.metadata.put(ContentMetaConstants.MIME_TYPE, JSON_MIME_TYPE);
        if (!this.metadata.containsKey(ContentMetaConstants.FETCH_TIME))
            this.metadata.put(ContentMetaConstants.FETCH_TIME, System.currentTimeMillis());
    }

    public byte[] getData() {
        return data;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
