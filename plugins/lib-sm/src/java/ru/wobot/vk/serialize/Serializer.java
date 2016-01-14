package ru.wobot.vk.serialize;

import com.google.gson.Gson;

public enum Serializer {
    INSTANCE;
    final private Gson gson = Builder.createGson();

    public static Serializer getInstance() {
        return Serializer.INSTANCE;
    }

    public <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public String toJson(Object obj) {
        return gson.toJson(obj);
    }
}
