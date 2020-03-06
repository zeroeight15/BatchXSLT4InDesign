<?xml version="1.0" encoding="UTF-8"?>
<!-- 
 ======================================================================
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.

THE PRODUCER:
Andreas Imhof
ai@aiedv.ch

Short Description: Flipbook XML/HTML
Description: Convert InDesign document content to flippable pages eBook XML/HTML.

Version: 46
Version date: 20200118
History:
20200118    Added support for images exported by InDesign
20141118		Fix/re-added meta viewport for IEMobile/11 (yes indeed)
20140828		addition url to PDFs and enlarged images for remote download
				request list of used images: getjpgused
V40 20140807: Add javascript for shortened version message
			  add the ability to process a certain number of pages only given in the xsl:param="maxpages"
			  suppress page and/or document PDFs
			  suppress all article text
			  removed frame border of content @type="image" - border is added to the img element
V39 20140712: fixed duplicated text from label divs on buttons
V38 20140627: enhanced video playing
			- enhanced multiple page JPEG sizes
			- fixed object styles handling
V37 Ai:		- enhanced/changed <a href and onclick attributes
V36 Ai:		- added xsl:output indent="no" to eliminate unwanted white-space
			and pretty print new line text after echa desired element like 'div'
			This leads to correct content for <span are not on a new line
			- added attribute '@data-objstyle' in <div class="Artcl_container" ... data-objstyle="ObjectStyle/Marginal_left_image">

			change jquery call:
			from specific version 'jquery-1.9.1.min.js'
			to 'jquery-min.js
			so any new jquery versions can be loaded
V35.1 Ai:	changed comment type of inline script from //name;type to /*name;type*/
			simplehtmldom struggled at this point when including html body content into a div

