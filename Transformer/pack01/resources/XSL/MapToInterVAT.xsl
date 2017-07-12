<?xml version="1.0"?>
<xsl:stylesheet version="1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:a="http://www.reverseXSL.com/FreeParser" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<xsl:output standalone="yes" method="xml" indent="yes"/>
	<xsl:template match="/">
		<!--generate all elements without a namespace-->
		<xsl:element name="VATSENDING" namespace="">
			<xsl:attribute name="xsi:noNamespaceSchemaLocation">DeclarationTVA-1.5.xsd</xsl:attribute>
			<DECLARER>
				<VATNUMBER>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:VATNUMBER"/>
				</VATNUMBER>
				<NAME>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:NAME"/>
				</NAME>
				<ADDRESS>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:ADDRESS"/>
				</ADDRESS>
				<POSTCODE>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:POSTCODE"/>
				</POSTCODE>
				<CITY>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:CITY"/>
				</CITY>
				<COUNTRY>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:COUNTRY"/>
				</COUNTRY>
				<SENDINGREFERENCE>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:SENDINGREFERENCE"/>
				</SENDINGREFERENCE>
			</DECLARER>
			<VATRECORD>
				<RECNUM>
					<xsl:text>1</xsl:text>
				</RECNUM>
				<VATNUMBER>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:VATNUMBER"/>
				</VATNUMBER>
				<NAME>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:NAME"/>
				</NAME>
				<ADDRESS>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:ADDRESS"/>
				</ADDRESS>
				<POSTCODE>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:POSTCODE"/>
				</POSTCODE>
				<CITY>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:CITY"/>
				</CITY>
				<COUNTRY>
					<xsl:value-of select="a:VATSENDING/a:DECLARER/a:COUNTRY"/>
				</COUNTRY>
				<DPERIODE>
					<QUARTER>
						<xsl:value-of select="a:VATSENDING/a:VATRECORD/a:DPERIODE/a:QUARTER"/>
					</QUARTER>
					<YEAR>
						<xsl:value-of select="a:VATSENDING/a:VATRECORD/a:DPERIODE/a:YEAR"/>
					</YEAR>
				</DPERIODE>
				<ASK PAYMENT="NO" RESTITUTION="NO"/>
				<DATA>
					<DATA_ELEM>
						<!--re-order elements by increasing index and enforce implicit decimals-->
						<xsl:for-each select="a:VATSENDING/a:VATRECORD/a:DATA/a:DATA_ELEM/*">
							<xsl:sort select="local-name(.)"/>
							<xsl:element name="{local-name(.)}">
								<xsl:value-of select="round(number(.) * 100)"/>
							</xsl:element>
						</xsl:for-each>
					</DATA_ELEM>
				</DATA>
			</VATRECORD>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet><!-- Stylus Studio meta-information - (c) 2004-2008. Progress Software Corporation. All rights reserved.

<metaInformation>
	<scenarios>
		<scenario default="yes" name="InterVATfinish" userelativepaths="yes" externalpreview="no" url="VATdeclaration.xml" htmlbaseurl="" outputurl="" processortype="internal" useresolver="no" profilemode="0" profiledepth="" profilelength=""
		          urlprofilexml="" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext="" validateoutput="no" validator="internal"
		          customvalidator=""/>
	</scenarios>
	<MapperMetaTag>
		<MapperInfo srcSchemaPathIsRelative="yes" srcSchemaInterpretAsXML="no" destSchemaPath="DeclarationTVA-1.5.xsd" destSchemaRoot="VATSENDING" destSchemaPathIsRelative="yes" destSchemaInterpretAsXML="no">
			<SourceSchema srcSchemaPath="VATdeclaration.xml" srcSchemaRoot="VATSENDING" AssociatedInstance="" loaderFunction="document" loaderFunctionUsesURI="no"/>
		</MapperInfo>
		<MapperBlockPosition>
			<template match="/">
				<block path="VATSENDING/VATRECORD/DATA/DATA_ELEM/xsl:for-each" x="190" y="187"/>
				<block path="VATSENDING/VATRECORD/DATA/DATA_ELEM/xsl:for-each/local-name[1]" x="128" y="176"/>
				<block path="VATSENDING/VATRECORD/DATA/DATA_ELEM/xsl:for-each/xsl:element/xsl:value-of" x="149" y="132"/>
			</template>
		</MapperBlockPosition>
		<TemplateContext></TemplateContext>
		<MapperFilter side="source"></MapperFilter>
	</MapperMetaTag>
</metaInformation>
-->