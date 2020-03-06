/** 
 * ftp.java
 *
 * Title:			FTP Utilities
 * Description:	
 * @author			Andreas Imhof
 * @version			23.05
 * @versionDate		20150815
 */

package com.epaperarchives.batchxslt;

// Imported java classes
import java.io.*;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.util.TrustManagerUtils;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/********************************
 * various FTP tools
 */
public class ftp
{
    private static final String PACKAGE_NAME = "com.epaperarchives.batchxslt.ftp";
	private static int max_ftpClients = 4;
	private static int initialized_ftpClients = -1;
	private static ftpClient[] ftpClients; // the_ftpClients

	private static ArrayList<String> ftp_sendQueue = new ArrayList<String>();

	/**
	 * CONSTRUCTOR
	 */
	public ftp() {
	}

	private static int    result_code= 0;		// a result code
	private static String result_text= "";		// a result string
	public static boolean DEBUG_FTP_CLIENT = false;
	public static void set_DEBUG_FTP_CLIENT(boolean bol) {
		DEBUG_FTP_CLIENT = bol;
	}


	public static class ftpClient
	{
		public ftpClient() {
		}

		private String type= "ftp";					// or 'ftps'
		private String controlEncoding= "UTF-8";	// default is ASCII
		private String transfermode= "stream";		// default 'stream' transfer
													// 'block' = BLOCK_TRANSFER_MODE = 11
													// 'comp' = COMPRESSED_TRANSFER_MODE = 12
		private int    defaultTimeout= 10000;		// default is 0 == indefinitely, we have to set it
		private int    sendBufferSize= 16384;		// set to -1 to use default
		private int    receiveBufferSize= 16384;		// default is 0 == indefinitely, we have to set it
		private String activePassive= "active";		// active or passive
		private String filetype= "binary";			// default to 'binary' transfer
													// 'ascii' = ASCII_FILE_TYPE = 0
													// 'binary' = BINARY_FILE_TYPE = 2
		private String server= "localhost";			// URL to FTP server
		private int	   port= 0;						// the port to use. 0 = use default port (21)
		private String username= "";				// login user name
		private String password= "";				// login password
		private String account= "";					// login account
		private String base_path= "";				// a base path to store/get files
		private String sub_path= "";				// a subpath below base path to store/get files
		private String remote_path_separator= "/";			// 
		private int    excludeHiddenFiles= 1;		// > 0 = ignore hidden files
		private int    last_result_code= 0;			// a result code
		private String last_result_text= "";		// a result string
		private	final int undefined_value = -9999;	// set to not set


		public FTPClient ftp = null;
		boolean	connected = false;
		boolean	loggedin = false;

		private String remote_curdir = "";				// the curren remote directory


		public int setOption(String which, String value) {
			int option_ok = 0;

			if (which.equals("type")) type = value;
			else if (which.equals("controlEncoding")) controlEncoding = value;
			else if (which.equals("transfermode")) transfermode = value;
			else if (which.equals("defaultTimeout")) defaultTimeout = Integer.valueOf(value);
			else if (which.equals("sendBufferSize")) {
				if (value.equals("") == false) {
					int tmp = Integer.valueOf(value);
					if (tmp > 0) sendBufferSize = tmp;
				}
			}
			else if (which.equals("receiveBufferSize")) {
				if (value.equals("") == false) {
					int tmp = Integer.valueOf(value);
					if (tmp > 0) receiveBufferSize = tmp;
				}
			}
			else if (which.equals("activePassive")) activePassive = value;
			else if (which.equals("filetype")) filetype = value;
			else if (which.equals("server")) server = value;
			else if (which.equals("port")) port = Integer.valueOf(value);
			else if (which.equals("user")) username = value;
			else if (which.equals("username")) username = value;
			else if (which.equals("password")) password = value;
			else if (which.equals("account")) account = value;
			else if (which.equals("base_path")) base_path = value;
			else if (which.equals("sub_path")) sub_path = value;
			else if (which.equals("remote_path_separator")) remote_path_separator = value;
			else if (which.equals("excludeHiddenFiles")) excludeHiddenFiles = Integer.valueOf(value);
			else option_ok = -1;

			return option_ok;
		}

		public void resetOptions() {
			type= "ftp";				// or 'ftps' or 'ftpes'
			controlEncoding= "UTF-8";	// default is ASCII
			transfermode= "stream";		// default 'stream' transfer
													// 'block' = BLOCK_TRANSFER_MODE = 11
													// 'comp' = COMPRESSED_TRANSFER_MODE = 12
			defaultTimeout= 10000;		// default is 0 == indefinitely, we have to set it
			sendBufferSize= 16384;		// set to -1 to use default
			receiveBufferSize= 16384;		// default is 0 == indefinitely, we have to set it
			activePassive= "active";	// active or passive
			filetype= "binary";		// default binary transfer
			server= "localhost";		// URL to FTP server
			port= 0;					// the port to use. 0 = use default port (21)
			username= "";				// login user name
			password= "";				// login password
			account= "";				// login account
			base_path= "";			// a base path to store/get files
			sub_path= "";				// a subpath below base path to store/get files
			last_result_code= 0;		// a result code
			last_result_text= "";		// a result string
		}


		public int getResultCode() {
			return last_result_code;
		}

		public String getResultMessage() {
			return last_result_text;
		}

		public int getDefaultPort() {
			if (ftp == null) {
				last_result_code = -1;
				last_result_text = "FTP client not defined";
				return -1;
			}
			return ftp.getDefaultPort();
		}

