package ru.wobot.sm.fetch;

import org.junit.Before;
import org.junit.Test;
import ru.wobot.sm.core.fetch.SMFetcher;
import ru.wobot.uri.UriTranslator;
import ru.wobot.uri.impl.ParsedUri;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class VKFetcher_Test {
    private UriTranslator translator;
    private SMFetcher scheme;

    @Before
    public void setUp() throws ClassNotFoundException {
        scheme = mock(VKFetcher.class);
        translator = new UriTranslator(scheme);
    }

    @Test
    public void when_translate_index_posts_than_should_be_invoke_getPostCount() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://user/index-posts"));

        //then
        verify(scheme).getPostCount("user");
    }

    @Test
    public void when_translate_friends_than_should_be_invoke_getFriendIds() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://user/friends/"));

        //then
        verify(scheme).getFriendIds("user");
    }

    @Test
    public void when_translate_batch_of_posts_than_should_be_invoke_getPostsData() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://user/index-posts/x99/0000000001"));

        //then
        verify(scheme).getPostsData("user", 99, 1);
    }

    @Test
    public void when_translate_one_post_than_should_be_invoke_getFriendIds() throws Exception {
        // given
        // when
        translator.translate(ParsedUri.parse("vk://user/posts/99"));

        //then
        verify(scheme).getPostData("user", "99");
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
        verify(scheme).getPostCommentsData("id1", "531296", 77, 112);
    }
}
