/*
 * ====================================================================
 * A simple http server to handle remote commands
 *
 * Version: 2.1
 * Version date: 20181127
 */

package com.epaperarchives.batchxslt;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;


/**
 * This example demonstrates the use of the {@link ResponseHandler} to simplify 
 * the process of processing the HTTP response and releasing associated resources.
 */
public class httpCommander {

  public static final String other_logged_in = "An other user is logged in. Try later.\n";
  public static final String already_logged_in = "You are already logged in.\n";
  public static final String logged_out = "You are logged out.\n";
  public static final String now_logged_in = "You are logged in. Your commands please...\n";
  public static final String invalid_login = "Invalid login. Try again.\n";
  public static final String must_login = "Must login to use me.\n";

  public static int httpCommander_DEBUG = 0;  // whether it show more messages
  public static int httpCommander_Active = 0;  // 1 = it should be activated
  public static String httpCommander_Name = "httpCommandServer";  // The name to publish to the world
  public static String httpCommander_IP = "";
  public static int httpCommander_Port = 8180;  // our commander http port

  public static String httpCommander_ContextPath = "/";          // context root localhost:8180/
  public static String httpCommander_DocumentRoot = "CommanderHTTP/";  // root folder within Batchxslt.app folder
  public static String httpCommander_StartMessage = httpCommander_Name + " is listening on port " + httpCommander_Port + "\n";  // Message to to show in BatchXSLT when started
  public static String httpCommander_HelloMessage = httpCommander_Name + " is listening for commands.\n";  // Message to send on connection
  public static String httpCommander_HelloFile = "";  // Path/name to file to send on connection
  public static String httpCommander_HelloFileType = "text/html; charset=UTF-8";  // File type

    public static String httpCommander_LoginFile = "";  // Path/name to file to send on connection
  public static String httpCommander_LoginFileType = "text/html; charset=UTF-8";  // File type


  private static httpCommander g_httpCommander = null;
  private static HttpContext context = null;
  public static HttpExchange current_http_exchange = null;
  private static Map<String,Object> attributes = null;

    private static boolean use_identification = false;
    private static long httpCommander_logout_timeout = 300000;  // 5 minutes inactive: logout automatically. -1 to never logout
    private static long httpCommander_last_activity = 0;      // store ticks of last activity

    private static Map<String,String> users = new HashMap<String,String>();
    
    public static String try_login_user = "";    // the user which tries to log in
    public static String try_login_password = "";  // the user's password

    private static String loggedin_user = "";        // the name of the actually logged in user or "" if no one is logged in
  private static String loggedin_user_requesterURL = "";  // the requester's URL

    public static void set_DEBUG(int val) {
        httpCommander_DEBUG = val;
    }

    public static void set_Active(int val) {
        httpCommander_Active = val;
    }
    public static int get_Active() {
        return(httpCommander_Active);
    }

    public static void set_httpCommander_Name(String name) {
        httpCommander_Name = name;
    }
    public static String get_httpCommander_Name() {
        return(httpCommander_Name);
    }

    public static void set_IP(String ip) {
        httpCommander_IP = ip;
    }

    public static void set_Port(String port) {
        httpCommander_Port = Integer.valueOf(port);
    httpCommander_StartMessage = httpCommander_Name + " is listening on port " + httpCommander_Port + "\n";  // Message to to show in BatchXSLT when started
    }
    public static int get_Port() {
        return(httpCommander_Port);
    }
    public static void set_contextPath(String root) {
        httpCommander_ContextPath = root;
    }
    public static void set_documentRoot(String root) {
        httpCommander_DocumentRoot = root;
    }
    public static void set_startMessage(String mess) {
        httpCommander_StartMessage = mess;
    }
    public static void set_helloMessage(String mess) {
        httpCommander_HelloMessage = mess;
    }
    public static void set_helloFile(String pathname) {
        httpCommander_HelloFile = pathname;
    }
    public static void set_helloFileType(String type) {
        httpCommander_HelloFileType = type;
    }
    public static void set_loginFile(String pathname) {
        httpCommander_LoginFile = pathname;
    }
    public static void set_loginFileType(String type) {
        httpCommander_LoginFileType = type;
    }

    public static String list_users() {
    Iterator iterator = users.keySet().iterator(); 
      String str ="Users:\n";
        while (iterator.hasNext()) {
       String u = iterator.next().toString();
       String p = users.get(u).toString();
       str += "'" + u + "' -> '" + p + "'\n";
        }
        return(str);
    }




    public static void set_httpCommander_logout_timeout(long ticks) {
        httpCommander_logout_timeout = ticks;
    }
    public static void set_use_identification(int val) {
      users.clear();  // users must be set again
        if (val > 0) use_identification = true;
        else use_identification = false;
    }
    public static boolean get_use_identification() {
        return(use_identification);
    }
    public static void add_user(String user_password) {
      String[] u_p = user_password.split(",");
      if (u_p.length < 2) return;
        try {
            add_user(URLDecoder.decode(u_p[0],"UTF-8"), URLDecoder.decode(u_p[1],"UTF-8"));
        } catch (UnsupportedEncodingException ex) {}
    }
    public static void add_user(String user, String password) {
      if (user.equals("") || password.equals("")) return;
        users.put(user, password);
    }
    public static boolean exists_user(String user) {
        return(users.containsKey(user));
    }
    public static boolean valid_user(String user, String given_password) {
        if (!users.containsKey(user))return(false);
        String set_password = users.get(user);
        if (given_password.equals(set_password) == true) return(true);
        return(false);
    }
    public static boolean login(String user, String password) {
        if (loggedin_user.equals("") == false) return(false);   // other user is logged in
        if (valid_user(user, password) == true) {
            loggedin_user = user;

      URI requestURI = httpCommander.current_http_exchange.getRequestURI();
      loggedin_user_requesterURL = get_requestersURL();
            return(true);
        }
        else {
        }
        return(false);
    }
    public static void logout() {
    try_login_user = "";
    try_login_password = "";
        loggedin_user = "";
        loggedin_user_requesterURL = "";
    }
    public static boolean loggedin() {
        if (!use_identification) return(true);
        if (loggedin_user.equals("") == false) return(true);   // someone is logged in
        return(false);
    }
    public static String get_loggedin_user() {
        return(loggedin_user);
    }
    public static String get_loggedin_user_requesterURL() {
        return(loggedin_user_requesterURL);
    }
    public static boolean is_same_loggedin_user_requesterURL() {
    //BatchXSLT.g_mainXSLTFrame.showMess("requestURL: " + loggedin_user_requesterURL + "\n" + loggedin_user_requesterURL + "\n" + get_requestersURL() + "\n");
        if (loggedin_user_requesterURL.equals(get_requestersURL()) == true) return(true);
        return(false);
    }
    public static String get_requestersURL() {
    InetSocketAddress requestSocketAddress = httpCommander.current_http_exchange.getRemoteAddress();
    InetAddress requestAddress = requestSocketAddress.getAddress();
    String requestURL = requestAddress.getHostAddress();
        return(requestURL);
    }


