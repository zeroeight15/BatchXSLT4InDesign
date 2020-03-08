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

Version: 2.22
Version date: 20160822

Multibyte support must be enabled in PHP!
In the php.ini file, the line

extension=php_mbstring.dll

must be uncommented

HOW TO CALL:
------------
latest.php
latest.php?o=object_ID
		request a link to the latest available issue of a certain publication or any publication
		returns 
		a permanent redirect to the latest HTML book file with object_id spezified

latest.php?o=ZF&action=issueavail
		request number of available issues within a given time range or no time limit
		returns
		a selection of key:value pairs encapsulated in '*'
		*numissuesavailable:18
latest.php?o=ZF&action=issueavail&sd=201204
		request number of issues available in April 2012
		returns
		*numissuesavailable:4
latest.php?action=issueavail&o=*ALL*&sd=201205
		request the number of available issues of any objects for April 2012
		returns
		*numissuesavailable:8
		where # is the total of available issues

latest.php?o=ZF&action=issuedate
		request the latest available issue date
		returns
		*numissuesavailable:1**issuedate:20120507**linkurl:http://mydomain.com/epaper/DATA/XYZ/mypublication/obj/2012/20120507/obj_20120507_20.htm*
latest.php?o=ZF&action=issuedate&sd=201204
		request the latest available issue date for April 2012
		returns
		*numissuesavailable:1**issuedate:20120430**linkurl:http://mydomain.com/epaper/DATA/XYZ/mypublication/obj/2012/20120507/obj_20120430_20.htm*
latest.php?o=ZF&action=issueobjectid
		request the latest available issue's object ID
		returns
		XX
latest.php?o=ZF&action=issuepath
		request the latest available issue's URL PATh (without object HTML file)
		returns
		http://mydomain.com/epaper/DATA/XYZ/mypublication/obj/2012/20120507/

------------
get issue dates and their links from an optional start date to an optional end date
latest.php?o=DAKE&action=issuedates&sd=201204&ed=201205
		returns:
		*requestAction:issuedates**numissuesavailable:2**object_id:DAKE,DAKE*
		*issuedates:20120424,20120414*
		*linkurl:http://mydomain.com/.../epaper/DATA/.../2012/20120424/DAKE_20120424.indb.htm,http://mydomain.com/.../epaper/DATA/.../DAKE/2012/20120414/DAKE_20120414.indb.htm*
	to handle as arrays:
	 *issuedates: 2 issue dates (comma separated)
	and
	 *linkurl: 2 urls (comma separated)
	
	if no start date is given, all availabe issues starting from the oldest are returned up to the end date (if given, otherwise all)
	a start date MUST be given! If not known, enter sd=0

latest.php?ol=DF__&action=issuedates
	*requestAction:issuedates**numissuesavailable:1**object_id:ABCD**issuedates:20130101**linkurl:http://mydomain.com/.../epaper/DATA/Cats/ABCD/2013/20130101/ABCD_20130101_dogs.indb.htm*
	ol = object_id LIKE 
	use MySQL Like syntax for wildcards: % = any number of characters, _ = 1 character

------------
latest.php?o=XY&latestzip
		returns:
		URL to the latest available issue [with the given object ID] as zip archive

------------
get a thumb of a page image
<img src="http://mydomain.com/.../epaper/search/latest.php?o=xyz&action=imagethumb&pageseq=1&sd=20120608&width=220&imagetype=jpg"><br>
<img src="http://mydomain.com/.../epaper/search/latest.php?o=ABC&action=imagethumb&pageseq=1&sd=20120608&height=220&imagetype=png"><br>
<img src="http://mydomain.com/.../epaper/search/latest.php?o=ABC&action=imagethumb&pageseq=1&sd=20120608&width=320&height=220&imagetype=gif"><br>

===================================
*/

ob_start();

/* special action to perform: 'issueavail' for just testing if a certain issue is available*/
$requestAction = "";
if (isset($_GET['action'])) { 
	$requestAction = $_GET['action'];	// what to do:
										// empty or not present : get the latest issue available
										// 'issueavail' : get number of issues available or get a certain issue
										// 'issuedate' : get info of latest issue
										// 'issuedates' : get all issue dates available starting from a certain issue date
										// 'latestzip','latestzip_phone','latestzip_tablet' : get link to latest issue as zip archive
										// 'imagethumb' : get a page thumb
}

/* special sorting. default is ORDER BY issue_date DESC */
$sortOrder = "";
if (isset($_GET['sort'])) {
	$sortOrder = $_GET['sort'];	// 'vidx' = sort by view_index of table $objects_tablename
}

$vidx_min = -1;
$vidx_max = -1;
if (isset($_GET['vidxmin'])) {
	$vidx_min = intval($_GET['vidxmin']);	// minimum view_index of table $objects_tablename
}
if (isset($_GET['vidxmax'])) {
	$vidx_max = intval($_GET['vidxmax']);	// maximum view_index of table $objects_tablename
	if (($vidx_max > -1) && ($vidx_min < 0)) $vidx_min = 0;
}
$get_objectdescriptions = false;
if (isset($_GET['objdescr'])) {
	$get_objectdescriptions = true;	// get descriptions from $objects_tablename
}

$flipbook_params = "";
if (isset($_GET['flipbook_params'])) {
	$flipbook_params = $_GET['flipbook_params'];	// '?flipbook_params=NAVISION' means add parameter to flipbook link
}

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

if ($requestAction != "imagethumb") {	// allow start and end date
	if (isset($_GET['ed'])) { 
		$issue_end_date = $_GET['ed'];	// get the end date, called with URL like: http://.../latest.php?sd=YYYYMMDD&ed=YYYY
	}
	if ($issue_end_date == "") {	// get search end date field from form
		if (isset($_POST['ft_searchenddate_field'])) { 
			$issue_end_date = $_POST['ft_searchenddate_field'];
		}
	}
	$orig_issue_end_date = $issue_end_date;
	if ($issue_end_date != "") $issue_end_date = str_pad ( $issue_end_date, 8, "0", STR_PAD_RIGHT);
}
else {	// for thumbs, we allow start date only
	if ($orig_issue_start_date != "") $issue_end_date = str_pad ( $orig_issue_start_date, 8, "9", STR_PAD_RIGHT);
}


