/** 
 * mainXSLTFrame.java
 *
 * Title:			BatchXSLT Batch XSL Transformation
 * Description:	
 * @author			Andreas Imhof
 * @version			40.0
 * @versionDate		20200221
 * History:
 * 20200221: Version 40.10 compatible with macos Catalina
 *           and publish github freeware
 *           
 */

package com.epaperarchives.batchxslt;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.SortedMap;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.iharder.dnd.FileDrop.*;




public class mainXSLTFrame extends javax.swing.JFrame {
	public mainXSLTFrame() {
	}

	/***************************************************************/
	/**
	 * general variables
	 */
	public static boolean			DEBUG = false;	// set to true to show messages on console for debugging
	public static int         DEBUGjtThread = 0;	// set to > 0  to show messages on console for debugging JTOverrideThread
	public static int         showThreadViewer = 0;	// set to > 0  to show all threads in a table



	public static String			applFullName = "BatchXSLT";
	public static String			applLogoNameDefault = "BatchXSLT.png";
	public static String			applLogoName = applLogoNameDefault;
	public static String			logfile_prefix = "BatchXSLT";	// results in BatchXSLT_log.txt
	public static String			applFullName2 = "easEPub";
	public static String			applLogoName2Default = "easepub.png";
	public static String			applLogoName2 = applLogoName2Default;
	public static String			logfile_prefix2 = "easEPub";	// results in easEpub_log.txt

	public static String			applShortName = "BXSLT";
	public static String			applLicenseShortcut = "bx";				// the id for licenses
	public static String			applMajorVersion = "40";
	public static String			applMinorVersion = "0";
	public static final String		applCategory = "Transformer";
	public static final String		applFunction_default = "Batch XML Transformer";
	public static String			applFunction = applFunction_default;

									// where to find the online manual
	public static final String		default_onlineManualUrl = "http://www.manuals.epaper-system.com/" + applCategory + "/";	// get manual default.htm from there
	public static String			onlineManualUrl = default_onlineManualUrl, save_onlineManualUrl;	// get manual default.htm from there


									// properties evaluated in initXSLT:
	public static String			systemOSname = "";	// System.getProperty("os.name")
	public static String			systemOSversion = "";	// System.getProperty("os.version");
	public static String			systemOSarchitecture = "";	// System.getProperty("os.arch");
	public static String			systemDefaultCharset = "";	// System.getProperty("file.encoding");
	public static String			VMversion = "";	// System.getProperty("java.version", "");
	public static String			VMhome = "";	// System.getProperty("java.home", "");

	public static String			appDir = "";	// app base directory like /Applications/BatchXSLT4..../BatchXSLT (the folder where the starter pgms are)
	public static String			userDir = "";	// System.getProperty("user.dir");	// User's current working directory
	public static String			userHome = System.getProperty("user.home");	// User home directory
	public static String			userName = "";	// System.getProperty("user.name");	// User account name
	public static String			userTempDir = "";	// System.getProperty("java.io.tmpdir");	// User temporary folder

	public static String			userLang = "en";	// System.getProperty("user.language");	// User account name
	public static String			userCountry = "en";	// System.getProperty("user.country");	// User country

	public static String			envirPath = "";	// System.getProperty("PATH")
	public static String			localMachineName = "";	// InetAddress.getLocalHost().getHostName()
	public static String			userEmailAddress = "";	// ... to send mail
	public static String			externalSmtpHost = "";	// ... to send mail


	public boolean				    environmentOK = false;
	public static String			version_xerces = "?";
	public static String			version_xerces1 = "?";
	public static String			version_xalan2x = "?";
	public static String			version_xalan1 = "?";
	public static String			version_DOM = "?";

	public static int				clearMessageWindow = 0;			// clear message window before displaying newly loaded jt

									// check for available external converters
	public static int				check_for_external_converters = 0, save_check_for_external_converters;	// 0 = don't check
																		// 1 = check for GhostScipt
																		// 2 = check for pstotext
																		// 4 = check for ImageMagick
																		// below paths will be used to find applications

									//=====================
									// ImageMagick stuff
	public static int				im_debug = 0;								// >0 = do show debug messages when registering ImageMagick
	public static int				im_gs_sanitycheck = 1;						// >0 = do make sanity check of ImageMagick and/or ghostscript
																				// 0 = no check
																				// 1 = do it and delete temporary image files
																				// 2 = do it and DON'T delete temporary image files

	public static String			im_version_str = "";						// evtl. will contain ImageMagick version string
																				// to indicate that ImageMagick is reachable
	public static String			im_version_num = "";						// version string number like "6.5.5"
	public static String			im_version_num_win = "";					// alternate version string number for windows like "6.4.1"
	public static String			im_quantumdepth = "";						// 16 or 8, pixel depth
	public static String			im_arch = "";								// i386 or whatever will come
	public static String			im_pgm_CONVERT = "";		// the path/name of Imagemagick's 'convert' program
	public static String			im_pgm_IDENTIFY = "";		// the path/name of Imagemagick's 'identify' program
	public static String			im_pgm_path = "";			// the full path to Imagemagick's install path
	public static String			im_pgm_installedhome = "";	// goes into envir var 'MAGICK_HOME' like '/usr/' or '/usr/local/'
	public static String			im_pgm_installedpath = "";	// the folder to an installed executable of Imagemagick like '/usr/bin/' or '/usr/local/bin/'
	public static String			im_envir = "";				// the environment to set to run Ghostscript pgm like PATH=/usr/loacl/bin+++MY=blabla
	public static String			im_envir_default = "PATH=/usr/local/bin/";	// default (install location) = PATH=/usr/local/bin/

	public static String			color_profiles_path = "";	// the path to packaged color profiles

									//=====================
									// GhostScript stuff
	public static int				gs_convert = 0, save_gs_convert;		// flag indicating how to use Ghostscript to convert files
														// 		ad them to do multiple operations
														// 0 = do NO conversion
														// 1 = convert EPS file to PDF
														// 2 = extract TEXT from above PDF into XML file
														// 4 = replace \n with <br/>\n in XML file
														// 1024 = debugging: the extracted text = do not delete the tx1 file
	public static String			gs_version_num = "";		// version string number like "8.54"
	public static String			gs_version_str = "";		// evtl. will contain Ghostscript version string
																		// to indicate that Ghostscript is reachable
	public static String			gs_arch = "";								// i386 or whatever will come
	public static String			gs_pgm_path = "", save_gs_pgm_path;					// the path to Ghostscript pgm
	public static String			gs_pgm_name = "", save_gs_pgm_name;					// the path to Ghostscript pgm
	public static String			gs_pgm_installedhome = "", save_gs_pgm_installedhome;	// goes into envir var 'MAGICK_HOME' like '/usr/' or '/usr/local/'
	public static String			gs_pgm_installedpath = "", save_gs_pgm_installedpath;	// the folder to an installed executable of Imagemagick like '/usr/bin/' or '/usr/local/bin/'
	public static String			gs_pgm_path_default = "/usr/local/bin/gs";			// default (install location) = /usr/local/bin/gs
	public static String			gs_pgm_path_envir = "", save_gs_pgm_path_envir;		// the PTH environment variable for Ghostscript
	public static String			gs_envir = "", save_gs_envir;						// the environment to set to run Ghostscript pgm like PATH=/usr/loacl/bin+++MY=blabla
	public static String			gs_envir_default = "PATH=/usr/local/bin/";	// default (install location) = PATH=/usr/local/bin/
	public static String			gs_pgm_parms_eps2pdf = "", save_gs_pgm_parms_eps2pdf;			// the parameters to call Ghostscript to convert EPS to PDF


									//=====================
									// pstotext stuff
	public static String			pstotext_version_str = "";			// evtl. will contain pstotext version string
																		// to indicat that pstotext is reachable
	public static String			pstotext_pgm_path_default = "/usr/local/bin/pstotext";		// default (install location) = /usr/local/bin/pstotext
	public static String			pstotext_pgm_path = "", save_pstotext_pgm_path;				// the path to to the text extract pgm
	public static String			pstotext_envir = "", save_pstotext_envir;				// the environment to set to run pstotext pgm like PATH=/usr/loacl/bin+++MY=blabla
	public static String			pstotext_envir_default = "PATH=/usr/local/bin/";	// default (install location) = PATH=/usr/local/bin/
	public static String			pstotext_pgm_parms_pdf2txt = "", save_pstotext_pgm_parms_pdf2txt;	// the parameters to extract text


	public static String			eps2xml_associate_XSL = "", save_eps2xml_associate_XSL;			// the XSL to associate into the XML file
	public static String			EPSPDF_exported_path = "";			// the EPS source path
	public static String			EPS_last_exported = "";				// the EPS source of the resulting PDF file
	public static String			PDF_last_exported = "";				// the PDF source of the resulting XML file



									//=====================
									// Log files stuff
	public static int				logfile_write = 0, save_logfile_write;			// 0 = do not write messages into log file
	public static String			logfile_path = userHome, save_logfile_path;	// the path to log file. default =user's home dir
	public static String			default_logfile_name = logfile_prefix + "_log.txt";		// the name of log file. default = BatchXSLT_log.txt
	public static String			logfile_name = default_logfile_name, save_logfile_name;
	public static int				logfile_maxsize = 10485760;	// def to 10M
	public static int				logfile_minsize = 1000;		// min possible size
	public static int				logfile_max = 10;			// max number of stored logfiles
	public static int				logfile_min = 1;			// min possible amount of files

	public static boolean			deleteFailedTransformFiles = true;			// delete result (partial) if a transform failed


	public static int				mode = 1, save_mode, mode_before_overridejt;			// may be: 0 = manual | 1 = Batch | 2 = do and then Quit
																							// 4 = called from override JT
	public static int				mode_setby_overridejt = 0;								// may be: 0 = not overriden by override.jt or 100 = transform request by override.jt
	public static String			jt_schedule, save_jt_schedule;							// a raw scheduler string
	public static String			jt_triggerfile, save_jt_triggerfile;					// a file that must be present to start JT
	public static String			sourcePathRoot, save_sourcePathRoot;
	public static String			sourcePathName, save_sourcePathName;
	public static String			xslPathName, save_xslPathName;
	public static String			xslParams, save_xslParams;
	public static String			xslVersionName = "";		// like D1
	public static String			xslVersionDate = "";		// like versionDate
	public static String			outputPathRoot, save_outputPathRoot;
	public static String			outputPathName, save_outputPathName;
	
	public static int				continueXMLparseOnFatalError = 0;					// may be: 0 = normal, > 0 to continue: USE WITH CAUTION
	public static int				doXMLvalidating = 0;								// 0 = don't validate input XML, >0 do validate
	public static int				XMLvalidatingMaxErrorMessages = 10;					// set to -1 to supress all messages
	public static int				XMLvalidatingMaxFatalErrorMessages = 10;			// set to -1 to supress all messages
	public static int				XMLvalidatingMaxWarningMessages = 10;			// set to -1 to supress all messages


	public static String			ftpUsername, save_ftpUsername;	// FTP user pass port
	public static String			ftpPassword, save_ftpPassword;
	public static String			ftpPort = "21", save_ftpPort;
	public static String			ftpEncodingDefault = "UTF-8";	// talk this way over control connection
	public static String			ftpEncoding = ftpEncodingDefault;
	public static String			save_ftpEncoding;
	public static String			ftpType, save_ftpType;	// ftp or ftps
	public static String			ftpActivePassive, save_ftpActivePassive;	// actice or passive

	public static int				includeXSLCSSfolderCopy = 0;			// set to 1 to to also FTP transfer XSLCSS folder copyFile()

	public static String			newoutputFileNameExt = "", save_newoutputFileNameExt;
	public static String			excludeCleanupRunFileNameExts = "", save_excludeCleanupRunFileNameExts = "";
	public static String			excludeSourceProcessingRunFileNameExts = "", save_excludeSourceProcessingRunFileNameExts = "";
	public static String			excludeSourceProcessingRunFileNames = "", save_excludeSourceProcessingRunFileNames = "";
	public static String			sourceFileAction = "", save_sourceFileAction;
	public static int				deleteSourceDirs = 0, save_deleteSourceDirs;
	public static int				loopDelay = 3000, save_loopDelay;		// how long to sleep before looping over in ms (3 secs)

	public static String			jobticketVersion = "10.0";
	public static String			jobTicketFileName = "";
	public static String			nextJobTicketFileName = "", save_nextJobTicketFileName = "";
	public static String			mainJobTicketPath = "";			// the main path where to look for jobtickets. usually: ~/BatchXSLT4InDesign/BatchXSLT/
	public static String			nextJobTicketPath = "";
	public static String			currentJobTicketPath = "";
	public static String			last_OK_JobTicketFileName = "";

	public static boolean			jobticket_is_loading = false;	// prevent from someone disturbing the jobTicket load
	public static boolean			jobticket_loaded = false;
	public static InputStream		jobTicketFile = null;
	public static int				writeCommPrefs = 0;		// write a communications prefs file
	public static String			runningLockFileName = "batchxsltisrunning";	// the file which says that BatchXSLT is already running (for windows)

	public static String			folderIndexFileName = "", save_folderIndexFileName;
	public static String			folderIndexFileXSLName = "", save_folderIndexFileXSLName;

	public static int				numTransformsDone = 0;	// how many transform we have done since startup

	public static boolean			processing_disabled = false;
	public static boolean			general_abort = false;
	public static int				start_over = 1, old_start_over;	// 1 == first start,  >1 == reactivated by a 'nextjobticket' call
	public static boolean					processing = false;	//	true if do_mainLoop() is running: we are transforming
	public boolean					fileCopyInProgress = false;	// we do a fileCopy (like FTP)

	boolean							ttipstate = true;

	public static String			runOnLoadedJobTicketMethod = "";	// internal java method to run when a Jobticket is loaded
	public static String			runBeforeJobMethod = "";	// internal java method to run before a Jobticket is started
	public static String			runAfterJobMethod = "";	// internal java method to run after JobTicket stuff is done

	public static String			runBeforeJobApp = "";	// command BatchXSLT to run an external Application or Script before a Jobticket is started
															// called by an application caller string, a comma separated string 'applString' described below:
															// applString ="app_path,params,envir,workdir,convertParamsToNativeCharset,waitComplete"
															// where:
															// app_path = full path/name to application to call
															// params = space separated list of parameters
															// envir = a "+++" separated list of environment variables to set like path=mypath+++var1=myvarstring1+++var2=myvarstring2
															// workdir = path of working directory to set
															// convertParamsToNativeCharset = convert paths to native charset or not
															// waitComplete = "0" to NOT to wait for application completed, otherwise: wait for completion
															// 
															// multiple application commands can be defined separated by '/#/'
	public static String			runAfterJobApp = "";	// command BatchXSLT to run an external Application or Script all JobTicket stuff is done
	public static String			runBeforeTransformApp = "";	// command BatchXSLT to run an external Application or Script before a transform is started
	public static String			runAfterTransformApp = "";	// command BatchXSLT to run an external Application or Script after a transform has completed

															// IDML package related vars
	public static int				IDMLprocessingMessages = 0;			// processing messages: 0 = no messages, 1=normal messages, 2=error messages only
	public static int				IDMLpreserveExpandedPackage = 0;	// set to 1 to preserve intermediary XML file merged from an IDML package

	public static long				externalProcessTimeout = 300000L;   // set to 5 mins as default
	
	
	public static String			transformBtn_ToolTipText_stopped = "<html>Start the Transform<br>or drop an XML file to transform.</html>";
	public static String			transformBtn_ToolTipText_running = "Stop running Transform";

	public static Calendar			cd;
	public static String			ct;

	public	static SortedMap		allAvailableCharsets = null;

	static double memmax = Runtime.getRuntime().maxMemory() / 1048576.0;
	static double mempeak = 0.0;
	static String basememlblStr = "Current Free/Total Memory in MB";

	
	String	modeChoice0 = "Manual (single shot)";
	String	modeChoice1 = "Once (on start)";
	String	modeChoice2 = "Once (quit when done)";
	String	modeChoice3 = "Batch (loop for ever)";
	public static int maxModeChoices = 3;		// possible max mode choice index starting at 0

	String	sourceFileAction0 = "Delete";
	String	sourceFileAction1 = "Nothing";
	String	sourceFileAction2 = "Move to Folder or FTP:";

	public SleeperThread			mySleeperThread = null;
	public boolean					sleeperAborted = false;
	public JTOverrideThread			myJTOverrideThread = null;
	public static int				jtoverrideSleepTime = 2000;		// how long to sleep before checking for override.jt
	public static String			jtoverrideName = "override.jt";	// name of jobticket override
	public static String			jtoverrideQueuePath = "";	// default application location, otherwise any path
	public static String			jtoverrideQueueName = "override.que";	// name of jobticket override que file
	public static boolean			exitFrom_jtoverrideQueue = false;	// queue file had entry line: "exit,exitcode". if set to true application will be forced to exit
	public static int				exitFrom_jtoverrideQueue_exitcode = 0;	// queue file had entry line: "exit,exitcode".
	public static String			jobticketsPackagePath = "jobtickets/";	// subpath of predefined Jobtickets within software package
	public static String			jobticketsWorkingPath = "~/jobtickets/";	// path to store working Jobtickets
	public static String			jobticketsSubPath = "jobtickets/";	// subpath of predefined Jobtickets within jtoverrideQueuePath
	public static String			commPrefsSubPath = "comm/";		// subpath of comm.prefs file within jtoverrideQueuePath
	public static String			commPrefsName = "comm.prefs";	// name of comm.prefs file

	public static String			transformStausMessage = "idle";	// set by BatchXSLTrans to 'busy' or 'idle'




	static javax.swing.JFrame global_frame = null;
	private static java.awt.Color normal_color = new java.awt.Color(20, 20, 20);
	private static java.awt.Color red_color = new java.awt.Color(255, 90, 40);
	private static java.awt.Color green_color = new java.awt.Color(0, 160, 0);
	private static java.awt.Color fieldBackground_color = new java.awt.Color(240, 240, 240);
	private static java.awt.Color transparent_color = new java.awt.Color(0, 0, 0, 0);
	private static java.awt.Color border_color = new java.awt.Color(0, 51, 102);

	private static java.awt.Color popBtnBorder_deactivated = new java.awt.Color(140, 140, 140);
	private static java.awt.Color popBtnBorder_activated = new java.awt.Color(51, 102, 153);
	private static java.awt.Color popBtnColor_deactivated = new java.awt.Color(100, 100, 100);
	private static java.awt.Color popBtnColor_activated = new java.awt.Color(0, 51, 102);



	/***************************************************************/
	/**
	 * our main window stuff
	 */
	java.awt.Dimension window_initial_Dimension = null;
	java.awt.Dimension window_current_Dimension = null;
	int topFrameMenubarHeight = 27;	// fine for OSX we will change it for windows
	
	
	
	
	
	// ==============================
	// member declarations icons
	javax.swing.JLabel batchxsltLbl = new javax.swing.JLabel();
	javax.swing.ImageIcon batchxsltImageIcon = null;

	// member declarations icons
	javax.swing.JLabel timeLbl = new javax.swing.JLabel();
	javax.swing.JLabel memLbl = new javax.swing.JLabel();
	javax.swing.JLabel modeLbl = new javax.swing.JLabel();
	javax.swing.JComboBox modeCBx = new javax.swing.JComboBox();
	javax.swing.JLabel runMessageLbl = new javax.swing.JLabel();

	javax.swing.JLabel sourcePathNameLbl = new javax.swing.JLabel();
	javax.swing.JTextField sourcePathNameFld = new javax.swing.JTextField();
	javax.swing.JButton sourcePathNameChooseBtn = new javax.swing.JButton();
	javax.swing.JLabel xslPathNameLbl = new javax.swing.JLabel();
	javax.swing.JTextField xslPathNameFld = new javax.swing.JTextField();
	javax.swing.JButton xslPathNameChooseBtn = new javax.swing.JButton();
	javax.swing.JLabel xslt_paramsLbl = new javax.swing.JLabel();
	javax.swing.JScrollPane xslt_paramsScrollPane = new javax.swing.JScrollPane();
	javax.swing.JTextArea xslt_paramsArea = new javax.swing.JTextArea();


	javax.swing.JLabel outputPathNameLbl = new javax.swing.JLabel();
	javax.swing.JTextField outputPathNameFld = new javax.swing.JTextField();
	javax.swing.JButton outputPathNameChooseBtn = new javax.swing.JButton();

	javax.swing.JLabel usernameLbl = new javax.swing.JLabel();
	javax.swing.JTextField usernameFld = new javax.swing.JTextField();
	javax.swing.JLabel passwordLbl = new javax.swing.JLabel();
	javax.swing.JPasswordField passwordFld = new javax.swing.JPasswordField();
	javax.swing.JLabel portLbl = new javax.swing.JLabel();
	javax.swing.JTextField portFld = new javax.swing.JTextField();
	javax.swing.JComboBox ftpEncodingCBx = new javax.swing.JComboBox();

	javax.swing.JLabel newoutputFileNameExtLbl = new javax.swing.JLabel();
	javax.swing.JTextField newoutputFileNameExtFld = new javax.swing.JTextField();

	javax.swing.JLabel excludeCleanupRunExtsLbl = new javax.swing.JLabel();
	javax.swing.JLabel excludeCleanupRunExtsLbl2 = new javax.swing.JLabel();
	javax.swing.JTextField excludeCleanupRunExtsFld = new javax.swing.JTextField();
	javax.swing.JLabel excludeSourceProcessingRunExtsLbl = new javax.swing.JLabel();
	javax.swing.JLabel excludeSourceProcessingRunExtsLbl2 = new javax.swing.JLabel();
	javax.swing.JTextField excludeSourceProcessingRunExtsFld = new javax.swing.JTextField();

	javax.swing.JLabel sourceFileActionLbl = new javax.swing.JLabel();
	javax.swing.JLabel sourceFileActionLbl2 = new javax.swing.JLabel();
	javax.swing.JComboBox sourceFileActionCBx = new javax.swing.JComboBox();
	javax.swing.JTextField sourceFileActionFld = new javax.swing.JTextField();
	javax.swing.JButton sourceFileActionChooseBtn = new javax.swing.JButton();
	javax.swing.JCheckBox deleteSourceDirsCheckBox = new javax.swing.JCheckBox();

	javax.swing.JLabel nextJTNameLbl = new javax.swing.JLabel();
	javax.swing.JTextField nextJTNameFld = new javax.swing.JTextField();
	javax.swing.JButton nextJTNameChooseBtn = new javax.swing.JButton();
	javax.swing.JLabel delayJTLbl = new javax.swing.JLabel();
	javax.swing.JLabel delayJTLbl2 = new javax.swing.JLabel();
	javax.swing.JTextField delayJTFld = new javax.swing.JTextField();
	javax.swing.JLabel jt_scheduleLbl = new javax.swing.JLabel();
	javax.swing.JTextField jt_scheduleFld = new javax.swing.JTextField();

	javax.swing.JLabel ImageMagickPathNameLbl = new javax.swing.JLabel();
	javax.swing.JTextField ImageMagickPathNameFld = new javax.swing.JTextField();
	javax.swing.JButton ImageMagickPathNameChooseBtn = new javax.swing.JButton();
	javax.swing.JLabel GhostscriptPathNameLbl = new javax.swing.JLabel();
	javax.swing.JTextField GhostscriptPathNameFld = new javax.swing.JTextField();
	javax.swing.JButton GhostscriptPathNameChooseBtn = new javax.swing.JButton();

	javax.swing.JScrollPane messageAreaScrollPane = new javax.swing.JScrollPane();
	javax.swing.JTextArea messageArea = new javax.swing.JTextArea();

	javax.swing.JButton transformBtn = new javax.swing.JButton();
	javax.swing.JButton loadJTBtn = new javax.swing.JButton();
	javax.swing.JButton saveJTBtn = new javax.swing.JButton();
	javax.swing.JButton quitBtn = new javax.swing.JButton();
	javax.swing.JButton showLogfileBtn = new javax.swing.JButton();
	javax.swing.JButton tTiponoffBtn = new javax.swing.JButton();

	javax.swing.JLabel sourcePopBtn = new javax.swing.JLabel();
	javax.swing.JLabel xslPopBtn = new javax.swing.JLabel();
	javax.swing.JLabel outputPopBtn = new javax.swing.JLabel();
	javax.swing.JLabel cleanupPopBtn = new javax.swing.JLabel();
	javax.swing.JLabel nextjobticketPopBtn = new javax.swing.JLabel();
	javax.swing.JLabel imagePopBtn = new javax.swing.JLabel();
/*
	javax.swing.JLabel folderindexnameLbl = new javax.swing.JLabel();
	javax.swing.JTextField folderindexnameFld = new javax.swing.JTextField();
	javax.swing.JLabel folderindexXSLLbl = new javax.swing.JLabel();
	javax.swing.JTextField folderindexXSLFld = new javax.swing.JTextField();
	javax.swing.JButton folderindexXSLChooseBtn = new javax.swing.JButton();
*/



