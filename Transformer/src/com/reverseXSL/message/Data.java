package com.reverseXSL.message;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.reverseXSL.types.DataFormat;

/**
 * Wraps byte-oriented collections or structures into an object enriched with numerous 
 * methods capable of normalizing 
 * interchange data.</p>
 * <p>The original data piece (e.g. a ByteBuffer) is just wrapped, and not 
 * cloned nor copied. Therefore any later change to the argument data piece may geopardize operations.
 * <p>
 * Raw data received via some communication channel or read from various media is often 
 * affected by additional
 * control characters, spurious record delimiters or other 'pollution' of the 
 * canonical formats required for fully automated processing. This Data class is used to wrap such raw data and
 * yield clean, de-polluted, streams of bytes or characters for message processing.
 * </p>
 * <p>NOTE: full use of this class is for future functional extensions of the reverseXSL software.</p> 
 * 
 * @author bernardH
 *
 */
public class Data {

	private static final int MSG_IDENTIFICATION_RANGE = 100; //tells how much bytes to look-up for an identification pattern

	private ByteBuffer buffer = null;
	private Charset charset = Charset.forName( "UTF-8" );
	private DataFormat dataformat = DataFormat.ANY;
	
	/**
	 * Instantiate a Data object from a byte buffer, assuming UTF-8 as charset
	 * for character oriented operations on this data.
	 * 
	 * @param bb the byte buffer wrapped as Data, which is rewound but NOT copied (later changes to the 
	 * ByteBuffer may adversely this Data)
	 */
	public Data(ByteBuffer bb) {
		buffer = bb==null? ByteBuffer.allocate(0):bb;
		bb.rewind();
	}
	
	
	/**
	 * Instantiate a Data object from a byte buffer, with the explicit charset
	 * that must be assumed for character oriented operations on this data. 
	 * 
	 * @param bb the byte buffer wrapped as Data, which is rewound but NOT copied (later changes to the 
	 * ByteBuffer may adversely  this Data)
	 * @param cs if null defaults back to UTF-8
	 */
	public Data(ByteBuffer bb,Charset cs) {
		buffer = bb;
		charset = cs==null?Charset.forName("UTF-8"):cs;
		identify();
	}

	/**
	 * Instantiate a Data object from a byte array, assuming UTF-8 as charset
	 * for character oriented operations on this data.
	 *
	 * @param ba the byte array wrapped as Data, which is NOT copied (later changes to the 
	 * byte array may adversely affect this Data)
	 */
	public Data(byte[] ba) {
		buffer = ByteBuffer.wrap(ba==null?new byte[0]:ba);
		identify();
	}

	/**
	 * Instantiate a Data object from a byte buffer, with the explicit charset
	 * that must be assumed for character oriented operations on this data. 
	 * 
	 * @param ba the byte array wrapped as Data, which is NOT copied (later changes to the 
	 * byte array may adversely affect this Data)
	 * @param cs if null defaults back to UTF-8
	 */
	public Data(byte[] ba,Charset cs) {
		buffer = ByteBuffer.wrap(ba==null?new byte[0]:ba);
		charset = cs==null?Charset.forName("UTF-8"):cs;
		identify();
	}

	/**
	 * Instantiate a Data object from a byte buffer, with the explicit charset
	 * that must be assumed for character oriented operations on this data. 
	 * 
	 * @param inS	the source of byte-oriented data
	 * @param cs	if null defaults back to UTF-8
	 * @throws IOException as would result from read errors from the argument input stream
	 */
	public Data(InputStream inS, Charset cs) throws IOException {
		//don't check args, let a null input stream cause exceptions
		byte[] ba = new byte[inS.available()];
		//initial buffer
		buffer = null;
		int nbin;
		do {
			nbin = inS.read(ba);
			if (nbin >0) {
				if (buffer==null) { 
					buffer = ByteBuffer.allocate(nbin);
					
				} else {
					//increase the buffer
					ByteBuffer bb = ByteBuffer.allocate(buffer.limit()+nbin);
					bb.put(buffer);
					buffer = bb;
				}
				buffer.put(ba,0,nbin);
				buffer.flip();
			}
		} while (nbin >0);
		charset = cs==null?Charset.forName("UTF-8"):cs;
		identify();
	}
	
