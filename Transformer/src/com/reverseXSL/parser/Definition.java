package com.reverseXSL.parser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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

/**
 * A Definition object holds the complete hierarchical description of a text-based 
 * structured message. In other words, it contains all instructions for the correct 
 * interpretation of an input message by the {@link com.reverseXSL.parser.Parser Parser}.
 * <p>The Definition class models the object in memory whereas a DEF file is the same object represented in
 * a sequential text file.
 * </p><p>
 * The DEF file contains a optional SET BASENAMESPACE statement, followed by a 'Named Conditions' block, 
 * followed by the larger 'MSG...END' block that describes the hierarchical message structure. 
 * </p><p>
 * A DEF file has the following general layout:<br>
 * <code>
 * SET BASENAMESPACE ... <i>optional specification of the root element namespace</i><br>
 * COND	... <i>specification of interdependency rules (conditions)</i><br>
 * COND	... <br>
 * <i>... more COND lines<br>
 *  Comments may be written elsewhere; by convention, these are lines starting 
 *  with a space or tab character.</i><br>
 * MSG	<i>The top level segment is the message itself and is level 0 by definition<br>
 * 	The top level segment is noted MSG instead of SEG, although it is a SEGment<br>
 *  The '|' characters are actually part of the syntax of the DEF file itself.</i><br>
 * |SEG	<i>first segment at level 1 with a single data piece</i><br>
 * ||D			<i>data and other segments here are at level 2, hence prefixed ||</i><br>
 * |SEG	<i>second segment with 4 data pieces</i><br>
 * ||D			<i>first data element</i><br>
 * ||D			<i>second data element</i><br>
 * ||SEG		<i>the third data element is a "sub-segment", i.e. a composite</i><br>
 * |||D			<i>first sub-element</i><br>
 * |||D			<i>second sub-element</i><br>
 * ||D			<i>fourth data element, back to level 2</i><br>
 * |SEG	<i>third segment within the top-level segment, i.e. at level 1</i><br>
 * ||D<br>
 * ||D<br>
 * |GRP 	<i>first group at level 1, a group is only a logical grouping of segments or 
 * other groups that are then, by principle, at the next level</i><br>
 * ||SEG		<i>a segment at level 2, first within the group</i><br>
 * |||D<br>
 * |||D<br>
 * |MARK	<i>evaluates a condition on the fly and marks it at this point in the message</i><br>
 * ||GRP		<i>a sub-group at level 2 within the level 1 group</i><br>
 * |||D 		<i>Data elements may be placed directly in groups as well</i><br>
 * |||SEG 		<i>a segment at level 3 within the previous level 2 group</i><br>
 * ||||SEG			<i>a sub-segment, then at level 4</i><br>
 * |||| etc.	<i>the rest of level 4 shall contain data and sub-segments, 
 * and sub-segments may again contain segments and groups!</i><br>
 * |||SEG<br>
 * ||SEG		<i>back to level 2, hence next to the previous group</i><br>
 * ||GRP		<i>...more groups, etc.</i><br>
 * END	<i>marks the end of the message definition</i><br>
 * </code>
 * Please refer to the MS-Word documentation 'Message DEF file specs.doc' for a complete description of the 
 * Definition objects and file syntax.
 * 
 * @author bernardH
 *
 */
public final class Definition {

	//constant: limit on the depth of nesting levels, used for sizing structures
	//NOTE: never put a value above 50!!!! (see IDENT in Parser to understand why, and other similar tricks in GSDElement subclasses)
	static final int MAXDEPTH = 20;
	
	//MSGDefinition root
	MSGDefinition msgDef = null;
	
	//Named Conditions map
	HashMap namedCond = new HashMap();
	
	//base Namespace as specified in a SET BASENAMESPACE statement
	String defBaseNamespace = null; 
	//release character, as specified in a SET RELEASECHARACTER statement
	Character releaseChar = null; 
	//namespace signature
	String defSignature = null;
	
	//working namespace when generating a schema or XML sample
	private String namespace = null;
	
	//delayed exception: we complete the DEF load and then we throw it! used about licensing
	private ParserException delayedException = null;

