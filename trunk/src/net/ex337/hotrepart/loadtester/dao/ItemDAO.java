package net.ex337.hotrepart.loadtester.dao;

import java.util.List;

/**
 * The item DAO interface.
 *
 * @author ian
 */
public interface ItemDAO {

    /**
     * inserts one item and returns the UUID in hex encoding.
     */
	public String insertItem(String desc, long owner);

    /**
     * Updates an item with the given UUID (in hex format).
     *
     * If the version supplied does not match the version in the DB, an exception is thrown.
     *
     * @param uuid the item ID
     * @param desc the description to update to
     * @param version the version of the item
     */
	public void updateItem(String uuid, String desc, int version);

    /**
     * Retrieves one item.
     * 
     * If the item requested does not exist, an exception is thrown.
     * 
     * @param uuid
     * @return the item.
     */
	public Item getItem(String uuid);

	/**
     * Returns an unordered list of all items in the DB
     * with an owner equal to one in the supplied array of owner IDs.
     *
     * @param owners the array of owner IDs
     * @return a list of items, which may be empty if no items are found.
     */
	public List<Item> getItems(Long[] owners);

}