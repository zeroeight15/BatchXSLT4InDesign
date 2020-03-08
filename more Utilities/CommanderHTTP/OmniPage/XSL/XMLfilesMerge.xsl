<?xml version="1.0" encoding="UTF-8"?>
<!-- 
 ======================================================================
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

Version: 1.11
Version date: 20120905
======================================================================
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				xmlns:lxslt="http://xml.apache.org/xslt"
				xmlns:xalan="http://xml.apache.org/xalan" 
				xmlns:myutils="com.epaperarchives.batchxslt.utils"
				xmlns:locjs="loc_funcs"

				exclude-result-prefixes="xalan lxslt myutils locjs"
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

	<xsl:param name="DEBUG"/><!-- set to 1 to show debug info in console -->
	<xsl:param name="DEBUGIMAGES"/><!-- set to 1 to show debug info in console -->
	<xsl:param name="DEBUG_cssfile"/><!-- set to 1 for additional info in css -->

<!-- custom parameters -->
	<xsl:param name="delete_individual_bookpagesXML" select="1"/><!--  default = 1, delete all indivial pages XML files of a book after having merged them, 0 = don't delete -->
	<xsl:param name="merge_individual_bookpagesPDF" select="1"/><!--  default = 1, merge all available page PDFs into one document, 0 = don't merge -->

	<xsl:output method="xml" />
	<xsl:output media-type="text/xml"/>
	<xsl:output omit-xml-declaration="no"/>

