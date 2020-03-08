
CREATE database if not exists ft_flipbook;

USE ft_flipbook;


SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `flipbook_1` (
  `object_id` varchar(64) DEFAULT '' COMMENT 'The id of the object (group) - usually the first 2-3 characters of the document name up to first underscore, divis, blank',
  `domain` varchar(255) NOT NULL DEFAULT '' COMMENT 'The http:// domain name where the document is stored. May also contain the full URL to a document',
  `root_datapath` varchar(255) DEFAULT '' COMMENT 'the path to data tree seen from domain',
  `path` varchar(1024) DEFAULT '' COMMENT 'The subpath to the document from domain/root_datapath. May be empty if full path is in domain',
  `name_html` varchar(255) DEFAULT '' COMMENT 'The name of the html document. May be empty if full path is in domain',
  `name_xml` varchar(255) NOT NULL DEFAULT '' COMMENT 'The name of the xml document. May be empty if no xml document available',
  `article_id` varchar(64) NOT NULL DEFAULT '' COMMENT 'an article''s id - usually the index number',
  `prev_id` varchar(64) DEFAULT '' COMMENT 'for splitted articles, the previous article id. empty for non splitted articles',
  `cont_id` varchar(64) DEFAULT '' COMMENT 'for splitted articles, the continued article id. empty for non splitted articles',
  `article_page` varchar(24) NOT NULL DEFAULT '' COMMENT 'the page_sequence the article is placed on',
  `article_page_name` varchar(32) DEFAULT '' COMMENT 'the page_name the article is placed on',
  `content` mediumtext COMMENT 'the plain text content for fulltext search',
  `doc_src_type` varchar(24) DEFAULT '' COMMENT 'Document source type like: indd xqp',
  `src_docname` varchar(255) NOT NULL DEFAULT '' COMMENT 'Origianl InDesign document name',
  `issue_year` varchar(32) DEFAULT '' COMMENT 'issue year like 2008 - preferably take from a part of the document name',
  `issue_date` varchar(64) DEFAULT '' COMMENT 'issue date as YYYYMMDD like 20080423 - preferably take from a part of the document name',
  `creation_date` varchar(64) DEFAULT '' COMMENT 'Creation date like 20080423',
  `creation_time` varchar(64) DEFAULT '' COMMENT 'Creation time like 20:12:33',
  `author` varchar(255) DEFAULT '' COMMENT 'Name of author',
  `title` varchar(255) DEFAULT '' COMMENT 'The document''s title',
  `description` varchar(255) DEFAULT '' COMMENT 'The document description',
  `rights` varchar(255) DEFAULT '' COMMENT 'Copyright notice',
  `subject` varchar(255) DEFAULT '' COMMENT 'A list of space separated keywords',
  `images` text COMMENT 'attached images: image1.jpg,image1.eps;image2.jpg,image2.pdf;',
  `imagesmeta` text COMMENT 'the meta info contained in an image as an XML string',
  PRIMARY KEY `idx_path_xml_articleID` (`path`(150),`name_xml`(64),`article_id`(32)),
  KEY `idx_author` (`author`),
  KEY `idx_title` (`title`),
  KEY `idx_src_docname` (`src_docname`),
  KEY `idx_object_id` (`object_id`),
  KEY `idx_issue_date` (`issue_date`),
  KEY `idx_images` (`images`(250)),
  KEY `idx_content` (`content`(333)),
  FULLTEXT KEY `ft_content` (`content`),
  FULLTEXT KEY `ft_imagesmeta` (`imagesmeta`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `objects` (
  `view_index` int(8) NOT NULL DEFAULT '0' COMMENT 'index sequence when building view list',
  `object_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'The id of the object (group) - usually the first 2-3 characters of the document name up to first underscore, divis, blank',
  `company` varchar(255) DEFAULT '' COMMENT 'the issuer''s name',
  `title` varchar(255) DEFAULT '' COMMENT 'the publication''s title like ''New York Times''',
  `issue` varchar(255) DEFAULT '' COMMENT 'the issue name',
  `description` varchar(255) DEFAULT '' COMMENT 'short publication description',
  `long_description` varchar(8192) DEFAULT '' COMMENT '8192 bytes publication long description text',
  `link` varchar(255) DEFAULT '' COMMENT 'link to main site',
  `language` varchar(64) DEFAULT '' COMMENT 'object language code',
  `copyright` varchar(255) DEFAULT '' COMMENT 'copyright note',
  `ttl` int(8) NOT NULL DEFAULT '1440' COMMENT 'Time To Live (for RSS feed)',
  `timezone` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`view_index`,`object_id`),
  KEY `idx_object_id` (`object_id`),
  KEY `idx_view_index` (`view_index`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

insert into `objects` values('1','MN','My News Company','MyNews Magazine','','This is the test magazine','','','','',1440,'');

SET FOREIGN_KEY_CHECKS = 1;


CREATE USER 'BatchXSLT4Ind2DB'@localhost IDENTIFIED BY 'mypassword';
GRANT select, insert, update ON ft_flipbook.* TO BatchXSLT4Ind2DB@localhost;

CREATE USER 'BatchXSLT4Ind2DB'@'%' IDENTIFIED BY 'mypassword';
GRANT select, insert, update ON ft_flipbook.* TO BatchXSLT4Ind2DB@'%';




CREATE USER 'ft_flipbookWEB'@localhost IDENTIFIED BY 'mypassword';
GRANT select ON ft_flipbook.* TO ft_flipbookWEB@localhost;

CREATE USER 'ft_flipbookWEB'@'%' IDENTIFIED BY 'mypassword';
GRANT select ON ft_flipbook.* TO ft_flipbookWEB@'%';


