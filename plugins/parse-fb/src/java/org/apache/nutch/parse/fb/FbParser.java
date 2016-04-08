package org.apache.nutch.parse.fb;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.protocol.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FbParser implements org.apache.nutch.parse.Parser {
    private static final Logger LOG = LoggerFactory.getLogger(FbParser.class.getName());
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

        return new ProfileParser(content).getParseResult();
    }
}
