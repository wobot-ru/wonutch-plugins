package ru.wobot.vk.dto;

import org.apache.commons.codec.digest.DigestUtils;

public class Page {
    public final String url;
    public final String content;
    public final String title;
    public final String digest;

    public Page(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
        //todo: продумать как зделать по уму. сейчас у нас content содержит title. но в общем случае это не так.
        this.digest = DigestUtils.md5Hex(title + content);
    }
}
