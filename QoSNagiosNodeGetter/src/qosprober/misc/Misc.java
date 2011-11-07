package qosprober.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import qosprober.main.RMProber;

/** This class is supposed to have multiple minor functionalities. */
public class Misc {

	private static Logger logger = Logger.getLogger(Misc.class.getName()); // Logger.
	
    private Misc(){}

    /* Read all the content of a resource file. */
    public static String readAllTextResource(String resource) throws IOException{
		InputStream is = Misc.class.getResourceAsStream(resource);
	    InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
	    String line;
	    String ret = "";
	    while ((line = br.readLine()) != null){
	      ret = ret + line + "\n";
	    }
	    br.close();
	    isr.close();
	    is.close();
	    return ret;
	}
    
    /* Return the name of a job by reading its job file descriptor. */
    public static String getJobNameFromJobDescriptor(String jobdescxmlpath) throws Exception{
    	Job job = JobFactory.getFactory().createJob(jobdescxmlpath);
    	return job.getName();
    }
    
	/** 
	 * Create a java.policy file to grant permissions, and load it for the current JVM. */
	public static void createPolicyAndLoadIt() throws Exception{
		try{
		    File temp = File.createTempFile("javapolicy", ".policy"); // Create temp file.
		    
		    temp.deleteOnExit(); // Delete temp. file when program exits.

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
	}
	
	/**
	 * Used when a parameter given by the user is wrong. 
	 * Print a message, then the usage of the application, and the exits the application. */
	public static void printMessageUsageAndExit(String mainmessage){
		System.out.println(mainmessage);
	    Misc.printUsage();
	    System.exit(RMProber.RESULT_CRITICAL);
	}
	
	/**
	 * Print the usage of the application. */
	public static void printUsage(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/usage.txt");
			System.err.println(usage);
		} catch (IOException e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	}

	/**
	 * Print the version of the plugin and the exits the application. */
	public static void printVersionAndExit(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/version.txt");
			System.err.println(usage);
		} catch (IOException e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	    System.exit(RMProber.RESULT_OK);
	}

	/**
	 * Parse the corresponding value, and if any problem, return the default value given. */
	public static Integer parseInteger(String o, Integer defaultvalue){
		Integer ret;
		try{
			ret = Integer.parseInt(o);
		}catch(Exception e){
			ret = defaultvalue;
		}
		return ret;
	}
	
	/**
	 * Creates a default set of properties for the log4j logging module. */
	public static Properties getSilentLoggingProperties(){
		Properties properties = new Properties();
		properties.put("log4j.rootLogger",				"ERROR,NULL"); 	// By default, do not show anything.
		properties.put("log4j.logger.org",				"ERROR,STDOUT");	// For this module, show warning messages in stdout.
		properties.put("log4j.logger.proactive", 		"ERROR,STDOUT");
		properties.put("log4j.logger.qosprober", 		"ERROR,STDOUT");
		/* NULL Appender. */
		properties.put("log4j.appender.NULL",			"org.apache.log4j.varia.NullAppender");
		/* STDOUT Appender. */
		properties.put("log4j.appender.STDOUT",			"org.apache.log4j.ConsoleAppender");
		properties.put("log4j.appender.STDOUT.Target",	"System.out");
		properties.put("log4j.appender.STDOUT.layout",	"org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.STDOUT.layout.ConversionPattern","%-4r [%t] %-5p %c %x - %m%n");
		return properties;
	}
	
	/**
	 * Configures de log4j module for logging. */
	public static void log4jConfiguration(int debuglevel){
		System.setProperty("log4j.configuration", "");
		if (debuglevel == RMProber.DEBUG_LEVEL_3USER){
			/* We load the log4j.properties file. */
			PropertyConfigurator.configure("log4j.properties");
		}else {
			/* We do the log4j configuration on the fly. */
			Properties properties = Misc.getSilentLoggingProperties();
			PropertyConfigurator.configure(properties);
		}
	}
}
