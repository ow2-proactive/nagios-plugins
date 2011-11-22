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
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.resourcemanager.exception.RMException;
import qosprober.misc.PAMRMisc;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.PAEnvironmentInitializer;
import qosprobercore.main.TimedStatusTracer;
import qosprobercore.misc.Misc;

/** 
 * This is a general Nagios plugin class that performs a test on the PAMR Router of ProActive based grids.
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class PAMRProber {
	
	public final static String SERVER_NAME = "server";		// Name that the server will use to register itself.
    public final static String PREFIX_URL = "pamr://";		// Prefix that is expected in a URL given by the registered server.
    
    public final static int MESSAGE_LENGTH = 1024;			// Length of the parameter passed when the client calls the server.
    
	public static Logger logger = Logger.getLogger(PAMRProber.class.getName()); // Logger.
	private Arguments arguments; 							// Arguments given to the prober. 
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this PAMRProber. */
	public PAMRProber(Arguments args){
		this.arguments = args;
	}
	
	/**
	 * Initialize the ProActive environment for this probe. */
	public void initializeEnvironment() throws Exception{
		Misc.log4jConfiguration(arguments.getInt("debug"));						// Loading log4j configuration. 
		PAEnvironmentInitializer.initPAConfiguration(
			arguments.getStr("paconf"),
			arguments.getStr("hostname"),
			arguments.getStr("port"));
	}
	
	/** 
	 * Validate all the arguments given to this probe. 
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	public void validateArguments() throws IllegalArgumentException{
		String[] notnull1 = {"critical"}; // These arguments should not be null.
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
	 * @throws KeyException 
	 * @throws IOException 
	 * @throws NodeException, Exception 
	 * @throws ActiveObjectCreationException */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws KeyException, LoginException, RMException, IOException, ActiveObjectCreationException, NodeException, Exception{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(arguments.getInt("critical")));
		if (arguments.getBoo("warning")==true){ // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(arguments.getInt("warning")));
		}
		
    	String serverurl = null;
    	Server server = null;
    
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
    	
        // Creates an active object for the server
        server = org.objectweb.proactive.api.PAActiveObject.newActive(Server.class, null);
        
		tracer.finishLastMeasurementAndStartNewOne("time_registering_server", "probe initialized, registering the server...");
        
        logger.info("Registering server...");
        org.objectweb.proactive.api.PAActiveObject.registerByName(server, SERVER_NAME);
        String url = org.objectweb.proactive.api.PAActiveObject.getActiveObjectNodeUrl(server);
        logger.info("Done.");
        
        serverurl = PREFIX_URL + PAMRMisc.getResourceNumberFromURL(url) + "/" + SERVER_NAME;
        logger.info(">>> Server standard URL: "+ serverurl);
        
		tracer.finishLastMeasurementAndStartNewOne("time_executing_client", "server registered, running client...");
       
        // Now we are ready to run the client. It needs to get connected to the same PAMR router, and afterwards contact
        // this just initialized server. About the initialization of the client, there are 2 possible argument-format 
        // for this module:
    	// SERVERURL PACONFIGURATIONFILE				(2 arguments, assume PA conf. file is given).
    	// SERVERURL PAMRADDRESS PAMRPORT				(3 arguments, assume server address and port are given).
    	
        logger.info("Running the client...");
        if (PAEnvironmentInitializer.usingPAConfigurationFile() == true){		
        								// Depending on whether there is a ProActive configuration file, these are
        								// the parameters that we send to the client to get connected to the same router.
	        qosprober.misc.PAMRMisc.runNewJVM(Client.class.getName(), serverurl + " " + arguments.getStr("paconf"));
        }else{
        	qosprober.misc.PAMRMisc.runNewJVM(Client.class.getName(), serverurl + " " + arguments.getStr("hostname") + " " + arguments.getStr("port"));
        }
        logger.info("Done.");
        
		tracer.finishLastMeasurementAndStartNewOne("time_waiting_message", "waiting interaction between Active objects...");
        
        logger.info("Waiting for the client's message...");
        
    	while(server.isDone()==false){	// Wait for the client's call...
    		Thread.sleep(300);
    	}
    
		tracer.finishLastMeasurement();
    	
		
		
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		if (arguments.isGiven("warning") && tracer.getTotal() > arguments.getInt("warning")){ // If it took longer than timeoutwarnsec, throw a warning message.
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING, "PROBE TOO SLOW"));
		}
		
		if (summary.isAllOkay() == true){	// If everything went okay...
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_0_OK,"OK"));
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
		options.addNewOption("w", "warning", true);									                // Timeout in seconds for the warning message to be thrown.
		options.addNewOption("c", "critical", true);                                                // Timeout in seconds for the job to be executed.
		
		options.addNewOption("f", "paconf", true);                                                  // Path of the ProActive xml configuration file.
		options.addNewOption("H", "hostname", true);                                                // Host to be tested. 
		options.addNewOption("x", "port"    , true);                                                // Port of the host to be tested. 
        
		options.parseAll();
		
		if ((Boolean)options.getBoo("help") == true)	
			NagiosPlugin.printMessageUsageAndExit("");
		
		if ((Boolean)options.getBoo("version") == true)
			NagiosPlugin.printVersionAndExit();
		
		
		final PAMRProber jobp = new PAMRProber(options);	// Create the prober.
		
		jobp.validateArguments();							// Validate its arguments. In case of problems, it throws an IllegalArgumentException.
		
		jobp.initializeEnvironment();						// Initializes the environment for ProActive objects and prober.
		
		final TimedStatusTracer tracer = TimedStatusTracer.getInstance();	// We want to get last status memory, and timing measurements.
		
		// We prepare our probe to run it in a different thread. 
		// The probe consists in a communication between two Active objects. 
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Callable<NagiosReturnObject> proberCallable = new Callable<NagiosReturnObject>(){
			public NagiosReturnObject call() throws Exception {
				return jobp.probe(tracer);
			}
		};

		// We submit to the executor the prober activity. 
		Future<NagiosReturnObject> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
		
		NagiosReturnObject res = null;
		try{								 // We execute the future using a timeout. 
			res = proberFuture.get(options.getInt("critical"), TimeUnit.SECONDS);
			res.addCurvesSection(tracer, "time_all");
		}catch(TimeoutException e){ 		// The execution took more time than expected. 
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "TIMEOUT OF "+options.getInt("critical")+ " SEC (last status was: " + tracer.getLastStatusDescription() + ")", e);
			res.addCurvesSection(tracer, null);
		}catch(ExecutionException e){ 		// There was an unexpected problem with the execution of the prober. 
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "FAILURE: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}catch(Exception e){				// There was an unexpected critical exception not captured. 
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}
		NagiosPlugin.printAndExit(res, options.getInt("debug"));
	}
}
