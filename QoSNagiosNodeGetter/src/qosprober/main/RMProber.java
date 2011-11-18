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
import java.util.HashMap;
import java.util.concurrent.*;
import javax.security.auth.login.LoginException;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.utils.NodeSet;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.NagiosReturnObject;
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
	private HashMap<String, Object> arguments; 				// Arguments given to the prober. 
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this RMProber. */
	public RMProber(HashMap<String, Object> args){
		this.arguments = args;
	}
	
	/**
	 * Initialize the ProActive environment for this probe. */
	public void initializeEnvironment() throws Exception{
		Misc.log4jConfiguration((Integer)arguments.get("debug"));						// Loading log4j configuration. 
		/* Loading job's expected output. */
		PAEnvironmentInitializer.initPAConfiguration(
			(String)arguments.get("paconf"),
			(String)arguments.get("hostname"),
			(String)arguments.get("port"));
	}
	
	/** 
	 * Validate all the arguments given to this probe. 
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	public void validateArguments() throws IllegalArgumentException{
		String[] notnull1 = {"url", "user", "pass", "timeout"}; // These arguments should not be null.
		Misc.allElementsAreNotNull(Arrays.asList(notnull1), arguments);
		Integer debug = (Integer)arguments.get("debug");
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
		tracer.addNewReference("timeout_threshold", new Double((Integer)arguments.get("timeout")));
		tracer.addNewReference("time_all_warning_threshold", new Double((Integer)arguments.get("timeoutwarning")));
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		RMStubProber rmstub = new RMStubProber();			// We get connected to the RM through this stub. 
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection", "connecting to RM...");
		
		rmstub.init(										// We get connected to the RM.
				(String)arguments.get("url"),  (String)arguments.get("user"), 
				(String)arguments.get("pass"));	
		
		tracer.finishLastMeasurementAndStartNewOne("time_getting_nodes", "connected to RM, getting nodes...");
		
		NodeSet nodes = rmstub.getNodes(					// Request some nodes.
				(Integer)arguments.get("nodesrequired")); 	
		int obtainednodes = nodes.size();
		
		tracer.finishLastMeasurementAndStartNewOne("time_releasing_nodes", "releasing nodes...");
    	
    	rmstub.releaseNodes(nodes);							// Release the nodes obtained.
    	
		tracer.finishLastMeasurementAndStartNewOne("time_disconn", "disconnecting...");
    	
    	rmstub.disconnect();								// Disconnect from the Resource Manager.
    				
		tracer.finishLastMeasurement();
		
		NagiosReturnObject res = null;  
		
		String summary = "(obtained/required/critical/warning)=(" + 
				obtainednodes + "/" + arguments.get("nodesrequired") + "/" + 
				arguments.get("nodescritical") + "/" + arguments.get("nodeswarning") + ")";
		
		Double time_all = tracer.getTotal();
		
		if (obtainednodes < (Integer)arguments.get("nodescritical")){	
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "CRITICAL STATE, TOO FEW NODES OBTAINED " + summary);
		}else if (obtainednodes < (Integer)arguments.get("nodeswarning")){		
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING, "WARNING STATE, TOO FEW NODES OBTAINED " + summary);
		}else if (time_all > (Integer)arguments.get("timeoutwarning")){			// If longer than timeoutwarnsec, warning message.
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING, "WARNING STATE, " + obtainednodes + " NODE/S OBTAINED TOO SLOWLY");
		}else{																	// Else everything was okay.
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_0_OK, obtainednodes + " NODE/S OBTAINED OK");
		}
	
		return res;
	}

	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
		/* Parsing of arguments. */
		Options options = new Options();
		// short, long, hasargument, description
        Option helpO =			new Option("h", "help", false, "");			options.addOption(helpO);
        Option debugO =			new Option("v", "debug", true, ""); 		options.addOption(debugO);
        Option userO = 			new Option("u", "user", true, ""); 			options.addOption(userO);
        Option passO = 			new Option("p", "pass", true, ""); 			options.addOption(passO);
        Option urlO = 			new Option("r", "url", true, ""); 			options.addOption(urlO);
		Option nodesrequiredO = new Option("q", "nodesrequired", true, "");	options.addOption(nodesrequiredO);
		Option nodeswarningO = 	new Option("b", "nodeswarning", true, "");	options.addOption(nodeswarningO);
        Option nodescriticalO = new Option("s", "nodescritical", true, "");	options.addOption(nodescriticalO);
        Option timeoutO = 		new Option("t", "timeout", true, "");		options.addOption(timeoutO);
        Option timeoutwarningO =new Option("n", "timeoutwarning", true, "");options.addOption(timeoutwarningO);
        Option paconfO = 		new Option("f", "paconf", true, ""); 		options.addOption(paconfO);
        Option hostO = 			new Option("H", "hostname", true, "");		options.addOption(hostO);
        Option portO = 			new Option("x", "port"    , true, "");		options.addOption(portO);
        Option warningO = 		new Option("w", "warning", true, "");		options.addOption(warningO);
        Option criticalO = 		new Option("c", "critical", true, "");		options.addOption(criticalO);
        Option versionO = 		new Option("V", "version", false, "");		options.addOption(versionO);

        CommandLine parser = null;
        try{
	        Parser parserrr = new GnuParser();
	        parser = parserrr.parse(options, args);
        }catch(org.apache.commons.cli.MissingOptionException ex){
	        NagiosPlugin.printMessageUsageAndExit(ex.getMessage());	
        }

        final HashMap<String, Object> ar = new HashMap<String, Object>();

		ar.put("help", parser.hasOption("h"));																	// Help message.
		ar.put("debug", Misc.parseInteger(parser.getOptionValue("v"), NagiosPlugin.DEBUG_LEVEL_1_EXTENDED));	// Level of verbosity.
		ar.put("user", (String)parser.getOptionValue("u"));			 											// User.
		ar.put("pass", (String)parser.getOptionValue("p")); 													// Pass.
		ar.put("url", (String)parser.getOptionValue("r")); 														// Url of the Scheduler/RM.
		ar.put("nodesrequired", Misc.parseInteger(parser.getOptionValue("q"),1)); 								// Amount of nodes to be asked to the Resource Manager.
		ar.put("nodeswarning", Misc.parseInteger(parser.getOptionValue("b"),(Integer)ar.get("nodesrequired"))); // Obtaining fewer nodes than this, a warning message will be thrown. 
		ar.put("nodescritical", Misc.parseInteger(parser.getOptionValue("s"),(Integer)ar.get("nodesrequired")));// Obtaining fewer nodes than this, a critical message will be thrown. 
		ar.put("timeout", Misc.parseInteger(parser.getOptionValue("t"), null));									// Timeout in seconds for the job to be executed.
		ar.put("timeoutwarning", Misc.parseInteger(parser.getOptionValue("n"),(Integer)ar.get("timeout")));		// Timeout in seconds for the warning message to be thrown.
		ar.put("paconf", (String)parser.getOptionValue("f")); 													// Path of the ProActive xml configuration file.
		ar.put("host", (String)parser.getOptionValue("H"));						 								// PAMR router host. Ignored.
		ar.put("port", (String)parser.getOptionValue("x"));														// PAMR router port. 
		ar.put("warning", (String)parser.getOptionValue("w", "ignored"));										// Warning level. Ignored.
		ar.put("critical", (String)parser.getOptionValue("c", "ignored")); 										// Critical level. Ignored. 
		ar.put("version", parser.hasOption("V"));																// Prints the version of the plugin.
	
		if ((Boolean)ar.get("help") == true)	
			NagiosPlugin.printMessageUsageAndExit("");
		
		if ((Boolean)ar.get("version") == true)
			NagiosPlugin.printVersionAndExit();
		
		final RMProber jobp = new RMProber(ar);				// Create the prober.
		
		jobp.validateArguments();							// Validate its arguments. In case of problems, it throws an IllegalArgumentException.
	
		jobp.initializeEnvironment();						// Initializes the environment for ProActive objects.
		
		for (String key: ar.keySet())						// Show all the arguments considered. 
			logger.info("\t" + key + ":'" + ar.get(key) + "'");
		
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
			res = proberFuture.get((Integer)ar.get("timeout"), TimeUnit.SECONDS);
			res.appendCurvesSection(tracer.getMeasurementsSummary("time_all"));
		}catch(TimeoutException e){
			/* The execution took more time than expected. */
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "TIMEOUT OF "+(Integer)ar.get("timeout")+ "s (last status was: " + tracer.getLastStatusDescription() + ")", e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}catch(ExecutionException e){
			/* There was an unexpected problem with the execution of the prober. */
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "FAILURE: " + e.getMessage(), e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}catch(Exception e){
			/* There was an unexpected critical exception not captured. */
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}
		NagiosPlugin.printAndExit(res, (Integer)ar.get("debug"));
	}
}	