======================================================================
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				version="1.0">
	<xsl:output method="html" />
	<!-- *** DOCTYPE stuff -->
	<!-- setting doctype-public and doctype-system to empty works for all browsers but Chrom (security issue when displaying XML from local disk -->
	<xsl:output doctype-public="" />
	<xsl:output doctype-system="" />
	<!-- also might use this for HTML5 output:
	<xsl:output doctype-system="about:legacy-compat"/>
	-->
	<!-- served textual below: but  does not work for Firefox -->
	<!-- *** end DOCTYPE stuff -->

	<xsl:output media-type="text/html"/>
	<xsl:output encoding="UTF-8"/>
	
	<xsl:output indent="no"/>
	<!--xsl:strip-space elements="*" /-->


<!-- ================ base variables ================ -->
	<xsl:param name="maxpages" select="0"/><!-- the maximum number of pages to process or 0 to process all -->
	<xsl:param name="noarticles" select="0"/><!-- suppress all articles and mouse overs: 0 = do suppress, otherwise show articles -->
	<xsl:param name="nopdf" select="0"/><!-- suppress all PDF: 0 = do suppress, otherwise show PDF -->
	<xsl:param name="nopagepdf" select="0"/><!-- suppress pages PDF: 0 = do suppress, otherwise show PDF -->
	<xsl:param name="nodocpdf" select="0"/><!-- suppress document PDF: 0 = do suppress, otherwise show PDF -->

	<xsl:param name="pdfurl"/><!-- the path URL to page/document PDF. empty for no additional path -->
	<xsl:param name="imglargeurl"/><!-- the path URL to enlarged images img2. empty for no additional path -->
	<xsl:param name="pagejpgsize"/><!-- the size of page JPEGs to get -->
	<xsl:param name="pagejpguse"/><!-- the size of page JPEGs to use -->
	<xsl:param name="getjpgused" select="0"/><!-- addable flags:  '0' = get flipbook. '1' = get list of used page JPEGs, '2' get list of used images in elemets 'img', '4' get list of used images in elemets 'img*' -->
	<xsl:param name="addwebappcapable" select="1"/><!-- '1' = set meta name="mobile-web-app-capable" content="yes" -->

	<xsl:variable name="xslcssSubPath">flipbook/</xsl:variable><!-- the subfolder within XSLCSS -->

<!-- ================ output control variables ================ -->
	<xsl:variable name="suppress_top_navbar">0</xsl:variable><!-- default = 0. set to 1 to suppress the whole top head - page navigation bar -->
	<xsl:variable name="suppress_bottom_toolbar">0</xsl:variable><!-- default = 0. set to 1 to suppress the bottom PDF toolbar -->
	<xsl:variable name="suppress_loadstatus">0</xsl:variable><!-- default = 0. set to 1 to suppress the loading progress -->
	<xsl:variable name="suppress_infomessage">0</xsl:variable><!-- default = 0. set to 1 to suppress the info message text below pages -->

	<xsl:variable name="suppress_all_mouseovers">0</xsl:variable><!-- default = 0. set to 1 to completely suppress the mouse over function -->
	<xsl:variable name="suppress_text_mouseovers">0</xsl:variable><!-- default = 0. set to 1 to suppress all text and the mouse over text -->
	<xsl:variable name="suppress_image_mouseovers">0</xsl:variable><!-- default = 0. set to 1 to suppress all images and the mouse over images -->
	<xsl:variable name="suppress_emptybox_mouseovers">0</xsl:variable><!-- default = 1. set to 1 to suppress mouse over on empty boxes or 0 to show mouse overs on empty boxes -->

	<xsl:variable name="suppressExportMouseOvers"><xsl:value-of select="/indd_document/call_parameters/par[@name = 'suppressExportMouseOvers']/."/></xsl:variable><!-- addable flags to suppress export and mouse overs in output html flipbook web site
															0 = default = do all mouse overs
															1 to completely suppress the mouse over function
															2 to suppress all text and the mouse over text
															4 to suppress all images and the mouse over images
															8 to suppress mouse over empty boxes
															add each flag as it own char like: '14' -->

	<xsl:variable name="suppress_empty_divs">0</xsl:variable><!-- default = 0. set to 0 to add a non breaking space in empty divs for it is not treated as vertical white space by browsers. 1 menas that it is left empty and might be suppressed by browsers -->

	<xsl:variable name="show_article_in_new_window">0</xsl:variable><!-- default = 0. set to 1 to show articles always in a new window -->

	<xsl:variable name="navigationbar_position"></xsl:variable><!-- default = empty = pre-set navigation on both sides. 1 = set on left side, 2 = set on right side -->

	<xsl:variable name="debug_labels">0</xsl:variable><!-- default = 0. set to 1 to show box labels within text in red -->
	<xsl:variable name="debug_ctrlchar">0</xsl:variable><!-- default = 0. set to 1 to show control character codes within text in red -->

	<xsl:variable name="debug_xmlelement">0</xsl:variable><!-- default = 0. set to 1 to visually mark elements 'xmlElement' -->
	<xsl:variable name="structure_xmlelement">0</xsl:variable><!-- default = 0. set to 1 to structure 'xmlElement' -->
	<xsl:variable name="addVirtualKeyboard">0</xsl:variable><!-- default = 0. set to 1 to include the js and css for VKI Virtual Keyboard Interface -->
	<xsl:variable name="with_textshortcuts">1</xsl:variable><!-- default = 0. set to 1 to include textshortcuts -->



<!-- ================ HERE WE GO ================ -->
	<!-- create the XHTML ePaper pages -->
	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="($getjpgused = '0') or ($getjpgused = '')">
				<!--xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;&#10;</xsl:text--><!-- does not work for Firefox -->
				<xsl:apply-templates select="indd_document"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="contains($getjpgused,'1')">
					<xsl:call-template name="getpagejpgusage"/>
				</xsl:if>
				<xsl:if test="contains($getjpgused,'2')">
					<xsl:call-template name="getimagejpgusage"/>
				</xsl:if>
				<xsl:if test="contains($getjpgused,'4')">
					<xsl:call-template name="getpagejpgusage"/>
					<xsl:call-template name="getimagejpgusage"><xsl:with-param name="which" select="1"/></xsl:call-template>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<xsl:template name="getpagejpgusage">
		<xsl:for-each select="/indd_document/page">
			<xsl:call-template name="list_pageJPEGS"/><xsl:text>,</xsl:text>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="getimagejpgusage">
		<xsl:param name="which"/>
		<xsl:choose>
			<xsl:when test="($which = '') or ($which = '1')">
				<xsl:for-each select="/indd_document//img">
					<xsl:value-of select="@src"/><xsl:text>,</xsl:text>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="/indd_document//img | /indd_document//img2 | /indd_document//img3 | /indd_document//img4">
					<xsl:value-of select="@src"/><xsl:text>,</xsl:text>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>




<!-- ================ Normal operation = make the HTML flipbook ================ -->
	<xsl:template match="indd_document">
		<html>
<xsl:text>
</xsl:text><!-- pretty print -->
		<head>
<xsl:text>
</xsl:text><!-- pretty print -->
			<title>
				<xsl:choose>
					<xsl:when test="/indd_document/header/headerfield[@name='titleHead']/. != ''"><xsl:value-of disable-output-escaping="yes" select="/indd_document/header/headerfield[@name='titleHead']/text()"/></xsl:when>
					<xsl:when test="(/indd_document/header/headerfield[@name='title']/. != '') and (contains(/indd_document/header/headerfield[@name='title']/.,'&lt;') = false)"><xsl:value-of disable-output-escaping="yes" select="/indd_document/header/headerfield[@name='title']/text()"/></xsl:when>
					<xsl:when test="(/indd_document/header/headerfield[@name='description']/. != '') and (contains(/indd_document/header/headerfield[@name='description']/.,'&lt;') = false)">
								<xsl:value-of select="/indd_document/header/headerfield[@name='description']/."/>
					</xsl:when>
					<xsl:when test="(/indd_document/header/headerfield[@name='creator']/. != '') and (contains(/indd_document/header/headerfield[@name='creator']/.,'&lt;') = false)"><xsl:value-of select="/indd_document/header/headerfield[@name='creator']/."/></xsl:when>
					<xsl:when test="(/indd_document/header/headerfield[@name='WebStatement']/. != '') and (contains(/indd_document/header/headerfield[@name='WebStatement']/.,'&lt;') = false)"><xsl:value-of select="/indd_document/header/headerfield[@name='WebStatement']/."/></xsl:when>
				</xsl:choose>
			</title>
<xsl:text>
</xsl:text><!-- pretty print -->

			<xsl:if test="$addwebappcapable != '0'">
				<meta name="mobile-web-app-capable" content="yes"/>
				<meta name="apple-mobile-web-app-capable" content="yes"/>
<xsl:text>
</xsl:text><!-- pretty print -->
			</xsl:if>

			<xsl:if test="/indd_document/header/headerfield[@name='subject']/.">
				<meta name="keywords">
					<xsl:attribute name="content"><xsl:value-of select="/indd_document/header/headerfield[@name='subject']/."/></xsl:attribute>
				</meta>
<xsl:text>
</xsl:text><!-- pretty print -->
			</xsl:if>

			<xsl:if test="/indd_document/header/headerfield[@name='description']/.">
				<meta name="description">
					<xsl:choose>
						<xsl:when test="contains(/indd_document/header/headerfield[@name='description']/.,'*#*')">
							<xsl:attribute name="content"><xsl:value-of select="substring-before(/indd_document/header/headerfield[@name='description']/.,'*#*')"/></xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="content"><xsl:value-of select="/indd_document/header/headerfield[@name='description']/."/></xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
				</meta>
<xsl:text>
</xsl:text><!-- pretty print -->
			</xsl:if>

			<xsl:if test="/indd_document/call_parameters/par[@name = 'metaViewportContent']/. != ''">
				<meta name="viewport">
					<xsl:attribute name="content"><xsl:value-of select="/indd_document/call_parameters/par[@name = 'metaViewportContent']/."/></xsl:attribute>
				</meta>
			</xsl:if>

			<!-- load all CSS BEFORE the scripts -->
			<!-- first, the document's css -->
				<!-- this URL-encodes the CSS name if it contains foreign language chars -->
				<!--
				<link type="text/css">
					<xsl:attribute name="rel">StyleSheet</xsl:attribute>
					<xsl:attribute name="href"><xsl:value-of disable-output-escaping="yes" select="doctypeinfos/@csspath"/></xsl:attribute>
				</link>
				-->
			<!-- therefore write as text -->
			<xsl:text disable-output-escaping="yes">&lt;link type="text/css" rel="StyleSheet" href="</xsl:text><xsl:value-of disable-output-escaping="yes" select="doctypeinfos/@csspath"/><xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
<xsl:text>
</xsl:text><!-- pretty print -->

			<!-- and now the application's css -->
			<link type="text/css">
				<xsl:attribute name="rel">StyleSheet</xsl:attribute>
				<xsl:attribute name="href"><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>flipbook.css</xsl:attribute>
			</link>
<xsl:text>
</xsl:text><!-- pretty print -->
			<link type="text/css">
				<xsl:attribute name="rel">StyleSheet</xsl:attribute>
				<xsl:attribute name="href"><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>searchlocal.css</xsl:attribute>
			</link>
<xsl:text>
</xsl:text><!-- pretty print -->

			<!-- load the virtual keyboard interface JS -->
			<xsl:if test="/indd_document/call_parameters/par[@name = 'addVirtualKeyboard'] and (/indd_document/call_parameters/par[@name = 'addVirtualKeyboard']/. != '0')">
				<link type="text/css">
					<xsl:attribute name="rel">StyleSheet</xsl:attribute>
					<xsl:attribute name="href"><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>keyboard.css</xsl:attribute>
				</link>
<xsl:text>
</xsl:text><!-- pretty print -->
			</xsl:if>


			<script type="text/javascript">
<![CDATA[function set_slidebook_load_state(state) {
try { _sb_settings.slidebook_load_state += state; }
catch(e) { setTimeout(function(){set_slidebook_load_state(state);}, 50); return; }
return;
}]]></script>
<xsl:text>
</xsl:text><!-- pretty print -->

			<!-- load all scripts AFTER the CSS -->
			<script type="text/javascript">
				<xsl:attribute name="src"><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>jquery-min.js</xsl:attribute>
			</script>
<xsl:text>
</xsl:text><!-- pretty print -->

			<!-- load the virtual keyboard interface JS -->
			<xsl:if test="/indd_document/call_parameters/par[@name = 'addVirtualKeyboard'] and (/indd_document/call_parameters/par[@name = 'addVirtualKeyboard']/. != '0')">
				<script type="text/javascript">
					<xsl:attribute name="src"><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>keyboard.js</xsl:attribute>
					<xsl:attribute name="charset">UTF-8</xsl:attribute>
				</script>
<xsl:text>
</xsl:text><!-- pretty print -->
			</xsl:if>

			<!-- load all application scripts AFTER the CSS -->
			<xsl:comment><![CDATA[[if IE]><script type="text/javascript" src="]]><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>excanvas.js<![CDATA["></script><![endif]]]></xsl:comment>
<xsl:text>
</xsl:text><!-- pretty print -->
			<script type="text/javascript">
				<xsl:attribute name="src"><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>custom.js</xsl:attribute>
			</script>
<xsl:text>
</xsl:text><!-- pretty print -->
			<script type="text/javascript">
				<xsl:attribute name="src"><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>searchlocal.js</xsl:attribute>
			</script>
<xsl:text>
</xsl:text><!-- pretty print -->
			<script type="text/javascript">
				<xsl:attribute name="src"><xsl:value-of select="concat(doctypeinfos/@xslbasepath,$xslcssSubPath)"/>flipbook.js</xsl:attribute>
			</script>
<xsl:text>
</xsl:text><!-- pretty print -->

		</head>
<xsl:text>
</xsl:text><!-- pretty print -->
		<xsl:call-template name="show_creator">
			<xsl:with-param name="creationDate"><xsl:value-of select="header/headerfield[@name='creationDate']/." /></xsl:with-param>
			<xsl:with-param name="outputVersion"><xsl:value-of select="header/headerfield[@name='outputVersion']/." /></xsl:with-param>
			<xsl:with-param name="inputPath"><xsl:value-of select="header/headerfield[@name='inputPath']/." /></xsl:with-param>
			<xsl:with-param name="indesignDocname"><xsl:value-of select="header/headerfield[@name='indesignDocname']/." /></xsl:with-param>
			<xsl:with-param name="sourceINXfileName"><xsl:value-of select="header/headerfield[@name='sourceINXfileName']/." /></xsl:with-param>
			<xsl:with-param name="transformEngine"><xsl:value-of select="header/headerfield[@name='transformEngine']/." /></xsl:with-param>
			<xsl:with-param name="uplid"><xsl:value-of select="call_parameters/par[@name = 'UPLID']/." /></xsl:with-param>
		</xsl:call-template>
<xsl:text>
</xsl:text><!-- pretty print -->
		<body id="sb_body" class="sb_body" onload="set_slidebook_load_state('2');">
<xsl:text>
</xsl:text><!-- pretty print -->
			<xsl:comment> **** GENERAL INFO:  **** </xsl:comment>
<xsl:text>
</xsl:text><!-- pretty print -->
			<div style="position:absolute; visibility:hidden; top:-1000px; left:-1000px; width:0px; height:0px; overflow:hidden">
				<xsl:attribute name="id">doctypeinfos</xsl:attribute>
				<!-- xsl:attribute name="enc"><xsl:value-of select="/indd_document/doctypeinfos/@encoding" /></xsl:attribute -->
				<xsl:value-of disable-output-escaping="yes" select="/indd_document/doctypeinfos/@csspath" /></div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<div style="position:absolute; visibility:hidden; top:-1000px; left:-1000px; width:0px; height:0px; overflow:hidden">
				<xsl:attribute name="id">magnifyingglass</xsl:attribute>
				<xsl:value-of select="/indd_document/call_parameters/par[@name = 'magnifyingGlass']/." /></div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<div style="position:absolute; visibility:hidden; top:-1000px; left:-1000px; width:0px; height:0px; overflow:hidden">
				<xsl:attribute name="id">websiteParams</xsl:attribute>
					<xsl:choose>
						<!-- first priority: document info overridden by export dialog settings: logoURL=xxx*#*logoTitle.....-->
						<xsl:when test="/indd_document/call_parameters/par[@name = 'websiteParams']/. != ''">
							<xsl:value-of select="/indd_document/call_parameters/par[@name = 'websiteParams']/."/>
						</xsl:when>
						<!-- set in document info in description field like "My Document*#*logoURL=xxx*#*logoTitle... -->
						<xsl:when test="contains(header/headerfield[@name='description']/.,'*#*')">
							<xsl:value-of select="substring-after(header/headerfield[@name='description']/.,'*#*')"/>
						</xsl:when>
					</xsl:choose>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<div style="position:absolute; visibility:hidden; top:-1000px; left:-1000px; width:0px; height:0px; overflow:hidden">
				<xsl:attribute name="id">moreDocumentInfos</xsl:attribute>
					<xsl:if test="/indd_document/call_parameters/par[@name = 'pageTurnMode']">pageTurnMode=<xsl:value-of select="/indd_document/call_parameters/par[@name = 'pageTurnMode']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="/indd_document/call_parameters/par[@name = 'fontsizeUnits']">fontsizeUnits=<xsl:value-of select="/indd_document/call_parameters/par[@name = 'fontsizeUnits']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="/indd_document/call_parameters/par[@name = 'fontsizeBase']">fontsizeBase=<xsl:value-of select="/indd_document/call_parameters/par[@name = 'fontsizeBase']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:choose>
						<xsl:when test="($nopdf != '0') or ($nodocpdf != '0')"></xsl:when><!-- suppress document PDF -->
						<xsl:when test="/indd_document/call_parameters/par[@name = 'documentPDFname']/. != ''">documentPDFname=<xsl:value-of select="$pdfurl"/><xsl:value-of select="/indd_document/call_parameters/par[@name = 'documentPDFname']/."/><xsl:text>*#*</xsl:text></xsl:when>
						<xsl:when test="(/indd_document/call_parameters/par[@name = 'documentPDFnameSearch']) and (/indd_document/documentPDF)">documentPDFname=<xsl:value-of select="$pdfurl"/><xsl:value-of select="/indd_document/documentPDF/."/><xsl:text>*#*</xsl:text></xsl:when><!-- in batch mode call_parameters/par[@name = 'documentPDFname'] is not set. We have to get from element documentPDF -->
					</xsl:choose>
					<xsl:if test="/indd_document/call_parameters/par[@name = 'magnifyingGlass']">magnifyingGlass=<xsl:value-of select="/indd_document/call_parameters/par[@name = 'magnifyingGlass']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="/indd_document/call_parameters/par[@name = 'XSLCSSPath']">XSLCSSPath=<xsl:value-of select="/indd_document/call_parameters/par[@name = 'XSLCSSPath']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="/indd_document/page/pageJPEG/@sizefactor">
						<xsl:if test="($pagejpgsize != '') or ($pagejpguse != '')">
							<xsl:text>pageImageUseSize=</xsl:text>
								<xsl:choose>
									<xsl:when test="$pagejpguse != ''">
										<xsl:value-of select="$pagejpguse"/>
									</xsl:when>
									<xsl:when test="($pagejpgsize = '2') and /indd_document/page/pageJPEG2/@sizefactor">1</xsl:when>
									<xsl:when test="($pagejpgsize = '3') and /indd_document/page/pageJPEG3/@sizefactor">2</xsl:when>
									<xsl:when test="($pagejpgsize = '4') and /indd_document/page/pageJPEG4/@sizefactor">3</xsl:when>
									<xsl:otherwise><!-- $pagejpgsize is '' or '1' -->
										<xsl:text>0</xsl:text>
									</xsl:otherwise>
								</xsl:choose>
							<xsl:text>*#*</xsl:text>
						</xsl:if>

						<xsl:text>pageJPEGsizefactor=</xsl:text>
								<xsl:value-of select="/indd_document/page/pageJPEG/@sizefactor/."/>
								<xsl:if test="/indd_document/page/pageJPEG2/@sizefactor"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG2/@sizefactor/."/></xsl:if>
								<xsl:if test="/indd_document/page/pageJPEG3/@sizefactor"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG3/@sizefactor/."/></xsl:if>
								<xsl:if test="/indd_document/page/pageJPEG4/@sizefactor"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG4/@sizefactor/."/></xsl:if>
						<!--
						<xsl:choose>
							<xsl:when test="($pagejpgsize = '2') and /indd_document/page/pageJPEG2/@sizefactor">
								<xsl:value-of select="/indd_document/page/pageJPEG2/@sizefactor/."/>
							</xsl:when>
							<xsl:when test="($pagejpgsize = '3') and /indd_document/page/pageJPEG3/@sizefactor">
								<xsl:value-of select="/indd_document/page/pageJPEG3/@sizefactor/."/>
							</xsl:when>
							<xsl:when test="($pagejpgsize = '4') and /indd_document/page/pageJPEG4/@sizefactor">
								<xsl:value-of select="/indd_document/page/pageJPEG4/@sizefactor/."/>
							</xsl:when>
							<xsl:otherwise>--><!-- $pagejpgsize is '' or '1' --><!--
								<xsl:value-of select="/indd_document/page/pageJPEG/@sizefactor/."/>
								<xsl:if test="$pagejpgsize != '1'">
									<xsl:if test="/indd_document/page/pageJPEG2/@sizefactor"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG2/@sizefactor/."/></xsl:if>
									<xsl:if test="/indd_document/page/pageJPEG3/@sizefactor"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG3/@sizefactor/."/></xsl:if>
									<xsl:if test="/indd_document/page/pageJPEG4/@sizefactor"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG4/@sizefactor/."/></xsl:if>
								</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
						-->
						<xsl:text>*#*</xsl:text>
					</xsl:if>
					<xsl:if test="/indd_document/page/pageJPEG/@w">
						<xsl:text>pageJPEGwidth=</xsl:text>
								<xsl:value-of select="/indd_document/page/pageJPEG/@w/."/>
								<xsl:if test="/indd_document/page/pageJPEG2/@w"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG2/@w/."/></xsl:if>
								<xsl:if test="/indd_document/page/pageJPEG3/@w"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG3/@w/."/></xsl:if>
								<xsl:if test="/indd_document/page/pageJPEG4/@w"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG4/@w/."/></xsl:if>
						<!--
						<xsl:choose>
							<xsl:when test="($pagejpgsize = '2') and /indd_document/page/pageJPEG2/@w">
								<xsl:value-of select="/indd_document/page/pageJPEG2/@w/."/>
							</xsl:when>
							<xsl:when test="($pagejpgsize = '3') and /indd_document/page/pageJPEG3/@w">
								<xsl:value-of select="/indd_document/page/pageJPEG3/@w/."/>
							</xsl:when>
							<xsl:when test="($pagejpgsize = '4') and /indd_document/page/pageJPEG4/@w">
								<xsl:value-of select="/indd_document/page/pageJPEG4/@w/."/>
							</xsl:when>
							<xsl:otherwise>--><!-- $pagejpgsize is '' or '1' --><!--
								<xsl:value-of select="/indd_document/page/pageJPEG/@w/."/>
								<xsl:if test="$pagejpgsize != '1'">
									<xsl:if test="/indd_document/page/pageJPEG2/@w"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG2/@w/."/></xsl:if>
									<xsl:if test="/indd_document/page/pageJPEG3/@w"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG3/@w/."/></xsl:if>
									<xsl:if test="/indd_document/page/pageJPEG4/@w"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG4/@w/."/></xsl:if>
								</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
						-->
						<xsl:text>*#*</xsl:text>
					</xsl:if>
					<xsl:if test="/indd_document/page/pageJPEG/@h">
						<xsl:text>pageJPEGheight=</xsl:text>
								<xsl:value-of select="/indd_document/page/pageJPEG/@h/."/>
								<xsl:if test="/indd_document/page/pageJPEG2/@h"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG2/@h/."/></xsl:if>
								<xsl:if test="/indd_document/page/pageJPEG3/@h"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG3/@h/."/></xsl:if>
								<xsl:if test="/indd_document/page/pageJPEG4/@h"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG4/@h/."/></xsl:if>
						<!--
						<xsl:choose>
							<xsl:when test="($pagejpgsize = '2') and /indd_document/page/pageJPEG2/@h">
								<xsl:value-of select="/indd_document/page/pageJPEG2/@h/."/>
							</xsl:when>
							<xsl:when test="($pagejpgsize = '3') and /indd_document/page/pageJPEG3/@h">
								<xsl:value-of select="/indd_document/page/pageJPEG3/@h/."/>
							</xsl:when>
							<xsl:when test="($pagejpgsize = '4') and /indd_document/page/pageJPEG4/@h">
								<xsl:value-of select="/indd_document/page/pageJPEG4/@h/."/>
							</xsl:when>
							<xsl:otherwise>--><!-- $pagejpgsize is '' or '1' --><!--
								<xsl:value-of select="/indd_document/page/pageJPEG/@h/."/>
								<xsl:if test="$pagejpgsize != '1'">
									<xsl:if test="/indd_document/page/pageJPEG2/@h"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG2/@h/."/></xsl:if>
									<xsl:if test="/indd_document/page/pageJPEG3/@h"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG3/@h/."/></xsl:if>
									<xsl:if test="/indd_document/page/pageJPEG4/@h"><xsl:text>,</xsl:text><xsl:value-of select="/indd_document/page/pageJPEG4/@h/."/></xsl:if>
								</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
						-->
						<xsl:text>*#*</xsl:text>
					</xsl:if>
					<xsl:if test="header/headerfield[@name='filename']/.">XMLfilename=<xsl:value-of disable-output-escaping="yes" select="header/headerfield[@name='filename']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="header/headerfield[@name='outputVersion']/.">outputVersion=<xsl:value-of select="header/headerfield[@name='outputVersion']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="/indd_document/call_parameters/par[@name='characterAttribsSuppress']/.">characterAttribsSuppress=<xsl:value-of select="/indd_document/call_parameters/par[@name='characterAttribsSuppress']/."/><xsl:text>*#*</xsl:text></xsl:if>

					<xsl:if test="header/headerfield[@name='pub_date']/.">pub_date=<xsl:value-of select="header/headerfield[@name='pub_date']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="header/headerfield[@name='pub_edition']/.">pub_edition=<xsl:value-of select="header/headerfield[@name='pub_edition']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="header/headerfield[@name='pub_longEdition']/.">pub_longEdition=<xsl:value-of select="header/headerfield[@name='pub_longEdition']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="header/headerfield[@name='pub_object']/.">pub_object=<xsl:value-of select="header/headerfield[@name='pub_object']/."/><xsl:text>*#*</xsl:text></xsl:if>
					<xsl:if test="header/headerfield[@name='pub_longObject']/.">pub_longObject=<xsl:value-of select="header/headerfield[@name='pub_longObject']/."/><xsl:text>*#*</xsl:text></xsl:if>

					<xsl:choose><!-- TOC available? -->
						<xsl:when test="/indd_document/descendant::article[descendant::content[contains(@label,'*TOC*')]]"><!-- find a *TOC* label explicitely marked as TOC from manually created index -->
							<xsl:text>TOCpageSequence=</xsl:text><xsl:value-of select="/indd_document/descendant::article[descendant::content[contains(@label,'*TOC*')]]/@page_sequence/."/><xsl:text>*#*</xsl:text>
							<xsl:text>TOCpageName=</xsl:text><xsl:value-of select="/indd_document/descendant::article[descendant::content[contains(@label,'*TOC*')]]/@page_name/."/><xsl:text>*#*</xsl:text>
							<xsl:text>TOCaid=</xsl:text><xsl:value-of select="/indd_document/descendant::article[descendant::content[contains(@label,'*TOC*')]]/@idx/."/><xsl:text>*#*</xsl:text>
						</xsl:when>
						<xsl:when test="/indd_document/descendant::article[descendant::content/@toc]"><!-- find a TOC: elem 'content' has @toc' attribute from automatic indd index -->
							<xsl:text>TOCpageSequence=</xsl:text><xsl:value-of select="/indd_document/descendant::article[descendant::content/@toc]/@page_sequence/."/><xsl:text>*#*</xsl:text>
							<xsl:text>TOCpageName=</xsl:text><xsl:value-of select="/indd_document/descendant::article[descendant::content/@toc]/@page_name/."/><xsl:text>*#*</xsl:text>
							<xsl:text>TOCaid=</xsl:text><xsl:value-of select="/indd_document/descendant::article[descendant::content/@toc]/@idx/."/><xsl:text>*#*</xsl:text>
						</xsl:when>
					</xsl:choose>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<div id="sb_temp_data" style="position:absolute; visibility:hidden; top:-1000px; left:-1000px; width:0px; height:0px; overflow:hidden"></div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<xsl:comment> **** GENERAL INFO: END **** </xsl:comment>
<xsl:text>
</xsl:text><!-- pretty print -->
			<noscript>
<xsl:text>
</xsl:text><!-- pretty print -->
			<div style="position:absolute; top:100px; width:350px; padding:20px; border:5px solid yellow; background-color:black; color:red; font-size:12pt">
				To read this site you must enable JavaScript in your Browser!<br/><br/>
				Um diese Seite anzuzeigen müssen Sie JavaScript in Ihrem Browser aktivieren!<br/><br/>
				Pour visualiser ce site activez JavaScript dans vôtre navigateur!
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->
			</noscript>
<xsl:text>
</xsl:text><!-- pretty print -->
	
			<xsl:call-template name="write_paper_container"/>
			<xsl:call-template name="write_pages_script"/>
			<xsl:call-template name="write_canvas_tags"/>
<xsl:text>
</xsl:text><!-- pretty print -->

<xsl:text>
</xsl:text><!-- pretty print -->
			<div id="status_message"></div>
<xsl:text>
</xsl:text><!-- pretty print -->

			<xsl:if test="$noarticles = '0'">
				<xsl:call-template name="output_articles"/><!-- output the articles -->
			</xsl:if>
<xsl:text>
</xsl:text><!-- pretty print -->

			<div id="debugmessage">
				<xsl:if test="/indd_document/@demo_mode = 1">
					<xsl:attribute name="style">color:red;</xsl:attribute>
					<xsl:text>****** DEMO MODE!! Contains scrambled text content!</xsl:text>
				</xsl:if>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<!-- add Javascript for shortened version -->
			<xsl:if test="$maxpages != '0'">
<script>
try{setTimeout(function(){shortenedBookMessage("<xsl:value-of select="$maxpages"/>","<xsl:value-of select="count(/indd_document/page)"/>","<xsl:value-of select="/indd_document/header/headerfield[@name='pub_companyName']/."/>","<xsl:value-of select="/indd_document/header/headerfield[@name='pub_objectShortcut']/."/>","<xsl:value-of select="/indd_document/header/headerfield[@name='title']/."/>","<xsl:value-of select="/indd_document/header/headerfield[@name='pub_issueDate']/."/>","<xsl:value-of select="/indd_document/header/headerfield[@name='filename']/."/>");},4000)}catch(ex){}
</script>
			</xsl:if>
		</body>
<xsl:text>
</xsl:text><!-- pretty print -->
		</html>
<xsl:text>
</xsl:text><!-- pretty print -->
		<xsl:comment> Created with "BatchXSLT for InDesign". Transformer Engine "<xsl:value-of select="header/headerfield[@name='transformEngine']/."/> * <xsl:value-of select="call_parameters/par[@name = 'UPLID']/." />" by www.AiEDV.ch </xsl:comment>
<xsl:text>
</xsl:text><!-- pretty print -->
	</xsl:template>




	<xsl:template name="write_paper_container">
		<!-- the header -->
		<xsl:if test="($suppress_top_navbar != '1') and not(contains(/indd_document/call_parameters/par[@name='suppressSiteElements']/.,'1'))">
			<!-- the page header logo -->
			<div id="scrollview-header-logocontainer">
				<div id="scrollview-header-logo"></div>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->

			<!-- the page header title -->
			<div id="scrollview-header-titlecontainer">
<xsl:text>
</xsl:text><!-- pretty print -->
				<div id="scrollview-header-title">
					<xsl:value-of disable-output-escaping="yes" select="/indd_document/header/headerfield[@name='title']/text()"/>
					<xsl:if test="(/indd_document/header/headerfield[@name='WebStatement']/. != '') or (/indd_document/header/headerfield[@name='creator']/. != '')">
						<div class="subtitle_div">
							<xsl:variable name="fullLinkURL"><!-- check if full link is given -->
								<xsl:choose>
									<xsl:when test="contains(/indd_document/header/headerfield[@name='WebStatement']/.,'&lt;')"><xsl:value-of select="/indd_document/header/headerfield[@name='WebStatement']/."/></xsl:when>
								</xsl:choose>
							</xsl:variable>
							<xsl:if test="/indd_document/header/headerfield[@name='creator']/. != ''"><xsl:value-of disable-output-escaping="yes" select="translate(/indd_document/header/headerfield[@name='creator']/.,';',' ')"/></xsl:if>
							<xsl:choose>
								<xsl:when test="$fullLinkURL != ''">
									<xsl:value-of disable-output-escaping="yes" select="$fullLinkURL"/>
								</xsl:when>
								<xsl:when test="/indd_document/header/headerfield[@name='WebStatement']/. != ''">
									<a class="subtitle_link" target="_blank">
									<xsl:attribute name="href">
										<xsl:choose>
											<xsl:when test="starts-with(/indd_document/header/headerfield[@name='WebStatement']/.,'http:')">
												<xsl:value-of select="/indd_document/header/headerfield[@name='WebStatement']/."/>
											</xsl:when>
											<xsl:otherwise>http://<xsl:value-of select="/indd_document/header/headerfield[@name='WebStatement']/."/></xsl:otherwise>
										</xsl:choose>
									</xsl:attribute>
									<xsl:value-of select="/indd_document/header/headerfield[@name='WebStatement']/."/></a>
								</xsl:when>
							</xsl:choose>
						</div>
					</xsl:if>
				</div>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->

			<div id="TOC_button_container" class="TOC_button_container"></div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<div id="features_container" class="features_container"></div>
<xsl:text>
</xsl:text><!-- pretty print -->
		</xsl:if>

		<!-- the page slider -->
		<div id="sb_container" class="sb_container">
<xsl:text>
</xsl:text><!-- pretty print -->
			<div id="scrollview-header"></div>
<xsl:text>
</xsl:text><!-- pretty print -->

			<div id="scrollview-container">
<xsl:text>
</xsl:text><!-- pretty print -->
				<div id="scrollview-content">
<xsl:text>
</xsl:text><!-- pretty print -->
					<div id="sb_pagelist" class="sb_pagelist">
						<!-- get the pages into li elements-->
						<xsl:call-template name="write_pages_list"/>
					</div>
<xsl:text>
</xsl:text><!-- pretty print -->
					<!-- get the image maps -->
					<xsl:if test="$noarticles = '0'">
						<xsl:for-each select="/indd_document/page[(number($maxpages) &lt;= 0) or (number(@page_sequence) &lt;= number($maxpages))]">
							<xsl:if test="articles/article">
								<map>
									<xsl:attribute name="name">Mmap_P<xsl:value-of select="@page_sequence"/></xsl:attribute>
									<xsl:attribute name="id">Mmap_P<xsl:value-of select="@page_sequence"/></xsl:attribute>
									<xsl:call-template name="get_articles_areas_indd"><!-- get articles to this physical page -->
										<xsl:with-param name="page"><xsl:value-of select="@page_sequence"/></xsl:with-param>
										<xsl:with-param name="maxwidth"><xsl:value-of select="pageJPEG/@w"/></xsl:with-param>
										<xsl:with-param name="maxheight"><xsl:value-of select="pageJPEG/@h"/></xsl:with-param>
										<xsl:with-param name="scale"><xsl:value-of select="pageJPEG/@scale"/></xsl:with-param>
									</xsl:call-template>
								</map>
<xsl:text>
</xsl:text><!-- pretty print -->
							</xsl:if>
						</xsl:for-each>
					</xsl:if>
				</div>
<xsl:text>
</xsl:text><!-- pretty print -->
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->

			<div id="scrollview-trailer">
				<div id="sb_pager">
					<button type="button" class="sb_pagearrow" id="sb_pagearrow_first"><img src="{concat(doctypeinfos/@xslbasepath,$xslcssSubPath)}arr-left2.png" alt="&amp;lt;"/></button>
					<button type="button" class="sb_pagearrow" id="sb_pagearrow_prev"><img src="{concat(doctypeinfos/@xslbasepath,$xslcssSubPath)}arr-left.png" alt="&amp;lt;"/></button>
					<button type="button" class="sb_pagearrow" id="sb_pagearrow_next"><img src="{concat(doctypeinfos/@xslbasepath,$xslcssSubPath)}arr-right.png" alt="&amp;gt;"/></button>
					<button type="button" class="sb_pagearrow" id="sb_pagearrow_last"><img src="{concat(doctypeinfos/@xslbasepath,$xslcssSubPath)}arr-right2.png" alt="&amp;gt;"/></button>
					<div class="sb_page_entry_cont" id="sb_page_entry_cont">
						<input class="sb_page_entry_field" id="sb_page_entry_field" type="text" title="" value="" onkeypress="handleGotoPageFieldKeyPress(event)"/>
					</div>
				</div>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<div id="scrollview-bottom">
				<xsl:if test="$nopdf = '0'">
					<xsl:if test="$nodocpdf = '0'">
						<div id="documentPDF_button_container"><div id="documentPDF_button"><img id="documentPDF_button_img" alt="PDF" src="{concat(doctypeinfos/@xslbasepath,$xslcssSubPath)}documentPDF_button.png"/></div></div>
					</xsl:if>
					<xsl:if test="$nopagepdf = '0'">
						<div id="pagePDF_button_container"><div id="pagePDF_button"><img id="pagePDF_button_img" alt="PDF" src="{concat(doctypeinfos/@xslbasepath,$xslcssSubPath)}pagePDF_button.png"/></div></div>
					</xsl:if>
				</xsl:if>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->
		</div>
<xsl:text>
</xsl:text><!-- pretty print -->

		<!-- page thunbs -->
		<div id="sb_pagethumbs-container" class="sb_pagethumbs-container">
			<div id="sb_pagethumbs-button" class="sb_pagethumbs-button"></div>
		
			<div id="sb_pagethumbs-scrollcontainer" class="sb_pagethumbs-scrollcontainer">
				<div id="sb_pagethumbs-scrollcontent" class="sb_pagethumbs-scrollcontent">
					<!-- page thumbs list ist generated dynamically -->
					<ul id="sb_pagethumbs-scrolllist" class="sb_pagethumbs-scrolllist"></ul>
				</div>
			</div>
		</div>
<xsl:text>
</xsl:text><!-- pretty print -->

		<!-- the light box to show text, images and more -->
		<div class="hidden" id="lightbox_overlay"></div>
<xsl:text>
</xsl:text><!-- pretty print -->
		<div class="hidden" id="lightbox" style="position:absolute; left:0px; top:0px;">
			<div class="hidden" id="lightbox_close"></div>
			<div class="hidden" id="lightbox_returnsearch"></div>
			<div id="lightbox_content">
				 <div class="hidden" id="lightbox_div"></div>
			</div>
		</div>
<xsl:text>
</xsl:text><!-- pretty print -->

		<!-- show page loading status -->
		<xsl:if test="($suppress_loadstatus != '1') and not(contains(/indd_document/call_parameters/par[@name='suppressSiteElements']/.,'4'))">
			<div id="loadstatus_div"></div>
<xsl:text>
</xsl:text><!-- pretty print -->
		</xsl:if>

		<div style="position: absolute; left: 0px; top: 0px;" id="debug_console"></div>
<xsl:text>
</xsl:text><!-- pretty print -->

	</xsl:template>



	<xsl:template match="pageJPEG | pageJPEG2 | pageJPEG3 | pageJPEG4">
		<xsl:param name="jpegelement"/>
		<xsl:param name="pagePDF"/>
		<xsl:if test="name() = $jpegelement">
			<div class="sb_pagelistli">
				<img class="sb_pagelistimg">
					<xsl:attribute name="id">pv_P<xsl:value-of select="@page_sequence"/></xsl:attribute>
					<xsl:attribute name="style">width:<xsl:value-of select="@w"/>px;height:<xsl:value-of select="@h"/>px;</xsl:attribute>
					<!-- WILL DO DEFFERED LOADING<xsl:attribute name="src"><xsl:value-of select="."/></xsl:attribute> -->
					<xsl:attribute name="src"><xsl:value-of select="concat(/indd_document/call_parameters/par[@name = 'XSLCSSPath']/.,$xslcssSubPath,'d_p.gif')"/></xsl:attribute>
					<xsl:attribute name="alt"><xsl:value-of select="@page_name"/></xsl:attribute>
					<xsl:attribute name="title"><xsl:value-of select="@page_name"/></xsl:attribute>
					<xsl:attribute name="usemap">#Mmap_P<xsl:value-of select="@page_sequence"/></xsl:attribute>
					<xsl:attribute name="data-name"><xsl:value-of select="@name"/></xsl:attribute>
					<xsl:attribute name="data-original"><xsl:value-of select="@original"/></xsl:attribute>
					<xsl:attribute name="data-page-name"><xsl:value-of select="@page_name"/></xsl:attribute>
					<xsl:attribute name="data-page_sequence"><xsl:value-of select="@page_sequence"/></xsl:attribute>
					<xsl:attribute name="data-scale"><xsl:value-of select="@scale"/></xsl:attribute>
					<xsl:attribute name="data-sizefactor"><xsl:value-of select="@sizefactor"/></xsl:attribute>
					<xsl:attribute name="data-w"><xsl:value-of select="@w"/></xsl:attribute>
					<xsl:attribute name="data-h"><xsl:value-of select="@h"/></xsl:attribute>
					<xsl:attribute name="data-Self"><xsl:value-of select="../@Self"/></xsl:attribute>
					<xsl:attribute name="data-spread"><xsl:value-of select="../@spread"/></xsl:attribute>
					<xsl:attribute name="data-pageOnSpread"><xsl:value-of select="../@pageOnSpread"/></xsl:attribute>
					<xsl:attribute name="data-page_side"><xsl:value-of select="../@page_side"/></xsl:attribute>
					<xsl:if test="$pagePDF != ''"><xsl:attribute name="data-page-pdf"><xsl:value-of select="$pdfurl"/><xsl:value-of select="$pagePDF"/></xsl:attribute></xsl:if>
				</img>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->
		</xsl:if>
	</xsl:template>


	<xsl:template name="write_pages_list">
		<xsl:for-each select="/indd_document/page[(number($maxpages) &lt;= 0) or (number(@page_sequence) &lt;= number($maxpages))]">
			<xsl:variable name="pagePDF">
				<xsl:if test="($nopdf = '0') and ($nopagepdf = '0')">
					<xsl:value-of select="pagePDF/."/>
				</xsl:if>
			</xsl:variable>
			
			<xsl:variable name="jpegelement">
				<xsl:choose>
					<xsl:when test="($pagejpgsize = '2') and pageJPEG2">pageJPEG2</xsl:when>
					<xsl:when test="($pagejpgsize = '3') and pageJPEG3">pageJPEG3</xsl:when>
					<xsl:when test="($pagejpgsize = '4') and pageJPEG4">pageJPEG4</xsl:when>
					<xsl:otherwise>pageJPEG</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>

			<xsl:apply-templates select="pageJPEG | pageJPEG2 | pageJPEG3 | pageJPEG4">
				<xsl:with-param name="jpegelement" select="$jpegelement"/>
				<xsl:with-param name="pagePDF" select="$pagePDF"/>
			</xsl:apply-templates>
		</xsl:for-each>
	</xsl:template>


	<xsl:template name="write_canvas_tags">
		<xsl:if test="($suppress_all_mouseovers != '1') and (not(contains($suppressExportMouseOvers,'1'))) and not(contains(/indd_document/call_parameters/par[@name='suppressExportMouseOvers']/.,'1'))">

			<!-- the canvas for the shaded mouse overs -->
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_0"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_1"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_2"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_3"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_4"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_5"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_6"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_7"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_8"></canvas>
			<canvas onMouseOut="floater(event,this,-1)" onMouseOver="floater(event,this,10)" style="position:absolute; top:0;left:0; z-index:0" height="0" width="0" id="areacanvas_9"></canvas>
<xsl:text>
</xsl:text><!-- pretty print -->
		</xsl:if>
	</xsl:template>

	<!-- ===============================================
		 the article's areas on the page JPEG for InDesign documents
	     =============================================== -->
	<xsl:template name="get_articles_areas_indd">
		<xsl:param name="page"/>
		<xsl:param name="maxwidth"/>
		<xsl:param name="maxheight"/>
		<xsl:param name="scale"/>
		
		<!-- first, get all areas from other pages: like chained boxes from other articles which were skipped -->
		<xsl:for-each select="/indd_document/descendant::page[@page_sequence != $page]/descendant::boxchain/box[@page_sequence = $page]">
					<xsl:variable name="hasWWWlink"><xsl:value-of select="descendant::span[@class='CwwwLink']/."/></xsl:variable>
					<!-- get areas of all chained boxes -->
					<xsl:variable name="thepage_sequence"><xsl:value-of select="@page_sequence/."/></xsl:variable>
					<xsl:variable name="boxchain_coords">
						<xsl:for-each select="../box">
							<xsl:choose>
								<xsl:when test="(($suppress_emptybox_mouseovers = '1') or contains($suppressExportMouseOvers,'8')) and (contains(@cont,'empty') or contains(@cont,'cins') or contains(@cont,'unas'))"></xsl:when>
								<xsl:when test="contains(@type,'text') and (($suppress_text_mouseovers = '1') or (contains($suppressExportMouseOvers,'2'))) "></xsl:when>
								<xsl:when test="contains(@type,'imag') and (($suppress_image_mouseovers = '1') or (contains($suppressExportMouseOvers,'4')))"></xsl:when>
								<xsl:otherwise><xsl:value-of select="@coords/."/><xsl:text>,</xsl:text><xsl:value-of select="@angle/."/><xsl:text>,</xsl:text><xsl:value-of select="@bbox/."/><xsl:text>,</xsl:text><xsl:value-of select="@page_sequence/."/><xsl:if test="position() != last()"><xsl:text>;</xsl:text></xsl:if></xsl:otherwise>
							</xsl:choose>
						</xsl:for-each>
					</xsl:variable>
					<xsl:variable name="idx"><xsl:value-of select="ancestor::article/@idx"/></xsl:variable>
					<xsl:variable name="ls"><xsl:value-of select="ancestor::article/@ls"/></xsl:variable>
					<xsl:variable name="ts"><xsl:value-of select="ancestor::article/@ts"/></xsl:variable>
					<xsl:variable name="rs"><xsl:value-of select="ancestor::article/@rs"/></xsl:variable>
					<xsl:variable name="bs"><xsl:value-of select="ancestor::article/@bs"/></xsl:variable>
					<xsl:variable name="textshortcut">
						<xsl:if test="$with_textshortcuts != '0'">
							<xsl:value-of select="ancestor::article/textshortcut/."/>
						</xsl:if>
					</xsl:variable>

					<xsl:variable name="chainposition"><xsl:value-of select="position()"/></xsl:variable>
					<xsl:variable name="boxchain_shape"><xsl:value-of select="@shape/."/></xsl:variable>
					<xsl:choose>
						<xsl:when test="(($suppress_emptybox_mouseovers = '1') or contains($suppressExportMouseOvers,'8')) and (contains(@cont,'empty') or contains(@cont,'cins') or contains(@cont,'unas'))"></xsl:when>
						<xsl:when test="contains(@type,'text') and (($suppress_text_mouseovers = '1') or (contains($suppressExportMouseOvers,'2'))) "></xsl:when>
						<xsl:when test="contains(@type,'imag') and (($suppress_image_mouseovers = '1') or (contains($suppressExportMouseOvers,'4')))"></xsl:when>
						<xsl:otherwise>
							<area>
								<!--xsl:attribute name="name">P<xsl:value-of select="$page"/>_A<xsl:value-of select="$idx"/>_<xsl:value-of select="$page"/></xsl:attribute--><!-- need name attr for Netscape -->
								<xsl:attribute name="id">c<xsl:value-of select="$chainposition"/>P<xsl:value-of select="$page"/>_A<xsl:value-of select="$idx"/>_<xsl:value-of select="$page"/></xsl:attribute>
								<xsl:attribute name="alt"><xsl:value-of select="$idx"/></xsl:attribute>
								<xsl:attribute name="onMouseOver">floater(event,this,1,<xsl:value-of select="ancestor::article/@page_sequence"/>,'','<xsl:value-of select="$textshortcut"/>','<xsl:value-of select="string($boxchain_coords)"/>')</xsl:attribute>
								<xsl:attribute name="onMouseOut">floater(event,this,0)</xsl:attribute>
								<xsl:attribute name="onClick">show_article(this,'<xsl:value-of select="$idx"/>','<xsl:value-of select="$page"/>','<xsl:value-of select="ancestor::article/@page_sequence"/>','<xsl:value-of select="$scale"/>','','<xsl:value-of select="/indd_document/header/headerfield[@name='filename']/."/>','<xsl:value-of select="$hasWWWlink"/>','');</xsl:attribute>
								<xsl:choose>
									<xsl:when test="$boxchain_shape != ''">
										<xsl:attribute name="shape">poly</xsl:attribute>
										<xsl:attribute name="coords"><xsl:value-of select="$boxchain_shape"/></xsl:attribute>
										<!-- also store original coords as the coords attribute will be scaled if book is scaled -->
										<xsl:attribute name="data-coords"><xsl:value-of select="$boxchain_shape"/></xsl:attribute>
									</xsl:when>
									<xsl:otherwise>
										<xsl:attribute name="shape">rect</xsl:attribute>
										<!-- xsl:attribute name="coords"><xsl:if test="$ls &lt; '0'">0</xsl:if><xsl:if test="$ls &gt;= '0'"><xsl:value-of select="$ls"/></xsl:if>,<xsl:if test="$ts &lt; '0'">0</xsl:if><xsl:if test="$ts &gt;= '0'"><xsl:value-of select="$ts"/></xsl:if>,<xsl:if test="$rs &gt; $maxwidth"><xsl:value-of select="$maxwidth"/></xsl:if><xsl:if test="$rs &lt;= $maxwidth"><xsl:value-of select="$rs"/></xsl:if>,<xsl:if test="$bs &gt; $maxheight"><xsl:value-of select="$maxheight"/></xsl:if><xsl:if test="$bs &lt;= $maxheight"><xsl:value-of select="$bs"/></xsl:if></xsl:attribute -->
										<xsl:attribute name="coords"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/></xsl:attribute>
										<!-- also store original coords as the coords attribute will be scaled if book is scaled -->
										<xsl:attribute name="data-coords"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/></xsl:attribute>
									</xsl:otherwise>
								</xsl:choose>
							</area>
						</xsl:otherwise>
					</xsl:choose>
		</xsl:for-each>

		<!-- ***** now get areas for the requested page only -->
		<xsl:for-each select="articles[@page_sequence = $page]/article">
			<xsl:sort select="(@bs - @ts) * (@rs - @ls)" data-type="number" order="ascending" /><!-- largest article area first -->
			<xsl:if test="(@page_sequence = $page) and (@bs &gt;= 0) and (@rs &gt;= 0) and (@ls &lt; $maxwidth)"><!-- ignore cutt off stuff -->
				<xsl:if test="content or button/buttonstate[@name != 'Rollover']//content"><!-- ignore empty articles like background boxes -->
					<!-- test if we have a 'wwwLink' class in this article. if yes: we assume to put this image into an active HTML link -->
					<xsl:variable name="hasWWWlink"><xsl:value-of select="descendant::span[@class='CwwwLink']/."/></xsl:variable>
					<!-- get areas of all chained boxes -->
					<xsl:variable name="thepage_sequence"><xsl:value-of select="@page_sequence/."/></xsl:variable>
					<xsl:variable name="boxchain_coords">
						<xsl:for-each select="boxchain/box">
							<xsl:choose>
								<xsl:when test="(($suppress_emptybox_mouseovers = '1') or contains($suppressExportMouseOvers,'8')) and (contains(@cont,'empty') or contains(@cont,'cins') or contains(@cont,'unas'))"></xsl:when>
								<xsl:when test="contains(@type,'text') and (($suppress_text_mouseovers = '1') or (contains($suppressExportMouseOvers,'2'))) "></xsl:when>
								<xsl:when test="contains(@type,'imag') and (($suppress_image_mouseovers = '1') or (contains($suppressExportMouseOvers,'4')))"></xsl:when>
								<xsl:otherwise><xsl:value-of select="@coords/."/><xsl:text>,</xsl:text><xsl:value-of select="@angle/."/><xsl:text>,</xsl:text><xsl:value-of select="@bbox/."/><xsl:text>,</xsl:text><xsl:value-of select="@page_sequence/."/><xsl:if test="position() != last()"><xsl:text>;</xsl:text></xsl:if></xsl:otherwise>
							</xsl:choose>
						</xsl:for-each>
					</xsl:variable>
					<xsl:variable name="idx"><xsl:value-of select="@idx"/></xsl:variable>
					<xsl:variable name="ls"><xsl:value-of select="@ls"/></xsl:variable>
					<xsl:variable name="ts"><xsl:value-of select="@ts"/></xsl:variable>
					<xsl:variable name="rs"><xsl:value-of select="@rs"/></xsl:variable>
					<xsl:variable name="bs"><xsl:value-of select="@bs"/></xsl:variable>
					<xsl:variable name="textshortcut">
						<xsl:if test="$with_textshortcuts != '0'">
							<xsl:value-of select="textshortcut/."/>
						</xsl:if>
					</xsl:variable>
					<xsl:for-each select="boxchain/box[@page_sequence = $page]"><!-- skip areas for other pages: like chained boxes from other articles -->
						<xsl:variable name="chainposition"><xsl:value-of select="position()"/></xsl:variable>
						<xsl:variable name="boxchain_shape"><xsl:value-of select="@shape/."/></xsl:variable>
						<xsl:choose>
							<xsl:when test="(($suppress_emptybox_mouseovers = '1') or contains($suppressExportMouseOvers,'8')) and (contains(@cont,'empty') or contains(@cont,'cins') or contains(@cont,'unas'))"></xsl:when>
							<xsl:when test="contains(@type,'text') and (($suppress_text_mouseovers = '1') or (contains($suppressExportMouseOvers,'2'))) "></xsl:when>
							<xsl:when test="contains(@type,'imag') and (($suppress_image_mouseovers = '1') or (contains($suppressExportMouseOvers,'4')))"></xsl:when>
							<xsl:otherwise>
								<area>
									<xsl:attribute name="id">c<xsl:value-of select="$chainposition"/>P<xsl:value-of select="$page"/>_A<xsl:value-of select="$idx"/>_<xsl:value-of select="$page"/></xsl:attribute>
									<xsl:attribute name="alt"><xsl:value-of select="$idx"/></xsl:attribute>
									<xsl:attribute name="onMouseOver">floater(event,this,1,<xsl:value-of select="$page"/>,'','<xsl:value-of select="$textshortcut"/>','<xsl:value-of select="string($boxchain_coords)"/>')</xsl:attribute>
									<xsl:attribute name="onMouseOut">floater(event,this,0)</xsl:attribute>
									<xsl:attribute name="onClick">show_article(this,'<xsl:value-of select="$idx"/>','<xsl:value-of select="$page"/>','<xsl:value-of select="$page"/>','<xsl:value-of select="$scale"/>','','<xsl:value-of select="/indd_document/header/headerfield[@name='filename']/."/>','<xsl:value-of select="$hasWWWlink"/>','');</xsl:attribute>
									<xsl:choose>
										<xsl:when test="$boxchain_shape != ''">
											<xsl:attribute name="shape">poly</xsl:attribute>
											<xsl:attribute name="coords"><xsl:value-of select="$boxchain_shape"/></xsl:attribute>
											<!-- also store original coords as the coords attribute will be scaled if book is scaled -->
											<xsl:attribute name="data-coords"><xsl:value-of select="$boxchain_shape"/></xsl:attribute>
										</xsl:when>
										<xsl:otherwise>
											<xsl:attribute name="shape">rect</xsl:attribute>
											<!-- xsl:attribute name="coords"><xsl:if test="$ls &lt; '0'">0</xsl:if><xsl:if test="$ls &gt;= '0'"><xsl:value-of select="$ls"/></xsl:if>,<xsl:if test="$ts &lt; '0'">0</xsl:if><xsl:if test="$ts &gt;= '0'"><xsl:value-of select="$ts"/></xsl:if>,<xsl:if test="$rs &gt; $maxwidth"><xsl:value-of select="$maxwidth"/></xsl:if><xsl:if test="$rs &lt;= $maxwidth"><xsl:value-of select="$rs"/></xsl:if>,<xsl:if test="$bs &gt; $maxheight"><xsl:value-of select="$maxheight"/></xsl:if><xsl:if test="$bs &lt;= $maxheight"><xsl:value-of select="$bs"/></xsl:if></xsl:attribute -->
											<xsl:attribute name="coords"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/></xsl:attribute>
											<!-- also store original coords as the coords attribute will be scaled if book is scaled -->
											<xsl:attribute name="data-coords"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/></xsl:attribute>
										</xsl:otherwise>
									</xsl:choose>
								</area>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
				</xsl:if>
			</xsl:if>
		</xsl:for-each>
	</xsl:template> 


	<xsl:template name="write_pages_script">
<xsl:text>
</xsl:text><!-- pretty print -->
<script type="text/javascript">
/* pages array:
epaper_pages[0]=new Array(8);	// this is the first page
	"	[0][0]="page_0.gif";	// URL to JPEG file
	"	[0][1]=0;				// currently showing in flip book: 0 = no, 1 = yes
	"	[0][2]="";				// layout ID
	"	[0][3]=0;				// sequence index number in flipbook
	"	[0][4]="";				// section page number from layout
	"	[0][5]=null;			// page image container obj: parent DIV
	"	[0][6]="";				// URL to original JPEG where epaper_pages[0][0] is derived from
	"	[0][7]="lfth";			// left or right side
*/
var num_epaper_pages=0,epaper_pages=new Array();
<xsl:variable name="firstpagegside">
	<xsl:choose>
		<xsl:when test="page/@page_side = 'usex'">rgth</xsl:when><!-- assume first page on right side. we will add a first blank page -->
		<xsl:otherwise>
			<xsl:value-of select="page/@page_side"/>
		</xsl:otherwise>
	</xsl:choose>
</xsl:variable>
<xsl:call-template name="getall_pages_script">
	<xsl:with-param name="firstpagegside"><xsl:value-of select="$firstpagegside"/></xsl:with-param>
	<xsl:with-param name="pageoffset">
		<xsl:choose>
			<xsl:when test="$firstpagegside != 'lfth'">0</xsl:when>
			<xsl:otherwise>1</xsl:otherwise>
		</xsl:choose>
	</xsl:with-param>
</xsl:call-template>
num_epaper_pages=epaper_pages.length;
for (var i=0;i&lt;num_epaper_pages;i++) {
	epaper_pages[i][3]=i;
	if(epaper_pages[i][4]=="-")epaper_pages[i][4]=""+i;
	if(i%2)epaper_pages[i][7]="rgth";
	else epaper_pages[i][7]="lfth";
}
function call_initFlipbook(){
	if (typeof $(window).initFlipbook=="undefined") {
		setTimeout("call_initFlipbook()",50);
		return;
	}
	$(window).initFlipbook();
}
call_initFlipbook();
</script>
<xsl:text>
</xsl:text><!-- pretty print -->
	</xsl:template>



	<xsl:template name="getall_pages_script">
		<xsl:param name="pageoffset"/>
		<xsl:param name="firstpagegside"/>
		<xsl:variable name="layoutID">
			<xsl:choose>
				<xsl:when test="contains(/indd_document/header/headerfield[@name='indesignDocname']/.,'.indd')"><xsl:value-of select="substring-before(/indd_document/header/headerfield[@name='indesignDocname']/.,'.indd')"/>
				</xsl:when>
				<xsl:otherwise><xsl:value-of select="/indd_document/header/headerfield[@name='indesignDocname']/."/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
<xsl:for-each select="page[(number($maxpages) &lt;= 0) or (number(@page_sequence) &lt;= number($maxpages))]">
	<xsl:variable name="current_page_side">
		<xsl:choose>
			<xsl:when test="@page_side = 'usex'"><!-- single page docs -->
				<xsl:choose>
					<xsl:when test="position() = 1"><xsl:value-of select="$firstpagegside"/></xsl:when>
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="(position() + number($pageoffset)) mod 2 = 1">rgth</xsl:when><!-- is a right page -->
							<xsl:otherwise>lfth</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise><xsl:value-of select="@page_side"/></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
epaper_pages[epaper_pages.length]=["<xsl:call-template name="getall_pageJPEGS"/>",0,"<xsl:value-of select="$layoutID"/>",<xsl:value-of select="@page_sequence"/>,"<xsl:value-of select="@page_name"/>","<xsl:value-of select="@page_sequence"/>","<xsl:value-of select="pageJPEG/@original"/>","<xsl:value-of select="$current_page_side"/>"];</xsl:for-each>
	</xsl:template>


	<xsl:template name="getall_pageJPEGS">
		<xsl:value-of select="pageJPEG/."/>
		<xsl:if test="pageJPEG2"><xsl:text>,</xsl:text><xsl:value-of select="pageJPEG2/."/></xsl:if>
		<xsl:if test="pageJPEG3"><xsl:text>,</xsl:text><xsl:value-of select="pageJPEG3/."/></xsl:if>
		<xsl:if test="pageJPEG4"><xsl:text>,</xsl:text><xsl:value-of select="pageJPEG4/."/></xsl:if>
	</xsl:template>



	<xsl:template name="list_pageJPEGS">
		<xsl:choose>
			<xsl:when test="($pagejpgsize = '2') and pageJPEG2">
				<xsl:value-of select="pageJPEG2/."/>
			</xsl:when>
			<xsl:when test="($pagejpgsize = '3') and pageJPEG3">
				<xsl:value-of select="pageJPEG3/."/>
			</xsl:when>
			<xsl:when test="($pagejpgsize = '4') and pageJPEG4">
				<xsl:value-of select="pageJPEG4/."/>
			</xsl:when>
			<xsl:otherwise><!-- $pagejpgsize is '' or '1' -->
				<xsl:value-of select="pageJPEG/."/>
				<xsl:if test="$pagejpgsize != '1'">
					<xsl:if test="pageJPEG2"><xsl:text>,</xsl:text><xsl:value-of select="pageJPEG2/."/></xsl:if>
					<xsl:if test="pageJPEG3"><xsl:text>,</xsl:text><xsl:value-of select="pageJPEG3/."/></xsl:if>
					<xsl:if test="pageJPEG4"><xsl:text>,</xsl:text><xsl:value-of select="pageJPEG4/."/></xsl:if>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>



	<xsl:template name="output_articles"><!-- output the articles -->
<xsl:text>
</xsl:text><!-- pretty print -->
		<xsl:comment> **** DOCUMENT ARTICLES **** </xsl:comment>
<xsl:text>
</xsl:text><!-- pretty print -->
		<xsl:for-each select="descendant::articles/article[(number($maxpages) &lt;= 0) or (number(@page_sequence) &lt;= number($maxpages))]">
			<xsl:comment> **** ARTICLE '<xsl:value-of select="@idx"/>' PAGE #<xsl:value-of select="@page_sequence"/>:  **** </xsl:comment>
<xsl:text>
</xsl:text><!-- pretty print -->

			<div style="position:absolute; visibility:hidden; top:-1000px; left:-1000px; width:0px; height:0px; overflow:hidden;">
				<xsl:attribute name="id">Art<xsl:value-of select="@idx"/>_<xsl:value-of select="@page_sequence"/></xsl:attribute>
				<!-- boxchain coords -->
				<xsl:variable name="boxchain_coords">
					<xsl:for-each select="boxchain/box">
						<xsl:choose>
							<xsl:when test="(($suppress_emptybox_mouseovers = '1') or contains($suppressExportMouseOvers,'8')) and (contains(@cont,'empty') or contains(@cont,'cins') or contains(@cont,'unas'))"></xsl:when>
							<xsl:when test="contains(@type,'text') and (($suppress_text_mouseovers = '1') or (contains($suppressExportMouseOvers,'2'))) "></xsl:when>
							<xsl:when test="contains(@type,'imag') and (($suppress_image_mouseovers = '1') or (contains($suppressExportMouseOvers,'4')))"></xsl:when>
							<xsl:otherwise><xsl:value-of select="@coords/."/><xsl:text>,</xsl:text><xsl:value-of select="@angle/."/><xsl:text>,</xsl:text><xsl:value-of select="@bbox/."/><xsl:text>,</xsl:text><xsl:value-of select="@page_sequence/."/><xsl:if test="position() != last()"><xsl:text>;</xsl:text></xsl:if></xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
				</xsl:variable>
				<xsl:if test="$boxchain_coords != ''">
					<xsl:attribute name="data-coords"><xsl:value-of select="$boxchain_coords"/></xsl:attribute>
				</xsl:if>
				<xsl:if test="@section !=''"><xsl:attribute name="data-section"><xsl:value-of select="@section"/></xsl:attribute></xsl:if>
				<xsl:if test="@longSection !=''"><xsl:attribute name="data-longSection"><xsl:value-of select="@longSection"/></xsl:attribute></xsl:if>
<xsl:text>
</xsl:text><!-- pretty print -->

				<!-- article nav arrows top -->
				<xsl:if test="(@previousid and (@previousid != '')) or (@continuedid and (@continuedid != ''))">
					<div class="goto_article_nav_top">
						<xsl:if test="@previouspage and (@previouspage != '') and @previousid and (@previousid != '')">
							<div class="goto_previous_article"><a class="goto_previous_article"><xsl:attribute name="href">javascript:goto_continued_article("<xsl:value-of select="string(@previousid)"/>","<xsl:value-of select="string(@previouspage)"/>")</xsl:attribute><img class="goto_previous_article_img" src="{concat(/indd_document/doctypeinfos/@xslbasepath,$xslcssSubPath)}arr-left.png" alt="&amp;lt;"/></a></div>
						</xsl:if>
						<xsl:if test="@continuedpage and (@continuedpage != '') and @continuedid and (@continuedid != '')">
							<div class="goto_continued_article"><a class="goto_continued_article"><xsl:attribute name="href">javascript:goto_continued_article("<xsl:value-of select="string(@continuedid)"/>","<xsl:value-of select="string(@continuedpage)"/>")</xsl:attribute><img class="goto_continued_article_img" src="{concat(/indd_document/doctypeinfos/@xslbasepath,$xslcssSubPath)}arr-right.png" alt="&amp;gt;"/></a></div>
						</xsl:if>
					</div>
<xsl:text>
</xsl:text><!-- pretty print -->
				</xsl:if>

				<!-- copy elements and remove carriage returns -->
				<xsl:apply-templates select="content | button | multistate | comment() | text()">
					<xsl:with-param name="pg"><xsl:value-of select="@page_sequence"/></xsl:with-param>
					<xsl:with-param name="pn"><xsl:value-of select="@page_name"/></xsl:with-param>
					<xsl:with-param name="aid"><xsl:value-of select="@idx"/></xsl:with-param>
					<xsl:with-param name="story_id"><xsl:value-of select="@parent_storyid"/></xsl:with-param>
					<xsl:with-param name="p_f"><xsl:value-of select="@previousframe"/></xsl:with-param>
					<xsl:with-param name="n_f"><xsl:value-of select="@continuedframe"/></xsl:with-param>
					<xsl:with-param name="p_id"><xsl:value-of select="@previousid"/></xsl:with-param>
					<xsl:with-param name="n_id"><xsl:value-of select="@continuedid"/></xsl:with-param>
					<xsl:with-param name="section"><xsl:value-of select="@section"/></xsl:with-param>
					<xsl:with-param name="longSection"><xsl:value-of select="@longSection"/></xsl:with-param>
				</xsl:apply-templates>

				<!-- article nav arrows bottom -->
				<xsl:if test="(@previousid and (@previousid != '')) or (@continuedid and (@continuedid != ''))">
					<div class="goto_article_nav_bot">
						<xsl:if test="@previouspage and (@previouspage != '') and @previousid and (@previousid != '')">
							<div class="goto_previous_article"><a class="goto_previous_article"><xsl:attribute name="href">javascript:goto_continued_article("<xsl:value-of select="string(@previousid)"/>","<xsl:value-of select="string(@previouspage)"/>")</xsl:attribute><img class="goto_previous_article_img" src="{concat(/indd_document/doctypeinfos/@xslbasepath,$xslcssSubPath)}arr-left.png" alt="&amp;lt;"/></a></div>
						</xsl:if>
						<xsl:if test="@continuedpage and (@continuedpage != '') and @continuedid and (@continuedid != '')">
							<div class="goto_continued_article"><a class="goto_continued_article"><xsl:attribute name="href">javascript:goto_continued_article("<xsl:value-of select="string(@continuedid)"/>","<xsl:value-of select="string(@continuedpage)"/>")</xsl:attribute><img class="goto_continued_article_img" src="{concat(/indd_document/doctypeinfos/@xslbasepath,$xslcssSubPath)}arr-right.png" alt="&amp;gt;"/></a></div>
						</xsl:if>
					</div>
				</xsl:if>
			</div>
<xsl:text>
</xsl:text><!-- pretty print -->
			<xsl:comment> **** ARTICLE '<xsl:value-of select="@idx"/>' PAGE #<xsl:value-of select="@page_sequence"/>: END **** </xsl:comment>
<xsl:text>
</xsl:text><!-- pretty print -->
		</xsl:for-each>
<xsl:text>
</xsl:text><!-- pretty print -->
	</xsl:template>


	<!-- get content element. This actually is the InDesign Story Element -->
	<xsl:template match="content">
		<xsl:param name="pg"/>
		<xsl:param name="pn"/>
		<xsl:param name="aid"/>
		<xsl:param name="story_id"/>
		<xsl:param name="p_f"/>
		<xsl:param name="n_f"/>
		<xsl:param name="p_id"/>
		<xsl:param name="n_id"/>
		<xsl:param name="section"/>
		<xsl:param name="longSection"/>
		<xsl:param name="anchorid"/>
		<!-- check if we have to add an outer container with special attribs -->
		<xsl:if test="$debug_labels != '0'"><div style="color:red"><xsl:value-of select="@label"/></div></xsl:if>
		<xsl:variable name="boxtextflowid" select="@id"/>
		<xsl:variable name="outerBackgroundColor">
			<xsl:choose>
				<xsl:when test="(@backgroundColor != '') and (@backgroundColor != '#')">
					<xsl:value-of select="@backgroundColor/."/>
				</xsl:when>
				<xsl:when test="(../boxchain/box[@textflowid = $boxtextflowid]/@backgroundColor != '') and (../boxchain/box[@textflowid = $boxtextflowid]/@backgroundColor != '#')">
					<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@backgroundColor/."/>
				</xsl:when>
				<xsl:otherwise></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="addOuterInsets" select="((@insetTopR != '0') and (@insetTopR != '')) or ((@insetLeftR != '0') and (@insetLeftR != '')) or ((@insetBottomR != '0') and (@insetBottomR != '')) or ((@insetRightR != '0') and (@insetRightR != ''))
													or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopL != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopB != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR != '0')"/>
		<xsl:variable name="addOuterBorder" select="(@type != 'image') and ((@frameWeight != '0') and (@frameWeight != '') and (@frameColor != '')) or ((../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameWeight != '0') and (../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameWeight != ''))"/><!-- ignore like 'chained_text' boxes -->

		<xsl:choose>
			<xsl:when test="($suppress_all_mouseovers = '1') or (contains($suppressExportMouseOvers,'1'))"></xsl:when>
			<xsl:when test="contains(@type,'text') and (($suppress_text_mouseovers = '1') or (contains($suppressExportMouseOvers,'2'))) "></xsl:when>
			<xsl:when test="contains(@type,'image') and (($suppress_image_mouseovers = '1') or (contains($suppressExportMouseOvers,'4')))"></xsl:when>
			
			<xsl:when test="descendant::*"><!-- non empty content element -->
				<xsl:variable name="self" select="@Self"/>
				<xsl:choose>
					<xsl:when test="($anchorid != '') or (@anchor_id != '') or contains(../@type,'anchored')"><!-- parent box is anchored -->
						<span>
							<xsl:attribute name="class">
								<xsl:text>Artcl_container</xsl:text>
								<xsl:choose>
									<xsl:when test="../@objstyle != ''"><xsl:text> </xsl:text><xsl:if test="not(starts-with(../@objstyle,'O_'))"><xsl:text>O_</xsl:text></xsl:if><xsl:choose><xsl:when test="contains(../@objstyle,'/')"><xsl:value-of select="substring-after(../@objstyle,'/')"/></xsl:when><xsl:otherwise><xsl:value-of select="../@objstyle"/></xsl:otherwise></xsl:choose></xsl:when>
									<xsl:when test="../boxchain/box[@Self = $self]/@objstyle"><xsl:text> </xsl:text><xsl:if test="not(starts-with(../boxchain/box[@Self = $self]/@objstyle,'O_'))"><xsl:text>O_</xsl:text></xsl:if><xsl:choose><xsl:when test="contains(../boxchain/box[@Self = $self]/@objstyle,'/')"><xsl:value-of select="substring-after(../boxchain/box[@Self = $self]/@objstyle,'/')"/></xsl:when><xsl:otherwise><xsl:value-of select="../boxchain/box[@Self = $self]/@objstyle"/></xsl:otherwise></xsl:choose></xsl:when>
								</xsl:choose>
							</xsl:attribute>
							<xsl:if test="(($outerBackgroundColor != '') or ($addOuterInsets = 'true') or ($addOuterBorder = 'true'))">
								<xsl:attribute name="style">
									<xsl:if test="$outerBackgroundColor != ''">background-color:<xsl:value-of select="$outerBackgroundColor" />;</xsl:if>
									<xsl:if test="$addOuterInsets = 'true'">
										<xsl:choose>
											<xsl:when test="(@insetTopR != '0') or (@insetLeftR != '0') or (@insetBottomR != '0') or (@insetRightR != '0')"> padding-top:<xsl:value-of select="@insetTopR" />px; padding-left:<xsl:value-of select="@insetLeftR" />px; padding-bottom:<xsl:value-of select="@insetBottomR" />px; padding-right:<xsl:value-of select="@insetRightR" />px;</xsl:when>
											<xsl:when test="(../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopL != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopB != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR != '0')"> padding-top:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR" />px; padding-left:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@insetLeftR" />px; padding-bottom:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@insetBottomR" />px; padding-right:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@insetRightR" />px;</xsl:when>
											<xsl:otherwise></xsl:otherwise>
										</xsl:choose>
									</xsl:if>
									<xsl:if test="$addOuterBorder = 'true'">
										<xsl:choose>
											<xsl:when test="(@frameWeight != '0') and (@frameWeight != '') and (@frameColor != '')"> border:<xsl:value-of select="ceiling(@frameWeight)" />px <xsl:value-of select="@frameStyleHTML" /><xsl:text> </xsl:text><xsl:if test="(@frameColor != '') and (@frameColor != 'transparent') and (starts-with(@frameColor,'#') = false)">#</xsl:if><xsl:value-of select="@frameColor" />;</xsl:when>
											<xsl:when test="(../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameWeight != '0') and (../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameWeight != '')"> border:<xsl:value-of select="ceiling(../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameWeight)" />px <xsl:value-of select="../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameStyleHTML" /><xsl:text> </xsl:text><xsl:if test="(../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameColor != '') and (../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameColor != 'transparent') and (starts-with(../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameColor,'#') = 'false')">#</xsl:if><xsl:value-of select="../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameColor" />;</xsl:when>
											<xsl:otherwise></xsl:otherwise>
										</xsl:choose>
									</xsl:if>
								</xsl:attribute>
							</xsl:if>
							<xsl:attribute name="data-Self"><xsl:value-of select="@Self"/></xsl:attribute>
							<xsl:attribute name="data-pg"><xsl:value-of select="$pg"/></xsl:attribute>
							<xsl:attribute name="data-pn"><xsl:value-of select="$pn"/></xsl:attribute>
							<xsl:attribute name="data-aid"><xsl:value-of select="$aid"/></xsl:attribute>
							<xsl:choose>
								<xsl:when test="@anchor_id != ''"><xsl:attribute name="data-anchor_id"><xsl:value-of select="@anchor_id"/></xsl:attribute></xsl:when>
								<xsl:when test="$anchorid != ''"><xsl:attribute name="data-anchor_id"><xsl:value-of select="$anchorid"/></xsl:attribute></xsl:when>
								<xsl:otherwise><xsl:attribute name="data-anchor_id"></xsl:attribute></xsl:otherwise>
							</xsl:choose>

							<xsl:choose>
								<xsl:when test="../@objstyle != ''"><xsl:attribute name="data-boxobjstyle"><xsl:value-of select="../@objstyle"/></xsl:attribute></xsl:when><!-- ANCHORED OBJECT: the parent container box may have an interesting object style -->
								<xsl:when test="../boxchain/box[@Self = $self]/@objstyle"><xsl:attribute name="data-boxobjstyle"><xsl:value-of select="../boxchain/box[@Self = $self]/@objstyle"/></xsl:attribute></xsl:when><!-- NORMAL OBJECT - parent is <article: the container boxchain/box with same @Self may have an interesting object style -->
							</xsl:choose>

							<xsl:if test="$story_id != ''"><xsl:attribute name="data-story_id"><xsl:value-of select="$story_id"/></xsl:attribute></xsl:if>
							<xsl:if test="$p_f != ''"><xsl:attribute name="data-p_f"><xsl:value-of select="$p_f"/></xsl:attribute></xsl:if>
							<xsl:if test="$n_f != ''"><xsl:attribute name="data-n_f"><xsl:value-of select="$n_f"/></xsl:attribute></xsl:if>
							<xsl:if test="$p_id != ''"><xsl:attribute name="data-p_id"><xsl:value-of select="$p_id"/></xsl:attribute></xsl:if>
							<xsl:if test="$n_id != ''"><xsl:attribute name="data-n_id"><xsl:value-of select="$n_id"/></xsl:attribute></xsl:if>
							<xsl:if test="@label != ''"><xsl:attribute name="data-label"><xsl:value-of select="@label"/></xsl:attribute></xsl:if>
							<xsl:if test="@toc"><xsl:attribute name="data-toc"><xsl:value-of select="@toc"/></xsl:attribute></xsl:if>
							<xsl:if test="$section != ''"><xsl:attribute name="data-section"><xsl:value-of select="$section"/></xsl:attribute></xsl:if>
							<xsl:if test="$longSection != ''"><xsl:attribute name="data-longSection"><xsl:value-of select="$longSection"/></xsl:attribute></xsl:if>

							<xsl:if test="@backgroundColor != ''"><xsl:attribute name="data-backgroundColor"><xsl:value-of select="@backgroundColor"/></xsl:attribute></xsl:if>
							<xsl:if test="@angle != ''"><xsl:attribute name="data-angle"><xsl:value-of select="@angle"/></xsl:attribute></xsl:if>
							<xsl:if test="@scaleX != ''"><xsl:attribute name="data-scaleX"><xsl:value-of select="@scaleX"/></xsl:attribute></xsl:if>
							<xsl:if test="@scaleY != ''"><xsl:attribute name="data-scaleY"><xsl:value-of select="@scaleY"/></xsl:attribute></xsl:if>
							<xsl:if test="@x1 != ''"><xsl:attribute name="data-x1"><xsl:value-of select="@x1"/></xsl:attribute></xsl:if>
							<xsl:if test="@y1 != ''"><xsl:attribute name="data-y1"><xsl:value-of select="@y1"/></xsl:attribute></xsl:if>
							<xsl:if test="@x2 != ''"><xsl:attribute name="data-x2"><xsl:value-of select="@x2"/></xsl:attribute></xsl:if>
							<xsl:if test="@y2 != ''"><xsl:attribute name="data-y2"><xsl:value-of select="@y2"/></xsl:attribute></xsl:if>
							<xsl:apply-templates select="* | comment() | text()"><xsl:with-param name="isanchored"><xsl:value-of select="1"/></xsl:with-param></xsl:apply-templates>
						</span>
					</xsl:when>
					<xsl:otherwise>
						<div>
							<xsl:attribute name="class">
								<xsl:text>Artcl_container</xsl:text>
								<xsl:choose>
									<xsl:when test="../@objstyle != ''"><xsl:text> </xsl:text><xsl:if test="not(starts-with(../@objstyle,'O_'))"><xsl:text>O_</xsl:text></xsl:if><xsl:choose><xsl:when test="contains(../@objstyle,'/')"><xsl:value-of select="substring-after(../@objstyle,'/')"/></xsl:when><xsl:otherwise><xsl:value-of select="../@objstyle"/></xsl:otherwise></xsl:choose></xsl:when>
									<xsl:when test="../boxchain/box[@Self = $self]/@objstyle"><xsl:text> </xsl:text><xsl:if test="not(starts-with(../boxchain/box[@Self = $self]/@objstyle,'O_'))"><xsl:text>O_</xsl:text></xsl:if><xsl:choose><xsl:when test="contains(../boxchain/box[@Self = $self]/@objstyle,'/')"><xsl:value-of select="substring-after(../boxchain/box[@Self = $self]/@objstyle,'/')"/></xsl:when><xsl:otherwise><xsl:value-of select="../boxchain/box[@Self = $self]/@objstyle"/></xsl:otherwise></xsl:choose></xsl:when>
								</xsl:choose>
							</xsl:attribute>
							<xsl:if test="(($outerBackgroundColor != '') or ($addOuterInsets = 'true') or ($addOuterBorder = 'true'))">
								<xsl:attribute name="style">
									<xsl:if test="$outerBackgroundColor != ''">background-color:<xsl:value-of select="$outerBackgroundColor" />;</xsl:if>
									<xsl:if test="$addOuterInsets = 'true'">
										<xsl:choose>
											<xsl:when test="(@insetTopR != '0') or (@insetLeftR != '0') or (@insetBottomR != '0') or (@insetRightR != '0')"> padding-top:<xsl:value-of select="@insetTopR" />px; padding-left:<xsl:value-of select="@insetLeftR" />px; padding-bottom:<xsl:value-of select="@insetBottomR" />px; padding-right:<xsl:value-of select="@insetRightR" />px;</xsl:when>
											<xsl:when test="(../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopL != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopB != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR != '0')"> padding-top:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR" />px; padding-left:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@insetLeftR" />px; padding-bottom:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@insetBottomR" />px; padding-right:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@insetRightR" />px;</xsl:when>
											<xsl:otherwise></xsl:otherwise>
										</xsl:choose>
									</xsl:if>
									<xsl:if test="$addOuterBorder = true()">
										<xsl:choose>
											<xsl:when test="(@frameWeight != '0') and (@frameWeight != '') and (@frameColor != '')"> border:<xsl:value-of select="ceiling(@frameWeight)" />px <xsl:value-of select="@frameStyleHTML" /><xsl:text> </xsl:text><xsl:if test="(@frameColor != '') and (@frameColor != 'transparent') and (starts-with(@frameColor,'#') = false)">#</xsl:if><xsl:value-of select="@frameColor" />;</xsl:when>
											<xsl:when test="(../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameWeight != '0') and (../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameWeight != '')"> border:<xsl:value-of select="ceiling(../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameWeight)" />px <xsl:value-of select="../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameStyleHTML" /><xsl:text> </xsl:text><xsl:if test="(../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameColor != '') and (../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameColor != 'transparent') and (starts-with(../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameColor,'#') = 'false')">#</xsl:if><xsl:value-of select="../boxchain/box[(@textflowid = $boxtextflowid) and (contains(@type,'_') = false)]/@frameColor" />;</xsl:when>
											<xsl:otherwise></xsl:otherwise>
										</xsl:choose>
									</xsl:if>
								</xsl:attribute>
							</xsl:if>
							<xsl:attribute name="data-Self"><xsl:value-of select="@Self"/></xsl:attribute>
							<xsl:attribute name="data-pg"><xsl:value-of select="$pg"/></xsl:attribute>
							<xsl:attribute name="data-pn"><xsl:value-of select="$pn"/></xsl:attribute>
							<xsl:attribute name="data-aid"><xsl:value-of select="$aid"/></xsl:attribute>

							<xsl:choose>
								<xsl:when test="../@objstyle != ''"><xsl:attribute name="data-boxobjstyle"><xsl:value-of select="../@objstyle"/></xsl:attribute></xsl:when><!-- ANCHORED OBJECT: the parent container box may have an interesting object style -->
								<xsl:when test="../boxchain/box[@Self = $self]/@objstyle"><xsl:attribute name="data-boxobjstyle"><xsl:value-of select="../boxchain/box[@Self = $self]/@objstyle"/></xsl:attribute></xsl:when><!-- NORMAL OBJECT - parent is <article: the container boxchain/box with same @Self may have an interesting object style -->
							</xsl:choose>

							<xsl:if test="$story_id != ''"><xsl:attribute name="data-story_id"><xsl:value-of select="$story_id"/></xsl:attribute></xsl:if>
							<xsl:if test="$p_f != ''"><xsl:attribute name="data-p_f"><xsl:value-of select="$p_f"/></xsl:attribute></xsl:if>
							<xsl:if test="$n_f != ''"><xsl:attribute name="data-n_f"><xsl:value-of select="$n_f"/></xsl:attribute></xsl:if>
							<xsl:if test="$p_id != ''"><xsl:attribute name="data-p_id"><xsl:value-of select="$p_id"/></xsl:attribute></xsl:if>
							<xsl:if test="$n_id != ''"><xsl:attribute name="data-n_id"><xsl:value-of select="$n_id"/></xsl:attribute></xsl:if>
							<xsl:if test="@label != ''"><xsl:attribute name="data-label"><xsl:value-of select="@label"/></xsl:attribute></xsl:if>
							<xsl:if test="@toc"><xsl:attribute name="data-toc"><xsl:value-of select="@toc"/></xsl:attribute></xsl:if>
							<xsl:if test="$section != ''"><xsl:attribute name="data-section"><xsl:value-of select="$section"/></xsl:attribute></xsl:if>
							<xsl:if test="$longSection != ''"><xsl:attribute name="data-longSection"><xsl:value-of select="$longSection"/></xsl:attribute></xsl:if>

							<xsl:if test="@backgroundColor != ''"><xsl:attribute name="data-backgroundColor"><xsl:value-of select="@backgroundColor"/></xsl:attribute></xsl:if>
							<xsl:if test="@angle != ''"><xsl:attribute name="data-angle"><xsl:value-of select="@angle"/></xsl:attribute></xsl:if>
							<xsl:if test="@scaleX != ''"><xsl:attribute name="data-scaleX"><xsl:value-of select="@scaleX"/></xsl:attribute></xsl:if>
							<xsl:if test="@scaleY != ''"><xsl:attribute name="data-scaleY"><xsl:value-of select="@scaleY"/></xsl:attribute></xsl:if>
							<xsl:if test="@x1 != ''"><xsl:attribute name="data-x1"><xsl:value-of select="@x1"/></xsl:attribute></xsl:if>
							<xsl:if test="@y1 != ''"><xsl:attribute name="data-y1"><xsl:value-of select="@y1"/></xsl:attribute></xsl:if>
							<xsl:if test="@x2 != ''"><xsl:attribute name="data-x2"><xsl:value-of select="@x2"/></xsl:attribute></xsl:if>
							<xsl:if test="@y2 != ''"><xsl:attribute name="data-y2"><xsl:value-of select="@y2"/></xsl:attribute></xsl:if>
