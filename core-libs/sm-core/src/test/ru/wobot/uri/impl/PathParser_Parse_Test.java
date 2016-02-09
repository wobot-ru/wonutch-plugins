package ru.wobot.uri.impl;

import com.google.protobuf.Internal;
import org.junit.Test;
import ru.wobot.sm.core.reflect.MethodInvoker;
import ru.wobot.uri.impl.stub.MethodInvokerStub;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

public class PathParser_Parse_Test {
    private HashMap<String, ValueConverter> converters = new HashMap<String, ValueConverter>() {{
        put("int", new ValueConverter(Internal.class));
        put("str", new ValueConverter(String.class));
    }};

    @Test
    public void test1() {
        final ParsedPath parsed = PathParser.parse(new MethodInvokerStub(), "abc/abc/{str}//", converters);
        assertThat(parsed.getSegments(), is(not(empty())));
    }

    @Test
    public void test2() {
        final ParsedPath parsed = PathParser.parse(new MethodInvokerStub(), "abc/abc/{str}/", converters);
        final Segment[] segments = parsed.getSegments().toArray(new Segment[parsed.getSegments().size()]);
        assertThat(segments[2], is(notNullValue()));
    }
}
