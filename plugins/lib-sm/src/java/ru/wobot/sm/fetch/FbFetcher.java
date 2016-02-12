package ru.wobot.sm.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.social.UncategorizedApiException;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.User;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.support.URIBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;
import ru.wobot.uri.QueryParam;
import ru.wobot.uri.Scheme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Scheme("fb")
public class FbFetcher {
    //TODO: Add more fields
    static final String[] PROFILE_FIELDS = {
            "id", "name", "username", "likes", "talking_about_count", "about", "artists_we_like", "website",
            "link", "location"
    };

    static final String[] COMMENT_PROFILE_FIELDS = {"from{id,name,link,first_name,last_name,name_format}"};

    @SuppressWarnings("unused")
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
            "parent{id}", "object{id}"};

    private static final String API_VERSION = "2.5";
    private final CredentialRepository repository;

    public FbFetcher(CredentialRepository repository) {
        this.repository = repository;
    }

    private String getObjectType(String id, String scope) {
        Facebook facebook = new FacebookTemplate(repository.getInstance().getAccessToken());
        try {
            if (scope != null && scope.equals("user")) { //may be user or page (comment author)
                MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
                queryParameters.set("fields", "metadata{type}");
                queryParameters.set("metadata", "1");
                User node = facebook.fetchObject(id, User.class, queryParameters);
                return ((Map) node.getExtraData().get("metadata")).get("type").toString();
            } else
                return "page";
        } catch (UncategorizedApiException e) {
            return "user";
        }
    }

    private JsonNode getObject(MultiValueMap<String, String> queryParameters, String path) {
        Facebook facebook = new FacebookTemplate(repository.getInstance().getAccessToken());
        URIBuilder uriBuilder = URIBuilder.fromUri(facebook.getBaseGraphApiUrl() + path);
        return facebook.restOperations().getForObject(uriBuilder.queryParams(queryParameters).build(), JsonNode.class);
    }

    /*public List<SMProfile> getProfiles(List<String> userIds) throws IOException {
        List<SMProfile> result = new ArrayList<>(userIds.size());
        for (String id : userIds) {
            String objectId = !id.contains("?") ? id : id.substring(0, id.indexOf("?"));
            String objectType = getObjectType(id);
            MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
            if (objectType.equals("page")) {
                queryParameters.set("fields", StringUtils.arrayToCommaDelimitedString(PROFILE_FIELDS));
                JsonNode page = getObject(queryParameters, objectId);
                String pageId = page.get("id").asText();
                JsonNode username = page.get("username");
                result.add(new SMProfile(pageId, username != null ? username.asText() : pageId, page.get("name").asText()));
            } else {
                queryParameters.set("fields", StringUtils.arrayToCommaDelimitedString(COMMENT_PROFILE_FIELDS));
                JsonNode object = getObject(queryParameters, id.substring(id.lastIndexOf("=") + 1)).get("from");
                result.add(new SMProfile(id, object.get("id").asText(), object.get("name").asText()));
            }
        }
        return result;
    }*/

    // fb://mastercardrussia
    // fb://96814974590
    // fb://1704938049732711?scope=user&comment_id=10154479526792506_10154481125912506
    @Path("{userId}")
    public FetchResponse getProfileData(@PathParam("userId") String userId,
                                        @QueryParam("scope") String scope,
                                        @QueryParam("comment_id") String commentId) throws IOException {
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
        }};

        //TODO: Think about refactor with redirect
        String objectType = getObjectType(userId, scope);
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        if (objectType.equals("page")) {
            queryParameters.set("fields", StringUtils.arrayToCommaDelimitedString(PROFILE_FIELDS));
            JsonNode user = getObject(queryParameters, userId);
            ((ObjectNode) user).put("type", "page");
            return new FetchResponse(user.toString(), metaData);
        } else {
            queryParameters.set("fields", StringUtils.arrayToCommaDelimitedString(COMMENT_PROFILE_FIELDS));
            JsonNode user = getObject(queryParameters, commentId).get("from");
            ((ObjectNode) user).put("type", "user");
            return new FetchResponse(user.toString(), metaData);
        }
    }

    // fb://1704938049732711/friends
    @Path("{userId}/friends")
    public FetchResponse getFriendIds(@PathParam("userId") String userId) throws IOException {
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
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
        }};

        return new FetchResponse(result.toString(), metaData);
    }

    // fb://191234802505/index-posts/x100/00000000
    // fb://96814974590/index-posts/x100/1453725511
    @Path("{userId}/index-posts/x{limit}/{until}")
    public FetchResponse getPostsData(@PathParam("userId") String userId,
                                      @PathParam("limit") int limit,
                                      @PathParam("until") long until
    ) throws IOException {
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.set("fields", StringUtils.arrayToCommaDelimitedString(POST_FIELDS));
        if (until > 0)
            queryParameters.set("until", String.valueOf(until));
        if (limit > 0)
            queryParameters.set("limit", String.valueOf(limit));

        JsonNode responseNode = getObject(queryParameters, userId + "/feed");
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
        }};
        return new FetchResponse(responseNode.toString(), metaData);
    }

    // fb://165107853523677/posts/165107853523677_1081856348515485/x100/0
    // fb://165107853523677/posts/165107853523677_1081856348515485/x10/WTI5dGJXVnVkRjlqZFhKemIzSTZNVEE0TWpneU5qQXdOVEE0TlRFNE5qb3hORFV5TWpRd01qTTM=
    @Path("{userId}/posts/{postId}/x{limit}/{pageToken}")
    public FetchResponse getPostCommentsData(@PathParam("userId") @SuppressWarnings("unused") String userId, // TODO: Think about refactor
                                             @PathParam("postId") String postId,
                                             @PathParam("limit") int limit,
                                             @PathParam("pageToken") String pageToken) throws IOException {
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.set("fields", StringUtils.arrayToCommaDelimitedString(COMMENT_FIELDS));
        if (!pageToken.equals("0"))
            queryParameters.set("after", pageToken);
        if (limit > 0)
            queryParameters.set("limit", String.valueOf(limit));
        queryParameters.set("order", "reverse_chronological");

        JsonNode responseNode = getObject(queryParameters, postId + "/comments");
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
        }};
        return new FetchResponse(responseNode.toString(), metaData);
    }
}
