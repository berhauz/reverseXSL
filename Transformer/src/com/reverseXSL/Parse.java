package com.reverseXSL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
import java.util.Properties;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
*/
import com.reverseXSL.message.*;
import com.reverseXSL.parser.Definition;
import com.reverseXSL.parser.Parser;

/**
 * <p>The stand-alone command-line Parser is quite useful in development-cycle automation. 
 * This is a wrapper class over the 
 * reverseXSL Parser API.</p><p>Usage (assuming ReverseXSL.jar on the CLASSPATH):<br>
 * <code>java com.reverseXSL.Parse myDefinitionFile</code>
 * <br>
 * &nbsp;&nbsp;&nbsp;Dumps a sample XML message matching the definition<br>
 * or :<br>
 * <code>java com.reverseXSL.Parse myDefinitionFile myInputMessageFile</code>
 * <br>
 * &nbsp;&nbsp;&nbsp;Executes the Parser from the command line and outputs a dump of the parser state 
 * followed by the generated XML.<br>
 * or :<br>
 * <code>java com.reverseXSL.Parse myDefinitionFile myTOKEN</code>
 * <br>
 * Applies the definition onto the first #myTOKEN=<filename>; found inside the DEF file (this
 * mode is used with Stylus Studio customs tools and tokens like ONE, TWO, THREE)<br>
 * or :<br>
 * <code>java com.reverseXSL.Parse myDefinitionFile myInputMessageFile &lt;MaxFatalExceptions&gt;
 * &lt;MaxExceptions&gt; &lt;true|false&gt;</code><br>
 * where the last argument tells to removeNonRepeatableNilOptionalElements
 * </p><p>Note that the XML message is written on stdout
 * whereas the parser state dump is written on stderr.
 * It is then easy to capture just the XML output by calling:<br>
 * <code>java com.reverseXSL.Parse myDefinitionFile myInputMessageFile &gt;&gt;out.xml</code>
 * </p> 
 */
public class Parse {

	/**
	 * Command-line tool entry point.
	 * 
	 * @param args see {@link Parse} javadoc
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		LineNumberReader lnrDef, lnrMsg = null;
		int MAXFATAL = 0;
		int MAXTOTAL = 0;
		String fileName="";
		String fileContents = null;
		boolean removeNRNOElts = false; //remove non repeatable Nil optional elements
		Definition def;
		Parser  parser;

		if ((args.length!=1)&&(args.length!=2)&&(args.length!=4)&&(args.length!=5)){
			System.err.println("\nUsage : \n" + 
					"java com.reverseXSL.Parse myDefinitionFile\n");
			System.err.println("or : \n" + 
					"java com.reverseXSL.Parse myDefinitionFile myInputMessageFile|myToken\n");
			System.err.println("or : \n" + 
					"java com.reverseXSL.Parse myDefinitionFile myInputMessageFile|myToken <MaxFatalExceptions> <MaxExceptions> <true|false>\n"
					+ "    where the last argument tells whether to removeNonRepeatableNilOptionalElements\n");
			System.err.println("\nNote that the XML message is written on stdout\n" + 
					"whereas the parser state dump is written on stderr\n");
			return;
		}
				
		if (args.length>=4) {
			MAXFATAL=Integer.parseInt(args[2]);
			MAXTOTAL=Integer.parseInt(args[3]);
		}
		if (args.length==5) {
			removeNRNOElts= Boolean.valueOf(args[4]).booleanValue();
		}
		lnrDef = new LineNumberReader(new FileReader(args[0]));
		def = new Definition();
		def.loadDefinition(lnrDef);
		
		//it may look quite stupid to test for command line redirection given that the command line interpreters
		//will not pass any redirection as an argument to the called command; however when executed as
		//an "external tool" within StylusStudio, the redirection element from the command line
		//is passed as literal argument to the program!!! hence we cater for this special case.
		if (args.length==1 || (args.length==2 && args[1].startsWith(">") )) {
			if (args.length==1)
				System.out.print(def.getXMLSample(true, true).toString());
			else { //case of redirected output within a Stylus Studio context
				FileWriter fw = new FileWriter(args[1].substring(1));
				fw.write(def.getXMLSample(true, true).toString());
				fw.close();
			}
			return;
		}

		//check Token or file name
		File f = new File(args[1]);

		if (f.canRead())
			if (args[1].toLowerCase().endsWith(".txt")) lnrMsg =  new LineNumberReader(new FileReader(f));
			else fileContents = loadFrom(f);
		else {
			//attempt extracting the file name from a token value inside the DEF file
			CharBuffer cb = CharBuffer.allocate(2000);
			FileReader fr = new FileReader(args[0]);
			char[] ca = new char[2000];
			int l = fr.read(ca);
			cb.put(ca, 0, l);
			cb.flip();
			fileName = Header.extractReference(cb, "#"+args[1]+"=(.*?);");
			if (fileName!= null && fileName.length()>0)
				if (fileName.toLowerCase().endsWith(".txt")) lnrMsg =  new LineNumberReader(new FileReader(fileName));
				else fileContents = loadFrom(new File(fileName));
			else throw new IOException("Argument ["+args[1]+"] not found as file, nor as token in DEF file "+ args[0]);
		}
		
		System.err.println("\nLoading definition from : " + args[0]);
		if (args.length>=2)
			System.err.println("and then parsing on : " + args[1]+(fileName.length()>0?"="+fileName:"")+"\n");
		else
			System.err.println("and generating a sample\n");

			
		//create a parser
		parser = new Parser(def,MAXFATAL, MAXTOTAL);
		parser.removeNonRepeatableNilOptionalElements(removeNRNOElts);
		//parse
		if (fileContents!=null)
		parser.parse( new SimpleDateFormat("yyMMdd_HHmmss").format(new Date()), fileContents, 1);
		else parser.parse( new SimpleDateFormat("yyMMdd_HHmmss").format(new Date()), lnrMsg, 1);
		
		//dump parser state
		System.err.println(parser.toString());
		
		//dump XML
		//indentation may not really work as desired! the JRE version and embedded JAXP library version affects the outcome
		System.out.print(parser.getXML(true, true).toString());

	}

	private static String loadFrom(File f) throws Exception {
		FileInputStream inS = new FileInputStream(f) ;
		StringBuffer sb = new StringBuffer();
		byte[] ba = new byte[inS.available()];
		int nbin;
		Charset defaultCharset = Charset.forName("UTF-8");
		do {
			nbin = inS.read(ba);
			if (nbin >0) sb.append(defaultCharset.decode(ByteBuffer.wrap(ba,0,nbin)).toString());
		} while (nbin >0);
		return sb.toString();
	}

}