if (($requestAction == 'issuedate') && ($issue_end_date == "")) {	// we want a single issue date only
	if ($orig_issue_start_date != "") $issue_end_date = str_pad ( $orig_issue_start_date, 8, "9", STR_PAD_RIGHT);
}




$target_callbackID = "";
if (isset($_GET['tcb'])) { 
	$target_callbackID = $_GET['tcb'];	// get the target ID to call in a callback script 'get_query_result'
}



$numissuesavailable = 0;	// how many issues available with a given date '$orig_issue_start_date'

// we have to check if this search window was called with 'www.'
// if NOT, then we have to remove 'www.' from any links
$http_host = $_SERVER['HTTP_HOST'];
$http_host_remove_www = false;
$http_host_www_pos = strpos($http_host, "www.");
if ($http_host_www_pos === false) $http_host_remove_www = true;
elseif ($http_host_www_pos >= 0) $http_host_remove_www = false;

/* main vars - overwritten by vars.php */
$DEBUG = 0;
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

$AUTH_USER = "";					// user name to load flipbook file from the filesystem via http: (used by latest.php)
$AUTH_PWD = "";						// password to load flipbook file from the filesystem via http: (used by latest.php)

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

$issuedate = "";
$object_id = "";
$linkurl = "";	// the full path/name
$linkpath = "";	// the path only
$nameHTML = "";	// HTML name only
$nameXML = "";	// the XML name only

$ot = new stdClass();
$ot -> valid = false;
$ot -> view_index = array();
$ot -> object_id = array();
$ot -> company = array();
$ot -> title = array();
$ot -> description = array();
$ot -> long_description = array();
$ot -> language = array();

$retrievedImageMessage = "";
$retrievedImagePath = "";
$retrievedImage = null;



if ($DEBUG != 0) {
	header('Content-Type: text/html; charset=UTF-8'); 
	echo ("username: \"" . $username . "\"<br>");
	echo ("password: \"" . $password . "\"<br>");
	echo ("hostname: \"" . $hostname . "\"<br>");
	echo ("dbname: \"" . $dbname . "\"<br>");
	echo ("ft_tablename: \"" . $ft_tablename . "\"<br>");
}

get_latest_issue();
$linkurl = url_save($linkurl);


if ($DEBUG != 0) {
	echo ("mysqlerrnum: " . $mysqlerrnum . "<br>");
	echo ("mysqlerrtext: \"" . $mysqlerrtext . "\"<br>");
	echo ("linkurl: \"" . $linkurl . "\"<br>");
	echo ("issuedate: \"" . $issuedate . "\"<br>");
	if ($errtext != "") {
		echo ("errtext: " . $errtext . "<br>");
	}
	if ($the_query_string != "") {
		echo ("the_query_string: " . $the_query_string . "<br>");
	}

}
else {
	switch ($requestAction) {
		case 'issueobjectid':
			header('Content-Type: text/html; charset=UTF-8'); 
			echo ($object_id);
			break;
		case 'issuepath':
			header('Content-Type: text/html; charset=UTF-8'); 
			echo (substr($linkurl,0,mb_strrpos($linkurl,"/")+1));
			break;
		case 'issuelink':
			header('Content-Type: text/html; charset=UTF-8'); 
			echo ($linkurl);
			break;
		case 'issuedate':
			header('Content-Type: text/html; charset=UTF-8'); 
			echo ("*requestAction:" . $requestAction . "*");	// what was  requested
			echo ("*numissuesavailable:" . $numissuesavailable . "*");	// number of issues available with a certain date
			echo ("*object_id:". $object_id . "*");
			echo ("*issuedate:". $issuedate . "*");
			echo ("*linkurl:". $linkurl . "*");
			if ($target_callbackID != "") {
				echo ("<script type=\"text/javascript\">parent.get_query_result(\"" . $target_callbackID . "\");</script>");
			}
			break;
		case 'issuedates':
			header('Content-Type: text/html; charset=UTF-8'); 
			echo ("*requestAction:" . $requestAction . "*");	// what was  requested
			echo ("*numissuesavailable:" . $numissuesavailable . "*");	// number of issues available with a certain date
			echo ("*object_id:". $object_id . "*");
			echo ("*issuedates:". $issuedate . "*");
			echo ("*linkurl:". $linkurl . "*");
			if ($ot -> valid == true) echo ("*ot:". json_encode($ot) . "*");
	
			if ($target_callbackID != "") {
				echo ("<script type=\"text/javascript\">parent.get_query_result(\"" . $target_callbackID . "\");</script>");
			}
			break;
		case 'issueavail':
			header('Content-Type: text/html; charset=UTF-8'); 
			echo ("*numissuesavailable:" . $numissuesavailable);	// number of issues available with a certain date
			break;
		case 'latestzip':
		case 'latestzip_phone':
		case 'latestzip_tablet':
			$options = "";
			switch ($requestAction) {
				case 'latestzip_phone':
					$options = "nopdf,noxml,nohtm,phone";
					break;
				case 'latestzip_tablet':
					$options = "nopdf,noxml,nohtm,tablet";
					break;
			}
			$pathurl = substr($linkurl,0,mb_strrpos($linkurl,"/")+1);
			$zipurl = zip_archive($linkurl, $options);

			header('Content-Type: text/html; charset=UTF-8'); 
			echo ("*requestAction:" . $requestAction . "*");	// what was  requested
			echo ("*numissuesavailable:" . $numissuesavailable . "*");	// number of issues available with a certain date
			echo ("*object_id:". $object_id . "*");
			echo ("*issuedate:". $issuedate . "*");
			echo ("*linkurl:". $linkurl . "*");
			echo ("*pathurl:". $pathurl . "*");
			echo ("*zipurl:". $zipurl . "*");
			if ($target_callbackID != "") {
				echo ("<script type=\"text/javascript\">parent.get_query_result(\"" . $target_callbackID . "\");</script>");
			}
			break;
		case 'imagethumb':
			$retval = imagethumb();
			if ($retval == 0) {	// image is ready to be returned
				$imagetype = "jpg";
				$imagequality = 75;
				if (isset($_GET['imagetype'])) { 
					$imagetype = $_GET['imagetype'];	// get the page name, called with URL like: http://.../latest.php&action=imagethumb&pagename=1&width=220&imagetype=jpg
				}
				if (isset($_GET['imagequality'])) { 
					$imagequality = intval($_GET['imagequality']);	// jpg quality 1..100
																	// png quality 0..9
				}
				switch($imagetype) {
					case 'jpg':
						header('Content-Type: image/jpeg');
						imagejpeg($retrievedImage,null,$imagequality);	// return the image
						break;
					case 'gif':
						header('Content-Type: image/gif');
						imagegif($retrievedImage);	// return the image
						break;
					case 'png':
						header('Content-Type: image/png');
						if ($imagequality > 9) $imagequality = intval($imagequality/100);
						imagepng($retrievedImage,null,$imagequality);	// return the image
						break;
				}
				try { imagedestroy($retrievedImage); } catch (Exception $e) {}
				break;
			}
			else {
				header('Content-Type: text/html; charset=UTF-8'); 
				echo("## ERROR ".$retval." while handling an imaging request.<br>");
				echo("## ERROR text: ".$retrievedImageMessage."<br>");
			}
			break;
		default:
			if ($linkurl != "") {
				// add reaquest URL parameters
				$parms = "";
				foreach($_GET as $key=>$value) {
					if ($key == 'o') continue;
					if ($parms == '') $parms .= '?';
					else $parms .= '&';
					$parms .= $key;
					if ($value != '') {
						$parms .= '='.$value;
					}
					//echo "$key=$value<br>";
				}
				$linkurl .= $parms;
				//echo $linkurl;
				header("Location: {$linkurl}");	// redirect to this link
			}
			break;
	}
}

