package ru.wobot.vk;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;

public class TestDomainService {
    //http://durov/index-posts
    @Test
    public void indexPosts() throws MalformedURLException {
        Response request = DomainService.request("http://durov/index-posts");

        Assert.assertNotNull(request);
    }
}
