package ru.wobot.sm.core.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class CookieRepository {
    public static final String COOKIES_FILE = "sm.cookies.file";
    private static final Log LOG = LogFactory.getLog(CookieRepository.class.getName());

    private Iterator<JsonNode> iterator;

    public synchronized void setConf(Configuration conf) {
        if (iterator == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            String cookiesFile = conf.get(COOKIES_FILE, "cookies.json");
            if (cookiesFile == null || cookiesFile.isEmpty())
                throw new IllegalStateException("No cookies file found in config.");

            Collection<JsonNode> cookies;
            try {
                cookies = objectMapper.readValue(conf.getConfResourceAsReader(cookiesFile), new TypeReference<Collection<JsonNode>>() {
                });
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't deserialize cookies from file provided in config.", e);
            }

            iterator = Iterators.cycle(cookies);
        }
    }

    public LoginData getLoginData() {
        JsonNode node = getNext();
        String proxy = node.get("proxy").get("address").asText();
        Collection<HttpCookie> cookies = new ArrayList<>();
        for (JsonNode n : node.get("cookies")) {
            HttpCookie cookie = new HttpCookie(n.get("name").asText(), n.get("value").asText());
            cookie.setDomain(n.get("domain").asText());
            cookie.setVersion(0); // for toString to generate only name value pairs
            cookies.add(cookie);
        }

        return new LoginData(cookies, proxy);
    }

    /*public Collection<String> getCookiesAsNameValuePairs() {
        Collection<String> result = new ArrayList<>();
        for (JsonNode n : getNext())
            result.add(n.get("name").asText() + "=" + n.get("value").asText());

        return result;
    }*/

    private JsonNode getNext() {
        if (iterator == null)
            throw new IllegalStateException("Cookies not initialized.");

        JsonNode next;
        synchronized (iterator) {
            next = iterator.next();
            // for debug only
            LOG.info("Thread: " + Thread.currentThread().getId() + "; Cookie used: " + next.get("cookies").get(2).asText()
                    + "; Proxy used: " + next.get("proxy").get("address").asText());
        }
        return next;
    }
}
