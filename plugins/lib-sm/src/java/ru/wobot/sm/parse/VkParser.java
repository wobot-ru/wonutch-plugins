package ru.wobot.sm.parse;

import com.google.gson.reflect.TypeToken;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.social.vkontakte.api.Comment;
import org.springframework.social.vkontakte.api.CommentsResponse;
import org.springframework.social.vkontakte.api.Counters;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import ru.wobot.sm.core.mapping.Sources;
import ru.wobot.sm.core.api.VkApiTypes;
import ru.wobot.sm.core.mapping.PostProperties;
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Types;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.core.parse.Parser;
import ru.wobot.sm.serialize.Serializer;

import java.lang.reflect.Type;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class VkParser implements Parser {
    private static final String DIGEST = "digest";

    @Override
    public ParseResult parse(URI uri, String content, String apiType, String apiVersion) {
        switch (apiType) {
            case VkApiTypes.PROFILE:
                return parseProfile(uri, content);
            case VkApiTypes.FRIEND_LIST_OF_ID:
                return parseFriends(uri, content);
            case VkApiTypes.POST:
                return parsePost(uri, content);
            case VkApiTypes.POST_COUNT:
                return parsePostsIndex(uri, content);
            case VkApiTypes.POST_BULK:
                return parsePostsIndexPage(uri, content);
            case VkApiTypes.COMMENT_BULK:
                return parseCommentPage(uri, content);
        }
        throw new UnsupportedOperationException("Parser for this content not found.");
    }

    protected ParseResult parseProfile(URI uri, String content) {
        final String urlString = uri.toString();
        final String userDomain = uri.getHost();
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
        String facebook = profile.getFacebook();
        if (facebook != null) {
            //hack: user http://vk.com/id16812401
            // contains facebook link to http://facebook.com/app_scoped_user_id/100008451725336
            // than VK API return json property facebook: '+100008451725336'
            // in that case we trim it
            if (facebook.startsWith("+"))
                facebook = facebook.substring(1);

            String anchor = profile.getFacebookName();
            links.put("https://www.facebook.com/profile.php?id=" + facebook, anchor == null ? userDomain + "-facebook" : anchor);
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

    protected ParseResult parseFriends(URI uri, String content) {
        final String userDomain = uri.getHost();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        String[] friendIds = Serializer.getInstance().fromJson(content, String[].class);

        Map<String, String> links = new HashMap<>(friendIds.length);
        for (String friendId : friendIds) {
            String friendHref = Sources.VKONTAKTE + "://" + friendId;
            links.put(friendHref, friendId);
        }

        return new ParseResult(uri.toString(), userDomain + "-friends", content, links, parseMeta, contentMeta);
    }

    protected ParseResult parsePostsIndex(URI uri, String content) {
        final String userDomain = uri.getHost();
        final String urlString = uri.toString();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        final int postsCount = Serializer.getInstance().fromJson(content, int.class);
        final int indexPageCount = postsCount / 100;
        final HashMap<String, String> links = new HashMap<>(indexPageCount);
        final boolean isAuth = uri.getQuery() != null && uri.getQuery().contains("auth");
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%08d", i);
            // generate link <a href='vk://user/index-posts/x100/00000001'>user-index-posts-x100-page-1</a>
            final String link;

            if (isAuth)
                link = uri.getScheme() + "://" + uri.getHost() + uri.getPath() + "/x100/" + blockNumber + "?" + uri.getQuery();
            else
                link = uri.getScheme() + "://" + uri.getHost() + uri.getPath() + "/x100/" + blockNumber;
            links.put(link, userDomain + "-index-posts-x100-page-" + i);
        }
        return new ParseResult(urlString, userDomain + "-index-posts", content, links, parseMeta, contentMeta);
    }

    protected ParseResult parsePostsIndexPage(URI uri, String content) {
        final String userDomain = uri.getHost();
        final String urlPrefix = Sources.VKONTAKTE + "://" + userDomain + "/posts/";
        final boolean isAuth = uri.getQuery() != null && uri.getQuery().contains("auth");
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> commonContentMeta = new HashMap<String, Object>() {{
            put(ContentMetaConstants.MULTIPLE_PARSE_RESULT, true);
        }};

        Type collectionType = new TypeToken<VKArray<Post>>() {
        }.getType();
        VKArray<Post> posts = Serializer.getInstance().fromJson(content, collectionType);

        Map<String, String> links = null;
        ParseResult[] parseResults = null;
        int i = 0;
        if (posts != null && posts.getItems() != null) {
            parseResults = new ParseResult[posts.getItems().size()];
            links = new HashMap<>(posts.getItems().size());
            for (Post post : posts.getItems()) {
                final int commentPageCount = post.getComments().getCount() / 100;
                for (long block = 0; block <= commentPageCount; block++) {
                    String blockNumber = String.format("%06d", block);

                    if (isAuth)
                        links.put(urlPrefix + post.getId() + "/x100/" + blockNumber + "?auth", "comment-index-x100-page-" + block);
                    else
                        links.put(urlPrefix + post.getId() + "/x100/" + blockNumber, "comment-index-x100-page-" + block);
                }
                Map<String, Object> postContent = new HashMap<>();
                Map<String, Object> postParse = new HashMap<>();

                final String ownerProfile = Sources.VKONTAKTE + "://" + "id" + post.getOwnerId();
                postParse.put(PostProperties.SOURCE, Sources.VKONTAKTE);
                postParse.put(PostProperties.PROFILE_ID, ownerProfile);
                postParse.put(PostProperties.HREF, "http://vk.com/wall" + post.getOwnerId() + "_" + post.getId()); //like http://vk.com/wall1_730207
                postParse.put(PostProperties.SM_POST_ID, post.getId());
                postParse.put(PostProperties.BODY, post.getText());
                //todo: replace to JodaTime
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                postParse.put(PostProperties.POST_DATE, dateFormat.format(post.getDate()));
                int engagement = post.getLikes().getCount() + post.getReposts().getCount() + post.getComments().getCount();
                postParse.put(PostProperties.ENGAGEMENT, engagement);
                postParse.put(PostProperties.IS_COMMENT, false);

                // fill content metadata
                postContent.put(ContentMetaConstants.TYPE, Types.POST);
                postContent.put(ContentMetaConstants.PARENT, ownerProfile);
                postContent.put(DIGEST, DigestUtils.md5Hex(post.toString()));
                parseResults[i++] = new ParseResult(urlPrefix + post.getId(), new HashMap<String, String>(), postParse, postContent);
            }
        }
        return new ParseResult(uri.toString(), userDomain, Serializer.getInstance().toJson(parseResults), (links
                == null ? new HashMap<String, String>() : links), parseMeta, commonContentMeta);
    }

    protected ParseResult parsePost(URI uri, String content) {
        final String urlString = uri.toString();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        Post post = Serializer.getInstance().fromJson(content, Post.class);
        final int indexPageCount = post.getComments().getCount() / 100;

        final Map<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%06d", i);
            // generate link <a href='vk://user/posts/1/x100/000001'>post-index-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, "post-index-x100-page-" + i);
        }

        final String ownerProfile = Sources.VKONTAKTE + "://" + "id" + post.getOwnerId();
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
        contentMeta.put(DIGEST, DigestUtils.md5Hex(post.toString()));
        return new ParseResult(urlString, links, parseMeta, contentMeta);
    }

    protected ParseResult parseCommentPage(URI uri, String content) {
        final String user = uri.getHost();
        final int userId = Integer.parseInt(user.substring(2));
        final String path = uri.getPath();
        final String[] split = path.split("/");
        final int postId = Integer.parseInt(split[2]);
        final int page = Integer.parseInt(split[4]);
        final Map<String, String> links = new IdentityHashMap<>();
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
            final String postUrl = Sources.VKONTAKTE + "://" + user + "/posts/" + postId;
            final String commentOwnerProfile = Sources.VKONTAKTE + "://id" + comment.getFromId();
            links.put(commentOwnerProfile, "");

            String commentUrl = postUrl + "/comments/" + comment.getId();
            Map<String, Object> commentParse = new HashMap<String, Object>() {{
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
                put(DIGEST, DigestUtils.md5Hex(comment.toString()));
            }};

            Map<String, Object> commentContentMeta = new HashMap<String, Object>() {{
                put(ContentMetaConstants.PARENT, commentOwnerProfile);
                put(ContentMetaConstants.TYPE, Types.POST);
            }};
            parseResults[i++] = new ParseResult(commentUrl, new HashMap<String, String>(), commentParse, commentContentMeta);
        }

        return new ParseResult(uri.toString(), user + "|post=" + postId + "|page=" + page, Serializer.getInstance().toJson(parseResults), links, parseMeta, contentMeta);
    }
}
