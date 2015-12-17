package ru.wobot.vk;

import org.apache.nutch.multipage.dto.Page;
import org.springframework.social.vkontakte.api.Comment;
import org.springframework.social.vkontakte.api.CommentsResponse;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import ru.wobot.vk.dto.PostIndex;
import ru.wobot.vk.serialize.Builder;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
        if (UrlCheck.isPost(url)) {
            return createPostParse(urlString, content);
        }
        if (UrlCheck.isCommentPage(url)) {
            return createCommentPageParse(url, content);
        }
        throw new UnsupportedOperationException();
    }

    private static ParseResult createCommentPageParse(URL url, String content) {
        String userDomain = url.getHost();
        String path = url.getPath();
        String[] split = path.split("/");
        int postId = Integer.parseInt(split[2]);
        int page = Integer.parseInt(split[4]);

        //todo: теряется totalCount определится, насколько это нам важно
        CommentsResponse response = fromJson(content, CommentsResponse.class);
        Page[] pages = new Page[response.getComments().size()];
        int i = 0;
        for (Comment comment : response.getComments()) {
            String commentUrl = "http://" + userDomain + "/posts/" + postId + "/comments/" + comment.getId();
            Page commentPage = new Page(commentUrl, comment.getText(), toJson(comment));
            pages[i++] = commentPage;
        }
        return new ParseResult(url.toString(), userDomain + "|post=" + postId + "|page=" + page, toJson(pages), true);
    }

    private static ParseResult createPostParse(String urlString, String content) {
        Post post = fromJson(content, Post.class);

        int indexPageCount = post.getComments().getCount() / 100;
        HashMap<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%06d", i);
            // generate link <a href='http://user/posts/x100/000001'>post-index-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, "post-index-x100-page-" + i);
        }

        return new ParseResult(urlString, post.getText(), content, links);
    }

    private static ParseResult createProfileParse(final String userId, final String urlString, String content) {
        VKontakteProfile profile = fromJson(content, VKontakteProfile.class);

        HashMap<String, String> links = new HashMap<String, String>(2) {
            {
                put(urlString + "/friends", userId + "-friends"); // generate link <a href='http://user/friends'>user-friends</a>
                put(urlString + "/index-posts", userId + "-index-posts"); // generate link <a href='http://user/index-posts'>user-index-posts</a>
            }
        };

        return new ParseResult(urlString, getFullName(profile), content, links);
    }

    private static ParseResult createFriendsParse(String userId, String urlString, String content) {
        String[] friendIds = fromJson(content, String[].class);
        HashMap<String, String> links = new HashMap<>(friendIds.length);
        for (String friendId : friendIds) {
            String friendHref = "http://" + friendId;
            links.put(friendHref, friendId);
        }
        String title = userId + "-friends";
        return new ParseResult(urlString, title, content, links);
    }

    private static ParseResult createPostsIndexParse(String userId, String urlString, String content) {
        int postsCount = fromJson(content, int.class);

        int indexPageCount = postsCount / 100;
        HashMap<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%08d", i);
            // generate link <a href='http://user/index-posts/x100/00000001'>user-index-posts-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, userId + "-index-posts-x100-page-" + i);
        }

        String title = userId + "-index-posts";
        return new ParseResult(urlString, title, content, links);
    }

    private static ParseResult createPostsIndexPageParse(URL url, String content) {
        String userId = url.getHost();
        String urlPrefix = "http://" + userId + "/posts/";
        PostIndex postIndex = fromJson(content, PostIndex.class);

        Map<String, String> links = new HashMap<>(postIndex.postIds.length);
        for (long id : postIndex.postIds) {
            links.put(urlPrefix + id, String.valueOf(id));
        }

        return new ParseResult(url.toString(), userId, content, links);
    }

    private static String getFullName(VKontakteProfile user) {
        String name = user.getFirstName() + " " + user.getLastName();
        return name;
    }

    private static <T> T fromJson(String json, Class<T> classOfT) {
        return Builder.getGson().fromJson(json, classOfT);
    }
    private static String toJson(Object obj) {
        String json = Builder.getGson().toJson(obj);
        return json;
    }
}
