package ru.wobot.sm;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.wobot.sm.core.auth.Credential;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.domain.SMContent;
import ru.wobot.sm.core.domain.service.DomainService;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.fetch.FbFetcher;
import ru.wobot.sm.parse.FbParser;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TestFbService {

    private FbFetcher fetcher() {
        CredentialRepository repository = mock(CredentialRepository.class);
        Credential credential = mock(Credential.class);
        given(credential.getAccessToken()).willReturn("1673845006238395|FkqeRkmP1kT_Ae42i8IkZx8KxBM");
        given(repository.getInstance()).willReturn(credential);
        return new FbFetcher(repository);
    }

    @Test
    public void check_that_request_and_parse_is_success_for_fb_user() throws IOException {
        SMContent request = new DomainService(fetcher()).request("fb://1704938049732711?scope=user&comment_id=10154479526792506_10154481125912506");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://1704938049732711?scope=user&comment_id=10154479526792506_10154481125912506"), content);

        Assert.assertNotNull(parse.getContent());
    }

    @Test
    public void check_that_request_get_friends_for_profile() throws IOException {
        SMContent request = new DomainService(fetcher()).request("fb://mastercardrussia/friends");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://mastercardrussia/friends"), content);

        Assert.assertNotNull(parse.getContent());
    }

    @Test
    public void request_should_get_and_parse_fb_posts_page() throws IOException {
        SMContent request = new DomainService(fetcher()).request("fb://191234802505/index-posts/x100/00000000");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://191234802505/index-posts/x100/00000000"), content);

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(101));
    }

    @Test
    public void request_should_get_and_parse_fb_posts_page_with_no_likes() throws IOException {
        SMContent request = new DomainService(fetcher()).request
                ("fb://96814974590/index-posts/x100/1453725511");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://96814974590/index-posts/x100/1453725511"), content);

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(greaterThan(101)));
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page() throws IOException {
        SMContent request = new DomainService(fetcher()).request
                ("fb://mastercardrussia/posts/165107853523677_1081856348515485/x100/0");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://mastercardrussia/posts/165107853523677_1081856348515485/x100/0"), content);

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_status_posts() throws IOException {
        SMContent request = new DomainService(fetcher()).request
                ("fb://mastercardrussia/posts/165107853523677_992162300818224/x100/0");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://mastercardrussia/posts/165107853523677_992162300818224/x100/0"), content);

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_event_posts() throws IOException {
        SMContent request = new DomainService(fetcher()).request
                ("fb://23680604925/posts/23680604925_157764544592548/x100/0");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://23680604925/posts/23680604925_157764544592548/x100/0"), content);

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_video_posts() throws IOException {
        SMContent request = new DomainService(fetcher()).request
                ("fb://162280197182417/posts/162280197182417_769592706451160/x100/0");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://162280197182417/posts/162280197182417_769592706451160/x100/0"), content);

        Assert.assertNotNull(parse);
    }

    @Test
    @Ignore
    public void check_that_request_get_and_parse_fb_comments_page_for_old_comments() throws IOException {
        SMContent request = new DomainService(fetcher()).request
                ("fb://162280197182417/posts/233436326701158:265932876784836:10102644640331371_10100522591459358/x100/0");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://162280197182417/posts/233436326701158:265932876784836:10102644640331371_10100522591459358/x100/0"), content);

        Assert.assertNotNull(parse);
    }
}
