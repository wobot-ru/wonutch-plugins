package ru.wobot.sm.serialize;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public enum Serializer {
    INSTANCE;
    final private Gson gson = Builder.createGson();

    public static Serializer getInstance() {
        return Serializer.INSTANCE;
    }

    public <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
    public <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }

    public String toJson(Object obj) {
        return gson.toJson(obj);
    }
}
