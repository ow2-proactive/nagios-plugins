package qosprobercore.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.objectweb.proactive.core.config.ProActiveConfiguration;

public class PAEnvironmentInitializer {

	public static final String COMMUNICATION_PROTOCOL = "pamr";									// Default protocol to be used to get connected to the RM.
	public static Logger logger = Logger.getLogger(PAEnvironmentInitializer.class.getName());	// Logger.
	
    /** 
	 * Create a java.policy file to grant permissions, and load it for the current JVM. */
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

	public static void initPAConfiguration(HashMap<String, Object> properties) throws Exception{
		/* Security policy procedure. */
		createPolicyAndLoadIt();

		String paconf = (String)properties.get("paconf");
		String host = (String)properties.get("host");
		String port = (String)properties.get("port");
		
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
}
