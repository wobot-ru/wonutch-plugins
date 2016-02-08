package ru.wobot.uri.stub;

import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;
import ru.wobot.uri.Scheme;

@Scheme("sm")
public interface SchemeInterface {
    @Path("root/{a}")
    String root(@PathParam("a") String a);
}

