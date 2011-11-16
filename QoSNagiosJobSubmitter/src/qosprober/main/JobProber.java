/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */

package qosprober.main;

import java.io.IOException;
import java.security.KeyException;
import java.util.HashMap;
import java.util.concurrent.*;
import javax.security.auth.login.LoginException;
import qosprobercore.misc.Misc;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.examples.WaitAndPrint;
import qosprober.exceptions.InvalidProtocolException;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.PAEnvironmentInitializer;
import qosprobercore.main.NagiosReturnObject;
import org.apache.commons.cli.*;

/** 
 * This is a general Nagios plugin class that performs a test on the scheduler, by doing:
 *    -Job submission
 *    -Job result retrieval
 *    -Job result comparison 
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class JobProber {

	public static final String JOB_NAME_DEFAULT = 
		"nagios_plugin_probe_job";							// Name of the probe job in the Scheduler, as the administrator will see it.
	public static final String TASK_CLASS_NAME = 
		WaitAndPrint.class.getName();						// Class to be instantiated and executed as a task in the Scheduler.
	public static String EXPECTED_JOB_OUTPUT;				// The job output that is expected. It is used to check the right execution of the job. 
	public static JobPriority DEFAULT_JOB_PRIORITY = 
		JobPriority.NORMAL;									// Priority of the probe job used for the test. 
	private static String lastStatus;						// Holds a message representative of the current status of the test.
															// It is used in case of TIMEOUT, to help the administrator guess
															// where the problem is.
	public static Logger logger = 
			Logger.getLogger(JobProber.class.getName()); 	// Logger.
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
	
		JobProber.setLastStatuss("started, parsing arguments and basic initialization...");
		
		/* Parsing of arguments. */
		Options options = new Options();
		// short, long, hasargument, description
        Option helpO =			new Option("h", "help", false, "");				helpO.setRequired(false); options.addOption(helpO);
        Option debugO =			new Option("v", "debug", true, ""); 			debugO.setRequired(false); options.addOption(debugO);
        Option userO = 			new Option("u", "user", true, ""); 				userO.setRequired(true); options.addOption(userO);
        Option passO = 			new Option("p", "pass", true, ""); 				passO.setRequired(true); options.addOption(passO);
        Option urlO = 			new Option("r", "url", true, ""); 				urlO.setRequired(false); options.addOption(urlO);
        Option timeoutsecO = 	new Option("t", "timeout", true, "");			timeoutsecO.setRequired(true); options.addOption(timeoutsecO);
        Option timeoutwarnsecO =new Option("n", "timeoutwarning", true, ""); 	timeoutwarnsecO.setRequired(false); options.addOption(timeoutwarnsecO);
        Option paconfO = 		new Option("f", "paconf", true, ""); 			paconfO.setRequired(false); options.addOption(paconfO);
        Option hostO = 			new Option("H", "hostname", true, "");			hostO.setRequired(false); options.addOption(hostO);
        Option portO = 			new Option("x", "port"    , true, "");			portO.setRequired(false); options.addOption(portO);
        Option warningO = 		new Option("w", "warning", true, ""); 			warningO.setRequired(false); options.addOption(warningO);
        Option criticalO = 		new Option("c", "critical", true, "");			criticalO.setRequired(false); options.addOption(criticalO);
        Option jobnameO = 		new Option("j", "jobname", true, ""); 			jobnameO.setRequired(false); options.addOption(jobnameO);
        Option deletealloldO = 	new Option("d", "deleteallold", false, ""); 	deletealloldO.setRequired(false); options.addOption(deletealloldO);
        Option pollingO = 		new Option("g", "polling", false, ""); 			pollingO.setRequired(false); options.addOption(pollingO);
        Option versionO = 		new Option("V", "version", false, ""); 			versionO.setRequired(false); options.addOption(versionO);

        CommandLine parser = null;
        try{
	        Parser parserrr = new GnuParser();
	        parser = parserrr.parse(options, args);
        }catch(org.apache.commons.cli.MissingOptionException ex){
	        NagiosPlugin.printMessageUsageAndExit(ex.getMessage());	
        }

        final HashMap<String, Object> ar = new HashMap<String, Object>();
		ar.put("help", new Boolean(parser.hasOption("h")));														// Help message.
		ar.put("debug", Misc.parseInteger(parser.getOptionValue("v"), NagiosPlugin.DEBUG_LEVEL_1_EXTENDED));	// Level of verbosity.
		ar.put("user", (String)parser.getOptionValue("u"));			 											// User.
		ar.put("pass", (String)parser.getOptionValue("p")); 													// Pass.
		ar.put("url", (String)parser.getOptionValue("r")); 														// Url of the Scheduler/RM.
		ar.put("timeoutsec", Misc.parseInteger(parser.getOptionValue("t"), null));								// Timeout in seconds for the job to be executed.
		ar.put("timeoutwarnsec", Misc.parseInteger(parser.getOptionValue("n"),(Integer)ar.get("timeoutsec")));	// Timeout in seconds for the warning message to be thrown.
		ar.put("paconf", (String)parser.getOptionValue("f")); 													// Path of the ProActive xml configuration file.
		ar.put("host", (String)parser.getOptionValue("H"));						 								// Host to be tested. 
		ar.put("port", (String)parser.getOptionValue("x"));														// Port of the host to be tested. 
		ar.put("warning", (String)parser.getOptionValue("w", "ignored"));										// Warning level. Ignored.
		ar.put("critical", (String)parser.getOptionValue("c", "ignored")); 										// Critical level. Ignored. 
		ar.put("jobname", (String)parser.getOptionValue("j", JobProber.JOB_NAME_DEFAULT));						// Name used to run the job in the Scheduler. 
		ar.put("deleteallold", new Boolean(parser.hasOption("d")));												// Delete all old jobs, not only the ones with the name of the current probe job.
		ar.put("polling", new Boolean(parser.hasOption("g")));													// Do polling or use an event based mechanism.
		ar.put("version", new Boolean(parser.hasOption("V")));													// Prints the version of the plugin.
	
		if ((Boolean)ar.get("help") == true){	
			NagiosPlugin.printMessageUsageAndExit("");
		}
		if ((Boolean)ar.get("version") == true){
			NagiosPlugin.printVersionAndExit();
		}
		
		/* Loading log4j configuration. */
		Misc.log4jConfiguration((Integer)ar.get("debug"));
		
		/* Show all the arguments considered. */
		
		for (Object key: ar.keySet()){
			logger.info("\t" + key + ": " + ar.get(key.toString()));
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		System.exit(0);
		
		/* Loading job's expected output. */
		EXPECTED_JOB_OUTPUT = Misc.readAllTextResource("/resources/expectedoutput.txt");
		
		PAEnvironmentInitializer.initPAConfiguration(ar);
		JobProber.setLastStatuss("basic initialization done, initializing probe module...");
		
		/* We prepare now our probe to run it in a different thread. */
		/* The probe consists in a job submission done to the Scheduler. */
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		Callable<Object[]> proberCallable = new Callable<Object[]>(){
			public Object[] call() throws Exception {
				return JobProber.probe(ar);
			}
		};

		/* We submit to the executor the prober activity (and the prober will then 
		 * submit a job to the scheduler in that activity). */
		Future<Object[]> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
		
		try{
			/* We execute the future using a timeout. */
			Object[] res = proberFuture.get((Integer)ar.get("timeoutsec"), TimeUnit.SECONDS);
			/* At this point all went okay. */ 
			NagiosPlugin.printAndExit((Integer)res[0], (String)res[1]);
		}catch(TimeoutException e){
			logger.info("Exception ", e);
			/* The execution took more time than expected. */
			NagiosPlugin.printAndExit(
					NagiosReturnObject.RESULT_2_CRITICAL, 
					"TIMEOUT OF "+(Integer)ar.get("timeoutsec")+ " SEC. (last status was: " + JobProber.getLastStatus() + ")", 
					(Integer)ar.get("debug"),
					e);
		}catch(ExecutionException e){
			/* There was a problem with the execution of the prober. */
			logger.info("Exception ", e);
			NagiosPlugin.printAndExit(
					NagiosReturnObject.RESULT_2_CRITICAL, 
					"FAILURE: " + e.getMessage(),
					(Integer)ar.get("debug"),
					e);
		}catch(Exception e){
			/* There was an unexpected critical exception not captured. */
			logger.info("Exception ", e);
			NagiosPlugin.printAndExit(
					NagiosReturnObject.RESULT_2_CRITICAL, 
					"CRITICAL ERROR: " + e.getMessage(),
					(Integer)ar.get("debug"),
					e);
		}
	}
	
	
	/**
	 * Probe the scheduler
	 * Several calls are done against the scheduler:
	 *   - join
	 *   - remove old jobs
	 *   - submit job
	 *   - get job status (based on polling or in events, depends on the protocol chosen)
	 *   - get job result
	 *   - remove job
	 *   - disconnect
	 *  After a correct disconnection call, the output of the job is compared with a 
	 *  given correct output, and the result of the test is told. 
	 * @return Object[Integer, String] with Nagios code error and a descriptive message of the test. */	 
	public static Object[] probe(HashMap<String, Object> args) throws IllegalArgumentException, LoginException, KeyException, ActiveObjectCreationException, NodeException, HttpException, SchedulerException, InvalidProtocolException, IOException, Exception{
		String jobname = (String)args.get("jobname");
		TimeTick timer = new TimeTick(); // We want to get time durations.
		
		/* We get connected to the Scheduler through this stub, later we submit a job, etc. */
		
		SchedulerStubProberJava schedulerstub = new SchedulerStubProberJava();		// We create directly the stub prober.
		double time_initializing = timer.tickSec();
		
		JobProber.setLastStatuss("scheduler stub created, connecting to shceduler...");
		
		// Connection with the scheduler. 
		schedulerstub.init((String)args.get("url"), (String)args.get("user"), (String)args.get("pass"), (Boolean)args.get("polling"));						// Login procedure...
		JobProber.setLastStatuss("connected to scheduler, removing old jobs...");
		
		double time_connection = timer.tickSec();
		
		// Removal of old probe jobs. 
		schedulerstub.removeOldProbeJobs(jobname, (Boolean)args.get("deleteallold"));
		
		double time_removing_old_jobs = timer.tickSec();
		JobProber.setLastStatuss("removed old jobs, submitting job...");
	
		// Job submission, wait for execution, and output retrieval. 
		
		int output_to_return = NagiosReturnObject.RESULT_2_CRITICAL; 
		String output_to_print = "NO TEST PERFORMED"; 			// Default output (for Nagios).
		
		String jobId = schedulerstub.submitJob(
				jobname, JobProber.TASK_CLASS_NAME); 			// Submission of the job.
		
		JobProber.setLastStatuss("job "+jobId+" submitted, waiting for it...");
		
		double time_submission = timer.tickSec();
		
		schedulerstub.waitUntilJobFinishes(jobId); 				// Wait for the job to finish.
		
		JobProber.setLastStatuss("job "+jobId+" finished, getting its result...");
		
		double time_execution = timer.tickSec();
		
		String jresult = schedulerstub.getJobResult(jobId); 	// Getting the result of the submitted job.
		
		JobProber.setLastStatuss("job "+jobId+" result retrieved, removing job from scheduler...");
		
		double time_retrieval = timer.tickSec();
	
		// Removal of the probe job from the scheduler. 
		schedulerstub.removeJob(jobId);							// Job removed from the list of jobs in the Scheduler.
		
		double time_removal = timer.tickSec();
		
		JobProber.setLastStatuss("job "+jobId+" removed from scheduler, disconnecting...");
	
		// Disconnection from the scheduler. 
		
		schedulerstub.disconnect();								// Getting disconnected from the Scheduler.
		
		JobProber.setLastStatuss("disconnected from scheduler, checking job result...");
		
		double time_disconn = timer.tickSec();
		
		double time_all = time_initializing+time_connection+time_removing_old_jobs+time_submission+time_execution+time_retrieval+time_removal+time_disconn;
		
		String timesummary = "";
		
/*		String timesummary =
			"time_initialization=" + String.format(Locale.ENGLISH, "%1.03f", time_initializing) + "s " +
			"time_connection=" + String.format(Locale.ENGLISH, "%1.03f", time_connection)   + "s " +
			"time_cleaning_old_jobs=" + String.format(Locale.ENGLISH, "%1.03f", time_removing_old_jobs) + "s " +
			"time_submission=" + String.format(Locale.ENGLISH, "%1.03f", time_submission)   + "s " + 
			"time_execution=" + String.format(Locale.ENGLISH, "%1.03f", time_execution )   + "s " + 
			"time_output_retrieval=" + String.format(Locale.ENGLISH, "%1.03f", time_retrieval )   + "s " +
			"time_job_removal=" + String.format(Locale.ENGLISH, "%1.03f", time_removal   )   + "s " + 
			"time_disconnection=" + String.format(Locale.ENGLISH, "%1.03f", time_disconn   )   + "s " +
			"timeout_threshold=" + String.format(Locale.ENGLISH, "%1.03f", (float)timeoutsec)   + "s " +
			"time_all_warning_threshold=" + String.format(Locale.ENGLISH, "%1.03f", (float)timeoutwarnsec)   + "s " +
			"time_all="   + String.format(Locale.ENGLISH, "%1.03f", time_all) + "s"; 
*/		
				
		logger.info("Checking output...");
		if (jresult==null){ 		// No job result obtained... It must never happen, but we check just in case.
			logger.info("Finished job  " + jobname + ":" + jobId + ". Result: NOT FINISHED");
			output_to_print = "JOBID " + jobId + " ERROR (no job result obtained)";
			output_to_return = NagiosReturnObject.RESULT_2_CRITICAL;
		}else{ 						// Non-timeout case. Result obtained.
			logger.info("Finished job  " + jobname + ":" + jobId + ". Result: '" + jresult.toString() + "'.");
			
//			try { 
//				/* To have a better checking of the output of the job, just remove the extension '.tmp' of this output, (remaining a '.out' file).
//				 * This '.out' file is the file used to match the output of the job.
//				 */
//				String ppath = "/tmp/output";
//				logger.info("Writing output in '" + ppath + "'...");
//				Misc.writeAllFile(ppath, jresult.toString());
//				logger.info("Done.");
//			} catch (Exception e1) {
//				logger.warn("Could not write the output of the process.", e1);
//			}
//			
			if (jresult.toString().equals(EXPECTED_JOB_OUTPUT)){ 		// Checked file, all OK.
				if (time_all > (Integer)args.get("timeoutwarnsec")){
					output_to_return = NagiosReturnObject.RESULT_1_WARNING;
					output_to_print = "JOBID " + jobId + " TOO SLOW | " + timesummary;
				}else{
					output_to_return = NagiosReturnObject.RESULT_0_OK;
					output_to_print = "JOBID " + jobId + " OK | " + timesummary;
				}
			}else{ 														// Outputs were different. 
				output_to_return = NagiosReturnObject.RESULT_2_CRITICAL;
				output_to_print = "JOBID " + jobId + " OUTPUT CHECK FAILED | " + timesummary;
			}
		}
		
		Object [] ret = new Object[2];							// Both, error code and message are returned to be shown.
		ret[0] = new Integer(output_to_return);
		ret[1] = output_to_print;
			
		return ret;
	}
		
	/** 
	 * Save a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. */
	public synchronized static void setLastStatuss(String laststatus){
		JobProber.lastStatus = laststatus;
	}
	
	/** 
	 * Get a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. 
	 * @return the last status of the test. */
	public synchronized static String getLastStatus(){
		return JobProber.lastStatus;
	}
}
