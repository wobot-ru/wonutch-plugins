package ru.wobot.sm.core.fetch;

import ru.wobot.sm.core.domain.SMProfile;

import java.io.IOException;
import java.util.List;

public interface SMService {
    List<SMProfile> getProfiles(List<String> userIds) throws IOException;

    List<String> getFriendIds(String userId) throws IOException;

    int getPostCount(String userId) throws IOException;

    List<String> getPostIds(String userId, int offset, int limit) throws IOException;

    FetchResponse getProfileData(String userId) throws IOException;

    FetchResponse getPostData(String userId, String postId) throws IOException;

    FetchResponse getPostsData(String userId, int skip, int take);

    FetchResponse getPostCommentsData(String userId, String postId, int skip, int take) throws IOException;
}
