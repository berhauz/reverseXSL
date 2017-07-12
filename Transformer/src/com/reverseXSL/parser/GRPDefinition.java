package com.reverseXSL.parser;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reverseXSL.types.Cardinality;
import com.reverseXSL.types.Handling;
import com.reverseXSL.types.Impact;

/**
 * A group is pure virtual structure used to bind a collection of segments (and then sub-groups) 
 * into a kind of association.
 * <p>
 * Groups are only a structural concept. They are used to associate conditions or tags to a 
 * group of segments (and sub-groups) instead of isolated segments, and to mark loop 
 * boundaries over the relevant group of segments.
 * <p>
 * A group has no syntax element of the input message associated to the group structure itself. 
 * It does exist only indirectly from the collection of underlying segments. A group may 
 * contain a single segment (with no tag) if so desired to indirectly associate some syntax 
 * framing to the group itself.
 * <p>The use of a group has a fivefold effect:<ul>
 * <li>It is always a level breaker; precisely, all direct group members are by definition 
 * down one nesting level.
 * <li>If any of the group members exist, the group itself will exist and may introduce 
 * a corresponding 'group' tag (an XML element name of type complex: sequence) into 
 * the output XML document
 * <li>A group can repeat and therefore define looping constructs than span over 
 * more than one segment.
 * <li>The group associates a Mandatory / Optional or Conditional constraint to its members 
 * as a whole, with possible min/max loop counts.
 * <li>A specific error text can be associated to a group.
 * </ul>A group may bear an identification pattern. This is never a requirement, but just 
 * a facility to immediately enter or skip a group structure (according to the 
 * match/non-match outcome) such as to speed up parsing. The use of a group identification 
 * pattern can become the origin of an exception, and thus the associated error text may 
 * differ from the case the identification pattern would not have been used at the 
 * group level but indirectly at the segment level.
 * <p>
 * A group has Min, Max and Accept loop counts alike a segment, and a Mandatory, an 
 * Optional or a Conditional cardinality.
 * <p><b>The Message itself as a whole is <u>not</u> a top-level group but a top-level segment.</b>
 *  
 */
final class GRPDefinition extends GSDDefinition {

	//additional fields
	
	/**
	 * Optional Identification Pattern for the group. When present, the group is identified only if
	 * <code>Pattern.compile(idPattern).matcher(segment).matches()</code> yields true.
	 * This pattern may be empty in which case it is ignored.
	 */
	String idPattern = "";
	

	GRPDefinition(Definition refDef) {
		super(refDef);
	}
	
