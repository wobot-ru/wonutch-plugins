package ru.wobot.vk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.social.vkontakte.api.*;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import org.springframework.social.vkontakte.api.impl.json.VKontakteModule;
import org.springframework.social.vkontakte.api.impl.wall.CommentsQuery;
import org.springframework.social.vkontakte.api.impl.wall.CommunityWall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class VKService {
    private ObjectMapper objectMapper;

    public VKService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new VKontakteModule());
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
    }

    public List<VKontakteProfile> getUsers(List<String> userIds) throws IOException {
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

        URL url = new URL(uriBuilder.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String responseStr = readStreamToString(con.getInputStream());
        VKontakteProfiles profiles = objectMapper.readValue(responseStr, VKontakteProfiles.class);
        return profiles.getProfiles();
    }

    public VKArray<VKontakteProfile> getFriends(Long userId) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/friends.get")
                .setParameter("user_id", userId.toString())
                .setParameter("fields", "ckname,domain,sex,bdate,city,country,timezone,photo_50,photo_100,photo_200_orig,has_mobile,contacts,education,online,relation,last_seen,status,can_write_private_message,can_see_all_posts,can_post,universities")
                .setParameter("v", "5.40");

        URL url = new URL(uriBuilder.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String responseStr = readStreamToString(con.getInputStream());
        VKGenericResponse vkResponse = objectMapper.readValue(responseStr, VKGenericResponse.class);
        return deserializeVK50ItemsResponse(vkResponse, VKontakteProfile.class);
    }

    public Post getPost(Long userId, String postId) throws IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("api.vk.com").setPath("/method/wall.getById")
                .setParameter("posts", userId + "_" + postId)
                .setParameter("v", "5.40");

        URL url = new URL(uriBuilder.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String responseStr = readStreamToString(con.getInputStream());
        VKGenericResponse vkResponse = objectMapper.readValue(responseStr, VKGenericResponse.class);
        Post post = objectMapper.readValue(vkResponse.getResponse().get(0).toString(), Post.class);
        return post;
    }

    public VKArray<Post> getPostsForUser(Long userId, int offset, int limit) throws IOException {
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

        URL url = new URL(uriBuilder.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String responseStr = readStreamToString(con.getInputStream());
        VKGenericResponse vkResponse = objectMapper.readValue(responseStr, VKGenericResponse.class);
        checkForError(vkResponse);
        VKArray<Post> posts = deserializeVK50ItemsResponse(vkResponse, Post.class);
        return posts;

    }

    public CommentsResponse getComments(CommentsQuery query) throws IOException {
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

        URL url = new URL(uriBuilder.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String responseStr = readStreamToString(con.getInputStream());
        VKGenericResponse vkResponse = objectMapper.readValue(responseStr, VKGenericResponse.class);
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

    protected String readStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    protected <T> VKArray<T> deserializeVK50ItemsResponse(VKGenericResponse response, Class<T> itemClass) {
        JsonNode jsonNode = response.getResponse();
        JsonNode itemsNode = jsonNode.get("items");
        org.springframework.util.Assert.isTrue(itemsNode.isArray());
        int count = jsonNode.get("count").asInt();
        return new VKArray<T>(count, deserializeItems((ArrayNode) itemsNode, itemClass));
    }

    protected <T> List<T> deserializeItems(ArrayNode items, Class<T> itemClass) {
        List<T> elements = new ArrayList<T>();
        for (int i = 0; i < items.size(); i++) {
            elements.add(objectMapper.convertValue(items.get(i), itemClass));
        }
        return elements;
    }

    // throw exception if VKontakte response contains error
    // TODO: consider to throw specific exceptions for each error code.
    //       like for error code 113 that would be let's say InvalidUserIdVKException
    protected <T extends VKResponse> void checkForError(T toCheck) {
        if(toCheck.getError() != null) {
            throw new VKontakteErrorException(toCheck.getError());
        }
    }
}