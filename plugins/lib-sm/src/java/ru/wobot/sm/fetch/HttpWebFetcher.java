package ru.wobot.sm.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SuccessResponse;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpWebFetcher {
    public static final String SELENIUM_HUB_URL = "sm.selenium.hub";
    private static final Log LOG = LogFactory.getLog(HttpWebFetcher.class.getName());

    private Configuration conf;
    private CookieRepository cookieRepository;

    private ThreadLocal<WebDriver> threadWebDriver = new ThreadLocal<WebDriver>() {
        @Override
        protected WebDriver initialValue() {
            String hubUrl = conf.get(SELENIUM_HUB_URL, "http://localhost:4444/wd/hub");
            if (hubUrl != null && !hubUrl.isEmpty()) {
                DesiredCapabilities caps = DesiredCapabilities.phantomjs();
                caps.setJavascriptEnabled(true);
                caps.setCapability("phantomjs.page.customHeaders." + "Accept-Language", "en-US");
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

    public HttpWebFetcher(Configuration conf, CookieRepository cookieRepository) {
        this.conf = conf;
        this.cookieRepository = cookieRepository;
    }

    public FetchResponse getHtmlPage(String url) {
        WebDriver driver = threadWebDriver.get();
        driver.get(url);

        boolean needToLogIn = false;
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        try {
            driver.findElement(By.linkText("Log In"));
            needToLogIn = true;
        } catch (NoSuchElementException alreadyLoggedIn) {
            // Already logged in to FB
        }

        String currentUrl = driver.getCurrentUrl();
        LOG.info("Thread: " + Thread.currentThread().getId() + "; needToLogin: " + needToLogIn + "; Current URL: " + currentUrl
                + "; Title: " + driver.getTitle());
        if (needToLogIn || currentUrl.contains("login") || driver.getTitle().toLowerCase().contains("facebook")) { //TODO: AFAIK every SM contains 'login' substring in login URL
            Collection<Cookie> cookies = getCookies();
            if (cookies.isEmpty())
                throw new IllegalStateException("No cookies found in cookies file. Can't authorize web driver.");

            for (Cookie cookie : cookies)
                driver.manage().addCookie(cookie);
            driver.navigate().refresh();
        }

        LOG.info("Thread: " + Thread.currentThread().getId() + "; Fetching URL: " + currentUrl + "; Original URL: " + url);

        // TODO: Consider other conditions for other SM
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        try {
            driver.findElement(By.cssSelector("div._5vf._2pie._2pip.sectionHeader")); // wait for this facebook only element
        } catch (NoSuchElementException e) {
            LOG.error("Thread: " + Thread.currentThread().getId() + "; No desired element for URL: " + currentUrl + "; Original URL: " + url);
        }

        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.MIME_TYPE, "text/html");
        }};
        return new SuccessResponse(driver.findElement(By.tagName("html")).getAttribute("innerHTML"), metaData);
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