package org.apache.nutch.protocol.selenium;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestHttpWebClient {

    @Test
    public void shouldGetFullPageDataForId() {
        // given
        String page = HttpWebClient.getHtmlPage("https://www.facebook.com/548469171978134");

        // when

        // then
        assertThat(page, containsString("Friends"));
    }
}
