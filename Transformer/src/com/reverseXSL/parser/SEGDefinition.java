package com.reverseXSL.parser;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reverseXSL.types.Cardinality;
import com.reverseXSL.types.Handling;
import com.reverseXSL.types.Impact;


/**
 * A segment is the definition for a string of characters from the original 
 * input message that gets cut into smaller strings. 
 * <p>
 * In other words, a segment 
 * is used to cut sub-strings out of the syntax of the segment-string itself.
 * A segment always corresponds to a physical portion of the input message 
 * and corresponds altogether to the enclosed data elements and the associated 
 * framing syntax (tags, separators, terminators).
 * <p>
 * The top-level segment(by convention at level 0) is always the whole 
 * message itself.
 * <p>A segment contains by definition:<ul>
 * <li>An identification pattern that is used to identify the segment;
 * <li>A cutting pattern or a built-in function to split the segment into 
 * smaller strings. The cutting logic is driven by capturing groups in 
 * regular expressions; see explanations further.
 * </ul>A segment may be associated to an XML tag (or NULL).<br>
 * A segment may be:<ul>
 * <li>Mandatory, in which case the failure to match the identification 
 * pattern throws/records an exception
 * <li>Optional, in which case the failure to match the identification 
 * pattern is interpreted as the absence of the segment
 * <li>Conditional, in which case the constraint is similar to the 
 * Optional case, but the absence or presence is now reported to a named condition.
 * </ul>
 * A segment may bear minimum, maximum and accept loop counts that drive 
 * looping logic in addition of being special kinds of conditions. 
 * They are combined with the Mandatory / Optional and Conditional (M/O/C) keywords. 
 * A Mandatory or Optional Segment contains an error text that will be 
 * thrown/recorded with an exception in case of violation of the mandatory 
 * or optional constraints, and with invalid loop counts. The error text 
 * that is associated to Conditional rule violations is defined in the named 
 * condition statement.
 * <p>
 * A segment also bears an indicator whether to actually Record or to 
 * Throw any exception.
 *
 */
class SEGDefinition extends GSDDefinition {

	//additional fields
	