<xsl:text>
</xsl:text><!-- pretty print -->
							<xsl:apply-templates select="* | comment() | text()"/>
						</div>
<xsl:text>
</xsl:text><!-- pretty print -->
					</xsl:otherwise>
				</xsl:choose>

			</xsl:when>
			<xsl:otherwise>
					<xsl:apply-templates select="* | comment() | text()"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- get anchored objects -->
	<xsl:template match="anchored_object">
		<xsl:choose>
			<xsl:when test="@name = 'Note'">
				<xsl:if test="/indd_document/call_parameters/par[@name='excludeNotes']/. = '0'"><div class="C_Notes__"><xsl:apply-templates/></div></xsl:if>
			</xsl:when>
			<xsl:when test="(@name = 'ctbl') and (/indd_document/call_parameters/par[@name='TABLE_AS_BLOCK']/. = '1')">
				<xsl:apply-templates/>
			</xsl:when>
			<xsl:when test="(@name = 'FNcl')"><!-- footnotes -->
				<span class="A_anchored_footnote__">
					<xsl:attribute name="title">
						<xsl:choose>
							<xsl:when test="descendant::footnote/@title">
								<xsl:value-of select="descendant::footnote/@title" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="descendant::div/." />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:apply-templates select="descendant::footnote_num | comment() | text()"/>
				</span>
			</xsl:when>
			<xsl:when test="(@TOCstyle != '')"><!-- TOC links -->
				<div class="A_TOCstyleDiv__">
					<xsl:apply-templates/>
				</div>
			</xsl:when>
			<xsl:otherwise><span class="A_anchored_object__"><xsl:attribute name="data-anchor_id"><xsl:value-of select="@anchorid"/></xsl:attribute><xsl:apply-templates><xsl:with-param name="anchorid"><xsl:value-of select="@anchorid"/></xsl:with-param></xsl:apply-templates></span></xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- get content of Notes -->
	<xsl:template match="Note">
		<xsl:apply-templates select="* | comment() | text()"/>
	</xsl:template>

	<!-- get content of footnotes -->
	<xsl:template match="footnote">
		<div class="A_footnotetext__"><xsl:apply-templates select="* | comment() | text()"/></div>
	</xsl:template>
	<xsl:template match="footnote_num">
		<xsl:apply-templates select="* | comment() | text()"/>
	</xsl:template>


	<!-- get content of boxes -->
	<xsl:template match="box">
		<xsl:apply-templates select="* | comment() | text()"/>
	</xsl:template>


	<!-- get content of groups -->
	<xsl:template match="group">
		<xsl:apply-templates select="* | comment() | text()"/>
	</xsl:template>


	<!-- get content of MultiStateObjects -->
	<xsl:template match="multistate">
		<xsl:apply-templates select="* | comment() | text()"/>
	</xsl:template>


	<!-- get content of xmlElement -->
	<xsl:template match="xmlElement">
		<xsl:choose>
			<xsl:when test="$debug_xmlelement != '0'">
				<xsl:choose>
					<xsl:when test="$structure_xmlelement != '0'">
						<xsl:variable name="structName">
							<xsl:choose>
								<xsl:when test="(name(..) = 'span') or (name(../..) = 'span') or (name(../../..) = 'span') or (name(../../../..) = 'span')">span</xsl:when>
								<xsl:otherwise>div</xsl:otherwise>
							</xsl:choose>
						</xsl:variable>
						<xsl:element name="{$structName}">
							<xsl:attribute name="class">xmlStructDebug</xsl:attribute>
							<xsl:attribute name="data-Self"><xsl:value-of select="@Self"/></xsl:attribute>
							<span class="xmlElemDebug">
								<xsl:attribute name="title">
									<xsl:for-each select="@*">
										<xsl:text> </xsl:text><xsl:value-of disable-output-escaping="yes" select="name()"/><xsl:text>='</xsl:text><xsl:value-of select="."/><xsl:text>'</xsl:text>
									</xsl:for-each>
								</xsl:attribute>
								<xsl:text>XML[</xsl:text></span>
							<xsl:apply-templates select="* | comment() | text()"/>
							<span class="xmlElemDebug">
								<xsl:attribute name="title">
									<xsl:for-each select="@*">
										<xsl:text> </xsl:text><xsl:value-of disable-output-escaping="yes" select="name()"/><xsl:text>='</xsl:text><xsl:value-of select="."/><xsl:text>'</xsl:text>
									</xsl:for-each>
								</xsl:attribute>
								<xsl:text>]</xsl:text></span>
						</xsl:element>
					</xsl:when>
					<xsl:otherwise>
						<span class="xmlElemDebug">
							<xsl:attribute name="data-Self"><xsl:value-of select="@Self"/></xsl:attribute>
							<xsl:attribute name="title">
								<xsl:for-each select="@*">
									<xsl:text> </xsl:text><xsl:value-of disable-output-escaping="yes" select="name()"/><xsl:text>='</xsl:text><xsl:value-of select="."/><xsl:text>'</xsl:text>
								</xsl:for-each>
							</xsl:attribute>
							<xsl:text>XML[</xsl:text></span>
						<xsl:apply-templates select="* | comment() | text()"/>
						<span class="xmlElemDebug">
							<xsl:attribute name="title">
								<xsl:for-each select="@*">
									<xsl:text> </xsl:text><xsl:value-of disable-output-escaping="yes" select="name()"/><xsl:text>='</xsl:text><xsl:value-of select="."/><xsl:text>'</xsl:text>
								</xsl:for-each>
							</xsl:attribute>
							<xsl:text>]</xsl:text></span>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="* | comment() | text()"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- suppress xmlInstruction -->
	<xsl:template match="xmlInstruction">
			<xsl:if test="$debug_xmlelement != '0'">
				<span class="xmlInstDebug">
					<xsl:attribute name="title">
						<xsl:for-each select="@*">
							<xsl:text> </xsl:text><xsl:value-of disable-output-escaping="yes" select="name()"/><xsl:text>='</xsl:text><xsl:value-of select="."/><xsl:text>'</xsl:text>
						</xsl:for-each>
					</xsl:attribute>
					<xsl:text>XMI[]</xsl:text></span>
			</xsl:if>
	</xsl:template>

	<!-- LINE SEPARATOR -->
	<xsl:template match="br"><br/></xsl:template><!-- <br type="LINE SEPARATOR"/> -->


	<!-- copy elements -->
	<xsl:template match="*">
		<xsl:param name="isanchored"/>
		<xsl:choose>
			<!-- handle images -->
			<xsl:when test="name() = 'img2'"/><!-- suppress now, this is handled in img tag -->
			<xsl:when test="name() = 'img3'"/><!-- suppress now, this is handled in img tag -->
			<xsl:when test="name() = 'img4'"/><!-- suppress now, this is handled in img tag -->
			<xsl:when test="name()='img'"><!-- comment images to prevent preloading. They will be enabled by XTXPepaper.js when showing an article -->
				<!-- check if we have a second image (enlarged) -->
				<xsl:variable name="imagetitle">
					<xsl:choose>
						<xsl:when test="@title/. != ''"><!-- get from own image title (usually not set) -->
							<xsl:value-of select="@title/."/>
						</xsl:when>
						<xsl:when test="../@title/. != ''"><!-- get from container title given by script label. this may be the 'content' or 'a' (link) element -->
							<xsl:value-of select="../@title/."/>
						</xsl:when>
						<xsl:when test="following-sibling::img2[1]">^</xsl:when>
						<xsl:otherwise></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>

				<xsl:variable name="addOuterBorder" select="(../@frameWeight != '0') and (../@frameWeight != '') and (../@frameColor != '')"/><!-- content/@frameWeight -->
				<xsl:variable name="style">
					<xsl:if test="$addOuterBorder != ''">
						<xsl:text>style="border:</xsl:text><xsl:value-of select="ceiling(../@frameWeight)" />px <xsl:value-of select="../@frameStyleHTML" /><xsl:text> </xsl:text><xsl:if test="(../@frameColor != '') and (../@frameColor != 'transparent') and (starts-with(../@frameColor,'#') = false)">#</xsl:if><xsl:value-of select="../@frameColor" /><xsl:text>;"</xsl:text>
					</xsl:if>
				</xsl:variable>

        <!-- get image1 width and height -->
				<xsl:variable name="width1">
					<xsl:if test="(../@x2 != '') and (../@x1 != '') ">
					  <xsl:value-of select="../@x2 - ../@x1"/>
					</xsl:if>
				</xsl:variable>
				<xsl:variable name="height1">
					<xsl:if test="(../@y2 != '') and (../@y1 != '') ">
					  <xsl:value-of select="../@y2 - ../@y1"/>
					</xsl:if>
				</xsl:variable>
        <!-- get enlarged image2 width and height -->
				<xsl:variable name="imgname2">
				  <xsl:if test="following-sibling::img2[1]">
				     <xsl:value-of select="following-sibling::img2[1]/@src"/>
				  </xsl:if>
				</xsl:variable>
				<xsl:variable name="width2">
				  <xsl:if test="following-sibling::img2[1]">
				     <xsl:value-of select="following-sibling::img2[1]/@width"/>
				  </xsl:if>
				</xsl:variable>
				<xsl:variable name="height2">
				  <xsl:if test="following-sibling::img2[1]">
				     <xsl:value-of select="following-sibling::img2[1]/@height"/>
				  </xsl:if>
				</xsl:variable>

				<xsl:choose>
					<xsl:when test="(@originalAvail = '1') and (($imagetitle = '') or ($imagetitle = '^'))"><!-- original image is available and no other @title is set -->
						<xsl:comment>image <xsl:if test="@class != ''"><xsl:text>class="</xsl:text><xsl:value-of select="@class"/><xsl:text>" </xsl:text></xsl:if><xsl:if test="$style != ''"><xsl:value-of select="$style"/><xsl:text> </xsl:text></xsl:if>src="<xsl:value-of select="@src" />" alt="<xsl:value-of select="@alt" />"<xsl:if test="following-sibling::img2[1]"> title="<xsl:value-of select="$imagetitle"/>" onClick="javascript:showImage('<xsl:value-of select="concat($imglargeurl,following-sibling::img2[1]/@src)"/>');"</xsl:if>/image</xsl:comment>
					</xsl:when>
					<xsl:otherwise><!-- original images are not available. Exported directly by InDesign? -->
						<xsl:comment>nimage <xsl:if test="$width1 != ''"><xsl:text>width="</xsl:text><xsl:value-of select="$width1"/><xsl:text>px" </xsl:text></xsl:if><xsl:if test="$height1 != ''"><xsl:text>height="</xsl:text><xsl:value-of select="$height1"/><xsl:text>px" </xsl:text></xsl:if><xsl:if test="@class != ''"><xsl:text>class="</xsl:text><xsl:value-of select="@class"/><xsl:text>" </xsl:text></xsl:if><xsl:if test="$style != ''"><xsl:value-of select="$style"/><xsl:text> </xsl:text></xsl:if>src="<xsl:value-of select="@src" />" alt="<xsl:value-of select="@alt" />"<xsl:if test="$imgname2 != ''"> onClick="javascript:showImage('<xsl:value-of select="concat($imglargeurl,$imgname2)"/>',<xsl:value-of select="$width2"/>,<xsl:value-of select="$height2"/>);"</xsl:if> title="<xsl:value-of select="$imagetitle"/>"/image</xsl:comment>
					</xsl:otherwise>
				</xsl:choose>
				<!-- check if we should image meta data -->
				<xsl:if test="metadata">
					<xsl:if test="(metadata/title/. != '') or (metadata/description/. != '')">
						<div class="imgmeta">
							<xsl:if test="metadata/title/. != ''">
								<div class="imgmeta_title"><xsl:value-of select="metadata/title/."/></div>
							</xsl:if>
							<xsl:if test="metadata/description/. != ''">
								<div class="imgmeta_description"><xsl:value-of select="metadata/description/."/></div>
							</xsl:if>
						</div>
					</xsl:if>
				</xsl:if>
			</xsl:when>

			<xsl:when test="name()='span' and starts-with(@class,'C_gotoPage')"><!-- check for index page navigation -->
				<xsl:element name="{name()}">
					<xsl:for-each select="@*">
						<xsl:if test="name() != 'cstyID'">
							<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
						</xsl:if>
					</xsl:for-each>
					<xsl:variable name="pagenumber">
						<xsl:value-of select="." />
					</xsl:variable>
					<a><xsl:attribute name="onclick">return goto_page('<xsl:value-of select="$pagenumber" />',false);</xsl:attribute>
						<xsl:attribute name="href">javascript:void(0);</xsl:attribute>
						<xsl:apply-templates select="* | text()"/>
					</a>
				</xsl:element>
			</xsl:when>

			<!-- ******** div -->
			<xsl:when test="name()='div'">
				<xsl:choose>
					<xsl:when test="$isanchored = '1'">
						<xsl:element name="span">
							<xsl:if test="not(@class)">
								<xsl:attribute name="class">C_anchored_object__</xsl:attribute>
							</xsl:if>
							<xsl:attribute name="data-anchored">1</xsl:attribute>

							<xsl:for-each select="@*">
								<xsl:choose>
									<xsl:when test="name() = 'class'">
										<xsl:attribute name="{name()}"><xsl:value-of select="." /> C_anchored_object__</xsl:attribute>
									</xsl:when>
									<xsl:when test="name() = 'style' and contains(.,'vertical-align:su')"><!-- sub and super font sizes -->
										<xsl:attribute name="{name()}"><xsl:value-of select="." />
											<xsl:choose>
												<xsl:when test="/indd_document/call_parameters/par[@name='fontsizeUnits']/. = '2'">font-size:0.75em;</xsl:when>
												<xsl:otherwise>font-size:75%;</xsl:otherwise>
											</xsl:choose>
										</xsl:attribute>
									</xsl:when>
									<xsl:when test="name() = 'style'">
										<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
									</xsl:when>
									<xsl:when test="name() = 'data-fontsize'">
										<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
									</xsl:when>
									<xsl:otherwise></xsl:otherwise><!-- suppress all other invalid HTML attribs -->
								</xsl:choose>
							</xsl:for-each>
							<xsl:if test="((. = '') or (. = ' ')) and ($suppress_empty_divs = '0')">&#160;</xsl:if><!-- empty divs are ignored by browsers as vertical white space. add a non breaking space -->
							<xsl:apply-templates select="* | text()"/>
						</xsl:element>
					</xsl:when>
					<xsl:otherwise>
						<xsl:element name="{name()}">
							<xsl:for-each select="@*">
								<xsl:choose>
									<xsl:when test="name() = 'class'">
										<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
									</xsl:when>
									<xsl:when test="name() = 'style' and contains(.,'vertical-align:su')"><!-- sub and super font sizes -->
										<xsl:attribute name="{name()}"><xsl:value-of select="." />
											<xsl:choose>
												<xsl:when test="/indd_document/call_parameters/par[@name='fontsizeUnits']/. = '2'">font-size:0.75em;</xsl:when>
												<xsl:otherwise>font-size:75%;</xsl:otherwise>
											</xsl:choose>
										</xsl:attribute>
									</xsl:when>
									<xsl:when test="name() = 'style'">
										<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
									</xsl:when>
									<xsl:when test="name() = 'data-fontsize'">
										<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
									</xsl:when>
									<xsl:otherwise></xsl:otherwise><!-- suppress all other invalid HTML attribs -->
								</xsl:choose>
							</xsl:for-each>
							<xsl:if test="((. = '') or (. = ' ')) and ($suppress_empty_divs = '0')">&#160;</xsl:if><!-- empty divs are ignored by browsers as vertical white space. add a non breaking space -->
							<xsl:apply-templates select="* | text()"/>
						</xsl:element>
