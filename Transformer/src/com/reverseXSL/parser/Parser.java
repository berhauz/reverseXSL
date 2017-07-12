package com.reverseXSL.parser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.reverseXSL.types.Cardinality;
import com.reverseXSL.types.Handling;
import com.reverseXSL.types.Impact;

/**
 * Provides the methods to translate input character streams into XML documents.
 * The parsing is based on {@link Definition DEF files}.
 * <p>
 * Please refer to the MS-Word documentation 'ReverseXSL DEF file specs.doc' for a complete description of the 
 * Definition objects and file syntaxes handled by this parser. 
 * <br>See also {@link com.reverseXSL.parser.Definition}.
 * </p><p>
 * Design Note: Given the simplicity of a parsing 
 * environment (simply comprising a DEF file) we have not associated a ParserFactory to the Parser itself. 
 * One shall simply 
 * instantiate a parser via the constructor:<br>
 * <code>myParser = new {@link #Parser(Definition, int, int) Parser}(def, maxFatal, maxExceptions);</code><br>
 * and then call it as often as desired, repeating in this case the same transformation, each time on a new message, as in:<br>
 * <code>{@link #parse(String, String, int) myParser.parse}(dataIn, ...);</code><br>
 * Note that the parse() method in proper returns a count of exceptions. Additional methods are used to inspect
 * results and get a rendering of the output, only as an XML-formatted document in the present version (additional 
 * output formats 
 * could be added in future releases). A Parser instance is a stateful object, whose state is reset 
 * at the start of any new parse() method call.
 * </p>
 * <p>The present class provides a fairly low-level API for reverse XSL transformations. Please consider the 
 * {@link com.reverseXSL.transform.TransformerFactory TransformerFactory} and 
 * {@link com.reverseXSL.transform.Transformer Transformer objects} for improved productivity.
 * </p>
 * @author bernardH
 * 
 */
public final class Parser {

	//-----------------------constants--------------------------------------------
	final static String INDENT="|  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  ";
	final static String ANONYMOUS = "anonymous";
	final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	final static String thisRelease = "2416O03010001907"; //YMDD protection mode O Vers03 Rel01 serial0001907 (matching 16 hex digits)
	//-----------------------init block-------------------------------------------
	/**
	 * Initially dedicated to dealing with licenses, the init() procedure still sets up the 
	 * default namespace and other reference info.
	 */
	final private void init() {
		//DISABLED: retrieve info from the backing store
		//cf. java.util.prefs.Preferences;
		//Preferences prefs =  Preferences.systemRoot().node("com.reverseXSL.transform");
		//date = prefs.get("date",dateFormat.format(new Date()));
		date = dateFormat.format(new Date());
		//release = prefs.get("release",thisRelease);
		release = thisRelease;
		//registeredNamespace = prefs.get("namespace","http://www.reverseXSL.com/FreeParser");
		registeredNamespace = "http://www.reverseXSL.com/FreeParser";
		//init default
		rootNamespace = "";
	}
	//-----------------------References--------------------------------------------

	static String date;

	static String release;
	
	static String registeredNamespace;
		
	//------------------------fields----------------------------------------------
	
	private final int maxFatal;
	
	private int fatalCount = 0;
	
	private final int maxAllExceptions;
	
	private int allExceptionsCount = 0;

	private final int maxSuccessiveMisMatches;
	
	private boolean removeNonRepeatNilOptElts = false; 

	//recorded exceptions list
	private ArrayList<ParserException> recExceptions;  //is ArrayList<BrokerException>

	//a list of elements recording all occurences of the GRP, SEG and Data elements
	//in the message instance that is parsed
	private ArrayList<Occurence> recMinMax; //is ArrayList<Occurence>
	
	//a map of Lists recording all string elements generated by
	//the named COND blocks in GRP, SEG and D elements when parsed
	//Design note: Lists are not nested alike the message structure but contain
	//elements indexed by an array of all occurences
	//Design note: collected tokens are first attached to their parent elements and then
	//consolidated into the present hash map.
	private HashMap<String, ArrayList> recNamedCond; //is Map(<String name>, <ArrayList<Token>>)
	
	//reference definition
	private final Definition refDef;
		
	//tagged data collection, i.e. nested array lists of tags and strings
	private TaggedMessage message;
	
	//message reference
	private String messageID;

	//applicable namespace
	private String namespace;
	
	//------------------------inner classes---------------------------------------
	
	//aggregated data pieces used as item of the map: recMinMax (Recorded MinMax)
	private class Occurence {
		int[] loop;
		int depth, atLine, atOffset;
		GSDDefinition definition;
		
		Occurence(final GSDDefinition def, final int[] lp, final int dep, int l, int o) {
			loop = new int[Definition.MAXDEPTH];
			depth = dep;
			definition = def;
			atLine = l;
			atOffset = o;
			for (int i=0; i<=depth; i++) loop[i]=lp[i];
			for (int i=depth+1; i<Definition.MAXDEPTH; i++) loop[i]=0;
		}
		
		public String toString() {
			char[] ct = new char[3*Definition.MAXDEPTH+2];
			String s="  ";
			ct[3*Definition.MAXDEPTH]=' ';
			ct[3*Definition.MAXDEPTH+1]='=';
			for (int i=0;i<Definition.MAXDEPTH;i++) {
				if (i<=depth) {
					ct[3*i]='-'; 
					if (loop[i]>99) s="**";
					else if (loop[i]>9) s=String.valueOf(loop[i]);
					else s=" "+String.valueOf(loop[i]);
					ct[1+3*i]= s.charAt(0); ct[2+3*i]= s.charAt(1);
				} else {
					ct[3*i]=' ';ct[1+3*i]=' ';ct[2+3*i]='.';
				}
			}
			s = definition.getName();
			return (new String(ct)+" "+definition.xmltag+"		"+s+" at DEF line "+definition.atDEFLineNb+"\n");
		}

	}
	
	//aggregated data pieces used as item of the map: recNamedCond (Recorded Named Conditions)
	private class Token {
		String condName; //associated name of the named condition
		int[] loop;
		int depth;
		String collected; 
		String sourceTag;
		int atLine, atOffset;
		
		Token(String cn, String clctd, int[] lp, int dep, String tag, int l, int o) {
			loop = new int[Definition.MAXDEPTH];
			depth = dep;
			collected = clctd;
			condName = cn;
			sourceTag = tag;
			atLine = l;
			atOffset = o;
			for (int i=0; i<=depth; i++) loop[i]=lp[i];
			for (int i=depth+1; i<Definition.MAXDEPTH; i++) loop[i]=0;
		}
		
		public String toString() {
			char[] ct = new char[3*Definition.MAXDEPTH+2];
			String s="  ";
			ct[3*Definition.MAXDEPTH]=' ';
			ct[3*Definition.MAXDEPTH+1]='=';
			for (int i=0;i<Definition.MAXDEPTH;i++) {
				if (i<=depth) {
					ct[3*i]='-'; 
					if (loop[i]>99) s="**";
					else if (loop[i]>9) s=String.valueOf(loop[i]);
					else s=" "+String.valueOf(loop[i]);
					ct[1+3*i]= s.charAt(0); ct[2+3*i]= s.charAt(1);
				} else {
					ct[3*i]=' ';ct[1+3*i]=' ';ct[2+3*i]='.';
				}
			}
			return (new String(ct)+" ["+collected+"]\n");
		}
		
	}

	//aggregated data pieces used as item in the collection 'taggedMessage' before tagging!
	private class UnTaggedElement {
		String data;
		int atLine = 0;
		int atOffset = 0;
		

		UnTaggedElement() {
			super();
		}
		UnTaggedElement(String s, int lin, int off) {
			data = s;
			atLine = lin;
			atOffset = off;
		}

		public String toString() {
			return ("L:"+atLine+" O:"+atOffset+" RAW:'"+data+"'\n");
		}
		
		public String toString(int indent) {
			return (INDENT.substring(0, indent*3)+this.toString());
		}
		
	}

	//aggregated data pieces used as item in the collection: taggedMessage
	//next to tagging!
	private class TaggedElement extends UnTaggedElement {
		String tag = "";
		String suffix = null;
		ArrayList<UnTaggedElement> subElts = null; 
		Parser.Token token = null; //temporary storage for the collected namedCondition token of this element
		
		TaggedElement() {
			//default constructor
			}
		
		TaggedElement(String tg, UnTaggedElement utelt) {
			this();
			data = utelt.data; //must always be overwritten in case of a data element, else is a segment
			tag = new String(tg);
			atLine = utelt.atLine;
			atOffset = utelt.atOffset;
		}

		public String toString() {
			return ("L:"+atLine+" O:"+atOffset+" ELEMENT:'"+data+"' as <"+tag+">\n");
		}
		public String toString(int indent) {
			StringBuffer sb = new StringBuffer();
			sb.append(INDENT.substring(0, indent*3)+this.toString());
			if (subElts==null) sb.append(INDENT.substring(0, indent*3+3)+"--NO SUBELTS--");
			else for (int i=0;i<subElts.size();i++)
					sb.append(subElts.get(i).toString(indent+1));
			return sb.toString();
		}

	}
	
	private class TaggedGroup extends TaggedElement {
		TaggedGroup(String tg, UnTaggedElement utelt,String nsSuffix) {
			super(tg, utelt);
			suffix = nsSuffix;
			subElts = new ArrayList<UnTaggedElement>();
		}
		public String toString() {
			return ("L:"+atLine+" O:"+atOffset+" GROUP as <"+tag+">\n");
		}

	}

	private class TaggedSegment extends TaggedElement {
		TaggedSegment() {
			super();
		}
		TaggedSegment(String tg, UnTaggedElement utelt,String nsSuffix) {
			super(tg, utelt);
			suffix = nsSuffix;
			subElts = new ArrayList<UnTaggedElement>();
		}
		public String toString() {
			return ("L:"+atLine+" O:"+atOffset+" SEGMENT:'"+data+"' as <"+tag+">\n");
		}

	}

	private class TaggedMessage extends TaggedSegment {

		TaggedMessage(String tg, int lin, int off,String nsSuffix) {
			super();
			suffix = nsSuffix;
			tag = tg;
			data = "...entire message...";
			subElts = new ArrayList<UnTaggedElement>();
			atLine = lin;
			atOffset = off;
		}
		public String toString() {
			return ("L:"+atLine+" O:"+atOffset+" MESSAGE as <"+tag+">\n");
		}

	}

	private class TaggedData extends TaggedElement {
		TaggedData(String tg, UnTaggedElement utelt) {
			super(tg,utelt);
			subElts = null;
		}
		public String toString() {
			return ("L:"+atLine+" O:"+atOffset+" DATA:'"+data+"' as <"+tag+">\n");
		}
		public String toString(int indent) {
			return(INDENT.substring(0, indent*3)+this.toString());
		}
	}
	
	private class TaggedMark extends TaggedElement {
		TaggedMark(String tg) {
			super();
			data = "";
			tag = new String(tg);
			atLine = 0;
			atOffset = 0;
		}
		public String toString() {
			return ("L:- O:- MARK:'"+data+"' as <"+tag+">\n");
		}
		public String toString(int indent) {
			return(INDENT.substring(0, indent*3)+this.toString());
		}
	
	}

