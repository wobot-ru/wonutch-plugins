package ru.wobot.sm.core.auth;

import org.joda.time.DateTimeUtils;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

public class TestDelayedCredential {
    @Test
    public void shouldBeReadyToUseAfterCreating() {
        //given
        DelayedCredential c = new DelayedCredential("", "", 1);

        // when

        // then
        // getDelay returns hom much time remains, before we can start to use Credential
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(lessThanOrEqualTo(0L)));
    }

    @Test
    public void shouldBeReadyToUseIfMaxRequestsNotReached() {
        //given
        DelayedCredential c = new DelayedCredential("", "", 2);

        // when
        c.used();

        // then
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(lessThanOrEqualTo(0L)));
    }

    @Test
    public void shouldNotBeReadyToUseIfMaxRequestsReached() {
        //given
        DelayedCredential c = new DelayedCredential("", "", 1);

        // when
        c.used();

        // then
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(greaterThan(0L)));
    }

    @Test
    public void shouldBeAlwaysReadyIfMaxRequestsLessThanOrEqualToZero() {
        //given
        DelayedCredential c = new DelayedCredential("", "", -1);

        // when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);

        // then
        for (int i = 0; i < 500; i++) { //some big value
            c.used();
            assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(lessThanOrEqualTo(0L)));
        }

        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldBeReadyToUseAfterSecond() {
        //given
        DelayedCredential c = new DelayedCredential("", "", 1);

        // when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);
        c.used();
        DateTimeUtils.setCurrentMillisFixed(curTime + 1000L);

        // then
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(lessThanOrEqualTo(0L)));
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldBeReadyToUseAfterSecondIfNoMaxRequestsReached() {
        //given
        DelayedCredential c = new DelayedCredential("", "", 2);

        // when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);
        c.used();
        DateTimeUtils.setCurrentMillisFixed(curTime + 1000L);
        c.used();

        // then
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(lessThanOrEqualTo(0L)));
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldNotBeReadyToUseAfterSecondIfMaxRequestsReached() {
        //given
        DelayedCredential c = new DelayedCredential("", "", 2);

        // when
        long curTime = 0L;
        DateTimeUtils.setCurrentMillisFixed(curTime);
        c.used();
        DateTimeUtils.setCurrentMillisFixed(curTime + 1000L);
        c.used();
        DateTimeUtils.setCurrentMillisFixed(curTime + 1010L);
        c.used();

        // then
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(greaterThan(0L)));
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldNotBeReadyToUseAfterTimeoutIfMaxRequestsReached() {
        //given
        DelayedCredential c = new DelayedCredential("", "", 1);

        // when
        long curTime = DateTimeUtils.currentTimeMillis();
        DateTimeUtils.setCurrentMillisFixed(curTime);

        c.used();
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(greaterThan(0L)));

        DateTimeUtils.setCurrentMillisFixed(curTime + 1000L);
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(lessThanOrEqualTo(0L)));
        c.used();

        // then
        assertThat(c.getDelay(TimeUnit.NANOSECONDS), is(greaterThan(0L)));
        DateTimeUtils.setCurrentMillisSystem();
    }

}
