package ru.wobot.vk;

import com.google.gson.Gson;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import ru.wobot.vk.dto.FriendListDto;
import ru.wobot.vk.dto.ProfileDto;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Parser {
    public static ParseResult parse(String urlString, byte[] data) throws MalformedURLException {
        URL url = new URL(urlString);
        String userId = url.getHost();
        String content = new String(data, StandardCharsets.UTF_8);

        if (UrlCheck.isProfile(url)) {
            return createProfileParse(userId, urlString, content);
        }
        if (UrlCheck.isFriends(url)) {
            return createFriendsParse(userId, urlString, content);
        }
        throw new UnsupportedOperationException();
    }

    private static ParseResult createProfileParse(String userId, String urlString, String content) {
        HashMap<String, String> links = new HashMap<String, String>() {
            {
                put(urlString + "/friends", userId + "-friends"); // generate link <a href='http://user/friends'>user-friends</a>
            }
        };
        Gson gson = new Gson();
        ProfileDto dto = gson.fromJson(content, ProfileDto.class);
        String title = getFullName(dto.user);
        return new ParseResult(urlString, title, content, links);
    }

    private static ParseResult createFriendsParse(String userId, String urlString, String content) {
        Gson gson = new Gson();
        FriendListDto dto = gson.fromJson(content, FriendListDto.class);
        HashMap<String, String> links = new HashMap<>(dto.friends.size());
        for (VKontakteProfile friend : dto.friends) {
            String friendHref = "http://" + friend.getScreenName();
            links.put(friendHref, getFullName(friend));
        }
        String title = userId + "-friends";
        return new ParseResult(urlString, title, content, links);
    }

    private static String getFullName(VKontakteProfile user) {
        String name = user.getFirstName() + " " + user.getLastName();
        return name;
    }
}