	//Data cleansing facilities
	
	/**
	 * arg for {@link #getConvertedData(int)} : case of no conversion requested.
	 */
	public static final int _NONE = 0;
	
	
	
	
	/**
	 * arg for {@link #getConvertedData(int)} : convert standalone LF's to CRLF's, 
	 * and preserve existing CRLF's.
	 */
	public static final int _ToCRLF = 1;
	
	
	
	
	/**
	 * arg for {@link #getConvertedData(int)}  : remove CR's.
	 */
	public static final int _ToLF = 2;
	
	
	
	
	/**
	 * arg for {@link #getConvertedData(int)} : suppress trailing empty lines 
	 * and ensure that the very last data line bears a single line terminator. 
	 * The line terminator is either CR or CRLF according to other conversions or 
	 * the existing data contents (by default).
	 */
	public static final int _1NewLineAtEnd = 4;
	
	
	
	/**
	 * arg for {@link #getConvertedData(int)} : convert all characters to their 
	 * uppercase equivalents (based on built-in java String methods).
	 */
	public static final int _ToUPPER = 8;
	
	
	
	/**
	 * arg for {@link #getConvertedData(int)} : control characters (value<32) 
	 * are discarded except for tabs, carriage returns and line feeds. 
	 * Character values above 127 are replaced by a '?'.
	 * <p>Note that this method operates on characters, not bytes, and thus also properly replaces 
	 * all multibyte characters (whose Unicode values are always >127) with a single '?'.
	 */
	public static final int _ASCII7bits = 16;

	
	
	/**
	 * arg for {@link #getConvertedData(int)} : Trim 
	 * Non-Breaking SPaces (i.e. space chars and tabs) at the <u>beginning and end</u> of each line in the message body.
	 * <p>Note that if you combine this trim operation with {@link #_NoCRLFBytes} you will only trim NBSP leading and
	 * trailing the entire data because _NoCRLFBytes transforms the whole data into a single big line first!</p>
	 */
	public static final int _TrimNBSP = 32;


	/**
	 * arg for {@link #getConvertedData(int)} : control BYTES (value<32) 
	 * are discarded except for tabs, carriage returns and line feeds. 
	 * 8-bit values (above 127) are preserved.
	 * <p>
	 * This is a byte-oriented method, applied <b>before</b> decoding bytes into characters!
	 * </p>
	 * <p>Compared with {@link #_NoCtrlChars}, the supporting function operates 
	 * on bytes and not characters, and thus may discard bytes
	 * actually belonging to multibyte character encodings</b>, thus scrambling the original data!. 
	 * <p>However,
	 * <ul><li>this potential side effect will not affect UTF-8 encodings because all multibyte values in UTF-8
	 * are over 128 (the most significant bit is always 1 by construction)</li>
	 * <li>UTF-16 encodings (miss-named 'Unicode' versus UTF-8 in MS-Windows) will notably be scrambled.</li>
	 * </ul>
	 * The function is peculiarly useful whenever legacy single-byte character codings are expected (e.g. ISO-8859) and must
	 * be de-polluted.
	 * 
	 */
	public static final int _NoCtrlBytes = 64; 

	
	
	/**
	 * arg for {@link #getConvertedData(int)} : suppress all CR's and LF's.
	 * In other words, consider the original data as a very long line.
	 * <p>
	 * This is a byte-oriented method, applied before decoding bytes into characters!
	 * </p>
	 * It is most useful when added to {@link #_NoCtrlBytes} in which case only tab characters (with a value <32)
	 * are preserved.
	 */
	public static final int _NoCRLFBytes = 128;

	
	
	/**
	 * arg for {@link #getConvertedData(int)} : suppress blank lines everywhere
	 * in the original data. Precisely, both the
	 * true empty lines and those containing only spaces or tabs are removed.
	 * <p>
	 * This is a byte-oriented method, applied before decoding bytes into characters!
	 * </p>
	 */
	public static final int _NoBlankLine = 256;

	
	
