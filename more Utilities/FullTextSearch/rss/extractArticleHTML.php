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

PURPOSE:
------------
Extract a sigle article from an epaper HTML file exported with "BatchXSLT for InDesign".

REQUIREMENTS:
-------------
PHP multibyte support enabled
Uses the library 'simple_html_dom.php'

HOW TO CALL:
------------
Syntax:
http://web_path/extractArticleHTML.php
	parameters:
	d=full path/URI to epaper HTML file
	a=Article ID number
	p= optional page number

Example:
http://www.domain.com/epaper/rss/extractArticleHTML.php?p=1&a=5&d=http://www.domain.com/epaper/DATA/company/objName/XY/2012/20120507/XY_20120507_20.htm

===================================
*/
include('simplehtmldom/simple_html_dom.php');


$get_document = "";
if (isset($_GET['d'])) { 
	$get_document = $_GET['d'];	// get the path to the html document like: 'http://mydomain.com/epaper/main/vacation/2011_small/20110602/Vacation.htm'
}
if ($get_document == "") {
	echo "no document path given";
	exit(1);
}

$css = "";
if (isset($_GET['css'])) { 
	$css = $_GET['css'];	// the desired CSS to include from the get_document: <link....
}

$articleID = "";
if (isset($_GET['a'])) { 
	$articleID = $_GET['a'];	// the articles's ID to ectract like: data-aid="15"
}

$pageLanguage = "";		// may be set to any language code as default
if (isset($_GET['l'])) { 
	$pageLanguage = $_GET['l'];	// the articles's language
}

$head=null;
$body=null;

$basepath = dirname($get_document)."/";	// is the full path/URI http:// www.mydomain.com/path to epaper file.htm
$document = $get_document;
$csspath = $basepath;

// clean/repair the document url 
$docURL_search = array ("/ /");
$docURL_replace = array ("%20");
$document = preg_replace($docURL_search, $docURL_replace, $document);


$artMainPartsArr = array();	// stores processed article IDs

// For security reasons, on some systems the http wrapper is disabled
// if possible, we have to open the file from the local system
if (false) {	// enable (true) this to turn the URI into alocal file acces path
	$search = array ("/http:\/\/www\.mydomain.com\/epaper/");
	$replace = array ("..");
	$document = preg_replace($search, $replace, $document);	// $document now contains a local access path
}
/*
echo "get_document: '" .$get_document."'<br>\n";
echo "articleID: '" .$articleID."'<br>\n";
echo "css: '" .$css."'<br>\n";
echo "local basepath: '" .$basepath."'<br>\n";
echo "document name: '" .$document."'<br>\n";
echo "==================<br>\n";
*/

// Create DOM from URL or file
$html = file_get_html($document);
if (!$html) {
	exit(1);
}

//**************** manipulate head URLs
echo "<!DOCTYPE html>\n";
if ($pageLanguage == "") echo "<html>\n";
else echo "<html lang=\"" . $pageLanguage . "\">\n";
echo "<head>\n";
echo "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n";

$head = $html->find('head',0);
if (!$head) {
	exit(1);
}

// Find all link and script elements
foreach($head->find('link') as $e) {
	$href = $e->href;
	$pos = mb_strpos ( $href , '/XSLCSS');
	if ($pos === false) {}
	else {
		$pos = mb_strpos ( $href , '/slidebook.css');
		if ($pos !== false) continue;
		$pos = mb_strpos ( $href , '/searchlocal.css');
		if ($pos !== false) continue;
		// this probably is a css we need
		// we have to change rel path to absolute path
		$href = $e->href;
		$cssname = basename($e->href);
		$cssrelpath = dirname($e->href);
		$cssabspath = rel2abs($e->href, $csspath);
		$e->href = $cssabspath;

		echo($e->outertext);
	}
}

echo "\n</head>\n";


//**************** manipulate body  URLs
echo "<body>\n";

