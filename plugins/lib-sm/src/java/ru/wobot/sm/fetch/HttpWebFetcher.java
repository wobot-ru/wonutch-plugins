package ru.wobot.sm.fetch;

import com.google.common.collect.Iterators;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.auth.LoginData;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class HttpWebFetcher {
    private static final String FACEBOOK_URI = "https://www.facebook.com";
    private static final Logger LOG = LoggerFactory.getLogger(HttpWebFetcher.class.getName());

    private static CookieRepository cookieRepository;

    private static final ThreadLocal<LoginData> threadLoginData = new ThreadLocal<LoginData>() {
        @Override
        protected LoginData initialValue() {
            LOG.info("Thread: " + Thread.currentThread().getId() + "; Trying to get LoginData...");
            return cookieRepository.getLoginData();
        }
    };

    private static final ThreadLocal<Iterator<Collection<HttpCookie>>> threadCookieSetIterator = new ThreadLocal<Iterator<Collection<HttpCookie>>>() {
        @Override
        protected Iterator<Collection<HttpCookie>> initialValue() {
            LOG.info("Thread: " + Thread.currentThread().getId() + "; Trying to get Cookie sets iterator...");
            return Iterators.cycle(threadLoginData.get().getCookieSets());
        }
    };

    private static final ThreadLocal<WebDriver> threadWebDriver = new ThreadLocal<WebDriver>() {
        @Override
        protected WebDriver initialValue() {
            LOG.info("Thread: " + Thread.currentThread().getId() + "; Trying to create browser...");
            ArrayList<String> cliArgsCap = new ArrayList<>();
            cliArgsCap.add("--web-security=no");
            cliArgsCap.add("--ignore-ssl-errors=yes");
            cliArgsCap.add("--ssl-protocol=any");
            cliArgsCap.add("--proxy=" + getProxy());
            cliArgsCap.add("--proxy-auth=snt@wobot.co:PfYZ7J(b<^<[rhm");
            cliArgsCap.add("--proxy-type=http");
            cliArgsCap.add("--load-images=false");
            cliArgsCap.add("--webdriver-loglevel=ERROR");
            DesiredCapabilities caps = DesiredCapabilities.phantomjs();
            caps.setJavascriptEnabled(true);
            caps.setCapability("phantomjs.page.customHeaders." + "Accept-Language", "ru-RU");
            caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);

            WebDriver driver = new PhantomJSDriver(caps);
            driver.get(FACEBOOK_URI);
            return driver;
        }
    };

    private static String getProxy() {
        return threadLoginData.get().getProxy();
    }

    private static Collection<Cookie> getCookies() {
        Collection<Cookie> result = new ArrayList<>();
        for (HttpCookie cookie : threadCookieSetIterator.get().next()) {
            result.add(new Cookie.Builder(cookie.getName(), cookie.getValue()).domain(cookie.getDomain()).build());
            // for debug only
            if (cookie.getName().equals("c_user"))
                LOG.info("Thread: " + Thread.currentThread().getId() + "; Cookie used of user ID: " + cookie.getValue());
        }
        return result;
    }

    public HttpWebFetcher(CookieRepository cookieRepository) {
        HttpWebFetcher.cookieRepository = cookieRepository;
    }

    public String getHtmlPage(String url) {
        WebDriver driver = threadWebDriver.get();
        driver.manage().deleteAllCookies();

        Collection<Cookie> cookies = getCookies();
        if (cookies.isEmpty())
            throw new IllegalStateException("No cookies found. Can't authorize web driver.");


        for (Cookie cookie : cookies)
            driver.manage().addCookie(cookie);

        driver.get(url);
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

}