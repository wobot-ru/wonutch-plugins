package ru.wobot.parsers;

import org.apache.nutch.multipage.dto.Page;
import org.springframework.social.vkontakte.api.Comment;
import org.springframework.social.vkontakte.api.CommentsResponse;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import ru.wobot.smm.core.dto.ParseResult;
import ru.wobot.smm.core.dto.PostIndex;
import ru.wobot.smm.core.UrlSchemaConstants;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Vk extends BaseParsable {
    @Override
    protected ParseResult parseProfile(URL url, String content) {
        final String urlString = url.toString();
        final String userDomain = url.getHost();

        VKontakteProfile profile = fromJson(content, VKontakteProfile.class);

        HashMap<String, String> links = new HashMap<String, String>(3) {
            {
                // generate link <a href='vk://user/friends'>user-friends</a>
                put(urlString + "/friends", userDomain + "-friends");

                // generate link <a href='vk://user/index-posts'>user-index-posts</a>
                put(urlString + "/index-posts", userDomain + "-index-posts");
            }
        };
        if (profile.getFacebook() != null) {
            links.put(UrlSchemaConstants.FACEBOOK + profile.getFacebook(), userDomain + "-facebook");
        }
        return new ParseResult(urlString, profile.getFirstName() + " " + profile.getLastName(), content, links);
    }

    @Override
    protected ParseResult parseFriends(URL url, String content) {
        String userDomain = url.getHost();

        String[] friendIds = fromJson(content, String[].class);

        HashMap<String, String> links = new HashMap<>(friendIds.length);
        for (String friendId : friendIds) {
            String friendHref = UrlSchemaConstants.VKONTAKTE + friendId;
            links.put(friendHref, friendId);
        }
        return new ParseResult(url.toString(), userDomain + "-friends", content, links);
    }

    @Override
    protected ParseResult parsePostsIndex(URL url, String content) {
        String userDomain = url.getHost();
        String urlString = url.toString();

        int postsCount = fromJson(content, int.class);
        int indexPageCount = postsCount / 100;

        HashMap<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%08d", i);
            // generate link <a href='vk://user/index-posts/x100/00000001'>user-index-posts-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, userDomain + "-index-posts-x100-page-" + i);
        }
        return new ParseResult(urlString, userDomain + "-index-posts", content, links);
    }

    @Override
    protected ParseResult parsePostsIndexPage(URL url, String content) {
        String userDomain = url.getHost();
        String urlPrefix = UrlSchemaConstants.VKONTAKTE + userDomain + "/posts/";

        PostIndex postIndex = fromJson(content, PostIndex.class);

        Map<String, String> links = new HashMap<>(postIndex.postIds.length);
        for (String id : postIndex.postIds) {
            links.put(urlPrefix + id, id);
        }
        return new ParseResult(url.toString(), userDomain, content, links);
    }

    @Override
    protected ParseResult parsePost(URL url, String content) {
        String urlString = url.toString();

        Post post = fromJson(content, Post.class);
        int indexPageCount = post.getComments().getCount() / 100;

        HashMap<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%06d", i);
            // generate link <a href='vk://user/posts/x100/000001'>post-index-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, "post-index-x100-page-" + i);
        }
        return new ParseResult(urlString, post.getText(), content, links);
    }

    @Override
    protected ParseResult parseCommentPage(URL url, String content) {
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
            String commentUrl = UrlSchemaConstants.VKONTAKTE + userDomain + "/posts/" + postId + "/comments/" + comment.getId();
            Page commentPage = new Page(commentUrl, comment.getText(), toJson(comment));
            pages[i++] = commentPage;
        }
        return new ParseResult(url.toString(), userDomain + "|post=" + postId + "|page=" + page, toJson(pages), true);
    }
}
