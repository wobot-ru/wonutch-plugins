package ru.wobot.nutch.indexer.sm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.util.StringUtil;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.url.UrlCheck;

import java.net.MalformedURLException;
import java.net.URL;

public class SMIndexingFilter implements IndexingFilter {
    private static final Log LOG = LogFactory.getLog(SMIndexingFilter.class.getName());

    private Configuration conf;

    @Override
    public NutchDocument filter(NutchDocument doc, Parse parse, Text textUrl, CrawlDatum crawlDatum, Inlinks inlinks) throws IndexingException {
        URL url;
        try {
            url = new URL(textUrl.toString());
        } catch (MalformedURLException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            return null;
        }

        // skip system's API calls from an index
        if (url == null || UrlCheck.isFriends(url) || UrlCheck.isPostsIndex(url) || UrlCheck.isPostsIndexPage(url))
            return null;

        if (parse == null || parse.getData() == null || parse.getData().getParseMeta() == null)
            return doc;

        LOG.info("SMIndexingFilter: filter(\"" + textUrl.toString() + "\")");

        Metadata contentMeta = parse.getData().getContentMeta();
        Metadata documentMeta = doc.getDocumentMeta();
        copyMetaKey(ContentMetaConstants.TYPE, contentMeta, documentMeta);
        copyMetaKey(ContentMetaConstants.PARENT, contentMeta, documentMeta);
        copyMetaKey(ContentMetaConstants.MULTIPLE_PARSE_RESULT, contentMeta, documentMeta);

        Metadata parseMeta = parse.getData().getParseMeta();
        for (String tag : parseMeta.names()) {
            doc.add(tag, parseMeta.get(tag));
        }

        if ("true".equals(contentMeta.get(ContentMetaConstants.MULTIPLE_PARSE_RESULT))
                && doc.getFieldValue("content") == null
                && !StringUtil.isEmpty(parse.getText())) {
            doc.add("content", StringUtil.cleanField(parse.getText()));
        }

        if (doc.getFieldValue("score") == null)
            doc.add("score", crawlDatum.getScore());

        return doc;
    }

    @Override
    public Configuration getConf() {
        return conf;
    }

    @Override
    public void setConf(Configuration configuration) {
        this.conf = configuration;
    }

    private void copyMetaKey(String key, Metadata from, Metadata to) {
        String value = from.get(key);
        if (value != null) {
            to.set(key, value);
        }
    }
}