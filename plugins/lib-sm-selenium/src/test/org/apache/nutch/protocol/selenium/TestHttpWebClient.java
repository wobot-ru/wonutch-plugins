package org.apache.nutch.protocol.selenium;

import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class TestHttpWebClient {
    @Test
    public void shouldGetFullPageDataForId() {
        // given
        Configuration conf = new Configuration();
        conf.set(HttpWebClient.PHANTOMJS_EXECUTABLE_FILE, "phantomjs.exe");
        conf.set(HttpWebClient.COOKIES_FILE, "cookies.txt");

        // when
        String page = HttpWebClient.getHtmlPage("https://www.facebook.com/548469171978134", conf);

        // then
        assertThat(page, stringContainsInOrder(Arrays.asList("Friends", "Lives in", "From")));
    }
}
