package ru.wobot.sm.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.wobot.sm.core.parse.ParseResult;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestFbParser {
    private static final String RAW_PROFILE = "{\"id\":\"165107853523677\",\"about\":\"Бесценным опытом хочется делиться. Предлагаем вам делиться здесь тем, что для вас по-настоящему бесценно!\",\"likes\":13517133,\"link\":\"https://www.facebook.com/mastercardrussia/\",\"name\":\"MasterCard\",\"talking_about_count\":1381,\"username\":\"mastercardrussia\",\"website\":\"http://www.mastercard.ru http://www.mastercardpremium.ru http://gift.mastercard.ru http://cyclesandseasons.mastercard.ru http://www.mastercard.com \"}";
    private static final String RAW_POSTS = "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"id\": \"165107853523677_1095411087160011\",\n" +
            "      \"created_time\": \"2016-01-29T17:22:27+0000\",\n" +
            "      \"from\": {\n" +
            "        \"id\": \"165107853523677\",\n" +
            "        \"name\": \"MasterCard\",\n" +
            "        \"link\": \"https://www.facebook.com/mastercardrussia/\",\n" +
            "        \"likes\": 13519446\n" +
            "      },\n" +
            "      \"is_hidden\": false,\n" +
            "      \"is_published\": true,\n" +
            "      \"link\": \"https://www.facebook.com/mastercardrussia/photos/a.210500762317719.60293.165107853523677/1095411087160011/?type=3\",\n" +
            "      \"message\": \"Зима — это не только праздники, теплые вещи и время чудес. Это еще и обещания самому себе провести время с пользой и подготовиться к лету.\\n\\nНачните со здорового питания! Рекомендуем обратить внимание на Justforyou — сервис по доставке вкусной и сбалансированной пищи. Вы сможете не только обеспечить себя правильным питанием в зависимости от ваших целей (похудение, детокс, спортивное питание, меню для беременных и многое другое), но и получить скидку в 5% в рамках программы MasterCard Бесценные Города.\\n\\nЕще одна приятная деталь — при оплате на 10 дней и более вы получаете бесплатное исследование массы тела. Начните новую жизнь на  http://mstr.cd/1SghQMp\",\n" +
            "      \"name\": \"Timeline Photos\",\n" +
            "      \"object_id\": \"1095411087160011\",\n" +
            "      \"status_type\": \"added_photos\",\n" +
            "      \"type\": \"photo\",\n" +
            "      \"updated_time\": \"2016-01-29T17:22:27+0000\",\n" +
            "      \"likes\": {\n" +
            "        \"paging\": {\n" +
            "          \"cursors\": {\n" +
            "            \"after\": \"MTc1NDM5OTc5NDc5NTMz\",\n" +
            "            \"before\": \"MTUxMzM1MjA4ODk2OTc0Mg==\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"summary\": {\n" +
            "          \"total_count\": 18,\n" +
            "          \"can_like\": false,\n" +
            "          \"has_liked\": false\n" +
            "        }\n" +
            "      },\n" +
            "      \"comments\": {\n" +
            "        \"data\": [],\n" +
            "        \"summary\": {\n" +
            "          \"order\": \"ranked\",\n" +
            "          \"total_count\": 1,\n" +
            "          \"can_comment\": false\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"165107853523677_1094750970559356\",\n" +
            "      \"created_time\": \"2016-01-28T17:25:41+0000\",\n" +
            "      \"from\": {" +
            "           \"name\": \"Ольга Миленина\"," +
            "           \"id\": \"900662163382117\"" +
            "      }," +
            "      \"is_hidden\": false,\n" +
            "      \"is_published\": true,\n" +
            "      \"link\": \"https://www.facebook.com/mastercardrussia/photos/a.210500762317719.60293.165107853523677/1094750970559356/?type=3\",\n" +
            "      \"message\": \"Сегодня во всех кинотеатрах страны состоялась самая пухлая премьера года — мультфильм «Кунг-фу Панда 3»! Но только в сети кинотеатров «Формула кино» держатели MasterCard® получат скидку 10% на билеты. Впрочем, как и на все фильмы в течение 2016 года.\\n\\nНе пропустите!\",\n" +
            "      \"name\": \"Timeline Photos\",\n" +
            "      \"object_id\": \"1094750970559356\",\n" +
            "      \"status_type\": \"added_photos\",\n" +
            "      \"type\": \"photo\",\n" +
            "      \"updated_time\": \"2016-01-30T19:56:30+0000\",\n" +
            "      \"shares\": {\n" +
            "        \"count\": 20\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    private static final String RAW_COMMENT = "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"message\": \"Если постараться не ездить в метро.не заходить в арабские кварталы.то по-прежнему Париж - город-сказка.Это Монмартр.Нотр-Дам.Елисейские поля,уютные кафе.сад Тюильри...Но есть опасения,что мигранты - это начало конца Европы.\",\n" +
            "      \"from\": {\n" +
            "        \"name\": \"Lidia  Mazurova\",\n" +
            "        \"id\": \"884167345038247\"\n" +
            "      },\n" +
            "      \"like_count\": 0,\n" +
            "      \"created_time\": \"2016-01-08T02:44:25+0000\",\n" +
            "      \"id\": \"1081856348515485_1082735698427550\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String RAW_REPLY = "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"message\": \"А вы что супермодель? Я много раз была в арабских районах и не заметила особого отношения к собственной персоне! С арабами или без - Париж это праздник!\",\n" +
            "      \"from\": {\n" +
            "        \"name\": \"Evgenia Zinovjeva\",\n" +
            "        \"id\": \"1117812631564394\"\n" +
            "      },\n" +
            "      \"like_count\": 4,\n" +
            "      \"parent\": {\n" +
            "        \"id\": \"1081856348515485_1082735698427550\"\n" +
            "      },\n" +
            "      \"created_time\": \"2016-01-08T02:44:25+0000\",\n" +
            "      \"id\": \"1081856348515485_1082938725073914\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private final FbParser fbParser = new FbParser();

    @Test
    public void shouldParseProfileContent() throws IOException {
        // given when
        ParseResult result = fbParser.parseProfile(new URL("fb://mastercardrussia"),
                RAW_PROFILE);

        // then
        assertThat(result.getContent(), containsString("165107853523677")); //ID
    }

    @Test
    public void shouldCreateProfileOutLinks() throws IOException {
        // given when
        ParseResult result = fbParser.parseProfile(new URL("fb://mastercardrussia"),
                RAW_PROFILE);

        // then
        assertThat(result.getLinks().keySet(), hasItems("fb://165107853523677/friends",
                "fb://165107853523677/index-posts/x100/00000000"));
    }

    @Test
    public void shouldCreateFriendsOutLinks() throws IOException {
        // given when
        ParseResult result = fbParser.parseFriends(new URL("fb://165107853523677/friends"), //mastercardrussia
                "[\"431891506856669\", \"21435141328\"]");

        // then
        assertThat(result.getLinks().keySet(), hasItems("fb://431891506856669",
                "fb://21435141328"));
    }

    @Test
    public void shouldCreatePostsOutLinks() throws IOException {
        // given when
        ParseResult result = fbParser.parsePostsIndexPage(new URL
                ("fb://165107853523677/index-posts/x100/00000000"), RAW_POSTS);

        // then
        assertThat(result.getLinks().size(), is(2));
        assertThat(result.getLinks().keySet(), hasItems
                ("fb://165107853523677/posts/165107853523677_1095411087160011/x100/0",
                        "fb://165107853523677/posts/165107853523677_1094750970559356/x100/0"));
    }

    @Test
    public void shouldCountPostsEngagement() throws IOException {
        // given
        ParseResult result = fbParser.parsePostsIndexPage(new URL
                ("fb://165107853523677/index-posts/x100/00000000"), RAW_POSTS);

        //when
        JsonNode node = getJsonContent(result);
        JsonNode firstPost = node.get(0);
        JsonNode secondPost = node.get(1);

        // then
        assertThat(firstPost.get("parseMeta").get("engagement").asInt(), is(19));
        assertThat(secondPost.get("parseMeta").get("engagement").asInt(), is(20));
    }

    @Test
    public void shouldFormPostsHref() throws IOException {
        // given
        ParseResult result = fbParser.parsePostsIndexPage(new URL
                ("fb://165107853523677/index-posts/x100/00000000"), RAW_POSTS);

        //when
        JsonNode node = getJsonContent(result);
        JsonNode firstPost = node.get(0);
        JsonNode secondPost = node.get(1);

        // then
        assertThat(firstPost.get("parseMeta").get("href").asText(),
                is("https://www.facebook.com/165107853523677/posts/1095411087160011"));
        assertThat(secondPost.get("parseMeta").get("href").asText(),
                is("https://www.facebook.com/165107853523677/posts/1094750970559356"));
    }

    @Test
    public void shouldFormPostsProfileId() throws IOException {
        // given
        ParseResult result = fbParser.parsePostsIndexPage(new URL
                ("fb://165107853523677/index-posts/x100/00000000"), RAW_POSTS);

        //when
        JsonNode node = getJsonContent(result);
        JsonNode firstPost = node.get(0);
        JsonNode secondPost = node.get(1);

        // then
        assertThat(firstPost.get("parseMeta").get("profile_id").asText(), is("fb://165107853523677"));
        assertThat(secondPost.get("parseMeta").get("profile_id").asText(), is("fb://900662163382117"));
    }

    @Test
    public void shouldFormCommentsHrefIfParentIsPost() throws IOException {
        // given
        ParseResult result = fbParser.parseCommentPage(new URL
                ("fb://165107853523677/posts/165107853523677_1081856348515485/x100/0"), RAW_COMMENT);

        //when
        JsonNode node = getJsonContent(result);
        JsonNode firstComment = node.get(0);

        // then
        assertThat(firstComment.get("parseMeta").get("href").asText(),
                is("https://www.facebook.com/165107853523677/posts/1081856348515485?comment_id=1082735698427550"));
    }

    @Test
    public void shouldFormCommentsParentIfParentIsPost() throws IOException {
        // given
        ParseResult result = fbParser.parseCommentPage(new URL
                ("fb://165107853523677/posts/165107853523677_1081856348515485/x100/0"), RAW_COMMENT);

        //when
        JsonNode node = getJsonContent(result);
        JsonNode firstComment = node.get(0);

        // then
        assertThat(firstComment.get("parseMeta").get("parent_post_id").asText(),
                is("fb://165107853523677/posts/165107853523677_1081856348515485"));
    }

    @Test
    public void shouldFormCommentsOutlinksIfParentIsPost() throws IOException {
        // given
        ParseResult result = fbParser.parseCommentPage(new URL
                ("fb://165107853523677/posts/165107853523677_1081856348515485/x100/0"), RAW_COMMENT);

        // then
        assertThat(result.getLinks().size(), is(2));
        assertThat(result.getLinks().keySet(), hasItems
                ("fb://165107853523677/posts/1081856348515485_1082735698427550/x100/0",
                        "fb://884167345038247?scope=user"));
    }

    @Test
    public void shouldFormCommentsHrefIfParentIsComment() throws IOException {
        // given
        ParseResult result = fbParser.parseCommentPage(new URL
                ("fb://165107853523677/posts/1081856348515485_1082735698427550/x100/0"), RAW_REPLY);

        //when
        JsonNode node = getJsonContent(result);
        JsonNode firstComment = node.get(0);

        // then
        assertThat(firstComment.get("parseMeta").get("href").asText(),
                is("https://www.facebook.com/165107853523677/posts/1081856348515485?comment_id=1082938725073914"));
    }

    @Test
    public void shouldFormCommentsParentIfParentIsComment() throws IOException {
        // given
        ParseResult result = fbParser.parseCommentPage(new URL
                ("fb://165107853523677/posts/1081856348515485_1082735698427550/x100/0"), RAW_REPLY);

        //when
        JsonNode node = getJsonContent(result);
        JsonNode firstComment = node.get(0);

        // then
        assertThat(firstComment.get("parseMeta").get("parent_post_id").asText(),
                is("fb://165107853523677/posts/165107853523677_1081856348515485"));
    }

    @Test
    public void shouldFormCommentsOutlinksIfParentIsComment() throws IOException {
        // given
        ParseResult result = fbParser.parseCommentPage(new URL
                ("fb://165107853523677/posts/1081856348515485_1082735698427550/x100/0"), RAW_REPLY);

        // then
        assertThat(result.getLinks().size(), is(2));
        assertThat(result.getLinks().keySet(), hasItems
                ("fb://1117812631564394?scope=user",
                        "fb://165107853523677/posts/1081856348515485_1082938725073914/x100/0"));
    }

    private JsonNode getJsonContent(ParseResult result) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(result.getContent(), JsonNode.class);
    }
}
