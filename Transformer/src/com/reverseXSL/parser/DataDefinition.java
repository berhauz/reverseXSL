package com.reverseXSL.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reverseXSL.types.Cardinality;
import com.reverseXSL.types.Handling;
import com.reverseXSL.types.Impact;


/**
 * A data element is a special instance of a segment that contains only one sub-string.
 * <p> 
 * In other words, the cutting process bound to {@link SEGDefinition segments} stops with data elements. 
 * Data elements can only have the effect or removing syntax characters from the original 
 * string. A data element yields always one single piece of data that will always fill one 
 * simple-type XML element.
 * <p>Unlike a segment, a data element is not associated to an identification pattern plus 
 * a cutting pattern, but bears instead a validation pattern. The validation pattern is also 
 * used as data value extraction pattern (the associated regular expression must contain at least 
 * one 'capturing group'; see {@link #validPattern explanations}).
 * <p>
 * Alike a segment, a data element will bear:<ul>
 * <li>An XML tag for the target XML element to fill up (cannot be NULL).
 * <li>Mandatory / Optional and Conditional (M/O/C) constraints with ad-justed effects as follows:<ul>
 * <li>Mandatory: the element value cannot be empty. The failure to match the validation pattern 
 * always throws/records an ex-ception.
 * <li>Optional: the whole element can be empty, or the element value alone can be empty . 
 * If there are any bytes, the failure to match the validation pattern throws/records an exception.
 * <li>Conditional: the constraint is similar to the Optional case, but the absence 
 * or presence of the element value is in addition reported to a named condition.
 * </ul><li>An element may also bear minimum, maximum and accept loop counts in combination with 
 * the Mandatory / Optional and Conditional (M/O/C) keywords.
 * <li>A Mandatory or Optional Data element also contains a description.
 * </ul>
 * For the DEF line syntax, please look into the conventional documentation (MS-Word doc or PDF).
 */
final class DataDefinition extends GSDDefinition {

	//additional fields
	
	/**
	 * Validation (and Cutting) Pattern for the data element. 
	 * <p>
	 * The validation pattern must contain at least one capturing 
	 * group�at least a pair of '(' ')'�that isolates the data portion from the syntax 
	 * and padding stuff.<br>
	 * In case multiple validation groups are contained the data value is defined as 
	 * the concatenation of all capturing groups 1 to n in that order. Such technique 
	 * can be used to remove syntax characters in the middle of the data element. 
	 * Note that during concatenation, nested capturing groups are ignored; only the 
	 * capturing groups following each other are concatenated (otherwise the data 
	 * matching sub-capturing-groups will be duplicated in the result!).
	 * <p>
	 * In all circumstances the validation pattern must match the entire input string 
	 * making the data element; i.e. the matcher.matches() method shall yield TRUE. 
	 * Yet in other words, the interpretation of the pattern is enforced as 'possessive'.
	 * <p>
	 * The validation pattern has a twofold purpose:<ul>
	 * <li>As a means to remove syntax characters and padding; Examples:<ul>
	 * 	<li>"(.*)" to accept anything but later restricted by <char-spec>.
	 * 	<li>"^/(.*)" to remove a leading '/'
	 * 	<li>"^(.+?) *$" to trim trailing spaces
	 * </ul><li>As a data value validation function, i.e. as performed by sub-pattern contained 
	 * within the capturing group(s).
	 * </ul>Note that this second role is only one of the two ways to validate the data element value:<ol>
	 * <li>The first method as described above is to use the validation-pattern itself and 
	 * enforce the necessary restrictions inside the capturing group(s).
	 * <li>The second method is to use a generic capturing group 
	 * specification�alike '(.*)'�and rely on a built-in character-set validation function 
	 * alike UPALPHA, NUMERIC, DIGIT and others defined in {@link CharValidation}.
	 * 
	 */
	String validPattern = "";
	
	
	/**
	 * The expected minimum character count of the data element value.
	 * <p>
	 * zero by default.
	 */
	int lengthMin = 0;
	
	/**
	 * The expected maximum character count of the data element value.
	 * <p>
	 * A value of -1 (the default) denotes by convention an unlimited size.
	 */
	int lengthMax = -1;
	
