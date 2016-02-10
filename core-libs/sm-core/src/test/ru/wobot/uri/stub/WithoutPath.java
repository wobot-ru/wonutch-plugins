package ru.wobot.uri.stub;

import ru.wobot.uri.Scheme;

@Scheme("https")
public class WithoutPath {
    String method(String arg1) {
        return arg1;
    }
}
