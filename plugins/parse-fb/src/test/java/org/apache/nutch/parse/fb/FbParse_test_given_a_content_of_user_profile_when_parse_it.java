package org.apache.nutch.parse.fb;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.NutchConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.wobot.sm.core.mapping.ProfileProperties;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(value = Parameterized.class)
public class FbParse_test_given_a_content_of_user_profile_when_parse_it {
    private final String uri;
    private final Integer friends;
    private final Integer followers;
    private final Content content;
    private final Configuration conf;
    private Parse parse;
    private Metadata parseMeta;
    private List<String> outlinks;

    public FbParse_test_given_a_content_of_user_profile_when_parse_it(String uri, String resourceHtml, Integer friends, Integer followers, List<String> outlinks) throws IOException, URISyntaxException {
        this.uri = uri;
        this.friends = friends;
        this.followers = followers;
        this.outlinks = outlinks;
        conf = NutchConfiguration.create();
        content = new Content(uri, uri, fetchContentData(resourceHtml), "text/html", new Metadata(), conf);
    }

    @Parameterized.Parameters(name = "{index}: parse({0})=friends={2}, followers={3}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"https://www.facebook.com/nat.must?as_id=987562", "nat.must.html", 371, 67, Arrays.asList("https://www.facebook.com/nat.must/about?section=contact-info")},
                {"https://www.facebook.com/katerina.mikhalkova", "katerina.mikhalkova.html", 248, null, Arrays.asList("https://www.facebook.com/katerina.mikhalkova/about?section=contact-info")},
                {"https://www.facebook.com/zurk", "zurk.html", null, 52_737_643, Arrays.asList("https://www.facebook.com/zurk/about?section=contact-info")},
                {"https://www.facebook.com/profile.php?id=100000045509142&as_id=9875621", "100000045509142.html", 2_662, 333, Arrays.asList("https://www.facebook.com/profile.php?id=100000045509142&sk=about&section=contact-info")},
                {"https://www.facebook.com/tanja.vit", "vitkina.unauth.html", null, null, Arrays.asList("")}
        });
    }

    @Before
    public void setup() {
        FbParser parser = new FbParser();
        parser.setConf(conf);
        parse = parser.getParse(content).get(uri);
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

    @Test
    public void then_parseMeta_should_contains_friend_count_equal_to() {
        if (parseMeta.get(ProfileProperties.FRIEND_COUNT) == null)
            assertThat(null, equalTo(friends));
        else
            assertThat(Integer.parseInt(parseMeta.get(ProfileProperties.FRIEND_COUNT)), equalTo(friends));
    }

    @Test
    public void then_parseMeta_should_contains_followers_count_equal_to() {
        if (parseMeta.get(ProfileProperties.FOLLOWER_COUNT) == null)
            assertThat(null, equalTo(followers));
        else
            assertThat(Integer.parseInt(parseMeta.get(ProfileProperties.FOLLOWER_COUNT)), equalTo(followers));
    }

    @Test
    public void then_parseMeta_should_contains_reach() {
        int reach = 0;
        if (parseMeta.get(ProfileProperties.FOLLOWER_COUNT) != null)
            reach = Integer.parseInt(parseMeta.get(ProfileProperties.FOLLOWER_COUNT));

        if (parseMeta.get(ProfileProperties.FRIEND_COUNT) != null)
            reach += Integer.parseInt(parseMeta.get(ProfileProperties.FRIEND_COUNT));

        assertThat(Integer.parseInt(parseMeta.get(ProfileProperties.REACH)), equalTo(reach));
    }

    @Test
    public void then_outlinks_should_contains_links() {
        List<String> actual = new ArrayList<>(outlinks.size());
        for (Outlink outlink : parse.getData().getOutlinks()) {
            actual.add(outlink.getToUrl());
        }
        assertThat(actual, is(equalTo(outlinks)));
    }

    private byte[] fetchContentData(String resource) throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        java.nio.file.Path path = java.nio.file.Paths.get(url.toURI());
        return Files.readAllBytes(path);
    }

}
