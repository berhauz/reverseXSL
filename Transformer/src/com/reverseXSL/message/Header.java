package com.reverseXSL.message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * NOTE: The support for EDI headers is not yet integrated into the current version of the tool set, but a few static 
 * utility procedures are already used elsewhere.
 * 
 * Headers will become syntax-independent address handling and routing vehicles in a future release.
 * 
 * @author bernardH
 *
 */
public abstract class Header {

	
	//TODO provide additional fields
	//	address lists
	//	message type
	// 	message version
	//	association assigned code
	//	security attriubutes
	
	/**
	 * The procedure concatenates all captured info pieces (by one or several
	 * capturing groups in the regular expression) from the <u>first</u> match found in the char sequence 
	 * passed as argument, using MULTILINE and DOTALL modes.
	 * <p>
	 * This procedure is inspired from {@link com.reverseXSL.parser.Parser#extractCompositeValue(StringBuffer, Pattern)}
	 * but with two differences: It does not loop on matcher.find() but calls it only once, and--second--it works in MULTILINE 	
	 * mode.<br>
	 * MULTILINE indicates that the '^' and '$' can match not only the start and end of the 
	 * entire char sequence, but also any intermediate line terminator (delimited by LF, CRLF, CR alone, or the Unicode 
	 * next-line, line-separator, and paragraph marks). Note that matching characters of a CRLF sequence requires 
	 * for instance the pattern "$..^". <br>
	 * DOTALL indicates that the '.' matches any character including
	 * line terminator characters.
	 * </p><p>
	 * </p>
	 * @param s		the char sequence from which to extract the reference: a CharBuffer, String, or StringBuffer
	 * @param regex	the regex pattern to match; only the first match is considered.
	 * @return		the catenation on all matched capturing groups (one level deep only, sub-capturing groups are ignored)
	 * 				or an exception string whenever the input sequence is NULL or the regex is corrupt.
	 * 				If there are no matches, the empty string is returned.
	 */
	public static String extractReference(CharSequence s, String regex) {
		try {
			return extractReference(s,regex,s.length());
		} catch (Exception e) {
			//in case of nullPointer exception or else
			return e.toString();
		}
	}

	/**
	 * Variant of {@link #extractReference(CharSequence, String)} that limits here the pattern 
	 * lookup to the region from the first to 'upTo' characters.
	 * 
	 * @param s		the char sequence from which to extract the reference: a CharBuffer, String, or StringBuffer
	 * @param regex	the regex pattern to match; only the first match is considered.
	 * @param upTo	is used to delimit the matching region, so that the entire message is surely not parsed
	 * @return		the catenation on all matched capturing groups (one level deep only, sub-capturing groups are ignored) 
	 * 				or an exception string whenever the input sequence is NULL or the regex is corrupt.
	 * 				If there are no matches, the empty string is returned.
	 */
	public static String extractReference(CharSequence s, String regex,int upTo) {
		try {
			String r = ""; //result
			int mEnd = 0; //current right-most matcher group end - used to exclude capturing sub-groups from concatenation scope
			Pattern ptrn = Pattern.compile(regex, Pattern.DOTALL+Pattern.MULTILINE);
			Matcher matcher = ptrn.matcher(s);
			//LATER !! the matcher.region() method is only supported from Java 5!!!
			if (upTo<=0) return "";
			//matcher.region(0, Math.min(upTo, s.length())); //supported only from JAVA 5 version, not available in JRE 1.4.2!
			if (matcher.find()) {
				for(int gn = 1; gn <= matcher.groupCount();gn++)
					if (matcher.start(gn) >= mEnd){
						//this is the trick to eliminate sub-groups from concatenation scope
						String g = matcher.group(gn).trim();
						// Verify that the business REF does not
						// contains LF or CR which means that the 
						// regular expression is wrong.
						if ( g.indexOf('\n') >=0 || g.indexOf('\r') >=0) {
							g = "BADREGEX"; // resulting value contains not permitted CR or LR
						}
						r = r.concat(g.trim());
						mEnd = matcher.end(gn);
					};
					//ELSE this group starts before the end of the last one, skip it
					//or this (optional?)group is null in which case .start==-1
					return r;	
			}
			return "";
		} catch (Exception e) {
			return e.toString();
		}
	}

}
