package org.apache.nutch.protocol.sm;

import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.auth.Proxy;
import ru.wobot.sm.fetch.FbFetcher;
import ru.wobot.sm.core.fetch.SMFetcher;

public class Fb extends SMProtocol {
    @Override
    public SMFetcher createSMFetcher() {
        CredentialRepository repository = Proxy.INSTANCE;
        repository.setConf(getConf());
        return new FbFetcher(repository);
    }
}
