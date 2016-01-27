package ru.wobot.sm.fetch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.facebook.api.impl.json.FacebookModule;
import org.springframework.social.support.URIBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
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
            "id", "name", "username", "likes", "talking_about_count", "about", "artists_we_like", "website"
    };

    private static final String[] POST_FIELDS = {
            "id", "actions", "admin_creator", "application", "caption", "created_time", "description", "from", "icon",
            "is_hidden", "is_published", "link", "message", "message_tags", "name", "object_id", "picture", "place",
            "privacy", "properties", "source", "status_type", "story", "to", "type", "updated_time",
            "with_tags", "shares", "likes{name,id,username,link,profile_type}", "comments{message,from,like_count,likes}"
    };
    private static final String API_VERSION = "2.5";
    private final Facebook facebook = new FacebookTemplate("1518184651811311|pvxdxslPYhiOS3xaB5V2lp0U2D0");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FbFetcher() {
        objectMapper.registerModule(new FacebookModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
    }

    @Override
    public List<SMProfile> getProfiles(List<String> userIds) throws IOException {
        List<SMProfile> result = new ArrayList<>(userIds.size());
        for (String id : userIds) {
            Page page = facebook.fetchObject(id, Page.class, PROFILE_FIELDS);
            result.add(new SMProfile(page.getId(), page.getUsername(), page.getName()));
        }
        return result;
    }

    @Override
    public FetchResponse getProfileData(String userId) throws IOException {
        Page page = facebook.fetchObject(userId, Page.class, PROFILE_FIELDS);

        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
        }};

        return new FetchResponse(objectMapper.writeValueAsString(page), metaData);
    }

    @Override
    public List<String> getFriendIds(String userId) throws IOException {
        List<String> result = new ArrayList<>();

        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.set("limit", "50");
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
        return 0;
    }

    @Override
    public FetchResponse getPostsData(String userId, long offset, int limit) throws IOException {
        URIBuilder uriBuilder = URIBuilder.fromUri(facebook.getBaseGraphApiUrl() + userId + "/feed");
        uriBuilder.queryParam("fields", StringUtils.arrayToCommaDelimitedString(POST_FIELDS));
        if (offset > 0)
            uriBuilder.queryParam("until", String.valueOf(offset));
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
    public FetchResponse getPostCommentsData(String userId, String postId, int skip, int take) throws IOException {
        return null;
    }
}
