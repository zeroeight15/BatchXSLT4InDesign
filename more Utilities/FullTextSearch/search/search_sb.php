<?php
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
THE PRODUCER:
Andreas Imhof
EDV-Dienstleistungen
CH-Guemligen, Switzerland
www.aiedv.ch

Version: 2.0
Version date: 20140810

Multibyte support must be enabled in PHP!
In the php.ini file, the line

extension=php_mbstring.dll

must be uncommented

HOW TO CALL:
------------
search_sb.php?getobjects
		request the list of all publications stored in the objects table
		returns: a NEW LINE "\n" and double semicolon separated list like:
			FAMW;;Faucheuses;;Agriculture;;Faucheuses Gaspardo et Marangon;;1008;;Aebi Suisse;;;;fr_FR;;
			FASP;;Bêcheuses;;Agriculture;;Bêcheuses Gramegna;;1009;;Aebi Suisse;;;;fr_FR;;
			FAGF;;Citerns à pression;;Agriculture;;Citernes à pression Kirchner;;1010;;Aebi Suisse;;;;fr_FR;;
	search_sb.php?action=getobjects&ol=%A__&lang=fr
		returns same list restricted object LIKE (ol=use LIKE syntax with % and _) and restricted to language LIKE fr%

search_sb.php?action=getobjectsissues
		request the list of all publications/issues stored in the flipbook_1 table
		returns: a CR and comma separated list with objects names, ID, and path to HTML file like:
			"Gazette de Vaucluse","GV","1848","01","18480119","http://domain.com","books/DATA/","Gazette de Vaucluse/GV/1848/18480119/","GV_18480119_N0317.htm"
			...
			"Illustrirte Zeitung","IZ","1856","01","18560101","http://domain.com","books/DATA/","Illustrirte Zeitung/IZ/1856/18560101/","ZI_18560101_N0652.htm"
			"L'UNIVERS ILLUSTRÃ‰","UI","1858","09","18580918","http://domain.com","books/DATA/","Univers Illustre/UI/1858/18580918/","UI_18580918_N0018.htm"
			....
			"L'UNIVERS ILLUSTRÃ‰","UI","1858","05","18580522","http://domain.com","books/DATA/","Univers Illustre/UI/1858/18580522/","UI_18580522_N0001.htm"

	search_sb.php?action=getobjectsissues&o=UI
		returns same list restricted to the stated object ID

search_sb.php?action=getobjectsissuesdrops
		request a standalone HTML drop down menu of all publications/issues stored in the flipbook_1 table

		search_sb.php?action=getobjectsissuesdrops&o=UI
				request a standalone HTML drop down menu restricted to the given object ID

search_sb.php?action=getobjectsissuesdrop	( no ending 's' )
		request a drop down menu like 'getobjectsissuesdrops' but no CSS and Javascript included

search_sb.php?action=getobjectpn&sd=18580911&o=UI
		request previous and next issue
		returns: a CR and comma separated list like:
			"PREV","UI","18580904","http://domain.com","books/DATA/","Univers Illustre/UI/1858/18580904/","UI_18580904_N0016.htm"
			"NEXT","UI","18580918","http://domain.com","books/DATA/","Univers Illustre/UI/1858/18580918/","UI_18580918_N0018.htm"
			where "PREV" or "NEXT" state if this info is for previous or next issue
		if no object is given, all publications will be searched

===================================
*/
if (function_exists('mysql_set_charset') === false) {
	echo ("The full-text search module does not run with the current PHP Version " . phpversion() . "<br>");
	echo ("<b>You need PHP 5.2.3 and newer!</b>");
	exit();
}

// we have to check if this search window was called with 'www.'
// if NOT, then we have to remove 'www.' from any links
$http_host = $_SERVER['HTTP_HOST'];
$http_host_remove_www = true;
$http_host_www_pos = strpos($http_host, "www.");
if ($http_host_www_pos === false) $http_host_remove_www = true;
elseif ($http_host_www_pos >= 0) $http_host_remove_www = false;

/* flag to return search results to html files with/ or eithout filename ( path only)
	default = 0 to return full path and name.
	set to 1 to drop file names by default */
$nofilename = 0;
if (isset($_GET['nfn']) && is_numeric($_GET['nfn'])) $nofilename = intval($_GET['nfn']);
else if (isset($_POST['nfn']) && is_numeric($_POST['nfn'])) $nofilename = intval($_POST['nfn']);


/* special action to perform: empty or *ALL* for all objects, the 'object_id' to search one object only */
$requestAction = "";
if (isset($_GET['action'])) { 
	$requestAction = $_GET['action'];	// get the selected object, called with URL like: http://.../search_sb.php?action=whatever
}

/* the object to search: empty or *ALL* for all objects, the 'object_id' to search one object only */
$objects_select = "";
if (isset($_GET['o'])) { 
	$objects_select = $_GET['o'];	// get the selected object, called with URL like: http://.../search_sb.php?o=myobject_id
}
if ($objects_select == "") {	// get object from form
	if (isset($_POST['objects_select'])) { 
		$objects_select = $_POST['objects_select'];
	}
}

/* the object to search: empty or *ALL* for all objects, the 'object_id' to search one object only */
$objects_select_like = "";
if (isset($_GET['ol'])) { 
	$objects_select_like = $_GET['ol'];	// get the selected objects, called with URL like: http://.../search_sb.php?ol=DB% (get all objects starting with 'DB'
}
if ($objects_select_like == "") {	// get object from form
	if (isset($_POST['objects_select_like'])) { 
		$objects_select_like = $_POST['objects_select_like'];
	}
}

/* an optional start date */
$search_start_date = "";
if (isset($_GET['sd'])) { 
	$search_start_date = $_GET['sd'];	// get the start date, called with URL like: http://.../search_sb.php?sd=YYYYMMDD
}
if ($search_start_date == "") {	// get search start date field from form
	if (isset($_POST['ft_searchstartdate_field'])) { 
		$search_start_date = $_POST['ft_searchstartdate_field'];
	}
}
$search_start_date_orig = $search_start_date;
if ($search_start_date != "") $search_start_date = str_pad ( $search_start_date, 8, "0", STR_PAD_RIGHT);
/* an optional end date */
$search_end_date = "";
if (isset($_GET['ed'])) { 
	$search_end_date = $_GET['ed'];	// get the end date, called with URL like: http://.../search_sb.php?ed=YYYYMMDD
}
if ($search_end_date == "") {	// get search end date field from form
	if (isset($_POST['ft_searchenddate_field'])) { 
		$search_end_date = $_POST['ft_searchenddate_field'];
	}
}
$search_end_date_orig = $search_end_date;
if ($search_end_date != "") {
	$search_end_date = str_pad ( $search_end_date, 8, "9", STR_PAD_RIGHT);
}


/* the text terms to search for */
$ft_searchstr = "";
if (isset($_GET['q'])) { 
	$ft_searchstr = $_GET['q'];	// GET the query string to find, called with URL like: http://.../search_sb.php?q=querytext*
}
if ($ft_searchstr != "") {	// decode it
	$ft_searchstr = urldecode($ft_searchstr);
}
if ($ft_searchstr == "") {	// get search text field from form
	if (isset($_GET['ft_searchentry_field'])) { 
		$ft_searchstr = $_GET['ft_searchentry_field'];
	}
	if ($ft_searchstr == "") {	// get search text field from form
		if (isset($_POST['ft_searchentry_field'])) { 
			$ft_searchstr = $_POST['ft_searchentry_field'];
		}
	}
}

/* the number of results to return */
$num_results = "";
if (isset($_GET['n'])) {
	$num_results = $_GET['n'];	// get num_results, called with URL like: http://.../search_sb.php?o=myobject_id
}
if ($num_results == "") {	// get from form
	if (isset($_POST['num_results'])) { 
		$num_results = $_POST['num_results'];
	}
}
$language_override = "";
if (isset($_GET['lang'])) {
	$language_override = $_GET['lang'];	// get num_results, called with URL like: http://.../search_sb.php?o=myobject_id
}

/* resize search window */
$resize_search_window = "";
if (isset($_GET['r'])) {
	$resize_search_window = $_GET['r'];	// if set to '1' then search window will be resized to standard size
}


