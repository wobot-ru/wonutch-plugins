package org.apache.nutch.parse.sm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.multipage.MultiElasticConstants;
import org.apache.nutch.parse.Parser;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.protocol.Content;
import ru.wobot.parsers.Vk;
import ru.wobot.sm.core.Parsable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SMParser implements Parser {
    private static final Log LOG = LogFactory.getLog(SMParser.class.getName());
    private Configuration conf;
    private Parsable parser;

    public Configuration getConf() {
        return this.conf;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
        if (parser == null) {
            parser = new Vk();
        }
    }

    @Override
    public org.apache.nutch.parse.ParseResult getParse(Content content) {
        String urlString = content.getUrl();
        if (LOG.isInfoEnabled()) {
            LOG.info("Start parse: " + urlString);
        }

        try {
            //todo: Should be implemented the parser for other social media.
            ru.wobot.sm.core.dto.ParseResult parseResult = parser.parse(new URL(urlString), new String(content.getContent(), StandardCharsets.UTF_8));
            return convert(parseResult, content.getMetadata(), new Metadata());

        } catch (MalformedURLException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            return new ParseStatus(ParseStatus.FAILED, e.getMessage())
                    .getEmptyParseResult(content.getUrl(), getConf());
        }
    }

    protected ParseResult convert(ru.wobot.sm.core.dto.ParseResult vk, Metadata contentMetadata, Metadata parseMetadata) throws MalformedURLException {
        if (vk.isMultiPage) {
            parseMetadata.add(MultiElasticConstants.MULTI_DOC, "true");
        }

        Outlink[] outlinks = new Outlink[vk.links.size()];
        int index = 0;
        for (Map.Entry<String, String> mapEntry : vk.links.entrySet()) {
            outlinks[index] = new Outlink(mapEntry.getKey(), mapEntry.getValue());
            index++;
        }

        ParseData parseData = new ParseData(ParseStatus.STATUS_SUCCESS, vk.title, outlinks, contentMetadata, parseMetadata);
        ParseResult parseResult = ParseResult.createParseResult(vk.url, new ParseImpl(vk.content, parseData));

        if (LOG.isTraceEnabled()) {
            LOG.trace("Finish parse links [" + vk.url + "] : [" + vk.title + "] : [link.size=" + vk.links.size() + "]");
            LOG.trace("Finish parse content [" + vk.url + "] : [" + vk.title + "] : [content='" + vk.content + "']");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Finish parse: " + vk.url);
        }
        return parseResult;
    }
}
