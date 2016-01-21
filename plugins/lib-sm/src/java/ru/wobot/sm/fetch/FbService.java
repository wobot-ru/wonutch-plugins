package ru.wobot.sm.fetch;

import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.SMService;

import java.io.IOException;
import java.util.List;

public class FbService implements SMService {
    @Override
    public List<SMProfile> getProfiles(List<String> userIds) throws IOException {
        return null;
    }

    @Override
    public List<String> getFriendIds(String userId) throws IOException {
        return null;
    }

    @Override
    public int getPostCount(String userId) throws IOException {
        return 0;
    }

    @Override
    public List<String> getPostIds(String userId, int offset, int limit) throws IOException {
        return null;
    }

    @Override
    public FetchResponse getProfileData(String userId) throws IOException {
        return null;
    }

    @Override
    public FetchResponse getPostData(String userId, String postId) throws IOException {
        return null;
    }

    @Override
    public FetchResponse getPostCommentsData(String userId, String postId, int skip, int take) throws IOException {
        return null;
    }
}
