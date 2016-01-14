package ru.wobot.parsers;

import ru.wobot.smm.core.Parsable;
import ru.wobot.vk.serialize.Serializer;


public abstract class BaseParsable extends Parsable {
    static <T> T fromJson(String json, Class<T> classOfT) {
        return Serializer.getInstance().fromJson(json, classOfT);
    }

    static String toJson(Object obj) {
        return Serializer.getInstance().toJson(obj);
    }
}
