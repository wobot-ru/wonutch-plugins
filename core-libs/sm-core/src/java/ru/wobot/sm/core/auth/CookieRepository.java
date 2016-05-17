package ru.wobot.sm.core.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CookieRepository {
    public static final String COOKIES_FILE = "sm.cookies.file";
    private static final Logger LOG = LoggerFactory.getLogger(CookieRepository.class.getName());

    private Iterator<JsonNode> iterator;

    public synchronized void setConf(Configuration conf) {
        if (iterator == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            String cookiesFile = conf.get(COOKIES_FILE, "cookies.json");
            if (cookiesFile == null || cookiesFile.isEmpty())
                throw new IllegalStateException("No cookies file found in config.");

            List<JsonNode> logins;
            try {
                logins = objectMapper.readValue(conf.getConfResourceAsReader(cookiesFile), new TypeReference<List<JsonNode>>() {
                });
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't deserialize cookies from file provided in config.", e);
            }

            /* simple (ugly) workaround for not to use same logins every fetch */
            int numThreads = conf.getInt("fetcher.threads.fetch", 1);
            int numPartitions = logins.size() / numThreads;
            int startIndex = 0;
            if (numPartitions != 0)
                startIndex = numThreads * new Random().nextInt(numPartitions);
            // for debug only
            LOG.info("Thread: " + Thread.currentThread().getId() + "; Start index for cookie repo: " + startIndex);
            List<JsonNode> partOfLogins = logins.subList(startIndex, startIndex + numThreads);

            iterator = Iterators.cycle(partOfLogins);
        }
    }

    public LoginData getLoginData() {
        JsonNode node = getNext();
        String proxy = node.get("proxy").get("address").asText();
        Collection<Collection<HttpCookie>> cookieSet = new ArrayList<>();

        for (JsonNode n : node.get("cookies")) {
            Collection<HttpCookie> cookies = new ArrayList<>();
            for (JsonNode cookieNode : n) {
                HttpCookie cookie = new HttpCookie(cookieNode.get("name").asText(), cookieNode.get("value").asText());
                cookie.setDomain(cookieNode.get("domain").asText());
                cookie.setVersion(0); // for toString to generate only name value pairs
                cookies.add(cookie);
            }
            cookieSet.add(cookies);
        }

        return new LoginData(cookieSet, proxy);
    }

    private JsonNode getNext() {
        if (iterator == null)
            throw new IllegalStateException("Cookies not initialized.");

        JsonNode next;
        synchronized (iterator) {
            next = iterator.next();
            // for debug only
            LOG.info("Thread: " + Thread.currentThread().getId() + /*"; " +*/
                    //"Cookie used: " + next.get("cookies").get(0).get(1).get("value").asText() +
                    "; Proxy used: " + next.get("proxy").get("address").asText());
        }

        return next;
    }
}
