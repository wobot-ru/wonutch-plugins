package ru.wobot.sm.core.auth;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class TestCookieRepository {
    static {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.DEBUG);
        rootLogger.addAppender(new ConsoleAppender(
                new PatternLayout("%d{ISO8601} %-5p %c{2} - %m%n")));
    }

    private CookieRepository cookieRepository = new CookieRepository(new Configuration());

    @Test
    public void shouldReturnCookies() {
        // given

        //when
        Collection<String> s = cookieRepository.getCookies();

        // then
        assertThat(s, is(not(nullValue())));
    }

    @Test
    public void shouldReturnFirstCookie() {
        // given

        //when
        List<String> cookies = (List<String>) cookieRepository.getCookies();

        // then
        assertThat(cookies.get(2), containsString("6uDOVuoYfrMqczTtQGCgnjoE")); // 'datr' cookie value
    }

    @Test
    public void shouldReturnThirdCookie() {
        // given

        //when
        cookieRepository.getCookies(); // first
        cookieRepository.getCookies(); // second
        List<String> cookies = (List<String>) cookieRepository.getCookies();

        // then
        assertThat(cookies.get(2), containsString("HOHOVu0kfz7KpQEqH9km1pZY")); // 'datr' cookie value
    }

    @Test
    public void shouldReturnFirstCookieAgain() {
        // given

        //when
        cookieRepository.getCookies(); // first
        cookieRepository.getCookies(); // second
        cookieRepository.getCookies(); // third
        List<String> cookies = (List<String>) cookieRepository.getCookies(); // first again

        // then
        assertThat(cookies.get(2), containsString("6uDOVuoYfrMqczTtQGCgnjoE")); // 'datr' cookie value
    }

    @Test
    public void shouldReturnFirstCookieSetAsNameValuePairs() {
        // given

        //when
        List<String> cookies = (List<String>) cookieRepository.getCookiesAsNameValuePairs(); // first again

        // then
        assertThat(cookies.get(2), is("datr=6uDOVuoYfrMqczTtQGCgnjoE")); // 'datr' cookie
    }
}