	/**
	 * This is the only way to create a new Definition object. Always empty at the begining.
	 * <p>
	 * It shall then be loaded with a definition
	 * using {@link #loadDefinition(LineNumberReader)}. 
	 * <p>
	 * Once loaded, pass it as argument to a Parser constructor.  
	 */
	public Definition() {
		super();
	}
	
	
	//local supporting method
	private String settings(LineNumberReader inputDEF, String atLine, int atLineNb) throws IOException, ParserException {
		//the SET BASENAMESPACE syntax still accepts a legacy [signature] string yet is ignored.
		final String _SET_BASENAMESPACE = "(?i)^SET\\s+BASENAMESPACE\\s+\"(.*?)\"(\\s+.*$|\\s*$)";
		final String _SET_RELEASECHARACTER = "(?i)^SET\\s+RELEASECHARACTER\\s+(['\"])(.*?)\\1\\s*$";
		//read and apply any SETting statement
		String line = atLine;
		int lineNb = atLineNb;
		Pattern p_SET_BASENAMESPACE = null;
		Pattern p_SET_RELEASECHARACTER = null;
		Matcher m = null;
		p_SET_BASENAMESPACE = Pattern.compile(_SET_BASENAMESPACE);
		p_SET_RELEASECHARACTER = Pattern.compile(_SET_RELEASECHARACTER);

		while (line!= null) {
			//returns the first non-SET line (or null) for the follower
			if (! line.startsWith("SET")) return line; 
			//analyse which SETTING
			while (true) {
				m = p_SET_BASENAMESPACE.matcher(line);
				if (m.matches()) {
					defBaseNamespace = m.group(1);
					//force namespace exceptions NOW, via a dummy parser instance, but throw LATER!
					Parser p = new Parser();
					p.setBaseNamespace(defBaseNamespace);
					break;
				}
				m = p_SET_RELEASECHARACTER.matcher(line);
				if (m.matches()) {
					String charSpec = m.group(2);
					if (charSpec.charAt(0)=='\\') {
						// assume \ or \u0000 or \U0000 or \\
						if (charSpec.length()<=1) releaseChar = new Character('\\');
						else if (charSpec.charAt(1)=='\\') releaseChar = new Character('\\');
						else if (charSpec.length()>=6 && charSpec.matches("(?i)\\\\U[0-9ABCDEF]{4}")) {
							//assume Unicode char
							releaseChar = new Character( (char)Integer.parseInt(charSpec.substring(2),16));
						}
						else throw new ParserException.DEFErrorInvalidReleaseChar(lineNb,m.group(1)+charSpec+m.group(1));
					} else {
						//any other single char
						if (charSpec.length()<=1) releaseChar = new Character(charSpec.charAt(0));
						else throw new ParserException.DEFErrorInvalidReleaseChar(lineNb,m.group(1)+charSpec+m.group(1));	
					}
					break;
				}
				//next SETting test can be inserted here
				//... extension point...
				//
				//no break above means nothing matched! 
				throw new ParserException.DEFErrorInvalidSetting(lineNb,line);
			}
			line = GSDDefinition.readNonCommentLine(line, inputDEF);
			lineNb = inputDEF.getLineNumber();
		}
		return null;
	}

	//local supporting method
	private String fillCONDMap(LineNumberReader inputDEF, String atLine, int atLineNb) throws IOException, ParserException {
		//read and fill up the conditions block
		CONDDefinition condDef = null;
		String line = atLine;
		int lineNb = atLineNb;

		while (line!= null) {
			//returns the first non-COND line (or null) for the follower
			if (! line.startsWith("COND")) return line; 
			//load COND
			condDef = new CONDDefinition(lineNb,line);
			if (this.namedCond.put(condDef.name, condDef)!=null) 
				throw new ParserException.DEFErrorDuplicateCOND(condDef.name,lineNb,line);
			line = GSDDefinition.readNonCommentLine(line, inputDEF);
			lineNb = inputDEF.getLineNumber();
		}
		return null;
	}
	
