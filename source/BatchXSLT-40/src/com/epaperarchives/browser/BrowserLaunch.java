/**
 * my simple Browser Launcher
 * derived from BareBones BrowserLaunch December 10, 2005
 * Version 1.51 
 * 20140220
 * For: Mac OS X, GNU/Linux, Unix, Windows
 * Example Usage:
 * String url = "http://www.epaperarchives.com/";
 * BrowserLaunch.openURL(url);
 */

package com.epaperarchives.browser;


import java.lang.reflect.Method;
import java.awt.Desktop;
import java.net.URI;
import java.io.File;


public class BrowserLaunch {
	public BrowserLaunch() {
	}

	static final String[] browsers = { "firefox", "opera", "safari", "konqueror", "epiphany", "seamonkey", "galeon", "kazehakase", "mozilla", "netscape" }; 
	private static final String errMsg = "Sorry, could not launch your standard web browser";
	private static final String errMsg2 = "Sorry, could not open  file";
	
	public static void openURL(String url) {
        try {
           Desktop.getDesktop().browse(new URI(url));
           return;
        }
        catch (Exception ex) {
            try {
               Desktop.getDesktop().open(new File(url));
               return;
            }
            catch (Exception ex1) {
                com.epaperarchives.batchxslt.utils.showMess(errMsg2 + ":\n" + ex1.getLocalizedMessage() + "\n");
            }
        }
		String osName = System.getProperty("os.name");
		try {
			if (osName.startsWith("Mac OS")) {
				Class fileMgr = Class.forName("com.apple.eio.FileManager");
				Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});
				openURL.invoke(null, new Object[] {url});
			}
			else {
				if (osName.startsWith("Windows")) Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
				else { //assume Unix or Linux
					String browser = null;
					for (int count = 0; count < browsers.length && browser == null; count++) {
						if (Runtime.getRuntime().exec(new String[] {"which", browsers[count]}).waitFor() == 0) browser = browsers[count];
					}
					if (browser == null) throw new Exception("Could not find web browser");
					else Runtime.getRuntime().exec(new String[] {browser, url});
				}
			}
		}
		catch (Exception ex) {
			com.epaperarchives.batchxslt.utils.showMess(errMsg + ":\n" + ex.getLocalizedMessage() + "\n");
		}
	}

}

