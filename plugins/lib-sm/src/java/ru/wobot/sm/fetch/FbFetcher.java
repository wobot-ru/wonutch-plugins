package ru.wobot.sm.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.social.UncategorizedApiException;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.support.URIBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import ru.wobot.sm.core.api.FbApiTypes;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.Redirect;
import ru.wobot.sm.core.fetch.SuccessResponse;
import ru.wobot.sm.core.mapping.Sources;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;
import ru.wobot.uri.QueryParam;
import ru.wobot.uri.Scheme;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private static final String[] POST_FIELDS = {
            "id", "actions", "admin_creator", "application", "caption", "created_time", "description", "from{id,name}", "icon",
            "is_hidden", "is_published", "link", "message", "message_tags", "name", "object_id", "picture", "place",
            "privacy", "properties", "source", "status_type", "story", "to", "type", "updated_time",
            "with_tags", "shares", "likes.summary(true).limit(0)", "comments.summary(true).limit(0)"
    };

    private static final String[] COMMENT_FIELDS = {"message", "from", "like_count", "likes", "created_time",
            "parent{id}", "object{id}"};

    private static final String API_VERSION = "2.5";
    private static final String FACEBOOK_API_URI = "https://graph.facebook.com/v" + API_VERSION;
    private static final String FACEBOOK_URI = "https://www.facebook.com";

    private final CredentialRepository repository;
    private final CookieRepository cookieRepository;

    public FbFetcher(CredentialRepository repository, CookieRepository cookieRepository) {
        this.repository = repository;
        this.cookieRepository = cookieRepository;
    }

    private JsonNode getObject(MultiValueMap<String, String> queryParameters, String path) {
        Facebook facebook = new FacebookTemplate(repository.getInstance().getAccessToken());
        URIBuilder uriBuilder = URIBuilder.fromUri(facebook.getBaseGraphApiUrl() + path);
        return facebook.restOperations().getForObject(uriBuilder.queryParams(queryParameters).build(), JsonNode.class);
    }

    // fb://mastercardrussia
    // fb://96814974590
    @Path("{userId}")
    public FetchResponse getPageData(@PathParam("userId") String userId) throws IOException {
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
            put(ContentMetaConstants.API_TYPE, FbApiTypes.PROFILE);
        }};

        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.set("fields", StringUtils.arrayToCommaDelimitedString(PROFILE_FIELDS));
        try {
            return new SuccessResponse(getObject(queryParameters, userId).toString(), metaData);
        } catch (UncategorizedApiException e) {
            return new Redirect(Sources.FACEBOOK + "://" + userId + "/profile/app_scoped", metaData);
        }
    }

    // fb://1001830254956/profile?as_id=4664892664605&screen_name
    @Path("{userId}/profile")
    public FetchResponse getProfileData(@PathParam("userId") String userId,
                                        @QueryParam("as_id") final String appScopedUserId,
                                        @QueryParam("screen_name") String screenName,
                                        @QueryParam("auth") String auth
    ) throws IOException {
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.MIME_TYPE, "text/html");
            put("app.scoped.user.id", appScopedUserId);
        }};

        /*CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        HttpHost proxyHost = new HttpHost("41.77.139.98", 6060);
        credentialsProvider.setCredentials(new AuthScope(proxyHost), new UsernamePasswordCredentials("snt@wobot.co", "PfYZ7J(b<^<[rhm"));*/
        CloseableHttpClient client = HttpClientBuilder.create().
                disableRedirectHandling().
                /*setProxy(proxyHost).
                setDefaultCredentialsProvider(credentialsProvider).*/
                        setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy()).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(client);
        RestOperations restOperations = new RestTemplate(requestFactory);

        String url = FACEBOOK_URI + "/" + userId; // screen name
        if (screenName == null) {
            url = FACEBOOK_URI + "/profile.php?id=" + userId;
            /*String redirectUrl = restOperations.headForHeaders(url).getFirst("Location");
            if (redirectUrl != null) {
                byte[] b = new byte[redirectUrl.length()];
                for (int i = 0; i < b.length; i++) {
                    b[i] = (byte) redirectUrl.charAt(i);
                }
                url = new String(b, StandardCharsets.UTF_8);
            }*/
        }

       /* HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
        headers.add("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
        ResponseEntity<byte[]> response = restOperations.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);*/
        return new SuccessResponse(new HttpWebFetcher(null, cookieRepository).getHtmlPage(url), metaData);
    }

    // fb://1001830254956/profile/app_scoped
    @Path("{userId}/profile/app_scoped")
    public FetchResponse getProfileId(@PathParam("userId") String userId) throws IOException {
        Map<String, Object> metaData = new HashMap<String, Object>();

        List<HttpMessageConverter<?>> emptyList = new ArrayList<>();
        emptyList.add(new MappingJackson2HttpMessageConverter());
        RestOperations restOperations = new RestTemplate(emptyList);

        URI pictureLocation = restOperations.headForHeaders(FACEBOOK_API_URI + "/" + userId + "/picture").getLocation();
        String part = pictureLocation.getPath();
        String[] parts = part.substring(part.lastIndexOf("/") + 1).split("_");
        if (parts.length >= 2) {
            try {
                pictureLocation = restOperations.headForHeaders(FACEBOOK_URI + "/" + parts[0] + "_" + parts[1]).getLocation();
                if (pictureLocation.getPath().contains("photo")) {
                    List<NameValuePair> params = URLEncodedUtils.parse(pictureLocation, "UTF-8");
                    if (params.get(1).getName().equals("set")) {
                        String[] paramValues = params.get(1).getValue().split("\\.");
                        String factUserId = paramValues[paramValues.length - 1];
                        if (!factUserId.equals("499829591"))  // I don't know who is this (his name is Will Chengberg), but his profile has default pictures
                            return new Redirect(Sources.FACEBOOK + "://" + factUserId + "/profile?as_id=" + userId, metaData);
                    }
                }
            } catch (Exception e) {
                // Any way, return redirect to app scoped URL
            }
        }
        return new Redirect(Sources.FACEBOOK + "://" + userId + "/profile/app_scoped/auth", metaData);
    }

    // fb://1153183591398867/profile/app_scoped/auth
    @Path("{userId}/profile/app_scoped/auth")
    public FetchResponse getProfileIdAuth(@PathParam("userId") String userId) throws IOException {
        List<HttpMessageConverter<?>> emptyList = new ArrayList<>();
        emptyList.add(new StringHttpMessageConverter());
        RestOperations restOperations = new RestTemplate(emptyList);

        String cookies = StringUtils.collectionToDelimitedString(cookieRepository.getCookiesAsNameValuePairs(), "; ");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookies);

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restOperations.exchange(FACEBOOK_URI + "/app_scoped_user_id/" + userId,
                HttpMethod.HEAD, request, String.class);

        URI location = response.getHeaders().getLocation();
        String factId;
        boolean screenName = false;
        List<NameValuePair> params = URLEncodedUtils.parse(location, "UTF-8");
        if (!params.isEmpty() && params.get(0).getName().equals("id"))
            factId = params.get(0).getValue();
        else {
            factId = location.getPath().substring(1); // screen name (user name) (w/o leading slash)
            screenName = true;
        }
        return new Redirect(Sources.FACEBOOK + "://" + factId + "/profile?as_id=" + userId + (screenName ? "&screen_name" : ""), new HashMap<String, Object>());
    }

    // fb://1704938049732711/friends
    @Path("{userId}/friends")
    public SuccessResponse getFriendIds(@PathParam("userId") String userId) throws IOException {
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
            put(ContentMetaConstants.API_TYPE, FbApiTypes.FRIEND_LIST_OF_ID);
            put(ContentMetaConstants.SKIP_FROM_ELASTIC_INDEX, 1);
        }};

        return new SuccessResponse(result.toString(), metaData);
    }

    // fb://191234802505/index-posts/x100/00000000
    // fb://96814974590/index-posts/x100/1453725511
    @Path("{userId}/index-posts/x{limit}/{until}")
    public SuccessResponse getPostsData(@PathParam("userId") String userId,
                                        @PathParam("limit") int limit,
                                        @PathParam("until") long until
    ) throws IOException {
        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.set("fields", StringUtils.arrayToCommaDelimitedString(POST_FIELDS));
        if (until > 0)
            queryParameters.set("until", String.valueOf(until));
        if (limit > 0)
            queryParameters.set("limit", String.valueOf(limit));
        queryParameters.set("order", "reverse_chronological");

        JsonNode responseNode = getObject(queryParameters, userId + "/posts");
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_VERSION);
            put(ContentMetaConstants.API_TYPE, FbApiTypes.POST_BULK);
        }};
        return new SuccessResponse(responseNode.toString(), metaData);
    }

    // fb://165107853523677/posts/165107853523677_1081856348515485/x100/0
    // fb://165107853523677/posts/165107853523677_1081856348515485/x10/WTI5dGJXVnVkRjlqZFhKemIzSTZNVEE0TWpneU5qQXdOVEE0TlRFNE5qb3hORFV5TWpRd01qTTM=
    @Path("{userId}/posts/{postId}/x{limit}/{pageToken}")
    public SuccessResponse getPostCommentsData(@PathParam("userId") @SuppressWarnings("unused") String userId, // TODO: Think about refactor
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
            put(ContentMetaConstants.API_TYPE, FbApiTypes.COMMENT_BULK);
        }};
        return new SuccessResponse(responseNode.toString(), metaData);
    }
}
