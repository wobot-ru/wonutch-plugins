package ru.wobot.sm.parse;

import org.apache.nutch.multipage.MultiElasticConstants;
import org.apache.nutch.multipage.dto.Page;
import org.springframework.social.vkontakte.api.*;
import ru.wobot.sm.core.Sources;
import ru.wobot.sm.core.mapping.PostProperties;
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Types;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.parse.AbstractParser;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.core.domain.PostIndex;
import ru.wobot.sm.core.url.UrlSchemaConstants;
import ru.wobot.sm.serialize.Serializer;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class Vk extends AbstractParser {
    @Override
    protected ParseResult parseProfile(URL url, String content) {
        final String urlString = url.toString();
        final String userDomain = url.getHost();
        final Map<String, String> parseMeta = new HashMap<>();
        final Map<String, String> contentMeta = new HashMap<>();

        VKontakteProfile profile = Serializer.getInstance().fromJson(content, VKontakteProfile.class);

        final HashMap<String, String> links = new HashMap<String, String>(3) {
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

        // fill parse metadata
        final String fullName = profile.getFirstName() + " " + profile.getLastName();
        parseMeta.put(ProfileProperties.SOURCE, Sources.VKONTAKTE);
        parseMeta.put(ProfileProperties.NAME, fullName);
        parseMeta.put(ProfileProperties.HREF, profile.getProfileURL());
        parseMeta.put(ProfileProperties.SM_PROFILE_ID, String.valueOf(profile.getId()));
        if (profile.getCity() != null) {
            parseMeta.put(ProfileProperties.CITY, String.valueOf(profile.getCity().getTitle()));
        }
        if (profile.getGender() != null) {
            parseMeta.put(ProfileProperties.GENDER, profile.getGender());
        }
        Counters counters = profile.getCounters();
        if (counters != null) {
            parseMeta.put(ProfileProperties.COVERAGE, String.valueOf(counters.getFollowers() + counters.getFriends()));
        }

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.PROFILE);
        return new ParseResult(urlString, fullName, content, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parseFriends(URL url, String content) {
        final String userDomain = url.getHost();
        final Map<String, String> parseMeta = new HashMap<>();
        final Map<String, String> contentMeta = new HashMap<>();

        String[] friendIds = Serializer.getInstance().fromJson(content, String[].class);

        Map<String, String> links = new HashMap<>(friendIds.length);
        for (String friendId : friendIds) {
            String friendHref = UrlSchemaConstants.VKONTAKTE + friendId;
            links.put(friendHref, friendId);
        }

        return new ParseResult(url.toString(), userDomain + "-friends", content, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parsePostsIndex(URL url, String content) {
        final String userDomain = url.getHost();
        final String urlString = url.toString();
        final Map<String, String> parseMeta = new HashMap<>();
        final Map<String, String> contentMeta = new HashMap<>();

        final int postsCount = Serializer.getInstance().fromJson(content, int.class);
        final int indexPageCount = postsCount / 100;

        final HashMap<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%08d", i);
            // generate link <a href='vk://user/index-posts/x100/00000001'>user-index-posts-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, userDomain + "-index-posts-x100-page-" + i);
        }
        return new ParseResult(urlString, userDomain + "-index-posts", content, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parsePostsIndexPage(URL url, String content) {
        final String userDomain = url.getHost();
        final String urlPrefix = UrlSchemaConstants.VKONTAKTE + userDomain + "/posts/";
        final Map<String, String> parseMeta = new HashMap<>();
        final Map<String, String> contentMeta = new HashMap<>();

        PostIndex postIndex = Serializer.getInstance().fromJson(content, PostIndex.class);

        final Map<String, String> links = new HashMap<>(postIndex.postIds.length);
        for (String id : postIndex.postIds) {
            links.put(urlPrefix + id, id);
        }
        return new ParseResult(url.toString(), userDomain, content, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parsePost(URL url, String content) {
        final String urlString = url.toString();
        final Map<String, String> parseMeta = new HashMap<>();
        final Map<String, String> contentMeta = new HashMap<>();

        Post post = Serializer.getInstance().fromJson(content, Post.class);
        final int indexPageCount = post.getComments().getCount() / 100;

        final HashMap<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%06d", i);
            // generate link <a href='vk://user/posts/x100/000001'>post-index-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, "post-index-x100-page-" + i);
        }

        parseMeta.put(PostProperties.SOURCE, Sources.VKONTAKTE);
        String ownerId = String.valueOf(post.getOwnerId());
        parseMeta.put(PostProperties.PROFILE_ID, ownerId);
        parseMeta.put(PostProperties.HREF, "http://vk.com/wall" + ownerId + "_" + post.getId()); //like http://vk.com/wall1_730207
        parseMeta.put(PostProperties.SM_POST_ID, String.valueOf(post.getId()));
        parseMeta.put(PostProperties.BODY, post.getText());
        //todo: replace to JodaTime
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        parseMeta.put(PostProperties.POST_DATE, dateFormat.format(post.getDate()));
        int involvement = post.getLikes().getCount() + post.getReposts().getCount() + post.getComments().getCount();
        parseMeta.put(PostProperties.INVOLVEMENT, String.valueOf(involvement));

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.POST);
        contentMeta.put(ContentMetaConstants.PARENT, "id" + post.getOwnerId());
        return new ParseResult(urlString, post.getText(), content, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parseCommentPage(URL url, String content) {
        final String userDomain = url.getHost();
        final String path = url.getPath();
        final String[] split = path.split("/");
        final int postId = Integer.parseInt(split[2]);
        final int page = Integer.parseInt(split[4]);
        final HashMap<String, String> links = new HashMap<>();
        final Map<String, String> parseMeta = new HashMap<>();
        final Map<String, String> contentMeta = new HashMap<String, String>() {{
            put(MultiElasticConstants.MULTI_PAGE, "true");
        }};

        //todo: теряется totalCount определится, насколько это нам важно
        CommentsResponse response = Serializer.getInstance().fromJson(content, CommentsResponse.class);

        final Page[] pages = new Page[response.getComments().size()];
        int i = 0;
        for (Comment comment : response.getComments()) {
            String commentUrl = UrlSchemaConstants.VKONTAKTE + userDomain + "/posts/" + postId + "/comments/" + comment.getId();
            Page commentPage = new Page(commentUrl, comment.getText(), Serializer.getInstance().toJson(comment));
            pages[i++] = commentPage;
        }

        return new ParseResult(url.toString(), userDomain + "|post=" + postId + "|page=" + page, Serializer.getInstance().toJson(pages), links, parseMeta, contentMeta);
    }
}
