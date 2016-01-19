package ru.wobot.sm.core.domain;

public class Response {
    public static final String MIME_TYPE = "application/json";

    public Response(String url, byte[] data, long fetchTime) {
        this.url = url;
        this.data = data;
        this.fetchTime = fetchTime;
    }

    public String url;
    public byte[] data;
    public long fetchTime;
}
