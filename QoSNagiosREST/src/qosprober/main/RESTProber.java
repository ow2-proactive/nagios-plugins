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

import qosprobercore.main.Arguments;
import qosprobercore.main.NagiosPlugin;
import qosprobercore.main.NagiosReturnObjectSummaryMaker;
import qosprobercore.main.NagiosReturnObject;
import qosprobercore.main.TimedStatusTracer;

public class RESTProber extends NagiosPlugin{
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this JobProber. 
	 * @throws Exception */
	public RESTProber(Arguments args) throws Exception{
		super(args);
		args.addNewOption("u", "user", true);			// User.
		args.addNewOption("p", "pass", true);			// Pass.
		args.addNewOption("r", "url", true);			// Url of the Scheduler/RM.
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
	 * Several calls are done against the scheduler: join, remove old jobs, submit job, get job status (based on 
	 * polling or in events, depends on the protocol chosen), get job result, remove job, disconnect.
	 * After a correct disconnection call, the output of the job is compared with a given correct output, and the 
	 * result of the test is told. 
	 * @param tracer tracer that lets keep track of the last status, and the time each call took to be executed.
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. */	 
	public NagiosReturnObject probe(TimedStatusTracer tracer) throws Exception{
		// We add some reference values to be printed later in the summary for Nagios.
		tracer.addNewReference("timeout_threshold", new Double(getArgs().getInt("critical")));
		if (getArgs().isGiven("warning")==true) // If the warning flag was given, then show it.
			tracer.addNewReference("time_all_warning_threshold", new Double(getArgs().getInt("warning")));
		
		tracer.finishLastMeasurementAndStartNewOne("time_initializing", "initializing the probe...");
		
		RestStubProber schedulerstub = new RestStubProber();	// We create directly the stub prober.
		
		tracer.finishLastMeasurementAndStartNewOne("time_connection", "connecting to the scheduler...");
		
		schedulerstub.init(										// We get connected to the Scheduler.
				getArgs().getStr("url"),  getArgs().getStr("user"), 
				getArgs().getStr("pass"));	
		
		
		tracer.finishLastMeasurementAndStartNewOne("time_disconnection", "connected, disconnecting...");
		
		schedulerstub.disconnect();												// Getting disconnected from the Scheduler.
		
		tracer.finishLastMeasurement();
	
		
		NagiosReturnObjectSummaryMaker summary = new NagiosReturnObjectSummaryMaker();  
		
		if (getArgs().isGiven("warning") && tracer.getTotal() > getArgs().getInt("warning"))
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_1_WARNING, "TOO SLOW"));
		
		if (summary.isAllOkay())
			summary.addNagiosReturnObject(new NagiosReturnObject(NagiosReturnObject.RESULT_0_OK, "OK"));
		
		return summary.getSummaryOfAll();
	}
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
        Arguments options = new Arguments(args);
		RESTProber prob = new RESTProber(options);													// Create the prober.
		prob.initializeProber();
		prob.startProbeAndExit();																	// Start the probe.
	}
}
