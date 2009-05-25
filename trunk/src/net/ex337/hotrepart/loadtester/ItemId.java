package net.ex337.hotrepart.loadtester;

/**
 * This object is used to store item Id and version information
 * for later use by the ItemUpdater, which updates pre-existing
 * items in the DB.
 *
 * The start cookie is needed so that any subsequent read of the
 * same item will confirm that it was written correctly.
 *
 */
public class ItemId {

	private String uuid;
	private byte[] startCookie;
	private int version;

	public ItemId(String uuid, byte[] startCookie, int version) {
		super();
		this.uuid = uuid;
		this.version = version;
		this.startCookie = startCookie;
	}

	public String getUuid() {
		return uuid;
	}

	public int getVersion() {
		return version;
	}

	public byte[] getStartCookie() {
		return startCookie;
	}

}
