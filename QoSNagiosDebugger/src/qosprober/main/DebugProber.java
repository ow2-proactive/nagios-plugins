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

import org.ow2.proactive.resourcemanager.common.RMState;

import qosprobercore.history.HistoryDataManager;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosMiniStatus;
import qosprobercore.main.PANagiosPlugin;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.TimedStatusTracer;

/** 
 * This is a general Nagios plugin class that performs a test on the RM, by doing:
 *    -Node obtaining
 *    -Node retrieval 
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
		args.addNewOption("W", "history", true);						// History file.
		
	}
	
	/**
	 * Initialize the ProActive environment for this probe. */
	public void initializeProber() throws Exception{
		super.initializeProber();
	}
	
	/** 
	 * Validate all the arguments given to this probe. 
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	public void validateArguments(Arguments arguments) throws IllegalArgumentException{
		super.validateArguments(arguments);
		arguments.checkIsGiven("url-sched");
		arguments.checkIsGiven("url-rm");
		arguments.checkIsGiven("user");
		arguments.checkIsGiven("pass");
		arguments.checkIsGiven("history");
	}
	
	/**
	 * Probe both scheduler and RM to detect buggy situation.
	 * @throws Exception */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws Exception{
		HistoryDataManager <HistoryElement> historyManager; 
		try{
			historyManager = new HistoryDataManager<HistoryElement>(getArgs().getStr("history"));
		}catch(Exception e){
			return new NagiosReturnObject(RESULT_3_UNKNOWN, "Problem locking history file... " + e.getMessage());
		}
		
		HistoryElement history = historyManager.get(new HistoryElement(0));

		logger.info("History element value: " + history.getData());
		historyManager.set(new HistoryElement(history.getData() + 1));
		historyManager.release();
		
		RMStubProber rmstub = new RMStubProber();						// We get connected to the RM through this stub. 
		SchedulerStubProber schedstub = new SchedulerStubProber();		// We get connected to the Scheduler through this stub.
		
		rmstub.init(										// We get connected to the RM.
				getArgs().getStr("url-rm"),  getArgs().getStr("user"), 
				getArgs().getStr("pass"));	
		
		schedstub.init(										// We get connected to the Scheduler.
				getArgs().getStr("url-sched"),  getArgs().getStr("user"), 
				getArgs().getStr("pass"));	
		
		RMState rmstate = rmstub.getRMState();
		int freenodes = rmstate.getFreeNodesNumber();	
		int alivenodes = rmstate.getTotalAliveNodesNumber();	
		int busynodes = alivenodes - freenodes;	
		int jobsnumber = schedstub.getAllRunningJobsList().size();							// Get the list of jobs running.
		
    	rmstub.disconnect();								// Disconnect from the Resource Manager.
    	schedstub.disconnect();								// Disconnect from the Scheduler.
    				
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		summary.addFact("NODE/S ALIVE=" + alivenodes + " FREE=" + freenodes + " JOB/S RUNNING=" + jobsnumber);
		
		if (jobsnumber == 0 && busynodes != 0){
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_2_CRITICAL, "NO JOBS AND " + busynodes + " BUSY NODES"));
		}
		
		if (summary.isAllOkay() == true){
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_0_OK, "OK"));
		}
		
		return summary.getSummaryOfAll();
		
	}

	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
        final Arguments options = new Arguments(args);
		DebugProber prob = new DebugProber(options);		// Create the prober.
		prob.initializeProber();
		prob.startProbeAndExit();							// Start the probe.
	}
}	
