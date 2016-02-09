package ru.wobot.uri.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ParsedUri {
    private final Collection<String> segments;
    private final String scheme;

    private ParsedUri(String scheme, Collection<String> segments) {
        this.segments = segments;
        this.scheme = scheme;
    }

    public Collection<String> getSegments() {
        return segments;
    }

    public static ParsedUri parse(final URI uri) {
        final Collection<String> segments = new ArrayList<String>() {{
            add(uri.getHost());
        }};
        if (uri.getPath() != null && uri.getPath().contains("/")) {
            Collections.addAll(segments, uri.getPath().substring(1).split("/"));
        }
        return new ParsedUri(uri.getScheme(), segments);
    }

    public static ParsedUri parse(final String uri) {
        return parse(URI.create(uri));
    }

    public String getScheme() {
        return scheme;
    }
}
