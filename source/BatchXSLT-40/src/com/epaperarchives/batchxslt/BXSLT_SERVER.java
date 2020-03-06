/*
 This file to be included in BatchXSLT_OSX_SERVER Target
 Will enable the BSH code in utils.java
 
 ********** IMPORTANT:
 uncomment the code in BatchXSLTransform.java at the title
 comment for NORMAL version and uncomment for SERVER version
	1. at section
		* start REMOTE BSH Interpreter Server
		mentioned between
		and 
		END comment for NORMAL version and uncomment for SERVER version
 
	2. at section
		* start REMOTE BSH Interpreter Server
*/

package com.epaperarchives.batchxslt;

import java.io.*;

import bsh.Interpreter;

public class BXSLT_SERVER
{
	private static final String PACKAGE_NAME = "com.epaperarchives.batchxslt.BXSLT_SERVER";
	public BXSLT_SERVER() {
	}
	/******************************
	 *
	 * bsh Interpreter stuff
	 */
	/******************************
	 *
	 * bsh Interprete a source file
	 */
	public static int BSH_InterpreteSource( String pathname) {
		if (pathname.equals("") == true) return(-1);	// no java source file
		
		Interpreter ipret = new Interpreter();
		try {
			ipret.source(pathname);
		}
		catch (FileNotFoundException fnfe) {
			return(-2);
		}
		catch (IOException ioe) {
			return(-3);
		}
		catch ( bsh.TargetError se ) { // The script threw an exception
			Throwable t = se.getTarget();
			System.out.println( "BSH ERROR -4. Script file threw exception: " + t);
			return(-4);
		}
		catch (bsh.EvalError eve) {
			System.out.println( "BSH ERROR -5. Exception: " + eve.getErrorText());
			return(-5);
		}
		return(0);
	}
	/******************************
	 *
	 * bsh Interprete a string
	 */
	public static int BSH_InterpreteString( String src) {
		if (src.equals("") == true) return(-1);	// nothing to interprete
		
		Interpreter ipret = new Interpreter();
		try {
			ipret.eval(src);
		}
		catch ( bsh.TargetError se ) { // The script threw an exception
			Throwable t = se.getTarget();
			System.out.println( "BSH ERROR -4. Script string threw exception: " + t);
			se.printStackTrace();
			return(-4);
		}
		catch (bsh.EvalError eve) {
			System.out.println( "BSH ERROR -5. Exception: " + eve.getErrorText());
			eve.printStackTrace();
			return(-5);
		}
		return(0);
	}
	/******************************
	 *
	 * bsh start remote session Interpreter
	 */
	public static int BSH_remote( String portstr) {
		if (portstr.equals("") == true) return(-1);	// nothing to start
		
		Interpreter ipret = new Interpreter();
		try {
			ipret.set( "BatchXSLTServer", "utils" );  // Provide a reference to your app
		}
		catch (bsh.EvalError eve2) {
			System.out.println( "BSH_Remote ERROR -2. Exception: " + eve2.getErrorText());
			eve2.printStackTrace();
			return(-2);
		}

		int port = Integer.valueOf(portstr);
		try {
			ipret.set( "portnum", port );  
		}
		catch (bsh.EvalError eve3) {
			System.out.println( "BSH_Remote ERROR -3. Exception: " + eve3.getErrorText());
			eve3.printStackTrace();
			return(-3);
		}
		try {
			ipret.eval("setAccessibility(true)"); // turn off access restrictions
		}
		catch (bsh.EvalError eve4) {
			System.out.println( "BSH_Remote ERROR -4. Exception: " + eve4.getErrorText());
			eve4.printStackTrace();
			return(-4);
		}
			
		try {
			ipret.eval("server(portnum)");
		}
		catch (bsh.EvalError eve5) {
			System.out.println( "BSH_Remote ERROR -4. Exception: " + eve5.getErrorText());
			eve5.printStackTrace();
			return(-5);
		}
		// System.out.println("*** BSH_Remote HTTP startet on port  " + port);
		BatchXSLT.g_mainXSLTFrame.showMess( "*** BSH_Remote HTTP startet on port  " + port + "\n");

		return(0);
	}
}
