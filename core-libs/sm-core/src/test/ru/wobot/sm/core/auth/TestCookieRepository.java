package ru.wobot.sm.core.auth;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import java.net.HttpCookie;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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

    private CookieRepository cookieRepository = new CookieRepository();

    {
        cookieRepository.setConf(new Configuration());
    }

    @Test
    public void shouldReturnCookiesAndProxy() {
        // given

        //when
        LoginData loginData = cookieRepository.getLoginData();

        // then
        assertThat(loginData, is(not(nullValue())));
    }

    @Test
    public void shouldReturnFirstCookie() {
        // given

        //when
        LoginData cookies = cookieRepository.getLoginData();

        // then
        assertThat(((List<HttpCookie>) cookies.getCookies()).get(2).getValue(), is(equalTo("6uDOVuoYfrMqczTtQGCgnjoE"))); // 'datr' cookie value
    }

    @Test
    public void shouldReturnFirstProxy() {
        // given

        //when
        LoginData loginData = cookieRepository.getLoginData();

        // then
        assertThat(loginData.getProxy(), is(equalTo("82.103.140.46:6060")));
    }

    @Test
    public void shouldReturnThirdCookie() {
        // given

        //when
        cookieRepository.getLoginData(); // first
        cookieRepository.getLoginData(); // second
        List<HttpCookie> cookies = (List<HttpCookie>) cookieRepository.getLoginData().getCookies();

        // then
        assertThat(cookies.get(2).getValue(), is(equalTo("HOHOVu0kfz7KpQEqH9km1pZY"))); // 'datr' cookie value
    }

    @Test
    public void shouldReturnFirstCookieAgain() {
        // given

        //when
        cookieRepository.getLoginData(); // first
        cookieRepository.getLoginData(); // second
        cookieRepository.getLoginData(); // third
        List<HttpCookie> cookies = (List<HttpCookie>) cookieRepository.getLoginData().getCookies(); // first again

        // then
        assertThat(cookies.get(2).getValue(), is(equalTo("6uDOVuoYfrMqczTtQGCgnjoE"))); // 'datr' cookie value
    }

}
