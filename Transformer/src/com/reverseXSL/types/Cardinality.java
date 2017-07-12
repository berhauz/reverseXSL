package com.reverseXSL.types;

import java.io.Serializable;



/**
 * Marks the <u>Cardinality</u> of a group, segment or data element: Mandatory, Optional
 * or Conditional.
 * It is a strong typed enumeration class backward compatible with JDK1.4.
 * <p>
 * Valid enumeration values are all like <code>Cardinality.CODE</code> where CODE
 * must be a valid code; for the list of codes see {@link #table}.
 * 
 * @author bernardH
 */
public final class Cardinality implements Serializable {

	private static final long serialVersionUID = 981753478079913544L;

	//private String name;
	private String code;
	
	private Cardinality (String n1, String c1) {
		//name = n1; 
		code = c1;
	}
	
	
	/**
	 * Marks a Mandatory group, segment or data element. Must always be
	 * present in a message instance.
	 */
	public final static Cardinality MANDATORY = new Cardinality("Mandatory","M");

	/**
	 * Marks an Optional group, segment or data element. May or may not exist
	 * in a message instance independently from all other 
	 * groups, segments or data elements.
	 */
	public final static Cardinality OPTIONAL = new Cardinality("Optional","O");

	/**
	 * Marks a Conditional group, segment or data element; its presence 
	 * in the message depends from the existence (or values) of other 
	 * groups, segments or data elements. The dependency is expressed 
	 * in a condition.
	 * (see CONDDefinition)
	 */
	public final static Cardinality CONDITIONAL = new Cardinality("Conditional","C");

//please preserve at all times the alignment between the table below and singletons hereabove
	/**
	 * May be used to iterate over all possible enumeration values.
	 */
	public final static Cardinality[] table = { 
		MANDATORY, OPTIONAL, CONDITIONAL };
		
	/**
	 * Provides a default string rendering with code only.
	 */	
	public String toString() {
		return code ;
	};

	public boolean equals(Object obj) {
		if (obj.getClass().getName()!=this.getClass().getName()) return false;
		return (((Cardinality)obj).code.equals(this.code));
	}

}
