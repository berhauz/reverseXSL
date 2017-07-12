package com.reverseXSL.transform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ListIterator;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.reverseXSL.message.Data;
import com.reverseXSL.parser.Definition;
import com.reverseXSL.parser.Parser;
import com.reverseXSL.transform.TransformerException;
import com.reverseXSL.parser.ParserException;
import com.reverseXSL.parser.Parser.ExceptionListIterator;
import com.reverseXSL.transform.TransformerFactory;
import com.reverseXSL.transform.MappingSelection.MappingEntry;
import com.reverseXSL.types.DataFormat;
import com.reverseXSL.util.Formatters;

 
/**
 * Executes message transformations, which may comprise a Parsing step and an
 * XSLT step (or only one of, or none at all = pass-through). The Parsing step takes a non-XML file and generates 
 * an XML document according to a Parser DEFinition file. The XSLT step transforms the generated 
 * XML document into another XML document, else any kind of text-based document format.
 * <p>Transformation steps are selected and executed as defined 
 * in the Mapping Selection Table.</p>
 * <p>IMPORTANT NOTE: the free software version ignores the XSLT step. Only the Parsing step, when specified,
 * is performed. In this context, any post-parsing XSL transformation shall be invoked via the java API for XML 
 * processing (JAXP)</p>
 *  
 * @see com.reverseXSL.transform.TransformerFactory factory methods on the means to set the source of meta data, notably the Mapping Selection Table.
 * @author bernardH
 */
public class Transformer {

	private static final int MSG_COUNT_MODULO = 1000000;

	private static final int MSG_SELECTION_RANGE = 1500; //tells how much bytes to look-up for a mapping selection pattern
							//note that just below 1000 bytes are required to hit the SMI in a IATA message prefixed by a
							//fully loaded type-B header

	//argument message id chunks, working variables
	String msg_id = null;
	String msg_id_base = null;
	// the final message ID as issued during the last transform
	String msgID = null;
	
	// working variables
	DecimalFormat msg_id_nb_format = null;
	Date msg_date = null;
	StringBuffer log;
	MappingEntry me = null;
	String entryName;
	Data data = null;
	DataFormat outputFormat = DataFormat.ANY;
	StringBuffer output = new StringBuffer("");
		
	//Transformer state variables 
	TransformerFactory.Resources factory_resources;
	long msg_id_count = 1;
	Charset targetCharSet = Charset.forName("UTF-8");

	//the following must be reset at the beginning of every new transform() call
	Parser parser;
	
	protected Transformer(TransformerFactory.Resources res) {
		super();
		factory_resources = res;
	}
	
	/**
	 * Impose a fixed message ID and fixed date as message references. Will only apply to the
	 * next invocation of Tranformer.transform(). The call must be renewed before
	 * every transformation with the next message ID value to be of any use.
	 * <p>The message ID and date will appear in Tranformer traces.</p>
	 * @param id 	message ID as string
	 * @param d 	a java Date for reference
	 */
	public void setLocalMessageReferences(String id, Date d) {
		if (id==null||d==null) return;
		msg_id = id;
		msg_date = d;
		msg_id_base = null;
		msg_id_nb_format = null;
	}
	
	/**
	 * Defines the message ID from a base string that will be followed by a decimal format.
	 * The transformer increments a local counter (per Transformer) with each invocation, starting at 1. 
	 * Message IDs take the value:  base.concat(decimal_format(internal_counter)). 
	 * <p>Set once and
	 * applies to all subsequent invocations of Tranformer.transform().
	 * </p>
	 * <p>The message ID and date will appear in Tranformer traces.</p>
	 * @param base 	String, to be used as unique tag or thread ID per Transformer
	 * @param df	decimal format, e.g. <code>new DecimalFormat("00000")</code>
	 */
	public void setLocalMessageReferences(String base, DecimalFormat df) {
		if (base==null||df==null) return;
		msg_id_base  = base;
		msg_id_nb_format = df;
		msg_id = null;
		msg_date = null;
	}

