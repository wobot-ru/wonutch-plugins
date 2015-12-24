package ru.wobot.smm.core.dto;

public class PostIndex {
    public String[] postIds;
    public int totalCountOfPosts;

    public PostIndex(String[] postIds, int totalCountOfPosts) {
        this.postIds = postIds;
        this.totalCountOfPosts = totalCountOfPosts;
    }
}
