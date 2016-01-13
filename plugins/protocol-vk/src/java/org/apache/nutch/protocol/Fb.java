package org.apache.nutch.protocol;

import ru.wobot.smm.core.SMService;
import ru.wobot.vk.VKService;

public class Fb extends SMProtocol {
    @Override
    SMService createSMService() {
        return new VKService();
    }
}