$target_callbackID = "";
if (isset($_GET['tcb'])) { 
	$target_callbackID = $_GET['tcb'];	// get the target ID to call in a callback script 'get_query_result'
}
if ($target_callbackID == "") {	// get from form
	if (isset($_POST['target_cb'])) { 
		$target_callbackID = $_POST['target_cb'];
	}
}

$classPrefix = "";	// a prefix to add to classnames
if (isset($_GET['cpfix'])) { 
	$classPrefix = $_GET['cpfix'];	// get the target ID to call in a callback script 'get_query_result'
}
if ($classPrefix == "") {	// get from form
	if (isset($_POST['classname_prefix'])) { 
		$classPrefix = $_POST['classname_prefix'];
	}
}

$nohead = false;	// do not write HTML header and body tags
if (isset($_GET['nohead'])) { 
	$nohead = true;
}
if ($classPrefix == "") {	// get from form
	if (isset($_POST['nohead'])) { 
		$nohead = true;
	}
}


/* main vars - overwritten by vars.php */
$DEBUG = 0;
$username = "";
$password = "";
$hostname = "";
$dbname = '';
$ft_tablename = '';
$objects_tablename = '';
$browser_preferred_language = '';	// set to language code like 'en' 'fr' 'de' ... or leave empty to detect browser 's preferred language
$max_query_results = 100;			// number full-text search results to show
$max_query_chars = 300;				// number of character to show in full-text search results
$mark_results_color = "#ffff00";	// the color to use to mark search terms in result. empty to not mark results
$include_image_thumbs = 1;			// if article has attached image(s) show thumbnail
$thumbs_height = "60px";			// the height of thumbnail or empty for original size
$max_query_result_title = 70;		// number of characters to show as title in a query result

$ft_search_excl_article_page = "";	// empty to include results from all pages
									// 1 to exclude results from article_page 1 (page sequence)
									// "1,3,17" to exclude results from article_page 1, 3 and 17 (page sequence)

/* override document url and paths */
$override_domain = "";				// set to new domain: the 'domain' field from the flipbook_x table will be replaced with this value
$override_root_datapath = "";		// set to new root data path: the 'root_datapath' field from the flipbook_x table will be replaced with this value
$override_path = "";				// set to new path: the 'path' field from the flipbook_x table will be replaced with this value

$default_publication_language = "en_US";	// the default language to use if not stated in the Objects table
$date_formattings = array(""=>"%e. %b. %Y", "en_US" => "%b %e, %Y");	// how to format an publication date like '20120823' depending on the locale like "en_US" or "de_DE"
																	// first empty locale is the default format

include "inc/vars.php";				// overwrite vars with external configurable values

include "inc/localized.php";		// include localized strings

if ( ($num_results != "") && ((int)$num_results > 0) ) {
	$max_query_results = $num_results;
}

// detect in which language we should talk to user
$accept_language = $_SERVER['HTTP_ACCEPT_LANGUAGE'];
$browser_preferred_language_code = 0;	// language code (index) default to english

if ($language_override != "") $browser_preferred_language = $language_override;
if ($browser_preferred_language == "") {
	$browser_preferred_language = detectLanguage($accept_language);
}
if ($browser_preferred_language != "") {
	$browser_preferred_language_code = detectLanguageCode($browser_preferred_language);
}
$blc = $browser_preferred_language_code;	// browser_preferred_language_code shorter

// some more work variables
$errtext = "";
$mysqlerrnum = 0;
$mysqlerrtext = "";
$mysqlstate = "";
$retval = 0;

$result = null;
$con = null;
$objectsList = "";
$objectsIssuesList = "";
$objects_available_arr = array();

