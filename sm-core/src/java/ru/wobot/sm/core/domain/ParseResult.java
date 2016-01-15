package ru.wobot.sm.core.domain;

import java.util.HashMap;
import java.util.Map;

public class ParseResult {
    public String url;
    public String title;
    public String content;
    public Map<String, String> links = new HashMap<>();
    public boolean isMultiPage;

    public ParseResult(String url, String title, String content, Map<String, String> links) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.links = links;
    }

    public ParseResult(String url, String title, String content, boolean isMultiPage) {
        this(url, title, content, new HashMap<String, String>());
        this.isMultiPage = isMultiPage;
    }
}