		public int connectLogin() {
			int result;

			if (ftp != null) {	// may be we re-connect with connectLogin() on same client
				disconnect();
			}
			result = connect();
			if (result == 0) {
				result = login();
			}
			return result;
		}
		
		public int connect() {
			last_result_code= 0;
			last_result_text= "";

			if (type.equals("ftps") || type.equals("ftpes")) {
				ftp = new FTPSClient();
				//System.out.println("FTPS Client created");
			}
			else {	// normaml unsecure ftp
				ftp = new FTPClient();
				//System.out.println("FTP Client created");
			}

			if (sendBufferSize >= 0) {
				try {
					ftp.setSendBufferSize(sendBufferSize);
				} catch (SocketException ex) {
					last_result_code = -1;
					last_result_text = ex.getMessage();
				}
			}
			if (receiveBufferSize >= 0) {
				try {
					ftp.setReceiveBufferSize(receiveBufferSize);
				} catch (SocketException ex) {
					last_result_code = -2;
					last_result_text = ex.getMessage();
				}
			}

			if (filetype.equals("ascii") == false) {	// default is filetype = FTP.ASCII_FILE_TYPE;
				int ft = FTP.BINARY_FILE_TYPE;
				if (filetype.equals("binary")) ft = FTP.BINARY_FILE_TYPE;
				//System.out.println("Setting file type to: " + ft);
				try {
					ftp.setFileType(ft);
				} catch (IOException ex) {
					last_result_code = -3;
					last_result_text = ex.getMessage();
				}
			}

			if (transfermode.equals("stream") == false) {	// default is STREAM_TRANSFER_MODE
				int tm = FTP.STREAM_TRANSFER_MODE;
				if (transfermode.equals("block")) tm = FTP.BLOCK_TRANSFER_MODE;
				else if (transfermode.equals("block")) tm = FTP.COMPRESSED_TRANSFER_MODE;
				//System.out.println("Setting transfer mode to: " + tm);
				try {
					ftp.setFileTransferMode(tm);
				} catch (IOException ex) {
					last_result_code = -4;
					last_result_text = ex.getMessage();
				}
			}

			//	ftp.setControlKeepAliveTimeout(1000);

			// set control encoding for internal stream (must be BEFORE connect)
			if ((controlEncoding != null) && (controlEncoding.equals("") == false)) {
				ftp.setControlEncoding(controlEncoding);
			}

			//System.out.println("Connecting to: " + server + " on port " + port);
			try {
				if (port > 0) {
					ftp.connect(server, port);
				} else {
					ftp.connect(server);
				}
			} catch (IOException ex) {
				last_result_code = -5;
				last_result_text = ex.getMessage();
				//System.out.println("#### EXCEPTION: " + last_result_text);
				return -5;
			}
		 	if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
		 		last_result_code= -6;
				last_result_text = "### ERROR Could not connect to: " + server + " port: " + port + ". Reply: " + ftp.getReplyString();
				return -6;
			}


			int actpass_set;
			if (activePassive.equals("active")) actpass_set = setActive();
			else actpass_set = setPassive();
			if (actpass_set != 0) {
				if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR connect(): Could not set '" + activePassive + "' mode.\n");
			}

			
			// negotiate control encoding with remote server (must be AFTER connect)
			if ((controlEncoding != null) && (controlEncoding.equals("") == false)) {
				if (controlEncoding.equals("UTF-8")) command("OPTS", "UTF8 ON");
				else command("OPTS", "UTF8 OFF");
				BatchXSLT.g_mainXSLTFrame.showMess(last_result_text + "\n");
			}

			loggedin = true;
			return 0;
		}

		public int disconnect() {
			last_result_code= 0;
			last_result_text= "";
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return 0;
			}
			try {
				ftp.disconnect();
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			ftp = null;
			connected = false;
			return 0;
		}

		public int login() {
			last_result_code= 0;
			last_result_text= "";
			boolean loggedin = false;
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			try {
				if (account.equals("") == false) loggedin = ftp.login(username, password, account);
				else loggedin = ftp.login(username, password);
			} catch (IOException ex) {
				last_result_code = -2;
				last_result_text = ex.getMessage();
				return -3;
			}
			if (!loggedin) {
				last_result_code= -3;
				last_result_text= "ERROR -3 Could not login with username: '" + username + "' and password: '" + password + "'";
				return -3;
			}
			else {
				last_result_code= 0;
				last_result_text= "Successfully logged in.";
			}
			loggedin = true;
			return 0;
		}

