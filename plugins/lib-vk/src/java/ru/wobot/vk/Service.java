package ru.wobot.vk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.social.vkontakte.api.CommentsResponse;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import org.springframework.social.vkontakte.api.impl.wall.CommentsQuery;
import org.springframework.social.vkontakte.api.impl.wall.UserWall;
import ru.wobot.vk.dto.PostIndex;
import ru.wobot.vk.serialize.Builder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Service {
    public static final int POSTS_LIMIT = 100;
    private static final Log LOG = LogFactory.getLog(Service.class.getName());
    private final ApiBindingRepository proxy;

    public Service(ApiBindingRepository proxy) {
        this.proxy = proxy;
    }

    public int getPostCountForUser(long userId) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userId + "].posts.count:");
        }
        VKArray<Post> posts = proxy.getInstance().wallOperations().getPostsForUser(userId, 0, 1);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userId + "].posts.count=" + posts.getCount() + "!");
        }
        return posts.getCount();
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

    private Response createCommentPageResponse(URL url) {
        String userDomain = url.getHost();
        VKontakteProfile user = getUserProfile(userDomain);
        String path = url.getPath();
        String[] split = path.split("/");
        int postId = Integer.parseInt(split[2]);
        int page = Integer.parseInt(split[4]);

        CommentsResponse response = getCommentResponse(user.getId(), postId, page);
        String json = toJson(response);
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private CommentsResponse getCommentResponse(long userId, int postId, int page) {
        CommentsQuery query = new CommentsQuery
                .Builder(new UserWall(userId), postId)
                .needLikes(true)
                .count(POSTS_LIMIT)
                .offset(page * POSTS_LIMIT)
                .build();

        CommentsResponse comments = proxy.getInstance().wallOperations().getComments(query);
        return comments;
    }

    private Response createProfileResponse(String userDomain, String urlString) throws IOException {
        VKontakteProfile user = getUserProfile(userDomain);
        String json = toJson(user);
        return new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private Response createFriendsResponse(String userDomain, String urlString) throws UnsupportedEncodingException {
        VKontakteProfile user = getUserProfile(userDomain);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userDomain + "].friends:");
        }
        //todo: reduce the amount of loaded fields. load only required!
        VKArray<VKontakteProfile> friends = proxy.getInstance().friendsOperations().get(user.getId());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userDomain + "]|.friends[count=" + friends.getCount() + "]!");
        }

        List<String> ids = new ArrayList<>(friends.getItems().size());
        for (VKontakteProfile p : friends.getItems()) {
            String sn = p.getScreenName();
            if (sn != null && !sn.isEmpty())
                ids.add(sn);
        }
        Collections.sort(ids);

        return new Response(urlString, toJson(ids).getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());

    }

    private Response createPostsIndexResponse(String userDomain, String urlString) {
        VKontakteProfile user = getUserProfile(userDomain);
        String json = toJson(getPostCountForUser(user.getId()));
        return new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    // http://user/index-posts/x100/0000000001
    private Response createPostsIndexPageResponse(URL url) {
        VKontakteProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String pageStr = path.substring(path.lastIndexOf('/') + 1);
        int page = Integer.parseInt(pageStr);

        int totalPosts = getPostCountForUser(user.getId());
        int offset = totalPosts - (page + 1) * POSTS_LIMIT;


        List<Post> posts = proxy.getInstance().wallOperations()
                .getPostsForUser(user.getId(), offset, POSTS_LIMIT)
                .getItems();
        long[] ids = new long[posts.size()];
        for (int i = 0; i < ids.length; i++)
            ids[i] = posts.get(i).getId();

        Arrays.sort(ids);

        String json = toJson(new PostIndex(ids, totalPosts));
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private Response createPostResponse(URL url) {
        VKontakteProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String posId = path.substring(path.lastIndexOf('/') + 1);
        Post post = proxy.getInstance().wallOperations().getPost(user.getId(), posId);
        String json = toJson(post);
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private VKontakteProfile getUserProfile(String userDomain) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userDomain + "]:");
        }
        List<VKontakteProfile> profileList = proxy.getInstance().usersOperations().getUsers(Arrays.asList(userDomain));
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userDomain + "]!");
        }
        return profileList.get(0);
    }

    private String toJson(Object obj) {
        String json = Builder.getGson().toJson(obj);
        return json;
    }
}
