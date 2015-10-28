package ru.wobot.vk;

import java.net.URL;

public class UrlCheck {
    public static boolean isProfile(URL url) {
        String path = url.getPath();
        return path.equals("") || path.equals("/");
    }
    public static boolean isFriends(URL url) {
        String path = url.getPath().toLowerCase();
        return path.equals("/friends") || path.equals("/friends/");
    }
}
