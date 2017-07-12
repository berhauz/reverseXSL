package com.reverseXSL.types;

import com.reverseXSL.exception.FormattedException;

/**
 * [NOTE: For future use in Headers...]
 * 
 * @author bernardH
 *
 */
public class TypeException extends FormattedException {

	private static final long serialVersionUID = -8611179262544568503L;

	protected TypeException(String code, Throwable t, Object[] args) {
		super(code, t, args);
	}

	/**
	 * T001 Invalid [{1}] Address: the value [{0}] contains characters not in [{2}].
	 */
	public static class InvalidCharsInAddress extends TypeException {
		private static final long serialVersionUID = -5502381681811539313L;

		public InvalidCharsInAddress(String value,String type, String pattern) {
			super("T001",null,new String[]{value,type, pattern});
		}
	}
	
	/**
	 * T002 Invalid [{1}] Address: the value [{0}] must be [{2}] characters long.
	 */
	public static class InvalidAddressValueLength extends TypeException {
		private static final long serialVersionUID = -533558943429376689L;

		public InvalidAddressValueLength(String value,String type, String pattern) {
			super("T002",null,new String[]{value,type, pattern});
		}
	}

	
}
