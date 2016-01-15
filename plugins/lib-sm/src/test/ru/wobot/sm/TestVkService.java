package ru.wobot.sm;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.social.vkontakte.api.CommentsResponse;
import org.springframework.social.vkontakte.api.impl.wall.CommentsQuery;
import org.springframework.social.vkontakte.api.impl.wall.UserWall;
import ru.wobot.sm.core.dto.SMProfile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TestVkService {
    @Test
    public void is_getSMProfiles_return_SMProfiles() throws IOException {
        VKService vkService = new VKService();
        List<SMProfile> profiles = vkService.getProfiles(Arrays.asList("durov", "id2"));
        Assert.assertTrue(profiles.size() == 2);
    }

    @Test
    public void is_getUsers_return_user_profile() throws IOException {
        VKService vkService = new VKService();
        String profileStr = vkService.getProfileData("id1");
        Assert.assertNotNull(profileStr);
    }

    @Test
    public void is_getFriends_return_users_friends() throws IOException {
        VKService vkService = new VKService();
        List<String> friendIds = vkService.getFriendIds("1");
        Assert.assertTrue(friendIds.size() > 1);
    }

    @Test
    public void is_getPost_return_post() throws IOException {
        VKService vkService = new VKService();
        String post = vkService.getPostData("1", "45558");
        Assert.assertTrue(post.contains("Способность"));
    }

    @Test
    public void is_getPostsForUser_return_posts() throws IOException {
        VKService vkService = new VKService();
        List<String> posts = vkService.getPostIds("1", 0, 100);
        Assert.assertTrue(posts.size() > 1);
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
