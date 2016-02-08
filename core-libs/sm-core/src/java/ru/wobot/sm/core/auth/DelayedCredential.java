package ru.wobot.sm.core.auth;

import org.joda.time.DateTimeUtils;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedCredential implements Delayed, Credential {
    private final String accessToken;
    private final String clientSecret;
    /**
     * Maximum requests per second with one Credential, allowed by provider.
     */
    private final int maxRequests;

    private int cnt;
    private long checkTime = -1;
    /**
     * Time, when this credential is ready to use. After creation - always ready.
     */
    private long startTime;

    public DelayedCredential(String accessToken, String clientSecret, int maxRequests) {
        this.accessToken = accessToken;
        this.clientSecret = clientSecret;
        this.maxRequests = maxRequests;
        this.startTime = DateTimeUtils.currentTimeMillis();
    }

    public void used() {
        if (maxRequests <= 0)
            return;
        long now = DateTimeUtils.currentTimeMillis();
        startTime = now;
        if (checkTime == -1) //first usage
            checkTime = now;

        if (++cnt == maxRequests) {
            long diff = now - checkTime;
            if (diff < 1000L) {
                startTime = checkTime = now + (1000L - diff);
                cnt = 0;
            } else {
                checkTime = now;
                cnt = 1;
            }
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = startTime - DateTimeUtils.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (this.startTime < ((DelayedCredential) o).startTime) {
            return -1;
        }
        if (this.startTime > ((DelayedCredential) o).startTime) {
            return 1;
        }
        return 0;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }
}
