package net.ex337.hotrepart.loadtester.dao;

import net.ex337.hotrepart.loadtester.LoadTesterRuntimeException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ex337.hotrepart.loadtester.util.CryptUtils;

import org.apache.commons.lang.ArrayUtils;

/**
 *
 * This is a mock implementation of the DAO interface, backed
 * by a HashMap.
 *
 * Because the random key is generated here, the insert method
 * doesn't need to be locked, or to check for overwrites.
 *
 * @author ian
 */
public class ItemDAOMockImpl implements ItemDAO {

	private Map<String, Item> storage;

	private SecureRandom random;
	
	public ItemDAOMockImpl() {
		storage = Collections.synchronizedMap(new HashMap<String, Item>());
		random = new SecureRandom();
		random.setSeed(random.generateSeed(Item.ID_SIZE_BYTES));
	}

	public Item getItem(String uuid) {
		
		Item result = storage.get(uuid);
		
		if(result == null) {
			throw new LoadTesterRuntimeException("item not found for uuid "+uuid);
		}

		return (Item) result.clone();
	}

	public List<Item> getItems(Long[] owners) {
		
		List<Item> result = new ArrayList<Item>();
		
		/*
		 * Doing this to avoid ConcurrentModificationException. 
		 */
		List<Item> values = new ArrayList<Item>(storage.values());
		
		for(Item i : values) {
			if(ArrayUtils.contains(owners, i.getOwner())) {
				result.add((Item) i.clone());
			}
		}
		
		return result;
	}

	public String insertItem(String desc, long owner) {
		
		byte[] key = new byte[Item.ID_SIZE_BYTES];
		random.nextBytes(key);
		String uuid = CryptUtils.toHex(key);

		storage.put(uuid, new Item(uuid, desc, owner, 0));
		
		return uuid;
	}

	public void updateItem(String uuid, String desc, int version) {

		Item result = storage.get(uuid);
		
		if(result == null) {
			throw new LoadTesterRuntimeException("item not found for uuid "+uuid);
		}
		
		synchronized(result) {
			if(result.getVersion() != version) {
				throw new LoadTesterRuntimeException("item versions for uuid "+uuid+" inconsistent: storage="+result.getVersion()+", passed="+version);
			}
			
			result.setDescription(desc);
			result.setVersion(result.getVersion()+1);

		}
		
		
	}

}
