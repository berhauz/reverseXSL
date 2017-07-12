package com.reverseXSL.types;

/**
 * To indicate the expected <u>Handling</u> of parser exceptions: 
 * Record or Throw the exception.
 * It is a strong typed enumeration class compatible with JDK1.4.
 * <p>
 * Valid enumeration values are all like <code>Handling.CODE</code>. 
 * For the list of valid codes, see {@link #table}.
 * 
 * @author bernardH
 */
public final class Handling {

	private String name;
	private String code;
	
	private Handling (String n1, String c1) {
		name = n1; 
		code = c1;
	}
	
	
	/**
	 * Instructs to Record parser exceptions.
	 */
	public final static Handling RECORD = new Handling("Record","R");

	/**
	 * Instructs to Throw parser exceptions.
	 */
	public final static Handling THROW = new Handling("Throw","T");


//please preserve at all times the alignment between the table below and singletons hereabove
	/**
	 * May be used to iterate over all possible enumeration values.
	 */
	public final static Handling[] table = { 
		RECORD, THROW };
		
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
		return (((Handling)obj).code.equals(this.code));
	}

}
