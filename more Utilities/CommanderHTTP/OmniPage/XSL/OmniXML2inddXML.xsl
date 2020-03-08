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

Version: 1.2
Version date: 20150924
======================================================================
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				xmlns:lxslt="http://xml.apache.org/xslt"
				xmlns:xalan="http://xml.apache.org/xalan" 
				xmlns:ssdoc="x-schema:http://www.scansoft.com/omnipage/xml/ssdoc-schema2.xml" 
				xmlns:myutils="com.epaperarchives.batchxslt.utils"
				xmlns:myJpgImage="com.epaperarchives.batchxslt.JpgImage"
				xmlns:exslt="http://exslt.org/common"
				xmlns:locjs="loc_funcs"

				xmlns:x="adobe:ns:meta/"
				xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
				xmlns:stRef="http://ns.adobe.com/xap/1.0/sType/ResourceRef#"
				xmlns:xap="http://ns.adobe.com/xap/1.0/"
				xmlns:xapMM="http://ns.adobe.com/xap/1.0/mm/"
				xmlns:xapGImg="http://ns.adobe.com/xap/1.0/g/img/"
				xmlns:xapRights="http://ns.adobe.com/xap/1.0/rights/"
				xmlns:xapTPg="http://ns.adobe.com/xap/1.0/t/pg/"
				xmlns:xapG="http://ns.adobe.com/xap/1.0/g/"
				xmlns:stFnt="http://ns.adobe.com/xap/1.0/sType/Font#"
				xmlns:dc="http://purl.org/dc/elements/1.1/"
				xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/"
				xmlns:illustrator="http://ns.adobe.com/illustrator/1.0/"
				xmlns:xmp="http://ns.adobe.com/xap/1.0/"
				xmlns:xmpGImg="http://ns.adobe.com/xap/1.0/g/img/"
								
				extension-element-prefixes="locjs"
				exclude-result-prefixes="locjs myutils myJpgImage exslt xalan lxslt ssdoc x rdf stRef xap xapMM xapGImg xapRights xapTPg xapG stFnt dc photoshop illustrator xmp xmpGImg"
				version="1.0">
