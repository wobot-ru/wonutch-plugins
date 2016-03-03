package org.apache.nutch.parse.fb;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.NutchConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FbParse_test_given_a_content_of_user_profile_and_screen_name_url_when_parse_it {
    private final String url = "https://www.facebook.com/katerina.mikhalkova";
    private Parse parse;
    private Metadata parseMeta;

    @Before
    public void setup() throws IOException, URISyntaxException {
        Configuration conf = NutchConfiguration.create();
        FbParser parser = new FbParser();
        parser.setConf(conf);
        Content content = new Content(url, url, fetchContentData("katerina.mikhalkova.html"), "text/html", new Metadata(), conf);
        parse = parser.getParse(content).get(url);
        parseMeta = parse.getData().getParseMeta();
    }

    @Test
    public void then_parse_should_be_not_null() {
        assertThat(parse, is(notNullValue()));
    }

    @Test
    public void then_parseMeta_should_be_not_null() {
        assertThat(parseMeta, is(notNullValue()));
    }

    private byte[] fetchContentData(String resource) throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        java.nio.file.Path path = java.nio.file.Paths.get(url.toURI());
        return Files.readAllBytes(path);
    }
}
