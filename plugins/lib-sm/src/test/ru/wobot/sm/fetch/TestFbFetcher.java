package ru.wobot.sm.fetch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.social.facebook.api.PagingParameters;
import org.springframework.social.facebook.api.Post;
import org.springframework.social.facebook.api.impl.PagedListUtils;
import org.springframework.social.facebook.api.impl.json.FacebookModule;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.auth.Credential;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SuccessResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TestFbFetcher {
    private ObjectMapper objectMapper = new ObjectMapper();
    private CredentialRepository repository = mock(CredentialRepository.class);
    private CookieRepository cookieRepository = new CookieRepository();
    private FbFetcher fbFetcher = new FbFetcher(repository, cookieRepository);

    {
        cookieRepository.setConf(new Configuration());
        Credential credential = mock(Credential.class);
        given(credential.getAccessToken()).willReturn("717502605052808|vJSXEhRP-HhsrDcY-6qj4Q2vTYU");
        given(repository.getInstance()).willReturn(credential);
        objectMapper.registerModule(new FacebookModule());
    }

    @Test
    public void shouldGetFullPageDataForUsername() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getPageData("mastercardrussia");

        // then
        assertThat(response.getData(), containsString("MasterCard"));
    }

    @Test
    public void shouldGetFullPageDataForId() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getPageData("24496278123");

        // then
        assertThat(response.getData(), containsString("Unilever"));
    }

    @Test
    public void shouldRedirectForUserId() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getPageData("892133830908265");

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("fb://892133830908265/profile/app_scoped"));
    }

    @Test
    public void shouldRedirectToRealIdForUserWithPhoto() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileId("892133830908265");

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("fb://100003349701954/profile?as_id=892133830908265"));
    }

    @Test
    public void shouldRedirectToAppScopedUrlForUserWithNoPhoto() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileId("1153183591398867");

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("fb://1153183591398867/profile/app_scoped/auth"));
    }

    @Test
    public void shouldRedirectToAppScopedUrlForUserWithDefaultPhoto() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileId("548469171978134");

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("fb://548469171978134/profile/app_scoped/auth"));
    }

    @Test
    public void shouldRedirectToRealIdForAppScopedUrl() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileIdAuth("548469171978134");

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("fb://100004451677809/profile?as_id=548469171978134"));
    }

    @Test
    public void shouldRedirectToRealScreenNameForAppScoped() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileIdAuth("892133830908265");

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("fb://lidia.mazurova/profile?as_id=892133830908265&screen_name"));
    }

    @Test
    public void shouldRedirectToRealScreenNameForAppScopedUrlWODots() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileIdAuth("948085905239709");

        // then
        assertThat(response.getData(), isEmptyString());
        assertThat(response.getMessage().toString(), is("fb://renatadavidova50/profile?as_id=948085905239709&screen_name"));
    }

    @Test
    @Ignore
    public void shouldGetProfileDataForUserWithNoScreenName() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileData("100004451677809", "548469171978134", null);

        // then
        assertThat(response.getData(), stringContainsInOrder(Arrays.asList("Наталья", "Санкт-Петербург")));
    }

    @Test
    @Ignore
    public void shouldGetProfileDataForUserWithScreenName() throws IOException {
        // given when
        FetchResponse response = fbFetcher.getProfileData("100003349701954", "892133830908265", null);

        // then
        assertThat(response.getData(), stringContainsInOrder(Arrays.asList("Мазурова", "МБОУ СОШ")));
    }

    @Test
    // get friends realisation for FB pages
    public void shouldGetWhoLikedPageForId() throws IOException {
        // given when
        SuccessResponse response = fbFetcher.getFriendIds("24496278123");

        List<String> friendIds = Arrays.asList(objectMapper.readValue(response.getData(), String[].class));

        // then
        assertThat(friendIds.size(), is(greaterThan(0)));
        assertThat(friendIds, hasItems("431891506856669", "21435141328"));
    }

    @Test
    public void shouldGetFirstPageOfPosts() throws IOException {
        // given when
        SuccessResponse response = fbFetcher.getPostsData("191234802505", 25, 0L);

        // when
        List<Post> posts = parsePosts(response.getData());
        JsonNode rawPosts = getRawData(response.getData());

        // then
        assertThat(posts.get(12).getId(), is(Matchers.equalTo(rawPosts.get(12).get("id").asText())));
        assertThat(posts.size(), is(25));
    }

    @Test
    public void shouldGetLinkToPageOfOlderPosts() throws IOException {
        // given when
        SuccessResponse response = fbFetcher.getPostsData("165107853523677", 0, 0L);

        // when
        List<Post> posts = parsePosts(response.getData());
        Post post = posts.get(posts.size() - 1);
        PagingParameters nextPage = parseNext(response.getData());

        // then
        assertThat(posts.size(), is(25));
        assertThat(post.getCreatedTime(), is(greaterThanOrEqualTo(new Date(nextPage.getUntil() * 1000))));
    }

    @Test
    public void shouldGetPageOfPostsNotAfterTimestamp() throws IOException {
        // given
        SuccessResponse response = fbFetcher.getPostsData("165107853523677", 0, 1450780702L); //Tue, 22 Dec 2015
        // 10:38:22 GMT

        // when
        List<Post> posts = parsePosts(response.getData());

        // then
        assertThat(posts.get(0).getCreatedTime(), is(not(greaterThan(new Date(1450780702000L)))));
    }

    @Test
    public void shouldGetTotalLikesCount() throws IOException {
        // given
        SuccessResponse response = fbFetcher.getPostsData("165107853523677", 0, 1450780702L); //Tue, 22 Dec 2015
        // 10:38:22 GMT

        // when
        JsonNode posts = getRawData(response.getData());
        // hope this post has some likes and comments.
        JsonNode likesSummary = posts.get(0).get("likes").get("summary");

        // then
        assertThat(likesSummary.get("total_count").asInt(), is(greaterThan(1)));
    }

    @Test
    public void shouldGetCommentsPagesForPost() throws IOException {
        // given
        SuccessResponse response = fbFetcher.getPostCommentsData(null, "165107853523677_1081856348515485", 3, "0");

        // when
        JsonNode comments = getRawData(response.getData());
        PagingParameters nextPage = parseNext(response.getData());
        response = fbFetcher.getPostCommentsData(null, "165107853523677_1081856348515485", 3, nextPage.getAfter());
        JsonNode nextComments = getRawData(response.getData());

        // then
        assertThat(comments.size(), is(3));
        assertThat(nextComments.size(), is(3));
        assertThat(comments.get(0).get("created_time").asText(), is(greaterThan(nextComments.get(0).get
                ("created_time").asText()
        )));
    }

    private JsonNode getRawData(String data) throws IOException {
        JsonNode node = objectMapper.readValue(data, JsonNode.class);
        return node.get("data");
    }

    private List<Post> parsePosts(String data) throws IOException {
        return objectMapper.readValue(getRawData(data).toString(), new TypeReference<List<Post>>() {
        });
    }

    private PagingParameters parseNext(String data) throws IOException {
        JsonNode node = objectMapper.readValue(data, JsonNode.class);
        JsonNode pagingNode = node.get("paging");
        return PagedListUtils.getPagedListParameters(pagingNode, "next");
    }
}
