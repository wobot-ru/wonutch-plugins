package ru.wobot.sm;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.fb.ProfileParser;
import org.apache.nutch.protocol.Content;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.wobot.sm.core.api.FbApiTypes;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.auth.Credential;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.fetch.FbFetcher;
import ru.wobot.sm.parse.FbParser;
import ru.wobot.uri.UriTranslator;
import ru.wobot.uri.impl.ParsedUri;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for API fetching and parsing
 */
public class TestFbService {
    private static final String API_VERSION = "2.5";
    private UriTranslator translator;

    public TestFbService() throws ClassNotFoundException {
        translator = new UriTranslator(fetcher());
    }

    private FbFetcher fetcher() {
        CredentialRepository repository = mock(CredentialRepository.class);
        Credential credential = mock(Credential.class);
        given(credential.getAccessToken()).willReturn("1796076883956849|ixyJ9j1fyTeXIGCKK--KF7VymMs");
        given(repository.getInstance()).willReturn(credential);
        CookieRepository cookieRepository = new CookieRepository();
        cookieRepository.setConf(new Configuration());
        return new FbFetcher(repository, cookieRepository);
    }

    private ParseResult getParseResult(String uri, String apiType, String apiVersion) throws URISyntaxException {
        FetchResponse response = translator.translate(ParsedUri.parse(uri));
        String content = response.getData();
        String mimeType = response.getMetadata().get(ContentMetaConstants.MIME_TYPE).toString();
        if (mimeType.equals("application/json"))
            return new FbParser().parse(new URI(uri), content, apiType, apiVersion);
        else {
            Map<String, Object> parseMeta = new HashMap<>();
            Metadata metadata = new ProfileParser(new Content(uri, uri, content.getBytes(StandardCharsets.UTF_8), "text/html",
                    new Metadata(), new Configuration())).getParseResult().get(uri).getData().getParseMeta();
            for (String name : metadata.names()) {
                parseMeta.put(name, metadata.get(name));
            }
            return new ParseResult(uri, new HashMap<String, String>(), parseMeta, new HashMap<String, Object>());
        }
    }

    @Test
    public void check_that_request_and_parse_is_success_for_fb_page() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://mastercardrussia", FbApiTypes.PROFILE, API_VERSION);

        Assert.assertNotNull(parse.getContent());
    }

    @Test
    public void check_that_request_and_parse_is_success_for_page_id() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://892133830908265", FbApiTypes.PROFILE, API_VERSION);

        assertThat(parse.getContent(), isEmptyString());
    }

    @Test
    public void check_that_request_is_success_for_profile_app_scoped_id() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://892133830908265/profile/app_scoped", FbApiTypes.PROFILE, API_VERSION);

        assertThat(parse.getContent(), isEmptyString());
    }

    @Test
    @Ignore
    public void check_that_request_is_success_for_profile_app_scoped_id_auth() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://1153183591398867/profile/app_scoped/auth", FbApiTypes.PROFILE, API_VERSION);

        assertThat(parse.getContent(), isEmptyString());
    }

    @Test
    @Ignore
    //TODO: Think of such integration tests
    public void check_that_request_is_success_for_profile_real_id() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://100004451677809/profile?as_id=548469171978134", null, null);

        assertThat(parse.getContent(), is(not(nullValue())));
    }

    @Test
    @Ignore
    public void check_that_request_is_success_for_profile_screen_name() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://titokate/profile?as_id=211734279170201&screen_name", null, null);

        assertThat(parse.getParseMeta(), is(not(nullValue())));
    }

    @Test
    @Ignore
    public void check_that_request_is_success_for_profile_with_name_in_comment() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://100003249378953/profile?as_id=884603914991246", null, null);

        assertThat(parse.getParseMeta().get("name"), is(not(nullValue())));
    }

    @Test
    public void check_that_request_get_friends_for_profile() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://165107853523677/friends", FbApiTypes.FRIEND_LIST_OF_ID, API_VERSION);

        Assert.assertNotNull(parse.getContent());
        assertThat(parse.getLinks().size(), is(greaterThan(1)));
    }

    @Test
    public void request_should_get_and_parse_fb_posts_page() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://191234802505/index-posts/x100/00000000", FbApiTypes.POST_BULK, API_VERSION);

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(greaterThanOrEqualTo(100)));
    }

    @Test
    public void request_should_get_and_parse_fb_posts_page_with_no_likes() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://96814974590/index-posts/x100/1453725511", FbApiTypes.POST_BULK, API_VERSION);

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(greaterThanOrEqualTo(101)));
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://165107853523677/posts/165107853523677_1170176776350108/x100/0", FbApiTypes.COMMENT_BULK, API_VERSION);

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_next_page() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://11784025953/posts/11784025953_10153439131790954/x100/WTI5dGJXVnVkRjlqZFhKemIzSTZNVEF4TlRNME5ERTJOamN6T0RVNU5UUTZNVFExTlRBeE1qazJPUT09", FbApiTypes.COMMENT_BULK, API_VERSION);

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(greaterThan(101)));
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_status_posts() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://165107853523677/posts/165107853523677_992162300818224/x100/0", FbApiTypes.COMMENT_BULK, API_VERSION);

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_event_posts() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://23680604925/posts/23680604925_157764544592548/x100/0", FbApiTypes.COMMENT_BULK, API_VERSION);

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_video_posts() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://162280197182417/posts/162280197182417_769592706451160/x100/0", FbApiTypes.COMMENT_BULK, API_VERSION);

        Assert.assertNotNull(parse);
    }

}
