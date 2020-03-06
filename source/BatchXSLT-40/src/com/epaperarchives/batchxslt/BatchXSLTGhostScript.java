package com.epaperarchives.batchxslt;



import java.io.*;






/* ===========================================================
 * interact with GhostScript
 */
public class BatchXSLTGhostScript
{
	public BatchXSLTGhostScript() {
	}

	// check if GhostScript is reachable
	public static String get_GS_version() {
		return(get_GS_version(false));
	}
	// check if GhostScript is reachable
	public static String get_GS_version(Boolean setEnvirs) {
	    int exitVal = 0;
	    String[] envir = null;
		String osName = mainXSLTFrame.systemOSname;
		String osVersion = mainXSLTFrame.systemOSversion;
		String osArch = mainXSLTFrame.systemOSarchitecture;
		boolean from_package = false;
		boolean exists = false;
		String GS_ver = "";			// the version string returned from Ghostsscript
		String GS_pgm = "";			// the full path/name to the Ghostsscript program like "c:\program file\gs\gs8.70\bin\gsdll32.dll"
		String GS_pgm_bin = "";		// the path to the Ghostsscript program bin folder like "c:\program file\gs\gs8.70\bin"
		String GS_envir = "";		// the environment variables to set (except PATH) like "GS_LIB=C:\Programme\gs\gs8.70\lib;C:\Programme\gs\fonts"
		String GS_HOME = "";		// the main path like "c:\program file\gs\gs8.70" containing the folder 'lib' and 'bin' 'fonts'...
		String GS_PATH_envir = "";	// the PATH environment variable path like "c:\program file\gs\gs8.70\bin"
		String GS_DLL = "";			// the full path/name to the dll like: "C:\Programme\gs\gs8.70\bin\gsdll32.dll"
		String GS_LIB = "";			// environment like: "GS_LIB=C:\Programme\gs\gs8.70\bin;C:\Programme\gs\fonts"

		String GS_PATH = "";	// a path from the PATH environment variable path like "c:\program file\gs\gs8.70\bin"

		String reg_exe_path = "";
		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("****** getting Ghostscript version ------------");

		// get the path to reg.exe to be able to query the registry
		if ( osName.toLowerCase().indexOf( "windows" ) >= 0 ) {
			reg_exe_path = utils.findFileInEnvirPath(mainXSLTFrame.envirPath, "reg.exe", "", "system");
			if (reg_exe_path.equals("") == false) reg_exe_path += "reg.exe";
			else {	// get reg.exe from package
				reg_exe_path = BatchXSLT.g_mainXSLTFrame.userDir + File.separator + "Utilities" + File.separator + "Helpers" + File.separator + "utils" + File.separator + "reg.exe";
				if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("REG.EXE PackagePATH: " + reg_exe_path);
				exists = utils.existsFile(reg_exe_path);	// test if program is contained in the package
				if (exists == false) reg_exe_path = "";		// not found

			}
			if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("REG.EXE PATH: " + reg_exe_path);
		}

		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("******* searching for Ghostscript");
		if (mainXSLTFrame.gs_pgm_path.equals("") == false) {	//  setup the indicated program if already stated in autostart.jt
			GS_pgm = mainXSLTFrame.gs_pgm_path;
			GS_PATH_envir = mainXSLTFrame.gs_pgm_path_envir;
			GS_envir = mainXSLTFrame.gs_envir;
			if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   !!!! SET IN JobTicket");
		}
		else {	// try to find Ghostscript program
			do {
				if (BatchXSLT.g_mainXSLTFrame.gs_pgm_name.equals("") == false) {
					GS_pgm = BatchXSLT.g_mainXSLTFrame.gs_pgm_name;
				}
				else {
					if ( osName.toLowerCase().startsWith( "windows" ) == true ) {	// like Windows NT, 2000, XP....
						GS_pgm = "gswin64c.exe";
						GS_DLL = "gsdll64.dll";
					}
					else {	// for unix like systems
						GS_pgm = "gs";
					}
				}

				// ------------------------------------------
				// get path to Ghostscript
				String pgm_path = "";
				// ------------------------------------------
				if (BatchXSLT.g_mainXSLTFrame.gs_pgm_installedpath.equals("") == false) {
					GS_HOME = BatchXSLT.g_mainXSLTFrame.gs_pgm_installedhome;
					pgm_path = BatchXSLT.g_mainXSLTFrame.im_pgm_installedpath;
					if (pgm_path.endsWith(File.separator) == false) pgm_path += File.separator;
					pgm_path += GS_pgm;
				}
				else {
					// try to find Ghostscript in the package installed in appDir
					pgm_path += mainXSLTFrame.appDir;
					if (pgm_path.endsWith(File.separator) == false) pgm_path += File.separator;
					pgm_path += "Utilities" + File.separator + "Imaging";	// like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging

					// ad pgm folder like 'Ghostscript'
					pgm_path += File.separator + "Ghostscript";
					// pgm_path is now like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging/Ghostscript

					// set app and Environment
					if ( osName.toLowerCase().indexOf( "windows" ) < 0 ) {
					  // OSX Ghostscript app
					  // Utilities/Imaging/Ghostscript/
					  //                              |_ gs.app/
					  //                                      |_ Contents/
					  //                                                 |_ Home/
					  //                                                        |_ bin/
					  //                                                              |_ gs
						pgm_path += File.separator + "gs.app" + File.separator + "Contents" + File.separator + "Home";  // pgm_path is now like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging/Ghostscript/gs.app/Contents/Home
						GS_HOME = pgm_path;
						// ad executable path/name
						pgm_path += File.separator + "bin";
						GS_PATH_envir = pgm_path;
						GS_pgm_bin = pgm_path;
						pgm_path += File.separator + GS_pgm;  // pgm_path is now like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging/Ghostscript/gs.app/Contents/Home/bin/gs
					}
					else {
						GS_HOME = pgm_path;
						//GS_DLL = GS_HOME + File.separator + "bin" + File.separator + GS_DLL;
						pgm_path += File.separator + "bin";
						GS_pgm_bin = pgm_path;
						pgm_path += File.separator + GS_pgm;  // pgm_path is now like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging/Ghostscript/bin/gs.exe
					}

					// test if is contained in the package
					if (pgm_path.equals("") == false) {
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   ---- GS test executable exists: " + pgm_path);
						exists = utils.existsFile(pgm_path);	// test if program is contained in the package
						if (exists == true) {	// we got it in the package!
							from_package = true;
							GS_pgm = pgm_path;
							GS_PATH_envir = pgm_path + File.separator + "bin";
							GS_DLL = GS_HOME + File.separator + "bin" + File.separator + GS_DLL;
							if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   !!!! FOUND");
							break;
						}
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   !!!! NOT FOUND");

						if ( osName.toLowerCase().indexOf( "windows" ) >= 0 ) {
							// test if 64-bit GS is contained in the package
							GS_pgm = "gswin32c.exe";
							GS_DLL = "gsdll32.dll";
							pgm_path = GS_pgm_bin + File.separator + GS_pgm;
							if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   ---- GS test 32-bit executable exists: " + pgm_path);
							exists = utils.existsFile(pgm_path);	// test if program is contained in the package
							if (exists == true) {	// we got it in the package!
								from_package = true;
								GS_pgm = pgm_path;
								GS_PATH_envir = pgm_path;// + File.separator + "bin";
								GS_DLL = GS_HOME + File.separator + "bin" + File.separator + GS_DLL;
								if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   !!!! FOUND 64-bit");
								break;
							}
							if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   !!!! NOT FOUND");
						}
					}

					// ****************************************
					// ***** WE HAVE NOT FOUND Ghostscript!!!!
					GS_pgm = GS_pgm_bin = GS_HOME = GS_DLL = GS_LIB = GS_PATH_envir = pgm_path = "";
					// ------------------------------------------
					if ( osName.toLowerCase().indexOf( "windows" ) >= 0 ) {	// like Windows NT, 2000, XP....
						// get from WINDOWS Registry
						GS_pgm = "gswin64c.exe";
						GS_DLL = "gsdll64.dll";
						pgm_path = "";
					
						String registryParams = "query \"HKLM\\SOFTWARE\\GPL Ghostscript\\" + BatchXSLT.g_mainXSLTFrame.gs_version_num + "\" /v GS_DLL";
						int retval = utils.callExternalApp(reg_exe_path, registryParams);
						String stdinresp = "";
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
							System.out.println("REGISTRY QUERY to find Ghostscript program -------------------------------");
							System.out.println("QUERY KEYS: " + registryParams);
							System.out.println("REGISTRY RETVAL : " + retval);
						}
						stdinresp = utils.callExternalAppGetStdinResponse();
						if (retval == 0) {	// on no error, the registry is set - let's check for correct value
							if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
								System.out.println("REGISTRY QUERY Ghostscript program: " + stdinresp);
							}
							if (stdinresp.equals("") == false) {
								String[] reg_arr = stdinresp.split("\t");
								pgm_path = reg_arr[reg_arr.length - 1];
								pgm_path = pgm_path.replaceAll("\r?\n", "");	// trim line endings
								String path1 = pgm_path.substring(0,pgm_path.lastIndexOf(File.separator));	// strip the dll file name
								path1 = path1.substring(0,path1.lastIndexOf(File.separator));				// strip the bin folder
								String dll_name = pgm_path.substring(pgm_path.lastIndexOf(File.separator)+1);
								if ( dll_name.indexOf( "64" ) >= 0 ) {
									GS_pgm = "gswin64c.exe";
									GS_DLL = "gsdll64.dll";
								}
								pgm_path = path1;
								GS_pgm_bin = pgm_path + File.separator + "bin";
								GS_pgm = GS_pgm_bin + File.separator + GS_pgm;
								if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
									System.out.println("REGISTRY Ghostscript path : " + pgm_path);
									System.out.println("REGISTRY Ghostscript dll_name : " + dll_name);
								}
								exists = utils.existsFile(GS_pgm);	// test if program can be found from Registy BinPath
								if (exists == true) {	// we got it!
									from_package = false;
									if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
										System.out.println("GHOSTSCRIPT found in Registry: '" + GS_pgm + "'");
									}
									GS_HOME = pgm_path;
									GS_PATH_envir = pgm_path + File.separator + "bin";
									GS_DLL = GS_HOME + File.separator + "bin" + File.separator + GS_DLL;
									break;
								}
							}
						}
					}

