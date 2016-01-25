package ru.wobot.sm.core.parse;

import java.util.Map;
import java.util.Objects;

public class ParseResult {
    private final String url;
    private final String title;
    private final String content;
    private final Map<String, String> links;
    private final Map<String, Object> parseMeta;
    private final Map<String, Object> contentMeta;

    public ParseResult(String url, Map<String, String> links, Map<String, Object> parseMeta, Map<String, Object> contentMeta) {
        this(url, "", "", links, parseMeta, contentMeta);
    }

    public ParseResult(String url, String title, String content, Map<String, String> links, Map<String, Object> parseMeta, Map<String, Object> contentMeta) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.parseMeta = Objects.requireNonNull(parseMeta, "parseMeta argument must not be null.");
        this.contentMeta = Objects.requireNonNull(contentMeta, "contentMeta argument must not be null.");
        this.links = Objects.requireNonNull(links, "links argument must not be null.");
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public Map<String, Object> getParseMeta() {
        return parseMeta;
    }

    public Map<String, Object> getContentMeta() {
        return contentMeta;
    }
}
