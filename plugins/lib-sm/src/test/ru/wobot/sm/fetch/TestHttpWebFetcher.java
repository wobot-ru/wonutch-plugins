package ru.wobot.sm.fetch;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Ignore;
import org.junit.Test;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.fetch.FetchResponse;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;

@Ignore
public class TestHttpWebFetcher {
    static {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(new ConsoleAppender(
                new PatternLayout("%d{ISO8601} %-5p %c{2} - %m%n")));
    }

    private static Configuration conf = new Configuration();
    private static CookieRepository cookieRepository = new CookieRepository(conf);
    private static HttpWebFetcher webFetcher = new HttpWebFetcher(conf, cookieRepository);

    private FetchResponse response(String url) {
        return webFetcher.getHtmlPage(url);
    }

    @Test
    public void shouldGetFullPageDataForIdWithNoUsernameAndNoPhoto() {
        // given
        String url = "https://www.facebook.com/profile.php?id=100004451677809&as_id=548469171978134";

        // when

        // then
        assertThat(response(url).getData(), stringContainsInOrder(Arrays.asList("Friends", "Lives in", "From")));
    }

    @Test
    public void shouldGetFullPageDataForIdWithScreenNameAndPhoto() {
        // given
        String url = "https://www.facebook.com/profile.php?id=100001830254956";

        // when

        // then
        assertThat(response(url).getData(), stringContainsInOrder(Arrays.asList("Friends", "Lives in", "From")));
    }

    @Test
    public void shouldGetFullPageDataForEmptyProfile() {
        // given
        String url = "https://www.facebook.com/profile.php?id=100010198925638";

        // when

        // then
        assertThat(response(url).getData(), stringContainsInOrder(Arrays.asList("Timeline", "About", "Friends")));
    }
}
