package org.apache.nutch.protocol.vk;

import crawlercommons.robots.BaseRobotRules;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.protocol.*;
import ru.wobot.vk.CredentialRepository;
import ru.wobot.smm.core.Proxy;
import ru.wobot.vk.Response;
import ru.wobot.vk.Service;

public class Vk implements Protocol {
    private static final Log LOG = LogFactory.getLog(Vk.class.getName());
    private Configuration conf;
    private CredentialRepository proxy = Proxy.INSTANCE;

    @Override
    public ProtocolOutput getProtocolOutput(Text url, CrawlDatum datum) {
        String urlString = url.toString();

        if (LOG.isInfoEnabled()) {
            LOG.info("Start fetching: " + urlString);
        }
        try {
            proxy.setConf(conf);
            Service service = new Service(proxy);
            Response response = service.request(urlString);
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
    }

    private Content convertToContent(Response response) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Finish fetching: " + response.url + " [fetchTime=" + response.fetchTime + "]");
        }

        Metadata metadata = new Metadata();
        metadata.add("nutch.fetch.time", Long.toString(response.fetchTime));
        return new Content(response.url, response.url, response.data, response.mimeType, metadata, this.conf);
    }
}