<xsl:text>
</xsl:text><!-- pretty print -->
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>

			<!-- ******** span -->
			<xsl:when test="name()='span'">
				<xsl:element name="{name()}">
					<xsl:for-each select="@*">
						<xsl:choose>
							<xsl:when test="name() = 'class'">
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:when>
							<xsl:when test="name() = 'style' and contains(.,'vertical-align:su')"><!-- sub and super font sizes -->
								<xsl:attribute name="{name()}"><xsl:value-of select="." />
									<xsl:choose>
										<xsl:when test="/indd_document/call_parameters/par[@name='fontsizeUnits']/. = '2'">font-size:0.75em;</xsl:when>
										<xsl:otherwise>font-size:75%;</xsl:otherwise>
									</xsl:choose>
								</xsl:attribute>
							</xsl:when>
							<xsl:when test="name() = 'style'">
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:when>
							<xsl:when test="starts-with(name(),'data-')"><!-- like 'data&min;fontsize' and 'data&min;overflow_content_id' (from OmniPage XML) -->
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:when>
							<xsl:otherwise></xsl:otherwise><!-- suppress all other invalid HTML attribs -->
						</xsl:choose>
					</xsl:for-each>
					<xsl:apply-templates select="* | text()"/>
				</xsl:element>
			</xsl:when>

			<!-- ******** a links -->
			<xsl:when test="name()='a'">
				<xsl:element name="{name()}">
					<xsl:choose>
						<xsl:when test="@class = 'TOCstyle'"><!-- MUST be FIRST!!! a Table of Contents anchor -->
							<xsl:attribute name="class">A_<xsl:value-of select="@class" />__</xsl:attribute>
							<xsl:choose>
								<xsl:when test="@targetPageName != ''">
									<xsl:attribute name="onclick">return goto_page('<xsl:value-of select="@targetPageName" />',false);</xsl:attribute>
									<xsl:attribute name="href">javascript:void(0);</xsl:attribute>
								</xsl:when>
								<xsl:when test="@ext_destPageName != ''"><!-- may be it is a book? -->
									<xsl:variable name="targetPageNameTmp">
										<xsl:value-of select="substring-before(@ext_destPageName/.,'.indd')"/>
									</xsl:variable>
									<xsl:variable name="targetPageIdx">
										<xsl:value-of select="@ext_destPageIdx"/>
									</xsl:variable>
									<xsl:variable name="targetPageName">
										<xsl:value-of select="/indd_document/descendant::page[(@docname = $targetPageNameTmp) and (@page_sequence_orig = $targetPageIdx)]/@page_name"/>
									</xsl:variable>
									<xsl:attribute name="onclick">return goto_page('<xsl:value-of select="$targetPageName" />',false);</xsl:attribute>
									<xsl:attribute name="href">javascript:void(0);</xsl:attribute>
								</xsl:when>
							</xsl:choose>
							<xsl:if test="@anchorname != ''">
								<xsl:attribute name="name"><xsl:value-of select="@anchorname" /></xsl:attribute>
							</xsl:if>
							<xsl:if test="@title != ''">
								<xsl:attribute name="title"><xsl:value-of select="@title" /></xsl:attribute>
							</xsl:if>
							<xsl:if test="@id != ''">
								<xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute>
							</xsl:if>
						</xsl:when>
						<xsl:when test="@data-destinationStoryID"><!-- internal link to HyperlinkTextDestination -->
							<xsl:variable name="data-destinationStoryID"><xsl:value-of select="@data-destinationStoryID"/></xsl:variable>
							<xsl:variable name="articleIDX">
								<xsl:variable name="articleIDXtmp"><xsl:value-of select="/indd_document/descendant::page/articles/descendant::article[descendant::content/@id = $data-destinationStoryID]/@idx"/></xsl:variable>
								<xsl:choose>
									<xsl:when test="$articleIDXtmp != ''"><xsl:value-of select="$articleIDXtmp"/></xsl:when><!-- CS5 IDML data -->
									<xsl:otherwise><!-- CS4 INX data -->
										<xsl:value-of select="/indd_document/descendant::page/articles/descendant::article[descendant::a/@anchorname = $data-destinationStoryID]/@idx"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<xsl:variable name="articlePS">
								<xsl:variable name="articlePStmp"><xsl:value-of select="/indd_document/descendant::page/articles/descendant::article[descendant::content/@id = $data-destinationStoryID]/@page_sequence"/></xsl:variable>
								<xsl:choose>
									<xsl:when test="$articlePStmp != ''"><xsl:value-of select="$articlePStmp"/></xsl:when><!-- CS5 IDML data -->
									<xsl:otherwise><!-- CS4 INX data -->
										<xsl:value-of select="/indd_document/descendant::page/articles/descendant::article[descendant::a/@anchorname = $data-destinationStoryID]/@page_sequence"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<xsl:attribute name="data-destinationArticleID">
								<xsl:text>Art</xsl:text>
								<xsl:value-of select="$articleIDX" />
								<xsl:text>_</xsl:text>
								<xsl:value-of select="$articlePS" />
							</xsl:attribute>
							<xsl:attribute name="href"><xsl:value-of select="@href" /></xsl:attribute>
						</xsl:when>
						<xsl:when test="@href != ''"><!-- a points to destination text anchor and has href -->
							<xsl:attribute name="href"><xsl:value-of select="@href" /></xsl:attribute>
							<xsl:attribute name="title"><xsl:value-of select="@title" /></xsl:attribute>
							<xsl:if test="@id != ''"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
						</xsl:when>
						<xsl:when test="@destinationanchorname"><!-- a source text anchor -->
							<xsl:attribute name="href"><xsl:value-of select="@destinationanchorhtm" />?anc=<xsl:value-of select="@destinationanchorname" /></xsl:attribute>
							<xsl:attribute name="title"><xsl:value-of select="@title" /></xsl:attribute>
							<xsl:if test="@id != ''"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
						</xsl:when>
						<xsl:when test="@anchorname"><!-- a destination text anchor -->
							<xsl:attribute name="name"><xsl:value-of select="@anchorname" /></xsl:attribute>
							<xsl:attribute name="title"><xsl:value-of select="@title" /></xsl:attribute>
							<xsl:if test="@id != ''"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
						</xsl:when>
						<xsl:when test="@targetPageName != ''"><!-- for HyperlinkPageDestination -->
							<xsl:attribute name="onclick">closeLightbox();return goto_page('<xsl:value-of select="@targetPageName" />',false,null,null,true);</xsl:attribute>
							<xsl:attribute name="href">javascript:void(0);</xsl:attribute>
						</xsl:when>
						<xsl:otherwise><!-- a url link -->
							<xsl:for-each select="@*">
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:for-each>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:apply-templates select="* | text()"/>
				</xsl:element>
			</xsl:when>

			<!-- ******** table -->
			<xsl:when test="name()='table'">
				<xsl:element name="{name()}">
					<xsl:if test="@class"><xsl:attribute name="class"><xsl:value-of select="@class" /></xsl:attribute></xsl:if>
					<xsl:if test="@style"><xsl:attribute name="style"><xsl:value-of select="@style" /></xsl:attribute></xsl:if>
					<xsl:apply-templates select="* | text()"/>
				</xsl:element>
			</xsl:when>

			<!-- strip table settings -->
			<xsl:when test="name()='tablesettings'">
			</xsl:when>

			<!-- ******** table rows -->
			<xsl:when test="name()='tr'">
				<xsl:element name="{name()}">
					<xsl:if test="@class"><xsl:attribute name="class"><xsl:value-of select="@class" /></xsl:attribute></xsl:if>
					<xsl:if test="@style"><xsl:attribute name="style"><xsl:value-of select="@style" /></xsl:attribute></xsl:if>
					<xsl:apply-templates select="* | text()"/>
				</xsl:element>
