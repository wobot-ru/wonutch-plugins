package ru.wobot.sm.core.domain.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.wobot.sm.core.fetch.SMService;
import ru.wobot.sm.core.url.UrlCheck;
import ru.wobot.sm.core.domain.PostIndex;
import ru.wobot.sm.core.domain.Response;
import ru.wobot.sm.core.domain.SMProfile;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DomainService {

    public static final int POSTS_LIMIT = 100;
    private static final Log LOG = LogFactory.getLog(DomainService.class.getName());
    private static final Gson Gson = new GsonBuilder().create();
    private final SMService smService;

    public DomainService(SMService smService) {
        this.smService = smService;
    }

    public int getPostCountForUser(String userId) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userId + "].posts.count:");
        }
        int postsCount = smService.getPostCount(userId);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userId + "].posts.count=" + postsCount + "!");
        }
        return postsCount;
    }

    public Response request(String urlString) throws IOException {
        URL url = new URL(urlString);
        String userDomain = url.getHost();
        if (UrlCheck.isProfile(url)) {
            return createProfileResponse(userDomain, urlString);
        }
        if (UrlCheck.isFriends(url)) {
            return createFriendsResponse(userDomain, urlString);
        }
        if (UrlCheck.isPostsIndex(url)) {
            return createPostsIndexResponse(userDomain, urlString);
        }
        if (UrlCheck.isPostsIndexPage(url)) {
            return createPostsIndexPageResponse(url);
        }
        if (UrlCheck.isPost(url)) {
            return createPostResponse(url);
        }
        if (UrlCheck.isCommentPage(url)) {
            return createCommentPageResponse(url);
        }

        throw new UnsupportedOperationException();
    }

    private Response createCommentPageResponse(URL url) throws IOException {
        String userDomain = url.getHost();
        SMProfile user = getUserProfile(userDomain);
        String path = url.getPath();
        String[] split = path.split("/");
        String postId = split[2];
        int page = Integer.parseInt(split[4]);

        String json = smService.getPostCommentsData(user.getId(), postId, page * POSTS_LIMIT, POSTS_LIMIT);
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private Response createProfileResponse(String userDomain, String urlString) throws IOException {
        SMProfile user = getUserProfile(userDomain);
        String json = smService.getProfileData(user.getId());
        return new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private Response createFriendsResponse(String userDomain, String urlString) throws IOException {
        SMProfile user = getUserProfile(userDomain);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userDomain + "].friends:");
        }
        List<String> ids = smService.getFriendIds(user.getId());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userDomain + "]|.friends[count=" + ids.size() + "]!");
        }
        Collections.sort(ids);
        return new Response(urlString, toJson(ids).getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());

    }

    private Response createPostsIndexResponse(String userDomain, String urlString) throws IOException {
        SMProfile user = getUserProfile(userDomain);
        String json = toJson(getPostCountForUser(user.getId()));
        return new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    // http://user/index-posts/x100/0000000001
    private Response createPostsIndexPageResponse(URL url) throws IOException {
        SMProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String pageStr = path.substring(path.lastIndexOf('/') + 1);
        int page = Integer.parseInt(pageStr);

        int totalPosts = getPostCountForUser(user.getId());
        int offset = totalPosts - (page + 1) * POSTS_LIMIT;

        List<String> ids = smService.getPostIds(user.getId(), offset, POSTS_LIMIT);
        Collections.sort(ids);
        String json = toJson(new PostIndex(ids.toArray(new String[ids.size()]), totalPosts));
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private Response createPostResponse(URL url) throws IOException {
        SMProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String posId = path.substring(path.lastIndexOf('/') + 1);
        String json = smService.getPostData(user.getId(), posId);
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private SMProfile getUserProfile(String userDomain) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userDomain + "]:");
        }

        List<SMProfile> profileList = smService.getProfiles(Arrays.asList(userDomain));
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userDomain + "]!");
        }
        return profileList.get(0);
    }

    private String toJson(Object obj) {
        return Gson.toJson(obj);
    }
}