	/**
	 * Identification Pattern for the segment. The segment is identified only if
	 * <code>Pattern.compile(idPattern).matcher(segment).find()</code>
	 * yields TRUE and further cutting and parsing of the segment contents 
	 * itself is also successful.<br>
	 * In other words, a segment must be matched entirely to the thinner 
	 * level of details in order to consider that an instance of it 
	 * has been "found" in the source message. This <b>strict matching philosophy</b> 
	 * allows matching input against the distinguished segment definitions in a 
	 * sequence of similar structures with only tiny differences in content 
	 * but exactly the same segment tag for instance.
	 */
	String idPattern = "";
		
	
	/**
	 * The cut function tells how to split a segment into sub-segments. The various Cut functions 
	 * are implemented as anonymous inner classes that implement this abstract inner class.
	 * <p>
	 * <b>Terminator or Delimiter?</b><br>
	 * CUT functions assume a <u>delimiter</u> semantic (hence "&lt;sep&gt;data&lt;sep&gt;" 
	 * yields "" and "data" and "") when applied to <b>segments</b>, and a <u>terminator</u> semantic when 
	 * applied to the <b>message</b> itself (the top-level segment indeed).
	 * For instance a CUT-ON-(') of the simple Message "'data'" yields two elements: "" and "data".
	 * <p>
	 * <b>CUT function specs:</b><br>
	 * A cut function is either <ul>
	 * <li>a simple function that is based on a given delimiter (e.g.
	 * a specified character like '/' ',' ':' or '+';
	 * <li>else a more complex function based on a regular expression.
	 * </ul>
	 * <b><u>The simple cut functions</u></b> are:<dl>
	 * <dt>CUT-ON-(x) <dd>The function uses the specified <u>single</u> character x 
	 * as cutting mark.<br>
	 * Example:<ul>
	 * <li>CUT-ON-(/)
	 * <li>CUT-ON-(.)
	 * <li>CUT-ON-(*)
	 * <li>CUT-ON-(+)
	 * <li>CUT-ON(:)
	 * </ul>Any single printable ASCII character value can be used.
	 * 
	 * <dt>CUT-ON-NL <dd>The function cuts the given segment into as many sub-segments as
	 * lines in the input string. Any of CR, CRLF, LF or FF are 
	 * valid line terminators (CR=\u000D, LF=\u000A, FF=\u000C).<br>
	 * 
	 * <dt>CUT-ON-1NBSP <dd>The function cuts the given segment into as many sub-segments as
	 * fields separated by <u>single</u> non-breaking space chars in the input line reader. 
	 * Both tab and space characters are valid separators.<br>
	 * This function assumes a single space char as delimiter, hence will generate an empty segment 
	 * whenever two space characters follow each other.
	 * 
	 * <dt>CUT-ON-RNBSP <dd>The function cuts the given segment into as many sub-segments as
	 * fields separated by <u>one or multiple</u> non-breaking space chars in the input line reader. 
	 * Both tab and space characters are valid separators.<br>
	 * This function assumes that following space characters (from 1 to any count) stand as a single delimiter;
	 * In other words, any commbination of tabs and space characters is equivalent to a single space character.
	 * 
	 * <dt>CUT-ON-TAB <dd>The function cuts the given segment assuming that a single tab character is a separator.
	 * Therefore, leading, successive and trailing tabs yield empty fields.
	 * 
	 * <dt>CUT-FIXED-(n) <dd>where n is a integer. This function cuts the segment in fixed size
	 * elements of n characters (not bytes, think Unicode).Example:<ul>
	 * <li>CUT-ON-(3)
	 * <li>CUT-ON-(15)
	 * <li>CUT-ON-(1000)
	 * </ul>Attention: the last sub-element may contain less than n characters.
	 * 
	 * <dt>CUT-ON-"&lt;regex&gt;" <dd>where regex is the specification of the <b>separator</b>. This function
	 * allows specifying a set of different characters as separator, or a combination of several characters.
	 * Example:<ul>
	 * <li>CUT-ON-"[./]" cuts a segment on every single '.' or single '/' 
	 * <li>CUT-ON-"--" will cut a segment on sequences of two hyphens '--'.
	 * <li>CUT-ON-"[ \t]+" is equivalent to CUT-ON-RNBSP
	 * </ul>
	 * 
	 * </dl>
	 * <p>
	 * <b><u>The complex cut functions</u></b> are based on a regular expression.
	 * They are simply specified as <code>CUT "&lt;pattern&gt;"</code><br>
	 * There are three possible cutting modes:<dl>
	 * <dt>Repeating Pattern cutting mode:<dd>
	 * In such mode, only capturing group 0 (the pattern itself) is used and the pattern 
	 * itself would be selected such as to repeat from the beginning to the end of the segment. 
	 * The successive matching elements of the original segment do define as many 
	 * sub-segments/data elements.<br>
	 * For instance the pattern is: "/[^/]*", and the segment is: "DIM/12345//ABCDEF/XYZ ABC".
	 * It yields the following segment pieces: "/12345", "/", "/ABCDEF", and "/XYZ ABC". 
	 * The "DIM" chunk is left out of the inventory, which is OK if DIM is the segment tag. 
	 * However, in some cases, the first element is also a data element.<br> 
	 * The following variant pattern allows to capture it: "/[^/]*|^[^/]*", with the 
	 * segment: "LUX/12345//ABCDEF/XYZ ABC", yields the following pieces: "LUX", "/12345", 
	 * "/", "/ABCDEF", and "/XYZ ABC".<br>
	 * We shall observe that in both cases the syntax character "/" is still part of 
	 * the generated data elements.
	 * <dt>Straight Capturing-Group cutting mode:<dd>
	 * The pattern must contain capturing groups (i.e. there's at least on pair of "()" in 
	 * the pattern string) and capturing groups 1 to n (indeed not group 0 which is 
	 * the pattern itself) and respectively yielding the successive matching elements from 
	 * the original segment. Nested capturing groups are ignored.<br>
	 * Example: the pattern is: "^DIM/(.*?)/(.*?)/((.).*?)/(.*)$", and the segment 
	 * is: "DIM/12345//ABCDEF/XYZ ABC". It yields the following matching groups and thus 
	 * four segment pieces: G1:"12345", G2:"", G3:"ABCDEF", G4 is ignored (nested), G5:"XYZ ABC".<br>
	 * We shall observe that the syntax characters "DIM" and then "/" are not part of 
	 * the generated data elements.
	 * <dt>Repeating Capturing-Group cutting mode:<dd>
	 * This mode combines the above two. Group 0 (the pattern match itself) is excluded from 
	 * the inventory of data elements in proper but only rep-resenting the pattern-matching 
	 * loop. Data elements are generated from group 1 to n within the group 0 loop. Nested 
	 * capturing groups are ignored.<br>
	 * Example: the pattern is: "(...)-([^/]*)/?", with the segment: 
	 * "123-4ABCDEF/456-XYZABC000/789-BBBCCC", we get a total of six data elements:<ul>
	 * <li>in pattern-loop 1: G1:"123", G2:"4ABCDEF"
	 * <li>in pattern-loop 2: G1:" 456", G2:" XYZABC000"
	 * <li>in pattern-loop 3: G1:" 789", G2:" BBBCCC"
	 * </ul>
	 * </dl>
	 * 
	 * @see #cutFunction
	 */
	abstract class CutFunction {
		final String fname;
		final String cutPattern;
		
