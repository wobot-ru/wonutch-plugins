package ru.wobot.uri.impl;

import java.lang.reflect.InvocationTargetException;

public class ValueConverter {
    private boolean isPrimitiveType;
    private Class aClass;

    public ValueConverter(Class aClass) {
        this.aClass = aClass;
        if (Boolean.TYPE == aClass) isPrimitiveType = true;
        if (Boolean.TYPE == aClass) isPrimitiveType = true;
        if (Byte.TYPE == aClass) isPrimitiveType = true;
        if (Short.TYPE == aClass) isPrimitiveType = true;
        if (Integer.TYPE == aClass) isPrimitiveType = true;
        if (Long.TYPE == aClass) isPrimitiveType = true;
        if (Float.TYPE == aClass) isPrimitiveType = true;
        if (Double.TYPE == aClass) isPrimitiveType = true;
    }

    public ConvertResult convert(String value) {
        try {
            if (isPrimitiveType)
                return new ConvertResult(convertPrimitive(value));
            Object result = aClass.getConstructor(new Class[]{String.class}).newInstance(value);
            return new ConvertResult(result);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        }
        return ConvertResult.getFailedConvertResult();
    }

    public static String parseValueTemplate(String tmp) {
        tmp = tmp.substring(tmp.indexOf("{") + 1, tmp.indexOf("}"));
        if (tmp.isEmpty())
            throw new IllegalArgumentException();

        return tmp;
    }

    private Object convertPrimitive(String value) {
        if (Boolean.TYPE == aClass) return Boolean.parseBoolean(value);
        if (Byte.TYPE == aClass) return Byte.parseByte(value);
        if (Short.TYPE == aClass) return Short.parseShort(value);
        if (Integer.TYPE == aClass) return Integer.parseInt(value);
        if (Long.TYPE == aClass) return Long.parseLong(value);
        if (Float.TYPE == aClass) return Float.parseFloat(value);
        if (Double.TYPE == aClass) return Double.parseDouble(value);
        throw new IllegalArgumentException();
    }
}
