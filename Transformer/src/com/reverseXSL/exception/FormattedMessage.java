package com.reverseXSL.exception;

import java.io.Serializable;


/**
 * Provides multi-language suport for application messages, regular traces, and logs. 
 * Equivalent 
 * to {@link com.reverseXSL.exception.FormattedException} but for non-failure cases.
 * 
 * @author bernardH
 *
 */
public class FormattedMessage implements Serializable {

	private static final long serialVersionUID = -2069355970355323873L;
	
	private String messageId;
	private Object[] arguments;
	
	// C O N S T R U C T O R S
	
	protected FormattedMessage(String messageId) {
		this(messageId, (Object[]) null);
	}

	protected FormattedMessage(String messageId, Object[] args) {
		this.messageId = messageId;
		this.arguments = args;
	}

	// P U B L I C   M E T H O D S
	
	/*
	 *  Message handling
	 */

	/**
	 * Get the formatted message through the FormattingService, itself based on 
	 * {@link java.text.MessageFormat}.
	 * 
	 */
	public String getMessage() {
		return FormattingService.getMessage(this.messageId, this.arguments);
	}

	/**
	 * Same as {@link #getMessage()}.
	 */
	public String toString() {
		return getMessage();
	}

}
