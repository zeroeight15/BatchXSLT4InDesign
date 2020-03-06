/*
===================================
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
www.aiedv.ch

DEPENDS on loaded:
	flipbook.js version 46
	needs variable: _sb_s.currentMetaViewPortScale

Version: 46
Version date: 20200219

Updates:
20140804 Ai: no show image in new full screen window when in an App
20140218 Ai: show exact touch point on image enlarged
20140122 Ai: added support for Windows pointer events
20130315 Ai: added support for multiple fingers to enlarge the image lightbox
===================================
*/

function showImage_lightbox() {

	// general working vars
	var DebugME = 0,				// set to > 0 to log all function calls
		_tmp, i,					// working var for unused or temporary stuff
		
		num_text_items = 5,			// number of language dependent string array elements
		num_languages = 5,			// number of languages
		ilblg = new Array(num_text_items),
		
		lb_ptr_evt,
			
		invalidXYpos = -99999999,	// flag any X Y position as 'not set'
		showImage_lightbox_image = null,
		showImage_lightbox_DOMimage = null,
		showImage_lightbox = null,
		showImage_lightbox_removeDraggable = null,
		showImage_lightbox_padding = {"left":0, "top":0, "right":0, "bottom":0},	// left, top, right, bottom padding
		showImage_lightbox_border = {"left":0, "top":0, "right":0, "bottom":0},	// left, top, right, bottom borders
		showImage_lightbox_content = null,
		showImage_lightbox_content_padding = {"left":0, "top":0, "right":0, "bottom":0},	// left, top, right, bottom padding
		showImage_lightbox_title = null,
		showImage_lightbox_title_height = 0,
		showImage_lightbox_lastX = invalidXYpos,	/* new positions after drag */
		showImage_lightbox_lastY = invalidXYpos,
		showImage_lightbox_oldBGcolor = "rgb(136, 136, 136)",	// init a color
	
		showImage_lightbox_timeout = null,
		showImage_lightbox_loadingtimeout = null,

		// settings for the image lightbox defined in image_lightbox.js
		_lb_settings = new Object();					// lightbox settings
		_lb_settings.draggable = 1;						// default => 1 to make lightbox draggable. Otherwise not draggable
		_lb_settings.dragFunction = null;				// caller may provide an own drag function
		_lb_settings.scaleToSize = 2;					// default => 1 = center to body width/height
														// 2 = center to window width/height
		_lb_settings.lightbox_2_window_padding = 2;	// the minimum padding from main window to this lightbox
		_lb_settings.top = -1;							// default = -1 = calculate and center in window
														// any other value is fixed position in pixels
		_lb_settings.left = -1;							// dito top
		_lb_settings.imageLoadIconLeft = "80px";		// left pos of loading icon  (sprocket.gif)
		_lb_settings.imageLoadIconTop = "80px";			// top pos of loading icon (sprocket.gif)
		_lb_settings.imageLoadIconWindowWidth = 200;	// width of loading icon window (sprocket.gif) (as number in pixels)
		_lb_settings.imageLoadIconWindowHeight = 200;	// height of loading icon window (sprocket.gif) (as number in pixels)
		_lb_settings.imageLoadIconBackgroundColor = "#fff";	// background color for loading icon window (sprocket.gif) (as hex number string)
		
		_lb_settings.containerWindowIsIFrame = runsInIFrame($(window));	// check if we load in an IFrame or in a top window
		_lb_settings.xslcssPath = "";					// the path to the XSLCSS folder

		_lb_settings.body = null;						// the body element to use
				try { _lb_settings.body = document.getElementsByTagName("body")[0]; } catch(e){}; // try to init the body element to use

		_lb_settings.currentMetaViewPortScale = 1.0;	// current viewport scale
		_lb_settings.windowWidth = 0;
		_lb_settings.windowHeight = 0;

		_lb_settings.body_offsetMarginLeft = 0;	// the body left margin
		_lb_settings.body_offsetMarginTop = 0;	// the body top margin
		_lb_settings.lightboxAdjustToWindowTimeout = 500;		// How long to wait before adjusting the lightbox size. default = 100 ms
		
		_lb_settings.silb_resizeHandlerTimeout = null;
		_lb_settings.imageWidth = null;
		_lb_settings.imageHeight = null;



	for (i=0; i<num_text_items; i++) { ilblg[i]=new Array(num_languages); }	//prepare sub-arrays for x languages
	
		/* language dependent strings array where languages are like this:
			ilblg[0][0]="english";
			ilblg[0][1]="deutsch";
			ilblg[0][2]="francais";
			ilblg[0][3]="dansk";
			ilblg[0][4]="polish";
			// more languages can be added
		 */
	ilblg[0][0]="Click to close this box";
	ilblg[0][1]="Klicken um diese Box zu schliessen";
	ilblg[0][2]="Cliquer pour fermer";
	ilblg[0][3]="Click to close this box";
	ilblg[0][4]="Click to close this box";
	ilblg[1][0]="Drag this";
	ilblg[1][1]="Ziehen zum Verschieben";
	ilblg[1][2]="Déplacer avec la souris";
	ilblg[1][3]="Drag this";
	ilblg[1][4]="Drag this";
	ilblg[2][0]="show image in new window";
	ilblg[2][1]="Bild in neuem Fenster zeigen";
	ilblg[2][2]="Cette image dans une nouvelle fenêtre";
	ilblg[2][3]="show image in new window";
	ilblg[2][4]="show image in new window";
	


	/*
	 * detect if we are running in an IFrame or in a 'normal' top window
	 */
	function runsInIFrame(win) {
		/*
			top window (normal windows) have window.frames [object DOMWindow]
			IFrames have window.frameElement  [object HTMLIFrameElement]
		*/
		var isIFrame = false;
		if (!win.frameElement) {	// top window has no frameElement
		}
		else {
		alert(typeof(win.frameElement));
			if (_sb_s.is_IE && (_sb_s.IEVersion <=7)) {
				if (typeof(win.frameElement) == "object") isIFrame = true;
			}
			else isIFrame = (win.frameElement && (win.frameElement+"").indexOf("HTMLIFrameElement") > -1);
		}
		//$.fn.log("log","win: " + win + "\n\twin.frameElement: " + win.frameElement + "\n\ttypeof win.frameElement: " + typeof(win.frameElement) + "\n\n\tisIFrame: " + isIFrame);
		return(isIFrame);
	}



	/*
	 * stop propagation of event
	 */
	function haltEvents(e) {
		if (e) {
			if (e.preventDefault) e.preventDefault();
			if (e.stopPropagation) e.stopPropagation();
			if (typeof(e.cancelBubble) != "undefined") e.cancelBubble = true;
		}
		return(false);
	}

	/*
	 * get the browser window width and height
	 */
	function get_window_dimensions() {
		/* this is urgently needed for iOS when in normal window (not in IFrame) */
		if (_sb_s.is_iPad || _sb_s.is_iPhone) {
			_lb_settings.windowWidth = (window.innerWidth ? window.innerWidth : document.body.clientWidth);
			if (!_lb_settings.containerWindowIsIFrame) _lb_settings.windowWidth *= _sb_s.currentMetaViewPortScale;	// WHEN NOT IN IFRAME: scale to viewport
			_lb_settings.windowWidth = Math.floor(_lb_settings.windowWidth);

			_lb_settings.windowHeight = (window.innerHeight ? window.innerHeight : document.body.clientHeight);
			if (!_lb_settings.containerWindowIsIFrame) _lb_settings.windowHeight *= _sb_s.currentMetaViewPortScale;	// WHEN NOT IN IFRAME: scale to viewport
			_lb_settings.windowHeight = Math.floor(_lb_settings.windowHeight);
			//$.fn.log("log","_sb_s.is_iPad || _sb_s.is_iPhone\n\twindow.innerWidth: " + window.innerWidth + ", innerHeight: " + window.innerHeight + "\n\t_sb_s.currentMetaViewPortScale: " + _sb_s.currentMetaViewPortScale);
		} 
		else {	// all other or Android
			_lb_settings.windowWidth = (window.innerWidth ? window.innerWidth : document.body.clientWidth);
			_lb_settings.windowHeight = (window.innerHeight ? window.innerHeight : document.body.clientHeight);
			//$.fn.log("log","is_Android or PC\n\twindow.innerWidth: " + window.innerWidth + ", innerHeight: " + window.innerHeight + "\n\t_sb_s.currentMetaViewPortScale: " + _sb_s.currentMetaViewPortScale);
		}
	}

	/*
	 * get the window and body dimension
	 */
	function get_window_body_coords() {
		// first window
		get_window_dimensions();
		// and then body
		_lb_settings.body_offsetMarginLeft = Math.ceil((_lb_settings.windowWidth - _lb_settings.body.offsetWidth) / 2);	// the body left margin
		_lb_settings.body_offsetMarginTop = _sb_e.body_marginTop;	// the body top margin
	}

	/*
	 * create the lightbox image HTML
	 */
	// <img class="showImage_lightbox_img" id="ilb_M497416898_img" src="../../../XSLCSS/flipbook/sprocket.gif">
	function imageLoadingHTML(handleStr, id, imageName) {
		if (DebugME > 0) $.fn.log("log","");
		var imageLoadingPrototype = "<img class=\"showImage_lightbox_img\" id=\"%id%_img\" src=\"%xslcssPath%sprocket.gif\">",
			str = imageLoadingPrototype.replace(/%handle%/g,handleStr);
		str = str.replace(/%id%/g,id);
		str = str.replace(/%name%/g,imageName);
		str = str.replace(/%xslcssPath%/g,_lb_settings.xslcssPath);
		return(str);
	}

	/*
	 * create the title HTML
	 */
	function lightboxTitleHTML(handleStr, id, imageName) {
		if (DebugME > 0) $.fn.log("log","");
		var lightboxTitlePrototype = "",
			str = "";
		lightboxTitlePrototype = ((_sb_s.inApp == false) ? "<span class=\"showImage_lightbox_title_newwindow\" id=\"%id%_newwindow\" on" + _sb_s.pointerEvents.end + "=\"return(showImage('%name%',1));\" title=\"" + ilblg[2][_sb_s.cur_lang_ID] + "\"></span>" : "");
		lightboxTitlePrototype += "<span class=\"showImage_lightbox_title_spacer\" id=\"%id%_spacer\"></span><span class=\"showImage_lightbox_title_close\" id=\"%id%_close\" on" + _sb_s.pointerEvents.end + "=\"return(%handle%.showImage_lightbox_fadeout(event));\" title=\"" + ilblg[0][_sb_s.cur_lang_ID] + "\"></span>";

		str = lightboxTitlePrototype.replace(/%handle%/g,handleStr);

		str = str.replace(/%id%/g,id);
		str = str.replace(/%name%/g,imageName);
		return(str);
	}

	/*
	 * create hash code from string
	 */
	String.prototype.hashCode = function(){
		var hash = 0, i, chr;
		if (this.length == 0) return hash;
		for (i = 0; i < this.length; i++) {
			chr = this.charCodeAt(i);
			hash = ((hash<<5)-hash)+chr;
			hash = hash & hash; // Convert to 32bit integer
		}
		return(hash);
	};

	/*
	 * create an ID from hashed string
	 */
	function getID(handle) {
		var hash = "" + handle.hashCode();
		hash = hash.replace(/\-/g,"M");	// no minus sign
		return("ilb_" + hash);
	}
	
	/*
	 * scale to mobile devices values
	 */
	function toMobiXY(val) {
		return(val);
		if (_sb_s.is_Android || (typeof(window.orientation) == 'undefined') || _lb_settings.containerWindowIsIFrame) return(val);
		return(Math.round(parseInt(val) / _sb_s.currentMetaViewPortScale));
	}

	/*
	 * set new x and y after drag
	 */
	function showImage_lightbox_setLastXY(e, x, y) {
			//$.fn.log("log","drag new X / Y: " + x + " / " +y);
		showImage_lightbox_lastX = x;
		showImage_lightbox_lastY = y;
	}

	lb_ptr_evt = {					// events stor for lightbox touch actions
			start: null,				// start ticks. if null pointerdown not taken place
			current: null,					// end ticks. if null pointerend not taken place
			end: null,					// end ticks. if null pointerend not taken place
			startXY: {
					x: null,			// pageX of start touch
					y: null				// pageY of start touch
					},
			endXY: {
					x: null,			// pageX of end touch
					y: null				// pageY of end touch
					},
			deltaXY: {
					x: 0,				// delta x after move
					y: 0				// delta y after move
					},
			isSwipe: false,				// true or false. if pointer was moved after start: true
			swipeTrigger: 1,				// how many pixels to move pointer before it is reported as a 'move'
			isTap: false,				// true or false. if pointer was moved after start: true
			tapTrigger: 200,			// how many ticks between start and end to be a tap

			imageXY: {					// current image top and left
					t: 0,				// delta x after move
					l: 0				// delta y after move
					},

			imageOrigWH: {				// original image width and height
					w: null,
					h: null
					}
	};
	// ad touch to the image element theimage
	function silb_touchHandler(e){
		//$.fn.log("log","event: " + e.type + "\ntarget: " + e.target.id);
	
		switch(e.type) {
			case _sb_s.pointerEvents.start:
				lb_ptr_evt.start = lb_ptr_evt.current = new Date().getTime();
				lb_ptr_evt.end = null;
				lb_ptr_evt.startXY = _sb_fn.get_pointerXY(e);
				lb_ptr_evt.endXY.x = lb_ptr_evt.endXY.y = null;
				lb_ptr_evt.isSwipe = false;
				lb_ptr_evt.isTap = false;
				lb_ptr_evt.deltaXY.x = _sb_s.lb_ptr_evt.deltaXY.y = 0;
				// set other event handlers
				//_sb_fn.addEventHandler(showImage_lightbox_DOMimage,_sb_s.pointerEvents.end + " " + _sb_s.pointerEvents.move + " " + _sb_s.pointerEvents.out + " " + _sb_s.pointerEvents.cancel,silb_touchHandler,false);
				_sb_fn.addEventHandler(showImage_lightbox_content,_sb_s.pointerEvents.end + " " + _sb_s.pointerEvents.move + " " + _sb_s.pointerEvents.out + " " + _sb_s.pointerEvents.cancel,silb_touchHandler,false);

				if (lb_ptr_evt.imageOrigWH.w === null) {
					lb_ptr_evt.imageOrigWH.w = parseFloat(showImage_lightbox_DOMimage.style.width);
					lb_ptr_evt.imageOrigWH.h = parseFloat(showImage_lightbox_DOMimage.style.height);
					// set enlarged image position to where we have tapped
					var ilbposX = _lb_settings.body_offsetMarginLeft + parseFloat(showImage_lightbox_content.offsetLeft) + showImage_lightbox_content_padding["left"] + showImage_lightbox_border["left"] + showImage_lightbox.offsetLeft,
						ilbposY = parseFloat(showImage_lightbox_content.offsetTop) + showImage_lightbox_content_padding["top"] + showImage_lightbox_border["top"]    + showImage_lightbox.offsetTop ,
						ilbwidth = parseFloat(showImage_lightbox_content.offsetWidth)   - showImage_lightbox_content_padding["left"] - showImage_lightbox_content_padding["right"] ,
					//	ilbheight = parseFloat(showImage_lightbox_content.offsetHeight) - showImage_lightbox_content_padding["top"]  - showImage_lightbox_content_padding["bottom"],
						tapposX = (lb_ptr_evt.startXY.x - ilbposX),
						tapposY = (lb_ptr_evt.startXY.y - ilbposY),
						scaleratio = showImage_lightbox_image.width / ilbwidth;
					tapposX = -(tapposX * scaleratio) + tapposX;
					tapposY = -(tapposY * scaleratio) + tapposY;
					/*
					$.fn.log("log","Window width: " + _lb_settings.windowWidth
							+ "\n\tbody left: " + _lb_settings.body_offsetMarginLeft
							+ "\n\torig img width: " + showImage_lightbox_image.width
							+ "\n\tscale: " + scaleratio
							+ "\n\tmouseX x mouseY: " + lb_ptr_evt.startXY.x + " x " + lb_ptr_evt.startXY.y
							+ "\n\tilbposX x ilbposY: " + ilbposX + " x " + ilbposY
							+ "\n\tilbwidth x ilbheight: " + ilbwidth + " x " + ilbheight
							+ "\n\ttapposX x tapposY: " + (lb_ptr_evt.startXY.x - ilbposX) + " x " + (lb_ptr_evt.startXY.y - ilbposY)
							+ "\n\tNEW tapposX x tapposY: " + tapposX + " x " + tapposY
							);
					*/
					showImage_lightbox_DOMimage.style.top = tapposY + "px";
					showImage_lightbox_DOMimage.style.left = tapposX + "px";
					
				}

				// scale image to original size
				//$.fn.log("log","orig W x H: " + showImage_lightbox_image.width + " x " + showImage_lightbox_image.height + "\ncurrent top/left: " + showImage_lightbox_DOMimage.style.top + "/" + showImage_lightbox_DOMimage.style.left);
				showImage_lightbox_DOMimage.style.width = showImage_lightbox_image.width + "px";
				showImage_lightbox_DOMimage.style.height = showImage_lightbox_image.height + "px";
				showImage_lightbox_DOMimage.style.cursor = "all-scroll";
				// store current top/left
				lb_ptr_evt.imageXY.t = parseFloat(showImage_lightbox_DOMimage.style.top);
				lb_ptr_evt.imageXY.l = parseFloat(showImage_lightbox_DOMimage.style.left);
				break;

			case _sb_s.pointerEvents.move:
				if (lb_ptr_evt.start === null) break;
				lb_ptr_evt.current = new Date().getTime();
				lb_ptr_evt.endXY = _sb_fn.get_pointerXY(e);
				if (lb_ptr_evt.endXY == null)break;
				lb_ptr_evt.deltaXY.x = lb_ptr_evt.endXY.x - lb_ptr_evt.startXY.x;
				lb_ptr_evt.deltaXY.y = lb_ptr_evt.endXY.y - lb_ptr_evt.startXY.y;
				if (   (Math.abs(lb_ptr_evt.deltaXY.x) > lb_ptr_evt.swipeTrigger)
					|| (Math.abs(lb_ptr_evt.deltaXY.y) > lb_ptr_evt.swipeTrigger)
					) lb_ptr_evt.isSwipe = true;
				else lb_ptr_evt.isSwipe = false;
				lb_ptr_evt.isTap = false;
				//$.fn.log("log","silb_touchHandler\n\te.type: " + e.type + "\n\tisSwipe: " + lb_ptr_evt.isSwipe + "\n\tdeltaXY: " + lb_ptr_evt.deltaXY.x + " x " + lb_ptr_evt.deltaXY.y);
				// set image position
				showImage_lightbox_DOMimage.style.top = (lb_ptr_evt.imageXY.t + lb_ptr_evt.deltaXY.y) + "px";
				showImage_lightbox_DOMimage.style.left = (lb_ptr_evt.imageXY.l + lb_ptr_evt.deltaXY.x) + "px";
				break;
			case _sb_s.pointerEvents.end:
			case _sb_s.pointerEvents.out:
			case _sb_s.pointerEvents.cancel:
				lb_ptr_evt.current = new Date().getTime();
				//alert(lb_ptr_evt.isSwipe);
				if (!lb_ptr_evt.isSwipe) {
					if ((lb_ptr_evt.current - lb_ptr_evt.start) <= lb_ptr_evt.tapTrigger) lb_ptr_evt.isTap = true;
					else lb_ptr_evt.isTap = false;
				}
				else lb_ptr_evt.isTap = false;
				lb_ptr_evt.start = lb_ptr_evt.end = null;
				//$.fn.log("log","event target: " + e.target.id + "\nisTap: " + lb_ptr_evt.isTap + "\nw x h: " + lb_ptr_evt.imageOrigWH.w + " x " + lb_ptr_evt.imageOrigWH.h);
				if (lb_ptr_evt.isTap && (lb_ptr_evt.imageOrigWH.w !== null)) {	// reset original W and H
					showImage_lightbox_DOMimage.style.top = "0px";
					showImage_lightbox_DOMimage.style.left = "0px";
					showImage_lightbox_DOMimage.style.width = lb_ptr_evt.imageOrigWH.w + "px";
					showImage_lightbox_DOMimage.style.height = lb_ptr_evt.imageOrigWH.h + "px";
					lb_ptr_evt.imageOrigWH.w = lb_ptr_evt.imageOrigWH.h = null;
				}
				//_sb_fn.removeEventHandler(showImage_lightbox_DOMimage,_sb_s.pointerEvents.end + " " + _sb_s.pointerEvents.move + " " + _sb_s.pointerEvents.out + " " + _sb_s.pointerEvents.cancel,silb_touchHandler,false);
				_sb_fn.removeEventHandler(showImage_lightbox_content,_sb_s.pointerEvents.end + " " + _sb_s.pointerEvents.move + " " + _sb_s.pointerEvents.out + " " + _sb_s.pointerEvents.cancel,silb_touchHandler,false);
				break;
		}
	
		return haltEvents(e);
	}


	/*
	 * show the lightbox
	 * 
	 * @param imageID The HTML image element to use for the image
	 * @param imgObj The javascript original image object or null (get the image size from this)
	 * @param imagename The url to the image
	 * @param imgW Optional the image width
	 * @param imgH Optional the image height
	 *
	 */
	function showImage_lightbox_fadein(imageID, imgObj, imagename, imgW, imgH) {
		if (DebugME > 0) $.fn.log("log","");
		if (showImage_lightbox_timeout != null) {
			clearTimeout(showImage_lightbox_timeout);
			showImage_lightbox_timeout = null;
		}

		lb_ptr_evt.imageOrigWH.w = lb_ptr_evt.imageOrigWH.h = null;

		var noImageScale = false,
			newtop = 0,  newleft = 0, newwidth = 0, newheight = 0, scaleX = 1.0, scaleY = 1.0, scale = 0.0, 
			imageWidth, imageHeight,
			offsetBodyMarginLeft = 0, 	// body offset margin left to use
			offsetBodyMarginTop = 0, 	// body offset margin top  to use
			titleheight = parseInt(showImage_lightbox_title_height),
			usableWindowWidth, usableWindowHeight;
			
		showImage_lightbox_DOMimage = document.getElementById(imageID);	// do we have the HTML image element?

		if (!showImage_lightbox_DOMimage) return(0);
		//$.fn.log("log","fadein imageID: " + imageID + "\n" + imagename + "\n\n" + document.getElementById(imageID) + "\n\ncomplete:" + showImage_lightbox_DOMimage.complete + "\n\nsrc:" + showImage_lightbox_DOMimage.src);
		if (!showImage_lightbox_DOMimage.complete) {
			showImage_lightbox_timeout = setTimeout(function(){showImage_lightbox_fadein(imageID, imgObj, imagename, imgW, imgH)},50);
			return(0);
		}
		showImage_lightbox_DOMimage.style.width = showImage_lightbox_DOMimage.style.height = "auto";		// reset scale

		// get body and window coords
		get_window_body_coords();

		// mobile devices always scale body to window size - except when in an IFrame
		if (typeof(window.orientation) != 'undefined') _lb_settings.scaleToSize = 2;	// size image lightbox to window
	
		switch (_lb_settings.scaleToSize) {	// size max to body size
			case 1:	// size max to body size
				usableWindowWidth = _lb_settings.body.offsetWidth - (2*_lb_settings.lightbox_2_window_padding)
									- showImage_lightbox_padding["left"] - showImage_lightbox_padding["right"] - showImage_lightbox_border["left"] - showImage_lightbox_border["right"]
									- showImage_lightbox_content_padding["left"] - showImage_lightbox_content_padding["right"];
				offsetBodyMarginLeft = 0;
				usableWindowHeight = _lb_settings.body.offsetHeight - (2*_lb_settings.lightbox_2_window_padding)
									- showImage_lightbox_padding["top"] - showImage_lightbox_padding["bottom"] - showImage_lightbox_border["top"] - showImage_lightbox_border["bottom"]
									- showImage_lightbox_content_padding["top"] - showImage_lightbox_content_padding["bottom"]
									- titleheight;
									//- 2;	// rendering security
				//offsetBodyMarginTop = titleheight - _lb_settings.lightbox_2_window_padding;
				offsetBodyMarginTop = _lb_settings.lightbox_2_window_padding;
				break;
			default:	// size max to window size
				usableWindowWidth = _lb_settings.windowWidth - (2*_lb_settings.lightbox_2_window_padding)
									- showImage_lightbox_border["left"] - showImage_lightbox_border["right"]
									- showImage_lightbox_padding["left"] - showImage_lightbox_padding["right"]
									- showImage_lightbox_content_padding["left"] - showImage_lightbox_content_padding["right"];
			//	if (typeof(window.orientation) != 'undefined') offsetBodyMarginLeft = 0;	// mobile device always scale body to window width (normal window or IFrame window
			//	else offsetBodyMarginLeft = _lb_settings.body_offsetMarginLeft;
				offsetBodyMarginLeft = _lb_settings.body_offsetMarginLeft;
				usableWindowHeight = _lb_settings.windowHeight - (2*_lb_settings.lightbox_2_window_padding)
									- showImage_lightbox_border["top"] - showImage_lightbox_border["bottom"]
									- showImage_lightbox_padding["top"] - showImage_lightbox_padding["bottom"]
									- titleheight
									- showImage_lightbox_content_padding["top"] - showImage_lightbox_content_padding["bottom"];
									//- 2;	// rendering security
				//offsetBodyMarginTop = titleheight - _lb_settings.lightbox_2_window_padding;
				offsetBodyMarginTop = _lb_settings.lightbox_2_window_padding;
				break;
		}

		// get image dimensions
		//alert("imgObj:"+imgObj);
		if (imgObj) {
			imageWidth = imgObj.width;
			imageHeight = imgObj.height;
			 // alert("1 imageWidth:"+imageWidth+"\nimageHeight:"+imageHeight);
		}
		if ((imageWidth == 0) || (imageHeight == 0)) {
			if (imgW && imgH) {
				imageWidth = imgW;
				imageHeight = imgH;
				//	alert("2 imageWidth:"+imageWidth+"\nimageHeight:"+imageHeight);
			} else {	// get from HTML element
				if (_sb_s.is_IE) showImage_lightbox.style.display = "block";	// IE can not calculate image size when display:none
				imageWidth = showImage_lightbox_DOMimage.width;
				imageHeight = showImage_lightbox_DOMimage.height;
				//  alert("3 imageWidth:"+imageWidth+"\nimageHeight:"+imageHeight);
			}
		}

		// calc needed width and height and scale factor
		if ((showImage_lightbox_DOMimage.src.indexOf("XSLCSS/") < 0)
			&& (showImage_lightbox_DOMimage.src.indexOf("/sprocket.gif") < 0)) {	// a normal image
			showImage_lightbox_content.style.backgroundColor = showImage_lightbox_oldBGcolor;
			showImage_lightbox_DOMimage.style.left = 0;
			showImage_lightbox_DOMimage.style.top = 0;
	
			if (imageWidth > usableWindowWidth) {	// calc x resize scale
				scaleX = usableWindowWidth / imageWidth;
			}
			if (imageHeight > usableWindowHeight) {	// calc y resize scale
				scaleY = usableWindowHeight / imageHeight;
			}
			if ((scaleX > 0) || (scaleY > 0)) {	// get larger scale
				scale = Math.min(scaleX,scaleY);
			}
			
			// set width
			if (scale != 0.0) {	// have to scale
				newwidth = Math.floor(imageWidth * scale);
				newheight = Math.floor(imageHeight * scale);
			}
			else {
				newwidth = imageWidth;
				newheight = imageHeight;
			}
		}
		else {	// is the waiting sprocket gif
			showImage_lightbox_content.style.backgroundColor = _lb_settings.imageLoadIconBackgroundColor;
			showImage_lightbox_DOMimage.style.position = "relative";
			showImage_lightbox_DOMimage.style.left = _lb_settings.imageLoadIconLeft;
			showImage_lightbox_DOMimage.style.top = _lb_settings.imageLoadIconTop;
			noImageScale = true;
			newwidth = _lb_settings.imageLoadIconWindowWidth;
			newheight = _lb_settings.imageLoadIconWindowHeight;
		}
		
		if (newwidth < 100) { newwidth = 100; noImageScale = true; }		// constrain to minimum 100px
		if (newheight < 100) { newheight = 100; noImageScale = true; }
	
		// calc left/top position of lightbox
		if (showImage_lightbox_lastX == invalidXYpos) {
			if (parseInt(_lb_settings.left) < 0) newleft = Math.floor((usableWindowWidth - newwidth) / 2) + _lb_settings.lightbox_2_window_padding;
			else newleft = parseInt(_lb_settings.left);
		}
		else newleft = showImage_lightbox_lastX;
	
		if (showImage_lightbox_lastY == invalidXYpos) {
			if (parseInt(_lb_settings.top) < 0) newtop = Math.floor((_lb_settings.windowHeight 	
																		- (showImage_lightbox_border["top"] + showImage_lightbox_padding["top"] + titleheight + showImage_lightbox_content_padding["top"] + newheight + showImage_lightbox_content_padding["bottom"] + showImage_lightbox_padding["bottom"] + showImage_lightbox_border["bottom"])	// lightbox height
																	) / 2 )
														- Math.ceil(_lb_settings.body_offsetMarginTop / 2);	//	outside the body container
			else newtop = parseInt(_lb_settings.top);
		}
		else newtop = showImage_lightbox_lastY;

		// subtract a body margin if ....
		if (showImage_lightbox_lastY == invalidXYpos) {
			newleft -= offsetBodyMarginLeft;
			newtop -= offsetBodyMarginTop;
	
			newleft = toMobiXY(newleft);
			newtop = toMobiXY(newtop);
			newwidth = toMobiXY(newwidth);
			newheight = toMobiXY(newheight);
		}
		else newtop -= offsetBodyMarginTop;

	//	newtop += showImage_lightbox_padding["top"];

	//	if (newtop < 0) newtop = 0;		// constrain to top of window
	//	if (newleft < 0) newleft = 0;	// constrain to top of window
	
		if (!isNaN(newleft)) newleft += "px";
		if (!isNaN(newtop)) newtop += "px";
		if (noImageScale) {
			showImage_lightbox_content.style.width = newwidth + "px";	// scale the div container
			showImage_lightbox_content.style.height = newheight + "px";	// scale the div container
			if (_sb_s.is_IE && (_sb_s.IEVersion <= 7)) {	// IE7 hack
				showImage_lightbox.style.width = (showImage_lightbox_padding["left"] + newwidth /* + showImage_lightbox_padding["right"] */) + "px";	// size the lightbox container
				showImage_lightbox.style.height = (titleheight + showImage_lightbox_padding["top"] + newheight + showImage_lightbox_padding["bottom"]) + "px";
			}
		}
		else {
			showImage_lightbox_DOMimage.style.width = newwidth + "px";		// scale the image
			showImage_lightbox_DOMimage.style.height = newheight + "px";	// scale the image
			//showImage_lightbox_content.style.width = "auto";	// unscale the div container
			//showImage_lightbox_content.style.height = "auto";	// unscale the div container
			showImage_lightbox_content.style.overflow = "hidden";	// at least Firefox has a 1 pixel vertical bug which makes show scrollbars
			showImage_lightbox_content.style.width = newwidth + "px";	// same size for div container
			showImage_lightbox_content.style.height = newheight + "px";	// same size for div container
			if (_sb_s.is_IE && (_sb_s.IEVersion <= 7)) {	// IE7 hack
				showImage_lightbox.style.width = (showImage_lightbox_padding["left"] + newwidth /* + showImage_lightbox_padding["right"] */) + "px";	// size the lightbox container
				showImage_lightbox.style.height = (titleheight + showImage_lightbox_padding["top"] + newheight + showImage_lightbox_padding["bottom"]) + "px";
			}
		}
		showImage_lightbox.style.left = newleft;
		showImage_lightbox.style.top = newtop;
		/*
		$.fn.log("log","Caller: " + arguments.callee.caller.name
				+ "\n\tsrc: " + showImage_lightbox_DOMimage.src + "\n\tcomplete: " + showImage_lightbox_DOMimage.complete 
				+ "\n\twindow.orientation: " + window.orientation
				+ "\n\tis_MobileOS: " + _sb_s.is_MobileOS
				+ "\n\tcurrentMetaViewPortScale: " + _sb_s.currentMetaViewPortScale 
				+ "\n\tImage width: " + imageWidth + ", height: " + imageHeight
				+ "\n\twindowWidth: " + _lb_settings.windowWidth + ", windowHeight: " + _lb_settings.windowHeight 
				+ "\n\tbodyWidth: " + _lb_settings.body.offsetWidth + ", bodyHeight: " + _lb_settings.body.offsetHeight 
				+ "\n\ttitleheight: " + titleheight 
				+ "\n\toffsetBodyMargin Left: " + offsetBodyMarginLeft + ", Top: " + offsetBodyMarginTop
				+ "\n\tlightbox_2_window_padding: " + _lb_settings.lightbox_2_window_padding
				+ "\n\tshowImage_lightbox_padding l,t,r,b: " + showImage_lightbox_padding["left"] + ", " + showImage_lightbox_padding["top"] + ", " + showImage_lightbox_padding["right"] + ", " + showImage_lightbox_padding["bottom"]
				+ "\n\tshowImage_lightbox_border l,t,r,b: " + showImage_lightbox_border["left"] + ", " + showImage_lightbox_border["top"] + ", " + showImage_lightbox_border["right"] + ", " + showImage_lightbox_border["bottom"]
				+ "\n\tshowImage_lightbox_content_padding l,t,r,b: " + showImage_lightbox_content_padding["left"] + ", " + showImage_lightbox_content_padding["top"] + ", " + showImage_lightbox_content_padding["right"] + ", " + showImage_lightbox_content_padding["bottom"]
				+ "\n\tusableWindowWidth: " + usableWindowWidth + ", usableWindowHeight: " + usableWindowHeight 
				+ "\n\ttitleHeight: " + titleheight
				+ "\n\tscaleX: " + scaleX + ", scaleY: " + scaleY + ", scale: " + scale
				+ "\n\tlastX: " + showImage_lightbox_lastX + ", lastY: " + showImage_lightbox_lastY
				+ "\n\tnewleft: " + newleft + ", newtop: " + newtop
				+ "\n\tnewwidth: " + newwidth + ", newheight: " + newheight
				);
		*/
		showImage_lightbox.style.display = "block";

		// showImage_lightbox_fadein handler on window resize
		function silb_resizeHandler(){
			if (_lb_settings.silb_resizeHandlerTimeout != null) {
				clearTimeout(_lb_settings.silb_resizeHandlerTimeout);
				_lb_settings.silb_resizeHandlerTimeout = null;
			}
			_lb_settings.silb_resizeHandlerTimeout = setTimeout(function(){showImage_lightbox_fadein(imageID, imgObj, imagename, imgW, imgH);},_lb_settings.lightboxAdjustToWindowTimeout);
		}

		// add window resize listener
		if (!_sb_s.is_MobileOS) {
			_sb_fn.removeEventHandler(window,'resize',silb_resizeHandler,false);	// first remove the current handler
			setTimeout(function(){_sb_fn.addEventHandler(window,'resize',silb_resizeHandler,false);},100);
		}
		else {
			_sb_fn.removeEventHandler(window,'orientationchange',silb_resizeHandler,false);	// first remove the current handler
			setTimeout(function(){_sb_fn.addEventHandler(window,'resize',silb_resizeHandler,false);},100);
		}
		
		//_sb_fn.addEventHandler(showImage_lightbox_DOMimage,_sb_s.pointerEvents.start,silb_touchHandler,false);
		_sb_fn.addEventHandler(showImage_lightbox_content,_sb_s.pointerEvents.start,silb_touchHandler,false);
		
		return(0);
	}



	
	/*
	 * set new image in the lightbox
	 */
	function showImage_lightbox_setimage(id, handle, name) {
		if (DebugME > 0) $.fn.log("log","");

		// clear the timeout to show loading icon
		if (showImage_lightbox_loadingtimeout != null) {
			clearTimeout(showImage_lightbox_loadingtimeout); showImage_lightbox_loadingtimeout = null;
		}
		
		//$.fn.log("log","setimage W: " + showImage_lightbox_image.width + ", H: " + showImage_lightbox_image.height);

		//alert("showImage_lightbox_setimage: " + id + "\n\nimage to show: " + name);
		var lightboximage = document.getElementById(id);
		if (!lightboximage) return;
		//alert("lightbox image id found!\nnew image is: " + showImage_lightbox_image.src);
		lightboximage.src = name;
		showImage_lightbox_fadein(id, showImage_lightbox_image, name, _lb_settings.imageWidth, _lb_settings.imageHeight);
	}
	
	/*
	 * handle successfully loaded external images
	 */
	function showImage_lightbox_loaded(id, handle, name) {
		if (DebugME > 0) $.fn.log("log","");

		var loadfunction = handle + ".showImage_lightbox_setimage(\"" +id + "_img\",\"" + handle + "\",\"" + name + "\")";
		//alert("loaded: " + loadfunction);
		setTimeout(loadfunction,1);
	}
	
	/*
	 * handle error while loading external images
	 */
	function showImage_lightbox_error(e) {
		if (DebugME > 0) $.fn.log("log","");
		showImage_lightbox_fadeout(e);
	}
	
	/*
	 * hide the lightbox
	 */
	function showImage_lightbox_fadeout(e) {
		if (DebugME > 0) $.fn.log("log","");
		// remove window resize listener
		if (!_sb_s.is_MobileOS) {
			_sb_fn.removeEventHandler(window,'resize',showImage_lightbox_fadein.silb_resizeHandler,false);
		}
		else {
			_sb_fn.removeEventHandler(window,'orientationchange',showImage_lightbox_fadein.silb_resizeHandler,false);
		}
		if (showImage_lightbox_timeout != null) {
			clearTimeout(showImage_lightbox_timeout);
			showImage_lightbox_timeout = null;
		}
		if (e) {
			if (e.preventDefault) e.preventDefault();
			if (e.stopPropagation) e.stopPropagation();
			if (typeof(e.cancelBubble) != "undefined") e.cancelBubble = true;
			//$.fn.log("log","event Type: " +e.type + ", Shift: " + e.shiftKey + "\nstopPropagation: " + e.stopPropagation + "\ncancelBubble: " + e.cancelBubble);
			if (e.shiftKey || e.altKey) {
				showImage_lightbox_lastX = invalidXYpos;	/* reset drag positions */
				showImage_lightbox_lastY = invalidXYpos;
			}
		}
		if (!showImage_lightbox_content) return(haltEvents(e));
		//  make sure: remove img event handlers
		//_sb_fn.removeEventHandler(showImage_lightbox_DOMimage,_sb_s.pointerEvents.start + " " +_sb_s.pointerEvents.end + " " + _sb_s.pointerEvents.move + " " + _sb_s.pointerEvents.out + " " + _sb_s.pointerEvents.cancel,silb_touchHandler,false);
		_sb_fn.removeEventHandler(showImage_lightbox_content,_sb_s.pointerEvents.start + " " +_sb_s.pointerEvents.end + " " + _sb_s.pointerEvents.move + " " + _sb_s.pointerEvents.out + " " + _sb_s.pointerEvents.cancel,silb_touchHandler,false);
		showImage_lightbox_image = null;

		showImage_lightbox_content.innerHTML = "";
		showImage_lightbox.style.display="none";
		showImage_lightbox.style.left = "-10000px";
		showImage_lightbox.style.top = "-10000px";
		return(haltEvents(e));
	}
	
	/*
	 * make our presets
	 */
	function makePresets(presets) {
		if (typeof(presets.draggable) != "undefined") _lb_settings.draggable = presets.draggable;
		if (typeof(presets.dragFunction) != "undefined") _lb_settings.dragFunction = presets.dragFunction;	

		if (typeof(presets.scaleToSize) != "undefined") _lb_settings.scaleToSize = presets.scaleToSize;
														
		if (typeof(presets.lightbox_2_window_padding) != "undefined") _lb_settings.lightbox_2_window_padding = presets.lightbox_2_window_padding;	

		if (typeof(presets.top) != "undefined") _lb_settings.top = presets.top;																		
		if (typeof(presets.left) != "undefined") _lb_settings.left = presets.left;							

		if (typeof(presets.imageLoadIconLeft) != "undefined") _lb_settings.imageLoadIconLeft = presets.imageLoadIconLeft;		
		if (typeof(presets.imageLoadIconTop) != "undefined") _lb_settings.imageLoadIconTop = presets.imageLoadIconTop;			
		if (typeof(presets.imageLoadIconWindowWidth) != "undefined") _lb_settings.imageLoadIconWindowWidth = presets.imageLoadIconWindowWidth;	
		if (typeof(presets.imageLoadIconWindowHeight) != "undefined") _lb_settings.imageLoadIconWindowHeight = presets.imageLoadIconWindowHeight;	
		if (typeof(presets.imageLoadIconBackgroundColor) != "undefined") _lb_settings.imageLoadIconBackgroundColor = presets.imageLoadIconBackgroundColor;
		
		if (typeof(presets.body) != "undefined") _lb_settings.body = presets.body;
		if (typeof(presets.xslcssPath) != "undefined") _lb_settings.xslcssPath = presets.xslcssPath;
	}

	/*
	 * setup/initialize the lightbox
	 */
	function showImage_lightbox_setup(handleStr, id, imageName) {
		if (DebugME > 0) $.fn.log("log","");
		showImage_lightbox = document.createElement('div');
		showImage_lightbox.className = "showImage_lightbox";
		showImage_lightbox.id = id + "_lightbox";
		showImage_lightbox.innerHTML = "<div class=\"showImage_lightbox_title\" id=\"" + showImage_lightbox.id + "_title\" title=\"" + ilblg[1][_sb_s.cur_lang_ID] + "\">" + lightboxTitleHTML(handleStr, id, imageName) + "</div><div class=\"showImage_lightbox_content\" id=\"" + showImage_lightbox.id + "_content\"></div>";
		_lb_settings.body.appendChild(showImage_lightbox);

		showImage_lightbox_title = document.getElementById(id + '_title');
		showImage_lightbox_content = document.getElementById(id + '_lightbox_content');
		// in local files, Firefox can not access the css at the higher path at ../XSLCSS....
		// all CSS values are 'undefined' in this case and we have to assume some values.
		// in online mode, CSS access is fine.
		showImage_lightbox_title_height = _sb_fn.get_css_value("showImage_lightbox_title","height");	// try to get stuff from css
		if (!showImage_lightbox_title_height) showImage_lightbox_title_height = 36;
		showImage_lightbox_oldBGcolor = _sb_fn.get_css_value("showImage_lightbox_content","backgroundColor");
		if (!showImage_lightbox_oldBGcolor) showImage_lightbox_oldBGcolor = "transparent";
		showImage_lightbox_padding["left"] = parseInt(_sb_fn.get_css_value("showImage_lightbox","paddingLeft"));
		if (!showImage_lightbox_padding["left"]) showImage_lightbox_padding["left"] = 5;
		showImage_lightbox_padding["top"] = parseInt(_sb_fn.get_css_value("showImage_lightbox","paddingTop"));
		if (!showImage_lightbox_padding["top"]) showImage_lightbox_padding["top"] = 5;
		showImage_lightbox_padding["right"] = parseInt(_sb_fn.get_css_value("showImage_lightbox","paddingRight"));
		if (!showImage_lightbox_padding["right"]) showImage_lightbox_padding["right"] = 5;
		showImage_lightbox_padding["bottom"] = parseInt(_sb_fn.get_css_value("showImage_lightbox","paddingBottom"));
		if (!showImage_lightbox_padding["bottom"]) showImage_lightbox_padding["bottom"] = 5;
		showImage_lightbox_border["left"] = parseInt(_sb_fn.get_css_value("showImage_lightbox","borderLeftWidth"));
		if (!showImage_lightbox_border["left"]) showImage_lightbox_border["left"] = 1;
		showImage_lightbox_border["top"] = parseInt(_sb_fn.get_css_value("showImage_lightbox","borderTopWidth"));
		if (!showImage_lightbox_border["top"]) showImage_lightbox_border["top"] = 1;
		showImage_lightbox_border["right"] = parseInt(_sb_fn.get_css_value("showImage_lightbox","borderRightWidth"));
		if (!showImage_lightbox_border["right"]) showImage_lightbox_border["right"] = 2;
		showImage_lightbox_border["bottom"] = parseInt(_sb_fn.get_css_value("showImage_lightbox","borderBottomWidth"));
		if (!showImage_lightbox_border["bottom"]) showImage_lightbox_border["bottom"] = 2;

		showImage_lightbox_content_padding["left"] = parseInt(_sb_fn.get_css_value("showImage_lightbox_content","paddingLeft"));
		if (!showImage_lightbox_content_padding["left"]) showImage_lightbox_content_padding["left"] = 2;
		showImage_lightbox_content_padding["top"] = parseInt(_sb_fn.get_css_value("showImage_lightbox_content","paddingTop"));
		if (!showImage_lightbox_content_padding["top"]) showImage_lightbox_content_padding["top"] = 2;
		showImage_lightbox_content_padding["right"] = parseInt(_sb_fn.get_css_value("showImage_lightbox_content","paddingRight"));
		if (!showImage_lightbox_content_padding["right"]) showImage_lightbox_content_padding["right"] = 2;
		showImage_lightbox_content_padding["bottom"] = parseInt(_sb_fn.get_css_value("showImage_lightbox_content","paddingBottom"));
		if (!showImage_lightbox_content_padding["bottom"]) showImage_lightbox_content_padding["bottom"] = 2;



		// make image lightbox draggable if caller provides a function
		if (_lb_settings.draggable != 0) {
			if (_lb_settings.dragFunction != null) {	// use external draggable function
				// draggable from title of the lightbox
				_tmp = new _lb_settings.dragFunction(document.getElementById(showImage_lightbox.id + "_title"),showImage_lightbox,null,null,null,showImage_lightbox_setLastXY);
			}
		}

		/*
		showImage_lightbox.ontouchstart = function(e) {
			//alert("showImage_lightbox.ontouchstart # touches: " + e.touches.length);
			if (e && e.touches && e.touches.length > 1) return(true);
			else if (e && e.stopPropagation) { e.stopPropagation(); return(false); }	// return false to disallow selection
			return(true);
		}
		showImage_lightbox.onmousedown = function(e) {
			//alert("showImage_lightbox.onmousedown # touches: "e.touches.length);
			if (e && e.touches) {	// leave this like this for IE8
				if (e.touches.length > 1) return(true);
			}
			else if (e && e.stopPropagation) { e.stopPropagation(); return(false); }	// return false to disallow selection
			return(true);
		}
		showImage_lightbox.ontouchmove = function(e) {
			if (e && e.touches && e.touches.length > 1) return(true);
			else if (e && e.stopPropagation) { e.stopPropagation(); return(false); }
			return(true);
		}
		*/
		_sb_fn.touchScroll(id + "_content",false);
	}
	
	/*
	 * main function to call to show an image in a lightbox
	 */
	function showImageLightBox(name, imgwidth, imgheight, handleStr, presets) {
		if (DebugME > 0) {
			_sb_s.DEBUGmode = 2;			// 0 = debug messages off, 1 = log to DIV 'debugmessage', 2 = log to log window
			$.fn.log("log","");
		}

		_lb_settings.body = document.getElementById("sb_body");
		 
		// get body and window coords
		get_window_body_coords();

		// override default presets
		if (presets) makePresets(presets);
		if (imgwidth) _lb_settings.imageWidth = imgwidth;
		else _lb_settings.imageWidth = null;
		if (imgheight) _lb_settings.imageHeight = imgheight;
		else _lb_settings.imageHeight = null;

		// create an ID from handle string
		var id = getID(handleStr),
				// the image is loading. meanwhile set the sprocket to wait for the real image
				// returns like: "<img class=\"showImage_lightbox_img\" id=\"%id%_img\" src=\"%xslcssPath%sprocket.gif\">"
			loadingHTML = imageLoadingHTML(handleStr, id, name);
		//$.fn.log("log","loadingHTML:\n" + loadingHTML);

		if (showImage_lightbox == null) showImage_lightbox_setup(handleStr, id, name);								// we have to setup the entire lightbox
		else document.getElementById(id + "_lightbox_title").innerHTML = lightboxTitleHTML(handleStr, id, name);	// we have to set new image names in title only

		showImage_lightbox_image = new Image();
		showImage_lightbox_image.onload = function(){ showImage_lightbox_loaded(id, handleStr, name); }; // triggers lightbox opening
		showImage_lightbox_image.onerror = function(){ showImage_lightbox_error(); };
		showImage_lightbox_image.src = name;
		if (imgwidth) showImage_lightbox_image.width = imgwidth;
		if (imgheight) showImage_lightbox_image.height = imgheight;
		showImage_lightbox.style.display="none";	// has to be done. otherwise the image size might not be correct - yes indeed!
		showImage_lightbox_content.innerHTML = loadingHTML;

		// make lightbox draggable from image if caller provides a function
		if (_lb_settings.draggable != 0) {
			if (_lb_settings.dragFunction != null) {	// use external draggable function
				// draggable from image within the lightbox 
// ai removed: to make zoomable				//_tmp = new _lb_settings.dragFunction(document.getElementById(id + "_img"),document.getElementById(showImage_lightbox.id),null,null,null,showImage_lightbox_setLastXY);
			}
		}

		// delay to show the loading icon - the requested image could be in cash
		showImage_lightbox_loadingtimeout = setTimeout(function(){ showImage_lightbox_fadein(id + "_img", null, name);},500);
		return;
	}
	
	function killDraggable() {
		showImage_lightbox_removeDraggable();
	}
	// propagate functions
	this.showImageLightBox = showImageLightBox;
	this.showImage_lightbox_fadein = showImage_lightbox_fadein;
	this.showImage_lightbox_fadeout = showImage_lightbox_fadeout;
	this.showImage_lightbox_setimage = showImage_lightbox_setimage;
	this.showImage_lightbox_image = showImage_lightbox_image;
	this.showImage_lightbox_setLastXY = showImage_lightbox_setLastXY;
	this.killDraggable = killDraggable;
	this.makePresets = makePresets;

	
} // END function showImage_lightbox()
