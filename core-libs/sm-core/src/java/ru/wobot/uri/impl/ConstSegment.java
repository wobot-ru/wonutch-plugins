package ru.wobot.uri.impl;

public class ConstSegment implements Segment {
    private String name;

    public ConstSegment(String name) {
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