<!-- standard parameters -->
	<xsl:param name="XMLSRC_VERSION"/>
	<xsl:param name="XMLSRC_ENCODING"/>
	<xsl:param name="XMLSRC_DOCTYPE_DECLARATION"/>
	<xsl:param name="SYSTEM_OS_NAME"/>
	<xsl:param name="SYSTEM_VM_VERSION"/>
	<xsl:param name="SYSTEM_DEFAULT_CHARSET"/>
	<xsl:param name="TRANSFORM_ENGINE"/>
	<xsl:param name="INPUT_PATH"/>
	<xsl:param name="INPUT_SUB_PATH"/>
	<xsl:param name="INPUT_NAME"/>
	<xsl:param name="OUTPUT_PATH"/>
	<xsl:param name="OUTPUT_NAME"/>
	<xsl:param name="STYLESHEET_PATH"/>
	<xsl:param name="STYLESHEET_NAME"/>
	<xsl:param name="LOGFILE_WRITE"/>
	<xsl:param name="LOGFILE_PATH"/>
	<xsl:param name="LOGFILE_NAME"/>
	<xsl:param name="USER_NAME"/><!-- user's name -->
	<xsl:param name="USER_HOME"/><!-- home dir -->
	<xsl:param name="USER_DIR"/><!-- work dir -->
	<xsl:param name="LOCAL_MACHINE_NAME"/>

	<xsl:param name="GS_VERSION"/><!-- Version string of reachable Ghostscript -->
	<xsl:param name="GS_VERSION_NUM"/><!-- Version number of reachable Ghostscript -->
	<xsl:param name="GS_PGM_PATH"/><!-- The path and name to Ghostscript program -->
	<xsl:param name="GS_ENVIR"/><!-- Environement variables to set when calling Ghostscript -->
	<xsl:param name="IM_VERSION"/><!-- Version string of reachable ImageMagick -->
	<xsl:param name="IM_VERSION_NUM"/><!-- Version number of reachable ImageMagick -->
	<xsl:param name="IM_PGM_PATH"/><!-- The path and name to ImageMagick program -->
	<xsl:param name="IM_ENVIR"/><!-- Environement variables to set when calling ImageMagick (the path to Ghostscript) -->

	<xsl:param name="DEBUG" select="1"/><!-- set to 1 to show debug info in console -->
	<xsl:param name="DEBUGIMAGES"/><!-- set to 1 to show debug info in console -->
	<xsl:param name="DEBUG_cssfile"/><!-- set to 1 for additional info in css -->

	<xsl:param name="outputModeCustomXSL"/><!-- specify name of XSL to use for output view mode -->
	<xsl:param name="XSLCSSPath"/><!--  a path or empty for output directory -->


	<xsl:param name="pageJPEGScale" select="1.0"/><!--  either the size of the page JPEG in percent without '%' char like 0.6 or 1.25
											if not spezified this is 1.0 or 100%
											or in pixels for widthxheight: like 350x400 or 350x or x400 as string
											-->
	<xsl:param name="pageJPEGQuality" select="80"/><!--  the quality the page JPEG -->
	<xsl:param name="pageJPEGdpi" select="150"/><!--  the resolution in dpi of the page JPEG -->
	<xsl:param name="pageJPEGParams" /><!--  additional params for IM's convert -->
	<xsl:param name="pageJPEGfinish" /><!--  page JPEG finish params -->

	<xsl:param name="pageJPEGcopy" select="1"/><!-- create a copy of the original page image (usually a TIFF) as JPEG -->
	<xsl:param name="pageJPEGcopyQuality" select="80"/><!--  the quality the page JPEG copy -->
	<xsl:param name="pageJPEGcopydpi" select="150"/><!--  the resolution in dpi of the page JPEG -->

	<!-- ********** ePaper capable or not
				    ePaperCapable flag enables/disbales all other flags -->
	<xsl:param name="ePaperCapable" select="0"/><!-- 1 = create everything to be ePaper capable. Otherwise such elements will not be included -->
	<xsl:param name="pageJPEGcreate" select="1"/><!-- 1 = create page JPEG element -->
	<xsl:param name="pagePDFcreate" select="1"/><!--  1 = make a page PDF -->

	<xsl:param name="with_boxchain" select="1"/><!-- 1 = include boxchain element for ePaper -->
	<xsl:param name="with_area" select="1"/><!-- 1 = include area element for ePaper -->

	<xsl:param name="suppress_paragraphs_below" select="9500"/><!-- 1 = suppress paragraph if below this twix position like page numbers -->
	<!-- ********** END ePaper capable flags -->



	<xsl:param name="imageInputParam"/><!--  params when IM is opening the canvas for original image like: -density 450  
											to clear all parameters, set to imageInputParam=*none*
											or
											must be individual for different input/output image formats:
											INPUT	OUTPUT		DENSITY				TRANSPARENCY
											type	type		param				param
											
											eps		jpg			
													gif								-background none -transparent black
													png			-density 300		-background none -transparent black
													tif			-density 300		-background none -transparent black
											pdf
											ai
											psd
											tif
											jpg
											gif
											wmf
											...		...		enhancable for any further input/output formats
											other	other	for non specified formats

											SYNTAX: Control string of above parameters:
											INPUTtype/OUTPUTtype::DENSITYparam;TRANSPARENCYparam##INPUTtype/OUTPUTtype::DENSITYparam;TRANSPARENCYparam##...
											like:
											eps/png::-dnsity 150;-background none -transparent black##
											eps/jpg::;##
											eps/gif:;-background none -transparent black##
											eps/tif:;-background none -transparent black##
										-->
	<xsl:param name="imageTYPE" select="1"/><!-- 0 or empty = dont'export images, 1 = JPEG, 2 = GIF, 3 = PNG, 4 = TIFF -->
	<xsl:param name="imageQUALITY" select="90"/><!-- the quality for images (for JPEGs) -->
	<xsl:param name="imageDPI" select="150"/><!-- the resolution in dpi -->
	<xsl:param name="imageSCALE"/><!-- CURRENTLY NOT USED: the scale factor (1.25) TO SCALE IMAGES -->

	<!--xsl:param name="imageMAXWIDTH" select="400"/-->
	<xsl:param name="imageMAXWIDTH"/><!-- the maximum width in pixels an image should have
														empty = do not resize = original size -->

	<xsl:param name="imageRENDERHINTS" select="68"/><!-- java image render hints as addable flags
													& 1:   hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
													& 2:   hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
													& 4:   hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
													& 8:   hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
													& 16:  hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
													& 32:  hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
													& 64:  hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
													& 128: hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
													-->
	<xsl:param name="imageNUMRESIZESTEPS" select="7"/><!-- resizing in steps results in better quality. 7 = images look less jagged and more accurate -->
	<xsl:param name="imageDoROTATE" select="0"/><!-- 1 to rotate output images IMAGES -->
	<xsl:param name="imageCUT2imagebox" select="1"/><!-- 1 = cut to imag box, 0 to cut to image itself -->
	<xsl:param name="imageOutputFinishParam"/><!-- parameters like "-strip" just before the output file -->
	<xsl:param name="imageNoDensityParam" select="0"/><!-- 1 = do not set an input desity parameter, 0 = default = set for vector images only -->
	
	<xsl:param name="imagePARAMS"/><!--  additional params for IM's convert for an image like -scale 3x1 -->
	<xsl:param name="imageCopyToOutput" select="1"/><!-- 0 or "" to not to copy original image files to output folder, 1 to copy them all, 2 to copy PDF, JPEG, GIF and PNG (web formats) only -->
	<xsl:param name="imageEXCLUDE"/><!-- exlude images from export when name starts with: "excl_;555" a semicolon separated list of chars -->
	<xsl:param name="imageCROP" select="1"/><!-- set to 1 to crop images to look like cut in InDesign -->
	<xsl:param name="imagesMORE"/><!-- more images parameters -->
	<xsl:param name="imageNOCONVERT" select="0"/><!-- set to 1 to export image box but do not convert the image to JPEG -->
	<xsl:param name="imagePreviewUseOriginals" select="0"/><!-- set to 1 to not convert images if created from internal preview. instead use the original image file -->


	<xsl:param name="CONVERTFILENAMES" select="0"></xsl:param><!-- combinable flags string  [1][2][1-w][2-w]
														0 = don't convert
														1 = make composed unicode NFC (Windows)
														2 = convert Params To NativeCharset 
														3 = make decomposed unicode NFD (OSX)
														optionally add -w = not on windows, -x = not on OSX
														examples:
														make composed unicode but not for windows: 1-w 
														make composed unicode but not for windows, on windows make native : 1-w2-x -->
	<xsl:param name="magnifyingGlass" select="0"/><!-- 0 to suppress view mode with magnifying glass, 1 allow -->
	<xsl:param name="pageCopyOriginal" select="0"/><!--  1 to copy the original page image to output folder, otherwise: don't copy -->

	<xsl:param name="extractImageMetaData" select="0"/><!--  1 to extract images metadata, otherwise: don't -->
	<xsl:param name="fullImageMetaData" select="0"/><!--  1 to additionally include the entire xmpmeta xml string, otherwise: title subject and description only -->
	<xsl:param name="typeImageMetaData" select="concat('&#45;&#45;','metadata')"/><!--  tika parameters (long or short version) comma separated preceeded by minus sign(s): &min;&min;metadata,&min;&min;xmp -->


	<xsl:output method="xml" />
	<xsl:output media-type="text/xml"/>
	<xsl:output omit-xml-declaration="no"/>

	<xsl:output indent="yes"/>
	<xsl:output encoding="UTF-8"/>

	<xsl:variable name="DEBUG_textoverflow" select="0"/>
	<xsl:variable name="move_textoverflow" select="0"/><!-- try to move text which seems to be out of region to the next region 
															*********** turn this off when plain text is copied into the OmniPage document!
															*********** In all such cases one can not rely on word positions!!!!!!!!!
														-->
	<xsl:variable name="distortion" select="100"/><!-- value do take in count that the text under a region may be distorted and therefore out of region -->

	<!-- ************************************************************************
		 SETTINGS
		 ========================= -->

	<xsl:variable name="the_OUTPUT_PATH">
		<xsl:choose>
			<xsl:when test="$OUTPUT_PATH = ''"><xsl:value-of select="$INPUT_PATH"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$OUTPUT_PATH"/></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>


	<xsl:variable name="XSLT_VERSION">1</xsl:variable><!-- the version of this transform -->
	<xsl:variable name="XSLT_VERSIONDATE">2011811</xsl:variable><!-- the version date of this transform -->

		<!-- the original document's name -->
	<xsl:variable name="docname" select="locjs:store_docname('',string($INPUT_NAME))"/>

	<xsl:variable name="the_OUTPUT_PATH">
		<xsl:choose>
			<xsl:when test="$OUTPUT_PATH = ''"><xsl:value-of select="$INPUT_PATH"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$OUTPUT_PATH"/></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>


	<!-- ========= Image converter settings -->
			<!-- for IM before 6.3.1 set to 32-w = 3:make composed unicode and 2:convertParamsToNativeCharset but not on windows may be [1][2][3][1-w][2-w][3-w][1-x][2-x][3-x] -->
			<!-- for IM after including 6.3.2 set to 3-w = 1:make composed unicode and but not on windows -->
	<xsl:variable name="filenameConversion">
		<xsl:choose>
			<xsl:when test="$CONVERTFILENAMES = '0'"></xsl:when>
			<xsl:when test="$CONVERTFILENAMES != ''"><xsl:value-of select="$CONVERTFILENAMES"/></xsl:when>
			<xsl:when test="locjs:compareVersionStrings(string($IM_VERSION_NUM),'6.3.1') &gt;= 0">3-w</xsl:when><!-- version newer or equal to 6.3.1 do NOT need conversion to local filename encoding -->
			<xsl:otherwise>3-w</xsl:otherwise><!-- make decomposed NFD for Mac only -->
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="ImageMagickConvert"><!--  use ImageMagick's convert command line tool to convert images -->
		<xsl:choose>
			<xsl:when test="$IM_PGM_PATH != ''"><xsl:value-of select="$IM_PGM_PATH"/></xsl:when>
			<xsl:otherwise></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="ImageMagickConvertEnvir"><!--  set path to GhostScript to convert EPS -->
		<xsl:choose>
			<xsl:when test="$IM_ENVIR != ''"><xsl:value-of select="$IM_ENVIR"/></xsl:when>
			<xsl:otherwise></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="IMtype">
		<xsl:choose>
			<xsl:when test="$imageTYPE = ''"></xsl:when>
			<xsl:when test="$imageTYPE = '0'"></xsl:when>
			<xsl:when test="$imageTYPE = '1'">.jpg</xsl:when>
			<xsl:when test="$imageTYPE = '2'">.gif</xsl:when>
			<xsl:when test="$imageTYPE = '3'">.png</xsl:when>
			<xsl:when test="$imageTYPE = '4'">.tif</xsl:when>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="IMquality"><!--  normal images quality (for JPEG) -->
		<xsl:choose>
			<xsl:when test="$imageQUALITY != ''">-quality <xsl:value-of select="$imageQUALITY"/></xsl:when>
			<xsl:otherwise></xsl:otherwise><!-- evtl given in imagePARAMS -->
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="IMPageJPEGquality">-quality <xsl:choose><xsl:when test="$pageJPEGQuality != ''"><xsl:value-of select="$pageJPEGQuality"/></xsl:when><xsl:otherwise>70</xsl:otherwise></xsl:choose></xsl:variable><!--  page JPEG quality -->
	<xsl:variable name="IMPageJPEGdpi">-resample <xsl:choose><xsl:when test="$pageJPEGdpi != ''"><xsl:value-of select="$pageJPEGdpi"/>x<xsl:value-of select="$pageJPEGdpi"/></xsl:when><xsl:otherwise>150x150</xsl:otherwise></xsl:choose></xsl:variable><!--  page JPEG dpi -->


	<!-- the install path of the folder XSLCSS -->
	<xsl:variable name="XSLCSSfoldername">XSLCSS</xsl:variable><!--  the source folder to copy from the application path -->
	<xsl:variable name="XSLCSSSourceFolderpath"><xsl:value-of select="$USER_DIR"/>/XSL/<xsl:value-of select="$XSLCSSfoldername"/></xsl:variable><!--  the source folder path seen from application dir -->
	<xsl:variable name="XSLCSSRelativePath"><!--  the relative path to XSL -->
		<xsl:choose>
		<xsl:when test="$XSLCSSPath = ''">../<xsl:value-of select="$XSLCSSfoldername"/>/</xsl:when>
		<xsl:otherwise><xsl:value-of select="$XSLCSSPath"/></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>

	<xsl:variable name="XSLCSSInstallPathnameRel"><xsl:value-of select="$the_OUTPUT_PATH"/><xsl:value-of select="$XSLCSSRelativePath"/></xsl:variable><!--  the relative target path -->
	<xsl:variable name="XSLCSSInstallPathname" select="myutils:resolveRelativePath(string($XSLCSSInstallPathnameRel))"/><!--  the target path -->

