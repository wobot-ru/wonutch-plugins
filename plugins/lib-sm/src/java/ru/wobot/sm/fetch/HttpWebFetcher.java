package ru.wobot.sm.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.util.Base64;
import org.apache.hadoop.conf.Configuration;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.wobot.sm.core.auth.CookieRepository;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HttpWebFetcher {
    public static final String SELENIUM_HUB_URL = "sm.selenium.hub";
    private static final String FACEBOOK_URI = "https://www.facebook.com";
    private static final Log LOG = LogFactory.getLog(HttpWebFetcher.class.getName());

    private Configuration conf;
    private CookieRepository cookieRepository;

    /*static {
        try
        {
            Field field = ProxyUtils.class.getDeclaredField("SHOULD_NOT_PROXY_HOP_BY_HOP_HEADERS");
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, ImmutableSet.of(
                    "Connection".toLowerCase(Locale.US), "Proxy-Authenticate".toLowerCase(Locale.US),
                    "TE".toLowerCase(Locale.US), "Trailer".toLowerCase(Locale.US), "Upgrade".toLowerCase(Locale.US),
                    new String[]{"Keep-Alive".toLowerCase(Locale.US)}));
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }
*/
    private static final ThreadLocal<WebDriver> threadWebDriver = new ThreadLocal<WebDriver>() {
        @Override
        protected WebDriver initialValue() {
            LOG.info("Thread: " + Thread.currentThread().getId() + "; Trying to get browser...");
            //String hubUrl = conf.get(SELENIUM_HUB_URL, "http://localhost:4444/wd/hub");
            String hubUrl = "http://localhost:4444/wd/hub"; //TODO: fix this workaround ASAP
            if (hubUrl != null && !hubUrl.isEmpty()) {
                /*Proxy proxy = new Proxy();
                proxy.setHttpProxy("http://snt%40wobot.co:PfYZ7J%28b%3c^%3c[rhm@46.182.28.173:6060");
                proxy.setProxyType(Proxy.ProxyType.MANUAL);*/
                // start the proxy
                BrowserMobProxy proxy = new BrowserMobProxyServer();
                /*proxy.setChainedProxy(new InetSocketAddress("46.182.28.173", 6060));
                String authParam = "snt@wobot.co:PfYZ7J(b<^<[rhm";
                //String authParam = "snt%40wobot.co:PfYZ7J%28b%3c^%3c[rhm";
                authParam = new String(Base64.encodeBase64(authParam.getBytes(StandardCharsets.UTF_8)));
                proxy.addHeader("Proxy-Authorization", "Basic " + authParam);*/
                proxy.start(0);

                // get the Selenium proxy object
                Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

                // configure it as a desired capability
                DesiredCapabilities caps = DesiredCapabilities.chrome();
                caps.setJavascriptEnabled(true);
                //caps.setCapability("phantomjs.page.customHeaders." + "Accept-Language", "ru-RU");
                caps.setCapability(CapabilityType.PROXY, seleniumProxy);
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
        /*if (driver.manage().getCookies().size() < 9) {
            Collection<Cookie> cookies = getCookies();
            if (cookies.isEmpty())
                throw new IllegalStateException("No cookies found in cookies file. Can't authorize web driver.");

            for (Cookie cookie : cookies)
                driver.manage().addCookie(cookie);
        }
*/
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