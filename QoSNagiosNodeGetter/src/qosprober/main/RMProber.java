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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.ow2.proactive.utils.NodeSet;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.PAEnvironmentInitializer;
import qosprobercore.main.TimedStatusTracer;

/** 
 * This is a general Nagios plugin class that performs a test on the RM, by doing:
 *    -Node obtaining
 *    -Node retrieval 
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class RMProber extends NagiosPlugin{

	private final ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor(); 	// The rights to manage the RM nodes will be given to this thread.
	private Runnable disconnectRunnable;												// Special mechanism to disconnect from RM (and unlock locked nodes)
																						// even though a timeout is reached (best effort approach).
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this RMProber. 
	 * @throws Exception */
	public RMProber(Arguments args) throws Exception{
		super(args);
		
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
		PAEnvironmentInitializer.initPAConfiguration(
			getArgs().getStr("paconf"),
			getArgs().getStr("hostname"),
			getArgs().getStr("port"));
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
		Boolean timeout = false;
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(getArgs().getInt("critical")));
		if (getArgs().isGiven("warning")==true){ // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(getArgs().getInt("warning")));
		}
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		final RMStubProber rmstub = new RMStubProber();							// We get connected to the RM through this stub. 
		
		this.setQuickDesconnectMechanism(rmstub);
		
		FutureTask<Object[]> task = new FutureTask<Object[]>(
			new Callable<Object[]>(){
				public Object[] call() throws Exception {
					tracer.finishLastMeasurementAndStartNewOne("time_connection", "connecting to RM...");
					
					rmstub.init(												// We get connected to the RM.
						getArgs().getStr("url"),  getArgs().getStr("user"), 
						getArgs().getStr("pass"));	
	
					tracer.finishLastMeasurementAndStartNewOne("time_getting_status", "connected to RM, getting status...");
		
					int freenodes = rmstub.getRMState().getFreeNodesNumber();	// Get the amount of free nodes.
		
					tracer.finishLastMeasurementAndStartNewOne("time_getting_nodes", "connected to RM, getting nodes...");
		
					NodeSet nodes = rmstub.getNodes(							// Request some nodes.
						getArgs().getInt("nodesrequired")); 	
					int obtainednodes = nodes.size();							// Get the amount of obtained nodes.
					
					tracer.finishLastMeasurementAndStartNewOne("time_releasing_nodes", "releasing nodes...");
					
			    	rmstub.releaseNodes(nodes);									// Release the nodes obtained.
			
					tracer.finishLastMeasurementAndStartNewOne("time_disconn", "disconnecting...");
					
			    	rmstub.disconnect();										// Disconnect from the Resource Manager.
			    	
					tracer.finishLastMeasurement();
					
					Object[] ret = {new Integer(freenodes), new Integer(obtainednodes)};
			    	return ret; 
				}
			}
		);
		THREAD_POOL.execute(task);
		
		Integer obtainednodes = null;
		Integer freenodes = null;
		Object[] ret = null;
		try{
			ret = task.get(getArgs().getInt("critical"), TimeUnit.SECONDS);
			freenodes     = (Integer)ret[0];
			obtainednodes = (Integer)ret[1];
		}catch(TimeoutException e){
			task.cancel(true);
			timeout = true;
		}
		
		
		if (timeout==true){
			executeDisconnectMechanism();
			throw new TimeoutException("TIMEOUT OF " + getArgs().getInt("critical") + " SEC.");
		}
		
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		int nodesrequired = getArgs().getInt("nodesrequired");
		
		Double time_all = tracer.getTotal();
		
		summary.addFact("NODE/S FREE=" +  freenodes + " REQUIRED=" + nodesrequired + " OBTAINED=" + obtainednodes);
	
		tracer.addNewReference("nodes_required", nodesrequired);
		tracer.addNewReference("nodes_free", freenodes);
		tracer.addNewReference("nodes_obtained", obtainednodes);
		
		
		if (obtainednodes < getArgs().getInt("nodescritical")){									// Fewer nodes than criticalnodes.	
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "TOO FEW NODES OBTAINED"));
		}else if (obtainednodes < getArgs().getInt("nodeswarning")){							// Fewer nodes than warningnodes.	
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING,  "TOO FEW NODES OBTAINED"));
		}
		
		// Having F free nodes, if W is the number of wanted nodes, I should get the min(F, W). 
		//        4 free nodes,    3                  wanted nodes, I should get the min(4, 3)=3 nodes. 
		if (obtainednodes < Math.min(freenodes, nodesrequired)){
			summary.addNagiosReturnObject(
					new NagiosReturnObject(
							NagiosReturnObject.RESULT_2_CRITICAL, "PROBLEM: NODES (OBTAINED/REQUIRED/FREE)=("+obtainednodes+"/"+nodesrequired+"/"+freenodes+")"));
		}
		
		if (getArgs().isGiven("warning") && time_all > getArgs().getInt("warning")){		// It took longer than timeoutwarnsec.
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING, "NODE/S OBTAINED TOO SLOWLY"));
		}																					// Everything was okay.
		
		if (freenodes == 0){
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_3_UNKNOWN, "NO FREE NODES"));
		}	
		
		if (summary.isAllOkay() == true){
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_0_OK, "OK"));
		}
		
		return summary.getSummaryOfAll();
		
	}

	/**
	 * Execute a quick disconnection mechanism to release quickly all the nodes that were possibly obtained during the probe. */
	public void executeDisconnectMechanism(){
			THREAD_POOL.execute(disconnectRunnable);
	}
	
	/**
	 * Configure all what is needed to be able to execute the quick disconnection mechanism to release quickly all the nodes that were
	 * possibly obtained during the probe.
	 * @param stub stub of the RM that should be disconnected quickly. */
	public void setQuickDesconnectMechanism(final RMStubProber stub){
		Runnable disc = new Runnable(){
			public void run(){
		    	stub.quickDisconnect();								// Disconnect from the Resource Manager.
			}
		};
		this.disconnectRunnable = disc;
		Runtime.getRuntime().addShutdownHook(new Thread(disc));
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
	}
}	
