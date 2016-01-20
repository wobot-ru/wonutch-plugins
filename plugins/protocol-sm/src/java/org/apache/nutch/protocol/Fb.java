package org.apache.nutch.protocol;

import ru.wobot.sm.fetch.FbService;
import ru.wobot.sm.core.fetch.SMService;

public class Fb extends SMProtocol {
    @Override
    public SMService createSMService() {
        return new FbService();
    }
}