		CutFunction(final String n, final String cp) {
			fname = n; cutPattern = cp;
		}
		
		class CutContext {
			//holds a cut context in order to support hasNext() and getNext() methods from call to call.
			int currentOffset = 0;
			int lastOffset = 0;
			int currentLineNb = 0;
			int lastLineNb = 0;
			String rest = null;
			Matcher matcher = null; //for the CUT "<pattern>" and CUT-ON-"<regex>" functions
			int nextGroupNb = 0;
			int lastEnd = 0;
			
			CutContext(String d,int cl, int co) {
				currentOffset = co;
				lastOffset = currentOffset;
				currentLineNb = cl;
				lastLineNb = currentLineNb;
				rest = d;
			}
		}
		/**
		 * Is a CutContext factory. Creates a cut context for a cut of a segment into (sub-)pieces.
		 * <p>
		 * Design note: the cut context cannot be stored in the CutFunction class itself because
		 * potential thread conflicts could occur whenever an indentical {@link Definition} is shared by multiple 
		 * {@link Parser}'s.
		 * 
		 * @param dataIn	the input string to cut
		 * @param startLineNb	the line offset applicable to the first line from the input (for tracing)
		 */
		CutContext cut(String dataIn, int startLineNb) {
			return new CutContext(dataIn,startLineNb,0);
		}

		/**
		 * Variant of {@link com.reverseXSL.parser.SEGDefinition.CutFunction#cut(String, int)}.
		 * 
		 * @param dataIn	the input string to cut
		 * @param startLineNb	the line offset applicable to the first line from the input (for tracing)
		 * @param startOffset 	the character offset within the line
		 */
		CutContext cut(String dataIn, int startLineNb, int startOffset) {
			return new CutContext(dataIn,startLineNb,startOffset);
		}

		
		/**
		 * Facilitates loop implementation using getNext(CutContext).
		 * <p>
		 * The method {@link com.reverseXSL.parser.SEGDefinition.CutFunction#cut(String, int)} <u>must</u> be invoked once 
		 * before this one in order to initiate a new cut.
		 * 
		 * @param cc	a cut context
		 * @return	true when more data pieces from the cut are available (can be the empty string, once!)
		 */
		boolean hasNext(CutContext cc) {
			return (cc.rest!=null);
		}
		
		/**
		 * To get data pieces from the cut, one after each other.
		 * <p>
		 * The method {@link com.reverseXSL.parser.SEGDefinition.CutFunction#cut(String, int)} <u>must</u> be invoked once 
		 * before this one in order to initiate a new cut.
		 * 
		 * @param cc	a cut context
		 * @return	the next 'cut' piece from the original data (may be ""), or null if the cut is finished.
		 */
		abstract String getNext(CutContext cc) ;
	
		
		/**
		 * To get relevant data piece offsets (relative to line start) from the cut, one after each other.
		 * 
		 * @param cc	a cut context
		 * @return the offset applicable to the last getNext(CutContext)
		 */
		int getOffset(CutContext cc) {
			return cc.lastOffset;
		}

		/**
		 * To get applicable line numbers from the cut, one after each other.
		 * 
		 * @return the line number applicable to the last getNext(CutContext)
		 */
		int getLineNb(CutContext cc) {
			return cc.lastLineNb;
		}

		
		/**
		 * Default rendering as string must be implemented by sub-classes
		 */
		public abstract String toString();
	
	}
	

	/**
	 * The one specific cut function implementation associated to this one segment 
	 * definition. 
	 * <p>
	 * Note that this implementation is generated as anonymous inner classes that extend
	 * the {@link SEGDefinition.CutFunction} abstract class.
	 * 
	 * @see SEGDefinition.CutFunction <i>inventory of the various cut functions</i>
	 */
	CutFunction cutFunction = null;
	

	
	SEGDefinition(Definition refDef) {
		super(refDef);
	}

