package ru.wobot.vk;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.json.VKArray;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Service {
    public static final int POSTS_LIMIT = 100;
    private static final Log LOG = LogFactory.getLog(Service.class.getName());

    public static int getPostCountForUser(long userId) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userId + "].posts.count:");
        }
        VKArray<Post> posts = Proxy.getInstance().wallOperations().getPostsForUser(userId, 0, 1);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userId + "].posts.count=" + posts.getCount() + "!");
        }
        return posts.getCount();
    }

    public static Response request(String urlString) throws IOException {
        URL url = new URL(urlString);
        String userId = url.getHost();
        if (UrlCheck.isProfile(url)) {
            return createProfileResponse(userId, urlString);
        }
        if (UrlCheck.isFriends(url)) {
            return createFriendsResponse(userId, urlString);
        }
        if (UrlCheck.isPostsIndex(url)) {
            return createPostsIndexResponse(userId, urlString);
        }
        if (UrlCheck.isPostsIndexPage(url)) {
            return createPostsIndexPageResponse(url);
        }
        throw new UnsupportedOperationException();
    }

    private static Response createProfileResponse(String userId, String urlString) throws IOException {
        VKontakteProfile user = getUserProfile(userId);
        String json = toJson(user);
        Response response = new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
        return response;
    }

    private static Response createFriendsResponse(String userId, String urlString) throws UnsupportedEncodingException {
        VKontakteProfile user = getUserProfile(userId);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userId + "].friends:");
        }

        VKArray<VKontakteProfile> friendArray = Proxy.getInstance().friendsOperations().get(user.getId());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userId + "]|.friends[count=" + friendArray.getCount() + "]!");
        }

        List<VKontakteProfile> friends = friendArray.getItems();
        String json = toJson(friends);
        Response response = new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
        return response;

    }

    private static Response createPostsIndexResponse(String userId, String urlString) {
        VKontakteProfile user = getUserProfile(userId);
        int postsCount = getPostCountForUser(user.getId());

        String json = toJson(postsCount);
        Response response = new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
        return response;
    }

    // http://user/index-posts/x100/0000000001
    private static Response createPostsIndexPageResponse(URL url) {
        String path = url.getPath();
        String pageStr = path.substring(path.lastIndexOf('/') + 1);
        int page = Integer.parseInt(pageStr);
        VKontakteProfile user = getUserProfile(url.getHost());
        int totalPosts = getPostCountForUser(user.getId());
        int offset = totalPosts - (page + 1) * POSTS_LIMIT;
        VKArray<Post> posts = Proxy.getInstance().wallOperations().getPostsForUser(user.getId(), offset, POSTS_LIMIT);
        List<Post> postList = posts.getItems();
        String json = toJson(postList);
        Response response = new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
        return response;
    }

    private static VKontakteProfile getUserProfile(String userId) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userId + "]:");
        }
        List<VKontakteProfile> profileList = Proxy.getInstance().usersOperations().getUsers(Arrays.asList(userId));
        VKontakteProfile profile = profileList.get(0);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userId + "]!");
        }

        return profile;
    }

    private static String toJson(Object obj) {
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        return json;
    }
}
