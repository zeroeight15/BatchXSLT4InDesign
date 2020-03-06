
package com.epaperarchives.batchxslt;

// Imported java classes
import java.io.*;
import java.io.IOException;
import java.util.*;


/* ===========================================================
 * a Job Scheduler
 */
public class Scheduler
{
    private static final String PACKAGE_NAME = "com.epaperarchives.batchxslt.Scheduler";


	// ============ Scheduler stuff ========================
	/*
	;a Start    End      Start  End    Day     Repeat Script to call
	;c date     date     time   time   of      time
	;t                   0><    0><    week    in
	;i                   235959 235959 1=Sun   secnds name                           path
	;v yyyymmtt yyyymmtt hhmmss hhmmss 1-7     sssss   		                        
	; ,--------,--------,------,------,-------,------,------------------------------,--------------------------------------------

	1,19980101,20991231,030000,,123456,,jobName,jobPath
	*/
	private String active = "";			// 0 or "" = this record is inactive, otherwise active
	private String startdate = "";		// YYYYMMDD date from which on this schedule is valid like 20070101
	private String enddate = "";		// YYYYMMDD date from which on this schedule is invalid like 20991231
	private String starttime = "";		// HHMMSS when to start
	private String endtime = "";		// HHMMSS when to stop
	private String weekdays = "";		// list of days when to start: 124 = sunday monday and wednesday
	private String repeatseconds = "";	// interval to recall in seconds
	private String runname = "";		// the name of the JT to run
	private String runpath = "";		// the path to the JT to run
	
	private boolean oneshot = false;		// true = to call once a day

	private Calendar CalStartDate = null;
	private Calendar CalEndDate = null;
	private Calendar now = null;
	private Calendar CalLastExec = null;

	// constructor
	public Scheduler (String raw_scheduler_string) {
		active = "";
		startdate = "";
		enddate = "";
		starttime = "";
		endtime = "";
		weekdays = "";
		repeatseconds = "";
		runname = "";
		runpath = "";

		CalStartDate = null;
		CalEndDate = null;

		String[] paramsarr = raw_scheduler_string.split(",");
		for (int i = 0; i < paramsarr.length; i++) {
			switch (i) {
				case 0: active = paramsarr[i]; break;
				case 1: startdate = paramsarr[i]; break;
				case 2: enddate = paramsarr[i]; break;
				case 3: starttime = paramsarr[i]; break;
				case 4: endtime = paramsarr[i]; break;
				case 5: weekdays = paramsarr[i]; break;
				case 6: repeatseconds = paramsarr[i]; break;
				case 7: runname = paramsarr[i]; break;
				case 8: runpath = paramsarr[i]; break;
			}
		}

		// set start date if given. NOW is immediate start now
		CalStartDate = Calendar.getInstance();		// get start date calendar, init to now
		CalStartDate.setLenient(true);
		if (startdate.length() != 8) startdate = "20000101";	// must be like 20070101: YYYYMMDD
		if (starttime.length() != 6) starttime = "000000";		// must be like 030005: HHMMSS
			String year_str = startdate.substring(0,4);		// year from 20070101
			String month_str = startdate.substring(4,6);	// month from 20070101
			String day_str = startdate.substring(6);		// day from 20070101
			String hour_str = starttime.substring(0,2);		// hour from 030005
			String minute_str = starttime.substring(2,4);	// minute from 030005
			String second_str = starttime.substring(4);		// second from 030005

			int year = Integer.valueOf(year_str);
			int month = (Integer.valueOf(month_str) - 1);
			int date = Integer.valueOf(day_str);
			int hour = Integer.valueOf(hour_str);
			int minute = Integer.valueOf(minute_str);
			int second = Integer.valueOf(second_str);

			CalStartDate.set(year, month, date, hour, minute, second);
			CalStartDate.set(Calendar.MILLISECOND, 0);

		// set end date if given. null is forever
		CalEndDate = Calendar.getInstance();		// get end date calendar
		CalEndDate.setLenient(true);
		if (enddate.length() != 8) enddate = "29991231";	// must be like 29991231: YYYYMMDD
		if (endtime.length() != 6) {
			endtime = "235959";		// must be like 030005: HHMMSS
			oneshot = true;
		}
			year_str = enddate.substring(0,4);
			month_str = enddate.substring(4,6);
			day_str = enddate.substring(6);
			hour_str = endtime.substring(0,2);
			minute_str = endtime.substring(2,4);
			second_str = endtime.substring(4);

			year = Integer.valueOf(year_str);
			month = (Integer.valueOf(month_str) - 1);
			date = Integer.valueOf(day_str);
			hour = Integer.valueOf(hour_str);
			minute = Integer.valueOf(minute_str);
			second = Integer.valueOf(second_str);

			CalEndDate.set(year, month, date, hour, minute, second);
			CalEndDate.set(Calendar.MILLISECOND, 0);


		if (repeatseconds.equals("") == false) oneshot = false;	// do repeat and not oneshot
	}




