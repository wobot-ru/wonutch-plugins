package ru.wobot.vk;

import com.google.gson.Gson;
import org.springframework.social.vkontakte.api.VKontakteProfile;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Parser {
    public static ParseResult parse(String urlString, byte[] data) throws MalformedURLException {
        URL url = new URL(urlString);
        String userId = url.getHost();
        String content = new String(data, StandardCharsets.UTF_8);

        if (UrlCheck.isProfile(url)) {
            return createProfileParse(userId, urlString, content);
        }
        if (UrlCheck.isFriends(url)) {
            return createFriendsParse(userId, urlString, content);
        }
        if (UrlCheck.isPostsIndex(url)) {
            return createPostsIndexParse(userId, urlString, content);
        }
        if (UrlCheck.isPostsIndexPage(url)) {
            return createPostsIndexPageParse(url, content);
        }
        throw new UnsupportedOperationException();
    }

    private static ParseResult createProfileParse(String userId, String urlString, String content) {
        Gson gson = new Gson();
        VKontakteProfile profile = gson.fromJson(content, VKontakteProfile.class);

        HashMap<String, String> links = new HashMap<String, String>(2) {
            {
                put(urlString + "/friends", userId + "-friends"); // generate link <a href='http://user/friends'>user-friends</a>
                put(urlString + "/index-posts", userId + "-index-posts"); // generate link <a href='http://user/index-posts'>user-index-posts</a>
            }
        };

        return new ParseResult(urlString, getFullName(profile), content, links);
    }

    private static ParseResult createFriendsParse(String userId, String urlString, String content) {
        Gson gson = new Gson();
        VKontakteProfile[] friends = gson.fromJson(content, VKontakteProfile[].class);
        HashMap<String, String> links = new HashMap<>(friends.length);
        for (VKontakteProfile friend : friends) {
            String friendHref = "http://" + friend.getScreenName();
            links.put(friendHref, getFullName(friend));
        }
        String title = userId + "-friends";
        return new ParseResult(urlString, title, content, links);
    }

    private static ParseResult createPostsIndexParse(String userId, String urlString, String content) {
        Gson gson = new Gson();
        int postsCount = gson.fromJson(content, int.class);

        int indexPageCount = postsCount / 100;
        HashMap<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%010d", i);
            // generate link <a href='http://user/index-posts/x100/0000000001'>user-index-posts-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, userId + "-index-posts-x100-page-" + i);
        }

        String title = userId + "-index-posts";
        return new ParseResult(urlString, title, content, links);
    }

    private static ParseResult createPostsIndexPageParse(URL url, String content) {
        String userId = url.getHost();
        HashMap<String, String> links = new HashMap<>();
        return new ParseResult(url.toString(), userId, content, links);
    }

    private static String getFullName(VKontakteProfile user) {
        String name = user.getFirstName() + " " + user.getLastName();
        return name;
    }
}
