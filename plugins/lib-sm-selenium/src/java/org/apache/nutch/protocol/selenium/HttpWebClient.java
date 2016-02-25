/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.protocol.selenium;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wobot.sm.core.auth.CookieRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class HttpWebClient {
    public static final String SELENIUM_HUB_URL = "sm.selenium.hub";
    private static final Logger LOG = LoggerFactory.getLogger(HttpWebClient.class);

    private Configuration conf;
    private CookieRepository cookieRepository;

    private ThreadLocal<WebDriver> threadWebDriver = new ThreadLocal<WebDriver>() {
        @Override
        protected WebDriver initialValue() {
            String hubUrl = conf.get(SELENIUM_HUB_URL, "http://localhost:4444/wd/hub");
            if (hubUrl != null && !hubUrl.isEmpty()) {
                DesiredCapabilities caps = DesiredCapabilities.phantomjs();
                caps.setJavascriptEnabled(true);
                WebDriver driver;
                try {
                    driver = new RemoteWebDriver(new URL(hubUrl), caps);
                } catch (MalformedURLException e) {
                    throw new IllegalStateException("Malformed Selenium grid hub URL found in config.", e);
                } catch (Exception e) {
                    throw new IllegalStateException("Browser start-up failure.", e);
                }
                return driver;
            } else
                throw new IllegalStateException("No Selenium grid hub URL found in config.");
        }
    };

    public HttpWebClient(Configuration conf, CookieRepository cookieRepository) {
        this.conf = conf;
        this.cookieRepository = cookieRepository;
    }

    public String getHtmlPage(String url) {
        WebDriver driver = threadWebDriver.get();
        driver.get(url);
        if (driver.getCurrentUrl().contains("login")) { //TODO: AFAIK every SM contains 'login' substring in login URL
            Collection<Cookie> cookies = getCookies();
            if (cookies.isEmpty())
                throw new IllegalStateException("No cookies found in cookies file. Can't authorize web driver.");

            for (Cookie cookie : cookies)
                driver.manage().addCookie(cookie);
        }
        // TODO: Consider other conditions for other SM
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        driver.findElement(By.cssSelector("div._5nb8")); // facebook only

        return driver.findElement(By.tagName("html")).getAttribute("innerHTML");
    }

    private Collection<Cookie> getCookies() {
        Collection<Cookie> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (String line : cookieRepository.getCookies()) {
            JsonNode cookie;
            try {
                cookie = mapper.readValue(line, JsonNode.class);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't deserialize cookie string [" + line + "] from repository", e);
            }
            result.add(new Cookie.Builder(cookie.get("name").asText(), cookie.get("value").asText()).domain(cookie.get("domain").asText()).build());
        }

        return result;
    }
}