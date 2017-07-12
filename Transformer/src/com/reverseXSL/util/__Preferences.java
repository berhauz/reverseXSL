package com.reverseXSL.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.BackingStoreException;

import com.reverseXSL.parser.Parser;


/**
 * legacy class managing preferences and licences in a backing store
 * @author bernardH
 * @deprecated
 */
public class __Preferences {
	
	final static String ANONYMOUS = "anonymous";
	final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	final static String thisRelease = "9410A03010000001"; //YMDD protection mode A Vers03 Rel01 serial0000001 (matching 16 hex digits)

	

	static String licencee;

	static String date;

	static String release;
	
	static String registeredNamespace;
	
	static String[] signatures;

	
	public static void main(String[] args) throws Exception {
		
		setPreferences();
		
		//removePreferences();
		
		getPreferences();
		
		Parser p = new Parser();
		p.getClass();
		java.util.prefs.Preferences prefs = java.util.prefs.Preferences.systemRoot().node("com.reverseXSL.transform");
		boolean freeSwMode = prefs.get("ls","F").equalsIgnoreCase("F");
		if (freeSwMode) System.err.println("------------ ffFRRRReeeee software mode ----------------");
		else System.out.println("############## LICENCED software mode ##############");
	}
	
	static void getPreferences() {
		java.util.prefs.Preferences prefs = java.util.prefs.Preferences.systemRoot().node("com.reverseXSL.transform");
		licencee = prefs.get("licensee",ANONYMOUS);
		date = prefs.get("date",dateFormat.format(new Date()));
		release = prefs.get("release",thisRelease);
		registeredNamespace = prefs.get("namespace","http://www.reverseXSL.com/FreeParser");
		//registered namespace signatures further mixed with licensee data!
		signatures = new String[] { 
				prefs.get("signature0","0000.0000.0000.0000"),
				prefs.get("signature1","0000.0000.0000.0000"),
				prefs.get("signature2","0000.0000.0000.0000"),
				prefs.get("signature3","0000.0000.0000.0000"),
				prefs.get("signature4","0000.0000.0000.0000"),
				prefs.get("signature5","0000.0000.0000.0000"),
				prefs.get("signature6","0000.0000.0000.0000"),
				prefs.get("signature7","0000.0000.0000.0000"),
				prefs.get("signature8","0000.0000.0000.0000"),
				prefs.get("signature9","0000.0000.0000.0000") };
		System.out.println("licensee: "+licencee);
		System.out.println("date    : "+date);
		System.out.println("release : "+release);
		System.out.println("registNs: "+registeredNamespace);
		for (int i=0;i<signatures.length;i++) System.out.println("signatures["+i+"]: "+signatures[i]);

	}
	
	static void setPreferences() {
		java.util.prefs.Preferences prefs = java.util.prefs.Preferences.systemRoot().node("com.reverseXSL.transform");
		prefs.put("date","2009-04-21 14:00:00 +0200");
		prefs.put("licensee","Hello World");
		prefs.put("release","9421A03010000001");
		prefs.put("namespace","http://www.HelloWorld.com"); //is the registered namespace
		//LICENCEE-BASED LICENCE FOR REGISTERED [http://www.HelloWorld.com] is [VUZ2.TIC4.SLVK.WQTI]
		signatures = new String[] {"VUZ2.TIC4.SLVK.WQTI","SLQ9.IKVU.2BDE.HA7F","5NQ9.2BSL.BAGG.7FSL","0000.0000.0000.AAAA",
								   "LU2B.IKTI.2BSL.K35N","VU5N.IKEJ.DETI.5NEJ","TI7F.GGMA.SLLU.RXQ9","0000.0000.0000.AAAA",
								   "IKEJ.WEBA.WETI.GG2B","0000.0000.ZZZZ.0000" };
		for (int i=0;i<signatures.length;i++) prefs.put("signature"+i,signatures[i]);
		try {
			prefs.flush();
		} catch (BackingStoreException e1) {
			System.err.println("backing store not in order! "+e1);
		}

	}
	
	static void removePreferences() {
		java.util.prefs.Preferences prefs = java.util.prefs.Preferences.systemRoot().node("com.reverseXSL.transform");
		try {
			prefs.removeNode();
		} catch (BackingStoreException e) {
			System.err.println("backing store not in order! "+e);
		} 
		System.err.println("!!!!!!!!REMOVED!!!!!!!");
	}
	
}
