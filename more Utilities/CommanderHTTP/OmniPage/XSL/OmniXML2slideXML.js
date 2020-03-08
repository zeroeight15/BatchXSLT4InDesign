/************
 * ======================================================================
 * THIS SOFTWARE IS PROVIDED 'AS IS' AND ANY EXPRESSED OR IMPLIED
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

THE PRODUCER:
Andreas Imhof
EDV-Dienstleistungen
CH-Guemligen, Switzerland
www.aiedv.ch

Version: 1.2
Version date: 20150925
======================================================================
*/

var DEBUG = 0;			// is set by the content of XSL variable 'DEBUG'

var pageIndex = -1;
var pageimageName = "";
var pageimageDPIx = 240;
var pageimageDPIy = 240;
var pageimageSIZEx = 0;
var pageimageSIZEy = 0;
var pageimageRESIZEx = 1.0;	// page image size/coords re-scale X. positive = page image is too small: enlarge coords
var pageimageRESIZEy = 1.0;	// page image size/coords re-scale Y
var targetDPI = 72;
var targetDPIfactor = pageimageDPIx / targetDPI;
var page_wh_rel = 1.0;

// Ein Twip ist eine angloamerikanische Längeneinheit und bezeichnet die Teilung „TWentieth of an Inch Point“, also 1/20 Punkt = 1/1440 Zoll.
// 1 twip = 1/20 inch point = 
// 1 mm = 56,6928 twip
function twip2px (twip, xy) {
	if ((twip == null) || (twip == "")) return(0);
	// pixels = (twip * dpi) (20 * 72)
	if (xy == "y") return(parseInt(twip) * pageimageDPIy / 1440 * pageimageRESIZEx / targetDPIfactor);
	return(parseInt(twip) * pageimageDPIx / 1440 * pageimageRESIZEx / targetDPIfactor);
}
function twip2pxLength (twip1, twip2, xy) {
	// pixels = (twip * dpi) (20 * 72)
	if (xy == "y") return((twip2 - twip1) * pageimageDPIy / 1440 * pageimageRESIZEx / targetDPIfactor);
	return((twip2 - twip1) * pageimageDPIx / 1440 * pageimageRESIZEx / targetDPIfactor);
}
function init_pageIndex() {
	pageIndex = -1;
}
function next_pageIndex() {
	return ++pageIndex;
}
function get_pageIndex() {
	return pageIndex;
}
function store_pageimageName(name) {
	if ((typeof(name) == 'undefined') || (name == null) || (name == "")) {
		pageimageName = "";
	}
	else pageimageName = name;
	return (pageimageName);
}
function get_pageimageName(addSequence) {
	if (typeof(addSequence) == 'undefined') return pageimageName;
	return pageimageName + "[" + pageIndex + "]";
}
function store_pageimageDPIx(dpistr) {
	pageimageDPIx = parseInt(dpistr);
	targetDPIfactor = pageimageDPIx / targetDPI;
	return (pageimageDPIx);
}
function store_pageimageDPIy(dpistr) {
	pageimageDPIy = parseInt(dpistr);
	return (pageimageDPIy);
}
function store_pageimageSIZEx(sizestr) {
	pageimageSIZEx = twip2px (sizestr, "x");
	return (pageimageSIZEx);
}
function store_pageimageSIZEy(sizestr) {
	pageimageSIZEy = twip2px (sizestr, "y");
	return (pageimageSIZEy);
}
function get_pagewidth() {
	return(pageimageSIZEx);
}
function get_pageheight() {
	return(pageimageSIZEy);
}
function store_pagebounds() {
	page_wh_rel = pageimageSIZEy / pageimageSIZEx;
}

