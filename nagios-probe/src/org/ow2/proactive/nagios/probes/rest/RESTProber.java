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

package org.ow2.proactive.nagios.probes.rest;

import org.ow2.proactive.nagios.common.Arguments;
import org.ow2.proactive.nagios.common.ElementalNagiosPlugin;
import org.ow2.proactive.nagios.common.NagiosMiniStatus;
import org.ow2.proactive.nagios.common.NagiosReturnObject;
import org.ow2.proactive.nagios.common.NagiosReturnObjectSummaryMaker;
import org.ow2.proactive.nagios.common.TimedStatusTracer;

/**
 * Prober for the REST API of the ProActive Scheduler. 
 * Steps for the probe: 
 * - Login to the scheduler (if no --avoidlogin flag present). 
 * - Get the version of the REST API.
 * - Get the isconnected REST resource response (if no --avoidlogin flag present).
 * - Logout from the scheduler (if no --avoidlogin flag present). 
 * - If everything was correctly obtained, then no problem is told. 
 */
public class RESTProber extends ElementalNagiosPlugin{
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this JobProber. 
	 * @throws Exception */
	public RESTProber(Arguments args) throws Exception{
		super("REST", args);
		args.addNewOption("H", "host", true);			// User.
		args.addNewOption("u", "user", true);			// User.
		args.addNewOption("p", "pass", true);			// Pass.
		args.addNewOption("r", "url", true);			// Url of the Scheduler/RM.
		args.addNewOption("A", "avoidlogin", false);	// Avoid login (result of the probe based only on the response of the REST API).  
		args.addNewOption("k", "skipauth", false);		// Skip https authentication checking.
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
		arguments.checkIsGiven("url");
		if (arguments.getStr("url").contains("scheduler") == false){
			throw new IllegalArgumentException("The URL given (" + arguments.getBoo("url") + 
					") seems to be incorrect since it does not contain the string 'scheduler'. The url" +
					"to specify is the scheduler's REST API.");
		}
		
		if (getArgs().getBoo("avoidlogin") == false){
			arguments.checkIsGiven("user");
			arguments.checkIsGiven("pass");
		}
	}
	
	/**
	 * Probe the REST API. 
	 * Several calls are done against the REST API, like isConnected and getVersion.
	 * @param tracer tracer that lets keep track of the last status, and the time each call took to be executed.
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws Exception{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(getArgs().getInt("critical")));
		if (getArgs().isGiven("warning")==true) // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(getArgs().getInt("warning")));
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "timeout reached while initializing the probe...");
		
		RestStubProber reststub = new RestStubProber(getArgs().getBoo("skipauth"));	// We create directly the stub prober.
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection", "timeout reached while connecting to the server...");
		
		reststub.connect(											// We get connected to the server...
				getArgs().getStr("url"));	
		
		if (getArgs().getBoo("avoidlogin") == false){
			tracer.finishLastMeasurementAndStartNewOne("time_login", "timeout reached while performing login to the scheduler through REST API...");
			reststub.login(											// We login in the Scheduler.
					getArgs().getStr("user"), 
					getArgs().getStr("pass"));	
		}
		
		tracer.finishLastMeasurementAndStartNewOne("time_transactions", "timeout reached while asking [isconnected and] version to the REST API...");
		
		Boolean connected = false; 
		if (getArgs().getBoo("avoidlogin") == false){
			connected = reststub.isConnected();						// Check whether we are connected or not to the scheduler.
		}
		
		String version = reststub.getVersion();						// Get the version of the REST API.
	    logger.info("Version: " + version);
		
		if (getArgs().getBoo("avoidlogin") == false){
			tracer.finishLastMeasurementAndStartNewOne("time_disconnection", "timeout reached while disconnecting from the REST API...");
			reststub.disconnect();									// Getting disconnected from the Scheduler.
		}
		
		tracer.finishLastMeasurement();
	
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		if (getArgs().getBoo("avoidlogin") == false){
			summary.addFact("Connected:" + connected);
			if (connected == false)
				summary.addMiniStatus(new NagiosMiniStatus(RESULT_2_CRITICAL, "couldn't connect to scheduler through REST API"));
		}
		
		if (getArgs().isGiven("warning") && tracer.getTotal() > getArgs().getInt("warning"))
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_1_WARNING, "the probe took too long"));
		
		if (summary.isAllOkay())
			summary.addMiniStatus(new NagiosMiniStatus(RESULT_0_OK, "OK"));
		
		return summary.getSummaryOfAllWithTimeAll(tracer);
	}
	
	/**
	 * Starting point. */
	public static void main(String[] args) throws Exception{
        Arguments options = new Arguments(args);
		RESTProber prob = new RESTProber(options);													// Create the prober.
		prob.initializeProber();
		prob.startProbeAndExit();																	// Start the probe.
	}
}
