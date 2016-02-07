package ru.wobot.uri;

import org.junit.Before;
import org.junit.Test;
import ru.wobot.uri.impl.ParsedUri;
import ru.wobot.uri.stub.SMScheme;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class UriTranslator_Translate_SMScheme_Test {
    private UriTranslator translator;
    private SMScheme scheme;

    @Before
    public void setUp() throws ClassNotFoundException {
        scheme = new SMScheme();
        translator = new UriTranslator(scheme);
    }

    @Test
    public void add_when_translate_const_segment_than_defined_method_should_be_invoked() throws URISyntaxException, InvocationTargetException, IllegalAccessException {
        String result = translator.translate(ParsedUri.parse(new URI("sm://root")));
        assertThat(result, equalTo("root"));
        assertThat(scheme.isRootInvoked(), equalTo(true));
    }

    @Test
    public void add_when_translate_uri_with_pathParams_than_defined_method_should_be_invoked() throws URISyntaxException, InvocationTargetException, IllegalAccessException {
        String result = translator.translate(ParsedUri.parse(new URI("sm://root/arg1/arg2")));
        assertThat(result, equalTo("root/arg1/arg2"));
        assertThat(scheme.isMethod1Invoked(), equalTo(true));
    }
}
