/** @preserve custom.js v46 20200118. (c) aiedv.ch */
/*
===================================
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL THE PRODUCER OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
+  KEEP TRACK OF ALL MODIFICATIONS!                   +
+  IF A NEW VERSION IS INSTALLED, MODIFICATIONS MUST  +
+  MANUALLY BE TRANSFERRED INTO THE NEW VERSION.      +
+  BEST, SAFE A COPY TO A SECURE PLACE!               +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++

THE PRODUCER:
Andreas Imhof
www.aiedv.ch

DEPENDS on loaded:
	flipbook.js

Version: 46
Version date: 20200118
===================================
*/
 
// ******* customizable loader for javascripts and css
//         is called very early from slidebook.js while loading the entire book
var custom_loader = function () {
	var load_CART,
		load_NAVISION,
		commentSystem = "",
		plugin = "",
		localStorVal = "";

	if (_sb_s.modules.issuenav_enable > 0) {
		_sb_fn.loadjscssfile(_sb_s.xslcssPath + "issuenav.js","js");
		_sb_fn.loadjscssfile(_sb_s.xslcssPath + "issuenav.css","css");
	}
	if ((_sb_s.modules.moreinfos_enable > 0) || (_sb_s.modules.moreinfos_enableauto > 0)) {
		_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_moreinfos/moreinfos.js","js");
		_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_moreinfos/moreinfos.css","css");
	}

	/* _easEEdit plugin */
	do {
		// get from parameter override
		plugin = _sb_fn.get_site_param("easeedit");
		//alert("localStorage: " + localStorage);
		localStorVal = localStorage.getItem('_easEEdit');
		//alert("plugin: " + plugin + "\nlocalStorVal: " + localStorVal);
		if ((plugin != null) || (localStorVal === 'true')) {
			if (plugin == '0') {	// turn easEEdit off
				localStorage.removeItem('_easEEdit');
				break;
			}
			//alert("loading easEEdit");
			_sb_s.modules.easeedit_enable = 1;	// mark that we are loding: system has to wait for us
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_easEEdit/easEEdit.js","js");
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_easEEdit/easEEdit.css","css");
			localStorage.setItem('_easEEdit', 'true');
			break;
		}

		if (_sb_s.modules.easeedit_enable > 0) {
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_easEEdit/easEEdit.js","js");
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_easEEdit/easEEdit.css","css");
		}
	} while(false);

	// the NAVISION extension and easECart exclude each other
	// because both act on the article number
	load_CART = _sb_fn.get_site_param("CART");
	load_NAVISION = _sb_fn.get_site_param("NAVISION");
	
	if ((load_NAVISION != null) || (_sb_s.modules.NAVISION_enable > 0)) {
		_sb_s.modules.NAVISION_enable = 1;
		_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_NAVISION/navision.css","css");
		_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_NAVISION/navision.js","js");
		
		// disable all conflicting other modules
		_sb_s.modules.easECart_enable = 0; load_CART = null;
	}

	if ( (load_CART != null) || (_sb_s.modules.easECart_enable > 0) ) {
		_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_easECart/easECart.js","js");
		_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_easECart/easECart_custom.js","js");
	}

	/* commenting systems */
	do {
		// get from parameter override
		commentSystem = _sb_fn.get_site_param("DISQUS");
		if (commentSystem != null) {
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_disqus/disqus.js","js");
			break;
		}
		commentSystem = _sb_fn.get_site_param("INTENSEDEBATE");
		if (commentSystem != null) {
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_intensedebate/intensedebate.css","css");
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_intensedebate/intensedebate.js","js");
			break;
		}
		commentSystem = _sb_fn.get_site_param("LIVEFYRE");
		if (commentSystem != null) {
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_livefyre/livefyre.js","js");
			break;
		}
		// get from fixed settings
		if (_sb_s.modules.disqus_enable > 0) {
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_disqus/disqus.js","js");
			break;
		}

		if (_sb_s.modules.intensedebate_enable > 0) {
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_intensedebate/intensedebate.css","css");
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_intensedebate/intensedebate.js","js");
			break;
		}

		if (_sb_s.modules.livefyre_enable > 0) {
			_sb_fn.loadjscssfile(_sb_s.xslcssPath + "_livefyre/livefyre.js","js");
			break;
		}
	} while(false);
},

SetCookie = function (cookieName,cookieValue,nDays) {
	try {
		var today = new Date(),
			expire = new Date();
		if (nDays==null || nDays==0) nDays=1;
		expire.setTime(today.getTime() + 3600000*24*nDays);
		document.cookie = cookieName+"="+escape(cookieValue) + ";expires="+expire.toGMTString();
	} catch(ex){}
},
ReadCookie = function (cookieName) {
	try {
		var theCookie=" "+document.cookie, 
			ind=theCookie.indexOf(" "+cookieName+"="),
			ind1;
		if (ind==-1) ind=theCookie.indexOf(";"+cookieName+"=");
		if (ind==-1 || cookieName=="") return "";
		ind1=theCookie.indexOf(";",ind+1);
		if (ind1==-1) ind1=theCookie.length; 
		return unescape(theCookie.substring(ind+cookieName.length+2,ind1));
	} catch(ex){ return ""; }
},

detMimetype = function (filename,mediatype) {
	if (filename == "") return("");
	var fn = filename.toLowerCase(),
		ext = fn.split("."),
		mt = "";
	ext = ext[ext.length-1];
	if (typeof(mediatype) != 'undefined') mt = mediatype;	// must be 'v' or 'a'
	if (mt == "") {	// detect from file name
		if (fn.indexOf("video") >= 0) mt = "v";
		else mt = "a";
	}
	// pre scan special media file which can be audio and video media type
	switch (ext) {
		case "mp4":
			if (mt == "v") ext = "mp4_VIDEO";
			else  ext = "mp4_AUDIO";
			break;
		case "webm":
			if (mt == "v") ext = "webm_VIDEO";
			else  ext = "webm_AUDIO";
			break;
	}
	// get the actual media type
	switch (ext) {
		// AUDIO file extensions
		case "mp3": 
		case "mp2": 
		case "mp1": 
		case "mpg": 
		case "mpeg": 
			 return("audio/mpeg");
		case "mp4_AUDIO": // also may be video
		case "m4a": 
			 return("audio/mp4");
		case "ogg": 
		case "oga": 
			 return("audio/ogg");
		case "webm_AUDIO":  // also may be video
			 return("audio/webm");
		case "wav": 
			 return("audio/x-wav");	// also audio/wav
		case "aac": 
			 return("audio/x-aac");	// also audio/aac (Safari only?)

		// VIDEO file extensions
		case "mp4_VIDEO": // also may be audio
		case "m4v": 
			 return("video/mp4");
		case "ogv": 
			 return("video/ogg");
		case "webm_VIDEO":  // also may be audio
			 return("video/webm");
		case "swf": 
			 return("application/x-shockwave-flash");
		case "flv": 
			 return("video/x-flv");
	}
	return("");
},


/*****************
 * customizable function to modify article text display
 * The functions below are called immediately before a clicked article is shown in the lightbox.
 */

