package ru.wobot.sm.core.fetch;

import java.util.Map;

/**
 * Response returned from fetchers, that fetch social media
 */
public interface FetchResponse {
    /**
     * Raw data
     * @return String, containing fetched data
     */
    String getData();

    /**
     * Fetch metadata (API version, etc)
     * @return Map, containing metadata objects as values
     * @see ru.wobot.sm.core.meta.ContentMetaConstants
     */
    Map<String, Object> getMetadata();

    /**
     * Some additional data
     * @return Object, represents add data
     */
    Object getMessage();
}
