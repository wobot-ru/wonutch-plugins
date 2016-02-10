package ru.wobot.sm.core.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInvoker {
    private Object object;
    private Method method;

    public MethodInvoker(Object object, Method method) {
        this.object = object;
        this.method = method;
    }

    public <T> T invoke(Object... params) throws InvocationTargetException, IllegalAccessException {
        return (T) method.invoke(object, params);
    }
}
