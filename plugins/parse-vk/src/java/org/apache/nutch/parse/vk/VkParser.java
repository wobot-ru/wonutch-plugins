package org.apache.nutch.parse.vk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.*;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.multielastic.MultiElasticConstants;

import java.net.MalformedURLException;
import java.util.Map;

public class VkParser implements Parser {

    private static final Log LOG = LogFactory.getLog(VkParser.class.getName());
    private Configuration conf;

    public Configuration getConf() {
        return this.conf;
    }

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
            ru.wobot.vk.ParseResult parseResult = ru.wobot.vk.Parser.parse(urlString, content.getContent());
            return convert(parseResult, content.getMetadata(), new Metadata());

        } catch (MalformedURLException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            e.printStackTrace();

            return new ParseStatus(ParseStatus.FAILED, e.getMessage())
                    .getEmptyParseResult(content.getUrl(), getConf());
        }
    }

    private ParseResult convert(ru.wobot.vk.ParseResult vk, Metadata contentMetadata, Metadata parseMetadata) throws MalformedURLException {
        if (vk.isMultiPage){
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
            LOG.trace("Finish parse links [" + vk.url + "] : [" + vk.title + "] : [link.size="+ vk.links.size()+"]");
            LOG.trace("Finish parse content [" + vk.url + "] : [" + vk.title + "] : [content='" + vk.content + "']");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Finish parse: " + vk.url);
        }
        return parseResult;
    }
}
