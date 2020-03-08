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
$http_host_no_www = true;
$http_host_www_pos = strpos($http_host, "www.");
if ($http_host_www_pos === false) $http_host_no_www = true;
elseif ($http_host_www_pos >= 0) $http_host_no_www = false;

/* flag to return serach results to html files with/ or eithout filename ( path only)
	default = 0 to return full path and name.
	set to 1 to drop file names by default */
$nofilename = 0;
if (isset($_GET['nfn']) && is_numeric($_GET['nfn'])) $nofilename = intval($_GET['nfn']);
else if (isset($_POST['nfn']) && is_numeric($_POST['nfn'])) $nofilename = intval($_POST['nfn']);


/* the object to search: empty or *ALL* for all objects, the 'object_id' to search one object only */
$objects_select = "";
if (isset($_GET['o'])) { 
	$objects_select = $_GET['o'];	// get the selected object, called with URL like: http://.../ft_flipbook.php?o=myobject_id
}
if ($objects_select == "") {	// get search text field from form
	if (isset($_POST['objects_select'])) { 
		$objects_select = $_POST['objects_select'];
	}
}
/* the text terms to search for */
$ft_searchstr = "";
if (isset($_GET['q'])) { 
	$ft_searchstr = $_GET['q'];	// GET the query string to find, called with URL like: http://.../ft_flipbook.php?q=querytext*
}
if ($ft_searchstr != "") {	// decode it
	$ft_searchstr = urldecode($ft_searchstr);
}
if ($ft_searchstr == "") {	// get search text field from form
	if (isset($_GET['ft_searchstr'])) { 
		$ft_searchstr = $_GET['ft_searchstr'];
	}
	if ($ft_searchstr == "") {	// get search text field from form
		if (isset($_POST['ft_searchstr'])) { 
			$ft_searchstr = $_POST['ft_searchstr'];
		}
	}
}

/* the number of results to return */
$num_results = "";
if (isset($_GET['n'])) {
	$num_results = $_GET['n'];	// get num_results, called with URL like: http://.../ft_flipbook.php?o=myobject_id
}
if ($num_results == "") {	// get from form
	if (isset($_POST['num_results'])) { 
		$num_results = $_POST['num_results'];
	}
}

