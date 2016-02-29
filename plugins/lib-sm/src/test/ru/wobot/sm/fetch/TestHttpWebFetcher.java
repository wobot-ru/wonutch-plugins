package ru.wobot.sm.fetch;

import org.apache.hadoop.conf.Configuration;
import org.junit.Test;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.fetch.FetchResponse;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.Is.is;

public class TestHttpWebFetcher {
    private static Configuration conf = new Configuration();
    private static CookieRepository cookieRepository = new CookieRepository(conf);
    private static HttpWebFetcher webFetcher = new HttpWebFetcher(conf, cookieRepository);

    private FetchResponse response(String url) {
        return webFetcher.getHtmlPage(url);
    }

    @Test
    public void shouldRedirectFromUrlIfNoUserPhoto() {
        // given
        String url = "https://graph.facebook.com/v2.5/548469171978134/picture";

        // when
        FetchResponse response = response(url);

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("https://www.facebook.com/548469171978134"));
    }

    @Test
    public void shouldGetFullPageDataForIdWithNoUsernameAndNoPhoto() {
        // given
        String url = "https://www.facebook.com/548469171978134";

        // when

        // then
        assertThat(response(url).getData(), stringContainsInOrder(Arrays.asList("Friends", "Lives in", "From")));
    }

    @Test
    public void shouldGetFullPageDataForIdWithScreenNameAndPhoto() {
        // given
        String url = "https://graph.facebook.com/v2.5/1015732591831073/picture";

        // when

        // then
        assertThat(response(url).getData(), stringContainsInOrder(Arrays.asList("Friends", "Lives in", "From")));
    }

    @Test
    public void shouldGetFullPageDataForIdWithHiddenPhoto() {
        // given
        String url = "https://graph.facebook.com/v2.5/990538281016665/picture";

        // when
        FetchResponse response = response(url);

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("https://www.facebook.com/990538281016665"));
    }

}
