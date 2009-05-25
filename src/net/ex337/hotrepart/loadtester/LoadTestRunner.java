package net.ex337.hotrepart.loadtester;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.ex337.hotrepart.loadtester.dao.ItemDAO;
import net.ex337.hotrepart.loadtester.executors.LoadGeneratingScheduledExecutor;
import net.ex337.hotrepart.loadtester.executors.LoadGeneratingThreadPoolExecutor;
import net.ex337.hotrepart.loadtester.telemetry.LoadReceiver;
import net.ex337.hotrepart.loadtester.telemetry.LoadRecorder;
import net.ex337.hotrepart.loadtester.workers.ItemUpdater;
import net.ex337.hotrepart.loadtester.workers.ItemWriter;
import net.ex337.hotrepart.loadtester.workers.StatisticsCollector;

/*
 * This class deals with running the actual load test.
 *
 * It loads the configuration, sets up the thread pools
 * and adds the initial workers to them, then lets them
 * run for the assigned duration.
 *
 */
public class LoadTestRunner implements Runnable {
	
	private ItemDAO dao;
    private LoadTesterConfiguration conf;
    private LoadReceiver receiver;

    public static final int COOKIE_SIZE = 10;

    /**
     *
     * @param dao the DAO implementation to run against
     * @param config the configuration object
     * @param receiver the implementation of the LoadReceiver that will receive the collated telemetry.
     */
	public LoadTestRunner(ItemDAO dao, LoadTesterConfiguration config, LoadReceiver receiver) {
		
		this.dao = dao;
        this.conf = config;
        this.receiver = receiver;
		
	}
	
	public void run() {

		LoadRecorder reader = new LoadGeneratingThreadPoolExecutor("reads", conf.getNumReadThreadsInit(), conf.getNumReadThreadsMax());
		LoadRecorder writer = new LoadGeneratingScheduledExecutor("writes", conf.getProxyPoolMaxSize(), 0, conf.getWritePeriod(),conf.getWritePeriodUnit());
		LoadRecorder updater = new LoadGeneratingScheduledExecutor("updates", conf.getProxyPoolMaxSize(), 0, conf.getWritePeriod(), conf.getWritePeriodUnit());

        //statistics executor doesn't need any special interfaces or configuration
		ScheduledExecutorService statsExecutor = new ScheduledThreadPoolExecutor(1);

		statsExecutor.scheduleAtFixedRate(new StatisticsCollector(receiver, reader, writer, updater), 0, conf.getStatsPeriod(), conf.getStatsPeriodUnit());

        //this queue is the channel between the "writer" and "updater" worker.
        BlockingDeque<ItemId> itemsToUpdate = new LinkedBlockingDeque<ItemId>(conf.getUpdateQueueSize());

		writer.addLoadGenerator(new ItemWriter(reader, itemsToUpdate, dao, conf.getBlockSize(), conf.getNumOwners(), conf.getCharset()));
		
		updater.addLoadGenerator(new ItemUpdater(reader, itemsToUpdate, dao, conf.getBlockSize(), conf.getCharset()));

        //wait for period
		try {
			Thread.sleep(TimeUnit.MILLISECONDS.convert(conf.getRunTime(), conf.getRunTimeUnit()));
		} catch (InterruptedException e) {
			throw new LoadTesterRuntimeException(e);
		}

        //shutdown everything.
		writer.shutdown();
		updater.shutdown();
		reader.shutdown();
		statsExecutor.shutdown();
	}

}
