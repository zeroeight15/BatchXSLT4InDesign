<?xml version="1.0" encoding="UTF-8"?>
<!-- 
 ======================================================================
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

Version: 1.0
Version date: 20110724
======================================================================
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				xmlns:lxslt="http://xml.apache.org/xslt"
				xmlns:xalan="http://xml.apache.org/xalan" 
				xmlns:ssdoc="x-schema:http://www.scansoft.com/omnipage/xml/ssdoc-schema2.xml" 
				xmlns:myutils="com.epaperarchives.batchxslt.utils"
				xmlns:locjs="loc_funcs"
								
				extension-element-prefixes="locjs"
				exclude-result-prefixes="locjs myutils xalan lxslt ssdoc"
				version="1.0">
	<xsl:output method="html" />
	<xsl:output doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" />
	<xsl:output doctype-system="http://www.w3.org/TR/html4/loose.dtd" />
	<xsl:output media-type="text/html"/>
	<xsl:output indent="yes"/>
	<xsl:output encoding="UTF-8"/>



<!-- ================ HERE WE GO ================ -->
	<xsl:template match="/">
		<xsl:apply-templates select="ssdoc:document" />
	</xsl:template>

	<xsl:template match="ssdoc:document">

		<html>
		<head>
			<title><xsl:value-of select="@title" /></title>
			<link type="text/css" rel="StyleSheet" href="omnidocs.css"></link>
		</head>
		<body id="body" class="docbody">

			<xsl:for-each select="ssdoc:page">
				<xsl:call-template name="get_page_coords" />

				<xsl:variable name="pagewidth" select="locjs:get_pagewidth()"/>
				<xsl:variable name="pageheight" select="locjs:get_pageheight()"/>
				<div style="position:relative; top:0; left:0; border:2px solid #888; width:{$pagewidth}px; height:{$pageheight}px;">
					<img class="pageimage" style="width:{$pagewidth}px; height:{$pageheight}px;" src="bund-cut.jpg"/>
					<xsl:apply-templates select="ssdoc:region" />
				</div>
			</xsl:for-each>
		</body>
		</html>
	</xsl:template>



	<xsl:template name="get_page_coords"><!-- get page size -->
		<xsl:variable name="d" select="locjs:store_pageimageDPIx(string(@x-res))"/><!-- store DPI BEFORE page sizes -->
		<xsl:variable name="d" select="locjs:store_pageimageDPIy(string(@y-res))"/>
		<xsl:variable name="d" select="locjs:store_pageimageSIZEx(string(@width))"/>
		<xsl:variable name="d" select="locjs:store_pageimageSIZEy(string(@height))"/>
	</xsl:template>

	<xsl:template match="ssdoc:region"><!-- get regions -->
		<div style="position:absolute; top:{locjs:twip2px(string(ssdoc:rc/@t),'y')}px; left:{locjs:twip2px(string(ssdoc:rc/@l),'x')}px; width:{locjs:twip2pxLength(string(ssdoc:rc/@l), string(ssdoc:rc/@r), 'x')}px; height:{locjs:twip2pxLength(string(ssdoc:rc/@t), string(ssdoc:rc/@b), 'y')}px; border:1px solid blue;">
			<xsl:apply-templates select="ssdoc:paragraph"/>
		</div>
	</xsl:template>

	<xsl:template match="ssdoc:paragraph"><!-- get paragraph -->
		<div class="paragraph">
			<xsl:for-each select="ssdoc:ln"><!-- get lines -->
				<xsl:variable name="linecontent">
					<xsl:call-template name="get_words" >
						<xsl:with-param name="lastLineInPara" select="position() = last()"/>
					</xsl:call-template>
				</xsl:variable>

				<!-- calc class attribute -->
				<xsl:variable name="class">
					<xsl:if test="@fs"><xsl:value-of select="locjs:getClassAttribute(string(@fs))"/></xsl:if>
				</xsl:variable>
				<!-- calc style attribute -->
				<xsl:variable name="style">
					<xsl:if test="@char-attr = 'bold'">font-weight:bold;</xsl:if>
					<xsl:if test="@char-attr = 'italic'">font-style:italic;</xsl:if>
				</xsl:variable>

				<xsl:choose>
					<xsl:when test="($class != '') or ($style != '')">
						<span>
							<xsl:if test="$class != ''"><xsl:attribute name="class"><xsl:value-of select="$class"/></xsl:attribute></xsl:if>
							<xsl:if test="$style != ''"><xsl:attribute name="style"><xsl:value-of select="$style"/></xsl:attribute></xsl:if>
							<xsl:copy-of select="$linecontent"/></span>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy-of select="$linecontent"/>
					</xsl:otherwise>
				</xsl:choose>

			</xsl:for-each>
		</div>
	</xsl:template>

	<xsl:template name="get_words"><!-- get words from lines -->
		<xsl:param name="lastLineInPara"/>
		<xsl:for-each select="ssdoc:wd"><!-- get words -->

			<xsl:variable name="textcontent">
				<xsl:apply-templates>
					<xsl:with-param name="lastLineInPara" select="$lastLineInPara"/>
					<xsl:with-param name="lastWordInLine" select="position() = last()"/>
				</xsl:apply-templates>
			</xsl:variable>
			
			<!-- calc style attribute -->
			<xsl:variable name="style">
				<xsl:if test="@char-attr = 'bold'">font-weight:bold;</xsl:if>
				<xsl:if test="@char-attr = 'italic'">font-style:italic;</xsl:if>
			</xsl:variable>

			<xsl:choose>
				<xsl:when test="$style != ''">
					<span style="{$style}"><xsl:value-of select="$textcontent"/></span>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$textcontent"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>

	</xsl:template>


	<xsl:template match="space"><xsl:text> </xsl:text></xsl:template>


	<!-- ===============================================
		 the plain text content
	     =============================================== -->
	<xsl:template match="text()">
		<xsl:param name="lastLineInPara"/>
		<xsl:param name="lastWordInLine"/>
		<xsl:choose>
			<xsl:when test=". = '&#x0a;'"></xsl:when><!-- remove any combination of line breaks -->
			<xsl:when test=". = '&#x0d;'"></xsl:when>
			<xsl:when test=". = '&#x0d;&#x0a;'"></xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="$lastLineInPara and $lastWordInLine">
						<xsl:value-of select="."/><xsl:text> </xsl:text>
					</xsl:when>
					<xsl:when test="$lastWordInLine = true()">
						<xsl:variable name="endsWithDivis" select="locjs:endsWith(string(.), '-')"/>
						<xsl:value-of select="locjs:cleanLastWordContent(string(.))"/>
						<xsl:if test="not($endsWithDivis)"><xsl:text> </xsl:text></xsl:if><!-- do not ad a space if we removed a hyphen/divis -->
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="."/><xsl:text> </xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>



	<!-- ===============================================
		 stuff to suppress
	     =============================================== -->





	<!-- =========================
		 our local JAVA Script functions: MERGED
		 ========================= -->
	<lxslt:component prefix="locjs">
	<lxslt:script lang="javascript">
	//<![CDATA[