	/**
	 * Unmarshals the specified DEF file input line passed as argument into a segment 
	 * definition object with the associated cut function implementation.
	 * 
	 * @param _linenb		(informative, for traces in exceptions) the relevant line number in the DEF file
	 * @param line			a line from the input DEF file containing a SEG specification
	 * @param refDef 		provides a link to the parent reference definition settings!
	 * @throws ValidationException
	 */
	SEGDefinition(int _linenb, String line, final HashMap nCond, Definition refDef) throws ParserException {
		super(refDef);
		final String _SEG_idpattern_tag = "^SEG\\s+([^\\s]{1})(.*?)\\1\\s+(\\w[\\w-]*)\\s+(.*)$";
		final String _MOC_min_max_ACC_acc = "^(M|O|C)\\s+(\\d+)\\s+(\\d+)\\s+ACC\\s+(\\d+)\\s*(.*)$";
		final String _RT_WF = "^(R|T)\\s*?(W|F)\\s*(.*)$";
		final String _COND_cname_cfeed = "^COND\\s+(\\w+)\\s+([^\\s]{1})(.*?)\\2\\s*(.*)$";
		final String _descr = "^\"(.*?)\"\\s*(.*)$";
		final String _suf = "^(/\\S*)\\s*(.*?)$";
		Pattern p = null;
		Matcher m = null;
		final String msgOrSeg = ((this instanceof MSGDefinition)?"MSG":"SEG");

		String restOfLine = this.setDepth(line);
		this.atDEFLineNb = _linenb;

		if (this instanceof MSGDefinition) restOfLine = restOfLine.replaceFirst("MSG","SEG");
			
		p = Pattern.compile(_SEG_idpattern_tag);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax(msgOrSeg,_linenb,line,msgOrSeg+" \"<pattern>\" <tag> ...");
		//m.reset();
		//m.group(1) is the character used for the quote
		this.idPattern = m.group(2);
		this.xmltag = m.group(3);
		restOfLine = m.group(4);
		//ensure that the id-pattern can be compiled
		try {
			Pattern.compile(this.idPattern).matcher("");
		} catch (Exception e) {
			throw new ParserException.DEFErrorInvalidRegex(this.idPattern,_linenb,e.getLocalizedMessage());
		}
		
		p = Pattern.compile(_MOC_min_max_ACC_acc);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax(msgOrSeg,_linenb,line,msgOrSeg+" ... M|O|C <min> <max> ACC <acc> ...");
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
			if (cardinality!=Cardinality.CONDITIONAL) throw new ParserException.DEFErrorCardinalityCRequired(_linenb,line,"C <min> <max> ACC <acc>",msgOrSeg);
			p = Pattern.compile(_COND_cname_cfeed);
			m = p.matcher(restOfLine);
			if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax(msgOrSeg,_linenb,line,msgOrSeg+" ... COND <name> \"<feed>\" ...");
			//m.reset();
			this.conditionName = m.group(1);
			//validate condition name against declared set 
			if (!nCond.containsKey(this.conditionName))
				throw new ParserException.DEFErrorBadCONDName("SEG",_linenb,line,this.conditionName);
			//validate compatibility between current depth and declared named condition depth
			if (this.depth<((CONDDefinition)nCond.get(this.conditionName)).depthScope)
				throw new ParserException.DEFErrorBadDepthVersusCOND("SEG",_linenb,line,this.depth,this.conditionName,((CONDDefinition)nCond.get(this.conditionName)).depthScope);				
			//m.group(2) is the character used for the quote
			this.conditionFeed = m.group(3);
			//ensure that the feed can be compiled
			try {
				Pattern.compile(this.conditionFeed).matcher("");
			} catch (Exception e) {
				throw new ParserException.DEFErrorInvalidRegex(this.conditionFeed,_linenb,e.getLocalizedMessage());
			}
			restOfLine = m.group(4);
		}
		else {
			//validate that cardinality is "M" or "O" in this case
			if (cardinality==Cardinality.CONDITIONAL) throw new ParserException.DEFErrorCardinalityMORequired(_linenb,line,"M|O <min> <max> ACC <acc>",msgOrSeg);
			p = Pattern.compile(_RT_WF);
			m = p.matcher(restOfLine);
			if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax(msgOrSeg,_linenb,line,msgOrSeg+" ... R|T W|F ...");
			//m.reset();
			this.handling = (m.group(1).equals("R")? Handling.RECORD : Handling.THROW );
			this.impact = (m.group(2).equals("W")? Impact.WARNING : Impact.FATAL );
			restOfLine = m.group(3);
		}
		
		p = Pattern.compile(_descr);
		m = p.matcher(restOfLine);
		if (!m.matches()) throw new ParserException.DEFErrorInvalidSyntax(msgOrSeg,_linenb,line,msgOrSeg+" ... \"<description>\" ...");
		//m.reset();
		this.description = m.group(1);
		restOfLine = m.group(2);

