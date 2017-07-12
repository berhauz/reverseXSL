package com.reverseXSL.parser;

import com.reverseXSL.exception.FormattedException;
import com.reverseXSL.types.Impact;

/**
 * Multilingual Parser error messages. 
 * See com.reverseXSL.exception.messages.properties 
 * for the default english-language messages.
 * 
 * 
 * @author bernardH
 *
 */
public class ParserException extends FormattedException {

	/**
	 * FATAL or WARNING exception impact affecting the broker message 
	 * in proper (not the broker system in general); default is FATAL.
	 * @see Impact
	 */
	Impact _impact = Impact.FATAL;
	
	public String getImpact() {
		return (_impact.toString());
	}

	private static final long serialVersionUID = -3865687699605848879L;

	protected ParserException(String code, Throwable t, Object[] args) {
		super(code, t, args);
	}
	public void adjustLineOffset(int adjustment) {
		//there is a possibility of fixing lineOffsets in all recorded parser exceptions!
		//no operation by default
	} 
	
	/**
	 * D001 DEF file error! Invalid [{0}] syntax at line [{1}]:[{2}], expected like [{3}].
	 */
	public static class DEFErrorInvalidSyntax extends ParserException {
		private static final long serialVersionUID = 4867713979976304786L;
		
		public DEFErrorInvalidSyntax(String msgOrSeg,int lineNb, String line, String pattern) {
			super("D001",null,new String[]{msgOrSeg,Integer.toString(lineNb),line, pattern});
		}
	}
	
	
	/**
	 * D002 DEF file error! Invalid [{0}] syntax at line [{1}]:[{2}], extra chars found after [{3}].
	 */
	public static class DEFErrorExtraChars extends ParserException {
		private static final long serialVersionUID = -762379742840192078L;
		
		public DEFErrorExtraChars(String msgOrSeg,int lineNb, String line, String pattern) {
			super("D002",null,new String[]{msgOrSeg,Integer.toString(lineNb),line, pattern});
		}
	}

	
	/**
	 * D003 DEF file error! Nesting levels over [{0}] are not supported in this version.
	 */
	public static class DEFErrorOverDepth extends ParserException {
		private static final long serialVersionUID = -6946417398874812454L;

		public DEFErrorOverDepth(int max) {
			super("D003",null,new String[]{Integer.toString(max)});
		}
	}
	
	
	/**
	 * D004 DEF file error! Missing child definitions for element near line [{0}]:[{1}].
	 */
	public static class DEFErrorMissingChild extends ParserException {
		private static final long serialVersionUID = -4087572149065348523L;

		public DEFErrorMissingChild(int lineNb, String line) {
			super("D004",null,new String[]{Integer.toString(lineNb),line});
		}
	}
	/**
	 * D005 DEF file error! Incorrect depth of element near line [{0}]:[{1}].
	 */
	public static class DEFErrorBadDepth extends ParserException {
		private static final long serialVersionUID = -60793999294510513L;

		public DEFErrorBadDepth(int lineNb, String line) {
			super("D005",null,new String[]{Integer.toString(lineNb),line});
		}
	}
	/**
	 * D006 DEF file error! Invalid syntax at line [{0}]:[{1}], expected one of [{2}].
	 */
	public static class DEFErrorWhatElement extends ParserException {
		private static final long serialVersionUID = 8339775089012447317L;

		public DEFErrorWhatElement(int lineNb, String line, String list) {
			super("D006",null,new String[]{Integer.toString(lineNb),line, list});
		}
	}
	/**
	 * D007 DEF file error! Unexpected end of DEF file at line [{0}] (current context [{1}] at depth [{2}]).
	 */
	public static class DEFErrorUnexpectedEOF extends ParserException {
		private static final long serialVersionUID = -6759454826793315676L;

		public DEFErrorUnexpectedEOF(int lineNb, String tag, int depth) {
			super("D007",null,new String[]{Integer.toString(lineNb),tag, Integer.toString(depth)});
		}
	}
	/**
	 * D009 DEF file error! Invalid D syntax at line [{0}]:[{1}], validation pattern ["{2}"] must contain at least one capturing group.
	 */
	public static class DEFErrorMissingCapturingGroup extends ParserException {
		private static final long serialVersionUID = -4327821754778051583L;

		public DEFErrorMissingCapturingGroup(int lineNb, String line, String patrn) {
			super("D009",null,new String[]{Integer.toString(lineNb),line, patrn});
		}
	}
	
	/**
	 * D010 DEF file error! Invalid [{2}] syntax at line [{0}]:[{1}], the XML tag of a [{2}] element cannot take the value NOTAG.
	 */
	public static class DEFErrorNOTAGNotAllowed extends ParserException {
		private static final long serialVersionUID = -8030880978618678979L;

