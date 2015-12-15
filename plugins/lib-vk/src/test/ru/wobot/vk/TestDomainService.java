package ru.wobot.vk;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.social.vkontakte.api.VKontakteErrorException;

import java.net.MalformedURLException;

public class TestDomainService {
    @Test(expected = VKontakteErrorException.class)
    public void is_indexPosts_for_private_profiles_should_throw_exception() throws MalformedURLException {
        Response request = DomainService.request("http://id7076373/index-posts");

        Assert.assertNotNull(request);
    }

    //http://durov/index-posts
    @Test
    public void is_indexPosts_for_durov_profile_should_return_response() throws MalformedURLException {
        Response request = DomainService.request("http://durov/index-posts");

        Assert.assertNotNull(request);
    }
}
