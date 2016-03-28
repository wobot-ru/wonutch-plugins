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
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.auth.CredentialRepository;
import ru.wobot.sm.core.auth.Proxy;
import ru.wobot.sm.core.fetch.AccessDenied;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.Redirect;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.fetch.FbFetcher;
import ru.wobot.sm.fetch.VkFetcher;
import ru.wobot.uri.UriTranslator;
import ru.wobot.uri.impl.ParsedUri;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SMProtocol implements Protocol {
    private static final Log LOG = LogFactory.getLog(SMProtocol.class.getName());
    private static final CredentialRepository fbCredentialRepository = new Proxy("fb");
    private static final CookieRepository fbCookieRepository = new CookieRepository();

    private Configuration conf;
    private UriTranslator translator;

    @Override
    public ProtocolOutput getProtocolOutput(Text url, CrawlDatum datum) {
        String urlString = url.toString();
        if (LOG.isInfoEnabled()) {
            LOG.info("Start fetching: " + urlString);
        }
        try {
            FetchResponse response = translator.translate(ParsedUri.parse(urlString));
            Content content = convertToContent(response, urlString);
            if (response instanceof Redirect) {
                return new ProtocolOutput(content, new ProtocolStatus(ProtocolStatus.MOVED, new URL(new URL(urlString), (String) response.getMessage())));
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

    private UriTranslator createUriTranslator(Object... objects) {
        try {
            return new UriTranslator(objects);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not instantiate translator.", e);
        }
    }

    private FbFetcher createFbFetcher() {
        fbCredentialRepository.setConf(getConf());
        fbCookieRepository.setConf(getConf());
        return new FbFetcher(fbCredentialRepository, fbCookieRepository);
    }

    private VkFetcher createVkFetcher() {
        CredentialRepository repository = new Proxy("vk");
        repository.setConf(getConf());
        return new VkFetcher(repository);
    }

    private Content convertToContent(FetchResponse response, String uri) {
        Map<String, Object> responseMetadata = response.getMetadata();
        if (LOG.isInfoEnabled()) {
            LOG.info("Finish fetching: " + uri + " [fetchTime=" + responseMetadata.get(ContentMetaConstants.FETCH_TIME) + "]");
        }

        Metadata metadata = new Metadata();
        for (Map.Entry<String, Object> entry : responseMetadata.entrySet()) {
            metadata.add(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return new Content(uri, uri, response.getData().getBytes(StandardCharsets.UTF_8),
                String.valueOf(responseMetadata.get(ContentMetaConstants.MIME_TYPE)), metadata, this.conf);
    }
}
