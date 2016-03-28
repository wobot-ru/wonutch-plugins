package ru.wobot.sm.core.auth;

import java.net.HttpCookie;
import java.util.Collection;

/**
 * Aggregate, containing user cookies, and proxy address, associated with it.
 */
public class LoginData {
    private final Collection<HttpCookie> cookies;

    private final String proxy;

    public LoginData(Collection<HttpCookie> cookies, String proxy) {
        this.cookies = cookies;
        this.proxy = proxy;
    }

    public Collection<HttpCookie> getCookies() {
        return cookies;
    }

    public String getProxy() {
        return proxy;
    }
}
