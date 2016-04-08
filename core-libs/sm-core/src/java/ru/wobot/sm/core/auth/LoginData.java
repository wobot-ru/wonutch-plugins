package ru.wobot.sm.core.auth;

import java.net.HttpCookie;
import java.util.Collection;

/**
 * Aggregate, containing set of user cookieSets, and proxy address, associated with it.
 */
public class LoginData {
    /**
     * Set of "cookie sets", used for authenticate user in social media site
     */
    private final Collection<Collection<HttpCookie>> cookieSets;

    /**
     * Proxy address:port, from which one should access social media site
     */
    private final String proxy;

    public LoginData(Collection<Collection<HttpCookie>> cookieSets, String proxy) {
        this.cookieSets = cookieSets;
        this.proxy = proxy;
    }

    public Collection<Collection<HttpCookie>> getCookieSets() {
        return cookieSets;
    }

    public String getProxy() {
        return proxy;
    }
}
