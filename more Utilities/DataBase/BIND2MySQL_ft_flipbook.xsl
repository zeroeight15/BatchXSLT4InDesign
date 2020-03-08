<?xml version="1.0" encoding="UTF-8"?>

<!-- ==============================================================
     XSL for XML Extracts
     Version: 1.73
     Version Date: 20120912

     Purpose:
     Transform extracted XML flipbook data and push it into a database
     for fulltext index searches.

     Currently, XML ePaper extracted from InDesign may be processed.
     The Extractor applications may be: BatchXSLT for InDesign

     For all parts of this package:
     use in terms of the following Software license:

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
     Copyright 2001-2012, All rights reserved.

     ============================================================== -->



<!-- ==============================================================
     This Stylesheet Element is for the use with:
     Apache's Java xalan.jar and xerces.jar XSL Transformation engine
     ============================================================== -->

<!--				xmlns:sql="com.hatop.sxql.SQLXtension" -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				xmlns:lxslt="http://xml.apache.org/xslt"
				xmlns:sql="com.hatop.sxql.SQLXtension"
				xmlns:myutils="com.epaperarchives.batchxslt.utils"
				xmlns:locjs="loc_funcs"
				extension-element-prefixes="sql locjs"
				exclude-result-prefixes="locjs myutils"
				version="1.0">
	<!-- the default Parameters given always by BatchXSLT -->
	<xsl:param name="XMLSRC_VERSION"/>
	<xsl:param name="XMLSRC_ENCODING"/>
	<xsl:param name="XMLSRC_DOCTYPE_DECLARATION"/>
	<xsl:param name="SYSTEM_OS_NAME"/>
	<xsl:param name="SYSTEM_VM_VERSION"/>
	<xsl:param name="INPUT_PATH"/>
	<xsl:param name="INPUT_SUB_PATH"/>
	<xsl:param name="INPUT_NAME"/>
	<xsl:param name="LOGFILE_WRITE"/>
	<xsl:param name="LOGFILE_PATH"/>
	<xsl:param name="LOGFILE_NAME"/>


	<!-- additional parameters given from jobticket -->
	<xsl:param name="db_type"/>
	<xsl:param name="db_drvr"/>
	<xsl:param name="db_url"/>
	<xsl:param name="port"/>
	<xsl:param name="db"/>
	<xsl:param name="dbtbl"/>
	<xsl:param name="user"/>
	<xsl:param name="pw"/>
	<xsl:param name="DOMAIN"/>
	<xsl:param name="root_datapath"/>
	<xsl:param name="sub_path_override"/>
	<xsl:param name="sub_path"/>
	<xsl:param name="src_name"/>
	<xsl:param name="object_id"/>
	<xsl:param name="encoding"/>
	<xsl:param name="ConnectCmd"/><!-- cmd to perform when connect -->

	<xsl:param name="PDFtextExtract" select="0"/><!--extract text from PDF images -->
	<xsl:param name="min_text_length_4db" select="3"/><!--articles with less than this number of chars will not be inserted -->


	<!-- ===============================================
		 the XSL Transform stuff
		 =============================================== -->
	<xsl:output method="text"/>
	<xsl:output media-type="text/html"/>
	<xsl:output encoding="UTF-8"/>
	<!-- xsl:output cdata-section-elements="*"/ -->
	<xsl:preserve-space elements="*"/>

	<!-- exclude PDFs from text extract if content@label contains this string. set to empty to extract all PDFs -->
	<xsl:variable name="PDFexcludeTextExtractLabel1">*nopdftextextract*</xsl:variable>
	<xsl:variable name="PDFexcludeTextExtractLabel2">&gt;advertisement&lt;</xsl:variable>
	<xsl:variable name="PDFexcludeTextExtractLabel3"></xsl:variable>

	<!-- the paragraph class name that contains the page section title to retrieve... -->
	<xsl:variable name="PSectionClass">PSection</xsl:variable>
	<!-- ... and/or the character class name that contains the page section title to retrieve -->
	<xsl:variable name="CSectionClass">CSection</xsl:variable>


	<xsl:variable name="DEBUG" select="1"/><!-- set into debug mode using '1' -->
	<xsl:variable name="writeTextFileOnly" select="0"/><!-- 1 = write output file of extracted text only (no database push) -->


	<!-- ===============================================
		 our local JAVA Script functions
		 =============================================== -->
	<lxslt:component prefix="locjs" 
		functions="init_ids store_creation_date get_creation_date store_creation_time get_creation_time 
					store_docpath_arr getelement_docpath_arr
					store_docpagenbr get_docpagenbr store_chaptpagenbr get_chaptpagenbr
					store_section get_section
					store_proj_name store_layout_name
					get_lid
					get_startpage get_endpage
					left_fill filter_str set_string_quoting get_string_quoting
					get_VM_version get_OS_name">

		<lxslt:script lang="javascript">
//<![CDATA[
var docpath_arr = null;
var creation_date = "";
var creation_time = "";
var docpagenbr = "";
var chaptpagenbr = "";
var publication = "";
var section = "";
var project_name = "";
var layout_name = "";
var layout_id = "";
var issue_year="";
var issue_date="";	// the issue date or number
var issue_nbr="";
var article_name = "";
var string_quoting = 0;		// if 1, we have to replace single quotes in output strings

