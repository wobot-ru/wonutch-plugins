package org.apache.nutch.protocol.selenium;

import org.apache.hadoop.conf.Configuration;
import org.junit.Test;
import ru.wobot.sm.core.auth.CookieRepository;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class TestHttpWebClient {
    private static Configuration conf = new Configuration();
    private static CookieRepository cookieRepository = new CookieRepository(conf);
    private static HttpWebClient webClient = new HttpWebClient(conf, cookieRepository);

    @Test
    public void shouldGetFullPageDataForId() {
        // given

        // when
        String page = webClient.getHtmlPage("https://www.facebook.com/548469171978134");

        // then
        assertThat(page, stringContainsInOrder(Arrays.asList("Friends", "Lives in", "From")));
    }

    @Test
    public void shouldGetFullPageDataForIdWithScreenName() {
        // given

        // when
        String page = webClient.getHtmlPage("https://www.facebook.com/1015732591831073");

        // then
        assertThat(page, stringContainsInOrder(Arrays.asList("Friends", "Lives in", "From")));
    }
}