		public DEFErrorNOTAGNotAllowed(int lineNb, String line, String elt) {
			super("D010",null,new String[]{Integer.toString(lineNb),line,elt});
		}
	}
	/**
	 * D011 DEF file error! Invalid [{3}] syntax at line [{0}]:[{1}], cardinality C is required with COND, as in [{2}].
	 */
	public static class DEFErrorCardinalityCRequired extends ParserException {
		private static final long serialVersionUID = -6091827839450929754L;

		public DEFErrorCardinalityCRequired(int lineNb, String line, String like, String elt) {
			super("D011",null,new String[]{Integer.toString(lineNb),line, like, elt});
		}
	}
	/**
	 * D012 DEF file error! Invalid [{3}] syntax at line [{0}]:[{1}], cardinality M or O is required with R|T W|F, as in [{2}].
	 */
	public static class DEFErrorCardinalityMORequired extends ParserException {
		private static final long serialVersionUID = 4133675997055409051L;

		public DEFErrorCardinalityMORequired(int lineNb, String line, String like, String elt) {
			super("D012",null,new String[]{Integer.toString(lineNb),line, like, elt});
		}
	}

	/**
	 * D013 DEF file error! Duplicate name [{0}] of named condition at line [{1}]:[{2}].
	 */
	public static class DEFErrorDuplicateCOND extends ParserException {
		private static final long serialVersionUID = 5277243014465414300L;

		public DEFErrorDuplicateCOND(String name,int lineNb, String line) {
			super("D013",null,new String[]{name,Integer.toString(lineNb),line});
		}
	}

	/**
	 * D014 DEF file error! Expected END of message definition near line [{0}]:[{1}].
	 */
	public static class DEFErrorExpectedEND extends ParserException {
		private static final long serialVersionUID = -7254554352825128140L;

		public DEFErrorExpectedEND(int lineNb, String line) {
			super("D014",null,new String[]{Integer.toString(lineNb),line});
		}
	}
	/**
	 * D021 DEF file error! Invalid [{0}] syntax at line [{1}], incorrect or missing CUT function in [{2}].
	 */
	public static class DEFErrorBadCUTFunction extends ParserException {
		private static final long serialVersionUID = 3008627772469226213L;

		public DEFErrorBadCUTFunction(String msgOrSeg,int lineNb, String restOfLine) {
			super("D021",null,new String[]{msgOrSeg,Integer.toString(lineNb),restOfLine});
		}
	}
	/**
	 * D022 DEF file error! Invalid MSG syntax at line [{0}], cardinality must be M 1 1 ACC 1.
	 */
	public static class DEFErrorMSGisM11ACC1 extends ParserException {
		private static final long serialVersionUID = 8451427681836492906L;

		public DEFErrorMSGisM11ACC1(int lineNb) {
			super("D022",null,new String[]{Integer.toString(lineNb)});
		}
	}
	/**
	 * D023 DEF file error! Invalid MSG syntax at line [{0}], MSG definition must be at depth 0.
	 */
	public static class DEFErrorMSGatDepth0 extends ParserException {
		private static final long serialVersionUID = -4733085923370903326L;

		public DEFErrorMSGatDepth0(int lineNb) {
			super("D023",null,new String[]{Integer.toString(lineNb)});
		}
	}
	/**
	 * D024 DEF file error! Invalid [{0}] definition at line [{1}]:[{2}], condition name [{3}] is undefined (case sensitive).
	 */
	public static class DEFErrorBadCONDName extends ParserException {
		private static final long serialVersionUID = 3427880317262754994L;

		public DEFErrorBadCONDName(String msgOrSeg,int lineNb, String line, String CONDname) {
			super("D024",null,new String[]{msgOrSeg,Integer.toString(lineNb),line, CONDname});
		}
	}
	/**
	 * D027 DEF file error! Invalid [{0}] definition at line [{1}]:[{2}], current element depth [{3}] cannot be less than condition [{4}] depth scope = [{5}].
	 */
	public static class DEFErrorBadDepthVersusCOND extends ParserException {
		private static final long serialVersionUID = 6183330856904621818L;

		public DEFErrorBadDepthVersusCOND(String msgOrSeg,int lineNb, String line, int locDepth, String CONDname, int CONDdepth) {
			super("D027",null,new String[]{msgOrSeg,Integer.toString(lineNb),line,Integer.toString(locDepth), CONDname, Integer.toString(CONDdepth)});
		}
	}

