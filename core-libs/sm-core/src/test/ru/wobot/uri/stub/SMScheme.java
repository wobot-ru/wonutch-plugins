package ru.wobot.uri.stub;

import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;
import ru.wobot.uri.Scheme;

@Scheme("sm")
public class SMScheme {
    private boolean isRootInvoked;
    private boolean ifMethod1Invoked;

    @Path("root")
    public String root() {
        isRootInvoked = true;
        return "root";
    }

    @Path("{host}/{arg1}/{arg2}")
    public String method1(@PathParam("host") String host, @PathParam("arg1") String arg1, @PathParam("arg2") String arg2) {
        ifMethod1Invoked = true;
        return host + "/" + arg1 + "/" + arg2;
    }

    public boolean isRootInvoked() {
        return isRootInvoked;
    }

    public boolean isMethod1Invoked() {
        return ifMethod1Invoked;
    }
}
