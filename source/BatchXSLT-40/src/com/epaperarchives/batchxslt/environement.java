
package com.epaperarchives.batchxslt;

import javax.swing.SwingUtilities;

// Imported java classes
public class environement extends Thread
{

	public environement() {
	}

	public void run() {
		Thread.currentThread().setName("environement");
		int curpri = getPriority();
		setPriority(curpri+1);
		for (int i = 0; i< 10; i++) {
			getEnviron();
			if ( (BatchXSLT.g_mainXSLTFrame.version_xalan2x.equals("?") == false) && (BatchXSLT.g_mainXSLTFrame.version_xerces.equals("!?") == false)
				&& (BatchXSLT.g_mainXSLTFrame.version_xerces.equals("?") == false) && (BatchXSLT.g_mainXSLTFrame.version_xerces.equals("!?") == false) ) break;
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
	}
  
	public static void run_getEnviron() {
        Runnable dogetEnviron = new Runnable() {
            public void run() {
				Thread.currentThread().setName("dogetEnviron");
            	do_getEnviron();
            }
        };
        //SwingUtilities.invokeLater(doinitXSLT);
        try { SwingUtilities.invokeAndWait(dogetEnviron); }
		catch (Exception e1) { }
	}


	static Object do_getEnviron() {
		getEnviron();
		return(null);
	}


	public static void getEnviron() {
		try {
			BatchXSLT.g_mainXSLTFrame.version_xalan2x = org.apache.xalan.Version.getVersion();
		}
		catch (Exception e) {
			BatchXSLT.g_mainXSLTFrame.version_xalan2x = "!?";
		}
		try {
			BatchXSLT.g_mainXSLTFrame.version_xerces = org.apache.xerces.impl.Version.getVersion();
		}
		catch (Exception e) {
			BatchXSLT.g_mainXSLTFrame.version_xalan2x = "!?";
		}
		if (!BatchXSLT.g_mainXSLTFrame.version_xalan2x.equals("?") && !BatchXSLT.g_mainXSLTFrame.version_xerces.equals("?")) BatchXSLT.g_mainXSLTFrame.environmentOK = true;
		/*
		try {
			EnvironmentCheck environmentCheck = new EnvironmentCheck();
			Hashtable environmentHash = environmentCheck.getEnvironmentHash();
			Enumeration enumeration = environmentHash.keys();

			while(enumeration.hasMoreElements()){
				Object hobject = enumeration.nextElement();
				//System.out.println("Key Name :"+object);
				//System.out.println("Key value :"+environmentHash.get(object));
				String key = hobject.toString();
				if (key.indexOf("xerces1") >= 0) {
					version_xerces1 = environmentHash.get(hobject).toString();
					continue;
				}
				if (key.indexOf("xerces") >= 0) {
					version_xerces = environmentHash.get(hobject).toString();
					continue;
				}
				if (key.indexOf("xalan2x") >= 0) {
					version_xalan2x = environmentHash.get(hobject).toString();
					continue;
				}
				if (key.indexOf("xalan1") >= 0) {
					version_xalan1 = environmentHash.get(hobject).toString();
					continue;
				}
				if (key.indexOf("DOM") >= 0) {
					version_DOM = environmentHash.get(hobject).toString();
					continue;
				}
			}
			environmentOK = true;
		}
		catch (Exception e) {}
*/
		return;
	}



}