function init_ids() {
	creation_date = "";
	creation_time = "";
	docpagenbr = "NaS";
	chaptpagenbr = "NaS";
	publication = "NaS";
	section = "NaS";
	project_name = "NaS";
	layout_name = "NaS";
	layout_id = "NaS";
	article_name = "NaS";
	return ;
}

function store_docpath_arr(path) {	// store and split the path of the original xpress document
									// and split the folders into an array
	docpath_arr = path.split(":");
	return;
}

function getelement_docpath_arr(element) {	// get an element from path array
	if (docpath_arr == null) return("");
	if (element >= docpath_arr.length) return("");
	if (element < 0) return("");
	return(docpath_arr[element]);
}


function get_startpage(pages_string) {	// split a pages sitring like '1' or '1-3', return start page number
	var page_arr = pages_string.split("-");
	return(page_arr[0]);
}
function get_endpage(pages_string) {	// split a pages sitring like '1' or '1-3', return end page number
	var page_arr = pages_string.split("-");
	if (page_arr.length < 2) return(page_arr[0]);	// one page only
	return(page_arr[1]);	// end page number
}


function store_creation_date(e) {	// stores a date in the form YYYYMMDD
	if (e != "") {
		// check if we have to transform the date - it might be like DD.MM.YYYY
		var mydate = "" + e;
		if (mydate.indexOf(".") < 0) creation_date = mydate;	// has probably the correct form YYYYMMDD
		else {
			if ((mydate.indexOf(".") == 4) || (mydate.length != 10)) { // probably the form: YYYY.DD.MM - just remove the '.'
				var re = /./g;
				creation_date = "" + mydate.replace(re,"");
			}
			else { // probably the form: DD.MM.YYYY - rearrange to YYYYMMDD
				var day = mydate.substr(0,2);
				var month = mydate.substr(mydate.indexOf(".")+1,2);
				var year = mydate.substr(mydate.lastIndexOf(".")+1,4);
				creation_date = "" + year + month + day;
			}
		}
	}
	else {	// no date given - store current date
		var cur_date = new Date();
		var m = "" + (cur_date.getMonth()+1);
		if (m.length != 2) m = "0" + m;
		var d = "" + cur_date.getDate();
		if (d.length != 2) d = "0" + d;
		creation_date = "" + cur_date.getFullYear() + m + d;
	}
	return ("" + creation_date) ;
}
function get_creation_date() { return ("" + creation_date); }

function store_creation_time(e) {
	if (e != "") creation_time = e;
	else {	// no time given - store current time
		var cur_date = new Date();
		var h = "" + cur_date.getHours(); if (h.length != 2) h = "0" + h;
		var m = "" + cur_date.getMinutes(); if (m.length != 2) m = "0" + m;
		var s = "" + cur_date.getSeconds(); if (s.length != 2) s = "0" + s;
		creation_time = "" + h + ":" + m + ":" + s;
	}
	return("" + creation_time) ;
}
function get_creation_time() { return creation_time; }

function store_docpagenbr(e) { docpagenbr = e; return(docpagenbr) ; }
function get_docpagenbr() { return docpagenbr; }

function store_chaptpagenbr(e) { chaptpagenbr = e; return(chaptpagenbr) ; }
function get_chaptpagenbr() { return chaptpagenbr; }

function store_section(e) { section = e; return(section) ; }
function get_section() { return section; }

function store_proj_name(e) { project_name = e; return(project_name) ; }

function store_layout_name(e) { layout_name = e; return(layout_name) ; }

function create_lid(docname,fullpath) {
	// let's create an ID string in the form:
	// object/issue_year/issue_nr(or issue_date)/project_name[/optional layout_name if not equal to project_name]
	// in this example we assume to retrieve some values from the docoment name or/and the document path
	// which we have stored and splited into its folder parts previously
	//
	// assume we have exported the layouts from this path
	//		Macintosh HD:ePaperData:Xport:inQXP:COMPANY:OBJECT:YEAR:ISSUE:
	//   and the document has a naming convention like this SSS-YYYYMMDD-##-section
	//		where sss = a 2-3 digits shortcut for the object
	//		where YYYYMMDD = the issue date
	//		where ## = the page number
	// we try to get all needed infos from the document name first otherwise from the path:
	//	publication=docpath_arr[5] [docpath_arr.length - 3]
	//	issue_year=docpath_arr[6] [docpath_arr.length - 2]
	//	issue_date="" 
	//	issue_nbr=docpath_arr[7] [docpath_arr.length - 1]

	// first, try get issue year and date from the document name
	// if we have no document date in the form YYYYMMDD - 8 following digits we have to try to get it from the path

	var s = "";
	// ok - lets try to find 8 following digits from the given 'docname'
	var found_a_date_in_name = false;
	for (var i=0; i < docname.length-7; i++) {
		if (	(isNaN(docname.charAt(i))==false)
				&& (isNaN(docname.charAt(i+1))==false)
				&& (isNaN(docname.charAt(i+2))==false)
				&& (isNaN(docname.charAt(i+3))==false)
				&& (isNaN(docname.charAt(i+4))==false)
				&& (isNaN(docname.charAt(i+5))==false)
				&& (isNaN(docname.charAt(i+6))==false)
				&& (isNaN(docname.charAt(i+7))==false)
			) {
				found_a_date_in_name = true;
				issue_year = docname.substr(i,4);
				issue_date = docname.substr(i,8);
				break; }
	}
	if (!found_a_date_in_name) {	// get year and date/issue#
		issue_year=docpath_arr[docpath_arr.length - 3];
		issue_date=docpath_arr[docpath_arr.length - 2];
	}

	publication = docname.substr(0,2);	// get the two chars publication ID
	if (	(isNaN(publication.charAt(0))==false) ) {		// publication ID must start with a char
		publication=docpath_arr[docpath_arr.length - 4];	// otherwise get from path
	}

	// try to filter a section
	section = "";

	if (layout_name == project_name) {	// old QXP4 or 5 docs
		s = "" + publication + '/' + issue_year+ '/' + issue_date + '/' + layout_name;
	}
	else {	// QXP6 documents
		s = "" + publication+ '/' + issue_year+ '/' + issue_date + '/' + project_name + '/' + layout_name;
	}
	layout_id = s;
	return null;
}

