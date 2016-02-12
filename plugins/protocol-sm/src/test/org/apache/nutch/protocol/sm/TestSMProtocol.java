package org.apache.nutch.protocol.sm;

import org.junit.Ignore;
import org.junit.Test;
import ru.wobot.sm.fetch.FbFetcher;

import java.io.IOException;

import static org.mockito.Mockito.mock;

public class TestSMProtocol {
    private static FbFetcher fetcher = mock(FbFetcher.class);

    @Test
    @Ignore
    public void shouldReturnProfileSMContent() throws IOException {
       //TODO Think about how to test protocol
    }
}
