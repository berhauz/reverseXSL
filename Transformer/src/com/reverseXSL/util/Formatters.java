package com.reverseXSL.util;

import java.io.ByteArrayOutputStream;

/**
 * A collection of utility methods bound to formatting data.
 * 
 * @author bernardH
 *
 */
public class Formatters {

	/**
     * There a BIG issue with indentation according to the JVM version and/or preferred XML parser on the classpath. 
     * The following sprouted from an afternoon of despair. A last resort
     * method that is guaranteed to work... It took me less than 2 hours to build and test... if I knew, I
     * would have done earlier to save my time.<p>
     * I wanted a method that worked in all circumstances; the issue is actually with
     * space-only text nodes attached elsewhere in the XML document tree that reflect indentation
     * and line feeds. Not only these may eat a significant portion of your XML documents, but diverse
     * XSLT, DOM and SAX tools actually exhibit different behaviours with the result of scrambling the output.
     * I was unable to find a common compatible core.
	 * <p>BEWARE OF LIMITATIONS: The formatting utility doesn't work on XML documents that
	 * mix non-blank text nodes with child elements within the same parent element... 
	 * kind of bad practice indeed but actually fully allowed by the XML standards.
	 * <p><b>
	 * THEREFORE, PLEASE ENJOY THIS FACILITY ONLY FOR TESTING AND CONTINUE FEEDING
	 * LOCAL APPLICATIONS WITH POTENTIALLY UGLY XMLs. THEY DON'T CARE LIKE OUR EYES DO.
	 * </b>
	 *  
	 * @param uglyXML whatever raw output that is anyhow a valid XML document
	 * @param indentPattern try for instance "  |" and see the magic!
	 * @return nicely indented XML as String
	 */
	public static String niceXML(StringBuffer uglyXML,String indentPattern,String EOL) {
    	StringBuffer sb = uglyXML; //shorter notation
    	int l = sb.length();
    	StringBuffer out = new StringBuffer(l);
    	int from, to, idx = 0;
    	//we assume the XML well-formed indeed!
    	//copy xml version
    	from = sb.indexOf("<?");
    	if (from>=0) { 
    		to = sb.indexOf("?>");
    		if (to>from && to-from<100 && from<100) {
    			out.append(sb.substring(from,to+2));
    			idx = to+2; 
    		}
    		else {//possibly not XML
    			return uglyXML.toString();
    		}
    	} //ELSE possibly not XML, or just no <?xml processing instruction, so we will check further
    	//advance to next '<', shall be root element start
    	idx = sb.indexOf("<",idx);
    	if (idx<0 || idx>100 || sb.indexOf(">",idx+1)<0)
    		//likely not XML!
    		return uglyXML.toString();
    	formatElement(sb,idx,EOL,indentPattern,out);
    	
		return out.toString();
	}
	
	/**
	 * Formats a single XML element and invokes formatElement() on children (recursive!).
	 * 
	 * @param s 		source string buffer with UNformatted XML document
	 * @param atIndex 	positionned on the next element to format
	 * @param indent	current indentation tag
	 * @param IndentBit	indentation increment
	 * @param out		the string buffer containing the formatted output
	 * @return			the index just next to the formatted element within source buffer
	 */
	static int formatElement (StringBuffer s,int atIndex, String indent, String indentBit, StringBuffer out) {
		int idx, next_lt, from = atIndex;
		char c;
		next_lt = s.indexOf("<",from+1);
		//skip element Qname
		idx=from+1; 
		for (c = s.charAt(idx); !isSpace(c) && c!='/' && c !='>' && idx<=next_lt; c = s.charAt(++idx)) ;
		//skip attributes
		while (c!='/' && c!='>' && idx<=next_lt) {
			//skip spaces
			for (c = s.charAt(idx); isSpace(c) && idx<next_lt ; c = s.charAt(++idx)) ;
			//skip attribute Qname if any
			for (c = s.charAt(idx); c!='=' && !isSpace(c) && c!='/' && c !='>' && idx<next_lt; c = s.charAt(++idx)) ;
			//skip spaces and =
			for (c = s.charAt(idx); (isSpace(c) || c=='=') && idx<next_lt ; c = s.charAt(++idx)) ;
			//skip "value" or 'value'
			if (c=='"') idx = s.indexOf("\"",idx+1);
			else if (c=='\'') idx = s.indexOf("'",idx+1);
			if (idx<from) { 
				out.append("ERROR IN XML DOCUMENT SYNTAX NEARBY ..." + s.substring(from,Math.max(next_lt,from+25))+"...");
				return s.length();
			}
			c = s.charAt(++idx);
		}
		if (c=='>') {
			//output element tag
			out.append(indent+s.substring(from,++idx));
			//check for data or child elements
			int childCnt = 0;
			if (s.charAt(next_lt+1)=='/') {
				//no child, preserve data
				if (next_lt>idx) out.append(s.substring(idx,next_lt));
			} else {
				//has child (mixed text data will be ignored)
				//loop on childs
				while (s.charAt(next_lt+1)!='/') {
					idx = formatElement(s,next_lt,indent+indentBit,indentBit, out);
					childCnt++;
					//skip following spaces
					next_lt = s.indexOf("<",idx);
				}
			}
			// // at this point we have the next_lt index on a "</"
			//we shall add the closing tag
			//skip element Qname 
			from = next_lt;
			idx = s.indexOf(">",next_lt+2);
			if (idx<=from+2) { 
				out.append("ERROR IN XML DOCUMENT SYNTAX NEARBY ..." + s.substring(from,Math.min(from+25,s.length()))+"...");
				return s.length();
			}
			out.append((childCnt>0?indent:"")+s.substring(from,idx+1));
			return idx+1;
		} else
		if (c=='/' && s.charAt(idx+1)=='>') {
			//output simple element
			out.append(s.substring(from,idx+2));
			return idx+2;
		}
		out.append("ERROR IN XML DOCUMENT SYNTAX NEARBY ..." + s.substring(from,Math.max(next_lt,from+25))+"...");
		return s.length();
	}
	
