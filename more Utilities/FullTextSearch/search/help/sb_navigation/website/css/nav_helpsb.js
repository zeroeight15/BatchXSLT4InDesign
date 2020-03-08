function doprintthis(contentobjID,cssname,title) {
	var contentobj = document.getElementById(contentobjID);
	if (!contentobj) return;
	var view_X=10, view_Y=10, wid=400, hig=400;
	F=window.open("","myPrintWindow","screenX=" + view_X + ", screenY=" + view_Y + ", left=" + view_X + ", top=" + view_Y + ", width=" + wid + ",height=" + hig + ",resizable=Yes,scrollbars=Yes,status=No,toolbar=No,menubar=No");
	F.document.write('<!DOCTYPE html>\r');
	F.document.write('<html><head>\r');
	F.document.write('<title>' + title + '</title>\r');
	F.document.write('<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />\r');
	F.document.write('<meta charset=UTF-8" />\r');
	F.document.write('<link rel="StyleSheet" href="' + cssname + '" type="text/css" media="all">\r');
	F.document.write('</head>\r<body style="visibility:hidden;">\r');
		// insert the content

	F.document.write(contentobj.innerHTML);
	F.document.writeln("\r</body></html>");
	F.document.close();

	// turn off certain elements
	var div_elements = F.document.getElementsByTagName("div");
	for (var i=0; i<div_elements.length;i++) {
		if ((div_elements[i].id == "printthis") 
			|| (div_elements[i].getAttribute("class") == "goto_article_nav_bot")
			|| (div_elements[i].getAttribute("id") == "lightbox_head")
			) div_elements[i].style.display = "none";
	}

	F.document.getElementsByTagName("body")[0].style.visibility="visible";
	F.focus();
	setTimeout("F.print();F.close();",200);	// at least Opera needs some time or it will print the main window
}
