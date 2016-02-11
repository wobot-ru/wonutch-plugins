package ru.wobot.sm.core.parse;

import java.net.URI;

public interface Parser {
    ParseResult parse(URI uri, String content);
}
