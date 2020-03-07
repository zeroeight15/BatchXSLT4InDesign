/** 
 * utils.java
 *
 * Title:			Utilities
 * Description:	
 * @author			Andreas Imhof
 * @version			40.0
 * @versionDate		20200221
 */

package com.epaperarchives.batchxslt;

// Imported java classes
import com.ibm.icu.text.Normalizer;
import de.schlichtherle.NZip;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.soap.encoding.soapenc.Base64;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
/**
 * if you need Tika to extract meta data from files, then 
 * a: uncomment below line: import org.apache.tika.cli.TikaCLI;
 * b: search for the line: if (metaData == "") return 0;  //  if tika is not needed we always return nothing
 *    and comment the line As it will always return nothing
 * c: search for the line: TikaCLI.main(args);
 *    (this is the tika call) and uncomment it
 * d: add the library 'tika-app-1.9.jar' (downloadable from apache tika)
 *    - to the lib folder
 *    - to the project's compile time libraries properties window
 * e: make sure the manifest.mf file contains 'tika-app-1.9.jar' in the Class-Path: line
 * f: recompile
 */
//import org.apache.tika.cli.TikaCLI;

/********************************
 * various tools
 */
public class utils
{
    private static final String PACKAGE_NAME = "com.epaperarchives.batchxslt.utils";
	
	private static String errorResponse = "";
	private static String stdoutResponse = "";

	private static String lastFileExceptionMessage = "";
	private static int lastFileExceptionError = 0;

	public utils() {
	}


	/**
	* get last file Exception error code
     * @return an Integer an error code
	**/
	public static int getLastFileExceptionError() {
		return(lastFileExceptionError);
	}

	/**
	* get last file Exception message
     * @return String an error message
	**/
	public static String getLastFileExceptionMessage() {
		return(lastFileExceptionMessage);
	}

	/**
	* covert from decomposed Unicode to composed
     * @param decomposedStr a decomposed string
     * @return String the composed string
	**/
	public static String composeUnicodeNFC(String decomposedStr) {
		Normalizer normalizer = new	Normalizer(decomposedStr, Normalizer.NFC, Normalizer.UNICODE_3_2);
		String composedString = Normalizer.compose(decomposedStr,false);
		return(composedString);
	}
	public static String composeUnicodeNFKC(String decomposedStr) {
		Normalizer normalizer = new	Normalizer(decomposedStr, Normalizer.NFKC, Normalizer.UNICODE_3_2);
		String composedString = Normalizer.compose(decomposedStr,true);
		return(composedString);
	}
	public static String decomposeUnicodeNFD(String composedStr) {
		Normalizer normalizer = new	Normalizer(composedStr, Normalizer.NFD, Normalizer.UNICODE_3_2);
		String decomposedString = Normalizer.decompose(composedStr,false);
		return(decomposedString);
	}
	

        public static int callMethod(String methodString) {
            if ( (methodString == null) || (methodString.equals("") == true) ) return(-11);
            String[] methodArr = methodString.split(",");	// get all method commands: they are separated by ','
                                                // [0] -> class name
                                                // [1] -> method name
                                                // [...] -> parameters
            String classname = "";
            String methodname = "";
            String[] arguments = new String[methodArr.length - 2];
            int err = 0;
            int i;
            int j;
            Class classToCall;
            Method methodToExecute;
            do {
                if (methodArr.length < 2) {
                        err = -101;
                        break;
                }
                classname = methodArr[0];
                methodname = methodArr[1];
                //arguments = new String[methodArr.length - 2];

                for (i = 2, j = 0; i < methodArr.length; i++, j++) {
                    arguments[j] = methodArr[i];
                    //showMess("new argument[" + j + "]: "  + arguments[j] + "\n");
                }
                try {            
                    classToCall = Class.forName(classname);
                } catch (ClassNotFoundException ex) {
                    err = -102;
                    break;
                }

                try {            
                    methodToExecute = classToCall.getDeclaredMethod(methodname, new Class[]{String[].class});
                } catch (NoSuchMethodException ex) {
                    err = -103;
                    break;
                } catch (SecurityException ex) {
                    err = -104;
                    break;
                }

                try {
                    // deprecated: methodToExecute.invoke(classToCall.newInstance(), new Object[]{arguments});
                    methodToExecute.invoke(classToCall.getDeclaredConstructor().newInstance(), new Object[]{arguments});
                } catch (IllegalAccessException ex) {
                    err = -105;
                    break;
                } catch (IllegalArgumentException ex) {
                    err = -106;
                    break;
                } catch (InvocationTargetException ex) {
                    err = -107;
                    break;
                } catch (InstantiationException ex) {
                    err = -108;
                    break;
                } catch (NoSuchMethodException ex) {
                    err = -109;
                    break;
                }
            } while(false);
            /*
            showMess("methodString: " + methodString + "\n");
            showMess("classname: " + classname + "\n");
            showMess("methodname: " + methodname + "\n");
            for (i = 0; i < arguments.length; i++) {
                showMess("arguments[" + i + "]: "  + arguments[i] + "\n");
            }
            showMess("err: " + err + "\n");
            */
            return(err);
	}


	public static String callExternalAppGetErrorResponse() {
		return(errorResponse);
	}
	public static String callExternalAppGetStdinResponse() {	// legacy
		return(stdoutResponse);
	}
	public static String callExternalAppGetStdoutResponse() {
		return(stdoutResponse);
	}

	
	/**
	 * Call an external application
	 *
   * @param applString a string of comma separated values like the application and parameters. See below
   * @return an error code or zero
	 *<p>
	 * applString is the command to pass to the operating system. It is a comma separated string:<br>
	 * &nbsp;&nbsp;&nbsp;"app_path,params,envir,workdir,convertParamsToNativeCharset,waitComplete"
	 *<p>
	 * where:<br>
	 * app_path = full path/name to application to call<br>
	 * params = space separated list of parameters<br>
	 * envir = "+++" separated list of environment variables to set like &nbsp;&nbsp;&nbsp;path=mypath+++var1=myvarstring1+++var2=myvarstring2<br>
	 * workdir = path of working directory to set<br>
	 * convertParamsToNativeCharset = convert paths to native charset or not<br>
	 * waitComplete = "0" to NOT to wait for application completed, otherwise: wait for completion<br>
	 *<br>
	 * multiple application call commands can be defined separated by '/#/'
	 */
	public static int callExternalApplication(String applString) {
		if ( (applString == null) || (applString.equals("") == true) ) return(-11);
		String app_path = null;
		String params = null;
		String envir = null;
		String workdir = null;
		String convertParamsToNativeCharset = null;
		String waitComplete = null;
		String timeout = null;
		String[] theApplicationsArr;
		String[] myApplicationArr;
		int err = 0;
		int i;

		theApplicationsArr = applString.split("/#/");	// get all application commands: they are separated by '/#/'

		for (i = 0; i < theApplicationsArr.length; i++) {
			myApplicationArr = theApplicationsArr[i].split(",");
			/* results in:
				myApplicationArr[0] : app_path = full path/name to application to call
				myApplicationArr[1] : params = space separated list of parameters
				myApplicationArr[2] : envir = "+++" separated list of environment variables to set like path=mypath+++var1=myvarstring1+++var2=myvarstring2
				myApplicationArr[3] : workdir = path of working directory to set
				myApplicationArr[4] : convertParamsToNativeCharset = convert paths to native charset or not
				myApplicationArr[5] : waitComplete = "0"or null to NOT to wait for application completed, otherwise: wait for completion
			*/
			if (myApplicationArr[0].equals("") == true) return(-12);	// no application path given
			app_path = myApplicationArr[0];
			if ( (myApplicationArr.length >= 2) && (myApplicationArr[1].equals("") == false) ) params = myApplicationArr[1];
			if ( (myApplicationArr.length >= 3) && (myApplicationArr[2].equals("") == false) ) envir = myApplicationArr[2];
			if ( (myApplicationArr.length >= 4) && (myApplicationArr[3].equals("") == false) ) workdir = myApplicationArr[3];
			if ( (myApplicationArr.length >= 5) && (myApplicationArr[4].equals("") == false) ) convertParamsToNativeCharset = myApplicationArr[4];
			if ( (myApplicationArr.length >= 6) && (myApplicationArr[5].equals("") == false) ) waitComplete = myApplicationArr[5];
			if ( (myApplicationArr.length >= 7) && (myApplicationArr[6].equals("") == false) ) timeout = myApplicationArr[6];

			err = callExternalApp(app_path, params, envir, workdir, convertParamsToNativeCharset, waitComplete, timeout);
		}
		return(err);
	}

	public static int callExternalApp(String app_path) {
		return(callExternalApp(app_path, null, null, null, null, "1", "-1"));
	}
	public static int callExternalApp(String app_path, String params) {
		return(callExternalApp(app_path, params, null, null, null, "1", "-1"));
	}
	public static int callExternalApp(String app_path, String params, String envir) {
		return(callExternalApp(app_path, params, envir, null, null, "1", "-1"));
	}
	public static int callExternalApp(String app_path, String params, String envir, String workdir) {
		return(callExternalApp(app_path, params, envir, workdir, null, "1", "-1"));
	}
	public static int callExternalApp(String app_path, String params, String envir, String workdir, String convertParamsToNativeCharset) {
		return(callExternalApp(app_path, params, envir, workdir, convertParamsToNativeCharset, "1", "-1"));
	}

	/**
	* Call an external application
	*
	* @param app_path String full path/name to application to call
	* @param params   String space separated list of parameters or <code>null<code>
	* @param envir    String "+++" separated list of environment variables to set  or <code>null<code>. String like path=mypath+++var1=myvarstring1+++var2=myvarstring2
	* @param workdir  String path of working directory to set or <code>null<code>
	* @param convertParamsToNativeCharset String "1" or "0" to convert paths to native charset or not
	* @param waitComplete String "0" to NOT to wait for application completed, otherwise: wait for completion
	* @param timeout String in milliseconds. "1000" is 1 second
	* @return int an error code or 0 for no error<br>
	*<pre>
	*     -2 no path specified
	*     -3 error calling application
	*     otherwise error code returned by called application
	*<pre>
	**********/
	public static int callExternalApp(String app_path, String params, String envir, String workdir, String convertParamsToNativeCharset, String waitComplete, String timeout) {
	    if ( (app_path == null) || (app_path.equals("") == true) ) return(-2);

	    int exitVal = -1;
	    String[] myparams = null;
	    int myparams_length = 0;
	    String[] myenvir = null;
	    File myworkdir = null;
	    String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
			System.out.println("***********************");
			System.out.println("*** Default Charset=" + Charset.defaultCharset());
			System.out.println("*** Default file Charset=" + BatchXSLT.g_mainXSLTFrame.systemDefaultCharset);
			System.out.println("*** callExternalApp app_path: '" + app_path + "'");
			System.out.println("*** callExternalApp params: '" + params + "'");
			System.out.println("*** callExternalApp envir: '" + envir + "'");
			System.out.println("*** callExternalApp workdir: '" + workdir + "'");
			System.out.println("*** callExternalApp convertParamsToNativeCharset: '" + convertParamsToNativeCharset + "'");
			System.out.println("*** callExternalApp params HEX: '" + BatchXSLTransform.stringUTF8_2hex(params) + "'");
		}

