package ru.wobot.nutch.indexwriter.sm.hbase;

import com.google.gson.GsonBuilder;
import com.google.protobuf.ServiceException;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapred.JobConf;
import org.apache.nutch.indexer.NutchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wobot.sm.core.meta.ContentMetaConstants;
import ru.wobot.sm.core.parse.ParseResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Node;
import static com.sun.tools.corba.se.idl.toJavaPortable.Arguments.Client;


@SuppressWarnings("Duplicates")
public class IndexWriter implements org.apache.nutch.indexer.IndexWriter {
    private static final int DEFAULT_MAX_BULK_DOCS = 250;
    private static final int DEFAULT_MAX_BULK_LENGTH = 2500500;
    public static Logger LOG = LoggerFactory.getLogger(IndexWriter.class);

    private Configuration config;

    private int maxBulkDocs;
    private int maxBulkLength;
    private long indexedDocs = 0;
    private int bulkDocs = 0;
    private int bulkLength = 0;
    private Connection connection;
    private HashMap<String, BufferedMutator> mutators = new HashMap<>();
    private Configuration conf;

    private static <T> T fromJson(String json, Class<T> classOfT) {
        return new GsonBuilder()
                .create()
                .fromJson(json, classOfT);
    }


    @Override
    public void open(JobConf job, String name) throws IOException {
        maxBulkDocs = job.getInt(ElasticConstants.MAX_BULK_DOCS,
                DEFAULT_MAX_BULK_DOCS);
        maxBulkLength = job.getInt(ElasticConstants.MAX_BULK_LENGTH,
                DEFAULT_MAX_BULK_LENGTH);

        conf = HBaseConfiguration.create();
        try {
            HBaseAdmin.checkHBaseAvailable(conf);
        } catch (MasterNotRunningException e) {
            LOG.error("Unable to find a running HBase instance", e);
        } catch (ZooKeeperConnectionException e) {
            LOG.error("Unable to connect to ZooKeeper", e);
        } catch (ServiceException e) {
            LOG.error("HBase service unavailable", e);
        } catch (IOException e) {
            LOG.error("Error when trying to get HBase status", e);
        }
        connection = ConnectionFactory.createConnection(conf);
    }

    @Override
    public void write(NutchDocument doc) throws IOException {
        final boolean isSingleDoc = !"true".equals(doc.getDocumentMeta().get(ContentMetaConstants.MULTIPLE_PARSE_RESULT));
        String id = (String) doc.getFieldValue("id");
        String type = doc.getDocumentMeta().get(ContentMetaConstants.TYPE);

        if (type == null && isSingleDoc) {
            LOG.info("Type not defined skipped document from the index: " + id);
            return;
        }

        if (isSingleDoc) {
            Put put = new Put(Bytes.toBytes(id));

            // Loop through all fields of this doc
            for (String fieldName : doc.getFieldNames()) {
                if (doc.getField(fieldName).getValues().size() > 1) {
                    throw new RuntimeException("The HBase import does not support multi-value field.");
                } else {
                    Object value = doc.getFieldValue(fieldName);
                    put.addColumn(Bytes.toBytes("p"), Bytes.toBytes(fieldName), toByte(value));
                    bulkLength += doc.getFieldValue(fieldName).toString().length();
                }
            }

            BufferedMutator mutator = getMutator(type);
            mutator.mutate(put);
            indexedDocs++;
            bulkDocs++;

            flushIfNecessary(id);
        } else {
            // for indexing documents more than one
            String content = (String) doc.getFieldValue("content");
            String segment = (String) doc.getFieldValue("segment");
            String boost = (String) doc.getFieldValue("boost");
            Float score = (Float) doc.getFieldValue("score");
            ParseResult[] parseResults = fromJson(content, ParseResult[].class);
            if (parseResults != null) {
                for (ParseResult parseResult : parseResults) {
                    id = parseResult.getUrl();
                    Put put = new Put(Bytes.toBytes(id));
                    put.addColumn(Bytes.toBytes("p"), Bytes.toBytes("segment"), toByte(segment));
                    bulkLength += segment.toString().length();
                    put.addColumn(Bytes.toBytes("p"), Bytes.toBytes("boost"), toByte(Float.parseFloat(boost)));
                    bulkLength += boost.length();
                    put.addColumn(Bytes.toBytes("p"), Bytes.toBytes("score"), toByte(score));
                    bulkLength += score.toString().length();

                    for (Map.Entry<String, Object> p : parseResult.getParseMeta().entrySet()) {
                        put.addColumn(Bytes.toBytes("p"), Bytes.toBytes(p.getKey()), toByte(p.getValue()));
                        bulkLength += p.getValue().toString().length();
                    }
                    String subType = (String) parseResult.getContentMeta().get(ContentMetaConstants.TYPE);
                    if (subType == null) {
                        subType = type;
                    }

                    BufferedMutator mutator = getMutator(subType);
                    mutator.mutate(put);

                    indexedDocs++;
                    bulkDocs++;

                    flushIfNecessary(id);
                }
            }
        }
    }

