/*
 ======================================================================
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
EDV-Dienstleistungen
CH-Guemligen, Switzerland
www.aiedv.ch

CREATED: 2007
Version: 5.1
Version date: 20081009
======================================================================
*/
this.name = "ft_searchWindow";	// set window name
this.focus();

var flipbookWin = null;
var flipbookWindowName = "BXSLTflipWin";

function get_page_article(request_url) {
	var breakpoint = 0;
	do {
		if ( (flipbookWin == null) || (flipbookWin.closed == true) ) {
			try{
				if (opener && (opener.name == flipbookWindowName) ) flipbookWin = opener;
			}
			catch (e) { breakpoint = 1; break; }	// fall through to open the search window
		}
		if ( (flipbookWin == null) || (flipbookWin.closed == true) ) {
			flipbookWin = window.open("",flipbookWindowName,"location=yes,menubar=yes,resizable=Yes,scrollbars=Yes,status=yes,titlebar=yes,toolbar=yes,dependent=No");
			breakpoint = 2; break;
		}
		if ( (flipbookWin == null) || (flipbookWin.closed == true) ) { breakpoint = 2; break; }
		//alert("flipbookWin.name: " + flipbookWin.name);
	
		// get requested page number and article idx
		var go2page_sequence = -1;
		var go2article_idx = -1;
		if (request_url.indexOf("?") >= 0) {
			var paramsarr = request_url.split("?")[1].split("&");
			for (var i = 0; i < paramsarr.length; i++) {
				param = paramsarr[i].split("=");
				switch (param[0]) {
					case "p": go2page_sequence = parseInt(param[1]);
						break;
					case "a": go2article_idx = parseInt(param[1]);
						break;
				}
			}
			//alert("go2page_sequence: " + go2page_sequence + "\ngo2article_idx: " + go2article_idx);
		}
		// the flip book window seems to be open but may have different document, page or article
		var plain_current_url = flipbookWin.location.protocol + "//" + flipbookWin.location.hostname + flipbookWin.location.pathname;
		plain_current_url = unescape(plain_current_url);
	
		if (request_url.toLowerCase().indexOf(plain_current_url.toLowerCase()) == 0) {	// requested document currently is open: flip page and article
			//alert("same document: flipping page and article");
			try {
				flipbookWin.focus();
				// call goto page and article
				if (go2page_sequence > -1) flipbookWin.goto_continued_article(go2article_idx,go2page_sequence);
				return(false);
			}
			catch (e) { breakpoint = 4; break; }	// load requested document using href
			
		}
		breakpoint = 5;
	} while (false);
	//alert("breakpoint: " + breakpoint + "\n" + request_url + "\n\n" + plain_current_url);

	// open the window to show flipping book
	if ( (flipbookWin != null) && (flipbookWin.closed == false) ) {
		try{
			flipbookWin.focus();
		}
		catch (e) { }	// fall through to open the search window
	}
	else flipbookWin = window.open("",flipbookWindowName,"location=yes,menubar=yes,resizable=Yes,scrollbars=Yes,status=yes,titlebar=yes,toolbar=yes,dependent=No");
	flipbookWin.location.href = request_url;
	declare_caller_window_cnt = 0;
	setTimeout("declare_caller_window()", 500);	// let flip window reload and then declare our search window
	return(false);
}

var declare_caller_window_cnt = 0;
function declare_caller_window () {
	declare_caller_window_cnt++;
	if (declare_caller_window_cnt > 50) return;
	try {
		flipbookWin.ft_searchWin = this;
		//alert("declare_caller_window: " + flipbookWin.ft_searchWin.name);
	} catch (e) {
		setTimeout("declare_caller_window()", 100);
	}
}


