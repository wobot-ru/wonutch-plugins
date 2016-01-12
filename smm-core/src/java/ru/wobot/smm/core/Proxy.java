package ru.wobot.smm.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

public enum Proxy implements CredentialRepository {
    INSTANCE;

    private static final String REQUESTS_PERSECOND = "smm.requests.persecond";
    private static final String ACCOUNTS_FILE = "smm.accounts";
    private static final Log LOG = LogFactory.getLog(Proxy.class.getName());
    private final BlockingQueue<DelayedCredential> credentials = new DelayQueue<>();
    private Configuration conf;
    private Collection<String> credentialsSource;

    public void setCredentialsSource(Collection<String> credentialsSource) {
        this.credentialsSource = credentialsSource;
    }

    private Collection<String> getTokensFromStream(InputStream input) throws IOException {
        Collection<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            String line;
            line = br.readLine();
            while (line != null && !line.isEmpty()) {
                result.add(line);
                line = br.readLine();
            }
        }
        return result;
    }

    public void resetQueue() {
        Objects.requireNonNull(conf);

        if (credentials.size() != 0)
            credentials.clear();

        int accounts = credentialsSource.size();
        int maxRequests = conf.getInt(REQUESTS_PERSECOND, 1);
        int partitions = conf.getInt("mapreduce.job.maps", 1);
        int partitionNum = conf.getInt("mapreduce.task.partition", 0);

        int num = accounts / partitions;
        int reminder = accounts % partitions;
        int startIndex = 1; // 1 based index
        if (partitionNum + 1 <= reminder) {
            startIndex = (partitionNum * (num + 1)) + 1;
            num++;
        } else if (num == 0)
            return;
        else
            startIndex = reminder + (partitionNum * num) + 1;

        int index = 1;
        for (String cred : credentialsSource) {
            if (index++ < startIndex)
                continue;
            String[] c = cred.split(",");
            try {
                credentials.put(new DelayedCredential(c[0], c[1], maxRequests));
            } catch (InterruptedException e) {
                LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
            }
            if (--num == 0)
                break;
        }
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
        synchronized (this) {
            if (this.credentialsSource == null) {
                try {
                    setCredentialsSource(getTokensFromStream(this.getClass().getClassLoader()
                            .getResourceAsStream(conf.getStrings(ACCOUNTS_FILE)[0])));
                } catch (IOException e) {
                    LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
                }
                resetQueue();
            }
        }
    }

    @Override
    public synchronized Credential getInstance() {
        DelayedCredential c = credentials.peek();
        if (c == null)
            throw new IllegalStateException("No credentials found.");
        if (c.getDelay(TimeUnit.NANOSECONDS) > 0)
            throw new TooManyRequestsException(c.getDelay(TimeUnit.MILLISECONDS));

        try {
            credentials.remove(c);
            c.used();
            credentials.put(c);
        } catch (InterruptedException e) {
            LOG.error(org.apache.hadoop.util.StringUtils.stringifyException(e));
        }

        LOG.info("Thread: " + Thread.currentThread().getId() + "; Credentials used: " + c.getAccessToken() + "-" + c.getClientSecret());
        return c;
    }

}
