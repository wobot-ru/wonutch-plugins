package org.apache.nutch.protocol.sm;

import ru.wobot.sm.core.fetch.SMFetcher;
import ru.wobot.sm.fetch.VKService;

public class Vk extends SMProtocol {
    @Override
    public SMFetcher createSMService() {
        return new VKService();
    }
}