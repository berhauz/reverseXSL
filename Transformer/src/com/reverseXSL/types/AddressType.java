package com.reverseXSL.types;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AddressType singletons specify the addressing space (IATA, EMAIL, URL, etc.). 
 * It is a strong typed enumeration class compatible with JDK1.4.
 * <p>
 * The AddressType tag plays an essential role in address mappings and routing.
 * <p>
 * The Address Type enumeration apply both to communication envelopes 
 * and higher level addresses in message headers.
 * <p>
 * Valid enumeration values are all like <code>AddressType.CODE</code>, for 
 * instance <code>AddressType.EMAIL</code>
 * For the list of valid codes, see {@link #table}.
 * <p>
 * One can then further validate the address using the {@link AddressType#isValid(String)} method; 
 * for instance <code>AddressType.EMAIL.isValid("john@champ.aero")</code>. 
 * Nothing is returned by the method but a {@link com.reverseXSL.types.TypeException} is thrown in case of failure.
 * <p>
 * There are associated Junit test methods.
 * 
 * @author bernardH
 *
 */
public class AddressType implements Serializable {
	
//	required for passing as argument to bean's business methods
	private static final long serialVersionUID = -6267584830575547004L;
	
	private final static String UNDEFINED_TYPE_STRING = "UNDEFINED";
	private final static String DEFAULT_TYPE_STRING = "DEFAULT";
	private final static String ALIAS_TYPE_STRING = "ALIAS";
	private final static String AIRLINE_TYPE_STRING = "AIRLINE";
	private final static String EDI_TYPE_STRING = "EDI";
	private final static String EMAIL_TYPE_STRING = "EMAIL";
	private final static String FAX_TYPE_STRING = "FAX";
	private final static String FILE_TYPE_STRING = "FILE";
	private final static String HOST_TYPE_STRING = "HOST";
	private final static String IATA_TYPE_STRING = "IATA";
	private final static String PIMA_TYPE_STRING = "PIMA";
	private final static String QNAME_TYPE_STRING = "QNAME";
	private final static String TAG_TYPE_STRING = "TAG";
	private final static String URL_TYPE_STRING = "URL";
	
	String code;
	String info;
	
	// + + + + + + + + + + + + +  I N N E R   C L A S S E S   + + + + + + + + + + + + +
	
	//LATER develop additional validation methods here when setting up the routing rules
	public static class UndefinedAddressType extends AddressType {
		
		private static final long serialVersionUID = 3054843269614618053L;

		public UndefinedAddressType() {
			super(UNDEFINED_TYPE_STRING, "-undefined kind of address-");
		}
	}
	
	public static class DefaultAddressType extends AddressType {
		
		private static final long serialVersionUID = 1457753153072480121L;

		public DefaultAddressType() {
			super(DEFAULT_TYPE_STRING, "-default kind of address-");
		}
	}
	
	public static class AliasAddressType extends AddressType {
		
		private static final long serialVersionUID = 1962274499882034088L;

		public AliasAddressType() {
			super(ALIAS_TYPE_STRING, "-alias kind of address-");
		}
	}
	
	public static class IataAddressType extends AddressType {
		
		private static final long serialVersionUID = 6163241690984245387L;

		public IataAddressType() {
			super(IATA_TYPE_STRING, "IATA Address in 7 or 8 chars");
		}
		
		public void isValid(String value) throws TypeException {
			Pattern pattern = Pattern.compile("[A-Z0-9]+");
	        Matcher matcher = pattern.matcher( value );
	        if (! matcher.matches()) throw new TypeException.InvalidCharsInAddress(value,info,pattern.pattern());
	        if ((value.length()>8)||(value.length()< 7) ) throw new TypeException.InvalidAddressValueLength(value,info,"7..8");
		}
	} 
	
	public static class PimaAddressType extends AddressType {
		
		private static final long serialVersionUID = 538795676197252432L;

		public PimaAddressType() {
			super(PIMA_TYPE_STRING, "Party Identification M Address");
		}
	}
	
	public static class AirlineAddressType extends AddressType {
		
		private static final long serialVersionUID = 6082308088361610459L;

		public AirlineAddressType() {
			super(AIRLINE_TYPE_STRING, "IATA Airline code");
		}
	}
	
	public static class EmailAddressType extends AddressType {
		
		private static final long serialVersionUID = 1824800922596194528L;

		public EmailAddressType() {
			super(EMAIL_TYPE_STRING, "Internet eMail Address");
		}
		
		public void isValid(String value) throws TypeException {
			try {
				//javax.mail.Address address = new javax.mail.internet.InternetAddress(value, true);
				//TODO provide a simple validation that releaves from J2EE dependencies
			} catch (Exception e) {
				//throw new TypeException.InvalidValueException(e.getMessage());
			}
		}
	}
	
	public static class FileAddressType extends AddressType {
		
//		required for passing as argument to bean's business methods
		private static final long serialVersionUID = -4142374432525497555L;

		public FileAddressType() {
			super(FILE_TYPE_STRING, "File Name");
		}
		
		public void isValid( String value ) throws TypeException {
			Pattern pattern = Pattern.compile("[-_a-zA-Z0-9.$~&()#=+%]+");
	        Matcher matcher = pattern.matcher( value );
	        if (! matcher.matches()) throw new TypeException.InvalidCharsInAddress(value,info,pattern.pattern());
		}
	}
	
	public static class QnameAddressType extends AddressType {
		
		private static final long serialVersionUID = 4320947620686380828L;

		public QnameAddressType() {
			super(QNAME_TYPE_STRING, "Messaging Queue Name");
		}
	}
	
	public static class UrlAddressType extends AddressType {
		
		private static final long serialVersionUID = -8135346458221072186L;

		public UrlAddressType() {
			super(URL_TYPE_STRING, "Internet URI/URL");
		}
	}
	
	public static class HostAddressType extends AddressType {
		
		private static final long serialVersionUID = 5913231072086387054L;

		public HostAddressType() {
			super(HOST_TYPE_STRING, "Hostname or IP Address");
		}
	}
	
	public static class FaxAddressType extends AddressType {
		
		private static final long serialVersionUID = 1406021582859388793L;

		public FaxAddressType() {
			super(FAX_TYPE_STRING, "FAX Address");
		}
	}
	
	public static class EdiAddressType extends AddressType {
		
		private static final long serialVersionUID = 8484280427508554539L;

		public EdiAddressType() {
			super(EDI_TYPE_STRING, "EDI Identifier & Qualifier");
		}
	}
	
	public static class TagAddressType extends AddressType {
		
		private static final long serialVersionUID = 8573610488486759823L;

		public TagAddressType() {
			super(TAG_TYPE_STRING, "Message TAG");
		}
	}
	
	// + + + + + + + + +  P R I V A T E   C O N S T R U C T O R   + + + + + + + + + + +
	
	/**
	 * @param info	a long descriptive name of an address type
	 * @param code	a short uppercase code for the address type
	 */
	private AddressType(String code, String info) {
		this.code = code;
		this.info = info;
	}
	
	public void isValid(String value) throws TypeException {
		// OVERRIDE THIS METHOD IN THE CHILD for true validation.
		// default is no validation!
		return;
	}
	
	// + + + + + + F I N A L   S T A T I C   T Y P E   D E F I N I T I O N  + + + + + +
	
	/**
	 * No address validation rules apply to UNDEFINED addresses; 
	 * hence this is the only AddressType 
	 * that does not trigger validation exceptions.
	 */
	public final static AddressType UNDEFINED = new UndefinedAddressType();
	
	/**
	 * A default address type tell the system not to not use its corresponding 
	 * associated address value but rather a default address value. No address 
	 * validation rules apply for it.
	 */
	public final static AddressType DEFAULT = new DefaultAddressType();
	
	/**
	 * An Alias address type is a LOGICAL address type mainly use to be translated
	 * into a real address. No address validation rules apply for it.
	 */
	public final static AddressType ALIAS = new AliasAddressType();
	
	/**
	 * A IATA address; must be 7 or 8 uppercase alpha-numeric, ASCII only
	 * Must contain only A-Z and 0-9, and be 7 to 8 chars long.
	 */
	public final static AddressType IATA = new IataAddressType();
	
	/**
	 * a IATA PIMA address as defined in the Cargo-IMP Manual (Participant Identification and Message Addressing)
	 * TODO PIMA Address validation
	 */
	public final static AddressType PIMA = new PimaAddressType();
	
	/**
	 * an IATA airline code, either as 2 letters, else as 3 digits (as used on waybill numbers)
	 */
	public final static AddressType AIRLINE = new AirlineAddressType();
	
	/**
	 * a valid Internet email address like john@champ.aero; domain name validity is not tested, only well-formedness
	 * TODO Internet email validation
	 */
	public final static AddressType EMAIL = new EmailAddressType();
	
	/**
	 * a File name without a directory path; printable chars with space and case sensitive, no diacritic 
	 * sign allowed (^ � �  ' `), neither " ! ? / \ | { } [ ] , ; or *<br>
	 * The validation pattern is exactly <code>"[-_a-zA-Z0-9.$~&()#=+%]+"</code>
	 */
	public final static AddressType FILE = new FileAddressType();
	
	/**
	 * A message queue name
	 * TODO document MQ name validation
	 */
	public final static AddressType QNAME = new QnameAddressType();
	
	/**
	 * an URL or more generically, an URI.
	 * TODO document URI validation rules
	 */
	public final static AddressType URL = new UrlAddressType();
	
	
//	/**
//	 * Reserved for Internal Broker routing: 
//	 * the Queue Name of an Internal Message Broker Queue
//	 * BHAU 19/6/2007 - decided to drop QDESTINATION Address Type! not needed in our architecture
//	 */
//	public final static AddressType QDESTINATION = new AddressType("Internal Broker Queue Destination","QDESTINATION", "noValidation");
	
	
	/**
	 * a host name (possiby with domain name) or directly its IP address like 10.11.255.3
	 */
	public final static AddressType HOST = new HostAddressType();
	
	/**
	 * An international fax number like &quot;1(212)1234567&quot;, without any formatting except parentheses.
	 */
	public final static AddressType FAX = new FaxAddressType();
	
	/**
	 * An EDI Identifier optionally followed by &quot;:&quot; and a qualifier. For
	 * instance &quot;5412345000010:14&quot; is an EAN number.
	 */
	public final static AddressType EDI = new EdiAddressType();
	
	/**
	 * A message tag value, used to seggragate traffic in flow groups and accepted here as a general 
	 * indication of a target application environment, hence a form of application-group address.
	 * 
	 */
	public final static AddressType TAG = new TagAddressType();
	
	/**
	 * May be used to iterate over all possible enumeration values.
	 */
	public final static AddressType[] table = { UNDEFINED, DEFAULT, ALIAS, IATA, PIMA, AIRLINE, EMAIL, FILE, QNAME, URL, HOST, FAX, EDI, TAG };
	
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
	
	public static AddressType find(String key) {
		for (int i=0;i<table.length;i++)
			if (table[i].code.equals(key)) return table[i];
		return UNDEFINED;
	}
	
	public boolean equals(Object otherAddressTypeObject) {
		return this.code.equals( ((AddressType)otherAddressTypeObject).code );
	}
}
