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
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;

public class HttpWebClient {

    private static final Logger LOG = LoggerFactory.getLogger("org.apache.nutch.protocol");

    public static ThreadLocal<WebDriver> threadWebDriver = new ThreadLocal<WebDriver>() {
        @Override
        protected WebDriver initialValue() {
            /*FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("permissions.default.stylesheet", 2);
            profile.setPreference("permissions.default.image", 2);
            profile.setPreference("dom.ipc.plugins.enabled.libflashplayer.so", "false");
            WebDriver driver = new FirefoxDriver(profile);
            return driver;*/
            System.setProperty("webdriver.chrome.driver", "src/testresources/chromedriver.exe");
            return new ChromeDriver();
        }
    };

    public static String getHtmlPage(String url, Configuration conf) {
        WebDriver driver = threadWebDriver.get();
        try {
            driver.get(url);

            Collection<String> cookies = new ArrayList<>();
            for (Cookie loadedCookie : driver.manage().getCookies()) {
                cookies.add(String.format("%s,%s", loadedCookie.getName(), loadedCookie.getValue()));
            }
            try {
                Files.write(FileSystems.getDefault().getPath("src/testresources/cookies.txt"), cookies, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                for (String s : Files.readAllLines(FileSystems.getDefault().getPath("src/testresources/cookies.txt"), StandardCharsets.UTF_8)) {
                    String[] c = s.split(",");
                    Cookie cookie = new Cookie(c[0], c[1]);
                    driver.manage().addCookie(cookie);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            WebElement element = driver.findElement(By.cssSelector("span._50f8._50f4"));
            //<span class="_50f8 _50f4"><a class="_39g5" href="https://www.facebook.com/profile.php?id=100004451677809&amp;sk=friends">39</a></span>

            //String innerHtml = driver.findElement(By.tagName("body")).getAttribute("innerHTML");
            return element.getText(); //innerHtml;
        } finally {
            if (driver != null) try {
                driver.quit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String getHtmlPage(String url) {
        return getHtmlPage(url, null);
    }
}