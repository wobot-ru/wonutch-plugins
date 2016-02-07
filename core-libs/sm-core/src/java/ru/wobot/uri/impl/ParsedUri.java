package ru.wobot.uri.impl;

import com.google.common.collect.Lists;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class ParsedUri {
    private Collection<String> segments;
    private String scheme;

    protected ParsedUri(String scheme, Collection<String> segments) {
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
            for (String seg : uri.getPath().substring(1).split("/")) {
                segments.add(seg);
            }
        }
        return new ParsedUri(uri.getScheme(), segments);
    }

    public String getScheme() {
        return scheme;
    }
}
