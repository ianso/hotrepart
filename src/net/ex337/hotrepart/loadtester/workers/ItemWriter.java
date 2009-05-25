package net.ex337.hotrepart.loadtester.workers;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.BlockingDeque;
import net.ex337.hotrepart.loadtester.LoadTestRunner;
import net.ex337.hotrepart.loadtester.LoadTesterRuntimeException;
import net.ex337.hotrepart.loadtester.ItemId;
import net.ex337.hotrepart.loadtester.dao.ItemDAO;
import net.ex337.hotrepart.loadtester.telemetry.GeneratedLoad;
import net.ex337.hotrepart.loadtester.telemetry.LoadGenerator;
import net.ex337.hotrepart.loadtester.telemetry.LoadReceiver;
import net.ex337.hotrepart.loadtester.telemetry.LoadRecorder;
import net.ex337.hotrepart.loadtester.util.CryptUtils;
import net.ex337.hotrepart.loadtester.workers.ItemReader.CookiePosition;

/**
 *
 * This worker is fired at regular intervals to write new data
 * to the database. The configuration is intended to fire writes
 * so often that concurrent writes are guaranteed.
 *
 * After the item is written, a "read" worker is created to read
 * back the data to ensure that it was written correctly, and the
 * newly created record is also submitted to the queue read by
 * the "update" worker.
 *
 * @author ian
 */
public class ItemWriter implements Runnable, LoadGenerator {

	private ItemDAO dao;
	private int writeSize;
	private int numOwners;
    private String charSet;

    /*
     * The "cookie" is used as part of the data written to the database.
     * It's used by the reader worker to confirm that the data was written
     * correctly.
     */
	private byte[] cookie;
	private String cookieStr;

	private Random rnd;
	
	private LoadReceiver receiver;
	private LoadRecorder readerExecutor;
	private BlockingDeque<ItemId> itemsToUpdate;
	
	public ItemWriter(LoadRecorder readerExecutor, BlockingDeque<ItemId> itemsToUpdate, ItemDAO dao, int writeSize, int numOwners, String charSet) {
		this.dao = dao;
		this.writeSize = writeSize;
		this.numOwners = numOwners;
        this.charSet = charSet;
		this.itemsToUpdate = itemsToUpdate;
		this.readerExecutor = readerExecutor;

        /*
         * Generate a random cookie in byte array and String.
         */
		if(writeSize <= LoadTestRunner.COOKIE_SIZE * 2) {
			throw new LoadTesterRuntimeException("Write size "+writeSize+" must be at least twice cookie size "+LoadTestRunner.COOKIE_SIZE);
		}
		rnd = new SecureRandom();

		cookie = new byte[LoadTestRunner.COOKIE_SIZE];
		rnd.nextBytes(cookie);
		CryptUtils.cleanForUTF8(cookie);

		try {
			cookieStr = new String(cookie, charSet);
		} catch (UnsupportedEncodingException e) {
			throw new LoadTesterRuntimeException(e);
		}

	}

	public void setLoadReceiver(LoadReceiver receiver) {
		this.receiver = receiver;
	}

	public void run() {

        //generate random data to write
		byte[] write = new byte[writeSize];
		rnd.nextBytes(write);

        //remove nulls
		CryptUtils.cleanForUTF8(write);

        //set cookie
		System.arraycopy(cookie, 0, write, 0, cookie.length);
		
		long start = System.currentTimeMillis();
		
		String uuid;
		try {
			uuid = dao.insertItem(new String(write, charSet), rnd.nextInt(numOwners));
		} catch (UnsupportedEncodingException e) {
			throw new LoadTesterRuntimeException(e);
		}

		long end = System.currentTimeMillis();

        // 16 + 4 + 2 = bytes for uuid + long + int
        receiver.addGeneratedLoad(new GeneratedLoad(1, write.length+16+4+2, end-start));

        //add a worker to read back the item and check the data
		readerExecutor.addLoadGenerator(new ItemReader(dao, uuid, cookieStr, CookiePosition.START));

        //add the item to the update queue if it's not full.
		itemsToUpdate.offerLast(new ItemId(uuid, cookie, 0));		
	}

}