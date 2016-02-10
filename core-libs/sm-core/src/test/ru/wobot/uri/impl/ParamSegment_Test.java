package ru.wobot.uri.impl;

import com.google.protobuf.Internal;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class ParamSegment_Test {
    private HashMap<String, ValueConverter> converters = new HashMap<String, ValueConverter>() {{
        put("int", new ValueConverter(Internal.class));
        put("str", new ValueConverter(String.class));
    }};

    @Test
    public void testName() {
        // given
        ParamSegment paramSegment = new ParamSegment("{int}-{str}", new ValueConverter(String.class));
        // when
        //boolean isValid = paramSegment.isValid("132-459");
        //then
        //assertThat(isValid, equalTo(true));
    }
}