<!--xsl:output xalan:indent-amount="0"/-->
	<xsl:output indent="yes"/>
	<xsl:output encoding="UTF-8"/>



	<!-- *********** templates for the folder index file 'anyname.idx' ********** -->
	<xsl:template match="/">
		<!-- *********** general variables ********** -->
		<xsl:variable name="outpath" select="$INPUT_PATH"/><!-- mergeXML files to same folder -->

		<!-- *********** init ********** -->
		<xsl:variable name="d" select="locjs:init_offsets()"/><!-- init page_sequence attr counter and more -->


		<xsl:variable name="mergeroot" select="/folderList/rootPath/." />


		<!-- optionally merge all single page PDf files -->
		<xsl:if test="$merge_individual_bookpagesPDF != '0'">
			<xsl:variable name="pages_pdf_list"><!-- create a comma separated file list string -->
				<xsl:for-each select="/folderList/file[locjs:endsWith(string(.),'.pdf')]">
					<xsl:sort data-type="text" select="."/><!-- sort by Path/filename -->
					<xsl:variable name="PDFmergename" select="." />
					<xsl:variable name="filesize" select="myutils:fileSizeS(string($PDFmergename))" />
					<xsl:choose>
						<xsl:when test="$filesize != '0'"><!-- ignore ZERO bytes files: they do not exist or are open -->
							<xsl:value-of select="."/>
							<xsl:if test="position() != last()">
								<xsl:text>,</xsl:text>
							</xsl:if>
						</xsl:when>
					</xsl:choose>
				</xsl:for-each>
			</xsl:variable>
	
			<xsl:if test="contains($pages_pdf_list,',')"><!-- this means we have a comma and therefore at least two PDF files -->
				<xsl:variable name="d1" select="myutils:showMess(concat('&#10;---- Merging PDF files at path: ',$mergeroot,'&#10;'),'1')"/>
				<!-- create output document PDF name -->
				<xsl:variable name="docPDFname"><xsl:value-of select="locjs:re_replace(string($INPUT_NAME),'.idx','_doc.pdf','')"/></xsl:variable>
				<!-- store the document PDF name into this container variable 'documentPDFname' -->
				<xsl:variable name="d" select="locjs:store_documentPDFname(string($docPDFname))"/>

				<!-- call PDF merger -->
				<xsl:variable name="filelist"><xsl:value-of select="$pages_pdf_list"/>,<xsl:value-of select="$mergeroot"/><xsl:value-of select="$docPDFname"/></xsl:variable>
				<xsl:variable name="mergeError" select="myutils:PDFmerge(string($filelist))" />
				<xsl:choose>
					<xsl:when test="$mergeError = 0">
						<xsl:variable name="d1" select="myutils:showMess(concat('          Merged PDF file: ',$docPDFname,'&#10;'),'1')"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="d1" select="myutils:showMess(concat('#### ERROR ',$mergeError,' while creating Merged PDF file: ',$docPDFname,'&#10;'),'1')"/>
						<xsl:variable name="d1" select="myutils:showMess(concat('#### ERROR Message: ',myutils:getPDFmergeError(),'&#10;'),'1')"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:if>
		<xsl:variable name="documentPDF" select="locjs:get_documentPDFname()" />

		<!-- do merge all XML files -->
		<xsl:variable name="d1" select="myutils:showMess(concat('&#10;---- Merging XML files at path: ',$mergeroot,'&#10;'),'1')"/>

		<xsl:variable name="xml_documents_list">
			<xsl:for-each select="/folderList/file[locjs:endsWith(string(.),'.xml')]">
				<xsl:sort data-type="text" select="."/><!-- sort by Path/filename -->
				<xsl:variable name="mergename" select="." />
				<xsl:variable name="filesize" select="myutils:fileSizeS(string($mergename))" />
				<xsl:choose>
					<xsl:when test="$filesize != '0'"><!-- ignore ZERO bytes files: they do not exist or are open -->
						<file><xsl:value-of select="."/></file>
					</xsl:when>
				</xsl:choose>
			</xsl:for-each>
		</xsl:variable>

		<!-- get the <?xml-stylesheet href="../XSLCSS/slidebook.xsl" type="text/xsl"?> -->
		<xsl:variable name="xmldocument" select="document(xalan:nodeset($xml_documents_list)/file/.)" />
		<xsl:text disable-output-escaping="yes">&#10;</xsl:text>
		<xsl:processing-instruction name="xml-stylesheet">
			<xsl:value-of select="xalan:nodeset($xmldocument)/processing-instruction('xml-stylesheet')/."/>
		</xsl:processing-instruction>
		<xsl:text disable-output-escaping="yes">&#10;</xsl:text>


		<indd_document>
			<MERGEFILES>
				<xsl:copy-of select="xalan:nodeset($xml_documents_list)" />
			</MERGEFILES>
			<xsl:for-each select="xalan:nodeset($xml_documents_list)/file">
				<xsl:variable name="d1" select="myutils:showMess(concat('          Merge XML file: ',.,'&#10;'),'1')"/>
				<xsl:choose>
					<xsl:when test="position() = 1"><!--get first page -->
						<xsl:call-template name="get_first_document">
							<xsl:with-param name="xml_filename"><xsl:value-of select="."/></xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise><!-- get document pages for following pages gt page#1 -->
						<xsl:call-template name="get_pages">
							<xsl:with-param name="xml_filename"><xsl:value-of select="."/></xsl:with-param>
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>

			<xsl:if test="$documentPDF != ''">
				<documentPDF>
					<xsl:attribute name="name"><xsl:value-of select="$documentPDF" /></xsl:attribute>
					<xsl:value-of select="$documentPDF" />
				</documentPDF>
			</xsl:if>
		</indd_document>
		
		<xsl:variable name="d1" select="myutils:showMess('         complete.&#10;','1')"/>

	</xsl:template>


	<xsl:template name="get_first_document">
		<xsl:param name="xml_filename"/>
		<xsl:comment> Merging head stuff from first XML file:<xsl:value-of select="$xml_filename"/> </xsl:comment>
		<!-- get head of first xml document -->
		<xsl:variable name="firstxmldocument" select="document($xml_filename)" />

		<xsl:copy-of select="xalan:nodeset($firstxmldocument)/indd_document/INPUT_PATH"/>
		<xsl:copy-of select="xalan:nodeset($firstxmldocument)/indd_document/OUTPUT_PATH"/>

		<!-- copy call_parameters -->
		<call_parameters>
			<xsl:for-each select="xalan:nodeset($firstxmldocument)/indd_document/call_parameters/par">
				<xsl:copy-of select="."/>
			</xsl:for-each>
			<xsl:if test="locjs:get_documentPDFname() != ''">
				<par name="documentPDFname"><xsl:value-of select="locjs:get_documentPDFname()"/></par>
			</xsl:if>
			<par name="documentPDFnameSearch">1</par>
		</call_parameters>

		<xsl:copy-of select="xalan:nodeset($firstxmldocument)/indd_document/doctypeinfos"/>

		<!-- copy header -->
		<header type="layout">
			<xsl:for-each select="xalan:nodeset($firstxmldocument)/indd_document/header/headerfield">
				<xsl:choose>
					<xsl:when test="@name='filename'">
						<headerfield name="filename"><xsl:value-of select="string($OUTPUT_NAME)"/></headerfield>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy-of select="."/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
		</header>

		<!-- copy pages -->
		<xsl:call-template name="get_pages">
			<xsl:with-param name="xml_filename"><xsl:value-of select="."/></xsl:with-param>
		</xsl:call-template>

	</xsl:template>



	<xsl:template name="get_pages">
		<xsl:param name="xml_filename"/>
		<xsl:comment> Merging pages from XML file:<xsl:value-of select="$xml_filename"/> </xsl:comment>
		<xsl:variable name="xmlpathname" select="$xml_filename"/>
		<xsl:variable name="exists" select="myutils:existsFile(string($xmlpathname))"/>
		<xsl:choose>
			<xsl:when test="($exists = true())">
				<xsl:variable name="xmldocument" select="document($xmlpathname)" />
				<xsl:text disable-output-escaping="yes">&#10;</xsl:text><xsl:comment>*******************************************</xsl:comment><xsl:text disable-output-escaping="yes">&#10;</xsl:text>
				<xsl:comment>*** Include document XML file <xsl:value-of select="position()"/>: <xsl:value-of select="$xmlpathname"/></xsl:comment><xsl:text disable-output-escaping="yes">&#10;</xsl:text>
				<xsl:for-each select="xalan:nodeset($xmldocument)/indd_document/page">
					<xsl:variable name="page_sequence" select="locjs:get_new_page_sequence()"/>
					<xsl:comment>*** page sequence: <xsl:value-of select="locjs:get_page_sequence()"/> -  articles offset: <xsl:value-of select="locjs:get_article_idx()"/></xsl:comment>

					<page>
						<xsl:for-each select="@*">
							<xsl:attribute name="{name()}">
								<xsl:choose>
									<xsl:when test="name() = 'Self'">
										<xsl:value-of select="."/>_<xsl:value-of select="$page_sequence"/>
									</xsl:when>
									<xsl:when test="name() = 'page_sequence'">
										<xsl:value-of select="$page_sequence"/>
									</xsl:when>
									<!--xsl:when test="name() = 'page_name'">
										<xsl:value-of select="$page_sequence"/>
									</xsl:when-->
									<xsl:otherwise>
										<xsl:value-of select="."/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
						</xsl:for-each>
						<xsl:apply-templates mode="copyelement">
							<xsl:with-param name="page_sequence"><xsl:value-of select="$page_sequence"/></xsl:with-param>
						</xsl:apply-templates>
					</page>

				</xsl:for-each>
				<!-- evtl. delete merged individual book document files -->
				<xsl:if test="$delete_individual_bookpagesXML != '0'">
					<xsl:variable name="d" select="myutils:deleteFile(string($xmlpathname))"/>
				</xsl:if>

			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="d1" select="myutils:showMess(concat('#### FATAL ERROR. Missing path/file: ',$xmlpathname,'&#10;'),'1')"/>
				<ErrorDocument>
					<ErrorText>#### FATAL ERROR. Missing path/file</ErrorText>
					<ErrorPathFile><xsl:value-of select="$xmlpathname"/>"</ErrorPathFile>
				</ErrorDocument>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<xsl:template match="* | @* | processing-instruction() | text() | comment()" mode="copyelement">
		<xsl:param name="page_sequence"/>
		<xsl:choose>
			<xsl:when test="name() ='page_sequence'"><!-- update page sequences -->
				<xsl:attribute name="{name()}"><xsl:value-of select="$page_sequence"/></xsl:attribute>
			</xsl:when>
			<xsl:when test="name() ='page_name'"><!-- update page name -->
				<xsl:attribute name="{name()}"><xsl:value-of select="$page_sequence"/></xsl:attribute>
			</xsl:when>
			<xsl:when test="name() ='idx'"><!-- update article idx -->
				<xsl:attribute name="{name()}"><xsl:value-of select="locjs:get_new_article_idx()"/></xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:apply-templates select="* | @* | processing-instruction() | text() | comment()" mode="copyelement">
						<xsl:with-param name="page_sequence"><xsl:value-of select="$page_sequence"/></xsl:with-param>
					</xsl:apply-templates>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>



	<!-- =========================
		 our local JAVA Script functions
		 ========================= -->
	<lxslt:component prefix="locjs">
	<lxslt:script lang="javascript">
	//<![CDATA[

