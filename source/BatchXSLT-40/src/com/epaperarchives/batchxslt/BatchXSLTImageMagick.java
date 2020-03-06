package com.epaperarchives.batchxslt;

import java.io.*;
import java.util.*;
import java.util.Iterator;


/* ===========================================================
 * interact with ImageMagick
 */
public class BatchXSLTImageMagick
{
    private static final String PACKAGE_NAME = "com.epaperarchives.batchxslt.BatchXSLTImageMagick";

	public BatchXSLTImageMagick() {
	}

	// check if ImageMagick is reachable
	public static String get_IMconvert_version() {
		return (get_IMconvert_version(false));
	}
	// check if ImageMagick is reachable
	public static String get_IMconvert_version(Boolean setEnvirs) {
	    int exitVal = 0;
	    String[] envir = null;
		String osName = mainXSLTFrame.systemOSname;	// System.getProperty("os.name" );
		String osVersion = mainXSLTFrame.systemOSversion;
		String osArch = mainXSLTFrame.systemOSarchitecture;
		boolean from_package = false;
		boolean exists = false;
		String IM_ver = "";
		String IM_pgm_PATH = "";
		String IM_pgm_CONVERT = "";
		String IM_pgm_IDENTIFY = "";
		String IM_envir = "";
		String IM_MAGICK_HOME = "";
		String IM_PATH = "";
		String GS_PATH = "";
		String im_version_num_for_windows = "";	// other version for windows - sh....

		im_version_num_for_windows = BatchXSLT.g_mainXSLTFrame.im_version_num_win;
		if (im_version_num_for_windows.equals("") == true) im_version_num_for_windows = BatchXSLT.g_mainXSLTFrame.im_version_num;

		String reg_exe_path = "";
		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("****** getting ImageMagick version ------------\n");

		// get the path to reg.exe to be able to query the registry
		if ( osName.toLowerCase().indexOf( "windows" ) >= 0 ) {
			reg_exe_path = utils.findFileInEnvirPath(mainXSLTFrame.envirPath, "reg.exe", "", "system");
			if (reg_exe_path.equals("") == false) reg_exe_path += "reg.exe";
			else {	// get reg.exe from package
				reg_exe_path = BatchXSLT.g_mainXSLTFrame.userDir + File.separator + "Utilities" + File.separator + "Helpers" + File.separator + "utils" + File.separator + "reg.exe";
				if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("REG.EXE PackagePATH: " + reg_exe_path + "\n");
				exists = utils.existsFile(reg_exe_path);	// test if program is contained in the package
				if (exists == false) reg_exe_path = "";		// not found

			}
			if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("REG.EXE PATH: " + reg_exe_path + "\n");
		}

		if (mainXSLTFrame.gs_pgm_path.equals("") == false) {	// get path to ghostscript
			File f = new File(mainXSLTFrame.gs_pgm_path);
			GS_PATH = f.getParent();
		}

		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("******* searching for ImageMagick\n");
		if (mainXSLTFrame.im_pgm_path.equals("") == false) {	//  setup the indicated program
			IM_pgm_CONVERT = mainXSLTFrame.im_pgm_CONVERT;
			IM_pgm_IDENTIFY = mainXSLTFrame.im_pgm_IDENTIFY;
			if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("   !!!! SET IN JobTicket\n");
		}
		else {	// try to find ImageMagick program
			do {
				if (BatchXSLT.g_mainXSLTFrame.im_pgm_CONVERT.equals("") == false) {
					IM_pgm_CONVERT = BatchXSLT.g_mainXSLTFrame.im_pgm_CONVERT;
					IM_pgm_IDENTIFY = BatchXSLT.g_mainXSLTFrame.im_pgm_IDENTIFY;
					break;
				}
				else {
					if ( osName.toLowerCase().startsWith( "windows" ) == true ) {	// like Windows NT, 2000, XP....
						IM_pgm_CONVERT = "convert.exe";
						IM_pgm_IDENTIFY = "identify.exe";
					}
					else {	// for unix like systems
						IM_pgm_CONVERT = "convert";
						IM_pgm_IDENTIFY = "identify";
					}
				}

				// ------------------------------------------
				// get path to IM
				String pgm_path = "";
				// check if we want run the installed version
				if (BatchXSLT.g_mainXSLTFrame.im_pgm_installedpath.equals("") == false) {
					IM_MAGICK_HOME = BatchXSLT.g_mainXSLTFrame.im_pgm_installedhome;
					pgm_path = BatchXSLT.g_mainXSLTFrame.im_pgm_installedpath;
					if (pgm_path.endsWith(File.separator) == false) pgm_path += File.separator;
					IM_pgm_PATH = pgm_path;
					pgm_path += IM_pgm_CONVERT;
				}
				else {
					// try to find ImageMagick in the package installed in appDir
					pgm_path += mainXSLTFrame.appDir;
					if (pgm_path.endsWith(File.separator) == false) pgm_path += File.separator;
					pgm_path += "Utilities" + File.separator + "Imaging";	// like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging

					// ad pgm folder like 'ImageMagick'
					pgm_path += File.separator + "ImageMagick";  // pgm_path is now like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging/ImageMagick

					// set app and Environment
					if ( osName.toLowerCase().indexOf( "windows" ) < 0 ) {
					  // OSX ImageMagick app
					  // Utilities/Imaging/ImageMagick/
					  //                              |_ im.app/
					  //                                      |_ Contents/
					  //                                                 |_ Home/
					  //                                                        |_ bin/
					  //                                                              |_ convert
						pgm_path += File.separator + "im.app" + File.separator + "Contents" + File.separator + "Home";  // pgm_path is now like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging/ImageMagick/im.app/Contents/Home
						IM_MAGICK_HOME = pgm_path;
						// ad executable path/name
						pgm_path +=  File.separator + "bin" +  File.separator + IM_pgm_CONVERT;  // pgm_path is now like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging/im/bin/convert
					}
					else {
						IM_MAGICK_HOME = pgm_path;
						// ad executable name
						pgm_path += File.separator + IM_pgm_CONVERT;  // pgm_path is now like: /Applications/BatchXSLT4xxx/BatchXSLT4/Utilities/Imaging/ImageMagick/convert.exe
					}

					// test if IM is contained in the package
					if (pgm_path.equals("") == false) {
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("   ---- IM test executable exists: " + pgm_path + "\n");
						exists = utils.existsFile(pgm_path);	// test if program is contained in the package
						if (exists == true) {	// we got it in the package!
							from_package = true;
							IM_pgm_CONVERT = pgm_path;
							IM_pgm_IDENTIFY = IM_pgm_PATH + IM_pgm_IDENTIFY;
							if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("   !!!! FOUND\n");
							break;
						}
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("   !!!! NOT FOUND\n");
					}

					// ****************************************
					// ***** WE HAVE NOT FOUND ImageMagick!!!!
					IM_MAGICK_HOME = IM_pgm_PATH = IM_pgm_CONVERT = IM_pgm_IDENTIFY = "";
					// ------------------------------------------
					if ( osName.toLowerCase().indexOf( "windows" ) >= 0 ) {	// like Windows NT, 2000, XP....
						// get from WINDOWS Registry

						String registryParams = "query \"HKLM\\SOFTWARE\\ImageMagick\\" + im_version_num_for_windows + "\\Q:" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth + "\" /v BinPath";
						int retval = utils.callExternalApp(reg_exe_path, registryParams);
						String stdinresp = "";
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
							BatchXSLT.g_mainXSLTFrame.showMess("REGISTRY QUERY to find ImageMagick program -------------------------------\n");
							BatchXSLT.g_mainXSLTFrame.showMess("QUERY KEYS: " + registryParams + "\n");
							BatchXSLT.g_mainXSLTFrame.showMess("REGISTRY RETVAL : " + retval + "\n");
						}
						stdinresp = utils.callExternalAppGetStdinResponse();
						if (retval == 0) {	// on no error, the registry is set - let's check for correct value
							if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
								BatchXSLT.g_mainXSLTFrame.showMess("REGISTRY QUERY ImageMagick BinPath: " + stdinresp + "\n");
							}
							if (stdinresp.equals("") == false) {
								String[] reg_arr = stdinresp.split("\t");
								pgm_path = reg_arr[reg_arr.length - 1];
								pgm_path = pgm_path.replaceAll("\r?\n", "");	// trim line endings
								IM_MAGICK_HOME = pgm_path;
								IM_pgm_CONVERT = pgm_path + File.separator + IM_pgm_CONVERT;
								IM_pgm_IDENTIFY = pgm_path + File.separator + IM_pgm_IDENTIFY;

								if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
									BatchXSLT.g_mainXSLTFrame.showMess("REGISTRY ImageMagick BinPath : " + pgm_path + "\n");
								}
								exists = utils.existsFile(IM_pgm_CONVERT);	// test if program can be found from Registy BinPath
								if (exists == true) {	// we got it in the package!
									from_package = false;
									if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
										BatchXSLT.g_mainXSLTFrame.showMess("IMAGEMAGICK found in Registry: '" + IM_pgm_CONVERT + "'\n");
									}
									break;
								}
							}
						}
					}
				
