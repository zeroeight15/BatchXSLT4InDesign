
package com.epaperarchives.batchxslt;


import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;







/* ===========================================================
 * implement own EntityResolver
 */
public class myEntityResolver implements EntityResolver {
	public InputSource resolveEntity (String publicId, String systemId)
	{
		if (systemId.equals("http://www.myhost.com/today")) {
			// return a special input source
			return new InputSource("/usr/local/content/localCopyright.xml");
		}
		else {
   	    	// use the default behaviour
			return null;
		}
	}
}

