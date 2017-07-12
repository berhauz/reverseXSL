package com.reverseXSL.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.reverseXSL.message.Data;
import com.reverseXSL.transform.MappingSelection.MappingEntry;

/**
 * A TransformerFactory is instantiated with a source of transformation
 * meta-data, namely mapping selection meta-data as well as parsing DEFinitions
 * and XSL transformation templates.
 * <p>
 * The distinction between a TransformerFactory and subsequent Transformer
 * objects follows the classical java pattern.
 * </p>
 * <p>
 * A factory is instantiated, used to set various parameters, and then used to
 * instantiate <b>Transformer objects</b>. All Transformer objects from the same
 * factory inherit and share meta-data from the factory. The factory can be used
 * to tune parameters that control the behaviour of Transformer objects.
 * </p>
 * <p>
 * Transformer objects are in turn used to execute message parsings and
 * transformations, with the help of resources and parameters inherited from the
 * factory.
 * </p>
 * Note that the link between the factory and Transformer objects is never
 * broken in the present design: changes to factory parameters may affect the
 * behaviour of Transformer objects already generated! Yet, proper practice
 * recommends to set all factory features before instantiating the first
 * Transformer, and using it.
 * 
 * @author bernardH
 * @see com.reverseXSL.transform.Transformer
 */
public final class TransformerFactory {

	// these initialisations can be edited to supply other default values
	// most are _working variables shared through the Resources inner class
	private int _maxFatalParserExceptions = 0;
	private int _maxTotalParserExceptions = 10;
	private boolean _removeNRNOElts = true; // Parser flag: remove Non
											// Repeatable Nil Optional Elements
	private String _xml_eOL = "\n"; // used by niceXML() if ever needed!
	private String _xml_indent = "   "; // used by niceXML() if ever needed!
	private Charset resourcesCharSet = Charset.forName("UTF-8");
	private Charset _inputCharSet = Charset.forName("UTF-8"); // default CharSet
																// applicable to
																// the input
																// stream
	private Charset _outputCharSet = Charset.forName("UTF-8"); // default
																// CharSet
																// applicable to
																// the output
																// stream
	private int _conversion_flags = Data._NONE;

	// other _working vars
	private MappingSelection _mst;
	private String _mst_source;
	private JarFile _jarfile;
	private Reader _myDEF;
	private Reader _myXSL;
	private String _classpathExtension;

	/**
	 * This is a wrapper class with accessors for all properties of a
	 * transformer factory. Given the numerous ways to instantiate a factory,
	 * this class will have as many implementations, yet provide a common
	 * interface and set of methods to deal with the factory.
	 * 
	 * @author bernardH
	 * 
	 */
	abstract class Resources {

		abstract Reader get(String resourceName) throws IOException;

		MappingEntry getMappingEntry(String msgChunk) {
			return _mst.matchEntry(msgChunk);
		}

		int getMaxFatal() {
			return _maxFatalParserExceptions;
		}

		int getMaxTotal() {
			return _maxTotalParserExceptions;
		}

		boolean getRemoveNRNOElts() {
			return _removeNRNOElts;
		}

		String getMappingSelectionSource() {
			return _mst_source;
		}

		String getXmlEOL() {
			return _xml_eOL;
		}

		String getXmlIndent() {
			return _xml_indent;
		}

		Charset getInputCharSet() {
			return _inputCharSet;
		}

		Charset getOutputCharSet() {
			return _outputCharSet;
		}

		int getConversions() {
			return _conversion_flags;
		}