    public static void set_activity() {
    httpCommander_last_activity = Calendar.getInstance().getTimeInMillis();
    //BatchXSLT.g_mainXSLTFrame.showMess("Last activity: '" + httpCommander_last_activity + ". logout in " + httpCommander_logout_timeout + "\n");
    }
  static class InactivityWatcher extends Thread {
    InactivityWatcher() {
    }

        @Override
    public void run() {
      Thread.currentThread().setName("InactivityWatcher");
      set_activity();
      while (httpCommander_logout_timeout > 0) {
        if (loggedin()) {  // if someone is logged in
          long nowticks = Calendar.getInstance().getTimeInMillis();    // get current ticks since 1970
          //BatchXSLT.g_mainXSLTFrame.showMess("InactivityWatcher now: " + nowticks + "\n");
          if (nowticks > (httpCommander_last_activity + httpCommander_logout_timeout)) {
            BatchXSLT.g_mainXSLTFrame.showMess("User '" + get_loggedin_user() + "' logged out due to inactivity timeout." + "\n");
            logout();
            set_activity();
          }
        }
        try {
          Thread.sleep(10000);  // sleep
        }
        catch (InterruptedException e) {}
      }
    }
  }    
    
    public static void start_server() throws IOException {

        InetSocketAddress addr = new InetSocketAddress(httpCommander.httpCommander_Port);
    HttpServer httpCommandServer = HttpServer.create(addr, 0);
    
    context = httpCommandServer.createContext(httpCommander.httpCommander_ContextPath, new httpCommanderHandler());
    // get server attributes map
    /*  NOT WORKING JET!!!
    attributes = context.getAttributes();
    attributes.put("DocumentRoot", httpCommander.httpCommander_DocumentRoot);
    */
    httpCommandServer.setExecutor(Executors.newCachedThreadPool());
    httpCommandServer.start();
    if (httpCommander.httpCommander_StartMessage.equals("") == false) BatchXSLT.g_mainXSLTFrame.showMess(httpCommander.httpCommander_StartMessage);
    if (httpCommander.httpCommander_DEBUG > 0) {
      dumpHTTPContextAttributes();
      BatchXSLT.g_mainXSLTFrame.showMess(list_users());
    }

    // start the 'no activity' watcher
    if (use_identification && (httpCommander_logout_timeout > 0)) {
      InactivityWatcher inact_runner = new InactivityWatcher();
            inact_runner.start();
    }
  }

  public static void start() {
    try {
      g_httpCommander = new httpCommander();
      g_httpCommander.start_server();
    } catch (IOException ioex) {
      BatchXSLT.g_mainXSLTFrame.showMess(ioex.getMessage());
    }
  }
  public static boolean is_started() {
    if (g_httpCommander != null) return(true);
    return(false);
  }
  
  public static void dumpHTTPContextAttributes() {
    
    attributes = context.getAttributes();
    BatchXSLT.g_mainXSLTFrame.showMess("CommanderHTTP Server Attributes. Number of Entries available: " + attributes.size() + "\n");
    for (Map.Entry entry : attributes.entrySet()) {
      BatchXSLT.g_mainXSLTFrame.showMess(entry.getKey() + ", " + entry.getValue());
      //System.out.println(entry.getKey() + ", " + entry.getValue());
    }
  }
}

class httpCommanderHandler implements HttpHandler {
  private static String commandExceptionNum = "";    // last errors during command execution
  private static String commandExceptionString = "";
  private static String commandMessageString = "";
  private boolean responseHeadersSent = false;

