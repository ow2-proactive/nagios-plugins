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

package org.ow2.proactive.nagios.common;

import org.apache.log4j.Logger;
import org.ow2.proactive.nagios.misc.Misc;


/**
 * Class to be inherited by any Nagios plugin that is strongly related to ProActive. */
public abstract class PANagiosPlugin extends ElementalNagiosPlugin {
	
	protected static Logger logger =						// Logger. 
			Logger.getLogger(PANagiosPlugin.class.getName()); 
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param probeid id of the current prober (RM, Scheduler, etc.).
	 * @param args arguments to create this NagiosPlugin. */
	public PANagiosPlugin(String probeid, Arguments args){
		super(probeid, args);
		args.addNewOption("f", "paconf", true);													// Path of the ProActive xml configuration file.
		args.addNewOption("H", "hostname", true);												// Host to be tested. 
		args.addNewOption("x", "port"    , true);												// Port of the host to be tested. 
		
	}
	
	/**
	 * Basic initialization for any NagiosProbe related to ProActive.
	 * @throws Exception in case of any error. */
	public void initializeProber() throws Exception{
		super.initializeProber();
		this.validateArguments(this.getArgs());
		PAEnvironmentInitializer.createPolicyAndLoadIt();										// Security policy procedure.
		PAEnvironmentInitializer.initPAConfiguration(											// PAMR router configuration.
			getArgs().getStr("paconf"),
			getArgs().getStr("hostname"),
			getArgs().getStr("port"));
	}
	
	/** 
	 * Validate all the arguments given to this probe. 
	 * @param args arguments to be validated.
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	private void validateArguments(Arguments args) throws IllegalArgumentException{
//		args.checkIsGiven("port");									// Might not be given (there is a default value), so we don't check it.
		args.checkIsValidInt("port", 0, 65535);
	}
	
	
	
	/**
	 * Used when a parameter given by the user is wrong. 
	 * Print a message, then the usage of the application, and the exits the application. 
	 * @param errormessage message of error to be shown to the user (through Nagios). */
	public void printMessageUsageAndExit(String errormessage){
		if (errormessage!=null){
			System.out.println(errormessage);
		}
		String usage = ""; 
		try {
			usage = usage + Misc.readAllTextResource(RESOURCES_PATH + "usage-" + probeID + ".txt");
			usage = usage + Misc.readAllTextResource(RESOURCES_PATH + "usage-pa.txt");
			usage = usage + Misc.readAllTextResource(RESOURCES_PATH + "usage-core.txt");
			System.err.println(usage);
		} catch (Exception e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	
	    System.exit(RESULT_2_CRITICAL);
	}
}
