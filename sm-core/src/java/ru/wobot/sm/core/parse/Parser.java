package ru.wobot.sm.core.parse;

import ru.wobot.sm.core.domain.ParseResult;

import java.net.URL;

public interface Parser {
    ParseResult parse(URL url, String content);
}
