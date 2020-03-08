<?php
$DEBUG = 0;							// set to 1 to show some debug messages
$username = "ft_slidebookWEB";		// the name of the user to login to database
$password = "mypassword";	// the password
$hostname = "127.0.0.1";			// the URL to the database
									// 127.0.0.1 = this machine
									// 123.45.67.8 = any IP address
									// www.mydomain.com = any domain name
									// www.mydomain.com:4306 = any domain name plus port if not default 3306
$dbname = "ft_flipbook";		// the database to use
$ft_tablename = "flipbook_1";		// the full-text search table to use
$objects_tablename = "objects";		// the table containing the objects descriptions
									//       set to "" if only 1 object is searchable
$browser_preferred_language = "";	// set to language code like 'en' 'fr' 'de' ... or leave empty to detect browser 's preferred language
$max_query_results = 100;			// number full-text search results to show
$max_query_chars = 300;				// number of character to show in full-text search results
$mark_results_color = "#ffff00";	// the color to use to mark search terms in result. empty to not mark results
$include_image_thumbs = 1;			// if article has attached image(s) show thumbnail
$thumbs_height = "60px";			// the height of thumbnail or empty for original size

/*
 * Override document url and paths
 * This mainly is for testing purposes to route data mentioned in the db to a different data web server */
$override_domain = "";				// set to new domain: the 'domain' field from the flipbook_x table will be replaced with this value
$override_root_datapath = "";		// set to new root data path: the 'root_datapath' field from the flipbook_x table will be replaced with this value
$override_path = "";				// set to new path: the 'path' field from the flipbook_x table will be replaced with this value

$dataRootPath = "/ePaper/DATA/";	// path from website root to data's base directory
$hostSubPath = "";					// path from host name to web root directory
?>