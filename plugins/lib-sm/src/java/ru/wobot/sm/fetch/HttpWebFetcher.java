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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class HttpWebFetcher {
    public static final String SELENIUM_HUB_URL = "sm.selenium.hub";
    private static final String FACEBOOK_URI = "https://www.facebook.com";
    private static final Log LOG = LogFactory.getLog(HttpWebFetcher.class.getName());

    private Configuration conf;
    private CookieRepository cookieRepository;

    private static final ThreadLocal<WebDriver> threadWebDriver = new ThreadLocal<WebDriver>() {
        @Override
        protected WebDriver initialValue() {
            LOG.info("Thread: " + Thread.currentThread().getId() + "; Trying to get browser...");
            //String hubUrl = conf.get(SELENIUM_HUB_URL, "http://localhost:4444/wd/hub");
            String hubUrl = "http://localhost:4444/wd/hub"; //TODO: fix this workaround ASAP
            if (hubUrl != null && !hubUrl.isEmpty()) {
                DesiredCapabilities caps = DesiredCapabilities.phantomjs();
                caps.setJavascriptEnabled(true);
                caps.setCapability("phantomjs.page.customHeaders." + "Accept-Language", "ru-RU");
                WebDriver driver;
                try {
                    driver = new RemoteWebDriver(new URL(hubUrl), caps);
                    driver.get(FACEBOOK_URI);
                } catch (MalformedURLException e) {
                    throw new IllegalStateException("Malformed Selenium grid hub URL found in config.", e);
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

    public String getHtmlPage(String url) {
        WebDriver driver = threadWebDriver.get();
        if (driver.manage().getCookies().size() < 9) {
            Collection<Cookie> cookies = getCookies();
            if (cookies.isEmpty())
                throw new IllegalStateException("No cookies found in cookies file. Can't authorize web driver.");

            for (Cookie cookie : cookies)
                driver.manage().addCookie(cookie);
        }

        driver.get(url);
        /*boolean needToLogIn = false;
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        try {
            driver.findElement(By.id("loginbutton"));
            needToLogIn = true;
        } catch (NoSuchElementException alreadyLoggedIn) {
            // Already logged in to FB
        }


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
*/
        String currentUrl = driver.getCurrentUrl();
        LOG.info("Thread: " + Thread.currentThread().getId() + "; Fetching URL: " + currentUrl + "; Original URL: " + url);

        // TODO: Consider other conditions for other SM
        driver.manage().timeouts().implicitlyWait(4, TimeUnit.SECONDS);
        try {
            driver.findElement(By.cssSelector("div._5vf._2pie._2pip.sectionHeader")); // wait for this facebook only element
        } catch (NoSuchElementException e) {
            LOG.error("Thread: " + Thread.currentThread().getId() + "; No desired element for URL: " + currentUrl + "; Original URL: " + url);
        }

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