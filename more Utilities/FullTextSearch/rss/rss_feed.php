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

Version: 1.1
Version date: 20120920

Multibyte support must be enabled in PHP!
In the php.ini file, the line

extension=php_mbstring.dll

must be uncommented

HOW TO CALL:
------------
rss_feed.ph?o=object_ID
		request an rss feed of the latest available issue of a certain publication
		returns 
		an rss feed XML file containing the article of the latest available issue

rss_feed.ph?o=object_ID&sd=20120412
		request an rss feed of issue of the publication dated April 12, 2012
		returns 
		an rss feed XML file containing the article of the latest available issue

===================================
*/
//error_reporting (E_ALL);

//ob_start();
//header('Content-Type: application/rss-xml; charset=UTF-8');	// does not work for Chrome, iPad, IE
header('Content-Type: text/xml; charset=UTF-8');	// does not work for Chrome


/* the object to search: empty or *ALL* for all objects, the 'object_id' to search one object only */
$objects_select = "";
if (isset($_GET['o'])) { 
	$objects_select = $_GET['o'];	// get the selected object, called with URL like: http://.../latest.php?o=myobject_id
}
if ($objects_select == "") {	// get object from form
	if (isset($_POST['objects_select'])) { 
		$objects_select = $_POST['objects_select'];
	}
}
// evtl. get an issue with a certain date
$issue_start_date = "";
$issue_end_date = "";
if (isset($_GET['sd'])) { 
	$issue_start_date = $_GET['sd'];	// get the start date, called with URL like: http://.../latest.php?sd=YYYYMMDD
}
if ($issue_start_date == "") {	// get search start date field from form
	if (isset($_POST['ft_searchstartdate_field'])) { 
		$issue_start_date = $_POST['ft_searchstartdate_field'];
	}
}
$orig_issue_start_date = $issue_start_date;
if ($issue_start_date != "") $issue_start_date = str_pad ( $issue_start_date, 8, "0", STR_PAD_RIGHT);

if ($orig_issue_start_date != "") $issue_end_date = str_pad ( $orig_issue_start_date, 8, "9", STR_PAD_RIGHT);

$min_content_length = 70;		// minimum number of characters an article must have to be shown
if (isset($_GET['mcl'])) { 
	$min_content_length = intval($_GET['mcl']);
}
$max_title_length = 70;		// number of characters to show as title
if (isset($_GET['tl'])) { 
	$max_title_length = intval($_GET['tl']);
}
$max_description_length = 250;
if (isset($_GET['cl'])) { 
	$max_description_length = intval($_GET['cl']);
}

$original_article_type = 1;	// 0 = point feed item link to main HTML epaper file
							// 1 = extract this article only as HTNML using the tool 'extractArticleHTML.php'
if (isset($_GET['t'])) { 
	$original_article_type = intval($_GET['t']);
}

$numissuesavailable = 0;	// how many issues available with a given date '$orig_issue_start_date'

// we have to check if this search window was called with 'www.'
// if NOT, then we have to remove 'www.' from any links
$http_host = $_SERVER['HTTP_HOST'];
$http_host_no_www = true;
$http_host_www_pos = strpos($http_host, "www.");
if ($http_host_www_pos === false) $http_host_no_www = true;
elseif ($http_host_www_pos >= 0) $http_host_no_www = false;

$hostpath = dirname(curPageURL()) . "/";

/* main vars - overwritten by vars.php */
$username = "";
$password = "";
$hostname = "";
$dbname = '';
$ft_tablename = '';
$objects_tablename = '';
$browser_preferred_language = '';	// set to language code like 'en' 'fr' 'de' ... or leave empty to detect browser 's preferred language

/* override document url and paths */
$override_domain = "";				// set to new domain: the 'domain' field from the flipbook_x table will be replaced with this value
$override_root_datapath = "";		// set to new root data path: the 'root_datapath' field from the flipbook_x table will be replaced with this value
$override_path = "";				// set to new path: the 'path' field from the flipbook_x table will be replaced with this value

include "inc/vars.php";				// overwrite vars with external configurable values

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
//echo($accept_language . "<br>" . $browser_preferred_language . "<br>" . $blc . "<br>");