<!-- ================ HERE WE GO ================ -->
	<xsl:template match="/">


		<xsl:variable name="m">&#10;+++ Transforming OmniPage OCR XML '<xsl:value-of select="$INPUT_NAME" />' to XML&#10;</xsl:variable>
		<xsl:variable name="d" select="myutils:showMess(string($m),'1')"/>

		<!-- include the desired xsl to view in browser in a certain mode? -->
		<xsl:choose>
			<xsl:when test="$outputModeCustomXSL != ''"><!-- special result XSL specified -->
				<xsl:variable name="viewEpaper_xsl"><xsl:value-of select="$outputModeCustomXSL"/></xsl:variable><!--  the xsl to use to transform to html in browser -->
				<xsl:variable name="fp"><xsl:value-of select="$XSLCSSInstallPathname"/><xsl:value-of select="$viewEpaper_xsl"/></xsl:variable>
				<xsl:variable name="exists" select="locjs:fileExists(string($fp))"/>
				<xsl:if test="$exists = false">
					<xsl:variable name="m1">&#10;   ++ Writing Browser XSL for flipping book view: <xsl:value-of select="$XSLCSSRelativePath"/></xsl:variable>
					<xsl:variable name="d1" select="myutils:showMess(string($m1),'1')"/>
					<xsl:variable name="created" select="myutils:copyFolderPath($XSLCSSSourceFolderpath, '', $XSLCSSInstallPathname, '', true(), true(), '#### XSLCSS Install error ')"/>
				</xsl:if>
				<xsl:text disable-output-escaping="yes">&lt;?xml-stylesheet href="</xsl:text><xsl:value-of select="locjs:encode_URI(concat($XSLCSSRelativePath,$viewEpaper_xsl))"/><xsl:text disable-output-escaping="yes">" type="text/xsl"?&gt;&#10;</xsl:text>
			</xsl:when>
			<xsl:otherwise><!-- XML tree: do not add an XSL: view as a XML tree -->
			</xsl:otherwise>
		</xsl:choose>

		<indd_document>

			<INPUT_PATH><xsl:value-of select="$INPUT_PATH"/></INPUT_PATH>
			<OUTPUT_PATH><xsl:value-of select="$OUTPUT_PATH"/></OUTPUT_PATH>
	
			<xsl:comment> **** calling XSL parameters:  **** </xsl:comment>
			<call_parameters>
				<par name="XSLCSSPath"><xsl:value-of select="$XSLCSSPath"/></par>
				<par name="CSSpath"><xsl:value-of select="$XSLCSSPath"/></par>
				<par name="DEBUG">0</par>
				<par name="metaViewportContent">initial-scale=0.57, width=device-width, user-scalable=yes</par>
				<par name="websiteParams">logoName=*#*logoURL=*#*logoTitle=*#*logoURLtarget=</par>
				<par name="fontsizeUnits">0</par>
				<par name="fontsizeBase">9</par>
				<par name="magnifyingGlass">0</par>
				<par name="characterAttribsSuppress">0</par>
				<par name="suppressSiteElements">0</par>
				<par name="suppressExportMouseOvers">8</par>
				<par name="excludeNotes">1</par>
				<par name="TABLE_CELLS_WIDTH_PRESERVE">1.3</par>
				<par name="TABLE_BORDER_COLLAPSE">1</par>
				<par name="TABLE_AS_BLOCK">0</par>
				<par name="preserveControlCharacters">0</par>
			</call_parameters>
	
			<xsl:comment> **** GENERAL INFO:  **** </xsl:comment>
			<doctypeinfos encoding="UTF-8">
				<xsl:attribute name="xslbasepath"><xsl:value-of select="$XSLCSSPath"/></xsl:attribute>
				<xsl:attribute name="xslpath"><xsl:value-of select="$XSLCSSPath"/>slidebook.xsl</xsl:attribute>
				<xsl:attribute name="csspath"><xsl:value-of select="$XSLCSSPath"/>omnidocs.css</xsl:attribute>
			</doctypeinfos>
	
	
			<header type="layout">
				<headerfield name="creationDate"></headerfield>
				<headerfield name="creationTime"></headerfield>
				<headerfield name="OCRversion"><xsl:value-of select="/ssdoc:document/@ocr-vers" /></headerfield>
				<headerfield name="filename"><xsl:value-of select="$INPUT_NAME" /></headerfield>
				<headerfield name="inputPath"><xsl:value-of select="$INPUT_PATH" /></headerfield>
				<headerfield name="inputSubPath"><xsl:value-of select="$INPUT_SUB_PATH" /></headerfield>
			</header>
			<xsl:comment> **** GENERAL INFO: END **** </xsl:comment>
	
	
			<xsl:apply-templates select="ssdoc:document" />
		</indd_document>

		<!-- ======== make a directory index file ========= -->
		<xsl:variable name="issuedate" select="locjs:getIssueDate()"/>
		<xsl:variable name="issuenumber" select="locjs:getIssueNumberOrig()"/>
		<xsl:variable name="dirindexfile" select="concat($OUTPUT_PATH,locjs:getIssueShortcut(),'_',$issuedate,'_',$issuenumber,'.idx')"/>
		<xsl:variable name="d" select="myutils:deleteFile(string($dirindexfile))"/>
		<xsl:variable name="d"><xsl:value-of select="myutils:directoryListCreate(string($OUTPUT_PATH), '', true())"/></xsl:variable>
		<xsl:variable name="d"><xsl:value-of select="myutils:directoryListWriteXMLFile(string($dirindexfile), true())"/></xsl:variable>

		<xsl:variable name="d" select="myutils:showMess('+++ Transforming OmniPage OCR XML complete.&#10;','1')"/>
	</xsl:template>

	<xsl:template match="ssdoc:document">
		<xsl:variable name="d" select="locjs:init_pageIndex()"/>

		<xsl:for-each select="ssdoc:page">
			<xsl:variable name="pageindex" select="locjs:next_pageIndex()"/>
			<xsl:call-template name="get_page_coords" />
			<xsl:variable name="d" select="locjs:set_pageJPEGScale(string($pageJPEGScale))"/>

			<xsl:variable name="pagewidth" select="locjs:get_pagewidth()"/>
			<xsl:variable name="pageheight" select="locjs:get_pageheight()"/>
			<xsl:variable name="pageimagename" select="locjs:getNamePart(string(@filename))"/>
			<xsl:variable name="d" select="locjs:store_pageimageName(string($pageimagename))"/>
			<xsl:variable name="d" select="locjs:store_issueParameters(string($pageimagename))"/>
			<xsl:variable name="pgnbr" select="locjs:getNewPageSequence()" />
			<xsl:variable name="pgsect" select="locjs:getPageSection()" />
			<xsl:variable name="pgsectorig" select="locjs:getPageSectionOrig()" />


			<page>
				<xsl:if test="$pgnbr != ''"><xsl:attribute name="page_sequence"><xsl:value-of select="$pgnbr" /></xsl:attribute></xsl:if>
				<xsl:if test="$pgsect != ''"><xsl:attribute name="page_name"><xsl:value-of select="$pgsect" /></xsl:attribute></xsl:if>
				<xsl:if test="$pgsectorig != ''"><xsl:attribute name="page_nameorig"><xsl:value-of select="$pgsectorig" /></xsl:attribute></xsl:if>
				<xsl:attribute name="issueshortcut"><xsl:value-of select="locjs:getIssueShortcut()" /></xsl:attribute>
				<xsl:if test="locjs:getIssueDate() != ''"><xsl:attribute name="issuedate"><xsl:value-of select="locjs:getIssueDate()" /></xsl:attribute></xsl:if>
				<xsl:if test="locjs:getIssueNumber() != ''"><xsl:attribute name="issue_number"><xsl:value-of select="locjs:getIssueNumber()" /></xsl:attribute></xsl:if>
				<xsl:if test="locjs:getIssueNumberOrig() != ''"><xsl:attribute name="issue_numberorig"><xsl:value-of select="locjs:getIssueNumberOrig()" /></xsl:attribute></xsl:if>

				<xsl:if test="$INPUT_NAME != ''"><xsl:attribute name="orig_pageXML"><xsl:value-of select="$INPUT_NAME" /></xsl:attribute></xsl:if>
				<xsl:if test="@filename/. != ''"><xsl:attribute name="orig_pageImagePathName"><xsl:value-of select="@filename/." /></xsl:attribute></xsl:if>
				<xsl:if test="$pageimagename != ''"><xsl:attribute name="orig_pageImageName"><xsl:value-of select="$pageimagename" /></xsl:attribute></xsl:if>
				<xsl:if test="@language/. != ''"><xsl:attribute name="orig_pageLanguage"><xsl:value-of select="@language/." /></xsl:attribute></xsl:if>
				<xsl:if test="@orientation/. != ''"><xsl:attribute name="orig_pageOrientation"><xsl:value-of select="@orientation/." /></xsl:attribute></xsl:if>
				<xsl:if test="@width/. != ''"><xsl:attribute name="orig_pageWidth"><xsl:value-of select="@width/." /></xsl:attribute></xsl:if>
				<xsl:if test="@height/. != ''"><xsl:attribute name="orig_pageHeight"><xsl:value-of select="@height/." /></xsl:attribute></xsl:if>
				<xsl:if test="@x-res/. != ''"><xsl:attribute name="orig_pageXres"><xsl:value-of select="@x-res/." /></xsl:attribute></xsl:if>
				<xsl:if test="@y-res/. != ''"><xsl:attribute name="orig_pageYres"><xsl:value-of select="@y-res/." /></xsl:attribute></xsl:if>
				<xsl:if test="@skew/. != ''"><xsl:attribute name="orig_pageSkew"><xsl:value-of select="@skew/." /></xsl:attribute></xsl:if>
				<xsl:if test="@bpp/. != ''"><xsl:attribute name="orig_bpp"><xsl:value-of select="@bpp/." /></xsl:attribute></xsl:if>

				<xsl:variable name="d" select="myutils:showMess(concat('--- Extracting content of page #',$pgnbr,'&#10;'),'1')"/>

				<xsl:if test="$ePaperCapable != '0'">
					<!-- get the JPEG of this page -->
					<xsl:if test="$pageJPEGcreate = '1'">
						<xsl:call-template name="get_pageJPEG">
							<xsl:with-param name="thepage"><xsl:value-of select="$pgnbr"/></xsl:with-param>
							<xsl:with-param name="page_sequence"><xsl:value-of select="$pgnbr"/></xsl:with-param>
							<xsl:with-param name="page_name"><xsl:value-of select="$pgsect"/></xsl:with-param>
						</xsl:call-template>
					</xsl:if>
	
					<!-- evtl create a PDf of this page -->
					<xsl:if test="$pagePDFcreate = 1">
						<xsl:variable name="pagePDFname" select="concat(locjs:getDocumentBaseName(),'.pdf')"/>
						<xsl:variable name="pagePDFPathName" select="concat($INPUT_PATH,$pagePDFname)"/>
						<xsl:variable name="exists" select="locjs:fileExists(string($pagePDFPathName))"/>
		
						<pagePDF>
							<xsl:attribute name="name"><xsl:value-of select="$pagePDFname"/></xsl:attribute>
							<xsl:attribute name="page_sequence"><xsl:value-of select="$pgnbr"/></xsl:attribute>
							<xsl:attribute name="page_name"><xsl:value-of select="$pgsect"/></xsl:attribute>
							<xsl:attribute name="exists"><xsl:value-of select="$exists"/></xsl:attribute>

							<xsl:value-of select="$pagePDFname"/>
						</pagePDF>
						<xsl:choose>
							<xsl:when test="$exists = true()">
								<xsl:variable name="d1" select="myutils:showMess(concat('      - Copying page PDF: ',$pagePDFname,'&#10;'),'1')"/>
								<xsl:variable name="d" select="myutils:copyFile(string($INPUT_PATH),string($pagePDFname),string($OUTPUT_PATH),string($pagePDFname),1,false)"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:variable name="d1" select="myutils:showMess(concat('    ## Missing page PDF: ',$pagePDFPathName),'1')"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:if>
				</xsl:if>
	
				<xsl:comment> **** ARTICLES ON PAGE #<xsl:value-of select="$pgnbr"/>: **** </xsl:comment>
				<articles>
					<xsl:attribute name="page_sequence"><xsl:value-of select="$pgnbr"/></xsl:attribute>
					<xsl:apply-templates select="ssdoc:region" >
						<xsl:sort select="ssdoc:rc/@t" data-type="number" order="ascending"/>
						<xsl:with-param name="thepage"><xsl:value-of select="$pgnbr"/></xsl:with-param>
						<xsl:with-param name="thepagesect"><xsl:value-of select="$pgsect"/></xsl:with-param>
					</xsl:apply-templates>
				</articles>

			</page>
		</xsl:for-each>

	</xsl:template>



	<xsl:template name="get_page_coords"><!-- get page size -->
		<xsl:variable name="d" select="locjs:store_pageimageDPIx(string(@x-res))"/><!-- store DPI BEFORE page sizes -->
		<xsl:variable name="d" select="locjs:store_pageimageDPIy(string(@y-res))"/>
		<xsl:variable name="d" select="locjs:store_pageimageSIZEx(string(@width))"/>
		<xsl:variable name="d" select="locjs:store_pageimageSIZEy(string(@height))"/>
		<xsl:variable name="d" select="locjs:store_pagebounds()"/>
	</xsl:template>


	<xsl:template match="ssdoc:region"><!-- get regions = article containers -->
		<xsl:param name="thepage"/>
		<xsl:param name="thepagesect"/>
		<xsl:comment> **** ARTICLE '<xsl:value-of select="@idx"/>' PAGE #<xsl:value-of select="$thepage"/>:  **** </xsl:comment>

		<xsl:variable name="l" select="locjs:twip2px(string(ssdoc:rc/@l))"/>
		<xsl:variable name="t" select="locjs:twip2px(string(ssdoc:rc/@t))"/>
		<xsl:variable name="r" select="locjs:twip2px(string(ssdoc:rc/@r))"/>
		<xsl:variable name="b" select="locjs:twip2px(string(ssdoc:rc/@b))"/>
		<xsl:variable name="ls" select="$l * locjs:get_pageJPEGScale()"/>
		<xsl:variable name="ts" select="$t * locjs:get_pageJPEGScale()"/>
		<xsl:variable name="rs" select="$r * locjs:get_pageJPEGScale()"/>
		<xsl:variable name="bs" select="$b * locjs:get_pageJPEGScale()"/>
		<article>
			<xsl:attribute name="idx"><xsl:value-of select="locjs:getArticleIdx()"/></xsl:attribute>
			<xsl:attribute name="page_sequence"><xsl:value-of select="$thepage"/></xsl:attribute>
			<xsl:attribute name="page_name"><xsl:value-of select="$thepagesect"/></xsl:attribute>
			<xsl:attribute name="l"><xsl:value-of select="$ls"/></xsl:attribute>
			<xsl:attribute name="t"><xsl:value-of select="$ts"/></xsl:attribute>
			<xsl:attribute name="r"><xsl:value-of select="$rs"/></xsl:attribute>
			<xsl:attribute name="b"><xsl:value-of select="$bs"/></xsl:attribute>
			<xsl:attribute name="ls"><xsl:value-of select="$ls"/></xsl:attribute>
			<xsl:attribute name="ts"><xsl:value-of select="$ts"/></xsl:attribute>
			<xsl:attribute name="rs"><xsl:value-of select="$rs"/></xsl:attribute>
			<xsl:attribute name="bs"><xsl:value-of select="$bs"/></xsl:attribute>
			<xsl:attribute name="coords"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/></xsl:attribute>

			<xsl:if test="$ePaperCapable != '0'">
				<xsl:if test="$with_boxchain != '0'">
					<boxchain>
						<box type="text" elementName="TextFrame" cont="text" colwidth="{$r - $l}" spread="1" page_sequence="{$thepage}" page_name="{$thepagesect}" groupid="" allgroupid="" anchorid="" objstyle="ObjectStyle/$ID/[None]" chainidx="0" previousflowid="n" nextflowid="n" >
							<xsl:choose>
								<xsl:when test="@reg-type='graphic'"><!-- images -->
									<xsl:attribute name="type">imag</xsl:attribute>
									<xsl:attribute name="elementName">Rectangle</xsl:attribute>
									<xsl:attribute name="cont">imag</xsl:attribute>
								</xsl:when>
								<xsl:otherwise><!-- like @reg-type='table' or @reg-type='horizontal' -->
									<xsl:attribute name="type">text</xsl:attribute>
									<xsl:attribute name="elementName">TextFrame</xsl:attribute>
									<xsl:attribute name="cont">text</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:attribute name="Self"><xsl:value-of select="locjs:getNewBoxSelf()" /></xsl:attribute>
							<xsl:attribute name="textflowid"><xsl:value-of select="locjs:getNewTextflowid()" /></xsl:attribute>
							<xsl:attribute name="coords"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/></xsl:attribute>
							<xsl:attribute name="coords_orig"><xsl:value-of select="$l"/>,<xsl:value-of select="$t"/>,<xsl:value-of select="$r"/>,<xsl:value-of select="$b"/></xsl:attribute>
							<xsl:attribute name="bbox"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/></xsl:attribute>
							<xsl:attribute name="bbox_orig"><xsl:value-of select="$l"/>,<xsl:value-of select="$t"/>,<xsl:value-of select="$r"/>,<xsl:value-of select="$b"/></xsl:attribute>
							<xsl:attribute name="shape"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/>,<xsl:value-of select="$ls"/>,<xsl:value-of select="$bs"/></xsl:attribute>
							<xsl:attribute name="shape_orig"><xsl:value-of select="$l"/>,<xsl:value-of select="$t"/>,<xsl:value-of select="$r"/>,<xsl:value-of select="$t"/>,<xsl:value-of select="$r"/>,<xsl:value-of select="$b"/>,<xsl:value-of select="$l"/>,<xsl:value-of select="$b"/></xsl:attribute>
							<xsl:attribute name="pageJPEGScale"><xsl:value-of select ="locjs:get_pageJPEGScale()" /></xsl:attribute>
							<xsl:attribute name="x1"><xsl:value-of select="$ls"/></xsl:attribute>
							<xsl:attribute name="y1"><xsl:value-of select="$ts"/></xsl:attribute>
							<xsl:attribute name="x2"><xsl:value-of select="$rs"/></xsl:attribute>
							<xsl:attribute name="y2"><xsl:value-of select="$bs"/></xsl:attribute>
							<xsl:attribute name="backgroundColor"></xsl:attribute>
						</box>
					</boxchain>
				</xsl:if>

				<xsl:if test="$with_area != '0'">
					<area>
						<xsl:attribute name="name"><xsl:value-of select="concat('A',locjs:getBoxSelf(),'_',locjs:getPageSequence())"/></xsl:attribute>
						<xsl:attribute name="coords"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/></xsl:attribute>
						<xsl:attribute name="shape"><xsl:value-of select="$ls"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$ts"/>,<xsl:value-of select="$rs"/>,<xsl:value-of select="$bs"/>,<xsl:value-of select="$ls"/>,<xsl:value-of select="$bs"/></xsl:attribute>
						<xsl:attribute name="x1"><xsl:value-of select="$ls"/></xsl:attribute>
						<xsl:attribute name="y1"><xsl:value-of select="$ts"/></xsl:attribute>
						<xsl:attribute name="x2"><xsl:value-of select="$rs"/></xsl:attribute>
						<xsl:attribute name="y2"><xsl:value-of select="$bs"/></xsl:attribute>
					</area>
				</xsl:if>
			</xsl:if>
			
			<xsl:choose>
				<!-- images -->
				<xsl:when test="@reg-type='graphic'">
					<content type="image" groupid="">
						<xsl:attribute name="Self"><xsl:value-of select="locjs:getTextflowid()" /></xsl:attribute>
						<xsl:attribute name="id"><xsl:value-of select="locjs:getTextflowid()" /></xsl:attribute>

						<xsl:variable name="source_imagename"><xsl:value-of select="locjs:getNextImageName()"/></xsl:variable>
						<xsl:variable name="d1" select="myutils:showMess(concat('     -- Handling Image: ',$source_imagename,'&#10;'),'1')"/>
						<xsl:variable name="source_imagepathname"><xsl:value-of select="concat($INPUT_PATH,$source_imagename)"/></xsl:variable>
						<xsl:variable name="exists" select="locjs:fileExists(string($source_imagepathname))"/>

						<!-- resize the original image image -->
						<xsl:variable name="source_imagename_resized">
							<xsl:choose>
								<xsl:when test="$imageMAXWIDTH != ''">
									<xsl:variable name="tmp_source_imagename_resized"><xsl:value-of select="locjs:create_new_name(string($source_imagename),string($imageMAXWIDTH))"/></xsl:variable>
									<!-- resize it -->
									<xsl:variable name="source_imagepathname_resized"><xsl:value-of select="concat($OUTPUT_PATH,$tmp_source_imagename_resized)"/></xsl:variable>
									<xsl:variable name="d1" select="myutils:showMess(concat('      - Resizing Image to width ',$imageMAXWIDTH,': ',$source_imagename,' to ',$tmp_source_imagename_resized,': '),'1')"/>
									<xsl:variable name="resizeOK" select="myutils:resize_jpeg_maxwidth(string($source_imagepathname), string($source_imagepathname_resized), number($imageMAXWIDTH), number($imageQUALITY), number($imageRENDERHINTS), number($imageNUMRESIZESTEPS), number($imageDPI))"/>
									<xsl:variable name="exists" select="locjs:fileExists(string($source_imagepathname_resized))"/>
									<xsl:variable name="m1"><xsl:choose><xsl:when test="$exists='true'">OK&#10;</xsl:when><xsl:otherwise>ERROR: <xsl:value-of select="$resizeOK"/>&#10;</xsl:otherwise></xsl:choose></xsl:variable>
									<xsl:variable name="d1" select="myutils:showMess(string($m1),'1')"/>
									<xsl:choose>
										<xsl:when test="$resizeOK = 0">
											<xsl:value-of select="$tmp_source_imagename_resized"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="concat('####ERROR RESIZE ',$resizeOK)"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="$source_imagename"/>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:variable>
						<!-- ****** this is the smaller image -->
						<img>
							<xsl:attribute name="src"><xsl:value-of select="$source_imagename_resized" /></xsl:attribute>
							<xsl:attribute name="alt"><xsl:value-of select="$source_imagename_resized" /></xsl:attribute>
							<xsl:if test="$exists != true()">
								<xsl:attribute name="exists"><xsl:value-of select="$exists"/></xsl:attribute>
							</xsl:if>
							<!-- Do we want meta data have extracted? -->
							<xsl:if test="($exists = true()) and ($extractImageMetaData != '0')">
								<xsl:variable name="d1" select="myutils:showMess(concat('      - Extracting Meta Data from file: ',$source_imagepathname,': '),'1')"/>
								<!-- Apache Tika Options:
									WHERE == should be two minus chars
											-?  or ==help          Print this usage message
											-v  or ==verbose       Print debug level messages
											-V  or ==version       Print the Apache Tika version number
										
											-g  or ==gui           Start the Apache Tika GUI
											-s  or ==server        Start the Apache Tika server
											-f  or ==fork          Use Fork Mode for out-of-process extraction
										
											-x  or ==xml           Output XHTML content (default)
											-h  or ==html          Output HTML content
											-t  or ==text          Output plain text content
											-T  or ==text-main     Output plain text content (main content only)
											-m  or ==metadata      Output only metadata
											-j  or ==json          Output metadata in JSON
											-y  or ==xmp           Output metadata in XMP
											-l  or ==language      Output only language
											-d  or ==detect        Detect document type
											-eX or ==encoding=X    Use output encoding X
											-pX or ==password=X    Use document password X
											-z  or ==extract       Extract all attachements into current directory
											==extract-dir=<dir>    Specify target directory for -z
											-r  or ==pretty-print  For XML and XHTML outputs, adds newlines and whitespace, for better readability
										
											==create-profile=X
												 Create NGram profile, where X is a profile name
											==list-parsers
												 List the available document parsers
											==list-parser-details
												 List the available document parsers, and their supported mime types
											==list-detectors
												 List the available document detectors
											==list-met-models
												 List the available metadata models, and their supported keys
											==list-supported-types
												 List all known media types and related information
										
										Description:
											Apache Tika will parse the file(s) specified on the
											command line and output the extracted text content
											or metadata to standard output.
											Instead of a file name you can also specify the URL
											of a document to be parsed.
											If no file name or URL is specified (or the special
											name "-" is used), then the standard input stream
											is parsed. If no arguments were given and no input
											data is available, the GUI is started instead.
										
										- GUI mode
											Use the "==gui" (or "-g") option to start the
											Apache Tika GUI. You can drag and drop files from
											a normal file explorer to the GUI window to extract
											text content and metadata from the files.
										
										- Server mode
											Use the "==server" (or "-s") option to start the
											Apache Tika server. The server will listen to the
											ports you specify as one or more arguments.
								-->
								<xsl:variable name="imageMetaOK" select="myutils:extractMetaData(concat('--metadata',',','--xmp',',','--encoding=utf-8',',',$source_imagepathname))"/><!-- make comma delimited string -->
								<xsl:variable name="imageMetaError" select="myutils:getMetaExtractorError()"/>
								<xsl:variable name="imageMeta">
									<xsl:if test="$imageMetaOK = 0">
										<xsl:value-of disable-output-escaping="yes" select="myutils:getMetaData()"/>
									</xsl:if>
								</xsl:variable>
								<!--
									<xsl:variable name="d" select="locjs:message(concat('*** getting Image Meta from file: ',$source_imagepathname))"/>
									<xsl:variable name="d" select="locjs:message(concat('### Meta error return code: ',$imageMetaOK))"/>
									<xsl:variable name="d" select="locjs:message(concat('### Meta error text: ',$imageMetaError))"/>
									<xsl:variable name="d" select="locjs:message($imageMeta)"/>
								-->
								<xsl:choose>
									<xsl:when test="$imageMetaOK = 0">
										<xsl:variable name="d1" select="myutils:showMess(concat('          OK','&#10;'),'1')"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:variable name="d1" select="myutils:showMess(concat('#### ERROR ',$imageMetaOK,' while extracing meta data','&#10;'),'1')"/>
										<xsl:variable name="d1" select="myutils:showMess(concat('#### ERROR Message: ',$imageMetaError,'&#10;'),'1')"/>
									</xsl:otherwise>
								</xsl:choose>
								<xsl:if test="$imageMeta != ''">
									<metadata>
										<xsl:if test="$fullImageMetaData != '0'"><!-- add entire meta data -->
											<metadatafull><xsl:copy-of select="$imageMeta"/></metadatafull>
										</xsl:if>
										<xsl:choose>
											<xsl:when test="starts-with($imageMeta,'&lt;x:xmpmeta') = 'true'"><!-- xmp XML -->
												<xsl:variable name="xmpmeta" select="myutils:parseStringToDom(string($imageMeta))"/>
												<title><xsl:value-of select="$xmpmeta//descendant::dc:title/rdf:Alt/rdf:li/." /></title>
												<description><xsl:value-of select="$xmpmeta//descendant::dc:description/rdf:Alt/rdf:li/." /></description>
												<subject>
													<xsl:for-each select="$xmpmeta//descendant::dc:subject/rdf:Bag/rdf:li[not (. = preceding::rdf:li/.)]">
														<xsl:sort select="." />
															<xsl:if test="position() != 1"><xsl:text>,</xsl:text></xsl:if><xsl:value-of select="." />
													</xsl:for-each>
												</subject>
											</xsl:when>
											<xsl:when test="starts-with($imageMeta,'{ ') = 'true'"><!-- json associative array -->
												<xsl:if test="$fullImageMetaData = '0'"><!-- add entire meta data in json -->
													<metadatafull><xsl:copy-of select="$imageMeta"/></metadatafull>
												</xsl:if>
											</xsl:when>
											<xsl:otherwise><!-- plain text lines -->
												<title><xsl:value-of select="locjs:parseMetaValue(string($imageMeta),'dc:title: ')" /></title>
												<description><xsl:value-of select="locjs:parseMetaValue(string($imageMeta),'dc:description: ')" /></description>
												<subject><xsl:value-of select="locjs:parseMetaValue(string($imageMeta),'dc:subject: ')" /></subject>
												<author><xsl:value-of select="locjs:parseMetaValue(string($imageMeta),'Author: ')" /></author>
												<copyright><xsl:value-of select="locjs:parseMetaValue(string($imageMeta),'Copyright: ')" /></copyright>
											</xsl:otherwise>
										</xsl:choose>
									</metadata>
								</xsl:if>
							</xsl:if>
						</img>
						<!-- ****** this is the large original image which is shown on click -->
						<xsl:if test="$imageMAXWIDTH != ''">
							<img2>
								<xsl:attribute name="src"><xsl:value-of select="$source_imagename" /></xsl:attribute>
								<xsl:attribute name="alt"><xsl:value-of select="$source_imagename" /></xsl:attribute>
								<xsl:if test="$exists != true()">
									<xsl:attribute name="exists"><xsl:value-of select="$exists"/></xsl:attribute>
								</xsl:if>
							</img2>
						</xsl:if>

						<!-- copy the image file -->
						<xsl:choose>
							<xsl:when test="($exists = true()) and ($imageCopyToOutput != '0') and ($source_imagename != '')">
								<xsl:choose>
									<!-- if input and output paths are the same: nothing to do -->
									<xsl:when test="($INPUT_PATH = $the_OUTPUT_PATH)">
									</xsl:when>
									<!-- copy web formats only -->
									<xsl:when test="$imageCopyToOutput = '2'">
										<xsl:if test="contains(locjs:to_lower_case(string($source_imagename)),'.pdf') or contains(locjs:to_lower_case(string($source_imagename)),'.jpg') or contains(locjs:to_lower_case(string($source_imagename)),'.gif') or contains(locjs:to_lower_case(string($source_imagename)),'.png')">
											<xsl:variable name="d1" select="myutils:showMess(concat('      - Copying Image: ',$source_imagename,'&#10;'),'1')"/>
											<xsl:variable name="d" select="myutils:copyFile(string($INPUT_PATH),string($source_imagename),string($the_OUTPUT_PATH),string($source_imagename),1,false)"/>
										</xsl:if>
									</xsl:when>
									<!-- copy all -->
									<xsl:otherwise>
										<xsl:variable name="d1" select="myutils:showMess(concat('      - Copying Image: ',$source_imagename,'&#10;'),'1')"/>
										<xsl:variable name="d" select="myutils:copyFile(string($INPUT_PATH),string($source_imagename),string($the_OUTPUT_PATH),string($source_imagename),1,false)"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:when>
							<xsl:otherwise>
								<xsl:variable name="d1" select="myutils:showMess(concat('    ## Missing image: ',$source_imagepathname),'1')"/>
							</xsl:otherwise>
						</xsl:choose>
					</content>
				</xsl:when>

				<xsl:when test="@reg-type='table'">
					<content type="text" groupid="">
						<xsl:attribute name="Self"><xsl:value-of select="locjs:getTextflowid()" /></xsl:attribute>
						<xsl:attribute name="id"><xsl:value-of select="locjs:getTextflowid()" /></xsl:attribute>
						<table>
							<!-- this will select ssdoc:row within a table -->
							<xsl:apply-templates>
								<xsl:with-param name="region_is_last" select="position() = last()"/><!-- pass region container coordinates to child elements from region/rc element -->
								<xsl:with-param name="rcl" select="number(ssdoc:rc/@l)"/>
								<xsl:with-param name="rct" select="number(ssdoc:rc/@t)"/>
								<xsl:with-param name="rcr" select="number(ssdoc:rc/@r)"/>
								<xsl:with-param name="rcb" select="number(ssdoc:rc/@b)"/>
							</xsl:apply-templates>
						</table>
					</content>
				</xsl:when>
				<!-- Text like @reg-type="horizontal" -->
				<xsl:otherwise>
					<content type="text" groupid="">
						<xsl:attribute name="Self"><xsl:value-of select="locjs:getTextflowid()" /></xsl:attribute>
						<xsl:attribute name="id"><xsl:value-of select="locjs:getTextflowid()" /></xsl:attribute>
						<!-- check if we have overflowed text from previous region -->
						<xsl:if test="locjs:have_overflowtext() = true()">
							<xsl:value-of disable-output-escaping="yes" select="locjs:get_region_overflowtext()"/>
						</xsl:if>
						<!-- this will select ssdoc:paragraph within a region -->
						<xsl:apply-templates>
							<xsl:with-param name="region_is_last" select="position() = last()"/><!-- pass region container coordinates to child elements from region/rc element -->
							<xsl:with-param name="rcl" select="number(ssdoc:rc/@l)"/>
							<xsl:with-param name="rct" select="number(ssdoc:rc/@t)"/>
							<xsl:with-param name="rcr" select="number(ssdoc:rc/@r)"/>
							<xsl:with-param name="rcb" select="number(ssdoc:rc/@b)"/>
						</xsl:apply-templates>
					</content>
				</xsl:otherwise>
			</xsl:choose>
		</article>
	</xsl:template>
	
	<xsl:template match="ssdoc:row"><!-- get table row -->
		<xsl:param name="region_is_last"/><!-- true() if we are in last region of document -->
		<xsl:param name="rcl"/><!-- the region container coordinates -->
		<xsl:param name="rct"/>
		<xsl:param name="rcr"/>
		<xsl:param name="rcb"/>
		<tr>
			<!-- this will select ssdoc:cell within a table row -->
			<xsl:apply-templates>
				<xsl:with-param name="region_is_last" select="$region_is_last"/><!-- pass region container coordinates to child elements from region/rc element -->
				<xsl:with-param name="rcl" select="$rcl"/>
				<xsl:with-param name="rct" select="$rct"/>
				<xsl:with-param name="rcr" select="$rcr"/>
				<xsl:with-param name="rcb" select="$rcb"/>
			</xsl:apply-templates>
		</tr>
	</xsl:template>
	<xsl:template match="ssdoc:cell"><!-- get table row cells -->
		<xsl:param name="region_is_last"/><!-- true() if we are in last region of document -->
		<xsl:param name="rcl"/><!-- the region container coordinates -->
		<xsl:param name="rct"/>
		<xsl:param name="rcr"/>
		<xsl:param name="rcb"/>
		<td>
			<xsl:attribute name="rowspan"><xsl:value-of select="@row-span" /></xsl:attribute>
			<xsl:attribute name="colspan"><xsl:value-of select="@col-span" /></xsl:attribute>
			<xsl:variable name="cell_style">
					<xsl:text>vertical-align:top;</xsl:text>
					<xsl:if test="@border-left != '0'">border-left:1px solid black;</xsl:if>
					<xsl:if test="@border-top != '0'">border-top:1px solid black;</xsl:if>
					<xsl:if test="@border-right != '0'">border-right:1px solid black;</xsl:if>
					<xsl:if test="@border-bottom != '0'">border-bottom:1px solid black;</xsl:if>
			</xsl:variable>
			<xsl:if test="$cell_style != ''">
				<xsl:attribute name="style"><xsl:value-of select="$cell_style" /></xsl:attribute>
			</xsl:if>
			<!-- this will select ssdoc:paragraph within a cell -->
			<xsl:apply-templates>
				<xsl:with-param name="region_is_last" select="$region_is_last"/><!-- pass region container coordinates to child elements from region/rc element -->
				<xsl:with-param name="rcl" select="$rcl"/>
				<xsl:with-param name="rct" select="$rct"/>
				<xsl:with-param name="rcr" select="$rcr"/>
				<xsl:with-param name="rcb" select="$rcb"/>
			</xsl:apply-templates>
		</td>
	</xsl:template>


	<xsl:template match="ssdoc:paragraph"><!-- get paragraph -->
		<xsl:param name="region_is_last"/><!-- true() if we are in last region of document -->
		<xsl:param name="rcl"/><!-- the region container coordinates -->
		<xsl:param name="rct"/>
		<xsl:param name="rcr"/>
		<xsl:param name="rcb"/>

		<xsl:if test="($ePaperCapable != '0') or ((number($suppress_paragraphs_below) &lt; 0) or (number(descendant::ssdoc:ln/@baseline) &lt; number($suppress_paragraphs_below)))">
			<!-- calc class attribute -->
			<xsl:variable name="class">
				<xsl:value-of select="locjs:getClassAttribute(string(descendant::ssdoc:ln/@ff),string(descendant::ssdoc:ln/@fs),true())"/>
			</xsl:variable>
			<!-- store the currently set class name -->
			<xsl:variable name="docname" select="locjs:store_paragraphClassName(string($class))"/>

			<div>
				<xsl:choose>
					<xsl:when test="$class != ''">
						<xsl:attribute name="class"><xsl:value-of select="$class"/></xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="class">paragraph</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
				<!-- this will select ssdoc:ln within a paragraph -->
				<xsl:apply-templates>
					<xsl:with-param name="region_is_last" select="$region_is_last"/><!-- pass region container coordinates to child elements from region/rc element -->
					<xsl:with-param name="rcl" select="$rcl"/>
					<xsl:with-param name="rct" select="$rct"/>
					<xsl:with-param name="rcr" select="$rcr"/>
					<xsl:with-param name="rcb" select="$rcb"/>
				</xsl:apply-templates>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="ssdoc:ln"><!-- get lines -->
		<xsl:param name="region_is_last"/><!-- true() if we are in last region of document -->
		<xsl:param name="rcl"/><!-- the region container coordinates -->
		<xsl:param name="rct"/>
		<xsl:param name="rcr"/>
		<xsl:param name="rcb"/>
			<!-- test the position of the first word of this line if it is still in container region's coordinates or if it belongs to the next region.
				If we have word elemnts like &lt;wd l="2147483647" t="2147483647" r="0" b="0"&gt;Word&lt;/wd&gt;
				where left and top cords are very large (2147483647) and right/bottom are Zero, the following words seem to belong to the next region.
			 -->
			<xsl:variable name="textoverflow">
				<xsl:choose>
					<!-- should we try to move words out of region bound to the next region? 
						If $move_textoverflow != '0' and $region_is_last = false() and we are not in a table cell
						then we will try
						 -->
					<xsl:when test="$move_textoverflow = '0'"><xsl:value-of select="0"/></xsl:when>
					<xsl:when test="name(../../*) = 'cell'"><xsl:value-of select="0"/></xsl:when>
					<xsl:when test="($region_is_last = false())
									and (number(ssdoc:wd/@l) &lt; 1000000000) and (number(ssdoc:wd/@t) &lt; 1000000000) 
									and ( (number(ssdoc:wd/@l) &gt; ($rcr + number($distortion)))
											or (number(ssdoc:wd/@t) &lt; ($rct - number($distortion))) or (number(ssdoc:wd/@t) &gt; ($rcb + number($distortion)))
										)">
						<xsl:value-of select="1"/>
					</xsl:when>
					<xsl:otherwise><!-- word seems to be within current region container or totally out of bounds like mentioned in above comment -->
						<xsl:value-of select="0"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>

			<xsl:if test="$DEBUG_textoverflow = '1'">
				<xsl:variable name="d1" select="myutils:showMess(concat('-- ',number(ssdoc:wd/@l),' > ',$rcr,' textoverflow: ',$textoverflow,'&#10;'),'1')"/>
			</xsl:if>

			<xsl:variable name="linecontent">
				<xsl:call-template name="get_words" >
					<xsl:with-param name="lastLineInPara" select="position() = last()"/>
				</xsl:call-template>
			</xsl:variable>
	
			<!-- calc class attribute -->
			<xsl:variable name="class">
				<xsl:variable name="lineclass">
					<xsl:if test="@fs"><xsl:value-of select="locjs:getClassAttribute(string(@ff),string(@fs),true())"/></xsl:if>
				</xsl:variable>
				<xsl:choose>
					<xsl:when test="locjs:get_paragraphClassName() != $lineclass">
						<xsl:value-of select="$lineclass"/>
					</xsl:when>
					<xsl:otherwise>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<!-- calc style attribute -->
			<xsl:variable name="style">
				<xsl:if test="@char-attr = 'bold'">font-weight:bold;</xsl:if>
				<xsl:if test="@char-attr = 'non-bold'">font-weight:normal;</xsl:if>
				<xsl:if test="@char-attr = 'italic'">font-style:italic;</xsl:if>
				<xsl:if test="@char-attr = 'non-italic'">font-style:normal;</xsl:if>
			</xsl:variable>

			<xsl:variable name="linecontent_formated">
				<xsl:choose>
					<xsl:when test="($class != '') or ($style != '') or ($textoverflow != '0')">
						<span>
							<xsl:if test="$class != ''"><xsl:attribute name="class"><xsl:value-of select="$class"/>
																		<xsl:if test="($textoverflow != '0') and ($DEBUG_textoverflow = '1')"><xsl:text> overflow_nextbox</xsl:text></xsl:if>
														</xsl:attribute></xsl:if>
							<xsl:if test="$style != ''"><xsl:attribute name="style"><xsl:value-of select="$style"/></xsl:attribute></xsl:if>
							<xsl:if test="$textoverflow != '0'"><xsl:attribute name="data-overflow_content_id"><xsl:value-of select="locjs:getTextflowid()"/></xsl:attribute></xsl:if>
							<xsl:copy-of select="$linecontent"/>
						</span>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy-of select="$linecontent"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			
			<xsl:choose>
				<xsl:when test="$textoverflow = '0'">
					<xsl:copy-of select="$linecontent_formated"/>
				</xsl:when>
				<xsl:otherwise><!-- store this text aside for next region -->
					<xsl:if test="locjs:have_overflowtext() = false()"><!--we set a marker to give a hint that we move text to next region container -->
						<overflow_nextbox data-overflow_content_id="{locjs:getTextflowid()}"/>
					</xsl:if>
					<xsl:if test="$DEBUG_textoverflow = '1'"><!-- in debug mode show us the overflowed text in current region too and then in new region too -->
						<xsl:copy-of select="$linecontent_formated"/>
					</xsl:if>
					<xsl:variable name="overflow_string" select="myutils:serializeDOM(xalan:nodeset($linecontent_formated),'0','yes')"/>
					<xsl:variable name="d" select="locjs:store_region_overflowtext(string($overflow_string))"/>
				</xsl:otherwise>
			</xsl:choose>

	</xsl:template>

	<xsl:template name="get_words"><!-- get words from lines -->
		<xsl:param name="lastLineInPara"/>
		<xsl:for-each select="ssdoc:wd"><!-- get words within a line: ln -->
			<xsl:variable name="textcontent">
				<xsl:apply-templates>
					<xsl:with-param name="lastLineInPara" select="$lastLineInPara"/>
					<xsl:with-param name="lastWordInLine" select="position() = last()"/>
				</xsl:apply-templates>
			</xsl:variable>
			
			<!-- calc style attribute -->
			<xsl:variable name="style">
				<xsl:if test="@char-attr = 'bold'">font-weight:bold;</xsl:if>
				<xsl:if test="@char-attr = 'non-bold'">font-weight:normal;</xsl:if>
				<xsl:if test="@char-attr = 'italic'">font-style:italic;</xsl:if>
				<xsl:if test="@char-attr = 'non-italic'">font-style:normal;</xsl:if>
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
		 create and get a (scaled) page preview JPEG
	     =============================================== -->
	<xsl:template name="get_pageJPEG">
		<xsl:param name="thepage"/>
		<xsl:param name="page_sequence"/>
		<xsl:param name="page_name"/>
		<xsl:if test="$DEBUG = 1">
			<xsl:variable name="d" select="locjs:message(concat('&#10;****** CREATE Page JPEG for docname: ',$docname))"/>
			<xsl:variable name="d" select="locjs:message(concat('******        Page original image name: ',locjs:get_pageimageName()))"/>
			<xsl:variable name="d" select="locjs:message(concat('******        Page JPEG scale requested: ',locjs:get_pageJPEGScale()))"/>
		</xsl:if>

		<xsl:variable name="pageJPEGname"><xsl:value-of select="concat($docname,'_',locjs:getPageSequence(string($thepage)),'.jpg')"/></xsl:variable>
		<xsl:variable name="pageJPEGpath"><xsl:value-of select="$INPUT_PATH"/><xsl:value-of select="$pageJPEGname"/></xsl:variable>

		<xsl:variable name="d0" select="locjs:store_Value(0,'')"/>
		<xsl:variable name="exists" select="locjs:fileExists(string($pageJPEGpath))"/>
		<xsl:if test="$DEBUG = 1">
			<xsl:variable name="d" select="locjs:message(concat('******        Looking for original Page JPEG: ',$pageJPEGpath))"/>
		</xsl:if>
		<xsl:if test="($exists = 'true')"><xsl:variable name="d0" select="locjs:store_Value(0,string($pageJPEGname))"/></xsl:if><!-- store original name -->
		<xsl:if test="$DEBUG = 1">
			<xsl:variable name="d" select="locjs:message(concat('              exists: ',$exists))"/>
		</xsl:if>

		<!-- create resized jpeg name -->
		<xsl:variable name="pageimageName" select="locjs:get_pageimageName(true())"/>
		<xsl:variable name="origWidth" select="locjs:get_pagewidth()"/>
		<xsl:variable name="origHeight" select="locjs:get_pageheight()"/>
		<xsl:variable name="newWidth" select="locjs:get_pageJPEGWidth()"/>
		<xsl:variable name="newHeight" select="locjs:get_pageJPEGHeight()"/>

		<xsl:variable name="resizedPageJPEGname">
			<xsl:variable name="tmp_resizedPageJPEGname"><xsl:value-of select="locjs:create_new_name(string($pageJPEGname),locjs:get_pageJPEGScale100())"/></xsl:variable>
			<xsl:value-of select="$tmp_resizedPageJPEGname"/>
		</xsl:variable>
		<xsl:variable name="d0" select="locjs:store_resizedPageJPEGname(string($resizedPageJPEGname))"/>
		<xsl:variable name="d0" select="locjs:store_Value(0,string($resizedPageJPEGname))"/>

		<!-- call external Image Converter 'ImageMagick' -->
		<xsl:variable name="source"><xsl:value-of select="$INPUT_PATH"/><xsl:value-of select="$pageimageName"/></xsl:variable>
		<xsl:variable name="target"><xsl:value-of select="$the_OUTPUT_PATH"/><xsl:value-of select="$resizedPageJPEGname"/></xsl:variable>

		<xsl:variable name="resize_param">-resize <xsl:value-of select="$newWidth"/>x<xsl:value-of select="$newHeight"/></xsl:variable>

		<xsl:variable name="mypageJPEGParams" select="locjs:removePercent(string(normalize-space($pageJPEGParams)))"/>
		<xsl:variable name="params" select="locjs:createParamString(string($mypageJPEGParams),locjs:escapeMagick(string($source)),string($IMPageJPEGdpi),string($resize_param),string($pageJPEGfinish),string($IMPageJPEGquality),'-colorspace RGB',locjs:escapeMagick(string($target)))"/>

		<xsl:variable name="imageconverted">
			<xsl:choose>
				<xsl:when test="$ImageMagickConvert != ''"><!-- create resized page jpeg using ImageMagick's CONVERT program if reachable -->
					<xsl:if test="$DEBUG = 1">
						<xsl:variable name="d" select="locjs:message(concat('****** Resizing Page Image pageJPEGdpi: ',$pageJPEGdpi,' params: ',$params))"/>
					</xsl:if>
					<xsl:variable name="m0">      - Resizing Page Image (<xsl:value-of select="$newWidth"/>x<xsl:value-of select="$newHeight"/>): '<xsl:value-of select="$pageimageName"/>' to '<xsl:value-of select="$resizedPageJPEGname"/>': </xsl:variable>
					<xsl:variable name="d1" select="myutils:showMess(string($m0),'1')"/>
					<xsl:value-of select="myutils:callExternalApp(string($ImageMagickConvert),$params,$ImageMagickConvertEnvir,'',$filenameConversion)"/>
				</xsl:when>
				<xsl:otherwise><!-- use internal BatchXSLT converter -->
					<xsl:if test="$DEBUG = 1">
						<xsl:variable name="startmess">****** Resizing Page Image (internal): scale=<xsl:value-of select="locjs:get_pageJPEGScale()"/>, quality=<xsl:value-of select="string($pageJPEGQuality)"/>, dpi=<xsl:value-of select="string($pageJPEGdpi)"/>, <xsl:value-of select="$source"/>, <xsl:value-of select="$target"/></xsl:variable>
						<xsl:variable name="d" select="locjs:message(string($startmess))"/>
					</xsl:if>
					<xsl:variable name="m0">      - !Resizing Page Image (<xsl:value-of select="$newWidth"/>x<xsl:value-of select="$newHeight"/>): '<xsl:value-of select="$pageimageName"/>' to '<xsl:value-of select="$resizedPageJPEGname"/>': </xsl:variable>
					<xsl:variable name="d1" select="myutils:showMess(string($m0),'1')"/>
					<!--xsl:value-of select="myutils:resize_jpeg(string($source),string($target),locjs:get_pageJPEGScale(),locjs:parseToFloat(string($pageJPEGQuality)),3,2,locjs:parseToInt(string($pageJPEGdpi)))"/-->
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="exists" select="locjs:fileExists(string($target))"/>
		<xsl:variable name="m1"><xsl:choose><xsl:when test="$exists='true'">OK&#10;</xsl:when><xsl:otherwise>### ERROR: <xsl:value-of select="$imageconverted"/>&#10;</xsl:otherwise></xsl:choose></xsl:variable>
		<xsl:variable name="d" select="myutils:showMess(string($m1),'1')"/>

		<!-- evtl. create a copy of the original page image to output folder -->
		<xsl:variable name="d1" select="locjs:store_pageJPEGcopyName('')"/>
		<xsl:if test="$pageJPEGcopy != '0'">
			<xsl:variable name="pageJPEGcopyName">
				<xsl:variable name="tmp_pageJPEGcopyName"><xsl:value-of select="locjs:new_name_ext(string($pageimageName),'.jpg')"/></xsl:variable><!-- convert TIFF to JPEG -->
				<xsl:value-of select="$tmp_pageJPEGcopyName"/>
			</xsl:variable>
			<xsl:variable name="d1" select="locjs:store_pageJPEGcopyName(string($pageJPEGcopyName))"/>
			<xsl:variable name="m0">      - Creating a JPEG copy of Page Image '<xsl:value-of select="$pageimageName"/>' to '<xsl:value-of select="$pageJPEGcopyName"/>': </xsl:variable>
			<xsl:variable name="d1" select="myutils:showMess(string($m0),'1')"/>

			<!-- prepare IM call -->
			<xsl:variable name="copy_source"><xsl:value-of select="$INPUT_PATH"/><xsl:value-of select="$pageimageName"/></xsl:variable>
			<xsl:variable name="copy_target"><xsl:value-of select="$the_OUTPUT_PATH"/><xsl:value-of select="$pageJPEGcopyName"/></xsl:variable>
			<xsl:variable name="IMPageJPEGcopyQuality">-quality <xsl:choose><xsl:when test="$pageJPEGcopyQuality != ''"><xsl:value-of select="$pageJPEGcopyQuality"/></xsl:when><xsl:otherwise>80</xsl:otherwise></xsl:choose></xsl:variable><!--  page copy JPEG quality -->
			<xsl:variable name="IMPageJPEGcopydpi">-resample <xsl:choose><xsl:when test="$pageJPEGcopydpi != ''"><xsl:value-of select="$pageJPEGcopydpi"/>x<xsl:value-of select="$pageJPEGcopydpi"/></xsl:when><xsl:otherwise>150x150</xsl:otherwise></xsl:choose></xsl:variable><!--  page copy JPEG dpi -->

			<xsl:variable name="params" select="locjs:createParamString(locjs:escapeMagick(string($copy_source)),string($IMPageJPEGcopydpi),string($IMPageJPEGcopyQuality),'-colorspace RGB',locjs:escapeMagick(string($copy_target)))"/>
			<xsl:variable name="pagecopycreated">
				<xsl:value-of select="myutils:callExternalApp(string($ImageMagickConvert),$params,$ImageMagickConvertEnvir,'',$filenameConversion)"/>
			</xsl:variable>

			<xsl:variable name="exists" select="locjs:fileExists(string($copy_target))"/>
			<xsl:variable name="m1"><xsl:choose><xsl:when test="$exists='true'">OK&#10;</xsl:when><xsl:otherwise>### ERROR: <xsl:value-of select="$pagecopycreated"/>&#10;</xsl:otherwise></xsl:choose></xsl:variable>
			<xsl:variable name="d" select="myutils:showMess(string($m1),'1')"/>

		</xsl:if>

		<!-- evtl. copy original page JPEGfiles to output folder (for magnifier glass) -->
		<xsl:if test="$magnifyingGlass != '0'">
			<xsl:variable name="d" select="myutils:copyFile(string($INPUT_PATH),string($pageJPEGname),string($the_OUTPUT_PATH),string($pageJPEGname),1,false)"/>
		</xsl:if>

		<!-- evtl. copy original page image to output folder -->
		<xsl:if test="$pageCopyOriginal != '0'">
			<xsl:variable name="m0">      - Copying original Page Image '<xsl:value-of select="$pageimageName"/>' to output folder.&#10;</xsl:variable>
			<xsl:variable name="d1" select="myutils:showMess(string($m0),'1')"/>
			<xsl:variable name="d" select="myutils:copyFile(string($INPUT_PATH),string($pageimageName),string($the_OUTPUT_PATH),string($pageimageName),1,false)"/>
		</xsl:if>

		<pageJPEG>
			<xsl:attribute name="page_sequence"><xsl:value-of select="$page_sequence" /></xsl:attribute>
			<xsl:attribute name="page_name"><xsl:value-of select="$page_name" /></xsl:attribute>
			<xsl:attribute name="sizefactor"><xsl:value-of select="locjs:get_pageJPEGScale()" /></xsl:attribute>
			<xsl:attribute name="scale"><xsl:value-of select="locjs:get_pageJPEGScalePercent()" /></xsl:attribute>
			<xsl:attribute name="w"><xsl:value-of select="locjs:get_pageJPEGWidth()" /></xsl:attribute>
			<xsl:attribute name="h"><xsl:value-of select="locjs:get_pageJPEGHeight()" /></xsl:attribute>
			<xsl:attribute name="original"><xsl:value-of select="$pageimageName" /></xsl:attribute>
			<xsl:choose>
				<xsl:when test="locjs:get_pageJPEGcopyName() != ''">
					<xsl:attribute name="originalJPEG"><xsl:value-of select="locjs:get_pageJPEGcopyName()" /></xsl:attribute>
				</xsl:when>
			</xsl:choose>

			<xsl:value-of select="locjs:get_resizedPageJPEGname()"/></pageJPEG>
	</xsl:template>




	<!-- ===============================================
		 stuff to suppress
	     =============================================== -->





	<!-- =========================
		 our local JAVA Script functions: include
		 ========================= -->
	<lxslt:component prefix="locjs">
	<lxslt:script lang="javascript" src="OmniXML2slideXML.js"></lxslt:script>
	</lxslt:component>


</xsl:stylesheet>
