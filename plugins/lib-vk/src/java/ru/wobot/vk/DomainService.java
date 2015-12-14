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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DomainService {

    public static final int POSTS_LIMIT = 100;
    private static final Log LOG = LogFactory.getLog(DomainService.class.getName());
    private static final VKService VKService = new VKService();

    public static int getPostCountForUser(long userId) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userId + "].posts.count:");
        }
        VKArray<Post> posts = VKService.getPostsForUser(userId, 0, 1);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userId + "].posts.count=" + posts.getCount() + "!");
        }
        return posts.getCount();
    }

    public static Response request(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        String userDomain = url.getHost();
        try {
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
        } catch (IOException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            e.printStackTrace();
        }

        throw new UnsupportedOperationException();
    }

    private static Response createCommentPageResponse(URL url) throws IOException {
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

    private static CommentsResponse getCommentResponse(long userId, int postId, int page) throws IOException {
        CommentsQuery query = new CommentsQuery
                .Builder(new UserWall(userId), postId)
                .needLikes(true)
                .count(POSTS_LIMIT)
                .offset(page * POSTS_LIMIT)
                .build();

        CommentsResponse comments = VKService.getComments(query);
        return comments;
    }

    private static Response createProfileResponse(String userDomain, String urlString) throws IOException {
        VKontakteProfile user = getUserProfile(userDomain);
        String json = toJson(user);
        return new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private static Response createFriendsResponse(String userDomain, String urlString) throws IOException {
        VKontakteProfile user = getUserProfile(userDomain);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userDomain + "].friends:");
        }
        //todo: reduce the amount of loaded fields. load only required!
        VKArray<VKontakteProfile> friends = VKService.getFriends(user.getId());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userDomain + "]|.friends[count=" + friends.getCount() + "]!");
        }

        List<String> ids = new ArrayList<>(friends.getItems().size());
        for (VKontakteProfile p : friends.getItems()) {
            String domain = p.getDomain();
            if (domain != null && !domain.isEmpty())
                ids.add("id" + p.getId());
        }
        Collections.sort(ids);

        return new Response(urlString, toJson(ids).getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());

    }

    private static Response createPostsIndexResponse(String userDomain, String urlString) throws IOException {
        VKontakteProfile user = getUserProfile(userDomain);
        String json = toJson(getPostCountForUser(user.getId()));
        return new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    // http://user/index-posts/x100/0000000001
    private static Response createPostsIndexPageResponse(URL url) throws IOException {
        VKontakteProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String pageStr = path.substring(path.lastIndexOf('/') + 1);
        int page = Integer.parseInt(pageStr);

        int totalPosts = getPostCountForUser(user.getId());
        int offset = totalPosts - (page + 1) * POSTS_LIMIT;


        List<Post> posts = VKService
                .getPostsForUser(user.getId(), offset, POSTS_LIMIT)
                .getItems();

        long[] ids = new long[posts.size()];
        for (int i = 0; i < ids.length; i++)
            ids[i] = posts.get(i).getId();

        Arrays.sort(ids);

        String json = toJson(new PostIndex(ids, totalPosts));
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private static Response createPostResponse(URL url) throws IOException {
        VKontakteProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String posId = path.substring(path.lastIndexOf('/') + 1);
        Post post = VKService.getPost(user.getId(), posId);
        String json = toJson(post);
        return new Response(url.toString(), json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
    }

    private static VKontakteProfile getUserProfile(String userDomain) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userDomain + "]:");
        }

        List<VKontakteProfile> profileList = VKService.getUsers(Arrays.asList(userDomain));
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userDomain + "]!");
        }
        return profileList.get(0);
    }

    private static String toJson(Object obj) {
        String json = Builder.getGson().toJson(obj);
        return json;
    }
}
