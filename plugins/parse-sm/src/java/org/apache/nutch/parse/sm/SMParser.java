package org.apache.nutch.parse.sm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.multipage.MultiElasticConstants;
import org.apache.nutch.parse.*;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import ru.wobot.sm.parse.Vk;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SMParser implements org.apache.nutch.parse.Parser {
    private static final Log LOG = LogFactory.getLog(SMParser.class.getName());
    private Configuration conf;
    private ru.wobot.sm.core.parse.Parser parser;

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
            ru.wobot.sm.core.parse.ParseResult parseResult = parser.parse(new URL(urlString), new String(content.getContent(), StandardCharsets.UTF_8));
            return convert(parseResult, content.getMetadata(), new Metadata());

        } catch (MalformedURLException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            return new ParseStatus(ParseStatus.FAILED, e.getMessage())
                    .getEmptyParseResult(content.getUrl(), getConf());
        }
    }

    protected ParseResult convert(ru.wobot.sm.core.parse.ParseResult parsedDto, Metadata contentMetadata, Metadata parseMetadata) throws MalformedURLException {
        Outlink[] outlinks = new Outlink[parsedDto.getLinks().size()];
        int index = 0;
        for (Map.Entry<String, String> mapEntry : parsedDto.getLinks().entrySet()) {
            outlinks[index] = new Outlink(mapEntry.getKey(), mapEntry.getValue());
            index++;
        }

        for (Map.Entry<String, Object> entry : parsedDto.getParseMeta().entrySet()) {
            parseMetadata.add(entry.getKey(), String.valueOf(entry.getValue()));
        }
        for (Map.Entry<String, Object> entry : parsedDto.getContentMeta().entrySet()) {
            contentMetadata.add(entry.getKey(), String.valueOf(entry.getValue()));
        }

        ParseData parseData = new ParseData(ParseStatus.STATUS_SUCCESS, parsedDto.getTitle(), outlinks, contentMetadata, parseMetadata);
        ParseResult parseResult = ParseResult.createParseResult(parsedDto.getUrl(), new ParseImpl(parsedDto.getContent(), parseData));

        if (LOG.isTraceEnabled()) {
            LOG.trace("Finish parse links [" + parsedDto.getUrl() + "] : [" + parsedDto.getTitle() + "] : [link.size=" + parsedDto.getLinks().size() + "]");
            LOG.trace("Finish parse content [" + parsedDto.getUrl() + "] : [" + parsedDto.getTitle() + "] : [content='" + parsedDto.getContent() + "']");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Finish parse: " + parsedDto.getUrl());
        }
        return parseResult;
    }
}
