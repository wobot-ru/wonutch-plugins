package ru.wobot.sm.parse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.social.UncategorizedApiException;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.Post;
import org.springframework.social.facebook.api.impl.PagedListUtils;
import org.springframework.social.facebook.api.impl.json.FacebookModule;
import ru.wobot.sm.core.mapping.Sources;
import ru.wobot.sm.core.api.FbApiTypes;
import ru.wobot.sm.core.mapping.PostProperties;
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Types;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.core.parse.Parser;
import ru.wobot.sm.serialize.Serializer;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class FbParser implements Parser {
    private static final String DIGEST = "digest";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FbParser() {
        objectMapper.registerModule(new FacebookModule());
    }

    @Override
    public ParseResult parse(URI uri, String content, String apiType, String apiVersion) {
        switch (apiType) {
            case FbApiTypes.PROFILE:
                return parseProfile(uri, content);
            case FbApiTypes.FRIEND_LIST_OF_ID:
                return parseFriends(uri, content);
            case FbApiTypes.POST_BULK:
                return parsePostsIndexPage(uri, content);
            case FbApiTypes.COMMENT_BULK:
                return parseCommentPage(uri, content);
        }
        throw new UnsupportedOperationException("Parser for this content not found.");
    }

    public ParseResult parseProfile(URI uri, String content) {
        String urlString = uri.toString();
        String userDomain = uri.getHost();
        Map<String, Object> parseMeta = new HashMap<>();
        Map<String, Object> contentMeta = new HashMap<>();
        if (content == null || content.isEmpty())
            return new ParseResult(urlString, userDomain, content, new HashMap<String, String>(), parseMeta,
                    contentMeta);
        JsonNode profile;
        try {
            profile = objectMapper.readValue(content, JsonNode.class);
        } catch (IOException e) {
            throw new UncategorizedApiException("facebook", "Error deserializing profile [" + userDomain + "]", e);
        }

        String userId = profile.get("id").asText();
        Map<String, String> links = new HashMap<>();

        // generate link <a href='fb://{user}/friends'>user-friends</a>
        links.put(Sources.FACEBOOK + "://" + userId + "/friends", userId + "-friends");

        // generate link <a href='fb://{user}/index-posts/x100/00000000'>user-index-posts-x100-page-0</a>
        links.put(Sources.FACEBOOK + "://" + userId + "/index-posts/x100/00000000", userId + "-index-posts-x100-page-0");

        // fill parse metadata
        final String fullName = profile.get("name").asText();
        parseMeta.put(ProfileProperties.SOURCE, Sources.FACEBOOK);
        parseMeta.put(ProfileProperties.NAME, fullName);
        parseMeta.put(ProfileProperties.HREF, profile.get("link").asText());
        parseMeta.put(ProfileProperties.SM_PROFILE_ID, userId);
        parseMeta.put(ProfileProperties.REACH, profile.get("likes") == null ? 1 : profile.get("likes").asText());  //default reach is 1 IMHO

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.PROFILE);
        return new ParseResult(urlString, fullName, content, links, parseMeta, contentMeta);
    }

    protected ParseResult parseFriends(URI uri, String content) {
        String userDomain = uri.getHost();
        Map<String, Object> parseMeta = new HashMap<>();
        Map<String, Object> contentMeta = new HashMap<>();

        String[] friendIds;
        try {
            friendIds = objectMapper.readValue(content, String[].class);
        } catch (IOException e) {
            throw new UncategorizedApiException("facebook", "Error deserializing friend IDs for user [" + userDomain + "]", e);
        }

        Map<String, String> links = new HashMap<>(friendIds.length);
        for (String friendId : friendIds) {
            // generate link <a href='fb://{user}'>user</a>
            links.put(Sources.FACEBOOK + "://" + friendId, friendId);
        }

        return new ParseResult(uri.toString(), userDomain + "-friends", content, links, parseMeta, contentMeta);
    }


    protected ParseResult parsePostsIndexPage(URI uri, String content) {
        String userDomain = uri.getHost();
        String urlPrefix = Sources.FACEBOOK + "://" + userDomain + "/posts/";
        Map<String, Object> contentMeta = new HashMap<String, Object>() {{
            put(ContentMetaConstants.MULTIPLE_PARSE_RESULT, true);
        }};

        List<Post> posts;
        JsonNode dataNode;
        PagingParameters nextPage;
        try {
            JsonNode node = objectMapper.readValue(content, JsonNode.class);
            dataNode = node.get("data");
            JsonNode pagingNode = node.get("paging");
            posts = objectMapper.readValue(dataNode.toString(), new TypeReference<List<Post>>() {
            });
            nextPage = PagedListUtils.getPagedListParameters(pagingNode, "next");
        } catch (IOException e) {
            throw new UncategorizedApiException("facebook", "Error deserializing feed for user [" + userDomain +
                    "]", e);
        }

        Map<String, String> links = null;
        ParseResult[] parseResults = null;
        if (posts != null && posts.size() != 0) {
            parseResults = new ParseResult[posts.size()];
            links = new HashMap<>(posts.size() * 2 + 1); //first comments page and author of each post (if author not this page) and optional next page
            if (nextPage != null)
                // generate link <a href='fb://{user}/index-posts/x100/{until}'>{user}-index-posts-x100-page-{until}</a>
                links.put(Sources.FACEBOOK + "://" + userDomain + "/index-posts/x100/" + nextPage.getUntil(),
                        userDomain + "-index-posts-x100-page-" + nextPage.getUntil());
            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);
                String postId = post.getId();
                JsonNode rawPost = dataNode.get(i);

                // generate link <a href='fb://{user}/posts/{post}/x100/0'>{post}-comments-index-x100-page-0</a>
                links.put(urlPrefix + postId + "/x100/0", postId + "-comments-index-x100-page-0");
                if (!post.getFrom().getId().equals(userDomain)) {
                    // generate link <a href='fb://{user}'>{user}</a>
                    links.put(Sources.FACEBOOK + "://" + post.getFrom().getId(), "");
                }

                Map<String, Object> postParse = new HashMap<>();
                Map<String, Object> postContent = new HashMap<>();

                postParse.put(PostProperties.SOURCE, Sources.FACEBOOK);
                postParse.put(PostProperties.PROFILE_ID, Sources.FACEBOOK + "://" + post.getFrom().getId());
                postParse.put(PostProperties.HREF, "https://www.facebook.com/" + userDomain + "/posts/" +
                        postId.substring(postId.lastIndexOf('_') + 1));
                postParse.put(PostProperties.SM_POST_ID, postId);
                postParse.put(PostProperties.BODY, post.getMessage() == null ? post.getDescription() : post.getMessage());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                postParse.put(PostProperties.POST_DATE, dateFormat.format(post.getCreatedTime()));
                // this nodes present not for all posts
                int engagement = 0;
                if (rawPost.get("likes") != null)
                    engagement = rawPost.get("likes").get("summary").get("total_count").asInt();
                if (rawPost.get("comments") != null)
                    engagement += rawPost.get("comments").get("summary").get("total_count").asInt();
                engagement += post.getShares();
                postParse.put(PostProperties.ENGAGEMENT, engagement);
                postParse.put(PostProperties.IS_COMMENT, false);

                // fill content metadata
                postContent.put(ContentMetaConstants.TYPE, Types.POST);
                postContent.put(ContentMetaConstants.PARENT, Sources.FACEBOOK + "://" + post.getFrom().getId());
                postContent.put(DIGEST, DigestUtils.md5Hex(post.toString()));
                parseResults[i] = new ParseResult(urlPrefix + postId, new HashMap<String, String>(), postParse, postContent);
            }
        }
        return new ParseResult(uri.toString(), userDomain, Serializer.getInstance().toJson(parseResults), (links
                == null ? new HashMap<String, String>() : links), new HashMap<String, Object>(), contentMeta);
    }

    protected ParseResult parseCommentPage(URI uri, String content) {
        final String userDomain = uri.getHost();
        final String[] path = uri.getPath().split("/");
        final String parentMessageId = path[2]; // may be post or comment (reply comments have comment as a parent, not post)

        Map<String, Object> parseMeta = new HashMap<>();
        Map<String, Object> contentMeta = new HashMap<String, Object>() {{
            put(ContentMetaConstants.MULTIPLE_PARSE_RESULT, true);
        }};

        JsonNode comments;
        String after = null;
        try {
            JsonNode node = objectMapper.readValue(content, JsonNode.class);
            comments = node.get("data");
            PagingParameters nextPage = PagedListUtils.getPagedListParameters(node.get("paging"), "next");
            if (nextPage != null)
                after = nextPage.getAfter();
        } catch (IOException e) {
            throw new UncategorizedApiException("facebook", "Error deserializing comments for user [" +
                    userDomain + "]", e);
        }
        Map<String, String> links = new IdentityHashMap<>(comments.size() * 2 + 1); // link for each commentor, replies and maybe next page
        if (after != null)
            // generate link <a href='fb://{user}/posts/{post}/x100/0?after={after}'>{post}-comments-index-x100-page-{after}</a>
            links.put(Sources.FACEBOOK + "://" + userDomain + "/posts/" + parentMessageId + "/x100/" + after,
                    parentMessageId + "comments-index-x100-page-" + after);

        ParseResult[] parseResults = new ParseResult[comments.size()];
        for (int i = 0; i < comments.size(); i++) {
            final JsonNode comment = comments.get(i);
            final String id = comment.get("id").asText();
            final String postObjectId;
            if (comment.get("object") != null) {
                String objectId = comment.get("object").get("id").asText();
                postObjectId = objectId.contains("_") ? objectId.substring(0, objectId.indexOf('_')) : objectId;
            } else
                postObjectId = id.substring(0, id.indexOf('_'));

            final String postUrl = Sources.FACEBOOK + "://" + userDomain + "/posts/" + userDomain + "_" + postObjectId;
            final String commentOwnerProfile = Sources.FACEBOOK + "://" + comment.get("from").get("id")
                    .asText();
            // generate link <a href='fb://{user}'>{user}</a>
            links.put(commentOwnerProfile, "");

            //TODO: Think about another link for replies, to not to confuse with posts
            // generate link <a href='fb://{user}/posts/{post}/x100/0'>{post}-comments-index-x100-page-0</a>
            links.put(Sources.FACEBOOK + "://" + userDomain + "/posts/" + id + "/x100/0",
                    id + "comments-index-x100-page-0");
            String commentUrl = postUrl + "/comments/" + id;
            Map<String, Object> commentParse = new HashMap<String, Object>() {{
                put(PostProperties.SOURCE, Sources.FACEBOOK);
                put(PostProperties.PROFILE_ID, commentOwnerProfile);
                put(PostProperties.PARENT_POST_ID, postUrl);
                put(PostProperties.HREF, "https://www.facebook.com/" + userDomain + "/posts/" + postObjectId +
                        "?comment_id=" + id.substring(id.lastIndexOf("_") + 1));
                put(PostProperties.SM_POST_ID, id);
                put(PostProperties.BODY, comment.get("message").asText());
                put(PostProperties.POST_DATE, comment.get("created_time").asText());
                //todo: is only one number?
                put(PostProperties.ENGAGEMENT, comment.get("like_count").asText());
                put(PostProperties.IS_COMMENT, true);
                put(DIGEST, DigestUtils.md5Hex(comment.toString()));
            }};

            Map<String, Object> commentContentMeta = new HashMap<String, Object>() {{
                put(ContentMetaConstants.PARENT, commentOwnerProfile);
                put(ContentMetaConstants.TYPE, Types.POST);
            }};
            parseResults[i] = new ParseResult(commentUrl, new HashMap<String, String>(), commentParse, commentContentMeta);
        }

        return new ParseResult(uri.toString(), userDomain + "|post=" + parentMessageId + "|page=" + after, Serializer.getInstance()
                .toJson(parseResults), links, parseMeta, contentMeta);
    }
}