	// the global pane containing all sub panes
	JPanel globalPane = new JPanel();

	// declare the popup panels stuff
	JPanel sourcePane = new JPanel();
	JPanel xslPane = new JPanel();
    JPanel outputPane = new JPanel();
	JPanel cleanupPane = new JPanel();
	JPanel nextjobticketPane = new JPanel();
	JPanel imagePane = new JPanel();


	// and for user defined controls
	JPanel userControlsPaneContainer = new JPanel();
	boolean userControlsPaneContainerIsVisible = true;

	Border userControlsPaneContainerIsVisibleBorder = BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(" Application Options [ Click to hide ] "),
                        BorderFactory.createEmptyBorder(1,1,1,1));
	Border userControlsPaneContainerIsInvisibleBorder = BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(" Application Options [ Click to show ] "),
                        BorderFactory.createEmptyBorder(1,1,1,1));

	JPanel userControlsPane = new JPanel();
	// END member declarations
	// ==============================





	/**
	 * Returns an ImageIcon, or null if the path was invalid.
	 */
	protected ImageIcon createImageIcon(String path, String description) {
		// first check the jar package
		java.net.URL imgURL = this.getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		}
		return(null);
	}


	/**
	 * Definition and init of dialog components.
         * @throws java.lang.Exception
	 */
	public void initComponents() throws Exception {
		if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
			topFrameMenubarHeight += 5;
		}
		// define components
		timeLbl.setText("00:00:00");
		timeLbl.setForeground(java.awt.Color.blue);
		timeLbl.setLocation(new java.awt.Point(10, 4));
		timeLbl.setVisible(true);
		timeLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		timeLbl.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		timeLbl.setToolTipText("Current System Time");
		timeLbl.setSize(new java.awt.Dimension(56, 11));
		memLbl.setForeground(new java.awt.Color(20, 20, 100));
		memLbl.setLocation(new java.awt.Point(0, 16));
		memLbl.setVisible(true);
		memLbl.setFont(new java.awt.Font("SansSerif", 0, 9));
		memLbl.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		memLbl.setToolTipText(basememlblStr);
		memLbl.setSize(new java.awt.Dimension(72, 11));
		modeLbl.setText("Mode:");
		modeLbl.setForeground(new java.awt.Color(20, 20, 20));
		modeLbl.setLocation(new java.awt.Point(67, 7));
		modeLbl.setVisible(true);
		modeLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		modeLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		modeLbl.setToolTipText("The operation Mode (Manual/Batch/Quit) for this JobTicket");
		modeLbl.setSize(new java.awt.Dimension(35, 14));
		modeCBx.setLocation(new java.awt.Point(105, 5));
		modeCBx.setVisible(true);
		modeCBx.setFont(new java.awt.Font("Dialog", 0, 10));
		modeCBx.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">Choose an operation Mode</p><hr> - <b>Manual</b> to do a single cycle and then wait<br> - <b>Batch</b> to continually scan a Source folder<br> - <b>Quit</b> to perform the choosen action(s) and then quit</html>");
		modeCBx.setSize(new java.awt.Dimension(150, 22));
		runMessageLbl.setForeground(java.awt.Color.blue);
		runMessageLbl.setLocation(new java.awt.Point(258, 7));
		runMessageLbl.setSize(new java.awt.Dimension(160, 16));
		runMessageLbl.setVisible(true);
		runMessageLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		runMessageLbl.setToolTipText("<html>Messages about the current state or<br>the currently performed action</html>");
		//runMessageLbl.setBorder(new LineBorder(java.awt.Color.blue, 1));


		batchxsltImageIcon = createImageIcon("/image/" + applLogoName,applFullName);	// try for BatchXSLT
		if (batchxsltImageIcon == null) {
			batchxsltImageIcon = createImageIcon("/image/" + applLogoName2,applFullName2);	// try for easepub
			if (batchxsltImageIcon == null) {
				batchxsltImageIcon = createImageIcon("/image/" + applFullName+".png",applFullName);	// try for name set from autostart.jt
				if (batchxsltImageIcon == null) {
					batchxsltImageIcon = createImageIcon("/image/logo.png",applFullName);	// try for this
				}
			}
			else {	// easepub.png found
				applFullName = applFullName2;
				applLogoName = applLogoName2;
				applShortName = "ezPub";
				applLicenseShortcut = "ep";				// the id for licenses
				default_logfile_name = logfile_prefix2 + "_log.txt";		// the name of log file easepub_log.txt
			}
		}

		if (batchxsltImageIcon != null) {
			batchxsltLbl.setIcon(batchxsltImageIcon);
			int batchxsltIconWidth = batchxsltImageIcon.getIconWidth();
			int batchxsltIconHeight = batchxsltImageIcon.getIconHeight();
			// logoPane has width 128 pixels. we want to horizontally center the logo
			int xpos = (128 - batchxsltIconWidth) / 2;
			batchxsltLbl.setLocation(new java.awt.Point(xpos, 0));
			batchxsltLbl.setSize(new java.awt.Dimension(batchxsltIconWidth, batchxsltIconHeight));
			batchxsltLbl.setVisible(true);
			String ttip = "<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">Version: " + applMajorVersion + "." + applMinorVersion + "</p><hr><center>Press [Alt]-key and click to read the<br><b>Online Manual</b></center></html>";
			batchxsltLbl.setToolTipText(ttip);
			//batchxsltLbl.setBorder(new LineBorder(java.awt.Color.red, 1));
		}

		

		sourcePathNameLbl.setText("Source Path/Name:");
		sourcePathNameLbl.setForeground(new java.awt.Color(20, 20, 20));
		sourcePathNameLbl.setLocation(new java.awt.Point(10, 13));
		sourcePathNameLbl.setVisible(true);
		sourcePathNameLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		sourcePathNameLbl.setHorizontalAlignment(javax.swing.JLabel.LEFT);
		sourcePathNameLbl.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">A path to the file(s) to process</p><hr> - either a directory path only or<br> - a path/filename of a single XML file</html>");
		sourcePathNameLbl.setSize(new java.awt.Dimension(100, 12));
		//sourcePathNameLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		sourcePathNameFld.setLocation(new java.awt.Point(10, 36));
		sourcePathNameFld.setSize(new java.awt.Dimension(383, 18));
		sourcePathNameFld.setVisible(true);
		sourcePathNameFld.setBackground(new java.awt.Color(240, 240, 240));
		sourcePathNameFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		sourcePathNameFld.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">The path to the XML file(s) to process</p><hr> - either a directory path only or<br> - a path/filename of a single file<br><b>You may drop here an XML file or a folder to be processed</b></html>");
		sourcePathNameFld.setDragEnabled(true);
		sourcePathNameFld.setTransferHandler(new TransferHandler("text"));
		sourcePathNameChooseBtn.setText("...");
		sourcePathNameChooseBtn.setLocation(new java.awt.Point(393, 36));
		sourcePathNameChooseBtn.setVisible(true);
		sourcePathNameChooseBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		sourcePathNameChooseBtn.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">Choose XML file(s) to process</p><hr> - either a single XML file or<br> - a directory containing the file(s) to process</html>");
		sourcePathNameChooseBtn.setSize(new java.awt.Dimension(25, 18));
		sourcePathNameChooseBtn.setFocusPainted(false);

		xslPathNameLbl.setText("XSL Path/Name:");
		xslPathNameLbl.setForeground(new java.awt.Color(20, 20, 20));
		xslPathNameLbl.setLocation(new java.awt.Point(2, 13));
		xslPathNameLbl.setVisible(true);
		xslPathNameLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		xslPathNameLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		xslPathNameLbl.setToolTipText("<html>The XSL file to use to transform the source file(s)<br>or empty if the XSL is given in the source XML file</html>");
		xslPathNameLbl.setSize(new java.awt.Dimension(100, 12));
		xslPathNameLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		xslPathNameFld.setLocation(new java.awt.Point(105, 11));
		xslPathNameFld.setVisible(true);
		xslPathNameFld.setBackground(new java.awt.Color(240, 240, 240));
		xslPathNameFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		xslPathNameFld.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">XSL Stylesheet file name to use</p><hr><b>You may drop here an XSL file to use</b><br> - empty if the XSL stated in the source XML should be used<br> - enter <b>*NONE*</b> to make no XSL transformation<br>    (do file transfer only)</html>");
		xslPathNameFld.setSize(new java.awt.Dimension(289, 18));
		xslPathNameFld.setDragEnabled(true);
		xslPathNameChooseBtn.setText("...");
		xslPathNameChooseBtn.setLocation(new java.awt.Point(393, 11));
		xslPathNameChooseBtn.setVisible(true);
		xslPathNameChooseBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		xslPathNameChooseBtn.setToolTipText("<html>Choose the XSL file to use<br>to transform the source file(s)</html>");
		xslPathNameChooseBtn.setSize(new java.awt.Dimension(25, 18));
		xslPathNameChooseBtn.setFocusPainted(false);

		xslt_paramsLbl.setText("XSL Parameters:");
		xslt_paramsLbl.setForeground(new java.awt.Color(20, 20, 20));
		xslt_paramsLbl.setLocation(new java.awt.Point(2, 31));
		xslt_paramsLbl.setVisible(true);
		xslt_paramsLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		xslt_paramsLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		xslt_paramsLbl.setToolTipText("The parameters to pass to the XSL style sheet");
		xslt_paramsLbl.setSize(new java.awt.Dimension(100, 12));
		xslt_paramsLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		xslt_paramsScrollPane.setLocation(new java.awt.Point(105, 29));
		xslt_paramsScrollPane.setAutoscrolls(true);
		xslt_paramsScrollPane.setVisible(true);
		xslt_paramsScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		xslt_paramsScrollPane.setToolTipText("scroll through parameters passed to XSL Style sheet");
		xslt_paramsScrollPane.setSize(new java.awt.Dimension(313, 160));
		xslt_paramsArea.setVisible(true);
		xslt_paramsArea.setBackground(new java.awt.Color(240, 240, 240));
		xslt_paramsArea.setFont(new java.awt.Font("SansSerif", 0, 10));
		xslt_paramsArea.setTabSize(5);
		xslt_paramsArea.setToolTipText("<html>Enter parameters to be passed to<br>the XSL style sheet as 'parameterName=value'</html>");

		outputPathNameLbl.setText("Output Path/Name:");
		outputPathNameLbl.setForeground(new java.awt.Color(20, 20, 20));
		outputPathNameLbl.setLocation(new java.awt.Point(2, 13));
		outputPathNameLbl.setVisible(true);
		outputPathNameLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		outputPathNameLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		outputPathNameLbl.setToolTipText("The path or path/name or FTP URL for output file(s)");
		outputPathNameLbl.setSize(new java.awt.Dimension(100, 12));
		outputPathNameLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		outputPathNameFld.setLocation(new java.awt.Point(105, 11));
		outputPathNameFld.setVisible(true);
		outputPathNameFld.setBackground(new java.awt.Color(240, 240, 240));
		outputPathNameFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		outputPathNameFld.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">The target path to store output file's</p><hr> - empty to use the Source Path<br> - a local path or<br> - an FTP URL like 'ftp:// 123.1.2.3/path/'<br> - <b>*NONE*</b> for no output file<br>     (output is transfered into a database)</html>");
		outputPathNameFld.setSize(new java.awt.Dimension(289, 18));
		outputPathNameFld.setDragEnabled(true);
		outputPathNameChooseBtn.setText("...");
		outputPathNameChooseBtn.setLocation(new java.awt.Point(393, 11));
		outputPathNameChooseBtn.setVisible(true);
		outputPathNameChooseBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		outputPathNameChooseBtn.setToolTipText("<html>Choose the path to store output files or<br>a path/name for a single output file</html>");
		outputPathNameChooseBtn.setSize(new java.awt.Dimension(25, 18));
		outputPathNameChooseBtn.setFocusPainted(false);

		usernameLbl.setText("User Name:");
		usernameLbl.setForeground(new java.awt.Color(20, 20, 20));
		usernameLbl.setLocation(new java.awt.Point(2, 37));
		usernameLbl.setVisible(true);
		usernameLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		usernameLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		usernameLbl.setToolTipText("<html>The FTP Server user's login name<br>if 'Output Path/Name' is an FTP URL</html>");
		usernameLbl.setSize(new java.awt.Dimension(100, 12));
		usernameLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		usernameFld.setLocation(new java.awt.Point(105, 34));
		usernameFld.setVisible(true);
		usernameFld.setBackground(new java.awt.Color(240, 240, 240));
		usernameFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		usernameFld.setToolTipText("<html>The FTP Server user login name<br>'anonymous' or empty for anonymous login as Guest</html>");
		usernameFld.setSize(new java.awt.Dimension(55, 18));
		passwordLbl.setText("Pass:");
		passwordLbl.setForeground(new java.awt.Color(20, 20, 20));
		passwordLbl.setLocation(new java.awt.Point(160, 37));
		passwordLbl.setVisible(true);
		passwordLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		passwordLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		passwordLbl.setToolTipText("<html>The FTP Server user's target login password<br>if 'Output Path/Name' is an FTP URL<html>");
		passwordLbl.setSize(new java.awt.Dimension(34, 12));
		passwordLbl.setFocusable(true);
		passwordFld.setLocation(new java.awt.Point(198, 34));
		passwordFld.setVisible(true);
		passwordFld.setBackground(new java.awt.Color(240, 240, 240));
		passwordFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		passwordFld.setToolTipText("The FTP Server user's password");
		passwordFld.setSize(new java.awt.Dimension(60, 18));
		portLbl.setText("Port:");
		portLbl.setForeground(new java.awt.Color(20, 20, 20));
		portLbl.setLocation(new java.awt.Point(258, 37));
		portLbl.setVisible(true);
		portLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		portLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		portLbl.setToolTipText("<html>The FTP Control Connection port number to use<br>default FTP = 21</html>");
		portLbl.setSize(new java.awt.Dimension(34, 12));
//		portLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		portFld.setLocation(new java.awt.Point(297, 34));
		portFld.setVisible(true);
		portFld.setBackground(new java.awt.Color(240, 240, 240));
		portFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		portFld.setToolTipText("<html>The FTP Control Connection port number to use<br>default FTP = 21</html>");
		portFld.setSize(new java.awt.Dimension(30, 18));
		ftpEncodingCBx.setLocation(new java.awt.Point(330, 33));
		ftpEncodingCBx.setVisible(true);
		ftpEncodingCBx.setFont(new java.awt.Font("Dialog", 0, 10));
		ftpEncodingCBx.setToolTipText("<html>The Encoding to use to talk with the<br>FTP server on the Control Connection</html>");
		ftpEncodingCBx.setSize(new java.awt.Dimension(91, 22));
		ftpEncodingCBx.setMaximumRowCount(20);
		//ftpEncodingCBx.setEditable(true);

		newoutputFileNameExtLbl.setText("New Output Filename Extension:");
		newoutputFileNameExtLbl.setForeground(new java.awt.Color(20, 20, 20));
		newoutputFileNameExtLbl.setLocation(new java.awt.Point(2, 73));
		newoutputFileNameExtLbl.setVisible(true);
		newoutputFileNameExtLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		newoutputFileNameExtLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		newoutputFileNameExtLbl.setToolTipText("The (new) file name extension of output files");
		newoutputFileNameExtLbl.setSize(new java.awt.Dimension(170, 14));
		newoutputFileNameExtLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		//newoutputFileNameExtLbl.setBorder(new LineBorder(java.awt.Color.black, 1));
		newoutputFileNameExtFld.setLocation(new java.awt.Point(174, 71));
		newoutputFileNameExtFld.setVisible(true);
		newoutputFileNameExtFld.setBackground(new java.awt.Color(240, 240, 240));
		newoutputFileNameExtFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		newoutputFileNameExtFld.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">New output file name extension</p><hr>Ex.: Set to '.html' if the XSL transform creates an html output format.<br>Leave empty to keep the original name untouched</html>");
		newoutputFileNameExtFld.setSize(new java.awt.Dimension(46, 18));



		sourceFileActionLbl.setVerticalAlignment(javax.swing.JLabel.TOP);
		sourceFileActionLbl.setText("Action to perform on");
		sourceFileActionLbl.setForeground(new java.awt.Color(20, 20, 20));
		sourceFileActionLbl.setLocation(new java.awt.Point(10, 12));
		sourceFileActionLbl.setVisible(true);
		sourceFileActionLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		sourceFileActionLbl.setVerticalTextPosition(javax.swing.JLabel.TOP);
		sourceFileActionLbl.setToolTipText("<html>What to do with a Source file<br>after having processed it</html>");
		sourceFileActionLbl.setSize(new java.awt.Dimension(130, 14));
		sourceFileActionLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		sourceFileActionLbl2.setVerticalAlignment(javax.swing.JLabel.TOP);
		sourceFileActionLbl2.setText("Source file(s) after done:");
		sourceFileActionLbl2.setForeground(new java.awt.Color(20, 20, 20));
		sourceFileActionLbl2.setLocation(new java.awt.Point(10, 22));
		sourceFileActionLbl2.setVisible(true);
		sourceFileActionLbl2.setFont(new java.awt.Font("SansSerif", 0, 10));
		sourceFileActionLbl2.setVerticalTextPosition(javax.swing.JLabel.TOP);
		sourceFileActionLbl2.setToolTipText("<html>What to do with a Source file<br>after having processed it</html>");
		sourceFileActionLbl2.setSize(new java.awt.Dimension(130, 14));
		sourceFileActionLbl2.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		sourceFileActionCBx.setLocation(new java.awt.Point(145, 12));
		sourceFileActionCBx.setVisible(true);
		sourceFileActionCBx.setFont(new java.awt.Font("Dialog", 0, 10));
		sourceFileActionCBx.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">Action to perform after transform</p><hr>Processed source files may be:<br> - <b>Delete</b> (trash them) <b>CAUTION!!</b><br> - <b>Nothing</b> (leave them where they are)<br> - <b>Move</b> (to folder or send by FTP)</html>");
		sourceFileActionCBx.setSize(new java.awt.Dimension(172, 22));
		sourceFileActionFld.setLocation(new java.awt.Point(10, 36));
		sourceFileActionFld.setSize(new java.awt.Dimension(383, 18));
		sourceFileActionFld.setVisible(true);
		sourceFileActionFld.setBackground(new java.awt.Color(240, 240, 240));
		sourceFileActionFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		sourceFileActionFld.setToolTipText("<html>The path to move processed file(s) to<br>or an FTP URL like 'ftp:// 123.1.2.3/path/'<br>For FTP, set user name at the Output tab</html>");
		sourceFileActionFld.setDragEnabled(true);
		sourceFileActionChooseBtn.setText("...");
		sourceFileActionChooseBtn.setLocation(new java.awt.Point(393, 36));
		sourceFileActionChooseBtn.setVisible(true);
		sourceFileActionChooseBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		sourceFileActionChooseBtn.setToolTipText("Choose the path to move file(s) to after having processed them");
		sourceFileActionChooseBtn.setSize(new java.awt.Dimension(25, 18));
		sourceFileActionChooseBtn.setFocusPainted(false);

		excludeCleanupRunExtsLbl.setText("Exclude Files");
		excludeCleanupRunExtsLbl.setForeground(new java.awt.Color(20, 20, 20));
		excludeCleanupRunExtsLbl.setLocation(new java.awt.Point(10, 70));
		excludeCleanupRunExtsLbl.setVisible(true);
		excludeCleanupRunExtsLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		excludeCleanupRunExtsLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		excludeCleanupRunExtsLbl.setToolTipText("<html>Name Extensions for files to exclude from processing<br>during Clean Up run (files not to leave in place)</html>");
		excludeCleanupRunExtsLbl.setSize(new java.awt.Dimension(95, 14));
		excludeCleanupRunExtsLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		excludeCleanupRunExtsLbl2.setText("from Clean Up:");
		excludeCleanupRunExtsLbl2.setForeground(new java.awt.Color(20, 20, 20));
		excludeCleanupRunExtsLbl2.setLocation(new java.awt.Point(10, 80));
		excludeCleanupRunExtsLbl2.setVisible(true);
		excludeCleanupRunExtsLbl2.setFont(new java.awt.Font("SansSerif", 0, 10));
		excludeCleanupRunExtsLbl2.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		excludeCleanupRunExtsLbl2.setToolTipText("<html>Name Extensions for files to exclude from processing<br>during Clean Up run (files not to leave in place)</html>");
		excludeCleanupRunExtsLbl2.setSize(new java.awt.Dimension(95, 14));
		excludeCleanupRunExtsLbl2.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		excludeCleanupRunExtsFld.setSize(new java.awt.Dimension(200, 18));
		excludeCleanupRunExtsFld.setLocation(new java.awt.Point(115, 72));
		excludeCleanupRunExtsFld.setBackground(new java.awt.Color(240, 240, 240));
		excludeCleanupRunExtsFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		excludeCleanupRunExtsFld.setVisible(true);
		excludeCleanupRunExtsFld.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">Exclude files from being moved</p><hr>Enter filename extensions to be excluded from clean up processing<br>Separate each extension with a comma:<br>Ex: .epp,.xsl,abc,myfile.abc</html>");

		excludeSourceProcessingRunExtsLbl.setText("Exclude Files");
		excludeSourceProcessingRunExtsLbl.setForeground(new java.awt.Color(20, 20, 20));
		excludeSourceProcessingRunExtsLbl.setLocation(new java.awt.Point(10, 70));
		excludeSourceProcessingRunExtsLbl.setVisible(true);
		excludeSourceProcessingRunExtsLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		excludeSourceProcessingRunExtsLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		excludeSourceProcessingRunExtsLbl.setToolTipText("<html>Name Extensions for files to exclude<br>from transform process (files not to be touched)</html>");
		excludeSourceProcessingRunExtsLbl.setSize(new java.awt.Dimension(95, 14));
		excludeSourceProcessingRunExtsLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		excludeSourceProcessingRunExtsLbl2.setText("from processing:");
		excludeSourceProcessingRunExtsLbl2.setForeground(new java.awt.Color(20, 20, 20));
		excludeSourceProcessingRunExtsLbl2.setLocation(new java.awt.Point(10, 80));
		excludeSourceProcessingRunExtsLbl2.setVisible(true);
		excludeSourceProcessingRunExtsLbl2.setFont(new java.awt.Font("SansSerif", 0, 10));
		excludeSourceProcessingRunExtsLbl2.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		excludeSourceProcessingRunExtsLbl2.setToolTipText("<html>Name Extensions for files to exclude<br>from transform process (files not to be touched)</html>");
		excludeSourceProcessingRunExtsLbl2.setSize(new java.awt.Dimension(95, 14));
		excludeSourceProcessingRunExtsLbl2.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		excludeSourceProcessingRunExtsFld.setSize(new java.awt.Dimension(200, 18));
		excludeSourceProcessingRunExtsFld.setLocation(new java.awt.Point(115, 72));
		excludeSourceProcessingRunExtsFld.setBackground(new java.awt.Color(240, 240, 240));
		excludeSourceProcessingRunExtsFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		excludeSourceProcessingRunExtsFld.setVisible(true);
		excludeSourceProcessingRunExtsFld.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">Exclude files from being transformed</p><hr>Enter filename extensions to be excluded from processing<br>Separate each extension with a comma:<br>Ex: .xsl,_int.xml</html>");

		deleteSourceDirsCheckBox.setText("Delete empty Source Folders");
		deleteSourceDirsCheckBox.setLocation(new java.awt.Point(10, 105));
		deleteSourceDirsCheckBox.setVisible(true);
		deleteSourceDirsCheckBox.setFont(new java.awt.Font("SansSerif", 0, 10));
		deleteSourceDirsCheckBox.setToolTipText("<html><p style=\"text-align:center;font-size:11pt;font-weight:bold;color:#003366\">Delete empty Source Folders</p><hr>Check if empty folders in the source path should be DELETED.<br>Uncheck if the Source path should NOT be removed");
		deleteSourceDirsCheckBox.setFocusPainted(false);
		deleteSourceDirsCheckBox.setSize(new java.awt.Dimension(183, 18));

