/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.protocol.vk;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.metadata.SpellCheckedMetadata;
import org.apache.nutch.net.protocols.Response;
import org.apache.nutch.protocol.ProtocolException;
import org.apache.nutch.protocol.http.api.HttpBase;
import org.apache.nutch.protocol.http.api.HttpException;
import org.springframework.social.vkontakte.api.impl.VKontakteTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class HttpResponse implements Response {

    private Configuration conf;
    private HttpBase http;
    private URL url;
    private String orig;
    private byte[] content;
    private int code;
    private Metadata headers = new SpellCheckedMetadata();

    protected enum Scheme {
        HTTP, HTTPS,
    }

    /**
     * Default public constructor.
     *
     * @param http
     * @param url
     * @param datum
     * @throws ProtocolException
     * @throws IOException
     */
    public HttpResponse(HttpBase http, URL url, CrawlDatum datum)
            throws ProtocolException {
        this.http = http;
        this.url = url;
        this.orig = url.toString();

        Scheme scheme = null;

        if ("http".equals(url.getProtocol())) {
            scheme = Scheme.HTTP;
        } else if ("https".equals(url.getProtocol())) {
            scheme = Scheme.HTTPS;
        } else {
            throw new HttpException("Unknown scheme (not http/https) for url:" + url);
        }

        if (Http.LOG.isTraceEnabled()) {
            Http.LOG.trace("fetching " + url);
        }

        code = 200;
        try {
            if (this.orig.contains("robots.txt")) {
                content = "User-agent: * \nAllow:/".getBytes("utf-8");
            } else {
                VKService vk = new VKService();
                String request = vk.request(url);
                content = request.getBytes("utf-8");
                String json = vk.requestJson(url);
                if (json != null) {
                    headers.add("json", json);
                }
                headers.add("display_type", vk.getDisplayType(url));
            }
        } catch (MalformedURLException e) {
            code = 400;
            content = new byte[]{};
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            code = 400;
            content = new byte[]{};
            e.printStackTrace();
        }

    /*
      InputStream in = url.openStream();
      try {
        String page = IOUtils.toString(in);
        content = page.getBytes("UTF-8");
      } finally {
        IOUtils.closeQuietly(in);
      }
*/

        //datum.getMetaData().put(new Text("trololo"), new Text("kvizkvizkviz"));
        headers.add("nutch.fetch.time", Long.toString(System.currentTimeMillis()));

        if (Http.LOG.isTraceEnabled()) {
            Http.LOG.trace("fetched " + content.length + " bytes from " + url);
        }
    }

    public URL getUrl() {
        return url;
    }

    public int getCode() {
        return code;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Metadata getHeaders() {
        return headers;
    }

    public byte[] getContent() {
        return content;
    }
}
