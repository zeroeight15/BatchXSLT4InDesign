# Compiling your own BatchXSLT4InDesign
The folder 'BatchXSLT-40' contains a **NetBeans** project including all java source files.

Using the NetBeans IDE select 'Open Project' from the menu and navigate to the folder 'BatchXSLT-40' - this will import the project.

Selecting the menu item 'Clean and Build' will compile the main class jar 'BatchXSLT.jar (and copy the libraries) to the folder 'dist'.\
To package the BatchXSLT.app choose 'Package as' -> All Artifacts. This will bundle the entire 'BatchXSLT.app' into the folder 'distOSX_BatchXSLT_JavaVM_bundled'.\
If no Java Virtual Machine app may be found, the bundle is built without JVM.\
The bundled app is a Mac OS X app.

#### Have a bunbled Java Virtual Machine
To include a JVM into the bundle place a JVM beside the build.xml file.\
A Java Virtual Machine must be bundles as an OSX app. The folder structure of an OSX app is:

**JavaVM.app/**\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-- **Contents/**\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-- Info.plist&nbsp;&nbsp;(MacOS only)\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-- PkgInfo&nbsp;&nbsp;(MacOS only)\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-- **MacOS/**&nbsp;&nbsp;(MacOS libjli.dylib)\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-- **Home/** (JVM binaries)\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-- **bin/**\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-- **lib/**\
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-- **conf/** 


If you want the Mac App then you may delete the file 'BatchXSLT.bat' (this is the starter for Windows).

If you want the Windows app you may delete the file 'BatchXSLT.sh'. Start BatchXSLT with a double-click on 'BatchXSLT.bat'.
