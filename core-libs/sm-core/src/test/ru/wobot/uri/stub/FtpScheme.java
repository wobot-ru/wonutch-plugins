package ru.wobot.uri.stub;

import ru.wobot.uri.Path;
import ru.wobot.uri.Scheme;

@Scheme("ftp")
public class FtpScheme {
    @Path("root")
    public Object root() {
        return new Object();
    }
}