replaceURLWithHTMLLinks = function (inputText) {
	var replacedText, replacePattern1, replacePattern2;

	//URLs starting with http://, https://, or ftp:// but don't replace existing full link tags
	replacePattern1 = /((^|[^"])(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
	replacedText = inputText.replace(replacePattern1, '<a class="wwwLink" href="$1" target="_blank">$1</a>');

	//URLs starting with www. (without // before it, or it'd re-link the ones done above)
	replacePattern2 = /(^|[^\/])(www\.[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
	replacedText = replacedText.replace(replacePattern2, '$1<a class="wwwLink" href="http://$2" target="_blank">$2</a>');

	//Change email addresses to mailto: links
	// official email match:  [a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?
	// ALSO replaces email addresses already linked !!
	// --------- currently email not handled
	return replacedText;
},


// ******* suppress unwanted character attributes set manually from <span
suppress_character_attribs = function (thestr) {
	var suppress = "", supp = 0, str;
	suppress = _sb_fn.get_moreDocumentInfo("characterAttribsSuppress");
	if ((suppress == "0") || (suppress == "")) return(thestr);
				// 0 = leave attribs as is
				// 1 = remove font-family
				// 2 = remove font-size
	try { supp = parseInt(suppress,10); } catch(e) { return(thestr); }
	if (supp == 0) return(thestr);
	str = thestr;
	// nothing removed so far. add code here
	return(str);
},

// ******* redirect certain oscommerce links
num_cart_strs = 2,
cart_strs = new Array(num_cart_strs);
for (var i=0; i<num_cart_strs; i++) { cart_strs[i]=new Array(5); }	//prepare sub-arrays for 5 languages
	cart_strs[0][0]="The product has been sent to the cart: ";
	cart_strs[0][1]="Das Produkt wurde in den Warenkorb gelegt: ";
	cart_strs[0][2]="Le produit a été placé dans le panier: ";
	cart_strs[0][3]="The product has been sent to the cart: ";
	cart_strs[0][4]="The product has been sent to the cart: ";
	cart_strs[1][0]="\n\nNo - not really - you will not be charged!\nJust a simulation of a shop connection.";
	cart_strs[1][1]="\n\nNein - nicht wirklich - es wird nichts belastet!\nDies ist nur eine simulation eines Shop-Links.";
	cart_strs[1][2]="\n\nNon - pas du tout - votre compte n'est pas chargé!\nC'est seulemnet une simulation d'un lien vers un shop.";
	cart_strs[1][3]="\n\nNo - not really - you will not be charged!\nJust a simulation of a shop connection.";
	cart_strs[1][4]="\n\nNo - not really - you will not be charged!\nJust a simulation of a shop connection.";
var sendToCart = function (url) {	// url usually is the original url's parameters parts only. like: ?partnum=12345&blabla
	// get part number
	var params = url.split("&"),
		partnum = params[0].split("=")[1];
	alert(cart_strs[0][_sb_s.cur_lang_ID] + partnum + cart_strs[1][_sb_s.cur_lang_ID]);
	return(false);	// prevent 'default' link behaviour
},

redirect_html_links = function (thestr) {
	var str, re_lnk = null, a_link_re, i = 0, a_link, source_string, re, pos, href_re, href, url_re, url, new_url;
					// modify script below to match needs
	if ((thestr == null) || (thestr == "")) return(thestr);
	if (thestr.toLowerCase().indexOf("<a ") < 0) return(thestr);	// MUST BE LOWER CASE! because IE translates all tag names tu UPPER CASE!
	str = thestr;

	try {	// old IE 6 cant understand this
		re_lnk = new RegExp("<a (.)*?\>","gi");		// get html <a href.....> construct
	}
	catch (e) { return (str); }
	a_link_re = re_lnk.exec(str);

	i = 0;		// just a security counter if the regexp can not replace some signatures
	while ((a_link_re != null) && (a_link_re[0] != "")) {
		a_link = "" + a_link_re[0];	// the whole <a...> begin link string
		// assume an original hyper link like this:
		// <a href="http://www.mydomain.com/shop/advanced_search_result.php?keywords=370006&x=0&y=0">....</a>
		// or 
		// <a href="http://www.myshop/shopware.php/sViewport,checkout?id=370006">....</a>
		source_string = "myshop/shopware.php/sViewport,checkout";	// a part in the URL to replace (this example is for shopware)
		re = new RegExp(source_string,"gi");
		pos = a_link.search(re);
		//alert(a_link + "\n\n" + source_string + "\n\n" + pos);
		if (pos >= 0) {
			re = /href="(.)*?"/;	// get the href attribute
			href_re = re.exec(a_link);
			href = "" + href_re[0];		// the whole href attribute: we will send this 

			re = /"(.)*"/;				// get the url in the href attribute
			url_re = re.exec(href);		// the url only
			url = "" + url_re[0];		// ... as string
			re = /"/g;					// remove double quotes
			url = url.replace(re,"");
			
			// make href regex aware
			href = href.replace(/\?/gi,"\\?");
			
			new_url = "javascript:sendToCart('" + url.substr(url.indexOf("?")+1) + "');";	// new action with filtered url parameters after ?
			re = new RegExp(href,"gi");	// replace entire a href="link" attribute

			str = str.replace(re,"href=\"#\" onclick=\"" + new_url + "\"");
			//alert("a_link: " + a_link + "\n\nurl: " + url + "\n\npos: " + pos + "\n\norig href: " + href + "\n\nnew_url: " + new_url + "\n\nindex: " + str.indexOf(href) + "\n\nstr\n: " + str);
		}
		// find next html link construct
		if (i > 1000) break;
		a_link_re = re_lnk.exec(str);
		i++;
	}
	return (str);
},



/**
 * retarget_links
 * re-target html links
 * check, if links not pointing to this host address should be openened in new window (_target="_blank")
 */
open_url_in_new_window = "",	// set to someting like ".mydomain.com"
retarget_links = function (thestr,internalURL) {
	var str, re_lnk = null, a_link, a_link_re, i, source_string, replace_string, rel;
	if ((internalURL == null) || (internalURL =="")) return(thestr);

	if ((thestr == null) || (thestr == "")) return(thestr);
	if (thestr.toLowerCase().indexOf("<a ") < 0) return(thestr);	// MUST BE LOWER CASE! because IE translates all tag names to UPPER CASE!
	str = thestr;

	try {	// old IE 6 can't understand this
		re_lnk = new RegExp("<a (.)*?\>","gi");		// get html <a .....> link construct
	}
	catch (e) { return (str); }
	a_link_re = str.match(re_lnk);	// get all <a links

	i = 0;		// just a security counter if the regexp can not replace some signatures
	while ((a_link_re != null) && (i < a_link_re.length) && (a_link_re[i] != "")) {
		a_link = "" + a_link_re[i];	// the whole <a ...> begin link string
		//alert("link #"+i + " of " + a_link_re.length + "\n\n" + a_link);
		if (a_link_re[i].indexOf("target=") > 0) {
			i++;		// do nothing if already contains a target
			continue;
		}
		if (a_link_re[i].indexOf("mailto:") > 0) {
			i++;		// do nothing if email link
			continue;
		}

		source_string = "<a ";
		replace_string = "<a target=\"_blank\" ";	// set to '_top' or '_blank' or any window name
		if (a_link_re[i].toLowerCase().indexOf(internalURL) > 0) {	// we want internal inks to show in same window _top
			replace_string = "<a target=\"_top\" ";	// set to '_top' or '_blank' or any window name
		}

		rel = new RegExp(source_string,"gi");
		a_link_re[i] = a_link_re[i].replace(rel,replace_string);
		a_link = a_link.replace("\?","\\?");	// escape question mark in source link
		//alert("new link #"+i + " of " + a_link_re.length + "\nlink:\n" + a_link + "\nnew link:\n" + a_link_re[i]);
		rel = new RegExp(a_link,"gi");
		str = str.replace(rel,a_link_re[i]);

		// process next html link construct
		if (i > 1000) break;
		i++;
	}
	return (str);
},

// ******* clean out some unwanted text or turn to what ever is needed
clean_content = function (thestr, clickedArticleID, clickedID, clickedPage) {
	var re,
		content = thestr,
		pos, i,
		re_label,		// get data-label="dirlink:http://www.domain.com" string
		datalabels = null,
		href = "", onclick ="", script = "",
		inNewWindow = false,
		contentDom,
		destinationArticleID, pageIdx,
		elems, l,
		target, key, val, equpos;

	// begin cleaning content
	re = / \r\n/gi;
	content = content.replace(re," ");
	re = / \r/gi;
	content = content.replace(re," ");
	re = / \n/gi;
	content = content.replace(re," ");
	//re = / \<br\>/gi;
	//content = content.replace(re," ");
	re = /-\<br\>/gi;
	content = content.replace(re,"");

	// remove leading 'empty' divs created by 
	// like <div class="P_NormalParagraphStyle" data-fontsize="12">&nbsp;</div>
	re = /[\n|\r]+\<div .*?\>&nbsp;\<\/div\>/;
	do {
		pos = content.search(re);
		if (pos != 0) break;	// ON POS 0 ONLY
		content = content.replace(re,"");
	} while(true);

	// kick out manually set font sizes
	// uncomment to kick manually set font-size, font-family, white color
	re = /([^-]color:)#FFFFFF;/gi;	// white to gray for font color: but not for background-color:
	content = content.replace(re,"$1#777;");
		// the same for IE (converts color numbers to rgb(...)
	re = /([^-]color:).?rgb\(255.*?255.*?255\);/gi;	// white to gray for font color: but not for background-color:
	content = content.replace(re,"$1#777;");
	re = /([^-]color:)transparent;/gi;	// transparent to white for font color: but not for background-color:
	content = content.replace(re,"$1#777;");
	re = /font-size:(.)*?pt;/g;
	content = content.replace(re,"");
	re = /font-family:(.)*?;/g;
	content = content.replace(re,"");

	re = /text-align:right;/g;
	content = content.replace(re,"");

 
	// remove empty style attributes
	re = / style="."/g;
	content = content.replace(re,"");

	// ------------
	// special characters from non-Unicode aware fonts
	re = /\uF044/g;	// EF8184	hand left
	content = content.replace(re,"<span style=\"font-size:1.5em;\">\u261C</span>");
	re = /\uF045/g;	// EF8185	hand right
	content = content.replace(re,"<span style=\"font-size:1.5em;\">\u261E</span>");
	re = /\uF04D/g;	// EF818D	hand down
	content = content.replace(re,"<span style=\"font-size:1.5em;\">\u261F</span>");

	// ------------
	// replace/modify existing html links and other stuff
		// ******** UN-comment to enable
	//content = redirect_html_links(content);

	// ------------
	// open, retarget external links in top window
	if (open_url_in_new_window != "") content = retarget_links(content,open_url_in_new_window);

	// ------------
	// suppress unwanted character attributes set manually
	try { content = suppress_character_attribs(content); } catch(e) {}
	
	// ------------
	// check if we have a direct link to open: content not to show in lightbox
	// means: if we find a tag like:
	// <div class="Artcl_container" ..... data-label="dirlink:http://www.bmw.ch">
	// whose data-label attribute contains the 'dirlink:' key, we open the link in same window
	// or the 'dirlinkn:' key, we open the link in NEW window.
	// or the 'dirlinki' keyword, we jump to an article
	// We extract the dirlink:value which is the link and open it
	try {
		_sb_fn.data_labels_clear();
		if (content.indexOf(" data-label=\"") >= 0) {
			re_label = new RegExp(" data\-label=\"(.*?)\"","ig");		// get data-label="dirlink:http://www.domain.com" string
			inNewWindow = false;

			while ((datalabels = re_label.exec(content)) !== null) {
				//alert("datalabels: " + datalabels[1]);	
				_sb_fn.data_labels_store(datalabels[1]);	// get the part in regex paranthesis
			}
			
			// if we have set data-label
			if (_sb_fn.data_labels_length() > 0) {

				if ((_sb_fn.data_labels_get("dirlinkn:") !== false) || (_sb_fn.data_labels_get("dirlink:") !== false)) {
					// label is like: data-label="dirlinkn:http://www.domain.com/index.html#id=start"
					// label is like: data-label="dirlink:http://www.domain.com/index.html#id=start##"
	
					// get the actual url
					href = _sb_fn.data_labels_get("dirlinkn:");		// get data-label="dirlinkn:http://www.domain.com" string
					if ((href !== false) && (href != "")) {			// link to open in NEW (_blank) window
						//alert("href new: " + href);
						inNewWindow = true;
					}
					else {	// link to open in same (_top) window
						href = _sb_fn.data_labels_get("dirlink:");
						inNewWindow = false;
					}

					if ((href !== false) && (href !== "")) {
						if (navigator.userAgent.indexOf("MSIE 1") != -1) inNewWindow = false; //	MS IE 10 always open in same window
						if (!inNewWindow) {
								//$.fn.log("log","hrefraw: " + hrefraw + "\n\tcurrent PageIdx: " + _sb_s.currentPageIdx + "\n\tnavigator.userAgent:\n\t" + navigator.userAgent);
							SetCookie("dirlinknReturnPageIdx",""+_sb_s.currentPageIdx,1);
						}
					
						href = href.split("##")[0];	// might end with two hashes
						if (href != "") {
							if (inNewWindow) {
								// must start with a protocol http:// or https://
								if ((href.indexOf("://") < 0) && (href.indexOf("www.") == 0)) href = "http://" + href;
								else {	// is a url relative to current page: get full url/path
								  if (href.indexOf("://") < 0) {  // no protocol
									  var baseurl = window.location.href.substr(0,window.location.href.lastIndexOf("/")+1);
									  href = baseurl + href;
									}
								}
								//alert("Ext link: " + href);
								window.open(href,"extdirlink");
							}
							else {
								//alert("Same Window link: " + href);
								this.location.href = href;
							}
							return("");	// we clear the content for the lightbox is not opened
						}
					}
				}

				// check for direct internal text or page destination links
				//alert ("dirlinki is set: " + _sb_fn.data_labels_isset("dirlinki"));
				if (_sb_fn.data_labels_isset("dirlinki")) {
					contentDom = document.createElement('div');

					// dirlinki
					// can be:
					// dirlinki					--> without param: goto an InDesign page target
					// dirlinki:p=4				--> goto page index 4
					// dirlinki:P=abc			--> got page name
					// dirlinki:s=text			--> search for text and show best match
					// dirlinki:S=text			--> search for text and show results if more than one match
					// dirlinki:l=labeltext		--> goto article labeled like this
					
					do {
						target = _sb_fn.data_labels_get("dirlinki");
						// dirlinki					--> without param: goto an InDesign page target
						//alert ("dirlinki target: '" + target + "'");
						if (target == "") {
							contentDom.innerHTML = content;
							elems = contentDom.getElementsByTagName("a");
							//alert(content);
							if (elems.length > 0) {
								for (l = 0; l < elems.length; l++) {
									try {
										// prevent lightbox open and directly jump to a page
										// onclick="closeLightbox();return goto_page('2',false,null,null,true);" href="javascript:void(0);"
										onclick = elems[l].getAttribute("onclick");
										if (onclick && (onclick.indexOf("goto_page") >= 0)) {
											//alert("found onclick=\"" + onclick + "\"" + "\n"+closeLightbox);
											//onclick = onclick.replace(/return/gi,"");	// eval() does not like 'return' statement
											onclick = "(function(){" + onclick + "})()";	//wrap into a function
											eval(onclick);
											contentDom = null;
											return("");	// we clear the content for the lightbox is not opened
										}
										
										// the href is already turned by flipbook.js to
										//  javascript:show_article(null,'Art7_4','','','','','','');goto_page(3,true,null,null,true);
										// or
										// javascript:goto_page('5',false);closeLightbox();
										href = elems[l].getAttribute("href");
										//alert("href: " + href);
									//	if (!href && !destinationArticleID) continue;
										if (href && (href.indexOf("goto_page") >= 0)) {
											// found href="javascript:goto_page('5',false);closeLightbox();"
											href = href.replace(/javascript:/gi,"");
											eval(href);
											contentDom = null;
											return("");	// we clear the content for the lightbox is not opened
										}
										
										destinationArticleID = elems[l].getAttribute("data-destinationArticleID");	// like data-destinationArticleID="Art7_2"
										//alert("destinationArticleID: " + destinationArticleID);
										if (destinationArticleID && (destinationArticleID.indexOf("Art") == 0)) {
											// found data-destinationArticleID="Art7_2"
											pageIdx = _sb_s.funcs.getPageIDXFromArticleID(destinationArticleID);
											show_article(null,destinationArticleID,"",true);
											goto_page(pageIdx,true,null,null,true);
											contentDom = null;
											return("");	// we clear the content for the lightbox is not opened
										}
									} catch(e){}
								}
							}
							contentDom = null;
							break;
						}

						// dirlinki:xxx					--> with param
						target = _sb_fn.data_labels_get("dirlinki:");
						//alert ("dirlinki: target: '" + target + "'");
						// dirlinki:p=4				--> goto page index 4
						// dirlinki:P=abc			--> got page name
						// dirlinki:s=text			--> search for text and show best match
						// dirlinki:S=text			--> search for text and show results if more than one match
						// dirlinki:l=labeltext		--> got article labeled like this
						
						// split the key value pair
						equpos = target.indexOf("=");
						key = target.substr(0, equpos);
						val = target.substr(equpos + 1);
						switch(key) {
							case "p":
								_sb_fn.gotoPage(val, true);
								return("");	// we clear the content for the lightbox is not opened
								break;
							case "P":
								_sb_fn.gotoPage(val, false);
								return("");	// we clear the content for the lightbox is not opened
								break;
							case "s":	// show best match
								//alert ("search: '" + val + "'");
								goto_searchterm(val, true, true);
								return("");	// we clear the content for the lightbox is not opened
								break;
							case "S":
								//alert ("SEARCH: '" + val + "'");
								goto_searchterm(val, true, false);
								return("");	// we clear the content for the lightbox is not opened
								break;
							case "l":
								_sb_fn.showLabeledArticle(val);
								return("");	// we clear the content for the lightbox is not opened
								break;
						}

					} while (false);
					contentDom = null;
				}


				// check for starting a script
				if (_sb_fn.data_labels_isset("script:")) {
					script = _sb_fn.data_labels_get("script:");
					re = /&quot;/g;
					script = script.replace(re,"\"");
					//alert("script to run: " + script);
					
					if (script != "") eval(script);
				}

			}
		}
	} catch (e) {}

	// ------------
	// manipulate img tags #1
	// In special cases, we want to completely change the content. Ex: When clicked on a ("more info") image, other content should be shown
	if (_sb_s.modules.moreinfos_enable > 0) {
		if (content.toLowerCase().indexOf("<img ") >= 0) {	// lower case needed for IE<=8
			do {
				if (content.toLowerCase().indexOf("_infosextra.jpg") < 0) break;
				// first show the sprocket in the lightbox
				//var new_content = "<div style=\"width:100%;margin-top:30%;text-align:center;\"><img src=\"" + _sb_s.xslcssPath + "sprocket.gif\"></div>";
				//showContent(new_content,"text");
		
				// extract the page number from
				//	<div class="Artcl_container" data-self="u1673" data-pg="3" data-pn="1" data-aid="10">
				var pn_numparts, pn_num, pn_folder, data_pnArr;
				re = new RegExp("data-pn=\".*?\"","i");	// get data-pn="1" attribute
				data_pnArr = content.match(re);
				if (data_pnArr == null) break;
				pn_numparts = data_pnArr[0].split("=\"");	// get page number from data-pn="1" attribute
				pn_num = pn_numparts[1].replace("\"","");
				pn_folder = "" + pn_num;
				while (pn_folder.length < 4) pn_folder = "0" + pn_folder;	// make 4 digits 000#
				pn_folder = "_P" + pn_folder;
				//alert("pn_num: " + pn_num + "\npn_folder: " + pn_folder);
			
				// get new content of extra/more infos from '_info' folder
				sb_moreinfoGet(pn_folder, pn_num);
				return("");	// we clear the content for the lightbox is not opened
			} while(false);
		}
	}

	// ------------
	// manipulate img tags #2
	// resize image tag when image is larger than Lightbox width
	// add an onclick to show original image in a new window
	// add onload handler to control the size
	if (content.toLowerCase().indexOf("<img ") >= 0) {	// lower case needed for IE<=8
		re = /title=\"\"/gi;	// remove empty title tags
		content = content.replace(re,"");

		if (_sb_s.shrinkImageToLightbox > 0) {
			// resize image tag when image is larger than Lightbox width
			// add onload handler to control the size
			// extract all src name attribs
			re = new RegExp(" src=\".*?\"","gi");		// get all src="imagename.jpg" strings
			var a_srcnames = null, srcname, handler;
			while ((a_srcnames = re.exec(content)) && (a_srcnames.length > 0)) {
				i = 0;
				while ((typeof(a_srcnames[i]) != "undefined") && (a_srcnames[i] != "") && (a_srcnames[i] != "g")) {
					srcname = ""+a_srcnames[i];
					srcname = srcname.substring(6,srcname.length-1);	// trim attribute name 'src' and double quotes
					//alert(i + " srcname: " + srcname);
					if (srcname && (srcname.indexOf("/XSLCSS/") < 0)) {	// exclude control images like arr-left.png, arr-right.png from XSLCSS folder
						//alert("length: " + a_srcnames.length+"\ni: " + i + "\n" + srcname);
						// we now replace/enhance the image src name with further attributes
						// original: <img width="632" alt="UI_18580710_N0008_0057_Picture2.jpg" src="UI_18580710_N0008_0057_Picture2.jpg">
						// new: <img width="632" alt="UI_18580710_N0008_0057_Picture2.jpg" src="UI_18580710_N0008_0057_Picture2.jpg" onload=".." onclick="..">
						handler = srcname + "\"";
						handler += " onload=\"if(this.width>_sb_s.lightboxWidth) this.width=_sb_s.lightboxWidth-20;\" ";
						content = content.replace(srcname+"\"",handler);
						re.lastIndex += handler.length;
					}
					i++;
				}
			}
		}

		if (_sb_s.enlargeImageIcon > 0) {
			// if the image has a second larger image
			// add a resize icon
			re = new RegExp("<img (.)*?[\"| ]>","gi");		// get all <img ...> strings
			var a_img = null, img, handle, reg;
			while ((a_img = re.exec(content)) && (a_img.length > 0)) {
				i = 0;
				while ((typeof(a_img[i]) != "undefined") && (a_img[i] != "") && (a_img[i] != "g")) {
					img = ""+a_img[i];
					if (img && (img.indexOf("javascript:showImage") >= 0)) {	// must have the onclick event
						// we now add a second image tag for the 'enlarge'-icon
						handle = img;
						handle = handle.replace("<img","<img class=\"imageEnlargeIcon\"");
						reg = /style=\"(.)*?\"/i; handle = handle.replace(reg,"");
						reg = /width=\"(.)*?\"/i; handle = handle.replace(reg,"");
						reg = /height=\"(.)*?\"/i; handle = handle.replace(reg,"");
						reg = /title=\"(.)*?\"/i; handle = handle.replace(reg,"title=\"" + _sb_s.lg[37][_sb_s.cur_lang_ID] + "\"");
						reg = /onload=\"(.)*?\"/i; handle = handle.replace(reg,"");
						reg = /src=\"(.)*?\"/i; handle = handle.replace(reg,"src=\"" + _sb_s.xslcssPath + "enlarge.png\"");
						//alert("img lastIndex: " + re.lastIndex + "\n"+img + "\n\nhandle:\n"+handle);
						re.lastIndex += handle.length;
						handle = img+handle;
						content = content.replace(img,handle);
					}
					i++;
				}
			}
		}
	}

	//=============================
	//=========== more custom stuff to go here

	return(content);
},




/*****************
 * customizable function for feature pop up. see Info Button in top-right corner 
 * The function below are called after the flip book is loaded.
 */

num_feattext_items=12,	//number of language dependent string array elements
feat=new Array(num_feattext_items);
for (i=0; i<num_feattext_items; i++) { feat[i]=new Array(5); }	//prepare sub-arrays for 5 languages

feat[0][0]="Navigation Help&nbsp;&nbsp;&raquo;";
feat[0][1]="Navigationshilfe&nbsp;&nbsp;&raquo;";
feat[0][2]="Instructions de navigation&nbsp;&nbsp;&raquo;";
feat[0][3]="Navigation Help&nbsp;&nbsp;&raquo;";
feat[0][4]="Navigation Help&nbsp;&nbsp;&raquo;";

feat[1][0]="Book Scale is Enabled";
feat[1][1]="Buchskalierung ist eingeschaltet";
feat[1][2]="Redimensionnement du livre est activé";
feat[1][3]="Book Scale is Enabled";
feat[1][4]="Book Scale is Enabled";

feat[2][0]="Book Scale is Disabled";
feat[2][1]="Buchskalierung ist ausgeschaltet";
feat[2][2]="Redimensionnement du livre désactivé";
feat[2][3]="Book Scale is Disabled";
feat[2][4]="Book Scale is Disabled";

feat[3][0]="ON";
feat[3][1]="EIN";
feat[3][2]="Activer";
feat[3][3]="ON";
feat[3][4]="ON";

feat[4][0]="OFF";
feat[4][1]="AUS";
feat[4][2]="Désactiver";
feat[4][3]="OFF";
feat[4][4]="OFF";

feat[5][0]="Page turn mode is 'TURN'";
feat[5][1]="Blättermodus ist 'BLÄTTERN'";
feat[5][2]="Mode de feuilleter est 'PLIER'";
feat[5][3]="Page turn mode is 'TURN'";
feat[5][4]="Page turn mode is 'TURN'";

feat[6][0]="Page turn mode is 'SLIDE'";
feat[6][1]="Blättermodus ist 'SCHIEBEN'";
feat[6][2]="Mode de feuilleter est 'GLISSER'";
feat[6][3]="Page turn mode is 'SLIDE'";
feat[6][4]="Page turn mode is 'SLIDE'";

feat[7][0]="TURN";
feat[7][1]="BLÄTTERN";
feat[7][2]="PLIER";
feat[7][3]="TURN";
feat[7][4]="TURN";

feat[8][0]="SLIDE";
feat[8][1]="SCHIEBEN";
feat[8][2]="GLISSER";
feat[8][3]="SLIDE";
feat[8][4]="SLIDE";

feat[9][0]="Back";
feat[9][1]="Zurück";
feat[9][2]="Retour";
feat[9][3]="Back";
feat[9][4]="Back";

var features_container = null,
features_opener = null,

// ******* set up the features popup content
init_features_popup = function () {
	// check if we actually want the features popup
	var fc, onevent, target,
		separator = false,
		features_pop = _sb_fn.get_site_param("nofeatures");
	if (features_pop != null) {
		document.getElementById('features_container').style.display = "none";
		return;
	}
	features_pop = _sb_fn.get_site_param("features");
	if (features_pop == null) {
		features_pop = _sb_fn.get_css_value("show_features_pop","zIndex");
		if ((typeof features_pop != "undefined") && (features_pop != "")) {	// found in css
			if (parseInt(features_pop,10) != 1) {
				document.getElementById('features_container').style.display = "none";
				return;
			}
		}
	}

	// now the popup content
	do {
		features_container = null;
		try {
			features_container = document.getElementById('features_container');
		} catch (e) {}
		if (features_container == null) break;

		features_container.style.display = 'block';

		onevent = "click";
		if (_sb_s.isTouchDevice) onevent = _sb_s.pointerEvents.end;

		// insert the open/close button
		fc = "<div id=\"features_opener\" class=\"features_opener\"></div>";
		// insert content
		fc += "<div class=\"features_hint\">Quick Links</div>";
		fc += "<div class=\"features_content\">";
			fc += "<div class=\"features_button features_navhelp cursor_pointer\" on" + onevent + "=\"get_sb_nav_helptext();return(false);\">" + feat[0][_sb_s.cur_lang_ID] + "</div>";
			fc += "<div class=\"features_spacer\"></div>";

				fc += "<div class=\"features_button_cont\">";
					fc += "<div class=\"features_scalebuttons_cont\">";
						if (!_sb_s.isTouchDevice) {	// suppress scale buttons for certain devices
							// book scaling
							fc += "<div id=\"bookscale_state\" style=\"text-align:center;\">&nbsp;</div>";
							fc += "<div id=\"features_button_on\" on" + onevent + "=\"_sb_fn.enableAdjustPageImageSize('btnEnable',3);featuresPop_setHooks(this);\" title=\"" + feat[3][_sb_s.cur_lang_ID] + "\" >&nbsp;</div>";
							fc += "<div id=\"features_button_off\" on" + onevent + "=\"_sb_fn.disableAdjustPageImageSize();featuresPop_setHooks(this);\" title=\"" + feat[4][_sb_s.cur_lang_ID] + "\" >&nbsp;</div>";
							separator = true;
						}
					fc += "</div>";
					if (!(_sb_s.is_IE && (_sb_s.IEVersion <= 8))
						&& !(_sb_s.is_Android && (_sb_s.AndroidVersion < _sb_s.AndroidVersionMin))) {
						// page turn mode
						fc += "<div id=\"pageturnmode\" style=\"text-align:center;\">&nbsp;</div>";
						fc += "<div id=\"pageturn_turn\" on" + onevent + "=\"newURL('turn')\" title=\"" + feat[7][_sb_s.cur_lang_ID] + "\" >&nbsp;</div>";
						fc += "<div id=\"pageturn_slide\" on" + onevent + "=\"newURL('slide')\" title=\"" + feat[8][_sb_s.cur_lang_ID] + "\" >&nbsp;</div>";
						//fc += "<div id=\"pageturn_turn\" on" + onevent + "=\"$.fn.loadFlipbook('turn');\" title=\"" + feat[7][_sb_s.cur_lang_ID] + "\" >&nbsp;</div>";
						//fc += "<div id=\"pageturn_slide\" on" + onevent + "=\"$.fn.loadFlipbook('slide')\" title=\"" + feat[8][_sb_s.cur_lang_ID] + "\" >&nbsp;</div>";
						separator = true;
					}
				fc += "</div>";
		fc += "</div>";
		features_container.innerHTML = fc;

		features_opener = null;
		try {
			features_opener = document.getElementById('features_opener');
		} catch (e) {}
		if (features_opener == null) break;
		_sb_fn.addEventHandler(features_opener,
											onevent,
											function(e) {
													target = (e.currentTarget ? e.currentTarget : e.srcElement);
													//alert(target.id);
													if (target.id != "features_opener") return(true);

													if (!_sb_fn.hasClass(features_container,'features_container_hover')) {	// open features pop
														_sb_fn.addClass(features_container,'features_container_hover');
														_sb_fn.addClass(features_opener,'features_opener_hover');
													}
													else {	// close features pop
														_sb_fn.removeClass(features_container,'features_container_hover');
														_sb_fn.removeClass(features_opener,'features_opener_hover');
													}
													return(true);	// allow propagation
												});

		// add hooks
		setTimeout("featuresPop_setHooks()",50);
	} while(false);
	return;
},
newURL = function (param) {
	//alert(window.location.protocol+"\n"+window.location.hostname+"\n"+window.location.pathname);
	var url = window.location.protocol+'//'+window.location.hostname + window.location.pathname+(param != "" ? "?" + param : "");
	window.location.href = url;	// get it
	return(url);
},

sb_helptext = "",
get_sb_nav_helptext = function () {
	var pathParts, basePath, numFolders, sb_helptext_file,
		sb_plainXSLCSS = "", sb_helptext_fldr = "", sb_helptext_filepath = "";
	if (sb_helptext == "") {
		// extract the non relative path of the XSLCSS folder
		sb_plainXSLCSS = _sb_s.xslcssPath;
		numFolders = 0;
		while (sb_plainXSLCSS.indexOf("../") >= 0) {	// remove relative paths from ../../../XSLCSS/
			numFolders++;
			sb_plainXSLCSS = sb_plainXSLCSS.substr(3);
		}
		// extract the URL part down to the XSLCSS folder
		//$.fn.log("log","location.href: " + document.location.href);
		pathParts = document.location.href.split("/");
		pathParts.length -= numFolders + 1;
		basePath = pathParts.join("/") + "/";
		sb_helptext_fldr = basePath + sb_plainXSLCSS + "_help/sb_navigation/";
		sb_helptext_file = "nav_helpsb_alone_" + _sb_s.cur_lang + ".html";
		sb_helptext_filepath = sb_helptext_fldr + sb_helptext_file;

		// load the CSS for navigation help text
		_sb_fn.loadjscssfile(sb_helptext_fldr + "website/css/nav_helpsb_alone.css","css","all");
		//$.fn.log("log","cur_lang: " + cur_lang + "\n\t_sb_s.xslcssPath: " + _sb_s.xslcssPath + "\n\tsb_helptext_filepath: " + sb_helptext_filepath);
		new _sb_fn.xmlHttpRequest('GET', sb_helptext_filepath, 'text/html', sb_helpHandler, 'gethelptext');
	}
	else {	// show in lightbox
		_sb_fn.showLightbox(null,sb_helptext,"text");
	}
	

	return(false);
},
sb_helpHandler = function (what,respStatus,response) {
	//$.fn.log("log","NAVrequestHandler what: " + what + "\n\tresponse: " + response);
	switch(what) {
		case "gethelptext":
			sb_helptext = response;
			var re = / src=\"/gi;	// modify the src path to images
			sb_helptext = sb_helptext.replace(re," src=\""+_sb_s.xslcssPath);
			_sb_fn.showLightbox(null,sb_helptext,"text");
			break;
		case "ERROR_gethelptext":
				//$.fn.log("log","ERROR_gethelptext: " + response);
			break;
	}
},

featuresPop_setHooks = function (elem) {
	var scaleinfodiv, pageturnmode, element;
	try {
		// book scaling
		scaleinfodiv = document.getElementById('bookscale_state');
		if (scaleinfodiv) {
			if (_sb_s.pageAdjustToWindow != 0) {	// is on
				element = elem;
				scaleinfodiv.innerHTML = feat[1][_sb_s.cur_lang_ID];

				if (!element) element = document.getElementById("features_button_on");
				element.style.opacity = 0.5;
				element = document.getElementById("features_button_off");
				element.style.opacity = 1;
			}
			else {	// is off
				scaleinfodiv.innerHTML = feat[2][_sb_s.cur_lang_ID];

				if (!element) element = document.getElementById("features_button_on");
				element.style.opacity = 1;
				element = document.getElementById("features_button_off");
				element.style.opacity = 0.5;
			}
		}
	} catch(e) {}
	try {
		// page turn mode
		pageturnmode = document.getElementById('pageturnmode');
		if (pageturnmode) {
			if (_sb_s.pageTurnMode == 'turn') {	// is is turn mode
				element = elem;
				pageturnmode.innerHTML = feat[5][_sb_s.cur_lang_ID];

				if (!element) element = document.getElementById("pageturn_turn");
				element.style.opacity = 0.5;
				element = document.getElementById("pageturn_slide");
				element.style.opacity = 1;
			}
			else {	// is off
				pageturnmode.innerHTML = feat[6][_sb_s.cur_lang_ID];

				if (!element) element = document.getElementById("pageturn_turn");
				element.style.opacity = 1;
				element = document.getElementById("pageturn_slide");
				element.style.opacity = 0.5;
			}
		}
	} catch(e) {}
},


trim = function (str) {
	return str.replace(/^\s+/,'').replace(/\s+$/,'')
},
trimZeroes = function (s) {
	return s.replace(/^0+/, '');
},


setTotalPages = function () {	// set total of available pages beside the page navigation
	if (_sb_s.is_IE && (_sb_s.IEVersion < 8)) return;
	try {	// older browsers might have problems
		var totalPages=document.createElement("div");
		totalPages.id="totalPages";
		totalPages.setAttribute('class', "totalPages");
		totalPages.innerHTML = epaper_pages[0][4] + " - " + epaper_pages[epaper_pages.length-1][4];
		document.getElementById("sb_pager").appendChild(totalPages);
		setTimeout(function(){document.getElementById("totalPages").style.visibility='visible';},2000);
	} catch(ex){}
},


/**
 * override variables defined in flipbook.js
 * these settings are set at the start of the page: CSS settings and parameter overrides will WIN!
 */
custom_sb_settings_Override = function () {
	//_sb_s.DEBUGmode = 2;			// 0 = debug messages off
										// addable flags:
										// 1 = log to DIV 'debugmessage'
										// 2 = log to log window
										// 4 = additional alert on errors
	//_sb_s.pageAdjustOversizeRatio = 1.2;		// how much a page image may be oversized. default = 1.2

	_sb_s.modules.moreinfos_enable = 0;			// default = 0. > 1 = enable to get addintional information from the 'Info' folder structure
														//  get the infos derived from the '_info' folder structure only from manually set buttons
														
	_sb_s.modules.moreinfos_enableauto = 0;		// default = 0. > 1 = enable to automatically get additional information from the 'Info' folder structure

	_sb_s.modules.NAVISION_enable = 0;			// enable NAVISION communication
	_sb_s.modules.easECart_enable = 0;			// enable easECart

	_sb_s.pageDisplayMaxDoublePX = 768;

	_sb_s.lightboxMaxWidth = 660;	/*	max width the lightbox should have in pixels. default = 490. Set to zero to always use lightbox2BodyWidth factor */

	_sb_s.pageAdjustToWindowWidthTrim = 0;	// add or trim (-25 pixels because we have scroll bars)
	_sb_s.pageAdjustToWindowHeightTrim = 0;	// add or trim pixels to make book larger


	/* commenting systems */
	_sb_s.modules.disqus_enable = 0;				// default = 0. > 2 = enable the DISQUS discussion forum
	_sb_s.modules.intensedebate_enable = 0;		// default = 0. > 2 = enable the intensedebate discussion forum
	_sb_s.modules.livefyre_enable = 0;			// default = 0. > 2 = enable the livefyre discussion forum

	_sb_s.modules.easeedit_enable = 0;			// default = 0. > 2 = enable the easeedit module to easily copy article content from lightbox
	return;
},


/**
 Early configuration callback.
 Allows us to do some custom work at an early load state.
 Called by slidebook_early_config() defined in slidebook.js, after main initialization has been done 
 */
custom_early_config_cb = function () {
	/* set total pages */
	setTotalPages();

	// we might have back stepped from an external page called from a "dirlink" (see abuve)
	var setPage = ReadCookie("dirlinknReturnPageIdx");
	if (setPage != "") {
		SetCookie("dirlinknReturnPageIdx","",1);
		setPage = parseInt(setPage,10);
			//$.fn.log("log","returning to page: " + setPage);
		if (parseInt(setPage,10) != 0) setTimeout(function(){goto_page(setPage,true,null,null,true);},200);
	}


	//=============================
	//=========== more custom stuff to go here

	return;
},


/*
 Printing the lightbox content callback.
 Allows us to do some custom work when printing an article.
 Called by printLightbox() defined in slidebook.js, after the print document is created and right before it is sent to the printer
 @param pdw = a reference to the print document window
 */
custom_printLightbox_cb = function (pdw) {
	try {
		if (!pdw) return;
		// copy the title from main document to the print document
		var titleEl = document.getElementsByTagName('title')[0],
			pdw_titleEl = pdw.document.getElementsByTagName('title')[0];
		//alert(titleEl.innerHTML);
		pdw_titleEl.innerHTML = titleEl.innerHTML;
	} catch(ex){}
	return;
},

custom_showLightbox_cb = function (lightboxEl, lightbox_contentEl) {
	//alert("lightboxHead:\n" + lightboxEl.innerHTML);
},
custom_showLightboxContent_cb = function (lightboxDiv, lightboxContent, lightbox_contentEl, lightboxEl) {
	//alert("lightboxContent:\n" + lightboxDiv.innerHTML + "\ncontent:\n" + content);
	var newcontent = lightboxContent;
	// modify content
	return(newcontent);
},

customSetVideo = function (vid,code,width,height,videoName) {
	var videoElem=null,
		w=_sb_s.defaultVideoWidth, h=_sb_s.defaultVideoHeight;

	function setVideoDims(caller) {
		if (videoElem.width) w = videoElem.width;
		else if (videoElem.videoWidth) w = videoElem.videoWidth;
		if (videoElem.height) h = videoElem.height;
		else if (videoElem.videoHeight) h = videoElem.videoHeight;
		if (w < _sb_s.minVideoWidth) w = _sb_s.minVideoWidth;
		if (h < _sb_s.minVideoHeight) h = _sb_s.minVideoHeight;
		// set attributes to store original dims
		videoElem.setAttribute('data-origw',w);
		videoElem.setAttribute('data-origh',h);
		//alert(_sb_fn.list_object(videoElem));
		
		//if(videoElem.videoHeight) setTimeout(function(){alert("setVideoDims caller: " + caller + "\nvid: " + vid + "\n" + videoElem + "\nw x h: " + w + " x " + h + "\nw x h: " + videoElem.width + " x " + videoElem.height + "\nvw x vh: " + videoElem.videoWidth + " x " + videoElem.videoHeight);},2000);
		_sb_fn.calcLightboxSizes(true, w, h, vid);
		_sb_fn.resizeLightboxElements();
	}

	try { document.getElementById("lightbox_div").innerHTML = code; } catch(e) { return; }
	videoElem = document.getElementById(vid);

	//alert("customDoSetVideo_cb vid: " + vid + "\nvideoElem: " + videoElem);
	if (!videoElem) return;


	// preset dims if 'loadedmetadata' does not fire
	w = (width>-1 ? width : w);
	h = (height>-1 ? height : h);
	//alert("video set: " + vid + "\n" + videoElem + "\nw x h: " + w + " x " + h);
	videoElem.style.width = w + "px";
	videoElem.style.height = h + "px"

	// turn off font size slider and print icon
	$('#lightbox_fontsize_slider_container').css('display', 'none');
	$('.lightbox_print_icon').css('display', 'none');
	if (videoElem.width) w = videoElem.width;
	if (videoElem.height) h = videoElem.height;

	if (_sb_s.is_IE && (_sb_s.IEVersion <= 8)) {
		_sb_fn.calcLightboxSizes(true, w, h, vid);
		//_sb_fn.resizeLightboxElements();
	}

	if (videoElem.addEventListener) {
		videoElem.addEventListener('loadeddata', function() {
			setVideoDims('loadeddata');
			//alert("video loadeddata: " + vid + "\n" + videoElem + "\nw x h: " + w + " x " + h + "\nw x h: " + videoElem.width + " x " + videoElem.height + "\nvw x vh: " + videoElem.videoWidth + " x " + videoElem.videoHeight);
		}, false);

		//alert("add listener");
		videoElem.addEventListener('loadedmetadata', function() {
			setVideoDims('loadedmetadata');
			//alert("video loadedmetadata: " + vid + "\n" + videoElem + "\nw x h: " + w + " x " + h + "\nw x h: " + videoElem.width + " x " + videoElem.height + "\nvw x vh: " + videoElem.videoWidth + " x " + videoElem.videoHeight);
		}, false);

		if (_sb_s.is_Android) {
			videoElem.addEventListener('touchstart',function(){
				videoElem.play();
				},false);
			setTimeout(function(){videoElem.play();},300);
		}
	}

	return;
},

// register our call back functions
wait_register_customCBs = function () {
	if (typeof(_sb_settings) == 'undefined') {
		setTimeout(function(){wait_register_customCBs();},50);
		return;
	}
	_sb_fn.registerCB_sb_settings_Override(custom_sb_settings_Override);
	_sb_fn.registerCB_early_config(custom_early_config_cb);
	_sb_fn.registerCB_before_printLightbox(custom_printLightbox_cb);
	
	// we might want to modify the Lightbox behavior. Uncomment to register a call back function
	// _sb_fn.registerCB_before_showLightbox(custom_showLightbox_cb);					// modify lightbox header
	// _sb_fn.registerCB_before_showLightboxContent(custom_showLightboxContent_cb);	// modify lightbox content
};

setTimeout(function(){wait_register_customCBs();},1);





var shortenedBookWorker = null,
shortenedBookAlertTimeout = null,
shortenedBookAlertRecall = 60000,
shortenedBookAlertAsk = true,	// true = ask user to purchase, false = don't ask
	
shortenedBookMessage = function(cutpages, pub_companyName, pub_objectShortcut, title, pub_issueDate, filename) {
	if (!shortenedBookAlertAsk) return;
	var	shortenedBookMessager = function (oEvent) {
			var XMLfilename = "",
				url = "",
				action;
			
			XMLfilename = _sb_fn.get_moreDocumentInfo("XMLfilename");
			url = window.location.href;
			
			//action = confirm("This book '" + title + "' is shortened to " + cutpages + " pages.\n\nIf you would like to purchase the entire issue, then, please, click the OK button.");
			action = confirm(get_sbmsg(0,title,cutpages));
			if (action == true) {	// clicked on OK
				// REDIRECT TO THE SHOP TO PURCHASE FULL ISSUE
				// implement your solution
				alert(get_sbmsg(1));
				alert("WE WANT TO PURCHASE!!\n----------------------------------------------------------"
					+ "\nTITLE: " + title 
					+ "\npub_companyName: " + pub_companyName 
					+ "\npub_objectShortcut: " + pub_objectShortcut 
					+ "\npub_issueDate: " + pub_issueDate 
					
					+ "\n\nXML: " + filename 
					+ "\nURL: " + url
					);
			}
			else {	// clicked on cancel
				// make special offer or do nothing
				//...
				// re-ask
				shortenedBookAlertTimeout = setTimeout(function(){shortenedBookMessager();},shortenedBookAlertRecall);
			}
		};


	if (_sb_s.xslcssPath == "") {	// wait for path is set
		setTimeout(function(){shortenedBookMessage(cutpages, pub_companyName, pub_objectShortcut, title, pub_issueDate, filename);},100);
		return;
	}
	
	if (typeof(Worker) != 'undefined') {
		if (shortenedBookWorker == null) {
			shortenedBookWorker = new Worker(_sb_s.xslcssPath+"cutPagesAction.js");

			shortenedBookWorker.addEventListener("message", shortenedBookMessager, false);
		}
		// SET TIMEOUT TO RE-ASK TO PURCHASE
		if (shortenedBookAlertTimeout != null) {
			clearTimeout(shortenedBookAlertTimeout);
		}
		else {	// not asked until yet
			shortenedBookWorker.postMessage(""+cutpages); // start the worker.
		}
	}
	else {
		shortenedBookAlertTimeout = setTimeout(function(){shortenedBookMessager();},10000);
	}

},
get_sbmsg = function(which) {
	var rex, i, msg;
	try {
		msg = sbmsg[which][_sb_s.cur_lang_ID];
	} catch(ex) { return which; }
	for (i=1; i < arguments.length; i++) {
		rex = new RegExp("%"+i+"%","g");
		msg = msg.replace(rex,arguments[i]);
		//alert(i + ": " + arguments[i]);
    }
    return(msg);
},
num_sbm_items=3,	//number of language dependent string array elements
sbmsg=new Array(num_sbm_items);
for (i=0; i<num_sbm_items; i++) { sbmsg[i]=new Array(5); }	//prepare sub-arrays for 5 languages

sbmsg[0][0]="This document '%1%' is shortened to %2% pages.\n\nIf you would like to purchase the entire issue, then, please, click the OK button.";
sbmsg[0][1]="Dieses Dokument '%1%' ist als Leseprobe gekürzt auf %2% Seiten.\n\nFalls Sie die vollständige Ausgabe kaufen möchten, dann klicken Sie bitte auf den OK Button.";
sbmsg[0][2]="Ce document '% 1%' est lu comme un échantillon tronqué à% 2% pages.\n\nSi vous voulez acheter la version complète, s'il vous plaît cliquez sur le bouton OK.";
sbmsg[0][3]="This book '%1%' is shortened to %2% pages.\n\nIf you would like to purchase the entire issue, then, please, click the OK button.";
sbmsg[0][4]="This book '%1%' is shortened to %2% pages.\n\nIf you would like to purchase the entire issue, then, please, click the OK button.";

sbmsg[1][0]="Thank you very much!\n\nYou will be forwarded to our store.";
sbmsg[1][1]="Herzlichen Dank!\n\nSie werden nun zu unserem Shop weitergeleitet.";
sbmsg[1][2]="Merci!\n\nVous allez être redirigé vers notre boutique Internet.";
sbmsg[1][3]="Thank you very much!\n\nYou will be forwarded to our store.";
sbmsg[1][4]="Thank you very much!\n\nYou will be forwarded to our store.";

sbmsg[2][0]="The page %1% is not available in this shortened example.\n\nIf you would like to purchase the entire issue, then, please, click the OK button.";
sbmsg[2][1]="Die Seite %1% ist in dieser Leseprobe nicht verfügbar.\n\nFalls Sie die vollständige Ausgabe kaufen möchten, dann klicken Sie bitte auf den OK Button.";
sbmsg[2][2]="La page %1% n'est pas disponible dans cet échantillon tronqué.\n\nSi vous voulez acheter la version complète, s'il vous plaît cliquez sur le bouton OK.";
sbmsg[2][3]="The page %1% is not available in this shortened example.\n\nIf you would like to purchase the entire issue, then, please, click the OK button.";
sbmsg[2][4]="The page %1% is not available in this shortened example.\n\nIf you would like to purchase the entire issue, then, please, click the OK button.";

