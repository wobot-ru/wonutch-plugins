package org.apache.nutch.parse.vk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.*;
import org.apache.nutch.protocol.Content;

import java.net.MalformedURLException;

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
        if (LOG.isInfoEnabled()) {
            LOG.info("VkParser parse: " + content.getUrl());
        }

        Outlink[] newlinks;
        try {
            newlinks = new Outlink[]{
                    new Outlink("http://durov/pages", "durov-post-groups"),
                    new Outlink("http://durov/page/1000X00000000", "durov-post-groups"),
                    new Outlink("http://durov/index", "durov-post-groups"),
                    new Outlink("http://durov/i", "durov-index"),
                    new Outlink("http://durov/i/1000X00000000", "durov-index"),
                    new Outlink("http://durov/i/1000X00000001", "durov-index"),
                    new Outlink("http://durov/i/1000X00000002", "durov-index"),
                    new Outlink("http://durov/i/1000X00000003", "durov-index"),
                    new Outlink("http://durov/i/type/1000X00000004", "durov-index by type"),
                    new Outlink("http://durov/i/type/1000X00000005", "durov-index"),

                    new Outlink("http://durov/post-groups", "durov-post-groups"),
                    new Outlink("http://durov/post/by1000/top10", "durov-post-groups"),
                    new Outlink("http://durov/post/x1000/top10", "durov-post-groups"),
                    new Outlink("http://durov/post/1000*7i", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000x7", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000x00007", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000-00007", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00007", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000/00007", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/x1000-00007", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00007", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00008", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00009", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00010", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00011", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00012", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00013", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00014", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00015", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00016", "durov-post-groups"),
                    new Outlink("http://durov/post-groups/1000X00017", "durov-post-groups"),
                    new Outlink("http://durov/p-groups/1000X00017", "durov-post-groups"),
                    new Outlink("http://durov/p-groups/1000X00018", "durov-post-groups"),
                    new Outlink("http://durov/p-groups/1000X00019", "durov-post-groups"),
                    new Outlink("http://durov/p-groups/1000X00020", "durov-post-groups"),
           /*       new Outlink("http://durov/p-groups/1000X00021", "durov-post-groups"),
                    new Outlink("http://durov/g-p/1000X00021", "durov-post-groups"),
                    new Outlink("http://durov/g-p/1000X00022", "durov-post-groups"),
                    new Outlink("http://durov/g-p/1000X00023", "durov-post-groups"),
                    new Outlink("http://durov/g-p/1000X00024", "durov-post-groups"),
                    new Outlink("http://durov/g-p/1000X00025", "durov-post-groups"),
                    new Outlink("http://durov/group-page/1000X00025", "durov-post-groups"),
                    new Outlink("http://durov/group-page/1000X00026", "durov-post-groups"),
                    new Outlink("http://durov/group-page/1000X00028", "durov-post-groups"),
                    new Outlink("http://durov/group-page/1000X00029", "durov-post-groups"),
                    new Outlink("http://durov/group-p/1000X00030", "durov-post-groups"),
                    new Outlink("http://durov/group-p/1000X00031", "durov-post-groups"),
                    new Outlink("http://durov/group-p/1000X00032", "durov-post-groups"),
                    new Outlink("http://durov/group-p/1000X00033", "durov-post-groups"),
                    new Outlink("http://durov/page/1000X00033", "durov-post-groups"),
                    new Outlink("http://durov/page/1000X00035", "durov-post-groups"),
                    new Outlink("http://durov/page/1000X00036", "durov-post-groups"),
                    new Outlink("http://durov/g/1000X00021", "durov-post-groups"),*/
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
        contentMetadata.add("name","pavel");
        contentMetadata.add("name2","pavel2");
        metadata.add("name","pavel");
        metadata.add("name2","pavel2");

        ParseData parseData = new ParseData(ParseStatus.STATUS_SUCCESS, "ParseTitle", newlinks, contentMetadata, metadata);
        ParseResult parseResult = ParseResult.createParseResult(content.getUrl(),
                new ParseImpl("ParseText", parseData));
        return parseResult;
    }
}
