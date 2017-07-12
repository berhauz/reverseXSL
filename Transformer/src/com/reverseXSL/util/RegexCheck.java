package com.reverseXSL.util;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import com.reverseXSL.parser.Parser;
import com.reverseXSL.parser.ParserException;
import com.reverseXSL.parser.SD;



/**
 * <p>Command-line tool.</p><p>Allows experimenting with regular expressions in general. 
 * Shows in particular the reverseXSL Parser 
 * behaviour 
 * when the relevant expression is used for segment cutting (as in CUT "pattern"), or data validation, or value extraction.
 * </p> <p>
 * Invoke via :<p><code>&nbsp;&nbsp;java -cp ReverseXSL.jar com.reverseXSL.util.RegexCheck</code></p>.
 *</p>
 */
public final class RegexCheck implements ActionListener {

	private static final long serialVersionUID = 7855341717884188567L;


	JFrame converterFrame;
    JPanel setupPanel, resultPanel;
    JComboBox regexField; 
    JTextArea targetField;
    JTextPane resultTextPane;
    StyledDocument doc;
    JScrollPane targetPane, resultZone;
    JButton testButton;
    JSplitPane splitPane;
    JRadioButton crlf, lfonly, nocrlf;
	Image rxIcon = null;
	Image downButton = null;

    // Constructor
    public RegexCheck() {
    	//load image resources
    	try {
    		byte[] iconImgBA;
        	java.lang.ClassLoader cl = this.getClass().getClassLoader();
        	InputStream imgStream = cl.getResourceAsStream("com/reverseXSL/RX-icon.PNG");
    		iconImgBA = new byte[imgStream.available()];
    		if (imgStream.read(iconImgBA)>0) {
    			rxIcon = java.awt.Toolkit.getDefaultToolkit().createImage(iconImgBA);
    		}
    		imgStream = cl.getResourceAsStream("com/reverseXSL/util/downButton.gif");
    		iconImgBA = new byte[imgStream.available()];
    		if (imgStream.read(iconImgBA)>0) {
    			downButton = java.awt.Toolkit.getDefaultToolkit().createImage(iconImgBA);
    		}
    	} catch (Exception e) {
    		//silently drop 
    	}
    		
    	// Create the frame and container.
    	converterFrame = new JFrame(" Regex Probe from www.reverseXSL.com");
    	if (rxIcon!=null) converterFrame.setIconImage(rxIcon);
    	// Exit when the window is closed.
    	converterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    	//set up split panes
    	
    	setupPanel = new JPanel();
    	setupPanel.setLayout(new BoxLayout(setupPanel,BoxLayout.PAGE_AXIS));
    	setupPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    	resultPanel = new JPanel();
    	resultPanel.setLayout(new BoxLayout(resultPanel,BoxLayout.PAGE_AXIS));
    	resultPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    	splitPane = new JSplitPane(
    	          JSplitPane.VERTICAL_SPLIT, setupPanel, resultPanel);
    	splitPane.setDividerSize(8);
    	splitPane.setContinuousLayout(true);
    	
    	//------------ Build the SETUP panel ----------------------.
    	
    	// Create regex editable combo
    	// Complex Demo String:
    	//		Paste or type here the String to match
    	// 		against the regular expression
    	// 		THIS/IS/DEMO/STUFF:A123:BCD567-X-Y-Z
    	//		"(?x).+ #match each line, (?x) flag allows comments",
    	//		"(?x)(S.*) #capture from first S till end of line",
    	//		"(?x)here #matching one word",
    	//		"(?x).*here.* #matching one line",
    	//		"(?xs).*here.* #matching all lines, s==dotAll flag",
    	//		"(?xm)^THIS #test if a line starts with THIS",
    	//		"(?x)or(.*)here #capture between 'or' and 'here'",
    	//		"(?x)(\\S+)\\s+ #cut-out words",
    	//		"(?xm)(?:^|[/:\\-])(.+?)(\\d+?)?(?=[/:\\-]|$) #fields!",
    	//		"(?x)^(...) #capture first 3 chars of entire String",
    	//		"(?xm)^(...) #first 3 chars, each line, m==multiline",
    	//		"(?x)(S.*?)(?:[\\ ]) #capture word(s) starting by S",
    	//		"(?x)/([^/]*) #cut elements preceded by /",
    	//		"(?x)[/:]([^/:]*) #cut elements preceded by / or :",
    	//		"(?xsm)(?:^.+?){2}^(.*?)/ #1st element on 3rd line"
    	
    	//Simpler Demo String:
    	//		THIS/IS/A/SAMPLE:A123:DEF567-X-Y-Z
    	String []  regexHist = { 
    			"^.*$",
    			"(?x).* #match entire line, (?x) flag allows #comments",
    			"(?x)^THIS #test if the line starts with THIS",
    			"(?x)^THAT #test if the line starts with THAT",
    			"(?x)(M..) #capture from M plus 2 characters",
    			"(?x)(M.*) #capture from M plus any chars",
    			"(?x)(M.*$) #capture from M till end of line",
    			"(?x)(M.*E) #capture from M till last E",
    			"(?x)(M.*?E) #capture from M till first E",
    			"(?x)M(.*?)E #capture between M and first E",
    			"(?x)(M.*?\\d) #capture from M till first digit",
    			"(?x)/([^/]*) #any not/ next to a /",
    			"(?x)([^/]*)/ #any not/ followed by /",
    			"(?x)([^/]*:) #any not/ ending with :",
    			"(?x)([^/:\\-]*) #capture 0 to n char not in /,:,-",
    			"(?x)([^/:\\-]+) #capture 1 to n char not in /,:,-",
    			"(?x)^(?:.*?/){2}(.*?)/ #3rd element /-separated",
    			"(?x)^(?:.*?[/:]){4}(.*?)[/:] #5th el't /or:separated",
    			"(?x)(?:^|[/:\\-])(.+?)(\\d+?)?(?=[/:\\-]|$) #whoaw!",
    	};
    	regexField = new JComboBox(regexHist);
    	regexField.setEditable(true);
    	regexField.setFont(new Font("Courier New",Font.PLAIN,12));
    	//add the same action listner for the RUN button and Regex Combo box
    	regexField.setActionCommand("SELECT");
    	regexField.addActionListener(this);
    	regexField.setMaximumSize(new Dimension(800,25));
    	
    	//target for applying the regex
    	targetField = new JTextArea("THIS/IS/A/SAMPLE:A123:DEF567-X-Y-Z",3, 50);
    	targetPane = new JScrollPane(targetField,
    			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
    			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    	targetField.setFont(new Font("Courier New",Font.PLAIN,12));
    	targetField.setEditable(true);
    	    	    	
        //Create the radio buttons.
        crlf = new JRadioButton("CR LF");
        crlf.setActionCommand("CRLF");
        crlf.setSelected(true);
        lfonly = new JRadioButton("LF only");
        lfonly.setActionCommand("LFONLY");
        nocrlf = new JRadioButton("no CR, no LF");
        nocrlf.setActionCommand("NOCRLF");
        ButtonGroup lineTermin = new ButtonGroup();
        lineTermin.add(lfonly);
        lineTermin.add(crlf);
        lineTermin.add(nocrlf);

    	//add widgets to the split panel
    	setupPanel.add(new JLabel("The RegExp:"));
    	
    	//wrap the combo in a panel of its own to enforce left alignment
    	JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1,
                               BoxLayout.PAGE_AXIS));
        p1.setAlignmentX(Component.LEFT_ALIGNMENT);
        p1.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
    	p1.add(regexField);
    	setupPanel.add(p1);
    	
