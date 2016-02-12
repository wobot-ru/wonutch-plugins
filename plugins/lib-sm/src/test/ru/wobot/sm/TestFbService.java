package ru.wobot.sm;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.wobot.sm.core.auth.Credential;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.fetch.FbFetcher;
import ru.wobot.sm.parse.FbParser;
import ru.wobot.uri.UriTranslator;
import ru.wobot.uri.impl.ParsedUri;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TestFbService {
    private UriTranslator translator;

    public TestFbService() throws ClassNotFoundException {
        translator = new UriTranslator(fetcher());
    }

    private FbFetcher fetcher() {
        CredentialRepository repository = mock(CredentialRepository.class);
        Credential credential = mock(Credential.class);
        given(credential.getAccessToken()).willReturn("1673845006238395|FkqeRkmP1kT_Ae42i8IkZx8KxBM");
        given(repository.getInstance()).willReturn(credential);
        return new FbFetcher(repository);
    }

    private ParseResult getParseResult(String uri) throws URISyntaxException {
        FetchResponse fetchResponse = translator.translate(ParsedUri.parse(uri));
        String content = fetchResponse.getData();
        return new FbParser().parse(new URI(uri), content);
    }

    @Test
    public void check_that_request_and_parse_is_success_for_fb_page() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://mastercardrussia");

        Assert.assertNotNull(parse.getContent());
    }

    @Test
    public void check_that_request_and_parse_is_success_for_fb_user() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://1704938049732711?scope=user&comment_id=10154479526792506_10154481125912506");

        Assert.assertNotNull(parse.getContent());
    }

    @Test
    public void check_that_request_get_friends_for_profile() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://165107853523677/friends");

        Assert.assertNotNull(parse.getContent());
        assertThat(parse.getLinks().size(), is(greaterThan(1)));
    }

    @Test
    public void request_should_get_and_parse_fb_posts_page() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://191234802505/index-posts/x100/00000000");

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(101));
    }

    @Test
    public void request_should_get_and_parse_fb_posts_page_with_no_likes() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://96814974590/index-posts/x100/1453725511");

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(greaterThan(101)));
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://165107853523677/posts/165107853523677_1081856348515485/x100/0");

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_next_page() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://11784025953/posts/11784025953_10153439131790954/x100/WTI5dGJXVnVkRjlqZFhKemIzSTZNVEF4TlRNME5ERTJOamN6T0RVNU5UUTZNVFExTlRBeE1qazJPUT09");

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(greaterThan(101)));
    }


    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_status_posts() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://165107853523677/posts/165107853523677_992162300818224/x100/0");

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_event_posts() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://23680604925/posts/23680604925_157764544592548/x100/0");

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_video_posts() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://162280197182417/posts/162280197182417_769592706451160/x100/0");

        Assert.assertNotNull(parse);
    }

    @Test
    @Ignore
    public void check_that_request_get_and_parse_fb_comments_page_for_old_comments() throws IOException, URISyntaxException {
        ParseResult parse = getParseResult("fb://162280197182417/posts/233436326701158:265932876784836:10102644640331371_10100522591459358/x100/0");

        Assert.assertNotNull(parse);
    }
}
