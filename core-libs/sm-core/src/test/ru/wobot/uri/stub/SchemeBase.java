package ru.wobot.uri.stub;

import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;
import ru.wobot.uri.Scheme;

@Scheme("sm")
public abstract class SchemeBase {
    @Path("root/{a}")
    public abstract String root(@PathParam("a") String a);
}
