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

package qosprobercore.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.apache.log4j.Logger;
import org.objectweb.proactive.core.config.ProActiveConfiguration;

/**
 * Class that helps initializing the ProActive environment, specially when referred to the PAMR router
 * connection parameters. */
public class PAEnvironmentInitializer {

	public static final String COMMUNICATION_PROTOCOL = "pamr";									// Default protocol to be used to get connected to the RM.
	public static Logger logger = Logger.getLogger(PAEnvironmentInitializer.class.getName());	// Logger.
	
	private static Boolean usePAConfFile = null;
	
    /** 
	 * Create a java.policy file to grant permissions, and load it in the current JVM. */
	public static void createPolicyAndLoadIt() throws Exception{
		logger.info("Setting security policies... ");
		try{
		    File temp = File.createTempFile("javapolicy", ".policy"); // Create temp file.
		    temp.deleteOnExit(); // Delete temp file when program exits.
		    // Write to temp file.
		    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
		    String policycontent = "grant {permission java.security.AllPermission;};";
		    out.write(policycontent);
		    out.close();
		    String policypath = temp.getAbsolutePath(); 
		    System.setProperty("java.security.policy", policypath); // Load security policy.
		}catch(Exception e){
			throw new Exception("Error while creating the security policy file. " + e.getMessage());
		}
		logger.info("Done.");
	}

	/**
	 * Initialize the ProActive configuration. It handles the PAMR router configuration.
	 * @param paconf ProActive configuration file (if needed).
	 * @param host host where the PAMR router is.
	 * @param port port where the PAMR router service is exposed.
	 * @throws Exception */
	public static void initPAConfiguration(String paconf, String host, String port) throws Exception{
		/* Security policy procedure. */
		createPolicyAndLoadIt();

		/* Load ProActive configuration. */
		boolean usepaconffilee = false;
		/* Check whether to use or not the ProActive configuration file. */
		if (paconf!=null){
			/* A ProActiveConf.xml file was given. If we find it, we use it. */
			if (new File(paconf).exists()==true){
				System.setProperty("proactive.configuration", paconf);
				usepaconffilee = true;
			}else{
				logger.warn("The ProActive configuration file '"+paconf+"' was not found. Using default configuration.");
			}
		}
		
		usePAConfFile = usepaconffilee;
		
		if (usepaconffilee == false){ // The PA configuration file was not given or was not found.
			logger.info("Avoiding ProActive configuration file...");
			ProActiveConfiguration pac = ProActiveConfiguration.getInstance();	
			if (host!=null && port!=null){
				pac.setProperty("proactive.communication.protocol", COMMUNICATION_PROTOCOL, false);
				pac.setProperty("proactive.net.router.address", host, false);
				pac.setProperty("proactive.net.router.port", port, false);
				logger.info("Using 'hostname' and 'port' provided...");
			}else{
				logger.info("Avoiding 'hostname' and 'port' provided...");
			}
		}
	}

	public static Boolean usingPAConfigurationFile(){
		return usePAConfFile;
	}
}
