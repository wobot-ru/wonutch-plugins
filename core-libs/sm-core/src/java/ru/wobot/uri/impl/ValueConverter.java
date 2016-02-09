package ru.wobot.uri.impl;

import java.lang.reflect.InvocationTargetException;

public class ValueConverter {
    private Class aClass;

    public ValueConverter(Class aClass) {
        this.aClass = aClass;
    }

    public ConvertResult convert(String from) {
        try {
            final Object converted = aClass.getConstructor(new Class[]{String.class}).newInstance(from);
            return new ConvertResult(converted);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        }
        return new ConvertResult();
    }

    public static String parseValueTemplate(String tmp) {
        tmp = tmp.substring(tmp.indexOf("{") + 1, tmp.indexOf("}"));
        if (tmp.isEmpty())
            throw new IllegalArgumentException();

        return tmp;
    }


    public class ConvertResult {
        private final boolean isConvertSuccess;
        private Object result;

        ConvertResult(Object result) {
            this.result = result;
            this.isConvertSuccess = true;
        }

        public ConvertResult() {
            isConvertSuccess = false;
        }

        public Object getResult() {
            return result;
        }

        public boolean isConvertSuccess() {
            return isConvertSuccess;
        }
    }
}