	/**
	 * arg for {@link #getConvertedData(int)} : control characters (value<32) 
	 * are discarded except for tabs, carriage returns and line feeds. 
	 * All other character values are preserved.  
	 * <p>Compared with {@link #_NoCtrlBytes}, the supporting function preserves
	 * character values that would be encoded as 8bit values in 
	 * ISO-8859, and all multibyte characters in
	 * UTF-16 Unicode Transformation Formats.
	 */
	public static final int _NoCtrlChars = 512; 

	
	/**
	 * arg for {@link #getConvertedData(int)} : IATA PSCRM messages still generated by older systems can enforce
	 * the 69 chars limit of the even older TELEX transmission system by cutting lines in the middle of remarks
	 * elements, e.g. <blockquote>1BIMMEL/LMRS-BV2 .L/272397 .R/TOP KRSV .R/CKIN HK1 1BAG 05KG-1BIMMEL/LMRS
</blockquote> becomes:<blockquote>1BIMMEL/LMRS-BV2 .L/272397 .R/TOP KRSV .R/CKIN HK1 1BAG<br>
<b>.RN/</b>05KG-1BIMMEL/LMRS</blockquote>
	 * thus breaking the .R/CKIN check-in luggage segment that normally runs to the end of line.
	 * This flag restores the canonical long line.</p>
	 */
	public static final int _UnfoldPSCRMRemarks = 1024;


	/**
	 * utility method to convert named conversion tokens into the corresponding
	 * conversion token value. This method is the inverse of {@link #namedTokens(int)}
	 * @param opt the named value
	 * @return the matching integer value, -1 if not found
	 */
	public static final int tokenValue(String opt) {
		if (opt==null || opt.length()<=0) return -1;
		int tokVal = 0;
		opt = opt.toUpperCase();
		if (opt.equals("NONE"))
			tokVal += Data._NONE;
		else if (opt.equals("TOCRLF"))
			tokVal += Data._ToCRLF;
		else if (opt.equals("TOLF"))
			tokVal += Data._ToLF;
		else if (opt.equals("TOUPPER"))
			tokVal += Data._ToUPPER;
		else if (opt.equals("FULLYTRIMMED"))
			tokVal += Data._NoBlankLine + Data._TrimNBSP
					+ Data._NoCtrlChars;
		else if (opt.equals("UNFOLDPSCRMREMARKS"))
			tokVal += Data._UnfoldPSCRMRemarks;
		else return -1;
		return tokVal;
	}

	/**
	 * utility method to convert a conversion value into the corresponding list of named conversion tokens
	 * conversion token value. This method is the inverse of {@link #tokenValue(String)}
	 * @param opt the named value
	 * @return the matching integer value, -1 if not found
	 */
	public static final String namedTokens(int conversions) {
		if (conversions<=0) return "NONE";
		boolean CRLFconv = ((conversions % 2) == 1);
		boolean LFconv = ((conversions % 4) >= 2);
		boolean oneNewLineAtEnd = ((conversions % 8) >=4);
		boolean UPPERconv = (conversions % 16) >= 8;
		boolean sevenBitsConv = (conversions % 32) >= 16;
		boolean trimNBSP = (conversions % 64) >= 32;
		boolean no_ctrl_bytes = (conversions % 128) >= 64; //byte-oriented
		boolean no_crlf_bytes = (conversions % 256) >= 128; //byte-oriented
		
		boolean no_blank_line = (conversions % 512) >= 256; //byte-oriented
		boolean noCtrlChars = (conversions % 1024) >= 512;
		
		boolean unfoldPSCRMRemarks = (conversions % 2048) >= 1024;

		StringBuffer sb = new StringBuffer("");
		if (no_blank_line&&trimNBSP&&noCtrlChars) sb.append("FULLYTRIMMED+");
		else sb.append((no_blank_line?"NoBlankLine+":"")+(trimNBSP?"TrimNBSP+":"")+(noCtrlChars?"NoCtrlChars+":""));
		if (no_ctrl_bytes) sb.append("NoCtrlBytes+");
		if (no_crlf_bytes) sb.append("NoCRLFBytes+");
		if (sevenBitsConv) sb.append("SevenBits+");
		if (UPPERconv) sb.append("ToUPPER+");
		if (oneNewLineAtEnd) sb.append("1NewLineAtEnd+");
		if (LFconv) sb.append("ToLF+");
		if (CRLFconv) sb.append("ToCRLF+");
		if (unfoldPSCRMRemarks) sb.append("UnfoldPSCRMRemarks+");
		
		return sb.substring(0, Math.max(sb.length()-1,0)).toString();
	}

	