do {
	// get a list of available objects from 'objects' table
	if ($requestAction == 'getobjects') {
		$objOK = getObjectsList();
		header('Content-Type: text/plain; charset=utf-8');
		//echo ("objects_select: '" . $objects_select . "'\n");
		//echo ("objects_select_like: '" . $objects_select_like . "'\n");
		if ($objOK != 0) {
			echo("##### ERROR:\nError#:" . $mysqlerrnum . "\n" . $mysqlerrtext . "\n\n" . $errtext);
		}
		else echo($objectsList);
		return;
	}
	// get a list or dropdown menu of available objects/issuedates from 'flipbook_1' table
	if (($requestAction == 'getobjectsissues')
		|| ($requestAction == 'getobjectsissuesdrop')	/* the menu HTML code only */
		|| ($requestAction == 'getobjectsissuesdrops')	/* standalone HTML5 page including head, styles, scripts and menu */
		) {
		$objOK = -1;
		if ($requestAction == 'getobjectsissuesdrop') $objOK = getObjectsIssuesList("drop",0);			// the menu HTML code only
		else if ($requestAction == 'getobjectsissuesdrops') $objOK = getObjectsIssuesList("drop",1);	// standalone HTML5 page including head, styles, scripts and menu
			 else $objOK = getObjectsIssuesList("list",0);
		if ($objOK != 0) {
			echo("##### ERROR:\nError#:" . $mysqlerrnum . "\n" . $mysqlerrtext . "\n\n" . $errtext);
		}
		else echo($objectsIssuesList);
		if ($target_callbackID != "") {
			echo ("<script type=\"text/javascript\">parent.get_query_result(\"" . $target_callbackID . "\",true);</script>");
		}
		return;
	}

	// get previous and next issue for a given issue date from 'flipbook_1' table
	if ($requestAction == 'getobjectpn') {
		$pnresult = getobjectpreviousnext($search_start_date, $objects_select);
		if ($pnresult != "") echo($pnresult);
		else {
			echo($mysqlerrtext);
		}
		return;
	}
	
	// no special action than search
	// ouptut the html header
	writeResponseHeader();
	//echo($accept_language . "<br>" . $browser_preferred_language . "<br>" . $blc . "<br>");

	if ($ft_searchstr == "") {
		echo("<div id=\"".$classPrefix."sb_ftresult_OKmessage_FTSS\">*** Empty Search but Fulltext Search System Reachable.</div>");
	}

	$con = mysql_connect($hostname, $username, $password, true);
	if (!$con) {
		$mysqlerrnum = mysql_errno();
		$mysqlerrtext = mysql_error();
		//$mysqlstate = mysql_sqlstate();
		$errtext = "Connection to MySQL failed: " . $mysqlerrtext;
		break;
	}
	if (function_exists('mysql_set_charset') === true) {
		$retval = mysql_set_charset ( "utf8", $con );
	}
	$db_selected = mysql_select_db($dbname, $con);
	if (!$db_selected) {
		$mysqlerrnum = mysql_errno();
		$mysqlerrtext = mysql_error();
		//$mysqlstate = mysql_sqlstate();
		$errtext = "Database '" . $dbname . "' could not be opened: " . $mysqlerrtext;
		break;
	}
	if ($ft_searchstr == "") {
		if ($DEBUG > 0) echo("<div id=\"".$classPrefix."sb_ftresult_OKmessage_DBAC\">*** Database '" . $dbname . "' accessible.</div>");
		break;
	}

	// expand search strings to be used with fulltext MATCH or LIKE
	$queryMODE = 0;	// 0 = select using MATCH, 1 = select using MATCH AND LIKE
	do {
		//echo("queryMODE: '".$queryMODE."'<br>");
		$hadError = false;
		$myft_searchstr = $ft_searchstr;
		$myft_likeArr = array();
		// ----- simply comment this block if no expanded search is wanted
		$quotepos = mb_strpos($myft_searchstr, "\"");
		$pluspos = mb_strpos($myft_searchstr, "+");
		$minuspos = mb_strpos($myft_searchstr, "-");
		$starpos = mb_strpos($myft_searchstr, "*");
		//echo("myft_searchstr: ".$myft_searchstr."<br>");
		if (($quotepos===false) && ($pluspos===false) && ($minuspos===false) || ($starpos >= 0)) {
			$myft_searchstr_arr = mb_split(" ",$ft_searchstr);
			//echo("num terms: ".count($myft_searchstr_arr)."<br>");

			if ($queryMODE == 1) {	// use LIKE
				for ($i = 0; $i < count($myft_searchstr_arr); $i++) {
					//echo("myft_searchstr $i: '".$myft_searchstr_arr[$i]."' - starpos: ".$starpos."<br>");
					$myft_likeArr[] = cleanTerm($myft_searchstr_arr[$i]);	// add to the LIKE match but remove *.+-<>~
					$myft_searchstr_arr[$i] = "";	// empty
				}
				$myft_searchstr_arr = array();	// clear the MATH serach array
			}
		
			//re-concat the MATCH search string
			//echo("num myft_searchstr_arr: ".count($myft_searchstr_arr)."<br>");
			$myft_searchstr = "";
			for ($i = 0; $i < count($myft_searchstr_arr); $i++) {
				if ($myft_searchstr_arr[$i] == "") continue;
				//echo("concat: '".$myft_searchstr_arr[$i]."'<br>");
				if ($myft_searchstr != "") $myft_searchstr .= " ";
				$myft_searchstr .= $myft_searchstr_arr[$i] . (endsWith($myft_searchstr_arr[$i],"*") ? "" : "*");
			}
		}
		// ------ end expand search
		$myft_searchstr = mysql_real_escape_string($myft_searchstr);

		$time_start = microtime(true);
	
		$query_string = "SELECT";
		if ($myft_searchstr != "") 
				$query_string .= " MATCH (content,imagesmeta) AGAINST (\"" . $myft_searchstr . "\" IN BOOLEAN MODE) AS score,";
		else	$query_string .= " 1 AS score,";
							$query_string .= " domain, root_datapath, path, name_html, name_xml, article_id, article_page, title, description, issue_date, content, images, object_id, imagesmeta " .
							" FROM " . $ft_tablename;
							$where_set = false;
							if ($myft_searchstr != "") {
								$query_string .= " WHERE (MATCH (content,imagesmeta) AGAINST (\"" . $myft_searchstr . "\" IN BOOLEAN MODE))";
								$where_set = true;
							}
							if (count($myft_likeArr) > 0) {
								$where_doend = false;
								$or_doend = false;
								if ($where_set === false) {	// add WHERE clause
									$query_string .= " WHERE (";
									$where_set = true;
									$where_doend = true;
								}
								else {
									$query_string .= " OR (";
									$or_doend = true;
								}
								for ($mi = 0; $mi < count($myft_likeArr); $mi++) {
									$query_string .= ($mi==0 && $where_set ? "" : " OR ") . "(content LIKE \"%" . $myft_likeArr[$mi] . "%\")";
								}
								if ($where_doend === true) {	// close WHERE clause
									$query_string .= ")";
								}
								if ($or_doend === true) {	// close OR clause
									$query_string .= ")";
								}
							}
							if ( ($objects_select != "*ALL*") && ($objects_select != "") ) $query_string = $query_string . " AND (object_id = \"" . $objects_select . "\")";
							if ( $search_start_date != "" ) $query_string = $query_string . " AND (CONVERT(issue_date,BINARY) >= CONVERT(\"" . $search_start_date . "\",BINARY))";
							if ( $search_end_date != "" ) $query_string = $query_string . " AND (CONVERT(issue_date,BINARY) <= CONVERT(\"" . $search_end_date . "\",BINARY))";
							if ( $ft_search_excl_article_page != "" ) $query_string = $query_string . " AND (article_page NOT IN (" . $ft_search_excl_article_page . "))";
							$query_string .= " ORDER BY score DESC, issue_date DESC";
							$query_string .= " LIMIT " . $max_query_results ;	/* do not end query string with a semicolon! */
		//echo($query_string."<br>");

		$result = mysql_query($query_string, $con);
		if (!$result) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			//$mysqlstate = mysql_sqlstate();
			$errtext = "Error in query string:<br>" . $query_string . "<br>ERROR: " . $mysqlerrtext;
			$hadError = true;
			break;
		}

		/* return the result */
		$num_rows = mysql_num_rows($result);
		if ($num_rows > 0) {
			break;
		}
		if ($queryMODE == 0) {
			$queryMODE = 1;	// 0 = select using MATCH, 1 = select using MATCH AND LIKE
			//echo("num_rows: " .$num_rows." - queryMODE: " .$queryMODE."<br>");
			continue;
		}

		if ($num_rows <= 0) {
			$errtext = $ta[6][$blc] . "'" . $ft_searchstr . "'";
			$hadError = true;
			break;
		}
		break;
	} while(true);
	if ($hadError == true) break;
	$time_end = microtime(true);
	$time_used = null;
	try {
		$time_used = round($time_end - $time_start,2);
	} catch (Exception $ex) {
	}


	// ouptut the number of results info
	echo("<div id=\"".$classPrefix."ft_result\">");
	echo("<div class=\"".$classPrefix."ft_num_results_info\">". $ta[8][$blc] . $num_rows . ($time_used ? " (" . $time_used . "s)" : ""));
	if ($search_start_date_orig != "" || $search_end_date_orig != "") echo("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
	if ($search_start_date_orig != "") echo($search_start_date_orig);
	if ($search_end_date_orig != "") echo("&nbsp;-&nbsp;" . $search_end_date_orig);
	else if ($search_start_date_orig != "") echo ("&nbsp;>");
	echo("</div>");
	// ouptut the result container header
	writeFtResultHeader();
	// ouptut the result rown
	$i = 0;
	while ($i < $num_rows) {
		/*
		$row[0] : score
		$row[1] : domain
		$row[2] : root_datapath
		$row[3] : path
		$row[4] : name_html
		$row[5] : name_xml
		$row[6] : article_id
		$row[7] : article_page
		$row[8] : title
		$row[9] : description
		$row[10] : issue_date
		$row[11] : content
		$row[12] : images
		$row[13] : object_id
		$row[14] : imagesmeta
		*/
		$row = mysql_fetch_row($result);
		// turn some special chars
		$content = $row[11];
		if ($content != null) {
			$content = mb_eregi_replace("<", "&lt;", $content);
			$content = mb_eregi_replace(">", "&gt;", $content);
			$content = mb_eregi_replace("--\+PDF\+--", "", $content);
			$content = mb_eregi_replace("---PDF---", "", $content);
		}
		else $content = "";
		$imagesmeta = $row[14];	// store image meta data
		if (!empty($imagesmeta)) {	// meta data for images are in string like:
									// ZI_18560105_N0653_0037_Picture2.jpg//-//Seni vor der Leiche Wallenstein's//-//von Karl Piloty//+//....next image metadata//+//
									//    image file  name                //-//IPTC title                       //-//IPTC description//+//
			// for each image meta data string split meta description and title
			$allimages_metas = mb_split("//\+//",$imagesmeta);	// get metadata for all images
			for ($im = 0; $im < count($allimages_metas); $im++) {
				if (empty($allimages_metas[$im])) continue;
				$image_metaparts = mb_split("//-//",$allimages_metas[$im]);	// get metadata for this image
				$meta_title = $image_metaparts[1];
				$meta_description = $image_metaparts[2];
				if (!empty($content)) $content .= '<br>';
				$content .= $meta_title . " // " . $meta_description;
			}
		}
		$imagepath = "";
		$image = "";
		if (($row[12] != null) && ($row[12] != "")) {	// image.jpg,image.eps;image2.jpg,image2.eps;
			$images_arr = mb_split(";", $row[12]);
			if (count($images_arr) > 0) {
				$image_arr = mb_split(",", $images_arr[0]);
				$image = $image_arr[0];
			}
		}
		echo ("<div class=\"".$classPrefix."ft_item" . is_odd($i) . "\">");
		// create an url to document
		$link = "";
		$thedomain = "";
		if ($override_domain != "") $thedomain = $override_domain;	// get domain
		else $thedomain = $row[1];
		if ($http_host_remove_www == true) {	// if we were called without 'www.' then we have to remove 'www.' from domain
			$re = "//www.";
			$thedomain = mb_eregi_replace ( $re, "//", $thedomain );
		}
		$link = $link . $thedomain;
		// check if domain contains protocol like http://
		$pos = strpos($link, "://");
		if ($pos === false) $link = "http://" . $link;
		if (endsWith($link, "/") == false) $link = $link . "/";	// check ending with slash

		if ($override_root_datapath != "") $link = $link . $override_root_datapath;	// add root_datapath
		else {
			$root_datapath = $row[2];
			if (startsWith($root_datapath, "/") == true) $root_datapath = mb_substr($root_datapath, 1);	// remove leading '/'
			$link = $link . $root_datapath;
		}
		if (endsWith($link, "/") == false) $link = $link . "/";	// 

		if ($override_path != "") $link = $link . $override_path;	// add path
		else {
			$path = $row[3];
			if (startsWith($path, "/") == true) $path = mb_substr($path, 1);	// remove leading '/'
			$link = $link . $path;
		}
		if (endsWith($link, "/") == false) $link = $link . "/";	// 
		
		$fullpath = $link;	// the http://www.domain.com/path/to/data/2008/20080102/

		if ($nofilename == 0) $link .= $row[4];		// add html name
		$link = $link . "?p=" . $row[7];			// add page_sequence query
		$link = $link . "&amp;a=" . $row[6];		// add article_id query

		$link_title = shortenText($content, 0, $max_query_result_title);		// get content part as title
		$re = "\r";
		$link_title = mb_eregi_replace ( $re, " - ", $link_title );

		if ($link_title == "") $link_title = $row[8];	// get title
		if ($link_title == "") $link_title = $row[9];	// get description
		if ($link_title == "") $link;	// ok... ad link
		//echo ("<a class=\"".$classPrefix."ft_link\" target=\"BXSLTflipWin\" onclick=\"return get_page_article('" . $link . "');\" href=\"" . $link . "\">" . $link_title . "</a><br>\n");
		$year = "";
		$month = "";
		$day = "";
		$date_formated = "";
		if ($row[10] != "") {
			$year = substr($row[10],0,4);
			$month = substr($row[10],4,2);
			$day = substr($row[10],6,2);
			//$date_formated = "&nbsp;&nbsp;&nbsp;" . $year . "." . $month . "." . $day;	// YYYY.MM.DD
			$date_formated = "&nbsp;&nbsp;&nbsp;" . $day . "." . $month . "." . $year;	// DD.MM.YYYY
		}
		if ($image != "") {
			$imagepath = $fullpath . $image;
			echo("<table class=\"".$classPrefix."ft_result_table\"><tr><td class=\"".$classPrefix."ft_result_td\">");
			echo("<a class=\"".$classPrefix."ft_link\" title=\"" . $row[10] . "\" onclick=\"return get_page_article('" . $link . "');\" href=\"#\"><img class=\"".$classPrefix."ft_result_img\"" . ($thumbs_height != "" ? (" style=\"height:" . $thumbs_height . ";\"") : "") . " src=\"" . $imagepath . "\" alt=\"" . $image . "\"></a>");
			echo("</td><td class=\"".$classPrefix."ft_result_td\">");
		}
		echo ("<a class=\"".$classPrefix."ft_link\" title=\"" . $row[10] . "\" onclick=\"return get_page_article('" . $link . "');\" href=\"#\">" . $link_title . "</a>" . ($queryMODE == 0 ? " (".$row[0].") " : "") . "&nbsp;" . $date_formated . "&nbsp;" . $row[13] ."<br>\n");

		$orig_contentlen = strlen($content);
		$first_match_pos = 0;
		if ($orig_contentlen > $max_query_chars) {
			$first_match_pos = mark_search_result(0, $content, $ft_searchstr, "");
			if ($first_match_pos < 0) $first_match_pos = 0;
			$content = shortenText($content, $first_match_pos, $max_query_chars);
		}
		if ($mark_results_color != "") $content = mark_search_result(1, $content, $ft_searchstr, $mark_results_color);
		//$content = $first_match_pos .":" . $content;	// debug
		echo( $content . "<br>\n"); // content
		if ($image != "") {
			echo("</td></tr></table>");
		}
		echo ("</div>\n");
		$i++;
	}
	if ($DEBUG > 0) {
		echo ("ft_searchstr: \"" . $ft_searchstr . "\"<br>");
		echo ("query_string: \"" . $query_string . "\"<br>");
	}
	echo ("</div>");	// end result container

	// ouptut the result trailer
	writeFtResultTrailer();
} while (false);

