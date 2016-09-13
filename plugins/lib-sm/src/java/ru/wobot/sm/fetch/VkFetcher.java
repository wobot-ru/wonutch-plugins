package ru.wobot.sm.fetch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.social.vkontakte.api.ApiVersion;
import org.springframework.social.vkontakte.api.Comment;
import org.springframework.social.vkontakte.api.CommentsResponse;
import org.springframework.social.vkontakte.api.Group;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKGenericResponse;
import org.springframework.social.vkontakte.api.VKResponse;
import org.springframework.social.vkontakte.api.VKontakteErrorException;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.VKontakteProfiles;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import org.springframework.social.vkontakte.api.impl.json.VKontakteModule;
import org.springframework.social.vkontakte.api.impl.wall.CommentsQuery;
import org.springframework.social.vkontakte.api.impl.wall.CommunityWall;
import org.springframework.social.vkontakte.api.impl.wall.UserWall;
import ru.wobot.sm.core.api.VkApiTypes;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.auth.TooManyRequestsException;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.AccessDenied;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.Redirect;
import ru.wobot.sm.core.fetch.SuccessResponse;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;
import ru.wobot.uri.QueryParam;
import ru.wobot.uri.Scheme;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.wobot.sm.serialize.Serializer.getInstance;

@Scheme("vk")
public class VkFetcher {
    public static final String API_v5_40 = "5.40";
    private final ObjectMapper objectMapper;
    private final CredentialRepository repository;

