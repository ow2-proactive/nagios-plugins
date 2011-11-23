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

import java.security.KeyException;
import java.util.Arrays;
import java.util.concurrent.*;
import javax.security.auth.login.LoginException;
import org.apache.log4j.Logger;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.utils.NodeSet;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.PAEnvironmentInitializer;
import qosprobercore.main.TimedStatusTracer;
import qosprobercore.misc.Misc;

/** 
 * This is a general Nagios plugin class that performs a test on the RM, by doing:
 *    -Node obtaining
 *    -Node retrieval 
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class RMProber {

	public static Logger logger = Logger.getLogger(RMProber.class.getName()); // Logger.
	private Arguments arguments; 				// Arguments given to the prober. 
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this RMProber. */
	public RMProber(Arguments args){
		this.arguments = args;
	}
	
	/**
	 * Initialize the ProActive environment for this probe. */
	public void initializeEnvironment() throws Exception{
		Misc.log4jConfiguration(arguments.getInt("debug"));						// Loading log4j configuration. 
		/* Loading job's expected output. */
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
	 * A few calls are done against the Resource Manager (RM):
	 *   - join
	 *   - get node/s
	 *   - release node/s
	 *   - disconnect
	 * @return Object[Integer, String] with Nagios code error and a descriptive message of the test. 
	 * @throws RMException 
	 * @throws LoginException 
	 * @throws KeyException */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws KeyException, LoginException, RMException{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(arguments.getInt("critical")));
		if (arguments.getBoo("warning")==true){ // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(arguments.getInt("warning")));
		}
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		RMStubProber rmstub = new RMStubProber();			// We get connected to the RM through this stub. 
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection", "connecting to RM...");
		
		rmstub.init(										// We get connected to the RM.
				arguments.getStr("url"),  arguments.getStr("user"), 
				arguments.getStr("pass"));	
		
		tracer.finishLastMeasurementAndStartNewOne("time_getting_status", "connected to RM, getting status...");
		
		int freenodes = rmstub.getRMState().getFreeNodesNumber(); // Get the amount of free nodes.
		
		tracer.finishLastMeasurementAndStartNewOne("time_getting_nodes", "connected to RM, getting nodes...");
		
		NodeSet nodes = rmstub.getNodes(					// Request some nodes.
				arguments.getInt("nodesrequired")); 	
		int obtainednodes = nodes.size();
		
		tracer.finishLastMeasurementAndStartNewOne("time_releasing_nodes", "releasing nodes...");
    	
    	rmstub.releaseNodes(nodes);							// Release the nodes obtained.
    	
		tracer.finishLastMeasurementAndStartNewOne("time_disconn", "disconnecting...");
    	
    	rmstub.disconnect();								// Disconnect from the Resource Manager.
    				
		tracer.finishLastMeasurement();
		
		
		
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		int nodesrequired = arguments.getInt("nodesrequired");
		
		Double time_all = tracer.getTotal();
		
		summary.addFact(obtainednodes + " NODE/S OBTAINED");
		
		if (obtainednodes < arguments.getInt("nodescritical")){									// Fewer nodes than criticalnodes.	
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "TOO FEW NODES (" + nodesrequired + " REQUIRED, " + freenodes + " FREE)"));
		}else if (obtainednodes < arguments.getInt("nodeswarning")){							// Fewer nodes than warningnodes.	
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING,  "TOO FEW NODES (" + nodesrequired + " REQUIRED, " + freenodes + " FREE)"));
		}
		
		// Having F free nodes, if W is the number of wanted nodes, I should get the min(F, W). 
		//        4 free nodes,    3                  wanted nodes, I should get the min(4, 3)=3 nodes. 
		if (obtainednodes < Math.min(freenodes, nodesrequired)){
			summary.addNagiosReturnObject(
					new NagiosReturnObject(
							NagiosReturnObject.RESULT_2_CRITICAL, "PROBLEM: NODES (OBTAINED/REQUIRED/FREE)=("+obtainednodes+"/"+nodesrequired+"/"+freenodes+")"));
		}
		
		if (arguments.isGiven("warning") && time_all > arguments.getInt("warning")){		// It took longer than timeoutwarnsec.
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
		options.addNewOption("w", "warning", true);													// Timeout in seconds for the warning message to be thrown.
		options.addNewOption("c", "critical", true);                                                // Timeout in seconds for the job to be executed.
		
		options.addNewOption("u", "user", true);													// User.
		options.addNewOption("p", "pass", true);                                                    // Pass.
		options.addNewOption("r", "url", true);                                                     // Url of the Scheduler/RM.
		options.addNewOption("f", "paconf", true);                                                  // Path of the ProActive xml configuration file.
		options.addNewOption("H", "hostname", true);                                                // Host to be tested. 
		options.addNewOption("x", "port"    , true);                                                // Port of the host to be tested. 
		
		options.addNewOption("q", "nodesrequired", true, new Integer(1));							// Amount of nodes to be asked to the Resource Manager.
		options.addNewOption("b", "nodeswarning", true, new Integer(0));							// Obtaining fewer nodes than this, a warning message will be thrown.
		options.addNewOption("s", "nodescritical", true, new Integer(0));							// Obtaining fewer nodes than this, a critical message will be thrown.

		options.parseAll();
		
		if (options.getBoo("help") == true)	
			NagiosPlugin.printMessageUsageAndExit("");
		
		if (options.getBoo("version") == true)
			NagiosPlugin.printVersionAndExit();
		
		final RMProber jobp = new RMProber(options);		// Create the prober.
		
		jobp.validateArguments();							// Validate its arguments. In case of problems, it throws an IllegalArgumentException.
	
		jobp.initializeEnvironment();						// Initialize the environment for ProActive objects and prober.
		
		options.printArgumentsGiven();						// Print a list with the arguments given by the user. 
		
		/* Now we prepare our probe to run it in a different thread. */
		/* The probe consists in a node obtaining done from the Resource Manager. */
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		final TimedStatusTracer tracer = TimedStatusTracer.getInstance();	// We want to get last status memory, and timing measurements.
		
		Callable<NagiosReturnObject> proberCallable = new Callable<NagiosReturnObject>(){
			public NagiosReturnObject call() throws Exception {
				return jobp.probe(tracer);
			}
		};

		/* We submit to the executor the prober activity (and the prober will then 
		 * obtain a node from the RM in that activity). */
		Future<NagiosReturnObject> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
		
		NagiosReturnObject res = null;
		try{ // We execute the future using a timeout.
			res = proberFuture.get(options.getInt("critical"), TimeUnit.SECONDS);
			res.addCurvesSection(tracer, "time_all");
		}catch(TimeoutException e){
			/* The execution took more time than expected. */
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "TIMEOUT OF "+options.getInt("critical")+ "s (last status was: " + tracer.getLastStatusDescription() + ")", e);
			res.addCurvesSection(tracer, null);
		}catch(ExecutionException e){
			/* There was an unexpected problem with the execution of the prober. */
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "FAILURE: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}catch(Exception e){
			/* There was an unexpected critical exception not captured. */
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}
		NagiosPlugin.printAndExit(res, options.getInt("debug"));
	}
}	