/*
		folderindexnameLbl.setText("Folder Index Name:");
		folderindexnameLbl.setForeground(new java.awt.Color(20, 20, 20));
		folderindexnameLbl.setLocation(new java.awt.Point(2, 267));
		folderindexnameLbl.setVisible(true);
		folderindexnameLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		folderindexnameLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		folderindexnameLbl.setToolTipText("Create XML file representing folder content");
		folderindexnameLbl.setSize(new java.awt.Dimension(100, 14));
		folderindexnameLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		folderindexnameFld.setLocation(new java.awt.Point(105, 265));
		folderindexnameFld.setVisible(true);
		folderindexnameFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		folderindexnameFld.setToolTipText("<html>Folder Content File:<br>the XML file's name like 'index.xml'<br> or empty to create no index file</html>");
		folderindexnameFld.setSize(new java.awt.Dimension(111, 18));
		folderindexXSLLbl.setText("Folder Index XSL:");
		folderindexXSLLbl.setForeground(new java.awt.Color(20, 20, 20));
		folderindexXSLLbl.setLocation(new java.awt.Point(222, 267));
		folderindexXSLLbl.setVisible(true);
		folderindexXSLLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		folderindexXSLLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		folderindexXSLLbl.setToolTipText("<html>The Path/Name of the XSL file to use to<br>transform the folder Content File</html>");
		folderindexXSLLbl.setSize(new java.awt.Dimension(91, 14));
		folderindexXSLLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		folderindexXSLFld.setLocation(new java.awt.Point(317, 265));
		folderindexXSLFld.setVisible(true);
		folderindexXSLFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		folderindexXSLFld.setToolTipText("<html>The Path/Name of the XSL file to use to<br>transform the folder Content File</html>");
		folderindexXSLFld.setSize(new java.awt.Dimension(204, 18));
		folderindexXSLChooseBtn.setText("...");
		folderindexXSLChooseBtn.setLocation(new java.awt.Point(519, 265));
		folderindexXSLChooseBtn.setVisible(true);
		folderindexXSLChooseBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		folderindexXSLChooseBtn.setToolTipText("<html>Choose the Path/Name of the XSL file to use to<br>transform the folder Content File</html>");
		folderindexXSLChooseBtn.setSize(new java.awt.Dimension(25, 18));
		folderindexXSLChooseBtn.setFocusPainted(false);
*/

		nextJTNameLbl.setText("Next JobTicket:");
		nextJTNameLbl.setForeground(new java.awt.Color(20, 20, 20));
		nextJTNameLbl.setLocation(new java.awt.Point(10, 13));
		nextJTNameLbl.setVisible(true);
		nextJTNameLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		nextJTNameLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		nextJTNameLbl.setToolTipText("The path/name of the next JobTicket to call");
		nextJTNameLbl.setSize(new java.awt.Dimension(91, 14));
		nextJTNameLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		nextJTNameFld.setLocation(new java.awt.Point(105, 11));
		nextJTNameFld.setVisible(true);
		nextJTNameFld.setBackground(new java.awt.Color(240, 240, 240));
		nextJTNameFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		nextJTNameFld.setToolTipText("<html>The path/name of the next JobTicket to call<br> - leave empty for none</html>");
		nextJTNameFld.setSize(new java.awt.Dimension(288, 18));
		nextJTNameChooseBtn.setText("...");
		nextJTNameChooseBtn.setLocation(new java.awt.Point(393, 11));
		nextJTNameChooseBtn.setVisible(true);
		nextJTNameChooseBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		nextJTNameChooseBtn.setToolTipText("<html>Choose the path/name of the<br> next JobTicket to call</html>");
		nextJTNameChooseBtn.setSize(new java.awt.Dimension(25, 18));
		nextJTNameChooseBtn.setFocusPainted(false);
		delayJTLbl.setText("Delay to next");
		delayJTLbl.setForeground(new java.awt.Color(20, 20, 20));
		delayJTLbl.setLocation(new java.awt.Point(10, 40));
		delayJTLbl.setVisible(true);
		delayJTLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		delayJTLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		delayJTLbl.setToolTipText("<html>Delay before calling the next (chained) JobTicket<br>default is 3 seconds = 3000</html>");
		delayJTLbl.setSize(new java.awt.Dimension(91, 14));
		delayJTLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		delayJTLbl2.setText("JobTicket in ms:");
		delayJTLbl2.setForeground(new java.awt.Color(20, 20, 20));
		delayJTLbl2.setLocation(new java.awt.Point(10, 51));
		delayJTLbl2.setVisible(true);
		delayJTLbl2.setFont(new java.awt.Font("SansSerif", 0, 10));
		delayJTLbl2.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		delayJTLbl2.setToolTipText("<html>Delay before calling the next (chained) JobTicket<br>default is 3 seconds = 3000</html>");
		delayJTLbl2.setSize(new java.awt.Dimension(91, 14));
		delayJTLbl2.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		delayJTFld.setLocation(new java.awt.Point(105, 44));
		delayJTFld.setVisible(true);
		delayJTFld.setBackground(new java.awt.Color(240, 240, 240));
		delayJTFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		delayJTFld.setToolTipText("<html>Delay before calling the next (chained) JobTicket<br>default is 3 seconds = 3000</html>");
		delayJTFld.setSize(new java.awt.Dimension(55, 18));
		jt_scheduleLbl.setText("Schedule:");
		jt_scheduleLbl.setForeground(new java.awt.Color(20, 20, 20));
		jt_scheduleLbl.setLocation(new java.awt.Point(10, 120));
		jt_scheduleLbl.setVisible(true);
		jt_scheduleLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		jt_scheduleLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		jt_scheduleLbl.setToolTipText("The path/name of the next JobTicket to call");
		jt_scheduleLbl.setSize(new java.awt.Dimension(91, 14));
		jt_scheduleLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		jt_scheduleFld.setLocation(new java.awt.Point(105, 118));
		jt_scheduleFld.setVisible(true);
		jt_scheduleFld.setBackground(new java.awt.Color(240, 240, 240));
		jt_scheduleFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		jt_scheduleFld.setToolTipText("<html>Scheduler definition string<br> - leave empty for no scheduling</html>");
		jt_scheduleFld.setSize(new java.awt.Dimension(272, 18));

		ImageMagickPathNameLbl.setText("ImageMagick:");
		ImageMagickPathNameLbl.setForeground(new java.awt.Color(20, 20, 20));
		ImageMagickPathNameLbl.setLocation(new java.awt.Point(10, 13));
		ImageMagickPathNameLbl.setVisible(true);
		ImageMagickPathNameLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		ImageMagickPathNameLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		ImageMagickPathNameLbl.setToolTipText("Imagemagick's path/name to call as image converter");
		ImageMagickPathNameLbl.setSize(new java.awt.Dimension(91, 14));
		ImageMagickPathNameLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		ImageMagickPathNameFld.setLocation(new java.awt.Point(103, 11));
		ImageMagickPathNameFld.setVisible(true);
		ImageMagickPathNameFld.setBackground(new java.awt.Color(240, 240, 240));
		ImageMagickPathNameFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		ImageMagickPathNameFld.setToolTipText("<html>The path/name to the ImagMagick converter<br>This is used to convert TIFF, BMP<br>and other images to JPEG<br>- leave empty for no image conversion</html>");
		ImageMagickPathNameFld.setSize(new java.awt.Dimension(290, 18));
		ImageMagickPathNameChooseBtn.setText("...");
		ImageMagickPathNameChooseBtn.setLocation(new java.awt.Point(393, 11));
		ImageMagickPathNameChooseBtn.setVisible(true);
		ImageMagickPathNameChooseBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		ImageMagickPathNameChooseBtn.setToolTipText("<html>Choose the ImageMagick application<br> to use for image conversion</html>");
		ImageMagickPathNameChooseBtn.setSize(new java.awt.Dimension(25, 18));
		ImageMagickPathNameChooseBtn.setFocusPainted(false);

		GhostscriptPathNameLbl.setText("Ghostscript:");
		GhostscriptPathNameLbl.setForeground(new java.awt.Color(20, 20, 20));
		GhostscriptPathNameLbl.setLocation(new java.awt.Point(10, 43));
		GhostscriptPathNameLbl.setVisible(true);
		GhostscriptPathNameLbl.setFont(new java.awt.Font("SansSerif", 0, 10));
		GhostscriptPathNameLbl.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		GhostscriptPathNameLbl.setToolTipText("Ghostscript path/name to call as EPS image converter");
		GhostscriptPathNameLbl.setSize(new java.awt.Dimension(91, 14));
		GhostscriptPathNameLbl.setHorizontalTextPosition(javax.swing.JLabel.LEFT);
		GhostscriptPathNameFld.setLocation(new java.awt.Point(103, 41));
		GhostscriptPathNameFld.setVisible(true);
		GhostscriptPathNameFld.setBackground(new java.awt.Color(240, 240, 240));
		GhostscriptPathNameFld.setFont(new java.awt.Font("SansSerif", 0, 10));
		GhostscriptPathNameFld.setToolTipText("<html>The path/name to the Ghostscript converter<br>This is used to convert EPS images to JPEG<br>- leave empty for none (if you have no EPS images)</html>");
		GhostscriptPathNameFld.setSize(new java.awt.Dimension(290, 18));
		GhostscriptPathNameChooseBtn.setText("...");
		GhostscriptPathNameChooseBtn.setLocation(new java.awt.Point(393, 41));
		GhostscriptPathNameChooseBtn.setVisible(true);
		GhostscriptPathNameChooseBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		GhostscriptPathNameChooseBtn.setToolTipText("<html>Choose the Ghostscript application<br> to use for EPS image conversion</html>");
		GhostscriptPathNameChooseBtn.setSize(new java.awt.Dimension(25, 18));
		GhostscriptPathNameChooseBtn.setFocusPainted(false);

		// define buttons
		transformBtn.setText("Transform");
		transformBtn.setLocation(new java.awt.Point(1, 1));
		transformBtn.setSize(new java.awt.Dimension(120, 22));
		transformBtn.setVisible(true);
		transformBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		transformBtn.setToolTipText(transformBtn_ToolTipText_stopped);

		loadJTBtn.setText("Load JobTicket");
		loadJTBtn.setLocation(new java.awt.Point(1, 31));
		loadJTBtn.setSize(new java.awt.Dimension(120, 22));
		loadJTBtn.setVisible(true);
		loadJTBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		loadJTBtn.setToolTipText("<html>Choose settings from a JobTicket file<br>or drop a JobTicket file to load.</html>");
		loadJTBtn.setHorizontalTextPosition(javax.swing.JButton.CENTER);

		saveJTBtn.setText("Save JobTicket");
		saveJTBtn.setLocation(new java.awt.Point(1, 54));
		saveJTBtn.setSize(new java.awt.Dimension(120, 22));
		saveJTBtn.setVisible(true);
		saveJTBtn.setFont(new java.awt.Font("SansSerif", 0, 10));
		saveJTBtn.setToolTipText("Save current settings to a JobTicket file");

		showLogfileBtn.setText("Show Log File");
		showLogfileBtn.setLocation(new java.awt.Point(1, 86));
		showLogfileBtn.setSize(new java.awt.Dimension(120, 22));
		showLogfileBtn.setVisible(true);
		showLogfileBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		showLogfileBtn.setToolTipText("Show Console Log File");
		showLogfileBtn.setHorizontalTextPosition(javax.swing.JButton.CENTER);

		quitBtn.setText("Quit");
		quitBtn.setLocation(new java.awt.Point(1, 112));
		quitBtn.setSize(new java.awt.Dimension(120, 22));
		quitBtn.setVisible(true);
		quitBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		quitBtn.setToolTipText("Quit this application");
		quitBtn.setHorizontalTextPosition(javax.swing.JButton.CENTER);

		tTiponoffBtn.setText("?");
		tTiponoffBtn.setLocation(new java.awt.Point(1, 142));
		tTiponoffBtn.setSize(new java.awt.Dimension(120, 19));
		tTiponoffBtn.setForeground(new java.awt.Color(20, 20, 20));
		tTiponoffBtn.setVisible(true);
		tTiponoffBtn.setContentAreaFilled(false);
		tTiponoffBtn.setFont(new java.awt.Font("SansSerif", 1, 13));
		tTiponoffBtn.setVerticalTextPosition(javax.swing.JButton.TOP);
		tTiponoffBtn.setToolTipText("ToolTips on/off");
		//tTiponoffBtn.setBorder(new LineBorder(java.awt.Color.black, 1));
		tTiponoffBtn.setBorderPainted(false);
		tTiponoffBtn.setFocusPainted(false);
		// Keep the tool tip showing
		ToolTipManager.sharedInstance().setDismissDelay(10000);


		messageAreaScrollPane.setLocation(new java.awt.Point(3, 287));
		messageAreaScrollPane.setSize(new java.awt.Dimension(560, 175));
		messageAreaScrollPane.setAutoscrolls(true);
		messageAreaScrollPane.setVisible(true);
		messageAreaScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		messageAreaScrollPane.setToolTipText("<html>Scroll through Messages<br>You may select, cut and past massages into<br>your text editor or mail program.</html>");
		messageArea.setVisible(true);
		messageArea.setBackground(new java.awt.Color(240, 240, 240));
		messageArea.setFont(new java.awt.Font("SansSerif", 0, 10));
		messageArea.setTabSize(5);
		messageArea.setToolTipText("<html><b>Processing Messages</b><br>[alt+ctrl]-click to clear</html>");
		messageArea.setEditable(false);
		// store current window size according to the controls we have set
		window_initial_Dimension = new java.awt.Dimension( messageAreaScrollPane.getWidth()  + (2*messageAreaScrollPane.getX()), messageAreaScrollPane.getY() +  messageAreaScrollPane.getHeight());
		window_current_Dimension = new java.awt.Dimension(window_initial_Dimension);

		// define popup buttons for different sections
		sourcePopBtn.setText("Source");
		sourcePopBtn.setLocation(new java.awt.Point(1, 1));
		sourcePopBtn.setSize(new java.awt.Dimension(70, 22));
		sourcePopBtn.setVisible(true);
		sourcePopBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		sourcePopBtn.setToolTipText("Show Source settings");
		sourcePopBtn.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		sourcePopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		xslPopBtn.setText("XSL");
		xslPopBtn.setLocation(new java.awt.Point(71, 1));
		xslPopBtn.setSize(new java.awt.Dimension(70, 22));
		xslPopBtn.setVisible(true);
		xslPopBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		xslPopBtn.setToolTipText("Show XSL settings");
		xslPopBtn.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		xslPopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		outputPopBtn.setText("Output");
		outputPopBtn.setLocation(new java.awt.Point(141, 1));
		outputPopBtn.setSize(new java.awt.Dimension(70, 22));
		outputPopBtn.setVisible(true);
		outputPopBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		outputPopBtn.setToolTipText("Show Output settings");
		outputPopBtn.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		outputPopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		cleanupPopBtn.setText("Clean Up");
		cleanupPopBtn.setLocation(new java.awt.Point(211, 1));
		cleanupPopBtn.setSize(new java.awt.Dimension(70, 22));
		cleanupPopBtn.setVisible(true);
		cleanupPopBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		cleanupPopBtn.setToolTipText("Show Cleanup settings");
		cleanupPopBtn.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		cleanupPopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		nextjobticketPopBtn.setText("JobTickets");
		nextjobticketPopBtn.setLocation(new java.awt.Point(281, 1));
		nextjobticketPopBtn.setSize(new java.awt.Dimension(70, 22));
		nextjobticketPopBtn.setVisible(true);
		nextjobticketPopBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		nextjobticketPopBtn.setToolTipText("Show Jobtickets settings");
		nextjobticketPopBtn.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		nextjobticketPopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		imagePopBtn.setText("Images");
		imagePopBtn.setLocation(new java.awt.Point(351, 1));
		imagePopBtn.setSize(new java.awt.Dimension(70, 22));
		imagePopBtn.setVisible(true);
		imagePopBtn.setFont(new java.awt.Font("Dialog", 0, 10));
		imagePopBtn.setToolTipText("Show Image Conversion settings");
		imagePopBtn.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		imagePopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));


		xslt_paramsScrollPane.getViewport().add(xslt_paramsArea);
		messageAreaScrollPane.getViewport().add(messageArea);


		// =======================================
		/**
		 * create container panels for our dialog components
		 */

		// first, a global pane containing all subpanes
		global_frame = this;	// get our main window
		global_frame.setResizable(false);
		global_frame.setFont(new java.awt.Font("SansSerif", 0, 10));
		global_frame.setLocation(new java.awt.Point(40, 40));
		global_frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		global_frame.setTitle(applFullName + " " + applMajorVersion + "." + applMinorVersion + ((xslVersionName.equals("") == false) ? ("." + xslVersionName) : ""));		// set the main frame's title
		global_frame.setVisible(true);
		setMainWindowSize();


		Container global_frameContainer = getContentPane();

        //Create a global JPanel, and add the sub Panels to it.
		globalPane.setLayout(null);
        //globalPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        globalPane.setOpaque(true); //content panes must be opaque
		globalPane.setLocation(new java.awt.Point(0,0));
		globalPane.setVisible(true);
		global_frameContainer.add(globalPane);
		global_frame.setContentPane(globalPane);
		
		int globalPane_right = global_frame.getSize().width;
		if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
			globalPane_right = globalPane_right-8;
		}

        // add the logo Panel
        JPanel logoPane = new JPanel();
		logoPane.setLayout(null);
		//logoPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        logoPane.setOpaque(false);
		logoPane.setSize(new java.awt.Dimension(128, 115));
		logoPane.setLocation(new java.awt.Point(globalPane_right-128-2, 0));
		logoPane.setVisible(true);
		globalPane.add(logoPane);
		// ad logo to the logoPane
		logoPane.add(batchxsltLbl);


		// add the actions Buttons Panel to it the stuff on the top of window.
		java.awt.Dimension buttonsPane_Size = new java.awt.Dimension(122, 164);
		java.awt.Point buttonsPane_Location = new java.awt.Point(globalPane_right-122-5, 121);
		JPanel buttonsPane = new JPanel();
		buttonsPane.setLayout(null);
		//buttonsPane.setBorder(BorderFactory.createLineBorder(border_color,1));
		buttonsPane.setOpaque(true);
		buttonsPane.setSize(buttonsPane_Size);
		buttonsPane.setLocation(buttonsPane_Location);
		buttonsPane.setVisible(true);
		globalPane.add(buttonsPane);
		// ad dialog components to the buttonsPane
		buttonsPane.add(transformBtn);
		buttonsPane.add(loadJTBtn);
		buttonsPane.add(saveJTBtn);
		buttonsPane.add(quitBtn);
		buttonsPane.add(showLogfileBtn);
		buttonsPane.add(tTiponoffBtn);


        // add general data Panel to it the stuff on the top of window.
		java.awt.Point generalPane_Location = new java.awt.Point(0, 0);
		java.awt.Dimension generalPane_Size = new java.awt.Dimension(430, 29);
        JPanel generalPane = new JPanel();
		generalPane.setLayout(null);
		//generalPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        generalPane.setOpaque(true);
		generalPane.setLocation(generalPane_Location);
		generalPane.setSize(generalPane_Size);
		generalPane.setVisible(true);
		globalPane.add(generalPane);
		// ad dialog components to the generalPane
		generalPane.add(timeLbl);
		generalPane.add(memLbl);
		generalPane.add(modeLbl);
		generalPane.add(modeCBx);
		generalPane.add(runMessageLbl);




		int popupPane_height = 252;
		int popupPane_width = 430;
		// add Panel to contain the popup buttons and content panes
 		java.awt.Point popupPane_Location = new java.awt.Point(3, generalPane_Location.y + generalPane_Size.height+3);
		java.awt.Dimension popupPane_Size = new java.awt.Dimension(popupPane_width, popupPane_height);
		JPanel popupPane = new JPanel();
		popupPane.setLayout(null);
		//popupPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        popupPane.setOpaque(true);
		popupPane.setLocation(popupPane_Location);
		popupPane.setSize(popupPane_Size);
		popupPane.setVisible(true);
		globalPane.add(popupPane);


        // add the popup Buttons Panel
 		java.awt.Point popupBtnsPane_Location = new java.awt.Point(4, 0);
		java.awt.Dimension popupBtnsPane_Size = new java.awt.Dimension(422, 22);
		JPanel popupBtnsPane = new JPanel();
		popupBtnsPane.setLayout(null);
		//popupBtnsPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        popupBtnsPane.setOpaque(true);
		popupBtnsPane.setLocation(popupBtnsPane_Location);
		popupBtnsPane.setSize(popupBtnsPane_Size);
		popupBtnsPane.setVisible(true);
		popupPane.add(popupBtnsPane);
		// ad dialog components to the buttonsPane
		popupBtnsPane.add(sourcePopBtn);
		popupBtnsPane.add(xslPopBtn);
		popupBtnsPane.add(outputPopBtn);
		popupBtnsPane.add(cleanupPopBtn);
		popupBtnsPane.add(nextjobticketPopBtn);
		popupBtnsPane.add(imagePopBtn);


        // add the popup CONTENT Panel
 		java.awt.Point popupContentPane_Location = new java.awt.Point(0, popupBtnsPane_Location.y + popupBtnsPane_Size.height);
		java.awt.Dimension popupContentPane_Size = new java.awt.Dimension(popupPane_width, popupPane_height-popupBtnsPane_Size.height);
		JPanel popupContentPane = new JPanel();
		popupContentPane.setLayout(null);
		//popupContentPane.setBorder(BorderFactory.createLineBorder(popBtnBorder_activated,2));
		//Border loweredbevel = BorderFactory.createLoweredBevelBorder();
		Color highlight = new java.awt.Color(120, 160, 180);
		Color shadow = new java.awt.Color(50, 120, 150);

		Border loweredbevel = new javax.swing.border.SoftBevelBorder(javax.swing.border.SoftBevelBorder.LOWERED, highlight, shadow);
		//BorderFactory.createCompoundBorder(raisedbevel, loweredbevel);
		popupContentPane.setBorder(loweredbevel);
        popupContentPane.setOpaque(true);
		popupContentPane.setLocation(popupContentPane_Location);
		popupContentPane.setSize(popupContentPane_Size);
		popupContentPane.setVisible(true);
		popupPane.add(popupContentPane);

        // add the panel for source file stuff.
		java.awt.Point sourcePane_Location = new java.awt.Point(0, 0);
		java.awt.Dimension sourcePane_Size = new java.awt.Dimension(popupPane_width, popupPane_height);
		sourcePane.setLayout(null);
		//sourcePane.setBorder(BorderFactory.createLineBorder(border_color,1));
        sourcePane.setOpaque(false); //content panes must be opaque
		sourcePane.setLocation(sourcePane_Location);
		sourcePane.setSize(sourcePane_Size);
		sourcePane.setVisible(true);
		popupContentPane.add(sourcePane);
		// ad dialog components to the sourcePane
		sourcePane.add(sourcePathNameLbl);
		sourcePane.add(sourcePathNameFld);
		sourcePane.add(sourcePathNameChooseBtn);
		sourcePane.add(excludeSourceProcessingRunExtsLbl);
		sourcePane.add(excludeSourceProcessingRunExtsLbl2);
		sourcePane.add(excludeSourceProcessingRunExtsFld);

        // add panel for xsl stuff.
		java.awt.Point xslPane_Location = new java.awt.Point(0, sourcePane_Location.y + sourcePane_Size.height);
		java.awt.Dimension xslPane_Size = new java.awt.Dimension(popupPane_width, popupPane_height);
		xslPane.setLayout(null);
		//xslPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        xslPane.setOpaque(false); //content panes must be opaque
		xslPane.setLocation(xslPane_Location);
		xslPane.setSize(xslPane_Size);
		xslPane.setVisible(true);
		popupContentPane.add(xslPane);
		// ad dialog components to the xslPane
		xslPane.add(xslPathNameLbl);
		xslPane.add(xslPathNameFld);
		xslPane.add(xslPathNameChooseBtn);
		xslPane.add(xslt_paramsLbl);
		xslPane.add(xslt_paramsScrollPane);

        // add panel for output stuff.
		java.awt.Point outputPane_Location = new java.awt.Point(0, xslPane_Location.y + xslPane_Size.height);
		java.awt.Dimension outputPane_Size = new java.awt.Dimension(popupPane_width, popupPane_height);
		outputPane.setLayout(null);
		//outputPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        outputPane.setOpaque(false); //content panes must be opaque
		outputPane.setLocation(outputPane_Location);
		outputPane.setSize(outputPane_Size);
		outputPane.setVisible(true);
		popupContentPane.add(outputPane);
		// ad dialog components to the outputPane
		outputPane.add(outputPathNameLbl);
		outputPane.add(outputPathNameFld);
		outputPane.add(outputPathNameChooseBtn);

		outputPane.add(usernameLbl);
		outputPane.add(usernameFld);
		outputPane.add(passwordLbl);
		outputPane.add(passwordFld);
		outputPane.add(portLbl);
		outputPane.add(portFld);
		outputPane.add(ftpEncodingCBx);
		outputPane.add(newoutputFileNameExtLbl);
		outputPane.add(newoutputFileNameExtFld);


        // add panel for cleanup stuff.
		java.awt.Point cleanupPane_Location = new java.awt.Point(0, outputPane_Location.y + outputPane_Size.height);
		java.awt.Dimension cleanupPane_Size = new java.awt.Dimension(popupPane_width, popupPane_height);
		cleanupPane.setLayout(null);
		//cleanupPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        cleanupPane.setOpaque(false); //content panes must be opaque
		cleanupPane.setLocation(cleanupPane_Location);
		cleanupPane.setSize(cleanupPane_Size);
		cleanupPane.setVisible(true);
		popupContentPane.add(cleanupPane);
		// ad dialog components to the cleanupPane
		cleanupPane.add(excludeCleanupRunExtsLbl);
		cleanupPane.add(excludeCleanupRunExtsLbl2);
		cleanupPane.add(excludeCleanupRunExtsFld);
		cleanupPane.add(sourceFileActionLbl);
		cleanupPane.add(sourceFileActionLbl2);
		cleanupPane.add(sourceFileActionCBx);
		cleanupPane.add(sourceFileActionFld);
		cleanupPane.add(sourceFileActionChooseBtn);
		cleanupPane.add(deleteSourceDirsCheckBox);


        // add panel for next jobticket stuff.
		java.awt.Point nextjobticketPane_Location = new java.awt.Point(0, cleanupPane_Location.y + cleanupPane_Size.height);
		java.awt.Dimension nextjobticketPane_Size = new java.awt.Dimension(popupPane_width, popupPane_height);
		nextjobticketPane.setLayout(null);
		//nextjobticketPane.setBorder(BorderFactory.createLineBorder(border_color,1));
        nextjobticketPane.setOpaque(false); //content panes must be opaque
		nextjobticketPane.setLocation(nextjobticketPane_Location);
		nextjobticketPane.setSize(nextjobticketPane_Size);
		nextjobticketPane.setVisible(true);
		popupContentPane.add(nextjobticketPane);
		// ad dialog components to the nextjobticketPane
		nextjobticketPane.add(nextJTNameLbl);
		nextjobticketPane.add(nextJTNameFld);
		nextjobticketPane.add(nextJTNameChooseBtn);
		nextjobticketPane.add(delayJTLbl);
		nextjobticketPane.add(delayJTLbl2);
		nextjobticketPane.add(delayJTFld);
		nextjobticketPane.add(jt_scheduleLbl);
		nextjobticketPane.add(jt_scheduleFld);

        // add panel for image stuff.
		java.awt.Point imagePane_Location = new java.awt.Point(280, cleanupPane_Location.y + cleanupPane_Size.height);
		java.awt.Dimension imagePane_Size = new java.awt.Dimension(popupPane_width, popupPane_height);
		imagePane.setLayout(null);
		//imagePane.setBorder(BorderFactory.createLineBorder(border_color,1));
        imagePane.setOpaque(false); //content panes must be opaque
		imagePane.setLocation(imagePane_Location);
		imagePane.setSize(imagePane_Size);
		imagePane.setVisible(true);
		popupContentPane.add(imagePane);
		// ad dialog components to the imagePane
		imagePane.add(ImageMagickPathNameLbl);
		imagePane.add(ImageMagickPathNameFld);
		imagePane.add(ImageMagickPathNameChooseBtn);
		imagePane.add(GhostscriptPathNameLbl);
		imagePane.add(GhostscriptPathNameFld);
		imagePane.add(GhostscriptPathNameChooseBtn);

		// we first hide all popups
		do_show_popup(1);	// and show source stuff popup


		// =======================================
		// ad messages stuff to the top pane
		globalPane.add(messageAreaScrollPane);


		// =======================================
		// add panel for user control stuff.
		java.awt.Point userControlsPaneContainer_Location = new java.awt.Point(1, messageAreaScrollPane.getLocation().y + messageAreaScrollPane.getSize().height + 2);
		java.awt.Dimension userControlsPaneContainer_Size = new java.awt.Dimension(messageAreaScrollPane.getWidth()+4, 0);
		userControlsPaneContainer.setLayout(null);

		userControlsPaneContainer.setFont(new java.awt.Font("SansSerif", 0, 11));
		userControlsPaneContainer.setBorder(userControlsPaneContainerIsVisibleBorder);

		//userControlsPaneContainer.setBorder(BorderFactory.createLineBorder(border_color,1));
		userControlsPaneContainer.setOpaque(true);
		userControlsPaneContainer.setLocation( userControlsPaneContainer_Location);
		userControlsPaneContainer.setSize( userControlsPaneContainer_Size);
		userControlsPaneContainer.setVisible(true);
		globalPane.add( userControlsPaneContainer);

		java.awt.Point userControlsPane_Location = new java.awt.Point(4, 18);
		java.awt.Dimension userControlsPane_Size = new java.awt.Dimension(userControlsPaneContainer_Size.width-8, 0);
		userControlsPane.setLayout(null);
		//userControlsPane.setBorder(BorderFactory.createLineBorder(border_color,1));
		userControlsPane.setOpaque(true);
		userControlsPane.setLocation( userControlsPane_Location);
		userControlsPane.setSize( userControlsPane_Size);
		userControlsPane.setVisible(true);
		userControlsPaneContainer.add( userControlsPane);

