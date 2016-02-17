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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class HttpWebClient {
    public static final String PHANTOMJS_EXECUTABLE_FILE = "sm.phantomjs.file";
    public static final String COOKIES_FILE = "sm.cookies.file";
    private static final Logger LOG = LoggerFactory.getLogger(HttpWebClient.class);
    private static Configuration conf;
    private static ThreadLocal<WebDriver> threadWebDriver = new ThreadLocal<WebDriver>() {
        @Override
        protected WebDriver initialValue() {
            String phantomPath = conf.get(PHANTOMJS_EXECUTABLE_FILE);
            if (phantomPath != null && !phantomPath.isEmpty()) {
                Capabilities caps = DesiredCapabilities.phantomjs();
                ((DesiredCapabilities) caps).setCapability(
                        PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                        conf.getResource(phantomPath).getFile()
                );
                ((DesiredCapabilities) caps).setJavascriptEnabled(true);
                ((DesiredCapabilities) caps).setCapability("takesScreenshot", true);
                return new PhantomJSDriver(caps);
            } else
                throw new IllegalStateException("No PhantomJS binary path found in config.");
        }
    };

    public static String getHtmlPage(String url, Configuration conf) {
        HttpWebClient.conf = Objects.requireNonNull(conf);
        WebDriver driver = threadWebDriver.get();
        try {
            driver.get(url);
            Collection<String> cookies = getCookies();
            if (cookies == null || cookies.isEmpty())
                throw new IllegalStateException("No cookies found in cookies file. Can't authorize web driver.");
            for (String c : cookies) {
                String[] cs = c.split(";");
                String name, value, domain;
                name = value = domain = null;
                for (int i = 0; i < cs.length; i++) {
                    String part = cs[i];
                    if (i == 0) {
                        name = part.split("=")[0];
                        value = part.split("=")[1];
                    } else if (part.split("=")[0].trim().equals("domain")) {
                        domain = part.split("=")[1];
                    }
                }
                Cookie cookie = new Cookie.Builder(name, value).domain(domain).build();
                driver.manage().addCookie(cookie);
            }
            driver.get(url);

            return driver.findElement(By.tagName("html")).getAttribute("innerHTML");
        } finally {
            if (driver != null) try {
                driver.quit();
            } catch (Exception e) {
                LOG.error(StringUtils.stringifyException(e));
            }
        }
    }

    private static Collection<String> getCookies() {
        String cookiesPath = conf.get(COOKIES_FILE);
        if (cookiesPath == null || cookiesPath.isEmpty())
            throw new IllegalStateException("No cookies file found in config.");
        Collection<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(conf.getConfResourceAsReader(cookiesPath))) {
            String line;
            line = br.readLine();
            while (line != null && !line.isEmpty()) {
                result.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            LOG.error(StringUtils.stringifyException(e));
            return null;
        }
        return result;
    }

    /*Collection<String> cookies = new ArrayList<>();
            for (Cookie loadedCookie : driver.manage().getCookies()) {
                cookies.add(loadedCookie.toString());
            }
            try {
                Files.write(FileSystems.getDefault().getPath("src/testresources/cookies.txt"), cookies, StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
}