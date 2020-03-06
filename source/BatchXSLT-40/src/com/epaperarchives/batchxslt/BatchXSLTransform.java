/** 
 * BatchXSLTransform.java
 *
 * Title:			BatchXSLT main Transformer Engine
 * Description:	
 * @author			Andreas Imhof
 * @version			40.10
 * @versionDate		20200221
 */

package com.epaperarchives.batchxslt;


// Imported java classes
import java.io.*;
import java.util.*;
import java.util.MissingResourceException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import javax.crypto.*;
import javax.crypto.spec.*;

// Imported TraX classes
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import javax.xml.parsers.*;


import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.w3c.dom.*;






public class BatchXSLTransform
{
    private static final String PACKAGE_NAME = "com.epaperarchives.batchxslt.BatchXSLTransform";
    private static boolean externalConvertersChecked = false;

	public BatchXSLTransform() {
	}

	public static	String	lastFTPControlEncoding = "";
	public static	String	lastFTPSystem = "";

	public static	int	scan_dot_cnt = 1;
	public static	ai.FilePack.MyFileStructure mf_tree = null;

	public static boolean isThreadViewer_started = false;

	static String scan_dots(int dots) {
		String sd = "";
		if (dots > 0) scan_dot_cnt = dots;
		for (int i = 1; i <= scan_dot_cnt; i++) sd += '.';
		scan_dot_cnt++;
		if (scan_dot_cnt > 20) scan_dot_cnt = 1;
		return(sd);
	}


	static int		g_numfiles_avail = 0;			// number of files available in a file list
	static int		the_found_file_type = 0;
	static String	xmlsrc_version = "";
	static String	xmlsrc_encoding = "";
	static String	xmlsrc_doctype_declaration = "";
	static boolean	inputpath_is_Dir = false;
	static String	my_InputPath = "";
	static String	my_InputName = "";
	static String	my_InputSubPath = "";
	static int		searchFileIdx = 0;
	static int		processing_runtype = 0;	// make two runs on the file list: first process XML and then clean
										// 0 = process XML files only
										// 1 = do cleanup
			// Use the static TransformerFactory.newInstance() method to instantiate 
			// a TransformerFactory. The javax.xml.transform.TransformerFactory 
			// system property setting determines the actual class to instantiate --
			// org.apache.xalan.transformer.TransformerImpl.

	static int		numberof_BatchXSLTrans_calls = 0;	// a counter on how many times 'BatchXSLTrans' was called
													// important to know if things must be done only at the first call

	public static int		numberof_readJobTicketFile_calls = 0;	// a counter on how many times 'BatchXSLTrans' was called
													// important to know if things must be done only at the first call
													// like showing some initial messages like Ghostscript version and ImageMagick
	public static String BatchXSLTrans_lastMessage = "";	// last message given by BatchXSLTrans
	
	public static int BatchXSLTrans(int func, boolean showmess)
	{
		BatchXSLTrans_lastMessage = "";
		numberof_BatchXSLTrans_calls++;
		processing_runtype = 0;	// make two runs on the file list: first process XML and then clean

		int		myErr = 0;
		String	answer = "";				// for yes/no input at end of loop

		String	the_xmlFileName = "";
		String	the_xslFileName = "";
		String	the_outputFileName = "";
		boolean	outputpath_is_Dir = false;
		boolean	process_single_file = true;	// process a single given file or if the given name is dir, process_single_file will be set to false

		boolean	f_exists = false;
		my_InputPath = "";
		my_InputName = "";
		xmlsrc_version =  xmlsrc_encoding = xmlsrc_doctype_declaration = "";
		searchFileIdx = 0;
		int		file_processing = 0;	// the current file counter
		String	my_InputPathName = "";
		my_InputSubPath = "";
		String	my_OutputPath = "";
		String	actual_my_OutputPath = "";
		String	actual_my_OutputName = "";
		String	my_OutputName = "";
		boolean	change_output_nameext = false;
		boolean	retflag = false;
		int		result_type = 1;	// set to: 'is local file'
		boolean	is_working_output = false;
		String	work_subdir = mainXSLTFrame.userHome + File.separator + "__BatchXSLT_transwork" + File.separator;
		boolean tree_ok = false;
		boolean isIDMLpackage = false;
		
		// check if certain path contain vaild path names and not just info strings
		if ((mainXSLTFrame.sourcePathName.equals("")) 
				|| ((mainXSLTFrame.sourcePathName.startsWith("**") == true))	// just an info message
				|| ((mainXSLTFrame.sourcePathName.startsWith("->") == true))	// "
				|| ((mainXSLTFrame.sourcePathName.startsWith("-->") == true))	// "
				) return(0);

		// state transformer as 'busy'
		mainXSLTFrame.transformStausMessage = "busy";

		// let's build a tree of all files and directories found in the given path
		if (mainXSLTFrame.sourcePathName.equals("")) {
			BatchXSLT.g_mainXSLTFrame.showRunMess( "" );
			BatchXSLTrans_lastMessage = "Empty source path";
			mainXSLTFrame.transformStausMessage = "idle";
			return(-1);
		}	// empty input - error!
		
		// expand the source and output paths with a root patt
		if (mainXSLTFrame.sourcePathRoot.equals("") == false) mainXSLTFrame.sourcePathName = mainXSLTFrame.sourcePathRoot + mainXSLTFrame.sourcePathName;
		
		the_xmlFileName = mainXSLTFrame.sourcePathName;		// get the preset hotfolder path or file
		if (the_xmlFileName.startsWith("." + File.separator) == true) {		// the path may start with './' - then we add the current path
			File curdir = new File (".");
			try { the_xmlFileName = curdir.getCanonicalPath() + File.separator + the_xmlFileName; }
			catch ( java.io.IOException e ) {}
		}
		the_xslFileName = mainXSLTFrame.xslPathName;
		the_outputFileName = mainXSLTFrame.outputPathName;
		inputpath_is_Dir = is_dir( the_xmlFileName );
		f_exists = file_exists( the_xmlFileName );						// check if path or file exists
		if (!f_exists) {
			if (inputpath_is_Dir) BatchXSLT.g_mainXSLTFrame.showRunMess( "Path not found." );
			else BatchXSLT.g_mainXSLTFrame.showRunMess( "Path/File not found." );
			try { Thread.sleep(500); }
			catch (InterruptedException e) {}
			BatchXSLTrans_lastMessage = "Source path or file not found: '" + the_xmlFileName + "'";
			mainXSLTFrame.transformStausMessage = "idle";
			return(-1);
		}
		if (inputpath_is_Dir) {	// build path tree if a path is given
			BatchXSLT.g_mainXSLTFrame.showRunMess( "scanning path" + scan_dots(3) );
			tree_ok = build_path_tree(mainXSLTFrame.sourcePathName);
			if (!tree_ok) {
				mainXSLTFrame.transformStausMessage = "idle";
				return(0);
			}
			if (file_tree_size() <= 0) {
				try { Thread.sleep(300); }
				catch (InterruptedException e) {}
				BatchXSLTrans_lastMessage = "no file(s).";
				//BatchXSLT.g_mainXSLTFrame.showRunMess( "no file(s)." );		// no more files
				mainXSLTFrame.transformStausMessage = "idle";
				return(1);	// check if we have files in the given source hotfolder
			}

			// evtl. convert EPS file to PDF and optionally to TEXT
			if ( mainXSLTFrame.gs_convert > 0) {
				int eps_converted;
				int old_filetree_size = file_tree_size();
				int new_filetree_size = -1;
				eps_converted = convert_EPS(mainXSLTFrame.gs_convert);
				if (eps_converted > 0) {	// we have added PDF/Text files: our files tree has grown
					new_filetree_size = file_tree_size();
				}
			}

			// evtl. create a folder XML file
			if ( mainXSLTFrame.folderIndexFileName.equals("") == false) {
				int epaper_idx_built;
				epaper_idx_built = build_epaper_indexes(-1);	// -1 == initial call
				if (epaper_idx_built >= 0) {	// we have added index files: rebuild tree for the index files are transformed too
					BatchXSLT.g_mainXSLTFrame.showMess( "---- rescaning main path..." );
					tree_ok = build_path_tree(mainXSLTFrame.sourcePathName);
					if (!tree_ok || (file_tree_size() <= 0)) {
						BatchXSLT.g_mainXSLTFrame.showMess( "ERROR.\n" );
						BatchXSLTrans_lastMessage = "Rescanning source path failed: '" + mainXSLTFrame.sourcePathName + "'";
						mainXSLTFrame.transformStausMessage = "idle";
						return(-2);
					}
					if (file_tree_size() <= 0) {
						BatchXSLTrans_lastMessage = "";
						mainXSLTFrame.transformStausMessage = "idle";
						return(0);	// check if we have files in the given source hotfolder
					}
					BatchXSLT.g_mainXSLTFrame.showMess( "done.\n" );
				}
			}



			//searchFileIdx = file_tree_size() - 1;	// point to last file/dir in tree
			searchFileIdx = 0;	// point to first file/dir in tree
			process_single_file = false;	// we process multiple files from a folder tree
			
			/*// =========just test output for the created tree
			int my_idx = 0; String my_path = mf_tree.get_file_path(my_idx); String my_name = mf_tree.get_file_name(my_idx);
			if (my_name.equals("")) { 
				System.out.println("   ---    nothing found");
				mainXSLTFrame.transformStausMessage = "idle";
				return(0);
			}
			while (my_name.equals("") == false) {
				System.out.println(my_path + "   ---" + (mf_tree.get_file_type(my_idx) == 1 ? "File: " : "Dir: ") + my_name);
				my_idx++; my_path = mf_tree.get_file_path(my_idx); my_name = mf_tree.get_file_name(my_idx);
			}
			if (my_name.equals("")) {
				System.out.println("   ---    no more files");
				mainXSLTFrame.transformStausMessage = "idle";
				return(0);
			}
			if (true) {
				mainXSLTFrame.transformStausMessage = "idle";
				return(0);
			}
			=========*/
		}

		do {	// as long as we find an InputFileName or already have an xmlFileName
 			if (mainXSLTFrame.general_abort == true) {
				BatchXSLTrans_lastMessage = "Transform aborted";
				mainXSLTFrame.transformStausMessage = "idle";
				return(2);
			}
 			myErr = 0;
			outputpath_is_Dir = false;					// setup the work path/names
			my_InputPath = my_InputName = my_InputPathName = my_OutputPath = my_OutputName = "";
			xmlsrc_version =  xmlsrc_encoding = xmlsrc_doctype_declaration = "";
			actual_my_OutputPath = actual_my_OutputName = "";
			change_output_nameext = false;
			is_working_output = false;
			isIDMLpackage = false;

			//======================================================================
			// ---- input file stuff
			//BatchXSLT.g_mainXSLTFrame.showRunMess( "scanning" + scan_dots(0) );
			// get a file name from the list if a path tree was created
			// otherwise we have to process 'the_xmlFileName' which contains a path and file name
			if (tree_ok) {	// get from tree
				the_found_file_type = get_file_from_tree(searchFileIdx);
				//BatchXSLT.g_mainXSLTFrame.showMess( "+++ Processing Tree file " + searchFileIdx + " my_InputName: '" + my_InputName + "'\n" );
				// after this the following vars are set up:
				// the_found_file_type
				// my_InputPath - the parent path
				// my_InputName - the dir or file name
				if ((my_InputPath.equals("") == true) && (processing_runtype == 0)) {			// no more files do cleanup run
					if ( mainXSLTFrame.sourceFileAction.equals("") == false) {		// if this is empty we have to do no cleanup
						processing_runtype++;
						tree_ok = build_path_tree(mainXSLTFrame.sourcePathName);	// rescan tree: may be a file was added
						searchFileIdx = 0;
						the_found_file_type = get_file_from_tree(searchFileIdx);
						if (my_InputPath.equals("") == false) BatchXSLT.g_mainXSLTFrame.showMess( "*** Clean Up path: " + my_InputPath +"\n" );
					}
				}
			}
			else {	// a single file
				the_found_file_type = 1;	// set to ' a valid xml file'
				my_InputPath = split_path(the_xmlFileName + my_InputSubPath, false);
				my_InputName = split_name(the_xmlFileName + my_InputSubPath, false);
				BatchXSLT.g_mainXSLTFrame.showMess( "+++ Processing single file: '" + my_InputName + "'\n" );
				// like for Adobe archive file '.idml' for InDesign Markup Language
				g_numfiles_avail = 1;
			}
			if (my_InputPath.equals("")) {		// no more files
				try { Thread.sleep(300); }
				catch (InterruptedException e) {}
				BatchXSLTrans_lastMessage = "no more file(s).";
				//BatchXSLT.g_mainXSLTFrame.showRunMess( "no more file(s)." );
				mainXSLTFrame.transformStausMessage = "idle";
				return(0);
			}

			if (my_InputPath.endsWith(File.separator) == false)  my_InputPath = my_InputPath + File.separator;

			// checkif we have to extract an Adobe archive file '.idml' for InDesign Markup Language
			if ( (processing_runtype == 0) && (the_xslFileName.equals("*NONE*") == false) && (my_InputName.toLowerCase().endsWith(".idml")) ) {	// idml ODF (zip) archive: unpack only if we do a transform (not when FTP transfer
				String idmlPath ="";
				String idmlArchiveFolder = "";
				String IDMLmergedPath = "";
				String IDMLmergedXMLName ="";
				// extract the archive and merge all contained xml files into one single xml
				BatchXSLT.g_mainXSLTFrame.showMess( "*** Merging IDML package (index " + searchFileIdx + "): '" + my_InputName + "'\n");		//+ "' into path: '" + IDMLmergedPath + "' XML name: '" + IDMLmergedXMLName + "'\n" );
				String mergedIDMLname = com.epaperarchives.batchxslt.utils.mergeIDML(my_InputPath, my_InputName, "", my_InputPath, "", mainXSLTFrame.IDMLprocessingMessages);
				if (mergedIDMLname.equals("") == false) {
					idmlPath = com.epaperarchives.batchxslt.utils.getIDMLexpandParentFolder();
					idmlArchiveFolder = com.epaperarchives.batchxslt.utils.getIDMLexpandFolderName();
					IDMLmergedPath = com.epaperarchives.batchxslt.utils.getIDMLmergedPath();
					IDMLmergedXMLName = com.epaperarchives.batchxslt.utils.getIDMLmergedXMLName();
					BatchXSLT.g_mainXSLTFrame.showMess( "*** Merged IDML package '" + my_InputName + "' into path: '" + IDMLmergedPath + "' XML name: '" + IDMLmergedXMLName + "'\n" );
					//BatchXSLT.g_mainXSLTFrame.showMess( "*** Deleting temporary archive structure: '" + idmlArchiveFolder + "' at path: '" + idmlPath + "'\n" );
					if (mainXSLTFrame.IDMLpreserveExpandedPackage == 0) com.epaperarchives.batchxslt.utils.deleteDirectory(new File(idmlPath + idmlArchiveFolder));
					isIDMLpackage = true;
					my_InputPath = IDMLmergedPath;
					my_InputName = IDMLmergedXMLName;
				}
				else {	// had an error
					BatchXSLT.g_mainXSLTFrame.showMess( "### ERROR " + com.epaperarchives.batchxslt.utils.getIDMLexpandErrorNum() + " handling IDML package '" + my_InputName + ": '" + com.epaperarchives.batchxslt.utils.getIDMLexpandError() + "'\n" );
					// delete the corrupt file in temporary directory
					idmlPath = com.epaperarchives.batchxslt.utils.getIDMLexpandParentFolder();
					idmlArchiveFolder = com.epaperarchives.batchxslt.utils.getIDMLexpandFolderName();
					BatchXSLT.g_mainXSLTFrame.showMess( "### Deleting corrupt file: '" + idmlArchiveFolder + "' at path: '" + idmlPath + "'\n" );
					if (mainXSLTFrame.IDMLpreserveExpandedPackage == 0) com.epaperarchives.batchxslt.utils.deleteFile(idmlPath + idmlArchiveFolder);
					if (tree_ok) {	// was from directory tree: skip and continue
						searchFileIdx++;
						continue;
					}
					else {	// was a single file: exit
						mainXSLTFrame.transformStausMessage = "idle";
						return(-3);
					}
				}
				
				if ((IDMLmergedXMLName.equals("") == false) && tree_ok && (mf_tree != null)) { // input path is a directory: add the newly merged .imx file to the tree
					// ad .imx file to mf_tree
					if (searchFileIdx < (mf_tree.file_list_size())) mf_tree.insertFile(IDMLmergedPath, IDMLmergedXMLName, searchFileIdx + 1);	// insert after current IDML file
					//else mf_tree.insertFile(IDMLmergedPath, IDMLmergedXMLName, searchFileIdx);
					searchFileIdx++;	// and skip the idml file entry 
										// searchFileIdx now points to the newly created merged .imx file
				}
				
			}
			
			// check if the source path or path/name exists
			f_exists = file_exists( my_InputPath + my_InputName );	// check if path or file still exists
			if (!f_exists) {
				BatchXSLT.g_mainXSLTFrame.showRunMess( "Path/File not found." );
				try { Thread.sleep(300); }
				catch (InterruptedException e) {}
				BatchXSLTrans_lastMessage = "Path or file not found: '" +  my_InputPath + my_InputName + "'";
				mainXSLTFrame.transformStausMessage = "idle";
				return(-1);
			}
			if (mainXSLTFrame.general_abort == true) {
				BatchXSLTrans_lastMessage = "Transform aborted";
				mainXSLTFrame.transformStausMessage = "idle";
				return(2);
			}

			file_processing++;	// inc the current file counter
			BatchXSLT.g_mainXSLTFrame.showRunMess( "Processing " + file_processing + "/" + g_numfiles_avail + " file(s)...");

			// at this point we have a path and a name either of a dir or a file
			// we now set the my_InputSubPath viewed from the main hotfolder path
			my_InputSubPath = get_sub_path(the_xmlFileName,my_InputPath);

			// --------------------------------
			// check, if we do simple file transfers only: if the_xslFileName.equals("*NONE*") == true
			if ( (processing_runtype == 0) && (the_outputFileName.equals("*NONE*") == false) && (the_xslFileName.equals("*NONE*") == true) ) {	// do simple file transfers
				if (the_found_file_type != 2) {	// don't copy folders
					if (mainXSLTFrame.outputPathRoot.equals("") == false) the_outputFileName = mainXSLTFrame.outputPathRoot + the_outputFileName;
					boolean doclose;
					//BatchXSLT.g_mainXSLTFrame.showMess("+++++++++++++++++++ copy file searchFileIdx: " + searchFileIdx + " of " + g_numfiles_avail);
					if (searchFileIdx < (g_numfiles_avail - 1)) doclose = false;
					else doclose = true;
					myErr = copyFile(my_InputPath, my_InputName, the_outputFileName + my_InputSubPath.replace('\\','/'), "", 1,true,doclose);
				}
				searchFileIdx++;
				continue;
			}

			// --------------------------------
			// we have to do XML transforms or are in cleanup run
			if (inputpath_is_Dir == true) {	// we received a file from the file tree
				// we should xml-process normal files only
				switch (the_found_file_type) {
					case 0:			// undefined file type (should not happen)
						searchFileIdx++;
						continue;
					case 1:			// is a valid XML file
					case 4:			// is an IDML package file
						//if (processing_runtype == 1) { searchFileIdx++; continue; }		// do it in cleanup run
						//break;
						// don't do it here, do it AFTER succesful processing! at the end of this loop
						// searchFileIdx++;	// prepare idx for the next input file from list
						// ... and process this xml file
						if (processing_runtype == 0) break;	// do the XSLT
						// fall through to clean up
					case 3:			// is an invalid file - copy/delete/do nothing - and get next name
						if (processing_runtype == 0) { searchFileIdx++; continue; }		// do it in cleanup run
						myErr = doSourceFileActions(my_InputPath, my_InputName, my_InputSubPath);
                        if (myErr != 0) {   // uuups - an error deleting the file...? ignore
						}
						searchFileIdx++;
						if ( mf_tree.is_folder_empty(my_InputPath, 
								(mainXSLTFrame.deleteSourceDirs > 0) ? true : false) ) {	// delete dir if empty
						}
						continue;
					case 2:			// is a dir
						//if (processing_runtype == 0) { searchFileIdx++; continue; }		// do it in cleanup run
						// check if dir is empty - if yes: delete the dir else search files in whole path
						if (mf_tree.is_empty_folder(searchFileIdx)) {	// is empty
							if (mainXSLTFrame.deleteSourceDirs > 0) {	// are we allowed to delete empty dirs
								//BatchXSLT.g_mainXSLTFrame.showMess( "---- Empty folder: '" + my_InputPath + "' # '" + my_InputName + "' # '" + my_InputSubPath + "'\n" );
								myErr = doSourceFileActions(my_InputPath, my_InputName, my_InputSubPath);
							}
						}
						searchFileIdx++;	// get next file from list
						continue;


					case -1:			// no security manager avail
						if (processing_runtype == 0) { searchFileIdx++; continue; }		// do it in cleanup run
						BatchXSLT.g_mainXSLTFrame.showRunMess( "### No security Manager available" );
						searchFileIdx++;
						continue;
					default:
					case 42:			// no write access
					case 43:
					case 49:
						if (processing_runtype == 0) { searchFileIdx++; continue; }		// do it in cleanup run
						BatchXSLT.g_mainXSLTFrame.showMess( "### No write access " + the_found_file_type + ": '" + my_InputPath + my_InputName + "'\n" );
						searchFileIdx++;
						continue;
					case 45:			// not enough chars
						if (processing_runtype == 0) { searchFileIdx++; continue; }		// do it in cleanup run
						BatchXSLT.g_mainXSLTFrame.showMess( "### Not enough bytes (<=0) " + the_found_file_type + ": '" + my_InputPath + my_InputName + "'\n" );
						searchFileIdx++;
						continue;
					case 44:			// no read access
					case 46:
						if (processing_runtype == 0) { searchFileIdx++; continue; }		// do it in cleanup run
						BatchXSLT.g_mainXSLTFrame.showMess( "### No read access " + the_found_file_type + ": '" + my_InputPath + my_InputName + "'\n" );
						searchFileIdx++;
						continue;
					case 47:			// no delete access
						if (processing_runtype == 0) { searchFileIdx++; continue; }		// do it in cleanup run
						BatchXSLT.g_mainXSLTFrame.showMess( "### No delete access " + the_found_file_type + ": '" + my_InputPath + my_InputName + "'\n" );
						searchFileIdx++;
						continue;
					case 100:			// found a file whose name extension is in the exclude list
						searchFileIdx++;
						continue;
				}

				try { Thread.sleep(50); }
				catch (InterruptedException e) {}
			}
			else {	// the file name is evtl. already given in the whole path
				if (my_InputPath.endsWith(my_InputName) == true) my_InputPath = my_InputPath.substring(0,my_InputPath.length() - my_InputName.length());
			}

			//======================================================================
			// if we are here, then we have to process an XML file
			// input path and name are set up in 'my_InputPath' and 'my_InputName'
			// to do a XML Transformation with this input xml file
			// ---- output path stuff
			if (the_outputFileName.equals("*NONE*") == true) {			// create NO output file (validating only or done by xsl)
				my_OutputPath = "";										// target to no file output
				my_OutputName = "*NONE*";
				outputpath_is_Dir = false;
			}
			else {
				my_OutputName = "";
				// check the given output the_outputFileName
				if (the_outputFileName.equals("") == true) {			// no output path given - set it up
					my_OutputPath = my_InputPath;						// target to same path as input
					outputpath_is_Dir = true;
				}
				else {
					if (   (the_outputFileName.endsWith(File.separator) == true)
						|| (the_outputFileName.endsWith("/") == true)			// if FTP target
						) {
						my_OutputPath = mainXSLTFrame.outputPathRoot + the_outputFileName + my_InputSubPath;
						outputpath_is_Dir = true;
					}
					else {	// a file path/name is given
						my_OutputPath = mainXSLTFrame.outputPathRoot + split_path(the_outputFileName, false);
						if (my_OutputPath.endsWith(File.separator) == false) my_OutputPath = my_OutputPath + File.separator;
						my_OutputName = split_name(the_outputFileName, false);
						outputpath_is_Dir = true;
					}
				}
			}

			if (my_OutputName.equals("*NONE*") == false) {			// if we have to create an output file
				result_type = get_URL_file_type(my_OutputPath);		// local file or url?
				switch (result_type) {
					case 2:	// an ftp URI
					case 3:	// an http URI
						// we let transform to a local file and then copy it via ftp to the remote location
						// !!!!!!! IF we have to make a transform!
						if (the_xslFileName.equals("*NONE*") == false) {	// do transform if not suppressed
																// otherwise it is just a file move 
							actual_my_OutputPath = my_OutputPath;	// store real target path and name
							if (my_OutputName.equals("")) my_OutputName = my_InputName;
							actual_my_OutputName = my_OutputName;
							boolean wd_created = make_dirpath(work_subdir);	// create a working dir in the current input path
							//my_OutputPath = my_InputPath + work_subdir;
							my_OutputPath = work_subdir;
							my_OutputName = my_InputName;
							is_working_output = true;
						}
						break;
					default: {	// result_type == 1 means a local file
						// check if the given path in 'my_OutputPath' exists - else create it
						if (!file_exists(my_OutputPath)) { retflag = make_dirpath(my_OutputPath); }
						break;
					}
				}
				// output path is set up or '*NONE*'

				//======================================================================
				// ---- output file name stuff
				if (my_OutputName.equals("")) {			// no output name given - set it up from input file
					my_OutputName = my_InputName;
				}
				// check if we have to change the out file name extension
				if (mainXSLTFrame.newoutputFileNameExt.equals("")) {	// no new extension given - use input file name
					if ((my_OutputPath.compareTo(my_InputPath) == 0)
						&& (my_InputName.compareTo(my_OutputName) == 0) ) change_output_nameext = true; // same path and name
				}
				else change_output_nameext = true;

				if (change_output_nameext == true) {	// cut file name ext from found input file and ad new ext
					int namelen, pointpos = -1;
					String my_outputFileNameExt = "";
					namelen = my_OutputName.length();
					do {
						if (namelen < 1) break;
						if (my_OutputName.charAt(namelen-1) == '.') { pointpos = namelen-1; break; }
						if (namelen < 2) break;
						if (my_OutputName.charAt(namelen-2) == '.') { pointpos = namelen-2; break; }
						if (namelen < 3) break;
						if (my_OutputName.charAt(namelen-3) == '.') { pointpos = namelen-3; break; }
						if (namelen < 4) break;
						if (my_OutputName.charAt(namelen-4) == '.') { pointpos = namelen-4; break; }
						if (namelen < 5) break;
						if (my_OutputName.charAt(namelen-5) == '.') { pointpos = namelen-5; break; }
						if (namelen < 6) break;
						if (my_OutputName.charAt(namelen-6) == '.') { pointpos = namelen-6; break; }
						break;
					} while (false);
					// prep the new name extension
					my_outputFileNameExt = mainXSLTFrame.newoutputFileNameExt;
					if (my_outputFileNameExt.equals("")) {	// none is given - create a default one
						my_outputFileNameExt = "_tr";
						if (pointpos >= 1) my_outputFileNameExt = my_outputFileNameExt + my_OutputName.substring(pointpos,namelen);
					}

					// cut ext from current output name
					if (pointpos >= 1) my_OutputName = my_OutputName.substring(0,pointpos);
					// set the new output name
					my_OutputName = my_OutputName + my_outputFileNameExt;
					if ((my_OutputPath.equals(my_InputPath) == true) && (my_OutputName.equals(my_InputName) == true)) {	// equal inut and output path/names?
						my_OutputName = my_OutputName.substring(0,pointpos);
						my_OutputName = my_OutputName + "_tr" + my_outputFileNameExt;
					}
					my_OutputName = add_vars(my_OutputName);	// expand with &&var&
					actual_my_OutputName = my_OutputName;
				}
			}

			/* ======================================================================
			 * all jt_file settings are done - start the XSL Transformation
			 */

			// check if we have to run an external application before transforming
			if (mainXSLTFrame.runBeforeTransformApp.equals("") == false) {
				BatchXSLT.g_mainXSLTFrame.showMess( "*** calling external application (before transform): " + mainXSLTFrame.runBeforeTransformApp + "\n" );
				int retval = com.epaperarchives.batchxslt.utils.callExternalApplication(mainXSLTFrame.runBeforeTransformApp);
				if (retval != 0) {
					BatchXSLT.g_mainXSLTFrame.showMess( "### External Application (before transform) exit value: " + retval + "\n" );
					String stdinresp = utils.callExternalAppGetStdinResponse();
					String errorresp = utils.callExternalAppGetErrorResponse();
					if (stdinresp.equals("") == false) System.out.println("External App STDIN: " + errorresp);
					if (errorresp.equals("") == false) System.out.println("External App ERROR: " + errorresp);
				}
				//BatchXSLT.g_mainXSLTFrame.runBeforeTransformApp = "";
			}

			if ( the_xslFileName.equals("*NONE*") == false ) {	// do transform if not suppressed
																// otherwise it is just a file move 
 				if (mainXSLTFrame.general_abort == true) break;

				// transform it
				BatchXSLT.g_mainXSLTFrame.showRunMess( "Processing " + file_processing + "/" + g_numfiles_avail + " file(s)...");
				myErr = doXSLT( my_InputPath + my_InputName, the_xslFileName, my_OutputPath + my_OutputName);
				//-- cleanUpMemory();

	 			if (mainXSLTFrame.general_abort == true) break;

				// .. evtl. copy working file(s) to the remote ( FTP ) destination location
				if (is_working_output == true) {
					try { com.epaperarchives.batchxslt.utils.copyFolder(my_OutputPath, actual_my_OutputPath, true, true); } catch (IOException e) {}
					copyFile_disconnect();	// we have ftp transferrred files and now have to disconnect
					is_working_output = false;
					my_OutputPath = actual_my_OutputPath;
					my_OutputName = actual_my_OutputName;
				}
			}
			
			// check if we have to run an external application after transforming
			if (mainXSLTFrame.runAfterTransformApp.equals("") == false) {
				BatchXSLT.g_mainXSLTFrame.showMess( "*** calling external application (after transform): " +mainXSLTFrame. runAfterTransformApp + "\n" );
				int retval = com.epaperarchives.batchxslt.utils.callExternalApplication(mainXSLTFrame.runAfterTransformApp);
				if (retval != 0) {
					BatchXSLT.g_mainXSLTFrame.showMess( "### External Application (after transform) exit value: " + retval + "\n" );
					String stdinresp = utils.callExternalAppGetStdinResponse();
					String errorresp = utils.callExternalAppGetErrorResponse();
					if (stdinresp.equals("") == false) System.out.println("External App STDIN: " + errorresp);
					if (errorresp.equals("") == false) System.out.println("External App ERROR: " + errorresp);
				}
				//BatchXSLT.g_mainXSLTFrame.runAfterTransformApp = "";
			}
			
	 		if (mainXSLTFrame.general_abort == true) break;
			// .. and clean up if last err was not fnfErr
			//if (myErr != -1) {
			if (myErr == 0) {	// clean up if we had no err
				myErr = doSourceFileActions(my_InputPath, my_InputName, my_InputSubPath);
				if (myErr != 0) break;	// uuups - an error deleting the file...?
			}

			// check if dir is empty - if yes: delete the dir else search files in whole path
			if ( mf_tree != null ) {	// this is set only in batch mode, not in single file transform mode
				mf_tree.is_folder_empty(my_InputPath,(mainXSLTFrame.deleteSourceDirs > 0) ? true : false );	// delete dir if empty
			}
/*
			System.out.println("***** BatchXSLTrans - g_numfiles_avail: " + g_numfiles_avail);
			System.out.println("                                  mode: " + BatchXSLT.g_mainXSLTFrame.mode);
			System.out.println("                         searchFileIdx: " + searchFileIdx);
			System.out.println("                      inputpath_is_Dir: " + inputpath_is_Dir);
*/
			// we may not rescan if input and output paths are equal - because we might catch an already transformed file!
			if (my_OutputPath.equals(my_InputPath) && my_InputName.equals(my_OutputName)) break;

			// if we had a single path/name then we process this file only; else we want to scan the whole dir path!
			//if (BatchXSLT.g_mainXSLTFrame.mode == 0 && (inputpath_is_Dir == false)) break;
			if ((g_numfiles_avail <= 1) && (inputpath_is_Dir == false)) break;
			if (process_single_file) break;

 			if (mainXSLTFrame.general_abort == true) break;


			searchFileIdx++;	// prepare idx for the next input file from list

		} while (true);

		BatchXSLT.g_mainXSLTFrame.showRunMess( "" );
		mainXSLTFrame.transformStausMessage = "idle";
		//-- cleanUpMemory();
		return(0);
	}




	/* =========================================================================
	 * Procedures
	   ========================================================================= */




