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
public class TestFbParser {
    private final String uri;
    private final Integer friends;
    private final Integer followers;
    private final Content content;
    private final Configuration conf;
    private final String id;
    private final String name;
    private final String city;
    private final String href;
    private Parse parse;
    private Metadata parseMeta;
    private List<String> outlinks;

    public TestFbParser(String uri,
                        String resourceHtml,
                        String id,
                        String name,
                        String city,
                        String href,
                        Integer friends,
                        Integer followers,
                        List<String> outlinks) throws IOException, URISyntaxException {
        this.uri = uri;
        this.id = id;
        this.name = name;
        this.city = city;
        this.href = href;
        this.friends = friends;
        this.followers = followers;
        this.outlinks = outlinks;
        conf = NutchConfiguration.create();
        content = new Content(uri, uri, fetchContentData(resourceHtml), "text/html", new Metadata(), conf);
    }

    @Parameterized.Parameters(name = "{index}: parse({0})=friends={6}, followers={7}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"fb://nat.must?as_id=987562&screen_name&auth", "nat.must.html", "669573146", "Nat Must", "Moscow", "https://www.facebook.com/profile.php?id=669573146",
                        371, 67, Arrays.asList()}, //https://www.facebook.com/nat.must/about?section=contact-info
                {"fb://katerina.mikhalkova?as_id=9875621&screen_name&auth", "katerina.mikhalkova.html", "100003015534456", "Katerina Mikhalkova", "Moscow", "https://www.facebook.com/profile.php?id=100003015534456",
                        248, null, Arrays.asList()}, //https://www.facebook.com/katerina.mikhalkova/about?section=contact-info
                {"fb://zuck?as_id=98756212&screen_name&auth", "zurk.html", "4", "Mark Zuckerberg", "Palo Alto", "https://www.facebook.com/profile.php?id=4",
                        null, 52_737_643, Arrays.asList()}, //"https://www.facebook.com/zurk/about?section=contact-info"
                {"fb://100000045509142?as_id=98756213&auth", "100000045509142.html", "100000045509142", "саша беляев", "Moscow", "https://www.facebook.com/profile.php?id=100000045509142",
                        2_662, 333, Arrays.asList()}, //"https://www.facebook.com/profile.php?id=100000045509142&sk=about&section=contact-info"
                {"fb://tanja.vit?as_id=987562134&screen_name", "vitkina.unauth.html", "100001830254956", "Таня Виткина", "Москва", "https://www.facebook.com/profile.php?id=100001830254956",
                        null, null, Arrays.asList()}, //"fb://tanja.vit?as_id=987562134&screen_name&auth"
                {"fb://1592850897/profile?as_id=10207485754534006", "tatyana.bystrova.5.html", "1592850897", "Татьяна Быстрова", "Москва", "https://www.facebook.com/profile.php?id=1592850897",
                        1756, 750, Arrays.asList()}
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
    public void then_parseMeta_should_contains_id() {
        assertThat(parseMeta.get(ProfileProperties.SM_PROFILE_ID), is(equalTo(id)));
    }

    @Test
    public void then_parseMeta_should_contains_name() {
        assertThat(parseMeta.get(ProfileProperties.NAME), is(equalTo(name)));
    }

    @Test
    public void then_parseMeta_should_contains_city() {
        assertThat(parseMeta.get(ProfileProperties.CITY), is(equalTo(city)));
    }

    @Test
    public void then_parseMeta_should_contains_href() {
        assertThat(parseMeta.get(ProfileProperties.HREF), is(equalTo(href)));
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
