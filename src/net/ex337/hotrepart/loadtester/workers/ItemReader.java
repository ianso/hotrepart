package net.ex337.hotrepart.loadtester.workers;

import net.ex337.hotrepart.loadtester.LoadTesterRuntimeException;
import net.ex337.hotrepart.loadtester.dao.Item;
import net.ex337.hotrepart.loadtester.dao.ItemDAO;
import net.ex337.hotrepart.loadtester.telemetry.GeneratedLoad;
import net.ex337.hotrepart.loadtester.telemetry.LoadGenerator;
import net.ex337.hotrepart.loadtester.telemetry.LoadReceiver;

/**
 *
 * This class reads back a given item and confirms that it was stored correctly.
 * It should be done as soon as possible after the item was created.
 *
 * @author ian
 */
public class ItemReader implements Runnable, LoadGenerator {
	
	public enum CookiePosition {START, END};
	
	private ItemDAO dao;
	private String uuid;
	private String cookie;
	private CookiePosition cookiePosition;

	private LoadReceiver receiver;

	public ItemReader(ItemDAO dao, String uuid, String cookie, CookiePosition cookiePosition) {
		this.dao = dao;
		this.uuid = uuid;
		this.cookie = cookie;
		this.cookiePosition = cookiePosition;
	}
	
	public void run() {
		
		long start = System.currentTimeMillis();

		Item item = dao.getItem(uuid);

		long end = System.currentTimeMillis();

		if(cookiePosition == CookiePosition.START) {
			if( ! item.getDescription().startsWith(cookie)) {
				throw new LoadTesterRuntimeException("Item uuid "+item.getUuid()+" description "+item.getDescription()+" does not start with cookie "+cookie);
			}
		} else {
			if( ! item.getDescription().endsWith(cookie)) {
				throw new LoadTesterRuntimeException("Item uuid "+item.getUuid()+" description "+item.getDescription()+" does not end with cookie "+cookie);
			}
		}

        /* 16+4+2 = bytes for uuid + long + int */
        receiver.addGeneratedLoad(new GeneratedLoad(1, item.getDescription().length()+16+4+2, end-start));
	}

	public void setLoadReceiver(LoadReceiver receiver) {
		this.receiver = receiver;
	}

}