// some more work variables
$errtext = "";
$mysqlerrnum = 0;
$mysqlerrtext = "";
$retval = 0;

$the_query_string = "";
$result = null;


$channel_title = "";
$channel_link = "";
$channel_description = "";
$channel_language = "";
$channel_pubDate = gmdate("Y-m-d\TH:i:s\Z");
$channel_copyright = "";
$channel_ttl = "60";			// time to live before refreshing in minutes
$timezone = "Europe/London";
date_default_timezone_set($timezone);
// output the feed XML
echo("<?xml version=\"1.0\" ?>\r\n");
echo("<rss version=\"2.0\">\r\n");
	echo("<channel>\r\n");
		get_issue(true);	// get issue information from objects table and latest issue date
		echo("<title>" . $channel_title . "</title>\r\n");
		echo("<link>" . $channel_link . "</link>\r\n");
		echo("<description>" . $channel_description . "</description>\r\n");
		echo("<language>" . $channel_language . "</language>\r\n");
		echo("<pubDate>" . $channel_pubDate . "</pubDate>\r\n");
		echo("<copyright>" . $channel_copyright . "</copyright>\r\n");
		echo("<ttl>" . $channel_ttl . "</ttl>\r\n");
		get_issue(false);

	echo("</channel>\r\n");
echo("</rss>\r\n");


return;

/******* END main ******/
function url_save( $str ) {
	$newstr = ereg_replace(" ", "%20", $str);
	$newstr = ereg_replace("\?", "%3F", $newstr);
	$newstr = ereg_replace("\&", "%26", $newstr);
	return ($newstr);
}





