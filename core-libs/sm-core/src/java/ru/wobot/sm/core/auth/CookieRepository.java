package ru.wobot.sm.core.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CookieRepository {
    public static final String COOKIES_FILE = "sm.cookies.file";
    private static final Log LOG = LogFactory.getLog(CookieRepository.class.getName());

    private final Iterator<Collection<JsonNode>> iterator;

    public CookieRepository(Configuration conf) {
        ObjectMapper objectMapper = new ObjectMapper();
        String cookiesFile = conf.get(COOKIES_FILE, "cookies.json");
        if (cookiesFile == null || cookiesFile.isEmpty())
            throw new IllegalStateException("No cookies file found in config.");

        Collection<Collection<JsonNode>> cookies;
        try {
            cookies = objectMapper.readValue(conf.getConfResourceAsReader(cookiesFile), new TypeReference<Collection<Collection<JsonNode>>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't deserialize cookies from file provided in config.");
        }

        iterator = Iterators.cycle(cookies);
    }

    public Collection<String> getCookies() {
        Collection<JsonNode> next;
        Collection<String> result = new ArrayList<>();
        synchronized (iterator) {
            next = iterator.next();
            // for debug only
            LOG.info("Thread: " + Thread.currentThread().getId() + "; Cookie used: " + ((List<JsonNode>)next).get(2).get("value").asText());
        }
        for (JsonNode n : next)
            result.add(String.valueOf(n));

        return result;
    }
}
