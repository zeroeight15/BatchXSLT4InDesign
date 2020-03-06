/** 
 * BatchXSLT.java
 *
 * Title:			a sleeper Thread
 * Description:	
 * @author			Andreas Imhof
 * @version			
 */

package com.epaperarchives.batchxslt;

import javax.swing.*;



public class SleeperThread extends Thread {
	private Thread myslepperThread = null;
	private String[] the_args = null;

	public SleeperThread( String[] args) {
		this.the_args = args;
		//System.out.println("++++++ Sleeper start...");
	}

	public void run() {
		Thread.currentThread().setName("SleeperThread");

		//System.out.println("++++++ Sleeper run...");
		if (BatchXSLT.g_mainXSLTFrame.loopDelay > 500) BatchXSLT.g_mainXSLTFrame.showRunMess( "Sleeping  " + BatchXSLT.g_mainXSLTFrame.loopDelay + " ms... " );
	 	int mySleepTime = 200;
		if (mySleepTime > BatchXSLT.g_mainXSLTFrame.loopDelay) mySleepTime = BatchXSLT.g_mainXSLTFrame.loopDelay;

		while ((BatchXSLT.g_mainXSLTFrame.general_abort == false) && (BatchXSLT.g_mainXSLTFrame.sleeperAborted == false)) {
			try {
				Thread.sleep(200);
			}
			catch (InterruptedException e) {}
			if (mySleepTime < com.epaperarchives.batchxslt.mainXSLTFrame.loopDelay) {
				if ((mySleepTime + 200) > BatchXSLT.g_mainXSLTFrame.loopDelay) mySleepTime = BatchXSLT.g_mainXSLTFrame.loopDelay - mySleepTime;	// our last small sleep
				else mySleepTime += 200;	// next part to sleep
				continue;					// go sleeping again
			}
			break;
		}

		// call our main engine thread if not aborted
		if (!BatchXSLT.g_mainXSLTFrame.sleeperAborted) {
			try {
				BatchXSLT.g_mainXSLTFrame.mainLoop(the_args);
			}
			catch (Exception e) {
				BatchXSLT.g_mainXSLTFrame.showRunMess( "##### ERROR: Exception waking up 'mainLoop'");
				e.printStackTrace();
			}
		}
		else {
			//System.out.println("## Sleeper aborted");
			// commented20090429 BatchXSLT.g_mainXSLTFrame.processing = false;
			BatchXSLT.g_mainXSLTFrame.general_abort = false;
			BatchXSLT.g_mainXSLTFrame.setItemStates(0,false);
		}
		//System.out.println("------ Sleeper done");
	}

}

