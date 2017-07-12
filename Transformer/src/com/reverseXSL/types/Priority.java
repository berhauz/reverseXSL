package com.reverseXSL.types;

import java.io.Serializable;

/**
 * Defines message priority (header level): Urgent, Normal, Low
 * 
 * @author bernardH
 * 
 */
public final class Priority implements Serializable {

	private static final long serialVersionUID = -4535127792329196464L;
	

	private final static String URGENT_TYPE_STRING = "URGENT";
	private final static String NORMAL_TYPE_STRING = "NORMAL";
	private final static String LOW_TYPE_STRING = "LOW";
	
	final String code;
	final String info;
		
	// + + + + + + + + +  P R I V A T E   C O N S T R U C T O R   + + + + + + + + + + +
	
	private Priority(String code, String info) {
		this.code = code;
		this.info = info;
	}
	
	// + + + + + + F I N A L   S T A T I C   T Y P E   D E F I N I T I O N  + + + + + +
	
	/**
	 * Urgent message; shall be processed first; can pass in front of other
	 * messages and/or be routed through specific/reserved communication
	 * channels
	 */
	public final static Priority URGENT = new Priority(URGENT_TYPE_STRING, "Urgent");

	/**
	 * Default Priority; just process on the fly
	 */
	public final static Priority NORMAL = new Priority(NORMAL_TYPE_STRING, "Default/Normal");

	/**
	 * Low priority message; may be batched for delayed transmission
	 */
	public final static Priority LOW = new Priority(LOW_TYPE_STRING, "Low priority");

	// please preserve at all times the alignment between the table below and
	// singletons hereabove
	/**
	 * May be used to iterate over all possible enumeration values.
	 */
	public final static Priority[] table = { URGENT, NORMAL, LOW };

	/**
	 * Provides a default string rendering with long name followed by (CODE) as
	 * would be used when using a member like <code>Priority.CODE</code>.
	 */
	public String toString() {
		return info + " (" + code + ")";
	};

	static public Priority find(String key) {
		for (int i = 0; i < table.length; i++)
			if (table[i].code.equals(key))
				return table[i];
		return NORMAL;
	}
	
	public boolean equals(Object obj) {
		if (obj.getClass().getName()!=this.getClass().getName()) return false;
		return (((Priority)obj).code.equals(this.code));
	}

}