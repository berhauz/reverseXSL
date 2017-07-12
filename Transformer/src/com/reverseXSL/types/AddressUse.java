package com.reverseXSL.types;

import java.io.Serializable;

/**
 * AddressUse singletons refine the semantic of a recipient address (TO,CC,SENTTO, etc.). 
 * It is a strong typed enumeration class compatible with JDK1.4.
 * <p>
 * The AddressUse tag may impact routing decisions.
 * <p>
 * Valid enumeration values are all like <code>AddressUse.CODE</code>. 
 * For the list of valid codes, see {@link #table}.
 * <p>
 * There are associated Junit test methods.
 * 
 * @author bernardH
 *
 */
public final class AddressUse implements Serializable {

	//	required for passing as argument to bean's business methods
	private static final long serialVersionUID = -8517269681323243821L;

	final String name;
	final String code;
	
	private AddressUse (String n1, String c1) {
		name = n1; code = c1;
	}
	

	/**
	 * The marked Address shall be ignored
	 */
	public final static AddressUse IGNORE = new AddressUse("-ignore this address-","IGNORE");
	/**
	 * A Generic Recipient; most simple recipient kind, no distinction made between
	 * primary or carbon copy or else; just recipient.
	 */
	public final static AddressUse GR = new AddressUse("Generic Recipient","GR");
	/**
	 * The message originator
	 */
	public final static AddressUse FROM = new AddressUse("Originator","FROM");
	/**
	 * Primary Recipient (for action)
	 */
	public final static AddressUse TO = new AddressUse("Primary Recipient","TO");
	/**
	 * Carbon Copy Recipient (for info)
	 */
	public final static AddressUse CC = new AddressUse("Carbon Copy Recipient","CC");
	/**
	 * Blind Carbon Copy Recipient (for info, unknown to other recipients)
	 */
	public final static AddressUse BCC = new AddressUse("Blind Copy Recipient","BCC");
	/**
	 * Recipient extracted from the SENT-> line at the end of a IATA message
	 */
	public final static AddressUse SENT = new AddressUse("Recipient in SENT-> line","SENT");
	/**
	 * A Group 1 Recipient, i.e. extracted from (or to locate within) the first 64 address block 
	 * in an extended IATA message.
	 */
	public final static AddressUse G1 = new AddressUse("IATA header main address block","G1");
	/**
	 * A Group 2 Recipient, i.e. extracted from (or to locate within) the second 64 address block 
	 * in an extended IATA message.
	 */
	public final static AddressUse G2 = new AddressUse("Extended IATA header, second address block","G2");
	/**
	 * Specific marker for a single Address to interpret as a Distribution List
	 */
	public final static AddressUse DL = new AddressUse("Distribution List","DL");
	/**
	 * Double Signature marker (who pays the bill) as defined by SITA and 
	 * found within the '.'originator reference field in a Type-B message
	 */
	public final static AddressUse DS = new AddressUse("Double Signature","DS");
	/**
	 * The Reply-to or say Return Address
	 */
	public final static AddressUse REPLY = new AddressUse("Reply-To or Return Address","REPLY");
			
//please preserve at all times the alignment between the table below and singletons hereabove
			/**
			 * May be used to iterate over all possible enumeration values.
			 */
	public final static AddressUse[] table = { 
		IGNORE, GR, FROM, TO, CC, BCC, SENT, G1, G2, DL, DS, REPLY };
		
	/**
	 * Provides a default string rendering with long name followed by (CODE) as
	 * would be used when using a member like <code>AddressUse.CODE</code>.
	 */	
	public String toString() {
		return name + " ("+code+")";
	};

	static public AddressUse find(String key) {
		for (int i=0;i<table.length;i++)
			if (table[i].code.equals(key)) return table[i];
		return IGNORE;
	}
	
	public boolean equals(Object obj) {
		if (obj.getClass().getName()!=this.getClass().getName()) return false;
		return (((AddressUse)obj).code.equals(this.code));
	}

}
