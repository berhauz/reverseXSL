package com.reverseXSL.parser;

import java.util.HashMap;

/**
 * Accessor to a Segment definition restricted to use by RegexCheck
 *
 */
public class SD {

	SEGDefinition sd = null;
	SEGDefinition.CutFunction.CutContext cc = null;
	
  public SD (int _linenb, String line, final HashMap nCond) throws ParserException {
		// TODO Auto-generated method stub
	  sd = new SEGDefinition(_linenb,line,nCond,null);
	}

  public void cut (String target) {
	  cc = sd.cutFunction.cut(target, 0);
  }
  
  public boolean hasNext() {
	  return sd.cutFunction.hasNext(cc);
  }
  
  public String getNext() {
	  return sd.cutFunction.getNext(cc);
  }

}
