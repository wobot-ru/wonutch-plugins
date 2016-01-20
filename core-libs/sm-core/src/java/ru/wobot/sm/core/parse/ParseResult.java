package ru.wobot.sm.core.parse;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ParseResult {
    public final String url;
    public final String title;
    public final String content;
    public final Map<String, String> links;
    public final Map<String, String> parseMeta;
    public final Map<String, String> contentMeta;

    public ParseResult(String url, String title, String content, Map<String, String> links, Map<String, String> parseMeta, Map<String, String> contentMeta) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.parseMeta = Objects.requireNonNull(parseMeta, "parseMeta argument must not be null.");
        this.contentMeta = Objects.requireNonNull(contentMeta, "contentMeta argument must not be null.");
        this.links = Objects.requireNonNull(links, "links argument must not be null.");
    }
}
