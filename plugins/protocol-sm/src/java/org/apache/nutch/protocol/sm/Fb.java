package org.apache.nutch.protocol.sm;

import ru.wobot.sm.fetch.FbFetcher;
import ru.wobot.sm.core.fetch.SMFetcher;

public class Fb extends SMProtocol {
    @Override
    public SMFetcher createSMFetcher() {
        return new FbFetcher();
    }
}
