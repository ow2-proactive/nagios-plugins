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
import java.util.Arrays;
import java.util.concurrent.*;
import javax.security.auth.login.LoginException;
import qosprobercore.misc.Misc;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.examples.WaitAndPrint;
import qosprobercore.exceptions.InvalidProtocolException;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.PAEnvironmentInitializer;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.TimedStatusTracer;

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
	public static Logger logger = 
			Logger.getLogger(JobProber.class.getName()); 	// Logger.
	public static String expectedJobOutput;					// The job output that is expected. It is used to check the right execution of the job. 
	
	private Arguments arguments; 							// Arguments given to the prober. 
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this JobProber. */
	public JobProber(Arguments args){
		this.arguments = args;
	}
	
	/**
	 * Initialize the ProActive environment for this probe. */
	public void initializeEnvironment() throws Exception{
		Misc.log4jConfiguration(arguments.getInt("debug"));						// Loading log4j configuration. 
		/* Loading job's expected output. */
		expectedJobOutput = Misc.readAllTextResource("/resources/expectedoutput.txt");
		PAEnvironmentInitializer.initPAConfiguration(
				arguments.getStr("paconf"),
				arguments.getStr("hostname"),
				arguments.getStr("port"));
	}

	/** 
	 * Validate all the arguments given to this probe. 
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	public void validateArguments() throws IllegalArgumentException{
		String[] notnull1 = {"url", "user", "pass", "critical"}; // These arguments should not be null.
		Misc.allElementsAreNotNull(Arrays.asList(notnull1), arguments);
		Integer debug = arguments.getInt("debug");
		if (debug<0 || debug>3)
			throw new IllegalArgumentException("The argument 'v' must be 0, 1, 2 or 3.");
	}
	
	/**
	 * Probe the scheduler
	 * Several calls are done against the scheduler: join, remove old jobs, submit job, get job status (based on 
	 * polling or in events, depends on the protocol chosen), get job result, remove job, disconnect.
	 * After a correct disconnection call, the output of the job is compared with a given correct output, and the 
	 * result of the test is told. 
	 * @param tracer tracer that lets keep track of the last status, and the time each call took to be executed.
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws IllegalArgumentException, LoginException, KeyException, ActiveObjectCreationException, NodeException, HttpException, SchedulerException, InvalidProtocolException, IOException, Exception{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(arguments.getInt("critical")));
		if (arguments.getBoo("warning")==true){ // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(arguments.getInt("warning")));
		}
		
		
		String jobname = arguments.getStr("jobname");							// Name of the job to be submitted to the scheduler.
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		SchedulerStubProberJava schedulerstub = new SchedulerStubProberJava();	// We create directly the stub prober.
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection", "connecting to the scheduler...");
		
		schedulerstub.init(														// We get connected to the Scheduler.
				arguments.getStr("url"),  arguments.getStr("user"), 
				arguments.getStr("pass"), arguments.getBoo("polling"));	
		
		tracer.finishLastMeasurementAndStartNewOne("time_removing_old_jobs", "connected, removing old jobs...");	
		
		schedulerstub.removeOldProbeJobs(										// Removal of old probe jobs.
				jobname,arguments.getBoo("deleteallold"));
		
		tracer.finishLastMeasurementAndStartNewOne("time_submission", "connected, submitting job...");
	
		String jobId = schedulerstub.submitJob(									// Submission of the job.
				jobname, JobProber.TASK_CLASS_NAME, arguments.getBoo("highpriority"));	
		
		tracer.finishLastMeasurementAndStartNewOne("time_execution", "job " + jobId + " submitted, waiting for its execution...");
		
		schedulerstub.waitUntilJobFinishes(jobId); 								// Wait for the job to finish.
		
		tracer.finishLastMeasurementAndStartNewOne("time_retrieval", "job " + jobId + " executed, getting its output...");
		
		String jresult = schedulerstub.getJobResult(jobId); 					// Getting the result of the submitted job.
		
		tracer.finishLastMeasurementAndStartNewOne("time_removal", "output obtained, removing job...");
	
		schedulerstub.removeJob(jobId);											// Job removed from the list of jobs in the Scheduler.
		
		tracer.finishLastMeasurementAndStartNewOne("time_disconn", "job removed, disconnecting...");
		
		schedulerstub.disconnect();												// Getting disconnected from the Scheduler.
		
		tracer.finishLastMeasurement();
	
		
		
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		summary.addFact("JOBID " + jobId + ":" + jobname);
		
		if (jresult==null){ 		// No job result obtained... It must never happen, but we check just in case.
			logger.info("Finished job  " + jobname + ":" + jobId + ". Result: NO OUTPUT");
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "NO JOB RESULT OBTAINED"));
		}else{ 						// Non-timeout case. Result obtained.
			logger.info("Finished job  " + jobname + ":" + jobId + ". Result: '" + jresult.toString() + "'.");
			if (jresult.toString().equals(expectedJobOutput) == false){ 
				summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "OUTPUT CHECK FAILED"));
			}
		}	
		
		if (arguments.isGiven("warning") && tracer.getTotal() > arguments.getInt("warning")){
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING, "TOO SLOW"));
		}
		
		if (summary.isAllOkay()){
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_0_OK, "OK"));
		}
		
		return summary.getSummaryOfAll();
	}
	
	
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
        final Arguments options = new Arguments(args);
		/* Parsing of arguments. */
		// short, long, hasargument, description
		options.addNewOption("h", "help", false);													// Help message.                                	
		options.addNewOption("V", "version", false);                                                // Prints the version of the plugin.
		options.addNewOption("v", "debug", true, new Integer(NagiosPlugin.DEBUG_LEVEL_1_EXTENDED)); // Level of verbosity.
		options.addNewOption("w", "warning", true);                                                 // Timeout in seconds for the warning message to be thrown.
		options.addNewOption("c", "critical", true);                                                // Timeout in seconds for the job to be executed.
                                                                                                                                                                                                                                        
		options.addNewOption("u", "user", true);													// User.
		options.addNewOption("p", "pass", true);                                                    // Pass.
		options.addNewOption("r", "url", true);                                                     // Url of the Scheduler/RM.
		options.addNewOption("f", "paconf", true);                                                  // Path of the ProActive xml configuration file.
		options.addNewOption("H", "hostname", true);                                                // Host to be tested. 
		options.addNewOption("x", "port"    , true);                                                // Port of the host to be tested. 
		
		options.addNewOption("j", "jobname", true);                                                 // Name used to run the job in the Scheduler. 
		options.addNewOption("d", "deleteallold", false);                                           // Delete all old jobs, not only the ones with the name 
		options.addNewOption("g", "polling", false);                                                // Do polling or use an event based mechanism.
		options.addNewOption("z", "highpriority", false);                                           // Set high priority for the job (not normal priority).

		options.parseAll();

		if (options.getBoo("help") == true)	
			NagiosPlugin.printMessageUsageAndExit("");
		
		if (options.getBoo("version") == true)
			NagiosPlugin.printVersionAndExit();

		
		final JobProber jobp = new JobProber(options);		// Create the prober.
		
		jobp.validateArguments();							// Validate its arguments. In case of problems, it throws an IllegalArgumentException.
	
		jobp.initializeEnvironment();						// Initializes the environment for ProActive objects and prober.
		
		/* We prepare now our probe to run it in a different thread. The probe consists in a job submission done to the Scheduler. */
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		final TimedStatusTracer tracer = TimedStatusTracer.getInstance();	// We want to get last status memory, and timing measurements.
		

		Callable<NagiosReturnObject> proberCallable = new Callable<NagiosReturnObject>(){
			public NagiosReturnObject call() throws Exception {
				return jobp.probe(tracer);
			}
		};

		// We submit to the executor the prober activity (and the prober will then submit a job to the scheduler in that activity). 
		Future<NagiosReturnObject> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
	
		NagiosReturnObject res = null;
		try{ 								// We execute the future using a timeout. 
			res = proberFuture.get(options.getInt("critical"), TimeUnit.SECONDS);
			res.addCurvesSection(tracer, "time_all");
		}catch(TimeoutException e){
			logger.info("Exception ", e); 	// The execution took more time than expected. 
			res = new NagiosReturnObject(
					NagiosReturnObject.RESULT_2_CRITICAL, "TIMEOUT OF "+options.getInt("critical")+ " SEC. (last status: " + tracer.getLastStatusDescription() + ")", e);
			res.addCurvesSection(tracer, null);
		}catch(ExecutionException e){ 		// There was a problem with the execution of the prober.
			logger.info("Exception ", e);
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "FAILURE: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}catch(Exception e){ 				// There was an unexpected critical exception not captured. 
			logger.info("Exception ", e);
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}
		NagiosPlugin.printAndExit(res, options.getInt("debug"));
	}
}