	/**
	 * The character set validation function checks the compliance of the data element value 
	 * with a named character set, else the validation pattern itself. The various charValidation
	 * functions are implemented as anonymous inner classes that implement this abstract inner class.
	 * <p>
	 * The supported validation functions are:<DL>
	 * <dt>UPALPHA <dd>is A-Z (no space char)
	 * <dt>ALPHA <dd>is A-Z a-z (no space char)
	 * <dt>UPALPHANUM <dd>is A-Z 0-9 (no space char)
	 * <dt>ALPHANUM <dd>is A-Z a-z 0-9 (no space char)
	 * <dt>IATA <dd>is A-Z 0-9 plus '-' '.' and ' ' (applies to Cargo-IMP and AHM standards, known as 't' <i>free-form-text</i>)
	 * <dt>DIGIT <dd>is 0-9 (no space char accepted)
	 * <dt>NUMERIC <dd>is 0-9 + - , . and ' '   
	 * <dt>ASCII <dd>stands for ASCII printable characters (with space), i.e. between U+0020 and U+007E inclusive.   
	 * <dt>REPEATED-"<regex>"
	 * <dd>checks whether the data element value is a repetition (from zero to any number of times) of the specified pattern.
	 * <dt>ASMATCHED
	 * <dd>implies that no additional validation is made but as already performed inside the capturing group of the <valid-pattern> itself.
	 * </DL>
	 * @see #charValidation
	 */
	abstract class CharValidation {
		String fname;
		String vPattern = "";
		
		CharValidation(final String n, final String vp) {
			fname = n; vPattern = vp;
		}
		
		/**
		 * Validates compliance of each character in the data element value 
		 * against a named character set.
		 * 
		 * @param in	the input data element value (as String)
		 * @return	true when complying.
		 */
		public abstract boolean check(String in);
		
		/**
		 * Default rendering as string must be implemented by sub-classes
		 */
		public abstract String toString();
	
	}
	

