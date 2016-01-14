package ru.wobot.sm.dto;

public class PostIndex {
    public long[] postIds;
    public int totalCountOfPosts;

    public PostIndex(long[] postIds, int totalCountOfPosts) {
        this.postIds = postIds;
        this.totalCountOfPosts = totalCountOfPosts;
    }
}
