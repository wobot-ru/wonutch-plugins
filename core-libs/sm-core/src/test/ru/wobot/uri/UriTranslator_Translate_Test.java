package ru.wobot.uri;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.wobot.uri.impl.ParsedUri;
import ru.wobot.uri.stub.FtpScheme;
import ru.wobot.uri.stub.HttpsScheme;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UriTranslator_Translate_Test {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void add_when_translate_with_unknown_schema_should_throw_exception() throws URISyntaxException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("is schema not supported");
        new UriTranslator(new HttpsScheme()).translate(ParsedUri.parse(new URI("mailto:abc@abc.com")));
    }

    @Test
    public void add_when_translate_root_domain_than_defined_method_should_be_invoked() throws URISyntaxException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        final FtpScheme ftpScheme = new FtpScheme();
        new UriTranslator(ftpScheme).translate(ParsedUri.parse(new URI("ftp://root")));
        assertThat(ftpScheme.isRootInvoked(), is(true));
    }
}