/* resize search window */
$resize_search_window = "";
if (isset($_GET['r'])) {
	$resize_search_window = $_GET['r'];	// if set to '1' then search window will be resized to standard size
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

/* override document url and paths */
$override_domain = "";				// set to new domain: the 'domain' field from the flipbook_x table will be replaced with this value
$override_root_datapath = "";		// set to new root data path: the 'root_datapath' field from the flipbook_x table will be replaced with this value
$override_path = "";				// set to new path: the 'path' field from the flipbook_x table will be replaced with this value

include "inc/vars.php";				// overwrite vars with external configurable values

include "inc/localized.php";		// include localized strings

if ( ($num_results != "") && ((int)$num_results > 0) ) {
	$max_query_results = $num_results;
}

// detect in which language we should talk to user
$accept_language = $_SERVER['HTTP_ACCEPT_LANGUAGE'];
$browser_preferred_language_code = 0;	// language code (index) default to english

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
$retval = 0;

$result = null;
$con = null;

do {
	// ouptut the html header
	writeResponseHeader();
	//echo($accept_language . "<br>" . $browser_preferred_language . "<br>" . $blc . "<br>");
	// ouptut the search form
	writeSearchForm($ft_searchstr);

	if ($ft_searchstr == "") {
		break;
	}

	$con = mysql_connect($hostname, $username, $password, true);
	if (!$con) {
		$mysqlerrnum = mysql_errno();
		$mysqlerrtext = mysql_error();
		$errtext = "Connection to MySQL failed.";
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
		break;
	}

	// expand search strings with '*' if not already contained
	$myft_searchstr = $ft_searchstr;
	// ----- simply comment this block if no expanded search is wanted
	$quotepos = strpos($myft_searchstr, "\"");
	$pluspos = strpos($myft_searchstr, "+");
	$minuspos = strpos($myft_searchstr, "-");
	$starpos = strpos($myft_searchstr, "*");
	if (!$quotepos && !$pluspos && !$minuspos && !$starpos) {
		$myft_searchstr_arr = mb_split(" ",$ft_searchstr);
		for ($i = 0; $i < count($myft_searchstr_arr); $i++) {
			$myft_searchstr_arr[$i] .= "*";
		}
		$myft_searchstr = ""; //re-concat
		for ($i = 0; $i < count($myft_searchstr_arr); $i++) {
			$myft_searchstr .= " " . $myft_searchstr_arr[$i];
		}
	}
	// ------ end expand search

	/* do not end query string with a semicolon! */
	$query_string = "SELECT MATCH (content) AGAINST (\"" . $myft_searchstr . "\" IN BOOLEAN MODE) AS score, domain, root_datapath, path, name_html, name_xml, article_id, article_page, title, description, issue_date, content, images " .
						" FROM " . $ft_tablename .
						" WHERE (MATCH (content) AGAINST (\"" . $myft_searchstr . "\" IN BOOLEAN MODE))";
						if ( ($objects_select != "*ALL*") && ($objects_select != "") ) $query_string = $query_string . " AND (object_id = \"" . $objects_select . "\")";
					//	$query_string = $query_string . " ORDER BY score DESC";
					//	$query_string = $query_string . " ORDER BY issue_date DESC";
						$query_string = $query_string . " ORDER BY score DESC, issue_date DESC";
						$query_string = $query_string . " LIMIT " . $max_query_results ;
	$result = mysql_query($query_string, $con);
	if (!$result) {
		$mysqlerrnum = mysql_errno();
		$mysqlerrtext = mysql_error();
		$errtext = "Error in query string '" . $query_string . "'";
		break;
	}

	/* return the result */
	$num_rows = mysql_num_rows($result);
	if ($num_rows <= 0) {
		$errtext = $ta[6][$blc] . "'" . $ft_searchstr . "'";
		break;
	}
	$i = 0;
	// ouptut the number of results info
	echo("<div class=\"ft_num_results_info\">". $ta[8][$blc] . $num_rows . "</div>");
	// ouptut the result container header
	writeFtResultHeader();
	// ouptut the result rown
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
		*/
		$row = mysql_fetch_row($result);
		// turn some special chars
		$content = $row[11];
		if ($content != null) {
			$content = ereg_replace("<", "&lt;", $content);
			$content = ereg_replace(">", "&gt;", $content);
			$content = ereg_replace("--\+PDF\+--", "", $content);
			$content = ereg_replace("---PDF---", "", $content);
		}
		else $content = "";
		$imagepath = "";
		$image = "";
		if (($row[12] != null) && ($row[12] != "")) {	// image.jpg,image.eps;image2.jpg,image2.eps;
			$images_arr = mb_split(";", $row[12]);
			if (count($images_arr) > 0) {
				$image_arr = mb_split(",", $images_arr[0]);
				$image = $image_arr[0];
			}
		}
		echo ("<div class=\"ft_item" . is_odd($i) . "\">");
		// create an url to document
		$link = "";
		if ($override_domain != "") $link = $link . $override_domain;	// get domain
		else $link = $link . $row[1];
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
		
		if ($http_host_no_www == true) {	// if the search window was called without 'www.' then we have to remove 'www.' from links
			$re = "www.";
			$link = mb_eregi_replace ( $re, "", $link );
		}

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
		//echo ("<a class=\"ft_link\" target=\"BXSLTflipWin\" onclick=\"return get_page_article('" . $link . "');\" href=\"" . $link . "\">" . $link_title . "</a><br>\n");
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
			echo("<table style=\"border:0;border-collapse:separate;border-spacing:2px;\"><tr><td>");
			echo("<a class=\"ft_link\" title=\"" . $row[10] . "\" onclick=\"return get_page_article('" . $link . "');\" href=\"#\"><img style=\"border:0;" . ($thumbs_height != "" ? ("height:" . $thumbs_height . ";") : "") . "\" src=\"" . $imagepath . "\" alt=\"" . $image . "\"></a>");
			echo("</td><td>");
		}
		echo ("<a class=\"ft_link\" title=\"" . $row[10] . "\" onclick=\"return get_page_article('" . $link . "');\" href=\"#\">" . $link_title . "</a>" . $date_formated . "<br>\n");

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

	// ouptut the result trailer
	writeFtResultTrailer();
} while (false);

if ($DEBUG > 0) {
	echo ("ft_searchstr: \"" . $ft_searchstr . "\"<br>");
	echo ("query_string: \"" . $query_string . "\"<br>");
}

if ($errtext != "") {
	echo ($errtext . "<br>");
}
// ouptut the html trailer
writeResponseTrailer();

/* free the result and close connection */
if ($result) mysql_free_result($result);
if ($con) mysql_close($con);
return;


function writeResponseHeader() {
?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html><head><title>Full-Text Search</title>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<meta http-equiv="Content-Style-Type" content="text/css">
<meta http-equiv="Content-Script-Type" content="text/javascript">
<script language="javascript" type="text/javascript" src="inc/ft_search.js" charset="utf-8"></script>
<link rel="stylesheet" href="inc/styles.css" type="text/css" media="all">
</head><body class="ft_body">
<noscript>
<div style="color:red; font-size:11pt">
To read this site you must enable JavaScript in your browser!<br>
Um diese Seite anzuzeigen m&uuml;ssen Sie JavaScript in Ihrem Browser aktivieren!<br>
Pour visualiser ce site activez JavaScript dans v&ocirc;tre navigateur!
</div>
</noscript>
<?php
	return;
}


