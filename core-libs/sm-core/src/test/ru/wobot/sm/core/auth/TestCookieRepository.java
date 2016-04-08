package ru.wobot.sm.core.auth;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import java.net.HttpCookie;
import java.util.Collection;
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
    public void shouldReturnTwoCookieSets() {
        // given

        //when
        LoginData loginData = cookieRepository.getLoginData();

        // then
        assertThat(loginData.getCookieSets().size(), is(2));
    }

    @Test
    public void shouldReturnFirstCookieSets() {
        // given

        //when
        List<Collection<HttpCookie>> cookieSets = (List<Collection<HttpCookie>>)cookieRepository.getLoginData().getCookieSets();
        List<HttpCookie> cookies = (List<HttpCookie>)cookieSets.get(0);

        // then
        assertThat(cookies.get(1).getValue(), is(equalTo("eUcFV7M8--hHjv_TObjjqn8L"))); // 'datr' cookie value
    }

    @Test
    public void shouldReturnFirstProxy() {
        // given

        //when
        LoginData loginData = cookieRepository.getLoginData();

        // then
        assertThat(loginData.getProxy(), is(equalTo("184.75.209.130:6060")));
    }

    @Test
    public void shouldReturnSecondCookieSets() {
        // given

        //when
        cookieRepository.getLoginData(); // first
        List<Collection<HttpCookie>> cookieSets = (List<Collection<HttpCookie>>)cookieRepository.getLoginData().getCookieSets();
        List<HttpCookie> cookies = (List<HttpCookie>)cookieSets.get(0); // first "cookie set"

        // then
        assertThat(cookies.get(1).getValue(), is(equalTo("jEcFV73mZnmKc0lwOAJMTU_v"))); // 'datr' cookie value
    }

    @Test
    public void shouldReturnFirstCookieSetsAgain() {
        // given

        //when
        cookieRepository.getLoginData(); // first
        cookieRepository.getLoginData(); // second
        List<Collection<HttpCookie>> cookieSets = (List<Collection<HttpCookie>>)cookieRepository.getLoginData().getCookieSets();
        List<HttpCookie> cookies = (List<HttpCookie>)cookieSets.get(1); // second "cookie set"

        // then
        assertThat(cookies.get(1).getValue(), is(equalTo("gkcFVzJWphU-HfSdwImvbq6F"))); // 'datr' cookie value
    }
}
