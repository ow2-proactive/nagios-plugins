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

package qosprober.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import qosprober.main.PAMRProber;

/** This class is supposed to have multiple minor functionalities. */
public class Misc {
	 
	private static Logger logger = Logger.getLogger(Misc.class.getName()); // Logger.
    private Misc(){}

    /** 
     * Return the classpath being used by this JVM. */
    public static String getClasspath() {
        // Get the System Classloader
    	String ret = "";
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
        ret = "";
        // Get the URLs
        URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();

        for(int i=0; i< urls.length; i++)
        {
            ret = ret + "" + urls[i].getFile() + ":";
        }       
        return ret;
    }
    
    /** 
     * Read all the content of a resource file. */
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
   
    /**
     * Create a security policy file granting all the permissions, and make the current JVM load it. */
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
     * Extract the number present in the server's url, which is used later to contact the server. */
    public static  String getResourceNumberFromURL(String url) throws Exception{
    	// Typical one is pamr://9607/Node1807777269
    	if (!url.startsWith(PAMRProber.PREFIX_URL)){
    		throw new Exception("Expected '" + PAMRProber.PREFIX_URL + "' at the beginning but found '" + url + "'.");
    	}
    	String rem = url.substring(PAMRProber.PREFIX_URL.length());
    	rem = rem.substring(0,rem.indexOf('/'));
    	return rem;
    }
    
    /**
     * Generate a non-trivial String with the given amount of characters. */
    public static String generateFibString(int length) { 
    	//System.out.println("Generating Fibonacci...");
		int f0 = 0; 
		int f1 = 1; 
		
		char [] ret = new char[length];
		
		for (int i = 0; i < length; i++) { 
			char c = (char)(f0 % 256);
			
			ret[i] = c;
			
			final int temp = f1; 
			f1 += f0; 
			f0 = temp; 
		} 
		return new String(ret);
    }

    /**
     * Get the java home (so we can use it to run a differentd JVM). */
    public static String getJavaHome(){
    	return System.getProperty("java.home");
    }
    
    /**
     * Run a new JVM with the given arguments. */
    public static void runNewJVM(String classname, String arguments) throws IOException{
    	String command = Misc.getJavaHome() + "/bin/java" 	+ " " + 
    						"-cp " + Misc.getClasspath() 	+ " " + 
    						classname 						+ " " + 
    						arguments;
    	// The command would be something like:
    	//	/tmp/JDK/bin/java -cp bin:../otherbins package.ClassToExecute [arguments]
    	logger.info("Running command: '" + command + "'...");
		Runtime.getRuntime().exec(command);
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
	 * Creates a default set of properties for the log4j logging module. */
	public static Properties getVerboseLoggingProperties(){
		Properties properties = new Properties();
		properties.put("log4j.rootLogger",				"INFO,rollingFile");// By default, do not show anything.
		//properties.put("log4j.logger.org",			"ERROR,STDOUT");	 // For this module, show warning messages in stdout.
		//properties.put("log4j.logger.proactive", 		"ERROR,STDOUT");
		//properties.put("log4j.logger.qosprober", 		"ERROR,STDOUT");
		/* NULL Appender. */
		properties.put("log4j.appender.NULL",			"org.apache.log4j.varia.NullAppender");
		
		properties.put("log4j.appender.rollingFile",				"org.apache.log4j.RollingFileAppender");
		properties.put("log4j.appender.rollingFile.File",			"output");
		properties.put("log4j.appender.rollingFile.MaxFileSize",	"1MB");
		properties.put("log4j.appender.rollingFile.MaxBackupIndex",	"2");
		properties.put("log4j.appender.rollingFile.layout",			"org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.rollingFile.layout.ConversionPattern",	"%p %t %c - %m%n");
		
		return properties;
	}
	
	/**
	 * Configures de log4j module for logging. */
	public static void log4jConfiguration(int debuglevel){
		System.setProperty("log4j.configuration", "");
		if (debuglevel == PAMRProber.DEBUG_LEVEL_3USER){
			/* We load the log4j.properties file. */
			PropertyConfigurator.configure("log4j.properties");
		}else {
			/* We do the log4j configuration on the fly. */
			Properties properties = Misc.getSilentLoggingProperties();
			PropertyConfigurator.configure(properties);
		}
	}

	public static void log4jConfiguration2(){
		System.setProperty("log4j.configuration", "");
		/* We do the log4j configuration on the fly. */
		Properties properties = Misc.getVerboseLoggingProperties();
		PropertyConfigurator.configure(properties);
	
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
	 * Used when a parameter given by the user is wrong. 
	 * Print a message, then the usage of the application, and the exits the application. */
	public static void printMessageUsageAndExit(String mainmessage){
		System.out.println(mainmessage);
	    Misc.printUsage();
	    System.exit(PAMRProber.RESULT_CRITICAL);
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
	    System.exit(PAMRProber.RESULT_OK);
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
}