function get_issue($getChannelInfo) {
	global $mysqlerrnum, $mysqlerrtext, $errtext;
	global $hostname, $username, $password, $dbname, $ft_tablename, $objects_tablename;
	global $override_domain, $override_root_datapath, $override_path;
	global $numissuesavailable;
	global $objects_select, $issue_start_date, $issue_end_date, $the_query_string;
	global $channel_title, $channel_link, $channel_description, $channel_language, $channel_pubDate, $channel_copyright, $channel_ttl;
	global $timezone, $result, $min_content_length, $max_title_length, $max_description_length;
	global $hostpath, $original_article_type;
	
	$link = "";
	$date = "";
	do {
		$con = mysql_connect($hostname, $username, $password, true);
		if (!$con) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Connection to MSQL failed.";
			break;
		}
		$retval = mysql_set_charset ( "utf8", $con );
		$db_selected = mysql_select_db($dbname, $con);
		if (!$db_selected) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Database '" . $dbname . "' could not be opened.";
			break;
		}

		if ($getChannelInfo) {	// get publication info from objects table
			$query_string = "SELECT object_id, company, title, description, link, language, copyright, ttl, timezone" .
								" FROM " . $objects_tablename ;
								if ($objects_select != "") {
									$query_string = $query_string . " WHERE (object_id = \"" . $objects_select . "\")";
								}
								else {
									$query_string = $query_string . " WHERE (view_index > 0)";
								}
								$query_string = $query_string . " ORDER BY view_index ASC LIMIT 1" ;
			$result = mysql_query($query_string, $con);
			//echo($query_string);
			if (!$result) {
				$mysqlerrnum = mysql_errno();
				$mysqlerrtext = mysql_error();
				$errtext = "Error in query string '" . $query_string . "'";
				break;
			}
			$num_rows = mysql_num_rows($result);
			if ($num_rows > 0) {
				$row = mysql_fetch_row($result);
				/*
				$row[0] : object_id
				$row[1] : company
				$row[2] : title
				$row[3] : description
				$row[4] : link
				$row[5] : language
				$row[6] : copyright
				$row[7] : language
				*/
				$channel_title = $row[2];
				$channel_link = $row[4];
				$channel_description = $row[3];
				$channel_language = $row[5];
				$channel_copyright = $row[6];
				$channel_ttl = $row[7];
				$timezone = $row[8];
				if ($timezone != "") date_default_timezone_set($timezone);
			}
			if ($result) { mysql_free_result($result); $result = null; }
		}

		// first get the latest issue date
		$query_string = "SELECT domain, root_datapath, path, name_html, issue_date" .
							" FROM " . $ft_tablename ;
							if ( ( ($objects_select != "*ALL*") && ($objects_select != "") ) || ($issue_start_date != "")) {
								$query_string = $query_string . " WHERE (";
								if ( ($objects_select != "*ALL*") && ($objects_select != "") ) $query_string = $query_string . " (object_id = \"" . $objects_select . "\")";
								if ($issue_start_date != "") {
									$pos = strpos($query_string, "object_id");
									if ($pos === false) ;
									else $query_string = $query_string . " AND";
									$query_string = $query_string . " (CONVERT(issue_date,BINARY) >= CONVERT(\"" . $issue_start_date . "\",BINARY)) AND (CONVERT(issue_date,BINARY) <= CONVERT(\"" . $issue_end_date . "\",BINARY))";
								}
								$query_string = $query_string . " )";
							}
							$query_string = $query_string . " ORDER BY issue_date DESC LIMIT 1" ;
		$result = mysql_query($query_string, $con);
		if (!$result) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Error in query string '" . $query_string . "'";
			break;
		}
		$num_rows = mysql_num_rows($result);
		$row = mysql_fetch_row($result);
		$issue_date = $row[4];	// like 20120519
		$stamp = substr($issue_date,6,2) . "-" . substr($issue_date,4,2) . "-" . substr($issue_date,0,4) . " 00:00:00";	// dd mm yyyy 00:00:00
		/* return the channel date */
		$channel_pubDate = gmdate('D, d M Y H:i:s \G\M\T', strtotime($stamp) );	// $stamp;
		if ($result) { mysql_free_result($result); $result = null; }
		if ($getChannelInfo) return;
		
		/* get the articles */
		$query_string = "SELECT issue_date, domain, root_datapath, path, name_html, name_xml, article_id, article_page, content " .
						" FROM " . $ft_tablename .
						" WHERE (issue_date = \"" . $issue_date . "\")" .
								" AND (CHAR_LENGTH(content) >= " . $min_content_length . ")" .
								" AND ((prev_id IS NULL) OR (prev_id = ''))";	// get strting articles only
						if ( ($objects_select != "*ALL*") && ($objects_select != "") ) $query_string = $query_string . " AND (object_id = \"" . $objects_select . "\")";
						if ( $search_start_date != "" ) $query_string = $query_string . " AND (CONVERT(issue_date,BINARY) >= CONVERT(\"" . $search_start_date . "\",BINARY))";
						if ( $search_end_date != "" ) $query_string = $query_string . " AND (CONVERT(issue_date,BINARY) <= CONVERT(\"" . $search_end_date . "\",BINARY))";
						$query_string = $query_string . " ORDER BY CONVERT(article_id,UNSIGNED) ASC";
		$the_query_string = $query_string;
		
		$result = mysql_query($query_string, $con);
		if (!$result) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Error in query string '" . $query_string . "'";
			break;
		}

		$num_rows = mysql_num_rows($result);
		/*
		echo("objects_select: " . $objects_select . "<br>\n");
		echo("issue_date: " . $issue_date . "<br>\n");
		echo("issue_start_date: " . $issue_start_date . "<br>\n");
		echo("issue_end_date: " . $issue_end_date . "<br>\n");
		echo("num_rows: " . $num_rows . "<br>\n");
		echo($the_query_string . "<br>\n");
		*/
		$numissuesavailable = $num_rows;
		if ($num_rows <= 0) {
			break;
		}
		$i = 0;
		/* build the result */
		while ($i < $num_rows) {
			/*
			$row[0] : issue_date
			$row[1] : domain
			$row[2] : root_datapath
			$row[3] : path
			$row[4] : name_html
			$row[5] : name_xml
			$row[6] : article_id
			$row[7] : article_page
			$row[8] : content
			*/
			$row = mysql_fetch_row($result);

			$content = $row[8];
			$content = ereg_replace("<", "&lt;", $content);
			$content = ereg_replace(">", "&gt;", $content);
			$content = ereg_replace("--\+PDF\+--", "", $content);
			$content = ereg_replace("---PDF---", "", $content);

			$date = $row[0];
			// create an url to document
			$link = "";
			if ($override_domain != "") $link = $link . $override_domain;	// get domain
			else $link = $link . $row[1];
			// check if domain contains protocol like http://
			$pos = mb_strpos($link, "://");
			if ($pos === false) $link = "http://" . $link;
			if (endsWith($link, "/") == false) $link = $link . "/";	// check ending with slash
	
			if ($override_root_datapath != "") $link = $link . $override_root_datapath;	// add root_datapath
			else $link = $link . $row[2];
			if (endsWith($link, "/") == false) $link = $link . "/";	// 
	
			if ($override_path != "") $link = $link . $override_path;	// add path
			else $link = $link . $row[3];
			if (endsWith($link, "/") == false) $link = $link . "/";	// 
	
			$link = $link . $row[4];					// add html name
			$link = url_save($link);					// this is the direct link to theepaper HTML file
			$link_epaper = $link;
				$link_epaper = $link_epaper . "?p=" . $row[7];			// add page_sequence query
				$link_epaper = $link_epaper . "&amp;a=" . $row[6];		// add article_id query
			if ($original_article_type == 0) {
				$link = $link . "?p=" . $row[7];			// add page_sequence query
				$link = $link . "&amp;a=" . $row[6];		// add article_id query
			}
			else {	// use the extract tool 'extractArticleHTML.php'
				$link = $hostpath."extractArticleHTML.php?d=". $link;
				$link = $link . "&amp;p=" . $row[7];		// add page_sequence query
				$link = $link . "&amp;a=" . $row[6];		// add article_id query
			}

			$title = "";
			$newlinepos = mb_strpos($content, "\r");
			if ($newlinepos >= 3) {
				$title = mb_substr($content, 0, $newlinepos);		// get first line as title
				$title = shortenText($title, 0, $max_title_length);		// shorten title to max chars
				$content = mb_substr($content, $newlinepos+1);		// get following lines as content
			}
			else {
				$title = shortenText($content, 0, $max_title_length);		// get content part as title
			}
			$content = shortenText($content, 0, $max_description_length);		// shorten

			// output XML for this article
			echo("<item>\r\n");
			echo("<title>" . $title . "</title>\r\n");
			echo("<link>" . $link . "</link>\r\n");
			echo("<description>" . $content . "</description>\r\n");
			echo("<guid>" . $link_epaper . "</guid>\r\n");
			echo("</item>\r\n");
			$i++;
		}
	} while (false);


	/* free the result and close connection */
	if ($result) mysql_free_result($result);
	if ($con) mysql_close($con);
	return;
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


