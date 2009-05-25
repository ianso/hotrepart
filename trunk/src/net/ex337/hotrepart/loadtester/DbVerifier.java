
package net.ex337.hotrepart.loadtester;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.ex337.hotrepart.loadtester.dao.Item;
import net.ex337.hotrepart.loadtester.dao.ItemDAO;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * Writes 1000 items to the database, then updates
 * them, then reads them back using the two different
 * methods.
 *
 * @author ian
 */
public class DbVerifier implements Runnable {

    private ItemDAO dao;

    public DbVerifier(ItemDAO dao) {
        this.dao = dao;

    }

    public void run() {

        Random random = new SecureRandom();

        System.out.println("starting test");

		final int NUM_OWNERS = 10;

		List<String> uuids = new ArrayList<String>();

		String prefix = Long.toString(System.currentTimeMillis());

		System.out.print("Prefix: "+prefix+", Inserting");

		for(int numInsert = 0; numInsert != 100; numInsert++) {

			uuids.add(dao.insertItem(StringUtils.repeat(prefix+numInsert + "name", 20), random.nextInt(NUM_OWNERS)));

			if(numInsert % 100 == 0) {
				System.out.print(".");
			}

		}

		System.out.println();

		System.out.print("inserted, updating 3x");

		for(int updateV = 0; updateV != 3; updateV++) {

			for(String uuid : uuids) {

				dao.updateItem(uuid, StringUtils.repeat(uuid+" v"+updateV, 20), updateV);

			}
		}

		System.out.println();

		System.out.print("updated, retrieving");

		for(String uuid : uuids) {

			Item i = dao.getItem(uuid);

			if( ! uuid.equals(StringUtils.remove(i.getUuid(), "-"))) {
				throw new LoadTesterRuntimeException("Wrong value for "+uuid+", got "+i.getUuid());
			}

			if( ! i.getDescription().contains(uuid + " v2")) {
				throw new LoadTesterRuntimeException("Wrong value for "+uuid+", got "+i.getDescription());
			}

		}

		System.out.println();

		System.out.print("retrieved by Id, doing by owners");

		for(int numQuery = 0; numQuery != 100; numQuery++) {

			Long[] owners = new Long[random.nextInt(NUM_OWNERS)+1];

			for(int numFriend = 0; numFriend != owners.length; numFriend++) {
				owners[numFriend] = new Long(random.nextInt(NUM_OWNERS));
			}

			List<Item> result = dao.getItems(owners);

			boolean none = true;

			Set<String> retrievedItems = new HashSet<String>();

			for(Item i : result) {

				none = false;

				if( ! ArrayUtils.contains(owners, i.getOwner())) {
					throw new LoadTesterRuntimeException("Item "+i.getUuid()+" returned with owner "+i.getOwner()+", not in array "+ArrayUtils.toString(owners));
				}

				if(retrievedItems.contains(i.getUuid())) {
					throw new LoadTesterRuntimeException("Item "+i.getUuid()+" returned more than once");
				}

				retrievedItems.add(i.getUuid());

			}

			if(none) {
				throw new LoadTesterRuntimeException("No result from get_items");
			}

			if(numQuery % 100 == 0) {
				System.out.print(".");
			}

		}
    }

}
