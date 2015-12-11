package ru.wobot.vk;

import org.apache.hadoop.conf.Configuration;
import org.springframework.social.vkontakte.api.impl.VKontakteTemplate;

/**
 * Created by Leon Misakyan on 04.12.2015.
 * Represents repository, maintaining pool of API bindings, authorized on behalf of a specific user
 */
public interface ApiBindingRepository {
    VKontakteTemplate getInstance();
    void setConf(Configuration conf);
}
