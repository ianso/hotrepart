package net.ex337.hotrepart.loadtester.dao;

import net.ex337.hotrepart.loadtester.LoadTesterRuntimeException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * The item object. This being a demo application, there's no logic in here.
 * 
 * @author ian
 */
public class Item implements  Cloneable {

    /**
     * The length of the ID in bytes.
     */
	public static final int ID_SIZE_BYTES = 16;

    private String uuid;
	private String description;
	private long owner;
	private int version;

	public Item() {
		
	}
	
	public Item(String uuid, String description, long owner, int version) {
		super();
		this.uuid = uuid;
		this.description = description;
		this.owner = owner;
		this.version = version;
	}
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public long getOwner() {
		return owner;
	}
	public void setOwner(long owner) {
		this.owner = owner;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}

    @Override
	public final String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	@Override
	public final boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		return this.getClass().equals(o.getClass()) && this.hashCode() == o.hashCode();
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public Object clone() {
		try {
			return BeanUtils.cloneBean(this);
		} catch (Exception e) {
			throw new LoadTesterRuntimeException("Cannot clone object", e);
		}
	}


}