ob_flush();

return;

/**
 * functions
 */
function url_save( $str ) {
	$newstr = mb_eregi_replace(" ", "%20", $str);
//	$newstr = mb_eregi_replace("\?", "%3F", $newstr);
//	$newstr = mb_eregi_replace("\&", "%26", $newstr);
	return ($newstr);
}





function get_latest_issue() {
	global $http_host_remove_www, $mysqlerrnum, $mysqlerrtext, $errtext;
	global $hostname, $username, $password, $dbname, $ft_tablename, $objects_tablename;
	global $override_domain, $override_root_datapath, $override_path;
	global $requestAction, $sortOrder, $flipbook_params, $numissuesavailable;
	global $objects_select, $objects_select_like, $issue_start_date, $issue_end_date, $the_query_string;
	global $object_id, $linkurl, $issuedate;
	global $linkpath, $nameHTML, $nameXML;
	global $ot, $vidx_min, $vidx_max, $get_objectdescriptions;
	
	$link = ""; $alink = "";
	$date = $object_id = "";
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
		
		/* do not end query string with a semicolon! */
		$query_string = "SELECT DISTINCT ft.issue_date, ft.domain, ft.root_datapath, ft.path, ft.name_html, ft.name_xml, ft.object_id";
							if ($sortOrder == 'vidx') $query_string = $query_string . ", ot.view_index";
							if ($get_objectdescriptions == true) $query_string = $query_string . ", ot.object_id, ot.company, ot.title, ot.description, ot.long_description, ot.language";
							$query_string = $query_string . " FROM " . $ft_tablename . " ft";
							if (($get_objectdescriptions == true) || ($sortOrder == 'vidx')) $query_string = $query_string . ", "  . $objects_tablename . " ot";
							if ( ( ($objects_select != "*ALL*") && ($objects_select != "") ) || ($objects_select_like != "") || ($issue_start_date != "") || ($sortOrder != "")) {
								$query_string = $query_string . " WHERE (";

								if ($sortOrder == 'vidx') {
									$query_string = $query_string . " (ft.object_id = ot.object_id)";
									$set_and = true;
								}
								else $set_and = false;

								if ($vidx_min > -1) {
									if ($set_and === true) $query_string = $query_string . " AND";
									$query_string = $query_string . " (ot.view_index >= " . $vidx_min . ")";
									$set_and = true;
								}
								if ($vidx_max > -1) {
									if ($set_and === true) $query_string = $query_string . " AND";
									$query_string = $query_string . " (ot.view_index <= " . $vidx_max . ")";
									$set_and = true;
								}

								if ($objects_select_like != "") {
									if ($set_and === true) $query_string = $query_string . " AND";
									$query_string = $query_string . " (ft.object_id LIKE \"" . $objects_select_like . "\")";
									$set_and = true;
								}
								else {
									if ( ($objects_select != "*ALL*") && ($objects_select != "") ) {
										$query_string = $query_string . " (ft.object_id = \"" . $objects_select . "\")";
										$set_and = true;
									}
								}

								if ($issue_start_date != "") {
									if ($set_and === true) $query_string = $query_string . " AND";
									if ($issue_end_date != "") $query_string = $query_string . " (CONVERT(ft.issue_date,BINARY) >= CONVERT(\"" . $issue_start_date . "\",BINARY)) AND (CONVERT(ft.issue_date,BINARY) <= CONVERT(\"" . $issue_end_date . "\",BINARY))";
									else $query_string = $query_string . " (CONVERT(ft.issue_date,BINARY) >= CONVERT(\"" . $issue_start_date . "\",BINARY))";
								}

								$query_string = $query_string . " )";
							}
							if ($sortOrder == 'vidx') $query_string = $query_string . " ORDER BY ot.view_index ASC" ;
							else $query_string = $query_string . " ORDER BY ft.issue_date DESC" ;
							if (($requestAction == 'issueavail') || ($requestAction == 'issuedates')) {	// counting number of available issues or want all issue dates?
							}
							else {
								$query_string = $query_string . " LIMIT 1" ;	// normal request for latest issue link
							}
		$the_query_string = $query_string;
		/*
		echo("requestAction: " . $requestAction . "<br>");
		echo("sortOrder: " . $sortOrder . "<br>");
		echo("objects_select: " . $objects_select . "<br>");
		echo("objects_select_like: " . $objects_select_like . "<br>");
		echo("issue_start_date: " . $issue_start_date . "<br>");
		echo("issue_end_date: " . $issue_end_date . "<br>");
		echo($the_query_string . "<br>");
		echo("------------------------<br>");
		*/
		$result = mysql_query($query_string, $con);
		if (!$result) {
			$mysqlerrnum = mysql_errno();
			$mysqlerrtext = mysql_error();
			$errtext = "Error in query string '" . $query_string . "'";
			break;
		}

		$num_rows = mysql_num_rows($result);
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
			$row[6] : object_id
			if $sortOrder  == 'vidx' - more fields
			$row[7] : ot.view_index
			$row[8] : ot.object_id
			$row[9] : ot.company
			$row[10] : ot.title
			$row[11] : ot.description
			$row[12] : ot.long_description
			$row[13] : ot.language
			*/
			$row = mysql_fetch_row($result);

			$object_id .= $row[6] . ($i < ($num_rows-1) ? "," : "");
			
			if ($requestAction == 'issuedates') $date .= $row[0] . ($i < ($num_rows-1) ? "," : "");
			else $date .= $row[0];

			// create an url to document
			$alink = "";
			$thedomain = "";
			if ($override_domain != "") $thedomain = $override_domain;	// get domain
			else $thedomain = $row[1];
			if ($http_host_remove_www == true) {	// if we were called without 'www.' then we have to remove 'www.' from domain
				$re = "//www.";
				$thedomain = mb_eregi_replace ( $re, "//", $thedomain );
			}
			$alink .= $thedomain;
			// check if domain contains protocol like http://
			$pos = strpos($alink, "://");
			if ($pos === false) $alink = "http://" . $alink;
			if (endsWith($alink, "/") == false) $alink = $alink . "/";	// check ending with slash
	
			if ($override_root_datapath != "") $alink = $alink . $override_root_datapath;	// add root_datapath
			else $alink = $alink . $row[2];
			if (endsWith($alink, "/") == false) $alink = $alink . "/";	// 
	
			if ($override_path != "") $alink = $alink . $override_path;	// add path
			else $alink = $alink . $row[3];
			if (endsWith($alink, "/") == false) $alink = $alink . "/";	// 

			$linkpath = $alink;		// save path
			$nameHTML = $row[4];	// save HTML name
			$nameXML = $row[5];		// save XML name
			if ($requestAction == 'issuedates') {
				$link .= $alink . $nameHTML;					// add html name
				$link .= ($flipbook_params != "" ? ("?".$flipbook_params) : "");	// add parameters
				$link .= ($i < ($num_rows-1) ? "," : "");		// add link separator
			}
			else {
				$link = $alink . $nameHTML;
				$link .= ($flipbook_params != "" ? ("?".$flipbook_params) : "");	// add parameters
			}

			// we have additional fields?
			if ($get_objectdescriptions == true) {
				$ot -> valid = true;
				array_push($ot -> view_index, $row[7]);
				array_push($ot -> object_id, $row[8]);
				array_push($ot -> company, $row[9]);
				array_push($ot -> title, $row[10]);
				array_push($ot -> description, $row[11]);
				array_push($ot -> long_description, $row[12]);
				array_push($ot -> language, $row[13]);
			}

			if ($requestAction != 'issuedates') break;
			$i++;
		}
	} while (false);

	/* return the result */
	if ($mysqlerrnum == 0) {
		$linkurl = $link;
		$issuedate = $date;
	}

	/* free the result and close connection */
	if ($result) mysql_free_result($result);
	if ($con) mysql_close($con);
	return;
}

