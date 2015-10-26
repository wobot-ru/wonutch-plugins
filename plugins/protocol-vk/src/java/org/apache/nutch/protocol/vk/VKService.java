package org.apache.nutch.protocol.vk;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKObject;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.VKontakteTemplate;
import org.springframework.social.vkontakte.api.impl.json.VKArray;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by cloudera on 10/15/15.
 */
public class VKService {
    private static URL proxy;
    private static Gson gson;

    static {
        java.net.URL temp;
        try {
            temp = new URL("http://localhost:9022/");
            gson = new Gson();
        } catch (java.net.MalformedURLException e) {
            temp = null;
        }
        proxy = temp;
    }

    public String getDisplayType(URL url) throws MalformedURLException {
        String path = url.getPath().toLowerCase();
        if (path == "" || url.getPath().equals("/")) {
            return "user";
        }
        if (path.contains("friends")) {
            return "friends";
        }
        if (path.contains("/posts/")) {
            return "post";
        }
        if (path.contains("posts")) {
            return "posts";
        }
        throw new MalformedURLException();
    }

    public String request(URL url) throws MalformedURLException {
        String path = url.getPath().toLowerCase();
        String userId = url.getHost();
        if (path == "" || url.getPath().equals("/")) {
            return getProfile(userId);
        }
        if (path.contains("friends")) {
            return getFriends(userId);
        }
        if (path.contains("/posts/")) {
            String postId = path.split("/")[2];
            return getPost(userId, postId);
        }
        if (path.contains("posts")) {
            return getPosts(userId);
        }
        throw new MalformedURLException();
    }

    private String getPost(String userId, String postId) {
        StringBuffer reqStr = new StringBuffer("<!DOCTYPE html>\n");
        reqStr.append("<html>\n");
        reqStr.append("<head>\n");
        reqStr.append("  <title>" + userId + " " + postId + "</title>\n");
        reqStr.append("</head>\n");
        reqStr.append("<body>\n");
        VKontakteTemplate vk = createTemplate();
        VKObject user = vk.utilsOperations().resolveScreenName(userId);
        Post post = vk.wallOperations().getPost(user.getId(), postId);
        if (post != null) {
            reqStr.append(post.getText());
        }
        reqStr.append("</body>\n");
        reqStr.append("</html>");
        return reqStr.toString();
    }

    private String getPosts(String userId) {
        StringBuffer reqStr = new StringBuffer("<!DOCTYPE html>\n");
        reqStr.append("<html>\n");
        reqStr.append("<head>\n");
        reqStr.append("  <title>" + userId + " posts</title>\n");
        reqStr.append("</head>\n");
        reqStr.append("<body>\n");
        VKontakteTemplate vk = createTemplate();
        VKObject user = vk.utilsOperations().resolveScreenName(userId);
        List<Post> posts = vk.wallOperations().getPostsForUser(user.getId(), 0, 100);
        for (Post post : posts) {
            String href = "http://" + userId + "/posts/" + post.getId() + "/";
            //reqStr.append("<a href=\"" + href + "\">Post" + post.getId() + "</a>\n");
            reqStr.append("<a href=\"" + href + "\"> </a>\n");
        }
        reqStr.append("</body>\n");
        reqStr.append("</html>");
        return reqStr.toString();
    }

    public String requestJson(URL url) throws MalformedURLException {
        String path = url.getPath().toLowerCase();
        String userId = url.getHost();
        if (path == "" || url.getPath().equals("/")) {
            return getProfileJson(userId);
        }
        if (path.contains("friends")) {
            return getFriendsJson(userId);
        }
        if (path.contains("/posts/")) {
            String postId = path.split("/")[2];
            return getPostJson(userId, postId);
        }
        if (path.contains("posts")) {
            return "{}";
        }
        throw new MalformedURLException();
    }

    private String getPostJson(String userId, String postId) {
        VKontakteTemplate vk = createTemplate();
        VKObject user = vk.utilsOperations().resolveScreenName(userId);
        Post post = vk.wallOperations().getPost(user.getId(), postId);
        return gson.toJson(post);
    }

    private String getProfileJson(String userId) {
        VKontakteTemplate vk = createTemplate();
        VKontakteProfile profile = vk.usersOperations().getUser(userId);
        return gson.toJson(profile);
    }

    private String getFriendsJson(String userId) {
/*
        VKontakteTemplate vk = createTemplate();
        VKontakteProfile profile = vk.usersOperations().getUser(userId);
*/
        VKontakteTemplate vk = createTemplate();
        VKArray<VKontakteProfile> friends = vk.friendsOperations().get(userId);
        return gson.toJson(friends);
    }

    private String getFriends(String userId) {
        String[] userIds = {userId};
        VKontakteTemplate vk = createTemplate();
        VKontakteProfile profile = vk.usersOperations().getUser(userId);
        String fullName = profile.getFirstName() + " " + profile.getLastName();
        StringBuffer reqStr = new StringBuffer("<!DOCTYPE html>\n");
        reqStr.append("<html>\n");
        reqStr.append("<head>\n");
        reqStr.append("  <title>" + fullName + " friends</title>\n");
        reqStr.append("</head>\n");
        reqStr.append("<body>\n");
        vk = createTemplate();
        VKArray<VKontakteProfile> friends = vk.friendsOperations().get(userId);
        for (VKontakteProfile friend : friends.getItems()) {
            String href;
            if (friend.getScreenName() == null) {
                href = "http://id" + friend.getId();
            } else {
                href = "http://" + friend.getScreenName();
            }
            fullName = friend.getFirstName() + " " + friend.getLastName();
            //reqStr.append("<a href=\"" + href + "\">" + fullName + "</a>\n");
            reqStr.append("<a href=\"" + href + "\"> </a>\n");
        }

        reqStr.append("</body>\n");
        reqStr.append("</html>");
        return reqStr.toString();
    }

    private String getProfile(String userId) {
        String[] userIds = {userId};
        VKontakteTemplate vk = createTemplate();
        VKontakteProfile profile = vk.usersOperations().getUser(userId);
        String fullName = profile.getFirstName() + " " + profile.getLastName();
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>" + fullName + "</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "   <a href=\"http://" + userId + "/friends\">Friends</a>\n" +
                "   <a href=\"http://" + userId + "/posts\">Posts</a>\n" +
                //"   <a href=\"http://" + userId + "/friends\">" + fullName + " friends</a>\n" +
                //"{name:'"+fullName+"'}" +
                "</body>\n" +
                "</html>";
    }

    private VKontakteTemplate createTemplate() {
        try {
            InputStream inputStream = proxy.openStream();
            try {
                String data = IOUtils.toString(inputStream, "UTF-8");
                String[] split = data.split(" ");
                Http.LOG.trace("create VKontakteTemplate: " + split[0] + ", " + split[1]);
                return new VKontakteTemplate(split[0], split[1]);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        } catch (IOException e) {

        }
        return null;
    }
}
