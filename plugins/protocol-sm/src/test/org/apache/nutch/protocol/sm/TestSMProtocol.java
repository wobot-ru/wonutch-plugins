package org.apache.nutch.protocol.sm;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.protocol.Content;
import org.junit.Test;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SMFetcher;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TestSMProtocol {
    private static SMFetcher fetcher = mock(SMFetcher.class);

    @Test
    public void shouldReturnProfileSMContent() throws IOException {
        //given
        SMProtocol protocol = new MockProtocol();
        protocol.setConf(new Configuration());

        given(fetcher.getProfiles(Collections.singletonList("1"))).willReturn(Collections.singletonList(new
                SMProfile("1", "name", "full name")));
        given(fetcher.getProfileData("1")).willReturn(new FetchResponse("Test data", new HashMap<String, Object>
                () {{
                put("Key", "Test Value");
            }}));

        // when
        Content content = protocol.getProtocolOutput(new Text("http://1"), null).getContent();
        String data = new String(content.getContent());

        // then
        assertThat(data, is(equalTo("Test data")));
        assertThat(content.getMetadata().get("Key"), is(equalTo("Test Value")));
    }

    private static class MockProtocol extends SMProtocol {
        @Override
        protected SMFetcher createSMFetcher() {
            return fetcher;
        }
    }
}