<xsl:text>
</xsl:text><!-- pretty print -->
			</xsl:when>

			<!-- ******** table cells -->
			<xsl:when test="name()='td'">
				<xsl:element name="{name()}">
					<xsl:for-each select="@*">
						<xsl:choose>
							<!-- === this is OmniPage XML stuff ===-->
							<xsl:when test="(name() = 'style') and contains(/indd_document/header/headerfield[@name = 'OCRversion'],'OmniPage')"><!-- contained in XML from Omnipage -->
									<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:when>
							<!-- === this is InDesign stuff ===-->
							<xsl:when test="(name() = 'cstyID') or (name() = 'pstyID')"></xsl:when>
							<xsl:when test="(name() = 'backgroundColorID') or (name() = 'backgroundColorFilt')"></xsl:when>
							<xsl:when test="(name() = 'borderLeftColorID') or (name() = 'borderTopColorID') or (name() = 'borderRightColorID') or (name() = 'borderBottomColorID')"></xsl:when>
							<xsl:when test="(name() = 'borderLeftStyleID') or (name() = 'borderTopStyleID') or (name() = 'borderRightStyleID') or (name() = 'borderBottomStyleID')"></xsl:when>
							<xsl:when test="((name() = 'colwidth') or (name() = 'style')) and ((/indd_document/call_parameters/par[@name = 'TABLE_CELLS_WIDTH_PRESERVE']/. != '0') and (/indd_document/call_parameters/par[@name = 'TABLE_CELLS_WIDTH_PRESERVE']/. != ''))">
								<xsl:choose>
									<xsl:when test="name() = 'colwidth'">
										<xsl:variable name="cellwidth" select="/indd_document/call_parameters/par[@name = 'TABLE_CELLS_WIDTH_PRESERVE']/."/>
										<xsl:attribute name="style"><xsl:value-of select="../@style/." /> width:<xsl:value-of select=". * $cellwidth" />px;</xsl:attribute>
									</xsl:when>
									<xsl:when test="name() = 'style'"></xsl:when><!-- handled above -->
									<xsl:otherwise>
										<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:when>
							<xsl:otherwise>
								<xsl:choose>
									<xsl:when test="name() = 'colwidth'"></xsl:when><!-- suppress -->
									<xsl:otherwise>
										<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
					<xsl:apply-templates select="* | text()"/>
				</xsl:element>
			</xsl:when>

			<!-- ******** push buttons -->
			<xsl:when test="name()='button'">
				<xsl:element name="{name()}">
					<xsl:attribute name="id"><xsl:value-of select="@Self" /></xsl:attribute>
					<xsl:attribute name="name"><xsl:value-of select="@buttonname" /></xsl:attribute>
					<xsl:attribute name="type">button</xsl:attribute>
					<xsl:choose>
						<xsl:when test="@objstyle">
							<xsl:attribute name="class"><xsl:value-of select="@objstyle" /></xsl:attribute>
						</xsl:when>
						<!-- style it if you want to
						<xsl:otherwise>
							<xsl:attribute name="style">border:0;margin:0;padding:0;background-color:transparent;</xsl:attribute>
						</xsl:otherwise>
						-->
					</xsl:choose>
					<xsl:if test="@title"><xsl:attribute name="title"><xsl:value-of select="@title" /></xsl:attribute></xsl:if>
					<!-- muev=onMouseUp, mdev=onClick, meev=onMouseOver, mxev=onMouseOut, ofev=onFocus, obev=onBlur -->
					<!-- BehaviorEvents_EnumValue = "MouseUp" | "MouseDown" | "MouseEnter" | "MouseExit" | "OnFocus" | "OnBlur" -->
					<xsl:for-each select="action">
						<xsl:variable name="self"><xsl:value-of select="../@Self" /></xsl:variable>
						<xsl:if test="@type/. = 'Sound'">
							<!-- IDML events -->
							<xsl:if test="@event/. = 'MouseUp'"><xsl:attribute name="onMouseUp">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseUp -->
							<xsl:if test="@event/. = 'MouseDown'"><xsl:attribute name="onClick">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseDown make onClick -->
							<xsl:if test="@event/. = 'MouseEnter'"><xsl:attribute name="onMouseOver">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseOver -->
							<xsl:if test="@event/. = 'MouseExit'"><xsl:attribute name="onMouseOut">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseOut -->
							<xsl:if test="@event/. = 'OnFocus'"><xsl:attribute name="onFocus">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onFocus -->
							<xsl:if test="@event/. = 'OnBlur'"><xsl:attribute name="onBlur">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onBlur -->
							<!-- old INX events -->
							<xsl:if test="@event/. = 'muev'"><xsl:attribute name="onMouseUp">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseUp -->
							<xsl:if test="@event/. = 'mdev'"><xsl:attribute name="onClick">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseDown make onClick -->
							<xsl:if test="@event/. = 'meev'"><xsl:attribute name="onMouseOver">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseOver -->
							<xsl:if test="@event/. = 'mxev'"><xsl:attribute name="onMouseOut">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseOut -->
							<xsl:if test="@event/. = 'ofev'"><xsl:attribute name="onFocus">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onFocus -->
							<xsl:if test="@event/. = 'obev'"><xsl:attribute name="onBlur">playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onBlur -->
						</xsl:if>
						<xsl:if test="(@type/. = 'Video') or (@type/. = 'Movie')">
							<!-- IDML events -->
							<xsl:if test="@event/. = 'MouseUp'"><xsl:attribute name="onMouseUp">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseUp -->
							<xsl:if test="@event/. = 'MouseDown'"><xsl:attribute name="onClick">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseDown make onClick -->
							<xsl:if test="@event/. = 'MouseEnter'"><xsl:attribute name="onMouseOver">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseOver -->
							<xsl:if test="@event/. = 'MouseExit'"><xsl:attribute name="onMouseOut">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseOut -->
							<xsl:if test="@event/. = 'OnFocus'"><xsl:attribute name="onFocus">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onFocus -->
							<xsl:if test="@event/. = 'OnBlur'"><xsl:attribute name="onBlur">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onBlur -->
							<!-- old INX events -->
							<xsl:if test="@event/. = 'muev'"><xsl:attribute name="onMouseUp">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseUp -->
							<xsl:if test="@event/. = 'mdev'"><xsl:attribute name="onClick">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseDown make onClick -->
							<xsl:if test="@event/. = 'meev'"><xsl:attribute name="onMouseOver">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseOver -->
							<xsl:if test="@event/. = 'mxev'"><xsl:attribute name="onMouseOut">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onMouseOut -->
							<xsl:if test="@event/. = 'ofev'"><xsl:attribute name="onFocus">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onFocus -->
							<xsl:if test="@event/. = 'obev'"><xsl:attribute name="onBlur">playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />('media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />','<xsl:value-of select="@name"/>');</xsl:attribute></xsl:if><!-- onBlur -->
						</xsl:if>
					</xsl:for-each>
					<!-- do not copy the alt attribute or Safari will try to open the original button image -->
					<img>
						<xsl:attribute name="src"><xsl:value-of select="buttonstate[@name != 'Rollover']//content/img/@src"/></xsl:attribute>
					</img>
				</xsl:element>
				<xsl:for-each select="action">
					<xsl:variable name="self"><xsl:value-of select="../@Self" /></xsl:variable>
					<xsl:if test="@type/. = 'Sound'">
