package ru.wobot.vk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import ru.wobot.vk.dto.PostIndex;
import ru.wobot.vk.serialize.Builder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        if (UrlCheck.isPost(url)) {
            return createPostResponse(url);
        }
        throw new UnsupportedOperationException();
    }

    private static Response createProfileResponse(String userId, String urlString) throws IOException {
        VKontakteProfile user = getUserProfile(userId);
        String json = toJson(user);
        return new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private static Response createFriendsResponse(String userId, String urlString) throws UnsupportedEncodingException {
        VKontakteProfile user = getUserProfile(userId);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userId + "].friends:");
        }
        VKArray<VKontakteProfile> friends = Proxy.getInstance().friendsOperations().get(user.getId());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userId + "]|.friends[count=" + friends.getCount() + "]!");
        }

        String[] ids = friends
                .getItems()
                .stream()
                .map(x -> x.getScreenName())
                .filter(Objects::nonNull) //todo: выработать стратегию работы у удалёнными/заблокированными пользователями
                .sorted()
                .toArray(String[]::new);

        return new Response(urlString, toJson(ids).getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());

    }

    private static Response createPostsIndexResponse(String userId, String urlString) {
        VKontakteProfile user = getUserProfile(userId);
        String json = toJson(getPostCountForUser(user.getId()));
        return new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    // http://user/index-posts/x100/0000000001
    private static Response createPostsIndexPageResponse(URL url) {
        VKontakteProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String pageStr = path.substring(path.lastIndexOf('/') + 1);
        int page = Integer.parseInt(pageStr);

        int totalPosts = getPostCountForUser(user.getId());
        int offset = totalPosts - (page + 1) * POSTS_LIMIT;

        long[] ids = Proxy.getInstance().wallOperations()
                .getPostsForUser(user.getId(), offset, POSTS_LIMIT)
                .getItems()
                .stream()
                .mapToLong(x -> x.getId())
                .sorted()
                .toArray();

        String json = toJson(new PostIndex(ids, totalPosts));
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private static Response createPostResponse(URL url) {
        VKontakteProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String posId = path.substring(path.lastIndexOf('/') + 1);
        Post post = Proxy.getInstance().wallOperations().getPost(user.getId(), posId);
        String json = toJson(post);
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private static VKontakteProfile getUserProfile(String userId) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userId + "]:");
        }
        List<VKontakteProfile> profileList = Proxy.getInstance().usersOperations().getUsers(Arrays.asList(userId));
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userId + "]!");
        }
        return profileList.get(0);
    }

    private static String toJson(Object obj) {
        String json = Builder.getGson().toJson(obj);
        return json;
    }
}
