package ru.wobot.vk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.social.vkontakte.api.impl.VKontakteTemplate;
import org.apache.hadoop.conf.Configuration;

import java.lang.management.ManagementFactory;

public enum Proxy implements ApiBindingRepository {
    INSTANCE;
    private static final Log LOG = LogFactory.getLog(Proxy.class.getName());

    private static final Credential[] credentials = new Credential[]{
            new Credential("2ff5fff8d49ffa2da2875c99faf61399f5480ee10a2d70dde44027b9b248eae6a014d7689f7d0f216ab00", "ZydztdkkmEDkvBNsulQt"),
            new Credential("16a33dd74f9bfd91577c32c9edf5c9ee39a5be5c453219ac6209933e3f34726d460007ae824703bac212b", "XViEI3tDzl3G70364Y3k"),
            new Credential("ceb1fdf23b5e71660e13e2706ec7280c5ce36ff73a129ead4390f201b0718a026ba8d916936d9dac8c1ff", "ecpZ4bprboHPTqnN8N7L"),
            new Credential("dbc7689873b05f6210e9dd869282c9bddd9e0f84ede72d089ffa89bb8c129c52243934ab7597f0adf3fe5", "L40pcjJEnQO3zC1rkrNH"),
    };
    private static int index = 0;
    private Configuration conf;

    private void printContext() {
        LOG.info("----------------------------------------------------------");
        LOG.info("JVM process: " + ManagementFactory.getRuntimeMXBean().getName());
        int i = conf.getInt("mapred.task.partition", -2);
        LOG.info("Thread id: " + Thread.currentThread().getId());
        LOG.info("Partition num: " + i);
        String s = conf.getStrings("mapred.job.id")[0];
        String s1 = conf.getStrings("mapred.tip.id")[0];
        String s2 = conf.getStrings("mapred.task.id")[0];

        LOG.info("Job id: " + s);
        LOG.info("Task id: " + s1);
        LOG.info("Attempt id: " + s2);
        LOG.info("Vk acc file: " + conf.getStrings("vk.accounts")[0]);
    }

    public synchronized void setConf(Configuration conf) {
        if (this.conf == null) {
            this.conf = conf;
            LOG.info("Thread id, who seted conf: " + Thread.currentThread().getId());
        } else
            LOG.info("Thread id - " + Thread.currentThread().getId() + "tried to set conf, but it was already set");
    }

    @Override
    public synchronized VKontakteTemplate getInstance() {
        printContext();
        index = (index + 1) % credentials.length;
        Credential c = credentials[index];
        return new VKontakteTemplate(c.accessToken, c.clientSecret);
    }

    static class Credential {
        String accessToken;
        String clientSecret;

        public Credential(String accessToken, String clientSecret) {
            this.accessToken = accessToken;
            this.clientSecret = clientSecret;
        }
    }

}

