package com.reverseXSL.transform;
 
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reverseXSL.message.Data;


/**
 * Simple support for a mapping selection mechanism. Suitable as such for small collections of
 * message brands.
 * 
 * @author bernardH
 *
 */
public class MappingSelection {

	//these constants shall not be edited without affecting overall application consistency
	public static final String MAPPING_SELECTION_TABLE = "mapping_selection_table.txt";
	public static final String INJAR_MAPPING_SELECTION_TABLE = "resources/TABLES/"+MAPPING_SELECTION_TABLE;

	ArrayList localSelectionTable = null;

	final Pattern pPatternKey = Pattern.compile("^([\"'/`~]{1})(.*?)\\1\\s*$");
	final Pattern pBlankLine = Pattern.compile("^\\s+$");
	final Pattern pInputNormalizing = Pattern.compile("(?i)^\\s+InputNormalizing\\s*=\\s*\"(.*?)\"\\s*$");
	final Pattern pParserDefinition = Pattern.compile("(?i)^\\s+ParserDefinition\\s*=\\s*\"(.*?)\"\\s*$");
	final Pattern pXSLTransformation = Pattern.compile("(?i)^\\s+XSLTransformation\\s*=\\s*\"(.*?)\"\\s*$");
	final Pattern pName = Pattern.compile("(?i)^\\s+Name\\s*=\\s*\"(.*?)\"\\s*$");
	final Pattern pComment = Pattern.compile("(?i)^\\s+Comment\\s*=\\s*\"(.*?)\"\\s*$");

    /**
     * Models one entry in the Mapping Selection Table
     * 
     * @author bernardH
     *
     */
    public class MappingEntry {
    	
    	public final String patternKey;
    	public final String normalizingTokens;
    	public final String defResource;
    	public final String xslResource;
    	public final String comment;
    	public final String name;
    	public final int sourceLineNb;
   	
    	public MappingEntry(String ptrn, String normTokens, String defRsrc, String xslRsrc, String nam, String info, int srcLineNb ) {
    		patternKey = ptrn;
    		normalizingTokens = normTokens;
    		defResource = defRsrc;
    		xslResource = xslRsrc;
    		name = nam;
    		comment = info;
    		sourceLineNb = srcLineNb;
    	}
    	
    	public String toString() {
    		return ("\""+patternKey
    				+"\" NORM=\""+normalizingTokens
    				+"\" DEF=\""+defResource
    				+"\" XSL=\""+xslResource
    				+"\" NAME=\""+name
    				+"\" INFO=\""+comment+"\" FROM LINE="+sourceLineNb+"\n");
    	}
    	
    	public int getConversions() {
    		int conversionFlags = 0;
			// add a '+' delimiter at the end
			String opts = new String(normalizingTokens + "+");
			String opt = "";
			for (int i = opts.indexOf('+'); i > 0; i = opts.indexOf('+')) {
				opt = opts.substring(0, i);
				opts = opts.substring(i + 1);
				int val = Data.tokenValue(opt);
				if (val >= 0)
					conversionFlags += val;
//				else
//					System.err.println("requested input data conversion ["
//							+ opt + "] is unknown! skipping...");
			}
			return conversionFlags;
    	}
    }
	
    /**
     * Construct a fake empty table to later overload the selection methods.
     */
    public MappingSelection() {
    	this.localSelectionTable = null;
    }
    
