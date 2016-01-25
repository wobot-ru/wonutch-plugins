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
    private FbService fbService = new FbService();

    @Test
    public void shouldGetProfilesFor2Ids() throws IOException {
        // given when
        List<SMProfile> profiles = fbService.getProfiles(Arrays.asList("mastercardrussia", "28312410177"));

        // then
        assertThat(profiles.size(), is(2));
    }

    @Test
    public void shouldGetProfileDataFor2Ids() throws IOException {
        // given when
        List<SMProfile> profiles = fbService.getProfiles(Arrays.asList("mastercardrussia", "28312410177"));

        // then
        assertThat(profiles.get(0).getId(), is(equalTo("165107853523677")));
        assertThat(profiles.get(0).getDomain(), is(equalTo("mastercardrussia")));
        assertThat(profiles.get(1).getFullName(), is(equalTo("World Food Programme")));
    }

    @Test
    public void shouldGetSomeProfileDataForId() throws IOException {
        // given when
        FetchResponse response = fbService.getProfileData("mastercardrussia");

        // then
        assertThat(response, is(not(nullValue())));
        assertThat(response.getData(), containsString("MasterCard"));
    }

    @Test
    public void shouldGetWhoLikedPageForId() throws IOException {
        // given when
        List<String> friendIds = fbService.getFriendIds("24496278123");

        // then
        assertThat(friendIds.size(), is(greaterThan(0)));
        assertThat(friendIds, hasItems("431891506856669", "21435141328"));
    }



}
