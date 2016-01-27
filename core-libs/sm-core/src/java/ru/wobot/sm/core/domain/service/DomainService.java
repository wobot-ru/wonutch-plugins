package ru.wobot.sm.core.domain.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.wobot.sm.core.domain.SMContent;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SMFetcher;
import ru.wobot.sm.core.url.UrlCheck;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Сервис, получающий информацию (пользователи, комментарии, посты) из социальных медиа.
 * Для доступа к конкретной социальной сети использует реализацию интерфейса {@link SMFetcher}.
 * Используется на фазе fetch процесса обхода веб ресурсов.
 */
public class DomainService {

    public static final int POSTS_LIMIT = 100;
    private static final Log LOG = LogFactory.getLog(DomainService.class.getName());
    private static final Gson Gson = new GsonBuilder().create();
    private final SMFetcher smService;

    public DomainService(SMFetcher smService) {
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

    public SMContent request(String urlString) throws IOException {
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

    private SMContent createCommentPageResponse(URL url) throws IOException {
        String userDomain = url.getHost();
        SMProfile user = getUserProfile(userDomain);
        String path = url.getPath();
        String[] split = path.split("/");
        String postId = split[2];
        int page = Integer.parseInt(split[4]);

        FetchResponse fetchResponse = smService.getPostCommentsData(user.getId(), postId, page * POSTS_LIMIT, POSTS_LIMIT);
        return new SMContent(url.toString(), fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse.getMetadata());
    }

    private SMContent createProfileResponse(String userDomain, String urlString) throws IOException {
        SMProfile user = getUserProfile(userDomain);
        FetchResponse fetchResponse = smService.getProfileData(user.getId());
        return new SMContent(urlString, fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse.getMetadata());
    }

    private SMContent createFriendsResponse(String userDomain, String urlString) throws IOException {
        SMProfile user = getUserProfile(userDomain);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userDomain + "].friends:");
        }
        List<String> ids = smService.getFriendIds(user.getId());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userDomain + "]|.friends[count=" + ids.size() + "]!");
        }
        Collections.sort(ids);
        return new SMContent(urlString, toJson(ids).getBytes(StandardCharsets.UTF_8));

    }

    private SMContent createPostsIndexResponse(String userDomain, String urlString) throws IOException {
        SMProfile user = getUserProfile(userDomain);
        String json = toJson(getPostCountForUser(user.getId()));
        return new SMContent(urlString, json.getBytes(StandardCharsets.UTF_8));
    }

    // http://user/index-posts/x100/0000000001
    private SMContent createPostsIndexPageResponse(URL url) throws IOException {
        SMProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String pageStr = path.substring(path.lastIndexOf('/') + 1);
        int page = Integer.parseInt(pageStr);

        int totalPosts = getPostCountForUser(user.getId());
        int offset = totalPosts - (page + 1) * POSTS_LIMIT;

        FetchResponse fetchResponse = smService.getPostsData(user.getId(), offset, POSTS_LIMIT);
        return new SMContent(url.toString(), fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse.getMetadata());
    }

    private SMContent createPostResponse(URL url) throws IOException {
        SMProfile user = getUserProfile(url.getHost());

        String path = url.getPath();
        String posId = path.substring(path.lastIndexOf('/') + 1);
        FetchResponse fetchResponse = smService.getPostData(user.getId(), posId);
        return new SMContent(url.toString(), fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse.getMetadata());
    }

    private SMProfile getUserProfile(String userDomain) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user[id=" + userDomain + "]:");
        }

        List<SMProfile> profileList = smService.getProfiles(Collections.singletonList(userDomain));
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user[id=" + userDomain + "]!");
        }
        return profileList.get(0);
    }

    private String toJson(Object obj) {
        return Gson.toJson(obj);
    }
}
