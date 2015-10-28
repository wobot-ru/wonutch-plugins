package ru.wobot.vk;

public class VkResponse {
    public static final String mimeType = "application/json";

    public VkResponse(String url, byte[] data, long fetchTime) {
        this.url = url;
        this.data = data;
        this.fetchTime = fetchTime;
    }

    public String url;
    public byte[] data;
    public long fetchTime;
}
