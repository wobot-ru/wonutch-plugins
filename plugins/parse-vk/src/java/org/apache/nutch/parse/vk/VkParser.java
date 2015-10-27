package org.apache.nutch.parse.vk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.*;
import org.apache.nutch.protocol.Content;
import ru.wobot.vk.UrlCheck;

import java.net.MalformedURLException;
import java.net.URL;

public class VkParser implements Parser {

    private static final Log LOG = LogFactory.getLog(VkParser.class
            .getName());
    private Configuration conf;
    private String defaultCharEncoding;

    public Configuration getConf() {
        return this.conf;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
        this.defaultCharEncoding = getConf().get(
                "parser.character.encoding.default", "UTF-8");
    }

    @Override
    public ParseResult getParse(Content content) {
        String urlString = content.getUrl();
        if (LOG.isInfoEnabled()) {
            LOG.info("VkParser parse: " + urlString);
        }

        try {
            URL url=new URL(content.getUrl());
            if (UrlCheck.isProfile(url)){
                return getProfileParse(url, content);
            }
            if (UrlCheck.isFriends(url)){
                return getFriendsParse(url, content);
            }
        } catch (MalformedURLException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            e.printStackTrace();

            return new ParseStatus(ParseStatus.FAILED, e.getMessage())
                    .getEmptyParseResult(content.getUrl(), getConf());
        }

        return new ParseStatus(ParseStatus.FAILED, "FAILED PARSE")
                .getEmptyParseResult(content.getUrl(), getConf());
    }

    private ParseResult getProfileParse(URL url, Content content) {
        Outlink[] newlinks;
        try {
            newlinks = new Outlink[]{
                    new Outlink("http://durov/friends", "durov friends")
            };
        } catch (MalformedURLException e) {
            newlinks = new Outlink[0];
            e.printStackTrace();
        }
        Metadata metadata = new Metadata();
        metadata.set(Metadata.ORIGINAL_CHAR_ENCODING, defaultCharEncoding);
        metadata.set(Metadata.CHAR_ENCODING_FOR_CONVERSION, defaultCharEncoding);

        Metadata contentMetadata = content.getMetadata();

        ParseData parseData = new ParseData(ParseStatus.STATUS_SUCCESS, "profile-page", newlinks, contentMetadata, metadata);
        ParseResult parseResult = ParseResult.createParseResult(content.getUrl(),
                new ParseImpl("ParseText", parseData));
        return parseResult;
    }

    private ParseResult getFriendsParse(URL url, Content content) {
        Outlink[] newlinks;
        try {
            newlinks = new Outlink[]{
                    new Outlink("http://polia", "polia"),
                    new Outlink("http://mabrouk", "mabrouk"),
                    new Outlink("http://id179155845", "id179155845")
            };
        } catch (MalformedURLException e) {
            newlinks = new Outlink[0];
            e.printStackTrace();
        }
        Metadata metadata = new Metadata();
        metadata.set(Metadata.ORIGINAL_CHAR_ENCODING, defaultCharEncoding);
        metadata.set(Metadata.CHAR_ENCODING_FOR_CONVERSION, defaultCharEncoding);

        Metadata contentMetadata = content.getMetadata();

        ParseData parseData = new ParseData(ParseStatus.STATUS_SUCCESS, "friends-page", newlinks, contentMetadata, metadata);
        ParseResult parseResult = ParseResult.createParseResult(content.getUrl(),
                new ParseImpl("ParseText", parseData));
        return parseResult;
    }
}