/*
		getContentPane().add(folderindexnameLbl);
		getContentPane().add(folderindexnameFld);
		getContentPane().add(folderindexXSLLbl);
		getContentPane().add(folderindexXSLFld);
		getContentPane().add(folderindexXSLChooseBtn);
*/



		global_frame.setVisible(true);


		// ===============================
		// Show online manual in browser
		batchxsltLbl.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				int modifiers = e.getModifiersEx();
				if ( (modifiers & (InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) > 0 ) { // alt key pressed
					String myURL = onlineManualUrl;
					if (myURL.equals("") == true) myURL = default_onlineManualUrl;
					com.epaperarchives.browser.BrowserLaunch.openURL(myURL);
					return;
				}
			}
		});

		// Show local Log file in app
		showLogfileBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				if (logfile_path.equals("") == true) logfile_path = userHome;
				if (logfile_path.endsWith(File.separator) == false)  logfile_path = logfile_path + File.separator;
				if (logfile_name.equals("")) logfile_name = default_logfile_name;
	
				//String myURL = ("file://" + logfile_path + logfile_name);
				String myURL = (logfile_path + logfile_name);
				//showMess("Log file: " + myURL + "\n");
				com.epaperarchives.browser.BrowserLaunch.openURL(myURL);
			}
		});




		userControlsPaneContainer.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				userControlsPaneContainerMouseClicked(e);
			}
		});
		sourcePopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				show_popup(1);
			}
		});
		sourcePopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseExited(java.awt.event.MouseEvent e) {
				cancel_show_popup(1);
			}
		});
		xslPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				show_popup(2);
			}
		});
		xslPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseExited(java.awt.event.MouseEvent e) {
				cancel_show_popup(2);
			}
		});
		outputPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				show_popup(3);
			}
		});
		outputPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseExited(java.awt.event.MouseEvent e) {
				cancel_show_popup(3);
			}
		});
		cleanupPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				show_popup(4);
			}
		});
		cleanupPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseExited(java.awt.event.MouseEvent e) {
				cancel_show_popup(4);
			}
		});
		nextjobticketPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				show_popup(5);
			}
		});
		nextjobticketPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseExitred(java.awt.event.MouseEvent e) {
				cancel_show_popup(5);
			}
		});
		imagePopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				show_popup(6);
			}
		});
		imagePopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseExited(java.awt.event.MouseEvent e) {
				cancel_show_popup(6);
			}
		});


		messageArea.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				int modifiers = e.getModifiersEx();
				if ( ((modifiers & InputEvent.ALT_DOWN_MASK) > 0) && ((modifiers & InputEvent.CTRL_DOWN_MASK) > 0) ) { // alt + ctrl key pressed
					clearMess(0);	// total clear
					return;
				}
				if ( ((modifiers & InputEvent.SHIFT_DOWN_MASK) > 0) && ((modifiers & InputEvent.CTRL_DOWN_MASK) > 0) ) { // shift + ctrl key pressed
					clearMess(1);	// show BatchXSLT version
					return;
				}
			}
		});




		sourcePathNameFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				sourcePathNameFldKeyTyped(e);
			}
		});

		sourcePathNameChooseBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				sourcePathNameChooseBtnMouseClicked(e);
			}
		});
		excludeSourceProcessingRunExtsFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				excludeSourceProcessingRunExtsFldKeyTyped(e);
			}
		});


		xslPathNameFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				xslPathNameFldKeyTyped(e);
			}
		});
		xslPathNameChooseBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				xslPathNameChooseBtnMouseClicked(e);
			}
		});
		xslt_paramsArea.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				xslt_paramsAreaMouseClicked(e);
			}
		});
		xslt_paramsArea.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				xslt_paramsAreaKeyTyped(e);
			}
		});
		outputPathNameFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				outputPathNameFldKeyTyped(e);
			}
		});
		outputPathNameFld.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				outputPathNameFldActionPerformed(e);
			}
		});
		outputPathNameChooseBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				outputPathNameChooseBtnMouseClicked(e);
			}
		});
		passwordFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				passwordFldKeyTyped(e);
			}
		});
		passwordFld.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				passwordFldActionPerformed(e);
			}
		});
		ftpEncodingCBx.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(java.awt.event.ItemEvent e) {
				ftpEncodingCBxItemStateChanged(e);
			}
		});

		newoutputFileNameExtFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				newoutputFileNameExtFldKeyTyped(e);
			}
		});
		excludeCleanupRunExtsFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				excludeCleanupRunExtsFldKeyTyped(e);
			}
		});
		sourceFileActionCBx.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(java.awt.event.ItemEvent e) {
				sourceFileActionCBxItemStateChanged(e);
			}
		});
		sourceFileActionFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				sourceFileActionFldKeyTyped(e);
			}
		});
		sourceFileActionChooseBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				sourceFileActionChooseBtnMouseClicked(e);
			}
		});
		deleteSourceDirsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				deleteSourceDirsCheckBoxMouseClicked(e);
			}
		});
		deleteSourceDirsCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				deleteSourceDirsCheckBoxStateChanged(e);
			}
		});

		delayJTFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				delayJTFldKeyTyped(e);
			}
		});
		nextJTNameFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				nextJTNameFldKeyTyped(e);
			}
		});
		nextJTNameChooseBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				nextJTNameChooseBtnMouseClicked(e);
			}
		});
		jt_scheduleFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				jt_scheduleFldKeyTyped(e);
			}
		});

		ImageMagickPathNameFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent e) {
				ImageMagickPathNameFldKeyTyped(e);
			}
		});
		ImageMagickPathNameChooseBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				ImageMagickPathNameChooseBtnMouseClicked(e);
			}
		});

		GhostscriptPathNameFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent e) {
				GhostscriptPathNameFldKeyTyped(e);
			}
		});
		GhostscriptPathNameChooseBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				GhostscriptPathNameChooseBtnMouseClicked(e);
			}
		});

		// add main button's actions
		transformBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				transformBtnMouseClicked(e);
			}
		});
		loadJTBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				loadJTBtnMouseClicked(e);
			}
		});
		saveJTBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				saveJTBtnMouseClicked(e);
			}
		});
		quitBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				quitBtnMouseClicked(e);
			}
		});
		tTiponoffBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				tTiponoffBtnMouseClicked(e);
			}
		});


		// add popup button's actions
		sourcePopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				popupBtnMouseClicked(1,e);
			}
		});
		xslPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				popupBtnMouseClicked(2,e);
			}
		});
		outputPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				popupBtnMouseClicked(3,e);
			}
		});
		cleanupPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				popupBtnMouseClicked(4,e);
			}
		});
		nextjobticketPopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				popupBtnMouseClicked(5,e);
			}
		});
		imagePopBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				popupBtnMouseClicked(6,e);
			}
		});
