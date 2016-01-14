package ru.wobot.smm.core;

import ru.wobot.smm.core.dto.ParseResult;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class Parsable {
    public ParseResult parse(URL url, String content) throws MalformedURLException {
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
