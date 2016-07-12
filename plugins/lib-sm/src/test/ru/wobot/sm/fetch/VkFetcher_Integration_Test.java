package ru.wobot.sm.fetch;

import org.junit.Ignore;
import org.junit.Test;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SuccessResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@Ignore
//TODO: Fails with timeout, rewrite
public class VkFetcher_Integration_Test {
    private CredentialRepository mockRepository = mock(CredentialRepository.class);
    private VkFetcher vkFetcher = new VkFetcher(mockRepository);

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
    public void is_getUsers_return_group_profile() throws IOException {
        FetchResponse r = vkFetcher.getGroupData("1"); //vk dev
        assertThat(r, is(not(nullValue())));
    }

    @Test
    public void is_getFriends_return_users_friends() throws IOException {
        FetchResponse response = vkFetcher.getFriendIds("1");
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void is_getPost_return_post() throws IOException {
        SuccessResponse r = (SuccessResponse) vkFetcher.getPostData("1", "45558");
        assertThat(r.getData(), containsString("Способность"));
    }

    @Test
    public void is_getPostsForUser_return_posts() throws IOException {
        SuccessResponse r = (SuccessResponse) vkFetcher.getPostsData("1", 0, 100, null);
        assertThat(r.getData(), is(not(nullValue())));
    }

    @Test
    public void is_getPostsForGroup_return_posts() throws IOException {
        SuccessResponse r = (SuccessResponse) vkFetcher.getGroupTopicsData("18496184", 0, 100);
        assertThat(r.getData(), is(not(nullValue())));
    }

    @Test
    public void is_getComments_return_comments() throws IOException {
        SuccessResponse r = (SuccessResponse) vkFetcher.getPostCommentsData("1", "593585", 100, 0, null);
        assertThat(r.getData(), is(not(nullValue())));
    }

    @Test
    public void is_getTopicComments_return_comments() throws IOException {
        SuccessResponse r = (SuccessResponse) vkFetcher.getTopicCommentsData("18496184", "33524500", 5, 5);
        assertThat(r.getData(), is(not(nullValue())));
    }

    @Test
    public void tender() throws IOException {
        SuccessResponse r = (SuccessResponse) vkFetcher.tender();
        assertThat(r.getData(), is(not(nullValue())));
    }
}