function store_issue_year(str) { issue_year=str; }
function store_issue_date(str) { issue_date=str; }
function retrieve_issue_year_date(docname,fullpath) {
	// retrieve an issue date and year from document name if a number like 20080423 (YYYYMMDD) is contained.
	// otherwise, try to retrieve from export path
	//
	// assume we have exported the layouts from this path
	//		Macintosh HD:ePaperData:Xport:inSRC:COMPANY:OBJECT:YEAR:ISSUE:
	//   and the document has a naming convention like this SSS-YYYYMMDD-##-section
	//		where sss = a 2-3 digits shortcut for the object
	//		where YYYYMMDD = the issue date
	//		where ## = the page number

	issue_year="";
	issue_date="";	// the issue date or number

	// first, try get issue year and date from the document name
	// if we have no document date in the form YYYYMMDD - 8 following digits we have to try to get it from the path

	var s = "";
	// ok - lets try to find 8 following digits from the given 'docname'
	var found_a_date_in_name = false;
	var string_to_search = docname;
	var run = 1;
	while (run <=2) {
		for (var i=0; i < string_to_search.length-7; i++) {
			if (	(isNaN(string_to_search.charAt(i))==false)
					&& (isNaN(string_to_search.charAt(i+1))==false)
					&& (isNaN(string_to_search.charAt(i+2))==false)
					&& (isNaN(string_to_search.charAt(i+3))==false)
					&& (isNaN(string_to_search.charAt(i+4))==false)
					&& (isNaN(string_to_search.charAt(i+5))==false)
					&& (isNaN(string_to_search.charAt(i+6))==false)
					&& (isNaN(string_to_search.charAt(i+7))==false)
				) {
					found_a_date_in_name = true;
					issue_year = string_to_search.substr(i,4);
					issue_date = string_to_search.substr(i,8);
					break;
			}
		}
		if (run > 2) break;
		if (!found_a_date_in_name) {
			string_to_search = fullpath;
			run++;
		}
		if (found_a_date_in_name) break;
	}
	return null;
}


function get_lid() { return layout_id; }
function get_publication() { return publication; }
function get_issue_year() { return issue_year; }
function get_issue_date() { return issue_date; }
function get_issue_nbr() { return issue_nbr; }


var object_id = "";
function store_objectShortcut(str) {
	object_id = str;
}
function store_object_id(docname) {	// get an id from document name
	if ( (docname == null) || (docname == null) ) return("");
	var start_underscore = docname.indexOf("_");
	var start_blank = docname.indexOf(" ");
	var start_divis = docname.indexOf("-");
	var end = start_underscore;
	do {
		if (end < 2) end = start_blank;
		else break;
		if (end < 2) end = start_divis;
		break;
	} while(false);
	if (end < 2) object_id = docname;
	else object_id = docname.substring(0,end);
	return (object_id);
}
function get_object_id () { return(object_id); }	// return id from document name

function left_fill(s) {		// left fill with '0' up to 6 digits
	var lf = "" + s;
	switch (s.length) {
		case 1: lf = "00000" + s; break;
		case 2: lf = "0000" + s; break;
		case 3: lf = "000" + s; break;
		case 4: lf = "00" + s; break;
		case 5: lf = "0" + s; break;
	}
	return lf;
}

function filter_str(s) {				// filter a string from unwanted chars (old, wrong entities)
	var lf = "" + s;
	var re = /\'/g;					// preset to qoute

	if (string_quoting > 0) {
		lf = lf.replace(re,"''");		// replace single apostrophs with double apostrophs for MsSQL Server 7
	}
	return lf;
}

function set_string_quoting(translate_quotes) {		// set or reset string quoting
	string_quoting = translate_quotes;
	return string_quoting;
}

function get_string_quoting() {			// get current flag for string quoting
	return string_quoting;
}

function get_VM_version() {				// get current VM version
	return java.lang.System.getProperty("java.version");
}

function get_OS_name() {				// get current VM version
	return java.lang.System.getProperty("os.name");
}



// ******** functions to store/retrieve a list (set) of contained images in a Layout
var imagenames_list = "";			// image list like name1,name2.eps,name3,....
function store_image_name(path) {	// split the path and store last element = original image name
									// and split the folders into an array
	if ( (path == null) || (path == "") ) return;
	var img_arr = path.split(":");
	if (imagenames_list != "") imagenames_list += ","; // ad a comma
	imagenames_list += "\'" + img_arr[img_arr.length - 1] + "\'";
	return;
}
function get_image_list() {
	return imagenames_list;
}



// ******** functions to use on extraced XML from EPS
var epspath_arr = null;
var EPSsrc = "";

