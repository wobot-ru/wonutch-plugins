package ru.wobot.vk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.social.vkontakte.api.*;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import org.springframework.social.vkontakte.api.impl.json.VKontakteModule;
import org.springframework.social.vkontakte.api.impl.wall.CommentsQuery;
import org.springframework.social.vkontakte.api.impl.wall.CommunityWall;
import org.springframework.social.vkontakte.api.impl.wall.UserWall;
import ru.wobot.smm.core.dto.SMProfile;
import ru.wobot.smm.core.SMService;
import ru.wobot.vk.serialize.Builder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VKService implements SMService {
    private ObjectMapper objectMapper;

    public VKService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new VKontakteModule());
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
    }

    @Override
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

    @Override
    public List<String> getFriendIds(String userId) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/friends.get")
                .setParameter("user_id", userId.toString())
                .setParameter("fields", "domain")
                .setParameter("v", "5.40");

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        VKArray<VKontakteProfile> friends = deserializeVK50ItemsResponse(vkResponse, VKontakteProfile.class);
        List<String> ids = new ArrayList<>(friends.getItems().size());
        for (VKontakteProfile p : friends.getItems()) {
            //todo: определиться, что мы храним "id" или "screenName a.k.a. domain"
            ids.add(p.getDomain());
        }
        return ids;
    }

    @Override
    public int getPostCount(String userId) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/wall.get")
                .setParameter("owner_id", userId.toString())
                .setParameter("v", "5.40");

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        VKArray<Post> posts = deserializeVK50ItemsResponse(vkResponse, Post.class);
        return posts.getCount();
    }

    @Override
    public List<String> getPostIds(String userId, int offset, int limit) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/wall.get")
                .setParameter("owner_id", userId.toString())
                .setParameter("v", "5.40");

        if (offset >= 0) {
            uriBuilder.setParameter("offset", String.valueOf(offset));
        }
        if (limit > 0) {
            uriBuilder.setParameter("count", String.valueOf(limit));
        }

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        VKArray<Post> posts = deserializeVK50ItemsResponse(vkResponse, Post.class);
        List<String> ids = new ArrayList<>(posts.getItems().size());
        for (int i = 0; i < posts.getItems().size(); i++)
            ids.add(String.valueOf(posts.getItems().get(i).getId()));
        return ids;
    }

    @Override
    public String getProfileData(String userId) throws IOException {
        String responseStr = getVKProfiles(Arrays.asList(userId));
        VKontakteProfiles profiles = objectMapper.readValue(responseStr, VKontakteProfiles.class);
        checkForError(profiles);
        return toJson(profiles.getProfiles().get(0));
    }

    @Override
    public String getPostData(String userId, String postId) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/wall.getById")
                .setParameter("posts", userId + "_" + postId)
                .setParameter("v", "5.40");

        VKGenericResponse vkResponse = getGenericResponse(uriBuilder.toString());
        Post post = objectMapper.readValue(vkResponse.getResponse().get(0).toString(), Post.class);
        return toJson(post);
    }

    @Override
    public String getPostCommentData(String userId, String postId, int skip, int take) throws IOException {
        CommentsQuery query = new CommentsQuery
                .Builder(new UserWall(Integer.parseInt(userId)), Integer.parseInt(postId))
                .needLikes(true)
                .count(take)
                .offset(skip)
                .build();

        return toJson(getComments(query));
    }

    protected CommentsResponse getComments(CommentsQuery query) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/wall.getComments")
                .setParameter("v", String.valueOf(ApiVersion.VERSION_5_33));

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
                .setParameter("v", "5.40");

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

    protected static String toJson(Object obj) {
        String json = Builder.getGson().toJson(obj);
        return json;
    }

    protected <T> VKArray<T> deserializeVK50ItemsResponse(VKGenericResponse response, Class<T> itemClass) {
        JsonNode jsonNode = response.getResponse();
        JsonNode itemsNode = jsonNode.get("items");
        org.springframework.util.Assert.isTrue(itemsNode.isArray());
        int count = jsonNode.get("count").asInt();
        return new VKArray<>(count, deserializeItems((ArrayNode) itemsNode, itemClass));
    }

    protected <T> List<T> deserializeItems(ArrayNode items, Class<T> itemClass) {
        List<T> elements = new ArrayList<T>();
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
            throw new VKontakteErrorException(toCheck.getError());
        }
    }
}