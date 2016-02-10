package ru.wobot.uri.stub;

import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;
import ru.wobot.uri.Scheme;

@Scheme("https")
public class HttpsScheme {
    @Path("root")
    public String root(){
        return "root";
    }

    @Path("{host}/{arg}")
    public String method(@PathParam("host") String host, @PathParam("arg") String arg) {
        return host + "/" + arg;
    }

}
