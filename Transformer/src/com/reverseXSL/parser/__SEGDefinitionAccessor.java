package com.reverseXSL.parser;

import java.util.ArrayList;

public class __SEGDefinitionAccessor {
	
	SEGDefinition sdef;
	
	public __SEGDefinitionAccessor(SEGDefinition sd) {
		sdef = sd;
	}
	public int getDepth() {
		return sdef.depth;
	}
	public int getOccMin() {
		return sdef.occMin;
	}
	public int getOccMax() {
		return sdef.occMax;
	}
	public int getOccAccept() {
		return sdef.occAccept;
	}
	public ArrayList getSubElts() {
		return sdef.subElts;
	}
	public String getXmlTag() {
		return sdef.xmltag;
	}
}
