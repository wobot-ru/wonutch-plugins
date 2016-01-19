package ru.wobot.sm.core.auth;

import org.apache.hadoop.conf.Configuration;

/**
 * Represents repository, maintaining pool of API bindings, authorized on behalf of a specific user
 */
public interface CredentialRepository {
    void setConf(Configuration conf);

    Credential getInstance();
}
