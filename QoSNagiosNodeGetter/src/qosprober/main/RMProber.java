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

import org.ow2.proactive.utils.NodeSet;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosMiniStatus;
import qosprobercore.main.PANagiosPlugin;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.RemainingTime;
import qosprobercore.main.TimedStatusTracer;

/** 
 * This is a general Nagios plugin class that performs a test on the RM, by doing:
 *    -Node obtaining
 *    -Node retrieval 
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class RMProber extends PANagiosPlugin{

	private boolean quickDisconnectionEnabled = true;
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this RMProber. 
	 * @throws Exception */
	public RMProber(Arguments args) throws Exception{
		super("RM", args);
		
		args.addNewOption("u", "user", true);							// User.
		args.addNewOption("p", "pass", true);							// Pass.
		args.addNewOption("r", "url", true);							// Url of the Scheduler/RM.
		
		args.addNewOption("q", "nodesrequired", true, new Integer(1));	// Amount of nodes to be asked to the Resource Manager.
		args.addNewOption("b", "nodeswarning", true, new Integer(0));	// Obtaining fewer nodes than this, a warning message will be thrown.
		args.addNewOption("s", "nodescritical", true, new Integer(0));	// Obtaining fewer nodes than this, a critical message will be thrown.

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
		arguments.checkIsGiven("url");
		arguments.checkIsGiven("user");
		arguments.checkIsGiven("pass");
	}
	
	/**
	 * Probe the scheduler
	 * A few calls are done against the Resource Manager (RM):
	 *   - join
	 *   - get node/s
	 *   - release node/s
	 *   - disconnect
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. 
	 * @throws Exception */	 
	public NagiosReturnObject probe(final TimedStatusTracer tracer) throws Exception{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(getArgs().getInt("critical")));
		if (getArgs().isGiven("warning")==true){ // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(getArgs().getInt("warning")));
		}
		
		RemainingTime rt = new RemainingTime(getArgs().getInt("critical") * 1000);
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		RMThroughSingleThread rmstub = new RMThroughSingleThread();							// We get connected to the RM through this stub. 
		
		this.setQuickDesconnectMechanism(rmstub);
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection", "connecting to RM...");
					
		rmstub.init(																		// We get connected to the RM.
			getArgs().getStr("url"),  getArgs().getStr("user"), 
			getArgs().getStr("pass"), rt.getRemainingTimeWE());	
	
		tracer.finishLastMeasurementAndStartNewOne("time_getting_status", "connected to RM, getting status...");

		int freenodes = rmstub.getRMState(rt.getRemainingTimeWE()).getFreeNodesNumber();	// Get the amount of free nodes.
		
		tracer.finishLastMeasurementAndStartNewOne("time_getting_nodes", "connected to RM, getting nodes...");
		
		NodeSet nodes = rmstub.getNodes(													// Request some nodes.
			getArgs().getInt("nodesrequired"), rt.getRemainingTimeWE()); 	
		int obtainednodes = nodes.size();													// Get the amount of obtained nodes.
					
		tracer.finishLastMeasurementAndStartNewOne("time_releasing_nodes", "releasing nodes...");
					
    	rmstub.releaseNodes(nodes, rt.getRemainingTimeWE());								// Release the nodes obtained.
			
		tracer.finishLastMeasurementAndStartNewOne("time_disconn", "disconnecting...");
					
    	Boolean disc = rmstub.disconnect(rt.getRemainingTimeWE());											// Disconnect from the Resource Manager.
    	if (disc == true){
			disableQuickDisconnectionHook();																// No need to try to disconnect again if it already went okay.
    	}
			    	
		tracer.finishLastMeasurement();
					
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		int nodesrequired = getArgs().getInt("nodesrequired");
		
		Double time_all = tracer.getTotal();
		
		summary.addFact("NODE/S FREE=" +  freenodes + " REQUIRED=" + nodesrequired + " OBTAINED=" + obtainednodes);
	
		tracer.addNewReference("nodes_required", nodesrequired);
		tracer.addNewReference("nodes_free", freenodes);
		tracer.addNewReference("nodes_obtained", obtainednodes);
		
		
		if (obtainednodes < getArgs().getInt("nodescritical")){								// Fewer nodes than criticalnodes.	
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_2_CRITICAL, "TOO FEW NODES OBTAINED"));
		}else if (obtainednodes < getArgs().getInt("nodeswarning")){						// Fewer nodes than warningnodes.	
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_1_WARNING,  "TOO FEW NODES OBTAINED"));
		}
		
		// Having F free nodes, if W is the number of wanted nodes, I should get the min(F, W). 
		//        4 free nodes,    3                  wanted nodes, I should get the min(4, 3)=3 nodes. 
		if (obtainednodes < Math.min(freenodes, nodesrequired)){
			summary.addMiniStatus(
					new NagiosMiniStatus(
							RESULT_2_CRITICAL, "PROBLEM: NODES (OBTAINED/REQUIRED/FREE)=("+obtainednodes+"/"+nodesrequired+"/"+freenodes+")"));
		}
		
		if (getArgs().isGiven("warning") && time_all > getArgs().getInt("warning")){		// It took longer than timeoutwarnsec.
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_1_WARNING, "NODE/S OBTAINED TOO SLOWLY"));
		}																					// Everything was okay.
		
		if (freenodes == 0){
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_3_UNKNOWN, "NO FREE NODES"));
		}	
		
		if (summary.isAllOkay() == true){
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_0_OK, "OK"));
		}
		
		return summary.getSummaryOfAllWithTimeAll(tracer);
		
	}
	
	public void disableQuickDisconnectionHook(){
		logger.info("Disabled disconnection hook.");
		quickDisconnectionEnabled = false;
	}

	public boolean isQuickDisconnectionHookEnabled(){
		return quickDisconnectionEnabled;
	}
	/**
	 * Configure all what is needed to be able to execute the quick disconnection mechanism to release quickly all the nodes that were
	 * possibly obtained during the probe.
	 * @param stub stub of the RM that should be disconnected quickly. */
	public void setQuickDesconnectMechanism(final RMThroughSingleThread stub){
		Runnable disc = new Runnable(){								// Runnable to perform the quick connection (no matter which thread with).
			public void run(){
				try{
					if (isQuickDisconnectionHookEnabled()){
				    	stub.quickDisconnect(5*1000);				// Disconnect from the Resource Manager.
					}else{
						logger.info("Quick Disconnection is not needed.");
					}
				}catch(Exception e){
					logger.warn("Faled while performing quickDisconnect..." + e);
				}
			}
		};
		
		Runtime.getRuntime().addShutdownHook(new Thread(disc)); // Connect the disconnection (THREAD_POOL executed) to the ShutdownHook. 
	}
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
        final Arguments options = new Arguments(args);
		RMProber prob = new RMProber(options);														// Create the prober.
		prob.initializeProber();
		prob.startProbeAndExitManualTimeout();														// Start the probe.
		// We control the timeout in this particular case (...ManualTimeout) because in case of timeout, this probed RM is susceptible 
		// of leaving one resource as 'locked', which is something we strongly avoid. We always keep calling the RM from the same Thread
		// through the RMThoughSingleThread object, but once a particular call exceeds this time limit, we ask the same executor to finish 
		// the call and then ask this executor (single threaded) to run the quickDisconnect to release any possible locked RM resource.
	}
}	
