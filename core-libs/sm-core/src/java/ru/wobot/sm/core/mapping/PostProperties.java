package ru.wobot.sm.core.mapping;

public interface PostProperties {
    String SOURCE = "source";
    String PROFILE_ID = "profile_id";
    String HREF = "href";
    String SM_POST_ID = "sm_post_id";
    String PARENT_POST_ID = "parent_post_id";
    String BODY = "body";
    String POST_DATE = "post_date";
    /**
     * Engagement is measured as the number of times your post is liked, shared or commented on.
     *
     * @see <a href="https://wobot-ru.atlassian.net/wiki/x/FAAq">Вовлеченность</a>
     */
    String ENGAGEMENT = "engagement";
    String IS_COMMENT = "is_comment";
}