	/**
	 * Converting the Data bytes to Characters while at the same time filtering and normalizing data.
	 * <p>
	 * The character set specified at instantiation (or default UTF-8) is used to interpret bytes into
	 * characters.
	 * </p>
	 * @param conversions 	either the value _NONE, else the addition of one or 
	 * more of the constants _ToCRLF, _ToLF, _1NewLineAtEnd, _ToUPPER, _ASCII7bits, _TrimNBSP
	 * _NoCtrlBytes, _NoCRLFBytes, _NoBlankLine, _NoCtrlChars. 
	 * 
	 * @return string buffer
	 */
	public StringBuffer getConvertedData(int conversions) {

		boolean CRLFconv = ((conversions % 2) == 1);
		boolean LFconv = ((conversions % 4) >= 2);
		boolean oneNewLineAtEnd = ((conversions % 8) >=4);
		boolean UPPERconv = (conversions % 16) >= 8;
		boolean sevenBitsConv = (conversions % 32) >= 16;
		boolean trimNBSP = (conversions % 64) >= 32;
		boolean no_ctrl_bytes = (conversions % 128) >= 64; //byte-oriented
		boolean no_crlf_bytes = (conversions % 256) >= 128; //byte-oriented
		boolean no_blank_line = (conversions % 512) >= 256; //byte-oriented
		boolean noCtrlChars = (conversions % 1024) >= 512;
		boolean unfoldPSCRMRemarks = (conversions % 2048) >= 1024;

        // We start with BYTE-wise operations
		// ==================================
		buffer.rewind();
		byte[] ba = buffer.array(); //which can be longer than the actual buffer limit!!!
		ByteBuffer bb = ByteBuffer.allocate( buffer.limit() );
        boolean is_empty = true; //at the beginning of every new line (i.e. after a LF)
        for (int i = 0; i < buffer.limit(); i++) {
            // line terminators
            if (ba[i] == 0x0D || ba[i] == 0x0A) {
                if (no_crlf_bytes || no_blank_line && is_empty) {
                    continue;
                } else {
                    bb.put( ba[i] );
                    if (ba[i] == 0x0A) {
                        is_empty = true;
                    }
                    continue;
                }
            }
            // CTRL chars but tab
            if (ba[i] >= 0 && ba[i] < ' ' && ba[i] != '\t') {
                if (no_ctrl_bytes) {
                    continue;
                } else {
                    bb.put( ba[i] );
                    is_empty = false;
                    continue;
                }
            }
            // printables
            if (ba[i] > ' ' || ba[i] < 0) {
                bb.put( ba[i] );
                is_empty = false;
                continue;
            }
            // space chars and tabs
            if (ba[i] == ' ' || ba[i] == '\t') {
                // fix is_empty if there's a printable ahead (up to LF) to prevent suppressing leading space chars
                int j = i;
            	for (; is_empty && j < buffer.limit(); j++) {
                    if (ba[j] > ' ' || ba[j] < 0) {
                    	//we have printables
                        is_empty = false;
                    }
                    if (!no_ctrl_bytes && ba[i] >= 0 && ba[i] < ' ' && ba[i] != '\t') {
                    	//We have CTRL bytes to preserve
                    	is_empty = false;
                    }
                    if (ba[j] == 0x0A) {
                        break;
                    }
                }
            	//at this point, if is_empty is still true, j points on the LF of an empty line, else to the limit
                if (no_blank_line && is_empty) {
                	i=j;
                    continue;
                }
                bb.put( ba[i] );
            }
        }
        bb.flip();

        //at this point we have a modified ByteBuffer bb with the buffer limit telling its actual size
        
        // we switch now to CHARACTER-oriented operations
        // ==============================================
		
		StringBuffer rawsb = new StringBuffer(charset.decode(bb).toString());
		StringBuffer sb = new StringBuffer((int)(bb.limit()*1.1));
		
		if ((conversions & ~_NoBlankLine & ~_NoCRLFBytes & ~_NoCtrlBytes)==0 ) return rawsb;
		
		char c,x;
		boolean foundCR = false;
		boolean trimIt = trimNBSP;
		
		for (int i=0; i<rawsb.length();i++) {
			c = rawsb.charAt(i);
			
			if (trimIt && (c=='\t' || c==' ')) continue;
			
			if (c=='\r' || c=='\n') {
				// > TRIM handling: 
				//reset TRIM state so that next line will be trimmed as needed
				trimIt = trimNBSP; 
				//backtrack all trailing space and tab chars
				int j = sb.length()-1;
				while(trimIt && j>=0) {
					x = sb.charAt(j);
					if (x!='\t' && x!=' ') break;
					j--;
				}
				//truncate NBSP chars at end of line
				if ((j+1)<sb.length()) sb.delete(j+1, sb.length());
			}
			
			if (c=='\r')
				if (CRLFconv||LFconv) continue;
				else {
					foundCR = true;
					sb.append('\r');
					continue;
				}

			if (c=='\n')
				// see if we have to unfold a PSCRM remark extension, i.e. '[<cr>]<lf>.RN/<string remark continuation>'
				if (unfoldPSCRMRemarks && (i+5)<rawsb.length() &&
						rawsb.charAt(i+1)=='.' && rawsb.charAt(i+2)=='R' && rawsb.charAt(i+3)=='N' && rawsb.charAt(i+4)=='/' ) {
					// replace '[<cr>]<lf>.RN/' by a single space char
					// backtrack all <cr>'s
					int j = sb.length()-1;
					while(foundCR && j>=0) {
						x = sb.charAt(j);
						if (x!='\r') break;
						j--;
					} // j points on the last non-<cr> char in sb
					//truncate <CR> chars at end of output buffer
					if ((j+1)<sb.length()) sb.delete(j+1, sb.length());
					// append a single space char
					sb.append(' ');
					// skip the remark extension tag
					i = i+4;
					// Now re-insert the new line in front of the previous ' .R/x ' element:
					j = sb.length()-5;
					// look max 60 chars backward to find a ' .R/' sequence
					while(j>=0 && sb.length()-j<=60) {
						if (sb.charAt(j)==' ' && sb.charAt(j+1)=='.' && sb.charAt(j+2)=='R' && sb.charAt(j+3)=='/' ) {
							// previous remark element found, insert [<cr>]<lf>
							sb.setCharAt(j, '\n');
							if (CRLFconv || (!LFconv && foundCR) ) sb.insert(j, '\r');
							// remove space chars before the new [<cr>]<lf> sequence
							for (int k=j-1; k>0 && sb.charAt(k)==' ';k--) sb.deleteCharAt(k);
							break;
						}  
						j--;
					} 
					continue;
				} else
				// other end of line normalising
				if (CRLFconv) {sb.append("\r\n"); continue; }
				else if (LFconv) {sb.append(c); continue; }
				// no normalising: just echo the input to the output
				else { sb.append(c); continue; }
			
			trimIt = false;
			
			if (UPPERconv) c = Character.toUpperCase(c);
			if (sevenBitsConv && (c>=127)) { sb.append('?'); continue;}
			if ((noCtrlChars||sevenBitsConv) && (c=='\t')) { sb.append(c); continue;}
			if ((noCtrlChars||sevenBitsConv) && (c<32)) continue;
			sb.append(c);
		}
		
		if (oneNewLineAtEnd) {
			int i = sb.length()-1;
			int lastLFindex = -1;
			//trace back to the last printable
			while(i>=0) {
				c = sb.charAt(i);
				if (c=='\n') lastLFindex=i;
				if (c>32) break;
				i--;
			}
			if (lastLFindex<0) { 
				//no terminating LF found, add one
				//any set of trailing empty lines to delete first?
				if ((i+1)<sb.length()) sb.delete(i+1, sb.length());
				if (CRLFconv||foundCR) sb.append("\r\n");
				else  sb.append('\n'); 
			} else {
				//keep up to the last LF found
				//anything left to delete?
				if ((lastLFindex+1)<sb.length()) sb.delete(lastLFindex+1, sb.length());
			}
		}
		return sb;
	}