    	setupPanel.add(new JLabel("is applied to:"));
    	JLabel rule = new JLabel("          1         2         3         4         5         6");
    	rule.setFont(new Font("Courier New",Font.PLAIN,12));
    	rule.setForeground(Color.darkGray);
    	setupPanel.add(rule);
    	rule = new JLabel("0123456789012345678901234567890123456789012345678901234567890");
    	rule.setFont(new Font("Courier New",Font.PLAIN,12));
    	rule.setForeground(Color.darkGray);
    	setupPanel.add(rule);
    	
    	//wrap the text area in a panel of its own to enforce left alignment
    	JPanel p2 = new JPanel();
        p2.setLayout(new BoxLayout(p2,
                               BoxLayout.PAGE_AXIS));
        p2.setAlignmentX(Component.LEFT_ALIGNMENT);
        //p2.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
    	p2.add(targetPane);
    	setupPanel.add(p2);
    	
    	//create a panel for the radio buttons
    	JPanel p3 = new JPanel();
        p3.setLayout(new BoxLayout(p3,
                               BoxLayout.LINE_AXIS));
        p3.setAlignmentX(Component.LEFT_ALIGNMENT);
        p3.setBorder(BorderFactory.createEmptyBorder(0,10,5,0));
    	p3.add(new JLabel("assuming lines terminated by:"));
    	p3.add(lfonly);
    	p3.add(crlf);
    	p3.add(nocrlf);
    	setupPanel.add(p3);
    	
