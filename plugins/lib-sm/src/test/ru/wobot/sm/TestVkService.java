package ru.wobot.sm;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.fetch.VKService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class TestVkService {
    private VKService vkService;

    @Before
    public void executedBeforeEach() {
        vkService = new VKService();
    }


    @Test
    public void is_getSMProfiles_return_SMProfiles() throws IOException {
        List<SMProfile> profiles = vkService.getProfiles(Arrays.asList("durov", "id2"));
        Assert.assertTrue(profiles.size() == 2);
    }

    @Test
    public void is_getUsers_return_user_profile() throws IOException {
        FetchResponse r = vkService.getProfileData("id1");
        assertThat(r, is(not(nullValue())));
    }

    @Test
    public void is_getFriends_return_users_friends() throws IOException {
        List<String> friendIds = vkService.getFriendIds("1");
        Assert.assertTrue(friendIds.size() > 1);
    }

    @Test
    public void is_getPost_return_post() throws IOException {
        FetchResponse r = vkService.getPostData("1", "45558");
        assertThat(r.getData(), containsString("Способность"));
    }

    @Test
    public void is_getPostsForUser_return_posts() throws IOException {
        List<String> posts = vkService.getPostIds("1", 0, 100);
        Assert.assertTrue(posts.size() > 1);
    }

    @Test
    public void is_getComments_return_comments() throws IOException {
        FetchResponse r = vkService.getPostCommentsData("1", "593585", 0, 100);
        assertThat(r.getData(), is(not(nullValue())));
    }
}
