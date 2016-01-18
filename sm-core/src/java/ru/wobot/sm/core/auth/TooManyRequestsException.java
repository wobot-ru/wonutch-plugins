package ru.wobot.sm.core.auth;

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