	static boolean isSpace(char c) {
		return (c==' ' || c=='\t' || c=='\r' || c=='\n');
	}
	
	/**
	 * Utility method to hex-encode a byte array so that it becomes XML-safe and readable whatever
	 * the enclosed byte values. Works like 'quoted-printable' because most printable characters
	 * are not encoded.
	 * <p>
	 * NOTE: I have not found a proper XML character escaping method
	 * so here is one custom-developed based on a bare conversion table (fastest access)
	 * taking care of '&' , '>' , and '<' (for XML safeness) and all non-printable 7bit ASCII.
	 * Magic is, most EDI messages would just look like they are really (but for CR replaced by %0D)
	 * </p>
	 * @param bytes 	the binary (byte array) version of the data to encode
	 * @param start		the starting offset in the byte array
	 * @param length	the number of bytes to hex-encode. If start+length is greater than the size of 
	 * the byte array, an array out of bounds exception is thrown as one shall expect.
	 * @return null if arg is null, else the hex-encoded string
	 */
	public static String hexEscape(byte[] bytes,int start, int length) {

		final String[] conversionTable = { 
			"%00","%01","%02","%03","%04","%05","%06","%07",
			"%08","\t","\n","%0B","%0C","%0D","%0E","%0F",
			"%10","%11","%12","%13","%14","%15","%16","%17",
			"%18","%19","%1A","%1B","%1C","%1D","%1E","%1F",
			" ","!","\"","#","$","%%","%26","'",
			"(",")","*","+",",","-",".","/",
			"0","1","2","3","4","5","6","7",
			"8","9",":",";","%3C","=","%3E","?",
			"@","A","B","C","D","E","F","G",
			"H","I","J","K","L","M","N","O",
			"P","Q","R","S","T","U","V","W",
			"X","Y","Z","[","\\","]","^","_",
			"`","a","b","c","d","e","f","g",
			"h","i","j","k","l","m","n","o",
			"p","q","r","s","t","u","v","w",
			"x","y","z","{","|","}","~","%7F",
			"%80","%81","%82","%83","%84","%85","%86","%87",
			"%88","%89","%8A","%8B","%8C","%8D","%8E","%8F",
			"%90","%91","%92","%93","%94","%95","%96","%97",
			"%98","%99","%9A","%9B","%9C","%9D","%9E","%9F",
			"%A0","%A1","%A2","%A3","%A4","%A5","%A6","%A7",
			"%A8","%A9","%AA","%AB","%AC","%AD","%AE","%AF",
			"%B0","%B1","%B2","%B3","%B4","%B5","%B6","%B7",
			"%B8","%B9","%BA","%BB","%BC","%BD","%BE","%BF",
			"%C0","%C1","%C2","%C3","%C4","%C5","%C6","%C7",
			"%C8","%C9","%CA","%CB","%CC","%CD","%CE","%CF",
			"%D0","%D1","%D2","%D3","%D4","%D5","%D6","%D7",
			"%D8","%D9","%DA","%DB","%DC","%DD","%DE","%DF",
			"%E0","%E1","%E2","%E3","%E4","%E5","%E6","%E7",
			"%E8","%E9","%EA","%EB","%EC","%ED","%EE","%EF",
			"%F0","%F1","%F2","%F3","%F4","%F5","%F6","%F7",
			"%F8","%F9","%FA","%FB","%FC","%FD","%FE","%FF" }; 
			//NOTE: a byte is a SIGNED integer from -128 to +127!!
		if (bytes==null) return null;
		StringBuffer sb = new StringBuffer(length * 3); //max size multiplier of initial nb of bytes
		for (int i=start; i<start+length;i++)
			sb.append(conversionTable[(bytes[i]>=0?bytes[i]:(256+bytes[i]))]);
		return sb.toString();
	}
	
	/**
	 * Inverse of {@link #hexEscape(byte[], int, int)}.
	 * 
	 * @param s	the string to decode into a byte array
	 * @return a byte array of the original binary data
	 */
	public static byte[] hexUnEscape(String s) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(s.length());
		char[] chars = new char[s.length()];
		s.getChars(0, s.length(), chars, 0);
		
		for (int i=0;i<s.length();i++)
			switch (chars[i]) {
			case ('%'): {
				i++;
				if (chars[i]=='%') baos.write('%');
				else {
					int j = (chars[i]<65?chars[i]-48:chars[i]-55)*16;
					i++;
					j+=(chars[i]<65?chars[i]-48:chars[i]-55);
					baos.write((j<128?j:j-256));
				}
				break; 
			}
			default: {
				baos.write(chars[i]);
				break;
			}
			}
		return (baos.toByteArray());

	}

}