	/* ===========================================================
	 * call the XSL Transformator
	 */
	public static int doXSLT(String the_inputFileName, String the_xslFileName, String the_outputFileName)
	{
		URL					out_url; 
		URLConnection		out_urlConn; 
		FileOutputStream	outputStream = null;
		int					anErr = 0;
		String				my_inputFileName = "";
		String				my_xslFileName = "";
		String				my_outputFileName = "";
		int 				source_type = 0;			// the type of the source to handle
		int 				result_type = 0;			// the type of the target to handle

		File				my_source_file = null;		// the input XML file
		StreamSource		my_stream_source = null;	// ... this file as StreamSource
		StreamResult		my_stream_result = null;

		//BatchXSLT.g_mainXSLTFrame.showMess( "**** doXSLT the_inputFileName: '" + the_inputFileName + "'\n     the_xslFileName: '" + the_xslFileName + "'\n     the_outputFileName: '" + the_outputFileName + "'\n" );

		// clear the transform error counters
		_XMLvalidatingMaxErrorMessages = mainXSLTFrame.XMLvalidatingMaxErrorMessages;
		_XMLvalidatingMaxFatalErrorMessages = mainXSLTFrame.XMLvalidatingMaxFatalErrorMessages;
		_XMLvalidatingMaxWarningMessages = mainXSLTFrame.XMLvalidatingMaxWarningMessages;

		// evtl. clear the message window from time to time
		if (mainXSLTFrame.numTransformsDone > 100) {
			BatchXSLT.g_mainXSLTFrame.clearMess(1);
			mainXSLTFrame.numTransformsDone = 0;
		}
		mainXSLTFrame.numTransformsDone++;



		// make proper URL names		
		if (!isMacintosh()) my_inputFileName = the_inputFileName;
		else my_inputFileName = makeURLString(the_inputFileName);
		if (!isMacintosh()) my_xslFileName = the_xslFileName;
		else my_xslFileName = makeURLString(the_xslFileName);
		// add full path
		if ((my_xslFileName.equals("") == false)
			&& (my_xslFileName.indexOf(File.separator) != 0)	// given with root
			&& (my_xslFileName.indexOf(":") != 1)	// first letter is drive letter
			&& (my_xslFileName.indexOf("\\") != 0)	// UNC path like \\server\share\file_path 
			) my_xslFileName = utils.file_fullPath(BatchXSLT.g_mainXSLTFrame.appDir, my_xslFileName);
		
		if (the_outputFileName.equals("*NONE*") == true) {		// create NO output file (validating only or done by xsl)
			my_outputFileName = "*NONE*";
		}
		else {													// we have to create an output file
			if (!isMacintosh()) my_outputFileName = the_outputFileName;
			else my_outputFileName = makeURLString(the_outputFileName);
		}


		BatchXSLT.g_mainXSLTFrame.showMess( "Transforming:\n");

		do {
			// -----------------------------
			// determine the source type
			source_type = 1;	// assume a file
			switch (source_type) {
				case 2:	// an URI - make a StreamSource from a URI object
					break;
				case 1:	// a 'normal' file from the file system - make a StreamSource from a File object
					try {	// check if the local file(s) exist
						FileInputStream inputStream = new FileInputStream( the_inputFileName );
						try { inputStream.close(); }
						catch ( java.io.IOException e ) { }
					}
					catch (FileNotFoundException e) {
						BatchXSLT.g_mainXSLTFrame.showMess( "#### XSLT Transformator: Source file not found!\n");
						return(-1);
					}

					my_source_file = new File(my_inputFileName);	// must create from a file because 'getAssociatedStylesheet' will not work otherwise!
 					my_stream_source = new StreamSource(my_source_file);
					// Construct a StreamSource from a URL
					String sourceSystemId = ""; 
					try {
						sourceSystemId = my_source_file.toURI().toASCIIString();
					} catch (Exception e) {
					}
					//BatchXSLT.g_mainXSLTFrame.showMess( "sourceSystemId: " + sourceSystemId + "\n");
					my_stream_source.setSystemId(sourceSystemId);
					//my_stream_source = new StreamSource(sourceSystemId);

					
					break;
				default:
					anErr = 1;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### Incompatible Source file type error " + anErr + "!\n" );
					break;
			}
			if (anErr != 0) break;
			BatchXSLT.g_mainXSLTFrame.showMess( "  Source file:\t" + my_inputFileName + "\n");

			// -----------------------------
			// determine the result type
			if (my_outputFileName.equals("*NONE*") == false) {		// create NO output file (validating only or done by xsl)
				result_type = get_URL_file_type(my_outputFileName);	// local file or url?
				switch (result_type) {
					case 2:	// an ftp URI - make a StreamSource from a URI object
					case 3:	// an http URI - make a StreamSource from a URI object
						my_stream_result = new StreamResult(my_outputFileName);
						if (my_stream_result == null) {
							anErr = 11;
							BatchXSLT.g_mainXSLTFrame.showMess( "#### Output StreamResult error :" + anErr + "'\n" );
							break;
						}
						break;
					case 1:	// a 'normal' file from the file system - make a FileOutputStream from a File object
						try { outputStream = new FileOutputStream(my_outputFileName); }
						catch (java.io.IOException e) {
							anErr = 12;
							BatchXSLT.g_mainXSLTFrame.showMess( "#### FileOutputStream IOException " + anErr + ": '" + e.getMessage() + "'\n" );
							break;
						}
						my_stream_result = new StreamResult(outputStream);
						if (my_stream_result == null) {
							anErr = 13;
							BatchXSLT.g_mainXSLTFrame.showMess( "#### Output StreamResult error :" + anErr + "'\n" );
							break;
						}
						break;
					default:
						anErr = 10;
						BatchXSLT.g_mainXSLTFrame.showMess( "#### Incompatible Output file type error " + anErr + "!\n" );
						break;
				}
			}
			else {			// no output
				my_stream_result = new StreamResult(new NullOutputStream());
			}
			if (anErr != 0) break;
			BatchXSLT.g_mainXSLTFrame.showMess( "  to output:\t" + ((my_outputFileName.compareTo("") == 0) ? "screen\n" : my_outputFileName + "\n") );
		
			// Have the XSLTProcessorFactory obtain an interface to a new XSLTProcessor object.
			Transformer transformer = null;

			// Use the static TransformerFactory.newInstance() method to instantiate 
			// a TransformerFactory. The javax.xml.transform.TransformerFactory 
			// system property setting determines the actual class to instantiate --
			// org.apache.xalan.transformer.TransformerImpl.
			TransformerFactory tFactory = TransformerFactory.newInstance();
	//		URIResolver myResolver;
	//		myResolver = new myURIResolver();
	//		tFactory.setURIResolver(myResolver);


			// Use the TransformerFactory to instantiate a Transformer that will work with  
			// the stylesheet you specify (or none to use the one stated in the source).
			// This method call also processes the stylesheet into a compiled Templates object.
		//BatchXSLT.g_mainXSLTFrame.showMess( "**** doXSLT my_inputFileName: '" + my_inputFileName + "'\n     my_xslFileName: '" + my_xslFileName + "'\n     my_outputFileName: '" + my_outputFileName + "'\n" );
			if (my_xslFileName.equals( "" ) == true) {	// no external XSL given: use the internal
				Source stylesheet = null;
				String media= null , title = null, charset = null;

				try {
					stylesheet = tFactory.getAssociatedStylesheet( my_stream_source, media, title, charset );
				}
				catch (TransformerConfigurationException e) {
					anErr = 20;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### External Stylesheet Exception " + anErr + ": '" + e.getMessage() + "'\n" );
					e.printStackTrace();
					//break;
				}
				if ( stylesheet == null) {
					BatchXSLT.g_mainXSLTFrame.showMess( "#### No associated XSL found'\n" );
					anErr = 21;
					break;
				}
				try { transformer = tFactory.newTransformer( stylesheet ); }
				catch (TransformerConfigurationException e) {
					anErr = 22;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### Transformer ConfigurationException when loading XSL " + anErr + ": '" + e.getMessage() + "'\n" );
					e.printStackTrace();
					break;
				}
			}
			else {	// use the given external XSL
				int b_len = 30;
				byte[] b = new byte[b_len];
				boolean is_xml = false;
				StreamSource xslt_inputstream = null;
				boolean f_exists = file_exists(my_xslFileName);
				if (f_exists) {	// try to open local XSL file
					try {
						xslt_inputstream = new StreamSource( new FileInputStream(my_xslFileName) );
						xslt_inputstream.setSystemId(new File(my_xslFileName));
					}
					catch (IOException e) {}	// should not happen because we have tested if file exists
					try {	// it might be encrypted
						BufferedInputStream bis = new BufferedInputStream(xslt_inputstream.getInputStream(),b_len);
						bis.mark(b_len); bis.read(b); bis.reset(); xslt_inputstream.setInputStream(bis);

						// check if is plain xml or encrypted
						for ( int i = 0; (i < (b.length-6)) && (i < 500); i++) {	// check if we find '<?xml'
							if ( ((b[i] == '<') && (b[i+1] == '?') && (b[i+2] == 'x') && (b[i+3] == 'm') && (b[i+4] == 'l'))
								|| ((b[i] == '<') && (b[i+1] == '!') && (b[i+2] == '-')) ) {
								is_xml = true; break;	// YES - is an XML file
							}
						}
						if ( !is_xml ) {	// might be encrypted?
							// check if we can decrypt 

							CipherInputStream cis = dcs(bis);
							xslt_inputstream.setInputStream(cis);
							if (xslt_inputstream == null) {
								anErr = 232; BatchXSLT.g_mainXSLTFrame.showMess( "#### XSL loading error: " + anErr + "'\n" ); break;
							}
							bis = new BufferedInputStream(xslt_inputstream.getInputStream(),b_len);
							bis.mark(b_len); bis.read(b); bis.reset(); xslt_inputstream.setInputStream(bis);
					//BatchXSLT.g_mainXSLTFrame.showMess( "******* decrypted2: '" +  new String(b) + "'\n" );
							// check if is decrypted now
							for ( int i = 0; (i < (b.length-6)) && (i < 500); i++) {	// check if we find '<?xml'
								if ( ((b[i] == '<') && (b[i+1] == '?') && (b[i+2] == 'x') && (b[i+3] == 'm') && (b[i+4] == 'l'))
									|| ((b[i] == '<') && (b[i+1] == '!') && (b[i+2] == '-')) ) {
									is_xml = true; break;	// YES - is an XML file
								}
							}
							if ( !is_xml ) {
								anErr = 233; BatchXSLT.g_mainXSLTFrame.showMess( "#### XSL loading error: " + anErr + "'\n" ); break;
							}
						}
					}
					catch (IOException e) {
						anErr = 230; BatchXSLT.g_mainXSLTFrame.showMess( "#### Transformer IOException when loading XSL " + anErr + ": '" + e.getMessage() + "'\n" ); break;
					}
				}
				else {
					// could not load the given stylesheet from local disk: try loading from encrypted jar
					xslt_inputstream = null;
					//BatchXSLT.g_mainXSLTFrame.showMess( "******* loading XSL from jar 1: " + my_xslFileName + "\n" );
					InputStream xslt_is = BatchXSLTransform.class.getClassLoader().getResourceAsStream(my_xslFileName);
					if (xslt_is != null) {
						xslt_inputstream = new StreamSource( xslt_is );
					//BatchXSLT.g_mainXSLTFrame.showMess( "******* loading XSL from jar 2: " + my_xslFileName + "\n" );
					}
					if (xslt_inputstream != null) {
						try {	// it might be encrypted
					//BatchXSLT.g_mainXSLTFrame.showMess( "******* loading XSL from jar: 3\n" );
					//BatchXSLT.g_mainXSLTFrame.showMess( "******* avalable bytes: '" + xslt_inputstream.getInputStream().available() + "'\n" );
							BufferedInputStream bis = new BufferedInputStream(xslt_inputstream.getInputStream(),b_len);
					//BatchXSLT.g_mainXSLTFrame.showMess( "******* avalable bytes: '" + xslt_inputstream.getInputStream().available() + "'\n" );
							bis.mark(b_len); bis.read(b); bis.reset(); xslt_inputstream.setInputStream(bis);
					//BatchXSLT.g_mainXSLTFrame.showMess( "******* decrypted1: '" + new String(b) + "'\n" );
							// check if is plain xml or encrypted
							for ( int i = 0; i < (b.length-6); i++) {	// check if we find '<?xml'
								if ( ((b[i] == '<') && (b[i+1] == '?') && (b[i+2] == 'x') && (b[i+3] == 'm') && (b[i+4] == 'l'))
									|| ((b[i] == '<') && (b[i+1] == '!') && (b[i+2] == '-')) ) {
									is_xml = true; break;	// YES - is an XML file
								}
							}
							if ( !is_xml ) {	// might be encrypted?
								CipherInputStream cis = dcs(bis);
								xslt_inputstream.setInputStream(cis);
								if (xslt_inputstream == null) {
									anErr = 242; BatchXSLT.g_mainXSLTFrame.showMess( "#### XSL loading error: " + anErr + "'\n" ); break;
								}
								bis = new BufferedInputStream(xslt_inputstream.getInputStream(),b_len);
								bis.mark(b_len); bis.read(b); bis.reset(); xslt_inputstream.setInputStream(bis);
					//BatchXSLT.g_mainXSLTFrame.showMess( "******* decrypted2: '" +  new String(b) + "'\n" );
								// check if is decrypted now
								for ( int i = 0; i < (b.length-6); i++) {	// check if we find '<?xml'
									if ( ((b[i] == '<') && (b[i+1] == '?') && (b[i+2] == 'x') && (b[i+3] == 'm') && (b[i+4] == 'l'))
										|| ((b[i] == '<') && (b[i+1] == '!') && (b[i+2] == '-')) ) {
										is_xml = true; break;	// YES - is an XML file
									}
								}
								if ( !is_xml ) {
									anErr = 243; BatchXSLT.g_mainXSLTFrame.showMess( "#### XSL loading error: " + anErr + "'\n" ); break;
								}
							}
						}
						catch (IOException e) {
							anErr = 241; BatchXSLT.g_mainXSLTFrame.showMess( "#### Transformer IOException when loading XSL " + anErr + ": '" + e.getMessage() + "'\n" ); break;
						}
					}
					else {
							anErr = 240;
							BatchXSLT.g_mainXSLTFrame.showMess( "#### XSL file '" + my_xslFileName + "' not found!\n" );
							break;
					}
				}
				try {
					//transformer = tFactory.newTransformer( new StreamSource( my_xslFileName ) );
					transformer = tFactory.newTransformer( xslt_inputstream );
				}
				//catch (TransformerConfigurationException e) {
				catch (Exception e) {
					anErr = 24;
					BatchXSLT.g_mainXSLTFrame.showMess( "#### Transformer ConfigurationException when loading XSL " + anErr + ": '" + e.getMessage() + "'\n" );
					break;
				}
			}
			BatchXSLT.g_mainXSLTFrame.showMess( "  using XSL file:\t" + my_xslFileName + "\n");

			// pass parameters to the stylesheet
			try {
				// our default parameters
				transformer.setParameter("XMLSRC_VERSION",xmlsrc_version);
				transformer.setParameter("XMLSRC_ENCODING",xmlsrc_encoding);
				transformer.setParameter("XMLSRC_DOCTYPE_DECLARATION",xmlsrc_doctype_declaration);
				transformer.setParameter("SYSTEM_OS_NAME",mainXSLTFrame.systemOSname);
				transformer.setParameter("SYSTEM_VM_VERSION",mainXSLTFrame.VMversion);
				transformer.setParameter("SYSTEM_DEFAULT_CHARSET",mainXSLTFrame.systemDefaultCharset);
				transformer.setParameter("TRANSFORM_ENGINE",mainXSLTFrame.applFullName + " " + mainXSLTFrame.applMajorVersion + "." + mainXSLTFrame.applMinorVersion);

				transformer.setParameter("INPUT_PATH",split_path(my_inputFileName, false) + File.separator);
				transformer.setParameter("INPUT_SUB_PATH",BatchXSLTransform.my_InputSubPath);
				transformer.setParameter("INPUT_NAME",split_name(my_inputFileName, false));

				if ((my_outputFileName.equals("*NONE*") == false) && (my_outputFileName.equals("") == false)) {
					String path = split_path(my_outputFileName, false);
					if (path.equals("") == false) transformer.setParameter("OUTPUT_PATH",path + File.separator);
					else transformer.setParameter("OUTPUT_PATH","");
					transformer.setParameter("OUTPUT_NAME",split_name(my_outputFileName, false));
				}
				else {
					transformer.setParameter("OUTPUT_PATH","");
					transformer.setParameter("OUTPUT_NAME","");
				}
				File tmpfile = new File(my_xslFileName);
				transformer.setParameter("STYLESHEET_PATH",split_path(tmpfile.getAbsolutePath(), false));
				transformer.setParameter("STYLESHEET_NAME",split_name(tmpfile.getName(), false));

				transformer.setParameter("LOGFILE_WRITE",mainXSLTFrame.logfile_write);
				transformer.setParameter("LOGFILE_PATH",mainXSLTFrame.logfile_path);
				transformer.setParameter("LOGFILE_NAME",mainXSLTFrame.logfile_name);

				transformer.setParameter("APP_DIR",mainXSLTFrame.appDir);
				transformer.setParameter("USER_NAME",mainXSLTFrame.userName);
				transformer.setParameter("USER_DIR",mainXSLTFrame.userDir);
				transformer.setParameter("USER_HOME",mainXSLTFrame.userHome);
				transformer.setParameter("LOCAL_MACHINE_NAME",mainXSLTFrame.localMachineName);

				transformer.setParameter("GS_VERSION",mainXSLTFrame.gs_version_str);
				transformer.setParameter("GS_VERSION_NUM",mainXSLTFrame.gs_version_num);
				transformer.setParameter("GS_PGM_PATH",mainXSLTFrame.gs_pgm_path);
				transformer.setParameter("GS_ENVIR",mainXSLTFrame.gs_envir);
				transformer.setParameter("GS_PGM_PARMS_EPS2PDF",mainXSLTFrame.gs_pgm_parms_eps2pdf);

				transformer.setParameter("IM_VERSION",mainXSLTFrame.im_version_str);
				transformer.setParameter("IM_VERSION_NUM",mainXSLTFrame.im_version_num);
				transformer.setParameter("IM_PGM_PATH",mainXSLTFrame.im_pgm_path);
				transformer.setParameter("IM_PGM_CONVERT",mainXSLTFrame.im_pgm_CONVERT);
				transformer.setParameter("IM_PGM_IDENTIFY",mainXSLTFrame.im_pgm_IDENTIFY);
				transformer.setParameter("IM_ENVIR",mainXSLTFrame.im_envir);

				transformer.setParameter("COLORPROFILES_path",mainXSLTFrame.color_profiles_path);

				transformer.setParameter("PSTOTEXT_VERSION",mainXSLTFrame.pstotext_version_str);
				transformer.setParameter("PSTOTEXT_PGM_PATH",mainXSLTFrame.pstotext_pgm_path);
				transformer.setParameter("PSTOTEXT_ENVIR",mainXSLTFrame.pstotext_envir);
				transformer.setParameter("PSTOTEXT_PGM_PARMS_EPS2PDF",mainXSLTFrame.pstotext_pgm_parms_pdf2txt);


				// now all others set in the BatXSLT parameters list
				if (false == mainXSLTFrame.xslParams.equals("")) {
					String p_name = "", p_val = "";
					String params_str = mainXSLTFrame.xslParams;	// get the whole parameters (multiple lines)
					String tmpstr = "";
					while (params_str.equals("") == false) {
						// get one parameter line
						int pos = params_str.indexOf('\n');
						if (pos < 0) {	// a line not ending with a \n
							tmpstr = params_str;
							params_str = ""; 		// and we are done
						}
						else {	// a line ending with a \n
							tmpstr = params_str.substring(0,pos);
							params_str = params_str.substring(pos + 1);	// get rest of string
						}
						if ((tmpstr.equals("") == false) && (tmpstr.length() >= 2) && (tmpstr.startsWith("#") == false)) {	// we have a parameter name and value
							pos = tmpstr.indexOf('=');
							if (pos > 0) {
								p_name = tmpstr.substring(0,pos);
								p_val = tmpstr.substring(pos + 1);
								// now add the variable to the p_val
								p_val = add_vars(p_val);
								// let's set it
								transformer.setParameter(p_name,p_val);
							}
						}
					}
				}
			}
			catch (Exception e) {
				anErr = 25;
				BatchXSLT.g_mainXSLTFrame.showMess( "#### Transformer ConfigurationException on XSL " + anErr + ": '" + e.getMessage() + "'\n" );
				break;
			}


			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			docFactory.setXIncludeAware(true);
			docFactory.setNamespaceAware(true);
			if (BatchXSLT.g_mainXSLTFrame.doXMLvalidating > 0) docFactory.setValidating(true);
			else docFactory.setValidating(false);

			// set additional features. see: http://xerces.apache.org/xerces2-j/features.html
			try {	// allow non-unique IDs
				docFactory.setFeature("http://apache.org/xml/features/validation/id-idref-checking", false);
			} 
			catch (ParserConfigurationException e) {
				System.err.println("### ERROR in DocumentBuilderFactory: could not set parser feature \"http://apache.org/xml/features/validation/id-idref-checking\"");
			}
			try {	// allow on duplicate attributes
				docFactory.setFeature("http://apache.org/xml/features/validation/warn-on-duplicate-attdef", false);
			} 
			catch (ParserConfigurationException e) {
				System.err.println("### ERROR in DocumentBuilderFactory: could not set parser feature \"http://apache.org/xml/features/validation/warn-on-duplicate-attdef\"");
			}
			if (BatchXSLT.g_mainXSLTFrame.continueXMLparseOnFatalError > 0) {
				try {	// continue on fatal error
					//	USE WITH CAUTION!
					// The behavior of the parser when this feature is set to true is undetermined! Therefore use this feature with extreme caution because the parser may get stuck in an infinite loop or worse. 
					docFactory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
				} 
				catch (ParserConfigurationException e) {
					System.err.println("### ERROR in DocumentBuilderFactory: could not set parser feature \"http://apache.org/xml/features/continue-after-fatal-error\" to true.");
				}
			}
			else {
				try {
					docFactory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", false);
				} 
				catch (ParserConfigurationException e) {
					System.err.println("### ERROR in DocumentBuilderFactory: could not set parser feature \"http://apache.org/xml/features/continue-after-fatal-error\" to false");
				}
			}
			
			DocumentBuilder docBuilder = null;
			Document xmlSourceDocument = null;
			try { docBuilder = docFactory.newDocumentBuilder(); }
			catch (Exception dbex) {
				anErr = 26;
				System.out.println( "### ERROR in creating DocumentBuilder for source document: '" + my_inputFileName + "'");
				dbex.printStackTrace();
			}
			if (docFactory.isValidating()) {
				docBuilder.setErrorHandler(new ValidationErrorHandler(1));
			}
			
			try { xmlSourceDocument = docBuilder.parse(my_source_file); }
			catch (Exception srcex) {
				anErr = 27;
				BatchXSLT.g_mainXSLTFrame.showMess( "\n#### Transformer Parse Exception " + anErr + ": '"+ srcex.getMessage() + "'\n" );
				System.out.println( "### ERROR in DocumentBuilder parsing source document: '" + my_inputFileName + "'");
				srcex.printStackTrace();
			}
			
			BatchXSLT.g_mainXSLTFrame.showMess( "please wait...");



			// Use the Transformer to apply the associated Templates object to an XML document
			// and evtl. write the output (to a file...).
			try {
				if (xmlSourceDocument != null) transformer.transform( new DOMSource(xmlSourceDocument), my_stream_result );
				else transformer.transform( my_stream_source, my_stream_result );
			}
			catch (Exception e) {
				if (transformer != null) transformer.clearParameters();
				anErr = 30;
				BatchXSLT.g_mainXSLTFrame.showMess( "\n#### Transformer Exception " + anErr + ": '"+ e.getMessage() + "'\n" );
				e.printStackTrace();
				break;
			}
			if (transformer != null) {
				transformer.clearParameters();
			}
			break;
		} while (false);

		// clean up
		if (my_outputFileName.equals("*NONE*") == false) {		// create an output file  - clean up
			if ((my_stream_result != null) || (outputStream != null)) {
				try { outputStream.flush(); }
				catch ( java.io.IOException e ) { }
				try { outputStream.close(); }
				catch ( java.io.IOException e ) { }
			}
		}

			
		// check if the transform really was done (evtl. abort by javascript compiler
		if ( my_outputFileName.equals("*NONE*") == false ) {
			File check_file = new File(my_outputFileName);
			if (check_file.exists() == true) {
				if (check_file.length() <= 0) {
					anErr = 32;
					BatchXSLT.g_mainXSLTFrame.showMess( "\n#### Error (" + anErr + ") during transform. Output file is empty. See consol log for details.\n" );
				}
			}
			else {
				anErr = 31;
				BatchXSLT.g_mainXSLTFrame.showMess( "\n#### Error (" + anErr + ") during transform. Output file was not created. See consol log for details.\n" );
			}
		}

		if ((anErr >= 10) && (BatchXSLT.g_mainXSLTFrame.deleteFailedTransformFiles == true)) {	// we had an error opening the result file or during transform: delete invalid result file
			File aFile = new File( my_outputFileName );
			aFile.delete();
		}

		if (anErr == 0) {
			BatchXSLT.g_mainXSLTFrame.showMess( "...successfully finished.\n");
		}
		return(anErr);
	}

	
	
	
	/* ===========================================================
	 * validation error handler
	 */
	private static int _XMLvalidatingMaxErrorMessages = 0;
	private static int _XMLvalidatingMaxFatalErrorMessages = 0;
	private static int _XMLvalidatingMaxWarningMessages = 0;
	static class ValidationErrorHandler implements ErrorHandler {
		private int showmessage = 1;
		
		ValidationErrorHandler(int mess) {
			showmessage = mess;
			
		}
		
		public void fatalError(SAXParseException spe) throws SAXParseException {
			if ( BatchXSLT.g_mainXSLTFrame.XMLvalidatingMaxFatalErrorMessages <= 0) return;
			_XMLvalidatingMaxFatalErrorMessages--;
			if (_XMLvalidatingMaxFatalErrorMessages <= 0) return;
			if (showmessage != 0) {
				String systemId = spe.getSystemId();
				if (systemId == null) {
					systemId = "null";
				}
				String message = "### ERROR FATAL: URI=" + systemId +
					" Line=" + spe.getLineNumber() +
					" Column=" + spe.getColumnNumber() +
					": " + spe.getMessage();
				System.err.println(message);
			}
		}
		
		public void error(SAXParseException spe) throws SAXParseException {
			if ( BatchXSLT.g_mainXSLTFrame.XMLvalidatingMaxErrorMessages <= 0) return;
			_XMLvalidatingMaxErrorMessages--;
			if (_XMLvalidatingMaxErrorMessages <= 0) return;
			if (showmessage != 0) {
				String systemId = spe.getSystemId();
				if (systemId == null) {
					systemId = "null";
				}
				String message = "### ERROR: URI=" + systemId +
					" Line=" + spe.getLineNumber() +
					" Column=" + spe.getColumnNumber() +
					": " + spe.getMessage();
				System.err.println(message);
			}
		}
		
		public void warning(SAXParseException spe) throws SAXParseException {
			if ( BatchXSLT.g_mainXSLTFrame.XMLvalidatingMaxWarningMessages <= 0) return;
			_XMLvalidatingMaxWarningMessages--;
			if (_XMLvalidatingMaxWarningMessages <= 0) return;
			if (showmessage != 0) {
				String systemId = spe.getSystemId();
				if (systemId == null) {
					systemId = "null";
				}
				String message = "### WARNIING: URI=" + systemId +
					" Line=" + spe.getLineNumber() +
					" Column=" + spe.getColumnNumber() +
					": " + spe.getMessage();
				System.err.println(message);
			}
		}
	}
	
	/* used as help only
	public class DefaultValidationErrorHandler extends DefaultHandler {
		static private int ERROR_COUNT_LIMIT = 10;
		private int errorCount = 0;
		
		// **** Fix message i18n
		public void error(SAXParseException e) throws SAXException {
			if (errorCount >= ERROR_COUNT_LIMIT) {
				// Ignore all errors after reaching the limit
				return;
			} else if (errorCount == 0) {
				// Print a warning before the first error
				System.err.println("Warning: validation was turned on but an org.xml.sax.ErrorHandler was not");
				System.err.println("set, which is probably not what is desired.  Parser will use a default");
				System.err.println("ErrorHandler to print the first " +
								   ERROR_COUNT_LIMIT +               " errors.  Please call");
				System.err.println("the 'setErrorHandler' method to fix this.");
			}
			
			String systemId = e.getSystemId();
			if (systemId == null) {
				systemId = "null";
			}
			String message = "Error: URI=" + systemId +
            " Line=" + e.getLineNumber() +
            ": " + e.getMessage();
			System.err.println(message);
			errorCount++;
		}
	}
	*/

	/* ===========================================================
	 * decrypt a stream
	 */
	private static CipherInputStream dcs(InputStream fis)
	{

		try {
			CipherInputStream cis;

			int	i = 0;
			// Creation of Secret key
			byte k1[] = "065rFD363W5fv3zFvhe43Rrz3en7nz5v74vz4saddsvy9u438n".getBytes();
			byte k2[] = "hgzd6gjMN466w667MN89zo8obNM,jkjFNB,kfk,kFjtiuj6ikj".getBytes();
			byte k3[] = "456Ffg789vauzd%&mhm.kvghvl()=kmhgvvhmcghy*w%&/dvvx".getBytes();
			byte k4[] = "y4cyd4gtdkFyhifu4uz5v4h8947cqh78cxhmh8mcNM56c4tc7M".getBytes();
			byte k0[] = new byte[200];
			int j = 0;
			for (i = k1.length-1; i >= 0; i--) { k0[j] = k1[i]; j++; }
			for (i = k2.length-1; i >= 0; i--) { k0[j] = k2[i]; j++; }
			for (i = k3.length-1; i >= 0; i--) { k0[j] = k3[i]; j++; }
			for (i = k4.length-1; i >= 0; i--) { k0[j] = k4[i]; j++; }
			if (BatchXSLT.g_mainXSLTFrame.applFunction.length() > 9) {
				byte ko[] = BatchXSLT.g_mainXSLTFrame.applFunction.getBytes();
				k0[97] = ko[3];
				k0[101] = ko[8];
			}
			//System.out.println(BatchXSLT.g_mainXSLTFrame.applFunction + ": " + (new String(k0)));

			SecretKeySpec secretKey = new SecretKeySpec(k0,96,8,"DES");

			// Creation of Cipher objects
			Cipher decrypt = Cipher.getInstance("DES/ECB/PKCS5Padding");
			decrypt.init(Cipher.DECRYPT_MODE, secretKey); 

			// decrypt stream
			cis = new CipherInputStream(fis, decrypt); 

			return(cis);
		}	catch(Exception e){
		}
		return(null);
	}

	public static int writeCommPrefsFile(String pathname, boolean overwrite) {
		File f = new File(pathname);
		String thepath = f.getParent();
		if (thepath != null) {	// create the path if not exist
			File apath = new File(thepath);
			boolean retflag = apath.mkdirs();
		}
		
		if (overwrite == false) {
			if (f.exists()) return (0);
		}
		try {
			FileWriter fwr = new FileWriter( f, false );
			fwr.write("BatchXSLTMajorVersion="+ mainXSLTFrame.applMajorVersion + "\r\n");
			fwr.write("BatchXSLTMinorVersion="+ mainXSLTFrame.applMinorVersion + "\r\n");
			fwr.write("BatchXSLTRequiredXslVersionName="+ mainXSLTFrame.xslVersionName + "\r\n");
			String apath = mainXSLTFrame.appDir;
			if (apath.endsWith(File.separator) == false) apath = apath + File.separator;
			fwr.write("BatchXSLTAppDir="+ apath + "\r\n");
			apath = mainXSLTFrame.jtoverrideQueuePath;
			if (apath.endsWith(File.separator) == false)  apath = apath + File.separator;
			fwr.write("BatchXSLTCommDir="+ apath + "\r\n");
			fwr.flush();
			fwr.close();
		} catch (IOException e) {
			return (-1);
		}
		return (0);
	}
	