if ($DEBUG > 0) {
	echo ("ft_searchstr: \"" . $ft_searchstr . "\"<br>");
	echo ("query_string: \"" . $query_string . "\"<br>");
}

if ($errtext != "") {
	global $classPrefix;
	echo ("<div id=\"".$classPrefix."sb_ftresult_ERRmessage_MSG\">" . $errtext . "</div>");
}
// ouptut the html trailer
writeResponseTrailer();

/* free the result and close connection */
if ($result) mysql_free_result($result);
if ($con) mysql_close($con);
exit(0);



// get a list of available objects from 'objects' table
function getObjectsList() {
	global $ta, $blc;
	global $dbname, $objects_tablename, $hostname, $username, $password, $mysqlerrnum, $mysqlerrtext, $errtext, $objectsList;
	global $language_override, $objects_select, $objects_select_like;
	$objectsList = "";
	if ($objects_tablename == "") return(-99);
	$err = 0;
	do {
		$con = mysql_connect($hostname, $username, $password, true);
		if (!$con) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Connection to MSQL failed.";
			$err = -1;
			break;
		}
		if (function_exists('mysql_set_charset') === true) {
			$retval = mysql_set_charset ( "utf8", $con );
		}
		$db_selected = mysql_select_db($dbname, $con);
		if (!$db_selected) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Database '" . $dbname . "' could not be opened.";
			$err = -2;
			break;
		}
		
		/* do not end query string with a semicolon! */
		$query_string = "SELECT object_id, title, issue, description, view_index, company, link, language, copyright " .
							" FROM " . $objects_tablename .
							" WHERE (view_index >= 0)";
							if ($language_override != "") {
								$query_string .= " AND (language LIKE '" . $language_override . "%')";
							}
							if ($objects_select_like != "") {
								$query_string .= " AND (object_id LIKE '" . $objects_select_like . "')";
							}
							else if ($objects_select != "") {
									$query_string .= " AND (object_id='" . $objects_select . "')";
								 }
							$query_string .= " ORDER BY view_index ASC";	/* do not end query string with a semicolon! */
		$result = mysql_query($query_string, $con);
		if (!$result) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Error in query string '" . $query_string . "'";
			$err = -3;
			break;
		}
	
		/* break, if no objects defined in objects db */
		$num_rows = mysql_num_rows($result);
		//echo ("num_rows: ".$num_rows);
		if ($num_rows <= 0) {
			$err = 0;
			break;
		}

		$i = 0;
		// ouptut the result
		while ($i < $num_rows) {
			/*
			$row[0] : object_id
			$row[1] : title
			$row[2] : issue
			$row[3] : description
			$row[4] : view_index
			$row[5] : company
			$row[6] : link
			$row[7] : language
			$row[8] : copyright
			*/
			$row = mysql_fetch_row($result);
			$objectsList .= "" . $row[0] . ";;" . $row[1] . ";;" . $row[2] . ";;" . $row[3] . ";;" . $row[4] . ";;" . $row[5] . ";;" . $row[6] . ";;" . $row[7] . ";;" . $row[8] . (($i < $num_rows-1) ? "\n" : "");
			$i++;
		}
	} while(false);
	/* free the result and close connection */
	if ($result) mysql_free_result($result);
	if ($con) mysql_close($con);
	return($err);
}

