/** 
 * JTOverrideThread.java
 *
 * Title: check for jobticket files
 * Description:    
 * @author            Andreas Imhof
 * @version            20140210
 */

package com.epaperarchives.batchxslt;

import java.io.*;



public class JTOverrideThread extends Thread {
    public JTOverrideThread() {
    }
    boolean running = false;
    
    @Override
    public void run() {
        if (this.running == true) {
            System.out.println("### JTOverrideThread already running. 2nd call prohibited");
            return;
        }
        Thread.currentThread().setName("JTOverrideThread");
        this.running = true;
        boolean exists = false;
        boolean canwrite = false;
        String overrideJTpath = "";
        String overrideJTfileName = "";
        String overrideJTQueueFileName = "";
        boolean was_exit_command_line = false;
        int breakpt = 0;

        while (mainXSLTFrame.jtoverrideName.equals("") == false) {
            //BatchXSLT.g_mainXSLTFrame.showMess("++++++ breakpt: " + breakpt + " sleeping: " + BatchXSLT.g_mainXSLTFrame.jtoverrideSleepTime + "\n");
            try {
                Thread.sleep(mainXSLTFrame.jtoverrideSleepTime);
            }
            catch (InterruptedException e) {}
            // check if we find a jobticket named 'override.jt'
            //System.out.println("++++++ JTOverrideThread run...");
            if (mainXSLTFrame.processing == true) { breakpt = 1; continue; }    // if main transformer is not busy then load and run it
            //BatchXSLT.g_mainXSLTFrame.showMess("/////////////////////////////do_mainLoop is not prosessing: " + mainXSLTFrame.processing + "\n");

            
            File overrideJTfile, overrideJTQueueFile;
            exists = false;
            canwrite = false;

            //---------------------------
            // check for override.jt
            overrideJTpath = mainXSLTFrame.jtoverrideQueuePath;
            if (overrideJTpath.endsWith(File.separator) == false) overrideJTpath = overrideJTpath + File.separator;
            if (mainXSLTFrame.DEBUGjtThread > 0) BatchXSLT.g_mainXSLTFrame.showMess( "*** Looking for : "+ mainXSLTFrame.jtoverrideName + " in path: '" + overrideJTpath +  "'\n" );
            for (int i = 0; i <= 8; i++) {
                overrideJTfileName = mainXSLTFrame.jtoverrideName;
                if (i > 0) overrideJTfileName += "" + i;
                overrideJTfile = new File(overrideJTpath + overrideJTfileName);
                exists = overrideJTfile.exists();
                if (!exists) continue;
                canwrite = overrideJTfile.canWrite();
                if (canwrite) break;
            }

            //---------------------------
            // if we have no 'override.jtx' then check for the queue file
            if (!exists) {
                breakpt = 2;
                overrideJTfileName = "";
                do {
                    overrideJTQueueFileName = mainXSLTFrame.jtoverrideQueuePath + mainXSLTFrame.jtoverrideQueueName;
                    overrideJTQueueFile = new File(overrideJTQueueFileName);
                    exists = overrideJTQueueFile.exists();
                    if (!exists) { breakpt = 3; break; }
                    canwrite = overrideJTQueueFile.canWrite();
                    if (!canwrite) { breakpt = 4; break; }
                    //System.out.println("reading: " + overrideJTQueueFileName);
                    // open the queue file and read a jobticket file name (with path);
                    BufferedReader br = null;
                    BufferedWriter bw = null;
                    String jtline = "";
                    int jtlinenumber = 0;
                    try {
                                            br = new BufferedReader(new FileReader(overrideJTQueueFileName));
                    } catch ( FileNotFoundException e ) {    // uups could not read it - locked file?
                         exists = canwrite = false;
                         try { br.close(); } catch ( IOException ebr ) {}
                         breakpt = 5;
                         break;
                    }
                    while (true) {    // read lines
                        was_exit_command_line = false;
                        overrideJTfileName = "";
                        try {
                            jtline = br.readLine();
                            jtlinenumber++;
                        //System.out.println("jtline: " +jtline);
                        } catch ( IOException e ) {    // uups could not read it - locked file?
                            System.out.println("Exception reading JobTicket Queue file: " + overrideJTQueueFileName + "\n" +e);
                            jtline = ""; exists = canwrite = false;
                            try { br.close(); } catch ( IOException ebr ) {}
                            break;
                        }
                        if (jtline == null) {    // is empty file or all lines are processed
                            exists = canwrite = false;
                            try {    br.close();
                                overrideJTQueueFile.delete();
                                if (mainXSLTFrame.exitFrom_jtoverrideQueue == true) {    // exit after que is done
                                    BatchXSLT.g_mainXSLTFrame.thisWindowClosing(null, mainXSLTFrame.exitFrom_jtoverrideQueue_exitcode);
                                }
                            } catch ( IOException e ) {}
                            break;
                        }
                        if (jtline.equals("") == true) {    // is empty line
                            continue;
                        }
                        if (jtline.startsWith("exit,") == true) {    // force exit after done
                            mainXSLTFrame.exitFrom_jtoverrideQueue = true;
                            try {
                                mainXSLTFrame.exitFrom_jtoverrideQueue_exitcode = Integer.valueOf(jtline.split(",")[1]);
                            } catch ( NumberFormatException e ) {}
                            was_exit_command_line = true;
                            //System.out.println("exit code: " +mainXSLTFrame.exitFrom_jtoverrideQueue_exitcode);
                            break;
                        }
                        // check if the read jobticket line points to an existing jobticket file
                        overrideJTfileName = jtline;
                        if (mainXSLTFrame.DEBUGjtThread > 0) BatchXSLT.g_mainXSLTFrame.showMess( "*** overrideJTfileName: "+ overrideJTfileName + "\n" );
                        if ((overrideJTfileName != null) && (overrideJTfileName.equals("") == false)) {

                            if (overrideJTfileName.indexOf(File.separator) < 0) {
                                String path = mainXSLTFrame.jtoverrideQueuePath;
                                if (path.endsWith(File.separator) == false) path = path + File.separator;
                                overrideJTfileName = path + overrideJTfileName;
                            }
                            if (overrideJTfileName.startsWith("~") == true) {
                                String home = System.getProperty("user.home"); if (home.endsWith(File.separator) == false) home = home + File.separator;
                                overrideJTfileName = home + overrideJTfileName.substring(2);    // cut leading path ~/
                            }
                            
                            overrideJTfile = new File(overrideJTfileName);
                            exists = overrideJTfile.exists();
                            //BatchXSLT.g_mainXSLTFrame.showMess("**** JT line#: " + jtlinenumber + " : '" + jtline+ " : " + exists + "\n" );
                            if (!exists) { continue; }    // may be an already processed (and deleted) jt file
                            canwrite = overrideJTfile.canWrite();
                            int wait = 0;
                            while (!canwrite && (wait < 10)) {
                                try { Thread.sleep(10); } catch (InterruptedException e) {}    // wait 10 ms max
                                canwrite = overrideJTfile.canWrite();
                                wait++;
                            }
                            if (!canwrite) {    // if we can not currently write to jt file we have to wait for next run
                                try { br.close(); } catch ( IOException e ) {}
                                exists = canwrite = false;
                                breakpt = 10; break;
                            }
                        }
                        else {
                            exists = canwrite = false;
                            continue;
                        }
                        break;
                    }
                    breakpt = 6;
                    if (!was_exit_command_line && (exists == false)) break;    // no queue file or no jt file available
                    if (mainXSLTFrame.DEBUGjtThread > 0) BatchXSLT.g_mainXSLTFrame.showMess("------- JT line#: " + jtlinenumber + " : '" + jtline + "'\n" );

                    // copy all following lines to temp file except the first line we just have read
                    if (was_exit_command_line || (jtlinenumber > 1)) {    // if jtlinenumber == 1 then the read 1st line is an existing jt file
                                                                          // if jtlinenumber > 1 we had some invalid lines before this valid jt
                        String overrideJTQueueFileNameTemp = "";
                        try {    // create temp file
                            String line = "";
                            overrideJTQueueFileNameTemp = overrideJTQueueFileName + ".tmp";
                            if (mainXSLTFrame.DEBUGjtThread > 0) BatchXSLT.g_mainXSLTFrame.showMess( "*** overrideJTQueueFileNameTemp: "+ overrideJTQueueFileNameTemp + " writing\n" );
                            bw = new BufferedWriter(new FileWriter(overrideJTQueueFileNameTemp));
                            if (!was_exit_command_line) { bw.write(jtline); bw.newLine(); }    // including current valid jt line: will be deleted in next run when jt file was done
                            while ((line = br.readLine()) != null) { bw.write(line); bw.newLine(); }
                        } catch ( IOException e ) {
                            System.out.println("Exception writing temp JobTicket Queue file: " + overrideJTQueueFileNameTemp + "\n" +e);
                        }
                        finally {
                            try { if (bw != null) { bw.flush(); bw.close(); } 
                            } catch ( IOException e ) {
                                System.out.println("Exception closing temp JobTicket Queue file: " + overrideJTQueueFileNameTemp + "\n" +e);
                            }
                            try { br.close(); } catch ( IOException e ) {}
                            try {
                                overrideJTQueueFile.delete();
                                File tmp = new File(overrideJTQueueFileNameTemp);
                                if (tmp.exists()) {    // rename temp file to opriginal name
                                    overrideJTQueueFile = new File(overrideJTQueueFileName);
                                    tmp.renameTo(overrideJTQueueFile);
                                }
                                if (mainXSLTFrame.DEBUGjtThread > 0) BatchXSLT.g_mainXSLTFrame.showMess( "*** overrideJTQueueFileNameTemp: "+ overrideJTQueueFileNameTemp + " deleted\n" );
                            } catch ( Exception e ) {
                                System.out.println("Exception renaming temp JobTicket Queue file: " + overrideJTQueueFileNameTemp + "\n" +e);
                                 break;
                            }
                        }
                    }
                    else {
                        try { br.close(); } catch ( IOException e ) {}
                    }
                    breakpt = 8;
                    break;
                } while(false);
            }

            //---------------------------
            // start this JobTicket
            if ((overrideJTfileName != null) && (overrideJTfileName.equals("") == false) && exists && canwrite) {    // if exists then load and run it
                if (mainXSLTFrame.DEBUGjtThread > 0) System.out.println("**JTOverrideThread: JobTicket found in Queue file: '" + overrideJTfileName + "'");
                mainXSLTFrame.nextJobTicketPath = mainXSLTFrame.jtoverrideQueuePath;
                mainXSLTFrame.jobTicketFileName = overrideJTfileName;
                mainXSLTFrame.old_start_over = mainXSLTFrame.start_over;
                mainXSLTFrame.start_over = 3;        // force a start over from override job ticket
                mainXSLTFrame.mode_before_overridejt = mainXSLTFrame.mode;
                mainXSLTFrame.mode_setby_overridejt = 100;    // like pressing the transform button
                mainXSLTFrame.mode = 4;
                BatchXSLT.g_mainXSLTFrame.showMess( "*** JobTicket '" + mainXSLTFrame.jobTicketFileName + "' scheduled to run.\n" );
                try {
                    BatchXSLT.g_mainXSLTFrame.mainLoop();
                }
                catch (Exception e) {
                    BatchXSLT.g_mainXSLTFrame.showRunMess( "##### ERROR: Exception waking up 'mainLoop' from JobTicket" + mainXSLTFrame.jtoverrideName);
                    e.printStackTrace();
                }
 
            }

        }


//        System.out.println("------ JTOverrideThread done");
    }

}