	/* ===========================================================
	 * read in a parameter file (jobticket)
	 */
	public static int readJobTicketFile(int func)
	{
		numberof_readJobTicketFile_calls++;	// remeber how may jobtickets were re- loaded
		int	tries = 0;
		String loaded_where = "";
		String ts;
		String tv;
		int init_previous_jt = 0;	// set to init the previously loaded jobticket
		
		//PropertyResourceBundle jt_file = null;
		JobTicket jt_file = null;
//BatchXSLT.g_mainXSLTFrame.showMess( "reading jobTicketFileName: " + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "\n" );
 		if (com.epaperarchives.batchxslt.BatchXSLT.g_mainXSLTFrame.general_abort == true) return(99);

		if (BatchXSLT.g_mainXSLTFrame.jobTicketFileName.equals("")) return(-1);

		// try to load a jobticket file
		String jobticket_filepath = "";
		if (BatchXSLT.g_mainXSLTFrame.jobTicketFileName.indexOf(File.separator) >= 0) jobticket_filepath = BatchXSLT.g_mainXSLTFrame.jobTicketFileName;
		else jobticket_filepath = BatchXSLT.g_mainXSLTFrame.nextJobTicketPath + BatchXSLT.g_mainXSLTFrame.jobTicketFileName;

//BatchXSLT.g_mainXSLTFrame.showMess( "reading jobticket_filepath: " + jobticket_filepath + "\n" );
		jobticket_filepath = utils.file_absolutePath(jobticket_filepath);
//BatchXSLT.g_mainXSLTFrame.showMess( "reading jobticket_filepath: " + jobticket_filepath + "\n" );

//BatchXSLT.g_mainXSLTFrame.showMess( "reading jobticket_filepath: " + jobticket_filepath + "\n" );
//System.out.println( "reading jt: " + jobticket_filepath);
		tries = 0;
		while (true) {
			tries++;
			if (tries > 4 ) {
				if (BatchXSLT.g_mainXSLTFrame.start_over != 1) {	// error message only if not the first start
					BatchXSLT.g_mainXSLTFrame.showMess( "#### JobTicket file '" + BatchXSLT.g_mainXSLTFrame.nextJobTicketPath + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "' not found.\n" );
				}
				BatchXSLT.g_mainXSLTFrame.showRunMess("");
				return(1);
			}
			//BatchXSLT.g_mainXSLTFrame.showMess( "*** Loading JobTicket file '" + jobticket_filepath + "'\n" );

			// Priority 1: try to load a jobticket file from the given path
			try {
				BatchXSLT.g_mainXSLTFrame.jobTicketFile = new FileInputStream( jobticket_filepath );
				loaded_where = "";
//BatchXSLT.g_mainXSLTFrame.showMess( "from FILE jobticket_filepath: " + jobticket_filepath + "\n" );
				break;
			}
			catch (FileNotFoundException e) {
//BatchXSLT.g_mainXSLTFrame.showMess( "from FILE jobticket_filepath: NOT FOUND\n" );
			}

			// Priority 2: try to load from user's home directory in path BatchXSLT/jt/name_of_jobticket.jt
			try {
				BatchXSLT.g_mainXSLTFrame.jobTicketFile = new FileInputStream( BatchXSLT.g_mainXSLTFrame.userHome + File.separator + BatchXSLT.g_mainXSLTFrame.applFullName + File.separator + "jt" + File.separator + BatchXSLT.g_mainXSLTFrame.jobTicketFileName );
				loaded_where = " from user's home";
				break;
			}
			catch (FileNotFoundException f) { }	// try next possible way....

			//  Priority 3: local file is not available: load from resource (signed jar file) when running under WebStart: jt/autostart.jt
			// Get current classloader
			InputStream jt_inputstream = BatchXSLTransform.class.getClassLoader().getResourceAsStream("jt/" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName);
			if (jt_inputstream != null) {
				//BatchXSLT.g_mainXSLTFrame.showMess( "Loading packaged JobTicket\n" );
				BatchXSLT.g_mainXSLTFrame.jobTicketFile = jt_inputstream;
				loaded_where = " from WebStart";
				break;
			}

			// let's wait
			try { Thread.sleep(100); }
			catch (InterruptedException x) {}

			// let's loop to try again
		}

		// Attempt to open parameter file as resource bundle.
		tries = 0;
		while (true) {
			tries++;
			//try { jt_file = new PropertyResourceBundle( BatchXSLT.g_mainXSLTFrame.jobTicketFile ); break; }
//BatchXSLT.g_mainXSLTFrame.showMess( "open JobTicket\n" );
			jt_file = new JobTicket();
//BatchXSLT.g_mainXSLTFrame.showMess( "load JobTicket\n" );
			try { jt_file.load( BatchXSLT.g_mainXSLTFrame.jobTicketFile ); break; }
			catch (IOException e) {
				if (tries > 2 ) {
					BatchXSLT.g_mainXSLTFrame.showMess( "#### Can not read JobTicket definitions from file '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "'.\n" );
					try { BatchXSLT.g_mainXSLTFrame.jobTicketFile.close(); }
					catch ( IOException e1 ) {
					}
					BatchXSLT.g_mainXSLTFrame.showRunMess("");
					return(2);
				}
				// let's wait
				try { Thread.sleep(100); }
				catch (InterruptedException x) {}
			}
			// let's try again
		}
//BatchXSLT.g_mainXSLTFrame.showMess( "loaded JobTicket\n" );

		// jobTicket file is ready to be read
		//-----------------------------------

		// this is used from an override.jt to not to inizialize current settings and controls
		try { init_previous_jt = Integer.valueOf(jt_file.getString( "init_previous_jobticket" )); }
		catch (MissingResourceException e) {}
		
		// ======= check if we should start ThreadViewer!
		try { BatchXSLT.g_mainXSLTFrame.showThreadViewer = Integer.valueOf(jt_file.getString( "showThreadViewer" ));
			if (BatchXSLT.g_mainXSLTFrame.showThreadViewer > 0) {
				System.out.println("showThreadViewer: " + BatchXSLT.g_mainXSLTFrame.showThreadViewer + "   isThreadViewer_started: " + isThreadViewer_started);
				if ((isThreadViewer_started == false) && (BatchXSLT.g_mainXSLTFrame.showThreadViewer > 0)) {
					System.out.println("Starting ThreadViewer");
					isThreadViewer_started = true;
					com.epaperarchives.batchxslt.ThreadViewer.main(null);
					System.out.println("ThreadViewer started");
				}
			}
		}
		catch (MissingResourceException e) {}


		// ======= check if we should run this JobTicket!
		// define a variable to store the info if we should start this jobticket
		int run_this_jt = 1;	// init to 'run it'
		// check if we must run this jt or if it must be enabled by a trigger file
		if ((run_this_jt == 1) && (func == 1)) {	// if we were called to start a transform
			String myjt_triggerfile = "";
			try { myjt_triggerfile = jt_file.getString( "jt_triggerfile" ); }
			catch (MissingResourceException e) { }
			if (myjt_triggerfile.equals("") == false) {	// we have a trigger file name
				do {
					String mysourcePathRoot = "";
					try {
						if (isWindows() == true) mysourcePathRoot = jt_file.getString( "sourcePathRoot" ).replace('/',File.separatorChar);
						else mysourcePathRoot = jt_file.getString( "sourcePathRoot" );
						if ( mysourcePathRoot.equals("") == false) {
							if ( mysourcePathRoot.startsWith("~") == true) {
								String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
								mysourcePathRoot = home + mysourcePathRoot.substring(2);	// cut leading path ~/
							}
							if (mysourcePathRoot.endsWith(File.separator) == false) mysourcePathRoot = mysourcePathRoot + File.separator;
						}
					}
					catch (MissingResourceException e) { }
					String mysourcePathName = "";
					try {
						if (isWindows() == true) mysourcePathName = jt_file.getString( "sourcePathName" ).replace('/',File.separatorChar);
						else mysourcePathName = jt_file.getString( "sourcePathName" );
						if ( mysourcePathName.startsWith("~") == true) {
							String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
							mysourcePathName = home + mysourcePathName.substring(2);	// cut leading path ~/
						}
					}
					catch (MissingResourceException e) {
						break;
					}
					if (mysourcePathRoot.equals("") == false) mysourcePathName = mysourcePathRoot + mysourcePathName;
					File triggerfile = new File( mysourcePathName + myjt_triggerfile );
					if ( triggerfile.exists() == false ) run_this_jt = 0;	// don't run it
				} while (false);
			}
		}

		// check if we must run this jt or if it is blocked by a schedule
		if ((run_this_jt == 1) && (func == 1)) {	// if we were called to start a transform
			String myjt_schedule = "";
			try { myjt_schedule = jt_file.getString( "jt_schedule" ); }
			catch (MissingResourceException e) { }
			if (myjt_schedule.equals("") == false) {	// we have a scheduler string
				Scheduler scheduler = new Scheduler(myjt_schedule);
				run_this_jt = scheduler.should_run();
			}
		}

		// ======= check if we should run this JobTicket!
		// if not, prepare some stuff of next JT
		if (run_this_jt <= 0) {
			// get jt mode
			int mymode = -1;
			try { mymode = Integer.valueOf(jt_file.getString( "mode" )); }
			catch (MissingResourceException e) {
				BatchXSLT.g_mainXSLTFrame.showMess( "#### Missing keyword in JobTicket file '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "': 'mode'\n" );
				try { BatchXSLT.g_mainXSLTFrame.jobTicketFile.close(); }
				catch ( IOException e1 ) {}
				BatchXSLT.g_mainXSLTFrame.showRunMess("");
				BatchXSLT.g_mainXSLTFrame.jobticket_is_loading = false;
				return(3);
			}
			if ((mymode < 0) || (mymode > BatchXSLT.g_mainXSLTFrame.maxModeChoices)) {
				BatchXSLT.g_mainXSLTFrame.showMess( "#### Invalid value in JobTicket file '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "': 'mode'\n" );
				try { BatchXSLT.g_mainXSLTFrame.jobTicketFile.close(); }
				catch ( IOException e1 ) {}
				BatchXSLT.g_mainXSLTFrame.showRunMess("");
				BatchXSLT.g_mainXSLTFrame.jobticket_is_loading = false;
				return(3);
			}
			// prepare next jobticket
			try {
				if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.nextJobTicketFileName = jt_file.getString( "nextJobTicketFileName" ).replace('/',File.separatorChar);
				else BatchXSLT.g_mainXSLTFrame.nextJobTicketFileName = jt_file.getString( "nextJobTicketFileName" );
			}
			catch (MissingResourceException e) { }

			// and loop delay
			try { BatchXSLT.g_mainXSLTFrame.loopDelay = Integer.valueOf(jt_file.getString( "loopDelay" )); }
			catch (MissingResourceException e) { }
			
			BatchXSLT.g_mainXSLTFrame.jobticket_loaded = true;	// simulate "everything is OK"

			return(-1);
		}




		// ======= if we are here, we will run this JobTicket!
		// initialize the settings for the new jobTicket
		if (init_previous_jt > 0) BatchXSLT.g_mainXSLTFrame.init_jt();	// do init if not inhibited by the new jobticket
		else BatchXSLT.g_mainXSLTFrame.remove_controls();	// ALWAYS remove them or it will overflow
		BatchXSLT.g_mainXSLTFrame.jobticket_is_loading = true;
		/* ----------------------------------------------------
		 * Get the jt_file and other commands. If any of the resources is missing, presume
		 * that the whole file is bad and require all file names to be typed at console.
		 */

                		
		
		// logfile stuff
		try { BatchXSLT.g_mainXSLTFrame.logfile_write = Integer.valueOf(jt_file.getString( "logfileWrite" )); }
		catch (MissingResourceException e) { }
		try { // we do not allow to have empty logfile path
			ts = jt_file.getString( "logfilePath" );
			if (ts.equals("")) {
				ts = System.getProperty("user.home"); 
			}
                        if ( ts.startsWith("~") == true) {
                                String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
                                ts = home + ts.substring(2);	// cut leading path ~/
                        }
			if (ts.endsWith(File.separator) == false) ts = ts + File.separator;
			if (isWindows() == true) ts = ts.replace('/',File.separatorChar);
			BatchXSLT.g_mainXSLTFrame.logfile_path = ts;
		}
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.logfile_name = jt_file.getString( "logfileName" ); }
		catch (MissingResourceException e) { }
		try {
			BatchXSLT.g_mainXSLTFrame.logfile_maxsize = Integer.valueOf(jt_file.getString( "logfileMaxSize" ));
			if ( BatchXSLT.g_mainXSLTFrame.logfile_maxsize <  BatchXSLT.g_mainXSLTFrame.logfile_minsize) BatchXSLT.g_mainXSLTFrame.logfile_maxsize = BatchXSLT.g_mainXSLTFrame.logfile_minsize;
		}
		catch (MissingResourceException e) {  }
		try {
			BatchXSLT.g_mainXSLTFrame.logfile_max = Integer.valueOf(jt_file.getString( "logfileMax" ));
			if ( BatchXSLT.g_mainXSLTFrame.logfile_max <  BatchXSLT.g_mainXSLTFrame.logfile_min) BatchXSLT.g_mainXSLTFrame.logfile_max = BatchXSLT.g_mainXSLTFrame.logfile_min;
		}
		catch (MissingResourceException e) {  }

                
                
		try { BatchXSLT.g_mainXSLTFrame.applFullName = jt_file.getString( "applFullName" ); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.applLogoName = jt_file.getString( "applLogoName" ); }
		catch (MissingResourceException e) { }

		try { BatchXSLT.g_mainXSLTFrame.mode = Integer.valueOf(jt_file.getString( "mode" )); }
		catch (MissingResourceException e) {
			BatchXSLT.g_mainXSLTFrame.showMess( "#### Missing keyword in JobTicket file '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "': 'mode'\n" );
			try { BatchXSLT.g_mainXSLTFrame.jobTicketFile.close(); }
			catch ( IOException e1 ) {}
			BatchXSLT.g_mainXSLTFrame.showRunMess("");
			BatchXSLT.g_mainXSLTFrame.jobticket_is_loading = false;
			return(3);
		}
		if ((BatchXSLT.g_mainXSLTFrame.mode < 0) || (BatchXSLT.g_mainXSLTFrame.mode > BatchXSLT.g_mainXSLTFrame.maxModeChoices)) {
			BatchXSLT.g_mainXSLTFrame.showMess( "#### Invalid value in JobTicket file '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "': 'mode'\n" );
			BatchXSLT.g_mainXSLTFrame.mode = 0;
			try { BatchXSLT.g_mainXSLTFrame.jobTicketFile.close(); }
			catch ( IOException e1 ) {}
			BatchXSLT.g_mainXSLTFrame.showRunMess("");
			BatchXSLT.g_mainXSLTFrame.jobticket_is_loading = false;
			return(3);
		}

		try { BatchXSLT.g_mainXSLTFrame.clearMessageWindow = Integer.valueOf(jt_file.getString( "clearMessages" )); }
		catch (MissingResourceException e) { }
		if (BatchXSLT.g_mainXSLTFrame.clearMessageWindow > 0) BatchXSLT.g_mainXSLTFrame.clearMess(BatchXSLT.g_mainXSLTFrame.clearMessageWindow - 1);

		try { BatchXSLT.g_mainXSLTFrame.jobticketVersion = jt_file.getString( "jobticketVersion" ); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.applFunction = jt_file.getString( "applicationPurpose" ); }
		catch (MissingResourceException e) { }
		
		try { 
			BatchXSLT.g_mainXSLTFrame.onlineManualUrl = jt_file.getString( "onlineManualUrl" );
			if (BatchXSLT.g_mainXSLTFrame.onlineManualUrl.indexOf("%%lang%%") > 0) {	// set language
				BatchXSLT.g_mainXSLTFrame.onlineManualUrl = BatchXSLT.g_mainXSLTFrame.onlineManualUrl.replaceAll("%%lang%%", BatchXSLT.g_mainXSLTFrame.userLang);
				//System.out.println(BatchXSLT.g_mainXSLTFrame.onlineManualUrl);
			}
		}
		catch (MissingResourceException e) { }
		
		
		try { BatchXSLT.g_mainXSLTFrame.jt_triggerfile = jt_file.getString( "jt_triggerfile" ); }
		catch (MissingResourceException e) {
			BatchXSLT.g_mainXSLTFrame.jt_triggerfile = "";	// clear when not stated
		}
		try { BatchXSLT.g_mainXSLTFrame.jt_schedule = jt_file.getString( "jt_schedule" ); }
		catch (MissingResourceException e) { }

		try { 
			int s = Integer.valueOf(jt_file.getString( "deleteFailedTransformFiles" ));
			if (s > 0) BatchXSLT.g_mainXSLTFrame.deleteFailedTransformFiles = true;
			else BatchXSLT.g_mainXSLTFrame.deleteFailedTransformFiles = false;
		}
		catch (MissingResourceException e) { }

		// starting internal java method before/after job or transform?
		try { BatchXSLT.g_mainXSLTFrame.runOnLoadedJobTicketMethod = jt_file.getString( "runOnLoadedJobTicketMethod" ); }
		catch (MissingResourceException e) { BatchXSLT.g_mainXSLTFrame.runOnLoadedJobTicketMethod = ""; }

		try { BatchXSLT.g_mainXSLTFrame.runBeforeJobMethod = jt_file.getString( "runBeforeJobMethod" ); }
		catch (MissingResourceException e) { BatchXSLT.g_mainXSLTFrame.runBeforeJobMethod = ""; }
		try { BatchXSLT.g_mainXSLTFrame.runAfterJobMethod = jt_file.getString( "runAfterJobMethod" ); }
		catch (MissingResourceException e) { BatchXSLT.g_mainXSLTFrame.runAfterJobMethod = ""; }
		// starting external applications before/after job or transform?
		try { BatchXSLT.g_mainXSLTFrame.runBeforeJobApp = jt_file.getString( "runBeforeJobApp" ); }
		catch (MissingResourceException e) { BatchXSLT.g_mainXSLTFrame.runBeforeJobApp = ""; }
		try { BatchXSLT.g_mainXSLTFrame.runAfterJobApp = jt_file.getString( "runAfterJobApp" ); }
		catch (MissingResourceException e) { BatchXSLT.g_mainXSLTFrame.runAfterJobApp = ""; }
		try { BatchXSLT.g_mainXSLTFrame.runBeforeTransformApp = jt_file.getString( "runBeforeTransformApp" ); }
		catch (MissingResourceException e) { BatchXSLT.g_mainXSLTFrame.runBeforeTransformApp = ""; }
		try { BatchXSLT.g_mainXSLTFrame.runAfterTransformApp = jt_file.getString( "runAfterTransformApp" ); }
		catch (MissingResourceException e) { BatchXSLT.g_mainXSLTFrame.runAfterTransformApp = ""; }


		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.sourcePathRoot = jt_file.getString( "sourcePathRoot" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.sourcePathRoot = jt_file.getString( "sourcePathRoot" );
			if ( BatchXSLT.g_mainXSLTFrame.sourcePathRoot.equals("") == false) {
				if ( BatchXSLT.g_mainXSLTFrame.sourcePathRoot.startsWith("~") == true) {
					String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
					BatchXSLT.g_mainXSLTFrame.sourcePathRoot = home + BatchXSLT.g_mainXSLTFrame.sourcePathRoot.substring(2);	// cut leading path ~/
				}
				if (BatchXSLT.g_mainXSLTFrame.sourcePathRoot.endsWith(File.separator) == false) BatchXSLT.g_mainXSLTFrame.sourcePathRoot = BatchXSLT.g_mainXSLTFrame.sourcePathRoot + File.separator;
				BatchXSLT.g_mainXSLTFrame.showMess("Source Path Root: '" + BatchXSLT.g_mainXSLTFrame.sourcePathRoot + "'\n");
			}
		}
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.sourcePathName = jt_file.getString( "sourcePathName" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.sourcePathName = jt_file.getString( "sourcePathName" );
			if ( BatchXSLT.g_mainXSLTFrame.sourcePathName.startsWith("~") == true) {
				//BatchXSLT.g_mainXSLTFrame.sourcePathName = BatchXSLT.g_mainXSLTFrame.sourcePathName.replaceFirst("~",System.getProperty("user.home") + File.separatorChar);
				String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
				BatchXSLT.g_mainXSLTFrame.sourcePathName = home + BatchXSLT.g_mainXSLTFrame.sourcePathName.substring(2);	// cut leading path ~/
			}
		}
		catch (MissingResourceException e) {
			BatchXSLT.g_mainXSLTFrame.showMess( "#### Missing keyword in JobTicket file '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "': 'sourcePathName'\n" );
			try { BatchXSLT.g_mainXSLTFrame.jobTicketFile.close(); }
			catch ( IOException e1 ) {}
			BatchXSLT.g_mainXSLTFrame.showRunMess("");
			BatchXSLT.g_mainXSLTFrame.jobticket_is_loading = false;
			return(3);
		}

		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.xslPathName = jt_file.getString( "xslPathName" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.xslPathName = jt_file.getString( "xslPathName" );
		}
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.xslVersionName = jt_file.getString( "xslVersionName" ); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.xslVersionDate = jt_file.getString( "xslVersionDate" ); }
		catch (MissingResourceException e) { }

		// IDML package related vars 
		try { BatchXSLT.g_mainXSLTFrame.IDMLprocessingMessages = Integer.valueOf(jt_file.getString( "IDMLprocessingMessages" )); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.IDMLpreserveExpandedPackage = Integer.valueOf(jt_file.getString( "IDMLpreserveExpandedPackage" )); }
		catch (MissingResourceException e) { }

		try {
			ts = jt_file.getString( "externalProcessTimeout" );
			if (ts.equals("") == false) {
				BatchXSLT.g_mainXSLTFrame.externalProcessTimeout = Long.parseLong(ts, 10);
			}
		 }
		catch (MissingResourceException e) { }

		// ---------------------------------------
		// add custom controls to main dialog pane
		String dialogControl_base = "dialogControl_";
		for (int i = 1; i <= 99; i++) {
			String ps = dialogControl_base + i;
			try {
				String properties = jt_file.getString( ps );
				int retval = BatchXSLT.g_mainXSLTFrame.make_dialogControl(properties);
			}
			catch (MissingResourceException e) { }
		}
		// do this AFTER all custom control are defined for we can set the values in the control
		// get the transformer parameters - this reads property strings in the form
		// 'tp_X=param_name=value' CR
		String param_base_name = "tp_";
		for (int i = 1; i <= 249; i++) {
			String pbn = param_base_name + i;
			try {
				String ps = jt_file.getString( pbn );	//  a parameter string like: 'tp_33 = XSLCSSPath=../XSLCSS/'
				if (ps.startsWith("#") == false) {	// like '# this is comment'
					int retval = BatchXSLT.g_mainXSLTFrame.update_dialogControl(ps);
				}
				String[] param_keys = ps.split("=");		// results in 'tp_33 '  (with ending blank for [0])
				// check if we already have this parameter entry
				String[] tp_arr = BatchXSLT.g_mainXSLTFrame.xslParams.split("\n");
				// find the parameter
				int idx = 0;
				boolean found = false;
				for (idx = 0; idx < tp_arr.length; idx++) {
					if (tp_arr[idx].indexOf(param_keys[0]+"=") >= 0) {
						//BatchXSLT.g_mainXSLTFrame.showMess( "---- idx=" + idx + ", line: " + tp_arr[idx] + "\n");
						tp_arr[idx] = ps;
						found = true;
						break;
					}
				}
				if (found) {
					BatchXSLT.g_mainXSLTFrame.xslParams = "";
					for (int k = 0; k < tp_arr.length; k++) BatchXSLT.g_mainXSLTFrame.xslParams += tp_arr[k]+ "\n";
				}
				else BatchXSLT.g_mainXSLTFrame.xslParams += ps + '\n';
			}
			catch (MissingResourceException e) { /*break;*/ }	// check all 99 params
		}


		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.outputPathRoot = jt_file.getString( "outputPathRoot" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.outputPathRoot = jt_file.getString( "outputPathRoot" );
			if ( BatchXSLT.g_mainXSLTFrame.outputPathRoot.equals("") == false) {
				if ( BatchXSLT.g_mainXSLTFrame.outputPathRoot.startsWith("~") == true) {
					String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
					BatchXSLT.g_mainXSLTFrame.outputPathRoot = home + BatchXSLT.g_mainXSLTFrame.outputPathRoot.substring(2);	// cut leading path ~/
				}
				if (BatchXSLT.g_mainXSLTFrame.outputPathRoot.endsWith(File.separator) == false) BatchXSLT.g_mainXSLTFrame.outputPathRoot = BatchXSLT.g_mainXSLTFrame.outputPathRoot + File.separator;
				BatchXSLT.g_mainXSLTFrame.showMess("Output Path Root: '" + BatchXSLT.g_mainXSLTFrame.outputPathRoot + "'\n");
			}
		}
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) {
				ts = jt_file.getString( "outputPathName" );
				if (get_URL_file_type(ts) != 1) BatchXSLT.g_mainXSLTFrame.outputPathName = ts;
				else BatchXSLT.g_mainXSLTFrame.outputPathName = ts.replace('/',File.separatorChar);
			}
			else BatchXSLT.g_mainXSLTFrame.outputPathName = jt_file.getString( "outputPathName" );
			if ( BatchXSLT.g_mainXSLTFrame.outputPathName.startsWith("~") == true) {
				//BatchXSLT.g_mainXSLTFrame.outputPathName = BatchXSLT.g_mainXSLTFrame.outputPathName.replaceFirst("~",System.getProperty("user.home") + File.separatorChar);
				String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
				BatchXSLT.g_mainXSLTFrame.outputPathName = home + BatchXSLT.g_mainXSLTFrame.outputPathName.substring(2);	// cut leading path ~/
			}
		}
		catch (MissingResourceException e) { }

		try { BatchXSLT.g_mainXSLTFrame.includeXSLCSSfolderCopy = Integer.valueOf(jt_file.getString( "includeXSLCSSfolderCopy" )); }
		catch (MissingResourceException e) { }

		try { BatchXSLT.g_mainXSLTFrame.ftpUsername = jt_file.getString( "username" ); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.ftpUsername = jt_file.getString( "ftpUsername" ); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.ftpPassword = jt_file.getString( "password" ); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.ftpPassword = jt_file.getString( "ftpPassword" ); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.ftpPort = jt_file.getString( "port" ); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.ftpPort = jt_file.getString( "ftpPort" ); }
		catch (MissingResourceException e) { }
		try {
			String my_ftpEncoding = jt_file.getString( "ftpEncoding" );
			// and also set the popup item
			BatchXSLT.g_mainXSLTFrame.set_ftpEncodingCBxItemByName(my_ftpEncoding);
		}
		catch (MissingResourceException e) { }
		try { 
		  ts = jt_file.getString( "ftpType" );
		  if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.ftpType = ts;
	  }
		catch (MissingResourceException e) { }
		try { 
		  ts = jt_file.getString( "ftpActivePassive" );
		  if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.ftpActivePassive = ts;
	  }
		catch (MissingResourceException e) { }

		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.newoutputFileNameExt = jt_file.getString( "newoutputFileNameExt" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.newoutputFileNameExt = jt_file.getString( "newoutputFileNameExt" );
		}
		catch (MissingResourceException e) { }

		try {	// 'excludeFileNameExts' is a historical variable
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.excludeCleanupRunFileNameExts = jt_file.getString( "excludeFileNameExts" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.excludeCleanupRunFileNameExts = jt_file.getString( "excludeFileNameExts" );
		}
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.excludeCleanupRunFileNameExts = jt_file.getString( "excludeCleanupRunFileNameExts" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.excludeCleanupRunFileNameExts = jt_file.getString( "excludeCleanupRunFileNameExts" );
		}
		catch (MissingResourceException e) { }
		try {	// HYSTORICAL: keyword is too long! SEE NEXT
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNameExts = jt_file.getString( "excludeSourceProcessingRunFileNameExts" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNameExts = jt_file.getString( "excludeSourceProcessingRunFileNameExts" );
		}
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNameExts = jt_file.getString( "excludeSourceFileNameExts" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNameExts = jt_file.getString( "excludeSourceFileNameExts" );
		}
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNames = jt_file.getString( "excludeSourceFileNames" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNames = jt_file.getString( "excludeSourceFileNames" );
		}
		catch (MissingResourceException e) { }


		try { BatchXSLT.g_mainXSLTFrame.deleteSourceDirs = Integer.valueOf(jt_file.getString( "deleteSourceDirs" )); }
		catch (MissingResourceException e) { }

		try {
			if (isWindows() == true) {
				ts = jt_file.getString( "sourceFileAction" );
				if (get_URL_file_type(ts) != 1) BatchXSLT.g_mainXSLTFrame.sourceFileAction = ts;
				else BatchXSLT.g_mainXSLTFrame.sourceFileAction = ts.replace('/',File.separatorChar);
			}
			else BatchXSLT.g_mainXSLTFrame.sourceFileAction = jt_file.getString( "sourceFileAction" );
			if ( BatchXSLT.g_mainXSLTFrame.sourceFileAction.startsWith("~") == true) {
				//BatchXSLT.g_mainXSLTFrame.sourceFileAction = BatchXSLT.g_mainXSLTFrame.sourceFileAction.replaceFirst("~",System.getProperty("user.home") + File.separatorChar);
				String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
				BatchXSLT.g_mainXSLTFrame.sourceFileAction = home + BatchXSLT.g_mainXSLTFrame.sourceFileAction.substring(2);	// cut leading path ~/
			}
		}
		catch (MissingResourceException e) { }

		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.folderIndexFileName = jt_file.getString( "folderIndexName" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.folderIndexFileName = jt_file.getString( "folderIndexName" );
		}
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.folderIndexFileXSLName = jt_file.getString( "folderIndexXSL" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.folderIndexFileXSLName = jt_file.getString( "folderIndexXSL" );
		}
		catch (MissingResourceException e) { }

		try { BatchXSLT.g_mainXSLTFrame.loopDelay = Integer.valueOf(jt_file.getString( "loopDelay" )); }
		catch (MissingResourceException e) { }
		
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.mainJobTicketPath = jt_file.getString( "mainJobTicketPath" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.mainJobTicketPath = jt_file.getString( "mainJobTicketPath" );
			if ( BatchXSLT.g_mainXSLTFrame.mainJobTicketPath.startsWith("~") == true) {
				String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
				BatchXSLT.g_mainXSLTFrame.mainJobTicketPath = home + BatchXSLT.g_mainXSLTFrame.mainJobTicketPath.substring(2);	// cut leading path ~/
			}
			if ((BatchXSLT.g_mainXSLTFrame.mainJobTicketPath.equals("") == false) && (BatchXSLT.g_mainXSLTFrame.mainJobTicketPath.endsWith(File.separator) == false)) BatchXSLT.g_mainXSLTFrame.mainJobTicketPath = BatchXSLT.g_mainXSLTFrame.mainJobTicketPath + File.separator;
			if (BatchXSLT.g_mainXSLTFrame.mainJobTicketPath.equals("") == false) {
				// create path if not exists
				File apath = new File(BatchXSLT.g_mainXSLTFrame.mainJobTicketPath);
				boolean retflag = true;
				if (apath.exists() == false) retflag = apath.mkdirs();
				// show message where we expect JobTickets
				if (retflag == true) BatchXSLT.g_mainXSLTFrame.showMess("JobTickets Path: '" + BatchXSLT.g_mainXSLTFrame.mainJobTicketPath + "'\n");
				else BatchXSLT.g_mainXSLTFrame.showMess("## ERROR ## JobTickets Path: '" + BatchXSLT.g_mainXSLTFrame.mainJobTicketPath + "' could not be created!\n");
			}
			if (BatchXSLT.g_mainXSLTFrame.nextJobTicketPath.equals("") == true) BatchXSLT.g_mainXSLTFrame.nextJobTicketPath = BatchXSLT.g_mainXSLTFrame.mainJobTicketPath;
		}
		catch (MissingResourceException e) { }
		
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.nextJobTicketPath = jt_file.getString( "nextJobTicketPath" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.nextJobTicketPath = jt_file.getString( "nextJobTicketPath" );
			if ( BatchXSLT.g_mainXSLTFrame.nextJobTicketPath.startsWith("~") == true) {
				String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
				BatchXSLT.g_mainXSLTFrame.nextJobTicketPath = home + BatchXSLT.g_mainXSLTFrame.nextJobTicketPath.substring(2);	// cut leading path ~/
			}
			if ((BatchXSLT.g_mainXSLTFrame.nextJobTicketPath.equals("") == false) && (BatchXSLT.g_mainXSLTFrame.nextJobTicketPath.endsWith(File.separator) == false)) BatchXSLT.g_mainXSLTFrame.nextJobTicketPath = BatchXSLT.g_mainXSLTFrame.nextJobTicketPath + File.separator;
			if (BatchXSLT.g_mainXSLTFrame.nextJobTicketPath.equals("") == false) {
				// create path if not exists
				File apath = new File(BatchXSLT.g_mainXSLTFrame.nextJobTicketPath);
				boolean retflag = true;
				if (apath.exists() == false) retflag = apath.mkdirs();
				// show message where we expect JobTickets
				if (retflag == true) BatchXSLT.g_mainXSLTFrame.showMess("Next JobTickets Path: '" + BatchXSLT.g_mainXSLTFrame.nextJobTicketPath + "'\n");
				else BatchXSLT.g_mainXSLTFrame.showMess("## ERROR ## JobTickets Path: '" + BatchXSLT.g_mainXSLTFrame.nextJobTicketPath + "' could not be created!\n");
			}
		}
		catch (MissingResourceException e) { }
		
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.nextJobTicketFileName = jt_file.getString( "nextJobTicketFileName" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.nextJobTicketFileName = jt_file.getString( "nextJobTicketFileName" );
		}
		catch (MissingResourceException e) { }

		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.jtoverrideName = jt_file.getString( "JobTicketOverrideName" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.jtoverrideName = jt_file.getString( "JobTicketOverrideName" );
		}
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.jtoverrideSleepTime = Integer.valueOf(jt_file.getString( "JobTicketSleepTime" )); }
		catch (MissingResourceException e) {}
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath = jt_file.getString( "jobticketsPackagePath" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath = jt_file.getString( "jobticketsPackagePath" );
			if (BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath.equals("") == false) {
				if (BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath.endsWith(File.separator) == false) BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath += File.separator;
				if (BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath.startsWith("~") == true) {
					String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
					BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath = home + BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath.substring(2);	// cut leading path ~/
				}
				BatchXSLT.g_mainXSLTFrame.showMess("JobTickets Package Path: '" + BatchXSLT.g_mainXSLTFrame.jobticketsPackagePath + "'\n");
			}
		}
		catch (MissingResourceException e) {}
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath = jt_file.getString( "jobticketsWorkingPath" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath = jt_file.getString( "jobticketsWorkingPath" );
			if (BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath.equals("") == false) {
				if (BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath.endsWith(File.separator) == false) BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath += File.separator;
				if (BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath.startsWith("~") == true) {
					String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
					BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath = home + BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath.substring(2);	// cut leading path ~/
				}
				BatchXSLT.g_mainXSLTFrame.showMess("JobTickets Working Path: '" + BatchXSLT.g_mainXSLTFrame.jobticketsWorkingPath + "'\n");
			}
		}
		catch (MissingResourceException e) {}
		try {
			if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath = jt_file.getString( "JobTicketOverrideQueuePath" ).replace('/',File.separatorChar);
			else BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath = jt_file.getString( "JobTicketOverrideQueuePath" );
			if (BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath.equals("") == false) {
				if (BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath.endsWith(File.separator) == false) BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath += File.separator;
				if (BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath.startsWith("~") == true) {
					String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
					BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath = home + BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath.substring(2);	// cut leading path ~/
				}
				BatchXSLT.g_mainXSLTFrame.showMess("JobTickets Queue Path: '" + BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath + "'\n");
			}
		}
		catch (MissingResourceException e) { }
		try {	// MUST be after reading JobTicketOverrideQueuePath
			BatchXSLT.g_mainXSLTFrame.writeCommPrefs = Integer.valueOf(jt_file.getString( "writeCommPrefs" ));
			if (BatchXSLT.g_mainXSLTFrame.writeCommPrefs == 1) {	// write the communication preferences file into folder 'jtoverrideQueuePath'
				int retval = writeCommPrefsFile(BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath + BatchXSLT.g_mainXSLTFrame.commPrefsSubPath + BatchXSLT.g_mainXSLTFrame.commPrefsName, true);
			}
		}
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) mainXSLTFrame.jtoverrideQueueName = jt_file.getString( "JobTicketOverrideQueueName" ).replace('/',File.separatorChar);
			else mainXSLTFrame.jtoverrideQueueName = jt_file.getString( "JobTicketOverrideQueueName" );
		}
		catch (MissingResourceException e) { }
		try { mainXSLTFrame.jtoverrideSleepTime = Integer.valueOf(jt_file.getString( "JobTicketSleepTime" )); }
		catch (MissingResourceException e) {}
		try {
			mainXSLTFrame.exitFrom_jtoverrideQueue_exitcode = Integer.valueOf(jt_file.getString( "exit_on_jtoverrideQueue_done" ));
			mainXSLTFrame.exitFrom_jtoverrideQueue = true;
		}
		catch (MissingResourceException e) {}
		
		
		// XML transform handling stuff
		try { BatchXSLT.g_mainXSLTFrame.continueXMLparseOnFatalError = Integer.valueOf(jt_file.getString( "continueXMLparseOnFatalError" )); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.doXMLvalidating = Integer.valueOf(jt_file.getString( "doXMLvalidating" )); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.XMLvalidatingMaxErrorMessages = Integer.valueOf(jt_file.getString( "XMLvalidatingMaxErrorMessages" )); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.XMLvalidatingMaxFatalErrorMessages = Integer.valueOf(jt_file.getString( "XMLvalidatingMaxFatalErrorMessages" )); }
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.XMLvalidatingMaxWarningMessages = Integer.valueOf(jt_file.getString( "XMLvalidatingMaxWarningMessages" )); }
		catch (MissingResourceException e) { }


		/***************************************************************/
		/**
		 * Imaging stuff
		 */
		// with Windows, Unix, OS X only
		if ( !isMacintosh() ) {
			try {
				ts = jt_file.getString( "eps2xml_associate_XSL" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.eps2xml_associate_XSL = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.eps2xml_associate_XSL = ts;
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "checkExtConverter" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.check_for_external_converters = 0;
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.check_for_external_converters = Integer.valueOf(ts);
			}
			catch (MissingResourceException e) { }

			// ================================
			// GhostScript stuff
			try {
				ts = jt_file.getString( "gs_convert" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_convert = 0;
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.gs_convert = Integer.valueOf(ts);
			}
			catch (MissingResourceException e) { }

			//if (init_previous_jt > 0) BatchXSLT.g_mainXSLTFrame.gs_envir = "";
			try {
				ts = jt_file.getString( "gs_envir" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_envir = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.gs_envir = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.gs_envir = ts;
				}
			}
			catch (MissingResourceException e) { }

			// get executable name of Ghostscript
			try {
				ts = jt_file.getString( "gs_pgm_name" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_pgm_name = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.gs_pgm_name = ts;
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "gs_pgm_path" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_pgm_path = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.gs_pgm_path = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.gs_pgm_path = ts;
				}
			}
			catch (MissingResourceException e) { }
			try {	// get path to a system installed Ghostscript
				ts = jt_file.getString( "gs_pgm_installedpath" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_pgm_installedpath = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.gs_pgm_installedpath = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.gs_pgm_installedpath = ts;
				}
			}
			catch (MissingResourceException e) { }

			try {	// get gs HOME to a system installed Ghostscript
				ts = jt_file.getString( "gs_pgm_installedhome" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_pgm_installedhome = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.gs_pgm_installedhome = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.gs_pgm_installedhome = ts;
				}
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "gs_pgm_path_envir" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_pgm_path_envir = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.gs_pgm_path_envir = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.gs_pgm_path_envir = ts;
				}
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "gs_version_num" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_version_num = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.gs_version_num = ts;
			}
			catch (MissingResourceException e) { }

			try {	// i386 or ....
				ts = jt_file.getString( "gs_arch" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_arch = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.gs_arch = ts;
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "gs_pgm_parms_eps2pdf" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.gs_pgm_parms_eps2pdf = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.gs_pgm_parms_eps2pdf = ts;
			}
			catch (MissingResourceException e) { }

			// ================================
			// pstotext stuff
			try {
				ts = jt_file.getString( "pstotext_envir" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.pstotext_envir = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.pstotext_envir = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.pstotext_envir = ts;
				}
			}
			catch (MissingResourceException e) { }
			try {
				ts = jt_file.getString( "pstotext_pgm_path" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.pstotext_pgm_path = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.pstotext_pgm_path = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.pstotext_pgm_path = ts;
				}
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "pstotext_pgm_parms_pdf2txt" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.pstotext_pgm_parms_pdf2txt = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.pstotext_pgm_parms_pdf2txt = ts;
			}
			catch (MissingResourceException e) { }

			
			// ================================
			// ImageMagick stuff
			try {
				ts = jt_file.getString( "im_debug" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_debug = 0;
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.im_debug = Integer.valueOf(ts);
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "im_gs_sanitycheck" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_gs_sanitycheck = 0;
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.im_gs_sanitycheck = Integer.valueOf(ts);
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "im_envir" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_envir = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.im_envir = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.im_envir = ts;
				}
			}
			catch (MissingResourceException e) { }

			// get executable name of ImageMagick 'convert'
			try {
				ts = jt_file.getString( "im_pgm_CONVERT" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_pgm_CONVERT = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.im_pgm_CONVERT = ts;
			}
			catch (MissingResourceException e) { }

			// get executable name of ImageMagick 'identify'
			try {
				ts = jt_file.getString( "im_pgm_IDENTIFY" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_pgm_IDENTIFY = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.im_pgm_IDENTIFY = ts;
			}
			catch (MissingResourceException e) { }

			try {	// get full path/name to ImageMagick
				ts = jt_file.getString( "im_pgm_path" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_pgm_path = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.im_pgm_path = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.im_pgm_path = ts;
				}
			}
			catch (MissingResourceException e) { }

			try {	// get path to a system installed ImageMagick
				ts = jt_file.getString( "im_pgm_installedpath" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_pgm_installedpath = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.im_pgm_installedpath = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.im_pgm_installedpath = ts;
				}
			}
			catch (MissingResourceException e) { }

			try {	// get MAGICK_HOME to a system installed ImageMagick
				ts = jt_file.getString( "im_pgm_installedhome" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_pgm_installedhome = "";
				else if (ts.equals("") == false) {
					if (isWindows() == true) BatchXSLT.g_mainXSLTFrame.im_pgm_installedhome = ts.replace('/',File.separatorChar);
					else BatchXSLT.g_mainXSLTFrame.im_pgm_installedhome = ts;
				}
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "im_version_num" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_version_num = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.im_version_num = ts;
			}
			catch (MissingResourceException e) { }

			try {
				ts = jt_file.getString( "im_version_num_win" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_version_num_win = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.im_version_num_win = ts;
			}
			catch (MissingResourceException e) { }

			try {	// 16 or 8
				ts = jt_file.getString( "im_quantumdepth" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_quantumdepth = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.im_quantumdepth = jt_file.getString( "im_quantumdepth" );
			}
			catch (MissingResourceException e) { }

			try {	// i386 or ....
				ts = jt_file.getString( "im_arch" );
				if (ts.equals("*NONE*")) BatchXSLT.g_mainXSLTFrame.im_arch = "";
				else if (ts.equals("") == false) BatchXSLT.g_mainXSLTFrame.im_arch = jt_file.getString( "im_arch" );
			}
			catch (MissingResourceException e) { }
		}
		else {	// disable on a classic macintosh
			BatchXSLT.g_mainXSLTFrame.gs_convert = 0;
		}

		
		/***************************************************************/
		/**
		 * DEBUG stuff
		 */
		try {
			int myDEBUG = Integer.valueOf(jt_file.getString( "DEBUG" ));
			if (myDEBUG > 0) BatchXSLT.g_mainXSLTFrame.DEBUG = true;
		}
		catch (MissingResourceException e) { }
		try { BatchXSLT.g_mainXSLTFrame.DEBUGjtThread = Integer.valueOf(jt_file.getString( "DEBUGjtThread" )); }
		catch (MissingResourceException e) { }

		/***************************************************************/
		/**
		 * httpCommander stuff
		 */
		try { httpCommander.set_DEBUG(Integer.valueOf(jt_file.getString( "httpCommander_DEBUG" ))); }
		catch (MissingResourceException e) { }
		try { httpCommander.set_Active(Integer.valueOf(jt_file.getString( "httpCommander_Active" ))); }
		catch (MissingResourceException e) { }
		try { httpCommander.set_httpCommander_Name(jt_file.getString( "httpCommander_Name" )); }
		catch (MissingResourceException e) { }
		try { httpCommander.set_IP(jt_file.getString( "httpCommander_IP" )); }
		catch (MissingResourceException e) { }
		try {
			httpCommander.set_Port(jt_file.getString( "httpCommander_Port" ));
			// we also must rebuild httpCommander_StartMessage
			httpCommander.set_startMessage(httpCommander.get_httpCommander_Name() + " is listening on port " + httpCommander.get_Port() + "\n");
		}
		catch (MissingResourceException e) { }

		try {
			httpCommander.set_use_identification(Integer.valueOf(jt_file.getString( "httpCommander_secure" )));
		} catch (MissingResourceException e) { }
		// get first user
		do {
			try {
				httpCommander.add_user(jt_file.getString( "httpCommander_userpass" ));
			} catch (MissingResourceException e) { break; }
			// get more users
			int uc = 1;
			do {
				try {
					httpCommander.add_user(jt_file.getString( "httpCommander_userpass" + uc ));
				} catch (MissingResourceException e) { break; }
				uc++;
			} while (uc < 100);
		} while(false);
		try {
			ts = jt_file.getString( "httpCommander_logout_timeout" );
			if (ts.equals("") == false) {
				httpCommander.set_httpCommander_logout_timeout(Long.parseLong(ts, 10));
			}
		}
		catch (MissingResourceException e) { }

		try {
			if (isWindows() == true) httpCommander.set_contextPath(jt_file.getString( "httpCommander_ContextPath" ).replace('/',File.separatorChar));
			else httpCommander.set_contextPath(jt_file.getString( "httpCommander_ContextPath" ));
		}
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) httpCommander.set_documentRoot(jt_file.getString( "httpCommander_DocumentRoot" ).replace('/',File.separatorChar));
			else httpCommander.set_documentRoot(jt_file.getString( "httpCommander_DocumentRoot" ));
		}
		catch (MissingResourceException e) { }
		try { httpCommander.set_helloMessage(jt_file.getString( "httpCommander_HelloMessage" )); }
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) httpCommander.set_helloFile(jt_file.getString( "httpCommander_HelloFile" ).replace('/',File.separatorChar));
			else httpCommander.set_helloFile(jt_file.getString( "httpCommander_HelloFile" ));
		}
		catch (MissingResourceException e) { }
		try {httpCommander.set_helloFileType(jt_file.getString( "httpCommander_HelloFileType" )); }
		catch (MissingResourceException e) { }
		try {
			if (isWindows() == true) httpCommander.set_loginFile(jt_file.getString( "httpCommander_LoginFile" ).replace('/',File.separatorChar));
			else httpCommander.set_loginFile(jt_file.getString( "httpCommander_LoginFile" ));
		}
		catch (MissingResourceException e) { }
		try {httpCommander.set_loginFileType(jt_file.getString( "httpCommander_LoginFileType" )); }
		catch (MissingResourceException e) { }



		/***************************************************************/
		// we are done
		try { mainXSLTFrame.jobTicketFile.close(); }
		catch ( IOException e1 ) {}

		// ---------------------------------
		// check if httpCommander should be started
		if (httpCommander.get_Active() > 0) {
			if (!httpCommander.is_started()) httpCommander.start();
		}


		// ---------------------------------
		// check if GhostScript is reachable
		if ( BatchXSLT.g_mainXSLTFrame.gs_convert > 0) {
			// check for GhostScript
			BatchXSLT.g_mainXSLTFrame.gs_version_str = BatchXSLTGhostScript.get_GS_version();
			// show version info only the frst time
			if (numberof_readJobTicketFile_calls == 1) {
				BatchXSLT.g_mainXSLTFrame.gs_version_num = BatchXSLTGhostScript.get_GS_version_num(BatchXSLT.g_mainXSLTFrame.gs_version_str);
				if ( BatchXSLT.g_mainXSLTFrame.gs_version_str.equals("") == true ) {
					BatchXSLT.g_mainXSLTFrame.gs_convert = 0;	// init if GhostScript is not reachable
					BatchXSLT.g_mainXSLTFrame.showMess("#### GhostScript program '" + BatchXSLT.g_mainXSLTFrame.gs_pgm_path + "' is NOT reachable!!\n");
				}
				else {
					BatchXSLT.g_mainXSLTFrame.showMess(BatchXSLT.g_mainXSLTFrame.gs_version_str + "\n");
				}
			}

			// check for pstotext
			if ( (BatchXSLT.g_mainXSLTFrame.gs_convert & 2) > 0) {
				BatchXSLT.g_mainXSLTFrame.pstotext_version_str = BatchXSLTGhostScript.get_pstotext_version();
				// show version info only the frst time
				if (numberof_readJobTicketFile_calls == 1) {
					if ( BatchXSLT.g_mainXSLTFrame.pstotext_version_str.equals("") == true ) {
						BatchXSLT.g_mainXSLTFrame.gs_convert -= 4;	// clear text extraction
						BatchXSLT.g_mainXSLTFrame.showMess("#### pstotext program '" + BatchXSLT.g_mainXSLTFrame.pstotext_pgm_path + "' is NOT reachable!!\n");
					}
					else {
						BatchXSLT.g_mainXSLTFrame.showMess(BatchXSLT.g_mainXSLTFrame.pstotext_version_str + "\n");
					}
				}
			}
		}


		// check for available external converters
		if ( !externalConvertersChecked && (BatchXSLT.g_mainXSLTFrame.check_for_external_converters > 0)) {
			externalConvertersChecked = true;	// do this once only at start up
	
			// check and show version info only the frst time
			// check for GhostScript
			if ( (BatchXSLT.g_mainXSLTFrame.check_for_external_converters & 1) > 0) {
				BatchXSLT.g_mainXSLTFrame.gs_version_str = BatchXSLTGhostScript.get_GS_version(true);
				BatchXSLT.g_mainXSLTFrame.gs_version_num = BatchXSLTGhostScript.get_GS_version_num(BatchXSLT.g_mainXSLTFrame.gs_version_str);
				if ( BatchXSLT.g_mainXSLTFrame.gs_version_str.equals("") == true ) {
					BatchXSLT.g_mainXSLTFrame.showMess("#### GhostScript program '" + BatchXSLT.g_mainXSLTFrame.gs_pgm_path + "' is NOT reachable!!\n");
				}
				else {
					if ((numberof_readJobTicketFile_calls == 1) || (BatchXSLT.g_mainXSLTFrame.im_debug > 0)) {
						BatchXSLT.g_mainXSLTFrame.showMess(BatchXSLT.g_mainXSLTFrame.gs_version_str + "\n");
						if (BatchXSLT.g_mainXSLTFrame.gs_pgm_path.equals("") == false) BatchXSLT.g_mainXSLTFrame.showMess("    PGM: " + BatchXSLT.g_mainXSLTFrame.gs_pgm_path + "\n");
					}
					if ( (BatchXSLT.g_mainXSLTFrame.im_debug > 0) && (BatchXSLT.g_mainXSLTFrame.gs_envir.equals("") == false) ) {
						String[] gs_envir = BatchXSLT.g_mainXSLTFrame.gs_envir.split("\\+\\+\\+");
						for (int i = 0; i < gs_envir.length; i++)  BatchXSLT.g_mainXSLTFrame.showMess("    " + gs_envir[i] + "\n");
					}
				}
			}

			// check for ImageMagick
			if ( (BatchXSLT.g_mainXSLTFrame.check_for_external_converters & 4) > 0) {
				BatchXSLT.g_mainXSLTFrame.im_version_str = BatchXSLTImageMagick.get_IMconvert_version(true);
				BatchXSLT.g_mainXSLTFrame.im_version_num = BatchXSLTImageMagick.get_IMconvert_version_num(BatchXSLT.g_mainXSLTFrame.im_version_str);
				if ( BatchXSLT.g_mainXSLTFrame.im_version_str.equals("") == true ) {
					BatchXSLT.g_mainXSLTFrame.showMess("#### ImageMagick convert program '" + BatchXSLT.g_mainXSLTFrame.im_pgm_path + "' is NOT reachable!!\n");
				}
				else {
					if ((numberof_readJobTicketFile_calls == 1) || (BatchXSLT.g_mainXSLTFrame.im_debug > 0)) {
						BatchXSLT.g_mainXSLTFrame.showMess(BatchXSLT.g_mainXSLTFrame.im_version_str + "\n");
						if (BatchXSLT.g_mainXSLTFrame.im_pgm_path.equals("") == false) BatchXSLT.g_mainXSLTFrame.showMess("    PGM: " + BatchXSLT.g_mainXSLTFrame.im_pgm_path + "\n");
					}
					if ( (BatchXSLT.g_mainXSLTFrame.im_debug > 0) && (BatchXSLT.g_mainXSLTFrame.im_envir.equals("") == false) ) {
						String[] im_envir = BatchXSLT.g_mainXSLTFrame.im_envir.split("\\+\\+\\+");
						for (int i = 0; i < im_envir.length; i++)  BatchXSLT.g_mainXSLTFrame.showMess("    " + im_envir[i] + "\n");
					}
				}
			}

			// check for pstotext
			if ( (BatchXSLT.g_mainXSLTFrame.check_for_external_converters & 2) > 0) {
				BatchXSLT.g_mainXSLTFrame.pstotext_version_str = BatchXSLTGhostScript.get_pstotext_version();
				if ( BatchXSLT.g_mainXSLTFrame.pstotext_version_str.equals("") == true ) {
					BatchXSLT.g_mainXSLTFrame.showMess("#### pstotext program '" + BatchXSLT.g_mainXSLTFrame.pstotext_pgm_path + "' is NOT reachable!!\n");
				}
				else {
					BatchXSLT.g_mainXSLTFrame.showMess(BatchXSLT.g_mainXSLTFrame.pstotext_version_str + "\n");
				}
			}

			
			// Make sanity check for ImageMagick and Ghostscript
			if ( (BatchXSLT.g_mainXSLTFrame.im_gs_sanitycheck > 0) && ((BatchXSLT.g_mainXSLTFrame.im_version_str.equals("") == false) || (BatchXSLT.g_mainXSLTFrame.gs_version_str.equals("") == false)) ) {
				String sanity_IM = "";
				String sanity_GS = "";
				String sanity_IM_GS = "";
				int sanity_result = -1;
				String sanity_app_path = "";
				String sanity_params = "";
				String sanity_envir = "";
				String temp_folder = BatchXSLT.g_mainXSLTFrame.userTempDir;
				temp_folder = temp_folder.replace(" ","~blnk~");
				if (temp_folder.endsWith(File.separator) == false) temp_folder += File.separator;
				String testimage_base_path = "Utilities" + File.separator + "Imaging" + File.separator + "test" + File.separator;

				if ( (BatchXSLT.g_mainXSLTFrame.im_debug > 0)
					&& ( (BatchXSLT.g_mainXSLTFrame.im_version_str.equals("") == false) || (BatchXSLT.g_mainXSLTFrame.gs_version_str.equals("") == false) )
						) BatchXSLT.g_mainXSLTFrame.showMess("--- Performing Imaging Sanity Checks\n");

			//BatchXSLT.g_mainXSLTFrame.showMess("*** Sanity Check ImageMagick version: '" + BatchXSLT.g_mainXSLTFrame.im_version_str + "'\n");

				// sanity check ImageMagick
				if (BatchXSLT.g_mainXSLTFrame.im_version_str.equals("") == false) {
					String testimage_source = testimage_base_path + "sanitytest.tif";
					testimage_source = utils.file_absolutePath(testimage_source);

					Boolean exists = utils.existsFile(testimage_source);
			//BatchXSLT.g_mainXSLTFrame.showMess("*** Sanity test image: '" + testimage_source + "'\nexits: " + exists + "\n");
					if (exists == true) { // call ImageMagick only
						testimage_source = testimage_source.replace(" ","~blnk~");
						String testimage_target = temp_folder + PACKAGE_NAME + ".sanitytest.gif";
						sanity_app_path = BatchXSLT.g_mainXSLTFrame.im_pgm_path;
						sanity_params = "-verbose -flatten " + testimage_source + " " + testimage_target;
						sanity_envir = BatchXSLT.g_mainXSLTFrame.im_envir;
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
							BatchXSLT.g_mainXSLTFrame.showMess("     Sanity ImageMagick PGM: '" + sanity_app_path + "'\n");
							BatchXSLT.g_mainXSLTFrame.showMess("                     params: '" + sanity_params + "'\n");
							BatchXSLT.g_mainXSLTFrame.showMess("                      envir: '" + sanity_envir + "'\n");
						}
						sanity_result = utils.callExternalApp(sanity_app_path, sanity_params, sanity_envir);
						if (sanity_result != 0) sanity_IM = "FAIL";
						else {	// check if target image exists
							exists = utils.existsFile(testimage_target);
							if (exists == true) {
								sanity_IM = "OK";
								if (BatchXSLT.g_mainXSLTFrame.im_gs_sanitycheck < 2) utils.deleteFile(testimage_target);
							}
							else sanity_IM = "fail";
						}
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("     Sanity ImageMagick RESULT: " + sanity_result + "\n");
					}
				}
				
				// sanity check Ghostscript
				if (BatchXSLT.g_mainXSLTFrame.gs_version_str.equals("") == false) {	// for Ghostscript
					String testimage_source = testimage_base_path + "sanitytest.eps";
					testimage_source = utils.file_absolutePath(testimage_source);

					Boolean exists = utils.existsFile(testimage_source);
					if (exists == true) { // call Ghostscript only
						testimage_source = testimage_source.replace(" ","~blnk~");
						String testimage_target = temp_folder + PACKAGE_NAME + ".sanitytest.jpg";
						sanity_app_path = BatchXSLT.g_mainXSLTFrame.gs_pgm_path;
						// make command: gs -sDEVICE=jpeg -dEPSCrop -dJPEGQ=100 -dNOPAUSE -dBATCH -dSAFER -r300 -sOutputFile="sanitytest.jpg" "sanitytest.eps"
						sanity_params = "-sDEVICE=jpeg -dEPSCrop -dJPEGQ=100 -dNOPAUSE -dBATCH -dSAFER -r300 -sOutputFile=" + testimage_target + " " + testimage_source;
						sanity_envir = BatchXSLT.g_mainXSLTFrame.gs_envir;
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
							BatchXSLT.g_mainXSLTFrame.showMess("     Sanity Ghostscript PGM: '" + sanity_app_path + "'\n");
							BatchXSLT.g_mainXSLTFrame.showMess("                     params: '" + sanity_params + "'\n");
							BatchXSLT.g_mainXSLTFrame.showMess("                      envir: '" + sanity_envir + "'\n");
						}
						sanity_result = utils.callExternalApp(sanity_app_path, sanity_params, sanity_envir);
						if (sanity_result != 0) sanity_GS = "FAIL";
						else {	// check if target image exists
							exists = utils.existsFile(testimage_target);
							if (exists == true) {
								sanity_GS = "OK";
								if (BatchXSLT.g_mainXSLTFrame.im_gs_sanitycheck < 2) utils.deleteFile(testimage_target);
							}
							else sanity_GS = "fail";
						}
						if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("     Sanity Ghostscript RESULT: " + sanity_result + "\n");
					}
				}
					
					// sanity check ImageMagick talking with Ghostscript
				if ((numberof_readJobTicketFile_calls == 1)) {
					if ( (BatchXSLT.g_mainXSLTFrame.im_version_str.equals("") == false) && (BatchXSLT.g_mainXSLTFrame.gs_version_str.equals("") == false) ) {
						String testimage_source = testimage_base_path + "sanitytest.eps";
						testimage_source = utils.file_absolutePath(testimage_source);
						Boolean exists = utils.existsFile(testimage_source);
						if (exists == true) { // call ImageMagick calling Ghostscript
							testimage_source = testimage_source.replace(" ","~blnk~");
							String testimage_target = temp_folder + PACKAGE_NAME + ".sanitytest.png";
							sanity_app_path = BatchXSLT.g_mainXSLTFrame.im_pgm_path;
							sanity_params = "-verbose -flatten " + testimage_source + " " + testimage_target;
							sanity_envir = BatchXSLT.g_mainXSLTFrame.im_envir;
							if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
								BatchXSLT.g_mainXSLTFrame.showMess("     Sanity IM-GS PGM: '" + sanity_app_path + "'\n");
								BatchXSLT.g_mainXSLTFrame.showMess("               params: '" + sanity_params + "'\n");
								BatchXSLT.g_mainXSLTFrame.showMess("                envir: '" + sanity_envir + "'\n");
							}
							sanity_result = utils.callExternalApp(sanity_app_path, sanity_params, sanity_envir);
							if (sanity_result != 0) {
							  if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) {
							    BatchXSLT.g_mainXSLTFrame.showMess("     #ERR: Sanity result code: '" + sanity_result + "'\n");
							  }
							  sanity_IM_GS = "FAIL";
							}
							else {	// check if target image exists
								exists = utils.existsFile(testimage_target);
								if (exists == true) {
									sanity_IM_GS = "OK";
									if (BatchXSLT.g_mainXSLTFrame.im_gs_sanitycheck < 2) utils.deleteFile(testimage_target);
								}
								else sanity_IM_GS = "fail";
							}
							if (BatchXSLT.g_mainXSLTFrame.im_debug > 0) BatchXSLT.g_mainXSLTFrame.showMess("     Sanity IM-GS RESULT: " + sanity_result + "\n");
						}
					}

					String sanity_result_summary = "";
					if (sanity_IM.equals("") == false) sanity_result_summary += "IM: " + sanity_IM;
					if (sanity_GS.equals("") == false) {
						if (sanity_result_summary.equals("") == false) sanity_result_summary += ", ";
						sanity_result_summary += "GS: " + sanity_GS;
					}
					if (sanity_IM_GS.equals("") == false) {
						if (sanity_result_summary.equals("") == false) sanity_result_summary += ", ";
						sanity_result_summary += "IM-GS: " + sanity_IM_GS;
					}
					BatchXSLT.g_mainXSLTFrame.showMess("*** Sanity Check Results: " + sanity_result_summary + "\n");
				}
			
			}


		}


