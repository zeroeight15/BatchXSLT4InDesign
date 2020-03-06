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

Short Description: Article List Simple XML/HTML
Description: Convert InDesign document content to simplyfied XML/HTML as continuous article list.

Version: 46
Version date: 20200219
======================================================================
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				version="1.0">
	<xsl:output method="html" />
	<xsl:output doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" />
	<xsl:output doctype-system="http://www.w3.org/TR/html4/loose.dtd" />
	<xsl:output media-type="text/html"/>
	<xsl:output indent="yes"/>
	<xsl:output encoding="UTF-8"/>


	<xsl:variable name="suppress_empty_divs">0</xsl:variable><!-- default = 0. set to 0 to add a non breaking space in empty divs for it is not treated as vertical white space by browsers. 1 menas that it is left empty and might be suppressed by browsers -->
	<xsl:variable name="debug_ctrlchar">0</xsl:variable><!-- default = 0. set to 1 to show control character codes within text in red -->


<!-- ================ HERE WE GO ================ -->
	<!-- create the XHTML ePaper pages -->
	<xsl:template match="/">
		<xsl:apply-templates select="indd_document" />
	</xsl:template>

	<xsl:template match="indd_document">
		<html>
		<head>
			<title><xsl:value-of select="@title" /></title>
			<link type="text/css">
				<xsl:attribute name="rel">StyleSheet</xsl:attribute>
				<xsl:attribute name="href"><xsl:value-of select="doctypeinfos/@csspath"/></xsl:attribute>
			</link>
			<script type="text/javascript" language="javascript">
