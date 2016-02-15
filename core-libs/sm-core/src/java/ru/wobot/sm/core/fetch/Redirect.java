package ru.wobot.sm.core.fetch;

public class Redirect implements Response {
    private String location;

    public Redirect(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
