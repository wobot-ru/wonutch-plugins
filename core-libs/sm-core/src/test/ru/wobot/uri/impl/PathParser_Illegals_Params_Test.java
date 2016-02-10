package ru.wobot.uri.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.wobot.uri.impl.stub.MethodInvokerStub;

import java.util.HashMap;

public class PathParser_Illegals_Params_Test {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void add_when_path_does_not_contain_hostname_should_throw_exception() throws Exception {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("path can't be contain empty segment");
        PathParser.parse(new MethodInvokerStub(),"/", new HashMap<String, ValueConverter>(), new HashMap<String, ValueConverter>());
    }


    @Test
    public void add_when_path_contains_empty_segment_should_throw_exception() throws Exception {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("segment can't be empty");
        PathParser.parse(new MethodInvokerStub(), "", new HashMap<String, ValueConverter>(), new HashMap<String, ValueConverter>());
    }

    @Test
    public void add_when_path_contains_empty_segment_2_should_throw_exception() throws Exception {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("segment can't be empty");
        PathParser.parse(new MethodInvokerStub(), "/sadsad/asdasd//asdasd", new HashMap<String, ValueConverter>(), new HashMap<String, ValueConverter>());
    }

}