	//local supporting method
	private String fillMSG(LineNumberReader inputDEF, String atLine, int atLineNb) throws IOException, ParserException {
		//read and fill up the root message
		String line = atLine;
		int lineNb = atLineNb;

		//there's a single MSG DEF line to expect (SEG keyword is tolerated, yet MSG definition constraints do apply)
		this.msgDef = new MSGDefinition(lineNb, line, this.namedCond, this);
		line = msgDef.fill(inputDEF, GSDDefinition.readNonCommentLine(line, inputDEF), inputDEF.getLineNumber(), this.namedCond);
		return line;
	}
	
	/**
	 * Build a Definition object from a {@link LineNumberReader} containing 
	 * a complete DEF file. 
	 * <p>
	 * Definition objects contain pure 'read-only' definitions and thus 
	 * can be shared by as many threads as desired.
	 * 
	 * 
	 * @param inputDEF line number reader containing a complete DEF file
	 * @throws IOException 
	 * @throws ParserException 
	 */
	public void loadDefinition(LineNumberReader inputDEF) throws IOException, ParserException {
		
		String currentLine = "";
		currentLine = GSDDefinition.readNonCommentLine(currentLine, inputDEF);
		int currentLineNb = inputDEF.getLineNumber();

		//settings
		currentLine = settings(inputDEF, currentLine, currentLineNb);	
		currentLineNb = inputDEF.getLineNumber();

		//Named conditions
		currentLine = fillCONDMap(inputDEF, currentLine, currentLineNb);	
		currentLineNb = inputDEF.getLineNumber();
		
		//Message
		currentLine = fillMSG(inputDEF, currentLine, currentLineNb);
		currentLineNb = inputDEF.getLineNumber();
		
		//END
		if (currentLine.startsWith("END")) {
			//check for delayed exception
			if (delayedException!=null) throw delayedException;
			return;
		}
		
		//anything else is an error
		throw new ParserException.DEFErrorExpectedEND(currentLineNb,currentLine);	
	}
	
	/**
	 * Supporting method for {@link #getXMLSample(boolean, boolean)}.
	 * 
	 * @param doc	the XML document and element factory
	 * @param fillIt	the XML element to fill up
	 * @param withElt	the piece to transform into XML elements
	 * @param withSKIP	whether elements with the special SKIP tag must be generated or not
	 * @param dummyParser used for accessing namespace handling facilities
	 */
	private void fillDOM (Document doc, Element fillIt, GSDDefinition withElt, final boolean withSKIP, Parser dummyParser) {
		Element xmlElt;
		Element fillThis = fillIt;
		
		if (withElt instanceof MSGDefinition) {
			for (int i=0; i<withElt.subElts.size();i++) {
				fillDOM(doc,fillThis,(GSDDefinition)withElt.subElts.get(i), withSKIP, dummyParser);
				}
			return;
		}
		if (withElt instanceof DataDefinition) {
			if (withElt.xmltag.equals("SKIP")&&!withSKIP) return;
			if (withElt.xmltag.charAt(0)=='@') {
				//promote as attribute to parent element
				fillThis.setAttribute(withElt.xmltag.substring(1),((DataDefinition)withElt).description);
			} 
			else {
				//regular child element
				xmlElt = doc.createElement(withElt.xmltag);
				xmlElt.appendChild(doc.createTextNode("{"+((DataDefinition)withElt).occMin
						+".."+((DataDefinition)withElt).occMax
						+"} "+((DataDefinition)withElt).description));
				//xmlElt.setAttribute("Z", Integer.toString(withElt.zzz));
				fillThis.appendChild(xmlElt);
			}
			return;
		}
		if (withElt instanceof MARKDefinition) {
			if (withElt.xmltag.charAt(0)=='@') {
				//promote as attribute to parent element
				fillThis.setAttribute(withElt.xmltag.substring(1),
						((MARKDefinition)withElt).conditionName
						+".matches("+((MARKDefinition)withElt).evalPattern+")? "+((MARKDefinition)withElt).yesString
						+" : "+((MARKDefinition)withElt).noString);
			} 
			else {
				xmlElt = doc.createElement(withElt.xmltag);
				xmlElt.appendChild(doc.createTextNode(
						((MARKDefinition)withElt).conditionName
						+".matches("+((MARKDefinition)withElt).evalPattern+")? "+((MARKDefinition)withElt).yesString
						+" : "+((MARKDefinition)withElt).noString));
				//xmlElt.setAttribute("Z", Integer.toString(withElt.zzz));
				fillThis.appendChild(xmlElt);
			}
			return;
		}
		if ((withElt instanceof GRPDefinition)||(withElt instanceof SEGDefinition)) {
			if (withElt.xmltag.equals("SKIP")&&!withSKIP) return;
			//add the withElt itself unless NOTAG is specified
			if (!withElt.xmltag.equals("NOTAG")) {
				xmlElt = doc.createElement(withElt.xmltag);
				//xmlElt.setAttribute("Z", Integer.toString(withElt.zzz));
				if (withElt.suffix!=null && withElt.suffix.length()>0) {
					namespace = dummyParser.getNamespace(withElt.suffix);
					xmlElt.setAttribute("xmlns",namespace);
				}
				fillThis.appendChild(xmlElt);
				fillThis = xmlElt; //the trick is here!
			}
			//in all cases fill up contents
			for (int i=0; i<withElt.subElts.size();i++) {
				fillDOM(doc,fillThis,(GSDDefinition) withElt.subElts.get(i), withSKIP, dummyParser);
				}
			return;
		}
		
		return;
	}
	
