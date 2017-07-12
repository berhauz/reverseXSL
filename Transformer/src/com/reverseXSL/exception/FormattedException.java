package com.reverseXSL.exception;


/**
 * FormattedException is the root of all exceptions that are handled according to 
 * the 'i18n' multi-language support standard by the reverseXSL software.
 * <p>
 * This class is abstract to ensure that an implementation only throws a
 * specific exception (e.g. ParserException) which is a sub-class.
 * </p><p>
 * Implementations of methods that throw FormattedException often bear throw
 * clauses that are more specific than FormattedException (i.e. child classes references).
 * </p>
 * 
 * @see java.text.MessageFormat
 */
public class FormattedException extends Exception {
	
	private static final long serialVersionUID = 2003734251156385913L;
	
	Throwable nestedException;
	protected Object[] arguments;
	
	// C O N S T R U C T O R S
	
	protected FormattedException(String code) {
		this(code, (Throwable) null, (Object[]) null);
	}

	protected FormattedException(String code, Object[] args) {
		this(code, (Throwable) null, args);
	}

	protected FormattedException(String code, Throwable t, Object[] args) {
		super(code,t);
		this.nestedException = t;
		this.arguments = args;
	}


	// P U B L I C   M E T H O D S
	
	public Throwable getCause() {
		// overloaded to provide backwards compatibility
		return nestedException;
	}

	/**
	 * Get the formatted message through the FormattingService, itself based on 
	 * {@link java.text.MessageFormat}.
	 * 
	 */
	public String getMessage() {
		return FormattingService.getMessage(super.getMessage(), arguments);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Throwable#getLocalizedMessage()
	 */
	public String getLocalizedMessage() {
		//same as getMessage!
		return FormattingService.getMessage(super.getMessage(), arguments);
	}



	/**
	 * Similar to {@link #getMessage()}, prefixed with the error code.
	 */
	public String toString() {
		//String cause="";
		//anticipate the Caused by??? no! done by standard stack trace
		//if (this.getCause()!=null) cause="\n...caused by "+this.getCause().toString();
		String msg = getMessage();
		//return "ERROR " + super.getMessage() + ": " + msg + cause ;
		return "ERROR " + super.getMessage() + ": " + msg  ;
	}

}
