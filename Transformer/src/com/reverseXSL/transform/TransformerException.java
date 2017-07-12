package com.reverseXSL.transform;

import com.reverseXSL.exception.FormattedException;

/**
 * Multilingual Transformer & TransformerFactory error messages.
 * See com.reverseXSL.exception.messages.properties 
 * for the default english-language messages.
 * 
 * @author bernardH
 *
 */
public class TransformerException extends FormattedException {
	
	private static final long serialVersionUID = 2287734776730525132L;
	
	protected TransformerException(String code, Throwable t, Object[] args) {
		super(code, t, args);
	}
	
	/**
	 * R001 Duplicate Pattern Keys in Mapping Selection Table! The selection at line [{0}] is unreachable because its pattern is identical to ["{1}"] at line [{2}].
	 */
	public static class DuplicateMappingSelectionKey extends TransformerException {
		private static final long serialVersionUID = 7923403227425694874L;
		
		public DuplicateMappingSelectionKey(int thisLineNb, String patternKey, int originalLineNb) {
			super("R001",null,new String[]{Integer.toString(thisLineNb),patternKey,Integer.toString(originalLineNb)});
		}
	}
	
	/**
	 * R002 Unexpected data in Mapping Selection Table at line [{0}]:[{1}]! Please refer to acceptable line formats as described within the file itself or the javaDoc.
	 */
	public static class UnexpectedMappingSelectionLine extends TransformerException {
		private static final long serialVersionUID = -55664738540199791L;

		public UnexpectedMappingSelectionLine(int thisLineNb, String line) {
			super("R002",null,new String[]{Integer.toString(thisLineNb),line});
		}
	}

	/**
	 * R003 Overloading attribute in Mapping Selection Table! line [{0}]:[{1}] redefines the existing [{2} = "{3}"] for selection rule starting line [{4}].
	 */
	public static class OverloadedMappingSelectionAttribute extends TransformerException {
		private static final long serialVersionUID = -664362200501197035L;

		public OverloadedMappingSelectionAttribute(int thisLineNb, String line,String param,String value,int refLine) {
			super("R003",null,new String[]{Integer.toString(thisLineNb),line,param,value,Integer.toString(refLine)});
		}
	}

	/**
	 * R004 Invalid regex [{0}] at line [{1}]! {2}
	 */
	public static class InvalidRegexSyntax extends TransformerException {
		private static final long serialVersionUID = -7634590638632086416L;

		public InvalidRegexSyntax(String regex, int thisLineNb, String explain) {
			super("R004",null,new String[]{regex,Integer.toString(thisLineNb),explain});
		}
	}
	
	/**
	 * R005 Failed to match this message [{0}... full range ...{1}] against any mapping selection regex from [{2}]! Cannot determine how to transform input.
	 */
	public static class MappingSelectionFailure extends TransformerException {
		private static final long serialVersionUID = -5667098277388165384L;

		public MappingSelectionFailure(String startBit, String endBit, String mstName) {
			super("R005",null,new String[]{startBit,endBit,mstName});
		}
	}
	/**
	 * R006 Failed to match this message [{0}... first {3} bytes subset ...{1}] against any mapping selection regex from [{2}]! Cannot determine how to transform input.
	 */
	public static class MappingSelectionFailure_MsgSubset extends TransformerException {
		private static final long serialVersionUID = 3271263034629499030L;

		public MappingSelectionFailure_MsgSubset(String startBit, String endBit, String mstName, int lgth) {
			super("R006",null,new String[]{startBit,endBit,mstName,Integer.toString(lgth)});
		}
	}
	
	

}
