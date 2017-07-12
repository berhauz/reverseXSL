package com.reverseXSL;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import com.reverseXSL.message.Data;
import com.reverseXSL.message.Header;
import com.reverseXSL.transform.MappingSelection;
import com.reverseXSL.transform.Transformer;
import com.reverseXSL.transform.TransformerFactory;

 
/**
 * <h2>Transformation command-line tool.</h2> 
 * <p><i>You can customize source code (available in the jar) and make your own 'TransformPlus' version.</i></p>
 * <p>This tool is a command-line wrapping of the original Transformer API.</p>
 * <p><b><u>Simplest use, with a single jar containing software and transformation meta-data:</u></b><br>
 * <i>(meta-data comprises Parsing DEFinitions, XSL templates, and the mapping selection table)</i></p>
 * <p><code>&nbsp;&nbsp;java  -jar ReverseXSL.jar  myInputDataFile</code></p>
 * <p><b><u>variant</u> (assuming ReverseXSL.jar on the CLASSPATH):</b>
 * <br><i>(picking-up resources from directories and/or multiple jar's using the CLASSPATH)</i><br></p>
 * <p><code>&nbsp;&nbsp;java  com.reverseXSL.Transform  myInputDataFile</code></p>
 * <p>both these command lines followed by <code>... >MyOutputFile</code><br>
 * do capture the transformed result to a file; indeed, output is written on stdout
 * whereas execution logs are written on stderr<br></p>
 * <p><b><u>Advanced use</u> (assuming ReverseXSL.jar on the CLASSPATH):</b></p>
 * <p><code>&nbsp;&nbsp;java com.reverseXSL.Transform myDEFFile myXSLFile myInputDataFile</code></p>
 * <p>where the parser DEF file and the XSL template file instruct to, respectively, parse
 * the input with the associated Parsing DEFinition, and then transform the resulting XML
 * with the specified XSL template. If any of those two resources are not found relative to
 * the current working directory, the corresponding transformation step is omitted.</p>
 * <p><i>Tip: simply use placeholder DEF or XSL arguments like 'NoDEF' and 'NoXSL' to skip the corresponding transformation step.</i></p>
 * <p>Note that optional command-line arguments are noted below within '[' ']', possibly nested.</p>
 * <p><code>&nbsp;&nbsp;java com.reverseXSL.Transform myDefinitionFile myXSLFile myInputDataFile [&lt;InputCleansing> &lt;MaxFatalExceptions> 
 * &lt;MaxExceptions> [&lt;true|false> [&lt;indent>] ] ]</code></p>
 * <ul>where<li><code>&lt;InputCleansing></code> is one of <code>NONE</code>, <code>ToCRLF</code>, <code>ToLF</code>, <code>ToUPPER</code>, 
 * <code>FullyTrimmed</code>, or <code>UnfoldPSCRMRemarks</code>. You may 
 * combine multiple cleansing options 
 * with a '+' as in <code>ToCRLF+fullytrimmed+TOUPPER</code>. Tokens are not case sensitive. FullyTrimmed 
 * removes leading & trailing spaces/tabs, as well as empty lines and control characters 
 * (more options via the java API). UnfoldPSCRMRemarks removes the arbitrary '&lt;CR&gt;&lt;LF&gt;.RN/' 
 * remarks-extension-line-breaks that can jeopardize the correct parsing of Remarks elements in IATA 
 * PSCRM messages (a legacy feature inherited from TELEX maximum line length at 69 chars).</li>
 * <li>MaxExceptions & MaxFatalExceptions are integers that define the Parser exception recovery thresholds.</li>
 * <li>The true/false argument tells to {@link com.reverseXSL.parser.Parser#removeNonRepeatableNilOptionalElements(boolean) removeNonRepeatableNilOptionalElements}.</li>
 * <li>The indent string instructs to produce printable Transforms with the given indent pattern. You may for instance try with <code>"  |"</code>.</li></ul>
 * <p>or yet:</p>
 * <p><code>&nbsp;&nbsp;java com.reverseXSL.Transform AUTO SELECT myInputDataFile [&lt;InputCleansing> &lt;MaxFatalExceptions> 
 * &lt;MaxExceptions> [&lt;true|false> [&lt;indent>] ] ]</code></p>
 * <p>In which case a <code>mapping_selection_table.txt</code> (fixed name) file is searched up in the directory 
 * hierarchy such as to dynamically resolve which parsing DEF and/or XSL template to apply to the given input data.
 * </p>
 * <p>/!\ Note that the output is written on stdout whereas execution messages and logs are written on stderr;
 * the transformed output is thus captured using command-line output redirection. For instance:</p>
 * <p><code>&nbsp;&nbsp;java  -jar ReverseXSL.jar  myInputDataFile >myOutputFile</code></p>
 * </p>
 * <p>Please refer to the Software Manual for an overview and tutorial samples.</p>
 */
