package ru.wobot.uri;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class UriInvoker {
    final Map<String, Collection<Collection<Object>>> schemas;

    public UriInvoker(Object... objs) {
        Objects.requireNonNull(objs);
        schemas = new HashMap<>();
        for (Object obj : objs) {
            Objects.requireNonNull(obj);
            final Class<?> aClass = obj.getClass();
            final Scheme scheme = aClass.getAnnotation(Scheme.class);
            if (scheme == null)
                throw new IllegalArgumentException(obj.toString() + " should be annotated by Scheme");

            Collection<Collection<Object>> paths = new ArrayList<>();
            for (Method method : aClass.getDeclaredMethods()) {
                final Path path = method.getDeclaredAnnotation(Path.class);
                if (path != null) {
                    final String p = path.value().trim();
                    final Map<String, Object> params = new HashMap<>();
                    for (String seg : p.split("/")) {
                        params.put(seg, null);
                    }

                    Collection<Object> list = new ArrayList<>();
                    paths.add(list);
                }
            }
            if (paths.isEmpty()) throw new IllegalArgumentException(obj.toString() + " can't find Path annotation");
            else schemas.put(scheme.value(), paths);
        }
    }

    public void process(String u) throws URISyntaxException {
        URI uri = new URI(u);
        if (!schemas.containsKey(uri.getScheme()))
            throw new IllegalArgumentException(uri.getScheme() + " is schema not supported");
    }

    static class PathParser {
        static void parse(String p) {
            final String path = Objects.requireNonNull(p).trim();
            final String[] segments = path.split("/");
            if (segments.length == 0)
                throw new IllegalArgumentException("Incorrect path<" + path + ">, path can't be contain empty segment");
            for (String seg : segments) {
                if (seg == null || seg.isEmpty())
                    throw new IllegalArgumentException("Incorrect <" + seg + ">, segment can't be empty");
            }
        }
    }
}