	/**
	 * Reads a message from the InputStream (till no more bytes are available) and then transforms it
	 * according to Parsing and XSL Transformation steps defined in the Mapping Selection Table. (Warning: The 
	 * free software version ignores any XSL Transformation directive.)
	 * <p>
	 * The OutputStream receives the transformed data.
	 * </p>
	 * 
	 * @param in	reading input message bytes from FileInputStream, ByteArrayInputStream, StringBufferInputStream, other implementations
	 * @param out	writing transformed output bytes to FileOutputStream, ByteArrayOutputStream, PrintStream, other implementations
	 * @return		count of Parser errors (still below {@link TransformerFactory#setParserExceptionThresholds(int, int) thresholds}, 
	 * otherwise an exception is thrown)
	 * @throws IOException
	 * @throws ParserException (ReverseXSL related) thrown when the tolerance thresholds 
	 * for Parser exceptions set by {@link TransformerFactory#setParserExceptionThresholds(int, int)}
	 * have been exceeded, else the DEFinition for the relevant message indicated to throw the exception in case of Parsing failures.
	 * @throws TransformerException (ReverseXSL related) most likely when the meta-data resources cannot be loaded.
	 * @throws javax.xml.transform.TransformerException (XSLT related)
	 * @throws TransformerFactoryConfigurationError  (XSLT related)
	 * @throws FactoryConfigurationError  (XSLT related)
	 * @throws ParserConfigurationException  (XSLT related)
	 * @see com.reverseXSL.transform.TransformerFactory#setParserExceptionThresholds(int, int)
	 */
	public int transform(InputStream in, OutputStream out) 
		throws IOException, TransformerException, ParserException, ParserConfigurationException, FactoryConfigurationError, TransformerFactoryConfigurationError, javax.xml.transform.TransformerException {

		//Reset
		parser = null; 
		int parseErrors = 0;
		entryName = null;
		me = null;
		log = new StringBuffer("[com.reverseXSL.transform.Transformer] LOG:\n");
		
		msgID = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date()); //default
		if (msg_id!=null) msgID = msg_id;
		else if (msg_id_base != null) {
			msgID = msg_id_base + msg_id_nb_format.format(msg_id_count);
			msg_id_count = ++msg_id_count % MSG_COUNT_MODULO;
		}

		//A. Load the message
		data = new Data(in,factory_resources.getInputCharSet());
		log.append(new TransformerMessage.LogBytesIn(data.length(),data.getFormat()));

		//B. Select Mapping
			//conventionally take the first N bytes as range for message selection patterns
			int l = Math.min(data.length(), MSG_SELECTION_RANGE);
			byte[] ba = new byte[l];
			System.arraycopy(data.getArray(), 0, ba,0,l); //getArray() is faster than getBytes() which makes yet another copy of the data
	    String msgChunk = factory_resources.getInputCharSet().decode(ByteBuffer.wrap(ba)).toString();
		me = factory_resources.getMappingEntry(msgChunk);
		if (me==null) {
			entryName = null;
			if (data.length()<MSG_SELECTION_RANGE)
			throw new TransformerException.MappingSelectionFailure(
						msgChunk.substring(0,Math.min(50,msgChunk.length()/2)).replace('\r','¤').replace('\n','§'),
						msgChunk.substring(Math.max(msgChunk.length()/2,msgChunk.length()-50),msgChunk.length()).replace('\r','¤').replace('\n','§'),
						factory_resources.getMappingSelectionSource());
			throw new TransformerException.MappingSelectionFailure_MsgSubset(
					msgChunk.substring(0,Math.min(50,msgChunk.length()/2)).replace('\r','¤').replace('\n','§'),
					msgChunk.substring(Math.max(msgChunk.length()/2,msgChunk.length()-50),msgChunk.length()).replace('\r','¤').replace('\n','§'),
					factory_resources.getMappingSelectionSource(),l);
			
			}
		entryName = me.name;
		log.append(new TransformerMessage.LogSelected_DEF_and_XSL(me.defResource,me.xslResource, entryName));		
		
		// set data conversions: those defined in mapping table entries OVERLOAD those of the transformer factory
		int conversions = me.getConversions();
		conversions = conversions>0? conversions:factory_resources.getConversions();
		log.append(new TransformerMessage.LogSelected_Conversions(Data.namedTokens(conversions)));		

		//C. PARSE
		if (me.defResource.length()>0) {
			// load the definition
			Definition def = new Definition();
			def.loadDefinition( new LineNumberReader( factory_resources.get(me.defResource) ) );
			// create a parser
			parser = new Parser( def, factory_resources.getMaxFatal(), factory_resources.getMaxTotal() );
			//applicable namespace:
			//remind that the namespace is exclusively from SET BASENAMESPACE in the DEF file
			//other parameters:
			parser.removeNonRepeatableNilOptionalElements( factory_resources.getRemoveNRNOElts() );
			// parse it, with possible data cleansing
			parseErrors = parser.parse( msgID, data.getConvertedData( conversions ).toString(),0 );
			
			if (parseErrors>0) {
				log.append(new TransformerMessage.GotParsingErrors(parseErrors,factory_resources.getMaxFatal(), factory_resources.getMaxTotal()));
				log.append(new TransformerMessage.RecordedExceptions());
				ListIterator iter = parser.exceptionIterator();
				int i = 1;
				ParserException e;
				Exception c;
				while (iter.hasNext()) {
					e = (ParserException) iter.next();
					log.append(new TransformerMessage.ExceptionReport(i,e));
					c = (Exception) e.getCause();
					if (c!=null) {
						log.append(new TransformerMessage.CausedBy(c.getLocalizedMessage()));
					}
					i++;
				}
				log.append(new TransformerMessage.TransformationContinues());

			} else 
				log.append(new TransformerMessage.ParsingOK());
			//the next method inserts the parsed XML as a "very-long-line" into the message (minimal overheads)
			output = parser.getXML( false, true ).getBuffer();
			outputFormat = DataFormat.XML;
			
		} else {
			output = data.getConvertedData( conversions );
			log.append(new TransformerMessage.NoParsing());
		}


