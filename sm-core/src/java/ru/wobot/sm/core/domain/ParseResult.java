package ru.wobot.sm.core.domain;

import java.util.HashMap;
import java.util.Map;

public class ParseResult {
    public final String url;
    public final String title;
    public final String content;
    public final Map<String, String> links;
    public boolean isMultiPage;

    public ParseResult(String url, String title, String content, Map<String, String> links) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.links = links;
        this.isMultiPage = false;
    }

    public ParseResult(String url, String title, String content, boolean isMultiPage) {
        this(url, title, content, new HashMap<String, String>());
        this.isMultiPage = isMultiPage;
    }
}