	//------------------------methods-------------------------------------------
	
	/**
	 * Required but not much useful as such. 
	 * <p>
	 * This method was made public just for
	 * the sake of invoking diverse utility methods notably in RegexCheck and Definition.
	 * 
	 * @see #Parser(Definition, int, int)
	 * @see #Parser(Definition, int, int, int)
	 */
	public Parser() {
		super();
		init();
		maxFatal = 0; //default
		maxAllExceptions = 0; //default
		maxSuccessiveMisMatches = 3; //default
		refDef = null; //will not be able to do anything
		namespace = null;		
	}

	/**
	 * Initialises a new Parser object with a reference Definition and Exception handling parameters.
	 * The {@link #parse(String, LineNumberReader, int)} method can then be repetitively invoked on diverse
	 * input message data.
	 * <p>
	 * The maximum number of successive missed-element-matching before backtracking is 3 by default.
	 * See {@link #Parser(Definition, int, int, int)}.
	 * 
	 * @see Definition
	 * 
	 * @param msgDef		the message Definition object to use for parsing 
	 * @param maxFatEx		the max number of fatal exceptions that will be recorded before being thrown
	 * @param maxEx			the max number of all kinds of exceptions (including fatal ones) that will 
	 * 						be recorded before being thrown
	 */
	public Parser(final Definition msgDef, final int maxFatEx, final int maxEx) {
		super();
		init();
		{
			maxFatal = maxFatEx;
			maxAllExceptions = maxEx;		
		}
		maxSuccessiveMisMatches = 3;
		refDef = msgDef;
		namespace = null;
		setBaseNamespace(refDef.defBaseNamespace);
		//now ready to parse!
	}

	/**
	 * Variant of {@link #Parser(Definition, int, int)} that allows to 
	 * set the max number of successive segment/element matching failures after which the 
	 * parser will attempt to 'backtrack'. 
	 * <p>
	 * Backtracking means that the Parser will give-up with
	 * the current input message element (i.e. skip data and leave it un-tagged as RAW input data), 
	 * jump back (i.e. 'backtrack) to the last unmatched definition,
	 * and attempt to resume parsing from there.
	 *  
	 * @see Definition
	 * 
	 * @param msgDef		the message Definition object to use for parsing 
	 * @param maxFatEx		the max number of fatal exceptions that will be recorded before being thrown
	 * @param maxEx			the max number of all kinds of exceptions (including fatal ones) that will 
	 * 						be recorded before being thrown
	 * @param maxMisMatch	the maximum number of successive missed-element-matching after which the parser
	 * 						will attempt to resume parsing by skipping input data 
	 * 						and backtracking into the definition.
	 */
	public Parser(final Definition msgDef, final int maxFatEx, final int maxEx, final int maxMisMatch) {
		super();
		init();
		if (getNamespace("").equals("http://www.reverseXSL.com/FreeParser")) {
			maxFatal = 0;
			maxAllExceptions = 5;
		} else {
			maxFatal = maxFatEx;
			maxAllExceptions = maxEx;		
		}
		maxSuccessiveMisMatches = getNamespace("").equals("http://www.reverseXSL.com/FreeParser")? 3: maxMisMatch;
		refDef = msgDef;
		namespace = null;
		setBaseNamespace(refDef.defBaseNamespace);
		//now ready to parse!
	}

	
	/**
	 * This method must be called before parsing in itself (i.e. {@link #parse(String, LineNumberReader, int)})
	 * and would cause (if set TRUE) to remove all data elements with a NIL value
	 * that are optional or conditional elements,
	 * <b>and</b> whose matching definition indicates that the element is non repeatable (i.e. ACC 1),
	 * <b>and</b> whose minimum size requirement is >0.
	 * <p>
	 * This function is actually quite useful on messages based on the principle of positional data elements
	 * within 'segments' (e.g. EDIFACT, TRADACOMS, X12, etc.). Indeed, most positions (think 'slots')
	 * in such segments are occupied by optional/conditional data elements, all unique and distinguished by their relative position
	 * in the 'segment'. Every unoccupied position
	 * will yield a corresponding NIL data element in XML, that can be suppressed from
	 * the XML output if this method is set to TRUE.
	 * <p>
	 * NIL data elements are supressed only if they have a min/max size
	 * specification (of the kind <code>[1..15]</code> ) with a minimum of at least 1.
	 * Obviously, if 0 is an acceptable size, there's no reason to suppress the element.
	 * <p>
	 * Moreover, the element must be non-repeatable otherwise there is a risk
	 * to eat-up first and intermediate elements causing undesirable rank shifts.
	 * <p>
	 * The default value is false.
	 * 
	 * @param tf	new value for the flag
	 */
	public void removeNonRepeatableNilOptionalElements(boolean tf) {
		//default is FALSE
		removeNonRepeatNilOptElts = tf;
	}
	
	
	/**
	 * Supporting method providing a clean starting point for a new parsing
	 */
	private void reset() {
		this.message = null;
		this.messageID = "";
		this.allExceptionsCount = 0;
		this.fatalCount = 0;
		this.recExceptions = new ArrayList<ParserException>();
		this.recMinMax = new ArrayList<Occurence>();
		this.recNamedCond= new HashMap<String, ArrayList>();
		//cleaned-up and initialised!
	}
	

	/**
	 * Provides an interface to the recorded named-conditions tokens
	 * 
	 * @param tok	the token to insert in the target hash map (below)
	 * @param intoMap	target Hash Map, of type (<String>,<ArrayList(<Token>)>)
	 */
	private void insertToken(Parser.Token tok,HashMap<String, ArrayList> intoMap) {
		if (tok==null) return;
		//create if not existing yet
		if (!intoMap.containsKey(tok.condName)) intoMap.put(tok.condName, new ArrayList());
		//get the relevant array list and insert the collected token
		intoMap.get(tok.condName).add(tok);
		return;
	}

	/**
	 * Recursively travels through an entire element structure and all its childs to collect
	 * named condition tokens attached to the elements in proper.
	 * 
	 * @param elt	the structure from which to collect named conditions tokens
	 * @param intoMap	the target Hash Map, of type (<String>,<ArrayList(<Token>)>)
	 */
	private void  compileConditions(UnTaggedElement elt,HashMap<String, ArrayList> intoMap) {
		if (elt instanceof TaggedMessage) {
			if (((TaggedElement)elt).subElts==null) return;
			for (int i=0; i<((TaggedMessage)elt).subElts.size();i++) {
				//there is never a named condition attached to the message level
				//just do it for child elements
				compileConditions(((TaggedElement)elt).subElts.get(i), intoMap);
				}
			return;
		}
		if (elt instanceof TaggedData) {
			insertToken(((TaggedData)elt).token, intoMap);
			return;
		}
		if ((elt instanceof TaggedGroup)||(elt instanceof TaggedSegment)) {
			insertToken(((TaggedElement)elt).token, intoMap);
			if (((TaggedElement)elt).subElts==null) return;
			for (int i=0; i<((TaggedElement)elt).subElts.size();i++) {
				compileConditions(((TaggedElement)elt).subElts.get(i), intoMap);
				}
			return;
		}
		//two cases left: a) elt is instanceof UnTaggedElement
		//b) elt is instanceof TaggedMark
		//No named Condition can be associated if UnTagged or TaggedMark;
		//no operation, just return.
		return;
	}
	
	/**
	 * variant of the {@link #compileConditions(UnTaggedElement, HashMap)} that
	 * starts with child elements only.
	 * 
	 */
	private void  compileChildConditions(UnTaggedElement elt,HashMap<String, ArrayList> intoMap) {
		if ((elt instanceof TaggedElement)&&(((TaggedElement)elt).subElts!=null)) {
			for (int i=0; i<((TaggedElement)elt).subElts.size();i++) {
				//just do it for child elements
				compileConditions(((TaggedElement)elt).subElts.get(i), intoMap);
				}
			return;
		} else {
			//no childs
			return;
		}
	}
	
	/**
	 * Recursive methods parsing the occurence arrayList backwards to remove spurious recordings
	 * resulting from attempts of matching sub-element structures in groups or segments up to the
	 * point where no significant sub elements is found and so the tentative structure is skipped;
	 * but recordings were made...
	 * 
	 * @see #filterOccurences()
	 * 
	 * @param pocc	applicable max occurence of the parent instance
	 * @param pdep	depth of the parent instance
	 * @param iter	iterator whose current cursor position is after the element that we have to filter.
	 * 				Remember, we parse backwards!
	 */
	private void filterOccurences(int pocc, int pdep, ListIterator<Occurence> iter) {
		//much compact but very subtle piece of code...plan 1H to understand why it works...
		Occurence occElt;
		int curDep = pdep+1;
		while (iter.hasPrevious()) {
			occElt=iter.previous(); //get one
			if (occElt.depth<curDep) {
				occElt=iter.next(); //occElt is actually unchanged, only the cursor moves
				break;
			}
			if (occElt.loop[pdep]>pocc) {
				iter.remove(); 
				continue;
			}
			if (occElt.depth>pdep) {
				//an element at parentDepth+1 is always a good occElt to keep even if its own occ is 0
				//for a	 leaf, just continue
				if (occElt.definition instanceof DataDefinition) continue;
				//this element is itself a parent of something
				filterOccurences(occElt.loop[occElt.depth], curDep, iter);
				continue;
			}
			//unreachable code...look at tests against occElt.depth
		}
		return;
	}
	
	
	/**
	 *	There's a need to filter out spurious null occurences recorded for childs of Groups or segments that
	 *	in the end were not constituted, e.g.:<br><font face="Courier">
	 *	- 1- 1.................................................. = D1		DataDefinition at DEF line 13<br>
	 *	- 1- 1.................................................. = D2		DataDefinition at DEF line 14<br>
	 *	- 1- 1- 2............................................... = D3		DataDefinition at DEF line 16<br>
	 *	- 1- 1- 1- 2............................................ = D4		DataDefinition at DEF line 18<br>
	 *	- 1- 1- 2- 2............................................ = D4		DataDefinition at DEF line 18<br>
	 *	- 1- 1- 3- 2............................................ = D4		DataDefinition at DEF line 18<br>
	 *	- 1- 1- 4- 1............................................ = D4		DataDefinition at DEF line 18<br>
	 *	- 1- 1- 5- 0.. >>>>!!!REMOVE THIS ONE because 5>4!!!.. . = D4		DataDefinition at DEF line 18<br>
	 *	- 1- 1- 4............................................... = BBB		GRPDefinition at DEF line 17<br>
	 *	- 1- 1.................................................. = AAA		GRPDefinition at DEF line 15e 19<br>
	 *	- 1- 1- 1- 0.. >>>>!!!REMOVE THIS ONE because 1>0!!!.... = D1		DataDefinition at DEF line 22<br>
	 *	- 1- 1- 1- 0.. >>>>!!!REMOVE idem ...................... = BEF		DataDefinition at DEF line 23<br>
	 *	- 1- 1- 1- 0...>>>>!!!REMOVE idem ...................... = AFT		DataDefinition at DEF line 24<br>
	 *	- 1- 1- 0............................................... = DIMDUM		SEGDefinition at DEF line 21<br>
	 * </font>
	 */
	private void filterOccurences() {
		//FORMULA: parse BACKWARDS
		//and remove null occurence elements whose recorded parent occurence is greater that parent final occurence count
		if (this.recMinMax.size()==0) return; //nothing to filter
		else {
			//position cursor after the last element
			ListIterator<Occurence> iter = this.recMinMax.listIterator(this.recMinMax.size());
			//and go
			filterOccurences(1,0,iter);
			return;
		}

	}

