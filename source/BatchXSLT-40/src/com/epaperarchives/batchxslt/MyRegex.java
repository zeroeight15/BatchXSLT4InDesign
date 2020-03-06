
package com.epaperarchives.batchxslt;


import java.util.regex.*;








/**
 * Regex replace text
 */
public final class MyRegex {
/* 
    private static String REGEX = "dog";
    private static String INPUT = "The dog says meow. All dogs say meow.";
    private static String REPLACE = "cat";
*/ 
	public static String replace(String REGEX, String INPUT, String REPLACE) {
	
		if ( (INPUT==null) || (INPUT.equals("")) ) return(INPUT);
		if ( (REGEX==null) || (REGEX.equals("")) ) return(INPUT);
		if ( (REPLACE==null) ) return(INPUT);
		try {
			Pattern p = Pattern.compile(REGEX);
			Matcher m = p.matcher(INPUT); // get a matcher object
			INPUT = m.replaceAll(REPLACE);
		} catch (Exception e) {}
		// System.out.println(INPUT);
		return(INPUT);
	}
}