		// make sure we have no empty strings in parameters
		String myparamsstr = params;
		if ( (myparamsstr != null) && (myparamsstr.equals("") == false) ) {
			while (myparamsstr.indexOf("  ") > 0) {
				myparamsstr = myparamsstr.replaceAll("  "," ");	// replace double spaces with one only
			}
			myparamsstr = myparamsstr.trim();	// remove leading and trailing whitespace
			if (myparamsstr.equals("") == true) myparamsstr = null;
		}
		if ( (myparamsstr != null) && (myparamsstr.equals("") == false) ) {
			myparams = myparamsstr.split(" ");
			if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
				System.out.println("*** callExternalApp myparams arr: '" + Arrays.toString(myparams) + "'");
			}
			for (int i = 0; i <  myparams.length; i++) {
				myparams[i] = myparams[i].replaceAll("~blnk~", " ");
				if (myparams[i].startsWith("~") == true) myparams[i] = home + myparams[i].substring(2);	// cut leading path ~/
			}
			if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
				System.out.println("*** callExternalApp myparams cleaned arr: '" + Arrays.toString(myparams) + "'");
			}

			do {	// convertParamsToNativeCharset may be [1][2][1-w][2-w]
				if ( (convertParamsToNativeCharset == null) || (convertParamsToNativeCharset.length() <= 0) ) break;

				if (convertParamsToNativeCharset.indexOf("1") >= 0) {	//  make composed unicode
					do {
						if (  (convertParamsToNativeCharset.indexOf("1-w") >= 0)	// no conversion of params characters on windows
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") >= 0) ) break;
						if (  (convertParamsToNativeCharset.indexOf("1-x") >= 0)	// no conversion of params characters on OSX
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") < 0) ) break;
						for (int i = 0; i <  myparams.length; i++) {	// make composed unicode
							myparams[i] = composeUnicodeNFC(myparams[i]);
						}
					} while (false);
				}
				if (convertParamsToNativeCharset.indexOf("3") >= 0) {	//  make decomposed unicode
					do {
						if (  (convertParamsToNativeCharset.indexOf("3-w") >= 0)	// no conversion of params characters on windows
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") >= 0) ) break;
						if (  (convertParamsToNativeCharset.indexOf("3-x") >= 0)	// no conversion of params characters on OSX
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") < 0) ) break;
						for (int i = 0; i <  myparams.length; i++) {	// make composed unicode
							myparams[i] = decomposeUnicodeNFD(myparams[i]);
						}
					} while (false);
				}
				
				if (convertParamsToNativeCharset.indexOf("2") >= 0) {	//  convert UTF-8 to local charset
					do {
						if (  (convertParamsToNativeCharset.indexOf("2-w") >= 0)	// no conversion of params characters on windows
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") >= 0) ) break;
						if (  (convertParamsToNativeCharset.indexOf("2-x") >= 0)	// no conversion of params characters on OSX
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") < 0) ) break;
						for (int i = 0; i <  myparams.length; i++) {
							myparams[i] = convertStringFromUTF8toSystemNative(myparams[i]);
						}
					} while (false);
				}
				
				if (convertParamsToNativeCharset.indexOf("4") >= 0) {	//  convert UTF8 to UTF16
					do {
						if (  (convertParamsToNativeCharset.indexOf("4-w") >= 0)	// no conversion of params characters on windows
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") >= 0) ) break;
						if (  (convertParamsToNativeCharset.indexOf("4-x") >= 0)	// no conversion of params characters on OSX
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") < 0) ) break;
						for (int i = 0; i <  myparams.length; i++) {
							myparams[i] = convertStringFromUTF8toUTF16LE(myparams[i]);
						}
					} while (false);
				}
				
				if (convertParamsToNativeCharset.indexOf("5") >= 0) {	//  convert UTF 16 to local charset
					do {
						if (  (convertParamsToNativeCharset.indexOf("5-w") >= 0)	// no conversion of params characters on windows
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") >= 0) ) break;
						if (  (convertParamsToNativeCharset.indexOf("5-x") >= 0)	// no conversion of params characters on OSX
							&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") < 0) ) break;
						for (int i = 0; i <  myparams.length; i++) {
							myparams[i] = convertStringFromUTF16toSystemNative(myparams[i]);
						}
					} while (false);
				}
			} while (false);
			myparams_length = myparams.length;
		}

		if ( (envir != null) && (envir.equals("") == false) ) {
			myenvir = envir.split("\\+\\+\\+");
		}

		if ( (workdir != null) && (workdir.equals("") == false) ) {
			String strworkdir = workdir.replaceAll("~blnk~", " ");
			if (strworkdir.startsWith("~") == true) strworkdir = home + strworkdir.substring(2);	// cut leading path ~/
			myworkdir = new File(strworkdir);
		}

		String[] cmd = new String[myparams_length + 1];	// set up the command to send to the OS to call app

		cmd[0] = app_path;		// into array element [0]
		cmd[0] = cmd[0].replaceAll("~blnk~", " ");
		if (cmd[0].startsWith("~") == true) cmd[0] = home + cmd[0].substring(2);	// cut leading path ~/

		for (int i = 0; i < myparams_length; i++) {
			//System.out.println("call param#" + i + " : " + myparams[i]);
			cmd[i+1] = myparams[i];
		}

//		System.out.println("*** callExternalApp with cmd: '" + Arrays.toString(cmd) + "'");
//		if (myenvir != null) System.out.println("        envir: '" + Arrays.toString(myenvir) + "'");

		try {            
                    Runtime rt = Runtime.getRuntime();
                    final Process proc;

                    if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
                            System.out.println("---------------------------------------------");
                            System.out.println("Program call: '" + Arrays.toString(cmd));
                    }
                    ProcessBuilder pb = new ProcessBuilder(cmd);		// this must be a List of strings! a single string commnad -params does not work!
                    if (envir != null) {
                            if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
                                    System.out.println("IMAGE MAGICK environment variables:");
                            }
                            Map<String, String> environment = pb.environment();
                            for (int i = 0; i < myenvir.length; i++) {
                                    String[] key_val = myenvir[i].split("=");
                                    environment.put(key_val[0], key_val[1]);
                                    if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
                                            System.out.println(key_val[0] + "=" + key_val[1] + "'");
                                    }
                            }
                    }
                    pb.directory(myworkdir);
                    pb.redirectErrorStream(true);	// merge STDOUT into STDERR

                    proc = pb.start();


                    // any (error) message?
                    StreamGobbler inGobbler = new StreamGobbler(proc.getInputStream(), "STDOUT", false);
                    StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", false);
                    // dump (errors) to console
                    inGobbler.start();
                    errorGobbler.start();

                    // this will kill the process after a timeout (if it's not finished yet).
                    Timer killtimer = null;
                    Long the_timeout = BatchXSLT.g_mainXSLTFrame.externalProcessTimeout;
                    if ((timeout.equals("-1") == false) && (timeout.equals("") == false)) the_timeout = Integer.valueOf(timeout).longValue();
                    if (the_timeout > 0) {
                            killtimer = new Timer();
                            killtimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                            proc.destroy();
                                            System.out.println("### External application destroyed after timeout!");
                                    }
                            }, the_timeout);
                    }
                    // wait for completion if waitComplete is not '0'
                    if ( (waitComplete == null) || (waitComplete.equals("") == true) || (waitComplete.indexOf("0") < 0) ) {
                            exitVal = proc.waitFor();
                    }
                    // we have to cnacle the killtimer
                    if (killtimer != null) {
                            killtimer.cancel();
                    }
                    // any error???
                    stdoutResponse = inGobbler.getResponse();	// after done get response streams as strings
                    errorResponse = errorGobbler.getResponse();
                    if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
                            System.out.println("*** ExternalApp response: " + stdoutResponse);
                            // is merged int STDOUT      System.out.println("*** ExternalApp err: " + errorResponse);
                    }
                    if (exitVal != 0) {
                            System.out.println("### External application ERROR " + exitVal + " in command:\n" + pb.command() + "\n   " + errorResponse);
                    }
            }
            catch (Throwable t) {
                    t.printStackTrace();
                    return(-3);
            }
            return(exitVal);
	}



	// remeber the last converted file
	private static String last_fileBynaryReplace_file = "";	// this is != "" when we have created a pdf file with CropBox comment
	public static String getname_last_fileBynaryReplace_file() {
		return ( last_fileBynaryReplace_file );
	}
	public static boolean delete_last_fileBynaryReplace_file() {
		if (last_fileBynaryReplace_file.equals("") == true) return(false);
		boolean deleted = deleteFile(last_fileBynaryReplace_file);
		last_fileBynaryReplace_file = "";
		return ( deleted );
	}
	public static void clear_last_fileBynaryReplace_file() {
		last_fileBynaryReplace_file = "";
		return;
	}
	// find and replace bytes in a file
	// returns 0 if no error
	public static int fileBynaryReplace(String infilepath, String outfilepath, String find, String replace) {
		last_fileBynaryReplace_file = "";
		if ( (infilepath == null) || (infilepath.equals("") == true) ) return(-1);
		if ( (find == null) || (find.equals("") == true) ) return(-2);
		
		String my_infilepath = infilepath;
		String my_outfilepath = outfilepath;
		if (my_outfilepath.equals("") == true) {
//System.out.println("wwwwwwwwwwwwwwwww my_infilepath: " + my_infilepath);
			String splitter = File.separator;
			if (splitter.equals("\\") == true) splitter = "\\\\";	// make it two backslashes
//System.out.println("wwwwwwwwwwwwwwwww splitter: " + splitter);
			String[] mypaths = null;
			try {
				mypaths = my_infilepath.split(splitter);
			} catch (Exception e) {
				System.out.println("## ERREOR splitting file path in 'fileBynaryReplace': " + my_infilepath);
				return (-3);
			}
			mypaths[mypaths.length - 1] = "zz_" + mypaths[mypaths.length - 1];
			for (int i = 0; i < (mypaths.length - 1); i++) my_outfilepath += mypaths[i] + File.separator;
			my_outfilepath += mypaths[mypaths.length - 1];
		}
//System.out.println("WWWWWWWWWWWWWWWWWW my_infilepath: " + my_infilepath);
//System.out.println("WWWWWWWWWWWWWWWWWW my_outfilepath: " + my_outfilepath);

		byte[] find_bytes = null;
		byte[] replace_bytes = null;
		try {
			find_bytes = find.getBytes("UTF-8");
			replace_bytes = replace.getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) { return (-3); }

		File infile = new File(my_infilepath);
		if (!infile.exists()) return(-4);
		byte[] inbytes = null;

		InputStream is = null;
		FileOutputStream fos = null;
		int infile_size = 0;
		try {
			is = new FileInputStream(my_infilepath);
		}
		catch (FileNotFoundException fnfex) { return(-5); }

		try {
			infile_size = is.available();
			if (infile_size <= 0) { is.close(); return(-6); }
			inbytes = new byte[infile_size];
			int num_read = is.read( inbytes, 0, infile_size );
			is.close();
			if (num_read <= 0) return(-7);
		}
		catch(IOException ioex) { return(-8); }

		// search and replace bytes
		boolean found = false;
		int i = 0;
		int f = 0;
		int serach_length = infile_size - find_bytes.length + 1;
//System.out.print("WWWWWWWWWWWWWWWWWW infile_size: " + infile_size);
//System.out.print("WWWWWWWWWWWWWWWWWW serach_length: " + serach_length);
		for ( i = 0; i < serach_length; i++ ) {
			for ( f = 0; f < find_bytes.length; f++ ) {
				if ((i + f) > serach_length) break;
				if (inbytes[i + f] != find_bytes[f]) break;
			}
			if (f >= find_bytes.length) found = true;
			if (found) break;
		} 

		if (!found) {	// not found: nothing to do
			return(-9);
		}
//System.out.print("WWWWWWWWWWWWWWWWWW found: " + found);
//System.out.print("WWWWWWWWWWWWWWWWWW found at: " + i);

		// open output file
		try {
			fos = new FileOutputStream(my_outfilepath);
		}
		catch (FileNotFoundException fnfex) { return(-10); }
		// write the stuff to output file
		try {
			// write leading bytes
			fos.write(inbytes, 0, i);
			// write replace bytes
			fos.write(replace_bytes);
			// write trailing bytes
			fos.write(inbytes, i + find_bytes.length, inbytes.length - i - find_bytes.length);
		}
		catch(IOException ioex) { try { fos.close(); } catch(IOException ioex1) {} return(-12); }

		try { fos.close(); } catch(IOException ioex) { return(-13); }
		last_fileBynaryReplace_file = my_outfilepath;
//System.out.print("WWWWWWWWWWWWWWWWWW last_fileBynaryReplace_file: " + last_fileBynaryReplace_file);
		return(0);
	}


	
	// set Job Ticket variables
	// only those which act after the transform has been done make sense
	public static String setJobTicketVariable(String varname, String valuestr) {
		if (varname.equals("") == true) return("");
		try {
			do {
				if (varname.equals("mode") == true) { BatchXSLT.g_mainXSLTFrame.mode = Integer.valueOf(valuestr); return(varname); }
				if (varname.equals("loopDelay") == true) { BatchXSLT.g_mainXSLTFrame.loopDelay = Integer.valueOf(valuestr); return(varname); }
				if (varname.equals("debug") == true) {
					int myDEBUG = Integer.valueOf(valuestr);
					if (myDEBUG > 0) BatchXSLT.g_mainXSLTFrame.DEBUG = true;
					else BatchXSLT.g_mainXSLTFrame.DEBUG = false;
					return(varname);
				}
				if (varname.equals("im_debug") == true) { BatchXSLT.g_mainXSLTFrame.im_debug = Integer.valueOf(valuestr); return(varname); }

				if (varname.equals("excludeSourceProcessingRunFileNameExts") == true) { BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNameExts = valuestr; return(varname); }
				if (varname.equals("excludeCleanupRunFileNameExts") == true) { BatchXSLT.g_mainXSLTFrame.excludeCleanupRunFileNameExts = valuestr; return(varname); }
				if (varname.equals("sourceFileAction") == true) { BatchXSLT.g_mainXSLTFrame.sourceFileAction = valuestr; return(varname); }
				if (varname.equals("deleteSourceDirs") == true) { BatchXSLT.g_mainXSLTFrame.deleteSourceDirs = Integer.valueOf(valuestr); return(varname); }

				
				if (varname.equals("runBeforeJobApp") == true) { BatchXSLT.g_mainXSLTFrame.runBeforeJobApp = valuestr; return(varname); }
				if (varname.equals("runAfterJobApp") == true) { BatchXSLT.g_mainXSLTFrame.runAfterJobApp = valuestr; return(varname); }
				if (varname.equals("runBeforeTransformApp") == true) { BatchXSLT.g_mainXSLTFrame.runBeforeTransformApp = valuestr; return(varname); }
				if (varname.equals("runAfterTransformApp") == true) { BatchXSLT.g_mainXSLTFrame.runAfterTransformApp = valuestr; return(varname); }

				if (varname.equals("mainJobTicketPath") == true) { BatchXSLT.g_mainXSLTFrame.mainJobTicketPath = valuestr; return(varname); }
				if (varname.equals("nextJobTicketPath") == true) { BatchXSLT.g_mainXSLTFrame.nextJobTicketPath = valuestr; return(varname); }
				if (varname.equals("nextJobTicketFileName") == true) { BatchXSLT.g_mainXSLTFrame.nextJobTicketFileName = valuestr; return(varname); }
				if (varname.equals("JobTicketOverrideQueuePath") == true) { BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath = valuestr; return(varname); }
				break;
			} while(false);
		}
		catch (NumberFormatException ex) { return(""); }

		return("");
	}
	


	// show a message in BatchXSLT message window
	public static void showMess(String mess) {
		BatchXSLT.g_mainXSLTFrame.showMess( mess );
	}
	public static void showMess(String mess, String writelog) {
		BatchXSLT.g_mainXSLTFrame.showMess( mess, (writelog.equals("0") == false) );
	}


	/***** works with xalan 2.9 and above ****/
	// serialize a tree to a string
	public static String serializeDOMls(org.w3c.dom.Node theDOMsource, String indent, String omit_xml_declaration) {
		if (theDOMsource == null) return ("");
		org.w3c.dom.ls.DOMImplementationLS impl = null;
		org.w3c.dom.ls.LSSerializer writer = null;
		try {
			// Get a factory (DOMImplementationLS) for creating a Load and Save object.
			impl = (org.w3c.dom.ls.DOMImplementationLS) org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
		} catch (Exception e1) {
			System.out.println("EXCEPTION in serializeDOMls when creating instance: " + e1);
			return("");
		}
		try {
			// Use the factory to create an object (LSSerializer) used to write out or save the document.
			writer = impl.createLSSerializer();
		} catch (Exception e2) {
			System.out.println("EXCEPTION in serializeDOMls when creating serializer: " + e2);
			return("");
		}
		try {
			org.w3c.dom.DOMConfiguration config = writer.getDomConfig();
			if (indent.equals("0") == true) config.setParameter("format-pretty-print", Boolean.FALSE);
			if (omit_xml_declaration.equals("yes") == true) config.setParameter("xml-declaration", Boolean.FALSE);
		} catch (Exception e3) {
			System.out.println("EXCEPTION in serializeDOMls when setting config: " + e3);
			return("");
		}
		try {
			// Use the LSSerializer to write out or serialize the document to a String.
			String serializedXML = writer.writeToString(theDOMsource);
			return serializedXML;
		} catch (Exception e4) {
			System.out.println("EXCEPTION in serializeDOMls while serializing: " + e4);
		}
		return("");
	}


	// serialize a tree to a string
	public static String serializeDOM(org.w3c.dom.Node theDOMsource1, String indent, String omit_xml_declaration) {
		Transformer serializer = null;
		String styleSheet =
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n"
				+ "<xsl:output method=\"xml\" media-type=\"text/xml\" indent=\"yes\" />\n"
				+ "<xsl:output encoding=\"UTF-8\" />\n"
				+ "<xsl:template match=\"*|@*|comment()|processing-instruction()|text()\">\n"
				+ "<xsl:copy>\n"
				+ "<xsl:apply-templates select=\"*|@*|comment()|processing-instruction()|text()\" />\n"
				+ "</xsl:copy>\n"
				+ "</xsl:template>\n"
				+ "</xsl:stylesheet>\n";
		if (theDOMsource1 == null) return ("");


		TransformerFactory tfactory = TransformerFactory.newInstance();
		java.io.StringReader reader = new java.io.StringReader(styleSheet);
		javax.xml.transform.stream.StreamSource src = new javax.xml.transform.stream.StreamSource(reader);
		try {
			serializer = tfactory.newTransformer(src);
		}
		catch (TransformerConfigurationException e) { return (""); }

		// set properties
		Properties oprops = new Properties();
		if (indent != null) oprops.setProperty("{http\u003a//xml.apache.org/xslt}indent-amount", indent);
		if (omit_xml_declaration != null) oprops.setProperty("omit-xml-declaration", omit_xml_declaration);
		serializer.setOutputProperties(oprops);
		// the result string
		java.io.StringWriter writer = new java.io.StringWriter();
		javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(writer);

		String XMLText = "";

		if (theDOMsource1 != null) {
			javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource();
			domSource.setNode(theDOMsource1);
			try { serializer.transform(domSource, result); }
			catch (TransformerException e) { return (""); }
		}

		writer.flush();
		try {
			writer.close();
		}
		catch (IOException e) { return (""); }
		
		return (writer.toString());
	}




	// Parse the string, convert to Document and return root element
	public static NodeList parseStringToDom(String in, String which_elements)
	{
		if (in == null) return null;
		if (in.equals("") == true) return null;
		int length = in.length();
		if (length < 4) return null;	// at least <e/>
		char[] chars = new char[length];
		in.getChars(0, length, chars, 0);
		Document doc = null;
		DOMParser parser = new DOMParser();

		CharArrayReader newStream = new CharArrayReader(chars);
		InputSource source = new InputSource(newStream);
		try {
			parser.parse(source);
			doc = parser.getDocument();
		}
		catch (Exception e) {
			newStream.close();
			BatchXSLT.g_mainXSLTFrame.showMess( "##### ERROR: Exception at 'parseStringToDom(String, String). See consol log.'");
			System.out.println("##### ERROR: Exception at 'parseStringToDom(String, String)'");
			System.out.println("##### Failing string:");
			System.out.println(in);
			System.out.println("##### END Failing string:");
			e.printStackTrace();
			return null;
		}

		NodeList nodes = doc.getElementsByTagName(which_elements);
		//System.out.println("There are " + nodes.getLength() + " nodes.");

		newStream.close();

		return (nodes);
	}

	public static Element parseStringToDom(String in)
	{
		if (in == null) return null;
		if (in.equals("") == true) return null;
		int length = in.length();
		if (length < 4) return null;	// at least <e/>
		char[] chars = new char[length];
		in.getChars(0, length, chars, 0);
		Document doc = null;
		DOMParser parser = new DOMParser();

		CharArrayReader newStream = new CharArrayReader(chars);
		InputSource source = new InputSource(newStream);
		try {
			parser.parse(source);
			doc = parser.getDocument();
		}
		catch (Exception e) {
			newStream.close();
			BatchXSLT.g_mainXSLTFrame.showMess( "##### ERROR: Exception at 'parseStringToDom(String). See consol log.'");
			System.out.println("##### ERROR: Exception at 'parseStringToDom(String)'");
			System.out.println("##### Failing string:");
			System.out.println(in);
			System.out.println("##### END Failing string:");
			e.printStackTrace();
			try {
				//Create instance of DocumentBuilderFactory
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				//Get the DocumentBuilder
				DocumentBuilder docparser = factory.newDocumentBuilder();
				//Create blank DOM Document
				Document newdoc = docparser.newDocument();
				Element rootElement = newdoc.createElement("parseStringToDomError");
				newdoc.appendChild(rootElement);

				return (newdoc.getDocumentElement());
			} catch(Exception dbe){
				System.out.println(dbe.getMessage());
			}
			
			return (null);
		}

		//System.out.println("There are " + nodes.getLength() + "  anchored objects.");

		newStream.close();

		return (doc.getDocumentElement());
	}





	public static String decodeBase64(String in) {
	    return ( new String(Base64.decode(in)) );
	}




	public static String convertStringFromUTF8toSystemNative(String theStringToConvert) {
		try {
			String stringToConvert = theStringToConvert;
			byte[] convertStringToByte = stringToConvert.getBytes("UTF-8");
					//System.out.println("##### systemDefaultCharset: '" +  BatchXSLT.g_mainXSLTFrame.systemDefaultCharset + "'");
			return new String(convertStringToByte, BatchXSLT.g_mainXSLTFrame.systemDefaultCharset);
		} catch (UnsupportedEncodingException e) {
			return theStringToConvert;
		}
	}
	
	public static String convertStringFromUTF8toUTF16LE(String theStringToConvert) {
		try {
			String stringToConvert = theStringToConvert;
			byte[] convertStringToByte = stringToConvert.getBytes("UTF-8");
			return new String(convertStringToByte, "UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			return theStringToConvert;
		}
	}
	
	public static String convertStringFromUTF16toSystemNative(String theStringToConvert) {
		try {
			String stringToConvert = theStringToConvert;
			byte[] convertStringToByte = stringToConvert.getBytes("UTF-16");
			return new String(convertStringToByte, BatchXSLT.g_mainXSLTFrame.systemDefaultCharset);
		} catch (UnsupportedEncodingException e) {
			return theStringToConvert;
		}
	}
	

	public static byte[] convertStringFromUNICODEtoOther(String theStringToConvert, String otherEnc) {
		try {
			byte[] barr = theStringToConvert.getBytes(otherEnc);
			//return new String(barr,otherEnc);
			return barr;
		} catch (UnsupportedEncodingException e) {
			return theStringToConvert.getBytes();
		}
	}

	public static int deleteLogFile(boolean doclear) {
		lastFileExceptionMessage = "";
        lastFileExceptionError = 0;
        boolean deleted;
        int retval = 0;
		if ( mainXSLTFrame.logfile_path.equals("") || mainXSLTFrame.logfile_name.equals("") ) return (-1);
        // we first have to reset the redirected System.err/out streams to standard
		mainXSLTFrame.reset_StdOutErr();

		File logFile = new File( mainXSLTFrame.logfile_path + mainXSLTFrame.logfile_name );
		do {
			try { deleted = logFile.delete(); }
			catch (Exception ex) {
				lastFileExceptionMessage = ex.getMessage();
				lastFileExceptionError = -2;
				if (doclear) writeFile(mainXSLTFrame.logfile_path + mainXSLTFrame.logfile_name,"", true);
				retval = -2;
				break;
			}
			if (!deleted) {
				lastFileExceptionMessage = "Log file could not be deleted at path: '" + mainXSLTFrame.logfile_path + mainXSLTFrame.logfile_name + "'. Cleared content instead.";
				lastFileExceptionError = -1;
				if (doclear) writeFile(mainXSLTFrame.logfile_path + mainXSLTFrame.logfile_name,"", true);
				retval = -1;
				break;
			}
		} while(false);
        // we again redirected System.err/out streams to log file
		mainXSLTFrame.redirect_StdOutErr_toLog();
		return (retval);
	}
	static public boolean deleteDirectory(File path) {
		if (path == null) return(false);	// this is ok
		boolean deleted = true;
		if( path.exists() ) {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i]);
				}
				else {
					try { deleted = files[i].delete(); } catch(Exception e) {}
				}
			}
		}
		else return(false);
		deleted = path.delete();	// and delete top dir
		return( deleted );
	}



	/* ===========================================================
	 * get absolute path of file
	 */
	public static String file_absolutePath(String the_pathname)
	{
		return(file_absolutePath(the_pathname, false));
	}
	public static String file_absolutePath(String the_pathname, boolean makeLocalFS)
	{
		File anyFile = new File(the_pathname);
		String path = anyFile.getAbsolutePath();
		if (makeLocalFS == true) {
		}
		return(path);
	}

	public static String file_fullPath(String theBasePath, String thePath)
	{
		String fullpath;
		if (theBasePath.equals("") == true) return(thePath);
		if (thePath.indexOf(File.separator) == 0) return(thePath);	// is already full qualified path
		if (thePath.indexOf("file:") == 0) return(thePath);			// is already full qualified path
		if (thePath.indexOf(":") == 1) return(thePath);				// first char[0] is drive letter (Windows)
		if (thePath.indexOf("\\") == 0) return(thePath);				// first char[0] is drive letter (Windows)
		
		fullpath = theBasePath;
		if ((fullpath.endsWith(File.separator) == false)
			&& (thePath.indexOf(File.separator) != 0)
			) fullpath = fullpath + File.separator;

		fullpath += thePath;

		return(fullpath);
	}



	public static boolean deleteFile(String pathname) {
		File f = new File(pathname);
		if (f.exists()) {
			try { f.delete(); } catch (Exception e) {}
			return (true);
		}
		return (false);
	}
	public static boolean existsFile(String pathname) {
		File f = new File(pathname);
		return (f.exists());
	}
	public static long fileSize(String pathname) {
		File f = new File(pathname);
		return (f.length());
	}
	public static String fileSizeS(String pathname) {
		long size = fileSize(pathname);
		return ("" + size);
	}

	/**
	 * return the path of a found file
	 * envirpathstrings = a series of delimited path strings to be searched
	 */
	public static String findFileInEnvirPath(String envirpathstrings, String filename, String skip_path) {
		return(findFileInEnvirPath( envirpathstrings, filename, skip_path, ""));
	}
	public static String findFileInEnvirPath(String envirpathstrings, String filename, String skip_path, String must_path) {
		String[] envirpath = null;
		envirpath = envirpathstrings.split(File.pathSeparator);
		for (int i = 0; i < envirpath.length; i++) {
			if (skip_path.equals("") == false) {	// path may not contain this, like 'system' in C:\windows\system32 path (exclude system paths)
				if (envirpath[i].toLowerCase().indexOf( skip_path ) >= 0 ) continue;
			}
			if (must_path.equals("") == false) {	// path must contain this, like 'system' in C:\windows\system32 path (search in system paths)
				if (envirpath[i].toLowerCase().indexOf( must_path ) < 0 ) continue;
			}
			if (envirpath[i].endsWith(File.separator) == false) envirpath[i] += File.separator;
			//System.out.println(envirpath[i]);
			File f = new File(envirpath[i] + filename);
			if (f.exists()) return(envirpath[i]);
		}
		return("");
	}


	public static int writeFile(String pathname, String content, int overwrite) {
		boolean dooverwrite = true;
		if (overwrite <= 0) dooverwrite = false;
		return (writeFile(pathname, content, dooverwrite, false,"UTF8"));
	}
	public static int writeFile(String pathname, String content, boolean overwrite) {
		return (writeFile(pathname, content, overwrite, false, "UTF8"));
	}
	public static int writeFile(String pathname, String content, boolean overwrite, boolean append, String encoding) {
		BufferedWriter bw = null;
		FileWriter fwr = null;
		if (pathname.equals("") == true) return(-2);
		File f = new File(pathname);
		String thepath = f.getParent();
		if (thepath != null) {	// create the path if not exist
			File apath = new File(thepath);
			boolean retflag = apath.mkdirs();
		}

		if (overwrite == false) {
			if (f.exists()) return (0);
		}
		if ((encoding.equals("") == true) || (encoding.equals("BINARY") == true)) {
			try {
				fwr = new FileWriter( f, append );
				fwr.write(content);
				fwr.flush();
				fwr.close();
			} catch (IOException e) {
				try { fwr.flush(); } catch ( IOException ioe ) { }
				try { fwr.close(); } catch ( IOException ioe ) { }
				return (-1);
			}
		}
		else {
			try {
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream( f, append ), "UTF8"));
				bw.write(content,0,content.length());
				bw.flush();
				bw.close();
			} catch (IOException e) {
				try { bw.flush(); } catch ( IOException ioe ) { }
				try { bw.close(); } catch ( IOException ioe ) { }
				return (-1);
			}
		}
		return (0);
	}

    /**
     * Read a file as binary
     * @param pathname a full path/name String
     * @return the read file as byte[]
     */
    public static byte[] readFile(String pathname) {
    	byte[] bytes = null;
		try{
			//Instantiate the file object
			File file = new File(pathname);
			//Instantiate the input stread
			InputStream insputStream = new FileInputStream(file);
			long length = file.length();
			bytes = new byte[(int) length];

			insputStream.read(bytes);
			insputStream.close();
		}catch(Exception e){
			System.out.println("Error is:" + e.getMessage());
			return(null);
		}
		return(bytes);
    }

    /**
     *
     * @param pathname a full path/name String
     * @return the read file as String
     */
    public static String readFileUTF(String pathname) {
		lastFileExceptionMessage = "";
        lastFileExceptionError = 0;
		if ( (pathname == null) || (pathname.equals("")) ) {
			lastFileExceptionMessage = "Path/Name not given.";
            lastFileExceptionError = -1;
			return("");
		}

		FileInputStream		fis = null;
		InputStreamReader	isr = null;
		char[]				cbuf = null;
		String				data = "";
		int                 input_bytes_avail = -1;
		int                 bytes_read = -1;
		int                 ofs = 0;

		// read the file into a char array
		try { fis = new FileInputStream( pathname ); }
		catch ( FileNotFoundException fnfe ) {
			lastFileExceptionMessage = fnfe.getMessage();
            lastFileExceptionError = -2;
			return("");
		}
		try { isr = new InputStreamReader(fis, "UTF8"); }
		catch ( UnsupportedEncodingException e ) {
			try { fis.close(); } catch ( IOException ioe ) { }
			lastFileExceptionMessage = e.getMessage();
            lastFileExceptionError = -3;
			return("");
		}
		
		try { input_bytes_avail = fis.available(); }
		catch ( IOException e ) {
			try { fis.close(); } catch ( IOException ioe ) { }
			try { isr.close(); } catch ( IOException ioe ) { }
			lastFileExceptionMessage = e.getMessage();
            lastFileExceptionError = -4;
			return("");
		}

		cbuf = new char[input_bytes_avail];
		try {
			bytes_read = isr.read(cbuf, 0, input_bytes_avail);	// returns -1 if eof reached or num bytes read
			if (bytes_read > 0) {
				data = new String(cbuf,  0,  bytes_read);	// get text from input file
			}
		} catch ( IOException e ) {
			try { fis.close(); } catch ( IOException ioe ) { }
			try { isr.close(); } catch ( IOException ioe ) { }
			lastFileExceptionMessage = e.getMessage();
            lastFileExceptionError = -5;
			return("");
		}
		try { fis.close(); } catch ( IOException ioe ) { }
		try { isr.close(); } catch ( IOException ioe ) { }
		return(data);
	}


	public static boolean copyFolderPath2(String srcpath, String srcname, String targpath, String targname, int recursive, int onlyNew, String errormessage) {
		boolean retflag = copyFolderPath(srcpath, srcname, targpath, targname, (recursive > 0 ? true : false), (onlyNew > 0 ? true : false), errormessage);
		return(retflag);
	}


	public static boolean copyFolderPath(String srcpath, String srcname, String targpath, String targname, final boolean recursive, final boolean onlyNew, String errormessage) {
		if ((srcpath == null) || srcpath.equals("")) return(false);
		String src = srcpath;
		if (src.endsWith(File.separator) == false)  src = src + File.separator;
		if ((srcname != null) && (srcname.equals("") == false))  src = src + srcname;
		src = resolveRelativePath(src);
		File sf = new File(src);
		if (sf.exists() == false) return (false);

		String targ = "";
		if ((targpath == null) || targpath.equals("")) targ = srcpath;
		else targ = targpath;
		if ((targname != null) && (targname.equals("") == false))  {
			if (targ.endsWith(File.separator) == false)  targ = targ + File.separator;
			targ = targ + targname;
		}
		else {
			if ((srcname != null) && (srcname.equals("") == false)) {
				if (targ.endsWith(File.separator) == false)  targ = targ + File.separator;
				targ = targ + srcname;
			}
		}
		targ = resolveRelativePath(targ);
		if (targ.equals(src) == true) return(false);
		File tf = new File(targ);
		try {
			copyFolder(sf, tf, recursive, onlyNew);
        }
		catch (Exception e) {
			if (errormessage.equals("") == false) BatchXSLT.g_mainXSLTFrame.showMess( errormessage + e.getMessage() + "'\n" );
			return(false);
		}

		return (true);
	}


    /**
     * Copies folder.
     * @param src source folder
     * @param dest target folder
     * @param recursive if <code>true</code>, processes folder recursively
     * @param onlyNew if <code>true</code>, target file will be overridden if it
     *                is older than source file only
     * @throws IOException if any I/O error has occurred
     */
    public static void copyFolder(final File src, final File dest, final boolean recursive, final boolean onlyNew) throws IOException {
		boolean copied = true;
		if (!src.isDirectory()) { throw new IOException(PACKAGE_NAME + " not a folder: " + src); }
        if (dest.isFile()) { throw new IOException(PACKAGE_NAME + " is a file: " + dest); }
        if (!dest.exists() && !dest.mkdirs()) { throw new IOException(PACKAGE_NAME + " can not make folder: " + dest); }
        File[] srcFiles = src.listFiles();
        for (int i = 0; i < srcFiles.length; i++) {
            File file = srcFiles[i];
            if (file.isDirectory()) {
                if (recursive) {
                     copyFolder(file, new File(dest, file.getName()), recursive, onlyNew);
                }
                continue;
            }
            File destFile = new File(dest, file.getName());
            if (onlyNew && destFile.isFile() && (destFile.lastModified() > file.lastModified())) {
                continue;
            }
            copyFile(file, destFile);
        }
        dest.setLastModified(src.lastModified());
    }

    /**
     * Copies one file, existing file will be overridden.
     * @param src source file to copy FROM
     * @param dest destination file to copy TO
     * @throws IOException if any I/O error has occurred
     */
    public static void copyFile(final File src, final File dest) throws IOException {
        if (!src.isFile()) {
            throw new IOException(PACKAGE_NAME + " not a file: " + src);
        }
        if (dest.isDirectory()) {
            throw new IOException(PACKAGE_NAME + "is Folder: " + dest);
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest, false));
            try {
                copyStream(in, out, 4096);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
        dest.setLastModified(src.lastModified());
    }

    
    /**
     * Copies streams.
     * @param in source stream
     * @param out destination stream
     * @param bufferSize buffer size to use
     * @throws IOException if any I/O error has occurred
     */
    public static void copyStream(final InputStream in, final OutputStream out, final int bufferSize) throws IOException {
        byte[] buf = new byte[bufferSize];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }





    /**
     * Copies local folder to local or remote location.
     * @param src source folder path
     * @param dest target folder path or ftp url
     * @param recursive if <code>true</code>, processes folder recursively
     * @param deletesource if <code>true</code>, source file is deleted after being copied
     * @throws IOException if any I/O error has occurred
     */
	public static void copyFolder(final String srcpath, final String destpath, final boolean recursive, final boolean deletesource) throws IOException {
		File src = new File(srcpath);
        if (!src.exists()) { throw new IOException(PACKAGE_NAME + " not found: " + srcpath); }

		boolean f_deleted = false;
		if (src.isDirectory()) {
			File[] srcFiles = src.listFiles();
			for (int i = 0; i < srcFiles.length; i++) {
				if (BatchXSLT.g_mainXSLTFrame.general_abort == true) return;
				File file = srcFiles[i];
				String path = file.getParent(); if (path.endsWith(File.separator) == false) path += File.separator;
				String pathname = path + file.getName();
				if (file.getName().equals(".DS_Store") == true) {
					f_deleted = BatchXSLTransform.file_delete(pathname);	// delete it
					continue;
				}
				if (file.isDirectory()) {
					if (recursive) {
						copyFolder(pathname, destpath + file.getName() + File.separator, recursive, deletesource);
						File[] flist = file.listFiles();
						if (flist.length <= 0) {
							//System.out.println("empty folder: '" + pathname + "'");
							if (deletesource == true) f_deleted = BatchXSLTransform.file_delete(pathname);	// delete this empty dir
							continue;
						}
					}
					continue;
				}
				// is a file
				int copied = BatchXSLTransform.copyFile(path, file.getName(), destpath, "", 1, true, false);
				if (deletesource == true) f_deleted = BatchXSLTransform.file_delete(pathname);	// delete the source file
			}
		}
		else {	// is a file
			String path = src.getParent(); if (path.endsWith(File.separator) == false) path += File.separator;
			String pathname = path + src.getName();
			if (src.getName().equals(".DS_Store") == true) {
				f_deleted = BatchXSLTransform.file_delete(pathname);	// delete it
				return;
			}
			int copied = BatchXSLTransform.copyFile(path, src.getName(), destpath, "", 1, true, false);
			if (deletesource == true) f_deleted = BatchXSLTransform.file_delete(pathname);	// delete the source file
		}
	}



    /**
     * Copies local folder to local or remote location.
     * @param src source folder path
     * @param dest target folder path or ftp url
     * @param recursive if <code>true</code>, processes folder recursively
     * @throws IOException if any I/O error has occurred
     */
	public static void copyMultipleFiles(final File[] files, final String destpath, final boolean recursive, final boolean deletesource) throws IOException {
			for (int idx = 0; (idx < files.length) && (BatchXSLT.g_mainXSLTFrame.general_abort == false); idx++ ) {
				if (files[idx].isDirectory() == true) {
					String my_PathName = (files[idx].getPath() != null ? files[idx].getPath() : "");
					my_PathName += File.separator;
					String my_outputPathName = destpath;
					if (my_outputPathName.endsWith("/") == false) my_outputPathName += "/";
					my_outputPathName += files[idx].getName() + "/";
					try { com.epaperarchives.batchxslt.utils.copyFolder(my_PathName, my_outputPathName, true, false); } catch (Exception e) {}
				}
				else {
					String my_Path = (files[idx].getParent() != null ? files[idx].getParent() : "");
					if (my_Path.endsWith(File.separator) == false) my_Path += File.separator;
					String my_Name = (files[idx].getName() != null ? files[idx].getName() : "");
					int copied = BatchXSLTransform.copyFile(my_Path, my_Name, destpath, "", 1, true, false);
				}
				try { Thread.sleep(50); }
				catch (InterruptedException x) {}
			}
			BatchXSLTransform.copyFile_disconnect();	// we evtl have ftp transferrred files and now have to disconnect
	}
    
    /**
     * resolve a path which may contain relative elements like: /mypath/sub1/../mydir
     * @param path the path String to resolve
     * @return the resolved path String
     */
    public static String resolveRelativePath(String path) {
		if ((path == null) || path.equals("")) return("");
		String relsep = ".." + File.separator;
		int pos = path.indexOf(relsep);
		if (pos < 0) return(path);	// no relative folders
		String resolvedpath = path;
		while (pos >= 0) {
			String part1 = resolvedpath.substring(0,resolvedpath.indexOf(relsep));
			if (part1.endsWith(File.separator)) part1 = part1.substring(0,part1.length()-1);	// cut last folder in this part
			part1 = part1.substring(0,part1.lastIndexOf(File.separator));
			String part2 = resolvedpath.substring(pos + relsep.length());

			resolvedpath = part1 + File.separator + part2;
			pos = resolvedpath.indexOf(relsep);
		}
		
		return(resolvedpath);
	}
	
	
	
	public static int copyFile(String the_sourcepath, String the_sourcename, String the_targetpath, String the_targetname, int create_target_path, boolean give_message) {
		int copied = BatchXSLTransform.copyFile(the_sourcepath, the_sourcename, the_targetpath, the_targetname, create_target_path, give_message);
		return(copied);
	}



    /**
     * Detect the mime type of a file, from file name extension.
     * @param path the path to file to detect
     * @return the detected mime type like image/tif, text/xml, image/jpeg
     */
    public static String detectMimeType(String path) {
		if ((path == null) || (path.equals("") == true)) return("");	// invalid path

		if (path.endsWith(File.separator) == true) return("");	// is a DIR
		// get from file name extension
		String lc_path = path.toLowerCase();
		if (path.indexOf(".") >= 0) {
			if ( (lc_path.endsWith(".tif") == true) || (lc_path.endsWith(".tiff") == true) ) return("image/tiff");	// TIFF
			if ( (lc_path.endsWith(".jpg") == true) || (lc_path.endsWith(".jpeg") == true) ) return("image/jpeg");	// JPEG
			if ( (lc_path.endsWith(".gif") == true) ) return("image/gif");	// GIF
			if ( (lc_path.endsWith(".png") == true) ) return("image/png");	// PNG
			if ( (lc_path.endsWith(".eps") == true) ) return("image/eps");	// EPS
			if ( (lc_path.endsWith(".ps") == true) ) return("application/postscript");	// PS
			if ( (lc_path.endsWith(".psd") == true) ) return("image/vnd.adobe.photoshop");	// Photoshop
			if ( (lc_path.endsWith(".ai") == true) ) return("application/postscript");	// Illustrator
			if ( (lc_path.endsWith(".bmp") == true) ) return("image/bmp");	// BMP
			if ( (lc_path.endsWith(".wbmp") == true) ) return("image/vnd.wap.wbmp");	// WBMP
			if ( (lc_path.endsWith(".webp") == true) ) return("image/webp");	// WEBP
			if ( (lc_path.endsWith(".svg") == true) ) return("image/svg+xml");
			if ( (lc_path.endsWith(".ico") == true) ) return("image/x-icon");

			if ( (lc_path.endsWith(".wav") == true) ) return("audio/wav");	// WAV
			if ( (lc_path.endsWith(".weba") == true) ) return("audio/webm");
			if ( (lc_path.endsWith(".webm") == true) ) return("audio/webm");
			if ( (lc_path.endsWith(".oga") == true) ) return("audio/ogg");
			if ( (lc_path.endsWith(".3gp") == true) ) return("video/3gpp");
			if ( (lc_path.endsWith(".3g2") == true) ) return("video/3gpp2");
			if ( (lc_path.endsWith(".ogv") == true) ) return("video/ogg");
			if ( (lc_path.endsWith(".mpeg") == true) ) return("video/mpeg");

			if ( (lc_path.endsWith(".html") == true) || (lc_path.endsWith(".htm") == true) ) return("text/html");	// HTML
			if ( (lc_path.endsWith(".txt") == true) ) return("text/plain");	// TEXT
			if ( (lc_path.endsWith(".xml") == true) ) return("text/xml");	// XML
			if ( (lc_path.endsWith(".xsl") == true) ) return("text/xml");	// XSL
			if ( (lc_path.endsWith(".xhtml") == true) || (lc_path.endsWith(".xhtm") == true) ) return("application/xhtml+xml");	// XHTML
			if ( (lc_path.endsWith(".xls") == true) ) return("application/vnd.ms-excel");
			if ( (lc_path.endsWith(".js") == true) ) return("application/javascript");

			if ( (lc_path.endsWith(".rar") == true) ) return("application/x-rar-compressed");	// RAR
			if ( (lc_path.endsWith(".sit") == true) ) return("application/x-sit");	// SIT
			if ( (lc_path.endsWith(".sitx") == true) ) return("application/x-sit");	// SITX
			if ( (lc_path.endsWith(".sea") == true) ) return("application/sea");	// SEA
			if ( (lc_path.endsWith(".zip") == true) ) return("application/zip");	// ZIP
			if ( (lc_path.endsWith(".7z") == true) ) return("application/x-7z-compressed");	// ZIP
			if ( (lc_path.endsWith(".epub") == true) ) return("application/epub+zip");	// ZIP InDesign epub archive

			if ( (lc_path.endsWith(".eot") == true) ) return("application/vnd.ms-fontobject");	// EOt font
			if ( (lc_path.endsWith(".woff") == true) ) return("font/woff");	// EOt font
			if ( (lc_path.endsWith(".woff2") == true) ) return("font/woff2");	// EOt font
			if ( (lc_path.endsWith(".ttf") == true) ) return("font/ttf");	// EOt font
		}
		return("application/octet-stream");
	}

    /**
     * Detect the type of a file, from file name extension or content.
     * @param path the path to file to detect
     * @return the detected type like tif, xml, jpg
     */
    public static String detectFileType(String path) {
		if ((path == null) || (path.equals("") == true)) return("");	// invalid path
		String detectedType = "";

		if (path.endsWith(File.separator) == true) return("");	// is a DIR
		// get from file name extension
		String lc_path = path.toLowerCase();
		if (path.indexOf(".") >= 0) {
			if ( (lc_path.endsWith(".tif") == true) || (lc_path.endsWith(".tiff") == true) ) return("tif");	// TIFF
			if ( (lc_path.endsWith(".jpg") == true) || (lc_path.endsWith(".jpeg") == true) ) return("jpg");	// JPEG
			if ( (lc_path.endsWith(".gif") == true) ) return("gif");	// GIF
			if ( (lc_path.endsWith(".png") == true) ) return("png");	// PNG
			if ( (lc_path.endsWith(".eps") == true) ) return("eps");	// EPS
			if ( (lc_path.endsWith(".ps") == true) ) return("ps");	// PS
			if ( (lc_path.endsWith(".psd") == true) ) return("psd");	// Photoshop
			if ( (lc_path.endsWith(".ai") == true) ) return("psd");	// Illustrator
			if ( (lc_path.endsWith(".bmp") == true) ) return("bmp");	// BMP
			if ( (lc_path.endsWith(".xml") == true) ) return("xml");	// XML
			if ( (lc_path.endsWith(".xsl") == true) ) return("xsl");	// XSL

			if ( (lc_path.endsWith(".qxd") == true) ) return("qxd");	// Quark DOC (QXD)
			if ( (lc_path.endsWith(".indd") == true) ) return("indd");	// InDesign DOC (INDD)

			if ( (lc_path.endsWith(".html") == true) || (lc_path.endsWith(".htm") == true) ) return("htm");	// HTML
			if ( (lc_path.endsWith(".xhtml") == true) || (lc_path.endsWith(".xhtm") == true) ) return("xhtm");	// XHTML
			if ( (lc_path.endsWith(".txt") == true) ) return("txt");	// TEXT
			if ( (lc_path.endsWith(".sit") == true) ) return("sit");	// SIT
			if ( (lc_path.endsWith(".sitx") == true) ) return("sitx");	// SITX
			if ( (lc_path.endsWith(".sea") == true) ) return("sea");	// SEA
			if ( (lc_path.endsWith(".zip") == true) ) return("zip");	// ZIP
			if ( (lc_path.endsWith(".7z") == true) ) return("7z");	// ZIP
			if ( (lc_path.endsWith(".idml") == true) ) return("idml");	// ZIP InDesign Markup Language archive
			if ( (lc_path.endsWith(".epub") == true) ) return("epub");	// ZIP InDesign epub archive
		}

		// get from file content if no extension available or not detected until now
		// try to read this file
		FileInputStream mystream = null;
		try { mystream = new FileInputStream( path ); }
		catch ( FileNotFoundException e ) {	// uups could not read it
			return(detectedType);
		}
		// check how many bytes can be read
		try {
			int numbytes = mystream.available();
			if ( numbytes <= 3 ) {			// not enough bytes avail
				try { mystream.close(); } catch ( java.io.IOException e1 ) {}
				return(detectedType);
			}
		}
		catch ( java.io.IOException e ) {	// uups could not read it
			try { mystream.close(); } catch ( java.io.IOException e1 ) {}
			return(detectedType);
		}

		// read some bytes into buffer
		int buffersize = 1000;
		byte mybuf[] = new byte[buffersize]; 
		try { mystream.read( mybuf, 0, buffersize ); }
		catch ( java.io.IOException e) {	// uups could not read it - invalid file
			try { mystream.close(); } catch ( java.io.IOException e1 ) {}
			return(detectedType);
		}
		try { mystream.close(); } catch (java.io.IOException e) {}

		// scan the buffer
		int scanLength = 50;
		for ( int i = 0; i < scanLength; i++) {	// check if we find 'MM\0*'
			if ( (mybuf[i] == 'M') && (mybuf[i+1] == 'M') && (mybuf[i+2] == '\0') && (mybuf[i+3] == '*') ) { detectedType = "tif"; break; }
			if ( (mybuf[i] == 'I') && (mybuf[i+1] == 'I') && (mybuf[i+2] == '*') ) { detectedType = "tif"; break; }

			if ( (mybuf[i] == '%') && (mybuf[i+1] == '!') && (mybuf[i+2] == 'P') && (mybuf[i+3] == 'S') ) { detectedType = "eps"; break; }
			if ( (mybuf[i] == '%') && (mybuf[i+1] == 'E') && (mybuf[i+2] == 'P') && (mybuf[i+3] == 'S') ) { detectedType = "eps"; break; }

			if ( (mybuf[i] == '%') && (mybuf[i+1] == 'P') && (mybuf[i+2] == 'D') && (mybuf[i+3] == 'F') ) { detectedType = "pdf"; break; }

			if ( (mybuf[i] == '8') && (mybuf[i+1] == 'B') && (mybuf[i+2] == 'P') && (mybuf[i+3] == 'S') ) { detectedType = "psd"; break; }

			if ( (mybuf[i] == 0x89) && (mybuf[i+1] == 'P') && (mybuf[i+2] == 'N') && (mybuf[i+3] == 'G') ) { detectedType = "png"; break; }

			if ( (i == 0) && (mybuf[i] == 0xff) && (mybuf[i+1] ==  0xd8) && (mybuf[i+2] ==  0xff) ) { detectedType = "jpg"; break; }	// must start ff d8 ff
			if ( (i == 6) && (mybuf[i] == 'J') && (mybuf[i+1] ==  'F') && (mybuf[i+2] ==  'I') && (mybuf[i+3] ==  'F') ) { detectedType = "jpg"; break; }	// Pos 6 must be: JFIF

			if ( (i == 0) && (mybuf[i] == 'G') && (mybuf[i+1] == 'I') && (mybuf[i+2] == 'F') && (mybuf[i+3] == '8') ) { detectedType = "gif"; break; }	// GIF89a, GIF87a

			if ( (i == 0) && (mybuf[i] == 'B') && (mybuf[i+1] == 'M') && (mybuf[i+2] == 'H') ) { detectedType = "bmp"; break; }

			if ( (i == 0) && (mybuf[i] == 0x00) && (mybuf[i+1] == 0x00) && (mybuf[i+2] == 'M') && (mybuf[i+3] == 'M') && (mybuf[i+4] == 'X') && (mybuf[i+5] == 'P') && (mybuf[i+6] == 'R') && (mybuf[i+7] == '3') ) { detectedType = "qxd"; break; }
			if ( (i == 0) && (mybuf[i] == 0x06) && (mybuf[i+1] == 0x06) && (mybuf[i+2] == 0xed) && (mybuf[i+3] == 0xf5) && (mybuf[i+4] == 0xd8) ) { detectedType = "indd"; break; }

			if ( (mybuf[i] == '<') && (mybuf[i+1] == '?') && (mybuf[i+2] == 'x') && (mybuf[i+3] == 'm') && (mybuf[i+4] == 'l') ) { detectedType = "xml"; break; }

			if ( (i == 0) && (mybuf[i] == 'S') && (mybuf[i+1] == 't') && (mybuf[i+2] == 'u') && (mybuf[i+3] == 'f') && (mybuf[i+4] == 'f') ) { detectedType = "sit"; break; }
			if ( (i == 0) && (mybuf[i] == 'P') && (mybuf[i+1] == 'K') && (mybuf[i+2] == 0x03) && (mybuf[i+3] == 0x04) ) { detectedType = "zip"; break; }
		}



		return(detectedType);
	}



    /**
     * detect a string in a file.
     * @param path the path to file to detect
     * @param searchstring the string to detect
     * @param checksize the number of bytes to check in a file or 0 for whole file
     * @return integer >= 0 if found, else -1
     */
    public static int detectFileContent(String path, String searchstring, int checksize) {
		if ((path == null) || (path.equals("") == true)) return(-1);	// invalid path
		if ((searchstring == null) || (searchstring.equals("") == true)) return(-1);	// invalid search string
		int pos = -1;
		int filesize = 0;
		int bytesread = 0;
		int buffersize = checksize;
		byte[] searchbytes = null;

		// try to read this file
		FileInputStream mystream = null;
		try { mystream = new FileInputStream( path ); }
		catch ( FileNotFoundException e ) {	// uups could not read it
			return(-1);
		}
		// check how many bytes can be read
		try {
			filesize = mystream.available();
			if ( filesize <= 0 ) {			// not enough bytes avail
				try { mystream.close(); } catch ( java.io.IOException e1 ) {}
				return(-1);
			}
		}
		catch ( java.io.IOException e ) {	// uups could not read it
			try { mystream.close(); } catch ( java.io.IOException e1 ) {}
			return(-1);
		}
		if ( filesize < searchstring.length() ) {			// not enough bytes avail
			try { mystream.close(); } catch ( java.io.IOException e1 ) {}
			return(-1);
		}

		// read some bytes into buffer
		if ( checksize <= 0 ) buffersize = filesize;
		byte mybuf[] = new byte[buffersize]; 
		try { bytesread = mystream.read( mybuf, 0, buffersize ); }
		catch ( java.io.IOException e) {	// uups could not read it - invalid file
			try { mystream.close(); } catch ( java.io.IOException e1 ) {}
			return(-1);
		}
		try { mystream.close(); } catch (java.io.IOException e) {}

		if ( bytesread < 0 ) return(-1);

		searchbytes = searchstring.getBytes();
		// scan the buffer
		int foundpos = -1;
		int k;
		for ( int i = 0; i < (bytesread-searchbytes.length); i++) {	// check if we find the bytes
			for ( k = 0; k < searchbytes.length; k++) {
				try {
					if ( mybuf[i + k] != searchbytes[k] ) break;
				} catch(Exception e){ return(-1); }
			}
			if ( k >= searchbytes.length ) { foundpos = i; break; }
		}

		return(foundpos);

	}
	
	
    /**
     * Get/prepare a file list iterator of the given directory.
     * @param rootpath the path tolist
     * @param theExtensions comma separated list of extensions or empty for all files
     * @param dorecursive recursevly walk subfolders too
     * @return int    the number of files found
     */
	static Iterator<File> directoryListIterator = null;
	static String directoryListRootPath = "";
    public static void directoryListInit() {
		directoryListIterator = null;
		directoryListRootPath = "";
	}
    public static boolean directoryListValid() {
		if (directoryListIterator == null) return(false);
		return(true);
	}

	/**
	 * Prepare a directory list into a directory iterator.
	 * The actaul directory list file can be cretated after having called this function by calling directoryListWriteXMLFile()
	 *
	 * @param rootPath String: the path to list like: c:/mypath or ~/myfolder/dir1 where ~is the user home folder
	 * @param theExtensions String: file name extensions filter: optional list of comma separated file name extensions to list like ".exe" or ".exe,.pdf,.txt" - set to "" (empty string) to list all files
	 * @param doRecursive Boolean: true/false true means to walk subdirectories too
	 * @return int  0 = all items processed
	 *              1 = have more items available
	 *             -1 = no path specified
	 *             -2 = path does not exist
	 *             -3 = path is not a directory
	 *
	 * @see directoryListWriteXMLFile()
	 */
    public static int directoryListCreate(String rootPath, String theExtensions, boolean doRecursive) {
		if ((rootPath == null) || (rootPath.equals("") == true)) return(-1);
		String therootPath = rootPath;
		if ( therootPath.startsWith("~") == true) {
			String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
			therootPath = home + therootPath.substring(2);	// cut leading path ~/
		}
		File rootdir = new File(therootPath);
		/*
		System.out.println("**** directoryListCreate: rootpath = '" + therootPath + "'");
		System.out.println("**** directoryListCreate: rootdir.exists = '" + rootdir.exists() + "'");
		System.out.println("**** directoryListCreate: rootdir.isDirectory = '" + rootdir.isDirectory() + "'");
		System.out.println("**** directoryListCreate: theExtensions = '" + theExtensions + "'");
		System.out.println("**** directoryListCreate: doRecursive = '" + doRecursive + "'");
		*/
		if (rootdir.exists() == false) return(-2);
		if (rootdir.isDirectory() == false) return(-3);
		
		String[] extensions = null;
		if ((theExtensions != null) && (theExtensions.equals("") == false)) extensions = theExtensions.split(",");
		try {
			directoryListIterator = org.apache.commons.io.FileUtils.iterateFiles(rootdir, extensions, doRecursive);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (directoryListIterator != null) directoryListRootPath = therootPath;

		//System.out.println("**** directoryListCreate: directoryListIterator = '" + directoryListIterator + "'");
		return(directoryListIterator.hasNext() ? 1 : 0);
	}
	
    public static String directoryListGetFile() {
		if (directoryListIterator == null) return("");
		String filePathname = "";
		while (directoryListIterator.hasNext() == true) {
			File thefile = directoryListIterator.next();
			if (thefile.exists() == false) continue;
			if (thefile.isDirectory() == true) continue;
			filePathname = thefile.getAbsolutePath();
			break;
		}
		return(filePathname);
	}
	
    public static String directoryListGetAsXMLString(boolean withXMLdeclaration) {
		if (directoryListIterator == null) return("<!-- EMPTY list -->");
		String CR = "\n";
		if (BatchXSLTransform.isWindows() == true) CR = "\r\n";
		String listXML = "";
		if (withXMLdeclaration == true) listXML += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CR + "<!DOCTYPE folderList>" + CR;
		listXML += "<folderList>" + CR;
		listXML += "<rootPath>" + directoryListRootPath + "</rootPath>" + CR;
		while (true) {
			String path = directoryListGetFile();
			if (path.equals("") == true) break;
			listXML += "<file>" + path + "</file>" + CR;
		}
		listXML += "</folderList>" + CR;
		return(listXML);
	}

	/**
	 * Write an XML file from a directory list prepared with directoryListCreate().
	 * The function directoryListCreate() must be called before this call to directoryListWriteXMLFile()
	 *
	 * @param xmlFilePathName     String: the path/name to write the file to like "c:/mydirlist.xml"
	 * @param withXMLdeclaration  Boolean: true/false to state the XML declaration in the list file
	 * @return path               String: the path/name of the created XMl file
	 *                            empty String: if writing the file has failed or no file name was given, an empt string is returned
	 *
	 * @see also directoryListCreate()
	 */
	public static String directoryListWriteXMLFile(String xmlFilePathName, boolean withXMLdeclaration) {
		if ((xmlFilePathName == null) || (xmlFilePathName.equals("") == true)) return("");
		
		String xmlstr = directoryListGetAsXMLString(withXMLdeclaration);
		int written = writeFile(xmlFilePathName, xmlstr, true, false, "UTF-8");
		if (written == 0) return(xmlFilePathName);
		return("");
	}






	public static int setLogFile(String dowrite, String path, String name, String maxsize, String maxlogfiles) {
		if (dowrite != null) BatchXSLT.g_mainXSLTFrame.logfile_write = Integer.valueOf(dowrite);
		if (path != null) BatchXSLT.g_mainXSLTFrame.logfile_path = path;
		if (name != null) BatchXSLT.g_mainXSLTFrame.logfile_name = name;
		if (maxsize != null) {
			BatchXSLT.g_mainXSLTFrame.logfile_maxsize = Integer.valueOf(maxsize);
			if (BatchXSLT.g_mainXSLTFrame.logfile_maxsize < BatchXSLT.g_mainXSLTFrame.logfile_minsize) BatchXSLT.g_mainXSLTFrame.logfile_maxsize = BatchXSLT.g_mainXSLTFrame.logfile_minsize;
		}
		if (maxlogfiles != null) {
			BatchXSLT.g_mainXSLTFrame.logfile_max = Integer.valueOf(maxlogfiles);
			if (BatchXSLT.g_mainXSLTFrame.logfile_max < BatchXSLT.g_mainXSLTFrame.logfile_min) BatchXSLT.g_mainXSLTFrame.logfile_max = BatchXSLT.g_mainXSLTFrame.logfile_min;
		}
		return (0);
	}
	public static int setLogFile(String dowrite, String path, String name, String maxsize) {
		return (setLogFile(dowrite, path, name, maxsize, null));
	}
	public static int setLogFile(String dowrite, String path, String name) {
		return (setLogFile(dowrite, path, name, null, null));
	}


	public static void sendMail(String mailHost, String from, String to, String subject, String body) {
		//if (mailHost == null || mailHost.equals("")) return;
		boolean debug = true;
		try {
			// If specified a mailhost, tell the system about it.
			if (debug) System.out.println("mail.host: " + System.getProperty("mail.host"));	// Tell the user what's happening
			if ((mailHost != null) && (mailHost.equals("") == false)) System.getProperties().put("mail.host", mailHost);
Properties props = new Properties();
props.put("mail.smtp.host", "localhost");

			// Establish a network connection for sending mail
			URL u = new URL("mailto:" + to);		// Create a mailto: URL 
			URLConnection c = u.openConnection();	// Create a URLConnection for it
			c.setAllowUserInteraction(false);		// do it silent
			c.setDoInput(false);					// Specify no input from this URL
			c.setDoOutput(true);					// Specify we'll do output
			c.setConnectTimeout(1000);				// timeout in ms

			if (debug) {
				System.out.println("Connecting..."); System.out.flush();	// Tell the user what's happening, right now
			}
			c.connect();							// Connect to mail host

			// write the mail message to the output
			BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
			bwr.write("From: \"" + from + "\" <" + System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName() + ">\n");
			bwr.write("To: " + to + "\n");
			bwr.write("Subject: " + subject + "\n");
			bwr.write("\n");
			bwr.write(body);
			bwr.flush();
			bwr.close();

			// Tell the user it was successfully sent.
			if (debug) {
				System.out.println("Message sent."); System.out.flush();
			}
		}
		catch (Exception e) {  // Handle any exceptions, print error message.
			if (debug) {
				System.out.println("ERROR sending mail:");
				System.out.println(e);
			}
		}
	}


	// check for available external converters
	public static String checkAvailableExtConverter(int whichConverter) {
		if (whichConverter <= 0) return ("");
		String version_str = "";
		// check for GhostScript
		if ( (whichConverter & 1) > 0) {
			version_str = BatchXSLTGhostScript.get_GS_version();
			return ( version_str );
		}

		// check for pstotext
		if ( (whichConverter & 2) > 0) {
			version_str = BatchXSLTGhostScript.get_pstotext_version();
			return ( version_str );
		}

		// check for ImageMagick
		if ( (whichConverter & 4) > 0) {
			version_str = BatchXSLTImageMagick.get_IMconvert_version();
			return ( version_str );
		}
		
		return ("");
	}

	// directly get info about external converters
	public static String getGhostScriptVersion() {
		return (BatchXSLT.g_mainXSLTFrame.gs_version_str);
	}
	public static String getPsToTextVersion() {
		return (BatchXSLT.g_mainXSLTFrame.pstotext_version_str);
	}
	public static String getImageMagickVersion() {
		return (BatchXSLT.g_mainXSLTFrame.im_version_str);
	}
	public static String getImageMagickVersionNum() {
		return (BatchXSLT.g_mainXSLTFrame.im_version_num);
	}


	/**
	 * JPEG image converter
	 *
	 * example usage of class JpgImage
	 * JpgImage ji = new JpgImage("picture.jpg");
	 * ji.scalePercent(0.5);
	 * ji.cropProportions(5, 7, true);
	 * ji.sendToFile("new_picture.jpg");
	 */
	public static int resize_jpeg(String sourcefileName, String targetfileName, double scale, float quality, int antialias, int numresizeruns) {
		return(resize_jpeg(sourcefileName, targetfileName, scale, quality, antialias, numresizeruns, 0, 0, 0));
	}
	public static int resize_jpeg(String sourcefileName, String targetfileName, double scale, float quality, int antialias, int numresizeruns, int dpi, int exif_imgWidth, int exif_imgHeight) {
		JpgImage ji = null;
		
		if ( (sourcefileName == null) || (sourcefileName.equals("") == true) ) return (-1);
		if ( (targetfileName == null) || (targetfileName.equals("") == true) ) return (-2);

		try {
			ji = new JpgImage(sourcefileName);	// throws IOException, ImageFormatException
		}
		catch (Exception e) { return (-3); }
		//System.out.println("****Image scaling " + sourcefileName + " quality:" + quality + " antialias:" + antialias + " numresizeruns:" + numresizeruns + " dpi:" + dpi);
		// scale it
		ji.scalePercent (scale, antialias, numresizeruns, null);
		// and store it to file
		float myquality = quality;
		if (myquality > 1) myquality = myquality / 100;
		//System.out.println("****Image writing " + targetfileName + " myquality:" + myquality + " dpi:" + dpi);
		try {
			ji.sendToFile (targetfileName, myquality, dpi, exif_imgWidth, exif_imgHeight); 	// throws IOException
		}
		catch (IOException ioe) { return (-4); }
		return (0);
	}
	/*
	 * resize a jpeg image if it is larger than the given maxwidth
	 */
	public static int resize_jpeg_maxwidth(String sourcefileName, String targetfileName, int maxwidth, float quality, int antialias, int numresizeruns, int dpi) {
		JpgImage ji;
		//System.out.println("****Image " + sourcefileName + " maxwidth:" + maxwidth + " quality:" + quality + " antialias:" + antialias + " numresizeruns:" + numresizeruns + " dpi:" + dpi);
		
		if ( (sourcefileName == null) || (sourcefileName.equals("") == true) ) return (-1);
		if ( (targetfileName == null) || (targetfileName.equals("") == true) ) return (-2);

		try {
			ji = new JpgImage(sourcefileName);	// throws IOException, ImageFormatException
		}
		catch (Exception e) { return (-3); }

		int currentWidth = ji.getWidth();
		// scale it if larger
		//System.out.println("Image currentWidth: " + currentWidth + " resize to " + maxwidth);
		if (currentWidth > maxwidth) {
			double scale = (double)maxwidth / (double)currentWidth;
			//System.out.println("Image resizing scale: " + scale);
			ji.scalePercent (scale, antialias, numresizeruns,null);
		}
		
		//System.out.println("Image newWidth: " + ji.getWidth());
		// and store it to file
		float myquality = quality;
		if (myquality > 1) myquality = myquality / 100;
		try {
			ji.sendToFile (targetfileName, myquality, dpi); 	// throws IOException
		}
		catch (IOException ioe) { return (-4); }
		return (0);
	}






	/**
	 * PDF Text Extractor image converter
	 *
	 */
	public static String PDFtextExtract(String sourcePDFpath) {
		if (sourcePDFpath == null || (sourcePDFpath.equals("") == true)) return("");
		return(PDFtextExtract(sourcePDFpath, 1, Integer.MAX_VALUE, 0, "UTF-8", "", 0));
	}
	public static String PDFtextExtract(String sourcePDFpath, int verbose) {
		if (sourcePDFpath == null || (sourcePDFpath.equals("") == true)) return("");
		return(PDFtextExtract(sourcePDFpath, 1, Integer.MAX_VALUE, 0, "UTF-8", "", verbose));
	}

	public static String PDFtextExtract(String sourcePDFpath, int startPage, int endPage, int sort, String encoding, String password, int verbose) {
		if (verbose > 0) {
			System.out.println( "**** PDFtextExtract" );
			System.out.println( "     sourcePDFpath: " + sourcePDFpath);
			System.out.println( "     startPage: " + startPage);
			System.out.println( "     endPage: " + endPage);
			System.out.println( "     sort: " + sort);
			System.out.println( "     encoding: " + encoding);
			System.out.println( "     password: " + password);
			System.out.println( "     verbose: " + verbose);
		}

		if (sourcePDFpath == null || (sourcePDFpath.equals("") == true)) return("");
		String pdftext = "";
		int mystartPage = 1;
        int myendPage = Integer.MAX_VALUE;
		if (startPage > 0) mystartPage = startPage;
		if (endPage > 0) myendPage = endPage;

	
		PDDocument document = null;

		try {
			try {	//basically try to load PDF file from a url first and if the URL
				//is not recognized then try to load it from the file system.
				URL url = new URL( sourcePDFpath );
				document = PDDocument.load( url );
				String fileName = url.getFile();
			}
			catch ( MalformedURLException e ) {
				document = PDDocument.load( sourcePDFpath );
			}

			if( document.isEncrypted() ) {
				StandardDecryptionMaterial sdm = new StandardDecryptionMaterial( password );                    
				document.openProtection( sdm );
				AccessPermission ap = document.getCurrentAccessPermission();

				if( ! ap.canExtractContent() ) {
					throw new IOException( "No permission to extract text from PDF file: " + sourcePDFpath);
				}
			}

			// finally extract the text
			PDFTextStripper stripper = new PDFTextStripper();
			if (sort > 0) stripper.setSortByPosition( true );
			else stripper.setSortByPosition( false );
			stripper.setStartPage( startPage );
			stripper.setEndPage( endPage );
			pdftext = stripper.getText( document );
			if (verbose > 0)  System.out.println( "**** Text: " + pdftext);

		} catch (Exception e) {
			if (verbose > 0)  System.out.println( "#### Exception: " + e);
		}

		try {
			if( document != null ) {
				document.close();
			}
		} catch (IOException ioe) {}
		return(pdftext);
	}

	/**
	 * PDF pages merger
	 *
	 */
	private static String PDFmergeError = "";
	public static String getPDFmergeError() {
		return(PDFmergeError);
	}
	/**
	 * PDF pages merger
	 *
	 * @param args a comma separated list of pdf file path/names and last entry is the output PDF file
	 * @return 0 = no error, -1 = not enough arguments, -2 = Exception while merging PDFs
	 */
	public static int PDFmerge( String args ) {
		String[] argsarr = args.split(",");
		return(PDFmerge(argsarr));
	}
	public static int PDFmerge( String[] args ) {
		PDFmergeError = "";
		String destinationFileName = "";
		String sourceFileName = null;
		if ( args.length < 3 ) {
			PDFmergeError = "#ERROR# PDFmerge: not enough arguments. Usage: PDFMerger(String[]) - 2 docs to merge, last ist destination pdf name";
			return(-1);	// not enough arguments
		}

		PDFMergerUtility merger = new PDFMergerUtility();
		for( int i=0; i<args.length-1; i++ ) {
			sourceFileName = args[i];
			merger.addSource(sourceFileName);
		}

		destinationFileName = args[args.length-1];
		merger.setDestinationFileName(destinationFileName);

		try {
			merger.mergeDocuments();
		} catch (Exception ex) {
			PDFmergeError = ex.getMessage();
			return(-2);
		}
		return(0);
	}

	/**
	 * Clean string from characters which are invalid for XML
	 *
	 * @param inputString String the string to clean
	 * @return The cleaned string
	 */
	public static String cleanXMLstring( String inputString ) {
		if ((inputString == null) || (inputString.equals(""))) return "";
		
		int s;
		int cr;
		StringBuilder sb = new StringBuilder(inputString);
		char[] chars2remove = { '\u0000',
								'\u0001',
								'\u0002',
								'\u0003',
								'\u0004',
								'\u0005',
								'\u0006',
								'\u0007',
								'\u0008',
								'\u000b',
								'\u000c',
								'\u000e',
								'\u000f',
								'\u0010',
								'\u0011',
								'\u0012',
								'\u0013',
								'\u0014',
								'\u0015',
								'\u0016',
								'\u0017',
								'\u0018',
								'\u0019',
								'\u001a',
								'\u001b',
								'\u001c',
								'\u001d',
								'\u001e',
								'\u001f'
								};
		for ( s = 0; s < sb.length(); s++ ) {
			for ( cr = 0; cr < chars2remove.length; cr++) {
				if (sb.charAt(s) == chars2remove[cr]) sb.setCharAt(s,' ');
				else if (sb.charAt(s) == '<') { if (sb.capacity() < (sb.length() + 3)) sb.ensureCapacity(sb.capacity() + 50); sb.replace(s,s+1,"&lt;"); s += 4; }
				else if (sb.charAt(s) == '>') { if (sb.capacity() < (sb.length() + 3)) sb.ensureCapacity(sb.capacity() + 50); sb.replace(s,s+1,"&gt;"); s += 4; }
				else if (sb.charAt(s) == '&') { if (sb.capacity() < (sb.length() + 4)) sb.ensureCapacity(sb.capacity() + 50); sb.replace(s,s+1,"&amp;"); s += 5; }
			}
		}

		return sb.toString();
	}

	private static String metaExtractError = "";
	private static String metaExtractData = "";
	public static String getMetaExtractorError() {
		return metaExtractError;
	}
	public static String getMetaData() {
		return getMetaData(true);
	}
	public static String getMetaData(boolean clean) {
		if (clean == true) return cleanXMLstring(metaExtractData);
		return metaExtractData;
	}
	/**
	 * Image metadata extractor
	 *
	 * @param args a comma separated list of options like '--meta-data' and as last entry a file path/name to extract meta data from
	 * @return 0 = no error, -1 = not enough arguments, -2 = Exception while merging PDFs
	 */
	public static int extractMetaData( String args ) {
		String[] argsarr = args.split(",");
		return extractMetaData(argsarr);
	}
	/**
	 * Image metadata extractor
	 *
	 * @param args String[] array of options like '--meta-data' and as last entry a file path/name to extract meta data from
	 * @return 0 = no error, -1 = not enough arguments, -2 = Exception while merging PDFs
	 */
	public static int extractMetaData( String[] args ) {
		metaExtractError = "";
		metaExtractData = "";
		String metaData = "";
		// comment the following line if Tika meta data extract is needed
		if (metaData.equals("") == true) return 0;  //  if tika is not needed we always return nothing

		if ((args == null) || args.length < 1) {
			metaExtractError = "No path to input file given.";
			return -1;
		}


		// redirect System.out
		// Create a stream to hold the output
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		// IMPORTANT: Save the old System.out!
		PrintStream old = System.out;
		// Tell Java to use your special stream
		System.setOut(ps);
		try {
		  // uncomment if you need Tika
			//TikaCLI.main(args);
		} catch (Exception ex) {
			metaExtractError = ex.getMessage();
			System.out.flush();
			System.setOut(old);
			return -2 ;
		} 
		System.out.flush();
		System.setOut(old);
		try {
			metaExtractData = baos.toString("UTF-8");
		} catch (UnsupportedEncodingException ex) {
			metaExtractError = "Unsupported ecoding: UTF-8";
			return -3;
		}

		return 0;
	}


	
	/**
	 * NZIP Expander
	 *
	 */
	private static String NZIPexpandError = "";
	public static String getNZIPexpandError() {
		return(NZIPexpandError);
	}
	/**
	 * NZIP expand a file
	 *
	 * @param source the path to the ZIP file to expand
	 * @param theTarget the path to the expanded file
	 */
	public static String nzip(String source, String theTarget) {
		return( nzip(source, theTarget, false));
	}
	public static String nzip(String source, String theTarget, Boolean showmessage) {
		NZIPexpandError = "";
		String target = theTarget;

		if (showmessage == true) BatchXSLT.g_mainXSLTFrame.showMess( "*** nzip: '" + source + "' to '" + target + "'\n" );
		String[] cmd = new String[3];	// set up the command to unzip idml archive
		cmd[0] = "cp";		// copy
		cmd[1] = source;	// source file (may be an archive to unizip or a folder structure to zip)
		cmd[2] = target;	// target (will be the unzipped folder structure or the compressed archive file)
		
		de.schlichtherle.NZip nzip = new NZip();
		try {
			nzip.runWithException(cmd);
		} catch (IOException ioe) {
			NZIPexpandError = "NZip failed to expand '" + source + "'";
			return("");
		}
		return(target);
	}




	/**
	 * IDML special stuff
	 *
	 * 
	 */
	private static String IDMLexpandError = "";
	private static int IDMLexpandErrorNum = 0;
	private static String IDMLexpandParentFolder = "";
	private static String IDMLexpandFolderName = "";
	private static String IDMLmergedPath = "";
	private static String IDMLmergedXMLName = "";
	public static String getIDMLexpandError() {
		return(IDMLexpandError);
	}
	public static int getIDMLexpandErrorNum() {
		return(IDMLexpandErrorNum);
	}
	public static String getIDMLexpandParentFolder() {
		return(IDMLexpandParentFolder);
	}
	public static String getIDMLexpandFolderName() {
		return(IDMLexpandFolderName);
	}
	public static String getIDMLmergedPath() {
		return(IDMLmergedPath);
	}
	public static String getIDMLmergedXMLName() {
		return(IDMLmergedXMLName);
	}
	public static String mergeIDML(String theSourcePath, String theSourceIDMLname, String theExpandPath, String theTargetPath, String theTargetName, int showmessage) {
		IDMLexpandError = "";
		IDMLexpandErrorNum = 0;
		IDMLexpandParentFolder = "";
		IDMLexpandFolderName = "";
		IDMLmergedPath = "";
		IDMLmergedXMLName = "";
		String sourcePath = theSourcePath;
		int error = 0;
		if (sourcePath.endsWith(File.separator) == false) sourcePath = sourcePath + File.separator;
		String targetPath = theExpandPath;
		if (targetPath.equals("")) targetPath = System.getProperty("java.io.tmpdir");
		if (targetPath.endsWith(File.separator) == false) targetPath = targetPath + File.separator;

		String[] fname = theSourceIDMLname.split(".idml");
		String targetName = theTargetName;
		if (targetName.equals("")) targetName = fname[0];

		// if the unzipped folder structure exists, we have to delete it before unzipping again
		File fldr = new File(targetPath + targetName);
		if (fldr.exists()) {
			deleteDirectory(fldr);
		}
		// unzip the idml file
		String fldrODF = com.epaperarchives.batchxslt.utils.nzip(sourcePath + theSourceIDMLname, targetPath + targetName, false);
		if (fldrODF.equals("") == false) {
			IDMLexpandParentFolder = targetPath;
			if (IDMLexpandParentFolder.endsWith(File.separator) == false) IDMLexpandParentFolder += File.separator;
			IDMLexpandFolderName = targetName;
			// check if the expanded stuff is a folder structure
			File unzippedFile = new File(IDMLexpandParentFolder + IDMLexpandFolderName);
			if (!unzippedFile.isDirectory()) {	// if not a directory: error
				IDMLexpandErrorNum = error = 2;
			}
		}
		else {	// nzip could not unzip file
			IDMLexpandErrorNum = error = 1;
		}
		if (error != 0) {
			IDMLexpandError = NZIPexpandError;
			if ((showmessage & 2) > 0) {
				switch (error) {
					case 1:
						// IDMLexpandError = NZIPexpandError; is already done
						break;
					case 2:
						IDMLexpandError = "uNZip IDML package did not result in a folder structure";
						break;
				}
			}
			return("");
		}
		// the idml archive is expanded
		// merge all xml files
		// BatchXSLT.g_mainXSLTFrame.showMess( "*** '" + theSourceIDMLname + "' expanded into: '" + IDMLexpandParentFolder + "' name: '" + IDMLexpandFolderName + "'\n" );
		String mergedXMLname = mergeIDMLxmlFiles(IDMLexpandParentFolder, IDMLexpandFolderName, theTargetPath, theTargetName, showmessage);
		
		return( mergedXMLname);
	}
	
	public static String mergeIDMLxmlFiles(String theSourcePath, String theSourceIDMLname, String theTargetPath, String theTargetName, int showmessage) {
		String mergedXMLname = theTargetName;
		if (mergedXMLname.equals("")) mergedXMLname = theSourceIDMLname + ".imx";

		// check if the container.xml file exists in the extracted folder structure
		String sourcePath = theSourcePath;
		if (sourcePath.endsWith(File.separator) == false) sourcePath = sourcePath + File.separator;
		String IDMLroot = sourcePath + theSourceIDMLname;
		if (IDMLroot.endsWith(File.separator) == false) IDMLroot = IDMLroot + File.separator;
		String containerXML = "META-INF/container.xml";
		String containerXMLpathname = IDMLroot + containerXML;
		if (!new File(containerXMLpathname).exists()) {
			IDMLexpandError = "IDML container XML file not found: '" + containerXML + "'";
			if ((showmessage & 2) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "### ERROR: " + IDMLexpandError + "\n" );
			return("");
		}
		String mergeXSLfileName = BatchXSLT.g_mainXSLTFrame.appDir;
		if (mergeXSLfileName.endsWith(File.separator) == false) mergeXSLfileName = mergeXSLfileName + File.separator;
		mergeXSLfileName += "XSL/IDMLmerge.xsl";
		int retval = XSLtransform( IDMLroot, containerXML, mergeXSLfileName, theTargetPath, mergedXMLname, showmessage, true, true, false);

		if (retval == 0) {
			IDMLmergedPath = theTargetPath;
			IDMLmergedXMLName = mergedXMLname;
			//if ((showmessage & 1) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "*** '" + theSourceIDMLname + "' merged into: '" + theTargetPath + "' name: '" + mergedXMLname + "'\n" );
		}
		return( mergedXMLname);
	}

	public static int XSLtransform(String XMLpath, String XMLname, String XSLpathname, String outPath, String outName ) {
		int error = XSLtransform( XMLpath, XMLname, XSLpathname, outPath, outName, 3, true, true, false );
		return(error);
	}
	public static int XSLtransform(String XMLpath, String XMLname, String XSLpathname, String outPath, String outName, int showmessage, Boolean XIncludeAware, Boolean NamespaceAware, Boolean Validating ) {
		int error = 0;
		File				my_source_file = null;
		StreamSource		my_stream_source = null;	// ... this file as StreamSource
		StreamResult		my_stream_result = null;
		FileOutputStream	outputStream = null;
		String				outputFileName = "";
		Transformer transformer = null;
		TransformerFactory tFactory = null;

		//BatchXSLT.g_mainXSLTFrame.showMess( "**** XSLtransform XMLpath: " + XMLpath + "\n     XMLname: " + XMLname + "\n     XSLpathname: " + XSLpathname + "\n" );

		do {
			// ******** load the XML
			String sourceXMLpath = XMLpath;
			if (sourceXMLpath.endsWith(File.separator) == false) sourceXMLpath = sourceXMLpath + File.separator;
			String sourceXMLpathname = sourceXMLpath + XMLname;
			if (!new File(sourceXMLpathname).exists()) {
				error = 1;
				if ((showmessage & 2) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "### ERROR " + error + " XML not found: " + sourceXMLpathname + "\n" );
				break;
			}
			my_source_file = new File(sourceXMLpathname);	// must create from a file because 'getAssociatedStylesheet' will not work otherwise!
			my_stream_source = new StreamSource(my_source_file);
			if ((showmessage & 1) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "Transforming XML: " + sourceXMLpathname + "\n" );

			try {
				tFactory = TransformerFactory.newInstance();
			}
			catch (Exception tfe) {
				error = 60;
				BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " TransformerFactory.newInstance: '" + tfe.getMessage() + "'\n" );
				tfe.printStackTrace();
				break;
			}
			if (tFactory == null) {
				error = 601;
				BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " TransformerFactory Error. TransformerFactory is NULL\n" );
				break;
			}
			
			// ******** load the XSL
			if (XSLpathname.equals( "" ) == true) {	// no external XSL given: use the internal
				Source stylesheet = null;
				String media= null , title = null, charset = null;
				
				try {
					stylesheet = tFactory.getAssociatedStylesheet( my_stream_source, media, title, charset );
				}
				catch (TransformerConfigurationException e) {
					error = 20;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " External Stylesheet Exception: '" + e.getMessage() + "'\n" );
					e.printStackTrace();
					break;
				}
				if ( stylesheet == null) {
					error = 21;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " No associated XSL found'\n" );
					break;
				}
				try { transformer = tFactory.newTransformer( stylesheet ); }
				catch (TransformerConfigurationException e) {
					error = 22;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " Transformer ConfigurationException when loading XSL: '" + e.getMessage() + "'\n" );
					break;
				}
				if (transformer == null) {
					error = 221;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " Transformer Instance Error. Transformer is NULL\n" );
					break;
				}
			}
			else {	// use the given external XSL
				StreamSource xslt_inputstream = null;
				if (!new File(XSLpathname).exists()) {
					error = 2;
					if ((showmessage & 2) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "### ERROR " + error + " XSL not found: " + XSLpathname + "\n" );
					break;
				}
				try {
					xslt_inputstream = new StreamSource( new FileInputStream(XSLpathname) );
					xslt_inputstream.setSystemId(new File(XSLpathname));
				}
				catch (IOException e) { // should not happen because we have tested if file exists
					error = 23;
					if ((showmessage & 2) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " Loading XSL :'" + XSLpathname + "'\n" );
					break;
				}
				if (xslt_inputstream == null) {
					error = 231;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " Transformer XSL Error. xslt_inputstream is NULL for XSL file: " + XSLpathname + "\n" );
					break;
				}
   				try {
					transformer = tFactory.newTransformer( xslt_inputstream );
				}
				catch (Exception e) {
					error = 24;
					if ((showmessage & 2) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " Transformer ConfigurationException when loading XSL: '" + e.getMessage() + "'\n" );
					break;
				}
				if (transformer == null) {
					error = 241;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " Transformer Instance Error. Transformer is NULL\n" );
					break;
				}
			}
			if ((showmessage & 1) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "       using XSL: " + XSLpathname + "\n" );

			// ******** prepare output stream
			outputFileName = outPath;
			if ((outputFileName.equals("") == false) && (outputFileName.endsWith(File.separator) == false)) outputFileName = outputFileName + File.separator;
			outputFileName += outName;
			if (outputFileName.equals("*NONE*") == false) {		// create NO output file (validating only or done by xsl)
				try { outputStream = new FileOutputStream(outputFileName); }
				catch (java.io.IOException e) {
					error = 30;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " FileOutputStream IOExceptio: '" + e.getMessage() + "'\n" );
					break;
				}
				my_stream_result = new StreamResult(outputStream);
				if (my_stream_result == null) {
					error = 31;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " Output StreamResult is NULL\n" );
					break;
				}
			}
			else {			// no output
				my_stream_result = new StreamResult(new NullOutputStream());
			}

			// ******** pass parameters to the stylesheet
			try {
				transformer.setParameter("XMLpath",XMLpath);
				transformer.setParameter("XMLname",XMLname);
			}
			catch (Exception e) {
				error = 40;
				BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " Transformer Parameter: '" + e.getMessage() + "'\n" );
				break;
			}
			
			// ******** start the transform
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			try { docFactory.setXIncludeAware(XIncludeAware); } catch (Exception xia) {}
			try { docFactory.setNamespaceAware(NamespaceAware); } catch (Exception nsa) {}
			try { docFactory.setValidating(Validating); } catch (Exception vali) {}
			// equal to above try { docFactory.setFeature("http://xml.org/sax/features/namespaces", false); } catch (ParserConfigurationException e) { System.err.println("could not set parser feature"); }
			
			DocumentBuilder docBuilder = null;
			Document xmlSourceDocument = null;
			try { docBuilder = docFactory.newDocumentBuilder(); }
			catch (Exception dbex) {
				error = 41;
				BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR " + error + " in creating DocumentBuilder for source document.\n" );
				dbex.printStackTrace();
				break;
			}
			try { xmlSourceDocument = docBuilder.parse(my_source_file); }
			catch (Exception srcex) {
				error = 42;
				System.out.println( "### ERROR " + error + " in DocumentBuilder parsing source document: '" + sourceXMLpathname + "'");
				srcex.printStackTrace();
				break;
			}
			try {
				if (xmlSourceDocument != null) transformer.transform( new DOMSource(xmlSourceDocument), my_stream_result );
				else transformer.transform( my_stream_source, my_stream_result );
			}
			catch (Exception e) {
				if (transformer != null) transformer.clearParameters();
				error = 43;
				BatchXSLT.g_mainXSLTFrame.showMess( "\n#### ERROR " + error + " Transformer Exception: '"+ e.getMessage() + "'\n" );
				e.printStackTrace();
				break;
			}
			break;
			
		} while(false);

		// do some cleanup
		if (transformer != null) {
			transformer.clearParameters();
		}

		my_stream_result = null;
		transformer = null;
		if ((my_stream_result != null) || (outputStream != null)) {
			try { outputStream.flush(); }
			catch ( java.io.IOException e ) { }
			try { outputStream.close(); }
			catch ( java.io.IOException e ) { }
		}
		
		if (showmessage > 0) {
			if ((error == 0) && ((showmessage & 1) > 0)) BatchXSLT.g_mainXSLTFrame.showMess( "Transform complete.\n" );
			else if ((showmessage & 2) > 0) BatchXSLT.g_mainXSLTFrame.showMess( "### ERROR " + error + " during transform\n" );
		}

		return(error);
	}
	
	/*************************
	 ZIP stuff
	*/
	public static byte[] compressZIP(String instring, int compression) {
		if ((instring == null) || (instring.equals("") == true)) return(null);
		Deflater compressor = new Deflater();
		switch(compression) {
			case 0: compressor.setLevel(Deflater.DEFAULT_COMPRESSION); break;
			case 1: compressor.setLevel(Deflater.BEST_COMPRESSION); break;
			case 2: compressor.setLevel(Deflater.BEST_SPEED); break;
			default: compressor.setLevel(Deflater.DEFAULT_COMPRESSION); break;
		}
		// Give the compressor the data to compress
		byte[] input = null;
		//System.out.println( "---- compressZIP() compressing: '" + instring + "'");
		//System.out.println( "---- compressZIP() BEFORE compressing " + instring.length() + " bytes'");
		try { input = instring.getBytes("UTF-8"); }
		catch ( UnsupportedEncodingException e ) {
			System.out.println( "### ERROR in compressZIP(): '" + e.getMessage() + "'");
			return(null);
		}
		compressor.setInput(input);
		compressor.finish(); 
		// Create an expandable byte array to hold the compressed data.
		// You cannot use an array that's the same size as the orginal because
		// there is no guarantee that the compressed data will be smaller than
		// the uncompressed data.
		ByteArrayOutputStream bos = new ByteArrayOutputStream(instring.length()); 
		// Compress the data
		byte[] output = new byte[1024];
		while (!compressor.finished()) {
			int count = compressor.deflate(output);
			bos.write(output, 0, count);
		}
		compressor.end(); 
		try { bos.close(); } catch (IOException e) { } 
		//System.out.println( "---- compressZIP() compressed size: '" + bos.size() + "'");
		//System.out.println( "---- compressZIP() compressed: '" + bos.toString() + "'");
		return(bos.toByteArray());
	}
	
	public static String decompressZIP(byte[] inbytes) {
		if ((inbytes == null) || (inbytes.length == 0)) return("");
		//System.out.println( "---- decompressZIP() compressed: '" + inbytes.toString() + "'");
		Inflater decompressor = new Inflater(); 
		decompressor.setInput(inbytes);
		// Create an expandable byte array to hold the decompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(inbytes.length);
		// Decompress the data
		byte[] buf = new byte[1024];
		while (!decompressor.finished()) {
			try {
				int count = decompressor.inflate(buf);
				bos.write(buf, 0, count);
			} 
			catch (DataFormatException e) {
				System.out.println( "### ERROR while decompressing in decompressZIP(): '" + e.getMessage() + "'");
				return("");
			}
		}
		try { bos.close(); } catch (IOException e) { }
		String retstr = null;
		try {
			retstr = bos.toString("UTF-8");
		}
		catch (Exception e) {
			System.out.println( "### ERROR at data return in decompressZIP(): '" + e.getMessage() + "'");
			return("");
		}
		//System.out.println( "---- decompressZIP() decompressed: '" + retstr + "'");
		return(retstr);
	}
	
	public static int testZIPstorage(String str) {
		System.out.println("*** calling testZIPstorage() with string: '" + str + "'");
		publicStorageArray_init();
		publicStorageArray_addString(str, true,0);
		publicStorageArray_addString(str, true,1);
		publicStorageArray_addString(str, true,2);
		int numElements = publicStorageArray_size();
		System.out.println("    added " + numElements + " zipped strings.'");

		for (int i = 0; i < numElements; i++) {
			String decompressed = publicStorageArray_getString(i, true);
			if (decompressed.equals(str) == true) {
				System.out.println("*** testZIP() returned from storage index # "+i+": '" + decompressed + "'");
				System.out.println("          input size: " + str.length() + " bytes.'");
				System.out.println("   decompressed size: " + decompressed.length() + " bytes.'");
			}
		}
		
		publicStorageArray_init();
		return(0);
	}

	
	/*************************
	ArrayList to store strings, zipped strings and other stuff
	 */
	// Create the ArrayList for it is ready to be used
	public static ArrayList publicStorageArray = new ArrayList();
	public static int publicStorageArray_init() {
		//publicStorageArray = new ArrayList();
		publicStorageArray.clear();
		System.gc();
		return(0);
	}
	public static int publicStorageArray_size() {
		return(publicStorageArray.size());
	}
	public static int publicStorageArray_addString(String thestring, boolean zip,int compression) {
		//System.out.println( "---- publicStorageArray_addString() ZIP: '" + zip + ", compression: " + compression);
		if (zip) publicStorageArray.add(compressZIP(thestring,compression));
		else publicStorageArray.add(thestring);
		return(publicStorageArray.size());
	}
	public static int publicStorageArray_setString(int index, String thestring, boolean zip,int compression) {
		if (zip) publicStorageArray.set(index,compressZIP(thestring,compression));
		else publicStorageArray.set(index,thestring);
		return(publicStorageArray.size());
	}
	public static String publicStorageArray_getString(int theindex, boolean dezip) {
		int index = theindex;
		if (index < 0) index = 0;
		if (index >= publicStorageArray.size()) index = publicStorageArray.size()-1;
		if (dezip) {
			return( decompressZIP((byte[])publicStorageArray.get(index)) );
		}
		else return(publicStorageArray.get(index).toString());
	}
	public static void publicStorageArray_remove(int theindex) {
		if ((theindex < 0) || (theindex >= publicStorageArray.size())) return;
		try { publicStorageArray.remove(theindex); } catch(Exception e){}
		return;
	}
	public static String publicStorageArray_serialize( boolean dezip, String delimiter) {
		int size = publicStorageArray.size();
		if (size <= 0) return("");
		String serialized = "";
		for (int i = 0; i < size; i++) {
			if (dezip == true) serialized += decompressZIP((byte[])publicStorageArray.get(i));
			else serialized += (String)publicStorageArray.get(i);
		}
		return(serialized);
	}




	
	/*************************
	 * Dump an object
	 */
	public static String var_dump( Object o ) {
	    return new ToStringBuilder(o,ToStringStyle.SHORT_PREFIX_STYLE).toString();
	}

}



