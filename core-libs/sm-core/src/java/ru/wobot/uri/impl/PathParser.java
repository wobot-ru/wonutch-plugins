package ru.wobot.uri.impl;

import ru.wobot.sm.core.reflect.MethodInvoker;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class PathParser {
    public static ParsedPath parse(MethodInvoker invoker, String path, Map<String, ValueConverter> converters, Map<String, ValueConverter> queryConverters) {
        final String trimmedPath = Objects.requireNonNull(path).trim();
        final String[] segments = trimmedPath.split("/");
        if (segments.length == 0)
            throw new IllegalArgumentException("Incorrect path<" + path + ">, path can't be contain empty segment");
        final ArrayList<Segment> parsedSegs = new ArrayList<>(segments.length);
        for (String seg : segments) {
            if (seg == null || seg.isEmpty())
                throw new IllegalArgumentException("Incorrect <" + seg + ">, segment can't be empty");

            if (seg.contains("{")) parsedSegs.add(new ParamSegment(seg, converters.get(ValueConverter.parseValueTemplate(seg))));
            else
                parsedSegs.add(new ConstSegment(seg));
        }

        return new ParsedPath(invoker, parsedSegs, queryConverters);
    }
}
