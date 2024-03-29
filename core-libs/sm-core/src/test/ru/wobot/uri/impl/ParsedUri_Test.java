package ru.wobot.uri.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ParsedUri_Test {
    @Test
    public void test_segment_parse() throws Exception {
        // given
        final Collection<String> segments = new ArrayList<String>() {{
            add("yaru");
            add("seg1");
            add("seg2");
        }};

        // when
        final ParsedUri parsed = ParsedUri.parse(new URI("https://yaru/seg1/seg2/"));

        // then
        assertThat(parsed.getSegments(), equalTo(segments));
        assertThat(parsed.getScheme(), equalTo("https"));
    }

    @Test
    public void test_query_parse() throws Exception {
        // given
        final Map<String, String> query = new HashMap<String, String>() {{
            put("arg1", "arg1");
            put("arg2", "2");
        }};

        // when
        final ParsedUri parsed = ParsedUri.parse(new URI("https://yaru?arg1=arg1&arg2=2"));

        // then
        assertThat(parsed.getQuery(), equalTo(query));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void should_throw_if_host_lasts_w_numbers_after_dot() throws Exception {
        // given
        URI uri = URI.create("fb://elena.sizova.35/profile?as_id=10153926526717679&screen_name");

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Host is null for URI [" + uri.toString() + "]");
        // when
        ParsedUri.parse(uri);

        // then

    }
}