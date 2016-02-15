package ru.wobot.sm.core.fetch;

import java.util.Map;

//todo: rename to something like OkResponce or SuccessResponse
public class FetchResponse implements Response {
    private final String data;
    //todo: Maybe should be replaced "metadata" type with something like Guava's Multimap.
    private final Map<String, Object> metadata;

    public FetchResponse(final String data, final Map<String, Object> metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getData() {
        return data;
    }
}