<div>
	<xsl:if test="(../@soundwidth='0') or (../@soundheight='0')">
		<xsl:attribute name="style">visibility:hidden</xsl:attribute>
	</xsl:if>
	<xsl:attribute name="id">media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" /></xsl:attribute>
</div>
<script type="text/javascript">
function playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />(id,src) {
	var code="",mid="a_"+id,klass="",style="",script="",width=0,height=0,controls=true,autoplay=true,soundname="",type="",thename="",thetype="",alternate="",altarr=null,nameparts=null;
	<xsl:if test="../@mmid">mid="<xsl:value-of select="../@mmid"/>";</xsl:if><!-- try to get from label -->
	<xsl:if test="../@mmclass">klass="<xsl:value-of select="../@mmclass"/>";</xsl:if><!-- try to get from label -->
	<xsl:if test="../@mmstyle">style="<xsl:value-of select="../@mmstyle"/>";</xsl:if><!-- try to get from label -->
	<xsl:if test="../@mmscript">script="<xsl:value-of select="../@mmscript"/>";</xsl:if><!-- try to get from label -->
	<xsl:if test="../@soundwidth">width=<xsl:value-of select="../@soundwidth"/>;</xsl:if>
	<xsl:if test="../@soundheight">height=<xsl:value-of select="../@soundheight"/>;</xsl:if>
	<xsl:if test="../@soundcontrols">controls=<xsl:value-of select="../@soundcontrols"/>;</xsl:if>
	<xsl:if test="../@soundautoplay">autoplay=<xsl:value-of select="../@soundautoplay"/>;</xsl:if>
	<xsl:if test="../@soundtype">type="<xsl:value-of select="../@soundtype"/>";</xsl:if>
	<xsl:choose>
		<xsl:when test="../@soundprimary">soundname="<xsl:value-of select="../@soundprimary"/>";</xsl:when>
		<xsl:otherwise>soundname="<xsl:value-of select="@name"/>";</xsl:otherwise>
	</xsl:choose>
	<xsl:if test="../@soundalternate">alternate="<xsl:value-of select="../@soundalternate"/>";</xsl:if>
	nameparts = soundname.split(";");
	thename=nameparts[0]; thetype="";
	if(nameparts.length>1) thetype=nameparts[1]; else thetype=type;
	if(thetype=="") thetype=(detMimetype?detMimetype(thename,"a"):"");
	code = "&lt;audio width='" + width + "' height='" + height + "'" + (autoplay==true?" autoplay=\"autoplay\"":"") + (controls==true?" controls=\"controls\"":"") + "&gt;";
		code += "&lt;source src='"+thename+"'"+(thetype!=""?" type='"+thetype+"'":"")+"&gt;&lt;/source&gt;";
		if (alternate != "") {
			altarr = alternate.split(",");
			for (var i=0;i&lt;altarr.length;i++){/*name;type*/
				nameparts=altarr[i].split(";");
				thename=nameparts[0];
				if(nameparts.length>1) thetype=nameparts[1]; else thetype="";
				if(thetype=="") thetype=(detMimetype?detMimetype(thename,"a"):"");
				code += "&lt;source src='" + thename + "'" + (thetype!=""?" type=\""+thetype+"\"":"") + "&gt;&lt;/source&gt;";
			}
		}
	code += "&lt;embed"+(thetype!=""?" type='"+thetype+"'":"") + " controls='console' hidden='false' autostart='" + (autoplay==true?'true':'false') + "' width='" + width + "' height='" + height + "' loop='false' src='"+thename+"'&gt;";
	code += "&lt;/audio&gt;";

	try { document.getElementById(id).innerHTML = code; } catch(e) {}
	if (script != "") {
		eval(script);
	}
}
</script>
					</xsl:if>
					<xsl:if test="(@type/. = 'Video') or (@type/. = 'Movie')">
