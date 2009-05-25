/**
 * Stores all configuratio data.
 *
 */

package net.ex337.hotrepart.loadtester;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author ian
 */
public class LoadTesterConfiguration {

    private int numOwners;
    private int writePeriod;
    private TimeUnit writePeriodUnit;
    private int blockSize;
    private int runTime;
    private TimeUnit runTimeUnit;

    private String charset;
    private int updateQueueSize;
    private int numReadThreadsInit;
    private int numReadThreadsMax;

    private int statsPeriod;
    private TimeUnit statsPeriodUnit;

    private String proxyHost;
    private int proxyPort;
    private String proxyDb;
    private String proxyUser;
    private String proxyPassword;
    private int proxyPoolInitSize;
    private int proxyPoolMaxSize;
    
    public LoadTesterConfiguration(File file) {

        Properties p = new Properties();

        try {
            InputStream fIn = new FileInputStream(file);
            p.load(fIn);
            fIn.close();
        } catch(IOException e) {
            throw new LoadTesterRuntimeException(e);
        }

        charset = p.getProperty("charset");
        numOwners = Integer.parseInt(p.getProperty("num.owners"));
        blockSize = Integer.parseInt(p.getProperty("block.size"));
        writePeriod = Integer.parseInt(p.getProperty("write.period"));
        writePeriodUnit = TimeUnit.valueOf(p.getProperty("write.period.unit").toUpperCase());
        runTime = Integer.parseInt(p.getProperty("run.time"));
        runTimeUnit = TimeUnit.valueOf(p.getProperty("run.time.unit").toUpperCase());
        statsPeriod = Integer.parseInt(p.getProperty("stats.period"));
        statsPeriodUnit = TimeUnit.valueOf(p.getProperty("stats.period.unit").toUpperCase());
        numReadThreadsInit = Integer.parseInt(p.getProperty("reader.num.threads.init"));
        numReadThreadsMax = Integer.parseInt(p.getProperty("reader.num.threads.max"));
        updateQueueSize = Integer.parseInt(p.getProperty("updater.queue.size"));
        proxyHost = p.getProperty("proxy.host");
        proxyDb = p.getProperty("proxy.db");
        proxyUser = p.getProperty("proxy.user");
        proxyPassword = p.getProperty("proxy.password");
        proxyPort = Integer.parseInt(p.getProperty("proxy.port"));
        proxyPoolInitSize = Integer.parseInt(p.getProperty("proxy.pool.initSize"));
        proxyPoolMaxSize = Integer.parseInt(p.getProperty("proxy.pool.maxSize"));

    }

    public int getBlockSize() {
        return blockSize;
    }

    public String getCharset() {
        return charset;
    }

    public String getProxyDb() {
        return proxyDb;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getNumOwners() {
        return numOwners;
    }

    public int getNumReadThreadsInit() {
        return numReadThreadsInit;
    }

    public int getNumReadThreadsMax() {
        return numReadThreadsMax;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public int getProxyPoolMaxSize() {
        return proxyPoolMaxSize;
    }

    public int getProxyPoolInitSize() {
        return proxyPoolInitSize;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public int getRunTime() {
        return runTime;
    }

    public TimeUnit getRunTimeUnit() {
        return runTimeUnit;
    }

    public int getUpdateQueueSize() {
        return updateQueueSize;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public int getWritePeriod() {
        return writePeriod;
    }

    public TimeUnit getWritePeriodUnit() {
        return writePeriodUnit;
    }

    public int getStatsPeriod() {
        return statsPeriod;
    }

    public TimeUnit getStatsPeriodUnit() {
        return statsPeriodUnit;
    }

    
}
