/**
 * Migrate from old databases (pre 2009 no PRIMARY KEYs) to the latest version
 */

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `flipbook_1_MIG` (
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


CREATE TABLE `objects_MIG` (
  `view_index` int(8) NOT NULL DEFAULT '0' COMMENT 'index sequence when building view list',
  `object_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'The id of the object (group) - usually the first 2-3 characters of the document name up to first underscore, divis, blank',
  `company` varchar(255) DEFAULT '' COMMENT 'the issuer''s name',
  `title` varchar(255) DEFAULT '' COMMENT 'the object''s title like ''New York Times''',
  `issue` varchar(255) DEFAULT '',
  `description` varchar(255) DEFAULT '',
  `link` varchar(255) DEFAULT '' COMMENT 'link to main site',
  `language` varchar(64) DEFAULT '' COMMENT 'object language code',
  `copyright` varchar(255) DEFAULT '' COMMENT 'copyright note',
  `ttl` int(8) NOT NULL DEFAULT '1440' COMMENT 'Time To Live (for RSS feed)',
  `timezone` varchar(255) NOT NULL DEFAULT '',
  KEY `idx_object_id` (`object_id`),
  KEY `idx_view_index` (`view_index`),
  PRIMARY KEY (`view_index`,`object_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


SET FOREIGN_KEY_CHECKS = 1;

/* copy data from old 'flipbook_1' table to new 'flipbook_1_MIG' table */
INSERT INTO `flipbook_1_MIG` (	`object_id`, 
								`domain`, 
								`root_datapath`, 
								`path`, 
								`name_html`, 
								`name_xml`, 
								`article_id`, 
								`article_page`, 
								`article_page_name`, 
								`content`, 
								`doc_src_type`, 
								`src_docname`, 
								`issue_year`, 
								`issue_date`, 
								`creation_date`, 
								`creation_time`, 
								`author`, 
								`title`, 
								`description`, 
								`rights`, 
								`subject`, 
								`images`
								)
		SELECT * FROM `flipbook_1`;

/* copy data from old 'objects' table to new 'objects_MIG' table */
INSERT INTO `objects_MIG` (`view_index`,`object_id`,`company`,`title`,`issue`,`description`)
		SELECT * FROM `objects`;

RENAME TABLE flipbook_1 TO flipbook_1_OLD,
			 flipbook_1_MIG TO flipbook_1,
			 objects TO objects_OLD,
			 objects_MIG TO objects;
