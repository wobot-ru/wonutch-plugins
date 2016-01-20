package ru.wobot.sm.core.parse;

import ru.wobot.sm.core.url.UrlCheck;

import java.net.URL;

public abstract class AbstractParser implements Parser {
    @Override
    public ParseResult parse(URL url, String content) {
        if (UrlCheck.isProfile(url)) {
            return parseProfile(url, content);
        }
        if (UrlCheck.isFriends(url)) {
            return parseFriends(url, content);
        }
        if (UrlCheck.isPostsIndex(url)) {
            return parsePostsIndex(url, content);
        }
        if (UrlCheck.isPostsIndexPage(url)) {
            return parsePostsIndexPage(url, content);
        }
        if (UrlCheck.isPost(url)) {
            return parsePost(url, content);
        }
        if (UrlCheck.isCommentPage(url)) {
            return parseCommentPage(url, content);
        }
        throw new UnsupportedOperationException();
    }

    protected abstract ParseResult parseProfile(URL url, String content);

    protected abstract ParseResult parseFriends(URL url, String content);

    protected abstract ParseResult parsePostsIndex(URL url, String content);

    protected abstract ParseResult parsePostsIndexPage(URL url, String content);

    protected abstract ParseResult parsePost(URL url, String content);

    protected abstract ParseResult parseCommentPage(URL url, String content);
}
