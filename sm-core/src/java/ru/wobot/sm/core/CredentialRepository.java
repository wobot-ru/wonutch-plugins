package ru.wobot.sm.core;

import org.apache.hadoop.conf.Configuration;

/**
 * Created by Leon Misakyan on 04.12.2015.
 * Represents repository, maintaining pool of API bindings, authorized on behalf of a specific user
 */
public interface CredentialRepository {
    void setConf(Configuration conf);

    Credential getInstance();
}