	public byte[] getBytes() {
		buffer.rewind();
		byte[] ba = new byte[buffer.limit()]; 
		System.arraycopy(buffer.array(),0,ba,0,ba.length); //the backing array may actually be longer than the present limit		
		return ba;
	}

	/**
	 * Get the backing byte array. Note that its size is often greater than the actual data.
	 * 
	 * @return backing array of bytes.
	 */
	public byte[] getArray() {
		buffer.rewind();
		return buffer.array();
	}

	/**
	 * get the actual data length.
	 * 
	 * @return lentgh in bytes, as integer, i.e. up to 4Gbytes
	 */
	public int length() {
		return buffer.limit();
	}
	
	/**
	 * Get the data format type.
	 * 
	 * @return one of ANY, IATA, CSV, TEXT, XML, EDIFACT, X12, TRADACOMS, SWIFT, PROPRIETARY, BINARY
	 */
	public DataFormat getFormat() {
		return dataformat;
	}
	
	/**
	 * Inspect data and set the data format type.
	 * 
	 * @return one of ANY, IATA, CSV, TEXT, XML, EDIFACT, X12, TRADACOMS, SWIFT, PROPRIETARY, BINARY
	 */
	public DataFormat identify() {
		//conventionally take the first N bytes as range for data format identification patterns
		buffer.rewind();
		int limit = buffer.limit();
		buffer.limit(Math.min(limit, MSG_IDENTIFICATION_RANGE)); //force subset into next decoding
		String msgChunk = charset.decode(buffer).toString();
		dataformat = identify(msgChunk);
		buffer.limit(limit);
		return dataformat;
	}
	
