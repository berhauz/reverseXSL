package com.reverseXSL.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reverseXSL.types.Cardinality;
import com.reverseXSL.types.Handling;
import com.reverseXSL.types.Impact;

/**
 * Conditions express interdependencies between data elements, segments, groups or anything else.
 * <p>
 * Conditions are by convention listed at the top of the DEF file, above the message definition itself.
 * <p>Conditions can either be:<dl>
 * <dt>Global <dd>in which case the condition shall be valid once for the whole message, 
 * whatever the depths and repetition counts from which the dependent elements may come;
 * <dt>Local <dd>in which case the condition shall be met for every instance of a specified 
 * depth and the enclosed sub-elements. For instance, a group repeats at depth 2 and 
 * contains segments and data elements affected by inter-dependencies that respectively 
 * occur at depth 3, 4 and 5. A Local condition depth 2 will test for the inter-dependency 
 * condition to apply within every repetition of the group at depth 2 .
 * </dl>It does mean that a Global condition is a Local condition depth 0.
 * <p>
 * Conditions are denoted <b>'named conditions'</b> because the condition name is used to link 
 * all the inter-dependent elements.
 * <p>
 * Named conditions are verified only after the complete parsing of the message. The data 
 * for verifying a condition is collected while parsing the input message, but the verification 
 * itself and any throwing/recording of exception occurs only at the end.
 * <p>
 * Named Conditions are listed at the beginning of the DEF file because the loading of such 
 * conditions triggers the creation of base objects required for recording the data to match 
 * ultimately against the condition itself.
 * <p>
 * The definition of a named condition contains:<ul>
 * <li>An indication of the Global or Local scope, with the associated nesting level 
 * (say depth) for the latter. (see above)
 * <li>A matching pattern that shall be verified for the condition to hold true, 
 * see the mechanism described in {@link #verifPattern}.
 * <li>An associated error text to throw/record in case the verification fails.
 * </ul>
 *
 */
final class CONDDefinition {

	//ORIGINAL fields (CONDDefinition is not a sub-class of GSDDEfinition)

	/**
	 * Names this condition. 
	 * <p>
	 * All CONDition names must be unique within a given DEF file.
	 * 
	 */
	String name = "";


	/**
	 * The nesting depth at which the condition must be verified.
	 * <p>
	 * Depth is counted from 0 for the Message itself (==the top segment).
	 */
	int depthScope = 0;

	/**
	 * Condition verification Pattern.
	 * <p>
	 * <b>How inter-dependency conditions are verified?</b><br>
	 * Conditions are verified using pattern-matching logic. The data feeding this 
	 * matching process is constituted from string elements collected during 
	 * the parsing of the message. The string elements can be arbitrary 
	 * string constants, else data element values. Each time a group, segment or 
	 * data element is declared as {@link Cardinality Conditional}, a specified string 
	 * (another pattern which yields a constant or specifies a capturing group) is 
	 * added to the named condition collection. The loop-counts for every nesting level 
	 * are added as attributes, as well as original element references and offsets 
	 * in the message.<br> 
	 * When the parsing of the message is completed, each condition is 
	 * then evaluated. The {@link #depthScope depth scope} will instruct how to proceed 
	 * with the grouping of collected strings and proceed with pattern matching 
	 * against the concatenated string result. Any depth above 1 causes
	 * the verification process to loop at that depth level. Pattern verification is
	 * then performed against the strings resulting from the concatenation of collected 
	 * string elements at that depth level and all sub-levels inside it.
	 * <p>
	 * The careful selection of string constants combined to the flexibility of 
	 * patterns allows matching about every inter-dependency constraint. 
	 * A straight inventory of all expected keyword combinations separated 
	 * by '|' (OR logic) in a regular expression would do the job in most cases.
	 * <p>
	 * The condition is formally verified only if: <code>Pattern.compile(pattern).matcher(depthString).matches()</code>
	 * yields TRUE.
	 */
	String verifPattern = "";

	/**
	 * Tells whether to Record or Throw exceptions bound to the verification of
	 * this condition.
	 * <p>
	 * When thrown, it is the module calling the parser that shall trap 
	 * the exception; the exception is effectively thrown as defined in java.
	 * When recorded the exception is recorded in an ordered list 
	 * of exception objects and processing continues.
	 * <p>
	 * When one of the following is exceeded, all
	 * exceptions are thrown: <ul>
	 * <li>the number of fatal exceptions, or 
	 * <li>the total number of exceptions (both warning and fatal impact).
	 * </ul>These thresholds are passed as arguments when calling the parser.
	 */
	Handling handling = Handling.THROW;
	
	/**
	 * Indicates whether exceptions bound to the verification of
	 * this condition are considered FATAL or just a Warning.
	 */
	Impact impact = Impact.WARNING;
	
	/**
	 * Defines the error message text associated to the verification of
	 * this condition.
	 */
	String errorText = "--undefined error--";
	

	
	CONDDefinition() {
		super();
	}

	/**
	 * Unmarshalls the specified DEF file input line passed as argument into a group 
	 * definition object.
	 * 
	 * @param _linenb		(informative, for traces in exceptions) the relevant line number in the DEF file
	 * @param line	a line from the input DEF file containing a COND specification
	 * @throws ValidationException
	 * @throws ParserException 
	 */
	CONDDefinition(int _linenb, String line) throws ParserException {
		super();
		final String _COND_name_idpattern = "^COND\\s+(\\w+)\\s+\"(.*?)\"\\s+(.*)$";
		final String _DEPTH_level = "DEPTH\\s+(\\d+)\\s+(.*)$";
		final String _RT_WF_errtext = "(R|T)\\s*?(W|F)\\s*?\"(.*?)\"\\s*(.*)$";
		Pattern p = null;
		Matcher m = null;
		
		p = Pattern.compile(_COND_name_idpattern);
		m = p.matcher(line);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("COND",_linenb,line,"COND <name> \"<pattern>\" ...");
		//m.reset();
		this.name = m.group(1);
		this.verifPattern = m.group(2);
		String restOfLine = m.group(3);

		p = Pattern.compile(_DEPTH_level);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("COND",_linenb,line,"COND ... DEPTH <level> ...");
		//m.reset();
		this.depthScope = Integer.parseInt(m.group(1));
		restOfLine = m.group(2);

		p = Pattern.compile(_RT_WF_errtext);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("COND",_linenb,line,"COND ... R|T W|F \"<error text>\" ...");
		//m.reset();
		this.handling = (m.group(1).equals("R")? Handling.RECORD : Handling.THROW );
		this.impact = (m.group(2).equals("W")? Impact.WARNING : Impact.FATAL );
		this.errorText = m.group(3);
		restOfLine = m.group(4);
		
		//rest of line shall be empty for a condition
		if (restOfLine.length()!=0) throw new ParserException.DEFErrorExtraChars("COND",_linenb,line,"R|T W|F \"<error text>\"");
		
	}
	
	public String toString() {
		//nesting depth is limited to 50 in Definiton
		String s = new String("COND "+name+" \""+verifPattern+"\" ");
		s = s.concat("DEPTH "+depthScope+" ");
		s = s.concat(handling.toCode()+" "+impact.toCode()+" \""+errorText+"\" ");
		return s;
	}

}
