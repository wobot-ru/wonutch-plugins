package ru.wobot.sm.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.User;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.facebook.api.impl.json.FacebookModule;
import org.springframework.social.support.URIBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SMFetcher;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FbFetcher implements SMFetcher {
    //TODO: Add more fields
    static final String[] PROFILE_FIELDS = {
            "id", "name", "username", "likes", "talking_about_count", "about", "artists_we_like", "website",
            "link", "location"
    };
    static final String[] USER_FIELDS = {
            "id", "about", "age_range", "bio", "birthday", "cover", "currency", "devices", "education", "email",
            "favorite_athletes", "favorite_teams", "first_name", "gender", "hometown", "inspirational_people", "installed", "install_type",
            "is_verified", "languages", "last_name", "link", "locale", "location", "meeting_for", "middle_name", "name", "name_format",
            "political", "quotes", "payment_pricepoints", "relationship_status", "religion", "security_settings", "significant_other",
            "sports", "test_group", "timezone", "third_party_id", "updated_time", "verified", "video_upload_limits", "viewer_can_send_gift",
            "website", "work"
    };
    private static final String[] POST_FIELDS = {
            "id", "actions", "admin_creator", "application", "caption", "created_time", "description", "from{id,name}", "icon",
            "is_hidden", "is_published", "link", "message", "message_tags", "name", "object_id", "picture", "place",
            "privacy", "properties", "source", "status_type", "story", "to", "type", "updated_time",
            "with_tags", "shares", "likes.summary(true).limit(0)", "comments.summary(true).limit(0)"
    };
    private static final String[] COMMENT_FIELDS = {"message", "from", "like_count", "likes", "created_time",
            "parent{id}"};

    private static final String API_VERSION = "2.5";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CredentialRepository repository;

    public FbFetcher(CredentialRepository repository) {
        this.repository = repository;
        objectMapper.registerModule(new FacebookModule());
    }

    @Override
    public List<SMProfile> getProfiles(List<String> userIds) throws IOException {
        Facebook facebook = new FacebookTemplate(repository.getInstance().getAccessToken());
        List<SMProfile> result = new ArrayList<>(userIds.size());
        for (String id : userIds) {
            if (id.contains("scope=user")) {
                MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
                queryParameters.set("fields", "metadata{type}");
                queryParameters.set("metadata", "1");
                User node = facebook.fetchObject(id.substring(0, id.indexOf("?")), User.class, queryParameters);
                String type = ((Map) node.getExtraData().get("metadata")).get("type").toString();
                if (type.equals("user")) {
                    User user = facebook.fetchObject(node.getId(), User.class, USER_FIELDS);
                    result.add(new SMProfile(id, user.getId(), user.getName()));
                    continue;
                }
            }
            Page page = facebook.fetchObject(id, Page.class, PROFILE_FIELDS);
            result.add(new SMProfile(page.getId(), page.getUsername(), page.getName()));
        }
        return result;
    }

    @Override
    public FetchResponse getProfileData(String userId) throws IOException {
        Facebook facebook = new FacebookTemplate(repository.getInstance().getAccessToken());
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
        }};

        if (userId.contains("scope=user")) {
            MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
            queryParameters.set("fields", "metadata{type}");
            queryParameters.set("metadata", "1");
            User node = facebook.fetchObject(userId.substring(0, userId.indexOf("?")), User.class, queryParameters);
            String type = ((Map) node.getExtraData().get("metadata")).get("type").toString();
            if (type.equals("user")) {
                User user = facebook.fetchObject(node.getId(), User.class, USER_FIELDS);
                return new FetchResponse(objectMapper.writeValueAsString(user), metaData);
            }
        }
        Page page = facebook.fetchObject(userId, Page.class, PROFILE_FIELDS);
        return new FetchResponse(objectMapper.writeValueAsString(page), metaData);
    }

    @Override
    public List<String> getFriendIds(String userId) throws IOException {
        Facebook facebook = new FacebookTemplate(repository.getInstance().getAccessToken());
        List<String> result = new ArrayList<>();

        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.set("limit", "100");
        PagedList<Page> pages = facebook.fetchConnections(userId, "likes", Page.class, queryParameters);
        for (Page p : pages)
            result.add(p.getId());

        PagingParameters params = pages.getNextPage();
        while (params != null) {
            pages = facebook.likeOperations().getPagesLiked(userId, params);
            for (Page p : pages)
                result.add(p.getId());
            params = pages.getNextPage();
        }

        return result;
    }

    @Override
    public int getPostCount(String userId) throws IOException {
        throw new UnsupportedOperationException("Method not supported by Facebook API.");
    }

    @Override
    public FetchResponse getPostsData(String userId, long offset, int limit) throws IOException {
        Facebook facebook = new FacebookTemplate(repository.getInstance().getAccessToken());
        URIBuilder uriBuilder = URIBuilder.fromUri(facebook.getBaseGraphApiUrl() + userId + "/feed");
        uriBuilder.queryParam("fields", StringUtils.arrayToCommaDelimitedString(POST_FIELDS));
        if (offset > 0)
            uriBuilder.queryParam("until", String.valueOf(offset));
        if (limit > 0)
            uriBuilder.queryParam("limit", String.valueOf(limit));
        URI uri = uriBuilder.build();
        JsonNode responseNode = facebook.restOperations().getForObject(uri, JsonNode.class);

        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
        }};

        return new FetchResponse(responseNode.toString(), metaData);
    }

    @Override
    public FetchResponse getPostData(String userId, String postId) throws IOException {
        return null;
    }

    @Override
    public FetchResponse getPostCommentsData(String userId, String postId, int take, int skip) throws IOException {
        Facebook facebook = new FacebookTemplate(repository.getInstance().getAccessToken());
        URIBuilder uriBuilder = URIBuilder.fromUri(facebook.getBaseGraphApiUrl() + postId + "/comments");
        uriBuilder.queryParam("fields", StringUtils.arrayToCommaDelimitedString(COMMENT_FIELDS));
        //TODO: HACK - rewrite ASAP
        if (userId != null)
            uriBuilder.queryParam("after", userId);
        if (take > 0)
            uriBuilder.queryParam("limit", String.valueOf(take));
        uriBuilder.queryParam("order", "reverse_chronological");
        URI uri = uriBuilder.build();
        JsonNode responseNode = facebook.restOperations().getForObject(uri, JsonNode.class);

        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
        }};

        return new FetchResponse(responseNode.toString(), metaData);
    }
}