		/*
		 * (non-Javadoc) A quick dump of what resources are used (where do they
		 * come from)
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("RESOURCES INVENTORY:\n");
			sb.append("     Parser DEFinitions and XSLTemplates from: ");
			if (_myDEF != null || _myXSL != null)
				sb.append("PreSET DEF and XSL\n");
			else if (_jarfile != null)
				sb.append("JAR FILE " + _jarfile.getName() + ";\n");
			else
				sb.append("CLASSPATH;\n");
			sb.append("     Mapping Selection Table: " + _mst_source + "\n");
			// sb.append(_mst.toString()+"\n\n");
			return sb.toString();
		}

	}

	private TransformerFactory() {
		super();
	}

	/**
	 * Instantiates a Transformer Factory that will load meta-data from the
	 * software distribution jar file itself.
	 * <p>
	 * This variant uses the classloader. It is then able to find all meta-data
	 * resources that are placed on the CLASSPATH.
	 * </p>
	 * 
	 * @return a factory instance
	 */
	public static final TransformerFactory newInstance() {
		TransformerFactory tf = new TransformerFactory();
		tf._myDEF = tf._myXSL = null;
		tf._mst = null;
		tf._mst_source = "--not set--";
		tf._jarfile = null;
		tf._classpathExtension = null;

		return tf;
	}

	/**
	 * Instantiate a new Transformer from the factory. In this simplest-to-use
	 * version, all resources are loaded from the CLASSPATH, meaning: the
	 * mapping selection table, plus the parsing DEF, and the XSL resources, are
	 * fetched from the ClassLoader when needed.
	 * 
	 * @return a Transformer instance
	 * @throws IOException
	 * @throws TransformerException
	 */
	public Transformer newTransformer() throws IOException,
			TransformerException {

		if (this._mst == null) {
			Reader r = null;
			StringBuffer sb = new StringBuffer("");
			try {
				r = getReaderFromClassLoader(MappingSelection.INJAR_MAPPING_SELECTION_TABLE);
			} catch (IOException ioe) {
				sb.append(ioe.getMessage());
			}
			if (r != null) {
				this._mst = new MappingSelection(r);
				this._mst_source = "$CLASSPATH/"
						+ MappingSelection.INJAR_MAPPING_SELECTION_TABLE;
			} else {
				try {
					r = getReaderFromClassLoader(MappingSelection.MAPPING_SELECTION_TABLE);
				} catch (IOException ioe) {
					sb.append(" and ").append(ioe.getMessage());
				}
				if (r == null) // none of the paths worked
					throw new IOException(sb.toString());
				else {
					this._mst = new MappingSelection(r);
					this._mst_source = "$CLASSPATH/"
							+ MappingSelection.MAPPING_SELECTION_TABLE;
				}
			} 
		}
		Resources res = new Resources() {
			Reader get(String resourceName) throws IOException {
				return TransformerFactory.this
						.getReaderFromClassLoader(resourceName);
			}
		};
		Transformer t = new Transformer(res);
		return t;
	}

	/**
	 * Instantiates a new Transformer instance from the factory, which will
	 * bypass the Mapping Selection process. It will always apply the parsing
	 * DEF and XSL transformation specified in argument. One or both of these
	 * arguments may be nulls in order to skip the corresponding transformation
	 * step.
	 * 
	 * @param myDefinition
	 *            a Reader on a DEF file or null
	 * @param myXSLT
	 *            a Reader on an XSL file or null
	 * @return a Transformer instance
	 * @throws IOException
	 *             in case of read errors from one of the Reader arguments
	 */
	public Transformer newTransformer(Reader myDefinition, Reader myXSLT)
			throws IOException {
		// convert whatever Readers passed as argument to StringReaders
		// so that a reader.reset() is surely feasible (otherwise a transform()
		// operation will fail on the second call!)
		char[] ca = new char[8000];
		StringBuffer sb = new StringBuffer();
		int nbin;
		if (myDefinition == null)
			_myDEF = null;
		else {
			// load by blocks of 8000 chars from the foreign reader
			do {
				nbin = myDefinition.read(ca);
				if (nbin > 0)
					sb.append(ca, 0, nbin);
			} while (nbin > 0);
			// and convert to a String reader (in memory!)
			_myDEF = new StringReader(sb.toString());
		}
		sb = new StringBuffer();
		if (myXSLT == null)
			_myXSL = null;
		else {
			// load by blocks of 8000 chars from the foreign reader
			do {
				nbin = myXSLT.read(ca);
				if (nbin > 0)
					sb.append(ca, 0, nbin);
			} while (nbin > 0);
			// and convert to a String reader (in memory!)
			_myXSL = new StringReader(sb.toString());
		}

		// create an empty Mapping Selection Table for building a MappingEntry
		// inner class with always the same DEF and XSL
		this._mst = new MappingSelection();
		Resources res = new Resources() {
			// overload the matching process
			MappingEntry getMappingEntry(String msgChunk) {
				return _mst.new MappingEntry(".*", "", _myDEF == null ? ""
						: "PreSET_DEF", _myXSL == null ? "" : "PreSET_XSL",
						"PRESET",
						"DEF and XSL sources as preSet in TransformerFactory",
						0);
			}

			Reader get(String resourceName) throws IOException {
				if (resourceName.equals("PreSET_DEF")) {
					_myDEF.reset();
					return _myDEF;
				}
				if (resourceName.equals("PreSET_XSL")) {
					_myXSL.reset();
					return _myXSL;
				}
				return null;
			}
		};

		Transformer t = new Transformer(res);
		return t;
	}

