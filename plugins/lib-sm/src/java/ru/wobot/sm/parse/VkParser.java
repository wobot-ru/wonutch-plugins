package ru.wobot.sm.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.social.UncategorizedApiException;
import org.springframework.social.vkontakte.api.Comment;
import org.springframework.social.vkontakte.api.CommentsResponse;
import org.springframework.social.vkontakte.api.Counters;
import org.springframework.social.vkontakte.api.Group;
import org.springframework.social.vkontakte.api.Post;
import org.springframework.social.vkontakte.api.VKontakteProfile;
import org.springframework.social.vkontakte.api.attachment.Attachment;
import org.springframework.social.vkontakte.api.attachment.LinkAttachment;
import org.springframework.social.vkontakte.api.attachment.PhotoAttachment;
import org.springframework.social.vkontakte.api.attachment.VideoAttachment;
import org.springframework.social.vkontakte.api.impl.json.VKArray;
import ru.wobot.sm.core.api.VkApiTypes;
import ru.wobot.sm.core.mapping.PostProperties;
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Sources;
import ru.wobot.sm.core.mapping.Types;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.parse.ParseResult;
import ru.wobot.sm.core.parse.Parser;
import ru.wobot.sm.serialize.Serializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class VkParser implements Parser {
    private static final String DIGEST = "digest";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ParseResult parse(URI uri, String content, String apiType, String apiVersion) {
        switch (apiType) {
            case VkApiTypes.PROFILE:
                return parseProfile(uri, content);
            case VkApiTypes.GROUP:
                return parseGroup(uri, content);
            case VkApiTypes.FRIEND_LIST_OF_ID:
                return parseFriends(uri, content);
            case VkApiTypes.POST:
                return parsePost(uri, content);
            case VkApiTypes.POST_COUNT:
                return parsePostsIndex(uri, content);
            case VkApiTypes.POST_BULK:
                return parsePostsIndexPage(uri, content);
            case VkApiTypes.COMMENT_BULK:
                return parseCommentPage(uri, content);
            case VkApiTypes.TOPIC_BULK:
                return parseTopicsIndexPage(uri, content);
            case VkApiTypes.TOPIC_COMMENT_BULK:
                return parseTopicCommentsPage(uri, content);
        }
        throw new UnsupportedOperationException("Parser for this content not found.");
    }

    protected ParseResult parseProfile(URI uri, String content) {
        final String urlString = uri.toString();
        final String userDomain = uri.getHost();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        VKontakteProfile profile = Serializer.getInstance().fromJson(content, VKontakteProfile.class);

        final HashMap<String, String> links = new HashMap<String, String>(3) {
            {
                // generate link <a href='vk://user/friends'>user-friends</a>
                put(urlString + "/friends", userDomain + "-friends");

                // generate link <a href='vk://user/index-posts'>user-index-posts</a>
                put(urlString + "/index-posts", userDomain + "-index-posts");
            }
        };
        /*String facebook = profile.getFacebook();
        if (facebook != null) {
            // hack: user http://vk.com/id16812401
            // contains facebook link to http://facebook.com/app_scoped_user_id/100008451725336
            // when VK API return json property facebook: '+100008451725336' then we trim it
            if (facebook.startsWith("+"))
                facebook = facebook.substring(1);

            String anchor = profile.getFacebookName();
            links.put("https://www.facebook.com/profile.php?id=" + facebook, anchor == null ? userDomain + "-facebook" : anchor);
        }*/

        // fill parse metadata
        final String fullName = profile.getFirstName() + " " + profile.getLastName();
        parseMeta.put(ProfileProperties.SOURCE, Sources.VKONTAKTE);
        parseMeta.put(ProfileProperties.NAME, fullName);
        parseMeta.put(ProfileProperties.HREF, profile.getProfileURL());
        parseMeta.put(ProfileProperties.SM_PROFILE_ID, String.valueOf(profile.getId()));
        if (profile.getCity() != null) {
            parseMeta.put(ProfileProperties.CITY, String.valueOf(profile.getCity().getTitle()));
        }
        if (profile.getGender() != null) {
            parseMeta.put(ProfileProperties.GENDER, profile.getGender());
        }
        Counters counters = profile.getCounters();
        if (counters != null) {
            parseMeta.put(ProfileProperties.REACH, counters.getFollowers() + counters.getFriends());
        }

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.PROFILE);
        return new ParseResult(urlString, fullName, content, links, parseMeta, contentMeta);
    }

    protected ParseResult parseGroup(URI uri, String content) {
        final String urlString = uri.toString();
        final String userDomain = uri.getHost();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        Group group = Serializer.getInstance().fromJson(content, Group.class);
        final HashMap<String, String> links = new HashMap<String, String>(3) {
            {
                // generate link <a href='vk://user/index-posts'>user-index-posts</a>
                put(Sources.VKONTAKTE + "://id-" + userDomain + "/index-posts", userDomain + "-index-posts");
                // generate link <a href='vk://user/topics/x50/0'>user-index-posts</a>
                put(Sources.VKONTAKTE + "://" + userDomain + "/topics/x50/0", userDomain + "-index-topics-x50-page-0");
            }
        };

        // fill parse metadata
        parseMeta.put(ProfileProperties.SOURCE, Sources.VKONTAKTE);
        parseMeta.put(ProfileProperties.NAME, group.getGroupName());
        parseMeta.put(ProfileProperties.HREF, "https://vk.com/public" + group.getGroupId());
        parseMeta.put(ProfileProperties.SM_PROFILE_ID, String.valueOf(group.getGroupId()));
        if (group.getCity() != null) {
            parseMeta.put(ProfileProperties.CITY, String.valueOf(group.getCity().getTitle()));
        }
        parseMeta.put(ProfileProperties.REACH, group.getMembersCount());

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.PROFILE);
        return new ParseResult(urlString, group.getGroupName(), content, links, parseMeta, contentMeta);
    }

    protected ParseResult parseFriends(URI uri, String content) {
        final String userDomain = uri.getHost();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        String[] friendIds = Serializer.getInstance().fromJson(content, String[].class);

        Map<String, String> links = new HashMap<>(friendIds.length);
        for (String friendId : friendIds) {
            String friendHref = Sources.VKONTAKTE + "://" + friendId;
            links.put(friendHref, friendId);
        }

        return new ParseResult(uri.toString(), userDomain + "-friends", content, links, parseMeta, contentMeta);
    }

    protected ParseResult parsePostsIndex(URI uri, String content) {
        final String userDomain = uri.getHost();
        final String urlString = uri.toString();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        final int postsCount = Serializer.getInstance().fromJson(content, int.class);
        final int indexPageCount = postsCount / 100;
        final HashMap<String, String> links = new HashMap<>(indexPageCount);
        final boolean isAuth = uri.getQuery() != null && uri.getQuery().contains("auth");
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%08d", i);
            // generate link <a href='vk://user/index-posts/x100/00000001'>user-index-posts-x100-page-1</a>
            final String link;

            if (isAuth)
                link = uri.getScheme() + "://" + uri.getHost() + uri.getPath() + "/x100/" + blockNumber + "?" + uri.getQuery();
            else
                link = uri.getScheme() + "://" + uri.getHost() + uri.getPath() + "/x100/" + blockNumber;
            links.put(link, userDomain + "-index-posts-x100-page-" + i);
        }
        return new ParseResult(urlString, userDomain + "-index-posts", content, links, parseMeta, contentMeta);
    }

    protected ParseResult parsePostsIndexPage(URI uri, String content) {
        final String userDomain = uri.getHost();
        final String urlPrefix = Sources.VKONTAKTE + "://" + userDomain + "/posts/";
        final boolean isAuth = uri.getQuery() != null && uri.getQuery().contains("auth");

        Type collectionType = new TypeToken<VKArray<Post>>() {
        }.getType();
        VKArray<Post> posts = Serializer.getInstance().fromJson(content, collectionType);

        Map<String, String> links = null;
        ParseResult[] parseResults = null;
        int i = 0;
        if (posts != null && posts.getItems() != null) {
            parseResults = new ParseResult[posts.getItems().size()];
            links = new HashMap<>(posts.getItems().size());
            for (Post post : posts.getItems()) {
                final int commentPageCount = post.getComments().getCount() / 100;
                for (long block = 0; block <= commentPageCount; block++) {
                    String blockNumber = String.format("%06d", block);

                    if (isAuth)
                        links.put(urlPrefix + post.getId() + "/x100/" + blockNumber + "?auth", "comment-index-x100-page-" + block);
                    else
                        links.put(urlPrefix + post.getId() + "/x100/" + blockNumber, "comment-index-x100-page-" + block);
                }
                Map<String, Object> postContent = new HashMap<>();
                Map<String, Object> postParse = new HashMap<>();

                final String ownerProfile = Sources.VKONTAKTE + "://" + "id" + post.getOwnerId();
                postParse.put(PostProperties.SOURCE, Sources.VKONTAKTE);
                postParse.put(PostProperties.PROFILE_ID, ownerProfile);
                postParse.put(PostProperties.HREF, "https://vk.com/wall" + post.getOwnerId() + "_" + post.getId()); //like http://vk.com/wall1_730207
                postParse.put(PostProperties.SM_POST_ID, post.getId());
                postParse.put(PostProperties.BODY, buildPostBody(post));
                //todo: replace to JodaTime
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                postParse.put(PostProperties.POST_DATE, dateFormat.format(post.getDate()));
                int engagement = post.getLikes().getCount() + post.getReposts().getCount() + post.getComments().getCount();
                postParse.put(PostProperties.ENGAGEMENT, engagement);
                postParse.put(PostProperties.IS_COMMENT, false);

                // fill content metadata
                postContent.put(ContentMetaConstants.TYPE, Types.POST);
                postContent.put(ContentMetaConstants.PARENT, ownerProfile);
                postContent.put(DIGEST, DigestUtils.md5Hex(post.toString()));
                parseResults[i++] = new ParseResult(urlPrefix + post.getId(), new HashMap<String, String>(), postParse, postContent);
            }
        }
        return new ParseResult(uri.toString(), userDomain, Serializer.getInstance().toJson(parseResults), (links
                == null ? new HashMap<String, String>() : links), new HashMap<String, Object>(), getCommonContentMeta());
    }

    private Map<String, Object> getCommonContentMeta() {
        return new HashMap<String, Object>() {{
            put(ContentMetaConstants.MULTIPLE_PARSE_RESULT, true);
        }};
    }

    private String buildPostBody(Post post) {
        List<String> body = new ArrayList<>();
        body.add(post.getText());

        if (post.getAttachments() != null && !post.getAttachments().isEmpty()) {
            Attachment att = post.getAttachments().get(0); // process only first attachment for now. TODO: add all
            switch (att.getType()) { // process only most common attachments for now. TODO: add all
                case VIDEO:
                    body.add(((VideoAttachment) att).getVideo().getTitle());
                    body.add(((VideoAttachment) att).getVideo().getDescription());
                    break;
                case PHOTO:
                    body.add(((PhotoAttachment) att).getPhoto().getText());
                    break;
                case LINK:
                    body.add(((LinkAttachment) att).getLink().getTitle());
                    body.add(((LinkAttachment) att).getLink().getDescription());
                    break;
            }
        }

        if (post.getCopyHistory() != null && !post.getCopyHistory().isEmpty()) {
            Post original = post.getCopyHistory().get(0); // process only first original post for now. TODO: add all
            body.add(buildPostBody(original));
        }
        return StringUtils.join(body, " ");
    }

    protected ParseResult parsePost(URI uri, String content) {
        final String urlString = uri.toString();
        final Map<String, Object> parseMeta = new HashMap<>();
        final Map<String, Object> contentMeta = new HashMap<>();

        Post post = Serializer.getInstance().fromJson(content, Post.class);
        final int indexPageCount = post.getComments().getCount() / 100;

        final Map<String, String> links = new HashMap<>(indexPageCount);
        for (long i = 0; i <= indexPageCount; i++) {
            String blockNumber = String.format("%06d", i);
            // generate link <a href='vk://user/posts/1/x100/000001'>post-index-x100-page-1</a>
            links.put(urlString + "/x100/" + blockNumber, "post-index-x100-page-" + i);
        }

        final String ownerProfile = Sources.VKONTAKTE + "://" + "id" + post.getOwnerId();
        parseMeta.put(PostProperties.SOURCE, Sources.VKONTAKTE);
        parseMeta.put(PostProperties.PROFILE_ID, ownerProfile);
        parseMeta.put(PostProperties.HREF, "https://vk.com/wall" + post.getOwnerId() + "_" + post.getId()); //like http://vk.com/wall1_730207
        parseMeta.put(PostProperties.SM_POST_ID, post.getId());
        parseMeta.put(PostProperties.BODY, post.getText());
        //todo: replace to JodaTime
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        parseMeta.put(PostProperties.POST_DATE, dateFormat.format(post.getDate()));
        int engagement = post.getLikes().getCount() + post.getReposts().getCount() + post.getComments().getCount();
        parseMeta.put(PostProperties.ENGAGEMENT, engagement);
        parseMeta.put(PostProperties.IS_COMMENT, false);
        parseMeta.put(DIGEST, DigestUtils.md5Hex(post.toString()));

        // fill content metadata
        contentMeta.put(ContentMetaConstants.TYPE, Types.POST);
        contentMeta.put(ContentMetaConstants.PARENT, ownerProfile);
        return new ParseResult(urlString, links, parseMeta, contentMeta);
    }

    protected ParseResult parseCommentPage(URI uri, String content) {
        final String user = uri.getHost();
        final int userId = Integer.parseInt(user.substring(2));
        final String path = uri.getPath();
        final String[] split = path.split("/");
        final int postId = Integer.parseInt(split[2]);
        final int page = Integer.parseInt(split[4]);
        final Map<String, String> links = new IdentityHashMap<>();

        CommentsResponse response = Serializer.getInstance().fromJson(content, CommentsResponse.class);
        Map<Long, VKontakteProfile> authors = new HashMap<>();
        for (VKontakteProfile author : response.getProfiles())
            authors.put(author.getId(), author);
        //todo: replace to JodaTime
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        final ParseResult[] parseResults = new ParseResult[response.getComments().size()];
        int i = 0;
        for (final Comment comment : response.getComments()) {
            final String postUrl = Sources.VKONTAKTE + "://" + user + "/posts/" + postId;
            final VKontakteProfile author = authors.get(comment.getFromId());
            final String commentOwnerProfile = Sources.VKONTAKTE + "://id" + comment.getFromId();
            links.put(commentOwnerProfile, "");

            String commentUrl = postUrl + "/comments/" + comment.getId();
            Map<String, Object> commentParse = new HashMap<String, Object>() {{
                put(PostProperties.SOURCE, Sources.VKONTAKTE);
                put(PostProperties.PROFILE_ID, commentOwnerProfile);
                put(PostProperties.PARENT_POST_ID, postUrl);
                put(PostProperties.HREF, "https://vk.com/wall" + userId + "_" + postId + "?reply=" + comment.getId());
                put(PostProperties.SM_POST_ID, comment.getId());
                put(PostProperties.BODY, comment.getText());
                put(PostProperties.POST_DATE, dateFormat.format(comment.getDate()));
                //todo: is only one number?
                put(PostProperties.ENGAGEMENT, comment.getLikes().getCount());
                put(PostProperties.IS_COMMENT, true);
                put(DIGEST, DigestUtils.md5Hex(comment.toString()));
                //todo: add constants
                if (author != null) {
                    put("profile_gender", author.getGender());
                    put("profile_href", "https://vk.com/id" + author.getId());
                    put("profile_name", author.getFirstName() + " " + author.getLastName());
                    put(ProfileProperties.SM_PROFILE_ID, author.getId());
                    put(ProfileProperties.REACH, 0);
                }
            }};

            Map<String, Object> commentContentMeta = new HashMap<String, Object>() {{
                put(ContentMetaConstants.PARENT, commentOwnerProfile);
                if (author != null)
                    put(ContentMetaConstants.TYPE, Types.DETAILED_POST);
                else
                    put(ContentMetaConstants.TYPE, Types.POST);
            }};
            parseResults[i++] = new ParseResult(commentUrl, new HashMap<String, String>(), commentParse, commentContentMeta);
        }

        return new ParseResult(uri.toString(), user + "|post=" + postId + "|page=" + page, Serializer.getInstance().toJson(parseResults), links, new HashMap<String, Object>(), getCommonContentMeta());
    }

    protected ParseResult parseTopicsIndexPage(URI uri, String content) {
        final String userDomain = uri.getHost();
        final String urlPrefix = Sources.VKONTAKTE + "://" + userDomain + "/topics/";

        JsonNode topics;
        JsonNode node;
        try {
            node = objectMapper.readValue(content, JsonNode.class);
            topics = node.get("items");
        } catch (IOException e) {
            throw new UncategorizedApiException("vkontakte", "Error deserializing topics for group [" +
                    userDomain + "]", e);
        }

        Map<String, String> links = null;
        ParseResult[] parseResults = null;
        int i = 0;
        if (topics != null && topics.size() != 0) {
            parseResults = new ParseResult[topics.size()];
            links = new HashMap<>();

            String[] parts = uri.toString().split("/");
            if (parts[5].equals("0")) {
                int pageSize = Integer.valueOf(parts[4].replace("x", ""));
                int totalCount = node.get("count").asInt();
                if (totalCount > pageSize) {
                    int pageCount = totalCount / pageSize;
                    for (int j = 1; j <= pageCount; j++) {
                        links.put(urlPrefix + "x" + pageSize + "/" + j, userDomain + "-index-topics-x50-page-" + j);
                    }
                }
            }

            for (JsonNode topic : topics) {
                int commentPageCount = topic.get("comments").asInt() / 100;
                for (int block = 0; block <= commentPageCount; block++) {
                    String blockNumber = String.valueOf(block);
                    links.put(urlPrefix + topic.get("id").asText() + "/x100/" + blockNumber, "comment-index-x100-page-" + block);
                }
                Map<String, Object> postContent = new HashMap<>();
                Map<String, Object> postParse = new HashMap<>();

                final String ownerProfile = Sources.VKONTAKTE + "://" + userDomain + "/group";
                postParse.put(PostProperties.SOURCE, Sources.VKONTAKTE);
                postParse.put(PostProperties.PROFILE_ID, ownerProfile);
                postParse.put(PostProperties.HREF, "https://vk.com/topic-" + userDomain + "_" + topic.get("id").asText()); //like https://vk.com/topic-18496184_33524500
                postParse.put(PostProperties.SM_POST_ID, topic.get("id").asText());
                postParse.put(PostProperties.BODY, topic.get("title").asText());
                //todo: replace to JodaTime
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                postParse.put(PostProperties.POST_DATE, dateFormat.format(new Date(topic.get("updated").asLong() * 1000)));
                postParse.put(PostProperties.ENGAGEMENT, topic.get("comments").asInt());
                postParse.put(PostProperties.IS_COMMENT, false);

                // fill content metadata
                postContent.put(ContentMetaConstants.TYPE, Types.POST);
                postContent.put(ContentMetaConstants.PARENT, ownerProfile);
                postContent.put(DIGEST, DigestUtils.md5Hex(topic.toString()));
                parseResults[i++] = new ParseResult(urlPrefix + topic.get("id").asText(), new HashMap<String, String>(), postParse, postContent);
            }
        }
        return new ParseResult(uri.toString(), userDomain, Serializer.getInstance().toJson(parseResults), (links
                == null ? new HashMap<String, String>() : links), new HashMap<String, Object>(), getCommonContentMeta());
    }

    protected ParseResult parseTopicCommentsPage(URI uri, String content) {
        final String user = uri.getHost();
        String path = uri.getPath();
        String[] split = path.split("/");
        final String postId = split[2];
        int page = Integer.parseInt(split[4]);
        Map<String, String> links = new IdentityHashMap<>();

        JsonNode comments;
        JsonNode node;
        JsonNode profiles;
        JsonNode groups;
        try {
            node = objectMapper.readValue(content, JsonNode.class);
            comments = node.get("items");
            profiles = node.get("profiles");
            groups = node.get("groups");
        } catch (IOException e) {
            throw new UncategorizedApiException("vkontakte", "Error deserializing topic comments for group [" +
                    user + "], topic [" + postId + "]", e);
        }
        //todo: replace to JodaTime
        final Map<String, JsonNode> authors = new HashMap<>();
        putToMap(authors, profiles);
        putToMap(authors, groups);
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        final ParseResult[] parseResults = new ParseResult[comments.size()];
        int i = 0;
        for (final JsonNode comment : comments) {
            final String commentId = comment.get("id").asText();
            final String authorId = comment.get("from_id").asText();
            final String postUrl = Sources.VKONTAKTE + "://" + user + "/topics/" + postId;
            final String commentOwnerProfile = Sources.VKONTAKTE + "://id" + authorId;
            links.put(commentOwnerProfile, "");
            final JsonNode author = authors.get(authorId);
            String commentUrl = postUrl + "/comments/" + commentId;
            Map<String, Object> commentParse = new HashMap<String, Object>() {{
                put(PostProperties.SOURCE, Sources.VKONTAKTE);
                put(PostProperties.PROFILE_ID, commentOwnerProfile);
                put(PostProperties.PARENT_POST_ID, postUrl);
                put(PostProperties.HREF, "https://vk.com/topic-" + user + "_" + postId + "?post=" + commentId);
                put(PostProperties.SM_POST_ID, commentId);
                put(PostProperties.BODY, comment.get("text").asText());
                put(PostProperties.POST_DATE, dateFormat.format(new Date(comment.get("date").asLong() * 1000)));
                //todo: is only one number?
                put(PostProperties.ENGAGEMENT, comment.get("likes").get("count").asText());
                put(PostProperties.IS_COMMENT, true);
                put(DIGEST, DigestUtils.md5Hex(comment.toString()));
                //todo: add constants
                if (author != null) {
                    put("profile_href", "https://vk.com/id" + authorId);
                    put(ProfileProperties.SM_PROFILE_ID, authorId);
                    put(ProfileProperties.REACH, 0);
                    if (author.get("sex") != null)
                        put("profile_gender", author.get("sex").asInt() == 0 ? "M" : "W");
                    if (author.get("first_name") != null)
                        put("profile_name", author.get("first_name").asText() + " " + author.get("last_name").asText());
                    else
                        put("profile_name", author.get("name").asText());

                }
            }};

            Map<String, Object> commentContentMeta = new HashMap<String, Object>() {{
                put(ContentMetaConstants.PARENT, commentOwnerProfile);
                if (author != null)
                    put(ContentMetaConstants.TYPE, Types.DETAILED_POST);
                else
                    put(ContentMetaConstants.TYPE, Types.POST);
            }};
            parseResults[i++] = new ParseResult(commentUrl, new HashMap<String, String>(), commentParse, commentContentMeta);
        }

        return new ParseResult(uri.toString(), user + "|topic=" + postId + "|page=" + page, Serializer.getInstance().toJson(parseResults), links, new HashMap<String, Object>(), getCommonContentMeta());
    }

    private void putToMap(Map<String, JsonNode> authors, JsonNode node) {
        for (JsonNode author : node)
            authors.put(author.get("id").asText(), author);
    }

}
