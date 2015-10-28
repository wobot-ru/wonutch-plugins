package ru.wobot.vk.dto;

import org.springframework.social.vkontakte.api.VKontakteProfile;

import java.util.LinkedList;
import java.util.List;

public class FriendListDto {
    public List<VKontakteProfile> friends = new LinkedList<>();
}