    private BufferedMutator getMutator(String type) throws IOException {
        BufferedMutator mutator = mutators.get(type);
        if (mutator == null) {
            mutator = connection.getBufferedMutator(TableName.valueOf(type));
            mutators.put(type, mutator);
        }
        return mutator;
    }

    private byte[] toByte(Object value) {
        if (value instanceof String)
            return Bytes.toBytes((String) value);
        if (value instanceof Boolean)
            return Bytes.toBytes((Boolean) value);
        if (value instanceof Byte)
            return Bytes.toBytes((Byte) value);
        if (value instanceof Short)
            return Bytes.toBytes((Short) value);
        if (value instanceof Integer)
            return Bytes.toBytes((Integer) value);
        if (value instanceof Long)
            return Bytes.toBytes((Long) value);
        if (value instanceof Character)
            return Bytes.toBytes((Character) value);
        if (value instanceof Float)
            return Bytes.toBytes((Float) value);
        if (value instanceof Double)
            return Bytes.toBytes((Double) value);
        throw new RuntimeException("Type not supporting");
    }

    private void flushIfNecessary(String id) throws IOException {
        if (bulkDocs >= maxBulkDocs || bulkLength >= maxBulkLength) {
            LOG.info("Processing bulk request [docs = " + bulkDocs + ", length = "
                    + bulkLength + ", total docs = " + indexedDocs
                    + ", last doc in bulk = '" + id + "']");
            // Flush the bulk of indexing requests
            commit();
        }
    }

    @Override
    public void delete(String key) throws IOException {
/*
        try {
            DeleteRequestBuilder builder = client.prepareDelete();
            builder.setIndex(defaultIndex);
            builder.setType("doc");
            builder.setId(key);
            builder.execute().actionGet();
        } catch (ElasticsearchException e) {
            throw makeIOException(e);
        }
*/
    }

    @Override
    public void update(NutchDocument doc) throws IOException {
        /*write(doc);*/
    }

    @Override
    public void commit() throws IOException {
        for (Map.Entry<String, BufferedMutator> mutator : mutators.entrySet()) {
            mutator.getValue().flush();
        }
    }

    @Override
    public void close() throws IOException {
        // Flush pending requests
        LOG.info("Processing remaining requests [docs = " + bulkDocs
                + ", length = " + bulkLength + ", total docs = " + indexedDocs + "]");
        commit();
        LOG.info("Processing to finalize last execute");
        try {
            // Close
            for (Map.Entry<String, BufferedMutator> mutator : mutators.entrySet()) {
                mutator.getValue().close();
            }
            connection.close();
        } catch (MasterNotRunningException e) {
            LOG.error("Unable to find a running HBase instance", e);
        } catch (ZooKeeperConnectionException e) {
            LOG.error("Unable to connect to ZooKeeper", e);
        } catch (IOException e) {
            LOG.error("Error when trying to get HBase status", e);
        }
    }

    @Override
    public String describe() {
        StringBuffer sb = new StringBuffer("SMIndexWriter for HBase\n");
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
    public Configuration getConf() {
        return config;
    }

    @Override
    public void setConf(Configuration conf) {
        config = conf;
    }
}
