package com.reverseXSL.parser;

public class __ParserAccessor {

	Parser p;
	
	public __ParserAccessor() {
		p= new Parser();
	}
	
	public __ParserAccessor(Parser parg) {
		p= parg;
	}

	public String getNamespace(String suf) {
		return p.getNamespace(suf);
	}

	public void setBaseNamespace(String bns) {
		p.setBaseNamespace(bns);
		return;
	}


}
