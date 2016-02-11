package ru.wobot.sm.fetch;

import org.junit.Test;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class TestVkService {
    private VKFetcher vkFetcher = new VKFetcher();

    @Test
    public void is_getSMProfiles_return_SMProfiles() throws IOException {
        List<SMProfile> profiles = vkFetcher.getProfiles(Arrays.asList("durov", "id2"));
        assertTrue(profiles.size() == 2);
    }

    @Test
    public void is_getUsers_return_user_profile() throws IOException {
        FetchResponse r = vkFetcher.getProfileData("id1");
        assertThat(r, is(not(nullValue())));
    }

    @Test
    public void is_getFriends_return_users_friends() throws IOException {
        final FetchResponse response = vkFetcher.getFriendIds("1");
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void is_getPost_return_post() throws IOException {
        FetchResponse r = vkFetcher.getPostData("1", "45558");
        assertThat(r.getData(), containsString("Способность"));
    }

    @Test
    public void is_getPostsForUser_return_posts() throws IOException {
        FetchResponse r = vkFetcher.getPostsData("1", 0, 100);
        assertThat(r.getData(), is(not(nullValue())));
    }

    @Test
    public void is_getComments_return_comments() throws IOException {
        FetchResponse r = vkFetcher.getPostCommentsData("1", "593585", 100, 0);
        assertThat(r.getData(), is(not(nullValue())));
    }
}
