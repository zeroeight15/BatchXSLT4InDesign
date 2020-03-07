# BatchXSLT4InDesign
## An XML/HTML transformer – even for InDesign documents
(not complete yet: source code comming soon)

BatchXSLT is a powerful XML transformer. It transforms any XML file to what ever the transform stylesheet commands.\
BatchXSLT can run in stand-alone mode or can be be remotely controlled.\
'Remotely controlled' means, that it can be controlled from any application capable to write Job Tickets. A Job Ticket is a human readable file which tells BatchXSLT what to do.\
InDesign scripting can write such files. It also can **export** documents to various formats like
- document content to IDML (XML)
- images to JPEG
- Pages to PDF
- and more...

After having exported a whole bunch of things from an document, InDesign can command BatchXSLT to create useful, readable XML from all these exported 'raw' files.\
In fact, BatchXSLT does nothing more than an XSL Transformation and image conversion and packs everything into a single XML file. This XML is very rich on information about the original InDesign document content for a HTML flipping pages eBook can be created at the same time.

This combination of BatchXSLT controlled by InDesign demands for the package name:
#### BatchXSLT4InDesign
BatchXSLT4InDesign is not just a XML transformer, but an entire application bundle including
- the transformer engine BatchXSLST.app (including the Java virtual machine)
- the InDesign exporter scripts to create a 'base export' of an InDesign document (IDML and images)
- needed transform style sheets (XSL) to transform InDesign IDML to readable and reusable XML
- the XSL style sheets to transform the XML to an HTML flip page eBook
- the Javascripts and CSS to display this flip book in a Browser

To download 'ready to run' binaries for Windows and Mac OS X enter the folder 'binaries'.

Or, as a programmer, click the button [ Clone or Download ]

More is comming soon - working...
