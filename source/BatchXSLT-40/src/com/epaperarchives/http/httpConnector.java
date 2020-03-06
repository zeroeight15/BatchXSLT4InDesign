/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.epaperarchives.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;

/**
 * This example demonstrates the use of the {@link ResponseHandler} to simplify 
 * the process of processing the HTTP response and releasing associated resources.
 */
public class httpConnector {
	
	private static int DEBUG = 0;

	private static String lastConnectionMessage = "";
	private static String lastConnectionState = "";
	private static String lastConnectionCode = "";
	private static String lastConnectionResponseBody = "";
	
	public static int setDEBUG(String val) {
		DEBUG = Integer.valueOf(val);
		return(DEBUG);
	}
	public static int setDEBUG(int val) {
		DEBUG = val;
		return(DEBUG);
	}
	public static String getLastConnectionMessage() {
		return(lastConnectionMessage);
	}
	public static String getLastConnectionState() {
		return(lastConnectionState);
	}
	public static String getLastConnectionCode() {
		return(lastConnectionCode);
	}
	public static String getLastConnectionResponseBody() {
		return(lastConnectionResponseBody);
	}
	
    public static String getHTTP(String[] params) {	// enc ="UTF-8" or 'binary'
    	if (params.length < 3) return("### ERROR -200");
    	return(getHTTP( params[0], params[1], params[2]));
	}
    public static String getHTTP(String url, String enc, String dodebug) {	// enc ="UTF-8" or 'binary'
    	String response;
    	if (dodebug.equals("0") == false) DEBUG = 1;
    	else DEBUG = 0;
    	response = getHTTP( url, enc, false);
    	DEBUG = 0;
    	return(response);
	}
    public static String getHTTP(String url, String enc) {	// enc ="UTF-8" or 'binary'
    	return(getHTTP( url, enc, false));
	}
    public static String getHTTP(String url, String enc, boolean silent) {	// enc ="UTF-8" or 'binary'
		lastConnectionMessage = "";
		lastConnectionState = "";
		lastConnectionCode = "";
		
		if (url.equals("") == true) return("");
		/*
		int connectionTimeoutMillis = 10000;
		int socketTimeoutMillis = 10000;
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMillis);
		HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMillis);

		ClientConnectionManager cm = new SimpleHttpConnectionManager(params);

		DefaultHttpClient httpclient = new DefaultHttpClient(cm, httpParams);
        */
		DefaultHttpClient httpclient = new DefaultHttpClient();        

		/* check if we have to use proxy */
		ProxySelectorRoutePlanner routePlanner = null;
		try {
			if (DEBUG > 0) listProxies();
			routePlanner = new ProxySelectorRoutePlanner( 
													   httpclient.getConnectionManager().getSchemeRegistry(),
													   ProxySelector.getDefault());  
			httpclient.setRoutePlanner(routePlanner);
		} catch(Exception rpe) {}
		
    HttpGet httpget = new HttpGet(url);

    if (DEBUG > 0) {
			com.epaperarchives.batchxslt.utils.showMess("getHTTP request URI: " + httpget.getURI() + "\n    encoding: " + enc + "\n    silent: " + silent + "\n","1");
		if (routePlanner != null) com.epaperarchives.batchxslt.utils.showMess("    using proxy route planner " + routePlanner + "\n","1");
			else com.epaperarchives.batchxslt.utils.showMess("    NO proxy found.\n","1");
		}
		
        // Create a response handler
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
		try {
			lastConnectionResponseBody = httpclient.execute(httpget, responseHandler);
		} catch (IOException ioe) {
			lastConnectionMessage = "Unexpected http error: " + ioe.getMessage();
			lastConnectionState = "";
			lastConnectionCode = "-2";
			if (!silent) {
				com.epaperarchives.batchxslt.utils.showMess("Unexpected http error: " + ioe.getMessage() + "\n","1");
				ioe.printStackTrace();
			}
		} finally {
			// When HttpClient instance is no longer needed, 
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpclient.getConnectionManager().shutdown();        
		}
		if (DEBUG > 0) {
			com.epaperarchives.batchxslt.utils.showMess("-------------- response body ----------\n","1");
			com.epaperarchives.batchxslt.utils.showMess(lastConnectionResponseBody,"1");
			com.epaperarchives.batchxslt.utils.showMess("\n----------------------------------------\n","1");
		}

		if (lastConnectionResponseBody == null) return("");
		try {
			if (enc.equals("binary") == true) return(new String(lastConnectionResponseBody));
			else {
				if (enc.equals("") == false) return(new String(lastConnectionResponseBody.getBytes(enc),enc));
				else return(new String(lastConnectionResponseBody));
			}
		} catch (Exception e) {
			lastConnectionMessage = e.getMessage();
			lastConnectionState = "";
			lastConnectionCode = "-2";
			return("");
		}
	
    }
 
	
	
	public static void listProxies() {
		com.epaperarchives.batchxslt.utils.showMess("******** available Proxy servers:\n","1");
        try {
            
            System.setProperty("java.net.useSystemProxies","true");
            List l = ProxySelector.getDefault().select(new URI("http://aiedv.ch"));
            
            for (Iterator iter = l.iterator(); iter.hasNext(); ) {
                
                Proxy proxy = (Proxy) iter.next();
                
                com.epaperarchives.batchxslt.utils.showMess("proxy hostname : " + proxy.type() + "\n","1");
                
                InetSocketAddress addr = (InetSocketAddress) proxy.address();
                
                if(addr == null) com.epaperarchives.batchxslt.utils.showMess("No Proxy\n","1");
                else {
                    com.epaperarchives.batchxslt.utils.showMess("proxy hostname : " + addr.getHostName() + "\n","1");
                    com.epaperarchives.batchxslt.utils.showMess("proxy port : " + addr.getPort() + "\n","1");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	
	
}

