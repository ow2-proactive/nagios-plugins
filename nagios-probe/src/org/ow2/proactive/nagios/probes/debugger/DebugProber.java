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

package org.ow2.proactive.nagios.probes.debugger;

import org.ow2.proactive.nagios.common.Arguments;
import org.ow2.proactive.nagios.common.NagiosMiniStatus;
import org.ow2.proactive.nagios.common.NagiosReturnObject;
import org.ow2.proactive.nagios.common.NagiosReturnObjectSummaryMaker;
import org.ow2.proactive.nagios.common.PANagiosPlugin;
import org.ow2.proactive.nagios.common.TimedStatusTracer;
import org.ow2.proactive.nagios.probes.debugger.misc.JsonRestRMStatus;
import org.ow2.proactive.nagios.probes.rest.RestStubProber;
import org.ow2.proactive.resourcemanager.common.RMState;
//import qosprobercore.history.HistoryDataManager;

/** 
 * This is a general Nagios plugin class that performs a test on the RM and Scheduler seeking out potential bugs
 * that the team came across over time. It gets information from both entities and tells if there are incoherent 
 * status.
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class DebugProber extends PANagiosPlugin{
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this RMProber. 
	 * @throws Exception */
	public DebugProber(Arguments args) throws Exception{
		super("DEBUG", args);
		args.addNewOption("u", "user", true);							// User.
		args.addNewOption("p", "pass", true);							// Pass.
		args.addNewOption("r", "url-sched", true);						// Url of the Scheduler.
		args.addNewOption("R", "url-rm", true);							// Url of the RM.
		args.addNewOption("Z", "url-rest-rm", true);					// Url of the REST API RM.
		args.addNewOption("W", "history", true);						// History file. Ignored for now. 
	}
	
	/**
	 * Initialize the ProActive environment for this probe. */
	public void initializeProber() throws Exception{
		super.initializeProber();
		this.validateArguments(this.getArgs());
	}
	
	/** 
	 * Validate all the arguments given to this probe. 
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	private void validateArguments(Arguments arguments) throws IllegalArgumentException{
		arguments.checkIsGiven("url-sched");
		arguments.checkIsGiven("url-rest-rm");
		arguments.checkIsGiven("url-rm");
		arguments.checkIsGiven("user");
		arguments.checkIsGiven("pass");
		// arguments.checkIsGiven("history");
	}
	
	/**
	 * Probe both scheduler and RM to detect buggy situation.
	 * @throws Exception */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws Exception{
//		HistoryDataManager <HistoryElement> historyManager; 
//		try{
//			historyManager = new HistoryDataManager<HistoryElement>(getArgs().getStr("history"));
//		}catch(Exception e){
//			return new NagiosReturnObject(RESULT_3_UNKNOWN, "Problem locking history file... " + e.getMessage());
//		}
		
//		HistoryElement history = historyManager.get(new HistoryElement(0));

//		logger.info("History element value: " + history.getData());
//		historyManager.set(new HistoryElement(history.getData() + 1));
//		historyManager.release();
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		RMStubProber rmstub = new RMStubProber();					// We get connected to the RM through this stub. 
		//SchedulerStubProber schedstub = new SchedulerStubProber();	// We get connected to the Scheduler through this stub.
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection_both", "connecting...");
		rmstub.init(												// We get connected to the RM.
				getArgs().getStr("url-rm"),  getArgs().getStr("user"), 
				getArgs().getStr("pass"));	
		
		//schedstub.init(												// We get connected to the Scheduler.
		//		getArgs().getStr("url-sched"),  getArgs().getStr("user"), 
		//		getArgs().getStr("pass"));	
		RestStubProber restrm = new RestStubProber(true);
		restrm.connect(getArgs().getStr("url-rest-rm"));
		restrm.login(getArgs().getStr("user"), getArgs().getStr("pass"));
		String rmstatus =  restrm.get("/state");
		JsonRestRMStatus rmstatus2 = new JsonRestRMStatus(rmstatus);
		
		tracer.finishLastMeasurementAndStartNewOne("time_getting_rm_state", "getting RM state...");
		
		RMState rmstate = rmstub.getRMState();
		int freenodes = rmstate.getFreeNodesNumber();	
		int alivenodes = rmstate.getTotalAliveNodesNumber();	
		int busynodes = alivenodes - freenodes;	
		
		tracer.finishLastMeasurementAndStartNewOne("time_getting_sched_state", "getting scheduler state...");
		//SchedulerState schedstate = schedstub.getSchedulerState();
		//int runningjobsnumber = schedstate.getRunningJobs().size();	// Get the list of running jobs.
		//int pendingjobsnumber = schedstate.getPendingJobs().size();	// Get the list of pending jobs.
		
		tracer.finishLastMeasurementAndStartNewOne("time_disconnection", "disconnecting...");
		
    	rmstub.disconnect();										// Disconnect from the Resource Manager.
    	//schedstub.disconnect();										// Disconnect from the Scheduler.
    				
		tracer.finishLastMeasurement();
		
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		tracer.addNewReference("free_nodes", freenodes);
		tracer.addNewReference("alive_nodes", alivenodes);
		tracer.addNewReference("busy_nodes", busynodes);
		//tracer.addNewReference("running_jobs", runningjobsnumber);
		//tracer.addNewReference("running_jobs", pendingjobsnumber);
		summary.addFact("nodesalive=" + alivenodes + " nodesfreejava=" + freenodes +  " nodesfreerest=" + rmstatus2.getFreeNodes());
		
		summary.addMiniStatus(new NagiosMiniStatus(RESULT_0_OK, rmstatus));
		//if (runningjobsnumber == 0 && busynodes != 0){
		//	summary.addMiniStatus(new NagiosMiniStatus(RESULT_2_CRITICAL, "no jobs running but " + busynodes + " busy nodes (busy doing what?)"));
		//}
		
		if (freenodes != rmstatus2.getFreeNodes()){
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_2_CRITICAL, "inconsistent freeNodes number between RM and RM REST API, RMRestFree="+ rmstatus2.getFreeNodes()));
		}
		
		if (summary.isAllOkay() == true){
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_0_OK, "OK"));
		}
		
		return summary.getSummaryOfAll();
	}

	/**
	 * Starting point. */
	public static void main(String[] args) throws Exception{
        final Arguments options = new Arguments(args);
		DebugProber prob = new DebugProber(options);		// Create the prober.
		prob.initializeProber();
		prob.startProbeAndExit();							// Start the probe.
	}
}	
