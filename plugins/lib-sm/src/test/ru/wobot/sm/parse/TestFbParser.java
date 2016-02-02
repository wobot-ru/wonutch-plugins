package ru.wobot.sm.parse;

import org.junit.Test;
import ru.wobot.sm.core.parse.ParseResult;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;

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
            "          \"total_count\": 0,\n" +
            "          \"can_comment\": false\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"165107853523677_1094750970559356\",\n" +
            "      \"created_time\": \"2016-01-28T17:25:41+0000\",\n" +
            "      \"from\": {\n" +
            "        \"id\": \"165107853523677\",\n" +
            "        \"name\": \"MasterCard\",\n" +
            "        \"link\": \"https://www.facebook.com/mastercardrussia/\",\n" +
            "        \"likes\": 13519446\n" +
            "      },\n" +
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
            "        \"count\": 18\n" +
            "      },\n" +
            "      \"likes\": {\n" +
            "        \"paging\": {\n" +
            "          \"cursors\": {\n" +
            "            \"after\": \"MTcwNTAzNTk2MzA0MTgxNg==\",\n" +
            "            \"before\": \"NjQzODc3NTQyNDE5NTAx\"\n" +
            "          },\n" +
            "          \"next\": \"https://graph.facebook.com/v2.5/165107853523677_1094750970559356/likes?summary=true&fields=name,id,username,link,profile_type&limit=25&after=MTcwNTAzNTk2MzA0MTgxNg%3D%3D\"\n" +
            "        },\n" +
            "        \"summary\": {\n" +
            "          \"total_count\": 589,\n" +
            "          \"can_like\": false,\n" +
            "          \"has_liked\": false\n" +
            "        }\n" +
            "      },\n" +
            "      \"comments\": {\n" +
            "        \"paging\": {\n" +
            "          \"cursors\": {\n" +
            "            \"after\": \"WTI5dGJXVnVkRjlqZFhKemIzSTZNVEE1TlRrNU5ESXdNemMyT0RNMk5qb3hORFUwTVRnek56a3c=\",\n" +
            "            \"before\": \"WTI5dGJXVnVkRjlqZFhKemIzSTZNVEE1TlRJek5EY3pNemcwTkRNeE16b3hORFUwTURjME56azE=\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"summary\": {\n" +
            "          \"order\": \"ranked\",\n" +
            "          \"total_count\": 10,\n" +
            "          \"can_comment\": false\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    private FbParser fbParser = new FbParser();

    @Test
    public void shouldParseProfileContent() throws IOException {
        // given when
        ParseResult result = fbParser.parseProfile(new URL("fb://mastercardrussia"),
                RAW_PROFILE);

        // then
        assertThat(result.getContent(), containsString("165107853523677"));  // test via ID, it shouldn't change
    }

    @Test
    public void shouldCreateProfileOutLinks() throws IOException {
        // given when
        ParseResult result = fbParser.parseProfile(new URL("fb://mastercardrussia"),
                RAW_PROFILE);

        // then
        assertThat(result.getLinks().keySet(), hasItems("fb://mastercardrussia/friends",
                "fb://mastercardrussia/index-posts/x100/00000000"));
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
    public void shouldParsePostsPageContent() throws IOException {
        // given when
        ParseResult result = fbParser.parsePostsIndexPage(new URL
                        ("fb://mastercardrussia/index-posts/x100/00000000"),
                RAW_POSTS);

        // then
        assertThat(result.getContent(), stringContainsInOrder(Arrays.asList("165107853523677", "18", "https://www.facebook.com/mastercardrussia/photos/a.210500762317719.60293.165107853523677/1094750970559356")));
    }

    @Test
    public void shouldCreatePostsOutLinks() throws IOException {
        // given when
        ParseResult result = fbParser.parsePostsIndexPage(new URL
                        ("fb://mastercardrussia/index-posts/x100/00000000"),
                RAW_POSTS);

        // then
        assertThat(result.getLinks().size(), is(2));
        assertThat(result.getLinks().keySet(), hasItems
                ("fb://mastercardrussia/posts/165107853523677_1095411087160011/x100/0",
                        "fb://mastercardrussia/posts/165107853523677_1094750970559356/x100/0"));

    }
}