		//rest of line shall contain a cut function
		//dummy loop just for the sake of breaking after first match against a function
		while (true) {
						
			//try CUT-ON-(<single char>)
			//this is actually an optimised version of CUT-ON-"<regex>"
			p = Pattern.compile("^CUT-ON-\\((.)\\)\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				cutFunction = new CutFunction("Cut-on-char",m.group(1)) {
					public String toString() {
						return ("CUT-ON-("+this.cutPattern+")");
					}
					String getNext(CutContext cc) {
						if (cc.rest==null) return null;
						char sep = cutPattern.charAt(0);
						String piece;
						int offset = cc.rest.indexOf(sep);
						if (offset>0 && parentDef!=null && parentDef.releaseChar!=null 
								&& (cc.rest.charAt(offset-1)==parentDef.releaseChar.charValue())) {
							//release characters exist before reaching the next cut char
							//so: recalculate the offset by a progression in the 'rest' string!
							offset = -1;
							for (int i=0;i<cc.rest.length();i++)
								if (cc.rest.charAt(i)==parentDef.releaseChar.charValue()) 
									i++; //skip next char, whatever it is
								else if (cc.rest.charAt(i)==sep) {
									offset = i; break;
								}
								else continue;
						}
						if (offset<0) {
							//return it all
							piece = cc.rest;
							cc.lastOffset = cc.currentOffset;
							cc.currentOffset += cc.rest.length(); //Cumulative
							cc.rest = null;
							return piece;
						}
						cc.lastOffset = cc.currentOffset;
						cc.currentOffset += offset+1; //Cumulative
						piece = cc.rest.substring(0, offset);
						cc.rest = cc.rest.substring(offset+1);
						//apply a terminator semantic in case of the message and delimiter in case of segment
						if ((cc.rest.length()<=0)&&(msgOrSeg.equals("MSG"))) cc.rest = null;
						return piece;
					}

				};
				restOfLine = m.group(2);
				break;
			}
			
