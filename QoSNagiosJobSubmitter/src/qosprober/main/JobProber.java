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

import qosprobercore.misc.Misc;

import org.ow2.proactive.resourcemanager.common.RMState;
import org.ow2.proactive.scheduler.examples.WaitAndPrint;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosMiniStatus;
import qosprobercore.main.PANagiosPlugin;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.TimedStatusTracer;

/** 
 * This is a general Nagios plugin class that performs a test on the scheduler, by doing:
 *    -Job submission
 *    -Job result retrieval
 *    -Job result comparison 
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class JobProber extends PANagiosPlugin{

	public static final String JOB_NAME_DEFAULT = 
		"nagios_plugin_probe_job";					// Name of the probe job in the Scheduler, as the administrator will see it.
	public static final String TASK_CLASS_NAME = 
		WaitAndPrint.class.getName();				// Class to be instantiated and executed as a task in the Scheduler.
	public static String expectedJobOutput;			// The job output that is expected. It is used to check the right execution of the job. 
	
	private RMStateGetter rmStateGetter; 			// Used to find out the number of free nodes, and prevent telling critical
													// errors because of timeout when actually there are no free nodes but everything is okay.
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this JobProber. 
	 * @throws Exception */
	public JobProber(Arguments args) throws Exception{
		super("SCHEDULER", args);
		args.addNewOption("u", "user", true);			// User.
		args.addNewOption("p", "pass", true);			// Pass.
		args.addNewOption("r", "url", true);			// Url of the Scheduler/RM.
		args.addNewOption("j", "jobname", true, 
				JOB_NAME_DEFAULT);						// Name used to run the job in the Scheduler. 
		args.addNewOption("d", "deleteallold", false);	// Delete all old jobs, not only the ones with the name 
		args.addNewOption("g", "polling", false);		// Do polling or use an event based mechanism.
		args.addNewOption("z", "highpriority", false);	// Set high priority for the job (not normal priority).
		args.addNewOption("R", "rm-checking", false);	// Performs extra checking on the RM to know availability of nodes and give more accurate results. 
	}
	
	/**
	 * Initialize the ProActive environment for this probe. */
	public void initializeProber() throws Exception{
		super.initializeProber();
		/* Loading job's expected output. */
		expectedJobOutput = Misc.readAllTextResource("/resources/expectedoutput.txt");
		if (getArgs().getBoo("rm-checking") == true){
			rmStateGetter = new RMStateGetter(
				getArgs().getStr("url"), 
				getArgs().getStr("user"), getArgs().getStr("pass"));
		}
	}

	/** 
	 * Validate all the arguments given to this probe. 
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	public void validateArguments(Arguments arguments) throws IllegalArgumentException{
		super.validateArguments(arguments);
		arguments.checkIsGiven("url");
		arguments.checkIsGiven("user");
		arguments.checkIsGiven("pass");
	}
	
	/**
	 * Probe the scheduler
	 * Several calls are done against the scheduler: join, remove old jobs, submit job, get job status (based on 
	 * polling or in events, depends on the protocol chosen), get job result, remove job, disconnect.
	 * After a correct disconnection call, the output of the job is compared with a given correct output, and the 
	 * result of the test is told. 
	 * @param tracer tracer that lets keep track of the last status, and the time each call took to be executed.
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws Exception{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(getArgs().getInt("critical")));
		if (getArgs().isGiven("warning")==true) // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(getArgs().getInt("warning")));
		
	
		String jobname = getArgs().getStr("jobname");							// Name of the job to be submitted to the scheduler.
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		SchedulerStubProberJava schedulerstub = new SchedulerStubProberJava();	// We create directly the stub prober.
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection", "connecting to the scheduler...");
		
		schedulerstub.init(														// We get connected to the Scheduler.
				getArgs().getStr("url"),  getArgs().getStr("user"), 
				getArgs().getStr("pass"), getArgs().getBoo("polling"));	
		
		tracer.finishLastMeasurementAndStartNewOne("time_removing_old_jobs", "connected, removing old jobs...");	
		
		schedulerstub.removeOldProbeJobs(										// Removal of old probe jobs.
				jobname,getArgs().getBoo("deleteallold"));
		
		tracer.finishLastMeasurementAndStartNewOne("time_submission", "connected, submitting job...");
	
		String jobId = schedulerstub.submitJob(									// Submission of the job.
				jobname, JobProber.TASK_CLASS_NAME, getArgs().getBoo("highpriority"));	
		
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
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_2_CRITICAL, "NO JOB RESULT OBTAINED"));
		}else{ 						// Non-timeout case. Result obtained.
			logger.info("Finished job  " + jobname + ":" + jobId + ". Result: '" + jresult.toString() + "'.");
			if (jresult.toString().equals(expectedJobOutput) == false) 
				summary.addMiniStatus(new NagiosMiniStatus(RESULT_2_CRITICAL, "OUTPUT CHECK FAILED"));
		}	
		
		if (getArgs().isGiven("warning") && tracer.getTotal() > getArgs().getInt("warning"))
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_1_WARNING, "TOO SLOW"));
		
		if (summary.isAllOkay())
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_0_OK, "OK"));
		
		return summary.getSummaryOfAllWithTimeAll(tracer);
	}
	
	
	/**
	 * We rewrite the method since the output depends on whether we haver or not some RM results. */
	protected NagiosReturnObject getNagiosReturnObjectForTimeoutException(Integer timeout, TimedStatusTracer tracer, Exception e){
		NagiosReturnObject ret;
		
		if (getArgs().getBoo("rm-checking") == true){ 	// Checking of the RM activated.
			RMState state = rmStateGetter.getQueryResult();
			if (state == null){									// We still do not have any result.
				ret = new NagiosReturnObject(RESULT_2_CRITICAL, "FREE RM NODES: UNKNOWN, TIMEOUT OF " + getArgs().getInt("critical")+ " SEC. (last status: " + tracer.getLastStatusDescription() + ")", e);
			}else{												// We already have a result.
				Integer freenodes = state.getFreeNodesNumber();
				logger.info("RM Free nodes: " + freenodes);
				if (freenodes == 0){							
					ret = new NagiosReturnObject(RESULT_3_UNKNOWN, "NO FREE RM NODES, TIMEOUT OF " + getArgs().getInt("critical") + " SEC. (last status: " + tracer.getLastStatusDescription() + ")", e);
				}else{
					ret = new NagiosReturnObject(RESULT_2_CRITICAL, "RM FREE RM NODES: " + freenodes + ", TIMEOUT OF " + getArgs().getInt("critical")+ " SEC. (last status: " + tracer.getLastStatusDescription() + ")", e);
				}
			}
		}else{											// Checking of the RM deactivated.
			ret = new NagiosReturnObject(RESULT_2_CRITICAL, "TIMEOUT OF " + getArgs().getInt("critical")+ " SEC. (last status: " + tracer.getLastStatusDescription() + ")", e);
		}
		ret.addCurvesSection(tracer, null);
		return ret;
	}
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
        Arguments options = new Arguments(args);
		JobProber prob = new JobProber(options);													// Create the prober.
		prob.initializeProber();
		prob.startProbeAndExit();																	// Start the probe.
	}
}
