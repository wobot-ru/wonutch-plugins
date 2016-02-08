package ru.wobot.sm.core.auth;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class TestProxy {
    private static Proxy proxy = Proxy.INSTANCE;

    static {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.DEBUG);
        rootLogger.addAppender(new ConsoleAppender(
                new PatternLayout("%d{ISO8601} %-5p %c{2} - %m%n")));
    }

    private Configuration conf = new Configuration();

    {
        proxy.setConf(conf);
    }

    public void setup(int maxRequests, Collection<String> credentialsSource) {
        conf.setInt("sm.requests.persecond", maxRequests);
        proxy.setCredentialsSource(credentialsSource);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldReturnCredentialFromQueue() {
        // given
        Collection<String> s = new ArrayList<>();
        s.add("first,1");
        setup(1, s);

        //when
        Credential c = proxy.getInstance();

        // then
        assertThat(c, is(not(nullValue())));
    }

    @Test
    public void shouldReturnCredentialWithoutClientSecretFromQueue() {
        // given
        Collection<String> s = new ArrayList<>();
        s.add("first");
        setup(1, s);

        //when
        Credential c = proxy.getInstance();

        // then
        assertThat(c.getAccessToken(), is("first"));
    }

    @Test
    public void shouldReturnNextCredential() {
        // given
        Collection<String> s = new ArrayList<>();
        s.add("first,1");
        s.add("second,2");
        setup(2, s);

        //when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);

        Credential c = proxy.getInstance(); // first Credential
        String at1 = c.getAccessToken();

        c = proxy.getInstance(); // second Credential
        String at2 = c.getAccessToken();

        // then
        assertThat(at1, is(not(equalTo(at2))));
    }

    @Test
    public void shouldReturnSameCredentialIfMaxRequestsNotReached() {
        // given
        Collection<String> s = new ArrayList<>();
        s.add("first,1");
        s.add("second,2");
        setup(2, s);

        //when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);

        Credential c = proxy.getInstance(); // first Credential
        String at1 = c.getAccessToken();

        c = proxy.getInstance(); // second Credential

        c = proxy.getInstance();  // first Credential again
        String at2 = c.getAccessToken();

        // then
        assertThat(at1, is(equalTo(at2)));
    }

    @Test(expected = TooManyRequestsException.class)
    public void shouldFailIfNextCredentialInSameSecond() {
        // given
        Collection<String> s = new ArrayList<>();
        s.add("first,1");
        s.add("second,2");
        setup(1, s);

        //when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);

        Credential c = proxy.getInstance(); // first Credential

        c = proxy.getInstance(); // second Credential

        c = proxy.getInstance();  // first Credential again

        // then, wait for exception
    }

    @Test(expected = TooManyRequestsException.class)
    public void shouldFailIfNextCredentialUsedInSameSecondAfterTimeout() {
        // given
        Collection<String> s = new ArrayList<>();
        s.add("first,1");
        s.add("second,2");
        setup(1, s);

        //when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);

        Credential c = proxy.getInstance(); // first Credential
        String at1 = c.getAccessToken();
        c = proxy.getInstance(); // second Credential
        String at2 = c.getAccessToken();
        assertThat(at1, is(not(equalTo(at2))));

        DateTimeUtils.setCurrentMillisFixed(curTime + 1000L);
        c = proxy.getInstance(); // first Credential
        at1 = c.getAccessToken();
        c = proxy.getInstance(); // second Credential
        at2 = c.getAccessToken();
        assertThat(at1, is(not(equalTo(at2))));

        // then, wait for exception
        c = proxy.getInstance();  // first Credential again, should fail
    }

    @Test
    public void shouldReturnSecondCredentialFromSource() {
        // given
        conf.setInt("mapreduce.job.maps", 2);
        conf.setInt("mapreduce.task.partition", 1);
        Collection<String> s = new ArrayList<>();
        s.add("first,1");
        s.add("second,2");
        setup(2, s);

        //when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);

        Credential c = proxy.getInstance(); // first and only Credential
        String at1 = c.getAccessToken();

        c = proxy.getInstance(); // first Credential again
        String at2 = c.getAccessToken();

        // then
        assertThat(at1, is(equalTo(at2)));
        assertThat(at1, is(equalTo("second")));
    }

    @Test
    public void shouldReturnTwoCredentialsFromSource() {
        // given
        conf.setInt("mapreduce.job.maps", 2);
        conf.setInt("mapreduce.task.partition", 0);
        Collection<String> s = new ArrayList<>();
        s.add("first,1");
        s.add("second,2");
        s.add("third,3");
        setup(2, s);

        //when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);

        Credential c = proxy.getInstance(); // first Credential
        String at1 = c.getAccessToken();

        c = proxy.getInstance(); // second Credential
        String at2 = c.getAccessToken();

        // then
        assertThat(at1, is(not(equalTo(at2))));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldReturnZeroCredentialsFromSource() {
        // given
        conf.setInt("mapreduce.job.maps", 3);
        conf.setInt("mapreduce.task.partition", 2);
        Collection<String> s = new ArrayList<>(2);
        s.add("first,1");
        s.add("second,2");
        setup(2, s);

        //when
        Credential c = proxy.getInstance(); // no Credentials
    }
}
