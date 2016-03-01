package ru.wobot.sm.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.Redirect;
import ru.wobot.sm.core.fetch.SuccessResponse;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpWebFetcher {
    public static final String SELENIUM_HUB_URL = "sm.selenium.hub";
    private static final String FACEBOOK_URI = "https://www.facebook.com";

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
        String currentUrl = driver.getCurrentUrl();

        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.MIME_TYPE, "text/html");
        }};

        if (currentUrl.contains("scontent.xx.fbcdn.net")) { // Profile picture. Trying to find user profile URL
            try {
                driver.get(retrieveFbProfileUrl(currentUrl));
            } catch (TimeoutException e) {
                return new Redirect(FACEBOOK_URI + "/" + url.split("/")[4], metaData);
            }
            currentUrl = driver.getCurrentUrl();
            try {
                if (currentUrl.contains("will.chengberg") ||  // I don't know who is this, but his profile has default pictures
                        currentUrl.contains("499829591") /*||
                        driver.findElement(By.cssSelector("h2._4-dp")).getText().contains("this page isn't available")*/)
                    return new Redirect(FACEBOOK_URI + "/" + url.split("/")[4], metaData);
            } catch (NoSuchElementException e) {
                // profile URL is correct
            }
        }

        if (currentUrl.contains("login") || driver.getTitle().toLowerCase().contains("facebook")) { //TODO: AFAIK every SM contains 'login' substring in login URL
            Collection<Cookie> cookies = getCookies();
            if (cookies.isEmpty())
                throw new IllegalStateException("No cookies found in cookies file. Can't authorize web driver.");

            for (Cookie cookie : cookies)
                driver.manage().addCookie(cookie);
            driver.navigate().refresh();
        }
        // TODO: Consider other conditions for other SM
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        //driver.findElement(By.cssSelector("div._e4b")); // wait for this facebook only element
        driver.findElement(By.cssSelector("div._5vf._2pie._2pip.sectionHeader"));
        return new SuccessResponse(driver.findElement(By.tagName("html")).getAttribute("innerHTML"), metaData);
    }

    private String retrieveFbProfileUrl(String url) {
        WebDriver driver = threadWebDriver.get();
        URI uri = URI.create(url);
        String part = uri.getPath();
        String[] parts = part.substring(part.lastIndexOf("/") + 1).split("_");
        if (parts.length < 2)
            throw new IllegalStateException("Unsupported URL [" + url + "].");

        driver.get(FACEBOOK_URI + "/" + parts[0] + "_" + parts[1]);
        (new WebDriverWait(driver, 5)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().contains("photo");
            }
        });
        List<NameValuePair> params = URLEncodedUtils.parse(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        String paramValue = params.get(1).getValue();
        return FACEBOOK_URI + "/" + paramValue.substring(paramValue.lastIndexOf(".") + 1);
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