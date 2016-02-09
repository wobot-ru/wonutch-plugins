package ru.wobot.uri.impl;

public class ParamSegment implements Segment {
    private final String pattern;
    private final ValueConverter converter;
    private final int start;
    private final int end;

    public ParamSegment(String pattern, ValueConverter converter) {
        start = pattern.indexOf("{");
        end = pattern.indexOf("}")+1;
        this.pattern = pattern;
        this.converter = converter;
    }

    public ValueConverter.ConvertResult convert(String from) {
        return converter.convert(from.substring(start, end-(pattern.length()-from.length())));
    }
}