function clean_classname(name) {
	var class_name;
	if ((typeof(name) == 'undefined') || (name == "")) return "";

	class_name = name;
	class_name = class_name.replace(/@/g,"a");
	class_name = class_name.replace(/#/g,"H");
	class_name = class_name.replace(/\$/g,"d");
	class_name = class_name.replace(/\*/g,"A");
	class_name = class_name.replace(/ /g,"_");
	class_name = class_name.replace(/,/g,"c");
	class_name = class_name.replace(/-/g,"M");
	class_name = class_name.replace(/\+/g,"P");
	class_name = class_name.replace(/\=/g,"E");
	class_name = class_name.replace(/\./g,"p");
	class_name = class_name.replace(/\;/g,"o");
	class_name = class_name.replace(/\:/g,"O");
	class_name = class_name.replace(/\!/g,"e");
	class_name = class_name.replace(/\?/g,"q");
	class_name = class_name.replace(/\\/g,"S");
	class_name = class_name.replace(/\//g,"s");
	class_name = class_name.replace(/\|/g,"B");
	class_name = class_name.replace(/\~/g,"t");

	class_name = class_name.replace(/\</g,"LT");
	class_name = class_name.replace(/\>/g,"GT");
	class_name = class_name.replace(/\(/g,"_");
	class_name = class_name.replace(/\)/g,"_");
	class_name = class_name.replace(/\{/g,"_");
	class_name = class_name.replace(/\}/g,"_");
	class_name = class_name.replace(/\]/g,"_");
	class_name = class_name.replace(/\[/g,"_");
	class_name = class_name.replace(/\%/g,"_");
	class_name = class_name.replace(/\&/g,"_");

	class_name = class_name.replace(/\'/g,"_");
	class_name = class_name.replace(/\"/g,"_");
	
	return class_name;
}

function getClassAttribute(fontface,fontsize,real) {
	var classfont = "",
		classsize = "",
		classname = "",
		fntsz;

	//message("******* fontface: " + fontface + ",   fontsize: " + fontsize);
	try {
		classfont = clean_classname(fontface);
	}
	catch(ex) {
		classfont = "";
	}

	try {
		fntsz = parseInt(fontsize);
	}
	catch(ex) {
		fntsz = 1000;
	}
	if ((typeof(real) != 'undefined') && (real == true)) {
		classsize = "" + fntsz;
	}
	else {
		do {
			if (fntsz >= 2500) { classsize = "F2500"; break; }
			if (fntsz >= 2000) { classsize = "F2000"; break; }
			if (fntsz >= 1750) { classsize = "F1750"; break; }
			if (fntsz >= 1500) { classsize = "F1500"; break; }
			if (fntsz >= 1250) { classsize = "F1250"; break; }
			if (fntsz >= 1150) { classsize = "F1150"; break; }
			if (fntsz >= 1100) { classsize = "F1100"; break; }
			if (fntsz >= 1000) { classsize = "F1000"; break; }
			if (fntsz >= 950) { classsize = "F950"; break; }
			if (fntsz >= 850) { classsize = "F850"; break; }
			if (fntsz >= 800) { classsize = "F800"; break; }
			if (fntsz >= 750) { classsize = "F750"; break; }
			if (fntsz >= 700) { classsize = "F700"; break; }
			classsize = "Fnorm";
		} while(false);
	}
	
	if (classfont != "") classname = classfont + "_" + classsize;
	else classname = classsize;
	//message("******* classname: " +classname);
	return(classname);
}


function cleanLastWordContent(txt) {
	if (txt == "") return("");
	if (txt == "-") return("-");
	var newtxt = txt;
	if (newtxt.charAt(newtxt.length-1) == "-") newtxt = newtxt.substr(0,newtxt.length-1);
	return(newtxt);
}


function getNamePart(pathname) {
	var namepart = "";
	do {
		if (pathname.indexOf("\\") >= 0) {	// Windows path separator
			namepart = pathname.substr(pathname.lastIndexOf("\\")+1);
			break;
		}
		if (pathname.indexOf("/") >= 0) {	// Unix path separator
			namepart = pathname.substr(pathname.lastIndexOf("/")+1);
			break;
		}
	} while(false);
	return(namepart);
}

var issueshortcut = "",
	issuedate = "",		// like YYYYMMDD
	issuenumber = "",
	issuenumberorig = "",
	pagenumber = 0,
	pagesection = "";
	pagesectionorig = "",
	imagecounter = 0,
	imageSourceNameExtension = ".gif",
	currentimagename = "";
function store_issueParameters(pagename) {
	if ((typeof(pagename) == 'undefined') || (pagename == null) || (pagename == "")) return;
	// filter issue parameters from page image namelike: UI_18580522_0047.tif
	//                      or from page image namelike: UI_18580605_N0003_0017.tif (3rd part = issue number)
	// Issue shortcut: 1st part: UI
	// Issue date: 2nd part: 18580522
	// Page name/section: 3rd part: 0047
	var namepart = pagename.split(".");
	var parts = namepart[0].split("_");
	issueshortcut = parts[0];
	if (parts.length < 2) return;
	issuedate = parts[1];
	if (parts.length > 3) {
		issuenumber = trimIssuenumber(parts[2]);
		issuenumberorig = parts[2];
		pagesection = trimZeroes(parts[3]);
		pagesectionorig = parts[3];
	}
	else {
		pagesection = trimZeroes(parts[2]);
		pagesectionorig = parts[2];
	}
	return;
}
function getIssueNumber() {
	return(issuenumber);
}
function getIssueNumberOrig() {
	return(issuenumberorig);
}
function getNewPageSequence() {
	pagenumber++;
	return(pagenumber);
}
function getPageSequence() {
	return(pagenumber);
}
function getPageSection() {
	return(pagesection);
}
function getPageSectionOrig() {
	return(pagesectionorig);
}
function getIssueDate() {
	return(issuedate);
}
function getIssueShortcut() {
	return(issueshortcut);
}
function getDocumentBaseName() {
	var basename = issueshortcut;
	if (issuedate != "") basename += "_" + issuedate;
	if (issuenumberorig != "") basename += "_" + issuenumberorig;
	if (pagesectionorig != "") basename += "_" + pagesectionorig;
	return(basename);
}

function getNextImageName() {
	imagecounter++;
	currentimagename = getDocumentBaseName() + "_Picture" + imagecounter + imageSourceNameExtension;
	return(currentimagename);
}
function getCurrentImageName() {
	return(currentimagename);
}

var articleIdx = -1;
function getArticleIdx() {
	articleIdx++;
	return articleIdx;
}
function currentArticleIdx() {
	return articleIdx;
}

var textflowid = 0;
function getNewTextflowid() {
	return(++textflowid);
}
function getTextflowid() {
	return(textflowid);
}

var boxSelf = 0;
function getNewBoxSelf() {
	return(++boxSelf);
}
function getBoxSelf() {
	return(boxSelf);
}


/**********************************************
 * document related stuff
 */
var docname = "";	// the original Indesign document name without name extension

function store_docname(path,input_name) {	// store and split the path of the original indd document
									// and split the folders into an array
	var doc_path_arr;
	var cur_date = new Date();
	docname = cur_date.getTime() + "genericINDDName";	// initialize
	var my_docname = "";
	if ( ((path == null) || (path == "")) && ((input_name == null) || (input_name == "")) ) return (docname);

	if ((path != null) && (path != "")) {
		do {	// generic URL path
			// Windows
			if ((path.indexOf(":\\") >= 0) || (path.indexOf("\\\\") >= 0)) {	// like c:\path... or \\192.168.23.12\path
				doc_path_arr = path.split("\\");
				break;
			}
			// mac
			if (path.indexOf(":") > 0) {	// like Volumes:myvol:fldr...
				doc_path_arr = path.split(":");
				break;
			}
			// other
			doc_path_arr = path.split("/");	// like Volumes/myvol/fldr...
		} while(false);
		my_docname = doc_path_arr[doc_path_arr.length-1];
	}
	else {
		my_docname = input_name;
	}
	if ( (my_docname == null) || (my_docname == "") ) return (docname);

	if (my_docname.lastIndexOf(".") >= 0) {
		do {
			if (my_docname.toLowerCase().lastIndexOf(".xml") >= 0) {
				my_docname = my_docname.substr(0,my_docname.toLowerCase().lastIndexOf(".xml"));
				break;
			}
		} while(false);
	}
	my_docname = my_docname.replace(/~sep~/g,'_');
	docname = my_docname;
	if (DEBUG > 0) java.lang.System.out.println ("**** converting document: '" + docname + "' ******");
	return (docname);
}

function get_indd_docname() {	// get indd document name
	return indd_docname;
}

function get_date_from_docpath(path) {
	var parts = path.split("/");
	return(parts[parts.length-2]);
}


var region_overflowtext = "";
function store_region_overflowtext(textnodesString) {
	region_overflowtext += textnodesString;
}
function have_overflowtext() {
	return(region_overflowtext != "");
}
function get_region_overflowtext() {
	var myregion_overflowtext = region_overflowtext;
	region_overflowtext = "";
	return(myregion_overflowtext);
}



/**********************************************
 * page related stuff
 */
var resizedPageJPEGname = "";	// page JPEG scaled name
var pageJPEGScale = 1.0;	// page JPEG scale factor
var pageJPEGWidth = 0;		// page JPEG width in pixesl
var pageJPEGHeight = 0;		// page JPEG height in pixesl
function set_pageJPEGScale(size) {	// 0.6 or 350x400 or 350x or x400 as string
	if ( (size == null) || (size == "") ) return;
	//if ( (size.indexOf(".") < 0) || (size.indexOf("x") >= 0) ) {	// seems to be w x h
	if (size.indexOf("x") >= 0) {	// seems to be w x h
		var wh = size.split("x");
		for (var i = 0; i < wh.length; i++) wh[i] = allTrim(wh[i]);
		do {
			if (wh[0] != "") {	// get from width
				pageJPEGWidth = parseInt(wh[0]);
				if ( (wh.length >= 2) && (wh[1] != "") ) pageJPEGHeight = parseInt(wh[1]);
				else pageJPEGHeight = Math.round(parseFloat(pageJPEGWidth) * page_wh_rel); // no height given: calculate
				pageJPEGScale = parseFloat(pageJPEGWidth) / pageimageSIZEx;	// calc page JPEG scale factor
				break;
			}
			if ( (wh.length >= 2) && (wh[1] != "") ) {	// get from height
				pageJPEGHeight = parseInt(wh[1]);
				if ( wh[0] != "" ) pageJPEGWidth = parseInt(wh[0]);
				pageJPEGWidth = Math.round(parseFloat(pageJPEGHeight) / page_wh_rel); // calculate width
				pageJPEGScale = parseFloat(pageJPEGHeight) / pageimageSIZEy;	// calc page JPEG scale factor
				break;
			}
		} while(false);
	}
	else { // set scale
		pageJPEGScale = parseFloat(size);
		if (pageJPEGScale < 0.0) pageJPEGScale = 1.0;
		pageJPEGWidth = Math.round(pageimageSIZEx * pageJPEGScale);
		pageJPEGHeight = Math.round(pageimageSIZEy * pageJPEGScale);
	}
	return;
}
function store_resizedPageJPEGname(name) {
	resizedPageJPEGname = name;
}
function get_resizedPageJPEGname() { return resizedPageJPEGname; }
function get_pageJPEGScale() { return pageJPEGScale; }
function get_pageJPEGScale100() { return Math.round(pageJPEGScale * 100.0); }
function get_pageJPEGScalePercent() { return ("" + (pageJPEGScale * 100) + "%"); }
function get_pageJPEGWidth() { return (pageJPEGWidth); }
function get_pageJPEGHeight() { return (pageJPEGHeight); }



/**********************************************
 * utilities
 */
function message(mess) {
	java.lang.System.out.println (mess);
}
// trim a string from leading and ending spaces
function allTrim(str) {
	if ( (str == null) || (str == "") ) return(str);
	while (str.substring(0,1) == ' ') {
		str = str.substring(1, str.length);
	}
	while (str.substring(str.length-1, str.length) == ' ') {
		str = str.substring(0,str.length-1);
	}
	return str;
}
function endsWith(str, s){
	if ( (str == null) || (str == "") ) return(false);
	if ( (s == null) || (s == "") ) return(false);
	var reg = new RegExp (s + "$");
	return reg.test(str);
}
function is_whitespace_only(str) {
	if (str.length < 1) return(true);
	if (str.length > 2) return(false);
	if (str.match(/^\s*$/)) {
		// nothing, or nothing but whitespace
		return(true);
	}
	return (false);
}

var storeValue = new Array(5);	// store up to 5 values
function store_Value(idx,str) {
	storeValue[idx] = str;
	return(str);
}
function restore_Value(idx) { return(storeValue[idx]); }

function fileExists(filename) {
	f = new java.io.File(filename);
	if (DEBUG > 0) java.lang.System.out.println ("*** file: '" + filename + "' exists: " + f.exists());
	return f.exists();
}
function create_new_name(filename, name_part) {
	return(create_new_name(filename, name_part, null));
}
function create_new_name(filename, name_part, pagenumber, newext) {
	var pos = filename.lastIndexOf(".");	// cut filename extension
	var name = filename;
	var ext = "";
	var page = "";
	if (pagenumber != null) page = pagenumber;
	if (pos >= 0) {
		name = filename.substr(0,pos);
		if (typeof(newext) != 'undefined') ext = newext;
		else ext = '.' + filename.substr(pos+1,filename.length);
	}
	return (name + page + "_" + name_part  + ext);
}
function new_name_ext(filename, newext) {
	var name = filename;
	var pos = filename.lastIndexOf(".");	// cut filename extension
	if (pos >= 0) {
		name = filename.substr(0,pos);
	}
	return (name + newext);
}

var pageJPEGcopyName = "";
function store_pageJPEGcopyName(name) {
	pageJPEGcopyName = name;
	return(pageJPEGcopyName);
}
function get_pageJPEGcopyName() {
	return(pageJPEGcopyName);
}


var currentParagraphClassName = "";
function store_paragraphClassName(paraclass) {
	currentParagraphClassName = paraclass;
}
function get_paragraphClassName() {
	return currentParagraphClassName;
}
function startsWithCapitalLetter(text) {
	var re = /^[A-Z].*/;
	return re.test(text);
}


function encode_URI(uri) {
	return (encode_URI(uri,'1','',0,false));
}
function encode_URI(uri,safe) {
	return (encode_URI(uri,safe,'',0,false));
}
// 0 or blank = do not encode leafe as is in filenames and uri
// 1 = URI encode  (%XX),
// 2 = Xplatform safe URI encode to  (xXX) -->
var max_filenname_length = 31;
function encode_URI(uri,safe,postfix,postfixlength,shorten31) {
	if ( (uri == null) || (uri == "") ) return("");
	var my_uri = encodeURI(uri);
	my_uri = my_uri.replace(/\#/g,"x23");
	my_uri = my_uri.replace(/\&/g,"x26");
	my_uri = my_uri.replace(/\?/g,"x3F");
	if ( (safe == null) || (safe == '') || (safe == '0') ) return(my_uri);
	do {
		if (safe.indexOf("2") >= 0) {
			my_uri = my_uri.replace(/\%/g,'x');
			if (my_uri.length <= 31) break;
			if ((shorten31 == null) || (shorten31 == false)) break;
			// shorten to a max length of 31 chars
			// split name and ext
			var my_name = my_uri;
			var my_ext = "";
			var pointpos = my_name.lastIndexOf(".");
			if (pointpos >= 0) {
				my_name = my_uri.substr(0,pointpos);
				my_ext = my_uri.substr(pointpos,my_uri.length - pointpos);
			}
			if (my_ext.length > 6) {	// this is not a real extension
				my_name = my_uri;
				my_ext = "";
			}
			// calc checksum
			var name_chksum = 0;
			// shorten name
			for (var i = 0; i < my_name.length; i++) name_chksum += i*my_name.charCodeAt(i);
			var name_chksum_str = "" + name_chksum;
			var mypostfix = "";
			var mypostfixstr = "";
			if ((postfix != null) && (postfix != "")) {
				mypostfix = prefixNumberString(postfix,postfixlength);
				mypostfixstr = '_' + mypostfix + '_';
			}
			var max_name_length = max_filenname_length - my_ext.length - name_chksum_str.length - mypostfixstr.length;
			my_name = my_name.substr(0,max_name_length);
			// add checksum + postfix
			my_name = my_name + mypostfixstr + name_chksum_str;
			// re-concat name and ext
			my_uri = my_name + my_ext;
		}
	} while(false);

	return (my_uri);
}

function escapeMagick(str) {
	var newstr = str;
//	newstr = newstr.replace(/-/g,'\\-');
	newstr = newstr.replace(/\|/g,'\\|');
//	newstr = newstr.replace(/%/g,'\\%');
//	newstr = newstr.replace(/\*/g,'\\*');

	newstr = newstr.replace(/ /g,'~blnk~');	// MUST be (LAST) to transfer arguments to JAVA routine -->
	return (newstr);
}

function removePercent(str) {
	if (str == "") return(str);
	if (str.indexOf("%") != 0) return(str);
	// remove starting %
	return (str.substr(1,str.length));
}

function createParamString(p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20) {
	var param = "";
	if (p1 && (p1 != "")) param += p1;
	if (p2 && (p2 != "")) param += ((param != "") ? " " : "") + p2;
	if (p3 && (p3 != "")) param += ((param != "") ? " " : "") + p3;
	if (p4 && (p4 != "")) param += ((param != "") ? " " : "") + p4;
	if (p5 && (p5 != "")) param += ((param != "") ? " " : "") + p5;
	if (p6 && (p6 != "")) param += ((param != "") ? " " : "") + p6;
	if (p7 && (p7 != "")) param += ((param != "") ? " " : "") + p7;
	if (p8 && (p8 != "")) param += ((param != "") ? " " : "") + p8;
	if (p9 && (p9 != "")) param += ((param != "") ? " " : "") + p9;
	if (p10 && (p10 != "")) param += ((param != "") ? " " : "") + p10;
	if (p11 && (p11 != "")) param += ((param != "") ? " " : "") + p11;
	if (p12 && (p12 != "")) param += ((param != "") ? " " : "") + p12;
	if (p13 && (p13 != "")) param += ((param != "") ? " " : "") + p13;
	if (p14 && (p14 != "")) param += ((param != "") ? " " : "") + p14;
	if (p15 && (p15 != "")) param += ((param != "") ? " " : "") + p15;
	if (p16 && (p16 != "")) param += ((param != "") ? " " : "") + p16;
	if (p17 && (p17 != "")) param += ((param != "") ? " " : "") + p17;
	if (p18 && (p18 != "")) param += ((param != "") ? " " : "") + p18;
	if (p19 && (p19 != "")) param += ((param != "") ? " " : "") + p19;
	if (p20 && (p20 != "")) param += ((param != "") ? " " : "") + p20;
	return (param);
}

function to_lower_case(str) { return str.toLowerCase(); }

function parseToFloat(str) {
	if (str == null || str == "") return(0.0);
	try {
		return(parseFloat(str));
	}
	catch(e) {
		return(0.0);
	}
	return (0.0);
}

function parseToInt(str) {
	if (str == null || str == "") return(0);
	try {
		return(parseInt(str));
	}
	catch(e) {
		return(0);
	}
	return (0);
}

function trimZeroes(s) {
	return s.replace(/^0+/, '');
}
function trimIssuenumber(s) {	// string like N0004
	var str = s.substr(1,s.length);	// remove leading N
	return trimZeroes(str);
}
