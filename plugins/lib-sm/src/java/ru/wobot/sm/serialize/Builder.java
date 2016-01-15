package ru.wobot.sm.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.social.vkontakte.api.attachment.Attachment;

public class Builder {
    public static Gson createGson() {
        //todo: remove repeated initialisation
        return new GsonBuilder().registerTypeAdapter(Attachment.class,
                new PropertyBasedInterfaceMarshal())
                .create();
    }
}