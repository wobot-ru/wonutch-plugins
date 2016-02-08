package ru.wobot.sm.core.url;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestUrlCheck {
    @Test
    public void is_postsIndex_should_be_true_for_postIndex_url() throws MalformedURLException {
        URL url = new URL("http://user/index-posts");
        assertTrue(UrlCheck.isPostsIndex(url));
    }

    @Test
    public void is_postsIndex_should_be_false_for_postIndexPage_url() throws MalformedURLException {
        URL url = new URL("http://user/index-posts/x100/0000000001");
        assertFalse(UrlCheck.isPostsIndex(url));
    }

    @Test
    public void is_post_should_be_true_for_post_url() throws MalformedURLException {
        URL url = new URL("http://user/posts/1");
        assertTrue(UrlCheck.isPost(url));
    }

    @Test
    public void is_isCommentPage_should_be_true_for_such_url() throws MalformedURLException {
        URL url = new URL("http://user/posts/1/x100/0");
        assertTrue(UrlCheck.isCommentPage(url));
    }

    @Test
     public void is_isCommentPage_should_be_true_for_fb_post_id_url() throws MalformedURLException {
        URL url = new URL("http://user/posts/1:2_3/x100/0");
        assertTrue(UrlCheck.isCommentPage(url));
    }

    @Test
    public void is_isCommentPage_should_be_true_for_token_url() throws MalformedURLException {
        URL url = new URL
                ("http://user/posts/1_2/x100/0" +
                        "?after=WTI5dGJXVnVkRjlqZFhKemIzSTZNVEE0TXpFMk1qYzJNVGN4T0RFM056b3hORFV5TWpreE56YzQ%3D");
        assertTrue(UrlCheck.isCommentPage(url));
    }

    @Test
    public void is_post_should_be_false_for_prefix_url() throws MalformedURLException {
        URL url = new URL("http://user/posts/");
        assertFalse(UrlCheck.isPost(url));
    }
}