					// still no Ghostscript found
					GS_pgm = GS_pgm_bin = GS_HOME = GS_DLL =  GS_LIB = GS_PATH_envir = pgm_path = "";
					// ------------------------------------------
					// the Utilities path from package does not contain Ghostscript
					// Not found until yet: get from envir PATH or default system path
					if ( osName.toLowerCase().indexOf( "windows" ) >= 0 ) {	// like Windows NT, 2000, XP....
						GS_pgm = "gswin64c.exe";
						GS_DLL = "gsdll64.dll";
						String tmp_pgm = utils.findFileInEnvirPath(mainXSLTFrame.envirPath, GS_pgm, "system");
						if (tmp_pgm.equals("") == false) {
							pgm_path = tmp_pgm;
							GS_PATH = tmp_pgm;
							GS_pgm = tmp_pgm + GS_pgm;
						}
						else {	// try to find the 64 bit version
							GS_pgm = "gswin64c.exe";
							GS_DLL = "gsdll64.dll";
							tmp_pgm = utils.findFileInEnvirPath(mainXSLTFrame.envirPath, GS_pgm, "system");
							if (tmp_pgm.equals("") == false) {
								pgm_path = tmp_pgm;
								GS_PATH = tmp_pgm;
								GS_pgm = tmp_pgm + GS_pgm;
							}
						}
					}
					else {	// for unix like systems
						pgm_path = File.separator + "usr" + File.separator + "local";
						GS_HOME = pgm_path;
						pgm_path +=  File.separator + "bin" + File.separator + GS_pgm;
						// test if exists
						exists = utils.existsFile(pgm_path);	// test if program is mentioned somewhere in PATH
						if (exists == false) {	// not found
							pgm_path = File.separator + "usr";
							GS_HOME = pgm_path;
							pgm_path +=  File.separator + "bin" + File.separator + GS_pgm;
						}
					}
				}

				// Test GS exists at pgm_path
				if (pgm_path.equals("") == false) {
					if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   ---- Test GS exists at pgm_path: " + pgm_path);
					exists = utils.existsFile(pgm_path);	// test if program is contained in the package
					if (exists == true) {	// we got it in the package!
						from_package = false;
						GS_pgm = pgm_path;
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   !!!! FOUND");
						break;
					}
					if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) System.out.println("   !!!! NOT FOUND");
				}
				

				/// not found at all				
				GS_pgm = GS_pgm_bin = GS_HOME = GS_DLL =  GS_LIB = GS_PATH_envir = pgm_path = "";
				break;
			} while(false);
		}

		if (GS_pgm.equals("") == true) return("");

		// set the environment if available
		if ( BatchXSLT.g_mainXSLTFrame.gs_envir.equals("") == false ) {
			envir = BatchXSLT.g_mainXSLTFrame.gs_envir.split("\\+\\+\\+");
		}
		else {
			// prepare the environment
			if ( osName.toLowerCase().indexOf( "windows" ) < 0 ) {	// like unix
				if (GS_envir.equals("") == true) {
					GS_envir = "GS_LIB=";
					GS_envir += GS_HOME + ";";
					GS_envir += GS_HOME + File.separator + "bin;";
					String apath = GS_HOME + File.separator + "lib";
					exists = utils.existsFile(apath);	// check for lib folder
					if (exists) GS_envir += GS_HOME + File.separator + "lib;";
					
					apath = GS_HOME + File.separator + "fonts";
					exists = utils.existsFile(apath);	// check for fonts folder
					if (exists) GS_envir += GS_HOME + File.separator + "fonts;";
				}
			}
			else {	// like Windows
				// we must have for environment:
				// GS_DLL=C:\Programme\gs\gs9.10\bin\gsdll64.dll
				// GS_LIB=C:\Program Files\gs\gs9.10\bin;C:\Program Files\gs\gs9.10\lib;C:\Program Files\gs\gs9.10\fonts
				if ( GS_HOME.equals("") == false ) {
					GS_envir = "GS_DLL=" + GS_DLL;
					// check for lib folder
					String apath = GS_HOME + File.separator + "lib";
					GS_LIB = GS_pgm_bin + ";";	// C:\Programme\gs\gs8.70\bin;
					GS_envir += "+++GS_LIB=" + GS_LIB;
					exists = utils.existsFile(apath);	// check for lib folder
					if (exists) {
						//GS_LIB += GS_LIB + apath + ";";	// for registry key GS_LIB = C:\Programme\gs\gs8.70\lib;
						GS_LIB += apath + ";";	// for registry key GS_LIB = C:\Programme\gs\gs8.70\lib;
						GS_envir += apath + ";";
					}
					// check for fonts folder
					apath = GS_HOME + File.separator + "fonts";
					exists = utils.existsFile(apath);
					if (exists) {
						GS_LIB += apath + ";";	// GS_LIB=C:\Programme\gs\gs8.70\lib;C:\Programme\gs\fonts
						GS_envir += apath + ";";
					}
					apath = GS_HOME + File.separator + "Resource";
					exists = utils.existsFile(apath);	// check for Resource folder
					if (exists) {
						GS_LIB += apath + ";";	// GS_LIB=C:\Programme\gs\gs8.70\lib;C:\Programme\gs\fonts;C:\Programme\gs\Resource
						GS_envir += apath + ";";
					}
				}
/*
				// set registry keys if reg.exe is reachable
				if ( (reg_exe_path.equals("") == false) 
						&& (BatchXSLT.g_mainXSLTFrame.gs_version_num.equals("") == false) ) {
					String registryParams = "query \"HKLM\\SOFTWARE\\GPL Ghostscript\\" + BatchXSLT.g_mainXSLTFrame.gs_version_num;
					int retval = utils.callExternalApp(reg_exe_path, registryParams);
					String stdinresp = "";
					if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
						System.out.println("REGISTRY QUERY Ghostscript -------------------------------");
						System.out.println("QUERY KEYS: " + registryParams);
						System.out.println("REGISTRY RETVAL : " + retval);
					}
					stdinresp = utils.callExternalAppGetStdinResponse();
					if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
						System.out.println("REGISTRY STDIN: " + stdinresp);
						String errorresp = utils.callExternalAppGetErrorResponse();
						if (errorresp.equals(stdinresp) == false) System.out.println("REGISTRY ERROR: " + errorresp);
						System.out.println("REGISTRY QUERY end -------------------------------");
					}
					// check if the correct path is reported
					if (retval == 0) {	// on no error, the registry is set - let's check for correct value
						if (stdinresp.toLowerCase().indexOf(GS_DLL.toLowerCase()) < 0) retval = 1;	// set to error for we can set to correct value below
					}
					// set registry entry - or our Ghostscript in the package calls wrong version
					if ( (from_package == true) && (retval != 0) ) {
						registryParams = "add \"HKLM\\SOFTWARE\\GPL Ghostscript\\" + BatchXSLT.g_mainXSLTFrame.gs_version_num + "\" /v GS_DLL /t REG_SZ /d \"" + GS_DLL + "\" /f";
						retval = utils.callExternalApp(reg_exe_path, registryParams);
						registryParams = "add \"HKLM\\SOFTWARE\\GPL Ghostscript\\" + BatchXSLT.g_mainXSLTFrame.gs_version_num + "\" /v GS_LIB /t REG_SZ /d \"" + GS_LIB + "\" /f";
						retval = utils.callExternalApp(reg_exe_path, registryParams);
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
							System.out.println("REGISTRY ADD RETVAL : " + retval + " -------------------------------");
							stdinresp = utils.callExternalAppGetStdinResponse();
							System.out.println("REGISTRY STDIN: " + stdinresp);
							String errorresp = utils.callExternalAppGetErrorResponse();
							if (errorresp.equals(stdinresp) == false) System.out.println("REGISTRY ERROR: " + errorresp);
							System.out.println("REGISTRY ADD end -------------------------------");
						}
					}

				}
*/
			}
			if ( GS_envir.equals("") == false ) {
				envir = GS_envir.split("\\+\\+\\+");
			}
		}

		
		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
			System.out.println("************* GHOSTSCRIPT summary *************");
			System.out.println("  GS_pgm: '" + GS_pgm + "'");
			System.out.println("  GS_HOME: '" + GS_HOME + "'");
			System.out.println("  GS_DLL: '" + GS_DLL + "'");
			System.out.println("  GS_pgm_bin: '" + GS_pgm_bin + "'");
			System.out.println("  GS_PATH_envir: '" + GS_PATH_envir + "'");
			System.out.println("  GS_envir: '" + GS_envir + "'");
		}

		// ----------- try to call Ghostscript
		String[] cmd = {	// set up the command to send to the OS to call Ghostscript
			GS_pgm,
			"-version"
		};

		try {            
	    Runtime rt = Runtime.getRuntime();
	    Process proc;
			StreamGobbler outputGobbler = null;
			StreamGobbler errorGobbler = null;            

	        // execute the program
			try {
				if (envir == null) proc = rt.exec(cmd);
				else proc = rt.exec(cmd,envir);
				// wait for termination of the program
				exitVal = proc.waitFor();
			}
			catch (Exception e) {
				System.out.println("#### GHOSTSCRIPT EXCEPTION: " + e);
				return("");
			}
	        
			// get output and error streams
			outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT", true);
			errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", true);            

	        GS_ver = outputGobbler.getLine(0);	// get first line of GhostScript's returned version
			if (GS_ver == null) GS_ver = "";	// may have nothing to read
	                                
	        // any error???
	        exitVal = proc.waitFor();
	        if (exitVal != 0) {
	        	GS_ver = "";
				if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
					BatchXSLT.g_mainXSLTFrame.showMess("Ghostscript error: " + exitVal + " on call: '" + GS_pgm + "'\n" );
					String errline = "";
					int i = 0;
					errline = errorGobbler.getLine(i);	// get first line of Ghostscript returned version
					if (errline == null) errline = "";	// may have nothing to read
					while ( (errline != null) && (errline.equals("") == false) ) {
						BatchXSLT.g_mainXSLTFrame.showMess("   " + errline + "\n" );
						i++;
						errline = errorGobbler.getLine(i);
					}
				}
	        }
			else {
				// prepare the environment
				if (mainXSLTFrame.gs_pgm_path.equals("") == true) {
					mainXSLTFrame.gs_pgm_path = GS_pgm;	//  set path
				}
				if (mainXSLTFrame.gs_pgm_path_envir.equals("") == true) {
					mainXSLTFrame.gs_pgm_path_envir = GS_PATH_envir;	//  set path
				}
				if (setEnvirs == true) {
					if (mainXSLTFrame.gs_envir.equals("") == true) {
						mainXSLTFrame.gs_envir = GS_envir;
					}
					else {
						if (mainXSLTFrame.gs_envir.indexOf(GS_PATH_envir) < 0) mainXSLTFrame.gs_envir += "+++PATH=" + GS_PATH_envir;
					}
//					if ( osName.toLowerCase().indexOf( "windows" ) < 0 ) mainXSLTFrame.gs_envir += ":$PATH";
//					else mainXSLTFrame.gs_envir += ";%PATH%";
				}
			}

		}
		catch (Throwable t) {
			t.printStackTrace();
	        return("");
	    }
		
		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
			System.out.println("  GS_ver: " + GS_ver);
			System.out.println("***********************************************");
		}
		return(GS_ver);
	}



	// get Ghostscript version number
	public static String get_GS_version_num(String version_str) {
		if (version_str.equals("") == true) return("");
		String[] versarr = version_str.split(" ");
		if (versarr.length >= 3) return(versarr[2]);
		return("");
	}



	// check if pstotext is reachable
	public static String get_pstotext_version() {
	    int exitVal = 0;
	    String[] envir = null;
		String osName = mainXSLTFrame.systemOSname;	// System.getProperty("os.name" );
		String pstotext_ver = "";
		String pstotext_pgm = "pstotext";	// init call the OS X unix GhostScript program 'gs'
		if (mainXSLTFrame.pstotext_pgm_path.equals("") == true) return("");	//  nothing to find
		pstotext_pgm = mainXSLTFrame.pstotext_pgm_path;
/*
		if (mainXSLTFrame.pstotext_pgm_path.equals("") == false) {	// setup the indicated program
		}
		else {	// use default program
			if ( osName.startsWith( "Windows" ) ) {	// like Windows NT, 2000, XP....
				pstotext_pgm = "pstotxt3.exe";
			}
		}
*/
		// set the environement if available
		if ( BatchXSLT.g_mainXSLTFrame.pstotext_envir.equals("") == false ) {
			envir = BatchXSLT.g_mainXSLTFrame.pstotext_envir.split("\\+\\+\\+");
		}

		String[] cmd = {	// set up the command to send to the OS to call Ghostscript
			pstotext_pgm,
			"-version"
		};

		try {            
	        Runtime rt = Runtime.getRuntime();
	        Process proc;
			if (envir == null) proc = rt.exec(cmd);
			else proc = rt.exec(cmd,envir);
	        
	        // any output? on error stream ( -version is not a valid parameter )
	        // so the response comes on the error stream
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", true);            
	        pstotext_ver = errorGobbler.getLine(0);	// get first line of GhostScript's returned version

	                                
	        // we dont check for errors because calling the parm '-version' is erroneous
	        /*
	        exitVal = proc.waitFor();
	        if (exitVal != 0) {
	        	pstotext_ver = "";
	        }
			*/
		}
		catch (Throwable t) {	// we come here if the program could not be executed
	        t.printStackTrace();
	        return(pstotext_ver);
	    }
		return(pstotext_ver);
	}


	
	
	// Ghostscript caller
	// convert an EPS input file to a PDF
	public static int GS_eps2pdf(String eps_in, String pdf_out, int action) {
		// calling on Windows: gswin32.exe (graphics window) or the console version gswin32.exe
		// calling on Mac OSX Unix: gs  from /usr/local/bin
		//
		// the parameters are equal for all OS
		// -q -dSAFER -dNOPAUSE -dBATCH -sDEVICE=pdfwrite -dEPSCrop -sOutputFile="hav.pdf" -c .setpdfwrite -f "hav.eps"
		//
		// CALL should look like:
		// gswin32c.exe -q -dSAFER -dNOPAUSE -dBATCH -sDEVICE=pdfwrite -dEPSCrop -sOutputFile="hav.pdf" -c .setpdfwrite -f "hav.eps"
		//
		// action:
		// 		ad them to do multiple operations
		// 0 = do NO conversion
		// 1 = convert EPS file to PDF
		// 2 = extract TEXT from above PDF

		if ( (eps_in == null) || (eps_in.equals("") == true) ) return(-2);
		if ( action < 1 ) return(-3);

	    String[] envir = null;
		String osName = System.getProperty("os.name" );
	    int exitVal = 0;
		String GS_pgm = "gs";	// init call the OS X unix GhostScript program 'gs'
		if (mainXSLTFrame.gs_pgm_path.equals("") == false) {	// setup the indicated program
			GS_pgm = mainXSLTFrame.gs_pgm_path;
		}
		else {	// use default program
			if ( osName.startsWith( "Windows" ) ) {	// like Windows
				GS_pgm = "gswin64c.exe";
			}
		}
		// set the environement if available
		if ( BatchXSLT.g_mainXSLTFrame.gs_envir.equals("") == false ) {
			envir = BatchXSLT.g_mainXSLTFrame.gs_envir.split("\\+\\+\\+");
		}


		BatchXSLT.g_mainXSLTFrame.showMess( "    -- EPS to PDF: " + eps_in + "\n" );

		String[] cmd = {	// set up the command to send to the OS to call Ghostscript
			GS_pgm,
			"-q",
			"-dSAFER",
			"-dNOPAUSE",
			"-dBATCH",
			"-sDEVICE=pdfwrite",
			"-dEPSCrop",
			"-sOutputFile=" + pdf_out,
			"-c",
			".setpdfwrite",
			"-f",
			eps_in
		};

		try {            
	        Runtime rt = Runtime.getRuntime();
	        Process proc;
			if (envir == null) proc = rt.exec(cmd);
			else proc = rt.exec(cmd,envir);
	        
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", true);            
            // dump errors to console
            errorGobbler.start();
                                    
            // any error???
            exitVal = proc.waitFor();
		}
		catch (Throwable t) {
	        t.printStackTrace();
	        return(-1);
	    }
		return(exitVal);
	}


	
	
	// Ghostscript caller
	// extract the text from an EPS or PDF file
	public static int GS_extract_TXT(String in_file, String txt_out, int action) {
		// calling on Windows: pstotxt3.exe 
		// calling on Mac OSX Unix: pstotext  from /usr/local/bin
		//
		// the parameters are equal for all OS
		// -output "/users/ai/Test_FTP/00770157.tx1" "/Users/ai/Test_FTP/00770157.pdf"
		//
		// CALL should look like:
		// pstotext -output "/users/ai/Test_FTP/00770157.tx1" "/Users/ai/Test_FTP/00770157.pdf"
		//
		// action:
		// 		ad them to do multiple operations
		// 0 = do NO conversion
		// 1 = convert EPS file to PDF
		// 2 = extract TEXT from above PDF



		if ( (in_file == null) || (in_file.equals("") == true) ) return(-2);
		if ( action < 1 ) return(-3);

	    String[] envir = null;
		String osName = System.getProperty("os.name" );
	    int exitVal = 0;
		String pstotext_pgm = "pstotext";	// init call the OS X unix text extractor program 'pstotext'
		if (mainXSLTFrame.pstotext_pgm_path.equals("") == false) {	// call ,the indicated program
			pstotext_pgm = mainXSLTFrame.pstotext_pgm_path;
		}
		else {	// use default program
			if ( osName.startsWith( "Windows" ) ) {	// like Windows NT, 2000, XP....
				pstotext_pgm = "pstotxt3.exe";
			}
		}
		// set the environement if available
		if ( BatchXSLT.g_mainXSLTFrame.pstotext_envir.equals("") == false ) {
			if ( !BatchXSLTransform.isWindows() ) {	// ignore on windows!! or GS is not called any more!!!??? set PATH environement on Windows
				envir = BatchXSLT.g_mainXSLTFrame.pstotext_envir.split("\\+\\+\\+");
			}
		}
		else {	// we HAVE to point to the gs program path!!!
			if ( !BatchXSLTransform.isWindows() ) {
				if ( BatchXSLT.g_mainXSLTFrame.gs_envir.equals("") == false ) {
					envir = new String[1];
					envir[0] = "PATH=/usr/local/bin/";
				}
				else envir = BatchXSLT.g_mainXSLTFrame.gs_envir.split("\\+\\+\\+");
			}
		}

		BatchXSLT.g_mainXSLTFrame.showMess( "    -- PDF to TXT: " + in_file + "\n" );

/*		THIS DOES NOT WORK!!! GS is not called
		String[] cmd = null;
		if	( mainXSLTFrame.gs_pgm_path.equals("") == false ) {
			cmd = new String[7];
			cmd[0] = pstotext_pgm;
			cmd[1] = "-debug";
			cmd[2] = "-gs";
			//cmd[3] = mainXSLTFrame.gs_pgm_path;
			cmd[3] = "F:\\programme\\gs\\gs8.51\\bin\\gswin32c.exe";
			cmd[4] = "-output";
			cmd[5] = txt_out;
			cmd[6] = in_file;
		}
*/
		String[] cmd = {	// set up the command to send to the OS to call Ghostscript
			pstotext_pgm,
			"-output",
			txt_out,
			in_file
		};

		try {            
	        Runtime rt = Runtime.getRuntime();
	        Process proc;
			if (envir == null) proc = rt.exec(cmd);
			else proc = rt.exec(cmd,envir);
	        
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", true);            
            // dump errors to console
            errorGobbler.start();
                                    
            // any error???
            exitVal = proc.waitFor();
		}
		catch (Throwable t) {
	        t.printStackTrace();
	        return(-1);
	    }
		return(exitVal);
	}




}


