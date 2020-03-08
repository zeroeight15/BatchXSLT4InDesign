# Compiling your own BatchXSLT4InDesign
The folder 'BatchXSLT-40' contains a **NetBeans** project including all java source files.

Using the NetBeans IDE select 'Open Project' from the menu and navigate to the folder 'BatchXSLT-40' - this will import the project.

Selecting the menu item 'Clean and Build' will compile the main class jar 'BatchXSLT.jar (and copy the libraries) to the folder 'dist'.\
To package the BatchXSLT.app choose 'Package as' -> All Artifacts. This will bundle the entire 'BatchXSLT.app' into the folder 'distOSX_BatchXSLT_JavaVM_bundled'.

The bundled app is a Mac OS X app.

If you want the Mac App then you may delete the file 'BatchXSLT.bat' (this is the starter for Windows).

If you want the Windows app you may delete the file 'BatchXSLT.sh'. Start BatchXSLT with a double-click on 'BatchXSLT.bat'.