var pageimageDPIx = 240;
var pageimageDPIy = 240;
var pageimageSIZEx = 0;
var pageimageSIZEy = 0;
var pageimageRESIZEx = 1.2381;	// page image size/coords re-scale X. positive = page image is too small: enlarge coords
var pageimageRESIZEy = 1.2381;	// page image size/coords re-scale Y
var targetDPI = 72;
var targetDPIfactor = pageimageDPIx / targetDPI;

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

function getClassAttribute(fontsize) {
	var classname = "";
	var fntsz = parseInt(fontsize);
	do {
		if (fntsz >= 2000) { classname = "F2000"; break; }
		if (fntsz >= 1500) { classname = "F1500"; break; }
		if (fntsz >= 1000) { classname = "F1500"; break; }
		if (fntsz >= 850) { classname = "F850"; break; }
		classname = "Fnorm";
	} while(false);
	return(classname);
}


function cleanLastWordContent(txt) {
	if (txt == "") return("");
	if (txt == "-") return("-");
	var newtxt = txt;
	if (newtxt.charAt(newtxt.length-1) == "-") newtxt = newtxt.substr(0,newtxt.length-1);
	return(newtxt);
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
function get_date_from_docpath(path) {
	var parts = path.split("/");
	return(parts[parts.length-2]);
}
	//]]>
	</lxslt:script>
	</lxslt:component>


</xsl:stylesheet>