function store_epspath_arr(path) {	// store and split the path of the original XML document
									// and split the folders into an array
	epspath_arr = path.split("/");
	return;
}
function store_EPSsrc(name) {
	EPSsrc = name;
	return;
}

function get_epsAid() {
	if (epspath_arr == null) return "AAAAAAAAAAA"
	var s = "" + epspath_arr[epspath_arr.length - 4] + '/' + epspath_arr[epspath_arr.length - 3]+ '/' + epspath_arr[epspath_arr.length - 2] + '/' + EPSsrc;
	return s;
}

function get_epsOid() {
	if (epspath_arr == null) return "OOOOOOOOOOO"
	var s = "" + epspath_arr[epspath_arr.length - 4] + '/' + epspath_arr[epspath_arr.length - 3]+ '/' + epspath_arr[epspath_arr.length - 2];	
		
	return s;
}

function get_epsIssue_date() {
	if (epspath_arr == null) return "00000000"
	var	s = "" + epspath_arr[epspath_arr.length - 2];
	return s;
}


// ******** functions for section titles
var section_title = "";
function store_section_title(t) {
	if ((t == null) || (t == "")) return;
	if (section_title != "") section_title += ", ";
	section_title += t;
	return;
}
function get_section_title() {
	return section_title;
}
function init_section_title() {
	section_title = "";
	return null;
}

var min_text_length_4db = 3;	// articles with less than this number of chars will not be inserted
function set_min_text_length_4db(minchars) {
	try {
		min_text_length_4db = parseInt(minchars);
	} catch(e) {}
}
function cleanText(text) {
	if ((text == null) || (text == "")) return("");
	if (text.length < min_text_length_4db) return("");
	try {
		var clean = text;

		var re = /-\r/g; clean = clean.replace(re,"");	// manual hyphens
		re = /-\n/g; clean = clean.replace(re,"");
		re = / \r/g; clean = clean.replace(re," ");	// manual line breaks
		re = / \n/g; clean = clean.replace(re," ");
		re = /\r /g; clean = clean.replace(re,"\r");
		re = /\n /g; clean = clean.replace(re,"\r");
		re = /[\r\n]+/g; clean = clean.replace(re,"\r");
		re = / +/g; clean = clean.replace(re," ");
		re = /_/g; clean = clean.replace(re,"");


		// some french stuff with leading l', d' and more
		re = /\u2019/g; clean = clean.replace(re,"' "); // typographic apostroph add a space after
		/* uncomment to throw them out
		re = /^.'/g; clean = clean.replace(re,""); // like l'homme at begin of line
		re = / .'/g; clean = clean.replace(re," "); //        after a space
		*/

		if (clean.length < min_text_length_4db) return("");
		return(clean);
	} catch(e) {}
	return(text); 
}

function split_area(coords, which) {	// return value from coords string l,t,r,b
	if ( (coords == null) || (coords == "") ) return("0");
	var coords_arr = coords.split(",");
	return(coords_arr[which]);
}


// replace using regexp
function re_replace(sourcestr,findstr,replstr,option) {
	if ( (sourcestr == "") || (findstr == "") ) return(sourcestr);
	var re = null;
	try { re = new RegExp(findstr,option); } catch(e) { return(sourcestr); }
	return (sourcestr.replace(re,replstr));
}


function message(mess) {
	java.lang.System.out.println (mess);
}

//]]>
		</lxslt:script>
	</lxslt:component>




	<!-- ******************************************************************************************** -->

	<!-- ===============================================
		 the root stuff: connect to DB, Transform, close connection
		 =============================================== -->
	<xsl:template match="/">
		<xsl:if test="$DEBUG = '1'">
			<xsl:variable name="d1" select="locjs:message(concat('============== Document: ',//header/headerfield[@name='filename']/.,' =============='))"/>
			<xsl:variable name="d2" select="locjs:message(concat('Input path: ',$INPUT_PATH))"/>
			<xsl:variable name="d3" select="locjs:message(concat('Input sub path: ',$INPUT_SUB_PATH))"/>
		</xsl:if>

		<xsl:variable name="d3" select="locjs:set_min_text_length_4db($min_text_length_4db)"/>


		<xsl:variable name="mysqlConnected">
			<xsl:call-template name="connectMysql"/>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$mysqlConnected != '0'">
				<xsl:variable name="d" select="myutils:showMess(concat('&#10;### ERROR #',$mysqlConnected,' while connecting to database: ',$db,'. See console for details!&#10;'),'1')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="d" select="myutils:showMess(concat('&#10;Successfully connected to database: ',$db,'&#10;'),'1')"/>
				<locjs:init_ids />
				<xsl:apply-templates select="indd_document" />
				<sql:close />
			</xsl:otherwise>
		</xsl:choose>

	</xsl:template>


	<xsl:template name="connectMysql">


		<!-- ===== Establishing connection to the database: ============= -->
		<!-- you may need to enhance the code below to connect to other Databases than MySQL -->

		<xsl:choose>
			<xsl:when test="contains($db_type,'mysql')"><!-- ===== to connect to MySQL: ========= -->
				<xsl:choose>
					<xsl:when test="element-available('sql:init') = false">
						<xsl:value-of select="-2"/>!-- set return value error -->
						<xsl:message terminate="yes">### SQL DB utility 'sxql' jar classes not reachable! Transform aborted!</xsl:message>
					</xsl:when>
					<xsl:otherwise>
							<!-- sql:init dburl='{$db_type}://{$db_url}:{$port}/{$db}?useUnicode=true&amp;characterEncoding=UTF-8' driver="{$db_drvr}" user="{$user}" passwd="{$pw}" / -->
						<xsl:variable name="sqlDbConnStr"><xsl:value-of select="$db_type"/>://<xsl:value-of select="$db_url"/>:<xsl:value-of select="$port"/>/<xsl:value-of select="$db"/>?useUnicode=true&amp;characterEncoding=UTF-8</xsl:variable>
						<xsl:variable name="sqlretval" select="sql:init($sqlDbConnStr, $db_drvr, $user, $pw)" />

						<xsl:choose>
							<xsl:when test="string($sqlretval) != 'true'" >
								<xsl:value-of select="-3"/><!-- set return value error -->
								<xsl:message terminate="yes">
