package ru.wobot.uri;

import org.junit.Test;
import ru.wobot.uri.impl.ParsedUri;
import ru.wobot.uri.stub.SchemeBaseImpl;
import ru.wobot.uri.stub.SchemeInterfaceIml;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class UriTranslator_Inheritance_Test {
    @Test
    public void add_when_annotation_defined_in_interface_than_translator_should_find_Path_and_PathParam_annotation() throws URISyntaxException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        final UriTranslator translator = new UriTranslator(new SchemeInterfaceIml());
        String result = translator.translate(ParsedUri.parse(new URI("sm://root/123")));
        assertThat(result, equalTo("123"));
    }

    @Test
    public void add_when_annotation_inherit_from_abstract_than_translator_should_find_Path_and_PathParam_annotation() throws URISyntaxException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        final UriTranslator translator = new UriTranslator(new SchemeBaseImpl());
        String result = translator.translate(ParsedUri.parse(new URI("sm://root/abc")));
        assertThat(result, equalTo("abc"));
    }
}
