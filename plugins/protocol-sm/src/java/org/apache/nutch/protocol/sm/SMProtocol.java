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
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.auth.Proxy;
import ru.wobot.sm.core.domain.SMContent;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.fetch.FbFetcher;
import ru.wobot.sm.fetch.VkFetcher;
import ru.wobot.uri.UriTranslator;
import ru.wobot.uri.impl.ParsedUri;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SMProtocol implements Protocol {
    private static final Log LOG = LogFactory.getLog(SMProtocol.class.getName());
    private Configuration conf;
    private UriTranslator translator;

    @Override
    public ProtocolOutput getProtocolOutput(Text url, CrawlDatum datum) {
        String urlString = url.toString();
        if (LOG.isInfoEnabled()) {
            LOG.info("Start fetching: " + urlString);
        }
        try {
            FetchResponse fetchResponse = translator.translate(ParsedUri.parse(urlString));
            return new ProtocolOutput(convertToContent(new SMContent(url.toString(), fetchResponse.getData().getBytes(StandardCharsets.UTF_8), fetchResponse.getMetadata())));
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
        try {
            translator = new UriTranslator(new VkFetcher(), createFbFetcher());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not instantiate translator.", e);
        }
    }

    private FbFetcher createFbFetcher() {
        CredentialRepository repository = Proxy.INSTANCE;
        repository.setConf(getConf());
        return new FbFetcher(repository);
    }

    private Content convertToContent(SMContent response) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Finish fetching: " + response.getUrl() + " [fetchTime=" + response.getMetadata().get(ContentMetaConstants.FETCH_TIME) + "]");
        }

        Metadata metadata = new Metadata();
        Properties p = new Properties();
        p.putAll(response.getMetadata());
        metadata.setAll(p);
        metadata.add("nutch.fetch.time", String.valueOf(response.getMetadata().get(ContentMetaConstants.FETCH_TIME)));
        return new Content(response.getUrl(), response.getUrl(), response.getData(), response.getMetadata().get
                (ContentMetaConstants.MIME_TYPE).toString(), metadata, this.conf);
    }
}
