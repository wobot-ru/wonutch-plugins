package ru.wobot.vk;

class Credential {
    String accessToken;
    String clientSecret;

    public Credential(String accessToken, String clientSecret) {
        this.accessToken = accessToken;
        this.clientSecret = clientSecret;
    }
}