	/**
	 * Unmarshals the specified DEF file input line passed as argument into a group 
	 * definition object.
	 * 
	 * @param _linenb		(informative, for traces in exceptions) the relevant line number in the DEF file
	 * @param line	a line from the input DEF file containing a SEG specification
	 * @param refDef 		provides a link to the parent reference definition settings!
	 * @throws ValidationException
	 * @throws ParserException 
	 */
	GRPDefinition(int _linenb, String line, final HashMap nCond, Definition refDef) throws ParserException {
		super(refDef);
		final String _GRP_idpattern_tag = "^GRP\\s+\"(.*?)\"\\s+(\\w[\\w-]*)\\s+(.*)$";
		final String _MOC_min_max_ACC_acc = "(M|O|C)\\s+(\\d+)\\s+(\\d+)\\s+ACC\\s+(\\d+)\\s*(.*)$";
		final String _RT_WF = "^(R|T)\\s*?(W|F)\\s*(.*)$";
		final String _COND_cname_cfeed = "^COND\\s+(\\w+)\\s+\"(.*?)\"\\s*(.*)$";
		final String _descr = "^\"(.*?)\"\\s*(.*)$";
		final String _suf = "^(/\\S*)\\s*(.*?)$";
		Pattern p = null;
		Matcher m = null;

		String restOfLine = this.setDepth(line);
		this.atDEFLineNb = _linenb;
		
		p = Pattern.compile(_GRP_idpattern_tag);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("GRP",_linenb,line,"GRP \"<pattern>\" <tag> ...");
		//m.reset();
		this.idPattern = m.group(1);
		this.xmltag = m.group(2);
		restOfLine = m.group(3);
		//ensure that the id-pattern can be compiled
		Pattern.compile(this.idPattern).matcher("");

		p = Pattern.compile(_MOC_min_max_ACC_acc);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("GRP",_linenb,line,"GRP ... M|O|C <min> <max> ACC <acc> ...");
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
			if (cardinality!=Cardinality.CONDITIONAL) throw new ParserException.DEFErrorCardinalityCRequired(_linenb,line,"C <min> <max> ACC <acc>","GRP");
			p = Pattern.compile(_COND_cname_cfeed);
			m = p.matcher(restOfLine);
			if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("GRP",_linenb,line,"GRP ... COND <name> \"<feed>\" ...");
			//m.reset();
			this.conditionName = m.group(1);
			//validate condition name against declared set 
			if (!nCond.containsKey(this.conditionName))
				throw new ParserException.DEFErrorBadCONDName("GRP",_linenb,line,this.conditionName);
			//validate compatibility between current depth and declared named condition depth
			if (this.depth<((CONDDefinition)nCond.get(this.conditionName)).depthScope)
				throw new ParserException.DEFErrorBadDepthVersusCOND("GRP",_linenb,line,this.depth,this.conditionName,((CONDDefinition)nCond.get(this.conditionName)).depthScope);				
			this.conditionFeed = m.group(2);
			//ensure that the feed can be compiled
			Pattern.compile(this.conditionFeed).matcher("");
			restOfLine = m.group(3);
		}
		else {
			//validate that cardinality is "M" or "O" in this case
			if (cardinality==Cardinality.CONDITIONAL) throw new ParserException.DEFErrorCardinalityMORequired(_linenb,line,"M|O <min> <max> ACC <acc>","GRP");
			p = Pattern.compile(_RT_WF);
			m = p.matcher(restOfLine);
			if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("GRP",_linenb,line,"GRP ... R|T W|F \"<error text>\" ...");
			//m.reset();
			this.handling = (m.group(1).equals("R")? Handling.RECORD : Handling.THROW );
			this.impact = (m.group(2).equals("W")? Impact.WARNING : Impact.FATAL );
			restOfLine = m.group(3);
		}
		
		p = Pattern.compile(_descr);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax("GRP",_linenb,line,"GRP ... \"<description>\" ...");
		//m.reset();
		this.description = m.group(1);
		restOfLine = m.group(2);

		//we are left with loading any optional namespace suffix
		p = Pattern.compile(_suf);
		m = p.matcher(restOfLine);
		if (m.matches()) {
			if (this.xmltag.equals("NOTAG"))
				throw new ParserException.DEFErrorNOTAGwithNamespace("GRP",_linenb,restOfLine);
			this.suffix = m.group(1);
			restOfLine = m.group(2);
		}

		//rest of line shall be empty for a group
		if (restOfLine.length()!=0) throw new ParserException.DEFErrorExtraChars("GRP",_linenb,line,"...\"<description>\" [/<suffix>]");

	}
	
	public String toString() {
		//nesting depth is limited to 50 in Definiton
		String s = new String("|||||||||||||||||||||||||||||||||||||||||||||||||||").substring(0, depth);
		s = s.concat("GRP \""+idPattern+"\" ");
		s = s.concat(xmltag+" ");
		s = s.concat(cardinality.toString()+" "+occMin+" "+occMax+" ACC "+occAccept+" ");
		if (cardinality.equals(Cardinality.CONDITIONAL)) s = s.concat("COND "+conditionName+" \""+conditionFeed+"\" ");
		else s = s.concat(handling.toCode()+" "+impact.toCode()+" \""+description+"\" ");
		if (suffix.length()>0) s = s.concat(" "+suffix);
		return (s + "\n" + super.toString());
	}
	/* (non-Javadoc)
	 * @see com.reverseXSL.parser.GSDDefinition#getName()
	 */
	String getName() {
		return "GRPDefinition";
	}


}