/*
		folderindexnameFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				folderindexnameFldKeyTyped(e);
			}
		});
		folderindexXSLFld.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				folderindexXSLFldKeyTyped(e);
			}
		});
		folderindexXSLChooseBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				folderindexXSLChooseBtnMouseClicked(e);
			}
		});
*/

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				thisWindowClosing(e);
			}
		});

	}



  	private boolean mShown = false;
  	
	// ----------------------------------------------------
	public void addNotify() {
		super.addNotify();
		
		if (mShown)
			return;
			
		// resize frame to account for menubar
		JMenuBar jMenuBar = getJMenuBar();
		if (jMenuBar != null) {
			int jMenuBarHeight = jMenuBar.getPreferredSize().height;
			Dimension dimension = getSize();
			dimension.height += jMenuBarHeight;
			setSize(dimension);
		}
		// resize frame bottom/right of frame to match for Windows and Mac
		// do this if running on Windows only

		mShown = true;
	}


	// ----------------------------------------------------
	// check if we have an 'early_init.jt' file
	void early_initXSLT() {
		String early_init_filename = "early_init.jt";
		String early_init_filepath = "init" + File.separator + early_init_filename;
		InputStream init_jobTicketFile = null;
		try {
			init_jobTicketFile = new FileInputStream( early_init_filepath );
		} catch (FileNotFoundException e) {
			//System.out.println("NOT FOUND early_initXSLT file: "+ early_init_filepath);
			return;
		}
		JobTicket init_jt_file = new JobTicket();
		try { init_jt_file.load( init_jobTicketFile ); }
		catch (IOException e) {
			try { init_jobTicketFile.close(); }
			catch ( IOException e1 ) { }
			return;
		}
		try { mainXSLTFrame.applFullName = init_jt_file.getString( "applFullName" ); }
		catch (MissingResourceException e) {}
		try { mainXSLTFrame.applLogoName = init_jt_file.getString( "applLogoName" ); }
		catch (MissingResourceException e) {}

		try { init_jobTicketFile.close(); }
		catch ( IOException e1 ) { }
		init_jt_file = null;
		return;
	}



	// ----------------------------------------------------
	void initXSLT(final String[] args) {
		
        Runnable doinitXSLT = new Runnable() {
            public void run() {
				Thread.currentThread().setName("initXSLT");
            	do_initXSLT(args);
            }
        };
        try { SwingUtilities.invokeAndWait(doinitXSLT); }
		catch (InvocationTargetException e1) { }
		catch (InterruptedException e2) {}
	}

	Object do_initXSLT(String[] args) {

		runMessageLbl.setText( "Initializing..." );

		//-------------------
		// Charactersets stuff
		allAvailableCharsets = java.nio.charset.Charset.availableCharsets();
		int numcharsets = allAvailableCharsets.size();
		Iterator it = null;

		if (DEBUG) {
			it = allAvailableCharsets.keySet().iterator();
			while (it.hasNext()) {
				String csName = (String) it.next(); System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get UTF-8
		SortedMap subCharset = allAvailableCharsets.subMap("UTF-8","V");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get all other UTF-XX excluding UTF-8
		subCharset = allAvailableCharsets.subMap("UTF","UTF-8");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get US-ASCII
		subCharset = allAvailableCharsets.subMap("US-A","US-B");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get all ISO-8...
		subCharset = allAvailableCharsets.subMap("ISO-8","J");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get all other ISO-2...
		subCharset = allAvailableCharsets.subMap("ISO-","ISO-8");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get all windows...
		subCharset = allAvailableCharsets.subMap("windows","x");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get all IBM...
		subCharset = allAvailableCharsets.subMap("IBM","IC");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get Big5 - GBK
		subCharset = allAvailableCharsets.subMap("Big","GC");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// get JIS - GBK
		subCharset = allAvailableCharsets.subMap("J","US");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}
		// and all others, starting at x-
		subCharset = allAvailableCharsets.subMap("x","z");
		it = subCharset.keySet().iterator();
		while (it.hasNext()) {
			String csName = (String) it.next();
			ftpEncodingCBx.addItem(csName);
			if (DEBUG) {
				System.out.print(csName);
				Iterator aliases = ((Charset) allAvailableCharsets.get(csName)).aliases().iterator();
				if (aliases.hasNext()) System.out.print(": ");
				while (aliases.hasNext()) {
					System.out.print(aliases.next());
					if (aliases.hasNext()) System.out.print(", ");
				}
				System.out.println();
			}
		}

		// get properties
		if (DEBUG == true) {
			System.getProperties().list(System.out);
		}
		try { systemOSname = System.getProperty("os.name"); } catch (Exception e) {}
		try { systemOSversion = System.getProperty("os.version"); } catch (Exception e) {}
		try { systemOSarchitecture = System.getProperty("os.arch"); } catch (Exception e) {}
		try { systemDefaultCharset = System.getProperty("file.encoding"); } catch (Exception e) {}
		try { VMversion = System.getProperty("java.version", ""); } catch (Exception e) {}
		try { VMhome = System.getProperty("java.home", ""); } catch (Exception e) {}

                try { 
                    String classname = this.getClass().getSimpleName();
                    appDir = "" + this.getClass().getResource(classname+".class");
                    // on OSX:
                    // jar:file:/Applications/BatchXSLT4InDesignV6/BatchXSLT/BatchXSLT.app/Contents/Java/BatchXSLT.jar!/com/epaperarchives/batchxslt/mainXSLTFrame.class
                    // on WIN:
                    // jar:file:/C:/Program%20Files/BatchXSLT4InDesignV6/BatchXSLT/BatchXSLT.app/Contents/Java/BatchXSLT.jar!/com/epaperarchives/batchxslt/mainXSLTFrame.class
                    //showMess("RAW App Dir: " + appDir + "\n", true, true);
                    int start = appDir.indexOf("file:")+5;
                    int end = appDir.indexOf(applFullName + ".app/")-1;
                    appDir = appDir.substring(start,end);
                    //showMess("1App Dir: " + appDir + "\n");
                    appDir = URLDecoder.decode(appDir, "UTF-8");
                    if ((systemOSname.indexOf("Wind") >= 0)
                        && (appDir.indexOf(":") == 2)	// 3rd letter is colon from drive letter '/C:'
                        ) {
                        appDir = appDir.substring(1);   // cut leading '/'
                        appDir = appDir.replace('/',File.separatorChar);
                    }
                    //showMess("2App Dir: " + appDir + "\n");

                    //DO NOT SET !!!!      System.setProperty("user.dir", appDir);
                } catch (Exception e) { appDir = ""; }	// app directory
		try { userDir = System.getProperty("user.dir"); } catch (Exception e) {}	// User's current working directory
				//System.out.println("userDir=" + userDir);
		if (userDir.endsWith("/..")) userDir = userDir.substring(0,userDir.length()-3);
				//System.out.println("userDir=" + userDir);
		try { userHome = System.getProperty("user.home"); } catch (Exception e) {}	// User home directory
		try { userName = System.getProperty("user.name"); } catch (Exception e) {}	// User account name
		try { userTempDir = System.getProperty("java.io.tmpdir"); } catch (Exception e) {}	// User temporary folder
		try { userLang = System.getProperty("user.language"); } catch (Exception e) {}		// User language
		try { userCountry = System.getProperty("user.country"); } catch (Exception e) {}	// User Country

		try { localMachineName = InetAddress.getLocalHost().getHostName(); } catch (Exception e) {}  // Handle any exceptions

		try {
			envirPath = System.getenv("PATH");
			if (envirPath == null || envirPath.equals("")) envirPath = System.getenv("path");
			if (DEBUG == true) {
				System.out.print("Envir PATH=" + envirPath);
			}
		} catch (Exception e) {}	// PATH envir variable

		logfile_path = userHome;	// the path to log file. default =user's home dir

		// path to packaged color profiles
		color_profiles_path = BatchXSLT.g_mainXSLTFrame.userDir + File.separator + "Utilities" + File.separator + "Profiles" + File.separator;



		cd = Calendar.getInstance();		// get current date and time
		ct = ( cd.get(Calendar.HOUR_OF_DAY) + ":" + cd.get(Calendar.MINUTE) + ":" + cd.get(Calendar.SECOND) );

		delayJTFld.setText(Integer.valueOf(loopDelay).toString());

		modeCBx.addItem(modeChoice0);
		modeCBx.addItem(modeChoice1);
		modeCBx.addItem(modeChoice2);
		modeCBx.addItem(modeChoice3);

		sourceFileActionCBx.addItem(sourceFileAction0);
		sourceFileActionCBx.addItem(sourceFileAction1);
		sourceFileActionCBx.addItem(sourceFileAction2);

		init_jt();	// init initial jobticket data
		mode = 1;	// on first start set automatic batch mode...
		jobTicketFileName = "autostart.jt";	// ...and try to load autostart.jt

		// parse commandline arguments
		String arg_str = "";
		for (int i = 0; i < args.length; i++) {
			arg_str = args[i];
			//System.out.println( "Marg_str: " + arg_str);
			do {
				if (arg_str.equals("--jt") == true) {
					if ( ((i+1) < args.length) && (args[i+1].startsWith("-")==false) ) {
						jobTicketFileName = args[i+1];
						i++;	// consumed next arg
					}
					else jobTicketFileName = "";	// clear initial jobticket
					break;
				}
			} while (false);
		}



		processing = false;
		fileCopyInProgress = false;
		runMessageLbl.setText( "" );

		// set the dialog item states
		do_setItemStates(false,false);

		ToolTipManager.sharedInstance().setEnabled(ttipstate);	// initially enabled

		// and show the first status message
		clearMess(1);

		return (null);
	}

	

	// ----------------------------------------------------
 	public void init_jt() {
		remove_controls();	// remove controls defined by jt
		
		jobticket_loaded = false;
		jt_schedule = "";
		jt_triggerfile = "";
		mode = 0;	// set manual mode
		sourcePathRoot = "";
		sourcePathName = "";
		xslPathName = "";
		xslParams = "";
		outputPathRoot = "";
		outputPathName = "";
		ftpUsername = "anonymous";
		ftpPassword = "";
		ftpPort = "21";
		ftpEncoding = ftpEncodingDefault;
		ftpType = "ftp";
		ftpActivePassive = "passive";
		newoutputFileNameExt = "";
		excludeCleanupRunFileNameExts = "";
		excludeSourceProcessingRunFileNameExts = "";
		excludeSourceProcessingRunFileNames = "";
		deleteSourceDirs = 0;
		sourceFileAction = "";
		folderIndexFileName = "";
		folderIndexFileXSLName = "";
		//jobTicketFileName = "";		NOOOO! don't change it - it might be overriden from commandline arguments!
		loopDelay = 3000;
		nextJobTicketPath = mainJobTicketPath;
		nextJobTicketFileName = "";
		onlineManualUrl = default_onlineManualUrl;	// reset to default
	/*
		logfile_write = 0;
		logfile_path = userHome;
		logfile_name = default_logfile_name;
		
		eps2xml_associate_XSL = "";
		check_for_external_converters = 0;
		gs_convert = 0;
		gs_envir = "";
		gs_pgm_path = "";
		gs_pgm_name = "";
		gs_pgm_installedpath = "";
		gs_pgm_installedhome = "";
		gs_pgm_parms_eps2pdf = "";
		
		pstotext_envir = "";
		pstotext_pgm_path = "";
		pstotext_pgm_parms_pdf2txt = "";
		
		im_envir = "";
		im_pgm_CONVERT = "";
		im_pgm_IDENTIFY = "";
		im_pgm_path = "";
		im_pgm_installedpath = "";
		im_pgm_installedhome = "";
	*/
		runOnLoadedJobTicketMethod = "";
		runBeforeJobMethod = "";
		runAfterJobMethod = "";
		runBeforeJobApp = "";
		runAfterJobApp = "";
		runBeforeTransformApp = "";
		runAfterTransformApp = "";
	}


	// ----------------------------------------------------
 	public void saveSettings() {
 		save_jt_schedule = jt_schedule;
		save_jt_triggerfile = jt_triggerfile;
		save_mode = mode;
		save_sourcePathRoot = sourcePathRoot;
		save_sourcePathName = sourcePathName;
		save_excludeSourceProcessingRunFileNameExts = excludeSourceProcessingRunFileNameExts;
		save_excludeSourceProcessingRunFileNames = excludeSourceProcessingRunFileNames;
		save_xslPathName = xslPathName;
		save_xslParams = xslParams;
		save_outputPathRoot = outputPathRoot;
		save_outputPathName = outputPathName;
		save_ftpUsername = ftpUsername;
		save_ftpPassword = ftpPassword;
		save_ftpPort = ftpPort;
		save_ftpEncoding = ftpEncoding;
		save_ftpType = ftpType;
		save_ftpActivePassive = ftpActivePassive;
		save_newoutputFileNameExt = newoutputFileNameExt;
		save_excludeCleanupRunFileNameExts = excludeCleanupRunFileNameExts;
		
		save_sourceFileAction = sourceFileAction;
		save_deleteSourceDirs = deleteSourceDirs;
		save_loopDelay = loopDelay;
		save_nextJobTicketFileName = nextJobTicketFileName;

		save_folderIndexFileName = folderIndexFileName;
		save_folderIndexFileXSLName = folderIndexFileXSLName;

		save_onlineManualUrl = onlineManualUrl;
		
		save_logfile_write = logfile_write;
		save_logfile_path = logfile_path;
		save_logfile_name = logfile_name;

		save_eps2xml_associate_XSL = eps2xml_associate_XSL;
		save_check_for_external_converters = check_for_external_converters;
		save_gs_convert = gs_convert;
		save_gs_envir = gs_envir;
		save_gs_pgm_path = gs_pgm_path;
		save_gs_pgm_name = gs_pgm_name;
		save_gs_pgm_installedpath = gs_pgm_installedpath;
		save_gs_pgm_installedhome = gs_pgm_installedhome;
		save_gs_pgm_parms_eps2pdf = gs_pgm_parms_eps2pdf;
		
		save_pstotext_envir = pstotext_envir;
		save_pstotext_pgm_path = pstotext_pgm_path;
		save_pstotext_pgm_parms_pdf2txt = pstotext_pgm_parms_pdf2txt;
		
	}


	// ----------------------------------------------------
 	public void restoreSettings() {
 		jt_schedule = save_jt_schedule;
		jt_triggerfile = save_jt_triggerfile;
 		mode = save_mode;
		sourcePathRoot = save_sourcePathRoot;
		sourcePathName = save_sourcePathName;
		excludeSourceProcessingRunFileNameExts = save_excludeSourceProcessingRunFileNameExts;
		excludeSourceProcessingRunFileNames = save_excludeSourceProcessingRunFileNames;
		xslPathName = save_xslPathName;
		xslParams = save_xslParams;
		outputPathRoot = save_outputPathRoot;
		outputPathName = save_outputPathName;
		ftpUsername = save_ftpUsername;
		ftpPassword = save_ftpPassword;
		ftpPort = save_ftpPort;
		ftpEncoding = save_ftpEncoding;
		ftpType = save_ftpType;
		ftpActivePassive = save_ftpActivePassive;
		newoutputFileNameExt = save_newoutputFileNameExt;
		excludeCleanupRunFileNameExts = save_excludeCleanupRunFileNameExts;
		
		sourceFileAction = save_sourceFileAction;
		deleteSourceDirs = save_deleteSourceDirs;
		loopDelay = save_loopDelay;
		nextJobTicketFileName = save_nextJobTicketFileName;

		folderIndexFileName = save_folderIndexFileName;
		folderIndexFileXSLName = save_folderIndexFileXSLName;
		
		onlineManualUrl = save_onlineManualUrl;

		logfile_write = save_logfile_write;
		logfile_path = save_logfile_path;
		logfile_name = save_logfile_name;

		eps2xml_associate_XSL = save_eps2xml_associate_XSL;
		check_for_external_converters = save_check_for_external_converters;
		gs_convert = save_gs_convert;
		gs_envir = save_gs_envir;
		gs_pgm_path = save_gs_pgm_path;
		gs_pgm_name = save_gs_pgm_name;
		gs_pgm_parms_eps2pdf = save_gs_pgm_parms_eps2pdf;
		
		pstotext_envir = save_pstotext_envir;
		pstotext_pgm_path = save_pstotext_pgm_path;
		pstotext_pgm_parms_pdf2txt = save_pstotext_pgm_parms_pdf2txt;
		
	}



	// ----------------------------------------------------
 	public void kill_Sleeper()
 	{
  		sleeperAborted = true;	// tell the sleeper to stop to execute
		return;
	}

	// ----------------------------------------------------
 	public void stop_currentAction()
 	{
		general_abort = true;
		//mode = 0;
		start_over = 0;
		return;
	}

	// ----------------------------------------------------
	int is_first_mainLoop = 0;
 	public void mainLoop(int first_mainLoop, final String[] args) {
		if (processing == true) return;
		is_first_mainLoop = first_mainLoop;
		mainLoop(args);
	}
 	public void mainLoop() {
		String[] dummy_args = null;
		if (processing == true) return;
		mainLoop(dummy_args);
	}
 	public void mainLoop(final String[] args) {
		if (processing == true) return;

		kill_Sleeper();

		SwingWorker worker = new SwingWorker() {
            @Override
			public Object construct() {
				return do_mainLoop(args);
			}
            @Override
			public void finished() {
				//showMess("########################## do_mainLoop finished!!!!!");
				if ((mode == 3) && (general_abort == false) && (jobticket_loaded == true)) {	// batch mode loop forever: seems we want to continue working...
	 				int mySleepTime = 500;
					if (mySleepTime > loopDelay) mySleepTime = loopDelay;
					//if (loopDelay >= 1000) showRunMess( "sleeping " + loopDelay + " ms... " );
					BatchXSLTransform.cleanUpMemory();

	 				if (general_abort == false) {
						// start the sleeper
						try {
							sleeperAborted = false;
							//System.out.println("----- Starting Sleeper!!!");
							mySleeperThread = new SleeperThread(args);
							mySleeperThread.start();
						}
						catch (Exception e) {
							System.out.println("###### Could not start the Sleeper!!!");
						}
					}
					else {	// stopping
						//processing = false;
						//general_abort = false;
                        //setItemStates(0,false);
					}

				}
				else {
					stop_currentAction();
					//processing = false;
					general_abort = false;
	//				setItemStates(0,false);

				}
				// set the dialog item states
				setItemStates(0,false);

			}
		};
		worker.start();
	}

 	Object do_mainLoop(String[] args) {
//System.out.println("***LOOP do_mainLoop: processing: " + processing);
 		if (processing == true) {
			System.out.println("##mainLoop: Reentering mainLoop blocked");
			return null;
		}
		processing = true;

 		int	transform_retval = 0;

		while (true) {
			transform_retval = 0;
	 		if (general_abort == true) break;

			// the processing may currently be disabled
			if (processing_disabled == true) break;

			// set the dialog item states
			setItemStates(0,false);

			// init scheduled jobs to run
			int run_this_jt = 1;

			int myErr = 0;
            //showMess("======================================LOOP start: jobTicketFileName: " + jobTicketFileName + " start_over: " + start_over + "\n");
			if (start_over != 0) { // do this as a starting task
				if (jobTicketFileName.compareTo("") != 0) {
					saveSettings();			// save the current vars settings
					currentJobTicketPath = nextJobTicketPath;
					if (DEBUG) System.out.println("**mainLoop: Reading JobTicket: '" + jobTicketFileName + "'");
					myErr = BatchXSLTransform.readJobTicketFile(1);	// try to load and RUN the default JT 'autostart.jt'
					mode += mode_setby_overridejt;	// if 100, then this was from an override.jt
					// myerr == 0 if jt loaded, -1 if jt blocked by schedule or trigger, otherwise an error occured
					if (myErr > 0) {
						System.out.println("Error " + myErr + " in loading JobTicket: '" + jobTicketFileName + "'");
						jobticket_loaded = false;
						jobTicketFileName = "";
						if (start_over == 1) {
							mode = 0;	// set manual mode if no autostart.jt found
							setDialogFields(true);
						}
						else {
							restoreSettings();			// restore the previous vars settings
							if (mode != 0) nextJobTicketFileName = last_OK_JobTicketFileName;
						}
						start_over = 0;
					}
					else {
						if (myErr == -1) {
							if (DEBUG) System.out.println("**mainLoop: JobTicket blocked: '" + jobTicketFileName + "' by schedule or missing trigger file");
							run_this_jt = 0;	// blocked by schedule or missing trigger file
							File jtf = new File( jobTicketFileName);
							String jtname = jtf.getName();
							showRunMess(jtname + " scheduled");
							if (mode != 0) start_over = 2;		// force a start over
						}
						if (myErr == -99) {	// from readJobTicketFile(0): load but dont' run jt
							run_this_jt = 0;
						}
						last_OK_JobTicketFileName = jobTicketFileName;
					}
				}

				// set the dialog item states
				setItemStates(0,false);
				redirect_StdOutErr_toLog();
			}
			if ((jobticket_loaded == false) && !(mode >= 100)) {
			    if (DEBUG) System.out.println("**mainLoop: Not loaded JobTicket: '" + jobTicketFileName + "'");
                            break;
			}

                        if (general_abort == true) {
                            if (DEBUG) System.out.println("**mainLoop: General Abort before starting Transform");
                            break;
	 		}

			if ( (run_this_jt > 0) && (mode != 0) ) {
                            showMess( "*** JobTicket '" + jobTicketFileName + "' ready to run.\n" );
			}

			// check if we have to run an internal method before job is done
			if (runOnLoadedJobTicketMethod.equals("") == false) {
				showMess( "*** calling internal method (JobTicket loaded): " + runOnLoadedJobTicketMethod + "\n" );
				int retval = com.epaperarchives.batchxslt.utils.callMethod(runOnLoadedJobTicketMethod);
				if (retval != 0) {
					showMess( "### internal method (JobTicket loaded) exit value: " + retval + "\n" );
				}
				runOnLoadedJobTicketMethod = "";
			}

			/*
			showMess( "*** External Application (before transform): " + runBeforeTransformApp + "\n" );
			showMess( "    run_this_jt: " + run_this_jt + "\n" );
			showMess( "    mode: " + mode + "\n" );
			*/
			// transform with source file cleanup if not blocked by schedule
			if ( (run_this_jt > 0) && (mode != 0) ) {
				// check if we have to run an internal method before job is done
				if (runBeforeJobMethod.equals("") == false) {
					showMess( "*** calling internal method (before job): " + runBeforeJobMethod + "\n" );
					int retval = com.epaperarchives.batchxslt.utils.callMethod(runBeforeJobMethod);
					if (retval != 0) {
						showMess( "### internal method (before job) exit value: " + retval + "\n" );
					}
					runBeforeJobMethod = "";
				}

				// check if we have to run an external application before transforming
				if (runBeforeJobApp.equals("") == false) {
					showMess( "*** calling external application (before job): " + runBeforeJobApp + "\n" );
					int retval = com.epaperarchives.batchxslt.utils.callExternalApplication(runBeforeJobApp);
					if (retval != 0) {
						showMess( "### External Application (before transform) exit value: " + retval + "\n" );
						String stdinresp = utils.callExternalAppGetStdinResponse();
						String errorresp = utils.callExternalAppGetErrorResponse();
						if (stdinresp.equals("") == false) System.out.println("External App STDIN: " + errorresp);
						if (errorresp.equals("") == false) System.out.println("External App ERROR: " + errorresp);
					}
					runBeforeJobApp = "";
				}

				// start the transform
				if (BatchXSLTransform.is_dir(sourcePathName)) showMess("    Source path: '" + sourcePathName + "'\n");
				if (mode >= 100) mode -= 100;	// transform manually started by 'Transform' Button or by an override.jt
				transform_retval = BatchXSLTransform.BatchXSLTrans(1,false);
				switch(transform_retval) {
					case 0:	// no err or nothing to do
						break;
					case -1:	// empty source path and other errors - get error text
						showMess( "## Error "+ transform_retval + ": '" + BatchXSLTransform.BatchXSLTrans_lastMessage + "'\n" );
						break;
					case -2:	// Rescanning source path failed - get error text
						showMess( "## Error "+ transform_retval + ": '" + BatchXSLTransform.BatchXSLTrans_lastMessage + "'\n" );
						break;
					case -3:	// error unpacking/transforming an IDML package
						// do not show a message: showMess( "## Error "+ transform_retval + ": '" + BatchXSLTransform.BatchXSLTrans_lastMessage + "'\n" );
						break;
					case 1:		// no error - simply nothing to do
					case 2:		// no error - transform aborted
						if (BatchXSLTransform.BatchXSLTrans_lastMessage.equals("") == false) showMess( "    -- " + BatchXSLTransform.BatchXSLTrans_lastMessage + "\n" );
						break;
					default:	// undefined error
						showMess( "## Undefined Error "+ transform_retval + ": '" + BatchXSLTransform.BatchXSLTrans_lastMessage + "'\n" );
						break;
				}

				// check if we have to run an external application after transforming
				if (runAfterJobApp.equals("") == false) {
					showMess( "*** calling external application (after job): " + runAfterJobApp + "\n" );
					int retval = com.epaperarchives.batchxslt.utils.callExternalApplication(runAfterJobApp);
					if (retval != 0) {
						showMess( "### External Application (after job) exit value: " + retval + "\n" );
						String stdinresp = utils.callExternalAppGetStdinResponse();
						String errorresp = utils.callExternalAppGetErrorResponse();
						if (stdinresp.equals("") == false) System.out.println("External App STDIN: " + errorresp);
						if (errorresp.equals("") == false) System.out.println("External App ERROR: " + errorresp);
					}
					runAfterJobApp = "";
				}

				// check if we have to run an internal method after job is done
				if (runAfterJobMethod.equals("") == false) {
					showMess( "*** calling internal method (after job): " + runAfterJobMethod + "\n" );
					int retval = com.epaperarchives.batchxslt.utils.callMethod(runAfterJobApp);
					if (retval != 0) {
						showMess( "### internal method (after job) exit value: " + retval + "\n" );
					}
					runAfterJobMethod = "";
				}

				run_this_jt = 0;	// we're done with this jobTicket
			}
			// ----------------------------------------------------
			// check if we should automatically quit after running once
			if (mode == 2) { thisWindowClosing(null); }	// quit = let's go home

            //showMess("***LOOP 1: nextJobTicketFileName: " + nextJobTicketFileName + "\n");

	 		if (general_abort == true) {
				if (DEBUG) System.out.println("**mainLoop: General Abort after Transform");
	 			break;
	 		}

            //showMess("***LOOP 2: mode: " + mode + " start_over: " + start_over + " mode_setby_overridejt: " + mode_setby_overridejt + "\n");
			if (mode_setby_overridejt == 100) {		// was called from override JT: delete this jt_f_name
                //showMess("***LOOP 2a: mode: " + mode + " start_over: " + start_over + " jobTicketFileName: " + jobTicketFileName + " jtoverrideName: " + jtoverrideName + "\n");
				if (jobTicketFileName.indexOf(jtoverrideName) >= 0) {	// override.jt, override.jt1, override.jt2 ....
					String jtfilename = jobTicketFileName;
					if ((jtfilename.indexOf(File.separator) < 0) && (currentJobTicketPath.equals("") == false)) {
						String path = currentJobTicketPath;
						if (path.endsWith(File.separator) == false) path = path + File.separator;
						jtfilename = path + jtfilename;
					}
					File overrideJTfile = new File(jtfilename);
					if (overrideJTfile.exists()) {	// if exists then delete it because it is done
						overrideJTfile.delete();
						showMess( "*** JobTicket '" + jobTicketFileName + "' deleted.\n" );
					}
					currentJobTicketPath = "";
				}
			}

			if ((mode != 0) && (start_over != 3)) start_over = 2;		// force a start over

               //showMess("***LOOP 3: nextJobTicketFileName: " + nextJobTicketFileName + " start_over: " + start_over + "\n");
			if (start_over == 3) {	// called from override.jt
				start_over = old_start_over;
				mode_setby_overridejt = 0;
				String nextJobTicketFileName_save = nextJobTicketFileName;
                //showMess( "*** 3 1 currentJobTicketPath: "+ currentJobTicketPath + "\n" );
                //showMess( "*** 3 2 nextJobTicketPath: "+ nextJobTicketPath + "\n" );
				restoreSettings();			// reset previous vars settings
				nextJobTicketFileName = nextJobTicketFileName_save;
                //showMess( "*** 3 3 nextJobTicketFileName: "+ nextJobTicketFileName + "\n" );
				if (nextJobTicketFileName.equals("") == false) {	// override.jt requests a next job ticket
					start_over = 3;
				}
			}

			// ----------------------------------------------------
			// Try to setup a next jt file name set by last JobTicket
            //showMess("***LOOP 5: nextJobTicketFileName: "+ nextJobTicketFileName + ", start_over: "+ start_over + "\n" );
			if (nextJobTicketFileName.equals("") == false) {
				jobTicketFileName = nextJobTicketFileName;
                //showMess("*** 5 1: jobTicketFileName: " + jobTicketFileName + "\n");
				nextJobTicketFileName = save_nextJobTicketFileName = "";
				start_over = 2;
				continue;
			}
            //showMess( "*********************** mode: " + mode + ", start_over: "+ start_over + ", jobTicketFileName: "+ jobTicketFileName + "\n" );

			// ----------------------------------------------------
			// start a thread (if not already started) to check for JobTicket file named 'override.jt'
			if ( (myJTOverrideThread == null) && ((jtoverrideName.equals("") == false) || (jtoverrideQueueName.equals("") == false)) ) {
				if (DEBUGjtThread > 0) System.out.println("**JTOverrideThread starting on folder: '" + jtoverrideQueuePath + "'");
				myJTOverrideThread = new JTOverrideThread();
				myJTOverrideThread.start();
			}

			break;
		}


		// set the dialog item states
		setDialogFields(true);
		setItemStates(0,false);

		BatchXSLTransform.cleanUpMemory();
		is_first_mainLoop = 0;
		processing = false;

		return null;
	}


	// ----------------------------------------------------
	int startTransform() {
		// for XSL processing
 		if (!processing && !fileCopyInProgress) {			// if in manual mode
			getDialogFields();
			mode += 100;			// this will start the Transformer in the main loop
			general_abort = false;
			// call our main engine
 			try { mainLoop(); }
			catch (Exception ex) {
				showRunMess( "##### ERROR: Exception at 'mainLoop'");
				ex.printStackTrace();
			}
			runMessageLbl.setText("scanning...");
		}
		else {
			runMessageLbl.setText("Stopping, please wait...");
 			kill_Sleeper();
			stop_currentAction();
			return(0);
		}
		return(0);
	}


	// ----------------------------------------------------
 	public void startFileCopy(final File[] files, final String destpath, final boolean recursive, final boolean deletesource) {
		SwingWorker worker = new SwingWorker() {
            @Override
			public Object construct() {
				runMessageLbl.setText("Copying...");
				runMessageLbl.invalidate();
				runMessageLbl.paintImmediately(runMessageLbl.getVisibleRect());
				fileCopyInProgress = true;
				processing = true;
				setItemStates(0,false,true);
				return do_FileCopy(files, destpath, recursive, deletesource);
			}
            @Override
			public void finished() {
				// set the dialog item states
				fileCopyInProgress = false;
				processing = false;
				general_abort = false;
				setItemStates(0,false);
			}
		};
		worker.start();
	}

 	Object do_FileCopy(final File[] files, final String destpath, final boolean recursive, final boolean deletesource) {
		try {
			com.epaperarchives.batchxslt.utils.copyMultipleFiles(files, destpath, recursive, deletesource);
		} catch (IOException ex) {}
		return null;
	}


	// ----------------------------------------------------
	void showRunMess(final String s) {
        /*
		Runnable doshowRunMess = new Runnable() {
            @Override
			public void run() {
				runMessageLbl.setText(s);
			}
		};
		SwingUtilities.invokeLater(doshowRunMess);
        */
		runMessageLbl.setText(s);

	}



	int maxmessages = 1000;
	MessageContainer[] messageBuffer = new MessageContainer[maxmessages];
	int messageIn = 0;	// the pointer to put new messages
	int messageOut = 0;	// the pointer to the message to display (if both are equal, we have nothing to do)
	public void initMessages() {
		for ( int i = 0; i < maxmessages; i++ ) {
			messageBuffer[i] = new MessageContainer();
		}
	}
	void storeMessage(final String message, final boolean writelog, final boolean clear) {
		if (message == null) return;
		String datestr;
		Calendar cal = Calendar.getInstance();		// get current date and time
		String	cy = "" + cal.get(Calendar.YEAR);
		String	cm = "" + (cal.get(Calendar.MONTH)+1); if (cm.length() < 2) cm = "0" + cm;
		String	cdm = "" + cal.get(Calendar.DAY_OF_MONTH); if (cdm.length() < 2) cdm = "0" + cdm;
		String	hh = "" + cal.get(Calendar.HOUR_OF_DAY); if (hh.length() < 2) hh = "0" + hh;
		String	mm = "" + cal.get(Calendar.MINUTE); if (mm.length() < 2) mm = "0" + mm;
		String	ss = "" + cal.get(Calendar.SECOND); if (ss.length() < 2) ss = "0" + ss;
		String	ms = "" + cal.get(Calendar.MILLISECOND); if (ms.length() < 2) ms = "0" + ms; if (ms.length() < 3) ms = "0" + ms;
		datestr = ( cy + cm + cdm + " " + hh + ":" + mm + ":" + ss + "." + ms + " ");

		messageIn++;
		if (messageIn >= maxmessages) messageIn = 0;
		messageBuffer[messageIn].valid = true;
		messageBuffer[messageIn].timestamp = datestr;
		messageBuffer[messageIn].writelog = writelog;
		messageBuffer[messageIn].clear = clear;
		messageBuffer[messageIn].message = message;
		// the message displayer
		if (showMessThread_is_running == false) {
				//System.out.println( "++++ STARTING showMessThread");
            showMessThread_is_running = true;
			showMessThread_runner = new showMessThread();
            showMessThread_runner.start();
		}
       	//else {
            synchronized(showMessThread_monitor) {
				//System.out.println( "++++ NOTIFYING  showMessThread of new message");
                showMessThread_monitor.notifyAll();
            }
        //}
        
	}

	MessageContainer getMessage() {
		try {
			if (messageIn == messageOut) return (null);
			messageOut++;
			if (messageOut >= maxmessages) messageOut = 0;
			int mess2return = messageOut;
			if (messageBuffer[mess2return].valid != true) return(null);
			messageBuffer[mess2return].valid = false;	// mark as processed
			return (messageBuffer[mess2return]);
		} catch (Exception e) {}
		return (null);
	}

	// ----------------------------------------------------
	void clearMess(final int func) {

		String message = "";
		if ((func & 1) > 0) {
			message = applFullName + " V" + applMajorVersion + "." + applMinorVersion + " ready. (" + systemOSname + ", " + systemOSversion + ", " + systemOSarchitecture + ", JVM: " + VMversion + ", Java home: " + VMhome + "),\n" + (version_xerces.equals("?") == false ? version_xerces : "Xerces:?") + ", " + (version_xalan2x.equals("?") == false ? version_xalan2x : "Xalan:?") + ", Status: " + (environmentOK == true ? "OK" : "FAILED") + "\nApp Dir: " + appDir + "\n";
		}
		if ((func & 2) > 0) {
			message = gs_version_str + "\n";
		}

		showMess(message, true, true);
	}

	// ----------------------------------------------------
	void showMessWait(final String s) {
		if ((s == null) || (s.equals("") == true)) return;
        
		Runnable doshowMessWait = new Runnable() {
            @Override
			public void run() {
				Thread.currentThread().setName("showMessWait");
				showMess(s, false, false);
			}
		};
		SwingUtilities.invokeLater(doshowMessWait);
    }



	showMessThread showMessThread_runner = null;
	boolean showMessThread_kill = false;
	boolean showMessThread_is_running = false;
    private static final Object showMessThread_monitor = new Object();

	class showMessThread extends Thread {
		showMessThread() {
		}

        @Override
		public void run() { // display message
			showMessThread_is_running = true;
			int curpri = getPriority();
			setPriority(curpri+1);
            String messStr;
            String logStr;
            String logHead;
            MessageContainer mess;

			Thread.currentThread().setName("showMessThread");

			// wait for notification on new messages
			synchronized(showMessThread_monitor) {
				while (showMessThread_kill == false) {
					messStr = "";
					logStr = "";
					logHead = "";
					//System.out.println( "++++ being NOTIFIED showMessThread");
					mess = getMessage();
					while ((mess != null) && (mess.equals("") == false)) {
						messStr += mess.message;
						//System.out.println( mess.message);
						if (mess.clear == true) {
							//System.out.println( "-------- writing with clear");
							doshowMess(messStr, mess.clear);
							messStr = "";
						}
						// if write to Log file
						if (logfile_write > 0) {
							if ((mess.message.charAt(0) == '\n') || (mess.message.charAt(0) == '\r')) {
								logHead = "\n";
								while ( (mess.message.length() >= 1) && ((mess.message.charAt(0) == '\n') || (mess.message.charAt(0) == '\r'))) {
									if ((mess.message.length() >= 2)) mess.message = mess.message.substring(1);
									else mess.message = "";
								}
							}
							logStr += logHead + mess.timestamp + mess.message;
						}
						// get next message if available (or is null)
						mess = getMessage();
					}

					if ((mess == null) && (messStr.equals("") == false)) {
						//System.out.println( "-------- writing");
						doshowMess(messStr, false);
					}
					if ((logfile_write > 0) && (logStr.equals("") == false)) {
					   //System.out.println( "-------- do_write_logfile");
					   do_write_logfile("", logStr);
					}
//break;
					try {
						showMessThread_monitor.wait();
					} catch(InterruptedException e) {}
				}

			}
			//System.out.println( "##### showMessThread DONE");
            showMessThread_runner = null;
            showMessThread_is_running = false;
		}
	}

	public void showMess(final String s) {
		if ((s == null) || (s.equals("") == true)) return;
		showMess(s, true, false);
	}
	public void showMess(final String s, final boolean writelog) {
		if ((s == null) || (s.equals("") == true)) return;
		showMess(s, writelog, false);
	}
	public void showMess(final String message, final boolean writelog, final boolean clear) {
		if (message == null) return;
		// store the message in buffer and start the displayer
		storeMessage(message, writelog, clear);
	}

//	void doshowMess(final String timestamp, final String message, final boolean writelog, final boolean clear) {
	void doshowMess(final String message, final boolean clear) {
		if (message == null) return;
	//	do_setMessageArea(message, clear);
        
        Runnable dosetMessageArea = new Runnable() {
            @Override
			public void run() {
				do_setMessageArea(message, clear);
			}
		};
        SwingUtilities.invokeLater(dosetMessageArea);
        
	}
	
	Object do_setMessageArea(final String message, final boolean clear) {
		try {
			if (clear == true) messageArea.setText("");
			
			messageArea.append(message);
			int textlen = messageArea.getText().length();
			messageArea.setCaretPosition( textlen );
			Rectangle2D rect;
			rect = messageArea.modelToView2D(textlen);	// get bounding box of last character
			rect.setRect(0,rect.getY(),1,rect.getHeight());
			messageArea.scrollRectToVisible((Rectangle)rect);
			//messageArea.invalidate();
			//messageArea.paintImmediately(messageArea.getVisibleRect());
			messageArea.repaint(messageArea.getVisibleRect());
		} catch (Exception ex) {}
		return (null);
	}
	
	void do_write_logfile(final String timestamp, final String message) {
		String linehead = "";
		String mymess = message;
		int i;
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
        BufferedWriter bw = null;
		for (i=0; i < mymess.length(); i++) {
			if ((mymess.charAt(i) == '\n') || (mymess.charAt(i) == '\r')) linehead += mymess.charAt(i);
			else break;
		}
		if (i > 0) mymess = mymess.substring(i);
		
		// check the log file
		if (logfile_path.equals("") == true) logfile_path = userHome;
		if (logfile_path.endsWith(File.separator) == false)  logfile_path = logfile_path + File.separator;
		if (logfile_name.equals("")) logfile_name = default_logfile_name;
		
		File f = new File(logfile_path + logfile_name);
		if (f.exists() && (f.length() >= logfile_maxsize)) {	// rename current log file to create new one
			String indext;
			int indlen = ("" + logfile_max).length();
			// rename old log files
			for (int idx = logfile_max; idx >= 1; idx--) {
				indext = "" + idx; while (indext.length() < indlen) indext = "0" + indext;
				File lf = new File(logfile_path + logfile_name + indext);
				if (lf.exists()) {
					if (idx == logfile_max) lf.delete();	// delete oldest log
					else {
						String indextnew = "" + (idx+1); while (indextnew.length() < indlen) indextnew = "0" + indextnew;
						File rl = new File(logfile_path + logfile_name + indextnew);
						lf.renameTo(rl);
					}
				}
			}
			// rename current log file
			indext = "1"; while (indext.length() < indlen) indext = "0" + indext;
			File rl = new File(logfile_path + logfile_name + indext);
			f.renameTo(rl);
			f = new File(logfile_path + logfile_name);	// re-get our original path
		}
		// write the message to file
		try {
			fos = new FileOutputStream( f, true );	// true to append
			osw = new OutputStreamWriter(fos, "UTF8");
			bw = new BufferedWriter(osw);
			String safemess = linehead + timestamp + mymess;
			safemess = safemess.replaceAll("\r?\n", System.getProperty("line.separator"));
			bw.write(safemess);
		}
		catch (IOException ex) { }
        finally {
            try { if (bw != null) bw.flush(); } catch (IOException exbw) {}

            try { if (fos != null) fos.close(); } catch (IOException exfos) {}
            try { if (osw != null) osw.close(); } catch (IOException exosw) {}
            try { if (bw != null) bw.close(); } catch (IOException exbw) {}
       }
	}


	// ----------------------------------------------------
	private int timedGarbageCollect = 0;
	void showTime(final int startmode) {
        String hh, mm, ss, ctime;
        String cY, cM, cD, cDstr;
        Calendar cld;
        try {
            cld = Calendar.getInstance();		// get current date and time
            hh = "" + cld.get(Calendar.HOUR_OF_DAY); if (hh.length() < 2) hh = "0" + hh;
            mm = "" + cld.get(Calendar.MINUTE); if (mm.length() < 2) mm = "0" + mm;
            ss = "" + cld.get(Calendar.SECOND); if (ss.length() < 2) ss = "0" + ss;
            ctime = ( hh + ":" + mm + ":" + ss );
            timeLbl.setText( ctime );

            cY = "" + cld.get(Calendar.YEAR);
            cM = "" + (cld.get(Calendar.MONTH) + 1);
            cD = "" + cld.get(Calendar.DAY_OF_MONTH);
            cDstr = "" + cY + "/" + ((cM.length() < 2) ? ("0"+cM) : cM) + "/"  + ((cD.length() < 2) ? ("0"+cD) : cD);
            String str = "<html>Current System Date: " + cDstr + "</html>";
            timeLbl.setToolTipText(str);

            double memf = Runtime.getRuntime().freeMemory() / 1048576.0;
            String memfs = Double.valueOf(memf).toString();
            int pointpos = memfs.indexOf('.');
            if (memfs.length() > (pointpos + 2)) memfs = memfs.substring(0, pointpos + 2);

            double memt = Runtime.getRuntime().totalMemory() / 1048576.0;
            String memts = Double.valueOf(memt).toString();
            pointpos = memts.indexOf('.');
            if (memts.length() > (pointpos + 2)) memts = memts.substring(0, pointpos + 2);
            memLbl.setText( memfs + " / " + memts );

            double memused = memt - memf;
            if (memused > mempeak) mempeak = memused;
            String mempeaks = Double.valueOf(mempeak).toString();
            pointpos = mempeaks.indexOf('.');
            if (mempeaks.length() > (pointpos + 2)) mempeaks = mempeaks.substring(0, pointpos + 2);

            String memmaxs = Double.valueOf(memmax).toString();
            pointpos = memmaxs.indexOf('.');
            if (memmaxs.length() > (pointpos + 2)) memmaxs = memmaxs.substring(0, pointpos + 2);

            String memusageStr = "used peak: " + mempeaks + "MB of VM max: " + memmaxs + "MB";
            String newMemTT = "<html>" + basememlblStr + "<br>" + memusageStr + "</html>";
            memLbl.setToolTipText("<html>" + basememlblStr + "<br>" + memusageStr + "</html>");

            cld = null;
            timedGarbageCollect++;
            if (timedGarbageCollect >= 10) {
                timedGarbageCollect = 0;
                System.gc();
            }
            /*
            try { Thread.sleep(1000); }	// wait 1 s before looping
            catch (InterruptedException e) {}
            */
        }
        catch (Exception ex) {
            memLbl.setToolTipText("<html>????</html>");
        }
	}


	// ----------------------------------------------------
 	public void enable_user_pwd_port(boolean state, boolean force_init) {
 		if (state == true) {	// enable fields
			usernameLbl.setEnabled(state);
			usernameFld.setEnabled(state);
			passwordLbl.setEnabled(state);
			passwordFld.setEnabled(state);
			portLbl.setEnabled(state);
			portFld.setEnabled(state);
			ftpEncodingCBx.setEnabled(state);

			if (force_init || (!usernameFld.hasFocus() && usernameFld.getText().equals(""))) {
				usernameFld.setText(ftpUsername);
				passwordFld.setText(ftpPassword);
				portFld.setText(ftpPort);
			}
		}
		else {	// disable fields
			usernameLbl.setEnabled(state);
			usernameFld.setEnabled(state); usernameFld.setText("");
			passwordLbl.setEnabled(state);
			passwordFld.setEnabled(state); passwordFld.setText("");
			portLbl.setEnabled(state);
			portFld.setEnabled(state);  portFld.setText("");
			ftpEncodingCBx.setEnabled(state);
		}
	}


	// ----------------------------------------------------
 	public void enable_newoutputFileNameExt(boolean state) {
		newoutputFileNameExtLbl.setEnabled(state);
		newoutputFileNameExtFld.setEnabled(state);
	}


	void setItemStates(final int should_wait, final boolean manually_loaded) {
		setItemStates(should_wait, manually_loaded, false);
	}
	// ----------------------------------------------------
	void setItemStates(final int should_wait, final boolean manually_loaded, final boolean forceEnableTransformButton) {
        Runnable dosetItemStates = new Runnable() {
            public void run() {
            	do_setItemStates(manually_loaded, forceEnableTransformButton);
            }
        };
		try {
        	if (should_wait > 0) SwingUtilities.invokeAndWait(dosetItemStates);
        	else SwingUtilities.invokeLater(dosetItemStates);
        }
		catch (InvocationTargetException e1) { }
		catch (InterruptedException e2) {}
	}

 	Object do_setItemStates(boolean manually_loaded,  boolean forceEnableTransformButton) {
		if (!processing || manually_loaded) {
			runMessageLbl.setText("Idle");
			transformBtn.setText("Transform");
			transformBtn.setToolTipText(transformBtn_ToolTipText_stopped);

			modeCBx.setEnabled(true);
			sourcePathNameFld.setEnabled(true);
			sourcePathNameChooseBtn.setEnabled(true);
			excludeSourceProcessingRunExtsFld.setEnabled(true);
			xslPathNameFld.setEnabled(true);
			xslPathNameChooseBtn.setEnabled(true);
			xslt_paramsArea.setEnabled(true);
			outputPathNameFld.setEnabled(true);
			outputPathNameChooseBtn.setEnabled(true);

			int	targtype = BatchXSLTransform.get_URL_file_type(outputPathNameFld.getText());
			if (targtype >= 2 )	enable_user_pwd_port(true, false); 	// an URL
			else {	// check if the sourceFileActionFld contains an ftp target
				targtype = BatchXSLTransform.get_URL_file_type(sourceFileActionFld.getText());
				if (targtype >= 2 )	enable_user_pwd_port(true, false); 		// an URL
				else enable_user_pwd_port(false, false);					// a local file
			}

			if (targtype == 0 )	enable_newoutputFileNameExt(false); 	// does not create an output file ( -evtl. to a DB ? )
			else enable_newoutputFileNameExt(true);

			sourceFileActionCBx.setEnabled(true);
 			switch ( sourceFileActionCBx.getSelectedIndex() ) {
 				case 2:
					sourceFileActionFld.setEnabled(true);
					sourceFileActionChooseBtn.setEnabled(true);
 					break;
 				default:
					sourceFileActionFld.setEnabled(false);
					sourceFileActionChooseBtn.setEnabled(false);
 				break;
 			}
			excludeCleanupRunExtsFld.setEnabled(true);
			deleteSourceDirsCheckBox.setEnabled(true);

/*
			folderindexnameFld.setEnabled(true);
			folderindexXSLFld.setEnabled(true);
			folderindexXSLChooseBtn.setEnabled(true);
*/

			loadJTBtn.setEnabled(true);
			saveJTBtn.setEnabled(true);
			
			delayJTFld.setEnabled(true);
			nextJTNameFld.setEnabled(true);
			nextJTNameChooseBtn.setEnabled(true);
			jt_scheduleFld.setEnabled(true);
		}
		else {
			transformBtn.setText("Hold");
			transformBtn.setToolTipText(transformBtn_ToolTipText_running);

			modeCBx.setEnabled(false);
			sourcePathNameFld.setEnabled(false);
			sourcePathNameChooseBtn.setEnabled(false);
			excludeSourceProcessingRunExtsFld.setEnabled(false);
			xslPathNameFld.setEnabled(false);
			xslPathNameChooseBtn.setEnabled(false);
			xslt_paramsArea.setEnabled(false);
			outputPathNameFld.setEnabled(false);
			outputPathNameChooseBtn.setEnabled(false);

			usernameFld.setEnabled(false);
			passwordFld.setEnabled(false);
			portFld.setEnabled(false);
			ftpEncodingCBx.setEnabled(false);

			enable_newoutputFileNameExt(false);

			sourceFileActionCBx.setEnabled(false);
			sourceFileActionFld.setEnabled(false);
			sourceFileActionChooseBtn.setEnabled(false);
			excludeCleanupRunExtsFld.setEnabled(false);
			deleteSourceDirsCheckBox.setEnabled(false);

/*
			folderindexnameFld.setEnabled(false);
			folderindexXSLFld.setEnabled(false);
			folderindexXSLChooseBtn.setEnabled(false);
*/

			loadJTBtn.setEnabled(false);
			saveJTBtn.setEnabled(false);
			
			delayJTFld.setEnabled(false);
			nextJTNameFld.setEnabled(false);
			nextJTNameChooseBtn.setEnabled(false);
			jt_scheduleFld.setEnabled(false);
		}
 
		if (forceEnableTransformButton == true) transformBtn.setEnabled(true);
		else {	// auto detect how button should be set
			if ((sourcePathNameFld.getText().equals("")) 
				|| ((sourcePathNameFld.getText().startsWith("**") == true))	// just an info message
				|| ((sourcePathNameFld.getText().startsWith("->") == true))	// "
				|| ((sourcePathNameFld.getText().startsWith("-->") == true))	// "
				|| ((xslPathNameFld.getText().startsWith("**") == true))	// just an info message
				|| ((xslPathNameFld.getText().startsWith("->") == true))	// "
				|| ((xslPathNameFld.getText().startsWith("-->") == true))	// "
				|| ((outputPathNameFld.getText().startsWith("**") == true))	// just an info message
				|| ((outputPathNameFld.getText().startsWith("->") == true))	// "
				|| ((outputPathNameFld.getText().startsWith("-->") == true))	// "
				|| ((sourceFileActionFld.getText().startsWith("**") == true))	// just an info message
				|| ((sourceFileActionFld.getText().startsWith("->") == true))	// "
				|| ((sourceFileActionFld.getText().startsWith("-->") == true))	// "
				) transformBtn.setEnabled(false);
			else transformBtn.setEnabled(true);
		}
		return (null);
	}


	// ----------------------------------------------------
	void setDialogFields(final boolean force_init) {
        Runnable dosetDialogFields = new Runnable() {
            public void run() {
            	do_setDialogFields(force_init);
            }
        };
		//SwingUtilities.invokeLater(dosetDialogFields);
		try { SwingUtilities.invokeAndWait(dosetDialogFields); }
		catch (InvocationTargetException e1) { }
		catch (InterruptedException e2) {}
	}

 	Object do_setDialogFields(boolean force_init) {
 		String jt_f_name = jobTicketFileName;
		
 		if (jobTicketFileName.lastIndexOf(File.separator) > -1)
 			jt_f_name = jobTicketFileName.substring(jobTicketFileName.lastIndexOf(File.separator)+1,jobTicketFileName.length());
 		if (jobTicketFileName.equals("") == false)		// set the main frame's title
			setTitle(applFullName + " " + applMajorVersion + "." + applMinorVersion + ((xslVersionName.equals("") == false) ? ("." + xslVersionName) : "") + "   ( " + jt_f_name + " )");
		else setTitle(applFullName + " " + applMajorVersion + "." + applMinorVersion + ((xslVersionName.equals("") == false) ? ("." + xslVersionName) : ""));
		if ((mode >= 0) && (mode <= 2)) modeCBx.setSelectedIndex(mode);
		else modeCBx.setSelectedIndex(0);
		sourcePathNameFld.setText(sourcePathName);
		excludeSourceProcessingRunExtsFld.setText(excludeSourceProcessingRunFileNameExts);

		xslPathNameFld.setText(xslPathName);
		xslt_paramsArea.setText(xslParams); xslt_paramsArea.setCaretPosition(0);
		outputPathNameFld.setText(outputPathName);

		int	targtype = BatchXSLTransform.get_URL_file_type(outputPathName);
		if (targtype >= 2 )	enable_user_pwd_port(true, force_init); 	// an URL
		else {	// check if the sourceFileActionFld contains an ftp target
			targtype = BatchXSLTransform.get_URL_file_type(sourceFileActionFld.getText());
			if (targtype >= 2 )	enable_user_pwd_port(true, force_init); 		// an URL
			else enable_user_pwd_port(false, force_init);					// a local file
		}
		
		if (sourceFileAction.equals("")) {					// Nothing
			sourceFileActionCBx.setSelectedIndex(1);
			sourceFileActionFld.setText("");
			sourceFileActionFld.setEnabled(false);
			sourceFileActionChooseBtn.setEnabled(false);
		}
		else {
			if (sourceFileAction.equals("*DELETE*")) {		// *DELETE*
				sourceFileActionCBx.setSelectedIndex(0);
				sourceFileActionFld.setText("");
				sourceFileActionFld.setEnabled(false);
				sourceFileActionChooseBtn.setEnabled(false);
			}
			else {											// a move folder is given
				sourceFileActionCBx.setSelectedIndex(2);
				sourceFileActionFld.setText(sourceFileAction);
				sourceFileActionFld.setEnabled(true);
				sourceFileActionChooseBtn.setEnabled(true);
			}
		}

		newoutputFileNameExtFld.setText(newoutputFileNameExt);
		excludeCleanupRunExtsFld.setText(excludeCleanupRunFileNameExts);
		deleteSourceDirsCheckBox.setSelected(deleteSourceDirs > 0);

/*
		folderindexnameFld.setText(folderIndexFileName);
		folderindexXSLFld.setText(folderIndexFileXSLName);
*/
		nextJTNameFld.setText(nextJobTicketFileName);
		delayJTFld.setText(Integer.valueOf(loopDelay).toString());
		jt_scheduleFld.setText(jt_schedule);

		ImageMagickPathNameFld.setText(im_pgm_path);
		GhostscriptPathNameFld.setText(gs_pgm_path);
		return (null);
	}


	// ----------------------------------------------------
 	public void getDialogFields() {
 		mode = modeCBx.getSelectedIndex();
		sourcePathName = sourcePathNameFld.getText();
		excludeSourceProcessingRunFileNameExts = excludeSourceProcessingRunExtsFld.getText();
		xslPathName = xslPathNameFld.getText();
		xslParams = xslt_paramsArea.getText();
		outputPathName = outputPathNameFld.getText();
		char[]	tmpchr;
		int i;


		ftpUsername = usernameFld.getText();
		ftpPassword = "";
		tmpchr = passwordFld.getPassword();
		for (i = 0; i < tmpchr.length; i++) {
			ftpPassword += tmpchr[i];
		}

		ftpPort = portFld.getText();
		ftpEncoding = "" + ftpEncodingCBx.getSelectedItem();
		
		switch (sourceFileActionCBx.getSelectedIndex()) {
			case 0 : sourceFileAction = "*DELETE*"; break;		// delete
			case 1 : sourceFileAction = ""; break;				// Nothing
			case 2 : sourceFileAction = sourceFileActionFld.getText(); break;	// move folder path
		}

		newoutputFileNameExt = newoutputFileNameExtFld.getText();
		excludeCleanupRunFileNameExts = excludeCleanupRunExtsFld.getText();
		if (deleteSourceDirsCheckBox.isSelected() == true) deleteSourceDirs = 1;
		else deleteSourceDirs = 0;

/*
		folderIndexFileName = folderindexnameFld.getText();
		folderIndexFileXSLName = folderindexXSLFld.getText();
*/
		nextJobTicketFileName = nextJTNameFld.getText();
		loopDelay = Integer.valueOf(delayJTFld.getText());
		jt_schedule = jt_scheduleFld.getText();

		im_pgm_path = ImageMagickPathNameFld.getText();
		gs_pgm_path = GhostscriptPathNameFld.getText();

	}

	
	/* --------------------------------------------------------
	 * --------------------------------------------------------
	 * dialog item actions
	 * --------------------------------------------------------
	 * -------------------------------------------------------- */


	// Close main window and exit when the close box is clicked
	void thisWindowClosing(java.awt.event.WindowEvent e) {
		thisWindowClosing(e, 0);
	}
	void thisWindowClosing(java.awt.event.WindowEvent e, int theexitcode) {
		int exitcode = 0;
		if (theexitcode != 0) exitcode = theexitcode;
		setVisible(false);
		dispose();
		System.exit(exitcode);
	}

	// popup buttons action
	public void userControlsPaneContainerMouseClicked(java.awt.event.MouseEvent e) {
		//showMess("X: '" + e.getX() + " , Y: " + e.getY() + "\n");
		if ( (e.getY() > 17) || (e.getX() > 250)) return;
		if (userControlsPaneContainerIsVisible == true) {	// change to invisible
			hideUserControls();
		}
		else {
			showUserControls();
		}
	}		

	void hideUserControls() {
		userControlsPaneContainerIsVisible = false;
		userControlsPaneContainer.setBorder(userControlsPaneContainerIsInvisibleBorder);
		userControlsPaneContainer.setSize( new java.awt.Dimension(userControlsPaneContainer.getWidth(), 22));
		global_frame.setSize(new java.awt.Dimension(getMainWindowWidth(), getMainWindowHeight() - userControlsPane.getHeight()));
	}
	void showUserControls() {
		userControlsPaneContainerIsVisible = true;
		userControlsPaneContainer.setBorder(userControlsPaneContainerIsVisibleBorder);
		userControlsPaneContainer.setSize( new java.awt.Dimension(userControlsPaneContainer.getWidth(), userControlsPane.getHeight()+22));
		setMainWindowSize();
	}

	public void setMainWindowSize() {
		if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
			global_frame.setSize(new java.awt.Dimension(window_current_Dimension.width+8, window_current_Dimension.height + topFrameMenubarHeight));
		}
		else global_frame.setSize(new java.awt.Dimension(window_current_Dimension.width, window_current_Dimension.height + topFrameMenubarHeight));
	}

	public int getMainWindowWidth() {
		if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
			return (window_current_Dimension.width+12);
		}
		else return(window_current_Dimension.width);
	}
	public int getMainWindowHeight() {
		return(window_current_Dimension.height + topFrameMenubarHeight);
	}
	public int getMainWindowLeft() {
		return(global_frame.getLocation().x);
	}
	public int getMainWindowTop() {
		return(global_frame.getLocation().y);
	}




	// popup buttons action
	public void popupBtnMouseClicked(int which, java.awt.event.MouseEvent e) {
		do_show_popup(which);
	}		

	public void hide_all_popup_content() {
		sourcePopBtn.setForeground(popBtnColor_deactivated);
		sourcePopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		xslPopBtn.setForeground(popBtnColor_deactivated);
		xslPopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		outputPopBtn.setForeground(popBtnColor_deactivated);
		outputPopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		cleanupPopBtn.setForeground(popBtnColor_deactivated);
		cleanupPopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		nextjobticketPopBtn.setForeground(popBtnColor_deactivated);
		nextjobticketPopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		imagePopBtn.setForeground(popBtnColor_deactivated);
		imagePopBtn.setBorder(new LineBorder(popBtnBorder_deactivated, 1));

		sourcePane.setLocation(new java.awt.Point(sourcePane.getLocation().x, -10000));
		xslPane.setLocation(new java.awt.Point(xslPane.getLocation().x, -10000));
		outputPane.setLocation(new java.awt.Point(outputPane.getLocation().x, -10000));
		cleanupPane.setLocation(new java.awt.Point(cleanupPane.getLocation().x, -10000));
		nextjobticketPane.setLocation(new java.awt.Point(nextjobticketPane.getLocation().x, -10000));
		imagePane.setLocation(new java.awt.Point(imagePane.getLocation().x, -10000));
	}
	public void do_show_popup(int which) {
		for ( int i = 0; i < showpopupflags.length; i++) showpopupflags[i] = false;
		hide_all_popup_content();
		switch (which) {
			case 1:	// 	sourcePane
				sourcePopBtn.setForeground(popBtnColor_activated);
				sourcePopBtn.setBorder(new LineBorder(popBtnBorder_activated, 2));
				sourcePane.setLocation(new java.awt.Point(0,0));
				break;
			case 2:	// 	xslPane
				xslPopBtn.setForeground(popBtnColor_activated);
				xslPopBtn.setBorder(new LineBorder(popBtnBorder_activated, 2));
				xslPane.setLocation(new java.awt.Point(0,0));
				break;
			case 3:	// 	outputPane
				outputPopBtn.setForeground(popBtnColor_activated);
				outputPopBtn.setBorder(new LineBorder(popBtnBorder_activated, 2));
				outputPane.setLocation(new java.awt.Point(0,0));
				break;
			case 4:	// 	cleanupPane
				cleanupPopBtn.setForeground(popBtnColor_activated);
				cleanupPopBtn.setBorder(new LineBorder(popBtnBorder_activated, 2));
				cleanupPane.setLocation(new java.awt.Point(0,0));
				break;
			case 5:	// 	nextjobticketPane
				nextjobticketPopBtn.setForeground(popBtnColor_activated);
				nextjobticketPopBtn.setBorder(new LineBorder(popBtnBorder_activated, 2));
				nextjobticketPane.setLocation(new java.awt.Point(0,0));
				break;
			case 6:	// 	imagePane
				imagePopBtn.setForeground(popBtnColor_activated);
				imagePopBtn.setBorder(new LineBorder(popBtnBorder_activated, 2));
				imagePane.setLocation(new java.awt.Point(0,0));
				break;
		}
	}		


	boolean[] showpopupflags = new boolean[6];	// set up the command to send to the OS to call Ghostscript

	public void cancel_show_popup(int which) {
		showpopupflags[which - 1] = false;
	}

	void show_popup(final int which) {
		for ( int i = 0; i < showpopupflags.length; i++) showpopupflags[i] = false;
		showpopupflags[which - 1] = true;
		showPopupThread spopt = new showPopupThread(which);
		spopt.start();
	}
	class showPopupThread extends Thread {
		int which = 0;
		showPopupThread(final int which) {
			this.which = which;
		}

		public void run() { // display message
			Thread.currentThread().setName("showPopupThread");
			if (showpopupflags[which - 1] == false) return;
			try { Thread.sleep(500); }	// wait
			catch (InterruptedException e) {}
			if (showpopupflags[which - 1] == false) return;
			do_show_popup(which);
			showpopupflags[which - 1] = false;
		}
	}



	public void modeCBxItemStateChanged(java.awt.event.ItemEvent e) {
	}		


	
	public void sourcePathNameChooseBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!sourcePathNameChooseBtn.isEnabled()) return;
		processing_disabled = true;
		File sf = sourcePathNameFld.getText().equals("") ? new File("") : new File(sourcePathNameFld.getText());
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose path/name for source file(s)...");
		fc.setDialogType(fc.OPEN_DIALOG);
		fc.setFileSelectionMode(fc.FILES_AND_DIRECTORIES);
		fc.setSelectedFile(sf);
		fc.setCurrentDirectory(sf);
		//int returnVal = fc.showDialog(mainXSLTFrame.this, "Choose");
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			String my_sourcePathName = (sf.getPath() != null ? sf.getPath() : "");
			if (sf.isDirectory() == true) my_sourcePathName += File.separator;
			sourcePathNameFld.setText(my_sourcePathName);
		}
		processing_disabled = false;
		// set the dialog item states
		setItemStates(0,false);
	}		
	public void sourcePathNameFldKeyTyped(java.awt.event.KeyEvent e) {
		// set the dialog item states
		setItemStates(0,false);
	}		

	public void xslPathNameChooseBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!xslPathNameChooseBtn.isEnabled()) return;
		processing_disabled = true;
		File sf = xslPathNameFld.getText().equals("") ? new File(userDir + File.separator,"") : new File(xslPathNameFld.getText());
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose path/name of the XSL file to use...");
		fc.setFileSelectionMode(fc.FILES_ONLY);
		fc.setSelectedFile(sf);
		fc.setCurrentDirectory(sf);
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			String my_xslPathName = (sf.getPath() != null ? sf.getPath() : "");
			xslPathNameFld.setText(my_xslPathName);
		}
		processing_disabled = false;
	}		
	public void xslPathNameFldKeyTyped(java.awt.event.KeyEvent e) {
		// set the dialog item states
		setItemStates(0,false);
	}		

	public void outputPathNameChooseBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!outputPathNameChooseBtn.isEnabled()) return;
		processing_disabled = true;
		File sf = outputPathNameFld.getText().equals("") ? new File("") : new File(outputPathNameFld.getText());
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose path/name for the output file(s)...");
		fc.setFileSelectionMode(fc.FILES_AND_DIRECTORIES);
		fc.setSelectedFile(sf);
		fc.setCurrentDirectory(sf);
		//int returnVal = fc.showDialog(mainXSLTFrame.this, "Choose");
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			String my_outputPathName = (sf.getPath() != null ? sf.getPath() : "");
			if (sf.isDirectory() == true) my_outputPathName += File.separator;
			outputPathNameFld.setText(my_outputPathName);
			outputPathName = my_outputPathName;
		}
		processing_disabled = false;
	}		

	public void outputPathNameFldKeyTyped(java.awt.event.KeyEvent e) {
		// check if we have an URL and therefore should enable/disable user/pwd and port fields
		int	targtype = BatchXSLTransform.get_URL_file_type(outputPathNameFld.getText());
		outputPathName = outputPathNameFld.getText();
		if (targtype >= 2 )	enable_user_pwd_port(true, false); 		// an URL
		else {	// check if the sourceFileActionFld contains an ftp target
			targtype = BatchXSLTransform.get_URL_file_type(sourceFileActionFld.getText());
			if (targtype >= 2 )	enable_user_pwd_port(true, false); 		// an URL
			else enable_user_pwd_port(false, false);					// a local file
		}
		if (targtype == 0 )	enable_newoutputFileNameExt(false); 	// does not create an output file ( -evtl. to a DB ? )
		else enable_newoutputFileNameExt(true);
		// set the dialog item states
		setItemStates(0,false);
	}		

	
	public void passwordFldActionPerformed(java.awt.event.ActionEvent e) {
	}
	public void passwordFldKeyTyped(java.awt.event.KeyEvent e) {
	}

	public void transformBtnMouseClicked(java.awt.event.MouseEvent e) {
		int retval = startTransform();
	}		


	public void loadJTBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!loadJTBtn.isEnabled()) return;
		BatchXSLTransform.numberof_readJobTicketFile_calls = 0;
		processing_disabled = true;
		File sf = jobTicketFileName.equals("") ? new File(userDir + File.separator,"autostart.jt") : new File(jobTicketFileName);
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose path/name of the JobTicket file to load...");
		fc.setFileSelectionMode(fc.FILES_ONLY);
		fc.setSelectedFile(sf);
		fc.setCurrentDirectory(sf);
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			int myErr = 0;
			sf = fc.getSelectedFile();
			jobTicketFileName = (sf.getPath() != null ? sf.getPath() : "");
			if (jobTicketFileName.compareTo("") != 0) {
				saveSettings();			// save the current vars settings
				myErr = BatchXSLTransform.readJobTicketFile(2);
				if (myErr > 0) {
					jobticket_loaded = false;
					mode = 0;	// set manual mode on error
					restoreSettings();		// restore the previous vars settings
				}
			}
		}
		processing_disabled = false;
	}		

	public void saveJTBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!saveJTBtn.isEnabled()) return;
		BatchXSLTransform.numberof_readJobTicketFile_calls = 0;
		processing_disabled = true;
		File sf = jobTicketFileName.equals("") ? new File(userDir + File.separator,"autostart.jt") : new File(jobTicketFileName);
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose path/name to save this JobTicket file to...");
		fc.setFileSelectionMode(fc.FILES_ONLY);
		fc.setSelectedFile(sf);
		fc.setCurrentDirectory(sf);
		int returnVal = fc.showSaveDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			jobTicketFileName = (sf.getAbsolutePath() != null ? sf.getAbsolutePath() : "");
			if (jobTicketFileName.compareTo("") != 0) {
				saveSettings();			// save the current vars settings
				getDialogFields();		// get the current dialog settings
				BatchXSLTransform.saveToJobTicketFile();
				restoreSettings();		// restore the previous vars settings
			}
		}
		processing_disabled = false;
	}		


	public void quitBtnMouseClicked(java.awt.event.MouseEvent e) {
		thisWindowClosing(null);
	}
	public void newoutputFileNameExtFldKeyTyped(java.awt.event.KeyEvent e) {
	}		
	
	public void excludeCleanupRunExtsFldKeyTyped(java.awt.event.KeyEvent e) {
	}
	
	public void excludeSourceProcessingRunExtsFldKeyTyped(java.awt.event.KeyEvent e) {
	}


	public void delayJTFldKeyTyped(java.awt.event.KeyEvent e) {
	}
	
	public void nextJTNameChooseBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!nextJTNameChooseBtn.isEnabled()) return;
		processing_disabled = true;
		File sf = jobTicketFileName.equals("") ? new File(userDir + File.separator,"autostart.jt") : new File(jobTicketFileName);
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose path/name of the next JobTicket file to call...");
		fc.setFileSelectionMode(fc.FILES_ONLY);
		fc.setSelectedFile(sf);
		fc.setCurrentDirectory(sf);
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			String canpath = "";
			try {
				canpath = sf.getCanonicalPath();
			} catch (Exception ex) {}
			nextJobTicketFileName = canpath;
			nextJTNameFld.setText(nextJobTicketFileName);
		}
		processing_disabled = false;
	}
	public void nextJTNameFldKeyTyped(java.awt.event.KeyEvent e) {
	}		
	public void jt_scheduleFldKeyTyped(java.awt.event.KeyEvent e) {
	}		
	
	public void ImageMagickPathNameChooseBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!ImageMagickPathNameChooseBtn.isEnabled()) return;
		processing_disabled = true;
		File sf = im_pgm_path.equals("") ? new File(File.separator,"") : new File(im_pgm_path);
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose ImageMagick's CONVERT application...");
		fc.setFileSelectionMode(fc.FILES_ONLY);
		fc.setCurrentDirectory(sf);
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			String canpath = "";
			try {
				canpath = sf.getCanonicalPath();
			} catch (Exception ex) {}
			im_pgm_path = canpath;
			ImageMagickPathNameFld.setText(im_pgm_path);

			im_version_str = BatchXSLTImageMagick.get_IMconvert_version();
			im_version_num = BatchXSLTImageMagick.get_IMconvert_version_num(im_version_str);
			if ( im_version_str.equals("") == true ) {
				showMessWait("#### ImageMagick convert program '" + BatchXSLT.g_mainXSLTFrame.im_pgm_path + "' is NOT reachable!!\n");
				ImageMagickPathNameFld.setForeground(new java.awt.Color(200, 20, 20));
			}
			else {
				showMessWait(BatchXSLT.g_mainXSLTFrame.im_version_str + "\n");
				ImageMagickPathNameFld.setForeground(new java.awt.Color(20, 180, 20));
/* this is done by get_IMconvert_version
				// as we have found ImageMagick operational we may set the envir
				String envir = "PATH=" + sf.getParent();
				if (envir.endsWith(File.separator) == false) envir += File.separator;
				im_envir = gs_envir;	// ImageMagick must know path to Ghostscript
*/
			}
		}
		processing_disabled = false;
	}
	public void ImageMagickPathNameFldKeyTyped(java.awt.event.KeyEvent e) {
		String pathstr = ImageMagickPathNameFld.getText();

		File sf = new File(pathstr);
		if ( (sf.exists() == true) && (sf.isFile() == true) ) {
			String canpath = "";
			try {
				canpath = sf.getCanonicalPath();
			} catch (Exception ex) {}
			im_pgm_path = canpath;
		}
		else {
			im_pgm_path = pathstr;
		}
		im_version_str = BatchXSLTImageMagick.get_IMconvert_version();
		im_version_num = BatchXSLTImageMagick.get_IMconvert_version_num(im_version_str);
		if ( im_version_str.equals("") == false ) {
			showMessWait(im_version_str + "\n");
			ImageMagickPathNameFld.setForeground(new java.awt.Color(20, 180, 20));
/* this is done by get_IMconvert_version
			// as we have found ImageMagick operational we may set the envir
			String envir = "PATH=" + sf.getParent();
			if (envir.endsWith(File.separator) == false) envir += File.separator;
			im_envir = gs_envir;	// ImageMagick must know path to Ghostscript
*/
		}
		else {
			ImageMagickPathNameFld.setForeground(new java.awt.Color(20, 20, 20));
		}
	}		

	public void GhostscriptPathNameChooseBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!GhostscriptPathNameChooseBtn.isEnabled()) return;
		processing_disabled = true;
		File sf = gs_pgm_path.equals("") ? new File(File.separator,"") : new File(gs_pgm_path);
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose the Ghostscript application...");
		fc.setFileSelectionMode(fc.FILES_ONLY);
		fc.setCurrentDirectory(sf);
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			String canpath = "";
			try {
				canpath = sf.getCanonicalPath();
			} catch (Exception ex) {}
			gs_pgm_path = canpath;
			GhostscriptPathNameFld.setText(gs_pgm_path);

			gs_version_str = BatchXSLTGhostScript.get_GS_version();
			gs_version_num = BatchXSLTGhostScript.get_GS_version_num(gs_version_str);
			if ( gs_version_str.equals("") == true ) {
				showMessWait("#### GhostScript program '" + BatchXSLT.g_mainXSLTFrame.gs_pgm_path + "' is NOT reachable!!\n");
				GhostscriptPathNameFld.setForeground(new java.awt.Color(200, 20, 20));
			}
			else {
				showMessWait(BatchXSLT.g_mainXSLTFrame.gs_version_str + "\n");
				GhostscriptPathNameFld.setForeground(new java.awt.Color(20, 180, 20));
/* this is done by get_GS_version
				// as we have found Ghostscript operational we may set the envir
				String envir = "PATH=" + sf.getParent();
				if (envir.endsWith(File.separator) == false) envir += File.separator;
				gs_envir = envir;
				im_envir = envir;	// ImageMagick must know it too
*/
			}
		}
		processing_disabled = false;
	}
	public void GhostscriptPathNameFldKeyTyped(java.awt.event.KeyEvent e) {
		String pathstr = GhostscriptPathNameFld.getText();

		File sf = new File(pathstr);
		if ( (sf.exists() == true) && (sf.isFile() == true) ) {
			String canpath = "";
			try {
				canpath = sf.getCanonicalPath();
			} catch (Exception ex) {}
			gs_pgm_path = canpath;
		}
		else {
			gs_pgm_path = pathstr;
		}
		gs_version_str = BatchXSLTGhostScript.get_GS_version();
		gs_version_num = BatchXSLTGhostScript.get_GS_version_num(gs_version_str);
		if ( gs_version_str.equals("") == false ) {
			showMessWait(BatchXSLT.g_mainXSLTFrame.gs_version_str + "\n");
			GhostscriptPathNameFld.setForeground(new java.awt.Color(20, 180, 20));
/* this is done by get_GS_version
			// as we have found GhostScript operational we may set the envir
			String envir = "PATH=" + sf.getParent();
			if (envir.endsWith(File.separator) == false) envir += File.separator;
			gs_envir = envir;
			im_envir = envir;	// ImageMagick must know it too
*/
		}
		else {
			GhostscriptPathNameFld.setForeground(new java.awt.Color(20, 20, 20));
		}
	}		



	public void sourceFileActionFldKeyTyped(java.awt.event.KeyEvent e) {
		int	targtype = BatchXSLTransform.get_URL_file_type(sourceFileActionFld.getText());
		if (targtype >= 2 )	enable_user_pwd_port(true, false); 		// an URL
		else {	// check if the sourceFileActionFld contains an ftp target
			targtype = BatchXSLTransform.get_URL_file_type(outputPathNameFld.getText());
			if (targtype >= 2 )	enable_user_pwd_port(true, false); 		// an URL
			else enable_user_pwd_port(false, false);					// a local file
		}
		// set the dialog item states
		setItemStates(0,false);
	}
	
	public void sourceFileActionChooseBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!sourceFileActionChooseBtn.isEnabled()) return;
		processing_disabled = true;
		File sf = sourceFileActionFld.getText().equals("") ? new File(userDir + File.separator,"") : new File(sourceFileActionFld.getText());
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose the path to move processed file(s) to...");
		fc.setFileSelectionMode(fc.DIRECTORIES_ONLY);
		fc.setSelectedFile(sf);
		fc.setCurrentDirectory(sf);
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			String my_sourceFileAction = (sf.getPath() != null ? sf.getPath() : "");
			if (sf.isDirectory() == true) my_sourceFileAction += File.separator;
			sourceFileActionFld.setText(my_sourceFileAction);
		}
		processing_disabled = false;
	}
	
	public void sourceFileActionCBxItemStateChanged(java.awt.event.ItemEvent e) {
 		switch ( sourceFileActionCBx.getSelectedIndex() ) {
 			case 2:
					sourceFileActionFld.setEnabled(true);
					sourceFileActionChooseBtn.setEnabled(true);
 			
 				break;
 			default:
					sourceFileActionFld.setText("");
					sourceFileActionFld.setEnabled(false);
					sourceFileActionChooseBtn.setEnabled(false);
 				break;
 		}
	}
	
	public void tTiponoffBtnMouseClicked(java.awt.event.MouseEvent e) {
		// enable/disable the tooltips
		ttipstate = ToolTipManager.sharedInstance().isEnabled();
		if (ttipstate == true) ttipstate = false;
		else ttipstate = true;
		ToolTipManager.sharedInstance().setEnabled(ttipstate);
	}
	
	public void xslt_paramsAreaMouseClicked(java.awt.event.MouseEvent e) {
	}
	
	public void xslt_paramsAreaKeyTyped(java.awt.event.KeyEvent e) {
	}
	
	public void outputPathNameFldActionPerformed(java.awt.event.ActionEvent e) {
	}
	
	public void deleteSourceDirsCheckBoxMouseClicked(java.awt.event.MouseEvent e) {
		if (deleteSourceDirsCheckBox.isSelected() == true) {
			deleteSourceDirs = 1;
		}
		else {
			deleteSourceDirs = 0;
		}
	}
	public void deleteSourceDirsCheckBoxStateChanged(javax.swing.event.ChangeEvent e) {
		return;
	}

	public void ftpEncodingCBxItemStateChanged(java.awt.event.ItemEvent e) {
		String enc = "" + ftpEncodingCBx.getSelectedItem();
		ftpEncoding = enc;

		return;
	}		
	public void set_ftpEncodingCBxItemByName(String itemname) {
		ftpEncodingCBx.setSelectedItem(itemname);	// this also fires ftpEncodingCBxItemStateChanged
		return;
	}		


