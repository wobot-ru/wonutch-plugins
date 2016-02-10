package ru.wobot.uri.impl;

import com.google.protobuf.Internal;
import org.hamcrest.Matchers;
import org.junit.Test;
import ru.wobot.uri.impl.stub.MethodInvokerStub;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

public class PathParser_Parse_Test {
    Map<String, ValueConverter> converters = new LinkedHashMap<String, ValueConverter>() {{
        put("int", new ValueConverter(Integer.class));
        put("str", new ValueConverter(String.class));
    }};

    @Test
    public void getSegments_not_empty() {
        final ParsedPath parsed = PathParser.parse(new MethodInvokerStub(), "abc/abc/{str}//", converters, converters);
        assertThat(parsed.getSegments(), is(not(empty())));
    }

    @Test
    public void is_segment_parsed_correct() {
        final ParsedPath parsed = PathParser.parse(new MethodInvokerStub(), "abc/abc/{str}/", converters, converters);
        final Segment[] segments = parsed.getSegments().toArray(new Segment[parsed.getSegments().size()]);
        assertThat(segments[2], is(notNullValue()));
    }

    @Test
    public void is_parsedPath_convert_query_correct() {
        final Map<String, String> query = new HashMap<String, String>() {{
            put("str", "str");
            put("int", "123");
        }};

        final ParsedPath parsed = PathParser.parse(new MethodInvokerStub(), "abc", converters, converters);
        Object[] objs = parsed.convertQuery(query);
        assertThat(objs, is(notNullValue()));
        assertThat((Integer) objs[0], equalTo(123));
        assertThat((String) objs[1], equalTo("str"));
    }
}
