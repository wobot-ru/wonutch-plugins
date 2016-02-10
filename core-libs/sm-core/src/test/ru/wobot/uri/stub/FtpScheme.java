package ru.wobot.uri.stub;

import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;
import ru.wobot.uri.Scheme;

import java.lang.annotation.Inherited;
import java.util.Objects;

@Scheme("ftp")
public class FtpScheme {
    @Path("{arg1}")
    String method(@PathParam("arg1") String arg1) {
        return arg1;
    }

    @Path("root")
    public Object root() {
        return new Object();
    }
}