	/**
	 * Load a mapping selection table from an input stream reader.
	 * 
	 * @param r whatever character stream reader available 
	 * @throws TransformerException
	 * @throws IOException
	 */
	public MappingSelection(Reader r) throws TransformerException, IOException {
		LineNumberReader lnr = new LineNumberReader(r);
		String line = lnr.readLine();
		Matcher m;
		String p,f,d,x,n,c; //Pattern, Flags, Def, Xslt, Name, Comment
		int l=0;
		p=f=d=x=n=c=null; //clear
		
		while (line!=null) {
			//skip lines starting with a # or empty or blank
			if (line.startsWith("#")||(line.length()<=0)
					|| line.matches(pBlankLine.pattern())) {
				line = lnr.readLine(); continue; 
			}
			//assume a structure as 
			//"<regex>"<NL>
			//  [<SP>InputNormalizing<SP>=<SP>"<CleansingFlags>"<NL>]
			//	[<SP>ParserDefinition<SP>=<SP>"<DEF resource name>"<NL>]
			//	[<SP>XSLTransformation<SP>=<SP>"<XSL resource name>"<NL>]
			//	[<SP>NameSpaceSuffix<SP>=<SP>"<namespace extension>"<NL>]
			//	[<SP>Comment<SP>=<SP>"any comment"<NL>]
			//where every element is optional but the regex
			m = pPatternKey.matcher(line);
			if ( (	line.startsWith("\"")||
					line.startsWith("'")||
					line.startsWith("/")||
					line.startsWith("`")||
					line.startsWith("~") )
					&& m.find()) {
				//save the previously loaded MappingEntry, if any
				if (p!=null)
					this.add(new MappingEntry(
							p, 
							f==null?"":f,
							d==null?"":d,
							x==null?"":x,
							n==null?"":n,
							c==null?"":c,
							l));
				p=f=d=x=n=c=null; l=0;//clear
				//start loading a new MappingEntry
				p = m.group(2); //pattern
				try {
				//ensure the pattern can be compiled (throws an exception)
				Pattern.compile(p).matcher("");
				} catch (Exception e){
					throw new TransformerException.InvalidRegexSyntax(p,lnr.getLineNumber(),e.getMessage());
				}
				l = lnr.getLineNumber();
				line = lnr.readLine();
				continue;
			}
			if (line.matches(pInputNormalizing.pattern())) {
				m = pInputNormalizing.matcher(line);
				m.find();
				if (f!=null) throw new TransformerException.OverloadedMappingSelectionAttribute(
						lnr.getLineNumber(),line,"InputNormalizing",f,l);
				f = m.group(1); //normalizing token list
				line = lnr.readLine();
				continue;
			}
			if (line.matches(pParserDefinition.pattern())) {
				m = pParserDefinition.matcher(line);
				m.find();
				if (d!=null) throw new TransformerException.OverloadedMappingSelectionAttribute(
						lnr.getLineNumber(),line,"ParserDefinition",d,l);
				d = m.group(1); //DEF resource
				line = lnr.readLine();
				continue;
			}
			if (line.matches(pXSLTransformation.pattern())) {
				m = pXSLTransformation.matcher(line);
				m.find();
				if (x!=null) throw new TransformerException.OverloadedMappingSelectionAttribute(
						lnr.getLineNumber(),line,"XSLTransformation",x,l);
				x = m.group(1); //Xsl transformation
				line = lnr.readLine();
				continue;
			}
			if (line.matches(pName.pattern())) {
				m = pName.matcher(line);
				m.find();
				if (n!=null) throw new TransformerException.OverloadedMappingSelectionAttribute(
						lnr.getLineNumber(),line,"Mapping Entry Name",n,l);
				n = m.group(1); //Name
				line = lnr.readLine();
				continue;
			}
			if (line.matches(pComment.pattern())) {
				m = pComment.matcher(line);
				m.find();
				if (c!=null) c = c.concat(", "+m.group(1));
				else c = m.group(1); //Comment
				line = lnr.readLine();
				continue;
			}
			throw new TransformerException.UnexpectedMappingSelectionLine(lnr.getLineNumber(),line);
		}
		//save the MappingEntry loaded last, if any
		if (p!=null)
			this.add(new MappingEntry(
					p, 
					f==null?"":f,
					d==null?"":d,
					x==null?"":x,
					n==null?"":n,
					c==null?"":c,
					l));
	}
		
	private boolean add(Object arg0) throws TransformerException {
			//50 is the initial capacity and not limitative
			if (localSelectionTable==null) localSelectionTable = new ArrayList(50);
			//no-operation if argument is not of the proper type
			if (!(arg0 instanceof MappingEntry))
				return false;
			//ensure unicity of entries
			for (int i=0;i<localSelectionTable.size();i++) {
				if (((MappingEntry)localSelectionTable.get(i)).patternKey.equals(((MappingEntry)arg0).patternKey))
					throw new TransformerException.DuplicateMappingSelectionKey(
							((MappingEntry)arg0).sourceLineNb,
							((MappingEntry)localSelectionTable.get(i)).patternKey,
							((MappingEntry)localSelectionTable.get(i)).sourceLineNb);
//unicity of names shall NOT be tested in order to allow the same onward 'routing' to be defined for different entries
//				if (((MappingEntry)localSelectionTable.get(i)).name.equals(((MappingEntry)arg0).name))
//					throw new TransformerException.DuplicateMappingSelectionKey(
//							((MappingEntry)arg0).sourceLineNb,
//							((MappingEntry)localSelectionTable.get(i)).name,
//							((MappingEntry)localSelectionTable.get(i)).sourceLineNb);
			}
			return localSelectionTable.add(arg0);
		}

	public MappingEntry matchEntry(String msgChunk ) {
		MappingEntry me;
		if (localSelectionTable!=null) {
			for (int i=0;i<localSelectionTable.size();i++) {
				me = (MappingEntry)localSelectionTable.get(i);
				if (Pattern.compile(me.patternKey).matcher(msgChunk).find())
					return me;
			}		
		}
		return (null);

	}

	String dump() {
		StringBuffer sb = new StringBuffer();
		sb.append("MAPPING SELECTION TABLE:\n");
		sb.append(localSelectionTable.toString());
		return sb.toString();
	}
}
