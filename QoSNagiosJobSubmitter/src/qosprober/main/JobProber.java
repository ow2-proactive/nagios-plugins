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
import org.ow2.proactive.scheduler.examples.WaitAndPrint;
import qosprober.exceptions.InvalidProtocolException;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.PAEnvironmentInitializer;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.TimedStatusTracer;

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
	public static String expectedJobOutput;					// The job output that is expected. It is used to check the right execution of the job. 
	public static Logger logger = 
			Logger.getLogger(JobProber.class.getName()); 	// Logger.
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
	
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
        Option highpriorityO = 	new Option("z", "highpriority", false, ""); 	highpriorityO.setRequired(false); options.addOption(highpriorityO);

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
		ar.put("highpriority", new Boolean(parser.hasOption("z")));												// Set high priority for the job (not normal priority).

		Integer debug = (Integer)ar.get("debug");																// Debug level to be used.
		
		if ((Boolean)ar.get("help") == true)	
			NagiosPlugin.printMessageUsageAndExit("");
		
		if ((Boolean)ar.get("version") == true)
			NagiosPlugin.printVersionAndExit();
		
		Misc.log4jConfiguration(debug);						// Loading log4j configuration. 
		
		for (String key: ar.keySet())						// Show all the arguments considered. 
			logger.info("\t" + key + ":'" + ar.get(key) + "'");
		
		/* Loading job's expected output. */
		expectedJobOutput = Misc.readAllTextResource("/resources/expectedoutput.txt");
		
		PAEnvironmentInitializer.initPAConfiguration(ar);
		
		/* We prepare now our probe to run it in a different thread. The probe consists in a job submission done to the Scheduler. */
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		final TimedStatusTracer tracer = TimedStatusTracer.getInstance();	// We want to get last status memory, and timing measurements.
		
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double((Integer)ar.get("timeoutsec")));
		tracer.addNewReference("time_all_warning_threshold", new Double((Integer)ar.get("timeoutwarnsec")));

		Callable<NagiosReturnObject> proberCallable = new Callable<NagiosReturnObject>(){
			public NagiosReturnObject call() throws Exception {
				return JobProber.probe(ar, tracer);
			}
		};

		// We submit to the executor the prober activity (and the prober will then submit a job to the scheduler in that activity). 
		Future<NagiosReturnObject> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
	
		NagiosReturnObject res = null;
		try{ 								// We execute the future using a timeout. 
			res = proberFuture.get((Integer)ar.get("timeoutsec"), TimeUnit.SECONDS);
			res.appendCurvesSection(tracer.getMeasurementsSummary("time_all"));
		}catch(TimeoutException e){
			logger.info("Exception ", e); 	// The execution took more time than expected. 
			res = new NagiosReturnObject(
					NagiosReturnObject.RESULT_2_CRITICAL, "TIMEOUT OF "+(Integer)ar.get("timeoutsec")+ " SEC. (last status: " + tracer.getLastStatusDescription() + ")", e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}catch(ExecutionException e){ 		// There was a problem with the execution of the prober.
			logger.info("Exception ", e);
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "FAILURE: " + e.getMessage(), e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}catch(Exception e){ 				// There was an unexpected critical exception not captured. 
			logger.info("Exception ", e);
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}
		NagiosPlugin.printAndExit(res, debug);
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
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. */	 
	public static NagiosReturnObject probe(HashMap<String, Object> args, TimedStatusTracer tracer) throws IllegalArgumentException, LoginException, KeyException, ActiveObjectCreationException, NodeException, HttpException, SchedulerException, InvalidProtocolException, IOException, Exception{
		String jobname = (String)args.get("jobname");							// Name of the job to be submitted to the scheduler.
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		SchedulerStubProberJava schedulerstub = new SchedulerStubProberJava();	// We create directly the stub prober.
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection", "connecting to the scheduler...");
		
		schedulerstub.init(														// We get connected to the Scheduler.
				(String)args.get("url"), (String)args.get("user"), (String)args.get("pass"), (Boolean)args.get("polling"));	
		
		tracer.finishLastMeasurementAndStartNewOne("time_removing_old_jobs", "connected, removing old jobs...");	
		
		schedulerstub.removeOldProbeJobs(										// Removal of old probe jobs.
				jobname,(Boolean)args.get("deleteallold"));
		
		tracer.finishLastMeasurementAndStartNewOne("time_submission", "connected, submitting job...");
	
		String jobId = schedulerstub.submitJob(									// Submission of the job.
				jobname, JobProber.TASK_CLASS_NAME, (Boolean)args.get("highpriority"));	
		
		tracer.finishLastMeasurementAndStartNewOne("time_execution", "job " + jobId + " submitted, waiting for its execution...");
		
		schedulerstub.waitUntilJobFinishes(jobId); 								// Wait for the job to finish.
		
		tracer.finishLastMeasurementAndStartNewOne("time_retrieval", "job " + jobId + " executed, getting its output...");
		
		String jresult = schedulerstub.getJobResult(jobId); 					// Getting the result of the submitted job.
		
		tracer.finishLastMeasurementAndStartNewOne("time_removal", "output obtained, removing job...");
	
		schedulerstub.removeJob(jobId);											// Job removed from the list of jobs in the Scheduler.
		
		tracer.finishLastMeasurementAndStartNewOne("time_disconn", "job removed, disconnecting...");
		
		schedulerstub.disconnect();												// Getting disconnected from the Scheduler.
		
		tracer.finishLastMeasurement();
	
		NagiosReturnObject ret = new NagiosReturnObject(
				NagiosReturnObject.RESULT_2_CRITICAL, "NO TEST PERFORMED"); // Default output (for Nagios).
		
		if (jresult==null){ 		// No job result obtained... It must never happen, but we check just in case.
			logger.info("Finished job  " + jobname + ":" + jobId + ". Result: NOT FINISHED");
			ret = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "JOBID " + jobId + " ERROR (no job result obtained)");
		}else{ 						// Non-timeout case. Result obtained.
			logger.info("Finished job  " + jobname + ":" + jobId + ". Result: '" + jresult.toString() + "'.");
			
			if (jresult.toString().equals(expectedJobOutput)){ 		// Checked file, all OK.
				if (tracer.getTotal() > (Integer)args.get("timeoutwarnsec")){
					ret = new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING, "JOBID " + jobId + " TOO SLOW");
				}else{
					ret = new NagiosReturnObject(NagiosReturnObject.RESULT_0_OK, "JOBID " + jobId + " OK");
				}
			}else{ 														// Outputs were different. 
				ret = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "JOBID " + jobId + " OUTPUT CHECK FAILED");
			}
		}
		return ret;
	}
}
//controlar cada uno de los casos y los timeouts
 //si se puede controlar que para ver la version nada va a funcionar (ver el -V)