					// still no ImageMagick found
					IM_MAGICK_HOME = IM_pgm_PATH = IM_pgm_CONVERT = IM_pgm_IDENTIFY = "";
					// ------------------------------------------
					// the Utilities path from package does not contain ImageMagick
					// Not found until yet: get from envir PATH or default system path
					if ( osName.toLowerCase().indexOf( "windows" ) >= 0 ) {	// like Windows NT, 2000, XP....
						pgm_path = utils.findFileInEnvirPath(mainXSLTFrame.envirPath, IM_pgm_CONVERT, "system");
						if (pgm_path.equals("") == false) {
							IM_MAGICK_HOME = pgm_path;
							IM_pgm_CONVERT = pgm_path + IM_pgm_CONVERT;
							IM_pgm_IDENTIFY = pgm_path + IM_pgm_IDENTIFY;
						}
					}
					else {	// for unix like systems
						pgm_path = File.separator + "usr" + File.separator + "local";
						IM_MAGICK_HOME = pgm_path;
						pgm_path +=  File.separator + "bin" + File.separator + IM_pgm_CONVERT;
						// test if exists
						exists = utils.existsFile(pgm_path);	// test if program is mentioned somewhere in PATH
						if (exists == false) {	// not found
							pgm_path = File.separator + "usr";
							IM_MAGICK_HOME = pgm_path;
							pgm_path +=  File.separator + "bin" + File.separator;
							IM_pgm_CONVERT = pgm_path + IM_pgm_CONVERT;
							IM_pgm_IDENTIFY = pgm_path + IM_pgm_IDENTIFY;
							pgm_path += IM_pgm_CONVERT;
						}
					}
				}
				
				// Test IM exists at pgm_path
				if (pgm_path.equals("") == false) {
					if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("   ---- Test IM exists at pgm_path: '" + pgm_path +"'\n");
					exists = utils.existsFile(pgm_path);	// test if program is mentioned somewhere in PATH
					if (exists == true) {	// we got it in the package!
						from_package = false;
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("   !!!! FOUND\n");
						break;
					}
					if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("   !!!! NOT FOUND\n");
				}
				
				
				/// not found at all				
				IM_MAGICK_HOME = IM_pgm_PATH = IM_pgm_CONVERT = IM_pgm_IDENTIFY = "";
				break;
			} while(false);
		}



		if (IM_pgm_CONVERT.equals("") == true) return("");
		
		// set the environement if available
		if ( BatchXSLT.g_mainXSLTFrame.im_envir.equals("") == true ) {
			/* prepare the environment
				Syntax like this:
				ENVIR_VAR_NAME = /basePathToImagingFolder/OSidentifier/Arch/PGM/Version/anySubfolder/
							### 'basePathToImagingFolder' is the 'Utilities/Imaging/' folder in the package
							### OSidentifier is 'Win' or 'Linux' or 'Mac'
							### Arch is Architecture 'i386' or 'i64' or whatever will come
							### PGM is 'im' or gs' for ImageMagick or Ghostscript
							### Version is pgm version like 6.8.8
							### anySubfolder is the subpath into the converter package like: lib/ImageMagick-6.8.8/config-Q16
				// some are not used any longer --> !!
				MAGICK_HOME=/basePathToImagingFolder/Mac/i386/im/6.8.8
			--> 	+++MAGICK_CONFIGURE_PATH=/basePathToImagingFolder/Mac/i386/im/6.8.8/lib/ImageMagick-6.8.8/config-Q16:/basePathToImagingFolder/Mac/i386/im/6.8.8/share/ImageMagick-6:/basePathToImagingFolder/Mac/i386/im/6.8.8/etc/ImageMagick-6
			--> 	+++MAGICK_CODER_MODULE_PATH=/basePathToImagingFolder/Mac/i386/im/6.8.8/etc/ImageMagick-6
				+++PATH=/basePathToImagingFolder/Mac/i386/im/6.8.8/bin:/home/parallels/Desktop/BatchXSLT4AlfaV1_Linux/BatchXSLT/Utilities/Imaging/Mac/i386/gs/9.10/bin:$PATH
				+++DYLD_LIBRARY_PATH=/home/parallels/Desktop/BatchXSLT4AlfaV1_Linux/BatchXSLT/Utilities/Imaging/Mac/i386/im/6.8.8/lib
				+++LD_LIBRARY_PATH=/home/parallels/Desktop/BatchXSLT4AlfaV1_Linux/BatchXSLT/Utilities/Imaging/Mac/i386/im/6.8.8/lib
			*/
			if ( osName.toLowerCase().indexOf( "windows" ) < 0 ) {	// like unix
				if ( IM_MAGICK_HOME.equals("") == false ) {
					//String lib_config_fldr = "config";	// the config folder within the 'lib' folder. 'config' for versions prior 6.8, 'config-Q16' for newer versions
					//String lib_share_fldr = "ImageMagick-6";	// the config folder within the 'share' folder

					IM_envir = "MAGICK_HOME=" + IM_MAGICK_HOME;
					IM_envir += "+++LD_LIBRARY_PATH=" + IM_MAGICK_HOME + File.separator + "lib";	// need on Linux and Solaris machines
					IM_envir += "+++DYLD_LIBRARY_PATH=" + IM_MAGICK_HOME + File.separator + "lib";	// think, this is nneded for older IM versions on OSX
				//	IM_envir += "+++MAGICK_CONFIGURE_PATH=" + IM_MAGICK_HOME + File.separator + "lib" + File.separator + "ImageMagick-" + BatchXSLT.g_mainXSLTFrame.im_version_num + File.separator + "config-Q" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth 
				//										+ ":" + IM_MAGICK_HOME + File.separator + "share" + File.separator + "ImageMagick-" + BatchXSLT.g_mainXSLTFrame.im_version_num + File.separator + "config";
				//	IM_envir += "+++MAGICK_CODER_MODULE_PATH=" + IM_MAGICK_HOME + File.separator + "lib" + File.separator + "ImageMagick-" + BatchXSLT.g_mainXSLTFrame.im_version_num + File.separator + "modules-Q" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth + File.separator + "coders";
				//	IM_envir += "+++MAGICK_CODER_FILTER_PATH=" + IM_MAGICK_HOME + File.separator + "lib" + File.separator + "ImageMagick-" + BatchXSLT.g_mainXSLTFrame.im_version_num + File.separator + "modules-Q" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth + File.separator + "filters";
					IM_PATH = IM_MAGICK_HOME + File.separator + "bin";
				//	IM_DYLD_LIBRARY_PATH = IM_MAGICK_HOME + File.separator + "lib";
				}

				if ( GS_PATH.equals("") == false ) {	// add path to ghostscript
					if (IM_PATH.equals("") == false) {
						if (IM_PATH.toLowerCase().indexOf(GS_PATH.toLowerCase()) < 0) {
							IM_PATH += ":" + GS_PATH;
						}
					}
					else IM_PATH += GS_PATH;
				}
				if ( IM_PATH.equals("") == false ) {
					IM_envir += "+++PATH=" + IM_PATH + ":$PATH";
				}

			}
			else {	// like Windows 
				if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
					System.out.println("REGISTERING IMAGEMAGICK version: " + im_version_num_for_windows + ", quantum depth:" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth);
				}
				IM_envir = "";
				// just manual debug:    IM_envir += "MAGICK_DEBUG=All+++";
				if ( IM_MAGICK_HOME.equals("") == false ) {
					IM_envir += "MAGICK_HOME=" + IM_MAGICK_HOME;
					// check for modules folder
					String apath = IM_MAGICK_HOME + File.separator + "modules";
					exists = utils.existsFile(apath);
					if (exists) {	// if it exists, then it is the dll version. otherwise it is the static version
						IM_envir += "+++MAGICK_CODER_MODULE_PATH=" + IM_MAGICK_HOME + File.separator + "modules" + File.separator + "coders";
						IM_envir += "+++MAGICK_CODER_FILTER_PATH=" + IM_MAGICK_HOME + File.separator + "modules" + File.separator + "filters";
						//IM_envir += "+++MAGICK_CONFIGURE_PATH=" + IM_MAGICK_HOME + File.separator + "config";
					}
					IM_PATH = IM_MAGICK_HOME;
				}

				if ( GS_PATH.equals("") == false ) {	// add path to ghostscript
					if (IM_PATH.equals("") == false) IM_PATH += ";";
					IM_PATH += GS_PATH;
				}
				if ( IM_PATH.equals("") == false ) {
					if (IM_envir.equals("") == false) IM_envir += "+++";
					IM_envir += "Path=" + IM_PATH + ";%Path%";
				}
				if ( BatchXSLT.g_mainXSLTFrame.gs_envir.equals("") == false ) {
					if (IM_envir.equals("") == false) IM_envir += "+++";
					IM_envir += BatchXSLT.g_mainXSLTFrame.gs_envir;
				}
/*
				// set registry keys if reg.exe is reachable and ImageMagick is from package
				if ( (reg_exe_path.equals("") == false) 
						&& (im_version_num_for_windows.equals("") == false) 
						&& (BatchXSLT.g_mainXSLTFrame.im_quantumdepth.equals("") == false) ) {
					String registryParams = "query \"HKLM\\SOFTWARE\\ImageMagick\\" + im_version_num_for_windows + "\\Q:" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth;
					int retval = utils.callExternalApp(reg_exe_path, registryParams);
					String stdinresp = "";
					if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
						System.out.println("REGISTRY QUERY ImageMagick -------------------------------");
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
						if (stdinresp.toLowerCase().indexOf(IM_MAGICK_HOME.toLowerCase()) < 0) retval = 1;	// set to error for we can set to correct value below
					}
					// set registry entry - or our ImageMagick in the package crashes
					if ( (from_package == true) && (retval != 0) ) {
						registryParams = "add \"HKLM\\SOFTWARE\\ImageMagick\\" +im_version_num_for_windows + "\\Q:" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth + "\" /v BinPath /t REG_SZ /d \"" + IM_MAGICK_HOME + "\" /f";
						retval = utils.callExternalApp(reg_exe_path, registryParams);
						registryParams = "add \"HKLM\\SOFTWARE\\ImageMagick\\" + im_version_num_for_windows + "\\Q:" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth + "\" /v LibPath /t REG_SZ /d \"" + IM_MAGICK_HOME + "\" /f";
						retval = utils.callExternalApp(reg_exe_path, registryParams);
						registryParams = "add \"HKLM\\SOFTWARE\\ImageMagick\\" + im_version_num_for_windows + "\\Q:" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth + "\" /v ConfigurePath /t REG_SZ /d \"" + IM_MAGICK_HOME + "\\config\" /f";
						retval = utils.callExternalApp(reg_exe_path, registryParams);
						registryParams = "add \"HKLM\\SOFTWARE\\ImageMagick\\" + im_version_num_for_windows + "\\Q:" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth + "\" /v CoderModulesPath /t REG_SZ /d \"" + IM_MAGICK_HOME + "\\modules\\coders\" /f";
						retval = utils.callExternalApp(reg_exe_path, registryParams);
						registryParams = "add \"HKLM\\SOFTWARE\\ImageMagick\\" + im_version_num_for_windows + "\\Q:" + BatchXSLT.g_mainXSLTFrame.im_quantumdepth + "\" /v FilterModulesPath /t REG_SZ /d \"" + IM_MAGICK_HOME + "\\modules\\filters\" /f";
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

			
			
			if ( IM_envir.equals("") == false ) {
				mainXSLTFrame.im_envir = IM_envir;
			}
		}

		envir = mainXSLTFrame.im_envir.split("\\+\\+\\+");

		
		if (mainXSLTFrame.im_debug > 0) {
			BatchXSLT.g_mainXSLTFrame.showMess("************* ImageMagick summary *************\n");
			BatchXSLT.g_mainXSLTFrame.showMess("  IM_pgm_CONVERT: '" + IM_pgm_CONVERT + "'\n");
			BatchXSLT.g_mainXSLTFrame.showMess("  IM_pgm_IDENTIFY: '" + IM_pgm_IDENTIFY + "'\n");
			BatchXSLT.g_mainXSLTFrame.showMess("  IM_MAGICK_HOME: '" + IM_MAGICK_HOME + "'\n");
			BatchXSLT.g_mainXSLTFrame.showMess("  IM_PATH: '" + IM_PATH + "'\n");
			BatchXSLT.g_mainXSLTFrame.showMess("  GS_PATH: '" + GS_PATH + "'\n");
			BatchXSLT.g_mainXSLTFrame.showMess("  IM_envir: '" + IM_envir + "'\n");
			BatchXSLT.g_mainXSLTFrame.showMess("  mainXSLTFrame.im_envir: '" + mainXSLTFrame.im_envir + "'\n");
		}
		
		// ----------- try to call ImageMagick
		
		String[] cmd = {	// set up the command to send to the OS to call ImageMagick
			IM_pgm_CONVERT,
			"-version"
			};

		// special debug only
/*
		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
			BatchXSLT.g_mainXSLTFrame.showMess("getting IMAGEMAGICK version with cmd: '" + Arrays.toString(cmd) + "'\n");
			if (envir != null) BatchXSLT.g_mainXSLTFrame.showMess("                               envir: '" + Arrays.toString(envir) + "'\n");
		}
*/		
		try {            
	    Runtime rt = Runtime.getRuntime();
	    Process proc;
			StreamGobbler outputGobbler = null;
			StreamGobbler errorGobbler = null;            
			
	        // execute the program
			try {

				if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
					BatchXSLT.g_mainXSLTFrame.showMess("---------------------------------------------\n");
					BatchXSLT.g_mainXSLTFrame.showMess("IMAGE MAGICK call: '" + IM_pgm_CONVERT + " -version'\n");
				}
				ProcessBuilder pb = new ProcessBuilder(cmd);		// this must be an array of strings! a single string "command -params" does not work!
				if (envir != null) {
					if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
						BatchXSLT.g_mainXSLTFrame.showMess("IMAGE MAGICK environment variables:\n");
					}
					Map<String, String> environment = pb.environment();
					for (int i = 0; i < envir.length; i++) {
						String[] key_val = envir[i].split("=");
						environment.put(key_val[0], key_val[1]);
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
							BatchXSLT.g_mainXSLTFrame.showMess(key_val[0] + "='" + key_val[1] + "'\n");
						}
					}
				}
		//		pb.directory(workdir);
				pb.redirectErrorStream(true);
				proc = pb.start();


				// get output and error streams
				outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT", true);
				errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", true);            
	        
				// wait for termination of the program
				exitVal = proc.waitFor();
			}
			catch (Exception e) {
				BatchXSLT.g_mainXSLTFrame.showMess("IMAGEMAGICK EXCEPTION: " + e + "\n");
				return("");
			}

	    IM_ver = outputGobbler.getLine(0);	// get first line of Imagemagick returned version
			if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
				BatchXSLT.g_mainXSLTFrame.showMess("returned IMAGEMAGICK version string: '" + IM_ver + "'\n");
				if (IM_ver != null) BatchXSLT.g_mainXSLTFrame.showMess("         IMAGEMAGICK version number: '" + get_IMconvert_version_num(IM_ver) + "'\n");
			}
			if (IM_ver == null) IM_ver = "";	// may have nothing to read
			if (IM_ver.indexOf(" file:") > 0) {
				String[] ver = IM_ver.split(" file:");
				IM_ver = ver[0];
			}
			
			// ImageMagick 6.4.0 with sub-version > 9 returns an exitVal of 1 (which is new and wrong: should return 0)
			// but returns the correct version string in the output stream
			// WE HAVE TO CATCH THIS
	    if ( (exitVal != 0) && (IM_ver.equals("") == false) ) {
				if ( (IM_ver.toLowerCase().indexOf("version:") >= 0) && (IM_ver.toLowerCase().indexOf("imagemagick") >= 0) ) {
					exitVal = 0;
				}
			}
				
	    if (exitVal != 0) {
				if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
					BatchXSLT.g_mainXSLTFrame.showMess("IMAGEMAGICK error: " + exitVal + " on call: '" + IM_pgm_CONVERT + "'\n" );
					if (IM_ver.equals("") == false) BatchXSLT.g_mainXSLTFrame.showMess("   " + IM_ver + "\n" );
					String errline = "";
					int i = 0;
					errline = errorGobbler.getLine(i);	// get error lines
					while ( errline != null ) {
						i++;
						if (errline.equals("") == false) BatchXSLT.g_mainXSLTFrame.showMess(i + ": " + errline + "\n" );
						errline = errorGobbler.getLine(i);	// get more lines
					}
				}
	      IM_ver = "";
	    }
			else {
				if (mainXSLTFrame.im_pgm_path.equals("") == true) {
					mainXSLTFrame.im_pgm_path = IM_pgm_CONVERT;	//  set path (compatibility)
					mainXSLTFrame.im_pgm_CONVERT = IM_pgm_CONVERT;	//  set path
					mainXSLTFrame.im_pgm_IDENTIFY = IM_pgm_IDENTIFY;	//  set path
				}
				if (setEnvirs == true) {	// set the environement
					if (mainXSLTFrame.im_envir.equals("") == true) {
						if (IM_envir.equals("") == false) mainXSLTFrame.im_envir = IM_envir;
						else {
							if ( (GS_PATH.equals("") == false) && (mainXSLTFrame.im_envir.indexOf(GS_PATH) < 0) ) {
								if ( osName.toLowerCase().indexOf( "windows" ) < 0 ) mainXSLTFrame.im_envir += "PATH=" + GS_PATH + ":$PATH";	// like unix
								else mainXSLTFrame.im_envir += "PATH=" + GS_PATH + ";%PATH%";
							}
						}
					}
					else {	// at least set path to ghostscript
						if ( (GS_PATH.equals("") == false) && (mainXSLTFrame.im_envir.indexOf(GS_PATH) < 0) ) {
							if ( osName.toLowerCase().indexOf( "windows" ) < 0 ) mainXSLTFrame.im_envir += "PATH=" + GS_PATH + ":$PATH";	// like unix
							else mainXSLTFrame.im_envir += "PATH=" + GS_PATH + ";%PATH%";
						}
					}
				}
			}

		}
		catch (Throwable t) {
			t.printStackTrace();
	        return("");
	    }

		if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
			BatchXSLT.g_mainXSLTFrame.showMess("***********************************************\n");
		}
		return(IM_ver);
	}




	// get ImageMagick version number
	public static String get_IMconvert_version_num(String version_str) {
		if (version_str.equals("") == true) return("");
		String[] versarr = version_str.split(" ");
		if (versarr.length >= 3) return(versarr[2]);
		return("");
	}


}