function fget_curl($url) {
	$ch = curl_init();
	curl_setopt($ch, CURLOPT_URL, $url);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, TRUE);
	curl_setopt($ch, CURLOPT_BINARYTRANSFER, TRUE);
	curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);	// blindly trust an SSL certificate
	$f = curl_exec($ch);
	curl_close( $ch );
	return($f);
}

function imagethumb() {
	global $linkpath, $nameHTML, $nameXML, $retrievedImageMessage, $retrievedImagePath, $retrievedImage;
	global $AUTH_USER, $AUTH_PWD;
	$pagename = "";
	$pageseq = "";
	$width = "";
	$height = "";
	$retrievedImageMessage = "";
	if (isset($_GET['pagename'])) { 
		$pagename = $_GET['pagename'];	// get the page name, called with URL like: http://.../latest.php&action=imagethumb&pagename=1&width=220
	}
	if (isset($_GET['pageseq'])) { 
		$pageseq = $_GET['pageseq'];	// get the sequence within document - starts with 1, called with URL like: http://.../latest.php&action=imagethumb&pageseq=0&width=220
	}
	if (isset($_GET['width'])) { // if no width given, the image is returned in original size
		$width = $_GET['width'];
	}
	if (isset($_GET['height'])) { // if no width given, the image is returned in original size
		$height = $_GET['height'];
	}
	
	$xml_pathname = $linkpath.rawurlencode($nameXML);
	if ($AUTH_USER != '') {
		$re = "://";
		$pathArr = mb_split($re, $xml_pathname, 2);
		$xml_pathname = $pathArr[0] . "://" . $AUTH_USER . ":" . $AUTH_PWD . "@" . $pathArr[1];
	}
	//echo("xml_pathname: " . $xml_pathname."<br>");
	//return(-1);
	$xmlf =  fget_curl($xml_pathname);
	//$xmlf = file_get_contents($xml_pathname);	// only works when PHP setting 'allow_url_fopen' is ON
												// so we have to use curl to be on safe side for most web hostings
	// TODO: in case of troubles we should check curl_errno($ch) and curl_error($ch)
	if ($xmlf === false) {
		$retrievedImageMessage = "XML file NOT FOUND: '".$xml_pathname."'";
		return(-1);
	}
	$xml = simplexml_load_string($xmlf);
	if ($xml === false) {
		$retrievedImageMessage = "XML file could not be parsed: '".$xml_pathname."'";
		return(-1);
	}
	$origimagename = "";
	
	if ($pageseq != "") $origimagename = $xml->xpath('//page[@page_sequence = \''.$pageseq.'\']/pageJPEG/@name');
	else if ($pagename != "") $origimagename = $xml->xpath('//page[@page_name = \''.$pagename.'\']/pageJPEG/@name/.');
	if (gettype($origimagename) == "array") $origimagename = $origimagename[0];

	if ($origimagename == "") {	// page not found
		$retrievedImageMessage = "Requested page image of page '".($pagename != "" ? $pagename : $pageseq)."' not found.";
		return(-2);
	}
	$origimagename = rawurlencode($origimagename);	// have to encode special chars like umlauts
	
	$retrievedImagePath = $linkpath.$origimagename;
	$retrievedImagePath = url_save( $retrievedImagePath );

	if ($AUTH_USER != '') {
		$re = "://";
		$pathArr = mb_split($re, $retrievedImagePath, 2);
		$retrievedImagePath = $pathArr[0] . "://" . $AUTH_USER . ":" . $AUTH_PWD . "@" . $pathArr[1];
	}
	//echo($retrievedImagePath."<br>\n");
	//return(-1);
	$originalImageString =  fget_curl($retrievedImagePath);
	//$originalImageString = file_get_contents($retrievedImagePath);	// only works when PHP setting 'allow_url_fopen' is ON
																		// so we have to use curl to be on safe side for most web hostings
	// TODO: in case of troubles we should check curl_errno($ch) and curl_error($ch)
	if($originalImageString ===  false) {
		$retrievedImageMessage = "Original image file not found at: '".$origimagename;
	 	return(-3);
	}
	$originalImage = imagecreatefromstring($originalImageString);
	if($originalImage ===  false) {
		$retrievedImageMessage = "Original image could not be created: '".$origimagename;
	 	return(-4);
	}
	if (($width == '') && ($height == '')) {	// no resizing: return original image
				/*
				echo("Saving original image<br>");
				$saved = imagejpeg ( $originalImage, "/tmp/myimage.jpg", 100 );
				if (!$saved) {
					imagedestroy($originalImage);
					$retrievedImage = null;
					$retrievedImageMessage = "image not saved: '".$origimagename;
					return(-6);
				}
				return(-1);
				*/
		$retrievedImage = $originalImage;
		return(0);
	}
	
	$origwidth = imagesx($originalImage);
	$origheight = imagesy($originalImage);
	$newwidth = ($width != '' ? intval($width) : -1);
	$newheight = ($height != '' ? intval($height) : -1);
	if (($newwidth > 0) && ($newheight > 0)) {	// fixed w h
	}
	else {
		if (($newwidth > 0) && ($newheight < 0)) {	// calculate resize ratio
			$ratio = $newwidth / $origwidth;
			$newheight = imagesy($originalImage) * $ratio;
		} else if (($newheight > 0) && ($newwidth < 0)) {	// calculate resize ratio
					$ratio = $newheight / $origheight;
					$newwidth = imagesx($originalImage) * $ratio;
				}
	}

	// resize the image
	$retrievedImage = imagecreatetruecolor($newwidth, $newheight);	// this is a base64 encoded png
	if($retrievedImage ===  false) {
		try { imagedestroy($originalImage); } catch (Exception $e) {}
		$retrievedImageMessage = "Original image file not found at: '".$originalImage;
	 	return(-5);
	}
	$resampleOK = imagecopyresampled($retrievedImage, $originalImage, 0, 0, 0, 0, $newwidth, $newheight, $origwidth, $origheight);
	if (!$resampleOK) {
		try { imagedestroy($originalImage); } catch (Exception $e) {}
		try { imagedestroy($retrievedImage); } catch (Exception $e) {}
		$retrievedImage = null;
		$retrievedImageMessage = "Original image could not be resampled: '".$origimagename;
		return(-6);
	}
	/*
	echo("Saving image newwidth: ".$newwidth." newheight: ". $newheight."<br>");
	$saved = imagejpeg ( $resampledImage, "/tmp/myimageResized.jpg", 100 );
	if (!$saved) {
		$retrievedImage = null;
		$retrievedImageMessage = "image not saved: '".$origimagename;
		return(-6);
	}
	*/
	try { imagedestroy($originalImage); } catch (Exception $e) {}

	//return ($retrievedImagePath." : ".$nameXML."<br>pagename: ".$pagename."<br>pageseq: ".$pageseq."<br>retrievedImagePath: ".$retrievedImagePath."<br>newwidth: ".$width."<br>origwidth: ".$origwidth."<br>");
	return (0);
}


