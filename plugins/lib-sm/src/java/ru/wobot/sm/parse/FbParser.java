package ru.wobot.sm.parse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.social.UncategorizedApiException;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.Post;
import org.springframework.social.facebook.api.impl.PagedListUtils;
import org.springframework.social.facebook.api.impl.json.FacebookModule;
import ru.wobot.sm.core.Sources;
import ru.wobot.sm.core.mapping.PostProperties;
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Types;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.meta.NutchDocumentMetaConstants;
import ru.wobot.sm.core.parse.AbstractParser;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.core.url.UrlSchemaConstants;
import ru.wobot.sm.serialize.Serializer;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class FbParser extends AbstractParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FbParser() {
        objectMapper.registerModule(new FacebookModule());
    }

    @Override
    public ParseResult parseProfile(URL url, String content) {
        String urlString = url.toString();
        String userDomain = url.getHost();
        Map<String, Object> parseMeta = new HashMap<>();
        Map<String, Object> contentMeta = new HashMap<>();
        if (content == null || content.isEmpty())
            return new ParseResult(urlString, userDomain, content, new HashMap<String, String>(), parseMeta,
                    contentMeta);
        Page profile;
        try {
            profile = objectMapper.reader(Page.class).readValue(content);
        } catch (IOException e) {
            throw new UncategorizedApiException("facebook", "Error deserializing profile [" + userDomain + "]", e);
        }

        String userId = profile.getId();
        Map<String, String> links = new HashMap<>();
        //TODO: HACK-REWRITE ASAP
        if (!urlString.contains("scope=user")) {
            // generate link <a href='fb://{user}/friends'>user-friends</a>
            links.put(UrlSchemaConstants.FACEBOOK + userId + "/friends", userId + "-friends");

            // generate link <a href='fb://{user}/index-posts/x100/00000000'>user-index-posts-x100-page-0</a>
            links.put(UrlSchemaConstants.FACEBOOK + userId + "/index-posts/x100/00000000", userId + "-index-posts-x100-page-0");
        }

        // fill parse metadata
        final String fullName = profile.getName();
        parseMeta.put(ProfileProperties.SOURCE, Sources.FACEBOOK);
        parseMeta.put(ProfileProperties.NAME, fullName);
        parseMeta.put(ProfileProperties.HREF, profile.getLink());
        parseMeta.put(ProfileProperties.SM_PROFILE_ID, String.valueOf(profile.getId()));
        parseMeta.put(ProfileProperties.REACH, profile.getLikes());

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.PROFILE);
        return new ParseResult(urlString, fullName, content, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parseFriends(URL url, String content) {
        String userDomain = url.getHost();
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
            links.put(UrlSchemaConstants.FACEBOOK + friendId, friendId);
        }

        return new ParseResult(url.toString(), userDomain + "-friends", content, links, parseMeta, contentMeta);
    }

    @Override
    protected ParseResult parsePostsIndex(URL url, String content) {
        throw new UnsupportedOperationException("Method not supported by Facebook API.");
    }

    @Override
    protected ParseResult parsePostsIndexPage(URL url, String content) {
        String userDomain = url.getHost();
        String urlPrefix = UrlSchemaConstants.FACEBOOK + userDomain + "/posts/";
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
            links = new HashMap<>(posts.size() + 1);
            if (nextPage != null)
                // generate link <a href='fb://{user}/index-posts/x100/{until}'>{user}-index-posts-x100-page-{until}</a>
                links.put(UrlSchemaConstants.FACEBOOK + userDomain + "/index-posts/x100/" + nextPage.getUntil(),
                        userDomain + "-index-posts-x100-page-" + nextPage.getUntil());
            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);
                String postId = post.getId();
                JsonNode rawPost = dataNode.get(i);

                // generate link <a href='fb://{user}/posts/{post}/x100/0'>{post}-comments-index-x100-page-0</a>
                links.put(urlPrefix + postId + "/x100/0", postId + "-comments-index-x100-page-0");

                Map<String, Object> postParse = new HashMap<>();
                Map<String, Object> postContent = new HashMap<>();

                postParse.put(PostProperties.SOURCE, Sources.FACEBOOK);
                postParse.put(PostProperties.PROFILE_ID, UrlSchemaConstants.FACEBOOK + post.getFrom().getId());
                postParse.put(PostProperties.HREF, "https://www.facebook.com/" + userDomain + "/posts/" + post.getObjectId());
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
                postContent.put(ContentMetaConstants.PARENT, UrlSchemaConstants.FACEBOOK + postId.substring(0, postId.indexOf('_')));
                postContent.put(NutchDocumentMetaConstants.DIGEST, DigestUtils.md5Hex(post.toString()));
                parseResults[i] = new ParseResult(urlPrefix + postId, new HashMap<String, String>(), postParse, postContent);
            }
        }
        return new ParseResult(url.toString(), userDomain, Serializer.getInstance().toJson(parseResults), (links
                == null ? new HashMap<String, String>() : links), new HashMap<String, Object>(), contentMeta);
    }

    @Override
    protected ParseResult parsePost(URL url, String content) {
        return null;
    }

    @Override
    protected ParseResult parseCommentPage(URL url, String content) {
        final String userDomain = url.getHost();
        final String[] path = url.getPath().split("/");
        final String parentMessageId = path[2]; // maybe post or comment (replies have comment as a parent, not post)

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
            links.put(UrlSchemaConstants.FACEBOOK + userDomain + "/posts/" + parentMessageId + "/x100/0?after=" + after,
                    parentMessageId + "comments-index-x100-page-" + after);

        ParseResult[] parseResults = new ParseResult[comments.size()];
        for (int i = 0; i < comments.size(); i++) {
            final JsonNode comment = comments.get(i);
            final String id = comment.get("id").asText();
            final String postObjectId = id.substring(0, id.indexOf('_'));
            final String postUrl = UrlSchemaConstants.FACEBOOK + userDomain + "/posts/" + userDomain + "_" + postObjectId;
            final String commentOwnerProfile = UrlSchemaConstants.FACEBOOK + comment.get("from").get("id")
                    .asText() + "?scope=user";
            // generate link <a href='fb://{user}?scope=user'>{user}?scope=user</a>
            links.put(commentOwnerProfile, "");

            //TODO: Think about another link for replies, to not to confuse with posts
            // generate link <a href='fb://{user}/posts/{post}/x100/0'>{post}-comments-index-x100-page-0</a>
            links.put(UrlSchemaConstants.FACEBOOK + userDomain + "/posts/" + id + "/x100/0",
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
                put(NutchDocumentMetaConstants.DIGEST, DigestUtils.md5Hex(comment.toString()));
            }};

            Map<String, Object> commentContentMeta = new HashMap<String, Object>() {{
                put(ContentMetaConstants.PARENT, commentOwnerProfile);
                put(ContentMetaConstants.TYPE, Types.POST);
            }};
            parseResults[i] = new ParseResult(commentUrl, new HashMap<String, String>(), commentParse, commentContentMeta);
        }

        return new ParseResult(url.toString(), userDomain + "|post=" + parentMessageId + "|page=" + after, Serializer.getInstance()
                .toJson(parseResults), links, parseMeta, contentMeta);
    }
}