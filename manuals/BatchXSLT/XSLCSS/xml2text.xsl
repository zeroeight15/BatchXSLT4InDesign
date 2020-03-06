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

Short Description: XML to Plain Text
Description: Convert InDesign document content to plain unformated text. No images are exported.

Version: 46
Version date: 20200219
======================================================================
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				version="1.0">
	<xsl:output method="text" />
	<xsl:output media-type="text/plain"/>
	<xsl:output indent="yes"/>
	<xsl:output encoding="UTF-8"/>


	<xsl:variable name="showSeparators">0</xsl:variable><!-- default = 0. set to 1 to add page and article separators -->
	<xsl:variable name="debug_ctrlchar">0</xsl:variable><!-- default = 0. set to 1 to show control character codes within text in red -->


<!-- ================ HERE WE GO ================ -->
	<!-- create the XHTML ePaper pages -->
	<xsl:template match="/">
		<xsl:apply-templates select="indd_document" />
	</xsl:template>

	<xsl:template match="indd_document">


		<xsl:for-each select="page">

			<xsl:if test="$showSeparators != '0'">
				<xsl:text> ******** PAGE #</xsl:text><xsl:value-of select="@page_sequence" /><xsl:text> CONTENT: **** &#x0d;&#x0a;</xsl:text>
			</xsl:if>

			<!-- output the articles -->
			<xsl:call-template name="output_articles"/>

			<xsl:if test="$showSeparators != '0'">
				<xsl:text> ******** PAGE #</xsl:text><xsl:value-of select="@page_sequence" /><xsl:text> CONTENT: END **** &#x0d;&#x0a;</xsl:text>
			</xsl:if>
			
		</xsl:for-each>

	</xsl:template>






	<xsl:template name="output_articles"><!-- output the articles -->
		<xsl:if test="$showSeparators != '0'">
			<xsl:text> ******** ARTICLES ON PAGE #</xsl:text><xsl:value-of select="@page_sequence"/><xsl:text>: **** &#x0d;&#x0a;</xsl:text>
		</xsl:if>
		<xsl:for-each select="articles/article">
			<xsl:sort select="@t" data-type="number" order="ascending" /><!-- sort top down -->
			<xsl:sort select="@l" data-type="number" order="ascending" /><!-- sort left right -->
			<xsl:if test="descendant-or-self::content//*"><!-- dont show empty articles -->
				<xsl:choose>
					<xsl:when test="$showSeparators != '0'">
						<xsl:text> **** ARTICLE '</xsl:text><xsl:value-of select="@idx"/><xsl:text>' PAGE #</xsl:text><xsl:value-of select="@page_sequence"/><xsl:text>:  **** &#x0d;&#x0a;</xsl:text>
					</xsl:when>
					<xsl:otherwise>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:apply-templates select="* | text()"/>
				<xsl:choose>
					<xsl:when test="$showSeparators != '0'">
						<xsl:text> **** ARTICLE '</xsl:text><xsl:value-of select="@idx"/><xsl:text>' PAGE #</xsl:text><xsl:value-of select="@page_sequence"/><xsl:text>: END **** &#x0d;&#x0a;</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>___________________________________________________&#x0d;&#x0a;</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:for-each>
		<xsl:if test="$showSeparators != '0'">
			<xsl:text>&#x0d;&#x0a; ******** ARTICLES ON PAGE #</xsl:text><xsl:value-of select="@page_sequence"/><xsl:text>: END **** &#x0d;&#x0a;&#x0d;&#x0a;</xsl:text>
		</xsl:if>
	</xsl:template>


	<!-- get content -->
	<xsl:template match="content">
		<xsl:apply-templates select="* | text()"/>
	</xsl:template>


	<!-- get anchored objects -->
	<xsl:template match="anchored_object">
		<xsl:choose>
			<xsl:when test="@name = 'Note'">
				<xsl:if test="/indd_document/call_parameters/par[@name='excludeNotes']/. = '0'">
					<xsl:if test="$showSeparators != '0'">
						<xsl:text>-----NOTE:&#x0d;&#x0a;</xsl:text>
					</xsl:if>
					<xsl:apply-templates/>
					<xsl:if test="$showSeparators != '0'">
						<xsl:text>-----NOTE END&#x0d;&#x0a;</xsl:text>
					</xsl:if>
				</xsl:if>
			</xsl:when>
			<xsl:when test="(@name = 'Table') and (/indd_document/call_parameters/par[@name='TABLE_AS_BLOCK']/. = '1')">
				<xsl:apply-templates/>
			</xsl:when>
			<!-- footnotes -->
			<xsl:when test="(@name = 'FNcl')">
				<xsl:if test="$showSeparators != '0'">
					<xsl:text>-----NOTE:&#x0d;&#x0a;</xsl:text>
				</xsl:if>
				<xsl:apply-templates select="descendant::footnote_num | text()"/>
				<xsl:if test="$showSeparators != '0'">
					<xsl:text>&#x0d;&#x0a;-----NOTE END&#x0d;&#x0a;</xsl:text>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="$showSeparators != '0'">
					<xsl:text>-----ANCHORED:&#x0d;&#x0a;</xsl:text>
				</xsl:if>
				<xsl:apply-templates/>
				<xsl:if test="$showSeparators != '0'">
					<xsl:text>&#x0d;&#x0a;-----ANCHORED END&#x0d;&#x0a;</xsl:text>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- get content of Notes -->
	<xsl:template match="Note">
		<xsl:apply-templates select="* | text()"/>
	</xsl:template>

	<!-- get content of footnotes -->
	<xsl:template match="footnote">
				<xsl:if test="$showSeparators != '0'">
					<xsl:text>-----FOOTNOTE:&#x0d;&#x0a;</xsl:text>
				</xsl:if>
				<xsl:apply-templates select="* | text()"/>
				<xsl:if test="$showSeparators != '0'">
					<xsl:text>&#x0d;&#x0a;-----FOOTNOTE END:&#x0d;&#x0a;</xsl:text>
				</xsl:if>
	</xsl:template>
	<xsl:template match="footnote_num">
		<xsl:apply-templates select="* | text()"/>
	</xsl:template>


	<!-- get content of boxes -->
	<xsl:template match="box">
		<xsl:apply-templates select="* | text()"/>
	</xsl:template>


	<!-- get content of groups -->
	<xsl:template match="group">
		<xsl:apply-templates select="* | text()"/>
	</xsl:template>


	<!-- get content of xmlElement -->
	<xsl:template match="xmlElement">
		<xsl:apply-templates select="* | text()"/>
	</xsl:template>

	<!-- LINE SEPARATOR -->
	<xsl:template match="br"><xsl:text>&#x0d;&#x0a;</xsl:text></xsl:template><!-- <br type="LINE SEPARATOR"/> -->


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
			<xsl:when test="name()='img'"/><!-- suppress for plain text -->

			<!-- ******** div -->
			<xsl:when test="name()='div'">
				<xsl:apply-templates select="* | text()"/><xsl:text>&#x0d;&#x0a;</xsl:text>
			</xsl:when>

			<!-- ******** span -->
			<xsl:when test="name()='span'">
				<xsl:apply-templates select="* | text()"/>
			</xsl:when>

			<!-- ******** a links -->
			<xsl:when test="name()='a'">
				<xsl:apply-templates select="* | text()"/>
			</xsl:when>

			<!-- ******** table -->
			<xsl:when test="name()='table'">
				<xsl:text>&#x0d;&#x0a;</xsl:text>
				<xsl:apply-templates select="* | text()"/>
			</xsl:when>

			<!-- strip table settings -->
			<xsl:when test="name()='tablesettings'">
			</xsl:when>

			<!-- ******** table rows -->
			<xsl:when test="name()='tr'">
				<xsl:apply-templates select="* | text()"/>
				<xsl:text>&#x0d;&#x0a;</xsl:text>
			</xsl:when>

			<!-- ******** table cells -->
			<xsl:when test="name()='td'">
				<xsl:apply-templates select="* | text()"/>
				<xsl:text>&#x09;</xsl:text>
			</xsl:when>

			<!-- ******** push buttons -->
			<xsl:when test="name()='button'">
				<xsl:if test="@title">[<xsl:value-of select="@title" />]</xsl:if>
			</xsl:when>

			<!-- all other elements are copied -->
			<xsl:otherwise>
				<xsl:apply-templates select="* | text()"/>
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
		 stuff to suppress
	     =============================================== -->
	<xsl:template match="textshortcut"></xsl:template>
	<xsl:template match="boxchain"></xsl:template>
	<xsl:template match="area"></xsl:template>
	<xsl:template match="paraopts"></xsl:template>




</xsl:stylesheet>