<div><xsl:attribute name="id">media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" /></xsl:attribute></div>
<script type="text/javascript">
function playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />(id,src) {
	var code="",mid="v_"+id,klass="",style="",script="",width=-1,height=-1,controls=true,autoplay=true,moviename="",type="",thename="",thetype="",alternate="",altarr=null,nameparts=null;
	<xsl:choose>
		<xsl:when test="../@mmid">mid="<xsl:value-of select="../@mmid"/>";</xsl:when><!-- try to get from label -->
		<xsl:when test="@mmid">mid="<xsl:value-of select="@mmid"/>";</xsl:when><!-- get from original movie -->
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@mmclass">klass="<xsl:value-of select="../@mmclass"/>";</xsl:when><!-- try to get from label -->
		<xsl:when test="@mmclass">klass="<xsl:value-of select="@mmclass"/>";</xsl:when><!-- get from original movie -->
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@mmstyle">style="<xsl:value-of select="../@mmstyle"/>";</xsl:when><!-- try to get from label -->
		<xsl:when test="@mmstyle">style="<xsl:value-of select="@mmstyle"/>";</xsl:when><!-- get from original movie -->
	</xsl:choose>

	<xsl:choose>
		<xsl:when test="../@moviewidth">width=<xsl:value-of select="../@moviewidth"/>;</xsl:when><!-- try to get from label -->
		<xsl:when test="@movieWidth">width=<xsl:value-of select="@movieWidth"/>;</xsl:when><!-- get from original movie -->
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@movieheight">height=<xsl:value-of select="number(../@movieheight)+20"/>;</xsl:when>
		<xsl:when test="@movieHeight">height=<xsl:value-of select="number(@movieHeight)+20"/>;</xsl:when>
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@moviecontrols">controls=<xsl:value-of select="../@moviecontrols"/>;</xsl:when>
		<xsl:when test="@moviecontrols">controls=<xsl:value-of select="@moviecontrols"/>;</xsl:when>
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@movieautoplay">autoplay=<xsl:value-of select="../@movieautoplay"/>;</xsl:when>
		<xsl:when test="@movieautoplay">autoplay=<xsl:value-of select="@movieautoplay"/>;</xsl:when>
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@movietype">type="<xsl:value-of select="../@movietype"/>";</xsl:when>
		<xsl:when test="@movietype">type="<xsl:value-of select="@movietype"/>";</xsl:when>
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@movieprimary">moviename="<xsl:value-of select="../@movieprimary"/>";</xsl:when>
		<xsl:otherwise>moviename="<xsl:value-of select="@name"/>";</xsl:otherwise>
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@moviealternate">alternate="<xsl:value-of select="../@moviealternate"/>";</xsl:when>
		<xsl:when test="@moviealternate">alternate="<xsl:value-of select="@moviealternate"/>";</xsl:when>
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@mmscript">script="<xsl:value-of select="../@mmscript"/>";</xsl:when><!-- try to get from label -->
		<xsl:when test="@mmscript">script="<xsl:value-of select="@mmscript"/>";</xsl:when><!-- get from original movie -->
		<xsl:otherwise>script="customSetVideo(mid,code,width,height,moviename);";</xsl:otherwise><!-- default script -->
	</xsl:choose>
	nameparts = moviename.split(";");
	thename=nameparts[0]; thetype="";
	if(nameparts.length>1) thetype=nameparts[1]; else thetype=type;
	if(thetype=="") thetype=(detMimetype?detMimetype(thename,"v"):"");
	code = "&lt;video" + (mid!=''?(" id=\""+mid+"\""):"") + (klass!=''?(" class=\""+klass+"\""):"") + (style!=''?(" style=\""+style+"\""):"") + (width>0?(" width=\""+width+"\""):"") + (height>0?(" height=\""+height+"\""):"") + (autoplay==true?" autoplay=\"autoplay\"":"") + (controls==true?" controls=\"controls\"":"") + "&gt;";
		if (_sb_s.is_Android) code += "&lt;source src='"+thename+"'&gt;&lt;/source&gt;";	<!--Android -->
		code += "&lt;source src='"+thename+"'" +(thetype!=""?" type='"+thetype+"'":"")+"&gt;&lt;/source&gt;";
		if (alternate != "") {
			altarr = alternate.split(",");
			for (var i=0;i&lt;altarr.length;i++){/*name;type*/
				nameparts=altarr[i].split(";");
				thename=nameparts[0];
				if(nameparts.length>1) thetype=nameparts[1]; else thetype="";
				if(thetype=="") thetype=(detMimetype?detMimetype(thename,"v"):"");
				code += "&lt;source src='" + thename + "'" + (thetype!=""?" type=\""+thetype+"\"":"") + "&gt;&lt;/source&gt;";
			}
		}
		code += "&lt;object" + (width>0?(" width=\""+width+"\""):"") + (height>0?(" height=\""+height+"\""):"") + " type='" + thetype + "' data='" + thename + "'&gt;"
				+ "&lt;param name='movie' value='"+thename+"'/&gt;"
				+ "&lt;param name='controller' value='" + (controls==true?'true':'false') + "'/&gt;&lt;\/object&gt;";
	code += "&lt;/video&gt;";
	if (script != "") eval(script);
	else try { document.getElementById("lightbox_div").innerHTML = code; } catch(e) {}
}
</script>
					</xsl:if>
				</xsl:for-each>

				<!-- xsl:apply-templates select="descendant::div | descendant::text()"/ -->
				<xsl:apply-templates select="descendant::div"/>

			</xsl:when>

			<xsl:otherwise>
				<xsl:element name="{name()}">
					<xsl:for-each select="@*">
						<xsl:choose>
							<xsl:when test="(name() = 'cstyID') or (name() = 'pstyID')"></xsl:when>
							<!-- adjust table attributes -->
							<xsl:when test="(name(parent::node()) = 'table')">
								<xsl:choose>
									<xsl:when test="(name() = 'cellpadding')">
										<xsl:attribute name="{name()}"><xsl:value-of select=". + 2" /></xsl:attribute>
									</xsl:when>
									<xsl:otherwise>
										<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:when>
							<xsl:otherwise>
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
					<xsl:apply-templates select="* | text()"/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- do what ever you need from ctrlchar (control characters: see developer kit TextChar.h) -->
	<xsl:template match="ctrlchar">
		<xsl:choose>
			<xsl:when test="$debug_ctrlchar != '0'"><span style="color:red;">char="<xsl:value-of select="@chr"/>"</span></xsl:when>
			<xsl:when test="/indd_document/call_parameters/par[@name='preserveControlCharacters']/. = '0'">
				<xsl:choose><!-- output pre-converted ctrlchar tag  -->
					<xsl:when test="@code='3'"><xsl:text>&#x9;</xsl:text></xsl:when><!-- kTextChar_BreakRunInStyle -->
					<xsl:when test="@code='7'"></xsl:when><!-- kTextChar_IndentToHere -->
					<xsl:when test="@code='8'"><xsl:text>&#x9;</xsl:text></xsl:when><!-- kTextChar_RightAlignedTab -->
					<xsl:when test="@code='9'"><xsl:text>&#x9;</xsl:text></xsl:when><!-- kTextChar_Tab -->
					<xsl:when test="@code='18'"><xsl:value-of select="@value"/></xsl:when>
					<xsl:when test="@code='19'"></xsl:when><!-- kTextChar_EndOfMedium -->

					<xsl:when test="@code='E00B'"><br/></xsl:when><!-- kTextChar_ColumnBreak -->
					<xsl:when test="@code='E00C'"><br/></xsl:when><!-- kTextChar_PageBreak -->
					<xsl:when test="@code='E00D'"><br/></xsl:when><!-- kTextChar_FrameBoxBreak -->
					<xsl:when test="@code='E00E'"><br/></xsl:when><!-- kTextChar_OddPageBreak -->
					<xsl:when test="@code='E00F'"><br/></xsl:when><!-- kTextChar_EvenPageBreak -->
					<xsl:otherwise><xsl:comment>char="<xsl:value-of select="@code"/>"</xsl:comment></xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise><!-- keep original control characters -->
				<xsl:copy-of select="."/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- ===============================================
		 suppress
	     =============================================== -->
	<xsl:template match="textshortcut"></xsl:template>
	<xsl:template match="boxchain"></xsl:template>
	<xsl:template match="area"></xsl:template>
	<xsl:template match="paraopts"></xsl:template>


	<!-- ===============================================
		 the plain text content
	     =============================================== -->
	<xsl:template match="text()">
		<xsl:choose>
			<xsl:when test=". = '&#x0a;'"></xsl:when><!-- remove any combination of line breaks -->
			<xsl:when test=". = '&#x0d;'"></xsl:when>
			<xsl:when test=". = '&#x0d;&#x0a;'"></xsl:when>
			<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- ===============================================
		 the plain text content
	     =============================================== -->
	<xsl:template match="text()" mode="plaintext">
		<xsl:choose>
			<xsl:when test=". = '&#x0a;'"></xsl:when><!-- remove any combination of line breaks -->
			<xsl:when test=". = '&#x0d;'"></xsl:when>
			<xsl:when test=". = '&#x0d;&#x0a;'"></xsl:when>
			<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- ===============================================
		 comments
	     =============================================== -->
	<xsl:template match="comment()">
		<xsl:copy-of select="."/>
	</xsl:template>




	<!-- ===============================================
		 show creator comment for this output
	     =============================================== -->
	<xsl:template name="show_creator">
			<xsl:param name="creationDate"/>
			<xsl:param name="outputVersion"/>
			<xsl:param name="inputPath"/>
			<xsl:param name="indesignDocname"/>
			<xsl:param name="sourceINXfileName"/>
			<xsl:param name="transformEngine"/>
			<xsl:param name="uplid"/>
<xsl:comment>
<xsl:if test="/indd_document/@demo_mode = 1">****** DEMO MODE!! Contains scrambled text content!</xsl:if>
Creation Date: <xsl:value-of select="$creationDate" />
Output Version: <xsl:value-of select="$outputVersion" />
Input Path: <xsl:value-of select="$inputPath" />
Indesign Document Name: <xsl:value-of select="$indesignDocname" />
Source INX File Name: <xsl:value-of select="$sourceINXfileName" />
Transformer Engine: <xsl:value-of select="$transformEngine" />
UPLID: <xsl:value-of select="$uplid" />
</xsl:comment>
	</xsl:template>



</xsl:stylesheet>