        //D. TRANSFORM
        //if (!freeSwMode && me.xslResource.length()>0) {
        if (me.xslResource.length()>0) {	
        	// prepare
        	javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
        	Templates tpl = tf.newTemplates( new StreamSource( factory_resources.get(me.xslResource) ) );
        	javax.xml.transform.Transformer tr = tpl.newTransformer();
        	StringWriter swOUT = new StringWriter( output.length() * 3 / 2 );
        	
        	// transform with XSL
        	tr.transform( new StreamSource( new StringReader( output.toString() ) ), new StreamResult( swOUT ) );
        	//when XSL transformation fails, exceptions are thrown
        	swOUT.close();
        	
        	// update the data with the whole new payload
        	output = swOUT.getBuffer();
        	outputFormat = Data.identify(output.substring(0,Math.min(100, output.length())).toString());

        	log.append(new TransformerMessage.XsltOK());
        } else
			// log.append(freeSwMode?new TransformerMessage.NoXslt_FreeSW().getMessage():new TransformerMessage.NoXslt().getMessage());
            log.append(new TransformerMessage.NoXslt().getMessage());


        //E. Output the result

        	//all cases:
        	ByteBuffer bout = factory_resources.getOutputCharSet().encode(output.toString());
        	out.write(bout.array(),0,bout.limit()); 
        	
		log.append(new TransformerMessage.LogBytesOut(bout.limit(),outputFormat));
		