    @Override
  public void handle(HttpExchange exchange) throws IOException {
    String commandExceptionNum = "";
    commandExceptionString = "";
    commandMessageString = "";
    responseHeadersSent = false;

    httpCommander.current_http_exchange = exchange;    // make public for others
    String requestMethod = exchange.getRequestMethod();

    Headers responseHeaders = exchange.getResponseHeaders();
    OutputStream responseBody = exchange.getResponseBody();

    String querykey = "";
    String queryvalue = "";
    String DocumentRoot = httpCommander.httpCommander_DocumentRoot;
    String s = "";  // working string

    if (httpCommander.httpCommander_DEBUG > 0) {
      BatchXSLT.g_mainXSLTFrame.showMess("*** Handling new request.\n");
      s = "requestMethod: '" + requestMethod +"'\n";
      BatchXSLT.g_mainXSLTFrame.showMess(s);
    }

    // set current ticks to state that something has happened
    httpCommander.set_activity();
    while (true) {
      /******** check if logged in and same user *******/
      if (httpCommander.loggedin() == true) {
        if (httpCommander.get_use_identification() && (httpCommander.is_same_loggedin_user_requesterURL() == false)) {
          try {
                      responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, 0);
            responseBody.write(httpCommander.other_logged_in.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
          }
          break;
        }
      }

      /******** handle GET parameters *******/
      if (requestMethod.equalsIgnoreCase("GET")) {
          
        // handle the GET parameters, commands
        URI requestURI = exchange.getRequestURI();
        String requestQuery = requestURI.getQuery();

        if ((requestQuery != null) && requestQuery.startsWith("cmd_fld=")) requestQuery = requestQuery.substring(8);  // send from form using get method
        if (httpCommander.httpCommander_DEBUG > 0) {
          BatchXSLT.g_mainXSLTFrame.showMess("Request by user: '" + httpCommander.get_loggedin_user() + "' logged in: " + httpCommander.loggedin() + " from: " + httpCommander.get_requestersURL() + "\n");
          BatchXSLT.g_mainXSLTFrame.showMess("QUERY: " + requestQuery + "\n");
          BatchXSLT.g_mainXSLTFrame.showMess("URI: " + requestURI + "\n");
        }

        if ((requestURI.toString().equals("/favicon.ico") == true)) {  // Browser requests the favicon.ico file
          if (!responseHeadersSent) {
            responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
          }
          if (httpCommander.httpCommander_DEBUG > 0) BatchXSLT.g_mainXSLTFrame.showMess("IGNORED file request: favicon.ico.\n");
          break;
        }

        if ((requestQuery == null) || (requestQuery.equals("") == true)) {  // nothing special - send the initial file if logged in

          /******** check if logged in *******/
          if (httpCommander.loggedin() == false) {
            // send login file
            if (httpCommander.httpCommander_LoginFile.equals("") == false) {
              if (!responseHeadersSent) {
                  if (httpCommander.httpCommander_LoginFileType.equals("") == false) responseHeaders.set("Content-Type", httpCommander.httpCommander_LoginFileType);
                  exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
                }
                if (httpCommander.httpCommander_DEBUG > 0) BatchXSLT.g_mainXSLTFrame.showMess("httpCommander: Sending Login File: " + httpCommander.httpCommander_LoginFile + "\n");

                try {
                    String fpath = utils.file_fullPath(mainXSLTFrame.appDir, DocumentRoot + httpCommander.httpCommander_LoginFile);
                    s = utils.readFileUTF(fpath);
                    int err = utils.getLastFileExceptionError();
                    if (err != 0) {
                        String mess = utils.getLastFileExceptionMessage();
                        BatchXSLT.g_mainXSLTFrame.showMess("Error while reading a file. Error code: " + err + " Message: " + mess + "\n");
                    }
                    else responseBody.write(s.getBytes("UTF-8"));
                } catch (IOException ex) {
                    commandExceptionString = ex.getMessage();
                    BatchXSLT.g_mainXSLTFrame.showMess(commandExceptionString);
                }
            }
            break;
          }

          if (httpCommander.httpCommander_HelloFile.equals("") == false) {
            if (!responseHeadersSent) {
              if (httpCommander.httpCommander_HelloFileType.equals("") == false) responseHeaders.set("Content-Type", httpCommander.httpCommander_HelloFileType);
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            if (httpCommander.httpCommander_DEBUG > 0) BatchXSLT.g_mainXSLTFrame.showMess("httpCommander: Sending Hello File: " + httpCommander.httpCommander_HelloFile + "\n");

            try {
              String fpath = utils.file_fullPath(mainXSLTFrame.appDir, DocumentRoot + httpCommander.httpCommander_HelloFile);
              s = utils.readFileUTF(fpath);
              int err = utils.getLastFileExceptionError();
              if (err != 0) {
                String mess = utils.getLastFileExceptionMessage();
                BatchXSLT.g_mainXSLTFrame.showMess("Error while reading a file. Error code: " + err + " Message: " + mess + "\n");
              }
              else responseBody.write(s.getBytes("UTF-8"));
            } catch (IOException ex) {
              commandExceptionString = ex.getMessage();
              BatchXSLT.g_mainXSLTFrame.showMess(commandExceptionString);
            }
          }
        }
        else {

          if (httpCommander.httpCommander_DEBUG > 1) {
            Headers requestHeaders = exchange.getRequestHeaders();
            Set<String> keySet = requestHeaders.keySet();
            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
              String key = iter.next();
              List values = requestHeaders.get(key);
              s = key + " = " + values.toString() + "\n";
              BatchXSLT.g_mainXSLTFrame.showMess(s);
            }
          }
          int idx;
          int equalpos;
          String commandReturnMessage = "";
          String paramArr[] = requestQuery.split("&");
          for (idx = 0; idx < paramArr.length; idx++) {
            querykey = queryvalue = "";
            paramArr[idx] = URLDecoder.decode(paramArr[idx], "UTF-8");
            equalpos = paramArr[idx].indexOf("=");
            if (equalpos >= 0) {
              querykey = paramArr[idx].substring(0, equalpos);
              queryvalue = paramArr[idx].substring(equalpos+1);
            }
            else querykey = paramArr[idx];
            
            if (httpCommander.httpCommander_DEBUG > 0) {
              s = "Executing QUERY part #" + idx + ": '" + querykey + "'='" + queryvalue + "'\n";
              BatchXSLT.g_mainXSLTFrame.showMess(s);
            }
            
            int retval = commandExecute(querykey, queryvalue, responseBody, exchange);
            switch (retval) {
              case 0:  // no error
                if (commandMessageString.equals("") == false) {
                  commandReturnMessage += commandMessageString + "\r\n";
                }
                break;
              case -1:  // command not found
                // we say nothing to the user but log it
                s = "#ERROR# " + retval + " command '" + querykey + "' not found.\n";
                BatchXSLT.g_mainXSLTFrame.showMess(s);
                break;
              default:  // error during command execution
                // we say nothing to the user but log it
                s = "#ERROR# " + retval + " while executing command '" + querykey + "'. ";
                if (commandExceptionString.equals("") == false) s += "ERRMSG: " + commandExceptionString + "\n";
                BatchXSLT.g_mainXSLTFrame.showMess(s);
                break;
            }
          }
          if (commandReturnMessage.equals("") == false) {
            if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(commandReturnMessage.getBytes("UTF-8"));
          }
        }
      }
      /******** handle POST forms *******/
      else {
        InputStreamReader isr =  new InputStreamReader(exchange.getRequestBody(),"utf-8");
        BufferedReader br = new BufferedReader(isr);
        String requestbody = "";

        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
          buf.append((char) b);
        }
        br.close();
        isr.close();
      
        requestbody = buf.toString();

        if ((requestbody != null) && requestbody.startsWith("post_content=")) requestbody = requestbody.substring(13);  // send from form using post method
        requestbody = requestbody.replaceAll("%26(?!amp;)", "&");

        if (httpCommander.httpCommander_DEBUG > 0) {  // dump request body
          BatchXSLT.g_mainXSLTFrame.showMess("*** requestbody : \n" + requestbody + "\n");
        }
        if (requestbody.equals("") == false) {
          if (httpCommander.httpCommander_DEBUG > 1) {
            Headers requestHeaders = exchange.getRequestHeaders();
            Set<String> keySet = requestHeaders.keySet();
            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
              String key = iter.next();
              List values = requestHeaders.get(key);
              s = key + " = " + values.toString() + "\n";
              BatchXSLT.g_mainXSLTFrame.showMess(s);
            }
          }
          int idx;
          int equalpos;
          String commandReturnMessage = "";
          String paramArr[] = requestbody.split("&");
          for (idx = 0; idx < paramArr.length; idx++) {
            querykey = queryvalue = "";
            paramArr[idx] = URLDecoder.decode(paramArr[idx], "UTF-8");
            equalpos = paramArr[idx].indexOf("=");
            if (equalpos >= 0) {
              querykey = paramArr[idx].substring(0, equalpos);
              queryvalue = paramArr[idx].substring(equalpos+1);
            }
            else querykey = paramArr[idx];
            
            if (httpCommander.httpCommander_DEBUG > 0) {
              s = "Executing QUERY part #" + idx + ": '" + querykey + "'='" + queryvalue + "'\n";
              BatchXSLT.g_mainXSLTFrame.showMess(s);
            }
            
            int retval = commandExecute(querykey, queryvalue, responseBody, exchange);
            switch (retval) {
              case 0:  // no error
                if (commandMessageString.equals("") == false) {
                  commandReturnMessage += commandMessageString + "\r\n";
                }
                break;
              case -1:  // command not found
                // we say nothing to the user but log it
                s = "#ERROR# " + retval + " command '" + querykey + "' not found.\n";
                BatchXSLT.g_mainXSLTFrame.showMess(s);
                break;
              default:  // error during command execution
                // we say nothing to the user but log it
                s = "#ERROR# " + retval + " while executing command '" + querykey + "'. ";
                if (commandExceptionString.equals("") == false) s += "ERRMSG: " + commandExceptionString + "\n";
                BatchXSLT.g_mainXSLTFrame.showMess(s);
                break;
            }
          }
          if (commandReturnMessage.equals("") == false) {
            if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(commandReturnMessage.getBytes("UTF-8"));
          }
        }
      }
      if (httpCommander.httpCommander_DEBUG > 0) {
        BatchXSLT.g_mainXSLTFrame.showMess("Request handled.\n");
      }

