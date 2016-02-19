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
import ru.wobot.sm.core.fetch.AccessDenied;
import ru.wobot.sm.core.fetch.ApiResponse;
import ru.wobot.sm.core.fetch.Redirect;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.fetch.FbFetcher;
import ru.wobot.sm.fetch.VkFetcher;
import ru.wobot.uri.UriTranslator;
import ru.wobot.uri.impl.ParsedUri;

import java.nio.charset.StandardCharsets;
import java.util.Map;
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
            ApiResponse response = translator.translate(ParsedUri.parse(urlString));
            Content content = convertToContent(response, urlString);
            if (response instanceof Redirect) {
                return new ProtocolOutput(content, new ProtocolStatus(ProtocolStatus.MOVED, response.getMessage()));
            }
            if (response instanceof AccessDenied) {
                return new ProtocolOutput(content, new ProtocolStatus(ProtocolStatus.ACCESS_DENIED, response.getMessage()));
            }
            return new ProtocolOutput(content);
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
        translator = createUriTranslator(createVkFetcher(), createFbFetcher());
    }

    protected UriTranslator createUriTranslator(Object... objects) {
        try {
            return new UriTranslator(objects);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not instantiate translator.", e);
        }
    }

    protected FbFetcher createFbFetcher() {
        CredentialRepository repository = new Proxy("fb");
        repository.setConf(getConf());
        return new FbFetcher(repository);
    }

    protected VkFetcher createVkFetcher() {
        CredentialRepository repository = new Proxy("vk");
        repository.setConf(getConf());
        return new VkFetcher(repository);
    }

    private Content convertToContent(ApiResponse response, String uri) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Finish fetching: " + uri + " [fetchTime=" + response.getMetadata().get(ContentMetaConstants.FETCH_TIME) + "]");
        }

        Metadata metadata = new Metadata();
        Properties p = new Properties();
        Map<String, Object> responseMetadata = response.getMetadata();
        p.putAll(responseMetadata);
        metadata.setAll(p);
        metadata.add("nutch.fetch.time", String.valueOf(responseMetadata.get(ContentMetaConstants.FETCH_TIME)));
        return new Content(uri, uri, response.getData().getBytes(StandardCharsets.UTF_8),
                String.valueOf(responseMetadata.get(ContentMetaConstants.MIME_TYPE)), metadata, this.conf);
    }
}