function is_odd( $int ) {
  return( $int & 1 );
}


function startsWith( $str, $sub ) {
   return ( substr( $str, 0, strlen( $sub ) ) === $sub );
}
function endsWith( $str, $sub ) {
   return ( substr( $str, strlen( $str ) - strlen( $sub ) ) === $sub );
}



function detectLanguage($accept_lg) {
	$lang = substr($accept_lg,0,2);
	return $lang;
}

function detectLanguageCode($lang) {
	if (($lang == null) || ($lang == "") || ($lang == "en") ) return(0);
	switch ($lang) {
		case "de": case "gs": return(1);	// german, schxyzerduetsch
		case "fr": return(2);	// french
		case "da": return(3);	// danish
		default: return(0);		// english
	}
	return (0);
}

function curPageURL() {
	$pageURL = 'http';
	if (isset($_SERVER["HTTPS"])) {
		if ($_SERVER["HTTPS"] == "on") {$pageURL .= "s";}
	}
	$pageURL .= "://";
	if ($_SERVER["SERVER_PORT"] != "80") {
	$pageURL .= $_SERVER["SERVER_NAME"].":".$_SERVER["SERVER_PORT"].$_SERVER["REQUEST_URI"];
	} else {
	$pageURL .= $_SERVER["SERVER_NAME"].$_SERVER["REQUEST_URI"];
	}
	return $pageURL;
}


?>

