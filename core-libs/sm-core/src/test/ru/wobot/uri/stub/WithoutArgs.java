package ru.wobot.uri.stub;

import ru.wobot.uri.Path;
import ru.wobot.uri.Scheme;

@Scheme("http")
public class WithoutArgs {
    @Path("/")
    String root() {
        return "root";
    }
}