/*
	public void folderindexnameFldKeyTyped(java.awt.event.KeyEvent e) {
		folderIndexFileName = folderindexnameFld.getText();
	}
	
	public void folderindexXSLFldKeyTyped(java.awt.event.KeyEvent e) {
		folderIndexFileXSLName = folderindexXSLFld.getText();
	}
	
	public void folderindexXSLChooseBtnMouseClicked(java.awt.event.MouseEvent e) {
		if (!folderindexXSLChooseBtn.isEnabled()) return;
		processing_disabled = true;
		File sf = new File(File.separator,"");
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose path/name of the next JobTicket file to call...");
		fc.setFileSelectionMode(fc.FILES_ONLY);
		fc.setSelectedFile(sf);
		fc.setCurrentDirectory(sf);
		int returnVal = fc.showOpenDialog(mainXSLTFrame.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sf = fc.getSelectedFile();
			folderindexXSLFld.setText((sf.getPath() != null ? sf.getPath() : ""));
			folderIndexFileXSLName = folderindexXSLFld.getText();
		}
		processing_disabled = false;
	}
*/
					

	
	/* --------------------------------------------------------
	 * --------------------------------------------------------
	 * drag and drop actions
	 * --------------------------------------------------------
	 * -------------------------------------------------------- */

	net.iharder.dnd.FileDrop fd_sourcePathNameFld = new net.iharder.dnd.FileDrop( sourcePathNameFld, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
			// handle file drop
			set_sourcePathName( files );
		}   // end filesDropped
	}); // end FileDrop.Listener
	net.iharder.dnd.FileDrop fd_sourcePopBtn = new net.iharder.dnd.FileDrop( sourcePopBtn, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
			// show pop content
			show_popup(1);
			// handle file drop
			set_sourcePathName( files );
		}
	}); // end FileDrop.Listener
	void set_sourcePathName( java.io.File[] files ) {
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) my_PathName += File.separator;
			sourcePathNameFld.setText(my_PathName);
			processing_disabled = false;
			// set the dialog item states
			setItemStates(0,false);
	}
	// this is like dropping on the Transform Btn
	net.iharder.dnd.FileDrop fd_sourcePane = new net.iharder.dnd.FileDrop( sourcePane, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) my_PathName += File.separator;
			sourcePathNameFld.setText(my_PathName);

			int retval = startTransform();
		}   // end filesDropped
	}); // end FileDrop.Listener


	net.iharder.dnd.FileDrop fd_transformBtn = new net.iharder.dnd.FileDrop( transformBtn, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) my_PathName += File.separator;
			sourcePathNameFld.setText(my_PathName);

			int retval = startTransform();
		}   // end filesDropped
	}); // end FileDrop.Listener






	net.iharder.dnd.FileDrop fd_xslPathNameFld = new net.iharder.dnd.FileDrop( xslPathNameFld, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
			// handle file drop
			set_xslPathName( files );
		}   // end filesDropped
	}); // end FileDrop.Listener
	net.iharder.dnd.FileDrop fd_xslPopBtn = new net.iharder.dnd.FileDrop( xslPopBtn, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
			// show pop content
			show_popup(2);
			// handle file drop
			set_xslPathName( files );
		}
	}); // end FileDrop.Listener
	void set_xslPathName( java.io.File[] files ) {
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) return;	// file name only
			xslPathNameFld.setText(my_PathName);
			processing_disabled = false;
			// set the dialog item states
			setItemStates(0,false);
	}








	net.iharder.dnd.FileDrop fd_outputPathNameFld = new net.iharder.dnd.FileDrop( outputPathNameFld, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
			// handle file drop
			set_outputPathName( files );
		}   // end filesDropped
	}); // end FileDrop.Listener
	net.iharder.dnd.FileDrop fd_outputPopBtn = new net.iharder.dnd.FileDrop( outputPopBtn, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
			// show pop content
			show_popup(3);
			// handle file drop
			set_outputPathName( files );
		}
	}); // end FileDrop.Listener
	void set_outputPathName( java.io.File[] files ) {
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) my_PathName += File.separator;
			outputPathName = my_PathName;
			outputPathNameFld.setText(my_PathName);
			processing_disabled = false;
			// set the dialog item states
			setItemStates(0,false);
	}









	net.iharder.dnd.FileDrop fd_outputPathNameChooseBtn = new net.iharder.dnd.FileDrop( outputPathNameChooseBtn, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			processing_disabled = false;
			outputPathName = outputPathNameFld.getText();
			if (outputPathName.equals("") == true) return;
			ftpUsername = usernameFld.getText();
			ftpPort = portFld.getText();

			char[]	tmpchr; int i;
			ftpPassword = "";
			tmpchr = passwordFld.getPassword();
			for (i = 0; i < tmpchr.length; i++) {
				ftpPassword += tmpchr[i];
			}

			try { 
				startFileCopy(files, outputPathName, true,false);
			}
			catch (Exception x) {}
		}   // end filesDropped
	}); // end FileDrop.Listener


	net.iharder.dnd.FileDrop fd_sourceFileActionFld = new net.iharder.dnd.FileDrop( sourceFileActionFld, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) my_PathName += File.separator;
			else return;	// directories only
			sourceFileActionFld.setText(my_PathName);
			processing_disabled = false;
			// set the dialog item states
			setItemStates(0,false);
		}   // end filesDropped
	}); // end FileDrop.Listener


	net.iharder.dnd.FileDrop fd_nextJTNameFld = new net.iharder.dnd.FileDrop( nextJTNameFld, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");

			String jtpath = "";
			String jtname = "";
			if (files[0].isDirectory() == false) {	// a file was dropped
				try {
					jtpath = (files[0].getParent() != null ? files[0].getParent() : "");
					if (jtpath.endsWith(File.separator) == false) jtpath += File.separator;
				} catch (Exception e) {}
				try {
					jtname = (files[0].getName() != null ? files[0].getName() : "");
				} catch (Exception e) {}
			}
			else {	// a folder was dropped
				jtpath = (files[0].getPath() != null ? files[0].getPath() : "");
				if (jtpath.endsWith(File.separator) == false) jtpath += File.separator;
			}
			nextJobTicketPath = jtpath;
			jobTicketFileName = jtname;
			nextJTNameFld.setText(jtname);
			processing_disabled = false;
			// set the dialog item states
			setItemStates(0,false);
		}   // end filesDropped
	}); // end FileDrop.Listener


