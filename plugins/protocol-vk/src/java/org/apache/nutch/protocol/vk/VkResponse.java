package org.apache.nutch.protocol.vk;

import com.google.gson.Gson;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.protocol.Content;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import ru.wobot.vk.UrlCheck;
import ru.wobot.vk.dto.FriendListDto;
import ru.wobot.vk.dto.ProfileDto;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

public class VkResponse {
    private static Gson gson = new Gson();
    private String mimeType;
    private String defaultCharEncoding;
    private String orig;
    private Vk vk;
    private Configuration conf;
    private CrawlDatum datum;
    private byte[] data;
    private Metadata metadata;

    public VkResponse(URL url, CrawlDatum datum, Vk vk, Configuration conf)
            throws IOException {
        this.orig = url.toString();
        this.datum = datum;
        this.vk = vk;
        this.conf = conf;
        this.defaultCharEncoding = this.conf.get("parser.character.encoding.default", "UTF-8");
        this.mimeType = this.conf.get("mime.type.vk", "application/json");
        this.metadata = new Metadata();
        this.data = getData(url);
    }

    private byte[] getData(URL url) throws UnsupportedEncodingException {
        String path = url.getPath().toLowerCase();
        String userId = url.getHost();
        String json = null;
        if (UrlCheck.isProfile(url)) {
            json = GetProfileJson(userId);
        }
        if (UrlCheck.isFriends(url)) {
            json = GetFriendsJson(userId);
        }
        if (json != null) {
            return json.getBytes(defaultCharEncoding);
        }
        return new byte[0];
    }

    private String GetProfileJson(String userId) {
        ProfileDto dto = new ProfileDto();
        dto.user = Proxy.getInctance().usersOperations().getUser(userId);
        return gson.toJson(dto);
    }

    private String GetFriendsJson(String userId) {
        FriendListDto dto = new FriendListDto();
        VKArray<VKontakteProfile> vkArray = Proxy.getInctance().friendsOperations().get(userId);
        dto.friends = vkArray.getItems();
        return gson.toJson(dto);
    }

    public Content toContent() throws UnsupportedEncodingException {
        return new Content(this.orig, this.orig, this.data, this.mimeType, this.metadata, this.conf);
    }
}