function writeSearchForm($ft_searchstr) {
	global $ta, $blc, $mysqlerrnum, $mysqlerrtext, $errtext;
	global $max_query_results;
	$myft_searchstr = mb_eregi_replace ( '\\\\', "", $ft_searchstr );
	echo ("<div class=\"ft_search_container\">");
	echo ("<form id=\"search_form\" name=\"search_form\" method=\"post\" class=\"ft_search_form\" enctype=\"application/x-www-form-urlencoded\" action=\"search.php\">");
	echo ("<table border=\"0\" class=\"ft_search_table\" cellspacing=\"0\" cellpadding=\"0\">");
	echo ("<tr><td rowspan=\"2\"><div class=\"ft_search_logo_td\">" . $ta[9][$blc] . "</div></td>");
	echo ("<td colspan=\"3\" class=\"ft_search_title_td\">" . $ta[0][$blc] . "</td>");
	echo ("<td rowspan=\"2\"><div class=\"ft_search_logo_td\">" . $ta[10][$blc] . "</div></td>");
	echo ("</tr><tr>");
	$retval = writeObjectsSelector();
	if ( ($retval != 0) && ($DEBUG > 0) ) {
		echo ("<td style=\"vertical-align:top;\">Objects Error: " . $retval . "<br>");
		echo ("mysqlerrnum: " . $mysqlerrnum . "<br>");
		echo ("mysqlerrtext: " . $mysqlerrtext . "<br>");
		echo ("errtext: " . $errtext);
		echo ("</td>");
	}
	echo ("<td class=\"ft_searchstr_td\">");
	echo ("<input class=\"ft_searchstr\" id=\"ft_searchstr\" name=\"ft_searchstr\" type=\"text\" title=\"" . $ta[7][$blc] . "\" size=\"42\" maxlength=\"150\" value='" . $myft_searchstr . "'>");
	echo ("<input type=\"image\" id=\"go\" name=\"go\" alt=\"go\" value=\"" . $ta[1][$blc] . "\" title=\"" . $ta[2][$blc] . "\" class=\"ft_go\" src=\"inc/Go.gif\">");
	echo ("<input class=\"ft_num_results\" id=\"num_results\" name=\"num_results\" type=\"text\" title=\"" . $ta[13][$blc] . "\" size=\"3\" maxlength=\"4\" value=\"" . $max_query_results . "\">");
						// the ? ( show ft search help text ) button
	echo ("</td><td class=\"ft_help_td\">");
	echo ("<a href=\"#\" class=\"ft_help\" title=\"" . $ta[5][$blc] . "\" onClick=\"" . $ta[4][$blc] . "\">" . $ta[3][$blc] . "</a>");
	echo ("</td></tr></table></form></div>");
}


function writeObjectsSelector() {
	global $ta, $blc;
	global $dbname, $objects_tablename, $hostname, $username, $password, $mysqlerrnum, $mysqlerrtext, $errtext;
	global $objects_select;
	if ($objects_tablename == "") return(0);
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
		$query_string = "SELECT object_id, title, description " .
							" FROM " . $objects_tablename .
							" WHERE (view_index >= 0)" .
							" ORDER BY view_index ASC";
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
		echo ("<td style=\"vertical-align:top;\">");
		echo ("<select style=\"width:210px;\" title=\"" . $ta[11][$blc] . "\" class=\"ft_objects_select\" id=\"objects_select\"  name=\"objects_select\">");
		if ($num_rows >= 2) {	// show ALL entry only if more than 1 objects
			echo ("<option selected value=\"*ALL*\" title=\"" . $ta[12][$blc] . "\">" . $ta[12][$blc] . "</option>");
		}
		// ouptut the result rown
		while ($i < $num_rows) {
			/*
			$row[0] : object_id
			$row[1] : title
			$row[2] : description
			*/
			$row = mysql_fetch_row($result);
			echo ("<option class=\"objects_option\" value=\"" . $row[0] . "\" title=\"" . $row[2] . "\"" . (($objects_select == $row[0]) ? " selected" : "") . ">" . $row[1] . "</option>");
			$i++;
		}
		echo ("</select></td>");
	} while(false);
	/* free the result and close connection */
	if ($result) mysql_free_result($result);
	if ($con) mysql_close($con);
	return($err);
}


function writeFtResultHeader() {
	echo ("<div class=\"ft_result_container\">");
	return;
}

function writeFtResultTrailer() {
	echo ("</div>");
	return;
}
function writeResponseTrailer() {
	global $ft_searchstr, $resize_search_window;
	if (($ft_searchstr == "") || ($resize_search_window == '1')) {
	echo ("<script type=\"text/javascript\">this.resizeTo(790, 680);document.search_form.ft_searchstr.focus()</script>");
	}
	echo ("</body></html>");
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
		if ($func == 1) $my_s = mb_eregi_replace  ( $re, "<span style=\"background-color:" . $color . "\">\\0</span>", $my_s );
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

