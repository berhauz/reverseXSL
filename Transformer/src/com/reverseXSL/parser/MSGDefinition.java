package com.reverseXSL.parser;



import java.util.HashMap;
import com.reverseXSL.types.Cardinality;


/**
 * The Message definition is the top-level {@link SEGDefinition segment definition} 
 * matching the whole message itself.
 * <p>
 * A Message definition is a segment definition with additional restrictions 
 * as follows:<ul>
 * <li>Cardinality must be Mandatory. Consequently, no named condition 
 * can be associated to the message itself.
 * <li>Min, max and accept loop counts are all at 1
 * <li>The keyword is changed from SEG to MSG
 * <li>The END line marks the end of the message definition
 * <li>A MSG definition always starts in column 0; 
 * in other words, there is no depth '|' indicator in front of such line.
 * <p>Just like SEG definitions, the MSG definition line accepts a namespace suffix argument that will be
 * appended to the licenced or default namespace and cover here the root element of 
 * the XML document and all descendants implicitly.</p>
 */
final class MSGDefinition extends SEGDefinition {

	MSGDefinition(Definition refDef) {
		super(refDef);
	}

	MSGDefinition(int _linenb, String line, final HashMap nCond, Definition refDef) throws ParserException {
		//execute a SEGment read
		super(_linenb, line, nCond, refDef);
		//then verify the additional constraints
		if ((cardinality!=Cardinality.MANDATORY)||(occMin!=1)||(occMax!=1)||(occAccept!=1)) 
			throw new ParserException.DEFErrorMSGisM11ACC1(_linenb);
		if (depth!=0) 
			throw new ParserException.DEFErrorMSGatDepth0(_linenb);
	}
	/* (non-Javadoc)
	 * @see com.reverseXSL.parser.GSDDefinition#getName()
	 */
	String getName() {
		return "MSGDefinition";
	}


}
