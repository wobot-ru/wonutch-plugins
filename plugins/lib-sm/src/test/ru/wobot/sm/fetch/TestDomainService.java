package ru.wobot.sm.fetch;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.social.vkontakte.api.VKontakteErrorException;
import ru.wobot.sm.core.domain.SMContent;
import ru.wobot.sm.core.domain.service.DomainService;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.parse.Vk;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
}
