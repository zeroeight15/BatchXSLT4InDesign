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

Version: 1.0
Version date: 20080503

Multibyte support must be enabled in PHP!
In the php.ini file, the line

extension=php_mbstring.dll

must be uncommented

===================================
*/



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
	$text = $text . " ";
	$text = mb_substr($text, $start, $maxchars);
	if (mb_strlen($text) >= $maxchars) $adddots = true;
	$text = mb_substr($text, 0, mb_strrpos($text,' '));

	if ($start > 0) $text = "..." . $text;
	if ($adddots) $text = $text . "...";
	return $text;
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

