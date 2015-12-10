package ru.wobot.vk;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.social.vkontakte.api.CommentsResponse;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import org.springframework.social.vkontakte.api.impl.wall.CommentsQuery;
import org.springframework.social.vkontakte.api.impl.wall.UserWall;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TestVkService {
    @Test
    public void is_getUsers_return_users_profiles() throws IOException {
        VKService vkService = new VKService();
        List<VKontakteProfile> profiles = vkService.getUsers(Arrays.asList("durov", "id2"));
        Assert.assertTrue(profiles.size() == 2);
    }

    @Test
    public void is_getFriends_return_users_friends() throws IOException {
        VKService vkService = new VKService();
        VKArray<VKontakteProfile> friends = vkService.getFriends(1l);
        Assert.assertTrue(friends.getItems().size() > 1);
    }

    @Test
    public void is_getPost_return_post() throws IOException {
        VKService vkService = new VKService();
        Post post = vkService.getPost(1l, "45558");
        Assert.assertTrue(post.getText().contains("Способность"));
    }

    @Test
    public void is_getPostsForUser_return_posts() throws IOException {
        VKService vkService = new VKService();
        VKArray<Post> posts = vkService.getPostsForUser(1l, 0, 100);
        Assert.assertTrue(posts.getItems().size() > 1);
    }

    @Test
    public void is_getComments_return_comments() throws IOException {
        VKService vkService = new VKService();
        CommentsQuery query = new CommentsQuery
                .Builder(new UserWall(1), 593585)
                .needLikes(true)
                .count(100)
                .offset(0)
                .build();

        CommentsResponse response = vkService.getComments(query);
        Assert.assertTrue(response.getComments().size()>0);
    }
}
