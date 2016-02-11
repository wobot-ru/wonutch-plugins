package ru.wobot.sm.core.parse;

import ru.wobot.sm.core.url.UrlCheck;

import java.net.URI;

public abstract class AbstractParser implements Parser {
    @Override
    public ParseResult parse(URI uri, String content) {
        if (UrlCheck.isProfile(uri)) {
            return parseProfile(uri, content);
        }
        if (UrlCheck.isFriends(uri)) {
            return parseFriends(uri, content);
        }
        if (UrlCheck.isPostsIndex(uri)) {
            return parsePostsIndex(uri, content);
        }
        if (UrlCheck.isPostsIndexPage(uri)) {
            return parsePostsIndexPage(uri, content);
        }
        if (UrlCheck.isPost(uri)) {
            return parsePost(uri, content);
        }
        if (UrlCheck.isCommentPage(uri)) {
            return parseCommentPage(uri, content);
        }
        throw new UnsupportedOperationException();
    }

    protected abstract ParseResult parseProfile(URI url, String content);

    protected abstract ParseResult parseFriends(URI url, String content);

    protected abstract ParseResult parsePostsIndex(URI url, String content);

    protected abstract ParseResult parsePostsIndexPage(URI url, String content);

    protected abstract ParseResult parsePost(URI url, String content);

    protected abstract ParseResult parseCommentPage(URI url, String content);
}
