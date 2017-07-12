package com.reverseXSL.util;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * To be used as Xalan XSLT extension function for inserting the current date into XSLT outputs.
 * <p>
 * use as follows in XSLT scripts:<br><br><code>
 * 	&lt;xsl:stylesheet version="1.0" 
 * 	xmlns:fct="com.reverseXSL.util.CurrentDate" extension-element-prefixes="fct" 
 * 	xmlns:rx="http://reverseXSL.com" 
 * 	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"&gt;<br>
 * ...<br>
 * &lt;xsl:text&gt;DATETIME=&lt/xsl:text&gt;<br>
 * &lt;xsl:value-of select="fct:nowDate()"/&gt;<br>
 * ...<br>
 * </code><br><br>
 * which will generate in output:<br>
 * <code>DATETIME=2007-07-11 07:51:03+0200</code><br>
</p>
 * @author bernardH
 *
 */
public class CurrentDate {

	/**
	 * Returns a predefined-format date string.
	 * 
	 * @return "yyyy-MM-dd HH:mm:ssZ", e.g. "2007-07-11 07:51:03+0200"
	 */
	public static Object nowDate() {
		return nowDate("yyyy-MM-dd' 'HH:mm:ssZ");
	}
	
	/**
	 * Returns a date and time value compliant with the XML-schema dateTime simple type.
	 * 
	 * @return almost as "yyyy-MM-ddTHH:mm:ss.SSSZ" but amended to insert a colon in the timeZone offset, i.e. "2008-05-31T13:20:00.000-05:00"
	 */
	public static Object nowXMLDate() {
		String s = (String)nowDate("yyyy-MM-dd'T'HH:mm:ssZ");
		return (s.substring(0,22)+":"+s.substring(22));
	}

	/**
	 * Returns a date string formatted as specified in argument.
	 * 
	 * @param dateFormat as specified in java.text.SimpleDateFormat.
	 * 
	 * @return	formatted date string
	 */
	public static Object nowDate(String dateFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		Date dt = new Date(); 
		String out = sdf.format(dt);
		return out;
	}
}
