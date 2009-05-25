package net.ex337.hotrepart.loadtester.dao;

import net.ex337.hotrepart.loadtester.LoadTesterRuntimeException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import net.ex337.hotrepart.loadtester.util.CryptUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/*
 * The PostgreSQL implementation of the DAO interface.
 *
 * This is just a simple Spring-based DAO implementation.
 *
 */
public class ItemDAOPostgresImpl extends JdbcTemplate implements ItemDAO {
	
	private SecureRandom random;

    private RowMapper ROWMAPPER = new ItemRowMapper();
	
	public ItemDAOPostgresImpl(DataSource ds) {
		random = new SecureRandom();
		random.setSeed(random.generateSeed(Item.ID_SIZE_BYTES));
		super.setDataSource(ds);
	}

	public String insertItem(String desc, long owner) {
		
		byte[] key = new byte[Item.ID_SIZE_BYTES];
		random.nextBytes(key);
		String uuid = CryptUtils.toHex(key);

		int result;
		
		if((result = super.queryForInt("select insert_item(?, ?, ?)", 
				new Object[] {uuid, desc, owner}, 
				new int[] {Types.OTHER, Types.VARCHAR, Types.BIGINT}
		)) >= 1) {
			return uuid;
		}
		
		throw new LoadTesterRuntimeException("insert_item returned less than 1:"+result);
	}
	
	public void updateItem(String uuid, String desc, int version) {

		int result;
		
		if((result = super.queryForInt("select update_item(?, ?, ?)", 
				new Object[] {uuid, desc, version}, 
				new int[] {Types.OTHER, Types.VARCHAR, Types.INTEGER}
		)) >= 1) {
			return;
		}
		
		throw new LoadTesterRuntimeException("update_item returned less than 1:"+result);
	}
	
	public Item getItem(String uuid) {
		
		return (Item) super.queryForObject("select * from get_item(?)",
				new Object[] {uuid}, 
				new int[] {Types.OTHER}, 
				ROWMAPPER);

	}
	
	@SuppressWarnings("unchecked")
	public List<Item> getItems(Long[] owners) {

		return super.query(
                "select * from get_items(?)",
                new GetItemsPreparedStatementSetter(owners),
                ROWMAPPER);

	}
	
	private static class GetItemsPreparedStatementSetter implements PreparedStatementSetter {
		
		private Long[] owners;
		
		private GetItemsPreparedStatementSetter(Long[] owners) {
			this.owners = owners;
		}
		
		public void setValues(PreparedStatement ps) throws SQLException {
			ps.setArray(1, ps.getConnection().createArrayOf("bigint", owners));
		}
		
	}
	
	private static class ItemRowMapper implements RowMapper {
		public Object mapRow(ResultSet rs, int arg1) throws SQLException {
			return new Item(rs.getString("iditem"), rs.getString("description"), rs.getLong("owner"), rs.getInt("v"));
		}
	}

}
