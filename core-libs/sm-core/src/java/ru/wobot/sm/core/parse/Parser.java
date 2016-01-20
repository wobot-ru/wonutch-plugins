package ru.wobot.sm.core.parse;

import java.net.URL;

public interface Parser {
    ParseResult parse(URL url, String content);
}
