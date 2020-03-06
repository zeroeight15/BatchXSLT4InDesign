
package com.epaperarchives.batchxslt;

// Imported java classes
import java.awt.*;
import java.io.*;
import java.util.*;
import java.lang.Character;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.*;

import java.util.regex.*;

// Imported TraX classes

import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.apache.xpath.XPathContext;






/* ===========================================================
 * implement own URIResolver
 */
public class myURIResolver implements URIResolver {
	public Source resolve(String href, String basePath) throws TransformerException
	{
		try {
			return new StreamSource("");
		}
		catch(Exception e) {
			return null;
		}
	}
}


