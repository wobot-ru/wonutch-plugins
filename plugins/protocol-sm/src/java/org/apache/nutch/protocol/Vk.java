package org.apache.nutch.protocol;

import ru.wobot.sm.core.fetch.SMService;
import ru.wobot.sm.fetch.VKService;

public class Vk extends SMProtocol {
    @Override
    public SMService createSMService() {
        return new VKService();
    }
}