	private void verifyOccurences() throws ParserException {
		Occurence occElt;
		int occ;
		if (this.recMinMax.size()==0) return; //nothing to check
		ListIterator<Occurence> iter = this.recMinMax.listIterator();
		while (iter.hasNext()){
			occElt=iter.next();
			occ=occElt.loop[occElt.depth];
			if (occ<occElt.definition.occMin) 
				recordException(new ParserException.ParserErrorOccBelowMin(occ,occElt.definition.occMin), occElt);
			if (occ>occElt.definition.occMax) 
				recordException(new ParserException.ParserErrorOccOverMax(occ,occElt.definition.occMax), occElt);
		}
	}
	
	private String occurenceOf(int[] lp, int upto) {
		StringBuffer sb = new StringBuffer();
		for (int i=0;i<=upto;i++) {
			sb.append("-"+lp[i]);
		}
		return sb.toString();
	}
	
	private void verifyConditions() throws ParserException {
		//loop on the named conditions
		Iterator iset;
		ListIterator ilst;
		Map.Entry men;
		CONDDefinition condDef;
		String name, tokString, bits;
		String currentOcc; //like -1-1-2-1-3 so that all levels are included up to depthScope
		Parser.Token tok = null;
		Parser.Token firstTok = null;

		Set defSet = (Set) refDef.namedCond.entrySet();
		
		iset = defSet.iterator();
		while (iset.hasNext()) {
			men = (Map.Entry)iset.next();
			name = (String) men.getKey();
			condDef = (CONDDefinition) men.getValue();
			//set the context for the first scope check
			tokString=""; bits="";
			currentOcc="";
			if (recNamedCond.containsKey(name)) {
				//tokens were collected
				firstTok=null;
				//extract the ArrayList and concatenate tokens in order at the specified level and above
				ilst = recNamedCond.get(name).listIterator();
				while (ilst.hasNext()) {
					tok = (Parser.Token)ilst.next();
					if (firstTok==null) firstTok=tok;
					if (tok.depth<condDef.depthScope) throw new ParserException.ParserErrorDepthBelowCOND(name);
					//Above test is potentially useless test because this test is performed at DEF load time!
					//tok.depth>=condDef.depthScope is implied
					if (currentOcc.length()<1) currentOcc = occurenceOf(tok.loop,condDef.depthScope); //first time
					if (currentOcc.equals(occurenceOf(tok.loop,condDef.depthScope))) {
						tokString = tokString.concat(tok.collected);
						bits = bits.concat("<"+tok.sourceTag+">");
						continue;
					}
					//scope break taking place:
					//1. verify condition
					if (!tokString.matches(condDef.verifPattern)) recordException(new ParserException.ParserFailedCOND(name, condDef.verifPattern, tokString),condDef, bits, firstTok.atLine, firstTok.atOffset);
					//2. set the context for the next scope check
					firstTok=tok;
					tokString = new String(tok.collected);
					bits = new String("<"+tok.sourceTag+">");
					currentOcc = occurenceOf(tok.loop,condDef.depthScope);
				}
				//implicit scope break at end of while
				//verify condition
				if (!tokString.matches(condDef.verifPattern)) {
					if (firstTok==null) firstTok=new Token("","",(new int[]{0}),0,"",0,0);
					recordException(new ParserException.ParserFailedCOND(name, condDef.verifPattern, tokString),condDef, bits, firstTok.atLine, firstTok.atOffset );
				}
			} 
			//ELSE: no tokens were collected. 
			//In this case, the condition must NOT be checked (for instance, it can be an interdependency 
			//entirely inside an optional structure)
		}

		//we looped on every condition
		return;
	}
	
	/**
	 * Supporting method for recording exceptions.
	 * <br>Invokes {@link #recordException(BrokerLegacyException, Impact, Handling)}
	 * @throws BrokerLegacyException 
	 */
	private void recordException(Exception e, Occurence occElt) throws ParserException {
		//build BrokerException and add to recExceptions
		String s = occElt.definition.getName() +" at DEF line "+occElt.definition.atDEFLineNb;
		ParserException prex = new ParserException.ParserDataError(occElt.definition.xmltag, occElt.definition.description,e,s,occElt.atLine,occElt.atOffset,occElt.definition.impact);
		recordException(prex,occElt.definition.impact,occElt.definition.handling);
	}


	/**
	 * Supporting method for recording exceptions.
	 * <br>Invokes {@link #recordException(ParserException, Impact, Handling)}
	 * 
	 * @throws ParserException 
	 */
	private void recordException(Exception e, CONDDefinition cond, String data, int ln, int lo) throws ParserException {
		//build BrokerException and add to recExceptions
		ParserException prex = new ParserException.ParserCONDError(cond.name, cond.errorText,e,data,ln,lo,cond.impact);
		recordException(prex,cond.impact,cond.handling);
	}
	
	/**
	 * Supporting method for recording exceptions.
	 * <br>Invokes {@link #recordException(ParserException, Impact, Handling)}
	 * 
	 * @throws ParserException 
	 */
	private void recordException(Exception e, UnTaggedElement ctxt, GSDDefinition def) throws ParserException {
		//build BrokerException and add to recExceptions
		String sctxt="--context not set--";
		if (ctxt instanceof TaggedGroup) sctxt = "GROUP<"+((TaggedGroup)ctxt).tag+">";
		else if (ctxt instanceof TaggedSegment) sctxt = "SEG<"+((TaggedSegment)ctxt).tag+">";
		else sctxt=ctxt.data;
		ParserException prex = new ParserException.ParserDataError(def.xmltag,def.description,e,sctxt,ctxt.atLine,ctxt.atOffset, def.impact);
		recordException(prex,def.impact,def.handling);
	}
	
	/**
	 * Supporting method for recording exceptions.
	 * 
	 * @throws ParserException 
	 */
	private void recordException(ParserException prex,Impact imp, Handling handl) throws ParserException {
		recExceptions.add(prex);
		//then check handling and exceptions counts versus impact and throw as applicable
		if (imp==Impact.FATAL) this.fatalCount++;
		this.allExceptionsCount++;
		if (handl==Handling.THROW) throw prex;
		if (fatalCount>maxFatal)
			throw new ParserException.ParserExceededFATALExceptions(maxFatal,prex);
		if (allExceptionsCount>maxAllExceptions)
			throw new ParserException.ParserExceededAllExceptions(maxAllExceptions,prex);
		return;
	}

	/**
	 * Helper method to lighten the matchGSD() method.
	 * 
	 * @param lp	current occurence table
	 * @param dep	current depth
	 * @param d		source data from which to possibly extract the token
	 * @param def	looked up element Definition
	 * @param parElt	parent element in the tagged structure under construction
	 */
	private void recordCondition(int[] lp, int dep, String d, GSDDefinition def, TaggedElement elt) {
		String s; StringBuffer sb;
		if (!def.cardinality.equals(Cardinality.CONDITIONAL)) return;
		if (def.conditionFeed.indexOf('(')<0) {
			//simple plain text not containing any bracket
			s = def.conditionFeed;
		} else {
			//conditionFeed is a regex used to extract value from original data
			Pattern ptrn = Pattern.compile(def.conditionFeed);
			sb = new StringBuffer(d);
			extractCompositeValue(sb, ptrn);
			//if a release char is defined and exists within the extracted value, 
			// then clean-up the extracted value!
			if (def.parentDef!=null && def.parentDef.releaseChar!=null 
					&& sb.indexOf(def.parentDef.releaseChar.toString())>=0) {
				// clean-up: restore released chars in their UN-released state
				char r = def.parentDef.releaseChar.charValue();
				StringBuffer sbClean = new StringBuffer(sb.length());
				for (int i=0;i<sb.length();i++) 
					if (sb.charAt(i)==r) {
						i++; //skip this one and always take the next
						if (i<sb.length()) sbClean.append(sb.charAt(i));
					}
					else sbClean.append(sb.charAt(i));
				s = sbClean.toString();
			} 
			else s = sb.toString(); //no release char interference, just the plain extracted value
		}
		elt.token = new Token(def.conditionName,s,lp,dep,elt.tag,elt.atLine, elt.atOffset);
	}
	
	
	/**
	 * Magic procedure able to return the concatenated value of all capturing groups in a 
	 * complex pattern applied to a string (the pattern can match once or more times). The procedure 
	 * ignores the sub-capturing-groups.
	 * (i.e. ignores nested Capturing groups) that would create data duplication in the result.
	 * <p>
	 * NOTE: This method was made public just for the sake of being invoked by the RegexCheck tool.
	 *  
	 * @param sb	string buffer containing original string and returned with the extracted result
	 * @param ptrn	pattern of reference with capturing groups
	 * @param sep	separator between multiple capturing group values in the concatenated resulting string
	 * @return		the offset in the original string of the first byte of the extracted part
	 */
	public int extractCompositeValue (StringBuffer sb, Pattern ptrn, String sep) {
        int mEnd = 0; //current right-most matcher group end - used to exclude capturing sub-groups from concatenation scope
		Matcher matcher = ptrn.matcher(sb.toString());
        int offsetOfFirstNonNullCaptGroup = sb.length();
        sb.setLength(0); //reset the buffer
		while (matcher.find()) {
        	for(int gn = 1; gn <= matcher.groupCount();gn++)
				if (matcher.start(gn) >= mEnd){
					if ((gn > 1) && (sep != null) && (sep != "")) sb.append(sep);
					sb.append(matcher.group(gn));
					offsetOfFirstNonNullCaptGroup = Math.min(offsetOfFirstNonNullCaptGroup, matcher.start(gn));
					mEnd = matcher.end(gn);
				};
					//ELSE this group starts before the end of the last one, skip it
				//or this (optional?)group is null in which case .start==-1
		}
		return offsetOfFirstNonNullCaptGroup;
	}
	
	/**
	 * Magic procedure able to return the concatenated value of all capturing groups in a 
	 * complex pattern applied to a string (the pattern can match once or more times). The procedure 
	 * ignores the sub-capturing-groups.
	 * (i.e. ignores nested Capturing groups) that would create data duplication in the result.
	 * <p>
	 * NOTE: This method was made public just for the sake of being invoked by the RegexCheck tool.
	 *  
	 * @param sb	string buffer containing original string and returned with the extracted result
	 * @param ptrn	pattern of reference with capturing groups
	 * @return		the offset in the original string of the first byte of the extracted part
	 */
	public int extractCompositeValue (StringBuffer sb, Pattern ptrn) {
        
		return extractCompositeValue (sb, ptrn,null);
	}
	
	

