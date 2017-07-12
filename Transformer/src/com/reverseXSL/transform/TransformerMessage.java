package com.reverseXSL.transform;

import com.reverseXSL.exception.FormattedMessage;
import com.reverseXSL.parser.ParserException;
import com.reverseXSL.types.DataFormat;

/**
 * Multilingual log messages used by the Transformer and TransformerFactory.
 * 
 * @author bernardH
 */
public class TransformerMessage extends FormattedMessage {
	private static final long serialVersionUID = -3049218562703106005L;
	
	protected TransformerMessage(String messageId, Object[] args) {
		super(messageId, args);
	}
	
	/**
	 * M_bytes_in = Bytes in: {0}. Format: {1}
	 */
	public static class LogBytesIn extends TransformerMessage {
		private static final long serialVersionUID = -7851518506443472584L;
		
		public LogBytesIn(int nb,DataFormat df) {
			super("M_bytes_in",new String[]{Integer.toString(nb),df.toString()});
		}
	}
	
	/**
	 * M_bytes_out = Bytes out: {0}. Format: {1}
	 */
	public static class LogBytesOut extends TransformerMessage {
		private static final long serialVersionUID = -2615903475284033047L;

		public LogBytesOut(int nb,DataFormat df) {
			super("M_bytes_out",new String[]{Integer.toString(nb),df.toString()});
		}
	}
	
	/**
	 * M_selected_DEF_and_XSL = Selected entry [{2}]: Parsing DEF [{0}] and XSL [{1}].
	 */
	public static class LogSelected_DEF_and_XSL extends TransformerMessage {
		private static final long serialVersionUID = 5190540905515051468L;
		
		public LogSelected_DEF_and_XSL(String def, String xsl, String name) {
			super("M_selected_DEF_and_XSL",new String[]{def,xsl, name});
		}
	}
	
	
	/**
	 * M_selected_Conversions = Input Data normalizing/depollution with [{0}].
	 */
	public static class LogSelected_Conversions extends TransformerMessage {
		private static final long serialVersionUID = -1308070452063479935L;

		public LogSelected_Conversions(String conversions) {
			super("M_selected_Conversions",new String[]{conversions});
		}
	}
	
	/**
	 * M_got_parsing-errors = Got [{0}] Parsing Errors!!!!! (still below thresholds: MaxFatal[{1}], MaxTotal[{2}])
	 */
	public static class GotParsingErrors extends TransformerMessage {
		private static final long serialVersionUID = -2576111273485615664L;
		
		public GotParsingErrors(int errCnt, int fatalCnt, int totalCnt) {
			super("M_got_parsing-errors",new String[]{Integer.toString(errCnt),Integer.toString(fatalCnt),Integer.toString(totalCnt)});
		}
	}
	
	/**
	 * M_recorded_exceptions = RECORDED EXCEPTIONS:
	 */
	public static class RecordedExceptions extends TransformerMessage {
		private static final long serialVersionUID = 898608428869963825L;
		
		public RecordedExceptions() {
			super("M_recorded_exceptions",new String[0]);
		}
	}
	
	/**
	 * M_caused_by = ...caused by {0}
	 */
	public static class CausedBy extends TransformerMessage {
		private static final long serialVersionUID = 1967809153418881708L;
		
		public CausedBy(String causeDescr) {
			super("M_caused_by",new String[] {causeDescr});
		}
	}

	/**
	 * M_exception_report = [{0}] {1}
	 */
	public static class ExceptionReport extends TransformerMessage {
		private static final long serialVersionUID = 2391281301606649911L;

		public ExceptionReport(int rank,ParserException pe) {
			super("M_exception_report",new String[] {Integer.toString(rank),pe.getMessage()});
		}
	}

	
	/**
	 * M_transf_continues = Transformation continues!!!
	 */
	public static class TransformationContinues extends TransformerMessage {
		private static final long serialVersionUID = -2822658414510026230L;

		public TransformationContinues() {
			super("M_transf_continues",new String[0]);
		}
	}
	
	/**
	 * M_parsing_OK = Parsing OK (no errors)
	 */
	public static class ParsingOK extends TransformerMessage {
		private static final long serialVersionUID = 3643788660745896671L;

		public ParsingOK() {
			super("M_parsing_OK",new String[0]);
		}
	}
	
	/**
	 * M_no_parsing = No parsing requested
	 */
	public static class NoParsing extends TransformerMessage {
		private static final long serialVersionUID = -7202349367734858102L;

		public NoParsing() {
			super("M_no_parsing",new String[0]);
		}
	}

	
	/**
	 * M_XSLT_OK = XSL Transformation OK (no errors)
	 */
	public static class XsltOK extends TransformerMessage {
		private static final long serialVersionUID = 6955526763836114416L;

		public XsltOK() {
			super("M_XSLT_OK",new String[0]);
		}
	}

	
	/**
	 * M_no_XSLT = No XSL Transformation requested
	 */
	public static class NoXslt extends TransformerMessage {
		private static final long serialVersionUID = 2478523208163535747L;

		public NoXslt() {
			super("M_no_XSLT",new String[0]);
		}
	}

	
	/**
	 * M_ignore_XSLT = (XSL Transformation skipped by the Free Software version)
	 */
	public static class NoXslt_FreeSW extends TransformerMessage {
		private static final long serialVersionUID = -5376332146580776841L;

		public NoXslt_FreeSW() {
			super("M_ignore_XSLT",new String[0]);
		}
	}

	
}