    	//------------ Build the RESULT panel ----------------------.
    	//button
    	if (downButton!=null) {
    		ImageIcon buttonIcon = new ImageIcon(downButton);
    		testButton = new JButton("GO",buttonIcon);
    	}
    	else testButton = new JButton("GO");
    	testButton.setVerticalTextPosition(AbstractButton.CENTER);
    	testButton.setHorizontalTextPosition(AbstractButton.LEADING);
    	testButton.setActionCommand("GO");
    	// Listen to events from Convert button.
    	testButton.addActionListener(this);

    	//result
    	resultTextPane = new JTextPane();
    	doc = resultTextPane.getStyledDocument();
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().
                        getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontSize(def, 12);
        StyleConstants.setFontFamily(def, "Courier New");
        Style regular = doc.addStyle("regular", def);
        StyleConstants.setItalic(regular, true);
        Style comp = doc.addStyle("computer", def);
        StyleConstants.setBold(comp, true);
        Style title = doc.addStyle("title", def);
        StyleConstants.setUnderline(title, true);
        Style red = doc.addStyle("red", def);
        StyleConstants.setForeground(red, Color.red);
        try {
			insertText("Results are displayed here\n\nThis is a free tool from ReverseXSL.com\n\n(c)2009");
		} catch (BadLocationException e) {
		}
    	
//    	resultField = new JTextArea("Parser results will be displayed here\n\nThis is a free tool from ReverseXSL.com\n\n(c)2009 Art of e.Biz",15, 30);
//    	resultField.setFont(new Font("Courier New",Font.PLAIN,12));
    	resultZone = new JScrollPane(resultTextPane,
    			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
    			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    	resultZone.setPreferredSize(new Dimension(250,250));
    	//resultZone.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(),BorderFactory.createEmptyBorder(0,5,5,5)));
    	resultTextPane.setEditable(false);

    	//add widgets to the split panel
    	JPanel p4 = new JPanel();
        p4.setLayout(new BoxLayout(p4,
                               BoxLayout.PAGE_AXIS));
        p4.setAlignmentX(Component.LEFT_ALIGNMENT);
        p4.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    	p4.add(testButton);
    	resultPanel.add(p4);
    	
    	//wrap the result area in a panel of its own to enforce left alignment
    	JPanel p5 = new JPanel();
        p5.setLayout(new BoxLayout(p5,
                               BoxLayout.PAGE_AXIS));
        p5.setAlignmentX(Component.LEFT_ALIGNMENT);
    	p5.add(resultZone);
    	resultPanel.add(p5);
    	  	
    	//--------------- Add both panels to the frame -------------------------
        converterFrame.setContentPane(splitPane);
    	
    	// Show the converter.
    	converterFrame.setLocation(300,150);
    	converterFrame.pack();
    	converterFrame.setVisible(true);
    }
    
    
    // Implementation of ActionListener interface.
    public void actionPerformed(ActionEvent event) {
    	if (event.getActionCommand()=="SELECT") {
    		//do nothing for simple selections
    		return;
    	}
    	try {
    		doc.remove(0,doc.getLength());
    		String regex = (String)regexField.getSelectedItem();
    		String target = targetField.getText();
    		if (regex==null || regex.length()<=0)
    			doc.insertString(0,"ERROR! Regular expression is empty!",doc.getStyle("regular"));
    		else {
    			if (target==null || target.length()<=0)
    				doc.insertString(0,"ERROR! Target string (to be matched) is empty!",doc.getStyle("regular"));
    			else {
    				//debug: insertText(event.paramString()+"\n");

    	    		//check if the selection is already in combo and if so just put it on top of list
    	    		for (int i=1;i<regexField.getItemCount();i++) //skip the element already on top
    	    			if (regex.equals((String)regexField.getItemAt(i))) {
    	    				regexField.removeItemAt(i);
    	    				//insertText("MATCHED AT["+i+"]\n");
    	    			}
    				//update history drop down
    	    		if (! regex.equals((String)regexField.getItemAt(0))) regexField.insertItemAt(regex,0);
    	    		regexField.setSelectedIndex(0);
    				//truncate history to 20 items max
    				if (regexField.getItemCount()>20) regexField.removeItemAt(regexField.getItemCount()-1);
    				//enforce CR LF handling
    				StringBuffer newTarget = new StringBuffer(target.length()*2);
    				for (int i=0;i<target.length();i++)
    				{
    					if (target.charAt(i)=='\r') continue;
    					if (target.charAt(i)=='\n') {
    						if (crlf.isSelected()) {
    							newTarget.append("\r\n");
    							continue; }
    						if (lfonly.isSelected()) {
    							newTarget.append("\n");
    							continue; }
    						if (nocrlf.isSelected())
    							continue;
    					}
    					newTarget.append(target.charAt(i));
    				}
    				target = newTarget.toString();
    				
    				regexTest(regex,target);
    			}
    		}
    	} catch (Exception e) {
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		baos.reset();
    		PrintStream ps = new PrintStream(baos);
    		e.printStackTrace(ps);
    		ps.flush(); ps.close();
    		try {
				baos.close();
				insertRedText(baos.toString());
			} catch (Exception e1) {
			}
    	}
    }