	/**
	 * Generates a sample XML message with element descriptions as data values.
	 * <p>
	 * The current implementation generates only one instance of each element.
	 * 
	 * @param indent whether we ask to indent the XML document
	 * @param withSKIP whether we ask to generate XML elements with SKIP tags
	 * @return	the XML document as string
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 */
	public StringWriter getXMLSample(boolean indent, boolean withSKIP) throws TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException {
		StringWriter sWout = new StringWriter(3000); //3000 is only initial capacity
		Parser p = new Parser(this,0,0); //needed for accessing namespace handling functions
		
		if ((msgDef==null)||(msgDef.subElts==null)) return null;
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setNamespaceAware(true);
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		//Document doc = builder.getDOMImplementation().createDocument("http://www.champ.aero/broker","acb:"+message.tag,null );
		Document doc = builder.newDocument();
		Element root = doc.createElement(msgDef.xmltag); 
		//Element root = doc.getDocumentElement();
		namespace = p.getNamespace(msgDef.suffix);
		if (namespace!=null) root.setAttribute("xmlns",namespace);
		Calendar clndr = Calendar.getInstance();
		root.setAttribute("messageID", "sample"+clndr.get(Calendar.YEAR)+"-"
				+(clndr.get(Calendar.MONTH)+1)+"-"
				+clndr.get(Calendar.DAY_OF_MONTH) );
		//root.setPrefix(null) would generate NAMESPACE_ERR!
		doc.appendChild(root);
		
		fillDOM(doc, root, msgDef, withSKIP, p);
		        
		//Florent finally solved the problem with not-working-indented outputs!
		//The trick being to set attributes on the factory! in addition to the tranformer
		TransformerFactory factory = TransformerFactory.newInstance();
		//save on line length with a low indent value because some cargo messages feature elements at a depth of 10
		if (indent) try { 
			factory.setAttribute("indent-number", "3"); 
		} catch (Exception e) { //eception caught when running in JRE 1.4 that does not support this attribute! 
		}
		Transformer tran = factory.newTransformer();
		tran.setOutputProperty(OutputKeys.METHOD, "xml");
		//transformer.setOutputProperty("omit-xml-declaration","yes");
		if (indent)  { 
			tran.setOutputProperty(OutputKeys.INDENT, "yes");
			tran.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
			tran.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "3");
		}
		//tran.setOutputProperty("standalone","yes");
		DOMSource src = new DOMSource(doc);
		StreamResult sr = new StreamResult(sWout);
		tran.transform(src, sr);
		
		return sWout;

	}

	/**
	 * In order to trace the definition itself, i.e. generate again the DEF file
	 * but without any of the original comments. 
	 *
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		//Conditions
		Iterator iter =	this.namedCond.values().iterator();
		while (iter.hasNext()) {
			sb.append((iter.next()).toString());
			sb.append('\n');
		}
		sb.append('\n');
		
		//Message
		sb.append(this.msgDef.toString());
		
		sb.append("END\n");
		
		return sb.toString();
		
		
	}
}
