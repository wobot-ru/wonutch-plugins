/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.protocol.selenium;

import crawlercommons.robots.BaseRobotRules;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.protocol.Protocol;
import org.apache.nutch.protocol.ProtocolOutput;
import org.apache.nutch.protocol.ProtocolStatus;
import org.apache.nutch.protocol.RobotRulesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wobot.sm.core.auth.CookieRepository;
import ru.wobot.sm.core.fetch.AccessDenied;
import ru.wobot.sm.core.fetch.FetchResponse;
import ru.wobot.sm.core.fetch.Redirect;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.fetch.HttpWebFetcher;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Https implements Protocol {
    public static final Logger LOG = LoggerFactory.getLogger(Https.class);
    private HttpWebFetcher webClient;
    private Configuration conf;

    @Override
    public Configuration getConf() {
        return this.conf;
    }

    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
        CookieRepository cookieRepository = new CookieRepository(conf);
        this.webClient = new HttpWebFetcher(conf, cookieRepository);
    }

    @Override
    public ProtocolOutput getProtocolOutput(Text url, CrawlDatum datum) {
        String urlString = url.toString();
        FetchResponse fetchResponse = new AccessDenied(null, null);//webClient.getHtmlPage(urlString);
        //TODO: Code duplication with SMProtocol
        Metadata metadata = new Metadata();
        metadata.add("nutch.fetch.time", String.valueOf(fetchResponse.getMetadata().get(ContentMetaConstants.FETCH_TIME)));
        for (Map.Entry<String, Object> entry : fetchResponse.getMetadata().entrySet()) {
            metadata.add(entry.getKey(), String.valueOf(entry.getValue()));
        }
        Content c = new Content(urlString, urlString, fetchResponse.getData().getBytes(StandardCharsets.UTF_8),
                String.valueOf(fetchResponse.getMetadata().get(ContentMetaConstants.MIME_TYPE)), metadata, this.conf);

        if (LOG.isInfoEnabled()) {
            LOG.info("Finish fetching: " + urlString + " [fetchTime=" + fetchResponse.getMetadata().get(ContentMetaConstants.FETCH_TIME) + "]");
        }

        if (fetchResponse instanceof Redirect)
            return new ProtocolOutput(c, new ProtocolStatus(ProtocolStatus.MOVED, fetchResponse.getMessage()));
        else
            return new ProtocolOutput(c);
    }

    @Override
    public BaseRobotRules getRobotRules(Text url, CrawlDatum datum) {
        return RobotRulesParser.EMPTY_RULES;
    }
}
