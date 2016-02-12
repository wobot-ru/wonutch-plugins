package ru.wobot.uri.impl;

import ru.wobot.sm.core.reflect.MethodInvoker;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class ParsedPath {
    private final MethodInvoker invoker;
    private final Collection<Segment> segments;
    private final Map<String, ValueConverter> queryConverters;

    public ParsedPath(MethodInvoker invoker, Collection<Segment> segments, Map<String, ValueConverter> queryConverters) {
        this.invoker = Objects.requireNonNull(invoker);
        this.segments = Objects.requireNonNull(segments);
        this.queryConverters = Objects.requireNonNull(queryConverters);
    }

    public Collection<Segment> getSegments() {
        return segments;
    }

    public <T> T invoke(Object... objs) throws InvocationTargetException, IllegalAccessException {
        return invoker.invoke(objs);
    }

    public Object[] convertQuery(Map<String, String> query) {
        final Object[] results = new Object[queryConverters.size()];
        int i = 0;
        for (Map.Entry<String, ValueConverter> entry : queryConverters.entrySet()) {
            final ValueConverter converter = entry.getValue();
            final ConvertResult convertResult = converter.convert(query.get(entry.getKey()));
            if (convertResult.isConvertSuccess())
                results[i++] = convertResult.getResult();
        }
        return results;
    }
}
