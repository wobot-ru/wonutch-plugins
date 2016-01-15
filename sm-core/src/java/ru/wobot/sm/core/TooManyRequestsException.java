package ru.wobot.sm.core;

/**
 * Created by lmisakyan on 25.12.2015.
 */
public class TooManyRequestsException extends RuntimeException {
    private final long delay;

    public TooManyRequestsException(long delay) {
        super("Too many requests per time unit. Wait for: " + delay + " milliseconds.");
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }
}