// get previous and next issue for a given issue date from 'flipbook_1' table
function getobjectpreviousnext($date, $objid) {
	global $DEBUG, $ta, $blc;
	global $dbname, $objects_tablename, $ft_tablename, $hostname, $username, $password, $mysqlerrnum, $mysqlerrtext, $errtext;
	global $override_domain, $override_root_datapath;

	if ($date == "") return("*NONE*");

	$pnStr = "";
	$err = 0;
	do {
		$con = mysql_connect($hostname, $username, $password, true);
		if (!$con) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Connection to MSQL failed.";
			$err = -1;
			break;
		}
		if (function_exists('mysql_set_charset') === true) {
			$retval = mysql_set_charset ( "utf8", $con );
		}
		$db_selected = mysql_select_db($dbname, $con);
		if (!$db_selected) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Database '" . $dbname . "' could not be opened.";
			$err = -2;
			break;
		}
		
		/* get preceeding and following issues of the given issue date */
		$titlequery = " (SELECT title FROM " . $objects_tablename . " WHERE " . $ft_tablename . ".object_id = " . $objects_tablename . ".object_id) as obj_title,";
		$query_string = "(SELECT DISTINCT \"CUR\", object_id," . $titlequery . " issue_date, domain, root_datapath, path, name_html " .
								" FROM " . $ft_tablename ;
								if ($objid != "") $query_string .= " WHERE (object_id = \"" . $objid . "\") AND (issue_date = " . $date . ")";
								else $query_string .= " WHERE (issue_date = " . $date . ")";
								$query_string .= " ORDER BY issue_date DESC LIMIT 1)";
						$query_string .= " UNION " .
						"(SELECT DISTINCT \"PREV\", object_id," . $titlequery . " issue_date, domain, root_datapath, path, name_html " .
								" FROM " . $ft_tablename ;
								if ($objid != "") $query_string .= " WHERE (object_id = \"" . $objid . "\") AND (issue_date < " . $date . ")";
								else $query_string .= " WHERE (issue_date < " . $date . ")";
								$query_string .= " ORDER BY issue_date DESC LIMIT 1)";
						$query_string .= " UNION " .
						"(SELECT DISTINCT \"NEXT\", object_id," . $titlequery . " issue_date, domain, root_datapath, path, name_html " .
								" FROM " . $ft_tablename ;
								if ($objid != "") $query_string .= " WHERE (object_id = \"" . $objid . "\") AND (issue_date > " . $date . ")";
								else $query_string .= " WHERE (issue_date > " . $date . ")";
								$query_string .= " ORDER BY issue_date ASC LIMIT 1)";
		//echo($query_string . "\n");
		/* result is like:
			"CUR","UI","L'UNIVERS ILLUSTRE","18580904","http://domain.com","books/DATA/","Univers Illustre/UI/1858/18580904/","UI_18580904_N0016.htm"
			"PREV","UI","L'UNIVERS ILLUSTRE","18580828","http://domain.com","books/DATA/","Univers Illustre/UI/1858/18580828/","UI_18580828_N0015.htm"
			"NEXT","UI","L'UNIVERS ILLUSTRE","18580911","http://domain.com","books/DATA/","Univers Illustre/UI/1858/18580911/","UI_18580911_N0017.htm"
		*/
		$result_dates = mysql_query($query_string, $con);
		if (!$result_dates) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Error in query string '" . $query_string . "'";
			$err = -3;
			break;
		}
	
		/* break, if no objects defined in objects db */
		$num_rows = mysql_num_rows($result_dates);
		//echo ("num_rows: ".$num_rows);
		if ($num_rows <= 0) {
			$err = 0;
			break;
		}
		/* result rows are:
			row[0] : 'PREV or 'NEXT' or 'CUR'
			row[1] : object_id
			row[2] : obj_title
			row[3] : issue_date
			row[4] : domain
			row[5] : root_datapath
			row[6] : path
			row[7] : name_html
		*/
		$i = 0;
		while ($i < $num_rows) {
			$row = mysql_fetch_row($result_dates);
			$pnStr .= "\"" . $row[0] . "\",\"" . $row[1] . "\",\"" . $row[2] . "\",\"" . $row[3] . "\",\"";
			if ($override_domain != "") $pnStr .= $override_domain;	// get domain override
			else $pnStr .= $row[4];
			$pnStr .= "\",\"";
			if ($override_root_datapath != "") $pnStr .= $override_root_datapath;	// get root path override
			else $pnStr .= $row[5];
			$pnStr .= "\",\"" . $row[6] . "\"";
			if ($nofilename == 0) $pnStr .= ",\"" . $row[7] . "\"";		// add html name
			$i++;
			if ($i < $num_rows) $pnStr .= "\n";	// last line not end witn NEW LINE
		}

	} while(false);
	/* free the result and close connection */
	if ($result_dates) mysql_free_result($result_dates);
	if ($con) mysql_close($con);
	return($pnStr);
}


// get a list or dropdown menu of available objects/issuedates from 'flipbook_1' table
function getObjectsIssuesList($type, $htmltype) {	// $type == 'list' for a comma delimited list 
													// $type == 'drop' for a complete drop down menu
													// for $type = 'drop':
													// $htmltype = 0 : no scripts and styles embedded - the plain html menu code only
													// $htmltype = 1 : make a standalone full HTML5 page


	global $DEBUG, $ta, $blc;
	global $dbname, $objects_tablename, $ft_tablename, $hostname, $username, $password, $mysqlerrnum, $mysqlerrtext, $errtext, $objectsIssuesList, $objects_available_arr;
	global $override_domain, $override_root_datapath, $objects_select;
	global $nofilename;
	$objectsIssuesList = "";
	$objects_available_arr = array();
	if ($objects_tablename == "") return(-99);
	$err = 0;
	do {
		$con = mysql_connect($hostname, $username, $password, true);
		if (!$con) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Connection to MSQL failed.";
			$err = -1;
			break;
		}
		if (function_exists('mysql_set_charset') === true) {
			$retval = mysql_set_charset ( "utf8", $con );
		}
		$db_selected = mysql_select_db($dbname, $con);
		if (!$db_selected) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Database '" . $dbname . "' could not be opened.";
			$err = -2;
			break;
		}
		
		/* FIRST, we get information from the 'object' table about stored publications */
		
		$query_string = "SELECT object_id, title, description, language " .
							" FROM " . $objects_tablename .
							" WHERE (view_index >= 0)";
							if ($objects_select != "") $query_string .= " AND (object_id = \"{$objects_select}\")";
							$query_string .= " ORDER BY view_index ASC";	/* do not end query string with a semicolon! */
		$result_objects = mysql_query($query_string, $con);
		if (!$result_objects) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Error in query string '" . $query_string . "'";
			$err = -3;
			break;
		}
	
		/* break, if no objects defined in objects db */
		$num_rows = mysql_num_rows($result_objects);
		//echo ("num_rows: ".$num_rows);
		if ($num_rows <= 0) {
			$err = 0;
			break;
		}

		$i = 0;
		// store the result from objects table  row(s) into an associative array
		while ($i < $num_rows) {
			/*
			$row[0] : object_id
			$row[1] : title
			$row[2] : description
			$row[3] : language identifier like de_DE, en_US
			*/
			$row = mysql_fetch_row($result_objects);
			$objects_available_arr[$row[0]] = array($row[0],$row[1],$row[2],$row[3]);
			$i++;
		}
		if ($DEBUG > 0) {
			foreach ($objects_available_arr as $publication) {
				$objectsIssuesList .= $publication[0]."***" . implode("///",$publication) . "<br>\n";
			}
		}

		/* Now, in the $objects_available_arr we have an associative array
		   with 4 values: object_id [0], title [1], description [2], publication language [3]
		   which can be accessed through the object_id name */

		/* get all available publications's issues */
		$query_string = "SELECT DISTINCT object_id, issue_year, SUBSTR(issue_date,5,2) AS issue_month, issue_date, domain, root_datapath, path, name_html " .
							" FROM " . $ft_tablename;
							$idx = 0;
							foreach ($objects_available_arr as $publication) {
								if ($idx == 0) {
									$query_string .= " WHERE (object_id = \"" . $publication[0] . "\")";
									$idx++;
								}
								else $query_string .= " OR (object_id = \"" . $publication[0] . "\")";
							}
							$query_string .= " ORDER BY object_id ASC, issue_year DESC, issue_month DESC, issue_date DESC";	// do not end query string with a semicolon!
		if ($DEBUG > 0) {
			$objectsIssuesList .= $query_string;
		}

		$result_objectsissues = mysql_query($query_string, $con);
		if (!$result_objectsissues) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Error in query string '" . $query_string . "'";
			$err = -3;
			break;
		}
		// continue, if no objects - issues defined in ft_search db
		$num_rows = mysql_num_rows($result_objectsissues);
		//echo ("num_rows: ".$num_rows);
		if ($num_rows <= 0) {
			$err = 0;
			break;
		}

		// check the type of list we want to have
		if ($type == "drop") {
			PIM_publications_menu($result_objectsissues,$htmltype);
			break;
		}

		/* otherwise assume a list as default */
		/* result rows are:
			row[0] : object_id
			row[1] : issue_year
			row[2] : issue_month
			row[3] : issue_date
			row[4] : domain
			row[5] : root_datapath
			row[6] : path
			row[7] : name_html
		*/
		$i = 0;
		while ($i < $num_rows) {
			$row = mysql_fetch_row($result_objectsissues);
			if ($objects_available_arr[$row[0]] != "") {
				$objectsIssuesList .= "\"" . $objects_available_arr[$row[0]][1] . "\",\"" . $row[0] . "\",\"" . $row[1] . "\",\"" . $row[2] . "\",\"" . $row[3] . "\",\"";
				if ($override_domain != "") $objectsIssuesList .= $override_domain;	// get domain override
				else $objectsIssuesList .= $row[4];
				$objectsIssuesList .= "\",\"";
				if ($override_root_datapath != "") $objectsIssuesList .= $override_root_datapath;	// get root path override
				else $objectsIssuesList .= $row[5];
 				$objectsIssuesList .= "\",\"" . $row[6] . "\"";
				if ($nofilename == 0) $objectsIssuesList .= ",\"" . $row[7] . "\"\n";		// add html name
				else $objectsIssuesList .= ",\"\"\n";
			}
			$i++;
		}

	} while(false);
	/* free the result and close connection */
	if ($result_objects) mysql_free_result($result_objects);
	if ($result_objectsissues) mysql_free_result($result_objectsissues);
	if ($con) mysql_close($con);
	return($err);
}