	/**
	 * D036 DEF file error! Invalid D (Data) syntax at line [{0}]:[{1}], expected valid size specs "[<min>..<max>]" or no extra chars next to "<Description>".
	 */
	public static class DEFErrorInvalidEndOfDataDEF extends ParserException {
		private static final long serialVersionUID = 44492434733233813L;

		public DEFErrorInvalidEndOfDataDEF(int lineNb, String line) {
			super("D036",null,new String[]{Integer.toString(lineNb),line});
		}
	}

	/**
	 * D037 DEF file error! Invalid regex ["{0}"] at line [{1}]! {2}
	 */
	public static class DEFErrorInvalidRegex extends ParserException {
		private static final long serialVersionUID = 5341324922561584159L;

		public DEFErrorInvalidRegex(String regex, int lineNb, String explain) {
			super("D037",null,new String[]{regex, Integer.toString(lineNb),explain});
		}
	}

	/**
	 * D038 DEF file error! Invalid [{0}] definition at line [{1}]:[... {2}], a namespace suffix cannot be associated to NOTAG elements.
	 */
	public static class DEFErrorNOTAGwithNamespace extends ParserException {
		private static final long serialVersionUID = 5400037178765608477L;

		public DEFErrorNOTAGwithNamespace(String msgOrSeg,int lineNb, String restofline) {
			super("D038",null,new String[]{msgOrSeg,Integer.toString(lineNb),restofline});
		}
	}
	
	/**
	 * D039 DEF file error! Invalid SET statement at line [{0}]:[{1}], bad syntax, expected SET BASENAMESPACE "myURI" [NAME.SPCE.SIGN.ATUR], or other documented SETtings 
	 */
	public static class DEFErrorInvalidSetting extends ParserException {
		private static final long serialVersionUID = 555396528359735866L;

		public DEFErrorInvalidSetting(int lineNb, String line) {
			super("D039",null,new String[]{Integer.toString(lineNb),line});
		}
	}

	/**
	 * D040 DEF file error! Invalid SET RELEASECHARACTER statement at line [{0}]: character specification [{1}] shall be one of '''c''' where c is a single printable char, or a Unicode char like '''\\u002E''', else '''\\\\''' for \\
	 */
	public static class DEFErrorInvalidReleaseChar extends ParserException {
		private static final long serialVersionUID = -2983277208592928254L;

		public DEFErrorInvalidReleaseChar(int lineNb, String charSpec) {
			super("D040",null,new String[]{Integer.toString(lineNb),charSpec});
		}
	}


	/**
	 * P001 Parser internal error! Unknown DEF element, class: [{0}], impact [{1}].
	 */
	public static class ParserInternalError extends ParserException {
		private static final long serialVersionUID = -392037129456477734L;

		public ParserInternalError(String className, Impact impact) {
			super("P001",null,new String[]{className, impact.toString()});
			this._impact = impact;
		}
	}

	/**
	 * Most of the following exceptions share a fixed set or arguments describing the source of the
	 * problem with the reference to the impacted item.
	 * 
	 * where:
	 * {0}		the item affected by the error
	 * {1}		the description of this item found in the DEF file
	 * {2}		the data at stake, generally described by the item in argument above.
	 * 			It is the relevant portion of the broker message, as needed. Avoid NULL and prefer "--not set--"
	 * {3}		the line number in the broker message causing the exception
	 * {4}		character offset within the given line, or absolute offset if line number is zero.
	 * {5}		the impact level: Warning or Fatal impact only as <code>Impact.FATAL</code> 
	 * 					or <code>Impact.WARNING</code>
	 * Examples:
	 * 	P002 Parser has nothing to match in input! affecting entire message, at L:{3} O:{4}, impact [{5}].
	 *  P013 Parsing error about item <{0}>({1}), context [{2}] at L:{3} O:{4}, impact [{5}].
	 */
	public static class ParserHasNothingToMatch extends ParserException {
		private static final long serialVersionUID = -7153580165367260747L;

		public ParserHasNothingToMatch(String tag, String descr, String ctxt, int lineNb, int offset,Impact impact) {
			super("P002",null,new String[]{tag,descr,ctxt,Integer.toString(lineNb),Integer.toString(offset),impact.toString()});
			this._impact = impact;
		}
		public void adjustLineOffset(int adjustment) {
			this.arguments[3] = Integer.toString(Integer.parseInt((String)this.arguments[3])+adjustment);
		}

	}

	/**
	 * 	P003 The parser failed to match any of the constituent elements of the message itself! context [{2}] at L:{3} O:{4}, impact [{5}].
	 */
	public static class ParserNoMsgEletsFound extends ParserException {
		private static final long serialVersionUID = -371632089509519671L;

