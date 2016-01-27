package ru.wobot.sm.fetch;

import org.junit.Test;
import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class TestFbService {
    private FbFetcher fbFetcher = new FbFetcher();

    @Test
    public void shouldGetProfilesFor2Ids() throws IOException {
        // given when
        List<SMProfile> profiles = fbFetcher.getProfiles(Arrays.asList("mastercardrussia", "28312410177"));

        // then
        assertThat(profiles.size(), is(2));
    }

    @Test
    public void shouldGetProfileDataFor2Ids() throws IOException {
        // given when
        List<SMProfile> profiles = fbFetcher.getProfiles(Arrays.asList("mastercardrussia", "28312410177"));

        // then
        assertThat(profiles.get(0).getId(), is(equalTo("165107853523677")));
        assertThat(profiles.get(0).getDomain(), is(equalTo("mastercardrussia")));
        assertThat(profiles.get(1).getFullName(), is(equalTo("World Food Programme")));
    }

    @Test
    public void shouldGetSomeProfileDataForId() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileData("mastercardrussia");

        // then
        assertThat(response, is(not(nullValue())));
        assertThat(response.getData(), containsString("MasterCard"));
    }

    @Test
    public void shouldGetWhoLikedPageForId() throws IOException {
        // given when
        List<String> friendIds = fbFetcher.getFriendIds("24496278123");

        // then
        assertThat(friendIds.size(), is(greaterThan(0)));
        assertThat(friendIds, hasItems("431891506856669", "21435141328"));
    }

    @Test
    public void shouldGetPageOfPostsNotAfterTimestamp() throws IOException {
        // given when
        FetchResponse posts = fbFetcher.getPostsData("165107853523677", 1450780702L, 25); //Tue, 22 Dec 2015 10:38:22 GMT

        // then
        assertThat(posts, is(not(nullValue())));
        assertThat(posts.getData(), is(not(nullValue())));
    }
}
