package org.apache.nutch.protocol.sm;

import ru.wobot.sm.core.fetch.SMFetcher;
import ru.wobot.sm.fetch.VKFetcher;

public class Vk extends SMProtocol {
    @Override
    public SMFetcher createSMFetcher() {
        return new VKFetcher();
    }
}