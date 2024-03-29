package ru.wobot.sm.parse;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.social.UncategorizedApiException;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.Post;
import org.springframework.social.facebook.api.impl.PagedListUtils;
import org.springframework.social.facebook.api.impl.json.FacebookModule;
import org.springframework.util.StringUtils;
import ru.wobot.sm.core.api.FbApiTypes;
import ru.wobot.sm.core.mapping.PostProperties;
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Sources;
import ru.wobot.sm.core.mapping.Types;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.core.parse.Parser;
import ru.wobot.sm.serialize.Serializer;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class FbParser implements Parser {
    private static final String DIGEST = "digest";
    private static final int TEXTSIZE_UPPER_LIMIT = 10000;
    private static final Logger LOG = LoggerFactory.getLogger(FbParser.class.getName());

    static {
        try {
            List<String> jsonProfiles = new ArrayList<>();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            for (Resource r : resolver.getResources("classpath*:profiles.sm/*"))
                jsonProfiles.add(IOUtils.toString(r.getInputStream()));

            DetectorFactory.loadProfile(jsonProfiles);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FbParser() {
        objectMapper.registerModule(new FacebookModule());
    }

    @Override
    public ParseResult parse(URI uri, String content, String apiType, String apiVersion) {
        switch (apiType) {
            case FbApiTypes.PROFILE:
                return parsePage(uri, content);
            case FbApiTypes.FRIEND_LIST_OF_ID:
                return parseFriends(uri, content);
            case FbApiTypes.POST_BULK:
                return parsePostsIndexPage(uri, content);
            case FbApiTypes.COMMENT_BULK:
                return parseCommentPage(uri, content);
        }
        throw new UnsupportedOperationException("Parser for this content not found.");
    }

    public ParseResult parsePage(URI uri, String content) {
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

        JsonNode desc = profile.get("description");
        JsonNode about = profile.get("about");
        StringBuilder pageDesc = new StringBuilder();
        if (desc != null)
            pageDesc.append(desc.asText());
        if (about != null) {
            pageDesc.append(" ");
            pageDesc.append(about.asText());
        }

        if (checkLanguage(pageDesc.toString().trim())) {
            // generate link <a href='fb://{user}/friends'>user-friends</a>
            links.put(Sources.FACEBOOK + "://" + userId + "/friends", userId + "-friends");

            // generate link <a href='fb://{user}/index-posts/x100/00000000'>user-index-posts-x100-page-0</a>
            links.put(Sources.FACEBOOK + "://" + userId + "/index-posts/x50/00000000", userId + "-index-posts-x50-page-0");
        } else
            LOG.info("URL filtered by language filter: " + Sources.FACEBOOK + "://" + userId);

        // fill parse metadata
        final String fullName = profile.get("name").asText();
        parseMeta.put(ProfileProperties.SOURCE, Sources.FACEBOOK);
        parseMeta.put(ProfileProperties.NAME, fullName);
        parseMeta.put(ProfileProperties.HREF, profile.get("link").asText());
        parseMeta.put(ProfileProperties.SM_PROFILE_ID, userId);
        parseMeta.put(ProfileProperties.REACH, profile.get("fan_count") == null ? 1 : profile.get("fan_count").asInt());  //default reach is 1 IMHO
        if (profile.get("location") != null && profile.get("location").get("city") != null)
            parseMeta.put(ProfileProperties.CITY, profile.get("location").get("city").asText());

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

        Map<String, String> links = new HashMap<>(posts.size() * 2 + 1); //first comments page and author of each post (if author not this page) and optional next page;
        List<ParseResult> parseResults = null;
        if (posts.size() != 0) {
            parseResults = new ArrayList<>();
            if (nextPage != null)
                // generate link <a href='fb://{userId}/index-posts/x100/{until}'>{userId}-index-posts-x100-page-{until}</a>
                links.put(Sources.FACEBOOK + "://" + userDomain + "/index-posts/x100/" + nextPage.getUntil(),
                        userDomain + "-index-posts-x100-page-" + nextPage.getUntil());
            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);
                String postId = post.getId();
                JsonNode rawPost = dataNode.get(i);

                StringBuilder body = new StringBuilder();
                if (StringUtils.hasText(post.getMessage()))
                    body.append(post.getMessage());
                if (StringUtils.hasText(post.getDescription())) {
                    body.append(" ");
                    body.append(post.getDescription());
                }

                //if (checkLanguage(body.toString()))
                // generate link <a href='fb://{userId}/posts/{postId}/x100/0'>{postId}-comments-index-x100-page-0</a>
                links.put(urlPrefix + postId + "/x100/0", postId + "-comments-index-x100-page-0");
                /*else
                    LOG.info("URL filtered by language filter: " + urlPrefix + postId);*/

                String postOwnerProfile = null;
                if (!post.getFrom().getId().equals(userDomain)) {
                    // generate link <a href='fb://{user}'>{user}</a>
                    postOwnerProfile = Sources.FACEBOOK + "://" + post.getFrom().getId();
                    links.put(postOwnerProfile, "");
                }

                Map<String, Object> postParse = new HashMap<>();
                Map<String, Object> postContent = new HashMap<>();

                postParse.put(PostProperties.SOURCE, Sources.FACEBOOK);
                postParse.put(PostProperties.PROFILE_ID, Sources.FACEBOOK + "://" + post.getFrom().getId());
                postParse.put(PostProperties.HREF, "https://www.facebook.com/" + userDomain + "/posts/" +
                        postId.substring(postId.lastIndexOf('_') + 1));
                postParse.put(PostProperties.SM_POST_ID, postId);
                postParse.put(PostProperties.BODY, body.toString().trim());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                postParse.put(PostProperties.POST_DATE, dateFormat.format(post.getCreatedTime()));
                // this nodes present not for all posts
                int engagement = 0;
                if (rawPost.get("likes") != null && rawPost.get("likes").get("summary") != null &&
                        rawPost.get("likes").get("summary").get("total_count") != null)
                    engagement = rawPost.get("likes").get("summary").get("total_count").asInt();
                if (rawPost.get("comments") != null && rawPost.get("comments").get("summary") != null &&
                        rawPost.get("comments").get("summary").get("total_count") != null)
                    engagement += rawPost.get("comments").get("summary").get("total_count").asInt();
                engagement += post.getShares();
                postParse.put(PostProperties.ENGAGEMENT, engagement);
                postParse.put(PostProperties.IS_COMMENT, false);
                postParse.put(DIGEST, DigestUtils.md5Hex(post.toString()));
                if (postOwnerProfile != null) {
                    postParse.put("profile_href", rawPost.get("from").get("link").asText());
                    postParse.put("profile_name", post.getFrom().getName());
                    postParse.put(ProfileProperties.SM_PROFILE_ID, post.getFrom().getId());
                    postParse.put(ProfileProperties.REACH, 0);
                }
                // fill content metadata
                postContent.put(ContentMetaConstants.TYPE, postOwnerProfile == null ? Types.POST : Types.DETAILED_POST);
                postContent.put(ContentMetaConstants.PARENT, Sources.FACEBOOK + "://" + post.getFrom().getId());

                parseResults.add(new ParseResult(urlPrefix + postId, new HashMap<String, String>(), postParse, postContent));
            }
        }
        return new ParseResult(uri.toString(), userDomain, Serializer.getInstance().toJson(parseResults), links,
                new HashMap<String, Object>(), contentMeta);
    }

    private boolean checkLanguage(String text) {
        if (!StringUtils.hasText(text))
            return true;
        Detector detector;
        try {
            detector = DetectorFactory.create();
            detector.setMaxTextLength(TEXTSIZE_UPPER_LIMIT);
            detector.append(text);
            String lang = detector.detect();
            return "ru".equals(lang) || "uk".equals(lang); // russian and ukraine for this time
        } catch (LangDetectException e) {
            return true; //in case of all errors return true - it's better to fetch irrelevant messages than miss relevant
        }
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
            // generate link <a href='fb://{userId}/posts/{postId}/x100/{after}'>{postId}-comments-index-x100-page-{after}</a>
            links.put(Sources.FACEBOOK + "://" + userDomain + "/posts/" + parentMessageId + "/x100/" + after,
                    parentMessageId + "comments-index-x100-page-" + after);

        List<ParseResult> parseResults = new ArrayList<>(comments.size() * 2);
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
                put("profile_href", comment.get("from").get("link").asText());
                put("profile_name", comment.get("from").get("name").asText());
                put(ProfileProperties.SM_PROFILE_ID, comment.get("from").get("id").asText());
                put(ProfileProperties.REACH, 0);
            }};

            Map<String, Object> commentContentMeta = new HashMap<String, Object>() {{
                put(ContentMetaConstants.PARENT, commentOwnerProfile);
                put(ContentMetaConstants.TYPE, Types.DETAILED_POST);
            }};
            parseResults.add(new ParseResult(commentUrl, new HashMap<String, String>(), commentParse, commentContentMeta));
        }

        return new ParseResult(uri.toString(), userDomain + "|post=" + parentMessageId + "|page=" + after, Serializer.getInstance()
                .toJson(parseResults.toArray()), links, parseMeta, contentMeta);
    }

    private Map<String, Object> getProfileParse(final JsonNode profile) {
        return new HashMap<String, Object>() {{
            //put(PostProperties.ID, Sources.FACEBOOK + "://" + profile.get("id").asText());
            put(ProfileProperties.SOURCE, Sources.FACEBOOK);
            put(ProfileProperties.HREF, profile.get("link").asText());
            put(ProfileProperties.NAME, profile.get("name").asText());
            put(ProfileProperties.SM_PROFILE_ID, profile.get("id").asText());
            put(ProfileProperties.REACH, 0);
            put(DIGEST, DigestUtils.md5Hex(profile.toString()));
        }};
    }
}