	/**
	 * Supporting method and parsing context builder
	 * 
	 * @param loop		loop count array of the parent structure, i.e. the segment or group within which we attempt to match sub-elements
	 * @param depth		depth of the <u>target</u> structures (i.e. also the size of the loop count array)
	 * @param utelt 	the raw element that we try to match against the reference segment definition (cf sDef)
	 * @param sDef 		the reference segment definition
	 * @param parentElt the parent element (tagged) in which the present segment is included
	 * 
	 * @throws ParserException
	 * 
	 */
	private TaggedSegment parseSegment(int[] loop, final int depth, UnTaggedElement utelt, final SEGDefinition sDef, TaggedElement parentElt) throws ParserException {
		//segment has been identified based on the id-pattern; good clue it can now be cut into sub-pieces
		TaggedSegment seg = new TaggedSegment(sDef.xmltag,utelt,sDef.suffix); //created with an empty subElts list
		SEGDefinition.CutFunction.CutContext cc = sDef.cutFunction.cut(seg.data, seg.atLine, seg.atOffset);
		while (sDef.cutFunction.hasNext(cc)) {
			String s = sDef.cutFunction.getNext(cc); //there's a possibility of optional capturing groups returning null string pieces
			if (s==null) continue;
			seg.subElts.add(new UnTaggedElement(s,sDef.cutFunction.getLineNb(cc),sDef.cutFunction.getOffset(cc)));
		}

		if (seg.subElts.size()<=0) {
			recordException(new ParserException.ParserUnableTo(sDef.cutFunction.fname,
					sDef.cutFunction.cutPattern,seg.data), utelt, sDef);
			return null;
		}
		//attempt to match the whole segment contents
		ListIterator<UnTaggedElement> newCursor = seg.subElts.listIterator();
		if(matchSGList(loop, depth, sDef, newCursor, seg)) return seg;
		seg=null; //freed for garbage collection
		return null; 

	}



