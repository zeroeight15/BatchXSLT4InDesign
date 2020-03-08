<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title></title>
<script type="text/javascript">

var PIM_menu = {
PIM_debug: false,	// ad in HTML: <div id="debugmessage"></div>

PIM_menu: null,
PIM_menu_ul_array: new Array(),
PIM_over_curlevel: -1,
PIM_isTouchDevice: false,

detectTouchDevice: function() {
	var canCreateTouchEvent = null;
	try {
		canCreateTouchEvent = document.createEvent("TouchEvent");	// Google Chrom can create this even if it is not a touch device!!!
		if (canCreateTouchEvent != null) canCreateTouchEvent = true;
	} catch (e) { canCreateTouchEvent = false; }
	if (canCreateTouchEvent && (('ontouchstart' in window) === true)) return(true);
	if (canCreateTouchEvent && ((window.DocumentTouch && document instanceof DocumentTouch) === true)) return(true);
	return false;
},

PIM_close_levels: function(level) {
	if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + "-> closing level: " + level + " and deeper";
	for (var i = level; i < this.PIM_menu_ul_array.length; i++) {
		for (var m = 0; m < this.PIM_menu_ul_array[i].length; m++) {
			this.PIM_menu_ul_array[i][m].style.display = "none";
		}
	}

},

PIM_menuCloser_timeout: null,
PIM_menuCloser: function(firstcall) {
	if (this.PIM_menuCloser_timeout != null) this.PIM_menuCloser_timeout = null;;
	if (this.PIM_over_curlevel != -1) return(true);
	if (firstcall === true) {
		this.PIM_menuCloser_timeout = setTimeout(function() {PIM_menu.PIM_menuCloser(null);},500);
		return(true);
	}
	if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + "closing all levels";
	this.PIM_close_levels(1);
	return(true);
},

PIM_menuWaitActionTime: 5000,	// wait 5 seconds for no action before closing menu
PIM_menuWatcherInterval: 2000,	// check every x seconds
PIM_menuLastActionTime: 0,
PIM_menuActionWatcher_timeout: null,
PIM_menuActionWatcher: function() {	// watcher to close menu after a given time if no touch happens
	PIM_menu.PIM_menuActionWatcher_timeout = null;
	if (PIM_menu.PIM_over_curlevel < 0) {
		if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + " Watcher menus already closed";
		return(true);	// menu is not open
	}
	var now = new Date().getTime();
	if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + " Watcher check";
	if (now < (PIM_menu.PIM_menuLastActionTime + PIM_menu.PIM_menuWaitActionTime)) {
		if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + " Watcher self restart in: " + PIM_menu.PIM_menuWatcherInterval;
		PIM_menu.PIM_menuActionWatcher_timeout = null;
		PIM_menu.PIM_menuActionWatcher_timeout = setTimeout(function() { PIM_menu.PIM_menuActionWatcher(); }, PIM_menu.PIM_menuWatcherInterval); // restart watcher
		return(true);
	}
	// nothing happened: close menu
	PIM_menu.PIM_menuActionWatcher_timeout = null;
	if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + " Watcher closing menu";
	PIM_menu.PIM_over_curlevel = -1;
	PIM_menu.PIM_close_levels(1);	// close all levels
	return(true);
},
PIM_menuAction: function() {	// set action happened
	PIM_menu.PIM_menuLastActionTime = new Date().getTime();
	if (PIM_menu.PIM_menuActionWatcher_timeout == null) {
		PIM_menu.PIM_menuActionWatcher_timeout = setTimeout(function() { PIM_menu.PIM_menuActionWatcher()}, PIM_menu.PIM_menuWatcherInterval); // start watcher if not already started
	}
	if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + " menu action";
	return(true);
},

PIM_prepareMenu: function() {
    // first lets make sure the browser understands the DOM methods we will be using
  	if (!document.getElementsByTagName) return(false);
  	if (!document.getElementById) return(false);
  	
  	// init touch screen stuff
  	this.PIM_isTouchDevice = this.detectTouchDevice();
  	
  	// wait for the element is loaded
  	if (!document.getElementById("PIM_ul_menu")) {
  		setTimeout(function() {PIM_menu.PIM_prepareMenu();},500);
  		return;
  	}
	// get our menu
  	this.PIM_menu = document.getElementById("PIM_ul_menu");
	// store all out menu block levels
  	var menu_blocks = this.PIM_menu.getElementsByTagName("div");
  	for (var i = 0; i < menu_blocks.length; i++) {
  	    var ul = menu_blocks[i];
		if ((ul.className=="PIM_ul") || (ul.className=="PIM_ul_deepest")) {
			var curlevel = parseInt(ul.getAttribute("data-level"));
			if (typeof(this.PIM_menu_ul_array[curlevel]) == 'undefined') this.PIM_menu_ul_array[curlevel] = new Array();
			this.PIM_menu_ul_array[curlevel][this.PIM_menu_ul_array[curlevel].length] = ul;
		}
	}

  	// add event handlers to all a elements (menu items)
  	var menu_links = this.PIM_menu.getElementsByTagName("a");
  	var numhandlers = 0;
  	for (var i = 0; i < menu_links.length; i++) {
  	    var li = menu_links[i];
		// we have children - append hover function to the parent
		if ((li.className=="PIM_a") || (li.className=="PIM_a_title")) {
			numhandlers++;
			li.onmousedown = function (e) {
				var evt = e ? e : window.event;
				if (evt.stopPropagation) evt.stopPropagation();
				if (evt.preventDefault) evt.preventDefault();
				return(false);
			}
			li.onmouseover = function (e) {
						var parentBlock = this.parentNode.parentNode, curlevel;
						var ul = this.nextSibling;
						while (ul != null) {
							if ((ul.className=="PIM_ul") || (ul.className=="PIM_ul_deepest")) {
								if (PIM_menu.PIM_isTouchDevice) PIM_menu.PIM_menuAction();
								if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + "-> OVER: "+this.className;
								curlevel = parseInt(parentBlock.getAttribute("data-level"));
								PIM_menu.PIM_close_levels(curlevel+1);	// close unused levels
								PIM_menu.PIM_over_curlevel = curlevel;
								// open our sub-menu
								ul.style.display = "block";
								return(true);
							}
							ul = ul.nextSibling;
						}
						// if we are landing here then we are at the deepest level without submenus
						if ((ul == null) || (typeof(ul) == 'undefined')) {	// we are at the deepest level
								curlevel = parseInt(parentBlock.getAttribute("data-level"));
								PIM_menu.PIM_over_curlevel = curlevel;
								return(true);
						}
						return true;
					}
			// attach onmouseout event handler when we totally leave the menu from any nested level
			li.onmouseout = function (e) {
					if (PIM_menu.PIM_debug) document.getElementById("debugmessage").innerHTML = new Date().getTime() + "-> out: "+this.innerHTML;
					PIM_menu.PIM_over_curlevel = -1;
					setTimeout(function() {PIM_menu.PIM_menuCloser(true);},400);
					return(true);
				}
		}
	}
	//alert("handlers added: " + numhandlers);
	
  	return true;
}
}
//initialize/create our Publication/Issues Menu
PIM_menu.PIM_prepareMenu();