### ERROR connecting to database: '<xsl:value-of select="$sqlDbConnStr"/>'
###    user: '<xsl:value-of select="$user"/>'    passwd: '<xsl:value-of select="$pw"/>'
### Return value was: '<xsl:value-of select="$sqlretval"/>' - Processing aborted!</xsl:message>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="0"/><!-- set return value OK -->
								<!-- ===== uncomment this for debugging purposes like: ========== -->
								<!-- problems to connect to the database -->
								<!-- problems to insert fields into the database ... -->
								<!-- sql:setLog level="4" /-->

								<!-- the 'setQuote' function is available in hatop sxql driver latest driver only: for java 1.4 and newer only -->
								<xsl:if test="element-available('sql:setQuote')">
									<sql:setQuote quote="true"/>	<!-- this is default anyway: single quotes are handled by sxql and replaced by \' -->
								</xsl:if>
								<xsl:variable name="d" select="locjs:set_string_quoting(0)" />

								<!-- initial command to DB when connected -->
								<xsl:if test="$ConnectCmd != ''" >
									<sql:execute select="{$ConnectCmd}"/>
									<xsl:message terminate="no">### ConnectCmd: '<xsl:value-of select="$ConnectCmd"/>' sent!</xsl:message>
								</xsl:if>

								<xsl:if test="$DEBUG = '1'">
									<xsl:variable name="d1" select="locjs:message(concat('**** Connected to Database ',$db_type,'//',$db_url,'/',$db))"/>
								</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>

			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="-1"/>!-- set return value error -->
				<xsl:message terminate="yes">### JDBC driver not implemented! Transform aborted!</xsl:message>
			</xsl:otherwise>
		</xsl:choose>

	</xsl:template>


	<!-- ===============================================
		 the 'document' stuff
		 =============================================== -->
	<xsl:template match="indd_document">

		<!-- first check if this file contains a 'header' element. if not, abort processing -->
		<xsl:choose>
			<xsl:when test="header" >
			</xsl:when>
			<xsl:otherwise>
				<xsl:message terminate="yes">
