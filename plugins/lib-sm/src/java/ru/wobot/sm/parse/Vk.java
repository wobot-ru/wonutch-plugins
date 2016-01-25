package ru.wobot.sm.parse;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.social.vkontakte.api.*;
import ru.wobot.sm.core.Sources;
import ru.wobot.sm.core.mapping.PostProperties;
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Types;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.meta.NutchDocumentMetaConstants;
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
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

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
            parseMeta.put(ProfileProperties.REACH, counters.getFollowers() + counters.getFriends());
        }

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.PROFILE);
        return new ParseResult(urlString, fullName, content, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parseFriends(URL url, String content) {
        final String userDomain = url.getHost();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

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
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

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
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

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
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        Post post = Serializer.getInstance().fromJson(content, Post.class);
        final int indexPageCount = post.getComments().getCount() / 100;

        final HashMap<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%06d", i);
            // generate link <a href='vk://user/posts/1/x100/000001'>post-index-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, "post-index-x100-page-" + i);
        }

        final String ownerProfile = UrlSchemaConstants.VKONTAKTE + "id" + post.getOwnerId();
        parseMeta.put(PostProperties.SOURCE, Sources.VKONTAKTE);
        parseMeta.put(PostProperties.PROFILE_ID, ownerProfile);
        parseMeta.put(PostProperties.HREF, "http://vk.com/wall" + post.getOwnerId() + "_" + post.getId()); //like http://vk.com/wall1_730207
        parseMeta.put(PostProperties.SM_POST_ID, post.getId());
        parseMeta.put(PostProperties.BODY, post.getText());
        //todo: replace to JodaTime
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        parseMeta.put(PostProperties.POST_DATE, dateFormat.format(post.getDate()));
        int engagement = post.getLikes().getCount() + post.getReposts().getCount() + post.getComments().getCount();
        parseMeta.put(PostProperties.ENGAGEMENT, engagement);
        parseMeta.put(PostProperties.IS_COMMENT, false);

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.POST);
        contentMeta.put(ContentMetaConstants.PARENT, ownerProfile);
        return new ParseResult(urlString, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parseCommentPage(URL url, String content) {
        final String user = url.getHost();
        final int userId = Integer.parseInt(user.substring(2));
        final String path = url.getPath();
        final String[] split = path.split("/");
        final int postId = Integer.parseInt(split[2]);
        final int page = Integer.parseInt(split[4]);
        final HashMap<String, String> links = new HashMap<>();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<String, Object>() {{
            put(ContentMetaConstants.MULTIPLE_PARSE_RESULT, true);
        }};

        CommentsResponse response = Serializer.getInstance().fromJson(content, CommentsResponse.class);
        //todo: replace to JodaTime
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        final ParseResult[] parseResults = new ParseResult[response.getComments().size()];
        int i = 0;
        for (final Comment comment : response.getComments()) {
            final String postUrl = UrlSchemaConstants.VKONTAKTE + user + "/posts/" + postId;
            final String commentOwnerProfile = UrlSchemaConstants.VKONTAKTE + "id" + comment.getFromId();
            links.put(commentOwnerProfile, "");

            String commentUrl = postUrl + "/comments/" + comment.getId();
            HashMap<String, Object> commentParseMeta = new HashMap<String, Object>() {{
                put(PostProperties.SOURCE, Sources.VKONTAKTE);
                put(PostProperties.PROFILE_ID, commentOwnerProfile);
                put(PostProperties.PARENT_POST_ID, postUrl);
                put(PostProperties.HREF, "http://vk.com/wall" + userId + "_" + postId + "?reply=" + comment.getId());
                put(PostProperties.SM_POST_ID, comment.getId());
                put(PostProperties.BODY, comment.getText());
                put(PostProperties.POST_DATE, dateFormat.format(comment.getDate()));
                //todo: is only one number?
                put(PostProperties.ENGAGEMENT, comment.getLikes().getCount());
                put(PostProperties.IS_COMMENT, true);
                put(NutchDocumentMetaConstants.DIGEST, DigestUtils.md5Hex(comment.toString()));
            }};

            HashMap<String, Object> commonCommentsContentMeta = new HashMap<String, Object>() {{
                put(ContentMetaConstants.PARENT, commentOwnerProfile);
                put(ContentMetaConstants.TYPE, Types.POST);
            }};
            parseResults[i++] = new ParseResult(commentUrl, new HashMap<String, String>(), commentParseMeta, commonCommentsContentMeta);
        }

        return new ParseResult(url.toString(), user + "|post=" + postId + "|page=" + page, Serializer.getInstance().toJson(parseResults), links, parseMeta, contentMeta);
    }
}
