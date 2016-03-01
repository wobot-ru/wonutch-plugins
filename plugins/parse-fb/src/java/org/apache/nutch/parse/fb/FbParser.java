package org.apache.nutch.parse.fb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.protocol.Content;
import ru.wobot.sm.core.Sources;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class FbParser implements org.apache.nutch.parse.Parser {
    private static final Log LOG = LogFactory.getLog(FbParser.class.getName());
    private Configuration conf;

    @Override
    public Configuration getConf() {
        return this.conf;
    }

    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public org.apache.nutch.parse.ParseResult getParse(Content content) {
        String urlString = content.getUrl();
        if (LOG.isInfoEnabled()) {
            LOG.info("Start parse: " + urlString);
        }

        try {
            URI url = new URI(urlString);
            final Metadata metadata = content.getMetadata();
            final String apiType = metadata.get(ContentMetaConstants.API_TYPE);
            final String apiVersion = metadata.get(ContentMetaConstants.API_VER);
            final String data = new String(content.getContent(),StandardCharsets.UTF_8);
            ru.wobot.sm.core.parse.ParseResult parseResult =
                    new ru.wobot.sm.core.parse.ParseResult(urlString, "", data, new HashMap<String, String>(), new HashMap<String, Object>(), new HashMap<String, Object>());
            return convert(parseResult, metadata, new Metadata());

        } catch (MalformedURLException | URISyntaxException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            return new ParseStatus(ParseStatus.FAILED, e.getMessage())
                    .getEmptyParseResult(content.getUrl(), getConf());
        }
    }

    private ParseResult convert(ru.wobot.sm.core.parse.ParseResult parsedDto, Metadata contentMetadata, Metadata
            parseMetadata) throws MalformedURLException {
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
