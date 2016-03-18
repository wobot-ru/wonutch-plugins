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
import ru.wobot.sm.core.mapping.ProfileProperties;
import ru.wobot.sm.core.mapping.Sources;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.net.URI;
import java.net.URISyntaxException;

public class SMIndexingFilter implements IndexingFilter {
    private static final Log LOG = LogFactory.getLog(SMIndexingFilter.class.getName());

    private Configuration conf;

    @Override
    public NutchDocument filter(NutchDocument doc, Parse parse, Text textUrl, CrawlDatum crawlDatum, Inlinks inlinks) throws IndexingException {
        URI url;
        try {
            url = new URI(textUrl.toString());
        } catch (URISyntaxException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            return null;
        }

        if (url == null || parse == null || parse.getData() == null || parse.getData().getParseMeta() == null)
            return doc;

        LOG.info("SMIndexingFilter: filter(\"" + textUrl.toString() + "\")");

        Metadata contentMeta = parse.getData().getContentMeta();
        // skip system's API calls from an index
        if (contentMeta.get(ContentMetaConstants.SKIP_FROM_ELASTIC_INDEX) != null
                && contentMeta.get(ContentMetaConstants.SKIP_FROM_ELASTIC_INDEX).equals("1"))
            return null;

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

        String id = (String) doc.getFieldValue("id");
        if (id.contains("https://www.facebook.com/")) {
            doc.removeField("id");
            String profileId = parse.getData().getParseMeta().get(ProfileProperties.SM_PROFILE_ID);
            doc.add("id", Sources.FACEBOOK + "://" + profileId);
        }
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