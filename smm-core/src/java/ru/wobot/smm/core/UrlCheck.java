package ru.wobot.smm.core;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlCheck {
    private static final Pattern posts = Pattern.compile("/posts/\\d+");
    private static final Pattern postsIndexPage = Pattern.compile("/index-posts/x100/\\d+");
    private static final Pattern commentPage = Pattern.compile("/posts/\\d+/x100/\\d+");

    public static boolean isProfile(URL url) {
        String path = url.getPath();
        return path.equals("") || path.equals("/");
    }

    public static boolean isFriends(URL url) {
        String path = url.getPath().toLowerCase();
        return path.equals("/friends") || path.equals("/friends/");
    }

    public static boolean isPostsIndex(URL url) {
        String path = url.getPath().toLowerCase();
        return path.equals("/index-posts") || path.equals("/index-posts/");
    }

    public static boolean isPostsIndexPage(URL url) {
        String path = url.getPath().toLowerCase();
        Matcher matcher = postsIndexPage.matcher(path);
        return matcher.matches();
    }

    public static boolean isPost(URL url) {
        String path = url.getPath().toLowerCase();
        Matcher matcher = posts.matcher(path);
        return matcher.matches();
    }

    public static boolean isCommentPage(URL url) {
        String path = url.getPath().toLowerCase();
        Matcher matcher = commentPage.matcher(path);
        return matcher.matches();
    }
}
