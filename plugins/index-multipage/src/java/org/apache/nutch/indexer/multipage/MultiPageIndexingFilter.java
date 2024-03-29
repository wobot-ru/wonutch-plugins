package org.apache.nutch.indexer.multipage;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.multipage.MultiElasticConstants;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPageIndexingFilter implements IndexingFilter {
    private static final Logger LOG = LoggerFactory.getLogger(MultiPageIndexingFilter.class.getName());

    private Configuration conf;

    @Override
    public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum crawlDatum, Inlinks inlinks) throws IndexingException {
        if (parse == null || parse.getData() == null || parse.getData().getParseMeta() == null) {
            return doc;
        }
        LOG.info("MultiPageIndexingFilter: filter(\"" + url.toString() + "\")");

        Metadata contentMeta = parse.getData().getContentMeta();
        if ("true".equals(contentMeta.get(MultiElasticConstants.MULTI_PAGE))) {
            LOG.info(url.toString() + " is MULTI_PAGE");
            doc.getDocumentMeta().set(MultiElasticConstants.MULTI_PAGE, contentMeta.get(MultiElasticConstants.MULTI_PAGE));
        }

        if (doc.getFieldValue("content") == null)
            doc.add("content", StringUtil.cleanField(parse.getText()));

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
}