package ru.wobot.vk;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Kviz on 10/27/2015.
 */
public class UrlCheck {
    public static boolean isProfile(String url) throws MalformedURLException {
        return isProfile(new URL(url));
    }
    public static boolean isFriends(String url) throws MalformedURLException {
        return isFriends(new URL(url));
    }
    public static boolean isProfile(URL url) {
        String path = url.getPath();
        return path.equals("") || path.equals("/");
    }
    public static boolean isFriends(URL url) {
        String path = url.getPath().toLowerCase();
        return path.equals("/friends") || path.equals("/friends/");
    }
}
