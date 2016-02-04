package ru.wobot.uri.stub;

import ru.wobot.uri.Path;
import ru.wobot.uri.Scheme;

@Scheme("https")
public class HttpsScheme {
    @Path("{arg1}")
    String method(String arg1) {
        return arg1;
    }
}
