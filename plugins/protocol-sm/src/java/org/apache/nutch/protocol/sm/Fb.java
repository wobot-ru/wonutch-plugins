package org.apache.nutch.protocol.sm;

import ru.wobot.sm.fetch.FbService;
import ru.wobot.sm.core.fetch.SMFetcher;

public class Fb extends SMProtocol {
    @Override
    public SMFetcher createSMService() {
        return new FbService();
    }
}
