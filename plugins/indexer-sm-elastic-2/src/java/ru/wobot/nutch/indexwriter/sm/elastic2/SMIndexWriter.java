package ru.wobot.nutch.indexwriter.sm.elastic2;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.nutch.indexer.IndexWriter;
import org.apache.nutch.indexer.NutchDocument;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.fieldstats.FieldStats;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.meta.NutchDocumentMetaConstants;
import ru.wobot.sm.core.parse.ParseResult;


@SuppressWarnings("Duplicates")
public class SMIndexWriter implements IndexWriter {
    public static Logger LOG = LoggerFactory.getLogger(SMIndexWriter.class);

    private static final int DEFAULT_MAX_BULK_DOCS = 250;
    private static final int DEFAULT_MAX_BULK_LENGTH = 2500500;

    private Client client;
    private Node node;
    private String defaultIndex;

    private Configuration config;

    private BulkRequestBuilder bulk;
    private ListenableActionFuture<BulkResponse> execute;
    private int port = -1;
    private String host = null;
    private String clusterName = null;
    private int maxBulkDocs;
    private int maxBulkLength;
    private long indexedDocs = 0;
    private int bulkDocs = 0;
    private int bulkLength = 0;
    private boolean createNewBulk = false;

    @Override
    public void open(JobConf job, String name) throws IOException {
        clusterName = job.get(ElasticConstants.CLUSTER);

        host = job.get(ElasticConstants.HOST);
        port = job.getInt(ElasticConstants.PORT, 9300);

        Settings.Builder settingsBuilder = Settings.settingsBuilder();

        BufferedReader reader = new BufferedReader(
                job.getConfResourceAsReader("elasticsearch.conf"));
        String line;
        String parts[];

        while ((line = reader.readLine()) != null) {
            if (StringUtils.isNotBlank(line) && !line.startsWith("#")) {
                line.trim();
                parts = line.split("=");

                if (parts.length == 2) {
                    settingsBuilder.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        if (StringUtils.isNotBlank(clusterName))
            settingsBuilder.put("cluster.name", clusterName);

        // Set the cluster name and build the settings
        Settings settings = settingsBuilder.build();

        // Prefer TransportClient
        if (host != null && port > 1) {
            TransportAddress transportAddress = new InetSocketTransportAddress(InetAddress.getByName(host), port);
            TransportClient.Builder transportClientBuilder = TransportClient.builder().settings(settings);
            client = transportClientBuilder.build().addTransportAddresses(transportAddress);
        } else if (clusterName != null) {
            node = nodeBuilder().settings(settings).client(true).node();
            client = node.client();
        }

        bulk = client.prepareBulk();
        defaultIndex = job.get(ElasticConstants.INDEX, "nutch");
        maxBulkDocs = job.getInt(ElasticConstants.MAX_BULK_DOCS,
                DEFAULT_MAX_BULK_DOCS);
        maxBulkLength = job.getInt(ElasticConstants.MAX_BULK_LENGTH,
                DEFAULT_MAX_BULK_LENGTH);
    }

    @Override
    public void write(NutchDocument doc) throws IOException {
        final boolean isSingleDoc = !"true".equals(doc.getDocumentMeta().get(ContentMetaConstants.MULTIPLE_PARSE_RESULT));
        String id = (String) doc.getFieldValue("id");
        String type = doc.getDocumentMeta().get(ContentMetaConstants.TYPE);
        if (type == null)
            type = "doc";

        IndexRequestBuilder request;
        Map<String, Object> source;
        if (isSingleDoc) {
            source = new HashMap<>();
            request = client.prepareIndex(defaultIndex, type, id);
            // Loop through all fields of this doc
            for (String fieldName : doc.getFieldNames()) {
                if (doc.getField(fieldName).getValues().size() > 1) {
                    source.put(fieldName, doc.getFieldValue(fieldName));
                    // Loop through the values to keep track of the size of this document
                    for (Object value : doc.getField(fieldName).getValues()) {
                        bulkLength += value.toString().length();
                    }
                } else {
                    source.put(fieldName, doc.getFieldValue(fieldName));
                    bulkLength += doc.getFieldValue(fieldName).toString().length();
                }
            }

            request.setSource(source);
            String parent = doc.getDocumentMeta().get(ContentMetaConstants.PARENT);
            if (parent != null) {
                request.setParent(parent);
            }
            // Add this indexing request to a bulk request
            bulk.add(request);
            indexedDocs++;
            bulkDocs++;

            flushIfNecessary(id);
        } else {
            // for indexing documents more than one
            String content = (String) doc.getFieldValue("content");
            String segment = (String) doc.getFieldValue(NutchDocumentMetaConstants.SEGMENT);
            String boost = (String) doc.getFieldValue(NutchDocumentMetaConstants.BOOST);
            Float score = (Float) doc.getFieldValue("score");
            ParseResult[] parseResults = fromJson(content, ParseResult[].class);
            if (parseResults != null) {
                for (ParseResult parseResult : parseResults) {
                    id = parseResult.getUrl();
                    source = new HashMap<>();
                    source.put("segment", segment);
                    source.put("boost", Float.parseFloat(boost));
                    source.put("score", score);
                    for (Map.Entry<String, Object> p : parseResult.getParseMeta().entrySet()) {
                        source.put(p.getKey(), p.getValue());
                    }

                    for (Map.Entry<String, Object> field : source.entrySet()) {
                        bulkLength += field.getValue().toString().length();
                    }
                    String subType = (String) parseResult.getContentMeta().get(ContentMetaConstants.TYPE);
                    if (subType == null) {
                        subType = type;
                    }
                    request = client.prepareIndex(defaultIndex, subType, id);
                    request.setSource(source);
                    String parent = (String) parseResult.getContentMeta().get(ContentMetaConstants.PARENT);
                    if (parent != null) {
                        request.setParent(parent);
                    }
                    // Add this indexing request to a bulk request
                    bulk.add(request);
                    indexedDocs++;
                    bulkDocs++;

                    flushIfNecessary(id);
                }
            }
        }
    }

    private void flushIfNecessary(String id) throws IOException {
        if (bulkDocs >= maxBulkDocs || bulkLength >= maxBulkLength) {
            LOG.info("Processing bulk request [docs = " + bulkDocs + ", length = "
                    + bulkLength + ", total docs = " + indexedDocs
                    + ", last doc in bulk = '" + id + "']");
            // Flush the bulk of indexing requests
            createNewBulk = true;
            commit();
        }
    }

    private static <T> T fromJson(String json, Class<T> classOfT) {
        return new GsonBuilder()
                .create()
                .fromJson(json, classOfT);
    }

    @Override
    public void delete(String key) throws IOException {
        try {
            DeleteRequestBuilder builder = client.prepareDelete();
            builder.setIndex(defaultIndex);
            builder.setType("doc");
            builder.setId(key);
            builder.execute().actionGet();
        } catch (ElasticsearchException e) {
            throw makeIOException(e);
        }
    }

    public static IOException makeIOException(ElasticsearchException e) {
        final IOException ioe = new IOException();
        ioe.initCause(e);
        return ioe;
    }

    @Override
    public void update(NutchDocument doc) throws IOException {
        write(doc);
    }

    @Override
    public void commit() throws IOException {
        if (execute != null) {
            // wait for previous to finish
            long beforeWait = System.currentTimeMillis();
            BulkResponse actionGet = execute.actionGet();
            if (actionGet.hasFailures()) {
                for (BulkItemResponse item : actionGet) {
                    if (item.isFailed()) {
                        throw new RuntimeException("First failure in bulk: "
                                + item.getFailureMessage());
                    }
                }
            }
            long msWaited = System.currentTimeMillis() - beforeWait;
            LOG.info("Previous took in ms " + actionGet.getTookInMillis()
                    + ", including wait " + msWaited);
            execute = null;
        }
        if (bulk != null) {
            if (bulkDocs > 0) {
                // start a flush, note that this is an asynchronous call
                execute = bulk.execute();
            }
            bulk = null;
        }
        if (createNewBulk) {
            // Prepare a new bulk request
            bulk = client.prepareBulk();
            bulkDocs = 0;
            bulkLength = 0;
        }
    }

    @Override
    public void close() throws IOException {
        // Flush pending requests
        LOG.info("Processing remaining requests [docs = " + bulkDocs
                + ", length = " + bulkLength + ", total docs = " + indexedDocs + "]");
        createNewBulk = false;
        commit();
        // flush one more time to finalize the last bulk
        LOG.info("Processing to finalize last execute");
        createNewBulk = false;
        commit();

        // Close
        client.close();
        if (node != null) {
            node.close();
        }
    }

    @Override
    public String describe() {
        StringBuffer sb = new StringBuffer("SMIndexWriter for Elasticsearch 2.x\n");
        sb.append("\t").append(ElasticConstants.CLUSTER)
                .append(" : elastic prefix cluster\n");
        sb.append("\t").append(ElasticConstants.HOST).append(" : hostname\n");
        sb.append("\t").append(ElasticConstants.PORT).append(" : port\n");
        sb.append("\t").append(ElasticConstants.INDEX)
                .append(" : elastic index command \n");
        sb.append("\t").append(ElasticConstants.MAX_BULK_DOCS)
                .append(" : elastic bulk index doc counts. (default 250) \n");
        sb.append("\t").append(ElasticConstants.MAX_BULK_LENGTH)
                .append(" : elastic bulk index length. (default 2500500 ~2.5MB)\n");
        return sb.toString();
    }

    @Override
    public void setConf(Configuration conf) {
        config = conf;
        String cluster = conf.get(ElasticConstants.CLUSTER);
        String host = conf.get(ElasticConstants.HOST);

        if (StringUtils.isBlank(cluster) && StringUtils.isBlank(host)) {
            String message = "Missing elastic.cluster and elastic.host. At least one of them should be set in nutch-site.xml ";
            message += "\n" + describe();
            LOG.error(message);
            throw new RuntimeException(message);
        }
    }

    @Override
    public Configuration getConf() {
        return config;
    }
}
