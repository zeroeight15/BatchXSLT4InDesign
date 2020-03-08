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

Purpose: get the latest (from folder date) eBook file available on disk.
Currently undocumented. Ask me...

Version: 1.0
Version date: 20081005

Multibyte support must be enabled in PHP!
In the php.ini file, the line

extension=php_mbstring.dll

must be uncommented

===================================
*/
ob_start();
require_once("inc/getDataTree.php");	// include this class
include "inc/util.php";				// get utilities functions

$DEBUG = false;

/* get the sub path */
$sub_path = $_GET['sp'];	// get it called with URL like: http://.../add.php?cn="my company name"
if ($sub_path == "") {		// get search text field from form
	$sub_path = $_POST['sub_path'];
}
if (startsWith( $sub_path, "\\\"" ) == true) {	// remove enclosing double quotes
	$re = "\\\\\""; $sub_path = eregi_replace ( $re, "", $company_name );
}

/* get the object id */
$object_id = $_GET['o'];	// get it called with URL like: http://.../add.php?oi=BD
if ($object_id == "") {		// get search text field from form
	$object_id = $_POST['object_id'];
}
if (startsWith( $object_id, "\\\"" ) == true) {	// remove enclosing double quotes
	$re = "\\\\\""; $object_id = eregi_replace ( $re, "", $object_id );
}

/* get the issue year */
$issue_year = $_GET['y'];	// get it called with URL like: http://.../add.php?iy="2008"
if ($issue_year == "") {		// get search text field from form
	$issue_year = $_POST['issue_year'];
}
if (startsWith( $issue_year, "\\\"" ) == true) {	// remove enclosing double quotes
	$re = "\\\\\""; $issue_year = eregi_replace ( $re, "", $issue_year );
}

/* get the issue date */
$issue_date = $_GET['d'];	// get it called with URL like: http://.../add.php?id="20080324"
if ($issue_date == "") {		// get search text field from form
	$issue_date = $_POST['issue_date'];
}
if (startsWith( $issue_date, "\\\"" ) == true) {	// remove enclosing double quotes
	$re = "\\\\\""; $issue_date = eregi_replace ( $re, "", $issue_date );
}

// this is the sub-path to store data
$data_sub_path = "";
if ($sub_path != "") $data_sub_path .= $sub_path . "/"; 
if ($object_id != "")    $data_sub_path .= $object_id . "/"; 
if ($issue_year != "")   $data_sub_path .= $issue_year . "/"; 
if ($issue_date != "")   $data_sub_path .= $issue_date; 
// get the base data path
$docpath = realpath(dirname(__FILE__));
$webroot = realpath(realpath(dirname(__FILE__)) . "/../..");
$base_datapath = $webroot . $dataRootPath;

if ($DEBUG) {
	echo "---------- request headers --------<br>\n";
	echo 'HTTP_HOST: ' . $_SERVER['HTTP_HOST'] . "<br>\n";
	echo 'REQUEST_URI: ' . $_SERVER['REQUEST_URI'] . "<br>\n";
	echo 'DOCUMENT_ROOT: ' . $_SERVER['DOCUMENT_ROOT'] . "<br>\n";
	echo 'HTTP_HOST: ' . $_SERVER['HTTP_HOST'] . "<br>\n";
	
	echo "---------- parameters --------<br>\n";
	echo "sub_path: " . $sub_path . "<br>\n";
	echo "object_id: " . $object_id . "<br>\n";
	echo "issue_year: " . $issue_year . "<br>\n";
	echo "issue_date: " . $issue_date . "<br>\n";
	echo "docpath: " . $docpath . "<br>\n";
	echo "webroot: " . $webroot . "<br>\n";
	echo "base_datapath: " . $base_datapath . "<br>\n";
	echo "data_sub_path: " . $data_sub_path . "<br>\n";
	echo "---------- END parameters --------<br>\n";
}

$getDataTree = new getDataTree();
$latest_path = $getDataTree->getTreePath($base_datapath, $data_sub_path);
$findfile = $latest_path . "/" . $object_id . "_*.htm";
$files = glob  ( $findfile );
$num_files = sizeof($files);
if ($DEBUG) {
	echo "latest_path: " . $latest_path . "<br>\n";
	echo "findfile: " . $findfile . "<br>\n";
	echo "num_files: " . $num_files . "<br>\n";
}

$file_returned = true;
if ($num_files > 0) {
	// return the file
	try {
		$fileSubPath = substr($files[0],strlen($webroot)+1);
		$fullPath = "http://" . $_SERVER['HTTP_HOST'] . "/" . $hostSubPath . "/" . $fileSubPath;
		$fullPath = url_save($fullPath);
		if ($DEBUG) {
			echo "file: " . $files[0] . "<br>\n";
			echo 'HTTP_HOST: ' . $_SERVER['HTTP_HOST'] . "<br>\n";
			echo 'hostSubPath: ' . $hostSubPath . "<br>\n";
			echo "fileSubPath: " . $fileSubPath . "<br>\n";
			echo "fullPath: " . $fullPath . "<br>\n";
		}
		else header("Location: {$fullPath}");
		ob_flush();
		//readfile($files[0]);
	}
	catch (exception $e) {
		$file_returned = false;
	}
}

function url_save( $str ) {
	$newstr = ereg_replace(" ", "%20", $str);
	$newstr = ereg_replace("\?", "%3F", $newstr);
	$newstr = ereg_replace("\&", "%26", $newstr);
	return ($newstr);
}

?>
