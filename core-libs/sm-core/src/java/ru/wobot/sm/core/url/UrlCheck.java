package ru.wobot.sm.core.url;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlCheck {
    private static final Pattern POST = Pattern.compile("/posts/\\d+");
    private static final Pattern POSTS_INDEX_PAGE = Pattern.compile("/index-posts/x100/\\d+");
    private static final Pattern COMMENTS_PAGE = Pattern.compile("/posts/[\\d_:]+/x100/\\d+(\\?)*");

    public static boolean isProfile(URL url) {
        String path = url.getPath();
        return path.isEmpty() || path.equals("/");
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
        Matcher matcher = POSTS_INDEX_PAGE.matcher(path);
        return matcher.matches();
    }

    public static boolean isPost(URL url) {
        String path = url.getPath().toLowerCase();
        Matcher matcher = POST.matcher(path);
        return matcher.matches();
    }

    public static boolean isCommentPage(URL url) {
        String path = url.getPath().toLowerCase();
        Matcher matcher = COMMENTS_PAGE.matcher(path);
        return matcher.matches();
    }
}