$body = $html->find('body',0);
do {
	if (!$body) {
		echo("no body");
		break;
	}
	$fullarticle = "";
	
	getArticlePart($articleID);
	
	// output entire article
	echo($fullarticle);
} while(false);
echo "</body>\n</html>\n";

// clean up... 
$html->clear(); 
unset($html);
exit(0);

/******************************************/
function getArticlePart($articleID) {
	global $fullarticle, $body, $artMainPartsArr;
	
	// the main container with article ID like:  id="Art53_5" (the hidden div containing article container divs with class="Artcl_container")
	if (in_array($articleID,$artMainPartsArr)) return;
	$artMainPartsArr[] = $articleID;	// store this article id as processed
	
	$artMainID = "Art" . $articleID . "_";	// we should find it without the ending page number
	//echo "artMainID: " . $artMainID;
	$artMain = $body->find("div[id^=$artMainID]",0);	// starting part of article
	fixCommentedImageLinks($artMain);

	$Artcl_containers = $artMain->find("div[class=Artcl_container]");	// get all article containers.....
	//echo " -".count($Artcl_containers)."* ";
	foreach($Artcl_containers as $e) {
		if ($e->getAttribute('data-pg') == "") continue;	// ... however, drop anchored div - they are already contained in the main article
		$fullarticle .= $e;	// add this article part
	}
	
	// now get more parts chained to this article: we get the attribute 'data-n_id' of 
	foreach($Artcl_containers as $e) {
		if ($e->hasAttribute('data-n_id')) {
			$nextpartID = $e->getAttribute('data-n_id');	// like data-n_id="60"
			getArticlePart($nextpartID);
		}
	}
}


function fixCommentedImageLinks($thearticlepart) {
	global $basepath;

	//--- replace commented image paths
	//    like: <!--nimage style="border:1px solid #000000;" src="u144019_pakistan.jpg" alt="pakistan.psd" title=""/image-->
	// with valid image tag
	//    <img style="border:1px solid #000000;" src="FULL_PATH + u144019_pakistan.jpg" alt="pakistan.psd" title="">

	$imgcomments = $thearticlepart->find('comment');
	//echo " IC".count($imgcomments);
	foreach($imgcomments as $imc) {
		$pos_nimage = strpos ( $imc , 'nimage');
		if ($pos_nimage === false) continue;
		$pos_src = strpos ( $imc , "src=\"$basepath");	// stop if already done for anchored images
		if ($pos_src !== false) continue;
		$pos_src = strpos ( $imc , "src=\"");
		if ($pos_src === false) continue;
		// we have src attribute in a commented image
		$imgElem = $imc->outertext;
		$search = array ("/!--nimage/", "/ src=\"/" , "/\/image-->/");
		$replace = array ("img", " src=\"$basepath" , ">");
		$imgElem = preg_replace($search, $replace, $imgElem);

		$imc->outertext = $imgElem;
	}
}


function rel2abs($rel, $base) {
	/* return if already absolute URL */
	if (parse_url($rel, PHP_URL_SCHEME) != '') return $rel;

	/* queries and anchors */
	if ($rel[0]=='#' || $rel[0]=='?') return $base.$rel;

	/* parse base URL and convert to local variables:
	 $scheme, $host, $path */
	extract(parse_url($base));

	/* remove non-directory element from path */
	$path = preg_replace('#/[^/]*$#', '', $path);

	/* destroy path if relative url points to root */
	if ($rel[0] == '/') $path = '';

	/* dirty absolute URL */
	$abs = "$host$path/$rel";

	/* replace '//' or '/./' or '/foo/../' with '/' */
	$re = array('#(/\.?/)#', '#/(?!\.\.)[^/]+/\.\./#');
	for($n=1; $n>0; $abs=preg_replace($re, '/', $abs, -1, $n)) {}

	/* absolute URL is ready! */
	return $scheme.'://'.$abs;
}

?>

