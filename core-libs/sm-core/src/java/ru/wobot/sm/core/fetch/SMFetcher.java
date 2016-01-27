package ru.wobot.sm.core.fetch;

import ru.wobot.sm.core.domain.SMProfile;

import java.io.IOException;
import java.util.List;

public interface SMFetcher {
    List<SMProfile> getProfiles(List<String> userIds) throws IOException;

    List<String> getFriendIds(String userId) throws IOException;

    int getPostCount(String userId) throws IOException;

    FetchResponse getPostsData(String userId, long offset, int limit) throws IOException;

    FetchResponse getProfileData(String userId) throws IOException;

    FetchResponse getPostData(String userId, String postId) throws IOException;

    FetchResponse getPostCommentsData(String userId, String postId, int skip, int take) throws IOException;
}