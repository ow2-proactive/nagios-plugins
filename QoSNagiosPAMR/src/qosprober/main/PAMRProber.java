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
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.resourcemanager.exception.RMException;
import qosprober.misc.PAMRMisc;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.NagiosReturnObject;
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
		Misc.log4jConfiguration((Integer)arguments.get("debug"));						// Loading log4j configuration. 
		PAEnvironmentInitializer.initPAConfiguration(
			(String)arguments.get("paconf"),
			(String)arguments.get("hostname"),
			(String)arguments.get("port"));
	}
	
	/** 
	 * Validate all the arguments given to this probe. 
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	public void validateArguments() throws IllegalArgumentException{
		String[] notnull1 = {"timeout"}; // These arguments should not be null.
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
	 * @throws KeyException 
	 * @throws IOException 
	 * @throws NodeException, Exception 
	 * @throws ActiveObjectCreationException */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws KeyException, LoginException, RMException, IOException, ActiveObjectCreationException, NodeException, Exception{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double((Integer)arguments.get("timeout")));
		tracer.addNewReference("time_all_warning_threshold", new Double((Integer)arguments.get("timeoutwarning")));
		
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
	        qosprober.misc.PAMRMisc.runNewJVM(Client.class.getName(), serverurl + " " + arguments.get("paconf"));
        }else{
        	qosprober.misc.PAMRMisc.runNewJVM(Client.class.getName(), serverurl + " " + arguments.get("hostname") + " " + arguments.get("port"));
        }
        logger.info("Done.");
        
		tracer.finishLastMeasurementAndStartNewOne("time_waiting_message", "waiting interaction between Active objects...");
        
        logger.info("Waiting for the client's message...");
        
    	while(server.isDone()==false){	// Wait for the client's call...
    		Thread.sleep(300);
    	}
    
		tracer.finishLastMeasurement();
    	
    	NagiosReturnObject res = null;
		//double time_all = time_initializing+time_registering_server+time_executing_client+time_waiting_message;
		
		//String timesummary = "time_initializing=" "time_registering_server=" "time_executing_client=" "time_waiting_message=" "timeout_threshold=" "time_all_warning_threshold=" 

    	Double time_all = tracer.getTotal();
    	
		if (time_all > (Integer)arguments.get("timeoutwarning")){	// If it took longer than timeoutwarnsec, throw a warning message.
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING, "WARNING STATE, SLOW PROBE");
		}else{														// Else, we consider that everything went okay.
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_0_OK,"OK");
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
        Option helpO =			new Option("h", "help", false, "");				options.addOption(helpO);
        Option debugO =			new Option("v", "debug", true, ""); 			options.addOption(debugO);
        Option timeoutsecO = 	new Option("t", "timeout", true, "");			options.addOption(timeoutsecO);
        Option timeoutwarnsecO =new Option("n", "timeoutwarning", true, "");	options.addOption(timeoutwarnsecO);
        Option paconfO = 		new Option("f", "paconf", true, ""); 			options.addOption(paconfO);
        Option hostnameO = 		new Option("H", "hostname", true, "");			options.addOption(hostnameO);
        Option portO = 			new Option("x", "port"    , true, "");			options.addOption(portO);
        Option warningO = 		new Option("w", "warning", true, ""); 			options.addOption(warningO);
        Option criticalO = 		new Option("c", "critical", true, "");			options.addOption(criticalO);
        Option versionO = 		new Option("V", "version", false, "");			options.addOption(versionO);
        
        CommandLine parser = null;
        try{
	        Parser parserrr = new GnuParser();
	        parser = parserrr.parse(options, args);
        }catch(org.apache.commons.cli.MissingOptionException ex){
	        NagiosPlugin.printMessageUsageAndExit(ex.getMessage());	
        }
        
        final Arguments ar = new Arguments();
        
		ar.put("help", parser.hasOption("h"));																// Help message.
		ar.put("debug", Misc.parseInteger(parser.getOptionValue("v"), NagiosPlugin.DEBUG_LEVEL_1_EXTENDED));	// Level of verbosity.
		ar.put("timeout", Misc.parseInteger(parser.getOptionValue("t"), null));							// Timeout in seconds for the job to be executed.
		ar.put("timeoutwarning", Misc.parseInteger(parser.getOptionValue("n"),(Integer)ar.get("timeout")));				// Timeout in seconds for the warning message to be thrown.
		ar.put("paconf", (String)parser.getOptionValue("f")); 												// Path of the ProActive xml configuration file.
		ar.put("hostname", (String)parser.getOptionValue("H"));						 							// Host to be tested. Ignored.
		ar.put("port", (String)parser.getOptionValue("x"));													// Port of the host to be tested. 
		ar.put("warning", (String)parser.getOptionValue("w", "ignored"));									// Warning level. Ignored.
		ar.put("critical", (String)parser.getOptionValue("c", "ignored")); 									// Critical level. Ignored. 
		ar.put("version", parser.hasOption("V"));															// Prints the version of the plugin.
	
		if ((Boolean)ar.get("help") == true)	
			NagiosPlugin.printMessageUsageAndExit("");
		
		if ((Boolean)ar.get("version") == true)
			NagiosPlugin.printVersionAndExit();
		
		
		final PAMRProber jobp = new PAMRProber(ar);			// Create the prober.
		
		jobp.validateArguments();							// Validate its arguments. In case of problems, it throws an IllegalArgumentException.
	
		jobp.initializeEnvironment();						// Initializes the environment for ProActive objects.
		
		for (String key: ar.keySet())						// Show all the arguments considered. 
			logger.info("\t" + key + ":'" + ar.get(key) + "'");
		
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
			res = proberFuture.get((Integer)ar.get("timeout"), TimeUnit.SECONDS);
			res.appendCurvesSection(tracer.getMeasurementsSummary("time_all"));
		}catch(TimeoutException e){ 		// The execution took more time than expected. 
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "TIMEOUT OF "+(Integer)ar.get("timeout")+ " SEC (last status was: " + tracer.getLastStatusDescription() + ")", e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}catch(ExecutionException e){ 		// There was an unexpected problem with the execution of the prober. 
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "FAILURE: " + e.getMessage(), e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}catch(Exception e){				// There was an unexpected critical exception not captured. 
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.appendCurvesSection(tracer.getMeasurementsSummary(null));
		}
		NagiosPlugin.printAndExit(res, (Integer)ar.get("debug"));
	}
}