		public ParserNoMsgEletsFound(String tag, String descr, String ctxt, int lineNb, int offset,Impact impact) {
			super("P003",null,new String[]{tag,descr,ctxt,Integer.toString(lineNb),Integer.toString(offset),impact.toString()});
			this._impact = impact;
		}
		public void adjustLineOffset(int adjustment) {
			this.arguments[3] = Integer.toString(Integer.parseInt((String)this.arguments[3])+adjustment);
		}
	}

	/**
	 * 	P004 Parser is unable to [{0}]! (validating this [{2}] against "{1}").
	 */
	public static class ParserUnableTo extends ParserException {
		private static final long serialVersionUID = 1636493841674043553L;

		public ParserUnableTo(String functor, String ptrn, String data) {
			super("P004",null,new String[]{functor,ptrn,data});
		}
	}
	/**
	 * 	P005 Data value invalid versus [{0}]! (validating this [{2}] against "{1}").
	 */
	public static class ParserInvalidValue extends ParserException {
		private static final long serialVersionUID = 4971110656015499235L;

		public ParserInvalidValue(String functor, String ptrn, String data) {
			super("P005",null,new String[]{functor,ptrn,data});
		}
	}

	/**
	 * 	P006 Missing mandatory data element [{0}]! (validating this [{2}] against "{1}").
	 */
	public static class ParserMissingMandatoryElt extends ParserException {
		private static final long serialVersionUID = 5122789725887564131L;

		public ParserMissingMandatoryElt(String elt, String ptrn, String data) {
			super("P006",null,new String[]{elt,ptrn,data});
		}
	}
	/**
	 * 	P007 Missing mandatory segment [{0}]! (identifying this [{2}] in "{1}").
	 */
	public static class ParserMissingMandatorySeg extends ParserException {
		private static final long serialVersionUID = 1176042241320368177L;

		public ParserMissingMandatorySeg(String elt, String ptrn, String data) {
			super("P007",null,new String[]{elt,ptrn,data});
		}
	}
	/**
	 * 	P008 Missing mandatory group [{0}]! (failed to identify [{2}] in "{1}" -or- unable to match constituent sub-elements).
	 */
	public static class ParserMissingMandatoryGroup extends ParserException {
		private static final long serialVersionUID = -9194424440964703812L;

		public ParserMissingMandatoryGroup(String elt, String ptrn, String data) {
			super("P008",null,new String[]{elt,ptrn,data});
		}
	}

	/**
	 * 	P009 End of data while still having this mandatory element [{0}] to match.
	 */
	public static class ParserPrematureEndOfData extends ParserException {
		private static final long serialVersionUID = -4662352144681935412L;

		public ParserPrematureEndOfData(String elt) {
			super("P009",null,new String[]{elt});
		}
	}

	/**
	 * P010 Unexpected Data found! No matching group, segment, or element definition for [{0}].
	 */
	public static class ParserNoMatchingDef extends ParserException {
		private static final long serialVersionUID = -1225057771252453957L;

		public ParserNoMatchingDef(String what) {
			super("P010",null,new String[]{what});
		}
	}
	/**
	 * 	P011 Failed identification of the whole message! (identifying this [{1}] in "{0}").
	 */
	public static class ParserMessageIdentificationFailure extends ParserException {
		private static final long serialVersionUID = 9004872325344600441L;

		public ParserMessageIdentificationFailure(String ptrn, String data) {
			super("P011",null,new String[]{ptrn,data});
		}
	}
	/**
	 * P012 Data [{0}] left in input after end of matching the message definition.
	 */
	public static class ParserInputNotExhausted extends ParserException {
		private static final long serialVersionUID = -7956330585768770701L;

		public ParserInputNotExhausted(String what) {
			super("P012",null,new String[]{what});
		}
	}

	/**
	 *  P013 Parsing error about element <{0}>({1}), context [{2}] at L:{3} O:{4}, impact [{5}].
	 */
	public static class ParserDataError extends ParserException {
		private static final long serialVersionUID = 7411703881828194928L;

		public ParserDataError(String tag, String descr, Throwable cause, String ctxt, int lineNb, int offset,Impact impact) {
			super("P013",cause,new String[]{tag,descr,ctxt,Integer.toString(lineNb),Integer.toString(offset),impact.toString()});
			this._impact =impact;
		}
		public void adjustLineOffset(int adjustment) {
			this.arguments[3] = Integer.toString(Integer.parseInt((String)this.arguments[3])+adjustment);
		}
	}

	/**
	 * P014 Parsing Exceeded [{0}] FATAL exceptions threshold.
	 */
	public static class ParserExceededFATALExceptions extends ParserException {
		private static final long serialVersionUID = -1801001575234819280L;

