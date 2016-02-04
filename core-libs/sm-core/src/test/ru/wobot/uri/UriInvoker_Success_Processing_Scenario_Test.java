package ru.wobot.uri;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.wobot.uri.stub.FtpScheme;
import ru.wobot.uri.stub.HttpsScheme;

import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UriInvoker_Success_Processing_Scenario_Test {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void add_when_process_with_unknown_schema_should_throw_exception() throws URISyntaxException {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("is schema not supported");
        new UriInvoker(new HttpsScheme(), new FtpScheme()).process("mailto:abc@abc.com");
    }

    @Test
    public void add_when_process_root_domain_than_defined_method_should_be_invoked() throws URISyntaxException {
        final boolean[] invoked = new boolean[1];
        new UriInvoker(new FtpScheme() {
            @Path("/")
            public Object root() {
                invoked[0] = true;
                return new Object();
            }
        }).process("ftp://root");
        assertThat(invoked[0], is(true));
    }
}
