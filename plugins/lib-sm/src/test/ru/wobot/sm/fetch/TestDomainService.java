package ru.wobot.sm.fetch;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.social.vkontakte.api.VKontakteErrorException;
import ru.wobot.sm.core.domain.SMContent;
import ru.wobot.sm.core.domain.service.DomainService;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.parse.FbParser;
import ru.wobot.sm.parse.Vk;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestDomainService {
    @Test(expected = VKontakteErrorException.class)
    public void is_indexPosts_for_private_profiles_should_throw_exception() throws IOException {
        SMContent request = new DomainService(new VKFetcher()).request("vk://id7076373/index-posts");

        Assert.assertNotNull(request);
    }

    //http://durov/index-posts
    @Test
    public void is_indexPosts_for_durov_profile_should_return_response() throws IOException {
        SMContent request = new DomainService(new VKFetcher()).request("vk://durov/index-posts");

        Assert.assertNotNull(request);
    }

    @Test
    public void check_that_request_and_parse_serialization_is_success_for_post() throws IOException {
        SMContent request = new DomainService(new VKFetcher()).request("vk://durov/posts/145488");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new Vk().parse(new URL("vk://durov/posts/145488"), content);

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_friends_for_prifile() throws IOException {
        SMContent request = new DomainService(new FbFetcher()).request("fb://mastercardrussia/friends");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://mastercardrussia/friends"), content);

        Assert.assertNotNull(parse);
    }

    @Test
    public void check_that_request_get_and_parse_fb_posts_page() throws IOException {
        SMContent request = new DomainService(new FbFetcher()).request("fb://191234802505/index-posts/x100/00000000");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://191234802505/index-posts/x100/00000000"), content);

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(101));
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page() throws IOException {
        SMContent request = new DomainService(new FbFetcher()).request
                ("fb://mastercardrussia/posts/165107853523677_1081856348515485/x100/0");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://mastercardrussia/posts/165107853523677_1081856348515485/x100/0"), content);

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(35));
    }

    @Test
    public void check_that_request_get_and_parse_fb_comments_page_for_status_posts() throws IOException {
        SMContent request = new DomainService(new FbFetcher()).request
                ("fb://mastercardrussia/posts/165107853523677_992162300818224/x100/0");
        String content = new String(request.getData(), StandardCharsets.UTF_8);
        ParseResult parse = new FbParser().parse(new URL("fb://mastercardrussia/posts/165107853523677_992162300818224/x100/0"), content);

        Assert.assertNotNull(parse);
        assertThat(parse.getLinks().size(), is(1));
    }
}
