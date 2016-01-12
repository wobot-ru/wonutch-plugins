package ru.wobot.vk;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.social.vkontakte.api.VKontakteErrorException;
import ru.wobot.smm.core.DomainService;
import ru.wobot.smm.core.dto.ParseResult;
import ru.wobot.smm.core.dto.Response;

import java.io.IOException;

public class TestDomainService {
    @Test(expected = VKontakteErrorException.class)
    public void is_indexPosts_for_private_profiles_should_throw_exception() throws IOException {
        Response request = new DomainService(new VKService()).request("vk://id7076373/index-posts");

        Assert.assertNotNull(request);
    }

    //http://durov/index-posts
    @Test
    public void is_indexPosts_for_durov_profile_should_return_response() throws IOException {
        Response request = new DomainService(new VKService()).request("vk://durov/index-posts");

        Assert.assertNotNull(request);
    }

    @Test
    public void check_that_request_and_parse_serialization_is_success_for_post() throws IOException {
        Response request = new DomainService(new VKService()).request("vk://durov/posts/145488");
        ParseResult parse = Parser.parse("vk://durov/posts/145488", request.data);

        Assert.assertNotNull(parse);
    }
}
