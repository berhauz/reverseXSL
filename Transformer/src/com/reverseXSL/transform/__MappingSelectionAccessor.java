package com.reverseXSL.transform;

import java.io.IOException;
import java.io.Reader;


public class __MappingSelectionAccessor {

	MappingSelection ms;
	
	public __MappingSelectionAccessor() {
		ms = new MappingSelection();
	}
	
	public __MappingSelectionAccessor(Reader r) throws TransformerException, IOException {
		ms = new MappingSelection(r);
	}
	
	public String matchEntry(String s) {
		return ms.matchEntry(s).toString();
	}
	
	public String toString() {
		return ms.dump();
	}
	
}
