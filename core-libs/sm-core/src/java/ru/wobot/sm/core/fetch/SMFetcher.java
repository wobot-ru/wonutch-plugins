package ru.wobot.sm.core.fetch;

import ru.wobot.sm.core.domain.SMProfile;
import ru.wobot.uri.Path;
import ru.wobot.uri.PathParam;

import java.io.IOException;
import java.util.List;

public interface SMFetcher {
    List<SMProfile> getProfiles(List<String> userIds) throws IOException;

    @Path("{userId}/friends")
    List<String> getFriendIds(@PathParam("userId") String userId) throws IOException;

    @Path("{userId}/index-posts")
    int getPostCount(@PathParam("userId") String userId) throws IOException;

    @Path("{userId}/index-posts/x{offset}/{limit}")
    FetchResponse getPostsData(@PathParam("userId") String userId, @PathParam("offset") long offset, @PathParam("limit") int limit) throws IOException;

    @Path("{userId}")
    FetchResponse getProfileData(@PathParam("userId") String userId) throws IOException;

    @Path("{userId}/posts/{postId}")
    FetchResponse getPostData(@PathParam("userId") String userId, @PathParam("postId") String postId) throws IOException;

    @Path("{userId}/posts/{postId}/x{take}/{skip}")
    FetchResponse getPostCommentsData(@PathParam("userId") String userId, @PathParam("postId") String postId, @PathParam("take") int take, @PathParam("skip") int skip) throws IOException;
}