	/**
	 * Supporting method and parsing context builder: Match Segment or Group List (of elements) with the
	 * relevant definition.
	 * <p>
	 * Recursively invokes parseSegment, parseGroup or ParseData.
	 * <p>
	 * Upon entry to this method, segment cut has been performed and the parser tries to match the resulting
	 * array list against the definition. Note that a segment, once cut, is almost like a group. 
	 * Differences are:<ul>
	 * <li>A segment has a cut-function, a group never has one.
	 * <li>Segment sub-element matching always starts at the beginning of an element list, 
	 * whereas Group element matching can take place anywhere else in the middle of a list, hence providing the
	 * cursor as argument.
	 * </ul>
	 * Upon entry, there must be at least one element available from the cursor.
	 * 
	 * @param loop		loop count array of the parent structure, i.e. the segment or group within which we attempt to match sub-elements
	 * @param depth		depth of the <u>target</u> structures (i.e. also the size of the loop count array)
	 * @param gsDef		the reference segment or group definition
	 * @param cursor	list iterator in the source message, pointing <u>just before</u> the one element supposed to
	 * 					be a valid first sub-element of the segment specified by gsDEF.<br>
	 * 					NOTE that a cursor (ListIterator) always fits in between list member elements.
	 * @param parentElt	the parent element (tagged) in which to move all the matched elements
	 * @return			true or false according to fact that some sub-elements were matched or none at all.
	 * @throws ParserException
	 */
	private boolean matchSGList(final int[] loop, final int depth, final GSDDefinition gsDef, ListIterator<UnTaggedElement> cursor, TaggedElement parentElt) 
	throws ParserException, ParserException {
		//an array of counters, with size up to the depth being parsed (max Definition.MAXDEPTH by design)
		int[] thisLoop = new int [depth+1];
		int occ=0;
		int thisLine, thisOffset;
		for (int i=0; i<depth; i++) thisLoop[i]=loop[i];
		thisLoop[depth]=0;
		ListIterator lookup = gsDef.subElts.listIterator();
		
		//local working fields
		UnTaggedElement cursElt = null;
		GSDDefinition lookElt = null;
		GSDDefinition backToUnmatchedLookElt = null; //while attempting to resume parsing next to a bad (skipped) data piece in input
		int successiveMissMatches = 0;
		int subEltCount=0;
		Pattern pattern;
		Matcher matcher;
		
		//try to match the cursor element in source message with the lookup element in the definition
		//loop as necessary to match all occurences of the same lookup element up to the accept count.
		cursElt = cursor.next();
		thisLine = cursElt.atLine; thisOffset = cursElt.atOffset;
		lookElt = (GSDDefinition) lookup.next();
		
		
		while(true) { 
			
			//There are four combinations of null cursor'Element and/or null definition
			
			//A. NULL CURSOR-ELEMENT AND NULL LOOKUP-DEFINITION
			//we have exhausted input data (may be within a seg) and definitions altogether
			if ((lookElt==null)&&(cursElt==null)) //source data is exhausted, and DEF sub-elements too; so far so good 
				return (subEltCount>0?true:false);
			
			//B. SPECIAL CASE: cursor is null or not, and the definition is a MARK
			if (lookElt instanceof MARKDefinition) {
				//M-A-R-K
				//evaluate the mark and insert into tagged structure:
				//1. prepare
				String s="";
				HashMap<String, ArrayList> tempNamedCond= new HashMap<String, ArrayList>();
				TaggedMark mrk = new TaggedMark(lookElt.xmltag);
				//2. compile tokens, implicitly at the present depth
				compileChildConditions(parentElt,tempNamedCond);
				//3. extract the relevant string of tokens
				if (tempNamedCond.containsKey(lookElt.conditionName)) {
					ListIterator ilst = tempNamedCond.get(lookElt.conditionName).listIterator();
					while (ilst.hasNext()) s = s.concat(((Parser.Token)ilst.next()).collected);
				} else
					s = "";
				//4. check against pattern
				if (s.matches(((MARKDefinition)lookElt).evalPattern))
				mrk.data = ((MARKDefinition)lookElt).yesString;
				else mrk.data = ((MARKDefinition)lookElt).noString;
				
				//5. insert the mark (if value is == "NULL", we skip it when generating the XML, not now)
				//add to a group, insert into a segment BEFORE the cursor-element
				if ((parentElt instanceof TaggedGroup)) {
					parentElt.subElts.add(mrk);
				} else if (cursElt==null) { // BHAU MBR-75 added: explicit case of a trailing MARK after all segment elements were exhausted
				    // we are not in a GROUP (that was the case above) but at the end of a segment (or the top TaggedMessage)
					cursor.add(mrk);
				} else {
					cursor.previous();cursor.add(mrk);cursor.next();
				}
	
				//6. record? ...a MARK has no effect on subElement count

				//7. continue the parsing with the next definition
				thisLoop[depth]=0;
				if (lookup.hasNext()) {
					lookElt = (GSDDefinition) lookup.next();
					//cursElt is still the same
					continue;
				} //ELSE
				lookElt = null;
				continue;
			}

			//C. NON-NULL CURSOR-ELEMENT WITH NULL LOOKUP-DEFINITION
			if (lookElt==null) {
				//more source data still available, but no more sub-element DEF to consider 
				//in the current segment or group (exhausted or has reached MAXSUCCESSIVEMISMATCHES)
				//-->We have finished with the present segment or group context, so, return to parent

				// By convention: move the cursor back before the last un-matched element 
				//(note that cursElt stays the same because a sequence of .next() 
				//and .previous() yields the same element)
				cursElt = cursor.previous(); 

				//but! but! wait... if we are at the end of the DEF list of sub-elements in a segment, we must
				//also be at the end of this (nested) lookup list of source data resulting from segment CUT
				if (parentElt instanceof TaggedSegment) {
					//we enter here for TaggedSegment and child classes, i.e. TaggedMessage!!!

					if (backToUnmatchedLookElt==null) { 
						recordException(new ParserException.ParserNoMatchingDef(cursElt.data), cursElt, gsDef);
						return (subEltCount>0?true:false); //no need to try
					}
					//ELSE
					recordException(new ParserException.ParserTryingToSkip(cursElt.data), cursElt, gsDef);
					//if we are still here (recording above did not escalate in a throw Exception)
					//we shall try to skip the bad input data and attempt to resume parsing on the last good DEF
					
					//a) BACKTRACK to last good DEF
					while (lookup.hasPrevious()) {
						lookElt = (GSDDefinition)lookup.previous();
						if (lookElt.equals(backToUnmatchedLookElt)) break;
						//TRICK! remove marks that were generated on the way forward since the last match
						//because they will be generated again
						if (lookElt instanceof MARKDefinition) {
							//we are in a segment by context, pop-it out
							cursor.previous();cursor.remove(); 
						}
					}
					//will resume the parsing with the found definition
					lookElt = (GSDDefinition) lookup.next(); //which here is again backToUnmatchedLookElt!
					thisLoop[depth]=0;
					//b) SKIP bad DATA 
					cursElt = cursor.next(); //still on the same element, cursor changed side, that's all 
					//so one more time for a real movement forward
					if (!cursor.hasNext()) {
						//cursElt = null;
						return (subEltCount>0?true:false); //that BAD data was the last piece, so return
					}
					cursElt = cursor.next(); //there is!
					thisLine = cursElt.atLine; thisOffset = cursElt.atOffset;
					successiveMissMatches = 0;
					continue;
				}
				//if we are in a Group that's OK to return with source data yet unParsed		
				return (subEltCount>0?true:false);

			}
			
			//D. NULL CURSOR-ELEMENT AND NON-NULL LOOKUP-DEFINITION
			if (cursElt==null){
				//source data is exhausted: 
				//1. record the zero occurence of the definition at stake
				recMinMax.add(new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
				//2. if the definition is mandatory, raise an exception
				if (lookElt.cardinality.equals(Cardinality.MANDATORY))
					recordException(new ParserException.ParserPrematureEndOfData(lookElt.xmltag), parentElt, lookElt); 
				//3. continue the parsing with the next definition
				if (lookup.hasNext()) {
					lookElt = (GSDDefinition) lookup.next();
					//cursElt is still null
					continue;
				} //ELSE
				lookElt = null;
				continue;
			}
			
			//E. NON-NULL CURSOR-ELEMENT AND NON-NULL LOOKUP-DEFINITION
			//E.1. The definition is a Data Element
			if (lookElt instanceof DataDefinition) {
				// D-A-T-A   E-L-E-M-E-N-T
				pattern = Pattern.compile(((DataDefinition)lookElt).validPattern);
				occ=0;
				
				while(true) { //DATA ELEMENT
					occ++;
					if (occ>lookElt.occAccept) {
						//1. record the element occurence
						recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
						thisLoop[depth]=0;
						//2. terminate loop
						break;
					}
					
					matcher= pattern.matcher(cursElt.data);

					if (matcher.matches()) {
						//we have a matching element and definition
						if (occ==lookElt.occAccept) backToUnmatchedLookElt = null;
						matcher.reset();
						//extract the data element value, tag the element, and update with extracted value
				        TaggedData elt = new TaggedData(lookElt.xmltag,cursElt);
				        StringBuffer sb = new StringBuffer(cursElt.data);
						elt.atOffset += extractCompositeValue(sb,pattern);
						//sb is updated by the method above to contain the extracted value
						//if a release char is defined and exists within the extracted value, 
						// then clean-up the extracted value!
						if (lookElt.parentDef!=null && lookElt.parentDef.releaseChar!=null 
								&& sb.indexOf(lookElt.parentDef.releaseChar.toString())>=0) {
							// clean-up: restore released chars in their UN-released state
							char r = lookElt.parentDef.releaseChar.charValue();
							StringBuffer sbClean = new StringBuffer(sb.length());
							for (int i=0;i<sb.length();i++) 
								if (sb.charAt(i)==r) {
									i++; //skip this one and always take the next
									if (i<sb.length()) sbClean.append(sb.charAt(i));
								}
								else sbClean.append(sb.charAt(i));
							elt.data = sbClean.toString();
						} 
						else elt.data = sb.toString(); //no release char interference, just the plain extracted value
						//validate the character set
						if (!((DataDefinition)lookElt).charValidation.check(elt.data)) {
							recordException(new ParserException.ParserInvalidValue(((DataDefinition)lookElt).charValidation.fname,
									((DataDefinition)lookElt).charValidation.vPattern,elt.data), cursElt, lookElt);
						}
						
						//test whether removeNonRepeatableNilOptionalElement is applicable to this one
						if (removeNonRepeatNilOptElts && (elt.data.length()<=0) && (lookElt.occAccept<=1)
								&& !lookElt.cardinality.equals(Cardinality.MANDATORY)
								&& (((DataDefinition)lookElt).lengthMin>0)) {
							//NOTE: no counting of the element instance
							//NOTE: no recording under a named condition
							//remove the element 
							cursor.remove();
							//advance cursor
							if (cursor.hasNext()) {
								cursElt = cursor.next();
								thisLine = cursElt.atLine; thisOffset = cursElt.atOffset;
								successiveMissMatches = 0;
							} //else source data is exhausted
							else cursElt = null;
							//in all cases:
							//1. record the element occurence
							recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
							thisLoop[depth]=0;
							//2. terminate loop
							break; //out of WHILE DATA ELEMENT loop
						}
						//ELSE, back to the normal case
						//validate the min/max length
						if (elt.data.length()<((DataDefinition)lookElt).lengthMin) {
							recordException(new ParserException.ParserDataUnderSized(((DataDefinition)lookElt).lengthMin,elt.data), 
									cursElt, lookElt);
						}
						if ((((DataDefinition)lookElt).lengthMax>=0)&&(elt.data.length()>((DataDefinition)lookElt).lengthMax)) {
							recordException(new ParserException.ParserDataOverSized(((DataDefinition)lookElt).lengthMax,elt.data), 
									cursElt, lookElt);
						}
						
						//account in both the absolute count of sub-elements and in this element loop count
						subEltCount++;
						thisLoop[depth]++;
						//collect any token under named condition as applicable
						recordCondition(thisLoop, depth, cursElt.data, lookElt, elt);						
						//remove Untagged element from source data and put the new tagged element into 
						//the parent element >if< the parent is a group
						//else only substitute in list (if the parent is a segment).
						if (parentElt instanceof TaggedGroup) {
							cursor.remove();
							parentElt.subElts.add(elt);
						} else cursor.set(elt);
						
						//advance cursor
						if (cursor.hasNext()) {
							cursElt = cursor.next();
							thisLine = cursElt.atLine; thisOffset = cursElt.atOffset;
							successiveMissMatches = 0;
							continue; //check for a next occurence
							//note: intermediate occurence counts are not recorded
						}
						//ELSE source data is exhausted
						cursElt = null;
						//1. record the element occurence
						recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
						thisLoop[depth]=0;
						//2. terminate loop
						break; //out of WHILE DATA ELEMENT loop
					}			
					//ELSE
					//there's no match (possibly after several occurence loops)
					occ--;

					//1. if Cardinality has not been met, raise an exception, except if this is
					//the first mandatory sub-elt in the parent structure. In the later case, consider that the
					//whole parent structure is not met -> return to parent
					if (lookElt.cardinality.equals(Cardinality.MANDATORY)&&(occ<1))
						if (subEltCount>0) {
							//some pieces of the parent structure were found anyhow!
							//push for backtracking if too many successive failures
							if ((++successiveMissMatches)>= maxSuccessiveMisMatches) {
								recordException(new ParserException.ParserNowBacktracking(maxSuccessiveMisMatches,pattern.pattern(),
										cursElt.data), cursElt, lookElt);
								//skip to the end of DEFs for current group or segment
								while (lookup.hasNext()) lookElt = (GSDDefinition)lookup.next();
							}
							else
								recordException(new ParserException.ParserMissingMandatoryElt(lookElt.xmltag,pattern.pattern(),
										cursElt.data), cursElt, lookElt);
							//still here? means the exception was recorded, so go on
							
						} else {
							//no piece of the parent was yet found and this mandatory one neither
							//By convention: move the cursor back before the last un-matched element 
							cursElt = cursor.previous(); 
							
							return false;
						}
					else //record the element occurence (no need if exceptions already raised as above)
						recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
					//2. will try to match source against next lookup element
					break; //out of WHILE DATA ELEMENT loop
					
				} //end of WHILE DATA ELEMENT loop
				//break here or exit after occAccept count has been reached:
				//the source data is exhausted, or all acceptable occurences were matched, 
				//or there's no (more) match

				// By convention: the cursor is still after the last un-matched element 

				//Try following DEF element
				if (lookup.hasNext()) {
					lookElt = (GSDDefinition) lookup.next();
					if ((backToUnmatchedLookElt==null)&&!(lookElt instanceof MARKDefinition)) backToUnmatchedLookElt = lookElt;
					thisLoop[depth]=0;
					continue;
				}
				else {
					lookElt = null;
					thisLoop[depth]=0;
					continue;
				}

			}
			
			//E.2. The definition is a Segment
			if (lookElt instanceof SEGDefinition) {
				//S-E-G-M-E-N-T
				pattern = Pattern.compile(((SEGDefinition)lookElt).idPattern);
				occ=0;
				while(true) { //SEGMENT
					occ++;
					if (occ>lookElt.occAccept) {
						//1. record the element occurrence
						recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
						thisLoop[depth]=0;
						//2. terminate loop
						break;
					}
					matcher= pattern.matcher(cursElt.data);
					
					if (matcher.find()) {
						//we have a _possibly_ matching element and definition, await parsing step to confirm this
						matcher.reset();
						
						//test whether removeNonRepeatableNilOptionalElement is applicable to this one
						if (removeNonRepeatNilOptElts && (cursElt.data.length()<=0) && (lookElt.occAccept<=1)
								&& !lookElt.cardinality.equals(Cardinality.MANDATORY)
								/*SUGG consider AND_test_non-Zero_length_segment, cf Data Element case */ ) {
							//NOTE: no counting of the instance
							//NOTE: no recording under a named condition
							//remove the element 
							cursor.remove();
							//advance cursor
							if (cursor.hasNext()) {
								cursElt = cursor.next();
								thisLine = cursElt.atLine; thisOffset = cursElt.atOffset;
								successiveMissMatches = 0;
							} //else source data is exhausted
							else cursElt = null;
							//in all cases:
							//1. record the element occurence
							recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
							thisLoop[depth]=0;
							//2. terminate loop
							break; //out of WHILE SEGMENT loop
						}

						//ELSE, back to the normal case
						//cut the segment content and parse, if OK, tag it (done within parse method)
						thisLoop[depth]++; //anticipate
						TaggedSegment elt = parseSegment(thisLoop,depth+1,cursElt,(SEGDefinition)lookElt,parentElt);
						if (elt!=null) {
							//sub-parsing is also OK, so we can _now_ consider that the given sub-segment is found
							if (occ==lookElt.occAccept) backToUnmatchedLookElt = null;
							//account in both the absolute count of sub-elements and in this element loop count
							subEltCount++;
							//thisLoop[depth]++ was anticipated
							//collect any token under named condition as applicable
							recordCondition(thisLoop, depth, cursElt.data, lookElt, elt);						
							//remove Untagged element from source data and put the new tagged element into the parent element >if< the parent is a group
							//else only substitute in list
							if (parentElt instanceof TaggedGroup) {
								cursor.remove();
								parentElt.subElts.add(elt);
							} else cursor.set(elt);

							//advance cursor
							if (cursor.hasNext()) {
								cursElt = cursor.next();
								thisLine = cursElt.atLine; thisOffset = cursElt.atOffset;
								successiveMissMatches = 0;
								continue; //check for a next occurence
								//note: intermediate occurence counts are not recorded
							}
							//ELSE source data is exhausted
							cursElt = null;
							//1. record the element occurence
							recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
							thisLoop[depth]=0;
							//2. terminate loop							
							break; //out of WHILE SEGMENT loop
						} 
						//ELSE
						//there's no match: parse() returned false (all exceptions raised within parse method)
						occ--;
						thisLoop[depth]--; //compensate anticipation and restore truth
						//check cardinality (next to if-else-block below) and record element occurence
					}
					else {
						//there's no (more) match: matcher.find() returned false
						occ--;
						//check cardinality (next to if-else-block below) and record element occurence
					}
					//HERE, either there's no match, either parseSegment() returned null 
					//(possibly after several occurence loops)
					
					//1. if Cardinality has not been met, raise an exception, except if this is
					//the first mandatory sub-elt in the parent structure. In the later case, consider that the
					//whole parent structure is not met -> return to parent
					if (lookElt.cardinality.equals(Cardinality.MANDATORY)&&(occ<1))
						if (subEltCount>=0) {
							//some pieces of the parent structure were found anyhow!
							//push for backtracking if too many successive failures
							if ((++successiveMissMatches)>= maxSuccessiveMisMatches) {
								recordException(new ParserException.ParserNowBacktracking(maxSuccessiveMisMatches,pattern.pattern(),
										cursElt.data), cursElt, lookElt);
								//skip to the end of DEFs for current group or segment
								while (lookup.hasNext()) lookElt = (GSDDefinition)lookup.next();
							}
							else
								recordException(new ParserException.ParserMissingMandatorySeg(lookElt.xmltag,pattern.pattern(),
										cursElt.data), cursElt, lookElt);
							//still here? means the exception was recorded, so go on

						} else {
							//no piece of the parent was yet found and this mandatory one neither
							//record the element occurence
							recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));							
							// By convention: move the cursor back before the last un-matched element 
							cursElt = cursor.previous(); 

							return false;
						}
					else //record the element occurence (no need if exceptions already raised as above)
						recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
					//2. will try to match source against next lookup element
					break; //out of WHILE SEGMENT loop
					
				} //end of WHILE SEGMENT loop
				//break here or exit after occAccept count has been reached:
				//the source data is exhausted, or all acceptable occurences were matched, or there's no (more) match

				// By convention: the cursor is still after the last un-matched element 

				//Try following DEF element
				if (lookup.hasNext()) {
					lookElt = (GSDDefinition) lookup.next();
					if ((backToUnmatchedLookElt==null)&&!(lookElt instanceof MARKDefinition)) backToUnmatchedLookElt = lookElt;
					thisLoop[depth]=0;
					continue;
				}
				else {
					lookElt = null;
					thisLoop[depth]=0;
					continue;
				}

			}

			//E.3. The definition is a Group
			if (lookElt instanceof GRPDefinition) {
				//G-R-O-U-P
				pattern = Pattern.compile(((GRPDefinition)lookElt).idPattern);
				occ=0;
				while(true) { //GROUP
					occ++;
					if (occ>lookElt.occAccept) {
						//1. record the element occurence
						recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
						thisLoop[depth]=0;
						//2. terminate loop
						break;
					}
					matcher = pattern.matcher(cursElt.data);
					if (matcher.find()) {
						//we have a _possibly_ matching group, await _matching_ step to confirm this
						matcher.reset();
						//prepare a new element to host the group
						TaggedGroup elt = new TaggedGroup(lookElt.xmltag,cursElt,lookElt.suffix);
						//try to match the group at the next depth level
						thisLoop[depth]++; //anticipate
						cursor.previous(); //reposition before the first element to match
						if (matchSGList(thisLoop, depth+1, lookElt, cursor, elt)) {
							//sub-parsing went OK, so we can _now_ consider that the given group is found
							//Design NOTE: we cannot roll back the effects of partial group parsing; if the match
							//wasn't complete (i.e. some of its mandatory pieces are missing), matchSGList() returns true too.
							//All relevant Exceptions have already been raised anyhow.
							if (occ==lookElt.occAccept) backToUnmatchedLookElt = null;
							//all matched UnTagged source data elements have been absorbed by matchSGList() above 
							//and >moved< into the TaggedGroup elt; 
							//Account in both the absolute count of sub-elements and in this element loop count
							subEltCount++;
							//thisLoop[depth]++ was anticipated
							//collect any token under named condition as applicable
							recordCondition(thisLoop, depth, cursElt.data, lookElt, elt);						
							//Now add the Tagged Group into the message
							//BUT, beware, if the parent element is a group, add  the element into it, else insert
							//in the segment (or message)
							if (parentElt instanceof TaggedGroup) {
								parentElt.subElts.add(elt); //is append!
							} else cursor.add(elt); //is actually an insert!
							
							//advance cursor
							if (cursor.hasNext()) {
								cursElt = cursor.next();
								thisLine = cursElt.atLine; thisOffset = cursElt.atOffset;
								successiveMissMatches = 0;
								continue; //check for a next occurence
								//note: intermediate occurence counts are not recorded
							}
							//ELSE source data is exhausted
							cursElt = null;
							//1. record the element occurence
							recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
							thisLoop[depth]=0;
							//2. terminate loop
							break; //out of WHILE GROUP loop
						}
						//ELSE
						//there's no match: matchSGList() returned false (all exceptions raised within parse method)
						
						//BEWARE: on return from matchSGList(), the cursor is positioned BEFORE the source element, 
						//and not after that element which we failed to parse with this one group DEF.
						//So, put cursor next to the element at stake
						if (cursor.hasNext()) {
							cursElt = cursor.next();
							thisLine = cursElt.atLine; thisOffset = cursElt.atOffset;
						} 
						else cursElt = null; //source data is exhausted
						
						//We need to record the occurence whenever source data was consumed
						//that is why matchSGList() returns false if no sub-element at all were matched
						
						//nothing (new) at all was matched within the present 'for' loop
						thisLoop[depth]--; //compensate anticipation and restore truth
						occ--;
						//check cardinality (next to if-else-block below) and record element occurence
					}
					else {
						//ELSE there's no (more) match: matcher.find() returned false
						occ--;	
						//check cardinality (next to if-else-block below) and record element occurence
					}
					//HERE, either there's no match, either matchSGlist returned false (possibly after several occurence loops)
					//By convention, the cursor is after cursElt that still contains the last unmatched element

					//1. if Cardinality has not been met, raise an exception, except if this is
					//the first mandatory sub-elt in the parent structure, in which case, consider that the
					//whole parent structure is just not met -> return to parent
					if (lookElt.cardinality.equals(Cardinality.MANDATORY)&&(occ<1))
						if (subEltCount>=0) {
							//some pieces of the parent structure were found anyhow!
							//push for backtracking if too many successive failures
							if ((++successiveMissMatches)>= maxSuccessiveMisMatches) {
								recordException(new ParserException.ParserNowBacktracking(maxSuccessiveMisMatches,pattern.pattern(),
										cursElt.data), cursElt, lookElt);
								//skip to the end of DEFs for current group or segment
								while (lookup.hasNext()) lookElt = (GSDDefinition)lookup.next();
							}
							else
								recordException(new ParserException.ParserMissingMandatoryGroup(lookElt.xmltag,pattern.pattern(),
										cursElt.data), cursElt, lookElt);
							//still here? means the exception was recorded, so go on

						} else {
							//no piece of the parent was yet found and this mandatory one neither
							//record the element occurence
							recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));							
							// By convention: move the cursor back before the last un-matched element 
							cursElt = cursor.previous(); 

							return false;
						}
					else //record the element occurence (no need if exceptions already raised as above)
						recMinMax.add( new Occurence(lookElt,thisLoop,depth, thisLine, thisOffset));
					//2. will try to match source against next lookup element
					break; //out of WHILE GROUP loop
					
				} //end of WHILE GROUP loop
				//break here or exit after occAccept count has been reached:
				//the source data is exhausted, or all acceptable occurences were matched, or there's no (more) match
				
