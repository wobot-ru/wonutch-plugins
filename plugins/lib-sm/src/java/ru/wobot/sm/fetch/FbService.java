package ru.wobot.sm.fetch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.facebook.api.impl.json.FacebookModule;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SMService;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FbService implements SMService {
    //TODO: Add more fields
    static final String[] PROFILE_FIELDS = {
            "id", "name", "username", "likes", "talking_about_count", "about", "artists_we_like", "website"
    };
    private static final String API_VERSION = "2.5";
    private final Facebook facebook = new FacebookTemplate("1518184651811311|pvxdxslPYhiOS3xaB5V2lp0U2D0");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FbService() {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        objectMapper.registerModule(new FacebookModule());
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

        Map<String, String> metaData = new HashMap<String, String>() {{
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
    public FetchResponse getPostsData(String userId, int offset, int limit) throws IOException {
        return null;
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
