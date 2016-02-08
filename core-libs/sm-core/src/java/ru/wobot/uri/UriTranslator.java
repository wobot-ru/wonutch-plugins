package ru.wobot.uri;

import ru.wobot.sm.core.reflect.MethodInvoker;
import ru.wobot.uri.impl.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class UriTranslator {
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
                    final HashMap<String, ValueConverter> converters = new HashMap<>();
                    int i = 0;
                    PathParam pathParam = null;
                    final Class<?>[] parameterTypes = method.getParameterTypes();
                    for (Class parameter : parameterTypes) {
                        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                        for (Annotation annotation : parameterAnnotations[i++]) {
                            if (annotation instanceof PathParam) {
                                pathParam = (PathParam) annotation;
                                break;
                            }
                        }
                        if (pathParam == null)
                            throw new IllegalArgumentException(parameter.toString() + " should be annotated by PathParam");
                        final String paramVal = pathParam.value();
                        if (paramVal.isEmpty())
                            throw new IllegalArgumentException(parameter.toString() + " PathParam can't be empty");
                        converters.put(paramVal, new ValueConverter(parameter));
                    }
                    paths.add(PathParser.parse(new MethodInvoker(obj, m), path.value().trim(), converters));
                }
            }
            if (paths.isEmpty()) throw new IllegalArgumentException(obj.toString() + " can't find Path annotation");
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
            }
        }
        final Class<?> superclass = declaringClass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            try {
                final Method base = superclass.getDeclaredMethod(method.getName(), parameterTypes);
                if (base != null)
                    return findPathAnnotatedMethod(base);
            } catch (NoSuchMethodException e) {
            }
        }

        return null;
    }


    public <T> T translate(ParsedUri u) throws InvocationTargetException, IllegalAccessException {
        final Collection<ParsedPath> paths = schemas.get(u.getScheme());
        if (paths == null)
            throw new IllegalArgumentException(u.getScheme() + " is schema not supported");
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
                        final ValueConverter converter = (ValueConverter) segment;
                        final ValueConverter.ConvertResult convertResult = converter.convert(uriSegmentsIterator.next());
                        if (convertResult.isConvertSuccess())
                            params.add(convertResult.getResult());
                        else {
                            canInvoke = false;
                            break;
                        }
                    }
                }
                if (canInvoke)
                    return path.invoke(params.toArray());
            }
        }
        throw new UriNoMapException();
    }

}