				// By convention: the cursor is after the last un-matched element, or null

				//Try following DEF element
				if (lookup.hasNext()) {
					lookElt = (GSDDefinition) lookup.next();
					if ((backToUnmatchedLookElt==null)&&!(lookElt instanceof MARKDefinition)) backToUnmatchedLookElt = lookElt;
					thisLoop[depth]=0;
					continue;
				}
				else {
					lookElt = null;
					thisLoop[depth]=0;
					continue;
				}

			}			
			
			//E.4. The definition is nothing else that we know about
			throw new ParserException.ParserInternalError(lookElt.getName(), Impact.FATAL);
			//block of unreachable code
			
		} //END of external while(true) loop
		
		//NO code here <unreachable section>

	}
	
	
	/**
	 * Parses an input character stream using a {@link LineNumberReader}. This implementation is able to
	 * trace line offsets in {@link ParserException Parser Exceptions}
	 * whenever the MSG level cut-function is actually CUT-ON-NL.
	 * <p>
	 * The parsing is successful when no exceptions are thrown <u>and</u> the returned number of 
	 * recorded exceptions is 0.
	 * <p>
	 * Next to parsing, the XML document can be generated using:<br>
	 * <code>{@link #getXML(boolean, boolean)}</code>
	 * 
	 * @param msgID		a message ID (will be recorded in exceptions and traced)
	 * @param dataIn		the line number reader, possibly reset(), so that readLine() will get the very first characters
	 * @param startLineNb	the line number to assume <u>next</u> to the first dataIn.readLine()
	 * @return				the total count of exceptions that were recorded
	 * @throws IOException 
	 * @throws ParserException
	 */
	public int parse(final String msgID, LineNumberReader dataIn, final int startLineNb) throws IOException, ParserException {
		
		this.reset();
		messageID = msgID;
		
		//special handling applies to MSG at depth 0: cut first, then match against identification
		//optimise handling for CUT-ON-NL
		if (refDef.msgDef.cutFunction.fname.equals("Cut-on-NewLine")) {
			//benefit from the LineNumberReader and ignore the cut-function implementation within the SEG/MSG
			String lineIn = dataIn.readLine();
			int atLineNb = startLineNb;
			dataIn.setLineNumber(atLineNb);

			message = new TaggedMessage(refDef.msgDef.xmltag,atLineNb,0,refDef.msgDef.suffix);
			//check MSG identification match against first line
			if ((refDef.msgDef.idPattern.length()>0) && (!Pattern.compile(refDef.msgDef.idPattern).matcher(lineIn).find())) 
				recordException(new ParserException.ParserMessageIdentificationFailure(refDef.msgDef.idPattern,lineIn),
						message, refDef.msgDef);
			//do the first level cut and load in message
			//SUGG consider optimised implementations for large messages by not 
			//entirely loading the message here below but building an intelligent cursor that loads on demand...
			//(TIP: sub-class the ListIterator in an anonymous inner class that extend the next() method)
			while (lineIn!=null) {
				message.subElts.add(new UnTaggedElement(lineIn,atLineNb,0));
				lineIn = dataIn.readLine();
				atLineNb = dataIn.getLineNumber();
			}
			//launch the matching at depth 1 (for the whole message contents)
			ListIterator<UnTaggedElement> cursor = message.subElts.listIterator();
			//stupid case but...if there's nothing available at the cursor, always throw! an exception (MSG is always MANDATORY)
			if (!cursor.hasNext()) {
				ParserException prex = new ParserException.ParserInternalErrorEmptyDEF(Impact.FATAL);
				recordException(prex, Impact.FATAL, Handling.THROW);
				throw prex; //shall never get here because thrown while recorded just above...
			}
			int[] loopMSG = { 1 };
			if (matchSGList(loopMSG, 1, refDef.msgDef, cursor,message)) {
				//matching done, but is there anything left unmatched in the source message?
				if (cursor.hasNext()) {
					//oooups, there's source data left after the message
					UnTaggedElement before = cursor.previous();
					cursor.next();
					UnTaggedElement after = cursor.next();
					recordException(new ParserException.ParserInputNotExhausted(after.data), 
							before, refDef.msgDef);
					//if we arrive here (no exception thrown) the rest of the processing 
					//is identical to steps just next 
				}
				compileConditions(message,recNamedCond); //group them in recNamedCond hash table
				filterOccurences();
				verifyOccurences();
				verifyConditions();
				return allExceptionsCount; //so far, this is the one exit point when everything went fine
			}
			else
			{ //MSG(segment) matching failed. Exceptions have been already recorded (and not thrown 
				//because we got here!) Just ensure we return a non-zero Exceptions Count
				if (allExceptionsCount>0) {
					compileConditions(message,recNamedCond); //group them in recNamedCond hash table
					filterOccurences();
					verifyOccurences();
					verifyConditions();
					return allExceptionsCount;
				}
				//ELSE, we haven't been able to match any bit of the message DEF itself
				UnTaggedElement last = message.subElts.get(0);
				if (cursor.hasPrevious()) last = cursor.previous();
				if (last!=null) {
					ParserException prex = new ParserException.ParserNoMsgEletsFound("MSG",
						"Entire Message",last.data,last.atLine, last.atOffset,Impact.FATAL);
					recordException(prex, Impact.FATAL, Handling.THROW);
					throw prex; //shall never get here because thrown while recorded just above...					
				}
				ParserException prex = new ParserException.ParserNoMsgEletsFound("MSG",
						"Entire Message","?????",0,0,Impact.FATAL);
				recordException(prex, Impact.FATAL, Handling.THROW);
				throw prex; //shall never get here because thrown while recorded just above...					
			}
			
		} 
		else { //MSG cutting is not based on new lines, why bother with a LineNumberReader?
			//convert input and invoke the other parse method based on an input string
			//read all into a string via a StringBuffer
			//SUGG consider optimised implementations here such as to process large files and benefit from input buffering
			StringBuffer sbuf = new StringBuffer();
			final int MAX = 10000;
			char[] cbuf = new char[MAX];
			int readCnt = 0;
			readCnt = dataIn.read(cbuf,0,MAX);
			while (readCnt>0) {
				sbuf.append(cbuf, 0, readCnt);
				readCnt = dataIn.read(cbuf,0,MAX);
			}
			//fall back onto the alternative parsing method
			return parse(messageID, sbuf.toString(),startLineNb);
		}

	}
	


	/**
	 * Variant parse method starting from a string and falling back onto 
	 * the other {@link #parse(String, LineNumberReader, int)} method 
	 * whenever the system discovers that the {@link SEGDefinition.CutFunction cut function}
	 * at the Message level is <code>CUT-ON-NL</code>.
	 * 
	 * @param msgID		a message ID
	 * @param dataIn	input string data message
	 * @param startLineNb	starting line number (e.g. from the original message that
	 * 						also possibly contained a header/envelope)
	 * @return				the total count of exceptions that were recorded
	 * @throws IOException
	 * @throws ParserException
	 */
	public int parse(final String msgID, String dataIn, final int startLineNb) throws IOException, ParserException {
		
		this.reset();
		messageID = msgID;
		
		//stupid case but...if there's no data, always throw! an exception (MSG is always MANDATORY)
		if (dataIn.length()<=0) {
			ParserException prex = new ParserException.ParserHasNothingToMatch("MSG","Entire Message", "--NO DATA--", startLineNb, 0, Impact.FATAL);
			recordException(prex, Impact.FATAL, Handling.THROW);
			throw prex; //shall never get here because thrown while recorded just above...
		}
		//attempt to benefit from line numbers when the MSG cutting function is CUT-ON-NL
		if (refDef.msgDef.cutFunction.fname.equals("Cut-on-NewLine")) {
			//convert the input string to a LineNumber Reader and fall back on the alternative
			//optimised method featuring line offset tracking
			return parse(messageID,new LineNumberReader(new StringReader(dataIn)), startLineNb);
		}
		else
		{
			//we shall cut a plain string
			message = new TaggedMessage(refDef.msgDef.xmltag,startLineNb,0,refDef.msgDef.suffix);
			//do the first level cut and load in message
			//SUGG consider optimised implementations for large messages by not 
			//entirely loading the message here below but building an intelligent cursor that loads on demand...
			SEGDefinition.CutFunction.CutContext cc = refDef.msgDef.cutFunction.cut(dataIn, startLineNb);
			while (refDef.msgDef.cutFunction.hasNext(cc)) {
				String s = refDef.msgDef.cutFunction.getNext(cc); //there's a possibility of optional capturing groups returning null string pieces
				if (s==null) continue;
				message.subElts.add(new UnTaggedElement(s,refDef.msgDef.cutFunction.getLineNb(cc),refDef.msgDef.cutFunction.getOffset(cc)));
			}

			//if the cut failed for the message itself, too bad: always throw! an exception
			if (message.subElts.size()<=0) {
				ParserException prex = new ParserException.ParserDataError(refDef.msgDef.xmltag,refDef.msgDef.description, new ParserException.ParserUnableTo(refDef.msgDef.cutFunction.fname,
						refDef.msgDef.cutFunction.cutPattern,message.data), "--NO CUT PIECE--", startLineNb, 0, Impact.FATAL);
				recordException(prex, Impact.FATAL, Handling.THROW);
				throw prex; //shall never get here because thrown while recorded just above...
			}
				
			//check MSG identification match against first segmented piece
			String fsp = message.subElts.get(0).data;
			if ((refDef.msgDef.idPattern.length()>0) && (!Pattern.compile(refDef.msgDef.idPattern).matcher(fsp).find()))
				recordException(new ParserException.ParserMessageIdentificationFailure(refDef.msgDef.idPattern,fsp),
						message, refDef.msgDef);

			//launch the matching at depth 1 (for the whole message contents)
			ListIterator<UnTaggedElement> cursor = message.subElts.listIterator();
			int[] loopMSG = { 1 };
			if (matchSGList(loopMSG, 1, refDef.msgDef, cursor,message)) {
				//matching done, but is there anything left unmatched in the source message?
				if (cursor.hasNext()) {
					//oooups, there's source data left after the message
					UnTaggedElement before = cursor.previous();
					cursor.next();
					UnTaggedElement after = cursor.next();
					recordException(new ParserException.ParserInputNotExhausted(after.data), 
							before, refDef.msgDef);
					//if we arrive here (no exception thrown) the rest of the processing 
					//is identical to steps just next 
				}
				compileConditions(message,recNamedCond); //group them in recNamedCond hash table
				filterOccurences();
				verifyOccurences();
				verifyConditions();
				return allExceptionsCount; //so far, this is the one exit point when everything went fine
			}
			else
			{ //MSG(segment) matching failed. Exceptions have been already recorded (and not thrown) 
				//because we got here! Just ensure we return a non-zero Exceptions Count
				if (allExceptionsCount>0) {
					compileConditions(message,recNamedCond); //group them in recNamedCond hash table
					filterOccurences();
					verifyOccurences();
					verifyConditions();
					return allExceptionsCount;
				}
				//ELSE, we haven't been able to match any bit of the message DEF itself
				UnTaggedElement last = message.subElts.get(0);
				if (cursor.hasPrevious()) last = cursor.previous();
				if (last!=null) {
					ParserException brex = new ParserException.ParserNoMsgEletsFound("MSG",
							"Entire Message",last.data,last.atLine, last.atOffset,Impact.FATAL);
					recordException(brex, Impact.FATAL, Handling.THROW);
					throw brex; //shall never get here because thrown while recorded just above...					
				}
				ParserException brex = new ParserException.ParserNoMsgEletsFound("MSG",
						"Entire Message","?????",0,0,Impact.FATAL);
				recordException(brex, Impact.FATAL, Handling.THROW);
				throw brex; //shall never get here because thrown while recorded just above...					
			}
		}


	}

	/**
	 * Supporting method that fills up structures at a given depth.<br>
	 * Recursively invoked.
	 * 
	 * @param withRAW	tells to generate RAW element or not;
	 * 					i.e. either UnTagged elements else those explicitly tagged as 'RAW'
	 * @param doc
	 * @param fillIt
	 * @param withElt
	 */
	private void fillDOM (Document doc, Element fillIt, UnTaggedElement withElt ,final boolean withRaw) {
		Element xmlElt;
		Element fillThis = fillIt;
		
		if (withElt instanceof TaggedMessage) {
			for (int i=0; i<((TaggedMessage)withElt).subElts.size();i++) {
				fillDOM(doc,fillThis,((TaggedMessage)withElt).subElts.get(i),withRaw);
				}
			return;
		}
		if (withElt instanceof TaggedData) {
			if (((TaggedData)withElt).tag.equals("SKIP")) return;
			if (((TaggedData)withElt).tag.equals("RAW")&&!withRaw) return;
			if (((TaggedData)withElt).tag.equals("NOTAG")) {
				//insert as text node directly within parent
				fillThis.appendChild(doc.createTextNode(withElt.data));
				return;
			}
			if (((TaggedData)withElt).tag.charAt(0)=='@') {
				//promote as attribute to parent element
				fillThis.setAttribute(((TaggedData)withElt).tag.substring(1),withElt.data);
			} 
			else {
				//regular child element
				xmlElt = doc.createElement(((TaggedData)withElt).tag);
				xmlElt.appendChild(doc.createTextNode(withElt.data));
				//xmlElt.setAttribute("L", Integer.toString(withElt.atLine));
				//xmlElt.setAttribute("O", Integer.toString(withElt.atOffset));
				fillThis.appendChild(xmlElt);
			}
			return;
		}
		if (withElt instanceof TaggedMark) {
			if (withElt.data.equals("NULL")) return;
			//rest is identical to TaggedData case
			if (((TaggedMark)withElt).tag.equals("SKIP")) return;
			if (((TaggedMark)withElt).tag.equals("RAW")&&!withRaw) return;
			if (((TaggedMark)withElt).tag.equals("NOTAG")) {
				//insert as text node directly within parent
				fillThis.appendChild(doc.createTextNode(withElt.data));
				return;
			}
			if (((TaggedMark)withElt).tag.charAt(0)=='@') {
				//promote as attribute to parent element
				fillThis.setAttribute(((TaggedMark)withElt).tag.substring(1),withElt.data);
			} 
			else {
				//regular child element
				xmlElt = doc.createElement(((TaggedMark)withElt).tag);
				xmlElt.appendChild(doc.createTextNode(withElt.data));
				//xmlElt.setAttribute("L", Integer.toString(withElt.atLine));
				//xmlElt.setAttribute("O", Integer.toString(withElt.atOffset));
				fillThis.appendChild(xmlElt);
			}
			return;
		}
		if ((withElt instanceof TaggedGroup)||(withElt instanceof TaggedSegment)) {
			if (((TaggedElement)withElt).tag.equals("SKIP")) return;
			if (((TaggedElement)withElt).tag.equals("RAW")&&!withRaw) return;
			//add the withElt itself unless NOTAG is specified
			if (!((TaggedElement)withElt).tag.equals("NOTAG")) {
				xmlElt = doc.createElement(((TaggedElement)withElt).tag);
				//xmlElt.setAttribute("L", Integer.toString(withElt.atLine));
				//xmlElt.setAttribute("O", Integer.toString(withElt.atOffset));
				if (((TaggedElement)withElt).suffix!=null && ((TaggedElement)withElt).suffix.length()>0) {
					namespace = getNamespace(((TaggedElement)withElt).suffix);
					xmlElt.setAttribute("xmlns",namespace);
				}
				fillThis.appendChild(xmlElt);
				fillThis = xmlElt; //the trick is here!
			}
			//in all cases fill up contents
			for (int i=0; i<((TaggedElement)withElt).subElts.size();i++) {
				fillDOM(doc,fillThis,((TaggedElement)withElt).subElts.get(i), withRaw);
				}
			return;
		}
		//one case left: withElt is instanceof UnTaggedElement
		if (!withRaw) return;
		xmlElt = doc.createElement("RAW");
		xmlElt.appendChild(doc.createTextNode(withElt.data));
		xmlElt.setAttribute("L", Integer.toString(withElt.atLine));
		xmlElt.setAttribute("O", Integer.toString(withElt.atOffset));
		fillThis.appendChild(xmlElt);
		
		return;
	}
	
	/**
	 * Provides an XML rendering of the tagged message as resulting from parsing, i.e. 
	 * next to a {@link #parse(String, LineNumberReader, int)} method call.
	 * <p>Data and Marks elements whose names start with the special character @ are promoted as
	 * attributes of the parent element.</p>
	 * 
	 * @param withRAW	tells to generate RAW element or not;
	 * 					i.e. either UnTagged elements else those explicitly tagged as 'RAW'
	 * @param indent	asks for indentation (only line breaks on elements as true indentation does not work!)
	 * @return the XML output in a {@link StringWriter}
	 * @throws FactoryConfigurationError 
	 * @throws ParserConfigurationException 
	 * @throws TransformerFactoryConfigurationError 
	 * @throws TransformerException 
	 */
	public StringWriter getXML(boolean withRAW, boolean indent) throws ParserConfigurationException, FactoryConfigurationError, TransformerFactoryConfigurationError, TransformerException {
		
		StringWriter sWout = new StringWriter(3000); //3000 is only initial capacity
		
		if ((message==null)||(message.subElts==null)) return null;
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		//builderFactory.setIgnoringElementContentWhitespace(true);
		//builderFactory.setValidating(true);
		//builderFactory.setNamespaceAware(true); --> causes a problem with TIBCO environment
		
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document doc = builder.newDocument();
		try { doc.setXmlStandalone(true);
		} catch (org.w3c.dom.DOMException de) {
		}
		Element root = doc.createElement(message.tag); 
		//Element root = doc.getDocumentElement();
		namespace = getNamespace(message.suffix);
		if (namespace!=null && namespace.length()>0 && !namespace.equalsIgnoreCase("NoNamespace")) root.setAttribute("xmlns",namespace);
		root.setAttribute("messageID", messageID);
		//root.setPrefix(null) would generate NAMESPACE_ERR!
		doc.appendChild(root);
		
		fillDOM(doc, root, message,withRAW);
		
		//Florent partially solved the problem with not-working-indented outputs!
		//The trick being to set attributes on the factory in addition to the tranformer
		TransformerFactory factory = TransformerFactory.newInstance();
		//save on line length with a low indent value because some cargo messages feature elements at a depth of 10
//		if (indent) {
//			try {
//				factory.setAttribute("indent-number", "3"); 
//			} catch (Exception e) { //exception caught when running in JRE 1.4 that does not support this attribute! 
//			}
//		}
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		//transformer.setOutputProperty("omit-xml-declaration","yes");
		if (indent)  { 
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
			transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "3");
		}
		//tran.setOutputProperty("standalone","yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(sWout);
		transformer.transform(source, result);
		
		return sWout;
	}
	
	/**
	 * This Inner Class sub-classes a ListIterator such as to support methods 
	 * more specific to the handling of the Exception list recorded by the parser.
	 * 
	 * @author bernardH
	 */
	public class ExceptionListIterator implements ListIterator {

		private ListIterator<ParserException> iterEx;
		
		public ExceptionListIterator(ArrayList<ParserException> recEx) {
			this.iterEx = recEx.listIterator();
		}
		
		public void add(Object arg0) {
			throw new UnsupportedOperationException("P021 Operation not allowed on the list of recorded exceptions");			
		}

		public int totalExceptionsCount() {
			return Parser.this.allExceptionsCount;
		}
		
		public int fatalExceptionsCount() {
			return Parser.this.fatalCount;
		}
		
		public boolean hasNext() {
			return iterEx.hasNext();
		}

		public boolean hasNextFatal() {
			ParserException prEx;
			boolean hasIt;
			if (iterEx.hasNext()) {
				prEx = iterEx.next();
				if (prEx._impact.equals(Impact.FATAL)) {
					hasIt = true;
				}
				else {
					hasIt = hasNextFatal();
				}
				iterEx.previous(); //neutralise last next().
				return hasIt;
			}
			else return false;
		}

		public boolean hasPrevious() {
			return iterEx.hasPrevious();
		}

		public Object next() {
			return iterEx.next();
		}

		public ParserException nextException() {
			return iterEx.next();
		}

		public ParserException nextFatalException() {
			ParserException prEx;
			if (iterEx.hasNext()) {
				prEx = iterEx.next();
				if (prEx._impact.equals(Impact.FATAL))
					return prEx;
				else return nextFatalException();
			} else return null;
		}

		public int nextIndex() {
			return iterEx.nextIndex();
		}

		public Object previous() {
			return iterEx.previous();
		}

		public int previousIndex() {
			return iterEx.previousIndex();
		}

		public void remove() {
			throw new UnsupportedOperationException("Operation not allowed on the list of recorded Parser exceptions");			
		}

		public void set(Object arg0) {
			throw new UnsupportedOperationException("Operation not allowed on the list of recorded Parser exceptions");			
		}		
	}
	
	/**
	 * Provides a List Iterator on the Array List of recorded exceptions (stored
	 * in parser state next to a {@link #parse(String, String, int)}.
	 * <p>
	 * Note that the last exception that possibly caused the 
	 * MaxFatal or MaxAllExceptions counts to be exceeded (and thrown) is also
	 * recorded.
	 * </p>
	 *  
	 * @return	an extended ListIterator supporting extra methods for improved
	 * 			iteration through a {@link Parser} state.
	 */
	public ExceptionListIterator exceptionIterator() {
		return new ExceptionListIterator(this.recExceptions); 
	}
	
	
	/**
	 * Adjust the line offsets of all recorded exceptions by adding the given adjustment value to
	 * all line offsets (relevant whenever the input message is set of lines). The adjustment can be either
	 * positive or negative and is actually added to exceptions' line offsets.<br>
	 * Consistency rule: Existing line offsets which may yield a negative value next to adjustment are not updated.
	 * The use of this method in a proper message parsing context shall never yield such case.
	 * <p>
	 * This method is a facility to perform the parsing of a input message on the text message body
	 * part alone (e.g. without the message's header lines), and then report line offsets of any parsing errors
	 * relative to the very beginning of the message, header lines included. If an input interchange contains
	 * several messages, this facility helps parsing each message in turn but reports offsets with regard to 
	 * the global interchange.
	 * </p>
	 * <p>Release note: a future release is planned that will de-pollute and 
	 * normalize well-known EDI formats like EDIFACT and X12
	 * before Parsing, and make segment offsets like line offsets.</p>
	 * 
	 * @param adjustment
	 */
	public void adjustExceptionsLineOffsets(int adjustment) {
		if (this.message==null) return; //no parsing yet performed
		if (this.recExceptions.size()==0) return; //nothing to adjust
		//ELSE
		ListIterator<ParserException> iter = this.recExceptions.listIterator();
		ParserException e;
		while (iter.hasNext()) {
			e = iter.next();
			e.adjustLineOffset(adjustment);
			//Cool, we do not need to process the next depth because we do not
			//have nested ParserExceptions featuring such offset details
			//nested exceptions report Mapping or Validation issues
			//Exception c = (Exception) e.getCause();
		}
	}
	
	private String rootNamespace = "";
	
	/**
	 * Sets the base XML namespace for every following {@link #parse(String, LineNumberReader, int)}
	 * invocation followed by {@link #getXML(boolean, boolean)}. 
	 * <p>
	 * This namespace is not reset in between parse(...) calls.
	 * </p><p>
	 * The default namespace is "http://www.reverseXSL.com/FreeParser".
	 * Calling this method with null or empty arguments does reset the namespace to the
	 * default (as if setBasenamespace() was never invoked). Note that the namespace 
	 * can be set via the java API alse the <b>SET BASENAMESPACE</b> statement in DEF files. In case
	 * both are used, the API takes precedence.
	 * </p>
	 * @param bns	the namespace that applies to this parser instance, e.g. "http://www.reverseXSL.com/Cargo"
	 */
	public final void setBaseNamespace(String bns) {
		if (bns==null||bns.length()<=0) {
			//fall back on defaults
			init();
		}
		//no namespace validation implemented, any string accepted
		else this.rootNamespace = bns;
	}


	/**
	 * gets the namespace as set in context plus any supplied suffix.
	 * 
	 * @param suffix namespace chunk to append to the base name space
	 * @return complete name space URI as string
	 */
	final String getNamespace(String suffix) {
		if (rootNamespace==null||rootNamespace.length()<=0) {
			//fall back on defaults
			init();
			rootNamespace=registeredNamespace;
		}
		//At this point the root namespace is always set to something.
		//C. Ready to append the suffix argument
		if (suffix==null||suffix.length()<=0) return rootNamespace;
		if (suffix.equals("/")) return rootNamespace; //a single / suffix is also a means to ask for just the rootNamespace
		return (suffix.startsWith("/")?rootNamespace+suffix:rootNamespace+"/"+suffix);
	}

	
	/**
	 * Dumps an overview of the parser state into a text string. 
	 * Useful for debugging Definitions under development and testing their 
	 * effect on sample messages.
	 * 
	 * @return complete parser state dump as text.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer(50000);
		sb.append("------PARSER STATE DUMP---------\n");
		
		if (this.message==null) return "------PARSER STATE DUMP == VOID! NO Parsing yet performed---\n"; 
		//occurence table
		sb.append("-1-RECORDED OCCURENCES:\n");
		sb.append("   Note: parent occurences appear once with a total count next to all their childs\n");
		if (this.recMinMax.size()==0) sb.append("-no records-\n");
		else {
			ListIterator<Occurence> iter = this.recMinMax.listIterator();
			while (iter.hasNext()) {
				sb.append(iter.next().toString());
			}
		}
		//condition table
		sb.append("\n-2-COLLECTED NAMED CONDITIONS CRITERIA:\n");
		if (this.recNamedCond.size()==0) sb.append("-no records-\n");
		else {
			ArrayList lst;
			Map.Entry men;
			Iterator iset, ilst; 
			
			Set set = (Set) this.recNamedCond.entrySet();
			iset = set.iterator();
			while (iset.hasNext()) {
				men = (Map.Entry)iset.next();
				sb.append("Name: "+(String)men.getKey()+" ");
				lst = (ArrayList) men.getValue();
				ilst = lst.iterator();
				sb.append("("+lst.size()+" token(s))\n");
				while (ilst.hasNext()) {
					sb.append(((Token)ilst.next()).toString());
				}
				
			};
		}

		sb.append("\n-3-RAW TAGGED MESSAGE:\n");
		sb.append(this.message.toString(0));
		sb.append("END-OF-MESSAGE\n");

		//Exception details
		sb.append("\n-4-RECORDED EXCEPTIONS:\n");
		sb.append("  Counted Exceptions         : "+allExceptionsCount);		
		sb.append("\n   of which Fatal Exceptions : "+fatalCount);
		if (this.recExceptions.size()==0) sb.append("\n  -no records-");
		else {
			ListIterator<ParserException> iter = this.recExceptions.listIterator();
			int i = 1;
			ParserException e;
			Exception c;
			while (iter.hasNext()) {
				sb.append("\n  ["+i+"] ");
				e = iter.next();
				sb.append(e.getMessage());
				c = (Exception) e.getCause();
				if (c!=null) {
					sb.append("\n  ...caused by ");
					sb.append(c.getMessage());
				}
				i++;
			}
		}
		
		sb.append("\n");
		return sb.toString();
	}

	/**
	 * @return the total count of exceptions recorded during the last parser execution
	 */
	public int getExceptionsCount() {
		return this.allExceptionsCount;
	}
	

	
}
