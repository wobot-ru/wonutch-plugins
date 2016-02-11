package ru.wobot.sm.core.domain.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.wobot.sm.core.Sources;
import ru.wobot.sm.core.domain.SMContent;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SMFetcher;
import ru.wobot.sm.core.url.UrlCheck;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

    public SMContent request(String urlString) throws IOException, URISyntaxException {
        URI uri = new URI(urlString);
        String userDomain = uri.getHost();
        if (UrlCheck.isProfile(uri)) {
            return createProfileResponse(uri);
        }
        if (UrlCheck.isFriends(uri)) {
            return createFriendsResponse(userDomain, urlString);
        }
        if (UrlCheck.isPostsIndex(uri)) {
            return createPostsIndexResponse(userDomain, urlString);
        }
        if (UrlCheck.isPostsIndexPage(uri)) {
            return createPostsIndexPageResponse(uri);
        }
        if (UrlCheck.isPost(uri)) {
            return createPostResponse(uri);
        }
        if (UrlCheck.isCommentPage(uri)) {
            return createCommentPageResponse(uri);
        }

        throw new UnsupportedOperationException();
    }

    private SMContent createCommentPageResponse(URI uri) throws IOException {
        String userDomain = uri.getHost();
        SMProfile user = getUserProfile(userDomain);
        String path = uri.getPath();
        String[] split = path.split("/");
        String postId = split[2];
        int page = Integer.parseInt(split[4]);
        //TODO: HACK - remove
        String param = null;
        if (uri.getScheme().equals(Sources.VKONTAKTE)) {
            param = user.getId();
        } else if (uri.getScheme().equals(Sources.FACEBOOK)) {
            String queryParam = uri.getQuery();
            if (queryParam != null)
                param = queryParam.substring(queryParam.indexOf("=") + 1);
        }

        FetchResponse fetchResponse = smService.getPostCommentsData(param, postId, POSTS_LIMIT, page * POSTS_LIMIT
        );
        return new SMContent(uri.toString(), fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse.getMetadata());
    }

    private SMContent createProfileResponse(URI uri) throws IOException {
        String userDomain = uri.getHost();
        //TODO: rewrite for different scopes
        if (uri.getQuery() != null)
            userDomain += "?" + uri.getQuery();

        SMProfile user = getUserProfile(userDomain);
        FetchResponse fetchResponse = smService.getProfileData(user.getId());
        return new SMContent(uri.toString(), fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse
                .getMetadata());
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
    private SMContent createPostsIndexPageResponse(URI uri) throws IOException {
        SMProfile user = getUserProfile(uri.getHost());

        String path = uri.getPath();
        String pageStr = path.substring(path.lastIndexOf('/') + 1);
        long offset = 0;
        //TODO: HACK - remove
        if (uri.getScheme().equals(Sources.VKONTAKTE)) {
            int page = Integer.parseInt(pageStr);
            int totalPosts = getPostCountForUser(user.getId());
            offset = totalPosts - (page + 1) * POSTS_LIMIT;
        } else if (uri.getScheme().equals(Sources.FACEBOOK)) {
            offset = Long.parseLong(pageStr);
        }

        FetchResponse fetchResponse = smService.getPostsData(user.getId(), offset, POSTS_LIMIT);
        return new SMContent(uri.toString(), fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse.getMetadata());
    }

    private SMContent createPostResponse(URI uri) throws IOException {
        SMProfile user = getUserProfile(uri.getHost());

        String path = uri.getPath();
        String posId = path.substring(path.lastIndexOf('/') + 1);
        FetchResponse fetchResponse = smService.getPostData(user.getId(), posId);
        return new SMContent(uri.toString(), fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse.getMetadata());
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
