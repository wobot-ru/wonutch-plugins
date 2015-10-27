package org.apache.nutch.protocol.vk;

import org.springframework.social.vkontakte.api.impl.VKontakteTemplate;

public enum Proxy {
    INSTANCE;
    public static VKontakteTemplate getInctance() {
        return new VKontakteTemplate("2ff5fff8d49ffa2da2875c99faf61399f5480ee10a2d70dde44027b9b248eae6a014d7689f7d0f216ab00", "ZydztdkkmEDkvBNsulQt");
    }
}