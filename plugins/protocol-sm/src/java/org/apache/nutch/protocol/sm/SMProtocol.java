package org.apache.nutch.protocol.sm;

import crawlercommons.robots.BaseRobotRules;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.protocol.Protocol;
import org.apache.nutch.protocol.ProtocolOutput;
import org.apache.nutch.protocol.ProtocolStatus;
import org.apache.nutch.protocol.RobotRulesParser;
import ru.wobot.sm.core.domain.SMContent;
import ru.wobot.sm.core.domain.service.DomainService;
import ru.wobot.sm.core.fetch.SMFetcher;
import ru.wobot.sm.core.meta.ContentMetaConstants;

import java.util.Properties;

public abstract class SMProtocol implements Protocol {
    private static final Log LOG = LogFactory.getLog(SMProtocol.class.getName());
    protected DomainService domainService;
    private Configuration conf;

    @Override
    public ProtocolOutput getProtocolOutput(Text url, CrawlDatum datum) {
        String urlString = url.toString();
        if (LOG.isInfoEnabled()) {
            LOG.info("Start fetching: " + urlString);
        }

        try {
            SMContent response = domainService.request(urlString);
            return new ProtocolOutput(convertToContent(response));
        } catch (Exception e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            return new ProtocolOutput(null, new ProtocolStatus(e));
        }
    }

    @Override
    public BaseRobotRules getRobotRules(Text url, CrawlDatum datum) {
        return RobotRulesParser.EMPTY_RULES;
    }

    @Override
    public Configuration getConf() {
        return this.conf;
    }

    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
        domainService = new DomainService(createSMService());
    }

    protected Content convertToContent(SMContent response) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Finish fetching: " + response.url + " [fetchTime=" + response.metadata.get
                    (ContentMetaConstants.FETCH_TIME) + "]");
        }

        Metadata metadata = new Metadata();
        Properties p = new Properties();
        p.putAll(response.metadata);
        metadata.setAll(p);
        metadata.add("nutch.fetch.time", response.metadata.get
                (ContentMetaConstants.FETCH_TIME));
        return new Content(response.url, response.url, response.data, SMContent.JSON_MIME_TYPE, metadata, this.conf);
    }

    protected abstract SMFetcher createSMService();
}
