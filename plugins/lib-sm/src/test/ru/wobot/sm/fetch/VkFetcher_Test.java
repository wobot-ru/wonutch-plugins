package ru.wobot.sm.fetch;

import org.junit.Before;
import org.junit.Test;
import ru.wobot.uri.UriTranslator;
import ru.wobot.uri.impl.ParsedUri;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class VkFetcher_Test {
    private UriTranslator translator;
    private VkFetcher scheme;

    @Before
    public void setUp() throws ClassNotFoundException {
        scheme = mock(VkFetcher.class);
        translator = new UriTranslator(scheme);
    }

    @Test
    public void when_translate_index_posts_than_should_be_invoke_getPostCount() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://id5/index-posts"));

        //then
        verify(scheme).getPostCount("5", null);
    }

    @Test
    public void when_translate_index_posts_wth_auth_than_should_be_invoke_getPostCount() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://id5/index-posts?auth"));

        //then
        verify(scheme).getPostCount("5", "");
    }

    @Test
    public void when_translate_friends_than_should_be_invoke_getFriendIds() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://id42/friends/"));

        //then
        verify(scheme).getFriendIds("42");
    }

    @Test
    public void when_translate_batch_of_posts_than_should_be_invoke_getPostsData() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://id10/index-posts/x99/0000000001"));

        //then
        verify(scheme).getPostsData("10", 99, 1, null);
    }

    @Test
    public void when_translate_batch_of_posts_with_auth_than_should_be_invoke_getPostsData() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://id10/index-posts/x99/0000000001?auth"));

        //then
        verify(scheme).getPostsData("10", 99, 1, "");
    }

    @Test
    public void when_translate_one_post_than_should_be_invoke_getFriendIds() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://id22/posts/99"));

        //then
        verify(scheme).getPostData("22", "99");
    }

    @Test
    public void when_translate_user_profile_than_should_be_invoke_getProfileData() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://user//"));

        //then
        verify(scheme).getProfileData("user");
    }

    @Test
    public void when_translate_comments_page_than_should_be_invoke_getProfileData() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://id1/posts/531296/x77/000112"));

        //then
        verify(scheme).getPostCommentsData("1", "531296", 77, 112);
    }
}
