package org.apache.nutch.parse.fb;

import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.parse.ParseText;
import org.apache.nutch.protocol.Content;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Sources;
import ru.wobot.sm.core.mapping.Types;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class ProfileParser {
    private static final String FACEBOOK_URI = "https://www.facebook.com";

    private final Content content;
    private final Metadata contentMetadata = new Metadata();
    private final Metadata parseMetadata = new Metadata();
    private final Document document;
    private final List<Outlink> outlinks = new ArrayList<>();
    private final String url;

    public ProfileParser(Content content) {
        this.content = content;
        this.url = content.getUrl();
        try {
            document = Jsoup.parse(new ByteArrayInputStream(content.getContent()), "UTF-8", this.url);
            parseFriends();
            parseFollowers();
            //addContactInfoLink(content.getUrl());
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse content for URL [" + this.url + "]", e);
        }
    }

    private void parseFriends() {
        Elements tags = document.select("._50f8._50f4 a");
        if (tags != null) {
            Element first = tags.first();
            if (first != null) {
                parseMetadata.add(ProfileProperties.FRIEND_COUNT, first.text().replace(",", "").replace("\u00A0", ""));
            }
        }
    }

    private void parseFollowers() {
        Elements tags = document.select("._42ef ._50f3 a[href*=followers]");
        if (tags != null) {
            Element tag = tags.last();
            if (tag != null) {
                String text = tag.text();
                if ((text.contains("people") && !text.contains("other people")) || text.contains("человек")) {
                    text = text.replace(",", "").replace("\u00A0", "");
                    parseMetadata.add(ProfileProperties.FOLLOWER_COUNT, text.substring(0, text.indexOf(" ")));
                }
            }
        }
    }

    private void addContactInfoLink(String url) throws MalformedURLException {
        if (url.contains("profile.php?id="))
            outlinks.add(new Outlink(url + "&sk=about&section=contact-info", null));
        else
            outlinks.add(new Outlink(url + "/about?section=contact-info", null));
    }

    private String getId() {
        Elements tags = document.select("meta[property=al:android:url]");
        if (tags != null) {
            Element tag = tags.first();
            if (tag != null) {
                String url = tag.attr("content");
                return url.substring(url.lastIndexOf("/") + 1);
            }
        }
        return null;
    }

    private String getName() {
        Element element = document.getElementById("fb-timeline-cover-name");
        if (element != null)
            return document.getElementById("fb-timeline-cover-name").text();
        else {
            String name = document.title();
            if (name.contains("("))
                name = name.substring(0, name.indexOf("("));
            return name.trim();
        }
    }

    private String getCity() {
        Elements tags;
        tags = document.select("._1zw6._md0._5vb9 ._50f3:matchesOwn(Lives|Живет) a");
        if (tags == null || tags.size() == 0)
            tags = document.select("#current_city a");

        if (tags != null) {
            Element city = tags.last();
            if (city != null) {
                String full = city.text();
                return full.split(",")[0];
            }
        }
        return null;
    }

    public ParseResult getParseResult() {
        String userId = getId();
        if (userId == null)
            throw new IllegalStateException("Can't find user ID in html of [" + url + "]");

        Metadata contentMetadata = content.getMetadata();
        contentMetadata.add(ContentMetaConstants.TYPE, Types.PROFILE);

        String followers = parseMetadata.get(ProfileProperties.FOLLOWER_COUNT);
        String friends = parseMetadata.get(ProfileProperties.FRIEND_COUNT);

        parseMetadata.add(ProfileProperties.REACH, String.valueOf(
                (followers == null ? 0 : Integer.parseInt(followers)) +
                        (friends == null ? 0 : Integer.parseInt(friends))
        ));
        parseMetadata.add("app_scoped_user_id", contentMetadata.get("app.scoped.user.id"));
        parseMetadata.add(ProfileProperties.SM_PROFILE_ID, userId);
        parseMetadata.add(ProfileProperties.SOURCE, Sources.FACEBOOK);
        parseMetadata.add(ProfileProperties.NAME, getName());
        parseMetadata.add(ProfileProperties.HREF, FACEBOOK_URI + "/profile.php?id=" + userId);
        parseMetadata.add(ProfileProperties.CITY, getCity());

        ParseData parseData = new ParseData(ParseStatus.STATUS_SUCCESS, document.title(), outlinks.toArray(new Outlink[outlinks.size()]), contentMetadata, parseMetadata);
        return ParseResult.createParseResult(content.getUrl(), new ParseImpl(new ParseText(document.title()), parseData));
    }
}