	/**
	 * The one specific character set validation function implementation associated to this one data element 
	 * definition. 
	 * <p>
	 * Note that this implementation is generated as anonymous inner classes that extend
	 * the {@link DataDefinition.CharValidation} abstract class.
	 * 
	 * @see DataDefinition.CharValidation <i>inventory of the various character set validation functions</i>
	 */
	CharValidation charValidation = null;
	
	
	DataDefinition(Definition refDef) {
		super(refDef);
//		this.subElts = (ArrayList) Collections.EMPTY_LIST;
	}

	
	/**
	 * Unmarshals the specified DEF file input line passed as argument into a data element 
	 * definition object with the associated character-set-validation function implementation.
	 * 
	 * @param _linenb		(informative, for traces in exceptions) the relevant line number in the DEF file
	 * @param line	a line from the input DEF file containing a D (data) specification
	 * @param refDef 		provides a link to the parent reference definition settings!
	 * @throws ValidationException
	 * @throws ParserException 
	 */
	DataDefinition(int _linenb, String line, final HashMap nCond, Definition refDef) throws ParserException {
		super(refDef);
		
		final String _D_validpattern_tag = "^D\\s+([^\\s]{1})(.*?)\\1\\s+([@\\w][\\w-]*)\\s+(.*)$";
		final String _MOC_min_max_ACC_acc = "(M|O|C)\\s+(\\d+)\\s+(\\d+)\\s+ACC\\s+(\\d+)\\s*(.*)$";
		final String _RT_WF = "^(R|T)\\s*?(W|F)\\s*(.*)$";
		final String _COND_cname_cfeed = "^COND\\s+(\\w+)\\s+([^\\s]{1})(.*?)\\2\\s*(.*)$";
		final String _descr = "^\"(.*?)\"\\s*(.*)$";
		final String _lengthMinMax = "^\\[(\\d*)\\.\\.(\\d*)]\\s*(.*)$";
		Pattern p = null;
		Matcher m = null;

		String restOfLine = this.setDepth(line);
		this.atDEFLineNb = _linenb;
		
		p = Pattern.compile(_D_validpattern_tag);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("D (Data)",_linenb,line,"D \"<pattern>\" <tag> ...");
		//m.reset();
		//m.group(1) is the character used for the quote
		this.validPattern = m.group(2);
		this.xmltag = m.group(3);
		restOfLine = m.group(4);
		
		//must ensure that there is at least one capturing group within the validation pattern
		if (Pattern.compile(this.validPattern).matcher("").groupCount()<1)
			throw new ParserException.DEFErrorMissingCapturingGroup(_linenb,line,this.validPattern);
//		Generalised use of NOTAG (sept 2009)
//		//must ensure that the xmltag is not 'NOTAG'
//		if (this.xmltag.equals("NOTAG"))
//			throw new ParserException.DEFErrorNOTAGNotAllowed(_linenb,line,"D (Data)");
		p = Pattern.compile(_MOC_min_max_ACC_acc);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("D (Data)",_linenb,line,"D ... M|O|C <min> <max> ACC <acc> ...");
		//m.reset();
		if (m.group(1).equals("M")) this.cardinality = Cardinality.MANDATORY;
		else if (m.group(1).equals("O")) this.cardinality = Cardinality.OPTIONAL;
		else this.cardinality = Cardinality.CONDITIONAL;
		this.occMin = Integer.parseInt(m.group(2));
		this.occMax = Integer.parseInt(m.group(3));
		this.occAccept = Integer.parseInt(m.group(4));
		restOfLine = m.group(5);
		
		if (restOfLine.startsWith("COND")) {
			//validate that cardinality is "C" in this case
			if (cardinality!=Cardinality.CONDITIONAL) throw new ParserException.DEFErrorCardinalityCRequired(_linenb,line, "C <min> <max> ACC <acc>","D (Data)");
			p = Pattern.compile(_COND_cname_cfeed);
			m = p.matcher(restOfLine);
			if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("D (Data)",_linenb,line,"D ... COND <name> \"<feed>\" ...");
			//m.reset();
			this.conditionName = m.group(1);
			//validate condition name against declared set 
			if (!nCond.containsKey(this.conditionName))
				throw new ParserException.DEFErrorBadCONDName("D (Data)",_linenb,line,this.conditionName);
			//validate compatibility between current depth and declared named condition depth
			if (this.depth<((CONDDefinition)nCond.get(this.conditionName)).depthScope)
				throw new ParserException.DEFErrorBadDepthVersusCOND("D (Data)",_linenb,line,this.depth,this.conditionName,((CONDDefinition)nCond.get(this.conditionName)).depthScope);			
			//m.group(2) is the character used for the quote
			this.conditionFeed = m.group(3);
			//ensure that the feed can be compiled
			Pattern.compile(this.conditionFeed).matcher("");
			restOfLine = m.group(4);
		}
		else {
			//validate that cardinality is "M" or "O" in this case
			if (cardinality==Cardinality.CONDITIONAL) throw new ParserException.DEFErrorCardinalityMORequired(_linenb,line, "M|O <min> <max> ACC <acc>","D (Data)");
			p = Pattern.compile(_RT_WF);
			m = p.matcher(restOfLine);
			if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("D (Data)",_linenb,line,"D ... R|T W|F ...");
			//m.reset();
			this.handling = (m.group(1).equals("R")? Handling.RECORD : Handling.THROW );
			this.impact = (m.group(2).equals("W")? Impact.WARNING : Impact.FATAL );
			restOfLine = m.group(3);
		}
		
		p = Pattern.compile(_descr);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("D (Data)",_linenb,line,"D ... \"<description>\" ...");
		//m.reset();
		this.description = m.group(1);
		restOfLine = m.group(2);

		
		//rest of line shall be a char validation function
		//optionally followed by a [<lmin>..<lmax>] size specification
		//dummy loop just for the sake of breaking after first match against a function
		//IMPORTANT: a wrong order of function identifications below can cause to take one function for another
		while (true) {
			
			//try UPALPHANUM (is A-Z 0-9 (no space char))
			p = Pattern.compile("^UPALPHANUM\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("UpperCase-AlphaNumerical","[A-Z0-9]*") {

					public boolean check(String in) {
						return in.matches(vPattern);
					}

					public String toString() {
						return ("UPALPHANUM");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try UPALPHA (is A-Z (no space char))
			p = Pattern.compile("^UPALPHA\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("UpperCase-Alphabetical","[A-Z]*") {

					public boolean check(String in) {
						return in.matches(vPattern);
					}

					public String toString() {
						return ("UPALPHA");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try ALPHANUM (is A-Z a-z 0-9 (no space char))
			p = Pattern.compile("^ALPHANUM\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("AlphaNumerical","[A-Za-z0-9]*") {

					public boolean check(String in) {
						return in.matches(vPattern);
					}

					public String toString() {
						return ("ALPHANUM");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try ALPHA (is A-Z a-z (no space char))
			p = Pattern.compile("^ALPHA\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("Alphabetical","[A-Za-z]*") {

					public boolean check(String in) {
						return in.matches(vPattern);
					}

					public String toString() {
						return ("ALPHA");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try IATA (is A-Z 0-9 plus - . and space )
			p = Pattern.compile("^IATA\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("IATA-CharacterSet","[A-Z0-9 .-]*") {

					public boolean check(String in) {
						return in.matches(vPattern);
					}

					public String toString() {
						return ("IATA");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try DIGIT (is 0-9 (no space char))
			p = Pattern.compile("^DIGIT\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("Numerical-Digit","[0-9]*") {

					public boolean check(String in) {
						return in.matches(vPattern);
					}

					public String toString() {
						return ("DIGIT");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try NUMERIC (is 0-9 + - , . and ' ')
			p = Pattern.compile("^NUMERIC\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("Numerical","[0-9,. +-]*") {

					public boolean check(String in) {
						return in.matches(vPattern);
					}

					public String toString() {
						return ("NUMERIC");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try ASCII (stands for ASCII printable characters (with space), i.e. between U+0020 and U+007E inclusive)
			p = Pattern.compile("^ASCII\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("ASCII-CharacterSet",".*") {

					public boolean check(String in) {
						for (int i=0; i<in.length(); i++ )
							if ((in.charAt(i)<'\u0020')||(in.charAt(i)>'\u007E')) return false;
						return true;
					}

					public String toString() {
						return ("ASCII");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try ASMATCHED (as defined in the capturing groups of the validPattern)
			p = Pattern.compile("^ASMATCHED\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("As-Matched",validPattern) {

					public boolean check(String in) {
						return true; //validation already occured while cutting the data element value
					}

					public String toString() {
						return ("ASMATCHED");
					}
					
				};
				restOfLine = m.group(1);
				break;
			}

			//try REPEATED-"<regex>"
			p = Pattern.compile("^REPEATED-([^\\s]{1})(.*?)\\1\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("Repeated-Pattern",m.group(2)) {

					public boolean check(String in) {
						if (in.length()<=0) return true; //repeated 0 times is valid
						Pattern p = Pattern.compile(vPattern);
						Matcher m = p.matcher(in);
						StringBuffer val = new StringBuffer();
						while (m.find()) val = val.append(m.group()); //concatenating all pattern repetitions
						if (val.toString().equals(in)) return true; //...must yield again the original string
						return false;
					}

					public String toString() {
						return ("REPEATED-\""+vPattern+"\"");
					}
					
				};
				restOfLine = m.group(3);
				break;
			}

			//try DATE-"<simple-date-format-pattern>" (see SimpleDateFormat class)
			p = Pattern.compile("^DATE-([^\\s]{1})(.*?)\\1\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				charValidation = new CharValidation("DATE",m.group(2)) {

					public boolean check(String in) {
						// A date pattern is required
						if (in.length()>0) {
							try {
								SimpleDateFormat formatter = new SimpleDateFormat(vPattern);
								formatter.setLenient(false);
								Date date = (Date)formatter.parse(in);
								if (date != null) return true;
							} catch (IllegalArgumentException e) {
								// The simple date format pattern is wrong
							} catch (ParseException e) {
								// The given string does not match the simple date format pattern
							}
						}
						return false;
					}

					public String toString() {
						return ("DATE-\""+vPattern+"\"");
					}
					
				};
				restOfLine = m.group(3);
				break;
			}
			
			//no charValidation function specs has been found
			throw new ParserException.DEFErrorInvalidSyntax("D (Data)",_linenb,line,"D ... <CharValidation specs> ...");
		};
		//all breaks above jump hereafter
		//do we have an optional a [<lmin>..<lmax>] size specification ?
		p = Pattern.compile(_lengthMinMax);
		m = p.matcher(restOfLine);
		if (m.matches()) {
			this.lengthMin = ((m.start(1)==m.end(1))? 0 : Integer.parseInt(m.group(1)));
			this.lengthMax = ((m.start(2)==m.end(2))? -1 : Integer.parseInt(m.group(2)));
			restOfLine = m.group(3);
		}		
		//check that there's no garbage left in the rest of the DEF line
		if (!restOfLine.matches("^\\s*$")) 
			throw new ParserException.DEFErrorInvalidEndOfDataDEF(_linenb,line);
	}
	
	public String toString() {
		//nesting depth is limited to 50 in class Definition
		String s = new String("|||||||||||||||||||||||||||||||||||||||||||||||||||").substring(0, depth);
		s = s.concat("D \""+validPattern+"\" ");
		s = s.concat(xmltag+" ");
		s = s.concat(cardinality.toString()+" "+occMin+" "+occMax+" ACC "+occAccept+" ");
		if (cardinality.equals(Cardinality.CONDITIONAL)) s = s.concat("COND "+conditionName+" \""+conditionFeed+"\" ");
		else s = s.concat(handling.toCode()+" "+impact.toCode()+" \""+description+"\" ");
		s = s.concat(charValidation.toString());
		s = s.concat(" ["+lengthMin+".."+((lengthMax<0)?"":String.valueOf(lengthMax))+"]");
		return (s + "\n"); //+ super.toString() not applicable here
	}


	/* (non-Javadoc)
	 * @see com.reverseXSL.parser.GSDDefinition#getName()
	 */
	String getName() {
		return "DataDefinition";
	}


}