    public VkFetcher(CredentialRepository repository) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new VKontakteModule());
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        this.repository = repository;
    }

    protected static String toJson(Object obj) {
        return getInstance().toJson(obj);
    }

    public List<SMProfile> getProfiles(List<String> userIds) throws IOException {
        String responseStr = getVKProfiles(userIds);
        VKontakteProfiles profiles = objectMapper.readValue(responseStr, VKontakteProfiles.class);
        checkForError(profiles);
        List<SMProfile> results = new ArrayList<>(profiles.getProfiles().size());
        for (VKontakteProfile profile : profiles.getProfiles()) {
            String domain = profile.getDomain();
            if (domain == null) domain = profile.getScreenName();
            results.add(new SMProfile(String.valueOf(profile.getId()), domain, profile.getFirstName() + " " + profile.getLastName()));
        }
        return results;
    }

    @Path("id{userId}/friends")
    public FetchResponse getFriendIds(@PathParam("userId") String userId) throws IOException {
        Map<String, Object> metaData = getMetaData(VkApiTypes.FRIEND_LIST_OF_ID, true);

        URIBuilder uriBuilder = new URIBuilder()
                .setScheme("http")
                .setHost("api.vk.com")
                .setPath("/method/friends.get")
                .setParameter("user_id", userId)
                .setParameter("fields", "domain")
                .setParameter("v", API_v5_40);

        try {
            VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
            VKArray<VKontakteProfile> friends = deserializeVK50ItemsResponse(vkResponse, VKontakteProfile.class);
            List<String> ids = new ArrayList<>(friends.getItems().size());
            for (VKontakteProfile p : friends.getItems()) {
                ids.add("id" + p.getId());
            }
            Collections.sort(ids);
            return new SuccessResponse(toJson(ids), metaData);
        } catch (VKontakteErrorException e) {
            if (e.getError().getCode().equals("15"))
                return new AccessDenied(e.getMessage(), metaData);
            else throw e;
        }
    }

    @Path("id{userId}/index-posts")
    public FetchResponse getPostCount(@PathParam("userId") String userId, @QueryParam("auth") String auth) throws IOException {
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_v5_40);
            put(ContentMetaConstants.API_TYPE, VkApiTypes.POST_COUNT);
            put(ContentMetaConstants.SKIP_FROM_ELASTIC_INDEX, 1);
        }};

        URIBuilder uriBuilder = new URIBuilder()
                .setScheme("http")
                .setHost("api.vk.com")
                .setPath("/method/wall.get")
                .setParameter("owner_id", userId)
                .setParameter("v", API_v5_40);

        if (auth != null) {
            preProcessURI(uriBuilder);
        }

        try {
            VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
            VKArray<Post> posts = deserializeVK50ItemsResponse(vkResponse, Post.class);
            return new SuccessResponse(toJson(posts.getCount()), metaData);
        } catch (VKontakteErrorException e) {
            switch (e.getError().getCode()) {
                case "15":
                    return new Redirect("vk://id" + userId + "/index-posts?auth", metaData);
                case "18":
                    return new AccessDenied(e.getMessage(), metaData);
                default:
                    throw e;
            }
        }
    }

    @Path("id{userId}/index-posts/x{pageSize}/{page}")
    public FetchResponse getPostsData(@PathParam("userId") String userId, @PathParam("pageSize") long pageSize, @PathParam("page") int page, @QueryParam("auth") String auth) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/wall.get")
                .setParameter("owner_id", userId)
                .setParameter("v", API_v5_40);
        if (auth != null) {
            preProcessURI(uriBuilder);
        }
        if (page > 0) {
            uriBuilder.setParameter("offset", String.valueOf(pageSize * page));
        }
        if (pageSize > 0) {
            uriBuilder.setParameter("count", String.valueOf(pageSize));
        }

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        VKArray<Post> posts = deserializeVK50ItemsResponse(vkResponse, Post.class);
        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_v5_40);
            put(ContentMetaConstants.API_TYPE, VkApiTypes.POST_BULK);
        }};
        return new SuccessResponse(toJson(posts), metaData);
    }

    @Path("{groupId}/topics/x{pageSize}/{page}")
    public FetchResponse getGroupTopicsData(@PathParam("groupId") String groupId,
                                            @PathParam("pageSize") long pageSize,
                                            @PathParam("page") int page) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/board.getTopics")
                .setParameter("group_id", groupId)
                .setParameter("order", "1")
                .setParameter("v", API_v5_40);
       /* if (auth != null) {
            preProcessURI(uriBuilder);
        }*/
        if (page > 0) {
            uriBuilder.setParameter("offset", String.valueOf(pageSize * page));
        }
        if (pageSize > 0) {
            uriBuilder.setParameter("count", String.valueOf(pageSize));
        }

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        return new SuccessResponse(vkResponse.getResponse().toString(), getMetaData(VkApiTypes.TOPIC_BULK, false));
    }

    @Path("{userId}")
    public FetchResponse getProfileData(@PathParam("userId") String userId) throws IOException {
        String responseStr = getVKProfiles(Collections.singletonList(userId));
        VKontakteProfiles profiles = objectMapper.readValue(responseStr, VKontakteProfiles.class);
        checkForError(profiles);
        final VKontakteProfile profile = profiles.getProfiles().get(0);
        Map<String, Object> metaData = getMetaData(VkApiTypes.PROFILE, false);

        if (!userId.startsWith("id")) {
            return new Redirect("vk://id" + profile.getId(), metaData);
        }

        String json = toJson(profile);
        return new SuccessResponse(json, metaData);
    }

    @Path("{groupId}/group")
    public FetchResponse getGroupData(@PathParam("groupId") String groupId) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/groups.getById")
                .setParameter("group_ids", groupId)
                .setParameter("fields", "city,country,counters,description,wiki_page,members_count,start_date,finish_date,status,site")
                .setParameter("v", API_v5_40);

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        Group group = objectMapper.readValue(vkResponse.getResponse().get(0).toString(), Group.class);

        String json = toJson(group);
        return new SuccessResponse(json, getMetaData(VkApiTypes.GROUP, false));
    }

    @Path("id{userId}/posts/{postId}")
    public FetchResponse getPostData(@PathParam("userId") String userId, @PathParam("postId") String postId) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/wall.getById")
                .setParameter("posts", userId + "_" + postId)
                .setParameter("v", API_v5_40);

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        Post post = objectMapper.readValue(vkResponse.getResponse().get(0).toString(), Post.class);

        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_v5_40);
            put(ContentMetaConstants.API_TYPE, VkApiTypes.POST);
        }};
        String json = toJson(post);
        return new SuccessResponse(json, metaData);
    }

    public FetchResponse tender() throws IOException, ParseException {
        URIBuilder uriBuilder = new URIBuilder();
        FileWriter writer = new FileWriter("d://tmp//csv_new//34759-s.csv");

        String startFrom = "";
        while (startFrom != null) {
            uriBuilder.setScheme("https").setHost("api.vk.com").setPath("/method/newsfeed.search")
                    .setParameter("q", "человек муравей")
                    .setParameter("count", "200")
                    .setParameter("start_time",
                            String.valueOf(new SimpleDateFormat("yyyyMMddHHmm").parse("201501070000").getTime() / 1000))
                    .setParameter("end_time",
                            String.valueOf(new SimpleDateFormat("yyyyMMddHHmm").parse("201501090000").getTime() / 1000))
                    //.setParameter("extended", "1")
                    //.setParameter("offset", String.valueOf(i))
                    .setParameter("access_token", "2ff5fff8d49ffa2da2875c99faf61399f5480ee10a2d70dde44027b9b248eae6a014d7689f7d0f216ab00")
                    .setParameter("v", "5.52");
            if (!startFrom.isEmpty())
                uriBuilder.setParameter("start_from", startFrom);

            VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
            for (JsonNode node : vkResponse.getResponse().get("items")) {
                writer.append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").
                        format(new Date(node.get("date").asLong() * 1000)));
                writer.append(',');

                String text = node.get("text").asText();
                JsonNode att = node.get("attachments");
                if (att != null) {
                    String type = att.get(0).get("type").asText();
                    switch (type) {
                        case "video":
                            text += " " + att.get(0).get("video").get("title").asText() + " " + att.get(0).get("video").get("description").asText();
                            break;
                        case "photo":
                            text += " " + att.get(0).get("photo").get("text").asText();
                            break;
                        case "link":
                            text += " " + att.get(0).get("link").get("title").asText() + " " + att.get(0).get("link").get("description").asText();
                            break;
                    }
                }

                if (text.trim().isEmpty())
                    continue;

                writer.append("\"" + text.replace('\n', ' ').trim() + "\"");
                writer.append(',');
                writer.append("http://vk.com/wall" + node.get("from_id").asText() + "_" + node.get("id").asText());
                writer.append(',');
                writer.append(node.get("from_id").asText());
                writer.append('\n');
            }

            startFrom = vkResponse.getResponse().get("next_from") != null
                    ? vkResponse.getResponse().get("next_from").asText() : null;
        }
        writer.flush();
        writer.close();
        return new SuccessResponse("", new HashMap<String, Object>());
    }

    @Path("id{userId}/posts/{postId}/x{pageSize}/{page}")
    public FetchResponse getPostCommentsData(
            @PathParam("userId") String userId,
            @PathParam("postId") String postId,
            @PathParam("pageSize") int pageSize,
            @PathParam("page") int page,
            @QueryParam("auth") String auth) throws IOException {
        CommentsQuery query = new CommentsQuery
                .Builder(new UserWall(Integer.parseInt(userId)), Integer.parseInt(postId))
                .needLikes(true)
                .extended(true)
                .count(pageSize)
                .offset(page * pageSize)
                .build();

        Map<String, Object> metaData = new HashMap<String, Object>() {{
            put(ContentMetaConstants.API_VER, API_v5_40);
            put(ContentMetaConstants.API_TYPE, VkApiTypes.COMMENT_BULK);
        }};

        try {
            String json = toJson(getComments(query, auth != null));
            return new SuccessResponse(json, metaData);
        } catch (VKontakteErrorException e) {
            if (e.getError().getCode().equals("212"))
                return new AccessDenied(e.getMessage(), metaData);
            else
                throw e;
        }
    }

    @Path("{groupId}/topics/{topicId}/x{pageSize}/{page}")
    public FetchResponse getTopicCommentsData(
            @PathParam("groupId") String groupId,
            @PathParam("topicId") String topicId,
            @PathParam("pageSize") int pageSize,
            @PathParam("page") int page) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/board.getComments")
                .setParameter("group_id", groupId)
                .setParameter("topic_id", topicId)
                .setParameter("sort", "desc")
                .setParameter("need_likes", "1")
                .setParameter("extended", "1")
                .setParameter("v", String.valueOf(ApiVersion.VERSION_5_33));

        if (page > 0) {
            uriBuilder.setParameter("offset", String.valueOf(pageSize * page));
        }
        if (pageSize > 0) {
            uriBuilder.setParameter("count", String.valueOf(pageSize));
        }

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        return new SuccessResponse(vkResponse.getResponse().toString(), getMetaData(VkApiTypes.TOPIC_COMMENT_BULK, false));
    }

    protected CommentsResponse getComments(CommentsQuery query, boolean needAuth) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/wall.getComments")
                .setParameter("v", String.valueOf(ApiVersion.VERSION_5_33));

        if (needAuth)
            preProcessURI(uriBuilder);

        if (query.owner instanceof CommunityWall) {
            uriBuilder.setParameter("owner_id", "-" + String.valueOf(query.owner.getId()));
        } else {
            uriBuilder.setParameter("owner_id", String.valueOf(query.owner.getId()));
        }

        uriBuilder.setParameter("post_id", String.valueOf(query.postId));

        if (query.needLikes) {
            uriBuilder.setParameter("need_likes", "1");
        }

        if (query.startCommentId != null && query.startCommentId > 0) {
            uriBuilder.setParameter("start_comment_id", query.startCommentId.toString());
        }

        if (query.offset != null && query.offset > 0) {
            uriBuilder.setParameter("offset", query.offset.toString());
        }

        if (query.count != null && query.count > 0) {
            uriBuilder.setParameter("count", query.count.toString());
        }

        if (query.sort != null) {
            uriBuilder.setParameter("sort", query.sort.toString());
        }

        if (query.previewLength != null && query.previewLength > 0) {
            uriBuilder.setParameter("preview_length", query.previewLength.toString());
        } else {
            //Specify 0 as it does not want to truncate comments.
            uriBuilder.setParameter("preview_length", "0");
        }

        if (query.extended) {
            uriBuilder.setParameter("extended", "1");
        }

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        List<Comment> comments = deserializeVK50ItemsResponse(vkResponse, Comment.class).getItems();
        List<VKontakteProfile> profiles = null;
        List<Group> groups = null;
        if (query.extended) {
            profiles = deserializeItems((ArrayNode) vkResponse.getResponse().get("profiles"), VKontakteProfile.class);
            groups = deserializeItems((ArrayNode) vkResponse.getResponse().get("groups"), Group.class);
        }
        long count = vkResponse.getResponse().get("count").asLong();
        Long realOffset = null;
        if (query.startCommentId != null) {
            realOffset = vkResponse.getResponse().get("real_offset").asLong();
        }
        return new CommentsResponse(comments, count, realOffset, profiles, groups);
    }

    protected String getVKProfiles(List<String> userIds) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/users.get")
                .setParameter("fields", "sex,bdate,city,country,photo_50,photo_100,photo_200_orig,photo_200,photo_400_orig,photo_max,photo_max_orig,photo_id,online,online_mobile,domain,has_mobile,contacts,connections,site,education,universities,schools,can_post,can_see_all_posts,can_see_audio,can_write_private_message,status,last_seen,relation,relatives,counters,screen_name,maiden_name,timezone,occupation,activities,interests,music,movies,tv,books,games,about,quotes,personal,friend_status,military,career")
                .setParameter("v", API_v5_40);

        if (userIds != null) {
            StringBuilder sb = new StringBuilder();
            for (String uid : userIds) {
                sb.append(uid).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            uriBuilder.setParameter("user_ids", sb.toString());
        }

        return readUrlToString(uriBuilder.toString());
    }

    private Map<String, Object> getMetaData(final String apiType, boolean skipFromIndex) {
        Map<String, Object> meta = new HashMap<>();
        meta.put(ContentMetaConstants.API_VER, API_v5_40);
        meta.put(ContentMetaConstants.API_TYPE, apiType);
        if (skipFromIndex)
            meta.put(ContentMetaConstants.SKIP_FROM_ELASTIC_INDEX, 1);
        return meta;
    }

    protected <T> VKArray<T> deserializeVK50ItemsResponse(VKGenericResponse response, Class<T> itemClass) {
        JsonNode jsonNode = response.getResponse();
        JsonNode itemsNode = jsonNode.get("items");
        org.springframework.util.Assert.isTrue(itemsNode.isArray());
        int count = jsonNode.get("count").asInt();
        return new VKArray<>(count, deserializeItems((ArrayNode) itemsNode, itemClass));
    }

    protected <T> List<T> deserializeItems(ArrayNode items, Class<T> itemClass) {
        List<T> elements = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            elements.add(objectMapper.convertValue(items.get(i), itemClass));
        }
        return elements;
    }

    protected VKGenericResponse getGenericResponse(String url) throws IOException {
        String responseStr = readUrlToString(url);
        VKGenericResponse response = objectMapper.readValue(responseStr, VKGenericResponse.class);
        checkForError(response);
        return response;
    }

    protected void preProcessURI(URIBuilder uri) {
        uri.setScheme("https");
        uri.addParameter("access_token", repository.getInstance().getAccessToken());
    }

    protected String readUrlToString(String urlStr) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            is = con.getInputStream();
            return readStreamToString(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    protected String readStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    // throw exception if VKontakte response contains error
    // TODO: consider to throw specific exceptions for each error code.
    //       like for error code 113 that would be let's say InvalidUserIdVKException
    protected <T extends VKResponse> void checkForError(T toCheck) {
        if (toCheck.getError() != null) {
            if (toCheck.getError().getCode().equals("6"))
                //todo: replace to something smart
                throw new TooManyRequestsException(-1);
            else
                throw new VKontakteErrorException(toCheck.getError());
        }
    }

}