		public int logout() {
			last_result_code= 0;
			last_result_text= "";
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			try {
				ftp.logout();
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			loggedin = false;
			return 0;
		}

		public String[] doCommand(String command) {
			return doCommand(command, null);
		}
		//Should only be used with commands that return replies on the command channel - do not use for LIST, NLST, MLSD etc. 
		public String[] doCommand(String command, String params) {
			String[] replies = null;
			last_result_code= 0;
			last_result_text= "";

			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return null;
			}
			try {
				replies = ftp.doCommandAsStrings(command, params);
				if (replies == null) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return replies;
		}

		public FTPFile[] listDir(String path) {
			FTPFile[] replies = null;
			last_result_code= 0;
			last_result_text= "";

			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return null;
			}
			try {
				replies = ftp.mlistDir(path);
				if (replies == null) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return replies;
		}

		public FTPFile listFile(String pathname) {
			FTPFile replies = null;
			last_result_code= 0;
			last_result_text= "";

			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return null;
			}
			try {
				replies = ftp.mlistFile(pathname);
				if (replies == null) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return replies;
		}

		public boolean mkdir(String dirname) {
			boolean success = false;
			last_result_code= 0;
			last_result_text= "";

			if (dirname.equals("") == true) {
				last_result_code= -2;
				last_result_text= "No directory name given";
				return false;
			}
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
			try {
				success = ftp.makeDirectory(dirname);
				if (!success) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return success;
		}

		public boolean mkdirPath(String path) {
			String[] path_parts;
			int i;
			boolean success;
			last_result_code= 0;
			last_result_text= "";

			if (path.equals("") == true) {
				last_result_code= -2;
				last_result_text= "No path given";
				return false;
			}
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}

			//System.out.println("======= mkdirPath path: " + path);
			path_parts = path.split("/");
			for (i = 0; i < path_parts.length; i++) {
				if (path_parts[i].equals("")) continue;
				//System.out.println("======= mkdirPath path_parts[" + i + "]: " + path_parts[i]);
				success = mkdir(path_parts[i]);
				if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("======= mkdirPath success: " +  success + " last_result_code: " + last_result_code + " : " + last_result_text + "\n");
				if (!success && (last_result_code != 550)) {	// might be: 550 Can't create directory: File exists
					return false;
				}
				success = cd(path_parts[i]);
				if (!success) {
					return false;
				}
		
			}

			return true;
		}


		public boolean rmdir(String pathname) {
			boolean success = false;
			last_result_code= 0;
			last_result_text= "";

			if (pathname.equals("") == true) {
				last_result_code= -2;
				last_result_text= "No path name name given";
				return false;
			}
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
					 
			try {
				success = ftp.removeDirectory(pathname);
				if (!success) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return success;
		}

		public boolean cd(String pathname) {
			boolean success = false;
			last_result_code= 0;
			last_result_text= "";

			if (pathname.equals("") == true) {
				last_result_code= -2;
				last_result_text= "No path name given";
				return false;
			}
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
			try {
				success = ftp.changeWorkingDirectory(pathname);
				if (!success) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return success;
		}

		public String pwd() {
			String currwd = null;
			last_result_code= 0;
			last_result_text= "";

			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return null;
			}
			try {
				currwd = ftp.printWorkingDirectory();
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return currwd;
		}

		public boolean storeFile(String local_pathname, String remote_filename) {
			return storeFile(local_pathname, remote_filename, FTP.BINARY_FILE_TYPE);
		}
		public boolean storeFile(String local_pathname, String remote_filename, int fileType) {
			boolean success = false;
			InputStream is = null;
			last_result_code= 0;
			last_result_text= "";

			if (remote_filename.equals("") == true) {
				last_result_code= -2;
				last_result_text= "No file name given";
				return false;
			}
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
				
			try {
				if (fileType != undefined_value) ftp.setFileType(fileType);
				is = new FileInputStream(local_pathname);	// opens with read;
				success = ftp.storeFile(remote_filename, is);
				try {
					if (is != null) {
						is.close();
						is = null;
					}
				} catch (IOException exis) {}
				if (!success) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			finally {
				try {
					if (is != null) {
						is.close();
						is = null;
					}
				} catch (IOException ex1) {}
			}

			return success;
		}

		public boolean getFile(String remote_filename, String local_pathname) {
			return getFile(remote_filename, local_pathname, FTP.BINARY_FILE_TYPE);
		}
		public boolean getFile(String remote_filename, String local_pathname, int fileType) {
			boolean success = false;
			OutputStream os = null;
			last_result_code= 0;
			last_result_text= "";

			if (remote_filename.equals("") == true) {
				last_result_code= -2;
				last_result_text= "No file name given";
				return false;
			}
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
					 
			try {
				ftp.setFileType(fileType);
				os = new FileOutputStream(local_pathname);
				success = ftp.retrieveFile(remote_filename, os);
				try {
					if (os != null) {
						os.flush();
						os.close();
						os = null;
					}
				} catch (IOException exos) {}
				if (!success) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			finally {
				try {
					if (os != null) {
						os.close();
						os = null;
					}
				} catch (IOException ex1) {}
			}
			return success;
		}

		public boolean renameFile(String from, String to) {
			boolean success = false;
			last_result_code= 0;
			last_result_text= "";

			if (from.equals("") == true) {
				last_result_code= -2;
				last_result_text= "No from name name given";
				return false;
			}
			if (to.equals("") == true) {
				last_result_code= -3;
				last_result_text= "No to name name given";
				return false;
			}
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
					 
			try {
				success = ftp.rename(from, to);
				if (!success) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return success;
		}

		public boolean existsFile(String remote_name) {
			FTPFile[] result;
			last_result_code= 0;
			last_result_text= "";

			if (remote_name.equals("") == true) {
				last_result_code= -2;
				last_result_text= "No file or directory name given";
				return false;
			}
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
					 
			try {
				result = ftp.listFiles(remote_name);
				if ((result == null) || (result.length == 0)) {
					return false;
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return true;
		}

		public boolean noop() {
			boolean success = false;
			last_result_code= 0;
			last_result_text= "";

			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
					 
			try {
				success = ftp.sendNoOp();
				if (!success) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return success;
		}

		public String systemType() {
			String result;
			result = null;
			last_result_code= 0;
			last_result_text= "";

			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return null;
			}
					 
			try {
				result = ftp.getSystemType();
				if (result == null) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
				}
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return result;
		}

		public boolean getFeature(String feature) {
			boolean result = false;
			last_result_code= 0;
			last_result_text= "";

			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
					 
			try {
				result = ftp.hasFeature(feature);
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return result;
		}
		public boolean getFeature(String feature, String value) {
			boolean result = false;
			last_result_code= 0;
			last_result_text= "";

			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return false;
			}
					 
			try {
				result = ftp.hasFeature(feature, value);
			} catch (IOException ex) {
				last_result_code = -1;
				last_result_text = ex.getMessage();
			}
			return result;
		}


		/* This both getters are protected in Socket
		public int getSendBufferSize() {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			 ftp.getSendBufferSize();
			return 0;
		}
		public int getReceiveBufferSize() {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			return ftp.getReceiveBufferSize();
		}
		*/
		public int setSendBufferSize(int s) {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
            try {
                ftp.setSendBufferSize(s);
            } catch (SocketException ex) {
                last_result_code = -1;
				last_result_text = ex.getMessage();
                return -1;
            }
            return 0;
		}
		public int setReceiveBufferSize(int s) {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
            try {
				ftp.setReceiveBufferSize(s);
            } catch (SocketException ex) {
                last_result_code = -1;
				last_result_text = ex.getMessage();
				return -1;
            }
            return 0;
		}

		public int setPassive() {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			ftp.enterLocalPassiveMode();
		 	if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
		 		last_result_code= -2;
				last_result_text = "### ERROR Could not enter into passive mode: " + ftp.getReplyString();
				return -2;
			}
			return 0;
		}
		public int setActive() {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			ftp.enterLocalActiveMode();
		 	if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
		 		last_result_code= -2;
				last_result_text = "### ERROR Could not enter into active mode: " + ftp.getReplyString();
				return -2;
			}
			return 0;
		}

		public int controlEncoding(String encoding) {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			ftp.setControlEncoding(encoding);
		 	if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
		 		last_result_code= -2;
				last_result_text = "### ERROR Could not control encoding to: " + encoding + ". Reply: " + ftp.getReplyString();
				return -2;
			}
			return 0;
		}
		public int command(String cmd, String params) {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			if ((params == null) || params.equals("")) {
				try {
					ftp.sendCommand(cmd);
				} catch (IOException ex) {
					last_result_code= -2;
					last_result_text= "### ERROR sending command; " + cmd;
					return -2;
				}
			}
			else {
				try {
					ftp.sendCommand(cmd, params);
				} catch (IOException ex) {
					last_result_code= -2;
					last_result_text= "### ERROR sending command; " + cmd + " " + params;
					return -2;
				}
			}
			last_result_code= ftp.getReplyCode();
			last_result_text = ftp.getReplyString();
			return 0;
		}

		/**
		 * @param timeout The default timeout in milliseconds that is used when opening a data connection socket. The value 0 means an infinite timeout.
		 *				  Sets the timeout in milliseconds to use when reading from the data connection. 
		 *				  This timeout will be set immediately after opening the data connection, provided that the value is ≥ 0. 
		 * @return 0 on success, otherwise error -1
		 */
		public int setDataTimeout(int timeout) {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
			ftp.setDataTimeout(timeout);
            return 0;
		}
		public int getSocketTimeout() {
			if (ftp == null) {
				last_result_code= -1;
				last_result_text= "No FTP client available";
				return -1;
			}
            try {
				ftp.getSoTimeout();
            } catch (SocketException ex) {
                last_result_code = -1;
				last_result_text = ex.getMessage();
               return -1;
            }
            return 0;
		}



		// ===========================================================
		/**
		 * The FTP putFile method as thread
		 */
		boolean putFileThread_isrunning = false;
		int putFileThread_returnvalue = -1;
		class putFileThread extends Thread {
			String localFileName;
			String remoteFileName;
			int fileType;
			int destroyWhenDoneID;
			putFileThread(final String localFileName, final String remoteFileName, final int fileType, final int destroyWhenDoneID) {
				this.localFileName = localFileName;
				this.remoteFileName = remoteFileName;
				this.fileType = fileType;
				this.destroyWhenDoneID = destroyWhenDoneID;
				//java.lang.System.out.println("****** putFileThread init ");
			}

			@Override
			public void run() {
				long startticks = Calendar.getInstance().getTimeInMillis();		// get current ticks
				long endticks;
				long transfernms;
				boolean	success_transfer;
				File f = new File(this.localFileName);
				long f_length = f.length();
				Thread.currentThread().setName("putFileThread");
				//java.lang.System.out.println("****** putFileThread running ");
				putFileThread_isrunning = true;
				//java.lang.System.out.println("****** putFileThread STORing: " + this.localFileName);
				success_transfer = storeFile(this.localFileName, this.remoteFileName, this.fileType);

				putFileThread_returnvalue = (success_transfer == true ? 0 : -1);
				putFileThread_isrunning = false;
				//java.lang.System.out.println("****** putFileThread complete: " + this.localFileName);

				endticks = Calendar.getInstance().getTimeInMillis();		// get current ticks
				transfernms = endticks - startticks;

				if (success_transfer) {
					last_result_code = ftp.getReplyCode();
					last_result_text = ftp.getReplyString();
					BatchXSLT.g_mainXSLTFrame.showMess("*** putFileThread file successfully sent: '" + this.localFileName + "' to remote: '" + this.remoteFileName + "'\n");
				}
				else {
					BatchXSLT.g_mainXSLTFrame.showMess("### ERROR putFileThread: " + last_result_text +"\n");
				}
				
				if (this.destroyWhenDoneID >= 0) {
					if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** FTP transfer id: " + this.destroyWhenDoneID + " complete. Success: " + success_transfer + ", File: " + this.localFileName + ", Size: " + f_length + " bytes in " + transfernms + " ms. sendBufferSize: " + sendBufferSize + " bytes\n");
					disconnectDestroyFTPClient(this.destroyWhenDoneID);
				}
			}
		 }	// end class putFileThread


		/**
		 * start the FTP putFile method as a thread
		 * @param localPathName path and name to local source file
		 * @param remoteFileName the name of the remote file to store
		 * @param waitCompleted if <code>false</code>, a file will be put without waiting for completion
		 * @return 0 on success, -99 on aborted, otherwise error code
		 */
		public int putFileAsThread(String localPathName, String remoteFileName, boolean waitCompleted) {
			return putFileAsThread(localPathName, remoteFileName, FTP.BINARY_FILE_TYPE, waitCompleted, -1);
		}
		/**
		 * start the FTP putFile method as a thread
		 * @param localPathName path and name to local source file
		 * @param remoteFileName the name of the remote file to store
		 * @param waitCompleted if <code>false</code>, a file will be put without waiting for completion
		 * @param destroyWhenDoneID if client id is given, the FTPClient will be destroyed on transfer completion
		 * @return 0 on success, -99 on aborted, otherwise error code
		 */
		public int putFileAsThread(String localPathName, String remoteFileName, int fileType, boolean waitCompleted, int destroyWhenDoneID) {
			//long max_sleep = copyFile_ftp_prot.getTimeout();
			long max_sleep = 8400000;	// 140 minutes max should be enough for a 500 MB file at transfer rate of 65 KB/sec
										// 7 min for 25MB at 65 KB/sec
			long total_sleep = 0;
			long sleep_interval = 300;	// first time wait longer

			//java.lang.System.out.println("****** start tread ");
			Thread pft = new putFileThread(localPathName, remoteFileName, fileType, destroyWhenDoneID);
			pft.start();

			if (waitCompleted) {
				// and now wait until file is transferred
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
						java.lang.System.out.println("      for local file: '" + localPathName + "'\n" );
						java.lang.System.out.println("      to remote file: '" + remoteFileName + "'\n" );
						break;
					}
				}
				//java.lang.System.out.println( "  FTP Tranfer complete, file: '" + localPathName + "'\n" );
			}
			else {
				//java.lang.System.out.println( "  FTP Tranfer initiated, file: '" + localPathName + "'\n" );
				return 0;
			}
			return (putFileThread_returnvalue);
		}



		/**
		 * FTP PUT local folder to remote location.
		 * @param srcpath source folder path
		 * @param destpath target folder path or ftp url
		 * @param recursive if <code>true</code>, processes folder recursively
		 * @param excludeHiddenFiles if <code>true</code>, to ignore hidden files
		 * @param deletesource if <code>true</code>, source file is deleted after being copied
		 * @param waitcompleted currently unused. if <code>false</code>, a file will be put without waiting for completion
		 * @return 0 on success, -99 on aborted, otherwise error code
		 */
		public int putFolder(final String srcpath, final String destpath, final boolean recursive, final boolean excludeHiddenFiles, final boolean deletesource, final boolean waitcompleted) {
			boolean f_deleted = false;
			int i;
			String path;
			String pathname;
			String my_destpath = destpath;
			String my_destfilename;
			boolean directory_created;
			int totalfiles = 0;
			File src = new File(srcpath);
			boolean stored;

			int max_reconnects = 5;
			boolean try_again = true;
			int result;


			if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder srcpath: '" + srcpath + "'\n");
			if (!src.exists()) {
				result_code= -1;
				result_text= "Source path does not exist";
				return result_code;
			}

			if (src.isDirectory()) {
				File[] srcFiles = src.listFiles();
				if (my_destpath.endsWith(remote_path_separator) == false) my_destpath += remote_path_separator;
				my_destpath += src.getName();
				for (i = 0; i < srcFiles.length; i++) {
					if (mainXSLTFrame.general_abort == true) return -99;
					File file = srcFiles[i];
					if (excludeHiddenFiles && (file.getName().startsWith(".") == true)) continue;
					path = file.getParent(); if (path.endsWith(File.separator) == false) path += File.separator;
					pathname = path + file.getName();
					if (file.isDirectory()) {
						if (recursive) {
							putFolder(pathname, my_destpath + remote_path_separator, recursive, excludeHiddenFiles, deletesource, waitcompleted);
							File[] flist = file.listFiles();
							if (flist.length <= 0) {
								//System.out.println("empty folder: '" + pathname + "'");
								if (deletesource == true) f_deleted = BatchXSLTransform.file_delete(pathname);	// delete this empty dir
								continue;
							}
						}
						continue;
					}
					else if (file.isFile()) {
						// is a file
				
						try_again = true;
						while (try_again) {	 
							try_again = false;

							// create remote path if we are not already there
							if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir requested: '" + my_destpath + "', is: '" + remote_curdir + "'\n");
							if (remote_curdir.equals("")) {
								remote_curdir = pwd();	// get remote directory
								if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir retrieved: '" + remote_curdir + "'\n");
							}
							else {
								if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir is: '" + remote_curdir + "'\n");
							}
							if (remote_curdir.equals(my_destpath) == false) {
								cd("/");
								directory_created = mkdirPath(my_destpath);	// also this will be set as current directory
								if (directory_created) {
									remote_curdir = my_destpath;	// store remote directory
									if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir created: '" + remote_curdir + "\n");
								}
								else {
									BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR putFolder while creating remote path: " + my_destpath + "\n");
								}
							}
							else directory_created = true;

							if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir: '" + remote_curdir + "'(" + remote_curdir.length() + "), my_destpath: '" + my_destpath + "(" + my_destpath.length() +")'\n");
							if (directory_created) {
								my_destfilename = file.getName();
								// transfer file
								BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder src: " + pathname + " to: " + my_destpath + " as '" + my_destfilename + "'\n");
								stored = storeFile(pathname, my_destfilename);
								if (!stored) {
									BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR putFolder: " + last_result_code + " : " + last_result_text + "\n");
									BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR putFolder source file: " + pathname + ", target path: " + my_destpath + " as file '" + my_destfilename + "'\n");
									remote_curdir = "";
									if (--max_reconnects > 0) {
										BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR - reconnecting...\n");
										result = connectLogin();
										if (result == 0) {
											try_again = true;
											continue;
										}
										BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR - could not re-connect. code: " + result +"\n");
									}
									last_result_code = ftp.getReplyCode();
									last_result_text = ftp.getReplyString();
								}
								totalfiles++;
								if (deletesource == true) f_deleted = BatchXSLTransform.file_delete(pathname);	// delete the source file
							}
						}
					}
				}
			}
			else {	// is a file
				if (excludeHiddenFiles && (src.getName().startsWith(".") == true)) {
					// f_deleted = BatchXSLTransform.file_delete(pathname);	// delete it
					return 0;
				}
				path = src.getParent(); if (path.endsWith(File.separator) == false) path += File.separator;

				// create remote path if we are not already there
				if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir requested: '" + my_destpath + "', is: '" + remote_curdir + "'\n");
				if (remote_curdir.equals("")) {
					remote_curdir = pwd();	// get remote directory
					if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir retrieved: '" + remote_curdir + "'\n");
				}
				else {
					if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir is: '" + remote_curdir + "'\n");
				}
				if (remote_curdir.equals(my_destpath) == false) {
					cd("/");
					directory_created = mkdirPath(my_destpath);	// also this will be set as current directory
					if (directory_created) {
						remote_curdir = my_destpath;	// store remote directory
						if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir created: '" + remote_curdir + "'\n");
					}
					else {
						BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR putFolder while creating remote path: " + my_destpath + "\n");
					}
				}
				else directory_created = true;

				if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder remote_curdir: '" + remote_curdir + "'(" + remote_curdir.length() + "), my_destpath: '" + my_destpath + "(" + my_destpath.length() +")'\n");
				if (directory_created) {
					pathname = path + src.getName();
					my_destfilename = src.getName();
					if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder src: " + pathname + ", my_destpath: " + my_destpath + " as '" + my_destfilename + "'\n");
					stored = storeFile(pathname, my_destfilename);
					if (!stored) {
						last_result_code = ftp.getReplyCode();
						last_result_text = ftp.getReplyString();
						BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR putFolder: " + last_result_code + " : " + last_result_text + "\n");
						BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR putFolder source file: " + pathname + ", target path: " + my_destpath + " as file '" + my_destfilename + "'\n");
					}
					totalfiles++;
					if (deletesource == true) f_deleted = BatchXSLTransform.file_delete(pathname);	// delete the source file
				}
			}
			BatchXSLT.g_mainXSLTFrame.showMess("*** putFolder totalfiles: " + totalfiles + "\n");
			return 0;
		}


	}	// end class ftpClient


    public static int init_ftpClients () {
    	return init_ftpClients (max_ftpClients);
    }
    public static int init_ftpClients (int num) {
        int i;
		ftpClients = new ftpClient[num]; // allocates memory for max_ftpClients
        for (i = 0; i < num; i++) ftpClients[i] = null;
        max_ftpClients = num;
        initialized_ftpClients = num;
		BatchXSLT.g_mainXSLTFrame.showMess("*** initialized FTPClients: " + ftpClients.length + "\n");
        return num;
    }

	public static int activeFTPClients() {
        int i;
        int numclients = 0;
   		if (initialized_ftpClients < 0) init_ftpClients (max_ftpClients);
        for (i = 0; i < max_ftpClients; i++) {
            if (ftpClients[i] != null) numclients++;
        }
        return numclients;	// num active FTP clients
	}
	public static int set_max_ftpClients(int max) {
		if (activeFTPClients() > 0) return -1;
		init_ftpClients (max);
		return ftpClients.length;
	}
	public static int maxFTPClients() {
        return max_ftpClients;	// max FTP clients possible
	}

	public static ftpClient newFTPClient() {
        int i;
   		if (initialized_ftpClients < 0) init_ftpClients (max_ftpClients);
        for (i = 0; i < max_ftpClients; i++) {
            if (ftpClients[i] == null) {
            	ftpClients[i] = new ftpClient();
            	
            	if (ftpClients[i] != null) {
            		if (ftpClients[i].defaultTimeout > 0) {
           				ftpClients[i].setDataTimeout(ftpClients[i].defaultTimeout);
            		}
            	}

				return ftpClients[i];
            }
        }
        return null;
	}
	public static int destroyFTPClient(ftpClient client) {
        int i;
        for (i = 0; i < max_ftpClients; i++) {
            if (ftpClients[i] == client) {
            	ftpClients[i] = null;
            	return 0;
            }
        }
        return -1;	// not found
	}

	public static void disconnectDestroyFTPClient(int clientID) {
     	if (ftpClients[clientID] == null) return;
		if (true == ftpClients[clientID].loggedin) ftpClients[clientID].logout();
		if (true == ftpClients[clientID].connected) ftpClients[clientID].disconnect();
     	ftpClients[clientID] = null;
		BatchXSLT.g_mainXSLTFrame.showMess("*** FTPClient " + clientID + " disconnected and released.\n");
	}

	public static int getFTPClientID(ftpClient client) {
        int i;
        for (i = 0; i < max_ftpClients; i++) {
            if (ftpClients[i] == client) return i;
        }
        return -1;	// not found
	}


	// ===========================================================
	/**
	 * work the ftp_sendQueue
	 */
	public static boolean DEBUG_FTP_QUEUE = false;
	public static void set_DEBUG_FTP_QUEUE(boolean bol) {
		DEBUG_FTP_QUEUE = bol;
	}
	private static boolean ftp_sendQueueThread_isrunning = false;
	private static int ftp_sendQueueThread_returnvalue = -1;
	private static class ftp_sendQueueThread extends Thread {
		ftp_sendQueueThread() {
			//java.lang.System.out.println("****** ftp_sendQueueThread init ");
		}

		@Override
		public void run() {
			boolean go_on = true;
			String xml_queueElement;
			boolean	success_transfer;
			boolean hasEntries = false;
			DocumentBuilder db;
			InputSource is;
			Document doc;
			NodeList nodes;
			Element elem;

			String type= "ftp";					// or 'ftps'
			String controlEncoding= "UTF-8";	// default is ASCII
			String transfermode= "stream";		// default 'stream' transfer
												// 'block' = BLOCK_TRANSFER_MODE = 11
												// 'comp' = COMPRESSED_TRANSFER_MODE = 12
			String defaultTimeout= "10000";		// default is 0 == indefinitely, we have to set it
			String sendBufferSize= "16384";		// set to -1 to use default
			String receiveBufferSize= "16384";	// default is 0 == indefinitely, we have to set it
			String activePassive= "active";		// active or passive
			String filetype= "binary";			// default to 'binary' transfer
												// 'ascii' = ASCII_FILE_TYPE = 0
												// 'binary' = BINARY_FILE_TYPE = 2
			String server= "localhost";			// URL to FTP server
			String port= "0";					// the port to use. 0 = use default port (21)
			String username= "";				// login user name
			String password= "";				// login password
			String account= "";					// login account
			String base_path= "";				// a base path to store/get files
			String sub_path= "";				// a subpath below base path to store/get files
			
			String src_pathname = "";
			String remote_path = "";
			String remote_filename = "";
			
			String remote_curdir = "";
			boolean directory_created;

			
			ftpClient ftpc = null;
			int ftpc_id = -1;
			File f;

			Thread.currentThread().setName("ftp_sendQueueThread");
			if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("****** ftp_sendQueueThread running\n");
			ftp_sendQueueThread_isrunning = true;

			while (go_on) {
				if (ftp_sendQueue.size() > 0) {
					xml_queueElement = ftp_sendQueue.get(0);	// is XML string like:
																// <ftp>
																// 		<type>ftps</type>
																// 		<controlEncoding>UTF-8</controlEncoding>
																// 		<transfermode>stream</transfermode>
																// 		<filetype>binary</filetype>
																// 		<activePassive>passive</activePassive>
																// 		<server>localhost</server>
																// 		<port>21</port>
																//
																// 		<src_pathname>the/local/file/name</src_pathname>
																// 		<remote_path>target/path</remote_path>
																// 		<remote_filename>afilename</remote_filename>
																//
																// 		<base_path></base_path>
																// 		<sub_path></sub_path>
																//
																// 		<username></username>
																// 		<password></password>
																// 		<account></account>
																// <ftp>
					if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread xml_queueElement: " + xml_queueElement + "\n");
					try {
						db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					} catch (ParserConfigurationException ex) {
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread; " + ex.getMessage() + "\n");
						ftp_sendQueue.remove(0);
						continue;
					}
					is = new InputSource();
					is.setCharacterStream(new StringReader(xml_queueElement));

					try {
						doc = db.parse(is);
					} catch (SAXException ex) {
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: " + ex.getMessage() + "\n");
						ftp_sendQueue.remove(0);
						continue;
					} catch (IOException ex) {
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: " + ex.getMessage() + "\n");
						ftp_sendQueue.remove(0);
						continue;
                    }

					nodes = doc.getElementsByTagName("type");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) type = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("controlEncoding");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) controlEncoding = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("transfermode");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) transfermode = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("defaultTimeout");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) defaultTimeout = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("filetype");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) filetype = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("activePassive");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) activePassive = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("server");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) server = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("port");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) port = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("src_pathname");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) src_pathname = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("remote_path");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) remote_path = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("remote_filename");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) remote_filename = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("base_path");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) base_path = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("sub_path");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) sub_path = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("username");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) username = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("password");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) password = myGetNodeValue(elem);
					}

					nodes = doc.getElementsByTagName("account");
					if (nodes != null) {
						elem = (Element) nodes.item(0);
						if (elem != null) account = myGetNodeValue(elem);
					}

					// check entries
					if ((src_pathname == null)
						|| (server == null)
						|| (remote_path == null)
						|| (remote_filename == null)
						) {
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: invali params\n");
						ftp_sendQueue.remove(0);
						continue;
					}
					f = new File(src_pathname);
					if (f.exists() == false) {
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: file not exists: " + src_pathname + "\n");
						ftp_sendQueue.remove(0);
						continue;
					}
					
					if (ftpc == null) {
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread: INIT connection.\n");
						ftpc = newFTPClient();
						if (ftpc == null) {	// no client available
							if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: No ftpClient available. waiting...\n");
							break;	// wait a bit
						}
						ftpc_id = getFTPClientID(ftpc);
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread Connection id: " + ftpc_id + ".\n");
						// set options
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread setting options.\n");
						if (type != null) ftpc.setOption("type",type);
						if (controlEncoding != null) ftpc.setOption("controlEncoding",controlEncoding);
						if (transfermode != null) ftpc.setOption("transfermode",transfermode);
						if (defaultTimeout != null) ftpc.setOption("defaultTimeout",defaultTimeout);
						if (activePassive != null) ftpc.setOption("activePassive",activePassive);
						if (filetype != null) ftpc.setOption("filetype",filetype);
						if (server != null) ftpc.setOption("server",server);
						if (port != null) ftpc.setOption("port",port);
						if (username != null) ftpc.setOption("user",username);
						if (password != null) ftpc.setOption("password",password);
						if (account != null) ftpc.setOption("account",account);
						if (base_path != null) ftpc.setOption("base_path",base_path);
						if (sub_path != null) ftpc.setOption("sub_path",sub_path);

						// set control encoding for internal stream (must be BEFORE connect)
						if ((controlEncoding != null) && (controlEncoding.equals("") == false)) {
							ftpc.controlEncoding(controlEncoding);
						}
						
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread connecting to server: " + server + "\n");
						int connected = ftpc.connect();
						if (connected != 0) {
							if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: Could not connect to FTP server\n");
							ftp_sendQueue.remove(0);
							continue;
						}
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread connected.\n");

						// set active or passive
						if (activePassive != null) {
							int actpass_set;
							if (activePassive.equals("active")) actpass_set = ftpc.setActive();
							else actpass_set = ftpc.setPassive();
							if (actpass_set != 0) {
								if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: Could not set '" + activePassive + "' mode.\n");
							}
						}
			
						// negotiate control encoding with remote server (must be AFTER connect)
						if ((controlEncoding != null) && (controlEncoding.equals("") == false)) {
							if (controlEncoding.equals("UTF-8")) ftpc.command("OPTS", "UTF8 ON");
							else ftpc.command("OPTS", "UTF8 OFF");
							BatchXSLT.g_mainXSLTFrame.showMess(ftpc.last_result_text + "\n");
						}

						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread logging in as user: " + username + "\n");
						int loggedin = ftpc.login();
						if (loggedin != 0) {
							if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: Could not login to FTP server\n");
							ftp_sendQueue.remove(0);
							continue;
						}
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread logged in.\n");
					}

					if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread QUEUEING: " + src_pathname + ", remote_path" + remote_path + "\n");

					success_transfer = true;

					// create remote path if we are not already there
					if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread remote_curdir requested: '" + remote_path + "', is: '" + remote_curdir + "'\n");
					if (remote_curdir.equals("")) {
						remote_curdir = ftpc.pwd();	// get remote directory
						if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread remote remote_curdir retrieved: '" + remote_curdir + "'\n");
					}
					else {
						if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread remote remote_curdir is: '" + remote_curdir + "'\n");
					}
					if (remote_curdir.equals(remote_path) == false) {
						ftpc.cd("/");
						directory_created = ftpc.mkdirPath(remote_path);	// also this will be set as current directory
						if (directory_created) {
							remote_curdir = remote_path;	// store remote directory
							if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread remote remote_curdir created: '" + remote_curdir + "'\n");
						}
						else {
							BatchXSLT.g_mainXSLTFrame.showMess("#### ERROR ftp_sendQueueThread while creating remote path: " + remote_path + "\n");
						}
					}
					else directory_created = true;

					if (directory_created) {
						if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread PUT file: '" + src_pathname + "' to remote: '" + remote_filename + "' type: '" + filetype +"'\n");
						success_transfer = ftpc.storeFile(src_pathname, remote_filename);
					}
					if (success_transfer) {
						BatchXSLT.g_mainXSLTFrame.showMess("*** ftp_sendQueueThread file successfully sent: '" + src_pathname + "' to remote: '" + remote_filename + "' type: '" + filetype +"'\n");
					}
					else {
						BatchXSLT.g_mainXSLTFrame.showMess("### ERROR ftp_sendQueueThread: " + ftpc.last_result_text +"\n");
					}

					ftp_sendQueue.remove(0);

				}

				if (ftp_sendQueue.isEmpty()) {
					if (ftpc_id >= 0) disconnectDestroyFTPClient(ftpc_id);
					go_on = false;
				}
				else {
					try { Thread.sleep(500); }
					catch (InterruptedException e) {}
				}
			}

			ftp_sendQueueThread_returnvalue = 0;
			ftp_sendQueueThread_isrunning = false;
			if (DEBUG_FTP_QUEUE) BatchXSLT.g_mainXSLTFrame.showMess("****** ftp_sendQueueThread QUIT\n");

		}
	 }	// end class ftp_sendQueueThread

	private static String myGetNodeValue(Element elem) {
		String cont;
		if (elem == null) return("");
		Node child = elem.getFirstChild();
		if (child == null) return("");
		try {
			cont = child.getNodeValue();
		} catch (DOMException ex) { return ""; }
		if (cont == null) return "";
		return cont;
	}

	public static int queue_putFile(String itemElement) {	// as XML string
        boolean added;

        added = ftp_sendQueue.add(itemElement);
        if (added) {	// queued
        	// start the queue worker thread
        	if (!ftp_sendQueueThread_isrunning) {
				if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("****** ftp_sendQueueThread will start\n");
				Thread sqt = new ftp_sendQueueThread();
				sqt.start();
				if (DEBUG_FTP_CLIENT) BatchXSLT.g_mainXSLTFrame.showMess("****** ftp_sendQueueThread STARTED\n");
        	}
        	return 0;
        }
        return -1;	// not queued
	}



}	// end class ftp



