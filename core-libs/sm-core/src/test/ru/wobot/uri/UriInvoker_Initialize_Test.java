package ru.wobot.uri;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.wobot.uri.stub.HttpsScheme;
import ru.wobot.uri.stub.WithoutPath;

public class UriInvoker_Initialize_Test {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void add_when_argument_is_null_should_throw_exception() throws Exception {
        this.thrown.expect(NullPointerException.class);
        new UriInvoker(null);
    }

    @Test
    public void add_when_argument_contains_null_should_throw_exception() throws Exception {
        this.thrown.expect(NullPointerException.class);
        new UriInvoker(new HttpsScheme(), null);
    }

    @Test
    public void add_when_argument_type_not_be_annotated_should_throw_exception() throws Exception {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("should be annotated by Scheme");
        new UriInvoker(new Object());
    }

    @Test
    public void add_when_argument_type_should_be_contain_method_with_path_annotation_otherwise_throw_exception() throws Exception {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("can't find Path annotation");
        new UriInvoker(new WithoutPath());
    }

}