### ERROR XML document does not contain a header!
###    Source file name: '<xsl:value-of select="$src_name"/>'
###    Processing aborted!</xsl:message>
			</xsl:otherwise>
		</xsl:choose>


		<!-- preset some general vars for this file -->
		<xsl:choose>
			<xsl:when test="/indd_document/header/headerfield[@name='creationDate'] != ''" >	<!-- check if is a newer XML version with OutputCreationDate attr -->
				<xsl:variable name="d" select="locjs:store_creation_date(string(/indd_document/header/headerfield[@name='creationDate']/.))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<!-- try to get the creation date from elsewhere -->
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
		<!-- make sure we HAVE a creation date -->
		<xsl:if test="locjs:get_creation_date() = ''" >
			<xsl:variable name="d" select="locjs:store_creation_date('')" />
		</xsl:if>

		<xsl:choose>
			<xsl:when test="/indd_document/header/headerfield[@name='creationTime'] != ''" >
				<xsl:variable name="d" select="locjs:store_creation_time(string(/indd_document/header/headerfield[@name='creationTime']/.))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="d" select="locjs:store_creation_time('00:00:00')" />
			</xsl:otherwise>
		</xsl:choose>

		<!-- MUST be before getting the header -->
		<xsl:variable name="d" select="locjs:init_section_title()" />
		<!-- store the section title from paragraph and character styles -->
		<xsl:if test="$PSectionClass != ''">
			<xsl:for-each select="descendant::div[@class=$PSectionClass]" >
				<xsl:variable name="d" select="locjs:store_section_title(string(.))" />
			</xsl:for-each>
		</xsl:if>
		<xsl:if test="$CSectionClass != ''">
			<xsl:for-each select="descendant::span[@class=$CSectionClass]" >
				<xsl:variable name="d" select="locjs:store_section_title(string(.))" />
			</xsl:for-each>
		</xsl:if>

		<!-- get the header data -->
		<xsl:apply-templates select="header" />

		<!-- get all articles -->
		<xsl:for-each select="page">
			<xsl:apply-templates select="articles/article">
				<xsl:with-param name="page_name"><xsl:value-of select="@page_name"/></xsl:with-param>
			</xsl:apply-templates>
		</xsl:for-each>

		<xsl:if test="$DEBUG = '1'">
			<xsl:variable name="mess" select="concat('============== END Document: ',//header/headerfield[@name='filename']/.,' ==============')"/>
			<xsl:variable name="d1" select="locjs:message(string($mess))"/>
		</xsl:if>
	</xsl:template>




	<!-- ===============================================
		 get the layout header data
		 =============================================== -->
	<xsl:template match="header">

		<xsl:variable name="d" select="locjs:store_docpath_arr(string(headerfield[@name='inputPath']))" />
		<xsl:variable name="d" select="locjs:store_proj_name(string(headerfield[@name='indesignDocname']))" />
		<xsl:variable name="d" select="locjs:store_layout_name(string(headerfield[@name='indesignDocname']))" />
		<xsl:choose>
			<xsl:when test="headerfield[@name='pub_issueDate']/. != ''">
				<xsl:variable name="d" select="locjs:store_issue_year(string(headerfield[@name='pub_issueYear']))" />
				<xsl:variable name="d" select="locjs:store_issue_date(string(headerfield[@name='pub_issueDate']))" />
			</xsl:when>
			<xsl:when test="headerfield[@name='indesignDocname']/. != ''">
				<xsl:variable name="d" select="locjs:retrieve_issue_year_date(string(headerfield[@name='indesignDocname']),string(headerfield[@name='inputPath']))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="d" select="locjs:retrieve_issue_year_date(string(headerfield[@name='filename']),string(headerfield[@name='inputPath']))" />
			</xsl:otherwise>
		</xsl:choose>
		<xsl:choose>
			<xsl:when test="$object_id != ''"><!-- highes priority when overriden from top caller -->
				<xsl:variable name="d" select="locjs:store_objectShortcut(string($object_id))" />
			</xsl:when>
			<xsl:when test="headerfield[@name='pub_objectShortcut']/. != ''">
				<xsl:variable name="d" select="locjs:store_objectShortcut(string(headerfield[@name='pub_objectShortcut']))" />
			</xsl:when>
			<!-- create an object id from document name -->
			<xsl:when test="headerfield[@name='indesignDocname']/. != ''">
				<xsl:variable name="d" select="locjs:store_objectShortcut(string(headerfield[@name='indesignDocname']))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="d" select="locjs:store_object_id(string(headerfield[@name='filename']/.))" />
			</xsl:otherwise>
		</xsl:choose>

		<xsl:if test="$DEBUG = '1'">
			<xsl:variable name="d1" select="locjs:message('************* Header *************')"/>
			<xsl:variable name="d1" select="locjs:message(concat('CreationDate=',locjs:get_creation_date()))"/>
			<xsl:variable name="d1" select="locjs:message(concat('CreationTime=',locjs:get_creation_time()))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Filename=',$src_name))"/>
			<xsl:variable name="d1" select="locjs:message(concat('encoding=',../doctypeinfos/@encoding))"/>
			<xsl:variable name="d1" select="locjs:message(concat('DOMAIN=',$DOMAIN))"/>
<!--
			<xsl:variable name="d1" select="locjs:message(concat('Publication=',locjs:get_publication()))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Section=',locjs:get_section()))"/>
-->
			<xsl:variable name="d1" select="locjs:message(concat('issue_year=',locjs:get_issue_year()))"/>
			<xsl:variable name="d1" select="locjs:message(concat('issue_date=',locjs:get_issue_date()))"/>
			<xsl:variable name="d1" select="locjs:message(concat('issue_nbr=',locjs:get_issue_nbr()))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Docpath=',headerfield[@name='inputPath']/.))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Projectname=',headerfield[@name='indesignDocname']/.))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Numlayouts=','1'))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Layoutname=',headerfield[@name='indesignDocname']/.))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Docnumpages=',count(//page)))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Docpagenbr=',//page/@page_sequence))"/>
			<xsl:variable name="d1" select="locjs:message(concat('Chaptpagenbr=',//page/@page_name))"/>
<!--
			<xsl:variable name="d1" select="locjs:message(concat('Imagenames=',locjs:get_image_list()))"/>
