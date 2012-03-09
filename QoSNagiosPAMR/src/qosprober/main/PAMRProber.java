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

import qosprober.misc.PAMRMisc;
import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosMiniStatus;
import qosprobercore.main.PANagiosPlugin;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.PAEnvironmentInitializer;
import qosprobercore.main.TimedStatusTracer;

/** 
 * This is a general Nagios plugin class that performs a test on the PAMR Router of ProActive based grids.
 *  After that, a short summary regarding the result of the test is shown using Nagios format. 
 *  This probe creates a server Active Object that uses the specified PAMR router. Then it launches another JVM
 *  that instances another Active Object, this time the client. The client contacts the server, going through 
 *  the PAMR router, so probing it. If the call that the client is supposed to do on a server's method does not arrive
 *  then there is a failure. 
 *  The steps of the probe: 
 *  - Create a server (Active Object) connected to the PAMR router. 
 *  - Obtain the URL of the server.
 *  - Create a client (another Acitve Object connected to the same PAMR router), while the server waits for a call from the client.  
 *  - If the server gets the call, everything is okay. Both server and client have timeout mechanisms.
 *  */
public class PAMRProber extends PANagiosPlugin{
	
	public final static String SERVER_NAME = "server";		// Name that the server will use to register itself.
    public final static String PREFIX_URL = "pamr://";		// Prefix that is expected in a URL given by the registered server.
    public final static int MESSAGE_LENGTH = 1024;			// Length of the parameter passed when the client calls the server.
    
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this PAMRProber. 
	 * @throws Exception */
	public PAMRProber(Arguments args) throws Exception{
		super("PAMR", args);
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
		/* Nothing to validate */
	}
	
	/**
	 * Probe the PAMR router creating two active objects (in different JVM) that should 
	 * get communicated. 
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. 
	 * @throws Exception */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws Exception{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(getArgs().getInt("critical")));
		if (getArgs().isGiven("warning")==true){ // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(getArgs().getInt("warning")));
		}
		
    	String serverurl = null;
    	Server server = null;
    
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "timeout reached while initializing the probe...");
    	
        // Creates an active object for the server
        server = org.objectweb.proactive.api.PAActiveObject.newActive(Server.class, null);
        
		tracer.finishLastMeasurementAndStartNewOne("time_registering_server", "timeout reached while registering the server...");
        
        logger.info("Registering server...");
        org.objectweb.proactive.api.PAActiveObject.registerByName(server, SERVER_NAME);
        String url = org.objectweb.proactive.api.PAActiveObject.getActiveObjectNodeUrl(server);
        logger.info("Done.");
        
        serverurl = PREFIX_URL + PAMRMisc.getResourceNumberFromURL(url) + "/" + SERVER_NAME;
        logger.info(">>> Server standard URL: "+ serverurl);
        
		tracer.finishLastMeasurementAndStartNewOne("time_executing_client", "timeout reached while executing the client active object...");
       
        // Now we are ready to run the client. It needs to get connected to the same PAMR router, and afterwards contact
        // this just initialized server. About the initialization of the client, there are 2 possible argument-format 
        // for this module:
    	// SERVERURL PACONFIGURATIONFILE				(2 arguments, assume PA conf. file is given).
    	// SERVERURL PAMRADDRESS PAMRPORT				(3 arguments, assume server address and port are given).
    	
        logger.info("Running the client...");
        if (PAEnvironmentInitializer.usingPAConfigurationFile() == true){		
        								// Depending on whether there is a ProActive configuration file, these are
        								// the parameters that we send to the client to get connected to the same router.
	        qosprober.misc.PAMRMisc.runNewJVM("-Xmx80m", Client.class.getName(), 
	        		serverurl + " " + getArgs().getStr("paconf") + " " + getArgs().getInt("critical"));
        }else{
        	qosprober.misc.PAMRMisc.runNewJVM("-Xmx80m", Client.class.getName(), 
        			serverurl + " " + getArgs().getStr("hostname") + " " + getArgs().getStr("port") + " " + getArgs().getInt("critical"));
        }
        logger.info("Done.");
        
		tracer.finishLastMeasurementAndStartNewOne("time_waiting_message", "timeout reached while waiting for the communication between both active object launched...");
        
        logger.info("Waiting for the client's message...");
        
    	while(server.isDone()==false){	// Wait for the client's call...
    		Thread.sleep(300);
    	}
    
		tracer.finishLastMeasurement();
    	
		
		
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		if (getArgs().isGiven("warning") && tracer.getTotal() > getArgs().getInt("warning")){ // If it took longer than timeoutwarnsec, throw a warning message.
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_1_WARNING, "PROBE TOO SLOW"));
		}
		
		if (summary.isAllOkay() == true){	// If everything went okay...
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_0_OK,"OK"));
		}
		
		return summary.getSummaryOfAllWithTimeAll(tracer);
	}
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt */
	public static void main(String[] args) throws Exception{
        final Arguments options = new Arguments(args);
        
		PAMRProber prob = new PAMRProber(options);													// Create the prober.
		prob.initializeProber();
		prob.startProbeAndExit();																	// Start the probe.
	}
}
