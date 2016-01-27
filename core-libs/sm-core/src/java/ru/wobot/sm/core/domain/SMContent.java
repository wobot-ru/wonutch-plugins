package ru.wobot.sm.core.domain;

import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SMContent {
    public static final String JSON_MIME_TYPE = "application/json";
    public final String url;
    public final byte[] data;
    public final Map<String, String> metadata;

    public SMContent(String url, byte[] data) {
        this(url, data, new HashMap<String, String>());
    }

    public SMContent(String url, byte[] data, Map<String, String> metadata) {
        this.url = url;
        this.data = data;
        this.metadata = Objects.requireNonNull(metadata);
        // for convenience, may be remove later
        this.metadata.put(ContentMetaConstants.MIME_TYPE, JSON_MIME_TYPE);
        this.metadata.putIfAbsent(ContentMetaConstants.FETCH_TIME, String.valueOf(System.currentTimeMillis
                ()));
    }
}
