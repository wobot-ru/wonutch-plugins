package ru.wobot.vk;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.json.VKArray;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Service {
    private static final Log LOG = LogFactory.getLog(Service.class.getName());

    public static Response request(String urlString) throws MalformedURLException, UnsupportedEncodingException {
        URL url = new URL(urlString);
        String userId = url.getHost();
        if (UrlCheck.isProfile(url)) {
            return createProfileResponse(userId, urlString);
        }
        if (UrlCheck.isFriends(url)) {
            return createFriendsResponse(userId, urlString);
        }
        throw new UnsupportedOperationException();
    }

    private static Response createProfileResponse(String userId, String urlString) throws UnsupportedEncodingException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user profile: [" + userId + "]");
        }

        VKontakteProfile user = Proxy.getInctance().usersOperations().getUsers(Arrays.asList(userId)).get(0);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user profile [userId=" + user.getId() + "]");
        }

        Gson gson = new Gson();
        String json = gson.toJson(user);
        Response response = new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
        return response;
    }

    private static Response createFriendsResponse(String userId, String urlString) throws UnsupportedEncodingException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user profile: [" + userId + "]");
        }

        VKontakteProfile user = Proxy.getInctance().usersOperations().getUsers(Arrays.asList(userId)).get(0);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user profile [userId=" + user.getId() + "]");
            LOG.trace("Starting fetching user friends: [" + userId + "]");
        }

        VKArray<VKontakteProfile> friendArray = Proxy.getInctance().friendsOperations().get(user.getId());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user friends: [friends.size=" + friendArray.getCount() + "]");
        }

        List<VKontakteProfile> friends = friendArray.getItems();
        Gson gson = new Gson();
        String json = gson.toJson(friends);
        Response response = new Response(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
        return response;

    }
}
