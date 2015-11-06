package ru.wobot.vk;

import java.util.HashMap;

public class ParseResult {
    public String url;
    public String title;
    public String content;
    public HashMap<String, String> links = new HashMap<>();
    public ParseResult(String url, String title, String content, HashMap<String, String> links) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.links = links;
    }

    public ParseResult(String url, String title, String content) {
        this(url, title, content, new HashMap<>());
    }
}
