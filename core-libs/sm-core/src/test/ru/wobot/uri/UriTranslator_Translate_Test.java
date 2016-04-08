package ru.wobot.uri;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.wobot.uri.impl.ParsedUri;
import ru.wobot.uri.stub.FtpScheme;
import ru.wobot.uri.stub.HttpsScheme;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class UriTranslator_Translate_Test {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void add_when_translate_with_unknown_schema_should_throw_exception() throws URISyntaxException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("Schema [fb] is  not supported");
        new UriTranslator(new HttpsScheme()).translate(ParsedUri.parse(new URI("fb://123456")));
    }

    @Test
    public void add_when_translate_root_domain_than_defined_method_should_be_invoked() throws URISyntaxException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        final FtpScheme ftpScheme = spy(new FtpScheme());
        new UriTranslator(ftpScheme).translate(ParsedUri.parse(new URI("ftp://root")));
        verify(ftpScheme).root();
    }
}