function PIM_publications_menu($mysql_result, $htmltype) {	// $htmltype = 0 : no scripts and styles embedded - the plain html menu code only
															// $htmltype = 1 : make a standalone full HTML5 page
	global $ta, $blc, $objectsIssuesList, $objects_available_arr, $default_publication_language, $default_date_formats, $date_formattings;
	global $nofilename;
	$cur_url = "";
	$cur_publicationName = "";	$previous_publicationName = "";
	$cur_publicationID = "";	$previous_publicationID = "";
	$cur_year = "";				$previous_year = "";
	$cur_month = "";			$previous_month = "";
	$cur_date = "";				$previous_date = "";
	$PIM_menu_file = "";
	
	// Current date/time in your server's time zone.
	date_default_timezone_set ("Europe/Zurich");
	$timezone_identifier = date_default_timezone_get();
	//echo("date_default_timezone_get: ".$timezone_identifier . "\<br>");

	function getPublicationLanguage($objectID) {
		global $objects_available_arr, $default_publication_language;

		//echo "getPublicationLanguage: ".$objectID."<br>\n";
		for ($x=0;$x<count($objects_available_arr);$x++) {
			if ($objects_available_arr[$objectID][0] == $objectID) {
				//echo "getPublicationLanguage: ".$objectID." found in ".$objects_available_arr[$objectID][3]."<br>\n";
				return($objects_available_arr[$objectID][3]);
			}
		}
		//echo "getPublicationLanguage: ".$objectID." not found in ".implode(',',$objects_available_arr)."<br>\n";
		return($default_publication_language);
	}



	// output the menu
	if ($htmltype == 1) {
		$PIM_menu_file = file_get_contents ("PIM_Menu/PIM_Menu.php");
	}

	// include full HTML head
	if (($htmltype == 1) && ($PIM_menu_file != false)) {
		$headend = mb_strpos ( $PIM_menu_file, "<!-- /PIM_HTMHEAD -->", 0, "UTF-8" );
		$header = mb_substr ( $PIM_menu_file, 0, $headend, "UTF-8" );
		if ($header != false) {
			$objectsIssuesList .= $header;
		}
	}
	
						/*	the available publications/issues are return from the db ordered : ORDER BY object_id ASC, issue_year DESC, issue_month DESC, issue_date DESC
						$row[0] : object_id			<-- first key ASC
						$row[1] : issue_year		<-- 2nd key DESC
						$row[2] : issue_month		<-- 3rd key DESC
						$row[3] : issue_date		<-- 4th key DESC
						$row[4] : domain
						$row[5] : root_datapath
						$row[6] : path
						$row[7] : name_html
						
						Our sort/group kriterias are:
						row[0]	row[1]	row[2]	row[3]		the URL to the files
						ObjID	year	month	date		$row[4] - [7] concatenated to $cur_url below
						BZB		2012	06		20120908	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120608/BZB_20120608.indb.htm
						BZB		2012	05		20120505	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120505/BZB_20120505.indb.htm
						BZB		2012	05		20120502	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120502/BZB_20120502.indb.htm
						BZB		2012	04		20120424	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120424/BZB_20120424.indb.htm
						BZB		2012	04		20120414	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120414/BZB_20120414.indb.htm
						BZB		2012	03		20120306	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120306/BZB_20120306.indb.htm
						BZB		2012	02		20120228	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120228/BZB_20120228.indb.htm
						BZB		2012	02		20120221	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120221/BZB_20120221.indb.htm
						BZB		2012	01		20120117	http://192.168.1.36/BZB/epaper/DATA/Berner Zeitung/BZ-Beilage/BZB/2012/20120117/BZB_20120117.indb.htm
						*/

					// read all into an array
					$PI_arr = array();	// an array of arrays and will hold above rows
					$PI_arrsize = 0;
					while ($row = mysql_fetch_array($mysql_result, MYSQL_NUM)) {
						$PI_arrsize = array_push($PI_arr,$row);
					}
	// debug 
	/*
	$objectsIssuesList .= count($PI_arr)."<br>\n";
	for ($i=0;$i<count($PI_arr);$i++) {
		$cur_publicationName = $objects_available_arr[$PI_arr[$i][0]][1];
		$cur_url = concatURL($PI_arr[$i][4], $PI_arr[$i][5], $PI_arr[$i][6], ($nofilename==0 ? $PI_arr[$i][7] : ""), false);
		$objectsIssuesList .= "\"" . $cur_publicationName . "\",\"" . $PI_arr[$i][0] . "\",\"" . $PI_arr[$i][1] . "\",\"" . $PI_arr[$i][2] . "\",\"" . $PI_arr[$i][3] . "\",\"" . $cur_url . "\"<br>\n";
	}
	$objectsIssuesList .= "Total Entries in PI_arr at index: " . count($PI_arr) ."<br>\n";
	*/
	// end debug

	// insert the menu head
	$objectsIssuesList .= "<div id=\"PIM_content\">\r\n";
		$objectsIssuesList .= "<div id=\"PIM_ul_menu\" data-level=\"0\">\r\n";	// the top of the menu is the menu opener and has level 0
			$objectsIssuesList .= "<div class=\"PIM_li_title\">\r\n";
				$objectsIssuesList .= "<a class=\"PIM_a_title\" href=\"#\" title=\"" . $ta[16][$blc] . "\">" . $ta[15][$blc] . "</a>\r\n";
					// insert the menu items
					$i = 0;	// index
					$cur_publicationName = $objects_available_arr[$PI_arr[$i][0]][1]; if ($cur_publicationName == '') $cur_publicationName = "&nbsp;?";
					$cur_publicationID = $PI_arr[$i][0];
					$cur_year = $PI_arr[$i][1];
					$cur_month = $PI_arr[$i][2];
					$cur_date = $PI_arr[$i][3];
										   /* --- level 1 menu: the publication names --- */
					$objectsIssuesList .= "<div class=\"PIM_ul\" data-level=\"1\">\r\n";	/* --- menu container to hold all publications names  --- */
												while ( ($i < count($PI_arr)) ) {	// get all publication
													$objectsIssuesList .= "<div class=\"PIM_li\">\r\n";
														$objectsIssuesList .= "<a class=\"PIM_a\" href=\"#\" title=\"\">" . $cur_publicationName . "</a>\r\n";
																			   /* --- level 2 menu: the issue year. we get all years belonging to the same cur_publicationName --- */
																				$objectsIssuesList .= "<div class=\"PIM_ul\" data-level=\"2\">\r\n";	/* --- the issue years menu container --- */
																					while ( ($i < count($PI_arr)) && ($PI_arr[$i][0] == $cur_publicationID) ) {	// get all years with same cur_publicationID
																						$objectsIssuesList .= "<div class=\"PIM_li\">\r\n";
																							$objectsIssuesList .= "<a class=\"PIM_a\" href=\"#\" title=\"\">" . $cur_year . "</a>\r\n";
																												   /* --- level 3 menu: the issue months. we get all months within cur_year --- */
																													$objectsIssuesList .= "<div class=\"PIM_ul\" data-level=\"3\">\r\n";	/* --- the months container --- */
																																				while ( ($i < count($PI_arr)) && ($PI_arr[$i][0] == $cur_publicationID) && ($PI_arr[$i][1] == $cur_year) ) {	// get all months with same publicationID + year
																																					$objectsIssuesList .= "<div class=\"PIM_li\">\r\n";
																																												$objectsIssuesList .= "<a class=\"PIM_a\" href=\"#\" title=\"\">" . formatMonth($PI_arr[$i][3],$PI_arr[$i][0]) . "</a>\r\n";
																																												/* --- level 4 menu: all issues' url for this month. we get all issues within cur_month --- */
																																												$objectsIssuesList .= "<div class=\"PIM_ul_deepest\" data-level=\"4\" onmouseover=\"PIM_menu.PIM_over_curlevel=parseInt(this.datalevel);\" onmouseout=\"PIM_menu.PIM_over_curlevel = -1; setTimeout('PIM_menu.PIM_menuCloser(true)',200);\">\r\n";
																																													while ( ($i < count($PI_arr)) && ($PI_arr[$i][0] == $cur_publicationID) && ($PI_arr[$i][1] == $cur_year) && ($PI_arr[$i][2] == $cur_month) ) {	// get all records with same publicationID + year + month
																																														$cur_url = concatURL($PI_arr[$i][4], $PI_arr[$i][5], $PI_arr[$i][6], ($nofilename==0 ? $PI_arr[$i][7] : ""), false);
																																														$objectsIssuesList .= "<div class=\"PIM_li\"><a class=\"PIM_a\" href=\"" . $cur_url . "\" title=\"" . $PI_arr[$i][3] . "\">" . formatDate($PI_arr[$i][3],$PI_arr[$i][0]) . "</a></div>\r\n";
																																														$i++;	// next record belongs to same publication + year + month ?
																																													}
																																													if ($i < count($PI_arr)) $cur_month = $PI_arr[$i][2];
																																													else $cur_month = '0';
																																												$objectsIssuesList .= "</div>\r\n";	/* --- close tag level 4 menu: all issues' urls for this month --- */
																																					$objectsIssuesList .= "</div>\r\n";
																																				}
																																				if ($i < count($PI_arr)) $cur_year = $PI_arr[$i][1];
																																				else $cur_year = '0';
																													$objectsIssuesList .= "</div>\r\n";	/* --- close the months container --- */
																						$objectsIssuesList .= "</div>\r\n";
																					}
																					if ($i < count($PI_arr)) $cur_publicationID = $PI_arr[$i][0];
																					else $cur_publicationID = '';
																				$objectsIssuesList .= "</div>\r\n";	/* --- close the issue years menu container --- */
													$objectsIssuesList .= "</div>\r\n";
													if ($i < count($PI_arr)) {
														$cur_publicationName = $objects_available_arr[$PI_arr[$i][0]][1]; if ($cur_publicationName == '') $cur_publicationName = "&nbsp;";
														$cur_publicationID = $PI_arr[$i][0];
														$cur_year = $PI_arr[$i][1];
														$cur_month = $PI_arr[$i][2];
														$cur_date = $PI_arr[$i][3];
													}
												}
					$objectsIssuesList .= "</div>\r\n";	/* --- close the publications names menu container --- */

	// insert the menu trailer
			$objectsIssuesList .= "</div>\r\n";
		$objectsIssuesList .= "</div>\r\n";
	$objectsIssuesList .= "</div>\r\n";

	// debug 
	/*
	$objectsIssuesList .= "Break PI_arr at index: " . $i ."<br>\r\n";
	*/
	// end debug

	// include HTML trailer
	if (($htmltype == 1) && ($PIM_menu_file != false)) {
		$trailerstart = mb_strpos ( $PIM_menu_file , "<!-- PIM_TRAILER -->", 0, "UTF-8" );
		$trailer = mb_substr ( $PIM_menu_file, $trailerstart, 1000000, "UTF-8" );
		if ($trailer != false) {
			$objectsIssuesList .= $trailer;
		}
	}
	return;
}

