package org.apache.nutch.protocol;

import ru.wobot.sm.core.SMService;
import ru.wobot.sm.VKService;

public class Vk extends SMProtocol {
    @Override
    SMService createSMService() {
        return new VKService();
    }
}