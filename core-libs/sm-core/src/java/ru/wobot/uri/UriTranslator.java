package ru.wobot.uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wobot.sm.core.auth.TooManyRequestsException;
import ru.wobot.sm.core.reflect.MethodInvoker;
import ru.wobot.uri.impl.ConstSegment;
import ru.wobot.uri.impl.ConvertResult;
import ru.wobot.uri.impl.ParamSegment;
import ru.wobot.uri.impl.ParsedPath;
import ru.wobot.uri.impl.ParsedUri;
import ru.wobot.uri.impl.PathParser;
import ru.wobot.uri.impl.Segment;
import ru.wobot.uri.impl.ValueConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UriTranslator {
    private static final Logger LOG = LoggerFactory.getLogger(UriTranslator.class.getName());
    final Map<String, Collection<ParsedPath>> schemas;

    public UriTranslator(Object... objs) throws ClassNotFoundException {
        Objects.requireNonNull(objs);
        schemas = new HashMap<>();
        for (Object obj : objs) {
            Objects.requireNonNull(obj);
            final Class<?> aClass = obj.getClass();
            final Scheme scheme = findScheme(aClass);
            if (scheme == null)
                throw new IllegalArgumentException(obj.toString() + " should be annotated by Scheme");

            Collection<ParsedPath> paths = new ArrayList<>();
            final Method[] declaredMethods = aClass.getDeclaredMethods();
            for (Method m : declaredMethods) {
                Method method = findPathAnnotatedMethod(m);
                if (method != null) {
                    final Path path = method.getAnnotation(Path.class);
                    final Map<String, ValueConverter> converters = new HashMap<>();
                    final Map<String, ValueConverter> queryConverters = new LinkedHashMap<>();
                    int i = 0;
                    final Class<?>[] parameterTypes = method.getParameterTypes();
                    for (Class parameter : parameterTypes) {
                        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                        PathParam pathParam = null;
                        QueryParam queryParam = null;
                        for (Annotation annotation : parameterAnnotations[i++]) {
                            if (annotation instanceof PathParam)
                                pathParam = (PathParam) annotation;
                            if (annotation instanceof QueryParam)
                                queryParam = (QueryParam) annotation;
                            if (pathParam != null && queryParam != null)
                                break;
                        }

                        if (pathParam != null) {
                            if (pathParam.value().isEmpty())
                                throw new IllegalArgumentException(parameter.toString() + " PathParam can't be empty");
                            converters.put(pathParam.value(), new ValueConverter(parameter));
                        } else if (queryParam != null) {
                            if (queryParam.value().isEmpty())
                                throw new IllegalArgumentException(parameter.toString() + " QueryParam can't be empty");
                            queryConverters.put(queryParam.value(), new ValueConverter(parameter));
                        }
                    }
                    paths.add(PathParser.parse(new MethodInvoker(obj, m), path.value().trim(), converters, queryConverters));
                }
            }
            if (paths.isEmpty())
                throw new IllegalArgumentException(obj.toString() + " can't find Path annotation");
            else schemas.put(scheme.value(), paths);
        }
    }

    private Scheme findScheme(Class<?> aClass) {
        if (aClass.isAnnotationPresent(Scheme.class))
            return aClass.getAnnotation(Scheme.class);
        for (Class<?> i : aClass.getInterfaces()) {
            final Scheme scheme = findScheme(i);
            if (scheme != null) return scheme;
        }
        return null;
    }

    private Method findPathAnnotatedMethod(Method method) {
        if (method.isAnnotationPresent(Path.class))
            return method;
        final Class<?> declaringClass = method.getDeclaringClass();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> aClass : declaringClass.getInterfaces()) {
            try {
                final Method base = aClass.getDeclaredMethod(method.getName(), parameterTypes);
                if (base != null)
                    return findPathAnnotatedMethod(base);
            } catch (NoSuchMethodException e) {
                if (LOG.isDebugEnabled())
                    LOG.debug(org.apache.hadoop.util.StringUtils.stringifyException(e));

            }
        }
        final Class<?> superclass = declaringClass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            try {
                final Method base = superclass.getDeclaredMethod(method.getName(), parameterTypes);
                if (base != null)
                    return findPathAnnotatedMethod(base);
            } catch (NoSuchMethodException e) {
                if (LOG.isDebugEnabled())
                    LOG.debug(org.apache.hadoop.util.StringUtils.stringifyException(e));
            }
        }

        return null;
    }

    public <T> T translate(ParsedUri u) {
        final Collection<ParsedPath> paths = schemas.get(u.getScheme());
        if (paths == null)
            throw new IllegalArgumentException("Schema [" + u.getScheme() + "] is  not supported");
        for (ParsedPath path : paths) {
            boolean canInvoke = true;
            List<Object> params = new ArrayList<>();
            final Iterator<String> uriSegmentsIterator = u.getSegments().iterator();
            if (path.getSegments().size() == u.getSegments().size()) {
                for (Segment segment : path.getSegments()) {
                    if (segment instanceof ConstSegment) {
                        if (!((ConstSegment) segment).getName().equals(uriSegmentsIterator.next())) {
                            canInvoke = false;
                            break;
                        }
                    } else {
                        final ParamSegment paramSegment = (ParamSegment) segment;
                        final ConvertResult convertResult = paramSegment.convert(uriSegmentsIterator.next());
                        if (convertResult.isConvertSuccess())
                            params.add(convertResult.getResult());
                        else {
                            canInvoke = false;
                            break;
                        }
                    }
                }
                if (canInvoke)
                    try {
                        Object[] allParams = concat(params.toArray(), path.convertQuery(u.getQuery()));
                        return path.invoke(allParams);
                    } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
                        if (LOG.isErrorEnabled())
                            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
                        if (e.getCause() instanceof TooManyRequestsException)
                            throw (TooManyRequestsException) e.getCause();
                    }
            }
        }
        throw new UriNoMapException();
    }

    private <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
