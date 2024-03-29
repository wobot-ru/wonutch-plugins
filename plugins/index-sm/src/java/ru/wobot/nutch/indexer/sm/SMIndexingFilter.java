package ru.wobot.nutch.indexer.sm;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wobot.sm.core.mapping.Sources;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SMIndexingFilter implements IndexingFilter {
    private static final Logger LOG = LoggerFactory.getLogger(SMIndexingFilter.class.getName());

    private Configuration conf;

    @Override
    public NutchDocument filter(NutchDocument doc, Parse parse, Text textUrl, CrawlDatum crawlDatum, Inlinks inlinks) throws IndexingException {
        if (parse == null || parse.getData() == null || parse.getData().getParseMeta() == null)
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        doc.add("fetch_time", dateFormat.format(new Date(crawlDatum.getFetchTime())));

        String id = (String) doc.getFieldValue("id");
        if (id.contains("as_id=")) {
            doc.removeField("id");
            String profileId = parse.getData().getParseMeta().get("app_scoped_user_id");
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