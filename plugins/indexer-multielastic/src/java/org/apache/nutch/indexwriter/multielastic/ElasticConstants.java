package org.apache.nutch.indexwriter.multielastic;

public interface ElasticConstants {
    String ELASTIC_PREFIX = "elastic.";

    String HOST = ELASTIC_PREFIX + "host";
    String PORT = ELASTIC_PREFIX + "port";
    String CLUSTER = ELASTIC_PREFIX + "cluster";
    String INDEX = ELASTIC_PREFIX + "index";
    String MAX_BULK_DOCS = ELASTIC_PREFIX + "max.bulk.docs";
    String MAX_BULK_LENGTH = ELASTIC_PREFIX + "max.bulk.size";
}