      // we are done
      break;
    }
    responseBody.close();
  }

  // the command executor
  private int commandExecute(String key, String value, OutputStream responseBody, HttpExchange exchange) {
    Headers responseHeaders = exchange.getResponseHeaders();
    commandExceptionNum = "";    // last errors during command execution
    commandExceptionString = "";
    String s = "";    // working string
    String path = "";  // working string
    File f = null;

    do {
      /*--------------------*/
      if (key.equals("user") == true) {  // user log in
        httpCommander.try_login_user = "";
        if (value.equals("") == true) {
          break;  // no user name given
        }
        if ((httpCommander.get_loggedin_user().equals("") == false) &&  (httpCommander.is_same_loggedin_user_requesterURL() == false)) {
          try {
            if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(httpCommander.other_logged_in.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        httpCommander.try_login_user = value;
        break;
      }
      /*--------------------*/
      if (key.equals("pass") == true) {  // the password
        httpCommander.try_login_password = "";
        if (value.equals("") == true) {
          httpCommander.try_login_user = "";
          break;  // no user name given
        }
        if (httpCommander.get_loggedin_user().equals("") == false) {
          try {
                        if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(httpCommander.other_logged_in.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        httpCommander.try_login_password = value;
        if (httpCommander.valid_user(httpCommander.try_login_user, httpCommander.try_login_password) == true) {
          boolean loggedin = httpCommander.login(httpCommander.try_login_user, httpCommander.try_login_password);
          if (loggedin) {
            try {
              if (!responseHeadersSent) {
                responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
              }
              responseBody.write(httpCommander.now_logged_in.getBytes("UTF-8"));
            } catch (IOException ex) {
              commandExceptionString = ex.getMessage();
              return(-2);
            }
          }
        }
        else {
          try {
            if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(httpCommander.invalid_login.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
        }
        httpCommander.try_login_user = "";
        httpCommander.try_login_password = "";
        break;
      }
      /*--------------------*/
      if (key.equals("logout") == true) {  // log user out
        httpCommander.logout();
        try {
                    if (!responseHeadersSent) {
            responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
          }
          responseBody.write(httpCommander.logged_out.getBytes("UTF-8"));
        } catch (IOException ex) {
          commandExceptionString = ex.getMessage();
          return(-2);
        }
        break;
      }

            /******** check if logged in *******/
            if (httpCommander.loggedin() == false) {
                try {
                     if (!responseHeadersSent) {
            responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
                    }
                    responseBody.write(httpCommander.must_login.getBytes("UTF-8"));
                } catch (IOException ex) {
                    commandExceptionString = ex.getMessage();
                    BatchXSLT.g_mainXSLTFrame.showMess(commandExceptionString + "\n");
                }
                return(0);
            }


      /*===============================================*/
      // commands for logged in users only
      if (httpCommander.loggedin() == true) {
        /*--------------------*/
        if (key.equals("lun") == true) {  // get logged in user name
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(httpCommander.get_loggedin_user().getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("luip") == true) {  // get logged in user IP
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(httpCommander.get_loggedin_user_requesterURL().getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        /*--------------------*/
        if (key.equals("quit") == true) {  // quit transformer
          System.exit(0);
          break;
        }
        /*--------------------*/
        if (key.equals("ts") == true) {  // get transformer status (busy or idle)
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.transformStausMessage.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("debug") == true) {  // set debug mode
          if (value.equals("0") == false) {
            httpCommander.httpCommander_DEBUG = 1;
          }
          else {
            httpCommander.httpCommander_DEBUG = 0;
          }
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(Integer.toString(httpCommander.httpCommander_DEBUG).getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("pdebug") == true) {  // print debug mode
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(Integer.toString(httpCommander.httpCommander_DEBUG).getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("pwd") == true) {  // print working directory
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(System.getProperty("user.dir").getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("pcd") == true) {  // print communication directory
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.jtoverrideQueuePath.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("pcn") == true) {  // print communication queue file name
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.jtoverrideQueueName.getBytes("UTF-8"));  // default = override.que
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("pcf") == true) {  // print content of comm.prefs file
          try {
            s = utils.readFileUTF(mainXSLTFrame.jtoverrideQueuePath + mainXSLTFrame.commPrefsSubPath + mainXSLTFrame.commPrefsName);
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(s.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        /*--------------------*/
        if ((key.equals("plf") == true)      // print content of log file as is (unwrapped, plain file contents)
          || (key.equals("plfp") == true)    // print content of log file as pre formatted text (wrap in <pre></pre>)
          || (key.equals("plfh") == true)) {  // print content of log file as HTML
          try {
            s = utils.readFileUTF(mainXSLTFrame.logfile_path + mainXSLTFrame.logfile_name);
            if (s.equals("")) {  // logfile empty or not found
              commandMessageString = "NO Log file content available at file: '" + mainXSLTFrame.logfile_path + mainXSLTFrame.logfile_name + "'. Cause: " + utils.getLastFileExceptionMessage();
              return(0);
            }
            if (key.equals("plfh") == true) {  // convert to HTML
              String sep = "\n";
              do {
                if (s.indexOf("\r\n") >= 0) { sep = "\r\n"; break; }
                if (s.indexOf("\r") >= 0) { sep = "\r"; break; }
              } while(false);
              s = s.replaceAll(sep,"<br>"+sep);
            }
            else if (key.equals("plfp") == true) {  // wrap in <pre></pre>
                s = "<pre>" + System.getProperty("line.separator") + s + "</pre>" + System.getProperty("line.separator");
               }
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(s.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if ((key.equals("plfn") == true)  // print log file name
          || (key.equals("plfpn") == true)) {  // print log file path and name
          if (key.equals("plfpn") == true) s = mainXSLTFrame.logfile_path + mainXSLTFrame.logfile_name;
          else s = mainXSLTFrame.logfile_name;
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(s.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("clf") == true) {  // delete/cancel log file
          int deleted = utils.deleteLogFile(true);  // returns 0 or -1
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
                        if (deleted == 0) responseBody.write("OK".getBytes("UTF-8"));
                        else {
                            String errstr = utils.getLastFileExceptionMessage();
                            int err = utils.getLastFileExceptionError();
                            responseBody.write((Integer.toString(err) + ": " + errstr).getBytes("UTF-8"));
                        }
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        /*--------------------*/
        if (key.equals("hcn") == true) {  // print commander name 
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(httpCommander.httpCommander_Name.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        if (key.equals("hcappn") == true) {  // print commander application name 
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.applFullName.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        if (key.equals("appn") == true) {  // print commander application name  and commander name
          try {
            s = mainXSLTFrame.applFullName + " " + httpCommander.httpCommander_Name;
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(s.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("hccp") == true) {  // print http commander context path
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(httpCommander.httpCommander_ContextPath.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("hcdr") == true) {  // print http commander document root
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(httpCommander.httpCommander_DocumentRoot.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("hcip") == true) {  // print commander ip address
          try {
            InetSocketAddress isa = exchange.getLocalAddress() ;
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(isa.toString().getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        /*--------------------*/
        if (key.equals("spn") || key.equals("srcpathname")) {  // set sourcePathName from GET or form POST
          if (value.equals("")) {
            commandExceptionString = "Not enough parameters. Syntax: spn=pathname";
            return(-2);
          }
          if (BatchXSLTransform.isWindows() == true) mainXSLTFrame.sourcePathName = value.replace('/',File.separatorChar);
          else mainXSLTFrame.sourcePathName = value;
          if ( mainXSLTFrame.sourcePathName.startsWith("~") == true) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            mainXSLTFrame.sourcePathName = home + mainXSLTFrame.sourcePathName.substring(2);  // cut leading path ~/
          }
          BatchXSLT.g_mainXSLTFrame.sourcePathNameFld.setText(mainXSLTFrame.sourcePathName);
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.sourcePathName.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        if (key.equals("pspn") == true) {  // print sourcePathName
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.sourcePathName.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        /*--------------------*/
        if (key.equals("opn") || key.equals("outputpathname")) {  // set outputPathName from GET or form POST  
          if (BatchXSLTransform.isWindows() == true) {
            if (BatchXSLTransform.get_URL_file_type(value) != 1) mainXSLTFrame.outputPathName = value;
            else mainXSLTFrame.outputPathName = value.replace('/',File.separatorChar);
          }
          else mainXSLTFrame.outputPathName = value;
          if ( mainXSLTFrame.outputPathName.startsWith("~") == true) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            mainXSLTFrame.outputPathName = home + mainXSLTFrame.outputPathName.substring(2);  // cut leading path ~/
          }
          BatchXSLT.g_mainXSLTFrame.outputPathNameFld.setText(mainXSLTFrame.outputPathName);
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.outputPathName.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        if (key.equals("popn") == true) {  // print outputPathName
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.outputPathName.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        /*--------------------*/
        if (key.equals("oext")) {  // set output file extension from GET or form POST  
          mainXSLTFrame.newoutputFileNameExt = value;
          BatchXSLT.g_mainXSLTFrame.newoutputFileNameExtFld.setText(mainXSLTFrame.newoutputFileNameExt);
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.newoutputFileNameExt.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        if (key.equals("poext") == true) {  // print output file extension
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.newoutputFileNameExt.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        /*--------------------*/
        if (key.equals("xslpn") == true) {  // set xslPathName
          if (BatchXSLTransform.isWindows() == true) mainXSLTFrame.xslPathName = value.replace('/',File.separatorChar);
          else mainXSLTFrame.xslPathName = value;
          if ( mainXSLTFrame.xslPathName.startsWith("~") == true) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            mainXSLTFrame.xslPathName = home + mainXSLTFrame.xslPathName.substring(2);  // cut leading path ~/
          }
          BatchXSLT.g_mainXSLTFrame.xslPathNameFld.setText(mainXSLTFrame.xslPathName);
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.xslPathName.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        if (key.equals("pxslpn") == true) {  // print xslPathName
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(mainXSLTFrame.xslPathName.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        if (key.equals("trans") == true) {  // start transform
          BatchXSLT.g_mainXSLTFrame.startTransform();
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(("OK").getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }

        /*--------------------*/
        if ((key.equals("jt") == true)      // load a JobTicket but do not run it
                            // used to set general options
          || (key.equals("jtr") == true)) {  // load a JobTicket and run it
          String jt = "";
          int loaded;
          if (value.equals("")) {
            commandExceptionString = "Not enough parameters. Syntax: jt=pathname";
            return(-2);
          }
          if (BatchXSLTransform.isWindows() == true) jt = value.replace('/',File.separatorChar);
          else jt = value;
          if ( jt.startsWith("~") == true) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            jt = home + jt.substring(2);  // cut leading path ~/
          }
          mainXSLTFrame.jobTicketFileName = jt;
          if (key.equals("jtr") == true) {
            loaded = BatchXSLTransform.readJobTicketFile(1);  // read this JobTicket and run the Transform: parameter 1
          }
          else {
            loaded = BatchXSLTransform.readJobTicketFile(0);  // read this JobTicket but do not run the Transform: parameter 0
          }
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(Integer.toString(loaded).getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("pjg") == true) {  // print JobTicket groups (subfolders in the user's jobtickets
          path = mainXSLTFrame.jtoverrideQueuePath + mainXSLTFrame.jobticketsSubPath;  // here we find the user's Jobtickets groups
          File flist[];
          String dirlist = "default";
          File jtDir = new File( path );
          flist = jtDir.listFiles();
          if (flist == null) {
            // we will simply return 'default' even if the user has no own stored JobTicket groups
            //      commandExceptionString = "User's JobTickets path '" + path + "' not found.";
            //      return(-2);
          }
          else {
            for(int i = 0; i < flist.length; i++){
              if (flist[i].getName().equalsIgnoreCase("default")) continue;
              if (!flist[i].isDirectory()) continue;
              dirlist += "," + flist[i].getName();
            }
          }

          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(dirlist.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if ((key.equals("pjf") == true)      // print content of a JobTicket file as plain text
          || (key.equals("pjfh") == true)) {  // print content of a JobTicket file as HTML
                        // pjf=[path/]name.jt (path is added automatically if not given)
                        // pjf=[object,][path/]name.jt (object is 'default' if not given) (path is added automatically if not given)
                        
                        // the default path to look for jobtickets is within the communications folder like: ~/easEPub4InDesignCommV6/easEPub/jobtickets/default/
                        // for an URL like: http://192.168.1.36:8180/?pjf=override.jt
                        
                        // The URL: http://192.168.1.36:8180/?pjf=Other%20Customer,override.jt
                        // finds an override.jt at path: ~/easEPub4InDesignCommV6/easEPub/jobtickets/Other Customer/override.jt
          String objectname = "default";
          String objectsubpath = "default/";
          String pathname ="";
          int pos = value.indexOf(",");  // get object,pathname
          if (pos < 0) {  // object identifier not given: pjf=name.jt
            pathname = value;
          }
          else {  // pjf=object,name.jt
            objectname = value.substring(0,pos);
            objectsubpath = objectname;
            pathname = value.substring(pos+1);
          }
          if (objectsubpath.endsWith(File.separator) == false) objectsubpath = objectsubpath + File.separator;
          try {
            boolean f_exists = false;
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            if (value.indexOf(File.separator) < 0) {
              path = mainXSLTFrame.jtoverrideQueuePath + mainXSLTFrame.jobticketsSubPath + objectsubpath + pathname;
              if (httpCommander.httpCommander_DEBUG > 0) {
                BatchXSLT.g_mainXSLTFrame.showMess("Search #1 JobTicket at: '" + path + "\n");
              }
              //responseBody.write(("1: " + path + "\n").getBytes("UTF-8"));
              f = new File(path);
              f_exists = f.exists();
              if (!f_exists) {  // try to find in working path
                path = mainXSLTFrame.jobticketsWorkingPath + objectsubpath + pathname;
                if (httpCommander.httpCommander_DEBUG > 0) {
                  BatchXSLT.g_mainXSLTFrame.showMess("Search #2 JobTicket at: '" + path + "\n");
                }
                //responseBody.write(("2: " + path + "\n").getBytes("UTF-8"));
                f = new File(path);
                f_exists = f.exists();
              }
              if (!f_exists) {  // try to find in package path
                path = mainXSLTFrame.jobticketsPackagePath + pathname;
                if (httpCommander.httpCommander_DEBUG > 0) {
                  BatchXSLT.g_mainXSLTFrame.showMess("Search #3 JobTicket at: '" + path + "\n");
                }
                //responseBody.write(("3: " + path + "\n").getBytes("UTF-8"));
                f = new File(path);
                f_exists = f.exists();
              }
            }
            else {  // full path is given
              path = pathname;
              if (httpCommander.httpCommander_DEBUG > 0) {
                BatchXSLT.g_mainXSLTFrame.showMess("Search full path JobTicket at: '" + path + "\n");
              }
              f = new File(path);
              f_exists = f.exists();
            }
            if (f_exists) {
              if (httpCommander.httpCommander_DEBUG > 0) {
                BatchXSLT.g_mainXSLTFrame.showMess("Found JobTicket at: '" + path + "\n");
              }
              s = utils.readFileUTF(path);
              if (key.equals("pjfh") == true) {  // convert to HTML
                String sep = "\n";
                do {
                  if (s.indexOf("\r\n") >= 0) { sep = "\r\n"; break; }
                  if (s.indexOf("\r") >= 0) { sep = "\r"; break; }
                } while(false);
                s = s.replaceAll(sep,"<br>"+sep);
              }
              responseBody.write(s.getBytes("UTF-8"));
            }
            else {
              commandExceptionString = "File '" + path + "' not found.";
              return(-2);
            }
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          break;
        }
        /*--------------------*/
        if (key.equals("savejtfile") == true) {  // savejtfile=pathname,content
          int pos = value.indexOf(",");  // get pathname
          if (pos < 0) {
            commandExceptionString = "Not enough parameters. Syntax: savejtfile=pathname,content";
            return(-2);
          }
          String pathname = value.substring(0,pos);
          if ( pathname.startsWith("~") == true) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            pathname = home + pathname.substring(2);  // cut leading path ~/
          }
          String content = value.substring(pos+1);
          //int written = utils.writeFile(String pathname, String content, boolean overwrite, boolean append, String encoding) {
          int written = utils.writeFile(pathname, content, true, false, "UTF-8");
          // writeFile returns:
          // 0 = OK: written and also 0 if file exists and overwrite is false
          // -1 = exception while writing file
          // -2 = empty pathname
          if (written != 0) {
            commandExceptionString = "#ERROR* " + written + " writing file '" + pathname + "'.";
            return(-2);
          }
          else commandMessageString = "OK";
          return(written);
        }

        /*--------------------*/
        if (key.equals("f") == true) {  // f=pathname
                        // get file from commander folder
          String dirlist = "";
          boolean f_exists = false;
          if (value.equals("")) {
            commandExceptionString = "Not enough parameters. Syntax: f=pathname";
            return(-2);
          }
          path = value;
                     path = utils.file_fullPath(mainXSLTFrame.appDir, httpCommander.httpCommander_DocumentRoot + path);
          f = new File(path);
          f_exists = f.exists();
          if (!f_exists) {  // silently die
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              try {
                exchange.sendResponseHeaders(404, 0); responseHeadersSent = true;
              } catch (IOException ex) {
              }
            }
            commandExceptionString = "File: '" + path + "' not found";
            return(-2);
          }
                    try {
                         if (!responseHeadersSent) {
                           String mime = utils.detectMimeType(path);
              responseHeaders.set("Content-Type", mime + "; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
                        if (httpCommander.httpCommander_DEBUG > 0) BatchXSLT.g_mainXSLTFrame.showMess("httpCommander: Sending File: " + path + "\n");
            byte[] b = utils.readFile(path);
            if (b == null) {
              commandExceptionString = "Error reading file '" + path + "'";
              return(-2);
            }
            else responseBody.write(b);
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          return(0);
        }

        /*--------------------*/
        if ((key.equals("flist") == true)    // flist=pathname to list files
          || (key.equals("dlist") == true)) {  // dlist=pathname to list directories
          File flist[];
          String dirlist = "";
          if (value.equals("")) {
            if (key.equals("flist") == true) commandExceptionString = "Not enough parameters. Syntax: flist=pathname";
            else commandExceptionString = "Not enough parameters. Syntax: dlist=pathname";
            return(-2);
          }
          path = value;
          if ( path.startsWith("~") == true) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            path = home + path.substring(2);  // cut leading path ~/
          }
          File fDir = new File( path );
          flist = fDir.listFiles();
          if (flist == null) {
            // no files we will simply return '*none*'
            dirlist = "*none*";
          }
          else {
            for(int i = 0; i < flist.length; i++){
              if (flist[i].getName().startsWith(".")) continue;  // skip dot files
              if (key.equals("flist") == true) {
                if (flist[i].isDirectory()) continue;    // want files only
              }
              else {
                if (!flist[i].isDirectory()) continue;    // want folders only
              }
              if (dirlist.equals("") == false) dirlist += "\n";
              dirlist += flist[i].getName();
            }
            if (dirlist.equals("") == true) dirlist = "*none*";
          }

          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/plain; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(dirlist.getBytes("UTF-8"));
          } catch (IOException ex) {
            // ignore errors
            //commandExceptionString = ex.getMessage();
            //return(-2);
          }
          return(0);
        }

        /*--------------------*/
        if (key.equals("existsfile") == true) {  // existsfile=pathname
          if (value.equals("")) {
            commandExceptionString = "Not enough parameters. Syntax: existsfile=pathname";
            return(-2);
          }
          String pathname = value;
          if ( pathname.startsWith("~") == true) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            pathname = home + pathname.substring(2);  // cut leading path ~/
          }
          boolean exists = utils.existsFile(pathname);
          if (exists) s = "true";
          else s = "false";
          try {
                         if (!responseHeadersSent) {
              responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
              exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            }
            responseBody.write(s.getBytes("UTF-8"));
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          return(0);
        }
        /*--------------------*/
        if ((key.equals("writefile") == true) || (key.equals("writefiledb64") == true)) {  // writefile=pathname,content
          int pos = value.indexOf(",");  // get pathname
          if (pos < 0) {
            commandExceptionString = "Not enough parameters. Syntax: writefile=pathname,content";
            return(-2);
          }
          String pathname = value.substring(0,pos);
          if ( pathname.startsWith("~") == true) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            pathname = home + pathname.substring(2);  // cut leading path ~/
          }
          String content = value.substring(pos+1);
          if (key.equals("writefiledb64") == true) {  // de-binhex content before writing
            try {
              byte[] contentbytes = org.apache.commons.codec.binary.Base64.decodeBase64(content);
              content = new String(contentbytes);
            } catch (Exception e) {
              // just write as is
            }
          }
          //int written = utils.writeFile(String pathname, String content, boolean overwrite, boolean append, String encoding) {
          int written = utils.writeFile(pathname, content, true, false, "UTF-8");
          // writeFile returns:
          // 0 = OK: written and also 0 if file exists and overwrite is false
          // -1 = exception while writing file
          // -2 = empty pathname
          if (httpCommander.httpCommander_DEBUG > 0) {
            BatchXSLT.g_mainXSLTFrame.showMess("writefile: '" + pathname + "\n" + content + "\n");
          }
          if (written != 0) {
            commandExceptionString = "#ERROR* " + written + " writing file '" + pathname + "'.";
            return(-2);
          }
          else commandMessageString = "OK";
          return(written);
        }

        /*--------------------*/
        if (key.equals("appendfile") == true) {  // appendfile=pathname,content
          int pos = value.indexOf(",");  // get pathname
          if (pos < 0) {
            commandExceptionString = "Not enough parameters. Syntax: appendfile=pathname,content";
            return(-2);
          }
          String pathname = value.substring(0,pos);
          String content = value.substring(pos+1);
          //int written = utils.writeFile(String pathname, String content, boolean overwrite, boolean append, String encoding) {
          int written = utils.writeFile(pathname, content, true, true, "UTF-8");
          // writeFile returns:
          // 0 = OK: written and also 0 if file exists and overwrite is false
          // -1 = exception while writing file
          // -2 = empty pathname
          if (httpCommander.httpCommander_DEBUG > 0) {
            BatchXSLT.g_mainXSLTFrame.showMess("appendfile: '" + pathname + "'\n" + content + "\n");
          }
          if (written != 0) {
            commandExceptionString = "#ERROR* " + written + " appending file '" + pathname + "'.";
            return(-2);
          }
          else commandMessageString = "OK";
          return(written);
        }

        /*--------------------*/
        if (key.equals("copyfile") == true) {
          String[] paths = value.split(",");  // parameter is like: copyfile=inpath,outpath
          if (paths.length < 2) {
            commandExceptionString = "Not enough parameters. Syntax: copyfile=inputpath,outputpath";
            return(-2);
          }
          String the_sourcepath = "";
          String the_sourcename = "";
          String the_targetpath = "";
          String the_targetname = "";

          if ( (paths[0].startsWith("~") == true) || (paths[1].startsWith("~") == true) ) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            if (paths[0].startsWith("~") == true) paths[0] = home + paths[0].substring(2);  // cut leading path ~/
            if (paths[1].startsWith("~") == true) paths[1] = home + paths[1].substring(2);  // cut leading path ~/
          }

          f = new File(paths[0]);
          the_sourcepath = f.getParent();
          the_sourcename = f.getName();
          if (paths[1].endsWith(File.separator) == false) {
            f = new File(paths[1]);
            the_targetpath = f.getParent();
            the_targetname = f.getName();
          }
          else the_targetpath = paths[1];
          if (the_sourcepath == null) the_sourcepath = "~/";
          if (the_targetpath == null) the_targetpath = "~/";
          if ( (the_sourcepath.startsWith("~") == true) || (the_targetpath.startsWith("~") == true) ) {
            String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
            if (the_sourcepath.startsWith("~") == true) the_sourcepath = home + the_sourcepath.substring(2);  // cut leading path ~/
            if (the_targetpath.startsWith("~") == true) the_targetpath = home + the_targetpath.substring(2);  // cut leading path ~/
          }
          if ((the_sourcepath.equals("") == false) && (the_sourcepath.endsWith(File.separator) == false)) the_sourcepath += File.separator;
          if ((the_targetpath.equals("") == false) && (the_targetpath.endsWith(File.separator) == false)) the_targetpath += File.separator;
          if (httpCommander.httpCommander_DEBUG > 0) {
            s = "Starting file copy: '" + the_sourcename + "' to '" + the_targetname +"'\n";
            BatchXSLT.g_mainXSLTFrame.showMess(s);
          }
          //int copied = BatchXSLTransform.copyFile(the_sourcepath, the_sourcename, the_targetpath, the_targetname, create_target_path, give_message);
          int copied = BatchXSLTransform.copyFile(the_sourcepath, the_sourcename, the_targetpath, the_targetname, 1, false);
          if (httpCommander.httpCommander_DEBUG > 0) {
            s = "the_sourcepath: " + the_sourcepath + "\n";
            s += "the_sourcename: " + the_sourcename + "\n";
            s += "the_targetpath: " + the_targetpath + "\n";
            s += "the_targetname: " + the_targetname + "\n";
            s += "copied: " + copied + "\n";
            BatchXSLT.g_mainXSLTFrame.showMess(s);
          }
          if (copied != 0) {
            commandExceptionString = "#ERROR* " + copied + " copying file '" + the_sourcename + "'.";
            return(copied);
          }
          else commandMessageString = "OK";
          return(copied);
        }

        /*--------------------*/
        if (key.equals("winshot") == true) {
          String format = value;
          String mime = "image/";
          if (format.equals("") == true) format = "png";
          if (format.equals("jpg") == true) format = "jpeg";
          
          mime = mime + format;
          ByteArrayOutputStream os = mainXSLTFrame.get_frame_screenshot(format);
          if (os == null) {
            commandExceptionString = "Error creating screen shot";
            return(-2);
          }
          try {
            responseHeaders.set("Content-Type", mime);
            responseHeaders.set("Pragma", "no-cache");
            responseHeaders.set("Cache-Control", "private, no-cache, no-store, max-age=0, must-revalidate, proxy-revalidate");
            responseHeaders.set("Expires", "Tue, 01 Jan 2012 00:00:00 GMT");
            exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
            responseBody.write(os.toByteArray());
            os.close();
            /*
                responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, 0); responseHeadersSent = true;
                responseBody.write(("os is OK and has bytes: " + os.size()).getBytes("UTF-8"));
            */
          } catch (IOException ex) {
            commandExceptionString = ex.getMessage();
            return(-2);
          }
          return(0);
        }
      
      }  // end commands for logged in users
      
      return(-1);  // command not found
    } while (false);

    return(0);
  }

}