		/************************************************************************
		 * REMOTE BSH Interpreter
		 */
		// call BSH Interpreter for strings or script files
		try {	// Interprete code from a String
			String srcStr = jt_file.getString( "BSHInterpreteString" );
			if (srcStr.equals("") == false) {
				int bsherror = BXSLT_SERVER.BSH_InterpreteString(srcStr);
				if (bsherror != 0) BatchXSLT.g_mainXSLTFrame.showMess("BSH ERROR " + bsherror + " in source string: '" + srcStr + "'\n");
			}
		} catch (MissingResourceException e) {}
		try {	// Interprete code from a source file
			String srcpath = jt_file.getString( "BSHInterpreteFile" );
			if (srcpath.equals("") == false) {
				if ( srcpath.startsWith("~") == true) {
					String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
					srcpath = home + srcpath.substring(2);	// cut leading path ~/
				}
				int bsherror = BXSLT_SERVER.BSH_InterpreteSource(srcpath);
				if (bsherror != 0) BatchXSLT.g_mainXSLTFrame.showMess("BSH ERROR " + bsherror + " i source file: '" + srcpath + "'\n");
			}
		} catch (MissingResourceException e) {}

		/************************************************************************
		 * start REMOTE BSH Interpreter Server
		 */
		try {
			String BSHremotePort = jt_file.getString( "BSHremote" );
			if (BSHremotePort.equals("") == false) {
				int bsherror = BXSLT_SERVER.BSH_remote(BSHremotePort);
				if (bsherror != 0) BatchXSLT.g_mainXSLTFrame.showMess("BSH ERROR REMOTE start " + bsherror + "\n");
			}
		} catch (MissingResourceException e) {}
		

		/************************************************************************
		 *  ok set the dialog fields
		 */
		if ((func == 1) || (func == 0)) {	// we have called this routine from outside Swing
			BatchXSLT.g_mainXSLTFrame.setDialogFields(true);
			BatchXSLT.g_mainXSLTFrame.setItemStates(0,false);
		}
		if (func == 2) {	// do it directly - we have called this routine from a Swing item
			BatchXSLT.g_mainXSLTFrame.do_setDialogFields(true);
			BatchXSLT.g_mainXSLTFrame.do_setItemStates(true,false);
		}