    // main method
    public static void main(String[] args) {
	// Set the look and feel.
	try {
	    UIManager.setLookAndFeel(
		UIManager.getCrossPlatformLookAndFeelClassName());
	} catch(Exception e) {}
    	
	new RegexCheck();
	
    }
   
    private void insertText(String text) throws BadLocationException {
    	doc.insertString(doc.getLength(),text,doc.getStyle("regular"));
    }
    private void insertRedText(String text) throws BadLocationException {
    	doc.insertString(doc.getLength(),text,doc.getStyle("red"));
    }
    private void insertComputerText(String text) throws BadLocationException {
    	doc.insertString(doc.getLength(),text,doc.getStyle("computer"));
    }
    private void insertTitleText(String text) throws BadLocationException {
    	doc.insertString(doc.getLength(),text,doc.getStyle("title"));
    }
	
    void regexTest(String regex, String target) throws BadLocationException, ParserException {
    	// Test Comment
    	//StringBuffer bufOut = new StringBuffer(500);    	
    	Pattern pattern = Pattern.compile(regex);
    	Matcher matcher = pattern.matcher(target);
    	
    	boolean found = false;
    	int gn ;
    	insertTitleText("Raw results:\n");
    	insertText("The regex has ");
    	insertComputerText(String.valueOf(matcher.groupCount()));
    	insertText(" capturing group(s)\n");
    	insertText("Entire string match yields ");
    	insertComputerText(String.valueOf(matcher.matches())+"\n");
    	matcher.reset();
    	while (matcher.find()) {
    		//System.out.println("\nmatcher.group is " + matcher.group());
    		insertText("\n");
    		for(gn = 0; gn <= matcher.groupCount();gn++){
    			
    			if (gn>=1) insertText("   ");
    			insertText("Grp");
    	    	insertComputerText(String.valueOf(gn));
    	    	if (gn==0) insertText("(whole regex)");
    	    	insertText(": matching [" );
    	    	insertComputerText(matcher.group(gn));
    	    	insertText("] from start");
    	    	insertComputerText(" "+matcher.start(gn));
    	    	insertText(" to end");
    	    	insertComputerText(" "+matcher.end(gn) );
    	    	insertText(".\n");
    			found = true;
    		}
    	}
    	if(!found){
    		insertText("No match found.\n");
    	}
    	insertText("\n");    	
    	insertTitleText("Within the RX Parser, we get:\n");
    	
    	insertText("  [");
    	insertComputerText(String.valueOf(matcher.matches()));
    	insertText("]in Data validation or Condition verification\n");
    	matcher.reset();
    	if (found) {
    		StringBuffer sb = new StringBuffer(target);
    		Parser parser = new Parser();
    		parser.extractCompositeValue(sb, pattern);
    		insertText("  [");
        	insertComputerText(sb.toString());
        	insertText("] as extracted Data Value or Condition Feed\n");
    	}
    	
    	matcher.reset();
    	
    	insertText("  [");
    	insertComputerText(String.valueOf(matcher.find()));
    	insertText("] in Group or Segment identification\n");
    	if (found) {
    		insertTitleText("Segmented elements (cuts...):\n" );insertText("  ");
    		//find a non conflicting regex delimiter
    		String delim = "\"";
    		if (regex.indexOf(delim)>=0) { 
    			delim = "¤";
    			if (regex.indexOf(delim)>=0) { 
    				delim = "~"; 
    				if (regex.indexOf(delim)>=0) { 
    					delim = "§"; 
    					if (regex.indexOf(delim)>=0) { 
    						delim = "%"; 
    						if (regex.indexOf(delim)>=0) { 
    							delim = "`"; 
    							if (regex.indexOf(delim)>=0) { 
    								delim = "°"; 
    							}
    						}
    					}
    				}
    			}
    		}
    		
    		SD segDef = new SD(0,"|SEG \".\" TEST M 1 1 ACC 1 T F \"test\" CUT "+delim+regex+delim,new HashMap());
    		segDef.cut(target);
    		while (segDef.hasNext()) {
    			String s = segDef.getNext(); //there's a possibility of optional capturing groups returning null string pieces
    			if (s==null) continue;
    			insertText("[");
    	    	insertComputerText(s);
    	    	insertText("] ");
    		}
    	}            
    	insertText("\n");
    }

}