function zip_archive($fileurl, $options) {
	/*
	 the folder to cach zip files, the $zipfolder, must have read/write for pHP/APACHE
	 use:
	 sudo chown -R _www:staff _zips
	 chmod -R 755 _zips
	*/
	$debug = false;
	$dataroot = "";
	$ziproot = "";
	$zipfolder = "_zips";
	$objshortcut = "";
	$objsubpath = "";
	$zipname = "";
	$zippathname = "";
	$zipurl = "";
	$xslcssBaseFolder = "";
	$ERROR = "";
	$usedimages = "";
	$pathurl = "";
	$htmfilename = "";
	$directories = explode("/",$fileurl);	// $fileurl is like:
											// http://192.168.1.36/EbnerVerlag/DATA/FM/2014/20140606/xyz.htm
											// and results in splitted array
											// ["http:","","192.168.1.36","EbnerVerlag","DATA","FM","2014","20140606","xyz.htm",""]
											// so "http://192.168.1.36/" points to web root = $_SERVER['DOCUMENT_ROOT']
											// next folders must be added
	function subPath($dirarray, &$dataroot, &$ziproot, &$zipfolder) {
		$sp = $_SERVER['DOCUMENT_ROOT'];
		for ($i = 3; $i < count($dirarray) - 1; $i++) {
			if ($dirarray[$i] != '') {
				$sp .= $dirarray[$i] . DIRECTORY_SEPARATOR;
				if (mb_strtolower($dirarray[$i], 'UTF-8') == 'data') {
					$dataroot = $sp;
					$ziproot = $dataroot . $zipfolder . DIRECTORY_SEPARATOR;
				}
			}
		}
		return($sp);
	}

	function getObjectShortcut($dirarray) {
		for ($i = (count($dirarray) - 2); $i >= 0; $i--) {
			if (is_numeric($dirarray[$i])) continue;	// year or date folder
			return $dirarray[$i];	// first non numeric folder seen from bottom
		}
		return("XX");
	}

	function getObjectSubpath($dirarray) {
		$i = 0;
		$subpath = "";
		while (mb_strtolower($dirarray[$i], 'UTF-8') != 'data') $i++;
		for ($i+1; $i < count($dirarray) - 1; $i++) {	// last is html file
			if ($dirarray[$i] != '') $subpath .= $dirarray[$i] . DIRECTORY_SEPARATOR;	// first non numeric folder seen from bottom
		}
		return $subpath;
	}
	function getXSLCSSPath($filepath, &$xslcssBaseFolder) {
		$i = 0;
		$dirs = explode("/",$filepath);
		$basepath = '';
		$xslcsspath = "";
		while ((mb_strtolower($dirs[$i], 'UTF-8') != 'data') && ($i < count($dirs))) {
			$basepath .= $dirs[$i] . DIRECTORY_SEPARATOR;
			$i++;
		}
		// $basepath is now like: /Users/Shared/Sites/EbnerVerlag/DATA/
		while (!file_exists($basepath . 'XSLCSS') && ($i < count($dirs))) {
			$basepath .= $dirs[$i] . DIRECTORY_SEPARATOR;
			$i++;
		}
		if ($i < count($dirs)) {
			$xslcsspath = $basepath . 'XSLCSS' . DIRECTORY_SEPARATOR;
			$xslcssBaseFolder = $dirs[--$i];
		}
		
		return($xslcsspath);
	}

	function getXSLCSSzipPath($fldrpath, &$xslcssBaseFolder, $debug = false) {
		$i = 0;
		$dirs = explode("/",$fldrpath);
		$zipxslpath = DIRECTORY_SEPARATOR;
		for ($i = 0; $i < count($dirs); $i++) {	
			$zipxslpath .= $dirs[$i] . DIRECTORY_SEPARATOR;
			if ($dirs[$i] == $xslcssBaseFolder) break;
		}
		return($zipxslpath);
	}

	function cutSubPath($fullpath, $subpath) {
		/*
		$subpath like:  /Users/Shared/Sites/EbnerVerlag/DATA/XSLCSS/
		$fullpath like: /Users/Shared/Sites/EbnerVerlag/DATA/XSLCSS/flipbook/_help/sb_navigation/pagePDF_button.png
		return: ending remaining path of $fullpath
		*/
		return(mb_substr($fullpath,mb_strlen($subpath),mb_strlen($fullpath),'UTF-8'));
	}

	function addObjectSubpathAndFiles(&$zip, $filepath, $htmfilename, &$objsubpath, $usedimages, &$ERROR, $debug = false) {
		global $options, $pathurl;
		$folders = explode("/",$objsubpath);
		$fldrpath = "";
		for ($i = 0; $i < count($folders); $i++) {
			if ($folders[$i] == '') continue;
			$fldrpath .= $folders[$i];
			if (!$zip->addEmptyDir($fldrpath)) {
				$ERROR = "##ERROR colud not add folder '" . $folders[$i] . "' to ZIP archive";
				return;
			}
			$fldrpath .= DIRECTORY_SEPARATOR;
		}
		
		// add the files
		if ($debug) {
			echo "<br>filepath => " . $filepath ."<br>";
			echo "fldrpath => " . $fldrpath ."<br>";
			echo "usedimages => " . $usedimages . " size=" . count($usedimages) ."<br>";
		}
		
		// copy flipbook files
		$iterator = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($filepath));
		// iterate over the directory and add each file found to the archive
		foreach ($iterator as $key=>$value) {
			if ((basename($key) == '.') || (basename($key) == '..') || (basename($key) == '.DS_Store')) continue;
			if (endsWith(basename($key),'.pdf') && (strpos($options,'nopdf') !== false)) continue;
			if ((endsWith(basename($key),'.xml') || endsWith(basename($key),'.xmi')) && (strpos($options,'noxml') !== false)) continue;
			if (endsWith(basename($key),'.htm') && (strpos($options,'nohtm') !== false)) continue;
			
			// check if JPEGS are really used
			if (endsWith(basename($key),'.jpg')) {
				$isused = in_array(basename($key), $usedimages);
				if ($debug) echo "TEST: " . basename($key) . " => " . ($isused === false ? 'NOT used' : 'is USED') . " - " .$isused ."<br>";
				if ($isused === false) continue;
			}

			if ($debug) echo "ADD ZIP FILE: " . realpath($key) . " => " . '/'.$fldrpath.basename($key) . "<br>";
			if (!$zip->addFile(realpath($key), '/'.$fldrpath.basename($key))) {
				$ERROR = "##ERROR colud not add file '" . $key . "' to ZIP archive";
				return;
			}
			
		}
			
		// add XSLCSS folder
		$xsllocalpath = getXSLCSSPath($filepath, $xslcssBaseFolder);
		$xslzippath = getXSLCSSzipPath($fldrpath, $xslcssBaseFolder, $debug);
		if ($debug) {
			echo "<br>xsllocalpath => " . $xsllocalpath ."<br>";
			echo "xslcssBaseFolder => " . $xslcssBaseFolder ."<br>";
			echo "xslzippath => " . $xslzippath ."<br>";
		}
		
		$iterator = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($xsllocalpath));
		// iterate over the directory and add each file found to the archive
		foreach ($iterator as $key=>$value) {
			if ((basename($key) == '.') || (basename($key) == '..')) continue;
			if ($debug) echo "ADD ZIP FILE: " . $key . " => " . $xslzippath . 'XSLCSS'. DIRECTORY_SEPARATOR. cutSubPath($key, $xsllocalpath) . "<br>";
			
			if (!$zip->addFile(realpath($key), $xslzippath . 'XSLCSS'. DIRECTORY_SEPARATOR. cutSubPath($key, $xsllocalpath))) {
				$ERROR = "##ERROR colud not add file '" . $key . "' to ZIP archive";
				return;
			}
		}


		// do we have to re-transform the HTM flipbook file?
		if (strpos($options,'nohtm') !== false) {
			if ($debug) {
				echo "<br>----- re-transform HTM file<br>";
				echo "filepath => " . $filepath ."<br>";
				echo "htmfilename => " . $htmfilename ."<br>";
				echo "objsubpath => " . $objsubpath ."<br>";
				echo "pathurl => " . $pathurl ."<br>";
			}
			$newhtm = transformHTM($filepath, $htmfilename, $pathurl, $debug);
			
			if (!$zip->addFromString('/'. $objsubpath . $htmfilename, $newhtm)) {
				$ERROR = "##ERROR colud not add file '" . $newhtm . "' to ZIP archive at path: " . '/'. $objpath;
				return;
			}
			
		}
		
	}
	
	function resolvePaths($abspath, $relpath) {
		$abs = explode(DIRECTORY_SEPARATOR, $abspath);
		$rel = explode(DIRECTORY_SEPARATOR, $relpath);
		$abslength = count($abs);
		$backpaths = 0;
		$resolved = '';
		for ($i = 0; $i < count($rel); $i++) {
			if ($rel[$i] == '..') $backpaths++;
		}
		if ($debug) echo ("<br>backpaths: " . $backpaths);
		$abslength -= ($backpaths + 1);
		for ($i = 0; $i < $abslength; $i++) if (($abs[$i] != '') && ($abs[$i] != '.')) $resolved .= DIRECTORY_SEPARATOR . $abs[$i];
		for ($i = $backpaths; $i < count($rel); $i++) $resolved .= DIRECTORY_SEPARATOR . $rel[$i];
		
		return $resolved;
	}
	
	function getUsedPageJpegs($htmpath, $htmname) {
		global $options, $debug;
		// get the xml file name
		if ($debug) echo ("<br>------- getUsedPageJpegs:<br>htmname: " . $htmname);
		$xml_filename = basename($htmname, ".htm").".xml";
		$xml_filename = $htmpath . $xml_filename;
		if ($debug) echo ("<br>xml_filename: " . $xml_filename);
		
		$xsl = new XSLTProcessor();

		$xmldoc = new DOMDocument();
		$xmldoc->load($xml_filename);
		$xpath = new DOMXpath($xmldoc);
		$xml_stylesheet = $xpath->evaluate('string(//processing-instruction()[name() = "xml-stylesheet"])');	// is like 'href="../../flipbook.xsl" type="text/xsl"'
		// parse out the href attribute
		$href_pos = mb_stripos($xml_stylesheet,'href="')+6;
		$xsl_filename = mb_substr($xml_stylesheet,$href_pos, mb_stripos($xml_stylesheet,'" ') - $href_pos);
		if ($debug) echo ("<br>htmpath: " . $htmpath . ' => xsl_filename: ' . $xsl_filename . '<br>');
		$xsl_filename = resolvePaths($htmpath, $xsl_filename);
		if ($debug) echo ("<br>xsl_filename: " . $xsl_filename .'<br>');

		$xsldoc = new DOMDocument();
		$xsldoc->load($xsl_filename);
		$xsl->importStyleSheet($xsldoc);

		$xsl->setParameter('','getjpgused','1');	// get comma separated list of used pgae JPGs
		if (strpos($options,'phone') !== false) $xsl->setParameter('','pagejpgsize','1');
		else if (strpos($options,'tablet') !== false) $xsl->setParameter('','pagejpgsize','2');
		// get the pages
		// a list of pages is returned, however with a starting DOCTOPE lin and ending element empty
		// clean this
		//	<!DOCTYPE html PUBLIC "" "">
		//	page1.jpg,page2.jpg,...
		$result = $xsl->transformToXML($xmldoc);
		$result = mb_ereg_replace("\r|\n","",$result);
		$result = mb_eregi_replace("<!DOCTYPE.*?>","",$result);
		$result = mb_eregi_replace(",$","",$result);
		// return the HTML result
		return $result;
	}
	
	function getUsedImages($htmpath, $htmname, $which = '4') {
		global $options, $debug;
		// get the xml file name
		if ($debug) echo ("<br>------- getUsedImages:<br>htmname: " . $htmname);
		$xml_filename = basename($htmname, ".htm").".xml";
		$xml_filename = $htmpath . $xml_filename;
		if ($debug) echo ("<br>xml_filename: " . $xml_filename);
		
		$xsl = new XSLTProcessor();

		$xmldoc = new DOMDocument();
		$xmldoc->load($xml_filename);
		$xpath = new DOMXpath($xmldoc);
		$xml_stylesheet = $xpath->evaluate('string(//processing-instruction()[name() = "xml-stylesheet"])');	// is like 'href="../../flipbook.xsl" type="text/xsl"'
		// parse out the href attribute
		$href_pos = mb_stripos($xml_stylesheet,'href="')+6;
		$xsl_filename = mb_substr($xml_stylesheet,$href_pos, mb_stripos($xml_stylesheet,'" ') - $href_pos);
		if ($debug) echo ("<br>htmpath: " . $htmpath . ' => xsl_filename: ' . $xsl_filename . '<br>');
		$xsl_filename = resolvePaths($htmpath, $xsl_filename);
		if ($debug) echo ("<br>xsl_filename: " . $xsl_filename .'<br>');

		$xsldoc = new DOMDocument();
		$xsldoc->load($xsl_filename);
		$xsl->importStyleSheet($xsldoc);

		$xsl->setParameter('','getjpgused',$which);	// get comma separated list of used images
		if (strpos($options,'phone') !== false) {
			$xsl->setParameter('','pagejpgsize','1');
		}
		else {
			if (strpos($options,'tablet') !== false) {
				$xsl->setParameter('','pagejpgsize','2');
			}
		}
		// a list of images is returned, however with a starting DOCTOPE lin and ending element empty
		// clean this
		//	<!DOCTYPE html PUBLIC "" "">
		//	img1.jpg,img2.jpg,...
		$result = $xsl->transformToXML($xmldoc);
		$result = mb_ereg_replace("\r|\n","",$result);
		$result = mb_eregi_replace("<!DOCTYPE.*?>","",$result);
		$result = mb_eregi_replace(",$","",$result);
		// return the HTML result
		return $result;
	}
	
	function transformHTM($htmpath, $htmname, $pathurl, $debug) {
		global $options, $objsubpath;
		// get the xml file name
		if ($debug) {
			echo ("<br>------- transformHTM:");
			echo ("<br>htmpath: " . $htmpath);
			echo ("<br>htmname: " . $htmname);
			echo ("<br>pathurl: " . $pathurl);
			echo ("<br>options: " . $options);
		}
		$xml_filename = basename($htmname, ".htm").".xml";
		$xml_filename = $htmpath . $xml_filename;
		if ($debug) echo ("<br>xml_filename: " . $xml_filename);
		
		$xsl = new XSLTProcessor();

		$xmldoc = new DOMDocument();
		$xmldoc->load($xml_filename);
		$xpath = new DOMXpath($xmldoc);
		$xml_stylesheet = $xpath->evaluate('string(//processing-instruction()[name() = "xml-stylesheet"])');	// is like 'href="../../flipbook.xsl" type="text/xsl"'
		// parse out the href attribute
		$href_pos = mb_stripos($xml_stylesheet,'href="')+6;
		$xsl_filename = mb_substr($xml_stylesheet,$href_pos, mb_stripos($xml_stylesheet,'" ') - $href_pos);
		if ($debug) echo ("<br>htmpath: " . $htmpath . ' => xsl_filename: ' . $xsl_filename . '<br>');
		$xsl_filename = resolvePaths($htmpath, $xsl_filename);
		if ($debug) echo ("<br>xsl_filename: " . $xsl_filename .'<br>');

		$xsldoc = new DOMDocument();
		$xsldoc->load($xsl_filename);
		$xsl->importStyleSheet($xsldoc);

		if (strpos($options,'phone') !== false) {
			$params = array ('pagejpgsize'=>'1','pagejpguse'=>'0');	// we copy the smaller JPEG only and use the first JPEG, this is size 0
			$xsl->setParameter('',$params);
		}
		else {
			if (strpos($options,'tablet') !== false) {
				$params = array ('pagejpgsize'=>'2','pagejpguse'=>'1');	// we copy the larger JPEG only, this is size 1
				$xsl->setParameter('',$params);
			}
		}
		$xsl->setParameter('','pdfurl',$pathurl);
		$xsl->setParameter('','imglargeurl',$pathurl);

		$result = $xsl->transformToXML($xmldoc);
		//echo "result => " . $result ."<br>";

		// return the HTML result
		return $result;
	}



	$pathurl = mb_substr($fileurl , 0, mb_strrpos($fileurl,'/',0,'UTF-8')+1, 'UTF-8');
	
	// the path into the local file system where the requested object's files reside
	$filepath = subPath($directories, $dataroot, $ziproot, $zipfolder);
	$htmfilename = $directories[count($directories)-1];
	
	// get the issue zip name
	// the object shortcut ist the non numeric folder name before the 4 digits year and 8 digits date folders
	$objshortcut = getObjectShortcut($directories);
	$objsubpath = getObjectSubpath($directories);
	
	// build the zip archive name
	$zipname = $objshortcut . "_" . $directories[count($directories) - 2];
	if (strpos($options,'tablet') !== false) $zipname .= '_tablet';
	else if (strpos($options,'phone') !== false) $zipname .= '_phone';
	$zipname .= ".zip";

	$zippathname = $ziproot . $zipname;

	if (!file_exists($zippathname)) {
		if ($debug) {
			echo "<br>****** start zipping<br>";
			echo "directories: [" . implode(",",$directories) . "]<br>";
		}
		do {
			// get allused page images
			if ( (strpos($options,'phone') !== false) || (strpos($options,'tablet') !== false) ) {
				$usedimages = getUsedImages($filepath, $htmfilename, '12');
				$usedimages = explode(",",$usedimages);
				if ($debug) echo "usedimages: [" . implode(",",$usedimages) . "]<br>";
			}
			
			// now create the zip archive
			$zip = new ZipArchive();
			if ($zip->open($zippathname, ZipArchive::CREATE) !== TRUE) {
				$ERROR = "##ERROR could not create ZIP archive";
				break;
			}
			if ($debug) echo "<br>zip created<br>";
			// ad object DATA /.../files
			addObjectSubpathAndFiles($zip, $filepath, $htmfilename, $objsubpath, $usedimages, $ERROR, $debug);

			// add the info file to html book file
			$zip->addFromString('/' . $zipname . '.json','{"local_bookurl":"' . $objsubpath . $directories[count($directories)-1]. '"}');
			
			// close this zip
			$zip->close();

			if (!file_exists($zippathname)) {
				$ERROR = "##ERROR ZIP archive could not be created";
				break;
			}
			
			// now we have a zip archive at local path like:
			//		/Users/Shared/Sites/EbnerVerlag/DATA/_zips/FM_20140606.zip
		} while(false);
	}
	// create the zip URL
	if (file_exists($zippathname)) {
		$zipurl = $directories[0] . "//" . $directories[2] . "/" . cutSubPath($zippathname, $_SERVER['DOCUMENT_ROOT']);
	}
	// get file size
	$fsize = filesize($zippathname);
	
	if ($debug) return("<br><br>fileurl=>" . $fileurl . "<br><br>pathurl=>" . $pathurl . "<br>directories=>".implode("*",$directories) . "<br>filepath=>" . $filepath . "<br>htmfilename=>" . $htmfilename . "<br>objsubpath=>" . $objsubpath . "<br>ziproot=>" . $ziproot . "<br>zippathname=>" . $zippathname . "<br>zipurl=>" . $zipurl . "<br>ERROR=> " . $ERROR . "<br><br>usedimages=> " . $usedimages );
	else return($zipurl. '**zipsize:' . $fsize);
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



?>