		BatchXSLT.g_mainXSLTFrame.jobticket_loaded = true;
		BatchXSLT.g_mainXSLTFrame.showRunMess("");
		if (numberof_readJobTicketFile_calls == 1) BatchXSLT.g_mainXSLTFrame.showMess( "*** JobTicket '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "' loaded " + loaded_where + ".\n" );
		BatchXSLT.g_mainXSLTFrame.jobticket_is_loading = false;

		// if we have declared dynamic controls we have to adjust window dimensions
		//BatchXSLT.g_mainXSLTFrame.setMainWindowSize();
		BatchXSLT.g_mainXSLTFrame.hideUserControls();
		if (func == 0) {	// load jobTicket but do not run it
			BatchXSLT.g_mainXSLTFrame.showMess( "*** JobTicket '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "' loaded " + loaded_where + ".\n" );
			return(-99);
		}
		return(0);
	}




	/* ===========================================================
	 * save info to a JobTicket file
	 */
	public static void saveToJobTicketFile()
	{
		Calendar cal = Calendar.getInstance();		// get current date and time
		String	cmonth = "" + (cal.get(cal.MONTH)+1); if (cmonth.length() < 2) cmonth = "0" + cmonth;
		String	cday = "" + cal.get(cal.DAY_OF_MONTH); if (cday.length() < 2) cday = "0" + cday;
		String	cd = ( cal.get(cal.YEAR) + "/" + cmonth + "/" + cday );
		String	cmin = "" + cal.get(cal.MINUTE); if (cmin.length() < 2) cmin = "0" + cmin;
		String	ct = ( cal.get(cal.HOUR_OF_DAY) + ":" + cmin );
		String	tmpstr = "";
		char[]	tmpchr;
		String	crlf = "\r\n";

	 	if (BatchXSLT.g_mainXSLTFrame.jobTicketFileName.compareTo("") != 0) {
	 		BufferedWriter pOutputFile;
	 		try{
	 			//pOutputFile = new BufferedWriter( new FileWriter( BatchXSLT.g_mainXSLTFrame.jobTicketFileName ) );
	 			pOutputFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream( BatchXSLT.g_mainXSLTFrame.jobTicketFileName ), "UTF8"));
	 			pOutputFile.write("#"); pOutputFile.write(crlf);
	 			pOutputFile.write("# JobTicket - generated by " + BatchXSLT.g_mainXSLTFrame.applFullName + " V" + BatchXSLT.g_mainXSLTFrame.applMajorVersion + "." + BatchXSLT.g_mainXSLTFrame.applMinorVersion + " (" + BatchXSLT.g_mainXSLTFrame.systemOSname + ")"); pOutputFile.write(crlf);
				pOutputFile.write("# Path and Name: " + BatchXSLT.g_mainXSLTFrame.jobTicketFileName); pOutputFile.write(crlf);
	 			pOutputFile.write("# Creation Date and Time: " + cd + " " + ct); pOutputFile.write(crlf);
	 			pOutputFile.write("#"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The folder separator character must be a '/'!"); pOutputFile.write(crlf);
	 			pOutputFile.write("#"); pOutputFile.write(crlf);
	 			pOutputFile.write("# See section 'OPTIONS SETTINGS Description' for a description of possible settings."); pOutputFile.write(crlf);
	 			pOutputFile.write("#"); pOutputFile.write(crlf);

				pOutputFile.write(crlf);
	 			pOutputFile.write("jobticketVersion = " + BatchXSLT.g_mainXSLTFrame.jobticketVersion); pOutputFile.write(crlf);
	 			pOutputFile.write("applicationPurpose = " + BatchXSLT.g_mainXSLTFrame.applFunction); pOutputFile.write(crlf);
	 			pOutputFile.write("onlineManualUrl = " + BatchXSLT.g_mainXSLTFrame.onlineManualUrl); pOutputFile.write(crlf);

				pOutputFile.write(crlf);
	 			pOutputFile.write("clearMessages = " + BatchXSLT.g_mainXSLTFrame.clearMessageWindow); pOutputFile.write(crlf);

				pOutputFile.write(crlf);
	 			pOutputFile.write("mode = " + BatchXSLT.g_mainXSLTFrame.modeCBx.getSelectedIndex()); pOutputFile.write(crlf);

				pOutputFile.write(crlf);
	 			pOutputFile.write("jt_triggerfile = " + BatchXSLT.g_mainXSLTFrame.jt_triggerfile); pOutputFile.write(crlf);

				if (BatchXSLT.g_mainXSLTFrame.jt_schedule.equals("") == false) {	// we have a scheduler string
					pOutputFile.write(crlf);
					pOutputFile.write("#             a Start    End      Start  End    Days    Repeat"); pOutputFile.write(crlf);
					pOutputFile.write("#             c date     date     time   time   of      time"); pOutputFile.write(crlf);
					pOutputFile.write("#             t                   0><    0><    week    in"); pOutputFile.write(crlf);
					pOutputFile.write("#             i                   235959 235959 1=Sun   secs"); pOutputFile.write(crlf);
					pOutputFile.write("#             v,yyyymmtt,yyyymmtt,hhmmss,hhmmss,1234567,ssss"); pOutputFile.write(crlf);
					pOutputFile.write("jt_schedule = " + BatchXSLT.g_mainXSLTFrame.jt_schedule); pOutputFile.write(crlf);
				}

				pOutputFile.write(crlf);
				tmpstr = BatchXSLT.g_mainXSLTFrame.sourcePathNameFld.getText();
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
	 			pOutputFile.write("sourcePathName = " + tmpstr); pOutputFile.write(crlf);
				tmpstr = BatchXSLT.g_mainXSLTFrame.xslPathNameFld.getText();
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
	 			pOutputFile.write("xslPathName = " + tmpstr); pOutputFile.write(crlf);
				tmpstr = BatchXSLT.g_mainXSLTFrame.outputPathNameFld.getText();
				if (isWindows() == true) {
					if (get_URL_file_type(tmpstr) == 1) tmpstr = tmpstr.replace('\\','/');	// for local files
					else ;	// for URLs leave all '/' as they are
				}
	 			pOutputFile.write("outputPathName = " + tmpstr); pOutputFile.write(crlf);

				pOutputFile.write("xslVersionName = " + BatchXSLT.g_mainXSLTFrame.xslVersionName); pOutputFile.write(crlf);
				pOutputFile.write("xslVersionDate = " + BatchXSLT.g_mainXSLTFrame.xslVersionDate); pOutputFile.write(crlf);
				pOutputFile.write(crlf);
				
				pOutputFile.write("# IDML package related vars"); pOutputFile.write(crlf);
				pOutputFile.write("IDMLprocessingMessages = " + BatchXSLT.g_mainXSLTFrame.IDMLprocessingMessages); pOutputFile.write(crlf);
				pOutputFile.write("IDMLpreserveExpandedPackage = " + BatchXSLT.g_mainXSLTFrame.IDMLpreserveExpandedPackage); pOutputFile.write(crlf);
				pOutputFile.write(crlf);
				
				// get the transformer parameters - this reads property strings in the form
				// 'tp_X=param_name=value' CR
	 			pOutputFile.write("# Parameters to pass to the XSL Style Sheet."); pOutputFile.write(crlf);
	 			pOutputFile.write("# The following 'tp_X' (tp_1..tp_99) entries are parameters passed to the XSL Style Sheet."); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax: tp_X = name=value"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Example: tp_1 = myparameter1=any desired value"); pOutputFile.write(crlf);

				String param_base_name = "tp_";
				String params_str = BatchXSLT.g_mainXSLTFrame.xslt_paramsArea.getText();	// get the whole parameters (multiple lines)
				int i = 1;
				while (params_str.equals("") == false) {
					// get one parameter line
					int pos = params_str.indexOf('\n');
					if (pos < 0) {	// a line not ending with a \n
						tmpstr = params_str;
						params_str = ""; 		// and we are done
					}
					else {	// a line ending with a \n
						tmpstr = params_str.substring(0,pos);
						params_str = params_str.substring(pos + 1);	// get rest of string
					}
					if ((tmpstr.equals("") == false) && (tmpstr.length() >= 2)) {	// write it down
						String ps = param_base_name + i;
		 				pOutputFile.write(ps + " = " + tmpstr); pOutputFile.write(crlf);
		 				i++;
		 			}
				}


				pOutputFile.write(crlf);
	 			pOutputFile.write("# The 3 following entries are used only if the target ('outputPathName') is an FTP URL."); pOutputFile.write(crlf);
				tmpstr = BatchXSLT.g_mainXSLTFrame.usernameFld.getText();
	 			pOutputFile.write("ftpUsername = " + tmpstr); pOutputFile.write(crlf);

				tmpstr = "";
				tmpchr = BatchXSLT.g_mainXSLTFrame.passwordFld.getPassword();
				for (i = 0; i < tmpchr.length; i++) {
					tmpstr += tmpchr[i];
				}
	 			pOutputFile.write("ftpPassword = " + tmpstr); pOutputFile.write(crlf);

				tmpstr = BatchXSLT.g_mainXSLTFrame.portFld.getText();
	 			pOutputFile.write("ftpPort = " + tmpstr); pOutputFile.write(crlf);

				tmpstr = "" + BatchXSLT.g_mainXSLTFrame.ftpEncodingCBx.getSelectedItem();
	 			pOutputFile.write("ftpEncoding = " + tmpstr); pOutputFile.write(crlf);
				pOutputFile.write(crlf);

				tmpstr = BatchXSLT.g_mainXSLTFrame.newoutputFileNameExtFld.getText();
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
	 			pOutputFile.write("newoutputFileNameExt = " + tmpstr); pOutputFile.write(crlf);

				tmpstr = BatchXSLT.g_mainXSLTFrame.excludeCleanupRunExtsFld.getText();
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
	 			pOutputFile.write("excludeCleanupRunFileNameExts = " + tmpstr); pOutputFile.write(crlf);

				tmpstr = BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunExtsFld.getText();
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
	 			pOutputFile.write("excludeSourceProcessingRunFileNameExts = " + tmpstr); pOutputFile.write(crlf);

				tmpstr = BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNames;
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
   	 			pOutputFile.write("excludeSourceProcessingRunFileNames = " + tmpstr); pOutputFile.write(crlf);

				switch (BatchXSLT.g_mainXSLTFrame.sourceFileActionCBx.getSelectedIndex()) {
					case 0: tmpstr = "*DELETE*"; break;
					case 1: tmpstr = ""; break;
					case 2: tmpstr = BatchXSLT.g_mainXSLTFrame.sourceFileActionFld.getText();
							if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
							break;
				}
	 			pOutputFile.write("sourceFileAction = " + tmpstr); pOutputFile.write(crlf);
				pOutputFile.write(crlf);

				if (BatchXSLT.g_mainXSLTFrame.deleteSourceDirsCheckBox.isSelected() == true)  tmpstr = "1";
				else tmpstr = "0";
	 			pOutputFile.write("deleteSourceDirs = " + tmpstr); pOutputFile.write(crlf);
				pOutputFile.write(crlf);

	 			pOutputFile.write("loopDelay = " + BatchXSLT.g_mainXSLTFrame.delayJTFld.getText()); pOutputFile.write(crlf);
				pOutputFile.write(crlf);
				tmpstr = BatchXSLT.g_mainXSLTFrame.nextJTNameFld.getText();
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
	 			pOutputFile.write("nextJobTicketFileName = " + tmpstr); pOutputFile.write(crlf);
	 			pOutputFile.write("nextJobTicketPath = " + BatchXSLT.g_mainXSLTFrame.nextJobTicketPath); pOutputFile.write(crlf);
				if (BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath.equals("") == false) {
					pOutputFile.write("JobTicketOverrideQueuePath = " + BatchXSLT.g_mainXSLTFrame.jtoverrideQueuePath); pOutputFile.write(crlf);
				}
				if (BatchXSLT.g_mainXSLTFrame.jtoverrideQueueName.equals("") == false) {
					pOutputFile.write("JobTicketOverrideQueueName = " + BatchXSLT.g_mainXSLTFrame.jtoverrideQueueName); pOutputFile.write(crlf);
				}
				if (BatchXSLT.g_mainXSLTFrame.writeCommPrefs != 0) {
					pOutputFile.write("writeCommPrefs = " + BatchXSLT.g_mainXSLTFrame.writeCommPrefs); pOutputFile.write(crlf);
				}
				
/*
	 			pOutputFile.write("# ---- Creating a folder index file (XML) ----"); pOutputFile.write(crlf);
				tmpstr = BatchXSLT.g_mainXSLTFrame.folderindexnameFld.getText();
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
	 			pOutputFile.write("folderIndexName = " + tmpstr); pOutputFile.write(crlf);
				tmpstr = BatchXSLT.g_mainXSLTFrame.folderindexXSLFld.getText();
				if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
	 			pOutputFile.write("folderIndexXSL = " + tmpstr); pOutputFile.write(crlf);
				pOutputFile.write(crlf);
*/

				pOutputFile.write(crlf);
	 			pOutputFile.write("# ---- Log File ----"); pOutputFile.write(crlf);
	 			pOutputFile.write("logfileWrite = " +  BatchXSLT.g_mainXSLTFrame.logfile_write); pOutputFile.write(crlf);


				String tmp_str = BatchXSLT.g_mainXSLTFrame.userHome; // userHome does not end with file.separator
				if (tmp_str.endsWith(File.separator) == false)  tmp_str = tmp_str + File.separator;
				pOutputFile.write("logfilePath = "); 
					if (BatchXSLT.g_mainXSLTFrame.logfile_path.equals(tmp_str) == false) {
						tmpstr = BatchXSLT.g_mainXSLTFrame.logfile_path;
						if (isWindows() == true) tmpstr = tmpstr.replace('\\','/');
						pOutputFile.write(tmpstr);
					}
					pOutputFile.write(crlf);
	 			pOutputFile.write("logfileName = " +  BatchXSLT.g_mainXSLTFrame.logfile_name); pOutputFile.write(crlf);
	 			pOutputFile.write("logfileMaxSize = " +  BatchXSLT.g_mainXSLTFrame.logfile_maxsize); pOutputFile.write(crlf);
	 			pOutputFile.write("logfileMax = " +  BatchXSLT.g_mainXSLTFrame.logfile_max); pOutputFile.write(crlf);
				pOutputFile.write(crlf);

	 			pOutputFile.write("# ---- Options to control GhostScript, pstotext and ImageMagick's convert ----"); pOutputFile.write(crlf);
	 			pOutputFile.write("gs_convert = " + BatchXSLT.g_mainXSLTFrame.gs_convert); pOutputFile.write(crlf);
	 			pOutputFile.write("gs_envir = " + BatchXSLT.g_mainXSLTFrame.gs_envir.replace('\\','/')); pOutputFile.write(crlf);
	 			pOutputFile.write("gs_pgm_path = " + BatchXSLT.g_mainXSLTFrame.gs_pgm_path.replace('\\','/')); pOutputFile.write(crlf);
	 			pOutputFile.write("gs_version_num = " + BatchXSLT.g_mainXSLTFrame.gs_version_num); pOutputFile.write(crlf);
	 			pOutputFile.write("eps2xml_associate_XSL = " + BatchXSLT.g_mainXSLTFrame.eps2xml_associate_XSL.replace('\\','/')); pOutputFile.write(crlf);
				pOutputFile.write(crlf);
	 			pOutputFile.write("pstotext_envir = " + BatchXSLT.g_mainXSLTFrame.pstotext_envir.replace('\\','/')); pOutputFile.write(crlf);
	 			pOutputFile.write("pstotext_pgm_path = " + BatchXSLT.g_mainXSLTFrame.pstotext_pgm_path.replace('\\','/')); pOutputFile.write(crlf);
				pOutputFile.write(crlf);
	 			pOutputFile.write("im_debug = " + BatchXSLT.g_mainXSLTFrame.im_debug); pOutputFile.write(crlf);
	 			pOutputFile.write("im_envir = " + BatchXSLT.g_mainXSLTFrame.im_envir.replace('\\','/')); pOutputFile.write(crlf);
	 			pOutputFile.write("im_pgm_path = " + BatchXSLT.g_mainXSLTFrame.im_pgm_path.replace('\\','/')); pOutputFile.write(crlf);
	 			pOutputFile.write("im_version_num = " + BatchXSLT.g_mainXSLTFrame.im_version_num); pOutputFile.write(crlf);
	 			pOutputFile.write("im_quantumdepth = " + BatchXSLT.g_mainXSLTFrame.im_quantumdepth); pOutputFile.write(crlf);
	 			pOutputFile.write("im_arch = " + BatchXSLT.g_mainXSLTFrame.im_arch); pOutputFile.write(crlf);
				pOutputFile.write(crlf);

	 			pOutputFile.write("# ---- Options to check external converters ----"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---- 0 = don't check, 1 = GhostScript, 2 = pstotext, 4 = ImageMagick. (may be added for multiple checks)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---- Above paths must be set"); pOutputFile.write(crlf);
	 			pOutputFile.write("checkExtConverter = " + BatchXSLT.g_mainXSLTFrame.check_for_external_converters); pOutputFile.write(crlf);
				pOutputFile.write(crlf);

	 			pOutputFile.write("# ---- Dynamic Dialog Controls ----"); pOutputFile.write(crlf);

				String dialogControl_base_name = "dialogControl_";
				String ps = "";
				for (int j = 0; (j < BatchXSLT.g_mainXSLTFrame.dynControlsNames.length) && (BatchXSLT.g_mainXSLTFrame.dynControlsNames[j][0] != null) && (BatchXSLT.g_mainXSLTFrame.dynControlsNames[j][0].equals("") == false) ; j++) {
					// get one parameter line
					ps = dialogControl_base_name + (j+1);
		 			pOutputFile.write(ps + "=" + BatchXSLT.g_mainXSLTFrame.dynControlsNames[j][1]); pOutputFile.write(crlf); // write the command line
				}

				pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------- END OPTIONS SETTINGS --------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The following is description only."); pOutputFile.write(crlf);

				pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------- PARAMETERS PASSED AUTOMATICALLY TO XSL STYLESHEET --------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# The following parameters may be accessed from the XSL stylesheet by defining "); pOutputFile.write(crlf);
	 			pOutputFile.write("# <xsl:param name=\"name\"/>"); pOutputFile.write(crlf);
	 			pOutputFile.write("# at the root lexel of the stylesheet"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Example: <xsl:param name=\"SYSTEM_OS_NAME\"/>"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: XMLSRC_VERSION"); pOutputFile.write(crlf);
				pOutputFile.write("# The version attribute of the XML source file"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: XMLSRC_ENCODING"); pOutputFile.write(crlf);
				pOutputFile.write("# the encoding of the XML source file"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: XMLSRC_DOCTYPE_DECLARATION"); pOutputFile.write(crlf);
				pOutputFile.write("# The DOCTYPE declaration of the XML source file"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: SYSTEM_OS_NAME"); pOutputFile.write(crlf);
				pOutputFile.write("# The system property os.name"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: SYSTEM_VM_VERSION"); pOutputFile.write(crlf);
				pOutputFile.write("# The system property os.version"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: SYSTEM_DEFAULT_CHARSET"); pOutputFile.write(crlf);
				pOutputFile.write("# The system's default encoding"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: TRANSFORM_ENGINE"); pOutputFile.write(crlf);
				pOutputFile.write("# The main engine name: (BatchXSLT)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: INPUT_NAME"); pOutputFile.write(crlf);
				pOutputFile.write("# The name of the input XML file"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: INPUT_PATH"); pOutputFile.write(crlf);
				pOutputFile.write("# The base path to the input XML file"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: INPUT_SUB_PATH"); pOutputFile.write(crlf);
				pOutputFile.write("# The sub path to the input XML file"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: OUTPUT_PATH"); pOutputFile.write(crlf);
				pOutputFile.write("# The path to the output folder"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: OUTPUT_NAME"); pOutputFile.write(crlf);
				pOutputFile.write("# The name of the output folder"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: STYLESHEET_PATH"); pOutputFile.write(crlf);
				pOutputFile.write("# The PATH TO the loaded XSL stylesheet"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: STYLESHEET_NAME"); pOutputFile.write(crlf);
				pOutputFile.write("# The name of the loaded XSL stylesheet"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: GS_VERSION"); pOutputFile.write(crlf);
				pOutputFile.write("# Version string of reachable Ghostscript"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: GS_VERSION_NUM"); pOutputFile.write(crlf);
				pOutputFile.write("# Version number of reachable Ghostscript"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: GS_PGM_PATH"); pOutputFile.write(crlf);
				pOutputFile.write("# The path and name to Ghostscript program"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: GS_ENVIR"); pOutputFile.write(crlf);
				pOutputFile.write("# Environement variables to set when calling Ghostscript"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: IM_VERSION"); pOutputFile.write(crlf);
				pOutputFile.write("# Version string of reachable ImageMagick"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: IM_VERSION_NUM"); pOutputFile.write(crlf);
				pOutputFile.write("# Version number of reachable ImageMagick"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: IM_PGM_PATH"); pOutputFile.write(crlf);
				pOutputFile.write("# The path and name to ImageMagick program"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
				pOutputFile.write("# Parameter: IM_ENVIR"); pOutputFile.write(crlf);
				pOutputFile.write("# Environement variables to set when calling ImageMagick (the path to Ghostscript)"); pOutputFile.write(crlf);

	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------- OPTIONS SETTINGS Description --------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: applicationPurpose"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The main purpose of this BatchXSLT application. Do not change."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: clearMessages"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# clearMessages = [0][1]"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Clear the messages area when a jobTicket is loaded."); pOutputFile.write(crlf);
	 			pOutputFile.write("# 0 = Don't clear"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 1 = Clear it"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: mode"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# mode = [0][1][2]"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the operation mode of a JobTicket."); pOutputFile.write(crlf);
	 			pOutputFile.write("# Possible values are:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 0 = Manual (single shot): just load the given settings and wait for more user input"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 1 = Once (on start): go and do what ever is defined once on program start"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 2 = Once (quit when done): go and do what ever is defined once on program start and then quit this application after done"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 3 = Batch (loop for ever): go and do what ever is defined in a loop over and over again"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Load the given stuff and wait:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# mode = 0"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Load the given stuff and do it once:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# mode = 1"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Load the given stuff and do it - quit after done:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# mode = 3"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# See keyword 'nextJobTicketFileName' below for Batch mode."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: jt_schedule"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# jt_schedule = active,[StartDate],[EndDate],[StartTime],[EndTime],[WeekDays],[RepeatInterval]"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The schedule when to run a JobTicket."); pOutputFile.write(crlf);
	 			pOutputFile.write("# Values:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# active = 0: schedule is inactive, otherwise is active"); pOutputFile.write(crlf);
	 			pOutputFile.write("# StartDate = YYYYMMDD: schedule is active after this date, otherwise is active immediately"); pOutputFile.write(crlf);
	 			pOutputFile.write("# EndDate   = YYYYMMDD: schedule is valid through this date, after this inactive, empty: valid forever"); pOutputFile.write(crlf);
	 			pOutputFile.write("# StartTime = HHMMSS: scheduling starts at this time, empty: immediate start"); pOutputFile.write(crlf);
	 			pOutputFile.write("# EndTime   = HHMMSS: scheduling ends at this time, empty: run through end of day"); pOutputFile.write(crlf);
	 			pOutputFile.write("# WeekDays  = on which days to run. 1 = Sunday... 7 = Saturday. 23456 = run from Monday to Friday"); pOutputFile.write(crlf);
	 			pOutputFile.write("# RepeatInterval = interval to run this JobTicket in seconds. 600 = run every 10 minutes"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Run every day from 11 to 19 every 10 minutes"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 1,20000101,29991231,110000,190000,1234567,600"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Run once a day at 3 O'clock in the night except on sundays"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 1,20000101,29991231,030000,,234567,"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: jt_triggerfile"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# jt_triggerfile = [filename]"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The name of a trigger file which must exiss to start transform in this folder."); pOutputFile.write(crlf);
	 			pOutputFile.write("# if empty: no trigger file is needed to start processing"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Note:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The trigger file does not need to have any content - it may be empty"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: sourcePathName"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# sourcePathName = Path/Name"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the path to the source file(s) to process."); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be either:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - the directory path only to scan for files to process"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   and usually contains source XML files with the file name extension '.xml'."); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - the path and name to a single file to process."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# A path only to automatically search for files:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# sourcePathName = /MyVolume/aDir/anextDir/theXMLDir/"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# A path and name of a single source XML file to transform:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# sourcePathName = /MyVolume/aDir/anextDir/theXMLDir/mySourceFile.xml"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: xslPathName"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# xslPathName = [Path/Name]"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# xslPathName = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the path/name to the XSL file to process the source file(s) with."); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be either:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - the path and name of the XSL file"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   if it is not already stated in the source XML file."); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - empty"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   if the XSL file to use is stated in the source XML file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   <?xml-stylesheet href='path/name to XSL file..' type='text/xsl'?>"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: tp_XX"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# tp_X = ParameterName=Value"); pOutputFile.write(crlf);
	 			pOutputFile.write("# where:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# X = Number from 1 to 99 - like: tp_1 or tp_23"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Named Parameters to pass to the XSL Style Sheet."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: outputPathName"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# outputPathName = [Path/Name][*NONE]"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the path where the target file(s) should be stored."); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be either:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - the directory path to a local or network volume only, use the source file's name."); pOutputFile.write(crlf);
	 			pOutputFile.write("#   if it is a path only, make sure it is terminated with a '/'"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - the directory path/name to a single output file's name"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - an URL to a remote FTP server and path"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   ex: ftp://196.23.12.120/path/"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   you have to enter the 'user name', 'password' and optionally the port number"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - empty"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   to store the output file at the same path as the source file."); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - *NONE*"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   to create no output file - like pushing the data into a database."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# A path only, the output file name is generated from the source file's name:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# outputPathName = /MyVolume/aDir/anextDir/theOutputDir/"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# A path and name of a single output file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# outputPathName = /MyVolume/aDir/anextDir/theOutputDir/myOutputFile.xml"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# An URL and path to a remote FTP server:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# outputPathName = ftp://196.23.12.120/mysubdir/"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# empty to store the output file at the same directory as the source file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# outputPathName = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# if empty, you have to set a 'newoutputFileNameExt' (see below)."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: ftpUsername"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpUsername = name"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpUsername = anonymous"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpUsername = anonymous"); pOutputFile.write(crlf);
	 			pOutputFile.write("# for an 'anonymous' login as Guest"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the user name the FTP protocol should use to login into the remote server."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: ftpPassword"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpPassword = password"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpPassword = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpPassword = mypassword"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the user's password the FTP protocoll should use to login into the remote server."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: ftpPort"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpPort = portnumber"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpPort = 21"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 21 is the default FTP port"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# none."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: ftpEncoding"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpEncoding = encoding name"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ftpEncoding = windows-1252"); pOutputFile.write(crlf);
	 			pOutputFile.write("# UTF-8 is the default encodingt"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The character encoding to use to talk with the remote FTP server"); pOutputFile.write(crlf);
	 			pOutputFile.write("# on the control connection."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: newoutputFileNameExt"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# newoutputFileNameExt = [.file name extension]"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# newoutputFileNameExt = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the new output file name extension to add to the output file(s)."); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be either:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - set to a name extension"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - empty"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   to leave the file name extension untouched"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# a new file name extension to represent the type of the output file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - if the content of the output file is HTML code:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# newoutputFileNameExt = .html"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - if the content of the output file is XML code:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# newoutputFileNameExt = .xml"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - if the content of the output file is text:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# newoutputFileNameExt = .txt"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# it may be empty if the original file name should be left as it is:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# newoutputFileNameExt = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: excludeSourceProcessingRunFileNameExts"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeSourceProcessingRunFileNameExts = [comma separated list of file name extensions]"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeSourceProcessingRunFileNameExts = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# A list of file name extension(s). These files will be excluded from being transformed."); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be either:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - a comma separated list of name extensions"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - empty"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   to process all files"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - To process all files: empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeSourceProcessingRunFileNameExts = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# - To exclude file names ending with '.xsl':"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeSourceProcessingRunFileNameExts = .xsl"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - To exclude multiple files:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeSourceProcessingRunFileNameExts = .xsl,_int.xml"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Filename extensions may be given in upper or lower case:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# '.xsl' will exclude files named 'xxx.xsl' and 'xxx.Xsl' and 'xxx.XSL'"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: excludeCleanupRunFileNameExts"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeCleanupRunFileNameExts = [comma separated list of file name extensions]"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeCleanupRunFileNameExts = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# A list of file name extension(s). These files will not be touched (or moved) during the clean up run."); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be either:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - a comma separated list of name extensions"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - empty"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   to move all files"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - To process all files: empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeCleanupRunFileNameExts = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# - To leave in place file names ending with '.xsl' and not to touch the folder XSLCSS:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# excludeCleanupRunFileNameExts = .xsl,XSLCSS"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Filename extensions may be given in upper or lower case:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# '.xsl' will exclude files named 'xxx.Xsl' and 'xxx.XSL'"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: sourceFileAction"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# sourceFileAction = [*DELETE*]"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# sourceFileAction = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This describes the action to perform with the source file(s) after having processed it."); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be either:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - empty"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   to leave the original file name extension untouched."); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - *DELETE*"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   to delete the source file(s) after done."); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - A folder path to move the file(s) to after processing."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# To delete the source file(s) after done:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# sourceFileAction = *DELETE*"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# To do nothing with the source file(s) after done:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# sourceFileAction = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# To do move the source file(s) after done:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# sourceFileAction = /MyVolume/myDir1/myDir2/"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Be careful:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# In 'Batch' mode, doing nothing with the source file(s) may result in"); pOutputFile.write(crlf);
	 			pOutputFile.write("# a never ending loop, processing the same file(s) over and over again!"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: deleteSourceDirs"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# deleteSourceDirs = [0][1]"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# By default, empty folders are deleted from a scanned subfolder path."); pOutputFile.write(crlf);
	 			pOutputFile.write("# You may leave empty folders untouched by setting:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# deleteSourceDirs = 0"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is especially important, if BatchXSLT scans folder structures"); pOutputFile.write(crlf);
	 			pOutputFile.write("# which are created by other processes."); pOutputFile.write(crlf);
	 			pOutputFile.write("# In such cases, it may be vital (for the creating process) that"); pOutputFile.write(crlf);
	 			pOutputFile.write("# these folders are not removed from a scanned tree."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: folderIndexName"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# folderIndexName = [filename.xml]"); pOutputFile.write(crlf);
	 			pOutputFile.write("# If the filename does not end with '.xml', it is added"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Create an XML file describing the content of each folder seen from 'sourcePathName' including sub folders."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# For each (sub)folder create an index file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# folderIndexName = fidx.xml"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Create NO index file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# folderIndexName = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# These folder index files may be used to automatically browse through folders."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: logfileWrite"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# logfileWrite = [0][1]"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 0 = write messages to log file, 0 = don't"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: logfilePath"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# logfilePath = a directory path or empty for default path: the user's home dir"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: logfileName"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# logfileName = a filename or empty for the default name '" + BatchXSLT.g_mainXSLTFrame.default_logfile_name + "'"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: logfileMaxSize"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# logfileMaxSize = the maximum size of log files in bytes. Must be >= " + BatchXSLT.g_mainXSLTFrame.logfile_minsize); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: logfileMax"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# logfileMax = the maximum number of log files to keep. must be >= " + BatchXSLT.g_mainXSLTFrame.logfile_min); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: folderIndexXSL"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# folderIndexXSL = [path/filename.xsl]"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This path is included into a stylesheet statement of the index file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# <?xml-stylesheet href=\"/XTscl/Scripts/XMLTools/ePaperFoldercontent.xsl\" type=\"text/xsl\"?>"); pOutputFile.write(crlf);
	 			pOutputFile.write("# A Transformer may use this statement to automatically create a html output file"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Include the xsl into index file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# folderIndexXSL = /XTscl/Scripts/XMLTools/ePaperFoldercontent.xsl"); pOutputFile.write(crlf);
	 			pOutputFile.write("# To include no stylesheet:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# folderIndexXSL = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# These folder index files may be used to automatically browse through folders."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: loopDelay"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# loopDelay = time in milliseconds"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the time in milliseconds to wait before processing the next JobTicket."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# To wait 3 seconds before calling the next JobTicket:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# loopDelay = 3000"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Each JobTicket may contain a different value."); pOutputFile.write(crlf);
	 			pOutputFile.write("# We might want to process all chain JobTickets in short time"); pOutputFile.write(crlf);
	 			pOutputFile.write("# and wait longer after having processed the last JobTicket"); pOutputFile.write(crlf);
	 			pOutputFile.write("# before starting over with the first JobTicket again."); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: nextJobTicketFileName"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# nextJobTicketFileName = [Path/Name]"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# nextJobTicketFileName = "); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# This is the path/name to the next JobTicket to call after the"); pOutputFile.write(crlf);
	 			pOutputFile.write("# current JobTicket has finished processing."); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be either:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - the path and name of the next JobTicket file to call"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - empty"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   if no next JobTicket should be called"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# For Batch mode:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# - If 'nextJobTicketFileName' is left empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   The same (this) JobTicket will be processed over and over again."); pOutputFile.write(crlf);
	 			pOutputFile.write("# - If a 'nextJobTicketFileName' is given:"); pOutputFile.write(crlf);
	 			pOutputFile.write("#   The next JobTicket will be processed."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------- GhostScript Settings -------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The settings below must be edited manually (they may not be set from the main window)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# The following Options are GhostScript stuff"); pOutputFile.write(crlf);
	 			pOutputFile.write("# These Key words: gs_convert, gs_pgm_path, pstotext_pgm_path, eps2xml_associate_XSL"); pOutputFile.write(crlf);
	 			pOutputFile.write("# are NOT available on Classic Macintosh like Mac OS 9.x.x"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Use Unix, OS X, or Windows to control GhostScript and pstotext"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: gs_convert"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_convert = Option"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Use GhostScript to create PDF from any EPS file and to extract text into an XML file."); pOutputFile.write(crlf);
	 			pOutputFile.write("# Option:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 0 = do no EPS conversion = default"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 1 = convert EPS to PDF"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 2 = extract text from the created PDF"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 4 = replace line endings '\\n' in XML file with '<br/>\\n'"); pOutputFile.write(crlf);
	 			pOutputFile.write("# 1024 = debugging the extracted text: do not delete the tx1 iso-8859-1 file'"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Options may be added"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Convert EPS to PDF and extract the text"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_convert = 3"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: gs_envir"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_envir = Name=value"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The environement variables to set when calling GhostScript."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Set the PATH environement variable:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_envir = F:/Programs/gs/gs8.51/bin/    (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_envir = /usr/local/bin/               (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# To set multiple environment variables, separate them by three + characters +++"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_envir = PATH=/usr/local/bin/:/usr/local/mydir/+++MyVAR=hello      (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_envir = PATH=C:/Programs/gs/;C:/Ghostscript/bin/+++MyVAR=hello    (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: gs_pgm_path"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_pgm_path = path to GhostScript"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The full path and program name to the GhostScript program to use."); pOutputFile.write(crlf);
	 			pOutputFile.write("# If empty, the default program is used: 'gs' for Unix or 'gswin32c.exe' for Windows"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Use default program:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_pgm_path = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# Use from specified location:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_pgm_path = F:/Programs/gs/gs8.51/bin/gswin32c.exe    (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# gs_pgm_path = /usr/local/bin/gs                         (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: pstotext_envir"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_envir = Name=value"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The environement variables to set when calling pstotext."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Set the PATH environement variable:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_envir = F:/Programs/pstotext/        (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_envir = /usr/local/bin/               (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# To set multiple environment variables, separate them by three + characters +++"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_envir = PATH=/usr/local/bin/:/usr/local/mydir/+++MyVAR=hello      (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_envir = PATH=C:/Programs/gs/;C:/Ghostscript/bin/+++MyVAR=hello    (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_envir MUST be set and point to the GhostScript path!!!!!"); pOutputFile.write(crlf);
	 			pOutputFile.write("# the pstotext (pstotxt3.exe) programm calls GhostScript just by calling 'gs' (gswin32.exe) without Path!"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: pstotext_pgm_path"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_pgm_path = path to pstotext program"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The full path and program name to the 'Postscript to text' program to use."); pOutputFile.write(crlf);
	 			pOutputFile.write("# If empty, the default program is used: 'pstotext' for Unix or 'pstotxt3.exe' for Windows"); pOutputFile.write(crlf);
	 			pOutputFile.write("# IMPORTANT NOTE: Add the PATH to the environment variable 'PATH' on Windows!!!"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Use default program:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_pgm_path = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# Use from specified location:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_pgm_path = F:/Programs/pstotext/pstotxt3.exe    (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# pstotext_pgm_path = /usr/local/bin/pstotext              (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ***********"); pOutputFile.write(crlf);
	 			pOutputFile.write("# IMPORTANT NOTE:"); pOutputFile.write(crlf);
	 			pOutputFile.write("#         The PATH to the GhostScript program MUST be in the environment variable 'PATH' on Windows!!!"); pOutputFile.write(crlf);
	 			pOutputFile.write("#         because the pstotxt3.exe programm calls GhostScript just by calling 'gswin32c.exe' without Path"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ***********"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: eps2xml_associate_XSL"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# eps2xml_associate_XSL = XSL_pathname"); pOutputFile.write(crlf);
	 			pOutputFile.write("# or empty:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# eps2xml_associate_XSL = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# To ad no style sheet"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Associate an XSL Style Sheet to the resulting text XML file."); pOutputFile.write(crlf);
	 			pOutputFile.write("# Ads the declaration line to the resulting text XML file:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# <?xml-stylesheet href=\"XSL_pathname\" type=\"text/xsl\"?>"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The 'gs_convert' option must be 3 or 7"); pOutputFile.write(crlf);
	 			pOutputFile.write("# It may be a relative path/name or absolute"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Like this, the extracted text from PDF might be viewed with a browser"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# eps2xml_associate_XSL = ../xsl/epsextract.xsl"); pOutputFile.write(crlf);
	 			pOutputFile.write("# eps2xml_associate_XSL = epsextract.xsl"); pOutputFile.write(crlf);
	 			pOutputFile.write("# eps2xml_associate_XSL = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------- ImageMagick Settings -------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The settings below must be edited manually (they may not be set from the main window)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: im_envir"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_envir = Name=value"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The environement variables to set when calling ImageMagicks' convert."); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Set the PATH environement variable:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_envir = F:/Programs/ImageMagick/        (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_envir = /usr/local/bin/               (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# To set multiple environment variables, separate them by three + characters +++"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_envir = PATH=/usr/local/bin/:/usr/local/mydir/+++MyVAR=hello      (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_envir = PATH=C:/Programs/gs/;C:/Ghostscript/bin/+++MyVAR=hello    (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# im_envir MUST be set and point to the GhostScript path!!!!!"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ImageMagick's convert (convert.exe) programm calls GhostScript to convert EPS to JPEG"); pOutputFile.write(crlf);
	 			pOutputFile.write("# just by calling 'gs' (gswin32.exe) without Path!"); pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Key word: im_pgm_path"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Syntax:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_pgm_path = path to ImageMagick's convert program"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Description:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# The full path and program name to the ImageMagick's convert program to use."); pOutputFile.write(crlf);
	 			pOutputFile.write("# If empty, the default program is used: 'convert' for Unix or 'convert.exe' for Windows"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# Examples:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# Use default program:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_pgm_path = "); pOutputFile.write(crlf);
	 			pOutputFile.write("# Use from specified location:"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_pgm_path = F:/Programs/gs/gs8.51/bin/convert.exe    (for Windows)"); pOutputFile.write(crlf);
	 			pOutputFile.write("# im_pgm_path = /usr/local/bin/convert                   (for Unix)"); pOutputFile.write(crlf);
	 			pOutputFile.write(crlf);
	 			pOutputFile.write("# ---------------------------------------------------------------"); pOutputFile.write(crlf);
	 			pOutputFile.write("# -------------- End of SETTINGS Description --------------"); pOutputFile.write(crlf);


	 			pOutputFile.close();
	 			BatchXSLT.g_mainXSLTFrame.showMessWait("JobTicket file '" + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + "' saved.\n");
	 		}
	 		catch (IOException e) {
	 			BatchXSLT.g_mainXSLTFrame.showMess("#### Unable to write JobTicket file " + BatchXSLT.g_mainXSLTFrame.jobTicketFileName + ".\n");
	 		}
	 	}	
	}






	/* ===========================================================
	 * check if the given dir path contains a 'lock file'
	 */
	public static boolean has_lockfile(String thePath)
	{
		File f = new File(thePath);
		return ( has_lockfile(f));
	}
	/* ===========================================================
	 * check if the given file path contains a 'lock file'
	 */
	public static boolean has_lockfile(File theFile)
	{
		if ((theFile == null) || !theFile.exists()) return(false);
		int		i;
		String	flist[];

		flist = theFile.list();
		return ( has_lockfile(flist));
	}
	/* ===========================================================
	 * check if the given file list contains a 'lock file'
	 */
	public static boolean has_lockfile(String flist[])
	{
		if (flist == null) return(false);
		int		i;
		
		for (i = 0; i < flist.length; i++) {
			if (flist[i].toLowerCase().equals("##folderlocked") == true) return(true);
		}
		return(false);
	}





	/* ===========================================================
	 * build a directory/file tree within a given path
	 */
	public static boolean build_path_tree(String thePath)
	{
		if (mf_tree != null) {
			mf_tree = null;
		}
		
		if (thePath.length() <= 0) return (false);
		mf_tree = new ai.FilePack.MyFileStructure();
		mf_tree.setDirname(thePath);
		mf_tree.build();
		mf_tree.build_file_list();
		return(true);
	}
	/* ===========================================================
	 * get the site of the previously created path tree
	 */
	public static int file_tree_size()
	{
		if (mf_tree == null) return(0);
		return(mf_tree.file_list_size());
	}



	/* ===========================================================
	 * build and xml which shows all files in a dir
	 */
	public static int build_epaper_indexes(int theidx)
	{
		int		my_idx = theidx;
		String	my_path = "";
		String	my_name = "";
		String	ws = "";
		String	cs = "UTF-8";
		byte[]	barr;
		int		my_type = 0;
		boolean	is_a_dir;
		File idx_file = null;
		FileOutputStream idx_file_data = null;

		boolean	idx_file_open = false;

		// do not recreate the folder index file if already exists
		String root_path = mf_tree.getDirname();
		String sep = "";
		if (root_path.endsWith(File.separator) == false) sep = File.separator;
		if (BatchXSLT.g_mainXSLTFrame.folderIndexFileName.toUpperCase().endsWith(".XML") == false) 
			BatchXSLT.g_mainXSLTFrame.folderIndexFileName += ".xml";
		if ( file_exists(root_path + sep + BatchXSLT.g_mainXSLTFrame.folderIndexFileName) ) {
			return(-1);
		}

		if ( (mf_tree == null) || (mf_tree.file_list_size() <= 0) ) {
			BatchXSLT.g_mainXSLTFrame.showMess( "#### ERROR Creating Folder Index files! (no file list available)\n" );
			return(-2);
		}

		for (my_idx = 0; my_idx < mf_tree.file_list_size(); my_idx++) {

			my_path = mf_tree.get_file_path(my_idx);
			my_name = mf_tree.get_file_name(my_idx);
			if (my_path.equals("")) return(my_idx);
			if ( my_name.toUpperCase().equals(".DS_STORE") == true) continue;	// ignore this file
			if ( my_name.startsWith("._") == true) continue;	// hiddeen OSX files

			if (mf_tree.get_file_type(my_idx) == 2) {	//  this entry's type: 1 == file, 2 == dir
				is_a_dir = true;
				/*
				if ( (my_path.toUpperCase().endsWith(File.separator + "DTDCSS") == true)
						|| (my_path.toUpperCase().endsWith(File.separator + "XSLCSS") == true)
						|| (my_path.toUpperCase().endsWith(File.separator + "XSL") == true)
						|| (mf_tree.is_locked_folder(my_idx) == true) ) continue;
				*/
			}
			else is_a_dir = false;

			if (!idx_file_open) {
				BatchXSLT.g_mainXSLTFrame.showMess( "---- Creating Folder Index files..." );
				try {
					idx_file = new File(root_path + sep + BatchXSLT.g_mainXSLTFrame.folderIndexFileName);
					idx_file_data = new FileOutputStream(idx_file);
				} catch ( java.io.IOException e ) { break; }		// uups could not create target?
				idx_file_open = true;
	 			try {
	 				ws = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r<!DOCTYPE foldercontent>\r";
	 				barr = ws.getBytes(cs); idx_file_data.write(barr);
					if ( BatchXSLT.g_mainXSLTFrame.folderIndexFileXSLName.equals("") == false) {
	 					ws = "<?xml-stylesheet href=\"" + makeURLString(BatchXSLT.g_mainXSLTFrame.folderIndexFileXSLName) + "\" type=\"text/xsl\"?>\r";
	 					barr = ws.getBytes(cs);idx_file_data.write(barr);
					}
					String htmidx = BatchXSLT.g_mainXSLTFrame.folderIndexFileName;
					if (htmidx.lastIndexOf(".") > -1) {	// ad new extension
						htmidx = htmidx.substring(0,htmidx.lastIndexOf("."));
						htmidx += BatchXSLT.g_mainXSLTFrame.newoutputFileNameExt;
					}
	 				ws = "<foldercontent path=\"" + root_path + "\" id=\"-1\"" +
	 					" idxfile=\"" + BatchXSLT.g_mainXSLTFrame.folderIndexFileName + "\"" + " idxhtm=\"" + htmidx + "\">\r";
	 				barr = ws.getBytes(cs); idx_file_data.write(barr);

					Calendar cal = Calendar.getInstance();		// get current date and time
					String	cd = ( cal.get(cal.YEAR) + "/" + (cal.get(cal.MONTH)+1) + "/" + cal.get(cal.DAY_OF_MONTH) );
					String	ct = ( cal.get(cal.HOUR_OF_DAY) + ":" + cal.get(cal.MINUTE) );
	 				ws = "<info creatorName=\"" + BatchXSLT.g_mainXSLTFrame.applFullName + "\"" +
	 						" creatorVersion=\"" + BatchXSLT.g_mainXSLTFrame.applMajorVersion + "." + BatchXSLT.g_mainXSLTFrame.applMinorVersion + "\"" +
	 						" OS=\"" + BatchXSLT.g_mainXSLTFrame.systemOSname + "\"" +
	 						" VM=\"" + BatchXSLT.g_mainXSLTFrame.VMversion + "\"" +
	 						" systemEncoding=\"" + System.getProperty("file.encoding") + "\"" +
	 						" createDate=\"" + cd + "\" createTime=\"" + ct + "\" />\r";
	 				barr = ws.getBytes(cs); idx_file_data.write(barr);

				} catch ( java.io.IOException e) { break; }
			}

			if (is_a_dir) {
				String sub_path = get_sub_path(root_path,my_path);
				// cut the folder name from path
				my_path = my_path.substring(0,my_path.length() - my_name.length() - 1);
				try { 
	 				ws = "<folder path=\"" + makeURLString(my_path) + 
	 					"\" subpath=\"" + makeURLString(sub_path) + 
	 					"\" name=\"" + makeURLString(my_name) + "\" id=\"" + my_idx + "\"/>\r";
	 				barr = ws.getBytes(cs); idx_file_data.write(barr);
				} catch ( java.io.IOException e) {}
			}
			else {	// a normal file
				//BatchXSLT.g_mainXSLTFrame.showMess("                    file: " + my_name + "\n");
				String f_t = "";
				String sub_path = get_sub_path(root_path,my_path);
				do {
					if (my_name.toUpperCase().endsWith(".XML") == true) { f_t = "xml"; break; }
					if (my_name.toUpperCase().endsWith(".JPG") == true) { f_t = "jpg"; break; }
					if (my_name.toUpperCase().endsWith(".PDF") == true) { f_t = "pdf"; break; }
					if (my_name.toUpperCase().endsWith(".HTM") == true) { f_t = "htm"; break; }
					if (my_name.toUpperCase().endsWith(".HTML") == true) { f_t = "htm"; break; }
				} while(false);
				try { 
					/*
					String myurlpathstr = java.net.URLEncoder.encode(my_path,my_path);
					String myurlsubpathstr = java.net.URLEncoder.encode(sub_path);
					String myurlnamestr = java.net.URLEncoder.encode(my_name);
					*/
	 				ws = "<file path=\"" + makeURLString(my_path) + 
	 						"\" subpath=\"" + makeURLString(sub_path) + 
	 						"\" name=\"" +  makeURLString(my_name) + "\" id=\"" + my_idx + "\" type=\"" + f_t + "\"/>\r";
	 				barr = ws.getBytes(cs); idx_file_data.write(barr);
				}
				catch ( java.io.IOException e) {}
			}
		}


		if (idx_file_open) {
			try {
 				ws = "</foldercontent>\r";
 				barr = ws.getBytes(cs); idx_file_data.write(barr);
				idx_file_data.close();
			} catch ( java.io.IOException e) {}
			idx_file_open = false;
			BatchXSLT.g_mainXSLTFrame.showMess( "done.\n" );
		}

		return(my_idx);
	}



	/* ===========================================================
	 * scan the folder tree for EPS files to convert to PDF and optionally to TEXT
	 */
	public static int convert_EPS(int convert_func)
	{

		if (mf_tree == null) return(0);
		if (mf_tree.file_list_size() <= 0) return(0);

		int		my_idx = 0;
		boolean do_EPS2PDF = false;
		String	my_path = "";
		String	my_name = "";
		String	pdf_out = "";
		String	txt_out = "";
		int num_EPS_converted = 0;
		File	aFile;
		FileInputStream my_fstr = null;

		BatchXSLT.g_mainXSLTFrame.showMess( "---- Checking for EPS files to convert...\n" );

		for (my_idx = 0; my_idx < mf_tree.file_list_size(); my_idx++) {
 			if (BatchXSLT.g_mainXSLTFrame.general_abort == true) return(num_EPS_converted);

			my_path = mf_tree.get_file_path(my_idx);
			my_name = mf_tree.get_file_name(my_idx);
			if (my_path.equals("")) return(num_EPS_converted);

			BatchXSLT.g_mainXSLTFrame.EPSPDF_exported_path = "";	// remember these file to write into the XML file
			BatchXSLT.g_mainXSLTFrame.EPS_last_exported = "";		// remember these file to write into the XML file
			BatchXSLT.g_mainXSLTFrame.PDF_last_exported = "";		// remember these file to write into the XML file

			do_EPS2PDF = false;
			do {
				// check if a file may be processed
				if ( (mf_tree.get_file_type(my_idx) != 1) ) break;	//  this entry's type: 1 == file, 2 == dir
				if ( my_name.toUpperCase().startsWith(".DS_STORE") ) break;	// OSX dir file
				if ( my_name.startsWith("._") == true) break;				// hiddeen OSX files

				if (my_name.toUpperCase().startsWith("ICO") && (my_name.length() == 5)
					&& (my_name.endsWith("\r") || my_name.endsWith("?"))) {	// like this it may be seen on windows and macintosh
					break;
				}

				// ----------- Convert EPS to PDF
				if ( (convert_func & 1) == 1 ) {
					
					pdf_out = replace_filename_ext(my_name, ".pdf");
					if (my_path.endsWith(File.separator) == false)  my_path = my_path + File.separator;

					// check filename extension or ....
					if ( (my_name.toUpperCase().endsWith(".EPS") == true)
						|| (my_name.toUpperCase().endsWith(".EPSF") == true)
						|| (my_name.toUpperCase().endsWith(".PS") == true)
						 ) {
						 do_EPS2PDF = true;
					}
					else {	// .... check the file content if the name has NO extension
						if ( !has_filename_ext(my_name) ) { // check if it is a valid PS/EPS file
							do {
								// check if we can read this file
								my_fstr = null;
								try { my_fstr = new FileInputStream( my_path + my_name ); }
								catch ( FileNotFoundException e ) {	// uups could not read it - locked file?
									do_EPS2PDF = false; break;
								}

								// read the file
								byte mybuf[] = new byte[100];
								int numbytes = -1;
								try { numbytes = my_fstr.read( mybuf, 0, 50 ); }
								catch ( java.io.IOException e) {	// uups could not read it - invalid file
									do_EPS2PDF = false;
									try { my_fstr.close(); } catch ( java.io.IOException e1 ) {}
									break;
								}
								try { my_fstr.close(); } catch (java.io.IOException e) {}
								// check how many bytes could be read
								if ( numbytes <= 6 ) {			// not enough bytes read ?
									do_EPS2PDF = false;
									break;
								}
								// let's check if the %!PS signature is contained in our buffer
								for ( int i = 0; i < numbytes; i++) {	// check if we find '%!PS'
									if ( (mybuf[i] == '%') && (mybuf[i+1] == '!')
										&& (mybuf[i+2] == 'P') && (mybuf[i+3] == 'S') ) {
										do_EPS2PDF = true;	// YES - a valid postscript file
										break;
									}
								}
							} while(false);
						}
					}

					// check if PDF already exists if we should convert to PDF
					if ( do_EPS2PDF ) {
						aFile = new File( my_path + pdf_out );
						if ( aFile.exists() == true ) {
							do_EPS2PDF = false;
							pdf_out = "";	// clear the PDF name - extract text only if we actually converted EPS - PDF
						}
					}

					// do the EPS to PDF conversion					
					if (do_EPS2PDF) {
						int converted = BatchXSLTGhostScript.GS_eps2pdf(my_path + my_name, my_path + pdf_out, convert_func);
						// check if the PDF file exists and add it to the mf_tree
						// like this we don't have to re-scan the path after EPS to PDF conversion
						aFile = new File( my_path + pdf_out );
						if ( aFile.exists() == true ) {
							num_EPS_converted++;
							// ad to mf_tree
							if (my_idx < (mf_tree.file_list_size())) mf_tree.insertFile(my_path, pdf_out, my_idx + 1);	// insert after EPS
							else mf_tree.insertFile(my_path, pdf_out, my_idx);
							my_idx++;	// and skip this new entry
							BatchXSLT.g_mainXSLTFrame.EPSPDF_exported_path = my_path;	// remember these file to write into the XML file
							BatchXSLT.g_mainXSLTFrame.EPS_last_exported = my_name;	// remember these file to write into the XML file
							BatchXSLT.g_mainXSLTFrame.PDF_last_exported = pdf_out;	// remember these file to write into the XML file
						}
						else pdf_out = "";	// clear the PDF name - extract text only if we actually converted EPS - PDF
					}
					else pdf_out = "";	// clear the PDF name - extract text only if we actually converted EPS - PDF
					
				}

				// ----------- evtl. call the PDF to Text extractor
				if ( ((convert_func & 2) == 2)
					&& (pdf_out.equals("") == false) ) {	// did we recently really convert and EPS to PDF?

					txt_out = replace_filename_ext(pdf_out, ".tx1");
					String txt_outUTF = replace_filename_ext(pdf_out, ".xml");
				
					// check if xml Text file already exists
					aFile = new File( my_path + txt_outUTF );
					if ( aFile.exists() == true ) break;

					int converted = BatchXSLTGhostScript.GS_extract_TXT(my_path + pdf_out, my_path + txt_out, convert_func);
					if (converted == 0) {	// we have the text in ISO-8859-1 encoding. let's convert it to UTF-8
						converted = file_iso2utf(my_path + txt_out, my_path + txt_outUTF, 1);	// ... and delete txt_out file
						if ( converted == 0) {	// have written an xml file?
							// ad to mf_tree
							if (my_idx < (mf_tree.file_list_size())) mf_tree.insertFile(my_path, txt_outUTF, my_idx + 1);	// insert after PDF
							else mf_tree.insertFile(my_path, txt_outUTF, my_idx);
							my_idx++;	// and skip this new entry
						}
					}
				}


				break;
			} while(false);

		}

		BatchXSLT.g_mainXSLTFrame.showMess( "  -- EPS files converted: " + num_EPS_converted + "\n" );
		return(num_EPS_converted);
	}




	/* ===========================================================
	 * search for a file in any directory
	 * return:	the_found_type	== 0	: nothing found
	 * 							== 1	: found a valid XML file
	 * 							== 2	: dir found
	 * 							== 3	: found an invalid file (no XML)
	 * 							== 4x	: found a locked file - no write/read access
	 * 							== 5	: found a locked file - no delete access
	 * 							== 100	: found a file whose name extension is in the exclude list
	 */
	public static String searchForFileInDir(String thePath, int the_idx)
	{
		String		flist[];
		int			my_idx = 0;
		boolean		is_locked = false;
		File		filepath = new File( thePath );
		String		mypath = filepath.getAbsolutePath();


		the_found_file_type = 0;		// set to 'nothing found'
		if (BatchXSLT.g_mainXSLTFrame.general_abort == true) return("");

		// add a '/' to the path if not already there
		if (mypath.endsWith(File.separator) == false) mypath = mypath + File.separator;
		
		flist = filepath.list();
		
		my_idx = the_idx;
		
		while (true) {
			the_found_file_type = 0;		// set to 'nothing found'
 			if (BatchXSLT.g_mainXSLTFrame.general_abort == true) return("");
			g_numfiles_avail = flist.length;
			if (g_numfiles_avail <= 0) return ("");

			if (my_idx < 0) my_idx = 0;
			if ((my_idx >= 0) && (my_idx > (g_numfiles_avail - 1))) { return (""); }

			// check if we have a file named '##folderlocked' in this path - do not process this folder
			is_locked = has_lockfile(flist);
			if (is_locked) return ("");
 
 			if (is_in_exclude_list(flist[my_idx], processing_runtype)) return ("");

			// check if it is a folder or file which should process as last in XSLT run
			if ((g_numfiles_avail > 1) && (processing_runtype == 0)) {
				while (true) {
					if ( (flist[my_idx].toUpperCase().startsWith("DTDCSS") == true)
						|| (flist[my_idx].toUpperCase().startsWith("XSLCSS") == true)
						|| (flist[my_idx].toUpperCase().startsWith("XSL") == true)
						) {
						if (my_idx < (flist.length - 1)) { my_idx++; continue; }
						break;
					}
					break;
				}
			}

			// check if it is a file or a dir
			// make both checks: dir and file to check if the file may be accessed correctly
			// even on windows because the macintosh files 'Icon\r' may not be accessed!!
			File anyFile = new File( mypath + flist[my_idx] );
			if (anyFile.isDirectory() == true) {
				// check if we have a file named '##folderlocked' in this path - do not process this folder
				is_locked = has_lockfile(anyFile);
				if (is_locked) { my_idx++; continue; }	// try a next list item

				the_found_file_type = 2;	// found an unlocked dir
			}
			else {
				if (anyFile.isFile() == true) {
					the_found_file_type = 1;	// found a file
					// -------------------------------
					// check if it is an 'Icon\r' file - delete immediately and try to get next file
					// a '\r' in a file name will return a code 63 ('?') in the file list's name item (Windows NT)
					// the filenae looks like 'Ico + n with grave in the file list's name item (Windows 2000)
					if (flist[my_idx].startsWith("Ico") && ((flist[my_idx].length() == 5) || (flist[my_idx].length() == 4))
						&& (flist[my_idx].endsWith("\r") || flist[my_idx].endsWith("?"))) {	// like this it may be seen on windows and macintosh
						if (anyFile.delete()) BatchXSLT.g_mainXSLTFrame.showMess( "**** File 'Icon' deleted\n" );
						else BatchXSLT.g_mainXSLTFrame.showMess( "#### File 'Icon' could not be deleted!\n" );
						flist = filepath.list();	// have deleted something - get new filelist
						continue;
					}
				}
				else {
					try { Thread.sleep(200); }
					catch (InterruptedException e) {}
					anyFile = new File( mypath + flist[my_idx] );
					if (anyFile.exists() == false) continue;	// file has gone
					for (int i = 1; i < 10; i++) {	// do not just try to delete the file but wait some time
						try { Thread.sleep(200); }
						catch (InterruptedException e) {}
						anyFile = new File( mypath + flist[my_idx] );
						if (anyFile.exists() == false) break;	// ok - the file has gone for any reason
					}
					anyFile = new File( mypath + flist[my_idx] );
					if (anyFile.exists() == false) continue;	// file has gone
					int exit_Val = 0;
					if (isWindows()) {
						String[] cmd = {
							"del \"" + mypath + flist[my_idx] + "\" /a"
						};
						exit_Val = exec_windows_command(cmd);
						if (exit_Val == 0) BatchXSLT.g_mainXSLTFrame.showMess( "---- File (inaccessible) '" + flist[my_idx] + "' deleted!\n" );
					}
					else exit_Val = 100;
	 				if ((exit_Val != 0) && (anyFile.exists() == true)) {	// may be the file has gone in the meen time?
						BatchXSLT.g_mainXSLTFrame.showMess( "#### Invalid Filename Exception: '" + flist[my_idx] + "' -- The name evtl. contains invalid characters!\n" );
						BatchXSLT.g_mainXSLTFrame.showMess( "#### File may not be accessed in any way! Please remove it manually from the path:\n" );
						BatchXSLT.g_mainXSLTFrame.showMess( "####     '" + mypath + "'\n" );
						try { Thread.sleep(3000); } catch (InterruptedException ie) {} // ... and let us wait a bit
	 				}
					flist = filepath.list();	// may be the file has been deleted - get new filelist
					continue;
				}
			}
			break;
		}

		// if it is a dir - return the name
		if (the_found_file_type == 2) return(flist[my_idx]);

		// do some checks on the found file
		if (the_found_file_type == 1) {
			do {

				// -------------------------------
				// check if we have a file name extensions list for filename to be excluded from being processed
				boolean exclude = is_in_exclude_list(flist[my_idx], processing_runtype);
				if (exclude == true) {
					the_found_file_type = 100;
					break;
				}


				// -------------------------------
				// check if we can access it for writing (no other process is writing to it)
			/*  this is always false on windows systems
				if (anyFile.canWrite() == false) {	// uups - no file writing allowed - still open?
					the_found_file_type = 49;
					break;
				} */

				// -------------------------------
				// check if we have matching file access rights
				if (BatchXSLT.mySecMan != null) {	// if a SecurityManager is installed only
   					try { BatchXSLT.mySecMan.checkWrite( mypath + flist[my_idx] ); }
   					catch ( SecurityException e ) {
						the_found_file_type = 42;
						break;
   					}
					try { BatchXSLT.mySecMan.checkDelete( mypath + flist[my_idx] ); }
   					catch ( SecurityException e ) {
						the_found_file_type = 52;
						break;
   					}
				}

				// -------------------------------
				// check if we can write to this file
				FileWriter mywriter = null;
				try { mywriter = new FileWriter( mypath + flist[my_idx], true );
					  mywriter.close();
				}
				catch ( java.io.IOException e ) {	// uups could not read it - locked file
					the_found_file_type = 43; break;
				}

				// -------------------------------
				// check if we can read this file
				FileInputStream mystream = null;
				try { mystream = new FileInputStream( mypath + flist[my_idx] ); }
				catch ( FileNotFoundException e ) {	// uups could not read it - locked file?
					the_found_file_type = 44; break;
				}
				// check how many bytes can be read
				try {
					int numbytes = mystream.available();
					if ( numbytes <= 10 ) {			// not enough bytes avail - is open?
						the_found_file_type = 45;
						mystream.close();
						break;
					}
				}
				catch ( java.io.IOException e ) {	// uups could not read it - locked file?
					the_found_file_type = 46;
					try { mystream.close(); }
					catch ( java.io.IOException e1 ) {}
					break;
				}

				// -------------------------------
				// check if it is a valid XML file
				byte mybuf[] = new byte[100]; 
				try { mystream.read( mybuf, 0, 50 ); }
				catch ( java.io.IOException e) {	// uups could not read it - invalid file
					the_found_file_type = 3; break;
				}
				try { mystream.close(); }
				catch (java.io.IOException e) {}
				// let's check if the XML signature is contained in our buffer
				the_found_file_type = 3;	// reinit to an invalid file
				for ( int i = 0; i < 50; i++) {	// check if we find '<?xml'
					if ( (mybuf[i] == '<') && (mybuf[i+1] == '?')
						&& (mybuf[i+2] == 'x') && (mybuf[i+3] == 'm') && (mybuf[i+4] == 'l') ) {
						the_found_file_type = 1;	// YES - a valid XML file
						// but now check if it is an XSL file!!!
						if ((flist[my_idx].endsWith(".xsl") == true) || (flist[my_idx].endsWith(".XSL") == true)) {
							the_found_file_type = 3; break;
						}
						break;
					}
				}
				// we came to this point - it should be a valid XML file
			} while (false);
		}
		
		return(flist[my_idx]);
	}
	



	/* ===========================================================
	 * return/prepare the path/name from a file from the file tree
	 * return:	the_found_file_type	== 0	: nothing found
	 * 								== 1	: found a valid XML file
	 * 								== 2	: dir found
	 * 								== 3	: found an invalid file (no XML)
	 * 								== 4	: found an IDMLzipped (ODF)  package file
	 * 								== 42	: security problem with file
	 * 								== 43	: found a locked file - no write access
	 * 								== 44	: found a locked file - no read access
	 * 								== 45	: not enough bytes in file
	 * 								== 46	: error reading file
	 * 								== 47	: found a locked file - no delete access
	 * 								== 100	: found a file whose name extension is in the exclude list
	 * 			my_InputPath		== the parent path
	 * 			my_InputName		== the dir or file name
	 */
	public static int get_file_from_tree(int the_idx)
	{
		int			my_idx = 0;
		boolean		is_locked = false;
		File		anyFile = null;
		xmlsrc_version =  xmlsrc_encoding = xmlsrc_doctype_declaration = "";


		my_idx = the_idx;
		while (true) {
			the_found_file_type = 0;		// set to 'nothing found'
			my_InputPath = "";
			my_InputName = "";
			if (mf_tree == null) return(0);
			if (BatchXSLT.g_mainXSLTFrame.general_abort == true) return(0);
			g_numfiles_avail = file_tree_size();
			if (g_numfiles_avail <= 0) return (0);

			if (my_idx < 0) my_idx = 0;
			if (my_idx >= g_numfiles_avail) return (0);

			int ftype = mf_tree.get_file_type(my_idx);	// is 1==file or 2==dir ?
			my_InputPath = mf_tree.get_file_path(my_idx);
			my_InputName = mf_tree.get_file_name(my_idx);

			if (ftype == 2) {	// if is a dir:
				the_found_file_type = ftype;		// set to 'nothing found'
				// trunc last path - this is the dir's name
				if (my_InputPath.endsWith(my_InputName) == true) my_InputPath = my_InputPath.substring(0,my_InputPath.length() - my_InputName.length());
				if (my_InputPath.endsWith(File.separator) == false) my_InputPath += File.separator;

				// check if we have a file named '##folderlocked' in this path - do not process this folder
				is_locked = mf_tree.is_locked_folder(my_idx);

				if (!is_locked) {	// check if we have a file name extensions list for filename to be excluded from being processed
					is_locked = is_in_exclude_list(my_InputName, processing_runtype);
				}
				if (is_locked) {	// lets skip to the next folder in the list which has no '## folderlocked' file
					my_idx++;	// inc file index pointer and go next unlocked dir in tree list
					while (my_idx < g_numfiles_avail) {
						if ((mf_tree.get_file_type(my_idx) == 2) && (mf_tree.is_locked_folder(my_idx) == false)) break;
						my_idx++;	// inc local file index pointer and get next file from tree list
					}
					if (my_idx >= g_numfiles_avail)	{	// reached end of list
						the_found_file_type = 0;		// set to 'nothing found'
						my_InputPath = ""; my_InputName = "";
						searchFileIdx = my_idx;	// inc global file index pointer
						return(0);
					}
					searchFileIdx = my_idx;	// inc global file index pointer
					continue;
				}
				
				// no further test if we are in cleanup run
				if ( processing_runtype != 0) break;
				
				//String fp = my_InputPath; if (fp.endsWith(File.separator) == false) fp = fp + File.separator; fp += my_InputName;
				//BatchXSLT.g_mainXSLTFrame.showMess("File type: " + ftype + ", copy: " + BatchXSLT.g_mainXSLTFrame.includeXSLCSSfolderCopy + " -> " + fp + "\n");
				
				// check if we do file copy only (like for FTP copyFile())
				if ( BatchXSLT.g_mainXSLTFrame.xslPathName.equals("*NONE*") == true ) {	// folder copy only
					if ( BatchXSLT.g_mainXSLTFrame.includeXSLCSSfolderCopy > 0) break;	// make copy/transfer
				}
				
				// check if it is a folder which should XSL process as last item in this dir
				if ( (my_InputPath.toUpperCase().indexOf("XSLCSS") >= 0) 
					&& ( (my_InputName.toUpperCase().startsWith("DTDCSS") == true)
						|| (my_InputName.toUpperCase().startsWith("XSLCSS") == true)
						|| (my_InputName.toUpperCase().startsWith("XSL") == true) )
					) {
					String flist[];
					File f = new File(my_InputPath);
					flist = f.list();
					if (	(flist.length > 1)	// we have still more files in this dir than only the 'dtdcss'
							|| (is_in_exclude_list(my_InputName, processing_runtype) == true)
						) {
						my_idx++;	// inc global file index pointer and go next unlocked dir in tree list
						while (my_idx < g_numfiles_avail) {
							if (mf_tree.get_file_type(my_idx) == 2) break;
							my_idx++;	// inc local file index pointer and get next file from tree list
						}
						searchFileIdx = my_idx;	// inc global file index pointer too
						continue;
					}
				}
			}
			if (my_InputPath.endsWith(File.separator) == false) my_InputPath += File.separator;

			// check if it is a file or a dir
			// make both checks: dir and file to check if the file may be accessed correctly
			// even on windows because the macintosh files 'Icon\r' may not be accessed!!
			anyFile = new File( my_InputPath + my_InputName );
			if (anyFile.exists() == false) { my_idx++; continue; }	// file has gone - ignore this

			// check if we do file copy only (like for FTP copyFile())
			if ( BatchXSLT.g_mainXSLTFrame.xslPathName.equals("*NONE*") == true ) {	// folder copy only. do we have to exclude XSLCSS folder?
				if ( ( BatchXSLT.g_mainXSLTFrame.includeXSLCSSfolderCopy <= 0)
						&& ((my_InputPath + my_InputName).toUpperCase().indexOf("XSLCSS") >= 0)
					) { my_idx++; continue; };	// do not copy/transfer
			}
			
			if (anyFile.isDirectory() == true) {
				// check if we have a file named '##folderlocked' in this path - do not process this folder
				is_locked = has_lockfile(anyFile);
				if (is_locked) { my_idx++; continue; }	// try a next list item

				the_found_file_type = 2;	// found an unlocked dir
			}
			else {
				if (anyFile.isFile() == true) {
					the_found_file_type = 1;	// found a file
					// -------------------------------
					// check if it is an 'Icon\r' file - delete immediately and try to get next file
					// a '\r' in a file name will return a code 63 ('?') in the file list's name item
					if (my_InputName.toUpperCase().startsWith("ICO") && (my_InputName.length() == 5)
						&& (my_InputName.endsWith("\r") || my_InputName.endsWith("?"))) {	// like this it may be seen on windows and macintosh
						boolean deleted = anyFile.delete();
						if (!deleted) BatchXSLT.g_mainXSLTFrame.showMess( "#### File 'Icon' could not be deleted!\n" ); 
						// SILENT!! else BatchXSLT.g_mainXSLTFrame.showMess( "**** File 'Icon' deleted\n" );
						searchFileIdx = ++my_idx;	// inc global file index pointer and get next file from tree list
						continue;
					}
					if ( (my_InputName.toUpperCase().startsWith(".DS_STORE"))	// OSX dir file
						|| (my_InputName.startsWith("._") == true)				// hiddeen OSX files
						) {
						boolean deleted = anyFile.delete();
						if (!deleted) BatchXSLT.g_mainXSLTFrame.showMess( "#### Hidden file '" + my_InputName + "' could not be deleted!\n" );
						// SILENT!! else BatchXSLT.g_mainXSLTFrame.showMess( "**** File '" +  my_InputPath + my_InputName + "' deleted\n" );
						searchFileIdx = ++my_idx;	// inc global file index pointer and get next file from tree list
						continue;
					}
				}
				else {
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					if (anyFile.exists() == false) { my_idx++; continue; }	// file has gone
					for (int i = 1; i < 10; i++) {	// do not just try to delete the file but wait some time
						try { Thread.sleep(200); }
						catch (InterruptedException e) {}
						if (anyFile.exists() == false) break;	// ok - the file has gone for any reason
					}
					if (anyFile.exists() == false) { my_idx++; continue; }	// file has gone
					int exit_Val = 0;
					if (isWindows()) {
						String[] cmd = {
							"del \"" + my_InputPath + my_InputName + "\" /a"
						};
						exit_Val = exec_windows_command(cmd);
						if (exit_Val == 0) BatchXSLT.g_mainXSLTFrame.showMess( "---- File (inaccessible) '" + my_InputName + "' deleted!\n" );
					}
					else exit_Val = 100;
	 				if (exit_Val != 0) {
						BatchXSLT.g_mainXSLTFrame.showMess( "#### Invalid Filename Exception: '" + my_InputName + "' -- The name evtl. contains invalid characters!\n" );
						BatchXSLT.g_mainXSLTFrame.showMess( "#### File may not be accessed in any way! Please remove it manually from the path:\n" );
						BatchXSLT.g_mainXSLTFrame.showMess( "####     '" + my_InputPath + "'\n" );
						try { Thread.sleep(3000); } catch (InterruptedException ie) {} // ... and let us wait a bit
	 				}
					searchFileIdx++;	// inc global file index pointer and get next file from tree list
					my_idx = searchFileIdx;
					continue;
				}
			}
			break;
		}

		// if it is a dir - return the name
		if (the_found_file_type == 2) return(the_found_file_type);

		// do some checks on the found file
		if (the_found_file_type == 1) {
			do {
				// check if we have a file name extensions list for filename to be excluded from being processed
				boolean exclude = is_in_exclude_list(my_InputName, processing_runtype);
				if (exclude == true) {
					the_found_file_type = 100;
					break;
				}


				// -------------------------------
				// check if we can access it for writing (no other process is writing to it)
				if (!isWindows()) {
					// this is always false on windows systems
					if (anyFile.canWrite() == false) {	// uups - no file writing allowed - still open?
						the_found_file_type = 49;
						break;
					}
				}

				// -------------------------------
				// check if we have matching file access rights
				if (BatchXSLT.mySecMan != null) {	// if a SecurityManager is installed only
   					try { BatchXSLT.mySecMan.checkWrite( my_InputPath + my_InputName ); }
   					catch ( SecurityException e ) {
						the_found_file_type = 42;
						break;
   					}
					try { BatchXSLT.mySecMan.checkDelete( my_InputPath + my_InputName ); }
   					catch ( SecurityException e ) {
						the_found_file_type = 47;
						break;
   					}
				}

				// -------------------------------
				// check if we can write to this file
				FileWriter mywriter = null;
				try { mywriter = new FileWriter( my_InputPath + my_InputName, true );
					  mywriter.close();
				}
				catch ( java.io.IOException e ) {	// uups could not write it - locked file
					the_found_file_type = 43; break;
				}

				// -------------------------------
				// check if we can read this file
				FileInputStream mystream = null;
				try { mystream = new FileInputStream( my_InputPath + my_InputName ); }
				catch ( FileNotFoundException e ) {	// uups could not read it - locked file?
					the_found_file_type = 44; break;
				}
				// check how many bytes can be read
				try {
					int numbytes = mystream.available();
					if ( numbytes < 1 ) {			// not enough bytes avail - is open?
						the_found_file_type = 45;
						mystream.close();
						break;
					}
				}
				catch ( java.io.IOException e ) {	// uups could not read it - locked file?
					the_found_file_type = 46;
					try { mystream.close(); }
					catch ( java.io.IOException e1 ) {}
					break;
				}

				// -------------------------------
				// check if it is a valid XML file
				byte mybuf[] = new byte[1000]; 
				try { mystream.read( mybuf, 0, 990 ); }
				catch ( java.io.IOException e) {	// uups could not read it - invalid file
					the_found_file_type = 3; break;
				}
				try { mystream.close(); }
				catch (java.io.IOException e) {}

				// check if it is an IDML package file
				if ( my_InputName.toLowerCase().endsWith(".idml") ) {	// idml ODF (zip) archive
					the_found_file_type = 4;
					break;
				}
				
				// let's check if the XML signature is contained in our buffer
				the_found_file_type = 3;	// reinit to an invalid file
				// nothing but white-space chars may be before <?xml.....
				//	' '  blank ,  '\t' tab,  '\n' new line,  '\f' form feed,  '\r' carriage return
				// check if it is an UTF-16 file: optionally with two leading bytes FF FE :BOM
				int bom = 0;
				int doublebyte = 0;
				if ( (mybuf[0] == 0xFF) && (mybuf[1] == 0xFE)) { bom = 2; }
				if ( (mybuf[3] == 0x00) ) { doublebyte = 1; }
				for ( int i = 0+bom; (i<150) && (i < (mybuf.length-6)); i++) {	// check if we find '<?xml' (dont test bejond 150 because JPEGS may have an xml header somewhere)
					///////if ( (mybuf[i] != '<') && (!Character.isWhitespace(mybuf[i+1])) && (!Character.isSpaceChar(mybuf[i+1])) ) break;	// illegal chars

					if ( (mybuf[i] == '<') && (mybuf[i+1+doublebyte] == '?')
						&& (mybuf[i+2+(2*doublebyte)] == 'x') && (mybuf[i+3+(3*doublebyte)] == 'm') && (mybuf[i+4+(4*doublebyte)] == 'l') ) {
						the_found_file_type = 1;	// YES - a valid XML file
						// but now check if it is an XSL file!!!
						if (my_InputName.toUpperCase().endsWith(".XSL") == true) {
							the_found_file_type = 3; break;
						}
						break;
					}
				}
				// if it is not an xml file, let's check if the XHTML signature is contained in our buffer
				// like this <html xmlns="http://www.w3.org/1999/xhtml">
				if (the_found_file_type == 3) {
					for ( int i = 0; i < (mybuf.length-6); i++) {	// check if we find ' xmlns=' or ' xmlns '
						if ( (mybuf[i] == ' ') && (mybuf[i+1] == 'x') && (mybuf[i+2] == 'm') && (mybuf[i+3] == 'l')
							&& (mybuf[i+4] == 'n') && (mybuf[i+5] == 's')
							&& ((mybuf[i+6] == ' ') || (mybuf[i+6] == '='))
							 ) {
							the_found_file_type = 1;	// YES - this could be any type of XML file like xhtml
							break;
						}
					}
				}

				//BatchXSLT.g_mainXSLTFrame.showMess(" ------- file: " + my_InputName + ", file type: " + the_found_file_type + "\n");

				// we came to this point - it should be a valid XML file
				if (the_found_file_type == 1) {
						// it seems to be an xml file - retrieve the 'version' and 'encoding' attributes
						// the two values will be sent as parameters to the xsl
					String xml_declaration = new String (mybuf); // like  <?xml version="1.0" encoding="iso-8859-1"?>
					int version_idx = xml_declaration.indexOf("version");
					if (version_idx < 0) version_idx = xml_declaration.indexOf("VERSION");
					if (version_idx >= 0) {	// have to extract the 'version' attribute
						int beginIndex = xml_declaration.indexOf("\"",version_idx+7);
						int endIndex = xml_declaration.indexOf("\"",beginIndex+1);
						xmlsrc_version = xml_declaration.substring(beginIndex+1,endIndex);
					}
					int encoding_idx = xml_declaration.indexOf("encoding");
					if (encoding_idx < 0) encoding_idx = xml_declaration.indexOf("ENCODING");
					if (encoding_idx >= 0) {	// have to extract the 'encoding' attribute
						int beginIndex = xml_declaration.indexOf("\"",encoding_idx+8);
						int endIndex = xml_declaration.indexOf("\"",beginIndex+1);
						xmlsrc_encoding = xml_declaration.substring(beginIndex+1,endIndex);
					}
					
					// now we retrieve the <!DOCTYPE declaration string like:
					// version a)
					// <!DOCTYPE document SYSTEM "../DTDCSS/XTXPe.dtd" [
					//		<!ENTITY % xtxpe PUBLIC "" "../DTDCSS/XTXPe.ent">
					//		%xtxpe;
					// ]>
					// or version b)
					// <!DOCTYPE document SYSTEM "../DTDCSS/XTXPe.dtd" >
					int doctype_idx = xml_declaration.indexOf("<!DOCTYPE");
					if (doctype_idx >= 0) {	// have to extract the '<!DOCTYPE' string from <! to last '>'
						// first we check if we have a version a) declaration <!DOCTYPE...[...]>
						int endIndex = xml_declaration.indexOf("]>",doctype_idx);
						if (endIndex < 0) {
							// check if we have a version b) declaration <!DOCTYPE...>
							endIndex = xml_declaration.indexOf(">",doctype_idx);
							endIndex++;
						}
						else endIndex += 2;
						if (endIndex >= 0) {
							xmlsrc_doctype_declaration = xml_declaration.substring(doctype_idx,endIndex);
						}
					}


				}
			} while (false);
		}
		
		return(the_found_file_type);
	}



	/* ===========================================================
	 * check if a file name is in the exclude list
	 * processing_runtype:	0 = process XML files only
	 *                      1 = do cleanup
	 */
	public static boolean is_in_exclude_list(String the_name, int processing_runtype)
	{
		String myname = the_name.toLowerCase();
		String flExtsStr = "", flNamesStr = "";

		switch (processing_runtype) {
			case 0:	{ // first run: XSL processing
				flExtsStr = BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNameExts.toLowerCase();
				flNamesStr = BatchXSLT.g_mainXSLTFrame.excludeSourceProcessingRunFileNames.toLowerCase();
				if ((flExtsStr.equals("") == true) && (flNamesStr.equals("") == true)) return(false);	// lists are empty
				break;
				}
			case 1:	{ // second run: Clean UP processing
				if (BatchXSLT.g_mainXSLTFrame.excludeCleanupRunFileNameExts.equals("") == true) return(false);	// list is empty
				flExtsStr = BatchXSLT.g_mainXSLTFrame.excludeCleanupRunFileNameExts.toLowerCase();
				break;
				}
			default: return(false);
		}
		int li, cpos;
		int maxExts = 100;
		String flExts[] = new String[maxExts];
		for (li = 0; li < maxExts; li++) flExts[li] = "";
		String flNames[] = new String[maxExts];
		for (li = 0; li < maxExts; li++) flNames[li] = "";

        // read in the comma separated name extensions into the 'flExts' list
		li = 0;
		do {
			cpos = flExtsStr.indexOf(",");
			if (cpos < 0) {	// not found: is last entry in comma separated list
				flExts[li] = flExtsStr;
				break;
			}
			else {
				// get the first entry
				flExts[li] = flExtsStr.substring(0, cpos);
				if (flExts[li].equals("") == false) {	// ignore empty extensions in the list string 'xxx,,yyy'
					li++;
				}
				// cut the entry
				flExtsStr = flExtsStr.substring(cpos + 1, flExtsStr.length());
			}
		} while(li < maxExts);

        // read in the comma separated name extensions into the 'flNames' list
		li = 0;
		do {
			cpos = flNamesStr.indexOf(",");
			if (cpos < 0) {	// not found: is last entry in comma separated list
				flNames[li] = flNamesStr;
				break;
			}
			else {
				// get the first entry
				flNames[li] = flNamesStr.substring(0, cpos);
				if (flNames[li].equals("") == false) {	// ignore empty extensions in the list string 'xxx,,yyy'
					li++;
				}
				// cut the entry
				flNamesStr = flNamesStr.substring(cpos + 1, flNamesStr.length());
			}
		} while(li < maxExts);
		
		// now check the flExts entries with the filename
		li = 0;
		while (li < maxExts) {
			if (flExts[li].equals("") == true) break;
			if (myname.endsWith(flExts[li]) == true) return(true);
			li++;
		}
		
		// now check the flExts entries with the filename
		li = 0;
		while (li < maxExts) {
			if (flNames[li].equals("") == true) break;
			if (myname.startsWith(flNames[li]) == true) return(true);
			li++;
		}

	
		return(false);
	}



	/* ===========================================================
	 * check actions to perform after Transform is done
	 */
	public static int doSourceFileActions(String the_path, String the_name, String the_outsubpath)
	{
		if ( BatchXSLT.g_mainXSLTFrame.sourceFileAction.equals("") == true) return(0);	// nothing to do

		int		myErr = 0;
		boolean	delete_it = false;
		String my_inpath = "";
		String my_inname = "";
		String my_outpath = BatchXSLT.g_mainXSLTFrame.sourceFileAction;
		if (get_URL_file_type(my_outpath) >= 2)	{	// http or ftp path
			if (my_outpath.endsWith("/") == false) my_outpath += "/";
		}
		else {	// a local path
			if (my_outpath.endsWith(File.separator) == false) my_outpath += File.separator;
		}
		String myoutsubpath = the_outsubpath;
		if ((myoutsubpath.length() > 0) && (myoutsubpath.endsWith(File.separator)) == false) myoutsubpath += File.separator;


		if (isMacintosh() == true) {
			my_inpath = makeURLString(the_path);
			my_inname = makeURLString(the_name);
		}
		else {
			my_inpath = the_path;
			my_inname = the_name;
		}


		do {
			// -------------------------------
			// check if we have to move it
			if ( (BatchXSLT.g_mainXSLTFrame.sourceFileAction.equals("") == false)
				&& (BatchXSLT.g_mainXSLTFrame.sourceFileAction.equals("*DELETE*") == false) ) {	// move the file to the given path
		 		BatchXSLT.g_mainXSLTFrame.showMess("Moving Source file: '" + my_inname + "' to '" + (my_outpath + myoutsubpath) + "'... ");
				myErr = copyFile(my_inpath, my_inname, my_outpath + myoutsubpath, "", 1,false,true);
	 			if (myErr == 0) {
					BatchXSLT.g_mainXSLTFrame.showMess("done.\n");
					delete_it = true;
				}
				else {
					BatchXSLT.g_mainXSLTFrame.showMess("### ERROR " + myErr + "\n");
					delete_it = true;
				}
			}

			// -------------------------------
			// check if we have to delete it
			if ((delete_it == true) || (BatchXSLT.g_mainXSLTFrame.sourceFileAction.compareTo("*DELETE*") == 0)) {	// delete the source  file
				boolean retflag = true, nopermission = false;
				int retry_cnt;
				boolean was_directory = false;
				try {
					File mytmpFile = new File(my_inpath + my_inname);
					if (!mytmpFile.exists()) {
	 					//BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR: Source file to delete '" + my_inpath + my_inname + "' not found!\n");
	 					// the file has gone for any reason - OK
						return(0);
					}
					// we will try to delete a dir only if it is empty
					if (mytmpFile.isDirectory() == true) {
						String	flist[];
						flist = mytmpFile.list();
						if (flist.length > 0) break;	// not empty dir
						was_directory = true;
					}

					// ok let's try to delete it
					for (retry_cnt = 0; retry_cnt < 5; retry_cnt++ ) {
						retflag = mytmpFile.delete();
						if (retflag == true) break;		// ok - deleted
						//-- cleanUpMemory();
					}
					if (retflag) {
						if (delete_it == false) {
							if (was_directory == true) BatchXSLT.g_mainXSLTFrame.showMess("Folder '" + my_inpath + my_inname + "' deleted.\n");
							else BatchXSLT.g_mainXSLTFrame.showMess("Source file '" + my_inpath + my_inname + "' deleted.\n");
						}
						break;
					}
	 				else {
	 					BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR: Source file '" + my_inpath + my_inname + "' could not be deleted!\n");
	 					myErr = 1;
	 				}
				}
		 		catch (SecurityException e) {
		 			BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR: no permission to delete Source file '" + my_inpath + my_inname + "'\n");
	 				myErr = 2;
		 		}
	 			break;
			}


		} while(false);
		return(myErr);
	}




	/* ===========================================================
	 * clean a string to a proper URL
	 */
	public static String makeURLString(String the_string)
	{
		String	new_string = "";
		int		j = 0, i = 0;
		char	c, c1;
		int		slen = 0;
		boolean	replace = false;
		String	replace_str = "%";
		
		slen = the_string.length();
		for (i = 0; i < slen; i++ ) {
			c = the_string.charAt(i);
			if (c >= '\u0080') c1 = '\u0080';
			else c1 = c;
			switch (c1) {
				case ' ':
				case '#':
				//case '%':			NEIN nicht wird evtl. schon vom chooser eingesetzt!
				case '?':
				case 128:
					replace_str += char2hex(c);
					replace = true;
					break;
				default:	// a good char
					new_string += c;
					break;
			}
			if (replace == true) {
				new_string += replace_str;
				replace = false;
				replace_str = "%";
			}
		}

		return(new_string);
	}




	/* ===========================================================
	 * convert a char to its hex code
	 */
	public static String char2hex(char c)
	{
		char	format[] = {'0','0'};
		char	conv[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		int		i;
		int		n = Integer.valueOf(c);
		
		for (i = 0; i < 2; n >>= 4, ++i)
			format[1-i] = conv[n & 0xF];
		
		return(String.copyValueOf(format));
	}

	/* ===========================================================
	 * convert a byte to its hex code
	 */
	public static String byte2hex(byte b)
	{
		char	format[] = {'0','0'};
		char	conv[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		int		i;
		int		n = Integer.valueOf(b);
		
            for (i = 0; i < 2; n >>= 4, ++i)
                    format[1-i] = conv[n & 0xF];

            return(String.copyValueOf(format));
	}


	/* ===========================================================
	 * convert a string to hex codes string
	 */
	public static String stringUTF8_2hex(String s)
	{
		if (s.equals("") == true) return(s);
		byte[] b;
		try {
			b = s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException ex) {
			return(s);
		}
		String sh = "";
		int i;

		for (i = 0; i < b.length; ++i)
				sh += byte2hex(b[i]);	// returns like 6162 for 'ab'

		return(sh);
	}





	static class thePutFileThread extends Thread {
         String localFileName;
		 String remoteFileName;
         thePutFileThread(final String localFileName, final String remoteFileName) {
             this.localFileName = localFileName;
             this.remoteFileName = remoteFileName;
         }
 
         public void run() {
			Thread.currentThread().setName("thePutFileThread");
			putFileThread_isrunning = true;
			boolean success = false;	
			success = copyFile_ftpClient.storeFile(localFileName, remoteFileName);

			putFileThread_returnvalue = success;
			putFileThread_isrunning = false;
         }
     }
 
	/* ===========================================================
	 * start the FTP putFile method as a thread
	 */
	static boolean putFileThread_isrunning = false;
	static boolean putFileThread_returnvalue = false;
	static boolean putFileThread(String localFileName, String remoteFileName) {
		Thread pft = new thePutFileThread(localFileName, remoteFileName);
		pft.start();
		// and now wait until file is transferred
		long max_sleep = 8400000;	// 140 minutes max should be enough for a 500 MB file at transfer rate of 65 KB/sec
									// 7 min for 25MB at 65 KB/sec
		long total_sleep = 0;
		long sleep_interval = 300;	// first time wait longer
		while (total_sleep < max_sleep) {
			try {
				pft.join(sleep_interval);
				sleep_interval = 30;
				if (pft.isAlive()) { // FTP has not finished
					total_sleep += sleep_interval;
					continue;
				}
				break; // Finished
			}
			catch (InterruptedException e) {
				java.lang.System.out.println("##### FTP Transfer EXCEPTION: " + e);
				java.lang.System.out.println("      for local file: '" + localFileName + "'\n" );
				java.lang.System.out.println("      to remote file: '" + remoteFileName + "'\n" );
				break;
			}
		}
//		java.lang.System.out.println( "  FTP Tranfer complete, file: '" + localFileName + "'\n" );
		return (putFileThread_returnvalue);
	}


	/* ===========================================================
	 * copy a file
	 */
	public static boolean DEBUG_COPYFILE = false;
	static com.epaperarchives.batchxslt.ftp.ftpClient copyFile_ftpClient = null;
	static int copyFile_ftpClientID = -1;
	static boolean copyFile_ftpClient_connected = false;
	public static boolean get_copyFile_connected() {
		return copyFile_ftpClient_connected;
	}
	public static void copyFile_disconnect() {
		if (copyFile_ftpClient == null) {
			copyFile_ftpClient_connected = false;
			return;
		}
		if (copyFile_ftpClient_connected == false) return;
		copyFile_ftpClient.disconnect();
		copyFile_ftpClient_connected = false;
		copyFile_ftpClient = null;
	}
	public static boolean set_DEBUG_COPYFILE(boolean bol) {
		DEBUG_COPYFILE = bol;
		return DEBUG_COPYFILE;
	}

	public static int copyFile(String the_sourcepath, String the_sourcename, String the_targetpath, String the_targetname, int create_target_path, boolean give_message) {
		return(copyFile(the_sourcepath, the_sourcename, the_targetpath, the_targetname, create_target_path, give_message, true));
	}
	public static int copyFile(String the_sourcepath, String the_sourcename, String the_targetpath, String the_targetname, int create_target_path, boolean give_message, boolean disconnect)
	{
		int					err = 0;
		int					ftp_err = 0;
		String				err_txt = "";
		int					lastFTPerr = 0;
		String				lastFTPerr_txt = "";
		URL					out_url;
		int 				result_type = 0;			// the type of the target to handle
		int					resp = -1;
		boolean				success;
		String				ftpType = "ftp";
		
		String				my_sourcepath = the_sourcepath;
		String				my_sourcename = the_sourcename;
		String				my_targetpath = the_targetpath;
		String				my_targetname = the_targetname;
		FileInputStream		my_instream = null;
		FileOutputStream	my_outstream = null;
		int					buflen = 65536;
		byte				mybuf[] = new byte[buflen];
		int					bytes_read = 0;
		int					total_bytes = 0;
		
		if (my_sourcepath.equals("") == true) return(-1);
		if (my_sourcename.equals("") == true) { }
		if (my_targetpath.equals("") == true) {
			my_targetpath = my_sourcepath;
		}
		if (my_targetname.equals("") == true) {
			my_targetname = my_sourcename;
		}

		if (DEBUG_COPYFILE) {
			BatchXSLT.g_mainXSLTFrame.showMess("==== copyFile ==============================\n");
			BatchXSLT.g_mainXSLTFrame.showMess("====     the_sourcepath: " + the_sourcepath + "\n");
			BatchXSLT.g_mainXSLTFrame.showMess("====     the_sourcename: " + the_sourcename + "\n");
			BatchXSLT.g_mainXSLTFrame.showMess("====     the_targetpath: " + the_targetpath + "\n");
			BatchXSLT.g_mainXSLTFrame.showMess("====     the_targetname: " + the_targetname + "\n");
			BatchXSLT.g_mainXSLTFrame.showMess("====     create_target_path: " + create_target_path + "\n");
			BatchXSLT.g_mainXSLTFrame.showMess("====     give_message: " + give_message + "\n");
			BatchXSLT.g_mainXSLTFrame.showMess("====     disconnect: " + disconnect + "\n");
			BatchXSLT.g_mainXSLTFrame.showMess("============================================\n");
		}

		// check if target file would overwrite source file
		if (my_sourcepath.equals(my_targetpath) == true) {
			if (my_sourcename.equals(my_targetname) == true) return(0);	// file in same dir: do nothing
		}

		// if 'my_sourcename' is a dir we have not to copy any content
		File aFile = new File(my_sourcepath+my_sourcename);
		if (aFile.isDirectory()) return(0);

		// determine the result type to open the target
		result_type = 0;
		result_type = get_URL_file_type(my_targetpath+my_targetname);	// local file or url?
		switch (result_type) {
			case 2:	// an ftp URI - connect and PUT
		 		my_targetpath = my_targetpath.replace('\\','/');
		 		my_targetname = my_targetname.replace('\\','/');
				if (my_targetpath.startsWith("ftps://")) {
					ftpType = "ftps";
					my_targetpath = MyRegex.replace("ftps://",my_targetpath,"ftp://");
				}

				try { out_url = new URL(my_targetpath+my_targetname); }
				catch ( java.net.MalformedURLException e ) {		// uups could not create target?
					if (give_message) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR FTP in copyFile: " + my_sourcepath+my_sourcename + " -> " + my_targetpath+my_targetname + "\n");
					err = -20; ftp_err = 0; err_txt = "Malformed URL";
					break;
				}
				try {
					if (copyFile_ftpClient_connected == false) {
						// create ftpClient
		 				if (give_message) BatchXSLT.g_mainXSLTFrame.showMess("*** FTP connecting to remote host: " + out_url.toString() + "\n");
						copyFile_ftpClient = com.epaperarchives.batchxslt.ftp.newFTPClient();
						if (copyFile_ftpClient == null) { err = -21; err_txt = "### ERROR FTP: Could not create FTP client"; break; }
						copyFile_ftpClientID = com.epaperarchives.batchxslt.ftp.getFTPClientID(copyFile_ftpClient);
					
						// set options
						copyFile_ftpClient.setOption("type", ftpType);
						copyFile_ftpClient.setOption("controlEncoding", mainXSLTFrame.ftpEncoding);
						copyFile_ftpClient.setOption("activePassive", mainXSLTFrame.ftpActivePassive);

						copyFile_ftpClient.setOption("server", out_url.getHost());
						String theport = "21";
						if (out_url.getPort() != -1) theport = "" + out_url.getPort();
						else {
							if (mainXSLTFrame.ftpPort.equals("") == false) theport = mainXSLTFrame.ftpPort;
						}
						copyFile_ftpClient.setOption("port", theport);
						if (DEBUG_COPYFILE) {
							BatchXSLT.g_mainXSLTFrame.showMess( "+++++ copyFile setOption 'type': " + out_url.getProtocol() + "\n");
							BatchXSLT.g_mainXSLTFrame.showMess( "+++++ copyFile setOption 'controlEncoding': " + mainXSLTFrame.ftpEncoding + "\n");
							BatchXSLT.g_mainXSLTFrame.showMess( "+++++ copyFile setOption 'activePassive': " + mainXSLTFrame.ftpActivePassive + "\n");
							BatchXSLT.g_mainXSLTFrame.showMess( "+++++ copyFile setOption 'server': " + out_url.getHost() + "\n");
							BatchXSLT.g_mainXSLTFrame.showMess( "+++++ copyFile setOption 'port': " + theport + "\n");
						}

						String my_user = mainXSLTFrame.ftpUsername;
						String my_pass = mainXSLTFrame.ftpPassword;
						String userinfo = out_url.getUserInfo();
						if ((userinfo != null) && (userinfo.equals("") == false)) {	// user given in the url
							String[] ui = userinfo.split(":");
							if (ui[0].equals("") == false) my_user = ui[0];
							if ((ui.length > 1) && (ui[1].equals("") == false)) my_pass = ui[1];
						}
						copyFile_ftpClient.setOption("user", my_user);
						copyFile_ftpClient.setOption("password", my_pass);
						if (DEBUG_COPYFILE) {
							BatchXSLT.g_mainXSLTFrame.showMess("+++++ copyFile setOption 'user': " + my_user + "\n");
							BatchXSLT.g_mainXSLTFrame.showMess("+++++ copyFile setOption 'password': " + my_pass + "\n");
						}

						// connect to remote server and also set the control stream encoding
						resp = copyFile_ftpClient.connect();
						if (resp != 0) {
							err = -23;
							ftp_err = resp;
							err_txt = "### ERROR FTP: Could not connect to host: " + out_url.getHost();
							lastFTPerr = copyFile_ftpClient.getResultCode();
							lastFTPerr_txt =  copyFile_ftpClient.getResultMessage();
							com.epaperarchives.batchxslt.ftp.disconnectDestroyFTPClient(copyFile_ftpClientID);
							copyFile_ftpClient_connected = false;
							copyFile_ftpClient = null;
							break;
						}	// host not found
						if (DEBUG_COPYFILE) BatchXSLT.g_mainXSLTFrame.showMess("+++++ copyFile successfully connected to remote host: " + out_url.getHost() + "\n");

						// login
						if (DEBUG_COPYFILE) BatchXSLT.g_mainXSLTFrame.showMess("+++++ copyFile log in my_user:" + my_user + " my_pass:" + my_pass + "\n");
						resp = copyFile_ftpClient.login();
						if (resp != 0) {
							err = -22;
							ftp_err = resp;
							err_txt = copyFile_ftpClient.getResultMessage();
							com.epaperarchives.batchxslt.ftp.disconnectDestroyFTPClient(copyFile_ftpClientID);
							copyFile_ftpClient_connected = false;
							copyFile_ftpClient = null;
							break;
						}
						copyFile_ftpClient_connected = true;
						if (give_message) BatchXSLT.g_mainXSLTFrame.showMess("*** FTP (" + ftpType + ") successfully connected to remote host: " + out_url.getHost() + "\n");

					}
					if (!copyFile_ftpClient_connected) break;

					// setup out path stuff
					String outpath, outname;
					outpath = split_path(out_url.getFile(), true);
					if (DEBUG_COPYFILE) BatchXSLT.g_mainXSLTFrame.showMess( "+++++ copyFile changing to remote path: " + outpath  + "\n");
					success = copyFile_ftpClient.cd(outpath);
					if (!success) {	// could not change remote dir
						if (DEBUG_COPYFILE) {
							BatchXSLT.g_mainXSLTFrame.showMess( "+++++ copyFile could not change to remote path: " + outpath  + "\n");
							BatchXSLT.g_mainXSLTFrame.showMess( "+++++ copyFile create remote path: " + outpath  + "\n");
						}
						// try to create the sub path at the target
						if (true == outpath.startsWith("/")) {
							success = copyFile_ftpClient.cd("/");	// change to root
							if (!success) {
								err = -31;
								ftp_err = -1; 
								err_txt = "### ERROR copyFile could not change to remote root path: /";
								if (DEBUG_COPYFILE) BatchXSLT.g_mainXSLTFrame.showMess( "### ERROR copyFile could not change to remote root path: /\n");
								com.epaperarchives.batchxslt.ftp.disconnectDestroyFTPClient(copyFile_ftpClientID);
								copyFile_ftpClient_connected = false;
								copyFile_ftpClient = null;
								break;
							}
						}
						success = copyFile_ftpClient.mkdirPath(outpath);
						if (!success) {	// could not create remote dir
							if (DEBUG_COPYFILE) {
								BatchXSLT.g_mainXSLTFrame.showMess( "### ERROR copyFile could not create remote path: " + outpath  + "\n");
								BatchXSLT.g_mainXSLTFrame.showMess( "### Error is: " + copyFile_ftpClient.getResultCode() + ", " +  copyFile_ftpClient.getResultMessage() + "\n");
							}
							err = -24;
							ftp_err = -1; 
							err_txt = "### ERROR FTP -1: Could not create remote path: " + outpath;
							com.epaperarchives.batchxslt.ftp.disconnectDestroyFTPClient(copyFile_ftpClientID);
							copyFile_ftpClient_connected = false;
							copyFile_ftpClient = null;
							break;
						}
						success = copyFile_ftpClient.cd(outpath);
						if (!success) {	// could not change into remote dir
							err = -25;
							ftp_err = resp;
							err_txt = "### ERROR FTP: Could not change into new remote path: " + outpath;
							if (DEBUG_COPYFILE) BatchXSLT.g_mainXSLTFrame.showMess( "### ERROR FTP: Could not change into new remote path: " + outpath  + "\n");
							com.epaperarchives.batchxslt.ftp.disconnectDestroyFTPClient(copyFile_ftpClientID);
							copyFile_ftpClient_connected = false;
							copyFile_ftpClient = null;
							break;
						}
					}

					// now transfer the file
					String target_filename = split_name(out_url.toString(), true);
					if (DEBUG_COPYFILE) BatchXSLT.g_mainXSLTFrame.showMess("******** copyFile connected: '" + my_sourcepath+my_sourcename + "' ->  '" + target_filename + "'\n");
		 			if (give_message) BatchXSLT.g_mainXSLTFrame.showMess("-- FTP file: " + out_url.toString() + " -> " + outpath + "/" + target_filename + " ... ");
					success = putFileThread(my_sourcepath+my_sourcename, target_filename);
					if (!success) {
						err = -27;
						ftp_err = -1;
						err_txt = "### ERROR FTP: Could not STOR file '" + my_sourcepath+my_sourcename + "' to remote location '" + target_filename +"'";
						lastFTPerr = copyFile_ftpClient.getResultCode();
						lastFTPerr_txt =  copyFile_ftpClient.getResultMessage();
						com.epaperarchives.batchxslt.ftp.disconnectDestroyFTPClient(copyFile_ftpClientID);
						copyFile_ftpClient_connected = false;
						copyFile_ftpClient = null;
						break;
					}
					if (err == 0){
						if (give_message) BatchXSLT.g_mainXSLTFrame.showMess("OK.\n");
					}

					if (disconnect == true) {
						com.epaperarchives.batchxslt.ftp.disconnectDestroyFTPClient(copyFile_ftpClientID);
						copyFile_ftpClient_connected = false;
						copyFile_ftpClient = null;
					}
				}
				catch (Exception exc) {
					err = -29; ftp_err = 0; err_txt = "### EXCEPTION FTP: " + exc;	// any exception
					com.epaperarchives.batchxslt.ftp.disconnectDestroyFTPClient(copyFile_ftpClientID);
					copyFile_ftpClient_connected = false;
					copyFile_ftpClient = null;
				}
				break;

			case 3:	// an http URI - make a StreamSource from a URI object
				break;

			default:	// a 'normal' file from the file system
				// check if 'my_targetpath' exists - else create it
				if (create_target_path > 0) {
					File anyFile = new File(my_targetpath);
					if (!anyFile.exists()) { boolean retflag = anyFile.mkdirs(); }
				}

		 		if (give_message) BatchXSLT.g_mainXSLTFrame.showMess("Copying Source file: '" + my_sourcename + "'...");
				// open the source
				try { my_instream = new FileInputStream( my_sourcepath+my_sourcename ); }
				catch ( FileNotFoundException fnfe ) {		// uups could not open source?
					return(-10);
				}
				// open the target
				try { my_outstream = new FileOutputStream( my_targetpath+my_targetname ); }
				catch ( java.io.FileNotFoundException fnfex ) {		// uups could not create target?
					try { my_instream.close(); } catch ( java.io.IOException ioex) {}	// close the source
					err = -11;
					break;
				}
				do {
					// read an input block...
					try { bytes_read = my_instream.read( mybuf ); }
					catch ( java.io.IOException e) {	// ahhaaa - eof reached
						break;
					}
					// ...and write it to the output
					if (bytes_read > 0) {
						try { my_outstream.write( mybuf, 0, bytes_read ); }
						catch ( java.io.IOException e) {	// uups error writing to file???
						}
						total_bytes += bytes_read;
					}

					if (bytes_read < buflen) break;	// ok - we have reached eof
				} while (true);

				// close in file
				if (my_instream != null) {
					try { my_instream.close(); } catch ( java.io.IOException e) {}	// close the source
				}
				if (my_outstream != null) {
					try { my_outstream.flush(); } catch ( java.io.IOException e) {}
					try { my_outstream.close(); } catch ( java.io.IOException e) {}
				}
				if (err == 0){
					if (give_message) BatchXSLT.g_mainXSLTFrame.showMess("OK.\n");
				}
				break;
		}

 		if (err != 0){
 			if (result_type == 2) {
 				BatchXSLT.g_mainXSLTFrame.showMess("### ERROR: " + err + " / FTP error: " + ftp_err +  " / '" + err_txt + "'\n");
				if (lastFTPerr_txt.equals("") == false) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR FTP is: " + lastFTPerr + ", " +  lastFTPerr_txt + "\n");
 			}
 			else BatchXSLT.g_mainXSLTFrame.showMess("### ERROR: " + err + "\n");
 		}

		return(err);
	}




	/* ===========================================================
	 * return true if we are on a Windows machine
	 */
	public static boolean isWindows()
	{
		if (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Wind") >= 0) return(true);
		return(false);
	}




	/* ===========================================================
	 * return true if we are on a Macintosh machine
	 */
	public static boolean isMacintosh()
	{
		if ((BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("OS X") < 0)
			&& (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("Mac") >= 0)) return(true);
		return(false);
	}




	/* ===========================================================
	 * return true if we are on a Macintosh machine
	 */
	public static boolean isMacOSX()
	{
		if (BatchXSLT.g_mainXSLTFrame.systemOSname.indexOf("OS X") >= 0) return(true);
		return(false);
	}





	/* ===========================================================
	 * create a local dir path
	 */
	public static boolean make_dirpath(String the_path)
	{
		File apath = new File(the_path);
		boolean retflag = apath.mkdirs();
		return(retflag);
	}





	/* ===========================================================
	 * delete a file or dir
	 */
	public static boolean file_delete(String the_path)
	{
		File apath = new File(the_path);
		boolean retflag = apath.delete();
		return(retflag);
	}





	/* ===========================================================
	 * check if a file or dir exists
	 */
	public static boolean file_exists(String the_pathname)
	{
		File anyFile = new File(the_pathname);
		boolean f_exists = anyFile.exists();
		return(f_exists);
	}





	/* ===========================================================
	 * check if a path is a dir or a file
	 */
	public static boolean is_dir(String the_pathname)
	{
		File anyFile = new File(the_pathname);
		boolean dir = anyFile.isDirectory();
		return(dir);
	}





	/* ===========================================================
	 * clean up memory and other stuff
	 */
	public static void cleanUpMemory()
	{
		// clean up memory and other stuff
		System.runFinalization();
		System.gc();
	}





	/* ===========================================================
	 * get the type of a file name: local file or an URI
	 */
	public static int get_URL_file_type(String the_path)
	{
		if (the_path.equals("")) return(1);

		if (the_path.startsWith("ftp:") == true) return(2);
		if (the_path.startsWith("ftps:") == true) return(2);
		if (the_path.startsWith("ftpes:") == true) return(2);
		if (the_path.startsWith("sftp:") == true) return(2);
		if (the_path.startsWith("ftp2:") == true) return(2);
		if (the_path.startsWith("http:") == true) return(3);
		if (the_path.startsWith("https:") == true) return(3);
		if (the_path.startsWith("*NONE") == true) return(0);	// no output file
		return(1);	// local file or no path given
	}





	/* ===========================================================
	 * get the name portion from a path/name
	 */
	public static String split_name(String in_pathname, boolean is_url)
	{
		int				pos = -1;
		String			myname = "";
		
		if (in_pathname.length() == 0) return(myname);
		if (is_url || get_URL_file_type(in_pathname) >= 2) pos = in_pathname.lastIndexOf("/");	// for URLs
		else pos = in_pathname.lastIndexOf(File.separator);	// for local files
		if (pos == in_pathname.length()) return(myname);	// in_pathname ends with a '/'
		if (pos < 0) {	// not found a path separator char
			myname = in_pathname;
		}
		else {
			myname = in_pathname.substring(pos + 1);
		}
		return(myname);
	}





	/* ===========================================================
	 * get the path portion from a path/name
	 */
	public static String split_path(String in_pathname, boolean is_url)
	{
		int				pos = -1;
		String			mypath = "";
		
		if (in_pathname.length() == 0) return(mypath);
		if (is_url || get_URL_file_type(in_pathname) >= 2) pos = in_pathname.lastIndexOf("/");	// for URLs
		else pos = in_pathname.lastIndexOf(File.separator);	// for local files
		if (pos < 0) {	// not found a path separator char
			//mypath = File.separator;	// set root
		}
		else {
			mypath = in_pathname.substring(0,pos);
			if (mypath.equals("") == true) mypath = File.separator;	// set root
		}
		return(mypath);
	}





	/* ===========================================================
	 * get the sub path portion from a base path and a new path
	 */
	public static String get_sub_path(String base_path,String full_path)
	{
		String	my_base_path = base_path;
		String	my_full_path = full_path;
		String	my_sub_path = "";

		if (my_base_path.equals(my_full_path)) return("");
		if (my_full_path.startsWith(my_base_path) == false) return("");	// totally different path

		if (my_base_path.endsWith(File.separator) == false) my_base_path += File.separator;

		my_sub_path = my_full_path.substring(my_base_path.length(),my_full_path.length());

		return(my_sub_path);
	}





	/* ===========================================================
	 * call an external windows command
	 */
    public static int exec_windows_command(String args[])
    {
    int exitVal = 0;
    
        if (args.length < 1) return(1);
         
        try
        {            
            String osName = System.getProperty("os.name" );
            String[] cmd = new String[3];

            if ( osName.startsWith( "Windows 9" ) ) {	// like Windows 95 98 ...
				cmd[0] = "command.com" ;
				cmd[1] = "/C" ;
				cmd[2] = args[0];
            }
            else {
	            if ( osName.startsWith( "Windows" ) ) {	// like Windows NT, 2000, XP....
					cmd[0] = "cmd.exe" ;
					cmd[1] = "/C" ;
					cmd[2] = args[0];
				}
				else return -1;
           }
            
            Runtime rt = Runtime.getRuntime();
         // System.out.println("Execing " + cmd[0] + " " + cmd[1] 
         //                      + " " + cmd[2]);
            Process proc = rt.exec(cmd);
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", true);            
            
            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT", true);
                
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
                                    
            // any error???
            exitVal = proc.waitFor();
        } catch (Throwable t)
          {
            t.printStackTrace();
            return(-1);
          }
        return(exitVal);
    }





	/* ===========================================================
	 * call an external program/command
	 */
    public static int exec_OS_command(String cmd[])
    {
    int exitVal = 0;
    
        if (cmd.length < 1) return(1);
         
        try
        {            
            
            Runtime rt = Runtime.getRuntime();
         // System.out.println("Execing " + cmd[0] + " " + cmd[1] 
         //                      + " " + cmd[2]);
            Process proc = rt.exec(cmd);
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", true);            
            
            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT", true);
                
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
                                    
            // any error???
            exitVal = proc.waitFor();
        } catch (Throwable t)
          {
            t.printStackTrace();
            return(-1);
          }
        return(exitVal);
    }






	/* ===========================================================
	 * add variable to a string
	 * variable names are in the form &&name&
	 */
	public static String add_vars(String the_str)
	{
		String		my_str = the_str;
		String		pre_str = "", var_str = "", end_str = "";
		
		if (my_str.length() <= 3) return(the_str);	// too short - may not have any variables
		int idx = 0;	// our index pointer through the whole string
		int pos = my_str.indexOf('&',idx);
		while (pos >= 0) {
			if (my_str.charAt(pos+1) == '&') {				// yes - we have found a double starting '&&' in this string
				int pos_end = my_str.indexOf('&',pos+2);	// where is the variable name's end?
				if (pos_end < 0) return(my_str);			// uups - no end found
				// at pos+2 = 
				pre_str = my_str.substring(0,pos);
				var_str = my_str.substring(pos+2,pos_end);
				end_str = my_str.substring(pos_end+1,my_str.length());
				// now replace the var_str with the varable
				var_str = get_var(var_str);
				// and reconcatenate the string
				my_str = pre_str + var_str + end_str;
			}
		
			idx = 0;
			pos = my_str.indexOf('&',idx);					// try to find more vars
		}

		return(my_str);
	}





	/* ===========================================================
	 * return a value from the given string
	 */
	public static String get_var(String the_str)
	{
		String my_str = "";

		switch (the_str.charAt(0)) {
			case 'I':
				if (the_str.equals("INPUT_SUBPATH")) {
					my_str = BatchXSLTransform.my_InputSubPath; my_str = my_str.replace('\\','/'); break;
				}
				break;
			case 'S':
				if (the_str.equals("SRC_FILENAME")) {
					my_str = BatchXSLTransform.my_InputName; break;
				}
				break;
			case 'c':	// random character string
				int len = the_str.length();
				my_str = org.apache.commons.lang.RandomStringUtils.randomAlphabetic(len);
				break;
		}
		return(my_str);
	}







	/* ===========================================================
	 * check if the given string is plain ascii
	 */
	public static boolean isProperURI(String the_str)
	{
		int	i, val;

		for (i = 0; i < the_str.length(); i++) {
			val = the_str.charAt(i);
			if (val < 29) return(false);
			if (the_str.charAt(i) == '/') return(false);
			if (the_str.charAt(i) == '\\') return(false);
			if (the_str.charAt(i) == ':') return(false);
			if (the_str.charAt(i) == '*') return(false);
			if (the_str.charAt(i) == '?') return(false);
			if (the_str.charAt(i) == '<') return(false);
			if (the_str.charAt(i) == '>') return(false);
			if (the_str.charAt(i) == '|') return(false);
			if (the_str.charAt(i) == '%') return(false);
			if (val < 127) continue;
			return(false);

		}
		return(true);
	}





	/* ===========================================================
	 * clean a file name to a proper URI
	 */
	public static String myURIencodeFileName(String the_string)
	{
		String minVMversion = BatchXSLT.g_mainXSLTFrame.VMversion.substring(0,3);
		String systemID;
		
		if (minVMversion.compareTo("1.4") >= 0) {	// for VM versions 1.4 and higher
			URI fileURI = new File(the_string).toURI();
			systemID = fileURI.toASCIIString();
			systemID = split_name(systemID, true);	// get name part only
			// here, all unwanted chars are changed to %xx
			// let's convert other unwanted chars
			systemID = myURIencode(systemID, false);	// do not convert the % char
		}
		else {	// for old JVM like on OS9
			systemID = the_string;
			// here, all unwanted chars are changed to %xx
			// let's convert other unwanted chars
			systemID = myURIencode(systemID, false);	// DO NOT convert the % char because in OS9 and URL's blanks are already %20
		}
		return(systemID);
	}




	/* ===========================================================
	 * clean a string to a proper URL
	 */
	public static String myURIencode(String the_string, boolean convert_percent)
	{
		String	new_string = "";
		int		i, val;
		char	c;
		int		slen = 0;
		boolean	replace = false;
		String	replace_str = "%";
		
		slen = the_string.length();
		for (i = 0; i < slen; i++ ) {
			c = the_string.charAt(i);
			switch (c) {
				case '\\':	// all these chars are not allowed on Windows systems
				case '/':	// |
				case ':':	// |
				case '*':	// |
				case '?':	// |
				case '<':	// |
				case '>':	// |
				case '|':	// |
				case '%':
					if ( c != '%' ) {
						replace_str += char2hex(c);
						replace = true;
						break;
					}
					else {	// the % char
						if (convert_percent) {
							replace_str += char2hex(c);
							replace = true;
							break;
						}
						else {
							new_string += c;	// leave the % as is
						}
					}
					break;
				default:
					val = c;
					if (val < 29 || val >= 127) {
						replace_str += char2hex(c);
						replace = true;
					}
					else {	// a good char
						new_string += c;
					}
					break;
			}
			if (replace == true) {
				new_string += replace_str;
				replace = false;
				replace_str = "%";
			}
		}

		return(new_string);
	}







	/* ===========================================================
	 * replace an existing filename extension with a new one
	 */
	public static String replace_filename_ext(String the_string, String new_ext)
	{
		String my_string = the_string;
		
		int namelen, pointpos = -1;
		String my_outputFileNameExt = "";
		namelen = my_string.length();
		do {
			if (namelen < 1) break;
			if (my_string.charAt(namelen-1) == '.') { pointpos = namelen-1; break; }
			if (namelen < 2) break;
			if (my_string.charAt(namelen-2) == '.') { pointpos = namelen-2; break; }
			if (namelen < 3) break;
			if (my_string.charAt(namelen-3) == '.') { pointpos = namelen-3; break; }
			if (namelen < 4) break;
			if (my_string.charAt(namelen-4) == '.') { pointpos = namelen-4; break; }
			if (namelen < 5) break;
			if (my_string.charAt(namelen-5) == '.') { pointpos = namelen-5; break; }
			if (namelen < 6) break;
			if (my_string.charAt(namelen-6) == '.') { pointpos = namelen-6; break; }
			break;
		} while (false);

		// cut ext from current output name
		if (pointpos >= 1) my_string = my_string.substring(0,pointpos);
		// ad the new extension
		my_string = my_string + new_ext;

		return(my_string);
	}





	/* ===========================================================
	 * check if filename has extension  like .ps, .eps .xml
	 */
	public static boolean has_filename_ext(String the_string)
	{
		int namelen = the_string.length();

		if (namelen < 1) return(false);
		if (the_string.charAt(namelen-1) == '.') return(true);
		if (namelen < 2) return(false);
		if (the_string.charAt(namelen-2) == '.') return(true);
		if (namelen < 3) return(false);
		if (the_string.charAt(namelen-3) == '.') return(true);
		if (namelen < 4) return(false);
		if (the_string.charAt(namelen-4) == '.') return(true);
		if (namelen < 5) return(false);
		if (the_string.charAt(namelen-5) == '.') return(true);

		return(false);
	}





	/* ===========================================================
	 * re-encode an iso-8859-1 file to utf8
	 */
	public static int file_iso2utf(String iso_file, String utf_file, int delete_input_file)
	{
		Calendar cal = Calendar.getInstance();		// get current date and time
		String	cY = "" + cal.get(cal.YEAR);
		String	cM = "" + (cal.get(cal.MONTH) + 1);
		String	cD = "" + cal.get(cal.DAY_OF_MONTH);
		
		String	ch = "" + cal.get(cal.HOUR_OF_DAY);
		String	cm = "" + cal.get(cal.MINUTE);
		String	cs = "" + cal.get(cal.SECOND);


		String	cd = "" + cY + ((cM.length() < 2) ? ("0"+cM) : cM) + ((cD.length() < 2) ? ("0"+cD) : cD);
		String	ct = ((ch.length() < 2) ? ("0"+ch) : ch) + ":" + ((cm.length() < 2) ? ("0"+cm) : cm) + ":" + ((cs.length() < 2) ? ("0"+cs) : cs);

		int					retval = 0;
		int					buf_size = 65536;
		char[]				cbuf = new char[buf_size];
		FileInputStream		fis = null;
		InputStreamReader	isr;

		FileOutputStream	fos = null;
		OutputStreamWriter	osw = null;
		BufferedWriter		bwr = null;
		int					chars_written = 0;


		String				URL_utf_file = utf_file;	// clean from unwanted chars
							URL_utf_file = MyRegex.replace("&",URL_utf_file,"\u0026amp;");	// & to entity
							URL_utf_file = MyRegex.replace("<",URL_utf_file,"\u0026lt;");	// < to entity
							URL_utf_file = MyRegex.replace(">",URL_utf_file,"\u0026gt;");	// > to entity

		String				xml_head = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE epsexport>\n" +
((BatchXSLT.g_mainXSLTFrame.eps2xml_associate_XSL.equals("") == false) ? ("<?xml-stylesheet href=\"" + BatchXSLT.g_mainXSLTFrame.eps2xml_associate_XSL + "\" type=\"text/xsl\"?>\n") : ("")) +
"<epsexport OutputCreationDate=\"" + cd + "\" OutputCreationTime=\"" + ct + "\" encoding=\"utf-8\" xml:space=\"preserve\">\n" +
"<epsheader type=\"eps\">\n" +
"<epsheaderfield name=\"Filename\">" + URL_utf_file + "</epsheaderfield>\n" +
"<epsheaderfield name=\"Sourcepath\">" + BatchXSLT.g_mainXSLTFrame.EPSPDF_exported_path + "</epsheaderfield>\n" +
"<epsheaderfield name=\"EPSsrc\">" + BatchXSLT.g_mainXSLTFrame.EPS_last_exported + "</epsheaderfield>\n" +
"<epsheaderfield name=\"PDFsrc\">" + BatchXSLT.g_mainXSLTFrame.PDF_last_exported + "</epsheaderfield>\n" +
"</epsheader>\n" +
"<epstext>";

		String				xml_trail = "</epstext>\n</epsexport>\n";
		
		
		// read the iso file into a char array
		try { fis = new FileInputStream( iso_file ); }
		catch ( FileNotFoundException fnfe ) {
			return(1);
		}
		try { isr = new InputStreamReader(fis, "iso-8859-1"); }
		catch ( UnsupportedEncodingException e ) {
			try { fis.close(); } catch ( IOException ioe ) { }
			return(1);
		}
		int input_bytes_avail = -1;
		int bytes_read = -1;
		int ofs = 0;
		
		try { input_bytes_avail = fis.available(); }
		catch ( IOException e ) {
			try { fis.close(); } catch ( IOException ioe ) { }
			return(2);
		}


		
		// write the char array to a new file iso file into a char array
		try { fos = new FileOutputStream( utf_file ); }
		catch ( FileNotFoundException fnfe ) {
			return(-1);
		}
		try { bwr = new BufferedWriter(new OutputStreamWriter(fos, "UTF8")); }
		catch ( UnsupportedEncodingException e ) {
			try { fos.close(); } catch ( IOException ioe ) { }
			File aFile = new File( utf_file );	// and delete the created file
			aFile.delete();
			return(-1);
		}


		// read and write files
		// write xml header
		do {
			try { bwr.write(xml_head); } catch ( IOException e ) { break; }
			do {
				try {
					bytes_read = isr.read(cbuf, 0, buf_size);	// returns -1 if eof reached or num bytes read
					// check if text ends with a linefeed and CR
					if (bytes_read > 0) {
						String str = new String(cbuf,  0,  bytes_read);	// get text from input file

						if (str.length() > 0) {											// clean :
							if (str.length() > 0) str = MyRegex.replace("\f\n",str,"");			// new line and form feed
							if (str.length() > 0) str = MyRegex.replace("&",str,"\u0026amp;");	// & to entity
							if (str.length() > 0) str = MyRegex.replace("<",str,"\u0026lt;");	// < to entity
							if (str.length() > 0) str = MyRegex.replace(">",str,"\u0026gt;");	// > to entity

							if (str.length() > 0) str = MyRegex.replace("\u009A",str,"\u00B0");	// Degree char

							if (str.length() > 0) str = MyRegex.replace(" m 2 ",str," m2 ");	// m2 square meter
							if (str.length() > 0) str = MyRegex.replace(" m 3 ",str," m3 ");	// m2 cube meter

							if (str.length() > 0) str = MyRegex.replace(".\u00AD\n",str,"");	// currency   .- LF at end of line
							if (str.length() > 0) str = MyRegex.replace(".\u00AD\u00AD\n",str,"");	// currency   .-- LF at end of line

							if (str.length() > 0) str = MyRegex.replace("\u00AD\n",str,"");		// AD 0A hyphenations
							if (str.length() > 0) str = MyRegex.replace("\u00AD",str,"-");		// single divis

							if (str.length() > 0) {
								if ((mainXSLTFrame.gs_convert & 4) == 4) str = MyRegex.replace("\n",str,"<epsbr/>\n");	// 0A to html line break
							}
						}
						
						// and write the string out
						if (str.length() > 0) {
							bwr.write(str, 0, str.length());
							chars_written += str.length();
						}
					}
		        }
				catch ( IOException e ) { break; }
				if ( bytes_read < 0 ) break;	// have reached eof
			} while(true);
			// write xml trailer
			try { bwr.write(xml_trail); } catch ( IOException e ) { break; }
		} while(false);

		// close input and output
		try { fis.close(); } catch ( IOException ioe ) { }
		try { bwr.flush(); bwr.close(); } catch ( IOException ioe ) { }

		// check if we have written to the output file - otherwise delete it
		if ( chars_written <= 0 ) {
			File aFile = new File( utf_file );
			aFile.delete();
			retval = 3;	// means no xml data written
		}
		if ( delete_input_file == 1 ) {
			if ((mainXSLTFrame.gs_convert & 1024) != 1024) {	// if 1024 is set we should not delete tx1 files
				File aFile = new File( iso_file );
				aFile.delete();
			}
		}
		
		return(retval);
	}



}




/* ===========================================================
 * get streams from external processes
 */
class StreamGobbler extends Thread
{
	InputStream is;
	String type;
	static String response;
	boolean write_to_console;

	StreamGobbler(InputStream is, String type, boolean write_to_console) {
	   this.is = is;
	   this.type = type;
	   this.write_to_console = write_to_console;
	   this.response = "";	// stores the last full response of this stream
	}

	public void run() {
		Thread.currentThread().setName("StreamGobbler");
		try {
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ( (line = br.readLine()) != null) {
				this.response += line + "\n";
				if (write_to_console == true) System.out.println(type + ">" + line);
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();  
		}
	}


	public String getLine(int line_nbr) {
		String line = "";
		int cur_line = 0;
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			while ( (cur_line <= line_nbr) &&  (line = br.readLine()) != null) {
				if (cur_line == line_nbr) break;
				cur_line++;
			} 
		}
		catch (IOException ioe) {
			ioe.printStackTrace();  
		}
		return(line);
	}

	public String getResponse() {
		return(this.response);
	}
}





/* ===========================================================
 * load a jobticket file into an array
 */
class JobTicket {
	private String jobticketstring = null;
	BufferedReader jobticketdata = null;
	int input_bytes_avail = -1;
	int bytes_read = -1;
	int ofs = 0;
	boolean DEBUGjt = false;
	
	public int load (InputStream the_is) throws IOException {

		if (the_is == null) {
			if (DEBUGjt) System.out.println("InputStream the_is null");
			throw new IOException("InputStream is null");
		}
		try { input_bytes_avail = the_is.available(); }
		catch ( IOException e ) {
			if (DEBUGjt) System.out.println("InputStream Exception: IOException while getting stream length");
			return(-3);
		}
		if (DEBUGjt) System.out.println("InputStream size: " + input_bytes_avail);
		if (input_bytes_avail <= 0) {
			if (DEBUGjt) System.out.println("InputStream: no data available");
			return(-4);
		}
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(the_is, "UTF8"));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		
		while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line + "\n");
		}
		
		bufferedReader.close();
		jobticketstring = stringBuilder.toString();
		
		return(0);
	}

	public String getString (String searchkey) throws MissingResourceException {
		if ((searchkey == null) || (searchkey.equals("") == true)) {
			if (DEBUGjt) System.out.println("JobTicket.getString(): Search key not defined");
			throw new java.util.MissingResourceException("Search key not defined", "java.lang.String", "");	// empty key
		}

    jobticketdata = new BufferedReader(new StringReader(jobticketstring));
		if (jobticketdata == null) {
			if (DEBUGjt) System.out.println("JobTicket.getString(): jobticketdata is null");
			throw new java.util.MissingResourceException("JobTicked file not available", "java.lang.String", searchkey);
		}

		String key = "";
		String val = "";
		String line = "";
		while (line != null) {
			try { line = jobticketdata.readLine(); }
			catch ( IOException e ) {
				if (DEBUGjt) System.out.println("JobTicket.getString(): readLine() failed");
				try { jobticketdata.close(); } catch ( IOException ioe ) {}
				throw new java.util.MissingResourceException("IOException while reading JobTicked data", "java.lang.String", searchkey);
			}
			if (line == null) {
				if (DEBUGjt) System.out.println("JobTicket.getString():reached eof while searching for key '" + searchkey + "'");
				try { jobticketdata.close(); } catch ( IOException e ) {}
				throw new java.util.MissingResourceException("Key not found", "java.lang.String", searchkey);	// reached eof
			}
			if (line.equals("") == true) continue;	// empty line

			if (line.startsWith("# -------------- END OPTIONS SETTINGS ") == true) {  // end of settings (rest is description)
				throw new java.util.MissingResourceException("Key not found", "java.lang.String", searchkey);	// reached eof
			}

			if ((line.charAt(0) == '#') || (line.charAt(0) == '!')) continue;	// skip comments
			int equalpos = line.indexOf("=");
			if (equalpos <= 0) continue;	// no '=' or at first pos: illegal line

			if (DEBUGjt) System.out.println("JobTicket line: '" + line + "'");

			key = line.substring(0,equalpos);
			key = key.trim();
			if (DEBUGjt) System.out.println("            key: '" + key + "', searchkey: '"+ searchkey +"'");
			if (key.equals("") == true) continue;	// empty key
			if (key.equals(searchkey) == false) continue;	// key no match

			// matching search key
			val = line.substring(equalpos+1);
			val = val.trim();
			if (DEBUGjt) System.out.println("            val: '" + val );
			break;
		}
    try { jobticketdata.close(); } catch ( IOException e ) {}	// we will recreate the BufferedReader
		return(val);
	}
	
}