	/**
	 * Instantiates a Transformer with meta-data from an explicitly specified
	 * jar file.
	 * <p>
	 * The jar file does not need to be on the CLASSPATH and may contain
	 * alternative transformation meta-data sets. The jar must contain a Mapping
	 * Selection Table and all other dependent meta-data pieces (Parsing DEFs
	 * and XSL templates). The specified jar does not take precedence but
	 * replaces entirely the default resources, including the mapping selection
	 * table.
	 * </p>
	 * <p>
	 * The method {@link #setExternalMappingSelectionTable(Reader)} still allows
	 * to overrride the mapping selection table (over that which can be on the
	 * CALSSPATH, else within the explicitly specified jar).
	 * </p>
	 * 
	 * @param jarFile
	 *            a JarFile object containing all meta-data resources
	 * @return a Transformer instance
	 * @throws IOException
	 * @throws TransformerException
	 */
	public Transformer newTransformer(JarFile jarFile) throws IOException,
			TransformerException {

		this._jarfile = jarFile;
		if (this._mst == null) {
			this._mst = new MappingSelection(
					getReaderFromJar(MappingSelection.INJAR_MAPPING_SELECTION_TABLE));
			this._mst_source = "JAR (" + jarFile.getName() + ")"
					+ MappingSelection.INJAR_MAPPING_SELECTION_TABLE;
		}
		Resources res = new Resources() {
			Reader get(String resourceName) throws IOException {
				return TransformerFactory.this.getReaderFromJar(resourceName);
			}
		};
		Transformer t = new Transformer(res);
		return t;
	}

	private Reader getReaderFromJar(String resourceName) throws IOException {
		ZipEntry ze = this._jarfile.getEntry(resourceName);
		if (ze == null)
			throw new IOException("The resource [" + resourceName
					+ "] is not a valid entry in the java archive ["
					+ this._jarfile.getName() + "]!");
		InputStream inS = this._jarfile.getInputStream(ze);
		if (inS == null)
			throw new IOException("The resource [" + resourceName
					+ "] is not available from the java archive ["
					+ this._jarfile.getName() + "]!");
		StringBuffer sb = new StringBuffer();
		byte[] ba = new byte[inS.available()];
		int nbin;
		do {
			nbin = inS.read(ba);
			if (nbin > 0)
				sb.append(resourcesCharSet.decode(ByteBuffer.wrap(ba, 0, nbin))
						.toString());
		} while (nbin > 0);
		return new StringReader(sb.toString());
	}

	private Reader getReaderFromClassLoader(String resourceName)
			throws IOException {
		java.lang.ClassLoader cl = this.getClass().getClassLoader();
		InputStream inS = cl.getResourceAsStream(resourceName);
		if (inS == null)
			// give a chance to the CLASSPATH extension
			if (this._classpathExtension != null) {
				inS = new FileInputStream(this._classpathExtension
						+ resourceName);
				// DEAD-CODE: if (inS==null) //a FileNotFoundException is
				// actually thrown...just in case of null...
				// throw new
				// IOException("The resource ["+resourceName+"] is not available from the CLASSPATH, neither "+this._classpathExtension+"!");
			} else
				throw new IOException("The resource [" + resourceName
						+ "] is not available from the CLASSPATH!");
		StringBuffer sb = new StringBuffer();
		byte[] ba = new byte[inS.available()];
		int nbin;
		do {
			nbin = inS.read(ba);
			if (nbin > 0)
				sb.append(resourcesCharSet.decode(ByteBuffer.wrap(ba, 0, nbin))
						.toString());
		} while (nbin > 0);
		return new StringReader(sb.toString());
	}