		public ParserExceededFATALExceptions(int threshold, Throwable cause) {
			super("P014",cause,new String[]{Integer.toString(threshold)});
		}
	}
	/**
	 * P015 Parsing Exceeded [{0}] overall exceptions threshold.
	 */
	public static class ParserExceededAllExceptions extends ParserException {
		private static final long serialVersionUID = -6162068481774738527L;

		public ParserExceededAllExceptions(int threshold, Throwable cause) {
			super("P015",cause,new String[]{Integer.toString(threshold)});
		}
	}

	/**
	 * P016 Named Condition Tokens were collected at a smaller depth than the depth scope of COND [{0}].
	 */
	public static class ParserErrorDepthBelowCOND extends ParserException {
		private static final long serialVersionUID = 980843754175274551L;

		public ParserErrorDepthBelowCOND(String name) {
			super("P016",null,new String[]{name});
		}
	}
	/**
	 * 	P017 Named CONDition [{0}] failed! (validating this [{2}] against "{1}").
	 */
	public static class ParserFailedCOND extends ParserException {
		private static final long serialVersionUID = -5283620550489056622L;

		public ParserFailedCOND(String name, String expected, String actual) {
			super("P017",null,new String[]{name,expected,actual});
		}
	}

	/**
	 * P018 Found [{0}] element occurences below required minimum [{1}].
	 */
	public static class ParserErrorOccBelowMin extends ParserException {
		private static final long serialVersionUID = 3100280917820566552L;

		public ParserErrorOccBelowMin(int found, int expected) {
			super("P018",null,new String[]{Integer.toString(found),Integer.toString(expected)});
		}
	}
	/**
	 * P019 Found [{0}] element occurences over expected maximum [{1}].
	 */
	public static class ParserErrorOccOverMax extends ParserException {
		private static final long serialVersionUID = -895274887627760468L;

		public ParserErrorOccOverMax(int found, int expected) {
			super("P019",null,new String[]{Integer.toString(found),Integer.toString(expected)});
		}
	}

	/**
	 * 	P020 Failed [{0}] times to match mandatory elements, now backtracking! (failed last to match [{2}] against "{1}").
	 */
	public static class ParserNowBacktracking extends ParserException {
		private static final long serialVersionUID = -5625124887427627887L;

		public ParserNowBacktracking(int failures, String expected, String actual) {
			super("P020",null,new String[]{Integer.toString(failures),expected,actual});
		}
	}
	/**
	 * 	P022 Data element value [{1}] is under minimum size of [{0}].
	 */
	public static class ParserDataUnderSized extends ParserException {
		private static final long serialVersionUID = -4255321666691002244L;

		public ParserDataUnderSized(int size, String data) {
			super("P022",null,new String[]{Integer.toString(size),data});
		}
	}
	/**
	 * 	P023 Data element value [{1}] is over maximum size of [{0}].
	 */
	public static class ParserDataOverSized extends ParserException {
		private static final long serialVersionUID = -13134035512950615L;

		public ParserDataOverSized(int size, String data) {
			super("P023",null,new String[]{Integer.toString(size),data});
		}
	}

	/**
	 * P024 Unexpected Data found! No definition matching [{0}], trying to skip it.
	 */
	public static class ParserTryingToSkip extends ParserException {
		private static final long serialVersionUID = -1491442716850740429L;

		public ParserTryingToSkip(String what) {
			super("P024",null,new String[]{what});
		}
	}

	/**
	 * P025 Parser internal error! Missing top-level MSG definition element, impact [{0}].
	 */
	public static class ParserInternalErrorEmptyDEF extends ParserException {
		private static final long serialVersionUID = 7725298643146733964L;

		public ParserInternalErrorEmptyDEF(Impact impact) {
			super("P025",null,new String[]{impact.toString()});
			this._impact = impact;
		}
	}
	
	/**
	 *  P026 Parsing error about condition [{0}]({1}), context [{2}] at L:{3} O:{4}, impact [{5}].
	 */
	public static class ParserCONDError extends ParserException {
		private static final long serialVersionUID = -5803843488659530798L;

		public ParserCONDError(String name, String descr, Throwable cause, String ctxt, int lineNb, int offset,Impact impact) {
			super("P026",cause,new String[]{name,descr,ctxt,Integer.toString(lineNb),Integer.toString(offset),impact.toString()});
			this._impact = impact;
		}
		public void adjustLineOffset(int adjustment) {
			this.arguments[3] = Integer.toString(Integer.parseInt((String)this.arguments[3])+adjustment);
		}
	}

}