	/**
	 * Attempts an identification of the data format based on a short string. Only the first 100 characters are
	 * actually inspected.
	 * 
	 * @param msg	typically, a short string
	 * @return one of ANY, IATA, CSV, TEXT, XML, EDIFACT, X12, TRADACOMS, SWIFT, PROPRIETARY, BINARY
	 */
	public static DataFormat identify(String msg) {
		final String _XML_ID = "\\s*(?:<\\?xml .*\\?>)?\\s*<(\\w+:)?(\\w+)(?: |>).*"; //root element local name is in matching group 2
		//final String _SDK_ID = "(ZCZC.*?\\s)?\\s*=(HEADER|PRIORITY|DESTINATION).*";
		final String _TYPEB_ID = "(ZCZC.*?\\s)?\\s*\\cA?(Q.\\s)?[A-Z0-9]{7,8}\\s.*";
		final String _EDIFACT_ID = "UN(A:|B)\\+.*";
		final String _X12_ID = "ISA(.).{2}\\1.{10}\\1.*";
		final String _SWIFT_ID = "\\{1:[FAL]\\d\\d.*";
		final String _TRADACOMS_ID = "STX=ANA.*";
		final String _CSV_ID = "(['\"]?)[^,;	'\"]*?\\1([,;	])((['\"]?)[^,;	'\"]*?\\4?\\2)+[^,;	]*";
		
		if (msg==null || msg.length()<5) return DataFormat.ANY; 
		String msgChunk = msg;
		if (msgChunk.length() >MSG_IDENTIFICATION_RANGE) {
			//truncate
			msgChunk = msgChunk.substring(0,MSG_IDENTIFICATION_RANGE);
		}
	   	// replace all CR and LF by spaces, to facilitate
		msgChunk = msgChunk.replace( '\r', ' ' ).replace( '\n', ' ' );
    	if (msgChunk.matches( _XML_ID )) return DataFormat.XML;
    	if (msgChunk.matches( _TYPEB_ID )) return DataFormat.IATA;
    	if (msgChunk.matches( _EDIFACT_ID )) return DataFormat.EDIFACT;
    	if (msgChunk.matches( _X12_ID )) return DataFormat.X12;
    	if (msgChunk.matches( _SWIFT_ID )) return DataFormat.SWIFT;
    	if (msgChunk.matches( _TRADACOMS_ID )) return DataFormat.TRADACOMS;
    	if (msgChunk.matches( _CSV_ID )) return DataFormat.CSV;
    	     	
		return DataFormat.ANY;
	}

}
