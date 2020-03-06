/** 
 * BatchXSLT.java
 *
 * Title:			BatchXSLT Batch XSL Transformation
 * Description:	
 * @author			Andreas Imhof
 * @version			
 */

package com.epaperarchives.batchxslt;

import java.io.*;
import javax.swing.*;


public class BatchXSLT {

	public static SecurityManager mySecMan = null;
	public static mainXSLTFrame g_mainXSLTFrame = null;
	public static BatchXSLTransform g_BatchXSLTransform = null;
	private Thread myclockThread = null;

	//Specify the look and feel to use.  Valid values:
    //null (use the default), "Metal", "System", "Motif", "GTK+"
	final static String LOOKANDFEEL = null;

	public BatchXSLT(String[] args) {
		// --------------------------------------------------
		//add a shutdown hook for final cleanup on exit
		Runtime.getRuntime().addShutdownHook(new Thread(){
                                             @Override
											 public void run(){
												Thread.currentThread().setName("doShutdownTask");
												doShutdownTask();
											 }});

		// --------------------------------------------------
		// init the main mainXSLTFrame class
		try {
			//initLookAndFeel();

			g_mainXSLTFrame = new mainXSLTFrame();
			g_mainXSLTFrame.early_initXSLT();
			g_mainXSLTFrame.initComponents();
			//g_mainXSLTFrame.setVisible(true); now done in initComponents
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// --------------------------------------------------
		// we set up our security manager
		mySecMan = System.getSecurityManager();




		// --------------------------------------------------
		// init the Transform class
		try {
			g_BatchXSLTransform = new BatchXSLTransform();
		}
		catch (Exception x) {
			BatchXSLT.g_mainXSLTFrame.showRunMess( "##### ERROR: Exception at 'BatchXSLTransform'");
			x.printStackTrace();
		}

		try {	// do this as thread because noclassdeffound error may happen and terminate this thread
			//environement.getEnviron();
			Thread getenvir_thread = new environement();
			getenvir_thread.start();
		}
		catch (Exception x) {
			System.out.println( "Could not determin the Version of Xalan and Xerces");
			x.printStackTrace();
		}

		try {
			g_mainXSLTFrame.initMessages();
		}
		catch (Exception x) {
			BatchXSLT.g_mainXSLTFrame.showRunMess( "##### ERROR: Exception at 'initMessageContainer'");
			x.printStackTrace();
		}
		try {
			g_mainXSLTFrame.initXSLT(args);
		}
		catch (Exception x) {
			BatchXSLT.g_mainXSLTFrame.showRunMess( "##### ERROR: Exception at 'initXSLT'");
			x.printStackTrace();
		}

		// call our main engine thread
		try {
			BatchXSLT.g_mainXSLTFrame.mainLoop(1,args);
		}
		catch (Exception e) {
			BatchXSLT.g_mainXSLTFrame.showRunMess( "##### ERROR: Exception at 'mainLoop'");
			e.printStackTrace();
		}

		// clean up memory and other stuff - please!
		BatchXSLTransform.cleanUpMemory();

		// start the time and memory display thread
		try {
			myclockThread = new ClockThread();
			myclockThread.start();
		}
		catch (Exception e) {}
		
	}

	/**
	 * Set the look and feel of the user admin window.
	 */
    private void initLookAndFeel() {
        String lookAndFeel = null;
        if (LOOKANDFEEL == null) return;

		if (LOOKANDFEEL.equals("Metal")) {
			lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
		} else if (LOOKANDFEEL.equals("System")) {
			lookAndFeel = UIManager.getSystemLookAndFeelClassName();
		} else if (LOOKANDFEEL.equals("Motif")) {
			lookAndFeel = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
		} else if (LOOKANDFEEL.equals("GTK+")) { //new in 1.4.2
			lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
		} else {
			System.err.println("Unexpected value of LOOKANDFEEL specified: " + LOOKANDFEEL);
			lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
		}

		try {
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (ClassNotFoundException e) {
			System.err.println("Couldn't find class for specified look and feel:" + lookAndFeel);
			System.err.println("Did you include the L&F library in the class path?");
			System.err.println("Using the default look and feel.");
		} catch (UnsupportedLookAndFeelException e) {
			System.err.println("Can't use the specified look and feel (" + lookAndFeel + ") on this platform.");
			System.err.println("Using the default look and feel.");
		} catch (Exception e) {
			System.err.println("Couldn't get specified look and feel (" + lookAndFeel + "), for some reason.");
			System.err.println("Using the default look and feel.");
			e.printStackTrace();
		}
    }

	// Main entry point
	static public void main(String[] args) {
		try {
			new BatchXSLT(args);
		} catch (Exception e) {
			System.err.println("##### ERROR: Exception starting Transformer at main()");
			e.printStackTrace();
		}
	}
	

	
	//+++++++++++++++ CLEAN UP Thread before exit
	private void doShutdownTask() {
		// remove the file 'batchxsltisrunning' if exists in path ~/BatchXSLT4InDesign/BatchXSLT/
		try {
			String pathname = BatchXSLT.g_mainXSLTFrame.nextJobTicketPath;
			if (pathname.endsWith(File.separator) == false) pathname += File.separator;
				pathname += BatchXSLT.g_mainXSLTFrame.runningLockFileName;
				
				//System.out.println("running file: " + pathname); 
				File f = new File(pathname);
				if (f.exists()) {	// if exists then delete it because we are done
					f.delete();
				}
		} catch (Exception excpt) {}
	}
	
}