-->
		</xsl:if>
	</xsl:template> 



	<!-- ===============================================
		 get all articles
		 =============================================== -->
	<xsl:template match="article">
		<xsl:param name="page_name"/>
		<xsl:if test="$DEBUG = '1'">
			<xsl:variable name="d1" select="locjs:message(concat('********* article_id=',@idx,' *********'))"/>
		</xsl:if>

		<xsl:variable name="textcontent"><!-- get text content for full text index -->
			<xsl:apply-templates select="content[@type='text']"/>
		</xsl:variable>
		<xsl:variable name="images"><!-- get images attached to an article -->
			<xsl:apply-templates select="descendant::img" mode="getimages"/>
		</xsl:variable>
		<xsl:variable name="imagesmeta"><!-- get attached images' meta data -->
			<xsl:apply-templates select="descendant::img" mode="getimagesmeta"/>
		</xsl:variable>
		<xsl:variable name="imagesText"><!-- get PDF image's text attached to an article -->
			<xsl:if test="$PDFtextExtract != 0">
				<xsl:variable name="tmp_imagesText"><!-- get PDF image's text attached to an article -->
					<xsl:apply-templates select="descendant::img" mode="getimagesText"/>
				</xsl:variable>
				<xsl:value-of select="locjs:cleanText(string($tmp_imagesText))"/>
			</xsl:if>
		</xsl:variable>

		<xsl:variable name="allText"><!-- normal text concatenated with PDF image's text -->
			<xsl:if test="$textcontent != ''"><xsl:value-of select="locjs:cleanText(string($textcontent))"/></xsl:if>
			<xsl:if test="$imagesText != ''">
				<xsl:text>--+PDF+--</xsl:text><!-- add signature for PDF text attached to article -->
				<xsl:value-of select="$imagesText"/>
				<xsl:text>---PDF---</xsl:text><!-- add signature for PDF text attached to article -->
			</xsl:if>
		</xsl:variable>

		<xsl:variable name="doc_src_type"><!-- determine the source of text -->
			<xsl:variable name="srcDocType">
				<xsl:choose>
					<xsl:when test="/indd_document/header/headerfield[@name='indesignDocname']/. != ''">indd</xsl:when>
					<xsl:when test="/indd_document/header/headerfield[@name='OCRversion']/. != ''">ocrXML</xsl:when>
					<xsl:otherwise>XML</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="($textcontent != '') and ($imagesText != '')"><xsl:value-of select="$srcDocType"/>+pdf</xsl:when>
				<xsl:when test="($textcontent = '') and ($imagesText != '')">pdf</xsl:when>
				<xsl:when test="($textcontent != '') and ($imagesText = '')"><xsl:value-of select="$srcDocType"/></xsl:when>
				<xsl:otherwise><xsl:value-of select="$srcDocType"/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="thesubpath">
			<xsl:choose>
				<xsl:when test="$sub_path_override != ''">
					<xsl:value-of select="$sub_path_override"/>
				</xsl:when>
				<xsl:when test="$sub_path != ''">
					<xsl:value-of select="$sub_path"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="//header/headerfield[@name='inputPath']/."/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>


		<xsl:if test="($allText != '') or ($images != '')"><!-- push to database if we have text and / or attached images -->
			<xsl:variable name="the_sourcedocumentname">
				<xsl:choose>
					<xsl:when test="//header/headerfield[@name='indesignDocname']/. != ''"><!-- get from an InDesign document -->
						<xsl:value-of select="//header/headerfield[@name='indesignDocname']/." />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="//header/headerfield[@name='filename']/." />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:if test="$DEBUG = '1'">
				<xsl:variable name="d1" select="locjs:message(concat('--- INSERT into Table: ',$dbtbl))"/>
				<xsl:variable name="d1" select="locjs:message(concat('object_id=',locjs:get_object_id()))"/>
				<xsl:variable name="d1" select="locjs:message(concat('domain=',$DOMAIN))"/>
			<!--
				<xsl:variable name="d1" select="locjs:message(concat('root_datapath=',$root_datapath))"/>
				<xsl:variable name="d1" select="locjs:message(concat('path=',$thesubpath))"/>
				<xsl:variable name="d1" select="locjs:message(concat('name_html=',locjs:re_replace(string(//header/headerfield[@name='filename']/.),'.xml','.htm','')))"/>
				<xsl:variable name="d1" select="locjs:message(concat('name_xml=',//header/headerfield[@name='filename']/.),'.xml','.htm','')))"/>
				<xsl:variable name="d1" select="locjs:message(concat('article_page=',@page_sequence))"/>
				<xsl:variable name="d1" select="locjs:message(concat('article_page_name=',$page_name))"/>
				<xsl:variable name="d1" select="locjs:message(concat('content=',$allText))"/>
				<xsl:variable name="d1" select="locjs:message(concat('doc_src_type=',$doc_src_type))"/>
				<xsl:variable name="d1" select="locjs:message(concat('src_docname=',$the_sourcedocumentname))"/>
				<xsl:variable name="d1" select="locjs:message(concat('issue_year=',locjs:get_issue_year()))"/>
				<xsl:variable name="d1" select="locjs:message(concat('issue_date=',locjs:get_issue_date()))"/>
	
				<xsl:variable name="d1" select="locjs:message(concat('creation_date=',//header/headerfield[@name='creationDate']/.))"/>
				<xsl:variable name="d1" select="locjs:message(concat('creation_time=',//header/headerfield[@name='creationTime']/.))"/>
				<xsl:variable name="d1" select="locjs:message(concat('author=',//header/headerfield[@name='creator']/.))"/>
				<xsl:variable name="d1" select="locjs:message(concat('title=',//header/headerfield[@name='title']/.))"/>
				<xsl:variable name="d1" select="locjs:message(concat('description=',//header/headerfield[@name='description']/.))"/>
				<xsl:variable name="d1" select="locjs:message(concat('rights=',//header/headerfield[@name='rights']/.))"/>
				<xsl:variable name="d1" select="locjs:message(concat('subject=',//header/headerfield[@name='subject']/.))"/>
				-->
			</xsl:if>

			<xsl:choose>
				<xsl:when test="$writeTextFileOnly != '0'">
					<xsl:variable name="created" select="myutils:writeFile(concat($INPUT_PATH,'extr.txt'),string($allText),1)"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:variable name="sqlretval">
						<sql:insert table="{$dbtbl}" 
								object_id ="{locjs:get_object_id()}"
								domain ="{$DOMAIN}"
								root_datapath ="{$root_datapath}"
								path ="{$thesubpath}"
								name_html ="{locjs:re_replace(string(/indd_document/header/headerfield[@name='filename']/.),'.xml','.htm','')}"
								name_xml ="{//header/headerfield[@name='filename']/.}"
								article_id ="{@idx}"
								prev_id ="{@previousid}"
								cont_id ="{@continuedid}"
								article_page ="{@page_sequence}"
								article_page_name ="{$page_name}"
								content="{$allText}"
								doc_src_type="{$doc_src_type}"
								src_docname="{$the_sourcedocumentname}"
								issue_year="{locjs:get_issue_year()}"
								issue_date="{locjs:get_issue_date()}"
								creation_date="{/indd_document/header/headerfield[@name='creationDate']/.}"
								creation_time="{/indd_document/header/headerfield[@name='creationTime']/.}"
								author="{/indd_document/header/headerfield[@name='creator']/.}"
								title="{/indd_document/header/headerfield[@name='title']/.}"
								description="{/indd_document/header/headerfield[@name='description']/.}"
								rights="{/indd_document/header/headerfield[@name='rights']/.}"
								subject="{/indd_document/header/headerfield[@name='subject']/.}"
								images="{$images}"
								imagesmeta="{$imagesmeta}"
								/>
					</xsl:variable>
					<xsl:variable name="d" select="myutils:showMess(concat('INSERT into ',$dbtbl,'= object id: ',locjs:get_object_id(),', article idx: ',@idx,'&#10;'),'1')"/>
					<xsl:if test="$DEBUG = '1'">
						<xsl:variable name="d1" select="locjs:message(concat('--- sqlretval: ',$sqlretval))"/>
						<xsl:variable name="d1" select="locjs:message('*******************************')"/>
					</xsl:if>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>

	</xsl:template> 

	<xsl:template match="img" mode="getimages">
		<xsl:value-of select="@src"/><xsl:text>,</xsl:text><xsl:value-of select="@alt"/><xsl:text>;</xsl:text>
	</xsl:template> 

	<xsl:template match="img" mode="getimagesmeta">
		<xsl:if test="metadata">	<!-- do we have and element name 'metadata'? (new since V 20.01) then create a string delimited with //-// and //+// as line end for each image like:
									imagefilename.jpg//-//meta text of title//-//meta description text//-//meta copyright text//+//
									imagefilename2.jpg//-//meta text of title//-//meta description text//-//meta copyright text//+//
									 -->
			<xsl:value-of select="@src"/><xsl:text>//-//</xsl:text><xsl:value-of select="metadata/title/."/><xsl:text>//-//</xsl:text><xsl:value-of select="metadata/description/."/><xsl:text>//-//</xsl:text><xsl:value-of select="metadata/author/."/><xsl:text>//+//</xsl:text>
		</xsl:if>
	</xsl:template> 

	<xsl:template match="img" mode="getimagesText">
		<xsl:if test="contains(@alt,'.pdf') or contains(@alt,'.PDF')">
			<xsl:choose>
				<xsl:when test="($PDFexcludeTextExtractLabel1 != '') and contains(../@label,$PDFexcludeTextExtractLabel1)"></xsl:when><!-- don't extract text -->
				<xsl:when test="($PDFexcludeTextExtractLabel2 != '') and contains(../@label,$PDFexcludeTextExtractLabel2)"></xsl:when><!-- don't extract text -->
				<xsl:when test="($PDFexcludeTextExtractLabel3 != '') and contains(../@label,$PDFexcludeTextExtractLabel3)"></xsl:when><!-- don't extract text -->
				<xsl:otherwise>
					<xsl:variable name="pdfpath" select="concat($INPUT_PATH,@alt)"/>
					<xsl:if test="$DEBUG = '1'">
						<xsl:variable name="d" select="myutils:showMess(concat('**** Text extract from PDF image: ',$pdfpath,'&#10;'))"/>
					</xsl:if>
					<!--xsl:variable name="pdftext" select="myutils:PDFtextExtract(string($pdfpath),$DEBUG)"/-->
					<xsl:variable name="pdftext" select="myutils:PDFtextExtract(string($pdfpath),0,100000000,0,'UTF-8','',$DEBUG)"/>
					<xsl:if test="$pdftext != ''">
						<xsl:value-of select="$pdftext"/>
					</xsl:if>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template> 

	<xsl:template match="div">
		<xsl:apply-templates/>
		<xsl:text>&#x0d;</xsl:text>
	</xsl:template> 


	<xsl:template match="table"><xsl:text>&#x0d;</xsl:text><xsl:apply-templates/><xsl:text>&#x0d;</xsl:text></xsl:template>
	<xsl:template match="tr"><xsl:apply-templates/><xsl:text>&#x0d;</xsl:text></xsl:template>
	<xsl:template match="td"><xsl:apply-templates/><xsl:text>&#x09;</xsl:text></xsl:template>


	<!-- ===============================================
		 the text content
		 =============================================== -->
	<xsl:template match="text()">
		<xsl:choose>
			<xsl:when test=". = '&#x0d;&#x0a;'"></xsl:when>
			<xsl:when test=". = '&#x0a;'"></xsl:when><!-- remove any combination of line breaks -->
			<xsl:when test=". = '&#x0d;'"></xsl:when>
			<xsl:when test=". = ' '"><xsl:text> </xsl:text></xsl:when>
			<xsl:when test=". = '  '"><xsl:text> </xsl:text></xsl:when>
			<xsl:when test=". = '   '"><xsl:text> </xsl:text></xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="."/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- ===============================================
		 ignore all other
		 =============================================== -->
	<xsl:template match="*"><xsl:apply-templates/></xsl:template>

	<!-- **************** textshortcuts are retrieved directly -->
	<xsl:template match="textshortcut"></xsl:template> 




	<!-- **************** character formats stuff -->
	<xsl:template match="br"><xsl:text>&#x0d;</xsl:text></xsl:template> 






</xsl:stylesheet>
