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
    private final Content content;
    private final Metadata contentMetadata = new Metadata();
    private final Metadata parseMetadata = new Metadata();
    private final Document document;
    private final List<Outlink> outlinks = new ArrayList<>();

    public ProfileParser(Content content) {
        this.content = content;
        try {
            document = Jsoup.parse(new ByteArrayInputStream(content.getContent()), "UTF-8", content.getUrl());
            parseFriends();
            parseFollowers();
            addContactInfoLink(content.getUrl());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseFriends() {
        Elements tags = document.select("._50f8._50f4 a");
        if (tags != null) {
            Element first = tags.first();
            if (first != null) {
                parseMetadata.add(ProfileProperties.FRIEND_COUNT, first.text().replace(",", ""));
            }
        }
    }

    private void parseFollowers() {
        Elements tags = document.select("._42ef ._50f3 a");
        if (tags != null) {
            Element tag = tags.last();
            if (tag != null) {
                String text = tag.text();
                if (text.contains("people") && !text.contains("other people")) {
                    text = text.replace(",", "");
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

    public ParseResult getParseResult() {
        Metadata contentMetadata = content.getMetadata();
        contentMetadata.add(ContentMetaConstants.TYPE, Types.PROFILE);
        String followers = parseMetadata.get(ProfileProperties.FOLLOWER_COUNT);
        String friends = parseMetadata.get(ProfileProperties.FRIEND_COUNT);
        parseMetadata.add(ProfileProperties.REACH, String.valueOf(
                (followers == null ? 0 : Integer.parseInt(followers)) +
                (friends == null ? 0 : Integer.parseInt(friends))
        ));
        parseMetadata.add(ProfileProperties.SM_PROFILE_ID, contentMetadata.get("app.scoped.user.id"));
        parseMetadata.add(ProfileProperties.SOURCE, Sources.FACEBOOK);
        parseMetadata.add(ProfileProperties.NAME, document.title());
        String url = content.getUrl();
        if (url.contains("as_id"))
            url = url.substring(0, url.lastIndexOf("as_id") - 1);
        parseMetadata.add(ProfileProperties.HREF, url);

        ParseData parseData = new ParseData(ParseStatus.STATUS_SUCCESS, document.title(), outlinks.toArray(new Outlink[outlinks.size()]), contentMetadata, parseMetadata);
        return ParseResult.createParseResult(content.getUrl(), new ParseImpl(new ParseText(document.title()), parseData));
    }
}