	/**
	 * Imposes the specified Mapping Selection Table meta-data (full
	 * replacement).
	 * <p>
	 * The CLASSPATH or a JAR will still be used as source for transformation
	 * resources (Parsing DEFs and XSL templates), depending upon the method of
	 * creating the Transformer.
	 * </p>
	 * 
	 * @param r
	 *            A Reader from which the table will be loaded
	 * @throws IOException
	 * @throws TransformerException
	 */
	public void setExternalMappingSelectionTable(Reader r) throws IOException,
			TransformerException {
		setExternalMappingSelectionTable(r, null);
	}

	/**
	 * Imposes the specified Mapping Selection Table meta-data (full
	 * replacement).
	 * <p>
	 * One can provide an optional path argument that will formally be added to
	 * the CLASSPATH for loading resources (Parsing DEFs and XSL templates).
	 * </p>
	 * 
	 * @param r
	 *            A Reader from which the table will be loaded
	 * @param path
	 *            if not null or empty, a directory path to be used as root
	 *            location for loading associated mapping resources
	 * @throws IOException
	 * @throws TransformerException
	 */
	public void setExternalMappingSelectionTable(Reader r, String path)
			throws IOException, TransformerException {
		this._mst = new MappingSelection(r);
		if (path != null && path.length() > 0)
			this._classpathExtension = path.endsWith(File.separator) ? path
					: path + File.separator;
		this._mst_source = "supplied via TransformerFactory API as Input Stream Reader";
	}

	/**
	 * Sets specific Parser tolerance to input message syntax errors. Every
	 * element of syntax definition for a given message in a Parser DEF file is
	 * associated to a Warning or Fatal exception. These exceptions are raised
	 * whenever the corresponding syntax rule is violated by an input message;
	 * but 'raised' doesn't mean 'thrown': indeed, the Parser silently records
	 * all violations till thresholds are reached, at which time an exception
	 * will effectively be thrown. In other words, the Parser actually attempts
	 * to recover from syntax violations and continue parsing the input (e.g.
	 * skipping faulty data) util the thresholds are met. The default threshold
	 * is 10 warnings and no fatal error.
	 * 
	 * @param maxFatal
	 *            new acceptable count of syntax violations conunted as Fatal
	 * @param maxTotal
	 *            new total acceptable count of syntax violations (all as
	 *            warnings, or acceptable fatal + acceptable warnings)
	 */
	public void setParserExceptionThresholds(int maxFatal, int maxTotal) {
		this._maxFatalParserExceptions = maxFatal;
		this._maxTotalParserExceptions = maxTotal;
	}

	/**
	 * Would cause (if set TRUE) to remove from the output XML document all data
	 * elements with a NIL value that are optional or conditional elements,
	 * <b>and</b> whose matching definition indicates that the element is non
	 * repeatable (i.e. ACC 1), <b>and</b> whose minimum size requirement is >0.
	 * <p>
	 * This function is actually quite useful on messages based on the principle
	 * of positional data elements within 'segments' (e.g. EDIFACT, TRADACOMS,
	 * X12, etc.). Indeed, most positions (think 'slots') in such segments are
	 * occupied by optional/conditional data elements, all unique and
	 * distinguished by their relative position in the 'segment'. Every
	 * unoccupied position will yield a corresponding NIL data element in XML,
	 * that can be suppressed from the XML output if this method is set to TRUE.
	 * <p>
	 * NIL data elements are supressed only if they have a min/max size
	 * specification (of the kind <code>[1..15]</code> ) with a minimum of at
	 * least 1. Obviously, if 0 is an acceptable size for the element, there's
	 * no reason to suppress the element.
	 * <p>
	 * Moreover, the element must be non-repeatable otherwise there is a risk to
	 * eat-up intermediate elements within series, causing undesirable rank
	 * shifts.
	 * <p>
	 * The default value is TRUE.
	 * 
	 * @param bool
	 *            whether or not to Remove non-repeatable NIL optionals
	 */
	public void setParserRemoveNonRepeatableNilOptionalElements(boolean bool) {
		this._removeNRNOElts = bool;
	}

