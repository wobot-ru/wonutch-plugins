package ru.wobot.uri.impl;

import ru.wobot.sm.core.reflect.MethodInvoker;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class ParsedPath {
    private MethodInvoker invoker;
    private Collection<Segment> segments;

    public ParsedPath(MethodInvoker invoker, Collection<Segment> segments) {
        this.invoker = Objects.requireNonNull(invoker);
        this.segments = Objects.requireNonNull(segments);
    }

    public Collection<Segment> getSegments() {
        if (segments == null) segments = new ArrayList<>();
        return segments;
    }

    public <T> T invoke(Object... objs) throws InvocationTargetException, IllegalAccessException {
        return invoker.invoke(objs);
    }
}
