package ru.wobot.sm;

import org.junit.Test;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.serialize.Serializer;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.core.Is.is;

public class TestSerialization {
    @Test
    public void for_ParseResult() throws Exception {
        // given
        Map<String, String> links = new HashMap<String, String>() {{
            put("vk://dura", "dura");
            put("vk://trololo", "trololo");
        }};
        Map<String, String> parseMeta = new HashMap<String, String>() {{
            put("p1", "p1");
            put("p2", "p2");
        }};
        Map<String, String> contentMeta = new HashMap<String, String>() {{
            put("c1", "c1");
            put("c2", "c2");
        }};

        // when
        ParseResult actual = new ParseResult("vk://yoyo", "title", "[42, 24]", links, parseMeta, contentMeta);
        String json = Serializer.getInstance().toJson(actual);
        ParseResult expected = Serializer.getInstance().fromJson(json, ParseResult.class);

        // then
        assertThat(actual, is(samePropertyValuesAs(expected)));
    }
}
