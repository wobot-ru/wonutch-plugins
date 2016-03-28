package ru.wobot.sm.fetch;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;
import ru.wobot.sm.core.auth.CookieRepository;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class TestHttpWebFetcher {
    private static Configuration conf = new Configuration();
    private static CookieRepository cookieRepository = new CookieRepository();
    private static HttpWebFetcher webFetcher = new HttpWebFetcher(cookieRepository);

    static {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(new ConsoleAppender(
                new PatternLayout("%d{ISO8601} %-5p %c{2} - %m%n")));

        cookieRepository.setConf(conf);
    }

    private String response(String url) {
        return webFetcher.getHtmlPage(url);
    }

    @Test
    public void shouldGetFullPageDataForIdWithNoUsernameAndNoPhoto() {
        // given
        String url = "https://www.facebook.com/profile.php?id=100004451677809";

        // when

        // then
        assertThat(response(url), stringContainsInOrder(Arrays.asList("Наталья", "SPBGUKI", "Живет в городе")));
    }

    @Test
    public void shouldGetFullPageDataForIdWithScreenNameAndPhoto() {
        // given
        String url = "https://www.facebook.com/profile.php?id=100001830254956";

        // when

        // then
        assertThat(response(url), stringContainsInOrder(Arrays.asList("Друзья", "Живет", "Из")));
    }

    @Test
    public void shouldGetFullPageDataForEmptyProfile() {
        // given
        String url = "https://www.facebook.com/profile.php?id=100010198925638";

        // when

        // then
        assertThat(response(url), stringContainsInOrder(Arrays.asList("Хроника", "Информация", "Друзья")));
    }

    /*@Test
    public void shouldGetFullPageDataForEmptyProfile_() {
        // given
        String url = "http://myip.ru/index_small.php";

        // when

        // then
        assertThat(response(url), stringContainsInOrder(Arrays.asList("Хроника", "Информация", "Друзья")));
    }*/
}