</script>
<style type="text/css">
#PIM_container { margin-left:200px;font-family:"Verdana","Arial","Tahoma",sans-serif; font-size:9pt; font-weight:normal; font-style:normal;  }
#PIM_content { width:170px; margin:0; padding:0; text-align:left; white-space:nowrap; font-size:1em; font-family:"Verdana","Arial","Tahoma",sans-serif; color:#333333; }
#PIM_ul_menu { margin:0; padding:0; }
.PIM_li_title { overflow:hidden;
	background-color:#e0e0e0;
	border:1px solid #555;
 	-webkit-border-top-left-radius:6px; -webkit-border-bottom-left-radius:6px; -webkit-border-top-right-radius:0; -webkit-border-bottom-right-radius:0;
	-moz-border-radius-topleft:6px; -moz-border-radius-bottomleft:6px; -moz-border-radius-topright:0; -moz-border-radius-bottomright:0;
	border-top-left-radius:6px; border-bottom-left-radius:6px; border-top-right-radius:0; border-bottom-right-radius:0;
}
.PIM_a_title { display:block; padding-left:0.8em; margin-right:0.3em; font-weight:normal; color:#333333; text-decoration:none; background-color:#f0f0f0; line-height:28px;
  	-webkit-border-top-left-radius:6px; -webkit-border-bottom-left-radius:6px; -webkit-border-top-right-radius:0; -webkit-border-bottom-right-radius:0;
	-moz-border-radius-topleft:6px; -moz-border-radius-bottomleft:6px; -moz-border-radius-topright:0; -moz-border-radius-bottomright:0;
	border-top-left-radius:6px; border-bottom-left-radius:6px; border-top-right-radius:0; border-bottom-right-radius:0;
}
.PIM_ul { display:none; padding:0; margin:0; position:absolute; margin-left:100px; border:1px solid #555; }
.PIM_ul_deepest { max-height:400px; overflow:auto; display:none; padding:0; margin:0; position:absolute; margin-left:100px; border:1px solid #222228; }
.PIM_li { text-align:left; white-space:nowrap; background-color:#e0e0e0; border:1px solid #cccccf; }
.PIM_a { display:block; width:150px; line-height:28px; overflow:hidden; padding-left:0.8em; margin-right:0.3em; font-weight:normal; color:#333333; text-decoration:none; background-color:#f0f0f0; }
.PIM_arr { display:inline-block; margin:0; margin-left:.5em; }
</style>
</head>

<body id="PIM_container">
<!-- /PIM_HTMHEAD -->
<!-- PIM_CONTENT -->
<div id="PIM_content">
	<!-- PIM_NAVIGATION -->
	<div id="PIM_ul_menu" data-level="0">
		<div class="PIM_li_title">
			<a class="PIM_a_title" href="#" title="Projects">Publications <span class="PIM_arr">&#9660;</span></a>
			<div class="PIM_ul" data-level="1">
				<div class="PIM_li">
					<a class="PIM_a" href="#" title="Publication #1">Beilage BZ, TT, BO, DB</a>
					<div class="PIM_ul" data-level="2">
						<div class="PIM_li">
							<a class="PIM_a" href="#" title="2012">2012</a>
							<div class="PIM_ul" data-level="3">
								<div class="PIM_li">
									<a class="PIM_a" href="#" title="Monat">März</a>
									<div class="PIM_ul_deepest" data-level="4" onmouseover="PIM_menu.PIM_over_curlevel=parseInt(this.datalevel);" onmouseout="PIM_menu.PIM_over_curlevel = -1; setTimeout('PIM_menu.PIM_menuCloser(true)',200);">
										<div class="PIM_li"><a class="PIM_a" href="http://192.168.1.36/BZB/epaper/DATA/Berner%20Zeitung/BZ-Beilage/BZB/2012/20120608/BZB_20120608.indb.htm" title="">20120608</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Mar. Any Other Publication</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Mar. And One More Publication</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Mar. Last Publication</a></div>
									</div>
								</div>
								<div class="PIM_li">
									<a class="PIM_a" href="#" title="Monat">Februar</a>
									<div class="PIM_ul" data-level="4">
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Feb. A Publication name</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Feb. Last Publication</a></div>
									</div>
								</div>
								<div class="PIM_li">
									<a class="PIM_a" href="#" title="Monat">Januar</a>
									<div class="PIM_ul" data-level="4">
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Jan. A Publication name</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Jan. Any Other Publication</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Jan. And One More Publication</a></div>
									</div>
								</div>
							</div>
						</div>
						<div class="PIM_li">
							<a class="PIM_a" href="#" title="2011">2011</a>
							<div class="PIM_ul" data-level="3">
								<div class="PIM_li"><a class="PIM_a" href="#" title="Monat">Dezember</a>
									<div class="PIM_ul" data-level="4">
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Dez. A Publication name</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Dez. Any Other Publication</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Dez. And One More Publication</a></div>
									</div>
								</div>
								<div class="PIM_li"><a class="PIM_a" href="#" title="Monat">November</a>
									<div class="PIM_ul" data-level="4">
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Nov. A Publication name</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Nov. Last Publication</a></div>
									</div>
								</div>
								<div class="PIM_li"><a class="PIM_a" href="#" title="Monat">Oktober</a>
									<div class="PIM_ul" data-level="4">
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Okt. A Publication name</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Okt. Any Other Publication</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Okt. And One More Publication</a></div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>

				<div class="PIM_li">
					<a class="PIM_a" href="#" title="Publication #1">Publication Name #2</a>
					<div class="PIM_ul" data-level="2">
						<div class="PIM_li"><a class="PIM_a" href="#" title="2012">2012</a>
							<div class="PIM_ul" data-level="3">
								<div class="PIM_li"><a class="PIM_a" href="#" title="Monat">Dezember</a>
									<div class="PIM_ul" data-level="4">
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Dez. A Publication name</a></div>
										<div class="PIM_li"><a class="PIM_a"href="#" title="">Dez. Any Other Publication</a></div>
										<div class="PIM_li"><a class="PIM_a"href="#" title="">Dez. And One More Publication</a></div>
									</div>
								</div>
								<div class="PIM_li"><a class="PIM_a" href="#" title="Monat">November</a>
									<div class="PIM_ul" data-level="4">
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Nov. A Publication name</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Nov. Last Publication</a></div>
									</div>
								</div>
								<div class="PIM_li"><a class="PIM_a" href="#" title="Monat">Oktober</a>
									<div class="PIM_ul" data-level="4">
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Okt. A Publication name</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Okt. Any Other Publication</a></div>
										<div class="PIM_li"><a class="PIM_a" href="#" title="">Okt. And One More Publication</a></div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- /PIM_NAVIGATION -->	
</div>
<!-- /PIM_CONTENT -->
<div id="debugmessage" style="margin-top:400px;"></div>
<!-- PIM_TRAILER -->
</body>
</html>
<!-- /PIM_TRAILER -->
