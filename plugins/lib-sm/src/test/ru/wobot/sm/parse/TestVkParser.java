package ru.wobot.sm.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.wobot.sm.core.parse.ParseResult;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static java.nio.file.Files.readAllBytes;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestVkParser {
    private static final VkParser vkParser = new VkParser();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String posts;
    private static String group;
    private static String topics;
    private static String topicComments;
    private static String postComments;

    static {
        try {
            posts = new String(readAllBytes(Paths.get("src/testresources/vk-posts.json")));
            group = new String(readAllBytes(Paths.get("src/testresources/vk-group.json")));
            topics = new String(readAllBytes(Paths.get("src/testresources/vk-topics.json")));
            topicComments = new String(readAllBytes(Paths.get("src/testresources/vk-topic-comments.json")));
            postComments = new String(readAllBytes(Paths.get("src/testresources/vk-post-comments.json")));
        } catch (IOException ignored) {
        }
    }

    @Test
    public void shouldCreatePostBodyWithAttachments() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parsePostsIndexPage(new URI("vk://id-18496184/index-posts/x3/0000000000"), posts);

        JsonNode content = objectMapper.readValue(result.getContent(), JsonNode.class);
        JsonNode firstPost = content.get(0);

        // then
        assertThat(firstPost.get("parseMeta").get("body").asText(), containsString("Мамочки поймут"));
    }

    @Test
    public void shouldCreatePostBodyForRepostWithOriginalText() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parsePostsIndexPage(new URI("vk://id-18496184/index-posts/x3/0000000000"), posts);

        JsonNode content = objectMapper.readValue(result.getContent(), JsonNode.class);
        JsonNode firstPost = content.get(1);

        // then
        assertThat(firstPost.get("parseMeta").get("body").asText(), containsString("Совет на все времена!"));
    }

    @Test
    public void shouldParseGroup() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parseGroup(new URI("vk://1/group"), group);

        // then
        assertThat(String.valueOf(result.getParseMeta().get("city")), is(equalTo("Санкт-Петербург")));
    }

    @Test
    public void shouldCreateGroupOutlinks() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parseGroup(new URI("vk://1/group"), group);

        // then
        assertThat(result.getLinks().keySet(), hasItems
                ("vk://id-1/index-posts", "vk://1/topics/x50/0"));
    }

    @Test
    public void shouldParseGroupTopics() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parseTopicsIndexPage(new URI("vk://18496184/topics/x50/0"), topics);

        JsonNode content = objectMapper.readValue(result.getContent(), JsonNode.class);
        JsonNode firstTopic = content.get(0);

        // then
        assertThat(firstTopic.get("parseMeta").get("body").asText(), containsString("«Пушкин»: обсуждаем сериал"));
        assertThat(firstTopic.get("parseMeta").get("engagement").asInt(), is(89));
        assertThat(firstTopic.get("parseMeta").get("post_date").asText(), is("2016-09-05T21:29:09.000+0300"));
    }

    @Test
    public void shouldParseGroupTopicsAndCreateOutlinks() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parseTopicsIndexPage(new URI("vk://18496184/topics/x50/0"), topics);

        // then
        assertThat(result.getLinks().keySet(), hasItems
                ("vk://18496184/topics/x50/1",
                        "vk://18496184/topics/33524500/x100/0",
                        "vk://18496184/topics/33524502/x100/0",
                        "vk://18496184/topics/32955627/x100/0",
                        "vk://18496184/topics/32955627/x100/1"));

    }

    @Test
    public void shouldParseGroupTopicsComments() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parseTopicCommentsPage(new URI("vk://18496184/topics/33524500/x100/0"), topicComments);

        JsonNode content = objectMapper.readValue(result.getContent(), JsonNode.class);
        JsonNode firstComment = content.get(0);

        // then
        assertThat(content.size(), is(5));
        assertThat(firstComment.get("parseMeta").get("body").asText(), containsString("#ВернитеЗакрытуюШколу"));
        assertThat(firstComment.get("parseMeta").get("engagement").asInt(), is(0));
        assertThat(firstComment.get("parseMeta").get("post_date").asText(), is("2016-07-01T13:31:54.000+0300"));
        assertThat(firstComment.get("parseMeta").get("parent_post_id").asText(), is("vk://18496184/topics/33524500"));
    }

    @Test
    public void shouldParseGroupTopicsCommentsAndCreateJoinedRecord() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parseTopicCommentsPage(new URI("vk://18496184/topics/33524500/x100/0"), topicComments);

        JsonNode content = objectMapper.readValue(result.getContent(), JsonNode.class);
        JsonNode secondComment = content.get(1);

        // then
        assertThat(secondComment.get("parseMeta").get("profile_gender").asText(), is("W"));
        assertThat(secondComment.get("parseMeta").get("profile_href").asText(), is("https://vk.com/id201123441"));
        assertThat(secondComment.get("parseMeta").get("profile_name").asText(), is("Светлана Харитонова"));
        assertThat(secondComment.get("parseMeta").get("sm_profile_id").asText(), is("201123441"));
        assertThat(secondComment.get("parseMeta").get("profile_id").asText(), is("vk://id201123441"));
    }

    @Test
    public void shouldParseGroupPostCommentsAndCreateJoinedRecord() throws IOException, URISyntaxException {
        // given when
        ParseResult result = vkParser.parseCommentPage(new URI("vk://id-18496184/posts/1494235/x100/0"), postComments);

        JsonNode content = objectMapper.readValue(result.getContent(), JsonNode.class);
        JsonNode thirdComment = content.get(2);

        // then
        assertThat(thirdComment.get("parseMeta").get("profile_gender").asText(), is("W"));
        assertThat(thirdComment.get("parseMeta").get("profile_href").asText(), is("https://vk.com/id379844276"));
        assertThat(thirdComment.get("parseMeta").get("profile_name").asText(), is("Алина Морозова"));
        assertThat(thirdComment.get("parseMeta").get("sm_profile_id").asText(), is("379844276"));
        assertThat(thirdComment.get("parseMeta").get("profile_id").asText(), is("vk://id379844276"));
        assertThat(thirdComment.get("parseMeta").get("reach").asInt(), is(0));
    }
}
