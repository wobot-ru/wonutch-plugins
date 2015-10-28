package ru.wobot.vk;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import ru.wobot.vk.dto.FriendListDto;
import ru.wobot.vk.dto.ProfileDto;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class VkService {
    private static final Log LOG = LogFactory.getLog(VkService.class.getName());

    public static VkResponse request(String urlString) throws MalformedURLException, UnsupportedEncodingException {
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

    private static VkResponse createProfileResponse(String userId, String urlString) throws UnsupportedEncodingException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user profile: [" + userId + "]");
        }

        ProfileDto dto = new ProfileDto();
        dto.user = Proxy.getInctance().usersOperations().getUsers(Arrays.asList(userId)).get(0);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user profile [userId=" + dto.user.getId() + "]");
        }

        Gson gson = new Gson();
        String json = gson.toJson(dto);
        VkResponse vkResponse = new VkResponse(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
        return vkResponse;
    }

    private static VkResponse createFriendsResponse(String userId, String urlString) throws UnsupportedEncodingException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting fetching user profile: [" + userId + "]");
        }

        FriendListDto dto = new FriendListDto();
        VKontakteProfile user = Proxy.getInctance().usersOperations().getUsers(Arrays.asList(userId)).get(0);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user profile [userId=" + user.getId() + "]");
            LOG.trace("Starting fetching user friends: [" + userId + "]");
        }

        VKArray<VKontakteProfile> profiles = Proxy.getInctance().friendsOperations().get(user.getId());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finished fetching user friends: [friends.size=" + profiles.getCount() + "]");
        }

        dto.friends = profiles.getItems();
        Gson gson = new Gson();
        String json = gson.toJson(dto);
        VkResponse vkResponse = new VkResponse(urlString, json.getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());
        return vkResponse;

    }
}
