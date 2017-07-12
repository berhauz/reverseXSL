package com.reverseXSL.types;

import java.io.Serializable;

/**
 * Impact singletons specify the impact-level of a given exception onto a processed
 * message (FATAL or WARNING), and not onto the system in general. 
 * It is a strong typed enumeration class compatible with JDK1.4.
 * <p>
 * Valid enumeration values are all like <code>Impact.CODE</code> where CODE
 * must be a valid code; for the list of codes see {@link #table}.
 * <p>
 *
 * @see com.reverseXSL.parser.ParserException
 * @author bernardH
 */
public final class Impact implements Serializable {

	private static final long serialVersionUID = -4554564697397788463L;

	private String name;
	private String code;
	
	private Impact (String n1, String c1) {
		name = n1; 
		code = c1;
	}
	
	
	/**
	 * FATAL exception for the relevant message; 
	 * it implies that the message cannot be processed to the end, 
	 * and will be rejected; the parser may be allowed to continue and
	 * record more 
	 * than one FATAL exception before rejecting a message. But in
	 * any case, one or more FATAL exceptions shall cause message rejection next to parsing.
	 */
	public final static Impact FATAL = new Impact("FATAL","F");
	/**
	 * WARNING impact exception affecting a message; the message may in theory be processed
	 * entirely and even distributed to recipients, but with warnings.
	 */
	public final static Impact WARNING = new Impact("Warning","W");

//please preserve at all times the alignment between the table below and singletons hereabove
			/**
			 * May be used to iterate over all possible enumeration values.
			 */
	public final static Impact[] table = { 
		FATAL, WARNING };
		
	/**
	 * Provides a default string rendering with long name only.
	 */	
	public String toString() {
		return name ;
	};

	public String toCode() {
		return code ;
	};

	public boolean equals(Object obj) {
		if (obj.getClass().getName()!=this.getClass().getName()) return false;
		return (((Impact)obj).code.equals(this.code));
	}

}
