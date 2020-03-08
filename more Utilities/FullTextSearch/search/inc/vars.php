<?php
$DEBUG = 0;							// set to 1 to show some debug messages
$username = "ft_flipbookWEB";		// the name of the user to login to database
$password = "mypassword";			// the password
$hostname = "127.0.0.1";			// the URL to the database
									// 127.0.0.1 = this machine
									// 123.45.67.8 = any IP address
									// www.mydomain.com = any domain name
									// www.mydomain.com:4306 = any domain name plus port if not default 3306
$dbname = "ft_flipbook";			// the database to use
$ft_tablename = "flipbook_1";		// the full-text search table to use
$objects_tablename = "objects";		// the table containing the objects descriptions
									//       set to "" if only 1 object is searchable
$browser_preferred_language = "";	// set to language code like 'en' 'fr' 'de' ... or leave empty to detect browser 's preferred language
$max_query_results = 100;			// number full-text search results to show
$max_query_chars = 300;				// number of character to show in full-text search results
$mark_results_color = "#ffff00";	// the color to use to mark search terms in result. empty to not mark results
$include_image_thumbs = 1;			// if article has attached image(s) show thumbnail
$thumbs_height = "60px";			// the height of thumbnail or empty for original size

$ft_search_excl_article_page = "";	// empty to include results from all pages
									// 1 to exclude results from article_page 1 (page sequence)
									// "1,3,17" to exclude results from article_page 1, 3 and 17 (page sequence)
/*
 * Override document url and paths
 * This mainly is for testing purposes to route data mentioned in the db to a different data web server */
$override_domain = "";				// set to new domain: the 'domain' field from the flipbook_x table will be replaced with this value
$override_root_datapath = "";		// set to new root data path: the 'root_datapath' field from the flipbook_x table will be replaced with this value
$override_path = "";				// set to new path: the 'path' field from the flipbook_x table will be replaced with this value

$dataRootPath = "/ePaper/DATA/";	// path from website root to data's base directory
$hostSubPath = "";					// path from host name to web root directory

									// access password protected directories (protected by .htaccess). Usually this is the DATA folder
$AUTH_USER = "";					// user name to load file from the filesystem via http: (used by latest.php, function imagethumb() to access XML and image files)
$AUTH_PWD = "";						// password to load file from the filesystem via http: (used by latest.php, function imagethumb() to access XML and image files)

$default_publication_language = "en_US.UTF-8,eng_usa";	// the default language to use if not stated in the table 'objects.language'. default is "en_US.UTF-8,eng_usa"
//$default_publication_language = "de_DE.UTF-8,deu_deu";	// the german version
											// PHP on *nix uses the 2 letters version, PHP on Windows uses the 3 letter version
											// The 2-letters versions are for PHP running on *nix. add .UTF-8 if month names have incorrect encoding
											// The 3-letters versions are for PHP running on Windows. Do not add any encoding - this is converted to utf8 automatically
											//
											// Set to empty to not convert date strings to a language/country representation
											// Set to * (asterisk) to use the server's current locale
											// Example: for german language publications/Websites this should be set to: "de_DE.UTF-8,deu_deu"

											// How to format a publication date like '20120823' depending on the language like "en" or "fr"
											// The first locale is the default format for all not stated languages
$date_formattings = array(""=>"%B %e, %Y", 
						"en"=> "%B %e, %Y",
						"de"=> "%e. %B %Y",
						"fr"=> "%e %B %Y",
						"it"=> "%e %B %Y",
						"es"=> "%e %B %Y",	/* %e de %B de %Y  is too long */
						"p"=> "%e %B %Y"	/* %e de %B de %Y  is too long */
						);

$lang_monthnames = array(
						"en"=> array('January','February','March','April','May','June','July','August','September','October','November','December'),
						"de"=> array('Januar','Februar','März','April','Mai','Juni','Juli','August','September','Oktober','November','Dezember'),
						"fr"=> array('janvier','février','mars','avril','mai','juin','juillet','août','septembre','octobre','novembre','décembre'),
						"it"=> array('gennaio','febbraio','marzo','aprile','maggio','giugno','luglio','agosto','settembre','ottobre','novembre','dicembre'),
						"es"=> array('enero','febrero','marzo','abril','mayo','junio','julio','agosto','septiembre','octubre','noviembre','diciembre'),
						"p"=> array('Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro')
						);

/*
											// How to format a publication date like '20120823' depending on the locale like "en_US"
											// The first locale is the default format for all not stated languages
$date_formattings = array(""=>"%B %e, %Y", 
								"de_DE"=>"%e. %B %Y", "de_DE.UTF-8"=>"%e. %B %Y","deu_deu"=>"%e. %B %Y",
								"fr_FR"=>"%e %B %Y", "fr_FR.UTF-8"=>"%e %B %Y","fre_fre"=>"%e %B %Y",
								"en_US" => "%B %e, %Y", "en_US.UTF-8" => "%B %e, %Y", "eng_usa" => "%B %e, %Y"
							);
*/
?>