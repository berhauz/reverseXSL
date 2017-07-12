package com.reverseXSL.parser;


import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import com.reverseXSL.types.Cardinality;
import com.reverseXSL.types.Handling;
import com.reverseXSL.types.Impact;

/**
 * Defines fields and methods common to group, segment and data element definitions.
 * 
 * @see com.reverseXSL.parser.SEGDefinition
 * @see com.reverseXSL.parser.MSGDefinition
 * @see com.reverseXSL.parser.GRPDefinition
 * @see com.reverseXSL.parser.DataDefinition
 * @see com.reverseXSL.parser.CONDDefinition
 * @see com.reverseXSL.parser.MARKDefinition
 * @see com.reverseXSL.parser.Definition
 * @author bernardH
 *
 */
abstract class GSDDefinition {

	//fields common to Groups, Segments, Data elements and Marks
	
	/**
	 * This reference to the top-level parent definition provides means of accessing
	 * the definition's global settings (e.g. release character).
	 */
	Definition parentDef = null;
	
	/**
	 * A trace of the line number of the DEF file that generated the present 
	 * MSG, SEG, GRP or Data element definition
	 */
	int atDEFLineNb = 0;
	
	/**
	 * The nesting depth of the group, segment or data element.
	 * Both groups and segments force a jump to depth+1 for all enclosed 
	 * groups, segments or data elements.<br>
	 * Depth is counted from 0 for the Message itself (==the top segment).
	 */
	int depth = 0;

	/**
	 * The XML element tag to generate when the relevant group, segment or data 
	 * element structure exists. "NOTAG" is a reserved value to indicate 
	 * that no XML tag shall be generated.
	 */
	String xmltag = "NOTAG";

	/**
	 * An optional namespace suffix for this element to be added to the base URI.
	 */
	String suffix = "";

	/**
	 * The element cardinality: Mandatory, Optional, Conditional
	 * @see Cardinality
	 */
	Cardinality cardinality = Cardinality.OPTIONAL;
	/**
	 * <u>Minimum</u> allowed occurences of the group, segment or data element.
	 * While parsing, the <code>ACC {@link #occAccept}</code> number prevails. On completion 
	 * of parsing, compliance with this official minimum is checked.
	 */
	int occMin = 0; 
	/**
	 * <u>Maximum</u> allowed occurences of the group, segment or data element
	 * While parsing, the <code>ACC {@link #occAccept}</code> number prevails. On completion 
	 * of parsing, compliance with this official maximum is checked.
	 */
	int occMax = 1;
	/**
	 * Count of occurences of the group, segment or data element
	 * that the parser will <u>accept</u> before throwing a ValidationException.
	 */
	int occAccept = 1;
	
	
	/**
	 * Tells whether to Record or Throw exceptions. Only relevant 
	 * for Mandatory or Optional GSD elements.
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
	Handling handling = Handling.RECORD;
	
	/**
	 * Indicates whether exceptions bound to the present group, segment, or
	 * data element are considered FATAL or just a Warning.
	 */
	Impact impact = Impact.WARNING;
	
	/**
	 * Compulsory description associated to the present group, segment, or
	 * data element.<p>
	 * The text is itself substructured into a keyword followed by text. 
	 * the keyword is by convention the first word (i.e. not containing space characters) 
	 * of the description text. That keyword must use only characters in 
	 * the ASCII character set. The parser will use it to find possible 
	 * text substitutes in other languages whenever a language-map object 
	 * is passed along as argument while invoking the methods that 
	 * handle exceptions.  
	 */
	String description = "--no description--";
	
	/**
	 * Names the condition associated to the present group, segment, or
	 * data element. Only relevant 
	 * for <u>Conditional</u> GSD elements. The named condition must be 
	 * declared in a COND statement.
	 * 
	 * @see CONDDefinition
	 */
	String conditionName = "";

	/**
	 * Is the pattern telling how to 'feed' the named condition. Only relevant 
	 * for <u>Conditional</u> GSD elements. The named condition must be 
	 * declared in a COND statement.
	 * <p>
	 * The pattern can be one of:<ul>
	 * <li>a plain text string without a single '(' , in which case 
	 * that is the string to feed into the condition collection.
	 * <li>a string containing at least one '(' in which case 
	 * it is interpreted as a pattern with capturing groups and the 
	 * feed-string is the concatenation of all capturing groups 
	 * of only the first pattern match loop (almost like a data element except 
	 * that the pattern is not interpreted here as possesive).
	 * </ul>
	 * @see CONDDefinition
	 */
	String conditionFeed = "";

	/**
	 * Contains the ordered list of sub-elements within the segment or group definition.
	 * <p>
	 * May contain sub-segments, sub-groups and data elements.
	 * <p>
	 * Data elements (subclass of GSDElement) also inherit this ArrayList but 
	 * leave it empty.
	 */
	@SuppressWarnings("rawtypes")
	ArrayList subElts = new ArrayList();
	