function formatMonthLocale($datestr, $objectID) {	// where datestr like "201206023" (YYYYMMDD) or '2000-01-01'
											// return the month's full name
	global $default_publication_language;
	if ($default_publication_language == "") return($datestr);	// return $datestr as it is
	$str = "";
	$langID = "";
	$set_langID = null;
	$oldLocale = setlocale(LC_TIME,"0");	//store current language
	try {
		/* set locale language for this object */
		$langID = getPublicationLanguage($objectID);
		if ($langID == "") $langID = $default_publication_language;
		//echo "formatMonth: ".$langID."<br>\n";
		if (($langID != '') && ($langID != '*')) {	// do not set other locale if is *
			$langIDarr = explode(",",$langID);				// now as array like 'de_DE,deu_deu' for Unix and Windows PHP
			$set_langID = setlocale (LC_TIME, $langIDarr);	// will now be set one given in the array $langID
			//echo "formatMonth: '".$objectID."' requested ".$langID."' set to '".$set_langID."' from '".implode(',',$langIDarr)."'<br>\n";
		}
		$format = 'Ymd';	// like "201206023"
		$date = strtotime($datestr);
		$str = strftime ("%B" , $date);
		if (strtoupper(substr(PHP_OS, 0, 3)) == 'WIN') {
			$str = iconv("Windows-1252","UTF-8",$str);
		}
	} catch (Exception $ex) {
			//echo 'Exception catched: ',  $ex->getMessage(), "\n";
			if ($set_langID != null) setlocale(LC_TIME,$oldLocale);	//restore previous language
			return($datestr);
	}
	if ($set_langID != null) setlocale(LC_TIME,$oldLocale);	//restore previous language
	return($str);
}
function formatDateLocale($datestr, $objectID) {	// where datestr like "201206023" (YYYYMMDD) or '2000-01-01'
	global $default_publication_language, $date_formattings;
	
	
	if ($default_publication_language == "") return($datestr);	// return $datestr as it is
	$str = "";
	$langID = "";
	$set_langID = null;
	$oldLocale = setlocale(LC_TIME,"0");	//store current language
	try {
		/* set locale language for this object */
		$langID = getPublicationLanguage($objectID);
		if ($langID == "") $langID = $default_publication_language;
		if (($langID != '') && ($langID != '*')) {	// do not set other locale if is *
			$langIDarr = explode(",",$langID);				// now as array like 'de_DE,deu_deu' for Unix and Windows PHP
			$set_langID = setlocale (LC_TIME, $langIDarr);	// will now be set one given in the array $langID
			//echo "formatDate: '".$objectID."' requested ".$langID."' set to '".$set_langID."' from '".implode(',',$langIDarr)."'<br>\n";
		}
		$format = 'Ymd';	// like "201206023"
		$date = strtotime($datestr);
		$dateformat = $date_formattings[$set_langID];
		//echo "formatDate: '".$objectID."' requested ".$langID."' set to '".$set_langID."' from '".implode(',',$langIDarr)."' ->dateformat: '".$dateformat."'<br>\n";
		if ($dateformat == "") $dateformat = $date_formattings[""];	// get first = default format
		// Check for Windows to find and replace the %e modifier correctly by '%#d'
		if (strtoupper(substr(PHP_OS, 0, 3)) == 'WIN') {
			$dateformat = preg_replace('#(?<!%)((?:%%)*)%e#', '\1%#d', $dateformat);
		}
		if ($dateformat =="") return($datestr);	// no formatting given - return original date
		$str = strftime ($dateformat , $date);
		if (strtoupper(substr(PHP_OS, 0, 3)) == 'WIN') {
			$str = iconv("Windows-1252","UTF-8",$str);
		}
	} catch (Exception $ex) {
			//echo 'Exception catched: ',  $ex->getMessage(), "\n";
			if ($set_langID != null) setlocale(LC_TIME,$oldLocale);	//restore previous language
			return($datestr);
	}
	if ($set_langID != null) setlocale(LC_TIME,$oldLocale);	//restore previous language
	return($str);
}

function formatMonth($datestr, $objectID) {	// where datestr like "201206023" (YYYYMMDD) or '2000-01-01'
	global $default_publication_language, $date_formattings, $lang_monthnames;
	$newdatestr = "";
	$langID = "";
	$set_langID = "";

	/* set language language for this object */
	$langID = getPublicationLanguage($objectID);
	if ($langID == "") $langID = $default_publication_language;
	if (($langID == '') || ($langID == '*')) {	// do not format if is * or empty
		return($datestr);
	}
	$set_langID = $langID;
	if (mb_strlen($set_langID, "UTF-8") > 2) $set_langID = mb_substr($set_langID, 0, 2, "UTF-8");	// shorted to 2 chars en, de, fr ....
	$dateformat = $date_formattings[$set_langID];	// like "%e. %B %Y" for 22. Juni 1853

	$monthNum = mb_substr($datestr, 4, 2, "UTF-8");
	if (!$dateformat) return($monthNum);

	$month = $lang_monthnames[$set_langID][intval($monthNum)-1];

	return($month);
}

