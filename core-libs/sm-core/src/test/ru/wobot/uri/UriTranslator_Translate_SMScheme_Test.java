package ru.wobot.uri;

import org.junit.Before;
import org.junit.Test;
import ru.wobot.uri.impl.ParsedUri;

import java.lang.reflect.InvocationTargetException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class UriTranslator_Translate_SMScheme_Test {
    private UriTranslator translator;
    private SMScheme scheme;

    @Before
    public void setUp() throws ClassNotFoundException {
        scheme = spy(SMScheme.class);
        translator = new UriTranslator(scheme);
    }

    @Test
    public void add_when_translate_const_segment_than_defined_method_should_be_invoked() throws InvocationTargetException, IllegalAccessException {
        // given
        // when
        translator.translate(ParsedUri.parse("sm://root"));

        // than
        verify(scheme).root();
    }

    @Test
    public void add_when_translate_uri_with_pathParams_than_defined_method_should_be_invoked() throws InvocationTargetException, IllegalAccessException {
        // given
        // when
        translator.translate(ParsedUri.parse("sm://root/arg1/arg2/"));

        // than
        verify(scheme).method1("root", "arg1", "arg2");
    }

    @Test
    public void given_complex_pathParam_and_when_translate_uri_than_invocation_should_be_correct() throws InvocationTargetException, IllegalAccessException {
        // given
        // when
        translator.translate(ParsedUri.parse("sm://prefix-test-suffix"));

        // than
        verify(scheme).method2("-test-");
    }

    @Scheme("sm")
    public interface SMScheme {
        @Path("root")
        String root();

        @Path("{host}/{arg1}/{arg2}")
        String method1(@PathParam("host") String host, @PathParam("arg1") String arg1, @PathParam("arg2") String arg2);

        @Path("prefix{host}suffix")
        String method2(@PathParam("host") String host);
    }
}