//<![CDATA[<!--
function showImage(name) {
	F=window.open('','resizedImage','screenX=20,screenY=20,left=20,top=20,width=600,height=600,resizable=Yes,scrollbars=Yes');
	F.location.href=name;
	F.focus();
	return;
}
-->//]]>
			</script>
		</head>
		<xsl:call-template name="show_creator">
			<xsl:with-param name="creationDate"><xsl:value-of select="header/headerfield[@name='creationDate']/." /></xsl:with-param>
			<xsl:with-param name="outputVersion"><xsl:value-of select="header/headerfield[@name='outputVersion']/." /></xsl:with-param>
			<xsl:with-param name="inputPath"><xsl:value-of select="header/headerfield[@name='inputPath']/." /></xsl:with-param>
			<xsl:with-param name="indesignDocname"><xsl:value-of select="header/headerfield[@name='indesignDocname']/." /></xsl:with-param>
			<xsl:with-param name="sourceINXfileName"><xsl:value-of select="header/headerfield[@name='sourceINXfileName']/." /></xsl:with-param>
		</xsl:call-template>

		<body id="body">

		<xsl:if test="//call_parameters/par[@name = 'documentPDFname']/. != ''">
			<xsl:call-template name="set_documentPDF">
				<xsl:with-param name="page"></xsl:with-param>
			</xsl:call-template>
		</xsl:if>


		<xsl:for-each select="page">

			<xsl:comment> **** PAGE #<xsl:value-of select="@page_sequence" /> CONTENT: **** </xsl:comment>

			<!-- output the articles -->
			<xsl:call-template name="output_articles"/>

			<xsl:if test="pagePDF">
				<xsl:call-template name="set_pagePDF">
					<xsl:with-param name="page"><xsl:value-of select="pagePDF/@page_sequence/."/></xsl:with-param>
				</xsl:call-template>
			</xsl:if>

			<xsl:comment> **** PAGE #<xsl:value-of select="@page_sequence" /> CONTENT: END **** </xsl:comment>
			<hr align="center" noshade="noshade" size="2" width="100%">
				<xsl:attribute name="id">pageseparator_<xsl:value-of select="@page_sequence"/></xsl:attribute>
			</hr>
		</xsl:for-each>

		</body>
		</html>
	</xsl:template>






	<xsl:template name="output_articles"><!-- output the articles -->
		<xsl:comment> **** ARTICLES ON PAGE #<xsl:value-of select="@page_sequence"/>: **** </xsl:comment>
		<xsl:for-each select="articles/article">
			<xsl:sort select="@t" data-type="number" order="ascending" /><!-- sort top down -->
			<xsl:sort select="@l" data-type="number" order="ascending" /><!-- sort left right -->
			<xsl:if test="descendant-or-self::content//*"><!-- dont show empty articles -->
				<xsl:comment> **** ARTICLE '<xsl:value-of select="@idx"/>' PAGE #<xsl:value-of select="@page_sequence"/>:  **** </xsl:comment>
	
					<xsl:choose>
						<xsl:when test="(@backgroundColor != '') and (@backgroundColor != '#000000')">
							<div>
								<xsl:attribute name="style">background-color:<xsl:value-of select="@backgroundColor" /></xsl:attribute>
								<xsl:apply-templates select="content/* | comment() | text()"/>
							</div>
						</xsl:when>
						<xsl:otherwise>
							<xsl:apply-templates select="* | comment() | text()"/>
						</xsl:otherwise>
					</xsl:choose>
				<xsl:comment> **** ARTICLE '<xsl:value-of select="@idx"/>' PAGE #<xsl:value-of select="@page_sequence"/>: END **** </xsl:comment>
				<hr align="center" noshade="noshade" size="3" width="50%" />
			</xsl:if>
		</xsl:for-each>
		<xsl:comment> **** ARTICLES ON PAGE #<xsl:value-of select="@page_sequence"/>: END **** </xsl:comment>
	</xsl:template>


	<!-- get content -->
	<xsl:template match="content">
		<!-- check if we have to add an outer container with special attribs -->
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
		<xsl:variable name="addOuterBorder" select="((@frameWeight != '0') and (@frameWeight != '')) or ((../boxchain/box[@textflowid = $boxtextflowid]/@frameWeight != '0') and (../boxchain/box[@textflowid = $boxtextflowid]/@frameWeight != ''))"/>
		<xsl:choose>
			<xsl:when test="($outerBackgroundColor != '') or ($addOuterInsets = 'true') or ($addOuterBorder = 'true')">
				<div>
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
								<xsl:when test="(@frameWeight != '0') and (@frameWeight != '')"> border:<xsl:value-of select="@frameWeight" />px <xsl:value-of select="@frameStyleHTML" /><xsl:text> </xsl:text><xsl:if test="(@frameColor != '') and (@frameColor != 'transparent') and (starts-with(@frameColor,'#') = false)">#</xsl:if><xsl:value-of select="@frameColor" />;</xsl:when>
								<xsl:when test="(../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopL != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopB != '0') or (../boxchain/box[@textflowid = $boxtextflowid]/@insetTopR != '0')"> border:<xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@frameWeight" />px <xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@frameStyleHTML" /><xsl:text> </xsl:text><xsl:if test="(@frameColor != '') and (@frameColor != 'transparent') and (starts-with(@frameColor,'#') = false)">#</xsl:if><xsl:value-of select="../boxchain/box[@textflowid = $boxtextflowid]/@frameColor" />;</xsl:when>
								<xsl:otherwise></xsl:otherwise>
							</xsl:choose>
						</xsl:if>

					</xsl:attribute>
					<xsl:apply-templates select="* | comment() | text()"/>
				</div>
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
			<xsl:when test="(@name = 'Table') and (/indd_document/call_parameters/par[@name='TABLE_AS_BLOCK']/. = '1')">
				<xsl:apply-templates/>
			</xsl:when>
			<xsl:when test="(@name = 'FNcl')"><!-- footnotes -->
				<div class="A_anchored_footnote__">
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
				</div>
			</xsl:when>
			<xsl:otherwise><div class="A_anchored_object__"><xsl:apply-templates/></div></xsl:otherwise>
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


	<!-- get content of xmlElement -->
	<xsl:template match="xmlElement">
		<xsl:apply-templates select="* | comment() | text()"/>
	</xsl:template>

	<!-- LINE SEPARATOR -->
	<xsl:template match="br"><br/></xsl:template><!-- <br type="LINE SEPARATOR"/> -->


	<!-- copy other elements -->
	<xsl:template match="*">
		<!-- copy elements but suppress certain paragraphs like 'Pull Quote' -->
		<xsl:choose>
			<!-- suppress certain elements -->
			<xsl:when test="(@class/. = 'P_PullbQuoteXX') or 
							(@class/. = 'P_SubjectbRunningbHeadXX')
							"/>

			<!-- handle images -->
			<xsl:when test="name() = 'img2'"/><!-- suppress now, this is handled in img tag -->
			<xsl:when test="name()='img'"><!-- comment images to prevent preloading. They will be enabled by XTXPepaper.js when showing an article -->
				<!-- check if we have a second image (enlarged) -->
				<xsl:variable name="imagetitle">
					<xsl:choose>
						<xsl:when test="@title/. != ''"><!-- get from own image title (usually not set) -->
							<xsl:value-of select="@title/."/>
						</xsl:when>
						<xsl:when test="../@title/. != ''"><!-- get from content title given by script label -->
							<xsl:value-of select="../@title/."/>
						</xsl:when>
						<xsl:when test="following-sibling::img2[1]">^</xsl:when>
						<xsl:otherwise></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>

				<xsl:choose>
					<xsl:when test="//call_parameters/par[@name='imageCopyToOutput']/. != '0'"><!-- original images are available -->
						<img>
							<xsl:for-each select="@*">
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:for-each>
							<xsl:if test="$imagetitle != ''">
								<xsl:attribute name="title"><xsl:value-of select="$imagetitle" /></xsl:attribute>
							</xsl:if>
							<xsl:if test="following-sibling::img2[1]">
								<xsl:attribute name="onClick">javascript:showImage('<xsl:value-of select="following-sibling::img2[1]/@src"/>');</xsl:attribute>
							</xsl:if>
						</img>
					</xsl:when>
					<xsl:otherwise><!-- original images are not available -->
						<img>
							<xsl:for-each select="@*">
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:for-each>
							<xsl:if test="$imagetitle != ''">
								<xsl:attribute name="title"><xsl:value-of select="$imagetitle" /></xsl:attribute>
							</xsl:if>
							<xsl:if test="following-sibling::img2[1]">
								<xsl:attribute name="onClick">javascript:showImage('<xsl:value-of select="following-sibling::img2[1]/@src"/>');</xsl:attribute>
							</xsl:if>
						</img>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>

			<!-- ******** div -->
			<xsl:when test="name()='div'">
				<xsl:element name="{name()}">
					<xsl:for-each select="@*">
						<xsl:choose>
							<xsl:when test="name() = 'style' and contains(.,'vertical-align')"><!-- sub and super font sizes -->
								<xsl:attribute name="{name()}"><xsl:value-of select="." />font-size:75%;</xsl:attribute>
							</xsl:when>
							<xsl:when test="name() = 'style'">
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:when>
							<xsl:when test="name() = 'class'">
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:when>
							<xsl:otherwise></xsl:otherwise><!-- suppress all other invalid HTML attribs -->
						</xsl:choose>
					</xsl:for-each>
					<xsl:if test="((. = '') or (. = ' ')) and ($suppress_empty_divs = '0')">&#160;</xsl:if><!-- empty divs are ignored by browsers as vertical white space. add a non breaking space -->
					<xsl:apply-templates select="* | text()"/>
				</xsl:element>
			</xsl:when>

			<!-- ******** span -->
			<xsl:when test="name()='span'">
				<xsl:element name="{name()}">
					<xsl:for-each select="@*">
						<xsl:choose>
							<xsl:when test="name() = 'style' and contains(.,'vertical-align')"><!-- sub and super font sizes -->
								<xsl:attribute name="{name()}"><xsl:value-of select="." />font-size:75%;</xsl:attribute>
							</xsl:when>
							<xsl:when test="name() = 'style'">
								<xsl:attribute name="{name()}"><xsl:value-of select="." /></xsl:attribute>
							</xsl:when>
							<xsl:when test="name() = 'class'">
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
						<xsl:when test="@destinationanchorname"><!-- a source text anchor -->
							<xsl:attribute name="href"><xsl:value-of select="@destinationanchorhtm" />?anc=<xsl:value-of select="@destinationanchorname" /></xsl:attribute>
							<xsl:attribute name="title"><xsl:value-of select="@title" /></xsl:attribute>
							<xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute>
						</xsl:when>
						<xsl:when test="@anchorname"><!-- a destination text anchor -->
							<xsl:attribute name="name"><xsl:value-of select="@anchorname" /></xsl:attribute>
							<xsl:attribute name="title"><xsl:value-of select="@title" /></xsl:attribute>
							<xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute>
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
			</xsl:when>

			<!-- ******** table cells -->
			<xsl:when test="name()='td'">
				<xsl:element name="{name()}">
					<xsl:for-each select="@*">
						<xsl:choose>
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
					<xsl:attribute name="style">border:0;margin:0;padding:0;background-color:transparent;</xsl:attribute>
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
						<xsl:if test="@type/. = 'Video'">
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
	<xsl:if test="not(../@soundwidth) and not(../@soundheight)">
		<xsl:attribute name="style">visibility:hidden</xsl:attribute>
	</xsl:if>
	<xsl:attribute name="id">media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" /></xsl:attribute>