			//try CUT-ON-NL
			p = Pattern.compile("^CUT-ON-NL\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				cutFunction = new CutFunction("Cut-on-NewLine","\\n") {
					public String toString() {
						return ("CUT-ON-NL");
					}
					String getNext(CutContext cc) {
						if (cc.rest==null) return null;

						String line;
						int upto,plus;
						plus=1;
						//simplify access to release character
						Character rCh = parentDef!=null? parentDef.releaseChar:null;
						
						for (upto=0;upto<cc.rest.length();upto++) {
							//check for any release char and skip if found
							if (rCh!=null && cc.rest.charAt(upto)==rCh.charValue()) {
								//skip the next char, or CRLF together
								if ((upto+2)<cc.rest.length()
									&& cc.rest.charAt(upto+1)=='\r'
									&& cc.rest.charAt(upto+2)=='\n')
									upto+=2;
								else upto+=1;
								continue;
							}
							//compute index up to first CR or LF or FF
							switch (cc.rest.charAt(upto)) {
							case ('\r'):
								if (((upto+1)<cc.rest.length())&&(cc.rest.charAt(upto+1)=='\n')) plus=2;
							case ('\n'):
							case ('\f'): {
								line = cc.rest.substring(0,upto);
								cc.lastLineNb = cc.currentLineNb;
								cc.currentLineNb++;
								cc.rest = cc.rest.substring(upto+plus);
								//apply a terminator semantic in case of the message and delimiter in case of segment
								if ((cc.rest.length()<=0)&&(msgOrSeg.equals("MSG"))) cc.rest = null;
								cc.lastOffset = cc.currentOffset;
								cc.currentOffset =0;
								return line;
							}
							default:
								continue;
							}
						}
						//no line terminator found, return the rest
						line = cc.rest;
						cc.lastOffset = cc.currentOffset;
						cc.currentOffset += cc.rest.length(); //Cumulative
						cc.rest = null;
						cc.lastLineNb = cc.currentLineNb;
						cc.currentLineNb++;
						return line;
					}
				};
				restOfLine = m.group(1);
				break;
			}

			//try CUT-FIXED-(<size>)
			p = Pattern.compile("^CUT-FIXED-\\((\\d*)\\)\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				cutFunction = new CutFunction("Cut-fixed-size",m.group(1)) {
					public String toString() {
						return ("CUT-FIXED-("+this.cutPattern+")");
					}
					String getNext(CutContext cc) {
						if (cc.rest==null) return null;
						int size = Integer.parseInt(cutPattern);
						int lgth = cc.rest.length();
						String piece;
						if (lgth<=size) {
							//return it all
							piece = cc.rest;
							cc.lastOffset = cc.currentOffset;
							cc.currentOffset += lgth; //cummulative
							cc.rest = null;
							return piece;
						}
						cc.lastOffset = cc.currentOffset;
						cc.currentOffset += size; //cummulative
						piece = cc.rest.substring(0, size);
						cc.rest = cc.rest.substring(size);
						return piece;
					}

				};
				restOfLine = m.group(2);
				break;
			}
			

			//try CUT-ON-1NBSP
			p = Pattern.compile("^CUT-ON-1NBSP\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				cutFunction = new CutFunction("Cut-on-One-NonBreakingSpace","[ \\t]") {
					public String toString() {
						return ("CUT-ON-1NBSP");
					}
					String getNext(CutContext cc) {
						if (cc.rest==null) return null;

						String piece;
						int upto;
						//simplify access to release character
						Character rCh = parentDef!=null? parentDef.releaseChar:null;
						
						for (upto=0;upto<cc.rest.length();upto++) {
							//check for any release char and skip if found
							if (rCh!=null && cc.rest.charAt(upto)==rCh.charValue()) {
								//skip the next char
								upto+=1;
								continue;
							}
							//compute index up to first SPACE or TAB
							switch (cc.rest.charAt(upto)) {
							case (' '):
							case ('\t'): {
								piece = cc.rest.substring(0,upto);
								cc.rest = cc.rest.substring(upto+1);
								//apply a terminator semantic in case of the message and delimiter in case of segment
								if ((cc.rest.length()<=0)&&(msgOrSeg.equals("MSG"))) cc.rest = null;
								cc.lastOffset = cc.currentOffset;
								cc.currentOffset += (upto + 1);
								return piece;
							}
							default:
								continue;
							}
						}
						//no space or tab separator found (or rest was the empty string): return the rest
						piece = cc.rest;
						cc.lastOffset = cc.currentOffset;
						cc.currentOffset += cc.rest.length(); //cumulative
						cc.rest = null;
						return piece;
					}
				};
				restOfLine = m.group(1);
				break;
			}

			//try CUT-ON-RNBSP
			p = Pattern.compile("^CUT-ON-RNBSP\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				cutFunction = new CutFunction("Cut-on-Repeating-NonBreakingSpace","[ \\t]*") {
					public String toString() {
						return ("CUT-ON-RNBSP");
					}
					String getNext(CutContext cc) {
						if (cc.rest==null) return null;

						String piece;
						int upto = 0;
						int plus = 0;
						//simplify access to release character
						Character rCh = parentDef!=null? parentDef.releaseChar:null;
						
						while ((upto+plus)<cc.rest.length()) {
							//compute index up to first SPACE or TAB and the 'plus' to the next non-space and non-tab
							if (plus==0) {
								//check for any release char and skip if found
								if (rCh!=null && cc.rest.charAt(upto)==rCh.charValue()) {
									//skip the next char
									upto+=2;
									continue;
								}
								//look for the next SPACE or TAB as separator
								if ((cc.rest.charAt(upto)==' ') || (cc.rest.charAt(upto)=='\t')) {
									plus=1;
									continue;
								}
								upto++;
								continue;
							} else {
								//we look for the next printable char
								if ((cc.rest.charAt(upto+plus)==' ') || (cc.rest.charAt(upto+plus)=='\t')) {
									plus++;
									continue;
								}
								break;
							}
						}
						if (plus==0) {
							//no separator was found
							piece = cc.rest;
							cc.lastOffset = cc.currentOffset;
							cc.currentOffset += cc.rest.length(); //cumulative
							cc.rest = null;
							return piece;
						}
						//we found one or more space chars as separator
						piece = cc.rest.substring(0,upto);
						cc.rest = cc.rest.substring(upto+plus);
						//apply a terminator semantic in case of the message and delimiter in case of segment
						if ((cc.rest.length()<=0)&&(msgOrSeg.equals("MSG"))) cc.rest = null;
						cc.lastOffset = cc.currentOffset;
						cc.currentOffset += (upto + plus);
						return piece;
					}
				};
				restOfLine = m.group(1);
				break;
			}

			//try CUT-ON-TAB
			p = Pattern.compile("^CUT-ON-TAB\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				cutFunction = new CutFunction("Cut-on-Tab","\\t") {
					public String toString() {
						return ("CUT-ON-TAB");
					}
					String getNext(CutContext cc) {
						if (cc.rest==null) return null;

						String piece;
						int upto;
						//simplify access to release character
						Character rCh = parentDef!=null? parentDef.releaseChar:null;
						
						for (upto=0;upto<cc.rest.length();upto++) {
							//check for any release char and skip if found
							if (rCh!=null && cc.rest.charAt(upto)==rCh.charValue()) {
								//skip the next char
								upto+=1;
								continue;
							}
							//compute index up to first TAB
							switch (cc.rest.charAt(upto)) {
							case ('\t'): {
								piece = cc.rest.substring(0,upto);
								cc.rest = cc.rest.substring(upto+1);
								//apply a terminator semantic in case of the message and delimiter in case of segment
								if ((cc.rest.length()<=0)&&(msgOrSeg.equals("MSG"))) cc.rest = null;
								cc.lastOffset = cc.currentOffset;
								cc.currentOffset += (upto + 1);
								return piece;
							}
							default:
								continue;
							}
						}
						//no tab found (or rest was the empty string): return the rest
						piece = cc.rest;
						cc.lastOffset = cc.currentOffset;
						cc.currentOffset += cc.rest.length(); //cumulative
						cc.rest = null;
						return piece;
					}
				};
				restOfLine = m.group(1);
				break;
			}

			
			//try CUT-ON-"<regex>"
			p = Pattern.compile("^CUT-ON-([^\\s]{1})(.*?)\\1\\s*(.*)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				cutFunction = new CutFunction("Cut-on-Pattern-as-Separator",m.group(2)) {
					public String toString() {
						return ("CUT-ON-\""+this.cutPattern+"\"");
					}

					CutContext cut(String dataIn, int startLineNb) {
						return cut(dataIn, startLineNb,0);
					}


					CutContext cut(String dataIn, int startLineNb, int startOffset) {
						CutContext cc = super.cut(dataIn, startLineNb, startOffset);
						Pattern pattern = Pattern.compile(this.cutPattern);
						cc.matcher = pattern.matcher(dataIn);
						return cc;
					}

					String getNext(CutContext cc) {
						if (cc.rest==null) return null;
						String piece = "";
						String sep = "";
						int offset = 0;
						int lineOffset = 0;
						//simplify access to release character
						Character rCh = parentDef!=null? parentDef.releaseChar:null;
						
						while (cc.matcher.find()) {
							//check for the case of a false match when 
							//		a) the release char is defined
							//	AND b) the match is actually 1 char long
							//	AND c) there is a just preceding release character
							//  AND d) an odd count of consecutive release characters actually precede it
							if (rCh!=null 
									&& cc.matcher.end()-cc.matcher.start()==1
									&& cc.matcher.start()>0 
									&& cc.rest.charAt(cc.matcher.start()-1)==rCh.charValue()) {
								//if there is an odd sequence of release characters actually preceding the match
								//then this is a false match
								int count = 1;
								while (cc.matcher.start()-count>0 
									&& cc.rest.charAt(cc.matcher.start()-count-1)==rCh.charValue())
									count++;
								if (count%2==1)	continue;
							}							
							//we found a valid separator matching the pattern
							piece = cc.rest.substring(cc.lastEnd, cc.matcher.start());
							sep = cc.matcher.group();
							offset = piece.length()+ sep.length();
							cc.lastEnd = cc.matcher.end();
							//apply a terminator semantic in case of the message and delimiter in case of segment
							if ((cc.lastEnd>=cc.rest.length())&&(msgOrSeg.equals("MSG"))) cc.rest = null;
							//update the lineOffset if the extracted separator contained LF's
							//count them first
							for (int i=0;i<sep.length();i++) 
								if (sep.charAt(i)=='\n') lineOffset++;
							//then update
							if (lineOffset>0) {
								cc.lastLineNb = cc.currentLineNb;
								cc.currentLineNb += lineOffset;
								cc.lastOffset = cc.currentOffset;
								cc.currentOffset = sep.length() - sep.lastIndexOf('\n') - 1; //for any extra chars after the linefeed
							}
							else {
								cc.lastOffset = cc.currentOffset;
								cc.currentOffset += offset; //cumulative
							}
							return piece;
						}

						//ELSE, no pattern found, return it all as the last piece
						piece = ((cc.lastEnd>=cc.rest.length())?"":cc.rest.substring(cc.lastEnd));
						cc.rest = null;
						offset = piece.length();
						cc.lastOffset = cc.currentOffset;
						cc.currentOffset += offset; //cumulative
						return piece;

					}

				};
				//ensure that the cut-pattern can be compiled
				try {
					Pattern.compile(m.group(2)).matcher("");
				} catch (Exception e) {
					throw new ParserException.DEFErrorInvalidRegex(m.group(2),_linenb,e.getLocalizedMessage());
				}
				restOfLine = m.group(3);
				break;		
			}
					
			

			//try CUT "<pattern>"
			p = Pattern.compile("^CUT\\s+([^\\s]{1})(.*?)\\1\\s*(.*?)$");
			m = p.matcher(restOfLine);
			if (m.matches()) {
				cutFunction = new CutFunction("Cut-by-Matching-Pattern",m.group(2)) {
					public String toString() {
						return ("CUT \""+this.cutPattern+"\"");
					}
					
					
					CutContext cut(String dataIn, int startLineNb) {
						return cut(dataIn, startLineNb,0);
					}


					CutContext cut(String dataIn, int startLineNb, int startOffset) {
						CutContext cc = super.cut(dataIn, startLineNb, startOffset);
						Pattern pattern = Pattern.compile(this.cutPattern);
						cc.matcher = pattern.matcher(cc.rest);
						if (cc.matcher.find()) cc.rest="TRUE";
						else cc.rest= null;
						cc.lastOffset = startOffset; //frozen to this value in the present case
						cc.nextGroupNb = 0;
						if (cc.matcher.groupCount()>0) cc.nextGroupNb=1;
						return cc;
					}

					private void calculateNextGroup(CutContext cc) {
						cc.nextGroupNb++;
						if (cc.nextGroupNb>cc.matcher.groupCount()) {
							cc.nextGroupNb=1;
							if (cc.matcher.find())
								cc.rest = "TRUE";
							else
								cc.rest = null;
							
						}
						return;
					}
					
					String getNext(CutContext cc) {
						if (cc.rest==null) return null;
						String s;
						if (cc.nextGroupNb>0) {
							//only consider capturing groups (not group(0)), in possibly repeating pattern
							//but we still need to skip nested capturing groups
							if (cc.matcher.start(cc.nextGroupNb)<cc.lastEnd) {
								//this group starts before the end of the last one, skip it
								//or this (optional?)group is null in which case .start==-1
								this.calculateNextGroup(cc);
								return this.getNext(cc);
							}
							else {
								s = cc.matcher.group(cc.nextGroupNb);
								cc.currentOffset = cc.matcher.start(cc.nextGroupNb)+cc.lastOffset;
								cc.lastEnd = cc.matcher.end(cc.nextGroupNb);
								//position for next group
								this.calculateNextGroup(cc);
								return s;
							}

						}
						else {
							//repeating pattern cutting mode, no capturing groups but group(0)
							s = cc.matcher.group(0);
							cc.currentOffset = cc.matcher.start(0)+cc.lastOffset;
							if (cc.matcher.find()) cc.rest="TRUE";
							else cc.rest= null;
							return s;
						}
						
					}

					int getOffset(CutContext cc) {
						return cc.currentOffset;
					}
						
				};
				//ensure that the cut-pattern can be compiled
				try {
					Pattern.compile(m.group(2)).matcher("");
				} catch (Exception e) {
					throw new ParserException.DEFErrorInvalidRegex(m.group(2),_linenb,e.getLocalizedMessage());
				}
				restOfLine = m.group(3);
				break;		
			}

			//no CUT function specs has been found
			throw new ParserException.DEFErrorBadCUTFunction(msgOrSeg,_linenb,restOfLine);
		};
		//all breaks above jump hereafter
		//we are left with loading any optional namespace suffix
		p = Pattern.compile(_suf);
		m = p.matcher(restOfLine);
		if (m.matches()) {
			if (this.xmltag.equals("NOTAG"))
				throw new ParserException.DEFErrorNOTAGwithNamespace(msgOrSeg,_linenb,restOfLine);
			this.suffix = m.group(1);
			restOfLine = m.group(2);
		}
		//rest of line shall be empty for a segment
		if (restOfLine.length()!=0) throw new ParserException.DEFErrorExtraChars(msgOrSeg,_linenb,line,"... <CUT-specs> [/<suffix>]");	
	}
	
	public String toString() {
		//nesting depth is limited to 50 in Definition
		String s = new String("|||||||||||||||||||||||||||||||||||||||||||||||||||").substring(0, depth);
		if (this instanceof MSGDefinition) s = s.concat("MSG \""+idPattern+"\" ");
		else s = s.concat("SEG \""+idPattern+"\" ");
		s = s.concat(xmltag+" ");
		s = s.concat(cardinality.toString()+" "+occMin+" "+occMax+" ACC "+occAccept+" ");
		if (cardinality.equals(Cardinality.CONDITIONAL)) s = s.concat("COND "+conditionName+" \""+conditionFeed+"\" ");
		else s = s.concat(handling.toCode()+" "+impact.toCode()+" \""+description+"\" ");
		s = s.concat(cutFunction.toString());
		if (suffix.length()>0) s = s.concat(" "+suffix);
		return (s + "\n" + super.toString());
	}
	
	/* (non-Javadoc)
	 * @see com.reverseXSL.parser.GSDDefinition#getName()
	 */
	String getName() {
		return "SEGDefinition";
	}



}
