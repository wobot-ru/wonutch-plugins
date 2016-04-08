package ru.wobot.uri;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;
import ru.wobot.uri.impl.ParsedUri;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UriTranslator_Primitive_Types_Test {
    static {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(new ConsoleAppender(
                new PatternLayout("%d{ISO8601} %-5p %c{2} - %m%n")));
    }

    @Test
    public void test() throws Exception {
        // given
        SchemaStub scheme = spy(SchemaStub.class);
        when(scheme.method1(123, 345, 678)).thenReturn(42);

        // when
        int result = new UriTranslator(scheme).translate(ParsedUri.parse("sm://123/345/?arg2=678"));

        // than
        verify(scheme).method1(123, 345, 678);
        assertThat(result, equalTo(42));
    }

    @Scheme("sm")
    public interface SchemaStub {
        @Path("{host}/{arg1}")
        int method1(@PathParam("host") int host, @PathParam("arg1") int arg1, @QueryParam("arg2") int arg2);
    }
}