function formatDate($datestr, $objectID) {	// where datestr like "201206023" (YYYYMMDD) or '2000-01-01'
	global $default_publication_language, $date_formattings, $lang_monthnames;
	$newdatestr = "";
	$langID = "";
	$set_langID = "";

	/* set language language for this object */
	$langID = getPublicationLanguage($objectID);
	if ($langID == "") $langID = $default_publication_language;
	if (($langID == '') || ($langID == '*')) {	// do not format if is * or empty
		return($datestr);
	}
	$set_langID = $langID;
	if (mb_strlen($set_langID, "UTF-8") > 2) $set_langID = mb_substr($set_langID, 0, 2, "UTF-8");	// shorted to 2 chars en, de, fr ....
	$dateformat = $date_formattings[$set_langID];	// like "%e. %B %Y" for 22. Juni 1853
	if (!$dateformat) return($datestr);

	$year = mb_substr($datestr, 0, 4, "UTF-8");
	$monthNum = mb_substr($datestr, 4, 2, "UTF-8");
	$day = mb_substr($datestr, 6, 2, "UTF-8"); $day = "".intval($day);
	$month = $lang_monthnames[$set_langID][intval($monthNum)-1];

    $newdatestr = mb_ereg_replace('%e', $day  , $dateformat); 
    $newdatestr = mb_ereg_replace('%B', $month, $newdatestr); 
    $newdatestr = mb_ereg_replace('%Y', $year , $newdatestr); 

 	//echo("objectID: '" . $objectID. "' set_langID: " . $set_langID. " dateformat: '" . $dateformat. "' newdatestr: '" . $newdatestr ."'<br>\n");
	return($newdatestr);
}


// concat URL parts to full URL
function concatURL($domain, $root_datapath, $path, $name_html, $do_encode) {
	global $http_host_remove_www, $override_domain, $override_root_datapath, $override_path;
	$link = "";
	if ($override_domain != "") $link = $link . $override_domain;	// get domain
	else $link = $link . $domain;
	// check if domain contains protocol like http://
	$pos = strpos($link, "://");
	if ($pos === false) $link = "http://" . $link;
	if (endsWith($link, "/") == false) $link = $link . "/";	// check ending with slash

	if ($override_root_datapath != "") $link = $link . $override_root_datapath;	// add root_datapath
	else {
		$myroot_datapath = $root_datapath;
		if (startsWith($myroot_datapath, "/") == true) $myroot_datapath = mb_substr($myroot_datapath, 1);	// remove leading '/'
		$link = $link . $myroot_datapath;
	}
	if (endsWith($link, "/") == false) $link = $link . "/";	// 

	if ($override_path != "") $link = $link . $override_path;	// add path
	else {
		$mypath = $path;
		if (startsWith($mypath, "/") == true) $mypath = mb_substr($mypath, 1);	// remove leading '/'
		$link = $link . $mypath;
	}
	if (endsWith($link, "/") == false) $link = $link . "/";	// 
	
	if ($http_host_remove_www == true) {	// if the search window was called without 'www.' then we have to remove 'www.' from links
		$re = "www.";
		$link = mb_eregi_replace ( $re, "", $link );
	}

	$myname_html = $name_html;
	//if ($do_encode == true) $myname_html = rawurlencode($myname_html);
	$link = $link . $myname_html;					// add html name
	return($link);
}


function writeResponseHeader() {
	global $classPrefix, $nohead;
	if ($nohead == true) return;
?>
<!DOCTYPE html>
<html><head><title>Full-Text Search</title>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
</head><body id="<?php echo($classPrefix); ?>sb_ftresults_body">
<?php
	return;
}


function writeFtResultHeader() {
	global $classPrefix;
	echo ("<div class=\"".$classPrefix."sb_ftresults_container\">");
	return;
}

function writeFtResultTrailer() {
	echo ("</div>");
	return;
}
function writeResponseTrailer() {
	global $target_callbackID, $nohead;
	// ad notifier function to state that we have a result from DB
	if ($target_callbackID != "") {
		echo ("<div id=\"ft_resulthandler_CB\"><script type=\"text/javascript\">" . $target_callbackID . "(\"query_resultDIV\");</script></div>");
	}
	if ($nohead == false) echo ("</body></html>");
	return;
}

function is_odd( $int ) {
	return( $int & 1 );
}


function startsWith( $str, $sub ) {
	return ( substr( $str, 0, strlen( $sub ) ) === $sub );
}
function endsWith( $str, $sub ) {
	return ( substr( $str, strlen( $str ) - strlen( $sub ) ) === $sub );
}

function cleanTerm( $str ) {
	mb_internal_encoding("UTF-8");
	mb_regex_encoding("UTF-8");
	return ( mb_ereg_replace("[*.+-<>~]", '', $str) );
}

function shortenText($text, $start, $maxchars) {
	// Change to the number of characters you want to display
	if ($maxchars <= 0) return ("");
	if (mb_strlen($text) <= $maxchars) return($text);
	$adddots = false;
	$text = $text . " ";
	$text = mb_substr($text, $start, $maxchars);
	if (mb_strlen($text) >= $maxchars) $adddots = true;
	$text = mb_substr($text, 0, mb_strrpos($text,' '));

	if ($start > 0) $text = "..." . $text;
	if ($adddots) $text = $text . "...";
	return $text;
}

function detectLanguage($accept_lg) {
	if (($accept_lg == null) || ($accept_lg == "") || (strlen($accept_lg) < 2)) return("en");
	$lang = substr($accept_lg,0,2);
	return $lang;
}

function detectLanguageCode($lang) {
	if (($lang == null) || ($lang == "") || ($lang == "en") ) return(0);
	switch ($lang) {
		case "de": case "gs": return(1);	// german, schxyzerduetsch
		case "fr": return(2);	// french
		case "da": return(3);	// danish
		case "pl": return(4);	// danish
		default: return(0);		// english
	}
	return (0);
}

/**
 * if func == 1 : mark the search word within s
 * if func == 0 : get the position of the first occurence of any of the word contained in search_term within s
 */
function mark_search_result($func, $s, $search_term, $color) {
	global $classPrefix;
	if ( ($func == 1) && ($color == "") ) return($s);
	if ( ($s == null) || ($s == "") ) {
		if ($func == 1) return($s);
		else return(-1);
	}
	if ( ($search_term == null) || ($search_term == "") ) {
		if ($func == 1) return($s);
		else return(-1);
	}
	$my_s = $s;
	$re = "\*";
	$my_search_term = mb_eregi_replace ( $re, "", $search_term );
	$re = "\?";
	$my_search_term = mb_eregi_replace ( $re, "\?", $my_search_term );
	$re = "\(";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\)";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\[";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\]";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\{";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\}";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\+";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\-";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = '\\\"';
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\"";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\>";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\<";
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );
	$re = "\.";
	$my_search_term = mb_eregi_replace ( $re, "\\.", $my_search_term );
	$re = '\\\\';
	$my_search_term = mb_eregi_replace ( $re, "", $my_search_term );

	//echo("<b>'" . $search_term . "':'" . $my_search_term . "'</b><br>");

	$terms_arr = mb_split(" ", $my_search_term);
	$firstpos = -1;
	$curpos = null;
	for ($i = 0; $i < count($terms_arr); $i++) {
		//echo("<b>Term: '" . $terms_arr[$i] . "'</b><br>");
		if ( strlen($terms_arr[$i]) <= 2) continue;
		$re = $terms_arr[$i];
		$ok = mb_ereg_search_init( $my_s, $re, "gi" );
		if ($func == 1) $my_s = mb_eregi_replace  ( $re, "<span class=\"".$classPrefix."ft_emphasterm\">\\0</span>", $my_s );
		$pos = mb_ereg_search_pos  ($re, "gi");
		if (is_array($pos) && (sizeof($pos) > 0)) {
			if ($firstpos < 0) $firstpos = $pos[0];
			elseif ($pos[0] < $firstpos) $firstpos = $pos[0];
		}
	}
//$my_s = $firstpos . ":" . $my_s;
	if ($func == 1) return($my_s);
	else return($firstpos);
}

?>

