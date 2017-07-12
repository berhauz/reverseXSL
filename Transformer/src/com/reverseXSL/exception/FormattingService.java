package com.reverseXSL.exception;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
* Implementation of the mechanism for locating
* formatted message strings and substituting arguments for message parameters.
* <p>
* The resource bundle mechanism from Java is used for locating messages based on
* keys; the preferred form of resource bundle is a property file mapping
* keys to messages.
* </p><p>
* NOTE: null arguments are accepted as arguments and yield &quot;null&quot; strings in the formated outputs. 
* </p>
*
*/
final class FormattingService {

	private static final Locale EN = new Locale("en", "US");

	private FormattingService() {}

	static ResourceBundle getBundle(Locale locale) {
		// This covers the case where neither the
		// requested locale or the default locale
		// have a resource.
		try {
			return ResourceBundle.getBundle("com.reverseXSL.exception.messages", locale);
		} catch (MissingResourceException mre) {
		}
		try {
			return ResourceBundle.getBundle("com.reverseXSL.exception.messages", EN);
		} catch (MissingResourceException mre) {
		}
		return null;
	}

	/**
	 * Get a simple fixed message with no arguments.
	 * 
	 * @param messageID message identifier in messages.properties
	 * @return the language-specific formatted message string
	 */
	public static String getMessage(String messageID) {
		return getMessage(messageID, (Object[]) null);
	}
	
	/**
	 Transform the message from messageId to the actual error, warning, or
	 info message using the correct locale.
	 <P>
	 The arguments to the messages are passed via an object array, the objects
	 in the array WILL be changed by this class. The caller should NOT get the
	 object back from this array.
	 </p><p>
	 * NOTE: null arguments are accepted as arguments and yield &quot;null&quot; strings in the formated outputs. 
	 * </p>
	 
	 */
	public static String getMessage(String messageId, Object[] arguments) {

		try {
			return formatMessage(getBundle(Locale.getDefault()), messageId, arguments);
		} catch (MissingResourceException mre) {
			// message does not exist in the requested locale or the default locale.
			// most likely it does exist in our default base class _en, so try that.
		}
		return formatMessage(getBundle(EN), messageId, arguments);
	}

	/**
	 * provides message formatting logic.
	 * 
	 * @param bundle
	 * @param messageId
	 * @param arguments
	 * @return formatted message string
	 */
	static String formatMessage(ResourceBundle bundle, String messageId, Object[] arguments) {

		String message = null;
		Object[] shorterArgs;
		if (arguments == null)
			shorterArgs = new Object[0];
		else {
			shorterArgs = new Object[arguments.length];
			//truncate large string arguments! 
			//	(don't need to test other CharSequence, buffers, etc., because not used here!
			for (int i=0;i<arguments.length;i++) {
				if (arguments[i] instanceof String && ((String)arguments[i]).length()>300) {
					shorterArgs[i]= ((String)arguments[i]).substring(0,300) + "...(more)";
				} else shorterArgs[i]=arguments[i];
			}
		}
		if (bundle != null) {
			
			try {
				message = bundle.getString(messageId);
				
				try {
					return MessageFormat.format(message, shorterArgs);
				}
				catch (IllegalArgumentException iae) {
				}
				catch (NullPointerException npe) {
					//null arguments cause a NullPointerException. 
					//This improves reporting.
				}
				
			} catch (MissingResourceException mre) {
			} 
		}

		//fail-over message formatting
		StringBuffer sb = new StringBuffer(messageId);
		sb.append(" : ");
		int len = shorterArgs.length;

		for (int i=0; i < len; i++) {
		    // prepend a comma to all args but the first
			if (i > 0)
				sb.append(", ");

			sb.append('[');
			sb.append(i);
			sb.append("] ");
			if (shorterArgs[i] == null)
				sb.append("null");
			else
				sb.append(shorterArgs[i].toString());
		}
		return sb.toString();
	}

}