/*
	net.iharder.dnd.FileDrop fd_folderindexnameFld = new net.iharder.dnd.FileDrop( folderindexnameFld, new net.iharder.dnd.FileDrop.Listener() {
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) return;	// file name only
			folderindexnameFld.setText(my_PathName);
			processing_disabled = false;
			// set the dialog item states
			setItemStates(0,false);
		}   // end filesDropped
	}); // end FileDrop.Listener


	net.iharder.dnd.FileDrop fd_folderindexXSLFld = new net.iharder.dnd.FileDrop( folderindexXSLFld, new net.iharder.dnd.FileDrop.Listener() {
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) return;	// file name only
			folderindexXSLFld.setText(my_PathName);
			processing_disabled = false;
			// set the dialog item states
			setItemStates(0,false);
		}   // end filesDropped
	}); // end FileDrop.Listener
*/



	net.iharder.dnd.FileDrop fd_ImageMagickPathNameFld = new net.iharder.dnd.FileDrop( ImageMagickPathNameFld, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			ImageMagickPathNameFld.setText(my_PathName);
			processing_disabled = false;
			if (files[0].isDirectory() == true) return;	// file name only
			if ( (files[0].exists() == true) && (files[0].isFile() == true) ) {
				String canpath = "";
				try {
					canpath = files[0].getCanonicalPath();
				} catch (IOException ex) {}
				im_pgm_path = canpath;
				im_version_str = BatchXSLTImageMagick.get_IMconvert_version();
				im_version_num = BatchXSLTImageMagick.get_IMconvert_version_num(im_version_str);
				if ( im_version_str.equals("") == false ) {
					showMessWait(im_version_str + "\n");
					ImageMagickPathNameFld.setForeground(new java.awt.Color(20, 180, 20));
/* this is done by get_IMconvert_version
					// as we have found ImageMagick operational we may set the envir
					String envir = "PATH=" + files[0].getParent();
					if (envir.endsWith(File.separator) == false) envir += File.separator;
					im_envir = envir;	// ImageMagick must know it too
*/
				}
				else {
					ImageMagickPathNameFld.setForeground(new java.awt.Color(20, 20, 20));
				}
			}
			else ImageMagickPathNameFld.setForeground(new java.awt.Color(20, 20, 20));
			// set the dialog item states
			setItemStates(0,false);
		}   // end filesDropped
	}); // end FileDrop.Listener

	net.iharder.dnd.FileDrop fd_GhostscriptPathNameFld = new net.iharder.dnd.FileDrop( GhostscriptPathNameFld, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {   
		// handle file drop
			String my_PathName = (files[0].getPath() != null ? files[0].getPath() : "");
			if (files[0].isDirectory() == true) return;	// file name only
			GhostscriptPathNameFld.setText(my_PathName);
			if ( (files[0].exists() == true) && (files[0].isFile() == true) ) {
				String canpath = "";
				try {
					canpath = files[0].getCanonicalPath();
				} catch (IOException ex) {}
				gs_pgm_path = canpath;
				gs_version_str = BatchXSLTGhostScript.get_GS_version();
				gs_version_num = BatchXSLTGhostScript.get_GS_version_num(gs_version_str);
				if ( gs_version_str.equals("") == false ) {
					showMessWait(mainXSLTFrame.gs_version_str + "\n");
					GhostscriptPathNameFld.setForeground(new java.awt.Color(20, 180, 20));
/* this is done by get_GS_version
					// as we have found GhostScript operational we may set the envir
					String envir = "PATH=" + files[0].getParent();
					if (envir.endsWith(File.separator) == false) envir += File.separator;
					gs_envir = envir;
					im_envir = envir;	// ImageMagick must know it too
*/
				}
				else {
					GhostscriptPathNameFld.setForeground(new java.awt.Color(20, 20, 20));
				}
			}
			else GhostscriptPathNameFld.setForeground(new java.awt.Color(20, 20, 20));
			processing_disabled = false;
			// set the dialog item states
			setItemStates(0,false);
		}   // end filesDropped
	}); // end FileDrop.Listener


	net.iharder.dnd.FileDrop fd_loadJTBtn = new net.iharder.dnd.FileDrop( loadJTBtn, new net.iharder.dnd.FileDrop.Listener() {
        @Override
		public void  filesDropped( java.io.File[] files ) {
			// handle file drop
			BatchXSLTransform.numberof_readJobTicketFile_calls = 0;

			if (files[0].isDirectory() == true) return;	// file name only

			String jtpath = "";
			String jtname = "";
			try {
				jtpath = (files[0].getParent() != null ? files[0].getParent() : "");
				if (jtpath.endsWith(File.separator) == false) jtpath += File.separator;
			} catch (Exception e) {}
			try {
				jtname = (files[0].getName() != null ? files[0].getName() : "");
			} catch (Exception e) {}
			nextJobTicketPath = jtpath;
			jobTicketFileName = jtname;
/*			try {
				jobTicketFileName = (files[0].getCanonicalPath() != null ? files[0].getCanonicalPath() : "");
			} catch (Exception e) {
				jobTicketFileName = (files[0].getPath() != null ? files[0].getPath() : "");
			}*/
			if (jobTicketFileName.compareTo("") != 0) {
				saveSettings();			// save the current vars settings
				int myErr = 0;
				myErr = BatchXSLTransform.readJobTicketFile(2);
				if (myErr > 0) {
					jobticket_loaded = false;
					mode = 0;	// set manual mode on error
					restoreSettings();		// restore the previous vars settings
				}
			}
			// set the dialog item states
			setItemStates(0,false);
		}   // end filesDropped
	}); // end FileDrop.Listener


	
					

	
	/* --------------------------------------------------------
	 * --------------------------------------------------------
	 * dynamic controls from JobTicket
	 * --------------------------------------------------------
	 * -------------------------------------------------------- */

	int curCtrl = 0;	// the dialog item to work on
	int maxDynControls = 150;	// max custom dialog items
	int numDynControls = 0;		// currently defined num of  custom dialog items
	String[][] dynControlsNames = new String[maxDynControls][2];
	javax.swing.JComboBox[] dynControlsJComboBox = new javax.swing.JComboBox[maxDynControls];
	javax.swing.JLabel[] dynControlsJLabel = new javax.swing.JLabel[maxDynControls];
	javax.swing.JCheckBox[] dynControlsJCheckBox = new javax.swing.JCheckBox[maxDynControls];
	javax.swing.JTextField[] dynControlsJTextField = new javax.swing.JTextField[maxDynControls];


	String get_dialogControlNameByIndex(int idx) {
		if ( (dynControlsNames[idx][0] == null) || dynControlsNames[idx][0].equals("") ) return("");
		else return(dynControlsNames[idx][0]);
	}
	int get_dialogControlIndexByName(String ct_name) {
		if (ct_name == null) return(-1);
		if (ct_name.equals("") == true) return(-1);
		int idx;
		int max = numDynControls;
		for (idx = 0; idx < numDynControls; idx++ ) {
			if (dynControlsNames[idx] == null) return(-1);
			if ( dynControlsNames[idx][0].equals(ct_name) == true ) return(idx);
		}
		return(-1);
	}
	
	int update_dialogControl(String props) {	// given prom the tp_ entries in the JT, props is a string like:
												// # -- Density of image JPEGs in pixels/inch  (for a comment)
												// imageJPEGdpi=150	(for a named custom control and it's value)
												// imageJPEGdpi =150	(or)
												// imageJPEGdpi = 150	(or)
												// imageJPEGdpi= 150
		String[] propsarr = props.split("=");
		if (propsarr.length < 2) return(-1);
		String control_name = propsarr[0].replace(" ","");	// remove all blanks
		String control_valuestr = propsarr[1];	// remove first blank if one
		if (control_valuestr.indexOf(" ") == 0) control_valuestr = control_valuestr.substring(1);	// remove first blank if one
		int ctidx = get_dialogControlIndexByName(control_name);
		if (ctidx < 0) return(-1);
		// we have fount the index of the control to update it's value
		// now we have to scan:
		//		javax.swing.JComboBox[] dynControlsJComboBox
		//		javax.swing.JCheckBox[] dynControlsJCheckBox
		//		javax.swing.JTextField[] dynControlsJTextField
		// for the control belonging to this ctidx
		do {
			if (dynControlsJComboBox[ctidx] != null) {
				try { dynControlsJComboBox[ctidx].setSelectedIndex(Integer.valueOf(control_valuestr)); } catch (NumberFormatException e) { }
				break;
			}
			if (dynControlsJCheckBox[ctidx] != null) {
				try { dynControlsJCheckBox[ctidx].setSelected(control_valuestr.equals("1")); } catch (Exception e) { }
				break;
			}
			if (dynControlsJTextField[ctidx] != null) {
				try { dynControlsJTextField[ctidx].setText(control_valuestr); } catch (Exception e) { }
				break;
			}
		} while (false);

		return(0);
	}

	int make_dialogControl(String what) {
		int retval = 0;
		String[] params = what.split(";");	// this splits ;-separated string into parts ordered like:
		int function = 0;	// 0 = add new control, 1 = add combobox item, -1 = comment
		String type = "";
		String name = "";
		String itemtext = "";
		int fontface = 0;
		int locationX = 0;
		int locationY = 0;
		int sizeW = 0;
		int sizeH = 0;
		String tooltiptext = "";
		
		for (int i = 0; i < params.length; i++) {
			if (i == 0) { function = Integer.valueOf(params[i]); continue; }
			if (i == 1) { type = params[i]; continue; }
			if (i == 2) { name = params[i]; continue; }
			if (i == 3) { itemtext = params[i]; continue; }
			if (i == 4) { fontface = Integer.valueOf(params[i]); continue; }
			if (i == 5) { locationX = Integer.valueOf(params[i]); continue; }
			if (i == 6) { locationY = Integer.valueOf(params[i]); continue; }
			if (i == 7) { sizeW = Integer.valueOf(params[i]); continue; }
			if (i == 8) { sizeH = Integer.valueOf(params[i]); continue; }
			if (i == 9) { tooltiptext = params[i]; continue; }
		}
		// prepare curCtrl to work with
		// first, find an empty dynControlsNames entry to add the declaration
		for (curCtrl = 0; curCtrl < dynControlsNames.length; curCtrl++ ) {
			if ( (dynControlsNames[curCtrl][0] == null) || dynControlsNames[curCtrl][0].equals("") ) break;
		}
		// store name and declaration
		dynControlsNames[curCtrl][0] = name;
		dynControlsNames[curCtrl][1] = what;	// store entire command line

		switch (function) {
			case 0: case -1:	// a new control or a comment: we are already at a new control entry
				break;
			case 1: // add combobox item
				// find existing control to add item (first entry with this name)
				for (curCtrl = 0; curCtrl < dynControlsNames.length; curCtrl++ ) {
					if ( (dynControlsNames[curCtrl][0] == null) || dynControlsNames[curCtrl][0].equals("") ) return(-2);
					if ( dynControlsNames[curCtrl][0].equals(name) == true ) break;
				}
				break;
		}

		if (curCtrl >= dynControlsNames.length) return(-2);
		//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControls: " + what + "\n" );


		if (function < 0) {	// store a comment: nothing more to do
			numDynControls++;
			return(0);
		}

		if (function == 1) {	// add combobox item
			retval = add_control( function, type, name, itemtext);
		}
		else {	// add new control
			retval = add_control( function, type, name, itemtext, fontface, locationX, locationY, sizeW, sizeH, tooltiptext);
		}
		numDynControls++;
		return(retval);
	}


	// adding dialog items to main pane
	
	// to set a combobox item
	int add_control(int function, String type, String name, String itemtext) {
		return ( add_control(function, type, name, itemtext, 0, 0, 0, 0, 0, null) );
	}
	// to create a new dialog control item
	int add_control(int function, String type, String name, String itemtext, int fontface, int locationX, int locationY, int sizeW, int sizeH, String tooltiptext) {

		int baseY = 0;

		if (function == 0) {
			//baseY = (int)messageAreaScrollPane.getLocation().getY() + (int)messageAreaScrollPane.getSize().getHeight();		// right below the messagte area
			int contrBottom = baseY + locationY + sizeH;
			if (userControlsPane.getHeight() < (contrBottom + 1)) {
				userControlsPane.setSize( new java.awt.Dimension(userControlsPane.getWidth(), contrBottom + 2));
				userControlsPaneContainer.setSize( new java.awt.Dimension(userControlsPaneContainer.getWidth(), userControlsPane.getHeight()+22));

				window_current_Dimension.setSize(window_current_Dimension.getWidth(),userControlsPaneContainer.getY() + userControlsPaneContainer.getHeight());
			}
		}

		/* ====================================================== */
		if (type.toLowerCase().equals("combobox") == true) {
			if (function == 0) {	// create a new JComboBox control
				//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControls#"+curCtrl+": " + name + "\n" );

				dynControlsJComboBox[curCtrl] = new javax.swing.JComboBox();
				dynControlsJComboBox[curCtrl].setLocation(new java.awt.Point(locationX, baseY + locationY));
				dynControlsJComboBox[curCtrl].setSize( new java.awt.Dimension(sizeW, sizeH));
				if ((tooltiptext != null) && (tooltiptext.equals("") == false)) dynControlsJComboBox[curCtrl].setToolTipText(tooltiptext);
				dynControlsJComboBox[curCtrl].setVisible(true);
				dynControlsJComboBox[curCtrl].setFont(new java.awt.Font("SansSerif", fontface, 10));
				dynControlsJComboBox[curCtrl].setEnabled(true);

				userControlsPane.add(dynControlsJComboBox[curCtrl]);

				// ad listener
				final int myCtrl = curCtrl;
				dynControlsJComboBox[myCtrl].addItemListener(new java.awt.event.ItemListener() {
                    @Override
					public void itemStateChanged(java.awt.event.ItemEvent e) {
						String keyName = get_dialogControlNameByIndex(myCtrl);
						//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControlsJComboBox: " + keyName + "=" + dynControlsJComboBox[myCtrl].getSelectedIndex() + "\n" );
						int retval = modify_xslt_params(keyName, "" + dynControlsJComboBox[myCtrl].getSelectedIndex());
					}
				});
			}
			else {	// add an item to JComboBox
				dynControlsJComboBox[curCtrl].addItem(itemtext);
			}
			return(0);
		}

		/* ====================================================== */
		if (type.toLowerCase().equals("label") == true) {
			if (function == 0) {	// create a new JLabel
				//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControls#"+curCtrl+": " + name + ", " + itemtext + "\n" );

				dynControlsJLabel[curCtrl] = new javax.swing.JLabel();
				dynControlsJLabel[curCtrl].setLocation(new java.awt.Point(locationX, baseY + locationY));
				dynControlsJLabel[curCtrl].setSize( new java.awt.Dimension(sizeW, sizeH));
				if ((itemtext != null) && (itemtext.equals("") == false)) dynControlsJLabel[curCtrl].setText(itemtext);
				if ((tooltiptext != null) && (tooltiptext.equals("") == false)) dynControlsJLabel[curCtrl].setToolTipText(tooltiptext);
				dynControlsJLabel[curCtrl].setFont(new java.awt.Font("SansSerif", fontface, 10));
				dynControlsJLabel[curCtrl].setVisible(true);

				userControlsPane.add(dynControlsJLabel[curCtrl]);
			}
			return(0);
		}

		/* ====================================================== */
		if ((type.toLowerCase().equals("checkbox") == true) || (type.toLowerCase().equals("checkboxc") == true)) {
			if (function == 0) {	// create a new JCheckBox control
				//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControls#"+curCtrl+": " + name + ", " + itemtext + "\n" );

				dynControlsJCheckBox[curCtrl] = new javax.swing.JCheckBox();
				dynControlsJCheckBox[curCtrl].setLocation(new java.awt.Point(locationX, baseY + locationY));
				dynControlsJCheckBox[curCtrl].setSize( new java.awt.Dimension(sizeW, sizeH));
				if ((itemtext != null) && (itemtext.equals("") == false)) dynControlsJCheckBox[curCtrl].setText(itemtext);
				if ((tooltiptext != null) && (tooltiptext.equals("") == false)) dynControlsJCheckBox[curCtrl].setToolTipText(tooltiptext);
				dynControlsJCheckBox[curCtrl].setVisible(true);
				dynControlsJCheckBox[curCtrl].setFont(new java.awt.Font("SansSerif", fontface, 10));
				dynControlsJCheckBox[curCtrl].setEnabled(true);
				dynControlsJCheckBox[curCtrl].setSelected(type.toLowerCase().equals("checkboxc"));	// checked if ends with 'c'

				userControlsPane.add(dynControlsJCheckBox[curCtrl]);
				// ad listener
				final int myCtrl = curCtrl;
				dynControlsJCheckBox[myCtrl].addItemListener(new java.awt.event.ItemListener() {
                    @Override
					public void itemStateChanged(java.awt.event.ItemEvent e) {
						String keyName = get_dialogControlNameByIndex(myCtrl);
						int selected = 0;
						if (e.getStateChange() == ItemEvent.SELECTED) selected = 1;
						//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControlsJCheckBox: " + keyName + "=" + selected + "\n" );
						int retval = modify_xslt_params(keyName, ""+ selected);
					}
				});
			}
			return(0);
		}

		/* ====================================================== */
		if (type.toLowerCase().equals("textfield") == true) {
			if (function == 0) {	// create a new JLabel
				//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControls#"+curCtrl+": " + name + ", " + itemtext + "\n" );

				dynControlsJTextField[curCtrl] = new javax.swing.JTextField();
				dynControlsJTextField[curCtrl].setLocation(new java.awt.Point(locationX, baseY + locationY));
				dynControlsJTextField[curCtrl].setSize( new java.awt.Dimension(sizeW, sizeH));
				if ((itemtext != null) && (itemtext.equals("") == false)) dynControlsJTextField[curCtrl].setText(itemtext);
				if ((tooltiptext != null) && (tooltiptext.equals("") == false)) dynControlsJTextField[curCtrl].setToolTipText(tooltiptext);
				dynControlsJTextField[curCtrl].setFont(new java.awt.Font("SansSerif", fontface, 10));
				dynControlsJTextField[curCtrl].setVisible(true);

				userControlsPane.add(dynControlsJTextField[curCtrl]);

				// ad listener
				final int myCtrl = curCtrl;
				dynControlsJTextField[myCtrl].getDocument().addDocumentListener(new DocumentListener() {
                    @Override
					public void changedUpdate(DocumentEvent documentEvent) { getNewText(); }
                    @Override
					public void insertUpdate(DocumentEvent documentEvent) { getNewText(); }
                    @Override
					public void removeUpdate(DocumentEvent documentEvent) { getNewText(); }
					public void getNewText() {
						String keyName = get_dialogControlNameByIndex(myCtrl);
						String fieldcontent = dynControlsJTextField[myCtrl].getText();
						//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControlsJTextField: " + keyName + "=" + fieldcontent + "\n" );
						int retval = modify_xslt_params(keyName, fieldcontent);
					}
				});
/*	does not work correctly for all edit cases
				dynControlsJTextField[myCtrl].addKeyListener(new java.awt.event.KeyAdapter() {
					public void keyTyped(java.awt.event.KeyEvent e) {
						String keyName = get_dialogControlNameByIndex(myCtrl);
						char keychar = e.getKeyChar();
						String fieldcontent = dynControlsJTextField[myCtrl].getText();
						if (keychar != KeyEvent.CHAR_UNDEFINED) fieldcontent += keychar;
						//BatchXSLT.g_mainXSLTFrame.showMess( "++++++ dynControlsJTextField: " + keyName + "=" + fieldcontent + "\n" );
						int retval = modify_xslt_params(keyName, fieldcontent);
					}
				});
*/
			}
			return(0);
		}

		return (-1);	// type not found, not set
	}



	int remove_controls() {
		numDynControls = 0;
		boolean removed_controls = false;
		for (int i = 0; i < dynControlsNames.length; i++ ) {
			removed_controls = true;
			if ( (dynControlsNames[i][0] == null) || dynControlsNames[i][0].equals("") ) break;
			dynControlsNames[i][0] = "";
			dynControlsNames[i][1] = "";
			if (dynControlsJComboBox[i] != null) userControlsPane.remove(dynControlsJComboBox[i]);
			if (dynControlsJLabel[i] != null) userControlsPane.remove(dynControlsJLabel[i]);
			if (dynControlsJCheckBox[i] != null) userControlsPane.remove(dynControlsJCheckBox[i]);
			if (dynControlsJTextField[i] != null) userControlsPane.remove(dynControlsJTextField[i]);

		}
		if (removed_controls == true) {
			userControlsPane.setSize( new java.awt.Dimension(userControlsPane.getWidth(), 0));
			userControlsPaneContainer.setSize( new java.awt.Dimension(userControlsPaneContainer.getWidth(), 0));

			window_current_Dimension = new java.awt.Dimension(window_initial_Dimension);	// init to current size without dyn added controls
			setMainWindowSize();
		}
		return(0);
	}


	int modify_xslt_params(String key, String value) {
		if (jobticket_is_loading == true) return(0);
		String params_str = BatchXSLT.g_mainXSLTFrame.xslt_paramsArea.getText();	// get the whole parameters (multiple lines)
		String[] params_arr = params_str.split("\n");
		// find the element
		int idx;
		for (idx = 0; idx < params_arr.length; idx++) {
			if (params_arr[idx].startsWith(key) == true) break;
		}
		if (idx >= params_arr.length) return(-1);
		// replace this element
		params_arr[idx] = key + "=" + value;
		// and again make lines
		xslParams = "";
		for (idx = 0; idx < params_arr.length; idx++) xslParams += params_arr[idx] + '\n';

		xslt_paramsArea.setText(xslParams); xslt_paramsArea.setCaretPosition(0);
		return(0);
	}


	public static PrintStream redir_StdErr = null;
	public static PrintStream redir_StdOut = null;
	public static int redirect_StdOutErr_toLog() {
		try {
			redir_StdErr = new PrintStream(new FileOutputStream(logfile_path + logfile_name, true), true, "UTF-8");	// append
			System.setErr(redir_StdErr);
			redir_StdOut = new PrintStream(new FileOutputStream(logfile_path + logfile_name, true), true, "UTF-8");	// append
			System.setOut(redir_StdOut);
		}
		catch (java.io.FileNotFoundException fnfEx) {
			System.out.println("#### ERROR: Could not redirect System.out and System.err.");
			return(-1);
		}
		catch (UnsupportedEncodingException ex) {
        }
		return(0);
	}
	public static int reset_StdOutErr() {
		if (redir_StdErr != null) {
			System.setErr(System.err);
			redir_StdErr.close();
			redir_StdErr = null;
		}
		if (redir_StdOut != null) {
			System.setOut(System.out);
			redir_StdOut.close();
			redir_StdOut = null;
		}
		return(0);
	}


	public static ByteArrayOutputStream get_frame_screenshot(String type) {
		/*
		// to write any portion of the screen
        Rectangle rec = new Rectangle(global_frame.getLocation().y, global_frame.getLocation().x, global_frame.getSize().width, global_frame.getSize().height);
		ByteArrayOutputStream os = null;
                Toolkit tk = Toolkit.getDefaultToolkit(); //Toolkit class returns the default toolkit
		        Dimension d = tk.getScreenSize();
		        rec = new Rectangle(0, 0, d.width, d.height);
		Robot ro = null;
        try {
            ro = new Robot(); //a very important class to capture the screen image
        } catch (AWTException ex) {
            return(null);
        }
		BufferedImage img = ro.createScreenCapture(rec);
		if (img == null) return(null);
		*/
		// to write a component image
		int imgtype = BufferedImage.TYPE_INT_ARGB;
		if (type.equals("jpeg") == true) imgtype = BufferedImage.TYPE_INT_RGB;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Rectangle rect = global_frame.getBounds();
		BufferedImage img = new BufferedImage(rect.width, rect.height, imgtype);
        global_frame.paint(img.getGraphics());
        try {
            ImageIO.write(img, type, os);
            os.flush();
        } catch (IOException ex) {
            return(null);
        }
		return(os);
    } 
}



	// ============ Messages stuff ========================
	class MessageContainer {
		public boolean valid = false;	// mark as unprocessed
		public String timestamp;		// timestamp when message was sent
		public boolean writelog = false;	// true == write to log file
		public boolean clear = false;		// true == clear message display before displaying mess
		public String message;		// the real message text
		//constructor
		public MessageContainer() {
			this.timestamp = "";		// timestamp when message was sent
			this.message = "";		// the real message text
		}
	}


