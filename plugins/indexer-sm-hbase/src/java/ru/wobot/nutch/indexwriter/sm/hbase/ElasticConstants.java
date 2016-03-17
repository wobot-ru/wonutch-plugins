/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.wobot.nutch.indexwriter.sm.hbase;

public interface ElasticConstants {
    String ELASTIC_PREFIX = "elastic.";

    String HOST = ELASTIC_PREFIX + "host";
    String PORT = ELASTIC_PREFIX + "port";
    String CLUSTER = ELASTIC_PREFIX + "cluster";
    String INDEX = ELASTIC_PREFIX + "index";
    String MAX_BULK_DOCS = ELASTIC_PREFIX + "max.bulk.docs";
    String MAX_BULK_LENGTH = ELASTIC_PREFIX + "max.bulk.size";
}