package com.reverseXSL.parser;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A mark is the evaluation, on the fly, of a named condition, whose result is 
 * inserted in the output message at the depth and at the level where it is evaluated.
 * <p>
 * A mark allows inserting a value in the message flow to 'mark'—literally—the verification 
 * or non-verification of a given pattern of occurrences in the message. 
 * <p>
 * A mark allows reporting explicitly in the output message (with XML tags and values) the 
 * result of evaluating a named condition. There are three possible purposes:<ol>
 * <li>One can attached a named condition to a data element in order to generate its value 
 * as token associated to such condition. Then, just next to the data element and at 
 * the same depth, one or several marks may be inserted in the message, each containing 
 * an evaluation expression that recognise the different patterns of the said token as, 
 * for instance, an email address, a fax number, a contact person or else, and consequently 
 * insert in the output message an explicit XML element whose value would 'mark' the type 
 * of data element just recognised.
 * <li>One can explicitly report that 'no such element' or 'no such group' or 'no such segment' 
 * was found in the source message.
 * <li>One may decide to report within the XML output itself that some interdependencies 
 * or other conditions (alike min and max occurrences, or the presence or absence of 
 * specific data) were met in the mes-sage (for which a named condition is associated) 
 * and thus give processing instructions to whoever will handle the XML output.
 * </ol>
 * Evaluating a mark is almost the same process than verifying a named condition 
 * but there are noteworthy differences:<ul>
 * <li>The evaluation is performed on the fly and not once the parsing is completed like 
 * named conditions. The evaluation is thus performed with whatever named condition 
 * tokens are already available at the point where it is evaluated, and at the depth 
 * that is that of the MARK element itself.
 * <li>A mark never throws nor records an exception: it is evaluated and the result 
 * of such evaluation could only be true or false. A corresponding value is inserted 
 * matching the true or false result, and that is all.
 * <li>The reserved value "NULL" may used to suppress the production of an output 
 * XML element in case of true or false outcome (both of them at the same 
 * time would not make sense but yield a no-operation).
 * </ul>
 * 
 * @author bernardH
 *
 */
final class MARKDefinition extends GSDDefinition {

	//additional fields
	
	
	/**
	 * verification pattern to apply to the condition. 
	 * <p>
	 * The evaluation is the result of:<br>
	 * <code>Pattern.compile(pattern).matcher(depthString).matches()</code><br>
	 * and yields TRUE or FALSE. A data element is then inserted in the parsed
	 * message using the specified XMLtag and the associated yes-string or no-string
	 * according to the TRUE / FALSE outcome of the pattern evaluation.
	 * <p>
	 * for more details on Condition evaluations, please look into
	 * {@link CONDDefinition#verifPattern Named conditions pattern verification principles}. 
	 */
	String evalPattern = "";
	
	/**
	 * The yes-string is generated as XML element value when 
	 * the {@link #evalPattern evaluation} yields TRUE.
	 * <p>
	 * The reserved value "NULL" may used to suppress the production of an output 
	 * XML element.
	 */
	String yesString = "TRUE";
	
	/**
	 * The no-string is generated as XML element value when 
	 * the {@link #evalPattern evaluation} yields FALSE.
	 * <p>
	 * The reserved value "NULL" may used to suppress the production of an output 
	 * XML element.
	 */
	String noString = "FALSE";
	
	
	
	MARKDefinition(Definition refDef) {
		super(refDef);
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
	MARKDefinition(int _linenb, String line, final HashMap nCond, Definition refDef) throws ParserException {
		super(refDef);
		
		final String _MARK_tag = "^MARK\\s+([@\\w][\\w-]*)\\s+(.*)$";
		final String _COND_cname_pattern = "^COND\\s+(\\w+)\\s+([^\\s]{1})(.*?)\\2\\s+(.*)$";
		final String _yesString_noString = "^\"(.*?)\"\\s+\"(.*?)\"\\s*(.*)$";
		Pattern p = null;
		Matcher m = null;

		String restOfLine = this.setDepth(line);
		this.atDEFLineNb = _linenb;
		
		p = Pattern.compile(_MARK_tag);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("MARK",_linenb,line,"MARK <tag> ...");
		//m.reset();
		this.xmltag = m.group(1);
		restOfLine = m.group(2);		
//		Generalised use of NOTAG (sept 2009)
//		//must ensure that the xmltag is not 'NOTAG'
//		if (this.xmltag.equals("NOTAG"))
//			throw new ParserException.DEFErrorNOTAGNotAllowed(_linenb,line,"MARK");		
		p = Pattern.compile(_COND_cname_pattern);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("MARK",_linenb,line,"MARK ... COND <name> \"<pattern>\" ...");
		//m.reset();
		this.conditionName = m.group(1);
			//validate condition name against declared set 
			if (!nCond.containsKey(this.conditionName))
				throw new ParserException.DEFErrorBadCONDName("MARK",_linenb,line,this.conditionName);
			//m.group(2) is the character used for the quote
			this.evalPattern = m.group(3);
			//ensure that the pattern can be compiled
			Pattern.compile(this.evalPattern).matcher("");
			restOfLine = m.group(4);

			p = Pattern.compile(_yesString_noString);
			m = p.matcher(restOfLine);
			if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("MARK",_linenb,line,"MARK ... \"<value if true>\" \"<value if false>\"");
			//m.reset();
			this.yesString = m.group(1);
			this.noString = m.group(2);
			restOfLine = m.group(3);
		
			//rest of line shall be empty for a MARK
			if (restOfLine.length()!=0) throw new ParserException.DEFErrorExtraChars("MARK",_linenb,line,"... \"<value if false>\"");
	}
	
	public String toString() {
		//nesting depth is limited to 50 in Definiton class
		String s = new String("|||||||||||||||||||||||||||||||||||||||||||||||||||").substring(0, depth);
		s = s.concat("MARK "+xmltag+" ");
		s = s.concat("COND "+conditionName+" \""+evalPattern+"\" ");
		s = s.concat("\""+yesString+" \""+noString+"\"");
		return (s + "\n");
	}
	/* (non-Javadoc)
	 * @see com.reverseXSL.parser.GSDDefinition#getName()
	 */
	String getName() {
		return "MARKDefinition";
	}


}
