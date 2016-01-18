package org.apache.nutch.protocol;

import ru.wobot.sm.FbService;
import ru.wobot.sm.core.service.SMService;

public class Fb extends SMProtocol {
    @Override
    public SMService createSMService() {
        return new FbService();
    }
}
