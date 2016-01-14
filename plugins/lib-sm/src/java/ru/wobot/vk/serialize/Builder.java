package ru.wobot.vk.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.social.vkontakte.api.attachment.Attachment;

public class Builder {
    public static Gson createGson() {
        //todo: remove repeated initialisation
        Gson gson = new GsonBuilder().registerTypeAdapter(Attachment.class,
                new PropertyBasedInterfaceMarshal())
                .create();
        return gson;
    }
}