	// check if we should run this job
	public int should_run () {
		if (active.equals("0") == true) return(0);									// schedule is inactive - do not run

		CalLastExec = Calendar.getInstance();
		now = Calendar.getInstance();

		File jtf = new File(BatchXSLT.g_mainXSLTFrame.jobTicketFileName);
		String schedule_done_file = "scheduler_done" + File.separator +  jtf.getName() + ".done";

		boolean exists = utils.existsFile(schedule_done_file);
		if (exists == true) {	// open file and get last execution date/time
			// open file and read first line
			String ms_str = "0";	// init to 'has never run'
			BufferedReader br = null;
			try { 
				br = new BufferedReader(new FileReader(schedule_done_file));
				ms_str = br.readLine();
				br.close();
			} catch ( Exception e ) {	// uups could not read it - locked file?
				try { br.close(); } catch ( Exception ebr ) {}
				return(0);
			}

			long millis = Long.valueOf(ms_str);
			CalLastExec.setTimeInMillis(millis);
		}
		else CalLastExec = null;

		// check week day
		if (weekdays.equals("") == false) {		// run on certain days only
			int dayofweek = now.get(Calendar.DAY_OF_WEEK);
			String dayofweek_str = "" + dayofweek;
			if (weekdays.indexOf(dayofweek_str) < 0) return(0);	// do not run it today
		}
		// check start date
		String now_year_str = "" + now.get(Calendar.YEAR);
		String now_month_str = "" + (now.get(Calendar.MONTH)+1); if (now_month_str.length() < 2) now_month_str = "0" + now_month_str;
		String now_day_str = "" + now.get(Calendar.DATE); if (now_day_str.length() < 2) now_day_str = "0" + now_day_str;
		String now_date_str = now_year_str + now_month_str + now_day_str;	// like 20070315
		int now_date =  Integer.valueOf(now_date_str);

		String start_year_str = "" + CalStartDate.get(Calendar.YEAR);
		String start_month_str = "" + (CalStartDate.get(Calendar.MONTH)+1); if (start_month_str.length() < 2) start_month_str = "0" + start_month_str;
		String start_day_str = "" + CalStartDate.get(Calendar.DATE); if (start_day_str.length() < 2) start_day_str = "0" + start_day_str;
		String start_date_str = start_year_str + start_month_str + start_day_str;	// like 20070315
		int start_date =  Integer.valueOf(start_date_str);
		
		if (now_date < start_date) return(0);	// not reached start date

		// check end date
		String end_year_str = "" + CalEndDate.get(Calendar.YEAR);
		String end_month_str = "" + (CalEndDate.get(Calendar.MONTH)+1); if (end_month_str.length() < 2) end_month_str = "0" + end_month_str;
		String end_day_str = "" + CalEndDate.get(Calendar.DATE); if (end_day_str.length() < 2) end_day_str = "0" + end_day_str;
		String end_date_str = end_year_str + end_month_str + end_day_str;	// like 20070315
		int end_date =  Integer.valueOf(end_date_str);
		
		if (now_date > end_date) return(0);	// beyond end date

		// check start time
		String now_hour_str = "" + now.get(Calendar.HOUR_OF_DAY); if (now_hour_str.length() < 2) now_hour_str = "0" + now_hour_str;
		String now_minute_str = "" + now.get(Calendar.MINUTE); if (now_minute_str.length() < 2) now_minute_str = "0" + now_minute_str;
		String now_second_str = "" + now.get(Calendar.SECOND); if (now_second_str.length() < 2) now_second_str = "0" + now_second_str;
		String now_time_str = now_hour_str + now_minute_str + now_second_str;	// like 032300
		int now_time =  Integer.valueOf(now_time_str);

		String start_hour_str = "" + CalStartDate.get(Calendar.HOUR_OF_DAY); if (start_hour_str.length() < 2) start_hour_str = "0" + start_hour_str;
		String start_minute_str = "" + CalStartDate.get(Calendar.MINUTE); if (start_minute_str.length() < 2) start_minute_str = "0" + start_minute_str;
		String start_second_str = "" + CalStartDate.get(Calendar.SECOND); if (start_second_str.length() < 2) start_second_str = "0" + start_second_str;
		String start_time_str = start_hour_str + start_minute_str + start_second_str;	// like 034500
		int start_time =  Integer.valueOf(start_time_str);
		
		if (now_time < start_time) return(0);	// not reached start time

		// check end time
		String end_hour_str = "" + CalEndDate.get(Calendar.HOUR_OF_DAY); if (end_hour_str.length() < 2) end_hour_str = "0" + end_hour_str;
		String end_minute_str = "" + CalEndDate.get(Calendar.MINUTE); if (end_minute_str.length() < 2) end_minute_str = "0" + end_minute_str;
		String end_second_str = "" + CalEndDate.get(Calendar.SECOND); if (end_second_str.length() < 2) end_second_str = "0" + end_second_str;
		String end_time_str = end_hour_str + end_minute_str + end_second_str;	// like 034500
		int end_time =  Integer.valueOf(end_time_str);
		
		if (now_time > end_time) return(0);		// beyond end time
		
		// check one shot tasks
		long last_ms = 0;
		long todayrun_ms = 0;
		if ((oneshot == true) && (CalLastExec != null)) {	// check if task already has run today
			// create new calendar when job should start TODAY
			Calendar scheduled_start_today = Calendar.getInstance();
			scheduled_start_today.setTimeInMillis(CalStartDate.getTimeInMillis());
			scheduled_start_today.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE));
			last_ms = CalLastExec.getTimeInMillis();
			todayrun_ms = scheduled_start_today.getTimeInMillis();
			if (last_ms >= todayrun_ms) return(0);	// we have already done it today
			//if (CalLastExec.after(today_start) == true) return(0);	// we have already done it today
		}
		
		// check repeated tasks
		if ((repeatseconds.equals("") == false) && (CalLastExec != null)) {	// check if task already has run today
			last_ms = CalLastExec.getTimeInMillis();
			long now_ms = now.getTimeInMillis();
			long repeat_ms = Long.valueOf(repeatseconds) * 1000;

			if (now_ms < (last_ms + repeat_ms)) return(0);	// not reached next interval - we must wait
		}

		// write done file for this jt containing millisecons since 1970
		String ms = "" + now.getTimeInMillis() + "\n" + now_date_str + " " + now_time_str + "\n";
		if (oneshot == true) ms += "oneshot == true\n";
		else ms += "oneshot == false\n";
		ms += "today scheduled for = " + todayrun_ms + "\n";
		ms += "      actual call   = " + now.getTimeInMillis() + "\n";
		ms += "previous call       = " + last_ms + "\n";
		int err = utils.writeFile(schedule_done_file, ms, true);

		return(1);	// run it!
	}



	// return path/name to job to run
	public String get_jobpathname () {
		String path = runpath;
		if ((path.length() > 0) && (path.endsWith(File.separator) == false))  path = path + File.separator;
		return(path + runname);
	}


}
