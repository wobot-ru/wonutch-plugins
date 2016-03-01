package ru.wobot.uri.impl;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ParsedUri {
    private final Collection<String> segments;
    private final String scheme;
    private final Map<String, String> query;

    private ParsedUri(String scheme, Collection<String> segments, Map<String, String> query) {
        this.segments = segments;
        this.scheme = scheme;
        this.query = query;
    }

    public static ParsedUri parse(final String uri) {
        return parse(URI.create(uri));
    }

    public static ParsedUri parse(final URI uri) {
        final Collection<String> segments = new ArrayList<String>() {{
            add(uri.getHost());
        }};
        if (uri.getPath() != null && uri.getPath().contains("/")) {
            Collections.addAll(segments, uri.getPath().substring(1).split("/"));
        }
        return new ParsedUri(uri.getScheme(), segments, splitQuery(uri));
    }

    private static Map<String, String> splitQuery(final URI uri) {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        if (uri.getQuery() == null)
            return queryPairs;

        try {
            for (String pair : uri.getQuery().split("&")) {
                int idx = pair.indexOf("=");
                if (idx < 0)
                    queryPairs.put(URLDecoder.decode(pair, "UTF-8"), "");
                else
                    queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
        } catch (UnsupportedEncodingException shouldNotHappens) {
            throw new RuntimeException("Broken VM doesn't support UTF-8.");
        }
        return queryPairs;
    }

    public Collection<String> getSegments() {
        return segments;
    }

    public String getScheme() {
        return scheme;
    }

    public Map<String, String> getQuery() {
        return query;
    }
}
