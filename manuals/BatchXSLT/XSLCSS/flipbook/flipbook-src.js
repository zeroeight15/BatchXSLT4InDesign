/** @preserve flipbook.js v46 20200219. (c) aiedv.ch */
 /* ==================================================================
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
 * THE PRODUCER:
 * Andreas Imhof
 * www.aiedv.ch
 *
 * Version: 46
 * Version date: 20200219
 * History:
 * 46 20200219:	support images exported directly with InDesign (no ImageMagick)
 * 45 20160812:	fixed hang 'slide' mode
 * 44 20160103:	fixed in func '_setFolding' flag indicating that a page is folding for 1 page documents (a single page can not flip)
 * 43 20150809:	enhanced speed and Android 5 compatibility
 * 41 20141209:	fixed iframe location.href stuff for search
 * 41 20141203:	fixed cross domain iframe stuff
 * 41 20141122:	fixed lightbox font size adjust for SELECT and BUTTON
 * 41 20141118:	added function fb_make_settings to change settings from inAppBrowser
 *				IE version getter for IEMobile version enhanced
 * 				several in App printing enhancements
 * 41 20140830:	Fixed add/remove flipping pages to DOM in function '_addPage'
 *					Fixed drag trigger on touch/drag move
 *					Reload on orientation change
 * 40 20140805: Image lightbox no new window when running in App
 *				handle inApp Browser stuff
 *				detect Firefox OS
 * 				flipbook.css: .sb_pagearrow { background:none;	// added for Firefox OS 
 * 39 20140714: fixed IE8 problem while testing the title content
 * 39 20140712: turn title element off if white space only
 * 39 20140710: enhanced unregister_after_showLightboxContent_custom_CB, defaultVideoWidth/Height and minVideoWidth/Height of videos
 * 39 20140707: some bug fixes on page load type
 *				enhanced to control web site with iframe attribute
 * 38 20140627: enhanced unregister_after_showLightboxContent_custom_CB, defaultVideoWidth/Height and minVideoWidth/Height of videos
 * 38 20140608: enhanced xmlHttpRequest target placing/insert
 * 37 20140601: added flag 'show_page_navigation' to disbale page navigation buttons completely
 * 37 20140421: play sound handled for touch and mouse devices
 * V36a Ai: swipe page slide on IE 8 was not working
 * V36 Ai: support pointer events and much more
 * V35.2 Ai: added after_showLightboxContent_custom_CBs
 * V35.1 Ai: in function getPageHeightAvailable(): also subtract _sb_s.bodyOffsetTop if embedded in div 
 * 			 in function calcLightboxSizes() top position: also subtract _sb_s.bodyOffsetTop if embedded in div 
 */

/* general vars */
var fb_version = "43",
	fb_subversion = "0",
	fb_versiondate = "20150809",
	fb_flipbookWindowName = "BXSLTflipWin",
	fb_flipbookParentWin = window,
	fb_flipbookWin = window,				// this is the reference to the window the flipbook is loaded in: may be the top window or an iFrame
	fb_flipbookWinLocation = "",			// the entire window URL which was used
	fb_flipbookWinLocationSearch = "";		// the search part like ?p=5&single
try {
	if (window.frameElement) {	// if in an iFrame
		fb_flipbookWin = window.frameElement.contentWindow;
		fb_flipbookWinLocation = fb_flipbookWin.location.href;
			// no!!! fb_flipbookWinLocation = fb_flipbookWin.src;
		if (fb_flipbookWinLocation.indexOf("?") >= 0) fb_flipbookWinLocationSearch = "?" + fb_flipbookWinLocation.split("?")[1];
	}
	else {
		fb_flipbookWinLocation = fb_flipbookWin.location.href;
		fb_flipbookWinLocationSearch = fb_flipbookWin.location.search;
	}
} catch(ex) {
		fb_flipbookWinLocation = fb_flipbookWin.location.href;
		fb_flipbookWinLocationSearch = fb_flipbookWin.location.search;
}

fb_flipbookWin.name = fb_flipbookWindowName;	// set window name
//alert("\nfb_flipbookWinLocation: " + fb_flipbookWinLocation + "\nfb_flipbookWinLocationSearch: " + fb_flipbookWinLocationSearch);

var invalidXYpos = -99999999,	// flag any X Y position as 'not set'

	floater = null,					// public functions
	show_article = null,
	show_clicked_article = null,
	showImage = null,
	clear_all_shadows = null,
	goto_continued_article = null,
	goto_thumbs_page = null,
	goto_page = function(the_page, isPageIndex, duration, easing, setThumbsScroll,theevent,caller,force,detectSwipe) {
		//alert("calling gotoPage");
		return _sb_fn.gotoPage(the_page, isPageIndex, duration, easing, setThumbsScroll,theevent,caller,force,detectSwipe);
	},
	handleGotoPageFieldKeyPress = null,
	manual_gotopage = null,
	closeLightbox = null,		// legacy function to hideLightbox()
	
	_sb_s = null,				// a shortcut to _sb_settings
	_sb_e = null,				// a shortcut to _sb_s.elements
	_sb_fn = null,				// a shortcut to _sb_s.funcs
	


	_sb_settings = {		/***** main settings and working vars: DO NOT touch if you don't know what you do! *****/
		start_ticks : new Date().getTime(),		// application start ticks
		inApp : false,							// true when running in an App
		addInAppMargin : false,					// whetehr to ad in app spacing inAppMarginTop
		inAppMarginTop : "18px",				// margin top when running in an App
												// or empty string to set nothing
		inAppPrinting : false,					// disable/enable printing when running in inAppBrowser
		inAppPrintPolling : false,				// main App is doing printing when running in inAppBrowser: print output is send to poller


		DEBUGmode : 0,			// 0 : debug messages off, 1 : log to DIV 'debugmessage', 2 : log to log window
		isTouchDevice : false,
		override_isTouchDevice : -1,	// 1: auto detect, 0: force to NO isTouchDevice, 1:	force IS isTouchDevice
		pointerEvents : { start:"mousedown", move:"mousemove", end:"mouseup", out:"mouseout" },
		_elementTouch : "mousedown",
		cur_lang_ID : 0,
		cur_lang : "en",
		lg : null,

		platform : navigator.platform,
		platformUC : navigator.platform.toUpperCase(),
		userAgent : (navigator.userAgent ? navigator.userAgent : ""),
		userAgentUC : (navigator.userAgent ? navigator.userAgent : "").toUpperCase(),
		browserName : "",
		UAfullVersion : 0,
		UAmajorVersion : 0,
		renderEngine : "",

		is_OSX : false,
		is_iOS : false,
		iOSVersion : 5,
		is_iPad : false,
		is_iPhone : false,
		is_Phone : false,
		IEVersion : 0,
		is_IE : false,
		is_IE5 : false,
		is_IE6 : false,
		is_IE7 : false,
		is_IE8 : false,
		is_IE9 : false,
		is_IE10 : false,
		is_IE11 : false,
		is_IE12 : false,
		is_IE13 : false,
		is_IE14 : false,
		is_IE15 : false,
		is_IE16 : false,
		is_IE17 : false,
		is_IE18 : false,
		is_IE19 : false,
		is_IE20 : false,
		is_IElt9 : false,
		is_Linux : false,
		is_Mac : false,
		is_Win : false,
		is_Android : false,
		AndroidVersion : 5.0,		// assume newer version. New Firefox Browser does not state Android version in userAgent string
		AndroidVersionMin : 4.2,	// min version needed to make flip tranform effects
		is_ChromeOS : false,
		is_MobileOS : false,
		is_Safari : false,
		is_Firefox : false,
		is_FirefoxOS : false,
		is_Chrome : false,
		is_Opera : false,
		is_otherBrowser : false,	// like (_sb_s.userAgent.toLowerCase().indexOf("series60") != -1) || (_sb_s.userAgent.toLowerCase().indexOf("symbian") != -1) || (_sb_s.userAgent.toLowerCase().indexOf("windows ce") != -1) || (_sb_s.userAgent.toLowerCase().indexOf("blackberry") != -1)

		enableDraggableObjects : 1,				// default:1. if > 0 : allow buttons and ... to be moved. 0 : not draggable

		containerWindow : this,					// check if we load in an iFrame or in a top window
		containerWindowIsIFrame : false,		// is set to true if we load in an IFrame, false when in a top window

		currentMetaViewPort : null,				// the current viewport meta tag element
		viewPortStyles_set : false,				// flag to indicate, that viewPortStyle and msViewPortStyle are set
												// the viewport style to add for all browsers
												// this viewport style is not recognized by IE mobile 10
		//viewPortStyle : "@viewport{width:400px;zoom:1;min-zoom:1;max-zoom:2;user-zoom:zoom;}",
												// the viewport style to add for IE mobile 10
												// setting the meta viewport has no other effect for IE mobile 10
		ms10reload : true,						// to make below IE 10 Windows Phone 8 style wqork we have to make a terrible history.go(0) hack!!!
		ms10reloadDO : false,					// flag to indicate if we really have to do a reload
												// is set to true if a ms - specific style has been inserted by setMetaViewPort()
//		ms10ViewPortLink : "flipbook_ms10viewport.css",	// does not work if dynamically set
		//ms10ViewPortStyle : "@-ms-viewport{width:400px;zoom:1;min-zoom:1;max-zoom:2;user-zoom:zoom;}",
		//ms10ViewPortStyle : "@-ms-viewport{width:400px; user-zoom:fixed; max-zoom: 1; min-zoom: 1; }",	// seems to be best
		ms10ViewPortStyle : {
			portrait : {
				style: "@-ms-viewport{width:400px;}", id:"ms-viewport_p_400" 	// portrait 400px
			},
			landscape : {
				style: "@-ms-viewport{width:800px;}", id:"ms-viewport_p_800"	// landscape 800px
			}
		},
		//ms10ViewPortStyle : "@-ms-viewport{width:auto!important}",
												// the default viewport meta tag as string
												// or null to not set a meta tag
												// like "<meta name=\"viewport\" content=\"width=device-width,target-densitydpi=160dpi,initial-scale=1,minimum-scale=1, maximum-scale=1, user-scalable=no\"/>\r<meta name=\"MobileOptimized\" content=\"320\">",
		//defaultMetaViewPort : null,
		
		defaultMetaViewPort : {					// we need this for iPone 3 and all browsers not knowing CSS viewport
												// setting the meta viewport has no effect for IE mobile 10
												// will be reversed order in head
								0: {			// the MobileOptimized meta tag
										name:"MobileOptimized",
										content:"400"
									},
								1: {			// the viewport meta tag
										name:"viewport",
										content:"width=device-width,target-densitydpi=160,initial-scale=1,minimum-scale=1, maximum-scale=1, user-scalable=no"
										//content:"width=400,initial-scale=1,minimum-scale=1, maximum-scale=1, user-scalable=no"
										//content:"width=400"
									}
							},
		
		load_device_css : false,				// true to FORCE to load device size dependent css like 'flipbook_w400.css'
												// if false, device css are loaded only for small devices
		// available device size css. Order: smallest device first
		deviceCSS : {
			portrait : {
				320: { href:"flipbook_p_320.css", id:"deviceCSS_p_320" },	// device-width: 320
				400: { href:"flipbook_p_400.css", id:"deviceCSS_p_400" }	// device-width: 400
			},

			landscape : {
				400: { href:"flipbook_l_400.css", id:"deviceCSS_l_400" },	// device-width: 400
				800: { href:"flipbook_l_800.css", id:"deviceCSS_l_800" }	// device-width: 800
				}
		},

		initialMetaViewPort : "",				// the viewport meta tag content
		initialMetaViewPortScale : 1.0,			// the viewport meta content="initial-scale=0.68 ....
		currentMetaViewPortScale : 1.0,			// the viewport meta current scale
		deviceOrientation : -999,				// orientation in degrees:  -999 for 'undefined'
												// portrait : 0 or 180; landscape CW : -90, CCW : 90 defined for portable devices
												// degrees change on different devices some are default ( Zero degrees ) for landscape like some Samsung Android tablets
												// iPad have default portrait layout (Zero degrees)
		orientation : "landscape",				// orientation as string: "portrait" or "landscape"
		oldOrientation : "",					// orientation as string: "portrait" or "landscape"
		reloadOnOrientationChange : 3,			// reload the book and re-render on orientation change
												// 0 = don't reloadBook
												// 1 = reloadBook for touch devices
												// 2 = reloadBook for normal devices
												// 3 = both
		noReloadOnOrientationChange : false,	// flag: no reload in certain cases: like open search field/touch keyboard (which resizes the window
		
		screenWidth : 1024, 					// actual scree width and height
		screenHeight : 768,
		
		screenPPI : -1,							// screen resolution 96 / inch

		windowWidth : 0,
		windowHeight : 0,

		bodyWidth : 0,
		bodyHeight : 0,
		bodyOffsetLeft : 0,	// body position
		bodyOffsetTop : 0,
		bodyOversize : 0,			// how much to add to the really needed body size

		slidebook_load_state : "",				// addable flags like '32' : body and images loaded
												// "" : nothing loaded
												// 1 : last element of body content is available: the div with id="debugmessage"
												// 2 : body comletely loaded (body onload event has fired - body element of the flipbook HTML)
												// 3 : all page images loaded

	
		pageTurnMode : "turn",					// can be 'turn' or 'slide'
		pageDisplay : "double",					// can be 'double' or 'single'
												// when to switch to 'single' page display
		pageDisplayForced : false,				// tru, when forced by a parameter like ?double
		pageDisplayMaxDoublePX : 600,			// if defined and > 0, this pixels have precedence over pageDisplayMaxDouble
		pageDisplayMaxDouble : 1.2,				// default = 1.2 * page width
		pageWidth : 0,							// calculated width of page preview images
		pageHeight : 0,							// calculated height of page preview images
		pageSlideFactor : 1.2,					// how many pages to show in minimum in slide mode
		page_borderLeftWidth : null,
		page_borderTopWidth : null,
		page_borderRightWidth : null,
		page_borderBottomWidth : null,

		pageAdjustToWindow : 3,					// Adjust the page image size to current window size
													// addable flag:
													// 0 : don't, 1 : adjust on load, 2 adjust on window resize
													// 3 : in both cases
		pageAdjustToWindowTimeout : 500,	// How long to wait before adjusting the page sizes. default : 500 ms
		pageAdjustMinWidth : 240,			// minimum page image width
		pageAdjustMinHeight : 100,			// minimum page image height
		pageAdjustCurrentRatio : 1.0,	// current page size ratio
		pageAdjustOversizeRatio : 1.2,	// how much a page image may be oversized. default : 1.2
		pageAdjustSizeTrim : 0,			// how many pixels to adjust the calculated max possible page size. default : 0
		pageAdjustWidthTrim : 0,		// how many pixels to adjust the calculated max possible page width. default : 0
		pageAdjustHeightTrim : 0,		// how many pixels to adjust the calculated max possible page height. default : 0
		pageAdjustAreas : null,					// working var for areas
		autoDet_pageImageUseSize : 1,		// 1 == (default) detect the size of page images to load if multiple are available
		pageImageUseSize : 0,				// 0 == (default) the first page image stated in the array 
											// epaper_pages[x][0]=["ba-A4-II_d_Test_CC_p1_84.jpg,ba-A4-II_d_Test_CC_p1_134.jpg,ba-A4-II_d_Test_CC_p1_168.jpg",0,"ba-A4-II_d_Test_CC",1,"1","1","ba-A4-II_d_Test_CC_p1.jpg","rgth"];
											// will be used
		pageImageCoordsRatio : 1.0,			// 1.0 is for default coords of default page image given in HTML code
											// new value is calculated by autoDet_pageImageUseSize() if other page image size is loaded to recalc all coords

		currentPageIdx : 0,
		currentPageScrollX : 0,
		totalPages : 0,
		pageToPageOffsetWidth : 300,			// pixels from one page left to next page left paper border
		show_page_navigation : 1, 				// 1 = show page navigation button bar, 0 = don't
		show_page_thumbs : 2, 					// addable flag
													// 0 : no page thumbs navigation at all
													// 1 : create the page thumbs navigation for all systems
													// default: 2 : create the page thumbs navigation but not for Android
													//			Android has a severe problem on click though on absolute positioned elements (page thumbs navigation IS absolute positioned)
		page_thumbs_nav_scrolled : 0,			// is set to the y-pixels scrolled from page thumbs scroller when thumbs were scrolled
		pageNumLeft : 0,						// dynamically changed by the flipping pages book!
		pageNumRight : 1,						// the currently shown page numbers for left and right side
												// 0 means not available/visible
		documentPDFbutton_enable : 1,			// show document PDF button if PDF available
		pagePDFbutton_enable : 1,				// show page PDF button if PDF available
		pagePDF_buttonCont : null,
		pagePDF_buttonContR : null,

		min_swipe : 30,							// how many pixels the mouse must move to be interpreted as a swipe (otherwise click)
		enable_mousewheel : 3,					// default : 3: addable flag: 1 for bookPages, 2 for pageThumbs
													// for Android disabled
													// for IE: no mouse wheel support on book pages. See: function bookPagesContent_mwhandler()
		wheelDeltaPix : 3,						// how many pixels to scroll on mouse wheel delta

		DEBUGPAGELOADSTATUS : false,		// (default) false, true : keep the load status display open
		pageloadStatusType : 0,				// 0 == (default) none, 1 == rotating wheel, 2 == rotating wheel without loaded page numbers, addable flag: 4 == show table (loader debug)
		pageImageLoadType : 0,				// 0 == (default) normal == load all pages in background. The first page image has some more time to load
												// > 0 == preload number of pages immediately, rest on demand. In this mode, page navigation icons will not be available
		pageImageMinFollowing : 3,			// the minimum number of pages that must be loaded befor and after the requested page
		pageImageLoadDelay : 500,			// the ms to wait after page 1 is loaded for loading following page images: let the rest of the site build
		pageImageUnloadUnused : 1,			// (default) 0 : keep loaded page images in memory, 1 : unload currently unused page images to safe memory
		pageTransitionEasing : "ease-out",	// (default) ease-out, ease-in, ease, linear
		pageTransitionDuration : 400,		// (default) 400 milli seconds, 0.0 for no animation : direct page flip
		pageTransitionNumSteps : 10,		// (default) 10 steps to shift a page in (css mode)
		pageTransitionStepDelay : 1,		// (default) 10 milli seconds, 0 for no animation : direct page flip (css mode)

		vendorPrefix : "",					// the vendor prefix lowercase like: webkit
		CamelVendorPrefix : "",				// the original vendor prefix like: Webkit
		touchActionStyle : "",				// supports css style 'touch-action' or '-ms-touch-action'
		nativeTransform : "",				// the vendor prefixed Transfrom like -moz-Transform
		transitionEndEvent : "",			// the vendor transition end event name 'transitionend'
		allowNativeTransitions : true,		// flag to inhibit native CSS3 transitions
												// true : default : always use native if we can
												// false : use css left/top
		pageTransitionCurrentX : null,		// is set while a page transitions. null means 'not set'

		pageIsScrolling : false,
		canvas_available : false,
		mouseOnCanvas : false,
		mapsAndCanvasAreMasked : false,

		showOldIEVersionwarning : 8,		// default:7. warn if IE version is less or equal than this.
		hasTouchScreen : false,				// default:false. true : is a "normal" system with touch screen but no keyboard and no mouse. 
												// Not a system with mobile OS but windows or unix.
												// Therefore no touch events but normal mouse events

		addVirtualKeybord : false,			// default:false. true to add a virtual keyboard to input fields for normal systems without a keyboard
		noArticleScrollBars : false,		// default:false. true to hide scrollbars on normal systems with touch screens. 
		useArticleScrollButtons : 0,		// default:0. 1 to set scrollbuttons like the close button, 2 to set buttons on top/bottom for scrolling.
		articleScrollButtonsStep : 5,		// default:5. the amount of pixels to scroll at interval articleScrollButtonsInterval. 
		articleScrollButtonsInterval : 20,	// default:20. the scroll interval in ms. 
		detectLightBoxOverflowTimeout : 400,	// default:400. ms to wait until detecting lightbox overflow and then set scroll buttons. 
		enableJStouchscroll : 6,			// add touchScroll js event handler to enable touch scroll on overflowed divs
												// addable flgs:
												// 0 : no touchScroll
												// 1 : always touchScroll
												// 2 : default = enable touchScroll but for iOS ( iOS  supports natively through CSS using -webkit-overflow-scrolling:touch;
												// 4 : not for IE mobile supported natively through "-ms-overflow-style:none;"
		ms10OverflowStyle: "-ms-overflow-style:none;",	// the scroll style to add for IE 10 mobile to make div 'touch scrollable'

		doArticleShade : true,				// default:true. Shade an article when on mouse over and also the shade on the page 
		highlightArticleOnPage : true,		// default:true. also highlight in lightbox shown article on the page. turned off if 	doArticleShade == false
		//	articleShadeColors : ["rgba(255,80,30,0.2)", "rgba(200,80,100,0.3)"],	// 2 alternating colors to shade article on mouse over
		articleShadeColors : ["rgba(0,80,160,0.1)", "rgba(0,100,80,0.1)"],	// 2 alternating colors to shade article on mouse over
		articlePageShadeColor : "rgba(255,255,0,0.2)",							// 1 color to shade current selected article on page"#0044B2"
		// darker:
		//	articleShadeColors : ["rgba(0,80,160,0.7)", "rgba(0,100,80,0.7)"],	// 2 alternating colors to shade article on mouse over
		//	articlePageShadeColor : "rgba(255,255,0,0.7)",							// 1 color to shade current selected article on page"#0044B2"

		// view scale: 
		viewWidthNeeded : 0,			// the width needed to see full page preview in window width
		viewHeightNeeded : 0,			// the height needed to see everything in window height
		viewCurrentScale : 1.0,			// the current calculated body scale factor
		viewScaleNeeded : 1.0,			// the scale factor needed to display the web page
		viewMaxScale : 0.0,				// > 1.0 to max enlarge like 1.25, to 0.0 to not allow scale: will scale the body to fit browser window height
		viewScaleOrigin : "center top",	// the origin point to scale
		currentPinchZoom : 1.0,			// the current pinch zoom scale factor

		// lightbox
		lb_ptr_evt : {					// events stor for lightbox touch actions
			start: null,				// start ticks. if null pointerdown not taken place
			end: null,					// end ticks. if null pointerend not taken place
			isMove: false,				// true or false. if pointer was moved after start: true
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
			moveTrigger: 15				// how many pixels to move pointer before it is reported as a 'move'

		},

		preconvertButtonsLinks: true,		// convert buttons and click attributes to TAP events

		defaultVideoWidth: 320,			// the width of videos if nothing given (16:9)
		defaultVideoHeight: 180,		// the height of videos if nothing given
		minVideoWidth: 320,				// the minimun width of videos if not zero (16:9)
		minVideoHeight: 180,			// the minimun height of videos if if not zero
		
		inhibit_calcLightboxSizes : false,	// set to troe to not dynamically calc/set lightbox size
		lightboxPosition : 0,			/*	0 : center of body
											1 : left side body
											2 : right side body
										*/
		lightboxMaxWidth : 490,			/*	max width the lightbox should have in pixels. default : 490. Set to zero to always use lightbox2BodyWidth factor */
		lightboxWidth : 490,			// like lightboxMaxWidth,
		lightbox2BodyWidth : 3,		/* factor of lightbox width related to body width. Default : 3 => 1 third of body width */
		lightboxMaxHeight : 0,		/*	max height of lightbox: 0 to calc dynamically, or a value like 350 (px) */
		lightboxMinHeight : 0,		/*	min height of lightbox: 0 to calc dynamically, or a value like 650 (px) */
		lightboxHeight : 0,
		lightboxContentWidth : 0,	/* width and height of lightbox content - lightbox should be as large as needed */
		lightboxContentHeight : 0,
		lightboxContentDimIDs : "",	/* the lightbox contents elements IDs requiring this width or height */
		lightboxTop : 0,
		lightboxLeft : 0,
		overridelightboxTop : invalidXYpos,
		overridelightboxLeft : invalidXYpos,
		enable_lightBoxFontSize : 1,	// default : 1 to enable font size change in lightbox, set to 0 to disable
		lightBoxFontSize : 100,
		lightBoxFontSizeSliderData : 100,
		lightBoxMinFontSize : 40,
		lightBoxMaxFontSize : 400,
		lightBoxFontSizeStep : 10,
		lightBoxFontSizeSliderWidth : "120px",
		lightBoxFontSizeSliderSMA : "A",
		lightBoxFontSizeSliderLGA : "A",

		lightboxFadeCloseButton : false,
		lightboxFadeSearchButton : false,
		lightboxIsFading : false,
		lightboxIsShowing : false,
		lightBoxDragHandler : null,
		lightBoxTouchScrollHandler : null,

		lightBoxFontSizeSlider : null,
		lightBoxFontSizeSliderUpdt : false,

		lightBoxArticleCommentSys : null,

		lastSearchResults : "",
		clickCameFromSearchResults : false,

		htmlCloseLightboxClickHandler : null,

		enable_print_lightbox : 1,		// default:1. show icon to print lightbox content
												// 0 : no printing
												// 2 : also will set 	print_entireArticle to 1 to entire article
		print_entireArticle : 0,		// default:0. print partial content shown in lightbox (splitted article part), 1 : print entire article

		enlargeImageIcon : 1,			// 1 : show an icon on the smaller image: show enlarged 2nd image
		shrinkImageToLightbox : 1,		// 1 : make too large images smaller so they fit in lightbox width

		show_features_pop : 1,			// default:1. show icon to open/close features pop (pin)

		showImageHandle_1 : null,		// the handle to the main function showImage_lightbox() in image_lightbox.js
		showImage : {			// show enlarged 2nd image in a window
			where : 2,				// default => 1 : in new window
									// 2 : show in showImageLightbox in same window
			x : -1,				// new window x pos or -1 for anywhere
			y : -1,				// new window y pos or -1 for anywhere
			w : -1,				// new window width or -1 for auto
			h : -1				// new window height or -1 for auto
		},

		showImageLightbox_enable : 1,		// 1 : allow to show enlarged images in a lightbox. Otherwise 0
				// settings for the image lightbox defined in image_lightbox.js
			showImageLightbox : {			// show enlarged 2nd image in new window
				draggable : 1,							// 1 : default : make image lightbox draggable. Otherwise not draggable
				dragFunction : null,					// the pointer to the drag function to be use be the image lightbox
				scaleToSize : 2,						// default => 1 : center to body width/height
														// 2 : center to window width/height
				lightbox_2_window_padding : 2,			// the minimum padding from main window to this lightbox
				top : -1,								// default : -1 : calculate and center in window
														// any other value is fixed position in pixels
				left : -1,								// dito top
				imageLoadIconLeft : "80px",				// left pos of loading icon  (sprocket.gif)
				imageLoadIconTop : "80px",				// top pos of loading icon (sprocket.gif)
				imageLoadIconWindowWidth : 200,			// width of loading icon window (sprocket.gif) (as number in pixels)
				imageLoadIconWindowHeight : 200,		// height of loading icon window (sprocket.gif) (as number in pixels)
				imageLoadIconBackgroundColor : "#fff",	// background color for loading icon window (sprocket.gif) (as hex number string)

				body : "",								// the body element will de set in delayed_presets()
				xslcssPath : ""							// the path to the XSLCSS folder will de set in delayed_presets()
			},

		modules: {					// pluggable modules
									// issuenav and moreinfos are standard modules
									// function custom_loader() defined in custom.js may add any other modules
									// like _easECart or _NAVISION
			issuenav_enable : 0,			// 0 : default : disable issue navigation
			moreinfos_enable : 0,		// default : 0. > 0 : enable to get addintional information from the 'Info' folder structure
											// get the 'more infos' buttons derived from the '_info' folder structure only from manually set buttons
			moreinfos_enableauto : 0,	// default : 0. > 0 : enable to get addintional information from the 'Info' folder structure
											// automatically set the 'more infos' buttons derived from the '_info' folder structure
			easeedit_enable : 0,	// enable easEEdit to copy Article from lightbox with a click
			
			NAVISION_enable : 0,	// the NAVISION access system
			easECart_enable : 0,	// easECart shopping plugin
			
			// discussion systems
			disqus_enable : 0,			// DISQUS
			livefyre_enable : 0,		// livefyre
			intensedebate_enable : 0	// intensedebate
		},
	
		// title and url when clicking on logo. Set: logoClickURL : "", if logo should not be clickable
		logoName : "",
		logoClickURL : "",
		logoURLtarget : "",
		logoTitle : "",

		/* other transient variables (internally used -  do not change) */
		oldBodyMargins : "",
		closebuttonWidth : 0,
		closebuttonLeft : 0,
		searchbuttonWidth : 0,
		searchbuttonLeft : 0,
		xslcssPath : "",
		xslcssSubpath : "flipbook/",
		xslcssThemeSubpath : "",
		pageThumbsLiHeight : 0,

		show_TOC_index : 1,	// default:1. show index button if InDesign index is available
		TOC_isShown : false,	// working variable: true when the TOC is shown in the article window
		close_lightbox_if_TOC_isClicked : true,	// default:true. click on an link in TOC will close the lightbox

		fontsizeUnits : 0,
		fontsizeBase : 9,

		showAllDBresults : 0,	// 0 : show all including status messages. 1 : show real results only.

		allowArticleStepping : true,	// default:true. when lightbox (article) is shown step through articles instead of pages
		lastShownArticleID : "",

		sb_page_entry_field_behaviour : 3,	// addable flags: 1 : clear field on focus, 2 : update current page display
		
		documentScrollPrevented : false,	// is true if function preventDocumentScroll was called
		
		user_entry_active : false,			// true when any user input filed is active and on mobile a keybord could rise up
											// in this case, we should not resize the flipbook.
											// on Android, the virtual keyboard decreases the height of the window

		splash : {
			timeout : 800,						// how many ms to show the splash screen, set to 0 to hide immediately
			styles_after : {
				"#sb_body" : [
								["visibility", "visible"]	// make visible
							 ],
				"html" : [
							["background-color", "transparent"],	// clear background color
							["background-image", "none"],			// clear background image
							["height", "auto"],
							["border", "0px none"]
						 ]
			}
		},

		elements : {
			headEl : null,
			html : null,
			bodyEl : null,
			body : null,
				body_marginLeft : 0,
				body_marginRight : 0,
				body_marginTop : 0,
				body_marginBottom : 0,
				body_paddingLeft : 0,
				body_paddingRight : 0,
				body_paddingTop : 0,
				body_paddingBottom : 0,
				body_borderLeftWidth : 0,
				body_borderTopWidth : 0,
				body_borderRightWidth : 0,
				body_borderBottomWidth : 0,

				body_min_marginLeft : 0,			// minimum body margins to keep
				body_min_marginRight : 0,
				body_min_marginTop : 0,
				body_min_marginBottom : 0,

			sb_container : null,

			scrollview_header : null,
			scrollview_container : null,
				scrollview_container_paddingLeft : 0,
				scrollview_container_paddingTop : 0,
				scrollview_container_paddingRight : 0,
				scrollview_container_paddingBottom : 0,
				scrollview_container_marginLeft : 0,
				scrollview_container_marginTop : 0,
				scrollview_container_marginRight : 0,
				scrollview_container_marginBottom : 0,
				scrollview_container_borderLeftWidth : 0,
				scrollview_container_borderTopWidth : 0,
				scrollview_container_borderRightWidth : 0,
				scrollview_container_borderBottomWidth : 0,
			scrollview_content : null,
				scrollview_content_borderLeftWidth : 0,
				scrollview_content_borderTopWidth : 0,
				scrollview_content_borderRightWidth : 0,
				scrollview_content_borderbottomWidth : 0,
				scrollview_content_paddingLeft : 0,
				scrollview_content_paddingTop : 0,
				scrollview_content_paddingRight : 0,
				scrollview_content_paddingBottom : 0,
			sb_pagelist : null,
				sb_pagelist_marginLeft : null,
				sb_pagelist_marginTop : null,
				sb_pagelist_marginRight : null,
				sb_pagelist_marginBottom : null,
				sb_pagelist_borderLeftWidth : 0,
				sb_pagelist_borderTopWidth : 0,
				sb_pagelist_borderRightWidth : 0,
				sb_pagelist_borderbottomWidth : 0,
				sb_pagelist_paddingLeft : 0,
				sb_pagelist_paddingTop : 0,
				sb_pagelist_paddingRight : 0,
				sb_pagelist_paddingBottom : 0,
			pv_P1 : null,
			scrollview_trailer : null,
			scrollview_bottom : null,
			status_message : null,
			debugmessage : null,
			bookPages_images : null,
			sb_page_entry_field : null,
			lightbox : null,
			lightbox_div : null,
			lightbox_divEl : null,
			lightbox_content : null,
			lightbox_close : null,
			lightbox_returnsearch : null,
			lightbox_overlay : null,

			doctypeinfos : null,
			logger_console : null,
			ft_searchentry_field : null,
			ft_searchentry_field_height : 190,

			pageThumbsScrollContainer : null,
			pageThumbsScrollContainerEl : null
		},

		funcs : {
			elapsedTicks_sinceStart : null,
			loadDeviceCSS : null,
			setMetaViewPort : null,
			getSlideBookSettings : null,

			get_pointerXY : null,
			touchDetector : null,
			touchDetector_vars : null,
			clear_all_shadows : null,
			get_moreDocumentInfo : null,
			hidePageThumbs : null,
			get_site_param : null,
			getall_site_params : null,
			get_css_value : null,
			set_css_value : null,
			listCssRules : null,
			list_object : null,
			removeEventHandler : null,
			haltEvents : null,
			addClass : null,
			removeClass : null,
			hasClass : null,
			loadjscssfile : null,
			loadjscssfileWait : null,
			showLightbox : null,
			hideLightbox : null,
			show_article_xml : null,
			show_searchResultsArticle: null,
			printLightbox : null,
			hide_print_IFrame : null,
			touchScroll : null,
			call_sb_fulltext_search : null,
			init_fulltext_search : null,
			enable_fulltext_search : null,
			disable_fulltext_search : null,
			handleFTsearchFieldKeyPress : null,
			getLatestIssue : null,
			fadeOutSearchWin : null,
			blurSearchWin : null,
			checknumeric : null,
			XdraggableObject : null,
			isEven : null,
			trim : null,
			endsWith : null,
			rectIntersects : null,
			getElementsByClassName : null,
			showLabeledArticle : null,
			gotoPage : null,
			getPageIDXFromArticleID : null,
			getArticleIDXFromArticleID : null,
			xmlHttpRequest : null,
			adjustPageImageSize : null,
			adjustPageImageSizeCaller : null,
			enableAdjustPageImageSize : null,
			disableAdjustPageImageSize : null,
			attachGestureDetector : null,
			cssIntVal : null,
			cssFloatVal : null,

			data_labels_clear : null,
			data_labels_store : null,
			data_labels_isset : null,
			data_labels_get : null,
			data_labels_getall : null,
			data_labels_length : null,

			// callback registering functions
			registerCB_sb_settings_Override : null,
			registerCB_early_config : null,
			registerCB_clean_content : null,
			registerCB_before_showLightbox : null,
			registerCB_before_showLightboxContent : null,
			registerCB_after_showLightboxContent : null,
			unregisterCB_after_showLightboxContent : null,
			registerCB_after_hideLightbox : null,
			registerCB_before_printLightbox : null,
			registerCB_after_bookResize : null,
			registerCB_after_pageTurn : null,
			registerCB_after_gotoPage : null
		}
	};

_sb_s = _sb_settings;		// a shortcut to _sb_settings
_sb_e = _sb_s.elements;		// 			  to _sb_settings.elements
_sb_fn = _sb_s.funcs;		// 			  to _sb_settings.funcs



var fb_make_settings = function(what) {
	var w_json, key;
	if (typeof(_sb_s) == 'undefined') {
		setTimeout(function(){fb_make_settings(what);},0);
		return;
	}
	//alert("called fb_make_settings: " + typeof(what) + "\n" + what);
	if (typeof(what) == 'string') w_json = JSON.parse(what);
	else w_json = what;
	for (key in w_json) {
		//alert("setting => " + key + " : " + w_json[key]);
		_sb_s[key] = w_json[key];
	}
},

fb_poll_actions = {'actions':0},
fb_poll_action = function() {
	//fb_poll_actions["openpdf"] = "http://someurl.com";
	var my_fb_poll_actions = fb_poll_actions;
	fb_poll_actions = {'actions':0};
	//alert("got polled: " + JSON.stringify(my_fb_poll_actions));
	return my_fb_poll_actions;
},
fb_poll_action_set = function(action, data) {
	fb_poll_actions["actions"] = fb_poll_actions["actions"] + 1;
	//alert("num actions: " + fb_poll_actions["actions"]);
	fb_poll_actions[action] = data;
};
_sb_fn.fb_poll_action_set = fb_poll_action_set;

/**
 * Handle search result clicks
 */
var get_page_article = function (request_url) {
	var breakpoint = 0,
		go2page_sequence = -1,
		go2article_idx = -1,
		paramsarr, param,
		plain_current_url,
		i;
	do {
		if ( (fb_flipbookWin == null) || (fb_flipbookWin.closed == true) ) {
			try{
				if (window.opener && (window.opener.name == fb_flipbookWindowName) ) fb_flipbookWin = window.opener;
			}
			catch (ex) { breakpoint = 1; break; }	// fall through to open the search window
		}
		if ( (fb_flipbookWin == null) || (fb_flipbookWin.closed == true) ) {
			fb_flipbookWin = window.open("",fb_flipbookWindowName,"location=yes,menubar=yes,resizable=Yes,scrollbars=Yes,status=yes,titlebar=yes,toolbar=yes,dependent=No");
			breakpoint = 2; break;
		}
		if ( (fb_flipbookWin == null) || (fb_flipbookWin.closed == true) ) { breakpoint = 2; break; }
		//alert("fb_flipbookWin.name: " + fb_flipbookWin.name);
	
		// get requested page number and article idx
		if (request_url.indexOf("?") >= 0) {
			paramsarr = request_url.split("?")[1].split("&");
			for (i = 0; i < paramsarr.length; i++) {
				param = paramsarr[i].split("=");
				switch (param[0]) {
					case "p": go2page_sequence = parseInt(param[1], 10);
						break;
					case "a": go2article_idx = parseInt(param[1], 10);
						break;
				}
			}
			//alert("go2page_sequence: " + go2page_sequence + "\ngo2article_idx: " + go2article_idx);
		}
		// the flip book window seems to be open but may have different document, page or article
		plain_current_url = fb_flipbookWinLocation;
		if (plain_current_url.indexOf("?") > -1) {
			plain_current_url = plain_current_url.substr(0,plain_current_url.indexOf("?"));
		}
		if (plain_current_url.indexOf("#") > -1) {
			plain_current_url = plain_current_url.substr(0,plain_current_url.indexOf("#"));
		}
		plain_current_url = unescape(plain_current_url);
		//alert("request_url: " + request_url + "\nfb_flipbookWin.location: " + fb_flipbookWin.location + "\nfb_flipbookWinLocation: " + fb_flipbookWinLocation + "\nplain_current_url: " + plain_current_url);
	
		if (request_url.toLowerCase().indexOf(plain_current_url.toLowerCase()) == 0) {	// requested document currently is open: flip page and article
			//alert("same document: flipping page and article to page: " + go2page_sequence);
			try {
				fb_flipbookWin.focus();
			}
			catch (ex) { breakpoint = 4; break; }	// load requested document using href
			try {
				// call goto page and article
				//alert("fb_flipbookWin.goto_continued_article: " + fb_flipbookWin.goto_continued_article);
				if (go2page_sequence > -1) fb_flipbookWin.goto_continued_article(go2article_idx,go2page_sequence);
				return(false);
			}
			catch (ex) { breakpoint = 5; break; }	// load requested document using href
			
		}
		breakpoint = 6;
	} while (false);
	//alert("breakpoint: " + breakpoint + "\n" + request_url + "\n\n" + plain_current_url);

	if ((typeof(_sb_s.modules.NAVISION_enable) != 'undefined') && (_sb_s.modules.NAVISION_enable > 0)) {
		//alert("request_url: " + request_url);
		if (request_url.indexOf("?") >= 0) request_url += "&NAVISION";
		else request_url += "?NAVISION";
		//alert("NEW request_url: " + request_url);
	}

	// open the window to show flipping book
	if ( (fb_flipbookWin != null) && (fb_flipbookWin.closed == false) ) {
		try{
			fb_flipbookWin.focus();
		}
		catch(ex) { }	// fall through to open the search window
	}
	else fb_flipbookWin = window.open("",fb_flipbookWindowName,"location=yes,menubar=yes,resizable=Yes,scrollbars=Yes,status=yes,titlebar=yes,toolbar=yes,dependent=No");
	fb_flipbookWin.location.href = request_url;
	return(false);
};

/**
 * The main flipbook
 */
(function($) {

'use strict';

	/*
	 * declare callback collectors
	 */
	var sb_settings_Override_CBs = [],
		early_config_custom_CBs = [],
		clean_content_custom_CBs = [],
		before_showLightbox_custom_CBs = [],
		before_showLightboxContent_custom_CBs = [],
		after_showLightboxContent_custom_CBs = [],
		after_hideLightbox_custom_CBs = [],
		before_printLightbox_custom_CBs = [],
		after_bookResize_custom_CBs = [],
		after_pageTurn_custom_CBs = [],
		after_gotoPage_custom_CBs = [],

		register_sb_settings_Override_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			sb_settings_Override_CBs.push(cb);
			//alert("registered sb_settings_Override_CBs: " + sb_settings_Override_CBs[sb_settings_Override_CBs.length - 1]);
			return true;
		},

		register_early_config_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			early_config_custom_CBs.push(cb);
			//alert("registered early_config_custom_CBs: " + early_config_custom_CBs[early_config_custom_CBs.length - 1]);
			return true;
		},

		register_clean_content_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			clean_content_custom_CBs.push(cb);
			//alert("registered clean_content_custom_CBs: " + clean_content_custom_CBs[clean_content_custom_CBs.length - 1]);
			return true;
		},

		register_before_showLightbox_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			before_showLightbox_custom_CBs.push(cb);
			//alert("registered before_showLightbox_custom_CBs: " + before_showLightbox_custom_CBs[before_showLightbox_custom_CBs.length - 1]);
			return true;
		},

		register_before_showLightboxContent_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			before_showLightboxContent_custom_CBs.push(cb);
			//alert("registered before_showLightboxContent_custom_CBs: " + before_showLightboxContent_custom_CBs[before_showLightboxContent_custom_CBs.length - 1]);
			return true;
		},

		register_after_showLightboxContent_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			after_showLightboxContent_custom_CBs.push(cb);
			//alert("registered after_showLightboxContent_custom_CBs: " + after_showLightboxContent_custom_CBs[after_showLightboxContent_custom_CBs.length - 1]);
			return true;
		},
		unregister_after_showLightboxContent_custom_CB = function(cb) {
			var i;
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			for (i = 0; i < after_showLightboxContent_custom_CBs.length; i++) {
				if (after_showLightboxContent_custom_CBs[i] == cb) {
					//alert("unregister_after_showLightboxContent_custom_CB: " + after_showLightboxContent_custom_CBs[i]);
					after_showLightboxContent_custom_CBs.splice(i, 1);
				}
			}
			return true;
		},

		register_after_hideLightbox_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			after_hideLightbox_custom_CBs.push(cb);
			//alert("registered after_hideLightbox_custom_CBs: " + after_hideLightbox_custom_CBs[after_hideLightbox_custom_CBs.length - 1]);
			return true;
		},
		
		register_before_printLightbox_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			before_printLightbox_custom_CBs.push(cb);
			//alert("registered before_printLightbox_custom_CBs: " + before_printLightbox_custom_CBs[before_printLightbox_custom_CBs.length - 1]);
			return true;
		},
		
		register_after_bookResize_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			after_bookResize_custom_CBs.push(cb);
			//alert("registered after_bookResize_custom_CB: " + after_bookResize_custom_CBs[after_bookResize_custom_CBs.length - 1]);
			return true;
		},
		
		register_after_pageTurn_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			for (var idx = 0; idx < after_pageTurn_custom_CBs.length; idx++) {
				//alert(after_pageTurn_custom_CBs[idx] + "\n" + cb);
				if (after_pageTurn_custom_CBs[idx].toString() === cb.toString()) {
					//alert("already registered:\n" + cb);
					return true;
				}
			}
			//	alert("registering:\n" + cb);
			after_pageTurn_custom_CBs.push(cb);
			//alert("registered after_pageTurn_custom_CB: " + after_pageTurn_custom_CBs[after_pageTurn_custom_CBs.length - 1]);
			return true;
		},

		register_after_gotoPage_custom_CB = function(cb) {
			if ( (typeof(cb) === undefined) || (typeof(cb) != 'function') ) return null;
			for (var idx = 0; idx < after_gotoPage_custom_CBs.length; idx++) {
				//alert(after_gotoPage_custom_CBs[idx] + "\n" + cb);
				if (after_gotoPage_custom_CBs[idx].toString() === cb.toString()) {
					//alert("already registered:\n" + cb);
					return true;
				}
			}
			//	alert("registering:\n" + cb);
			after_gotoPage_custom_CBs.push(cb);
			//alert("registered after_gotoPage_custom_CBs: " + after_gotoPage_custom_CBs[after_gotoPage_custom_CBs.length - 1]);
			return true;
		};
	// make CB collector globally available
	_sb_fn.registerCB_sb_settings_Override = register_sb_settings_Override_CB;
	_sb_fn.registerCB_early_config = register_early_config_custom_CB;
	_sb_fn.registerCB_clean_content = register_clean_content_custom_CB;
	_sb_fn.registerCB_before_showLightbox = register_before_showLightbox_custom_CB;
	_sb_fn.registerCB_before_showLightboxContent = register_before_showLightboxContent_custom_CB;
	_sb_fn.registerCB_after_showLightboxContent = register_after_showLightboxContent_custom_CB;
	_sb_fn.unregisterCB_after_showLightboxContent = unregister_after_showLightboxContent_custom_CB;
	_sb_fn.registerCB_after_hideLightbox = register_after_hideLightbox_custom_CB;
	_sb_fn.registerCB_before_printLightbox = register_before_printLightbox_custom_CB;
	_sb_fn.registerCB_after_bookResize = register_after_bookResize_custom_CB;
	_sb_fn.registerCB_after_pageTurn = register_after_pageTurn_custom_CB;
	_sb_fn.registerCB_after_gotoPage = register_after_gotoPage_custom_CB;


	/*
	 * get elapsed tick since application start
	 */
	var elapsedTicks_sinceStart = function () {
		return (new Date().getTime()) - _sb_s.start_ticks;
	},

	/***********************
	 * detect current window
	 **/
	runsInIFrame = function (win) {
		/*
			top window (normal windows) have window.frames [object DOMWindow]
			IFrames have window.frameElement  [object HTMLIFrameElement]
		*/
		var isIframe = false;
		//alert("\nwin.frameElement: " + win.frameElement + "\n\nwin.parent: " + win.parent + "\nwin.self: " + win.self + " type: " + typeof(win.self) + "\nwin.top: " + win.top + " type: " + typeof(win.top)  + "\n\nself==parent: " + (win.self === win.parent));
		try {
			isIframe = win.self !== win.top;
		} catch (e) {
			return true;
		}
		try {
			if (!win.frameElement) {	// top window has no frameElement
			}
			else {
				if (_sb_s.is_IE && (_sb_s.IEVersion <=7)) {
					if (typeof(win.frameElement) == "object") isIframe = true;
				}
				else isIframe = (win.frameElement && (win.frameElement+"").indexOf("HTMLIFrameElement") > -1);
			}
		} catch(ex) {}
		//alert("type of window:" + win + "\nwin: " + win + "\ntop: "+top + "\nwin===top: " +(win===top) + "\ntop.name: " +top.name + "\n\nwin.frames: " + win.frames + "\nwin.frameElement: " + win.frameElement + "\nwin.id: " + win.id + "\nwin.name: " + win.name + "\nisIframe: "+isIframe);
		return(isIframe);
	};
	_sb_s.containerWindowIsIFrame = runsInIFrame(window);	// check if we load in an IFrame or in a top window
	_sb_fn.elapsedTicks_sinceStart = elapsedTicks_sinceStart;




	/*
	 * handling site parameters
	 */
	var site_parameters = new Array(),	//site  parameters

	get_site_param = function (key) {
		for (var i = 0; i < site_parameters.length; i++) {
			if (site_parameters[i][0] == key) { return(site_parameters[i][1]); }	// found
		}
		return(null);	// not set
	},
	getall_site_params = function (key) {
		var params = {};
		for (var i = 0; i < site_parameters.length; i++) {
			params[site_parameters[i][0]] = site_parameters[i][1];
		}
		return(params);
	};
	_sb_fn.get_site_param = get_site_param;	// make globally available
	_sb_fn.getall_site_params = getall_site_params;	// make globally available

	try {
		var the_search_parameters = "",
			the_siteparams_run = 0,
			more_siteparams = true;
		while (more_siteparams) {
			the_siteparams_run++;
			the_search_parameters = "";
			switch (the_siteparams_run) {
				case 1:	// add from location.search
					the_search_parameters = fb_flipbookWinLocationSearch;
					break;
				case 2:	// add from iFrame window parameter
					// check window (iframe) attribute "data-fbparams"
					if (fb_flipbookWin.getAttribute) {
						if (fb_flipbookWin.getAttribute("data-fbparams")) {
							the_search_parameters = fb_flipbookWin.getAttribute("data-fbparams");
							//alert("loadedWin data-fbparams: " + the_search_parameters);
							if (!the_search_parameters) the_search_parameters = "";
						}
					}
					break;
				default:
					more_siteparams = false;
					break;
			}

			if (the_search_parameters != "") {
				var site_params_str = the_search_parameters.split("?"),
					site_params_arr = site_params_str[site_params_str.length-1].split("&"),
					keyVal = null, i = 0;
				for (i = 0; i < site_params_arr.length; i++) {
					keyVal = site_params_arr[i].split("=");
					site_parameters[site_parameters.length] = new Array();
					if (keyVal.length < 2) {
						site_parameters[site_parameters.length-1][0] = keyVal[0];
						site_parameters[site_parameters.length-1][1] = "";
					}
					else site_parameters[site_parameters.length-1] = keyVal;
				}
			}
		}
		//alert("site_parameters:\n" + site_parameters);
	} catch(ex) {}



	/*
	 * detect device and browser type
	 */
	var detectTouchDevice = function () {
		//alert(navigator.userAgent + "\nplatform: " + _sb_s.platform);
		var touchable = false,
			canCreateTouchEvent = null;
		//alert("navigator.msMaxTouchPoints: " + navigator.msMaxTouchPoints);
		do {
			if ((('ontouchstart' in window) === true) 
				|| (navigator.maxTouchPoints && (navigator.maxTouchPoints > 0))
				|| (navigator.msMaxTouchPoints && (navigator.msMaxTouchPoints > 0))
				) { touchable = true; break; }
			try {
				canCreateTouchEvent = document.createEvent("TouchEvent");	// Google Chrome can create this even if it is not a touch device!!!
				if (canCreateTouchEvent != null) canCreateTouchEvent = true;
			} catch(ex) {
				//alert("CANNOT canCreateTouchEvent")
				canCreateTouchEvent = false;
				touchable = false;
			}
			if (canCreateTouchEvent && (('ontouchstart' in window) === true)) { touchable = true; break; }
			//if (canCreateTouchEvent && ((window.DocumentTouch && document instanceof DocumentTouch) === true)) { touchable = true; break; }
		} while(false);
		//alert("touchable: " + touchable);
		return touchable;
	},
	get_pointerXY = function (e) {
		if ((typeof(e) == 'undefined') || (e == null)) return null;
		var evt = (e.originalEvent ? e.originalEvent : e),
			touches,
			pXY = null;
		if (!evt) return null;
		do {
			if ((evt.type.indexOf("ointer") > 0) || window.PointerEvent || window.MSPointerEvent) {	// like 'MSPointerMove' or 'pointerdown'
				pXY = { x:evt.pageX, y:evt.pageY, length:1, isPrimary:evt.isPrimary };
				break;
			}
			touches = evt.touches || evt.targetTouches;
			if (touches) {
				pXY = { x:touches[0].pageX, y:touches[0].pageY, length:touches.length, isPrimary:true };
				break;
			}
			// probably a mouse device
			if (evt.pageX) {
				pXY = { x:evt.pageX, y:evt.pageY, length:1, isPrimary:true };
			}
			if (evt.clientX) {
				pXY = { x:evt.clientX + document.documentElement.scrollLeft, y:evt.clientY + document.documentElement.scrollTop, length:1, isPrimary:true };
			}
		} while(false);
		//$.fn.log("log","get_pointerXY: " + evt.type + "\n\tX / Y; " + JSON.stringify(pXY));

		return pXY;
	},

	touchDetector_vars = {					// events stor for lightbox touch actions
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
			swipeTrigger: 10,			// how many pixels to move pointer before it is reported as a 'move'
			isTap: false,				// true or false. if pointer was moved after start: true
			tapTrigger: 200				// how many ticks between start and end to be a tap
	},
	// detect pointer tap and swipe
	touchDetector = function (e){
		//$.fn.log("log","event: " + e.type + "\ntarget: " + e.target.id);
	
		switch(e.type) {
			case _sb_s.pointerEvents.start:
				//$.fn.log("log","touchDetector\n\te.type: " + e.type);
				touchDetector_vars.start = touchDetector_vars.current = new Date().getTime();
				touchDetector_vars.end = null;
				touchDetector_vars.startXY = _sb_fn.get_pointerXY(e);
				touchDetector_vars.endXY.x = touchDetector_vars.endXY.y = null;
				touchDetector_vars.isSwipe = false;
				touchDetector_vars.isTap = false;
				touchDetector_vars.deltaXY.x = touchDetector_vars.deltaXY.y = 0;
				break;

			case _sb_s.pointerEvents.move:
				//$.fn.log("log","touchDetector\n\te.type: " + e.type);
				if (touchDetector_vars.start === null) break;
				touchDetector_vars.current = new Date().getTime();
				touchDetector_vars.endXY = _sb_fn.get_pointerXY(e);
				if (touchDetector_vars.endXY == null)break;
				touchDetector_vars.deltaXY.x = touchDetector_vars.endXY.x - touchDetector_vars.startXY.x;
				touchDetector_vars.deltaXY.y = touchDetector_vars.endXY.y - touchDetector_vars.startXY.y;
				if (   (Math.abs(touchDetector_vars.deltaXY.x) > touchDetector_vars.swipeTrigger)
					|| (Math.abs(touchDetector_vars.deltaXY.y) > touchDetector_vars.swipeTrigger)
					) touchDetector_vars.isSwipe = true;
				else touchDetector_vars.isSwipe = false;
				touchDetector_vars.isTap = false;
				//$.fn.log("log","touchDetector\n\te.type: " + e.type + "\n\tisSwipe: " + touchDetector_vars.isSwipe + "\n\tdeltaXY: " + touchDetector_vars.deltaXY.x + " x " + touchDetector_vars.deltaXY.y);
				break;
			case _sb_s.pointerEvents.end:
			case _sb_s.pointerEvents.out:
			case _sb_s.pointerEvents.cancel:
				touchDetector_vars.current = new Date().getTime();
				if ((touchDetector_vars.current - touchDetector_vars.start) <= touchDetector_vars.tapTrigger) touchDetector_vars.isTap = true;
				else touchDetector_vars.isTap = false;
				touchDetector_vars.start = touchDetector_vars.end = null;
				//$.fn.log("log","touchDetector\n\te.type: " + e.type + "\nisTap: " + touchDetector_vars.isTap + "\nisSwipe: " + touchDetector_vars.isSwipe);
				break;
		}
	
	};
	_sb_fn.get_pointerXY = get_pointerXY;	// make globally available
	_sb_fn.touchDetector = touchDetector;
	_sb_fn.touchDetector_vars = touchDetector_vars;


	_sb_s.isTouchDevice = detectTouchDevice();
	
	
	/****************
	 * detect device orientation
	 */
	/*
	alert("_sb_s.userAgent: " + _sb_s.userAgent
		+ "\n\nwindow.orientation: " + typeof(window.orientation) 
		+ "\nscreen.orientation: " + typeof(screen.orientation) 
		+ "\nwindow.onorientationchange: " + typeof(window.onorientationchange) 
		+ "\nwindow.deviceonorientation: " + typeof(window.deviceonorientation) 
		+ "\nwindow.DeviceOrientationEvent: " + typeof(window.DeviceOrientationEvent) 
		+ "\nwindow.ondeviceonorientation: " + typeof(window.ondeviceonorientation) 
		+ "\nwindow.screen.onorientationchange: " + typeof(window.screen.onorientationchange) 
		+ "\nwindow.screen.mozOrientation: "+ typeof(window.screen.mozOrientation) + (window.screen.mozOrientation ? (": " + window.screen.mozOrientation) : "") );
	*/
	//alert(_sb_s.userAgent + "\n---------\n"+_sb_s.platform);
	_sb_s.is_MobileOS = //(typeof(window.orientation) != 'undefined')	// common
						 //|| (typeof(window.DeviceOrientationEvent) != 'undefined')	// some new technique
						 //|| (typeof(screen.mozOrientation) != 'undefined')	// Firefox Android
						 (_sb_s.userAgentUC.indexOf("ANDROID") > -1)	// ugly un-secure work around. new tablet browsers do not say 'i am a mobile device'
						 || (_sb_s.userAgentUC.indexOf("MOBILE") > -1)
						 || (_sb_s.userAgentUC.indexOf("WPDESK") > -1)	// Windows Phone 8 IE 10 says 'WPDesktop'
						 || (_sb_s.userAgentUC.indexOf("TOUCH") > -1)
						 || (_sb_s.userAgentUC.indexOf("TABLET") > -1)
						 || (_sb_s.userAgentUC.indexOf("PHONE") > -1);



	/* userAgent strings:
		Firefox (Gecko):
			Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:16.0) Gecko/20100101 Firefox/16.0
		Safari (WebKit):
		Some Android also use WebKit:
			Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2
		Chrome (WebKit):
			Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11
		Opera (Presto):
			Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8) Presto/2.12.388 Version/12.10

		Internet Explorer
			V11 on Desktop
				navigator.appName: Netscape
				navigator.userAgent:
					Fullscreen version:     Mozilla/5.0 (Windows NT 6.3; Win64; x64; Trident/7.0; .NET/4.0E; .NET/4.0C; rv:11.0) like Gecko
					Desktop window version: Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; .NET/4.0E; .NET/4.0C; rv:11.0) like Gecko
		
			V10 on Window Phone 8
				navigator.appName: Microsoft Internet Explorer
				navigator.userAgent: Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0; ARM; Touch; WPDesktop)
			
	*/


	//=====================
	// get machine type
	do {
		if ((_sb_s.userAgentUC.indexOf("WINDOWS") > -1) || (_sb_s.platformUC.indexOf("WIN32") > -1) || (_sb_s.platformUC.indexOf("WIN64") > -1)) { _sb_s.is_Win = true; break; }
		if (_sb_s.userAgentUC.indexOf(" MAC") > -1) { _sb_s.is_Mac = true; break; }
		if (_sb_s.userAgentUC.indexOf(" OS X") > -1) { _sb_s.is_OSX = true; break; }
		if (_sb_s.userAgentUC.indexOf(" CROS") > -1) { _sb_s.is_ChromeOS = true; }
		if (_sb_s.userAgentUC.indexOf(" FFOS") > -1) { _sb_s.is_FirefoxOS = true; }
		if ((_sb_s.userAgentUC.indexOf("MOZILLA") > -1) && (_sb_s.userAgentUC.indexOf("MOBILE") > -1) && (_sb_s.userAgentUC.indexOf("FIREFOX") > -1)) { _sb_s.is_FirefoxOS = true; }
		// not break for more Chrome os test (is Linux)
		if (_sb_s.platformUC.indexOf("LINUX ARM") > -1) { _sb_s.is_Linux = true; _sb_s.is_Android = true; }
		if (_sb_s.platformUC.indexOf("LINUX") > -1) _sb_s.is_Linux = true;
		if (_sb_s.userAgentUC.indexOf("LINUX") > -1) _sb_s.is_Linux = true;
		// take as last: some devices do not identify as 'Android'
		if ((_sb_s.userAgentUC.indexOf("ANDROID") > -1) || (!_sb_s.is_Win && !_sb_s.is_ChromeOS && !_sb_s.is_FirefoxOS  && !_sb_s.is_OSX && !_sb_s.is_Mac && !_sb_s.is_iOS ? true : false)) _sb_s.is_Android = true;
	} while(false);

	//=====================
	// get OS version
	var nameOffset,verOffset,ix;
	do {
		//alert("_sb_s.platform: " + _sb_s.platform + "\n\n_sb_s.userAgent: " + _sb_s.userAgent);
		// be careful!
		// Firefox Android states userAgent like: Mozilla/5.0 (Android; Tablet; rv:39.0) Gecko/9.0 Firefox/39.0
		if (_sb_s.is_Android) {	// search for ....; Android 2.2.2; ...
			if ((verOffset=_sb_s.userAgent.indexOf(" Android ")) > -1) {
				verOffset += 9;
				_sb_s.AndroidVersion = parseFloat(_sb_s.userAgent.substring(verOffset, _sb_s.userAgent.indexOf(";", verOffset)));	// we want 2.2 from 2.2.2 only
			}
		}
		if (_sb_s.is_iOS) {
			if ((verOffset=_sb_s.userAgent.indexOf(" OS ")) > -1) {
				verOffset += 4;
				_sb_s.iOSVersion = parseInt(_sb_s.userAgent.substr(verOffset), 10);
			}
		}
	} while(false);


	//=====================
	// get browser
	do {
		if (_sb_s.userAgentUC.indexOf("SAFARI") > -1) { _sb_s.is_Safari = true; break; }
		if (_sb_s.userAgentUC.indexOf("FIREFOX") > -1) { _sb_s.is_Firefox = true; break; }
		if (_sb_s.userAgentUC.indexOf("CHROME") > -1) { _sb_s.is_Chrome = true; break; }
		if (_sb_s.userAgentUC.indexOf("OPERA") > -1) { _sb_s.is_Opera = true; break; }
		_sb_s.is_otherBrowser = true; // like (_sb_s.userAgent.toLowerCase().indexOf("series60") != -1) || (_sb_s.userAgent.toLowerCase().indexOf("symbian") != -1) || (_sb_s.userAgent.toLowerCase().indexOf("windows ce") != -1) || (_sb_s.userAgent.toLowerCase().indexOf("blackberry") != -1)
	} while(false);

	//=====================
	// get browser render engine
	if (_sb_s.userAgentUC.indexOf("WEBKIT") > -1) _sb_s.renderEngine = "webkit";
		else if (_sb_s.userAgentUC.indexOf("GECKO") > -1) _sb_s.renderEngine = "gecko";
			else if (_sb_s.userAgentUC.indexOf("PRESTO") > -1) _sb_s.renderEngine = "presto";
				else if (_sb_s.userAgentUC.indexOf("TRIDENT") > -1) _sb_s.renderEngine = "trident";
					else if (_sb_s.userAgentUC.indexOf("KHTML") > -1) _sb_s.renderEngine = "khtml";

	//=====================
	// detect devices
	// iOS stuff
	// userAgent like: Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_3 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5
	if (_sb_s.platform.indexOf("iPad") > -1) { _sb_s.is_iOS = _sb_s.is_iPad = true; }
	if (_sb_s.platform.indexOf("iPhone") > -1) { _sb_s.is_iOS = _sb_s.is_iPhone = true; }
	if (_sb_s.userAgentUC.indexOf("PHONE") > -1) { _sb_s.is_Phone = true; }

	try {
		// In some MSIE, the true version is after "MSIE" in userAgent
		if ((verOffset=_sb_s.userAgent.indexOf("MSIE")) > -1) {
			 _sb_s.browserName = "Microsoft Internet Explorer";
			 _sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+5);
		}
		// In new MSIE, the true version is after "rv:" in userAgent
		else if ((_sb_s.userAgentUC.indexOf("TRIDENT") > -1) && (_sb_s.userAgentUC.indexOf(".NET") > -1) && (verOffset=_sb_s.userAgent.indexOf(" rv:")) > -1) {
				do {
					if (_sb_s.is_Win) {
						_sb_s.browserName = "Microsoft Internet Explorer";
			 			_sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+4);
			 			break;
			 		}
			 	}while(false);
		}
		// In Chrome, the true version is after "Chrome" 
		else if ((verOffset=_sb_s.userAgent.indexOf("Chrome"))!=-1) {
			 _sb_s.browserName = "Chrome";
			 _sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+7);
		}
		// In Safari, the true version is after "Safari" or after "Version" 
		else if ((verOffset=_sb_s.userAgent.indexOf("Safari"))!=-1) {
			_sb_s.browserName = "Safari";
			_sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+7);
			if ((verOffset=_sb_s.userAgent.indexOf("Version"))!=-1) 
				_sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+8);
		}
		// In Firefox, the true version is after "Firefox" 
		else if ((verOffset=_sb_s.userAgent.indexOf("Firefox"))!=-1) {
			_sb_s.browserName = "Firefox";
			_sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+8);
		}
		// In Opera, the true version is after "Opera" or after "Version"
		else if ((verOffset=_sb_s.userAgent.indexOf("Opera"))!=-1) {
			_sb_s.browserName = "Opera";
			_sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+6);
			if ((verOffset=_sb_s.userAgent.indexOf("Version"))!=-1) 
				_sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+8);
		}
		// In most other browsers, "name/version" is at the end of userAgent 
		else if ( (nameOffset=_sb_s.userAgent.lastIndexOf(' ')+1) < (verOffset=_sb_s.userAgent.lastIndexOf('/')) ) {
			_sb_s.browserName = _sb_s.userAgent.substring(nameOffset,verOffset);
			_sb_s.UAfullVersion = _sb_s.userAgent.substring(verOffset+1);
		}
		// trim the UAfullVersion string at semicolon/space if present
		if ((ix=_sb_s.UAfullVersion.indexOf(";"))!=-1) _sb_s.UAfullVersion=_sb_s.UAfullVersion.substring(0,ix);
		if ((ix=_sb_s.UAfullVersion.indexOf("."))!=-1) _sb_s.UAfullVersion=_sb_s.UAfullVersion.substring(0,ix);
		if ((ix=_sb_s.UAfullVersion.indexOf(" "))!=-1) _sb_s.UAfullVersion=_sb_s.UAfullVersion.substring(0,ix);
	
		_sb_s.UAmajorVersion = parseInt(''+_sb_s.UAfullVersion,10);
		if (isNaN(_sb_s.UAmajorVersion)) {
			_sb_s.UAfullVersion  = ''+parseFloat(navigator.appVersion); 
			_sb_s.UAmajorVersion = parseInt(navigator.appVersion,10);
		}
	}
	catch(ex) {}
	if (       (navigator.appName.toUpperCase().indexOf("NETSCAPE")>-1)	// Windows Internet Explorer 11 and newer identifies as Netscape
			&& (_sb_s.userAgentUC.indexOf("TRIDENT")>-1)
			&& (_sb_s.userAgent.indexOf(" rv:")>-1)
		) {
		_sb_s.is_IE = true;
		_sb_s.is_IElt9 = false; _sb_s.IEVersion = _sb_s.UAfullVersion;
		//alert("is_IE v: " + _sb_s.IEVersion);
	}
	if ((navigator.appName.toUpperCase().indexOf("EXPLORER")>-1) || (_sb_s.userAgent.indexOf("IEMobile")>-1)) {	// Windows Phone Explorer 10 or 11
		_sb_s.cur_lang=navigator.userLanguage;	//for Internet Explorer
		_sb_s.cur_lang=_sb_s.cur_lang.substr(0,2);
		_sb_s.is_IE = true;
		do {
			if (_sb_s.userAgent.indexOf("MSIE 5") >= 0) { _sb_s.is_IE5 = true; _sb_s.is_IElt9 = true; _sb_s.IEVersion = 5; break; }
			if (_sb_s.userAgent.indexOf("MSIE 6") >= 0) { _sb_s.is_IE6 = true; _sb_s.is_IElt9 = true; _sb_s.IEVersion = 6; break; }
			if (_sb_s.userAgent.indexOf("MSIE 7") >= 0) { _sb_s.is_IE7 = true; _sb_s.is_IElt9 = true; _sb_s.IEVersion = 7; break; }
			if (_sb_s.userAgent.indexOf("MSIE 8") >= 0) { _sb_s.is_IE8 = true; _sb_s.is_IElt9 = true; _sb_s.IEVersion = 8; break; }
			if (_sb_s.userAgent.indexOf("MSIE 9") >= 0) { _sb_s.is_IE9 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 9; break; }
			if (_sb_s.userAgent.indexOf("MSIE 10") >= 0) { _sb_s.is_IE10 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 10; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 11") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/11") >= 0)) { _sb_s.is_IE11 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 11; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 12") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/12") >= 0)) { _sb_s.is_IE12 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 12; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 13") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/13") >= 0)) { _sb_s.is_IE13 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 13; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 14") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/14") >= 0)) { _sb_s.is_IE14 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 14; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 15") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/15") >= 0)) { _sb_s.is_IE15 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 15; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 16") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/16") >= 0)) { _sb_s.is_IE16 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 16; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 17") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/17") >= 0)) { _sb_s.is_IE17 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 17; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 18") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/18") >= 0)) { _sb_s.is_IE18 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 18; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 19") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/19") >= 0)) { _sb_s.is_IE19 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 19; break; }
			if ((_sb_s.userAgent.indexOf("MSIE 20") >= 0) || (_sb_s.userAgent.indexOf("IEMobile/20") >= 0)) { _sb_s.is_IE20 = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 20; break; }
			// set large enough
			_sb_s.is_IE = true; _sb_s.is_IElt9 = false; _sb_s.IEVersion = 99;
		} while(false);
	}
	else {//like: " Mozilla/5.0 (Macintosh, U; PPC Mac OS X Mach-O; de-DE; rv:1.7.10) Gecko/20050717 Firefox/1.0.6
		//lets filter the 4th part - semicolon separated - within the ()
		try {
			if (window.navigator.language) {
				_sb_s.cur_lang = window.navigator.language.substr(0,2);
			}
			else {
				var start=_sb_s.userAgent.indexOf("("),
					end=_sb_s.userAgent.indexOf(")"),
					verstr=_sb_s.userAgent.substring(start+1,end),
					parts=verstr.split("; ");
				if (parts.length > 3) _sb_s.cur_lang=parts[3].substr(0,2);	// FF 4, opera WIN have 3 parts only
				else {
					_sb_s.cur_lang = "en";
					try {	// may be, we should get this from HTTP_ACCEPT_LANGUAGE
						_sb_s.cur_lang = window.navigator.language.substr(0,2);
					} catch(ex) { _sb_s.cur_lang = "en"; }
				}
			}
		} catch(ex) { _sb_s.cur_lang="en"; }
	}



	var lang_override = null;
	try { lang_override = get_site_param("lang"); }
	catch(ex1) {
		try { lang_override = window.opener.parent.get_site_param("lang"); } catch(ex) {}
	}
	if (lang_override != null) _sb_s.cur_lang = lang_override;
	switch (_sb_s.cur_lang) {	//make sure the translated texts are available, otherwise set english
		case "de": _sb_s.cur_lang_ID=1; break;
		case "fr": _sb_s.cur_lang_ID=2; break;
		case "da": _sb_s.cur_lang_ID=3; break;
		case "pl": _sb_s.cur_lang_ID=4; break;
		case "en": default:
			_sb_s.cur_lang="en"; _sb_s.cur_lang_ID=0; break;
	}

	

	// we have click, pen or touch events
	if (_sb_s.isTouchDevice) {
		_sb_s.pointerEvents = { start:"touchstart", move:"touchmove", end:"touchend", out:"touchleave", cancel:"touchcancel" };
	}
	if (window.PointerEvent) {
		_sb_s.pointerEvents = { start:"pointerdown", move:"pointermove", end:"pointerup", out:"pointerout", cancel:"pointercancel" };
	}
	else {
		if (window.MSPointerEvent) {
			_sb_s.pointerEvents = { start:"MSPointerDown", move:"MSPointerMove", end:"MSPointerUp", out:"MSPointerOut", cancel:"MSPointerCancel" };
		}
	}
	_sb_s._elementTouch = _sb_s.pointerEvents.end;


	//alert("platform: " + _sb_s.platform + "\nappName: " + navigator.appName + "\nappVersion: " + navigator.appVersion + "\nlanguage: " + navigator.language + "\nuserLanguage: " + navigator.userLanguage + "\nuserAgent: " + navigator.userAgent + "\nonorientationchange: " + ("onorientationchange" in window) + "\norientation avail: " + ("orientation" in window) + "\norientation: " + (window.orientation) + "\nonorientation: " + ("onorientation" in window) + "\n_sb_s.orientation: " + _sb_s.orientation + "\n\nisTouchDevice: " + _sb_s.isTouchDevice + "\nis_MobileOS: " + _sb_s.is_MobileOS + "\nis_Phone: " + _sb_s.is_Phone + "\nis_IE: " + _sb_s.is_IE + "\nIEVersion: " + _sb_s.IEVersion + "\npointerEvents: " + JSON.stringify(_sb_s.pointerEvents));


	/* ========================
	 * the last clicked/hovered article
	 */
	var current_article = {
		obj : null,
		obj_id : null,
		coords : null,	//coords as string "x1,y1,x2,y2"
		page : "",
		page_number : "",
		pageside : "",
		srcxml : "",
		tit : "",	// article title
		txt : ""	// article text
	},



	/* ========================
	 * a logger window
	 */
 	log_methods = {
		init : function( options ) {
			return(null);
		},
		
		setup_logwin: function () {
				var logwin_clear = function(e) {
						document.getElementById("loggingdiv").innerHTML="";
						logwin_stopPropagation(e);
						return false;
					},
					logwin_hide = function(e) {
						document.getElementById("loggingdivcont").style.display="none";
						return false;
						//logwin_stopPropagation(e);
					},
					logwin_stopPropagation = function(e) {
						 if (e && e.stopPropagation) e.stopPropagation();
						 return true;	// return true to allow text selection
					},
					loggingdivcont = document.createElement('div');
				loggingdivcont.id = "loggingdivcont";
				loggingdivcont.innerHTML = "<div id='loggingdivtitle'><span id='loggingdivclear' title='click to clear messages'>C</span> <span id='loggingdivhide' title='Hide messages'>X</span>Logging enabled</div><div id=\"loggingdiv\"></div>";
				if (_sb_e.body) _sb_e.body.append(loggingdivcont);
				else $('body').append(loggingdivcont);
				_sb_e.logger_console = document.getElementById('loggingdiv');
				if (_sb_e.logger_console.addEventListener) {	// W3C DOM
					document.getElementById("loggingdivclear").addEventListener(_sb_s.pointerEvents.start, logwin_clear, false);
					document.getElementById("loggingdivhide").addEventListener(_sb_s.pointerEvents.start, logwin_hide, false);
					_sb_e.logger_console.addEventListener(_sb_s.pointerEvents.start, logwin_stopPropagation, false);	// return true to allow text selection
					_sb_e.logger_console.addEventListener(_sb_s.pointerEvents.move, logwin_stopPropagation, false);	// return true to allow text selection
				}
				else if (_sb_e.logger_console.attachEvent) { // IE <= 8 DOM
					document.getElementById("loggingdivclear").attachEvent("on"+_sb_s.pointerEvents.start, logwin_clear);
					document.getElementById("loggingdivhide").attachEvent("on"+_sb_s.pointerEvents.start, logwin_hide);
					_sb_e.logger_console.attachEvent("on"+_sb_s.pointerEvents.start, logwin_stopPropagation);	// return true to allow text selection
					_sb_e.logger_console.attachEvent("on"+_sb_s.pointerEvents.move, logwin_stopPropagation);	// return true to allow text selection
				}
				loggingdivcont.style.display = "block";
				// set body to visible
				document.getElementById("sb_body").style.visibility = "visible";
				setTimeout(function(){ new XdraggableObject(document.getElementById("loggingdivtitle"),loggingdivcont);},100);
		},

		/**
		 * Show a message to the logging window
		 * call like:
		 *		logmethos.log('itsme', 'my message to show');
		 *
		 * @param {string} caller the name of the function which calls us or any string
		 * optional parameters;
		 * @param {string|null=} message the message text (may also be in caller
		 * @param {string|null=} url the url of the function which is calling us
		 * @param {string|null=} line the line number within the script
		 * @param {boolean|null=} syserror true or false to indicate that this message is a system error
		 */
		log : function (caller,message,url,line,syserror) {
			if (_sb_s.DEBUGmode <= 0) return false;
			if (_sb_e.logger_console == null) {
				switch (_sb_s.DEBUGmode) {
					case 1: case 0:
						_sb_e.logger_console = document.getElementById('debugmessage');
						break;
					default:
						log_methods.setup_logwin();
						break;
				}
			}
			if (message == "*CLEAR*") {
				_sb_e.logger_console.innerHTML = "";
				return true;
			}
			var mess = "";
			if (syserror) mess += "#### ";
			mess += new Date().getTime() + ": ";
			if (caller) mess += caller + "\n";
			else mess += "\n";
			if (url) mess += "\tURL: " + url + "\n";
			if (line) mess += "\tLine #"+line + "\n" ;
			mess += "\t" + (typeof(message) == 'undefined' ? "" : message);
			mess = mess.replace(/\n/gi,"<br>");
			mess = mess.replace(/\t/gi,"&nbsp;&nbsp;&nbsp;&nbsp;");
			switch (_sb_s.DEBUGmode) {
				case 1: case 0:
					_sb_e.logger_console.innerHTML = mess;
					break;
				default:
					if (_sb_e.logger_console.innerHTML.length > 50000) _sb_e.logger_console.innerHTML = "";
					document.getElementById('loggingdivcont').style.display = "block";
					$(_sb_e.logger_console).append(mess + "<br>");
					_sb_e.logger_console.scrollTop = _sb_e.logger_console.scrollHeight;
					break;
			}
			return true;
		}

	};
	
	$.fn.log = function (method,message,url,line,syserror) {
		if (_sb_e.body == null) {
			setTimeout(function(){$.fn.log(method,message,url,line,syserror);},200);
			return(false);
		}
		// Method calling logic
		if ( log_methods[method] ) {
			return log_methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else if ( typeof method === 'object' || ! method ) {
			return log_methods.init.apply( this, arguments );
		} else {
			$.error( 'Method ' +  method + ' does not exist on jQuery.log' );
		}
		return(true);
	};
	/* ======= END a logger window ======= */




	/* ========================
	 * detect vendor prefix
	 */
	var getVendorPrefix = function () {
		var tmp = document.createElement("div"),
			prefixes = ['','webkit','Moz','ms','Khtml','O'],
			i;
				/* //DEBUG only
     			for (var prop in tmp.style) {
   							//$.fn.log("log","Vendor prop: " + prop);
	   				if ( (prop.toLowerCase().indexOf("transform") > -1) 
     						|| (prop.toLowerCase().indexOf("transition") > -1)
     						|| (prop.toLowerCase().indexOf("gradient") > -1)
     					 )
 							$.fn.log("log","Vendor prop: " + prop);
 				}
 				*/
 			
 		// check for 'touch-action' support	
		/* // DEBUG only
		for (i in tmp.style) {
			if ((i.toLowerCase().indexOf("touch") > -1) || (i.toLowerCase().indexOf("scroll") > -1) || (i.toLowerCase().indexOf("overflow") > -1)) {
				alert(i + ": " + tmp.style[i]);
			}
		}
		*/
		// check for touch action style
		for (i in prefixes) {
			if ((typeof tmp.style[prefixes[i] + 'TouchAction'] != 'undefined') || (typeof tmp.style[prefixes[i] + 'touchAction'] != 'undefined')) {
				if (prefixes[i] == "") _sb_s.touchActionStyle = 'touch-action';
				else _sb_s.touchActionStyle = "-" + prefixes[i] + '-touch-action';
				//alert("touchActionStyle: " + _sb_s.touchActionStyle);
				break;
			}
		}
		// check for transitionEnd event name
		var transitionEnd_events = {
			'transition': 'transitionend',
			'WebkitTransition': 'webkitTransitionEnd',
			'MozTransition': 'transitionend',
			'otransition': 'otransitionend',
			"oTransition" : "oTransitionEnd"
		};

		for (i in transitionEnd_events) {
			if (tmp.style[i] !== undefined) {
				_sb_s.transitionEndEvent = transitionEnd_events[i];
				break;
			}
		}
		//alert("_sb_s.transitionEndEvent: " + _sb_s.transitionEndEvent);


		// MUST be last test
		// check for native transform prefix
		for (i in prefixes) {
			if ((typeof tmp.style[prefixes[i] + 'Transform'] != 'undefined') || (typeof tmp.style[prefixes[i] + 'transform'] != 'undefined')) {
					//alert("vendor: '" + prefixes[i] + "' " + (prefixes[i] == ""));
				if (prefixes[i] == "") {
					_sb_s.CamelVendorPrefix = "";
					return("*");			// we can the 'transform' without prefix
				}
				_sb_s.CamelVendorPrefix = prefixes[i];
				tmp = null;
				return ('-'+prefixes[i].toLowerCase()+'-');	// we need a prefix
			}
		}
		tmp = null;
		//alert("NONATIVE");
		_sb_s.CamelVendorPrefix = "";
		return("NONATIVE");
	};
	_sb_s.vendorPrefix = getVendorPrefix();
	if (_sb_s.vendorPrefix == "*") {
		_sb_s.nativeTransform = "transform";			// we can the 'transform' without prefix
		_sb_s.vendorPrefix = "";
	} else if (_sb_s.vendorPrefix == "NONATIVE") {
				_sb_s.nativeTransform = "";			// we can not 'transform'
				_sb_s.allowNativeTransitions = false;
			} else {
					if (_sb_s.is_IE && (_sb_s.IEVersion >= 10) && _sb_s.vendorPrefix == "-ms-") _sb_s.nativeTransform = "transform";	// IE10 > uses no transform prefix, IE9 uses
					else _sb_s.nativeTransform = _sb_s.vendorPrefix + "Transform";			// we can '-xxx-Transform'
					}
	//alert("vendorPrefix: " + _sb_s.vendorPrefix + "\n_sb_s.nativeTransform: " + _sb_s.nativeTransform);
	/* ====== END detect vendor prefix ====== */


	/* ========================
	 * dynamic js and css loader wait for loaded js or css
	 */
	var loadjscssfileWait = function (cssid, onload_callback) {
		var styles, i;
		// check if style already is set
		styles = _sb_e.headEl.getElementsByTagName("link");
		for (i = 0; i < styles.length; i++) {
			if (styles[i].id && (styles[i].id == cssid)) {	// found it
				//alert("loaded: " + cssid + "\n" + onload_callback);
				//log_methods.log("list css: " + cssid + "\ntype: " + styles[i].sheet + "\n"  + _sb_fn.list_object(styles[i].sheet,"css",'link',true,"\n"));
				if (hasCssRules(styles[i].sheet) < 1) {
					setTimeout(function(){_sb_fn.loadjscssfileWait(cssid, onload_callback);},100);
					return;
				}
				setTimeout(onload_callback,0);
				return;
			}
		}
		// not already found: wait
		setTimeout(function(){_sb_fn.loadjscssfileWait(cssid, onload_callback);},10);
	},

	/**
	 * dynamic js and css loader
	 *
     * @param {string} filename the url to the file to load
     * @param {string} filetype the type of the file: 'js'or 'css'
     * optional parameters;
     * @param {string|null=} cssmedia the media type attribute to set
     * @param {string|null=} cssid the media id attribute to set
     * @param {boolean=} top if this 'link' or 'script' element is to be set at the top of 'head' element
     * @param {Function|null=} onload_callback the function to call when loaded
	 */
	loadjscssfile = function (filename, filetype, cssmedia, cssid, top, onload_callback) {
		var fileref = null,
			head = document.getElementsByTagName("head")[0];
		if (filetype == "js") {	// filename is external javascript file
			fileref=document.createElement('script');
			fileref.setAttribute("type","text/javascript");
			fileref.setAttribute("src", filename);
		}
		else if (filetype == "css") {	// filename is external CSS file
				fileref=document.createElement("link");
				fileref.setAttribute("rel", "stylesheet");
				fileref.setAttribute("type", "text/css");
				fileref.setAttribute("href", filename);
				if (cssid) fileref.setAttribute("id", cssid);
				if (cssmedia) fileref.setAttribute("media", cssmedia);
			}
		if (fileref != null) {
			if (onload_callback) {
				// onload event does not fire on android (at least 2.2.2)
				//alert(("onload" in fileref) + "\n" + fileref["onload"]);
				//log_methods.log("LINK onload event:\n" + _sb_fn.list_object(fileref,"load",'link',true,"\n"));
				setTimeout(function(){_sb_fn.loadjscssfileWait(cssid, onload_callback);},100);
			}
			if (!top) head.appendChild(fileref);
			else head.insertBefore(fileref,head.firstChild);
		}
	};
	_sb_fn.loadjscssfileWait = loadjscssfileWait;	// make globally available
	_sb_fn.loadjscssfile = loadjscssfile;



	/* ========================
	 * dynamically set meta viewport tag
	 */
	var setMetaViewPort = function() {
		var viewportStyle, styles,
			already_loaded = false,
			new_loaded = false,
			i;
		// below added elements will be in reverse order because will always add to the top of head
		//alert("is_MobileOS: " + _sb_s.is_MobileOS + "\npageDisplay: " + _sb_s.pageDisplay + "\nhas_metaViewPort: " + has_metaViewPort());
		//alert("platform: " + _sb_s.platform + "\nappName: " + navigator.appName + "\nappVersion: " + navigator.appVersion + "\nlanguage: " + navigator.language + "\nuserLanguage: " + navigator.userLanguage + "\nuserAgent: " + navigator.userAgent + "\nonorientationchange: " + ("onorientationchange" in window) + "\norientation: " + ("orientation" in window) + "\nonorientation: " + ("onorientation" in window));

		// as always: IEmobile XX on Windows Phone X special:
		if (_sb_s.load_device_css 
			// Windows Phone 8.0
			|| _sb_s.userAgent.match(/IEMobile\/10\./)
			|| _sb_s.userAgent.match(/MSIE 10\./)
			// Windows Phone OS 7.5
			|| _sb_s.userAgent.match(/IEMobile\/9\./)
			|| _sb_s.userAgent.match(/MSIE 9\./)
			) {
			//log_methods.log("set viewport style\norientation: " + _sb_s.orientation + "\n" + _sb_s.ms10ViewPortStyle[_sb_s.orientation]);
			// set viewport css as style element like "@-ms-viewport{width:400px; user-zoom:fixed; max-zoom: 1; min-zoom: 1; }"
			if (_sb_s.ms10ViewPortStyle && (_sb_s.ms10ViewPortStyle[_sb_s.orientation]) && (_sb_s.ms10ViewPortStyle[_sb_s.orientation] != "")) {
				// check if viewport style already is set
				styles = _sb_e.headEl.getElementsByTagName("style");
				for (i = 0; i < styles.length; i++) {
					if (styles[i].id.indexOf("ms-viewport_") == 0) {	// found one
						// check if this is the wanted one
						if (styles[i].id == _sb_s.ms10ViewPortStyle[_sb_s.orientation].id) {	// exactly this one we want
							already_loaded = true;
							break;
						}
						else {	// AN OTHER IS LOADED: REMOVE IT
							styles[i].parentNode.removeChild(styles[i]);
						}
					}
				}

				//log_methods.log(_sb_s.ms10ViewPortStyle[_sb_s.orientation].id + "\nis already loaded: " + already_loaded);
				if (!already_loaded) {
					//log_methods.log("add ms viewport style:\n" + _sb_s.ms10ViewPortStyle[_sb_s.orientation].style);
					viewportStyle = document.createElement("style");
					viewportStyle.setAttribute("id", _sb_s.ms10ViewPortStyle[_sb_s.orientation].id);
					viewportStyle.appendChild(
						document.createTextNode(_sb_s.ms10ViewPortStyle[_sb_s.orientation].style)
					);
					_sb_e.headEl.insertBefore(viewportStyle,_sb_e.headEl.firstChild);	// no difference to appendChild
					if (_sb_s.ms10reload) _sb_s.ms10reloadDO = true;
					new_loaded = true;
				}
			}

			// load viewport css as link element like "flipbook_ms10viewport.css" containing "@-ms-viewport{width:400px; user-zoom:fixed; max-zoom: 1; min-zoom: 1; }"
			if (_sb_s.ms10ViewPortLink && (_sb_s.ms10ViewPortLink != "")) {
				//alert("add ms viewport stylesheet as link: " + _sb_s.ms10ViewPortLink);
				_sb_fn.loadjscssfile(_sb_s.xslcssPath + _sb_s.ms10ViewPortLink,"css",null,null,false);
				if (_sb_s.ms10reload) _sb_s.ms10reloadDO = true;
				new_loaded = true;
			}
		}

		if (_sb_s.viewPortStyle && (_sb_s.viewPortStyle != "")) {
			//alert("add general viewport style");
			viewportStyle = document.createElement("style");
			viewportStyle.appendChild(
				document.createTextNode(_sb_s.viewPortStyle)
			);
			_sb_e.headEl.insertBefore(viewportStyle,_sb_e.headEl.firstChild);	// no difference to appendChild
		}

		// check if meta tag 'viewport' is set for mobile devices
		if (_sb_s.defaultMetaViewPort && (_sb_s.defaultMetaViewPort != null) && !has_metaViewPort()) {	// add meta tag if not already there
			//alert("add meta viewport");
			var meta, metadef, m, k, v;

			if (_sb_s.defaultMetaViewPort != null) {
				for (m in _sb_s.defaultMetaViewPort) {
					meta = document.createElement('meta');
					metadef = _sb_s.defaultMetaViewPort[m];
					for (k in metadef) {
						meta.setAttribute(k,metadef[k]);
					}
					_sb_e.headEl.insertBefore(meta,_sb_e.headEl.firstChild);	// no difference to appendChild
					//_sb_e.headEl.appendChild(meta);
				}
			}
		}
		_sb_s.viewPortStyles_set = true;
		return new_loaded;
	},


	/* ========================
	 * dynamic loader of device specific CSS
	 */
	loadDeviceCSS = function (devWidth, thecss, id, callback) {
		var haveLoaded = false,
			links, i, w, css = thecss;
		if (!css) {	// auto detect
		}
		else {
			if (css.indexOf("/") > -1) {
				css = css.split("/");
				css = css[css.length-1];
			}
		}
		
		//log_methods.log("loadDeviceCSS: " + _sb_s.load_device_css + "\nis_Phone: " + _sb_s.is_Phone + "\nclient w x h: " + _sb_s.clientWidth + " x " + _sb_s.clientHeight + "\npage w x h: " + _sb_s.pageWidth + " x " + _sb_s.pageHeight + "\norientation: " + _sb_s.orientation);
		if (_sb_s.load_device_css || (_sb_s.is_Phone && _sb_s.is_IE && (_sb_s.IEVersion <= 10))) {
			links = (id ? document.getElementById(id) :document.getElementsByTagName("link"));
			// check if already loaded
			for (i = links.length-1; i >= 0; i--) {
				//log_methods.log("loadDeviceCSS\ncss: " + css + "\nlinks[" + i + "].href: " + links[i].href);
				if (links[i].href.indexOf("/"+css) > -1) {	// delete if IS loaded and reload at end of StyleSheets
					//alert("css already loaded: " + css);
					$('link[rel=stylesheet][href$="' + css + '"]').remove();	// delete and reload
					break;
					// move to end of head does not move internal styleSheet position 
					//_sb_e.head.append($(links[i]));
					//return;
				}
			}

			if (!css) {
				// detect css file to load
				for (w in _sb_s.deviceCSS[_sb_s.orientation]) {
					//log_methods.log("post loadDeviceCSS:\n" + _sb_s.xslcssPath + _sb_s.deviceCSS[_sb_s.orientation][w].href + "'\nclient w x h: " + _sb_s.clientWidth + " x " + _sb_s.clientHeight + "\npage w x h: " + _sb_s.pageWidth + " x " + _sb_s.pageHeight);
					//alert("devwidth: " + w);
					if (_sb_s.clientWidth <= (devWidth  ? devWidth : w)) {
						//log_methods.log("loadDeviceCSS loading:\n " + _sb_s.xslcssPath + _sb_s.deviceCSS[_sb_s.orientation][w].href + "'\nclient w x h: " + _sb_s.clientWidth + " x " + _sb_s.clientHeight + "\npage w x h: " + _sb_s.pageWidth + " x " + _sb_s.pageHeight);
						//_sb_fn.loadjscssfile(_sb_s.xslcssPath + "flipbook_w400.css","css",null,(id ? id : "deviceCSS"));
						_sb_fn.loadjscssfile(_sb_s.xslcssPath + _sb_s.deviceCSS[_sb_s.orientation][w].href,"css",null,(_sb_s.deviceCSS[_sb_s.orientation][w].id ? _sb_s.deviceCSS[_sb_s.orientation][w].id : null),false,callback);
						haveLoaded = true;
						break;
					}
				}
			}
			else {
				_sb_fn.loadjscssfile(_sb_s.xslcssPath + css,"css",null,(id ? id : null));
				haveLoaded = true;
			}
		}
		return haveLoaded;
	},

	getSlideBookSettings = function () {
		_sb_e.html = $("html");
		_sb_e.body = $("#sb_body");
			_sb_e.body_borderLeftWidth = cssIntVal(_sb_e.body.css('borderLeftWidth'));
			_sb_e.body_borderTopWidth = cssIntVal(_sb_e.body.css('borderTopWidth'));
			_sb_e.body_borderRightWidth = cssIntVal(_sb_e.body.css('borderRightWidth'));
			_sb_e.body_borderBottomWidth = cssIntVal(_sb_e.body.css('borderBottomWidth'));
		_sb_e.bodyEl = document.getElementById("sb_body");
		_sb_e.sb_container = $('#sb_container');
			_sb_e.sb_container_paddingLeft = cssIntVal(_sb_e.sb_container.css("paddingLeft"));
			_sb_e.sb_container_paddingTop = cssIntVal(_sb_e.sb_container.css("paddingTop"));
			_sb_e.sb_container_paddingRight = cssIntVal(_sb_e.sb_container.css("paddingRight"));
			_sb_e.sb_container_paddingBottom = cssIntVal(_sb_e.sb_container.css("paddingBottom"));
			_sb_e.sb_container_marginLeft = cssIntVal(_sb_e.sb_container.css("marginLeft"));
			_sb_e.sb_container_marginTop = cssIntVal(_sb_e.sb_container.css("marginTop"));
			_sb_e.sb_container_marginRight = cssIntVal(_sb_e.sb_container.css("marginRight"));
			_sb_e.sb_container_marginBottom = cssIntVal(_sb_e.sb_container.css("marginBottom"));
			_sb_e.sb_container_borderLeftWidth = cssIntVal(_sb_e.sb_container.css("borderLeftWidth"));
			_sb_e.sb_container_borderTopWidth = cssIntVal(_sb_e.sb_container.css("borderTopWidth"));
			_sb_e.sb_container_borderRightWidth = cssIntVal(_sb_e.sb_container.css("borderRightWidth"));
			_sb_e.sb_container_borderBottomWidth = cssIntVal(_sb_e.sb_container.css("borderBottomWidth"));
		_sb_e.scrollview_header = $('#scrollview-header');
		_sb_e.scrollview_container = $('#scrollview-container');
			_sb_e.scrollview_container_paddingLeft = cssIntVal(_sb_e.scrollview_container.css("paddingLeft"));
			_sb_e.scrollview_container_paddingTop = cssIntVal(_sb_e.scrollview_container.css("paddingTop"));
			_sb_e.scrollview_container_paddingRight = cssIntVal(_sb_e.scrollview_container.css("paddingRight"));
			_sb_e.scrollview_container_paddingBottom = cssIntVal(_sb_e.scrollview_container.css("paddingBottom"));
			_sb_e.scrollview_container_marginLeft = cssIntVal(_sb_e.scrollview_container.css("marginLeft"));
			_sb_e.scrollview_container_marginTop = cssIntVal(_sb_e.scrollview_container.css("marginTop"));
			_sb_e.scrollview_container_marginRight = cssIntVal(_sb_e.scrollview_container.css("marginRight"));
			_sb_e.scrollview_container_marginBottom = cssIntVal(_sb_e.scrollview_container.css("marginBottom"));
			_sb_e.scrollview_container_borderLeftWidth = cssIntVal(_sb_e.scrollview_container.css("borderLeftWidth"));
			_sb_e.scrollview_container_borderTopWidth = cssIntVal(_sb_e.scrollview_container.css("borderTopWidth"));
			_sb_e.scrollview_container_borderRightWidth = cssIntVal(_sb_e.scrollview_container.css("borderRightWidth"));
			_sb_e.scrollview_container_borderBottomWidth = cssIntVal(_sb_e.scrollview_container.css("borderBottomWidth"));
		_sb_e.scrollview_content = $("#scrollview-content");
			_sb_e.scrollview_content_paddingLeft = cssIntVal(_sb_e.scrollview_content.css("paddingLeft"));
			_sb_e.scrollview_content_paddingTop = cssIntVal(_sb_e.scrollview_content.css("paddingTop"));
			_sb_e.scrollview_content_paddingRight = cssIntVal(_sb_e.scrollview_content.css("paddingRight"));
			_sb_e.scrollview_content_paddingBottom = cssIntVal(_sb_e.scrollview_content.css("paddingBottom"));
			_sb_e.scrollview_content_marginLeft = cssIntVal(_sb_e.scrollview_content.css("marginLeft"));
			_sb_e.scrollview_content_marginTop = cssIntVal(_sb_e.scrollview_content.css("marginTop"));
			_sb_e.scrollview_content_marginRight = cssIntVal(_sb_e.scrollview_content.css("marginRight"));
			_sb_e.scrollview_content_marginBottom = cssIntVal(_sb_e.scrollview_content.css("marginBottom"));
			_sb_e.scrollview_content_borderLeftWidth = cssIntVal(_sb_e.scrollview_content.css("borderLeftWidth"));
			_sb_e.scrollview_content_borderTopWidth = cssIntVal(_sb_e.scrollview_content.css("borderTopWidth"));
			_sb_e.scrollview_content_borderRightWidth = cssIntVal(_sb_e.scrollview_content.css("borderRightWidth"));
			_sb_e.scrollview_content_borderBottomWidth = cssIntVal(_sb_e.scrollview_content.css("borderBottomWidth"));
		_sb_e.sb_pagelistEl = document.getElementById("sb_pagelist");
		_sb_e.sb_pagelist = $("#sb_pagelist");
			_sb_e.sb_pagelist_marginLeft = cssIntVal(_sb_e.sb_pagelist.css("marginLeft"));
			_sb_e.sb_pagelist_marginTop = cssIntVal(_sb_e.sb_pagelist.css("marginTop"));
			_sb_e.sb_pagelist_marginRight = cssIntVal(_sb_e.sb_pagelist.css("marginRight"));
			_sb_e.sb_pagelist_marginBottom = cssIntVal(_sb_e.sb_pagelist.css("marginBottom"));
			_sb_e.sb_pagelist_borderLeftWidth = cssIntVal(_sb_e.sb_pagelist.css("borderLeftWidth"));
			_sb_e.sb_pagelist_borderTopWidth = cssIntVal(_sb_e.sb_pagelist.css("borderTopWidth"));
			_sb_e.sb_pagelist_borderRightWidth = cssIntVal(_sb_e.sb_pagelist.css("borderRightWidth"));
			_sb_e.sb_pagelist_borderBottomWidth = cssIntVal(_sb_e.sb_pagelist.css("borderBottomWidth"));
			_sb_e.sb_pagelist_paddingLeft = cssIntVal(_sb_e.sb_pagelist.css("paddingLeft"));
			_sb_e.sb_pagelist_paddingTop = cssIntVal(_sb_e.sb_pagelist.css("paddingTop"));
			_sb_e.sb_pagelist_paddingRight = cssIntVal(_sb_e.sb_pagelist.css("paddingRight"));
			_sb_e.sb_pagelist_paddingBottom = cssIntVal(_sb_e.sb_pagelist.css("paddingBottom"));
		_sb_e.scrollview_trailer = $('#scrollview-trailer');
		_sb_e.scrollview_bottom = $('#scrollview-bottom');
		_sb_e.sb_page_entry_field = document.getElementById('sb_page_entry_field');
		_sb_e.doctypeinfos = $("#doctypeinfos");
		_sb_e.lightbox = $("#lightbox");
		_sb_e.lightboxEl = document.getElementById("lightbox");
		_sb_e.lightbox_content = $("#lightbox_content");
		_sb_e.lightbox_contentEl = document.getElementById("lightbox_content");
		_sb_e.lightbox_div = $("#lightbox_div");
		_sb_e.lightbox_divEl = document.getElementById("lightbox_div");
		_sb_e.lightbox_close = $("#lightbox_close");
		_sb_e.lightbox_returnsearch = $("#lightbox_returnsearch");
		_sb_e.lightbox_overlay = $("#lightbox_overlay");


		// Element '#debugmessage' is the very last element in body
		// we get it in 'watchBodyContentLoaded', when full body is loaded
		// _sb_e.debugmessage = $('#debugmessage');
	},

	autoDet_pageImageUseSize = function () {
		var pgsizes = [],
			pgWidths = [], pgHeights = [], pageHeightAvail, bookWidthAvailable,
			old_pageImageUseSize, new_pageImageUseSize,
			i;
		//alert("_sb_s.autoDet_pageImageUseSize: " + _sb_s.autoDet_pageImageUseSize + "\n_sb_s.pageImageUseSize: " + _sb_s.pageImageUseSize);
		if ((_sb_s.autoDet_pageImageUseSize < 1) && (_sb_s.pageImageUseSize == 0)) return;

		try { pgsizes = _sb_fn.get_moreDocumentInfo("pageJPEGsizefactor").split(","); } catch(ex) {}
		if (pgsizes.length > 1) {
			_sb_s.pageImageCoordsRatio = parseFloat(pgsizes[_sb_s.pageImageUseSize]) / parseFloat(pgsizes[0]);
		}
		//alert("_sb_s.autoDet_pageImageUseSize: " + _sb_s.autoDet_pageImageUseSize);

		if (_sb_s.autoDet_pageImageUseSize < 1) return;

		// detect the size of page image to load if multiple available
		old_pageImageUseSize = new_pageImageUseSize = _sb_s.pageImageUseSize;
		// get available page image widths
		try { pgWidths = _sb_fn.get_moreDocumentInfo("pageJPEGwidth").split(","); } catch(ex) { return; }
		if (pgWidths.length < 2) return;	// no multiple page images available
		// get available page image heights
		try { pgHeights = _sb_fn.get_moreDocumentInfo("pageJPEGheight").split(","); } catch(ex) { return; }
		if (pgHeights.length < 2) return;	// no multiple page images available
		
		pageHeightAvail = getPageHeightAvailable();
		bookWidthAvailable = getBookWidthAvailable("autoDet_pageImageUseSize");
		if (_sb_s.pageDisplay != "single") {
			bookWidthAvailable /= 2;	// like 1328 pixels for double pages
		}
		// check by available width
		for (i = 0; i < pgWidths.length; i++) {
			if ((parseInt(pgWidths[i],10) >= bookWidthAvailable)
				|| (i >= (pgWidths.length-1))
				) {
				new_pageImageUseSize = i;
				break;
			}
		}
		// check by available height
		for (i = 0; i < pgHeights.length; i++) {
			if ((parseInt(pgHeights[i],10) >= pageHeightAvail)
				|| (i >= (pgHeights.length-1))
				) {
				if (i < new_pageImageUseSize) new_pageImageUseSize = i;
				break;
			}
		}
		//alert("_sb_s.pageDisplay: " + _sb_s.pageDisplay + "\npageHeightAvail: " + pageHeightAvail + "\nbookWidthAvailable: " + bookWidthAvailable + "\n\npgWidths: " + pgWidths + "\npgHeights: " + pgHeights + "\n\nnew_pageImageUseSize: " + new_pageImageUseSize);
		
		if (old_pageImageUseSize != new_pageImageUseSize) {
			_sb_s.pageImageUseSize = new_pageImageUseSize;
			// re-get page settings
			if (pgsizes.length > 1) {
				_sb_s.pageImageCoordsRatio = parseFloat(pgsizes[_sb_s.pageImageUseSize]) / parseFloat(pgsizes[0]);
			}
			_sb_fn.getPageSettings();

		}
	},

	getPageSettings = function () {
		/* we can have multiple page images - take from moreDocumentInfo
		_sb_s.pageWidth = _sb_e.pv_P1.width();
		_sb_s.pageHeight = _sb_e.pv_P1.height();
		alert("_sb_s.pageWidth: " + _sb_s.pageWidth + "\n_sb_s.pageHeight: " + _sb_s.pageHeight);
		*/
		_sb_s.pageWidth = parseInt(_sb_fn.get_moreDocumentInfo("pageJPEGwidth").split(",")[_sb_s.pageImageUseSize],10);
		_sb_s.pageHeight = parseInt(_sb_fn.get_moreDocumentInfo("pageJPEGheight").split(",")[_sb_s.pageImageUseSize],10);
		//alert("_sb_s.pageImageUseSize: " + _sb_s.pageImageUseSize + "\n_sb_s.pageWidth: " + _sb_s.pageWidth + "\n_sb_s.pageHeight: " + _sb_s.pageHeight);
		

		_sb_s.page_borderLeftWidth = cssIntVal(_sb_e.pv_P1.css("borderLeftWidth"));
		_sb_s.page_borderTopWidth = cssIntVal(_sb_e.pv_P1.css("borderTopWidth"));
		_sb_s.page_borderRightWidth = cssIntVal(_sb_e.pv_P1.css("borderRightWidth"));
		_sb_s.page_borderBottomWidth = cssIntVal(_sb_e.pv_P1.css("borderBottomWidth"));
		_sb_s.page_paddingLeft = cssIntVal(_sb_e.pv_P1.css("paddingLeft"));
		_sb_s.page_paddingTop = cssIntVal(_sb_e.pv_P1.css("paddingTop"));
		_sb_s.page_paddingRight = cssIntVal(_sb_e.pv_P1.css("paddingRight"));
		_sb_s.page_paddingBottom = cssIntVal(_sb_e.pv_P1.css("paddingBottom"));
		_sb_s.page_marginLeft = cssIntVal(_sb_e.pv_P1.css("marginLeft"));
		_sb_s.page_marginTop = cssIntVal(_sb_e.pv_P1.css("marginTop"));
		_sb_s.page_marginRight = cssIntVal(_sb_e.pv_P1.css("marginRight"));
		_sb_s.page_marginBottom = cssIntVal(_sb_e.pv_P1.css("marginBottom"));

		// this may change when window is resized
		_sb_s.pageToPageOffsetWidth = _sb_e.sb_pagelist.children(":first").outerWidth()
											+ cssIntVal(_sb_e.sb_pagelist.children(":first").css("marginLeft"))
											+ cssIntVal(_sb_e.sb_pagelist.children(":first").css("marginRight"));

	};
	_sb_fn.loadDeviceCSS = loadDeviceCSS;	// make globally available
	_sb_fn.setMetaViewPort = setMetaViewPort;
	_sb_fn.getSlideBookSettings = getSlideBookSettings;
	_sb_fn.getPageSettings = getPageSettings;



	/* ========================
	 * setup variables whose content might not already be loaded
	 */
	var delayed_presets = function () {

		_sb_s.showImageLightbox.body = _sb_e.bodyEl;
		_sb_s.showImageLightbox.xslcssPath = _sb_s.xslcssPath;

		// load the js and css for the image lightbox
		if (_sb_s.is_IE && (_sb_s.IEVersion <= 7)) _sb_s.showImageLightbox_enable = 0;	// never for IE <= 8
		if (_sb_s.showImageLightbox_enable > 0) {
			loadjscssfile(_sb_s.xslcssPath + "image_lightbox.js","js");
			loadjscssfile(_sb_s.xslcssPath + "image_lightbox.css","css");
		}
	
	
		// ******* call the customizable loader for javascripts and css in custom.js
		try {
			custom_loader();
		} catch(ex){}
	
	},
	cssIntVal = function (str) {
		if ((str == null) || typeof(str) == 'undefined') return(0);
		var nbr = parseInt(str, 10);
		//alert("str: " + str + "\nnbr: " + nbr);
		if (isNaN(nbr)) return(0);
		return(nbr);
	},
	cssFloatVal = function (str) {
		if ((str == null) || typeof(str) == 'undefined') return(0.0);
		var nbr = parseFloat(str);
		//alert("str: " + str + "\nnbr: " + nbr);
		if (isNaN(nbr)) return(0.0);
		return(nbr);
	},
	getPageImage = function (pgidx) {
		var pgimg;
		try { pgimg = epaper_pages[pgidx][0].split(",")[_sb_s.pageImageUseSize]; }
		catch(ex){
			return epaper_pages[pgidx][0];
		}
		//alert("pgidx: " + pgidx + "\nimage: " + pgimg);
		return pgimg;
	},
	getCurrentPageIdx = function() {
		return(_sb_s.currentPageIdx);
	},
	reloadBook = function() {
		var oldhref = "";
		if (_sb_s.noReloadOnOrientationChange) return;
		//alert('reload: ' + window.location.href);
		if (window.localStorage) window.localStorage.setItem('dirlinknReturnPageIdx', ""+_sb_s.currentPageIdx);
		oldhref = window.location.href;
		window.location.href = "";	// some browsers do not load href if same as before
		window.location.href = oldhref;
		return;
	};
	_sb_fn.cssIntVal = cssIntVal;
	_sb_fn.cssFloatVal = cssFloatVal;
	_sb_fn.getPageImage = getPageImage;
	_sb_fn.getCurrentPageIdx = getCurrentPageIdx;
	_sb_fn.reloadBook = reloadBook;




	/* ========================
	 * early initializer
	 */
	$.fn.initFlipbook = function (no_loadDeviceCSS) {
		var _value_css,
			_value_param,
			cbidx,
			haveloaded;

		if (window.localStorage) {	// check if reload: turn splash off immediatel splash
			var gotoPageIDX = window.localStorage.getItem('dirlinknReturnPageIdx');
			if (gotoPageIDX !== null) {
				// set/enable body style
				var prop, el;
				for (el in _sb_s.splash.styles_after) {
					for (prop = 0; prop < _sb_s.splash.styles_after[el].length; prop++) {	// array of css properties,values to set
						//log_methods.log(prop + ": " + _sb_s.splash.styles_after[el][prop]);
						$(el).css(_sb_s.splash.styles_after[el][prop][0], _sb_s.splash.styles_after[el][prop][1]);
					}
				}
			}
		}


		// wait for head available to add css
		_sb_e.headEl = document.getElementsByTagName("head")[0];
			//alert("head: " + _sb_e.headEl + "\nno_loadDeviceCSS: " + no_loadDeviceCSS);
		if (!_sb_e.headEl) {
			setTimeout(function(){$.fn.initFlipbook(no_loadDeviceCSS);},10);
			return;
		}


		// wait for turnjs ready
		if(typeof(_g_turnjs_pageIsFolding) == "undefined") {	// wait for turn.js
			setTimeout(function(){$.fn.initFlipbook(no_loadDeviceCSS);},50);
			return;
		}

		// if #status_message available, this is below all areas and canvas but BEFORE all articles
		_sb_e.status_message = $("#status_message");
		if (_sb_e.status_message.length <= 0) {
			setTimeout(function(){$.fn.initFlipbook(no_loadDeviceCSS);},50);
			return;
		}

		/* ====== get document infos ====== */
		init_DocumentInfos();

		// detect if running in app
		_value_param = get_site_param("inapp");
		if (_value_param != null) {
			_sb_s.inApp = true;
			_sb_s.addInAppMargin = true;
		}
		_value_param = get_site_param("iabapp");
		if (_value_param != null) {
			_sb_s.inApp = true;
			_sb_s.addInAppMargin = false;
		}
		//alert("inApp: " + _sb_s.inApp);


		// check to load additional css
		_value_param = get_site_param("css");	// load css
		if (_value_param != null) {
			_sb_s.xslcssPath = _sb_fn.get_moreDocumentInfo("XSLCSSPath");
			//alert(_sb_s.xslcssPath + _value_param);
			_sb_fn.loadjscssfile((_value_param.indexOf("/")<0 ? _sb_s.xslcssPath : "") + _value_param,"css");
			setTimeout(function(){$.fn.initFlipbook2(false);},500);
			return;
		}
		
		// call final initializer
		$.fn.initFlipbook2(false);
	}

	$.fn.initFlipbook_noDevcss = function () {
		//DO NOT MAKE ALERT IN CALLBACK OR FIREFOX WILL HANG! alert("recall initFlipbook_noDevcss");
		setTimeout(function(){$.fn.initFlipbook2(true);},1000);
	};
	$.fn.initFlipbook2 = function (no_loadDeviceCSS) {
		var _value_css,
			_value_param,
			cbidx,
			haveloaded;


		// show IE warning message
		if (_sb_s.is_IE && (_sb_s.IEVersion <= _sb_s.showOldIEVersionwarning)) alert(ftst[11][_sb_s.cur_lang_ID]);	// warning to use newer IE


		/* ========================
		 * get settings from CSS or from site call parameters
		 */
		_value_param = get_site_param("pus");
		if (_value_param != null) {
			try {
				_sb_s.pageImageUseSize = parseInt(_value_param, 10);
				_sb_s.autoDet_pageImageUseSize = 0;
			} catch(ex) {}
		}
		else {
			_value_param = _sb_fn.get_moreDocumentInfo("pageImageUseSize");
			//alert("_value_param: " + _value_param);
			if (_value_param != "") {
				try {
					_sb_s.pageImageUseSize = parseInt(_value_param, 10);
					_sb_s.autoDet_pageImageUseSize = 0;
				} catch(ex) {}
			}
		}

		_value_css = get_css_value("draggableObjects","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) _sb_s.enableDraggableObjects = parseInt(_value_css, 10);
		_value_param = get_site_param("do");
		if (_value_param != null) try { _sb_s.enableDraggableObjects = parseInt(_value_param, 10); } catch(ex) {}
		if (_sb_s.is_IE && (_sb_s.IEVersion <= 7)) _sb_s.enableDraggableObjects = 0;	// never for IE <= 8

		_value_css = get_css_value("lightbox_position","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) _sb_s.lightboxPosition = parseInt(_value_css, 10);
		_value_param = get_site_param("lbp");
		if (_value_param != null) try { _sb_s.lightboxPosition = parseInt(_value_param, 10); } catch(ex) {}

		_value_css = get_css_value("paginate_pages","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) _sb_s.doPaginate = parseInt(_value_css, 10);
		_value_param = get_site_param("paginate");
		if (_value_param != null) try { _sb_s.doPaginate = parseInt(_value_param, 10); } catch(ex) {}

		_value_css = get_css_value("viewMaxScale","width");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) _sb_s.viewMaxScale = parseFloat(_value_css);
		_value_param = get_site_param("sm");
		if (_value_param != null) try { _sb_s.viewMaxScale = parseFloat(_value_param); } catch(ex) {}
		_value_param = get_site_param("sc");
		if (_value_param != null) {
			try {
				_sb_s.viewManualScale = parseFloat(_value_param);
				if (_sb_s.viewMaxScale == 0.0) _sb_s.viewMaxScale = 1.25;	// enable scaling if not already enabled
			} catch(ex) {}
		}
		_value_css = get_css_value("show_page_thumbs","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) _sb_s.show_page_thumbs = parseInt(_value_css, 10);
		_value_param = get_site_param("thumbs");
		if (_value_param != null) try { _sb_s.show_page_thumbs = parseInt(_value_param, 10); } catch(ex) {}

		_value_css = get_css_value("show_TOC_index","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) _sb_s.show_TOC_index = parseInt(_value_css, 10);
		_value_param = get_site_param("toc");
		if (_value_param != null) try { _sb_s.show_TOC_index = parseInt(_value_param, 10); } catch(ex) {}

		_value_css = get_css_value("enable_print_article","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) _sb_s.enable_print_lightbox = parseInt(_value_css, 10);
		_value_param = get_site_param("print");
		if (_value_param != null) try { _sb_s.enable_print_lightbox = parseInt(_value_param, 10); } catch(ex) {}
		if (_sb_s.enable_print_lightbox == 2) _sb_s.print_entireArticle = 1;

		_value_css = get_css_value("enable_fontsize_change","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) _sb_s.enable_lightBoxFontSize = parseInt(_value_css, 10);
		_value_param = get_site_param("fschg");
		if (_value_param != null) try { _sb_s.enable_lightBoxFontSize = parseInt(_value_param, 10); } catch(ex) {}

		_value_param = get_site_param("nohead");	// turn off entire flipbook header
		if (_value_param != null) {
			set_css_value('#scrollview-header','display','none');
			//alert("head off");
		}

		_value_param = get_site_param("notrail");	// turn off entire flipbook trailer
		if (_value_param != null) {
			set_css_value('#scrollview-trailer','height','0');
			set_css_value('#scrollview-trailer','display','none');
			//alert("trailer off");
		}

		_value_param = get_site_param("notitle");	// turn off title
		if (_value_param != null) {
			set_css_value('#scrollview-header-titlecontainer','display','none');
			//alert("title off");
		}
		else {	// turn off if title is empty
			var sht = document.getElementById("scrollview-header-titlecontainer"),
				shttext = sht.innerText || sht.textContent;	// IE8  returns 'undefined' if innerText is white space only
			//alert("1 innerText: '"+sht.innerText+"'\n\ntextContent: '"+sht.textContent+"'\n\ntypeof: "+typeof(shttext));
			try {
				if (typeof(shttext) == 'undefined') {
					set_css_value('#scrollview-header-titlecontainer','display','none');
				}
				else {
					if ((typeof(shttext) != 'undefined') && (shttext.length <= 3)) {
						shttext = shttext.replace(/[\r|\n| ]/g,"");
						if (shttext.length == 0) set_css_value('#scrollview-header-titlecontainer','display','none');
					}
				}
			} catch(ex){}
		}

		_value_param = get_site_param("nologo");	// turn off title
		if (_value_param != null) {
			set_css_value('#scrollview-header-logocontainer','display','none');
			//alert("logo off");
		}

		_value_param = get_site_param("nopagenav");	// turn off page navigation buttons
		if (_value_param != null) {
			_sb_s.show_page_navigation = 0;
			//alert("page nav off");
		}

		_value_param = get_site_param("nodocpdf");	// disable document PDF button
		if (_value_param != null) {
			_sb_s.documentPDFbutton_enable = 0;
			//alert("doc pdf off");
		}

		_value_param = get_site_param("nopagepdf");	// disable page PDF buttons
		if (_value_param != null) {
			_sb_s.pagePDFbutton_enable = 0;
			//alert("page pdf off");
		}

		_value_param = get_site_param("pageloadtype");	// how to load pages
		if (_value_param != null) {
			_sb_s.pageImageLoadType = parseInt(_value_param, 10);
		}


		_value_param = get_site_param("debug");
		if (_value_param != null) _sb_s.DEBUGmode = parseInt(_value_param, 10);

		//-------------------
		// override default page turn mode. can be 'turn' or 'slide'
		_value_param = _sb_fn.get_moreDocumentInfo("pageTurnMode");
		if (_value_param != "") if ((_value_param == 'turn') || (_value_param == 'slide')) _sb_s.pageTurnMode = _value_param;

		_value_css = get_css_value("page_turn_mode","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) {
			if (_value_css == '0') _sb_s.pageTurnMode = 'turn';
			else _sb_s.pageTurnMode = 'slide';
		}
		_value_param = get_site_param("ptm");	//?ptm=slide or ?ptm=turn
		if (_value_param != null) try { if ((_value_param == 'turn') || (_value_param == 'slide')) _sb_s.pageTurnMode = _value_param; } catch(ex) {}
		else {
			_value_param = get_site_param("slide");	//?slide
			if (_value_param != null) _sb_s.pageTurnMode = 'slide';
			else {
				_value_param = get_site_param("turn");	//?turn
				if (_value_param != null) _sb_s.pageTurnMode = 'turn';
			}
		}

		//-------------------
		// override default page display mode. can be 'double' or 'single'
		_value_param = _sb_fn.get_moreDocumentInfo("pageDisplayMode");
		if (_value_param != "") if ((_value_param == 'double') || (_value_param == 'single')) {
			_sb_s.pageDisplay = _value_param;
			_sb_s.pageDisplayForced = true;
		}

		_value_css = get_css_value("page_display_mode","zIndex");	// try to get from css
		if ((typeof _value_css != "undefined") && (_value_css != "")) {
			if (_value_css == '0') _sb_s.pageDisplay = 'double';
			else _sb_s.pageDisplay = 'single';
			_sb_s.pageDisplayForced = true;
		}
		_value_param = get_site_param("pdm");	//?pdm=double or ?pdm=single
		if (_value_param != null) try { if ((_value_param == 'turn') || (_value_param == 'slide')) _sb_s.pageDisplay = _value_param; } catch(ex) {}
		else {
			_value_param = get_site_param("double");	//?double
			if (_value_param != null) {
				_sb_s.pageDisplay = 'double';
				_sb_s.pageDisplayForced = true;
			}
			else {
				_value_param = get_site_param("single");	//?single
				if (_value_param != null) {
					_sb_s.pageDisplay = 'single';
					_sb_s.pageDisplayForced = true;
				}
			}
		}

		// simulate to be a phone and mobile os
		_value_param = get_site_param("phone");	//?phone
		if (_value_param != null) { _sb_s.is_Phone = _sb_s.is_MobileOS = true; }
		// force to load device size dependent CSS like 'flipbook_w400.css'
		_value_param = get_site_param("devcss");	//?devcss
		if (_value_param != null) { _sb_s.load_device_css = true; }
		//alert("load_device_css: " + _sb_s.load_device_css + "\nis_Phone: " +  _sb_s.is_Phone);


		// override / inhibit native transforms
		_value_param = get_site_param("nnt");	//?nnt  (no native transforms)
		if (_value_param != null) {
			_sb_s.nativeTransform = "";			// force we can not 'transform'
			_sb_s.allowNativeTransitions = false;
		}

		// get all settings like body and container border/padding/margin...
		_sb_fn.getSlideBookSettings();


		// set top margin for inApp status bar if running as app in Cordova WebView
		if ((_sb_s.inApp == true) && (_sb_s.addInAppMargin == true)) {
			if (_sb_s.inAppMarginTop != "") _sb_e.sb_container.css("margin-top",_sb_s.inAppMarginTop);
		}


		/* ====== init canvas ====== */
		if (!_sb_s.isTouchDevice && _sb_s.doArticleShade) $.init_canvas();


		/* ====== get total body margins and padding ====== */
		getBodyLRMarginsTotal();
		getBodyTBMarginsTotal();


		// allow us to override settings
		// our functions to perform such tasks is must be registered
		for (cbidx = 0; cbidx < sb_settings_Override_CBs.length; cbidx++) {
			try {
				sb_settings_Override_CBs[cbidx]();
			} catch(ex){}
		}
		// reset local vars when re-defined sb_settings_Override()
		switch (_sb_s.override_isTouchDevice) {
			case 0:	// force NO isTouchDevice
				_sb_s.isTouchDevice = false;
				break;
			case 1:	// force IS isTouchDevice
				_sb_s.isTouchDevice = true;
				break;
		}


		// before trying to load pages we have to know the path to the XSLCSS folder
		_sb_s.xslcssPath = _sb_fn.get_moreDocumentInfo("XSLCSSPath");
		if (_sb_s.xslcssPath != "") _sb_s.xslcssPath += _sb_s.xslcssSubpath;
		else _sb_s.xslcssPath = null;
		_sb_s.xslcssThemeSubpath = _sb_fn.get_moreDocumentInfo("themeSubPath");


		// load device css and set meta viewport
		//log_methods.log("no_loadDeviceCSS: " + no_loadDeviceCSS);
		//alert(typeof(window.matchMedia) + "\n" + typeof(window.msMatchMedia));

		if (!no_loadDeviceCSS) {
			haveloaded = false;
			// loading more css must be done after knowing the path to XSLCSS folder
			//alert("is_MobileOS: " + _sb_s.is_MobileOS + "\nviewPortStyles_set: " + _sb_s.viewPortStyles_set + "\nis_Phone: " + _sb_s.is_Phone + "\nis_IE: " + _sb_s.is_IE + "\nIEVersion: " + _sb_s.IEVersion);
			if (_sb_s.load_device_css || (!_sb_s.viewPortStyles_set && _sb_s.is_MobileOS)) {
				haveloaded = _sb_fn.setMetaViewPort();
				//log_methods.log("setMetaViewPort haveloaded: " + haveloaded);
				//alert("setMetaViewPort haveloaded: " + haveloaded);

				if (haveloaded) {
					//log_methods.log("---- restart initFlipbook() after setMetaViewPort");
					setTimeout(function(){$.fn.initFlipbook2(no_loadDeviceCSS);},1500);	// if on Intranet:
																								// IE 10 Windows Phone needs at least 100 ms to activate the newly set viewport style
																								// if on INTERNET:
																								// IE 10 Windows Phone needs at least 1500 ms to activate the newly set viewport style
																								//	because other stuff is currently loading too
					return;
				}
			}

			if (_sb_s.load_device_css || _sb_s.is_MobileOS) {
				//log_methods.log("want load_device_css: " + _sb_s.load_device_css + "\nis_MobileOS: " + _sb_s.is_MobileOS + "\nclient w x h: " + _sb_s.clientWidth + " x " + _sb_s.clientHeight);

				haveloaded = _sb_fn.loadDeviceCSS(null, null, null, $.fn.initFlipbook_noDevcss);
				if (haveloaded) return;	// $.fn.initFlipbook2 will be called when css is loaded: the loadjscssfile callback function '$.fn.initFlipbook_noDevcss'
				//_sb_fn.loadDeviceCSS(null, _sb_settings.xslcssPath + "flipbook_l_400.css");
				// DEBUG only: load twice: setTimeout(function(){_sb_fn.loadDeviceCSS(null, _sb_settings.xslcssPath + "flipbook_w400.css");},3000);
			}
		}

		//alert("platform: " + _sb_s.platform + "\nappName: " + navigator.appName + "\nappVersion: " + navigator.appVersion + "\nlanguage: " + navigator.language + "\nuserLanguage: " + navigator.userLanguage + "\nuserAgent: " + navigator.userAgent + "\nonorientationchange: " + ("onorientationchange" in window) + "\norientation avail: " + ("orientation" in window) + "\norientation: " + (window.orientation) + "\nonorientation: " + ("onorientation" in window) + "\n_sb_s.orientation: " + _sb_s.orientation + "\n\nisTouchDevice: " + _sb_s.isTouchDevice + "\nis_MobileOS: " + _sb_s.is_MobileOS + "\nis_Phone: " + _sb_s.is_Phone + "\npointerEvents: " + JSON.stringify(_sb_s.pointerEvents));

		// DEBUG to show order loaded CSS. post-loaded css must be at end
		//setTimeout(function(){alert(_sb_fn.listCssRules());},16000);

		/* ---- device css and neta are now loaded: GO ON --- **/

		// wait for first page image container loaded
		_sb_e.pv_P1 = $("#pv_P1");
		if (_sb_e.pv_P1.length <= 0) {
			setTimeout(function(){$.fn.initFlipbook2(true);},50);
			return;
		}
		if (_sb_e.pv_P1.width() <= 10) {
			setTimeout(function(){$.fn.initFlipbook2(true);},50);
			return;
		}

		update_screenDimensions(true,"initFlipbook2 2");		// LEAVE this here!!!!! we need window dimensions soon

		// the first page is loaded
		/* ====== get all page settings ====== */
		_sb_fn.getPageSettings();

		// get all page container DIV elements containing the page image
		var i,
			pageDivs = _sb_e.sb_pagelist.children();
		//alert("num pageDivs: " + pageDivs.eq(1).prop('class'));
		for (i = 0; i < num_epaper_pages; i++) {
			epaper_pages[i][5] = pageDivs.eq(i);
		}

		switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
			default:
			case "turn":
				update_screenDimensions(true,"initFlipbook2 2");		// LEAVE this here!!!!! we need window dimensions soon
				break;
			case "slide":
				update_screenDimensions(false,"initFlipbook2 2");		// LEAVE this here!!!!! we need window dimensions soon
				break;
		}


		// now make sure that page also fits for small devices
		// on most modern mobiles, the meta viewport tag must be set in the HTML document to represent a smaller device
		get_clientSize("initFlipbook2 dir2");
		//alert("pageDisplayForced: " +_sb_s.pageDisplayForced + "\n_sb_s.pageDisplay: "+ _sb_s.pageDisplay + "\norientation: "+ _sb_s.orientation + "\n_sb_s.clientWidth: " +  _sb_s.clientWidth + "\n_sb_s.clientHeight: " +  _sb_s.clientHeight + "\n_sb_s.pageWidth: " +  _sb_s.pageWidth + "\n_sb_s.pageDisplayMaxDoublePX: " +  _sb_s.pageDisplayMaxDoublePX + "\n_sb_s.is_Phone: " +  _sb_s.is_Phone);
		if (_sb_s.pageDisplayForced != true) {
			if (_sb_s.orientation == "portrait") {
				//alert("single");
				_sb_s.pageDisplay = 'single';
			}
		}
		//alert("_sb_s.pageDisplay: " + _sb_s.pageDisplay);
		//alert("_sb_s.clientWidth: " + _sb_s.clientWidth + "\n_sb_s.clientHeight: " + _sb_s.clientHeight);

		// we want to handle window resize or orientation change
		if ("onorientationchange" in window) {
			_sb_fn.addEventHandler(window,"orientationchange",
						function() {
							handleOrientationChange(false, "windowonresize",false);
							//_sb_s.DEBUGmode = 2; log_methods.log("onorientationchange\norientation: " + _sb_s.orientation + "\ndeviceOrientation: " + _sb_s.deviceOrientation);
						}
						);
		}
		else if ("DeviceOrientationEvent" in window) {
			_sb_fn.addEventHandler(window,"DeviceOrientationEvent",
						function() {
							handleOrientationChange(false, "DeviceOrientationEvent",false);
							//_sb_s.DEBUGmode = 2; log_methods.log("onorientationchange\norientation: " + _sb_s.orientation + "\ndeviceOrientation: " + _sb_s.deviceOrientation);
						}
						);
		}
		_sb_fn.addEventHandler(window,"resize",
					function() {
						handleOrientationChange(false, "windowonresize",false);
						//_sb_s.DEBUGmode = 2; log_methods.log("window resize\norientation: " + _sb_s.orientation + "\ndeviceOrientation: " + _sb_s.deviceOrientation);
					}
					);


		// enable logging of javascript errors
		if (_sb_s.DEBUGmode > 0) {
			window.onerror = function(msg, url, line) {
									if ((_sb_s.DEBUGmode & 2) > 0) log_methods.log("SCRIPT ERROR", msg, url, line, true);
									if ((_sb_s.DEBUGmode & 4) > 0) alert("SCRIPT ERROR:\nmessage: " + msg + "\nurl: " + url + "\nline: " + line);
									// return true : suppress error, browser will NOT report it
									// return false : normal error, browser will report it
									return false;
									};
		}



		//log_methods.log("*** go on ticks: " + _sb_fn.elapsedTicks_sinceStart() + "\nclient w x h: " + _sb_s.clientWidth + " x " + _sb_s.clientHeight + "\npage w x h: " + _sb_s.pageWidth + " x " + _sb_s.pageHeight + "\n_orientation: " + _sb_s.orientation);


		// start watcher to wait for entire body loaded
		// waits for the element '#debugmessage'
		$.watchBodyContentLoaded();


		// ====== load pages
		$.show_pageimages_load_status();
		/*
		alert("userAgent: " + _sb_s.userAgent
				+ "\n\nis_IE: " + _sb_s.is_IE 
				+ "\nis_FirefoxOS: " + _sb_s.is_FirefoxOS 
				+ "\nis_Android: " + _sb_s.is_Android + " V " + _sb_s.AndroidVersion
				+ "\npageTurnMode: " + _sb_s.pageTurnMode);
		*/
		// force 'slide' for IE <= 8 and android old versions
		if ((_sb_s.is_IE && (_sb_s.IEVersion <= 8))
			|| (_sb_s.is_Android && (_sb_s.AndroidVersion < _sb_s.AndroidVersionMin))
			) {
			_sb_s.pageTurnMode = 'slide';
		}

		/* ====== determine the size of page images to load (if multiple available) ====== */
		autoDet_pageImageUseSize();


		// make sure, the first x pages are loaded
		//alert("_sb_s.pageImageLoadType: " + _sb_s.pageImageLoadType);
		if (_sb_s.pageImageLoadType > 0) {
			var holdMS = 1;
			if (_sb_s.pageImageLoadType > num_epaper_pages) _sb_s.pageImageLoadType = num_epaper_pages;
			for (var pl = 0; (pl < _sb_s.pageImageLoadType) && (pl < num_epaper_pages); pl++) {
				//log_methods.log("preloading page idx: " + pl + "\n\tsrc: " + _sb_fn.getPageImage(pl));
				setTimeout("$.load_page(\"pv_P"+(pl+1) + "\"," + pl + ",\"" + _sb_fn.getPageImage(pl) + "\",true)", holdMS);
				holdMS += 30;	// make sure the very first page has more time to load
				if (holdMS > 200) holdMS = 200;
			}
			if (_sb_s.slidebook_load_state.indexOf("3") < 0) _sb_s.slidebook_load_state += "3";
		}
		else {
			switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
				default:
				case "turn":
					// immediately get page image 1 (index 0)
					$.load_page("pv_P1", 0, _sb_fn.getPageImage(0),false);
					// preload rest of page images after site is rendered in $.startFlipbook
					break;
				case 'slide':
					// immediately get page image 1 (index 0)
					$.load_page("pv_P1", 0, _sb_fn.getPageImage(0),false);
					break;
			}
		}




		//alert("_sb_s.pageTurnMode: " + _sb_s.pageTurnMode);
		if (_sb_s.pageTurnMode == 'turn') {		// can be 'turn' or 'slide'
			// register a callback for end of page turn 
			_sb_e.sb_pagelist.register_cb_turn_end(cb_turn_end);
			// register a callback before a page is removed from DOM
			_sb_e.sb_pagelist.register_cb_before_removepage(cb_before_removepage);
			// register a callback after a page is added to DOM
			_sb_e.sb_pagelist.register_cb_after_addpage(cb_after_addpage);
		}

		// render the flip/slide book
		// but first, calculate the proper page dimensions
		// we absolutely HAVE to calculate book dimension!!!
		var maxbookdim = null;
		maxbookdim = adjustPageImageSize('initFlipbook', true, (_sb_s.pageAdjustToWindow > 0 ? true : false),false);	// return an array with book and page dimensions: and also set the body width
																	// maxbookdim[0] = new book width (double page or single)
																	// maxbookdim[1] = new book height
																	// maxbookdim[2] = page div width (container of page image)
																	// maxbookdim[3] = page div height (container of page image)
																	// maxbookdim[4] = page img width 
																	// maxbookdim[5] = page img height
		//_sb_s.DEBUGmode = 2;log_methods.log("maxbookdim W, H: " + maxbookdim[0] + ", " + maxbookdim[1]);
		adjustPageImageSize('initFlipbook', false, false, true);	// recalc areas only


		//log_methods.log("***** RENDERING flip book as: " + _sb_s.pageTurnMode);
		switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
			default:
			case "turn":
				var turnOpts = {
									display: _sb_s.pageDisplay,
									acceleration: true,
									/*gradients: !$.isTouch,*/
									elevation:50,
									duration:_sb_s.pageTransitionDuration,
									when: {
										turned: function(e, page) {
											var cbidx;
											//_sb_s.DEBUGmode = 2; log_methods.log("turned cb: " + _g_turnjs_pageIsFolding +"\ncurrentPage idx: " + _sb_s.currentPageIdx +"\nlastPageIndex: " + (_sb_s.totalPages - 1));
											_g_turnjs_pageIsFolding = _sb_s.pageIsScrolling = false;	// page turn complete
											//log_methods.log("turn end page: " + page + "\n\tCurrent page view: " + $(this).turn('view') + "\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding);
											// call registered CBs after goto page success
											for (cbidx = 0; cbidx < after_gotoPage_custom_CBs.length; cbidx++) {
												try { after_gotoPage_custom_CBs[cbidx](0,_sb_s.currentPageIdx,_sb_s.currentPageIdx+1,(_sb_s.totalPages - 1), 'OK'); } catch(ex){}
											}
										}
									}
								};

				if (maxbookdim != null) {
					turnOpts["width"] = maxbookdim[0];
					turnOpts["height"] = maxbookdim[1];
					if (_sb_s.is_IE && (_sb_s.IEVersion < 9)) {
						turnOpts["duration"] = 0;
						turnOpts["gradients"] = false;
					}
				}
					/* debug only
					for (var prop in turnOpts) {
						log_methods.log(prop + ":"+turnOpts[prop]);
					}
					*/

				//alert("turnOpts: " + JSON.stringify(turnOpts));
				_sb_e.sb_pagelist.turn(turnOpts);	// NOW render it!

				if (maxbookdim != null) {	// we have preset the book dims, we have to resize the page images too
					_sb_e.sb_pagelist.pagesize(maxbookdim[2], maxbookdim[3], maxbookdim[4], maxbookdim[5]);
				}
				// turn on visibility of book
				if (maxbookdim == null) _sb_e.sb_pagelist.css('height','auto');
				// visibility of sb_pagelist will be turned on by afterSplashEnable() 
				//_sb_e.sb_pagelist.css('visibility','visible');
				//_sb_e.sb_pagelist.css('overflow','visible');

				update_screenDimensions(true,"initFlipbook2 3");

				_sb_s.currentPageIdx = _sb_e.sb_pagelist.turn("page")-1;
				_sb_s.totalPages = _sb_e.sb_pagelist.turn("pages");
				if (_sb_s.is_IE && (_sb_s.IEVersion < 9)) {
					_sb_e.sb_pagelist.disablecorners(true);
				}
				break;
			case "slide":
				//log_methods.log("***** RENDERING slide book");
				//change book body class book
				_sb_e.body.removeClass('sb_body').addClass('sb_body_slide');
				_sb_e.scrollview_container.attr('id','scrollview-container_slide');
				_sb_e.sb_pagelist.find('div').removeClass("sb_pagelistli").addClass('sb_pagelistli_slide');
				if (_sb_s.is_IE && (_sb_s.IEVersion <= 7)) {
					_sb_e.body.css('overflow','hidden');	// hack to cut pages on body border
				}
				// turn on visibility of book
				_sb_e.sb_pagelist.css('height','auto');
				_sb_e.sb_pagelist.css('visibility','visible');
				_sb_e.sb_pagelist.css('overflow','visible');

				// this may change when window is resized
				_sb_s.pageToPageOffsetWidth = _sb_e.sb_pagelist.children(":first").outerWidth()
													+ cssIntVal(_sb_e.sb_pagelist.children(":first").css("marginLeft"))
													+ cssIntVal(_sb_e.sb_pagelist.children(":first").css("marginRight"));

				update_screenDimensions(true,"initFlipbook2 4");

				_sb_s.currentPageIdx = 0;
				_sb_s.totalPages = num_epaper_pages;

				// the first page image is loaded
				// now, get the next 4 page images
				var pl = 1;
				for (pl = 1; (pl <= 4) && (pl < num_epaper_pages); pl++) {
					setTimeout("$.load_page('pv_P"+(pl+1)+"',"+pl+",'"+_sb_fn.getPageImage(pl)+"',true)", 20);
				}
				// now load all other page images
				if (pl < num_epaper_pages) {
					setTimeout("$.preload_pageimages(5)",100);	// page image 0-4 is already loaded
				}

				break;
		}



		// show body after splash
		$.fn.afterSplashEnable();
	};

	$.fn.afterSplashEnable = function () {
		var is_reload = false;
		if (window.localStorage) {
			var gotoPageIDX = window.localStorage.getItem('dirlinknReturnPageIdx');
			if (gotoPageIDX !== null) is_reload = true;
		}
		// show a splash screen?
			//log_methods.log("splash: " + _sb_s.splash.timeout + "\nelapsed: " +  _sb_fn.elapsedTicks_sinceStart());
		if (!is_reload && (_sb_s.splash.timeout > 0)) {	// wait for splash done
			if (_sb_s.splash.timeout > _sb_fn.elapsedTicks_sinceStart()) {
				//log_methods.log("splash wait");
				setTimeout(function(){$.fn.afterSplashEnable();}, (_sb_s.splash.timeout - _sb_fn.elapsedTicks_sinceStart()));
				return;
			}
		}

		// set/enable body style
		var prop, el;
		for (el in _sb_s.splash.styles_after) {
			for (prop = 0; prop < _sb_s.splash.styles_after[el].length; prop++) {	// array of css properties,values to set
				//log_methods.log(prop + ": " + _sb_s.splash.styles_after[el][prop]);
				$(el).css(_sb_s.splash.styles_after[el][prop][0], _sb_s.splash.styles_after[el][prop][1]);
			}
		}

		// turn on visibility of sb_pagelist
		_sb_e.sb_pagelist.css('visibility','visible');
		_sb_e.sb_pagelist.css('overflow','visible');

		//next step loading flipbook
		$.fn.loadFlipbook();
	};



	/* ========================
	 * load the book
	 */
	$.fn.loadFlipbook = function () {
		var patw,
			cbidx;

		//log_methods.log("rendering as: " +  _sb_s.pageTurnMode);
		//alert("is_Android: " + _sb_s.is_Android + "\nAndroidVersion: " + _sb_s.AndroidVersion + "\nAndroidVersionMin: " + _sb_s.AndroidVersionMin)

		// get web site parameters automatic page adjust to window
		patw = get_site_param("patw");
		if (patw != null) if (!isNaN(patw)) _sb_s.pageAdjustToWindow = parseInt(patw, 10);


		// setup more stuff and variables which require all stuff loaded
		delayed_presets();


		// allow us to do some custom work before continuing
		// our functions to perform such tasks must be registered
		for (cbidx = 0; cbidx < early_config_custom_CBs.length; cbidx++) {
			early_config_custom_CBs[cbidx]();
		}


		$.waitFlipbookModulesLoaded(0);	// wait until flipbook modules are loaded

	};
	$.waitFlipbookModulesLoaded = function (cnt) {
		for (var module in _sb_s.modules) {
			var mod = JSON.stringify(module);
			if ( (mod.indexOf("issuenav_") > 0) || (mod.indexOf("moreinfos_") > 0) ) continue; // skip standard modules
			//log_methods.log("**** module: " + mod + " -> " + _sb_s.modules[module]);
			if ( (_sb_s.modules[module] <= 0) || (_sb_s.modules[module] > 1) ) continue;	// this module will not be loaded or is already marked as loaded 
																										// a module marks itself as loded by setting _sb_s.modules.MODULENAME_enable=2
			
			if (cnt < 100) {	// but don't wait longer than 10 sec
				//log_methods.log("++++ waiting for module loaded:\n\t" + module);
				setTimeout(function(){$.waitFlipbookModulesLoaded(++cnt);},100);
				return;
			}

		}
		$.waitFlipbookRendered(0);	// wait until flipbook is rendered
	}
	$.waitFlipbookRendered = function (cnt) {

		switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
			default:
			case "turn":
				if ((cnt < 100) && (_g_turnjs_pageIsFolding === true)) {	// but don't wait longer than 1 sec
					//_sb_s.DEBUGmode = 2; log_methods.log("waitFlipbookRendered");
					setTimeout(function(){$.waitFlipbookRendered(++cnt);},10);
					return;
				}
				break;
		}

		// start up the rest of flipbook
		$.startFlipbook();
	}


	/* ========================
	 * prevent or enable default behavior
	 */
	$.enab_prev_default = function (prevent, which) {
		if (prevent) {
			if ((which & 1) > 0) _sb_e.html.on(_sb_s.pointerEvents.start, function(e) { e.preventDefault(); });
			if ((which & 2) > 0) _sb_e.sb_container.on(_sb_s.pointerEvents.start, function(e) { e.preventDefault(); });
		}
		else {
			if ((which & 1) > 0) _sb_e.html.off(_sb_s.pointerEvents.start);
			if ((which & 2) > 0) _sb_e.sb_container.off(_sb_s.pointerEvents.start);
		}
	}

	/* ========================
	 * start up the flipbook
	 */
	$.startFlipbook = function () {
		//log_methods.log("startFlipbook");

		// enable page adjust to window
		if (_sb_s.pageAdjustToWindow > 0) _sb_fn.enableAdjustPageImageSize("startFlipbook",_sb_s.pageAdjustToWindow);


		// Prevent default select behavior
		$.enab_prev_default(true,3);	// prevent default marking on html and sb_container


		/* ========================
		 * document TOC button
		 */
		var the_documentTOC = _sb_fn.get_moreDocumentInfo("TOCaid");
		if ((the_documentTOC != "") && (_sb_s.show_TOC_index > 0)) {	// make TOC button visible
			var documentTOC_buttonCont = $('#TOC_button_container');
			if (documentTOC_buttonCont) {
				var the_docTOCpage = _sb_fn.get_moreDocumentInfo("TOCpageSequence"),
					the_docTOCpageName = _sb_fn.get_moreDocumentInfo("TOCpageName"),
					the_docTOCaid = _sb_fn.get_moreDocumentInfo("TOCaid");
				if ((the_docTOCpage != '') || (the_docTOCaid != '')) {
					documentTOC_buttonCont.css('display', 'block');
					documentTOC_buttonCont.on(_sb_s._elementTouch, function(e) {
							//log_methods.log("TOC on: " + _sb_s._elementTouch);
							e.preventDefault();
							e.stopPropagation();
							show_article_xml(null,the_docTOCaid,'','','','','',the_docTOCpage);
							_sb_s.TOC_isShown = true;	// true when the TOC is shown in the article window
							goto_page(the_docTOCpageName,false,null,null,null,e,"TocButton");
							return false;
							});
				}
			}
		}



		/***************
		 * document PDF button
		 */
		if (_sb_s.documentPDFbutton_enable > 0) {
			var the_documentPDF = _sb_fn.get_moreDocumentInfo("documentPDFname");
			if (the_documentPDF != "") {	// make the button visible
		
				var documentPDF_buttonCont = $('#documentPDF_button_container');
				if (documentPDF_buttonCont) {
					// clear old current image element
					documentPDF_buttonCont.html("");
					// move button from parent #scrollview-bottom to end of parent #sb_body
					_sb_e.body.append($(documentPDF_buttonCont));

					var showDocumentPDFHandler = function(e) {
						var winname = "PDF",
							winparams = "resizable=Yes,scrollbars=Yes";
						if (_sb_s.inApp == true) {
							winname = "_blank";
							winparams = "location=yes,toolbar=yes,enableViewportScale=yes";
							if (window.localStorage) window.localStorage.setItem('dirlinknReturnPageIdx', ""+_sb_s.currentPageIdx);
							if (_sb_s.is_Android) {
								// call the PDF viewer from parent window App
								//alert("calling viewpdf: "+the_documentPDF);
								fb_poll_action_set("viewpdf",the_documentPDF);
								return;
							}
						}
						//log_methods.log("showDocumentPDFHandler the_documentPDF: " + the_documentPDF);
						// open the document PDF in a new Window
						window.open(the_documentPDF,winname,winparams);
					}
				
					documentPDF_buttonCont.attr('title', lg[29][_sb_s.cur_lang_ID]);
					documentPDF_buttonCont.attr('alt', lg[29][_sb_s.cur_lang_ID]);
					documentPDF_buttonCont.css('display', 'block');

					/* make document PDF button draggable */
					if (_sb_s.enableDraggableObjects > 0) {
						new XdraggableObject(document.getElementById("documentPDF_button_container"),null,null,showDocumentPDFHandler);
					}
					else documentPDF_buttonCont.on(_sb_s._elementTouch, showDocumentPDFHandler);

				}
			}
		}



		/* ========================
		 * page PDF button
		 */
		if (_sb_s.pagePDFbutton_enable > 0) {
			var the_pagePDF = $("#pv_P1").attr('data-page-pdf');
				//log_methods.log("adding page PDFbutton(s)\n\tPDF: " + the_pagePDF + "\n\tturnmode: " + _sb_s.pageTurnMode);
			if (the_pagePDF != undefined) {	// make the button visible
				// the page PDF icon for left (even) pages
				_sb_s.pagePDF_buttonCont = $('#pagePDF_button_container');
				if (_sb_s.pagePDF_buttonCont) {
					var pagePDF_button = $('#pagePDF_button'),
						showPagePDFHandler;
					// clear old current image element
					_sb_s.pagePDF_buttonCont.html("");
					// move button from parent #scrollview-bottom to end of parent #sb_body
					_sb_e.body.append($(_sb_s.pagePDF_buttonCont));
					// the page PDF icon for left (even) pages or for slide mode
					if (_sb_s.pageTurnMode == 'turn') {
						if (_sb_s.pageDisplay == 'double') {
							showPagePDFHandler = function(e) {
								var pageImgID = "#pv_P" + _sb_s.pageNumLeft,
									current_pagePDF = $(pageImgID).attr('data-page-pdf'),
									winname = "PDF",
									winparams = "resizable=Yes,scrollbars=Yes";
								if (_sb_s.inApp == true) {
									winname = "_blank";
									winparams = "location=yes,toolbar=yes,enableViewportScale=yes";
									if (window.localStorage) window.localStorage.setItem('dirlinknReturnPageIdx', ""+(_sb_s.pageNumLeft-1));
									if (_sb_s.is_Android) {
										// call the PDF viewer from parent window App
										//alert("calling viewpdf: "+current_pagePDF);
										fb_poll_action_set("viewpdf",current_pagePDF);
										return;
									}
								}
								//log_methods.log("showPagePDFHandler pageImgID: " + pageImgID + "\n\tcurrent_pagePDF: " + current_pagePDF );
								// open the page PDF in a new Window
								window.open(current_pagePDF,winname,winparams);
							}
						}
						else {	// single turning pages
							showPagePDFHandler = function(e) {
								var pageImgID = "#pv_P" + (_sb_s.currentPageIdx + 1),
									current_pagePDF = $(pageImgID).attr('data-page-pdf'),
									winname = "PDF",
									winparams = "resizable=Yes,scrollbars=Yes";
								if (_sb_s.inApp == true) {
									winname = "_blank";
									winparams = "location=yes,toolbar=yes,enableViewportScale=yes";
									if (window.localStorage) window.localStorage.setItem('dirlinknReturnPageIdx', ""+_sb_s.currentPageIdx);
									if (_sb_s.is_Android) {
										// call the PDF viewer from parent window App
										//alert("calling viewpdf: "+current_pagePDF);
										fb_poll_action_set("viewpdf",current_pagePDF);
										return;
									}
								}

								//log_methods.log("showPagePDFHandler pageImgID: " + pageImgID + "\n\tcurrent_pagePDF: " + current_pagePDF );
								// open the page PDF in a new Window
								window.open(current_pagePDF,winname,winparams);
							}
						}
					}
					else {	// 'slide' mode
						showPagePDFHandler = function(e) {
							var pageImgID = "#pv_P" + (_sb_s.currentPageIdx + 1),
								current_pagePDF = $(pageImgID).attr('data-page-pdf'),
								winname = "PDF",
								winparams = "resizable=Yes,scrollbars=Yes";
							if (_sb_s.inApp == true) {
								winname = "_blank";
								winparams = "location=yes,toolbar=yes,enableViewportScale=yes";
								if (window.localStorage) window.localStorage.setItem('dirlinknReturnPageIdx', ""+_sb_s.currentPageIdx);
								if (_sb_s.is_Android) {
									// call the PDF viewer from parent window App
									//alert("calling viewpdf: "+current_pagePDF);
									fb_poll_action_set("viewpdf",current_pagePDF);
									return;
								}
							}
							//log_methods.log("showPagePDFHandler pageImgID: " + pageImgID + "\n\tcurrent_pagePDF: " + current_pagePDF );
							// open the page PDF in a new Window
							window.open(current_pagePDF,winname,winparams);
						}
					}
				
					_sb_s.pagePDF_buttonCont.attr('title', lg[13][_sb_s.cur_lang_ID]);
					_sb_s.pagePDF_buttonCont.attr('alt', lg[13][_sb_s.cur_lang_ID]);
					// will be turned on by 'cb_turn_end()'   _sb_s.pagePDF_buttonCont.css('display', 'block');

					/* make page PDF button draggable */
					if (_sb_s.enableDraggableObjects > 0) {
						new XdraggableObject(document.getElementById("pagePDF_button_container"),null,null,showPagePDFHandler);
					}
					else pagePDF_button.on(_sb_s._elementTouch, showPagePDFHandler);

					if ((_sb_s.pageTurnMode == 'turn') && (_sb_s.pageDisplay == 'double')) {
						var showPagePDFHandlerR = function(e) {
							var pageImgID = "#pv_P" + _sb_s.pageNumRight,
								current_pagePDF = $(pageImgID).attr('data-page-pdf'),
								winname = "PDF",
								winparams = "resizable=Yes,scrollbars=Yes";
							if (_sb_s.inApp == true) {
								winname = "_blank";
								winparams = "location=yes,toolbar=yes,enableViewportScale=yes";
								if (window.localStorage) window.localStorage.setItem('dirlinknReturnPageIdx', ""+(_sb_s.pageNumRight-1));
								if (_sb_s.is_Android) {
									// call the PDF viewer from parent window App
									//alert("calling viewpdf: "+current_pagePDF);
									fb_poll_action_set("viewpdf",current_pagePDF);
									return;
								}
							}
							//log_methods.log("showPagePDFHandler pageImgID: " + pageImgID + "\n\tcurrent_pagePDF: " + current_pagePDF );
							// open the page PDF in a new Window
							window.open(current_pagePDF,winname,winparams);
						};
				
						// the page PDF icon for right (odd) pages
						_sb_s.pagePDF_buttonContR=document.createElement("div");
						_sb_s.pagePDF_buttonContR.id= "pagePDF_button_containerR";
						_sb_e.bodyEl.appendChild(_sb_s.pagePDF_buttonContR);

						_sb_s.pagePDF_buttonContR = $('#pagePDF_button_containerR');	// make JQuery access

						_sb_s.pagePDF_buttonContR.attr('title', lg[13][_sb_s.cur_lang_ID]);
						_sb_s.pagePDF_buttonContR.attr('alt', lg[13][_sb_s.cur_lang_ID]);
						// will be turned on by 'cb_turn_end()'   _sb_s.pagePDF_buttonContR.css('display', 'block');

						/* make page PDF button draggable */
						if (_sb_s.enableDraggableObjects > 0) {
							new XdraggableObject(document.getElementById("pagePDF_button_containerR"),null,null,showPagePDFHandlerR);
						}
						else _sb_s.pagePDF_buttonContR.on(_sb_s._elementTouch, showPagePDFHandlerR);

						// set buttons
						cb_turn_end("init", 1);
					}
					else {	// turn on page PDF button for slidebook mode
						_sb_s.pagePDF_buttonCont.css('display', 'block');
					}
				
				}
			}
		}



		/* ========================
		 * creating the page thumbs navigation must be delayed until all main pages are loaded
		 */
		if ((_sb_s.show_page_thumbs > 0) && (_sb_s.pageImageLoadType <= 0)) {
			do {
				if (_sb_s.show_page_thumbs == 1) { createPageThumbsNavigation(); break; }	// page thumbs for all systems
				if ((_sb_s.show_page_thumbs & 2) > 0) { createPageThumbsNavigation(); break; }	// create page thumbs for all
			} while(false);
		}



		/* ========================
		 * creating the page navigation using a buttons bar
		 */
		var nsb_pager = $('#sb_pager');
		if ((_sb_s.show_page_navigation > 0) && nsb_pager) {
			// move pager from parent #scrollview-trailer to end of parent #sb_body
			_sb_e.body.append($(nsb_pager));
			/* set the pager arrows */
			$('#sb_pagearrow_first').on(_sb_s._elementTouch, function(e){ if (_sb_s.lightboxIsShowing && _sb_s.allowArticleStepping) show_previous_article(true,true); else goto_page (0,true,null,null,true,this.event); this.blur(); });
			$('#sb_pagearrow_prev').on(_sb_s._elementTouch, function(e){ if (_sb_s.lightboxIsShowing && _sb_s.allowArticleStepping) show_previous_article(true); else goto_page (-2,true,null,null,true,this.event); this.blur(); });
			$('#sb_pagearrow_next').on(_sb_s._elementTouch, function(e){ if (_sb_s.lightboxIsShowing && _sb_s.allowArticleStepping) show_next_article(true); else goto_page (-1,true,null,null,true,this.event); this.blur(); });
			$('#sb_pagearrow_last').on(_sb_s._elementTouch, function(e){ if (_sb_s.lightboxIsShowing && _sb_s.allowArticleStepping) show_next_article(true,true); else goto_page (_sb_s.totalPages-1,true,null,null,true,this.event); this.blur(); });
			$('#sb_page_entry_cont').on(_sb_s._elementTouch, function(e) {
					manual_gotopage(e, _sb_e.sb_page_entry_field.value,false);
					hideLightbox(e);
					_sb_e.sb_page_entry_field.blur();
					_sb_s.user_entry_active = false;	// this enables flipbook resize 
					//e.stopPropagation();
					//alert("touch sb_page_entry_cont");
					});
			if (!_sb_s.isTouchDevice) {
				$('#sb_page_entry_field').on('click', function(e) {
						if (_sb_s.sb_page_entry_field_behaviour & 1) _sb_e.sb_page_entry_field.value = "";	// clear on focus
						_sb_e.sb_page_entry_field.focus();
						e.preventDefault();
						e.stopPropagation();
						//alert("click sb_page_entry_field");
						});
			}
			else {
				$('#sb_page_entry_field').on(_sb_s._elementTouch, function(e) {
						_sb_s.user_entry_active = true;	// this disables flipbook resize during search entry 
						_sb_e.sb_page_entry_field.focus();
						if (_sb_s.sb_page_entry_field_behaviour & 1) _sb_e.sb_page_entry_field.value = "";	// clear on focus
						e.preventDefault();
						e.stopPropagation();
						//alert("touch3 " + _sb_e.sb_page_entry_field + " sb_page_entry_field");
						});
				$('#sb_page_entry_field').on("blur", function(e) {
						_sb_s.user_entry_active = true;	// this disables flipbook resize during search entry 
						_sb_e.sb_page_entry_field.blur();
						e.preventDefault();
						e.stopPropagation();
						});
				$('#sb_page_entry_field').on("keypress", function(e) {
						var key=e.keyCode || e.which;
						if (key==13) {
							//alert("key: " + key);
							_sb_e.sb_page_entry_field.blur();
							_sb_s.user_entry_active = false;	// this enables flipbook resize 
						}
						//e.preventDefault();
						e.stopPropagation();
						});
			}
			// have to z-index pager above lightboy overlay shade
			if (_sb_s.allowArticleStepping) {
				nsb_pager.css("zIndex",1010);
			}
			// add a virtual keyboard to page number input field
			try {
				if (_sb_s.addVirtualKeybord && !_sb_s.isTouchDevice) {
					var pageInput = document.getElementById('sb_page_entry_field');
					//alert("pageInput field: " + pageInput.getAttribute("VKI_attached"));
					if (!pageInput.getAttribute("VKI_attached")) VKI_attach(pageInput);
					//alert("VKI to page input field attached: "+pageInput.getAttribute("VKI_attached"));
				}
			} catch(ex){}
			// make the page navigation arrows draggable
			if (_sb_s.enableDraggableObjects > 0) {
					new XdraggableObject(document.getElementById("sb_pager"));
					new XdraggableObject(document.getElementById("sb_page_entry_cont"),document.getElementById("sb_pager"));	// make it draggable from the "goto page arrow"
						//log_methods.log("Xdraggable sb_pager");
			}

			if (_sb_s.totalPages <= 1) nsb_pager.css('visibility', 'hidden');
			//nsb_pager.css('display', 'inline-block');
			nsb_pager.css('display', 'block'); // must be 'block' because is moved to body
			// update the goto page field
			if (_sb_s.sb_page_entry_field_behaviour & 2) {
				var curentPageDisplay = getPageNameFromPageIndex(_sb_s.currentPageIdx);
				//log_methods.log("_sb_s.currentPageIdx: " + _sb_s.currentPageIdx + "\n\tcurentPageDisplay: " + curentPageDisplay);
				if (curentPageDisplay != "") _sb_e.sb_page_entry_field.value = curentPageDisplay;
			}
		}


		// start page image loading in background for all the rest of pages
		// if turnmode is 'turn'
		// for 'slide' mode we load pages earlyer
		if ((_sb_s.pageImageLoadType <= 0) && (_sb_s.pageTurnMode == 'turn')) {
			setTimeout("$.preload_pageimages(1)",_sb_s.pageImageLoadDelay);	// page image 0 is already loaded
		}

		$.wait_preloaded_pageimages();

		// do next init step
		$.init_more();

	};

	$.init_more = function () {
		var areas, i, ii, hdlrStr;

		// attach more event handlers to area for touch devices
		areas = document.getElementsByTagName("area");
		for (i = 0, ii = areas.length; i < ii; i++) {
			_sb_fn.attachGestureDetector(areas[i],null,'area');
		}

		if (_sb_s.isTouchDevice || !_sb_s.doArticleShade) {	// if no canvas shading
			//log_methods.log("torque map areas: " + _sb_s.isTouchDevice);
			for (i = 0, ii = areas.length; i < ii; i++) {
				hdlrStr = areas[i].getAttribute("onMouseOver");
				if ((hdlrStr != null) && (hdlrStr != "")) {	//turn "mouse over" to "touch start"
					//log_methods.log("area onMouseOver: " + hdlrStr);
					areas[i].removeAttribute("onMouseOver");
					areas[i].setAttribute("on"+_sb_s.pointerEvents.start,hdlrStr);
				}
				hdlrStr = areas[i].getAttribute("onClick");
				if ((hdlrStr != null) && (hdlrStr != "")) {	//turn "click" to "touch end"
					//log_methods.log("area onClick: " + hdlrStr);
					areas[i].removeAttribute("onClick");
					areas[i].setAttribute("on"+_sb_s.pointerEvents.end,hdlrStr);
				}
				
				hdlrStr = areas[i].getAttribute("onMouseOut");
				if ((hdlrStr != null) && (hdlrStr != "")) {
					//log_methods.log("area onMouseOut: " + hdlrStr);
					areas[i].removeAttribute("onMouseOut");
					areas[i].setAttribute("on"+_sb_s.pointerEvents.out,hdlrStr);
				}
				
				
				/*
				addEventHandler(areas[i],[_sb_s._elementTouch,_sb_s.pointerEvents.move],	// on mousedown
								function(e) {
									floater(e,this,20);
								},
								false);
				*/
			}
		}
		

		// try to find the full-text search system to add the 'search' button
		var stopPreloadingPages = false,
			doInitSearch = 1,
			overrideSearch,
			want_search = get_site_param("search"),	// check if search is disabled by params
			want_nosearch = get_site_param("nosearch");	// check if search is disabled by params
		if (want_search != null) try { doInitSearch = parseInt(want_search, 10); } catch(ex) {}
		if (want_nosearch != null) try { doInitSearch = 0; } catch(ex) {}
		if (_sb_s.is_IE && (_sb_s.IEVersion < 8)) doInitSearch = 0;
		if (doInitSearch > 0) {
			var fts_enabled = 3,	// init to both searches: local and via ft-DB
				fts_enabled_css = get_css_value("enable_full_text_search","zIndex");	// check if we have full-text search flag style in css
			if ((typeof fts_enabled_css != "undefined") && (fts_enabled_css != "")) {	// found helper style
				fts_enabled = parseInt(fts_enabled_css, 10);
			}
				//log_methods.log("fts_enabled: " + fts_enabled + " - " + (fts_enabled & 1));

			var local_fts_enabled = ((fts_enabled & 2) > 0 ? 1 : 0),	// init to state given in fts_enabled
				local_fts_enabled_css = get_css_value("enable_local_search","zIndex");	// check if we have full-text search flag style in css
			if ((typeof local_fts_enabled_css != "undefined") && (local_fts_enabled_css != "")) {	// found helper style
				local_fts_enabled = parseInt(local_fts_enabled_css, 10);
			}
				//log_methods.log("local_fts_enabled: " + local_fts_enabled);

			var param_fts_enabled = get_site_param("nosearch");
			if (param_fts_enabled != null) {
				local_fts_enabled = fts_enabled = 0;	// fulltext search totally disabled
			}
			// now we may override above settings with a site param
			overrideSearch = get_site_param("searchm");	// check if search mode is given by params
			if (overrideSearch != null) {
				try {
					overrideSearch = parseInt(overrideSearch, 10);
						//log_methods.log("overridden to: " + overrideSearch);
					fts_enabled = overrideSearch & 1;
					local_fts_enabled = overrideSearch & 2;
				} catch(ex){}
			}

			if ((local_fts_enabled > 0) && ((fts_enabled & 1) == 0)) {	// enable local search if not ft-DB search
					//log_methods.log("starting local search - local_fts_enabled: " + local_fts_enabled);
				try { enable_local_search(); } catch(ex) {}
			}
			if (fts_enabled > 0) {				// enable ft-DB search
					//log_methods.log("starting DB search - local_fts_enabled: " + fts_enabled);
				if ((fts_enabled & 1) > 0) setTimeout("_sb_fn.init_fulltext_search()", 20);
			}
		}

		// check if we have to jump directly to a page or an article
		var pageidx, pagenumberHandled = false,
			gotoPageIDX;

		// check if we have to restore a page because we return from an external link like an opened PDF in a inAppBrowser
		//alert (window.localStorage);
		if (window.localStorage) {
			gotoPageIDX = window.localStorage.getItem('dirlinknReturnPageIdx');
			if (gotoPageIDX !== null) {
				gotoPageIDX = parseInt(gotoPageIDX, 10);
				//alert("gotoPageIDX: " + gotoPageIDX);
				if (gotoPageIDX >= 0) goto_page(gotoPageIDX,true);
				window.localStorage.removeItem('dirlinknReturnPageIdx');
			}
		}

		// check if we have to jump directly to a page
		gotoPageIDX = get_site_param("P");	// a page index starting from 0
		if (!pagenumberHandled && (gotoPageIDX != null)) {
			pageidx = parseInt(gotoPageIDX, 10);
			try {
				if ((pageidx != null) && (pageidx >= 0)) goto_page(pageidx,true);
			} catch(ex) {}
		}
		var gotoPageNumber = get_site_param("p");	// a page sequence number starting from 1 Usually 1 higher than page index
		if (!pagenumberHandled && (gotoPageNumber != null)) {
			pageidx = parseInt(gotoPageNumber, 10) - 1;
			try {
				if ((pageidx != null) && (pageidx >= 0)) goto_page(pageidx,true);
			} catch(ex) {}
		}
		var gotoPageName = get_site_param("pn");	//  a page name like 1,2,3,a,ab,xxii
		if (gotoPageName != null) {
			pageidx = getPageIndexFromPageName(gotoPageName);
			try {
				if ((pageidx != null) && (pageidx >= 0)) goto_page(pageidx,true);
			} catch(ex) {}
		}

		var gotoLabel = get_site_param("lbl");	//  a label like TOC
		if (gotoLabel != null) {
			_sb_fn.showLabeledArticle(gotoLabel);
		}

		var gotoArticleID = get_site_param("a");	// an article id like id="Art32_3" = art 32 on page 3
													// or ?a=32&p=3
		if (gotoArticleID != null) {
			if (gotoArticleID.indexOf("Art") < 0) {
				var the_pagenumber = get_site_param("p");
				if (the_pagenumber != null) {
					gotoArticleID = "Art" + gotoArticleID + "_" + the_pagenumber;
					pagenumberHandled = true;
				}
			}
			pageidx = getPageIDXFromArticleID(gotoArticleID);
			try {	// get the page number too
				if ((pageidx != null) && (pageidx >= 0)) goto_page(pageidx,true);
			} catch(ex) {}
			stopPreloadingPages = true;
			show_article_xml(null,gotoArticleID,'','','','','',pageidx+1);
		}


		var pMaxSearchResults = get_site_param("msr");	//  set max search results
		if (pMaxSearchResults != null) {
			try {
				searchMaxResults = parseInt(pMaxSearchResults, 10);
			} catch(ex) {}
		}
		var pMaxSearchResultsTextLength = get_site_param("msl");	//  set max search text length
		if (pMaxSearchResultsTextLength != null) {
			try {
				searchMaxTextLength = parseInt(pMaxSearchResultsTextLength, 10);
			} catch(ex) {}
		}

		var searchTerm = get_site_param("s");	//  a search term and show best match
		if (searchTerm != null) {
			if (searchTerm.indexOf("%22") == 0) searchTerm = searchTerm.substr(3);
			if (searchTerm.indexOf("%22") == searchTerm.length-3) searchTerm = searchTerm.substr(0,searchTerm.length-3);
			try {
				goto_searchterm(searchTerm, true, true);
			} catch(ex) {}
		}
		else {
			searchTerm = get_site_param("S");	//  a search term and show search results
			if (searchTerm != null) {
				if (searchTerm.indexOf("%22") == 0) searchTerm = searchTerm.substr(3);
				if (searchTerm.indexOf("%22") == searchTerm.length-3) searchTerm = searchTerm.substr(0,searchTerm.length-3);
				try {
					goto_searchterm(searchTerm, true, false);
				} catch(ex) {}
			}
		}

		if (stopPreloadingPages == true) $.stopPreloadPagesUntilBodyLoaded(0);


		// set up the features popup content. defined in 'custom.js'
		var showfeaturescss = get_css_value("show_features_pop","zIndex"),	// try to get from css
			doShowFeatures = 1;
		if ((typeof showfeaturescss != "undefined") && (showfeaturescss != "")) doShowFeatures = parseInt(showfeaturescss, 10);

		showfeaturescss = get_site_param("nofeatures");	// turn off features button
		if (showfeaturescss != null) {
			doShowFeatures = 0;
			//alert("features off");
		}
		_sb_s.show_features_pop = doShowFeatures;
		if (doShowFeatures > 0) {
			try { setTimeout("init_features_popup()",500) } catch(ex) {}
		}

		//log_methods.log("init_more DONE");

		return;
	};



	/* ========================
	 * Page Thumbs
	 */
	// dynamically create the page thumbs navigation
	var createPageThumbsNavigation = function () {
		//log_methods.log("CREATE createPageThumbsNavigation\n\tnum_epaper_pages: " + num_epaper_pages + "\n\tslidebook_load_state: " + _sb_s.slidebook_load_state);
		/* internal functions */
			function createPageThumbsList() {
				var ptlist = "", l;
				for (l = 0; l < num_epaper_pages; l++) {
					ptlist += '<li id="sb_pagethumbs-scrollli" class="sb_pagethumbs-scrollli"><img id="pt_P' + l + '" class="sb_pagethumbs-scrollimg" src="' + _sb_fn.getPageImage(l) + '" alt="' + (l+1) + '" ' + 'on' + _sb_s.pointerEvents.end + '="goto_page(' + l + ',true,null,null,true,event);"/><div class="sb_pagethumbs-scrollpgnum">' + getPageNameFromPageIndex(l) + '</div></li>'
				}
				//log_methods.log("LIST createPageThumbsNavigation\n\t" + ptlist);
				nsb_pagethumbsScrollist.html(ptlist);
				return(num_epaper_pages);
			}

			function calcThumbsHeight() {
				//return(_sb_e.scrollview_container.height() + _sb_e.scrollview_trailer.height() - 14);
				return(_sb_e.scrollview_container.height() - 14);
			}

		if (_sb_s.slidebook_load_state.indexOf("3") < 0) {	// wait for all images loaded
			if (_sb_s.pageImageLoadType == 0) setTimeout(function() { createPageThumbsNavigation(); }, 400);
			return;
		}

		var nsb_pagethumbsContainer = $('#sb_pagethumbs-container'),
			nsb_pagethumbsScrollist = $('#sb_pagethumbs-scrolllist');
		_sb_e.pageThumbsScrollContainer = $('#sb_pagethumbs-scrollcontainer');
		_sb_e.pageThumbsScrollContainerEl = document.getElementById('sb_pagethumbs-scrollcontainer');

		if (nsb_pagethumbsContainer.length > 0) {
			var numpages = createPageThumbsList();
			if (numpages < 0) {	// wait for all images loaded
				setTimeout(function() { createPageThumbsNavigation(); }, 200);
				return;
			}
			do {
				var nPtScrollistLi = $('#sb_pagethumbs-scrollli');
				_sb_s.pageThumbsLiHeight = nPtScrollistLi.outerHeight() + cssIntVal(nPtScrollistLi.css("marginTop")) + cssIntVal(nPtScrollistLi.css("marginBottom"));
				//alert("_sb_s.pageThumbsLiHeight: " + _sb_s.pageThumbsLiHeight + " \nimgHeight: " + nPtScrollistLi.children(":first").outerHeight() + " \nliHeight: " + nPtScrollistLi.outerHeight() + " \nmargin-top: " + nPtScrollistLi.css("marginTop") + " \nmargin-bottom: " + nPtScrollistLi.css("marginBottom"));
				if (numpages <= 1) break;

				calcThumbsHeight();
				_sb_e.pageThumbsScrollContainer.list();

				var fadeOutPageThumbsAnim = function (){
					_sb_e.pageThumbsScrollContainer.css('visibility', 'visible');
					_sb_e.pageThumbsScrollContainer.animate(
								{height: calcThumbsHeight()},
								600);
					},

				fadeInPageThumbsAnim = function (){
					_sb_e.pageThumbsScrollContainer.animate(
								{height: 0},
								600,
								function() {
									_sb_e.pageThumbsScrollContainer.css('visibility', 'hidden');
								});
					},
	
				showPageThumbs = function() {
						//log_methods.log("showPageThumbs height: " + calcThumbsHeight());
					fadeOutPageThumbsAnim();
				},
				hidePageThumbs = function() {
						//log_methods.log("hidePageThumbs height: " + calcThumbsHeight());
					fadeInPageThumbsAnim();
				},
				pageThumbsButtonClickHandler = function(e) {
						//_sb_s.DEBUGmode = 2; log_methods.log("pageThumbsButtonClickHandler: click on id:\n\t" + (e.target ? e.target : window.event.srcElement).id +"\n\ttarget: " + (e.target ? e.target : window.event.srcElement)+"\n\tcurrentTarget: " + e.currentTarget);
					if (e && e.preventDefault) e.preventDefault();
					if (e && e.stopPropagation) e.stopPropagation();
					//alert("click height: " + _sb_e.pageThumbsScrollContainer.css("height"));
					if (parseInt(_sb_e.pageThumbsScrollContainer.css("height"), 10) <= 0) showPageThumbs();
					else hidePageThumbs();
					return(false);
				};
				_sb_fn.hidePageThumbs = hidePageThumbs;

				/* make page Thumbs draggable */
				if (_sb_s.enableDraggableObjects > 0) {
					new XdraggableObject(document.getElementById("sb_pagethumbs-button"),document.getElementById("sb_pagethumbs-container"),[0,0],pageThumbsButtonClickHandler);
				}
				else nsb_pagethumbsContainer.on(_sb_s._elementTouch, pageThumbsButtonClickHandler);
				nsb_pagethumbsContainer.css('visibility', 'visible');
			} while(false);
		}
	};


	$.watchBodyContentLoaded = function () {
		_sb_e.debugmessage = $('#debugmessage');
		if (_sb_e.debugmessage.length <= 0) {
			setTimeout(function(){$.watchBodyContentLoaded();},100);
			return;
		}
		_sb_s.slidebook_load_state += "1";	// set to: body completely loaded

		return;
	}

			
	/* ========================
	 * canvas stuff to draw a shadow on article area(s)
	 */
	var canvas = new Array(),
		canvasVisible = false,
		canvasAllCleared = true,
		mouseOnCanvas = false,
		num_canvas = 0,
		xd = 0,
		yd = 0,
		wd = 0,
		hd = 0,
		shadecolor = _sb_s.articleShadeColors[0],
		shadetimeout = null;
	
	$.init_canvas = function() {
		if (!_sb_s.doArticleShade) return(false);
		if (_sb_s.is_IE &&  (_sb_s.IEVersion <= 7)) {
			_sb_s.canvas_available = false;
			return(false);
		}
		num_canvas = 0;
		while (true) {
			try {
				var canvas_id = "areacanvas_" + num_canvas;
				canvas[num_canvas] = document.getElementById(canvas_id);
				//alert("canvas #"+num_canvas+ ": " + canvas[num_canvas]);
				if ((num_canvas == 0) && (canvas[num_canvas] == null)) {
					//alert(typeof $.init_canvas);
					setTimeout(function(){$.init_canvas();},50);
					return(false);
				}
				if (canvas[num_canvas] == null) { canvas.length -= 1; break; }
				var my_canvas_style = canvas[num_canvas].style;
				my_canvas_style.zIndex = my_canvas_style.left = my_canvas_style.top = my_canvas_style.width = my_canvas_style.height = 0;
				canvas[num_canvas].setAttribute("data-canvasfilled","0");	// mark as cleared shadow
			} catch(ex) { break; }
			num_canvas++;
		}
		num_canvas = canvas.length;
		if (num_canvas <= 0) _sb_s.canvas_available = false;
		else {
			try {
				if (typeof(canvas[0].getContext) == 'function') _sb_s.canvas_available = true;
				else _sb_s.canvas_available = false;
			} catch(ex) {
				_sb_s.canvas_available = false;
			}
		}
		//alert("_sb_s.canvas_available: " + _sb_s.canvas_available);
		return (_sb_s.canvas_available);
	};

	/**
	 * Clear all article shading
	 * a global function
	 *
     * optional parameters;
	 * @param {number|null=} func The function to perform
	 *
	 * @return {boolean} true  if actually had to clear
	 */
	_sb_fn.clear_all_shadows = function (func) {
		if (!_sb_s.canvas_available) return(false);
		if (shadetimeout != null) {
			clearTimeout(shadetimeout);
			shadetimeout = null;
		}
		//log_methods.log("clear_all_shadows canvasAllCleared: " + canvasAllCleared + " func: " + func);
		if (canvasAllCleared) return(false);
	
		var idx = 0;
		//document.getElementById("debugmessage").innerHTML = "clear_all_shadows(func): " + func;
		while (idx < num_canvas) {
			try { 
				if ((func == -1) && (canvas[idx].getAttribute("data-articleshadow") == "1")) {
					idx++; continue;
				}
			} catch(ex) {}	// canvas mouseout
			try { canvas[idx].removeAttribute("data-articleshadow"); } catch(ex) {}
			if (canvas[idx].getAttribute("data-canvasfilled") == "0") { idx++; continue; }	// is already cleared
			try {
				//log_methods.log("clear_all_shadows clear canvas: " + idx);
				//if ((typeof(func) == 'undefined') || (func != 0)) drawshadow(canvas[idx],0);	// clear canvas
				drawshadow(canvas[idx],0);	// clear canvas
				var my_canvas_style = canvas[idx].style;
				my_canvas_style.zIndex = my_canvas_style.left = my_canvas_style.top = my_canvas_style.width = my_canvas_style.height = 0;
				canvas[idx].left = canvas[idx].top = canvas[idx].width = canvas[idx].height = 0;
			} catch(ex) {  }
	
			idx++;
		}
	
		canvasAllCleared = true;
		_sb_s.mouseOnCanvas = mouseOnCanvas = false;
		return(true);
	};
	
	var ctx = null;
	/**
	 * Draw article shadow
	 * call like:
	 *		goto_page(10, false);
	 *
	 * @param {Object} the_canvas the canvas object to use
	 * @param {number} func which function to perform
	 * optional parameters;
	 * @param {number|null=} x X-position
	 * @param {number|null=} y Y-position
	 * @param {number|null=} w the width
	 * @param {number|null=} h the height
	 * @param {number|null=} angle the rotation angle
	 * @param {number|null=} tx x displace
	 * @param {number|null=} ty y displace
	 * @param {number|null=} obj an info object with id attribute
	 */
	var drawshadow = function (the_canvas,func,x,y,w,h,angle,tx,ty,obj) {
		var cs;
		if (!_sb_s.canvas_available) return(false);
		if (the_canvas == null) return(false);
		//if (ctx != null) ctx = null;	// make sure to trash it
		ctx = the_canvas.getContext("2d");
		if ((func === 1) || (func === 10)) {
			// test to show unrotated 0,0
			//ctx.fillStyle = "rgba(200,0,0,1)"; ctx.fillRect (0, 0, 4, 4);
			ctx.save();
			if ((angle != null) && (angle != 0)) {
				var DEG2RAD = Math.PI/180,
					myangle = angle * DEG2RAD;
				ctx.translate(tx,ty);
				ctx.rotate(myangle);
			}
			// test to show translated/rotated 0,0
			//ctx.fillStyle = "rgba(0,0,255,1)"; ctx.fillRect (0, 0, 6, 6);
	
			//log_methods.log("drawshadow x, y, w, h: " + x + ", " + y + ", " + w + ", " + h);
			ctx.fillStyle = shadecolor;
			try {
				ctx.fillRect (x, y, w, h);
				the_canvas.setAttribute("data-canvasfilled","1");	// mark as filled shadow
				canvasAllCleared = false;
			} catch(ex) {
				//alert("invalid area coords (x,y,w,h): " + x + "," + y + "," + w + "," + h + " on object id: " + obj.id);
			}
			canvasVisible = true;
			ctx.restore();
			//log_methods.log("drawshadow - canvas: " + "x: " + x + ", y:" + y + ", w: " + w + ", h: " + h);
		}
		else {
//			ctx.save();
			ctx.clearRect(0,0,the_canvas.width,the_canvas.height);
			//ctx.beginPath();
			canvasVisible = false;
			the_canvas.setAttribute("data-canvasfilled","0");	// mark as cleared
			//log_methods.log("drawshadow - CLEAR func: " + func);
		}
		//ctx = null;	// make sure to trash it
		return(true);
	};
	
	var areashadow = function (func,obj,areas,windowOffsX,windowOffsY,pagesequence) {
		var i, ii,
			coords_arr,
			coords,
			l, t, w, h,
			lc, tc, wc, hc,
			angle,
			tx, ty,
			my_canvas_style, prevShadeColor;
		
		function resizeCoord(coord) {
			if ((_sb_s.pageAdjustCurrentRatio == 1.0) && (_sb_s.pageImageCoordsRatio == 1.0)) return(parseInt(coord, 10));

			return( Math.floor(parseInt(coord, 10) * _sb_s.pageAdjustCurrentRatio) * _sb_s.pageImageCoordsRatio);
		}
		if (!_sb_s.canvas_available || (canvas == null) ) return(false);
		//_sb_s.DEBUGmode = 2; log_methods.log("areashadow:" + func + "\n\tareas: " + areas + "\n\twindowOffsX: " + windowOffsX + "\n\twindowOffsY: " + windowOffsY + "\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding);
		_sb_fn.clear_all_shadows(func);
		if (func <= 0) return(false);
		if (_g_turnjs_pageIsFolding === true) return false;	// we are moving a folded page corner to turn the page
		if (_sb_s.pageIsScrolling) return false;

		coords_arr = null;
		if ( (areas != null) && (areas != "") ) coords_arr = areas.split(";");
		else return(false);
		if (!coords_arr) return(true);

		for (i = 0, ii = coords_arr.length; i < ii; i++) {
			if (coords_arr[i] =="") continue;
			if (i >= canvas.length) break;
	
			coords = coords_arr[i].split(",");	// l,t,r,b
			if ((coords.length >= 10) && (typeof(pagesequence) != 'undefined')) {	// [9] is the pagesequence in question
				if (coords[9] != parseInt(pagesequence, 10)) continue;
			}
			if (parseInt(coords[0], 10) < 0) coords[0] = 0;	// left
			if (parseInt(coords[1], 10) < 0) coords[1] = 0;	// top
			l = windowOffsX + resizeCoord(coords[0]);
			t = windowOffsY + resizeCoord(coords[1]);
			w = resizeCoord(coords[2]) - resizeCoord(coords[0]);
			h = resizeCoord(coords[3]) - resizeCoord(coords[1]);
			lc = l;	// init canvas size to same
			tc = t;
			wc = w;
			hc = h;
			angle = 0.0;
			if (coords.length > 4) {
				if (coords[4] != "") angle = parseFloat(coords[4]);
			}
			if (coords.length > 5) {	// override canvas coords with rotated containing rect
				if (parseInt(coords[5], 10) < 0) coords[5] = 0;	// left
				if (parseInt(coords[6], 10) < 0) coords[6] = 0;	// top
				lc = windowOffsX + resizeCoord(coords[5]);
				tc = windowOffsY + resizeCoord(coords[6]);
				wc = resizeCoord(coords[7]) - resizeCoord(coords[5]);
				hc = resizeCoord(coords[8]) - resizeCoord(coords[6]);
			}
			if ((wc <= 0) || (hc <= 0)) continue;
			if ((tc + hc) > ((_sb_s.pageHeight * _sb_s.pageAdjustCurrentRatio) + windowOffsY)) {
				hc = Math.floor((_sb_s.pageHeight * _sb_s.pageAdjustCurrentRatio) + windowOffsY - tc);
			}
			tx = l - lc; if (lc > l) tx = lc - l;
			ty = t - tc; if (tc > t) ty = tc - t;
			/*
				log_methods.log("areashadow:"
						+ "<br>windowOffsX : windowOffsY: " + windowOffsX +":"+ windowOffsY
						+ "<br>areas: " + areas
						+ "<br>coords: " + coords
						+ "<br>lc:"+lc + "\ntc:"+tc + "\nwc:"+wc + "\nhc:"+hc
						+ "<br>l:"+l+" lc:"+lc + " tx:"+tx
						+ "<br>t:"+t+" tc:"+tc + " ty:"+ty
						+ "<br>w:"+w+" h:"+h
						+ "<br>angle:"+angle 
						+ "<br>pageWidth:"+_sb_s.pageWidth+" height:"+_sb_s.pageHeight
						+ "<br>pageAdjustCurrentRatio:"+_sb_s.pageAdjustCurrentRatio);
			*/
			//document.getElementById("debugmessage").innerHTML = canvas.length + "/canvas:" + i + " / " + coords;
			my_canvas_style = canvas[i].style;
	
		//	if (_sb_s.is_IE) my_canvas_style.backgroundImage = "url('.')";	// hack to enable IE to handle mouse clicks on 'transparent' canvas !!!
			my_canvas_style.zIndex = 100;
			my_canvas_style.left = (lc+xd) +"px";
			my_canvas_style.top = (tc+yd) +"px";
			my_canvas_style.width = (wc+wd) +"px";
			my_canvas_style.height = (hc+hd) +"px";
			canvas[i].left = lc + xd;
			canvas[i].top = tc + yd;
			canvas[i].width = wc + wd;
			canvas[i].height = hc + hd;
			if (func != 10) attachGestureDetector(canvas[i],null,"canvas");
			else canvas[i].setAttribute("data-articleshadow","1");	// mark as pagearticle shadow
	
			prevShadeColor = shadecolor;
			if (func == 10) shadecolor = _sb_s.articlePageShadeColor;
			drawshadow(canvas[i],func, 0, 0, w+wd, h+hd, -angle, tx, ty, obj);
			if (func == 10) shadecolor = prevShadeColor;
		}
		if (shadecolor == _sb_s.articleShadeColors[0]) shadecolor = _sb_s.articleShadeColors[1];
		else shadecolor = _sb_s.articleShadeColors[0];
		if (func != 10) {
			if (shadetimeout != null) { clearTimeout(shadetimeout); shadetimeout = null; }
			shadetimeout = setTimeout("_sb_fn.clear_all_shadows(0)", 2000);	// this allows smaller areas to become active
		}
		else {}	// page article shadows will be hidden when the article window is closed
	
		return(false);
	};



	/* ========================
	 * Lightbox
	 */
	var nScrollUpButton = null,
		nScrollDownButton = null,
		artScrollUpButtonHeight = 0,
		artScrollDownButtonHeight = 0;
	if (_sb_s.useArticleScrollButtons > 0) {
		var scrollUpButtonCont = "", scrollDownButtonCont = "", lightboxButtonScrolling = false, lightboxButtonScrolltimer = null;
		switch (_sb_s.useArticleScrollButtons) {
			case 2:	// button bars on top/bottom
				scrollUpButtonCont = '<div id="ArtScrollUpButton" class="lightbox_scrollUpButton2 hidden"></div> ';
				scrollDownButtonCont = '<div id="ArtScrollDownButton" class="lightbox_scrollDownButton2 hidden"></div> ';
				_sb_e.lightbox_content.prepend(scrollUpButtonCont);
				_sb_e.lightbox_content.append(scrollDownButtonCont);
				break;
			default:
				scrollUpButtonCont = '<div id="ArtScrollUpButton" class="lightbox_scrollUpButton1 hidden"></div> ';
				scrollDownButtonCont = '<div id="ArtScrollDownButton" class="lightbox_scrollDownButton1 hidden"></div> ';
				_sb_e.lightbox.prepend(scrollUpButtonCont);
				_sb_e.lightbox.append(scrollDownButtonCont);
				break;
		}
		nScrollUpButton = $('#ArtScrollUpButton');
		nScrollDownButton = $('#ArtScrollDownButton');
		artScrollUpButtonHeight = nScrollUpButton.height();
		artScrollDownButtonHeight = nScrollDownButton.height();
		
		var scrollUpButtonHandler = function (e,repeat) {
			if ((typeof(repeat) != 'undefined') && (repeat == true) && !lightboxButtonScrolling) return;
			lightboxButtonScrolling = true;
			var curscrolltoppx = _sb_e.lightbox_div.scrollTop();
			if (curscrolltoppx > 0) {
				curscrolltoppx -= _sb_s.articleScrollButtonsStep; if (curscrolltoppx < 0) curscrolltoppx = 0;
				_sb_e.lightbox_div.scrollTop(curscrolltoppx);
				if (lightboxButtonScrolltimer != null) clearTimeout(lightboxButtonScrolltimer);
				lightboxButtonScrolltimer = setTimeout(function(){ scrollUpButtonHandler(e,true); },_sb_s.articleScrollButtonsInterval);
			}
			//document.getElementById('debugmessage').innerHTML = "<br><br>scrollDown - offsetHeight: "+_sb_e.lightbox_div.height() + " scrollHeight: " +_sb_e.lightbox_div.get('scrollHeight')+ " scrollTop: " +curscrolltoppx + " ticks:"+new Date().getTime();
		},
		scrollDownButtonHandler = function (e,repeat) {
			if ((typeof(repeat) != 'undefined') && (repeat == true) && !lightboxButtonScrolling) return;
			lightboxButtonScrolling = true;
			var curscrolltoppx = _sb_e.lightbox_div.scrollTop(),
				maxscrolltoppx = _sb_e.lightbox_div.prop('scrollHeight')-_sb_e.lightbox_div.offset().height;
			if (curscrolltoppx < maxscrolltoppx) {
				curscrolltoppx += _sb_s.articleScrollButtonsStep; if (curscrolltoppx > maxscrolltoppx) curscrolltoppx = maxscrolltoppx;
				_sb_e.lightbox_div.scrollTop(curscrolltoppx);
				if (lightboxButtonScrolltimer != null) clearTimeout(lightboxButtonScrolltimer);
				lightboxButtonScrolltimer = setTimeout(function(){ scrollDownButtonHandler(e,true); },_sb_s.articleScrollButtonsInterval);
			}
			//document.getElementById('debugmessage').innerHTML = "<br><br>scrollDown - offsetHeight: "+_sb_e.lightbox_div.height() + " scrollHeight: " +_sb_e.lightbox_div.get('scrollHeight') + " maxscrolltoppx: " +maxscrolltoppx + " scrollTop: " +curscrolltoppx + " interv:"+_sb_s.articleScrollButtonsInterval + " step:"+_sb_s.articleScrollButtonsStep;
		};
	}


	var fadeOutLightboxAnim = function () {
		_sb_e.lightbox_div.animate(
			{ opacity:0, height: '0px', width: '0px' },
			200,
			function() {
				//alert("closed");
				_sb_e.lightbox_div.off();	// detach all events also from childs
				_sb_e.lightbox.off();	// detach all events also from childs
				_sb_e.lightbox.addClass('hidden');
				_sb_s.lightboxIsFading = false;
			});
		},          

	fadeInClosebuttonAnim = function () {
		_sb_e.lightbox_close.animate(
			{left: -62},
			400);
		},

	fadeOutClosebuttonAnim = function () {
		_sb_e.lightbox_close.animate(
			{left: -62},
			200,
			function() {	// animation end: lightbox is now closed
				closeDownLightbox();
			});
		},
	
	fadeOverlayAnim = function (settings, duration, fn) {
		//alert("settings: " + settings);
		_sb_e.lightbox_overlay.animate(
			settings,
			duration,
			fn);
		},

	fadeInReturnSearchbuttonAnim = function () {
		_sb_e.lightbox_returnsearch.animate(
			{left: -62},
			400);
		},

	fadeOutReturnSearchbuttonAnim = function () {
		_sb_e.lightbox_returnsearch.animate(
			{left: 0},
			200);
		};

	/**
	 * resize elements in the lightbox which require a certain dimension
	 */
	var resizeLightboxElements = function() {
		// check if we have to size any content elements
		if (_sb_s.lightboxContentDimIDs != "") {
			var ids = _sb_s.lightboxContentDimIDs.split(","), idx, el;
			for (idx = 0; idx < ids.length; idx++) {
				//alert("_sb_s.lightboxContentDimIDs: " + _sb_s.lightboxContentDimIDs);
				el = $("#"+ids[idx]);
				if (el.length <= 0) continue;
				if (el.width() > _sb_s.lightboxWidth) el.css('width', _sb_s.lightboxWidth + 'px');
				else el.width( Math.min(parseInt(el.attr('data-origw'),10),_sb_s.lightboxWidth));

				if (el.height() > _sb_s.lightboxHeight) el.css('height', _sb_s.lightboxHeight + 'px');
				else el.height( Math.min(parseInt(el.attr('data-origh'),10),_sb_s.lightboxHeight));
			}
		}
	};
	_sb_fn.resizeLightboxElements = resizeLightboxElements;

	/**
	 * Calculate the lightbox dimensions
	 * call like:
	 *		calcLightboxSizes(true);
	 *
	 * optional parameters;
	 * @param {boolean|null=} setHeight true to actually set the calculated dimensions
	 * @param {number|null=} lb_width required lightbox width if not default
	 * @param {number|null=} lb_height required lightbox height if not default
	 * @param {string|null=} contentDimIDs the IDs of elements requiring this dimensions
	 */
	var calcLightboxSizes = function(setHeight, lb_width, lb_height, contentDimIDs) {
		var scrollbarwidth = 30,
			lb_head_height,
			lightboxWidthTotal;
		if (lb_width) _sb_s.lightboxContentWidth = lb_width;	/* width and height of lightbox content - lightbox should be as large as needed */
		if (lb_height) _sb_s.lightboxContentHeight = lb_height;
		if (contentDimIDs) _sb_s.lightboxContentDimIDs = contentDimIDs;

		// calc buttons dimensions
		if (_sb_s.lightboxFadeCloseButton) _sb_s.closebuttonWidth = _sb_e.lightbox_close.width() - 16;
		else _sb_s.closebuttonWidth = 0;
		_sb_s.closebuttonLeft = _sb_e.lightbox_close.offset().left;
		_sb_s.searchbuttonWidth = _sb_e.lightbox_returnsearch.width() - 16;
		_sb_s.searchbuttonLeft = _sb_e.lightbox_returnsearch.offset().left;

		lb_head_height = 0;
		try { lb_head_height = $("#lightbox_head_container").height(); } catch(ex){}

		// calc max possible height
		_sb_s.lightboxHeight = _sb_e.sb_pagelist.height() - lb_head_height
								 - cssIntVal(_sb_e.lightbox.css("paddingTop")) - cssIntVal(_sb_e.lightbox.css("paddingBottom"))
								 - cssIntVal(_sb_e.lightbox_div.css("paddingTop")) - cssIntVal(_sb_e.lightbox_div.css("paddingBottom"));
		// check for requested height
		if ((_sb_s.lightboxContentHeight > 0) && (_sb_s.lightboxContentHeight < _sb_s.lightboxHeight)) _sb_s.lightboxHeight = _sb_s.lightboxContentHeight;
		if ((_sb_s.lightboxMaxHeight > 0) && (_sb_s.lightboxHeight > _sb_s.lightboxMaxHeight)) _sb_s.lightboxHeight = _sb_s.lightboxMaxHeight;
		if ((_sb_s.lightboxMinHeight > 0) && (_sb_s.lightboxHeight < _sb_s.lightboxMinHeight)) _sb_s.lightboxHeight = _sb_s.lightboxMinHeight;

		_sb_s.lightboxWidth = _sb_s.lightboxMaxWidth;
		if (_sb_s.lightboxContentWidth > 0) {
			 _sb_s.lightboxWidth = _sb_s.lightboxContentWidth;
			 if (_sb_s.lightboxWidth > _sb_s.windowWidth) _sb_s.lightboxWidth = _sb_s.windowWidth;
		}
		if ((_sb_s.windowWidth / _sb_s.lightbox2BodyWidth) > _sb_s.lightboxWidth) _sb_s.lightboxWidth = Math.floor(_sb_s.windowWidth / _sb_s.lightbox2BodyWidth);
		lightboxWidthTotal = _sb_s.lightboxWidth + _sb_s.closebuttonWidth + scrollbarwidth;
		if (_sb_s.windowWidth < lightboxWidthTotal) {
			_sb_s.lightboxWidth = _sb_s.windowWidth - _sb_s.closebuttonWidth - scrollbarwidth;	// also subtract evtl window scrollbars
			lightboxWidthTotal = _sb_s.lightboxWidth + _sb_s.closebuttonWidth + scrollbarwidth;
		}
		if ((_sb_s.lightboxContentWidth <= 0) && (_sb_s.lightboxMaxWidth > 0) && (_sb_s.lightboxWidth > _sb_s.lightboxMaxWidth)) _sb_s.lightboxWidth = _sb_s.lightboxMaxWidth;
		// check requested width

		_sb_s.lightboxTop = _sb_e.scrollview_container.offset().top;
		_sb_s.lightboxTop -= _sb_e.body_borderTopWidth + _sb_s.bodyOffsetTop;	// also subtract top offset if embedded in div
		_sb_s.lightboxLeft = _sb_s.closebuttonWidth;	// this also is the minimum left
		//log_methods.log("lightboxMaxWidth: " + _sb_s.lightboxMaxWidth + ", lightboxWidth: " + _sb_s.lightboxWidth);
		switch (_sb_s.lightboxPosition) {
			case 0:	// center in body minus close button width
				_sb_s.lightboxLeft = parseInt(_sb_s.windowWidth/2 - lightboxWidthTotal/2 + _sb_s.closebuttonWidth, 10);
				break;
			case 1:	// align to left body margin
				// already set
				break;
			case 2:	// align to right body margin
				_sb_s.lightboxLeft = _sb_s.windowWidth - _sb_s.lightboxWidth - scrollbarwidth - _sb_e.body_borderRightWidth - _sb_e.body_borderLeftWidth;	// don't forget scrollbar width
				break;
		}
		if (_sb_s.overridelightboxTop != invalidXYpos) {	// the lightbox was dragged
			_sb_s.lightboxLeft = _sb_s.overridelightboxLeft;
			_sb_s.lightboxTop = _sb_s.overridelightboxTop;
		}
		_sb_e.lightbox.css('top', _sb_s.lightboxTop + 'px');
		_sb_e.lightbox.css('left', _sb_s.lightboxLeft + 'px');
		_sb_e.lightbox_div.css('width', _sb_s.lightboxWidth + 'px');
		if (typeof(setHeight) != 'undefined') _sb_e.lightbox_div.css('height', _sb_s.lightboxHeight + 'px');

		/*
		_sb_s.DEBUGmode = 2;
		log_methods.log("calcLightboxSizes: " 
			+ "\n\t_sb_s.windowWidth: " + _sb_s.windowWidth
			+ "\n\ttop / left: " + _sb_s.lightboxTop + " / " + _sb_s.lightboxLeft
			+ " \n\twidth: " + lightboxWidthTotal+ ", height: " + _sb_s.lightboxHeight
			+ " \n\toverridelightboxTop: " + _sb_s.overridelightboxTop
			+ " \n\tscrollview_container top: " + _sb_e.scrollview_container.offset().top
			);
		*/
	};
	_sb_fn.calcLightboxSizes = calcLightboxSizes;
	
	/**
	 * Calculate the overlay dimensions
	 * call like:
	 *		calcOverlaySizes(true);
	 *
	 * optional parameters;
	 * @param {boolean|null=} setDims true to actually set the calculated dimensions
	 */
	var calcOverlaySizes = function(setDims) {
		var ovlHeight = _sb_e.sb_container.outerHeight(),
			ovlWidth = _sb_s.windowWidth;
		if (typeof(setDims) != 'undefined') {
			_sb_e.lightbox_overlay.css('width', ovlWidth + 'px');
			_sb_e.lightbox_overlay.css('height', ovlHeight + 'px');
		}

		return( [ovlWidth,ovlHeight] );
	};

	/**
	 * Show the lightbox
	 * call like:
	 *		showLightbox(e, "blabla", "text", "art1_3", true);
	 *
	 * @param {Object|null} e the event object or null
	 * @param {string} content the html text to show
	 * @param {string} contenttype the type of the content 'text'
	 * optional parameters;
	 * @param {string|null=} articleID the original article's id
	 * @param {boolean|null=} forceNewContent just do it
	 */
	var showLightbox = function(e,content, contenttype, articleID, forceNewContent) {
		//alert("showLightbox arguments: " + listArguments(arguments));
		if ((forceNewContent != true) && (_sb_s.lightboxIsFading == true)) return;	// semaphore to prevent multiple calls
		_sb_s.lightboxIsFading = true;
		_sb_s.lightboxIsShowing = true;
		setTimeout("_sb_s.lightboxIsFading = false;",2000);	// make sure we will never get blocked
		// set text into lightbox
		var setLightboxHeader = function() {
			if ((_sb_s.enable_print_lightbox > 0) || (_sb_s.enableDraggableObjects > 0) || (_sb_s.enable_lightBoxFontSize > 0)) {
				if ($('#lightbox_head_container').length <= 0) {
					var head = "<div id=\"lightbox_head_container\">",
						set_printer = false;
					if (!(_sb_s.is_IE && (_sb_s.IEVersion <= 8))) {		// all but IE8 and below
						if (_sb_s.enable_lightBoxFontSize > 0) head += '<div id="lightbox_fontsize_slider_container" class="lightbox_fontsize_slider_container"><span id="lightbox_fontsize_slider_sma" class="lightbox_fontsize_slider_sma">' + _sb_s.lightBoxFontSizeSliderSMA + '</span><div id="lightbox_fontsize_slider" class="lightbox_fontsize_slider"></div><span id="lightbox_fontsize_slider_lga" class="lightbox_fontsize_slider_lga">' + _sb_s.lightBoxFontSizeSliderLGA + '</span></div>';
						head += '<div id="lightbox_head"';
						if ((_sb_s.enable_print_lightbox > 0) || (_sb_s.enableDraggableObjects > 0) || (_sb_s.enable_lightBoxFontSize > 0)) {
							head += '>';
							if (_sb_s.enable_print_lightbox > 0) {
								if (_sb_s.inApp == false) set_printer = true;
								if ((_sb_s.inApp == true) && (_sb_s.inAppPrinting == true)) set_printer = true;
							}
							if (set_printer) head += '<div class="lightbox_print_icon" on' + _sb_s.pointerEvents.end + '="_sb_fn.printLightbox(event)"></div>';
						}
						else head += ' style="height:0">';
						head += '</div>';
					}
						head += '</div>';
					_sb_e.lightbox_content.prepend(head);

					if (_sb_s.enable_lightBoxFontSize > 0) {
							//log_methods.log("+++++ creatiung fontsize slider");
						// setup the font size slider
						_sb_s.lightBoxFontSizeSlider = $("#lightbox_fontsize_slider");
						_sb_s.lightBoxFontSizeSlider.attr("data-slider","true");
						_sb_s.lightBoxFontSizeSlider.attr("data-slider-range",""+_sb_s.lightBoxMinFontSize+","+_sb_s.lightBoxMaxFontSize);
						$(window).initSimpleSlider(_sb_s.lightBoxFontSizeSlider);
						_sb_s.lightBoxFontSizeSlider.simpleSlider("setValue", 100);
						_sb_s.lightBoxFontSizeSlider.on( "slider:ready slider:changed", function(e, data) {
									if (!_sb_s.lightBoxFontSizeSliderUpdt) {
										//log_methods.log("NEW slider data: " + data.value);
										_sb_s.lightBoxFontSizeSliderData = data.value;
										if (Math.abs(_sb_s.lightBoxFontSizeSliderData - _sb_s.lightBoxFontSize) >= 10) {	// do not run on any value
											_sb_s.lightBoxFontSize = _sb_s.lightBoxFontSizeSliderData;
											newLightBoxFontSize(_sb_s.lightBoxFontSize, false);
										}
									}
									else setTimeout("_sb_s.lightBoxFontSizeSliderUpdt = false;",100);	// was just a request to set slider knob position
								} );
						$('#lightbox_fontsize_slider_sma').on(_sb_s.pointerEvents.start, function(e) {
																		_sb_s.lightBoxFontSize = _sb_s.lightBoxMinFontSize;
																		newLightBoxFontSize(_sb_s.lightBoxFontSize, true);
																		return(false);
																	});
						$('#lightbox_fontsize_slider_lga').on(_sb_s.pointerEvents.start, function(e) {
																		_sb_s.lightBoxFontSize = _sb_s.lightBoxMaxFontSize;
																		newLightBoxFontSize(_sb_s.lightBoxFontSize, true);
																		return(false);
																	});
						$('.dragger').dblclick(function(e) {
																		_sb_s.lightBoxFontSize = 100;
																		newLightBoxFontSize(_sb_s.lightBoxFontSize, true);
																		return(false);
																	});

						// Prevent default select behavior
						$('#lightbox_fontsize_slider_container').on(_sb_s.pointerEvents.start, function(e) { e.preventDefault(); return(false); });

					}

				}
			}
		},

		setLightboxContent = function(e,content, contenttype, articleID) {
			var detectLightBoxOverflow,
				cbidx, wdiv, links, hdlrStr, i, ii;
			/* internal function */
				function cb_lightBoxDragEnd(e,x,y) {
					_sb_s.overridelightboxLeft = x;
					_sb_s.overridelightboxTop = y;
				}

			if (_sb_s.preconvertButtonsLinks) {
				// turn link or button onclick event into TAP event
				wdiv = document.createElement("div");
				wdiv.innerHTML = content;
				// the links <a ....
				links = wdiv.getElementsByTagName("a");
				for (i = 0, ii = links.length; i < ii; i++) {
					hdlrStr = links[i].getAttribute("href");
					//log_methods.log("hdlrStr: " + hdlrStr);
					if ((hdlrStr != null) && (hdlrStr != "") && (hdlrStr.indexOf("javascript:goto_page") > -1)) {
						hdlrStr = hdlrStr.replace("javascript:","");
						if (hdlrStr[hdlrStr.length-1] == ";") hdlrStr = hdlrStr.substr(0,hdlrStr.length-1);
						//log_methods.log("hdlrStr: " + hdlrStr);
						// expand the goto_page call
						if (hdlrStr.indexOf(",false\)") > 0) {
							// should call with param 'detectSwipe' = true
							//		var goto_page = function (the_page, isPageIndex, duration, easing, setThumbsScroll,theevent,caller,force,detectSwipe) {
							hdlrStr = hdlrStr.replace(",false\)", ",false,null,null,null,null,null,null,true\)");
						}
						links[i].setAttribute("href","javascript:void(0)");
						if (_sb_s.isTouchDevice) links[i].setAttribute("on" + _sb_s.pointerEvents.end,"return(" + hdlrStr + ")");
						else links[i].setAttribute("onclick","return(" + hdlrStr + ")");
					}
				}

				// the images to enlarge <img ....
				links = wdiv.getElementsByTagName("img");
				for (i = 0, ii = links.length; i < ii; i++) {
					hdlrStr = links[i].getAttribute("onclick");
					//log_methods.log("hdlrStr: " + hdlrStr);
					if ((hdlrStr != null) && (hdlrStr != "") && (hdlrStr.indexOf("javascript:showImage") > -1)) {
						hdlrStr = hdlrStr.replace("javascript:","");
						if (hdlrStr[hdlrStr.length-1] == ";") hdlrStr = hdlrStr.substr(0,hdlrStr.length-1);
						//log_methods.log("hdlrStr: " + hdlrStr);
						if (_sb_s.isTouchDevice) links[i].setAttribute("on" + _sb_s.pointerEvents.end,"return(" + hdlrStr + ")");
						else links[i].setAttribute("onclick","return(" + hdlrStr + ")");
					}
				}

				// the buttons <button ....
				links = wdiv.getElementsByTagName("button");
				for (i = 0, ii = links.length; i < ii; i++) {
					// handle movies
					hdlrStr = links[i].getAttribute("onClick");
					//log_methods.log("hdlrStr: " + hdlrStr);
					if ((hdlrStr != null) && (hdlrStr != "")) {
						if (hdlrStr.indexOf("playmovie_") > -1) {
							if (hdlrStr[hdlrStr.length-1] == ";") hdlrStr = hdlrStr.substr(0,hdlrStr.length-1);
							//log_methods.log("hdlrStr: " + hdlrStr);
							if (true || _sb_s.isTouchDevice) {
								links[i].removeAttribute("onclick");
								links[i].setAttribute("on" + _sb_s.pointerEvents.end, hdlrStr);
							}
							//else links[i].setAttribute("onclick","return(" + hdlrStr + ")");
						}
					}

					// handle sound
					hdlrStr = links[i].getAttribute("onMouseOver");
					//log_methods.log("hdlrStr: " + hdlrStr);
					if ((hdlrStr != null) && (hdlrStr != "")) {
						if (hdlrStr.indexOf("playsnd_") > -1) {
							if (hdlrStr[hdlrStr.length-1] == ";") hdlrStr = hdlrStr.substr(0,hdlrStr.length-1);
							//log_methods.log("hdlrStr: " + hdlrStr);
							if (_sb_s.isTouchDevice || !_sb_s.doArticleShade) {	// if no canvas shading
								links[i].removeAttribute("onMouseOver");
								links[i].setAttribute("on" + _sb_s.pointerEvents.start, hdlrStr);
							}
							//else links[i].setAttribute("onclick","return(" + hdlrStr + ")");
						}
					}
				}
				content = wdiv.innerHTML;
				wdiv = null;
			}

			_sb_e.lightbox_div.css('fontSize', _sb_s.fontsizeBase+'pt');
			if (_sb_s.noArticleScrollBars) _sb_e.lightbox_div.css('overflow', 'hidden');	// hide scrollbars on normal systems with touch screens. 

			_sb_e.lightbox_div.scrollTop(0);
			// allow us to do some custom work on content before showing lightbox
			// call registered CBs to modify the lightbox text content
			for (cbidx = 0; cbidx < before_showLightboxContent_custom_CBs.length; cbidx++) {
				content = before_showLightboxContent_custom_CBs[cbidx](_sb_e.lightbox_divEl, content, _sb_e.lightbox_contentEl, _sb_e.lightboxEl, articleID);	// pass ref to lightbox and content
			}
			
			// set the lightbox content
			_sb_e.lightbox_div.html(content);

			if (articleID && (articleID !="")) _sb_e.lightbox_div.attr('title', articleID);

			// test if this is the TOC
			if (content.indexOf("data-toc=") >= 0) {
				_sb_s.TOC_isShown = true;
			}
			else {
				if (content.search(/data-label=.*\*TOC\*.*>/g) >= 0) {
					_sb_s.TOC_isShown = true;
				}
			}
			
			// adjust display font size
			if ((_sb_s.enable_lightBoxFontSize > 0) && (_sb_s.lightBoxFontSize != 100)) newLightBoxFontSize(_sb_s.lightBoxFontSize,true);

			/* make the lightbox draggable */
			if (_sb_s.enableDraggableObjects > 0) {
				if (_sb_s.lightBoxDragHandler == null) {
					_sb_s.lightBoxDragHandler = new XdraggableObject(document.getElementById("lightbox_head"),_sb_e.lightboxEl,[_sb_s.closebuttonWidth,0],null,null,cb_lightBoxDragEnd);
				}
			}
			// detect overflow to set scroll buttons
			detectLightBoxOverflow = function(e) {
				if (_sb_s.useArticleScrollButtons > 0) {
					var lightBoxOverflowPX = _sb_e.lightbox_div.prop("scrollHeight") - _sb_e.lightbox_div.height();
					if (lightBoxOverflowPX > 0) {
						if (_sb_s.useArticleScrollButtons == 2) _sb_e.lightbox_div.css('height',_sb_e.lightbox_div.css('height') - artScrollUpButtonHeight - artScrollDownButtonHeight +"px");
						nScrollUpButton.removeClass('hidden'); nScrollUpButton.addClass('lightbox_scrollUpButton');
						nScrollDownButton.removeClass('hidden'); nScrollDownButton.addClass('lightbox_scrollDownButton');
						nScrollUpButton.on(_sb_s.pointerEvents.start, scrollUpButtonHandler);
						nScrollUpButton.on(_sb_s.pointerEvents.end, function(e) { lightboxButtonScrolling = false; });
						nScrollDownButton.on(_sb_s.pointerEvents.start, scrollDownButtonHandler);
						nScrollDownButton.on(end, function(e) { lightboxButtonScrolling = false; });
					}
				}
			}
			if (_sb_s.useArticleScrollButtons > 0) setTimeout(function(){detectLightBoxOverflow(e);},_sb_s.detectLightBoxOverflowTimeout);

		},

		fadeLightboxAnim = function (settings) {
			_sb_e.lightbox_div.animate(
				settings,
				300,
				function() {	// animation end: lightbox is now open
							// fade in the close button
							_sb_e.lightbox_close.removeClass('hidden');
							if (_sb_s.lightboxFadeCloseButton == true) {
								fadeInClosebuttonAnim();
							}
							else {	// turn closebutton on
								_sb_e.lightbox_close.css("visibility", "visible");
							}

							// fade in the return to search results lupe
							if ((_sb_s.lastSearchResults != "") && (_sb_s.clickCameFromSearchResults == true)) {
								_sb_e.lightbox_returnsearch.removeClass('hidden');
								if (_sb_s.lightboxFadeSearchButton == true) {
									var return2toSearchWidth = -(parseInt(_sb_e.lightbox_returnsearch.css("width"), 10) - 16);
									fadeInReturnSearchbuttonAnim.set('to', { left: return2toSearchWidth });
									fadeInReturnSearchbuttonAnim.run();
								}
								else {	// turn  search button on
									_sb_e.lightbox_returnsearch.css("visibility", "visible");
								}
							}
							if (_sb_s.clickCameFromSearchResults == false) {
								if (_sb_s.lightboxFadeSearchButton == true) {
									fadeOutReturnSearchbuttonAnim.set('to', { left: _sb_s.searchbuttonLeft });
									fadeOutReturnSearchbuttonAnim.run();
								}
								else {	// turn  search button off
									_sb_e.lightbox_returnsearch.css("visibility", "hidden");
								}
							}
							_sb_s.clickCameFromSearchResults = false;

							// set the text content
							setLightboxContent(e,content, contenttype, articleID);
							
							// enable scrollable lightbox div
							if ((_sb_s.enableJStouchscroll > 0) && (_sb_s.isTouchDevice || _sb_s.hasTouchScreen)) {
								do {
									if ( ((_sb_s.enableJStouchscroll & 2) > 0) && (_sb_s.is_iPad || _sb_s.is_iPhone)) break;	// they support scrolling divs through css
									//alert(_sb_s.AndroidVersion+"\nv: "+_sb_s.isTouchDevice);
									if (_sb_s.is_Android && (_sb_s.AndroidVersion >= 4)) break;	// support native scroll
									if ( ((_sb_s.enableJStouchscroll & 4) > 0) && (_sb_s.is_IE && _sb_s.is_MobileOS)) {
										if (_sb_s.ms10OverflowStyle != "") {	// set scroll style for IE 10 mobile
											var st = _sb_e.lightbox_div.attr("style") || "";
											if (st.indexOf(_sb_s.ms10OverflowStyle) < 0) {
												st += _sb_s.ms10OverflowStyle;			// add css like: -ms-overflow-style:scrollbar;
												_sb_e.lightbox_div.attr("style", st);
											}
										}
										break;
									}

									if (_sb_s.lightBoxTouchScrollHandler == null) {
										_sb_s.lightBoxTouchScrollHandler = new touchScroll("lightbox_div",false);
									}
									//else _sb_s.lightBoxTouchScrollHandler.touchScroll("lightbox_div",false);
								} while(false);
							}

							_sb_s.lightboxIsFading = false;


							// call registered CBs when lightbox is shown
							for (var cbidx = 0; cbidx < after_showLightboxContent_custom_CBs.length; cbidx++) {
								after_showLightboxContent_custom_CBs[cbidx](_sb_e.lightbox_divEl, content, _sb_e.lightbox_contentEl, _sb_e.lightboxEl, articleID);	// pass ref to lightbox and content
							}

				});
			};            

		//recalc always because device orientation could have changed
		setLightboxHeader();
		if (_sb_s.inhibit_calcLightboxSizes == false) {
			calcLightboxSizes();
		}
		else _sb_s.inhibit_calcLightboxSizes = false;	// reset

		if (nScrollUpButton) { nScrollUpButton.removeClass('lightbox_scrollUpButton'); nScrollUpButton.addClass('hidden'); }
		if (nScrollDownButton) { nScrollDownButton.removeClass('lightbox_scrollDownButton'); nScrollDownButton.addClass('hidden'); }

		// allow us to do some custom work before showing lightbox
		// call registered CBs to modify the lightbox header
		for (var cbidx = 0; cbidx < before_showLightbox_custom_CBs.length; cbidx++) {
			var retval = before_showLightbox_custom_CBs[cbidx](_sb_e.lightboxEl, _sb_e.lightbox_contentEl);	// pass ref to lightbox
			if (retval === false) return;
		}


		// fade in lightbox
		_sb_e.lightbox.removeClass('hidden');
		_sb_e.lightbox_div.removeClass('hidden');
		fadeLightboxAnim({ opacity: 1, height: _sb_s.lightboxHeight });

		// track all pointer events on lightbox_div
		_sb_fn.addEventHandler(_sb_e.lightbox_divEl,_sb_s.pointerEvents.start + " " + _sb_s.pointerEvents.end + " " + _sb_s.pointerEvents.move + " " + _sb_s.pointerEvents.out + " " + _sb_s.pointerEvents.cancel, _sb_fn.touchDetector,false);

		// fade in body overlay
		// calc overlay dimensions
		var ovlDims = calcOverlaySizes();
		_sb_e.lightbox_overlay.removeClass("hidden");
		//alert("Overlay width: " + ovlWidth + ", height: " + ovlHeight);
		fadeOverlayAnim({ opacity: 0.3, width: ovlDims[0], height: ovlDims[1] },
							0,
							function() {	// animation end: lightbox is now open
							}
							);
		// make sure the click handler is not already attached
		_sb_e.html.off("click");
		_sb_s.htmlCloseLightboxClickHandler = _sb_e.html.on(_sb_s.pointerEvents.start, function(e) {
														try {
															if (!e || !e.target || !e.target.nodename) return false;
															//log_methods.log("htmlCloseLightboxClickHandler: " + e.target.nodeName+", target: " + e.target+", currentTarget: " + e.currentTarget);
															if (e.target.nodeName.toUpperCase() == 'AREA') { _sb_fn.haltEvents(e); };
															if (e.target.nodeName.toUpperCase() == 'HTML') lightboxClickHandler(e);
														} catch(x) { log_methods.log("htmlCloseLightboxClickHandler EXEPTION: " + x.message); }
														return false;
														});

		var lightboxClickHandler = function(e) {
			//log_methods.log("lightboxClickHandler target: " + e.target + ", currentTarget: " + e.currentTarget);
			e.preventDefault(); e.stopPropagation();
			hideLightbox(e);
			if (_sb_s.showImageHandle_1 != null) _sb_s.showImageHandle_1.showImage_lightbox_fadeout();
			return false;
		};
		// attach event handlers
		_sb_e.lightbox_close.off(_sb_s.pointerEvents.end); _sb_e.lightbox_close.on(_sb_s.pointerEvents.end, lightboxClickHandler);
		_sb_e.lightbox_overlay.off(_sb_s.pointerEvents.end); _sb_e.lightbox_overlay.on(_sb_s.pointerEvents.end, lightboxClickHandler);
		_sb_e.lightbox_returnsearch.off(_sb_s.pointerEvents.end); _sb_e.lightbox_returnsearch.on(_sb_s.pointerEvents.end, showLastSearchResults);

		var lightboxPointerHandler = function(e) {
			//log_methods.log("lightboxPointerHandler\n\te.type: " + e.type + "\n\te.target: " + e.target.className);
			switch (e.type) {
				case _sb_s.pointerEvents.start:
					_sb_s.lb_ptr_evt.start = new Date().getTime();
					_sb_s.lb_ptr_evt.end = null;
					_sb_s.lb_ptr_evt.startXY = _sb_fn.get_pointerXY(e);
					_sb_s.lb_ptr_evt.endXY.x = _sb_s.lb_ptr_evt.endXY.y = null;
					_sb_s.lb_ptr_evt.isMove = false;
					_sb_s.lb_ptr_evt.deltaXY.x = _sb_s.lb_ptr_evt.deltaXY.y = 0;
					break;
				case _sb_s.pointerEvents.move:
					if (_sb_s.lb_ptr_evt.start === null) break;
					_sb_s.lb_ptr_evt.end = new Date().getTime();
					_sb_s.lb_ptr_evt.endXY = _sb_fn.get_pointerXY(e);
					if (_sb_s.lb_ptr_evt.endXY == null)break;
					_sb_s.lb_ptr_evt.deltaXY.x = _sb_s.lb_ptr_evt.endXY.x - _sb_s.lb_ptr_evt.startXY.x;
					_sb_s.lb_ptr_evt.deltaXY.y = _sb_s.lb_ptr_evt.endXY.y - _sb_s.lb_ptr_evt.startXY.y;
					if (   (Math.abs(_sb_s.lb_ptr_evt.deltaXY.x) > _sb_s.lb_ptr_evt.moveTrigger)
						|| (Math.abs(_sb_s.lb_ptr_evt.deltaXY.y) > _sb_s.lb_ptr_evt.moveTrigger)
						) _sb_s.lb_ptr_evt.isMove = true;
					else _sb_s.lb_ptr_evt.isMove = false;
					//log_methods.log("lightboxPointerHandler\n\te.type: " + e.type + "\n\te.target: " + e.target.className + "\n\tisMove: " + _sb_s.lb_ptr_evt.isMove);
					break;
				case _sb_s.pointerEvents.end:
					_sb_s.lb_ptr_evt.start = _sb_s.lb_ptr_evt.end = null;
					break;
			}

			if (e && e.stopPropagation) e.stopPropagation();
			return(true); 	// stop propagation to allow touch scroll and allow text selection by returning true
		};

		//_sb_e.lightbox_div.on(_sb_s.pointerEvents.start,function(e){ if (e && e.stopPropagation) e.stopPropagation(); return(true); });	// stop propagation but allow text selection
		_sb_e.lightbox_div.on(_sb_s.pointerEvents.start,function(e){return lightboxPointerHandler(e);});
		_sb_e.lightbox_div.on(_sb_s.pointerEvents.move,function(e){return lightboxPointerHandler(e);});
		_sb_e.lightbox_div.on(_sb_s.pointerEvents.end,function(e){return lightboxPointerHandler(e);});


		// evtl, highlight article on page
		if (_sb_s.doArticleShade && articleID && (_sb_s.highlightArticleOnPage == true)) {
			setTimeout(function(){highlightArticleOnPage(articleID,0);},1000);
		}
		
	};
	_sb_fn.showLightbox = showLightbox;	// make globally available

	var showLastSearchResults = function () {
		if (_sb_s.lastSearchResults == "") return;
		hideReturnSearchButton();
		_sb_e.lightbox_div.html(_sb_s.lastSearchResults);
	},

	closeDownLightbox = function() {
			_sb_e.lightbox_close.addClass('hidden');
			_sb_e.lightbox_returnsearch.addClass('hidden');
			// now clear content
			if (_sb_s.lightBoxArticleCommentSys != null) _sb_s.lightBoxArticleCommentSys = null;
			var lbheadcnt = $('#lightbox_head_container');
			if (lbheadcnt != null) lbheadcnt.remove();
			if (nScrollUpButton) { nScrollUpButton.removeClass('lightbox_scrollUpButton'); nScrollUpButton.addClass('hidden'); }
			if (nScrollDownButton) { nScrollDownButton.removeClass('lightbox_scrollDownButton'); nScrollDownButton.addClass('hidden'); }
			fadeOutLightboxAnim();
	},
	hideCloseButton = function() {
		if (_sb_s.lightboxFadeCloseButton == true) {
			fadeOutClosebuttonAnim.run();
			// when buttons are closed hide text
			fadeOutClosebuttonAnim.once('end', function() {
				closeDownLightbox();
			});
		}
		else {	// turn closebutton off
			closeDownLightbox();
		}
	},
	hideReturnSearchButton = function() {
		if (_sb_s.lightboxFadeSearchButton == true) {
			fadeOutReturnSearchbuttonAnim.run();
		}
		else {	// turn search button off
			_sb_e.lightbox_returnsearch.css("visibility", "hidden");
		}
	};

	/**
	 * Hide the lightbox
	 * call like:
	 *		hideLightbox(true);
	 *
	 * optional parameters;
	 * @param {Object|null=} e if available, the event object
	 * @param {boolean|null=} closeImageLightbox true to also close the image lightbox
	 */
	var hideLightbox = function(e,closeImageLightbox) {
		var videos, vi;
		//alert("Hiding Lightbox");
		_sb_s.lightboxContentWidth = _sb_s.lightboxContentHeight = 0;	/* reset needed content dimensions */
		_sb_s.lightboxContentDimIDs = "";
		if (_sb_s.lightboxIsShowing == false) return;
		if (closeImageLightbox && (_sb_s.showImageHandle_1 != null)) _sb_s.showImageHandle_1.showImage_lightbox_fadeout();
		if (_sb_s.lightboxIsFading == true) return;	// semaphore to prevent multiple calls
		_sb_s.lightboxIsFading = true;
		_sb_s.lightboxIsShowing = false;
		setTimeout("_sb_s.lightboxIsFading = false;",2000);	// make sure we will never get blocked

		_sb_s.TOC_isShown = false;	// closing
		try { _sb_s.htmlCloseLightboxClickHandler.off(); } catch(ex) {}
		// remove drag handler event listeners BEFORE clearing content
		if (_sb_s.lightBoxDragHandler != null) {
			try { _sb_s.lightBoxDragHandler.removeDraggable("hideLightbox"); } catch(ex){}
			_sb_s.lightBoxDragHandler = null;
		}
		// remove touch scroll handler
		//alert("hideLightbox: " + _sb_s.lightBoxTouchScrollHandler.detach);
		if (_sb_s.lightBoxTouchScrollHandler != null) {
			_sb_s.lightBoxTouchScrollHandler.detach("lightbox_div");
			_sb_s.lightBoxTouchScrollHandler = null;
		}
		// remove touchDetector
		_sb_fn.removeEventHandler(_sb_e.lightbox_divEl,_sb_s.pointerEvents.start + " " + _sb_s.pointerEvents.end + " " + _sb_s.pointerEvents.move + " " + _sb_s.pointerEvents.out + " " + _sb_s.pointerEvents.cancel, _sb_fn.touchDetector,false);

		// clear content
		_sb_e.lightbox_div.html('');

		// fade away buttons
		hideCloseButton();
		hideReturnSearchButton();

		// fade away overlay
		fadeOverlayAnim({ opacity: 0, width: 0, height: 0 },
							200,
							function() {
								var cbidx;
								// call registered CBs when lightbox closing
								for (cbidx = 0; cbidx < after_hideLightbox_custom_CBs.length; cbidx++) {
									var retval = after_hideLightbox_custom_CBs[cbidx](_sb_e.lightboxEl, _sb_e.lightbox_contentEl);	// pass ref to lightbox
									if (retval === false) return;
								}
								_sb_e.lightbox_overlay.off();	// detach all events also from childs
								_sb_e.lightbox_overlay.addClass("hidden");
							});

		if (_sb_s.canvas_available) setTimeout("_sb_fn.clear_all_shadows(0)",100);
	};

	var newLightBoxFontSize_timeoutID = null;
	/**
	 * Set the fontsize of text in lightbox
	 *
	 * @param {number} newFSscale the font scale factor
	 * optional parameters;
	 * @param {boolean|null=} updateslider update the slider
	 * @param {Object|null=} thenode the node to process, otherwise all
	 * @param {boolean|null=} is_recall true if already call as task
	 */
	var newLightBoxFontSize = function(newFSscale, updateslider, thenode, is_recall) {
		if ((typeof(is_recall) == 'undefined') || (is_recall != true)) {	// make sure we are not called in to fast intervals
			if (newLightBoxFontSize_timeoutID != null) { clearTimeout(newLightBoxFontSize_timeoutID); newLightBoxFontSize_timeoutID = null; }
			newLightBoxFontSize_timeoutID = setTimeout(function(){newLightBoxFontSize(newFSscale, updateslider, thenode, true);},150);
			return;
		}
		if (newLightBoxFontSize_timeoutID != null) { clearTimeout(newLightBoxFontSize_timeoutID); newLightBoxFontSize_timeoutID = null; }


		if (_sb_s.enable_lightBoxFontSize <= 0) return;
		if (_sb_s.lightboxIsShowing == false) return;
		//log_methods.log("%%%%%%%%%%%% newFSscale: "+newFSscale);
		var numloops = 0, numFSdone = 0, numTDdone = 0,
			lb_divs, aNode, anode_id, anode_class,
			curFS, newFS, origFSdata, origFSmeasure, curW, origW, curstyle,
			mynode = _sb_e.lightbox_div;
		if (thenode) mynode = thenode;
		//lb_divs = mynode.find("div,span,a,table td,table td div,table td span,table td a");
		lb_divs = mynode.find("div,span,a,td,select,button");
			//log_methods.log("num lb_divs: " + lb_divs.size());
		lb_divs.each(function(nidx) {
			// for tests only:    if(nidx > 3) return;
			aNode = this;
			numloops++;
			anode_id = aNode.getAttribute("id");
			anode_class = aNode.getAttribute("class");

			if (anode_class) {
				if (anode_class.indexOf("goto_article_nav_") >= 0) return;	// skip fontsize slider
				if (anode_class == "Artcl_container") return;	// skip article containers
				if (anode_class.indexOf("A_anchored_object__") >= 0) return;	// skip anchored object container
			}
			//log_methods.log("**** aNode #" + nidx + " tagName: " + aNode.tagName +"\n\tanode_id: " + anode_id +"\n\tanode_class: " + anode_class);
			if (newFSscale != 100) {
				if (newFSscale < 0) newFSscale = 100;	// -1 means reset to original
				if (aNode.tagName.toUpperCase() == "TD") {	// resize table cells
					numTDdone++;
					// get current cell width
					curW = aNode.style.width;
					// store original style width aside
					origW = aNode.getAttribute("data-Worig");
					if (origW == "") {
						aNode.getAttribute("data-Worig",curW);
						aNode.style.width = "auto";
					}
					//log_methods.log("****** aNode #" + nidx + " tagName: " + aNode.tagName +"\n\tanode_id: " + anode_id +"\n\tanode_class: " + anode_class +"\n\tcurW: " + curW +"\n\torigW: " + origW);
				}
				// set fonsizes
				numFSdone++;
				// store original style font-size aside
				origFSdata = aNode.getAttribute("data-FSorig");
				//log_methods.log("--- origFSdata: " + origFSdata);
				if (origFSdata === null) {		// originally set font-size not stored
					// get current font size
					curFS = getStyle(aNode,"font-size",1,false);			// get from class or style
					//log_methods.log("..... getting font-size from class or style: " + curFS);
					// do not get computed for all elements - get for containers only! if element has no font-size, it will inherit it
					if ( (curFS == null) && (anode_id == "ft_result") ) {
						//log_methods.log("..... getting font-size computed");
						curFS = getStyle(aNode,"font-size",1,false);	// get computed
					}
					
					if (curFS == null) {
						//log_methods.log("----- skipping: inherited font-size");
						return;
					}
					aNode.setAttribute("data-FSorig",curFS);
					//log_methods.log("..... data-FSorig set to: \"" + curFS + "\"");
					curstyle = aNode.getAttribute("style");
					if (curstyle && curstyle.toUpperCase().indexOf("font-size") >-1) aNode.setAttribute("data-FSreset",curFS);
					else aNode.setAttribute("data-FSreset","");
				}
				else curFS = origFSdata;	// calculate from original font size
					//log_methods.log("===== origFSdata: " + origFSdata + "\n\tcurFS: " + curFS);

				// get measure units
				origFSmeasure = "";
				for (var m=curFS.length; m>=0; m--) {
					if (isNaN(curFS[m])) origFSmeasure = curFS.substr(m);
					else break;
				}
				// set new font-size
				newFS = Math.floor(parseFloat(curFS)*newFSscale)/100 + origFSmeasure;
					//log_methods.log("+++++ New Size factor: " + newFSscale + "\n\torigFSdata: " + origFSdata + "\n\tcurFS: " + curFS + "\n\tnewFS: " + newFS);
				aNode.style.fontSize = newFS;
				//document.getElementById('debugmessage').innerHTML += "<br>origFSdata: "+aNode.getData("FSorig")+" - newLightBoxFontSize: "+newFS;
			}
			else {	// restore to original
				if (aNode.tagName.toUpperCase() == "TD") {	// reset table cells
					numTDdone++;
					origFSdata = aNode.getAttribute("data-FSreset");
					if(origFSdata != null) aNode.style.fontSize = origFSdata;
					origW = aNode.getAttribute("data-Worig");
					if (origW != "") {
						aNode.style.width = origW;
					}
				}
				else {
					// restore originally set font-size
					numFSdone++;
					origFSdata = aNode.getAttribute("data-FSreset");
					if(origFSdata == null) return;	// was not set: was inherited
					//log_methods.log("###### reset to attr value of data-FSreset: " + origFSdata);
					aNode.style.fontSize = origFSdata;
				}
			}
			});
		//log("num lb_divs done: " + numloops + "\n\tnumTDdone: " + numTDdone + "\n\tnumFSdone: " + numFSdone);
			
		//document.getElementById('debugmessage').innerHTML += "<br>update FS slider: "+updateslider+ " to value: "+_sb_s.lightBoxFontSize+" - "+new Date().getTime();
		if ((updateslider === true) && (_sb_s.lightBoxFontSizeSlider != null)) {
			_sb_s.lightBoxFontSizeSliderUpdt = true;	// indicate just a request to set slider knob position
			_sb_s.lightBoxFontSizeSlider.simpleSlider("setValue", _sb_s.lightBoxFontSize);
		}
		return;
	};
	_sb_fn.hideLightbox = closeLightbox = hideLightbox;



	_sb_s.printLightbox_IFrame = null;
	var hide_print_IFrame = function () {
		/* DO NOT CLEAR CONTENT!!! Printing runs asynch and content must be visible until print is complete
		var print_IFrame_doc = (_sb_s.printLightbox_IFrame.contentDocument || _sb_s.printLightbox_IFrame.contentWindow.document);
		print_IFrame_doc.open('text/html', 'replace');
		print_IFrame_doc.write("");
		print_IFrame_doc.close();
		*/
		_sb_s.printLightbox_IFrame.setAttribute("style","display:block; position:absolute; left:-10000px; top:-10000px;");
	},
	printLightbox = function (){
		var view_X=10, view_Y=10, wid=400, hig=400, is_multipart_story = false, full_storyHTML = "",
			stylesheetLink = "", ft_help_idattr, nav_help_idattr, print_fullarticle, print_LBcontent, div_elements,
			print_IFrame_doc, print_Div = null,
			print_head = "", print_content = "", print_trail = "",	// the entire content to print
			cbidx,
			basepath = "",
			xslcsspath = _sb_s.xslcssPath,// THIS IS A RELATIVE PATH
			csspath = document.getElementById("doctypeinfos").innerHTML,
			img_elements,
			printContainerVisible = "display:block; background-color:white; width:"+wid+"px; height:"+hig+"px; position:absolute; top:"+view_X+"px; left:"+view_Y+"px; z-index:99999;",
			show_print_IFrame = function () {
					//alert("show");
				_sb_s.printLightbox_IFrame.setAttribute("style",printContainerVisible);
			};

		basepath = fb_flipbookWinLocation.substr(0,fb_flipbookWinLocation.lastIndexOf("/")+1);
		// to simulate print in normal browser:	_sb_s.inApp = true;
		if (_sb_s.inApp) {
			//alert("basepath: " + basepath + "\n\nxslcsspath: " + xslcsspath + "\n\ncsspath: " + csspath);
			xslcsspath = resolvePath(basepath,xslcsspath);
			csspath = resolvePath(basepath,csspath);
			//alert("xslcsspath: " + xslcsspath + "\n\ncsspath: " + csspath);
		}

		// check if we want print full article (over multiple pages) or the part in the lightbox only
		print_fullarticle = (_sb_s.print_entireArticle > 0);
		try { if (mEvent.shiftKey) print_fullarticle = !print_fullarticle; }catch(err){}	// invert this behavior if shift key is pressed
		//log("mEvent.shiftKey: " + mEvent.shiftKey + "\n\tprint_fullarticle: " + print_fullarticle);

		print_LBcontent = _sb_e.lightbox_div.html();
		// we have to clean it
		var reg = /onload=\"(.)*?\"/gi; print_LBcontent = print_LBcontent.replace(reg,"");
		reg = /onclick=\"(.)*?\"/gi; print_LBcontent = print_LBcontent.replace(reg,"");
	
		ft_help_idattr= 'id="ft_help_container"';
		nav_help_idattr= 'id="nav_help_container"';
		if (_sb_s.is_IE && (_sb_s.IEVersion <= 8)) {
			ft_help_idattr= 'id=ft_help_container';
			nav_help_idattr= 'id=nav_help_container';
		}
		if (print_LBcontent.indexOf(ft_help_idattr) >= 0) {	// help text to be printed
			stylesheetLink = '<link rel="StyleSheet" href="' + xslcsspath + 'flipbook.css" type="text/css" media="all">\r';
		}
		else if (print_LBcontent.indexOf(nav_help_idattr) >= 0) {	// help text to be printed
				stylesheetLink = '<link rel="StyleSheet" href="' + xslcsspath + '_help/sb_navigation/website/css/nav_helpsb_alone.css" type="text/css" media="all">\r';
			 }
			 else {
				stylesheetLink = '<link rel="StyleSheet" href="' + csspath + '" type="text/css" media="all">\r';
			 }
		//alert(stylesheetLink);

		// the print header
		print_head = "<!DOCTYPE html>\r";
		print_head += "<html><head>\r";
		print_head += "<title>Print</title>\r";
		print_head += "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\r";
		print_head += stylesheetLink;
		if (!_sb_s.inApp) {
			print_head += "<script type=\"text/javascript\">\r";
			if (_sb_s.is_IE) print_head += "function printMe(){try{parent._sb_s.printLightbox_IFrame.focus();parent._sb_s.printLightbox_IFrame.contentWindow.document.execCommand('print',false,null);}catch(ex){alert('ERROR while printing:\\n' + ex.message);}setTimeout(function(){parent._sb_fn.hide_print_IFrame();},1000);}\r";
			else print_head += "function printMe(){try{parent._sb_s.printLightbox_IFrame.contentWindow.focus();parent._sb_s.printLightbox_IFrame.contentWindow.print();}catch(ex){alert('ERROR while printing:\\n' + ex.message);}setTimeout(function(){parent._sb_fn.hide_print_IFrame();},1000);}\r";
			//print_head += "//parent._sb_s.printLightbox_IFrame.contentWindow.onload=function(){printMe();};\r";
			print_head += "</script>\r";
		}
		print_head += "</head>\r<body>\r";


		// the print trailer
		if (!_sb_s.inApp) {
			print_trail += "\r<script>setTimeout(function(){printMe();},1000);</script>\r";
		}
		print_trail += "</body></html>";


		print_Div = document.createElement("div");	// create hidden
		print_Div.setAttribute("id","myArticlePrintWindow");
		print_Div.setAttribute("style","visibility:hidden;");
		print_Div.innerHTML = print_LBcontent;


		div_elements = print_Div.getElementsByTagName("div");	// get divs from this article(part)
		//alert("div_elements.length: " + div_elements.length);
		// check, if this is a part of a story only
		if (print_fullarticle) {
			do {
				var data_story_id = null, storydiv_elements, storydivsArr = new Array(), i, slidebookWin = null, slidebookWindowName = "BXSLTflipWin";
				// get the first Artcl_container from the article shown in lightbox
				for (i=0; i<div_elements.length;i++) {
					if (div_elements[i].getAttribute("class") == "Artcl_container") {		// check have continued article box(es)
						data_story_id = div_elements[i].getAttribute("data-story_id");	// yes, if has attr "data-story_id"
						if ((data_story_id == null) || (data_story_id == "")) continue;	// no, is not a splited story
						break;
					}
				}
				//alert("data_story_id : " + data_story_id +"\nthis.name: " + this.name);
				if (data_story_id == null) break;	// no story-id found

				// get main HTML to find all Artcl_container with data_story_id
				try {
					if (window && (window.name == slidebookWindowName) ) slidebookWin = window;
					else break;
				} catch(ex) { break; }	// could not access main window
				storydiv_elements = slidebookWin.document.getElementsByTagName("div");
				for (i=storydiv_elements.length-1; i>=0;i--) {
					if (storydiv_elements[i].getAttribute("id") == "status_message") break;		// do not check above this
					if (storydiv_elements[i].getAttribute("class") != "Artcl_container") continue;	// check Artcl_container only
					if (storydiv_elements[i].getAttribute("data-story_id") != data_story_id) continue;	// check has Artcl_container has data_story_id in question
					storydivsArr[storydivsArr.length] = i;
				}
				if (storydivsArr.length <= 1) break; 	// not a multi part story
				is_multipart_story = true;
				for (i = storydivsArr.length-1; i >= 0; i--) {
					full_storyHTML += storydiv_elements[storydivsArr[i]].innerHTML;
				}
				//log("num story parts: " + storydivsArr.length);
				// correct content of full story
				var re = /\<!--image/gi; full_storyHTML = full_storyHTML.replace(re,"<img");
				re = /\<!--nimage/gi; full_storyHTML = full_storyHTML.replace(re,"<img");
				re = /\/image--\>/gi; full_storyHTML = full_storyHTML.replace(re,">");
			} while(false);
		}
		// turn off certain elements
		if (!is_multipart_story) {
			div_elements = print_Div.getElementsByTagName("div");
			// turn off arrows
			for (var i=0; i<div_elements.length;i++) {
				if ((div_elements[i].getAttribute("class") == "goto_article_nav_top") 
					|| (div_elements[i].getAttribute("class") == "goto_article_nav_bot")
					|| (div_elements[i].getAttribute("id") == "lightbox_head")
					) div_elements[i].style.display = "none";
			}
		}
		// turn off image enlarge icons
		img_elements = print_Div.getElementsByTagName("img");
		if (_sb_s.inApp) {
			if (basepath.indexOf("file:/") != 0) {	// view a local book? must add full paths
				basepath = "";
			}
		}
		for (var i=0; i<img_elements.length;i++) {
			if ((img_elements[i].getAttribute("class") == "imageEnlargeIcon")) {
				img_elements[i].style.display = "none";
			}
			else if (basepath != "") {
				img_elements[i].setAttribute("src", basepath + img_elements[i].getAttribute("src"));
			}
		}


		// set print content
		print_content = print_head + print_Div.innerHTML + print_trail;
		//alert(print_content);
		
		print_Div = null;	// not used any longer: garbage


		// when not inApp, print, hide the print iframe is done by it self
		// otherwise, we send the content to print to the main App
		if (_sb_s.inApp && _sb_s.inAppPrintPolling) {
			// allow us to do some custom work before printing
			// call registered CBs
			for (cbidx = 0; cbidx < before_printLightbox_custom_CBs.length; cbidx++) {
				before_printLightbox_custom_CBs[cbidx](print_content);
			}
			//alert("printing content:\n" + print_head + print_content + print_trail);
			fb_poll_action_set("printhtm",encodeURI(print_content));
		}
		else {
			// print with iFrame

			if (!_sb_s.printLightbox_IFrame) {
				_sb_s.printLightbox_IFrame = document.createElement("iframe");	// create hidden
					_sb_s.printLightbox_IFrame.setAttribute("id","myArticlePrintWindow");
					_sb_s.printLightbox_IFrame.setAttribute("style","display:none;");
				_sb_e.bodyEl.appendChild(_sb_s.printLightbox_IFrame);
			}
			// get iframe's document
			print_IFrame_doc = (_sb_s.printLightbox_IFrame.contentDocument || _sb_s.printLightbox_IFrame.contentWindow.document);


			// allow us to do some custom work before printing
			// call registered CBs
			for (cbidx = 0; cbidx < before_printLightbox_custom_CBs.length; cbidx++) {
				before_printLightbox_custom_CBs[cbidx](_sb_s.printLightbox_IFrame, print_head + print_content + print_trail);
			}

			show_print_IFrame();
			//alert(_sb_s.printLightbox_IFrame.getAttribute("style"));

			print_IFrame_doc.open('text/html', 'replace');
			print_IFrame_doc.write(print_content);
			print_IFrame_doc.close();
		}
	};
	_sb_fn.printLightbox = printLightbox;
	_sb_fn.hide_print_IFrame = hide_print_IFrame;



	// ========================
	var mEvent = new Object();
		mEvent.event = null;
		mEvent.type="";			// last event type
		mEvent.timestamp=0;		// timestamp of mouse event
		mEvent.target=null;		// the element which the event takes place on
		mEvent.currentTarget=null;		// the element which the event takes place on
		mEvent.mx=0;			// current mouse position clientX
		mEvent.my=0;
		mEvent.pageX=0;			// current mouse position pageX
		mEvent.pageY=0;
		mEvent.button=-1;		// pressed mouse button
		mEvent.altKey=false;	// pressed modifyer keys
		mEvent.ctrlKey=false;
		mEvent.shiftKey=false;

		mEvent.currentGesture="";	// currently running gesture like "gesturemovestart" or "" for none
		mEvent.wheelDelta = 0;
	/* Android key codes:
	a - z: 29 - 54
	"0" - "9": 7 - 16
	BACK BUTTON - 4
	MENU BUTTON - 82
	UP, DOWN, LEFT, RIGHT: 19, 20, 21, 22
	SELECT (MIDDLE) BUTTON - 23
	SPACE: 62
	SHIFT: 59
	ENTER: 66
	BACKSPACE: 67 
	*/

	var keyEventHandler = function (evt){
		if (evt == null) return;
		try {
			var key= evt.keyCode || evt.which || evt.charCode;
			//log("key: "+key+" evt type: "+evt.type);
			switch (key) {
				case 16:	// SHIFT
					if (evt.type == "keydown") mEvent.shiftKey = true;
					else mEvent.shiftKey = false;
					break;
				case 17:	// CTRL
					if (evt.type == "keydown") mEvent.ctrlKey = true;
					else mEvent.ctrlKey = false;
					break;
				case 18:	// ALT
					if (evt.type == "keydown") mEvent.altKey = true;
					else mEvent.altKey = false;
					break;
			}
			//log_methods.log("Key: " + key);
		} catch(err){}
	}

	// attach key handler to close lightbox with ESC key and to flip pages
	var lightboxKeyHandler = function(e) {
		var ev = (e || window.event),
			key;
		try {
			key = ev.keyCode || ev.which || ev.charCode;
			//log_methods.log("Key: " + key+ "\nev.type: "+ev.type);
			if (_sb_s.canvas_available) _sb_fn.clear_all_shadows(0);
			switch (key) {
				//case 16:	// SHIFT is handled at keyEventHandler
				//	break;
				case 27:	// ESC
					if (ev.type == "keydown") {
						hideLightbox();
						if (_sb_s.showImageHandle_1 != null) _sb_s.showImageHandle_1.showImage_lightbox_fadeout();
					}
					break;
				case 35:	// last page
					if (ev.type == "keydown") {
						hideLightbox();
						goto_page(_sb_s.totalPages-1,true,null,null,true,ev);
					}
					break;
				case 36:	// first page
					if (ev.type == "keydown") {
						hideLightbox();
						goto_page(0,true,null,null,true,ev);
					}
					break;
							// goto next page
				case 34:	// page down
				case 40:	// cursor down
					if (ev.type == "keydown") {
						hideLightbox();
						goto_page(-1,true,null,null,true,ev);
					}
					break;
							// goto previous page
				case 33:	// page up
				case 38:	// cursor up
					if (ev.type == "keydown") {
						hideLightbox();
						goto_page(-2,true,null,null,true,ev);
					}
					break;

				case 39:	// cursor right
					if (ev.type == "keydown") {
						if (_sb_s.lightboxIsShowing && _sb_s.allowArticleStepping) show_next_article(true);
						else {
							//alert("next page");
							goto_page(-1,true,null,null,true,ev);
						}
					}
					break;
				case 37:	// cursor left
					if (ev.type == "keydown") {
						if (_sb_s.lightboxIsShowing && _sb_s.allowArticleStepping) show_previous_article(true,mEvent.shiftKey);
						else {
							//alert("previous page");
							goto_page(-2,true,null,null,true,ev);
						}
					}
					break;

				case 107: // plus-key (numeric pad): make fonts larger
				case 187: // plus-key (numeric pad) for CHROME
					//log_methods.log("Key: " + key + "\nev.type: "+ev.type + "\nlightboxIsShowing: "+_sb_s.lightboxIsShowing + "\nlightBoxFontSize: "+_sb_s.lightBoxFontSize);
					if ((_sb_s.enable_lightBoxFontSize > 0) && _sb_s.lightboxIsShowing && (ev.type == "keydown") && (_sb_s.lightBoxFontSize < _sb_s.lightBoxMaxFontSize)) {
						_sb_s.lightBoxFontSize += (mEvent.shiftKey == false) ? _sb_s.lightBoxFontSizeStep : 2*_sb_s.lightBoxFontSizeStep;
						if (_sb_s.lightBoxFontSize > _sb_s.lightBoxMaxFontSize) _sb_s.lightBoxFontSize = _sb_s.lightBoxMaxFontSize;
						newLightBoxFontSize(_sb_s.lightBoxFontSize,true);
					}
					break;
				case 109: // minus-key (numeric pad): make fonts smaller
				case 189: // minus-key (numeric pad): for CHROME
					if ((_sb_s.enable_lightBoxFontSize > 0) && _sb_s.lightboxIsShowing && (ev.type == "keydown") && (_sb_s.lightBoxFontSize > _sb_s.lightBoxMinFontSize)) {
						_sb_s.lightBoxFontSize -= (mEvent.shiftKey == false) ? _sb_s.lightBoxFontSizeStep : 2*_sb_s.lightBoxFontSizeStep;
						if (_sb_s.lightBoxFontSize < _sb_s.lightBoxMinFontSize) _sb_s.lightBoxFontSize = _sb_s.lightBoxMinFontSize;
						newLightBoxFontSize(_sb_s.lightBoxFontSize,true);
					}
					break;
				case 96: // 0-key (numeric pad): remove font-size
					if ((_sb_s.enable_lightBoxFontSize > 0) && _sb_s.lightboxIsShowing && (ev.type == "keydown") && (_sb_s.lightBoxFontSize != 100)) {
						//log_methods.log("Key: " + key + "\nev.type: "+ev.type + "\nlightboxIsShowing: "+_sb_s.lightboxIsShowing + "\nlightBoxFontSize: "+_sb_s.lightBoxFontSize);
						newLightBoxFontSize(-1,true);	// reset to original
						_sb_s.lightBoxFontSize = 100;
					}
					break;
			}
		} catch(x){}
		keyEventHandler(ev);
	}
	if (typeof(document.onkeydown) != 'function') document.onkeydown = lightboxKeyHandler;
	if (typeof(document.onkeyup) != 'function') document.onkeyup = lightboxKeyHandler;



	/**
	 * add event handler
	 *
	 * @param {Object} elem either a single element or an array of elements
	 * @param {string} etype either a single event type as string  like "mousedown" or an array of types
	 * @param {Function} cb the function to call
     * optional parameters;
	 * @param {boolean=} capture for capture phase == true or bubbling phase = false. if not set = bubbeling phase
	 */
	var addEventHandler = function (elem,etype,cb,capture) {
		if (!elem || !etype || !cb) return;
		var etypes,
			docapture = (capture ? capture : false),	// if not given, set to bubbeling phase
			i;
		//log_methods.log("addEventHandler: " + etype + " - type: " + typeof(etype));
		switch (typeof(etype)) {
			case "string":
				//etypes = new Array(etype);
				etypes = etype.split(" ");
				break;
			case "object":	// is array
				etypes = etype;
				break;
			default: return;
		}

		if (elem.addEventListener) {	// W3C DOM
			for (i = 0; i < etypes.length; i++) {
				elem.addEventListener(etypes[i],cb,docapture);
			}
		}
		else if (elem.attachEvent) { // IE <= 8 DOM
				for (i = 0; i < etypes.length; i++) {
					elem.attachEvent("on"+etypes[i],cb);
				}
			} else {
					for (i = 0; i < etypes.length; i++) {
						elem.setAttribute(etypes[i],cb);
					}
				}
		return;
	};


	/**
	 * remove event handler
	 *
	 * @param {Object} elem either a single element or an array of elements
	 * @param {string} etype either a single event type as string  like "mousedown" or an array of types
	 * @param {Function} cb the function to call
     * optional parameters;
	 * @param {boolean=} capture for capture phase == true or bubbling phase = false. if not set = bubbeling phase
	 */
	var removeEventHandler = function (elem,etype,cb,capture) {
		if (!elem || !etype || !cb) return;
		var etypes,
			docapture = (capture ? capture : false),	// if not given, set to bubbeling phase
			i;
		//log_methods.log("removeEventHandler: " + etype + " - type: " + typeof(etype));
		switch (typeof(etype)) {
			case "string":
				//etypes = new Array(etype);
				etypes = etype.split(" ");
				break;
			case "object":	// is array
				etypes = etype;
				break;
			default: return;
		}

		if (elem.removeEventListener) {	// W3C DOM
			for (i = 0; i < etypes.length; i++) {
				elem.removeEventListener(etypes[i],cb,docapture);
			}
		}
		else if (elem.detachEvent) { // IE <= 8 DOM
				for (i = 0; i < etypes.length; i++) {
					elem.detachEvent("on"+etypes[i],cb);
				}
			} else {
					for (i = 0; i < etypes.length; i++) {
						elem.setAttribute(etypes[i],null);
					}
				}
		return;
	};
	// make these functions accessible globally
	_sb_fn.addEventHandler = addEventHandler;
	_sb_fn.removeEventHandler = removeEventHandler;


	var gestureDetectorObj_arr = [],	// array of attached gesture detors
	listAttachedGestureDetectors = function () {
		var gdlist = "", key;
		for (key in gestureDetectorObj_arr) {
			//alert(key);
			if (gdlist != "") gdlist += ",";
			gdlist += key;
		}
		return(gdlist);
	},
	preventDocumentScroll = function(e) {
	//return;
		var evt;
		if (!_sb_s.isTouchDevice) return;
		if (_sb_s.documentScrollPrevented == true) return;
		if (!e) evt = window.event;
		else evt = e;
		//log_methods.log("preventDocumentScroll");
		$(document).on(_sb_s.pointerEvents.move,function(evt){ evt.preventDefault(); return(true); });
		_sb_s.documentScrollPrevented = true;
	//	if (evt.preventDefault) evt.preventDefault();
	//	evt.returnValue = false;
		setTimeout(function(){_sb_s.documentScrollPrevented = false; $(document).off(_sb_s.pointerEvents.move);},2000);	// make sure it will be re-enabled
	},
	clearSelection = function() {
		try {
			var sel = window.getSelection ? window.getSelection() : document.selection;
			if (sel) {
				if (sel.removeAllRanges) sel.removeAllRanges();
				else if (sel.empty) sel.empty();
			}
		} catch(ex){}
	},
	
	gestureDetectorPointer = {
		touchstarted:false,		// true if pointer down
		isSwipe:false,			// true if swipe gesture
		swipeDirection:"",		// "" for no swipe, 'x' for x-direction, 'y' for y-direction
		_gmX:0, _gmY:0,			// new pointer X and Y 
		_gmXs:0, _gmYs:0,		// previous mouse X and Y
		_gdX:0, _gdY:0,			// mouse move delta
		winHandlers_attached:false,
		attachDetachWindowEvents: 1	// 0 = attach event handlers on start
									// 1 = attach on start and detach events on out and end
	};

	/**
	 * The gesture detector object
	 *
	 * @param {Object} elementNode The element to detect gestures
     * optional parameters;
	 * @param {Object=} gestureDelegate Optional: The element to which to delegate the gesture
	 * @param {string|null=} elementName Optional: the element's name
	 *
	 * @return {Object} this object
	 * @constructor
	 */
	var gestureDetectorObj = function (elementNode,gestureDelegate,elementName) {
		var inval = -999999, 
			elemNode = elementNode,
			_mDown = _sb_s.pointerEvents.start,
			_mMove =  _sb_s.pointerEvents.move,
			_mUp =  _sb_s.pointerEvents.end,
			_mOut =  _sb_s.pointerEvents.out;

		//log_methods.log("gestureDetectorObj ID: " + elemNode.attr("id"));
		gestureDetectorPointer._gmXs = gestureDetectorPointer._gmYs = inval;

		var gestureHandler = function (e, triggeredEvent) {
			var delta = false, doHaltEvents = false, doPreventDefault = false,
				eventType,
				isPrimary = true;
			//log_methods.log("gestureHandler\ntype: " + e.type + "\ntriggeredEvent: " + triggeredEvent);

			if ((typeof e.originalEvent.isPrimary) != 'undefined') {
				isPrimary = e.originalEvent.isPrimary;
				//log_methods.log("gestureHandler\ne.type: " + e.type + "\nisPrimary: " + isPrimary);
				if (!isPrimary) {
					gestureDetectorPointer.isSwipe = false;
					gestureDetectorPointer.swipeDirection = "";
					gestureDetectorPointer.touchstarted = false;
					return(true);
				}
			}

			var getMouseDelta = function(e,init) {
				var touches = null;
				if (e.touches) touches = e.touches;
				else if (e.originalEvent && e.originalEvent.touches) touches = e.originalEvent.touches;

				if (touches) {	// touch event
					if (touches.length > 1) {	// we accept one finger touch only
						gestureDetectorPointer.touchstarted = false;
						gestureDetectorPointer.isSwipe = false;
						return(false);
					}
					gestureDetectorPointer._gmX = touches[0].pageX;
					gestureDetectorPointer._gmY = touches[0].pageY;
					//log_methods.log("Touch _gmX: " + gestureDetectorPointer._gmX + ", _gmY: " + gestureDetectorPointer._gmY);
				}
				else {	// mouse or pointer event
					//log_methods.log("e.pageX: " + e.pageX + ", e.pageY: " + e.pageY);
					if (e.pageX) {
						gestureDetectorPointer._gmX = (e.pageX ? e.pageX : e.clientX + document.documentElement.scrollLeft);
						gestureDetectorPointer._gmY = (e.pageY ? e.pageY : e.clientY + document.documentElement.scrollTop);
					}
					else {
						gestureDetectorPointer._gmX = (e.originalEvent.pageX ? e.originalEvent.pageX : e.originalEvent.clientX + document.documentElement.scrollLeft);
						gestureDetectorPointer._gmY = (e.originalEvent.pageY ? e.originalEvent.pageY : e.originalEvent.clientY + document.documentElement.scrollTop);
					}
					//log_methods.log("Pointer _gmX: " + gestureDetectorPointer._gmX + ", _gmY: " + gestureDetectorPointer._gmY);
				}
				if (init) {
					gestureDetectorPointer._gdX = gestureDetectorPointer._gdY = 0;
					gestureDetectorPointer._gmXs = gestureDetectorPointer._gmX;
					gestureDetectorPointer._gmYs = gestureDetectorPointer._gmY;
					//log_methods.log("getMouseDelta init X Y: " + gestureDetectorPointer._gdX + ", " + gestureDetectorPointer._gdY);
					return([gestureDetectorPointer._gdX, gestureDetectorPointer._gdY]);
				}
				// calc delta
				if (gestureDetectorPointer._gmXs == inval) gestureDetectorPointer._gdX = 0;
				else gestureDetectorPointer._gdX = gestureDetectorPointer._gmX - gestureDetectorPointer._gmXs;
				if (gestureDetectorPointer._gmYs == inval) gestureDetectorPointer._gdY = 0;
				else gestureDetectorPointer._gdY = gestureDetectorPointer._gmY - gestureDetectorPointer._gmYs;
					//log_methods.log("getMouseDelta delta X Y: " + gestureDetectorPointer._gdX + ", " + gestureDetectorPointer._gdY);
				return([gestureDetectorPointer._gdX, gestureDetectorPointer._gdY]);
			};
			
			eventType = e.type;
			//log_methods.log("gestureHandler type: " + eventType + ", triggeredEvent: " + triggeredEvent + "\n\tpageIsScrolling: " + _sb_s.pageIsScrolling);
			if (triggeredEvent) eventType = triggeredEvent;
			switch (eventType) {
				case _sb_s.pointerEvents.start:
					//log_methods.log("gestureHandler\n" + eventType + " on: " + this.id + "\n\ttouchstarted: " + gestureDetectorPointer.touchstarted + "\n\tpageIsScrolling: " + _sb_s.pageIsScrolling + "\nevent: " + e.originalEvent.isPrimary);
					gestureDetectorPointer.isSwipe = false;
					gestureDetectorPointer.swipeDirection = "";
					doPreventDefault = false;
					clearSelection();
					if (_sb_s.pageIsScrolling) {
						//log_methods.log("gestureHandler " + eventType + " on:\n\t" + this.id + " pageIsScrolling");
						gestureDetectorPointer.touchstarted = false;
						return(true);
					}
					gestureDetectorPointer.touchstarted = true;
					//preventDocumentScroll(e);
					//log_methods.log("gestureHandler " + eventType + " on:\n\t" + this.id + " mouse X: " + gestureDetectorPointer._gmX);
					delta = getMouseDelta(e,true);
					//log_methods.log("gestureHandler " + eventType + "\n\ton: " + this.id + "\n\tmouse X: " +  gestureDetectorPointer._gmX + " Y: " +  gestureDetectorPointer._gmY + (delta !== false ? "\n\tdX: " + delta[0]+ ", dY: " + delta[1] : "") +"\n\t(delta === false): " + (delta === false));
					if (delta === false) return(true);	// multiple touches
					//if (_sb_s.canvas_available) _sb_fn.clear_all_shadows(0);
					attachWindowGestureDetectorListeners();
					/*
					if (_sb_s.pageTurnMode == 'turn') {
						if (!_sb_s.isTouchDevice) doPreventDefault = false;	// true would mean, that the page corners can not be folded any longer
						//e.preventDefault();
					}
					*/
					// do not halt events: flipMethods need thestart too
					//else doHaltEvents = true;	// prevent content marking
					//doHaltEvents = true;
					break;
				//case "touchmove": case "mousemove":
				case _sb_s.pointerEvents.move:
					//_sb_s.DEBUGmode = 2; log_methods.log("gestureHandler\n" + eventType + " on: " + this + "\n\ttouchstarted: " + gestureDetectorPointer.touchstarted + "\n\tpageIsScrolling: " + _sb_s.pageIsScrolling);
					preventDocumentScroll(e);	// DO NOT PREVENT OR IEMOBILE DOES NOT TURN PAGE!! don't know why
					if (gestureDetectorPointer.touchstarted === false) return(true);	// mousedown did not happen yet; swipe not initiated
					if (_sb_s.pageIsScrolling) {
						gestureDetectorPointer.touchstarted = false;
						return(true);
					}
					if (_g_turnjs_pageIsFolding === true) {
						//_sb_s.DEBUGmode = 2; log_methods.log("gestureHandler corner is folding\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding);
						gestureDetectorPointer.touchstarted = false;
					}
					if (e.preventDefault) e.preventDefault();
					delta = getMouseDelta(e,false);
					if (delta === false) return(true);
					if (Math.abs(delta[1]) > Math.abs(delta[0])) {
						if (Math.abs(delta[1]) > _sb_s.min_swipe) {
							gestureDetectorPointer.isSwipe = true;
							gestureDetectorPointer.swipeDirection = "y";	// swipe in y-direction
						}
						//log_methods.log("gestureHandler SWIPE " + eventType + "\n\ton: " + e.target.id + "\n\tdelta X: "+delta[0] +"\n\tdelta Y: "+delta[1] + "\n\tisSwipe: "+gestureDetectorPointer.isSwipe + "\n\tswipeDirection: "+gestureDetectorPointer.swipeDirection);
						return(false);	// y delta is larger than x delta: is swipe in y direction
					}
					if (Math.abs(delta[0]) < _sb_s.min_swipe) return(false);	// x-axis only
					gestureDetectorPointer.touchstarted = false;	// IMPORTANT! do not remove! block all further mousemoves until next mousedown
					gestureDetectorPointer.isSwipe = true;
					gestureDetectorPointer.swipeDirection = "x";
					//log_methods.log("gestureHandler SWIPE " + eventType + "\n\ton: " + e.target.id + "\n\tdelta XY : "+delta[0] +" x  "+delta[1] + "\n\tisSwipe: "+gestureDetectorPointer.isSwipe + "\n\tswipeDirection: "+gestureDetectorPointer.swipeDirection);
					switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
						default:
						case "turn":
						/* done in turnjs function page:
							if (_sb_settings.canvas_available) {		// this may trigger immediately the mouse over (floater) on a covered smaller area. 
								_sb_settings.pageIsScrolling = true;	// we have to set pageIsScrolling to true for floater doesn't shoot
								_sb_settings.funcs.clear_all_shadows(99);
							}
						*/
							if (delta[0] < 0) {
								//alert("next page: " + (_sb_s.currentPageIdx + 1) + " To pageimage: " + _sb_fn.getPageImage(_sb_s.currentPageIdx+1));
								if (_sb_s.currentPageIdx < (_sb_s.totalPages - 1)) {
									$.load_page("pv_P"+(_sb_s.currentPageIdx+2),_sb_s.currentPageIdx+1,_sb_fn.getPageImage(_sb_s.currentPageIdx+1),false);
									_sb_e.sb_pagelist.turn('next');
								}
							}
							else {
								//alert("prev page: " + (_sb_s.currentPageIdx -1) + " To pageimage: " + _sb_fn.getPageImage(_sb_s.currentPageIdx-1));
								if (_sb_s.currentPageIdx > 0) {
									 $.load_page("pv_P"+(_sb_s.currentPageIdx),_sb_s.currentPageIdx-1,_sb_fn.getPageImage(_sb_s.currentPageIdx-1),false);
									_sb_e.sb_pagelist.turn('previous');
								}
							}
							break;
						case "slide":
							if (delta[0] < 0) goto_page(-1,true,null,null,true,e);
							else goto_page(-2,true,null,null,true,e);
							break;
					}
					//setTimeout("_g_turnjs_pageIsFolding=_sb_s.pageIsScrolling=false;",(_sb_s.pageTransitionDuration*5));	// should not last longer than 5 times transition duration
					// remove unused gesture detectors
					//_sb_fn.detachGestureDetector("c*");	// remove all gesture detectors on area elements like <area id="c1P1_A1_1" ....
					doHaltEvents = true;
					break;
				case _sb_s.pointerEvents.end:
				//case _sb_s.pointerEvents.out:		// DO NOT or IE 10 mobi will stop firing events
					//log_methods.log("gestureHandler\n" + eventType + " on: " + e.target.id + "\n\ttouchstarted: " + gestureDetectorPointer.touchstarted + "\n\tpageIsScrolling: " + _sb_s.pageIsScrolling);
					removeWindowGestureDetectorListeners();
					if (_sb_s.pageIsScrolling) {
						//gestureDetectorPointer.touchstarted = false;
						gestureDetectorPointer.isSwipe = false;
						gestureDetectorPointer.swipeDirection = "";
						return(true);
					}
					if (gestureDetectorPointer.touchstarted === false) return(true);	// mousedown did not happen yet; swipe not initiated
					gestureDetectorPointer.touchstarted = false;	// block all further mousemoves until next mousedown
					delta = false;
					break;
/* do like _sb_s.pointerEvents.end
				case _sb_s.pointerEvents.out:
					if (_sb_s.pageIsScrolling) {
						gestureDetectorPointer.touchstarted = false;
						return(true);
					}
					if (gestureDetectorPointer.touchstarted === false) return(true);	// mousedown did not happen yet; swipe not initiated
					//log_methods.log("gestureHandler " + eventType + " on:\n\t" + this + "\n\tisSwipe: "+gestureDetectorPointer.isSwipe);
					doHaltEvents = false;
					break;
*/
				case "click":
					//log_methods.log("gestureHandler " + eventType + " on:\n\t" + this.id);
					if (_sb_s.isTouchDevice) {
						doHaltEvents = true;
						break;
					}

					gestureDetectorPointer.touchstarted = false;
					delta = false;	// mousedown did not happen yet; swipe not initiated
					if (_sb_s.pageIsScrolling) {
						gestureDetectorPointer.touchstarted = false;
						return(true);
					}
					if (gestureDetectorPointer.isSwipe) {
						gestureDetectorPointer.isSwipe = false;
						gestureDetectorPointer.swipeDirection = "";
						break;	// handled in mousemove
					}
					try { if (this.id.indexOf("pv_P") > -1) return(true); } catch(ex){}	// no action on click on page images
					show_clicked_article(e,"attachGestureDetector CLICK");
					doHaltEvents = true;
					break;
			}
			
			//log_methods.log("gestureHandler doHaltEvents: " + doHaltEvents + "\ndoPreventDefault: " + doPreventDefault);
			if (doHaltEvents) return(haltEvents(e));
			if (doPreventDefault) { if (e.preventDefault) e.preventDefault(); return(false); }
			return(true);
		};

		// attach all window event listeners
		function attachWindowGestureDetectorListeners() {
			if ((gestureDetectorPointer.attachDetachWindowEvents == 1) && !gestureDetectorPointer.winHandlers_attached) {
				$(window).on(_mMove + " " + _mUp + " " + _mOut, gestureHandler);
				gestureDetectorPointer.winHandlers_attached = true;
				//log_methods.log("attachWindowGestureDetectorListeners " + _mMove + " " + _mUp + " " + _mOut);
			}
		}

		// remove all window event listeners
		function removeWindowGestureDetectorListeners() {
			$(window).off(_mMove + " " + _mUp + " " + _mOut, gestureHandler);	// remove listeners
			gestureDetectorPointer.winHandlers_attached = false;
			//log_methods.log("removeWindowGestureDetectorListeners");
		}
	
	
		// remove all event listeners
		function removeGestureDetectorListeners() {
			elemNode.off();	// remove listeners
				//log_methods.log("removeGestureDetector event handlers on:\n\t" + elemNode.attr("id"));
		}
	

		//elemNode.on(_mDown + " " + _mMove + " " + _mUp + " " + _mOut + " click", gestureHandler);
		elemNode.on(_mDown + " click", gestureHandler);
			// for IE8 we need to attach on element too
		if (_sb_s.is_IE && (_sb_s.IEVersion <= 8)) {
			elemNode.on(_mMove, gestureHandler);
		}
		// mark us as attached
		elemNode.attr("data-GDattached","1");
		//log_methods.log("attachGestureDetector\n\tattached to: "+elemNode.attr("id")+"\n\tdelegate to: "+elementName+"\n\tdata-GDattached: "+elemNode.attr("data-GDattached"));

		// propagate functions
		this.attachWindowGestureDetectorListeners = attachWindowGestureDetectorListeners;
		this.removeWindowGestureDetectorListeners = removeWindowGestureDetectorListeners;
		this.removeGestureDetectorListeners = removeGestureDetectorListeners;
		this.gestureHandler = gestureHandler;

		return(this);
	};
	
	/**
	 * Attach a gesture detector to an element
	 *
	 * @param {Object} element The element to detect gestures
     * optional parameters;
	 * @param {Object=} gestureDelegate Optional: The element to which to delegate the gesture
	 * @param {string|null=} elemName Optional: the element's name
	 * @param {Object|null=} evt Optional: the event object
	 * @param {boolean|null=} triggerevent Optional: to immediateleytrigger such an event
	 *
	 * @return nothing
	 */
	var attachGestureDetector = function (element,gestureDelegate,elemName, evt, triggerevent) {
		var elID, elIDhash, elemNode;
		if (typeof(element) == 'string') {	// given an element ID
			//alert("element is string ID: " + element);
			elID = elIDhash = element;
			if (elIDhash.indexOf("#") != 0) elIDhash = "#" + elIDhash;
			elemNode = $(elIDhash);
		}
		else {	// given the element
			//alert("element is object with ID: "+element.id);
			elemNode = $(element);
			elID = elemNode.attr("id");
		}

		if (elID in gestureDetectorObj_arr) return
		if (elemNode.attr("data-GDattached") == "1") return;

		gestureDetectorObj_arr[elID] = new gestureDetectorObj(elemNode,gestureDelegate,elemName);
			//log_methods.log("attachGestureDetector ATTACHED [ " + elID + ","+gestureDetectorObj_arr[elID] + "]");
			//log_methods.log("attachGestureDetector ALL ATTACHED:\n\t" + listAttachedGestureDetectors());
		if (evt && triggerevent) {
			gestureDetectorObj_arr[elID].gestureHandler(evt, triggerevent);
		}

	};

	// detach gesture detectors
	var detachGestureDetector = function (elemID) {
		var removeID = elemID, key;
		//log_methods.log("detachGestureDetector DETACHING:\n\t" + elemID);

		//if ((typeof(removeID) == 'undefined') || (removeID == null) || removeID == "") removeID = "*";
		do {
			// detach all
			if (removeID == "*") {
				for (key in gestureDetectorObj_arr) {
					gestureDetectorObj_arr[key].removeGestureDetectorListeners();	// remove event handlers
					$("#"+key).attr("data-GDattached","0");
					gestureDetectorObj_arr[key] = null;						// remove GD object
				}
				gestureDetectorObj_arr = [];	// clear array
				break;
			}

			// remove with wildcard
			if (removeID.indexOf("*") == removeID.length - 1) {	// ends with *
				removeID = removeID.substr(0,removeID.length-1);
				for (key in gestureDetectorObj_arr) {
					if (key.indexOf(removeID) == 0) {
						//log_methods.log("detachGestureDetector REMOVING detector for ID:\n\t" + key + "\n\tremoveID: " + removeID);
						gestureDetectorObj_arr[key].removeGestureDetectorListeners();	// remove event handlers
						$("#"+key).attr("data-GDattached","0");
						gestureDetectorObj_arr[key] = null;						// remove GD object
					}
				}
				break;
			}

			// remove selected only
			if (gestureDetectorObj_arr[removeID]) {
				//log_methods.log("detachGestureDetector REMOVING detector for ID:\n\t" + removeID);
				gestureDetectorObj_arr[removeID].removeGestureDetectorListeners();	// remove event handlers
				$("#"+removeID).attr("data-GDattached","0");
				gestureDetectorObj_arr[removeID] = null;						// remove GD object
			}

		} while(false);

		// clean array
		//for (key = gestureDetectorObj_arr.length; key >= 0; key--) {
		for (key in gestureDetectorObj_arr) {
			if (gestureDetectorObj_arr[key] == null) {
					//log_methods.log("detachGestureDetector CLEAN detector for key:\n\t" + key);
				delete gestureDetectorObj_arr[key];
			}
		}
		//log_methods.log("detachGestureDetector REMAINING detectors:\n\t" + listAttachedGestureDetectors());
	};
	_sb_fn.attachGestureDetector = attachGestureDetector;
	_sb_fn.detachGestureDetector = detachGestureDetector;


	/* ====================== */
	/*
	 * automatically adjust boock size to window size
	 */
	var getBookWidthFactor = function () {
		switch (_sb_s.pageDisplay) {
			case "double":
				if (_sb_s.pageTurnMode == 'turn') return 2;
				return _sb_s.pageSlideFactor;
				break;
			case "single":
				return 1;
				break;
		}
		return 2;
	},

	getPageHeightAvailable = function () {
		//_sb_s.DEBUGmode = 2; log_methods.log("header css display: " + _sb_e.scrollview_header.css("display"));
		var pageHeightMax = _sb_s.clientHeight	// clientHeight seems to be right
								- _sb_s.bodyOffsetTop		/* also subtract top offset if embedded in div */
								- _sb_e.body_marginTop 
										- (!_sb_s.isTouchDevice ? _sb_e.body_marginBottom : 0 )
								- _sb_e.body_paddingTop - _sb_e.body_paddingBottom
								- _sb_e.body_borderTopWidth - _sb_e.body_borderBottomWidth

								- _sb_e.sb_container_marginTop - _sb_e.sb_container_marginBottom
								- _sb_e.sb_container_paddingTop - _sb_e.sb_container_paddingBottom
								- _sb_e.sb_container_borderTopWidth - _sb_e.sb_container_borderBottomWidth

								- (_sb_e.scrollview_header.css("display") != "none" ? _sb_e.scrollview_header.outerHeight(true) : 0)
								/*
								- parseInt(_sb_e.scrollview_header.css('marginTop'), 10) - parseInt(_sb_e.scrollview_header.css('marginBottom'), 10)
								- parseInt(_sb_e.scrollview_header.css('paddingTop'), 10) - parseInt(_sb_e.scrollview_header.css('paddingBottom'), 10)
								- parseInt(_sb_e.scrollview_header.css('borderTopWidth'), 10) - parseInt(_sb_e.scrollview_header.css('borderBottomWidth'), 10)
								*/

								- _sb_e.scrollview_container_marginTop - _sb_e.scrollview_container_marginBottom
								- _sb_e.scrollview_container_paddingTop - _sb_e.scrollview_container_paddingBottom
								- _sb_e.scrollview_container_borderTopWidth - _sb_e.scrollview_container_borderBottomWidth

								- _sb_e.scrollview_content_marginTop - _sb_e.scrollview_content_marginBottom
								- _sb_e.scrollview_content_paddingTop - _sb_e.scrollview_content_paddingBottom
								- _sb_e.scrollview_content_borderTopWidth - _sb_e.scrollview_content_borderBottomWidth

								- _sb_e.sb_pagelist_marginTop - _sb_e.sb_pagelist_marginBottom
								- _sb_e.sb_pagelist_paddingTop - _sb_e.sb_pagelist_paddingBottom
								- _sb_e.sb_pagelist_borderTopWidth - _sb_e.sb_pagelist_borderBottomWidth

								- _sb_s.page_marginTop - _sb_s.page_marginBottom
								- _sb_s.page_paddingTop - _sb_s.page_paddingBottom
								- _sb_s.page_borderTopWidth - _sb_s.page_borderBottomWidth

								- _sb_e.scrollview_trailer.outerHeight(true)
								/*
								- parseInt(_sb_e.scrollview_trailer.css('marginTop'), 10) - parseInt(_sb_e.scrollview_trailer.css('marginBottom'), 10)
								- parseInt(_sb_e.scrollview_trailer.css('paddingTop'), 10) - parseInt(_sb_e.scrollview_trailer.css('paddingBottom'), 10)
								- parseInt(_sb_e.scrollview_trailer.css('borderTopWidth'), 10) - parseInt(_sb_e.scrollview_trailer.css('borderBottomWidth'), 10)
								*/
								- _sb_e.scrollview_bottom.outerHeight(true)
								/*
								- parseInt(_sb_e.scrollview_bottom.css('marginTop'), 10) - parseInt(_sb_e.scrollview_bottom.css('marginBottom'), 10)
								- parseInt(_sb_e.scrollview_bottom.css('paddingTop'), 10) - parseInt(_sb_e.scrollview_bottom.css('paddingBottom'), 10)
								- parseInt(_sb_e.scrollview_bottom.css('borderTopWidth'), 10) - parseInt(_sb_e.scrollview_bottom.css('borderBottomWidth'), 10)
								*/
								- (_sb_e.status_message.outerHeight(true) ? _sb_e.status_message.outerHeight(true) : 0)
								- (_sb_e.debugmessage && _sb_e.debugmessage.outerHeight(true) ? _sb_e.debugmessage.outerHeight(true) : 0)
								
								+ _sb_s.pageAdjustSizeTrim + _sb_s.pageAdjustHeightTrim
							;
		/*
		_sb_s.DEBUGmode = 2;
		log_methods.log("getPageHeightAvailable"
			+ "\n\t_sb_s.windowHeight: " + _sb_s.windowHeight

			+ "\n\t_sb_s.bodyOffsetTop: " + _sb_s.bodyOffsetTop
		
			+ "\n\n\tsb_body marginTop / bottom: " + _sb_e.body_marginTop + " / " + _sb_e.body_marginBottom
			+ "\n\tsb_body paddingTop / bottom: " + _sb_e.body_paddingTop + " / " + _sb_e.body_paddingBottom
			+ "\n\tsb_body borderTopWidht / bottom: " + _sb_e.body_borderTopWidth + " / " + _sb_e.body_borderBottomWidth

			+ "\n\tsb_container marginTop / bottom: " + _sb_e.sb_container_marginTop + " / " + _sb_e.sb_container_marginBottom
			+ "\n\tsb_container paddingTop / bottom: " + _sb_e.sb_container_paddingTop + " / " + _sb_e.sb_container_paddingBottom
			+ "\n\tsb_container borderTopWidht / bottom: " + _sb_e.sb_container_borderTopWidth + " / " + _sb_e.sb_container_borderBottomWidth

			+ "\n\n\tscrollview-header outerHeight: " + 	(_sb_e.scrollview_header.css("display") != "none" ? _sb_e.scrollview_header.outerHeight(true) : 0)

			+ "\n\tscrollview-header marginTop / bottom: " + parseInt(_sb_e.scrollview_header.css('marginTop'), 10) + " / " + parseInt(_sb_e.scrollview_header.css('marginBottom'), 10)
			+ "\n\tscrollview-header paddingTop / bottom: " + parseInt(_sb_e.scrollview_header.css('paddingTop'), 10) + " / " + parseInt(_sb_e.scrollview_header.css('paddingBottom'), 10)
			+ "\n\tscrollview-header borderTopWidht / bottom: " + parseInt(_sb_e.scrollview_header.css('borderTopWidth'), 10) + " / " + parseInt(_sb_e.scrollview_header.css('borderBottomWidth'), 10)

			+ "\n\n\tscrollview-container marginTop / bottom: "    + _sb_e.scrollview_container_marginTop + " / " + _sb_e.scrollview_container_marginBottom
			+ "\n\tscrollview-container paddingTop / bottom: "     + _sb_e.scrollview_container_paddingTop + " / " + _sb_e.scrollview_container_paddingBottom
			+ "\n\tscrollview-container borderTopWidht / bottom: " + _sb_e.scrollview_container_borderTopWidth + " / " + _sb_e.scrollview_container_borderBottomWidth

			+ "\n\n\tscrollview_content marginTop / bottom: "    + _sb_e.scrollview_content_marginTop + " / " + _sb_e.scrollview_content_marginBottom
			+ "\n\tscrollview_content paddingTop / bottom: "     + _sb_e.scrollview_content_paddingTop + " / " + _sb_e.scrollview_content_paddingBottom
			+ "\n\tscrollview_content borderTopWidht / bottom: " + _sb_e.scrollview_content_borderTopWidth + " / " + _sb_e.scrollview_content_borderBottomWidth

			+ "\n\n\t\tpv_P1 marginTop / bottom: "    + _sb_s.page_marginTop + " / " + _sb_s.page_marginBottom
			+ "\n\t\tpv_P1 paddingTop / bottom: "     + _sb_s.page_paddingTop + " / " + _sb_s.page_paddingBottom
			+ "\n\t\tpv_P1 borderTopWidht / bottom: " + _sb_s.page_borderTopWidth + " / " + _sb_s.page_borderBottomWidth

			+ "\n\n\tscrollview-trailer outerHeight: " + _sb_e.scrollview_trailer.outerHeight(true)
			+ "\n\tscrollview-trailer marginTop / bottom: " + parseInt(_sb_e.scrollview_trailer.css('marginTop'), 10) + " / " + parseInt(_sb_e.scrollview_trailer.css('marginBottom'), 10)
			+ "\n\tscrollview-trailer paddingTop / bottom: " + parseInt(_sb_e.scrollview_trailer.css('paddingTop'), 10) + " / " + parseInt(_sb_e.scrollview_trailer.css('paddingBottom'), 10)
			+ "\n\tscrollview-trailer borderTopWidht / bottom: " + parseInt(_sb_e.scrollview_trailer.css('borderTopWidth'), 10) + " / " + parseInt(_sb_e.scrollview_trailer.css('borderBottomWidth'), 10)

			+ "\n\n\tscrollview-bottom outerHeight: " + _sb_e.scrollview_bottom.outerHeight(true)
			+ "\n\tscrollview-bottom marginTop / bottom: " + parseInt(_sb_e.scrollview_bottom.css('marginTop'), 10) + " / " + parseInt(_sb_e.scrollview_bottom.css('marginBottom'), 10)
			+ "\n\tscrollview-bottom paddingTop / bottom: " + parseInt(_sb_e.scrollview_bottom.css('paddingTop'), 10) + " / " + parseInt(_sb_e.scrollview_bottom.css('paddingBottom'), 10)
			+ "\n\tscrollview-bottom borderTopWidht / bottom: " + parseInt(_sb_e.scrollview_bottom.css('borderTopWidth'), 10) + " / " + parseInt(_sb_e.scrollview_bottom.css('borderBottomWidth'), 10)

			+ "\n\n\tstatus_message outerHeight: " +  (_sb_e.status_message.outerHeight(true) ? _sb_e.status_message.outerHeight(true) : 0)
			+ "\n\n\tdebugmessage outerHeight: " +  (_sb_e.debugmessage && _sb_e.debugmessage.outerHeight(true) ? _sb_e.debugmessage.outerHeight(true) : 0)

			+ "\n\n\t_sb_s.pageAdjustSizeTrim: " +  _sb_s.pageAdjustSizeTrim
			+ "\n\n\t_sb_s.pageAdjustHeightTrim: " +  _sb_s.pageAdjustHeightTrim
			+ "\n\n\t__________" 
			+ "\n\n\tpageHeightMax: " + pageHeightMax + " - " + typeof(pageHeightMax)
			);
		*/
		return(pageHeightMax);
	},
	getBookWidthAvailable = function (caller) {
		//log_methods.log("getBookWidthAvailable called by: " + caller);
		get_clientSize("getBookWidthAvailable");
		var bookWidthMax = _sb_s.clientWidth
								- _sb_e.body_min_marginLeft - _sb_e.body_min_marginRight	// minimum
								/* do not ad margin l/r!!! the only browser which reports bodi-margin is Chrome. All others return 0
								- (!_sb_s.isTouchDevice ?
												- _sb_e.body_marginLeft - _sb_e.body_marginRight
												: 0 )
								*/
								- _sb_e.body_borderLeftWidth - _sb_e.body_borderRightWidth
								- _sb_e.body_paddingLeft - _sb_e.body_paddingRight

								- _sb_e.sb_container_marginLeft - _sb_e.sb_container_marginRight
								- _sb_e.sb_container_borderLeftWidth - _sb_e.sb_container_borderRightWidth
								- _sb_e.sb_container_paddingLeft - _sb_e.sb_container_paddingRight

								- _sb_e.scrollview_container_marginLeft - _sb_e.scrollview_container_marginRight
								- _sb_e.scrollview_container_borderLeftWidth - _sb_e.scrollview_container_borderRightWidth
								- _sb_e.scrollview_container_paddingLeft - _sb_e.scrollview_container_paddingRight

								- _sb_e.scrollview_content_marginLeft - _sb_e.scrollview_content_marginRight
								- _sb_e.scrollview_content_borderLeftWidth - _sb_e.scrollview_content_borderRightWidth
								- _sb_e.scrollview_content_paddingLeft - _sb_e.scrollview_content_paddingRight

								- _sb_e.sb_pagelist_marginLeft - _sb_e.sb_pagelist_marginRight
								- _sb_e.sb_pagelist_borderLeftWidth - _sb_e.sb_pagelist_borderRightWidth
								- _sb_e.sb_pagelist_paddingLeft - _sb_e.sb_pagelist_paddingRight

								//- (_sb_s.pageDisplay == 'double' ? 2 : 1) * (  _sb_s.page_marginLeft + _sb_s.page_marginRight
								- getBookWidthFactor() * (  _sb_s.page_marginLeft + _sb_s.page_marginRight
																					+ _sb_s.page_paddingLeft + _sb_s.page_paddingRight
																					+ _sb_s.page_borderLeftWidth + _sb_s.page_borderRightWidth
																					)
								+ _sb_s.pageAdjustSizeTrim + _sb_s.pageAdjustWidthTrim
							;
		/*
		_sb_s.DEBUGmode = 2;
		log_methods.log("getBookWidthAvailable"
						+ "\n\t_sb_s.clientWidth: " + _sb_s.clientWidth
						+ "\n\tbody_marginLeft - Right: " + _sb_e.body_marginLeft + " - " + _sb_e.body_marginRight
						+ "\n\tbody_borderLeftWidth - Right: " + _sb_e.body_borderLeftWidth + " - " + _sb_e.body_borderRightWidth
						+ "\n\tbody_paddingLeft - Right: " + _sb_e.body_paddingLeft + " - " + _sb_e.body_paddingRight

						+ "\n\tsb_container_marginLeft - Right: " + _sb_e.sb_container_marginLeft + " - " + _sb_e.sb_container_marginRight
						+ "\n\tsb_container_borderLeftWidth - Right: " + _sb_e.sb_container_borderLeftWidth + " - " + _sb_e.sb_container_borderRightWidth
						+ "\n\tsb_container_paddingLeft - Right: " + _sb_e.sb_container_paddingLeft + " - " + _sb_e.sb_container_paddingRight

						+ "\n\tscrollview_container_marginLeft - Right: " + _sb_e.scrollview_container_marginLeft + " - " + _sb_e.scrollview_container_marginRight
						+ "\n\tscrollview_container_borderLeftWidth - Right: " + _sb_e.scrollview_container_borderLeftWidth + " - " + _sb_e.scrollview_container_borderRightWidth
						+ "\n\tscrollview_container_paddingLeft - Right: " + _sb_e.scrollview_container_paddingLeft + " - " + _sb_e.scrollview_container_paddingRight

						+ "\n\tscrollview_content_marginLeft - Right: " + _sb_e.scrollview_content_marginLeft + " - " + _sb_e.scrollview_content_marginRight
						+ "\n\tscrollview_content_borderLeftWidth - Right: " + _sb_e.scrollview_content_borderLeftWidth + " - " + _sb_e.scrollview_content_borderRightWidth
						+ "\n\tscrollview_content_paddingLeft - Right: " + _sb_e.scrollview_content_paddingLeft + " - " + _sb_e.scrollview_content_paddingRight

						+ "\n\tsb_pagelist_marginLeft - Right: " + _sb_e.sb_pagelist_marginLeft + " - " + _sb_e.sb_pagelist_marginRight
						+ "\n\tsb_pagelist_borderLeftWidth - Right: " + _sb_e.sb_pagelist_borderLeftWidth + " - " + _sb_e.sb_pagelist_borderRightWidth
						+ "\n\tsb_pagelist_paddingLeft - Right: " + _sb_e.sb_pagelist_paddingLeft + " - " + _sb_e.sb_pagelist_paddingRight

						+ "\n\tpage_marginLeft - Right: " + _sb_s.page_marginLeft + " - " + _sb_s.page_marginRight
						+ "\n\tpage_paddingLeft - Right: " + _sb_s.page_paddingLeft + " - " + _sb_s.page_paddingRight
						+ "\n\tpage_borderLeftWidth - Right: " + _sb_s.page_borderLeftWidth + " - " + _sb_s.page_borderRightWidth

						+ "\nbookWidthMax: " + bookWidthMax);
		*/
		return(bookWidthMax);
	},
	calcOrigBookWidth = function () {
		var origBookWidth = _sb_s.pageWidth
							+ _sb_s.page_borderLeftWidth + _sb_s.page_borderRightWidth;
			//origBookWidth *= (_sb_s.pageDisplay == 'double' ? 2 : 1);				
			origBookWidth *= getBookWidthFactor();				
			origBookWidth += _sb_e.sb_pagelist_paddingLeft + _sb_e.sb_pagelist_paddingRight
							+ _sb_e.sb_pagelist_borderLeftWidth + _sb_e.sb_pagelist_borderRightWidth
							+ _sb_e.sb_pagelist_marginLeft + _sb_e.sb_pagelist_marginRight
							;
			/*
			_sb_s.DEBUGmode = 2;
			log_methods.log("calcOrigBookWidth"
							+ "\n\tpageWidth: " + _sb_s.pageWidth
							+ "\n\tpage border l / r: " + _sb_s.page_borderLeftWidth + " / " + _sb_s.page_borderRightWidth
							+ "\n\tsb_pagelist padding l / r: " + _sb_e.sb_pagelist_paddingLeft + " / " + _sb_e.sb_pagelist_paddingRight
							+ "\n\tsb_pageliste border l / r: " + _sb_e.sb_pagelist_borderLeftWidth + " / " + _sb_e.sb_pagelist_borderRightWidth
							+ "\n\tsb_pagelist margin l / r: " + _sb_e.sb_pagelist_marginLeft + " / " + _sb_e.sb_pagelist_marginRight
							+ "\norigBookWidth: " + origBookWidth
							);
			*/
		return(origBookWidth);
	},

	adjustPageImageSize = function (caller, bookCalcOnly, setBodyWidth, recalcAreas) {
		//log_methods.log("adjustPageImageSize called by: \n\t" + caller);
			function setNewBodyWidth(bookwidth) {
				_sb_s.bodyWidth = bookwidth 
											+ _sb_e.sb_pagelist_paddingLeft + _sb_e.sb_pagelist_paddingRight
											+ _sb_e.sb_pagelist_borderLeftWidth + _sb_e.sb_pagelist_borderRightWidth
											+ _sb_e.sb_pagelist_marginLeft + _sb_e.sb_pagelist_marginRight

											+ _sb_e.scrollview_content_marginLeft + _sb_e.scrollview_content_marginRight
											+ _sb_e.scrollview_content_borderLeftWidth + _sb_e.scrollview_content_borderRightWidth
											+ _sb_e.scrollview_content_paddingLeft + _sb_e.scrollview_content_paddingRight

											+ _sb_e.scrollview_container_marginLeft + _sb_e.scrollview_container_marginRight
											+ _sb_e.scrollview_container_borderLeftWidth + _sb_e.scrollview_container_borderRightWidth
											+ _sb_e.scrollview_container_paddingLeft + _sb_e.scrollview_container_paddingRight

											+ _sb_e.sb_container_marginLeft + _sb_e.sb_container_marginRight
											+ _sb_e.sb_container_borderLeftWidth + _sb_e.sb_container_borderRightWidth
											+ _sb_e.sb_container_paddingLeft + _sb_e.sb_container_paddingRight

											+ _sb_e.body_paddingLeft + _sb_e.body_paddingRight
											+ _sb_e.body_borderLeftWidth + _sb_e.body_borderRightWidth
									
											- _sb_e.body_min_marginLeft - _sb_e.body_min_marginRight;	// minimum
					//alert("setNewBodyWidth bookwidth: '" + bookwidth + "' _sb_s.bodyWidth: " + _sb_s.bodyWidth);
				return(_sb_s.bodyWidth);
			}


			function recalcArticleAreas() {
				var i, ii, p, pp;

				//_sb_s.DEBUGmode = 2;log_methods.log("recalcArticleAreas _g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding);
				if (_sb_s.pageAdjustAreas == null) _sb_s.pageAdjustAreas = document.getElementsByTagName("area");
				//_sb_s.starttime = new Date().getTime();
				for (p = 0, pp = _sb_s.pageAdjustAreas.length; p < pp; p++) {
					// store coords for default page image size
					if (!_sb_s.pageAdjustAreas[p].getAttribute("data-coords")) _sb_s.pageAdjustAreas[p].setAttribute("data-coords", _sb_s.pageAdjustAreas[p].getAttribute("coords"));
					coords = _sb_s.pageAdjustAreas[p].getAttribute("data-coords");	// is attribute like:  coords="19,41,71,41,71,46,19,46"
					coords_arr = coords.split(",");
					if (coords_arr.length >= 4) {
						for (i = 0, ii = coords_arr.length; i < ii; i++) {
							coords_arr[i] = "" + Math.floor((parseInt(coords_arr[i], 10) * _sb_s.pageAdjustCurrentRatio * _sb_s.pageImageCoordsRatio));
						}
						_sb_s.pageAdjustAreas[p].setAttribute("coords", coords_arr.join(","));
 						//_sb_s.DEBUGmode = 2;log_methods.log("RESIZING ratio: " + _sb_s.pageAdjustCurrentRatio + "\n_sb_s.pageImageCoordsRatio: " + _sb_s.pageImageCoordsRatio + "\ncoords old: " + coords + "\n\t\t\tnew: " + coords_arr.join(","));
					}
				}
				//_sb_s.endtime = new Date().getTime();
				//log_methods.log("RESIZING areas done, elapsed ms: " + (_sb_s.endtime - _sb_s.starttime));
			}
		if (recalcAreas) {
			recalcArticleAreas();
			return;
		}
		if (_g_turnjs_pageIsFolding === true) return;

		if (_sb_fn.hidePageThumbs) _sb_fn.hidePageThumbs();

		var i,
			cbidx,
			maxPageHeightAvailable = getPageHeightAvailable(),	// calc max avail height and width
			maxBookWidthAvailable = getBookWidthAvailable("adjustPageImageSize"),
			origBookWidth = calcOrigBookWidth(),
			// calc height
			pageTBborderWidth = _sb_s.page_borderTopWidth + _sb_s.page_borderBottomWidth,
			outerPageImageHeight = _sb_s.pageHeight + pageTBborderWidth,
			// calc width
			pageLRborderWidth = _sb_s.page_borderLeftWidth + _sb_s.page_borderRightWidth,
			outerPageImageWidth = _sb_s.pageWidth + pageLRborderWidth,
			// calc book
			curBookWidth = _sb_e.sb_pagelist.width(),
			curBookHeight = _sb_e.sb_pagelist.height(),
			ratioW, ratioH;

		if (maxPageHeightAvailable < _sb_s.pageAdjustMinHeight) maxPageHeightAvailable = _sb_s.pageAdjustMinHeight;	// don't make it smaller
			//_sb_s.DEBUGmode = 2; log_methods.log("_sb_s.pageWidth: " + _sb_s.pageWidth + "\n\touterPageImageHeight: " + outerPageImageHeight + "\n\tmaxPageHeightAvailable: " + maxPageHeightAvailable + "\n\t_sb_s.pageAdjustToWindow: " + _sb_s.pageAdjustToWindow);

		//alert("maxBookWidthAvailable:"+maxBookWidthAvailable + "/ " + origBookWidth);
		//alert("maxPageHeightAvailable:"+maxPageHeightAvailable + "/ " + outerPageImageHeight);
		ratioW = maxBookWidthAvailable / origBookWidth;
		ratioH = maxPageHeightAvailable / outerPageImageHeight;

		if (_sb_s.pageAdjustToWindow == 0) {
			_sb_s.pageAdjustCurrentRatio = 1.0;
		}
		else {
			_sb_s.pageAdjustCurrentRatio = Math.min(ratioW, ratioH);
			if (_sb_s.pageAdjustCurrentRatio > _sb_s.pageAdjustOversizeRatio) _sb_s.pageAdjustCurrentRatio = _sb_s.pageAdjustOversizeRatio;
		}

		//alert("outerPageImageWidth: " + outerPageImageWidth + "\n_sb_s.pageAdjustCurrentRatio: " + _sb_s.pageAdjustCurrentRatio);
		var	p, coords, coords_arr,
			pageDivWidth = Math.floor(outerPageImageWidth * _sb_s.pageAdjustCurrentRatio),		// the div containg the page img
			pageDivHeight = Math.floor(outerPageImageHeight * _sb_s.pageAdjustCurrentRatio),
			
			newPageImgW = pageDivWidth - pageLRborderWidth,	// the page img
			newPageImgH = pageDivHeight - pageTBborderWidth,

			newW = (_sb_s.pageDisplay == 'double' ? (2 * pageDivWidth) : pageDivWidth),
			newH = pageDivHeight,
			adjust_sb_body = false,
			bodyZIndex;
		/*
		_sb_s.DEBUGmode = 2;
		log_methods.log("adjustPageImageSize - caller: " + caller
					+ "\nWindow:"
					+ "\n\clientWidth x Height: " + _sb_s.clientWidth + " x " + _sb_s.clientHeight
					+ "\n\tmaxBookWidthAvailable: " + maxBookWidthAvailable
					+ "\n\torigBookWidth: " + origBookWidth
					+ "\n\t\tRatio width: " + ratioW
					+ "\n\tmaxPageHeightAvailable: " + maxPageHeightAvailable
					+ "\n\touterPageImageHeight: " + outerPageImageHeight
					+ "\n\t\tRatio height: " + ratioH
					+ "\ncurrent book width x height: " + curBookWidth + " x " + curBookHeight
					+ "\n\touterPageImageWidth x Height: " + outerPageImageWidth + " x " + outerPageImageHeight
					+ "\nnew book image:"
					+ "\n\tnewPageImgW x H: " + newPageImgW + " x " + newPageImgH
					+ "\nnew book:"
					+ "\n\tpageDivWidth x Height: " + pageDivWidth + " x " + pageDivHeight
					+ "\nwidth:"
					+ "\n\tnew book Width x Height: " + newW + " x " + newH
					+ "\nRatio: " + _sb_s.pageAdjustCurrentRatio
					+ "\n\tnew Book size w x h: "  + newW + " x " + newH
					+ "\n\tnew bodyWidth: " + _sb_s.bodyWidth
					+ "\n\tsb_pagelist width: " + _sb_e.sb_pagelist.width()
					+ "\n\tbody_min_marginLeft, Right: " + _sb_e.body_min_marginLeft + ", " +  _sb_e.body_min_marginRight
					);
		*/

		if (setBodyWidth) {
			setNewBodyWidth(newW);	// sets _sb_s.bodyWidth
			if (_sb_s.pageTurnMode == 'turn') {
				bodyZIndex = getStyle(_sb_e.bodyEl,"z-index",1); // get z-index of sb_body to check which element's width must be adjusted
				//alert("adjust_sb_body: '" + bodyZIndex + "' to width: " + _sb_s.bodyWidth);
				if ((bodyZIndex === null) || (parseInt(bodyZIndex, 10) === 0)) adjust_sb_body = false;
				else adjust_sb_body = true;
				if (adjust_sb_body) {
					_sb_e.body.css("width",_sb_s.bodyWidth+"px");
					_sb_e.scrollview_content.css("width","auto");
				}
				else {
					//if (_sb_s.pageDisplay == 'double') _sb_e.body.css("width",_sb_s.bodyWidth+"px");
					_sb_e.scrollview_content.css("width",_sb_s.bodyWidth+"px");
				}
			}
		}

		if (bookCalcOnly) {

			return([newW, newH, pageDivWidth, pageDivHeight, newPageImgW, newPageImgH]);
		}

		if ((newW == curBookWidth) && (newH == curBookHeight)) {
			//log_methods.log("adjustPageImageSize caller - " + caller + "\n\tNOTHING to do\n\tnewW: " + newW + ", curBookWidth: " + curBookWidth + "\n\tnewH: " + newH + ", curBookWidth: " + curBookHeight);
			return(false);	// we do nothing
		}
		

		//_sb_s.DEBUGmode = 2; log_methods.log("RESIZING page images to w x h: " + pageDivWidth + " x " + pageDivHeight + "\n\tRatio: " + _sb_s.pageAdjustCurrentRatio + "\n\tnew Book size w x h: "  + newW + " x " + newH);

		// set the book and page images size
		switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
			default:
			case "turn":
				_sb_e.sb_pagelist.pagesize(pageDivWidth, pageDivHeight, newPageImgW, newPageImgH);
				_sb_e.sb_pagelist.turn("size", newW, newH);
				// also set new body width
				setNewBodyWidth(newW);	// sets _sb_s.bodyWidth
				bodyZIndex = getStyle(_sb_e.bodyEl,"z-index",1); // get z-index of sb_body to check which element's width must be adjusted
				//alert("adjust_sb_body: " + bodyZIndex + " to width: " + _sb_s.bodyWidth);
				if ((bodyZIndex === null) || (parseInt(bodyZIndex, 10) === 0)) adjust_sb_body = false;
				else adjust_sb_body = true;
				if (adjust_sb_body) {
					_sb_e.body.css("width",_sb_s.bodyWidth+"px");
					_sb_e.scrollview_content.css("width","auto");
				}
				else {
					//if (_sb_s.pageDisplay == 'double') _sb_e.body.css("width",_sb_s.bodyWidth+"px");
					_sb_e.scrollview_content.css("width",_sb_s.bodyWidth+"px");
				}
				break;
			case "slide":
				for (i = 0; i < num_epaper_pages; i++) {
					//alert("i:"+i+"\nnum_epaper_pages:"+num_epaper_pages + "\npageDivWidth:"+pageDivWidth + "\npageDivHeight:"+pageDivHeight + "\nepaper_pages[i]:"+epaper_pages[i]);
					// resize the container
					epaper_pages[i][5].css({width: pageDivWidth, height: pageDivHeight});
					// resize the container's child: the image
					epaper_pages[i][5].children(":first").css({width: newPageImgW, height: newPageImgH});
				}
				break;
		}

			// this may change when window is resized
		_sb_s.pageToPageOffsetWidth = _sb_e.sb_pagelist.children(":first").outerWidth()
											+ cssIntVal(_sb_e.sb_pagelist.children(":first").css("marginLeft"))
											+ cssIntVal(_sb_e.sb_pagelist.children(":first").css("marginRight"));
		//alert("pageToPageOffsetWidth:"+_sb_s.pageToPageOffsetWidth);
		if (_sb_s.pageTurnMode == "slide") {
			// adjust current page view
			slide_page(_sb_s.currentPageIdx, 0, true)
		}

		// resize the area elements attribute coords
		//log_methods.log("RESIZING areas: ");
		recalcArticleAreas();

		// call registered CBs after book resize
		for (cbidx = 0; cbidx < after_bookResize_custom_CBs.length; cbidx++) {
			after_bookResize_custom_CBs[cbidx](_sb_s.pageAdjustCurrentRatio);
		}

		// resize the lightbox when showing
		//		hideLightbox(null, false);
		if (_sb_s.lightboxIsShowing == true) {
			calcLightboxSizes(true);
			resizeLightboxElements();
			calcOverlaySizes(true);
			_sb_fn.clear_all_shadows(0);
		}

		return(true);
	},

	adjustPageImageSizeCallerTimeout = null,
	adjustPageImageSizeCaller = function (caller) {
		if (adjustPageImageSizeCallerTimeout != null) {
			clearTimeout(adjustPageImageSizeCallerTimeout);
			adjustPageImageSizeCallerTimeout = null;
		}
		adjustPageImageSizeCallerTimeout = setTimeout(function(){_sb_fn.adjustPageImageSize(caller,false,false,false)},_sb_s.pageAdjustToWindowTimeout);
	},

	disableAdjustPageImageSize = function() {
		if (_sb_s.pageAdjustToWindow != 0) _sb_s.pageAdjustToWindow = 0;
	},

	enableAdjustPageImageSize = function(caller, mode) {
		//log_methods.log("enableAdjustPageImageSize caller: " + caller + ", mode: " + mode);

		if ((typeof(mode) != 'undefined') && (mode != null)) _sb_s.pageAdjustToWindow = parseInt(mode, 10);

		if (_sb_s.pageAdjustToWindow != 0) {
			if (_sb_s.pageAdjustToWindow & 1) update_screenDimensions(false, "enableAdjustPageImageSize");
		}
	};
	// make globally available
	_sb_fn.adjustPageImageSize = adjustPageImageSize;
	_sb_fn.adjustPageImageSizeCaller = adjustPageImageSizeCaller;
	_sb_fn.disableAdjustPageImageSize = disableAdjustPageImageSize;
	_sb_fn.enableAdjustPageImageSize = enableAdjustPageImageSize;



	// ******* add click for PDF to img tags
	var add_image_pdf_click = function (thestr) {
		if ((thestr == null) || (thestr == "")) return(thestr);
		if (thestr.indexOf("<img ") < 0) return(thestr);
		if ( (thestr.toLowerCase().indexOf(".pdf") < 0)
			&& (thestr.toLowerCase().indexOf(".jpg") < 0)
			&& (thestr.toLowerCase().indexOf(".gif") < 0)
			&& (thestr.toLowerCase().indexOf(".png") < 0)
			) return(thestr);
		var str = thestr,
			re_img = null,
			imagetags,
			brk = 0;		// just a security counter if the regexp can not replace some signatures
		try {	// old IE 6 cant understand this
			re_img = new RegExp("<img [^>]+>","gi");		// get html <img .....> construct
			imagetags = re_img.exec(str);	// get all image tags into array
			if ((imagetags == null) || (imagetags[0] == "")) return(thestr);

			while ((imagetags != null) && (imagetags[0] != "")) {
				//alert("str:\n" + str + "\n\nimagetags.length: "+ imagetags.length + "\n\nimagetags: "+ imagetags);
				for (var i = 0; i < imagetags.length; i++) {
					var alt_re = new RegExp("alt=\"(.)*?\"","gi"),		// get content of alt attr construct
						altattrs = alt_re.exec(imagetags[i]),
						altattr, pos;
					if ((altattrs == null) || (altattrs[0] == "")) continue;
	
					altattr = "" + altattrs[i];	// the whole alt="xxx.pdf" string
					pos = 0;
					do {
						var re = new RegExp("\.pdf","gi"); pos = altattr.search(re); if (pos >= 0) break;
						re = new RegExp("\.jpg","gi"); pos = altattr.search(re); if (pos >= 0) break;
						re = new RegExp("\.gif","gi"); pos = altattr.search(re); if (pos >= 0) break;
						re = new RegExp("\.png","gi"); pos = altattr.search(re); if (pos >= 0) break;
					} while (false);
					if (pos < 0) continue;
	
					// original is a pdf,jpg,gif or png file
					var re = /"(.)*"/,				// get the pdf name in the alt attribute
						pdf_re = re.exec(altattr),		// the pdf name only
						pdf = "" + pdf_re[0];		// ... as string
					re = /"/g;						// remove double quotes
					pdf = pdf.replace(re,"");
					pdf = escape(pdf);
	
					// replace the <img ...> with <a href="pdf"><img...></a>
					var new_image = imagetags[i].replace(/\<img /gi,"\<XXimg title=\"^\" style=\"border:0;\" "),
						new_img_lnk = "<a style=\"text-decoration:none;\" href=\"" + pdf + "\" target=\"_blank\">" + new_image + "</a>";
					//alert("imagetags: " + imagetags + "\n\naltattr: " + altattr + "\npos: " + pos + "\npdf: " + pdf+ "\n\nnew_img_lnk: " + new_img_lnk);
					re = new RegExp(imagetags[i],"gi");
					str = str.replace(re, new_img_lnk);
	
					// go to next img alt construct
					if (brk > 1000) break;
					brk++;
				}
				// get next img tag
				imagetags = re_img.exec(str);	// get all image tags into array
			}
		}
		catch(ex) {
			str = str.replace(/\<XXimg /gi, "<img ");
			return (str);
		}
		str = str.replace(/\<XXimg /gi, "<img ");
		return(str);
	}



	// show a file (image) in new window or in a lightbox
	var showImage_lightbox_recallcount = 0;
	showImage = function (name,imgwidth,imgheight,whereOverride) {	// public function
		var mywhere = _sb_s.showImage.where;
		if (whereOverride) mywhere = whereOverride;
		//log_methods.log("image name: " + name + "\n\twhereOverride: " + whereOverride);

		if (_sb_s.is_IE && (_sb_s.IEVersion <= 7)) mywhere = 1;	// show in new window for IE<=7
		if (_sb_s.showImageLightbox_enable <= 0) mywhere = 1;	// show in new window

		switch (mywhere) {
			default:
			case 1:	// show in new window
				try {	// on some devices the access to open a new window is not granted like on Windows Phone
					var winparams =   "resizable=Yes,scrollbars=Yes"
									+ (_sb_s.showImage.x > 0 ? ',screenX=' + _sb_s.showImage.x : "")
									+ (_sb_s.showImage.y > 0 ? ',screenY=' + _sb_s.showImage.y : "")
									+ (_sb_s.showImage.x > 0 ? ',left=' + _sb_s.showImage.x : "")
									+ (_sb_s.showImage.x > 0 ? ',top=' + _sb_s.showImage.x : "")
									+ (_sb_s.showImage.w > 0 ? ',width=' + _sb_s.showImage.w : "")
									+ (_sb_s.showImage.h > 0 ? ',height=' + _sb_s.showImage.h : ""),
						winname = "resizedImage";
					if (_sb_s.inApp == true) winname = "_self";
					try {
						F=window.open('',winname,winparams);		// may throw 'access denied' or just may be null
					} catch(ex) {}
					if (!F) {
						F=window.open('','_self',winparams);		// may throw 'access denied'
						//alert("nope3");
					}
					if (F) {	// may have a popup blocker
						F.location.href=name;
						F.focus();
					}
					//else alert("nope2: ");
				} catch(ex) {
					//alert("nope: " + ex.message);
				}
				break;

			case 2:	// show in a lightbox overlay
				if (typeof(showImage_lightbox) == 'undefined') {	// wait until loaded
					if (showImage_lightbox_recallcount++ < 50) {	// wait 2 secs max
						setTimeout(function() { showImage(name,imgwidth,imgheight,whereOverride); },40);
					}
					else {
						showImage_lightbox_recallcount = 49;
						setTimeout(function() { showImage(name,imgwidth,imgheight,1); },5);	// show in new window instead
					}
					return false;
				}
			 
				if (_sb_s.showImageHandle_1 == null) {
					_sb_s.showImageHandle_1 = new showImage_lightbox();
				}
				_sb_s.showImageHandle_1.showImageLightBox(name,imgwidth,imgheight,"_sb_s.showImageHandle_1",_sb_s.showImageLightbox);

				break;
		}
		return false;
	};

	// public function
	goto_thumbs_page = function (the_page, isPageIndex) {
		//log_methods.log("goto_thumbs_page the_page: " + the_page + "\n\tisPageIndex: " + isPageIndex);
		var myPage = 0; if (the_page) myPage = parseInt(the_page, 10);
		if (!isPageIndex) myPage--;
		// scroll to the desired page index
		if (_sb_e.pageThumbsScrollContainer) _sb_e.pageThumbsScrollContainer.list("scrollToPage",myPage);
		return;
	};

	var cb_turn_end = function(evtmsg, page) {
		var cbidx;
		if (_sb_s.isTouchDevice) $(document).off("touchmove");	// re-enable default window scroll on touch devices

		var curpage = parseInt(page, 10),
			curentPageDisplay;
		switch (curpage) {
			case 1:	// first page shown
				_sb_s.pageNumLeft = 0;
				_sb_s.pageNumRight = curpage;
				break;
			default:
				if (curpage >= _sb_s.totalPages) {
					if (isEven(curpage)) {	// left page
						_sb_s.pageNumLeft = curpage;
						_sb_s.pageNumRight = 0;
						break;
					}
					// document ends with an odd/right page
					_sb_s.pageNumLeft = curpage - 1;
					_sb_s.pageNumRight = curpage;
					break;
				}
				// current page is in range between first and last page
				if (isEven(curpage)) {	// left page
					_sb_s.pageNumLeft = curpage;
					_sb_s.pageNumRight = curpage + 1;
					break;
				}
				// an odd/right
				_sb_s.pageNumLeft = curpage - 1;
				_sb_s.pageNumRight = curpage;
				break;
		}

		// set page index
		_sb_s.currentPageIdx = curpage - 1;

		// update the goto page field
		if (_sb_s.sb_page_entry_field_behaviour & 2) {
			curentPageDisplay = getPageNameFromPageIndex(_sb_s.currentPageIdx);
			//log_methods.log("cb_turn_end: goto page field - _sb_s.currentPageIdx: " + _sb_s.currentPageIdx + "\n\tcurentPageDisplay: " + curentPageDisplay);
			if (curentPageDisplay != "") _sb_e.sb_page_entry_field.value = curentPageDisplay;
		}

		// enable/disable the page PDF buttons accordingly
		if (_sb_s.pagePDF_buttonCont) {	// may not already be here
			if ((_sb_s.pageTurnMode == 'turn') && (_sb_s.pageDisplay == 'double')) {
				// left pages
				if (_sb_s.pageNumLeft > 0) _sb_s.pagePDF_buttonCont.css('display', 'block');
				else _sb_s.pagePDF_buttonCont.css('display', 'none');
				// right pages
				if (_sb_s.pageNumRight > 0) _sb_s.pagePDF_buttonContR.css('display', 'block');
				else _sb_s.pagePDF_buttonContR.css('display', 'none');
			}
		}

		// adjust page thumbs scroller
		//already done by goto_page!!	goto_thumbs_page(_sb_s.currentPageIdx, true);
		
		_sb_s.pageIsScrolling = false;	// page turn complete


		// call registered CBs after page has turned
		for (cbidx = 0; cbidx < after_pageTurn_custom_CBs.length; cbidx++) {
			try { after_pageTurn_custom_CBs[cbidx](_sb_s.currentPageIdx, curentPageDisplay); } catch(ex){}
		}


		//log_methods.log("CALLBACK cb_turn_end: " + evtmsg + " page: " + page + "\n\ttotal pages: " + _sb_s.totalPages + "\n\tpageNumLeft: " + _sb_s.pageNumLeft + "\n\tpageNumRight: " + _sb_s.pageNumRight + "\n\tpageIsScrolling: " + _sb_s.pageIsScrolling);
	},

	cb_before_removepage = function(evtmsg, pageID) {
		//log_methods.log("CALLBACK cb_before_removepage:\n\t'" + evtmsg + "' pageID: " + pageID);
		_sb_fn.detachGestureDetector(pageID);	// remove all gesture detectors on this page (like 'pv_P3')
	},
	cb_after_addpage = function(evtmsg, pageID) {
		//log_methods.log("CALLBACK cb_after_addpage:\n\t'" + evtmsg + "' pageID: " + pageID);
		attachGestureDetector(pageID,null,"pageimage_"+pageID);
	};


	var transition_property = "",
		transition_duration = "",
		transition_timing_function = "",
		transition_delay = "",
		transitionText = "",
		transform_property = "",
		TRANSITION_END = _sb_s.transitionEndEvent,
		TRANSITION_END_TIMER = null;

	/**
	 * Slide/step to a given X position
	 * call like:
	 *		touchScroll("thedivid");
	 *
	 * @param {Object} elem the pages container like sb_pagelist
	 * @param {number} dXstep	the X position, may be positive to scroll right, otherwise to left
	 * @param {number} numSteps	the number of steps
	 * @param {number} stepDelay	the wait between steps
	 * optional parameters;
	 * @param {boolean|null=} iseasing	
	 */
	$.slideXElement = function (elem, dXstep, numSteps, stepDelay, iseasing) {
		var easing = (iseasing ? iseasing : false),
			offsX;

		// get current slide offset X
		offsX = _sb_e.sb_pagelist.attr('data-offsX');
		if (offsX != undefined) _sb_s.pageTransitionCurrentX = parseFloat(offsX);
		else {
			_sb_s.pageTransitionCurrentX = 0;
			_sb_e.sb_pagelist.attr('data-offsX',_sb_s.pageTransitionCurrentX);
		}

		_sb_s.pageTransitionCurrentX += dXstep;	// may be positive to scroll right, otherwise to left
		if (Math.abs(_sb_s.pageTransitionCurrentX) < 1) _sb_s.pageTransitionCurrentX = 0;	// round down very small values
		_sb_e.sb_pagelist.attr('data-offsX',_sb_s.pageTransitionCurrentX);

		//THIS is SLOWER: translateX vs. translate3D!!!
		//if (_sb_s.nativeTransform != "") elem.css(_sb_s.nativeTransform,"translateX("+Math.ceil(_sb_s.pageTransitionCurrentX)+"px)");
		if ((_sb_s.nativeTransform != "") && _sb_s.is_IE && (_sb_s.IEVersion <= 9)) {
			elem.css(_sb_s.nativeTransform,"translate("+Math.ceil(_sb_s.pageTransitionCurrentX)+"px,0)");	// IE9 2D transforms only
		}
		else if (!_sb_s.is_Opera && (_sb_s.nativeTransform != "")) elem.css(_sb_s.nativeTransform,"translate3D("+Math.ceil(_sb_s.pageTransitionCurrentX)+"px,0,0)");	//DOES NOT WORK IN OPERA
			 else elem.css("left",_sb_s.pageTransitionCurrentX+"px");

		//log_methods.log("slideXElement newX: " + _sb_s.pageTransitionCurrentX + "\n\tsteps: " + numSteps + ", stepDelay: " + stepDelay + ", nativeTransform: " + _sb_s.nativeTransform);
		
		if (numSteps > 1) {
			numSteps--;
			if (!(_sb_s.is_IE && (_sb_s.IEVersion <= 9))
				&& !easing && (numSteps == 1)) {	// last step: we start easing
				numSteps = 5;
				dXstep = dXstep / numSteps;
				easing = true;
				//stepDelay /= 2;
				stepDelay = 1;
			}
			setTimeout(function(){$.slideXElement(elem, dXstep, numSteps, stepDelay, easing);},stepDelay);
			return;
		}
		_g_turnjs_pageIsFolding = _sb_s.pageIsScrolling = false;
		//log_methods.log("slideXElement END X: " + _sb_s.pageTransitionCurrentX);
		_sb_s.pageTransitionCurrentX = null;
	};

	/**
	 * Slide page
	 *
	 * @param {number} pidx The the zero based page index  to slide to
     * optional parameters;
	 * @param {number|null=} duration The transition duration
	 * @param {boolean|null=} force Just do it
	 *
	 * @return {number} The page number we have slided to
	 */
	var slide_page = function (pidx, duration, force) {
		//alert("slide to pidx: " + pidx + "\n_sb_s.pageIsScrolling: " + _sb_s.pageIsScrolling + "\n\n_sb_s.nativeTransform: " + _sb_s.nativeTransform);
		if (_sb_s.pageIsScrolling) return(pidx);	// still scrolling
		if (!force && (_sb_s.currentPageIdx == pidx)) return(pidx);
		_sb_s.pageIsScrolling = true;

		if (_sb_s.canvas_available) _sb_fn.clear_all_shadows(0);

		var newOffsetX, offsX,
			numSteps,
			dXtotal,	// may be positive to scroll right, otherwise to left
			dXstep,
			stepdelay = _sb_s.pageTransitionStepDelay,
			myduration = duration,
			myvendorPrefix = _sb_s.vendorPrefix;
		if (_sb_s.is_IE) {
			if ((_sb_s.IEVersion >= 10) && (myvendorPrefix == "-ms-")) myvendorPrefix = "";	// IE10 > uses no transform prefix, IE9 does
			if (_sb_s.IEVersion <= 9) _sb_s.nativeTransform = "";	// IE9 and older > no native transitions
		}
		if (typeof(myduration) == 'undefined') myduration = _sb_s.pageTransitionDuration;
		if ((myduration != 0) && (_sb_s.pageTransitionStepDelay != 0) && (_sb_s.pageTransitionNumSteps != 0)) {
			numSteps = myduration / _sb_s.pageTransitionNumSteps;
			if (_sb_s.nativeTransform == "") numSteps = 6;	// more speed for CSS slide
			numSteps = Math.floor(numSteps);	// make integer
		}
		else numSteps = 1;

		// get current slide offset X
		offsX = _sb_e.sb_pagelist.attr('data-offsX');
		if (offsX != undefined) _sb_s.pageTransitionCurrentX = parseFloat(offsX);
		else {
			_sb_s.pageTransitionCurrentX = 0;
			_sb_e.sb_pagelist.attr('data-offsX',_sb_s.pageTransitionCurrentX);
		}

		if (!(_sb_s.is_IE && (_sb_s.IEVersion <= 9)) && _sb_s.nativeTransform != "") {
			newOffsetX = pidx * _sb_s.pageToPageOffsetWidth;
		}
		else {
			if (!force) newOffsetX = (pidx - _sb_s.currentPageIdx) * _sb_s.pageToPageOffsetWidth;
			else newOffsetX = _sb_s.pageTransitionCurrentX + (pidx  * _sb_s.pageToPageOffsetWidth);
		}

		dXtotal = -newOffsetX,	// may be positive to scroll right, otherwise to left
		dXstep = dXtotal / numSteps;

		//log_methods.log("slide_page current: " + _sb_s.currentPageIdx + "\n\tcurrentX: " + _sb_s.pageTransitionCurrentX + "\n\tgoto pidx: " + pidx + "\n\t_sb_s.pageToPageOffsetWidth: " + _sb_s.pageToPageOffsetWidth + "\n\tdXtotal: " + dXtotal + "\n\tdXstep: " + dXstep + "\n\tnumSteps: " + numSteps + "\n\tstepDelay: " + _sb_s.pageTransitionStepDelay + "\n\tduration: " + _sb_s.pageTransitionDuration + "\n\tnativeTransform: " + _sb_s.nativeTransform);
		_sb_s.currentPageIdx = pidx;

		//log_methods.log("nativeTransform:\n" + _sb_s.nativeTransform + "\nTRANSITION_END:\n" + TRANSITION_END);
		if (_sb_s.allowNativeTransitions && (_sb_s.nativeTransform != "") && (_sb_s.transitionEndEvent != "")) {	// do native transition but not IE9: IE9 has no transitionend event and no transition-duration
			if (transition_property == "") {	// setup transition
				transition_property = myvendorPrefix + "transition-property:"+myvendorPrefix+"transform; ";
				transition_duration = myvendorPrefix + "transition-duration:"+_sb_s.pageTransitionDuration+"ms; ";	// transition-duration property is not supported in Internet Explorer 9 and earlier 
				transition_timing_function = myvendorPrefix + "transition-timing-function:"+_sb_s.pageTransitionEasing+"; ";
				transition_delay = myvendorPrefix + "transition-delay:0ms; ";
				transitionText = transition_property + transition_duration + transition_timing_function + transition_delay;
				//log_methods.log(TRANSITION_END + "\n_sb_s.CamelVendorPrefix: " + _sb_s.CamelVendorPrefix + "\n_sb_s.pageToPageOffsetWidth: " + _sb_s.pageToPageOffsetWidth);
				addEventHandler(_sb_e.sb_pagelistEl,
								TRANSITION_END,
								function(e) {
									//log_methods.log("*****" + TRANSITION_END + "\n_sb_s.currentPageIdx: " + _sb_s.currentPageIdx+ "\nTRANSITION_END_TIMER: " + TRANSITION_END_TIMER);
									if (TRANSITION_END_TIMER != null) {
										clearTimeout(TRANSITION_END_TIMER); TRANSITION_END_TIMER = null;
									}
									//alert(TRANSITION_END + "\n\n_sb_s.currentPageIdx: " + _sb_s.currentPageIdx);
									_g_turnjs_pageIsFolding = _sb_s.pageIsScrolling = false;
								},
								false);
			}
			transform_property = "matrix(1,0,0,1,"+dXtotal+",0)";	//"translate3D("+dXtotal+"px,0,0);";
			var oldCssText = _sb_e.sb_pagelistEl.style.cssText;

			if (oldCssText.indexOf("transition") < 0) _sb_e.sb_pagelistEl.style.cssText += transitionText + _sb_s.nativeTransform + ":" + transform_property;	// not already set
			else {
				//alert("prop nativeTransform:" + _sb_e.sb_pagelist.css(_sb_s.nativeTransform));
				_sb_e.sb_pagelist.css(_sb_s.nativeTransform,"matrix(1,0,0,1,"+dXtotal+",0)");
			}
			_sb_e.sb_pagelist.attr('data-offsX',dXtotal);
			// make sure we are never blocked
			if (TRANSITION_END_TIMER != null) clearTimeout(TRANSITION_END_TIMER);
			TRANSITION_END_TIMER = setTimeout("_g_turnjs_pageIsFolding = _sb_s.pageIsScrolling = false;",(_sb_s.pageTransitionDuration*3));
			//alert("oldCssText: " + oldCssText + "\n\ntransitionText: " + transitionText + "\n\ntransform_property: " + transform_property + "\n\nstyle.cssText: " + _sb_e.sb_pagelistEl.style.cssText);
		}
		else {
			//log_methods.log("NONATIVE TRANSFORM");
			$.slideXElement(_sb_e.sb_pagelist, dXstep, numSteps, stepdelay);
		}
		return(_sb_s.currentPageIdx);
	};

	_sb_fn.showLabeledArticle = function (label) {
		var elems = document.getElementsByTagName("div"),
			mylabel = "#" + label + "#",
			artid = "",
			attr,
			l,
			pageIdx;
			//alert("label: " + mylabel + "\nnum: " +elems.length );
			for (l = 0; l < elems.length; l++) {
				attr = elems[l].getAttribute("data-label");
				if (attr && (attr.indexOf(mylabel) >= 0)) {
					//alert("found label: " + attr + "\nid: " + elems[l].parentNode.getAttribute("id"));
					artid = elems[l].parentNode.getAttribute("id");	// get article id like id="Art82_13"
					break;
				}
			}
			//alert("artid: " + artid + "\npage: " + _sb_s.funcs.getPageIDXFromArticleID(artid));
			if ((artid != null) && (artid != "")) {
				pageIdx = _sb_s.funcs.getPageIDXFromArticleID(artid);
				goto_page(pageIdx,true,null,null,true);
				show_article(null,artid,"",true);
			}
	};

	/**
	 * Flip to a given page
	 * call like:
	 *		goto_page(10, false);
	 *
	 * @param {string|number} the_page the page number
	 * @param {boolean} isPageIndex if this is zero based page number
	 * optional parameters;
	 * @param {number|null=} duration the duration of the page transition
	 * @param {string|null=} easing the css transition easing type
	 * @param {boolean|null=} setThumbsScroll true to also set the page thumbs scroller
	 * @param {Object|null=} theevent object if availabe
	 * @param {string|null=} caller who is calling us (debugging)
	 * @param {boolean|null=} force just do it
	 * @param {boolean|null=} detectSwipe detect if we are swiping
	 */
	// public function (var goto_page is defined at top around line 42)
	var goto_page = function (the_page, isPageIndex, duration, easing, setThumbsScroll,theevent,caller,force,detectSwipe) {
		var myPage, myDuration, myEasing, lpage,
			currentPage, lastPageIndex, havescrolled, cbidx;
		/*
		log_methods.log("goto_page()"
			 + "\nthe_page: " + the_page
			 + "\nisPageIndex: " + isPageIndex
			 + "\nduration: " + duration
			 + "\neasing: " + easing
			 + "\nsetThumbsScroll: " + setThumbsScroll
			 + "\ntheevent: " + theevent
			 + "\ncaller: " + caller
			 + "\n\n_sb_s.currentPageIdx: " + _sb_s.currentPageIdx
			 + "\n_sb_s.pageToPageOffsetWidth: " + _sb_s.pageToPageOffsetWidth
			 + "\n_sb_s.pageTransitionDuration: " + _sb_s.pageTransitionDuration
			 + "\n_sb_s.pageTransitionEasing: " + _sb_s.pageTransitionEasing
			 );
		*/
		//log_methods.log("goto_page #" +the_page + "\n\tTOC_isShown: " + _sb_s.TOC_isShown + "\n\tclose_lightbox_if_TOC_isClicked: " + _sb_s.close_lightbox_if_TOC_isClicked + "\n\tcaller: " + caller);
		// detect if pointer was moved (on a link) or if it was a tap
		//log_methods.log("goto_page detectSwipe: " + detectSwipe + "\n\tisSwipe: " + _sb_fn.touchDetector_vars.isSwipe);
		if (typeof(detectSwipe) != 'undefined' && (detectSwipe == true)) {
			if (_sb_fn.touchDetector_vars.isSwipe) return(false);
		}

		if (typeof(the_page) == 'undefined' || the_page === null) return(false);

		//if (_sb_s.TOC_isShown && _sb_s.close_lightbox_if_TOC_isClicked) {	// true when the TOC is shown in the article window: close it
			switch (caller) {
				case "TocButton":	// called from TOC-button to go to TOC page ( do not close lightbox because it now contains the TOC
				case "goto_continued_article":// close lightbox only if we do not step to the next TOC part
					break;
				case "noLBcloseClearShadows":// do not close lightbox if caller doesn't want
					current_article.valid=false;
					_sb_fn.clear_all_shadows(0);
					break;
				default: 
					hideLightbox();
					break;
			}
		//}

		// we should not halt this event because the thumbs list will not get the 'touchend' after clicking on a page thumb
		/*
		if (theevent) {
			theevent.preventDefault();
			//try { theevent.stopPropagation(); theevent.preventDefault(); }
			//catch (err) { theevent.cancelBubble = true; }
		}
		*/
		if ((typeof(duration) != 'undefined') && (duration != null)) myDuration = duration;
		else myDuration = _sb_s.pageTransitionDuration;
		if (easing) myEasing = easing;
		else myEasing = _sb_s.pageTransitionEasing;
		myPage = -98;
		if ((the_page==-1) || (the_page==-2)) myPage = the_page;
		else {
			if (isPageIndex) myPage = parseInt(the_page, 10);
			else myPage = getPageIndexFromPageName(the_page);
			isPageIndex = true;
		}

		currentPage = _sb_s.currentPageIdx;	// get current page from flip book
		lastPageIndex = _sb_s.totalPages - 1;
		if (myPage < -2) {
			// call registered CBs after goto page success
			for (cbidx = 0; cbidx < after_gotoPage_custom_CBs.length; cbidx++) {
				try { after_gotoPage_custom_CBs[cbidx](-2,_sb_s.currentPageIdx,the_page,lastPageIndex, 'RANGE_ERROR'); } catch(ex){}
			}
			return(false);
		}
		havescrolled = false;
		/*
		_sb_s.DEBUGmode = 2; 
		log_methods.log("goto_page myPage: " + myPage
			+"\ncurrentPage: " + currentPage
			+"\nlastPageIndex: " + lastPageIndex
			);
		*/
		switch (myPage) {
			case -1:	// goto next page
				if (currentPage < lastPageIndex) {
					preventDocumentScroll(null);
					switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
						default:
						case "turn":
							_sb_e.sb_pagelist.turn('next');
							_sb_s.currentPageIdx = _sb_e.sb_pagelist.turn("page")-1;
							break;
						case "slide":
							slide_page(_sb_s.currentPageIdx + 1, myDuration);
							break;
					}
					myPage = _sb_s.currentPageIdx;
					//log_methods.log("goto_page NEXT flipped to next page: " + _sb_s.currentPageIdx);
					havescrolled = true;
				}
				else return(false);
				break;
			case -2:	// goto previous page
				if (currentPage > 0) {
					preventDocumentScroll(null);
					switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
						default:
						case "turn":
							_sb_e.sb_pagelist.turn('previous');
							_sb_s.currentPageIdx = _sb_e.sb_pagelist.turn("page")-1;
							break;
						case "slide":
							slide_page(_sb_s.currentPageIdx - 1, myDuration);
							break;
					}
					myPage = _sb_s.currentPageIdx;
					//log_methods.log("goto_page PREVIOUS flipped to previous page: " + _sb_s.currentPageIdx);
					havescrolled = true;
				}
				else return(false);
				break;
			default:
				if (!isPageIndex) myPage--;
				if (myPage < 0) myPage = 0;
				if (myPage > lastPageIndex) myPage = lastPageIndex;
				/*
				log_methods.log("goto_page myPage: " + myPage
					+"\ncurrentPage: " + currentPage
					+"\n_sb_s.currentPageIdx: " + _sb_s.currentPageIdx
					+"\nlastPageIndex: " + lastPageIndex
					+"\nforce: " + force
					);
				*/
				if (force || ((myPage >= 0) && (myPage <= lastPageIndex) && (myPage != _sb_s.currentPageIdx)) ) {
					preventDocumentScroll(null);
					switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
						default:
						case "turn":
							_sb_e.sb_pagelist.turn('page',myPage+1);	// call as page sequences for flip book
							_sb_s.currentPageIdx = _sb_e.sb_pagelist.turn("page")-1;
							break;
						case "slide":
								/*
								log_methods.log("goto_page() (myPage idx): " + myPage
									 + "\n\t_sb_s.currentPageIdx: " + _sb_s.currentPageIdx
									 + "\n\t_sb_s.pageToPageOffsetWidth: " + _sb_s.pageToPageOffsetWidth
									 );
								*/
								slide_page(myPage, myDuration);
							break;
					}

					//alert(_sb_s.currentPageIdx);
					havescrolled = true;
				}
				else {
					if (myPage != _sb_s.currentPageIdx) {	// don't say anything - we are on this page
						// call registered CBs after goto page success
						for (cbidx = 0; cbidx < after_gotoPage_custom_CBs.length; cbidx++) {
							try { after_gotoPage_custom_CBs[cbidx](-1,_sb_s.currentPageIdx,myPage,lastPageIndex, 'RANGE_ERROR'); } catch(ex){}
						}
					}
					return(false);
				}
		}
		// update the goto page field
		if (_sb_s.sb_page_entry_field_behaviour & 2) {
			var curentPageDisplay = getPageNameFromPageIndex(_sb_s.currentPageIdx);
			//log_methods.log("goto_page goto page field - _sb_s.currentPageIdx: " + _sb_s.currentPageIdx + "\n\tcurentPageDisplay: " + curentPageDisplay);
			if (curentPageDisplay != "") _sb_e.sb_page_entry_field.value = curentPageDisplay;
		}
	
		// make sure the page image is here
		if (myPage >= 0) {
			$.load_page("pv_P"+(myPage+1),myPage,_sb_fn.getPageImage(myPage),false);
				//log_methods.log("goto_page load requested pg idx: "+myPage);
		}

		// as not all page images are preloaded, we have to load following page images
		if (_sb_s.pageImageMinFollowing > 0) {
			// load 1 following and 1 preceding page now
			lpage = myPage+1;
			if (lpage < num_epaper_pages) {
				$.load_page("pv_P"+(lpage+1),lpage,_sb_fn.getPageImage(lpage),false);
				//log_methods.log("goto_page load following pg idx: "+lpage);
			}
			lpage = myPage-1;
			if (lpage >= 0) {
				$.load_page("pv_P"+(lpage+1),lpage,_sb_fn.getPageImage(lpage),false);
				//log_methods.log("goto_page load preceding pg idx: "+lpage);
			}

			// load more following pages
			for (lpage = myPage+2; (lpage <= myPage+_sb_s.pageImageMinFollowing) && (lpage < num_epaper_pages); lpage++ ) {
				//log_methods.log("goto_page load following pg idx: "+lpage);
				$.load_page("pv_P"+(lpage+1),lpage,_sb_fn.getPageImage(lpage),false);
			}
			// load more preceding pages
			for (lpage = myPage-2; (lpage >= myPage-_sb_s.pageImageMinFollowing) && (lpage >= 0); lpage-- ) {
				//log_methods.log("goto_page load preceding pg: "+lpage);
				$.load_page("pv_P"+(lpage+1),lpage,_sb_fn.getPageImage(lpage),false);
			}
		}
		// may be we want to unload currently invisible page images?
		if ((_sb_s.pageImageUnloadUnused != 0) && (_sb_s.pageImageLoadType > 0)) {
			setTimeout("$.pageImageUnloadUnused();",1000);
		}

		if (havescrolled && setThumbsScroll) {
			if (setThumbsScroll) goto_thumbs_page(_sb_s.currentPageIdx, true);
//			_sb_fn.detachGestureDetector("c*");	// remove all gesture detectors on area elements like <area id="c1P1_A1_1" ....
		}

		// call registered CBs after goto page success
		for (cbidx = 0; cbidx < after_gotoPage_custom_CBs.length; cbidx++) {
			try { after_gotoPage_custom_CBs[cbidx](0,_sb_s.currentPageIdx,myPage,lastPageIndex, 'OK'); } catch(ex){}
		}
		//log_methods.log("goto_page currentPageIdx: " + _sb_s.currentPageIdx );

		return(false);
	};
	_sb_fn.gotoPage = goto_page;

	// public function
	handleGotoPageFieldKeyPress = function (e) {
		var key=e.keyCode || e.which;
		//log_methods.log("handleGotoPageFieldKeyPress key: " + key);
		if (key==13) {
			hideLightbox(e);
			manual_gotopage(e,_sb_e.sb_page_entry_field.value,false);
		}
	};

	// public function
	manual_gotopage = function (e, thepage, isPageIndex) {
		if (typeof(thepage) == "undefined") return(false);
		var pageindex = getPageIndexFromPageName(thepage);
		//log_methods.log("manual_gotopage: " + e + "\n\tthepage: " + thepage + "\n\tisPageIndex: " + isPageIndex + "\n\tpageindex: " + pageindex);
		if (pageindex == -99) {
			if (thepage.indexOf("-") == 0) {	// like -2 - then count from end backward
				try {
					var minuspage = parseInt(thepage, 10);
					pageindex = _sb_s.totalPages - 1 + minuspage;
					if (pageindex < 0) pageindex = 0;
				} catch(ex) { return(false); }
			}
			else {	// a positive number?
				try {
					var page = parseInt(thepage, 10),
						newpageindex;
					if (page > _sb_s.totalPages-1) pageindex = _sb_s.totalPages - 1;
					else {
						newpageindex = getPageIndexFromPageName(page);
						if (newpageindex == -99) {
							pageindex = 0;	// goto first page
						}
						else {
							pageindex = _sb_s.currentPageIdx + page;
						}
					}
				} catch(ex) { return(false); }
			}
		}
		goto_page(pageindex,true,null,null,true);
		return(false);
	};

	
	// data-label attributes stuff
	var data_labels = new Array(),
		data_labels_clear = function() {
			data_labels = new Array();
		},
		data_labels_store = function(labelstring) {
			//var split_labels = labelstring.split("##"),
			var split_labels = labelstring.split("#"),
				i;
			for (i = 0; i < split_labels.length; i++) {
				if (split_labels[i] != "") data_labels[data_labels.length] = split_labels[i];
			}
			//alert("data_labels_store: " + data_labels);
		},
		data_labels_isset = function(whichattrib) {
			if ( (whichattrib == null) || (whichattrib == "") ) return false;
			if ( data_labels.length <= 0 ) return false;
			for (var i = 0; i < data_labels.length; i++) {
				if (data_labels[i].indexOf(whichattrib) == 0) return true;
			}
			return false;
		},
		data_labels_get = function(whichattrib) {
			if ( (whichattrib == null) || (whichattrib == "") ) return false;
			if ( data_labels.length <= 0 ) return("");
			for (var i = 0; i < data_labels.length; i++) {
				if (data_labels[i].indexOf(whichattrib) == 0) return(data_labels[i].substr(data_labels[i].lastIndexOf(whichattrib) + whichattrib.length,data_labels[i].length));
			}
			return "";
		},
		data_labels_getall = function() {
			return data_labels;
		},
		data_labels_length = function() {
			return data_labels.length;
		};
	// make these functions accessible globally
	_sb_fn.data_labels_clear = data_labels_clear;
	_sb_fn.data_labels_store = data_labels_store;
	_sb_fn.data_labels_isset = data_labels_isset;
	_sb_fn.data_labels_get = data_labels_get;
	_sb_fn.data_labels_getall = data_labels_getall;
	_sb_fn.data_labels_length = data_labels_length;



	/**
	 * Show the next article
	 * call like:
	 *		show_next_article(true, false);
	 *
	 * @param {boolean} flippage also flip to this page
	 * optional parameters;
	 * @param {boolean|null=} showLastArticle true to show the very last article
	 */
	var show_next_article = function (flippage, showLastArticle) {
		var artid = "", pg, aidx, debugmess, prev;
		if ((_sb_s.lastShownArticleID == "") || showLastArticle) {
			if (showLastArticle) {	// get last article index
				debugmess = document.getElementById("debugmessage");	// is last div
				prev = debugmess.previousSibling;
				while (true) {	// 1 = get element nodes
																// 2 = attribute node
																// 3 = text node
							//log_methods.log("prev article: " + prev.id + "\n\tprevious nodeType: " + prev.nodeType + "\n\tprevious nodeName: " + prev.nodeName + "\n\tprevious id: " + prev.id);
					if (prev.nodeType == 1) {	// is a div?
						artid = prev.getAttribute("id");	// we get the article containers only
						if ((typeof(artid) != 'undefined') && (artid != null) && (artid != "") && (artid.indexOf("Art") == 0))  {
							// found it!
							pg = getPageIDXFromArticleID(artid)+1;
							aidx = getArticleIDXFromArticleID(artid);
							//log_methods.log("last article: " + prev.id + "\n\tprevious nodeType: " + prev.nodeType + "\n\tprevious nodeName: " + prev.nodeName + "\n\tprevious id: " + prev.id);
							break;
						}
					}
					prev = prev.previousSibling;
					if (!prev) {
						artid = "";
						break;
					}
				}
			}
			else artid = step_article(0,1,0);
		}
		else {
			//document.getElementById("debugmessage").innerHTML = "show_next_article - start at lastShownArticleID: " + _sb_s.lastShownArticleID;
			pg = getPageIDXFromArticleID(_sb_s.lastShownArticleID)+1;
			aidx = getArticleIDXFromArticleID(_sb_s.lastShownArticleID);
			artid = step_article(aidx,pg,1);
			//log_methods.log("show_next_article: " + _sb_s.lastShownArticleID + "\n\tpg: " + pg + "\n\taidx: " + aidx + "\n\tnext artid: " + artid);
			pg = getPageIDXFromArticleID(artid);
		}
		if (artid != "") {
			//log_methods.log("show_next_article: next page: pg: " + pg + "\n\taidx: " + aidx + "\n\tnext artid: " + artid);
			show_article_xml(null,artid,'','','','','',pg);
			if (flippage == true) {
				preventDocumentScroll(null);
				switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
					default:
					case "turn":
						_sb_e.sb_pagelist.turn('page',pg+1);
						break;
					case "slide":
						goto_page(pg,true,null,null,true);
						break;
				}
			}
		}
	};

	/**
	 * Show the previous article
	 * call like:
	 *		show_previous_article(true, false);
	 *
	 * @param {boolean} flippage also flip to this page
	 * optional parameters;
	 * @param {boolean|null=} showFirstArticle true to show the very first article
	 */
	var show_previous_article = function (flippage, showFirstArticle) {
		var artid = "", pg, aidx;
		if ((_sb_s.lastShownArticleID == "") || showFirstArticle) {
			artid = step_article(0,1,0);
			pg = getPageIDXFromArticleID(artid);
		}
		else {
			//document.getElementById("debugmessage").innerHTML = "show_previous_article - start at lastShownArticleID: " + _sb_s.lastShownArticleID;
			pg = getPageIDXFromArticleID(_sb_s.lastShownArticleID)+1;
			aidx = getArticleIDXFromArticleID(_sb_s.lastShownArticleID);
			artid = step_article(aidx,pg,-1);
			pg = getPageIDXFromArticleID(artid);
			//document.getElementById("debugmessage").innerHTML += "<br>show_previous_article  - " + artid + " - on page idx: " + pg + "     " + new Date().getTime();
		}
		if (artid != "") {
			show_article_xml(null,artid,'','','','','',pg);
			if (flippage == true) {
				preventDocumentScroll(null);
				switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
					default:
					case "turn":
						_sb_e.sb_pagelist.turn('page',pg+1);
						break;
					case "slide":
						goto_page(pg,true,null,null,true);
						break;
				}
			}
		}
	};
	
	var step_article = function (theartIDX,thepage,thedelta) {
		var artfound = "",
			ai = 0,
			artIDX = theartIDX,
			artID, articletext,
			page = thepage,
			delta = thedelta,
			divs = document.getElementsByTagName("div");
		if ((typeof(artIDX) == 'undefined') || (artIDX == null)) artIDX = 0;	// set first article in document
		if ((typeof(page) == 'undefined') || (page == null)) page = 1;		// set first page in document
		if ((typeof(delta) == 'undefined') || (delta == null)) delta = 0;	// no delta
		// find the article specified with artIDX and page
		//log_methods.log("step_article start - theartIDX: " + theartIDX + "\n\tthepage: " + thepage + "\n\tthedelta: " + thedelta);
		for (ai = 0; ai < divs.length; ai++) {
			artID = divs[ai].getAttribute("id");	// we get the article containers only
			if ((typeof(artID) == 'undefined') || (artID == null) || (artID == "")) continue;
			if (artID.indexOf("Art") != 0) continue;
		
			if (artID == "Art"+artIDX+"_"+page) {
				if (article_has_dirlink(artID)) continue;
				articletext = get_plain_text(artID,-1);	// get all plain text
				if (articletext == "") { artIDX++; continue; }	// get next non empty article
				artfound = "Art"+artIDX+"_"+page;
				break;
			}
		}
		//log_methods.log("step_article found start - article ID: " + artfound);
		_sb_s.lastShownArticleID = artfound;
		if ((artfound != "") && (delta != 0)) {
			for (ai = ai+delta; (delta > 0 ? (ai < divs.length) : (ai >= 0)); (delta > 0 ? ai++ : ai--)) {
				artID = divs[ai].getAttribute("id");	// we get the article containers only
				if ((typeof(artID) == 'undefined') || (artID == null) || (artID == "")) continue;
				if (artID.indexOf("Art") != 0) continue;
				if (article_has_dirlink(artID)) continue;
				articletext = get_plain_text(artID,-1);	// get all plain text
				if (articletext == "") continue;
			
				artfound = _sb_s.lastShownArticleID = artID;
				break;
			}
		}

		//document.getElementById("debugmessage").innerHTML += "<br>&nbsp;&nbsp;* step_article next: " + artfound + "     " + new Date().getTime();
		return(artfound);
	},
	// check if article has a direct link to open in new window
	// search for divs like
	// <div class="Artcl_container" ... data-label="dirlinkn:http://www.bmw.ch">
	// with a 'dirlinkn:' or  'dirlink:' keyword in the data-label attribute

	article_has_dirlink = function (articleID) {
		if ((articleID == null) || (articleID == "")) return("");
		var article = document.getElementById(articleID),
			childnodes = article.childNodes,
			label,
			i;

		for (i = 0; i < childnodes.length; i++) {
			//alert("nodetype: " + childnodes[i].nodeType);
			if (childnodes[i].nodeName.toUpperCase() == "DIV") {
				label = childnodes[i].getAttributeNode("data-label");	// we get the data-label attribute
				if ((typeof(label) == 'undefined') || (label == null) || (label == "")) continue;
				if (label.nodeValue.indexOf("dirlink") >= 0) {
					//alert(label.nodeValue);
					return(true);
				}
			}
		}
		return(false);
	};


	var in_show_article_xml = false,	// semaphore to prevent multiple calls within certain time (click events)
		in_show_article_xml_timer = null,
		F = null, view_X=10, view_Y=10,
	
	/**
	 * Show an article in the article lightbox
	 * call like:
	 *		logmethos.log('itsme', 'my message to show');
	 *
	 * @param {Object|null} obj the element we are called from
	 * @param {string} id the element id we are called from
	 * @param {number|string|null} scale the scale value of the page jpeg
	 * @param {string|null} jpg the url to the article jpeg
	 * @param {string|null} xml url to the xml file this article is contained in
	 * @param {boolean|string|null} hasWWWlink (obsolete) null or 0 or 1 if the article contains a www link
	 * @param {boolean|string|null} hasExtPDF (obsolete) null or the name of an external PDF
	 * @param {string} the_pagenumber the page number this article is on
	 * optional parameters;
	 * @param {string|null=} theArticleWindowName the name of the window if the article has to shown in a new window
	 * @param {string|null=} theArticleWindowDim the new window dimensions as comma separated string. Results in: screenX=" + articleWindowDimensions[0] + ", screenY=" + articleWindowDimensions[1] + ", left=" + articleWindowDimensions[0] + ", top=" + articleWindowDimensions[1] + ", width=" + articleWindowDimensions[2] + ",height=" + articleWindowDimensions[3]
	 * @param {string|null=} theWindowTitle the title of the new window
	 */
	show_article_xml = function (obj,id,scale,jpg,xml,hasWWWlink,hasExtPDF,the_pagenumber,theArticleWindowName,theArticleWindowDim,theWindowTitle) {
		//log_methods.log("show_article_xml id: " + id +"\n\tthe_pagenumber: " + the_pagenumber + "\n\tisSwipe: " + gestureDetectorPointer.isSwipe + "\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding + "\n\tpageIsScrolling: " + _sb_s.pageIsScrolling);

		if (_g_turnjs_pageIsFolding === true) return true;	// mainly in IE 11, a click may fire after a 'pointerend'
		//if (gestureDetectorPointer.isSwipe) return true;
		if (in_show_article_xml == true) return(false);
		if (_sb_s.slidebook_load_state.indexOf("1") < 0) {	// body text must be loaded
			if (in_show_article_xml_timer != null) { clearTimeout(in_show_article_xml_timer); in_show_article_xml_timer = null; }
			in_show_article_xml_timer = setTimeout(function(){show_article_xml(obj,id,scale,jpg,xml,hasWWWlink,hasExtPDF,the_pagenumber,theArticleWindowName,theArticleWindowDim,theWindowTitle);}, 100);
			return(false);
		}
		if (in_show_article_xml_timer != null) { clearTimeout(in_show_article_xml_timer); in_show_article_xml_timer = null; }
		in_show_article_xml = true;

		var anchors,
			i,
			artobj,
			wid=400, hig=500,
			articleID = "" + id;	// may be the whole id given as 'Art12_2' OR ARTICLE'S ID NUMBER
		if (articleID.indexOf("Art") < 0) articleID = "Art" + id + "_" + the_pagenumber;

		artobj=document.getElementById(articleID);
		if (!artobj) {	//is not available: do nothing
			//alert("article not found: "+articleID);
			in_show_article_xml = false;
			return(false);
		}
		//log_methods.log("show_article_xml #2 on articleID:" + articleID + "\n\t_sb_s.slidebook_load_state: " + _sb_s.slidebook_load_state);

		_sb_s.lastShownArticleID = articleID;
		// re-target links pointing to internal name anchor like <a data-destinationStoryID="u442" href="#linkref_6" title="Read next article ->" data-destinationArticleID="Art15_4">Read next article -&gt;</a>
		anchors = artobj.getElementsByTagName("a");
		if (anchors.length > 0) {
			for (i = 0; i < anchors.length; i++) {
				try {
					if (!anchors[i].getAttribute("href")) continue;
					if (anchors[i].getAttribute("href").indexOf("#") == 0) {		// found href="#name" attr starting eith #
						var pageIdx,
							newhref,
							touchend,
							destinationArticleID = anchors[i].getAttribute("data-destinationArticleID");
						if (!destinationArticleID) continue;
						pageIdx = getPageIDXFromArticleID(destinationArticleID);
						newhref = "javascript:void(0)";
						touchend = "show_article(null,'"+destinationArticleID+"','','','','','','');goto_page("+pageIdx+",true,null,null,true);return(false);";

						anchors[i].setAttribute("href",newhref);
						anchors[i].setAttribute("on" + _sb_s.pointerEvents.end, touchend);
						//alert("newhref: " + newhref);
					}
				} catch(ex){}
			}
		}
		anchors = null;

		var cbidx,
			coordsAttr = artobj.getAttribute("data-coords"),
			content = artobj.innerHTML,
			re = /"Artcl_container"/;
		// enhance the Artcl_container with the data-coords attribute
		content = content.replace(re,'"Artcl_container" data-coords="' + coordsAttr + '"');

		// replace commented image links <!--image src="image.jpg" alt="iamge.eps"/image-->
		// either quirked by flipbook.xsl in browser or by transformer
		re = /\<\!--image/gi;	// we need the \! or closure compiler will compile it wrong
		content = content.replace(re,"<img");
		re = /\/image--\>/gi;
		content = content.replace(re,">");

		// add click for PDF,JPEG,GIF,PNG to img tags for above image links: original images are available
		content = add_image_pdf_click(content);
	
		// replace commented image links <!--nimage src="image.jpg" alt="iamge.eps"/image-->
		// original images are NOT available
		re = /\<\!--nimage/gi;	// we need the \! or closure compiler will compile it wrong
		content = content.replace(re,"<img");
		re = /\/image--\>/gi;
		content = content.replace(re,">");

		// clean other stuff
		try { content = clean_content(content, articleID, id, the_pagenumber); } catch(ex) {}	// defined in custom.js
		// UNCOMMENT TO DEBUG: content = clean_content(content, articleID, id, the_pagenumber);

		// after having done the standard clean_content, we call more custom handlers
		for (cbidx = 0; cbidx < clean_content_custom_CBs.length; cbidx++) {
			content = clean_content_custom_CBs[cbidx](content);
		}

		// if we do not have content to display: silently exit
		// content may have been changed by the call back functions
		if ((content) == null || (content == "")) {
			content = null;
			in_show_article_xml = false;
			return(false);
		}
	
		// check if we can display the article content right of flipping pages
		var show_article_in_new_window = 0, newwin;
		try {
			newwin = get_css_value("display_article_window","zIndex");	// try to get flag from css
			if ((typeof newwin != "undefined") && (newwin != "")) show_article_in_new_window = parseInt(newwin, 10);
		} catch(ex) {}

		//log_methods.log("show_article_xml #3 on obj.id:" +  (obj ? obj.id : "") + "\n\tshow_article_in_new_window: " + show_article_in_new_window + "\n\t_sb_s.slidebook_load_state: " + _sb_s.slidebook_load_state);
		if ( (show_article_in_new_window == 0) && ((_sb_e.lightbox != null) && (typeof(theArticleWindowName) == 'undefined')) ) {
			showLightbox(null, content,"text",articleID,false);
		}
		else {
			//this is used for direct epaper/article display within a new window
			var articleWinName = "myArticleWindow",
				articleWindowDimensions = null,	// array: x,y,w,h
				articleWinTitle = "&nbsp;";
			if ((theArticleWindowName != null) && (typeof(theArticleWindowName) != 'undefined') && (theArticleWindowName != "")) articleWinName = theArticleWindowName;
			//alert("articleWinName: '" + articleWinName + "'" + "\ntheArticleWindowName: '" + theArticleWindowName + "'");
			if ((typeof(theArticleWindowDim) != 'undefined') && (theArticleWindowDim != "")) articleWindowDimensions = theArticleWindowDim.split(",");
			if ((theWindowTitle != null) && (typeof(theWindowTitle) != 'undefined') && (theWindowTitle != "")) articleWinTitle = unescape(theWindowTitle);
	
			try { if (F && !F.closed)  { F.close(); F=null; }//browser compatibility
			} catch(ex) {}
			if (articleWindowDimensions != null) F=window.open("",articleWinName,"screenX=" + articleWindowDimensions[0] + ", screenY=" + articleWindowDimensions[1] + ", left=" + articleWindowDimensions[0] + ", top=" + articleWindowDimensions[1] + ", width=" + articleWindowDimensions[2] + ",height=" + articleWindowDimensions[3] + ",resizable=yes,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,dependent=yes");
			else F=window.open("",articleWinName,"screenX=" + view_X + ", screenY=" + view_Y + ", left=" + view_X + ", top=" + view_Y + ", width=" + wid + ",height=" + hig + ",resizable=yes,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,dependent=yes");
			F.document.write('<!DOCTYPE HTML>\r');
			F.document.write('<html><head>\r');
			F.document.write('<title>' + articleWinTitle + '</title>\r');
			F.document.write('<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">\r');
			F.document.write('<link rel="StyleSheet" href="' + document.getElementById("doctypeinfos").innerHTML + '" type="text/css" media="all">\r');
			F.document.write('<link rel="StyleSheet" href="' + _sb_s.xslcssPath + 'flipbook.css" type="text/css" media="all">\r');
			//if (_sb_s.xslcssPath) {
				//F.document.write('<script language="javascript" type="text/javascript" src="' + _sb_s.xslcssPath + 'slidebook.js"></script>\r');
			//}
			F.document.write('</head>\r<body>\r');
			if (_sb_s.xslcssPath) { //the div to display the www and mail icons
				F.document.write('<div id="qm_func" style="visibility:hidden;position:fixed;top:0px;right:0px;cursor:pointer;border:0px solid #000000;height:0px;vertical-align:middle;font-family:Verdana;font-size:9pt;text-align:left;">');
				F.document.write('<img border="0" id="qm_func1" align="top" src="' + _sb_s.xslcssPath + 'earth.gif" vspace="1" hspace="1" style="cursor:pointer;height:22px;background-color:transparent;vertical-align:middle" alt="" title="web" onMouseOver="sub_f(1,1)" onMouseOut="sub_f(1,0)" onClick="sub_do(1)">');
				F.document.write('<img border="0" id="qm_func2" src="' + _sb_s.xslcssPath + 'email.gif" vspace="1" hspace="1" style="cursor:pointer;height:22px;background-color:transparent;vertical-align:middle" alt="" title="e-mail" onMouseOver="sub_f(2,1)" onMouseOut="sub_f(2,0)" onClick="sub_do(2)">');
				F.document.write('</div>');
			}
				//the article's text
			F.document.write('<div style="float:left;border:0px solid #0000FF;">');
			F.document.write(content);
			F.document.write('</div>');
			// we open additional article in new window: add function wrapper to opener window
				F.document.write('\n<script language="javascript" type="text/javascript">\r');
				F.document.write('this.name = "' + theArticleWindowName + '";\r');
				F.document.write('function goto_page(the_page) { opener.goto_page(the_page,false) }\r');
				F.document.write('function goto_continued_article(target_article_id,target_article_page) {\r');
				F.document.write('opener.show_article_xml(null,target_article_id,1.0,null,"",false,false,target_article_page,this.name);\r');
				F.document.write('}\r');
				F.document.write('</script>\r');
			F.document.writeln("\r</body></html>");
			F.document.close();
			F.focus();
		}
		content = null;
		in_show_article_xml = false;
		return(false);
	};
	_sb_fn.show_article_xml = show_article_xml;

	var show_searchResultsArticle = function (fromSearchResults,
												go2page_idx, 
												obj,id,scale,jpg,xml,hasWWWlink,hasExtPDF,the_pagenumber,theArticleWindowName,theArticleWindowDim,theWindowTitle) {
		if (_sb_s.lb_ptr_evt.isMove) return true;			// was pointermove on lightbox

		//log_methods.log("show_searchResultsArticle\n\tfromSearchResults: " + fromSearchResults + "\n\tgo2page_idx: " + go2page_idx + "\n\tid: " +id);
		_sb_s.clickCameFromSearchResults = fromSearchResults;
		show_article_xml(obj,id,scale,jpg,xml,hasWWWlink,hasExtPDF,the_pagenumber,theArticleWindowName,theArticleWindowDim,theWindowTitle);
		if (go2page_idx !== null) _sb_fn.gotoPage(go2page_idx,true);
	};
	_sb_fn.show_searchResultsArticle = show_searchResultsArticle;

	// a global function
	goto_continued_article = function (target_article_id,target_article_page,doflipPage) {
		//alert("target_article_id: " + target_article_id + "\ntarget_article_page: " + target_article_page + "\ndoflipPage: " + doflipPage);
		if (typeof(doflipPage == 'undefined') || (doflipPage == true)) {
			var go2page_idx = target_article_page - 1;
			goto_page(go2page_idx,true,null,null,true,null,"goto_continued_article");
		}
		show_article_xml(null,target_article_id,1.0,null,"",false,false,target_article_page);
		return;
	};

	// a global function
	show_article = function (obj,id,page,force,scale,jpg,xml,hasWWWlink,hasExtPDF) {
		//_sb_s.DEBUGmode = 2;
		//log_methods.log("show_article id: " + id +"\n\tpage: " + page +"\n\tforce: " + force + " " + typeof(force) + "\n\tisSwipe: " + gestureDetectorPointer.isSwipe + "\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding + "\n\pageIsScrolling: " + _sb_s.pageIsScrolling);
		if (_g_turnjs_pageIsFolding === true) return true;	// mainly in IE 11, a click may fire after a 'pointerend'
		if (gestureDetectorPointer.isSwipe) return true;
		if (gestureDetectorPointer.touchstarted) gestureDetectorPointer.touchstarted = false;
		if ((typeof(force) == 'undefined') || (force === null) || (force === false) || (typeof(force) == 'string')) {	// this comes from area onclick (pointerend) event
			if (_sb_s.pageIsScrolling == true) return(true);
		}
		else {
			in_show_article_xml = false;
		}
		show_article_xml(obj,id,scale,jpg,xml,hasWWWlink,hasExtPDF,page);
		return(haltEvents(window.event));
	};


	var in_show_clicked_article = false;	// semaphore to prevent multiple calls within certain time (click events)
	show_clicked_article = function (e,caller) {	// public function
		//log_methods.log("show_clicked_article: " + in_show_clicked_article + "\ncaller: " + caller);
		if (in_show_clicked_article == true) return(true);
		in_show_clicked_article = true;
		// get article idx
		try { var id_split = current_article.obj_id.split("_"); } catch(ex) { return(true); }
		var art_idx = id_split[1].substr(1);
		current_article.page = id_split[0].split("P")[1];
		/*
		log_methods.log("show_clicked_article","obj: " + current_article.obj
			+ "\n\tobj_id: " + current_article.obj_id
			+ "\n\tpage: " + current_article.page
			+ "\n\tart_idx: " + art_idx
			+ "\n\tsrcxml: " + current_article.srcxml
			);
		*/
		try {
			_sb_s.lastSearchResults = "";	// clear last search results because we have clicked on an article on the page
			_sb_s.clickCameFromSearchResults = false;
		} catch(ex){}
		show_article_xml(current_article.obj,art_idx,'','',current_article.srcxml,'','',current_article.articleStartPage);
		in_show_clicked_article = false;
		return(haltEvents(e));
	}


	/* ========================
	 * the article floater
	 */
	function highlightArticleOnPage(articleID,count) {
		if (!current_article.valid) return false;
		if(_g_turnjs_pageIsFolding === true) return false;	// we are moving a folded page corner to turn the page
		if (_sb_s.pageIsScrolling) return false;
		if (!_sb_s.canvas_available || !_sb_s.lightboxIsShowing) return false;	// no shades or lightbox not open: do nothing

		areashadow(10,current_article.obj,current_article.coords,current_article.pagePreviewOffsX,current_article.pagePreviewOffsY,current_article.pagesequence);

		return true;
	}

	var floater_timeoutID = null;	// leave semicoloen: next 'floater' func is public

	// a public function
	floater = function (e,obj,func,articleStartPage,tit,txt,areas,bbox,xml,is_recall) {
		//if (is_recall) log_methods.log("floater arguments func: " + func + (obj ? "\n\ton obj.id: " +obj.id : "") + (e ? "\n\te.target.id: " + e.target.id : "") + "\n\tpageIsFolding: " + _g_turnjs_pageIsFolding + "\n\t_sb_s.pageIsScrolling: " + _sb_s.pageIsScrolling);
		//if (is_recall) log_methods.log("floater arguments func: " + func + (obj ? "\n\ton obj.id: " +obj.id : "") + "\n\tpageIsFolding: " + _g_turnjs_pageIsFolding + "\n\t_sb_s.pageIsScrolling: " + _sb_s.pageIsScrolling);
		if (_g_turnjs_pageIsFolding === true) return false;	// we are moving a folded page corner to turn the page
		if (_sb_s.pageIsScrolling) return false;
		if ((typeof(is_recall) == 'undefined') || (is_recall != true)) {
			if (floater_timeoutID != null) { clearTimeout(floater_timeoutID); floater_timeoutID = null; }
			floater_timeoutID = setTimeout(function(){floater(e,obj,func,articleStartPage,tit,txt,areas,bbox,xml,true);},1);
			return false;
		}
		if (floater_timeoutID != null) { clearTimeout(floater_timeoutID); floater_timeoutID = null; }

		if (func == 10) {	// from canvas onMouseOver
			_sb_s.mouseOnCanvas = true;
			if (obj) {
				_sb_fn.attachGestureDetector(obj,null,"canvas");
				//log_methods.log("floater attachGestureDetector\n\t on canvas obj: " +obj.id);
			}
			return false;
		}

		if (func == 20) {	// from area onmousemove for touch devices or canvas shadows turned off
			// delegate this to the underlaying page image for gestures
			//log_methods.log("floater event type '" + e.type + "' on: " +obj.id);
			var pageName = getPageFromObjID(obj.id),
				pageImageID = "#pv_P" + pageName;
			$(pageImageID).triggerHandler(e.type,e);
			return(haltEvents(e));
		}

		if (func == -1) {	// from canvas onMouseOut
			_sb_s.mouseOnCanvas = false;
			_sb_fn.clear_all_shadows(0);
			//log_methods.log("floater canvas event type '" + e.type + "' on: " +obj.id);
			return false;
		}

		if (func == 0) {	// from area onMouseOut
			return false;
			/*
			try { _sb_fn.clear_all_shadows(0); } catch(ex){}
			log_methods.log("floater area event type '" + e.type + "' on: " +obj.id);
			return false;
			*/
		}

		if (func == 1) {	// from area onMouseOver
			_sb_s.mouseOnCanvas = false;
			/*
			if (obj && obj.id) {
				log_methods.log("floater func == 1: " + obj.id);
				//if (_sb_fn.attachGestureDetector) _sb_fn.attachGestureDetector(obj,null,'area');
			}
			*/
		}

		if (!obj) return false;

		var pagesequence = getPageFromObjID(obj.id),
			pagePreviewID = "pv_P" + pagesequence,
			pagePreviewObj = document.getElementById(pagePreviewID),
			pagePreviewOffsX = 0, pagePreviewOffsY = 0;
		if (!pagePreviewObj) return false;

		if (_sb_s.is_Firefox) pagePreviewOffsY = getTopPos(pagePreviewObj,false);
		else pagePreviewOffsY = getTopPos(pagePreviewObj,true);
		pagePreviewOffsX = $(pagePreviewObj).offset().left - _sb_s.bodyOffsetLeft;//pagePreviewObj.offsetLeft;//getLeftPos(pagePreviewObj,true);

		current_article.valid=true;
		current_article.obj=obj;
		current_article.obj_id = obj.id;
		current_article.articleStartPage = articleStartPage;
		current_article.coords=areas;
		current_article.srcxml=xml;
		current_article.tit=tit; current_article.txt=txt;
		
		current_article.pagesequence=pagesequence;
		current_article.pagePreviewOffsX=pagePreviewOffsX;
		current_article.pagePreviewOffsY=pagePreviewOffsY;


		/*
		log_methods.log("floater pagePreviewOffsX: " + pagePreviewOffsX + "\n\tpagePreviewOffsY: " + pagePreviewOffsY  + "\n\t_sb_s.bodyOffsetLeft: " + _sb_s.bodyOffsetLeft
						+ "\n\tfunc: " + func + "\n\tcanvas_available: " + _sb_s.canvas_available + "\n\tmouseOnCanvas: " + _sb_s.mouseOnCanvas + "\n\tmapsAndCanvasAreMasked: " + _sb_s.mapsAndCanvasAreMasked);
		log_methods.log("floater areas: " + areas);
		*/
		if ( (_sb_s.canvas_available == true) && (_sb_s.mapsAndCanvasAreMasked == false) ) {
			if (!_sb_s.doArticleShade) return false;
			if (_sb_s.mouseOnCanvas && (func == 0)) return false;	// this comes from <area onMouseOut="floater(this,0)"...>
									// shoots always when the canvas is drawn and overlays article
			areashadow(func,obj,areas,pagePreviewOffsX,pagePreviewOffsY,pagesequence);
			return false;
		}
		return false;
	};


	/**
	 * Make an div touch scrollable
	 * call like:
	 *		touchScroll("thedivid");
	 *
	 * @param {string} id the id of the div to scroll
	 * optional parameters;
	 * @param {boolean|null=} destroy true to destroy the scroller
	 * @constructor
	 */
	var touchScroll = function (id,destroy){
		var scroll_enable = false,
			have_scrolled = false,
			scroll_element,
			scrollStartPosY=0, scrollStartPosX=0;
		//alert("is_IE: " + _sb_s.is_IE + "\n\tis_MobileOS: " + _sb_s.is_MobileOS + "\n\t_sb_s.is_iPad: " + _sb_s.is_iPad);
		if (_sb_s.enableJStouchscroll <= 0) return null;
		if (_sb_s.enableJStouchscroll > 1) {
			if ( ((_sb_s.enableJStouchscroll & 2) > 0) && (_sb_s.is_iPad || _sb_s.is_iPhone)) return null;	// they support scrolling divs through css
			if ( ((_sb_s.enableJStouchscroll & 4) > 0) && (_sb_s.is_IE && _sb_s.is_MobileOS)) {
				//alert("not for ie");
				return null;	// they support scrolling divs through css
			}
		}

		scroll_element = document.getElementById(id);
		if (scroll_element == null) return null;
			//log_methods.log("touchScroll on id: " + scroll_element.id + "\n\tdestroy: " + destroy);

		/*--- Internal functions ---*/
			function clearSelection() {
				var sel;
				if(document.selection && document.selection.empty) document.selection.empty() ;
				else {
					if(window.getSelection) {
						sel=window.getSelection();
						if(sel && sel.removeAllRanges) sel.removeAllRanges() ;
					}
				}
			}

			function artpop_mouseout(e) {
				if (have_scrolled) {
					//scroll_enable = false;
					//have_scrolled = false;
					return _sb_fn.haltEvents(e);
				}
				clearSelection();
				return false;
			}

			function artpop_touchstart(e) {
				scroll_enable = true;
				have_scrolled = false;
				var pXY = _sb_fn.get_pointerXY(e);
				if (pXY == null) return;
				//log_methods.log("artpop_touchstart at X/Y: " + pXY.x + " / " + pXY.y + "\n\tscrollTop: " + this.scrollTop + " Left: " + this.scrollLeft);
				scrollStartPosY=this.scrollTop+pXY.y;
				scrollStartPosX=this.scrollLeft+pXY.x;
				//if (e.stopPropagation) e.stopPropagation();
				//e.preventDefault(); //don't prevent! prevent also means clicking links
				return true;
			}
			function artpop_touchmove(e) {
				if (!scroll_enable) return false;
				var pXY = _sb_fn.get_pointerXY(e);
				//log_methods.log("artpop_touchmove #touches: " + pXY.length + "\n\tat X/Y: " + pXY.x + " / " + pXY.y + "\n\tthis: " + this.id);
				if (pXY == null) return;
				if (Math.abs(scrollStartPosY-pXY.y) < 5) return false;
				have_scrolled = true;
				this.scrollTop=scrollStartPosY-pXY.y;
				this.scrollLeft=scrollStartPosX-pXY.x;
				//log_methods.log("artpop_mousemove\n\tX - Y: " + pXY.x + " - " + pXY.y);
				return _sb_fn.haltEvents(e);
			}
			function artpop_touchend(e) {
				//log_methods.log("artpop_touchend");
				if (have_scrolled) {
					if (e.preventDefault) e.preventDefault();
					if (e.stopPropagation) e.stopPropagation();
				}
				scroll_enable = false;
				have_scrolled = false;
				return(true);
			}
		/*--- END Internal functions ---*/

		function detach(id) {
			var scroll_element = document.getElementById(id);
			//log_methods.log("touchScroll remove on:\n\t" + scroll_element.id);
			if (scroll_element.removeEventListener) {	// W3C DOM
				scroll_element.removeEventListener(_sb_s.pointerEvents.start,artpop_touchstart,false);
				scroll_element.removeEventListener(_sb_s.pointerEvents.end  ,artpop_touchend,false);
				scroll_element.removeEventListener(_sb_s.pointerEvents.move ,artpop_touchmove,false);
				scroll_element.removeEventListener(_sb_s.pointerEvents.out  ,artpop_mouseout,false);
			}
			else {
				if (scroll_element.detachEvent) { // IE DOM
					scroll_element.detachEvent("on"+_sb_s.pointerEvents.start, artpop_touchstart);
					scroll_element.detachEvent("on"+_sb_s.pointerEvents.end  , artpop_touchend);
					scroll_element.detachEvent("on"+_sb_s.pointerEvents.move , artpop_touchmove);
					scroll_element.detachEvent("on"+_sb_s.pointerEvents.out  , artpop_mouseout);
				}
				else {
					scroll_element.onmousedown = null;
					scroll_element.onmouseup = null;
					scroll_element.onmouseout = null;
					scroll_element.onmousemove = null;
				}
			}
			//log_methods.log("touchScroll handlers removed");
			return;
		}

		if (destroy == true) {
			detach(id);
			return;
		}
		else {

				// attach event handlers for scrolling by mouse
			if (scroll_element.addEventListener) {	// W3C DOM
				scroll_element.addEventListener(_sb_s.pointerEvents.start,artpop_touchstart,false);
				scroll_element.addEventListener(_sb_s.pointerEvents.end,artpop_touchend,false);
				scroll_element.addEventListener(_sb_s.pointerEvents.move,artpop_touchmove,false);
				scroll_element.addEventListener(_sb_s.pointerEvents.out,artpop_mouseout,false);
			}
			else {
				if (scroll_element.attachEvent) { // IE DOM
					scroll_element.attachEvent("on"+_sb_s.pointerEvents.start, artpop_touchstart);
					scroll_element.attachEvent("on"+_sb_s.pointerEvents.end, artpop_touchend);
					scroll_element.attachEvent("on"+_sb_s.pointerEvents.move, artpop_touchmove);
					scroll_element.attachEvent("on"+_sb_s.pointerEvents.out, artpop_mouseout);
				}
				else {
					scroll_element.onmousedown = artpop_touchstart;
					scroll_element.onmouseup = artpop_touchend;
					scroll_element.onmousemove = artpop_touchmove;
					scroll_element.onmouseout = artpop_mouseout;
				}
			}
			//log_methods.log("touchScroll handlers ATTACHED");
		}
	
		// propagate functions
		this.touchScroll = touchScroll;
		this.detach = detach;
		//alert(typeof touchScroll);
		return(this);
	};
	_sb_fn.touchScroll = touchScroll;


	/* ========================
	 * stop propagation of event
	 */
	var haltEvents = function (e) {
		var evt;
		if (!e) evt = window.event;
		else evt = e;
		if (evt) {
			if (evt.preventDefault) evt.preventDefault();
			if (evt.stopPropagation) evt.stopPropagation();
			if (typeof(evt.cancelBubble) != "undefined") evt.cancelBubble = true;
		}
		return false;
	},
	stopPropagation = function (e) {
		var evt;
		if (!e) evt = window.event;
		else evt = e;
		if (evt) {
			if (evt.stopPropagation) evt.stopPropagation();
			if (typeof(evt.cancelBubble) != "undefined") evt.cancelBubble = true;
		}
		return false;
	};
	_sb_fn.stopPropagation = stopPropagation;

	
	/**
	 * Get an element's style
	 *
	 * @param {Object} el The element
	 * @param {string} styleProp the style property
	 * @param {number} computed get the computed style
     * optional parameters;
	 * @param {boolean|null=} makeInt true to return as integer
	 */
	var getStyle = function (el, styleProp, computed, makeInt) {
		// computed: 0 = from class or style only
		//			 1 = computed only
		//			 2 = computed if Ok or then from class and style
		var st = "";
	
		if (computed > 0) {
			//log_methods.log("***getStyle id: " + el.id + "\nel.currentStyle " + el.currentStyle + ": " + styleProp);
			if (el.currentStyle) st = el.currentStyle[styleProp];	// IE <= 8 does not have 'getComputedStyle()'
			else if (window.getComputedStyle) {
					st = document.defaultView.getComputedStyle(el, null);
					if (st) st = st.getPropertyValue(styleProp);
				 }
			//log_methods.log("getStyle computed prop " + styleProp + ": " + st + " - " + typeof(st));
			if ((st !== null) && (typeof(st) != "undefined") && (st !== "")) {
				//alert("st: " + st);
				if (st === "auto") return(0);
				else return(makeInt ? parseInt(st, 10) : st);
			}
			if (computed == 1) return(null);	// computed only
		}

		// get from style property
		if (el.style) st = el.style.styleProp;
			//log_methods.log("getStyle '" + el.style + "' from style prop: " + styleProp + ": " + st + " - " + typeof(st));
		if ((typeof(st) != "undefined") && (st != null) && (st != "")) { if (st == "auto") return(0); else return(makeInt ? parseInt(st, 10) : st); }
		// get from class name
		if (el.className) st = get_css_value(el.className,styleProp);
			//log_methods.log("getStyle '" + el.className + "' from class prop: " + styleProp + ": " + st + " - " + typeof(st));
		if ((typeof(st) != "undefined") && (st != null) && (st != "undefined") && (st != "")) { if (st == "auto") return(0); else return(makeInt ? parseInt(st, 10) : st); }
		// get from id name
		if (el.id) st = get_css_value(el.id,styleProp);
			//log_methods.log("getStyle '" + el.id + "' from ID prop: " + styleProp + ": " + st + " - " + typeof(st));
		if ((typeof(st) != "undefined") && (st != null) && (st != "undefined") && (st != "")) { if (st == "auto") return(0); else return(makeInt ? parseInt(st, 10) : st); }
		return(null);
	};


	/**
	 * Make an element draggable
	 *
	 * @param {Object} elementToDrag The element which should react on mouse move
     * optional parameters;
	 * @param {Object=} delegateDragElement Optional: The element which should be moved. Usually a contained div. If null, elementToDrag will be moved
	 * @param {Array|null=} doConstrain Optional: Constrain the drag to an area: array [left,top,right,bottom]
	 * @param {Function|null=} cb_clickFunction Optional: a callback function to be called when the draggable element was clicked only
	 * @param {Function|null=} cb_dragStatus Optional: a callback function continuously called during drag
	 * @param {Function|null=} cb_dragEnd Optional: a callback function continuously called during drag
	 * @param {Array|null=} handleCurrentTargets Optional: array of element IDs to handle too. can also be ["*"] to handle all children of elementToDrag
	 *
	 * @return nothing
     * @constructor
	 */
	var XdraggableObject = function (elementToDrag, delegateDragElement, doConstrain, cb_clickFunction, cb_dragStatus, cb_dragEnd, handleCurrentTargets)
	{
		if (_sb_s.is_IE && (_sb_s.IEVersion < 8)) return;	// not IE7
		if (!elementToDrag) {
			//alert("shit");
			return;
		}
		var the_elementToDrag = elementToDrag,						// the element to attach the event handlers
			the_delegateDragElement = delegateDragElement,			// the actual element to drag (usually a container element of elementToDrag)
			dragElement = (the_delegateDragElement ? the_delegateDragElement : the_elementToDrag), // we drag this
			dragElement_marginLeft = 0, dragElement_marginTop = 0,	// margins are used to get ist's absolute position
			the_doConstrain = null,									// constrain to array [left,top,right,bottom]
			the_cb_clickFunction = null,							// the function to call when mouse was clicked
			the_cb_dragStatus = null,								// drag status change notifier
			the_cb_dragEnd = null,									// will be called after drag ends (onmouseup)
			the_handleCurrentTargets = (handleCurrentTargets || null),
		
			statusDragged = false,									// will be set to true if a drag occured
			elementCurrentX = 0, elementCurrentY = 0,				// store element's current position
			eventTarget,
			pointerEvents = _sb_s.pointerEvents,
			lastMX = 0,
			lastMY = 0,
			startMX = 0,
			startMY = 0,
			dragTrigger = 10;	// 10 delta pixels to say 'drag'


		//if (elementToDrag.id != "loggingdivcont") $.fn.log("log", "elementToDrag.id: " + elementToDrag.id + (delegateDragElement ? "\n\tdelegateDragElement.id: " + delegateDragElement.id : "") + "\n\tdoConstrain: " + doConstrain + "\n\thandleCurrentTargets: " + (handleCurrentTargets || null));
	
		if ((doConstrain != null) && typeof(doConstrain) != "undefined") the_doConstrain = doConstrain;
		if ((cb_clickFunction != null) && typeof(cb_clickFunction) != "undefined") the_cb_clickFunction = cb_clickFunction;
		if ((cb_dragStatus != null) && typeof(cb_dragStatus) != "undefined") the_cb_dragStatus = cb_dragStatus;
		if ((cb_dragEnd != null) && typeof(cb_dragEnd) != "undefined") the_cb_dragEnd = cb_dragEnd;
		//if ((handleCurrentTargets != null) && typeof(handleCurrentTargets) != "undefined") the_handleCurrentTargets = handleCurrentTargets;
		// get the element's left/top margins
		dragElement_marginLeft = getStyle(dragElement, "margin-left", 2, true);
			//$.fn.log("log","dragElement '" + dragElement.id + " margin-left: " + dragElement_marginLeft);
		if (dragElement_marginLeft === null) dragElement_marginLeft = getStyle(dragElement, "marginLeft", 1, true);
		if (dragElement_marginLeft === null) dragElement_marginLeft = 0;
			//$.fn.log("log","dragElement '" + dragElement.id + " marginLeft: " + dragElement_marginLeft);
		dragElement_marginTop = getStyle(dragElement, "margin-top", 2, true);
		if (dragElement_marginTop === null) dragElement_marginTop = getStyle(dragElement, "marginTop", 1, true);
		if (dragElement_marginTop === null) dragElement_marginTop = 0;
			//log_methods.log("log","dragElement '" + dragElement.id + " marginTop: " + dragElement_marginTop);

		if (_sb_s.touchActionStyle != "") {
			var style = dragElement.getAttribute("style");	// get current style
			if (!style) style = "";
			if (style.indexOf(_sb_s.touchActionStyle) < 0) {
				style += _sb_s.touchActionStyle + ":none;";
				dragElement.setAttribute("style", style);
				//log_methods.log("XdraggableObject style: " + style);
				//alert(style);
			}
		}


		function getElementXY(elem,margLeft,margTop) {
			var left = parseInt(elem.offsetLeft, 10) - margLeft,
				top = parseInt(elem.offsetTop, 10) - margTop,
				prntBl, prntBt;
			// Firefox is the only browser which does not include body borders
			if (!_sb_s.is_Firefox) {
				if (elem.parentNode) {
					prntBl = getStyle(elem.parentNode,"borderLeftWidth",0,true);
					prntBt = getStyle(elem.parentNode,"borderTopWidth",0,true);
					if (prntBl === null) prntBl = 0;
					if (prntBt === null) prntBt = 0;
					left -= prntBl;
					top -= prntBt;
				}
			}
				//log_methods.log("getElementXY left: " + elem.offsetLeft + ", top: " + elem.offsetTop + "\n\toffsetLeft left: " + elem.offsetLeft + ", offsetTop: " + elem.offsetTop + "\n\tmargLeft: " + margLeft + "\n\tmargTop: " + margTop + "\n\tparentNode: " + elem.parentNode + "\n\tparentNode borderLeft: " + getStyle(elem.parentNode,"borderLeftWidth",0,true) + ", top: " + getStyle(elem.parentNode,"borderTopWidth",0,true));
			return([left,top]);
		}

		function getMouseXY(e) {
			var mouseX, mouseY;
			if (_sb_s.isTouchDevice) {
				// when in IFrame, Android reports for e.pageX and Y always 0
				// we have to get the e.touches[0].pageX / Y
				if (e.touches) {
					mouseX = e.touches[0].pageX ? e.touches[0].pageX : e.pageX;	// mobiles have touches[x]
					mouseY = e.touches[0].pageY ? e.touches[0].pageY : e.pageY;
				}
				else {
					mouseX = e.pageX ? e.pageX : 0;
					mouseY = e.pageY ? e.pageY : 0;
				}
			}
			else {
				mouseX = e.pageX ? e.pageX : e.clientX + document.documentElement.scrollLeft;
				mouseY = e.pageY ? e.pageY : e.clientY + document.documentElement.scrollTop;
			}
			/*
			var logstr = "getMouseXY isTouchDevice: " + _sb_s.isTouchDevice
						+ "\n\t e.pageX: " + e.pageX + ", e.pageY: " + e.pageY;
						if (e.touches) {
							logstr += "\n\t e.touches: " + e.touches
									+ "\n\t e.touches[0]pageX: " + e.touches[0].pageX + ", e.touches[0]pageY: " + e.touches[0].pageY;
						}
						logstr += "\n\t e.clientX: " + e.clientX + ", e.clientY: " + e.clientY
								+ "\n\t scrollLeft: " + document.documentElement.scrollLeft + ", scrollTop: " + document.documentElement.scrollTop
								+ "\n\t scrollLeft: " + document.body.scrollLeft + ", scrollTop: " + document.body.scrollTop;
			log_methods.log(logstr);
			*/
			return([mouseX,mouseY]);
		}

		function EvtMouseDown(e) {
				//_sb_s.DEBUGmode = 2; log_methods.log("MouseDown on target: '" + (e.target ? e.target : window.event.srcElement).id + "'\n\tcurrentTarget: '" + (e.currentTarget ? e.currentTarget : window.event.srcElement).id + "'\n\tregistered Element: '" + (the_elementToDrag ? the_elementToDrag.id : 'undefined') + "'\n\tregistered Element exists: '" + document.getElementById(the_elementToDrag.id) + "'\n\tthe_delegateDragElement: '" + (the_delegateDragElement ? the_delegateDragElement.id : typeof(the_delegateDragElement)) + "'\n\tdelegate Element exists: '" + (the_delegateDragElement? document.getElementById(the_delegateDragElement.id) : 'undefined') + "'\n\tthe_handleCurrentTargets: '" + the_handleCurrentTargets +"'" + "\n\ttouches: " + (e.touches ? e.touches.length : 'undefined'));
			if (e.touches && e.touches.length > 1) return(true);	// allow resizig with fingers
			eventTarget = (e.target ? e.target : window.event.srcElement);
			if ((the_elementToDrag.id != eventTarget.id) && the_delegateDragElement && (the_delegateDragElement != eventTarget.id)) {
				if (the_cb_clickFunction != null) {
					if (the_handleCurrentTargets == null) return(true);	// LET BUBLE!!
						//log_methods.log("should handle target '" + "*" + "'?: " + the_handleCurrentTargets.indexOf("*"));
					if (the_handleCurrentTargets.indexOf("*") >= 0) {
							//log_methods.log("calling cb for ID: " + this.id);
						try { return(the_cb_clickFunction(e)); } catch(ex){}
						return(true);
					}
						//log_methods.log("should handle target '" + eventTarget.id + "'?: " + the_handleCurrentTargets.indexOf(eventTarget.id));
					if (the_handleCurrentTargets.indexOf(eventTarget.id) >= 0) {
							//log_methods.log("calling cb for eventTarget ID: " + eventTarget.id);
						try { return(the_cb_clickFunction(e)); } catch(ex){}
						return(true);	// LET BUBLE!!
					}
				}
				return(true);	// LET BUBBLE!!
			}
			// initiate drag
			if (_sb_s.is_IE && (_sb_s.IEVersion <= 8)) setMouseEventHandlers(the_elementToDrag);	// sets EvtMouseUp, EvtMouseOut, EvtMouseMove
			setWindowMouseEventHandlers();
			//log_methods.log("MouseDown handlers installed");
			var elemXY = getElementXY(dragElement,dragElement_marginLeft,dragElement_marginTop),		// get current element position
				mouseXY = getMouseXY(e);

			elementCurrentX = elemXY[0];
			elementCurrentY = elemXY[1];
			lastMX = mouseXY[0];	// remember last mouse position
			lastMY = mouseXY[1];
			startMX = lastMX;
			startMY = lastMY;

			/*
			log_methods.log("XdraggableObject EvtMouseDown"
							+ "\n\tElem position left: " + elemXY[0] + ", top: " + elemXY[1]
							+ "\n\tMouse left: " + mouseXY[0] + ", top: " + mouseXY[1]);
			*/
			if (statusDragged && the_cb_dragStatus != null) try { the_cb_dragStatus(e, statusDragged, elementCurrentX, elementCurrentY); } catch(ex){}
			return(haltEvents(e));
		}

		function EvtMouseUp(e) {
				//log_methods.log("MouseUp on target: '" + (e.target ? e.target : window.event.srcElement).id + "'\n\tcurrentTarget: '" + (e.currentTarget ? e.currentTarget : window.event.srcElement).id + "'\n\tregistered Element: '" + the_elementToDrag.id + "'\n\tthe_handleCurrentTargets: '" + the_handleCurrentTargets +"'");
			removeMouseEventHandlers(the_elementToDrag);	// removes EvtMouseUp, EvtMouseOut, EvtMouseMove
			removeWindowMouseEventHandlers();

			// may be we have to make a callback
			//log_methods.log("MouseUp on target: '" + (e.target ? e.target : window.event.srcElement).id + "'\n\tstatusDragged: " + statusDragged + "\n\tthe_cb_clickFunction: '" + the_cb_clickFunction);
			if (!statusDragged && (the_cb_clickFunction != null)) {
				try { the_cb_clickFunction(e); } catch(ex){}
			}
			if (statusDragged && (the_cb_dragEnd != null)) {
					//log_methods.log("Drag end mouseup. element X: " + elementCurrentX + ", Y: " + elementCurrentY);
				try { the_cb_dragEnd(e, elementCurrentX, elementCurrentY); } catch(ex){}
			}

			statusDragged = false;
		}

		function EvtMouseOut(e) {
			//	log_methods.log("Mouse OUT: " + e.type);
			//setWindowMouseEventHandlers(the_elementToDrag);
			//return(haltEvents(e));
		}

		function EvtMouseMove(e) {
			//	log_methods.log("MouseMove on target: '" + (e.target ? e.target : window.event.srcElement).id + "'\n\tcurrentTarget: '" + (e.currentTarget ? e.currentTarget : window.event.srcElement).id+ "'\n\tregistered Element: '" + the_elementToDrag.id + "\n\ttouches: " + (e.touches ? e.touches.length : 'undefined'));
			if (e.touches && e.touches.length > 1) return(true);	// allow resizig with fingers
			var mouseXY = getMouseXY(e),				// get current mouse positon
				deltaMX = mouseXY[0] - lastMX,			// calculate delta
				deltaMY = mouseXY[1] - lastMY,
				elemXY = getElementXY(dragElement,dragElement_marginLeft,dragElement_marginTop),		// get current element position
				style = "", prop, newLeft, newTop, i,
				styleArrAss, styleArr,
				retval;
			/*
			log_methods.log("EvtMouseMove buttons: " + e.buttons
						+ "\n\tLast elem X: " + elemXY[0] + ", Y: " + elemXY[1]
						+ "\n\tLast lastMX: " + lastMX + ", lastMY: " + lastMY
						+ "\n\tCurrent mouse X: " + mouseXY[0] + ", mouse Y: " + mouseXY[1]
						+ "\n\tDelta Mouse X: " + deltaMX + ", Y: " + deltaMY
						+ "\n\tstartMX: " + startMX + ", Y: " + startMY
						+ "\n\tDELTA start X: " + Math.abs(lastMX - startMX) + ", Y: " + Math.abs(lastMY - startMY)
						);
			*/
			// IE <=8 continuously sends mouse move events but mouse has not moved: catch this
			if ((deltaMX == 0) && (deltaMY == 0)) return(haltEvents(e));
			lastMX = mouseXY[0];
			lastMY = mouseXY[1];

			if ((Math.abs(lastMX - startMX) > dragTrigger) 
				|| (Math.abs(lastMY - startMY) > dragTrigger)) statusDragged = true;
			else return(true);
		
			// we are here when drag is started
			// calculate new position
			newLeft = (elemXY[0] + deltaMX);
			newTop = (elemXY[1] + deltaMY);
				//log_methods.log("dragging element: " + dragElement.id + "\n\tviewpaortScale: " + _sb_s.currentMetaViewPortScale + "\n\tclientWidth: " + _sb_s.clientWidth + "\n\tis_IFrame: " + _sb_s.containerWindowIsIFrame + "\n\tconstrain: " + the_doConstrain + "\n\tnewLeft: " + newLeft + "\n\tnewTop: " + newTop);
			if (the_doConstrain != null) {
					//log("constrain: "  + the_doConstrain);
				if ((the_doConstrain.length > 0) && (newLeft < the_doConstrain[0])) newLeft = the_doConstrain[0];
				if ((the_doConstrain.length > 1) && (newTop < the_doConstrain[1])) newTop = the_doConstrain[1];
				if ((the_doConstrain.length > 2) && (newLeft > the_doConstrain[2])) newLeft = the_doConstrain[2];
				if ((the_doConstrain.length > 3) && (newTop > the_doConstrain[3])) newTop = the_doConstrain[3];
			}

			if (the_cb_dragStatus != null) try {
												retval = the_cb_dragStatus(e, statusDragged, newLeft, newTop);
												if (retval == false) return(haltEvents(e));	// stop repositioning when callback returns false
												return(true);
											 } catch(ex){}
		
			// position the element
/*
			style = dragElement.getAttribute("style");	// get current style
			if (!style) style = "";
			style = trimAll(style);	// trim blanks
			if ((style != "") && endsWith(style,";")) style = style.substr(0,style.length-1);	// remove last semicolon

				//log_methods.log("current style = \"" + style + "\"");
			// make style associative array
			styleArrAss = new Array();
			styleArr = style.split(";")
				//log_methods.log("styleArr.length = " + styleArr.length + "\n\tstyleArr[0]: " + styleArr[0]);
			for (i = 0; i < styleArr.length; i++) {
				if (styleArr[i].indexOf(":") <= 0) continue;
				styleArr[i] = styleArr[i].split(":");
			}
			// now make it an associative array
			for (i = 0; i < styleArr.length; i++) {
				if (!styleArr[i][1]) continue;
				styleArrAss[styleArr[i][0]] = styleArr[i][1];
			}
	
			// set new property values
			if (styleArrAss.indexOf("position") >= 0) styleArrAss["position"] = "absolute";
			else styleArrAss["position"] = "absolute";
			if (styleArrAss.indexOf("left") >= 0) styleArrAss["left"] = newLeft+"px";
			else styleArrAss["left"] = newLeft+"px";
			if (styleArrAss.indexOf("top") >= 0) styleArrAss["top"] = newTop+"px";
			else styleArrAss["top"] = newTop+"px";
			// get array properties as string
			style = "";
			for (prop in styleArrAss) style += prop + ":" + styleArrAss[prop] +";";
				//log_methods.log("new style\n\t\"" + style + "\"");

			dragElement.setAttribute("style", style);
				//log_methods.log("element: " + the_elementToDrag.id + ", X: " + elemXY[0] + ", Y: " + elemXY[1] + "\n\tDelta Mouse X: " + deltaMX + ", Y: " + deltaMY+ "\n\tstyle = " + style);
*/
				//this type of settings styles does not work in all cases !!!!!!!!
				dragElement.style.positon = "absolute";
				dragElement.style.top = newTop+"px";
				dragElement.style.left = newLeft+"px";
					//log_methods.log("element: " + dragElement.id + ", X: " + elemXY[0] + ", Y: " + elemXY[1] + "\n\tDelta Mouse X: " + deltaMX + ", Y: " + deltaMY + "\n\tnewLeft / top: " + newLeft + " / " + newTop + "\n\tnew style Left / top: " + dragElement.style.left + " / " + dragElement.style.top);
				

			// remember this
			elementCurrentX = newLeft;
			elementCurrentY = newTop;

			return(haltEvents(e));
		}
	
		setMouseDownEventHandlers(elementToDrag);

		function is_handler_attached(elem, what) {
			var isset = elem.getAttribute(what);	// what is '"data-md-att"'	for mouse down attached
			if ((isset == null) || (isset == "") || (isset == "0")) return false;
			return true;
		}

		// set mousedown event handlers
		function setMouseDownEventHandlers(elem) {
			if (!is_handler_attached(elem, "data-md-att")) {
				addEventHandler(elem,pointerEvents.start,EvtMouseDown,false);
					elem.setAttribute("data-md-att","1");	// set mouse down handler is attached
				//log_methods.log("mousedown on: " + elem.id);
			}
		}

		// set other event handlers
		function setMouseEventHandlers(elem) {
			if (!is_handler_attached(elem, "data-mm-att")) {
				addEventHandler(elem,pointerEvents.move,EvtMouseMove,false);
					elem.setAttribute("data-mm-att","1");	// set mouse move handler is attached
			}
			if (!is_handler_attached(elem, "data-mu-att")) {
				addEventHandler(elem,pointerEvents.end,EvtMouseUp,false);
					elem.setAttribute("data-mu-att","1");	// set mouse up handler is attached
			}
			if (!is_handler_attached(elem, "data-mo-att")) {
				addEventHandler(elem,pointerEvents.out,EvtMouseOut,false);
					elem.setAttribute("data-mo-att","1");	// set mouse out handler is attached
			}
		}
		var window_mousehandlers_attached = false;
		// set event handlers for window
		function setWindowMouseEventHandlers() {
			if (window_mousehandlers_attached) return true;

			if (window.addEventListener) {	// W3C DOM
				window.addEventListener(pointerEvents.move,EvtMouseMove,false);
				window.addEventListener(pointerEvents.end,EvtMouseUp,false);
				window.addEventListener(pointerEvents.out,EvtMouseOut,false);
				if (pointerEvents.cancel) window.addEventListener(pointerEvents.cancel,EvtMouseOut,false);
			}
			else if (window.attachEvent) { // IE <= 8 DOM
					window.attachEvent("on"+pointerEvents.move,EvtMouseMove);
					window.attachEvent("on"+pointerEvents.end,EvtMouseUp);
					window.attachEvent("on"+pointerEvents.out,EvtMouseOut);
					if (pointerEvents.cancel) window.attachEvent("on"+pointerEvents.cancel,EvtMouseOut);
				}
			//log_methods.log("setWindowMouseEventHandlers: " + pointerEvents.move);
			window_mousehandlers_attached = true;
			return true;
		}

		// remove mousedown event handlers
		function removeMouseDownEventHandlers(elem) {
			removeEventHandler(elem,pointerEvents.start,EvtMouseDown,false);
				elem.setAttribute("data-md-att","0");	// set mouse down handler is detached
		}

		// remove other event handlers
		function removeMouseEventHandlers(elem) {
			removeEventHandler(elem,pointerEvents.move,EvtMouseMove,false);
				elem.setAttribute("data-mm-att","0");	// set mouse move handler is detached
			removeEventHandler(elem,pointerEvents.end,EvtMouseUp,false);
				elem.setAttribute("data-mu-att","0");	// set mouse up handler is detached
			removeEventHandler(elem,pointerEvents.out,EvtMouseOut,false);
				elem.setAttribute("data-mo-att","0");	// set mouse out handler is detached
		}

		// remove event handlers for window
		function removeWindowMouseEventHandlers() {
			if (!window_mousehandlers_attached) return;

			if (window.removeEventListener) {	// W3C DOM
				window.removeEventListener(pointerEvents.move,EvtMouseMove,false);
				window.removeEventListener(pointerEvents.end,EvtMouseUp,false);
				window.removeEventListener(pointerEvents.out,EvtMouseOut,false);
				if (pointerEvents.cancel) window.removeEventListener(pointerEvents.cancel,EvtMouseOut,false);
			}
			else if (window.detachEvent) { // IE <= 8 DOM
					window.detachEvent("on"+pointerEvents.move,EvtMouseMove);
					window.detachEvent("on"+pointerEvents.end,EvtMouseUp);
					window.detachEvent("on"+pointerEvents.out,EvtMouseOut);
					if (pointerEvents.cancel) window.detachEvent("on"+pointerEvents.cancel,EvtMouseOut);
				}
			window_mousehandlers_attached = false;
		}
	
		// remove all event listeners
		function removeDraggable() {
			removeMouseDownEventHandlers(the_elementToDrag);
			removeMouseEventHandlers(the_elementToDrag);
				//log("removed draggable");
		}
	
		// propagate functions
		this.removeDraggable = removeDraggable;
		return(this);
	}
	// propagate this function to the image lightbox
	_sb_s.showImageLightbox.dragFunction = XdraggableObject;		// the pointer to our drag function to be used by the image lightbox
	_sb_fn.XdraggableObject = XdraggableObject;





	/* ========================
	 *
	 * Full-Text Search Stuff through fulltext database on multiple objects and issues
	 *
	 */
	var objectsList = new Array(),

	getObjectsList = function (what,respStatus,list) {
			//log_methods.log("getObjectsList what: " + what + "\n\trespStatus: " + respStatus + "\n\tlist: " + list);
		if ((respStatus != 200) || (list.indexOf("#####") == 0)) {
			return;
		}
		var i, objects = list.split("\n"),
			searchform,
			objectsPopHTM = "";
		for (i = 0; i < objects.length; i++) objectsList[objectsList.length] = objects[i].split(";;");
		while (objectsList[objectsList.length-1] =="") objectsList.length--;
		// create the objects popup
		objectsPopHTM += "<div class=\"ft_objects_select_container\" id=\"ft_objects_select_container\">";
		objectsPopHTM += "<select class=\"ft_objects_select\" id=\"objects_select\" name=\"objects_select\" on" + _sb_s.pointerEvents.start + "=\"this.focus();event.stopPropagation();return(true);\">";
		objectsPopHTM += "<option selected=\"selected\" value=\"*ALL*\">" + ftst[2][_sb_s.cur_lang_ID] + "</option>";
		for (i = 0; i < objectsList.length; i++) {	// show ALL entry only if more than 1 objects
			objectsPopHTM += "<option class=\"ft_objects_option\" value=\"" + objectsList[i][0] + "\" title=\"" + objectsList[i][3] + "\">" + objectsList[i][1] + "</option>";
		}
		objectsPopHTM += "</select>";
		objectsPopHTM += "<div class=\"searchPopButtonsContainer\">";
		objectsPopHTM += "<button type=\"button\" class=\"getLatestPublicationButton\" on" + _sb_s.pointerEvents.end + "=\"_sb_fn.getLatestIssue();\">" + ftst[7][_sb_s.cur_lang_ID] + "</button><br>";
		objectsPopHTM += "<button type=\"button\" class=\"startSearchButton\" on" + _sb_s.pointerEvents.end + "=\"_sb_fn.call_sb_fulltext_search(event,document.getElementById('ft_searchentry_form'), 'ft_searchentry_win CLICK');\">" + ftst[8][_sb_s.cur_lang_ID] + "</button>";
		objectsPopHTM += "</div>";
		objectsPopHTM += "</div>";

		searchform = document.getElementById("ft_searchentry_form");
		searchform.innerHTML = searchform.innerHTML + objectsPopHTM;

		//alert(objectsPopHTM);
		// we have to re-get this:
		_sb_e.ft_searchentry_field = document.getElementById("ft_searchentry_field");
		// URGENTLY needed for Android!!! seems to be slow rendering the new content
		setTimeout("_sb_e.ft_searchentry_field = document.getElementById(\"ft_searchentry_field\");",200);

	},

	getLatestIssue = function () {
		// first, test if an issue is available
		var objects_select = document.getElementById("objects_select"),
			selidx = objects_select.selectedIndex,
			getobj = objects_select.options[selidx].value,
			issue = document.getElementById("ft_searchstartdate_field").value,
			request = ft_root + "/search/latest.php?action=issueavail&o=" + getobj + "&sd=" + issue,
			issues_request;

			issues_request = new xmlHttpRequest('GET', request, 'text/html', cb_getAnIssue, getobj);
	},

	cb_getAnIssue = function (what,respStatus,resulttxt) {
		// what contains the object ID
		//alert("what: " + what + "\n" + resulttxt);
		var num_issues_avail = 0,
			request;
		try {
			num_issues_avail = resulttxt.split(":")[1];
			if (isNaN(num_issues_avail)) {
				alert(ftst[10][_sb_s.cur_lang_ID]);
				return;
			}
			num_issues_avail = parseInt(num_issues_avail, 10);
		}
		catch(ex) {
			alert(ftst[10][_sb_s.cur_lang_ID]);
			return;
		}
		if (num_issues_avail <= 0) {
			alert(ftst[10][_sb_s.cur_lang_ID]);
			return;
		}
		// load issue
		request = ft_root + "/search/latest.php";
		if ((what != "") && (what != "*ALL*")) request += "?o=" + what;
		//alert(request);
		window.location.href=request;

	},

	ft_doc_loc = window.location.href,
	ft_root = ft_doc_loc.substr(0,ft_doc_loc.toLowerCase().indexOf("/data/")),
	ft_incpath = ft_root + "/search/inc/",	// the 'inc' path
	ft_searchWin = null,

	init_fulltext_search = function () {	// try to find the fulltext search system
		if (!window.FormData
			|| (_sb_s.is_Android && (_sb_s.AndroidVersion < _sb_s.AndroidVersionMin))
			) {	// Android 2.2.2 does not POST formData
								// local search only
			//alert("shit");
			disable_fulltext_search();
			return;
		}
		var qb_img = new Image();
		if ( _sb_s.is_IE || _sb_s.is_Safari ) {
			qb_img.onload=function(){ enable_fulltext_search(); };
			qb_img.onerror=function(){ disable_fulltext_search(); };
		}
		else {
			qb_img.setAttribute("onload","_sb_fn.enable_fulltext_search();");
			qb_img.setAttribute("onerror","_sb_fn.disable_fulltext_search()");
		}
		qb_img.src = ft_incpath + "qb.gif";
		return;
	},

	enable_fulltext_search = function () {	// fulltext search system IS reachable
		enable_sb_fulltext_search();
	},

	disable_fulltext_search = function () {	// fulltext search system NOT reachable - we try to enable the local document search
			//log_methods.log("disable_fulltext_search");
		try { enable_local_search(); } catch(ex) {}	// defined in searchlocal.js
	},

	enable_sb_fulltext_search = function () {	// create the slidebook search stuff via database
			//log_methods.log("enable_fulltext_search: " + ft_root + "/search/search_sb.php");
		//create div for fulltext search input field
		var ft_searchentry_win=document.createElement("div"),
			wincont='',
			obj_request;
		ft_searchentry_win.id="ft_searchentry_win";
		ft_searchentry_win.className= "ft_searchentry_win";

		wincont+='<form id=\"ft_searchentry_form\" name=\"ft_searchentry_form\" class=\"ft_search_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" action=\"javascript:return(call_sb_fulltext_search(event,this,\'ft_searchentry_win FORM\'))\">'
			wincont+='<div class="ft_searchentry_cont" id="ft_searchentry_cont">';
				wincont+='<input class="ft_searchentry_field" id="ft_searchentry_field" name="ft_searchentry_field" type="text" title="' + ftst[1][_sb_s.cur_lang_ID] + '" value="' + ftst[0][_sb_s.cur_lang_ID] + '" onkeypress="_sb_fn.handleFTsearchFieldKeyPress(event)">';
			wincont+='</div>';

			wincont+='<div class="ft_searchdate_cont" id="ft_searchdate_cont">';
				wincont+='<table class="ft_date_tbl" id="ft_date_tbl"><tr><td>';
					wincont+='<div class="ft_searchdate_title" id="ft_searchdate_title">' + ftst[3][_sb_s.cur_lang_ID] + '</div>';
				wincont+='</td><td>';
					wincont+=ftst[12][_sb_s.cur_lang_ID];	// help button
				wincont+='</td></tr><tr><td>';
					wincont+='<div class="ft_searchstartdate_title" id="ft_searchstartdate_title">' + ftst[4][_sb_s.cur_lang_ID] + '</div>';
					wincont+='<input class="ft_searchstartdate_field" id="ft_searchstartdate_field" name="ft_searchstartdate_field" type="text" title="' + ftst[6][_sb_s.cur_lang_ID] + '" value="" onkeyup="_sb_fn.checknumeric(this)">';
				wincont+='</td><td>';
					wincont+='<div class="ft_searchenddate_title" id="ft_searchenddate_title">' + ftst[5][_sb_s.cur_lang_ID] + '</div>';
					wincont+='<input class="ft_searchenddate_field" id="ft_searchenddate_field" name="ft_searchenddate_field" type="text" title="' + ftst[6][_sb_s.cur_lang_ID] + '" value="" onkeyup="_sb_fn.checknumeric(this)">';
				wincont+='</td></tr></table>';

			wincont+='</div>';
		wincont+='</form>';


		ft_searchentry_win.innerHTML=wincont;

		_sb_e.bodyEl.appendChild(ft_searchentry_win);
		_sb_e.ft_searchentry_field = document.getElementById("ft_searchentry_field");

		ft_searchentry_win = $('#ft_searchentry_win');
		var	searchInitialHeight = ft_searchentry_win.height(),
			ft_searchwin_open = false,

			fadeInSearchWin = function (){
				//log_methods.log("fadeInSearchWin");
				ft_searchentry_win.animate(
							{height: _sb_e.ft_searchentry_field_height, opacity: 1},
							300,
							function() {
								ft_searchentry_win.css('height', 'auto');
								ft_searchwin_open = true;
								//log_methods.log("fadeInSearchWin IS SHOWN");
							});

				// show ft help text when clicked on question mark
				$('#ft_help_mark').off();
				$('#ft_help_mark').on(_sb_s._elementTouch, function(e) {
						if (ft_searchwin_open) fadeOutSearchWin();
						showFtHelp();
						return(haltEvents(e));
						});
 				_sb_e.html.on(_sb_s.pointerEvents.start,blurSearchWin);
			},

			fadeOutSearchWin = function (){
				//log_methods.log("fadeOutSearchWin to height: " + searchInitialHeight);
				ft_searchentry_win.animate(
							{height: searchInitialHeight, opacity: 1},
							300,
							function() {
								_sb_s.user_entry_active = false;	// this enables flipbook resize 
								ft_searchentry_win.css('height', searchInitialHeight);	// because it was 'auto' after opening
								ft_searchwin_open = false;
								//log_methods.log("fadeOutSearchWin IS HIDDEN");
							});
			},
			
			blurSearchWin = function(e) {
 				_sb_e.html.off(_sb_s.pointerEvents.start,blurSearchWin);
				//_sb_s.DEBUGmode = 2; log_methods.log("blurSearchWin");
				fadeOutSearchWin();
				_sb_e.ft_searchentry_field.blur();
				document.getElementById("ft_searchstartdate_field").blur();
				document.getElementById("ft_searchenddate_field").blur();
			},

			toggle_search_options = function () {
				if (ft_searchwin_open) fadeOutSearchWin();
				else fadeInSearchWin();
			},

			call_ft_search = function (e){
				var target = (e.target ? e.target : window.event.srcElement).id;
				//_sb_s.DEBUGmode = 2; log_methods.log("CALLBACK!! call_ft_search click on \n\ttarget: '" + target + "'");
				switch (target) {
					case "objects_select":	// click on objects drop list
						break;
					case "ft_searchentry_form":	// click on lupe to start search
						if ((_sb_e.ft_searchentry_field.value.indexOf("\u00a0\u00a0") == 0) || (_sb_e.ft_searchentry_field.value == "")) {
							_sb_e.ft_searchentry_field.focus();
							_sb_e.ft_searchentry_field.value = '';
							// open the search options
							if (!ft_searchwin_open) fadeInSearchWin();
						}
						else if (_sb_e.ft_searchentry_field.value != "") {
								// close the search options
								if (ft_searchwin_open) fadeOutSearchWin();
								//log_methods.log("CALLBACK!! calling call_sb_fulltext_search");
								call_sb_fulltext_search(e,document.getElementById("ft_searchentry_form"), "ft_searchentry_win CLICK");
								try {
									_sb_e.ft_searchentry_field.blur();
								} catch(ex){}
							 }
						return(haltEvents(e));
						break;
					case "ft_searchentry_field":	// enter search term
						_sb_s.user_entry_active = true;	// this disables flipbook resize during search entry 
						if (_sb_e.ft_searchentry_field.value.indexOf("\u00a0\u00a0") == 0) _sb_e.ft_searchentry_field.value = '';
						//log_methods.log(_sb_e.ft_searchentry_field);
						setTimeout("_sb_e.ft_searchentry_field.focus()",10);	// Hack: FF does not set focus sometimes
						// open or close the search options
						toggle_search_options();
						stopPropagation(e);
						//return(haltEvents(e));	// do not prevent default or propagation or field may not be edited correctly like marking text or moving cursor
						return(true);
						break;
					case "ft_searchstartdate_field":
						document.getElementById("ft_searchstartdate_field").focus();
						stopPropagation(e);
						break;
					case "ft_searchenddate_field":
						document.getElementById("ft_searchenddate_field").focus();
						stopPropagation(e);
						break;
					default:
						haltEvents(e);
						break;
				}
				return;	// allow to bubble into objects_select or...
			};
			// make globally accessible
			_sb_fn.fadeOutSearchWin = fadeOutSearchWin;
			_sb_fn.blurSearchWin = blurSearchWin;
 
		_sb_fn.addEventHandler(_sb_e.ft_searchentry_field,
											"blur",
											function(e) {
												_sb_e.ft_searchentry_field.blur();
												_sb_s.user_entry_active = false;	// this enables flipbook resize 
												$.enab_prev_default(true,1);	// prevent default marking on html
											});

		// make the ft_searchentry_win draggable
		if (_sb_s.enableDraggableObjects > 0) {
			new XdraggableObject(document.getElementById("ft_searchentry_form"),document.getElementById("ft_searchentry_win"),null,call_ft_search,null,null,["*"]);
		}
		else {	// handle clicks

			$('#ft_searchentry_form').on(_sb_s._elementTouch, function(e) {
						//log_methods.log("ft_searchentry_form\nhandle event e.target: " + e.target.id + "\n\te.currentTarget: " + e.currentTarget.id);
					_sb_e.ft_searchentry_field.blur();
					return call_ft_search(e);		// pass the event
				});
				
			// start search when clicked on lupe
			$('#ft_searchentry_cont').on(['click', _sb_s.pointerEvents.start], function(e) {	// call search php
					//alert("click cont");
					haltEvents(e);
					fadeOutSearchWin();
					_sb_e.ft_searchentry_field.blur();
					_sb_s.user_entry_active = false;	// this enables flipbook resize 
					call_sb_fulltext_search(e,document.getElementById("ft_searchentry_form"), "ft_searchentry_win CLICK");
					});

		}
		// get the objects to create the popup
		obj_request = new xmlHttpRequest('GET', ft_root + "/search/search_sb.php?action=getobjects", 'text/html', getObjectsList,'getObjectsList');
	},

	handleFTsearchFieldKeyPress = function (e) {
		//alert("handleFTsearchFieldKeyPress");
		var key=e.keyCode || e.which;
		if (key==13) {
			// close the keyboard
			if (_sb_e.ft_searchentry_field != null) _sb_e.ft_searchentry_field.blur();
			// call search
			call_sb_fulltext_search(e, document.getElementById("ft_searchentry_form"), "ft_searchentry_win ENTER");
			return false;
		}
		return(true);
	},

	call_sb_fulltext_search = function (evt, form, caller) {
		//log_methods.log("call_sb_fulltext_search with form: " + form.id + " caller: " + caller);
		if (_sb_e.ft_searchentry_field == null) return(true);
		var searchFor = _sb_e.ft_searchentry_field.value,
			searching, formData, hturl, ftresult_request;
		if (searchFor.indexOf("\u00a0\u00a0") == 0) return(true);
		if (searchFor.length < 3) return(true);
		haltEvents(evt);
		_sb_fn.fadeOutSearchWin();
		_sb_fn.blurSearchWin();
		_sb_s.user_entry_active = false;	// this enables flipbook resize 
		// show spinning wheel
		searching = "<div style=\"width:100%;margin-top:30%;text-align:center;\"><img src=\"" + _sb_s.xslcssPath + "sprocket.gif\"></div>";
		showLightbox(null, searching, "text");
		// stop page image preload
		hold_preload_pageimages(1);

		// prepare form data
  		formData = new FormData(form);	// is unknown by Android 2.2.2

		// kick search now
   		hturl = ft_root + "/search/search_sb.php?nohead";
 		ftresult_request = new xmlHttpRequest('POST', hturl, 'text/html', cb_ftSearchResults, 'call_sb_fulltext_search', null, null, null, formData);

		return false;
	},
	cb_ftSearchResults = function (what,respStatus,resulttxt) {
		// called from new xmlHttpRequest() success handler
		//alert("resulttxt:\n"+resulttxt);
		hold_preload_pageimages(0);	// release page image preload if it was on hold

		showLightbox(null, resulttxt, "text", null, true);
	},

	checknumeric = function (elem) {
		if ((elem == null) || (elem.value == "")) return(true);
		if (!isNaN(elem.value)) return(true);
		var val = elem.value;
		while ((val != "") && isNaN(val)) {
			val = val.substring(0,val.length-1);
		}
		//alert(val);
		elem.value = val;
		return false;
	},

	showFtHelp = function () {
		// get the help text file
		var hturl = ft_root + "/search/help/ft_helpsb_" + _sb_s.cur_lang + ".html";
		new xmlHttpRequest('GET', hturl, 'text/html', cb_showFtHelpText,'showFtHelp');
		// the success handler will call showFtHelpText(). see HTTPget()
	},
	cb_showFtHelpText = function (what,respStatus,txt) {
		// called from xmlHttpRequest() success handler
		showLightbox(null, txt, "text");
	};
	// make globally available
	_sb_fn.call_sb_fulltext_search = call_sb_fulltext_search;
	_sb_fn.init_fulltext_search = init_fulltext_search;
	_sb_fn.enable_fulltext_search = enable_fulltext_search;
	_sb_fn.disable_fulltext_search = disable_fulltext_search;
	_sb_fn.handleFTsearchFieldKeyPress = handleFTsearchFieldKeyPress;
	_sb_fn.getLatestIssue = getLatestIssue;
	_sb_fn.checknumeric = checknumeric;




	/* ========================
	 * Utilities functions
	 */
	/* ====== get total left right body margins and paddings ====== */
	var getBodyLRMarginsTotal = function () {
		_sb_e.body_marginLeft = cssIntVal(_sb_e.body.css('marginLeft'));
		_sb_e.body_marginRight = cssIntVal(_sb_e.body.css("marginRight"));
		_sb_e.body_paddingLeft = cssIntVal(_sb_e.body.css("paddingLeft"));
		_sb_e.body_paddingRight = cssIntVal(_sb_e.body.css("paddingRight"));
		//log_methods.log("body margins LR: " + _sb_e.body_marginLeft + " " + _sb_e.body_marginRight + " " +  paddingleft + " " +  paddingright);
		return(_sb_e.body_marginLeft + _sb_e.body_marginRight + _sb_e.body_paddingLeft + _sb_e.body_paddingRight);
	},
	/* ====== get total top bottom body margins and paddings ====== */
	getBodyTBMarginsTotal = function () {
		_sb_e.body_marginTop = cssIntVal(_sb_e.body.css('marginTop'));
		_sb_e.body_marginBottom = cssIntVal(_sb_e.body.css("marginBottom"));
		_sb_e.body_paddingTop = cssIntVal(_sb_e.body.css("paddingTop"));
		_sb_e.body_paddingBottom = cssIntVal(_sb_e.body.css("paddingBottom"));
		//log_methods.log("body margins TB: " + _sb_e.body_marginTop + " " + _sb_e.body_marginBottom + " " +  _sb_e.body_paddingTop + " " +  _sb_e.body_paddingBottom);
		return(_sb_e.body_marginTop + _sb_e.body_marginBottom + _sb_e.body_paddingTop + _sb_e.body_paddingBottom);
	},

	get_viewWidthNeeded = function () {
		var wfactor = 2, margin = 0, border = 0, w;	// wfactor 2 = double pages
		margin = getBodyLRMarginsTotal();
		border = _sb_e.sb_container_borderLeftWidth + _sb_e.sb_container_borderRightWidth
				+ _sb_e.sb_pagelist_borderLeftWidth + _sb_e.sb_pagelist_borderRightWidth
				+ wfactor * (_sb_s.page_borderLeftWidth + _sb_s.page_borderRightWidth);

		w = wfactor * _sb_s.pageWidth;
		if (w == null) w = _sb_fn.get_moreDocumentInfo("pageJPEGwidth").split(",")[_sb_s.pageImageUseSize];
		w = parseInt(w, 10);
		
		w += margin + border;
		//log_methods.log("get_viewWidthNeeded\n\tmargin: " + margin + "\n\tborder: " + border + "\n\tWidth needed: " + w);
		return(w);
	},
	/* ====== get total height needed for book ====== */
	get_viewHeightNeeded = function () {
		var debugmessage_height = 0, margin = 0, border = 0, h;
		try { debugmessage_height = _sb_e.debugmessage.outerHeight(); } catch(ex) { debugmessage_height = 0; }	// might be missing
		if (debugmessage_height === null) debugmessage_height = 0;
		margin = getBodyTBMarginsTotal();
		border = _sb_s.page_borderTopWidth + _sb_s.page_borderBottomWidth + _sb_e.sb_pagelist_borderTopWidth + _sb_e.sb_pagelist_borderBottomWidth;
		h = margin + border
				+ _sb_e.sb_container.outerHeight()
				+ debugmessage_height
				+ _sb_s.bodyOversize ;
		/*
		log_methods.log("get_viewHeightNeeded"
						+ "\n\tmargins TB total: " + margin
						+ "\n\tborder TB total: " + border
						+ "\n\tsb_container height: " + _sb_e.sb_container.height()
						+ "\n\tdebugmessage height: " + debugmessage_height
						+ "\n\tOversize: " + _sb_s.bodyOversize
						+ "\n\tHeight needed: " + h);
		*/
		return(h);
	},

	get_pinchZoom = function () {
		if (window.devicePixelRatio) _sb_s.currentPinchZoom = window.devicePixelRatio;
		else {
			get_screenWidth();
			//alert("get_pinchZoom windowWidth: " + _sb_s.windowWidth + " screenWidth:" + _sb_s.screenWidth);
			_sb_s.currentPinchZoom = _sb_s.windowWidth/_sb_s.screenWidth;
		}
		//alert("get_pinchZoom: " + _sb_s.currentPinchZoom);
		return (_sb_s.currentPinchZoom);
	},

	get_screenPPI = function () {
		var div = document.createElement("div");
		div.style.width = "1in";
		div.setAttribute("style","width:1in;");
		_sb_e.bodyEl.appendChild(div);
		_sb_s.screenPPI = div.offsetWidth;
		//alert("width: " + div.offsetWidth);
		div.parentNode.removeChild(div);
	},

	// detect orientation of screen
	get_screenOrientation = function (caller) {
		var screenorientation = "";
		/*
		log_methods.log("get_screenOrientation called by: \n\t" + (caller ? caller : "")
			+ "\n\twindow.orientation: " + window.orientation
			+ "\n\tscreen.mozOrientation: " + screen.mozOrientation
			+ "\nscreen.width: " + screen.width
			+ "\nscreen.height: " + screen.height
			);
		*/
		if (typeof(screen.mozOrientation) != 'undefined') {
			if (screen.mozOrientation.indexOf("landscape") > -1) screenorientation = "landscape";
			else screenorientation = "portrait";
		}
		else if (_sb_s.is_iPad || _sb_s.is_iPhone && (typeof(window.orientation) != 'undefined')) {
			if ((window.orientation == 0) || (window.orientation == 180)) {	// on iPad prtrait device, 0 and 180 are portrait
																			// on landscape devices 0 and 180 are landscape
				screenorientation = "portrait";
			}
			else screenorientation = "landscape";
				
		}
		else if (screen.width != 'undefined') {
			if (screen.width < screen.height) {	// on iPad prtrait device, 0 and 180 are portrait
																			// on landscape devices 0 and 180 are landscape
				screenorientation = "portrait";
			}
			else screenorientation = "landscape";
				
		}
		else if (typeof(window.orientation) != 'undefined') {
			if ((window.orientation == 0) || (window.orientation == 180)) {	// on iPad prtrait device, 0 and 180 are portrait
																			// on landscape devices 0 and 180 are landscape
				screenorientation = "portrait";
			}
			else screenorientation = "landscape";
				
		}
		//log_methods.log("get_screenOrientation: " + screenorientation);
		return screenorientation;
	},
	
	get_clientSize = function (caller) {
		var screenorientation = "";
		/*
		alert("window.devicePixelRatio: " + window.devicePixelRatio
			+ "\nwindow.screen.pixelDepth: " + window.screen.pixelDepth
			+ "\nscreen.width: " + screen.width
			+ "\nscreen.height: " + screen.height
			);
		*/
		if (_sb_s.screenPPI < 0) get_screenPPI();
		get_pinchZoom();
		/*
		_sb_s.DEBUGmode = 2;
		log_methods.log("get_clientSize by: " + caller
			+ "\n\twindow.innerWidth: " + window.innerWidth
			+ "\n\twindow.innerHeight: " + window.innerHeight
			+ "\n\tscreen.width: " + screen.width
			+ "\n\tscreen.height: " + screen.height
			+ "\n\t_sb_s.currentPinchZoom: " + _sb_s.currentPinchZoom
			);
		*/
		if( typeof( window.innerWidth ) != 'undefined' ) {	//Non-IE
			_sb_s.clientWidth = window.innerWidth;
			_sb_s.clientHeight = window.innerHeight;
		} else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
			//IE 6+ in 'standards compliant mode'
			_sb_s.clientWidth = document.documentElement.clientWidth;
			_sb_s.clientHeight = document.documentElement.clientHeight;
		} else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
			//IE 4 compatible
			_sb_s.clientWidth = document.body.clientWidth;
			_sb_s.clientHeight = document.body.clientHeight;
		}
		_sb_s.clientWidth = parseInt(_sb_s.clientWidth, 10);	//Math.floor(parseInt(_sb_s.clientWidth, 10) * _sb_s.currentMetaViewPortScale);
		_sb_s.clientHeight = parseInt(_sb_s.clientHeight, 10);

		/*
		log_methods.log("get_clientSize by: " + caller
			+ "\n\tscreen.width X height: " + screen.width + " x " + screen.height
			+ "\n\tclient Width x Height: " + _sb_s.clientWidth+"x"+_sb_s.clientHeight 
			);
		*/
		if (_sb_s.clientWidth < _sb_s.pageAdjustMinWidth) _sb_s.clientWidth = _sb_s.pageAdjustMinWidth;
		if (_sb_s.clientHeight < _sb_s.pageAdjustMinHeight) _sb_s.clientHeight = _sb_s.pageAdjustMinHeight;

		if (_sb_s.clientWidth < _sb_s.clientHeight) _sb_s.orientation = "portrait";
		else _sb_s.orientation = "landscape";
		// check if this can be trusted
		screenorientation = get_screenOrientation("get_clientSize");
		if (_sb_s.is_MobileOS) {	// a Firefox hack stating wrong dimensions in portrait
			_sb_s.orientation = screenorientation ;
		}
		
		if (_sb_s.oldOrientation == "") _sb_s.oldOrientation = _sb_s.orientation;	// set if not initialized
		
		//log_methods.log("get_clientSize\n\tclient Width x Height: " + _sb_s.clientWidth+"x"+_sb_s.clientHeight+"\ncurrentMetaViewPortScale: " + _sb_s.currentMetaViewPortScale + "\ncurrentPinchZoom: " + _sb_s.currentPinchZoom);
		/*
		log_methods.log("get_clientSize by: " + caller
			+ "\n\tscreen.width X height: " + screen.width + " x " + screen.height
			+ "\n\tscreenorientation: " + screenorientation
			+ "\n\tclient Width x Height: " + _sb_s.clientWidth+"x"+_sb_s.clientHeight 
			+ "\n\t_sb_s.orientation: " +  _sb_s.orientation 
			+ "\n\tcurrentMetaViewPortScale: " + _sb_s.currentMetaViewPortScale 
			+ "\n\tcurrentPinchZoom: " + _sb_s.currentPinchZoom
			+ "\n\twindow.devicePixelRatio: " + window.devicePixelRatio
			+ "\n\twindow.screen.pixelDepth: " + window.screen.pixelDepth
			);
		*/
	},

	// check for meta tag "viewport" available in HTML document
	has_metaViewPort = function () {
		// get meta viewport
		var metatags = document.getElementsByTagName("meta"),
			m = 0;
		for (m = 0; m < metatags.length; m++) {
			if (metatags[m].getAttribute("name") == "viewport") return true;
		}
		return false;
	},

	get_metaViewPortScale = function () {
		// get meta viewport
		var metatags = document.getElementsByTagName("meta"),
			metascale = 1.0,
			m = 0,
			subcontent;
		for (m = 0; m < metatags.length; m++) {
			if (metatags[m].getAttribute("name") != "viewport") continue;
			_sb_s.initialMetaViewPort = metatags[m].getAttribute("content");
			subcontent = _sb_s.initialMetaViewPort.split(",");
			for (m = 0; m < subcontent.length; m++) {
				if (subcontent[m].indexOf("initial-scale") < 0) continue;
				metascale = parseFloat(subcontent[m].split("=")[1]);
				break;
			}
			break;
		}
		return(metascale);
	},

	handleOrientationChangeTimeout = null,
	handleOrientationChange = function (init, caller, recalled) {
		//log_methods.log("handleOrientationChange");
		if ((typeof(recalled) == 'undefined') || (recalled == false)) {
			if (handleOrientationChangeTimeout != null) {
				clearTimeout(handleOrientationChangeTimeout);
				handleOrientationChangeTimeout = null;
			}
			handleOrientationChangeTimeout = setTimeout(function() {handleOrientationChange(init,caller,true);},1000);	// wait a bit before reaction on device orientation changes
			return;
		}
		if (handleOrientationChangeTimeout != null) {
			clearTimeout(handleOrientationChangeTimeout);
			handleOrientationChangeTimeout = null;
		}
		update_screenDimensions(init,caller);
	},
	
	update_screenDimensions = function (init,caller) {
		//log_methods.log("update_screenDimensions called by: \n\t" + caller);
		if (_sb_s.is_MobileOS && _sb_s.user_entry_active) return;
		_sb_s.currentMetaViewPortScale = get_metaViewPortScale();
	//	if (init == true) {
			/* this is urgently needed for iOS when in normal window (not in IFrame) */
			if (_sb_s.is_iPad || _sb_s.is_iPhone) {
				_sb_s.windowWidth = Math.floor((window.innerWidth ? window.innerWidth : document.body.clientWidth));
				if (!_sb_s.containerWindowIsIFrame) _sb_s.windowWidth *= _sb_s.currentMetaViewPortScale;	// WHEN NOT IN IFRAME: scale to viewport
				_sb_s.windowHeight = Math.floor((window.innerHeight ? window.innerHeight : document.body.clientHeight));
				if (!_sb_s.containerWindowIsIFrame) _sb_s.windowHeight *= _sb_s.currentMetaViewPortScale;	// WHEN NOT IN IFRAME: scale to viewport
				//log_methods.log("_sb_s.is_iPad || _sb_s.is_iPhone\n\twindow.innerWidth: " + window.innerWidth + ", innerHeight: " + window.innerHeight + "\n\t_sb_s.currentMetaViewPortScale: " + _sb_s.currentMetaViewPortScale);
			} 
			else {	// all other or Android
				// these 2 do not work for IE8
				_sb_s.windowWidth = (window.innerWidth ? window.innerWidth : document.documentElement.clientWidth);
				_sb_s.windowHeight = (window.innerHeight ? window.innerHeight : document.documentElement.clientHeight);
				//log_methods.log("is_Android or PC\n\twindow.innerWidth: " + window.innerWidth + ", innerHeight: " + window.innerHeight + "\n\t_sb_s.currentMetaViewPortScale: " + _sb_s.currentMetaViewPortScale);
			}
	//	}

		_sb_s.screenWidth = get_screenWidth();	// these 2 values always for portrait
		_sb_s.screenHeight = get_screenHeight();


		get_clientSize("update_screenDimensions");	// calcs _sb_s.clientWidth and _sb_s.clientHeight
							// and get pinch zoom

		
		//_sb_s.DEBUGmode = 2; log_methods.log("update_screenDimensions:\n\t_sb_s.orientation: " + _sb_s.orientation + "\n\t_sb_s.oldOrientation: " + _sb_s.oldOrientation);
		if (_sb_s.pageAdjustToWindow != 0) {
			if (_sb_s.orientation != _sb_s.oldOrientation) {
				//log_methods.log("update_screenDimensions reloading book:\n\tisTouchDevice: " + _sb_s.isTouchDevice + "\n\t_sb_s.reloadOnOrientationChange: " + _sb_s.reloadOnOrientationChange);
				if (_sb_s.isTouchDevice && (_sb_s.reloadOnOrientationChange & 1)) {
					setTimeout(function(){reloadBook();},0);
					return;
				}
				else if (!_sb_s.isTouchDevice && (_sb_s.reloadOnOrientationChange & 2)) {
					setTimeout(function(){reloadBook();},0);
					return;
				}
				_sb_s.oldOrientation = _sb_s.orientation;
			}
		}


		_sb_s.viewWidthNeeded = get_viewWidthNeeded();
		_sb_s.viewHeightNeeded = get_viewHeightNeeded();
		_sb_s.bodyWidth = _sb_e.body.width();
		_sb_s.bodyHeight = _sb_e.body.height();
		_sb_s.bodyOffsetLeft = parseInt(_sb_e.body.offset().left, 10);
			if (!_sb_s.is_IE) _sb_s.bodyOffsetLeft += _sb_e.body_borderLeftWidth;
		_sb_s.bodyOffsetTop = parseInt(_sb_e.body.offset().top, 10);
			if (_sb_s.is_IE || _sb_s.is_Opera || _sb_s.is_Firefox) _sb_s.bodyOffsetTop += _sb_e.body_borderTopWidth;
			if ((_sb_s.is_IE &&  (_sb_s.IEVersion <= 8)) || _sb_s.is_Opera) _sb_s.bodyOffsetTop += _sb_e.body_borderTopWidth;	// YES indeed: TWICE!!!

		if (!init && _sb_s.pageAdjustToWindow != 0) adjustPageImageSize("update_screenDimensions", false, true, false);

		/*
		log_methods.log("update_screenDimensions - init: " + init 
				+ "\n\tcaller: " + caller
				+ "\n\tdeviceOrientation: " + _sb_s.deviceOrientation
				+ "\n\tcurrentMetaViewPortScale: " + _sb_s.currentMetaViewPortScale
				+ "\n\twindow innerWidth / Height: " + window.innerWidth + " / " + window.innerHeight
				+ "\n\twindowWidth / Height: " + _sb_s.windowWidth + " / " + _sb_s.windowHeight
				+ "\n\tclientWidth / Height: " + _sb_s.clientWidth + " / " + _sb_s.clientHeight
				+ "\n\tbody clientWidth / Height: " + document.body.clientWidth + " / " + document.body.clientHeight
				+ "\n\tbodyWidth / Height: " + _sb_s.bodyWidth + " / " + _sb_s.bodyHeight
				+ "\n\tbodyOffsetLeft / Top: " + _sb_s.bodyOffsetLeft + " / " + _sb_s.bodyOffsetTop
				+ "\n\tborderLeftWidth / Top: " + _sb_e.body_borderLeftWidth + " / " + _sb_e.body_borderTopWidth
				+ "\n\tviewWidthNeeded / Height: " + _sb_s.viewWidthNeeded + " / " + _sb_s.viewHeightNeeded
				);
		*/
	},

	get_screenWidth = function () {
		var width = parseInt(screen.width, 10),
			height = parseInt(screen.height, 10),
			w;
		//log_methods.log(_sb_fn.list_object(window, null, "window", true, "<br>"));

		if (height > width) _sb_s.orientation = "portrait";
		else _sb_s.orientation = "landscape";
		// if call is from within an IFrame, window.orientation always is 0 = portrait
		//log_methods.log("get_screenWidth\nis_MobileOS: " + _sb_s.is_MobileOS + "\nwindow.orientation: " + window.orientation + "\nWxH: "+width +"x"+height);
		if (!_sb_s.containerWindowIsIFrame && (typeof(window.orientation) != 'undefined')) {
			if (_sb_s.orientation == "portrait") {	// portrait
				if (width > height) {
					w = width; width = height; height = w;
					_sb_s.orientation = "landscape";
				}
				//log_methods.log("get_screenWidth\nwindow.orientation: " + window.orientation + " portrait: "+width +"x"+height)
			}
			else {	// 90 and -90 = landscape
				if (height > width) {
					w = width; width = height; height = w;
					_sb_s.orientation = "portrait";
				}
				//log_methods.log("get_screenWidth\nwindow.orientation: " + window.orientation + " landscape: "+width +"x"+height)
			}
		}
		//alert("get_screenWidth\nis_MobileOS: " + _sb_s.is_MobileOS + "\n_sb_s.orientation: " + _sb_s.orientation + "\nWxH: "+width +"x"+height);
		return(width);
	},

	get_screenHeight = function () {
		var width = parseInt(screen.width, 10),
			height = parseInt(screen.height, 10),
			w;
		if (height > width) _sb_s.orientation = "portrait";
		else _sb_s.orientation = "landscape";
		// if call is from within an IFrame, window.orientation always is 0 = portrait
		if (_sb_s.is_Android) {
			width = Math.min(width,_sb_s.windowWidth);
			height = Math.min(height,_sb_s.windowHeight);
		}
		else {
			if (!_sb_s.containerWindowIsIFrame && (typeof(window.orientation) != 'undefined')) {
				if (width > height) {
					w = width; width = height; height = w;
					_sb_s.orientation = "landscape";
				}
				//log_methods.log("get_screenWidth\nwindow.orientation: " + window.orientation + " portrait: "+width +"x"+height)
			}
			else {	// 90 and -90 = landscape
				if (height > width) {
					w = width; width = height; height = w;
					_sb_s.orientation = "portrait";
				}
				//log_methods.log("get_screenWidth\nwindow.orientation: " + window.orientation + " landscape: "+width +"x"+height)
			}
		}
		return(height);
	},

	websiteParams = "",
	websiteParamsArr = null,
	init_websiteParams = function () {
		// prepare variables
		try {
			websiteParams = document.getElementById('websiteParams').innerHTML;
		} catch(ex) {}
		if (websiteParams != "") {
			websiteParamsArr = websiteParams.split("*#*");
		}
		return;
	},

	get_websiteParam = function (keyword) {
		if ((keyword == null) || (keyword == "") || (websiteParamsArr == null) || (websiteParamsArr.length <= 0)) return("");
		for (var i = 0; i < websiteParamsArr.length; i++) {
			if (websiteParamsArr[i].indexOf(keyword+"=") == 0) return(websiteParamsArr[i].substr(websiteParamsArr[i].indexOf("=")+1));
		}
		return("");
	},

	set_siteTheme = function (themeSubPath) {
		if (themeSubPath == "") return;
		//alert(_sb_s.xslcssPath+" - "+ themeSubPath);
		var btn_src, btn_path, btn_name;
		try {
			btn_src = document.getElementById("pagePDF_button_img").getAttributeNode("src"),
			btn_path = btn_src.nodeValue,
			btn_name = btn_path.substr(btn_path.lastIndexOf("/")+1);
			btn_src.nodeValue = _sb_s.xslcssPath + themeSubPath + btn_name;
		}catch(ex){};
	},

	loadExtCSS = function (href) {
		if (href == "") return(-1);
		var fileref=document.createElement("link");
		fileref.setAttribute("rel", "stylesheet");
		fileref.setAttribute("type", "text/css");
		fileref.setAttribute("href", href);
		//alert("href: "+ href);

		if (typeof fileref!="undefined") document.getElementsByTagName("head")[0].appendChild(fileref);
		else return(-2);
		return(0);
	},

	bookTheme = function (xslcsspath,themeSubPath,css1,css2) {
		if (css1 == "") return;
		var retval = loadExtCSS(xslcsspath+themeSubPath+css1);
		if (css2 != "") retval = loadExtCSS(xslcsspath+themeSubPath+css2);
		//alert("bookTheme new: " + xslcsspath+themeSubPath+css1 + " \nretval: " + retval);
		// and update PDF buttons
		set_siteTheme(themeSubPath);
	},

	moreDocumentInfos = "",
	moreDocumentInfosArr = null,
	init_DocumentInfos = function () {
		// prepare variables
		try {
			moreDocumentInfos = document.getElementById('moreDocumentInfos').innerHTML;
		} catch(ex) {}
		if (moreDocumentInfos != "") {
			moreDocumentInfosArr = moreDocumentInfos.split("*#*");
		}
		/* we have waited long enough, element 'moreDocumentInfos' should be available if set
		if (moreDocumentInfosArr == null) {
			setTimeout("init_DocumentInfos()",20);
			return;
		}
		*/
		
		init_websiteParams();
	};

	_sb_fn.get_moreDocumentInfo = function (keyword) {
		if ((keyword == null) || (keyword == "") || (moreDocumentInfosArr == null) || (moreDocumentInfosArr.length <= 0)) return("");
		for (var i = 0; i < moreDocumentInfosArr.length; i++) {
			if (moreDocumentInfosArr[i].indexOf(keyword+"=") == 0) return(moreDocumentInfosArr[i].substr(moreDocumentInfosArr[i].indexOf("=")+1));
		}
		return("");
	};


	var	list_object = function(obj, which, objname, nofuncs, separator) {
		if (typeof(obj) == 'undefined') return('undefined');
		if (typeof(obj) == null) return('null');
		var key,
			str = "", sep = separator || "",
			numprops = 0, n = 0;
		if (objname) str += objname + "={";
		else str += "{";
		for (key in obj) numprops++;
		for (key in obj) {
			if (which && (key.toLowerCase().indexOf(which) < 0)) continue;
			try {
				if (obj[key] == null) {
					str += "\"" + key + "\":null" + sep;
					continue;
				}
				if (typeof(obj[key]) == 'undefined') {
					str += "\"" + key + "\":undefined" + sep;
					continue;
				}
				if (!obj[key].constructor) {
					str += "\"" + key + "\":no constructor" + sep;
					continue;
				}
				//log_methods.log("++++ " + key + ": " + obj[key] + ": " + obj[key].constructor.name);

				str += "\"" + key + "\":";
				//switch (obj[key].constructor.name.toLowercase()) {
				switch (typeof(obj[key])) {
					case 'null':
						 str += "null";
						 break;
					case 'array':
						var arr = obj[key];
						str += "[";
						for (var i = 0; i < arr.length; i++) {
							str += (typeof(arr[i]) == 'string' ? "\"" : "") + arr[i] + (typeof(arr[i]) == 'string' ? "\"" : "");
							if (i < (arr.length-1)) str += ",";
						}
						str += "]";
						break;
					case 'string':
						 str += "\"" + obj[key] + "\"";
						break;
					case 'point':
						 str += "[" + obj[key][0] + "," + obj[key][1] + "]";
						break;
					case 'function':
						if (nofuncs) str += "function()";
						else str += "[" + obj[key][0] + "," + obj[key][1] + "]";
						break;
					case 'number':
						 str += obj[key];
						break;
					default:	// other
						//alert(typeof(obj[key])); return;

						 str += typeof(obj[key]);
						 break;
				}
			} catch(ex){
				str += obj[key] + " \"exception \"" + typeof(obj[key]);
			}
			n++;
			if (n < numprops) str += ",";
			str += sep;
		}
		str += "}";
		return(str);
	},

	

	listCssRules = function (lineseparator) {
		var cssRules, S, R,
			linesep = "\n",
			list = "StyleSheets and rules:\r";

		if (lineseparator) linesep = lineseparator;
		list = "StyleSheets and rules:" + linesep;
		if (!document.styleSheets) return list + "no stylesheets" + linesep;

		if (document.all) cssRules = 'rules';
		else if (document.getElementById) cssRules = 'cssRules';
		// to get the latest definition of a style we have to search backwards
		for (S = 0; S < document.styleSheets.length; S++){
			list += "URL: " + document.styleSheets[S].href + linesep;
			try {
				//for (R = document.styleSheets[S][cssRules].length-1; R >= 0; R--) {	// does not work !!??
				for (R = 0; R < document.styleSheets[S][cssRules].length; R++) {
						//alert(theClass + ": " + document.styleSheets[S][cssRules][R].selectorText);
						list += "\t" + document.styleSheets[S][cssRules][R].selectorText + linesep;
				}
			} catch(ex){};
		}
		// return nothing = undefined
		return(list);
	},

	hasCssRules = function (sheet) {
		var cssRules;
		if (document.all) cssRules = 'rules';
		else cssRules = 'cssRules';
		try {	// might fail !!! ???
			if (!sheet) return -1;
			if (!sheet[cssRules]) return -2;
			return sheet[cssRules].length;
		} catch(ex) {}
		return 1;	// we had error -assume we have rules
	},

	get_css_value = function (theClass,element) {
		if (!document.styleSheets) return "";
		var cssRules, S, R;
		if (document.all) cssRules = 'rules';
		else if (document.getElementById) cssRules = 'cssRules';
		// to get the latest definition of a style we have to search backwards
		for (S = document.styleSheets.length-1; S >= 0; S--){
			try {
				//for (R = document.styleSheets[S][cssRules].length-1; R >= 0; R--) {	// does not work !!??
				for (R = 0; R < document.styleSheets[S][cssRules].length; R++) {
					try {
						//alert(theClass + ": " + document.styleSheets[S][cssRules][R].selectorText);
						if (document.styleSheets[S][cssRules][R].selectorText.indexOf(theClass) >= 0) {
							//alert("CSS title: " + document.styleSheets[S].title
							//						+ "\nFound in CSS class: " + theClass + ": " + document.styleSheets[S][cssRules][R].selectorText + "\nselector: " + element + ": " + document.styleSheets[S][cssRules][R].style[element]);
							return ("" + document.styleSheets[S][cssRules][R].style[element]);	// for browser compatibility (IE6) always return as string
							break;
						}
					} catch(ex){};
				}
			} catch(ex){};
		}
		// return nothing = undefined
		return(undefined);
	},

	set_css_value = function (theClass,element,value) {
		if (!document.styleSheets) return;
		//alert("theClass: " + theClass + "\nelement: " + element + "\nvalue: " + value);
		var cssRules, S, R;
		if (document.all) cssRules = 'rules';
		else if (document.getElementById) cssRules = 'cssRules';
		// to set the latest definition of a style we have to search backwards
		//alert("NUM STYLESHEETS: " + document.styleSheets.length);
		for (S = document.styleSheets.length-1; S >= 0; S--){
			try {
				for (R = 0; R < document.styleSheets[S][cssRules].length; R++) {
					try {
						//alert("css rule   " + document.styleSheets[S][cssRules][R].selectorText);
						if (document.styleSheets[S][cssRules][R].selectorText.indexOf(theClass) >= 0) {
							document.styleSheets[S][cssRules][R].style[element] = value;
							//alert(element + ": " + document.styleSheets[S][cssRules][R].style[element]);
							return;
						}
					} catch(ex){};
				}
			} catch(ex){};
		}
	},

	add_local_css_rule = function (selector,declaration) {
		if (!document.styleSheets) return;
		var cssRules, S, cssNode,
			numcss = document.styleSheets.length,
			localCssIdx = -1;
		if (document.all) cssRules = 'rules';
		else if (document.getElementById) cssRules = 'cssRules';
		// to set the latest definition of a style we have to search backwards
		for (S = numcss-1; S >= 0; S--){
			try {
				if (document.styleSheets[S].title == "localFlipbookSheet") {
					localCssIdx = S;
					break;
				}
			} catch(ex){};
		}
		if (localCssIdx < 0) {
			cssNode = document.createElement('style');
			cssNode.type = 'text/css';
			cssNode.rel = 'stylesheet';
			cssNode.media = 'all';
			cssNode.title = 'localFlipbookSheet';
			document.getElementsByTagName("head")[0].appendChild(cssNode);
			localCssIdx = numcss-1;	// add at end
			//alert("added stylsheet at: " + localCssIdx);
			for (S = numcss-1; S >= 0; S--){
				try {
					if (document.styleSheets[S].title == "localFlipbookSheet") {
						localCssIdx = S;
						break;
					}
				} catch(ex){};
			}
			//alert("found stylsheet at: " + localCssIdx);
		}
		//alert(document.styleSheets[localCssIdx].title + "current length: " + document.styleSheets[localCssIdx].cssRules.length);
		try {
			if (_sb_s.is_IE) document.styleSheets[localCssIdx].addRule(selector, declaration, -1);
			else document.styleSheets[localCssIdx].insertRule((selector + " {" + declaration + ";}"), 0);
		} catch(ex){};
		//alert("Style added: " + selector + "{" + declaration + ";}" + "\nat: " + document.styleSheets[localCssIdx].cssRules.length);
	};
	// make these functions accessible globally
	_sb_fn.get_css_value = get_css_value;
	_sb_fn.set_css_value = set_css_value;
	_sb_fn.listCssRules = listCssRules;
	_sb_fn.list_object = list_object;

	var hasClass = function (elem,classname) {
		if (!elem || !classname) return false;
		var currentClassName = elem.className;
		if (currentClassName.indexOf(classname) >= 0) return(true);
		return false;
	},
	addClass = function (elem,classname) {
		if (!elem || !classname) return;
		var currentClassName = elem.className;
		if (currentClassName.indexOf(classname) >= 0) return;
		currentClassName += " " + classname;
		elem.className = currentClassName;
		//alert("addClass new className: '" + currentClassName + "'");
		return;
	},
	removeClass = function (elem,classname) {
		if (!elem || !classname) return;
		var currentClassName = elem.className,
			re;
		if (currentClassName.indexOf(classname) < 0) return;
		re = new RegExp(classname,"g");		// get html <img .....> construct
		currentClassName = currentClassName.replace(re, "");
		currentClassName = trim(currentClassName);
		//alert("removeClass new className: '" + currentClassName + "'");
		elem.className = currentClassName;
		return;
	};
	// make these functions accessible globally
	_sb_fn.hasClass = hasClass;
	_sb_fn.addClass = addClass;
	_sb_fn.removeClass = removeClass;


	/* ========================
	 * Return the number of pixels from left of a requested object to the <Body> tag
	 */
	var getLeftPos = function (obj,intern,onlythis) {
		// let's get the distance of all elements from outer frame to parent's outer frame
		// Firefox gets offsetTop returns pixels from the inner (inside  frame) position of the parent element to the outer frame of requested element: we must ad the frame width
		// SAFARI  gets offsetTop returns pixels from the outer (outside frame) position of the parent element to the outer frame of requested element
		var tempEl,
			xPos = 0,
			transform = "";
		try {	// old versions of IE 6 WIN  do not know userAgent
			if ( (_sb_s.userAgentUC.indexOf("MSIE") > -1) && _sb_s.is_Mac )  {	// for Explorer Mac
				if (isNaN(obj.clientLeft) == false) xPos = parseInt(obj.clientLeft, 10);
			}
			else xPos = parseInt(obj.offsetLeft, 10);
		}
		catch(ex) { xPos = parseInt(obj.offsetLeft, 10); }

		if ((typeof(obj.style.transform) != "undefined") || (typeof(obj.style.webkitTransform) != "undefined") || (typeof(obj.style.MozTransform) != "undefined")) {	// chrome and safari
			if (typeof(obj.style.webkitTransform) != "undefined") transform = obj.style.webkitTransform;
			else if (typeof(obj.style.MozTransform) != "undefined") transform = obj.style.MozTransform;
				 else if (typeof(obj.style.transform) != "undefined") transform = obj.style.transform;
			//alert(transform);
			if (transform.indexOf("translate(") >= 0) {
				transform = transform.split("(");
				transform = transform[1].split("px");
				//alert(parseInt(transform[0]), 10);
				xPos += parseInt(transform[0], 10);
			}
		}
		if (onlythis) return(xPos);

		tempEl = obj.offsetParent;
		while (tempEl != null) {
			if (tempEl.id.indexOf("sb_body") == 0) {break;}
			//alert("tempEl:"+tempEl+"\ntype:"+(tempEl.tagName)+"\nid:"+(tempEl.id)+"\noffsetLeft:"+tempEl.offsetLeft+"\nxPos:"+xPos);

			xPos += parseInt(tempEl.offsetLeft, 10);
			if ((tempEl.id == "scrollview-content") && ((typeof(tempEl.style.transform) != "undefined") || (typeof(tempEl.style.webkitTransform) != "undefined") || (typeof(tempEl.style.MozTransform) != "undefined"))) {	// chrome and safari
				transform = "";
				if (typeof(tempEl.style.webkitTransform) != "undefined") transform = tempEl.style.webkitTransform;
				else if (typeof(tempEl.style.MozTransform) != "undefined") transform = tempEl.style.MozTransform;
					 else if (typeof(tempEl.style.transform) != "undefined") transform = tempEl.style.transform;
				//alert(transform);
				if (transform.indexOf("translate(") >= 0) {
					transform = transform.split("(");
					transform = transform[1].split("px");
					//alert(parseInt(transform[0]), 10);
					xPos += parseInt(transform[0], 10);
				}
			}

			tempEl = tempEl.offsetParent;
		}

		if (intern && (obj.style.borderLeftWidth)) xPos += parseInt(obj.style.borderLeftWidth, 10);
		return xPos;
	},

	/* ========================
	 * Return the number of pixels from top of a requested object to the <Body> tag
	 */
	getTopPos = function (obj,intern) {
		// let's get the distance of all elements from outer frame to parent's outer frame
		// Firefox gets offsetTop returns pixels from the inner (inside  frame) position of the parent element to the outer frame of requested element: we must ad the frame width
		// SAFARI  gets offsetTop returns pixels from the outer (outside frame) position of the parent element to the outer frame of requested element
		var yPos = 0,
			tempEl;
		try {	// old versions of IE 6 WIN  do not know userAgent
			if ( (_sb_s.userAgentUC.indexOf("MSIE") > -1)
				&& _sb_s.is_Mac )  {	// for Explorer Mac
				if (isNaN(obj.clientTop) == false) yPos = parseInt(obj.clientTop, 10);
			}
			else yPos = parseInt(obj.offsetTop, 10);
		}
		catch(ex) { yPos = parseInt(obj.offsetTop, 10); }

		tempEl = obj.offsetParent;
		while (tempEl != null) {
			if (tempEl.id.indexOf("sb_body") == 0) {break;}
			//alert("tempEl:"+tempEl+"\ntype:"+(tempEl.tagName)+"\noffsetTop:"+tempEl.offsetTop);
			if (_sb_s.is_Firefox && (tempEl.tagName.toLowerCase() == "body")) {;}	// FF adds border-top as negativ offsetLeft - ignore it
			else {
				yPos += parseInt(tempEl.offsetTop, 10);
			}
			tempEl = tempEl.offsetParent;
		}

		if (intern) yPos -= _sb_e.body_borderTopWidth;
		return yPos;
	},

	/**
	 * get the associated page number from an object id
	 * @param theid the id of  an area element which looks like: c1P1_A1_1
	 * @return the page number (the digits after 'P' up to the '_') as string
	 */
	getPageFromObjID = function (theid) {
		if ((theid == null) || (theid == "")) return("");
		var parts = theid.split("_"),
			Ppos = parts[0].indexOf("P");
		return(parts[0].substr(Ppos+1));
	},

	getPageIDXFromArticleID = function (theid) {	// expects an article id like "Art32_3"
		if ((theid == null) || (theid == "")) return(null);
		if (theid.indexOf("_") <= 0) return(null);
		var parts = theid.split("_"),
			pagename = parts[1],
			pageidx = parseInt(pagename, 10)-1;
		if (pageidx >= 0) return(pageidx);
		return(null);
	},
	getArticleIDXFromArticleID = function (theid) {	// expects an article id like "Art32_3"
		if ((theid == null) || (theid == "")) return(null);
		if (theid.indexOf("_") <= 0) return(null);
		var parts = theid.split("_"),
			artidx = null;
		try { artidx = parts[0].split("Art")[1]; } catch(ex){}
		return(artidx);
	},

	getPageIndexFromPageName = function (thePageName) {
		var pagename = thePageName,
			i;
		if ((pagename == null) || (pagename == "")) pagename = "";
		for (i=0; i < _sb_s.totalPages; i++) {
			if (epaper_pages[i][4] == pagename) return(i);
		}
		return(-99);	// not found
	},

	getPageNameFromPageIndex = function (thePageIndex) {
		if ((thePageIndex == null) || (thePageIndex < 0) || (thePageIndex >= _sb_s.totalPages)) return("");
		//alert("thePageIndex: " + thePageIndex);
		return(epaper_pages[thePageIndex][4]);
	},

	endsWith = function (str, s){
		// escape special chars like * . ? with a backslash ////
		if ( (str == null) || (str == "") ) return false;
		if ( (s == null) || (s == "") ) return false;
		var reg = new RegExp (s + "$");
		return reg.test(str);
	},
	trim = function (str) {
		return str.replace(/^\s+/,'').replace(/\s+$/,'')
	},
	trimAll = function (str) {
		return str.replace(/\s+/g,'').replace(/\s+/g,'')
	},
	isEven = function (x) {
		return (x%2)?false:true;
	},

	resolvePath = function (basepath, relative) {
		// base is path like; http://site.com/ or file://path1/path2/
		// it MUST end with '/' or last part will treated as filename and be cut
		var stack,
			base = basepath.substr(0,basepath.lastIndexOf("/")+1),
			parts = relative.split("/"), i;
		stack = base.split("/");
		stack.pop(); // remove current file name (or empty string)
					 // (omit if "base" is the current folder without trailing slash)
		for (i=0; i<parts.length; i++) {
			if (parts[i] == ".") continue;
			if (parts[i] == "..") stack.pop();
			else stack.push(parts[i]);
		}
		return stack.join("/");
	},

	/**
	 * Returns whether two rectangles intersect. Two rectangles intersect if they
	 * touch at all, for example, two zero width and height rectangles would
	 * intersect if they had the same top and left.
	 * @param a A Rectangle.
	 * @param b A Rectangle.
	 * @return {boolean} Whether a and b intersect.
	 */
	rectIntersects = function(a, b) {	// a and be given top/left and right/bottom
	  return (a.left <= b.right && b.left <= a.right &&
		  a.top <= b.bottom && b.top <= a.bottom);
	},
	rectIntersectsW = function(a, b) {	// a and be given top/left and width/height
	  return (a.left <= b.left + b.width && b.left <= a.left + a.width &&
		  a.top <= b.top + b.height && b.top <= a.top + a.height);
	},

	getElementsByClassName = function(classSelector) {
		return($(classSelector));
	};

		
	// IE<=8 does not have array.indexOf() 
	if (!Array.prototype.indexOf) {
		Array.prototype.indexOf = function(obj, start) {
			for (var i = (start || 0), j = this.length; i < j; i++) {
				if (this[i] === obj) { return i; }
			}
			return -1;
		}
	}

	// make utilities funcs globally available
	_sb_fn.isEven = isEven;
	_sb_fn.trim = trim;
	_sb_fn.endsWith = endsWith;
	_sb_fn.rectIntersects = rectIntersects;
	_sb_fn.getElementsByClassName = getElementsByClassName;
	_sb_fn.getPageIDXFromArticleID = getPageIDXFromArticleID;
	_sb_fn.getArticleIDXFromArticleID = getArticleIDXFromArticleID;




	/* ========================
	 * page image loader
	 */
	var page_images_max_loadchannels = 3,	// max number of concurrent channels to use when loading page images
		page_images_loading = 0,			// number of concurrent loading page images
		stop_preload_pageimages = 0,		// stop/go flag for page image preload
		doing_preload_pageimages = false,
	
		preload_pageimages_errors = 0,
		preloadWatcherCount = 0,
		pageImageUnloadUnused_timer = null,

		pageload_status_inited = false,
		pageload_status_div = null,
		page_sprocket_status_div = null,
		pageload_status_table_row = null;

	$.load_page = function (objID,pg_num,src,istask) {
		if (typeof(istask) == 'undefined') {
			setTimeout(function(){$.load_page(objID,pg_num,src,true);}, 1);
			return;
		}
		if ((typeof(epaper_pages) == 'undefined') || (typeof(epaper_pages[pg_num]) == 'undefined')) {
			setTimeout(function(){$.load_page(objID,pg_num,src,true);}, 1);
			return;
		}

		var the_src = "";
		if ( _sb_s.is_IE && _sb_s.is_Mac )  {	// for Explorer Mac
			the_src = src;
		}
		else try { the_src = encodeURI(src); } catch(ex){}	// explorer 5 (old) does not know this
		//alert("load_page: " + pg_num + "\nsrc: " + the_src + "\nobjID: " +objID);
		if (epaper_pages[pg_num][8] && endsWith("/"+epaper_pages[pg_num][8].src,src)) {	// is already loaded
			//log_methods.log("load_page ALREADY LOADED: " + pg_num + ", objID: " + objID + "\n\tsrc: " + the_src);
			return;
		}

		//_sb_s.DEBUGmode = 2;log_methods.log("load_page: " + pg_num + ", objID: " + objID + "\n\tsrc: " + the_src + "\n\timg: " + ((epaper_pages[pg_num][8] && epaper_pages[pg_num][8].src) ? epaper_pages[pg_num][8].src : 'undefined'));

		if (!epaper_pages[pg_num][8]) {	// create the image object for this page
			epaper_pages[pg_num][8] = new Image();
			epaper_pages[pg_num][8].isLoading = true;
			epaper_pages[pg_num][8].isLoaded = false;
			epaper_pages[pg_num][8].notFound = false;

			epaper_pages[pg_num][8].onload=function(){ pageFoundLoader(pg_num,epaper_pages[pg_num][8],objID); }
			epaper_pages[pg_num][8].onerror=function(){ pageNotFoundLoader(pg_num,epaper_pages[pg_num][8],objID); }

			epaper_pages[pg_num][8].src = src;
			page_images_loading++;	// one more load channel occupied
		}
		else pageFoundLoader(pg_num,epaper_pages[pg_num][8],objID);
		// all the rest is done by the handlers 'pageFoundLoader' and 'pageNotFoundLoader'
		return;
	};

	var isPageIdxLoaded = function (pageidx) {
		if ((pageidx < 0) || (pageidx > (num_epaper_pages-1))) return false;
		if (!epaper_pages[pageidx]) return false;
		if (!epaper_pages[pageidx][8]) return false;
		if (!epaper_pages[pageidx][8].isLoaded) return false;
		return(true);
	},

	pageFoundLoader = function (pg_num,epaper_pageobj,imgElemID) {	// image found on server
															// epaper_pageobj is the image in the browser's images array
															// imgElemID is the img DOM element
		//alert("pageFoundLoader:\nepaper_pageobj: "+ epaper_pageobj + "\nimage found on Server: " + epaper_pageobj.src);
		if (!epaper_pageobj) return;
		epaper_pageobj.isLoading = false;
		epaper_pageobj.isLoaded=true;
		epaper_pageobj.notFound=false;
		epaper_pageobj.setAttribute("onload",null);	// detach event
		epaper_pageobj.setAttribute("onerror",null);	// detach event
		// set the source attribute of the image element
		switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
			default:
			case "turn":
				//log_methods.log("pageFoundLoader _sb_s.pageTurnMode: " + _sb_s.pageTurnMode + "\npg index: "+pg_num + "\nobj:"+epaper_pageobj + "\nsrc: "+epaper_pageobj.src + "\n_sb_s.pageTurnMode: "+_sb_s.pageTurnMode);
				_sb_e.sb_pagelist.pagesrc(pg_num+1,epaper_pageobj.src);
				break;
			case "slide":
				var browserpage_obj = document.getElementById(imgElemID);	// the image object in the browser's page scroll list
				browserpage_obj.src = epaper_pageobj.src;
				attachGestureDetector(browserpage_obj,null,"pageimage_"+imgElemID);
			break;
		}
		if (page_images_loading > 0) page_images_loading--;	// one less load channel occupied
		return(true);
	},
	pageNotFoundLoader = function (pg_num,epaper_pageobj,imgElemID) {	// image not found on server
		//alert("epaper_pageobj: "+ epaper_pageobj + "\nimage not found on Server: " + epaper_pageobj.src);
		if (!epaper_pageobj) return false;
		epaper_pageobj.isLoading = false;
		epaper_pageobj.isLoaded=false;
		epaper_pageobj.notFound=true;
		epaper_pageobj.setAttribute("onload",null);	// detach event
		epaper_pageobj.setAttribute("onerror",null);	// detach event
		epaper_pageobj.src = _sb_s.xslcssPath+"notfoundpage.jpg";
		//log_methods.log("pageNotFoundLoader pg index: "+pg_num + "\nobj:"+epaper_pageobj + "\nsrc: "+epaper_pageobj.src);
		switch (_sb_s.pageTurnMode) {		// can be 'turn' or 'slide'
			default:
			case "turn":
				_sb_e.sb_pagelist.pagesrc(pg_num+1,_sb_s.xslcssPath+"notfoundpage.jpg");
				break;
			case "slide":
				var browserpage_obj = document.getElementById(imgElemID);	// the image object in the browser's page scroll list
				browserpage_obj.src = epaper_pageobj.src;
				attachGestureDetector(browserpage_obj,null,"pageimage_"+imgElemID);
				break;
		}
		if (page_images_loading > 0) page_images_loading--;	// one less load channel occupied
		return(true);
	},
	hold_preload_pageimages = function (stopGo) {
			//$.fn.log("log","hold_preload_pageimages: " + stop_preload_pageimages);
		if (stopGo > 0) {	// hold page preloading
			stop_preload_pageimages++;
			$.preloadWatcher();
		}
		else {	// evtl. release page preloading
			if (stop_preload_pageimages > 0) stop_preload_pageimages--;	// stop/go flag for page image preload
			if (stop_preload_pageimages <= 0) {
				$.preloadWatcher();
			}
		}
	};

	$.stopPreloadPagesUntilBodyLoaded = function (init) {
		if (init == 0) {
			hold_preload_pageimages(1);
		}
			//$.fn.log("log","stopPreloadPagesUntilBodyLoaded init: " + init);
		if (_sb_s.slidebook_load_state.indexOf("1") < 0) {
			setTimeout("$.stopPreloadPagesUntilBodyLoaded(1)",100);
		}
		else {
			hold_preload_pageimages(0);
		}
		return;
	};
	$.preloadWatcher = function () {
		preloadWatcherCount++;
		if ((stop_preload_pageimages <= 0) || (preloadWatcherCount > 500)) {	// wait max 5 secs before releasing page preload again
			//try { document.getElementById('debugmessage').innerHTML = "Released page preload from Watcher";}catch(ex){}
			stop_preload_pageimages = 0;
			preloadWatcherCount = 0;
			return;
		}
		setTimeout("$.preloadWatcher()",10);
		return;
	};
	$.preload_pageimages = function (pageidx,istask) {
		var eppi0, pageloadDelay;
		if ( (pageidx < 0) || (pageidx >= num_epaper_pages) ) {
			doing_preload_pageimages = false;
			return;
		}
		if (typeof(istask) == 'undefined') {
			setTimeout(function(){$.preload_pageimages(pageidx,true);},1);
			return;
		}

		try {
			if ((stop_preload_pageimages > 0) && (pageidx > 3)) {	// let preload the first 3 page images
				setTimeout(function(){$.preload_pageimages(pageidx,true);},50);
				return;
			}
		}
		catch(ex){}

		if ((typeof(epaper_pages) == 'undefined') || (typeof(num_epaper_pages) == 'undefined')) {
			setTimeout(function(){$.preload_pageimages(pageidx,true);}, 50);
			return;
		}
		// check if we can access the page image
		try {
			eppi0 = _sb_fn.getPageImage(pageidx);
		} catch(ex) {
			preload_pageimages_errors++;
			if (preload_pageimages_errors < 20) {
				setTimeout(function(){$.preload_pageimages(pageidx,true);},50);
				return;
			}
			if (preload_pageimages_errors >= 20) alert("ERROR loading page index " + pageidx);
			return;
		}
		if (page_images_loading >= page_images_max_loadchannels) {	// wait until we have free download channels
			setTimeout(function(){$.preload_pageimages(pageidx,true);},50);
			return;
		}
		doing_preload_pageimages = true;

		$.load_page("pv_P"+(pageidx+1),pageidx,_sb_fn.getPageImage(pageidx),false);

		// prepare next page to preload
		pageloadDelay = 20;
		if (pageidx <= 6) {
			if (pageidx == 0) pageloadDelay = 100;	// get second page image after x ms
			else pageloadDelay = pageidx*20;		// give the first 6 pages more time to load
		}
		setTimeout(function(){$.preload_pageimages(pageidx+1,true);},pageloadDelay);
		return;
	};

	$.pageImageUnloadUnused = function (istask) {
		if (_sb_s.pageImageLoadType >= num_epaper_pages-_sb_s.pageImageLoadType) return;
		if (!pageImageUnloadUnused_timer || (typeof(istask) == 'undefined')) {
			if (pageImageUnloadUnused_timer != null) return; // timer for this task already started
			pageImageUnloadUnused_timer = setTimeout(function(){$.pageImageUnloadUnused(true);}, 300);
			return;
		}
		var curpgidx = _sb_s.currentPageIdx;
		if (curpgidx > num_epaper_pages-_sb_s.pageImageLoadType) curpgidx = num_epaper_pages-_sb_s.pageImageLoadType;
		var	vispgStart = curpgidx - _sb_s.pageImageLoadType + 1,
			vispgEnd = curpgidx + _sb_s.pageImageLoadType - 1,
			p = 0, scrimg,
			dummypageimage = "d_p.gif",
			dummypageimagepath = _sb_s.xslcssPath + dummypageimage;
		//document.getElementById('debugmessage').innerHTML = "pageImageUnloadUnused _sb_s.currentPageIdx: " + _sb_s.currentPageIdx + "\nvispgStart: " + vispgStart + "\nvispgEnd: " + vispgEnd;
		// trash images from the page_images array
		for (p=0; p<num_epaper_pages; p++) {
			if (!epaper_pages[p][8]) continue;
			if ((p < vispgStart) || (p > vispgEnd) ) {
				epaper_pages[p][8] = null;
			}
		}
		// trash images from the sb_pagelist scrollview list
		//alert("dummypageimage: "+dummypageimage);
		for (p=0; p<num_epaper_pages; p++) {
			if ((p < vispgStart) || (p > vispgEnd) ) {
				scrimg = document.getElementById("pv_P"+(p+1));
				if (!scrimg) break;
				if (!endsWith(scrimg.src,dummypageimage)) {
					//document.getElementById('debugmessage').innerHTML = "pageImageUnloadUnused pageimg: " + (p+1) + "\nvispgStart: " + vispgStart + "\nvispgEnd: " + vispgEnd + " src: " + scrimg.src + " - "+endsWith(scrimg.src,dummypageimage);
					//alert("pageImageUnloadUnused pageimg: " + (p+1) + "\nvispgStart: " + vispgStart + "\nvispgEnd: " + vispgEnd + " src: " + scrimg.src + " - "+endsWith(scrimg.src,dummypageimage));
					scrimg.src = dummypageimagepath;
				}
			}
		}
		clearTimeout(pageImageUnloadUnused_timer);
		pageImageUnloadUnused_timer = null;
		return;
	};

	$.show_pageimages_load_status = function () {
		var i, status_table, pageload_status_type_css,
			not_loaded = 0, loaded = 0;

		if (typeof(epaper_pages) == 'undefined') { setTimeout("show_pageimages_load_status()", 200); return; }
		if (!pageload_status_inited) {	// not initialized
			pageload_status_type_css = get_css_value("page_load_status","zIndex");	// try to get from css
			if ((typeof pageload_status_type_css != "undefined") && (pageload_status_type_css != "")) {
				_sb_s.pageloadStatusType = parseInt(pageload_status_type_css, 10);
			}
			pageload_status_inited = true;
		}
		if (_sb_s.pageloadStatusType == 0) return;	// no loader status display
		if (pageload_status_div == null) {
			pageload_status_div = document.getElementById('loadstatus_div');
			if (!pageload_status_div) return;
		}

		if ( (_sb_s.pageloadStatusType & 3) > 0 ) {	// show spinning wheel
			for (i=0; i<num_epaper_pages; i++) {
				try {
					if (!epaper_pages[i][8]) { not_loaded++; continue; }
				} catch(ex) { not_loaded++; continue; }
				if ( (epaper_pages[i][8].complete)			// works for Firefox, IE Mac&Win, Safari
					|| (epaper_pages[i][8].isLoaded) ) {	// works for Firefox, IE Mac&Win (NOT in Safari)
					loaded++;
					continue;
				}
				not_loaded++;	// is currently loading
			}
			if (page_sprocket_status_div == null) {
				pageload_status_div.innerHTML = "<div id=\"page_sprocket_status_div\"></div>";
				page_sprocket_status_div = document.getElementById('page_sprocket_status_div');
			}
			if (page_sprocket_status_div && ((_sb_s.pageloadStatusType & 2) == 0) ) page_sprocket_status_div.innerHTML = "&nbsp;" + loaded + "/" + num_epaper_pages;
		}

		if ( (_sb_s.pageloadStatusType & 4) > 0) {	// show table
			not_loaded = 0; loaded = 0;
			if (!pageload_status_table_row) {
				status_table = "<div id=\"loadstatus_table_row\" style=\"disploay:inline-block;font-size:8pt; font-family:sans-serif; color:#f99;\">";
				for (i=0; i<num_epaper_pages; i++) status_table += "<span>" + ((i+1)%10) + " </span>";
				status_table += "</div>";
				pageload_status_div.innerHTML += status_table;
				pageload_status_table_row = document.getElementById('loadstatus_table_row');
				// this MUST BE! because we have added the 'loadstatus_table_row' div
				page_sprocket_status_div = document.getElementById('page_sprocket_status_div');
			}
			if (pageload_status_table_row) {
				for (i=0; i<num_epaper_pages; i++) {
					if (!epaper_pages[i][8]) { not_loaded++; pageload_status_table_row.childNodes[i].style.color = "#F88"; continue; }
					if ( (epaper_pages[i][8].isLoaded)		// works for Firefox, IE Mac&Win (NOT in Safari)
						|| (epaper_pages[i][8].complete) ) {	// works for Firefox, IE Mac&Win, Safari
						pageload_status_table_row.childNodes[i].style.color = "#79F423";
						loaded++;
						continue;
					}
					not_loaded++;	// is currently loading
				}
			}
		}

		if ((_sb_s.pageImageLoadType == 0) && (loaded < num_epaper_pages)) setTimeout("$.show_pageimages_load_status()", 500);
		else {
			if (_sb_s.DEBUGPAGELOADSTATUS == false) pageload_status_div.innerHTML = "";	// clear load bar
			else pageload_status_div.innerHTML = "load complete: " + pageload_status_div.innerHTML;
		}
		return;
	};

	var check_preloaded_pageimages = function (which_page) {
		if ((typeof(epaper_pages) == 'undefined') || (typeof(num_epaper_pages) == 'undefined')) return false;
		var all_images_loaded = false,
			start_page = 0,
			end_page = num_epaper_pages - 1,
			i = 0;
		if (which_page != null) {
			end_page = start_page = which_page;
		}

		for (i = start_page; i <= end_page; i++) {	// IE problem: ignore page_0.gif isLoaded is never set to true
			if ((epaper_pages[i][8] == null) || (typeof(epaper_pages[i][8]) == 'undefined')) return false;
			try {
				if ( (epaper_pages[i][8].src.indexOf("/d_p.gif") >= 0)	// the dummy pages must have gone
					|| ( (!epaper_pages[i][8].isLoaded)		// works for Firefox, IE Mac&Win (NOT in Safari)
						&& (!epaper_pages[i][8].complete) )		// works for Firefox, IE Mac&Win, Safari
					) {
					all_images_loaded = false;
					break;
				}
			} catch(ex) {
				//document.getElementById('debugmessage').innerHTML = "ERROR check_preloaded_pageimages at epaper_pages[i] for i = " + i + " - of total: " + num_epaper_pages;
			}
		}
		if (i >= _sb_s.totalPages) all_images_loaded = true;
		if ((which_page == null) && (all_images_loaded == true)) {
			//alert("all images loaded from " + start_page + " to " + i + " of " + _sb_s.totalPages);
			if (_sb_s.slidebook_load_state.indexOf("3") < 0) _sb_s.slidebook_load_state += "3";
		}
		return all_images_loaded;
	};

	$.wait_preloaded_pageimages = function () {
		var all_images_loaded = check_preloaded_pageimages(null);
		if (all_images_loaded != true) {
			window.status = 'Loading Slidebook Pages...';
			setTimeout(function(){$.wait_preloaded_pageimages();},200);
		}
		else {
			window.status = 'Slidebook Ready';
		}
		return all_images_loaded;
	};



	/**
	 * An XMLHttpRequest function to get external stuff
	 * call like:
	 *		new xmlHttpRequest('GET', 'http://www.easepub.net/SLIDEbooks/search/search_sb.php?action=getobjectsissuesdrops',  'text/html', null, null, 'request1_target', "div", "xmlHttpObjMsg1");
	 *		new xmlHttpRequest('GET', 'http://www.easepub.net/SLIDEbooks/search/search_sb.php?action=getobjectsissuesdrops',  'text/html', null, null, 'request1_target', "div");
	 *
	 *		new xmlHttpRequest('GET', 'http://domain.com/path/XHTTPtunnel.php?url=http://www.easepub.net/SLIDEbooks/search/search_sb.php?action=getobjectsissuesdrops', 'text/html', null, null, 'ifrm', "iframe", "xmlHttpObjMsg2");
     *
     * @param {string} requesttype the type of a request: 'GET' or 'POST'
     * @param {string} url the URL to call
     * optional parameters;
     * @param {string|null=} mimetype
     * @param {Function|null=} cb_onload
     * @param {string|null=} cb_onloadParam
     * @param {string|null=} targetId
     * @param {string|null=} targetType
     * @param {string|null=} statusmessageId
     * @param {FormData|null=} postdata
     * @constructor
 	 */
	var xmlHttpRequest = function (requesttype, url, mimetype, cb_onload, cb_onloadParam, targetId, targetType, statusmessageId, postdata) { 
		var xmlHttpObject,	// create xmlHttpObject
			mytargettype,
			targetcontainer;
		if (typeof XMLHttpRequest != 'undefined') xmlHttpObject = new XMLHttpRequest();
		// if xmlHttpObject is undefined we have to make one for IE <= 6
		if (!xmlHttpObject
			|| ( (_sb_s.is_IE && (_sb_s.IEVersion <= 8)) && (url.indexOf("file://") >= 0) )	// help from local file system in IE8 needs the ActiveXObject
			) {
			try { xmlHttpObject = new ActiveXObject("Msxml2.XMLHTTP"); }
			catch(ex) {
				try { xmlHttpObject = new ActiveXObject("Microsoft.XMLHTTP"); }
				catch(ex1) { xmlHttpObject = null; }
			}
		}
		if (xmlHttpObject == null) return(-1);	// error - return

		// function reacts at status change
		function handleStateChange() {
			// return current status (if wanted)
			if (statusmessageId) document.getElementById(statusmessageId).innerHTML = "xmlHttpObject.readyState = " + xmlHttpObject.readyState + (xmlHttpObject.readyState >= 3 ? " HTTP-Status = " + xmlHttpObject.status : '');
				/* readyState values: 0 = Uninitiated, 1 = Loading, 2 = Loaded, 3 = Interactive, 4 = Complete */
			if (xmlHttpObject.readyState != 4) {
				return;
			}
			// the result is ready
			if (cb_onload) {	// call the result handler if given
				cb_onload(cb_onloadParam,xmlHttpObject.status,xmlHttpObject.responseText);
				return;
			}

			if (!targetId) return;	// no result is to be returned
		
			mytargettype = targetType;
			targetcontainer = document.getElementById(targetId);
			if (!mytargettype) {	// IE9 and below can not do this: must spezify type in 'targetType' at call!!
				mytargettype = targetcontainer.toString();
				if (mytargettype.toLowerCase().indexOf("iframe") >= 0) mytargettype = "iframe"
				else mytargettype = "div";
			}
			switch (mytargettype) {
				case 'iframe':
					var ifrmdoc = (targetcontainer.contentWindow || targetcontainer.contentDocument);
					if (ifrmdoc.document) ifrmdoc = ifrmdoc.document;

					ifrmdoc.open('text/html', 'replace');
					ifrmdoc.write(xmlHttpObject.responseText);
					ifrmdoc.close();
					break;
				case 'before':
					targetcontainer.insertAdjacentHTML('beforebegin', xmlHttpObject.responseText);
					break;
				case 'prepend':
					targetcontainer.insertAdjacentHTML('afterbegin', xmlHttpObject.responseText);
					break;
				case 'append':
					targetcontainer.insertAdjacentHTML('beforeend', xmlHttpObject.responseText);
					break;
				case 'after':
					targetcontainer.insertAdjacentHTML('afterend', xmlHttpObject.responseText);
					break;
				default:
					targetcontainer.innerHTML = xmlHttpObject.responseText;
					break;
			}
		}
		if(mimetype && xmlHttpObject.overrideMimeType) {	// default is "text/xml"
			xmlHttpObject.overrideMimeType(mimetype);
		}
		//alert("requesttype: " + requesttype + "\nurl: " + url);
		xmlHttpObject.open(requesttype, url, true);	// prepare request
		xmlHttpObject.onreadystatechange = handleStateChange;	// set state change handler
		if (postdata) xmlHttpObject.send(postdata);
		else xmlHttpObject.send(null);	// send request
		if (statusmessageId) document.getElementById(statusmessageId).innerHTML = "Request sendt: " + url;
	};
	// make globally available
	_sb_fn.xmlHttpRequest = xmlHttpRequest;



	/* language dependent strings array where languages are like this:
	lg[0][0]="english";
	lg[0][1]="deutsch";
	lg[0][2]="francais";
	lg[0][3]="dansk";
	lg[0][4]="polish";
	lg[0][5]="";	//5-7 are free: not currently translated
	lg[0][6]="";
	lg[0][7]=""; */
	var num_text_items=46,	//number of language dependent string array elements
		lg=new Array(num_text_items);
	_sb_s.lg = lg;	// make globally available
	for (var i=0; i<num_text_items; i++) { lg[i]=new Array(5); }	//prepare sub-arrays for 8 languages

	var tag_s="<span style=\"color:#404080;font-size:10pt\"><b>",
		tag_e="</b></span><br>";
	lg[0][0]=tag_s+"Display Mode:"+tag_e;
	lg[0][1]=tag_s+"Anzeige Modus:"+tag_e;
	lg[0][2]=tag_s+"Mode d'affichage:"+tag_e;
	lg[0][3]=tag_s+"Visningsform:"+tag_e;
	lg[0][4]=tag_s+"Tryb wy\u015Bwietlania:"+tag_e;

	tag_s="<span style=\"color:#333333; font-size: 8pt; font-family: 'Verdana','Arial',sans-serif; white-space:nowrap;\">";
	tag_e="</span>";
	lg[1][0]=tag_s+"Article Text"+tag_e;
	lg[1][1]=tag_s+"Artikel Text"+tag_e;
	lg[1][2]=tag_s+"L'article en texte"+tag_e;
	lg[1][3]=tag_s+"Artikel i tekstform"+tag_e;
	lg[1][4]=tag_s+"Tekst artyku\u0142u"+tag_e;

	tag_s="<span style=\"color:#333333; font-size: 8pt; font-family: 'Verdana','Arial',sans-serif; white-space:nowrap;\">";
	tag_e="</span>";
	lg[2][0]=tag_s+"Article JPEG"+tag_e;
	lg[2][1]=tag_s+"Artikel JPEG"+tag_e;
	lg[2][2]=tag_s+"L'article en JPEG"+tag_e;
	lg[2][3]=tag_s+"Artikel som jpg-billede"+tag_e;
	lg[2][4]=tag_s+"Artyku\u0142 w formacie JPEG"+tag_e;

	lg[3][0]=tag_s+"PDF"+tag_e;
	lg[3][1]=tag_s+"PDF"+tag_e;
	lg[3][2]=tag_s+"PDF"+tag_e;
	lg[3][3]=tag_s+"PDF"+tag_e;
	lg[3][4]=tag_s+"PDF"+tag_e;

	lg[4][0]=tag_s+"PDF"+tag_e;
	lg[4][1]=tag_s+"PDF"+tag_e;
	lg[4][2]=tag_s+"PDF"+tag_e;
	lg[4][3]=tag_s+"PDF"+tag_e;
	lg[4][4]=tag_s+"PDF"+tag_e;

				//when article is displayed: the text in link anchor to the JPEG view
	tag_s="<span style=\"font-family:'Verdana',sans-serif;font-size:8pt;font-weight:bold;color:#404080;background-color:#FFFFFF;text-decoration:none\">&#160;";
	tag_e="&#160;&#160;&raquo;</span>";
	lg[5][0]=tag_s+"Article JPEG"+tag_e;
	lg[5][1]=tag_s+"Artikel JPEG"+tag_e;
	lg[5][2]=tag_s+"Article JPEG"+tag_e;
	lg[5][3]=tag_s+"Artikel JPEG"+tag_e;
	lg[5][4]=tag_s+"Artyku\u0142 w formacie JPEG"+tag_e;

				//when article is displayed: the text in link anchor to the PDF view
	lg[6][0]=tag_s+"Article PDF"+tag_e;
	lg[6][1]=tag_s+"Artikel PDF"+tag_e;
	lg[6][2]=tag_s+"Article PDF"+tag_e;
	lg[6][3]=tag_s+"Artikel PDF"+tag_e;
	lg[6][4]=tag_s+"Artyku\u0142 w formacie PDF"+tag_e;

	lg[8][0]="You must login to read articles!\n";
	lg[8][1]="Bitte anmelden um Artikel zu lesen!\n";
	lg[8][2]="Veuillez-vous inscrire pour lire des articles!\n";
	lg[8][3]="Du skal v\u00e6re logget ind for at benytte friteksts\u00f8gningen!";
	lg[8][4]="Musisz zalogowa\u0107 si\u0119, aby przeczyta\u0107 artyku\u0142y\n";

	lg[9][0]="You must login to view PDFs!\n";
	lg[9][1]="Bitte anmelden um PDFs anzuzeigen!\n";
	lg[9][2]="Veuillez-vous inscrire pour afficher des PDF!\n";
	lg[9][3]="Du skal v\u00e6re logget ind for at for at se PDF-filen!\n";
	lg[9][4]="Musisz zalogowa\u0107 si\u0119, aby ogl\u0105da\u0107 PDF-y\n";

	lg[11][0]="Article as text";
	lg[11][1]="Artikel in Textform";
	lg[11][2]="L'article en texte";
	lg[11][3]="Artikel i tekstform";
	lg[11][4]="Artyku\u0142 w formacie tekstowym";

	lg[12][0]="Article as JPEG";
	lg[12][1]="Artikel als JPEG";
	lg[12][2]="L'article en JPEG";
	lg[12][3]="Artikel som jpg-billede";
	lg[12][4]="Artyku\u0142 w formacie JPEG";

	lg[13][0]="Page PDF";
	lg[13][1]="Seiten-PDF";
	lg[13][2]="PDF de cette page";
	lg[13][3]="Side som PDF";
	lg[13][4]="Strona w formacie PDF";

	lg[14][0]="Click on a page area to display it larger.<br>Flip pages by clicking on the page corners.";
	lg[14][1]="Klicken Sie in einen Seitenbereich, um diesen gr\u00f6sser anzuzeigen.<br>Bl\u00e4ttern Sie, indem Sie mit der Maus auf die Seitenecken klicken.";
	lg[14][2]="Cliquez sur une zone de la page pour l'afficher plus grande.<br>Feuilletez page \u00e0 page avec votre souris en cliquant sur le bord des pages.";
	lg[14][3]="Click on a page area to display it larger.<br>Flip pages by clicking on the page corners.";
	lg[14][4]="Kliknij na wybrany obszar strony, aby go powi\u0119kszy\u0107.<br>Przegl\u0105daj strony poprzez klikni\u0119cie na naro\u017Cniki stron.";

	lg[15][0]="<span style=\"font-family:'Verdana',sans-serif;font-size:12pt;font-weight:bold;color:red;\">You must enable Cookies and Session Cookies to use the Shop!</span>";
	lg[15][1]="<span style=\"font-family:'Verdana',sans-serif;font-size:12pt;font-weight:bold;color:red;\">Sie m\u00fcssen Cookies und Session-Cookies erlauben um den Shop zu benutzen!</span>";
	lg[15][2]="<span style=\"font-family:'Verdana',sans-serif;font-size:12pt;font-weight:bold;color:red;\">You must enable Cookies and Session Cookies to use the Shop!</span>";
	lg[15][3]="<span style=\"font-family:'Verdana',sans-serif;font-size:12pt;font-weight:bold;color:red;\">You must enable Cookies and Session Cookies to use the Shop!</span>";
	lg[15][4]="<span style=\"font-family:'Verdana',sans-serif;font-size:12pt;font-weight:bold;color:red;\">Musisz w\u0142\u0105czy\u0107 Cookies and Session Cookies w przegladarce, aby u\u017Cywa\u0107 sklepu!</span>";

	// fill the logo_td with this content like:
	//   <a href=\"http://www.mydomain.com\" alt=\"To mydomain.com Web Site\"><img id=\"logoIcon_img\" class=\"logoIcon_img\" src=\"**xslcss_path**mydomainLogo_103x40.gif\" title=\"To mydomain web site\"></a>
	lg[16][0]="";
	lg[16][1]="";
	lg[16][2]="";
	lg[16][3]="";
	lg[16][4]="";

	lg[20][0] = "Add this product to the cart";
	lg[20][1] = "Dieses Produkt in den Warenkorb legen";
	lg[20][2] = "Ajouter ce produit au panier";
	lg[20][3] = "Add this product to the cart";
	lg[20][4] = "Dodaj ten produkt do koszyka";

	lg[21][0] = "This product has been added to the cart.";	//\n\nReturn to this catalogue to select more articles";
	lg[21][1] = "Das Produkt wurde in den Warenkorb gelegt.";	//\n\nKehren Sie zu diesem Katalog zur\u00fcck um weiter einzukaufen.";
	lg[21][2] = "Ce produit est maintenant dans le panier.";	//\n\nRetournez au catalogue pour continuer vos achats.";
	lg[21][3] = "This product has been added to the cart.";	//\n\nReturn to this catalogue to select more articles";
	lg[21][4] = "Wybrany produkt zosta\u0142 dodany do koszyka.";	//\n\nPowr\u0119\u0107 do wybranego katalogu, aby doda\u0107 wi\u0119cej artyku\u0142\u0119w";

	lg[26][0]="Click to open full-text search window";
	lg[26][1]="Klicken um die Volltext-Suche zu \u00f6ffnen";
	lg[26][2]="Cliquer pour la recherche en text int&#xe9;gral";
	lg[26][3]="Benytte friteksts&#x0f8;gningen";
	lg[26][4]="Kliknij, aby otworzy\u0107 okno wyszukiwarki tekstowej";

	lg[27][0]="Page Inspector (magnifying glass) is on. Click to turn off.";
	lg[27][1]="Seiten Inpektor (Lupe) ist eingeschaltet. Klicken um auszuschalten.";
	lg[27][2]="Inspecteur Page (loupe) est activ\u00e9. Clicker pour desactiver.";
	lg[27][3]="Page Inspector (magnifying glass) is on. Click to turn off.";
	lg[27][4]="Inspekcja strony (lupa) jest w\u0142\u0105czona. Kliknij, aby j\u0105 wy\u0142\u0105czy\u0107.";

	lg[28][0]="Page Inspector (magnifying glass) is off. Click to turn on.";
	lg[28][1]="Seiten Inpektor (Lupe) ist ausgeschaltet. Klicken um einzuschalten.";
	lg[28][2]="Inspecteur Page (loupe) est desactiv\u00e9. Clicker pour activer.";
	lg[28][3]="Page Inspector (magnifying glass) is off. Click to turn on.";
	lg[28][4]="Inspekcja strony (lupa) jest wy\u0142\u0105czona. Kliknij, aby j\u0105 w\u0142\u0105czy\u0107.";

	lg[29][0]="Click to download the document as PDF";
	lg[29][1]="Klicken um das Dokument-PDF zu downloaden";
	lg[29][2]="Cliquer pour t\u00e9l\u00e9charger le PDF du document";
	lg[29][3]="Click to download the document as PDF";
	lg[29][4]="Pobierz dokument w formacie PDF";

	lg[30][0]="Page Inspector HELP (magnifying glass)";
	lg[30][1]="Seiten Inpektor HILFE (Lupe)";
	lg[30][2]="Inspecteur Page AIDE (loupe)";
	lg[30][3]="Page Inspector HELP (magnifying glass)";
	lg[30][4]="Inspekcja strony POMOC (lupa)";

	lg[31][0]="Activate the magnifying glass<br>and point the mouse<br>on the page.<br><div style=\"font-size:12pt;font-weight:bold;\">Press and hold the mouse!</div>";
	lg[31][1]="Lupe aktivieren<br>und mit der Maus<br>auf die Seite zeigen.<br><div style=\"font-size:12pt;font-weight:bold;\">Maustaste klicken<br>und halten!</div>";
	lg[31][2]="Activer la loupe et placer<br>la pointe de la souris<br>sur la page.<br><div style=\"font-size:12pt;font-weight:bold;\">Cliquer et maintenir<br>la touche appuy\u00e9e!</div>";
	lg[31][3]="Activate the magnifying glass<br>and point the mouse<br>on the page.<br><div style=\"font-size:12pt;font-weight:bold;\">Press and hold the mouse!</div>";
	lg[31][4]="W\u0142\u0105cz lup\u0119<br>i wska\u017C myszk\u0105<br>wybrany obszar<br>na stronie.<br><div style=\"font-size:12pt;font-weight:bold;\">Wci\u015Bnij i przytrzymaj myszk\u0119!</div>";

	lg[32][0]="<span style=\"font-size:32pt;font-weight:bold;\">&#8593;</span><br>move up<br>to enlarge";
	lg[32][1]="<span style=\"font-size:32pt;font-weight:bold;\">&#8593;</span><br>nach oben<br>bewegen um zu<br>vergr\u00f6ssern";
	lg[32][2]="<span style=\"font-size:32pt;font-weight:bold;\">&#8593;</span><br>bouger vers<br>le haut<br>pour agrandir";
	lg[32][3]="<span style=\"font-size:32pt;font-weight:bold;\">&#8593;</span><br>move up<br>to enlarge";
	lg[32][4]="<span style=\"font-size:32pt;font-weight:bold;\">&#8593;</span><br>przesu\u0144 w g\u00F3r\u0119,<br>aby powi\u0119kszy\u0107";

	lg[33][0]="<span style=\"font-size:32pt;font-weight:bold;\">&#8595;</span><br>move down<br>to shrink";
	lg[33][1]="<span style=\"font-size:32pt;font-weight:bold;\">&#8595;</span><br>nach unten<br>bewegen um zu <br>verkleinern";
	lg[33][2]="<span style=\"font-size:32pt;font-weight:bold;\">&#8595;</span><br>bouger vers<br>le bas<br>pour diminuer";
	lg[33][3]="<span style=\"font-size:32pt;font-weight:bold;\">&#8595;</span><br>move down<br>to shrink";
	lg[33][4]="<span style=\"font-size:32pt;font-weight:bold;\">&#8595;</span><br>przesu\u0144 na d\u00F3\u0142,<br>aby zmniejszy\u0107";

	lg[34][0]="<span style=\"font-size:32pt;font-weight:bold;\">&#8594;</span><br>move right to<br>enlarge magnifying glass";
	lg[34][1]="<span style=\"font-size:32pt;font-weight:bold;\">&#8594;</span><br>nach rechts<br>bewegen um die<br>Lupe zu vergr\u00f6ssern";
	lg[34][2]="<span style=\"font-size:32pt;font-weight:bold;\">&#8594;</span><br>bouger vers<br>la droite pour<br>agrandir la loupe";
	lg[34][3]="<span style=\"font-size:32pt;font-weight:bold;\">&#8594;</span><br>move right to<br>enlarge magnifying glass";
	lg[34][4]="<span style=\"font-size:32pt;font-weight:bold;\">&#8594;</span><br>przesu\u0144 w prawo, aby<br>powi\u0119kszy\u0107 lup\u0119";

	lg[35][0]="<span style=\"font-size:32pt;font-weight:bold;\">&#8592;</span><br>move left to<br>shrink magnifying glass";
	lg[35][1]="<span style=\"font-size:32pt;font-weight:bold;\">&#8592;</span><br>nach links<br>bewegen um die<br>Lupe zu verkleinern";
	lg[35][2]="<span style=\"font-size:32pt;font-weight:bold;\">&#8592;</span><br>bouger vers<br>la gauche pour<br>r\u00e9tr\u00e9cir la loupe";
	lg[35][3]="<span style=\"font-size:32pt;font-weight:bold;\">&#8592;</span><br>move left to<br>shrink magnifying glass";
	lg[35][4]="<span style=\"font-size:32pt;font-weight:bold;\">&#8592;</span><br>przesu\u0144 w lewo, aby<br>zmniejszy\u0107 lup\u0119";

	lg[36][0]="<table><tr><td><img id=\"inspectorPageIcon\" src=\"\"></td><td>Hello! It's me!<br>&laquo;Inspector Page&raquo;</td></tr></table>";
	lg[36][1]="<table><tr><td><img id=\"inspectorPageIcon\" src=\"\"></td><td>Hello! It's me!<br>&laquo;Inspector Page&raquo;</td></tr></table>";
	lg[36][2]="<table><tr><td><img id=\"inspectorPageIcon\" src=\"\"></td><td>Hello! It's me!<br>&laquo;Inspector Page&raquo;</td></tr></table>";
	lg[36][3]="<table><tr><td><img id=\"inspectorPageIcon\" src=\"\"></td><td>Hello! It's me!<br>&laquo;Inspector Page&raquo;</td></tr></table>";
	lg[36][4]="<table><tr><td><img id=\"inspectorPageIcon\" src=\"\"></td><td>Witaj! To ja!<br>&laquo;Twoja lupa&raquo;</td></tr></table>";

	lg[37][0]="show enlarged image";
	lg[37][1]="vergr\u00f6ssertes Bild zeigen";
	lg[37][2]="agrandir cette image";
	lg[37][3]="billede at forøge";
	lg[37][4]="obraz powiększać";



	/* ========================
	 * localized strings for full text search
			ftst[x][0]=en - english, and all not implemented languages
			ftst[x][1]=de - deutsch
			ftst[x][2]=fr - français
			ftst[x][3]=da - dansk
			ftst[x][4]=pl - polish
	 */

	var num_ftsearchtext_items=13,	//number of language dependent string array elements
		ftst=new Array(num_ftsearchtext_items);
	for (var i=0; i<num_ftsearchtext_items; i++) { ftst[i]=new Array(5); }	//prepare sub-arrays for 5 languages

	ftst[0][0]="&nbsp;&nbsp;Search";
	ftst[0][1]="&nbsp;&nbsp;Suchen";
	ftst[0][2]="&nbsp;&nbsp;Recherche";
	ftst[0][3]="&nbsp;&nbsp;S\u00f8g";
	ftst[0][4]="&nbsp;&nbsp;Przeszukiwa\u0107";

	ftst[1][0]="Enter search term(s)";
	ftst[1][1]="Suchbegriff(e) eingeben";
	ftst[1][2]="Recherche document";
	ftst[1][3]="S\u00f8g";
	ftst[1][4]="Przeszukiwa\u0107";

	ftst[2][0]="&mdash; All Objects &mdash;";
	ftst[2][1]="&mdash; Alle Objekte &mdash;";
	ftst[2][2]="&mdash; Tous les publications &mdash;";
	ftst[2][3]="&mdash; All Objects &mdash;";
	ftst[2][4]="&mdash; Wszystkich obiektach &mdash;";

	ftst[3][0]="Date Limits";
	ftst[3][1]="Datums-<br>Einschr\u00e4nkung";
	ftst[3][2]="Restrictions date";
	ftst[3][3]="Date Limits";
	ftst[3][4]="Date Limits";

	ftst[4][0]="From:";
	ftst[4][1]="Von:";
	ftst[4][2]="de:";
	ftst[4][3]="From:";
	ftst[4][4]="From:";

	ftst[5][0]="to:";
	ftst[5][1]="bis:";
	ftst[5][2]="\u00e0:";
	ftst[5][3]="to:";
	ftst[5][4]="to:";

	ftst[6][0]="YYYY or YYYYMM or YYYYMMDD";
	ftst[6][1]="JJJJ oder JJJJMM oder JJJJMMTT";
	ftst[6][2]="AAAA ou AAAAMM ou AAAAMMJJ";
	ftst[6][3]="YYYY or YYYYMM or YYYYMMDD";
	ftst[6][4]="YYYY or YYYYMM or YYYYMMDD";

	ftst[7][0]="Get Latest Issue";
	ftst[7][1]="Neuste Ausgabe laden";
	ftst[7][2]="\u00c9dition la plus r\u00e9cente";
	ftst[7][3]="Get Latest Issue";
	ftst[7][4]="Get Latest Issue";

	ftst[8][0]="Search";
	ftst[8][1]="Suchen";
	ftst[8][2]="Recherche";
	ftst[8][3]="S\u00f8g";
	ftst[8][4]="Przeszukiwa\u0107";

	ftst[9][0]="";
	ftst[9][1]="";
	ftst[9][2]="";
	ftst[9][3]="";
	ftst[9][4]="";

	ftst[10][0]="No such issue available!";
	ftst[10][1]="Keine Ausgabe vorhanden!";
	ftst[10][2]="Aucune \u00e9dition disponible!";
	ftst[10][3]="No such issue available!";
	ftst[10][4]="No such issue available!";

	ftst[11][0]="Use Internet Explorer version 9 or newer or another browser";
	ftst[11][1]="Benutzen Sie Internet Explorer Version 9 oder neuer oder einen anderen Browser";
	ftst[11][2]="Utilisez Internet Explorer version 9 ou plus ou un autre navigateur";
	ftst[11][3]="Use Internet Explorer version 9 or newer or another browser";
	ftst[11][4]="Use Internet Explorer version 9 or newer or another browser";

	ftst[12][0]="<div id=\"ft_help_mark\" class=\"ft_help_mark\">?</div>";
	ftst[12][1]="<div id=\"ft_help_mark\" class=\"ft_help_mark\">?</div>";
	ftst[12][2]="<div id=\"ft_help_mark\" class=\"ft_help_mark\">?</div>";
	ftst[12][3]="<div id=\"ft_help_mark\" class=\"ft_help_mark\">?</div>";
	ftst[12][4]="<div id=\"ft_help_mark\" class=\"ft_help_mark\">?</div>";

})(jQuery);



/* ========================
 * scrollable vertical list
 * call: $('#myList').list()
 * Methods
 * ---------
 * .list()
 * Initializes a list component.
 * 
 * .list('setDataProvider', dataProviderArray)
 * Sets the data provider array for the list instance. There is no concrete limit on the length of the data provider array. Elements within the array can be of primitive or complex types.
 * 
 * .list('setLabelFunction', labelFunction)
 * Sets label function that can be used to format items in the data provider array into string values that can be displayed within the list.
 * 
 * .list('setSelectedIndex', index)
 * Set the item at selected index as selected.
 * 
 * .list('getSelectedIndex')
 * Returns the currently selected index. If no list item is selected, returns -1.
 * 
 * .list('clearSelectedIndex')
 * Clears the currently selected item in the list.
 * 
 * Event 
 * ---------
 * change
 * This event is fired when the selected item in the list is changed. You can access details of the selected item in the list by accessing attributes of the event.
 * 
 * event.index
 * The numeric index for the item in the list that was clicked/touched.
 * 
 * event.srcElement
 * A jQuery reference to the list item that was clicked/touched.
 * 
 * event.item
 * A reference to the data item for the list item. If using inline <li> in markup, this will be the same DOM element as event.srcElement. If using a dataProvider, it will be the object in the dataProvider array corresponding to the selected list item.
 */

(function( $ ){

  "use strict";

 /* LIST CLASS DEFINITION
  * ========================= */

	var List = function ( element ) {
          this.touchSupported = false;
          this.$el = null;
        this.init( element );
    };
      
      

  List.prototype = {

    constructor: List,
    
    init: function ( element ) {
        this.withScrollbar = false;

        this.vendorPrefix = this.getVendorPrefix();
        this.transformCSS = "transform";
        switch (this.vendorPrefix) {
         	case '': // no vendor prefix
        		break;
  	     	case 'O': this.transformCSS = "OTransform";
        		break;
        	case 'ms': this.transformCSS = "msTransform";
        		break;
        	default: this.transformCSS = "-"+this.vendorPrefix+"-transform"
        }
		var body = document.body || document.documentElement;
		this.useTransform = false;	// for newer browsers
		this.useTransform = body.style.WebkitTransition !== undefined || body.style.MozTransition !== undefined || body.style.OTransition !== undefined || body.style.transition !== undefined;

        var self = this;
        
        //params used in detecting "click" action - without collision of DOM events            
        this.MAX_CLICK_DURATION_MS = 350;
        this.MAX_MOUSE_POSITION_FLOAT_PX = 10;
        this.MAX_TOUCH_POSITION_FLOAT_PX = 25;
        
        this.SCROLLBAR_BORDER = 1;
        this.SCROLLBAR_MIN_SIZE = 10;
                
        this.RESIZE_TIMEOUT_DELAY = 100;
        
        this.processedItems = {};
        this.totalItems = [];
            
        this.touchSupported = _sb_s.isTouchDevice;

        this.$el = $(element);
        this.$ul = this.$el.find( "ul" );
        if (this.withScrollbar) {
			this.$scrollbar = $("<div id='scrollbar'></div>");
        
			if ( this.$ul.length <= 0 ) {
				this.$ul = $("<ul />");
				this.$el.append( this.$ul );
			}
       		this.$el.append( this.$scrollbar );
		}
        
        this.dataProvider = this.$ul.find( "li" );
        this.listItems = (this.dataProvider.length > 0) ? this.dataProvider : $();
        //this.itemHeight = -1;
        this.itemHeight = this.getItemAtIndex(0).outerHeight() + _sb_fn.cssIntVal(this.getItemAtIndex(0).css("marginTop")) + _sb_fn.cssIntVal(this.getItemAtIndex(0).css("marginBottom"));
        this.scrollHeight = -1;
        //$.fn.log("log","LIST this.scrollHeight: " + this.scrollHeight + "\n\tthis.itemHeight: " + this.itemHeight);
       
        this.listItems.each( function(i) {
            if ( i === 0 ) {
                self.itemHeight = $(this).outerHeight() + _sb_fn.cssIntVal($(this).css("marginBottom"));
                //alert("self.itemHeight: " + self.itemHeight);
            }
  // ****commented Ai           $(this).remove();
        });
        
 // ****commented Ai       this.$ul.css( "visibility", "visible" );
        
        this.yPosition = 0;
        this.updateLayout();
        
        
        this.resizeHandler = function( event ) { return self.onResize(event); };
        this.touchStartHandler = function( event ) { return self.onTouchStart(event); };
        this.touchMoveHandler = function( event ) { return self.onTouchMove(event); };
        this.touchEndHandler = function( event ) { return self.onTouchEnd(event); };
        
        this.TOUCH_START =  _sb_s.pointerEvents.start;
        this.TOUCH_MOVE = _sb_s.pointerEvents.move;
        this.TOUCH_END = _sb_s.pointerEvents.end;
        this.MOUSE_WHEEL = (navigator.userAgent.search("Fire") < 0) ? "mousewheel" : "DOMMouseScroll";
        
        $(window).resize( this.resizeHandler );
        this.$el.bind( this.TOUCH_START, this.touchStartHandler );
        this.$el.bind( this.MOUSE_WHEEL, function( event ) { event.preventDefault();  return self.onMouseWheel(event); } );
        
		if (this.withScrollbar) {
			if ( !this.touchSupported) {
				var sbWidth = _sb_fn.cssIntVal(this.$scrollbar.css( "width" ), 10);
				this.$scrollbar.css( "width", 1.25*sbWidth );
				
				this.scrollbarStartHandler = function( event ) { return self.scrollbarTouchStart(event); };
				this.scrollbarMoveHandler = function( event ) { return self.scrollbarTouchMove(event); };
				this.scrollbarEndHandler = function( event ) { return self.scrollbarTouchEnd(event); };
				this.$scrollbar.bind( this.TOUCH_START, this.scrollbarStartHandler );
			}
			else {
				this.$scrollbar.fadeTo( 0,0 );
			}
		}
        
        this.inputCoordinates = null;
        this.velocity = {distance:0, lastTime:0, timeDelta:0};

    },

getVendorPrefix: function () {
    var tmp = document.createElement("div"),
        prefixes = ['webkit','Moz','ms','Khtml','O'],
        i;
    for (i in prefixes) {
        if (typeof tmp.style[prefixes[i] + 'Transform'] != 'undefined') {
 				//$.fn.log("log","LIST vendor: " + prefixes[i]);
            return (prefixes[i]);
        }
    }
    return("");
},
 /*
    getVendorPrefix: function() {
        //vendor prefix logic from http://lea.verou.me/2009/02/find-the-vendor-prefix-of-the-current-browser/
        var regex = /^(Moz|Webkit|webkit|Khtml|O|ms|Icab)(?=[A-Z])/;
    
        var someScript = document.getElementsByTagName('script')[0];
        for(var prop in someScript.style) {
 				$.fn.log("log","LIST vendor prop: " + prop);
            if(regex.test(prop))
            {
                // test is faster than match, so it's better to perform
                // that on the lot and match only when necessary
 				$.fn.log("log","LIST vendor: " + prop.match(regex)[0]);
               return prop.match(regex)[0];
            }
        }
        for(var prop in document.body.style) {
 				//$.fn.log("log","LIST vendor prop: " + prop);
            if(regex.test(prop))
            {
                // test is faster than match, so it's better to perform
                // that on the lot and match only when necessary
 				$.fn.log("log","LIST vendor: " + prop.match(regex)[0]);
               return prop.match(regex)[0];
            }
        }
    
        // Nothing found so far? Webkit does not enumerate over the CSS properties of the style object.
        // However (prop in style) returns the correct value, so we'll have to test for
        // the precence of a specific property
        if('WebkitOpacity' in someScript.style) { return 'Webkit'; }
        if('KhtmlOpacity' in someScript.style) { return 'Khtml'; }
    
        return '';
    },
*/

	haltEvents: function (e) {
		var evt;
		if (!e) evt = window.event;
		else evt = e;
		if (evt) {
			if (evt.preventDefault) evt.preventDefault();
			if (evt.stopPropagation) evt.stopPropagation();
			if (typeof(evt.cancelBubble) != "undefined") evt.cancelBubble = true;
		}
		return false;
	},

    onResize: function( event ) {
        if (this.resizeTimeout) clearTimeout( this.resizeTimeout );
        var maxPosition = (this.dataProvider.length*this.itemHeight)-(this.$el.height());
        this.yPosition = Math.min( this.yPosition, maxPosition );
        var self = this;
        this.resizeTimeout = setTimeout( function() { 
            self.updateLayout(); 
        }, this.RESIZE_TIMEOUT_DELAY );
    },
    
    onTouchStart: function ( event ) {
 			//$.fn.log("log","LIST event: " + event.type + "\n\ttarget: " + event.target.id);
        this.stopAnimation();
        this.cleanupListItems(true);
        
        if (this.withScrollbar && this.touchSupported ) {
            this.$scrollbar.fadeTo( 300,1 );
        }
        
        this.cleanupEventHandlers();
        
        $(document).bind( this.TOUCH_MOVE, this.touchMoveHandler );
        $(document).bind( this.TOUCH_END, this.touchEndHandler );
        this.$el.bind( this.TOUCH_MOVE, this.touchMoveHandler );
        this.$el.bind( this.TOUCH_END, this.touchEndHandler );
        //alert("this.$el: " + this.$el.attr("id"));
        
        this.inputCoordinates = this.getInputCoordinates( event );
        this.inputStartCoordinates = this.inputCoordinates;
        this.inputStartTime = new Date().getTime();
        
        _sb_s.page_thumbs_nav_scrolled = 0;	// state that we are statriung touch on scroll list
        
        event.preventDefault();
        return false;
    },
    
    onTouchMove: function ( event ) {
       var newCoordinates = this.getInputCoordinates( event );
   			//$.fn.log("log","LIST onTouchMove: " + JSON.stringify(newCoordinates));
       var yDelta = this.inputCoordinates.y - newCoordinates.y;
        
        _sb_s.page_thumbs_nav_scrolled += yDelta;
        
        this.yPosition += yDelta;
        this.updateVelocity( yDelta );
        
        //limit scroll to within range of visible area
        var startPosition = Math.ceil(this.yPosition/this.itemHeight);
        if ( startPosition < 0 && startPosition*this.itemHeight <= -(this.$el.height()-this.itemHeight) ) {
            this.yPosition = -(this.$el.height()-this.itemHeight);
        }
        
        var maxPosition = (this.dataProvider.length*this.itemHeight)-this.itemHeight;
        if ( this.yPosition > maxPosition ) {
            this.yPosition = maxPosition;
        }

        //end scroll limiting
            
        this.updateLayout();
        this.inputCoordinates = newCoordinates;
        
        event.preventDefault();
 			//$.fn.log("log","LIST onTouchMove: " + this.yPosition + " of max: " + maxPosition + "\n\titemHeight: " + this.itemHeight + " * " + this.dataProvider.length);
        return false;
    },
    
    onTouchEnd: function ( event ) {

        this.inputEndCoordinates = this.inputCoordinates;
        var clickEvent = this.detectClickEvent( event );
			//$.fn.log("log","LIST event: " + event.type + "\n\tis clickEvent: " + clickEvent);

        var yDelta = this.inputCoordinates.y - this.inputStartCoordinates.y;

        this.inputCoordinates = null;
        
        this.cleanupEventHandlers();
        
        if ( !clickEvent ) {
            this.scrollWithInertia();
        }
        else {
            this.cleanupListItems();
        }
        event.preventDefault();
        return false;
    },
    
    onMouseWheel: function ( event ) {
        this.stopAnimation();
        clearTimeout( this.cleanupTimeout );
    
        //only concerned about vertical scroll
        //scroll wheel logic from: https://github.com/brandonaaron/jquery-mousewheel/blob/master/jquery.mousewheel.js
        var orgEvent = event.originalEvent,
        	delta = 0;
        
        // Old school scrollwheel delta
        /*
			Chrome: orgEvent.wheelDelta: -7
					orgEvent.detail: 0	always 0
			Safari: orgEvent.wheelDelta: -3
					orgEvent.detail: 0	always 0
			Opera:	orgEvent.wheelDelta: -50
					orgEvent.detail: 1
			FireFox: orgEvent.wheelDelta: undefined
					orgEvent.detail: 3
			IE:		orgEvent.wheelDelta: 240	large number
					orgEvent.detail: 0	always 0

        */
        if ( orgEvent.wheelDelta ) {
        	delta = orgEvent.wheelDelta/1000;	// for IE 9
        	do {
        		if (_sb_s.is_Chrome) { delta = orgEvent.wheelDelta/700; break; }
        		if (_sb_s.is_Safari) { delta = orgEvent.wheelDelta/700; break; }
        	} while(false);
  	  		//$.fn.log("log","LIST wheelDelta: " + delta + "\n\torgEvent.wheelDelta: " + orgEvent.wheelDelta + "\n\torgEvent.detail: " + orgEvent.detail);
		}
        if ( orgEvent.detail ) {	// Firefox and Opera
        	delta = -orgEvent.detail/15;	// Firefox
        	do {
        		if (_sb_s.is_Opera) { delta = -orgEvent.detail/200; break; }
        	} while(false);
 	  		//$.fn.log("log","LIST scrollwheel detail: " + delta + "\n\torgEvent.wheelDelta: " + orgEvent.wheelDelta + "\n\torgEvent.detail: " + orgEvent.detail);
       }
        
        // Webkit
        //if ( orgEvent.wheelDeltaY !== undefined ) { delta = orgEvent.wheelDeltaY/120; }
        
        this.yPosition -= (delta*this.itemHeight);
        
        
        //limit the mouse wheel scroll area
        if (this.scrollHeight == -1) this.scrollHeight = parseInt(this.$el.css('height'), 10);	// not already initialized
       	var maxPosition = ((this.dataProvider.length)*this.itemHeight) - this.scrollHeight;
        //$(window).log("log","this.scrollHeight: " + this.scrollHeight + "\nthis.dataProvider.length: " + this.dataProvider.length + "\nthis.itemHeight: " + this.itemHeight + "\nmaxPosition: " + maxPosition + "\nthis.yPosition: " + this.yPosition);

        if ( this.yPosition > maxPosition ) {
            this.yPosition = maxPosition;
        }

        if ( this.yPosition < 0 ) {
            this.yPosition = 0;
        }
        
        var self = this;
        this.updateLayout();
        this.cleanupTimeout = setTimeout( function(){ self.cleanupListItems(); }, 100 );
        return false;
    },
    
    detectClickEvent: function(event) {
 			//$.fn.log("log","LIST detectClickEvent on\n\tnodeName: " + event.target.nodeName + "\n\tthis.touchSupported: " + this.touchSupported + "\n\tinputStartCoordinates: " + JSON.stringify(this.inputStartCoordinates));
        if ( (event.target.nodeName === "IMG")
       		 || (event.target.nodeName === "UL")
       		 || (event.target.nodeName === "LI")
        	) {
          
            var endTime = new Date().getTime();
            
  			//$.fn.log("log","LIST detectClickEvent\n\tendTime: " + endTime + "\n\tthis.inputStartTime: " + this.inputStartTime);
           if (( endTime - this.inputStartTime ) < this.MAX_CLICK_DURATION_MS ) {
                var delta = {
                    x:    Math.abs( this.inputStartCoordinates.x - this.inputEndCoordinates.x ),
                    y:    Math.abs( this.inputStartCoordinates.y - this.inputEndCoordinates.y )
                };
                
                var triggerEvent = false;
                if ( this.touchSupported ) {
                    triggerEvent = delta.x <= this.MAX_TOUCH_POSITION_FLOAT_PX && delta.y <= this.MAX_TOUCH_POSITION_FLOAT_PX;
                }
                else {
                    triggerEvent = delta.x <= this.MAX_MOUSE_POSITION_FLOAT_PX && delta.y <= this.MAX_MOUSE_POSITION_FLOAT_PX;
                }
                
                if ( triggerEvent ) {
                	//alert("trigger");
                    var index = $(event.target).attr( "list-index" );
                    if (index === this.selectedIndex) { return false; }
                    this.setSelectedIndex( index );
                
                    //make this asynch so that any "alert()" on a change event
                    //does not block the UI from updating the selected row
                    //this is particularly an issue on mobile devices
                    var self = this;
                    setTimeout( function() {
                            var data = { selectedIndex: index, 
                                         srcElement: $(event.srcElement), 
                                         item: self.dataProvider[index]  };
                            var e = jQuery.Event("change", data);
                            self.$el.trigger( e );
                        }, 150 );
                        
                    return true;
                }
            }
        }
        return false;
    },
    
    cleanupEventHandlers: function() {
        $(document).unbind( this.TOUCH_MOVE, this.touchMoveHandler );
        $(document).unbind( this.TOUCH_END, this.touchEndHandler );
        this.$el.unbind( this.TOUCH_MOVE, this.touchMoveHandler );
        this.$el.unbind( this.TOUCH_END, this.touchEndHandler );
        if (this.withScrollbar) {
			$(document).unbind( this.TOUCH_MOVE, this.scrollbarMoveHandler );
        	$(document).unbind( this.TOUCH_END, this.scrollbarEndHandler );
        }
    },
    
    cleanupListItems: function(keepScrollBar) {
        //remove any remaining LI elements hanging out on the dom
        var item, index;
        for ( var x=0; x<this.totalItems.length; x++ ) {
            item = this.totalItems[x];
            index = item.attr( "list-index" );
            if ( this.processedItems[ index ] === undefined ) {
                item.remove();
            }
        }
        //cleanup totalItems array
        var temp = [];
        if ( this.processedItems ) {
			for ( index in this.processedItems)
			{
				temp.push( this.processedItems[ index ] );
			}
        }
        this.totalItems = temp;
        
        if (this.withScrollbar && this.touchSupported && keepScrollBar !== true ) {
            this.$scrollbar.fadeTo( 300,0 );
        }
    },
    
    getInputCoordinates: function ( event ) {
    	return _sb_fn.get_pointerXY(event);
    },
    
    updateLayout: function(ignoreScrollbar) {

        if ( this.dataProvider.length > 0 ) {
            
 //           var height = this.$el.height();            
//            var i = -1;
//            var startPosition = Math.ceil(this.yPosition/this.itemHeight);
//            var offset = -(this.yPosition % this.itemHeight);
            this.setItemPosition( this.$ul, 0, -this.yPosition );
 			//$.fn.log("log","LIST updateLayout: " + this.yPosition);
/*
            this.processedItems = {};
            while (((i)*this.itemHeight) < (height+(2*this.itemHeight))) {
            
                var index = Math.max(  startPosition+i, 0 );
                index = Math.min( index, this.dataProvider.length );
                
                var item = this.getItemAtIndex( index );
                this.totalItems.push( item );
                
                this.processedItems[ index.toString() ] = item;
                this.setItemPosition( item, 0, ((startPosition+i)*this.itemHeight) );
                if ( item.parent().length <= 0 ) {
                    this.$ul.append( item );
                    
                    if ( this.itemHeight <= 0 ) {
                        this.itemHeight = item.outerHeight();
                        this.updateLayout();
                        return;
                    }
                }
                i++;
            }
*/
            if ( ignoreScrollbar !== true ) {
                this.updateScrollBar();
            }
        }
    },
    
	updateScrollBar: function() {
		var parent,
			height = this.$el.height(),
			maxScrollbarHeight = this.$el.height() - (2*this.SCROLLBAR_BORDER),
			maxItemsHeight = (this.dataProvider.length) * this.itemHeight,
			targetHeight = Math.min(maxScrollbarHeight / maxItemsHeight, 1) * maxScrollbarHeight,
			actualHeight = Math.max(targetHeight, this.SCROLLBAR_MIN_SIZE),

			scrollPosition = this.SCROLLBAR_BORDER+((this.yPosition/(maxItemsHeight-height)) * (maxScrollbarHeight-actualHeight));
        if ( scrollPosition < this.SCROLLBAR_BORDER ) {
            actualHeight = Math.max( actualHeight+scrollPosition, 0 );
            scrollPosition = this.SCROLLBAR_BORDER;
		}    
		else if ( scrollPosition > (height-actualHeight) ) {
			actualHeight = Math.min( actualHeight, (height-(scrollPosition+this.SCROLLBAR_BORDER)) );
		}

		if (this.withScrollbar) {
			this.$scrollbar.height( actualHeight );
			parent = this.$scrollbar.parent();

			if ((this.dataProvider.length * this.itemHeight) <= this.$el.height() ) {
				if ( parent.length > 0 )  this.$scrollbar.remove();
			}
			else {
				if ( parent.length <= 0 ) {
					this.$el.append( this.$scrollbar );
				}
				this.$scrollbar.css( "top", scrollPosition );
			}
		}
    },
    
	updateVelocity: function( yDelta ) {
		this.velocity.distance = yDelta;
		var time = new Date().getTime();
		this.velocity.timeDelta = time - this.velocity.lastTime;
		this.velocity.lastTime = time;
		//$.fn.log("log","LIST updateVelocity time delta: " + this.velocity.timeDelta);

		if ( this.velocity.timeDelta > 1000 ) {
			this.velocity.distance = 0;
		}
	},

	scrollWithInertia: function() {
		var friction = 0.96,
			animationInterval = 25,
			maxPosition, yDelta, self,
        
    	    //detect bounds and "snap back" if needed
        	startPosition = Math.ceil(this.yPosition/this.itemHeight);
    
        if ( startPosition <= 0 && this.yPosition <= 0 || (this.dataProvider.length * this.itemHeight) < this.$el.height() ) {
             this.snapToTop();
             return;
        }
        
        maxPosition = (this.dataProvider.length*this.itemHeight)-(this.$el.height());
        if ( this.yPosition > maxPosition ) {
             this.snapToBottom();
             return;
        }
        
        //end "snap back"
        
        
        yDelta = this.velocity.distance * (friction*(Math.max(1000 - this.velocity.timeDelta, 0)/1000));
        this.yPosition += yDelta;
        this.updateVelocity( yDelta );
        this.updateLayout();
        
        self= this;
        this.stopAnimation();
        if ( Math.abs(yDelta) >= 1 ) this.animationTimeout = setTimeout( function() { self.scrollWithInertia(); }, animationInterval );    
        else this.cleanupListItems();
    },

	snapToTop: function() {
		var animationInterval = 25,
			self = this,
			snapRatio = 1.5,
			targetPosition;
        this.stopAnimation();
        targetPosition = 0;
        
        if ( this.yPosition !== 0 ) {
            this.yPosition += (targetPosition-this.yPosition)/snapRatio;
            this.yPosition = Math.round(this.yPosition);
            this.updateLayout();
            this.animationTimeout = setTimeout( function() { self.snapToTop(); }, animationInterval );    
        }
        else {
            this.updateLayout();
            this.cleanupListItems();
        }
    },
    
	snapToBottom: function() {
		var animationInterval = 25,
			self = this,
			snapRatio = 1.5,
			maxPosition;
        this.stopAnimation();
        
		maxPosition = (this.dataProvider.length*this.itemHeight) - (this.$el.height());
        if ( this.yPosition > maxPosition ) {
            
            this.yPosition += (maxPosition - this.yPosition)/snapRatio;
            
            this.updateLayout();
            this.animationTimeout = setTimeout( function() { self.snapToBottom(); }, animationInterval );    
        }
        else this.cleanupListItems();
    },
    
    stopAnimation: function() {
        clearTimeout( this.animationTimeout );
    },
    
    setItemPosition: function( item, x, y ) {

        if (this.useTransform && (this.vendorPrefix != 'O')) {		// Opera has but can NOT!
            var cssString = "translate3d("+x+"px, "+y+"px, 0px)";
         //   item.css( "-"+this.vendorPrefix+"-transform", cssString );
            item.css( this.transformCSS, cssString );
          	//$.fn.log("log","LIST useTransform: '" + this.transformCSS + "' value: '" + cssString + "'");
		} 
        else {
			//$.fn.log("log","LIST use CSS on " + item.attr("id") + "\n\tx, y: " + x + ", " + y);
			item.css( "left", x+"px" );
			item.css( "top", y+"px" );
        }
    },
    
    getItemAtIndex: function( i ) {
        var item;
        if (this.dataProvider === this.listItems) {
            item = $(this.listItems[i]);
        }
        else if ( i !== undefined ){
            var iString = i.toString(),
            	data, label;
            if ( this.listItems[ iString ] === null || this.listItems[ iString ] === undefined ) {
                item = $("<li/>");
                this.listItems[ iString ] = item;
            }
            else item = this.listItems[ i ];

            if ( i >= 0 && i < this.dataProvider.length ){
                data = this.dataProvider[i];
                label =  this.labelFunction ? this.labelFunction( data ) : data.toString();
                item.text( label );
            }
        }
        if ( item !== null && item !== undefined ) {
            item.attr( "list-index", i );
        }
        return item;
    },
    
    setDataProvider: function( dataProvider ) {
        this.clearSelectedIndex();
        this.dataProvider = dataProvider;
        
        this.$ul.find("li").each( function(i) {
            $(this).remove();
        });
        
        this.yPosition = 0;
        this.updateLayout();
    },
    
    setLabelFunction: function( labelFunction ) {
        this.labelFunction = labelFunction;
        this.updateLayout();
    },
    
    getSelectedIndex: function() {
        return parseInt(this.selectedIndex, 10);
    },
    
    setSelectedIndex: function( index ) {
        var item = this.getItemAtIndex( this.selectedIndex );
        
        if ( item !== undefined ) item.removeClass( "listSelected" );
        
        this.selectedIndex = index;
        this.getItemAtIndex( index ).addClass( "listSelected" );
    },
    
    clearSelectedIndex: function() {
        var item = this.getItemAtIndex( this.selectedIndex );
        
        if ( item !== undefined ) item.removeClass( "listSelected" );
        this.selectedIndex = -1;
    },
    
    scrollbarTouchStart: function( event ) {
        this.cleanupEventHandlers();
        this.scrollbarInputCoordinates = this.getInputCoordinates( event );
        
        $(document).bind( this.TOUCH_MOVE, this.scrollbarMoveHandler );
        $(document).bind( this.TOUCH_END, this.scrollbarEndHandler );
       
        event.preventDefault();
        return false;
    },
    
	scrollbarTouchMove: function( event ) {
		var newCoordinates = this.getInputCoordinates( event ),
			yDelta = this.scrollbarInputCoordinates.y - newCoordinates.y,
			yPosition = parseInt( this.$scrollbar.css( "top" ), 10 ),
			newYPosition;
        yPosition -= yDelta;
        
        yPosition = Math.max( yPosition, this.SCROLLBAR_BORDER );
        yPosition = Math.min( yPosition, this.$el.height()-this.SCROLLBAR_BORDER-this.$scrollbar.height() );
        
        this.$scrollbar.css( "top", yPosition );
        this.scrollbarInputCoordinates = newCoordinates;
        
        newYPosition = ((yPosition-this.SCROLLBAR_BORDER)/
                            (this.$el.height()-(2*this.SCROLLBAR_BORDER)-this.$scrollbar.height())
                           )*(this.itemHeight*this.dataProvider.length-1);
        newYPosition = Math.max( 0, newYPosition );
        newYPosition = Math.min( newYPosition, (this.itemHeight*(this.dataProvider.length))-(this.$el.height()-(2*this.SCROLLBAR_BORDER)-this.$scrollbar.height()) );
        
        this.yPosition = newYPosition;
        this.updateLayout(true);
        
        event.preventDefault();
        return false;
    },
    
    scrollbarTouchEnd: function( event ) {
        this.cleanupEventHandlers();
        this.cleanupListItems();
        event.preventDefault();
        return false;
    },

    scrollToPage: function ( itemnum ) {
        this.yPosition = (itemnum*this.itemHeight);
        //$(window).log("log","scrollto page: " + itemnum + "\nthis.itemHeight: " + this.itemHeight + "\nthis.yPosition: " + this.yPosition);
        //alert("scrollto page: " + itemnum + "\nthis.itemHeight: " + this.itemHeight + "\nthis.yPosition: " + this.yPosition);

        this.updateLayout();
        return false;
    }

  };



	/* LIST PLUGIN DEFINITION
	 * ====================== */

	$.fn.list = function ( option, params ) {
		return this.each(function () {
			var $this = $(this), data = $this.data('list');
			if (!data) { $this.data('list', (data = new List(this))); }
			if (typeof option === 'string') { data[option](params); }
		});
	};

	$.fn.list.Constructor = List;

})( jQuery );


/*
 jQuery Simple Slider

 Copyright (c) 2012 James Smith (http://loopj.com)

 Licensed under the MIT license (http://mit-license.org/)
*/

var __slice = [].slice,
	__indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

(function($, window) {
	var SimpleSlider;
	SimpleSlider = (function() {

	function SimpleSlider(input, options) {
			//alert("SimpleSlider on input element ID: " + input.attr("id"));
			var body = $("body"),
				ratio,
				_this = this;

		  this.input = input;
		  this.defaultOptions = {
			animate: true,
			snapMid: false,
			classPrefix: null,
			classSuffix: null,
			theme: null
		  };
		  this.settings = $.extend({}, this.defaultOptions, options);
		  if (this.settings.theme) {
			this.settings.classSuffix = "-" + this.settings.theme;
		  }
		/* removed Ai
		  this.input.hide();
		  this.slider = $("<div>").addClass("slider" + (this.settings.classSuffix || "")).css({
			position: "relative",
			userSelect: "none",
			boxSizing: "border-box"
		  }).insertBefore(this.input);
		*********/
		// added Ai
			this.slider = this.input;
			this.slider.css({
							position: "relative",
							userSelect: "none",
							boxSizing: "border-box"
						  });
		// END added Ai
		/* removed Ai
		  if (this.input.attr("id")) {
			this.slider.attr("id", this.input.attr("id") + "-slider");
		  }
		*********/
		this.track = $("<div>").addClass("track").css({
				position: "absolute",
				top: "50%",
				width: "100%",
				userSelect: "none",
				cursor: "pointer"
			}).appendTo(this.slider);
		this.dragger = $("<div>").addClass("dragger").css({
			position: "absolute",
			top: "50%",
			userSelect: "none",
			cursor: "pointer"
			}).appendTo(this.slider);
		this.slider.css({
			minHeight: this.dragger.outerHeight(),
			marginLeft: this.dragger.outerWidth() / 2,
			marginRight: this.dragger.outerWidth() / 2
			});
		this.track.css({
			marginTop: this.track.outerHeight() / -2
			});
		this.dragger.css({
			marginTop: this.dragger.outerWidth() / -2,
			marginLeft: this.dragger.outerWidth() / -2
			});

		this.track.on(_sb_s.pointerEvents.start,function(e) {
								var pXY = _sb_fn.get_pointerXY(e);
								_this.domDrag(pXY.x, pXY.y, true);
								_this.dragging = true;
								return false;
								});

		this.dragger.on(_sb_s.pointerEvents.start, function(e) {
								var pXY = _sb_fn.get_pointerXY(e);
								_this.domDrag(pXY.x, pXY.y);
								_this.dragging = true;
								_this.dragger.addClass("dragging");
								return false;
								});

		body.on(_sb_s.pointerEvents.move,function(e) {
								if (_this.dragging) {
									var pXY = _sb_fn.get_pointerXY(e);
									_this.domDrag(pXY.x, pXY.y);
									body.css({ cursor: "pointer" });
									return false;
								}
								return(true);
							})
			.on(_sb_s.pointerEvents.end,function(e) {
								if (_this.dragging) {
									_this.dragging = false;
									_this.dragger.removeClass("dragging");
									return body.css({ cursor: "auto" });
								}
								return(true);
							});

		this.pagePos = 0;
		if (this.input.val() === "") {
			this.value = this.getRange().min;
			this.input.val(this.value);
		}
		else this.value = this.nearestValidValue(this.input.val());
		this.setSliderPositionFromValue(this.value);
		ratio = this.valueToRatio(this.value);
		this.input.trigger("slider:ready", {
					value: this.value,
					ratio: ratio,
					position: ratio * this.slider.outerWidth(),
					el: this.slider
					});
	}

	SimpleSlider.prototype.setRatio = function(ratio) {
	var value;
		ratio = Math.min(1, ratio);
		ratio = Math.max(0, ratio);
		value = this.ratioToValue(ratio);
		this.setSliderPositionFromValue(value);
		return this.valueChanged(value, ratio, "setRatio");
	};

	SimpleSlider.prototype.setValue = function(value) {
		var ratio;
		value = this.nearestValidValue(value);
		ratio = this.valueToRatio(value);
		this.setSliderPositionFromValue(value);
		return this.valueChanged(value, ratio, "setValue");
	};

	SimpleSlider.prototype.domDrag = function(pageX, pageY, animate) {
		var pagePos, ratio, value;
		if (animate == null) animate = false;
		pagePos = pageX - this.slider.offset().left;
		pagePos = Math.min(this.slider.outerWidth(), pagePos);
		pagePos = Math.max(0, pagePos);
		if (this.pagePos !== pagePos) {
			this.pagePos = pagePos;
			ratio = pagePos / this.slider.outerWidth();
			value = this.ratioToValue(ratio);
			this.valueChanged(value, ratio, "domDrag");
			if (this.settings.snap) return this.setSliderPositionFromValue(value, animate);
			else return this.setSliderPosition(pagePos, animate);
		}
	};

	SimpleSlider.prototype.setSliderPosition = function(position, animate) {
		if (animate == null) animate = false;
		if (animate && this.settings.animate) {
						return this.dragger.animate({
						left: position
						}, 200);
		}
		else return this.dragger.css({ left: position });
	};

    SimpleSlider.prototype.setSliderPositionFromValue = function(value, animate) {
      var ratio;
      if (animate == null) {
        animate = false;
      }
      ratio = this.valueToRatio(value);
      return this.setSliderPosition(ratio * this.slider.outerWidth(), animate);
    };

	SimpleSlider.prototype.getRange = function() {
		if (this.settings.allowedValues) {
			return {
					min: Math.min.apply(Math, this.settings.allowedValues),
					max: Math.max.apply(Math, this.settings.allowedValues)
					};
		}
		else if (this.settings.range) {
			return {
					min: parseFloat(this.settings.range[0]),
					max: parseFloat(this.settings.range[1])
					};
		}
		else {
			return {
					min: 0,
					max: 1
					};
		}
	};

	SimpleSlider.prototype.nearestValidValue = function(rawValue) {
		var closest, maxSteps, range, steps;
			range = this.getRange();
		rawValue = Math.min(range.max, rawValue);
		rawValue = Math.max(range.min, rawValue);
		if (this.settings.allowedValues) {
			closest = null;
			$.each(this.settings.allowedValues, function() {
															if (closest === null || Math.abs(this - rawValue) < Math.abs(closest - rawValue)) {
																return closest = this;
															}
															});
			return closest;
		}
		else if (this.settings.step) {
					maxSteps = (range.max - range.min) / this.settings.step;
					steps = Math.floor((rawValue - range.min) / this.settings.step);
					if ((rawValue - range.min) % this.settings.step > this.settings.step / 2 && steps < maxSteps) {
						steps += 1;
					}
					return steps * this.settings.step + range.min;
				}
				else return rawValue;
	};

	SimpleSlider.prototype.valueToRatio = function(value) {
		var allowedVal, closest, closestIdx, idx, range, _i, _len, _ref;
		if (this.settings.equalSteps) {
			_ref = this.settings.allowedValues;
			for (idx = _i = 0, _len = _ref.length; _i < _len; idx = ++_i) {
				allowedVal = _ref[idx];
				if (!(typeof closest !== "undefined" && closest !== null) || Math.abs(allowedVal - value) < Math.abs(closest - value)) {
					closest = allowedVal;
					closestIdx = idx;
				}
			}
			if (this.settings.snapMid) return (closestIdx + 0.5) / this.settings.allowedValues.length;
			else return closestIdx / (this.settings.allowedValues.length - 1);
		}
		else {
			range = this.getRange();
			return (value - range.min) / (range.max - range.min);
		}
	};

	SimpleSlider.prototype.ratioToValue = function(ratio) {
		var idx, range, rawValue, step, steps;
		if (this.settings.equalSteps) {
			steps = this.settings.allowedValues.length;
			step = Math.round(ratio * steps - 0.5);
			idx = Math.min(step, this.settings.allowedValues.length - 1);
			return this.settings.allowedValues[idx];
		}
		else {
			range = this.getRange();
			rawValue = ratio * (range.max - range.min) + range.min;
			return this.nearestValidValue(rawValue);
		}
	};

	SimpleSlider.prototype.valueChanged = function(value, ratio, trigger) {
		var eventData;
		if (value.toString() === this.value.toString()) return;
		this.value = value;
		eventData = {
					value: value,
					ratio: ratio,
					position: ratio * this.slider.outerWidth(),
					trigger: trigger,
					el: this.slider
					};
		return this.input.val(value).trigger($.Event("change", eventData)).trigger("slider:changed", eventData);
	};

	return SimpleSlider;

	})();
	$.extend($.fn, {
		simpleSlider: function() {
			var params, publicMethods, settingsOrMethod;
			settingsOrMethod = arguments[0], params = 2 <= arguments.length ? __slice.call(arguments, 1) : [];
			publicMethods = ["setRatio", "setValue"];
			return $(this).each(function() {
											var obj, settings;
											if (settingsOrMethod && __indexOf.call(publicMethods, settingsOrMethod) >= 0) {
												obj = $(this).data("slider-object");
												return obj[settingsOrMethod].apply(obj, params);
											}
											else {
												settings = settingsOrMethod;
												return $(this).data("slider-object", new SimpleSlider($(this), settings));
											}
										});
		},

	initSimpleSlider: function(element) {
		//alert("initSimpleSlider: ");
		var $el, allowedValues, settings, x;
		$el = element;
		settings = {};
		allowedValues = $el.data("slider-values");
		if (allowedValues) {
			settings.allowedValues = (function() {
										var _i, _len, _ref, _results;
										_ref = allowedValues.split(",");
										_results = [];
										for (_i = 0, _len = _ref.length; _i < _len; _i++) {
											x = _ref[_i];
											_results.push(parseFloat(x));
										}
										return _results;
										})();
		}
		if ($el.data("slider-range")) settings.range = $el.data("slider-range").split(",");
		if ($el.data("slider-step")) settings.step = $el.data("slider-step");
		settings.snap = $el.data("slider-snap");
		settings.equalSteps = $el.data("slider-equal-steps");
		if ($el.data("slider-theme")) settings.theme = $el.data("slider-theme");
		return $el.simpleSlider(settings);
		}
	});

})(jQuery, this);



/* ========================
 * turn.js 3rd release
 * www.turnjs.com
 *
 * Copyright (C) 2012, Emmanuel Garcia.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Any redistribution, use, or modification is done solely for personal 
 * benefit and not for any commercial purpose or for monetary gain.
 * 
 **/

var _g_turnjs_pageIsFolding = false;	// globale variable to retrieve if page is turning

(function($) {

'use strict';

	var detectTouchDevice = function () {
		//alert(navigator.userAgent);
		var touchable = false,
			canCreateTouchEvent = null;
		//alert("navigator.msMaxTouchPoints: " + navigator.msMaxTouchPoints);
		do {
			if ((('ontouchstart' in window) === true) 
				|| (navigator.maxTouchPoints && (navigator.maxTouchPoints > 0))
				|| (navigator.msMaxTouchPoints && (navigator.msMaxTouchPoints > 0))
				) { touchable = true; break; }
			try {
				canCreateTouchEvent = document.createEvent("TouchEvent");	// Google Chrome can create this even if it is not a touch device!!!
				if (canCreateTouchEvent != null) canCreateTouchEvent = true;
			} catch(ex) {
				//alert("CANNOT canCreateTouchEvent")
				canCreateTouchEvent = false;
				touchable = false;
			}
			if (canCreateTouchEvent && (('ontouchstart' in window) === true)) { touchable = true; break; }
			//if (canCreateTouchEvent && ((window.DocumentTouch && document instanceof DocumentTouch) === true)) { touchable = true; break; }
		} while(false);
		//alert("touchable: " + touchable);
		return touchable;
	},
	has3d,

	vendor = '',

	PI = Math.PI,

	A90 = PI/2,
	isTouch = detectTouchDevice(),

	events = _sb_s.pointerEvents,

	// Contansts used for each corner
	// tl * tr
	// *     *
	// bl * br

	corners = {
		backward: ['bl', 'tl'],
		forward: ['br', 'tr'],
		all: ['tl', 'bl', 'tr', 'br']
	},
	
	// no corner animation at all
	cornersdisabled = false,

	displays = ['single', 'double'],

	// call backs
	cb_turn_end = null,	// called when a page turn is complete
	cb_before_removepage = null,	// called before a page is removed from the DOM
	cb_after_addpage = null,	// called before a page is removed from the DOM

	// Default options

	turnOptions = {

		// First page

		page: 1,
		
		// Enables gradients

		gradients: true,

		// Duration of transition in milliseconds

		duration: 500,

		// Enables hardware acceleration

		acceleration: true,

		// Display

		display: 'double',
		disable: false,

		// Events

		when: null
	},

	flipOptions = {

		// Back page
		
		folding: null,

		// Corners
		// backward: Activates both tl and bl corners
		// forward: Activates both tr and br corners
		// all: Activates all the corners

		corners: 'forward',

		// how far to pull an edge before page turn occurs
		// 1 = full page width
		// 2 = half page width
		// 3 = third of page width
		flipThreshold: 3,

		// Size of the active zone of each corner

		cornerSize: 60,	// Ai orig was 100,

		// Enables gradients

		gradients: true,

		// Duration of transition in milliseconds

		duration: 600,

		// Enables hardware acceleration

		acceleration: true
	},

	// Number of pages in the DOM, minimum value: 6

	pagesInDOM = 6,
	
	pagePosition = {0: {top: 0, left: 0, right: 'auto', bottom: 'auto'},
					1: {top: 0, right: 0, left: 'auto', bottom: 'auto'}},

	// Gets basic attributes for a layer

	divAtt = function(top, left, zIndex, overf) {
		return {'css': {
					position: 'absolute',
					top: top,
					left: left,
					'overflow': overf || 'hidden',
					'z-index': zIndex || 'auto'
					}
			};
	},

	// Gets a 2D point from a bezier curve of four points

	bezier = function(p1, p2, p3, p4, t) {
		var mum1 = 1 - t,
			mum13 = mum1 * mum1 * mum1,
			mu3 = t * t * t;

		return point2D(Math.round(mum13*p1.x + 3*t*mum1*mum1*p2.x + 3*t*t*mum1*p3.x + mu3*p4.x),
						Math.round(mum13*p1.y + 3*t*mum1*mum1*p2.y + 3*t*t*mum1*p3.y + mu3*p4.y));
	},
	
	// Converts an angle from degrees to radians

	rad = function(degrees) {
		return degrees/180*PI;
	},

	// Converts an angle from radians to degrees

	deg = function(radians) {
		return radians/PI*180;
	},

	// Gets a 2D point

	point2D = function(x, y) {
		return {x: x, y: y};
	},

	// Returns the traslate value

	translate = function(x, y, use3d) {
		// 20130412 Ai: added 'rotate(0.0001deg) ' to make transition smooth and faster on firefox
		return (has3d && use3d) ? ' translate3d(' + x + 'px,' + y + 'px, 0px) rotate(0.0001deg) ' : ' translate(' + x + 'px, ' + y + 'px) ';
	},

	// Returns the rotation value

	rotate = function(degrees) {
		return ' rotate(' + degrees + 'deg) ';
	},

	// Checks if a property belongs to an object

	has = function(property, object) {
		return Object.prototype.hasOwnProperty.call(object, property);
	},

	// Gets the CSS3 vendor prefix
	getPrefix = function () {
		var tmp = document.createElement("div"),
			vendorprefix = "",
			prefixes = ['webkit','Moz','ms','Khtml','O'],
			i;
				/*	// DEBUG only
     			for (var prop in tmp.style) {
   							//$.fn.log("log","TURNJS prop: " + prop);
	   				if ( (prop.toLowerCase().indexOf("transform") > -1) 
     						|| (prop.toLowerCase().indexOf("transition") > -1)
     						|| (prop.toLowerCase().indexOf("gradient") > -1)
     					 )
 							$.fn.log("log","TURNJS prop: " + prop);
 				}
 				*/
 		/*
 		if ((typeof tmp.style['transform'] != 'undefined')
 			|| (typeof tmp.style['transition'] != 'undefined')
 			) {
 		}
 		else {
 		*/
			for (i in prefixes) {
				if (typeof tmp.style[prefixes[i] + 'Transform'] != 'undefined') {
					//	$.fn.log("log","TURNJS vendor: " + prefixes[i]);
					vendorprefix = ('-'+prefixes[i].toLowerCase()+'-');
					break;
				}
			}
		//}
		//alert("vendorprefix: '" + vendorprefix + "'");
		return vendorprefix;
	},

	// Adds gradients
	gradient = function(obj, p0, p1, colors, numColors) {
	
		var j, cols = [];

		if (vendor=='-webkit-') {
		
			for (j = 0; j<numColors; j++)
					cols.push('color-stop('+colors[j][0]+', '+colors[j][1]+')');
			obj.css({'background-image': '-webkit-gradient(linear, '+p0.x+'% '+p0.y+'%, '+p1.x+'% '+p1.y+'%, '+ cols.join(',') +' )'});

		} else {

			// This procedure makes the gradients for non-webkit browsers
			// It will be reduced to one unique way for gradients in next versions
			
			p0 = {x:p0.x/100 * obj.width(), y:p0.y/100 * obj.height()};
			p1 = {x:p1.x/100 * obj.width(), y:p1.y/100 * obj.height()};

			var graddef = {},
				perc,
				dx = p1.x-p0.x,
				dy = p1.y-p0.y,
				angle = Math.atan2(dy, dx),
				angle2 = angle - Math.PI/2,
					//angle = vendor == '' ? (Math.PI/2 - Math.atan2(dy, dx)) : Math.atan2(dy, dx),
					//angle2 = Math.PI/2 * angle,
				diagonal = Math.abs(obj.width()*Math.sin(angle2)) + Math.abs(obj.height()*Math.cos(angle2)),
				gradientDiagonal = Math.sqrt(dy*dy + dx*dx),
				corner = point2D((p1.x<p0.x) ? obj.width() : 0, (p1.y<p0.y) ? obj.height() : 0),
				slope = Math.tan(angle),
				//inverse = -1/slope, // slope can be 0 !!!
				inverse = (slope!=0 ? -1/slope : 1),
				x = (inverse*corner.x - corner.y - slope*p0.x + p0.y) / (inverse-slope),
				c = {x: x, y: inverse*x - inverse*corner.x + corner.y},
				segA = (Math.sqrt( Math.pow(c.x-p0.x,2) + Math.pow(c.y-p0.y,2)));

				//$.fn.log("log","slope: " + slope +"\n\tangle: " + angle);
				for (j = 0; j<numColors; j++) {
					perc = (( segA + gradientDiagonal*colors[j][0] )*100/diagonal);
					if (perc < 1.0e-8) perc = 0;	// like 2.1879299224854843e-14 (very small numbers convert to 0
					//$.fn.log("log","perc: " + perc.toString(10));
					cols.push(' '+colors[j][1]+' '+perc+'%');
				}

				graddef = {'background-image': vendor+'linear-gradient(' + (-angle) + 'rad,' + cols.join(',') + ')'};
				//$.fn.log("log","graddef:\n" + _sb_fn.list_object(graddef));
				obj.css(graddef);
		}
	},

turnMethods = {

	// Singleton constructor
	// $('#selector').turn([options]);

	init: function(opts) {
		//alert("turnMethods init");
		// Define constants
		if (has3d===undefined) {
			has3d = 'WebKitCSSMatrix' in window || 'MozPerspective' in document.body.style;
			vendor = getPrefix();
		}
   		//$.fn.log("log","TURNJS has3d: " + has3d);

		var i,
			data = this.data(),
			ch = this.children();
		//alert(JSON.stringify(this));

		opts = $.extend({width: this.width(), height: this.height()}, turnOptions, opts);
		data.opts = opts;
		data.pageObjs = {};
		data.pages = {};
		data.pageWrap = {};
		data.pagePlace = {};
		data.pageMv = [];
		data.totalPages = opts.pages || 0;
		//_sb_s.DEBUGmode = 2;
   		//$.fn.log("log","TURNJS init - totalPages: " + data.totalPages);

		if (opts.when)
			for (i in opts.when)
				if (has(i, opts.when))
					this.bind(i, opts.when[i]);


		this.css({position: 'relative', width: opts.width, height: opts.height});

		this.turn('display', opts.display);

		if (has3d && !isTouch && opts.acceleration)
			this.transform(translate(0, 0, true));
	
		for (i = 0; i<ch.length; i++)
			this.turn('addPage', ch[i], i+1);
		//alert("init data:\n" + JSON.stringify(data));
	
		if (opts.page) this.turn('page', opts.page);

        // allow setting active corners as an option
        if (typeof(opts.corners) != 'undefined') corners = $.extend({}, corners, opts.corners);

		// Event listeners
		$(this).bind(events.start, function(e) {
			//$.fn.log("log","events.start: " + events.start);
			for (var page in data.pages)
				if (has(page, data.pages) && flipMethods._eventStart.call(data.pages[page], e)===false)	null;// LET OTHERS HANDLE TOO   return false;
		});
		
		$(document).bind(events.move, function(e) {
			//$.fn.log("log","events.move");
			for (var page in data.pages)
				if (has(page, data.pages)) {
					flipMethods._eventMove.call(data.pages[page], e);
				}
			}).
			bind(events.end, function(e) {
				//$.fn.log("log","events.end on: " + e.target);
				for (var page in data.pages)
					if (has(page, data.pages))
						flipMethods._eventEnd.call(data.pages[page], e);

			});
		
//********* added Ai: need it for iPad
		if (isTouch) {
			$(this).bind(events.move, function(e) {
				//$.fn.log("log","events.move");
				for (var page in data.pages)
					if (has(page, data.pages)) {
						flipMethods._eventMove.call(data.pages[page], e);
					}
				}).
				bind(events.end, function(e) {
					//$.fn.log("log","events.end");
					for (var page in data.pages)
						if (has(page, data.pages))
							flipMethods._eventEnd.call(data.pages[page], e);

				});
		}
//*********

		data.done = true;
		//alert("turnMethods init DONE\ndata.totalPages: " + data.totalPages);

		return this;
	},

	// Adds a page from external data

	addPage: function(element, page) {

		var incPages = false,
			data = this.data(),
			lastPage = data.totalPages+1;

		if (page) {
			if (page==lastPage) {
				page = lastPage;
				incPages = true;
			} else if (page>lastPage)
				throw new Error ('It is impossible to add the page "'+page+'", the maximum value is: "'+lastPage+'"');

		} else {
			page = lastPage;
			incPages = true;
		}

		if (page>=1 && page<=lastPage) {

			// Stop animations
			if (data.done) this.turn('stop');

			// Move pages if it's necessary
			if (page in data.pageObjs)
				turnMethods._movePages.call(this, page, 1);

			// Update number of pages
			if (incPages)
				data.totalPages = lastPage;

			// Add element
			data.pageObjs[page] = $(element).addClass('turn-page p' + page);

			// Add page
			turnMethods._addPage.call(this, page);

			// Update view
			if (data.done)
				this.turn('update');

			turnMethods._removeFromDOM.call(this);
		}

		return this;
	},

	// Adds a page from internal data

	_addPage: function(page) {
		
		var data = this.data(),
			element = data.pageObjs[page];

		if (element)
			if (turnMethods._necessPage.call(this, page)) {
				
				if (!data.pageWrap[page]) {

					var pageWidth = (data.display=='double') ? this.width()/2 : this.width(),
						pageHeight = this.height(),
						firstDOMpage, lastDOMpage,
						firstDOMpageNum = undefined, lastDOMpageNum = undefined;

					element.css({width:pageWidth, height:pageHeight});

					// Place
					data.pagePlace[page] = page;

					// Wrapper
					data.pageWrap[page] = $('<div/>', {'class': 'turn-page-wrapper',
														page: page,
														css: {position: 'absolute',
															overflow: 'visible',
															width: pageWidth,
															height: pageHeight,
															msTouchAction: 'none',
															touchAction: 'none'
															}
														}).
													css(pagePosition[(data.display=='double') ? page%2 : 0]);

					// determine if we have to prepend or to append to pages in DOM
					// 'this' is our pages list with ID 'sb_pagelist'
					firstDOMpage = $(this).children("div[page]:first");
					firstDOMpageNum = firstDOMpage.attr("page");
					lastDOMpage = $(this).children("div[page]:last");
					lastDOMpageNum = lastDOMpage.attr("page");
					if ((typeof(firstDOMpageNum) == 'undefined') || (typeof(lastDOMpageNum) == 'undefined')	// DOM is empty, we append
						|| (page > parseInt(lastDOMpageNum,10))
						) {
						// Append to this
						this.append(data.pageWrap[page]);
					}
					else {
						if (page > parseInt(firstDOMpageNum,10)) data.pageWrap[page].insertAfter(firstDOMpage);
						else data.pageWrap[page].insertBefore(firstDOMpage);
					}

					// Move data.pageObjs[page] (element) to wrapper
					data.pageWrap[page].prepend(data.pageObjs[page]);

					//_sb_s.DEBUGmode = 2; $.fn.log("log","_addPage " + page + " ID: " + data.pageObjs[page].children(":first").attr("id") + "\n1stPage: " + firstDOMpageNum + " lastPage: " + lastDOMpageNum + "\npage to add: " + page + "  " +typeof(page) );
					if (cb_after_addpage != null) cb_after_addpage("addpage",data.pageObjs[page].children(":first").attr("id"));
				}

				// If the page is in the current view, create the flip effect
				if (!page || turnMethods._setPageLoc.call(this, page)==1) {
					turnMethods._makeFlip.call(this, page);
				}
				
			} else {

				// Place
				data.pagePlace[page] = 0;

				// Remove element from the DOM
				if (data.pageObjs[page])
					data.pageObjs[page].remove();

			}

	},

	// Checks if a page is in memory
	
	hasPage: function(page) {

		return page in this.data().pageObjs;
	
	},

	// Prepares the flip effect for a page

	_makeFlip: function(page) {

		var data = this.data();

		if (!data.pages[page] && data.pagePlace[page]==page) {

			var single = data.display=='single',
				even = page%2;
			
			data.pages[page] = data.pageObjs[page].
								css({width: (single) ? this.width() : this.width()/2, height: this.height()}).
								flip({page: page,
									next: (single && page === data.totalPages) ? page -1 : ((even || single) ? page+1 : page-1),
									turn: this,
									duration: data.opts.duration,
									acceleration : data.opts.acceleration,
									corners: (single) ? 'all' : ((even) ? 'forward' : 'backward'),
									backGradient: data.opts.gradients,
									frontGradient: data.opts.gradients
									}).
									flip('disable', ((typeof(data.disabled) != 'undefined') ? data.disabled : undefined)).
									bind('pressed', turnMethods._pressed).
									bind('released', turnMethods._released).
									bind('start', turnMethods._start).
									bind('end', turnMethods._end).
									bind('flip', turnMethods._flip);
		}
		return data.pages[page];
	},

	// Makes pages within a range

	_makeRange: function() {

		var page,
			data = this.data(),
			range = this.turn('range');

			for (page = range[0]; page<=range[1]; page++)
				turnMethods._addPage.call(this, page);

	},

	// Returns a range of `pagesInDOM` pages that should be in the DOM
	// Example:
	// - page of the current view, return true
	// * page is in the range, return true
	// 0 page is not in the range, return false
	//
	// 1 2-3 4-5 6-7 8-9 10-11 12-13
	//    **  **  --   **  **

	range: function(page) {
		var remainingPages, left, right,
			data = this.data();
			page = page || data.tpage || data.page;
			var view = turnMethods._view.call(this, page);

			if (page<1 || page>data.totalPages) {
				//throw new Error ('"'+page+'" is not a page for range');
				return;
			}
		
			view[1] = view[1] || view[0];
			
			if (view[0]>=1 && view[1]<=data.totalPages) {

				remainingPages = Math.floor((pagesInDOM-2)/2);

				if (data.totalPages-view[1] > view[0]) {
					left = Math.min(view[0]-1, remainingPages);
					right = 2*remainingPages-left;
				} else {
					right = Math.min(data.totalPages-view[1], remainingPages);
					left = 2*remainingPages-right;
				}

			} else {
				left = pagesInDOM-1;
				right = pagesInDOM-1;
			}

			return [Math.max(1, view[0]-left), Math.min(data.totalPages, view[1]+right)];

	},

	// Detects if a page is within the range of `pagesInDOM` from the current view

	_necessPage: function(page) {
		
		if (page===0)
			return true;

		var range = this.turn('range');

		return page>=range[0] && page<=range[1];
		
	},

	// Releases memory by removing pages from the DOM

	_removeFromDOM: function() {

		var page, data = this.data();

		for (page in data.pageWrap)
			if (has(page, data.pageWrap) && !turnMethods._necessPage.call(this, page))
				turnMethods._removePageFromDOM.call(this, page);
		

	},

	// Removes a page from DOM and its internal references

	_removePageFromDOM: function(page) {

		var data = this.data();
		// added Ai
		//$.fn.log("log","_removePageFromDOM " + page + " ID: " + data.pageObjs[page].children(":first").attr("id"));
		if (cb_before_removepage != null) cb_before_removepage("removepage",data.pageObjs[page].children(":first").attr("id"));
		// END added Ai

		if (data.pages[page]) {
			var dd = data.pages[page].data();
			if (dd.f && dd.f.fwrapper)
				dd.f.fwrapper.remove();
			data.pages[page].remove();
			delete data.pages[page];
		}

		if (data.pageObjs[page])
			data.pageObjs[page].remove();

		if (data.pageWrap[page]) {
			data.pageWrap[page].remove();
			delete data.pageWrap[page];
		}

		delete data.pagePlace[page];

	},

	// Removes a page

	removePage: function(page) {

		var data = this.data();

		if (data.pageObjs[page]) {
			// Stop animations
			this.turn('stop');

			// Remove `page`
			turnMethods._removePageFromDOM.call(this, page);
			delete data.pageObjs[page];

			// Move the pages behind `page`
			turnMethods._movePages.call(this, page, -1);

			// Resize the size of this magazine
			data.totalPages = data.totalPages-1;
			turnMethods._makeRange.call(this);

			// Check the current view
			if (data.page>data.totalPages)
				this.turn('page', data.totalPages);
		}

		return this;
	
	},

	// remove all pages
	removeAllPages: function() {

		var page,
			data = this.data();

		for (page = 1; page <= data.totalPages; page++) {
			// Remove `page`
			turnMethods._removePageFromDOM.call(this, page);
			delete data.pageObjs[page];

			// Move the pages behind `page`
			turnMethods._movePages.call(this, page, -1);

			// Resize the size of this magazine
			data.totalPages = data.totalPages-1;
			turnMethods._makeRange.call(this);

			// Check the current view
			if (data.page>data.totalPages)
				this.turn('page', data.totalPages);
		}
		data.totalPages = 0;

	},

	// Moves pages

	_movePages: function(from, change) {

		var page,
			data = this.data(),
			single = data.display=='single',
			move = function(page) {

				var next = page + change,
					odd = next%2;

				if (data.pageObjs[page])
					data.pageObjs[next] = data.pageObjs[page].removeClass('page' + page).addClass('page' + next);

				if (data.pagePlace[page] && data.pageWrap[page]) {
					data.pagePlace[next] = next;
					data.pageWrap[next] = data.pageWrap[page].css(pagePosition[(single) ? 0 : odd]).attr('page', next);
					
					if (data.pages[page])
						data.pages[next] = data.pages[page].flip('options', {
							page: next,
							next: (single || odd) ? next+1 : next-1,
							corners: (single) ? 'all' : ((odd) ? 'forward' : 'backward')
						});

					if (change) {
						delete data.pages[page];
						delete data.pagePlace[page];
						delete data.pageObjs[page];
						delete data.pageWrap[page];
						delete data.pageObjs[page];
					}
			}
		};

		if (change>0)
			for (page=data.totalPages; page>=from; page--) move(page);
		else
			for (page=from; page<=data.totalPages; page++) move(page);

	},

	// Sets or Gets the display mode

	display: function(display) {

		var data = this.data(),
			currentDisplay = data.display;

		if (display) {

			if ($.inArray(display, displays)==-1)
				throw new Error ('"'+display + '" is not a value for display');
			
			if (display=='single') {
				if (!data.pageObjs[0]) {
					this.turn('stop').
						css({'overflow': 'hidden'});
					data.pageObjs[0] = $('<div />', {'class': 'turn-page p-temporal'}).
									css({width: this.width(), height: this.height()}).
										appendTo(this);
				}
			} else {
				if (data.pageObjs[0]) {
					this.turn('stop').
						css({'overflow': ''});
					data.pageObjs[0].remove();
					delete data.pageObjs[0];
				}
			}

			data.display = display;

			if (currentDisplay) {
				var size = this.turn('size');
				turnMethods._movePages.call(this, 1, 0);
				this.turn('size', size.width, size.height).
						turn('update');
			}

			return this;

		} else
			return currentDisplay;
	
	},

	// Detects if the pages are being animated

	animating: function() {

		return this.data().pageMv.length>0;

	},

	// Disables and enables the effect

	disable: function(bool) {

		var page,
			data = this.data(),
			view = this.turn('view');

			data.disabled = bool===undefined || bool===true;
	//alert("data.disabled: " + data.disabled);
		for (page in data.pages)
			if (has(page, data.pages))
				data.pages[page].flip('disable', bool ? $.inArray(page, view) : false );

		return this;

	},

	// Gets and sets the size

	size: function(width, height) {

		if (width && height) {

			var data = this.data(), pageWidth = (data.display=='double') ? width/2 : width, page;

			this.css({width: width, height: height});

			if (data.pageObjs[0])
				data.pageObjs[0].css({width: pageWidth, height: height});
			
			for (page in data.pageWrap) {
				if (!has(page, data.pageWrap)) continue;
				data.pageObjs[page].css({width: pageWidth, height: height});
				data.pageWrap[page].css({width: pageWidth, height: height});
				if (data.pages[page])
					data.pages[page].css({width: pageWidth, height: height});
			}

			this.turn('resize');

			return this;

		} else {
			
			return {width: this.width(), height: this.height()};

		}
	},

	// Resizes each page

	resize: function() {

		var page, data = this.data();

		if (data.pages[0]) {
			data.pageWrap[0].css({left: -this.width()});
			data.pages[0].flip('resize', true);
		}

		for (page = 1; page <= data.totalPages; page++)
			if (data.pages[page])
				data.pages[page].flip('resize', true);


	},

	// Removes an animation from the cache

	_removeMv: function(page) {

		var i, data = this.data();
			
		for (i=0; i<data.pageMv.length; i++)
			if (data.pageMv[i]==page) {
				data.pageMv.splice(i, 1);
				return true;
			}

		return false;

	},

	// Adds an animation to the cache
	
	_addMv: function(page) {

		var data = this.data();

		turnMethods._removeMv.call(this, page);
		data.pageMv.push(page);

	},

	// Gets indexes for a view

	_view: function(page) {
	
		var data = this.data();
		page = page || data.page;

		if (data.display=='double')
			return (page%2) ? [page-1, page] : [page, page+1];
		else
			return [page];

	},

	// Gets a view

	view: function(page) {

		var data = this.data(), view = turnMethods._view.call(this, page);

		return (data.display=='double') ? [(view[0]>0) ? view[0] : 0, (view[1]<=data.totalPages) ? view[1] : 0]
				: [(view[0]>0 && view[0]<=data.totalPages) ? view[0] : 0];

	},

	// Stops animations

	stop: function(ok) {

		var i, opts, data = this.data(), pages = data.pageMv;

		data.pageMv = [];

		if (data.tpage) {
			data.page = data.tpage;
			delete data['tpage'];
		}

		for (i in pages) {
			if (!has(i, pages)) continue;
			opts = data.pages[pages[i]].data().f.opts;
			flipMethods._moveFoldingPage.call(data.pages[pages[i]], null);
			data.pages[pages[i]].flip('hideFoldedPage');
			data.pagePlace[opts.next] = opts.next;
			
			if (opts.force) {
				opts.next = (opts.page%2===0) ? opts.page-1 : opts.page+1;
				delete opts['force'];
			}

		}

		this.turn('update');

		return this;
	},

	// Gets and sets the number of pages

	pages: function(pages) {

		var data = this.data();

		if (pages) {
			if (pages<data.totalPages) {

				for (var page = pages+1; page<=data.totalPages; page++)
					this.turn('removePage', page);

				if (this.turn('page')>pages)
					this.turn('page', pages);
			}

			data.totalPages = pages;

			return this;
		}
		else {
			return data.totalPages;
		}

	},

	// set flag indicating that a corner of a page is folding
	_setFolding: function(val,totalPages,caller) {
		if ((val !== true)
			|| (typeof(totalPages) == 'undefined')
			|| (totalPages == null)
			|| (totalPages < 2)
			) _g_turnjs_pageIsFolding = false;
		else {
			_g_turnjs_pageIsFolding = val;
		}

		//_sb_s.DEBUGmode = 2; $.fn.log("log","_setFolding caller:"+(caller?caller:"unknown")+"\n\t_g_turnjs_pageIsFolding: "+_g_turnjs_pageIsFolding+"\n\ttotalPages: "+totalPages);
	},

	// Sets a page without effect

	_fitPage: function(page, ok) {
		
		var data = this.data(), newView = this.turn('view', page);

		if (data.page!=page) {
			this.trigger('turning', [page, newView]);
			if ($.inArray(1, newView)!=-1) this.trigger('first');
			if ($.inArray(data.totalPages, newView)!=-1) this.trigger('last');
		}

		if (!data.pageObjs[page])
			return;

		data.tpage = page;

		this.turn('stop', ok);
		turnMethods._removeFromDOM.call(this);
		turnMethods._makeRange.call(this);
		this.trigger('turned', [page, newView]);
		turnMethods._setFolding(false,data.totalPages,"_fitPage");	//_g_turnjs_pageIsFolding = false;
		if (cb_turn_end != null) cb_turn_end("turn_end",page);


	},
	
	// Turns to a page

	_turnPage: function(page) {

		var current, next,
			data = this.data(),
			view = this.turn('view'),
			newView = this.turn('view', page);
		if (data.page!=page) {
			this.trigger('turning', [page, newView]);
			if ($.inArray(1, newView)!=-1) this.trigger('first');
			if ($.inArray(data.totalPages, newView)!=-1) this.trigger('last');
		}

		if (!data.pageObjs[page]) return;
		turnMethods._setFolding(true,data.totalPages,"_turnPage");	//_g_turnjs_pageIsFolding = true;

		data.tpage = page;

		this.turn('stop');

		turnMethods._makeRange.call(this);

		if (data.display=='single') {
			current = view[0];
			next = newView[0];
		} else if (view[1] && page>view[1]) {
			current = view[1];
			next = newView[0];
		} else if (view[0] && page<view[0]) {
			current = view[0];
			next = newView[1];
		}

		if (data.pages[current]) {

			var opts = data.pages[current].data().f.opts;
			if (!opts) {
				turnMethods._setFolding(false,data.totalPages,"_turnPage");	//_g_turnjs_pageIsFolding = false;
				return this;
			}
			data.tpage = next;
			
			if (opts.next!=next) {
				opts.next = next;
				data.pagePlace[next] = opts.page;
				opts.force = true;
			}

			if (data.display=='single')
				data.pages[current].flip('turnPage', (newView[0] > view[0]) ? 'br' : 'bl');
			else
				data.pages[current].flip('turnPage');
		}
		turnMethods._setFolding(false,data.totalPages,"_turnPage");	//_g_turnjs_pageIsFolding = false;

	},

	// Gets and sets a page

	page: function(page) {

		var thepage = null,
			data = this.data();
		if (typeof page != 'undefined') thepage = parseInt(page, 10);

		if ((thepage !== null) && (thepage>0) && thepage<=data.totalPages) {
			//alert("add page: " + page);
			if (!data.done || $.inArray(thepage, this.turn('view'))!=-1) {
				//$(window).log("log","turn init to page: " + thepage);
				turnMethods._fitPage.call(this, thepage);
			}
			else {
				turnMethods._setFolding(true,data.totalPages,"page");	//_g_turnjs_pageIsFolding = true;
				// ai added: 20140222 IMPORTANT
					if (_sb_settings.canvas_available) {		// this may trigger immediately the mouse over (floater) on a covered smaller area. 
						_sb_settings.pageIsScrolling = true;	// we have to set pageIsScrolling to true for floater doesn't shoot
						_sb_settings.funcs.clear_all_shadows(99);
					}
				//$(window).log("log","turning to page: " + thepage);
				turnMethods._turnPage.call(this, thepage);
			}
			return this;

		} 
		else {
			return data.page;
		}
	
	},

	// Turns to the next view

	next: function() {
		var data = this.data();
		return this.turn('page', turnMethods._view.call(this, data.page).pop() + 1);
	},

	// Turns to the previous view

	previous: function() {		
		var data = this.data();
		return this.turn('page', turnMethods._view.call(this, data.page).shift() - 1);
	},

	// Adds a motion to the internal list

	_addMotionPage: function() {

		var opts = $(this).data().f.opts,
			turn = opts.turn,
			dd = turn.data();

		opts.pageMv = opts.page;
		turnMethods._addMv.call(turn, opts.pageMv);
		dd.pagePlace[opts.next] = opts.page;
		turn.turn('update');

	},

	// This event is called in context of flip

	_start: function(e, opts, corner) {

		var data = opts.turn.data(),
			event = $.Event('start');

		e.stopPropagation();

		opts.turn.trigger(event, [opts, corner]);

		//_sb_s.DEBUGmode = 2; $.fn.log("log","turnMethods._start:\n\t_g_turnjs_pageIsFolding: "+_g_turnjs_pageIsFolding);
		if (event.isDefaultPrevented()) {
			e.preventDefault();
			return;
		}
		
		turnMethods._setFolding(true,data.totalPages,"_start");	//_g_turnjs_pageIsFolding = true;
		if (data.display=='single') {

			var left = corner.charAt(1)=='l';
			if ((opts.page==1 && left) || (opts.page==data.totalPages && !left))
				e.preventDefault();
			else {
				if (left) {
					opts.next = (opts.next<opts.page) ? opts.next : opts.page-1;
					opts.force = true;
				} else
					opts.next = (opts.next>opts.page) ? opts.next : opts.page+1;
			}

		}

		turnMethods._addMotionPage.call(this);
	},

	// This event is called in context of flip

	_end: function(e, turned) {
		var that = $(this),
			data = that.data().f,
			opts = data.opts,
			turn = opts.turn,
			dd = turn.data();
		turnMethods._setFolding(false,data.totalPages,"_end");	//_g_turnjs_pageIsFolding = false;

		e.stopPropagation();

		if (turned || dd.tpage) {
			if (cb_turn_end != null) cb_turn_end("turn_end",dd.tpage);
			// END added Ai
			if (dd.tpage==opts.next || dd.tpage==opts.page) {
				delete dd['tpage'];
				turnMethods._fitPage.call(turn, dd.tpage || opts.next, true);
			}

		} else {
			turnMethods._removeMv.call(turn, opts.pageMv);
			turn.turn('update');
		}
		//_sb_s.DEBUGmode = 2; $.fn.log("log","_end: _g_turnjs_pageIsFolding: "+_g_turnjs_pageIsFolding);
	},
	
	// This event is called in context of flip

	_pressed: function() {

		var page,
			that = $(this),
			data = that.data().f,
			turn = data.opts.turn,
			pages = turn.data().pages;
	
		for (page in pages)
			if (page!=data.opts.page)
				pages[page].flip('disable', true);

		return data.time = new Date().getTime();

	},

	// This event is called in context of flip

	_released: function(e, point) {
		
		var that = $(this),
			data = that.data().f,
			turnThreshold = $(this).width()/flipOptions.flipThreshold,
			foldFrom = 'l';

		e.stopPropagation();
		if (!data.corner) return;

		//$.fn.log("log","_released at\n\tX:" + point.x + "\n\tpage width: " + $(this).width()+ "\n\tcorner: " + JSON.stringify(data.corner));
		if (data.corner.corner.indexOf("r") > 0) {	// folding from a corner 'tr' or 'br'
			foldFrom = 'r';
		}
		//if ((new Date().getTime())-data.time<200 || point.x<0 || point.x>($(this).width()/2)) {
		if ( ((new Date().getTime()-data.time)<300) || (foldFrom == 'l' ? point.x>turnThreshold : point.x<($(this).width()-turnThreshold)) ) {
			turnMethods._setFolding(true,data.totalPages,"_showFoldedPage");	//set _g_turnjs_pageIsFolding = true;
			e.preventDefault();
			data.opts.turn.data().tpage = data.opts.next;
			data.opts.turn.turn('update');
			$(that).flip('turnPage');
		}

	},

	// This event is called in context of flip
	
	_flip: function() {
		var opts = $(this).data().f.opts;

		opts.turn.trigger('turn', [opts.next]);

	},

	// Calculate the z-index value for pages during the animation

	calculateZ: function(mv) {

		var i, page, nextPage, placePage, dpage,
			that = this,
			data = this.data(),
			view = this.turn('view'),
			currentPage = view[0] || view[1],
			r = {pageZ: {}, partZ: {}, pageV: {}},

			addView = function(page) {
				var view = that.turn('view', page);
				if (view[0]) r.pageV[view[0]] = true;
				if (view[1]) r.pageV[view[1]] = true;
			};
		
			for (i = 0; i<mv.length; i++) {
				page = mv[i];
				nextPage = data.pages[page].data().f.opts.next;
				placePage = data.pagePlace[page];
				addView(page);
				addView(nextPage);
				dpage = (data.pagePlace[nextPage]==nextPage) ? nextPage : page;
				r.pageZ[dpage] = data.totalPages - Math.abs(currentPage-dpage);
				r.partZ[placePage] = data.totalPages*2 + Math.abs(currentPage-dpage);
			}

		return r;
	},

	// Updates the z-index and display property of every page

	update: function() {

		var page,
			data = this.data();

		if (data.pageMv.length && data.pageMv[0]!==0) {

			// Update motion

			var apage,
				pos = this.turn('calculateZ', data.pageMv),
				view = this.turn('view', data.tpage);
		
			if (data.pagePlace[view[0]]==view[0]) apage = view[0];
			else if (data.pagePlace[view[1]]==view[1]) apage = view[1];
		
			for (page in data.pageWrap) {

				if (!has(page, data.pageWrap)) continue;

				data.pageWrap[page].css({display: (pos.pageV[page]) ? '' : 'none', 'z-index': pos.pageZ[page] || 0});

				if (data.pages[page]) {
					data.pages[page].flip('z', pos.partZ[page] || null);

					if (pos.pageV[page])
						data.pages[page].flip('resize');

					if (data.tpage)
						data.pages[page].flip('disable', true); // data.disabled || page!=apage
				}
			}
				
		} else {

			// Update static pages

			for (page in data.pageWrap) {
				if (!has(page, data.pageWrap)) continue;
					var pageLocation = turnMethods._setPageLoc.call(this, page);
					if (data.pages[page])
						data.pages[page].flip('disable', data.disabled || pageLocation!=1).flip('z', null);
			}
		}
	},

	// Sets the z-index and display property of a page
	// It depends on the current view

	_setPageLoc: function(page) {

		var data = this.data(),
			view = this.turn('view');

		if (page==view[0] || page==view[1]) {
			data.pageWrap[page].css({'z-index': data.totalPages, display: ''});
			return 1;
		} else if ((data.display=='single' && page==view[0]+1) || (data.display=='double' && page==view[0]-2 || page==view[1]+2)) {
			data.pageWrap[page].css({'z-index': data.totalPages-1, display: ''});
			return 2;
		} else {
			data.pageWrap[page].css({'z-index': 0, display: 'none'});
			return 0;
		}
	}
},

// Methods and properties for the flip page effect

flipMethods = {

	// Constructor

	init: function(opts) {
 		//$.fn.log("log","TURNJS flipMethods - gradients: " + opts.gradients);

		if (opts.gradients) {
			opts.frontGradient = true;
			opts.backGradient = true;
		}

		this.data({f: {}});
		this.flip('options', opts);

		flipMethods._addPageWrapper.call(this);

		return this;
	},

	setData: function(d) {
		
		var data = this.data();

		data.f = $.extend(data.f, d);

		return this;
	},

	options: function(opts) {
		
		var data = this.data().f;

		if (opts) {
			flipMethods.setData.call(this, {opts: $.extend({}, data.opts || flipOptions, opts) });
			return this;
		} else
			return data.opts;

	},

	z: function(z) {

		var data = this.data().f;
		if (!data) return this;
		if (!data.opts) return this;
		data.opts['z-index'] = z;
		data.fwrapper.css({'z-index': z || parseInt(data.parent.css('z-index'), 10) || 0});

		return this;
	},

	_cAllowed: function() {

		return corners[this.data().f.opts.corners] || this.data().f.opts.corners;

	},

	_cornerActivated: function(e) {
			//$.fn.log("log","turnjs _cornerActivated e: " + e + "\n\te.touches: " + e.touches + "\n\ttypeof(e.originalEvent): " + (e.originalEvent ? e.originalEvent.touches : 'undefined'));
		//alert("_sb_fn.list_object(e):\n" + _sb_fn.list_object(e.originalEvent));

		var data = this.data().f,
			pos = data.parent.offset(),
			width = this.width(),
			height = this.height(),
			pXY = _sb_fn.get_pointerXY(e),
			c = {},
			csz = data.opts.cornerSize,
			allowedCorners = flipMethods._cAllowed.call(this);

		if (pXY == null) return false;
		c = {x: Math.max(0, pXY.x-pos.left), y: Math.max(0, pXY.y-pos.top)};

		//$.fn.log("log","turnjs _cornerActivated\n\twidth x height: " + width + " x " + height + "\n\tc.x x c.y: " + c.x + " x " + c.y + "\n\tcornerSize: " + csz );

		if (c.x<=0 || c.y<=0 || c.x>=width || c.y>=height) return false;

		if (c.y<csz) c.corner = 't';
		else if (c.y>=height-csz) c.corner = 'b';
		else return false;
		
		if (c.x<=csz) c.corner+= 'l';
		else if (c.x>=width-csz) c.corner+= 'r';
		else return false;
			//$.fn.log("log","turnjs _cornerActivated\n\tcorner: " + c.corner + "\n\tallowedCorners: " + allowedCorners + "\n\tcorner: " +  $.inArray(c.corner, allowedCorners));

		return ($.inArray(c.corner, allowedCorners)==-1) ? false : c;

	},

	_c: function(corner, opts) {

		opts = opts || 0;
		return ({tl: point2D(opts, opts),
				tr: point2D(this.width()-opts, opts),
				bl: point2D(opts, this.height()-opts),
				br: point2D(this.width()-opts, this.height()-opts)})[corner];

	},

	_c2: function(corner) {

		return {tl: point2D(this.width()*2, 0),
				tr: point2D(-this.width(), 0),
				bl: point2D(this.width()*2, this.height()),
				br: point2D(-this.width(), this.height())}[corner];

	},

	_foldingPage: function() {

		var opts = this.data().f.opts;
		
		if (opts.folding) return opts.folding;
		else if(opts.turn) {
			var data = opts.turn.data();
			if (data.display == 'single')
				return (data.pageObjs[opts.next]) ? data.pageObjs[0] : null;
			else
				return data.pageObjs[opts.next];
		}

	},

	_backGradient: function() {

		var data =	this.data().f,
			turn = data.opts.turn,
			gradient = data.opts.backGradient &&
						(!turn || turn.data().display=='single' || (data.opts.page!=2 && data.opts.page!=turn.data().totalPages-1) );

		if (gradient && !data.bshadow)
			data.bshadow = $('<div/>', divAtt(0, 0, 1, null)).
				attr('page', data.opts.page).
				attr('name', 'bgtpwrap').
				css({'position': '', width: this.width(), height: this.height()}).
					appendTo(data.parent);

		return gradient;

	},

	resize: function(full) {
		
		var data = this.data().f,
			width = this.width(),
			height = this.height(),
			size = Math.round(Math.sqrt(Math.pow(width, 2)+Math.pow(height, 2)));
		if (!data) return;

		if (full) {
			data.wrapper.css({width: size, height: size});
			data.fwrapper.css({width: size, height: size}).
				children(':first-child').
					css({width: width, height: height});

			data.fpage.css({width: height, height: width});

			if (data.opts.frontGradient)
				data.ashadow.css({width: height, height: width});

			if (flipMethods._backGradient.call(this))
				data.bshadow.css({width: width, height: height});
		}

		if (!data.parent) return;
		if (data.parent.is(':visible')) {
			data.fwrapper.css({top: data.parent.offset().top,
				left: data.parent.offset().left});

			if (data.opts.turn)
				data.fparent.css({top: -data.opts.turn.offset().top, left: -data.opts.turn.offset().left});
		}

		this.flip('z', data.opts['z-index']);

	},

	// Prepares the page by adding a general wrapper and another objects

	_addPageWrapper: function() {

		var att,
			data = this.data().f,
			parent = this.parent();

		if (!data.wrapper) {

			var left = this.css('left'),
				top = this.css('top'),
				width = this.width(),
				height = this.height(),
				size = Math.round(Math.sqrt(Math.pow(width, 2)+Math.pow(height, 2)));
			
			data.parent = parent;
			data.fparent = (data.opts.turn) ? data.opts.turn.data().fparent : $('#turn-fwrappers');

			if (!data.fparent) {
				var fparent = $('<div/>', {css: {'pointer-events': 'none'}}).hide();
					fparent.data().flips = 0;

				if (data.opts.turn) {
					fparent.css(divAtt(-data.opts.turn.offset().top, -data.opts.turn.offset().left, 'auto', 'visible').css).
							appendTo(data.opts.turn);
					
					data.opts.turn.data().fparent = fparent;
				} else {
					fparent.css(divAtt(0, 0, 'auto', 'visible').css).
						attr('id', 'turn-fwrappers').
							appendTo($('body'));
				}

				data.fparent = fparent;
			}

			this.css({position: 'absolute', top: 0, left: 0, bottom: 'auto', right: 'auto'});

			data.wrapper = $('<div/>', divAtt(0, 0, this.css('z-index'), null)).
								attr('page', data.opts.page).
								attr('name', 'tpwrap').
								appendTo(parent).
									prepend(this);

			data.fwrapper = $('<div/>', divAtt(parent.offset().top, parent.offset().left, null, null)).
								hide().
									appendTo(data.fparent);

			data.fpage = $('<div/>', {css: {cursor: 'default'}}).
					appendTo($('<div/>', divAtt(0, 0, 0, 'visible')).
								appendTo(data.fwrapper));

			if (data.opts.frontGradient)
				data.ashadow = $('<div/>', divAtt(0, 0, 1, null)).
					appendTo(data.fpage);

			// Save data

			flipMethods.setData.call(this, data);

			// Set size
			flipMethods.resize.call(this, true);
		}

	},

	// Takes a 2P point from the screen and applies the transformation

	_fold: function(point) {
		//$.fn.log("log","_fold");

		var that = this,
			a = 0,
			alpha = 0,
			beta,
			px,
			gradientEndPointA,
			gradientEndPointB,
			gradientStartV,
			gradientSize,
			gradientOpacity,
			mv = point2D(0, 0),
			df = point2D(0, 0),
			tr = point2D(0, 0),
			width = this.width(),
			height = this.height(),
			folding = flipMethods._foldingPage.call(this),
			tan = Math.tan(alpha),
			data = this.data().f,
			ac = data.opts.acceleration,
			h = data.wrapper.height(),
			o = flipMethods._c.call(this, point.corner),
			top = point.corner.substr(0, 1) == 't',
			left = point.corner.substr(1, 1) == 'l',

			compute = function() {
				var rel = point2D((o.x) ? o.x - point.x : point.x, (o.y) ? o.y - point.y : point.y),
					tan = (Math.atan2(rel.y, rel.x)),
					middle;

				alpha = A90 - tan;
				a = deg(alpha);
				middle = point2D((left) ? width - rel.x/2 : point.x + rel.x/2, rel.y/2);

				var gamma = alpha - Math.atan2(middle.y, middle.x),
					distance =  Math.max(0, Math.sin(gamma) * Math.sqrt(Math.pow(middle.x, 2) + Math.pow(middle.y, 2)));

					tr = point2D(distance * Math.sin(alpha), distance * Math.cos(alpha));

					if (alpha > A90) {
					
						tr.x = tr.x + Math.abs(tr.y * Math.tan(tan));
						tr.y = 0;

						if (Math.round(tr.x*Math.tan(PI-alpha)) < height) {

							point.y = Math.sqrt(Math.pow(height, 2)+2 * middle.x * rel.x);
							if (top) point.y =  height - point.y;
							return compute();

						}
					}
			
				if (alpha>A90) {
					var beta = PI-alpha, dd = h - height/Math.sin(beta);
					mv = point2D(Math.round(dd*Math.cos(beta)), Math.round(dd*Math.sin(beta)));
					if (left) mv.x = - mv.x;
					if (top) mv.y = - mv.y;
				}

				px = Math.round(tr.y/Math.tan(alpha) + tr.x);
			
				var side = width - px,
					sideX = side*Math.cos(alpha*2),
					sideY = side*Math.sin(alpha*2);
					df = point2D(Math.round( (left ? side -sideX : px+sideX)), Math.round((top) ? sideY : height - sideY));
					
				
				// GRADIENTS

					gradientSize = side*Math.sin(alpha);
						var endingPoint = flipMethods._c2.call(that, point.corner),
						far = Math.sqrt(Math.pow(endingPoint.x-point.x, 2)+Math.pow(endingPoint.y-point.y, 2));

					gradientOpacity = (far<width) ? far/width : 1;


				if (data.opts.frontGradient) {

					gradientStartV = gradientSize>100 ? (gradientSize-100)/gradientSize : 0;
					gradientEndPointA = point2D(gradientSize*Math.sin(A90-alpha)/height*100, gradientSize*Math.cos(A90-alpha)/width*100);
				
					if (top) gradientEndPointA.y = 100-gradientEndPointA.y;
					if (left) gradientEndPointA.x = 100-gradientEndPointA.x;
				}

				if (flipMethods._backGradient.call(that)) {

					gradientEndPointB = point2D(gradientSize*Math.sin(alpha)/width*100, gradientSize*Math.cos(alpha)/height*100);
					if (!left) gradientEndPointB.x = 100-gradientEndPointB.x;
					if (!top) gradientEndPointB.y = 100-gradientEndPointB.y;
				}
				//

				tr.x = Math.round(tr.x);
				tr.y = Math.round(tr.y);

				return true;
			},

			transform = function(tr, c, x, a) {
			
				var f = ['0', 'auto'], mvW = (width-h)*x[0]/100, mvH = (height-h)*x[1]/100,
					v = {left: f[c[0]], top: f[c[1]], right: f[c[2]], bottom: f[c[3]]},
					aliasingFk = (a!=90 && a!=-90) ? (left ? -1 : 1) : 0;

					x = x[0] + '% ' + x[1] + '%';

				that.css(v).transform(rotate(a) + translate(tr.x + aliasingFk, tr.y, ac), x);

				data.fpage.parent().css(v);
				data.wrapper.transform(translate(-tr.x + mvW-aliasingFk, -tr.y + mvH, ac) + rotate(-a), x);

				data.fwrapper.transform(translate(-tr.x + mv.x + mvW, -tr.y + mv.y + mvH, ac) + rotate(-a), x);
				data.fpage.parent().transform(rotate(a) + translate(tr.x + df.x - mv.x, tr.y + df.y - mv.y, ac), x);

				//$.fn.log("log","data.opts.frontGradient: " + data.opts.frontGradient);
				if (data.opts.frontGradient)
					gradient(data.ashadow,
							point2D(left?100:0, top?100:0),
							point2D(gradientEndPointA.x, gradientEndPointA.y),
							[[gradientStartV, 'rgba(0,0,0,0)'],
							[((1-gradientStartV)*0.8)+gradientStartV, 'rgba(0,0,0,'+(0.2*gradientOpacity)+')'],
							[1, 'rgba(255,255,255,'+(0.2*gradientOpacity)+')']],
							3,
							alpha);
		
				//$.fn.log("log","flipMethods._backGradient.call(that): " + flipMethods._backGradient.call(that));
				if (flipMethods._backGradient.call(that))
					gradient(data.bshadow,
							point2D(left?0:100, top?0:100),
							point2D(gradientEndPointB.x, gradientEndPointB.y),
							[[0.8, 'rgba(0,0,0,0)'],
							[1, 'rgba(0,0,0,'+(0.3*gradientOpacity)+')'],
							[1, 'rgba(0,0,0,0)']],
							3);
				
			};

		switch (point.corner) {
			case 'tl' :
				point.x = Math.max(point.x, 1);
				compute();
				transform(tr, [1,0,0,1], [100, 0], a);
				data.fpage.transform(translate(-height, -width, ac) + rotate(90-a*2) , '100% 100%');
				folding.transform(rotate(90) + translate(0, -height, ac), '0% 0%');
			break;
			case 'tr' :
				point.x = Math.min(point.x, width-1);
				compute();
				transform(point2D(-tr.x, tr.y), [0,0,0,1], [0, 0], -a);
				data.fpage.transform(translate(0, -width, ac) + rotate(-90+a*2) , '0% 100%');
				folding.transform(rotate(270) + translate(-width, 0, ac), '0% 0%');
			break;
			case 'bl' :
				point.x = Math.max(point.x, 1);
				compute();
				transform(point2D(tr.x, -tr.y), [1,1,0,0], [100, 100], -a);
				data.fpage.transform(translate(-height, 0, ac) + rotate(-90+a*2), '100% 0%');
				folding.transform(rotate(270) + translate(-width, 0, ac), '0% 0%');
			break;
			case 'br' :
				point.x = Math.min(point.x, width-1);
				//$.fn.log("log","_fold corner: " + point.corner);
				compute();
				transform(point2D(-tr.x, -tr.y), [0,1,1,0], [0, 100], a);
				data.fpage.transform(rotate(90-a*2), '0% 0%');
				folding.transform(rotate(90) + translate(0, -height, ac), '0% 0%');

			break;
		}

		data.point = point;
	
	},

	_moveFoldingPage: function(bool) {

		var data = this.data().f,
			folding = flipMethods._foldingPage.call(this);
//$.fn.log("log","turnjs _moveFoldingPage\n\tfolding: " + folding + "\n\tbool: " + bool+ "\n\tdata.corner: " + data.corner+ "\n\tdata.fpage.children()[data.ashadow? '1' : '0']: " + data.fpage.children()[data.ashadow? '1' : '0'] + "\n\tdata.backParent: " + data.backParent);

		if (folding) {
			if (bool) {
				if (!data.fpage.children()[data.ashadow? '1' : '0']) {
					flipMethods.setData.call(this, {backParent: folding.parent()});
//$.fn.log("log","turnjs _moveFoldingPage\n\tfolding.parent(): " + folding.parent);
					data.fpage.prepend(folding);
				}
			} else {
				if (data.backParent)
					data.backParent.prepend(folding);

			}
		}

	},

	_showFoldedPage: function(c, animate) {

		var folding = flipMethods._foldingPage.call(this),
			dd = this.data(),
			data = dd.f;

		if (!data.point || data.point.corner!=c.corner) {
			var event = $.Event('start');
			this.trigger(event, [data.opts, c.corner]);

			if (event.isDefaultPrevented()) return false;
		}


		if (folding) {
			//$.fn.log("log","_showFoldedPage folding:\n\t" + JSON.stringify(folding));

			if (animate) {

				var that = this,
					point = (data.point && data.point.corner==c.corner) ? data.point : flipMethods._c.call(this, c.corner, 1);
			
				this.animatef({from: [point.x, point.y], to:[c.x, c.y], duration: 300, 
					frame: function(v) {
						c.x = Math.round(v[0]);
						c.y = Math.round(v[1]);
						flipMethods._fold.call(that, c);
					}
					/*
					complete: function() {	// immediately fold out again
						flipMethods.hideFoldedPage.call(that, true);
					}
					*/
					});

			} else	{
				flipMethods._fold.call(this, c);
				if (dd.effect && !dd.effect.turning)
					this.animatef(false);

			}

			if (!data.fwrapper.is(':visible')) {
				data.fparent.show().data().flips++;
				flipMethods._moveFoldingPage.call(this, true);
				data.fwrapper.show();

				if (data.bshadow)
					data.bshadow.show();
			}

			//_sb_s.DEBUGmode = 2; $.fn.log("log","dd: "+JSON.stringify(dd));
			turnMethods._setFolding(true,999,"_showFoldedPage");	// (here, we dont have the totalPages, we set it to 999. set _g_turnjs_pageIsFolding = true;
			return true;
		}
		return false;
	},

	hide: function() {

		var data = this.data().f,
			folding = flipMethods._foldingPage.call(this);

		if ((--data.fparent.data().flips)===0)
			data.fparent.hide();

		this.css({left: 0, top: 0, right: 'auto', bottom: 'auto'}).transform('', '0% 100%');

		data.wrapper.transform('', '0% 100%');

		data.fwrapper.hide();

		if (data.bshadow)
			data.bshadow.hide();

		folding.transform('', '0% 0%');

		return this;
	},

	hideFoldedPage: function(animate) {

		var data = this.data().f;
		//_end: will call this!!!    
		//turnMethods._setFolding(false,data.totalPages,"hideFoldedPage");

		if (!data.point) {	// if data.point == null, then no corner is folded
			//$.fn.log("log","hideFoldedPage NO data.point");
			return;
		}
		//$.fn.log("log","hideFoldedPage data.point: " + JSON.stringify(data.point));

		var that = this,
			p1 = data.point,
			hide = function() {
				data.point = null;
				that.flip('hide');
				that.trigger('end', [false]);
						turnMethods._setFolding(false,data.totalPages,"hideFoldedPage");

			};

		if (animate) {
			var p4 = flipMethods._c.call(this, p1.corner),
				top = (p1.corner.substr(0,1)=='t'),
				delta = (top) ? Math.min(0, p1.y-p4.y)/2 : Math.max(0, p1.y-p4.y)/2,
				p2 = point2D(p1.x, p1.y+delta),
				p3 = point2D(p4.x, p4.y-delta);
		
			this.animatef({
				from: 0,
				to: 1,
				frame: function(v) {
					var np = bezier(p1, p2, p3, p4, v);
					p1.x = np.x;
					p1.y = np.y;
					flipMethods._fold.call(that, p1);
					//$.fn.log("log","hideFoldedPage v:"+v);
				},
				complete: hide,
				duration: 300,
				hiding: true
				});

		} else {
			this.animatef(false);
			hide();
		}
	},

	turnPage: function(corner) {

		var that = this,
			data = this.data().f;

		corner = {corner: (data.corner) ? data.corner.corner : corner || flipMethods._cAllowed.call(this)[0]};

		// make sure, the requested page is loaded
			//alert("corner: " + corner.corner);
		if (corner.corner.indexOf("r") > -1) {	// turn to next page
			$.load_page("pv_P"+(_sb_s.currentPageIdx+2),_sb_s.currentPageIdx+1,_sb_fn.getPageImage(_sb_s.currentPageIdx+1),false);
		}
		else {	// turn to previous page
			$.load_page("pv_P"+(_sb_s.currentPageIdx),_sb_s.currentPageIdx-1,_sb_fn.getPageImage(_sb_s.currentPageIdx-1),false);
		}

		var p1 = data.point || flipMethods._c.call(this, corner.corner, (data.opts.turn) ? data.opts.turn.data().opts.elevation : 0),
			p4 = flipMethods._c2.call(this, corner.corner);

			this.trigger('flip').
				animatef({
					from: 0,
					to: 1,
					frame: function(v) {
						var np = bezier(p1, p1, p4, p4, v);
						corner.x = np.x;
						corner.y = np.y;
						flipMethods._showFoldedPage.call(that, corner);
					},
					
					complete: function() {
						that.trigger('end', [true]);
					},
					duration: data.opts.duration,
					turning: true
				});

			data.corner = null;
	},

	moving: function() {

		return 'effect' in this.data();
	
	},

	isTurning: function() {

		return this.flip('moving') && this.data().effect.turning;
	
	},

	_eventStart: function(e) {

		// pointerdown may occur on (example for page #3:
		// a) a div with class="turn-page-wrapper" and an attribute page="3" (has no id attribute)
		// b) on a img = page image with id="pv_P3"
		var data = this.data().f;

		//$.fn.log("log","","*CLEAR*");
		//$.fn.log("log","_eventStart" + "\n\ton target: " + e.target + "\n\tcurrentTarget.id: " + e.currentTarget.id + "\n\ton id: " + e.target.id + "\n\tclass: " + e.target.getAttribute("class") + "\n\tpage: " + data.opts.page + "\n\tisTouch:" + isTouch + "\n\tdata.disabled: " + data.disabled);
		//$.fn.log("log","_eventStart\n\ton elem: " + e.target + "\n\ton id: " + e.target.id + "\n\tisTouch:" + isTouch + "\n\tdata.disabled: " + data.disabled + "\n\tdata.corner: " + data.corner + "\n\tthis.flip('isTurning'): " + this.flip('isTurning') + "\n\tthis.data(): " + JSON.stringify(this.data(),null,"\r\t"));
		if (!data.disabled && isTouch) {	// we have to activate the corner
			//$.fn.log("log","flipMethods._eventStart, disabled: " + data.disabled + "\n\tisTurning: " + this.flip('isTurning') + "\n\tevent type: " + e + "\n\tdata.corner: " + data.corner + "\n\tthis.data().effect: " + this.data().effect);
			//if (!data.corner && !this.data().effect && this.is(':visible')) { // roll over
			if (!data.corner && !this.data().effect) { // roll over
				var corner = flipMethods._cornerActivated.call(this, e);
				//$.fn.log("log","turnjs corner: " + corner + "\n\tcorner X: " + corner.x + "\n\ttypeof corner: " + typeof(corner));
				if (corner) {	// mouse over a corner
					var origin = flipMethods._c.call(this, corner.corner, data.opts.cornerSize/2);
					//$.fn.log("log","turnjs corner touch X: " + origin.x);
					corner.x = origin.x;
					corner.y = origin.y;
					flipMethods._showFoldedPage.call(this, corner, true);
				}
				else {
					//$.fn.log("log","turnjs corner out");
					flipMethods.hideFoldedPage.call(this, true);
					//$.fn.log("log","_eventStart corner hideFoldedPage\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding);
				}
			}
		}

		if (!data.disabled && !this.flip('isTurning')) {
			data.corner = flipMethods._cornerActivated.call(this, e);
				//$.fn.log("log","_eventStart\n\tcorner: " + JSON.stringify(data.corner));

			if (data.corner && flipMethods._foldingPage.call(this, data.corner)) {
				flipMethods._moveFoldingPage.call(this, true);
				this.trigger('pressed', [data.point]);
				e.preventDefault();
				return false;
			} else
				data.corner = null;
		}
		e.preventDefault();
		return false;

	},

	_eventMove: function(e) {
		//_sb_s.DEBUGmode = 2; $.fn.log("log","turnjs _eventMove - cornersdisabled: " + cornersdisabled + "\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding);
		if (cornersdisabled) return;

		var data = this.data().f;
		//$.fn.log("log","_eventMove\n\ton elem: " + e.target + "\n\ton id: " + e.target.id+ "\n\tdata.disabled: " + data.disabled + "\n\tdata.corner: " + data.corner + "\n\tthis.is(':visible'): " + this.is(':visible') + "\n\tthis.data(): " + JSON.stringify(this.data(),null,"\r\t"));
		if (!data) return;

		if (!data.disabled) {
			//$.fn.log("log","_eventMove\n\tx/y: " + e.originalEvent.pageX + " / " + e.originalEvent.pageY + "\n\tcorner: " + JSON.stringify(data.corner) + "\n\tdata: " + data);

			if (data.corner) {
				//$.fn.log("log","_eventMove\n\tcorner: " + JSON.stringify(data.corner));
				var pos = data.parent.offset(),
					pXY = _sb_fn.get_pointerXY(e);
				if (pXY == null) return;

				data.corner.x = pXY.x-pos.left;
				data.corner.y = pXY.y-pos.top;

				flipMethods._showFoldedPage.call(this, data.corner);
			
			} else {
				if (!this.data().effect && this.is(':visible')) { // if this.data().effect is an object, any folding effect is active, then 'roll over' (folding a corner after mouse over) is in action
					if (isTouch) flipMethods._eventStart.call(this, e);
					else {
						var corner = flipMethods._cornerActivated.call(this, e);
							//$.fn.log("log","turnjs corner ROLL over:\n\tcorner: " + JSON.stringify(corner));
						if (corner) {	// mouse over a corner 
							//$.fn.log("log","turnjs corner over:\n\t"+JSON.stringify(corner));
							var origin = flipMethods._c.call(this, corner.corner, data.opts.cornerSize/2);
							corner.x = origin.x;
							corner.y = origin.y;
							flipMethods._showFoldedPage.call(this, corner, true);
						}
						else {
							//$.fn.log("log","turnjs corner out");
							flipMethods.hideFoldedPage.call(this, true);
							//$.fn.log("log","_eventMove corner hideFoldedPage\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding);
						}
					}
				}
			}
		}
	},

	_eventEnd: function() {
		var data = this.data().f;
		if (!data) return;
		//$.fn.log("log","_eventEnd\n\tdata.disabled: " + data.disabled + "\n\tdata.point: " + JSON.stringify(data.point));

		if (!data.disabled && data.point) {
			turnMethods._setFolding(true,data.totalPages,"_showFoldedPage");	//set _g_turnjs_pageIsFolding = true;
			var event = $.Event('released');
			this.trigger(event, [data.point]);
			if (!event.isDefaultPrevented()) {
				flipMethods.hideFoldedPage.call(this, true);
//$.fn.log("log","_eventEnd corner hideFoldedPage\n\t_g_turnjs_pageIsFolding: " + _g_turnjs_pageIsFolding);
			}
		}

		data.corner = null;

	},

	disable: function(disable) {

		flipMethods.setData.call(this, {'disabled': disable});
		return this;

	}
},

cla = function(that, methods, args) {
	//	alert("cla\nthat:" + that + "\nmethods: " + JSON.stringify(methods) + "\nargs: " + JSON.stringify(args) + "\ntypeof args: " + typeof(args) + "\ntypeof args[0]: " + typeof(args[0]));
	if (!args[0] || typeof(args[0])=='object') {
		//alert("1 args[0]:" + args[0]);
		return methods.init.apply(that, args);
	}
	else if(methods[args[0]] && args[0].toString().substr(0, 1)!='_') {
			//alert("2 args[0]:" + args[0]+ "\nmethods[args[0]]:" + methods[args[0]]);
			return methods[args[0]].apply(that, Array.prototype.slice.call(args, 1));
		}
		else {
			//alert(args[0] + ' is an invalid value');
			throw args[0] + ' is an invalid value';
		}
};

$.extend($.fn, {
// added Ai
	register_cb_turn_end: function(cb) {
		//alert("register_cb_turn_end: \n" + cb);
		cb_turn_end = cb;
	},

	register_cb_before_removepage: function(cb) {
		//alert("register_cb_before_removepage: \n" + cb);
		cb_before_removepage = cb;
	},

	register_cb_after_addpage: function(cb) {
		//alert("register_cb_after_addpagee: \n" + cb);
		cb_after_addpage = cb;
	},

	pagesize: function(w, h, wi, hi){	//w h of container, wi hi of image
		var data = this.data();
		//alert("pagesize w x h: " + w + " x " + h + "\ntotal pages: " + data.totalPages);
		for (var i = 1; i <= data.totalPages; i++) {
			// resize the container
			data.pageObjs[i].css({width: w, height: h});
			// resize the container's child: the image
			data.pageObjs[i].children(":first").css({width: wi, height: hi});
			//$.fn.log("log","page #" + i + " width: " + data.pageObjs[i].width());
		}
	},

	pagesrc: function(pgnum,url){
		var data = this.data();
		//$.fn.log("log","pagesrc - page: " + pgnum + "\n\ttotalPages: " + data.totalPages + "\n\tsrc: " + url);
		if (pgnum > 0 && pgnum <= data.totalPages) {
			// set the image path
			data.pageObjs[pgnum].children(":first").attr('src',url);
		}
	},
	
	getpageData: function() {
		return(this.data());
	},
	
	disablecorners: function(bool) {
		if(typeof(bool) != 'undefined') cornersdisabled = bool;
		return(cornersdisabled);
	},


// END added Ai

	flip: function(req, opts) {
		return cla(this, flipMethods, arguments);
	},

	turn: function(req) {
		return cla(this, turnMethods, arguments);
	},

	transform: function(transform, origin) {

		var properties = {};

		if (origin) properties[vendor+'transform-origin'] = origin;

		properties[vendor+'transform'] = transform;
 		//$.fn.log("log","TURNJS properties: " + properties[vendor+'transform-origin']);
	
		return this.css(properties);

	},

	animatef: function(point) {

		var data = this.data();

		if (data.effect)
			clearInterval(data.effect.handle);

		if (point) {

			if (!point.to.length) point.to = [point.to];
			if (!point.from.length) point.from = [point.from];
			if (!point.easing) point.easing = function (x, t, b, c, data) { return c * Math.sqrt(1 - (t=t/data-1)*t) + b; };

			var j, diff = [],
				len = point.to.length,
				that = this,
				fps = point.fps || 45,	// changed Ai. from 30 to 45 (faster, less calculating)
				time = - fps,
				f = function() {
					var j, v = [];
					time = Math.min(point.duration, time + fps);
	
					for (j = 0; j < len; j++)
						v.push(point.easing(1, time, point.from[j], diff[j], point.duration));

					point.frame((len==1) ? v[0] : v);
					//$.fn.log("log","turnjs animatef time:"+time+" point.duration:"+point.duration+"\n point:"+JSON.stringify(point));

					if ((time==point.duration) || ((point["from"][0]==point["to"][0]) && (point["from"][1]==point["to"][1]))) {
						//$.fn.log("log","turnjs animatef DONE fps:"+fps);
						clearInterval(data.effect.handle);
						delete data['effect'];
						that.data(data);
						if (point.complete)
							point.complete();
						}
					};

			for (j = 0; j < len; j++)
				diff.push(point.to[j] - point.from[j]);

			data.effect = point;
			data.effect.handle = setInterval(f, fps);
			this.data(data);
			f();
		} else {
			delete data['effect'];
		}
	}
});


$.isTouch = isTouch;

})(jQuery);