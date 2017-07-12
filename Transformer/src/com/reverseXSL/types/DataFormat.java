package com.reverseXSL.types;

import java.io.Serializable;

/**
 * DataFormat singletons indicate the format of message data (CSV, XML, free text, etc.). 
 * It is a strong typed enumeration class compatible with JDK1.4.
 * <p>
 * Valid enumeration values are all like <code>DataFormat.CODE</code> where CODE
 * must be a valid code; for the list of codes see {@link #table}.
 * </p>
 * @author bernardH
 *
 */
public final class DataFormat implements Serializable {
	
	private static final long serialVersionUID = 8344414652907959701L;
		
	final String code;
	final String info;
	
	// + + + + + + + + +  P R I V A T E   C O N S T R U C T O R   + + + + + + + + + + +
	
	private DataFormat (String code, String info) {
		this.code = code;
		this.info = info;
	}
	
	// + + + + + + F I N A L   S T A T I C   T Y P E   D E F I N I T I O N  + + + + + +
	
	/**
	 * DataFormat data can be anything; it is unspecified.
	 */
	public final static DataFormat ANY = new DataFormat("ANY", "-anything-");
	
	/**
	 * A standard IATA message for IMP, AHM, SSIM and other sub-brands, starting with a Type-B SITA header.
	 * The header itself may be pure ASCII, or native, or with the full set of control characters.
	 */
	public final static DataFormat IATA = new DataFormat("IATA", "IATA Cargo-IMP, SSIM, AHM, and other variants with a Type-B header");
	
	/**
	 * A text-oriented dataFormat with Comma Separated Values; also holds for ';', tabs and 
	 * other usual separators.
	 */
	public final static DataFormat CSV = new DataFormat("CSV", "Comma (or tab,semi-colon,...) Separated Values");
	
	/**
	 * Plain human readable text
	 */
	public final static DataFormat TEXT = new DataFormat("TEXT", "Plain Text");
	
	/**
	 * a valid XML document
	 */
	public final static DataFormat XML = new DataFormat("XML", "XML Document");
	
	
	/**
	 * An EDIFACT interchange as defined by standard ISO 9735 (made by United Nations Trade Facilitation Committee).
	 * <p>This same tag is used for various EDIFACT sub-brands:<ul>
	 * <li>syntax-level 3 and 4
	 * <li>The native EDIFACT format comprises only EDIFACT segments as defined in the proper Trade Data
	 * Interchange Directory (TDID), e.g. D96A, and complies with one of the official UN-EDIFACT standard
	 * messages. The IATA CARGOFACT messages belong to such category.
	 * <li>The IATA sub-brand defined in the CARGO IMP manual allows to wrap 
	 * a Cargo-IMP message (5 lines at a time) into FTX EDIFACT segments, these being placed inside a
	 * UNH~UNT message. 
	 * <li>The TRAXON Cargo Community System places the CARGO-IMP content directly inside the EDIFACT UNH~UNT message,
	 * CRLF included.
	 * </ul>
	 * All these variants are, at this stage, globally labelled 'EDIFACT'.
	 * </p>
	 */
	public final static DataFormat EDIFACT = new DataFormat("EDIFACT", "UN or IATA EDIFACT");

	
	/**
	 * The American National Standard ANSI X12, published by NIST.
	 */
	public final static DataFormat X12 = new DataFormat("X12", "ANSI X12");
	
	/**
	 * A Standard TRADACOMS message as edited by the UK Article Numbering Association.
	 */
	public final static DataFormat TRADACOMS = new DataFormat("TRADACOMS", "UK ANA TRADACOMS");

	/**
	 * A standard financial message as defined in the SWIFT FIN User Handbook.
	 */
	public final static DataFormat SWIFT = new DataFormat("SWIFT", "S.W.I.F.T. FIN User Message");

	/**
	 * A bilaterally agreed format, a corporate flat file.
	 */
	public final static DataFormat PROPRIETARY = new DataFormat("PROPRIETARY", "Custom or Proprietary Format");
	
	/**
	 * Full transparent binary content that must be preserved from any alteration
	 */
	public final static DataFormat BINARY = new DataFormat("BINARY", "Transparent Binary Data");
	
//please preserve at all times the alignment between the table below and singletons hereabove
	/**
	 * May be used to iterate over all possible enumeration values.
	 */
	public final static DataFormat[] table = { 
		ANY, IATA, CSV, TEXT, XML, EDIFACT, X12, TRADACOMS, SWIFT, PROPRIETARY, BINARY };
		
	// + + + + + + + + + + + + +  P U B L I C   M E T H O D S   + + + + + + + + + + + + +
	
	/**
	 * Provides a default string rendering with long name followed by (CODE) as
	 * would be used when using a member like <code>AddressType.CODE</code>.
	 */	
	public String toVerboseString() {
		return this.info + " (" + this.code + ")";
	};
	
	public String toString() {
		return this.code;
	};

	static public DataFormat find(String key) {
		for (int i=0;i<table.length;i++)
			if (table[i].code.equals(key)) return table[i];
		return ANY;
	}

	public boolean equals(Object obj) {
		if (obj.getClass().getName()!=this.getClass().getName()) return false;
		return (((DataFormat)obj).code.equals(this.code));
	}

}