public class Transform {

	/**
	 * Entry to the {@link Transform command-line tool} for 'reverse-XSL' data transformations.
 	 * 
	 * @param args command line arguments as described in {@link Transform}
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		if ((args.length<1)){
			System.err.println("\nReverseXSL Transformer\n\nSimplest usage is with a single jar containing software and transformation meta-data\n" +
					"   (meta-data comprises Parsing DEFinitions, XSL templates, and the mapping selection table):\n\n" + 
					" java  -jar ReverseXSLTransformations.jar  myInputDataFile\n");
			System.err.println("variant, picking-up resources from directories and/or jar's using the CLASSPATH:\n\n" + 
					" java  com.reverseXSL.Transform  myInputDataFile\n");
			System.err.println("both these command lines followed by ... >MyOutputFile\n" +
					"do capture the transformed result to a file; indeed, output is written on stdout\n" + 
					"whereas execution logs are written on stderr\n");
			System.err.println("Execute:  java com.reverseXSL.Transform HELP\n" +
					"   or     java -jar reverseXSL.jar HELP\n" +
					"      for advanced command-line options\n" + 
					"\n");
			return;
		}

		if ((args.length>1)||(args.length==1 && args[0].equalsIgnoreCase("help"))) {
			advanced(args);
			return;
		}
		
		FileInputStream fis = new FileInputStream(args[0]);
		
		com.reverseXSL.transform.Transformer t = com.reverseXSL.transform.TransformerFactory.newInstance().newTransformer();
		StringBuffer out = new StringBuffer();
		//formally, t.transform(...) shall be used, but here, this is a 
		//test application with intentionally printable outputs!
		t.printableTransform(fis, out);
		
		System.out.print(out.toString());
		//use error output for logs to prevent mixing outputs on the console
		System.err.println("---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- \n");
		System.err.println("Processing input file : " + args[0]);
		System.err.println(t.getLog().toString());

	}

	/**
	 * Implementation of the advanced command-mode. 
	 * @param args
	 * @throws Exception
	 */
	static void advanced(String[] args) throws Exception {
		int MAXTOTAL = 10;
		int MAXFATAL = 0;
		File mstFile = null;
		int conversionFlags = Data._NONE;
		FileReader myDEFReader, myXSLReader;
		FileReader mstReader;
		FileInputStream inStream = null;
		ByteArrayOutputStream outStream = null;
		boolean printableMode = false;
		boolean removeNRNOElts = false; //remove non repeatable Nil optional elements
		
		if ((args.length!=3)&&(args.length!=6)&&(args.length!=7)&&(args.length!=8)){
			System.err.println("\nAdvanced use : \n\n" + 
					" java com.reverseXSL.Transform myDEFFile myXSLFile myInputDataFile\n\n"
					+ "    where the parser DEF file and the XSL template file instruct to, respectively, parse\n"
					+ "    the input with the associated Parsing DEFinition, and then transform the resulting XML\n"
					+ "    with the specified XSL template. If any of those two resources are not found relative to\n"
					+ "    the current working directory, the corresponding transformation step is omitted.\n" 
					+ "    Example: java com.reverseXSL.Transform FFM15.def NOXSLT myIATA_FFM_file.txt\n" );
			System.err.println("with all options within '[' ']' : \n\n" + 
					" java Transform myDefinitionFile myXSLFile myInputDataFile [<InputCleansing> <MaxFatalExceptions> <MaxExceptions> [<true|false> [<indent>] ] ]\n\n"
					+ "    where the <InputCleansing> is one or more of NONE, ToCRLF, ToLF, ToUPPER, FullyTrimmed, UnfoldPSCRMRemarks.\n"
					+ "      You may combine multiple cleansing options with a '+' as in ToCRLF+fullytrimmed+TOUPPER. Tokens are not case sensitive.\n"
					+ "      FullyTrimmed removes leading & trailing spaces/tabs, as well as empty lines and control characters (more options via the API).\n"
					+ "      UnfoldPSCRMRemarks removes the arbitrary '<CR><LF>.RN/' remarks-extension-line-breaks that can jeopardize the correct\n"
					+ "      parsing of Remarks elements in IATA PSCRM messages (a legacy feature inherited from TELEX max line at 69 chars).\n"
					+ "    <MaxExceptions> & <MaxFatalExceptions> are integers that define the Parser exception recovery thresholds.\n"
					+ "    The true or false argument tells to removeNonRepeatableNilOptionalElements (cfr Paser documentation).\n"
					+ "    The <indent> string instructs to produce printable Transforms with the given indent pattern (try \"  |\" and see...)\n");
			System.err.println("or yet: \n\n" + 
					" java Transform AUTO SELECT myInputDataFile [<InputCleansing> <MaxFatalExceptions> <MaxExceptions> [<true|false> [<indent>] ] ]\n\n"
					+ "    In which case a 'mapping_selection_table.txt' (fixed name) file is searched up in the directory hierarchy\n"
					+ "    such as to dynamically resolve which parsing DEF and/or XSL template to apply to the given input data.\n");
			System.err.println("/!\\ Note that the output is written on stdout whereas execution messages and logs are written on stderr;\n"
					+ "    The transformed output is thus captured using command-line output redirection like ... >myOutput.data\n");
			return;
		}
		
		//we need to set-up a factory, in order to instantiate in turn one or more Transformer threads
		TransformerFactory tf = TransformerFactory.newInstance();
		
		if (args.length>=6) {
			// add a '+' delimiter at the end
			String opts = new String(args[3] + "+");
			String opt = "";
			for (int i = opts.indexOf('+'); i > 0; i = opts.indexOf('+')) {
				opt = opts.substring(0, i);
				opts = opts.substring(i + 1);
				int val = Data.tokenValue(opt);
				if (val >= 0)
					conversionFlags += val;
				else
					System.err.println("requested input data conversion ["
							+ opt + "] is unknown! skipping...");
			}
			tf.setInputDataConversions(conversionFlags);
			MAXFATAL=Integer.parseInt(args[4]);
			MAXTOTAL=Integer.parseInt(args[5]);
			tf.setParserExceptionThresholds(MAXFATAL, MAXTOTAL);
		}
		if (args.length>=7) {
			removeNRNOElts= Boolean.valueOf(args[6]).booleanValue();
			tf.setParserRemoveNonRepeatableNilOptionalElements(removeNRNOElts);
		}
		if (args.length>=8) {
			tf.setPrintableXmlIndent(args[7]);
			printableMode = true;
		}
				
		Transformer t;
		
		if (args[0].equalsIgnoreCase("AUTO") && args[1].equalsIgnoreCase("SELECT")) {
			//handle the AUTO SELECT case
			mstFile = findUpInDirs(MappingSelection.MAPPING_SELECTION_TABLE);
			if (mstFile!=null) System.err.println("Using Mapping Selection Table: "+mstFile.getAbsolutePath());
			else throw new IOException("File ["+MappingSelection.MAPPING_SELECTION_TABLE+"] not found in default directory ["+System.getProperty("user.dir", "???")+"], neither in any parent directory");
			//java issue! .getParent() returns null instead of the path elements
			//so we have to extract the parent dir path the hard way!...but we have the tool
			String parentPath = Header.extractReference(mstFile.getAbsolutePath(),"^(.:)?(.*[/\\\\]).+?$");
			mstReader = new FileReader(mstFile);
			tf.setExternalMappingSelectionTable(mstReader, parentPath);
			//instantiate a Transformer
			t = tf.newTransformer();
		} else {
			//instantiate a transformer with the specified DEF and XSLT
			if (new File(args[0]).canRead()) myDEFReader = new FileReader(args[0]);
			else myDEFReader = null;
			if (new File(args[1]).canRead()) myXSLReader = new FileReader(args[1]);
			else myXSLReader = null;
			//instantiate a Transformer
			t = tf.newTransformer(myDEFReader, myXSLReader);
		}
		
		try {
			//read input data and transform it
			inStream = new FileInputStream(args[2]);
		} catch (Exception e) {
			throw new Exception("Cannot read the input data file ["+args[2]+"], cause: "+e.getMessage(),e);
		}
		if (printableMode) {
			//PRINTABLE CASE
			StringBuffer out = new StringBuffer();
			// Transform NOW!
			t.printableTransform(inStream, out);
			System.err.println(t.getLog());
			System.out.println(out.toString());
			
		} else {
			//EXACT CASE
			outStream = new ByteArrayOutputStream();
			// Transform NOW!
			t.transform(inStream, outStream);
			outStream.close();
			System.err.println(t.getLog());
			System.out.println(outStream.toString());
		}
	}
	
	/**
	 * Facility to locate a file elsewhere up in a directory hierarchy.
	 * 
	 * @param fileName
	 * @return a File object or null
	 */
	static File findUpInDirs(String fileName) {
		File f = new File(fileName);
		String workPath;
		while (! f.canRead()) {
			workPath = Header.extractReference(f.getAbsolutePath(), 
			"^(.:)?(.*[/\\\\]).+?[/\\\\](.+?)$");
			if (workPath==null || workPath.length()<1) return null;
			f = new File(workPath);
		}
		return f;
	}

}
