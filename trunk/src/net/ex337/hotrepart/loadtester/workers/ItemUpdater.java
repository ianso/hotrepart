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
 * This worker checks the queue containing inserted or updated items
 * a regular intervals, then updates them, adds a read-back job, and
 * re-adds them to the same queue.
 *
 * @author ian
 */
public class ItemUpdater implements Runnable, LoadGenerator {

	private ItemDAO dao;
	private int updateSize;

    /*
     * The "cookie" is used as part of the data written to the database.
     * It's used by the reader worker to confirm that the data was written
     * correctly.
     */
	private byte[] endCookie;
	private String cookieStr;
    private String charSet;

	private Random rnd;
	
	private LoadReceiver receiver;
	private LoadRecorder readerExecutor;
	private BlockingDeque<ItemId> itemsToUpdate;
	
	public ItemUpdater(LoadRecorder reader, BlockingDeque<ItemId> itemsToUpdate, ItemDAO dao, int updateSize, String charSet) {
		this.dao = dao;
		this.updateSize = updateSize;
		this.itemsToUpdate = itemsToUpdate;
        this.charSet = charSet;
		this.readerExecutor = reader;

        /*
         * generate cookie.for update. The write cookie goes at the
         * start of the random sequence. The update cookie goes at
         * the end to prevent conflicts.
         */

		if(updateSize <= LoadTestRunner.COOKIE_SIZE * 2) {
			throw new LoadTesterRuntimeException("Update size "+updateSize+" must be at least twice cookie size "+LoadTestRunner.COOKIE_SIZE);
		}
		rnd = new SecureRandom();

		endCookie = new byte[LoadTestRunner.COOKIE_SIZE];
		rnd.nextBytes(endCookie);
		CryptUtils.cleanForUTF8(endCookie);

		try {
			cookieStr = new String(endCookie, charSet);
		} catch (UnsupportedEncodingException e) {
			throw new LoadTesterRuntimeException(e);
		}
		
	}

	public void setLoadReceiver(LoadReceiver receiver) {
		this.receiver = receiver;
	}
	
	public void run() {
		
		ItemId id = itemsToUpdate.poll();
		
		if(id == null) return;
		
		byte[] write = new byte[updateSize];
		
		rnd.nextBytes(write);
		CryptUtils.cleanForUTF8(write);

        //write update cookie
		System.arraycopy(endCookie, 0, write, write.length-endCookie.length, endCookie.length);
        //preserve write cookie
        System.arraycopy(id.getStartCookie(), 0, write, 0, id.getStartCookie().length);

		long start = System.currentTimeMillis();
		
		try {
			dao.updateItem(id.getUuid(), new String(write, charSet), id.getVersion());
		} catch (UnsupportedEncodingException e) {
			throw new LoadTesterRuntimeException(e);
		}
		
		long end = System.currentTimeMillis();
		
        // 16 + 4+2 = bytes for uuid + long + int
        receiver.addGeneratedLoad(new GeneratedLoad(1, write.length+16+4+2, end-start));

        //register the read-back
		readerExecutor.addLoadGenerator(new ItemReader(dao, id.getUuid(), cookieStr, CookiePosition.END));

        //not strictly sure if this is necessary.
		itemsToUpdate.offerLast(new ItemId(id.getUuid(), id.getStartCookie(), id.getVersion()+1));
		
	}

}