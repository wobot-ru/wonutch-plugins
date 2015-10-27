package org.apache.nutch.protocol.vk;

import crawlercommons.robots.BaseRobotRules;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.protocol.Protocol;
import org.apache.nutch.protocol.ProtocolOutput;
import org.apache.nutch.protocol.ProtocolStatus;
import org.apache.nutch.protocol.RobotRulesParser;

import java.net.URL;

/**
 * Created by Kviz on 10/27/2015.
 */
public class Vk implements Protocol {
    private static final Log LOG = LogFactory.getLog(Vk.class
            .getName());
    private Configuration conf;

    @Override
    public ProtocolOutput getProtocolOutput(Text url, CrawlDatum datum) {
        String urlString = url.toString();

        if (LOG.isInfoEnabled()) {
            LOG.info("Vkjs start");
        }
        VkResponse response;
        try {
            URL u = new URL(urlString);
            response = new VkResponse(u, datum, this, getConf());
            return new ProtocolOutput(response.toContent());
        } catch (Exception e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            e.printStackTrace();
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
}