        return parseErrors;
	}
	
	
	/**
	 * A variant of {@link #transform(InputStream, OutputStream)} that guarantees
	 * a nice indentation of XML outputs; neutral operation for other brands.
	 * <p>This method is only good for printing the output or displaying it. 
	 * It is NOT recommended to use it in production as only a subset of the XML
	 * standard (good for all regular XML uses but...) is supported in the final formatting.
	 * Moreover, this operation inflates the output with a hell of extra space characters.</p>

	 * @param in	reading input message bytes from FileInputStream, ByteArrayInputStream, StringBufferInputStream, other implementations
	 * @param out	transformed output is now directed to a StringBuffer (printable!)
	 * @return		count of Parser errors (still below {@link TransformerFactory#setParserExceptionThresholds(int, int) thresholds}, 
	 * otherwise an exception is thrown)
	 * @throws IOException
	 * @throws ParserException 
	 * @throws TransformerException 
	 * @throws javax.xml.transform.TransformerException (XSLT related)
	 * @throws TransformerFactoryConfigurationError  (XSLT related)
	 * @throws FactoryConfigurationError  (XSLT related)
	 * @throws ParserConfigurationException  (XSLT related)
	 * @see com.reverseXSL.transform.TransformerFactory#setParserExceptionThresholds(int, int)
	 */
	public int printableTransform(InputStream in, StringBuffer out) 
		throws IOException, TransformerException, ParserException, ParserConfigurationException, FactoryConfigurationError, TransformerFactoryConfigurationError, javax.xml.transform.TransformerException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		int errCnt = transform(in, baos);
		baos.close();
		out.append(Formatters.niceXML(new StringBuffer(baos.toString()),
				factory_resources.getXmlIndent(),
				factory_resources.getXmlEOL()));
		return errCnt;
	}
	
	
	
	/**
	 * Reset the Transformer state and free associated resources.
	 * The transformer state is in any cases reset before each invocation of transform().
	 * <p>The internal counter is not reset.</p>
	 */
	public void reset() {
		parser = null;
		data = null;
		output = null;
		log = null;
		outputFormat = null;
		entryName = null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (log==null) log = new StringBuffer();
		log.append(factory_resources.toString());
		if (parser.getExceptionsCount()>0) log.append(parser.toString());
		return log.toString();
	}
	
	/**
	 * get a printable log of the Transformer activities during the last
	 * call to {@link #transform(InputStream, OutputStream)}. Parser warnings are also detailed.
	 * 
	 * @return	textual log in a StringBuffer
	 */
	public StringBuffer getLog() {
		return log;
	}

	/**
	 * get the name of the Mapping Selection Table entry that has just been used
	 * to transform the last message.
	 * 
	 * @return name or null, in cased no transformation yet performed or no name attached to the selected mapping table entry.
	 */
	public String getName() {
		return entryName;
	}
	
	/**
	 * Whenever {@link #transform(InputStream, OutputStream)} is invoked, it returns the total count of parser exceptions (below thresholds, otherwise
	 * an exception would have been thrown). This method simply 'reminds' about the value returned by the last run.
	 * @return total count of parser exceptions as was returned by the last call to {@link #transform(InputStream, OutputStream)}
	 */
	public int getParserExceptionsCount() {
		return this.parser.getExceptionsCount();
	}

	/**
	 * Browse the detail, exception per exception with possible nested causes, of the Parser warnings and errors.
	 * @return	a list iterator extending the standard iterator interface
	 */
	public ExceptionListIterator getParserExceptionListIterator() {
		if (parser==null) return null;
		return parser.exceptionIterator();
	}
	
	/**
	 * Returns an XML document representation of all Parser warnings and errors in sequence.
	 * @return an XML document as string, compliant with <i>ParserExceptionList.xsd</i> (see docs)
	 * @throws javax.xml.transform.TransformerException (not about ReverseXSL Transformer) when the JAXP libraries fail to build the XML representation of the Exception List
	 * @throws ParserConfigurationException (not about ReverseXSL Parser) when the JAXP libraries fail to build the XML representation of the Exception List
	 */
	public String getParserExceptionListXML() throws javax.xml.transform.TransformerException, ParserConfigurationException {
		if (parser==null) return ( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+"<ParserExceptionList xmlns=\"http://www.reverseXSL.com/Parser/Exceptions\" exceptionsCount=\"0\" relatingToMessageID=\""
				+ this.msgID +"\"><Remark>Parser not invoked!</Remark></ParserExceptionList>");
				
		StringWriter sWout = new StringWriter(3000); //3000 is only initial capacity	
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		//builderFactory.setNamespaceAware(true); --> causes a problem with TIBCO environment
		
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document doc = builder.newDocument();
		try { doc.setXmlStandalone(true);
		} catch (org.w3c.dom.DOMException de) {
		}
		Element root = doc.createElement("ParserExceptionList"); 
		root.setAttribute("xmlns","http://www.reverseXSL.com/Parser/Exceptions");
		root.setAttribute("relatingToMessageID", this.msgID);
		root.setAttribute("exceptionsCount", String.valueOf(this.parser.getExceptionsCount()));
		//root.setPrefix(null) would generate NAMESPACE_ERR!
		doc.appendChild(root);
		
		Element remarkElt = doc.createElement("Remark");
		root.appendChild(remarkElt);
		remarkElt.appendChild(doc.createTextNode("used DEFinition:"+this.me.defResource
				+", FatalExceptionsThreshold:"+factory_resources.getMaxFatal()
				+", TotalExceptionsThreshold:"+factory_resources.getMaxTotal()));
		
		// fill up the document
		ExceptionListIterator eli = parser.exceptionIterator();
		ParserException pe;
		Exception e;
		Element exceptionElt, descriptionElt, causeElt;		
		int i = 1;
		while (eli.hasNext()) {
			pe = eli.nextException();
			
			exceptionElt = doc.createElement("Exception");
			root.appendChild(exceptionElt);
			descriptionElt = doc.createElement("Error");
			descriptionElt.setAttribute("class", pe.getClass().getSimpleName());
			descriptionElt.appendChild(doc.createTextNode(pe.getMessage()));
			exceptionElt.appendChild(descriptionElt);
			exceptionElt.setAttribute("impact", pe.getImpact());
			exceptionElt.setAttribute("sequence", String.valueOf(i));
			// with cause?
			e = (Exception) pe.getCause();
			if (e!=null) {
				causeElt = doc.createElement("CausedBy");
				exceptionElt.appendChild(causeElt);
				causeElt.setAttribute("class", e.getClass().getSimpleName());
				causeElt.appendChild(doc.createTextNode(e.getMessage()));
			}
			i++;
		}
		// output the document
		javax.xml.transform.TransformerFactory factory = javax.xml.transform.TransformerFactory.newInstance();
		javax.xml.transform.Transformer dOMTransformer = factory.newTransformer();
		dOMTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
		//transformer.setOutputProperty("omit-xml-declaration","yes");
			dOMTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
			dOMTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
			dOMTransformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "3");
		//tran.setOutputProperty("standalone","yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(sWout);
		dOMTransformer.transform(source, result);
		return sWout.toString();
	}


}