var page_sequence = 0;
var article_idx = 0;
var article_count_document = 0;
function init_offsets() {
	page_sequence = 0;
	article_idx = 0;
	article_idx_document = 0;
}
function get_page_sequence() {
	return(page_sequence);
}
function get_new_page_sequence() {
	return(++page_sequence);
}

function get_new_article_idx() {
	return(++article_idx);
}
function get_article_idx() {
	return(article_idx);
}
var documentPDFname = "";
function store_documentPDFname(name) {
	documentPDFname = name;
}
function get_documentPDFname() {
	return(documentPDFname);
}



function parse_int(str) {
	if ((str == null) || (str == "")) return(str);
	var val;
	try {
		val = parseInt(str);
	}
	catch(e) { return(str); }
	return(val);
}

function endsWith(str, s){
	if ( (str == null) || (str == "") ) return(false);
	if ( (s == null) || (s == "") ) return(false);
	var reg = new RegExp (s + "$");
	return reg.test(str);
}

function renameFile(pathname,newpathname) {
	var f = new java.io.File(pathname);
	if (f.exists() == true) {
		var fnew = new java.io.File(newpathname);
		f.renameTo(fnew);
	}
}


// replace using regexp
function re_replace(sourcestr,findstr,replstr,option) {
	if ( (sourcestr == "") || (findstr == "") ) return(sourcestr);
	var re = null;
	try { re = new RegExp(findstr,option); } catch(e) { return(sourcestr); }
	return (sourcestr.replace(re,replstr));
}

function SysMess(mess) {
	java.lang.System.out.println (mess);
}
	//]]>
	</lxslt:script>
	</lxslt:component>


</xsl:stylesheet>