</div>
<script type="text/javascript">
function playsnd_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />(id,src) {
	var width = 0;
	var height = 0;
	<xsl:if test="../@soundwidth">width = <xsl:value-of select="../@soundwidth"/>;</xsl:if>
	<xsl:if test="../@soundheight">height = <xsl:value-of select="../@soundheight"/>;</xsl:if>
	var code = "&lt;embed type='audio/x-wav' controls='console' hidden='false' autostart='true' width='" + width + "' height='" + height + "' loop='false' src='<xsl:value-of select="@name"/>'&gt;";
	try { document.getElementById(id).innerHTML = code; } catch(e) {}
}
</script>
					</xsl:if>
					<xsl:if test="@type/. = 'Video'">
<div><xsl:attribute name="id">media_<xsl:value-of select="position()" />_<xsl:value-of select="$self" /></xsl:attribute></div>
<script type="text/javascript">
function playmovie_<xsl:value-of select="position()" />_<xsl:value-of select="$self" />(id,src) {
	var width = 400;
	var height = 300;
	<xsl:choose>
		<xsl:when test="../@moviewidth">width = <xsl:value-of select="../@moviewidth"/>;</xsl:when><!-- try to get from label -->
		<xsl:when test="@movieWidth">width = <xsl:value-of select="@movieWidth"/>;</xsl:when><!-- get from original movie -->
	</xsl:choose>
	<xsl:choose>
		<xsl:when test="../@movieheight">height = <xsl:value-of select="number(../@movieheight)+20"/>;</xsl:when><!-- try to get from label -->
		<xsl:when test="@movieHeight">height = <xsl:value-of select="number(@movieHeight)+20"/>;</xsl:when><!-- get from original movie -->
	</xsl:choose>
	var code = "&lt;object width='" + width + "' height='" + height + "'&gt;"
				+ "&lt;param name='movie' value='<xsl:value-of select="@name"/>'/&gt;"
				+ "&lt;param name='controller' value='true'/&gt;"
				+ "&lt;embed src='<xsl:value-of select="@name"/>' width='" + width + "' height='" + height + "' controller='true'" + "'&gt;"
				+ "&lt;\/embed&gt;&lt;\/object&gt;";
	try { document.getElementById(id).innerHTML = code; } catch(e) {}
}
</script>
					</xsl:if>
				</xsl:for-each>

			</xsl:when>

			<!-- all other elements are copied -->
			<xsl:otherwise>
				<xsl:element name="{name()}">
					<xsl:if test="(../@backgroundColor != '') and (../@backgroundColor != '#000000')">
						<xsl:attribute name="style">background-color:<xsl:value-of select="../@backgroundColor" /></xsl:attribute>
					</xsl:if>
					<xsl:for-each select="@*">
						<xsl:choose>
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
	
							<!-- adjust class names to an existing external CSS -->
							<xsl:when test="(name() = 'class')">
								<!-- convert classname to lower case -->
								<xsl:variable name="classname">
									<xsl:call-template name="toLowerCase">
										<xsl:with-param name="thetext"><xsl:value-of select="." /></xsl:with-param>
									</xsl:call-template>
								</xsl:variable>
								<xsl:choose>
									<!-- pragraph styles torque -->
									<xsl:when test="($classname = 'p_headXX') or ($classname = 'p_in_brief_headXX')"><!-- titles -->
										<xsl:attribute name="{name()}">article_title</xsl:attribute>
									</xsl:when>
									<xsl:when test="$classname = 'p_deckXX'">
										<xsl:attribute name="{name()}">article_deck</xsl:attribute>
									</xsl:when>

									<!-- character styles torque -->
									<xsl:when test="$classname = 'c_body_boldXX'"><!-- bold text -->
										<xsl:attribute name="style">font-weight:bold</xsl:attribute>
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
				<!-- uncomment to add a break after each paragraph -->
				<!--
				<xsl:if test="name() = 'div'">
					<br/>
				</xsl:if>
				-->
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
		 the page PDF 
	     =============================================== -->
	<xsl:template name="set_pagePDF">
		<xsl:param name="page"/>
		<br/>
		<a>
			<xsl:attribute name="href"><xsl:value-of select="pagePDF/."/></xsl:attribute>
			<xsl:attribute name="title">PDF</xsl:attribute>
			<xsl:value-of select="pagePDF/."/>
		</a>
		<br/>
	</xsl:template>


	<!-- ===============================================
		 the document PDF 
	     =============================================== -->
	<xsl:template name="set_documentPDF">
		<xsl:param name="page"/>
		<a>
			<xsl:attribute name="href"><xsl:value-of select="/indd_document/documentPDF/."/></xsl:attribute>
			<xsl:attribute name="title">Doc PDF</xsl:attribute>
			<xsl:value-of select="/indd_document/documentPDF/."/>
		</a>
		<br/>
	</xsl:template>


	<!-- ===============================================
		 stuff to suppress
	     =============================================== -->
	<xsl:template match="textshortcut"></xsl:template>
	<xsl:template match="boxchain"></xsl:template>
	<xsl:template match="area"></xsl:template>
	<xsl:template match="paraopts"></xsl:template>


	<!-- ===============================================
		 show creator comment for this output
	     =============================================== -->
	<xsl:template name="show_creator">
			<xsl:param name="creationDate"/>
			<xsl:param name="outputVersion"/>
			<xsl:param name="inputPath"/>
			<xsl:param name="indesignDocname"/>
			<xsl:param name="sourceINXfileName"/>
<xsl:comment>
<xsl:if test="/indd_document/@demo_mode = 1">****** DEMO MODE!! This document contains scrambled text content!</xsl:if>
Creation Date: <xsl:value-of select="$creationDate" />
Output Version: <xsl:value-of select="$outputVersion" />
Input Path: <xsl:value-of select="$inputPath" />
Indesign Document Name: <xsl:value-of select="$indesignDocname" />
Source INX File Name: <xsl:value-of select="$sourceINXfileName" />
</xsl:comment>
	</xsl:template>






	<!-- ===============================================
		 Utilities functions
	     =============================================== -->
	<xsl:template name="toLowerCase">
		<xsl:param name="thetext"/>
		<xsl:variable name="lowercase">abcdefghijklmnopqrstuvwxyz</xsl:variable>
		<xsl:variable name="uppercase">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
		<xsl:value-of select="translate($thetext,$uppercase,$lowercase)"/>
	</xsl:template>



</xsl:stylesheet>