	protected GSDDefinition(Definition refDef) {
		super();
		parentDef = refDef;
	}

	
	/**
	 * Counts and removes '|' in front of a definition. Sets the depth field to the
	 * number of counted '|'.
	 * 
	 * @param line 	the original definition line
	 * @return the definition line without the '|' prefixing characters.
	 */
	protected String setDepth(String line) throws ParserException {
		for (int i=0; i<line.length();i++) {
			if (line.charAt(i)=='|') continue;
			this.depth = i;
			if (i>=Definition.MAXDEPTH) throw new ParserException.DEFErrorOverDepth(Definition.MAXDEPTH);
			return line.substring(i);
		}
		this.depth = 0;
		return line;
	}

	
	/**
	 * Supporting Method, allowing to skip all comment lines in DEF files.
	 * 
	 * @param line dynamic working variable passed to a static method to make its invocation thread-safe
	 * @param inputDEF
	 *  
	 * @return line, also passed as parameter!
	 * @throws IOException
	 */
	static String readNonCommentLine(String line, LineNumberReader inputDEF) throws IOException {
		//To make it thread-safe (and avoid static fields, the line being read must
		//also be passed as argument so that we avoid using (implicitly) static working fields
		//String line = inputDEF.readLine(); could cause bad effects if declared here
		line = inputDEF.readLine();
		while (line != null) {
			//skip empty lines and those beginning with a white space or tab
			if (line.length()<1) { line=inputDEF.readLine(); continue; }
			if (line.startsWith("\t")) { line=inputDEF.readLine(); continue; }
			if (line.startsWith(" ")) { line=inputDEF.readLine(); continue; }
			break;
		}
		return line;
	}

	/**
	 * Recursively fills up a Group or Segment body (the subElts list) with sub-group, 
	 * sub-segment and data element definitions.
	 * 
	 * @param inputDEF	input Line Number Reader on the DEF file
	 * @param atLine	current line
	 * @param atLineNb	current line number
	 * @param nCond 	passing the namedConditions table for validation
	 * @return	the next input line or null if end of file.
	 */
	@SuppressWarnings("unchecked")
	String fill(LineNumberReader inputDEF, String atLine, int atLineNb, @SuppressWarnings("rawtypes") final HashMap nCond) throws ParserException, IOException {
		//read and fill up the subElts list
		SEGDefinition sDef = null;
		DataDefinition dDef = null;
		GRPDefinition gDef = null;
		MARKDefinition mrkDef = null;
		String line = atLine;
		int lineNb = atLineNb;

		while (line!= null) {
			//return the next line (or null) to the follower whenever it is a step-back in depth
			int checkDepth = 0;
			for (int i=0; i<line.length();i++) {
				if (line.charAt(i)=='|') continue;
				checkDepth = i; break;
			}
			if (checkDepth<=this.depth) {
				//current line is a parent or sibling element
				if (this.subElts.size()<1) 
					throw new ParserException.DEFErrorMissingChild(lineNb,line);
				return line;
			}
			//more than one step-forward in depth is not acceptable
			if ((checkDepth-this.depth)>1) 
				throw new ParserException.DEFErrorBadDepth(lineNb,line);
			
			//expect a GRP, SEG or D element
			switch (line.charAt(checkDepth)) {
			case 'D':	//try load a sub-data element
				dDef = new DataDefinition(lineNb,line,nCond,parentDef);
				this.subElts.add(dDef);
				//go-on reading next sub-element of the current group or segment
				line = readNonCommentLine(line, inputDEF);
				lineNb = inputDEF.getLineNumber();			
				break;
			case 'S':	//try load a sub-segment
				sDef = new SEGDefinition(lineNb,line, nCond,parentDef);
				//fill-up body
				line = sDef.fill(inputDEF, readNonCommentLine(line, inputDEF), inputDEF.getLineNumber(), nCond);
				lineNb = inputDEF.getLineNumber();			
				this.subElts.add(sDef);
				break;
			case 'G':	//try load a sub-group
				gDef = new GRPDefinition(lineNb,line, nCond, parentDef);
				//fill-up body
				line = gDef.fill(inputDEF, readNonCommentLine(line, inputDEF), inputDEF.getLineNumber(), nCond);
				lineNb = inputDEF.getLineNumber();							
				this.subElts.add(gDef);
				break;
			case 'M':	//try load a mark
				mrkDef = new MARKDefinition(lineNb,line,nCond, parentDef);
				this.subElts.add(mrkDef);
				//go-on reading next sub-element of the current group or segment
				line = readNonCommentLine(line, inputDEF);
				lineNb = inputDEF.getLineNumber();			
				break;
			default:
				throw new ParserException.DEFErrorWhatElement(lineNb,line,"D SEG GRP MSG MARK");
			}
			
		}
		throw new ParserException.DEFErrorUnexpectedEOF(lineNb,this.xmltag,this.depth+1);
	}
	
	/**
	 * Prints only the sub element list.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();

		if (this.subElts.size()==0) return ("-no sub elements-\n");

		ListIterator iter = this.subElts.listIterator();
		while (iter.hasNext()) {
		   sb.append(iter.next().toString());
		}
		return sb.toString();
	}
		
	/**
	 * Provides a name for tracing
	 * 
	 * @return name string
	 */
	String getName() {
		return "GSDDefinition";
	}
}