	/**
	 * Sets the pattern of chars that will be repeated at each depth level to
	 * indent the printable-XML output. Only applicable to
	 * {@link Transformer#printableTransform(InputStream, StringBuffer)}
	 * 
	 * @param ptrn
	 *            a pattern like "   ", or "|  " for increased readability
	 *            (really cool!).
	 */
	public void setPrintableXmlIndent(String ptrn) {
		_xml_indent = ptrn;
	}

	/**
	 * Sets the Charset used for decoding input message bytes into characters,
	 * using a java Charset object. The default is UTF-8.
	 * 
	 * @param charset
	 *            a java {@link Charset} or null to reset to default UTF-8
	 * @see #setInputCharSet(String)
	 */
	public void setInputCharSet(Charset charset) {
		_inputCharSet = charset == null ? Charset.forName("UTF-8") : charset;
	}

	/**
	 * Sets the Charset used for decoding input message bytes into characters,
	 * using a java Charset name. The default is UTF-8.
	 * <p>
	 * Useful character set names to consider are the legacy 7bit "US-ASCII",
	 * 8-bit collections like "ISO-8859-1" (ISO Latin Alphabet No. 1 or
	 * 2,3,4..), "EBCDIC-INT", and "EBCDIC-CP-US", multibyte sets like
	 * "Shift_JIS", and the now standard Unicode Transformation Formats "UTF-8",
	 * or "UTF-16". The full listing is available at the <a
	 * href="http://www.iana.org/assignments/character-sets"><i>IANA Charset
	 * Registry</i></a> (http://www.iana.org/assignments/character-sets).
	 * </p>
	 * 
	 * @param charset
	 *            a java {@link Charset} name as String or null to reset to
	 *            default UTF-8
	 */
	public void setInputCharSet(String charset) {
		_inputCharSet = charset == null ? Charset.forName("UTF-8") : Charset
				.forName(charset);
	}

	/**
	 * Sets the Charset used for encoding output message characters into bytes,
	 * using a java Charset object. The default is UTF-8.
	 * 
	 * @param charset
	 *            a java {@link Charset} or null to reset to default UTF-8
	 * @see #setOutputCharSet(String)
	 */
	public void setOutputCharSet(Charset charset) {
		_outputCharSet = charset == null ? Charset.forName("UTF-8") : charset;
	}

	/**
	 * Sets the Charset used for encoding output message characters into bytes,
	 * using a java Charset object. The default is UTF-8.
	 * <p>
	 * Useful character set names to consider are the legacy 7bit "US-ASCII",
	 * 8-bit collections like "ISO-8859-1" (ISO Latin Alphabet No. 1 or
	 * 2,3,4..), "EBCDIC-INT", and "EBCDIC-CP-US", multibyte sets like
	 * "Shift_JIS", and the now standard Unicode Transformation Formats "UTF-8",
	 * or "UTF-16". The full listing is available at the <a
	 * href="http://www.iana.org/assignments/character-sets"><i>IANA Charset
	 * Registry</i></a> (http://www.iana.org/assignments/character-sets).
	 * 
	 * </p>
	 * 
	 * @param charset
	 *            a java {@link Charset} name as String or null to reset to
	 *            default UTF-8
	 */
	public void setOutputCharSet(String charset) {
		_outputCharSet = charset == null ? Charset.forName("UTF-8") : Charset
				.forName(charset);
	}

	/**
	 * Sets the conversions to perform on input data at byte or character level
	 * before applying transformations in proper.
	 * 
	 * @param specs
	 *            add flag values as defined by {@link Data} constants.
	 * @see Data
	 */
	public void setInputDataConversions(int specs) {
		_conversion_flags = specs;
	}

}
