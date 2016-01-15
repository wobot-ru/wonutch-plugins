package ru.wobot.sm.core.domain;

public class PostIndex {
    public String[] postIds;
    public int totalCountOfPosts;

    public PostIndex(String[] postIds, int totalCountOfPosts) {
        this.postIds = postIds;
        this.totalCountOfPosts = totalCountOfPosts;
    }
}
