package ru.wobot.sm.core.parse;

import ru.wobot.sm.core.domain.ParseResult;

import java.net.URL;

/**
 * Created by Leon Misakyan on 15.01.2016.
 */
public interface Parser {
    ParseResult parse(URL url, String content);
}
